package com.bml.module.system.theme.service;

import com.bml.module.system.theme.dto.ThemePresetUpsertDTO;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.enums.ThemeScope;
import com.bml.module.system.theme.vo.ThemePresetVO;
import com.bml.module.system.theme.vo.ThemeProfileVO;

import java.util.List;

/**
 * 主题模块业务服务接口。
 * <p>
 * 集中承载「中台管理（{@code ADMIN}）/ 业务系统（{@code BUSINESS}）」双作用域
 * 主题的全部应用层用例：当前用户主题查询 / 更新 / 恢复默认、主题预设列表、
 * 默认主题查询、平台级自定义预设的增删改。所有方法均围绕以下两张数据库表运转：
 * </p>
 * <ul>
 *   <li>{@code bml_theme_preset} —— 主题预设（系统内置 + 平台级自定义）；</li>
 *   <li>{@code bml_theme_user_setting} —— 用户主题设置（每用户每作用域唯一）。</li>
 * </ul>
 *
 * <h3>核心契约</h3>
 * <ul>
 *   <li><b>恢复默认</b>：{@link #restoreToBest(ThemeScope)} 必须读取
 *       {@link com.bml.module.system.theme.constant.ThemeConstants#PRESET_BEST_ID PRESET_BEST}
 *       对应作用域的变体并 upsert 到 {@code bml_theme_user_setting}（参见 R3.AC2 / R3.AC3）。</li>
 *   <li><b>内置预设保护</b>：对 {@code is_built_in=true} 的预设执行
 *       {@link #updatePreset(String, ThemePresetUpsertDTO) 修改} 或
 *       {@link #deletePreset(String) 删除} 必须抛出
 *       {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE
 *       THEME_BUILTIN_NOT_MUTABLE}（参见 R12.AC2）。</li>
 *   <li><b>预设删除解引用</b>：删除自定义预设时必须调用
 *       {@code clearPresetRefByPresetId(id)} 把所有引用该预设的用户记录的
 *       {@code preset_ref} 置为 {@code NULL}，但保留其 Profile 字段值（参见 R12.AC4）。</li>
 *   <li><b>统一异常 / 统一响应</b>：业务异常统一抛出
 *       {@link com.bml.core.common.exception.BusinessException}，由
 *       {@code GlobalExceptionHandler} 包装为
 *       {@code com.bml.core.common.result.Result<T>}（参见 R7.AC2 / R7.AC3）。</li>
 *   <li><b>请求体校验</b>：写入接口在执行前由
 *       {@link com.bml.module.system.theme.validator.ThemeProfileValidator}
 *       做全量字段收集校验，确保非法字段全量返回（参见 R4.AC8 / R7.AC4）。</li>
 * </ul>
 *
 * @author BML Team
 * @see com.bml.module.system.theme.service.impl.ThemeServiceImpl
 */
public interface ThemeService {

    /**
     * 查询当前登录用户在指定作用域下的主题配置。
     * <p>
     * 优先返回 {@code bml_theme_user_setting} 中按 {@code (userId, scope)}
     * 命中的记录；若用户尚未保存任何设置（首次访问 / 未登录后首次登录），
     * 则回退到 {@code PRESET_BEST} 在该作用域下的变体（{@code profileAdmin}
     * 或 {@code profileBusiness}），从而保证开箱即用即可获得最佳默认外观
     * （参见 R2.AC3 / R3.AC1）。
     * </p>
     *
     * @param scope 作用域；不可为 {@code null}
     * @return 主题配置 VO；当用户与默认预设均缺失时仍可能返回 {@code null}
     *         （正常运行环境下不会出现，仅作为兜底契约）
     */
    ThemeProfileVO getMyProfile(ThemeScope scope);

    /**
     * 新增或更新当前登录用户在指定作用域下的主题配置。
     * <p>
     * 流程：
     * <ol>
     *   <li>先经
     *       {@link com.bml.module.system.theme.validator.ThemeProfileValidator}
     *       做全字段收集校验，非法时一次性抛出
     *       {@code BusinessException(THEME_INVALID_PROFILE, List<FieldError>)}；</li>
     *   <li>再以 {@code (userId, scope)} 为唯一键 upsert 到
     *       {@code bml_theme_user_setting}（数据库唯一索引
     *       {@code uk_user_scope} 保证一行一域）；</li>
     *   <li>最后将持久化结果转换为 VO 返回。</li>
     * </ol>
     * </p>
     *
     * @param scope 作用域；不可为 {@code null}
     * @param dto   主题配置请求体；不可为 {@code null}
     * @return 持久化后的主题配置 VO
     */
    ThemeProfileVO upsertMyProfile(ThemeScope scope, ThemeProfileDTO dto);

    /**
     * 将当前登录用户在指定作用域下的主题恢复为系统默认（{@code PRESET_BEST}）。
     * <p>
     * 必须从 {@code bml_theme_preset} 表读取 {@code PRESET_BEST}（即
     * {@code is_default=1} 的预设）的 {@code profileAdmin} 或
     * {@code profileBusiness} 变体，再以该变体 upsert 到
     * {@code bml_theme_user_setting}，覆盖用户当前所有自定义。
     * </p>
     * <p>
     * 该方法满足 <em>幂等性（{@code P_RESTORE_IDEMPOTENT}）</em>：连续调用 N 次
     * 与单次调用结果完全一致。
     * </p>
     *
     * @param scope 作用域；不可为 {@code null}
     * @return 恢复后的主题配置 VO
     * @throws com.bml.core.common.exception.BusinessException 当系统中不存在
     *         {@code PRESET_BEST} 时抛出 {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_PRESET_NOT_FOUND}
     */
    ThemeProfileVO restoreToBest(ThemeScope scope);

    /**
     * 列出全部主题预设（含系统内置与平台级自定义），按 {@code sort_order} 升序。
     * <p>
     * 直接调用 {@code ThemePresetMapper#selectAllOrderBySort()}；返回结果中
     * 通过 {@code isBuiltIn} 字段区分内置 / 自定义，{@code isDefault} 标识
     * {@code PRESET_BEST}（参见 R12.AC3 / R2.AC5）。
     * </p>
     *
     * @return 所有预设 VO 的有序列表（可能为空集合，永不为 {@code null}）
     */
    List<ThemePresetVO> listPresets();

    /**
     * 返回 {@code PRESET_BEST} 在指定作用域下的默认主题配置。
     * <p>
     * 该方法面向「未登录访问」与「首屏防 FOUC 兜底」场景（参见 R2.AC4 / R6.AC5）。
     * </p>
     *
     * @param scope 作用域；不可为 {@code null}
     * @return {@code PRESET_BEST} 对应作用域的主题配置 VO
     * @throws com.bml.core.common.exception.BusinessException 当系统中不存在
     *         {@code PRESET_BEST} 时抛出 {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_PRESET_NOT_FOUND}
     */
    ThemeProfileVO getDefault(ThemeScope scope);

    /**
     * 创建一条平台级自定义主题预设。
     * <p>
     * 自定义预设固定 {@code is_built_in=false}、{@code is_default=false}；
     * ID 由业务层生成并保证全局唯一。两个作用域变体分别经
     * {@link com.bml.module.system.theme.validator.ThemeProfileValidator}
     * 全字段校验，非法时一次性抛出
     * {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_INVALID_PROFILE}。
     * </p>
     *
     * @param dto 预设新增请求体；不可为 {@code null}
     * @return 创建完成的预设 VO（含数据库回填的 {@code id} / 时间戳）
     */
    ThemePresetVO createPreset(ThemePresetUpsertDTO dto);

    /**
     * 修改指定 ID 的自定义预设。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>若目标预设不存在，抛出
     *       {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_PRESET_NOT_FOUND}；</li>
     *   <li>若目标预设 {@code is_built_in=true}，抛出
     *       {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE}
     *       且数据库不发生任何写操作（参见 R12.AC2 / Property 4
     *       — {@code P_BUILTIN_IMMUTABLE}）；</li>
     *   <li>否则将 {@code name} / {@code description} / {@code sortOrder} /
     *       {@code profileAdmin} / {@code profileBusiness} 字段更新到目标行。</li>
     * </ul>
     * 写入前同样会经
     * {@link com.bml.module.system.theme.validator.ThemeProfileValidator}
     * 校验两个 Profile 变体。
     * </p>
     *
     * @param id  预设 ID；不可为 {@code null} 或空
     * @param dto 预设修改请求体；不可为 {@code null}
     * @return 修改后的预设 VO
     */
    ThemePresetVO updatePreset(String id, ThemePresetUpsertDTO dto);

    /**
     * 删除指定 ID 的自定义预设并解除所有引用。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>若目标预设不存在，抛出
     *       {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_PRESET_NOT_FOUND}；</li>
     *   <li>若目标预设 {@code is_built_in=true}，抛出
     *       {@link com.bml.module.system.theme.enums.ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE}（参见 R12.AC2）；</li>
     *   <li>否则在同一事务中：先调用
     *       {@code ThemeUserSettingMapper#clearPresetRefByPresetId(id)}
     *       将所有引用该预设的用户记录的 {@code preset_ref} 置为 {@code NULL}
     *       （保留其 {@code profile} 字段值），再删除 {@code bml_theme_preset}
     *       中的目标行（参见 R12.AC4 / Property 5 — {@code P_PRESET_DEREF}）。</li>
     * </ul>
     * </p>
     *
     * @param id 预设 ID；不可为 {@code null} 或空
     */
    void deletePreset(String id);
}
