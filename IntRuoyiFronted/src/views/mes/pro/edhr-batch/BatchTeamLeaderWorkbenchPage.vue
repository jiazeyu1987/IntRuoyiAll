<template>
  <div class="edhr-batch-team-leader-page" data-edhr-batch-team-leader-page>
    <EdhrBatchRecordTabs active-tab="teamLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PQC"
      :show-leader-type-tabs="false"
      title="组长工作台"
      subtitle="查看和复核 PQC 检验员提交、项目明细与过程检验汇集状态"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchTeamLeaderWorkbench' })
</script>

<style scoped>
.edhr-batch-team-leader-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
