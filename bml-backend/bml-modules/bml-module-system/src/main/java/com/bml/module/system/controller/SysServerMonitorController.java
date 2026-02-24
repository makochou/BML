package com.bml.module.system.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.vo.ServerInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物理服务器硬件与环境监控主控入口
 * 天然被全局拦截器保护、强鉴权验证
 *
 * @author BML Team
 */
@Tag(name = "服务器监控资源获取接口")
@RestController
@RequestMapping("/system/monitor")
public class SysServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    public SysServerMonitorController(ServerMonitorService serverMonitorService) {
        this.serverMonitorService = serverMonitorService;
    }

    @Operation(summary = "全量抓取服务器、JVM、磁盘最新硬件物理探针态")
    // @PreAuthorize("@ss.hasPermi('monitor:server:list')") // 如需单独为大屏角色设定强权限可放开
    @GetMapping("/server")
    public Result<ServerInfoVO> getServerInfo() {
        return Result.ok(serverMonitorService.getServerInfo());
    }
}
