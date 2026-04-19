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

    /**
     * 解析服务器公网 IP。
     * <p>
     * 策略：
     * <ol>
     *   <li>若功能未启用，直接返回"未启用"；</li>
     *   <li>若缓存未过期（1小时内），直接返回缓存值，避免频繁外网请求；</li>
     *   <li>依次尝试多个国内可用的公网 IP 查询服务（主用 + 备用），任意一个成功即返回；</li>
     *   <li>全部失败时返回"获取失败"，不再使用"离线或不可达"等歧义文案。</li>
     * </ol>
     * </p>
     *
     * <p>
     * 国内可用的公网 IP 查询服务列表（按优先级排序）：
     * <ul>
     *   <li>主配置：由 {@code bml.monitor.public-ip-provider-url} 指定（默认 ipinfo.io/ip）</li>
     *   <li>备用1：https://myip.ipip.net（国内 CDN，速度快）</li>
     *   <li>备用2：https://ip.3322.net（老牌国内服务）</li>
     *   <li>备用3：https://api.ipify.org（国际服务，作为最后兜底）</li>
     * </ul>
     * </p>
     *
     * @return 公网 IP 字符串，或状态描述文案
     */
    private String resolvePublicIp() {
        if (!publicIpEnabled) {
            return "未启用";
        }
        long currentTime = System.currentTimeMillis();
        // 缓存命中：1小时内不重复请求外网，避免频繁调用
        if (cachedPublicIp != null
                && !cachedPublicIp.equals("未启用")
                && !cachedPublicIp.equals("获取失败")
                && currentTime - lastPublicIpRefreshTime < PUBLIC_IP_REFRESH_INTERVAL_MS) {
            return cachedPublicIp;
        }

        // 备用查询地址列表：主配置优先，依次降级
        // 全部为国内可访问的服务，避免 checkip.amazonaws.com 境外超时问题
        String[] providers = {
                publicIpProviderUrl,          // 主配置（默认 https://ipinfo.io/ip）
                "https://myip.ipip.net",      // 备用1：国内 CDN，返回格式 "当前 IP：x.x.x.x"
                "https://ip.3322.net",        // 备用2：老牌国内服务，直接返回 IP
                "https://api.ipify.org"       // 备用3：国际兜底
        };

        for (String provider : providers) {
            try {
                // 超时设置为 3 秒，避免单个服务阻塞过久
                String raw = cn.hutool.http.HttpUtil.get(provider, 3000);
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                // myip.ipip.net 返回格式为 "当前 IP：1.2.3.4  来自于：..."，需要提取 IP 部分
                String ip = extractIpFromResponse(raw.trim());
                if (ip != null && !ip.isBlank()) {
                    cachedPublicIp = ip;
                    lastPublicIpRefreshTime = currentTime;
                    return cachedPublicIp;
                }
            } catch (Exception ex) {
                // 当前服务不可用，静默跳过，尝试下一个备用地址
            }
        }

        // 所有服务均不可用
        cachedPublicIp = "获取失败";
        lastPublicIpRefreshTime = currentTime;
        return cachedPublicIp;
    }

    /**
     * 从公网 IP 查询服务的响应文本中提取纯 IP 地址。
     * <p>
     * 不同服务返回格式不同：
     * <ul>
     *   <li>ipinfo.io / ip.3322.net / api.ipify.org：直接返回 IP，如 {@code 1.2.3.4}</li>
     *   <li>myip.ipip.net：返回 {@code 当前 IP：1.2.3.4  来自于：中国 北京}</li>
     * </ul>
     * 使用正则统一提取，兼容所有格式。
     * </p>
     *
     * @param raw 原始响应文本
     * @return 提取到的 IP 地址，若无法提取则返回 null
     */
    private String extractIpFromResponse(String raw) {
        // 使用正则匹配 IPv4 地址（兼容各种响应格式）
        java.util.regex.Pattern ipPattern = java.util.regex.Pattern.compile(
                "\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b"
        );
        java.util.regex.Matcher matcher = ipPattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
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
