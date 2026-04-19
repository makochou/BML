package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.vo.ServerInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务器监控治理控制器。
 * <p>
 * 提供服务器基础硬件（CPU、内存、磁盘）以及 JVM 运行时的实时监控数据。
 * 支持强制触发 JVM 垃圾回收（GC）以优化内存占用。
 * 所有接口均受到 Spring Security 权限框架保护。
 * </p>
 *
 * @author BML Team
 */
@Tag(name = "硬件监测", description = "提供硬件、环境监控及 JVM 治理功能")
@RestController
@RequestMapping("/system/monitor")
public class SysServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    public SysServerMonitorController(ServerMonitorService serverMonitorService) {
        this.serverMonitorService = serverMonitorService;
    }

    /**
     * 获取服务器全量监控信息。
     *
     * @return 统一响应载荷 {@link ServerInfoVO}
     */
    @Operation(summary = "全量抓取服务器监控数据")
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping("/server")
    public Result<ServerInfoVO> getServerInfo() {
        return Result.ok(serverMonitorService.getServerInfo());
    }

    /**
     * 强制发送 JVM 垃圾回收指令 (GC)。
     *
     * @return 状态消息提醒
     */
    @Operation(summary = "强制发送JVM垃圾回收指令(GC)")
    @PreAuthorize("@ss.hasPermi('monitor:server:gc')")
    @PostMapping("/gc")
    public Result<String> cleanJvmMemory() {
        System.gc(); // Suggests to the JVM that it expend effort toward recycling unused objects
        return Result.ok("JVM 垃圾回收 (GC) 指令已发送，物理内存正尝试释放...");
    }
}
