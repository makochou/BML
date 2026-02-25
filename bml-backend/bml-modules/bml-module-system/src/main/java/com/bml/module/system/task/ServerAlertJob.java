package com.bml.module.system.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.service.ISysAlertService;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.vo.ServerInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 服务器资源指标自动巡检与告警任务
 *
 * @author BML Team
 */
@Slf4j
@Component
public class ServerAlertJob {

    @Autowired
    private ServerMonitorService serverMonitorService;

    @Autowired
    private ISysAlertService sysAlertService;

    // 冷却时间: 15分钟 (毫秒)
    private static final long COOLDOWN_MS = 15 * 60 * 1000L;

    // 告警阈值设定
    private static final double CPU_THRESHOLD = 85.0;
    private static final double MEM_THRESHOLD = 85.0;
    private static final double JVM_THRESHOLD = 90.0;
    private static final double DISK_THRESHOLD = 90.0;

    /**
     * 每分钟执行一次巡检
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkServerMetrics() {
        log.debug("开始巡检服务器资源指标...");
        try {
            ServerInfoVO metrics = serverMonitorService.getServerInfo();
            checkCpu(metrics.getCpu());
            checkMemory(metrics.getMem());
            checkJvm(metrics.getJvm());
            checkDisks(metrics.getDisks());
        } catch (Exception e) {
            log.error("巡检服务器资源指标失败: {}", e.getMessage(), e);
        }
    }

    private void checkCpu(ServerInfoVO.CpuInfo cpu) {
        if (cpu == null)
            return;
        if (cpu.getUsed() + cpu.getSys() > CPU_THRESHOLD) {
            String content = String.format("系统 CPU 负载过高！当前消耗: %.2f%%，用户进程: %.2f%%，请检查是否有异常占用。",
                    (cpu.getUsed() + cpu.getSys()), cpu.getUsed());
            triggerAlert("CPU_HIGH", "warning", "🎯 CPU 负载超标告警", content);
        }
    }

    private void checkMemory(ServerInfoVO.MemInfo mem) {
        if (mem == null)
            return;
        if (mem.getUsage() > MEM_THRESHOLD) {
            String content = String.format("物理内存占用过高！当前使用率: %.2f%% (已用: %s)。",
                    mem.getUsage(), mem.getUsed());
            triggerAlert("MEMORY_HIGH", "warning", "🧠 物理内存超标告警", content);
        }
    }

    private void checkJvm(ServerInfoVO.JvmInfo jvm) {
        if (jvm == null)
            return;
        if (jvm.getUsage() > JVM_THRESHOLD) {
            String content = String.format("JVM 内存堆栈占用过高！当前使用率: %.2f%% (已用: %s)。建议进行一键 GC 或排查内存泄漏。",
                    jvm.getUsage(), jvm.getUsed());
            triggerAlert("JVM_HIGH", "error", "☕ JVM 内存濒临溢出", content);
        }
    }

    private void checkDisks(java.util.List<ServerInfoVO.DiskInfo> disks) {
        if (disks == null)
            return;
        for (ServerInfoVO.DiskInfo disk : disks) {
            if (disk.getUsage() > DISK_THRESHOLD) {
                String content = String.format("磁盘 [%s] 空间即将耗尽！当前使用率: %.2f%% (可用: %s)。",
                        disk.getDirName(), disk.getUsage(), disk.getFree());
                triggerAlert("DISK_FULL", "critical", "💾 磁盘空间红线告警", content);
            }
        }
    }

    /**
     * 触发并插入告警 (带冷却期过滤)
     */
    private void triggerAlert(String type, String level, String title, String content) {
        // 查询该类型最新的一条未解决的同类型告警
        SysAlert lastAlert = sysAlertService.getOne(new LambdaQueryWrapper<SysAlert>()
                .eq(SysAlert::getAlertType, type)
                .orderByDesc(SysAlert::getCreateTime)
                .last("LIMIT 1"));

        if (lastAlert != null) {
            long lastTime = DateUtil.date(lastAlert.getCreateTime()).getTime();
            if (System.currentTimeMillis() - lastTime < COOLDOWN_MS) {
                // 在冷却期内，直接忽略，防止狂刷数据库和前端
                log.debug("告警类型 [{}] 还在冷却期内，跳过插表。", type);
                return;
            }
        }

        SysAlert newAlert = new SysAlert();
        newAlert.setAlertType(type);
        newAlert.setAlertLevel(level);
        newAlert.setAlertTitle(title);
        newAlert.setAlertContent(content);
        newAlert.setReadStatus(0);

        sysAlertService.save(newAlert);
        log.warn("🚨 触发新监控告警: {}", title);
    }
}
