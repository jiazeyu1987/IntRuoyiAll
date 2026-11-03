<template>
  <div class="edhr-renderer-shell">
    <h3>执行快照</h3>
    <p>按字段查看执行内容，原始快照可在技术详情中核查。</p>
    <el-alert
      v-if="!execution.executionSnapshotJson?.trim()"
      title="执行快照缺失，无法查看执行内容。"
      type="error"
      :closable="false"
      show-icon
    />
    <ReadableData v-else title="执行内容" :source="execution.executionSnapshotJson" />
  </div>
</template>

<script setup lang="ts">
import type { ProFeedbackEdhrExecutionVO } from '@/api/mes/pro/feedback'
import ReadableData from '@/components/BusinessData/ReadableData.vue'
defineOptions({ name: 'EdhrExecutionRenderer' })
defineProps<{ execution: ProFeedbackEdhrExecutionVO }>()
</script>

<style scoped>
.edhr-renderer-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}
.edhr-renderer-shell h3,
.edhr-renderer-shell p {
  margin: 0;
}
.edhr-renderer-shell p {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
