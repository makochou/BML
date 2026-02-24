package com.bml.module.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.vo.ServerInfoVO;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * 深入底层的高级系统参数提取服务
 */
@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    // CPU 与 网速 等待捕获时间用于计算负载浮动值
    private static final int OSHI_WAIT_SECOND = 1000;

    @Override
    public ServerInfoVO getServerInfo() {
        ServerInfoVO serverInfo = new ServerInfoVO();

        // 实例化 OSHI 的顶层查询对象
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        // 获取前置数据：CPU Ticks & Network Bytes
        CentralProcessor processor = hal.getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        long prevRxBytes = 0;
        long prevTxBytes = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            prevRxBytes += net.getBytesRecv();
            prevTxBytes += net.getBytesSent();
        }

        // 统一休眠等待 1 秒钟，计算增量差值
        Util.sleep(OSHI_WAIT_SECOND);

        // 获取后置数据并执行计算
        serverInfo.setCpu(getCpuInfo(processor, prevTicks));
        serverInfo.setNet(getNetInfo(networkIFs, prevRxBytes, prevTxBytes));

        // 获取其他无须强依赖等待时长的静态参数
        serverInfo.setMem(getMemInfo(hal.getMemory()));
        serverInfo.setJvm(getJvmInfo());
        serverInfo.setSys(getSysInfo(os));
        serverInfo.setDisks(getDiskInfo(os));

        return serverInfo;
    }

    private ServerInfoVO.CpuInfo getCpuInfo(CentralProcessor processor, long[] prevTicks) {
        ServerInfoVO.CpuInfo cpu = new ServerInfoVO.CpuInfo();
        cpu.setCpuModel(processor.getProcessorIdentifier().getName());
        cpu.setCpuNum(processor.getLogicalProcessorCount());

        long[] ticks = processor.getSystemCpuLoadTicks();

        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = Math.max(user + nice + cSys + idle + iowait + irq + softirq + steal, 1);

        cpu.setTotal(totalCpu);
        cpu.setSys(NumberUtil.round(cSys * 100.0 / totalCpu, 2).doubleValue());
        cpu.setUsed(NumberUtil.round(user * 100.0 / totalCpu, 2).doubleValue());
        cpu.setWait(NumberUtil.round(iowait * 100.0 / totalCpu, 2).doubleValue());
        cpu.setFree(NumberUtil.round(idle * 100.0 / totalCpu, 2).doubleValue());

        return cpu;
    }

    private ServerInfoVO.NetInfo getNetInfo(List<NetworkIF> networkIFs, long prevRxBytes, long prevTxBytes) {
        ServerInfoVO.NetInfo net = new ServerInfoVO.NetInfo();
        long currentRxBytes = 0;
        long currentTxBytes = 0;

        for (NetworkIF nif : networkIFs) {
            nif.updateAttributes();
            currentRxBytes += nif.getBytesRecv();
            currentTxBytes += nif.getBytesSent();
        }

        long rxSpeedBytes = Math.max(currentRxBytes - prevRxBytes, 0);
        long txSpeedBytes = Math.max(currentTxBytes - prevTxBytes, 0);

        net.setRxSpeed(formatTrafficSpeed(rxSpeedBytes));
        net.setTxSpeed(formatTrafficSpeed(txSpeedBytes));
        net.setTotalRxBytes(formatTrafficSize(currentRxBytes));
        net.setTotalTxBytes(formatTrafficSize(currentTxBytes));

        return net;
    }

    private ServerInfoVO.MemInfo getMemInfo(GlobalMemory memory) {
        ServerInfoVO.MemInfo mem = new ServerInfoVO.MemInfo();
        double total = memory.getTotal() / (1024.0 * 1024.0 * 1024.0);
        double available = memory.getAvailable() / (1024.0 * 1024.0 * 1024.0);
        double used = total - available;

        mem.setTotal(NumberUtil.round(total, 2).doubleValue());
        mem.setUsed(NumberUtil.round(used, 2).doubleValue());
        mem.setFree(NumberUtil.round(available, 2).doubleValue());
        if (total == 0) {
            mem.setUsage(0.0);
        } else {
            mem.setUsage(NumberUtil.round(used * 100.0 / total, 2).doubleValue());
        }
        return mem;
    }

    private ServerInfoVO.JvmInfo getJvmInfo() {
        ServerInfoVO.JvmInfo jvm = new ServerInfoVO.JvmInfo();
        Properties props = System.getProperties();

        long time = ManagementFactory.getRuntimeMXBean().getStartTime();

        double total = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double max = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
        double free = Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0);
        double used = total - free;

        jvm.setTotal(NumberUtil.round(total, 2).doubleValue());
        jvm.setMax(NumberUtil.round(max, 2).doubleValue());
        jvm.setFree(NumberUtil.round(free, 2).doubleValue());
        jvm.setUsed(NumberUtil.round(used, 2).doubleValue());
        if (total == 0) {
            jvm.setUsage(0.0);
        } else {
            jvm.setUsage(NumberUtil.round(used * 100.0 / total, 2).doubleValue());
        }

        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
        jvm.setName(ManagementFactory.getRuntimeMXBean().getVmName());
        jvm.setStartTime(DateUtil.formatDateTime(DateUtil.date(time)));
        jvm.setRunTime(DateUtil.formatBetween(DateUtil.date(time), DateUtil.date()));

        return jvm;
    }

    private ServerInfoVO.SysInfo getSysInfo(OperatingSystem os) {
        ServerInfoVO.SysInfo sys = new ServerInfoVO.SysInfo();
        Properties props = System.getProperties();
        try {
            sys.setComputerName(InetAddress.getLocalHost().getHostName());
            sys.setComputerIp(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            sys.setComputerName("Unknown");
            sys.setComputerIp("127.0.0.1");
        }

        // 尝试捕获公网 IP
        try {
            String wanIp = cn.hutool.http.HttpUtil.get("http://checkip.amazonaws.com", 2000);
            if (wanIp != null) {
                sys.setComputerWanIp(wanIp.trim());
            } else {
                sys.setComputerWanIp("获取失败");
            }
        } catch (Exception e) {
            sys.setComputerWanIp("离线或隔离");
        }
        sys.setOsName(os.getFamily() + " " + os.getVersionInfo().getVersion());
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));

        return sys;
    }

    private List<ServerInfoVO.DiskInfo> getDiskInfo(OperatingSystem os) {
        List<ServerInfoVO.DiskInfo> list = new LinkedList<>();
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray) {
            long free = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            long used = total - free;

            ServerInfoVO.DiskInfo disk = new ServerInfoVO.DiskInfo();
            disk.setDirName(fs.getMount());
            disk.setSysTypeName(fs.getName());
            disk.setTypeName(fs.getType());
            disk.setTotal(formatDiskSize(total));
            disk.setFree(formatDiskSize(free));
            disk.setUsed(formatDiskSize(used));
            disk.setUsage(total == 0 ? 0 : NumberUtil.round(used * 100.0 / total, 2).doubleValue());
            list.add(disk);
        }
        return list;
    }

    private String formatDiskSize(long size) {
        return NumberUtil.round(size / (1024.0 * 1024.0 * 1024.0), 2) + " GB";
    }

    private String formatTrafficSpeed(long bytes) {
        if (bytes < 1024) {
            return bytes + " B/s";
        } else if (bytes < 1024 * 1024) {
            return NumberUtil.round(bytes / 1024.0, 2) + " KB/s";
        } else if (bytes < 1024 * 1024 * 1024) {
            return NumberUtil.round(bytes / (1024.0 * 1024.0), 2) + " MB/s";
        } else {
            return NumberUtil.round(bytes / (1024.0 * 1024.0 * 1024.0), 2) + " GB/s";
        }
    }

    private String formatTrafficSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return NumberUtil.round(bytes / 1024.0, 2) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return NumberUtil.round(bytes / (1024.0 * 1024.0), 2) + " MB";
        } else {
            return NumberUtil.round(bytes / (1024.0 * 1024.0 * 1024.0), 2) + " GB";
        }
    }
}
