<template>
  <section class="edhr-deviation-trace-pane">
    <el-skeleton v-if="loading" :rows="4" animated />
    <el-alert v-else-if="error" :title="error" type="error" :closable="false" show-icon>
      <el-button link type="primary" @click="load">重试</el-button>
    </el-alert>
    <el-empty v-else-if="!rows.length" description="没有偏差" />
    <el-table v-else :data="rows" row-key="id" size="small">
      <el-table-column prop="deviationCode" label="偏差编号" min-width="170" />
      <el-table-column label="来源" min-width="180"><template #default="{ row }">{{ row.batchExecutionCode || (row.activeOrderId ? `${row.workOrderCode || '订单'}（生产中发起）` : '—') }}</template></el-table-column>
      <el-table-column label="等级" width="110"><template #default="{ row }">{{ row.level === 'CRITICAL' ? '重大（关键）' : '普通' }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }">{{ row.status === 'OPEN' ? '未处理' : '已处理' }}</template></el-table-column>
      <el-table-column label="不合格评审" min-width="220"><template #default="{ row }">{{ row.nonconformanceReviewCode || (row.nonconformanceReviewId ? `#${row.nonconformanceReviewId}` : '—') }}<span v-if="row.nonconformanceDisposition"> / {{ nonconformanceDispositionLabel(row.nonconformanceDisposition) }}</span></template></el-table-column>
      <el-table-column label="详情" width="160" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDeviation(row)">查看发起与处理</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="detailVisible" title="偏差详情" width="min(1240px, calc(100vw - 32px))" top="5vh" class="edhr-detail-surface" destroy-on-close append-to-body>
      <DeviationDetail v-if="detailVisible && selectedDeviationId" :id="selectedDeviationId" :actions-target="detailActionsTarget" :readonly="true" />
      <template #footer>
        <div class="flex flex-wrap items-center justify-between gap-12px">
          <div ref="detailActionsTarget"></div>
          <el-button size="default" @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>
<script setup lang="ts">
import { getDeviationPage, type DeviationRespVO } from '@/api/mes/pro/edhr/deviation'
import DeviationDetail from '@/views/mes/pro/edhr-deviation/DeviationDetail.vue'
import { nonconformanceDispositionLabel } from '@/views/mes/pro/processpool/components/activeOrderDisplayLabels'
const props = defineProps<{ batchExecutionId?: number | string; activeOrderId?: number | string }>()
const loading = ref(false)
const error = ref('')
const rows = ref<DeviationRespVO[]>([])
const detailVisible = ref(false)
const detailActionsTarget = ref<HTMLElement | null>(null)
const selectedDeviationId = ref<number>()
let requestSerial = 0
const openDeviation = (row: DeviationRespVO) => {
  selectedDeviationId.value = row.id
  detailVisible.value = true
}
const load = async () => {
  const serial = ++requestSerial
  loading.value = true; error.value = ''
  rows.value = []
  detailVisible.value = false
  selectedDeviationId.value = undefined
  const batchExecutionId = props.batchExecutionId
  const activeOrderId = props.activeOrderId
  const identity = activeOrderId ?? batchExecutionId
  if ((activeOrderId != null && batchExecutionId != null) || !(typeof identity === 'string' ? /^[1-9]\d*$/.test(identity) : Number.isSafeInteger(identity) && Number(identity) > 0)) {
    rows.value = []
    error.value = '偏差追溯缺少唯一有效订单或批记录编号，请重试'
    loading.value = false
    return
  }
  try {
    const first = await getDeviationPage({ pageNo: 1, pageSize: 200, batchExecutionId, activeOrderId })
    const allRows = [...(first.list || [])]
    const total = Number(first.total || allRows.length)
    const pageCount = Math.ceil(total / 200)
    for (let pageNo = 2; pageNo <= pageCount; pageNo += 1) {
      const next = await getDeviationPage({ pageNo, pageSize: 200, batchExecutionId, activeOrderId })
      allRows.push(...(next.list || []))
    }
    if (serial !== requestSerial) return
    rows.value = allRows.slice(0, total)
  }
  catch (errorValue: any) {
    if (serial !== requestSerial) return
    rows.value = []
    const status = errorValue?.response?.status
    error.value = status === 403 ? '当前账号没有偏差追溯权限' : status === 401 ? '登录状态已失效，请重新登录' : '偏差追溯加载失败，请重试'
  }
  finally { if (serial === requestSerial) loading.value = false }
}
watch(() => [props.batchExecutionId, props.activeOrderId], load, { immediate: true })
</script>
