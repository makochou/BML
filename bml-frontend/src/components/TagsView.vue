
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
        <div
          class="tags-view-item"
          :class="{ active: isActive(tag) }"
          @click="navigate"
          @contextmenu.prevent="openMenu(tag, $event)"
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
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useTagsViewStore } from '../store/tagsView';
import type { TagView } from '../store/tagsView';
import { IconClose } from '@arco-design/web-vue/es/icon';

const route = useRoute();
const router = useRouter();
const tagsViewStore = useTagsViewStore();

const visitedViews = computed(() => tagsViewStore.visitedViews);

const isActive = (tag: TagView) => {
  return tag.path === route.path;
};

const isAffix = (tag: TagView) => {
  return tag.meta && tag.meta.affix;
};

const initTags = () => {
  const routes = router.getRoutes();
  const affixTags = routes.filter(route => route.meta && route.meta.affix);
  
  for (const tag of affixTags) {
    if (tag.name) {
      tagsViewStore.addVisitedView({
        ...tag,
        path: tag.path,
        fullPath: tag.path, // getRoutes() returns absolute paths
        meta: tag.meta
      } as TagView);
    }
  }
};

const addTags = () => {
  if (route.name) {
    tagsViewStore.addView(route);
  }
  return false;
};

const closeSelectedTag = (view: TagView) => {
  tagsViewStore.delView(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews as TagView[], view);
    }
  });
};

const toLastView = (visitedViews: TagView[], view: TagView) => {
  const latestView = visitedViews.slice(-1)[0];
  if (latestView) {
    router.push(latestView.fullPath as string);
  } else {
    // If no tags left, default to Dashboard (which should be affixed anyway)
    if (view.name === 'Dashboard') {
      router.replace({ path: '/redirect' + view.fullPath });
    } else {
      router.push('/');
    }
  }
};

const openMenu = (tag: TagView, e: MouseEvent) => {
    // Context menu logic (optional for MVP)
    console.log('Context menu', tag, e);
};

watch(() => route.path, () => {
  addTags();
});

onMounted(() => {
  initTags();
  addTags();
});
</script>

<style scoped>
.tags-view-container {
  height: 44px; /* Slightly compact height */
  width: 100%;
  background: transparent;
  display: flex;
  align-items: center;
  padding: 0 10px; /* Reduced padding for left alignment */
}

.tags-scroll-wrapper {
  display: flex;
  align-items: center;
  gap: 6px; /* Tighter gap */
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
  height: 30px; /* More refined height */
  padding: 0 12px;
  border-radius: 6px; /* Slightly squarer, more technical look */
  font-size: 13px;
  color: #4e5969;
  
  /* Inactive: Subtle transparency */
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid transparent; /* Placeholder to prevent layout shift */
  
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

/* Active State: Clean White Pill */
.tags-view-item.active {
  background: #fff;
  color: #165dff;
  font-weight: 500;
  /* Crisp shadow instead of glow */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

/* Removed ::after (the bottom line) as requested */

.tag-title {
  margin-right: 4px;
  transition: margin 0.2s;
}

/* Close Icon Logic: Show on Hover/Active */
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
  font-size: 8px; /* Delicate icon */
  color: #86909c;
}

.tag-close-icon:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d2129;
}
</style>
