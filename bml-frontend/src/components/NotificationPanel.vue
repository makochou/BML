<template>
  <div class="h-notification-panel">
    
    <!-- 幻彩高能头部 -->
    <div class="h-header h-notify-header">
      <div class="h-glow-orb orb-1"></div>
      <div class="h-glow-orb orb-2"></div>
      
      <div class="h-header-inner">
         <div class="h-brand">
            <div class="h-icon-wrapper bell-wiggle">
               <a-badge :count="store.unreadCount" :max-count="99" dot :offset="[-2, 2]">
                 <icon-notification />
               </a-badge>
            </div>
            <div class="h-title-group">
               <h2 class="h-title">系统告警</h2>
               <p class="h-sub">NOTIFICATIONS</p>
            </div>
         </div>
         
         <div class="h-close" @click="store.closeDrawer()">
            <icon-close />
         </div>
      </div>

      <!-- 快捷操作区 -->
      <div class="h-header-actions">
        <button class="h-btn-text" @click="handleMarkAllRead" :disabled="store.unreadCount === 0">
           <icon-check-circle /> 全部已读
        </button>
        <div class="h-btn-divider"></div>
        <button class="h-btn-text danger" @click="handleClearAll">
           <icon-delete /> 清空历史
        </button>
      </div>
    </div>

    <!-- 苹果式胶囊悬浮导航 -->
    <div class="h-sticky-nav">
      <div class="h-capsule-bg">
        <div class="h-capsule-slider" :style="sliderStyle"></div>
        
        <div class="h-capsule-btn" :class="{ 'is-active': activeTab === 'all' }" @click="activeTab = 'all'">
           <span class="btn-text">全部记录</span>
           <span class="btn-badge" v-if="allCount > 0">{{ allCount > 99 ? '99+' : allCount }}</span>
        </div>
        
        <div class="h-capsule-btn" :class="{ 'is-active': activeTab === 'unread' }" @click="activeTab = 'unread'">
           <span class="btn-text">待处理</span>
           <span class="btn-badge is-urgent" v-if="store.unreadCount > 0">{{ store.unreadCount }}</span>
        </div>
      </div>
    </div>

    <!-- 列表瀑布流 -->
    <div class="h-scrollable">
      <transition-group name="h-stagger" tag="div" class="h-list" v-if="filteredAlerts.length > 0">
        
        <div 
          v-for="alert in filteredAlerts" 
          :key="alert.id"
          class="h-n-card"
          :class="[`is-${alert.alertLevel}`, { 'is-read': alert.readStatus === 1 }]"
        >
          <!-- 侧边霓虹背光条 -->
          <div class="h-neon-bar"></div>
          
          <div class="h-n-content">
            <div class="h-n-row">
               <div class="h-tag-combo">
                 <div class="h-level-icon">
                    <component :is="getIconForLevel(alert.alertLevel)" />
                 </div>
                 <span class="h-tag-text">{{ alert.alertType }}</span>
               </div>
               <span class="h-time">{{ formatTime(alert.createTime) }}</span>
            </div>
            
            <h3 class="h-n-title">{{ alert.alertTitle }}</h3>
            <p class="h-n-desc">{{ alert.alertContent }}</p>
            
            <div class="h-n-footer">
               <div class="h-status-indicator">
                 <div class="h-dot" v-if="alert.readStatus === 0"></div>
                 <span>{{ alert.readStatus === 0 ? 'URGENT' : 'RESOLVED' }}</span>
               </div>
               
               <div class="h-n-actions">
                  <div class="h-action-icon check" v-if="alert.readStatus === 0" @click.stop="store.markAsRead(alert.id)" v-tooltip="'标为已读'">
                    <icon-check />
                  </div>
                  <div class="h-action-icon delete" @click.stop="store.deleteAlert(alert.id)" v-tooltip="'彻底删除'">
                    <icon-close />
                  </div>
               </div>
            </div>
          </div>
        </div>

      </transition-group>

      <!-- 绝对绝美的骨架/艺术插画空状态 -->
      <div class="h-empty-art" v-else>
         <div class="h-art-container">
            <div class="h-art-ring r1"></div>
            <div class="h-art-ring r2"></div>
            <div class="h-art-ring r3"></div>
            <div class="h-art-center">
               <icon-check-circle-fill class="art-icon check-art" v-if="activeTab === 'unread'" />
               <icon-message class="art-icon box-art" v-else />
            </div>
         </div>
         <h4 class="h-empty-title">{{ activeTab === 'unread' ? 'All Caught Up' : 'No Notifications' }}</h4>
         <p class="h-empty-desc">
            {{ activeTab === 'unread' ? '太棒了！所有需要您立即关注的系统风险和告警都已被妥善处理。' : '目前风平浪静，系统处于最佳运行状态。有任何异常我们会第一时间通知您。' }}
         </p>
      </div>

    </div>

  </div>
</template>

<script lang="ts" setup>
import { ref, computed, watch, onMounted } from 'vue';
import { useNotificationStore } from '../store/notification';
import { 
  IconInfoCircleFill, IconExclamationCircleFill, 
  IconCloseCircleFill, IconNotification, IconClose,
  IconCheckCircle, IconCheckCircleFill, IconDelete,
  IconCheck, IconMessage
} from '@arco-design/web-vue/es/icon';
import { Message } from '@arco-design/web-vue';

const store = useNotificationStore();

const activeTab = ref<'all' | 'unread'>('all');
const sliderStyle = ref({ transform: 'translateX(2px)', width: 'calc(50% - 4px)' });

const allCount = computed(() => store.alerts.length);

const filteredAlerts = computed(() => {
    if (activeTab.value === 'unread') {
        return store.alerts.filter(a => a.readStatus === 0);
    }
    return store.alerts;
});

watch(activeTab, (val) => {
    if (val === 'all') {
        sliderStyle.value = { transform: 'translateX(2px)', width: 'calc(50% - 4px)' };
    } else {
        sliderStyle.value = { transform: 'translateX(calc(100% + 2px))', width: 'calc(50% - 4px)' };
    }
}, { immediate: true });

onMounted(() => {
    if(store.alerts.length === 0) store.fetchAlerts();
});

const formatTime = (timeStr: string) => {
    const d = new Date(timeStr);
    const today = new Date();
    if (d.toDateString() === today.toDateString()) {
        return `今日 ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    }
    return `${d.getMonth()+1}/${d.getDate()} ${d.getHours()}:${d.getMinutes().toString().padStart(2, '0')}`;
};

const getIconForLevel = (level: string) => {
    switch(level) {
        case 'info': return IconInfoCircleFill;
        case 'warning': return IconExclamationCircleFill;
        case 'error': return IconCloseCircleFill;
        case 'critical': return IconCloseCircleFill;
        default: return IconInfoCircleFill;
    }
};

const handleMarkAllRead = async () => {
    if (store.unreadCount === 0) return;
    await store.markAllAsRead();
    Message.success('系统：所有告警已被标记为已处理');
};

const handleClearAll = () => {
    if (store.alerts.length === 0) return;
    store.alerts.forEach((a: any) => store.deleteAlert(a.id));
    Message.success('系统：历史告警记录已清空');
};
</script>

<style scoped>
/* ================= 全局容器 ================= */
.h-notification-panel {
    display: flex; flex-direction: column; height: 100%;
    background: #fdfdfd; 
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", sans-serif;
}

/* ================= 幻彩高能头部 ================= */
.h-header {
    position: relative; overflow: hidden; padding: 40px 32px 16px;
    border-bottom: 1px solid rgba(0,0,0,0.03);
    background: #fdfdfd; flex-shrink: 0;
}
.h-notify-header .orb-1 {
    position: absolute; top: -60px; right: 0; width: 160px; height: 160px;
    background: rgba(22, 93, 255, 0.12); border-radius: 50%; filter: blur(40px); z-index: 0;
}
.h-notify-header .orb-2 {
    position: absolute; bottom: 0; left: -40px; width: 120px; height: 120px;
    background: rgba(0, 180, 42, 0.08); border-radius: 50%; filter: blur(30px); z-index: 0;
}

.h-header-inner { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.h-brand { display: flex; align-items: center; gap: 16px; }

.h-icon-wrapper {
    width: 48px; height: 48px; border-radius: 16px;
    background: linear-gradient(135deg, #ffffff 0%, #f4f5f7 100%);
    box-shadow: 0 12px 24px rgba(22,93,255,0.1), inset 0 2px 4px rgba(255,255,255,0.8), inset 0 -2px 4px rgba(0,0,0,0.02);
    display: flex; align-items: center; justify-content: center;
    font-size: 24px; color: #165dff;
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

.h-title { margin: 0; font-size: 24px; font-weight: 800; color: #111; letter-spacing: 0.5px; line-height: 1.2; }
.h-sub { display: inline-block; margin: 4px 0 0 0; font-size: 11px; font-weight: 800; color: #86909c; letter-spacing: 1.5px; background: rgba(0,0,0,0.04); padding: 2px 8px; border-radius: 20px;}

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
.h-btn-text:hover { color: #165dff; }
.h-btn-text.danger:hover { color: #f53f3f; }
.h-btn-text:disabled { color: #c9cdd4; cursor: not-allowed; }
.h-btn-divider { width: 1px; height: 12px; background: rgba(0,0,0,0.08); }


/* ================= 苹果式悬浮导航 ================= */
.h-sticky-nav {
    padding: 16px 32px 8px; background: #fdfdfd; position: sticky; top: 0; z-index: 10;
}
.h-capsule-bg {
    position: relative; background: rgba(0,0,0,0.04); border-radius: 12px;
    padding: 3px; display: flex; align-items: center;
    box-shadow: inset 0 2px 4px rgba(0,0,0,0.02);
}
.h-capsule-slider {
    position: absolute; top: 3px; bottom: 3px; left: 0;
    background: #fff; border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.06), inset 0 0 0 1px rgba(0,0,0,0.02);
    transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), width 0.4s;
    z-index: 1;
}
.h-capsule-btn {
    flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px;
    padding: 10px 0; position: relative; z-index: 2; cursor: pointer;
    font-size: 14px; font-weight: 600; color: #86909c; transition: color 0.3s;
}
.h-capsule-btn.is-active { color: #111; }
.btn-badge {
    background: rgba(0,0,0,0.08); color: #4e5969; padding: 2px 8px;
    border-radius: 12px; font-size: 12px; font-weight: 700;
}
.h-capsule-btn.is-active .btn-badge { background: #f2f3f5; color: #111; }
.h-capsule-btn.is-active .btn-badge.is-urgent { background: #f53f3f; color: #fff; box-shadow: 0 4px 8px rgba(245,63,63,0.3); }


/* ================= 超感瀑布流列表 ================= */
.h-scrollable { flex: 1; overflow-y: auto; padding: 16px 32px 32px; display: flex; flex-direction: column; }
.h-list { display: flex; flex-direction: column; gap: 16px; }

/* 动画队列 */
.h-stagger-enter-active, .h-stagger-leave-active { transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1); }
.h-stagger-enter-from { opacity: 0; transform: translateY(30px) scale(0.95); }
.h-stagger-leave-to { opacity: 0; transform: translateX(100%) scale(0.95); }

/* 卡片本体 */
.h-n-card {
    position: relative; background: #fff; border-radius: 20px;
    box-shadow: 0 8px 24px rgba(0,0,0,0.03), inset 0 0 0 1px rgba(0,0,0,0.03);
    padding: 20px; padding-left: 24px; overflow: hidden;
    transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    display: flex; flex-direction: column; cursor: pointer;
    background-image: radial-gradient(circle at top right, rgba(255,255,255,0.8), transparent 40%);
}
.h-n-card:hover { transform: translateY(-4px) scale(1.02); box-shadow: 0 16px 40px rgba(0,0,0,0.08), inset 0 0 0 1px rgba(0,0,0,0.04); z-index: 2; }

/* 霓虹背光指示条 */
.h-neon-bar { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; transition: all 0.3s; }
.h-n-card.is-info .h-neon-bar { background: #165dff; box-shadow: 2px 0 12px rgba(22,93,255,0.5); }
.h-n-card.is-warning .h-neon-bar { background: #ff7d00; box-shadow: 2px 0 12px rgba(255,125,0,0.5); }
.h-n-card.is-error .h-neon-bar, .h-n-card.is-critical .h-neon-bar { background: #f53f3f; box-shadow: 2px 0 12px rgba(245,63,63,0.5); }

/* 卡片内容排版 */
.h-n-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.h-tag-combo { display: flex; align-items: center; gap: 8px; }
.h-level-icon { font-size: 16px; }
.is-info .h-level-icon { color: #165dff; }
.is-warning .h-level-icon { color: #ff7d00; }
.is-error .h-level-icon, .is-critical .h-level-icon { color: #f53f3f; }

.h-tag-text { font-size: 13px; font-weight: 800; color: #4e5969; letter-spacing: 0.5px; text-transform: uppercase; background: rgba(0,0,0,0.04); padding: 2px 8px; border-radius: 8px;}
.h-time { font-size: 12px; font-weight: 600; color: #86909c; }

.h-n-title { margin: 0 0 6px 0; font-size: 16px; font-weight: 800; color: #111; line-height: 1.4; letter-spacing: 0.2px;}
.h-n-desc { margin: 0; font-size: 14px; color: #4e5969; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* 卡片尾部与动作 */
.h-n-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 1px dashed rgba(0,0,0,0.06); }
.h-status-indicator { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 800; letter-spacing: 0.5px; }
.h-dot { width: 8px; height: 8px; border-radius: 50%; background: #f53f3f; box-shadow: 0 0 12px rgba(245,63,63,0.8); animation: h-pulse 2s infinite; }
@keyframes h-pulse { 0% { box-shadow: 0 0 0 0 rgba(245,63,63,0.4); } 70% { box-shadow: 0 0 0 6px rgba(245,63,63,0); } 100% { box-shadow: 0 0 0 0 rgba(245,63,63,0); } }

.is-info .h-status-indicator { color: #165dff; } .is-warning .h-status-indicator { color: #ff7d00; } .is-error .h-status-indicator, .is-critical .h-status-indicator { color: #f53f3f; }

.h-n-actions { display: flex; gap: 8px; opacity: 0; transform: translateY(10px); transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.h-n-card:hover .h-n-actions { opacity: 1; transform: translateY(0); }
.h-action-icon {
    width: 32px; height: 32px; border-radius: 10px; background: rgba(0,0,0,0.03);
    display: flex; align-items: center; justify-content: center; font-size: 16px; color: #4e5969;
    transition: all 0.2s;
}
.h-action-icon:hover { transform: scale(1.1); }
.h-action-icon.check:hover { background: #165dff; color: #fff; box-shadow: 0 4px 12px rgba(22,93,255,0.3); }
.h-action-icon.delete:hover { background: #f53f3f; color: #fff; box-shadow: 0 4px 12px rgba(245,63,63,0.3); }

/* 已读状态极简风化 */
.h-n-card.is-read { opacity: 0.5; filter: grayscale(80%); box-shadow: 0 4px 12px rgba(0,0,0,0.02), inset 0 0 0 1px rgba(0,0,0,0.02); }
.h-n-card.is-read:hover { opacity: 0.9; filter: grayscale(0%); box-shadow: 0 8px 24px rgba(0,0,0,0.06); }
.h-n-card.is-read .h-status-indicator { color: #86909c; }


/* ================= 艺术中心级别空状态 ================= */
.h-empty-art {
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    flex: 1; min-height: 400px;
}
.h-art-container {
    position: relative; width: 160px; height: 160px; display: flex; align-items: center; justify-content: center;
    margin-bottom: 32px;
}
.h-art-ring { position: absolute; border-radius: 50%; top: 50%; left: 50%; transform: translate(-50%, -50%); border: 1px solid rgba(22, 93, 255, 0.1); }
.r1 { width: 160px; height: 160px; animation: wave 4s infinite linear; }
.r2 { width: 110px; height: 110px; animation: wave 4s infinite linear 1.3s; }
.r3 { width: 60px; height: 60px; animation: wave 4s infinite linear 2.6s; }
@keyframes wave { 0% { width: 60px; height: 60px; opacity: 1; border-color: rgba(22,93,255,0.4); border-width: 2px;} 100% { width: 220px; height: 220px; opacity: 0; border-width: 1px;} }

.h-art-center {
    position: relative; z-index: 2; width: 80px; height: 80px; border-radius: 24px;
    background: linear-gradient(135deg, #ffffff 0%, #f4f5f7 100%);
    box-shadow: 0 20px 40px rgba(22,93,255,0.15), inset 0 2px 4px rgba(255,255,255,0.8);
    display: flex; align-items: center; justify-content: center;
}
.art-icon { font-size: 40px; }
.check-art { color: #00b42a; filter: drop-shadow(0 4px 8px rgba(0,180,42,0.3)); }
.box-art { color: #165dff; filter: drop-shadow(0 4px 8px rgba(22,93,255,0.3)); }

.h-empty-title { font-size: 20px; font-weight: 800; color: #111; margin: 0 0 12px 0; font-family: 'Inter', sans-serif; letter-spacing: -0.5px;}
.h-empty-desc { font-size: 14px; color: #86909c; text-align: center; max-width: 280px; line-height: 1.6; margin: 0; }


/* ================= 极致暗黑模式 ================= */
:global(body[arco-theme='dark']) .h-notification-panel { background: #18181A; }
:global(body[arco-theme='dark']) .h-header { background: #18181A; border-bottom-color: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-notify-header .orb-1 { background: rgba(22,93,255,0.15); }
:global(body[arco-theme='dark']) .h-icon-wrapper { background: #2A2A2C; box-shadow: 0 12px 24px rgba(0,0,0,0.5), inset 0 2px 4px rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-sub { background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-close { color: #C9CDD4; background: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-close:hover { background: #f53f3f; color: #fff; box-shadow: 0 8px 16px rgba(245,63,63,0.3); }

:global(body[arco-theme='dark']) .h-sticky-nav { background: #18181A; }
:global(body[arco-theme='dark']) .h-capsule-bg { background: rgba(255,255,255,0.05); box-shadow: inset 0 2px 4px rgba(0,0,0,0.2); }
:global(body[arco-theme='dark']) .h-capsule-slider { background: #2A2A2C; box-shadow: 0 4px 12px rgba(0,0,0,0.4), inset 0 0 0 1px rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-capsule-btn { color: #86909c; }
:global(body[arco-theme='dark']) .h-capsule-btn.is-active { color: #E5E6EB; }
:global(body[arco-theme='dark']) .btn-badge { background: rgba(255,255,255,0.1); color: #C9CDD4; }
:global(body[arco-theme='dark']) .h-capsule-btn.is-active .btn-badge { background: rgba(255,255,255,0.2); color: #fff; }

:global(body[arco-theme='dark']) .h-n-card { background: #2A2A2C; box-shadow: 0 8px 24px rgba(0,0,0,0.4), inset 0 0 0 1px rgba(255,255,255,0.05); background-image: none; }
:global(body[arco-theme='dark']) .h-n-card:hover { box-shadow: 0 16px 40px rgba(0,0,0,0.6), inset 0 0 0 1px rgba(255,255,255,0.1); background: #333336; }
:global(body[arco-theme='dark']) .h-n-title { color: #E5E6EB; }
:global(body[arco-theme='dark']) .h-n-desc { color: #86909c; }
:global(body[arco-theme='dark']) .h-tag-text { background: rgba(255,255,255,0.05); color: #C9CDD4; }
:global(body[arco-theme='dark']) .h-n-footer { border-top-color: rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-action-icon { background: rgba(255,255,255,0.05); color: #C9CDD4; }
:global(body[arco-theme='dark']) .h-action-icon:hover { color: #fff; }

:global(body[arco-theme='dark']) .h-art-center { background: #2A2A2C; box-shadow: 0 20px 40px rgba(0,0,0,0.4), inset 0 2px 4px rgba(255,255,255,0.05); }
:global(body[arco-theme='dark']) .h-empty-title { color: #E5E6EB; }
</style>
