<template>
  <ContentWrap>
    <div class="mb-12px flex items-center justify-between">
      <h2 class="m-0 text-16px">全部在制活跃订单</h2>
      <el-button :loading="loading" @click="loadOrders">刷新订单</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" class="mb-12px" />
    <el-table v-loading="loading" :data="orders" border stripe row-key="activeOrderId">
      <el-table-column prop="workOrderCode" label="工单编号" min-width="240" />
      <el-table-column prop="workOrderName" label="工单名称" min-width="180" />
      <el-table-column prop="quantity" label="生产数量" width="100" />
      <el-table-column prop="productionLeaderUserId" label="原生产组长编号" width="140" />
      <el-table-column prop="routeId" label="工艺路线编号" width="140" />
      <el-table-column prop="businessStatus" label="业务状态" width="120">
        <template #default="{ row }">{{ row.businessStatus === 'ACTIVE' ? '在制' : row.businessStatus }}</template>
      </el-table-column>
    </el-table>
    <p class="mb-0 text-14px text-[var(--el-text-color-secondary)]">
      共 {{ orders.length }} 条；检验提交仍按订单冻结规程执行，复核仅处理自己人员的提交。
    </p>
  </ContentWrap>
</template>

<script setup lang="ts">
import request from '@/config/axios'

interface ActiveOrder {
  activeOrderId: number
  workOrderId: number
  workOrderCode: string
  workOrderName: string
  quantity: number
  productionLeaderUserId: number
  routeId: number
  businessStatus: string
}
const orders = ref<ActiveOrder[]>([])
const loading = ref(false)
const error = ref('')
const loadOrders = async () => {
  loading.value = true
  error.value = ''
  try {
    orders.value = await request.get<ActiveOrder[]>({
      url: '/mes/pro-process-pool/pqc-packaging-leader/active-orders'
    })
  } catch (cause) {
    orders.value = []
    error.value = cause instanceof Error ? cause.message : '活跃订单查询失败，请重试'
  } finally {
    loading.value = false
  }
}
onMounted(loadOrders)
</script>
