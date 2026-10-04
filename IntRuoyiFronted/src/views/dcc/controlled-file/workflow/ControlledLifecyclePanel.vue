<template>
  <section>
    <p>受控日期：{{ facts.controlledTime || '尚未受控' }}</p>
    <p>预设生效日期：{{ facts.effectiveDate }}</p>
    <p>实际生效时间：{{ facts.activatedTime || '尚未生效' }}</p>
    <p>文控下发：{{ facts.distributedTime || '待下发' }}</p>
    <p v-if="view.warning" role="status">{{ view.warning }}</p>
    <p v-if="view.reworkHint" role="status">{{ view.reworkHint }}</p>
    <slot name="distribution" v-if="view.canDistribute" ></slot>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { lifecyclePresentation, type LifecycleFacts } from './workflow-actions'
const props = defineProps<{ facts: LifecycleFacts }>()
const view = computed(() => lifecyclePresentation(props.facts))
</script>
