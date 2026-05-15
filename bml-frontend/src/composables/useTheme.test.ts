/**
 * `useTheme` Composable 单元测试。
 *
 * 覆盖：
 *   1. 作用域推断（scope inference）：通过 mock `vue-router` useRoute 控制路径
 *   2. `updateProfile(partial)`：happy path（store.patch + api PUT + 服务端回填）与失败降级
 *   3. `applyPreset(id)`：happy path + 预设不存在
 *   4. `restoreDefault()`：委托 store.restore
 *
 * 关联需求：Requirements 5.6 / 8.1 / 13.1。
 *
 * @vitest-environment jsdom
 */

// @vitest-environment jsdom

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import type { ThemePreset, ThemeProfile } from '@/types/theme';

/* ──────────────────────────────────────────────────────
   Mock 设置
   ────────────────────────────────────────────────────── */

// Mock vue-router useRoute
vi.mock('vue-router', () => ({
    useRoute: () => (globalThis as any).__mockRoute__,
}));

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
    applyTokens: vi.fn(),
}));

// Mock themeBroadcast
vi.mock('@/utils/themeBroadcast', () => ({
    themeBroadcast: {
        publish: vi.fn(),
        subscribe: vi.fn(() => vi.fn()),
        dispose: vi.fn(),
    },
}));

// Mock request 模块（themeStore 内部使用）
vi.mock('@/utils/request', () => ({
    default: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        delete: vi.fn(),
    },
}));

// Mock @/api/theme 模块（useTheme 内部使用）
vi.mock('@/api/theme', () => ({
    updateMyProfile: vi.fn(),
    parseFieldErrors: vi.fn(() => []),
}));

import { useTheme } from './useTheme';
import { useThemeStore } from '@/store/theme';
import request from '@/utils/request';
import { Message } from '@arco-design/web-vue';
import { updateMyProfile, parseFieldErrors } from '@/api/theme';

/** 类型安全的 mock 引用。 */
const mockUpdateMyProfile = vi.mocked(updateMyProfile);
const mockParseFieldErrors = vi.mocked(parseFieldErrors);
const mockMessage = vi.mocked(Message);

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
   测试主体
   ────────────────────────────────────────────────────── */

describe('useTheme Composable 单元测试', () => {
    beforeEach(() => {
        window.localStorage.clear();
        setActivePinia(createPinia());
        vi.clearAllMocks();
        // 默认路由为 admin
        (globalThis as any).__mockRoute__ = { path: '/admin/dashboard' };
    });

    afterEach(() => {
        window.localStorage.clear();
    });

    /* ═══════════════════════════════════════════════════
       一、作用域推断（scope inference）
       ═══════════════════════════════════════════════════ */

    describe('作用域推断', () => {
        it('/admin/... 路径推断为 ADMIN', () => {
            (globalThis as any).__mockRoute__ = { path: '/admin/settings' };

            const { scope } = useTheme();

            expect(scope).toBe('ADMIN');
        });

        it('/dashboard 路径推断为 BUSINESS', () => {
            (globalThis as any).__mockRoute__ = { path: '/dashboard' };

            const { scope } = useTheme();

            expect(scope).toBe('BUSINESS');
        });

        it('/system/... 路径推断为 BUSINESS', () => {
            (globalThis as any).__mockRoute__ = { path: '/system/users' };

            const { scope } = useTheme();

            expect(scope).toBe('BUSINESS');
        });

        it('无法识别的路径抛出 THEME_SCOPE_UNRESOLVED', () => {
            (globalThis as any).__mockRoute__ = { path: '/unknown/page' };

            expect(() => useTheme()).toThrowError();
            try {
                useTheme();
            } catch (err) {
                expect((err as Error & { code: string }).code).toBe('THEME_SCOPE_UNRESOLVED');
            }
        });

        it('显式传入 scope 时不依赖路由推断', () => {
            (globalThis as any).__mockRoute__ = { path: '/unknown/page' };

            const { scope } = useTheme('BUSINESS');

            expect(scope).toBe('BUSINESS');
        });
    });

    /* ═══════════════════════════════════════════════════
       二、updateProfile(partial)
       ═══════════════════════════════════════════════════ */

    describe('updateProfile(partial)', () => {
        it('happy path：本地 patch + API PUT + 服务端回填', async () => {
            (globalThis as any).__mockRoute__ = { path: '/admin/theme' };
            const serverProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#FF0066',
                presetRef: null,
            };
            mockUpdateMyProfile.mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: serverProfile,
            });

            const { updateProfile, profile, error } = useTheme('ADMIN');
            await updateProfile({ primaryColor: '#FF0066' });

            // store 最终应用了服务端回填的 Profile
            expect(profile.value.primaryColor).toBe('#FF0066');
            expect(profile.value.presetRef).toBeNull();
            // 错误应被清空
            expect(error.value).toBeNull();
            // API 被调用
            expect(mockUpdateMyProfile).toHaveBeenCalledTimes(1);
            expect(mockUpdateMyProfile).toHaveBeenCalledWith('ADMIN', expect.objectContaining({
                primaryColor: '#FF0066',
            }));
        });

        it('服务端回填覆盖本地值', async () => {
            const serverProfile: ThemeProfile = {
                ...PRESET_BEST_ADMIN,
                primaryColor: '#FF0066',
                // 服务端可能归一化某些字段
                secondaryColor: '#AAAAAA',
            };
            mockUpdateMyProfile.mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: serverProfile,
            });

            const { updateProfile, profile } = useTheme('ADMIN');
            await updateProfile({ primaryColor: '#FF0066' });

            // 服务端返回的 secondaryColor 覆盖了本地
            expect(profile.value.secondaryColor).toBe('#AAAAAA');
        });

        it('后端失败时保留本地更改并显示 Message.warning', async () => {
            mockUpdateMyProfile.mockRejectedValueOnce(new Error('Network Error'));
            mockParseFieldErrors.mockReturnValueOnce([]);

            const { updateProfile, profile, error } = useTheme('ADMIN');
            await updateProfile({ primaryColor: '#FF0000' });

            // 本地更改保留
            expect(profile.value.primaryColor).toBe('#FF0000');
            // 错误被设置
            expect(error.value).not.toBeNull();
            expect(error.value!.code).toBe('THEME_PERSIST_FAILED');
            // Message.warning 被调用
            expect(mockMessage.warning).toHaveBeenCalledWith('主题云端同步失败，已使用本地配置');
        });

        it('后端返回字段校验错误时 error 包含 fieldErrors', async () => {
            const fieldErrors = [
                { field: 'primaryColor', code: 'INVALID_HEX_COLOR', message: '非法颜色值' },
            ];
            const apiError = new Error('主题配置存在非法字段') as Error & { code?: number; data?: unknown };
            apiError.code = 40010;
            apiError.data = fieldErrors;
            mockUpdateMyProfile.mockRejectedValueOnce(apiError);
            mockParseFieldErrors.mockReturnValueOnce(fieldErrors);

            const { updateProfile, error } = useTheme('ADMIN');
            await updateProfile({ primaryColor: 'invalid' });

            expect(error.value).not.toBeNull();
            expect(error.value!.code).toBe('THEME_INVALID_PROFILE');
            expect(error.value!.fieldErrors).toEqual(fieldErrors);
        });
    });

    /* ═══════════════════════════════════════════════════
       三、applyPreset(id)
       ═══════════════════════════════════════════════════ */

    describe('applyPreset(id)', () => {
        it('happy path：应用预设后 profile 更新且同步到服务端', async () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];

            const serverProfile: ThemeProfile = {
                ...MOCK_PRESET_OCEAN.profileAdmin,
            };
            mockUpdateMyProfile.mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: serverProfile,
            });

            const { applyPreset, profile, error } = useTheme('ADMIN');
            await applyPreset('PRESET_OCEAN');

            expect(profile.value.primaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.primaryColor);
            expect(profile.value.presetRef).toBe('PRESET_OCEAN');
            expect(error.value).toBeNull();
            expect(mockUpdateMyProfile).toHaveBeenCalledTimes(1);
        });

        it('预设不存在时设置 THEME_PRESET_NOT_FOUND 错误且不调用 API', async () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST];

            const { applyPreset, error } = useTheme('ADMIN');
            await applyPreset('NON_EXISTENT');

            expect(error.value).not.toBeNull();
            expect(error.value!.code).toBe('THEME_PRESET_NOT_FOUND');
            // 不应调用后端 API
            expect(mockUpdateMyProfile).not.toHaveBeenCalled();
        });

        it('后端失败时保留本地预设更改并显示提示', async () => {
            const store = useThemeStore();
            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];
            mockUpdateMyProfile.mockRejectedValueOnce(new Error('Server Error'));

            const { applyPreset, profile, error } = useTheme('ADMIN');
            await applyPreset('PRESET_OCEAN');

            // 本地预设已应用
            expect(profile.value.primaryColor).toBe(MOCK_PRESET_OCEAN.profileAdmin.primaryColor);
            // 错误被设置
            expect(error.value).not.toBeNull();
            expect(error.value!.code).toBe('THEME_PERSIST_FAILED');
            // 提示已显示
            expect(mockMessage.warning).toHaveBeenCalledWith('主题云端同步失败，已使用本地配置');
        });
    });

    /* ═══════════════════════════════════════════════════
       四、restoreDefault()
       ═══════════════════════════════════════════════════ */

    describe('restoreDefault()', () => {
        it('委托 store.restore 完成恢复', async () => {
            vi.mocked(request.post).mockResolvedValueOnce({
                code: 200,
                message: 'success',
                data: PRESET_BEST_ADMIN,
            });

            const { restoreDefault, profile } = useTheme('ADMIN');
            await restoreDefault();

            // store.restore 被调用后 profile 应为 PRESET_BEST
            expect(profile.value.primaryColor).toBe(PRESET_BEST_ADMIN.primaryColor);
            expect(profile.value.mode).toBe(PRESET_BEST_ADMIN.mode);
        });

        it('store.restore 后端失败时不抛出异常', async () => {
            vi.mocked(request.post).mockRejectedValueOnce(new Error('Network Error'));

            const { restoreDefault } = useTheme('ADMIN');

            // 不应抛出异常
            await expect(restoreDefault()).resolves.toBeUndefined();
        });

        it('store.restore 后端失败时回退到 PRESET_BEST', async () => {
            const store = useThemeStore();
            // 先修改状态
            store.patch('ADMIN', { primaryColor: '#FF0000' });
            vi.clearAllMocks();

            vi.mocked(request.post).mockRejectedValueOnce(new Error('Network Error'));

            const { restoreDefault, profile } = useTheme('ADMIN');
            await restoreDefault();

            // 回退到 PRESET_BEST
            expect(profile.value.primaryColor).toBe(PRESET_BEST_ADMIN.primaryColor);
        });
    });

    /* ═══════════════════════════════════════════════════
       五、响应式视图
       ═══════════════════════════════════════════════════ */

    describe('响应式视图', () => {
        it('profile 反映 store 当前作用域状态', () => {
            const store = useThemeStore();
            const { profile } = useTheme('ADMIN');

            expect(profile.value.primaryColor).toBe(store.admin.primaryColor);

            store.patch('ADMIN', { primaryColor: '#AABBCC' });
            expect(profile.value.primaryColor).toBe('#AABBCC');
        });

        it('presets 反映 store 预设列表', () => {
            const store = useThemeStore();
            const { presets } = useTheme('ADMIN');

            expect(presets.value).toEqual([]);

            store.presets = [MOCK_PRESET_BEST, MOCK_PRESET_OCEAN];
            expect(presets.value).toHaveLength(2);
        });

        it('isLoading 反映 store 加载状态', () => {
            const store = useThemeStore();
            const { isLoading } = useTheme('ADMIN');

            expect(isLoading.value).toBe(false);

            store.loading.admin = true;
            expect(isLoading.value).toBe(true);
        });

        it('error 反映 store 错误状态', () => {
            const store = useThemeStore();
            const { error } = useTheme('ADMIN');

            expect(error.value).toBeNull();

            store.errors.admin = { code: 'THEME_PERSIST_FAILED', message: '测试错误' };
            expect(error.value).not.toBeNull();
            expect(error.value!.code).toBe('THEME_PERSIST_FAILED');
        });
    });
});
