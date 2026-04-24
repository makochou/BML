import { defineStore } from 'pinia';
import type { RouteLocationNormalized } from 'vue-router';

export interface TagView extends Partial<RouteLocationNormalized> {
  title?: string;
}

interface TagsViewState {
  visitedViews: TagView[];
  cachedViews: string[];
}

const isAffixView = (view: TagView): boolean => {
  return Boolean(view.meta?.affix);
};

const isSameView = (source: TagView, target: TagView): boolean => {
  return source.path === target.path;
};

const resolveCacheViewName = (view: TagView): string | null => {
  if (!view.name || view.meta?.noCache) {
    return null;
  }

  const name = String(view.name);
  // 调试：打印缓存名称，帮助排查 keep-alive 不生效的问题
  console.debug('[TagsView] resolveCacheViewName:', name, '| noCache:', view.meta?.noCache);
  return name;
};

const buildStateSnapshot = (state: TagsViewState): TagsViewState => {
  return {
    visitedViews: [...state.visitedViews],
    cachedViews: [...state.cachedViews],
  };
};

const rebuildCachedViewsByVisitedViews = (retainedViews: TagView[]): string[] => {
  const cachedViews: string[] = [];

  for (const retainedView of retainedViews) {
    const cacheViewName = resolveCacheViewName(retainedView);
    if (cacheViewName && !cachedViews.includes(cacheViewName)) {
      cachedViews.push(cacheViewName);
    }
  }

  return cachedViews;
};

export const useTagsViewStore = defineStore('tagsView', {
  state: (): TagsViewState => ({
    visitedViews: [],
    cachedViews: [],
  }),
  actions: {
    addView(view: TagView) {
      this.addVisitedView(view);
      this.addCachedView(view);
    },
    addVisitedView(view: TagView) {
      if (this.visitedViews.some((visitedView) => isSameView(visitedView, view))) {
        return;
      }

      this.visitedViews.push({
        ...view,
        title: typeof view.meta?.title === 'string' ? view.meta.title : 'no-name',
      });
    },
    addCachedView(view: TagView) {
      const cacheViewName = resolveCacheViewName(view);
      if (cacheViewName && !this.cachedViews.includes(cacheViewName)) {
        this.cachedViews.push(cacheViewName);
      }
    },
    delView(view: TagView): Promise<TagsViewState> {
      if (isAffixView(view)) {
        return Promise.resolve(buildStateSnapshot(this));
      }

      this.visitedViews = this.visitedViews.filter((visitedView) => !isSameView(visitedView, view));
      this.cachedViews = rebuildCachedViewsByVisitedViews(this.visitedViews);

      return Promise.resolve(buildStateSnapshot(this));
    },
    delOthersViews(view: TagView): Promise<TagsViewState> {
      this.visitedViews = this.visitedViews.filter((visitedView) => {
        return isAffixView(visitedView) || isSameView(visitedView, view);
      });

      // Rebuild the cache from retained tabs so keep-alive always matches the visible tag list.
      this.cachedViews = rebuildCachedViewsByVisitedViews(this.visitedViews);

      return Promise.resolve(buildStateSnapshot(this));
    },
    delAllViews(): Promise<TagsViewState> {
      this.visitedViews = this.visitedViews.filter((visitedView) => isAffixView(visitedView));
      this.cachedViews = rebuildCachedViewsByVisitedViews(this.visitedViews);

      return Promise.resolve(buildStateSnapshot(this));
    },
  },
});
