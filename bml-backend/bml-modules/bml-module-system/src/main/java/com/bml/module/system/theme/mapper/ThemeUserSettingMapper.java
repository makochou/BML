package com.bml.module.system.theme.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.enums.ThemeScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户主题设置 Mapper 接口。
 * <p>
 * 对应表 {@code bml_theme_user_setting}，提供按 ({@code user_id},{@code scope})
 * 唯一查询，以及自定义预设删除时的批量解引用 SQL。配合
 * {@link com.bml.module.system.theme.entity.ThemeUserSetting} 上的
 * {@code @TableName(autoResultMap = true)} 与 {@code JacksonTypeHandler}，
 * 即便使用 {@link Select} 注解仍能正确反序列化 {@code profile} JSON 列为
 * {@link com.bml.module.system.theme.entity.ThemeProfileJson}。
 * </p>
 *
 * @author BML Team
 * @see com.bml.module.system.theme.entity.ThemeUserSetting
 * @see ThemeScope
 */
@Mapper
public interface ThemeUserSettingMapper extends BaseMapper<ThemeUserSetting> {

    /**
     * 按用户 ID 与作用域查询用户主题设置。
     * <p>
     * 与数据库唯一索引 {@code uk_user_scope (user_id, scope)} 对齐，最多返回 1 条。
     * 调用方传入的 {@code scope} 应使用 {@link ThemeScope#name()}（即
     * {@code "ADMIN"} 或 {@code "BUSINESS"}），与表中字符串列对齐。
     * </p>
     *
     * @param userId 用户 ID（不为 {@code null}）
     * @param scope  作用域字符串（{@code "ADMIN"} 或 {@code "BUSINESS"}）
     * @return 命中的用户主题设置记录；未命中返回 {@code null}
     */
    @Select("SELECT * FROM bml_theme_user_setting WHERE user_id = #{userId} AND scope = #{scope} LIMIT 1")
    ThemeUserSetting selectByUserAndScope(@Param("userId") Long userId, @Param("scope") String scope);

    /**
     * 按预设 ID 批量解引用 {@code preset_ref}。
     * <p>
     * 在自定义预设被删除时调用，将所有引用该预设的 {@code bml_theme_user_setting}
     * 行的 {@code preset_ref} 字段置为 {@code NULL}，但保留其 {@code profile}
     * JSON 字段的具体 Token 值不变（参见需求 R12.AC4 / 设计文档 Property 5）。
     * </p>
     *
     * @param presetId 待解引用的预设 ID（不为 {@code null}）
     * @return 受影响的行数
     */
    @Update("UPDATE bml_theme_user_setting SET preset_ref = NULL WHERE preset_ref = #{presetId}")
    int clearPresetRefByPresetId(@Param("presetId") String presetId);
}
