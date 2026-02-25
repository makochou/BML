package com.bml.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.mapper.SysAlertMapper;
import com.bml.module.system.service.ISysAlertService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统告警通知 Service 实现类
 * <p>
 * 核心方法说明：
 * <ul>
 * <li>{@code getLatestAlerts(lastId)} — 前端增量轮询，每次只拉取新产生的告警</li>
 * <li>{@code markAsRead / markAllAsRead} — 已读标记操作</li>
 * <li>{@code deleteAlert} — 逻辑删除（由 MyBatis-Plus @TableLogic 自动处理）</li>
 * </ul>
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysAlertServiceImpl extends ServiceImpl<SysAlertMapper, SysAlert> implements ISysAlertService {

    /** 增量轮询默认返回条数（当 lastId 为 null 时） */
    private static final int DEFAULT_LATEST_LIMIT = 20;

    /**
     * 获取未读告警总数
     */
    @Override
    public long getUnreadCount() {
        return this.count(new LambdaQueryWrapper<SysAlert>()
                .eq(SysAlert::getReadStatus, 0));
    }

    /**
     * 获取最近 N 条告警（按时间倒序），用于通知面板全量展示
     */
    @Override
    public List<SysAlert> getRecentAlerts(int limit) {
        return this.list(new LambdaQueryWrapper<SysAlert>()
                .orderByDesc(SysAlert::getCreateTime)
                .last("LIMIT " + limit));
    }

    /**
     * 前端增量轮询核心方法
     * <p>
     * 前端每 30 秒携带上一次拿到的最大 ID 进行轮询，
     * 后端仅返回该 ID 之后新产生的告警记录（按 ID 升序），
     * 实现低成本的准实时通知推送。
     * </p>
     *
     * @param lastId 上次已知的最大告警 ID；为 null 时返回最近 DEFAULT_LATEST_LIMIT 条
     * @return 增量告警列表
     */
    @Override
    public List<SysAlert> getLatestAlerts(Long lastId) {
        if (lastId == null) {
            // 首次加载：返回最近的告警（按时间倒序）
            return this.list(new LambdaQueryWrapper<SysAlert>()
                    .orderByDesc(SysAlert::getCreateTime)
                    .last("LIMIT " + DEFAULT_LATEST_LIMIT));
        }
        // 增量查询：只返回 ID 大于 lastId 的新告警（按 ID 升序）
        return this.list(new LambdaQueryWrapper<SysAlert>()
                .gt(SysAlert::getId, lastId)
                .orderByAsc(SysAlert::getId));
    }

    /**
     * 标记单条告警为已读
     */
    @Override
    public boolean markAsRead(Long id) {
        return this.update(new LambdaUpdateWrapper<SysAlert>()
                .eq(SysAlert::getId, id)
                .set(SysAlert::getReadStatus, 1));
    }

    /**
     * 一键标记所有未读告警为已读
     *
     * @return 实际更新的记录数
     */
    @Override
    public int markAllAsRead() {
        LambdaUpdateWrapper<SysAlert> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysAlert::getReadStatus, 0)
                .set(SysAlert::getReadStatus, 1);
        return this.baseMapper.update(null, updateWrapper);
    }

    /**
     * 逻辑删除指定告警
     * <p>
     * 由于实体类 {@link SysAlert} 继承了 {@code BaseEntity}，
     * 其中的 {@code @TableLogic deleted} 字段会被 MyBatis-Plus 自动处理为逻辑删除。
     * 因此调用 {@link #removeById(java.io.Serializable)} 实际执行的是 UPDATE SET deleted=1
     * 而非物理 DELETE。
     * </p>
     */
    @Override
    public boolean deleteAlert(Long id) {
        return this.removeById(id);
    }

    /**
     * 获取存在告警的日期列表
     * <p>
     * 查询所有告警记录的 createTime，提取日期部分去重后倒序排列。
     * 前端日期选择器据此高亮有告警的日期。
     * </p>
     */
    @Override
    public List<String> getAlertDates() {
        // 查询所有告警的创建时间（仅需 createTime 字段以减少数据传输）
        List<SysAlert> alerts = this.list(new LambdaQueryWrapper<SysAlert>()
                .select(SysAlert::getCreateTime)
                .orderByDesc(SysAlert::getCreateTime));

        // 提取日期字符串并去重
        return alerts.stream()
                .map(alert -> alert.getCreateTime().toLocalDate().toString())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 按指定日期查询当天的所有告警
     * <p>
     * 使用 LocalDateTime 范围 [date 00:00:00, date 23:59:59] 进行精确过滤。
     * </p>
     */
    @Override
    public List<SysAlert> getAlertsByDate(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

        return this.list(new LambdaQueryWrapper<SysAlert>()
                .ge(SysAlert::getCreateTime, dayStart)
                .le(SysAlert::getCreateTime, dayEnd)
                .orderByDesc(SysAlert::getCreateTime));
    }
}
