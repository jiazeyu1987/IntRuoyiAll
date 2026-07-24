<template>
  <div class="showroom-dashboard-workbench" v-loading="loading">
    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      type="error"
      :title="loadError"
    />

    <template v-else>
      <div class="showroom-dashboard-workbench__toolbar">
        <div>
          <h3 class="showroom-dashboard-workbench__title">Dashboard</h3>
          <p class="showroom-dashboard-workbench__subtitle">
            展柜后台汇总视图，所有统计均来自真实接口；精确音频陈旧数待后端契约补齐。
          </p>
        </div>
        <el-button :loading="loading" @click="loadDashboard">刷新统计</el-button>
      </div>

      <ShowroomDashboardCards :cards="cards" />

      <el-alert
        :closable="false"
        show-icon
        type="warning"
        title="讲解音频陈旧统计待后端契约补齐"
        description="统计暂不可用"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ShowroomAdminApi } from '@/api/showroom-admin'
import ShowroomDashboardCards from './ShowroomDashboardCards.vue'
import { createDashboardCards, fetchPagedTotal, type ShowroomDashboardCard } from './contracts'

defineOptions({ name: 'ShowroomDashboardWorkbench' })

const loading = ref(false)
const loadError = ref('')
const cards = ref<ShowroomDashboardCard[]>(createDashboardCards({
  liveHallCount: 0,
  productCount: 0,
  incompleteProductCount: 0,
  pendingApprovalCount: 0,
  pendingAssignmentCount: 0
}))

const loadDashboard = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const [
      liveHallCount,
      productCount,
      incompleteProductCount,
      approvalRows,
      pendingAssignmentCount
    ] = await Promise.all([
      fetchPagedTotal((pageNo, pageSize) =>
        ShowroomAdminApi.getHallPage({ keyword: '', pageNo, pageSize })
      ),
      fetchPagedTotal((pageNo, pageSize) =>
        ShowroomAdminApi.getProductPage({ keyword: '', pageNo, pageSize })
      ),
      fetchPagedTotal((pageNo, pageSize) =>
        ShowroomAdminApi.getProductPage({
          keyword: '',
          pageNo,
          pageSize,
          incompleteStatus: 'INCOMPLETE'
        })
      ),
      ShowroomAdminApi.getApprovalPage({ pageNo: 1, pageSize: 20 }),
      fetchPagedTotal((pageNo, pageSize) =>
        request.get({ url: '/showroom/assignment/page', params: { status: 'OPEN', pageNo, pageSize } })
      )
    ])

    if (!Array.isArray(approvalRows)) {
      throw new Error('Dashboard 缺少真实审批数组：approvalRows')
    }

    cards.value = createDashboardCards({
      liveHallCount,
      productCount,
      incompleteProductCount,
      pendingApprovalCount: approvalRows.length,
      pendingAssignmentCount
    })
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped>
.showroom-dashboard-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-dashboard-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-dashboard-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-dashboard-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

@media (max-width: 760px) {
  .showroom-dashboard-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
