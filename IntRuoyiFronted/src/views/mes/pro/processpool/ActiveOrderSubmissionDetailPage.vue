<template>
  <ContentWrap>
    <div class="team-leader-workbench__active-order-detail-page" data-team-leader-active-order-detail-page>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :source-work-order="sourceWorkOrder"
        :production-material-lists="productionMaterialLists"
        :production-material-list-loading="productionMaterialListLoading"
        :production-material-list-error="productionMaterialListError"
        :loading="loading"
        :error="error"
        @retry="loadDetail"
        @open-batch-execution-production-form="handleOpenBatchExecutionSubmissionForm('production', $event)"
        @open-batch-execution-pqc-form="handleOpenBatchExecutionSubmissionForm('pqc')"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ActiveOrderSubmissionDetailPanel from './components/ActiveOrderSubmissionDetailPanel.vue'
import {
  getTeamLeaderActiveOrderDetail,
  simulateStage2_5BackfillBatchExecution,
  type Stage2_5BackfillBatchExecutionSimulationRespVO,
  type TeamLeaderActiveOrderProcessDetailRespVO,
  type TeamLeaderActiveOrderDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'
import {
  ErpProductionMaterialListApi,
  type ErpProductionMaterialListVO
} from '@/api/erp/production/material-list'

defineOptions({ name: 'MesProcessPoolActiveOrderSubmissionDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const sourceWorkOrder = ref<ProWorkOrderVO>()
const productionMaterialLists = ref<ErpProductionMaterialListVO[]>([])
const productionMaterialListLoading = ref(false)
const productionMaterialListError = ref('')
const loading = ref(false)
const error = ref('')
const batchExecutionOpening = ref(false)
const batchExecutionOpenResultCache = new Map<
  number,
  Stage2_5BackfillBatchExecutionSimulationRespVO
>()

const resolveErrorMessage = (errorValue: unknown, fallback: string) => {
  if (errorValue instanceof Error && errorValue.message) return errorValue.message
  if (typeof errorValue === 'string' && errorValue.trim()) return errorValue
  const responseMessage = (errorValue as { response?: { data?: { msg?: string; message?: string } } })
    ?.response?.data
  return responseMessage?.msg || responseMessage?.message || fallback
}

const requireActiveOrderId = () => {
  const activeOrderId = Number(route.params.activeOrderId)
  if (!Number.isFinite(activeOrderId) || activeOrderId <= 0) {
    throw new Error('活跃订单记录ID不能为空')
  }
  return activeOrderId
}

const resolveSourceWorkOrderCode = () => {
  const sourceWorkOrderCode = route.query.sourceWorkOrderCode
  return typeof sourceWorkOrderCode === 'string' ? sourceWorkOrderCode.trim() : ''
}

const loadSourceWorkOrder = async (sourceWorkOrderCode: string) => {
  const data = await ProWorkOrderApi.getWorkOrderPage({
    pageNo: 1,
    pageSize: 20,
    code: sourceWorkOrderCode
  })
  const rows = (data?.list ?? []).filter((row: ProWorkOrderVO) => row.code === sourceWorkOrderCode)
  if (rows.length !== 1) {
    throw new Error(`Stage1 来源生产工单 ${sourceWorkOrderCode} 未找到或不唯一，无法显示真实生产工单资料`)
  }
  return rows[0]
}

const resolveDisplayedProductionOrderNo = (
  detailResult: TeamLeaderActiveOrderDetailRespVO,
  sourceWorkOrderCode: string,
  sourceWorkOrderResult?: ProWorkOrderVO
) => {
  const displayedCode = sourceWorkOrderResult?.code || sourceWorkOrderCode || detailResult.workOrderCode
  if (!displayedCode) {
    throw new Error('当前详情缺少生产订单编号，无法查询生产用料清单')
  }
  return displayedCode
}

const loadProductionMaterialLists = async (productionOrderNo: string) => {
  productionMaterialListLoading.value = true
  productionMaterialListError.value = ''
  productionMaterialLists.value = []
  try {
    const pageSize = 100
    const rows: ErpProductionMaterialListVO[] = []
    let pageNo = 1
    let total = 0
    do {
      const data = await ErpProductionMaterialListApi.getPage({
        pageNo,
        pageSize,
        productionOrderNo
      })
      const pageRows = Array.isArray(data?.list) ? data.list : []
      rows.push(...pageRows)
      total = Number(data?.total ?? rows.length)
      pageNo += 1
      if (!pageRows.length) break
    } while (rows.length < total)
    productionMaterialLists.value = rows
  } catch (loadError) {
    productionMaterialListError.value = resolveErrorMessage(loadError, '生产用料清单加载失败')
  } finally {
    productionMaterialListLoading.value = false
  }
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  detail.value = undefined
  sourceWorkOrder.value = undefined
  productionMaterialLists.value = []
  productionMaterialListError.value = ''
  try {
    const sourceWorkOrderCode = resolveSourceWorkOrderCode()
    const [detailResult, sourceWorkOrderResult] = await Promise.all([
      getTeamLeaderActiveOrderDetail(requireActiveOrderId()),
      sourceWorkOrderCode ? loadSourceWorkOrder(sourceWorkOrderCode) : Promise.resolve(undefined)
    ])
    if (!detailResult.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法显示提交详情')
    }
    detail.value = detailResult
    sourceWorkOrder.value = sourceWorkOrderResult
    const productionOrderNo = resolveDisplayedProductionOrderNo(
      detailResult,
      sourceWorkOrderCode,
      sourceWorkOrderResult
    )
    await loadProductionMaterialLists(productionOrderNo)
  } catch (loadError) {
    error.value = resolveErrorMessage(loadError, '工序提交详情加载失败')
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const requireCurrentDetail = () => {
  if (!detail.value?.activeOrderId) {
    throw new Error('活跃订单详情尚未加载，无法打开批次执行表单')
  }
  if (detail.value.version === undefined || detail.value.version === null) {
    throw new Error('活跃订单详情缺少当前版本，无法安全生成或打开批次执行')
  }
  return detail.value
}

const resolveBatchExecutionOpenResult = async (
  currentDetail: TeamLeaderActiveOrderDetailRespVO
) => {
  const cached = batchExecutionOpenResultCache.get(currentDetail.activeOrderId)
  if (cached?.batchExecutionId) return cached
  const result = await simulateStage2_5BackfillBatchExecution({
    simulationRunId: `DETAIL-FORM-${Date.now()}`,
    activeOrderId: currentDetail.activeOrderId,
    expectedVersion: currentDetail.version
  })
  batchExecutionOpenResultCache.set(currentDetail.activeOrderId, result)
  return result
}

const handleOpenBatchExecutionSubmissionForm = async (
  mode: 'production' | 'pqc',
  process?: TeamLeaderActiveOrderProcessDetailRespVO
) => {
  if (batchExecutionOpening.value) return
  batchExecutionOpening.value = true
  try {
    const currentDetail = requireCurrentDetail()
    const result = await resolveBatchExecutionOpenResult(currentDetail)
    const query: Record<string, string | number> = {
      id: result.batchExecutionId,
      simulationRunId: result.simulationRunId,
      formSlotType: mode === 'production' ? 'MAIN' : 'PROCESS_INSPECTION'
    }
    if (mode === 'production' && process?.routeProcessId) {
      query.routeProcessId = process.routeProcessId
    }
    await router.push({
      path: '/mes/pro/feedback/edhr-batch-execution/detail',
      query
    })
  } catch (openError) {
    ElMessage.error(resolveErrorMessage(openError, '打开批次执行表单失败'))
  } finally {
    batchExecutionOpening.value = false
  }
}

watch(
  () => [route.params.activeOrderId, route.query.sourceWorkOrderCode],
  () => {
    void loadDetail()
  }
)

onMounted(loadDetail)
</script>
<style scoped>
.team-leader-workbench__active-order-detail-page {
  display: grid;
  gap: 16px;
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

</style>
