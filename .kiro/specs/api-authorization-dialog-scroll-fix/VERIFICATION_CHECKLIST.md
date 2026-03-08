# 授权弹窗滚动修复验证清单

## 修复内容总结

本次修复针对 API 授权配置抽屉的滚动行为问题，通过调整 CSS Flexbox 布局实现以下目标：

✅ **抽屉整体固定高度** - 不出现整体滚动条
✅ **授权树区域局部滚动** - 滚动条仅出现在授权树卡片内
✅ **工具栏和侧边栏固定可见** - 始终保持在视口内

## 修改的文件

1. **bml-frontend/src/components/governance/GovernanceWorkbenchShell.vue**
   - 为 `.governance-workbench__layout` 添加 flex 布局支持
   - 为 `.governance-workbench__main` 和 `.governance-workbench__aside` 添加 `min-height: 0`

2. **bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue**
   - 完善整个 flex 布局链条
   - 为 `.authorization-panel--tree` 添加 flex 容器属性
   - 为 `.authorization-toolbar` 和 `.section-heading` 添加 `flex-shrink: 0`
   - 优化 `.authorization-tree-shell` 的滚动容器样式

## 验证步骤

### 1. 重启开发服务器

```bash
# 停止当前运行的服务器（Ctrl+C）
cd bml-frontend
npm run dev
```

### 2. 清除浏览器缓存

- 打开浏览器开发者工具（F12）
- 右键点击刷新按钮
- 选择"清空缓存并硬性重新加载"

### 3. 测试场景

#### 场景 1：长授权树滚动测试 ✅

**操作步骤：**
1. 登录 BML 企业管理系统
2. 进入 API 账号管理页面
3. 选择一个有 50+ 接口的 API 账号
4. 点击"授权"按钮打开授权配置抽屉
5. 打开浏览器开发者工具（F12）
6. 使用元素选择器检查滚动条位置

**预期结果：**
- ✅ 滚动条仅出现在 `.authorization-tree-shell` 容器内（授权树卡片内）
- ✅ 抽屉整体（`.arco-drawer-body`）不出现滚动条
- ✅ 向下滚动时，顶部筛选工具栏保持固定可见
- ✅ 向下滚动时，左侧边栏（账号画像、授权数据概览）保持固定可见

#### 场景 2：工具栏交互测试 ✅

**操作步骤：**
1. 在授权树中滚动到底部
2. 尝试使用顶部的搜索框、模块下拉、方法下拉

**预期结果：**
- ✅ 工具栏始终可见，无需滚动回顶部即可使用
- ✅ 筛选功能正常工作

#### 场景 3：侧边栏信息测试 ✅

**操作步骤：**
1. 在授权树中滚动到底部
2. 观察左侧边栏的账号画像和授权数据概览

**预期结果：**
- ✅ 侧边栏信息始终可见
- ✅ 勾选接口时，"已选接口"数量实时更新且始终可见

#### 场景 4：短授权树测试 ✅

**操作步骤：**
1. 选择一个只有 3-5 个接口的 API 账号
2. 点击"授权"按钮打开授权配置抽屉

**预期结果：**
- ✅ 不出现不必要的滚动条
- ✅ 所有内容正常显示，布局美观

#### 场景 5：响应式布局测试 ✅

**操作步骤：**
1. 调整浏览器窗口大小（1920px、1440px、1280px、768px）
2. 观察抽屉布局变化

**预期结果：**
- ✅ 所有屏幕尺寸下滚动行为正常
- ✅ 响应式布局正常工作

## 开发者工具检查

### CSS 属性验证

使用浏览器开发者工具检查以下元素的计算样式：

#### `.authorization-panel--tree` 应该具有：
```css
display: flex
flex-direction: column
min-height: 0
flex: 1 1 0%
```

#### `.authorization-toolbar` 和 `.section-heading` 应该具有：
```css
flex-shrink: 0
```

#### `.authorization-tree-shell` 应该具有：
```css
flex: 1 1 0%
min-height: 0
overflow-y: auto
overflow-x: hidden
padding: 16px
```

#### `.governance-workbench__main` 应该具有：
```css
display: flex
flex-direction: column
min-height: 0
overflow: hidden
```

## 常见问题排查

### 问题 1：滚动条仍然出现在抽屉整体上

**可能原因：**
- 浏览器缓存未清除
- 开发服务器未重启
- CSS 样式未正确应用

**解决方案：**
1. 清除浏览器缓存并硬性重新加载
2. 重启开发服务器
3. 检查浏览器控制台是否有 CSS 错误

### 问题 2：授权树内容被裁剪

**可能原因：**
- `overflow-y: auto` 未正确应用到 `.authorization-tree-shell`

**解决方案：**
1. 使用开发者工具检查 `.authorization-tree-shell` 的计算样式
2. 确认 `overflow-y: auto` 存在
3. 确认 `flex: 1` 和 `min-height: 0` 存在

### 问题 3：工具栏或侧边栏随内容滚动

**可能原因：**
- `flex-shrink: 0` 未正确应用

**解决方案：**
1. 检查 `.authorization-toolbar` 和 `.section-heading` 的计算样式
2. 确认 `flex-shrink: 0` 存在

## 功能回归测试

确保以下功能仍然正常工作：

- ✅ 勾选/取消勾选接口
- ✅ 使用筛选工具栏（搜索、模块、方法）
- ✅ 点击"全选当前视图"和"清空当前视图"
- ✅ 点击"保存授权"
- ✅ 展开/折叠树节点
- ✅ 侧边栏信息显示和实时更新

## 验证完成标准

- [ ] 场景 1 测试通过
- [ ] 场景 2 测试通过
- [ ] 场景 3 测试通过
- [ ] 场景 4 测试通过
- [ ] 场景 5 测试通过
- [ ] CSS 属性验证通过
- [ ] 功能回归测试通过

## 技术说明

### Flexbox 布局原理

本次修复的核心是建立正确的 Flexbox 布局链：

```
.arco-drawer-body (flex container)
  └─ .drawer-body (flex: 1, min-height: 0)
      └─ .authorization-workbench (flex: 1, min-height: 0)
          └─ .governance-workbench__layout (flex: 1, min-height: 0)
              └─ .governance-workbench__main (flex, min-height: 0)
                  └─ .authorization-panel--tree (flex: 1, min-height: 0)
                      ├─ .authorization-toolbar (flex-shrink: 0) ← 固定
                      ├─ .section-heading (flex-shrink: 0) ← 固定
                      └─ .authorization-tree-shell (flex: 1, overflow-y: auto) ← 滚动
```

### 关键 CSS 属性说明

- **`min-height: 0`**: 允许 flex 子元素收缩到小于其内容高度，这是实现滚动的关键
- **`flex: 1`**: 占据父容器的剩余空间
- **`flex-shrink: 0`**: 不参与收缩，保持固定高度
- **`overflow-y: auto`**: 当内容超出容器高度时显示垂直滚动条

## 联系支持

如果验证过程中遇到问题，请提供以下信息：

1. 浏览器类型和版本
2. 屏幕截图或视频录制
3. 浏览器控制台错误信息
4. 开发者工具中相关元素的计算样式截图

---

**修复日期**: 2026-03-07
**修复版本**: v1.0.0
**相关文档**: 
- `.kiro/specs/api-authorization-dialog-scroll-fix/bugfix.md`
- `.kiro/specs/api-authorization-dialog-scroll-fix/design.md`
- `.kiro/specs/api-authorization-dialog-scroll-fix/tasks.md`
