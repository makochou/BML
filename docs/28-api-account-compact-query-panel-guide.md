# API账号管理紧凑查询面板维护指南（Ultra版）

## 1. 文档目标

本文档用于说明 `API账号管理` 页面查询区的最新实现，重点是“超紧凑（Ultra）”卡片模式的接入方式与维护规范。

当前查询区采用以下工作模式：

1. 高频条件首屏常显，保持单屏快速筛选。
2. 低频条件按需展开，避免挤占主视区。
3. 模糊查找/精确查找与重置动作固定在底部右侧，减少光标移动距离。
4. 所有字段继续使用 schema 驱动，保障后续扩展可维护性。

## 2. 核心代码位置

- 页面接入：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 通用查询容器：`bml-frontend/src/components/governance/GovernanceCompactQueryPanel.vue`
- 通用分区渲染：`bml-frontend/src/components/governance/GovernanceFormSections.vue`
- 查询 schema：`bml-frontend/src/composables/useApiAccountQuerySchema.ts`
- 主次拆分工具：`bml-frontend/src/composables/useGovernanceSectionPriority.ts`

## 3. 本次改造要点

本次新增了可复用的密度能力：

1. `GovernanceCompactQueryPanel` 新增 `density` 属性：
   - `regular`：常规密度（默认）
   - `compact`：紧凑密度
   - `ultra`：超紧凑密度（本页面使用）
2. 页面通过 `density="ultra"` 开启“最紧凑”布局，无需改业务逻辑。
3. 容器关键尺寸已抽象为 CSS 变量，后续按页面差异可局部覆盖。

## 4. 页面接入示例（推荐）

```vue
<GovernanceCompactQueryPanel
  class="query-panel"
  max-width="1260px"
  density="ultra"
>
  <template v-if="querySecondarySections.length" #note>
    <a-button class="query-panel__toggle-btn" @click="toggleQueryAdvanced">
      {{ queryAdvancedToggleText }}
    </a-button>
  </template>

  <template #footerActions>
    <a-button type="primary" @click="handleTextModeSearch('fuzzy')">模糊查找</a-button>
    <a-button type="primary" @click="handleTextModeSearch('exact')">精确查找</a-button>
    <a-button @click="handleResetSearch">重置条件</a-button>
  </template>

  <a-form :model="queryForm" layout="vertical" class="query-form">
    <GovernanceFormSections
      :model="queryFormAsRecord"
      :sections="queryPrimarySections"
      variant="embedded"
    />
  </a-form>
</GovernanceCompactQueryPanel>
```

## 5. 字段拆分与布局规则

当前查询区采用“常显 3 条 + 其他展开”的结构：

1. 常显（`primary`）：
   - 账号名称
   - 业务系统
   - 状态
2. 折叠（`secondary`）：
   - 账号类型
   - 接入环境
   - 调用客户端
   - 账号ID
   - AccessKey
   - 系统编码
   - 系统名称
   - 负责人
   - 回调地址
   - 签名版本
   - 白名单IP
   - 负责人联系方式
   - 更新开始/结束时间
   - 创建开始/结束时间
   - 限流下限/上限
   - 到期开始/结束时间
   - 备注

说明：

1. 拆分逻辑由 `splitGovernanceSectionsByPriority` 统一处理。
2. 常显 3 项遵循行业高频检索顺序：先定位账号主体，再限定业务归属，再快速筛选启停状态。
3. 折叠区字段按运营检索频率分层排序：运营筛选 > 对象定位 > 治理排障 > 时间容量治理。
4. 网格列数会根据字段数量自动归一化，不需要手写模板列布局。

## 6. 维护步骤（新增字段）

新增查询字段时，按以下顺序执行：

1. 在 `queryForm` 增加响应式字段。
2. 在查询接口参数中增加同名入参映射。
3. 在 `useApiAccountQuerySchema.ts` 增加字段 schema。
4. 在后端 `SysApiAccountPageQuery` 中新增对应字段。
5. 在 `SysApiAccountService#buildAccountQueryWrapper` 中补充过滤条件。
6. 为字段设置 `priority`（高频用 `primary`，低频用 `secondary`）。
7. 在 `handleResetSearch` 中补充重置逻辑。
8. 检查 `queryAdvancedToggleText` 文案是否仍适配当前密度。

## 7. 超紧凑样式维护建议

1. 查询卡片外层留白建议保持在 `8~10px` 区间，不建议回升到 16px+。
2. 控件高度建议 `30px` 左右，确保一屏可见字段数量最大化。
3. 折叠区仅保留必要说明，不要加入大段提示文字。
4. 尽量通过容器变量调整，不在页面里复制一套新卡片样式。

## 8. 禁止项

1. 直接在页面模板中手写新的查询输入控件结构，绕开 schema。
2. 将全部低频字段重新放回首屏。
3. 在 `#footerActions` 区域堆叠 3 个以上按钮。
4. 对 `ultra` 模式单独新增一套组件，造成重复维护。

## 9. 回归验证清单

1. 执行 `npm run build`，确保编译通过。
2. 首屏确认主条件维持单行可扫读状态。
3. “更多条件/收起条件”切换动画与布局无抖动。
4. 模糊查找、精确查找、重置条件按钮在底部右侧对齐，且可点击。
5. 移动端下查询区按钮可换行，未出现遮挡。

## 10. 查询卡片上移/下移调优（推荐做法）

如果后续还要继续上移或下移查询卡片，优先调页面级变量，不要直接写死 `margin-top`：

```css
.page {
  --api-account-query-offset-y: -8px;
  --api-account-stage-gap-y: 8px;
}

.query-panel {
  margin-top: var(--api-account-query-offset-y);
}

.table-shell {
  margin-top: var(--api-account-stage-gap-y);
}
```

调优建议：

1. 查询卡片再上移：把 `--api-account-query-offset-y` 调小到 `-10px` 或 `-12px`。
2. 查询卡片下移：把 `--api-account-query-offset-y` 调大到 `-6px`、`-4px`、`0`。
3. 列表与查询区距离过近/过远：调 `--api-account-stage-gap-y`。
4. 移动端建议单独覆盖变量，避免紧贴顶部造成视觉拥挤。

## 11. 字符字段模糊/精准规则

当前页面的字符输入查询字段（如账号名称、系统名称、负责人、回调地址、备注、白名单IP等）统一受“右下角按钮组”控制：

1. `模糊查找`：点击后切换为 `fuzzy` 并立即执行查询，后端使用 `LIKE` 模糊匹配。
2. `精确查找`：点击后切换为 `exact` 并立即执行查询，后端使用 `=` 精准匹配（白名单IP使用 `FIND_IN_SET` 精确命中单条）。
3. 按钮状态映射到 `queryForm.textMatchMode`，是统一开关，能保证所有字符字段行为一致。
