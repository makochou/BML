# 🎨 授权树勾选状态样式优化

## 问题描述

在授权弹窗的接口树中，当勾选接口时，选中的接口行显示为白色高亮，颜色太浅，不够明显，用户难以识别哪些接口已被选中。

**问题表现**：
- 勾选的接口行背景色太浅（接近白色）
- 与未选中的接口对比度不足
- 不符合主题色系统的设计语言
- 用户体验不佳，难以快速识别选中状态

## 问题根源

`ApiCatalogTree.vue`组件中定义了`.is-checked`类来标识选中状态，但没有为这个类添加明显的背景色样式，导致选中状态不够突出。

## 修复方案

### 设计理念

采用"主题色渐变背景"设计，让选中的接口：
1. 使用主题色的浅色渐变背景
2. 左侧边框使用主题色
3. 文字颜色使用主题色
4. 有轻微的阴影效果
5. 悬停时背景色加深

### 视觉效果

#### 选中状态（is-checked）
- **背景**：主题色浅色渐变（从左到右，从10%透明度到50%透明度）
- **左边框**：3px主题色实线
- **文字颜色**：主题色，加粗
- **路径颜色**：主题色深色
- **阴影**：轻微的主题色阴影

#### 悬停状态（is-checked + hover）
- **背景**：主题色渐变加深（15%透明度）
- **阴影**：阴影加深
- **位置**：向右移动4px

### 技术实现

**文件**: `bml-frontend/src/components/api/ApiCatalogTree.vue`

**核心样式代码**：

```css
/**
 * 勾选状态样式
 * 使用主题色的浅色背景，让选中的接口更明显
 */
.tree-item-api.is-checked {
  background: linear-gradient(90deg, 
    var(--color-primary-light-1, rgba(22, 93, 255, 0.1)) 0%, 
    rgba(255, 255, 255, 0.5) 100%
  );
  border-left-color: var(--color-primary, #165dff);
  box-shadow: 0 2px 8px var(--bml-shadow, rgba(22, 93, 255, 0.08));
}

.tree-item-api.is-checked:hover {
  background: linear-gradient(90deg, 
    var(--color-primary-light-2, rgba(22, 93, 255, 0.15)) 0%, 
    rgba(255, 255, 255, 0.3) 100%
  );
  border-left-color: var(--color-primary, #165dff);
  box-shadow: 0 3px 10px var(--bml-shadow, rgba(22, 93, 255, 0.12));
}

.tree-item-api.is-checked .api-friendly {
  color: var(--color-primary, #165dff);
  font-weight: 600;
}

.tree-item-api.is-checked .api-path {
  color: var(--color-primary-dark-1, #0e42d2);
}

.tree-item-api.active::before,
.tree-item-api.is-checked::before {
  background: var(--color-primary, #165dff);
}
```

**Checkbox样式优化**：

```css
/**
 * Checkbox 样式优化
 * 确保复选框与主题色一致
 */
.tree-item :deep(.arco-checkbox-checked .arco-checkbox-icon) {
  background-color: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox:hover .arco-checkbox-icon) {
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox-indeterminate .arco-checkbox-icon) {
  background-color: var(--color-primary, #165dff) !important;
  border-color: var(--color-primary, #165dff) !important;
}

.tree-item :deep(.arco-checkbox-checked:hover .arco-checkbox-icon) {
  background-color: var(--color-primary-light-4, #4080ff) !important;
  border-color: var(--color-primary-light-4, #4080ff) !important;
}
```

## 技术要点

### 1. 渐变背景

使用CSS渐变创建从左到右的颜色过渡：
```css
background: linear-gradient(90deg, 
  var(--color-primary-light-1, rgba(22, 93, 255, 0.1)) 0%, 
  rgba(255, 255, 255, 0.5) 100%
);
```

这样可以：
- 左侧颜色更深，与左边框呼应
- 右侧颜色渐淡，不会过于突兀
- 整体视觉效果柔和

### 2. 主题色变量

使用CSS变量确保与主题系统一致：
```css
var(--color-primary, #165dff)          /* 主色 */
var(--color-primary-light-1, ...)      /* 浅色1级 */
var(--color-primary-light-2, ...)      /* 浅色2级 */
var(--color-primary-dark-1, #0e42d2)   /* 深色1级 */
var(--bml-shadow, rgba(22, 93, 255, 0.08)) /* 阴影 */
```

### 3. 状态层叠

选中状态的样式优先级：
1. 基础状态：`.tree-item-api`
2. 悬停状态：`.tree-item-api:hover`
3. 选中状态：`.tree-item-api.is-checked`
4. 选中+悬停：`.tree-item-api.is-checked:hover`
5. 激活状态：`.tree-item-api.active`（点击选中，优先级最高）

### 4. 深度选择器

使用`:deep()`穿透scoped样式，修改Arco Design的checkbox样式：
```css
.tree-item :deep(.arco-checkbox-checked .arco-checkbox-icon) {
  background-color: var(--color-primary, #165dff) !important;
}
```

## 验证方法

### 1. 视觉检查

打开授权弹窗，勾选一些接口，检查：
- ✅ 选中的接口行有明显的主题色浅色背景
- ✅ 左侧有3px的主题色边框
- ✅ 接口名称显示为主题色，加粗
- ✅ 接口路径显示为主题色深色
- ✅ 有轻微的阴影效果
- ✅ 与未选中的接口对比度明显

### 2. 交互测试

鼠标悬停在选中的接口上，验证：
- ✅ 背景色加深（从10%到15%透明度）
- ✅ 阴影加深
- ✅ 向右移动4px
- ✅ 动画流畅

### 3. Checkbox测试

检查复选框的样式：
- ✅ 选中时复选框背景为主题色
- ✅ 选中时复选框边框为主题色
- ✅ 半选状态（indeterminate）也是主题色
- ✅ 悬停时边框颜色变为主题色
- ✅ 选中+悬停时背景色变浅

### 4. 主题切换测试

在主题设置中切换不同的主题色，验证：
- ✅ 选中接口的背景色随主题色变化
- ✅ 左边框颜色随主题色变化
- ✅ 文字颜色随主题色变化
- ✅ 复选框颜色随主题色变化
- ✅ 所有状态下都清晰可见

### 5. 批量选择测试

测试模块和分组的批量选择：
- ✅ 勾选模块时，所有子接口都显示选中状态
- ✅ 勾选分组时，该分组下所有接口都显示选中状态
- ✅ 部分选中时，父级复选框显示半选状态（主题色）
- ✅ 全部选中时，父级复选框显示全选状态（主题色）

### 6. 浏览器控制台检查

在浏览器控制台执行以下代码：

```javascript
// 检查选中的接口样式
const checkedApi = document.querySelector('.tree-item-api.is-checked');
if (checkedApi) {
  const styles = window.getComputedStyle(checkedApi);
  console.log('=== 选中接口样式 ===');
  console.log('background:', styles.background);
  console.log('border-left-color:', styles.borderLeftColor);
  console.log('box-shadow:', styles.boxShadow);
  
  const label = checkedApi.querySelector('.api-friendly');
  if (label) {
    const labelStyles = window.getComputedStyle(label);
    console.log('\n=== 接口名称样式 ===');
    console.log('color:', labelStyles.color);
    console.log('font-weight:', labelStyles.fontWeight);
  }
}

// 检查复选框样式
const checkbox = document.querySelector('.tree-item .arco-checkbox-checked .arco-checkbox-icon');
if (checkbox) {
  const styles = window.getComputedStyle(checkbox);
  console.log('\n=== 复选框样式 ===');
  console.log('background-color:', styles.backgroundColor);
  console.log('border-color:', styles.borderColor);
}
```

**预期输出**：
```
=== 选中接口样式 ===
background: linear-gradient(90deg, rgba(22, 93, 255, 0.1) 0%, rgba(255, 255, 255, 0.5) 100%)
border-left-color: rgb(22, 93, 255)
box-shadow: rgba(22, 93, 255, 0.08) 0px 2px 8px

=== 接口名称样式 ===
color: rgb(22, 93, 255)
font-weight: 600

=== 复选框样式 ===
background-color: rgb(22, 93, 255)
border-color: rgb(22, 93, 255)
```

## 设计规范

### 选中状态颜色

- **背景渐变起点**：主题色10%透明度
- **背景渐变终点**：白色50%透明度
- **左边框**：3px主题色实线
- **文字颜色**：主题色
- **路径颜色**：主题色深色（dark-1）
- **阴影**：主题色8%透明度，2px偏移，8px模糊

### 悬停状态颜色

- **背景渐变起点**：主题色15%透明度
- **背景渐变终点**：白色30%透明度
- **阴影**：主题色12%透明度，3px偏移，10px模糊

### Checkbox颜色

- **选中背景**：主题色
- **选中边框**：主题色
- **半选背景**：主题色
- **悬停边框**：主题色
- **选中+悬停背景**：主题色浅色（light-4）

### 动画参数

- **过渡时间**：0.2s（checkbox）、0.25s（接口行）
- **缓动函数**：cubic-bezier(0.4, 0, 0.2, 1)
- **移动距离**：4px（向右）

## 适用场景

这套选中状态样式适用于：
- 授权弹窗的接口树
- API列表页的接口树
- 任何使用ApiCatalogTree组件的地方

## 对比效果

### 修复前
- 选中接口：白色背景，几乎看不出来
- 复选框：默认蓝色，与主题色不一致
- 对比度：不足，难以识别

### 修复后
- 选中接口：主题色渐变背景，清晰明显
- 复选框：主题色，与整体设计一致
- 对比度：充足，一目了然

## 后续优化建议

### 1. 添加选中数量提示

可以在树的顶部添加一个提示条，显示已选中的接口数量：

```vue
<div class="tree-selection-summary">
  已选中 <strong>{{ checkedKeys.length }}</strong> 个接口
</div>
```

### 2. 添加快速操作

可以为选中的接口添加快速操作按钮：

```vue
<div v-if="checkedKeys.length > 0" class="tree-quick-actions">
  <a-button size="small" @click="clearAll">清空选择</a-button>
  <a-button size="small" type="primary" @click="confirmSelection">确认选择</a-button>
</div>
```

### 3. 添加选中动画

可以为选中状态添加更流畅的动画效果：

```css
.tree-item-api.is-checked {
  animation: checkIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes checkIn {
  0% {
    transform: translateX(-10px);
    opacity: 0;
  }
  100% {
    transform: translateX(0);
    opacity: 1;
  }
}
```

### 4. 支持键盘操作

可以添加键盘快捷键支持：
- `Space`：勾选/取消勾选当前接口
- `Ctrl+A`：全选所有接口
- `Ctrl+D`：取消所有选择
- `↑/↓`：上下移动焦点

## 相关文档

- [主题色系统说明](./THEME_COLOR_SYSTEM.md)
- [按钮显示修复指南](./BUTTON_VISIBILITY_FIX.md)
- [空状态按钮优化](./EMPTY_STATE_BUTTON_FIX.md)

---

**修复日期**: 2026-03-08
**版本**: v1.0.0
**状态**: ✅ 已修复并优化

