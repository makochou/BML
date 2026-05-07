/**
 * 按钮级权限检查 composable。
 *
 * 从权限 Store 中读取当前用户的按钮权限列表，提供 `hasPermission(perm)` 方法
 * 供业务页面判断是否拥有特定操作权限（如编辑、删除、字段可见性等）。
 *
 * 设计说明：
 * - 权限标识格式与后端 @PreAuthorize 一致，例如 'system:org:edit'
 * - 超级管理员（权限含 '*:*:*'）自动拥有全部权限
 * - 通过 `buttonPermissionsLoaded` 标记区分两种状态：
 *     1. **未加载**（首屏 /auth/info 尚未返回）→ 默认放行，避免 UI 闪烁
 *     2. **已加载**（无论权限列表是否为空）→ 严格校验，无权限则隐藏
 *   后端接口仍会做最终鉴权，前端仅用于 UI 展示控制。
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
     * 检查当前用户是否拥有指定按钮/字段权限。
     *
     * 判定逻辑（按优先级）：
     * 1. 空字符串或未传入 → 不限制，返回 true
     * 2. 权限尚未从后端加载（buttonPermissionsLoaded === false）→ 默认放行
     *    （路由守卫会在页面渲染前完成加载，此分支仅为极端竞态兜底）
     * 3. 权限已加载 → 委托 permissionStore.hasPermission 做精确匹配
     *
     * @param perm 权限标识，如 'system:org:edit' 或 'system:org:field:phone'
     * @returns true = 有权限（允许显示） / false = 无权限（隐藏）
     */
    function hasPermission(perm: string): boolean {
        /* 空字符串或未传入 → 不限制 */
        if (!perm) return true;

        /*
         * 权限尚未从后端加载时（buttonPermissionsLoaded === false），
         * 默认放行，避免首屏因网络延迟而禁用按钮或隐藏字段。
         *
         * 注意：不能使用 buttonPermissions.length === 0 判断！
         * 因为用户可能确实没有任何 B/F 类型权限（仅有模块菜单权限），
         * 此时 length === 0 会导致所有按钮/字段误判为"放行"。
         */
        if (!permissionStore.buttonPermissionsLoaded) return true;

        return permissionStore.hasPermission(perm);
    }

    return { hasPermission };
}
