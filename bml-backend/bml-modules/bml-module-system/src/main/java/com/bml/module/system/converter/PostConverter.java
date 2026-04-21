package com.bml.module.system.converter;

import com.bml.module.system.dto.SysPostDTO;
import com.bml.module.system.entity.SysPost;
import com.bml.module.system.vo.SysPostVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 岗位对象转换器
 * <p>
 * 基于 MapStruct 自动生成 DTO ↔ Entity ↔ VO 之间的转换代码。
 * </p>
 *
 * @author BML Team
 */
@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface PostConverter {

    /** 转换器实例 */
    PostConverter INSTANCE = Mappers.getMapper(PostConverter.class);

    /**
     * DTO 转 Entity
     */
    SysPost toEntity(SysPostDTO dto);

    /**
     * Entity 转 VO
     */
    SysPostVO toVO(SysPost entity);

    /**
     * Entity 列表 转 VO 列表
     */
    List<SysPostVO> toVOList(List<SysPost> list);
}
