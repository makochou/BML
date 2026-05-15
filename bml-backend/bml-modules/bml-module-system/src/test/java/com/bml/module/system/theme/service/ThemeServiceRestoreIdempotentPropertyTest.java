package com.bml.module.system.theme.service;

import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.theme.constant.ThemeConstants;
import com.bml.module.system.theme.entity.ThemePreset;
import com.bml.module.system.theme.entity.ThemeProfileJson;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeMode;
import com.bml.module.system.theme.enums.ThemeScope;
import com.bml.module.system.theme.mapper.ThemePresetMapper;
import com.bml.module.system.theme.mapper.ThemeUserSettingMapper;
import com.bml.module.system.theme.service.impl.ThemeServiceImpl;
import com.bml.module.system.theme.validator.ThemeProfileValidator;
import com.bml.module.system.theme.vo.ThemeProfileVO;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterTry;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ThemeServiceImpl#restoreToBest(ThemeScope)} 幂等性属性测试（基于 jqwik + Mockito）。
 *
 * <h3>Property 2 — Restore 幂等（{@code P_RESTORE_IDEMPOTENT}）</h3>
 * <p>
 * <b>Validates: Requirements 3.2, 3.6</b>
 * </p>
 *
 * <h3>属性语义</h3>
 * <p>
 * 连续调用 {@code restoreToBest(scope)} N 次（N ∈ [1, 20]），最终返回的
 * {@link ThemeProfileVO} 与第一次调用的结果逐字段相等，且所有返回结果的
 * {@code primaryColor} 等于 {@code PRESET_BEST} 对应作用域变体的主色，
 * {@code presetRef} 始终为 {@code "PRESET_BEST"}。
 * </p>
 *
 * <h3>测试策略</h3>
 * <ul>
 *   <li>使用 jqwik 随机生成 {@link ThemeScope}（{@code ADMIN} / {@code BUSINESS}）
 *       与调用次数 {@code n}（1..20）；</li>
 *   <li>Mock {@link ThemePresetMapper#selectByDefault()} 返回固定的 PRESET_BEST 实体；</li>
 *   <li>Mock {@link ThemeUserSettingMapper#selectByUserAndScope(Long, String)}：
 *       首次调用返回 {@code null}（触发 insert 路径），后续调用返回已插入的实体
 *       （触发 update 路径），模拟真实数据库行为；</li>
 *   <li>Mock {@link SecurityUtils#getUserId()} 返回固定用户 ID；</li>
 *   <li>循环调用 N 次后断言幂等性。</li>
 * </ul>
 *
 * @author BML Team
 */
class ThemeServiceRestoreIdempotentPropertyTest {

    /** 测试中固定使用的当前用户 ID。 */
    private static final Long MOCK_USER_ID = 99L;

    /** PRESET_BEST ADMIN 变体主色。 */
    private static final String PRESET_BEST_ADMIN_PRIMARY = "#165DFF";

    /** PRESET_BEST BUSINESS 变体主色。 */
    private static final String PRESET_BEST_BUSINESS_PRIMARY = "#3491FA";

    /** 主题预设 mapper（mock）。 */
    private ThemePresetMapper themePresetMapper;

    /** 用户主题设置 mapper（mock）。 */
    private ThemeUserSettingMapper themeUserSettingMapper;

    /** 真实校验器（restoreToBest 路径不触发校验，仅作为依赖注入）。 */
    private ThemeProfileValidator themeProfileValidator;

    /** 被测对象：服务实现。 */
    private ThemeServiceImpl themeService;

    /** SecurityUtils 静态桩 —— 用于模拟当前登录用户。 */
    private MockedStatic<SecurityUtils> securityUtilsStatic;

    /**
     * 每次属性 try 前创建全新 mock 与服务实例，确保各 try 之间完全隔离。
     */
    @BeforeTry
    void setUp() {
        themePresetMapper = mock(ThemePresetMapper.class);
        themeUserSettingMapper = mock(ThemeUserSettingMapper.class);
        themeProfileValidator = new ThemeProfileValidator();
        themeService = new ThemeServiceImpl(
                themePresetMapper, themeUserSettingMapper, themeProfileValidator);

        // 模拟 SecurityUtils.getUserId() 静态方法
        securityUtilsStatic = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsStatic.when(SecurityUtils::getUserId).thenReturn(MOCK_USER_ID);
    }

    /**
     * 每次属性 try 后关闭静态桩，避免线程泄漏。
     */
    @AfterTry
    void tearDown() {
        if (securityUtilsStatic != null) {
            securityUtilsStatic.close();
        }
    }

    /**
     * 主属性：对随机作用域与随机调用次数 N，连续调用 {@code restoreToBest(scope)} N 次，
     * 断言每次返回的 {@link ThemeProfileVO} 均与首次结果逐字段相等，且等于 PRESET_BEST 对应变体。
     *
     * @param scope 随机生成的作用域（ADMIN / BUSINESS）
     * @param n     随机生成的调用次数（1..20）
     */
    @Property(tries = 100)
    @Label("P_RESTORE_IDEMPOTENT: restoreToBest(scope) 连续调用 N 次结果与单次调用相等且等于 PRESET_BEST 变体")
    void shouldBeIdempotent(
            @ForAll("scopes") ThemeScope scope,
            @ForAll("callCounts") int n) {

        // ── 1. 准备 PRESET_BEST 实体 ────────────────────────────────────────
        ThemePreset presetBest = buildPresetBest();
        when(themePresetMapper.selectByDefault()).thenReturn(presetBest);

        // ── 2. 模拟 selectByUserAndScope：首次返回 null，后续返回已存在实体 ──
        // 使用 AtomicReference 追踪 insert 后的实体状态
        AtomicReference<ThemeUserSetting> savedEntity = new AtomicReference<>(null);

        when(themeUserSettingMapper.selectByUserAndScope(anyLong(), anyString()))
                .thenAnswer(invocation -> savedEntity.get());

        // mock insert：将实体保存到 AtomicReference，模拟数据库插入
        doAnswer(invocation -> {
            ThemeUserSetting entity = invocation.getArgument(0);
            entity.setId(1L);
            savedEntity.set(entity);
            return 1;
        }).when(themeUserSettingMapper).insert((ThemeUserSetting) any());

        // mock updateById：更新 AtomicReference 中的实体
        doAnswer(invocation -> {
            ThemeUserSetting entity = invocation.getArgument(0);
            savedEntity.set(entity);
            return 1;
        }).when(themeUserSettingMapper).updateById((ThemeUserSetting) any());

        // ── 3. 连续调用 N 次 restoreToBest ──────────────────────────────────
        ThemeProfileVO firstResult = null;
        ThemeProfileVO lastResult = null;

        for (int i = 0; i < n; i++) {
            ThemeProfileVO result = themeService.restoreToBest(scope);

            if (i == 0) {
                firstResult = result;
            }
            lastResult = result;

            // 每次调用结果的 primaryColor 必须等于 PRESET_BEST 对应变体
            String expectedPrimary = (scope == ThemeScope.ADMIN)
                    ? PRESET_BEST_ADMIN_PRIMARY
                    : PRESET_BEST_BUSINESS_PRIMARY;
            assertThat(result.getPrimaryColor())
                    .as("第 %d 次调用 restoreToBest(%s) 的 primaryColor 应等于 PRESET_BEST 变体", i + 1, scope)
                    .isEqualTo(expectedPrimary);

            // 每次调用结果的 presetRef 必须为 PRESET_BEST
            assertThat(result.getPresetRef())
                    .as("第 %d 次调用 restoreToBest(%s) 的 presetRef 应为 PRESET_BEST", i + 1, scope)
                    .isEqualTo(ThemeConstants.PRESET_BEST_ID);
        }

        // ── 4. 断言幂等：最终结果与首次结果逐字段相等 ────────────────────────
        assertThat(lastResult)
                .as("最终结果应与首次结果逐字段相等（幂等性）")
                .usingRecursiveComparison()
                .isEqualTo(firstResult);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // jqwik 生成器
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 作用域生成器：等概率生成 {@code ADMIN} 或 {@code BUSINESS}。
     *
     * @return 作用域的 jqwik Arbitrary
     */
    @Provide
    Arbitrary<ThemeScope> scopes() {
        return Arbitraries.of(ThemeScope.ADMIN, ThemeScope.BUSINESS);
    }

    /**
     * 调用次数生成器：生成 1 到 20 之间的整数。
     *
     * @return 调用次数的 jqwik Arbitrary
     */
    @Provide
    Arbitrary<Integer> callCounts() {
        return Arbitraries.integers().between(1, 20);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 构造 PRESET_BEST 实体，包含 ADMIN 与 BUSINESS 两个作用域变体。
     *
     * @return PRESET_BEST 预设实体
     */
    private ThemePreset buildPresetBest() {
        ThemeProfileJson adminProfile = buildProfileJson(PRESET_BEST_ADMIN_PRIMARY);
        ThemeProfileJson businessProfile = buildProfileJson(PRESET_BEST_BUSINESS_PRIMARY);

        return new ThemePreset()
                .setId(ThemeConstants.PRESET_BEST_ID)
                .setName("最佳实践")
                .setDescription("系统内置默认预设")
                .setIsBuiltIn(true)
                .setIsDefault(true)
                .setSortOrder(0)
                .setProfileAdmin(adminProfile)
                .setProfileBusiness(businessProfile)
                .setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                .setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    }

    /**
     * 构造一个完整的 {@link ThemeProfileJson}，以指定主色为基础。
     *
     * @param primaryColor 主色（{@code #RRGGBB}）
     * @return 完整的 Profile JSON 载体
     */
    private ThemeProfileJson buildProfileJson(String primaryColor) {
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
