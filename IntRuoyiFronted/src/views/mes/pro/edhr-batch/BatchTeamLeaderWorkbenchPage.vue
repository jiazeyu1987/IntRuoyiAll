<template>
  <div class="edhr-batch-team-leader-page" data-edhr-batch-team-leader-page>
    <EdhrBatchRecordTabs active-tab="teamLeader" />
    <TeamLeaderWorkbenchPage />
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
