package com.bml.module.system.converter;

import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.vo.SysAlertVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 告警对象转换器（手动实现）。
 * <p>
 * 负责将数据库实体 {@link SysAlert} 与视图对象 {@link SysAlertVO} 之间进行映射转换。
 * 采用手动编写方式替代 MapStruct 自动生成，以避免在不同 IDE 环境下
 * 因注解处理器缓存问题导致的 {@code ExceptionInInitializerError}。
 * </p>
 *
 * <h3>设计说明：</h3>
 * <ul>
 *   <li>使用单例模式（{@code INSTANCE}），与项目中其他 Converter 保持一致的调用风格；</li>
 *   <li>所有映射方法均做 null 安全校验，避免空指针异常；</li>
 *   <li>列表转换使用预分配容量的 {@link ArrayList}，减少扩容开销。</li>
 * </ul>
 *
 * @author BML Team
 * @see SysAlert
 * @see SysAlertVO
 */
public final class AlertConverter {

    /**
     * 全局唯一实例，与 MapStruct 的 {@code Mappers.getMapper()} 调用方式兼容。
     */
    public static final AlertConverter INSTANCE = new AlertConverter();

    /**
     * 私有构造器，禁止外部实例化。
     */
    private AlertConverter() {
    }

    /**
     * 将告警实体转换为告警视图对象。
     * <p>
     * 仅映射前端展示所需的字段，屏蔽 {@code createBy}、{@code updateBy}、{@code deleted} 等内部字段。
     * </p>
     *
     * @param entity 数据库告警实体，允许为 null
     * @return 告警视图对象，若入参为 null 则返回 null
     */
    public SysAlertVO toVO(SysAlert entity) {
        if (entity == null) {
            return null;
        }

        SysAlertVO vo = new SysAlertVO();
        vo.setId(entity.getId());
        vo.setAlertType(entity.getAlertType());
        vo.setAlertLevel(entity.getAlertLevel());
        vo.setAlertTitle(entity.getAlertTitle());
        vo.setAlertContent(entity.getAlertContent());
        vo.setReadStatus(entity.getReadStatus());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 将告警实体列表批量转换为告警视图对象列表。
     *
     * @param list 实体列表，允许为 null 或空
     * @return 视图对象列表；若入参为 null 返回空列表，不会返回 null
     */
    public List<SysAlertVO> toVOList(List<SysAlert> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysAlertVO> result = new ArrayList<>(list.size());
        for (SysAlert entity : list) {
            result.add(toVO(entity));
        }
        return result;
    }
}
