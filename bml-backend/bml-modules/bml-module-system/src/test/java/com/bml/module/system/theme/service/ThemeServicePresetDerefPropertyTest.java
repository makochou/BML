package com.bml.module.system.theme.service;

import com.bml.module.system.theme.entity.ThemePreset;
import com.bml.module.system.theme.entity.ThemeProfileJson;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
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
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.InOrder;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ThemeServiceImpl} 预设删除解引用属性测试（基于 jqwik + Mockito）。
 *
 * <h3>Property 5 — 预设删除解引用（{@code P_PRESET_DEREF}）</h3>
 * <p>
 * <b>Validates: Requirements 12.4</b>
 * </p>
 *
 * <h3>属性语义</h3>
 * <p>
 * 删除自定义预设 {@code pid} 时，{@link ThemeServiceImpl#deletePreset(String)} 必须满足：
 * </p>
 * <ol>
 *   <li>先调用 {@link ThemeUserSettingMapper#clearPresetRefByPresetId(String)}
 *       将所有引用该预设的用户设置 {@code preset_ref} 置为 {@code NULL}
 *       （保留 {@code profile} 字段值不变）；</li>
 *   <li>再调用 {@link ThemePresetMapper#deleteById(java.io.Serializable)}
 *       删除预设本身；</li>
 *   <li>两步操作的顺序不可颠倒（先解引用再删除，避免外键约束或数据不一致）；</li>
 *   <li>不应对 {@link ThemeUserSettingMapper} 执行任何
 *       {@code updateById} / {@code insert} 操作（即用户 profile 保持原值不变）。</li>
 * </ol>
 *
 * <h3>jqwik 输入空间</h3>
 * <ul>
 *   <li>{@code presetId} —— 由 {@link #presetIds()} 提供：长度 1..32 的字母数字串；</li>
 *   <li>{@code affectedRows} —— 1..10 的整数，模拟解引用影响的行数。</li>
 * </ul>
 *
 * @author BML Team
 */
class ThemeServicePresetDerefPropertyTest {

    /** 主题预设 mapper（mock）。 */
    private ThemePresetMapper themePresetMapper;

    /** 用户主题设置 mapper（mock）。 */
    private ThemeUserSettingMapper themeUserSettingMapper;

    /** 真实校验器：deletePreset 路径下不会被触及，仅作为依赖注入。 */
    private ThemeProfileValidator themeProfileValidator;

    /** 被测对象：服务实现。 */
    private ThemeServiceImpl themeService;

    /**
     * jqwik 在每次属性 try 前调用，确保 mocks 互相独立、计数器重置。
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
     * 主属性：删除自定义预设时，必须先解引用再删除，且不触碰用户 profile。
     *
     * <p><b>Validates: Requirements 12.4</b></p>
     *
     * @param presetId     随机生成的预设 ID（长度 1..32）
     * @param affectedRows 模拟 clearPresetRefByPresetId 返回的受影响行数（1..10）
     */
    @Property(tries = 100)
    @Label("P_PRESET_DEREF: 删除自定义预设时先解引用再删除，用户 profile 保持不变")
    void shouldDerefBeforeDeleteAndPreserveProfile(
            @ForAll("presetIds") String presetId,
            @ForAll @IntRange(min = 1, max = 10) int affectedRows) {

        // ── 1. 准备一个自定义预设（isBuiltIn=false） ─────────────────────────
        ThemePreset customPreset = newCustomPreset(presetId);
        when(themePresetMapper.selectById(presetId)).thenReturn(customPreset);

        // ── 2. Stub clearPresetRefByPresetId 返回受影响行数 ──────────────────
        when(themeUserSettingMapper.clearPresetRefByPresetId(presetId)).thenReturn(affectedRows);

        // ── 3. 执行删除 ─────────────────────────────────────────────────────
        themeService.deletePreset(presetId);

        // ── 4. 验证调用顺序：clearPresetRefByPresetId 必须在 deleteById 之前 ──
        InOrder inOrder = inOrder(themeUserSettingMapper, themePresetMapper);
        inOrder.verify(themeUserSettingMapper).clearPresetRefByPresetId(presetId);
        inOrder.verify(themePresetMapper).deleteById(presetId);

        // ── 5. 验证不对 ThemeUserSettingMapper 执行 updateById / insert ──────
        //    （用户 profile 保持原值不变）
        verify(themeUserSettingMapper, never()).updateById((ThemeUserSetting) any());
        verify(themeUserSettingMapper, never()).insert((ThemeUserSetting) any());
    }

    /**
     * 预设 ID 生成器：长度 1..32 的字母数字串。
     *
     * @return 预设 ID 的 jqwik Arbitrary
     */
    @Provide
    Arbitrary<String> presetIds() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(32);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 构造一个 {@code is_built_in=false} 的自定义预设实体。
     *
     * @param id 预设 ID
     * @return 自定义预设实体
     */
    private ThemePreset newCustomPreset(String id) {
        ThemeProfileJson profileAdmin = newSampleProfileJson("#FF5722");
        ThemeProfileJson profileBusiness = newSampleProfileJson("#2196F3");
        return new ThemePreset()
                .setId(id)
                .setName("自定义预设-" + id)
                .setDescription("用于属性测试的自定义预设")
                .setIsBuiltIn(false)
                .setIsDefault(false)
                .setSortOrder(10)
                .setProfileAdmin(profileAdmin)
                .setProfileBusiness(profileBusiness)
                .setCreatedAt(LocalDateTime.of(2025, 6, 1, 12, 0, 0))
                .setUpdatedAt(LocalDateTime.of(2025, 6, 1, 12, 0, 0));
    }

    /**
     * 构造一个 {@link ThemeProfileJson} 样本。
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
}
