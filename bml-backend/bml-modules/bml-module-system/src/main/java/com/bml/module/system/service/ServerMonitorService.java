package com.bml.module.system.service;

import com.bml.module.system.vo.ServerInfoVO;

/**
 * 物理服务器硬件与应用实时监控服务接口
 *
 * @author BML Team
 */
public interface ServerMonitorService {

    /**
     * 拉取当前物理机操作系统、硬件 CPU 和 JVM 的全面运行态资源快照
     *
     * @return 详尽的资源快照实体
     */
    ServerInfoVO getServerInfo();
}
