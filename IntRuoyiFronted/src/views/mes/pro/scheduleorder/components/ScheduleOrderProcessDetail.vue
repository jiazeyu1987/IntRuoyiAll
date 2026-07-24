<template>
  <Dialog
    :model-value="modelValue"
    title="工艺流程"
    width="min(1360px, calc(100vw - 24px))"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <slot></slot>
  </Dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'ScheduleOrderProcessDetail' })

defineProps({
  modelValue: { type: Boolean, required: true }
})

const emit = defineEmits(['update:modelValue'])
</script>
