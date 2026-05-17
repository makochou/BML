<template>
  <!--
    用户名称单元格组件
    ════════════════════
    在表格中将用户 ID 自动解析为用户名称（nickname）显示。
    使用全局用户名称缓存，避免重复请求。

    使用方式：
      <template #createBy="{ record }">
        <UserNameCell :user-id="record.createBy" />
      </template>
  -->
  <span class="user-name-cell">{{ displayName }}</span>
</template>

<script lang="ts" setup>
/**
 * 用户名称单元格组件
 *
 * 将用户 ID 自动解析为用户名称，用于表格列渲染。
 * 使用 useUserNameCache 全局缓存，同一 ID 只请求一次。
 */
import { computed, watch } from 'vue';
import { useUserNameCache } from '../../composables/useUserNameCache';

const props = defineProps<{
  /** 用户 ID（支持 number 或 string，兼容雪花 ID） */
  userId: number | string | null | undefined;
}>();

const { resolveUserName, cache } = useUserNameCache();

/** 触发名称解析 */
watch(
  () => props.userId,
  (id) => { if (id) resolveUserName(id); },
  { immediate: true }
);

/** 显示名称 */
const displayName = computed(() => {
  if (!props.userId) return '—';
  return cache[String(props.userId)] || '...';
});
</script>

<style scoped>
.user-name-cell {
  font-size: inherit;
  color: inherit;
}
</style>
