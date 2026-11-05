<template>
  <div class="pqc-completion-summary" role="status" :aria-label="description" :title="description">
    <span class="pqc-completion-summary__label">已完成</span>
    <span v-if="completed.first" class="pqc-completion-summary__badge">
      <svg viewBox="0 0 16 16" aria-hidden="true"><path d="m3 8 3 3 7-7" /></svg>
      首检
    </span>
    <span v-if="completed.patrolRounds.length" class="pqc-completion-summary__patrol">
      <span class="pqc-completion-summary__patrol-label">巡检</span>
      <span
        v-for="round in completed.patrolRounds"
        :key="round"
        class="pqc-completion-summary__round"
        :aria-label="`巡检第 ${round} 轮已完成`"
        >{{ round }}</span
      >
    </span>
    <span v-if="completed.final" class="pqc-completion-summary__badge">
      <svg viewBox="0 0 16 16" aria-hidden="true"><path d="m3 8 3 3 7-7" /></svg>
      末检
    </span>
    <span v-if="!hasCompleted" class="pqc-completion-summary__empty">暂无</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FrontlinePqcTaskOptionVO } from '@/api/mes/pro/feedback'
import { getCompletedPqcInspections } from './frontlinePqcCompletion'

const props = defineProps<{
  tasks: Pick<FrontlinePqcTaskOptionVO, 'inspectionType' | 'roundNo' | 'taskStatus'>[]
}>()
const completed = computed(() => getCompletedPqcInspections(props.tasks))
const hasCompleted = computed(
  () => completed.value.first || completed.value.final || completed.value.patrolRounds.length > 0
)
const description = computed(() => {
  const labels = [
    ...(completed.value.first ? ['首检'] : []),
    ...completed.value.patrolRounds.map((round) => `巡检第 ${round} 轮`),
    ...(completed.value.final ? ['末检'] : [])
  ]
  return labels.length ? `当前工序已完成：${labels.join('、')}` : '当前工序暂无已完成检验'
})
</script>

<style scoped lang="scss">
.pqc-completion-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  font-size: 13px;
  line-height: 24px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}
.pqc-completion-summary__label,
.pqc-completion-summary__empty {
  color: var(--frontline-muted);
  white-space: nowrap;
}
.pqc-completion-summary__label {
  margin-right: 2px;
}
.pqc-completion-summary__badge,
.pqc-completion-summary__round {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-height: 24px;
  padding: 0 7px;
  border: 1px solid #b4d9c8;
  border-radius: 7px;
  background: #e1f2e9;
  color: #156447;
  font-weight: 700;
  white-space: nowrap;
}
.pqc-completion-summary__badge {
  gap: 3px;
  svg {
    width: 13px;
    height: 13px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
}
.pqc-completion-summary__patrol {
  display: contents;
}
.pqc-completion-summary__patrol-label {
  margin-left: 2px;
  color: var(--frontline-muted);
}
.pqc-completion-summary__round {
  min-width: 25px;
  border-radius: 50%;
}
</style>
