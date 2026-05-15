package com.bml.module.system.theme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.system.theme.entity.ThemePreset;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 主题预设 Mapper 接口。
 * <p>
 * 提供对 {@code bml_theme_preset} 表的访问，覆盖系统内置预设（如
 * {@code PRESET_BEST}、{@code PRESET_OCEAN}）以及平台级自定义预设的
 * 增删改查能力。
 * </p>
 *
 * <h3>JSON 列处理</h3>
 * <p>
 * 实体 {@link ThemePreset} 通过
 * {@link com.baomidou.mybatisplus.annotation.TableName#autoResultMap()} 启用自动
 * ResultMap，{@code profile_admin} / {@code profile_business} 两列由
 * {@link com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler}
 * 与 {@link com.bml.module.system.theme.entity.ThemeProfileJson} 互转。
 * 然而 {@code @TableField(typeHandler=...)} 仅自动应用于 BaseMapper 生成的内置
 * SQL；对于本接口中的自定义查询，需要在配套的 XML（
 * {@code mapper/theme/ThemePresetMapper.xml}）里声明 {@code <resultMap>} 并显式
 * 指定 {@code typeHandler}，才能保证 JSON 列被正确反序列化。
 * </p>
 *
 * <h3>方法约定</h3>
 * <ul>
 *   <li>{@link #selectByDefault()}：仅返回 {@code is_default = 1} 的那一行；
 *       系统种子保证仅有 {@code PRESET_BEST} 满足，因此返回单个对象，无记录时返回
 *       {@code null}。</li>
 *   <li>{@link #selectAllOrderBySort()}：按 {@code sort_order} 升序、相同时按
 *       {@code created_at} 升序返回所有预设，供前端预设网格展示。</li>
 * </ul>
 *
 * @author BML Team
 * @see ThemePreset
 * @see com.bml.module.system.theme.entity.ThemeProfileJson
 */
@Mapper
public interface ThemePresetMapper extends BaseMapper<ThemePreset> {

    /**
     * 查询当前系统默认预设（{@code is_default = 1}）。
     * <p>
     * 平台约定仅 {@code PRESET_BEST} 一条记录满足该条件，故只取 1 行。
     * 当数据库被异常清空（无任何默认预设）时返回 {@code null}，由调用方
     * 决定降级策略。
     * </p>
     *
     * @return 默认预设实体，未命中返回 {@code null}
     */
    ThemePreset selectByDefault();

    /**
     * 按 {@code sort_order} 升序返回所有预设。
     * <p>
     * 主排序键为 {@code sort_order}（数字越小越靠前），并以 {@code created_at}
     * 升序作为稳定次键。该结果用于前端 {@code ThemeSettingsPanel} 的预设网格、
     * Open API 文档示例以及管理端预设管理界面。
     * </p>
     *
     * @return 所有预设的有序列表（可能为空集合，永不为 {@code null}）
     */
    List<ThemePreset> selectAllOrderBySort();
}
