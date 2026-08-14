<template>
  <div class="production-leader-workbench-page" data-production-leader-workbench-page>
    <TeamLeaderWorkbenchPage
      leader-type="PRODUCTION"
      :show-leader-type-tabs="false"
      :show-production-module-tabs="true"
      title="生产组长"
      subtitle="查看生产一线原始数据、员工切换、FIFO 分配和异常记录"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'

defineOptions({ name: 'MesProProcessPoolProductionLeaderWorkbench' })
</script>

<style scoped>
.production-leader-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
