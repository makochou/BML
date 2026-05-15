/**
 * `themeStore` 单元测试。
 *
 * 覆盖 `patch` / `applyPreset` / `restore` / `onBroadcast` 行为，
 * 验证三方写入（store 状态 + localStorage + DOM CSS 变量）的一致性。
 *
 * 关联需求：Requirements 5.2 / 8.6。
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import type { ThemeBroadcastMessage, ThemePreset, ThemeProfile } from '@/types/theme';

/* ──────────────────────────────────────────────────────
   Mock 设置
   ────────────────────────────────────────────────────── */

// Mock @arco-design/web-vue Message
vi.mock('@arco-design/web-vue', () => ({
    Message: {
        warning: vi.fn(),
        success: vi.fn(),
        error: vi.fn(),
        info: vi.fn(),
    },
}));

// Mock themeEngine - applyTokens
vi.mock('@/utils/themeEngine', () => ({
    applyTokens: vi.fn((profile, scope) => {
        // 模拟 DOM 写入：将 primaryColor 写入 document.documentElement.style
        if (profile && profile.primaryColor) {
            document.documentElement.style.setProperty('--bml-color-primary', profile.primaryColor);
        }
    }),
}));

// Mock themeBroadcast
vi.mock('@/utils/themeBroadcast', () => ({
    themeBroadcast: {
        publish: vi.fn(),
        subscribe: vi.fn(() => vi.fn()),
        dispose: vi.fn(),
    },
}));

// Mock request 模块
vi.mock('@/utils/request', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

import { useThemeStore } from './theme';
import { applyTokens } from '@/utils/themeEngine';
import { themeBroadcast } from '@/utils/themeBroadcast';
import request from '@/utils/request';
import { Message } from '@arco-design/web-vue';

/* ──────────────────────────────────────────────────────
   测试数据
   ────────────────────────────────────────────────────── */

/** 基础 ADMIN Profile（与 store 内部 PRESET_BEST_PROFILE_ADMIN 对齐）。 */
const PRESET_BEST_ADMIN: ThemeProfile = {
    primaryColor: '#165DFF',
    secondaryColor: '#4080FF',
    accentColor: '#722ED1',
    successColor: '#00B42A',
    warningColor: '#FF7D00',
    errorColor: '#F53F3F',
    infoColor: '#86909C',
    textColor: '#1D2129',
    backgroundColor: '#FFFFFF',
    borderColor: '#E5E6EB',
    mode: 'LIGHT',
    radius: 'MEDIUM',
    density: 'DEFAULT',
    sidebarStyle: 'LIGHT',
    sidebarCollapsedStyle: 'LIGHT',
    headerStyle: 'LIGHT',
    fontScale: 'DEFAULT',
    presetRef: 'PRESET_BEST',
};

/** 基础 BUSINESS Profile。 */
const PRESET_BEST_BUSINESS: ThemeProfile = {
    ...PRESET_BEST_ADMIN,
    backgroundColor: '#F7F8FA',
};

/** 模拟预设数据。 */
const MOCK_PRESET_OCEAN: ThemePreset = {
    id: 'PRESET_OCEAN',
    name: '海洋蓝',
    description: '清新海洋风格',
    isBuiltIn: true,
    isDefault: false,
    sortOrder: 1,
    profileAdmin: {
        ...PRESET_BEST_ADMIN,
        primaryColor: '#0052D9',
        secondaryColor: '#366EF4',
        presetRef: 'PRESET_OCEAN',
    },
    profileBusiness: {
        ...PRESET_BEST_BUSINESS,
        primaryColor: '#0052D9',
        secondaryColor: '#366EF4',
        presetRef: 'PRESET_OCEAN',
    },
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
};

const MOCK_PRESET_BEST: ThemePreset = {
    id: 'PRESET_BEST',
    name: '最佳默认',
    isBuiltIn: true,
    isDefault: true,
    sortOrder: 0,
    profileAdmin: PRESET_BEST_ADMIN,
    profileBusiness: PRESET_BEST_BUSINESS,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
};

/* ──────────────────────────────────────────────────────
   辅助函数
   ────────────────────────────────────────────────────── */

/** localStorage 键名（与 store 内部逻辑一致）。 */
function lsKey(scope: 'ADMIN' | 'BUSINESS'): string {
    return `bml-theme-${scope.toLowerCase()}`;
}

/** 从 localStorage 读取并解析 Profile。 */
function readLsProfile(scope: 'ADMIN' | 'BUSINESS'): ThemeProfile | null {
    const raw = window.localStorage.getItem(lsKey(scope));
    return raw ? JSON.parse(raw) : null;
}

/* ──────────────────────────────────────────────────────
   测试主体
   ────────────────────────────────────────────────────── */

describe('themeStore 单元测试', () => {
    beforeEach(() => {
        window.localStorage.clear();
        document.documentElement.style.cssText = '';
        setActivePinia(createPinia());
        vi.clearAllMocks();
    });

    afterEach(() => {
        window.localStorage.clear();
        document.documentElement.style.cssText = '';
    });

    /* ═══════════════════════════════════════════════════
       patch(scope, partial)
       ═══════════════════════════════════════════════════ */

    describe('patch(scope, partial)', () => {
        it('合并部分字段后 store 状态正确更新', () => {
            const store = useThemeStore();
            const partial = { primaryColor: '#FF0000', mode: 'DARK' as const };

            store.patch('ADMIN', partial);

            expect(store.admin.primaryColor).toBe('#FF0000');
            expect(store.admin.mode).toBe('DARK');
            // 未修改的字段保持原值
            expect(store.admin.secondaryColor).toBe(PRESET_BEST_ADMIN.secondaryColor);
        });

        it('patch 后 localStorage 同步更新', () => {
            const store = useThemeStore();
            const partial = { primaryColor: '#00FF00' };

            store.patch('ADMIN', partial);

            const stored = readLsProfile('ADMIN');
            expect(stored).not.toBeNull();
            expect(stored!.primaryColor).toBe('#00FF00');
        });

        it('patch 后 DOM CSS 变量通过 applyTokens 更新', () => {
            const store = useThemeStore();
            const partial = { primaryColor: '#AABBCC' };

            store.patch('ADMIN', partial);

            // applyTokens mock 会将 primaryColor 写入 DOM
            expect(document.documentElement.style.getPropertyValue('--bml-color-primary')).toBe(
                '#AABBCC',
            );
        });

        it('patch 后触发 BroadcastChannel 广播', () => {
            const store = useThemeStore();
            const partial = { primaryColor: '#112233' };

            store.patch('BUSINESS', partial);

            expect(themeBroadcast.publish).toHaveBeenCalledTimes(1);
            expect(themeBroadcast.publish).toHaveBeenCalledWith(
                expect.objectContaining({
                    kind: 'profile-changed',
                    scope: 'BUSINESS',
                }),
            );
        });

        it('patch ADMIN 不影响 BUSINESS 状态与 localStorage', () => {
            const store = useThemeStore();
            const businessBefore = { ...store.business };

            store.patch('ADMIN', { primaryColor: '#FF0000' });

            expect(store.business).toEqual(businessBefore);
            // BUSINESS 的 localStorage 不应被写入（初始状态下无 localStorage 条目）
            // 但 store 初始化时可能已写入，所以检查值未变
            const businessLs = readLsProfile('BUSINESS');
            if (businessLs) {
                expect(businessLs.primaryColor).not.toBe('#FF0000');
            }
        });
    });

    /* ═══════════════════════════════════════════════════
       applyPreset(scope, id)
       ═══════════════════════════════════════════════════ */

    describe('applyPreset(scope, id)', () => {
        it('应用预设后 store.admin 等于预设的 profileAdmin 且 presetRef 已设置', () => {
            const store = useThemeStore();
            // 先填充预设列表
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];

            store.applyPreset('ADMIN', 'PRESET_OCEAN');

            expect(store.admin.primaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.primaryColor);
            expect(store.admin.secondaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.secondaryColor);
            expect(store.admin.presetRef).toBe('PRESET_OCEAN');
        });

        it('应用预设后 localStorage 同步更新', () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];

            store.applyPreset('ADMIN', 'PRESET_OCEAN');

            const stored = readLsProfile('ADMIN');
            expect(stored).not.toBeNull();
            expect(stored!.primaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.primaryColor);
            expect(stored!.presetRef).toBe('PRESET_OCEAN');
        });

        it('应用预设后触发 preset-applied 广播', () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];

            store.applyPreset('BUSINESS', 'PRESET_OCEAN');

            expect(themeBroadcast.publish).toHaveBeenCalledWith(
                expect.objectContaining({
                    kind: 'preset-applied',
                    scope: 'BUSINESS',
                    presetId: 'PRESET_OCEAN',
                }),
            );
        });

        it('预设不存在时设置 errors 且不修改 store 状态', () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST];
            const adminBefore = { ...store.admin };

            store.applyPreset('ADMIN', 'NON_EXISTENT');

            expect(store.errors.admin).not.toBeNull();
            expect(store.errors.admin!.code).toBe('THEME_PRESET_NOT_FOUND');
            expect(store.admin.primaryColor).toBe(adminBefore.primaryColor);
        });
    });

    /* ═══════════════════════════════════════════════════
       restore(scope)
       ═══════════════════════════════════════════════════ */

    describe('restore(scope)', () => {
        it('后端成功时 store 更新为服务端返回的 Profile', async () => {
            const store = useThemeStore();
            const serverProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#165DFF',
                presetRef: 'PRESET_BEST',
            };
            vi.mocked(request.post).mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: serverProfile,
            });

            await store.restore('ADMIN');

            expect(store.admin.primaryColor).toBe('#165DFF');
            expect(store.admin.presetRef).toBe('PRESET_BEST');
        });

        it('后端成功时 localStorage 同步更新', async () => {
            const store = useThemeStore();
            const serverProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#165DFF',
            };
            vi.mocked(request.post).mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: serverProfile,
            });

            await store.restore('ADMIN');

            const stored = readLsProfile('ADMIN');
            expect(stored).not.toBeNull();
            expect(stored!.primaryColor).toBe('#165DFF');
        });

        it('后端失败时回退到 PRESET_BEST 并显示提示', async () => {
            const store = useThemeStore();
            // 先修改 store 状态使其偏离 PRESET_BEST
            store.patch('ADMIN', { primaryColor: '#FF0000' });
            vi.clearAllMocks();

            vi.mocked(request.post).mockRejectedValueOnce(new Error('Network Error'));

            await store.restore('ADMIN');

            // 回退到 PRESET_BEST
            expect(store.admin.primaryColor).toBe(PRESET_BEST_ADMIN.primaryColor);
            expect(store.admin.mode).toBe(PRESET_BEST_ADMIN.mode);
            // 显示一次性提示
            expect(Message.warning).toHaveBeenCalledWith('主题云端同步失败，已使用本地配置');
            // errors 被设置
            expect(store.errors.admin).not.toBeNull();
            expect(store.errors.admin!.code).toBe('THEME_PERSIST_FAILED');
        });

        it('restore 后触发 restored 广播', async () => {
            const store = useThemeStore();
            vi.mocked(request.post).mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: PRESET_BEST_ADMIN,
            });

            await store.restore('BUSINESS');

            expect(themeBroadcast.publish).toHaveBeenCalledWith(
                expect.objectContaining({
                    kind: 'restored',
                    scope: 'BUSINESS',
                }),
            );
        });
    });

    /* ═══════════════════════════════════════════════════
       onBroadcast(msg)
       ═══════════════════════════════════════════════════ */

    describe('onBroadcast(msg)', () => {
        it('接收 profile-changed 消息后 store 更新', () => {
            const store = useThemeStore();
            const newProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#ABCDEF',
            };
            const msg: ThemeBroadcastMessage = {
                kind: 'profile-changed',
                scope: 'ADMIN',
                profile: newProfile,
                senderId: 'other-tab-123',
            };

            store.onBroadcast(msg);

            expect(store.admin.primaryColor).toBe('#ABCDEF');
        });

        it('接收 profile-changed 消息后 DOM 通过 applyTokens 更新', () => {
            const store = useThemeStore();
            const newProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#FEDCBA',
            };
            const msg: ThemeBroadcastMessage = {
                kind: 'profile-changed',
                scope: 'ADMIN',
                profile: newProfile,
                senderId: 'other-tab-456',
            };

            store.onBroadcast(msg);

            // applyTokens 被调用
            expect(applyTokens).toHaveBeenCalledWith(newProfile, 'ADMIN');
            // DOM 更新
            expect(document.documentElement.style.getPropertyValue('--bml-color-primary')).toBe(
                '#FEDCBA',
            );
        });

        it('接收 profile-changed 消息后 localStorage 不被写入', () => {
            const store = useThemeStore();
            // 先清空 localStorage 确保干净状态
            window.localStorage.clear();

            const newProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#999999',
            };
            const msg: ThemeBroadcastMessage = {
                kind: 'profile-changed',
                scope: 'ADMIN',
                profile: newProfile,
                senderId: 'other-tab-789',
            };

            // 记录 onBroadcast 前的 localStorage 状态
            const lsBefore = window.localStorage.getItem(lsKey('ADMIN'));

            store.onBroadcast(msg);

            // localStorage 不应被 onBroadcast 写入
            const lsAfter = window.localStorage.getItem(lsKey('ADMIN'));
            expect(lsAfter).toBe(lsBefore);
        });

        it('接收 restored 消息后 store 更新为 PRESET_BEST', () => {
            const store = useThemeStore();
            // 先修改状态
            store.patch('BUSINESS', { primaryColor: '#FF0000' });
            vi.clearAllMocks();

            const msg: ThemeBroadcastMessage = {
                kind: 'restored',
                scope: 'BUSINESS',
                senderId: 'other-tab-abc',
            };

            store.onBroadcast(msg);

            expect(store.business.primaryColor).toBe(PRESET_BEST_BUSINESS.primaryColor);
        });

        it('接收 preset-applied 消息后 store 更新为对应预设', () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];

            const msg: ThemeBroadcastMessage = {
                kind: 'preset-applied',
                scope: 'ADMIN',
                presetId: 'PRESET_OCEAN',
                senderId: 'other-tab-def',
            };

            store.onBroadcast(msg);

            expect(store.admin.primaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.primaryColor);
            expect(store.admin.presetRef).toBe('PRESET_OCEAN');
        });

        it('onBroadcast 不触发二次广播', () => {
            const store = useThemeStore();
            const msg: ThemeBroadcastMessage = {
                kind: 'profile-changed',
                scope: 'ADMIN',
                profile: { ...PRESET_BEST_ADMIN, primaryColor: '#111111' },
                senderId: 'other-tab-xyz',
            };

            store.onBroadcast(msg);

            // themeBroadcast.publish 不应被调用
            expect(themeBroadcast.publish).not.toHaveBeenCalled();
        });
    });
});
