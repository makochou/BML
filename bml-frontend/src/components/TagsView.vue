<template>
  <div class="tags-view-container">
    <div class="tags-scroll-wrapper">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        custom
        v-slot="{ navigate }"
      >
        <a-dropdown
          trigger="contextMenu"
          position="bl"
          @select="handleTagMenuSelect(tag, $event)"
        >
          <div
            class="tags-view-item"
            :class="{ active: isActive(tag) }"
            @click="navigate"
          >
            <span class="tag-title">{{ tag.title }}</span>
            <span
              v-if="!isAffix(tag)"
              class="tag-close-icon"
              @click.prevent.stop="closeSelectedTag(tag)"
            >
              <icon-close />
            </span>
          </div>
          <template #content>
            <a-doption
              v-for="action in createActionOptions(tag)"
              :key="action.key"
              :value="action.key"
              :disabled="action.disabled"
            >
              <span
                class="tags-dropdown-label"
                :class="{ danger: action.danger }"
              >
                {{ action.label }}
              </span>
            </a-doption>
          </template>
        </a-dropdown>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { IconClose } from '@arco-design/web-vue/es/icon';
import type { TagView } from '../store/tagsView';
import { useTagsViewStore } from '../store/tagsView';

type TagActionKey = 'closeCurrent' | 'closeOthers' | 'closeAll';
type TagActionSource = 'contextmenu' | 'close-icon';

interface TagActionOption {
  key: TagActionKey;
  label: string;
  disabled: boolean;
  danger?: boolean;
}

interface TagActionContext {
  targetView: TagView;
  source: TagActionSource;
}

/** 根据当前路径判断所属布局，返回对应的工作台首页路径 */
const dashboardFallbackPath = computed(() =>
  route.path.startsWith('/admin') ? '/admin/dashboard' : '/dashboard'
);
const tagActionLabels: Record<TagActionKey, string> = {
  closeCurrent: '\u5173\u95ed\u5f53\u524d\u6807\u7b7e',
  closeOthers: '\u5173\u95ed\u5176\u4ed6\u6807\u7b7e\u9875',
  closeAll: '\u5173\u95ed\u6240\u6709\u6807\u7b7e\u9875',
};
const tagActionKeys: TagActionKey[] = ['closeCurrent', 'closeOthers', 'closeAll'];

const route = useRoute();
const router = useRouter();
const tagsViewStore = useTagsViewStore();

const isSameView = (source?: TagView | null, target?: TagView | null) => {
  return Boolean(source?.path && target?.path && source.path === target.path);
};

const isAffix = (tag?: TagView | null) => {
  return Boolean(tag?.meta?.affix);
};

const isActive = (tag: TagView) => {
  return isSameView(tag, route as TagView);
};

const isTagActionKey = (value: string | number): value is TagActionKey => {
  return typeof value === 'string' && tagActionKeys.includes(value as TagActionKey);
};

const findTagView = (views: TagView[], target?: TagView | null) => {
  if (!target) {
    return null;
  }

  return views.find((view) => isSameView(view, target)) ?? null;
};

const findFirstAffixView = (views: TagView[]) => {
  return views.find((view) => isAffix(view)) ?? null;
};

const visitedViews = computed(() => tagsViewStore.visitedViews);
const currentTagView = computed(() => {
  return visitedViews.value.find((tag) => isSameView(tag, route as TagView)) ?? null;
});

const navigateToView = async (targetView?: TagView | null) => {
  if (targetView?.fullPath) {
    await router.push(String(targetView.fullPath));
    return;
  }

  if (targetView?.path) {
    await router.push(String(targetView.path));
    return;
  }

  await router.push(dashboardFallbackPath.value);
};

const initTags = () => {
  const routes = router.getRoutes();
  /** 判断当前是否处于中台管理布局（/admin 前缀） */
  const isAdmin = route.path.startsWith('/admin');
  /**
   * 仅保留与当前布局上下文匹配的 affix 路由，
   * 避免业务系统标签页中混入中台的工作台标签（反之亦然）
   */
  const affixTags = routes.filter((r) => {
    if (!r.meta?.affix) return false;
    return isAdmin ? r.path.startsWith('/admin') : !r.path.startsWith('/admin');
  });

  for (const affixTag of affixTags) {
    if (!affixTag.name) {
      continue;
    }

    const normalizedAffixTag = {
      ...affixTag,
      path: affixTag.path,
      fullPath: affixTag.path,
      meta: affixTag.meta,
    } as TagView;

    tagsViewStore.addVisitedView(normalizedAffixTag);
    tagsViewStore.addCachedView(normalizedAffixTag);
  }
};

const addTags = () => {
  if (route.name) {
    tagsViewStore.addView(route);
  }
};

const createActionOptions = (targetView?: TagView | null): TagActionOption[] => {
  const closableViews = visitedViews.value.filter((tag) => !isAffix(tag));
  const hasTargetView = Boolean(targetView?.path);

  return [
    {
      key: 'closeCurrent',
      label: tagActionLabels.closeCurrent,
      disabled: !hasTargetView || isAffix(targetView),
    },
    {
      key: 'closeOthers',
      label: tagActionLabels.closeOthers,
      disabled: !hasTargetView || !visitedViews.value.some((tag) => !isAffix(tag) && !isSameView(tag, targetView)),
    },
    {
      key: 'closeAll',
      label: tagActionLabels.closeAll,
      disabled: closableViews.length === 0,
      danger: closableViews.length > 0,
    },
  ];
};

// Stable routing after closing the active tab keeps navigation predictable and avoids jumping to unrelated pages.
const resolveCloseFallbackView = (
  previousViews: TagView[],
  retainedViews: TagView[],
  removedView: TagView
) => {
  const removedIndex = previousViews.findIndex((view) => isSameView(view, removedView));

  if (removedIndex >= 0) {
    for (let index = removedIndex + 1; index < previousViews.length; index += 1) {
      const candidate = findTagView(retainedViews, previousViews[index]);
      if (candidate) {
        return candidate;
      }
    }

    for (let index = removedIndex - 1; index >= 0; index -= 1) {
      const candidate = findTagView(retainedViews, previousViews[index]);
      if (candidate) {
        return candidate;
      }
    }
  }

  return findFirstAffixView(retainedViews);
};

// Right-click actions and close buttons dispatch through one path so future tab operations stay consistent.
const executeTagAction = async (actionKey: TagActionKey, context: TagActionContext) => {
  const actionOption = createActionOptions(context.targetView).find((action) => action.key === actionKey);
  if (!actionOption || actionOption.disabled) {
    return;
  }

  const previousViews = [...visitedViews.value];
  const activeView = currentTagView.value;

  if (actionKey === 'closeCurrent') {
    const result = await tagsViewStore.delView(context.targetView);

    if (activeView && isSameView(activeView, context.targetView)) {
      await navigateToView(
        resolveCloseFallbackView(previousViews, result.visitedViews as TagView[], context.targetView)
      );
    }

    return;
  }

  if (actionKey === 'closeOthers') {
    const result = await tagsViewStore.delOthersViews(context.targetView);
    const retainedViews = result.visitedViews as TagView[];
    const activeViewRetained = activeView ? retainedViews.some((view) => isSameView(view, activeView)) : false;

    if (!activeViewRetained) {
      await navigateToView(findTagView(retainedViews, context.targetView) ?? findFirstAffixView(retainedViews));
    }

    return;
  }

  const result = await tagsViewStore.delAllViews();
  await navigateToView(findFirstAffixView(result.visitedViews as TagView[]));
};

const handleTagMenuSelect = async (tag: TagView, value: string | number) => {
  if (!isTagActionKey(value)) {
    return;
  }

  await executeTagAction(value, {
    targetView: tag,
    source: 'contextmenu',
  });
};

const closeSelectedTag = (tag: TagView) => {
  void executeTagAction('closeCurrent', {
    targetView: tag,
    source: 'close-icon',
  });
};

watch(
  () => route.name,
  (newName) => {
    if (newName) {
      addTags();
    }
  }
);

onMounted(() => {
  initTags();
  addTags();
});
</script>

<style scoped>
.tags-view-container {
  height: 44px;
  width: 100%;
  display: flex;
  align-items: center;
  padding: 0 10px;
  background: transparent;
}

.tags-scroll-wrapper {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding: 4px;
}

.tags-scroll-wrapper::-webkit-scrollbar {
  display: none;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #4e5969;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  user-select: none;
  flex-shrink: 0;
  position: relative;
}

.tags-view-item:hover {
  background: #fff;
  color: #1d2129;
}

.tags-view-item.active {
  background: #fff;
  color: var(--bml-primary, #165dff);
  font-weight: 500;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.tag-title {
  margin-right: 4px;
  transition: margin 0.2s;
}

.tags-view-item:not(.active):not(:hover) .tag-close-icon {
  width: 0;
  opacity: 0;
  margin-left: 0;
  overflow: hidden;
}

.tags-view-item:hover .tag-close-icon,
.tags-view-item.active .tag-close-icon {
  width: 16px;
  opacity: 1;
  margin-left: 4px;
}

.tag-close-icon {
  width: 14px;
  height: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
  font-size: 8px;
  color: #86909c;
}

.tag-close-icon:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d2129;
}

.tags-dropdown-label {
  display: inline-flex;
  align-items: center;
  min-width: 112px;
}

.tags-dropdown-label.danger {
  color: #c53434;
}
</style>
