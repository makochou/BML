<template>
  <div class="h-notification-panel">
    
    <!-- 幻彩高能头部 / 动态导航栏 -->
    <div class="h-header h-notify-header">
      <div class="h-glow-orb orb-1"></div>
      <div class="h-glow-orb orb-2"></div>
      
      <div class="h-header-inner">
         <div class="h-brand">
            <div class="h-icon-wrapper bell-wiggle">
               <icon-notification />
               <a-badge :count="store.unreadCount" :max-count="99" dot :offset="[-2, 2]" />
            </div>
            
            <div class="h-title-group">
               <h2 class="h-title">系统告警看板</h2>
               <p class="h-sub">NOTIFICATIONS CENTER</p>
            </div>
         </div>
         
         <div class="h-header-right-group">
            <!-- 快捷操作区 移入顶部 Header -->
            <div class="h-header-actions">
              <button class="h-btn-text" @click="handleMarkAllRead" :disabled="store.unreadCount === 0">
                 <icon-check-circle /> 全部已读
              </button>
            </div>
            
            <div class="h-close" @click="store.closeDrawer()">
               <icon-close />
            </div>
         </div>
      </div>
    </div>

    <!-- 3列弹性容器 -->
    <div class="h-columns">
        
        <!-- ================= Column 1: 日历与日期聚合 ================= -->
        <div class="h-col h-col-dates">
          <div class="h-calendar-wrapper">
             <a-calendar 
                :key="calendarKey"
                v-model="pickedDate" 
                :panel="true" 
                :modes="['month']"
                @panel-change="handlePanelChange"
             >
                 <template #header>
                   <a-month-picker 
                     :model-value="`${currentPanelDate.getFullYear()}-${String(currentPanelDate.getMonth() + 1).padStart(2, '0')}`" 
                     @change="handleMonthPickerChange" 
                     :allow-clear="false" 
                     position="bl"
                   >
                      <div class="calendar-title h-month-trigger">
                         {{ currentPanelDate.getFullYear() }}年{{ currentPanelDate.getMonth() + 1 }}月
                         <icon-down class="h-trigger-arrow" />
                      </div>
                   </a-month-picker>
                </template>
                <template #default="{ year, month, date }">
                    <div class="arco-calendar-date h-custom-date" @click.stop="handleDateSelectFromSlot(year, month, date)">
                       <div class="arco-calendar-date-value">{{ date }}</div>
                       <div class="h-cell-indicator">
                          <!-- month from arco slot is already 1-indexed (1=Jan, 2=Feb) -->
                          <div v-if="hasAlertForDate(year, month, date)" class="h-dot error"></div>
                       </div>
                    </div>
                </template>
             </a-calendar>
          </div>

          <div class="h-date-legend">
              <div class="legend-item"><span class="h-dot error"></span> 存在系统告警</div>
          </div>
        </div>


        <!-- ================= Column 2: 告警瀑布流 (条件显示或空状态) ================= -->
        <div class="h-col h-col-list">
          <div class="h-col-header" v-if="store.selectedDate">
            <!-- 已移除冗余的日期标题 -->
            <!-- 添加过滤按钮组 -->
            <div class="list-filters">
                <a-radio-group v-model="listFilter" type="button" size="small">
                    <a-radio value="all">全部</a-radio>
                    <a-radio value="unread">未读</a-radio>
                    <a-radio value="read">已读</a-radio>
                </a-radio-group>
            </div>
          </div>
          
          <div class="h-scrollable no-top-pad" v-if="store.selectedDate">
            <transition-group name="h-stagger" tag="div" class="h-list" v-if="filteredDateAlerts.length > 0">
              <div 
                v-for="alert in filteredDateAlerts" 
                :key="alert.id"
                class="h-n-card compact"
                :class="[`is-${alert.alertLevel}`, { 'is-read': alert.readStatus === 1 }, { 'is-selected': store.selectedAlert?.id === alert.id }]"
                @click="store.selectAlert(alert)"
              >
                <div class="h-neon-bar"></div>
                
                <div class="h-n-content">
                  <div class="h-n-row">
                     <div class="h-tag-combo">
                       <!-- 隐藏类型字眼，只留图标缩小体积 -->
                       <div class="h-level-icon"><component :is="getIconForLevel(alert.alertLevel)" /></div>
                       <!-- 标题同行显示 -->
                       <h3 class="h-n-title">{{ alert.alertTitle }}</h3>
                     </div>
                     <span class="h-time">{{ formatTime(alert.createTime) }}</span>
                  </div>
                  
                  <div class="h-card-bottom">
                     <!-- 隐藏长串内容 <p class="h-n-desc">{{ alert.alertContent }}</p> -->
                      <span class="h-tag-text">{{ alert.alertType }}</span>
                     <div class="h-card-badge">
                        <div class="h-dot" v-if="alert.readStatus === 0"></div>
                        <span>{{ alert.readStatus === 0 ? 'URGENT' : 'RESOLVED' }}</span>
                     </div>
                  </div>
                </div>
              </div>
            </transition-group>
            
            <div class="h-list-footer" v-if="filteredDateAlerts.length > 0">
               共 {{ filteredDateAlerts.length }} 条告警记录
            </div>
            
            <div class="h-empty-art min-padding" v-else>
               <p class="h-empty-desc">{{ store.dateAlerts.length > 0 ? '没有符合当前过滤条件的记录。' : '该日期下没有系统告警记录。' }}</p>
            </div>
          </div>

          <!-- 未选中日期提示 -->
          <div class="h-col-placeholder" v-else>
              <icon-desktop />
              <p>请在左侧选择日期以查看对应的告警列表</p>
          </div>
        </div>


        <!-- ================= Column 3: 告警详情 (条件显示) ================= -->
        <div class="h-col h-col-detail">
          <div class="h-scrollable detail-scroll-area" v-if="store.selectedAlert">
              <!-- 详情卡片主区块 -->
              <div class="h-detail-box" :class="`is-${store.selectedAlert.alertLevel}`">
                 <div class="h-detail-box-bg"></div>
                 
                 <div class="h-detail-header-block">
                    <div class="h-d-icon">
                       <component :is="getIconForLevel(store.selectedAlert.alertLevel)" />
                    </div>
                    <h2 class="h-d-title">{{ store.selectedAlert.alertTitle }}</h2>
                 </div>

                 <!-- 核心元数据面板 -->
                 <div class="h-d-meta-panel">
                    <div class="d-meta-item">
                       <span class="m-label">记录 ID</span>
                       <span class="m-value id-text">#{{ store.selectedAlert.id }}</span>
                    </div>
                    <div class="d-meta-item">
                       <span class="m-label">类型</span>
                       <div class="h-tag-combo"><span class="h-tag-text">{{ store.selectedAlert.alertType }}</span></div>
                    </div>
                    <div class="d-meta-item">
                       <span class="m-label">时间</span>
                       <span class="m-value time-val"><icon-clock-circle /> {{ store.selectedAlert.createTime }}</span>
                    </div>
                    <div class="d-meta-item">
                       <span class="m-label">状态</span>
                       <span class="m-value status-val resolved">
                          <icon-check-circle-fill /> {{ store.selectedAlert.readStatus === 1 ? 'RESOLVED' : 'UNREAD' }}
                       </span>
                    </div>
                 </div>

                 <div class="h-d-divider"></div>

                 <!-- 详情正文 -->
                 <div class="h-d-content-block">
                    <h4 class="content-title">告警详情内容</h4>
                    <div class="content-body">{{ store.selectedAlert.alertContent }}</div>
                 </div>
              </div>
          </div>

          <!-- 未选中特定告警提示 -->
          <div class="h-col-placeholder" v-else>
             <icon-file />
             <p>请在中间列表选择告警以审查详细信息</p>
          </div>
        </div>

    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, watch, computed, onMounted } from 'vue';
import { useNotificationStore } from '../store/notification';
import { 
  IconInfoCircleFill, IconExclamationCircleFill, 
  IconCloseCircleFill, IconNotification, IconClose,
  IconCheckCircle, IconCheckCircleFill,
  IconClockCircle, IconDown,
  IconDesktop, IconFile
} from '@arco-design/web-vue/es/icon';
import { Message } from '@arco-design/web-vue';

const store = useNotificationStore();

// 用于强制重绘日历的 key
const calendarKey = ref(0);

// 日期选择器的双向绑定变量
const pickedDate = ref(new Date());

// 当前日历正在展示的面板月份标点
const currentPanelDate = ref(new Date());

// 面板切换事件（点击左右箭头触发）
const handlePanelChange = (date: Date) => {
    currentPanelDate.value = date;
};

// 列表过滤变量：'all' | 'unread' | 'read'
const listFilter = ref('all');

onMounted(() => {
    store.fetchAlertDates();
    store.fetchUnreadCount();
});

// 计算属性：根据过滤条件返回告警列表
const filteredDateAlerts = computed(() => {
    if (listFilter.value === 'all') return store.dateAlerts;
    if (listFilter.value === 'unread') return store.dateAlerts.filter(a => a.readStatus === 0);
    if (listFilter.value === 'read') return store.dateAlerts.filter(a => a.readStatus === 1);
    return store.dateAlerts;
});

// 判断日期是否有告警
const hasAlertForDate = (year: number, month: number, date: number) => {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
    return store.alertDates.includes(dateStr);
};

// 手动处理来自自定义插槽的日期点击（Arco calendar 在使用自定义 default 插槽时会丢失 click 事件）
const handleDateSelectFromSlot = async (year: number, month: number, date: number) => {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
    
    // 更新日历 v-model 用于高亮
    pickedDate.value = new Date(dateStr + 'T00:00:00');
    
    // 更新 store 数据
    if (store.selectedDate !== dateStr) {
        await store.selectDate(dateStr);
    }
};

// 监听 pickedDate（日历内部 v-model，如上/下月切换由于 arco 内置按钮不走 slot 触发）
watch(() => pickedDate.value, async (newDate) => {
    if (!newDate) return;
    const year = newDate.getFullYear();
    const month = String(newDate.getMonth() + 1).padStart(2, '0');
    const day = String(newDate.getDate()).padStart(2, '0');
    const dateString = `${year}-${month}-${day}`;
    
    if (store.selectedDate !== dateString) {
        await store.selectDate(dateString);
    }
});

// 处理由月份快速选择器触发的更改
const handleMonthPickerChange = async (date: any) => {
    if (!date) return;
    
    // date 通常等于 'YYYY-MM'
    const dateObj = new Date(date);
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, '0');
    // 默认跳转到该月的第一天
    const dateString = `${year}-${month}-01`;
    
    // 更新外部绑定的 pickedDate
    pickedDate.value = new Date(dateString);
    currentPanelDate.value = new Date(dateString);
    await store.selectDate(dateString);
    
    // 由于 arco-calendar 不会自动根据外部 v-model 修改内置的视图月份，我们通过更改 key 强制其重载到选中月份
    calendarKey.value++;
};

// 监听 selectedDate，以便同步选择器回显（当点击列表而不是使用选择器时）
watch(() => store.selectedDate, (newVal) => {
    if (newVal) {
        const currentPicked = pickedDate.value;
        const year = currentPicked.getFullYear();
        const month = String(currentPicked.getMonth() + 1).padStart(2, '0');
        const day = String(currentPicked.getDate()).padStart(2, '0');
        const currentStr = `${year}-${month}-${day}`;
        
        // 只有当 store 中的选中日期与当前日历日期的字符串不相同时，才重新复制和刷新，防止因 v-model 与 selectedDate 的相互触发引起 reactivity loop 和重绘失败
        if (currentStr !== newVal) {
            pickedDate.value = new Date(newVal + 'T00:00:00');
            calendarKey.value++; // 强制日历组件刷新以高亮新日期
        }
    } else {
        pickedDate.value = new Date();
        calendarKey.value++;
    }
});

// 工具：时间格式化
const formatTime = (timeStr: string) => {
    const d = new Date(timeStr);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
        return `今日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    }
    return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`;
};

// 工具：获取各级别图标
const getIconForLevel = (level: string) => {
    switch(level) {
        case 'info': return IconInfoCircleFill;
        case 'warning': return IconExclamationCircleFill;
        case 'error': return IconCloseCircleFill;
        case 'critical': return IconCloseCircleFill;
        default: return IconInfoCircleFill;
    }
};

// 一键全部已读
const handleMarkAllRead = async () => {
    if (store.unreadCount === 0) return;
    await store.markAllAsRead();
    Message.success('系统：所有告警已被标记为已处理');
};
</script>

<style scoped>
/* ================= 全局容器 ================= */
.h-notification-panel {
    display: flex; flex-direction: column; height: 100%;
    background: #fdfdfd; 
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
    overflow: hidden;
}

/* 3列弹性容器设定 */
.h-columns {
    flex: 1; display: flex; flex-direction: row; overflow: hidden;
    background: #f4f5f7; /* 背景稍微暗一点作为分界线 */
}

.h-col {
    display: flex; flex-direction: column; overflow: hidden;
    background: #fdfdfd; 
}
/* 列宽度分配 */
.h-col-dates { width: 260px; flex-shrink: 0; border-right: 1px solid rgba(0,0,0,0.04); }
.h-col-list { width: 300px; flex-shrink: 0; border-right: 1px solid rgba(0,0,0,0.04); background: #fdfdfd;}
.h-col-detail { flex: 1; min-width: 300px; background: #f8f9fb; }

/* 缺省状态提示 */
.h-col-placeholder {
    flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
    color: #c9cdd4; font-size: 64px; text-align: center; padding: 32px;
}
.h-col-placeholder p { font-size: 15px; font-weight: 500; margin-top: 16px; color: #86909c; }


/* ================= 极简高级动画 ================= */
.h-stagger-enter-active, .h-stagger-leave-active { transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.h-stagger-enter-from { opacity: 0; transform: translateY(20px) scale(0.98); }
.h-stagger-leave-to { opacity: 0; transform: translateX(100%); }

/* ================= 幻彩高能头部 ================= */
.h-header {
    position: relative; overflow: hidden; padding: 20px 24px 12px;
    border-bottom: 1px solid rgba(0,0,0,0.03);
    background: #fdfdfd; flex-shrink: 0;
}

.h-notify-header .orb-1 {
    position: absolute; top: -100px; right: 20%; width: 240px; height: 240px;
    background: rgba(22, 93, 255, 0.08); border-radius: 50%; filter: blur(60px); z-index: 0; pointer-events: none;
}
.h-notify-header .orb-2 {
    position: absolute; bottom: -80px; left: 10%; width: 180px; height: 180px;
    background: rgba(0, 180, 42, 0.06); border-radius: 50%; filter: blur(50px); z-index: 0; pointer-events: none;
}

.h-header-inner { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0px; }
.h-brand { display: flex; align-items: center; gap: 16px; }

.h-icon-wrapper {
    width: 44px; height: 44px; border-radius: 14px;
    background: linear-gradient(135deg, #ffffff 0%, #f4f5f7 100%);
    box-shadow: 0 12px 24px rgba(22,93,255,0.1), inset 0 2px 4px rgba(255,255,255,0.8), inset 0 -2px 4px rgba(0,0,0,0.02);
    display: flex; align-items: center; justify-content: center;
    font-size: 24px; color: var(--bml-primary, #165dff); position: relative; transition: all 0.3s;
}
.bell-wiggle { transform-origin: top center; animation: bell-swing 8s cubic-bezier(0.34, 1.56, 0.64, 1) infinite; }
@keyframes bell-swing {
    0%, 90% { transform: rotate(0); }
    92% { transform: rotate(15deg) scale(1.05); }
    94% { transform: rotate(-10deg); }
    96% { transform: rotate(5deg); }
    98% { transform: rotate(-2deg); }
    100% { transform: rotate(0); }
}

.h-title { margin: 0; font-size: 20px; font-weight: 800; color: #111; letter-spacing: 0.5px; line-height: 1.2; }
.h-sub { display: inline-block; margin: 4px 0 0 0; font-size: 10px; font-weight: 800; color: #86909c; letter-spacing: 1px; background: rgba(0,0,0,0.04); padding: 2px 8px; border-radius: 20px;}

/* 右侧操作区组合 */
.h-header-right-group { display: flex; align-items: center; gap: 24px; }

.h-close {
    width: 36px; height: 36px; border-radius: 50%;
    background: rgba(0,0,0,0.03); display: flex; align-items: center; justify-content: center;
    color: #4e5969; font-size: 16px; transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); cursor: pointer;
}
.h-close:hover { background: #f53f3f; color: #fff; transform: rotate(90deg) scale(1.1); box-shadow: 0 8px 16px rgba(245,63,63,0.25); }

/* 快捷操作区 */
.h-header-actions {
    position: relative; z-index: 1; display: flex; align-items: center; gap: 12px;
}
.h-btn-text {
    background: transparent; border: none; padding: 0; margin: 0;
    font-family: inherit; font-size: 13px; font-weight: 700; color: #4e5969;
    display: flex; align-items: center; gap: 6px; cursor: pointer; transition: all 0.2s;
}
.h-btn-text:hover { color: var(--bml-primary, #165dff); }
.h-btn-text:disabled { opacity: 0.5; cursor: not-allowed; color: #86909c!important; }
.h-btn-text.danger:hover { color: #f53f3f; }

.h-btn-divider { width: 1px; height: 14px; background: rgba(0,0,0,0.06); }


/* ================= 滚动区域与基础间隔 ================= */
.h-scrollable { flex: 1; overflow-y: auto; padding: 12px 20px 24px; display: flex; flex-direction: column; }
.no-top-pad { padding-top: 0px; }

/* ================= Column 1：月历看板 ================= */
.h-calendar-wrapper { padding: 8px 12px; border-bottom: 1px dashed rgba(0,0,0,0.04); }
.h-calendar-wrapper :deep(.arco-calendar-panel) { padding: 4px 0; }
.h-calendar-wrapper :deep(.arco-calendar-month-cell) { padding: 2px 0; }
.h-calendar-wrapper :deep(.arco-calendar-date) { height: 42px; width: 100%; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; padding-top: 6px; background: transparent !important; margin: 0;}
.h-calendar-wrapper :deep(.arco-calendar-date-value) { height: 24px; min-height: 24px; width: 24px; line-height: 24px; font-size: 11px; font-family: 'Inter', sans-serif; display: flex; align-items: center; justify-content: center; margin: 0; }

/* 自定义日期指示器圆点 */
.h-custom-date { 
    gap: 4px; 
    height: 100%; 
    justify-content: flex-start; /* 将点放在数字下面更自然一点 */
}
.h-cell-indicator { height: 6px; width: 100%; display: flex; align-items: center; justify-content: center; }
.mini-dim { transform: scale(0.6); opacity: 0.3; animation: none !important; box-shadow: none !important;}

/* 自定义日历头部与按钮样式 */
.h-month-trigger { 
    display: inline-flex; align-items: center; gap: 4px; 
    font-size: 14px; font-weight: 800; color: #111; font-family: 'Inter', monospace; 
    padding: 2px 8px; border-radius: 6px; cursor: pointer; transition: background 0.2s;
}
.h-month-trigger:hover { background: rgba(0,0,0,0.04); }
.h-trigger-arrow { font-size: 12px; color: #86909c; }

/* 覆盖默认的“今天”按钮外观和文字 */
.h-calendar-wrapper :deep(.arco-calendar-header-left .arco-btn) {
    font-size: 0; /* 隐藏原本的“今天”两字 */
    padding: 2px 10px;
    height: 26px;
    border-radius: 6px;
    background: rgba(22,93,255,0.1);
    color: var(--bml-primary, #165dff);
    transition: all 0.2s;
}
.h-calendar-wrapper :deep(.arco-calendar-header-left .arco-btn:hover) {
    background: var(--bml-primary, #165dff);
    color: #fff;
}
.h-calendar-wrapper :deep(.arco-calendar-header-left .arco-btn::after) {
    content: '今';
    font-size: 13px;
    font-weight: 700;
}

/* 隐藏日历右侧默认的 月/年 切换按钮，因为小面板下不需要切换 */
.h-calendar-wrapper :deep(.arco-calendar-header-right) { display: none; }

/* 自定义星期栏头：Arco Vue 源码不支持 slot 修改表头文字，直接用 CSS 首字母隐藏 (font-size:0 隐藏整个字，::first-letter 无法剔除第一个)
   这里利用 text-indent 将“周”字挤出可见区域，或者通过 visibility: hidden 组合 
*/
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item) { 
    font-size: 0; /* 隐藏原始文字 */
    color: transparent;
    display: flex; align-items: center; justify-content: center;
}
/* 利用伪元素提取第二个字，默认 arco design calendar 从周日开始 */
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(1))::after { content: "日"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(2))::after { content: "一"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(3))::after { content: "二"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(4))::after { content: "三"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(5))::after { content: "四"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(6))::after { content: "五"; font-size: 11px; font-weight: 700; color: #86909c; }
.h-calendar-wrapper :deep(.arco-calendar-week-list .arco-calendar-week-list-item:nth-child(7))::after { content: "六"; font-size: 11px; font-weight: 700; color: #86909c; }

/* 隐藏当天日期下方的蓝色小圆点（今日指示器） */
.h-calendar-wrapper :deep(.arco-calendar-cell-today::after),
.h-calendar-wrapper :deep(.arco-calendar-date-circle::after) { display: none !important; }

/* 为今天的日期数字增加浅浅的特殊背景底色标识 */
.h-calendar-wrapper :deep(.arco-calendar-cell-today .arco-calendar-date-value) {
    background: rgba(var(--bml-primary-rgb, 22,93,255), 0.08); /* 极淡的品牌蓝背景 */
    color: var(--bml-primary, #165dff);
    font-weight: 800;
    border-radius: 6px;
}

.h-date-legend { display: flex; gap: 12px; padding: 12px 20px; font-size: 11px; font-weight: 500; color: #86909c; }
.legend-item { display: flex; align-items: center; gap: 6px; }
.h-dot { width: 6px; height: 6px; border-radius: 50%; opacity: 0.8; }
.h-dot.info { background: var(--bml-primary, #165dff); }
.h-dot.error { background: #f53f3f; box-shadow: 0 0 6px rgba(245,63,63,0.5); }

/* ================= Column 2：列表视图瀑布流 ================= */
.h-col-header { padding: 12px 20px 8px; border-bottom: 1px dashed rgba(0,0,0,0.04); margin-bottom: 8px; display: flex; flex-direction: column; gap: 8px;}
/* 隐藏无用的列标题，但保留 CSS 以防万一他日复用 */
.col-title { display: none; margin: 0; font-family: 'Inter', monospace; }

.list-filters { display: flex; }
.list-filters :deep(.arco-radio-group) { background: #f4f5f7; border-radius: 6px; padding: 2px;}
.list-filters :deep(.arco-radio-button) { background: transparent; border: none; font-size: 11px; font-weight: 600; color: #4e5969; padding: 0 10px; height: 22px; line-height: 22px;}
.list-filters :deep(.arco-radio-button-checked) { background: #fff; color: var(--bml-primary, #165dff); box-shadow: 0 2px 6px rgba(0,0,0,0.05); border-radius: 4px;}

.h-list { display: flex; flex-direction: column; gap: 8px; padding-bottom: 12px; }
.h-list-footer { text-align: center; padding: 12px 0; font-size: 11px; color: #86909c; font-weight: 600; border-top: 1px dashed rgba(0,0,0,0.04); }

.h-n-card {
    position: relative; background: #fff; border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.02), inset 0 0 0 1px rgba(0,0,0,0.04);
    overflow: hidden; cursor: pointer;
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.h-n-card.compact { padding: 6px 10px; }
.h-n-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.06), inset 0 0 0 1px rgba(0,0,0,0.06); }
.h-n-card.is-selected { background: #f4f7ff; box-shadow: 0 4px 16px rgba(22,93,255,0.1), inset 0 0 0 1px rgba(22,93,255,0.3); transform: translateX(4px); }

.h-neon-bar { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; transition: all 0.3s; }
.h-n-card.is-info .h-neon-bar { background: var(--bml-primary, #165dff); }
.h-n-card.is-warning .h-neon-bar { background: #ff7d00; }
.h-n-card.is-error .h-neon-bar, .h-n-card.is-critical .h-neon-bar { background: #f53f3f; }

.h-n-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.h-tag-combo { display: flex; align-items: center; gap: 6px; }
.h-level-icon { font-size: 12px; display: flex;}
.is-info .h-level-icon { color: var(--bml-primary, #165dff); }
.is-warning .h-level-icon { color: #ff7d00; }
.is-error .h-level-icon, .is-critical .h-level-icon { color: #f53f3f; }

.h-tag-text { font-size: 10px; font-weight: 700; color: #86909c; letter-spacing: 0.5px; text-transform: uppercase;}

.h-time { font-size: 10px; font-weight: 600; color: #c9cdd4; }

.h-n-title { margin: 0; font-size: 13px; font-weight: 800; color: #111; line-height: 1.4; letter-spacing: 0.2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 180px;}
.h-card-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 4px; }

/* 列表卡片右下角徽章 */
.h-card-badge { display: flex; align-items: center; gap: 6px; font-size: 10px; font-weight: 800; letter-spacing: 0.5px; justify-content: flex-end; width: 100%; border-top: 1px dashed rgba(0,0,0,0.05); padding-top: 10px; }
.h-dot { width: 6px; height: 6px; border-radius: 50%; background: #f53f3f; box-shadow: 0 0 8px rgba(245,63,63,0.8); animation: h-pulse 2s infinite; }
@keyframes h-pulse { 0% { box-shadow: 0 0 0 0 rgba(245,63,63,0.4); } 70% { box-shadow: 0 0 0 4px rgba(245,63,63,0); } 100% { box-shadow: 0 0 0 0 rgba(245,63,63,0); } }

.is-info .h-card-badge { color: var(--bml-primary, #165dff); } .is-warning .h-card-badge { color: #ff7d00; } .is-error .h-card-badge, .is-critical .h-card-badge { color: #f53f3f; }

/* 已读半透明形态 */
.h-n-card.is-read:not(.is-selected) { opacity: 0.6; filter: grayscale(50%); box-shadow: 0 2px 8px rgba(0,0,0,0.02), inset 0 0 0 1px rgba(0,0,0,0.02); }
.h-n-card.is-read:hover { opacity: 0.95; filter: grayscale(0%); }
.h-n-card.is-read .h-card-badge { color: #86909c; }


/* ================= Column 3：详情页设计 ================= */
.h-detail-box {
    position: relative; background: #fff; border-radius: 16px; padding: 20px 20px;
    box-shadow: 0 16px 40px rgba(0,0,0,0.04), inset 0 0 0 1px rgba(0,0,0,0.03);
    overflow: hidden;
}

.h-detail-box-bg {
    position: absolute; top: 0; left: 0; right: 0; height: 180px;
    background: linear-gradient(180deg, rgba(22,93,255,0.06) 0%, rgba(255,255,255,0) 100%);
    z-index: 0; pointer-events: none; opacity: 0.8;
}
.is-warning .h-detail-box-bg { background: linear-gradient(180deg, rgba(255,125,0,0.08) 0%, rgba(255,255,255,0) 100%); }
.is-error .h-detail-box-bg, .is-critical .h-detail-box-bg { background: linear-gradient(180deg, rgba(245,63,63,0.08) 0%, rgba(255,255,255,0) 100%); }

.h-detail-header-block { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 20px; }
.h-d-icon { 
    font-size: 24px; margin-bottom: 12px; 
    width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
    box-shadow: 0 8px 16px rgba(0,0,0,0.05);
}
.is-info .h-d-icon { color: var(--bml-primary, #165dff); background: rgba(var(--bml-primary-rgb, 22,93,255), 0.1); }
.is-warning .h-d-icon { color: #ff7d00; background: rgba(255,125,0,0.1); }
.is-error .h-d-icon, .is-critical .h-d-icon { color: #f53f3f; background: rgba(245,63,63,0.1); }

.h-d-title { margin: 0; font-size: 16px; font-weight: 800; color: #111; line-height: 1.4; font-family: 'Inter', sans-serif;}

/* 元数据表格面板 */
.h-d-meta-panel {
    position: relative; z-index: 1;
    background: #fdfdfd; border-radius: 12px; padding: 16px;
    display: flex; flex-direction: column; gap: 12px;
    border: 1px solid rgba(0,0,0,0.04);
}
.d-meta-item { display: flex; align-items: center; justify-content: space-between; }
.m-label { font-size: 12px; font-weight: 700; color: #86909c; }
.m-value { font-size: 12px; font-weight: 600; color: #1D2129; display: flex; align-items: center; gap: 6px; }
.id-text { font-family: monospace; background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 4px;}
.time-val, .status-val { font-family: 'Inter', monospace; }
.status-val.resolved { color: #00b42a; font-weight: 800;}

.h-d-divider { height: 1px; background: rgba(0,0,0,0.04); margin: 20px 0; border: none; }

/* 详情富文本展示区 */
.h-d-content-block { position: relative; z-index: 1; margin-bottom: 20px; }
.content-title { margin: 0 0 8px 0; font-size: 13px; font-weight: 800; color: #111; }
.content-body {
    font-size: 13px; line-height: 1.6; color: #4e5969;
    padding: 16px; border-radius: 10px; background: #fafafa;
    border: 1px solid rgba(0,0,0,0.03); 
    white-space: pre-wrap; word-break: break-all;
}


/* ================= 艺术中心级别空状态 ================= */
.h-empty-art {
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    padding: 32px 0; opacity: 0.6;
}
.min-padding { padding: 64px 0; }
.h-art-container {
    position: relative; width: 64px; height: 64px; display: flex; align-items: center; justify-content: center;
    margin-bottom: 16px;
}
.h-art-ring { position: absolute; border-radius: 50%; top: 50%; left: 50%; transform: translate(-50%, -50%); border: 1px solid rgba(22, 93, 255, 0.1); }
.r1 { width: 80px; height: 80px; animation: wave 4s infinite linear; }
.r2 { width: 60px; height: 60px; animation: wave 4s infinite linear 1.3s; }
.h-art-center {
    position: relative; z-index: 2; width: 40px; height: 40px; border-radius: 12px;
    background: #fff; box-shadow: 0 8px 16px rgba(22,93,255,0.1), inset 0 2px 4px rgba(255,255,255,0.8);
    display: flex; align-items: center; justify-content: center;
}
.art-icon { font-size: 20px; color: #00b42a;}

.h-empty-desc { font-size: 13px; color: #86909c; text-align: center; margin: 0; }


/* ================= 极致暗黑模式支持 ================= */
:global(body[arco-theme='dark']) .h-notification-panel { background: #18181A; }
:global(body[arco-theme='dark']) .h-columns { background: #131314; }
:global(body[arco-theme='dark']) .h-col { background: #18181A; border-color: rgba(255,255,255,0.03); }
:global(body[arco-theme='dark']) .h-col-detail { background: #18181A; border-left: 1px solid rgba(255,255,255,0.04); }
:global(body[arco-theme='dark']) .h-col-header { border-color: rgba(255,255,255,0.04); }

:global(body[arco-theme='dark']) .h-header { background: #18181A; border-bottom-color: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-title, :global(body[arco-theme='dark']) .col-title, :global(body[arco-theme='dark']) .h-d-title, :global(body[arco-theme='dark']) .content-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-sub { background: rgba(255,255,255,0.05); color: #86909c;}
:global(body[arco-theme='dark']) .h-close { color: #C9CDD4; background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-close:hover { background: #f53f3f; color: #fff; }
:global(body[arco-theme='dark']) .h-section-title { color: #86909C; }

/* Date Picker/Calendar Dark Mode */
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-panel) { background: #18181A; }
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-date) { color: #86909c; }
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-date:hover) { background: #2A2A2C; color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-date-selected) { background: var(--bml-primary, #165dff); color: #fff; }

:global(body[arco-theme='dark']) .calendar-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-month-trigger:hover { background: rgba(255,255,255,0.08); }
:global(body[arco-theme='dark']) .h-trigger-arrow { color: #86909c; }
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-header-left .arco-btn) { background: rgba(22,93,255,0.2); }
:global(body[arco-theme='dark']) .h-calendar-wrapper :deep(.arco-calendar-header-left .arco-btn:hover) { background: var(--bml-primary, #165dff); }

/* Filter Tabs Dark */
:global(body[arco-theme='dark']) .list-filters :deep(.arco-radio-group) { background: #2A2A2C; }
:global(body[arco-theme='dark']) .list-filters :deep(.arco-radio-button) { color: #86909c; }
:global(body[arco-theme='dark']) .list-filters :deep(.arco-radio-button-checked) { background: #333336; color: var(--bml-primary, #165dff); box-shadow: 0 2px 4px rgba(0,0,0,0.4);}

/* Waterfall List */
:global(body[arco-theme='dark']) .h-n-card { background: #2A2A2C; box-shadow: inset 0 0 0 1px rgba(255,255,255,0.05); background-image: none; }
:global(body[arco-theme='dark']) .h-n-card:hover { background: #333336; box-shadow: inset 0 0 0 1px rgba(255,255,255,0.1); }
:global(body[arco-theme='dark']) .h-n-card.is-selected { background: #283042; box-shadow: inset 0 0 0 1px rgba(22,93,255,0.4); }
:global(body[arco-theme='dark']) .h-n-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-tag-text { color: #86909c; }

/* Detail */
:global(body[arco-theme='dark']) .h-detail-box { background: #2A2A2C; box-shadow: inset 0 0 0 1px rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-d-meta-panel { background: rgba(255,255,255,0.02); border-color: rgba(255,255,255,0.03); }
:global(body[arco-theme='dark']) .m-value { color: #E5E6EB; }
:global(body[arco-theme='dark']) .id-text { background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .content-body { background: rgba(255,255,255,0.02); border-color: rgba(255,255,255,0.03); color: #86909c;}

:global(body[arco-theme='dark']) .h-col-placeholder { color: #3A3A3C; }
</style>
