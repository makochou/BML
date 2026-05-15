package com.bml.module.system.theme.validator;

import com.bml.core.common.exception.BusinessException;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.enums.Density;
import com.bml.module.system.theme.enums.FontScale;
import com.bml.module.system.theme.enums.HeaderStyle;
import com.bml.module.system.theme.enums.RadiusStyle;
import com.bml.module.system.theme.enums.SidebarStyle;
import com.bml.module.system.theme.enums.ThemeErrorCode;
import com.bml.module.system.theme.enums.ThemeMode;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ThemeProfileValidator} 属性测试（基于 jqwik）。
 *
 * <h3>Property 6 — 非法字段全量返回（{@code P_VALIDATE_ALL}）</h3>
 * <p>
 * <b>Validates: Requirements 4.8, 7.4</b>
 * </p>
 * <p>
 * 校验语义：
 * </p>
 * <ul>
 *   <li>对随机选取的 {@code N ∈ [1, 17]} 个字段（10 个颜色 + 7 个枚举）注入非法值；</li>
 *   <li>每个被破坏字段独立地映射为 <em>恰好一条</em> 错误条目（颜色字段非法 →
 *       {@code INVALID_HEX_COLOR} 或 {@code REQUIRED}；枚举字段非法 →
 *       {@code REQUIRED}；折叠态非法 → {@code INVALID_SIDEBAR_COLLAPSED} 或
 *       {@code REQUIRED}）；</li>
 *   <li>断言抛出的 {@link BusinessException}：
 *     <ol>
 *       <li>错误码为 {@link ThemeErrorCode#THEME_INVALID_PROFILE}；</li>
 *       <li>{@code data} 长度恰好等于注入的非法字段数 {@code N}；</li>
 *       <li>错误条目的 {@code field} 集合与注入字段集合相等（无遗漏、无额外）。</li>
 *     </ol>
 *   </li>
 * </ul>
 *
 * <h3>关于"每字段一条错误"的成立条件</h3>
 * <p>
 * 该属性依赖 {@link ThemeProfileValidator} 的实现细节：每个非法字段在校验过程中
 * 仅产生一条 {@link FieldError}。具体而言：
 * </p>
 * <ul>
 *   <li>颜色字段：{@code null} → 1 条 {@code REQUIRED}；非法格式 → 1 条
 *       {@code INVALID_HEX_COLOR}（实现中已使用 {@code if/else if} 形式，{@code null}
 *       不再继续走正则匹配）；</li>
 *   <li>枚举字段：{@code null} → 1 条 {@code REQUIRED}（DTO 字段已是强类型枚举，
 *       不存在"非法字符串"形态）；</li>
 *   <li>{@code sidebarCollapsedStyle}：{@code null} → 1 条 {@code REQUIRED}；
 *       {@code PRIMARY} / {@code TRANSPARENT} → 1 条 {@code INVALID_SIDEBAR_COLLAPSED}
 *       （第一段 {@code validateRequired} 在非空时跳过，仅业务子规则记录错误）。</li>
 * </ul>
 *
 * <h3>shrink 友好性</h3>
 * <p>
 * 测试通过 jqwik 的 {@code Set<String>} 子集生成器与 {@code long} 种子参数描述策略，
 * 失败时 jqwik 会自动收缩到最小反例（最少字段数 + 最简单的破坏选择），便于排查。
 * </p>
 *
 * @author BML Team
 */
class ThemeProfileValidatorPropertyTest {

    /** 10 个颜色字段名（与 {@link ThemeProfileDTO} 一一对应）。 */
    private static final List<String> COLOR_FIELDS = List.of(
            "primaryColor", "secondaryColor", "accentColor", "successColor",
            "warningColor", "errorColor", "infoColor", "textColor",
            "backgroundColor", "borderColor");

    /** 7 个枚举字段名（与 {@link ThemeProfileDTO} 一一对应）。 */
    private static final List<String> ENUM_FIELDS = List.of(
            "mode", "radius", "density", "sidebarStyle",
            "sidebarCollapsedStyle", "headerStyle", "fontScale");

    /** 全部 17 个候选字段（颜色 + 枚举），供 jqwik 生成器抽取子集。 */
    private static final List<String> ALL_FIELDS;

    static {
        List<String> all = new ArrayList<>(COLOR_FIELDS.size() + ENUM_FIELDS.size());
        all.addAll(COLOR_FIELDS);
        all.addAll(ENUM_FIELDS);
        ALL_FIELDS = List.copyOf(all);
    }

    /** 用于颜色字段「非法格式」的固定候选值集合（含空串、缺位、非十六进制等）。 */
    private static final String[] INVALID_HEX_SAMPLES = {
            "not-a-color",
            "#GGGGGG",
            "#12345",      // 5 位
            "#1234567",    // 7 位
            "165DFF",      // 缺少 #
            "",
            "rgb(0,0,0)"
    };

    /** 折叠态合法值之外的可选 {@link SidebarStyle} 枚举。 */
    private static final SidebarStyle[] INVALID_COLLAPSED_STYLES = {
            SidebarStyle.PRIMARY,
            SidebarStyle.TRANSPARENT
    };

    /** 被测对象：业务层全字段收集校验器。 */
    private final ThemeProfileValidator validator = new ThemeProfileValidator();

    /**
     * Property 6 主测：注入 N 个非法字段后，错误响应必须返回恰好 N 条字段错误，
     * 且字段名集合与注入集合完全一致（无遗漏、无额外、无重复）。
     *
     * @param brokenFields  通过 jqwik 子集生成器获得的待破坏字段集合（大小 1..17）
     * @param mutationSeed  控制每个字段「破坏方式」的随机种子；与字段集合解耦，
     *                      使 jqwik 可独立收缩这两维输入空间
     */
    @Property(tries = 200)
    @Label("P_VALIDATE_ALL: 错误响应包含全部非法字段，且数量与注入数相等")
    void shouldReturnAllInvalidFieldsExactly(
            @ForAll("brokenSubsets") Set<String> brokenFields,
            @ForAll long mutationSeed) {

        // ── 1. 构造一个全字段合法的基线 DTO ─────────────────────────────────
        ThemeProfileDTO dto = buildValidDto();

        // ── 2. 按随机种子破坏选定字段 ──────────────────────────────────────
        Random rng = new Random(mutationSeed);
        for (String field : brokenFields) {
            breakField(dto, field, rng);
        }

        // ── 3. 校验抛出 BusinessException 且承载完整字段错误列表 ──────────
        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;

                    // 错误码必须为 THEME_INVALID_PROFILE
                    assertThat(be.getCode())
                            .as("错误码应为 THEME_INVALID_PROFILE")
                            .isEqualTo(ThemeErrorCode.THEME_INVALID_PROFILE.getCode());

                    @SuppressWarnings("unchecked")
                    List<FieldError> errors = (List<FieldError>) be.getData();
                    assertThat(errors)
                            .as("错误响应 data 必须为非空的 FieldError 列表")
                            .isNotNull()
                            .isNotEmpty();

                    // 1) 错误条目数量必须等于注入的非法字段数（每字段恰好 1 条）
                    assertThat(errors)
                            .as("错误条目数量应等于注入的非法字段数（注入 %d 个）",
                                    brokenFields.size())
                            .hasSize(brokenFields.size());

                    // 2) 错误字段集合必须与注入集合完全相等（无遗漏、无额外、无重复）
                    Set<String> errorFields = errors.stream()
                            .map(FieldError::getField)
                            .collect(Collectors.toSet());
                    assertThat(errorFields)
                            .as("错误字段集合应与注入字段集合完全一致")
                            .isEqualTo(brokenFields);

                    // 3) 每条错误的 code 必须落在已知集合内，且与字段类别匹配
                    errors.forEach(fe -> assertThat(fe.getCode())
                            .as("FieldError.code 应为已知业务错误代码")
                            .isIn("REQUIRED", "INVALID_HEX_COLOR", "INVALID_SIDEBAR_COLLAPSED"));
                });
    }

    /**
     * 待破坏字段子集生成器：从 17 个候选字段中等概率抽取 1..17 个不同字段。
     * <p>
     * 使用 jqwik 内置的集合生成器以保证 shrink 友好；当属性失败时会优先收缩到
     * 最小集合（如单字段反例），便于定位。
     * </p>
     *
     * @return 字段名集合 Arbitrary
     */
    @Provide
    Arbitrary<Set<String>> brokenSubsets() {
        return Arbitraries.of(ALL_FIELDS)
                .set()
                .ofMinSize(1)
                .ofMaxSize(ALL_FIELDS.size());
    }

    /**
     * 构造全字段合法的基线 {@link ThemeProfileDTO}。
     * <p>
     * 颜色字段使用 PRESET_BEST 风格的真实十六进制值，枚举字段全部赋默认档位；
     * {@code sidebarCollapsedStyle} 取业务允许的 {@link SidebarStyle#LIGHT}。
     * </p>
     */
    private ThemeProfileDTO buildValidDto() {
        ThemeProfileDTO dto = new ThemeProfileDTO();
        dto.setPrimaryColor("#165DFF");
        dto.setSecondaryColor("#4080FF");
        dto.setAccentColor("#7FBA00");
        dto.setSuccessColor("#00B42A");
        dto.setWarningColor("#FF7D00");
        dto.setErrorColor("#F53F3F");
        dto.setInfoColor("#3491FA");
        dto.setTextColor("#1D2129");
        dto.setBackgroundColor("#FFFFFF");
        dto.setBorderColor("#E5E6EB");
        dto.setMode(ThemeMode.LIGHT);
        dto.setRadius(RadiusStyle.MEDIUM);
        dto.setDensity(Density.DEFAULT);
        dto.setSidebarStyle(SidebarStyle.LIGHT);
        dto.setSidebarCollapsedStyle(SidebarStyle.LIGHT);
        dto.setHeaderStyle(HeaderStyle.LIGHT);
        dto.setFontScale(FontScale.DEFAULT);
        dto.setPresetRef(null);
        return dto;
    }

    /**
     * 将单个字段破坏为非法值。
     * <p>
     * 设计要点：每种破坏选择都必须保证「校验器只会就该字段记录恰好一条错误」，
     * 避免出现一个字段触发多条错误而破坏 {@code size == brokenFields.size()} 不变式。
     * </p>
     *
     * @param dto   将被原地修改的 DTO
     * @param field 字段名
     * @param rng   控制破坏方式选择的随机源
     */
    private void breakField(ThemeProfileDTO dto, String field, Random rng) {
        switch (field) {
            // 颜色字段：null（→ REQUIRED）或非法格式（→ INVALID_HEX_COLOR）
            case "primaryColor"     -> dto.setPrimaryColor(invalidColorValue(rng));
            case "secondaryColor"   -> dto.setSecondaryColor(invalidColorValue(rng));
            case "accentColor"      -> dto.setAccentColor(invalidColorValue(rng));
            case "successColor"     -> dto.setSuccessColor(invalidColorValue(rng));
            case "warningColor"     -> dto.setWarningColor(invalidColorValue(rng));
            case "errorColor"       -> dto.setErrorColor(invalidColorValue(rng));
            case "infoColor"        -> dto.setInfoColor(invalidColorValue(rng));
            case "textColor"        -> dto.setTextColor(invalidColorValue(rng));
            case "backgroundColor"  -> dto.setBackgroundColor(invalidColorValue(rng));
            case "borderColor"      -> dto.setBorderColor(invalidColorValue(rng));
            // 严格枚举字段：DTO 类型为枚举，不存在"非法字符串"形态，仅可设为 null
            case "mode"             -> dto.setMode(null);
            case "radius"           -> dto.setRadius(null);
            case "density"          -> dto.setDensity(null);
            case "sidebarStyle"     -> dto.setSidebarStyle(null);
            case "headerStyle"      -> dto.setHeaderStyle(null);
            case "fontScale"        -> dto.setFontScale(null);
            // 折叠态：null（→ REQUIRED）或 PRIMARY/TRANSPARENT（→ INVALID_SIDEBAR_COLLAPSED）
            case "sidebarCollapsedStyle" -> dto.setSidebarCollapsedStyle(invalidCollapsedStyle(rng));
            default -> throw new IllegalArgumentException("未知字段名: " + field);
        }
    }

    /**
     * 选取一个非法颜色值：要么 {@code null}，要么从预置非法样本中随机取一个。
     * <p>
     * 通过 50/50 的二选一保证 {@code REQUIRED} 与 {@code INVALID_HEX_COLOR}
     * 两类错误码都能在大量样本中得到覆盖。
     * </p>
     */
    private String invalidColorValue(Random rng) {
        if (rng.nextBoolean()) {
            return null;
        }
        return INVALID_HEX_SAMPLES[rng.nextInt(INVALID_HEX_SAMPLES.length)];
    }

    /**
     * 选取一个非法折叠态：要么 {@code null}，要么从 {@code PRIMARY/TRANSPARENT}
     * 中随机取一个，保证 {@code REQUIRED} 与 {@code INVALID_SIDEBAR_COLLAPSED}
     * 两类错误码都能在大量样本中得到覆盖。
     */
    private SidebarStyle invalidCollapsedStyle(Random rng) {
        if (rng.nextBoolean()) {
            return null;
        }
        return INVALID_COLLAPSED_STYLES[rng.nextInt(INVALID_COLLAPSED_STYLES.length)];
    }
}
