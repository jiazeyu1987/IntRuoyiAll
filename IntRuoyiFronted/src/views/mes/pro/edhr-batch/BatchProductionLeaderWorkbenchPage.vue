<template>
  <div class="edhr-batch-production-leader-page" data-edhr-batch-production-leader-page>
    <EdhrBatchRecordTabs active-tab="productionLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PRODUCTION"
      :show-leader-type-tabs="false"
      title="生产组长"
      subtitle="专门查看生产一线原始数据、员工切换、FIFO 分配和异常记录"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
import EdhrBatchRecordTabs from './EdhrBatchRecordTabs.vue'

defineOptions({ name: 'MesProEdhrBatchProductionLeaderWorkbench' })
</script>

<style scoped>
.edhr-batch-production-leader-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
