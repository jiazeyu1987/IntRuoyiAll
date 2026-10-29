<template>
  <ContentWrap>
    <ActiveOrderDetailLayout data-edhr-batch-active-order-detail-page>
      <div class="edhr-batch-active-order-detail">
        <el-tooltip content="返回" placement="top">
          <el-button
            class="edhr-batch-active-order-detail__back"
            aria-label="返回"
            circle
            data-edhr-batch-source-detail-back
            @click="goBack"
          >
            <Icon icon="ep:back" :size="18" />
          </el-button>
        </el-tooltip>
        <fieldset class="market-release-readonly" :disabled="marketReleaseReadOnly">
        <ActiveOrderSubmissionDetailPanel
          hide-correction-history
          :detail="detail"
          :production-material-lists="productionMaterialLists"
          :production-material-list-loading="productionMaterialListLoading"
          :production-material-list-error="productionMaterialListError"
          :loading="loading"
          :error="error"
          :initial-active-tab="initialActiveTab"
          audit-scope-type="BATCH"
          :audit-scope-id="auditDetailQuery"
          record-scope="FORMAL_BATCH_SOURCE_DETAIL"
          @retry="loadDetail"
        />
        </fieldset>
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
import { computed, onBeforeUnmount, onMounted, onActivated, onDeactivated, ref, watch } from 'vue'
import { getMarketReleaseAccess } from '@/api/mes/pro/edhr/marketReleaseHandover'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ActiveOrderSubmissionDetailPanel from '@/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from '@/views/mes/pro/processpool/components/ActiveOrderDetailLayout.vue'
import BatchReverseTracePanel from './components/BatchReverseTracePanel.vue'
import { canReverseTrace, readReverseTraceState, reverseTraceHistoryPath } from './reverseTraceState'
import { getEdhrBatchActiveOrderDetail, getEdhrBatchActiveOrderProductionMaterialLists, type EdhrRouteId } from '@/api/mes/pro/edhr/batchExecution'
import type { ErpProductionMaterialListVO } from '@/api/erp/production/material-list'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'

defineOptions({ name: 'MesProEdhrBatchExecutionActiveOrderDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const productionMaterialLists = ref<ErpProductionMaterialListVO[]>([])
const productionMaterialListLoading = ref(false)
const productionMaterialListError = ref('')
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

let detailSequence = 0
const loadProductionMaterialLists = async (query: ReturnType<typeof resolveDetailQuery>, sequence: number) => {
  productionMaterialListLoading.value = true
  try {
    const rows = await getEdhrBatchActiveOrderProductionMaterialLists(query)
    if (sequence !== detailSequence) return
    if (!Array.isArray(rows)) throw new Error('生产用料清单返回格式错误')
    productionMaterialLists.value = rows
  } catch (errorValue) {
    if (sequence !== detailSequence) return
    productionMaterialListError.value = `生产用料清单加载失败：${resolveErrorMessage(errorValue)}`
  } finally {
    if (sequence === detailSequence) productionMaterialListLoading.value = false
  }
}

const loadDetail = async () => {
  const sequence = ++detailSequence
  loading.value = true
  error.value = ''
  detail.value = undefined
  productionMaterialLists.value = []
  productionMaterialListLoading.value = false
  productionMaterialListError.value = ''
  try {
    const query = resolveDetailQuery()
    const result = await getEdhrBatchActiveOrderDetail(query)
    if (sequence !== detailSequence) return
    if (!result.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法展示详情批记录。')
    }
    detail.value = result
    void loadProductionMaterialLists(query, sequence)
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

const marketReleaseReadOnly = ref(true)
let accessTimer: ReturnType<typeof setInterval> | undefined
let accessGeneration = 0
let accessChecking = false
const refreshMarketReleaseAccess = async () => {
  if (accessChecking || document.hidden) return
  const scope = JSON.stringify(resolveDetailQuery()), generation = accessGeneration
  accessChecking = true
  try {
    const state = await getMarketReleaseAccess(resolveDetailQuery())
    if (generation === accessGeneration && scope === JSON.stringify(resolveDetailQuery())) marketReleaseReadOnly.value = !state.writable
  } catch (failure) {
    if (generation !== accessGeneration || scope !== JSON.stringify(resolveDetailQuery())) return
    detailSequence++; detail.value = undefined; productionMaterialLists.value = []; reverseTraceVisible.value = false
    marketReleaseReadOnly.value = true
    ElMessage.error(failure instanceof Error ? failure.message : '该批记录访问权限已变化，请重新进入列表。')
    await router.replace('/mes/pro/feedback/edhr-batch-execution')
  } finally { accessChecking = false }
}
const stopMarketReleaseAccess = () => { accessGeneration++; clearInterval(accessTimer) }
const startMarketReleaseAccess = () => {
  stopMarketReleaseAccess()
  void refreshMarketReleaseAccess()
  accessTimer = setInterval(refreshMarketReleaseAccess, 5000)
}
onMounted(startMarketReleaseAccess)
onActivated(startMarketReleaseAccess)
onDeactivated(stopMarketReleaseAccess)
onBeforeUnmount(stopMarketReleaseAccess)

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
  position: relative;
  display: grid;
  gap: 12px;
  min-width: 0;
}
.edhr-batch-active-order-detail__back {
  position: absolute;
  top: 18px;
  right: 0;
  z-index: 2;
}
.edhr-batch-active-order-detail :deep(.team-leader-workbench__active-order-detail-summary) {
  margin-right: 44px;
}
</style>

<style scoped>
.market-release-readonly { border: 0; margin: 0; padding: 0; min-width: 0; }
.market-release-readonly:disabled { opacity: 0.65; }
</style>
