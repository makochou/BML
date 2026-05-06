# 授权治理页面完整设计 - 快速参考卡片

## 🚀 5分钟快速集成

```vue
<!-- ApiAccountManage.vue -->
<style scoped lang="scss">
/* 查询面板样式 */
@import './ApiAccountManageEnhanced.scss';

/* 表格列表样式 */
@import './ApiAccountTableEnhanced.scss';
</style>
```

---

## 🎨 CSS 变量速查表

### 尺寸变量

```scss
--query-panel-padding: 12px;              // 外层内边距
--query-panel-radius: 24px;               // 外层圆角
--query-panel-body-padding: 18px 20px 12px; // 内容区内边距
--query-panel-input-min-height: 40px;     // 输入框高度
--query-panel-button-height: 40px;        // 按钮高度
--query-panel-field-gap-x: 14px;          // 字段横向间距
```

### 颜色变量

```scss
--query-panel-shell-border-color: rgba(148, 163, 184, 0.25);
--query-panel-input-border-color: rgba(203, 213, 225, 0.6);
--query-panel-label-color: #1e293b;
--query-panel-muted-button-color: #475569;
```

---

## 🎯 常用自定义

### 修改主题色

```scss
.api-account-page .query-panel {
  --query-panel-accent-bar-background: 
    linear-gradient(90deg, #your-color-1, #your-color-2);
}
```

### 调整输入框高度

```scss
.api-account-page .query-panel {
  --query-panel-input-min-height: 36px;
  --query-panel-button-height: 36px;
}
```

### 禁用动画

```scss
.api-account-page .query-panel {
  --query-panel-orb-animation: none;
}
```

---

## 📱 响应式断点

| 设备 | 屏幕宽度 | 关键调整 |
|------|----------|----------|
| 桌面 | > 1024px | 完整布局 |
| 平板 | 768-1024px | 减小间距 |
| 移动 | < 768px | 纵向堆叠，按钮全宽 |

---

## 🐛 常见问题

### 样式不生效？
```bash
# 清除缓存
Ctrl + Shift + R

# 重启服务器
npm run dev
```

### 背景模糊不显示？
```scss
/* 添加前缀 */
backdrop-filter: blur(20px);
-webkit-backdrop-filter: blur(20px);
```

### 动画卡顿？
```scss
/* 禁用动画 */
@media (prefers-reduced-motion: reduce) {
  * {
    animation: none !important;
    transition: none !important;
  }
}
```

---

## 📊 性能指标

| 指标 | 目标值 |
|------|--------|
| 首次渲染 | < 100ms |
| 动画帧率 | 60fps |
| 交互响应 | < 16ms |
| 文件大小 | < 50KB |

---

## ✅ 验收清单

- [ ] 玻璃态背景
- [ ] 彩虹装饰条
- [ ] 动态光球
- [ ] 输入框圆角和阴影
- [ ] 按钮渐变色
- [ ] 悬停效果
- [ ] 聚焦效果
- [ ] 响应式布局
- [ ] 动画流畅

---

## 📚 完整文档

- [设计指南](./QUERY_PANEL_DESIGN_GUIDE.md)
- [集成指南](./INTEGRATION_GUIDE.md)
- [使用示例](./EXAMPLE_USAGE.md)
- [项目README](./README_QUERY_PANEL_REDESIGN.md)

---

## 🎨 颜色速查

| 颜色名 | 色值 | 用途 |
|--------|------|------|
| Indigo 500 | `#6366f1` | 主按钮 |
| Violet 500 | `#8b5cf6` | 渐变中间 |
| Purple 500 | `#a855f7` | 渐变结束 |
| Slate 900 | `#0f172a` | 主文字 |
| Slate 400 | `#94a3b8` | 占位符 |

---

**快速参考 v1.0.0** | **更新：2026-04-30**


---

## 📊 表格样式变量

### 尺寸变量

```scss
--table-header-height: 48px;          // 表头高度
--table-row-height: 56px;             // 行高
--table-cell-padding: 12px 16px;      // 单元格内边距
```

### 颜色变量

```scss
--table-border-color: rgba(226, 232, 240, 0.6);
--table-header-bg: linear-gradient(...);
--table-row-bg: rgba(255, 255, 255, 0.6);
```

---

## 🏷️ 标签样式

### 状态标签

```scss
/* 成功/启用 - 绿色 */
.arco-tag-green {
  background: linear-gradient(135deg, 
    rgba(16, 185, 129, 0.15), 
    rgba(5, 150, 105, 0.12)
  );
  color: #047857;
}

/* 错误/禁用 - 红色 */
.arco-tag-red {
  background: linear-gradient(135deg, 
    rgba(239, 68, 68, 0.15), 
    rgba(220, 38, 38, 0.12)
  );
  color: #b91c1c;
}

/* 信息 - 蓝色 */
.arco-tag-blue {
  background: linear-gradient(135deg, 
    rgba(59, 130, 246, 0.15), 
    rgba(37, 99, 235, 0.12)
  );
  color: #1d4ed8;
}
```

---

## 🎯 常用自定义

### 调整表格行高

```scss
.api-account-page .account-table {
  --table-row-height: 60px;  // 增加行高
}
```

### 调整标签样式

```scss
.api-account-page .account-table :deep(.arco-tag) {
  height: 28px;
  padding: 0 14px;
  font-size: 13px;
}
```

### 调整操作按钮

```scss
.api-account-page .table-action-btn {
  height: 34px;
  padding: 0 16px;
  font-size: 14px;
}
```

---

## 📱 响应式快速配置

```scss
/* 平板端 */
@media (max-width: 1024px) {
  --table-row-height: 52px;
}

/* 移动端 */
@media (max-width: 768px) {
  --table-row-height: 48px;
  --table-cell-padding: 8px 12px;
}
```

---

## 🎨 完整颜色系统

### 主色调
| 颜色 | 色值 | 用途 |
|------|------|------|
| Indigo 500 | `#6366f1` | 主按钮、聚焦 |
| Violet 500 | `#8b5cf6` | 渐变中间 |
| Purple 500 | `#a855f7` | 渐变结束 |

### 状态色
| 状态 | 色值 | 用途 |
|------|------|------|
| 成功 | `#10b981` | 启用、成功 |
| 错误 | `#ef4444` | 禁用、错误 |
| 信息 | `#3b82f6` | 信息、提示 |
| 警告 | `#f59e0b` | 警告 |

### 中性色
| 颜色 | 色值 | 用途 |
|------|------|------|
| Slate 900 | `#0f172a` | 主文字 |
| Slate 800 | `#1e293b` | 标签文字 |
| Slate 600 | `#475569` | 次要文字 |
| Slate 400 | `#94a3b8` | 占位符 |

---

## ✅ 完整验收清单

### 查询面板
- [ ] 玻璃态背景
- [ ] 彩虹装饰条
- [ ] 动态光球
- [ ] 精致输入框
- [ ] 渐变按钮

### 表格列表
- [ ] 玻璃态容器
- [ ] 渐变表头
- [ ] 悬停动画
- [ ] 精美标签
- [ ] 现代按钮

---

## 📚 完整文档

- [查询面板设计指南](./QUERY_PANEL_DESIGN_GUIDE.md)
- [表格列表设计指南](./TABLE_DESIGN_GUIDE.md)
- [完整集成指南](./COMPLETE_INTEGRATION_GUIDE.md)
- [使用示例](./EXAMPLE_USAGE.md)

---

**快速参考 v2.0.0** | **更新：2026-04-30**
