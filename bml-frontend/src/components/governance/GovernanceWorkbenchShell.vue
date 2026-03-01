<template>
  <div class="governance-workbench" :class="[`theme-${theme}`, { 'hero-sticky': stickyHero }]">
    <header class="governance-workbench__hero" :class="{ sticky: stickyHero }">
      <div class="governance-workbench__hero-main">
        <p class="governance-workbench__eyebrow">{{ eyebrow }}</p>
        <div class="governance-workbench__title-row">
          <h2>{{ title }}</h2>
          <div v-if="$slots.titleBadge" class="governance-workbench__title-badge">
            <slot name="titleBadge" />
          </div>
        </div>
        <p class="governance-workbench__desc">{{ description }}</p>
        <div v-if="tags.length" class="governance-workbench__chip-row">
          <span v-for="item in tags" :key="item" class="governance-workbench__chip">{{ item }}</span>
        </div>
      </div>

      <div
        v-if="stats.length || $slots.heroActions || $slots.heroSide"
        class="governance-workbench__hero-side"
      >
        <slot name="heroSide" />
        <div v-if="stats.length" class="governance-workbench__stats">
          <article
            v-for="item in stats"
            :key="item.label"
            class="governance-workbench__stat"
            :class="`tone-${item.tone}`"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.hint }}</small>
          </article>
        </div>
        <div v-if="$slots.heroActions" class="governance-workbench__actions">
          <slot name="heroActions" />
        </div>
      </div>
    </header>

    <div class="governance-workbench__layout" :class="{ 'has-aside': !!$slots.aside }">
      <aside v-if="$slots.aside" class="governance-workbench__aside">
        <slot name="aside" />
      </aside>
      <section class="governance-workbench__main">
        <slot />
      </section>
    </div>

    <footer v-if="$slots.footer" class="governance-workbench__footer">
      <slot name="footer" />
    </footer>
  </div>
</template>

<script lang="ts" setup>
import type { GovernanceWorkbenchTheme, WorkbenchStatCard } from '../../types/governance';

withDefaults(
  defineProps<{
    eyebrow: string;
    title: string;
    description: string;
    tags?: string[];
    stats?: WorkbenchStatCard[];
    theme?: GovernanceWorkbenchTheme;
    stickyHero?: boolean;
  }>(),
  {
    tags: () => [],
    stats: () => [],
    theme: 'ocean',
    stickyHero: false
  }
);
</script>

<style scoped>
.governance-workbench {
  --workbench-hero-start: #1769ff;
  --workbench-hero-mid: #1490d4;
  --workbench-hero-end: #12b8a6;
  --workbench-chip-bg: rgba(7, 24, 44, 0.18);
  --workbench-chip-border: rgba(255, 255, 255, 0.14);
  --workbench-hero-shadow: rgba(15, 23, 42, 0.14);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.governance-workbench.theme-emerald {
  --workbench-hero-start: #0f8d66;
  --workbench-hero-mid: #15a37b;
  --workbench-hero-end: #18b2ad;
}

.governance-workbench.theme-ocean {
  --workbench-hero-start: #165dff;
  --workbench-hero-mid: #1976d2;
  --workbench-hero-end: #11b2ba;
}

.governance-workbench.theme-teal {
  --workbench-hero-start: #0f766e;
  --workbench-hero-mid: #0f9f8f;
  --workbench-hero-end: #14b8a6;
}

.governance-workbench.theme-violet {
  --workbench-hero-start: #4338ca;
  --workbench-hero-mid: #6d28d9;
  --workbench-hero-end: #9333ea;
}

.governance-workbench__hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 1fr);
  gap: 18px;
  padding: 24px 26px;
  border-radius: 28px;
  overflow: hidden;
  color: #ffffff;
  background:
    radial-gradient(circle at top right, rgba(255, 255, 255, 0.18), transparent 35%),
    linear-gradient(135deg, var(--workbench-hero-start), var(--workbench-hero-mid) 55%, var(--workbench-hero-end));
  box-shadow: 0 28px 64px var(--workbench-hero-shadow);
}

.governance-workbench__hero::before,
.governance-workbench__hero::after {
  position: absolute;
  content: '';
  pointer-events: none;
}

.governance-workbench__hero::before {
  inset: auto -72px -110px auto;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(12px);
}

.governance-workbench__hero::after {
  top: 18px;
  right: 22px;
  width: 176px;
  height: 176px;
  border-radius: 34px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  opacity: 0.44;
}

.governance-workbench__hero.sticky {
  position: sticky;
  top: 0;
  z-index: 4;
}

.governance-workbench__hero-main,
.governance-workbench__hero-side {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.governance-workbench__hero-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.governance-workbench__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.8);
}

.governance-workbench__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.governance-workbench__title-row h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.12;
  color: #ffffff;
}

.governance-workbench__title-badge {
  display: inline-flex;
  align-items: center;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.governance-workbench__desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.88);
}

.governance-workbench__chip-row,
.governance-workbench__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.governance-workbench__chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid var(--workbench-chip-border);
  background: var(--workbench-chip-bg);
  backdrop-filter: blur(8px);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.92);
}

.governance-workbench__hero-side {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
}

.governance-workbench__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.governance-workbench__stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 16px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.14);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
}

.governance-workbench__stat span,
.governance-workbench__stat small {
  font-size: 12px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.76);
}

.governance-workbench__stat strong {
  font-size: 30px;
  line-height: 1;
  color: #ffffff;
}

.governance-workbench__actions {
  justify-content: flex-end;
}

.governance-workbench__actions :deep(.arco-btn) {
  height: 42px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  font-weight: 600;
}

.governance-workbench__actions :deep(.arco-btn-primary) {
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
}

.governance-workbench__actions :deep(.arco-btn:not(.arco-btn-primary)) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.12);
}

.governance-workbench__layout {
  display: block;
}

.governance-workbench__layout.has-aside {
  display: grid;
  grid-template-columns: minmax(280px, 0.84fr) minmax(0, 1.56fr);
  gap: 20px;
  align-items: start;
}

.governance-workbench__aside,
.governance-workbench__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.governance-workbench__footer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tone-blue {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(125, 211, 252, 0.18));
}

.tone-green {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(52, 211, 153, 0.2));
}

.tone-teal {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(45, 212, 191, 0.2));
}

.tone-gold {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(251, 191, 36, 0.2));
}

.tone-violet {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(196, 181, 253, 0.22));
}

@media (max-width: 1280px) {
  .governance-workbench__hero,
  .governance-workbench__layout.has-aside {
    grid-template-columns: 1fr;
  }

  .governance-workbench__hero-side,
  .governance-workbench__actions {
    justify-content: flex-start;
  }

  .governance-workbench__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .governance-workbench {
    gap: 16px;
  }

  .governance-workbench__hero {
    padding: 20px;
    border-radius: 24px;
  }

  .governance-workbench__title-row h2 {
    font-size: 28px;
  }

  .governance-workbench__stats {
    grid-template-columns: 1fr;
  }
}
</style>
