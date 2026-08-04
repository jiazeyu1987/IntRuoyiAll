<template>
  <div class="edhr-batch-team-leader-page" data-edhr-batch-team-leader-page>
    <EdhrBatchRecordTabs active-tab="teamLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PRODUCTION"
      :show-leader-type-tabs="false"
      title="组长工作台"
      subtitle="查看生产一线原始数据、员工切换、FIFO 分配和异常记录"
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
