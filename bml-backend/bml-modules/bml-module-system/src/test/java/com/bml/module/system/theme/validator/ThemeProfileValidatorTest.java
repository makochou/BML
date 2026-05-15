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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ThemeProfileValidator} 单元测试。
 * <p>
 * 主要验证「全量字段收集」语义：所有非法字段在一次校验中累积返回，
 * 而不会因首字段失败提前抛出（对应设计文档 Property 6 — {@code P_VALIDATE_ALL}）。
 * </p>
 */
class ThemeProfileValidatorTest {

    private final ThemeProfileValidator validator = new ThemeProfileValidator();

    /**
     * 构造一个全字段合法的基线 {@link ThemeProfileDTO}，用于在测试中按需"打破"个别字段。
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

    @Test
    @DisplayName("全字段合法时不抛异常")
    void shouldPassForValidProfile() {
        assertThatCode(() -> validator.validate(buildValidDto())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("入参为 null 时直接返回不抛异常")
    void shouldReturnSilentlyWhenNull() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("单个非法颜色应被收集并抛出 THEME_INVALID_PROFILE")
    void shouldCollectSingleInvalidColor() {
        ThemeProfileDTO dto = buildValidDto();
        dto.setPrimaryColor("not-a-color");

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(ThemeErrorCode.THEME_INVALID_PROFILE.getCode());
                    @SuppressWarnings("unchecked")
                    List<FieldError> errors = (List<FieldError>) be.getData();
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getField()).isEqualTo("primaryColor");
                    assertThat(errors.get(0).getCode()).isEqualTo("INVALID_HEX_COLOR");
                });
    }

    @Test
    @DisplayName("多字段非法时应一次性返回全部错误（全量字段收集）")
    void shouldCollectAllInvalidFields() {
        ThemeProfileDTO dto = buildValidDto();
        // 注入 5 个非法字段：3 个颜色非法 + 1 个枚举缺失 + 折叠态非法
        dto.setPrimaryColor("xyz");                     // 非法颜色
        dto.setBackgroundColor(null);                   // 必填缺失
        dto.setBorderColor("#GGGGGG");                  // 非法十六进制
        dto.setMode(null);                              // 枚举缺失
        dto.setSidebarCollapsedStyle(SidebarStyle.PRIMARY); // 折叠态业务规则违规

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    @SuppressWarnings("unchecked")
                    List<FieldError> errors = (List<FieldError>) be.getData();

                    // 5 条错误必须全部呈现，证明非首字段失败即返回
                    assertThat(errors).hasSize(5);
                    assertThat(errors).extracting(FieldError::getField)
                            .containsExactlyInAnyOrder(
                                    "primaryColor",
                                    "backgroundColor",
                                    "borderColor",
                                    "mode",
                                    "sidebarCollapsedStyle");

                    // 校验各错误码语义
                    FieldError primary = errors.stream()
                            .filter(e -> "primaryColor".equals(e.getField()))
                            .findFirst().orElseThrow();
                    assertThat(primary.getCode()).isEqualTo("INVALID_HEX_COLOR");

                    FieldError background = errors.stream()
                            .filter(e -> "backgroundColor".equals(e.getField()))
                            .findFirst().orElseThrow();
                    assertThat(background.getCode()).isEqualTo("REQUIRED");

                    FieldError mode = errors.stream()
                            .filter(e -> "mode".equals(e.getField()))
                            .findFirst().orElseThrow();
                    assertThat(mode.getCode()).isEqualTo("REQUIRED");

                    FieldError collapsed = errors.stream()
                            .filter(e -> "sidebarCollapsedStyle".equals(e.getField()))
                            .findFirst().orElseThrow();
                    assertThat(collapsed.getCode()).isEqualTo("INVALID_SIDEBAR_COLLAPSED");
                });
    }

    @Test
    @DisplayName("全部 17 个字段缺失时应返回 17 条 REQUIRED 错误")
    void shouldCollectRequiredErrorsForAllNullFields() {
        ThemeProfileDTO dto = new ThemeProfileDTO(); // 全部字段为 null

        assertThatThrownBy(() -> validator.validate(dto))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    @SuppressWarnings("unchecked")
                    List<FieldError> errors = (List<FieldError>) be.getData();
                    // 10 个颜色 + 7 个枚举 = 17 个 REQUIRED 错误
                    assertThat(errors).hasSize(17);
                    assertThat(errors).allMatch(e -> "REQUIRED".equals(e.getCode()));
                });
    }

    @Test
    @DisplayName("折叠态合法值（LIGHT/DARK）不应触发业务规则错误")
    void shouldAcceptLightOrDarkForCollapsedSidebar() {
        ThemeProfileDTO light = buildValidDto();
        light.setSidebarCollapsedStyle(SidebarStyle.LIGHT);
        assertThatCode(() -> validator.validate(light)).doesNotThrowAnyException();

        ThemeProfileDTO dark = buildValidDto();
        dark.setSidebarCollapsedStyle(SidebarStyle.DARK);
        assertThatCode(() -> validator.validate(dark)).doesNotThrowAnyException();
    }
}
