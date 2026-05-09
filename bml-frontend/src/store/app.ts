/**
 * BML 全局应用状态管理
 * ──────────────────────────────────────────────────────
 * 管理主题色、暗色模式、侧边栏/头部外观、布局折叠等全局配置。
 * 所有外观类设置自动持久化到 localStorage，刷新后恢复。
 */
import { defineStore } from 'pinia';
import { applyThemeColor, getSavedThemeColor } from '../utils/theme';

/* ═══════════════════════════════════════════════════════
   常量
   ═══════════════════════════════════════════════════════ */

/** localStorage 中保存偏好设置的 key */
const SETTINGS_STORAGE_KEY = 'bml-app-settings';

/* ═══════════════════════════════════════════════════════
   类型定义
   ═══════════════════════════════════════════════════════ */

export interface AppState {
    /** 全局明暗模式 */
    theme: 'light' | 'dark';
    /** 色弱滤镜开关 */
    colorWeek: boolean;
    /** 是否显示顶部导航栏 */
    navbar: boolean;
    /** 是否显示侧边菜单 */
    menu: boolean;
    /** 是否隐藏侧边菜单（响应式） */
    hideMenu: boolean;
    /** 侧边菜单是否折叠 */
    menuCollapse: boolean;
    /** 是否显示页脚 */
    footer: boolean;
    /** 当前主题色（HEX） */
    themeColor: string;
    /** 头部栏外观主题 */
    headerTheme: 'light' | 'dark' | 'primary' | 'transparent';
    /** 侧边栏外观主题 */
    sidebarTheme: 'light' | 'dark' | 'white' | 'primary';
    /** 偏好设置抽屉可见性 */
    settingsVisible: boolean;
}

/* ═══════════════════════════════════════════════════════
   持久化工具
   ═══════════════════════════════════════════════════════ */

/** 需要持久化到 localStorage 的字段 */
const PERSISTED_KEYS: (keyof AppState)[] = [
    'theme', 'colorWeek', 'headerTheme', 'sidebarTheme', 'menuCollapse',
];

/**
 * 从 localStorage 读取保存的偏好设置
 * @returns 部分 AppState 或空对象
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
 * 将当前偏好设置保存到 localStorage
 */
function savePersistedSettings(state: AppState) {
    try {
        const data: Record<string, unknown> = {};
        for (const key of PERSISTED_KEYS) {
            data[key] = state[key];
        }
        localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(data));
    } catch {
        // 写入失败不影响运行
    }
}

/* ═══════════════════════════════════════════════════════
   Store 定义
   ═══════════════════════════════════════════════════════ */

export const useAppStore = defineStore('app', {
    state: (): AppState => {
        const saved = loadPersistedSettings();
        return {
            theme: saved.theme || 'light',
            colorWeek: saved.colorWeek || false,
            navbar: true,
            menu: true,
            hideMenu: false,
            menuCollapse: saved.menuCollapse || false,
            footer: true,
            themeColor: getSavedThemeColor(),
            headerTheme: saved.headerTheme || 'transparent',
            sidebarTheme: saved.sidebarTheme || 'white',
            settingsVisible: false,
        };
    },
    actions: {
        /**
         * 切换明暗模式
         * @param dark - true=暗色 / false=亮色
         */
        toggleTheme(dark: boolean) {
            if (dark) {
                this.theme = 'dark';
                document.body.setAttribute('arco-theme', 'dark');
            } else {
                this.theme = 'light';
                document.body.removeAttribute('arco-theme');
            }
            savePersistedSettings(this.$state);
        },
        /** 显示/隐藏偏好设置抽屉 */
        toggleSettings(visible: boolean) {
            this.settingsVisible = visible;
        },
        /**
         * 切换色弱滤镜
         * @param weak - 是否启用
         */
        toggleColorWeek(weak: boolean) {
            this.colorWeek = weak;
            if (weak) {
                document.documentElement.style.filter = 'invert(80%)';
            } else {
                document.documentElement.style.filter = 'none';
            }
            savePersistedSettings(this.$state);
        },
        /**
         * 批量更新设置并自动持久化
         */
        updateSettings(partial: Partial<AppState>) {
            this.$patch(partial);
            savePersistedSettings(this.$state);
        },
        /**
         * 初始化主题：在应用启动时调用，
         * 从 localStorage 恢复保存的主题色并应用 CSS 变量，
         * 同时恢复暗色模式状态
         */
        initTheme() {
            try {
                /* 恢复暗色模式 */
                if (this.theme === 'dark') {
                    document.body.setAttribute('arco-theme', 'dark');
                }
                /* 恢复色弱滤镜 */
                if (this.colorWeek) {
                    document.documentElement.style.filter = 'invert(80%)';
                }
                /* 应用主题色 CSS 变量 */
                applyThemeColor(this.themeColor);
            } catch (error) {
                console.error('[BML Store] 初始化主题失败:', error);
            }
        },
    },
});
