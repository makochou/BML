package com.bml.module.system.converter;

import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.vo.SysAlertVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 告警对象转换器。
 *
 * @author BML Team
 */
@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface AlertConverter {

    AlertConverter INSTANCE = Mappers.getMapper(AlertConverter.class);

    /**
     * Entity 转 VO。
     */
    SysAlertVO toVO(SysAlert entity);

    /**
     * Entity 列表转 VO 列表。
     */
    List<SysAlertVO> toVOList(List<SysAlert> list);
}
