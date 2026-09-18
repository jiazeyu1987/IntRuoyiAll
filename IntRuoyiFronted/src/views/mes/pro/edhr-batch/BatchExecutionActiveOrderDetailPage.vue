<template>
  <ContentWrap>
    <div class="edhr-batch-source-detail" data-edhr-batch-source-detail-page>
      <div class="edhr-batch-source-detail__toolbar">
        <el-button data-edhr-batch-source-detail-back @click="goBack">返回</el-button>
      </div>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :loading="loading"
        :error="error"
        :initial-active-tab="initialActiveTab"
        record-scope="FORMAL_BATCH_SOURCE_DETAIL"
        @retry="loadDetail"
      />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ActiveOrderSubmissionDetailPanel from '@/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import { getEdhrBatchActiveOrderDetail, type EdhrRouteId } from '@/api/mes/pro/edhr/batchExecution'
import type { TeamLeaderActiveOrderDetailRespVO } from '@/api/mes/pro/processpool/teamLeader'

defineOptions({ name: 'MesProEdhrBatchExecutionActiveOrderDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const loading = ref(false)
const error = ref('')

const initialActiveTab = computed(() => {
  const requestedTab = typeof route.query.tab === 'string' ? route.query.tab.trim() : ''
  return requestedTab === 'otherUpload' ? 'dossierFile:OTHER_FILE' : undefined
})

const parseBatchExecutionId = (): EdhrRouteId => {
  const value =
    typeof route.query.batchExecutionId === 'string' ? route.query.batchExecutionId.trim() : ''
  if (!/^\d+$/.test(value) || Number(value) <= 0) {
    throw new Error('缺少有效批次执行编号，无法查看详情批记录。')
  }
  return value
}

const resolveReturnPath = () => {
  const source = typeof route.query.from === 'string' ? route.query.from.trim() : ''
  if (source === 'execution') return '/mes/pro/feedback/edhr-batch-execution'
  if (source === 'history') return '/mes/pro/feedback/edhr-batch-history'
  throw new Error('缺少详情来源页面，无法返回原列表。')
}

const resolveErrorMessage = (errorValue: unknown) => {
  if (errorValue instanceof Error && errorValue.message.trim()) return errorValue.message
  const responseMessage = (
    errorValue as { response?: { data?: { msg?: string; message?: string } } }
  )?.response?.data
  return responseMessage?.msg || responseMessage?.message || '活跃订单详情批记录加载失败。'
}

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  detail.value = undefined
  try {
    const result = await getEdhrBatchActiveOrderDetail(parseBatchExecutionId())
    if (!result.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法展示详情批记录。')
    }
    detail.value = result
  } catch (errorValue) {
    error.value = resolveErrorMessage(errorValue)
  } finally {
    loading.value = false
  }
}

const goBack = async () => {
  await router.push({ path: resolveReturnPath() })
}

watch(
  () => [route.query.batchExecutionId, route.query.from],
  () => {
    void loadDetail()
  }
)

onMounted(loadDetail)
</script>

<style scoped>
.edhr-batch-source-detail {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.edhr-batch-source-detail__toolbar {
  display: flex;
  justify-content: flex-start;
}
</style>
