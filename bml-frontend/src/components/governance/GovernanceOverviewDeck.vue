<template>
  <section
    class="governance-overview-deck"
    :class="[`theme-${theme}`, `align-${align}`, { 'density-compact': compact }]"
    :style="{ '--overview-max-width': maxWidth || '100%' }"
  >
    <header class="governance-overview-deck__hero">
      <div class="governance-overview-deck__main">
        <div class="governance-overview-deck__heading">
          <p class="governance-overview-deck__eyebrow">{{ eyebrow }}</p>
          <span v-if="badge" class="governance-overview-deck__badge">{{ badge }}</span>
        </div>

        <div class="governance-overview-deck__title-block">
          <component :is="titleTag" class="governance-overview-deck__title">
            {{ title }}
          </component>
          <p class="governance-overview-deck__description">{{ description }}</p>
        </div>

        <div
          v-if="tags.length || $slots.actions"
          class="governance-overview-deck__toolbar"
        >
          <div v-if="tags.length" class="governance-overview-deck__chip-row">
            <span
              v-for="item in tags"
              :key="item"
              class="governance-overview-deck__chip"
            >
              {{ item }}
            </span>
          </div>
          <div v-if="$slots.actions" class="governance-overview-deck__actions">
            <slot name="actions" />
          </div>
        </div>
      </div>

      <div v-if="heroStats.length" class="governance-overview-deck__hero-side">
        <div class="governance-overview-deck__hero-stats">
          <article
            v-for="item in heroStats"
            :key="item.label"
            class="governance-overview-deck__hero-stat"
            :class="`tone-${item.tone}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </div>
    </header>

    <div v-if="summaryCards.length" class="governance-overview-deck__summary-grid">
      <article
        v-for="item in summaryCards"
        :key="item.title"
        class="governance-overview-deck__summary-card"
        :class="`accent-${item.accent}`"
      >
        <div class="governance-overview-deck__summary-icon" :class="`accent-${item.accent}`">
          <component :is="item.icon" />
        </div>
        <div class="governance-overview-deck__summary-copy">
          <span>{{ item.title }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </article>
    </div>
  </section>
</template>

<script lang="ts" setup>
import type {
  GovernanceOverviewCard,
  GovernanceWorkbenchTheme,
  WorkbenchStatCard
} from '../../types/governance';

/**
 * 通用顶部概览 Deck。
 * 统一承载“标题说明 / 能力标签 / 操作按钮 / 快捷 KPI / 业务摘要卡”，
 * 适用于需要在首屏同时展示概览信息与快捷动作的治理类页面。
 * 组件支持最大宽度、对齐方式和紧凑密度控制，便于在不同页面复用相同结构。
 */
withDefaults(
  defineProps<{
    eyebrow: string;
    title: string;
    description: string;
    badge?: string;
    tags?: string[];
    heroStats?: WorkbenchStatCard[];
    summaryCards?: GovernanceOverviewCard[];
    theme?: GovernanceWorkbenchTheme;
    titleTag?: 'h1' | 'h2' | 'h3';
    maxWidth?: string;
    align?: 'start' | 'center';
    compact?: boolean;
  }>(),
  {
    badge: '',
    tags: () => [],
    heroStats: () => [],
    summaryCards: () => [],
    theme: 'ocean',
    titleTag: 'h1',
    maxWidth: '',
    align: 'start',
    compact: false
  }
);
</script>

<style scoped>
.governance-overview-deck {
  --overview-hero-start: #165dff;
  --overview-hero-mid: #1e7ee9;
  --overview-hero-end: #14b8a6;
  --overview-surface: linear-gradient(180deg, #f6faff, #eef4fa);
  --overview-surface-border: rgba(219, 230, 243, 0.92);
  width: min(100%, var(--overview-max-width));
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.governance-overview-deck.align-start {
  margin-right: auto;
}

.governance-overview-deck.align-center {
  margin-inline: auto;
}

.governance-overview-deck.theme-emerald {
  --overview-hero-start: #0f8d66;
  --overview-hero-mid: #119a74;
  --overview-hero-end: #18b2ad;
}

.governance-overview-deck.theme-teal {
  --overview-hero-start: #0f766e;
  --overview-hero-mid: #108c87;
  --overview-hero-end: #16b2ab;
}

.governance-overview-deck.theme-violet {
  --overview-hero-start: #4338ca;
  --overview-hero-mid: #5b35d5;
  --overview-hero-end: #7c3aed;
}

.governance-overview-deck__hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(360px, 0.92fr);
  gap: 20px;
  padding: 24px 26px;
  overflow: hidden;
  border-radius: 30px;
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.22), transparent 34%),
    radial-gradient(circle at bottom left, rgba(255, 255, 255, 0.08), transparent 28%),
    linear-gradient(135deg, var(--overview-hero-start), var(--overview-hero-mid) 52%, var(--overview-hero-end));
  box-shadow: 0 26px 60px rgba(15, 23, 42, 0.14);
}

.governance-overview-deck__hero::before,
.governance-overview-deck__hero::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.governance-overview-deck__hero::before {
  top: 16px;
  right: 18px;
  width: 190px;
  height: 190px;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  opacity: 0.36;
}

.governance-overview-deck__hero::after {
  inset: auto -70px -98px auto;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(10px);
}

.governance-overview-deck__main,
.governance-overview-deck__hero-side {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.governance-overview-deck__main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.governance-overview-deck__heading {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.governance-overview-deck__eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.82);
}

.governance-overview-deck__badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.94);
}

.governance-overview-deck__title-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.governance-overview-deck__title {
  margin: 0;
  font-size: clamp(34px, 3.4vw, 52px);
  line-height: 1.06;
  letter-spacing: -0.02em;
  color: #ffffff;
}

.governance-overview-deck__description {
  max-width: 760px;
  margin: 0;
  font-size: 16px;
  line-height: 1.82;
  color: rgba(255, 255, 255, 0.9);
}

.governance-overview-deck__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 2px;
}

.governance-overview-deck__chip-row,
.governance-overview-deck__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.governance-overview-deck__chip {
  display: inline-flex;
  align-items: center;
  padding: 9px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(10, 28, 49, 0.16);
  backdrop-filter: blur(10px);
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.92);
}

.governance-overview-deck__actions {
  justify-content: flex-end;
}

.governance-overview-deck__actions :deep(.arco-btn) {
  min-width: 126px;
  height: 44px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  font-weight: 700;
}

.governance-overview-deck__actions :deep(.arco-btn-primary) {
  background: linear-gradient(135deg, #1be0bd, #14c7a5);
  box-shadow: 0 14px 28px rgba(20, 199, 165, 0.24);
}

.governance-overview-deck__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  color: #0f172a;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
}

.governance-overview-deck__hero-side {
  display: flex;
  align-items: stretch;
}

.governance-overview-deck__hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.governance-overview-deck__hero-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 138px;
  padding: 16px 16px 14px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
}

.governance-overview-deck__hero-stat span,
.governance-overview-deck__hero-stat small {
  font-size: 12px;
  line-height: 1.68;
  color: rgba(255, 255, 255, 0.76);
}

.governance-overview-deck__hero-stat strong {
  font-size: 34px;
  line-height: 1;
  color: #ffffff;
}

.governance-overview-deck__hero-stat.tone-blue {
  border-color: rgba(191, 219, 254, 0.28);
}

.governance-overview-deck__hero-stat.tone-green {
  border-color: rgba(187, 247, 208, 0.26);
}

.governance-overview-deck__hero-stat.tone-teal {
  border-color: rgba(153, 246, 228, 0.26);
}

.governance-overview-deck__hero-stat.tone-gold {
  border-color: rgba(253, 230, 138, 0.28);
}

.governance-overview-deck__hero-stat.tone-violet {
  border-color: rgba(216, 180, 254, 0.24);
}

.governance-overview-deck__summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.governance-overview-deck__summary-card {
  position: relative;
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 18px 18px 16px;
  overflow: hidden;
  border: 1px solid var(--overview-surface-border);
  border-radius: 24px;
  background: var(--overview-surface);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.06);
}

.governance-overview-deck__summary-card::before {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  content: '';
  opacity: 0.94;
}

.governance-overview-deck__summary-card.accent-blue::before {
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.governance-overview-deck__summary-card.accent-green::before {
  background: linear-gradient(90deg, #059669, #34d399);
}

.governance-overview-deck__summary-card.accent-teal::before {
  background: linear-gradient(90deg, #0f766e, #2dd4bf);
}

.governance-overview-deck__summary-card.accent-gold::before {
  background: linear-gradient(90deg, #d97706, #fbbf24);
}

.governance-overview-deck__summary-card.accent-violet::before {
  background: linear-gradient(90deg, #6d28d9, #a78bfa);
}

.governance-overview-deck__summary-icon {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  color: #ffffff;
  font-size: 24px;
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.12);
}

.governance-overview-deck__summary-icon.accent-blue {
  background: linear-gradient(135deg, #2563eb, #4f86ff);
}

.governance-overview-deck__summary-icon.accent-green {
  background: linear-gradient(135deg, #059669, #10b981);
}

.governance-overview-deck__summary-icon.accent-teal {
  background: linear-gradient(135deg, #0f766e, #14b8a6);
}

.governance-overview-deck__summary-icon.accent-gold {
  background: linear-gradient(135deg, #d97706, #fbbf24);
}

.governance-overview-deck__summary-icon.accent-violet {
  background: linear-gradient(135deg, #6d28d9, #8b5cf6);
}

.governance-overview-deck__summary-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.governance-overview-deck__summary-copy span,
.governance-overview-deck__summary-copy small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.65;
}

.governance-overview-deck__summary-copy strong {
  color: #0f172a;
  font-size: 34px;
  line-height: 1.08;
  letter-spacing: -0.02em;
}

.governance-overview-deck.density-compact {
  gap: 12px;
}

.governance-overview-deck.density-compact .governance-overview-deck__hero {
  grid-template-columns: minmax(0, 1.22fr) minmax(320px, 0.84fr);
  gap: 18px;
  padding: 20px 22px;
  border-radius: 26px;
}

.governance-overview-deck.density-compact .governance-overview-deck__hero::before {
  top: 14px;
  right: 16px;
  width: 150px;
  height: 150px;
  border-radius: 28px;
}

.governance-overview-deck.density-compact .governance-overview-deck__hero::after {
  inset: auto -56px -84px auto;
  width: 180px;
  height: 180px;
}

.governance-overview-deck.density-compact .governance-overview-deck__main {
  gap: 14px;
}

.governance-overview-deck.density-compact .governance-overview-deck__title-block {
  gap: 8px;
}

.governance-overview-deck.density-compact .governance-overview-deck__title {
  font-size: clamp(28px, 2.8vw, 42px);
}

.governance-overview-deck.density-compact .governance-overview-deck__description {
  max-width: 640px;
  font-size: 15px;
  line-height: 1.72;
}

.governance-overview-deck.density-compact .governance-overview-deck__toolbar {
  gap: 14px;
}

.governance-overview-deck.density-compact .governance-overview-deck__chip {
  padding: 8px 13px;
}

.governance-overview-deck.density-compact .governance-overview-deck__actions :deep(.arco-btn) {
  min-width: 118px;
  height: 40px;
  padding: 0 16px;
}

.governance-overview-deck.density-compact .governance-overview-deck__hero-stat {
  min-height: 112px;
  padding: 14px 14px 12px;
  border-radius: 20px;
}

.governance-overview-deck.density-compact .governance-overview-deck__hero-stat strong {
  font-size: 28px;
}

.governance-overview-deck.density-compact .governance-overview-deck__summary-grid {
  gap: 12px;
}

.governance-overview-deck.density-compact .governance-overview-deck__summary-card {
  grid-template-columns: 52px minmax(0, 1fr);
  gap: 12px;
  padding: 15px 16px 14px;
  border-radius: 22px;
}

.governance-overview-deck.density-compact .governance-overview-deck__summary-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  font-size: 22px;
}

.governance-overview-deck.density-compact .governance-overview-deck__summary-copy strong {
  font-size: 28px;
}

@media (max-width: 1200px) {
  .governance-overview-deck__hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .governance-overview-deck__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .governance-overview-deck__actions {
    justify-content: flex-start;
  }
}

@media (max-width: 1280px) {
  .governance-overview-deck__summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .governance-overview-deck__hero-stats,
  .governance-overview-deck__summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .governance-overview-deck {
    gap: 12px;
    width: 100%;
  }

  .governance-overview-deck__hero {
    padding: 20px;
    border-radius: 24px;
  }

  .governance-overview-deck__title {
    font-size: 30px;
  }

  .governance-overview-deck__summary-card {
    grid-template-columns: 52px minmax(0, 1fr);
    padding: 16px;
  }

  .governance-overview-deck__summary-icon {
    width: 52px;
    height: 52px;
    border-radius: 16px;
    font-size: 22px;
  }

  .governance-overview-deck__actions :deep(.arco-btn) {
    width: 100%;
  }
}
</style>
