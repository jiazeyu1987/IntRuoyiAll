<template>
  <div class="pqc-leader-workbench-page" data-pqc-leader-workbench-page>
    <ContentWrap v-if="isPackagingLeader">
      <h1 class="m-0 text-18px font-semibold">PQC包装组长</h1>
      <p class="mb-0 mt-8px text-14px text-[var(--el-text-color-secondary)]">
        查看本租户全部在制活跃订单，管理自己的检验人员并复核其提交
      </p>
    </ContentWrap>
    <PqcPackagingActiveOrders v-if="isPackagingLeader" />
    <TeamLeaderWorkbenchPage
      leader-type="PQC"
      :show-leader-type-tabs="false"
      :show-pqc-module-tabs="true"
      :title="isPackagingLeader ? 'PQC包装组长' : 'PQC组长'"
      subtitle="查看和复核 PQC 检验员提交、项目明细与过程检验汇集状态"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
import PqcPackagingActiveOrders from './PqcPackagingActiveOrders.vue'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const isPackagingLeader = computed(() => userStore.getRoles.includes('pqc_packaging_leader_permission'))

defineOptions({ name: 'MesProProcessPoolPqcLeaderWorkbench' })
</script>

<style scoped>
.pqc-leader-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
