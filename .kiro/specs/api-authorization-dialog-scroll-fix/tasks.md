# Implementation Plan

- [x] 1. 编写 bug condition 探索性测试
  - **Property 1: Bug Condition** - 授权树区域局部滚动
  - **重要**: 在未修复的代码上运行此测试 - 测试将失败（这是预期的，确认 bug 存在）
  - **不要尝试修复测试或代码**
  - **注意**: 此测试编码了期望行为 - 在实施修复后通过时将验证修复
  - **目标**: 展示 bug 存在的具体表现，理解根本原因
  - **测试方法**: 手动浏览器测试 + 开发者工具检查
  - 在浏览器中打开授权配置抽屉（ApiAuthorizationWorkbenchDrawer）
  - 测试场景 1：打开包含 50+ 接口的授权抽屉，使用开发者工具检查滚动条出现的位置
  - 测试场景 2：向下滚动查看授权树底部，观察顶部筛选工具栏是否被滚动到视口外
  - 测试场景 3：向下滚动查看授权树底部，观察侧边栏的账号画像是否被滚动到视口外
  - 测试场景 4：打开只包含 3 个接口的授权抽屉，观察是否出现不必要的滚动条
  - 使用开发者工具检查 `.authorization-panel--tree`、`.authorization-tree-shell` 的计算样式
  - 检查 flex 布局链：`.arco-drawer-body` → `.authorization-workbench` → `.governance-workbench__main` → `.authorization-panel--tree`
  - **预期结果**: 测试失败 - 滚动条出现在抽屉整体而非 `.authorization-tree-shell`，工具栏和侧边栏会被滚动到视口外
  - 记录观察到的具体行为和可能的根本原因（如缺少 `min-height: 0`、flex 布局链断裂等）
  - 任务完成标准：测试已编写、已运行、失败已记录
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. 编写保留性属性测试（在实施修复之前）
  - **Property 2: Preservation** - 所有现有功能不变
  - **重要**: 遵循观察优先方法
  - 在未修复的代码上观察非 bug 相关的功能行为
  - 记录所有交互功能的预期行为模式
  - **测试方法**: 手动功能测试 + 视觉回归测试
  - 测试场景 1：在授权树中勾选和取消勾选接口，观察选中状态和统计数据更新
  - 测试场景 2：使用筛选工具栏（搜索框、模块下拉、方法下拉）过滤接口，观察授权树显示过滤结果
  - 测试场景 3：点击"全选当前视图"和"清空当前视图"按钮，观察批量选择操作
  - 测试场景 4：点击"保存授权"按钮，观察保存操作和抽屉关闭
  - 测试场景 5：展开和折叠授权树的模块和控制器节点，观察树形结构变化
  - 测试场景 6：观察侧边栏的账号画像和授权数据概览显示
  - 测试场景 7：在不同屏幕尺寸（1920px、1440px、1280px、768px）下观察响应式布局
  - 在未修复的代码上运行这些测试
  - **预期结果**: 所有功能测试通过（确认基线行为需要保留）
  - 记录所有观察到的功能行为作为保留基准
  - 任务完成标准：测试已编写、已运行、在未修复代码上通过
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 3. 修复授权抽屉滚动行为

  - [x] 3.1 实施 CSS 布局修复
    - 在 `bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue` 的 `<style scoped>` 部分添加或修改样式
    - 为 `.authorization-panel--tree` 添加 flex 布局和收缩约束：
      - `display: flex`
      - `flex-direction: column`
      - `min-height: 0` （关键：允许 flex 子元素收缩）
      - `flex: 1` （占据剩余空间）
    - 为 `.authorization-toolbar` 和 `.section-heading` 添加固定约束：
      - `flex-shrink: 0` （不参与收缩，保持固定）
    - 为 `.authorization-tree-shell` 确保滚动容器设置：
      - `flex: 1` （占据剩余空间）
      - `min-height: 0` （关键：允许收缩）
      - `overflow-y: auto` （已存在，确保保留）
    - 验证父容器 flex 链配置正确
    - _Bug_Condition: isBugCondition(input) where input.treeContentHeight > input.viewportHeight_
    - _Expected_Behavior: 滚动条仅出现在 .authorization-tree-shell 内，工具栏和侧边栏保持固定可见_
    - _Preservation: 所有交互功能（勾选、筛选、批量操作、保存、展开/折叠、响应式布局）保持不变_
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 3.2 验证 bug condition 探索性测试现在通过
    - **Property 1: Expected Behavior** - 授权树区域局部滚动
    - **重要**: 重新运行任务 1 中的相同测试 - 不要编写新测试
    - 任务 1 中的测试编码了期望行为
    - 当此测试通过时，确认期望行为已满足
    - 重新运行任务 1 中的所有测试场景
    - **预期结果**: 测试通过 - 滚动条仅出现在 `.authorization-tree-shell` 内，工具栏和侧边栏保持固定可见
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.3 验证保留性测试仍然通过
    - **Property 2: Preservation** - 所有现有功能不变
    - **重要**: 重新运行任务 2 中的相同测试 - 不要编写新测试
    - 重新运行任务 2 中的所有功能测试场景
    - **预期结果**: 所有测试通过（确认没有功能回归）
    - 确认修复后所有功能行为与未修复代码完全相同（除滚动行为外）
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 4. Checkpoint - 确保所有测试通过
  - 确保所有测试通过，如有问题请咨询用户
