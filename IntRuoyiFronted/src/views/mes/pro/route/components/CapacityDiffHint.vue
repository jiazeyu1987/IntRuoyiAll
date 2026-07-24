<template>
  <el-alert
    v-if="visible"
    :closable="false"
    type="warning"
    show-icon
    class="capacity-diff-hint"
  >
    <template #title>
      产能覆盖与资源计算差异 {{ diffText }}，请确认是否继续保留覆盖值。
    </template>
  </el-alert>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  manualHourlyCapacity?: number | null
  resourceHourlyCapacity?: number | null
}>()

const manualHourly = computed(() => {
  if (props.manualHourlyCapacity !== undefined && props.manualHourlyCapacity !== null) {
    return Number(props.manualHourlyCapacity)
  }
  return undefined
})

const diff = computed(() => {
  if (manualHourly.value === undefined || props.resourceHourlyCapacity === undefined || props.resourceHourlyCapacity === null) return undefined
  return manualHourly.value - Number(props.resourceHourlyCapacity)
})

const visible = computed(() => diff.value !== undefined && Math.abs(diff.value) > 0.000001)
const diffText = computed(() => (diff.value === undefined ? '' : `${diff.value > 0 ? '+' : ''}${diff.value.toFixed(2)} / 小时`))
</script>

<style scoped>
.capacity-diff-hint {
  margin-top: 8px;
  border-radius: 6px;
}
</style>
