/**
 * BML 全局应用状态管理
 * ──────────────────────────────────────────────────────
 * 管理与"布局 / 可访问性 / 偏好抽屉可见性"相关的全局状态：
 *   - 侧边菜单折叠 / 显隐；
 *   - 顶部导航 / 页脚显隐；
 *   - 色弱滤镜（无障碍特性）；
 *   - 偏好设置抽屉可见性。
 */
import { defineStore } from 'pinia';

/* ═══════════════════════════════════════════════════════
   常量
   ═══════════════════════════════════════════════════════ */

/** localStorage 中保存偏好设置的 key。 */
const SETTINGS_STORAGE_KEY = 'bml-app-settings';

/* ═══════════════════════════════════════════════════════
   类型定义
   ═══════════════════════════════════════════════════════ */

/**
 * 应用全局状态。
 */
export interface AppState {
    /** 色弱滤镜开关（无障碍特性）。 */
    colorWeek: boolean;
    /** 是否显示顶部导航栏。 */
    navbar: boolean;
    /** 是否显示侧边菜单。 */
    menu: boolean;
    /** 是否隐藏侧边菜单（响应式断点下使用）。 */
    hideMenu: boolean;
    /** 侧边菜单是否折叠。 */
    menuCollapse: boolean;
    /** 是否显示页脚。 */
    footer: boolean;
    /** 偏好设置抽屉可见性。 */
    settingsVisible: boolean;
}

/* ═══════════════════════════════════════════════════════
   持久化工具
   ═══════════════════════════════════════════════════════ */

/** 需要持久化到 localStorage 的字段。 */
const PERSISTED_KEYS: (keyof AppState)[] = ['colorWeek', 'menuCollapse'];

/**
 * 从 localStorage 读取保存的偏好设置。
 * @returns 部分 AppState 或空对象。
 */
function loadPersistedSettings(): Partial<AppState> {
    try {
        const raw = localStorage.getItem(SETTINGS_STORAGE_KEY);
        if (!raw) return {};
        return JSON.parse(raw) as Partial<AppState>;
    } catch {
        return {};
    }
}

/**
 * 将当前偏好设置保存到 localStorage。
 */
function savePersistedSettings(state: AppState) {
    try {
        const data: Record<string, unknown> = {};
        for (const key of PERSISTED_KEYS) {
            data[key] = state[key];
        }
        localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(data));
    } catch {
        /* 写入失败不影响运行 */
    }
}

/* ═══════════════════════════════════════════════════════
   Store 定义
   ═══════════════════════════════════════════════════════ */

export const useAppStore = defineStore('app', {
    state: (): AppState => {
        const saved = loadPersistedSettings();
        return {
            colorWeek: saved.colorWeek || false,
            navbar: true,
            menu: true,
            hideMenu: false,
            menuCollapse: saved.menuCollapse || false,
            footer: true,
            settingsVisible: false,
        };
    },
    actions: {
        /** 显示 / 隐藏偏好设置抽屉。 */
        toggleSettings(visible: boolean) {
            this.settingsVisible = visible;
        },
        /**
         * 切换色弱滤镜。
         * @param weak 是否启用。
         */
        toggleColorWeek(weak: boolean) {
            this.colorWeek = weak;
            if (typeof document !== 'undefined') {
                if (weak) {
                    document.documentElement.style.filter = 'invert(80%)';
                } else {
                    document.documentElement.style.filter = 'none';
                }
            }
            savePersistedSettings(this.$state);
        },
        /**
         * 批量更新偏好设置并自动持久化。
         */
        updateSettings(partial: Partial<AppState>) {
            this.$patch(partial);
            savePersistedSettings(this.$state);
        },
        /**
         * 应用启动时初始化偏好（当前仅恢复色弱滤镜）。
         */
        initAppPreferences() {
            try {
                if (this.colorWeek && typeof document !== 'undefined') {
                    document.documentElement.style.filter = 'invert(80%)';
                }
            } catch (error) {
                console.error('[BML AppStore] 初始化偏好失败:', error);
            }
        },
    },
});
