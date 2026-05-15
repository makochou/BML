package com.bml.module.system.theme.controller;

import com.bml.core.common.result.Result;
import com.bml.module.system.theme.dto.ThemePresetUpsertDTO;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.enums.ThemeScope;
import com.bml.module.system.theme.service.ThemeService;
import com.bml.module.system.theme.vo.ThemePresetVO;
import com.bml.module.system.theme.vo.ThemeProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 主题模块对外 REST 控制器。
 * <p>
 * 承载「中台管理（{@link ThemeScope#ADMIN ADMIN}）/ 业务系统（{@link ThemeScope#BUSINESS BUSINESS}）」
 * 双作用域主题的全部 HTTP 端点，作用域由请求参数 {@code scope} 携带，所有端点
 * 路径相对前缀为 {@code /theme}（再叠加全局 {@code /api} 上下文路径）。
 * </p>
 *
 * <h3>端点矩阵（与设计文档接口表对齐）</h3>
 * <table border="1">
 *   <caption>主题接口权限矩阵</caption>
 *   <tr><th>方法</th><th>路径</th><th>权限</th><th>说明</th></tr>
 *   <tr><td>GET</td>    <td>{@code /theme/presets}</td>      <td>已登录</td>             <td>列出全部预设（含内置与自定义）</td></tr>
 *   <tr><td>GET</td>    <td>{@code /theme/me}</td>           <td>已登录</td>             <td>查询当前用户在指定作用域下的主题</td></tr>
 *   <tr><td>PUT</td>    <td>{@code /theme/me}</td>           <td>已登录</td>             <td>upsert 当前用户在指定作用域下的主题</td></tr>
 *   <tr><td>POST</td>   <td>{@code /theme/restore}</td>      <td>已登录</td>             <td>恢复至 {@code PRESET_BEST}</td></tr>
 *   <tr><td>GET</td>    <td>{@code /theme/default}</td>      <td>公开（白名单）</td>     <td>返回 {@code PRESET_BEST} 的对应作用域变体</td></tr>
 *   <tr><td>POST</td>   <td>{@code /theme/presets}</td>      <td>{@code system:theme:manage}</td><td>新增自定义预设</td></tr>
 *   <tr><td>PUT</td>    <td>{@code /theme/presets/{id}}</td> <td>{@code system:theme:manage}</td><td>修改自定义预设</td></tr>
 *   <tr><td>DELETE</td> <td>{@code /theme/presets/{id}}</td> <td>{@code system:theme:manage}</td><td>删除自定义预设</td></tr>
 * </table>
 *
 * <h3>统一响应与统一异常</h3>
 * <p>
 * 所有端点以 {@link Result} 包装返回值，禁止裸返回业务对象（参见 R7.AC2）；
 * 业务异常由 {@code GlobalExceptionHandler} 统一映射为 {@code Result} 错误响应
 * （参见 R7.AC3）。
 * </p>
 *
 * <h3>参数校验</h3>
 * <p>
 * 类上 {@link Validated} 启用方法级参数校验，使 {@link RequestParam} 上的
 * {@link NotNull} / {@link NotBlank} 注解生效；请求体在端点形参上使用
 * {@code @Validated} 触发 JSR-380 字段级校验，进一步业务校验
 * （全字段收集语义）由 {@code ThemeProfileValidator} 在 service 入口完成
 * （参见 R4.AC8 / R7.AC4）。
 * </p>
 *
 * <h3>白名单</h3>
 * <p>
 * {@code GET /theme/default} 用于未登录访问与首屏防 FOUC 兜底（R2.AC4 / R6.AC5），
 * 需要在平台 Security 白名单中放行，由相邻任务 8.1 统一在 {@code SecurityConfig}
 * 注册；本控制器不再额外处理匿名访问。
 * </p>
 *
 * @author BML Team
 * @see ThemeService
 */
@Tag(name = "主题设置", description = "中台 / 业务系统双作用域主题配置接口")
@RestController
@RequestMapping("/theme")
@RequiredArgsConstructor
@Validated
public class ThemeController {

    /** 主题模块业务服务，承载全部应用层用例。 */
    private final ThemeService themeService;

    // ─────────────────────────────────────────────────────────────────────────
    // 预设查询
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 列出全部主题预设（含系统内置与平台级自定义），按 {@code sort_order} 升序。
     * <p>
     * 仅要求已登录访问；返回结果通过 {@code isBuiltIn} 字段区分内置 / 自定义，
     * 通过 {@code isDefault} 标识 {@code PRESET_BEST}（参见 R12.AC3 / R2.AC5）。
     * </p>
     *
     * @return 预设 VO 有序列表
     */
    @Operation(summary = "列出全部主题预设", description = "返回内置与自定义预设，按排序权重升序")
    @GetMapping("/presets")
    public Result<List<ThemePresetVO>> listPresets() {
        return Result.ok(themeService.listPresets());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 当前用户主题
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 查询当前登录用户在指定作用域下的主题配置。
     * <p>
     * 当用户尚未保存任何配置时回退到 {@code PRESET_BEST} 对应作用域变体
     * （参见 R2.AC3 / R3.AC1）。
     * </p>
     *
     * @param scope 作用域（必填）
     * @return 主题配置 VO
     */
    @Operation(summary = "查询当前用户主题", description = "命中用户设置则返回，否则返回 PRESET_BEST 对应作用域变体")
    @GetMapping("/me")
    public Result<ThemeProfileVO> getMyProfile(
            @RequestParam @NotNull(message = "作用域不能为空") ThemeScope scope) {
        return Result.ok(themeService.getMyProfile(scope));
    }

    /**
     * 新增或更新当前登录用户在指定作用域下的主题配置。
     * <p>
     * 请求体经 JSR-380 与 {@code ThemeProfileValidator} 双重校验；非法字段全量返回
     * （参见 R4.AC8 / R7.AC4）。
     * </p>
     *
     * @param scope 作用域（必填）
     * @param dto   主题配置请求体（必填）
     * @return 持久化后的主题配置 VO
     */
    @Operation(summary = "更新当前用户主题", description = "在唯一索引 (user_id, scope) 上 upsert")
    @PutMapping("/me")
    public Result<ThemeProfileVO> updateMyProfile(
            @RequestParam @NotNull(message = "作用域不能为空") ThemeScope scope,
            @Validated @RequestBody ThemeProfileDTO dto) {
        return Result.ok(themeService.upsertMyProfile(scope, dto));
    }

    /**
     * 将当前登录用户在指定作用域下的主题恢复为系统默认（{@code PRESET_BEST}）。
     * <p>
     * 该接口满足幂等性（{@code P_RESTORE_IDEMPOTENT}），连续调用 N 次与单次调用结果一致
     * （参见 R3.AC2 / R3.AC3）。
     * </p>
     *
     * @param scope 作用域（必填）
     * @return 恢复后的主题配置 VO
     */
    @Operation(summary = "恢复至系统默认主题", description = "将当前作用域 Profile 重置为 PRESET_BEST 对应变体")
    @PostMapping("/restore")
    public Result<ThemeProfileVO> restore(
            @RequestParam @NotNull(message = "作用域不能为空") ThemeScope scope) {
        return Result.ok(themeService.restoreToBest(scope));
    }

    /**
     * 返回 {@code PRESET_BEST} 在指定作用域下的默认主题配置。
     * <p>
     * 该端点面向「未登录访问」与「首屏防 FOUC 兜底」场景（R2.AC4 / R6.AC5），
     * 由 {@code SecurityConfig} 统一加入白名单放行未登录调用。
     * </p>
     *
     * @param scope 作用域（必填）
     * @return {@code PRESET_BEST} 对应作用域的主题配置 VO
     */
    @Operation(summary = "查询系统默认主题", description = "返回 PRESET_BEST 对应作用域变体（公开访问）")
    @GetMapping("/default")
    public Result<ThemeProfileVO> getDefault(
            @RequestParam @NotNull(message = "作用域不能为空") ThemeScope scope) {
        return Result.ok(themeService.getDefault(scope));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 预设增删改（受保护）
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 新增一条平台级自定义主题预设。
     * <p>
     * 受 {@code system:theme:manage} 权限保护；ID 由业务层生成，
     * {@code is_built_in} / {@code is_default} 固定为 {@code false}
     * （参见 R12.AC1 / R12.AC5）。
     * </p>
     *
     * @param dto 预设新增请求体（必填）
     * @return 创建完成的预设 VO
     */
    @Operation(summary = "新增自定义主题预设", description = "仅 system:theme:manage 权限可访问")
    @PreAuthorize("@ss.hasPermi('system:theme:manage')")
    @PostMapping("/presets")
    public Result<ThemePresetVO> createPreset(
            @Validated @RequestBody ThemePresetUpsertDTO dto) {
        return Result.ok(themeService.createPreset(dto));
    }

    /**
     * 修改指定 ID 的自定义主题预设。
     * <p>
     * 受 {@code system:theme:manage} 权限保护；当目标预设 {@code is_built_in=true}
     * （含 {@code PRESET_BEST}）时由 service 层抛出 {@code THEME_BUILTIN_NOT_MUTABLE}
     * （参见 R12.AC2 / Property 4 — {@code P_BUILTIN_IMMUTABLE}）。
     * </p>
     *
     * @param id  预设 ID（路径参数，必填）
     * @param dto 预设修改请求体（必填）
     * @return 修改后的预设 VO
     */
    @Operation(summary = "修改自定义主题预设", description = "仅 system:theme:manage 权限可访问；内置预设受保护")
    @PreAuthorize("@ss.hasPermi('system:theme:manage')")
    @PutMapping("/presets/{id}")
    public Result<ThemePresetVO> updatePreset(
            @PathVariable @NotBlank(message = "预设 ID 不能为空") String id,
            @Validated @RequestBody ThemePresetUpsertDTO dto) {
        return Result.ok(themeService.updatePreset(id, dto));
    }

    /**
     * 删除指定 ID 的自定义主题预设并自动解除引用。
     * <p>
     * 受 {@code system:theme:manage} 权限保护；删除前会将所有引用该预设的
     * {@code bml_theme_user_setting.preset_ref} 置为 {@code NULL}（保留 profile 字段值）
     * 以满足 R12.AC4 / Property 5 — {@code P_PRESET_DEREF}；内置预设受保护，
     * 抛出 {@code THEME_BUILTIN_NOT_MUTABLE}。
     * </p>
     *
     * @param id 预设 ID（路径参数，必填）
     * @return 空响应
     */
    @Operation(summary = "删除自定义主题预设", description = "仅 system:theme:manage 权限可访问；内置预设受保护")
    @PreAuthorize("@ss.hasPermi('system:theme:manage')")
    @DeleteMapping("/presets/{id}")
    public Result<Void> deletePreset(
            @PathVariable @NotBlank(message = "预设 ID 不能为空") String id) {
        themeService.deletePreset(id);
        return Result.ok();
    }
}
