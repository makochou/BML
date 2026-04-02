<template>
  <div class="workplace-container">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="banner-content">
        <div class="banner-left">
          <div class="welcome-icon">
            <icon-thunderbolt />
          </div>
          <div class="welcome-text">
            <h1 class="welcome-title">欢迎回来，管理员</h1>
            <p class="welcome-subtitle">{{ greeting }}，今天是 {{ currentDate }}，祝您工作顺利。</p>
          </div>
        </div>
        <div class="banner-right">
          <div class="time-display">
            <span class="time-value">{{ currentTime }}</span>
            <span class="time-label">当前时间</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片区 -->
    <div class="stats-grid">
      <div
        v-for="stat in stats"
        :key="stat.label"
        class="stat-card"
        :style="{ '--card-color': stat.color }"
      >
        <div class="stat-icon">
          <component :is="stat.icon" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
          <component :is="stat.trend > 0 ? IconArrowUp : IconArrowDown" />
          <span>{{ Math.abs(stat.trend) }}%</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="section-title">快捷入口</div>
    <div class="quick-actions">
      <div
        v-for="action in quickActions"
        :key="action.label"
        class="action-card"
        @click="router.push(action.path)"
      >
        <div class="action-icon" :style="{ background: action.gradient }">
          <component :is="action.icon" />
        </div>
        <div class="action-label">{{ action.label }}</div>
        <div class="action-desc">{{ action.desc }}</div>
      </div>
    </div>

    <!-- 系统状态 -->
    <div class="section-title">系统状态</div>
    <div class="status-row">
      <div class="status-card">
        <div class="status-header">
          <span class="status-title">系统运行状态</span>
          <a-tag color="green" size="small">
            <template #icon><icon-check-circle-fill /></template>
            运行正常
          </a-tag>
        </div>
        <div class="status-items">
          <div class="status-item" v-for="item in systemStatus" :key="item.label">
            <span class="status-item-label">{{ item.label }}</span>
            <a-progress
              :percent="item.percent"
              :color="item.color"
              :track-color="'rgba(0,0,0,0.06)'"
              size="small"
              style="flex: 1; margin: 0 12px;"
            />
            <span class="status-item-value">{{ item.value }}</span>
          </div>
        </div>
      </div>

      <div class="status-card">
        <div class="status-header">
          <span class="status-title">近期活动</span>
        </div>
        <div class="activity-list">
          <div class="activity-item" v-for="item in recentActivities" :key="item.time">
            <div class="activity-dot" :style="{ background: item.color }"></div>
            <div class="activity-content">
              <span class="activity-text">{{ item.text }}</span>
              <span class="activity-time">{{ item.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import {
  IconThunderbolt, IconApps, IconSafe, IconLayers, IconSettings,
  IconArrowUp, IconArrowDown, IconCheckCircleFill
} from '@arco-design/web-vue/es/icon';

// 声明组件名称，用于 keep-alive 缓存
defineOptions({ name: 'Dashboard' });

const router = useRouter();

// ── 时间显示 ──
const currentTime = ref(dayjs().format('HH:mm:ss'));
const currentDate = ref(dayjs().format('YYYY年MM月DD日'));
let timer: ReturnType<typeof setInterval> | null = null;

/** 根据当前小时返回问候语 */
const greeting = computed(() => {
  const hour = dayjs().hour();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '早上好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

// ── 统计数据（实际项目中应从接口获取） ──
const stats = ref([
  { label: 'API 账号总数', value: '—', trend: 0, color: '#165dff', icon: IconSafe },
  { label: '纳管接口数',   value: '—', trend: 0, color: '#00b42a', icon: IconLayers },
  { label: '今日调用量',   value: '—', trend: 0, color: '#ff7d00', icon: IconApps },
  { label: '系统用户数',   value: '—', trend: 0, color: '#722ed1', icon: IconSettings },
]);

// ── 快捷入口 ──
const quickActions = [
  {
    label: '全源资产目录',
    desc: '查看所有纳管 API 接口',
    path: '/admin/api/list',
    icon: IconLayers,
    gradient: 'linear-gradient(135deg, #165dff 0%, #3c7eff 100%)',
  },
  {
    label: '授权治理中心',
    desc: '管理 API 账号与授权',
    path: '/admin/api/account',
    icon: IconSafe,
    gradient: 'linear-gradient(135deg, #00b42a 0%, #23c343 100%)',
  },
  {
    label: '系统管理',
    desc: '用户、角色、菜单配置',
    path: '/admin/system/user',
    icon: IconSettings,
    gradient: 'linear-gradient(135deg, #722ed1 0%, #9254de 100%)',
  },
  {
    label: '服务器监控',
    desc: '实时查看服务器资源',
    path: '/admin/monitor/server',
    icon: IconApps,
    gradient: 'linear-gradient(135deg, #ff7d00 0%, #ff9a2e 100%)',
  },
];

// ── 系统状态（示例数据） ──
const systemStatus = ref([
  { label: 'CPU 使用率',  percent: 32, value: '32%',  color: '#165dff' },
  { label: '内存使用率',  percent: 58, value: '58%',  color: '#00b42a' },
  { label: '磁盘使用率',  percent: 45, value: '45%',  color: '#ff7d00' },
]);

// ── 近期活动（示例数据） ──
const recentActivities = ref([
  { text: '超级管理员 登录系统',         time: '刚刚',    color: '#165dff' },
  { text: 'API 接口目录同步完成',         time: '5 分钟前', color: '#00b42a' },
  { text: '新增 API 账号「测试账号」',    time: '1 小时前', color: '#722ed1' },
  { text: '服务器 CPU 告警已恢复正常',    time: '2 小时前', color: '#ff7d00' },
  { text: '系统完成每日数据备份',         time: '昨天',    color: '#86909c' },
]);

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('HH:mm:ss');
  }, 1000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style scoped>
/* ── 整体容器 ── */
.workplace-container {
  height: 100%;
  overflow-y: auto;
  padding: 20px 24px;
  background: #f2f3f5;
  scrollbar-width: thin;
  scrollbar-color: #e5e6eb transparent;
}

.workplace-container::-webkit-scrollbar {
  width: 6px;
}
.workplace-container::-webkit-scrollbar-thumb {
  background: #e5e6eb;
  border-radius: 3px;
}

/* ── 欢迎横幅 ── */
.welcome-banner {
  background: linear-gradient(135deg, #165dff 0%, #722ed1 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 20px;
  color: white;
  position: relative;
  overflow: hidden;
}

.welcome-banner::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 50%;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  bottom: -60px;
  right: 80px;
  width: 150px;
  height: 150px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
}

.banner-content {
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
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  backdrop-filter: blur(10px);
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px 0;
}

.welcome-subtitle {
  font-size: 14px;
  opacity: 0.85;
  margin: 0;
}

.time-display {
  text-align: right;
}

.time-value {
  display: block;
  font-size: 32px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

.time-label {
  font-size: 12px;
  opacity: 0.7;
}

/* ── 统计卡片 ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: var(--card-color);
  border-radius: 4px 0 0 4px;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--card-color) 12%, transparent);
  color: var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 4px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
}

.stat-trend.up {
  color: #00b42a;
}

.stat-trend.down {
  color: #f53f3f;
}

/* ── 区块标题 ── */
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid #165dff;
}

/* ── 快捷入口 ── */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.action-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.action-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  margin-bottom: 12px;
}

.action-label {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 4px;
}

.action-desc {
  font-size: 12px;
  color: #86909c;
}

/* ── 状态行 ── */
.status-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.status-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.status-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.status-items {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.status-item {
  display: flex;
  align-items: center;
}

.status-item-label {
  font-size: 13px;
  color: #4e5969;
  width: 80px;
  flex-shrink: 0;
}

.status-item-value {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  width: 40px;
  text-align: right;
  flex-shrink: 0;
}

/* ── 近期活动 ── */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.activity-content {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-text {
  font-size: 13px;
  color: #4e5969;
}

.activity-time {
  font-size: 12px;
  color: #c9cdd4;
  flex-shrink: 0;
  margin-left: 8px;
}

/* ── 响应式适配 ── */
@media (max-width: 1200px) {
  .stats-grid,
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid,
  .quick-actions,
  .status-row {
    grid-template-columns: 1fr;
  }

  .banner-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .time-display {
    text-align: left;
  }
}
</style>
