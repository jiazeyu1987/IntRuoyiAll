<template>
  <div class="pqc-inspection-selector" role="group" aria-label="选择本次检验类型">
    <div v-if="tabs.length" class="pqc-inspection-selector__types">
      <button
        v-for="tab in tabs"
        :key="tab.ruleKey"
        type="button"
        :data-pqc-inspection-type-tab="tab.type"
        :data-pqc-inspection-rule-tab="tab.ruleKey"
        :class="{ active: selectedRuleKey === tab.ruleKey }"
        :aria-pressed="selectedRuleKey === tab.ruleKey"
        :disabled="disabled"
        @click="selectType(tab.ruleKey)"
      >
        <strong>{{ tab.label }}</strong>
        <span v-if="tab.type === 'PATROL'">
          {{
            selectedRuleKey === 'PATROL' && selectedPatrolRound
              ? `第 ${selectedPatrolRound} 轮`
              : '选择轮次 / 新建'
          }}
        </span>
      </button>
    </div>
    <p v-else class="pqc-inspection-selector__empty">当前没有可选择的检验任务</p>
    <dialog
      ref="patrolDialog"
      class="pqc-inspection-selector__dialog"
      aria-labelledby="pqc-patrol-selection-title"
    >
      <header>
        <div><h2 id="pqc-patrol-selection-title">巡检轮次</h2></div>
        <button type="button" aria-label="关闭巡检轮次选择" @click="patrolDialog?.close()"
          >×</button
        >
      </header>
      <p v-if="!patrolRounds.length" class="pqc-inspection-selector__current"
        >尚未创建巡检，点击下方按钮开始第 1 轮。</p
      >
      <div class="pqc-inspection-selector__rules">
        <article
          v-for="round in patrolRounds" :key="round.roundNo" class="pqc-inspection-selector__round"
          :aria-label="`第 ${round.roundNo} 轮${round.cancelled ? '，已撤销' : ''}`"
          :class="{ 'is-active': !round.cancelled && selectedRuleKey === 'PATROL' && selectedPatrolRound === round.roundNo, 'is-cancelled': round.cancelled }">
          <button
type="button" class="pqc-inspection-selector__round-select" :data-pqc-patrol-round="round.roundNo"
            :class="{ active: selectedRuleKey === 'PATROL' && selectedPatrolRound === round.roundNo }"
            :disabled="disabled || round.pendingCount === 0 || round.cancelled"
            @click="selectRound(round.roundNo)">
            <span class="pqc-inspection-selector__round-heading">
              <strong>第 {{ round.roundNo }} 轮</strong>
              <span v-if="!round.cancelled" class="pqc-inspection-selector__progress">已检工序 {{ round.totalCount - round.pendingCount }}/{{ round.totalCount }}</span>
            </span>
            <time class="pqc-inspection-selector__created-time">{{ formatDateTimeValue(round.createTime, '新建时间缺失') }}</time>
          </button>
          <div class="pqc-inspection-selector__round-actions">
            <button
v-if="round.cancelled" type="button" :disabled="disabled"
              :aria-label="`恢复第 ${round.roundNo} 轮`" @click="roundAction('restore-round', round.roundNo)">恢复</button>
            <button
v-else-if="round.pendingCount === round.totalCount" type="button" :disabled="disabled"
              :aria-label="`撤销第 ${round.roundNo} 轮`" @click="roundAction('cancel-round', round.roundNo)">撤销</button>
            <button
v-else-if="round.pendingCount < round.totalCount" type="button" :disabled="disabled"
              :aria-label="`更正第 ${round.roundNo} 轮`" @click="roundAction('correct-round', round.roundNo)">更正轮次</button>
          </div>
        </article>
      </div>
      <footer>
        <button
v-if="currentTask?.inspectionType === 'PATROL'" type="button" :disabled="disabled"
          @click="patrolDialog?.close(); emit('move-draft')">移动</button>
        <button type="button" data-pqc-create-patrol :disabled="disabled" @click="createPatrol"
          >新建</button
        >
      </footer>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { formatDateTimeValue } from '@/utils/formatTime'
import type {
  FrontlinePqcInspectionRuleKey,
  FrontlinePqcInspectionType,
  FrontlinePqcTaskOptionVO
} from '@/api/mes/pro/feedback'

interface InspectionRuleTab {
  ruleKey: FrontlinePqcInspectionRuleKey
  type: FrontlinePqcInspectionType
  label: string
}
const props = defineProps<{
  tabs: InspectionRuleTab[]
  selectedRuleKey?: FrontlinePqcInspectionRuleKey
  currentTask?: Pick<FrontlinePqcTaskOptionVO, 'inspectionType' | 'roundNo' | 'businessDate'>
  patrolRounds: { roundNo: number; pendingCount: number; totalCount: number; createTime?: number; cancelled?: boolean; cancelReason?: string }[]
  finalCreated?: boolean
  selectedPatrolRound?: number
  disabled: boolean
}>()
const emit = defineEmits<{
  (event: 'select-rule', ruleKey: FrontlinePqcInspectionRuleKey): void
  (event: 'select-round', roundNo: number): void
  (event: 'create-patrol'): void
  (event: 'create-final'): void
  (event: 'cancel-round', roundNo: number): void
  (event: 'restore-round', roundNo: number): void
  (event: 'correct-round', roundNo: number): void
  (event: 'move-draft'): void
}>()
const patrolDialog = ref<HTMLDialogElement>()
function selectType(ruleKey: FrontlinePqcInspectionRuleKey) {
  if (props.disabled) return
  if (ruleKey === 'PATROL') patrolDialog.value?.showModal()
  else if (ruleKey === 'FINAL' && props.finalCreated === false) emit('create-final')
  else emit('select-rule', ruleKey)
}
function roundAction(action: 'cancel-round' | 'restore-round' | 'correct-round', roundNo: number) {
  if (props.disabled) return
  patrolDialog.value?.close()
  if (action === 'cancel-round') emit('cancel-round', roundNo)
  else if (action === 'restore-round') emit('restore-round', roundNo)
  else emit('correct-round', roundNo)
}
function selectRound(roundNo: number) {
  if (
    props.disabled ||
    !props.patrolRounds.some((round) => round.roundNo === roundNo && round.pendingCount > 0 && !round.cancelled)
  )
    return
  patrolDialog.value?.close()
  emit('select-round', roundNo)
}
function createPatrol() {
  if (props.disabled) return
  patrolDialog.value?.close()
  emit('create-patrol')
}
watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) patrolDialog.value?.close()
  }
)
watch(
  () => props.tabs.map((tab) => tab.ruleKey).join(','),
  () => patrolDialog.value?.close()
)
onBeforeUnmount(() => patrolDialog.value?.close())
</script>

<style scoped lang="scss">
.pqc-inspection-selector {
  min-width: 0;
  width: 100%;
}
.pqc-inspection-selector__types {
  display: grid;
  grid-auto-columns: minmax(0, 1fr);
  grid-auto-flow: column;
  gap: 10px;
}
.pqc-inspection-selector button {
  min-width: 0;
  min-height: 64px;
  padding: 8px 12px;
  border: 3px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  color: var(--frontline-ink);
  font: inherit;
  cursor: pointer;
  &:focus-visible {
    outline: 4px solid #86c8ad;
    outline-offset: 2px;
  }
  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
  &.active {
    border-color: var(--frontline-dark);
    background: var(--frontline-dark);
    color: #fff;
  }
  strong {
    display: block;
    font-size: 32px;
    font-weight: 900;
    line-height: 1.15;
  }
  span {
    display: block;
    margin-top: 4px;
    font-size: 14px;
    line-height: 1.3;
  }
}
.pqc-inspection-selector__empty {
  margin: 0;
  color: var(--frontline-muted);
  text-align: center;
  font-size: 20px;
}
.pqc-inspection-selector__dialog {
  margin: auto;
  width: min(1020px, calc(100% - 48px));
  max-height: calc(100dvh - 48px);
  padding: 24px;
  box-sizing: border-box;
  overflow: hidden;
  border: 3px solid var(--frontline-line);
  border-radius: 24px;
  background: var(--frontline-panel);
  color: var(--frontline-ink);
  font: inherit;
  &[open] {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }
  &::backdrop {
    background: #14251db3;
  }
  header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-shrink: 0;
  }
  h2 {
    margin: 0;
    font-size: 28px;
  }
  header button {
    min-height: 48px;
    min-width: 48px;
    padding: 0;
    font-size: 30px;
  }
  footer {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    flex-shrink: 0;
    gap: 12px;
  }
  footer button {
    min-height: 52px;
    min-width: 100px;
    font-size: 22px;
  }
  [data-pqc-create-patrol] {
    margin-left: auto;
  }
}
.pqc-inspection-selector__rules {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  min-height: 0;
  max-height: min(540px, calc(100dvh - 240px));
  overflow-y: auto;
  gap: 16px;
  padding-right: 4px;
  align-content: start;
}
.pqc-inspection-selector__current {
  margin: 18px 0 0;
  font-size: 16px;
  line-height: 1.6;
  color: var(--frontline-muted);
}
.pqc-inspection-selector__round {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 156px;
  padding: 14px 16px;
  box-sizing: border-box;
  border: 2px solid var(--frontline-line);
  border-radius: 18px;
  background: #f8faf8;
  &.is-active {
    border-color: var(--frontline-dark);
    background: var(--frontline-dark);
    color: #fff;
  }
  &.is-cancelled {
    border-color: #e3aaaa;
    background: #fbe5e5;
    color: #8e2929;
  }
  .pqc-inspection-selector__round-select {
    width: 100%;
    min-height: 0;
    padding: 0;
    border: 0;
    border-radius: 10px;
    background: transparent;
    color: inherit;
    text-align: left;
    &:disabled {
      opacity: 1;
    }
    &.active {
      background: transparent;
      color: inherit;
    }
  }
  .pqc-inspection-selector__round-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin: 0;
    strong {
      font-size: 30px;
      line-height: 36px;
      white-space: nowrap;
    }
  }
  .pqc-inspection-selector__progress {
    max-width: 88px;
    margin: 0;
    font-size: 13px;
    font-weight: 600;
    text-align: right;
  }
  .pqc-inspection-selector__created-time {
    display: block;
    margin-top: 10px;
    font-size: 16px;
    font-weight: 400;
    font-variant-numeric: tabular-nums;
    line-height: 24px;
    white-space: nowrap;
  }
}
.pqc-inspection-selector__round-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  button {
    min-height: 38px;
    min-width: 72px;
    padding: 4px 16px;
    border-width: 1px;
    border-radius: 10px;
    font-size: 16px;
    font-weight: 600;
  }
  .is-cancelled & button {
    border-color: #dba0a0;
    background: #fff6f6;
    color: #8e2929;
  }
}
@media (max-width: 760px) {
  .pqc-inspection-selector__rules {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 540px) {
  .pqc-inspection-selector__types {
    gap: 6px;
  }
  .pqc-inspection-selector__types button {
    padding-inline: 6px;
    strong {
      font-size: 26px;
    }
    span {
      font-size: 12px;
    }
  }
  .pqc-inspection-selector__dialog {
    width: calc(100% - 24px);
    max-height: calc(100dvh - 24px);
    padding: 18px;
  }
  .pqc-inspection-selector__rules {
    grid-template-columns: 1fr;
    gap: 10px;
  }
}
</style>
