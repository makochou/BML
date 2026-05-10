<template>
  <div class="page-wrapper security-alert-page">
    <GovernanceCompactQueryPanel
      eyebrow="RISK AUDIT"
      title="安全告警 / 风险审计"
      description="聚合服务器资源、许可证变更、异常行为等风险告警，为安全审计和运维值守提供统一视图。"
      density="ultra"
      theme="aurora"
      :meta-items="queryMetaItems"
    >
      <template #actions>
        <a-button @click="handleReset"><template #icon><icon-refresh /></template>重置筛选</a-button>
        <a-button type="primary" :loading="loading" @click="loadData"><template #icon><icon-refresh /></template>刷新告警</a-button>
      </template>
      <a-form :model="filterForm" layout="inline" class="biz-query-form risk-query-form">
        <div class="biz-query-form-primary biz-query-form-grid-4">
          <a-form-item field="level" label="告警级别">
            <a-select v-model="filterForm.level" placeholder="全部级别" allow-clear>
              <a-option value="critical">严重</a-option>
              <a-option value="error">错误</a-option>
              <a-option value="warning">警告</a-option>
              <a-option value="info">提示</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="readStatus" label="阅读状态">
            <a-select v-model="filterForm.readStatus" placeholder="全部状态" allow-clear>
              <a-option :value="0">未读</a-option>
              <a-option :value="1">已读</a-option>
            </a-select>
          </a-form-item>
          <a-form-item field="keyword" label="关键词" class="biz-query-form-span-2">
            <a-input-search v-model="filterForm.keyword" placeholder="搜索告警标题、内容或类型" allow-clear />
          </a-form-item>
        </div>
      </a-form>
    </GovernanceCompactQueryPanel>

    <GovernanceListStage class="risk-list-stage" density="ultra" body-fill :meta-items="tableMetaItems">
      <template #actions>
        <a-tag color="arcoblue">当前筛选 {{ filteredAlerts.length }} 条</a-tag>
        <a-tag color="orangered">未读 {{ unreadFilteredCount }} 条</a-tag>
      </template>

      <div class="risk-list-shell">
        <a-spin :loading="loading" class="risk-list-spin">
          <div class="risk-timeline-board">
            <aside class="risk-calendar-panel">
              <div class="risk-calendar-heading">
                <div>
                  <span>告警日历</span>
                  <b>{{ calendarMonthLabel }}</b>
                </div>
                <div class="risk-calendar-actions">
                  <button type="button" class="risk-calendar-nav" @click="handleCalendarMonthChange(-1)">‹</button>
                  <button type="button" class="risk-calendar-nav" @click="handleCalendarMonthChange(1)">›</button>
                </div>
              </div>

              <div class="risk-calendar-weekdays">
                <span v-for="weekday in calendarWeekdays" :key="weekday">{{ weekday }}</span>
              </div>
              <div class="risk-calendar-grid">
                <button
                  v-for="cell in calendarCells"
                  :key="cell.dateKey"
                  type="button"
                  class="risk-calendar-day"
                  :class="{
                    'is-outside': !cell.currentMonth,
                    'is-selected': cell.dateKey === filterForm.alertDate,
                    'is-today': cell.dateKey === todayDateKey,
                    'has-alert': cell.total > 0,
                    'has-severe': cell.severe > 0,
                  }"
                  @click="handleSelectDate(cell.dateKey)"
                >
                  <span>{{ cell.day }}</span>
                  <em v-if="cell.total">{{ cell.total }}</em>
                </button>
              </div>

              <div class="risk-calendar-summary">
                <strong>{{ selectedDateLabel }}</strong>
                <span>{{ selectedDaySummary.total }} 条 · 高危 {{ selectedDaySummary.severe }} · 未读 {{ selectedDaySummary.unread }}</span>
                <em>最新 {{ selectedDaySummary.latestTime }}</em>
              </div>
              <a-button size="mini" class="risk-today-btn" @click="handleBackToday">回到今天</a-button>
            </aside>

            <div class="risk-timeline-content">
              <section v-if="timelineGroups.length" class="risk-day-section">
                <div v-for="group in timelineGroups" :key="group.dateKey" class="risk-day-track">
                  <article v-for="item in group.items" :key="item.id" class="risk-timeline-item">
                    <div class="risk-time-axis">
                      <span class="risk-time">{{ formatAlertTime(item.createTime) }}</span>
                      <span class="risk-dot" :class="`level-${normalizeLevel(item.alertLevel) || 'unknown'}`" />
                    </div>
                    <div
                      class="risk-alert-card"
                      :class="`level-${normalizeLevel(item.alertLevel) || 'unknown'}`"
                      role="button"
                      tabindex="0"
                      @click="handleView(item)"
                      @keydown.enter="handleView(item)"
                      @keydown.space.prevent="handleView(item)"
                    >
                      <div class="risk-alert-main">
                        <div class="risk-alert-title-row">
                          <a-tag :color="levelColor(item.alertLevel)">{{ levelLabel(item.alertLevel) }}</a-tag>
                          <h4>{{ item.alertTitle || '未命名告警' }}</h4>
                        </div>
                        <p>{{ item.alertContent || '暂无告警详情' }}</p>
                        <div class="risk-alert-tags">
                          <a-tag color="arcoblue">{{ typeLabel(item.alertType) }}</a-tag>
                          <a-tag color="gray">{{ item.alertType || 'UNKNOWN' }}</a-tag>
                          <a-tag :color="item.readStatus === 0 ? 'orange' : 'green'">{{ item.readStatus === 0 ? '未读' : '已读' }}</a-tag>
                        </div>
                      </div>
                      <div class="risk-alert-action">查看详情</div>
                    </div>
                  </article>
                </div>
              </section>
              <a-empty v-else class="risk-empty" description="当前日期暂无匹配的风险告警" />
            </div>
          </div>
        </a-spin>
      </div>

      <div class="biz-table-footer">
        <div class="biz-table-footer__stats">
          <span class="biz-table-footer__total">共 <b>{{ alerts.length }}</b> 条</span>
          <a-divider direction="vertical" />
          <span class="stat-disabled">高危 <b>{{ severeFilteredCount }}</b></span>
          <a-divider direction="vertical" />
          <span class="stat-locked">未读 <b>{{ unreadFilteredCount }}</b></span>
          <a-divider direction="vertical" />
          <span>当前筛选 <b>{{ filteredAlerts.length }}</b> 条</span>
        </div>
      </div>
    </GovernanceListStage>

    <a-drawer :visible="detailVisible" width="560px" title="风险告警详情" unmount-on-close @cancel="handleDetailCancel">
      <a-descriptions v-if="currentRecord" :column="1" bordered>
        <a-descriptions-item label="告警标题">{{ currentRecord.alertTitle }}</a-descriptions-item>
        <a-descriptions-item label="告警级别"><a-tag :color="levelColor(currentRecord.alertLevel)">{{ levelLabel(currentRecord.alertLevel) }}</a-tag></a-descriptions-item>
        <a-descriptions-item label="告警类型">{{ currentRecord.alertType }}</a-descriptions-item>
        <a-descriptions-item label="阅读状态">{{ currentRecord.readStatus === 0 ? '未读' : '已读' }}</a-descriptions-item>
        <a-descriptions-item label="发生时间">{{ currentRecord.createTime }}</a-descriptions-item>
        <a-descriptions-item label="详情"><a-typography-paragraph copyable class="risk-detail-text">{{ currentRecord.alertContent }}</a-typography-paragraph></a-descriptions-item>
      </a-descriptions>
      <template #footer>
        <div class="risk-detail-footer">
          <a-button @click="handleDetailCancel">取消</a-button>
          <a-button type="primary" :loading="readConfirmLoading" @click="handleConfirmRead">
            确定
          </a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message } from '@arco-design/web-vue';
import { IconRefresh } from '@arco-design/web-vue/es/icon';
import GovernanceCompactQueryPanel from '../../../../components/governance/GovernanceCompactQueryPanel.vue';
import GovernanceListStage from '../../../../components/governance/GovernanceListStage.vue';
import { fetchSecurityAlerts, markSecurityAlertRead, type SecurityAlertVO } from '../../../../api/system';

type RiskFilterForm = {
  level?: string;
  readStatus?: number;
  keyword: string;
  alertDate: string;
};

type TimelineGroup = {
  dateKey: string;
  dateLabel: string;
  items: SecurityAlertVO[];
  severe: number;
  unread: number;
  latestTime: string;
};

type CalendarCell = {
  dateKey: string;
  day: number;
  currentMonth: boolean;
  total: number;
  severe: number;
  unread: number;
};

const loading = ref(false);
const alerts = ref<SecurityAlertVO[]>([]);
const padTime = (value: number) => String(value).padStart(2, '0');
const formatDateObject = (date: Date) => `${date.getFullYear()}-${padTime(date.getMonth() + 1)}-${padTime(date.getDate())}`;
const getTodayDateKey = () => formatDateObject(new Date());
const createMonthDate = (dateKey: string) => {
  const [year, month] = dateKey.split('-').map(Number);
  return new Date(year, month - 1, 1);
};
const todayDateKey = getTodayDateKey();
const calendarWeekdays = ['日', '一', '二', '三', '四', '五', '六'];
const calendarMonth = ref(createMonthDate(todayDateKey));
const filterForm = reactive<RiskFilterForm>({
  level: undefined,
  readStatus: undefined,
  keyword: '',
  alertDate: getTodayDateKey(),
});
const detailVisible = ref(false);
const currentRecord = ref<SecurityAlertVO | null>(null);
const readConfirmLoading = ref(false);

const filteredAlerts = computed(() => {
  const normalizedKeyword = filterForm.keyword.trim().toLowerCase();
  return alerts.value.filter(item => {
    const matchLevel = !filterForm.level || normalizeLevel(item.alertLevel) === filterForm.level;
    const matchReadStatus = filterForm.readStatus === undefined || item.readStatus === filterForm.readStatus;
    const matchDate = isInSelectedDate(item.createTime);
    const text = `${item.alertTitle || ''} ${item.alertContent || ''} ${item.alertType || ''}`.toLowerCase();
    const matchKeyword = !normalizedKeyword || text.includes(normalizedKeyword);
    return matchLevel && matchReadStatus && matchDate && matchKeyword;
  });
});

const unreadCount = computed(() => alerts.value.filter(item => item.readStatus === 0).length);
const severeCount = computed(() => alerts.value.filter(item => isSevereLevel(item.alertLevel)).length);
const unreadFilteredCount = computed(() => filteredAlerts.value.filter(item => item.readStatus === 0).length);
const severeFilteredCount = computed(() => filteredAlerts.value.filter(item => isSevereLevel(item.alertLevel)).length);
const sortedAlerts = computed(() => [...filteredAlerts.value].sort((left, right) => getAlertTimestamp(right.createTime) - getAlertTimestamp(left.createTime)));
const selectedDateLabel = computed(() => formatDateLabel(filterForm.alertDate));
const selectedDaySummary = computed(() => ({
  total: filteredAlerts.value.length,
  severe: severeFilteredCount.value,
  unread: unreadFilteredCount.value,
  latestTime: timelineGroups.value[0]?.latestTime || '--:--',
}));
const calendarMonthLabel = computed(() => `${calendarMonth.value.getFullYear()}年${padTime(calendarMonth.value.getMonth() + 1)}月`);
const calendarCells = computed<CalendarCell[]>(() => {
  const year = calendarMonth.value.getFullYear();
  const month = calendarMonth.value.getMonth();
  const firstDate = new Date(year, month, 1);
  const startDate = new Date(year, month, 1 - firstDate.getDay());
  return Array.from({ length: 42 }, (_, index) => {
    const cellDate = new Date(startDate);
    cellDate.setDate(startDate.getDate() + index);
    const dateKey = formatDateObject(cellDate);
    const dayAlerts = alerts.value.filter(item => getAlertDateKey(item.createTime) === dateKey);
    return {
      dateKey,
      day: cellDate.getDate(),
      currentMonth: cellDate.getMonth() === month,
      total: dayAlerts.length,
      severe: dayAlerts.filter(item => isSevereLevel(item.alertLevel)).length,
      unread: dayAlerts.filter(item => item.readStatus === 0).length,
    };
  });
});
const timelineGroups = computed<TimelineGroup[]>(() => {
  const groupMap = new Map<string, SecurityAlertVO[]>();
  sortedAlerts.value.forEach(item => {
    const dateKey = getAlertDateKey(item.createTime);
    groupMap.set(dateKey, [...(groupMap.get(dateKey) || []), item]);
  });
  return Array.from(groupMap.entries()).map(([dateKey, items]) => ({
    dateKey,
    dateLabel: formatDateLabel(dateKey),
    items,
    severe: items.filter(item => isSevereLevel(item.alertLevel)).length,
    unread: items.filter(item => item.readStatus === 0).length,
    latestTime: formatAlertTime(items[0]?.createTime),
  }));
});

const queryMetaItems = computed(() => [
  { label: '告警总数', value: alerts.value.length, tone: 'blue' as const },
  { label: '未读告警', value: unreadCount.value, tone: 'gold' as const },
  { label: '严重/错误', value: severeCount.value, tone: 'violet' as const },
]);

const tableMetaItems = computed(() => [
  { label: '当前筛选', value: String(filteredAlerts.value.length), tone: 'blue' as const },
  { label: '高危告警', value: String(severeFilteredCount.value), tone: 'gold' as const },
]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchSecurityAlerts({ limit: 100 }) as any;
    alerts.value = res.data || [];
  } catch (error) {
    Message.error('加载风险告警失败');
  } finally {
    loading.value = false;
  }
};

const handleReset = () => {
  filterForm.level = undefined;
  filterForm.readStatus = undefined;
  filterForm.keyword = '';
  filterForm.alertDate = getTodayDateKey();
  calendarMonth.value = createMonthDate(filterForm.alertDate);
};
const handleBackToday = () => {
  filterForm.alertDate = getTodayDateKey();
  calendarMonth.value = createMonthDate(filterForm.alertDate);
};
const handleCalendarMonthChange = (offset: number) => {
  calendarMonth.value = new Date(calendarMonth.value.getFullYear(), calendarMonth.value.getMonth() + offset, 1);
};
const handleSelectDate = (dateKey: string) => {
  filterForm.alertDate = dateKey;
  calendarMonth.value = createMonthDate(dateKey);
};
const handleView = (record: SecurityAlertVO) => { currentRecord.value = record; detailVisible.value = true; };
const handleDetailCancel = () => { detailVisible.value = false; };
const handleConfirmRead = async () => {
  const record = currentRecord.value;
  if (!record) {
    detailVisible.value = false;
    return;
  }
  if (record.readStatus !== 0) {
    detailVisible.value = false;
    return;
  }
  readConfirmLoading.value = true;
  try {
    await markSecurityAlertRead(record.id);
    const alert = alerts.value.find(item => item.id === record.id);
    if (alert) {
      alert.readStatus = 1;
    }
    currentRecord.value = { ...record, readStatus: 1 };
    Message.success('风险告警已标记为已读');
    detailVisible.value = false;
  } catch (error) {
    Message.error('标记风险告警已读失败');
  } finally {
    readConfirmLoading.value = false;
  }
};
const normalizeLevel = (level?: string) => (level || '').toLowerCase();
const isSevereLevel = (level?: string) => ['critical', 'error'].includes(normalizeLevel(level));
const levelLabel = (level?: string) => ({ critical: '严重', error: '错误', warning: '警告', info: '提示' } as Record<string, string>)[normalizeLevel(level)] || level || '未知';
const levelColor = (level?: string) => ({ critical: 'red', error: 'red', warning: 'orange', info: 'blue' } as Record<string, string>)[normalizeLevel(level)] || 'gray';
const typeLabel = (type?: string) => ({
  CPU_HIGH: 'CPU 负载',
  MEMORY_HIGH: '内存风险',
  DISK_FULL: '磁盘空间',
  JVM_HIGH: 'JVM 风险',
  LICENSE_EXPIRE: '许可证',
} as Record<string, string>)[type || ''] || type || '未知类型';
const getAlertTimestamp = (value?: string) => {
  const matched = (value || '').match(/^(\d{4})-(\d{1,2})-(\d{1,2})(?:[ T](\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/);
  if (!matched) {
    return 0;
  }
  const [, year, month, day, hour = '0', minute = '0', second = '0'] = matched;
  return new Date(Number(year), Number(month) - 1, Number(day), Number(hour), Number(minute), Number(second)).getTime();
};
const isInSelectedDate = (value?: string) => {
  if (!filterForm.alertDate) {
    return true;
  }
  return getAlertDateKey(value) === filterForm.alertDate;
};
const getAlertDateKey = (value?: string) => {
  const timestamp = getAlertTimestamp(value);
  if (!timestamp) {
    return 'unknown';
  }
  const date = new Date(timestamp);
  return `${date.getFullYear()}-${padTime(date.getMonth() + 1)}-${padTime(date.getDate())}`;
};
const formatDateLabel = (dateKey: string) => {
  if (dateKey === 'unknown') {
    return '未知日期';
  }
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  const todayKey = `${today.getFullYear()}-${padTime(today.getMonth() + 1)}-${padTime(today.getDate())}`;
  const yesterdayKey = `${yesterday.getFullYear()}-${padTime(yesterday.getMonth() + 1)}-${padTime(yesterday.getDate())}`;
  if (dateKey === todayKey) {
    return '今天';
  }
  if (dateKey === yesterdayKey) {
    return '昨天';
  }
  const [, month, day] = dateKey.split('-');
  return `${Number(month)}月${Number(day)}日`;
};
const formatAlertTime = (value?: string) => {
  const timestamp = getAlertTimestamp(value);
  if (!timestamp) {
    return '--:--';
  }
  const date = new Date(timestamp);
  return `${padTime(date.getHours())}:${padTime(date.getMinutes())}`;
};

onMounted(loadData);
</script>

<style scoped>
.security-alert-page { min-height: 100%; }
.risk-query-form { width: 100%; }
.risk-list-stage :deep(.governance-list-stage__body) { overflow: hidden; }
.risk-list-shell { display: flex; flex: 1; min-height: 0; }
.risk-list-spin { display: flex; flex: 1; min-height: 0; width: 100%; }
.risk-list-spin :deep(.arco-spin-children) { display: flex; flex: 1; min-height: 0; flex-direction: column; width: 100%; }
.risk-timeline-board { display: grid; grid-template-columns: 260px minmax(0, 1fr); gap: 14px; flex: 1; min-height: 0; overflow: hidden; padding: 2px 6px 12px; }
.risk-calendar-panel { align-self: start; display: flex; flex-direction: column; gap: 10px; padding: 12px; border-radius: 22px; border: 1px solid rgba(147, 197, 253, 0.48); background: radial-gradient(circle at 10% 0%, rgba(59,130,246,0.14), transparent 34%), linear-gradient(180deg, rgba(255,255,255,0.98), rgba(238,247,255,0.96)); box-shadow: 0 18px 42px rgba(37, 99, 235, 0.1), inset 0 1px 0 rgba(255,255,255,0.94); }
.risk-calendar-heading { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.risk-calendar-heading span { display: block; color: #64748b; font-size: 11px; font-weight: 800; letter-spacing: 0.08em; }
.risk-calendar-heading b { display: block; margin-top: 2px; color: #0f172a; font-size: 17px; }
.risk-calendar-actions { display: inline-flex; gap: 6px; }
.risk-calendar-nav { width: 28px; height: 28px; border: 1px solid rgba(148, 163, 184, 0.3); border-radius: 999px; background: rgba(255,255,255,0.88); color: #475569; font-size: 18px; line-height: 1; cursor: pointer; transition: all 0.18s ease; }
.risk-calendar-nav:hover { border-color: rgba(59,130,246,0.45); color: #2563eb; box-shadow: 0 8px 18px rgba(37,99,235,0.12); }
.risk-calendar-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; padding: 6px 2px 0; color: #94a3b8; font-size: 11px; font-weight: 800; text-align: center; }
.risk-calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.risk-calendar-day { position: relative; display: flex; align-items: center; justify-content: center; width: 100%; aspect-ratio: 1; border: 1px solid transparent; border-radius: 12px; background: transparent; color: #334155; font-size: 12px; font-weight: 800; cursor: pointer; transition: all 0.18s ease; }
.risk-calendar-day:hover { background: rgba(59,130,246,0.08); color: #2563eb; }
.risk-calendar-day.is-outside { color: #cbd5e1; }
.risk-calendar-day.is-today { border-color: rgba(20,184,166,0.38); color: #0f766e; }
.risk-calendar-day.is-selected { border-color: rgba(37,99,235,0.42); background: linear-gradient(135deg, #2563eb, #14b8a6); color: #ffffff; box-shadow: 0 10px 20px rgba(37,99,235,0.22); }
.risk-calendar-day.has-alert::after { content: ''; position: absolute; bottom: 4px; width: 4px; height: 4px; border-radius: 999px; background: #38bdf8; }
.risk-calendar-day.has-severe::after { background: #f97316; }
.risk-calendar-day.is-selected::after { background: rgba(255,255,255,0.9); }
.risk-calendar-day em { position: absolute; top: 2px; right: 2px; min-width: 14px; height: 14px; padding: 0 4px; border-radius: 999px; background: rgba(248,113,113,0.12); color: #ef4444; font-size: 9px; font-style: normal; line-height: 14px; }
.risk-calendar-day.is-selected em { background: rgba(255,255,255,0.2); color: #ffffff; }
.risk-calendar-summary { display: flex; flex-direction: column; gap: 5px; padding: 10px; border-radius: 16px; background: rgba(255, 255, 255, 0.78); box-shadow: inset 0 1px 0 rgba(255,255,255,0.88); }
.risk-calendar-summary strong { color: #0f172a; font-size: 15px; }
.risk-calendar-summary span { color: #64748b; font-size: 12px; line-height: 1.55; }
.risk-calendar-summary em { color: #2563eb; font-size: 12px; font-style: normal; font-weight: 800; }
.risk-today-btn { align-self: stretch; border-radius: 999px; background: linear-gradient(180deg, rgba(255,255,255,0.96), rgba(241,248,255,0.94)); font-weight: 800; }
.risk-timeline-content { display: flex; min-width: 0; min-height: 0; overflow: auto; padding: 0 4px 24px 0; box-sizing: border-box; scroll-padding-bottom: 24px; }
.risk-day-section { flex: 1; min-width: 0; }
.risk-day-track { position: relative; display: flex; flex-direction: column; gap: 10px; min-width: 0; padding: 0 0 28px 14px; }
.risk-day-track::before { content: ''; position: absolute; top: 8px; bottom: 8px; left: 86px; width: 2px; border-radius: 999px; background: linear-gradient(180deg, rgba(59,130,246,0.22), rgba(20,184,166,0.12)); }
.risk-timeline-item { position: relative; display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: 10px; align-items: start; }
.risk-time-axis { position: relative; z-index: 1; display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-height: 42px; }
.risk-time { padding: 4px 8px; border-radius: 999px; background: rgba(255,255,255,0.96); color: #64748b; font-size: 12px; font-weight: 800; box-shadow: 0 6px 16px rgba(15,23,42,0.06); }
.risk-dot { width: 10px; height: 10px; border: 2px solid #fff; border-radius: 50%; background: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,0.12); }
.risk-dot.level-critical, .risk-dot.level-error { background: #ef4444; box-shadow: 0 0 0 4px rgba(239,68,68,0.14); }
.risk-dot.level-warning { background: #f59e0b; box-shadow: 0 0 0 4px rgba(245,158,11,0.15); }
.risk-dot.level-info { background: #0ea5e9; box-shadow: 0 0 0 4px rgba(14,165,233,0.14); }
.risk-alert-card { position: relative; display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; min-height: 74px; padding: 10px 12px 10px 16px; overflow: hidden; border-radius: 16px; border: 1px solid rgba(217, 226, 237, 0.92); background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(248,251,255,0.98)); box-shadow: 0 10px 22px rgba(15,23,42,0.06); cursor: pointer; transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease; }
.risk-alert-card::before { content: ''; position: absolute; inset: 0 auto 0 0; width: 4px; background: linear-gradient(180deg, #3b82f6, #14b8a6); }
.risk-alert-card.level-critical::before, .risk-alert-card.level-error::before { background: linear-gradient(180deg, #ef4444, #f97316); }
.risk-alert-card.level-warning::before { background: linear-gradient(180deg, #f59e0b, #f97316); }
.risk-alert-card.level-info::before { background: linear-gradient(180deg, #3b82f6, #06b6d4); }
.risk-alert-card:hover { transform: translateY(-2px); border-color: rgba(59,130,246,0.3); box-shadow: 0 18px 38px rgba(15,23,42,0.12); }
.risk-alert-main { min-width: 0; }
.risk-alert-title-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.risk-alert-title-row h4 { margin: 0; overflow: hidden; color: #0f172a; font-size: 15px; font-weight: 800; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
.risk-alert-card p { margin: 6px 0 8px; overflow: hidden; color: #475569; font-size: 12px; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.risk-alert-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.risk-alert-action { align-self: center; padding: 6px 10px; border-radius: 999px; background: rgba(37, 99, 235, 0.08); color: #2563eb; font-size: 12px; font-weight: 800; white-space: nowrap; }
.risk-empty { display: flex; flex: 1; align-items: center; justify-content: center; min-height: 320px; }
.risk-detail-text { white-space: pre-wrap; word-break: break-all; }
.risk-detail-footer { display: flex; justify-content: flex-end; gap: 10px; }
.risk-detail-footer :deep(.arco-btn) { min-width: 84px; border-radius: 999px; font-weight: 800; }
.risk-detail-footer :deep(.arco-btn-primary) { border: 0; background: linear-gradient(135deg, #1769ff, #12b8a6); box-shadow: 0 12px 24px rgba(23, 105, 255, 0.18); }
@media (max-width: 1180px) {
  .risk-timeline-board { grid-template-columns: 1fr; }
  .risk-calendar-panel { position: relative; top: auto; }
}
@media (max-width: 768px) {
  .risk-day-track { padding: 0 0 28px; }
  .risk-day-track::before { left: 6px; }
  .risk-timeline-item { grid-template-columns: 1fr; }
  .risk-time-axis { justify-content: flex-start; padding-left: 16px; min-height: auto; }
  .risk-alert-card { grid-template-columns: 1fr; }
  .risk-alert-action { justify-self: flex-start; }
}
</style>
