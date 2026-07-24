<template>
  <ContentWrap>
    <div class="edhr-query">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-query__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无追踪记录"
        >
          <el-table-column type="expand" width="40">
            <template #default="{ row }">
              <div class="edhr-tracking__evidence">
                <div class="edhr-tracking__evidence-title">追踪证据</div>
                <div class="edhr-tracking__evidence-sections">
                  <div class="edhr-tracking__evidence-section">
                    <div class="edhr-tracking__evidence-section-title">普通工序填写签名证据</div>
                    <div class="edhr-tracking__evidence-section-copy">
                      当前工序以填写提交签名作为流转证据，普通工序不要求审核/批准。
                    </div>
                    <el-tag effect="plain" type="success">
                      {{ row.lastEvidenceCategoryName || resolveEvidenceCategoryName(row.lastEventType) }}
                    </el-tag>
                  </div>
                  <div class="edhr-tracking__evidence-section">
                    <div class="edhr-tracking__evidence-section-title">放行阶段审核/批准证据</div>
                    <div class="edhr-tracking__evidence-section-copy">
                      质量审核和批准在放行阶段追溯；历史工序审核/批准证据（只读）仅作为既有记录展示。
                    </div>
                    <el-tag effect="plain" type="warning">放行阶段审核/批准证据</el-tag>
                  </div>
                </div>
                <div class="edhr-tracking__evidence-grid">
                  <div class="edhr-tracking__evidence-item">
                    <span>执行记录</span>
                    <strong>{{ row.executionId || '--' }}</strong>
                  </div>
                  <div class="edhr-tracking__evidence-item">
                    <span>工单编号</span>
                    <strong>{{ row.workOrderId || '--' }}</strong>
                  </div>
                  <div class="edhr-tracking__evidence-item">
                    <span>批次编号</span>
                    <strong>{{ row.batchId || '--' }}</strong>
                  </div>
                  <div class="edhr-tracking__evidence-item">
                    <span>流程实例</span>
                    <strong>{{ row.processInstanceId || '--' }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="执行编号" width="190">
            <template #default="{ row }">
              <el-button
                v-if="canOpenDetail(row)"
                link
                type="primary"
                class="edhr-tracking__execution-link"
                @click="openTrackingDetail(row)"
              >
                {{ resolveExecutionDisplay(row) }}
              </el-button>
              <el-tooltip v-else content="缺少执行记录 ID，无法打开执行追踪。">
                <span class="edhr-tracking__execution-link edhr-tracking__execution-link--disabled">
                  {{ resolveExecutionDisplay(row) }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="生产上下文" min-width="270">
            <template #default="{ row }">
              <div>
                <el-button
                  v-if="row.workOrderId"
                  link
                  type="primary"
                  class="edhr-tracking__link"
                  @click="openWorkOrder(row)"
                >
                  {{ row.workOrderCode || '--' }}
                </el-button>
                <span v-else class="edhr-tracking__strong">{{ row.workOrderCode || '--' }}</span>
              </div>
              <div class="edhr-tracking__muted">
                批次：
                <el-button
                  v-if="row.batchId"
                  link
                  type="primary"
                  class="edhr-tracking__inline-link"
                  @click="openBatch(row)"
                >
                  {{ row.batchCode || '--' }}
                </el-button>
                <span v-else>{{ row.batchCode || '--' }}</span>
              </div>
              <div class="edhr-tracking__muted">
                {{ row.processName || '--' }} / {{ row.workstationName || '--' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="当前阶段" min-width="190">
            <template #default="{ row }">
              <el-tag :type="resolveStatusType(row.status)" effect="plain">
                {{ resolveStatusLabel(row.status) }}
              </el-tag>
              <div class="edhr-tracking__muted">{{ row.currentNodeName || '--' }}</div>
              <div class="edhr-tracking__muted">
                {{ row.currentAssigneeNames?.join('、') || '--' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="最后处理" min-width="230">
            <template #default="{ row }">
              <div class="edhr-tracking__strong">{{ formatTrackingEvent(row.lastEventType) }}</div>
              <div class="edhr-tracking__muted">{{ formatTrackingLastEventAt(row.lastEventAt) }}</div>
              <div class="edhr-tracking__muted">{{ row.lastEventReason || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="归档状态" width="120">
            <template #default="{ row }">
              <el-tag :type="formatArchiveStatusType(row.archiveStatus)" effect="plain">
                {{ formatArchiveStatusLabel(row.archiveStatus) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>
    <BatchForm ref="batchFormRef" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { EDHR_EXECUTION_STATUS } from '@/api/mes/pro/edhr/approval'
import {
  getEdhrTrackingPage,
  type EdhrTrackingEventType,
  type EdhrTrackingRowVO
} from '@/api/mes/pro/edhr/tracking'
import { formatDate } from '@/utils/formatTime'
import BatchForm from '@/views/mes/wm/batch/BatchForm.vue'

defineOptions({ name: 'MesProFeedbackEdhrTracking' })

const route = useRoute()
const router = useRouter()
const batchFormRef = ref<InstanceType<typeof BatchForm>>()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrTrackingRowVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  executionCode: typeof route.query.executionCode === 'string' ? route.query.executionCode : '',
  workOrderCode: typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : '',
  batchCode: typeof route.query.batchCode === 'string' ? route.query.batchCode : '',
  processId: undefined as number | undefined,
  workstationId: undefined as number | undefined,
  status: undefined as number | undefined,
  submittedBy: undefined as number | undefined,
  approvedBy: undefined as number | undefined,
  processInstanceId: '',
  actorName: ''
})

const TRACKING_EVENT_LABELS: Record<EdhrTrackingEventType, string> = {
  CREATE: '创建',
  SAVE_DRAFT: '保存草稿',
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '填写提交签名',
  REVIEW_APPROVE: '审核签名',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}
const ARCHIVE_STATUS_LABELS: Record<NonNullable<EdhrTrackingRowVO['archiveStatus']>, string> = {
  GENERATING: '生成中',
  SEALED: '已封存',
  FAILED: '生成失败'
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const message = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof message === 'string' && message.trim()) return message
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const normalizePositiveProcessId = (value?: number | string | null) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const resolveStatusLabel = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return '待审批'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return '已驳回'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return '已关闭'
  if (status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) return '填写完成'
  return '草稿'
}

const resolveStatusType = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return 'warning'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return 'danger'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return 'success'
  if (status === EDHR_EXECUTION_STATUS.FILL_COMPLETED) return 'success'
  return 'info'
}

const formatTrackingEvent = (eventType?: EdhrTrackingEventType) => {
  if (!eventType) return '--'
  const label = TRACKING_EVENT_LABELS[eventType]
  if (!label) {
    throw new Error(`未知追踪最后事件：${String(eventType)}`)
  }
  return label
}

const resolveEvidenceCategoryName = (eventType?: EdhrTrackingEventType) => {
  if (eventType === 'SUBMIT') return '普通工序填写提交证据'
  if (eventType === 'FORM_REVIEW') return '历史工序审核/批准证据（只读）'
  if (eventType === 'APPROVE' || eventType === 'REVIEW_APPROVE' || eventType === 'REJECT') {
    return '放行阶段审核/批准证据'
  }
  if (eventType === 'ARCHIVE_SEAL') return '归档封存证据'
  return '技术追踪证据'
}

const formatArchiveStatusLabel = (archiveStatus?: EdhrTrackingRowVO['archiveStatus']) => {
  if (!archiveStatus) return '未归档'
  const label = ARCHIVE_STATUS_LABELS[archiveStatus]
  if (!label) {
    throw new Error(`未知归档状态：${String(archiveStatus)}`)
  }
  return label
}

const formatArchiveStatusType = (archiveStatus?: EdhrTrackingRowVO['archiveStatus']) => {
  if (archiveStatus === 'SEALED') return 'success'
  if (archiveStatus === 'FAILED') return 'danger'
  if (archiveStatus === 'GENERATING') return 'warning'
  return 'info'
}

const formatTrackingLastEventAt = (lastEventAt?: string | number | Date) => {
  if (!lastEventAt) return '--'
  const parsedDate =
    typeof lastEventAt === 'number' || /^\d+$/.test(String(lastEventAt))
      ? new Date(Number(lastEventAt))
      : new Date(lastEventAt)
  if (Number.isNaN(parsedDate.getTime())) {
    throw new Error(`最后处理时间不可解析：${String(lastEventAt)}`)
  }
  return formatDate(parsedDate, 'YYYY年M月D日')
}

const resolveExecutionDisplay = (row: EdhrTrackingRowVO) =>
  row.executionCode || (row.executionId ? `#${row.executionId}` : '--')

const canOpenDetail = (row: EdhrTrackingRowVO) =>
  Number.isFinite(Number(row.executionId)) && Number(row.executionId) > 0

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrTrackingPage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      executionCode: queryParams.executionCode.trim() || undefined,
      workOrderCode: queryParams.workOrderCode.trim() || undefined,
      batchCode: queryParams.batchCode.trim() || undefined,
      processId: normalizePositiveProcessId(queryParams.processId),
      workstationId: Number.isFinite(queryParams.workstationId) ? queryParams.workstationId : undefined,
      status: queryParams.status as any,
      submittedBy: Number.isFinite(queryParams.submittedBy) ? queryParams.submittedBy : undefined,
      approvedBy: Number.isFinite(queryParams.approvedBy) ? queryParams.approvedBy : undefined,
      processInstanceId: queryParams.processInstanceId.trim() || undefined,
      actorName: queryParams.actorName.trim() || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 追踪列表加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const openTrackingDetail = async (row: EdhrTrackingRowVO) => {
  if (!canOpenDetail(row)) {
    throw new Error('缺少执行记录 ID，无法打开执行追踪。')
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row.executionId), viewMode: 'tracking' }
  })
}

const openWorkOrder = async (row: EdhrTrackingRowVO) => {
  if (!row.workOrderId) {
    throw new Error(`追踪行缺少工单ID：executionId=${row.executionId}`)
  }
  await router.push({
    path: '/mes/pro/work-order',
    query: {
      code: row.workOrderCode,
      openId: String(row.workOrderId)
    }
  })
}

const openBatch = (row: EdhrTrackingRowVO) => {
  if (!row.batchId) {
    throw new Error(`追踪行缺少批次ID：executionId=${row.executionId}`)
  }
  if (!batchFormRef.value) {
    throw new Error('批次详情组件未挂载')
  }
  batchFormRef.value.open(row.batchId)
}

onMounted(() => getList())
</script>

<style scoped>
.edhr-query__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}
.edhr-query__table {
  border-radius: 8px;
}

.edhr-tracking__execution-link,
.edhr-tracking__link,
.edhr-tracking__inline-link {
  padding: 0;
  font-weight: 600;
}

.edhr-tracking__execution-link--disabled {
  color: #8a94a6;
  cursor: not-allowed;
}

.edhr-tracking__inline-link {
  font-size: 12px;
}

.edhr-tracking__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-tracking__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-tracking__evidence {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-tracking__evidence-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-tracking__evidence-sections {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.edhr-tracking__evidence-section {
  padding: 12px;
  border: 1px solid #d9ecff;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-tracking__evidence-section-title {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-tracking__evidence-section-copy {
  margin: 6px 0 8px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-tracking__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.edhr-tracking__evidence-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-tracking__evidence-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-tracking__evidence-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}
</style>
