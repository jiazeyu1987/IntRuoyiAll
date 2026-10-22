<template>
  <ContentWrap>
    <ActiveOrderDetailLayout data-team-leader-active-order-detail-page>
      <el-button data-team-leader-active-order-detail-back @click="goBack">返回</el-button>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :source-work-order="sourceWorkOrder"
        :production-material-lists="productionMaterialLists"
        :production-material-list-loading="productionMaterialListLoading"
        :production-material-list-error="productionMaterialListError"
        :loading="loading"
        :error="error"
        @retry="loadDetail"
      />
    </ActiveOrderDetailLayout>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import ActiveOrderSubmissionDetailPanel from './components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from './components/ActiveOrderDetailLayout.vue'
import {
  getTeamLeaderActiveOrderDetail,
  getTeamLeaderActiveOrderProductionMaterialLists,
  type TeamLeaderActiveOrderDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'
import type { ErpProductionMaterialListVO } from '@/api/erp/production/material-list'

defineOptions({ name: 'MesProcessPoolActiveOrderSubmissionDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const sourceWorkOrder = ref<ProWorkOrderVO>()
const productionMaterialLists = ref<ErpProductionMaterialListVO[]>([])
const productionMaterialListLoading = ref(false)
const productionMaterialListError = ref('')
let detailRequestGeneration = 0
const loading = ref(false)
const error = ref('')

const resolveErrorMessage = (errorValue: unknown, fallback: string) => {
  if (errorValue instanceof Error && errorValue.message) return errorValue.message
  if (typeof errorValue === 'string' && errorValue.trim()) return errorValue
  const responseMessage = (
    errorValue as { response?: { data?: { msg?: string; message?: string } } }
  )?.response?.data
  return responseMessage?.msg || responseMessage?.message || fallback
}

const requireActiveOrderId = () => {
  const activeOrderId = parsePositiveRouteQueryId(route.params.activeOrderId)
  if (!activeOrderId) {
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
    throw new Error(
      `Stage1 来源生产工单 ${sourceWorkOrderCode} 未找到或不唯一，无法显示真实生产工单资料`
    )
  }
  return rows[0]
}

const loadProductionMaterialLists = async (activeOrderId: string, requestGeneration: number) => {
  productionMaterialListLoading.value = true
  productionMaterialLists.value = []
  productionMaterialListError.value = ''
  try {
    const rows = await getTeamLeaderActiveOrderProductionMaterialLists(activeOrderId)
    if (requestGeneration !== detailRequestGeneration) return
    if (!Array.isArray(rows)) throw new Error('生产用料清单返回格式错误')
    productionMaterialLists.value = rows
  } catch (loadError) {
    if (requestGeneration !== detailRequestGeneration) return
    productionMaterialLists.value = []
    productionMaterialListError.value = resolveErrorMessage(loadError, '生产用料清单加载失败')
  } finally {
    if (requestGeneration === detailRequestGeneration) productionMaterialListLoading.value = false
  }
}

const loadDetail = async () => {
  const requestGeneration = ++detailRequestGeneration
  loading.value = true
  error.value = ''
  detail.value = undefined
  sourceWorkOrder.value = undefined
  productionMaterialLists.value = []
  productionMaterialListLoading.value = false
  productionMaterialListError.value = ''
  try {
    const activeOrderId = requireActiveOrderId()
    const sourceWorkOrderCode = resolveSourceWorkOrderCode()
    const [detailResult, sourceWorkOrderResult] = await Promise.all([
      getTeamLeaderActiveOrderDetail(activeOrderId),
      sourceWorkOrderCode ? loadSourceWorkOrder(sourceWorkOrderCode) : Promise.resolve(undefined)
    ])
    if (requestGeneration !== detailRequestGeneration) return
    if (!detailResult.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法显示提交详情')
    }
    detail.value = detailResult
    sourceWorkOrder.value = sourceWorkOrderResult
    void loadProductionMaterialLists(activeOrderId, requestGeneration)
  } catch (loadError) {
    if (requestGeneration !== detailRequestGeneration) return
    error.value = resolveErrorMessage(loadError, '工序提交详情加载失败')
    ElMessage.error(error.value)
  } finally {
    if (requestGeneration === detailRequestGeneration) loading.value = false
  }
}

const goBack = () => {
  router.back()
}

watch(
  () => [route.params.activeOrderId, route.query.sourceWorkOrderCode],
  () => {
    void loadDetail()
  }
)

onMounted(loadDetail)
onBeforeUnmount(() => {
  detailRequestGeneration += 1
})
</script>
