<template>
  <ContentWrap>
    <section class="edhr-batch-active-order-detail" data-edhr-batch-active-order-detail-page>
      <el-button :icon="ArrowLeft" data-edhr-batch-active-order-detail-back @click="goBack">
        返回
      </el-button>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :loading="loading"
        :error="error"
        :record-scope="'FORMAL_BATCH_SOURCE_DETAIL'"
        @retry="loadDetail"
      />
    </section>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { getEdhrBatchActiveOrderDetail } from '@/api/mes/pro/edhr/batchExecution'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'
import ActiveOrderSubmissionDetailPanel from '@/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'

defineOptions({ name: 'MesProEdhrBatchExecutionActiveOrderDetail' })

const router = useRouter()
const route = useRoute()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const loading = ref(false)
const error = ref('')

const resolveErrorMessage = (loadError: unknown, fallback: string) => {
  const responseMessage =
    (loadError as any)?.response?.data?.msg || (loadError as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (loadError instanceof Error && loadError.message.trim()) return loadError.message
  return fallback
}

const resolveBatchExecutionId = () => {
  const value = Array.isArray(route.query.batchExecutionId)
    ? route.query.batchExecutionId[0]
    : route.query.batchExecutionId
  const id = Number(value)
  if (!Number.isSafeInteger(id) || id <= 0) {
    throw new Error('缺少正式批次执行来源，无法展示活跃订单详情。')
  }
  return id
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  detail.value = undefined
  try {
    const nextDetail = await getEdhrBatchActiveOrderDetail(resolveBatchExecutionId())
    if (!nextDetail.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法展示详情批记录。')
    }
    detail.value = nextDetail
  } catch (loadError) {
    error.value = resolveErrorMessage(loadError, '活跃订单详情批记录加载失败。')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  const from = Array.isArray(route.query.from) ? route.query.from[0] : route.query.from
  if (from && from.startsWith('/mes/pro/feedback/edhr-batch-')) {
    router.push(from)
    return
  }
  router.back()
}

onMounted(loadDetail)
</script>

<style scoped>
.edhr-batch-active-order-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-active-order-detail > .el-button {
  align-self: flex-start;
}
</style>
