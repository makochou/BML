# Vue KeepAlive 页面缓存修复指南

## 问题描述

用户反馈：打开多个菜单后，切换回之前的菜单时页面内容不显示（空白）。

## 根本原因

Vue 的 `<keep-alive :include="cachedViews">` 通过**组件 name** 来匹配缓存实例。原代码存在以下问题：

1. **所有页面组件缺少 `name` 声明**：Vue 3 Composition API 需要通过 `defineOptions({ name: '...' })` 显式声明组件名
2. **路由 name 与组件 name 不匹配**：后端生成的路由 name（如 `monitor_server_index`）与前端组件 name（如 `ServerMonitor`）不一致
3. **错误的嵌套顺序**：`<transition>` 包裹 `<keep-alive>` 会破坏缓存机制
4. **高度塌陷问题**：页面组件使用 `height: 100vh` 在 flex 容器中失效，导致内容区域高度为 0

## 修复内容

### 1. 前端 - 所有页面组件添加 `defineOptions`

为以下 14 个页面组件添加了 `defineOptions({ name: '...' })`：

| 文件路径 | 组件 name | 路由 name |
|---|---|---|
| `views/dashboard/Workplace.vue` | `Dashboard` | `Dashboard` |
| `views/api/ApiList.vue` | `ApiList` | `ApiList` |
| `views/api/ApiAccountManage.vue` | `ApiAccountManage` | `ApiAccountManage` |
| `views/api/ApiAccountDetail.vue` | `ApiAccountDetail` | `ApiAccountDetail` |
| `views/system/config/index.vue` | `SystemConfig` | `SystemConfig` |
| `views/system/license/index.vue` | `LicenseManagement` | `LicenseManagement` |
| `views/monitor/server/index.vue` | `ServerMonitor` | `ServerMonitor` |
| `views/monitor/alert/index.vue` | `AlertCenter` | `AlertCenter` |
| `views/business/dashboard/index.vue` | `BusinessDashboard` | `BusinessDashboard` |
| `views/business/system/org/index.vue` | `SystemOrg` | `SystemOrg` |
| `views/business/system/dept/index.vue` | `SystemDept` | `SystemDept` |
| `views/business/system/post/index.vue` | `SystemPost` | `SystemPost` |
| `views/business/system/user/index.vue` | `SystemUser` | `SystemUser` |
| `views/business/system/role/index.vue` | `SystemRole` | `SystemRole` |
| `views/business/system/menu/index.vue` | `SystemMenu` | `SystemMenu` |

**示例代码：**

```vue
<script setup lang="ts">
/**
 * 主机监控页面
 *
 * 重要说明：
 *   defineOptions({ name: 'ServerMonitor' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致，
 *   否则 <keep-alive :include="cachedViews"> 无法匹配到该组件，
 *   导致切换标签页后页面内容被销毁、重新加载。
 */
defineOptions({ name: 'ServerMonitor' });

import { ref, onMounted } from 'vue';
// ... 其他 imports
</script>
```

### 2. 后端 - 修复路由 name 生成逻辑

修改 `SysMenuServiceImpl.java` 中的 `buildRouteName` 方法，为特殊组件路径添加显式映射：

```java
private String buildRouteName(SysMenu menu) {
    if (StrUtil.isNotBlank(menu.getComponent())) {
        String component = menu.getComponent();
        // ── 特殊组件路径：显式映射为语义化路由名称 ──
        if ("dashboard/Workplace".equals(component)) {
            return "Dashboard";
        }
        if ("api/ApiAccountManage".equals(component)) {
            return "ApiAccountManage";
        }
        if ("monitor/server/index".equals(component)) {
            return "ServerMonitor";  // ← 新增
        }
        if ("monitor/alert/index".equals(component)) {
            return "AlertCenter";    // ← 新增
        }
        // ── 通用规则 ──
        return component.replace("/", "_").replace("-", "_");
    }
    return "menu_" + menu.getId();
}
```

### 3. 前端 - 修复 Layout 布局

#### 3.1 移除错误的 transition 嵌套

**修改前（错误）：**
```vue
<router-view v-slot="{ Component }">
  <transition name="fade-transform" mode="out-in">
    <keep-alive :include="cachedViews">
      <component :is="Component" :key="route.fullPath" />
    </keep-alive>
  </transition>
</router-view>
```

**修改后（正确）：**
```vue
<router-view v-slot="{ Component, route: currentRoute }">
  <keep-alive :include="cachedViews">
    <component :is="Component" :key="currentRoute.name" />
  </keep-alive>
</router-view>
```

#### 3.2 修复 page-container 高度问题

**修改前：**
```css
.page-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
```

**修改后：**
```css
.page-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

/* 让 router-view 渲染出的根元素撑满 page-container */
.page-container :deep(> *) {
  height: 100%;
  overflow: auto;
}
```

### 4. 前端 - 修复页面组件高度

将所有页面组件的根容器从 `height: 100vh` 改为 `height: 100%`：

- `views/monitor/server/index.vue` → `.monitor-shell { height: 100%; }`
- `views/monitor/alert/index.vue` → `.alert-shell { height: 100%; }`
- `views/api/ApiAccountManage.vue` → `.api-account-page { height: 100%; }`

## 验证步骤

### 1. 重新编译后端

```bash
cd bml-backend
mvn clean compile
```

### 2. 重启后端服务

```bash
cd bml-backend/bml-app
mvn spring-boot:run
```

### 3. 重新编译前端

```bash
cd bml-frontend
npm run build
```

### 4. 启动前端开发服务器

```bash
cd bml-frontend
npm run dev
```

### 5. 测试步骤

1. 登录中台管理系统（`http://localhost:5173/admin/login`）
2. 依次打开以下菜单：
   - 工作台
   - 主机监控
   - 告警中心
   - 资产目录
   - 授权治理
3. 随意切换标签页，验证：
   - ✅ 每个页面内容都能正常显示
   - ✅ 切换回之前打开的标签页时，页面状态保持（不重新加载）
   - ✅ 滚动位置、表单输入、筛选条件等状态都被保留
   - ✅ 标签页关闭后，缓存被正确清理

### 6. 浏览器开发者工具验证

打开浏览器控制台（F12），在 Vue DevTools 中检查：

1. **Components 面板**：
   - 切换标签页时，组件实例不应该被销毁重建
   - keep-alive 缓存的组件会显示 `<KeepAlive>` 标记

2. **Console 面板**：
   - 添加调试日志验证组件生命周期：
   ```typescript
   onMounted(() => {
     console.log('[ServerMonitor] 组件已挂载');
   });
   onUnmounted(() => {
     console.log('[ServerMonitor] 组件已卸载');
   });
   ```
   - 切换标签页时，不应该看到 "组件已卸载" 日志

3. **Pinia Store**：
   - 检查 `tagsView` store 的 `cachedViews` 数组
   - 应该包含所有打开标签页的路由 name

## 技术要点

### Vue 3 KeepAlive 工作原理

1. **组件 name 匹配**：
   ```vue
   <!-- Layout.vue -->
   <keep-alive :include="['Dashboard', 'ApiList', 'ServerMonitor']">
     <component :is="Component" />
   </keep-alive>
   ```

   ```vue
   <!-- Dashboard.vue -->
   <script setup>
   defineOptions({ name: 'Dashboard' });  // ← 必须声明
   </script>
   ```

2. **缓存策略**：
   - `include` 数组中的组件会被缓存
   - 组件切换时，Vue 会保留实例在内存中
   - 再次激活时，直接复用缓存的实例（不触发 onMounted）

3. **生命周期钩子**：
   - `onActivated()` - 组件被激活时触发（从缓存中恢复）
   - `onDeactivated()` - 组件被停用时触发（进入缓存）

### 常见陷阱

❌ **错误1：transition 包裹 keep-alive**
```vue
<transition>
  <keep-alive>
    <component />
  </keep-alive>
</transition>
```

✅ **正确：keep-alive 直接包裹 component**
```vue
<keep-alive>
  <component />
</keep-alive>
```

❌ **错误2：使用 fullPath 作为 key**
```vue
<component :is="Component" :key="route.fullPath" />
```
→ query 参数变化会导致组件重建

✅ **正确：使用 name 作为 key**
```vue
<component :is="Component" :key="route.name" />
```

❌ **错误3：页面组件使用 height: 100vh**
```css
.my-page {
  height: 100vh;  /* 在 flex 容器中会溢出 */
}
```

✅ **正确：使用 height: 100%**
```css
.my-page {
  height: 100%;  /* 填充父容器 */
}
```

## 后续开发规范

### 新增页面时的 Checklist

- [ ] 在 `<script setup>` 顶部添加 `defineOptions({ name: '...' })`
- [ ] 组件 name 与路由 name 保持一致（PascalCase）
- [ ] 根容器使用 `height: 100%` 而非 `height: 100vh`
- [ ] 如果页面不需要缓存，在路由 meta 中设置 `noCache: true`
- [ ] 如果需要监听激活/停用，使用 `onActivated` / `onDeactivated` 钩子

### 示例模板

```vue
<template>
  <div class="my-page">
    <!-- 页面内容 -->
  </div>
</template>

<script setup lang="ts">
/**
 * 我的页面
 *
 * 重要说明：
 *   defineOptions({ name: 'MyPage' }) 是 keep-alive 缓存的关键。
 *   组件 name 必须与路由配置中的 name 字段保持一致。
 */
defineOptions({ name: 'MyPage' });

import { ref, onMounted, onActivated, onDeactivated } from 'vue';

// 组件首次挂载时执行
onMounted(() => {
  console.log('[MyPage] 组件已挂载');
  // 初始化数据
});

// 从缓存中激活时执行（切换回此标签页）
onActivated(() => {
  console.log('[MyPage] 组件已激活');
  // 刷新数据、恢复定时器等
});

// 切换到其他标签页时执行（进入缓存）
onDeactivated(() => {
  console.log('[MyPage] 组件已停用');
  // 清理定时器、暂停轮询等
});
</script>

<style scoped>
.my-page {
  height: 100%;  /* 关键：使用 100% 而非 100vh */
  padding: 20px;
  overflow: auto;
}
</style>
```

## 调试技巧

### 1. 检查 cachedViews 数组

在浏览器控制台执行：

```javascript
// 查看当前缓存的组件列表
$pinia.state.value.tagsView.cachedViews

// 应该输出类似：
// ['Dashboard', 'ApiList', 'ServerMonitor', 'AlertCenter']
```

### 2. 检查组件 name

在浏览器控制台执行：

```javascript
// 查看当前激活组件的 name
$vm.$options.name

// 或者在 Vue DevTools 中查看组件树
```

### 3. 监听路由变化

在 `Layout.vue` 中添加调试日志：

```typescript
watch(
  () => route.name,
  (newName) => {
    console.log('[Layout] 路由切换:', newName);
    console.log('[Layout] 缓存列表:', tagsViewStore.cachedViews);
  }
);
```

## 性能优化建议

### 1. 限制缓存数量

如果标签页过多，可以限制缓存数量：

```typescript
// tagsView.ts
const MAX_CACHE_COUNT = 10;

addCachedView(view: TagView) {
  const cacheViewName = resolveCacheViewName(view);
  if (cacheViewName && !this.cachedViews.includes(cacheViewName)) {
    this.cachedViews.push(cacheViewName);
    // 超过限制时，移除最早的缓存（保留 affix 标签）
    if (this.cachedViews.length > MAX_CACHE_COUNT) {
      const oldestNonAffix = this.visitedViews.find(
        v => !v.meta?.affix && this.cachedViews.includes(String(v.name))
      );
      if (oldestNonAffix) {
        this.cachedViews = this.cachedViews.filter(
          name => name !== String(oldestNonAffix.name)
        );
      }
    }
  }
}
```

### 2. 手动清理缓存

在页面组件中，可以手动触发缓存清理：

```typescript
import { useTagsViewStore } from '@/store/tagsView';

const tagsViewStore = useTagsViewStore();

// 清理当前页面缓存（强制重新加载）
const refreshPage = () => {
  tagsViewStore.delView(route);
  router.replace({ name: route.name });
};
```

## 相关文件清单

### 前端修改

- `src/layout/Layout.vue` - 中台管理布局
- `src/layout/BusinessLayout.vue` - 业务系统布局
- `src/views/dashboard/Workplace.vue`
- `src/views/api/ApiList.vue`
- `src/views/api/ApiAccountManage.vue`
- `src/views/api/ApiAccountDetail.vue`
- `src/views/system/config/index.vue`
- `src/views/system/license/index.vue`
- `src/views/monitor/server/index.vue`
- `src/views/monitor/alert/index.vue`
- `src/views/business/dashboard/index.vue`
- `src/views/business/system/org/index.vue`
- `src/views/business/system/dept/index.vue`
- `src/views/business/system/post/index.vue`
- `src/views/business/system/user/index.vue`
- `src/views/business/system/role/index.vue`
- `src/views/business/system/menu/index.vue`

### 后端修改

- `bml-backend/bml-modules/bml-module-system/src/main/java/com/bml/module/system/service/impl/SysMenuServiceImpl.java`

## 参考资料

- [Vue 3 KeepAlive 官方文档](https://vuejs.org/guide/built-ins/keep-alive.html)
- [Vue Router 缓存路由](https://router.vuejs.org/guide/advanced/meta.html)
- [Arco Design Vue 布局组件](https://arco.design/vue/component/layout)
