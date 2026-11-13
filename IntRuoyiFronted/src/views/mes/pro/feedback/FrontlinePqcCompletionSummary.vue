<template>
  <div class="pqc-completion-summary" role="status" :aria-label="description" :title="description">
    <span class="pqc-completion-summary__label">检验进度</span>
    <span
      v-for="stage in stages"
      :key="stage.key"
      class="pqc-completion-summary__badge"
      :class="{ 'is-complete': stage.completed }"
      :aria-label="`${stage.label}已提交 ${stage.submittedCount}/${stage.totalCount}`"
    >
      <svg v-if="stage.completed" viewBox="0 0 16 16" aria-hidden="true"><path d="m3 8 3 3 7-7" /></svg>
      {{ stage.label }} ({{ stage.submittedCount }}/{{ stage.totalCount }})
    </span>
    <span v-if="!stages.length" class="pqc-completion-summary__empty">暂无</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FrontlinePqcProcessVO } from '@/api/mes/pro/feedback'
import { getPqcInspectionProgressSummary } from './frontlinePqcCompletion'

const props = defineProps<{ processes: FrontlinePqcProcessVO[] }>()
const stages = computed(() => getPqcInspectionProgressSummary(props.processes))
const description = computed(() => stages.value.length
  ? `当前订单检验进度：${stages.value.map((stage) => `${stage.label}已提交 ${stage.submittedCount}/${stage.totalCount}`).join('、')}`
  : '当前订单暂无检验任务')
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
.pqc-completion-summary__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  gap: 3px;
  min-height: 24px;
  padding: 0 7px;
  border: 1px solid #e2b341;
  border-radius: 7px;
  background: #fff3bf;
  color: #805b10;
  font-weight: 700;
  white-space: nowrap;
  &.is-complete {
    border-color: #b4d9c8;
    background: #e1f2e9;
    color: #156447;
  }
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
</style>
