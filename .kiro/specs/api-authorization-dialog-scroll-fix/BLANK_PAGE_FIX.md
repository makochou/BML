# 🔧 空白页面问题修复指南

## 问题描述

访问 BML 中台时出现空白页面，无法正常显示内容。

## 已实施的修复

### 1. 添加错误处理机制

为防止 JavaScript 错误导致应用崩溃，已在以下位置添加错误处理：

#### main.ts - 全局错误捕获
```typescript
// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
    console.error('Global Error Handler:', err);
    console.error('Component:', instance);
    console.error('Error Info:', info);
};
```

#### App.vue - 组件错误捕获
```typescript
// 捕获所有子组件错误，防止白屏
onErrorCaptured((err, instance, info) => {
  console.error('Vue Error Captured:', err);
  console.error('Component:', instance);
  console.error('Error Info:', info);
  return false;
});
```

#### theme.ts - 主题系统错误处理
- `applyThemeColor()` 函数添加 try-catch
- `getSavedThemeColor()` 函数添加 try-catch
- `generateColorScale()` 函数添加 try-catch

#### store/app.ts - Store 错误处理
- `initTheme()` 方法添加 try-catch

### 2. 启动失败提示

如果应用完全无法启动，会显示友好的错误提示页面。

---

## 诊断步骤

### 步骤 1：清除缓存并重启

```bash
# 1. 停止开发服务器（Ctrl+C）

# 2. 清除 Vite 缓存
cd bml-frontend
rm -rf node_modules/.vite

# 3. 清除浏览器缓存
# - 打开开发者工具（F12）
# - 右键点击刷新按钮
# - 选择"清空缓存并硬性重新加载"

# 4. 重新启动
npm run dev
```

### 步骤 2：检查浏览器控制台

1. 打开浏览器开发者工具（F12）
2. 切换到 "Console" 标签
3. 查看是否有红色错误信息

#### 常见错误类型

**A. 模块加载错误**
```
Failed to load module script
```
**解决方案**：清除缓存并重启服务器

**B. 网络错误**
```
net::ERR_CONNECTION_REFUSED
```
**解决方案**：确认后端服务器正在运行（localhost:8080）

**C. JavaScript 语法错误**
```
SyntaxError: Unexpected token
```
**解决方案**：检查最近修改的文件是否有语法错误

**D. 主题系统错误**
```
Error applying theme color
Error initializing theme
```
**解决方案**：清除 localStorage 中的主题设置
```javascript
localStorage.removeItem('bml-theme-color');
location.reload();
```

### 步骤 3：检查网络请求

1. 在开发者工具中切换到 "Network" 标签
2. 刷新页面
3. 查看是否有失败的请求（红色）

#### 关键请求检查

- `main.ts` - 应用入口文件
- `App.vue` - 根组件
- `router/index.ts` - 路由配置
- `/api/auth/routers` - 动态路由加载

### 步骤 4：使用无痕模式测试

1. 按 `Ctrl + Shift + N` 打开无痕窗口
2. 访问 `http://localhost:5173`
3. 查看是否能正常显示

如果无痕模式正常，说明是浏览器缓存或扩展程序导致的问题。

### 步骤 5：检查开发服务器输出

查看终端中的 Vite 开发服务器输出，确认：

✅ 正常输出示例：
```
VITE v4.x.x  ready in 1234 ms

➜  Local:   http://localhost:5173/
➜  Network: http://192.168.x.x:5173/
```

❌ 错误输出示例：
```
Error: Cannot find module ...
Failed to resolve import ...
```

---

## 快速诊断脚本

在浏览器控制台执行以下脚本，快速诊断问题：

```javascript
console.clear();
console.log('=== 🔍 BML 中台诊断 ===\n');

// 1. 检查 Vue 应用是否挂载
const app = document.getElementById('app');
if (app) {
    console.log('✅ #app 元素存在');
    console.log('   innerHTML 长度:', app.innerHTML.length);
    if (app.innerHTML.length === 0) {
        console.log('❌ #app 元素为空！应用未正常挂载');
    } else {
        console.log('✅ #app 元素有内容');
    }
} else {
    console.log('❌ #app 元素不存在！');
}

// 2. 检查 Vue 实例
if (window.__VUE__) {
    console.log('✅ Vue 实例存在');
} else {
    console.log('⚠️ Vue 实例不存在（可能是生产模式）');
}

// 3. 检查路由
if (window.location.pathname) {
    console.log('✅ 当前路径:', window.location.pathname);
}

// 4. 检查 localStorage
try {
    const themeColor = localStorage.getItem('bml-theme-color');
    console.log('✅ localStorage 可访问');
    console.log('   主题色:', themeColor || '未设置');
} catch (e) {
    console.log('❌ localStorage 访问失败:', e);
}

// 5. 检查控制台错误
const errors = window.console.error;
console.log('\n=== 检查完成 ===');
console.log('请查看上方是否有红色错误信息');
```

---

## 常见问题解决方案

### Q1: 页面完全空白，控制台无错误

**可能原因**：
- 开发服务器未启动
- 端口被占用
- 浏览器缓存问题

**解决方案**：
1. 确认终端显示 "VITE ready"
2. 访问 `http://localhost:5173`（不是 8080）
3. 清除浏览器缓存
4. 使用无痕模式测试

### Q2: 控制台显示 "Failed to fetch dynamically imported module"

**可能原因**：
- Vite 缓存损坏
- 文件路径错误

**解决方案**：
```bash
rm -rf node_modules/.vite
npm run dev
```

### Q3: 页面闪烁后变空白

**可能原因**：
- 路由配置错误
- 组件加载失败
- 权限验证失败

**解决方案**：
1. 检查控制台错误
2. 检查 Network 标签中的 `/api/auth/routers` 请求
3. 确认后端服务器正在运行

### Q4: 登录后空白

**可能原因**：
- Token 无效
- 动态路由加载失败
- 后端接口错误

**解决方案**：
```javascript
// 在控制台执行，清除认证信息
localStorage.clear();
sessionStorage.clear();
location.reload();
```

### Q5: 主题相关错误

**解决方案**：
```javascript
// 清除主题设置
localStorage.removeItem('bml-theme-color');
location.reload();
```

---

## 紧急恢复步骤

如果以上方法都无效，执行以下紧急恢复：

### 1. 完全重置前端

```bash
cd bml-frontend

# 停止服务器
# Ctrl+C

# 清除所有缓存
rm -rf node_modules/.vite
rm -rf dist

# 重新安装依赖（如果需要）
# npm install

# 重新启动
npm run dev
```

### 2. 重置浏览器状态

```javascript
// 在控制台执行
localStorage.clear();
sessionStorage.clear();
indexedDB.deleteDatabase('bml');
location.reload();
```

### 3. 检查最近的代码更改

使用 Git 查看最近的更改：

```bash
git log --oneline -10
git diff HEAD~1
```

如果需要回滚：

```bash
git stash
# 或
git reset --hard HEAD~1
```

---

## 预防措施

### 1. 开发时使用 TypeScript 严格模式

确保 `tsconfig.json` 中启用严格检查：

```json
{
  "compilerOptions": {
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}
```

### 2. 使用 ESLint 检查代码

```bash
npm run lint
```

### 3. 定期清理缓存

```bash
# 每天开始工作前执行
rm -rf node_modules/.vite
```

### 4. 使用版本控制

在进行重大更改前：

```bash
git add .
git commit -m "保存当前工作状态"
```

---

## 技术支持

如果问题仍未解决，请提供以下信息：

1. **浏览器控制台截图**（包含所有错误信息）
2. **Network 标签截图**（显示所有请求）
3. **终端输出**（Vite 开发服务器的完整输出）
4. **浏览器信息**（Chrome/Edge/Firefox + 版本号）
5. **操作系统**（Windows/Mac/Linux）
6. **最近的代码更改**（git log）

---

**创建日期**: 2026-03-08
**版本**: v1.0.0
**状态**: 已添加全面错误处理

