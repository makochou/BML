package com.bml.module.system.theme.service;

import com.bml.core.common.exception.BusinessException;
import com.bml.core.framework.security.utils.SecurityUtils;
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
import com.bml.module.system.theme.enums.ThemeScope;
import com.bml.module.system.theme.mapper.ThemePresetMapper;
import com.bml.module.system.theme.mapper.ThemeUserSettingMapper;
import com.bml.module.system.theme.service.impl.ThemeServiceImpl;
import com.bml.module.system.theme.validator.ThemeProfileValidator;
import com.bml.module.system.theme.vo.ThemePresetVO;
import com.bml.module.system.theme.vo.ThemeProfileVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ThemeServiceImpl} 单元测试。
 *
 * <p>使用 Mockito 隔离 Mapper / 校验器 / 安全上下文等外部依赖，覆盖以下用例：</p>
 * <ul>
 *     <li>{@link ThemeServiceImpl#getMyProfile(ThemeScope) getMyProfile}：
 *         命中已有用户设置 / 未命中回退到 PRESET_BEST 两条路径；</li>
 *     <li>{@link ThemeServiceImpl#upsertMyProfile(ThemeScope, ThemeProfileDTO) upsertMyProfile}：
 *         新建插入 / 既有更新 / 校验失败短路三种情况；</li>
 *     <li>{@link ThemeServiceImpl#restoreToBest(ThemeScope) restoreToBest}：
 *         新建 / 更新 / PRESET_BEST 缺失三种情况，并验证幂等回写 {@code presetRef=PRESET_BEST}；</li>
 *     <li>{@link ThemeServiceImpl#listPresets() listPresets} 与
 *         {@link ThemeServiceImpl#getDefault(ThemeScope) getDefault}：
 *         返回 VO 列表 / 设置 PRESET_BEST 标识；</li>
 *     <li>预设 CRUD：{@link ThemeServiceImpl#createPreset(ThemePresetUpsertDTO) createPreset}
 *         强制系统受控字段；{@link ThemeServiceImpl#updatePreset(String, ThemePresetUpsertDTO) updatePreset}
 *         与 {@link ThemeServiceImpl#deletePreset(String) deletePreset} 对内置预设的不可变保护
 *         （Property 4 — {@code P_BUILTIN_IMMUTABLE}）；删除时调用解引用 SQL
 *         （Property 5 — {@code P_PRESET_DEREF}）。</li>
 * </ul>
 *
 * <p>对应任务：tasks.md → 6.2；对应需求：3.2、5.1、7.1、12.1。</p>
 *
 * @author BML Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ThemeServiceImpl 业务服务测试")
class ThemeServiceImplTest {

    /** 测试中固定使用的当前用户 ID（由 {@link SecurityUtils#getUserId()} 静态桩返回）。 */
    private static final Long MOCK_USER_ID = 42L;

    @Mock
    private ThemePresetMapper themePresetMapper;

    @Mock
    private ThemeUserSettingMapper themeUserSettingMapper;

    @Mock
    private ThemeProfileValidator themeProfileValidator;

    @InjectMocks
    private ThemeServiceImpl themeService;

    /** SecurityUtils 静态桩 —— 仅在需要当前用户上下文的用例中开启。 */
    private MockedStatic<SecurityUtils> securityUtilsStatic;

    @BeforeEach
    void setUp() {
        securityUtilsStatic = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsStatic.when(SecurityUtils::getUserId).thenReturn(MOCK_USER_ID);
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsStatic != null) {
            securityUtilsStatic.close();
        }
    }

    // =========================================================================
    // getMyProfile
    // =========================================================================

    @Nested
    @DisplayName("getMyProfile —— 当前用户主题查询")
    class GetMyProfileTests {

        @Test
        @DisplayName("命中用户已保存设置 → 返回该设置展开的 VO")
        void getMyProfile_HitsUserSetting_ReturnsItsVo() {
            ThemeUserSetting setting = new ThemeUserSetting()
                    .setId(1L)
                    .setUserId(MOCK_USER_ID)
                    .setScope(ThemeScope.ADMIN)
                    .setPresetRef("CUSTOM_X")
                    .setProfile(buildBaselineJson("#165DFF"));

            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.ADMIN.name()))
                    .thenReturn(setting);

            ThemeProfileVO vo = themeService.getMyProfile(ThemeScope.ADMIN);

            assertThat(vo).isNotNull();
            assertThat(vo.getPrimaryColor()).isEqualTo("#165DFF");
            // 以外层 presetRef 为准（解引用 SQL 直接生效）
            assertThat(vo.getPresetRef()).isEqualTo("CUSTOM_X");

            // 命中时不应回退到 PRESET_BEST
            verify(themePresetMapper, never()).selectByDefault();
            verify(themePresetMapper, never()).selectById(anyString());
        }

        @Test
        @DisplayName("未命中用户设置 → 回退到 PRESET_BEST 对应作用域的变体")
        void getMyProfile_NoUserSetting_FallsBackToPresetBest() {
            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.BUSINESS.name()))
                    .thenReturn(null);

            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectByDefault()).thenReturn(best);

            ThemeProfileVO vo = themeService.getMyProfile(ThemeScope.BUSINESS);

            assertThat(vo).isNotNull();
            // BUSINESS 变体的主色（与 buildPresetBest 中设置一致）
            assertThat(vo.getPrimaryColor()).isEqualTo("#22A6F2");
            assertThat(vo.getMode()).isEqualTo(ThemeMode.LIGHT);
            // getDefault 路径会把 presetRef 标记为 PRESET_BEST
            assertThat(vo.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);
            verify(themePresetMapper, times(1)).selectByDefault();
        }
    }

    // =========================================================================
    // upsertMyProfile
    // =========================================================================

    @Nested
    @DisplayName("upsertMyProfile —— 当前用户主题写入")
    class UpsertMyProfileTests {

        @Test
        @DisplayName("用户尚无设置 → 校验后插入新行并返回 VO")
        void upsertMyProfile_NewSetting_InsertsAndReturnsVo() {
            ThemeProfileDTO dto = buildValidProfileDto();
            dto.setPresetRef("PRESET_OCEAN");

            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.ADMIN.name()))
                    .thenReturn(null);
            when(themeUserSettingMapper.insert(any(ThemeUserSetting.class)))
                    .thenAnswer(inv -> {
                        ThemeUserSetting entity = inv.getArgument(0);
                        entity.setId(99L);
                        entity.setUpdatedAt(LocalDateTime.now());
                        return 1;
                    });

            ThemeProfileVO vo = themeService.upsertMyProfile(ThemeScope.ADMIN, dto);

            // 校验先于写入
            verify(themeProfileValidator, times(1)).validate(dto);
            // 走 insert 而不是 update 路径
            verify(themeUserSettingMapper, times(1)).insert(any(ThemeUserSetting.class));
            verify(themeUserSettingMapper, never()).updateById(any(ThemeUserSetting.class));

            assertThat(vo).isNotNull();
            assertThat(vo.getPrimaryColor()).isEqualTo(dto.getPrimaryColor());
            assertThat(vo.getMode()).isEqualTo(dto.getMode());
            assertThat(vo.getPresetRef()).isEqualTo("PRESET_OCEAN");
        }

        @Test
        @DisplayName("用户已有设置 → 校验后更新现有行并返回 VO")
        void upsertMyProfile_ExistingSetting_UpdatesAndReturnsVo() {
            ThemeUserSetting existing = new ThemeUserSetting()
                    .setId(7L)
                    .setUserId(MOCK_USER_ID)
                    .setScope(ThemeScope.BUSINESS)
                    .setPresetRef(ThemeConstants.PRESET_BEST_ID)
                    .setProfile(buildBaselineJson("#000000"));

            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.BUSINESS.name()))
                    .thenReturn(existing);
            when(themeUserSettingMapper.updateById(any(ThemeUserSetting.class))).thenReturn(1);

            ThemeProfileDTO dto = buildValidProfileDto();
            dto.setPrimaryColor("#11AA22");
            dto.setPresetRef(null); // 用户改为完全自定义，解除预设引用

            ThemeProfileVO vo = themeService.upsertMyProfile(ThemeScope.BUSINESS, dto);

            verify(themeProfileValidator, times(1)).validate(dto);
            verify(themeUserSettingMapper, never()).insert(any(ThemeUserSetting.class));
            verify(themeUserSettingMapper, times(1)).updateById(any(ThemeUserSetting.class));

            assertThat(vo).isNotNull();
            assertThat(vo.getPrimaryColor()).isEqualTo("#11AA22");
            assertThat(vo.getPresetRef()).isNull();
            // 现有实体的 profile / presetRef 已被覆写
            assertThat(existing.getPresetRef()).isNull();
            assertThat(existing.getProfile().getPrimaryColor()).isEqualTo("#11AA22");
        }

        @Test
        @DisplayName("校验失败 → 抛出 BusinessException 且不写入数据库")
        void upsertMyProfile_InvalidProfile_ThrowsAndSkipsPersistence() {
            ThemeProfileDTO dto = buildValidProfileDto();
            BusinessException expected =
                    new BusinessException(ThemeErrorCode.THEME_INVALID_PROFILE, List.of());
            Mockito.doThrow(expected).when(themeProfileValidator).validate(dto);

            assertThatThrownBy(() -> themeService.upsertMyProfile(ThemeScope.ADMIN, dto))
                    .isSameAs(expected);

            // 校验失败必须短路：mapper 不应被调用
            verifyNoInteractions(themeUserSettingMapper);
        }
    }

    // =========================================================================
    // restoreToBest
    // =========================================================================

    @Nested
    @DisplayName("restoreToBest —— 恢复 PRESET_BEST")
    class RestoreToBestTests {

        @Test
        @DisplayName("用户尚无设置 → 插入新行，profile 为 PRESET_BEST 变体且 presetRef=PRESET_BEST")
        void restoreToBest_NewSetting_InsertsPresetBestVariant() {
            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectByDefault()).thenReturn(best);
            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.ADMIN.name()))
                    .thenReturn(null);
            when(themeUserSettingMapper.insert(any(ThemeUserSetting.class)))
                    .thenAnswer(inv -> {
                        ThemeUserSetting entity = inv.getArgument(0);
                        entity.setId(123L);
                        entity.setUpdatedAt(LocalDateTime.now());
                        return 1;
                    });

            ThemeProfileVO vo = themeService.restoreToBest(ThemeScope.ADMIN);

            verify(themeUserSettingMapper, times(1)).insert(any(ThemeUserSetting.class));
            verify(themeUserSettingMapper, never()).updateById(any(ThemeUserSetting.class));

            assertThat(vo).isNotNull();
            // ADMIN 变体的主色（buildPresetBest 中 ADMIN=#165DFF）
            assertThat(vo.getPrimaryColor()).isEqualTo("#165DFF");
            assertThat(vo.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);

            // 不应写回 best 实体内存中的 presetRef（深拷贝保证）
            assertThat(best.getProfileAdmin().getPresetRef()).isNull();
        }

        @Test
        @DisplayName("用户已有设置 → 覆盖现有行，profile 替换为 PRESET_BEST 变体且 presetRef=PRESET_BEST")
        void restoreToBest_ExistingSetting_UpdatesToPresetBestVariant() {
            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectByDefault()).thenReturn(best);

            ThemeUserSetting existing = new ThemeUserSetting()
                    .setId(8L)
                    .setUserId(MOCK_USER_ID)
                    .setScope(ThemeScope.BUSINESS)
                    .setPresetRef(null)
                    .setProfile(buildBaselineJson("#999999"));
            when(themeUserSettingMapper.selectByUserAndScope(MOCK_USER_ID, ThemeScope.BUSINESS.name()))
                    .thenReturn(existing);
            when(themeUserSettingMapper.updateById(any(ThemeUserSetting.class))).thenReturn(1);

            ThemeProfileVO vo = themeService.restoreToBest(ThemeScope.BUSINESS);

            verify(themeUserSettingMapper, never()).insert(any(ThemeUserSetting.class));
            verify(themeUserSettingMapper, times(1)).updateById(any(ThemeUserSetting.class));

            // 现有实体被改写
            assertThat(existing.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);
            assertThat(existing.getProfile().getPrimaryColor()).isEqualTo("#22A6F2");

            assertThat(vo).isNotNull();
            assertThat(vo.getPrimaryColor()).isEqualTo("#22A6F2");
            assertThat(vo.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);
        }

        @Test
        @DisplayName("PRESET_BEST 缺失 → 抛出 THEME_PRESET_NOT_FOUND")
        void restoreToBest_NoPresetBest_ThrowsThemePresetNotFound() {
            when(themePresetMapper.selectByDefault()).thenReturn(null);
            when(themePresetMapper.selectById(ThemeConstants.PRESET_BEST_ID)).thenReturn(null);

            assertThatThrownBy(() -> themeService.restoreToBest(ThemeScope.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_PRESET_NOT_FOUND.getCode()));

            verify(themeUserSettingMapper, never()).insert(any(ThemeUserSetting.class));
            verify(themeUserSettingMapper, never()).updateById(any(ThemeUserSetting.class));
        }
    }

    // =========================================================================
    // listPresets / getDefault
    // =========================================================================

    @Nested
    @DisplayName("预设查询")
    class ListAndDefaultTests {

        @Test
        @DisplayName("listPresets 应将 mapper 返回的实体列表转换为 VO 列表")
        void listPresets_ReturnsAllConvertedToVo() {
            ThemePreset best = buildPresetBest();
            ThemePreset ocean = buildOceanPreset();
            when(themePresetMapper.selectAllOrderBySort()).thenReturn(List.of(best, ocean));

            List<ThemePresetVO> vos = themeService.listPresets();

            assertThat(vos).hasSize(2);
            assertThat(vos).extracting(ThemePresetVO::getId)
                    .containsExactly(ThemeConstants.PRESET_BEST_ID, ThemeConstants.PRESET_OCEAN_ID);
            assertThat(vos.get(0).getIsBuiltIn()).isTrue();
            assertThat(vos.get(0).getIsDefault()).isTrue();
            assertThat(vos.get(0).getProfileAdmin()).isNotNull();
            assertThat(vos.get(0).getProfileAdmin().getPrimaryColor()).isEqualTo("#165DFF");
            assertThat(vos.get(0).getProfileBusiness().getPrimaryColor()).isEqualTo("#22A6F2");
        }

        @Test
        @DisplayName("getDefault 应返回 PRESET_BEST 对应作用域变体并标记 presetRef=PRESET_BEST")
        void getDefault_ReturnsPresetBestVariantWithPresetRef() {
            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectByDefault()).thenReturn(best);

            ThemeProfileVO admin = themeService.getDefault(ThemeScope.ADMIN);
            assertThat(admin.getPrimaryColor()).isEqualTo("#165DFF");
            assertThat(admin.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);

            ThemeProfileVO business = themeService.getDefault(ThemeScope.BUSINESS);
            assertThat(business.getPrimaryColor()).isEqualTo("#22A6F2");
            assertThat(business.getPresetRef()).isEqualTo(ThemeConstants.PRESET_BEST_ID);
        }

        @Test
        @DisplayName("PRESET_BEST 缺失 → getDefault 抛出 THEME_PRESET_NOT_FOUND")
        void getDefault_NoPresetBest_ThrowsThemePresetNotFound() {
            when(themePresetMapper.selectByDefault()).thenReturn(null);
            when(themePresetMapper.selectById(ThemeConstants.PRESET_BEST_ID)).thenReturn(null);

            assertThatThrownBy(() -> themeService.getDefault(ThemeScope.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_PRESET_NOT_FOUND.getCode()));
        }
    }

    // =========================================================================
    // CRUD presets
    // =========================================================================

    @Nested
    @DisplayName("createPreset / updatePreset / deletePreset")
    class PresetCrudTests {

        @Test
        @DisplayName("createPreset 应校验两个变体、写入实体并强制系统受控字段")
        void createPreset_InsertsWithGeneratedIdAndForcedFlags() {
            ThemeProfileDTO adminDto = buildValidProfileDto();
            adminDto.setPrimaryColor("#101010");
            ThemeProfileDTO businessDto = buildValidProfileDto();
            businessDto.setPrimaryColor("#202020");
            ThemePresetUpsertDTO dto = new ThemePresetUpsertDTO()
                    .setName("自定义海洋")
                    .setDescription("基于海洋蓝衍生")
                    // 不显式提供 sortOrder，验证 service 默认置 0
                    .setProfileAdmin(adminDto)
                    .setProfileBusiness(businessDto);
            when(themePresetMapper.insert(any(ThemePreset.class))).thenReturn(1);

            ThemePresetVO vo = themeService.createPreset(dto);

            // 两个变体都校验过（用不同实例避免 equals 收敛为同一调用）
            verify(themeProfileValidator, times(1)).validate(adminDto);
            verify(themeProfileValidator, times(1)).validate(businessDto);
            verify(themePresetMapper, times(1)).insert(any(ThemePreset.class));

            assertThat(vo).isNotNull();
            assertThat(vo.getId()).isNotBlank();
            assertThat(vo.getName()).isEqualTo("自定义海洋");
            assertThat(vo.getDescription()).isEqualTo("基于海洋蓝衍生");
            // 系统受控字段
            assertThat(vo.getIsBuiltIn()).isFalse();
            assertThat(vo.getIsDefault()).isFalse();
            assertThat(vo.getSortOrder()).isEqualTo(0);
            assertThat(vo.getProfileAdmin()).isNotNull();
            assertThat(vo.getProfileAdmin().getPrimaryColor()).isEqualTo("#101010");
            assertThat(vo.getProfileBusiness()).isNotNull();
            assertThat(vo.getProfileBusiness().getPrimaryColor()).isEqualTo("#202020");
        }

        @Test
        @DisplayName("updatePreset 内置预设 → 抛出 THEME_BUILTIN_NOT_MUTABLE 且不写入")
        void updatePreset_BuiltinPreset_ThrowsAndSkipsPersistence() {
            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectById(ThemeConstants.PRESET_BEST_ID)).thenReturn(best);

            ThemePresetUpsertDTO dto = new ThemePresetUpsertDTO()
                    .setName("篡改尝试")
                    .setProfileAdmin(buildValidProfileDto())
                    .setProfileBusiness(buildValidProfileDto());

            assertThatThrownBy(() -> themeService.updatePreset(ThemeConstants.PRESET_BEST_ID, dto))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE.getCode()));

            // Property 4 P_BUILTIN_IMMUTABLE：内置预设保护必须先于校验与写入触发
            verify(themeProfileValidator, never()).validate(any(ThemeProfileDTO.class));
            verify(themePresetMapper, never()).updateById(any(ThemePreset.class));
        }

        @Test
        @DisplayName("updatePreset 预设不存在 → 抛出 THEME_PRESET_NOT_FOUND")
        void updatePreset_NotFound_ThrowsThemePresetNotFound() {
            when(themePresetMapper.selectById("UNKNOWN")).thenReturn(null);

            assertThatThrownBy(() -> themeService.updatePreset("UNKNOWN", new ThemePresetUpsertDTO()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_PRESET_NOT_FOUND.getCode()));

            verify(themePresetMapper, never()).updateById(any(ThemePreset.class));
        }

        @Test
        @DisplayName("updatePreset 自定义预设 → 校验后更新并返回 VO，受控字段不变")
        void updatePreset_CustomPreset_UpdatesAndReturnsVo() {
            ThemePreset custom = buildOceanPreset()
                    .setId("CUSTOM_X")
                    .setIsBuiltIn(false)
                    .setIsDefault(false)
                    .setSortOrder(50);
            when(themePresetMapper.selectById("CUSTOM_X")).thenReturn(custom);
            when(themePresetMapper.updateById(any(ThemePreset.class))).thenReturn(1);

            ThemeProfileDTO adminDto = buildValidProfileDto();
            adminDto.setPrimaryColor("#AABBCC");
            ThemeProfileDTO businessDto = buildValidProfileDto();
            businessDto.setPrimaryColor("#DDEEFF");
            ThemePresetUpsertDTO dto = new ThemePresetUpsertDTO()
                    .setName("更名后的海洋")
                    .setDescription("最新描述")
                    .setSortOrder(10)
                    .setProfileAdmin(adminDto)
                    .setProfileBusiness(businessDto);

            ThemePresetVO vo = themeService.updatePreset("CUSTOM_X", dto);

            verify(themeProfileValidator, times(1)).validate(adminDto);
            verify(themeProfileValidator, times(1)).validate(businessDto);
            verify(themePresetMapper, times(1)).updateById(any(ThemePreset.class));

            assertThat(custom.getName()).isEqualTo("更名后的海洋");
            assertThat(custom.getDescription()).isEqualTo("最新描述");
            assertThat(custom.getSortOrder()).isEqualTo(10);
            // 系统受控字段保持不变
            assertThat(custom.getIsBuiltIn()).isFalse();
            assertThat(custom.getIsDefault()).isFalse();
            assertThat(custom.getProfileAdmin().getPrimaryColor()).isEqualTo("#AABBCC");
            assertThat(custom.getProfileBusiness().getPrimaryColor()).isEqualTo("#DDEEFF");

            assertThat(vo).isNotNull();
            assertThat(vo.getName()).isEqualTo("更名后的海洋");
            assertThat(vo.getProfileAdmin().getPrimaryColor()).isEqualTo("#AABBCC");
            assertThat(vo.getProfileBusiness().getPrimaryColor()).isEqualTo("#DDEEFF");
        }

        @Test
        @DisplayName("deletePreset 内置预设 → 抛出 THEME_BUILTIN_NOT_MUTABLE 且不写入")
        void deletePreset_BuiltinPreset_ThrowsAndSkipsPersistence() {
            ThemePreset best = buildPresetBest();
            when(themePresetMapper.selectById(ThemeConstants.PRESET_BEST_ID)).thenReturn(best);

            assertThatThrownBy(() -> themeService.deletePreset(ThemeConstants.PRESET_BEST_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_BUILTIN_NOT_MUTABLE.getCode()));

            // 删除被拒绝时既不解引用也不删除
            verify(themeUserSettingMapper, never()).clearPresetRefByPresetId(anyString());
            verify(themePresetMapper, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("deletePreset 预设不存在 → 抛出 THEME_PRESET_NOT_FOUND")
        void deletePreset_NotFound_ThrowsThemePresetNotFound() {
            when(themePresetMapper.selectById("UNKNOWN")).thenReturn(null);

            assertThatThrownBy(() -> themeService.deletePreset("UNKNOWN"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(ThemeErrorCode.THEME_PRESET_NOT_FOUND.getCode()));

            verify(themeUserSettingMapper, never()).clearPresetRefByPresetId(anyString());
            verify(themePresetMapper, never()).deleteById(anyString());
        }

        @Test
        @DisplayName("deletePreset 自定义预设 → 先解引用 user setting 再删除预设本身")
        void deletePreset_CustomPreset_ClearsRefsThenDeletes() {
            ThemePreset custom = buildOceanPreset().setId("CUSTOM_X").setIsBuiltIn(false);
            when(themePresetMapper.selectById("CUSTOM_X")).thenReturn(custom);
            when(themeUserSettingMapper.clearPresetRefByPresetId("CUSTOM_X")).thenReturn(3);
            when(themePresetMapper.deleteById(eq("CUSTOM_X"))).thenReturn(1);

            themeService.deletePreset("CUSTOM_X");

            // 顺序：先解引用，再删除
            org.mockito.InOrder inOrder = Mockito.inOrder(themeUserSettingMapper, themePresetMapper);
            inOrder.verify(themeUserSettingMapper).clearPresetRefByPresetId("CUSTOM_X");
            inOrder.verify(themePresetMapper).deleteById(eq("CUSTOM_X"));
        }
    }

    // =========================================================================
    // 测试夹具构造方法
    // =========================================================================

    /**
     * 构造一个全字段合法的 {@link ThemeProfileDTO}，用于在测试中按需「打破」个别字段。
     */
    private static ThemeProfileDTO buildValidProfileDto() {
        return new ThemeProfileDTO()
                .setPrimaryColor("#165DFF")
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
                .setFontScale(FontScale.DEFAULT);
    }

    /**
     * 以指定主色构造一个最小可用的 {@link ThemeProfileJson}（其余字段使用默认稳态值）。
     */
    private static ThemeProfileJson buildBaselineJson(String primaryColor) {
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
                .setFontScale(FontScale.DEFAULT);
    }

    /**
     * 构造 {@code PRESET_BEST} 实体（{@code is_built_in=true}、{@code is_default=true}）。
     * <p>
     * 两个变体使用不同主色以便区分：
     * <ul>
     *     <li>ADMIN  → {@code #165DFF}</li>
     *     <li>BUSINESS → {@code #22A6F2}</li>
     * </ul>
     * </p>
     */
    private static ThemePreset buildPresetBest() {
        return new ThemePreset()
                .setId(ThemeConstants.PRESET_BEST_ID)
                .setName("最佳默认")
                .setDescription("系统内置最佳默认主题")
                .setIsBuiltIn(true)
                .setIsDefault(true)
                .setSortOrder(0)
                .setProfileAdmin(buildBaselineJson("#165DFF"))
                .setProfileBusiness(buildBaselineJson("#22A6F2"));
    }

    /**
     * 构造一个备选内置预设（海洋蓝），用于 {@code listPresets} 与 CRUD 用例。
     */
    private static ThemePreset buildOceanPreset() {
        return new ThemePreset()
                .setId(ThemeConstants.PRESET_OCEAN_ID)
                .setName("海洋蓝")
                .setDescription("备选内置预设")
                .setIsBuiltIn(true)
                .setIsDefault(false)
                .setSortOrder(1)
                .setProfileAdmin(buildBaselineJson("#1F6FEB"))
                .setProfileBusiness(buildBaselineJson("#2EA8E0"));
    }
}
