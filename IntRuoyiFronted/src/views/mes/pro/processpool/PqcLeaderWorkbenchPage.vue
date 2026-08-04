<template>
  <div class="pqc-leader-workbench-page" data-pqc-leader-workbench-page>
    <TeamLeaderWorkbenchPage
      leader-type="PQC"
      :show-leader-type-tabs="false"
      title="PQC组长"
      subtitle="查看和复核 PQC 检验员提交、项目明细与过程检验汇集状态"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'

defineOptions({ name: 'MesProProcessPoolPqcLeaderWorkbench' })
</script>

<style scoped>
.pqc-leader-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
