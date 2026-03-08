# 如何在浏览器控制台检查样式

## 📋 完整步骤说明

### 步骤 1：打开系统并进入授权弹窗

1. 在浏览器中打开 BML 企业管理系统
2. 登录系统
3. 点击左侧菜单 "API账号管理"
4. 在账号列表中，点击任意一个账号的 "授权" 按钮
5. 此时会弹出 "接口授权" 弹窗

**保持这个弹窗打开！**

---

### 步骤 2：打开浏览器开发者工具

有三种方法可以打开：

#### 方法 1：使用快捷键（推荐）
- **Windows/Linux**: 按 `F12` 键
- **Mac**: 按 `Cmd + Option + I`

#### 方法 2：使用鼠标右键
1. 在页面空白处点击鼠标右键
2. 选择 "检查" 或 "Inspect"

#### 方法 3：使用浏览器菜单
- **Chrome**: 点击右上角三个点 → 更多工具 → 开发者工具
- **Edge**: 点击右上角三个点 → 更多工具 → 开发者工具
- **Firefox**: 点击右上角三条线 → 更多工具 → Web 开发者工具

开发者工具通常会在浏览器底部或右侧打开。

---

### 步骤 3：切换到 Console（控制台）标签

在开发者工具的顶部，你会看到几个标签：
- Elements（元素）
- Console（控制台）← **点击这个**
- Sources（源代码）
- Network（网络）
- 等等...

**点击 "Console" 标签**

---

### 步骤 4：在控制台输入代码

在 Console 标签的底部，你会看到一个蓝色的 `>` 符号和一个闪烁的光标。

**复制以下代码**（全选并复制）：

```javascript
const layout = document.querySelector('.governance-workbench__layout');
console.log('align-items:', window.getComputedStyle(layout).alignItems);
```

**粘贴到控制台**：
1. 点击 `>` 符号后面的空白区域
2. 按 `Ctrl + V`（Windows）或 `Cmd + V`（Mac）粘贴代码
3. 按 `Enter` 键执行

---

### 步骤 5：查看输出结果

执行后，控制台会显示类似这样的输出：

#### ✅ 正确的输出（修复成功）：
```
align-items: stretch
```

#### ❌ 错误的输出（修复未生效）：
```
align-items: start
```

或者：
```
Uncaught TypeError: Cannot read properties of null
```
（这表示找不到元素，可能弹窗没有打开）

---

## 🔍 完整检查脚本

如果你想检查更多信息，可以使用这个更详细的脚本：

**复制以下完整代码**：

```javascript
console.clear(); // 清空控制台

console.log('=== 🔍 授权弹窗样式检查 ===\n');

// 检查 layout
const layout = document.querySelector('.governance-workbench__layout');
if (layout) {
  const layoutStyles = window.getComputedStyle(layout);
  console.log('✅ 找到 .governance-workbench__layout');
  console.log('  align-items:', layoutStyles.alignItems);
  console.log('  grid-template-rows:', layoutStyles.gridTemplateRows);
  console.log('  height:', layoutStyles.height);
  console.log('  display:', layoutStyles.display);
} else {
  console.log('❌ 找不到 .governance-workbench__layout');
  console.log('   请确保授权弹窗已打开！');
}

console.log('\n---\n');

// 检查 main
const main = document.querySelector('.governance-workbench__main');
if (main) {
  const mainStyles = window.getComputedStyle(main);
  console.log('✅ 找到 .governance-workbench__main');
  console.log('  height:', mainStyles.height);
  console.log('  overflow:', mainStyles.overflow);
  console.log('  min-height:', mainStyles.minHeight);
} else {
  console.log('❌ 找不到 .governance-workbench__main');
}

console.log('\n---\n');

// 检查 tree-shell
const treeShell = document.querySelector('.authorization-tree-shell');
if (treeShell) {
  const treeStyles = window.getComputedStyle(treeShell);
  console.log('✅ 找到 .authorization-tree-shell');
  console.log('  overflow-y:', treeStyles.overflowY);
  console.log('  height:', treeStyles.height);
  console.log('  flex:', treeStyles.flex);
  console.log('  min-height:', treeStyles.minHeight);
} else {
  console.log('❌ 找不到 .authorization-tree-shell');
}

console.log('\n=== 检查完成 ===');

// 判断修复是否成功
if (layout && window.getComputedStyle(layout).alignItems === 'stretch') {
  console.log('\n✅ 修复成功！align-items 已设置为 stretch');
} else if (layout && window.getComputedStyle(layout).alignItems === 'start') {
  console.log('\n❌ 修复未生效！align-items 仍然是 start');
  console.log('   请尝试：');
  console.log('   1. 清除浏览器缓存');
  console.log('   2. 重启开发服务器');
  console.log('   3. 使用无痕模式测试');
} else {
  console.log('\n⚠️ 无法判断修复状态');
}
```

**粘贴并按 Enter 执行**

---

## 📸 截图示例

### 正确的控制台位置

```
┌─────────────────────────────────────────────┐
│  Elements  Console  Sources  Network  ...   │ ← 标签栏
├─────────────────────────────────────────────┤
│                                              │
│  > const layout = document.querySelector... │ ← 输入区域
│  align-items: stretch                        │ ← 输出结果
│  >                                           │ ← 光标位置
│                                              │
└─────────────────────────────────────────────┘
```

---

## 🎯 预期结果对照表

| 检查项 | 正确值 | 错误值 | 说明 |
|--------|--------|--------|------|
| `align-items` | `stretch` | `start` | 最关键的检查项 |
| `grid-template-rows` | 包含 `px` 的值 | `none` | 应该有具体数值 |
| `overflow-y` (tree-shell) | `auto` | `visible` 或 `hidden` | 滚动容器必须是 auto |
| `height` (layout) | 具体的 `px` 值 | `auto` | 应该有固定高度 |

---

## ❓ 常见问题

### Q1: 找不到 Console 标签

**解决方案**：
1. 确认开发者工具已打开（按 F12）
2. 查看顶部标签栏，可能需要点击 `>>` 展开更多标签
3. 或者按 `Ctrl + Shift + J`（Windows）直接打开 Console

### Q2: 显示 "Cannot read properties of null"

**原因**：找不到元素

**解决方案**：
1. 确认授权弹窗已打开
2. 确认弹窗完全加载完成（不是加载中状态）
3. 刷新页面后重新打开弹窗

### Q3: 输出是 "start" 而不是 "stretch"

**原因**：修复未生效

**解决方案**：
1. 清除浏览器缓存（右键刷新按钮 → 清空缓存并硬性重新加载）
2. 重启开发服务器
3. 使用无痕模式测试（Ctrl + Shift + N）

### Q4: 代码粘贴后显示多行

**这是正常的**！直接按 Enter 执行即可。

如果想在一行输入，可以用分号连接：
```javascript
const layout = document.querySelector('.governance-workbench__layout'); console.log('align-items:', window.getComputedStyle(layout).alignItems);
```

---

## 🎬 视频教程步骤（文字版）

1. **[0:00]** 打开 BML 系统，登录
2. **[0:10]** 点击左侧 "API账号管理"
3. **[0:15]** 点击任意账号的 "授权" 按钮
4. **[0:20]** 弹窗打开后，按 F12 键
5. **[0:25]** 点击顶部的 "Console" 标签
6. **[0:30]** 在底部输入框粘贴代码
7. **[0:35]** 按 Enter 键执行
8. **[0:40]** 查看输出结果

---

## 📞 需要帮助？

如果执行后遇到问题，请提供：

1. **截图**：整个浏览器窗口（包括开发者工具）
2. **控制台输出**：完整的输出内容
3. **浏览器信息**：Chrome/Edge/Firefox + 版本号

---

**创建日期**: 2026-03-07
**适用浏览器**: Chrome, Edge, Firefox, Safari
