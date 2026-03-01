# API账号管理查询区精简设计说明

## 1. 调整目标

`API账号管理` 页面的查询区当前已经收敛为极简版工作区。

当前设计目标如下：

1. 首屏只放高频查询条件。
2. 左下角只保留一个“展开更多查询条件”入口。
3. 右下角只保留“查询账号”和“重置条件”两个按钮。
4. 不再叠加额外说明、标题或状态徽标。

## 2. 当前实现位置

- 页面文件：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 查询面板组件：`bml-frontend/src/components/governance/GovernanceCompactQueryPanel.vue`
- 通用栅格组件：`bml-frontend/src/components/governance/GovernanceFormSections.vue`
- 查询字段配置：`bml-frontend/src/composables/useApiAccountQuerySchema.ts`
- 主次拆分工具：`bml-frontend/src/composables/useGovernanceSectionPriority.ts`

## 3. 当前结构说明

当前查询区由三部分组成：

1. 常显查询字段区。
2. 按需展开的扩展字段区。
3. 底部操作区。

底部操作区当前规则如下：

1. 左侧：展开更多查询条件。
2. 右侧：查询账号、重置条件。

## 4. 当前视觉原则

1. 查询区保持单张大圆角卡片。
2. 扩展条件区使用轻量虚线层区分。
3. 底部按钮布局尽量简洁，不再叠加多余内容。
4. 主按钮继续使用蓝绿渐变，重置按钮保持浅色胶囊样式。

## 5. 新增查询字段的标准做法

1. 在 `queryForm` 中新增字段。
2. 在查询接口参数中补充对应入参。
3. 在 `useApiAccountQuerySchema.ts` 中新增字段 schema。
4. 根据业务频率决定字段 `priority`。
5. 在 `handleResetSearch` 中补充重置逻辑。

## 6. 不建议的做法

1. 恢复额外的标题、列表说明或状态徽标。
2. 在底部操作区继续增加刷新、新建等按钮。
3. 直接在页面模板中手写新的查询结构。

## 7. 验证方式

1. 执行 `npm run build`。
2. 检查左下角是否为展开更多查询条件。
3. 检查右下角是否只保留查询和重置。
4. 检查扩展条件展开后是否正常。
5. 检查移动端是否正常换行。
