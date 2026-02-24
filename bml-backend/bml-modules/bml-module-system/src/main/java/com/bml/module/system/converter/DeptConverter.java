package com.bml.module.system.converter;

import com.bml.module.system.dto.SysDeptDTO;
import com.bml.module.system.entity.SysDept;
import com.bml.module.system.vo.SysDeptVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 部门对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * 使用 {@code Mappers.getMapper()} 获取实例（无 Spring 容器依赖）。
 * </p>
 * <p>
 * <b>使用方式：</b> {@code DeptConverter.INSTANCE.toEntity(dto)}
 * </p>
 *
 * @author BML Team
 */
@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface DeptConverter {

    /** 转换器实例 */
    DeptConverter INSTANCE = Mappers.getMapper(DeptConverter.class);

    /**
     * DTO 转 Entity
     *
     * @param dto 部门传输对象
     * @return 部门实体
     */
    SysDept toEntity(SysDeptDTO dto);

    /**
     * Entity 转 VO
     *
     * @param entity 部门实体
     * @return 部门视图对象
     */
    SysDeptVO toVO(SysDept entity);

    /**
     * Entity 列表 转 VO 列表
     *
     * @param list 部门实体列表
     * @return 部门视图对象列表
     */
    List<SysDeptVO> toVOList(List<SysDept> list);
}
