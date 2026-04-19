<template>
  <div class="workplace-container">
    <!-- 头部横幅与核心概览 -->
    <div class="welcome-banner" :class="{ expired: !licenseData.valid }">
      <div class="banner-bg-graphics">
        <div class="circle-1"></div>
        <div class="circle-2"></div>
      </div>
      
      <div class="banner-content">
        <div class="banner-left">
          <div class="welcome-icon">
            <icon-thunderbolt />
          </div>
          <div class="welcome-text">
            <h1 class="welcome-title">欢迎回来，管理员</h1>
            <p class="welcome-subtitle">
              {{ greeting }}，今天是 {{ currentDate }}。
              系统授权给 <strong>{{ licenseData.clientName }}</strong>
            </p>
          </div>
        </div>
        
        <div class="banner-right">
          <div class="auth-countdown">
            <div class="countdown-label">授权剩余可用时间</div>
            <div class="countdown-val-wrap">
              <span class="countdown-value">{{ licenseData.daysLeft }}</span>
              <span class="countdown-unit">天</span>
            </div>
            <div class="countdown-sub">
              {{ licenseData.valid ? `到期日: ${licenseData.expireDate}` : '授权已过期或无效' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 核心运行指标 -->
    <div class="stats-grid">
      <div class="stat-card" style="--card-theme: #165dff">
        <div class="stat-icon"><icon-layers /></div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboardSummary.apiRegistry.total }}</div>
          <div class="stat-label">资产总数 (纳管API)</div>
        </div>
      </div>
      <div class="stat-card" style="--card-theme: #00b42a">
        <div class="stat-icon"><icon-safe /></div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboardSummary.apiAccount.enabled }} <span class="stat-sub">/ {{ dashboardSummary.apiAccount.total }}</span></div>
          <div class="stat-label">启用账号 / 账号总数</div>
        </div>
      </div>
      <div class="stat-card" style="--card-theme: #ff7d00">
        <div class="stat-icon"><icon-notification /></div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboardSummary.alert.unresolved }} <span class="stat-sub">件</span></div>
          <div class="stat-label">当前未处理告警</div>
        </div>
      </div>
      <div class="stat-card" style="--card-theme: #722ed1">
        <div class="stat-icon"><icon-sync /></div>
        <div class="stat-info">
          <div class="stat-value">{{ dashboardSummary.alert.todayTotal }} <span class="stat-sub">次</span></div>
          <div class="stat-label">今日告警触发总量</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口区 -->
    <div class="section-title">快捷入口</div>
    <div class="quick-actions">
      <div class="action-card" v-for="action in quickActions" :key="action.label" @click="router.push(action.path)">
        <div class="action-icon" :style="{ background: action.gradient }">
          <component :is="action.icon" />
        </div>
        <div class="action-text">
          <div class="action-title">{{ action.label }}</div>
          <div class="action-desc">{{ action.desc }}</div>
        </div>
        <div class="action-arrow">
          <icon-right />
        </div>
      </div>
    </div>

    <!-- 底部状态面板：自适应填充剩余高度 -->
    <div class="status-row">
      <!-- 物理运行机体看板 -->
      <div class="status-panel">
        <div class="panel-header">
          <div class="panel-title">主机运行指标</div>
          <a-tag v-if="dashboardSummary.monitor.cpuPercent < 90" color="green" size="small">
            <template #icon><icon-check-circle-fill /></template>运行正常
          </a-tag>
          <a-tag v-else color="red" size="small">
            <template #icon><icon-exclamation-circle-fill /></template>负载过高
          </a-tag>
        </div>
        
        <div class="monitor-items">
          <div class="monitor-item">
            <div class="m-label">CPU占用 <span class="m-val">{{ dashboardSummary.monitor.cpuPercent.toFixed(1) }}%</span></div>
            <a-progress :percent="dashboardSummary.monitor.cpuPercent / 100" color="#165dff" :stroke-width="8" :show-text="false" />
          </div>
          <div class="monitor-item">
            <div class="m-label">内存占用 <span class="m-val">{{ dashboardSummary.monitor.memPercent.toFixed(1) }}%</span></div>
            <a-progress :percent="dashboardSummary.monitor.memPercent / 100" color="#00b42a" :stroke-width="8" :show-text="false" />
          </div>
          <div class="monitor-item">
            <div class="m-label">磁盘占用 <span class="m-val">{{ dashboardSummary.monitor.diskPercent.toFixed(1) }}%</span></div>
            <a-progress :percent="dashboardSummary.monitor.diskPercent / 100" color="#ff7d00" :stroke-width="8" :show-text="false" />
          </div>
        </div>
      </div>

      <!-- 近期活动与告警日志 -->
      <div class="status-panel">
        <div class="panel-header">
          <div class="panel-title">近期告警日志</div>
          <a-link @click="router.push('/admin/alert/center')">查看全部</a-link>
        </div>
        
        <div class="activity-list" v-if="dashboardSummary.recentActivities.length > 0">
          <div class="activity-item" v-for="(act, idx) in dashboardSummary.recentActivities" :key="idx">
            <div class="act-dot" :style="{ backgroundColor: act.color }"></div>
            <div class="act-text">{{ act.text }}</div>
            <div class="act-time">{{ act.time }}</div>
          </div>
        </div>
        <a-empty v-else description="暂无近期活动" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import {
  IconThunderbolt, IconLayers, IconSafe, IconSettings, IconComputer,
  IconNotification, IconSync, IconRight, IconCheckCircleFill, IconExclamationCircleFill
} from '@arco-design/web-vue/es/icon';
import { getDashboardSummary, type DashboardSummary } from '@/api/dashboard';

defineOptions({ name: 'Dashboard' });

const router = useRouter();
const currentDate = ref(dayjs().format('YYYY年MM月DD日'));
let timer: ReturnType<typeof setInterval> | null = null;
let dataTimer: ReturnType<typeof setInterval> | null = null;

const greeting = computed(() => {
  const hour = dayjs().hour();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

// 初始化防御性空数据
const dashboardSummary = ref<DashboardSummary>({
  license: { valid: true, clientName: '—', expireDate: '—', daysLeft: 0 },
  monitor: { cpuPercent: 0, memPercent: 0, diskPercent: 0, os: '—' },
  alert: { todayTotal: 0, unresolved: 0 },
  apiAccount: { total: 0, enabled: 0 },
  apiRegistry: { total: 0 },
  recentActivities: []
});

const licenseData = computed(() => dashboardSummary.value.license);

// 四个主要子系统快捷入口
const quickActions = [
  {
    label: '资产目录',
    desc: '全源 API 映射与发现',
    path: '/admin/api/list',
    icon: IconLayers,
    gradient: 'linear-gradient(135deg, #165dff 0%, #4a8dff 100%)',
  },
  {
    label: '授权治理',
    desc: 'API 消费方账号分配',
    path: '/admin/api/account',
    icon: IconSafe,
    gradient: 'linear-gradient(135deg, #00b42a 0%, #23c343 100%)',
  },
  {
    label: '主机监控',
    desc: '硬件与 OS 资源池运行状态',
    path: '/admin/monitor/server',
    icon: IconComputer,
    gradient: 'linear-gradient(135deg, #ff7d00 0%, #ff9a2e 100%)',
  },
  {
    label: '告警中心',
    desc: '全局异常事件跟踪与派发',
    path: '/admin/alert/center',
    icon: IconNotification,
    gradient: 'linear-gradient(135deg, #f53f3f 0%, #ff6e6e 100%)',
  }
];

const fetchData = async () => {
  try {
    const res = await getDashboardSummary() as any;
    if (res.code === 200) {
      dashboardSummary.value = res.data;
    }
  } catch (err) {
    console.error('[Dashboard] 无法拉取工作台实时数据', err);
  }
};

onMounted(() => {
  fetchData();
  
  // 5秒轮询最新状态
  dataTimer = setInterval(fetchData, 5000);
  
  // 天数变化时更新日期
  timer = setInterval(() => {
    currentDate.value = dayjs().format('YYYY年MM月DD日');
  }, 1000 * 60 * 60);
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
  if (dataTimer) clearInterval(dataTimer);
});
</script>

<style scoped>
/* 严格的 Zero-Scroll (溢出隐藏，利用流式自适应充满整个屏幕高度) */
.workplace-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px - 44px); /* 视窗高度减去顶栏和内边距 */
  box-sizing: border-box;
  padding: 16px 20px;
  background: #f2f3f5;
  overflow: hidden;
  gap: 16px;
}

/* ── 头部欢迎与授权看板 ── */
.welcome-banner {
  flex-shrink: 0;
  height: 110px;
  background: linear-gradient(135deg, #165dff 0%, #632ef3 100%);
  border-radius: 12px;
  padding: 0 32px;
  color: white;
  position: relative;
  overflow: hidden;
  display: flex;
  box-shadow: 0 4px 10px rgba(22, 93, 255, 0.2);
}
.welcome-banner.expired {
  background: linear-gradient(135deg, #f53f3f 0%, #cf2626 100%);
  box-shadow: 0 4px 10px rgba(245, 63, 63, 0.2);
}

.banner-bg-graphics {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
}
.circle-1 {
  position: absolute; top: -50px; right: 20%;
  width: 200px; height: 200px; border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.circle-2 {
  position: absolute; bottom: -80px; right: -20px;
  width: 150px; height: 150px; border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
}

.banner-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.banner-left {
  display: flex;
  align-items: center;
  gap: 20px;
}
.welcome-icon {
  width: 56px; height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px;
  backdrop-filter: blur(8px);
}
.welcome-title { margin: 0 0 6px 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
.welcome-subtitle { margin: 0; font-size: 14px; opacity: 0.9; }

.banner-right {
  display: flex;
  align-items: center;
}
.auth-countdown {
  text-align: right;
  padding-left: 24px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}
.countdown-label { font-size: 12px; opacity: 0.8; margin-bottom: 2px; }
.countdown-value { font-size: 32px; font-weight: 900; font-family: 'Inter', sans-serif; }
.countdown-unit { font-size: 14px; padding-left: 4px; opacity: 0.9; }
.countdown-sub { font-size: 12px; opacity: 0.7; margin-top: -2px; }

/* ── 核心运行指标四格卡片 ── */
.stats-grid {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.stat-card {
  background: white; border-radius: 12px; padding: 20px;
  display: flex; align-items: center; gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  position: relative; overflow: hidden;
}
.stat-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
  background: var(--card-theme);
}
.stat-icon {
  width: 48px; height: 48px; border-radius: 12px;
  background: color-mix(in srgb, var(--card-theme) 12%, transparent);
  color: var(--card-theme);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; flex-shrink: 0;
}
.stat-info { flex: 1; }
.stat-value { font-size: 24px; font-weight: 800; color: #1d2129; line-height: 1.1; font-family: 'Inter', sans-serif;}
.stat-sub { font-size: 14px; font-weight: 500; color: #86909c; }
.stat-label { font-size: 13px; color: #86909c; margin-top: 6px; }

/* ── 快捷入口区块 ── */
.section-title {
  flex-shrink: 0;
  font-size: 15px; font-weight: 600; color: #1d2129;
  padding-left: 10px; border-left: 3px solid #165dff;
  margin: 4px 0 0px 0;
}
.quick-actions {
  flex-shrink: 0;
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
}
.action-card {
  background: white; border-radius: 12px; padding: 16px 20px;
  display: flex; align-items: center; gap: 14px;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  transition: all 0.25s ease;
}
.action-card:hover { transform: translateY(-3px); box-shadow: 0 6px 16px rgba(0,0,0,0.08); }
.action-icon {
  width: 42px; height: 42px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: white; flex-shrink: 0;
}
.action-text { flex: 1; }
.action-title { font-size: 15px; font-weight: 600; color: #1d2129; margin-bottom: 3px; }
.action-desc { font-size: 12px; color: #86909c; }
.action-arrow { color: #c9cdd4; font-size: 18px; transition: transform 0.2s; }
.action-card:hover .action-arrow { color: #165dff; transform: translateX(3px); }

/* ── 底部双布局（状态与日志），动态填充余下空间 ── */
.status-row {
  flex: 1; /* 占据剩余全部高度 */
  min-height: 0; /* 必须设置，否则 flex 子项无法被内部滚动控制 */
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
}
.status-panel {
  background: white; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  display: flex; flex-direction: column;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; flex-shrink: 0;
}
.panel-title { font-size: 16px; font-weight: 600; color: #1d2129; }

/* 主机指标 */
.monitor-items {
  flex: 1; display: flex; flex-direction: column; justify-content: space-around;
}
.monitor-item { margin-bottom: 12px; }
.m-label { display: flex; justify-content: space-between; font-size: 13px; color: #4e5969; margin-bottom: 8px; }
.m-val { font-weight: 700; color: #1d2129; font-family: 'Inter', sans-serif;}

/* 活动日志：超过高度后出内部滚动条，保证主体大框架 Zero-Scroll */
.activity-list {
  flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 14px;
  padding-right: 8px;
}
.activity-list::-webkit-scrollbar { width: 4px; }
.activity-list::-webkit-scrollbar-thumb { background: #e5e6eb; border-radius: 2px; }
.activity-item { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.act-dot { width: 8px; height: 8px; border-radius: 50%; box-shadow: 0 0 0 2px rgba(255,255,255,0.8); }
.act-text { flex: 1; font-size: 13px; color: #4e5969;  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
.act-time { font-size: 12px; color: #86909c; white-space: nowrap;}
</style>
