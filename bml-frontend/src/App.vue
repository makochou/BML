<template>
  <router-view />
</template>

<script setup lang="ts">
import { onErrorCaptured } from 'vue';

// 捕获所有子组件错误，防止白屏
onErrorCaptured((err, instance, info) => {
  console.error('Vue Error Captured:', err);
  console.error('Component:', instance);
  console.error('Error Info:', info);
  // 返回 false 会阻止错误继续向上传播
  // 返回 true 或不返回会继续传播
  return false;
});
</script>

<style>
/* 全局样式 */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
}
#app {
  height: 100%;
}
</style>
