# 最终验证清单

## ⚠️ 重要提示

如果修改后滚动条仍然出现在整个抽屉上，请**严格按照以下步骤**操作：

## 🔄 步骤 1：完全重启（必须执行）

### 1.1 停止所有服务

```bash
# 停止前端服务器（在终端按 Ctrl+C）
# 停止后端服务器（如果在运行）
```

### 1.2 清理缓存

```bash
cd bml-frontend

# 清理 Vite 缓存
rm -rf node_modules/.vite

# 清理 node_modules 缓存（可选）
rm -rf node_modules/.cache
```

### 1.3 重新启动

```bash
npm run dev
```

**等待服务器完全启动**（看到 "ready in XXXms" 消息）

## 🌐 步骤 2：清除浏览器缓存（必须执行）

### 方法 1：硬性重新加载（推荐）

1. 打开浏览器开发者工具（F12）
2. **右键点击**浏览器地址栏旁边的刷新按钮
3. 选择"**清空缓存并硬性重新加载**"

### 方法 2：清除所有缓存

1. 按 `Ctrl + Shift + Delete`（Windows）或 `Cmd + Shift + Delete`（Mac）
2. 选择"**缓存的图片和文件**"
3. 时间范围选择"**全部时间**"
4. 点击"**清除数据**"

### 方法 3：使用无痕模式测试

1. 按 `Ctrl + Shift + N`（Chrome）或 `Ctrl + Shift + P`（Firefox）
2. 在无痕窗口中打开系统
3. 测试授权弹窗

## 🔍 步骤 3：验证代码是否正确

### 3.1 检查 ApiAuthorizationWorkbenchDrawer.vue

打开文件：`bml-frontend/src/components/api-account/ApiAuthorizationWorkbenchDrawer.vue`

搜索 `.authorization-workbench`，确认有以下代码：

```css
.authorization-workbench {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
  min-height: 0 !important;
  gap: 16px;
  overflow: hidden !important;
  height: 100% !important;
}
```

**关键点**：必须有 `overflow: hidden !important;` 和 `height: 100% !important;`

### 3.2 检查 GovernanceWorkbenchShell.vue

打开文件：`bml-frontend/src/components/governance/GovernanceWorkbenchShell.vue`

搜索 `.governance-workbench__layout`，确认有以下代码：

```css
.governance-workbench__layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
```

搜索 `.governance-workbench__main`，确认有以下代码：

```css
.governance-workbench__aside,
.governance-workbench__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
  min-height: 0;
}
```

**关键点**：必须有 `min-height: 0;`

## 🧪 步骤 4：使用开发者工具验证

### 4.1 打开授权弹窗

1. 登录系统
2. 进入 API 账号管理
3. 点击任意账号的"授权"按钮

### 4.2 检查滚动条位置

**方法 1：视觉检查**

观察滚动条出现的位置：
- ✅ **正确**：滚动条在授权树卡片内（灰白色渐变背景的区域）
- ❌ **错误**：滚动条在整个弹窗的右侧

**方法 2：使用元素选择器**

1. 打开开发者工具（F12）
2. 点击左上角的元素选择器图标（箭头）
3. 将鼠标悬停在滚动条上
4. 查看高亮显示的元素

应该高亮显示 `.authorization-tree-shell` 元素。

### 4.3 检查计算样式

1. 在开发者工具中选中 `.authorization-tree-shell` 元素
2. 切换到 "Computed" 标签
3. 搜索以下属性并验证值：

| 属性 | 预期值 |
|------|--------|
| `overflow-y` | `auto` |
| `flex` | `1 1 0%` 或 `1` |
| `min-height` | `0px` |

如果值不正确，说明样式没有正确应用。

## 🐛 步骤 5：故障排查

### 5.1 检查控制台错误

打开浏览器控制台（F12 → Console），查看是否有：
- ❌ 红色错误信息
- ⚠️ 黄色警告信息
- 🔵 Vue 相关警告

如果有错误，请记录下来。

### 5.2 检查网络请求

1. 切换到 "Network" 标签
2. 刷新页面
3. 查找 `.vue` 或 `.css` 文件
4. 确认所有文件都成功加载（状态码 200）

### 5.3 检查样式是否加载

在控制台执行以下代码：

```javascript
// 检查 authorization-tree-shell 元素
const treeShell = document.querySelector('.authorization-tree-shell');
if (treeShell) {
  const styles = window.getComputedStyle(treeShell);
  console.log('=== Authorization Tree Shell 样式 ===');
  console.log('overflow-y:', styles.overflowY);
  console.log('flex:', styles.flex);
  console.log('min-height:', styles.minHeight);
  console.log('height:', styles.height);
} else {
  console.log('❌ 找不到 .authorization-tree-shell 元素');
}

// 检查 authorization-workbench 元素
const workbench = document.querySelector('.authorization-workbench');
if (workbench) {
  const styles = window.getComputedStyle(workbench);
  console.log('=== Authorization Workbench 样式 ===');
  console.log('overflow:', styles.overflow);
  console.log('flex:', styles.flex);
  console.log('min-height:', styles.minHeight);
  console.log('height:', styles.height);
} else {
  console.log('❌ 找不到 .authorization-workbench 元素');
}
```

**预期输出**：
```
=== Authorization Tree Shell 样式 ===
overflow-y: auto
flex: 1 1 0%
min-height: 0px
height: XXXpx (具体数值)

=== Authorization Workbench 样式 ===
overflow: hidden
flex: 1 1 0%
min-height: 0px
height: 100%
```

## 📸 步骤 6：截图记录

如果问题仍然存在，请截取以下内容：

### 6.1 整体截图
- 打开授权弹窗的完整界面
- 显示滚动条的位置

### 6.2 开发者工具截图
- Elements 标签：显示元素层级
- Computed 标签：显示 `.authorization-tree-shell` 的计算样式
- Console 标签：显示步骤 5.3 的输出

### 6.3 文件内容截图
- `ApiAuthorizationWorkbenchDrawer.vue` 的 `<style scoped>` 部分
- `GovernanceWorkbenchShell.vue` 的相关样式部分

## ✅ 成功标准

修复成功后，您应该看到：

1. ✅ 滚动条**仅**出现在授权树卡片内（灰白色渐变背景区域）
2. ✅ 整个弹窗**不**出现滚动条
3. ✅ 向下滚动时，顶部筛选工具栏**保持可见**
4. ✅ 向下滚动时，左侧边栏（账号画像、授权数据概览）**保持可见**
5. ✅ 所有功能正常工作（勾选、筛选、保存等）

## 🆘 如果仍然无法解决

请提供以下信息：

1. **环境信息**
   ```bash
   node --version
   npm --version
   ```

2. **依赖版本**
   ```bash
   cd bml-frontend
   npm list vue
   npm list @arco-design/web-vue
   ```

3. **浏览器信息**
   - 浏览器类型和版本
   - 操作系统

4. **截图**
   - 步骤 6 中要求的所有截图

5. **控制台输出**
   - 步骤 5.3 中 JavaScript 的输出
   - 任何错误或警告信息

## 📝 备注

- 修改 CSS 后**必须**重启开发服务器
- 修改 CSS 后**必须**清除浏览器缓存
- 如果使用了浏览器扩展（如广告拦截器），请尝试禁用后测试
- 如果使用了代理或 VPN，请尝试关闭后测试

---

**创建日期**: 2026-03-07
**最后更新**: 2026-03-07
**版本**: v3.0.0
