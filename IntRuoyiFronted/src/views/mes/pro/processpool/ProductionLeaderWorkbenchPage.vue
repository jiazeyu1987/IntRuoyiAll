<template>
  <div class="production-leader-workbench-page" data-production-leader-workbench-page>
    <ContentWrap v-if="isPackagingLeader">
      <h1 class="m-0 text-18px font-semibold">包装组长</h1>
      <p class="mb-0 mt-8px text-14px text-[var(--el-text-color-secondary)]">
        查看本租户全部活跃工单，审核自己员工的报工，并办理整单完工
      </p>
    </ContentWrap>
    <TeamLeaderWorkbenchPage
      leader-type="PRODUCTION"
      :show-leader-type-tabs="false"
      :show-production-module-tabs="true"
      :title="isPackagingLeader ? '包装组长' : '生产组长'"
      :subtitle="isPackagingLeader
        ? '查看本租户全部活跃工单，审核自己员工的报工，并办理整单完工'
        : '查看生产一线原始数据、员工切换、FIFO 分配和异常记录'"
    />
  </div>
</template>

<script setup lang="ts">
import TeamLeaderWorkbenchPage from '@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
import { useUserStore } from '@/store/modules/user'

const userStore = useUserStore()
const isPackagingLeader = computed(() => userStore.getRoles.includes('packaging_leader_permission'))

defineOptions({ name: 'MesProProcessPoolProductionLeaderWorkbench' })
</script>

<style scoped>
.production-leader-workbench-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
