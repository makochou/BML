package com.bml.module.system.theme.converter;

import com.bml.module.system.theme.dto.ThemePresetUpsertDTO;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.entity.ThemePreset;
import com.bml.module.system.theme.entity.ThemeProfileJson;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.vo.ThemePresetVO;
import com.bml.module.system.theme.vo.ThemeProfileVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 主题对象转换器。
 * <p>
 * 基于 MapStruct 在编译期自动生成 Entity ↔ DTO ↔ VO 之间的映射代码，
 * 集中承担主题模块所有跨层对象转换工作，避免在 Service 中散落 {@code BeanUtils.copyProperties}。
 * 与项目内 {@link com.bml.module.system.converter.OrgConverter} /
 * {@link com.bml.module.system.converter.RoleConverter} 等风格保持一致：
 * 不依赖 Spring 容器，通过静态 {@link #INSTANCE} 暴露单例
 * （由 {@link Mappers#getMapper(Class)} 在类加载阶段反射构建）。
 * </p>
 *
 * <h3>转换关系总览</h3>
 * <ul>
 *   <li><b>{@link ThemeProfileJson} ↔ {@link ThemeProfileDTO} / {@link ThemeProfileVO}</b>：
 *       同名同语义的扁平字段映射，由 MapStruct 自动逐字段拷贝；颜色字符串、
 *       枚举（{@code ThemeMode}/{@code RadiusStyle}/{@code Density}/{@code SidebarStyle}/{@code HeaderStyle}/{@code FontScale}）、
 *       {@code presetRef} 一一对应，无需 {@link Mapping} 显式声明。</li>
 *   <li><b>{@link ThemePreset} → {@link ThemePresetVO}</b>：元信息字段同名映射；
 *       JSON 嵌套字段 {@code profileAdmin} / {@code profileBusiness}
 *       （{@link ThemeProfileJson} → {@link ThemeProfileVO}）由 MapStruct 自动调用
 *       本接口中已声明的 {@link #toProfileVO(ThemeProfileJson)} 完成嵌套转换。</li>
 *   <li><b>{@link ThemePresetUpsertDTO} → {@link ThemePreset}</b>：用于新增 / 修改自定义
 *       预设；对受系统控制的字段（{@code id} / {@code isBuiltIn} / {@code isDefault} /
 *       {@code createdAt} / {@code updatedAt}）显式 {@code ignore}，由 Service / 数据库
 *       自行赋值；嵌套 DTO 通过 {@link #toJson(ThemeProfileDTO)} 转为 {@link ThemeProfileJson}。</li>
 *   <li><b>{@link ThemeUserSetting} → {@link ThemeProfileVO}</b>：从用户设置表读取
 *       {@code profile} JSON 列后展开为扁平 VO，回填 {@code presetRef}
 *       （详见 {@link #toProfileVO(ThemeUserSetting)}）。</li>
 * </ul>
 *
 * <h3>JSON / 枚举处理说明</h3>
 * <p>
 * JSON 嵌套字段在数据库层由 MyBatis-Plus
 * {@code com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler} 反序列化为
 * {@link ThemeProfileJson} 对象后到达本转换器；本接口仅做内存对象转换，
 * 不直接处理 JSON 字符串。枚举字段在 Java 层始终以强类型枚举传递，
 * MapStruct 默认按枚举常量名称（{@code name()}）一一对应，与数据库 JSON
 * 内容、前端 TypeScript 联合类型完全同形。
 * </p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * ThemeProfileVO vo = ThemeProfileConverter.INSTANCE.toProfileVO(profileJson);
 * ThemePresetVO presetVO = ThemeProfileConverter.INSTANCE.toPresetVO(preset);
 * ThemePreset entity = ThemeProfileConverter.INSTANCE.toPresetEntity(upsertDTO);
 * }</pre>
 *
 * @author BML Team
 * @see ThemeProfileJson
 * @see ThemePreset
 * @see ThemeUserSetting
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ThemeProfileConverter {

    /** 转换器单例（与项目其它 Converter 保持一致的获取方式）。 */
    ThemeProfileConverter INSTANCE = Mappers.getMapper(ThemeProfileConverter.class);

    // ─────────────────────────────────────────────────────────────────────────
    // ThemeProfileJson ↔ DTO / VO （扁平字段同名映射）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@link ThemeProfileDTO} → {@link ThemeProfileJson}。
     * <p>
     * 用于将控制器层接收到的请求体转换为可写入数据库 JSON 列的载体；
     * 全部字段同名同类型，MapStruct 自动逐字段拷贝。
     * </p>
     *
     * @param dto 主题配置 DTO（允许为 {@code null}，返回 {@code null}）
     * @return 主题配置 JSON 载体
     */
    ThemeProfileJson toJson(ThemeProfileDTO dto);

    /**
     * {@link ThemeProfileJson} → {@link ThemeProfileVO}。
     * <p>
     * 用于将数据库 JSON 列展开为对外输出的视图对象；同时被
     * {@link #toPresetVO(ThemePreset)} 内部递归调用，处理嵌套字段。
     * </p>
     *
     * @param json 主题配置 JSON 载体（允许为 {@code null}，返回 {@code null}）
     * @return 主题配置 VO
     */
    ThemeProfileVO toProfileVO(ThemeProfileJson json);

    /**
     * {@link ThemeProfileJson} → {@link ThemeProfileDTO}。
     * <p>
     * 主要用于内部测试或基于内置预设构造默认请求体的场景
     * （例如生成 PRESET_BEST 对应的 DTO 用于回填表单）。
     * </p>
     *
     * @param json 主题配置 JSON 载体（允许为 {@code null}，返回 {@code null}）
     * @return 主题配置 DTO
     */
    ThemeProfileDTO toProfileDTO(ThemeProfileJson json);

    // ─────────────────────────────────────────────────────────────────────────
    // ThemeUserSetting → ThemeProfileVO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@link ThemeUserSetting} → {@link ThemeProfileVO}。
     * <p>
     * 将用户设置记录展开为扁平 Profile 视图：
     * <ul>
     *   <li>展开 {@code profile} JSON 列内的 10 个颜色 + 7 个枚举字段；</li>
     *   <li>从外层记录回填 {@code presetRef}，覆盖 JSON 内同名字段
     *       （以外层为准，确保解引用 SQL 直接生效）。</li>
     * </ul>
     * </p>
     *
     * @param setting 用户主题设置实体（允许为 {@code null}，返回 {@code null}）
     * @return 主题配置 VO
     */
    @Mapping(target = "primaryColor", source = "profile.primaryColor")
    @Mapping(target = "secondaryColor", source = "profile.secondaryColor")
    @Mapping(target = "accentColor", source = "profile.accentColor")
    @Mapping(target = "successColor", source = "profile.successColor")
    @Mapping(target = "warningColor", source = "profile.warningColor")
    @Mapping(target = "errorColor", source = "profile.errorColor")
    @Mapping(target = "infoColor", source = "profile.infoColor")
    @Mapping(target = "textColor", source = "profile.textColor")
    @Mapping(target = "backgroundColor", source = "profile.backgroundColor")
    @Mapping(target = "borderColor", source = "profile.borderColor")
    @Mapping(target = "mode", source = "profile.mode")
    @Mapping(target = "radius", source = "profile.radius")
    @Mapping(target = "density", source = "profile.density")
    @Mapping(target = "sidebarStyle", source = "profile.sidebarStyle")
    @Mapping(target = "sidebarCollapsedStyle", source = "profile.sidebarCollapsedStyle")
    @Mapping(target = "headerStyle", source = "profile.headerStyle")
    @Mapping(target = "fontScale", source = "profile.fontScale")
    @Mapping(target = "presetRef", source = "presetRef")
    ThemeProfileVO toProfileVO(ThemeUserSetting setting);

    // ─────────────────────────────────────────────────────────────────────────
    // ThemePreset → ThemePresetVO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@link ThemePreset} → {@link ThemePresetVO}。
     * <p>
     * 元信息字段（{@code id} / {@code name} / {@code description} / {@code isBuiltIn} /
     * {@code isDefault} / {@code sortOrder} / {@code createdAt} / {@code updatedAt}）同名映射；
     * 嵌套字段 {@code profileAdmin} / {@code profileBusiness}（{@link ThemeProfileJson}
     * → {@link ThemeProfileVO}）由 MapStruct 自动调用 {@link #toProfileVO(ThemeProfileJson)}。
     * </p>
     *
     * @param preset 主题预设实体（允许为 {@code null}，返回 {@code null}）
     * @return 主题预设 VO
     */
    ThemePresetVO toPresetVO(ThemePreset preset);

    /**
     * 批量将主题预设实体列表转换为 VO 列表。
     *
     * @param presets 实体列表（允许为 {@code null}）
     * @return VO 列表（入参为 {@code null} 时由 MapStruct 返回 {@code null}）
     */
    List<ThemePresetVO> toPresetVOList(List<ThemePreset> presets);

    // ─────────────────────────────────────────────────────────────────────────
    // ThemePresetUpsertDTO → ThemePreset
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@link ThemePresetUpsertDTO} → {@link ThemePreset}。
     * <p>
     * 用于新增或修改自定义预设；以下字段由系统而非请求体决定，统一忽略：
     * <ul>
     *   <li>{@code id} — 新增时由 Service 生成；修改时由 URL 路径变量提供并在 Service 层回填。</li>
     *   <li>{@code isBuiltIn} — 自定义预设固定为 {@code false}（DDL 默认 0）。</li>
     *   <li>{@code isDefault} — 自定义预设固定为 {@code false}（仅 PRESET_BEST 为 true）。</li>
     *   <li>{@code createdAt} / {@code updatedAt} — 由数据库默认值与 {@code ON UPDATE} 维护。</li>
     * </ul>
     * 嵌套 {@link ThemeProfileDTO} 通过 {@link #toJson(ThemeProfileDTO)} 自动转为
     * {@link ThemeProfileJson}，与数据库 JSON 列契合。
     * </p>
     *
     * @param dto 预设新增 / 修改 DTO（允许为 {@code null}，返回 {@code null}）
     * @return 主题预设实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isBuiltIn", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ThemePreset toPresetEntity(ThemePresetUpsertDTO dto);
}
