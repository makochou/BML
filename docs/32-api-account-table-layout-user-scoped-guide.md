# API账号管理列表列宽按账号持久化维护指南

## 1. 目标与范围

本指南用于说明 `API账号管理` 页面“列设置”能力的标准实现，覆盖以下目标：

1. 列宽（列间距）、列顺序、列显隐均按当前登录账号隔离存储。
2. 页面刷新后保留当前账号的个性化布局。
3. 点击“恢复默认”后，列宽/顺序/显隐统一回退到默认值，并立即持久化。
4. 方案可复用到其他列表页面，避免重复造轮子。

相关代码：

- 页面：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 鉴权工具：`bml-frontend/src/utils/auth.ts`
- 用户作用域存储工具：`bml-frontend/src/utils/userScopedStorage.ts`
- 单测：`bml-frontend/src/utils/auth.test.ts`、`bml-frontend/src/utils/userScopedStorage.test.ts`

## 2. 关键设计

### 2.1 按用户隔离 key 统一收口

通过 `buildUserScopedStorageKey(prefix)` 统一拼装 `localStorage` key：

1. 自动获取当前登录用户标识。
2. 标识统一 `trim + lower-case`，避免大小写差异导致重复配置。
3. 兜底匿名标识，保证 key 结构稳定。

示例 key：

```text
bml.api-account.manage.table-layout.v4:admin
```

### 2.2 用户标识写入策略修复

`setAuthTokens` 增加统一策略：

1. 登录时显式传入 `userIdentity`，直接写入。
2. token 刷新时如果未传入 `userIdentity`，优先保留已有标识，不覆盖。
3. 仅当本地缺失标识时，才尝试从 JWT 解析并写入。

这样可以避免“刷新 token 后用户标识被改写，导致列宽配置读取错 key”的问题。

### 2.3 恢复默认行为

`resetAccountTableColumnLayout` 执行流程：

1. 重建默认列布局（默认宽度、默认顺序、默认显隐）。
2. 归一化顺序并写回当前账号作用域 key。
3. 前端提示“列宽、顺序与显示列已恢复默认”。

## 3. 页面改造说明

`ApiAccountManage.vue` 中仅保留业务语义：

1. 定义页面级 key 前缀：`bml.api-account.manage.table-layout.v4`。
2. 通过 `buildUserScopedStorageKey` 获取当前账号的完整 key。
3. 复用现有 `persist/restore/reset` 流程，不改动交互入口。

## 4. 单元测试说明

### 4.1 `auth.test.ts`

新增验证点：

1. 登录写入显式身份后，token 刷新不会覆盖身份。
2. 本地无身份时可从 JWT 解析并写入身份。

### 4.2 `userScopedStorage.test.ts`

新增验证点：

1. 身份归一化规则正确。
2. 支持显式身份构建 key。
3. 未显式传入时，自动读取当前登录身份构建 key。

## 5. 回归验证清单

1. 使用账号 A 登录，调整列表列宽，刷新后检查布局保持不变。
2. 切换账号 B 登录，验证读取的是账号 B 的独立布局，不受账号 A 影响。
3. 在账号 B 下点击“恢复默认”，验证所有列宽回到默认值并刷新后仍保持默认。
4. 返回账号 A，验证账号 A 的历史布局仍存在。

## 6. 复用到其他页面的步骤

1. 在目标页面定义 `XXX_LAYOUT_STORAGE_KEY_PREFIX`（带版本号）。
2. 使用 `buildUserScopedStorageKey(prefix)` 获取存储 key。
3. 在列宽调整、列排序、列显隐变更后统一调用 `persist`。
4. 在“恢复默认”入口调用默认布局工厂函数并再次 `persist`。
5. 新页面补齐最少 2 类单测：身份稳定性、key 构建正确性。
