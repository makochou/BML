package com.bml.module.system.theme.enums;

import com.bml.core.common.result.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ThemeErrorCode} 单元测试。
 *
 * <p>该测试用于锁定主题模块错误码的稳定性：</p>
 * <ul>
 *     <li>每个枚举项的 {@link ThemeErrorCode#getCode()} 必须等于设计文档
 *         (<em>design.md → ThemeErrorCode</em>) 约定的整数值；</li>
 *     <li>每个枚举项的 {@link ThemeErrorCode#getMessage()} 必须等于设计文档约定的中文消息，
 *         避免后续重构时无意修改对外契约；</li>
 *     <li>枚举必须实现统一错误码协议 {@link ErrorCode}，以便配合
 *         {@code BusinessException} 与 {@code Result<T>} 序列化。</li>
 * </ul>
 *
 * <p>对应任务：tasks.md → 2.3 编写 {@code ThemeErrorCodeTest} 单元测试；
 * 对应需求：Requirements 7.3、7.4。</p>
 *
 * @author BML Team
 */
@DisplayName("ThemeErrorCode 错误码契约测试")
class ThemeErrorCodeTest {

    @Test
    @DisplayName("THEME_INVALID_PROFILE 的 code 与 message 与设计文档一致")
    void themeInvalidProfile_shouldMatchSpec() {
        ThemeErrorCode errorCode = ThemeErrorCode.THEME_INVALID_PROFILE;

        assertThat(errorCode.getCode()).isEqualTo(40_010);
        assertThat(errorCode.getMessage()).isEqualTo("主题配置存在非法字段");
    }

    @Test
    @DisplayName("THEME_SCOPE_INVALID 的 code 与 message 与设计文档一致")
    void themeScopeInvalid_shouldMatchSpec() {
        ThemeErrorCode errorCode = ThemeErrorCode.THEME_SCOPE_INVALID;

        assertThat(errorCode.getCode()).isEqualTo(40_011);
        assertThat(errorCode.getMessage()).isEqualTo("非法作用域参数");
    }

    @Test
    @DisplayName("THEME_PRESET_NOT_FOUND 的 code 与 message 与设计文档一致")
    void themePresetNotFound_shouldMatchSpec() {
        ThemeErrorCode errorCode = ThemeErrorCode.THEME_PRESET_NOT_FOUND;

        assertThat(errorCode.getCode()).isEqualTo(40_410);
        assertThat(errorCode.getMessage()).isEqualTo("主题预设不存在");
    }

    @Test
    @DisplayName("THEME_BUILTIN_NOT_MUTABLE 的 code 与 message 与设计文档一致")
    void themeBuiltinNotMutable_shouldMatchSpec() {
        ThemeErrorCode errorCode = ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE;

        assertThat(errorCode.getCode()).isEqualTo(40_310);
        assertThat(errorCode.getMessage()).isEqualTo("系统内置预设不可修改或删除");
    }

    @Test
    @DisplayName("THEME_PERSIST_FAILED 的 code 与 message 与设计文档一致")
    void themePersistFailed_shouldMatchSpec() {
        ThemeErrorCode errorCode = ThemeErrorCode.THEME_PERSIST_FAILED;

        assertThat(errorCode.getCode()).isEqualTo(50_010);
        assertThat(errorCode.getMessage()).isEqualTo("主题持久化失败");
    }

    @Test
    @DisplayName("所有枚举项均实现 ErrorCode 接口契约")
    void allValues_shouldImplementErrorCodeContract() {
        ThemeErrorCode[] values = ThemeErrorCode.values();

        // 防止后续无意修改：枚举项数量应严格为 5 个
        assertThat(values).hasSize(5);

        for (ThemeErrorCode value : values) {
            // 接口契约：必须是 ErrorCode 实例
            assertThat(value)
                    .as("ThemeErrorCode.%s 必须实现 ErrorCode 接口", value.name())
                    .isInstanceOf(ErrorCode.class);

            // 接口契约：getMessage 不得为空
            assertThat(value.getMessage())
                    .as("ThemeErrorCode.%s 的 message 不得为空", value.name())
                    .isNotBlank();

            // 接口契约：getCode 落在设计文档约定的业务错误码区间
            assertThat(value.getCode())
                    .as("ThemeErrorCode.%s 的 code 必须落在设计文档约定的区间", value.name())
                    .isBetween(40_000, 59_999);
        }
    }

    @Test
    @DisplayName("不同枚举项之间的 code 必须互不相同")
    void allValues_shouldHaveDistinctCodes() {
        ThemeErrorCode[] values = ThemeErrorCode.values();

        long distinctCount = java.util.Arrays.stream(values)
                .mapToInt(ThemeErrorCode::getCode)
                .distinct()
                .count();

        assertThat(distinctCount)
                .as("所有 ThemeErrorCode 的 code 必须互不相同，避免覆盖既有错误码")
                .isEqualTo(values.length);
    }
}
