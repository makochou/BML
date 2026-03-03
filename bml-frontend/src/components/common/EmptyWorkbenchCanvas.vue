<template>
  <section class="empty-workbench-canvas" :class="{ 'is-pure': pure }">
    <div class="empty-workbench-canvas__aurora"></div>
    <div class="empty-workbench-canvas__grain"></div>
    <div class="empty-workbench-canvas__float empty-workbench-canvas__float--a"></div>
    <div class="empty-workbench-canvas__float empty-workbench-canvas__float--b"></div>
    <div class="empty-workbench-canvas__float empty-workbench-canvas__float--c"></div>

    <header v-if="!pure" class="empty-workbench-canvas__head">
      <span v-if="resolvedEyebrow" class="empty-workbench-canvas__eyebrow">{{ resolvedEyebrow }}</span>
      <div class="empty-workbench-canvas__tags" v-if="resolvedTags.length">
        <span
          v-for="tag in resolvedTags"
          :key="tag"
          class="empty-workbench-canvas__tag"
        >
          {{ tag }}
        </span>
      </div>
    </header>

    <div v-if="!pure" class="empty-workbench-canvas__hero">
      <div class="empty-workbench-canvas__orb empty-workbench-canvas__orb--left"></div>
      <div class="empty-workbench-canvas__orb empty-workbench-canvas__orb--right"></div>
      <div class="empty-workbench-canvas__hero-card">
        <h3>{{ title }}</h3>
        <p>{{ description }}</p>
      </div>
    </div>

    <footer v-if="!pure && resolvedCards.length" class="empty-workbench-canvas__footer">
      <article
        v-for="item in resolvedCards"
        :key="item.label"
        class="empty-workbench-canvas__metric"
      >
        <small>{{ item.label }}</small>
        <strong>{{ item.value }}</strong>
        <p>{{ item.hint }}</p>
      </article>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

export type EmptyWorkbenchMetric = {
  label: string;
  value: string;
  hint: string;
};

const props = withDefaults(defineProps<{
  eyebrow?: string;
  title?: string;
  description?: string;
  tags?: string[];
  metrics?: EmptyWorkbenchMetric[];
  pure?: boolean;
}>(), {
  eyebrow: '',
  title: '',
  description: '',
  tags: () => [],
  metrics: () => [],
  pure: false
});

/**
 * 空白画布组件：
 * 用于业务页面在“暂不展示具体表单内容”时仍保持视觉完整性，
 * - `pure=true`：只渲染视觉背景，不展示任何文字内容。
 * - `pure=false`：可通过标题、标签和指标卡承载说明信息。
 */
const resolvedEyebrow = computed(() => props.eyebrow.trim());
const resolvedTags = computed(() => props.tags.map(item => item.trim()).filter(Boolean));
const resolvedCards = computed(() => props.metrics.filter(item => item.label && item.value));
const pure = computed(() => props.pure);
</script>

<style scoped>
.empty-workbench-canvas {
  --canvas-text-main: #0f172a;
  --canvas-text-sub: #475569;
  position: relative;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 22px;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: clamp(22px, 2.4vw, 34px);
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 22px;
  background:
    radial-gradient(circle at 18% -12%, rgba(20, 184, 166, 0.24), transparent 40%),
    radial-gradient(circle at 88% 0%, rgba(59, 130, 246, 0.2), transparent 36%),
    linear-gradient(155deg, #f8fbff 0%, #f3f8ff 42%, #f6f8fc 100%);
  box-shadow:
    0 24px 52px rgba(15, 23, 42, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
  isolation: isolate;
}

.empty-workbench-canvas.is-pure {
  display: block;
  padding: 0;
  border-radius: 24px;
  background:
    radial-gradient(circle at 10% -18%, rgba(45, 212, 191, 0.26), transparent 38%),
    radial-gradient(circle at 92% -10%, rgba(37, 99, 235, 0.24), transparent 34%),
    radial-gradient(circle at 76% 84%, rgba(249, 115, 22, 0.18), transparent 30%),
    linear-gradient(140deg, #f2fbff 0%, #eaf5ff 42%, #eef2ff 100%);
}

.empty-workbench-canvas__aurora {
  position: absolute;
  inset: -24% -18%;
  background:
    conic-gradient(from 200deg at 52% 54%, rgba(37, 99, 235, 0.2), rgba(16, 185, 129, 0.18), rgba(249, 115, 22, 0.18), rgba(37, 99, 235, 0.2));
  filter: blur(74px);
  opacity: 0.45;
  animation: canvasAuroraShift 14s ease-in-out infinite alternate;
  pointer-events: none;
  z-index: -2;
}

.empty-workbench-canvas__grain {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(148, 163, 184, 0.24) 0.35px, transparent 0.35px);
  background-size: 7px 7px;
  opacity: 0.22;
  pointer-events: none;
  z-index: -1;
}

.empty-workbench-canvas__float {
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(0.2px);
  z-index: 0;
}

.empty-workbench-canvas__float--a {
  width: clamp(220px, 26vw, 360px);
  aspect-ratio: 1;
  left: -6%;
  top: 14%;
  background:
    radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.86), rgba(186, 230, 253, 0.3) 54%, rgba(37, 99, 235, 0.08) 100%);
  animation: canvasFloatA 14s ease-in-out infinite;
}

.empty-workbench-canvas__float--b {
  width: clamp(180px, 20vw, 300px);
  aspect-ratio: 1;
  right: 6%;
  top: 12%;
  background:
    radial-gradient(circle at 35% 35%, rgba(255, 255, 255, 0.8), rgba(196, 181, 253, 0.28) 55%, rgba(99, 102, 241, 0.08) 100%);
  animation: canvasFloatB 16s ease-in-out infinite;
}

.empty-workbench-canvas__float--c {
  width: clamp(300px, 34vw, 460px);
  height: clamp(140px, 20vw, 230px);
  left: 50%;
  bottom: -8%;
  transform: translateX(-50%);
  background:
    radial-gradient(ellipse at 50% 20%, rgba(255, 255, 255, 0.7), rgba(186, 230, 253, 0.18) 58%, rgba(14, 165, 233, 0.06) 100%);
  animation: canvasFloatC 18s ease-in-out infinite;
}

.empty-workbench-canvas__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.empty-workbench-canvas__eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  color: #0f172a;
  font-family: 'Segoe UI Semibold', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.empty-workbench-canvas__tags {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-workbench-canvas__tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(16, 185, 129, 0.14));
  color: #0f5de2;
  font-family: 'Segoe UI Semibold', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 12px;
  line-height: 1;
}

.empty-workbench-canvas__hero {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 0;
}

.empty-workbench-canvas__orb {
  position: absolute;
  width: clamp(128px, 18vw, 196px);
  aspect-ratio: 1;
  border-radius: 999px;
  background: radial-gradient(circle at 32% 28%, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.08) 58%, transparent 100%);
  border: 1px solid rgba(255, 255, 255, 0.48);
  box-shadow: 0 12px 28px rgba(30, 64, 175, 0.18);
  pointer-events: none;
}

.empty-workbench-canvas__orb--left {
  left: 8%;
  top: 14%;
  animation: canvasOrbFloatLeft 10s ease-in-out infinite;
}

.empty-workbench-canvas__orb--right {
  right: 8%;
  bottom: 8%;
  animation: canvasOrbFloatRight 12s ease-in-out infinite;
}

.empty-workbench-canvas__hero-card {
  position: relative;
  z-index: 1;
  width: min(760px, 100%);
  padding: clamp(24px, 2.8vw, 34px) clamp(22px, 3.6vw, 42px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.78), rgba(246, 250, 255, 0.9));
  box-shadow:
    0 18px 42px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  text-align: center;
}

.empty-workbench-canvas__hero-card h3 {
  margin: 0;
  color: var(--canvas-text-main);
  font-family: 'Trebuchet MS', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: clamp(30px, 4.2vw, 42px);
  font-weight: 800;
  letter-spacing: 0.01em;
  line-height: 1.2;
}

.empty-workbench-canvas__hero-card p {
  margin: 14px auto 0;
  max-width: 640px;
  color: var(--canvas-text-sub);
  font-size: clamp(15px, 1.75vw, 18px);
  line-height: 1.8;
}

.empty-workbench-canvas__footer {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.empty-workbench-canvas__metric {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.78);
}

.empty-workbench-canvas__metric small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.2;
}

.empty-workbench-canvas__metric strong {
  color: #0f172a;
  font-family: 'Trebuchet MS', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 20px;
  line-height: 1.2;
}

.empty-workbench-canvas__metric p {
  margin: 0;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

@keyframes canvasAuroraShift {
  0% {
    transform: translate3d(-3%, -2%, 0) scale(1);
  }
  100% {
    transform: translate3d(3%, 2%, 0) scale(1.06);
  }
}

@keyframes canvasFloatA {
  0%, 100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(14px, -10px, 0);
  }
}

@keyframes canvasFloatB {
  0%, 100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-12px, 12px, 0);
  }
}

@keyframes canvasFloatC {
  0%, 100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(-12px);
  }
}

@keyframes canvasOrbFloatLeft {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-12px);
  }
}

@keyframes canvasOrbFloatRight {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(14px);
  }
}

@media (max-width: 1024px) {
  .empty-workbench-canvas__footer {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .empty-workbench-canvas {
    padding: 18px 14px;
    border-radius: 16px;
  }

  .empty-workbench-canvas.is-pure {
    border-radius: 16px;
  }

  .empty-workbench-canvas__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .empty-workbench-canvas__tags {
    justify-content: flex-start;
  }

  .empty-workbench-canvas__hero-card h3 {
    font-size: 28px;
  }

  .empty-workbench-canvas__orb {
    display: none;
  }

  .empty-workbench-canvas__float--a,
  .empty-workbench-canvas__float--b {
    width: clamp(140px, 36vw, 210px);
  }
}
</style>
