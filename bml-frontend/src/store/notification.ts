import { defineStore } from 'pinia';
import request from '../utils/request';

/**
 * 告警通知项接口定义
 * 对应后端 SysAlert 实体
 */
export interface AlertItem {
    /** 告警唯一 ID (雪花算法生成的 BIGINT) */
    id: string;
    /** 告警类型：CPU_HIGH / MEMORY_HIGH / JVM_HIGH / DISK_FULL */
    alertType: string;
    /** 告警级别：info / warning / error / critical */
    alertLevel: string;
    /** 告警标题 */
    alertTitle: string;
    /** 告警详细内容 */
    alertContent: string;
    /** 阅读状态：0=未读, 1=已读 */
    readStatus: number;
    /** 创建时间（yyyy-MM-dd HH:mm:ss） */
    createTime: string;
}

/**
 * 通知中心 Pinia 状态管理
 *
 * 核心职责：
 * 1. 管理告警列表数据和未读计数
 * 2. 定时轮询后端获取增量告警（每 30 秒）
 * 3. 维护"新告警队列"驱动右下角 Toast 弹窗
 * 4. 提供已读/全部已读/删除操作
 *
 * 使用方式：
 * - Layout.vue 中 onMounted 调用 startPolling()，onUnmounted 调用 stopPolling()
 * - NotificationPanel.vue 读取 alerts 和 unreadCount
 * - AlertToast.vue 消费 newAlerts 队列
 */
export const useNotificationStore = defineStore('notification', {
    state: () => ({
        /** 告警列表（通知面板展示用，按时间倒序） */
        alerts: [] as AlertItem[],

        /** 未读告警数量（驱动铃铛 Badge） */
        unreadCount: 0,

        /** 上次已知的最大告警 ID（增量轮询用） */
        lastAlertId: null as string | null,

        /** 新到达的告警队列（驱动 Toast 弹窗，先进先出） */
        newAlerts: [] as AlertItem[],

        /** 轮询定时器 ID */
        _pollTimer: null as number | null,

        /** 是否已初始化（首次加载完成标记） */
        _initialized: false,

        // ========== 三步面板专用状态 ==========

        /** 存在告警的日期列表 (yyyy-MM-dd 格式，倒序) */
        alertDates: [] as string[],

        /** 按日期查询到的告警列表 */
        dateAlerts: [] as AlertItem[],

        /** 当前选中的日期 */
        selectedDate: null as string | null,

        /** 当前选中查看详情的告警 */
        selectedAlert: null as AlertItem | null,

        /** 面板当前视图步骤：'dates' | 'list' | 'detail' */
        panelStep: 'dates' as 'dates' | 'list' | 'detail',

        /** 右侧抽屉是否可见 */
        drawerVisible: false,
    }),

    getters: {
        /** 是否有未读告警 */
        hasUnread: (state) => state.unreadCount > 0,
    },

    actions: {
        /**
         * 获取未读告警数量
         * 独立接口，用于初始化和刷新 Badge
         */
        async fetchUnreadCount() {
            try {
                const res: any = await request.get('/system/alert/unread-count');
                if (res.code === 200) {
                    this.unreadCount = res.data || 0;
                }
            } catch (e) {
                console.error('[通知中心] 获取未读数量失败:', e);
            }
        },

        /**
         * 获取告警列表（全量，用于通知面板打开时刷新）
         */
        async fetchAlerts() {
            try {
                const res: any = await request.get('/system/alert/list', {
                    params: { limit: 50 },
                });
                if (res.code === 200) {
                    this.alerts = res.data || [];
                    // 更新 lastAlertId 为列表中最大的 ID
                    if (this.alerts.length > 0) {
                        this._updateLastId(this.alerts);
                    }
                }
            } catch (e) {
                console.error('[通知中心] 获取告警列表失败:', e);
            }
        },

        /**
         * 增量轮询 — 核心方法
         *
         * 每次调用只获取 lastAlertId 之后的新告警，
         * 新告警同时推入 alerts 列表头部和 newAlerts 队列（驱动 Toast）。
         */
        async pollLatest() {
            try {
                const params: any = {};
                if (this.lastAlertId) {
                    params.lastId = this.lastAlertId;
                }
                const res: any = await request.get('/system/alert/latest', { params });

                if (res.code === 200 && res.data) {
                    const latestAlerts: AlertItem[] = res.data;

                    if (!this._initialized) {
                        // 首次加载：直接设置列表，不触发 Toast
                        this.alerts = latestAlerts.slice().reverse(); // 转为时间倒序
                        this._initialized = true;
                    } else if (latestAlerts.length > 0) {
                        // 增量数据：unshift 到列表头部 + 推入 Toast 队列
                        for (const alert of latestAlerts) {
                            // 避免重复（防御性编程）
                            if (!this.alerts.find((a) => a.id === alert.id)) {
                                this.alerts.unshift(alert);

                                // 解析日期部分并同步更新 alertDates 数组以便日历实时出现红点
                                if (alert.createTime) {
                                    const datePart = alert.createTime.split(' ')[0]; // "yyyy-MM-dd"
                                    if (datePart && !this.alertDates.includes(datePart)) {
                                        this.alertDates.push(datePart);
                                    }
                                }
                            }
                            // 只有未读的新告警才触发 Toast
                            if (alert.readStatus === 0) {
                                this.newAlerts.push(alert);
                            }
                        }
                    }

                    // 更新 lastAlertId
                    if (latestAlerts.length > 0) {
                        this._updateLastId(latestAlerts);
                    }

                    // 同步刷新未读数
                    await this.fetchUnreadCount();
                }
            } catch (e) {
                console.error('[通知中心] 增量轮询失败:', e);
            }
        },

        /**
         * 标记单条告警为已读
         */
        async markAsRead(id: string) {
            try {
                const res: any = await request.put(`/system/alert/read/${id}`);
                if (res.code === 200) {
                    // 本地同步状态
                    const alert = this.alerts.find((a) => a.id === id);
                    if (alert) {
                        alert.readStatus = 1;
                    }
                    // 刷新未读数
                    if (this.unreadCount > 0) {
                        this.unreadCount--;
                    }
                }
            } catch (e) {
                console.error('[通知中心] 标记已读失败:', e);
            }
        },

        /**
         * 一键全部已读
         */
        async markAllAsRead() {
            try {
                const res: any = await request.put('/system/alert/read-all');
                if (res.code === 200) {
                    // 本地全部标记已读
                    this.alerts.forEach((a) => (a.readStatus = 1));
                    this.unreadCount = 0;
                }
            } catch (e) {
                console.error('[通知中心] 全部已读失败:', e);
            }
        },

        /**
         * 删除指定告警
         */
        async deleteAlert(id: string) {
            try {
                const res: any = await request.delete(`/system/alert/${id}`);
                if (res.code === 200) {
                    const idx = this.alerts.findIndex((a) => a.id === id);
                    if (idx !== -1) {
                        // 如果是未读告警，同步减少未读数
                        if (this.alerts[idx] && this.alerts[idx].readStatus === 0 && this.unreadCount > 0) {
                            this.unreadCount--;
                        }
                        this.alerts.splice(idx, 1);
                    }
                }
            } catch (e) {
                console.error('[通知中心] 删除告警失败:', e);
            }
        },

        /**
         * 消费一条新告警（Toast 弹窗展示后调用）
         * @returns 被消费的告警，队列为空返回 null
         */
        consumeNewAlert(): AlertItem | null {
            return this.newAlerts.shift() || null;
        },

        /**
         * 启动轮询定时器（每 30 秒）
         * 在 Layout.vue 的 onMounted 中调用
         */
        startPolling() {
            // 立即执行一次
            this.pollLatest();

            // 设置定时器，每 30 秒轮询
            if (this._pollTimer) {
                clearInterval(this._pollTimer);
            }
            this._pollTimer = window.setInterval(() => {
                this.pollLatest();
            }, 30_000);
        },

        /**
         * 停止轮询定时器
         * 在 Layout.vue 的 onUnmounted 中调用
         */
        stopPolling() {
            if (this._pollTimer) {
                clearInterval(this._pollTimer);
                this._pollTimer = null;
            }
        },

        /**
         * 内部方法：从告警列表中提取最大 ID 并更新 lastAlertId
         */
        _updateLastId(alerts: AlertItem[]) {
            let maxId = this.lastAlertId;
            for (const alert of alerts) {
                if (!maxId || BigInt(alert.id) > BigInt(maxId)) {
                    maxId = alert.id;
                }
            }
            this.lastAlertId = maxId;
        },

        // ========== 三步面板专用操作 ==========

        /**
         * 获取存在告警的日期列表（第一步：日期选择器）
         */
        async fetchAlertDates() {
            try {
                const res: any = await request.get('/system/alert/dates');
                if (res.code === 200) {
                    this.alertDates = res.data || [];
                }
            } catch (e) {
                console.error('[通知中心] 获取告警日期列表失败:', e);
            }
        },

        /**
         * 按日期查询告警列表（第二步：日期下的告警）
         */
        async fetchAlertsByDate(date: string) {
            try {
                const res: any = await request.get('/system/alert/by-date', {
                    params: { date },
                });
                if (res.code === 200) {
                    this.dateAlerts = res.data || [];
                }
            } catch (e) {
                console.error('[通知中心] 按日期查询告警失败:', e);
            }
        },

        /**
         * 选中日期 → 进入列表视图
         */
        async selectDate(date: string) {
            this.selectedDate = date;
            this.selectedAlert = null; // 切换日期时，清空右侧详情板
            this.panelStep = 'list';
            await this.fetchAlertsByDate(date);
        },

        /**
         * 选中告警 → 进入详情视图 + 标记已读
         */
        async selectAlert(alert: AlertItem) {
            this.selectedAlert = alert;
            this.panelStep = 'detail';
            // 自动标记为已读
            if (alert.readStatus === 0) {
                await this.markAsRead(alert.id);
                // 同步本地 dateAlerts 中该条的状态
                const item = this.dateAlerts.find((a) => a.id === alert.id);
                if (item) item.readStatus = 1;
                this.selectedAlert = { ...alert, readStatus: 1 };
            }
        },

        /**
         * 从详情返回列表
         */
        goBackToList() {
            this.selectedAlert = null;
            this.panelStep = 'list';
        },

        /**
         * 从列表返回日期选择
         */
        goBackToDates() {
            this.selectedDate = null;
            this.dateAlerts = [];
            this.panelStep = 'dates';
        },

        /**
         * 重置面板状态（面板关闭时调用）
         */
        resetPanel() {
            this.panelStep = 'dates';
            this.selectedDate = null;
            this.selectedAlert = null;
            this.dateAlerts = [];
        },

        /**
         * 打开通知抽屉
         * 自动获取告警日期列表和未读数
         */
        openDrawer() {
            this.drawerVisible = true;
            this.fetchAlertDates();
            this.fetchUnreadCount();

            // 默认选中当天的日期
            const today = new Date();
            const year = today.getFullYear();
            const month = String(today.getMonth() + 1).padStart(2, '0');
            const day = String(today.getDate()).padStart(2, '0');
            const todayStr = `${year}-${month}-${day}`;
            this.selectDate(todayStr);
        },

        /**
         * 关闭通知抽屉 + 重置面板状态
         */
        closeDrawer() {
            this.drawerVisible = false;
            this.resetPanel();
        },
    },
});
