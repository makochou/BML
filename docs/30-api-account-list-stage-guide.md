# API账号管理列表区维护指南（Ultra版）

## 1. 文档目标

本文档用于说明 `API账号管理` 列表区的最新实现方式，重点覆盖“列表容器超紧凑卡片 + 表格超紧凑行高”的维护标准。

本次改造目标：

1. 列表主区域保持极致紧凑，同时不牺牲可读性。
2. 高频操作持续可见，低频信息统一留在详情弹窗。
3. 通过通用容器参数化能力，保证后续页面可直接复用。

## 2. 核心代码位置

- 页面入口：`bml-frontend/src/views/api/ApiAccountManage.vue`
- 通用列表容器：`bml-frontend/src/components/governance/GovernanceListStage.vue`
- 详情弹窗：`bml-frontend/src/components/api-account/ApiAccountPreviewModal.vue`
- 列模型工具：`bml-frontend/src/composables/useTableColumns.ts`

## 3. 本次改造要点

1. `GovernanceListStage` 新增 `density` 属性：
   - `regular`：常规密度（默认）
   - `compact`：紧凑密度
   - `ultra`：超紧凑密度（本页面使用）
2. 页面接入改为 `density="ultra"`，且表格开启 `size="small"`。
3. 列表行内样式做紧凑化：
   - 行内间距、字体、标签高度、操作按钮尺寸统一下调
   - 头部与单元格 padding 收口到 8px
   - 固定列阴影减弱，避免视觉拥挤

## 4. 页面接入示例（推荐）

```vue
<GovernanceListStage
  class="table-shell"
  max-width="1260px"
  density="ultra"
>
  <template #actions>
    <a-button :loading="syncingRegistry" @click="handleSyncRegistry">同步接口目录</a-button>
    <a-button type="primary" @click="openCreateModal">新建账号</a-button>
  </template>

  <a-table
    class="account-table"
    size="small"
    row-key="id"
    :data="accountList"
    :loading="tableLoading"
    :pagination="paginationConfig"
  >
    <!-- columns -->
  </a-table>
</GovernanceListStage>
```

## 5. 列信息保持策略

当前列表继续维持 6 列，不新增重型信息列：

1. 账号信息
2. 业务系统 / 负责人
3. 接入概览
4. 状态 / 授权
5. 最近更新
6. 操作

维护原则：

1. 列表只承载“扫一眼就要判断”的高频字段。
2. 白名单明细、回调地址全文、备注、凭证明细等低频内容进入详情弹窗。
3. 危险操作继续保留确认弹窗，不可省略。

## 6. 行内交互规则

1. 双击行：打开账号详情弹窗。
2. 右侧按钮区：阻止双击冒泡，避免误触发详情。
3. 高频动作常显：`详情 / 编辑 / 授权 / 更多`。
4. `更多` 菜单承接低频动作：`回调日志 / 重置密钥 / 删除账号`。

## 7. 紧凑样式维护建议

1. 表格头与单元格 vertical padding 建议维持 `8px`。
2. 操作按钮高度建议不超过 `26px`，宽度维持可点击面积。
3. 标签高度建议 `20px` 左右，避免换行抖动。
4. 若后续新增列，优先压缩文案和摘要，不先放大行高。

## 8. 后续改版顺序

如需继续调整列表区，请按以下顺序：

1. 改容器壳层：优先改 `GovernanceListStage.vue`（全页可复用）。
2. 改列内容：改 `accountTableColumnModel` 与 `kind` 分支渲染。
3. 改详情内容：改 `ApiAccountPreviewModal` 相关 facts/stats 计算。
4. 改动作流程：复用现有统一方法，不新增重复逻辑分支。

## 9. 禁止项

1. 将列表改回重型卡片堆叠模式。
2. 在列表顶部重新堆叠大段说明文本。
3. 将低频治理字段塞回列表主列。
4. 新增一套与弹窗不一致的动作处理链路。

## 10. 回归验证清单

1. 执行 `npm run build`，确保编译通过。
2. 验证列表是否仍可在单屏内高密度展示。
3. 验证行双击详情、按钮点击、菜单动作全部正常。
4. 验证删除/重置密钥仍有二次确认。
5. 验证分页与筛选联动无回归。
