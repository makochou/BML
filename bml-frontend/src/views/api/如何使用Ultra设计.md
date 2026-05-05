# 如何使用 Ultra Edition 设计

## 🎯 快速开始（只需3步）

### 第1步：打开文件
打开文件：`bml-frontend/src/views/api/ApiAccountManage.vue`

### 第2步：添加样式导入
在文件的 `<style>` 标签中添加以下代码：

```vue
<style scoped lang="scss">
/* ═══════════════════════════════════════════════════════════════
   导入 Ultra Edition 超级精美样式
   ═══════════════════════════════════════════════════════════════ */

/* 1. 查询面板 Ultra 样式 */
@import './ApiAccountManageUltra.scss';

/* 2. 表格列表 Ultra 样式 */
@import './ApiAccountTableUltra.scss';

/* 3. 分页统计 Ultra 样式 */
@import './ApiAccountPaginationUltra.scss';

/* 原有样式保持不变 */
</style>
```

### 第3步：配置分页（可选）
在 `<script>` 部分找到分页配置，修改为：

```typescript
const pagination = ref({
  current: 1,
  pageSize: 30, // 改为30条（原来是10条）
  total: 0
});
```

## ✅ 完成！

保存文件，刷新浏览器，您就能看到超级精美的新设计了！

---

## 🎨 效果预览

### 查询面板
- ✨ 4层渐变背景 + 30px模糊
- 🌈 7色彩虹装饰条 + 流动动画
- 💫 3个浮动光球
- ✨ 输入框发光效果
- 🎯 按钮3D浮动动画

### 表格列表
- 📊 卡片化设计
- 🏷️ 3D标签系统
- 💫 行悬停浮动效果
- 🎨 渐变表头
- ✨ 图标旋转动画

### 分页统计
- 📊 实时统计卡片
- 🔢 每页30条数据
- 📈 数字滚动动画
- ⚡ 3D分页按钮

---

## 📚 详细文档

如需了解更多信息，请查看：

1. **README_ULTRA.md** - 项目入口
2. **ULTRA_QUICK_REFERENCE.md** - 快速参考
3. **ULTRA_INTEGRATION_GUIDE.md** - 完整指南

---

## 🐛 遇到问题？

### 样式不生效？
1. 清除浏览器缓存：`Ctrl + Shift + R`
2. 重启开发服务器：`npm run dev`

### 光球不显示？
在查询面板模板中添加：
```vue
<div class="query-panel__orb-2"></div>
<div class="query-panel__orb-3"></div>
```

---

**就这么简单！享受超级精美的设计吧！** 🚀
