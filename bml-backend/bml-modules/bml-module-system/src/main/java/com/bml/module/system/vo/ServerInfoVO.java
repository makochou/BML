package com.bml.module.system.vo;

import lombok.Data;

import java.util.List;

/**
 * 服务器硬件与环境监控实体类 (VO)
 *
 * @author BML Team
 */
@Data
public class ServerInfoVO {

    /** CPU信息 */
    private CpuInfo cpu;

    /** 内存信息 */
    private MemInfo mem;

    /** JVM信息 */
    private JvmInfo jvm;

    /** 系统环境参数 */
    private SysInfo sys;

    /** 网络速率信息 */
    private NetInfo net;

    /** 磁盘分区的监控列表 */
    private List<DiskInfo> disks;

    @Data
    public static class CpuInfo {
        private int cpuNum; // 核心数
        private double total; // CPU总使用率
        private double sys; // 系统使用率
        private double used; // 用户使用率
        private double wait; // 当前等待率
        private double free; // 当前空闲率
        private String cpuModel; // CPU型号名称

        // 增量指标：系统负载 (1, 5, 15 分钟)
        private double load1;
        private double load5;
        private double load15;
    }

    @Data
    public static class MemInfo {
        private double total; // 内存总量(GB)
        private double used; // 已用内存(GB)
        private double free; // 剩余内存(GB)
        private double usage; // 使用率 (%)
    }

    @Data
    public static class JvmInfo {
        private double total; // 当前JVM占用的内存总数(M)
        private double max; // JVM最大可用内存总数(M)
        private double free; // JVM空闲内存(M)
        private double used; // JVM已用内存(M)
        private double usage; // JVM使用率 (%)
        private String version; // JDK版本
        private String home; // JDK路径
        private String name; // JVM名称
        private String startTime; // JDK启动时间
        private String runTime; // JDK运行时间
    }

    @Data
    public static class SysInfo {
        private String computerName; // 服务器名称
        private String computerIp; // 服务器内网IP
        private String computerWanIp; // 服务器公网（外网）IP
        private String osName; // 操作系统
        private String osArch; // 系统架构
        private String userDir; // 项目运行路径

        // 增量指标：系统启动与运行时间
        private String bootTime; // 系统启动时间
        private String upTime; // 系统运行时长
    }

    @Data
    public static class DiskInfo {
        private String dirName; // 盘符路径
        private String sysTypeName; // 盘符类型（如 NTFS, ext4）
        private String typeName; // 文件系统挂载类型（如 local, network）
        private String total; // 总大小
        private String free; // 剩余大小
        private String used; // 已经使用量
        private double usage; // 对应的使用率 (%)

        // 增量指标：磁盘 IO 速率
        private String readSpeed; // 读取速度
        private String writeSpeed; // 写入速度
    }

    @Data
    public static class NetInfo {
        private String rxSpeed; // 当前下行网速（如: 1.5 MB/s）
        private String txSpeed; // 当前上行网速（如: 100 KB/s）
        private String totalRxBytes;// 总接收流量
        private String totalTxBytes;// 总发送流量

        // 增量指标：网络连接状态 (考虑到效率与跨平台兼容性，OSHI获取全量连接较慢，提供概览支持)
        private int tcpConnections; // TCP连接数
        private int udpConnections; // UDP连接数
    }
}
