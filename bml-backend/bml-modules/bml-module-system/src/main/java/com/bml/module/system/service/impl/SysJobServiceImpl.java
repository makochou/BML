package com.bml.module.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.result.PageResult;
import com.bml.module.system.dto.SysJobDTO;
import com.bml.module.system.dto.SysJobLogQuery;
import com.bml.module.system.entity.SysJob;
import com.bml.module.system.entity.SysJobLog;
import com.bml.module.system.mapper.SysJobLogMapper;
import com.bml.module.system.mapper.SysJobMapper;
import com.bml.module.system.service.SysJobService;
import com.bml.module.system.task.ServerAlertJob;
import com.bml.module.system.vo.SysJobLogVO;
import com.bml.module.system.vo.SysJobVO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
@RequiredArgsConstructor
public class SysJobServiceImpl extends BaseServiceImpl<SysJobMapper, SysJob> implements SysJobService {

    private final SysJobLogMapper jobLogMapper;

    private final ServerAlertJob serverAlertJob;

    private final Map<Long, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    private final Set<Long> runningJobs = ConcurrentHashMap.newKeySet();

    private ThreadPoolTaskScheduler taskScheduler;

    @PostConstruct
    public void initScheduler() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(4);
        taskScheduler.setThreadNamePrefix("BML-System-Job-");
        taskScheduler.initialize();
        this.lambdaQuery().eq(SysJob::getStatus, 1).list().forEach(this::scheduleJob);
    }

    @PreDestroy
    public void shutdownScheduler() {
        scheduledJobs.values().forEach(future -> future.cancel(false));
        scheduledJobs.clear();
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }

    @Override
    public PageResult<SysJobVO> selectJobPage(SysJobDTO dto) {
        SysJobDTO safeDto = dto == null ? new SysJobDTO() : dto;
        int pageNum = normalizePageNum(safeDto.getPageNum());
        int pageSize = normalizePageSize(safeDto.getPageSize());
        LambdaQueryWrapper<SysJob> wrapper = new LambdaQueryWrapper<SysJob>()
                .like(StrUtil.isNotBlank(safeDto.getJobName()), SysJob::getJobName, safeDto.getJobName())
                .eq(StrUtil.isNotBlank(safeDto.getJobGroup()), SysJob::getJobGroup, safeDto.getJobGroup())
                .eq(safeDto.getStatus() != null, SysJob::getStatus, safeDto.getStatus())
                .orderByDesc(SysJob::getCreateTime, SysJob::getId);
        Page<SysJob> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        List<SysJobVO> records = page.getRecords().stream().map(this::toJobVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public SysJobVO selectJobById(Long id) {
        return id == null ? null : toJobVO(this.getById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertJob(SysJobDTO dto) {
        validateJob(dto);
        SysJob job = toJobEntity(dto);
        if (StrUtil.isBlank(job.getJobGroup())) {
            job.setJobGroup("DEFAULT");
        }
        if (job.getConcurrent() == null) {
            job.setConcurrent(0);
        }
        if (job.getMisfirePolicy() == null) {
            job.setMisfirePolicy(1);
        }
        boolean saved = this.save(job);
        if (saved && Integer.valueOf(1).equals(job.getStatus())) {
            scheduleJob(job);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateJob(SysJobDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        validateJob(dto);
        SysJob job = toJobEntity(dto);
        boolean updated = this.updateById(job);
        if (updated) {
            cancelJob(dto.getId());
            SysJob current = this.getById(dto.getId());
            if (current != null && Integer.valueOf(1).equals(current.getStatus())) {
                scheduleJob(current);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteJob(Long id) {
        cancelJob(id);
        return id != null && this.removeById(id);
    }

    @Override
    public boolean runOnce(Long id) {
        SysJob job = id == null ? null : this.getById(id);
        if (job == null) {
            return false;
        }
        executeJob(job);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeStatus(Long id, Integer status) {
        SysJob job = id == null ? null : this.getById(id);
        if (job == null) {
            return false;
        }
        job.setStatus(status == null ? 0 : status);
        boolean updated = this.updateById(job);
        if (updated) {
            cancelJob(id);
            if (Integer.valueOf(1).equals(job.getStatus())) {
                scheduleJob(job);
            }
        }
        return updated;
    }

    @Override
    public PageResult<SysJobLogVO> selectLogPage(SysJobLogQuery query) {
        SysJobLogQuery safeQuery = query == null ? new SysJobLogQuery() : query;
        int pageNum = normalizePageNum(safeQuery.getPageNum());
        int pageSize = normalizePageSize(safeQuery.getPageSize());
        LambdaQueryWrapper<SysJobLog> wrapper = new LambdaQueryWrapper<SysJobLog>()
                .like(StrUtil.isNotBlank(safeQuery.getJobName()), SysJobLog::getJobName, safeQuery.getJobName())
                .eq(StrUtil.isNotBlank(safeQuery.getJobGroup()), SysJobLog::getJobGroup, safeQuery.getJobGroup())
                .eq(safeQuery.getStatus() != null, SysJobLog::getStatus, safeQuery.getStatus())
                .orderByDesc(SysJobLog::getStartTime, SysJobLog::getId);
        Page<SysJobLog> page = jobLogMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<SysJobLogVO> records = page.getRecords().stream().map(this::toLogVO).toList();
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cleanLogs() {
        return jobLogMapper.delete(new LambdaQueryWrapper<SysJobLog>().isNotNull(SysJobLog::getId)) >= 0;
    }

    @Override
    public List<String> registeredTargets() {
        return List.of("serverAlertJob.checkServerMetrics");
    }

    private void scheduleJob(SysJob job) {
        if (taskScheduler == null || job == null || job.getId() == null) {
            return;
        }
        cancelJob(job.getId());
        ScheduledFuture<?> future = taskScheduler.schedule(() -> executeJob(job), new CronTrigger(job.getCronExpression()));
        if (future != null) {
            scheduledJobs.put(job.getId(), future);
        }
    }

    private void cancelJob(Long id) {
        if (id == null) {
            return;
        }
        ScheduledFuture<?> future = scheduledJobs.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void executeJob(SysJob job) {
        if (job == null || job.getId() == null) {
            return;
        }
        if (!Integer.valueOf(1).equals(job.getConcurrent()) && !runningJobs.add(job.getId())) {
            return;
        }
        LocalDateTime start = LocalDateTime.now();
        long startMs = System.currentTimeMillis();
        SysJobLog log = new SysJobLog();
        log.setJobName(job.getJobName());
        log.setJobGroup(job.getJobGroup());
        log.setInvokeTarget(job.getInvokeTarget());
        log.setStartTime(start);
        try {
            invokeTarget(job.getInvokeTarget());
            log.setStatus(0);
            log.setJobMessage("任务执行成功");
        } catch (Exception ex) {
            log.setStatus(1);
            log.setJobMessage("任务执行失败");
            log.setExceptionInfo(truncate(ex.getMessage(), 2000));
        } finally {
            log.setEndTime(LocalDateTime.now());
            log.setCostTime(System.currentTimeMillis() - startMs);
            jobLogMapper.insert(log);
            runningJobs.remove(job.getId());
        }
    }

    private void invokeTarget(String invokeTarget) {
        if ("serverAlertJob.checkServerMetrics".equals(invokeTarget)) {
            serverAlertJob.checkServerMetrics();
            return;
        }
        throw new IllegalArgumentException("不支持的调用目标：" + invokeTarget);
    }

    private void validateJob(SysJobDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("任务参数不能为空");
        }
        if (!registeredTargets().contains(dto.getInvokeTarget())) {
            throw new IllegalArgumentException("调用目标未注册");
        }
        CronExpression.parse(dto.getCronExpression());
    }

    private SysJob toJobEntity(SysJobDTO dto) {
        SysJob entity = new SysJob();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private SysJobVO toJobVO(SysJob entity) {
        if (entity == null) {
            return null;
        }
        SysJobVO vo = new SysJobVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private SysJobLogVO toLogVO(SysJobLog entity) {
        if (entity == null) {
            return null;
        }
        SysJobLogVO vo = new SysJobLogVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }
}
