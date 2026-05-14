/**
 * 按钮级权限检查 composable。
 *
 * 从权限 Store 中读取当前用户的按钮权限列表，提供权限检查方法
 * 供业务页面判断是否拥有特定操作权限（如编辑、删除、字段可见性等）。
 *
 * 设计说明：
 * - 权限标识格式与后端 @PreAuthorize 一致，例如 'system:org:edit'
 * - 超级管理员（权限含 '*:*:*'）自动拥有全部权限
 * - 按钮无权限时**变灰禁用**而非隐藏，让用户能看到功能存在但无法操作
 * - 通过 `buttonPermissionsLoaded` 标记区分两种状态：
 *     1. **未加载**（首屏 /auth/info 尚未返回）→ 默认放行，避免 UI 闪烁
 *     2. **已加载**（无论权限列表是否为空）→ 严格校验
 *   后端接口仍会做最终鉴权，前端仅用于 UI 展示控制。
 *
 * 使用示例：
 * ```ts
 * const { hasPermission, permDisabled } = useButtonPermission();
 *
 * // 方式1：按钮始终显示，无权限时禁用变灰
 * <a-button :disabled="permDisabled('system:org:edit')" @click="handleEdit">编辑</a-button>
 *
 * // 方式2：传统隐藏模式（用于确实需要隐藏的场景，如菜单项）
 * <a-menu-item v-if="hasPermission('system:org:edit')">编辑</a-menu-item>
 * ```
 */
import { usePermissionStore } from '../store/permission';

/**
 * 提供按钮级权限检查能力。
 *
 * @returns hasPermission 函数 — 传入权限标识字符串，返回 boolean（有权限=true）
 * @returns permDisabled 函数 — 传入权限标识字符串，返回 boolean（无权限=true，用于 :disabled 绑定）
 */
export function useButtonPermission() {
    const permissionStore = usePermissionStore();

    /**
     * 检查当前用户是否拥有指定按钮/字段权限。
     *
     * 判定逻辑（按优先级）：
     * 1. 空字符串或未传入 → 不限制，返回 true
     * 2. 权限尚未从后端加载（buttonPermissionsLoaded === false）→ 默认放行
     * 3. 权限已加载 → 委托 permissionStore.hasPermission 做精确匹配
     *
     * @param perm 权限标识，如 'system:org:edit' 或 'system:org:field:phone'
     * @returns true = 有权限（允许操作） / false = 无权限（禁止操作）
     */
    function hasPermission(perm: string): boolean {
        if (!perm) return true;
        if (!permissionStore.buttonPermissionsLoaded) return true;
        return permissionStore.hasPermission(perm);
    }

    /**
     * 检查指定权限是否应禁用（无权限时返回 true）。
     * 用于绑定按钮的 :disabled 属性，实现"无权限变灰"效果。
     *
     * @param perm 权限标识
     * @returns true = 无权限（按钮应禁用变灰） / false = 有权限（按钮可用）
     */
    function permDisabled(perm: string): boolean {
        return !hasPermission(perm);
    }

    return { hasPermission, permDisabled };
}
