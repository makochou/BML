/**
 * 按钮级权限检查 composable。
 *
 * 从权限 Store 中读取当前用户的按钮权限列表，提供 `hasPermission(perm)` 方法
 * 供业务页面判断是否拥有特定操作权限（如编辑、删除）。
 *
 * 设计说明：
 * - 权限标识格式与后端 @PreAuthorize 一致，例如 'system:org:edit'
 * - 超级管理员（权限含 '*:*:*'）自动拥有全部权限
 * - 若权限列表尚未加载（为空），则**默认放行**——避免网络延迟导致按钮不可用
 *   （后端仍会做最终鉴权，前端仅用于 UI 展示控制）
 *
 * 使用示例：
 * ```ts
 * const { hasPermission } = useButtonPermission();
 *
 * // 判断是否有编辑权限
 * const canEdit = computed(() => hasPermission('system:org:edit'));
 *
 * // 双击行：有编辑权限则打开编辑，否则打开查看
 * function handleRowDblClick(record: OrgVO) {
 *   openFormDialog(record, canEdit.value ? 'edit' : 'view');
 * }
 * ```
 */
import { usePermissionStore } from '../store/permission';

/**
 * 提供按钮级权限检查能力。
 *
 * @returns hasPermission 函数 — 传入权限标识字符串，返回 boolean
 */
export function useButtonPermission() {
    const permissionStore = usePermissionStore();

    /**
     * 检查当前用户是否拥有指定按钮权限。
     *
     * @param perm 权限标识，如 'system:org:edit'
     * @returns true = 有权限 / false = 无权限
     */
    function hasPermission(perm: string): boolean {
        /* 空字符串或未传入 → 不限制 */
        if (!perm) return true;

        /*
         * 权限列表尚未加载时（页面初始化瞬间 /auth/info 还未返回），
         * 默认放行，以保证首屏不会因网络延迟而禁用按钮。
         * 后端接口仍会做严格鉴权，前端仅做 UI 展示控制。
         */
        if (permissionStore.buttonPermissions.length === 0) return true;

        return permissionStore.hasPermission(perm);
    }

    return { hasPermission };
}
