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
        <el-tabs v-model="activeTab" class="edhr-batch-active-order-detail__tabs">
          <el-tab-pane label="批记录详情" name="detail">
            <ActiveOrderSubmissionDetailPanel
              :detail="detail"
              :loading="loading"
              :error="error"
              :initial-active-tab="initialActiveTab"
              audit-scope-type="BATCH"
              :audit-scope-id="auditDetailQuery"
              record-scope="FORMAL_BATCH_SOURCE_DETAIL"
              @retry="loadDetail"
            />
          </el-tab-pane>
          <el-tab-pane label="偏差" name="deviation">
            <el-skeleton v-if="deviationLoading" :rows="4" animated />
            <el-alert
              v-else-if="deviationError"
              :title="deviationError"
              type="error"
              :closable="false"
              show-icon
            >
              <el-button link type="primary" @click="loadDeviationBatches">重试</el-button>
            </el-alert>
            <template v-else-if="deviationBatches.length">
              <section v-for="batch in deviationBatches" :key="batch.batchExecutionId">
                <h3 v-if="deviationBatches.length > 1">{{ batch.batchExecutionCode }}</h3>
                <DeviationTracePane :batch-execution-id="batch.batchExecutionId" />
              </section>
            </template>
            <el-empty v-else description="暂无正式批记录，无法查看偏差" />
          </el-tab-pane>
        </el-tabs>
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
import DeviationTracePane from '@/views/mes/pro/edhr/components/DeviationTracePane.vue'
import BatchReverseTracePanel from './components/BatchReverseTracePanel.vue'
import { canReverseTrace, readReverseTraceState, reverseTraceHistoryPath } from './reverseTraceState'
import { getEdhrBatchActiveOrderDetail, type EdhrRouteId } from '@/api/mes/pro/edhr/batchExecution'
import {
  getDeviationBatchOptionsByActiveOrder,
  type DeviationBatchOptionRespVO
} from '@/api/mes/pro/edhr/deviation'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'

defineOptions({ name: 'MesProEdhrBatchExecutionActiveOrderDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const loading = ref(false)
const error = ref('')
const activeTab = ref<'detail' | 'deviation'>('detail')
const deviationBatches = ref<DeviationBatchOptionRespVO[]>([])
const deviationLoading = ref(false)
const deviationError = ref('')
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

const auditDetailQuery = computed(() => {
  if (!detail.value || loading.value || error.value) return undefined
  return resolveDetailQuery()
})

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

let deviationSequence = 0
const loadDeviationBatches = async () => {
  const sequence = ++deviationSequence
  deviationLoading.value = true
  deviationError.value = ''
  deviationBatches.value = []
  try {
    const query = resolveDetailQuery()
    const batches = query.activeOrderId
      ? await getDeviationBatchOptionsByActiveOrder(Number(query.activeOrderId))
      : [{ batchExecutionId: Number(query.batchExecutionId) }]
    if (sequence !== deviationSequence) return
    deviationBatches.value = batches
  } catch (errorValue) {
    if (sequence !== deviationSequence) return
    deviationError.value = resolveErrorMessage(errorValue)
  } finally {
    if (sequence === deviationSequence) deviationLoading.value = false
  }
}

watch(
  () => [activeTab.value, route.query.batchExecutionId, route.query.activeOrderId],
  () => {
    deviationSequence++
    deviationBatches.value = []
    deviationError.value = ''
    deviationLoading.value = false
    if (activeTab.value === 'deviation') void loadDeviationBatches()
  }
)

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
onBeforeUnmount(() => { detailSequence++; deviationSequence++; loading.value = false })

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
