<template>
  <el-tag :type="tagType" effect="light" class="capacity-source-tag">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  value?: 'RESOURCE_CALCULATED' | 'MANUAL_OVERRIDE' | 'FINITE_HOURLY' | 'INFINITE_FORMULA' | 'MACHINE' | 'WORKER' | 'UNCONFIGURED' | string | null
}>()

const label = computed(() => {
  switch (props.value) {
    case 'RESOURCE_CALCULATED':
      return '资源计算'
    case 'MANUAL_OVERRIDE':
      return '产能覆盖'
    case 'FINITE_HOURLY':
      return '小时产能'
    case 'INFINITE_FORMULA':
      return '无限公式'
    case 'MACHINE':
      return '设备资源'
    case 'WORKER':
      return '人工资源'
    case 'UNCONFIGURED':
      return '资源未配置'
    default:
      return props.value || '未配置'
  }
})

const tagType = computed(() => {
  if (props.value === 'FINITE_HOURLY' || props.value === 'UNCONFIGURED') return 'danger'
  if (props.value === 'MANUAL_OVERRIDE') return 'warning'
  if (props.value === 'RESOURCE_CALCULATED' || props.value === 'MACHINE' || props.value === 'WORKER') return 'success'
  return 'info'
})
</script>

<style scoped>
.capacity-source-tag {
  border-radius: 6px;
  font-weight: 600;
}
</style>
