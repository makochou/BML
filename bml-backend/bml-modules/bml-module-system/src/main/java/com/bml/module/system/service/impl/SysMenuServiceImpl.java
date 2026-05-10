package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.security.model.LoginUser;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.mapper.SysMenuMapper;
import com.bml.module.system.mapper.SysRoleMapper;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.vo.RouterMetaVO;
import com.bml.module.system.vo.RouterVO;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单管理 服务实现
 * <p>
 * 提供菜单的查询、新增、修改、删除以及菜单树构建等功能。
 * </p>
 *
 * <h3>超级管理员处理策略：</h3>
 * <p>
 * 超级管理员（拥有 {@code role_key='admin'} 角色的用户）可查看所有菜单，
 * 通过 {@link #isSuperAdmin(Long)} 方法判断，不再依赖硬编码的用户 ID。
 * </p>
 *
 * <h3>依赖说明：</h3>
 * <p>
 * 此处直接注入 {@link SysRoleMapper} 而非 {@link com.bml.module.system.service.SysRoleService}，
 * 原因是 SysRoleServiceImpl 内部依赖了其他 Bean，若注入 SysRoleService 会产生循环依赖。
 * Mapper 层不存在此问题，且查询逻辑简单，直接使用 Mapper 更合适。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    /**
     * 直接注入 SysRoleMapper，避免与 SysRoleServiceImpl 产生循环依赖。
     * 使用 @Resource 字段注入（而非构造函数注入），进一步规避 Spring 循环依赖检测。
     */
    @Resource
    private SysRoleMapper roleMapper;

    /**
     * 判断指定用户是否为超级管理员。
     *
     * <p>判断依据（满足任一即为超级管理员）：</p>
     * <ol>
     *   <li>userId 等于 {@link GlobalConstants#SYSTEM_USER_ID}（bml 用户，ID=2）</li>
     *   <li>userId 等于 {@link GlobalConstants#ADMIN_USER_ID}（中台配置管理员，虚拟 ID=-1）</li>
     *   <li>当前登录用户的权限集合中包含 {@code *:*:*} 通配符（最通用的判断方式）</li>
     *   <li>用户拥有 {@code role_key='admin'} 的角色（数据库角色判断）</li>
     * </ol>
     *
     * <p><b>优先级说明：</b>前三种判断不需要查询数据库，性能更好；
     * 第四种需要查询 sys_user_role 和 sys_role 表，作为兜底判断。</p>
     *
     * @param userId 用户ID
     * @return {@code true} 表示是超级管理员
     */
    private boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        // 判断1：SYSTEM_USER_ID（当前为 2L，即 bml 用户）
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
            return true;
        }
        // 判断2：ADMIN_USER_ID（中台配置管理员虚拟 ID = -1L）
        if (GlobalConstants.ADMIN_USER_ID.equals(userId)) {
            return true;
        }
        // 判断3：当前登录用户权限集合中含 *:*:* 通配符（无需查库，性能最优）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null
                && loginUser.getPermissions() != null
                && loginUser.getPermissions().contains("*:*:*")) {
            return true;
        }
        // 判断4：检查用户是否拥有 role_key='admin' 的角色（兜底，需查库）
        // 直接查 Mapper，避免注入 SysRoleService 导致循环依赖
        return roleMapper.selectRolesByUserId(userId)
                .stream()
                .anyMatch(role -> GlobalConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode()));
    }

    /**
     * 根据条件查询菜单列表（管理端使用）
     * <p>
     * 超级管理员显示所有菜单，普通用户按角色过滤。
     * 注意：MyBatis-Plus 的 {@code @TableLogic} 会自动追加 deleted=0 条件，
     * 无需在业务代码中手动添加。
     * </p>
     *
     * @param menu   查询条件 DTO
     * @param userId 当前用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenuDTO menu, Long userId) {
        List<SysMenu> menuList;
        // 超级管理员显示所有菜单信息
        if (isSuperAdmin(userId)) {
            menuList = this.lambdaQuery()
                    .like(StrUtil.isNotEmpty(menu.getMenuName()), SysMenu::getMenuName, menu.getMenuName())
                    .eq(StrUtil.isNotEmpty(menu.getMenuType()), SysMenu::getMenuType, menu.getMenuType())
                    .eq(menu.getStatus() != null, SysMenu::getStatus, menu.getStatus())
                    .eq(menu.getVisible() != null, SysMenu::getVisible, menu.getVisible())
                    .orderByAsc(SysMenu::getParentId, SysMenu::getSort)
                    .list();
        } else {
            // 普通用户：按角色关联查询菜单
            menuList = baseMapper.selectMenuTreeByUserId(userId);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询菜单权限标识集合
     * <p>
     * 查询用户通过角色关联的所有菜单权限标识（perms），
     * 支持逗号分隔的多权限标识拆分。
     * 示例：perms="system:user:list,system:user:query" → {"system:user:list",
     * "system:user:query"}
     * </p>
     *
     * @param userId 用户ID
     * @return 权限标识集合
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        List<String> perms = baseMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StrUtil.isNotEmpty(perm)) {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单树信息（用于前端路由构建）
     * <p>
     * 仅查询目录（M）和菜单（C）类型，不包括按钮（F）。
     * 查询结果会自动构建为父子树形结构。
     * </p>
     *
     * @param userId 用户ID
     * @return 菜单树列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus;
        if (isSuperAdmin(userId)) {
            // 超级管理员显示所有正常状态的目录和菜单
            menus = this.lambdaQuery()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, GlobalConstants.STATUS_NORMAL)
                    .orderByAsc(SysMenu::getParentId, SysMenu::getSort)
                    .list();
        } else {
            menus = baseMapper.selectMenuTreeByUserId(userId);
        }
        return buildTree(menus, GlobalConstants.ROOT_NODE_ID);
    }

    /**
     * 构建前端路由所需的菜单结构
     * <p>
     * 将菜单实体转换为前端路由格式（RouterVO），
     * 输出字段包含 path/component/name/meta/children，前端可直接动态装载。
     * </p>
     *
     * @param menus 菜单列表
     * @return 路由菜单列表
     */
    @Override
    public List<RouterVO> buildMenus(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return new ArrayList<>();
        }
        List<RouterVO> routers = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (!isRouteMenu(menu)) {
                continue;
            }
            RouterVO router = buildRouter(menu, true);
            if (router != null) {
                routers.add(router);
            }
        }
        return routers;
    }

    /**
     * 查询权限分配面板所需的扁平菜单列表（业务系统角色授权专用）
     * <p>
     * <b>仅返回业务系统菜单</b>，即挂载在"系统管理"（path=
     * {@value GlobalConstants#BUSINESS_SYSTEM_MENU_PATH}）目录下的所有子孙菜单。
     * 中台管理的菜单（工作台、资产目录、授权治理、系统监控等）不包含在结果中。
     * </p>
     *
     * <h3>架构说明：</h3>
     * <ul>
     *   <li>中台管理平台（ip:port/admin）拥有独立的菜单体系，不参与业务系统角色授权</li>
     *   <li>业务系统（ip:port/）的菜单全部挂载在 path='system' 的顶级 M 目录下</li>
     *   <li>该目录本身 visible=0（在路由构建时隐藏），但其子菜单 visible=1，
     *       权限分配时需要包含该目录以保持树形结构的完整性</li>
     * </ul>
     *
     * <h3>过滤规则：</h3>
     * <ol>
     *   <li>查询所有 status=1（正常）的菜单，不过滤 visible（业务根目录 visible=0）</li>
     *   <li>定位业务系统根目录：path='system'、menuType='M'、parentId=0</li>
     *   <li>递归收集该根目录及其所有子孙菜单</li>
     *   <li>仅返回这些菜单，天然排除中台管理的所有菜单</li>
     * </ol>
     *
     * @return 扁平菜单列表（仅包含业务系统菜单，含根目录本身）
     */
    @Override
    public List<SysMenu> selectPermissionMenuList() {
        // 1. 查询所有正常状态的菜单（不过滤 visible，因为业务根目录 visible=0）
        List<SysMenu> allMenus = this.lambdaQuery()
                .eq(SysMenu::getStatus, GlobalConstants.STATUS_NORMAL)
                .orderByAsc(SysMenu::getParentId, SysMenu::getSort)
                .list();

        // 2. 定位业务系统菜单根目录（path='system' 的顶级 M 类型目录）
        Optional<SysMenu> businessRoot = allMenus.stream()
                .filter(m -> GlobalConstants.BUSINESS_SYSTEM_MENU_PATH.equals(m.getPath())
                        && "M".equals(m.getMenuType())
                        && GlobalConstants.ROOT_NODE_ID.equals(m.getParentId()))
                .findFirst();

        if (businessRoot.isEmpty()) {
            // 未找到业务系统根目录，返回空列表（避免误授权中台管理菜单）
            return Collections.emptyList();
        }

        // 3. 递归收集业务根目录及其所有子孙菜单的 ID
        Long rootId = businessRoot.get().getId();
        Set<Long> includeIds = new HashSet<>();
        includeIds.add(rootId);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysMenu m : allMenus) {
                if (!includeIds.contains(m.getId()) && includeIds.contains(m.getParentId())) {
                    includeIds.add(m.getId());
                    changed = true;
                }
            }
        }

        // 4. 仅返回业务系统菜单（根目录 + 所有子孙）
        return allMenus.stream()
                .filter(m -> includeIds.contains(m.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysMenu> selectMenuTree(SysMenuDTO dto) {
        SysMenuDTO safeDto = dto == null ? new SysMenuDTO() : dto;
        List<SysMenu> menus = selectMenuList(safeDto, GlobalConstants.SYSTEM_USER_ID);
        return buildTree(menus, GlobalConstants.ROOT_NODE_ID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertMenu(SysMenuDTO dto) {
        SysMenu menu = toMenuEntity(dto);
        if (menu.getParentId() == null) {
            menu.setParentId(GlobalConstants.ROOT_NODE_ID);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getVisible() == null) {
            menu.setVisible(GlobalConstants.STATUS_NORMAL);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(GlobalConstants.STATUS_NORMAL);
        }
        if (menu.getIsFrame() == null) {
            menu.setIsFrame(GlobalConstants.STATUS_DISABLE);
        }
        return this.save(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMenu(SysMenuDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("菜单ID不能为空");
        }
        if (Objects.equals(dto.getId(), dto.getParentId())) {
            throw new IllegalArgumentException("上级菜单不能选择当前菜单");
        }
        SysMenu menu = toMenuEntity(dto);
        return this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long menuId) {
        if (menuId == null) {
            throw new IllegalArgumentException("菜单ID不能为空");
        }
        long childCount = this.lambdaQuery().eq(SysMenu::getParentId, menuId).count();
        if (childCount > 0) {
            throw new IllegalArgumentException("存在下级菜单或权限项，不允许删除");
        }
        return this.removeById(menuId);
    }

    // ======================== 私有方法 ========================

    private SysMenu toMenuEntity(SysMenuDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("菜单信息不能为空");
        }
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        return menu;
    }

    /**
     * 构建树形结构
     * <p>
     * 从 menus 列表中，找出所有 parentId 等于 rootId 的节点作为根节点，
     * 递归构建完整的树形结构。使用 {@link Objects#equals} 进行 Long 比较，
     * 避免包装类 {@code ==} 比较在超出 -128~127 缓存范围时失效。
     * </p>
     *
     * @param menus  菜单平铺列表
     * @param rootId 根节点父ID（通常为 0）
     * @return 树形结构列表
     */
    private List<SysMenu> buildTree(List<SysMenu> menus, Long rootId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (Objects.equals(menu.getParentId(), rootId)) {
                buildChildren(menus, menu);
                returnList.add(menu);
            }
        }
        return returnList;
    }

    /**
     * 递归构建子节点
     *
     * @param list   菜单平铺列表
     * @param parent 父节点
     */
    private void buildChildren(List<SysMenu> list, SysMenu parent) {
        List<SysMenu> children = list.stream()
                .filter(m -> Objects.equals(m.getParentId(), parent.getId()))
                .collect(Collectors.toList());
        parent.setChildren(children);
        for (SysMenu child : children) {
            if (hasChildren(list, child)) {
                buildChildren(list, child);
            }
        }
    }

    /**
     * 判断节点是否有子节点
     *
     * @param list 菜单平铺列表
     * @param menu 当前节点
     * @return 是否有子节点
     */
    private boolean hasChildren(List<SysMenu> list, SysMenu menu) {
        return list.stream().anyMatch(m -> Objects.equals(m.getParentId(), menu.getId()));
    }

    /**
     * 将菜单节点转换为前端路由节点。
     */
    private RouterVO buildRouter(SysMenu menu, boolean root) {
        if (menu == null || !isRouteMenu(menu)) {
            return null;
        }

        RouterVO router = new RouterVO();
        router.setName(buildRouteName(menu));
        router.setPath(normalizePath(menu.getPath(), root));
        router.setComponent(resolveComponent(menu, root));
        router.setHidden(menu.getVisible() != null && GlobalConstants.STATUS_DISABLE.equals(menu.getVisible()));
        router.setAlwaysShow(CollUtil.isNotEmpty(menu.getChildren()) && "M".equalsIgnoreCase(menu.getMenuType()));
        router.setMeta(buildMeta(menu));

        if (CollUtil.isNotEmpty(menu.getChildren())) {
            List<RouterVO> children = new ArrayList<>();
            for (SysMenu child : menu.getChildren()) {
                RouterVO childRouter = buildRouter(child, false);
                if (childRouter != null) {
                    children.add(childRouter);
                }
            }
            router.setChildren(children);
        }
        return router;
    }

    /**
     * 仅目录与菜单参与前端路由构建。
     */
    private boolean isRouteMenu(SysMenu menu) {
        if (menu == null || StrUtil.isBlank(menu.getMenuType())) {
            return false;
        }
        return "M".equalsIgnoreCase(menu.getMenuType()) || "C".equalsIgnoreCase(menu.getMenuType());
    }

    /**
     * 生成稳定且可读的路由名称。
     */
    private String buildRouteName(SysMenu menu) {
        if (StrUtil.isNotBlank(menu.getComponent())) {
            String component = menu.getComponent();
            // ── 特殊组件路径：显式映射为语义化路由名称 ──
            // 规则：组件 name 必须与前端 defineOptions({ name: '...' }) 保持一致，
            //       否则 <keep-alive :include="cachedViews"> 无法正确缓存页面。
            if ("dashboard/Workplace".equals(component)) {
                return "Dashboard";
            }
            if ("api/ApiAccountManage".equals(component)) {
                return "ApiAccountManage";
            }
            if ("monitor/server/index".equals(component)) {
                return "ServerMonitor";
            }
            if ("monitor/alert/index".equals(component)) {
                return "AlertCenter";
            }
            // ── 通用规则：将路径分隔符转换为下划线 ──
            // 例如：system/config/index → system_config_index
            return component.replace("/", "_").replace("-", "_");
        }
        return "menu_" + menu.getId();
    }

    /**
     * 统一规范路由 path，根路由强制补全前导斜杠。
     */
    private String normalizePath(String path, boolean root) {
        String normalized = StrUtil.blankToDefault(path, "menu");
        if (root) {
            return StrUtil.startWith(normalized, "/") ? normalized : "/" + normalized;
        }
        return normalized;
    }

    /**
     * 目录节点默认映射到 Layout，菜单节点使用实际组件。
     */
    private String resolveComponent(SysMenu menu, boolean root) {
        if ("M".equalsIgnoreCase(menu.getMenuType()) && (root || StrUtil.isBlank(menu.getComponent()))) {
            return "Layout";
        }
        if (StrUtil.isBlank(menu.getComponent())) {
            return "common/FeatureDisabled";
        }
        return menu.getComponent();
    }

    /**
     * 构建路由元信息。
     */
    private RouterMetaVO buildMeta(SysMenu menu) {
        RouterMetaVO meta = new RouterMetaVO();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(StrUtil.blankToDefault(menu.getIcon(), "apps"));
        meta.setNoCache(Boolean.FALSE);
        return meta;
    }

}
