<template>
  <ContentWrap>
    <div class="team-leader-workbench__active-order-detail-page" data-team-leader-active-order-detail-page>
      <div class="team-leader-workbench__active-order-detail-page-header">
        <div>
          <div class="team-leader-workbench__active-order-detail-page-eyebrow">活跃订单</div>
          <h2>
            {{
              detail
                ? stage1SourceWorkOrderCode
                  ? `Stage1模拟详情：${stage1SourceWorkOrderCode} → ${detail.workOrderCode}`
                  : `订单 ${detail.workOrderCode} · 工序提交详情`
                : '工序提交详情'
            }}
          </h2>
        </div>
        <el-button @click="goBack">返回</el-button>
      </div>
      <ActiveOrderSubmissionDetailPanel
        :detail="detail"
        :loading="loading"
        :error="error"
        :stage1-source-work-order-code="stage1SourceWorkOrderCode"
        @retry="loadDetail"
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
  type TeamLeaderActiveOrderDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'

defineOptions({ name: 'MesProcessPoolActiveOrderSubmissionDetail' })

const route = useRoute()
const router = useRouter()

const detail = ref<TeamLeaderActiveOrderDetailRespVO>()
const loading = ref(false)
const error = ref('')
const stage1SourceWorkOrderCode = ref('')

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

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  detail.value = undefined
  stage1SourceWorkOrderCode.value = String(route.query.sourceWorkOrderCode || '').trim()
  try {
    const detailResult = await getTeamLeaderActiveOrderDetail(requireActiveOrderId())
    if (!detailResult.processes?.length) {
      throw new Error('活跃订单缺少正式工序目标，无法显示提交详情')
    }
    detail.value = detailResult
  } catch (loadError) {
    error.value = resolveErrorMessage(loadError, '工序提交详情加载失败')
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push({ name: 'MesProProcessPoolProductionLeaderWorkbench' })
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

.team-leader-workbench__active-order-detail-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  min-width: 0;
}

.team-leader-workbench__active-order-detail-page-header h2 {
  margin: 4px 0 0;
  color: var(--el-text-color-primary);
  font-size: 20px;
  line-height: 1.35;
  word-break: break-word;
}

.team-leader-workbench__active-order-detail-page-eyebrow {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
