package com.bml.module.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.base.service.impl.BaseServiceImpl;
import com.bml.core.common.constant.GlobalConstants;
import com.bml.core.framework.license.BmlLicense;
import com.bml.core.framework.license.BmlLicenseHolder;
import com.bml.core.framework.license.LicenseFeatureConstants;
import com.bml.module.system.converter.MenuConverter;
import com.bml.module.system.dto.SysMenuDTO;
import com.bml.module.system.entity.SysMenu;
import com.bml.module.system.mapper.SysMenuMapper;
import com.bml.module.system.service.SysMenuService;
import com.bml.module.system.vo.RouterMetaVO;
import com.bml.module.system.vo.RouterVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单管理 服务实现
 * <p>
 * 提供菜单的查询、新增、修改、删除以及菜单树构建等功能。
 * 超级管理员（{@link GlobalConstants#SYSTEM_USER_ID}）可查看所有菜单，
 * 普通用户仅能查看自身角色关联的菜单。
 * </p>
 *
 * @author BML Team
 */
@Service
public class SysMenuServiceImpl extends BaseServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Resource
    private BmlLicenseHolder licenseHolder;

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
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
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
        if (GlobalConstants.SYSTEM_USER_ID.equals(userId)) {
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
        // 根据许可证授权的功能模块过滤菜单
        return filterByLicenseFeatures(routers);
    }

    /**
     * 校验同一父级下菜单名称是否唯一
     *
     * @param menu 菜单 DTO（包含 menuName、parentId、id）
     * @return {@code true} 表示名称已存在（不唯一），{@code false} 表示唯一
     */
    @Override
    public boolean checkMenuNameUnique(SysMenuDTO menu) {
        long count = this.lambdaQuery()
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId, menu.getParentId())
                .ne(menu.getId() != null, SysMenu::getId, menu.getId())
                .count();
        return count > 0;
    }

    /**
     * 新增菜单
     *
     * @param menuDto 菜单 DTO
     * @return 是否成功
     */
    @Override
    public boolean insertMenu(SysMenuDTO menuDto) {
        SysMenu menu = MenuConverter.INSTANCE.toEntity(menuDto);
        return this.save(menu);
    }

    /**
     * 修改菜单
     *
     * @param menuDto 菜单 DTO
     * @return 是否成功
     */
    @Override
    public boolean updateMenu(SysMenuDTO menuDto) {
        SysMenu menu = MenuConverter.INSTANCE.toEntity(menuDto);
        return this.updateById(menu);
    }

    // ======================== 私有方法 ========================

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
            if ("dashboard/Workplace".equals(component)) {
                return "Dashboard";
            }
            if ("api/ApiAccountManage".equals(component)) {
                return "ApiAccountManage";
            }
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

    // ======================== 许可证功能模块过滤 ========================

    /**
     * 根据许可证授权的功能模块过滤路由菜单。
     * <p>
     * 遍历路由树，移除许可证未授权模块对应的菜单项。
     * 对于目录节点（component = Layout），如果过滤后没有可见子菜单则一并移除。
     * 许可证未启用或未加载时不过滤（由拦截器负责整体拦截）。
     * </p>
     *
     * @param routers 原始路由列表
     * @return 过滤后的路由列表
     */
    private List<RouterVO> filterByLicenseFeatures(List<RouterVO> routers) {
        if (!licenseHolder.isEnabled()) {
            return routers;
        }
        BmlLicense license = licenseHolder.getCurrentLicense();
        if (license == null) {
            return routers;
        }
        List<String> features = license.getFeatures();
        if (features == null) {
            features = Collections.emptyList();
        }
        return doFilterRouters(routers, features);
    }

    /**
     * 递归过滤路由树。
     *
     * @param routers  路由列表
     * @param features 许可证已授权的功能模块列表
     * @return 过滤后的路由列表
     */
    private List<RouterVO> doFilterRouters(List<RouterVO> routers, List<String> features) {
        List<RouterVO> result = new ArrayList<>();
        for (RouterVO router : routers) {
            // 递归过滤子路由
            if (router.getChildren() != null && !router.getChildren().isEmpty()) {
                router.setChildren(doFilterRouters(router.getChildren(), features));
            }

            String component = router.getComponent();
            if ("Layout".equals(component)) {
                // 目录节点：至少保留一个可见子菜单才显示
                if (router.getChildren() != null && !router.getChildren().isEmpty()) {
                    result.add(router);
                }
            } else {
                // 叶子菜单：检查功能模块授权
                String requiredFeature = LicenseFeatureConstants.getRequiredFeature(component);
                if (requiredFeature == null || features.contains(requiredFeature)) {
                    result.add(router);
                }
            }
        }
        return result;
    }
}
