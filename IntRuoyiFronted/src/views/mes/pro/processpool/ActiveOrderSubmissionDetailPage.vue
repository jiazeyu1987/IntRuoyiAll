<template>
  <ContentWrap>
    <div class="team-leader-workbench__active-order-detail-page" data-team-leader-active-order-detail-page>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :source-work-order="sourceWorkOrder"
        :loading="loading"
        :error="error"
        @retry="loadDetail"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ActiveOrderSubmissionDetailPanel from './components/ActiveOrderSubmissionDetailPanel.vue'
import {
  getTeamLeaderActiveOrderDetail,
  type TeamLeaderActiveOrderDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'

defineOptions({ name: 'MesProcessPoolActiveOrderSubmissionDetail' })

const route = useRoute()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const sourceWorkOrder = ref<ProWorkOrderVO>()
const loading = ref(false)
const error = ref('')

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

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  detail.value = undefined
  sourceWorkOrder.value = undefined
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
  } catch (loadError) {
    error.value = resolveErrorMessage(loadError, '工序提交详情加载失败')
    ElMessage.error(error.value)
  } finally {
    loading.value = false
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
