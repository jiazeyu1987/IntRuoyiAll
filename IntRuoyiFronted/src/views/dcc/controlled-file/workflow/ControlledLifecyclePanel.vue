<template>
  <section>
    <p>受控日期：{{ facts.controlledTime || '尚未受控' }}</p>
    <p>生效方式：{{ effectiveness.modeText }}</p>
    <p v-if="effectiveness.presetDateVisible">预设生效日期：{{ facts.effectiveDate || '未记录' }}</p>
    <p>实际生效时间：{{ effectiveness.effectiveText }}</p>
    <p>日常启用时间：{{ effectiveness.activatedText }}</p>
    <p>文控下发：{{ facts.distributedTime || '待下发' }}</p>
    <p v-if="view.warning" role="status">{{ view.warning }}</p>
    <p v-if="view.reworkHint" role="status">{{ view.reworkHint }}</p>
    <slot name="distribution" v-if="view.canDistribute && facts.status === 'ACTIVE' && facts.controlledTime && facts.activatedTime" ></slot>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { LifecycleFacts } from './workflow-actions'
import { effectiveFacts, type EffectiveFacts } from './effective-facts'
const props = defineProps<{ facts: Omit<LifecycleFacts, 'effectiveDate'> & EffectiveFacts }>()
const view = computed(() => ({
  canDistribute: props.facts.status === 'ACTIVE' && Boolean(props.facts.controlledTime)
    && Boolean(props.facts.activatedTime) && !props.facts.distributedTime,
  warning: props.facts.status === 'CONTROLLED_PENDING_EFFECTIVE'
    ? props.facts.effectiveMode === 'PRESET_DATE'
      ? `${props.facts.effectiveDate || '预设日期未记录'} 生效，日常启用前不得执行`
      : props.facts.effectiveMode === 'AFTER_STANDARDIZATION_SIGNATURE'
        ? '已受控，日常启用前不得执行' : '已受控，生效方式未记录，日常启用前不得执行'
    : '',
  reworkHint: props.facts.status === 'REJECTED'
    ? '查看签名驳回原因，修改后重新提交；新一轮须重新指派、会签和批准' : ''
}))
const effectiveness = computed(() => effectiveFacts(props.facts))
</script>
