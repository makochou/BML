package com.bml.module.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.bml.module.system.security.monitor.ActiveUserTracker;
import com.bml.module.system.service.ServerMonitorService;
import com.bml.module.system.vo.ServerInfoVO;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    private static final long DEFAULT_SAMPLE_INTERVAL_MS = 3000L;
    private static final long PUBLIC_IP_REFRESH_INTERVAL_MS = 60 * 60 * 1000L;

    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    private volatile ServerInfoVO cachedServerInfo;
    private volatile long[] previousCpuTicks;
    private volatile long previousRxBytes;
    private volatile long previousTxBytes;
    private volatile long previousDiskReadBytes;
    private volatile long previousDiskWriteBytes;
    private volatile long lastSampleTime;
    private volatile String cachedPublicIp = "未启用";
    private volatile long lastPublicIpRefreshTime;

    @Autowired
    private ActiveUserTracker activeUserTracker;

    @Value("${bml.monitor.refresh-interval-ms:3000}")
    private long refreshIntervalMs;

    @Value("${bml.monitor.public-ip-enabled:false}")
    private boolean publicIpEnabled;

    @Value("${bml.monitor.public-ip-provider-url:http://checkip.amazonaws.com}")
    private String publicIpProviderUrl;

    @PostConstruct
    public void initializeSnapshot() {
        refreshSnapshot();
    }

    @Scheduled(fixedDelayString = "${bml.monitor.refresh-interval-ms:3000}")
    public synchronized void refreshSnapshot() {
        CentralProcessor processor = hardware.getProcessor();
        List<NetworkIF> networkIFs = hardware.getNetworkIFs();
        List<HWDiskStore> diskStores = hardware.getDiskStores();

        long currentTime = System.currentTimeMillis();
        long[] currentCpuTicks = processor.getSystemCpuLoadTicks();
        long currentRxBytes = sumReceivedBytes(networkIFs);
        long currentTxBytes = sumSentBytes(networkIFs);
        long currentDiskReadBytes = sumDiskReadBytes(diskStores);
        long currentDiskWriteBytes = sumDiskWriteBytes(diskStores);

        ServerInfoVO serverInfo = new ServerInfoVO();
        serverInfo.setCpu(buildCpuInfo(processor, currentCpuTicks));
        serverInfo.setNet(buildNetInfo(networkIFs, currentRxBytes, currentTxBytes));
        serverInfo.setMem(buildMemInfo(hardware.getMemory()));
        serverInfo.setJvm(buildJvmInfo());
        serverInfo.setSys(buildSysInfo());
        serverInfo.setDisks(buildDiskInfo(currentDiskReadBytes, currentDiskWriteBytes));

        previousCpuTicks = currentCpuTicks;
        previousRxBytes = currentRxBytes;
        previousTxBytes = currentTxBytes;
        previousDiskReadBytes = currentDiskReadBytes;
        previousDiskWriteBytes = currentDiskWriteBytes;
        lastSampleTime = currentTime;
        cachedServerInfo = serverInfo;
    }

    @Override
    public ServerInfoVO getServerInfo() {
        if (cachedServerInfo == null) {
            refreshSnapshot();
        }
        return cachedServerInfo;
    }

    private ServerInfoVO.CpuInfo buildCpuInfo(CentralProcessor processor, long[] currentTicks) {
        ServerInfoVO.CpuInfo cpu = new ServerInfoVO.CpuInfo();
        cpu.setCpuModel(processor.getProcessorIdentifier().getName());
        cpu.setCpuNum(processor.getLogicalProcessorCount());

        long[] baseTicks = previousCpuTicks == null ? currentTicks : previousCpuTicks;
        long nice = currentTicks[CentralProcessor.TickType.NICE.getIndex()]
                - baseTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = currentTicks[CentralProcessor.TickType.IRQ.getIndex()]
                - baseTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = currentTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - baseTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = currentTicks[CentralProcessor.TickType.STEAL.getIndex()]
                - baseTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long system = currentTicks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - baseTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = currentTicks[CentralProcessor.TickType.USER.getIndex()]
                - baseTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = currentTicks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - baseTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = currentTicks[CentralProcessor.TickType.IDLE.getIndex()]
                - baseTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = Math.max(user + nice + system + idle + iowait + irq + softirq + steal, 1);

        cpu.setTotal(totalCpu);
        cpu.setSys(NumberUtil.round(system * 100.0 / totalCpu, 2).doubleValue());
        cpu.setUsed(NumberUtil.round(user * 100.0 / totalCpu, 2).doubleValue());
        cpu.setWait(NumberUtil.round(iowait * 100.0 / totalCpu, 2).doubleValue());
        cpu.setFree(NumberUtil.round(idle * 100.0 / totalCpu, 2).doubleValue());

        double[] loadAverage = processor.getSystemLoadAverage(3);
        cpu.setLoad1(loadAverage[0] < 0 ? 0.0 : NumberUtil.round(loadAverage[0], 2).doubleValue());
        cpu.setLoad5(loadAverage[1] < 0 ? 0.0 : NumberUtil.round(loadAverage[1], 2).doubleValue());
        cpu.setLoad15(loadAverage[2] < 0 ? 0.0 : NumberUtil.round(loadAverage[2], 2).doubleValue());
        return cpu;
    }

    private ServerInfoVO.NetInfo buildNetInfo(List<NetworkIF> networkIFs, long currentRxBytes, long currentTxBytes) {
        ServerInfoVO.NetInfo net = new ServerInfoVO.NetInfo();
        double elapsedSeconds = getElapsedSeconds();

        long rxSpeedBytes = previousRxBytes == 0 ? 0 : Math.max(currentRxBytes - previousRxBytes, 0);
        long txSpeedBytes = previousTxBytes == 0 ? 0 : Math.max(currentTxBytes - previousTxBytes, 0);

        net.setRxSpeed(formatTrafficSpeed((long) (rxSpeedBytes / elapsedSeconds)));
        net.setTxSpeed(formatTrafficSpeed((long) (txSpeedBytes / elapsedSeconds)));
        net.setTotalRxBytes(formatTrafficSize(currentRxBytes));
        net.setTotalTxBytes(formatTrafficSize(currentTxBytes));

        try {
            int activeUsers = activeUserTracker != null ? activeUserTracker.getActiveUserCount() : 0;
            net.setTcpConnections(activeUsers);
            oshi.software.os.InternetProtocolStats ipStats = operatingSystem.getInternetProtocolStats();
            long udpConnections = ipStats.getConnections().stream()
                    .filter(connection -> connection.getType() != null
                            && connection.getType().toLowerCase(Locale.ROOT).startsWith("udp"))
                    .count();
            net.setUdpConnections((int) Math.min(udpConnections, Integer.MAX_VALUE));
        } catch (Exception ex) {
            net.setTcpConnections(0);
            net.setUdpConnections(0);
        }

        return net;
    }

    private ServerInfoVO.MemInfo buildMemInfo(GlobalMemory memory) {
        ServerInfoVO.MemInfo mem = new ServerInfoVO.MemInfo();
        double total = memory.getTotal() / (1024.0 * 1024.0 * 1024.0);
        double available = memory.getAvailable() / (1024.0 * 1024.0 * 1024.0);
        double used = total - available;

        mem.setTotal(NumberUtil.round(total, 2).doubleValue());
        mem.setUsed(NumberUtil.round(used, 2).doubleValue());
        mem.setFree(NumberUtil.round(available, 2).doubleValue());
        mem.setUsage(total == 0 ? 0.0 : NumberUtil.round(used * 100.0 / total, 2).doubleValue());
        return mem;
    }

    private ServerInfoVO.JvmInfo buildJvmInfo() {
        ServerInfoVO.JvmInfo jvm = new ServerInfoVO.JvmInfo();
        Properties props = System.getProperties();
        long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();

        double total = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double max = Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0);
        double free = Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0);
        double used = total - free;

        jvm.setTotal(NumberUtil.round(total, 2).doubleValue());
        jvm.setMax(NumberUtil.round(max, 2).doubleValue());
        jvm.setFree(NumberUtil.round(free, 2).doubleValue());
        jvm.setUsed(NumberUtil.round(used, 2).doubleValue());
        jvm.setUsage(total == 0 ? 0.0 : NumberUtil.round(used * 100.0 / total, 2).doubleValue());
        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
        jvm.setName(ManagementFactory.getRuntimeMXBean().getVmName());
        jvm.setStartTime(DateUtil.formatDateTime(DateUtil.date(startTime)));
        jvm.setRunTime(DateUtil.formatBetween(DateUtil.date(startTime), DateUtil.date()));
        return jvm;
    }

    private ServerInfoVO.SysInfo buildSysInfo() {
        ServerInfoVO.SysInfo sys = new ServerInfoVO.SysInfo();
        Properties props = System.getProperties();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            sys.setComputerName(localHost.getHostName());
            sys.setComputerIp(localHost.getHostAddress());
        } catch (UnknownHostException ex) {
            sys.setComputerName("Unknown");
            sys.setComputerIp("127.0.0.1");
        }

        sys.setComputerWanIp(resolvePublicIp());
        sys.setOsName(operatingSystem.getFamily() + " " + operatingSystem.getVersionInfo().getVersion());
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));

        long bootTimeMillis = operatingSystem.getSystemBootTime() * 1000L;
        sys.setBootTime(DateUtil.formatDateTime(DateUtil.date(bootTimeMillis)));
        sys.setUpTime(DateUtil.formatBetween(DateUtil.date(bootTimeMillis), DateUtil.date()));
        return sys;
    }

    private List<ServerInfoVO.DiskInfo> buildDiskInfo(long currentDiskReadBytes, long currentDiskWriteBytes) {
        List<ServerInfoVO.DiskInfo> list = new LinkedList<>();
        double elapsedSeconds = getElapsedSeconds();
        long globalReadSpeed = previousDiskReadBytes == 0 ? 0 : Math.max(currentDiskReadBytes - previousDiskReadBytes, 0);
        long globalWriteSpeed = previousDiskWriteBytes == 0 ? 0
                : Math.max(currentDiskWriteBytes - previousDiskWriteBytes, 0);

        FileSystem fileSystem = operatingSystem.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        for (int index = 0; index < fsArray.size(); index++) {
            OSFileStore fs = fsArray.get(index);
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

            if (index == 0) {
                disk.setReadSpeed(formatTrafficSpeed((long) (globalReadSpeed / elapsedSeconds)));
                disk.setWriteSpeed(formatTrafficSpeed((long) (globalWriteSpeed / elapsedSeconds)));
            } else {
                disk.setReadSpeed("-");
                disk.setWriteSpeed("-");
            }
            list.add(disk);
        }
        return list;
    }

    private double getElapsedSeconds() {
        long sampleInterval = refreshIntervalMs > 0 ? refreshIntervalMs : DEFAULT_SAMPLE_INTERVAL_MS;
        if (lastSampleTime == 0L) {
            return sampleInterval / 1000.0;
        }
        return Math.max((System.currentTimeMillis() - lastSampleTime) / 1000.0, 1.0);
    }

    private long sumReceivedBytes(List<NetworkIF> networkIFs) {
        long total = 0;
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            total += networkIF.getBytesRecv();
        }
        return total;
    }

    private long sumSentBytes(List<NetworkIF> networkIFs) {
        long total = 0;
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            total += networkIF.getBytesSent();
        }
        return total;
    }

    private long sumDiskReadBytes(List<HWDiskStore> diskStores) {
        long total = 0;
        for (HWDiskStore diskStore : diskStores) {
            diskStore.updateAttributes();
            total += diskStore.getReadBytes();
        }
        return total;
    }

    private long sumDiskWriteBytes(List<HWDiskStore> diskStores) {
        long total = 0;
        for (HWDiskStore diskStore : diskStores) {
            diskStore.updateAttributes();
            total += diskStore.getWriteBytes();
        }
        return total;
    }

    private String resolvePublicIp() {
        if (!publicIpEnabled) {
            return "未启用";
        }
        long currentTime = System.currentTimeMillis();
        if (cachedPublicIp != null && currentTime - lastPublicIpRefreshTime < PUBLIC_IP_REFRESH_INTERVAL_MS) {
            return cachedPublicIp;
        }
        try {
            String wanIp = cn.hutool.http.HttpUtil.get(publicIpProviderUrl, 2000);
            cachedPublicIp = wanIp == null || wanIp.isBlank() ? "获取失败" : wanIp.trim();
        } catch (Exception ex) {
            cachedPublicIp = "离线或不可达";
        }
        lastPublicIpRefreshTime = currentTime;
        return cachedPublicIp;
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
