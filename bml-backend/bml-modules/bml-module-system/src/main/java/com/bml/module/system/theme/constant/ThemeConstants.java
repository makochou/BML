package com.bml.module.system.theme.constant;

/**
 * 主题模块常量集合。
 * <p>
 * 集中维护后端主题特性涉及的全部魔法值，包括：
 * <ul>
 *   <li>内置预设语义 ID（{@link #PRESET_BEST_ID}、{@link #PRESET_OCEAN_ID}）；</li>
 *   <li>前端 {@code localStorage} 键名规范（仅作为后端文档参考，确保前后端命名一致）；</li>
 *   <li>跨标签广播通道名（与前端 {@code BroadcastChannel} 同名）；</li>
 *   <li>颜色字段统一正则；</li>
 *   <li>主题管理权限标识。</li>
 * </ul>
 * </p>
 * <p>
 * 该类不可被实例化，亦不允许被继承；通过私有构造函数与 {@code final}
 * 修饰符强制以"常量持有者"语义对外暴露。
 * </p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * if (ThemeConstants.PRESET_BEST_ID.equals(presetId)) {
 *     throw new BusinessException(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE);
 * }
 * }</pre>
 *
 * @author BML Team
 */
public final class ThemeConstants {

    // ─────────────────────────────────────────────────────────────────────────
    // 内置预设语义 ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 系统默认预设 ID（{@code PRESET_BEST}）。
     * <p>
     * 对应数据表 {@code bml_theme_preset} 中 {@code is_default=1} 的记录，
     * 作为「首次访问 / 未登录 / 配置缺失 / 恢复默认」动作的统一目标。
     * 该 ID 在 SQL 种子数据、后端 Service 与前端 TypeScript 中保持完全一致。
     * </p>
     */
    public static final String PRESET_BEST_ID = "PRESET_BEST";

    /**
     * 海洋蓝备选内置预设 ID（{@code PRESET_OCEAN}）。
     * <p>
     * 主要用于测试与文档示例，证明系统支持 {@code PRESET_BEST} 之外的
     * 其它内置预设；同样以 {@code is_built_in=1} 写入数据库。
     * </p>
     */
    public static final String PRESET_OCEAN_ID = "PRESET_OCEAN";

    // ─────────────────────────────────────────────────────────────────────────
    // 前端本地存储键名（后端不直接使用，仅作文档与前后端契约参考）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 中台管理（{@code ADMIN}）作用域 {@code localStorage} 键名。
     * <p>
     * 前端在写入 / 读取 {@code ThemeProfile} 时使用此键，与后端
     * {@code scope=ADMIN} 一一对应；后端 Java 层不直接读写浏览器本地存储，
     * 但通过本常量与前端保持命名同步，避免前后端因键名漂移产生不一致。
     * </p>
     */
    public static final String LOCAL_STORAGE_KEY_ADMIN = "bml-theme-admin";

    /**
     * 业务系统（{@code BUSINESS}）作用域 {@code localStorage} 键名。
     * <p>
     * 用法与 {@link #LOCAL_STORAGE_KEY_ADMIN} 一致；两者必须保持完全独立的
     * 键名，以满足 R5.AC7 作用域隔离要求。
     * </p>
     */
    public static final String LOCAL_STORAGE_KEY_BUSINESS = "bml-theme-business";

    // ─────────────────────────────────────────────────────────────────────────
    // 跨标签广播通道
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 主题跨标签同步广播通道名。
     * <p>
     * 前端 {@code BroadcastChannel('bml-theme-sync')} 与该常量同名；
     * 后端不参与广播，但保留常量便于前后端契约文档检索与单元测试。
     * </p>
     */
    public static final String BROADCAST_CHANNEL_NAME = "bml-theme-sync";

    // ─────────────────────────────────────────────────────────────────────────
    // 校验正则
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 6 位十六进制颜色正则：{@code ^#[0-9A-Fa-f]{6}$}。
     * <p>
     * 用于 {@code ThemeProfile} 全部颜色字段的合法性校验。该常量同时
     * 被 {@code @HexColor} 注解所对应的 {@code HexColorValidator} 使用，
     * 避免正则在多处复制导致漂移。
     * </p>
     */
    public static final String HEX_COLOR_REGEX = "^#[0-9A-Fa-f]{6}$";

    // ─────────────────────────────────────────────────────────────────────────
    // 权限标识
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 主题预设管理权限标识：{@code system:theme:manage}。
     * <p>
     * 控制器层在新增 / 修改 / 删除自定义预设接口上使用
     * {@code @PreAuthorize("@ss.hasPermi('system:theme:manage')")}；
     * 普通用户只能读取与应用预设，不具备管理权。
     * </p>
     */
    public static final String THEME_MANAGE_PERMISSION = "system:theme:manage";

    /**
     * 私有构造函数 — 禁止外部实例化。
     *
     * @throws AssertionError 一旦通过反射强行实例化即抛出，提示该类纯为常量持有者
     */
    private ThemeConstants() {
        throw new AssertionError("ThemeConstants 为常量持有类，不允许实例化");
    }
}
