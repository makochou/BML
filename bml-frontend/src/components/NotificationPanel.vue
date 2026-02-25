<template>
  <!--
    通知面板 — 三栏并排设计
    左栏：日期选择 | 中栏：当日告警列表 | 右栏：告警详情
    三栏同时可见，点击联动
  -->
  <div class="notification-panel">
    <!-- 三栏主体 -->
    <div class="panel-columns">

      <!-- ========== 左栏：日期列表 ========== -->
      <div class="col col-dates">
        <div class="col-label">选择日期</div>
        <div class="dates-scroll" v-if="store.alertDates.length > 0">
          <div
            v-for="date in store.alertDates"
            :key="date"
            :class="['date-item', { active: store.selectedDate === date }]"
            @click="store.selectDate(date)"
          >
            <div class="date-main">{{ formatDateLabel(date) }}</div>
            <div class="date-sub">{{ date }}</div>
          </div>
        </div>
        <div class="col-empty" v-else>
          <span>暂无记录</span>
        </div>
      </div>

      <!-- ========== 中栏：告警列表 ========== -->
      <div class="col col-list">
        <div class="col-label">
          {{ store.selectedDate ? formatDateLabel(store.selectedDate) + ' 的告警' : '请先选择日期' }}
        </div>
        <div class="list-scroll" v-if="store.dateAlerts.length > 0">
          <div
            v-for="alert in store.dateAlerts"
            :key="alert.id"
            :class="['list-item', { active: store.selectedAlert?.id === alert.id, unread: alert.readStatus === 0 }]"
            @click="store.selectAlert(alert)"
          >
            <div :class="['item-dot', `dot-${alert.alertLevel}`]"></div>
            <div class="item-body">
              <div class="item-title">
                {{ alert.alertTitle }}
                <span v-if="alert.readStatus === 0" class="item-unread-dot"></span>
              </div>
              <div class="item-time">{{ extractTime(alert.createTime) }}</div>
            </div>
          </div>
        </div>
        <div class="col-empty" v-else>
          <span>{{ store.selectedDate ? '当天无告警' : '← 请选择日期' }}</span>
        </div>
      </div>

      <!-- ========== 右栏：告警详情 ========== -->
      <div class="col col-detail">
        <div class="col-label">告警详情</div>
        <div class="detail-scroll" v-if="store.selectedAlert">
          <!-- 级别标签 -->
          <div :class="['detail-tag', `tag-${store.selectedAlert.alertLevel}`]">
            <icon-exclamation-circle v-if="store.selectedAlert.alertLevel === 'critical'" />
            <icon-close-circle v-else-if="store.selectedAlert.alertLevel === 'error'" />
            <icon-exclamation v-else-if="store.selectedAlert.alertLevel === 'warning'" />
            <icon-info-circle v-else />
            {{ levelLabel(store.selectedAlert.alertLevel) }}
          </div>

          <!-- 标题 -->
          <h3 class="detail-title">{{ store.selectedAlert.alertTitle }}</h3>

          <!-- 元信息 -->
          <div class="detail-meta">
            <div class="meta-row">
              <icon-clock-circle class="meta-icon" />
              {{ store.selectedAlert.createTime }}
            </div>
            <div class="meta-row">
              <icon-tag class="meta-icon" />
              {{ store.selectedAlert.alertType }}
            </div>
          </div>

          <!-- 内容 -->
          <div class="detail-box">
            {{ store.selectedAlert.alertContent }}
          </div>

          <!-- 删除按钮 -->
          <div class="detail-footer">
            <a-button type="outline" size="small" status="danger" @click="handleDelete">
              <template #icon><icon-delete /></template>
              删除
            </a-button>
          </div>
        </div>
        <div class="col-empty" v-else>
          <span>← 请选择告警</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * NotificationPanel — 三栏并排通知面板
 *
 * 布局：左 (日期) | 中 (列表) | 右 (详情)
 * 三栏同时可见，点击联动切换内容
 */
import {
  IconExclamationCircle, IconCloseCircle, IconExclamation, IconInfoCircle,
  IconDelete, IconClockCircle, IconTag,
} from '@arco-design/web-vue/es/icon';
import { useNotificationStore } from '../store/notification';

const store = useNotificationStore();

/** 日期友好标签 */
const formatDateLabel = (dateStr: string): string => {
  const date = new Date(dateStr + 'T00:00:00');
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const yesterday = new Date(today); yesterday.setDate(yesterday.getDate() - 1);
  if (date.getTime() === today.getTime()) return '今天';
  if (date.getTime() === yesterday.getTime()) return '昨天';
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return `${date.getMonth() + 1}月${date.getDate()}日 ${weekDays[date.getDay()]}`;
};

/** 提取时间 */
const extractTime = (dt: string): string => dt?.split(' ')[1] || dt || '';

/** 级别标签 */
const levelLabel = (l: string) => ({ info: '提示', warning: '警告', error: '错误', critical: '严重' }[l] || l);

/** 删除 → 刷新列表 */
const handleDelete = async () => {
  if (!store.selectedAlert) return;
  await store.deleteAlert(store.selectedAlert.id);
  store.selectedAlert = null;
  if (store.selectedDate) await store.fetchAlertsByDate(store.selectedDate);
};
</script>

<style scoped>
/* ==================== 面板容器 ==================== */
.notification-panel {
  width: 100%;
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  background: transparent;
  overflow: hidden;
}

/* ==================== 面板头部（已移入 drawer title） ==================== */

/* ==================== 三栏布局 ==================== */
.panel-columns {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.col-dates { width: 180px; border-right: 1px solid rgba(0,0,0,0.06); flex-shrink: 0; }
.col-list  { width: 270px; border-right: 1px solid rgba(0,0,0,0.06); flex-shrink: 0; }
.col-detail { flex: 1; }

.col-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #86909c;
  font-weight: 600;
  padding: 10px 14px 6px;
  flex-shrink: 0;
}

.col-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c9cdd4;
  font-size: 13px;
}

/* ==================== 左栏：日期 ==================== */
.dates-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
  scrollbar-width: thin;
}
.dates-scroll::-webkit-scrollbar { width: 3px; }
.dates-scroll::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 3px; }

.date-item {
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.15s;
  border: 1px solid transparent;
}
.date-item:hover { background: rgba(22,93,255,0.04); }
.date-item.active {
  background: rgba(22,93,255,0.08);
  border-color: rgba(22,93,255,0.2);
}
.date-main {
  font-size: 14px; font-weight: 600; color: #1d2129;
}
.date-item.active .date-main { color: #165dff; }
.date-sub {
  font-size: 11px; color: #86909c; margin-top: 2px;
  font-family: 'Courier New', monospace;
}

/* ==================== 中栏：列表 ==================== */
.list-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
  scrollbar-width: thin;
}
.list-scroll::-webkit-scrollbar { width: 3px; }
.list-scroll::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.08); border-radius: 3px; }

.list-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.15s;
  border: 1px solid transparent;
}
.list-item:hover { background: rgba(22,93,255,0.03); }
.list-item.active {
  background: rgba(22,93,255,0.06);
  border-color: rgba(22,93,255,0.15);
}
.list-item.unread { font-weight: 600; }

.item-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.item-dot.dot-info { background: #165dff; }
.item-dot.dot-warning { background: #ff7d00; }
.item-dot.dot-error { background: #f53f3f; }
.item-dot.dot-critical { background: #cb2634; }

.item-body { flex: 1; min-width: 0; }
.item-title {
  font-size: 13px; color: #1d2129;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  display: flex; align-items: center; gap: 4px;
}
.item-unread-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: #165dff; flex-shrink: 0;
  box-shadow: 0 0 4px rgba(22,93,255,0.5);
}
.item-time {
  font-size: 11px; color: #c9cdd4; margin-top: 2px;
  font-family: 'Courier New', monospace;
}

/* ==================== 右栏：详情 ==================== */
.detail-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  scrollbar-width: thin;
}

.detail-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 16px;
  font-size: 11px; font-weight: 600; width: fit-content;
}
.detail-tag.tag-info { background: rgba(22,93,255,0.1); color: #165dff; }
.detail-tag.tag-warning { background: rgba(255,125,0,0.1); color: #ff7d00; }
.detail-tag.tag-error { background: rgba(245,63,63,0.1); color: #f53f3f; }
.detail-tag.tag-critical { background: rgba(203,38,52,0.15); color: #cb2634; }

.detail-title {
  margin: 0; font-size: 16px; font-weight: 700;
  color: #1d2129; line-height: 1.4;
}

.detail-meta { display: flex; flex-direction: column; gap: 6px; }
.meta-row {
  display: flex; align-items: center;
  font-size: 12px; color: #86909c;
}
.meta-icon { margin-right: 5px; font-size: 13px; }

.detail-box {
  background: #f7f8fa; border-radius: 10px;
  padding: 14px; border-left: 3px solid #165dff;
  font-size: 13px; color: #4e5969; line-height: 1.7;
  white-space: pre-wrap; word-break: break-word;
}

.detail-footer {
  display: flex; justify-content: flex-end;
  margin-top: auto; padding-top: 4px;
}

/* ==================== 深色模式 ==================== */
:global(body[arco-theme='dark']) .col-dates, :global(body[arco-theme='dark']) .col-list {
  border-right-color: rgba(255,255,255,0.06);
}
:global(body[arco-theme='dark']) .date-main, :global(body[arco-theme='dark']) .item-title,
:global(body[arco-theme='dark']) .detail-title { color: #f2f3f5; }
:global(body[arco-theme='dark']) .date-item:hover, :global(body[arco-theme='dark']) .list-item:hover {
  background: rgba(255,255,255,0.04);
}
:global(body[arco-theme='dark']) .date-item.active {
  background: rgba(22,93,255,0.12); border-color: rgba(22,93,255,0.3);
}
:global(body[arco-theme='dark']) .list-item.active {
  background: rgba(22,93,255,0.1); border-color: rgba(22,93,255,0.2);
}
:global(body[arco-theme='dark']) .detail-box {
  background: rgba(255,255,255,0.04); color: #c9cdd4;
}
:global(body[arco-theme='dark']) .col-empty { color: #4e5969; }
</style>
