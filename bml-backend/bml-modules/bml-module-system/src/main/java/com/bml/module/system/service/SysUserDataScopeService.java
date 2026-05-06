package com.bml.module.system.service;

import com.bml.core.base.service.BaseService;
import com.bml.module.system.entity.SysUserDataScope;

/**
 * 用户个人数据权限配置 服务接口
 * <p>
 * 提供用户个人数据权限的管理功能。个人数据权限优先级高于角色数据权限。
 * </p>
 * <p>
 * <b>优先级规则：</b>
 * <ol>
 *   <li>若用户有 sys_user_data_scope 记录且 status=1 且未过期，以此为准</li>
 *   <li>否则以用户所有角色中最宽的 data_scope 为准（取并集）</li>
 * </ol>
 * </p>
 *
 * @author BML Team
 */
public interface SysUserDataScopeService extends BaseService<SysUserDataScope> {

    /**
     * 根据用户ID查询生效中的个人数据权限配置
     * <p>
     * 返回 status=1 且未过期的记录，若不存在则返回 null。
     * </p>
     *
     * @param userId 用户ID
     * @return 用户个人数据权限配置，不存在则返回 null
     */
    SysUserDataScope selectActiveByUserId(Long userId);

    /**
     * 新增或更新用户个人数据权限配置
     * <p>
     * 如果用户已存在配置则更新，不存在则新增。
     * </p>
     *
     * @param dataScope 数据权限配置
     * @return 是否成功
     */
    boolean saveOrUpdateByUserId(SysUserDataScope dataScope);

    /**
     * 根据用户ID删除个人数据权限配置（逻辑删除）
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean removeByUserId(Long userId);
}
