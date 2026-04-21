package com.bml.module.system.converter;

import com.bml.module.system.dto.SysOrgDTO;
import com.bml.module.system.entity.SysOrg;
import com.bml.module.system.vo.SysOrgVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 机构对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * 使用 {@code Mappers.getMapper()} 获取实例（无 Spring 容器依赖）。
 * </p>
 *
 * @author BML Team
 */
@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface OrgConverter {

    /** 转换器实例 */
    OrgConverter INSTANCE = Mappers.getMapper(OrgConverter.class);

    /**
     * DTO 转 Entity
     *
     * @param dto 机构传输对象
     * @return 机构实体
     */
    SysOrg toEntity(SysOrgDTO dto);

    /**
     * Entity 转 VO
     *
     * @param entity 机构实体
     * @return 机构视图对象
     */
    SysOrgVO toVO(SysOrg entity);

    /**
     * Entity 列表 转 VO 列表
     *
     * @param list 机构实体列表
     * @return 机构视图对象列表
     */
    List<SysOrgVO> toVOList(List<SysOrg> list);
}
