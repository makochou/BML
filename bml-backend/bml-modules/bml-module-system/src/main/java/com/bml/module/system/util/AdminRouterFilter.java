package com.bml.module.system.util;

import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.vo.RouterVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * 中台路由过滤器
 * <p>
 * 用于在 {@code /auth/routers} 接口中，针对<b>中台管理平台</b>的访问者，
 * 过滤掉属于业务系统的菜单子树，从而实现：
 * <ul>
 *   <li>中台管理平台（/admin/**）只展示中台自身的菜单（工作台、资产目录、授权治理、授权管理、系统配置等）</li>
 *   <li>业务系统（/**）不受任何影响，仍可使用完整的 sys_menu 树进行权限控制与鉴权</li>
 * </ul>
 * </p>
 *
 * <h3>设计目标：</h3>
 * <ol>
 *   <li><b>通用</b>：提供基于 Predicate 的根节点过滤接口，便于后续扩展其它过滤维度</li>
 *   <li><b>零侵入</b>：仅在控制器返回前对结果做过滤，不修改底层菜单查询与构建逻辑</li>
 *   <li><b>可维护</b>：过滤规则集中在本类中，避免在多处 Controller 硬编码</li>
 * </ol>
 *
 * <h3>过滤策略：</h3>
 * <p>
 * 仅过滤顶级根节点。当顶级节点的路径与 {@link GlobalConstants#BUSINESS_SYSTEM_MENU_PATH}
 * 相同且类型为 {@code M}（目录）时，视为业务系统根目录，整棵子树从结果中剔除。
 * </p>
 *
 * <h3>为什么只过滤根节点？</h3>
 * <p>
 * 业务系统菜单全部挂载在 {@code parent_id=0、path='system'、menu_type='M'} 的唯一顶级目录下，
 * 只要剔除该顶级节点，子孙节点即无法出现在前端侧边栏。若未来需要更细粒度的过滤，
 * 可扩展 {@link #filterRouters(List, Predicate)} 的过滤条件。
 * </p>
 *
 * @author BML Team
 * @see com.bml.module.system.controller.AuthController#getRouters()
 */
public final class AdminRouterFilter {

    /**
     * 业务系统菜单类型标识（M = 目录）。
     */
    private static final String MENU_TYPE_DIRECTORY = "M";

    /**
     * 私有构造函数，禁止实例化。
     */
    private AdminRouterFilter() {
    }

    /**
     * 判断当前登录用户是否为中台管理平台用户。
     * <p>
     * 中台管理平台的管理员账号不存储在 {@code sys_user} 表中，而是配置在 application.yml，
     * 登录时使用虚拟 userId = {@link GlobalConstants#ADMIN_USER_ID} 构建 {@link LoginUser}。
     * </p>
     *
     * @param loginUser 当前登录用户，可为 {@code null}
     * @return {@code true} 表示当前登录用户为中台管理平台用户
     */
    public static boolean isAdminPlatformUser(LoginUser loginUser) {
        if (loginUser == null) {
            return false;
        }
        return GlobalConstants.ADMIN_USER_ID.equals(loginUser.getUserId());
    }

    /**
     * 判断指定 {@link SysMenu} 节点是否为业务系统根目录。
     * <p>
     * 判定条件（需全部满足）：
     * <ol>
     *   <li>parentId = 0（顶级目录）</li>
     *   <li>menuType = "M"（目录类型）</li>
     *   <li>path = "system"（业务系统根路径标识）</li>
     * </ol>
     * </p>
     *
     * @param menu 菜单节点，可为 {@code null}
     * @return {@code true} 表示是业务系统根目录
     */
    public static boolean isBusinessSystemRoot(SysMenu menu) {
        if (menu == null) {
            return false;
        }
        return Objects.equals(GlobalConstants.ROOT_NODE_ID, menu.getParentId())
                && MENU_TYPE_DIRECTORY.equalsIgnoreCase(menu.getMenuType())
                && GlobalConstants.BUSINESS_SYSTEM_MENU_PATH.equalsIgnoreCase(menu.getPath());
    }

    /**
     * 判断指定 {@link RouterVO} 根节点是否为业务系统根目录。
     * <p>
     * 判定条件（需全部满足）：
     * <ol>
     *   <li>path 为 {@code system} 或 {@code /system}（业务系统根路径标识；
     *       注意 {@code SysMenuServiceImpl#normalizePath} 会为根节点补前导斜杠）</li>
     *   <li>children 非空（业务系统根目录必有子菜单，避免误伤偶发同名空节点）</li>
     * </ol>
     * </p>
     *
     * @param router 路由节点，可为 {@code null}
     * @return {@code true} 表示是业务系统根目录
     */
    public static boolean isBusinessSystemRootRouter(RouterVO router) {
        if (router == null) {
            return false;
        }
        String path = router.getPath();
        if (path == null) {
            return false;
        }
        // 去除前导斜杠后按大小写不敏感比较，兼容 "/system" 与 "system" 两种形态
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (!GlobalConstants.BUSINESS_SYSTEM_MENU_PATH.equalsIgnoreCase(normalized)) {
            return false;
        }
        return router.getChildren() != null && !router.getChildren().isEmpty();
    }

    /**
     * 将业务系统菜单子树从路由列表中剔除，返回只包含中台菜单的新列表。
     * <p>
     * 典型用法：在 {@code /auth/routers} 控制器中，检测到当前登录用户为中台管理平台用户时，
     * 调用此方法对 {@code menuService.buildMenus(...)} 的结果进行过滤后再返回给前端。
     * </p>
     *
     * <pre>{@code
     * List<RouterVO> all = menuService.buildMenus(menus);
     * return AdminRouterFilter.isAdminPlatformUser(loginUser)
     *         ? AdminRouterFilter.filterBusinessSystemRoot(all)
     *         : all;
     * }</pre>
     *
     * @param routers 原始路由列表，可为 {@code null} 或空
     * @return 过滤后的新列表；永不返回 {@code null}
     */
    public static List<RouterVO> filterBusinessSystemRoot(List<RouterVO> routers) {
        return filterRouters(routers, router -> !isBusinessSystemRootRouter(router));
    }

    /**
     * 通用路由过滤方法。
     * <p>
     * 仅对顶级节点应用过滤条件，子节点保持原样。
     * 提供本方法以便未来扩展其它维度的过滤需求（如按角色、按平台类型等）。
     * </p>
     *
     * @param routers   原始路由列表，可为 {@code null} 或空
     * @param predicate 过滤条件，只有 {@code test} 返回 {@code true} 的节点才保留
     * @return 过滤后的新列表；永不返回 {@code null}
     */
    public static List<RouterVO> filterRouters(List<RouterVO> routers, Predicate<RouterVO> predicate) {
        if (routers == null || routers.isEmpty() || predicate == null) {
            return routers == null ? new ArrayList<>() : new ArrayList<>(routers);
        }
        List<RouterVO> filtered = new ArrayList<>(routers.size());
        for (RouterVO router : routers) {
            if (predicate.test(router)) {
                filtered.add(router);
            }
        }
        return filtered;
    }

    /**
     * 从原始 {@link SysMenu} 菜单列表中剔除业务系统根目录及其所有后代节点。
     * <p>
     * 用于需要在 {@code buildMenus} 之前就过滤的场景，例如在服务层统一应用过滤规则。
     * 当前版本主要在控制器层使用 {@link #filterBusinessSystemRoot(List)}，
     * 此方法作为备用扩展点提供。
     * </p>
     *
     * @param menus 原始扁平菜单列表（含父子关系，已按 parent_id、sort 排序）
     * @return 过滤后的新列表；永不返回 {@code null}
     */
    public static List<SysMenu> excludeBusinessSystemMenus(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        // 1. 先找出业务系统根目录的 ID
        Long rootId = null;
        for (SysMenu menu : menus) {
            if (isBusinessSystemRoot(menu)) {
                rootId = menu.getId();
                break;
            }
        }
        if (rootId == null) {
            // 未找到业务系统根目录，直接返回副本（不改动原列表）
            return new ArrayList<>(menus);
        }
        // 2. 递归收集根目录及其所有后代节点的 ID
        List<Long> excludeIds = new ArrayList<>();
        excludeIds.add(rootId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysMenu menu : menus) {
                if (menu.getId() != null
                        && !excludeIds.contains(menu.getId())
                        && excludeIds.contains(menu.getParentId())) {
                    excludeIds.add(menu.getId());
                    changed = true;
                }
            }
        }
        // 3. 返回剔除后的新列表
        List<SysMenu> remaining = new ArrayList<>(menus.size());
        for (SysMenu menu : menus) {
            if (!excludeIds.contains(menu.getId())) {
                remaining.add(menu);
            }
        }
        return remaining;
    }
}
