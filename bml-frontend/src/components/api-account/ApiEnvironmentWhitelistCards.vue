<template>
  <div class="api-environment-whitelist-grid">
    <article
      v-for="environment in whitelistCards"
      :key="environment.value"
      class="api-environment-whitelist-card"
      :class="{ active: environment.active }"
    >
      <div class="api-environment-whitelist-card__head">
        <a-tag :color="getEnvironmentTagColor(environment.value)">{{ environment.label }}</a-tag>
        <span>{{ environment.active ? activeLabel : standbyLabel }}</span>
      </div>
      <div class="api-environment-whitelist-card__body">
        <strong>{{ environment.entries.length ? `${environment.entries.length} 条来源` : '未限制' }}</strong>
        <div v-if="environment.entries.length" class="api-environment-whitelist-card__tags">
          <a-tag
            v-for="entry in environment.entries"
            :key="`${environment.value}-${entry}`"
            color="cyan"
          >
            {{ entry }}
          </a-tag>
        </div>
        <small v-else>{{ emptyHint }}</small>
      </div>
    </article>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { AccessEnvironment, ApiAccountDetail, ApiAccountItem, ApiCredentialPayload } from '../../types/apiAccount';
import { environmentOptions, getEnvironmentTagColor, getWhitelistByEnvironment } from '../../utils/api-account-governance';

type GovernanceWhitelistPayload = Pick<
  ApiAccountItem | ApiAccountDetail | ApiCredentialPayload,
  'environmentIpWhitelist' | 'accessEnvironment' | 'ipWhitelist'
>;

/**
 * 通用环境白名单卡片组。
 * 统一展示测试、预发、生产三套白名单，并突出当前实际生效环境，
 * 适用于详情页、凭证查看页及后续需要回显环境治理结果的页面。
 */
const props = withDefaults(defineProps<{
  payload: GovernanceWhitelistPayload;
  activeLabel?: string;
  standbyLabel?: string;
  emptyHint?: string;
}>(), {
  activeLabel: '当前生效',
  standbyLabel: '待命环境',
  emptyHint: '当前环境未配置 IP 限制，默认允许全部来源。'
});

const whitelistCards = computed(() => environmentOptions.map(environment => ({
  label: environment.label,
  value: environment.value as AccessEnvironment,
  entries: getWhitelistByEnvironment(props.payload, environment.value),
  active: props.payload.accessEnvironment === environment.value
})));
</script>

<style scoped>
.api-environment-whitelist-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.api-environment-whitelist-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.86);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.05);
}

.api-environment-whitelist-card.active {
  border-color: rgba(16, 185, 129, 0.28);
  box-shadow: 0 20px 42px rgba(16, 185, 129, 0.16);
}

.api-environment-whitelist-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.api-environment-whitelist-card__head span,
.api-environment-whitelist-card__body small {
  color: #64748b;
}

.api-environment-whitelist-card__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.api-environment-whitelist-card__body strong {
  color: #0f172a;
}

.api-environment-whitelist-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1280px) {
  .api-environment-whitelist-grid {
    grid-template-columns: 1fr;
  }
}
</style>
