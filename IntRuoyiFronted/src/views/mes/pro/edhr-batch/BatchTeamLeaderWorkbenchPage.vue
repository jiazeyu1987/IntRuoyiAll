<template>
  <div class="edhr-batch-team-leader-page" data-edhr-batch-team-leader-page>
    <EdhrBatchRecordTabs active-tab="teamLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PRODUCTION"
      :show-leader-type-tabs="false"
      title="工序池班组长工作台"
      subtitle="负责生产报工确认、活跃订单分配、异常上报和班组配置中心维护"
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
