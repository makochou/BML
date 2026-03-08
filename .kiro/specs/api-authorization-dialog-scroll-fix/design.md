# API 授权弹窗滚动修复设计文档

## Overview

本次修复针对 API 授权配置抽屉（ApiAuthorizationWorkbenchDrawer）的滚动行为问题。当前整个抽屉界面会出现滚动条，导致用户在浏览授权树时需要滚动整个页面，顶部筛选工具栏和侧边栏信息会被滚动到视口外。

修复策略是通过调整 CSS 布局，使抽屉整体保持固定高度，仅在"授权树"卡片区域（`.authorization-tree-shell`）内进行局部滚动。这样可以保持筛选工具栏和侧边栏信息始终可见，提供更好的交互体验。

## Glossary

- **Bug_Condition (C)**: 当授权树内容超出可视区域时，整个抽屉出现滚动条的条件
- **Property (P)**: 修复后应实现的行为 - 抽屉整体固定高度，仅授权树区域内滚动
- **Preservation**: 所有现有功能（勾选、筛选、批量操作、保存等）必须保持不变
- **ApiAuthorizationWorkbenchDrawer**: 位于 `bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue` 的授权配置抽屉组件
- **GovernanceWorkbenchShell**: 位于 `bml-frontend/src/components/governance/GovernanceWorkbenchShell.vue` 的工作台布局容器组件
- **authorization-panel--tree**: 包含筛选工具栏和授权树的主要内容区域
- **authorization-tree-shell**: 授权树的滚动容器，应该是唯一的滚动区域

## Bug Details

### Bug Condition

当用户打开授权配置抽屉且授权树中的接口列表内容较多时，整个抽屉界面会出现垂直滚动条。用户滚动时，顶部的筛选工具栏和侧边栏的账号画像、授权数据概览等信息会被滚动到视口外，影响使用体验。

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type { treeContentHeight: number, viewportHeight: number }
  OUTPUT: boolean
  
  RETURN input.treeContentHeight > input.viewportHeight
         AND drawerHasScrollbar()
         AND NOT treeShellHasIsolatedScroll()
END FUNCTION
```

### Examples

- **场景 1**: 用户打开授权抽屉，授权树包含 50+ 个接口项，抽屉整体出现滚动条，用户向下滚动查看底部接口时，顶部的搜索框和模块筛选器被滚动到视口外
- **场景 2**: 用户在授权树中展开多个模块和控制器，内容高度超过 1000px，整个抽屉需要滚动，侧边栏的账号画像信息也被滚动到视口外
- **场景 3**: 用户使用筛选功能后，授权树仍然很长，用户需要滚动整个抽屉才能看到底部的接口，同时失去了对筛选工具栏的快速访问
- **边缘情况**: 授权树内容很少（如只有 3 个接口），此时不应出现任何滚动条，所有内容应正常显示

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 用户在授权树中勾选或取消勾选接口时，系统应继续正常更新选中状态和统计数据
- 用户使用筛选工具栏（搜索、模块、方法）过滤接口时，授权树应继续正常显示过滤后的结果
- 用户点击"全选当前视图"或"清空当前视图"按钮时，系统应继续正常执行批量选择操作
- 用户点击"保存授权"按钮时，系统应继续正常保存授权配置并关闭抽屉
- 用户展开或折叠授权树的模块和控制器节点时，树形结构应继续正常展开和折叠
- 侧边栏显示账号画像和授权数据概览时，这些信息应继续正常显示且数据准确
- 抽屉在不同屏幕尺寸下显示时，响应式布局应继续正常工作

**Scope:**
所有不涉及滚动行为的交互和功能应完全不受影响。这包括：
- 所有按钮点击事件（保存、取消、全选、清空等）
- 表单输入和筛选逻辑
- 树形组件的展开/折叠和勾选逻辑
- 数据加载和状态更新
- 响应式布局和样式（除滚动相关样式外）

## Hypothesized Root Cause

基于代码分析，问题的根本原因是：

1. **缺少高度限制**: `.authorization-panel--tree` 区域使用了 `grow` 类，但没有配合 `min-height: 0` 和明确的 flex 布局约束，导致其高度随内容增长而无限扩展

2. **滚动容器层级错误**: 当前 `.authorization-tree-shell` 虽然设置了 `overflow-y: auto`，但由于父容器没有高度限制，滚动条实际出现在更外层的抽屉容器上

3. **Flex 布局链断裂**: 从 `.arco-drawer-body` 到 `.authorization-workbench` 再到 `.governance-workbench__main` 的 flex 布局链中，`.authorization-panel--tree` 没有正确参与 flex 收缩，导致其高度不受约束

4. **现有样式冲突**: 代码中已经有一些尝试修复的样式（如 `:deep(.authorization-workbench .governance-workbench__main)` 设置了 `height: 100%`），但这些样式与实际需要的 flex 布局约束不完全匹配

## Correctness Properties

Property 1: Bug Condition - 授权树区域局部滚动

_For any_ 授权树内容高度超出可视区域的情况，修复后的组件 SHALL 仅在 `.authorization-tree-shell` 容器内显示滚动条并进行滚动，而不是在整个抽屉上滚动，同时筛选工具栏和侧边栏信息应保持固定可见。

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - 所有现有功能不变

_For any_ 用户交互（勾选、筛选、批量操作、保存、展开/折叠等），修复后的组件 SHALL 产生与原始组件完全相同的功能行为，保持所有业务逻辑、数据更新和响应式布局不变。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

## Fix Implementation

### Changes Required

修复方案仅涉及 CSS 样式调整，不需要修改任何 JavaScript 逻辑或 HTML 结构。

**File**: `bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue`

**Specific Changes**:

1. **确保 .authorization-panel--tree 参与 flex 收缩**:
   - 为 `.authorization-panel--tree` 添加 `display: flex` 和 `flex-direction: column`
   - 添加 `min-height: 0` 以允许 flex 子元素收缩
   - 保持 `flex: 1` 或添加以确保其占据剩余空间

2. **确保 .authorization-tree-shell 成为滚动容器**:
   - 确认 `.authorization-tree-shell` 已设置 `overflow-y: auto`（当前已有）
   - 确保其设置了 `flex: 1` 以占据父容器的剩余空间
   - 确保其设置了 `min-height: 0` 以允许收缩

3. **修复 .authorization-toolbar 布局**:
   - 确保 `.authorization-toolbar` 不参与 flex 收缩（使用 `flex-shrink: 0`）
   - 保持其固定在顶部，不随授权树滚动

4. **修复 .section-heading 布局**:
   - 确保 `.section-heading` 不参与 flex 收缩（使用 `flex-shrink: 0`）
   - 保持其固定在授权树上方，不随授权树滚动

5. **验证父容器 flex 链**:
   - 确认 `.governance-workbench__main` 的 flex 布局设置正确
   - 确认 `.authorization-workbench` 的 flex 布局设置正确
   - 确保整个 flex 链从抽屉容器到授权树容器都正确配置

### 具体 CSS 修改

在 `<style scoped>` 部分添加或修改以下样式：

```css
/* 确保 .authorization-panel--tree 参与 flex 布局并允许收缩 */
.authorization-panel--tree {
  display: flex;
  flex-direction: column;
  min-height: 0; /* 关键：允许 flex 子元素收缩 */
  flex: 1; /* 占据剩余空间 */
}

/* 确保工具栏和标题不参与收缩，保持固定 */
.authorization-toolbar,
.section-heading {
  flex-shrink: 0;
}

/* 确保授权树容器成为滚动区域 */
.authorization-tree-shell {
  flex: 1; /* 占据剩余空间 */
  min-height: 0; /* 关键：允许收缩 */
  overflow-y: auto; /* 已存在，确保保留 */
}
```

## Testing Strategy

### Validation Approach

测试策略采用两阶段方法：首先在未修复的代码上观察和记录当前的滚动行为和功能行为，然后在修复后验证滚动行为已改变但所有功能行为保持不变。

### Exploratory Bug Condition Checking

**Goal**: 在实施修复之前，在未修复的代码上演示 bug。确认或反驳根本原因分析。如果反驳，需要重新假设。

**Test Plan**: 在浏览器中打开授权配置抽屉，使用开发者工具检查元素和样式，观察滚动条出现的位置和滚动行为。测试不同内容量的授权树，记录滚动行为。在未修复的代码上运行这些测试以观察失败并理解根本原因。

**Test Cases**:
1. **长授权树测试**: 打开包含 50+ 接口的授权抽屉，观察滚动条出现在抽屉整体而非授权树区域（将在未修复代码上失败）
2. **工具栏可见性测试**: 向下滚动查看授权树底部，观察顶部筛选工具栏被滚动到视口外（将在未修复代码上失败）
3. **侧边栏可见性测试**: 向下滚动查看授权树底部，观察侧边栏的账号画像被滚动到视口外（将在未修复代码上失败）
4. **短授权树测试**: 打开只包含 3 个接口的授权抽屉，观察是否出现不必要的滚动条（可能在未修复代码上失败）

**Expected Counterexamples**:
- 滚动条出现在 `.arco-drawer-body` 或更外层容器上，而不是 `.authorization-tree-shell` 上
- 可能原因：`.authorization-panel--tree` 缺少 `min-height: 0`，flex 布局链断裂，滚动容器层级错误

### Fix Checking

**Goal**: 验证对于所有 bug 条件成立的输入，修复后的组件产生期望的行为。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := renderDrawer_fixed(input)
  ASSERT scrollbarAppearsOnlyInTreeShell(result)
  ASSERT toolbarRemainsVisible(result)
  ASSERT sidebarRemainsVisible(result)
END FOR
```

### Preservation Checking

**Goal**: 验证对于所有 bug 条件不成立的输入，修复后的组件产生与原始组件相同的结果。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT renderDrawer_original(input) = renderDrawer_fixed(input)
END FOR
```

**Testing Approach**: 手动测试和视觉回归测试相结合，因为：
- 滚动行为是视觉和交互层面的，难以用单元测试覆盖
- 需要在真实浏览器环境中验证 CSS 布局效果
- 需要测试不同屏幕尺寸和内容量的组合

**Test Plan**: 在未修复代码上观察所有功能行为（勾选、筛选、批量操作等），记录预期行为，然后在修复后验证这些行为完全不变。

**Test Cases**:
1. **勾选功能保持**: 在未修复代码上观察勾选接口的行为，修复后验证勾选功能完全相同
2. **筛选功能保持**: 在未修复代码上观察使用搜索和筛选器的行为，修复后验证筛选功能完全相同
3. **批量操作保持**: 在未修复代码上观察"全选当前视图"和"清空当前视图"的行为，修复后验证批量操作完全相同
4. **保存功能保持**: 在未修复代码上观察保存授权的行为，修复后验证保存功能完全相同
5. **展开/折叠保持**: 在未修复代码上观察树节点展开和折叠的行为，修复后验证展开/折叠功能完全相同
6. **响应式布局保持**: 在未修复代码上观察不同屏幕尺寸下的布局，修复后验证响应式布局完全相同（除滚动行为外）

### Unit Tests

由于本次修复仅涉及 CSS 样式调整，不涉及 JavaScript 逻辑变更，因此不需要编写传统的单元测试。验证将主要通过以下方式进行：

- 在浏览器中手动测试滚动行为
- 使用开发者工具检查元素和计算样式
- 测试不同内容量和屏幕尺寸的组合
- 验证所有交互功能保持不变

### Property-Based Tests

对于 CSS 布局修复，属性测试不太适用。验证将通过：

- 视觉回归测试：截图对比修复前后的布局（除滚动条位置外应完全相同）
- 交互测试：在多种场景下测试所有功能，确保行为一致
- 跨浏览器测试：在 Chrome、Firefox、Safari 等浏览器中验证布局效果

### Integration Tests

- **完整流程测试**: 打开授权抽屉 → 使用筛选器 → 滚动授权树 → 勾选接口 → 批量操作 → 保存授权，验证整个流程中滚动行为正确且所有功能正常
- **响应式测试**: 在不同屏幕尺寸（1920px、1440px、1280px、768px）下测试抽屉布局和滚动行为
- **内容量测试**: 测试不同数量的接口（3 个、20 个、50 个、100+ 个）下的滚动行为和性能
- **边缘情况测试**: 测试空授权树、单个接口、全部展开、全部折叠等边缘情况
