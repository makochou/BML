<template>
  <!--
    通用审计信息组件（标题栏两行展示）
    ════════════════════════════════════
    在弹窗标题栏右侧以两行方式展示新增/修改审计信息：
      新增人：xxx    新增时间：xxx
      修改人：xxx    修改时间：xxx
    仅在编辑/查看已有数据时显示（新增时自动隐藏）。

    使用方式（放在 BmlModal 的 header-extra 插槽中）：
      <template #header-extra>
        <AuditInfoFooter :data="formData" />
      </template>
  -->
  <span v-if="visible" class="audit-meta">
    <span class="audit-meta__row">
      <span class="audit-meta__field"><span class="audit-meta__lbl">新增人：</span><b>{{ createByDisplay }}</b></span>
      <span class="audit-meta__field"><span class="audit-meta__lbl">新增时间：</span>{{ formatTime(data.createTime) }}</span>
    </span>
    <span class="audit-meta__row">
      <span class="audit-meta__field"><span class="audit-meta__lbl">修改人：</span><b>{{ updateByDisplay }}</b></span>
      <span class="audit-meta__field"><span class="audit-meta__lbl">修改时间：</span>{{ formatTime(data.updateTime) }}</span>
    </span>
  </span>
</template>

<script lang="ts" setup>
/**
 * 通用审计信息组件（标题栏两行版）
 *
 * 功能说明：
 *   在弹窗标题栏以两行方式展示数据的新增/修改人和时间。
 *   通过 useUserNameCache 自动将用户 ID 解析为用户名称。
 *   仅在编辑或查看已有数据时显示（新增时自动隐藏）。
 */
import { computed, watch } from 'vue';
import { useUserNameCache } from '../../composables/useUserNameCache';

const props = defineProps<{
  /** 表单数据对象，需包含 id/createTime/createBy/updateTime/updateBy 字段 */
  data: Record<string, any>;
}>();

const { resolveUserName, cache } = useUserNameCache();

/** 仅在编辑/查看模式下显示（有 id 即为已有数据） */
const visible = computed(() => !!props.data?.id);

/** 当数据变化时，预加载用户名称 */
watch(
  () => [props.data?.createBy, props.data?.updateBy],
  ([createBy, updateBy]) => {
    if (createBy) resolveUserName(createBy);
    if (updateBy) resolveUserName(updateBy);
  },
  { immediate: true }
);

/** 新增人显示名称 */
const createByDisplay = computed(() => {
  const id = props.data?.createBy;
  if (!id) return '—';
  return cache[String(id)] || '...';
});

/** 修改人显示名称 */
const updateByDisplay = computed(() => {
  const id = props.data?.updateBy;
  if (!id) return '—';
  return cache[String(id)] || '...';
});

/** 格式化时间显示（精简到分钟） */
const formatTime = (time: string | null | undefined): string => {
  if (!time) return '—';
  return String(time).replace('T', ' ').substring(0, 16);
};
</script>

<style scoped>
.audit-meta {
  display: inline-flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 14px;
  vertical-align: middle;
}

.audit-meta__row {
  display: flex;
  align-items: center;
  gap: 16px;
  white-space: nowrap;
  font-size: 10px;
  color: var(--color-text-3, #86909c);
  line-height: 1.1;
}

.audit-meta__field {
  display: inline-flex;
  align-items: center;
  min-width: 150px;
}

.audit-meta__lbl {
  display: inline-block;
  width: 45px;
  text-align-last: justify;
  flex-shrink: 0;
}

.audit-meta__field b {
  font-weight: 500;
  color: var(--color-text-2, #4e5969);
}
</style>
