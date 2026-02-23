import { defineStore } from 'pinia';
import type { RouteLocationNormalized } from 'vue-router';

export interface TagView extends Partial<RouteLocationNormalized> {
    title?: string;
}

interface TagsViewState {
    visitedViews: TagView[];
    cachedViews: string[];
}

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
            if (this.visitedViews.some((v) => v.path === view.path)) return;
            this.visitedViews.push(Object.assign({}, view, {
                title: view.meta?.title || 'no-name'
            }));
        },
        addCachedView(view: TagView) {
            if (view.name && !this.cachedViews.includes(view.name as string)) {
                if (!view.meta?.noCache) {
                    this.cachedViews.push(view.name as string);
                }
            }
        },
        delView(view: TagView): Promise<TagsViewState> {
            return new Promise((resolve) => {
                this.delVisitedView(view);
                this.delCachedView(view);
                resolve({
                    visitedViews: [...this.visitedViews],
                    cachedViews: [...this.cachedViews],
                });
            });
        },
        delVisitedView(view: TagView) {
            for (const [i, v] of this.visitedViews.entries()) {
                if (v.path === view.path) {
                    this.visitedViews.splice(i, 1);
                    break;
                }
            }
        },
        delCachedView(view: TagView) {
            if (view.name) {
                const index = this.cachedViews.indexOf(view.name as string);
                index > -1 && this.cachedViews.splice(index, 1);
            }
        },
        delOthersViews(view: TagView): Promise<TagsViewState> {
            return new Promise((resolve) => {
                this.delOthersVisitedViews(view);
                this.delOthersCachedViews(view);
                resolve({
                    visitedViews: [...this.visitedViews],
                    cachedViews: [...this.cachedViews],
                });
            });
        },
        delOthersVisitedViews(view: TagView) {
            this.visitedViews = this.visitedViews.filter((v) => {
                return v.meta?.affix || v.path === view.path;
            });
        },
        delOthersCachedViews(view: TagView) {
            if (view.name) {
                const index = this.cachedViews.indexOf(view.name as string);
                if (index > -1) {
                    this.cachedViews = this.cachedViews.slice(index, index + 1);
                } else {
                    this.cachedViews = [];
                }
            }
        },
        delAllViews(): Promise<TagsViewState> {
            return new Promise((resolve) => {
                this.delAllVisitedViews();
                this.delAllCachedViews();
                resolve({
                    visitedViews: [...this.visitedViews],
                    cachedViews: [...this.cachedViews],
                });
            });
        },
        delAllVisitedViews() {
            // keep affix tags
            const affixTags = this.visitedViews.filter((tag) => tag.meta?.affix);
            this.visitedViews = affixTags;
        },
        delAllCachedViews() {
            this.cachedViews = [];
        }
    },
});
