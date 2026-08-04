<template>
  <div class="edhr-batch-pqc-leader-page" data-edhr-batch-pqc-leader-page>
    <EdhrBatchRecordTabs active-tab="pqcLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PQC"
      :show-leader-type-tabs="false"
      title="PQC组长"
      subtitle="专门查看和复核 PQC 检验员提交、项目明细与过程检验汇集状态"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchPqcLeaderWorkbench' })
</script>

<style scoped>
.edhr-batch-pqc-leader-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
