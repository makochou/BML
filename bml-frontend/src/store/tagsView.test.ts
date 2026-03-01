import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useTagsViewStore, type TagView } from './tagsView';

const createTagView = (
  path: string,
  name: string,
  options?: {
    affix?: boolean;
    noCache?: boolean;
    title?: string;
  }
): TagView => {
  return {
    path,
    fullPath: path,
    name,
    meta: {
      title: options?.title ?? name,
      affix: options?.affix,
      noCache: options?.noCache,
    },
  } as TagView;
};

describe('tagsView store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  it('deduplicates visited views by path', () => {
    const store = useTagsViewStore();
    const apiListView = createTagView('/admin/api/list', 'ApiList');

    store.addView(apiListView);
    store.addView({
      ...apiListView,
      fullPath: '/admin/api/list?tab=debug',
    });

    expect(store.visitedViews).toHaveLength(1);
    expect(store.cachedViews).toEqual(['ApiList']);
  });

  it('removes normal view and rebuilds cached views together', async () => {
    const store = useTagsViewStore();
    const dashboardView = createTagView('/admin/dashboard', 'Dashboard', { affix: true });
    const apiListView = createTagView('/admin/api/list', 'ApiList');
    const apiDebugView = createTagView('/admin/api/debug', 'ApiDebug');

    store.addView(dashboardView);
    store.addView(apiListView);
    store.addView(apiDebugView);

    await store.delView(apiListView);

    expect(store.visitedViews.map((view) => view.path)).toEqual([
      '/admin/dashboard',
      '/admin/api/debug',
    ]);
    expect(store.cachedViews).toEqual(['Dashboard', 'ApiDebug']);
  });

  it('does not remove affix view', async () => {
    const store = useTagsViewStore();
    const dashboardView = createTagView('/admin/dashboard', 'Dashboard', { affix: true });

    store.addView(dashboardView);

    await store.delView(dashboardView);

    expect(store.visitedViews.map((view) => view.path)).toEqual(['/admin/dashboard']);
    expect(store.cachedViews).toEqual(['Dashboard']);
  });

  it('keeps target view and affix views when closing others', async () => {
    const store = useTagsViewStore();
    const dashboardView = createTagView('/admin/dashboard', 'Dashboard', { affix: true });
    const apiListView = createTagView('/admin/api/list', 'ApiList');
    const apiDebugView = createTagView('/admin/api/debug', 'ApiDebug');

    store.addView(dashboardView);
    store.addView(apiListView);
    store.addView(apiDebugView);

    await store.delOthersViews(apiDebugView);

    expect(store.visitedViews.map((view) => view.path)).toEqual([
      '/admin/dashboard',
      '/admin/api/debug',
    ]);
    expect(store.cachedViews).toEqual(['Dashboard', 'ApiDebug']);
  });

  it('rebuilds cached views after closing others and skips no-cache view', async () => {
    const store = useTagsViewStore();
    const dashboardView = createTagView('/admin/dashboard', 'Dashboard', { affix: true });
    const appListView = createTagView('/admin/app', 'AppList', { noCache: true });
    const apiDebugView = createTagView('/admin/api/debug', 'ApiDebug');

    store.addView(dashboardView);
    store.addView(appListView);
    store.addView(apiDebugView);

    await store.delOthersViews(appListView);

    expect(store.visitedViews.map((view) => view.path)).toEqual([
      '/admin/dashboard',
      '/admin/app',
    ]);
    expect(store.cachedViews).toEqual(['Dashboard']);
  });

  it('keeps only affix views after closing all', async () => {
    const store = useTagsViewStore();
    const dashboardView = createTagView('/admin/dashboard', 'Dashboard', { affix: true });
    const apiListView = createTagView('/admin/api/list', 'ApiList');

    store.addView(dashboardView);
    store.addView(apiListView);

    await store.delAllViews();

    expect(store.visitedViews.map((view) => view.path)).toEqual(['/admin/dashboard']);
    expect(store.cachedViews).toEqual(['Dashboard']);
  });

  it('returns empty cached views when all retained affix views are no-cache', async () => {
    const store = useTagsViewStore();
    const affixNoCacheView = createTagView('/admin/feature-disabled', 'FeatureDisabled', {
      affix: true,
      noCache: true,
    });
    const apiListView = createTagView('/admin/api/list', 'ApiList');

    store.addView(affixNoCacheView);
    store.addView(apiListView);

    await store.delAllViews();

    expect(store.visitedViews.map((view) => view.path)).toEqual(['/admin/feature-disabled']);
    expect(store.cachedViews).toEqual([]);
  });

  it('never adds no-cache view into cached views', () => {
    const store = useTagsViewStore();
    const appListView = createTagView('/admin/app', 'AppList', { noCache: true });

    store.addView(appListView);

    expect(store.visitedViews.map((view) => view.path)).toEqual(['/admin/app']);
    expect(store.cachedViews).toEqual([]);
  });
});
