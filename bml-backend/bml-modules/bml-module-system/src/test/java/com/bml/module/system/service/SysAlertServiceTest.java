package com.bml.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bml.module.system.entity.SysAlert;
import com.bml.module.system.mapper.SysAlertMapper;
import com.bml.module.system.service.impl.SysAlertServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

/**
 * 系统告警服务测试类
 * <p>
 * 重点测试告警智能去重功能，确保相同类型和标题的告警不会重复创建。
 * 使用 Mockito 进行单元测试，不依赖 Spring 容器和数据库。
 * </p>
 *
 * @author BML Team
 */
@ExtendWith(MockitoExtension.class)
class SysAlertServiceTest {

    @Mock
    private SysAlertMapper sysAlertMapper;

    @InjectMocks
    private SysAlertServiceImpl sysAlertService;

    /**
     * 创建测试用的告警对象
     */
    private SysAlert createTestAlert(String type, String title, String content, Integer readStatus) {
        SysAlert alert = new SysAlert();
        alert.setAlertType(type);
        alert.setAlertLevel("info");
        alert.setAlertTitle(title);
        alert.setAlertContent(content);
        alert.setReadStatus(readStatus);
        return alert;
    }

    @BeforeEach
    void setUp() {
        // 每个测试前重置 Mock 对象
        reset(sysAlertMapper);
        // 注入 baseMapper 到 ServiceImpl（MyBatis-Plus 需要）
        ReflectionTestUtils.setField(sysAlertService, "baseMapper", sysAlertMapper);
    }

    @Test
    @DisplayName("测试告警智能去重 - 首次保存应创建新记录")
    void testSaveOrUpdateAlert_FirstTime_ShouldCreateNew() {
        // 准备测试数据
        SysAlert alert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级", 
                "许可证更新：最大 API 账号数从 5 变更为 10。", 0);

        // Mock 行为：查询不到已存在的未读告警（selectOne 返回 null）
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(null);
        
        // Mock 行为：插入成功，返回 1
        when(sysAlertMapper.insert(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert inserted = invocation.getArgument(0);
                    inserted.setId(1L);
                    inserted.setCreateTime(LocalDateTime.now());
                    inserted.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(alert);

        // 验证结果
        assertNotNull(savedAlert.getId(), "告警 ID 不应为空");
        assertEquals("LICENSE_CHANGE", savedAlert.getAlertType());
        assertEquals("API 账号上限升级", savedAlert.getAlertTitle());
        assertEquals(0, savedAlert.getReadStatus(), "新告警应为未读状态");
        
        // 验证调用了插入方法
        verify(sysAlertMapper, times(1)).insert(any(SysAlert.class));
        verify(sysAlertMapper, never()).updateById(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 相同类型和标题的未读告警应更新而非新建")
    void testSaveOrUpdateAlert_SameTypeAndTitle_ShouldUpdate() {
        // 准备已存在的告警
        SysAlert existingAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 5 变更为 10。", 0);
        existingAlert.setId(1L);
        existingAlert.setCreateTime(LocalDateTime.now().minusHours(1));
        existingAlert.setUpdateTime(LocalDateTime.now().minusHours(1));

        // 准备新的告警
        SysAlert newAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 10 变更为 15。", 0);

        // Mock 行为：第一次查询未读告警时返回已存在的告警
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(existingAlert);
        
        // Mock 行为：更新成功，返回 1
        when(sysAlertMapper.updateById(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert updated = invocation.getArgument(0);
                    updated.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：应该是更新同一条记录
        assertEquals(1L, savedAlert.getId(), "应该更新同一条记录，ID 应相同");
        assertEquals("许可证更新：最大 API 账号数从 10 变更为 15。", savedAlert.getAlertContent(),
                "告警内容应更新为最新值");
        
        // 验证调用了更新方法而不是插入方法
        verify(sysAlertMapper, never()).insert(any(SysAlert.class));
        verify(sysAlertMapper, times(1)).updateById(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 已读告警不影响新告警的创建")
    void testSaveOrUpdateAlert_ReadAlert_ShouldNotAffectNew() {
        // 准备新的告警
        SysAlert newAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 10 变更为 15。", 0);

        // 准备已读告警（24小时内）
        SysAlert readAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 5 变更为 10。", 1);
        readAlert.setId(1L);
        readAlert.setUpdateTime(LocalDateTime.now().minusHours(2));

        // Mock 行为：第一次查询未读告警返回 null，第二次查询已读告警返回已读记录
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(null)  // 第一次：查询未读告警，返回 null
                .thenReturn(readAlert);  // 第二次：查询24小时内已读告警，返回已读记录

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：应该返回已读告警（不创建新记录）
        assertEquals(1L, savedAlert.getId(), "应该返回已读告警，不创建新记录");
        assertEquals(1, savedAlert.getReadStatus(), "返回的应该是已读告警");
        
        // 验证没有调用插入或更新方法
        verify(sysAlertMapper, never()).insert(any(SysAlert.class));
        verify(sysAlertMapper, never()).updateById(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 不同标题的告警应分别创建")
    void testSaveOrUpdateAlert_DifferentTitle_ShouldCreateSeparately() {
        // 准备新的告警（不同标题）
        SysAlert newAlert = createTestAlert("LICENSE_CHANGE", "业务用户上限升级",
                "许可证更新：业务用户上限从 100 变更为 200。", 0);

        // Mock 行为：查询不到相同类型和标题的未读告警和已读告警
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(null);
        
        // Mock 行为：插入成功
        when(sysAlertMapper.insert(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert inserted = invocation.getArgument(0);
                    inserted.setId(2L);
                    inserted.setCreateTime(LocalDateTime.now());
                    inserted.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：应该创建新记录
        assertNotNull(savedAlert.getId(), "不同标题的告警应分别创建");
        assertEquals("业务用户上限升级", savedAlert.getAlertTitle());
        
        // 验证调用了插入方法
        verify(sysAlertMapper, times(1)).insert(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 不同类型的告警应分别创建")
    void testSaveOrUpdateAlert_DifferentType_ShouldCreateSeparately() {
        // 准备新的告警（不同类型）
        SysAlert newAlert = createTestAlert("CPU_HIGH", "配额升级",
                "CPU 使用率过高。", 0);

        // Mock 行为：查询不到相同类型和标题的未读告警和已读告警
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(null);
        
        // Mock 行为：插入成功
        when(sysAlertMapper.insert(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert inserted = invocation.getArgument(0);
                    inserted.setId(2L);
                    inserted.setCreateTime(LocalDateTime.now());
                    inserted.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：应该创建新记录
        assertNotNull(savedAlert.getId(), "不同类型的告警应分别创建");
        assertEquals("CPU_HIGH", savedAlert.getAlertType());
        
        // 验证调用了插入方法
        verify(sysAlertMapper, times(1)).insert(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 更新时应保持未读状态")
    void testSaveOrUpdateAlert_Update_ShouldKeepUnreadStatus() {
        // 准备已存在的告警
        SysAlert existingAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 5 变更为 10。", 0);
        existingAlert.setId(1L);
        existingAlert.setCreateTime(LocalDateTime.now().minusHours(1));
        existingAlert.setUpdateTime(LocalDateTime.now().minusHours(1));

        // 准备新的告警
        SysAlert newAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限升级",
                "许可证更新：最大 API 账号数从 10 变更为 15。", 0);

        // Mock 行为：查询到已存在的未读告警
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(existingAlert);
        
        // Mock 行为：更新成功
        when(sysAlertMapper.updateById(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert updated = invocation.getArgument(0);
                    updated.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：更新后应保持未读状态
        assertEquals(0, savedAlert.getReadStatus(), "更新后应保持未读状态");
        
        // 验证调用了更新方法
        verify(sysAlertMapper, times(1)).updateById(any(SysAlert.class));
    }

    @Test
    @DisplayName("测试告警智能去重 - 更新时应更新告警级别")
    void testSaveOrUpdateAlert_Update_ShouldUpdateLevel() {
        // 准备已存在的告警
        SysAlert existingAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限变更",
                "许可证更新：最大 API 账号数从 5 变更为 10。", 0);
        existingAlert.setId(1L);
        existingAlert.setAlertLevel("info");
        existingAlert.setCreateTime(LocalDateTime.now().minusHours(1));
        existingAlert.setUpdateTime(LocalDateTime.now().minusHours(1));

        // 准备新的告警（级别不同）
        SysAlert newAlert = createTestAlert("LICENSE_CHANGE", "API 账号上限变更",
                "许可证更新：最大 API 账号数从 10 变更为 3（降级）。", 0);
        newAlert.setAlertLevel("warning");

        // Mock 行为：查询到已存在的未读告警
        when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
                .thenReturn(existingAlert);
        
        // Mock 行为：更新成功
        when(sysAlertMapper.updateById(any(SysAlert.class)))
                .thenAnswer(invocation -> {
                    SysAlert updated = invocation.getArgument(0);
                    updated.setUpdateTime(LocalDateTime.now());
                    return 1;
                });

        // 执行保存
        SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

        // 验证结果：告警级别应更新
        assertEquals("warning", savedAlert.getAlertLevel(), "告警级别应更新为最新值");
        
        // 验证调用了更新方法
        verify(sysAlertMapper, times(1)).updateById(any(SysAlert.class));
    }
}
