<template>
  <ContentWrap>
    <div class="edhr-detail">
      <div class="edhr-detail__header">
        <div>
          <div class="edhr-detail__title">eDHR 审批详情</div>
          <div class="edhr-detail__subtitle">审批详情只读展示执行快照、追踪、签名记录和归档状态</div>
        </div>
        <div class="edhr-detail__actions">
          <el-button @click="goBack">返回审批列表</el-button>
          <el-button type="primary" :loading="loading" @click="loadDetail">刷新</el-button>
        </div>
      </div>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <template v-if="detail">
        <el-descriptions :column="2" border class="edhr-detail__summary">
          <el-descriptions-item label="执行编号">{{ detail.executionCode }}</el-descriptions-item>
          <el-descriptions-item label="工单号">{{ detail.workOrderCode }}</el-descriptions-item>
          <el-descriptions-item label="批次号">{{ detail.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="工序">{{ detail.processName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="工作站">{{ detail.workstationName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="resolveStatusType(detail.status)">{{ resolveStatusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前处理人">
            {{ detail.currentAssigneeNames?.join('、') || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前签字格">
            {{ currentReviewSignatureCell }}
          </el-descriptions-item>
          <el-descriptions-item label="审核来源">
            {{ detail.reviewSourceName || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="当前节点">
            {{ detail.bpmTaskName || detail.taskName || '--' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-collapse class="edhr-detail__evidence">
          <el-collapse-item title="审批证据" name="approval-evidence">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="流程实例">{{ detail.processInstanceId || '--' }}</el-descriptions-item>
              <el-descriptions-item label="BPM 任务">{{ detail.bpmTaskId || '--' }}</el-descriptions-item>
              <el-descriptions-item label="审批快照">{{ detail.approvalSnapshotId || '--' }}</el-descriptions-item>
              <el-descriptions-item label="快照状态">
                {{ formatApprovalSnapshotStatus(detail.approvalSnapshotStatus) || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="审批快照哈希" :span="2">
                <span class="edhr-detail__hash">{{ detail.approvalSnapshotHash || '--' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>

        <div class="edhr-detail__action-bar">
          <el-button
            type="primary"
            :disabled="!canApprove"
            :loading="actionLoading"
            @click="openActionDialog('approve')"
          >
            {{ approveActionLabel }}
          </el-button>
          <el-button
            v-if="showRejectAction"
            type="danger"
            :disabled="!canReject"
            :loading="actionLoading"
            @click="openActionDialog('reject')"
          >
            审核驳回
          </el-button>
          <el-button
            v-if="showArchiveGenerateAction"
            v-hasPermi="['mes:pro-batch-record-execution-archive:create']"
            type="primary"
            :loading="archiveGenerateLoading"
            @click="openArchiveGenerateDialog"
          >
            生成归档
          </el-button>
          <span v-else class="edhr-detail__hint">{{ archiveGateText }}</span>
        </div>

        <el-tabs class="edhr-detail__tabs">
          <el-tab-pane label="执行内容" name="content">
            <ExecutionRenderer v-if="detailExecution && isReadonlyReviewForm" :execution="detailExecution" />
          </el-tab-pane>
          <el-tab-pane label="追踪" name="tracking">
            <el-table
              :data="trackingTimeline"
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无追踪事件"
            >
              <el-table-column type="expand" width="40">
                <template #default="{ row }">
                  <div class="edhr-detail__evidence-panel">
                    <div class="edhr-detail__evidence-title">追踪证据</div>
                    <div class="edhr-detail__evidence-grid">
                      <div class="edhr-detail__evidence-item">
                        <span>事件编号</span>
                        <strong>{{ row.eventId || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>流程实例</span>
                        <strong>{{ row.processInstanceId || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>BPM 任务</span>
                        <strong>{{ row.bpmTaskId || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>任务定义</span>
                        <strong>{{ row.taskDefinitionKey || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>签名编号</span>
                        <strong>{{ row.signatureId || '--' }}</strong>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="事件" width="130">
                <template #default="{ row }">{{ formatTrackingEvent(row.eventType) }}</template>
              </el-table-column>
              <el-table-column label="节点" prop="nodeName" min-width="140" />
              <el-table-column label="处理人" prop="actorName" width="140" />
              <el-table-column label="处理时间" width="190">
                <template #default="{ row }">{{ formatApprovalDetailTime(row.occurredAt) || '--' }}</template>
              </el-table-column>
              <el-table-column label="结果" width="100">
                <template #default="{ row }">{{ formatTrackingResult(row.result) || '--' }}</template>
              </el-table-column>
              <el-table-column label="意见/原因" min-width="220">
                <template #default="{ row }">{{ row.rejectReason || row.comment || '--' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="签名记录" name="signature">
            <el-table
              :data="signatureRows"
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无签名记录"
            >
              <el-table-column type="expand" width="40">
                <template #default="{ row }">
                  <div class="edhr-detail__evidence-panel">
                    <div class="edhr-detail__evidence-title">签名时间证据</div>
                    <div class="edhr-detail__evidence-grid">
                      <div class="edhr-detail__evidence-item">
                        <span>系统签名时间</span>
                        <strong>{{ formatApprovalDetailTime(row.signedAt) || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>选择签名时间</span>
                        <strong>{{ formatApprovalDetailTime(row.selectedSignedAt) || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>显示签名时间</span>
                        <strong>{{ formatApprovalDetailTime(row.signatureDisplayAt) || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>时间模式</span>
                        <strong>{{ formatSignatureTimeMode(row.signatureTimeMode) || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>时区</span>
                        <strong>{{ row.selectedTimeZone || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item">
                        <span>流程任务</span>
                        <strong>{{ row.bpmTaskName || row.bpmTaskId || '--' }}</strong>
                      </div>
                      <div class="edhr-detail__evidence-item edhr-detail__evidence-item--wide">
                        <span>时间审计哈希</span>
                        <strong class="edhr-detail__hash">{{ row.selectedTimeAuditHash || '--' }}</strong>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="签名编号" prop="id" width="90" />
              <el-table-column label="执行编号" prop="executionCode" min-width="155" />
              <el-table-column label="签名动作" width="120">
                <template #default="{ row }">{{ formatSignatureAction(row.actionType) }}</template>
              </el-table-column>
              <el-table-column label="签名含义" prop="meaningText" min-width="160" />
              <el-table-column label="签名人" min-width="130">
                <template #default="{ row }">
                  <div>{{ row.actorName || '--' }}</div>
                  <div v-if="row.actorNickname" class="edhr-detail__muted">{{ row.actorNickname }}</div>
                </template>
              </el-table-column>
              <el-table-column label="签名确认" width="105">
                <template #default="{ row }">
                  <div>{{ formatSignatureMode(row.signatureMode) }}</div>
                  <el-tag size="small" :type="row.passwordVerified ? 'success' : 'danger'">
                    {{ row.passwordVerified ? '通过' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="签名时间" width="190">
                <template #default="{ row }">
                  {{
                    formatApprovalDetailTime(
                      row.signatureDisplayAt || row.selectedSignedAt || row.signedAt
                    ) || '--'
                  }}
                </template>
              </el-table-column>
              <el-table-column label="意见/原因" min-width="220">
                <template #default="{ row }">{{ row.reason || row.comment || '--' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="归档状态" name="archive">
            <el-alert
              v-if="archiveError"
              :title="archiveError"
              type="error"
              :closable="false"
              show-icon
            />
            <el-descriptions v-else-if="latestArchive" :column="2" border>
              <el-descriptions-item label="归档状态">
                {{ latestArchive.archiveStatus || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="归档编号">
                {{ latestArchive.archiveCode || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="文件名">
                {{ latestArchive.fileName || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="封存时间">
                {{ latestArchive.sealedAt || '--' }}
              </el-descriptions-item>
            </el-descriptions>
            <el-empty v-else description="未归档" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <Dialog :title="actionDialogTitle" v-model="actionDialogVisible" width="520px">
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon class="edhr-detail__alert" />
      <el-form label-width="96px">
        <el-form-item label="当前密码" required>
          <el-input v-model="actionForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item v-if="actionMode === 'reject'" label="驳回原因" required>
          <el-input v-model="actionForm.rejectReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="actionCommentLabel">
          <el-input v-model="actionForm.comment" type="textarea" :rows="3" />
        </el-form-item>
        <el-divider content-position="left">签名显示时间</el-divider>
        <el-form-item label="签名时间">
          <el-date-picker
            v-model="actionSignatureTimeForm.selectedSignedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选择人工签名时间"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="签名时区">
          <el-input v-model="actionSignatureTimeForm.selectedTimeZone" placeholder="例如 Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="时间原因">
          <el-input
            v-model="actionSignatureTimeForm.selectedTimeReason"
            type="textarea"
            :rows="2"
            placeholder="选择人工签名时间时必须说明原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="submitAction">{{ actionConfirmLabel }}</el-button>
      </template>
    </Dialog>

    <Dialog title="生成归档" v-model="archiveDialogVisible" width="520px">
      <el-form label-width="96px">
        <el-form-item label="封存密码" required>
          <el-input v-model="archiveForm.sealPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="归档备注">
          <el-input v-model="archiveForm.comment" type="textarea" :rows="3" />
        </el-form-item>
        <el-divider content-position="left">签名显示时间</el-divider>
        <el-form-item label="签名时间">
          <el-date-picker
            v-model="archiveSignatureTimeForm.selectedSignedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选择人工签名时间"
            class="!w-1/1"
          />
        </el-form-item>
        <el-form-item label="签名时区">
          <el-input v-model="archiveSignatureTimeForm.selectedTimeZone" placeholder="例如 Asia/Shanghai" />
        </el-form-item>
        <el-form-item label="时间原因">
          <el-input
            v-model="archiveSignatureTimeForm.selectedTimeReason"
            type="textarea"
            :rows="2"
            placeholder="选择人工签名时间时必须说明原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="archiveDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="archiveGenerateLoading" @click="handleGenerateArchive">
          确 认 生 成
        </el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF,
  generateEdhrExecutionArchive,
  getLatestEdhrExecutionArchive,
  isEdhrExecutionArchiveNotExistsMessage,
  type ProFeedbackEdhrExecutionArchiveRespVO
} from '@/api/mes/pro/edhr/archive'
import {
  approveEdhrExecution,
  EDHR_APPROVAL_ACTION_RESULT_TYPE,
  EDHR_EXECUTION_STATUS,
  getEdhrApprovalDetail,
  rejectEdhrExecution,
  type EdhrApprovalDetailVO
} from '@/api/mes/pro/edhr/approval'
import { getEdhrExecutionSignaturePage, type EdhrSignatureSummaryVO } from '@/api/mes/pro/edhr/signatures'
import {
  getEdhrTrackingTimeline,
  type EdhrTrackingEventType,
  type EdhrTrackingEventVO
} from '@/api/mes/pro/edhr/tracking'
import { hasPermission } from '@/directives/permission/hasPermi'
import { formatDate } from '@/utils/formatTime'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import ExecutionRenderer from './ExecutionRenderer.vue'
import { buildSignatureTimePayload, createSignatureTimeForm, type EdhrSignatureTimeForm } from './signatureTime'

defineOptions({ name: 'MesProFeedbackEdhrApprovalDetail' })

const APPROVE_PERMISSION = 'mes:pro-batch-record-execution:approve'
const TRACKING_EVENT_LABELS: Record<EdhrTrackingEventType, string> = {
  CREATE: '创建记录',
  SAVE_DRAFT: '保存草稿',
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '提交审批',
  REVIEW_APPROVE: '审核签名',
  APPROVE: '最终批准',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}
const TRACKING_RESULT_LABELS: Record<NonNullable<EdhrTrackingEventVO['result']>, string> = {
  PASS: '通过',
  REJECT: '驳回',
  FAIL: '失败'
}
const SIGNATURE_ACTION_LABELS: Record<EdhrSignatureSummaryVO['actionType'], string> = {
  FIELD_CHANGE: '字段变更',
  FORM_REVIEW: '表单复核',
  SUBMIT: '提交审批',
  REVIEW_APPROVE: '审核签名',
  APPROVE: '最终批准',
  REJECT: '审批驳回',
  ARCHIVE_SEAL: '归档封存'
}
const SIGNATURE_MODE_LABELS: Record<EdhrSignatureSummaryVO['signatureMode'], string> = {
  PASSWORD: '密码签名'
}
const SIGNATURE_TIME_MODE_LABELS: Record<
  NonNullable<EdhrSignatureSummaryVO['signatureTimeMode']>,
  string
> = {
  SERVER_TIME: '服务端时间',
  USER_SELECTED: '手动选择时间'
}
const APPROVAL_SNAPSHOT_STATUS_LABELS: Record<
  NonNullable<EdhrApprovalDetailVO['approvalSnapshotStatus']>,
  string
> = {
  SUBMITTED: '已提交审批',
  APPROVED: '审批通过',
  REJECTED: '审批驳回'
}

const route = useRoute()
const router = useRouter()
const message = useMessage()

const loading = ref(false)
const actionLoading = ref(false)
const archiveGenerateLoading = ref(false)
const loadError = ref('')
const actionError = ref('')
const archiveError = ref('')
const detail = ref<EdhrApprovalDetailVO>()
const trackingTimeline = ref<EdhrTrackingEventVO[]>([])
const signatureRows = ref<EdhrSignatureSummaryVO[]>([])
const latestArchive = ref<ProFeedbackEdhrExecutionArchiveRespVO>()
const actionDialogVisible = ref(false)
const archiveDialogVisible = ref(false)
const actionMode = ref<'approve' | 'reject'>('approve')
const actionForm = reactive({ password: '', comment: '', rejectReason: '' })
const archiveForm = reactive({ sealPassword: '', comment: '' })
const actionSignatureTimeForm = reactive<EdhrSignatureTimeForm>(createSignatureTimeForm())
const archiveSignatureTimeForm = reactive<EdhrSignatureTimeForm>(createSignatureTimeForm())

const executionId = computed(() => parsePositiveRouteQueryId(route.query.id) || undefined)
const workTaskId = computed(() => parsePositiveRouteQueryId(route.query.workTaskId) || undefined)
const bpmTaskId = computed(() => (typeof route.query.bpmTaskId === 'string' ? route.query.bpmTaskId : undefined))
const allowedSignatureCellKey = computed(() => detail.value?.signatureCellKey || '')
const resolveApprovalTaskKind = () => {
  const taskType = detail.value?.taskType
  if (taskType === 'REVIEW') return 'REVIEW'
  if (taskType === 'APPROVE') return 'APPROVE'
  return undefined
}
const approvalTaskKind = computed(resolveApprovalTaskKind)
const approveActionLabel = computed(() => {
  if (approvalTaskKind.value === 'REVIEW') return '审核签名'
  if (approvalTaskKind.value === 'APPROVE') return '最终批准'
  return '审批签名'
})
const actionDialogTitle = computed(() => {
  if (actionMode.value === 'reject') return '驳回 eDHR 审核'
  if (approvalTaskKind.value === 'REVIEW') return '审核签名 / 复核完成'
  if (approvalTaskKind.value === 'APPROVE') return '最终批准 eDHR'
  return 'eDHR 审批签名'
})
const actionConfirmLabel = computed(() => {
  if (actionMode.value === 'reject') return '确认驳回'
  if (approvalTaskKind.value === 'REVIEW') return '确认审核签名'
  if (approvalTaskKind.value === 'APPROVE') return '确认最终批准'
  return '确认审批签名'
})
const actionCommentLabel = computed(() =>
  approvalTaskKind.value === 'REVIEW'
    ? '审核意见'
    : approvalTaskKind.value === 'APPROVE'
      ? '批准意见'
      : '审批意见'
)
const currentReviewSignatureCell = computed(() => {
  const current = detail.value
  if (!current?.signatureCellKey) return '--'
  if (typeof current.signatureRowIndex === 'number' && typeof current.signatureColumnIndex === 'number') {
    return `第 ${current.signatureRowIndex + 1} 行 / 第 ${current.signatureColumnIndex + 1} 列`
  }
  return current.signatureCellKey
})
const isReadonlyReviewForm = computed(() => true)
const detailExecution = computed(() =>
  detail.value ? { ...detail.value, id: detail.value.id } : undefined
)
const hasApprovePermission = computed(() => hasPermission([APPROVE_PERMISSION]))

const requiredDetailError = computed(() => {
  const current = detail.value
  if (!current) return ''
  if (
    !current.executionId ||
    !current.processInstanceId ||
    !current.approvalSnapshotId ||
    !current.approvalSnapshotHash
  ) {
    return '审批详情接口缺少必填字段，无法审批。'
  }
  if (current.status === EDHR_EXECUTION_STATUS.SUBMITTED && !current.bpmTaskId) {
    return '缺少 BPM 任务，无法处理。'
  }
  if (current.status === EDHR_EXECUTION_STATUS.SUBMITTED && !current.workTaskId) {
    return '审批详情接口缺少 eDHR 工作任务上下文，无法审批。'
  }
  if (current.status === EDHR_EXECUTION_STATUS.SUBMITTED && !current.taskType) {
    return '审批详情接口缺少 eDHR 工作任务类型，无法审批。'
  }
  return ''
})

const resolveActionDisabledReason = (mode: 'approve' | 'reject') => {
  if (!detail.value) return '审批详情未加载，无法处理。'
  if (!hasApprovePermission.value) return '当前账号没有 eDHR 审批权限。'
  if (detail.value.status !== EDHR_EXECUTION_STATUS.SUBMITTED) return '当前记录不是待审批状态。'
  if (!detail.value.bpmTaskId) return '缺少 BPM 任务，无法处理。'
  if (mode === 'approve' && detail.value.canApprove !== true) return '后端未授权当前账号通过该 eDHR。'
  if (mode === 'reject' && detail.value.canReject !== true) return '后端未授权当前账号驳回该 eDHR。'
  if (requiredDetailError.value) return requiredDetailError.value
  return ''
}

const canApprove = computed(() => !resolveActionDisabledReason('approve'))
const canReject = computed(() => !resolveActionDisabledReason('reject'))
const showRejectAction = computed(() => approvalTaskKind.value === 'REVIEW' && detail.value?.canReject === true)
const showArchiveGenerateAction = computed(
  () =>
    detail.value?.status === EDHR_EXECUTION_STATUS.APPROVED &&
    Boolean(detail.value?.closedAt) &&
    detail.value?.approvalSnapshotStatus === 'APPROVED' &&
    detail.value?.canGenerateArchive === true
)
const archiveGateText = computed(() => {
  if (detail.value?.status === EDHR_EXECUTION_STATUS.SUBMITTED) return '审批关闭后才可归档'
  if (detail.value?.status === EDHR_EXECUTION_STATUS.REJECTED) return '驳回记录不可归档'
  if (!showArchiveGenerateAction.value) return '只有审批关闭后的 eDHR 执行记录才允许归档'
  return ''
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const resolveStatusLabel = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return '待审批'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return '已驳回'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return '已关闭'
  return '草稿'
}

const resolveStatusType = (status?: number) => {
  if (status === EDHR_EXECUTION_STATUS.SUBMITTED) return 'warning'
  if (status === EDHR_EXECUTION_STATUS.REJECTED) return 'danger'
  if (status === EDHR_EXECUTION_STATUS.APPROVED) return 'success'
  return 'info'
}

const formatTrackingEvent = (eventType?: EdhrTrackingEventType) => {
  if (!eventType) return ''
  const label = TRACKING_EVENT_LABELS[eventType]
  if (!label) {
    throw new Error(`未知追踪事件：${String(eventType)}`)
  }
  return label
}

const formatTrackingResult = (result?: EdhrTrackingEventVO['result']) => {
  if (!result) return ''
  const label = TRACKING_RESULT_LABELS[result]
  if (!label) {
    throw new Error(`未知追踪结果：${String(result)}`)
  }
  return label
}

const formatSignatureAction = (actionType?: EdhrSignatureSummaryVO['actionType']) => {
  if (!actionType) return ''
  const label = SIGNATURE_ACTION_LABELS[actionType]
  if (!label) {
    throw new Error(`未知签名动作：${String(actionType)}`)
  }
  return label
}

const formatSignatureMode = (signatureMode?: EdhrSignatureSummaryVO['signatureMode']) => {
  if (!signatureMode) return ''
  const label = SIGNATURE_MODE_LABELS[signatureMode]
  if (!label) {
    throw new Error(`未知签名方式：${String(signatureMode)}`)
  }
  return label
}

const formatSignatureTimeMode = (signatureTimeMode?: EdhrSignatureSummaryVO['signatureTimeMode']) => {
  if (!signatureTimeMode) return ''
  const label = SIGNATURE_TIME_MODE_LABELS[signatureTimeMode]
  if (!label) {
    throw new Error(`未知签名时间模式：${String(signatureTimeMode)}`)
  }
  return label
}

const formatApprovalSnapshotStatus = (status?: EdhrApprovalDetailVO['approvalSnapshotStatus']) => {
  if (!status) return ''
  const label = APPROVAL_SNAPSHOT_STATUS_LABELS[status]
  if (!label) {
    throw new Error(`未知审批快照状态：${String(status)}`)
  }
  return label
}

const formatApprovalDetailTime = (value?: string | number | Date) => {
  if (!value) return ''
  const parsedDate =
    typeof value === 'number' || /^\d+$/.test(String(value))
      ? new Date(Number(value))
      : new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    throw new Error(`审批详情时间不可解析：${String(value)}`)
  }
  return formatDate(parsedDate, 'YYYY年M月D日 HH:mm:ss')
}

const resolveRejectSuccessMessage = (result: Awaited<ReturnType<typeof rejectEdhrExecution>>) => {
  if (!result.revisionExecutionId || !result.reworkTaskId) {
    throw new Error('审批驳回后后端未返回修订草稿或返工任务。')
  }
  return `已驳回并创建返工任务 ${result.reworkTaskId}，修订执行 ${result.revisionExecutionId}`
}

const resolveApproveSuccessMessage = (result: Awaited<ReturnType<typeof approveEdhrExecution>>) => {
  if (
    result.status === EDHR_EXECUTION_STATUS.SUBMITTED &&
    (
      result.resultType === EDHR_APPROVAL_ACTION_RESULT_TYPE.REVIEW_INTERMEDIATE ||
      result.resultType === EDHR_APPROVAL_ACTION_RESULT_TYPE.REVIEW_TO_APPROVE
    )
  ) {
    return '审核签名已完成，等待其他审核人或最终批准。'
  }
  if (
    result.status === EDHR_EXECUTION_STATUS.APPROVED &&
    result.resultType === EDHR_APPROVAL_ACTION_RESULT_TYPE.FINAL_APPROVED
  ) {
    return '最终批准完成，eDHR 已审批关闭。'
  }
  throw new Error('审批通过后后端未返回一致的动作结果和执行状态。')
}

const loadArchive = async () => {
  if (!detail.value?.id) return
  archiveError.value = ''
  try {
    latestArchive.value = await getLatestEdhrExecutionArchive(
      detail.value.id,
      EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF
    )
  } catch (error) {
    const errorMessage = resolveErrorMessage(error, '归档状态加载失败，请联系管理员。')
    latestArchive.value = undefined
    archiveError.value = isEdhrExecutionArchiveNotExistsMessage(errorMessage) ? '' : errorMessage
  }
}

const loadDetail = async () => {
  if (!executionId.value) {
    loadError.value = '缺少 eDHR 执行记录 ID，无法加载审批详情。'
    return
  }
  if (!workTaskId.value) {
    loadError.value = '缺少 eDHR 工作任务上下文，无法加载审批详情。'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    detail.value = await getEdhrApprovalDetail(executionId.value, workTaskId.value, bpmTaskId.value)
    if (!detail.value.executionSnapshotJson) throw new Error('eDHR 审批详情缺少 executionSnapshotJson。')
    if (detail.value.status === EDHR_EXECUTION_STATUS.SUBMITTED && !allowedSignatureCellKey.value) {
      throw new Error('eDHR 审批详情缺少当前工作任务绑定的签字格。')
    }
    if (requiredDetailError.value) actionError.value = requiredDetailError.value
    trackingTimeline.value = await getEdhrTrackingTimeline(detail.value.id)
    const signaturePage = await getEdhrExecutionSignaturePage({
      pageNo: 1,
      pageSize: 20,
      executionId: detail.value.id
    })
    signatureRows.value = signaturePage.list || detail.value.signatureSummaries || []
    await loadArchive()
  } catch (error) {
    detail.value = undefined
    trackingTimeline.value = []
    signatureRows.value = []
    latestArchive.value = undefined
    loadError.value = resolveErrorMessage(error, 'eDHR 审批详情加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const openActionDialog = (mode: 'approve' | 'reject') => {
  const disabledReason = resolveActionDisabledReason(mode)
  if (disabledReason) {
    actionError.value = disabledReason
    message.error(disabledReason)
    return
  }
  actionMode.value = mode
  actionError.value = ''
  actionForm.password = ''
  actionForm.comment = ''
  actionForm.rejectReason = ''
  Object.assign(actionSignatureTimeForm, createSignatureTimeForm())
  actionDialogVisible.value = true
}

const submitAction = async () => {
  const disabledReason = resolveActionDisabledReason(actionMode.value)
  if (!detail.value || disabledReason) {
    actionError.value = disabledReason || '审批详情接口缺少必填字段，无法审批。'
    return
  }
  if (!actionForm.password.trim()) {
    actionError.value = '当前账号密码不能为空。'
    return
  }
  if (actionMode.value === 'reject' && !actionForm.rejectReason.trim()) {
    actionError.value = '驳回原因不能为空。'
    return
  }
  if (!workTaskId.value) {
    actionError.value = '缺少 eDHR 工作任务上下文，不能审批。'
    return
  }
  actionLoading.value = true
  actionError.value = ''
  try {
    const payload = {
      executionId: detail.value.executionId,
      workTaskId: workTaskId.value,
      processInstanceId: detail.value.processInstanceId,
      approvalSnapshotId: detail.value.approvalSnapshotId!,
      approvalSnapshotHash: detail.value.approvalSnapshotHash!,
      bpmTaskId: detail.value.bpmTaskId!,
      password: actionForm.password.trim(),
      comment: actionForm.comment.trim() || undefined,
      signatureTime: buildSignatureTimePayload(actionSignatureTimeForm)
    }
    const result =
      actionMode.value === 'approve'
        ? await approveEdhrExecution(payload)
        : await rejectEdhrExecution({ ...payload, reason: actionForm.rejectReason.trim() })
    if (actionMode.value === 'reject' && result.status !== EDHR_EXECUTION_STATUS.REJECTED) {
      throw new Error('审批驳回后后端未返回 REJECTED 状态。')
    }
    const successText =
      actionMode.value === 'approve'
        ? resolveApproveSuccessMessage(result)
        : resolveRejectSuccessMessage(result)
    actionDialogVisible.value = false
    message.success(successText)
    await loadDetail()
  } catch (error) {
    actionError.value = resolveErrorMessage(error, 'eDHR 审批动作失败，请联系管理员。')
  } finally {
    actionLoading.value = false
  }
}

const openArchiveGenerateDialog = () => {
  if (!showArchiveGenerateAction.value) {
    message.error('只有审批关闭后的 eDHR 执行记录才允许归档。')
    return
  }
  archiveForm.sealPassword = ''
  archiveForm.comment = ''
  Object.assign(archiveSignatureTimeForm, createSignatureTimeForm())
  archiveDialogVisible.value = true
}

const handleGenerateArchive = async () => {
  if (!detail.value?.id || !showArchiveGenerateAction.value) {
    message.error('只有审批关闭后的 eDHR 执行记录才允许归档。')
    return
  }
  if (!archiveForm.sealPassword.trim()) {
    message.error('封存密码不能为空。')
    return
  }
  archiveGenerateLoading.value = true
  try {
    latestArchive.value = await generateEdhrExecutionArchive({
      executionId: detail.value.id,
      artifactType: EDHR_EXECUTION_ARCHIVE_ARTIFACT_PDF,
      sealPassword: archiveForm.sealPassword.trim(),
      comment: archiveForm.comment.trim() || undefined,
      signatureTime: buildSignatureTimePayload(archiveSignatureTimeForm)
    })
    archiveDialogVisible.value = false
    message.success('归档生成成功')
  } catch (error) {
    archiveError.value = resolveErrorMessage(error, '归档生成失败，请联系管理员。')
    message.error(archiveError.value)
  } finally {
    archiveGenerateLoading.value = false
  }
}

const goBack = async () => {
  await router.push({ path: '/mes/pro/feedback/edhr-approval', query: { tab: 'pending' } })
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.edhr-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-detail__header,
.edhr-detail__summary,
.edhr-detail__evidence,
.edhr-detail__action-bar,
.edhr-detail__tabs {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-detail__header,
.edhr-detail__action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.edhr-detail__title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-detail__subtitle,
.edhr-detail__hint,
.edhr-detail__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
}

.edhr-detail__actions {
  display: flex;
  gap: 12px;
}

.edhr-detail__alert {
  margin-bottom: 12px;
}

.edhr-detail__evidence-panel {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-detail__evidence-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-detail__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.edhr-detail__evidence-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-detail__evidence-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-detail__evidence-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-detail__evidence-item--wide {
  grid-column: 1 / -1;
}

.edhr-detail__hash {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  overflow-wrap: anywhere;
}
</style>
