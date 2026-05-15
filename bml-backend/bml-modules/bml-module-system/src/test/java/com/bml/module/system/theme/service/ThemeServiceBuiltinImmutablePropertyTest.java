package com.bml.module.system.theme.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.theme.constant.ThemeConstants;
import com.bml.module.system.theme.dto.ThemePresetUpsertDTO;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.entity.ThemePreset;
import com.bml.module.system.theme.entity.ThemeProfileJson;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeErrorCode;
import com.bml.module.system.theme.enums.ThemeMode;
import com.bml.module.system.theme.mapper.ThemePresetMapper;
import com.bml.module.system.theme.mapper.ThemeUserSettingMapper;
import com.bml.module.system.theme.service.impl.ThemeServiceImpl;
import com.bml.module.system.theme.validator.ThemeProfileValidator;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.BeforeTry;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ThemeServiceImpl} 内置预设不可变属性测试（基于 jqwik + Mockito）。
 *
 * <h3>Property 4 — 内置预设不可变（{@code P_BUILTIN_IMMUTABLE}）</h3>
 * <p>
 * <b>Validates: Requirements 2.2, 12.2</b>
 * </p>
 *
 * <h3>属性语义</h3>
 * <p>
 * 对任意 {@code is_built_in=true} 的预设执行
 * {@link ThemeServiceImpl#updatePreset(String, ThemePresetUpsertDTO) updatePreset} 或
 * {@link ThemeServiceImpl#deletePreset(String) deletePreset} 时，必须满足：
 * </p>
 * <ol>
 *   <li>抛出 {@link BusinessException}，错误码为
 *       {@link ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE}；</li>
 *   <li>{@code bml_theme_preset} 表对应行不发生任何字段变更
 *       —— 体现为 mock 的 {@link ThemePresetMapper#updateById(Object)} /
 *       {@link ThemePresetMapper#deleteById(java.io.Serializable)} /
 *       {@link ThemePresetMapper#insert(Object)} 全部 <em>零调用</em>；</li>
 *   <li>{@code bml_theme_user_setting} 表也不应被触及
 *       —— 体现为 mock 的解引用 SQL
 *       {@link ThemeUserSettingMapper#clearPresetRefByPresetId(String)}
 *       与其它写入 API 全部 <em>零调用</em>；</li>
 *   <li>实体对象的字段（{@code id / name / description / isBuiltIn / isDefault /
 *       sortOrder / profileAdmin / profileBusiness / createdAt / updatedAt}）
 *       与拒绝前的快照逐一相等，验证「内存中也未发生任何修改」。</li>
 * </ol>
 *
 * <h3>实现约束（来自 {@link ThemeServiceImpl}）</h3>
 * <p>
 * 内置保护检查在 {@code rejectIfBuiltIn} 中执行，<em>位于校验与任何写操作之前</em>，
 * 因此本测试仅需 mock {@link ThemePresetMapper#selectById(java.io.Serializable)}
 * 返回一个 {@code isBuiltIn=true} 的实体即可触发拒绝路径；
 * {@link ThemeProfileValidator} 不会被调用，可使用真实实例（行为更接近生产）。
 * </p>
 *
 * <h3>jqwik 输入空间</h3>
 * <ul>
 *   <li>{@code presetId} —— 由 {@link #presetIds()} 提供：包含语义 ID
 *       （{@code PRESET_BEST}、{@code PRESET_OCEAN}）以及任意非空字母数字串；</li>
 *   <li>{@code operation} —— {@link Operation#UPDATE} 与 {@link Operation#DELETE}
 *       等概率出现，对两条受保护路径同时形成覆盖。</li>
 * </ul>
 * <p>
 * 失败时 jqwik 会自动收缩到最短的字符串与最简单的操作枚举，便于定位。
 * </p>
 *
 * @author BML Team
 */
class ThemeServiceBuiltinImmutablePropertyTest {

    /** 主题预设 mapper（mock）。 */
    private ThemePresetMapper themePresetMapper;

    /** 用户主题设置 mapper（mock）。 */
    private ThemeUserSettingMapper themeUserSettingMapper;

    /** 真实校验器：本路径下不会被触及，仅作为依赖注入。 */
    private ThemeProfileValidator themeProfileValidator;

    /** 被测对象：服务实现。 */
    private ThemeServiceImpl themeService;

    /**
     * jqwik 在每次属性 try 前会调用本方法，确保 mocks 互相独立、计数器重置。
     */
    @BeforeTry
    void setUp() {
        themePresetMapper = mock(ThemePresetMapper.class);
        themeUserSettingMapper = mock(ThemeUserSettingMapper.class);
        themeProfileValidator = new ThemeProfileValidator();
        themeService = new ThemeServiceImpl(
                themePresetMapper, themeUserSettingMapper, themeProfileValidator);
    }

    /**
     * 主属性：对随机预设 ID + 随机受保护操作（UPDATE / DELETE），均必须抛出
     * {@code THEME_BUILTIN_NOT_MUTABLE} 且数据库无任何写入。
     *
     * @param presetId  随机生成的预设 ID（含 {@code PRESET_BEST} 等语义 ID）
     * @param operation 随机操作类型（UPDATE / DELETE）
     */
    @Property(tries = 200)
    @Label("P_BUILTIN_IMMUTABLE: 内置预设的 PUT/DELETE 必抛 THEME_BUILTIN_NOT_MUTABLE 且数据库不变")
    void shouldRejectMutationsOnBuiltInPresets(
            @ForAll("presetIds") String presetId,
            @ForAll Operation operation) {

        // ── 1. 准备一个内置预设；保留快照用于「字段未变」断言 ──────────────
        ThemePreset builtIn = newBuiltInPreset(presetId);
        ThemePreset snapshot = cloneShallow(builtIn);

        // 任何 selectById(anyString()) 调用都返回该内置实体，以适配两条受保护路径
        when(themePresetMapper.selectById(anyString())).thenReturn(builtIn);

        // ── 2. 触发受保护操作并捕获异常 ─────────────────────────────────────
        ThemePresetUpsertDTO dto = newDummyDto();
        Throwable thrown;
        if (operation == Operation.UPDATE) {
            thrown = catchThrown(() -> themeService.updatePreset(presetId, dto));
        } else {
            thrown = catchThrown(() -> themeService.deletePreset(presetId));
        }

        // ── 3. 断言异常为 THEME_BUILTIN_NOT_MUTABLE ────────────────────────
        assertThat(thrown)
                .as("操作 %s on 预设 [%s] 应抛出 BusinessException", operation, presetId)
                .isInstanceOf(BusinessException.class);
        BusinessException be = (BusinessException) thrown;
        assertThat(be.getCode())
                .as("错误码应为 THEME_BUILTIN_NOT_MUTABLE")
                .isEqualTo(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE.getCode());
        assertThat(be.getMessage())
                .as("错误消息应为 ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE.message")
                .isEqualTo(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE.getMessage());

        // ── 4. 断言数据库未发生任何写操作 ──────────────────────────────────
        // 只允许 selectById 被调用恰好 1 次（用于加载并判定 isBuiltIn）；
        // 所有写 API 必须零调用。
        // 注：BaseMapper 的 updateById/insert 在新版本 mybatis-plus 中存在
        // 单实体与 Collection 两种重载，需通过 cast 显式消歧
        // （any(Class) 在编译期不足以让 javac 区分两个泛型重载）。
        verify(themePresetMapper, times(1)).selectById(anyString());
        verify(themePresetMapper, never()).updateById((ThemePreset) any());
        verify(themePresetMapper, never()).deleteById(any());
        verify(themePresetMapper, never()).insert((ThemePreset) any());
        verify(themeUserSettingMapper, never()).clearPresetRefByPresetId(anyString());
        verify(themeUserSettingMapper, never()).insert((ThemeUserSetting) any());
        verify(themeUserSettingMapper, never()).updateById((ThemeUserSetting) any());
        verify(themeUserSettingMapper, never()).deleteById(any());

        // ── 5. 断言内存实体的字段未被任何 setter 触碰 ──────────────────────
        assertThat(builtIn.getId()).isEqualTo(snapshot.getId());
        assertThat(builtIn.getName()).isEqualTo(snapshot.getName());
        assertThat(builtIn.getDescription()).isEqualTo(snapshot.getDescription());
        assertThat(builtIn.getIsBuiltIn()).isEqualTo(snapshot.getIsBuiltIn());
        assertThat(builtIn.getIsDefault()).isEqualTo(snapshot.getIsDefault());
        assertThat(builtIn.getSortOrder()).isEqualTo(snapshot.getSortOrder());
        assertThat(builtIn.getProfileAdmin()).isSameAs(snapshot.getProfileAdmin());
        assertThat(builtIn.getProfileBusiness()).isSameAs(snapshot.getProfileBusiness());
        assertThat(builtIn.getCreatedAt()).isEqualTo(snapshot.getCreatedAt());
        assertThat(builtIn.getUpdatedAt()).isEqualTo(snapshot.getUpdatedAt());
    }

    /**
     * 预设 ID 生成器：覆盖语义 ID 与任意字母数字串。
     *
     * @return 预设 ID 的 jqwik Arbitrary
     */
    @Provide
    Arbitrary<String> presetIds() {
        Arbitrary<String> semantic = Arbitraries.of(
                ThemeConstants.PRESET_BEST_ID,
                "PRESET_OCEAN",
                "PRESET_FOREST",
                "PRESET_SUNSET");
        Arbitrary<String> random = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(32);
        return Arbitraries.oneOf(semantic, random);
    }

    /**
     * 受保护操作枚举：覆盖修改与删除两条 {@code rejectIfBuiltIn} 触发路径。
     */
    private enum Operation {
        /** 调用 {@link ThemeServiceImpl#updatePreset(String, ThemePresetUpsertDTO)}。 */
        UPDATE,
        /** 调用 {@link ThemeServiceImpl#deletePreset(String)}。 */
        DELETE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 构造一个 {@code is_built_in=true} 的预设实体，字段值用于「未变」断言。
     *
     * @param id 预设 ID
     * @return 内置预设实体
     */
    private ThemePreset newBuiltInPreset(String id) {
        ThemeProfileJson profileAdmin = newSampleProfileJson("#165DFF");
        ThemeProfileJson profileBusiness = newSampleProfileJson("#3491FA");
        return new ThemePreset()
                .setId(id)
                .setName("最佳实践（内置）")
                .setDescription("系统内置默认预设，不可修改或删除")
                .setIsBuiltIn(true)
                .setIsDefault(ThemeConstants.PRESET_BEST_ID.equals(id))
                .setSortOrder(0)
                .setProfileAdmin(profileAdmin)
                .setProfileBusiness(profileBusiness)
                .setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    }

    /**
     * 构造一个 {@link ThemeProfileJson} 样本，用于内置预设的两个变体。
     *
     * @param primaryColor 主色
     * @return Profile JSON 载体
     */
    private ThemeProfileJson newSampleProfileJson(String primaryColor) {
        return new ThemeProfileJson()
                .setPrimaryColor(primaryColor)
                .setSecondaryColor("#4080FF")
                .setAccentColor("#7FBA00")
                .setSuccessColor("#00B42A")
                .setWarningColor("#FF7D00")
                .setErrorColor("#F53F3F")
                .setInfoColor("#3491FA")
                .setTextColor("#1D2129")
                .setBackgroundColor("#FFFFFF")
                .setBorderColor("#E5E6EB")
                .setMode(ThemeMode.LIGHT)
                .setRadius(RadiusStyle.MEDIUM)
                .setDensity(Density.DEFAULT)
                .setSidebarStyle(SidebarStyle.LIGHT)
                .setSidebarCollapsedStyle(SidebarStyle.LIGHT)
                .setHeaderStyle(HeaderStyle.LIGHT)
                .setFontScale(FontScale.DEFAULT)
                .setPresetRef(null);
    }

    /**
     * 浅克隆：仅复制基本字段引用，用于断言原实体的引用 / 标量字段未被改写。
     * <p>
     * {@link ThemeProfileJson} 字段使用引用相等（{@code isSameAs}）断言，因此
     * 浅克隆已足够发现「替换为新对象」的修改；如果实现内部直接修改 JSON 内部字段，
     * 这里同样可由 {@link ThemeServiceImpl#updatePreset} 的 setter 替换检测到。
     * </p>
     *
     * @param src 源实体
     * @return 字段对齐的克隆体
     */
    private ThemePreset cloneShallow(ThemePreset src) {
        return new ThemePreset()
                .setId(src.getId())
                .setName(src.getName())
                .setDescription(src.getDescription())
                .setIsBuiltIn(src.getIsBuiltIn())
                .setIsDefault(src.getIsDefault())
                .setSortOrder(src.getSortOrder())
                .setProfileAdmin(src.getProfileAdmin())
                .setProfileBusiness(src.getProfileBusiness())
                .setCreatedAt(src.getCreatedAt())
                .setUpdatedAt(src.getUpdatedAt());
    }

    /**
     * 构造一个内容随意的 {@link ThemePresetUpsertDTO}（不会被实际使用）。
     * <p>
     * 由于 {@code rejectIfBuiltIn} 会在校验之前抛出异常，因此 DTO 字段
     * 既不会被 {@link ThemeProfileValidator} 校验，也不会被持久化。
     * 这里仍构造合法值以保持「逻辑上是合法请求」的语义清晰。
     * </p>
     *
     * @return 一个合法但永远不会被使用的 DTO
     */
    private ThemePresetUpsertDTO newDummyDto() {
        ThemeProfileDTO admin = new ThemeProfileDTO();
        admin.setPrimaryColor("#165DFF");
        admin.setSecondaryColor("#4080FF");
        admin.setAccentColor("#7FBA00");
        admin.setSuccessColor("#00B42A");
        admin.setWarningColor("#FF7D00");
        admin.setErrorColor("#F53F3F");
        admin.setInfoColor("#3491FA");
        admin.setTextColor("#1D2129");
        admin.setBackgroundColor("#FFFFFF");
        admin.setBorderColor("#E5E6EB");
        admin.setMode(ThemeMode.LIGHT);
        admin.setRadius(RadiusStyle.MEDIUM);
        admin.setDensity(Density.DEFAULT);
        admin.setSidebarStyle(SidebarStyle.LIGHT);
        admin.setSidebarCollapsedStyle(SidebarStyle.LIGHT);
        admin.setHeaderStyle(HeaderStyle.LIGHT);
        admin.setFontScale(FontScale.DEFAULT);

        ThemeProfileDTO business = new ThemeProfileDTO();
        business.setPrimaryColor("#3491FA");
        business.setSecondaryColor("#4080FF");
        business.setAccentColor("#7FBA00");
        business.setSuccessColor("#00B42A");
        business.setWarningColor("#FF7D00");
        business.setErrorColor("#F53F3F");
        business.setInfoColor("#3491FA");
        business.setTextColor("#1D2129");
        business.setBackgroundColor("#FFFFFF");
        business.setBorderColor("#E5E6EB");
        business.setMode(ThemeMode.LIGHT);
        business.setRadius(RadiusStyle.MEDIUM);
        business.setDensity(Density.DEFAULT);
        business.setSidebarStyle(SidebarStyle.LIGHT);
        business.setSidebarCollapsedStyle(SidebarStyle.LIGHT);
        business.setHeaderStyle(HeaderStyle.LIGHT);
        business.setFontScale(FontScale.DEFAULT);

        return new ThemePresetUpsertDTO()
                .setName("dummy")
                .setDescription("never-applied")
                .setSortOrder(100)
                .setProfileAdmin(admin)
                .setProfileBusiness(business);
    }

    /**
     * 捕获 lambda 抛出的异常（对应 AssertJ {@code catchThrowable} 的轻量包装）。
     *
     * @param runnable 待执行的操作
     * @return 抛出的 throwable，未抛出时返回 {@code null}
     */
    private Throwable catchThrown(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
