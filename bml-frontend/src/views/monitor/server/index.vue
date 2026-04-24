
<template>
  <div class="monitor-shell">
    <div class="monitor-aurora"></div>

    <!-- ══════════════════════════════════════════════════════
         顶部信息栏：标题 / 网速 / IP（单行水平排列）
         ══════════════════════════════════════════════════════ -->
    <div class="monitor-header-sc">
      <div class="header-left">
        <div class="pulse-indicator"></div>
        <div class="title-meta">
          <h1>服务器运维驾驶舱</h1>
          <p>系统资源实时观测矩阵 · 自动化探针已就绪</p>
        </div>
      </div>

      <div class="header-center">
        <div class="traffic-rail">
          <div class="traffic-node">
            <span class="label">上行 (UP)</span>
            <span class="value up"><icon-arrow-rise />{{ serverInfo?.net?.txSpeed || '0 B/s' }}</span>
          </div>
          <div class="node-gap"></div>
          <div class="traffic-node">
            <span class="label">下行 (DOWN)</span>
            <span class="value down"><icon-arrow-fall />{{ serverInfo?.net?.rxSpeed || '0 B/s' }}</span>
          </div>
          <div class="node-gap"></div>
          <div class="traffic-node">
            <span class="label">累计收</span>
            <span class="value neutral">{{ serverInfo?.net?.totalRxBytes || '0 B' }}</span>
          </div>
          <div class="node-gap"></div>
          <div class="traffic-node">
            <span class="label">累计发</span>
            <span class="value neutral">{{ serverInfo?.net?.totalTxBytes || '0 B' }}</span>
          </div>
        </div>
      </div>

      <div class="header-right">
        <div class="ip-console">
          <div class="ip-item">
            <span class="chip">内网 IP</span>
            <span class="addr">{{ serverInfo?.sys?.computerIp || '---.---.---.---' }}</span>
          </div>
          <div class="ip-divider"></div>
          <div class="ip-item">
            <span class="chip pub">外网 IP</span>
            <span class="addr">{{ serverInfo?.sys?.computerWanIp || '获取中...' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         主内容三栏网格
         左侧(300px)：系统环境 + JVM
         中间(1fr)  ：核心算力（缩小版仪表盘 + GC按钮）
         右侧(300px)：磁盘阵列 + 网络状态
         ══════════════════════════════════════════════════════ -->
    <div class="monitor-viewport">
      <div class="viewport-grid">

        <!-- ══ 左侧栏 ══ -->
        <div class="grid-side">

          <!-- 基础运行环境：flex: 2，高度约占左侧 40%，紧凑展示系统信息 -->
          <div class="lv-card side-card-env entrance-anim-1">
            <div class="lv-card__accent lv-card__accent--blue"></div>
            <div class="card-title-sc">
              <icon-computer />
              <span>基础运行环境</span>
            </div>
            <div class="info-list">
              <div class="info-row">
                <span class="info-key">主机名</span>
                <strong class="info-val">{{ serverInfo?.sys?.computerName || 'N/A' }}</strong>
              </div>
              <div class="info-row">
                <span class="info-key">操作系统</span>
                <strong class="info-val">{{ serverInfo?.sys?.osName || 'N/A' }}</strong>
              </div>
              <div class="info-row">
                <span class="info-key">系统架构</span>
                <strong class="info-val">{{ serverInfo?.sys?.osArch || 'N/A' }}</strong>
              </div>
              <div class="info-row">
                <span class="info-key">工作目录</span>
                <strong class="info-val text-ellipsis" :title="serverInfo?.sys?.userDir">
                  {{ serverInfo?.sys?.userDir || 'N/A' }}
                </strong>
              </div>
              <div class="info-row">
                <span class="info-key">系统启动</span>
                <strong class="info-val">{{ serverInfo?.sys?.bootTime || 'N/A' }}</strong>
              </div>
              <div class="info-row">
                <span class="info-key">持续运行</span>
                <strong class="info-val tint-blue">{{ serverInfo?.sys?.upTime || 'N/A' }}</strong>
              </div>
            </div>
          </div>

          <!-- JVM 运行引擎：flex: 3，高度约占左侧 60%，展示更多 JVM 内存与时间信息 -->
          <div class="lv-card side-card-jvm entrance-anim-2">
            <div class="lv-card__accent lv-card__accent--teal"></div>
            <div class="card-title-sc">
              <icon-settings />
              <span>虚拟机运行引擎 (JVM)</span>
            </div>
            <div class="jvm-tags">
              <div class="spec-pill"><span class="tag java">JAVA</span>{{ serverInfo?.jvm?.version || 'N/A' }}</div>
              <div class="spec-pill"><span class="tag jvm">JVM</span>{{ serverInfo?.jvm?.name || 'N/A' }}</div>
            </div>
            <div class="jvm-mem-grid">
              <div class="jvm-mem-item">
                <span class="m-lbl">已用内存</span>
                <span class="m-val tint-purple">{{ serverInfo?.jvm?.used?.toFixed(1) || 0 }} MB</span>
              </div>
              <div class="jvm-mem-item">
                <span class="m-lbl">空闲内存</span>
                <span class="m-val tint-green">{{ serverInfo?.jvm?.free?.toFixed(1) || 0 }} MB</span>
              </div>
              <div class="jvm-mem-item">
                <span class="m-lbl">已分配</span>
                <span class="m-val">{{ serverInfo?.jvm?.total?.toFixed(1) || 0 }} MB</span>
              </div>
              <div class="jvm-mem-item">
                <span class="m-lbl">最大堆</span>
                <span class="m-val">{{ serverInfo?.jvm?.max?.toFixed(1) || 0 }} MB</span>
              </div>
            </div>
            <div class="jvm-time-list">
              <div class="jvm-time-row">
                <span class="t-key">JVM 启动于</span>
                <span class="t-val">{{ serverInfo?.jvm?.startTime || 'N/A' }}</span>
              </div>
              <div class="jvm-time-row">
                <span class="t-key">已持续运行</span>
                <span class="t-val tint-blue">{{ serverInfo?.jvm?.runTime || 'N/A' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- ══ 中间主卡片 ══ -->
        <div class="grid-main">
          <div class="lv-card main-card entrance-anim-scale">
            <div class="lv-card__accent lv-card__accent--gold"></div>

            <!-- 卡片头：标题 + 负载系数 -->
            <div class="main-head">
              <div class="main-title-group">
                <h2>核心算力资源观测</h2>
                <p>实时采样 · 3 秒刷新</p>
              </div>
              <div class="load-pills">
                <div class="load-pill">
                  <span class="lp-label">1min</span>
                  <span class="lp-val">{{ serverInfo?.cpu?.load1 ?? 0 }}</span>
                </div>
                <div class="load-pill">
                  <span class="lp-label">5min</span>
                  <span class="lp-val">{{ serverInfo?.cpu?.load5 ?? 0 }}</span>
                </div>
                <div class="load-pill">
                  <span class="lp-label">15min</span>
                  <span class="lp-val">{{ serverInfo?.cpu?.load15 ?? 0 }}</span>
                </div>
              </div>
            </div>

            <!-- 三环仪表盘（紧凑版） -->
            <div class="gauges-row">
              <!-- CPU 环 -->
              <div class="gauge-cell">
                <div class="ring ring-sm" :style="getGaugeStyle(serverInfo?.cpu?.used, '#165dff')">
                  <div class="ring-inner">
                    <span class="ring-pct">{{ serverInfo?.cpu?.used ?? 0 }}%</span>
                    <span class="ring-tag">CPU</span>
                  </div>
                </div>
                <span class="gauge-label">处理器负荷</span>
              </div>

              <!-- 内存环（主环，稍大） -->
              <div class="gauge-cell gauge-hero">
                <div class="ring ring-lg" :style="getGaugeStyle(serverInfo?.mem?.usage, '#14c9c9')">
                  <div class="ring-inner">
                    <span class="ring-pct">{{ serverInfo?.mem?.usage ?? 0 }}%</span>
                    <span class="ring-tag">MEM</span>
                  </div>
                </div>
                <span class="gauge-label">物理内存占用</span>
                <div class="mem-detail-row">
                  <span>{{ serverInfo?.mem?.used?.toFixed(1) ?? 0 }}GB 已用</span>
                  <span class="dot-sep">·</span>
                  <span>{{ serverInfo?.mem?.free?.toFixed(1) ?? 0 }}GB 空闲</span>
                  <span class="dot-sep">·</span>
                  <span>共 {{ serverInfo?.mem?.total?.toFixed(1) ?? 0 }}GB</span>
                </div>
              </div>

              <!-- JVM 环 -->
              <div class="gauge-cell">
                <div class="ring ring-sm" :style="getGaugeStyle(serverInfo?.jvm?.usage, '#722ed1')">
                  <div class="ring-inner">
                    <span class="ring-pct">{{ serverInfo?.jvm?.usage ?? 0 }}%</span>
                    <span class="ring-tag">JVM</span>
                  </div>
                </div>
                <span class="gauge-label">虚拟机内存</span>
              </div>
            </div>

            <!-- 底部工具栏：GC 清理内存按钮 -->
            <div class="main-footer">
              <div class="footer-tip">
                <icon-info-circle class="tip-icon" />
                <span>JVM 堆内存已用 <strong>{{ serverInfo?.jvm?.used?.toFixed(1) || 0 }} MB</strong>，空闲 <strong>{{ serverInfo?.jvm?.free?.toFixed(1) || 0 }} MB</strong></span>
              </div>
              <a-button
                type="primary"
                size="small"
                class="gc-btn"
                :loading="gcLoading"
                @click="handleManualGC"
              >
                <template #icon><icon-refresh /></template>
                清理 JVM 内存 (GC)
              </a-button>
            </div>

            <!-- CPU 进度条区 -->
            <div class="cpu-detail-box">
              <div class="cpu-model-line">
                <icon-thunderbolt class="cpu-icon" />
                <span>{{ serverInfo?.cpu?.cpuModel || 'N/A' }}</span>
                <span class="cpu-core-badge">{{ serverInfo?.cpu?.cpuNum ?? 0 }} 核</span>
              </div>
              <div class="prog-list">
                <div class="prog-item">
                  <span class="prog-label">内核消耗</span>
                  <div class="prog-track"><div class="prog-fill fill-sys" :style="{ width: clamp(serverInfo?.cpu?.sys) + '%' }"></div></div>
                  <span class="prog-val">{{ serverInfo?.cpu?.sys ?? 0 }}%</span>
                </div>
                <div class="prog-item">
                  <span class="prog-label">用户进程</span>
                  <div class="prog-track"><div class="prog-fill fill-user" :style="{ width: clamp(serverInfo?.cpu?.used) + '%' }"></div></div>
                  <span class="prog-val">{{ serverInfo?.cpu?.used ?? 0 }}%</span>
                </div>
                <div class="prog-item">
                  <span class="prog-label">IO 等待</span>
                  <div class="prog-track"><div class="prog-fill fill-io" :style="{ width: clamp(serverInfo?.cpu?.wait) + '%' }"></div></div>
                  <span class="prog-val">{{ serverInfo?.cpu?.wait ?? 0 }}%</span>
                </div>
                <div class="prog-item">
                  <span class="prog-label">空闲</span>
                  <div class="prog-track"><div class="prog-fill fill-idle" :style="{ width: clamp(serverInfo?.cpu?.free) + '%' }"></div></div>
                  <span class="prog-val">{{ serverInfo?.cpu?.free ?? 0 }}%</span>
                </div>
              </div>
            </div>

          </div>
        </div>

        <!-- ══ 右侧栏 ══ -->
        <div class="grid-side">

          <!-- 网络连接状态（调至上方） -->
          <div class="lv-card net-card entrance-anim-3">
            <div class="lv-card__accent lv-card__accent--purple"></div>
            <div class="card-title-sc">
              <icon-wifi />
              <span>网络连接状态</span>
            </div>
            <div class="net-grid">
              <div class="net-item">
                <div class="net-icon-wrap tcp"><icon-link /></div>
                <div class="net-text">
                  <span class="net-num">{{ serverInfo?.net?.tcpConnections ?? 0 }}</span>
                  <span class="net-desc">活跃会话 (TCP)</span>
                </div>
              </div>
              <div class="net-item">
                <div class="net-icon-wrap udp"><icon-send /></div>
                <div class="net-text">
                  <span class="net-num">{{ serverInfo?.net?.udpConnections ?? 0 }}</span>
                  <span class="net-desc">UDP 连接数</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 存储阵列（调至下方） -->
          <div class="lv-card side-card entrance-anim-4">
            <div class="lv-card__accent lv-card__accent--blue"></div>
            <div class="card-title-sc">
              <icon-common />
              <span>存储阵列矩阵监控</span>
            </div>
            <div class="disk-scroller">
              <div v-for="disk in serverInfo?.disks" :key="disk.dirName" class="disk-unit">
                <div class="disk-meta">
                  <span class="disk-mount">{{ disk.dirName }}</span>
                  <span class="disk-type-tag">{{ disk.sysTypeName || disk.typeName }}</span>
                </div>
                <div class="disk-bar-wrap">
                  <div class="disk-bar-fill" :style="{ width: disk.usage + '%', background: getDiskColor(disk.usage) }"></div>
                </div>
                <div class="disk-stat">
                  <span>{{ disk.used }} / {{ disk.total }}</span>
                  <strong :class="getDiskTextClass(disk.usage)">{{ disk.usage }}%</strong>
                </div>
                <div v-if="disk.readSpeed && disk.readSpeed !== '-'" class="disk-io">
                  <span class="io-chip read"><icon-arrow-rise />{{ disk.readSpeed }}</span>
                  <span class="io-chip write"><icon-arrow-fall />{{ disk.writeSpeed }}</span>
                </div>
              </div>
              <div v-if="!serverInfo?.disks?.length" class="disk-empty">暂无磁盘数据</div>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 主机监控页面
 *
 * 重要说明：
 *   defineOptions({ name: 'ServerMonitor' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'ServerMonitor' });

import { ref, onMounted, onUnmounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconComputer, IconSettings, IconCommon, IconRefresh,
  IconArrowRise, IconArrowFall, IconWifi, IconLink, IconSend,
  IconThunderbolt, IconInfoCircle
} from '@arco-design/web-vue/es/icon';
/**
 * 使用项目统一的 request 实例（已内置 Authorization Token 注入 + 401 自动刷新）。
 * 禁止直接使用裸 axios，否则请求不携带 Token 会被 Spring Security 拦截返回 401。
 */
import request from '@/utils/request';

// ─────────────────────────────────────────────
// 类型定义：与后端 ServerInfoVO 完整字段一一对应
// ─────────────────────────────────────────────
interface ServerInfoVO {
  sys?: {
    computerName: string;
    computerIp: string;
    computerWanIp: string;
    osName: string;
    osArch: string;
    userDir: string;
    bootTime: string;
    upTime: string;
  };
  cpu?: {
    cpuNum: number;
    used: number;
    sys: number;
    wait: number;
    free: number;
    cpuModel: string;
    load1: number;
    load5: number;
    load15: number;
  };
  mem?: {
    usage: number;
    total: number;
    used: number;
    free: number;
  };
  jvm?: {
    usage: number;
    total: number;
    max: number;
    free: number;
    used: number;
    version: string;
    home: string;
    name: string;
    startTime: string;
    runTime: string;
  };
  disks?: Array<{
    dirName: string;
    sysTypeName: string;
    typeName: string;
    total: string;
    free: string;
    used: string;
    usage: number;
    readSpeed: string;
    writeSpeed: string;
  }>;
  net?: {
    txSpeed: string;
    rxSpeed: string;
    totalRxBytes: string;
    totalTxBytes: string;
    tcpConnections: number;
    udpConnections: number;
  };
}

// ─────────────────────────────────────────────
// 响应式状态
// ─────────────────────────────────────────────
const serverInfo = ref<ServerInfoVO | null>(null);
const gcLoading = ref(false);
/** 轮询定时器句柄，组件卸载时必须清除，防止内存泄漏 */
const refreshTimer = ref<ReturnType<typeof setInterval> | null>(null);

// ─────────────────────────────────────────────
// 数据拉取：每 3 秒轮询，与后端 @Scheduled 刷新间隔保持一致
// ─────────────────────────────────────────────
const pullServerStats = async () => {
  try {
    const res = await request.get('/system/monitor/server') as any;
    serverInfo.value = res?.data ?? null;
  } catch (err) {
    console.error('[ServerMonitor] 无法同步服务器实时探针数据', err);
  }
};

// ─────────────────────────────────────────────
// 手动触发 JVM GC（清理内存）
// 后端接口：POST /api/system/monitor/gc
// 权限标识：monitor:server:gc
// ─────────────────────────────────────────────
const handleManualGC = async () => {
  gcLoading.value = true;
  try {
    const res = await request.post('/system/monitor/gc') as any;
    Message.success(res?.data ?? 'JVM 垃圾回收指令已成功下达');
  } finally {
    gcLoading.value = false;
  }
};

// ─────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────
/** 将值限制在 0~100 之间，防止进度条溢出 */
const clamp = (v?: number) => Math.min(Math.max(v ?? 0, 0), 100);

/** 根据使用率生成圆环仪表盘 conic-gradient 样式 */
const getGaugeStyle = (pct: number = 0, color: string) => ({
  background: `conic-gradient(${color} ${pct * 3.6}deg, rgba(226,232,240,0.35) 0deg)`
});

/** 根据磁盘使用率返回进度条渐变色 */
const getDiskColor = (usage: number) => {
  if (usage > 90) return 'linear-gradient(90deg,#f53f3f,#ff7875)';
  if (usage > 70) return 'linear-gradient(90deg,#ff7d00,#ffb454)';
  return 'linear-gradient(90deg,#14c9c9,#6ee7b7)';
};

/** 根据磁盘使用率返回百分比文字颜色 class */
const getDiskTextClass = (usage: number) => {
  if (usage > 90) return 'text-danger';
  if (usage > 70) return 'text-warning';
  return 'text-ok';
};

// ─────────────────────────────────────────────
// 生命周期
// ─────────────────────────────────────────────
onMounted(() => {
  pullServerStats();
  refreshTimer.value = setInterval(pullServerStats, 3000);
});

onUnmounted(() => {
  if (refreshTimer.value !== null) {
    clearInterval(refreshTimer.value);
    refreshTimer.value = null;
  }
});
</script>

<style scoped>
/* ══════════════════════════════════════════════════════════
   BML 运维驾驶舱 — 全屏零滚动布局
   布局比例：左(300px) | 中(1fr) | 右(300px)
   中间卡片紧凑化，左右侧栏加宽，GC按钮置于中间卡片底部
   ══════════════════════════════════════════════════════════ */

.monitor-shell {
  height: 100%;
  padding: 14px 18px;
  background: var(--color-bg-1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
}

.monitor-aurora {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 85%, rgba(20,201,201,0.04), transparent 50%),
    radial-gradient(circle at 85% 15%, rgba(22,93,255,0.05), transparent 50%);
  pointer-events: none;
  z-index: 0;
}

/* ══ 顶部信息栏 ══ */
.monitor-header-sc {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
  z-index: 2;
  position: relative;
}

.header-left { display: flex; align-items: center; gap: 10px; }
.pulse-indicator {
  width: 9px; height: 9px; background: #00b42a; border-radius: 50%;
  box-shadow: 0 0 8px rgba(0,180,42,0.7); animation: pulse 2s infinite; flex-shrink: 0;
}
.title-meta h1 { margin: 0; font-size: 19px; font-weight: 800; color: #0f172a; letter-spacing: -0.01em; }
.title-meta p  { margin: 0; font-size: 11px; color: #64748b; font-weight: 500; }

/* 网速看板 */
.header-center { position: absolute; left: 50%; transform: translateX(-50%); }
.traffic-rail {
  display: flex; align-items: center;
  background: #fff; padding: 5px 14px; border-radius: 12px;
  border: 1px solid rgba(226,232,240,0.9); box-shadow: 0 2px 10px rgba(15,23,42,0.05);
}
.traffic-node { display: flex; align-items: center; gap: 7px; }
.traffic-node .label { font-size: 9px; font-weight: 800; color: #94a3b8; }
.traffic-node .value { font-family: 'JetBrains Mono', monospace; font-size: 12px; font-weight: 800; }
.traffic-node .value.up      { color: #165dff; }
.traffic-node .value.down    { color: #00b42a; }
.traffic-node .value.neutral { color: #475569; }
.node-gap { width: 1px; height: 14px; background: #e2e8f0; margin: 0 10px; }

/* IP 单行 */
.header-right { align-self: center; }
.ip-console {
  display: flex; align-items: center;
  background: #fff; padding: 5px 14px; border-radius: 12px;
  border: 1px solid rgba(226,232,240,0.9); box-shadow: 0 2px 10px rgba(15,23,42,0.05);
  white-space: nowrap;
}
.ip-item { display: flex; align-items: center; gap: 6px; }
.ip-divider { width: 1px; height: 14px; background: #e2e8f0; margin: 0 12px; flex-shrink: 0; }
.ip-item .chip {
  font-size: 9px; font-weight: 800; color: #64748b;
  padding: 1px 5px; background: rgba(15,23,42,0.04); border-radius: 4px; flex-shrink: 0;
}
.ip-item .chip.pub { color: #165dff; background: rgba(22,93,255,0.07); }
.ip-item .addr { font-family: 'JetBrains Mono', monospace; font-size: 12px; font-weight: 700; color: #334155; }

/* ══ 主视口 ══ */
.monitor-viewport { flex: 1; min-height: 0; display: flex; flex-direction: column; z-index: 1; }
.viewport-grid {
  flex: 1;
  display: grid;
  /*
   * 布局比例调整：
   *   左右侧栏从 300px 加宽到 360px，给工作目录路径、磁盘信息更多展示空间；
   *   中间卡片由 1fr 自适应缩窄，仅展示核心仪表盘，视觉更聚焦。
   */
  grid-template-columns: 360px 1fr 360px;
  gap: 12px;
  min-height: 0;
}
.grid-side { display: flex; flex-direction: column; gap: 12px; min-height: 0; }
.grid-main { min-height: 0; display: flex; flex-direction: column; }

/* ══ 卡片基础 ══ */
.lv-card {
  border-radius: 16px;
  border: 1px solid rgba(226,232,240,0.85);
  background: rgba(255,255,255,0.95);
  box-shadow: 0 2px 12px rgba(15,23,42,0.06);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
/*
 * 侧边卡片高度分配：
 *   默认 flex: 1 表示等分。通过在模板中给两个卡片加不同 flex 值来控制比例。
 *   基础运行环境卡片（.side-card-env）：flex: 2（约 40% 高度，紧凑显示6行信息）
 *   JVM 卡片（.side-card-jvm）：flex: 3（约 60% 高度，展示更多 JVM 详情）
 */
.side-card     { flex: 1; padding: 14px; min-height: 0; }
.side-card-env { flex: 5; padding: 14px; min-height: 0; }
.side-card-jvm { flex: 6; padding: 14px; min-height: 0; }
/* 中间主卡片：撑满高度 */
.main-card { flex: 1; padding: 16px; min-height: 0; }
/* 网络卡片：调至右侧上方后，给一个合适的固定比例，不与磁盘卡片争高度 */
.net-card  { flex: 1; padding: 14px; min-height: 0; max-height: 140px; }

/* 顶部彩色条 */
.lv-card__accent           { position: absolute; top: 0; left: 0; right: 0; height: 3px; }
.lv-card__accent--blue     { background: linear-gradient(90deg,#165dff,#6aa1ff); }
.lv-card__accent--teal     { background: linear-gradient(90deg,#14c9c9,#6ee7b7); }
.lv-card__accent--gold     { background: linear-gradient(90deg,#ff7d00,#ffb454); }
.lv-card__accent--purple   { background: linear-gradient(90deg,#722ed1,#b37feb); }

/* 卡片标题行 */
.card-title-sc { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.card-title-sc .arco-icon { color: #165dff; font-size: 14px; }
.card-title-sc span { font-size: 13px; font-weight: 800; color: #0f172a; }

/* ══ 左侧：系统环境卡片排版 ══
 * 设计原则：
 *   - 每行用浅色分隔线隔开，增加层次感
 *   - key 标签与 value 字号拉开差距，形成主次对比
 *   - 行间距从 8px 增大到 0（改用 padding 控制），视觉更舒展
 */
.info-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.55);
}
/* 最后一行不显示分隔线 */
.info-row:last-child { border-bottom: none; }

.info-key {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
  flex-shrink: 0;
  letter-spacing: 0.01em;
}
.info-val {
  font-size: 12px;
  color: #1e293b;
  font-weight: 700;
  text-align: right;
  word-break: break-all;
  line-height: 1.4;
}
/*
 * 工作目录路径最大宽度：随侧栏加宽同步放大，确保路径完整显示不被截断。
 * 侧栏 360px - 左右 padding(28px) - 标签宽度(~52px) - gap(8px) ≈ 272px
 */
.text-ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 240px; display: block; }
.tint-blue   { color: #165dff !important; }
.tint-purple { color: #722ed1 !important; }
.tint-green  { color: #00b42a !important; }

/* ══ 左侧：JVM 卡片排版 ══ */

/* 版本标签行：加大行高，增加呼吸感 */
.jvm-tags { display: flex; flex-direction: column; gap: 6px; margin-bottom: 12px; }
.spec-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
  background: #f8fafc;
  padding: 6px 10px;
  border-radius: 8px;
  border: 1px solid rgba(226, 232, 240, 0.7);
  line-height: 1.4;
}
.tag { font-size: 9px; font-weight: 900; color: #fff; padding: 2px 6px; border-radius: 4px; flex-shrink: 0; }
.tag.java { background: #e84d31; }
.tag.jvm  { background: #52c41a; }

/* JVM 内存四格：加大内边距，数字更突出 */
.jvm-mem-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; margin-bottom: 12px; }
.jvm-mem-item {
  background: #f8fafc;
  border-radius: 10px;
  padding: 9px 11px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}
.m-lbl {
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
  letter-spacing: 0.02em;
}
.m-val {
  font-size: 15px;
  font-weight: 800;
  color: #1e293b;
  font-family: 'JetBrains Mono', monospace;
  line-height: 1.2;
}

/* JVM 时间行：加大行间距，用分隔线区分 */
.jvm-time-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-top: 1px solid rgba(226, 232, 240, 0.55);
  padding-top: 2px;
}
.jvm-time-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.45);
}
.jvm-time-row:last-child { border-bottom: none; }
.t-key {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 600;
  letter-spacing: 0.01em;
}
.t-val {
  font-size: 11px;
  color: #334155;
  font-weight: 700;
  line-height: 1.4;
}

/* ══ 中间：主卡片头部 ══ */
.main-head {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 14px; flex-shrink: 0;
}
.main-title-group h2 { margin: 0; font-size: 16px; font-weight: 900; color: #0f172a; }
.main-title-group p  { margin: 2px 0 0; font-size: 10px; color: #94a3b8; font-weight: 600; }

/* 负载胶囊 */
.load-pills { display: flex; gap: 6px; align-items: center; }
.load-pill {
  display: flex; flex-direction: column; align-items: center;
  background: rgba(22,93,255,0.05); border: 1px solid rgba(22,93,255,0.12);
  border-radius: 8px; padding: 4px 10px; min-width: 44px;
}
.lp-label { font-size: 8px; font-weight: 800; color: #94a3b8; }
.lp-val   { font-size: 14px; font-weight: 900; color: #165dff; font-family: 'JetBrains Mono', monospace; line-height: 1.2; }

/* ══ 中间：三环仪表盘（紧凑版） ══ */
.gauges-row {
  display: flex;
  justify-content: space-around;
  align-items: center;
  flex: 1;
  min-height: 0;
  padding: 4px 0;
}
.gauge-cell { display: flex; flex-direction: column; align-items: center; gap: 6px; }

/* 小环（CPU / JVM） */
.ring-sm {
  width: 88px; height: 88px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; padding: 7px;
}
/* 大环（内存，主角） */
.ring-lg {
  width: 120px; height: 120px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; padding: 8px;
}
.ring-inner {
  width: 100%; height: 100%; background: #fff; border-radius: 50%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  box-shadow: 0 3px 10px rgba(15,23,42,0.07);
}
.ring-pct  { font-weight: 900; color: #0f172a; line-height: 1.1; }
.ring-sm .ring-pct { font-size: 16px; }
.ring-lg .ring-pct { font-size: 24px; }
.ring-tag  { font-size: 8px; font-weight: 800; color: #94a3b8; letter-spacing: 0.1em; }
.ring-lg .ring-tag { font-size: 9px; }

.gauge-label { font-size: 10px; font-weight: 700; color: #64748b; }

/* 内存详情小字 */
.mem-detail-row {
  display: flex; align-items: center; gap: 4px;
  font-size: 9px; font-weight: 700; color: #94a3b8;
}
.dot-sep { color: #cbd5e1; }

/* ══ 中间：CPU 进度条区 ══ */
.cpu-detail-box {
  background: rgba(248,250,252,0.9);
  border-radius: 12px; border: 1px solid rgba(226,232,240,0.6);
  padding: 12px 14px; margin-top: 12px; flex-shrink: 0;
}
.cpu-model-line {
  display: flex; align-items: center; gap: 7px;
  font-size: 11px; font-weight: 800; color: #475569; margin-bottom: 10px;
}
.cpu-icon { color: #165dff; font-size: 13px; }
.cpu-core-badge {
  margin-left: auto; font-size: 9px; font-weight: 800; color: #165dff;
  background: rgba(22,93,255,0.08); padding: 1px 7px; border-radius: 20px;
}
.prog-list { display: flex; flex-direction: column; gap: 7px; }
.prog-item { display: grid; grid-template-columns: 68px 1fr 42px; align-items: center; gap: 10px; }
.prog-label { font-size: 10px; font-weight: 700; color: #64748b; }
.prog-track { height: 5px; background: #e2e8f0; border-radius: 999px; overflow: hidden; }
.prog-fill  { height: 100%; border-radius: 999px; transition: width 0.8s ease-in-out; }
.fill-sys  { background: linear-gradient(90deg,#165dff,#6aa1ff); }
.fill-user { background: linear-gradient(90deg,#14c9c9,#6ee7b7); }
.fill-io   { background: linear-gradient(90deg,#722ed1,#b37feb); }
.fill-idle { background: linear-gradient(90deg,#e2e8f0,#f1f5f9); }
.prog-val  { font-family: 'JetBrains Mono', monospace; font-size: 11px; font-weight: 800; color: #1e293b; text-align: right; }

/* ══ 中间：底部工具栏（GC 按钮） ══ */
.main-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(22,93,255,0.04), rgba(114,46,209,0.04));
  border-radius: 12px;
  border: 1px solid rgba(22,93,255,0.1);
  flex-shrink: 0;
}
.footer-tip {
  display: flex; align-items: center; gap: 6px;
  font-size: 11px; color: #64748b; font-weight: 600;
}
.footer-tip strong { color: #165dff; }
.tip-icon { color: #165dff; font-size: 13px; flex-shrink: 0; }
.gc-btn {
  font-weight: 700 !important;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #165dff, #722ed1) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(22,93,255,0.3) !important;
  transition: box-shadow 0.2s, transform 0.15s !important;
  flex-shrink: 0;
}
.gc-btn:hover {
  box-shadow: 0 6px 18px rgba(22,93,255,0.45) !important;
  transform: translateY(-1px) !important;
}
.gc-btn:active { transform: translateY(0) !important; }

/* ══ 右侧：磁盘阵列 ══ */
.disk-scroller { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; scrollbar-width: thin; }
.disk-unit {
  padding: 10px 12px; background: #fff; border-radius: 10px;
  border: 1px solid rgba(226,232,240,0.7);
}
.disk-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.disk-mount { font-size: 12px; font-weight: 800; color: #0f172a; }
.disk-type-tag {
  font-size: 8px; font-weight: 800; color: #94a3b8;
  padding: 1px 5px; border: 1px solid #e2e8f0; border-radius: 4px;
}
.disk-bar-wrap {
  height: 4px; background: #f1f5f9; border-radius: 999px; overflow: hidden; margin-bottom: 5px;
}
.disk-bar-fill { height: 100%; border-radius: 999px; transition: width 0.8s ease; }
.disk-stat { display: flex; justify-content: space-between; font-size: 10px; font-weight: 700; color: #64748b; }
.text-danger  { color: #f53f3f !important; }
.text-warning { color: #ff7d00 !important; }
.text-ok      { color: #00b42a !important; }

.disk-io { display: flex; gap: 8px; margin-top: 6px; padding-top: 6px; border-top: 1px solid #f1f5f9; }
.io-chip {
  display: flex; align-items: center; gap: 3px;
  font-size: 10px; font-weight: 700; padding: 1px 6px; border-radius: 4px;
}
.io-chip.read  { color: #165dff; background: rgba(22,93,255,0.06); }
.io-chip.write { color: #00b42a; background: rgba(0,180,42,0.06); }
.io-chip .arco-icon { font-size: 10px; }
.disk-empty { text-align: center; color: #94a3b8; font-size: 12px; padding: 20px 0; }

/* ══ 右侧：网络状态 ══ */
.net-grid { display: flex; gap: 10px; }
.net-item {
  flex: 1; display: flex; align-items: center; gap: 10px;
  background: #f8fafc; border-radius: 10px; padding: 10px 12px;
}
.net-icon-wrap {
  width: 34px; height: 34px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.net-icon-wrap.tcp { background: rgba(22,93,255,0.1); color: #165dff; }
.net-icon-wrap.udp { background: rgba(114,46,209,0.1); color: #722ed1; }
.net-icon-wrap .arco-icon { font-size: 16px; }
.net-text { display: flex; flex-direction: column; gap: 2px; }
.net-num  { font-size: 22px; font-weight: 900; color: #0f172a; font-family: 'JetBrains Mono', monospace; line-height: 1; }
.net-desc { font-size: 9px; font-weight: 700; color: #94a3b8; }

/* ══ 动画 ══ */
@keyframes pulse {
  0%,100% { transform: scale(1);   opacity: 1;   }
  50%      { transform: scale(1.25); opacity: 0.65; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0);    }
}
@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.97); }
  to   { opacity: 1; transform: scale(1);    }
}
.entrance-anim-1     { animation: fadeInUp 0.4s backwards; }
.entrance-anim-2     { animation: fadeInUp 0.4s 0.07s backwards; }
.entrance-anim-3     { animation: fadeInUp 0.4s 0.14s backwards; }
.entrance-anim-4     { animation: fadeInUp 0.4s 0.21s backwards; }
.entrance-anim-scale { animation: scaleIn 0.6s backwards; }
</style>
