package com.bml.module.system.converter;

import com.bml.module.system.dto.SysUserDTO;
import com.bml.module.system.entity.SysUser;
import com.bml.module.system.vo.SysUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 用户对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * 使用 {@code Mappers.getMapper()} 获取实例（无 Spring 容器依赖）。
 * </p>
 * <p>
 * <b>使用方式：</b> {@code UserConverter.INSTANCE.toEntity(dto)}
 * </p>
 *
 * @author BML Team
 */
@Mapper
public interface UserConverter {

    /** 转换器实例 */
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * DTO 转 Entity
     *
     * @param dto 用户传输对象
     * @return 用户实体
     */
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "realName", ignore = true)
    @Mapping(target = "orgId", ignore = true)
    @Mapping(target = "loginIp", ignore = true)
    @Mapping(target = "loginDate", ignore = true)
    SysUser toEntity(SysUserDTO dto);

    /**
     * Entity 转 VO
     * <p>
     * 部门名称和角色名称需要在 Service 层手动填充或通过关联查询获取。
     * </p>
     *
     * @param entity 用户实体
     * @return 用户视图对象
     */
    @Mapping(target = "deptName", ignore = true)
    @Mapping(target = "roleNames", ignore = true)
    @Mapping(target = "roleIds", ignore = true)
    SysUserVO toVO(SysUser entity);

    /**
     * Entity 列表 转 VO 列表
     *
     * @param list 用户实体列表
     * @return 用户视图对象列表
     */
    List<SysUserVO> toVOList(List<SysUser> list);
}
