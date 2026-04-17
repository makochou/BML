/// <reference types="vite/client" />

/**
 * Vue 单文件组件类型声明。
 * 说明：
 * 1. 统一为 .vue 文件提供模块类型，避免在 vue-tsc / IDE 中出现“找不到模块”提示；
 * 2. 声明文件放在 src 目录下，自动被 tsconfig.app.json 纳入编译范围；
 * 3. 后续若接入更细粒度的全局类型，也建议集中放在该目录统一维护。
 */
declare module '*.vue' {
  import type { DefineComponent } from 'vue';

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}
