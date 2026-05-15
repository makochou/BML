package com.bml.module.system.theme.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.utils.SecurityUtils;
import com.bml.module.system.theme.constant.ThemeConstants;
import com.bml.module.system.theme.converter.ThemeProfileConverter;
import com.bml.module.system.theme.dto.ThemePresetUpsertDTO;
import com.bml.module.system.theme.dto.ThemeProfileDTO;
import com.bml.module.system.theme.entity.ThemePreset;
import com.bml.module.system.theme.entity.ThemeProfileJson;
import com.bml.module.system.theme.entity.ThemeUserSetting;
import com.bml.module.system.theme.enums.ThemeErrorCode;
import com.bml.module.system.theme.enums.ThemeScope;
import com.bml.module.system.theme.mapper.ThemePresetMapper;
import com.bml.module.system.theme.mapper.ThemeUserSettingMapper;
import com.bml.module.system.theme.service.ThemeService;
import com.bml.module.system.theme.validator.ThemeProfileValidator;
import com.bml.module.system.theme.vo.ThemePresetVO;
import com.bml.module.system.theme.vo.ThemeProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 主题模块业务服务实现。
 *
 * <p>承载「中台管理（{@code ADMIN}）/ 业务系统（{@code BUSINESS}）」双作用域主题的
 * 全部应用层用例。读路径直接访问 Mapper；写路径以
 * {@link Transactional}（{@code rollbackFor = Exception.class}）
 * 包裹，确保 upsert / 解引用 + 删除 等多步操作的原子性。</p>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link ThemePresetMapper} —— 主题预设表读写；</li>
 *   <li>{@link ThemeUserSettingMapper} —— 用户主题设置读写与解引用 SQL；</li>
 *   <li>{@link ThemeProfileValidator} —— 全字段收集校验（保证非法字段全量返回）；</li>
 *   <li>{@link ThemeProfileConverter#INSTANCE} —— Entity / DTO / VO 互转；</li>
 *   <li>{@link SecurityUtils#getUserId()} —— 当前登录用户 ID。</li>
 * </ul>
 *
 * <h3>错误约定</h3>
 * <ul>
 *   <li>非法 Profile：{@link ThemeErrorCode#THEME_INVALID_PROFILE}（载荷为
 *       字段错误列表，由校验器抛出）；</li>
 *   <li>预设不存在：{@link ThemeErrorCode#THEME_PRESET_NOT_FOUND}；</li>
 *   <li>内置预设保护：{@link ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE}（在校验之前抛出，
 *       确保数据库不发生写操作 — Property 4 {@code P_BUILTIN_IMMUTABLE}）。</li>
 * </ul>
 *
 * @author BML Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThemeServiceImpl implements ThemeService {

    /** 主题预设 Mapper。 */
    private final ThemePresetMapper themePresetMapper;

    /** 用户主题设置 Mapper。 */
    private final ThemeUserSettingMapper themeUserSettingMapper;

    /** 主题配置全字段校验器。 */
    private final ThemeProfileValidator themeProfileValidator;

    // ─────────────────────────────────────────────────────────────────────────
    // 当前用户主题
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public ThemeProfileVO getMyProfile(ThemeScope scope) {
        Long userId = SecurityUtils.getUserId();
        ThemeUserSetting setting = themeUserSettingMapper.selectByUserAndScope(userId, scope.name());
        if (setting != null) {
            // 命中用户已保存的设置，直接展开为扁平 VO
            return ThemeProfileConverter.INSTANCE.toProfileVO(setting);
        }
        // 用户尚未保存任何设置，回退到 PRESET_BEST 对应作用域变体（R2.AC3 / R3.AC1）
        return getDefault(scope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThemeProfileVO upsertMyProfile(ThemeScope scope, ThemeProfileDTO dto) {
        // 1. 全字段收集校验，非法时一次性抛出 THEME_INVALID_PROFILE
        themeProfileValidator.validate(dto);

        // 2. 在唯一索引 (user_id, scope) 上 upsert
        Long userId = SecurityUtils.getUserId();
        ThemeProfileJson profileJson = ThemeProfileConverter.INSTANCE.toJson(dto);

        ThemeUserSetting existing = themeUserSettingMapper.selectByUserAndScope(userId, scope.name());
        if (existing == null) {
            ThemeUserSetting entity = new ThemeUserSetting()
                    .setUserId(userId)
                    .setScope(scope)
                    .setPresetRef(dto.getPresetRef())
                    .setProfile(profileJson);
            themeUserSettingMapper.insert(entity);
            return ThemeProfileConverter.INSTANCE.toProfileVO(entity);
        }
        existing.setPresetRef(dto.getPresetRef());
        existing.setProfile(profileJson);
        themeUserSettingMapper.updateById(existing);
        return ThemeProfileConverter.INSTANCE.toProfileVO(existing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThemeProfileVO restoreToBest(ThemeScope scope) {
        // 1. 加载 PRESET_BEST，缺失时直接抛出 THEME_PRESET_NOT_FOUND
        ThemePreset best = loadPresetBest();

        // 2. 根据作用域取出对应变体，构造 JSON 载体（深拷贝，避免后续写入污染缓存）
        ThemeProfileJson source = pickProfileJson(best, scope);
        if (source == null) {
            // PRESET_BEST 必须同时具备两套作用域变体；缺失视为种子数据损坏
            throw new BusinessException(ThemeErrorCode.THEME_PRESET_NOT_FOUND);
        }
        ThemeProfileJson restored = copyOf(source);
        // 恢复默认时 presetRef 指向 PRESET_BEST，前端可据此渲染「当前为系统默认」
        restored.setPresetRef(ThemeConstants.PRESET_BEST_ID);

        // 3. upsert 到 bml_theme_user_setting，覆盖用户当前所有自定义
        Long userId = SecurityUtils.getUserId();
        ThemeUserSetting existing = themeUserSettingMapper.selectByUserAndScope(userId, scope.name());
        if (existing == null) {
            ThemeUserSetting entity = new ThemeUserSetting()
                    .setUserId(userId)
                    .setScope(scope)
                    .setPresetRef(ThemeConstants.PRESET_BEST_ID)
                    .setProfile(restored);
            themeUserSettingMapper.insert(entity);
            return ThemeProfileConverter.INSTANCE.toProfileVO(entity);
        }
        existing.setPresetRef(ThemeConstants.PRESET_BEST_ID);
        existing.setProfile(restored);
        themeUserSettingMapper.updateById(existing);
        return ThemeProfileConverter.INSTANCE.toProfileVO(existing);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 预设查询
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ThemePresetVO> listPresets() {
        List<ThemePreset> presets = themePresetMapper.selectAllOrderBySort();
        return ThemeProfileConverter.INSTANCE.toPresetVOList(presets);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThemeProfileVO getDefault(ThemeScope scope) {
        ThemePreset best = loadPresetBest();
        ThemeProfileJson json = pickProfileJson(best, scope);
        if (json == null) {
            throw new BusinessException(ThemeErrorCode.THEME_PRESET_NOT_FOUND);
        }
        ThemeProfileVO vo = ThemeProfileConverter.INSTANCE.toProfileVO(json);
        if (vo != null) {
            // 标识当前 Profile 来源为 PRESET_BEST，便于前端展示「系统默认」徽章
            vo.setPresetRef(ThemeConstants.PRESET_BEST_ID);
        }
        return vo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 预设增删改
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThemePresetVO createPreset(ThemePresetUpsertDTO dto) {
        // 1. 校验两个作用域变体（全字段收集，错误一次性返回）
        themeProfileValidator.validate(dto.getProfileAdmin());
        themeProfileValidator.validate(dto.getProfileBusiness());

        // 2. 转为实体并固定系统受控字段
        ThemePreset entity = ThemeProfileConverter.INSTANCE.toPresetEntity(dto);
        entity.setId(IdUtil.fastSimpleUUID());
        entity.setIsBuiltIn(false);
        entity.setIsDefault(false);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }

        // 3. 持久化
        themePresetMapper.insert(entity);
        return ThemeProfileConverter.INSTANCE.toPresetVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ThemePresetVO updatePreset(String id, ThemePresetUpsertDTO dto) {
        // 1. 加载并校验存在性 / 内置保护（内置保护必须在校验 / 写入前完成，
        //    确保 Property 4 P_BUILTIN_IMMUTABLE — 数据库不发生任何变更）
        ThemePreset existing = requirePreset(id);
        rejectIfBuiltIn(existing);

        // 2. 校验两个作用域变体
        themeProfileValidator.validate(dto.getProfileAdmin());
        themeProfileValidator.validate(dto.getProfileBusiness());

        // 3. 字段覆盖（保留系统受控字段：id / isBuiltIn / isDefault / 时间戳）
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) {
            existing.setSortOrder(dto.getSortOrder());
        }
        existing.setProfileAdmin(ThemeProfileConverter.INSTANCE.toJson(dto.getProfileAdmin()));
        existing.setProfileBusiness(ThemeProfileConverter.INSTANCE.toJson(dto.getProfileBusiness()));

        themePresetMapper.updateById(existing);
        return ThemeProfileConverter.INSTANCE.toPresetVO(existing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePreset(String id) {
        // 1. 加载并校验存在性 / 内置保护
        ThemePreset existing = requirePreset(id);
        rejectIfBuiltIn(existing);

        // 2. 解引用：将所有引用此预设的用户设置 preset_ref 置 NULL
        //    （保留 profile 字段值不变，参见 R12.AC4 / Property 5 P_PRESET_DEREF）
        int affected = themeUserSettingMapper.clearPresetRefByPresetId(id);
        if (log.isDebugEnabled()) {
            log.debug("删除自定义预设 [{}]，已解除 {} 条用户设置的 preset_ref 引用", id, affected);
        }

        // 3. 删除预设本身
        themePresetMapper.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 加载 {@code PRESET_BEST}（系统默认预设），缺失时抛出
     * {@link ThemeErrorCode#THEME_PRESET_NOT_FOUND}。
     * <p>
     * 优先按 {@code is_default=1} 查询，未命中时按语义 ID 兜底再查一次；
     * 二者皆缺意味着种子数据被破坏。
     * </p>
     *
     * @return PRESET_BEST 实体
     */
    private ThemePreset loadPresetBest() {
        ThemePreset best = themePresetMapper.selectByDefault();
        if (best == null) {
            best = themePresetMapper.selectById(ThemeConstants.PRESET_BEST_ID);
        }
        if (best == null) {
            throw new BusinessException(ThemeErrorCode.THEME_PRESET_NOT_FOUND);
        }
        return best;
    }

    /**
     * 按预设 ID 加载预设实体，缺失或参数为空时抛出
     * {@link ThemeErrorCode#THEME_PRESET_NOT_FOUND}。
     *
     * @param id 预设 ID
     * @return 预设实体（永不为 {@code null}）
     */
    private ThemePreset requirePreset(String id) {
        if (StrUtil.isBlank(id)) {
            throw new BusinessException(ThemeErrorCode.THEME_PRESET_NOT_FOUND);
        }
        ThemePreset preset = themePresetMapper.selectById(id);
        if (preset == null) {
            throw new BusinessException(ThemeErrorCode.THEME_PRESET_NOT_FOUND);
        }
        return preset;
    }

    /**
     * 若预设为系统内置（{@code is_built_in=true}）则抛出
     * {@link ThemeErrorCode#THEME_BUILTIN_NOT_MUTABLE}，以保证数据库不发生写操作
     * （满足 R12.AC2 / Property 4 {@code P_BUILTIN_IMMUTABLE}）。
     *
     * @param preset 待检查的预设
     */
    private void rejectIfBuiltIn(ThemePreset preset) {
        if (Boolean.TRUE.equals(preset.getIsBuiltIn())) {
            throw new BusinessException(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE);
        }
    }

    /**
     * 按作用域选择预设的对应变体（{@code profileAdmin} 或 {@code profileBusiness}）。
     *
     * @param preset 预设实体（不可为 {@code null}）
     * @param scope  作用域（不可为 {@code null}）
     * @return 对应变体的 JSON 载体；当对应字段缺失时返回 {@code null}
     */
    private ThemeProfileJson pickProfileJson(ThemePreset preset, ThemeScope scope) {
        return scope == ThemeScope.ADMIN ? preset.getProfileAdmin() : preset.getProfileBusiness();
    }

    /**
     * 创建 {@link ThemeProfileJson} 的浅拷贝。
     * <p>
     * 该 POJO 字段全部为 {@link String} 与枚举（不可变），浅拷贝即可满足
     * 「不污染源对象」的需求；在 restore 路径中用于避免将
     * {@code presetRef = PRESET_BEST_ID} 写回内存中的预设实体。
     * </p>
     *
     * @param src 源对象（不可为 {@code null}）
     * @return 副本
     */
    private ThemeProfileJson copyOf(ThemeProfileJson src) {
        return new ThemeProfileJson()
                .setPrimaryColor(src.getPrimaryColor())
                .setSecondaryColor(src.getSecondaryColor())
                .setAccentColor(src.getAccentColor())
                .setSuccessColor(src.getSuccessColor())
                .setWarningColor(src.getWarningColor())
                .setErrorColor(src.getErrorColor())
                .setInfoColor(src.getInfoColor())
                .setTextColor(src.getTextColor())
                .setBackgroundColor(src.getBackgroundColor())
                .setBorderColor(src.getBorderColor())
                .setMode(src.getMode())
                .setRadius(src.getRadius())
                .setDensity(src.getDensity())
                .setSidebarStyle(src.getSidebarStyle())
                .setSidebarCollapsedStyle(src.getSidebarCollapsedStyle())
                .setHeaderStyle(src.getHeaderStyle())
                .setFontScale(src.getFontScale())
                .setPresetRef(src.getPresetRef());
    }
}
