<template>
  <span v-if="supervisor" :class="statusClass" class="dept-supervisor-label">
    <template v-if="supervisor.status === 'DIRECT'">{{ supervisor.userName }}</template>
    <template v-else-if="supervisor.status === 'INVALID_HIERARCHY'">组织关系异常，请修复</template>
    <template v-else>
      <span>{{ supervisor.configuredUserInvalid ? '原主管已失效' : supervisor.configuredUserId ? '本部门已停用' : '未配置主管' }}</span>
      <span v-if="supervisor.status === 'INHERITED'"> · 由{{ supervisor.userName }}代管</span>
      <span v-else> · 无可用主管</span>
    </template>
  </span>
</template>
<script setup lang="ts">
import type { DeptSupervisorVO } from '@/api/system/dept'
const props = defineProps<{ supervisor?: DeptSupervisorVO }>()
const statusClass = computed(() => ({
  'dept-supervisor-label--warning': props.supervisor?.status === 'INHERITED',
  'dept-supervisor-label--danger': ['MISSING', 'INVALID_HIERARCHY'].includes(props.supervisor?.status ?? '')
}))
</script>
<style scoped>
.dept-supervisor-label { font-size: 12px; white-space: normal; }
.dept-supervisor-label--warning { color: var(--el-color-warning-dark-2); }
.dept-supervisor-label--danger { color: var(--el-color-danger); }
</style>
