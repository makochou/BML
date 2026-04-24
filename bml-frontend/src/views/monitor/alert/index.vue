
<template>
  <div class="alert-shell">
    <div class="alert-aurora"></div>

    <!-- ══════════════════════════════════════════════════════
         顶部信息栏：标题 / 统计数字 / 操作按钮
         ══════════════════════════════════════════════════════ -->
    <div class="alert-header">
      <!-- 左：标题 -->
      <div class="header-left">
        <div class="pulse-dot" :class="unreadCount > 0 ? 'pulse-danger' : 'pulse-ok'"></div>
        <div class="title-meta">
          <h1>系统告警治理中心</h1>
          <p>实时扫描探测引擎 · 数字化风险管控</p>
        </div>
      </div>

      <!-- 中：统计胶囊 -->
      <div class="header-center">
        <div class="stat-rail">
          <div class="stat-node">
            <span class="stat-label">未读告警</span>
            <span class="stat-val danger">{{ unreadCount }}</span>
          </div>
          <div class="stat-gap"></div>
          <div class="stat-node">
            <span class="stat-label">今日总量</span>
            <span class="stat-val">{{ alertList.length }}</span>
          </div>
          <div class="stat-gap"></div>
          <div class="stat-node">
            <span class="stat-label">紧急</span>
            <span class="stat-val critical">{{ countByLevel('critical') }}</span>
          </div>
          <div class="stat-gap"></div>
          <div class="stat-node">
            <span class="stat-label">警告</span>
            <span class="stat-val warning">{{ countByLevel('warning') }}</span>
          </div>
          <div class="stat-gap"></div>
          <div class="stat-node">
            <span class="stat-label">提示</span>
            <span class="stat-val info">{{ countByLevel('info') }}</span>
          </div>
        </div>
      </div>

      <!-- 右：操作按钮 -->
      <div class="header-right">
        <a-button
          type="outline"
          size="small"
          class="btn-read-all"
          :loading="markAllLoading"
          @click="handleMarkAllRead"
        >
          <template #icon><icon-check-square /></template>
          全部已读
        </a-button>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         主内容：左侧告警列表 / 右侧级别筛选 + 类型分布
         ══════════════════════════════════════════════════════ -->
    <div class="alert-viewport">
      <div class="alert-grid">

        <!-- ══ 左侧：告警列表 ══ -->
        <div class="alert-main">
          <!-- 筛选工具栏 -->
          <div class="filter-bar">
            <div class="filter-tabs">
              <button
                v-for="tab in levelTabs"
                :key="tab.value"
                class="filter-tab"
                :class="{ active: activeLevel === tab.value }"
                @click="activeLevel = tab.value"
              >
                <span class="tab-dot" :class="`dot-${tab.value}`"></span>
                {{ tab.label }}
                <span class="tab-count">{{ tab.value === 'all' ? alertList.length : countByLevel(tab.value) }}</span>
              </button>
            </div>
            <div class="filter-right">
              <a-input-search
                v-model="searchKeyword"
                placeholder="搜索告警标题..."
                size="small"
                class="search-input"
                allow-clear
              />
            </div>
          </div>

          <!-- 告警卡片列表 -->
          <div class="list-scroll">
            <template v-if="filteredList.length > 0">
              <div
                v-for="(alert, index) in filteredList"
                :key="alert.id"
                class="alert-card"
                :class="[`card-${alert.alertLevel}`, { 'card-unread': alert.readStatus === 0 }]"
                :style="{ animationDelay: (index * 0.04) + 's' }"
              >
                <!-- 左侧彩色竖条 -->
                <div class="card-stripe" :class="`stripe-${alert.alertLevel}`"></div>

                <div class="card-body">
                  <!-- 顶部：级别标签 + 类型 + 时间 + 操作 -->
                  <div class="card-top">
                    <div class="card-top-left">
                      <span class="level-badge" :class="`badge-${alert.alertLevel}`">
                        <icon-exclamation-circle-fill />
                        {{ getLevelLabel(alert.alertLevel) }}
                      </span>
                      <span class="type-chip">{{ getTypeLabel(alert.alertType) }}</span>
                      <span v-if="alert.readStatus === 0" class="unread-dot"></span>
                    </div>
                    <div class="card-top-right">
                      <span class="card-time">{{ formatTime(alert.createTime) }}</span>
                      <a-button
                        v-if="alert.readStatus === 0"
                        type="text"
                        size="mini"
                        class="btn-read"
                        :loading="readingId === alert.id"
                        @click="handleReadOne(alert.id)"
                      >
                        标记已读
                      </a-button>
                      <span v-else class="archived-tag">已存档</span>
                      <a-button
                        type="text"
                        size="mini"
                        class="btn-delete"
                        :loading="deletingId === alert.id"
                        @click="handleDelete(alert.id)"
                      >
                        <icon-delete />
                      </a-button>
                    </div>
                  </div>

                  <!-- 标题 -->
                  <h3 class="card-title">{{ alert.alertTitle }}</h3>

                  <!-- 内容 -->
                  <p class="card-content">{{ alert.alertContent }}</p>
                </div>
              </div>
            </template>

            <!-- 空状态 -->
            <div v-else class="empty-state">
              <div class="empty-icon"><icon-check-circle /></div>
              <p class="empty-title">全域系统运行安全</p>
              <span class="empty-sub">{{ searchKeyword ? '没有匹配的告警记录' : '当前没有告警异常' }}</span>
            </div>
          </div>
        </div>

        <!-- ══ 右侧：统计面板 ══ -->
        <div class="alert-aside">

          <!-- 告警级别分布 -->
          <div class="lv-card aside-card entrance-anim-1">
            <div class="lv-card__accent lv-card__accent--gold"></div>
            <div class="aside-title">
              <icon-bar-chart />
              <span>告警级别分布</span>
            </div>
            <div class="level-dist">
              <div v-for="item in levelDistribution" :key="item.level" class="dist-item">
                <div class="dist-meta">
                  <span class="dist-dot" :class="`dot-${item.level}`"></span>
                  <span class="dist-label">{{ item.label }}</span>
                  <span class="dist-count" :class="`text-${item.level}`">{{ item.count }}</span>
                </div>
                <div class="dist-bar-track">
                  <div
                    class="dist-bar-fill"
                    :class="`fill-${item.level}`"
                    :style="{ width: alertList.length ? (item.count / alertList.length * 100) + '%' : '0%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 告警类型分布 -->
          <div class="lv-card aside-card entrance-anim-2">
            <div class="lv-card__accent lv-card__accent--teal"></div>
            <div class="aside-title">
              <icon-apps />
              <span>告警类型分布</span>
            </div>
            <div class="type-dist">
              <div v-for="item in typeDistribution" :key="item.type" class="type-item">
                <div class="type-icon-wrap" :class="`type-icon-${item.colorKey}`">
                  <component :is="item.icon" />
                </div>
                <div class="type-info">
                  <span class="type-name">{{ item.label }}</span>
                  <div class="type-bar-track">
                    <div
                      class="type-bar-fill"
                      :class="`fill-type-${item.colorKey}`"
                      :style="{ width: alertList.length ? (item.count / alertList.length * 100) + '%' : '0%' }"
                    ></div>
                  </div>
                </div>
                <span class="type-count">{{ item.count }}</span>
              </div>
            </div>
          </div>

          <!-- 最近活动时间线 -->
          <div class="lv-card aside-card aside-card-flex entrance-anim-3">
            <div class="lv-card__accent lv-card__accent--purple"></div>
            <div class="aside-title">
              <icon-history />
              <span>最近告警时间线</span>
            </div>
            <div class="timeline-scroll">
              <div v-if="recentFive.length === 0" class="timeline-empty">暂无告警记录</div>
              <div v-for="alert in recentFive" :key="alert.id" class="timeline-item">
                <div class="tl-dot" :class="`tl-dot-${alert.alertLevel}`"></div>
                <div class="tl-body">
                  <span class="tl-title">{{ alert.alertTitle }}</span>
                  <span class="tl-time">{{ formatTime(alert.createTime) }}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 告警中心页面
 *
 * 重要说明：
 *   defineOptions({ name: 'AlertCenter' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'AlertCenter' });

import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Message } from '@arco-design/web-vue';
import {
  IconExclamationCircleFill, IconCheckCircle, IconCheckSquare,
  IconDelete, IconHistory, IconApps, IconBarChart,
  IconThunderbolt, IconStorage, IconDesktop, IconCode
} from '@arco-design/web-vue/es/icon';
/**
 * 使用项目统一的 request 实例（已内置 Authorization Token 注入 + 401 自动刷新）。
 * 禁止直接使用裸 axios，否则请求不携带 Token 会被 Spring Security 拦截返回 401。
 */
import request from '@/utils/request';

// ─────────────────────────────────────────────
// 类型定义：与后端 SysAlertVO 字段一一对应
// ─────────────────────────────────────────────
interface SysAlertVO {
  id: number;
  alertType: string;    // CPU_HIGH | MEMORY_HIGH | DISK_FULL | JVM_HIGH
  alertLevel: string;   // critical | warning | info
  alertTitle: string;
  alertContent: string;
  readStatus: number;   // 0:未读 1:已读
  createTime: string;
}

// ─────────────────────────────────────────────
// 响应式状态
// ─────────────────────────────────────────────
const alertList    = ref<SysAlertVO[]>([]);
const unreadCount  = ref(0);
const activeLevel  = ref('all');
const searchKeyword = ref('');
const markAllLoading = ref(false);
const readingId    = ref<number | null>(null);
const deletingId   = ref<number | null>(null);
/** 增量轮询：记录上次已知的最大告警 ID，避免重复拉取 */
const lastKnownId  = ref<number | null>(null);
/** 轮询定时器句柄 */
const pollTimer    = ref<ReturnType<typeof setInterval> | null>(null);

// ─────────────────────────────────────────────
// 筛选 Tab 配置
// ─────────────────────────────────────────────
const levelTabs = [
  { value: 'all',      label: '全部'   },
  { value: 'critical', label: '紧急'   },
  { value: 'warning',  label: '警告'   },
  { value: 'info',     label: '提示'   },
];

// ─────────────────────────────────────────────
// 计算属性
// ─────────────────────────────────────────────
/** 按级别 + 关键词过滤后的列表 */
const filteredList = computed(() => {
  let list = alertList.value;
  if (activeLevel.value !== 'all') {
    list = list.filter(a => a.alertLevel === activeLevel.value);
  }
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase();
    list = list.filter(a =>
      a.alertTitle?.toLowerCase().includes(kw) ||
      a.alertContent?.toLowerCase().includes(kw)
    );
  }
  return list;
});

/** 最近 5 条告警（用于时间线） */
const recentFive = computed(() => alertList.value.slice(0, 5));

/** 级别分布数据 */
const levelDistribution = computed(() => [
  { level: 'critical', label: '紧急告警', count: countByLevel('critical') },
  { level: 'warning',  label: '警告提醒', count: countByLevel('warning')  },
  { level: 'info',     label: '运行提示', count: countByLevel('info')     },
]);

/** 类型分布数据 */
const typeDistribution = computed(() => {
  const typeMap: Record<string, { label: string; colorKey: string; icon: any; count: number }> = {
    CPU_HIGH:    { label: 'CPU 过载',  colorKey: 'cpu',  icon: IconThunderbolt, count: 0 },
    MEMORY_HIGH: { label: '内存告警',  colorKey: 'mem',  icon: IconDesktop,     count: 0 },
    DISK_FULL:   { label: '磁盘告警',  colorKey: 'disk', icon: IconStorage,     count: 0 },
    JVM_HIGH:    { label: 'JVM 告警',  colorKey: 'jvm',  icon: IconCode,        count: 0 },
  };
  alertList.value.forEach(a => {
    if (typeMap[a.alertType]) {
      typeMap[a.alertType].count++;
    }
  });
  return Object.entries(typeMap).map(([type, v]) => ({ type, ...v }));
});

// ─────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────
/** 按级别统计数量 */
const countByLevel = (level: string) =>
  alertList.value.filter(a => a.alertLevel === level).length;

/** 告警级别中文标签 */
const getLevelLabel = (level: string) => {
  const map: Record<string, string> = {
    critical: '紧急', warning: '警告', info: '提示'
  };
  return map[level] ?? level;
};

/** 告警类型中文标签 */
const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    CPU_HIGH: 'CPU', MEMORY_HIGH: '内存', DISK_FULL: '磁盘', JVM_HIGH: 'JVM'
  };
  return map[type] ?? type;
};

/** 格式化时间：今天显示 HH:mm，其他显示 MM-DD HH:mm */
const formatTime = (t: string) => {
  if (!t) return '';
  const d = new Date(t);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  const hm = d.toTimeString().slice(0, 5);
  if (isToday) return hm;
  const md = `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  return `${md} ${hm}`;
};

// ─────────────────────────────────────────────
// 数据拉取
// ─────────────────────────────────────────────
/**
 * 全量加载告警列表（页面初始化时调用）。
 * 后端接口：GET /api/system/alert/list?limit=100
 */
const loadAlerts = async () => {
  try {
    const [listRes, unreadRes] = await Promise.all([
      request.get('/system/alert/list?limit=100'),
      request.get('/system/alert/unread-count'),
    ]) as any[];
    alertList.value = listRes?.data ?? [];
    unreadCount.value = unreadRes?.data ?? 0;
    // 记录当前最大 ID，供增量轮询使用
    if (alertList.value.length > 0) {
      lastKnownId.value = Math.max(...alertList.value.map(a => a.id));
    }
  } catch (err) {
    console.error('[AlertCenter] 加载告警列表失败', err);
  }
};

/**
 * 增量轮询（每 30 秒执行一次）。
 * 后端接口：GET /api/system/alert/latest?lastId=xxx
 * 仅拉取 lastKnownId 之后的新告警，追加到列表头部并更新未读数。
 */
const pollNewAlerts = async () => {
  try {
    const res = await request.get(
      `/system/alert/latest${lastKnownId.value ? `?lastId=${lastKnownId.value}` : ''}`
    ) as any;
    const newAlerts: SysAlertVO[] = res?.data ?? [];
    if (newAlerts.length > 0) {
      // 新告警插入列表头部
      alertList.value = [...newAlerts, ...alertList.value];
      // 更新未读数
      unreadCount.value += newAlerts.filter(a => a.readStatus === 0).length;
      // 更新最大 ID
      lastKnownId.value = Math.max(...newAlerts.map(a => a.id));
      // 有新告警时弹出提示
      Message.warning({
        content: `收到 ${newAlerts.length} 条新告警`,
        duration: 3000,
      });
    }
  } catch (err) {
    console.error('[AlertCenter] 增量轮询失败', err);
  }
};

// ─────────────────────────────────────────────
// 操作函数
// ─────────────────────────────────────────────
/** 标记单条已读 */
const handleReadOne = async (id: number) => {
  readingId.value = id;
  try {
    await request.put(`/system/alert/read/${id}`);
    const item = alertList.value.find(a => a.id === id);
    if (item) {
      item.readStatus = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  } catch {
    Message.error('标记已读失败');
  } finally {
    readingId.value = null;
  }
};

/** 全部标记已读 */
const handleMarkAllRead = async () => {
  markAllLoading.value = true;
  try {
    await request.put('/system/alert/read-all');
    alertList.value.forEach(a => { a.readStatus = 1; });
    unreadCount.value = 0;
    Message.success('全部告警已标记为已读');
  } catch {
    Message.error('操作失败');
  } finally {
    markAllLoading.value = false;
  }
};

/** 删除单条告警 */
const handleDelete = async (id: number) => {
  deletingId.value = id;
  try {
    await request.delete(`/system/alert/${id}`);
    alertList.value = alertList.value.filter(a => a.id !== id);
    Message.success('告警记录已删除');
  } catch {
    Message.error('删除失败');
  } finally {
    deletingId.value = null;
  }
};

// ─────────────────────────────────────────────
// 生命周期
// ─────────────────────────────────────────────
onMounted(() => {
  loadAlerts();
  // 每 30 秒增量轮询一次新告警
  pollTimer.value = setInterval(pollNewAlerts, 30000);
});

onUnmounted(() => {
  if (pollTimer.value !== null) {
    clearInterval(pollTimer.value);
    pollTimer.value = null;
  }
});
</script>

<style scoped>
/* ══════════════════════════════════════════════════════════
   BML 告警治理中心 — 全屏零滚动设计
   布局：顶部信息栏 + 主内容（左侧列表 1fr | 右侧面板 280px）
   ══════════════════════════════════════════════════════════ */

.alert-shell {
  height: 100%;
  padding: 14px 18px;
  background: var(--color-bg-1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
}

.alert-aurora {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 90%, rgba(245,63,63,0.04), transparent 45%),
    radial-gradient(circle at 90% 10%, rgba(255,125,0,0.04), transparent 45%);
  pointer-events: none;
  z-index: 0;
}

/* ══ 顶部信息栏 ══ */
.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
  z-index: 2;
  position: relative;
}

.header-left { display: flex; align-items: center; gap: 10px; }

/* 状态指示灯 */
.pulse-dot {
  width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0;
  animation: pulse 2s infinite;
}
.pulse-danger { background: #f53f3f; box-shadow: 0 0 8px rgba(245,63,63,0.7); }
.pulse-ok     { background: #00b42a; box-shadow: 0 0 8px rgba(0,180,42,0.6); }

.title-meta h1 { margin: 0; font-size: 19px; font-weight: 800; color: #0f172a; letter-spacing: -0.01em; }
.title-meta p  { margin: 0; font-size: 11px; color: #64748b; font-weight: 500; }

/* 统计胶囊 */
.header-center { position: absolute; left: 50%; transform: translateX(-50%); }
.stat-rail {
  display: flex; align-items: center;
  background: #fff; padding: 5px 16px; border-radius: 12px;
  border: 1px solid rgba(226,232,240,0.9); box-shadow: 0 2px 10px rgba(15,23,42,0.05);
}
.stat-node { display: flex; align-items: center; gap: 7px; }
.stat-label { font-size: 9px; font-weight: 800; color: #94a3b8; }
.stat-val {
  font-family: 'JetBrains Mono', monospace; font-size: 16px; font-weight: 900; color: #0f172a;
}
.stat-val.danger   { color: #f53f3f; }
.stat-val.critical { color: #f53f3f; }
.stat-val.warning  { color: #ff7d00; }
.stat-val.info     { color: #165dff; }
.stat-gap { width: 1px; height: 20px; background: #e2e8f0; margin: 0 14px; }

/* 操作按钮 */
.header-right { align-self: center; }
.btn-read-all {
  font-weight: 700 !important; border-radius: 8px !important;
  border-color: rgba(22,93,255,0.3) !important; color: #165dff !important;
}

/* ══ 主视口 ══ */
.alert-viewport { flex: 1; min-height: 0; display: flex; flex-direction: column; z-index: 1; }
.alert-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 12px;
  min-height: 0;
}

/* ══ 左侧：告警列表 ══ */
.alert-main { flex: 1; display: flex; flex-direction: column; gap: 10px; min-height: 0; overflow: hidden; }

/* 筛选工具栏 */
.filter-bar {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; padding: 8px 14px; border-radius: 12px;
  border: 1px solid rgba(226,232,240,0.85); box-shadow: 0 2px 8px rgba(15,23,42,0.04);
  flex-shrink: 0;
}
.filter-tabs { display: flex; gap: 4px; }
.filter-tab {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 12px; border-radius: 8px; border: none; cursor: pointer;
  font-size: 12px; font-weight: 700; color: #64748b; background: transparent;
  transition: all 0.15s;
}
.filter-tab:hover { background: #f8fafc; color: #334155; }
.filter-tab.active { background: rgba(22,93,255,0.08); color: #165dff; }
.tab-dot {
  width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
}
.dot-all      { background: #94a3b8; }
.dot-critical { background: #f53f3f; }
.dot-warning  { background: #ff7d00; }
.dot-info     { background: #165dff; }
.tab-count {
  font-size: 10px; font-weight: 900; padding: 0 5px; border-radius: 10px;
  background: rgba(15,23,42,0.06); color: #475569; min-width: 18px; text-align: center;
}
.filter-tab.active .tab-count { background: rgba(22,93,255,0.12); color: #165dff; }
.filter-right { display: flex; align-items: center; }
.search-input { width: 180px; }

/* 告警卡片列表 */
.list-scroll {
  flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px;
  scrollbar-width: thin; min-height: 0;
}

/* 告警卡片 */
.alert-card {
  display: flex; border-radius: 12px;
  background: #fff; border: 1px solid rgba(226,232,240,0.8);
  box-shadow: 0 2px 8px rgba(15,23,42,0.04);
  animation: fadeInUp 0.35s backwards;
  transition: box-shadow 0.2s, transform 0.15s;
  overflow: hidden;
  /* 关键：禁止卡片被 flex 容器压缩，高度由内容自然撑开 */
  flex-shrink: 0;
}
.alert-card:hover { box-shadow: 0 4px 16px rgba(15,23,42,0.08); transform: translateY(-1px); }
.alert-card.card-unread { background: rgba(255,255,255,0.98); }

/* 左侧彩色竖条 */
.card-stripe { width: 4px; flex-shrink: 0; }
.stripe-critical { background: linear-gradient(180deg,#f53f3f,#ff7875); }
.stripe-warning  { background: linear-gradient(180deg,#ff7d00,#ffb454); }
.stripe-info     { background: linear-gradient(180deg,#165dff,#6aa1ff); }

.card-body { flex: 1; padding: 12px 14px; min-width: 0; }

.card-top {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 7px;
}
.card-top-left  { display: flex; align-items: center; gap: 8px; }
.card-top-right { display: flex; align-items: center; gap: 6px; }

/* 级别徽章 */
.level-badge {
  display: flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 800; padding: 2px 8px; border-radius: 6px;
}
.badge-critical { color: #f53f3f; background: rgba(245,63,63,0.08); }
.badge-warning  { color: #ff7d00; background: rgba(255,125,0,0.08); }
.badge-info     { color: #165dff; background: rgba(22,93,255,0.08); }
.badge-critical .arco-icon, .badge-warning .arco-icon, .badge-info .arco-icon { font-size: 11px; }

/* 类型标签 */
.type-chip {
  font-size: 9px; font-weight: 800; color: #94a3b8;
  padding: 1px 6px; border: 1px solid #e2e8f0; border-radius: 4px;
}

/* 未读红点 */
.unread-dot {
  width: 6px; height: 6px; background: #f53f3f; border-radius: 50%;
  box-shadow: 0 0 4px rgba(245,63,63,0.5);
}

.card-time { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: #94a3b8; font-weight: 600; }

.btn-read {
  font-size: 11px !important; font-weight: 700 !important;
  color: #165dff !important; padding: 0 6px !important;
}
.archived-tag {
  font-size: 10px; font-weight: 700; color: #94a3b8;
  padding: 1px 6px; background: #f1f5f9; border-radius: 4px;
}
.btn-delete { color: #94a3b8 !important; padding: 0 4px !important; }
.btn-delete:hover { color: #f53f3f !important; }

.card-title {
  display: block;
  margin: 0 0 5px; font-size: 14px; font-weight: 800; color: #1e293b; line-height: 1.4;
}
.card-content {
  display: block;
  margin: 0; font-size: 12px; color: #64748b; line-height: 1.6;
}

/* 空状态 */
.empty-state {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 0; color: #94a3b8;
}
.empty-icon { font-size: 48px; color: #00b42a; margin-bottom: 12px; }
.empty-icon .arco-icon { font-size: 48px; }
.empty-title { font-size: 16px; font-weight: 800; color: #334155; margin: 0 0 4px; }
.empty-sub   { font-size: 12px; color: #94a3b8; }

/* ══ 右侧：统计面板 ══ */
.alert-aside { display: flex; flex-direction: column; gap: 12px; min-height: 0; }

.lv-card {
  border-radius: 16px; border: 1px solid rgba(226,232,240,0.85);
  background: rgba(255,255,255,0.95); box-shadow: 0 2px 12px rgba(15,23,42,0.06);
  position: relative; overflow: hidden; display: flex; flex-direction: column;
  flex-shrink: 0; padding: 14px;
}
.aside-card-flex { flex: 1; min-height: 0; }

.lv-card__accent           { position: absolute; top: 0; left: 0; right: 0; height: 3px; }
.lv-card__accent--gold     { background: linear-gradient(90deg,#ff7d00,#ffb454); }
.lv-card__accent--teal     { background: linear-gradient(90deg,#14c9c9,#6ee7b7); }
.lv-card__accent--purple   { background: linear-gradient(90deg,#722ed1,#b37feb); }

.aside-title {
  display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
}
.aside-title .arco-icon { color: #165dff; font-size: 14px; }
.aside-title span { font-size: 13px; font-weight: 800; color: #0f172a; }

/* 级别分布 */
.level-dist { display: flex; flex-direction: column; gap: 10px; }
.dist-item  { display: flex; flex-direction: column; gap: 4px; }
.dist-meta  { display: flex; align-items: center; gap: 7px; }
.dist-dot   { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.dot-critical { background: #f53f3f; }
.dot-warning  { background: #ff7d00; }
.dot-info     { background: #165dff; }
.dist-label { font-size: 11px; font-weight: 700; color: #475569; flex: 1; }
.dist-count { font-size: 13px; font-weight: 900; font-family: 'JetBrains Mono', monospace; }
.text-critical { color: #f53f3f; }
.text-warning  { color: #ff7d00; }
.text-info     { color: #165dff; }
.dist-bar-track { height: 4px; background: #f1f5f9; border-radius: 999px; overflow: hidden; }
.dist-bar-fill  { height: 100%; border-radius: 999px; transition: width 0.8s ease; }
.fill-critical { background: linear-gradient(90deg,#f53f3f,#ff7875); }
.fill-warning  { background: linear-gradient(90deg,#ff7d00,#ffb454); }
.fill-info     { background: linear-gradient(90deg,#165dff,#6aa1ff); }

/* 类型分布 */
.type-dist { display: flex; flex-direction: column; gap: 10px; }
.type-item { display: flex; align-items: center; gap: 10px; }
.type-icon-wrap {
  width: 30px; height: 30px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.type-icon-cpu  { background: rgba(22,93,255,0.1);  color: #165dff; }
.type-icon-mem  { background: rgba(20,201,201,0.1); color: #14c9c9; }
.type-icon-disk { background: rgba(255,125,0,0.1);  color: #ff7d00; }
.type-icon-jvm  { background: rgba(114,46,209,0.1); color: #722ed1; }
.type-icon-wrap .arco-icon { font-size: 14px; }
.type-info { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.type-name { font-size: 11px; font-weight: 700; color: #475569; }
.type-bar-track { height: 3px; background: #f1f5f9; border-radius: 999px; overflow: hidden; }
.type-bar-fill  { height: 100%; border-radius: 999px; transition: width 0.8s ease; }
.fill-type-cpu  { background: linear-gradient(90deg,#165dff,#6aa1ff); }
.fill-type-mem  { background: linear-gradient(90deg,#14c9c9,#6ee7b7); }
.fill-type-disk { background: linear-gradient(90deg,#ff7d00,#ffb454); }
.fill-type-jvm  { background: linear-gradient(90deg,#722ed1,#b37feb); }
.type-count { font-size: 14px; font-weight: 900; color: #1e293b; font-family: 'JetBrains Mono', monospace; }

/* 时间线 */
.timeline-scroll { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 0; scrollbar-width: thin; }
.timeline-empty  { font-size: 12px; color: #94a3b8; text-align: center; padding: 16px 0; }
.timeline-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 8px 0; border-bottom: 1px solid rgba(226,232,240,0.5);
}
.timeline-item:last-child { border-bottom: none; }
.tl-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; margin-top: 4px;
}
.tl-dot-critical { background: #f53f3f; box-shadow: 0 0 4px rgba(245,63,63,0.5); }
.tl-dot-warning  { background: #ff7d00; box-shadow: 0 0 4px rgba(255,125,0,0.4); }
.tl-dot-info     { background: #165dff; box-shadow: 0 0 4px rgba(22,93,255,0.4); }
.tl-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.tl-title { font-size: 11px; font-weight: 700; color: #334155; line-height: 1.4; }
.tl-time  { font-size: 10px; color: #94a3b8; font-family: 'JetBrains Mono', monospace; }

/* ══ 动画 ══ */
@keyframes pulse {
  0%,100% { transform: scale(1);    opacity: 1;   }
  50%      { transform: scale(1.25); opacity: 0.65; }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0);   }
}
.entrance-anim-1 { animation: fadeInUp 0.4s backwards; }
.entrance-anim-2 { animation: fadeInUp 0.4s 0.07s backwards; }
.entrance-anim-3 { animation: fadeInUp 0.4s 0.14s backwards; }
</style>
