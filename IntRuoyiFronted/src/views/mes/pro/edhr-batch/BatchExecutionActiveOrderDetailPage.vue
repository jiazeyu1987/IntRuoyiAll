<template>
  <ContentWrap>
    <ActiveOrderDetailLayout data-edhr-batch-active-order-detail-page>
      <div class="edhr-batch-active-order-detail">
        <div class="edhr-batch-active-order-detail__toolbar">
          <el-button data-edhr-batch-source-detail-back @click="goBack">返回</el-button>
          <el-button
            v-if="canOpenReverseTrace"
            type="primary"
            plain
            data-edhr-detail-reverse-trace
            @click="reverseTraceVisible = true"
          >
            反查关联批次
          </el-button>
        </div>
        <ActiveOrderSubmissionDetailPanel
          :detail="detail"
          :loading="loading"
          :error="error"
          :initial-active-tab="initialActiveTab"
          record-scope="FORMAL_BATCH_SOURCE_DETAIL"
          @retry="loadDetail"
        />
        <BatchReverseTracePanel
          v-model:visible="reverseTraceVisible"
          :anchor-batch-execution-id="batchExecutionId"
          :restore-key="reverseTraceRestoreKey"
        />
      </div>
    </ActiveOrderDetailLayout>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ActiveOrderSubmissionDetailPanel from '@/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from '@/views/mes/pro/processpool/components/ActiveOrderDetailLayout.vue'
import BatchReverseTracePanel from './components/BatchReverseTracePanel.vue'
import { canReverseTrace, readReverseTraceState, reverseTraceHistoryPath } from './reverseTraceState'
import { getEdhrBatchActiveOrderDetail, type EdhrRouteId } from '@/api/mes/pro/edhr/batchExecution'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'

defineOptions({ name: 'MesProEdhrBatchExecutionActiveOrderDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const loading = ref(false)
const error = ref('')
const reverseTraceVisible = ref(false)
const batchExecutionId = computed(() => {
  const value = typeof route.query.batchExecutionId === 'string' ? route.query.batchExecutionId.trim() : ''
  return value
})
const hasBatchExecutionId = computed(() => /^\d+$/.test(batchExecutionId.value) && Number(batchExecutionId.value) > 0)
const isHistoryDetail = computed(() => {
  const source = typeof route.query.from === 'string' ? route.query.from.trim() : ''
  return source === 'history' || source === '/mes/pro/feedback/edhr-batch-history'
})
const canOpenReverseTrace = computed(() => hasBatchExecutionId.value && isHistoryDetail.value && canReverseTrace())
const reverseTraceRestoreKey = computed(() => typeof route.query.reverseTraceRestore === 'string' ? route.query.reverseTraceRestore : undefined)

const initialActiveTab = computed(() => {
  const requestedTab = typeof route.query.tab === 'string' ? route.query.tab.trim() : ''
  return requestedTab === 'otherUpload' ? 'dossierFile:OTHER_FILE' : undefined
})

const parseBatchExecutionId = (): EdhrRouteId => {
  const value = batchExecutionId.value
  if (!/^\d+$/.test(value) || Number(value) <= 0) {
    throw new Error('缺少有效批次执行编号，无法查看详情批记录。')
  }
  return value
}

const parseActiveOrderId = (): EdhrRouteId | undefined => {
  const value = typeof route.query.activeOrderId === 'string' ? route.query.activeOrderId.trim() : ''
  if (!value) return undefined
  if (!/^\d+$/.test(value) || Number(value) <= 0) {
    throw new Error('缺少有效活跃订单编号，无法查看详情批记录。')
  }
  return value
}

const resolveDetailQuery = () => {
  const activeOrderId = parseActiveOrderId()
  if (activeOrderId) {
    return { activeOrderId }
  }
  return { batchExecutionId: parseBatchExecutionId() }
}

const resolveReturnPath = () => {
  const source = typeof route.query.from === 'string' ? route.query.from.trim() : ''
  if (source === '/mes/pro/feedback/edhr-batch-voided') {
    return '/mes/pro/feedback/edhr-batch-voided'
  }
  if (source === 'execution' || source === '/mes/pro/feedback/edhr-batch-execution') {
    return '/mes/pro/feedback/edhr-batch-execution'
  }
  if (source === 'history' || source === '/mes/pro/feedback/edhr-batch-history') {
    return '/mes/pro/feedback/edhr-batch-history'
  }
  throw new Error('缺少详情来源页面，无法返回原列表。')
}

const resolveErrorMessage = (errorValue: unknown) => {
  if (errorValue instanceof Error && errorValue.message.trim()) return errorValue.message
  const responseMessage = (
    errorValue as { response?: { data?: { msg?: string; message?: string } } }
  )?.response?.data
  return responseMessage?.msg || responseMessage?.message || '活跃订单详情批记录加载失败。'
}

let detailSequence = 0
const loadDetail = async () => {
  const sequence = ++detailSequence
  loading.value = true
  error.value = ''
  detail.value = undefined
  try {
    const result = await getEdhrBatchActiveOrderDetail(resolveDetailQuery())
    if (sequence !== detailSequence) return
    if (!result.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法展示详情批记录。')
    }
    detail.value = result
  } catch (errorValue) {
    if (sequence !== detailSequence) return
    error.value = resolveErrorMessage(errorValue)
  } finally {
    if (sequence === detailSequence) loading.value = false
  }
}

const goBack = async () => {
  if (typeof route.query.reverseTraceReturn === 'string') {
    const saved = readReverseTraceState(route.query.reverseTraceReturn)
    if (saved) {
      await router.push({ path: saved.origin.path, query: { ...saved.origin.query,
        reverseTraceRestore: route.query.reverseTraceReturn, reverseTraceAnchor: saved.anchorBatchExecutionId, tab: 'reverseTrace' } })
      return
    }
    await router.push({ path: reverseTraceHistoryPath, query: { reverseTraceExpired: '1' } })
    return
  }
  await router.push({ path: resolveReturnPath() })
}

watch(
  () => [route.query.batchExecutionId, route.query.activeOrderId, route.query.from],
  () => {
    void loadDetail()
  }
)

onMounted(loadDetail)
onBeforeUnmount(() => { detailSequence++; loading.value = false })

watch(
  () => [route.query.tab, route.query.batchExecutionId, reverseTraceRestoreKey.value, canOpenReverseTrace.value],
  () => {
    reverseTraceVisible.value = canOpenReverseTrace.value && route.query.tab === 'reverseTrace'
  },
  { immediate: true }
)
</script>

<style scoped>
.edhr-batch-active-order-detail {
  display: grid;
  gap: 12px;
  min-width: 0;
}
</style>
