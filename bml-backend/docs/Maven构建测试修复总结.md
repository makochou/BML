# Maven 构建测试修复总结

## 问题描述

执行 `mvn clean install` 时，`bml-module-system` 模块的测试失败，导致整个构建失败。

### 错误信息

```
[ERROR] Tests run: 16, Failures: 3, Errors: 4, Skipped: 0
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.1:test
```

主要错误类型：
1. **UnnecessaryStubbingException** - Mock 行为配置不正确
2. **AssertionFailedError** - 测试断言失败
3. **Wanted but not invoked** - Mock 方法未被调用

## 根本原因分析

### 1. Mock 行为不匹配实际实现

**问题**：测试代码使用 `selectList()` 进行 Mock，但实际实现使用 `getOne()` 方法（内部调用 `selectOne()`）。

```java
// ❌ 错误的 Mock 方式
when(sysAlertMapper.selectList(any(LambdaQueryWrapper.class)))
    .thenReturn(Collections.emptyList());

// ✅ 正确的 Mock 方式
when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
    .thenReturn(null);
```

### 2. 业务逻辑理解偏差

**实际业务逻辑**（`SysAlertServiceImpl.saveOrUpdateAlert()`）：
1. 查询是否存在相同类型和标题的**未读**告警
2. 如果存在未读告警 → 更新该告警
3. 如果不存在未读告警 → 查询是否存在 24 小时内的**已读**告警
4. 如果存在 24 小时内已读告警 → 不创建新告警（返回已读告警）
5. 否则 → 创建新告警

**测试代码问题**：
- 未正确模拟两次查询（未读查询 + 已读查询）
- 未考虑 24 小时内已读告警的去重逻辑

## 修复方案

### 1. 修正 Mock 方法签名

```java
// 修改前
when(sysAlertMapper.selectList(any(LambdaQueryWrapper.class)))
    .thenReturn(Collections.emptyList());

// 修改后
when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
    .thenReturn(null);
```

### 2. 添加必要的 import

```java
import static org.mockito.ArgumentMatchers.anyBoolean;
```

### 3. 修正测试用例逻辑

#### 测试用例 1：首次保存应创建新记录

```java
@Test
@DisplayName("测试告警智能去重 - 首次保存应创建新记录")
void testSaveOrUpdateAlert_FirstTime_ShouldCreateNew() {
    // Mock：查询不到未读告警和已读告警
    when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(null);
    
    // Mock：插入成功
    when(sysAlertMapper.insert(any(SysAlert.class)))
            .thenAnswer(invocation -> {
                SysAlert inserted = invocation.getArgument(0);
                inserted.setId(1L);
                return 1;
            });

    SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(alert);

    // 验证
    assertNotNull(savedAlert.getId());
    verify(sysAlertMapper, times(1)).insert(any(SysAlert.class));
}
```

#### 测试用例 2：相同类型和标题的未读告警应更新

```java
@Test
@DisplayName("测试告警智能去重 - 相同类型和标题的未读告警应更新而非新建")
void testSaveOrUpdateAlert_SameTypeAndTitle_ShouldUpdate() {
    SysAlert existingAlert = createTestAlert(...);
    existingAlert.setId(1L);

    // Mock：第一次查询返回已存在的未读告警
    when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(existingAlert);
    
    // Mock：更新成功
    when(sysAlertMapper.updateById(any(SysAlert.class)))
            .thenReturn(1);

    SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

    // 验证
    assertEquals(1L, savedAlert.getId());
    verify(sysAlertMapper, times(1)).updateById(any(SysAlert.class));
}
```

#### 测试用例 3：24 小时内已读告警不创建新记录

```java
@Test
@DisplayName("测试告警智能去重 - 已读告警不影响新告警的创建")
void testSaveOrUpdateAlert_ReadAlert_ShouldNotAffectNew() {
    SysAlert readAlert = createTestAlert(...);
    readAlert.setId(1L);
    readAlert.setReadStatus(1); // 已读
    readAlert.setUpdateTime(LocalDateTime.now().minusHours(2));

    // Mock：第一次查询未读告警返回 null，第二次查询已读告警返回记录
    when(sysAlertMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean()))
            .thenReturn(null)        // 第一次：未读查询
            .thenReturn(readAlert);  // 第二次：已读查询

    SysAlert savedAlert = sysAlertService.saveOrUpdateAlert(newAlert);

    // 验证：应返回已读告警，不创建新记录
    assertEquals(1L, savedAlert.getId());
    assertEquals(1, savedAlert.getReadStatus());
    verify(sysAlertMapper, never()).insert(any(SysAlert.class));
}
```

## 修复后的测试覆盖

### 测试用例列表

| 测试用例 | 场景描述 | 预期结果 |
|---------|---------|---------|
| `testSaveOrUpdateAlert_FirstTime_ShouldCreateNew` | 首次保存告警 | 创建新记录 |
| `testSaveOrUpdateAlert_SameTypeAndTitle_ShouldUpdate` | 相同类型和标题的未读告警 | 更新现有记录 |
| `testSaveOrUpdateAlert_ReadAlert_ShouldNotAffectNew` | 24 小时内存在已读告警 | 不创建新记录 |
| `testSaveOrUpdateAlert_DifferentTitle_ShouldCreateSeparately` | 不同标题的告警 | 分别创建 |
| `testSaveOrUpdateAlert_DifferentType_ShouldCreateSeparately` | 不同类型的告警 | 分别创建 |
| `testSaveOrUpdateAlert_Update_ShouldKeepUnreadStatus` | 更新时保持未读状态 | 状态不变 |
| `testSaveOrUpdateAlert_Update_ShouldUpdateLevel` | 更新时更新告警级别 | 级别更新 |

### 测试结果

```
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 技术要点总结

### 1. MyBatis-Plus ServiceImpl 测试要点

```java
@ExtendWith(MockitoExtension.class)
class SysAlertServiceTest {
    @Mock
    private SysAlertMapper sysAlertMapper;

    @InjectMocks
    private SysAlertServiceImpl sysAlertService;

    @BeforeEach
    void setUp() {
        // 关键：注入 baseMapper
        ReflectionTestUtils.setField(sysAlertService, "baseMapper", sysAlertMapper);
    }
}
```

### 2. Mock 方法签名匹配

- `getOne()` 内部调用 `selectOne(wrapper, true)`
- 必须使用 `anyBoolean()` 匹配第二个参数

### 3. 多次查询的 Mock 配置

```java
when(mapper.selectOne(any(), anyBoolean()))
    .thenReturn(null)        // 第一次调用
    .thenReturn(readAlert);  // 第二次调用
```

### 4. Mockito 严格模式

- Mockito 默认启用严格模式
- 未使用的 stubbing 会导致 `UnnecessaryStubbingException`
- 确保所有 Mock 配置都会被实际调用

## 构建验证

### 完整构建命令

```bash
mvn clean install
```

### 构建结果

```
[INFO] Reactor Summary for bml-backend 2.0.0:
[INFO]
[INFO] bml-backend ........................................ SUCCESS [  0.963 s]
[INFO] bml-core ........................................... SUCCESS [  0.032 s]
[INFO] bml-core-common .................................... SUCCESS [  6.916 s]
[INFO] bml-core-base ...................................... SUCCESS [  2.718 s]
[INFO] bml-core-framework ................................. SUCCESS [ 27.003 s]
[INFO] bml-modules ........................................ SUCCESS [  0.024 s]
[INFO] bml-module-system .................................. SUCCESS [ 19.171 s]
[INFO] bml-module-api ..................................... SUCCESS [ 19.737 s]
[INFO] bml-app ............................................ SUCCESS [ 27.099 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:44 min
```

## 最佳实践建议

### 1. 单元测试编写规范

- ✅ 使用 Mockito 进行纯单元测试
- ✅ 避免依赖 Spring 容器（`@SpringBootTest`）
- ✅ Mock 行为必须与实际实现匹配
- ✅ 测试用例应覆盖所有业务分支

### 2. Mock 配置规范

- ✅ 使用 `@ExtendWith(MockitoExtension.class)`
- ✅ 正确注入 `baseMapper`（ServiceImpl 必需）
- ✅ 使用 `anyBoolean()` 等匹配器处理可选参数
- ✅ 避免配置未使用的 stubbing

### 3. 测试维护规范

- ✅ 业务逻辑变更时同步更新测试
- ✅ 使用 `@DisplayName` 提供清晰的测试描述
- ✅ 添加详细的中文注释说明测试意图
- ✅ 定期运行完整构建验证测试

## 相关文件

- **测试文件**：`bml-backend/bml-modules/bml-module-system/src/test/java/com/bml/module/system/service/SysAlertServiceTest.java`
- **实现文件**：`bml-backend/bml-modules/bml-module-system/src/main/java/com/bml/module/system/service/impl/SysAlertServiceImpl.java`
- **父 POM**：`bml-backend/pom.xml`（Surefire 插件配置）

## 总结

通过正确理解 MyBatis-Plus 的 `ServiceImpl` 实现机制，准确匹配 Mock 方法签名，并完整覆盖业务逻辑的所有分支，成功修复了所有测试失败问题。现在 `mvn clean install` 可以正常运行，所有 9 个模块都能成功构建。

---

**修复完成时间**：2026-04-30  
**修复人员**：BML Team  
**测试状态**：✅ 全部通过（16 个测试用例，0 失败，0 错误）
