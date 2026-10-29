<template>
  <ContentWrap>
    <div class="edhr-record-change">
      <el-form :inline="true" :model="queryParams" class="edhr-record-change__toolbar" @submit.prevent>
        <el-form-item label="变更类型">
          <el-select v-model="queryParams.changeType" clearable class="!w-150px">
            <el-option
              v-for="option in changeTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.changeStatus" clearable class="!w-150px">
            <el-option
              v-for="option in changeStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="queryParams.targetScope" clearable class="!w-150px">
            <el-option label="执行记录" value="EXECUTION" />
            <el-option label="批次" value="BATCH" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button v-hasPermi="['mes:pro-edhr-change:reopen', 'mes:pro-edhr-change:supplement', 'mes:pro-edhr-change:void']" @click="openRequest">申请变更</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-record-change__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无变更记录"
        >
          <el-table-column type="expand" width="40">
            <template #default="{ row }">
              <div class="edhr-record-change__evidence">
                <div class="edhr-record-change__evidence-title">变更证据</div>
                <div class="edhr-record-change__evidence-grid">
                  <div class="edhr-record-change__evidence-item">
                    <span>申请签名</span>
                    <strong>{{ row.requestSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>审批签名</span>
                    <strong>{{ row.approvalSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>原 Head Hash</span>
                    <strong>{{ row.previousHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>新 Head Hash</span>
                    <strong>{{ row.newHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>原归档 Hash</span>
                    <strong>{{ row.previousArchiveHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>新归档 Hash</span>
                    <strong>{{ row.newArchiveHash || '--' }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="变更编号" prop="changeCode" min-width="210" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="resolveChangeTypeTag(row.changeType)">
                {{ resolveChangeTypeLabel(row.changeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="resolveChangeStatusTag(row.changeStatus)">
                {{ resolveChangeStatusLabel(row.changeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象" min-width="190">
            <template #default="{ row }">
              <div>{{ resolveTargetScopeLabel(row.targetScope) }}</div>
              <div class="edhr-record-change__muted">
                批次ID：
                <el-button
                  v-if="canOpenBatchExecution(row)"
                  link
                  type="primary"
                  class="edhr-record-change__object-link"
                  @click="openBatchExecution(row)"
                >
                  {{ formatObjectId(row.batchExecutionId) }}
                </el-button>
                <span v-else class="edhr-record-change__object-link--disabled">--</span>
              </div>
              <div class="edhr-record-change__muted">
                执行ID：
                <el-button
                  v-if="canOpenExecution(row)"
                  link
                  type="primary"
                  class="edhr-record-change__object-link"
                  @click="openExecution(row)"
                >
                  {{ formatObjectId(row.executionId) }}
                </el-button>
                <span v-else class="edhr-record-change__object-link--disabled">--</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" min-width="160">
            <template #default="{ row }">
              <div>{{ resolveExecutionStatusLabel(row.previousStatus) }}</div>
              <div class="edhr-record-change__muted">到 {{ resolveExecutionStatusLabel(row.newStatus) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="240">
            <template #default="{ row }">
              <div>{{ row.reasonCategory || '--' }}</div>
              <div class="edhr-record-change__muted">{{ row.reasonText || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column
            label="申请时间"
            prop="requestedAt"
            width="180"
            :formatter="edhrDateTimeFormatter"
          />
          <el-table-column
            label="生效时间"
            prop="effectiveAt"
            width="180"
            :formatter="edhrDateTimeFormatter"
          />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="row.canResubmit" link type="primary" @click="openDecision(row, 'resubmit')">补正重提</el-button>
              <el-button v-if="row.canApprove" v-hasPermi="['mes:pro-edhr-change:approve']" link type="primary" @click="openDecision(row, 'approve')">审批</el-button>
              <el-button v-if="row.changeStatus === 'DRAFT' && String(row.requestedBy) === String(currentUserId)" v-hasPermi="['mes:pro-edhr-change:supplement']" link type="primary" @click="openDecision(row, 'submit')">提交草稿</el-button>
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

    <Dialog title="eDHR 变更详情" v-model="detailDialogVisible" width="760px">
      <div class="edhr-record-change__detail-summary">
        <div class="edhr-record-change__detail-summary-item">
          <span>变更编号</span>
          <strong>{{ selectedChange?.changeCode || '--' }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>变更对象</span>
          <strong>{{ resolveTargetScopeLabel(selectedChange?.targetScope) }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>状态</span>
          <strong>{{ resolveChangeStatusLabel(selectedChange?.changeStatus) }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>生效时间</span>
          <strong>{{ formatEdhrDateTime(selectedChange?.effectiveAt) }}</strong>
        </div>
      </div>

      <el-descriptions :column="2" border class="edhr-record-change__detail">
        <el-descriptions-item label="变更类型">{{ resolveChangeTypeLabel(selectedChange?.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="状态变化">
          {{ resolveExecutionStatusLabel(selectedChange?.previousStatus) }} -> {{
            resolveExecutionStatusLabel(selectedChange?.newStatus)
          }}
        </el-descriptions-item>
        <el-descriptions-item label="批次ID">
          <el-button
            v-if="canOpenBatchExecution(selectedChange)"
            link
            type="primary"
            class="edhr-record-change__object-link"
            @click="openBatchExecution(selectedChange)"
          >
            {{ formatObjectId(selectedChange?.batchExecutionId) }}
          </el-button>
          <span v-else class="edhr-record-change__object-link--disabled">--</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行ID">
          <el-button
            v-if="canOpenExecution(selectedChange)"
            link
            type="primary"
            class="edhr-record-change__object-link"
            @click="openExecution(selectedChange)"
          >
            {{ formatObjectId(selectedChange?.executionId) }}
          </el-button>
          <span v-else class="edhr-record-change__object-link--disabled">--</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="selectedChange?.newExecutionId" label="补录新修订">{{ selectedChange.newExecutionId }}</el-descriptions-item>
        <el-descriptions-item label="上一申请轮次">{{ selectedChange?.previousChangeEventId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="决定原因" :span="2">{{ selectedChange?.decisionReason || '--' }}</el-descriptions-item>
        <el-descriptions-item label="指定审批人">{{ selectedChange?.approverUserId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="审批待办">{{ selectedChange?.approvalWorkTaskId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原因分类">{{ selectedChange?.reasonCategory || '--' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">
          {{ formatEdhrDateTime(selectedChange?.requestedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="原因说明" :span="2">{{ selectedChange?.reasonText || '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-button v-if="selectedChange?.canResubmit" type="primary" @click="openDecision(selectedChange!, 'resubmit')">补正并重新提交</el-button>
      <el-button v-if="selectedChange?.canApprove" v-hasPermi="['mes:pro-edhr-change:approve']" type="primary" class="mt-12px" @click="openDecision(selectedChange!, 'approve')">审批此变更</el-button>
      <el-button v-if="selectedChange?.revisionActionUrl" type="primary" class="mt-12px" @click="router.push(selectedChange.revisionActionUrl)">办理获批修改任务</el-button>
      <el-collapse v-model="detailEvidenceNames" class="edhr-record-change__evidence-collapse">
        <el-collapse-item title="链路证据" name="chain-evidence">
          <div class="edhr-record-change__evidence-grid">
            <div class="edhr-record-change__evidence-item">
              <span>申请签名</span>
              <strong>{{ selectedChange?.requestSignatureId || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>审批签名</span>
              <strong>{{ selectedChange?.approvalSignatureId || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>原 Head Hash</span>
              <strong>{{ selectedChange?.previousHeadHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>新 Head Hash</span>
              <strong>{{ selectedChange?.newHeadHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>原归档 Hash</span>
              <strong>{{ selectedChange?.previousArchiveHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>新归档 Hash</span>
              <strong>{{ selectedChange?.newArchiveHash || '--' }}</strong>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </Dialog>
    <Dialog :title="actionMode === 'approve' ? '审批记录变更' : actionMode === 'resubmit' ? '补正并重新提交' : actionMode === 'submit' ? '提交补录草稿' : '申请记录变更'" v-model="actionVisible" width="560px">
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon class="mb-12px" />
      <el-form label-width="100px" @submit.prevent>
        <template v-if="actionMode === 'request'">
          <el-form-item label="变更类型" required>
            <el-select v-model="actionForm.changeType">
              <el-option label="重开" value="REOPEN" />
              <el-option label="补录" value="SUPPLEMENT" />
              <el-option label="单张记录作废" value="VOID" />
            </el-select>
          </el-form-item>
          <el-form-item label="变更对象" required>
            <el-radio-group v-model="actionForm.targetScope" :disabled="['SUPPLEMENT', 'VOID'].includes(actionForm.changeType)">
              <el-radio value="BATCH">批次</el-radio><el-radio value="EXECUTION">执行记录</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="批次 ID" required><el-input v-model="actionForm.batchExecutionId" placeholder="请输入真实批次 ID" /></el-form-item>
          <el-form-item v-if="actionForm.targetScope === 'EXECUTION'" label="执行记录 ID" required><el-input v-model="actionForm.executionId" placeholder="请输入真实执行记录 ID" /></el-form-item>
          <el-alert v-if="actionForm.changeType === 'VOID'" type="warning" :closable="false" show-icon class="mb-12px" :title="`本次仅申请作废批次 #${actionForm.batchExecutionId || '待填写'} 中的执行记录 #${actionForm.executionId || '待填写'}。批准后该记录及其归档失效。`" />
          <el-form-item label="原因分类" required><el-input v-model="actionForm.reasonCategory" /></el-form-item>
          <el-form-item label="原因说明" required><el-input v-model="actionForm.reasonText" type="textarea" :rows="3" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="变更编号">{{ actionRecord?.changeCode }}</el-form-item>
          <el-form-item label="变更对象">{{ resolveChangeTypeLabel(actionRecord?.changeType) }} · 批次 {{ formatObjectId(actionRecord?.batchExecutionId) }}<template v-if="actionRecord?.targetScope === 'EXECUTION'"> · 执行记录 {{ formatObjectId(actionRecord?.executionId) }}</template></el-form-item>
          <el-form-item label="原因说明">{{ actionRecord?.reasonText }}</el-form-item>
        </template>
        <template v-if="actionMode === 'resubmit'">
          <el-alert :title="actionRecord?.decisionReason" type="warning" :closable="false" class="mb-12px" />
          <el-form-item label="原因分类" required><el-input v-model="actionForm.reasonCategory" /></el-form-item>
          <el-form-item label="补正说明" required><el-input v-model="actionForm.reasonText" type="textarea" :rows="3" maxlength="500" /></el-form-item>
        </template>
        <template v-if="actionMode === 'approve'">
          <el-form-item label="审批决定" required><el-radio-group v-model="decision"><el-radio value="APPROVE">批准</el-radio><el-radio value="REJECTED">驳回终止</el-radio><el-radio value="RETURNED">退回补正</el-radio></el-radio-group></el-form-item>
          <el-form-item v-if="decision !== 'APPROVE'" label="决定原因" required><el-input v-model="decisionReason" type="textarea" maxlength="500" :rows="3" /></el-form-item>
        </template>
        <el-form-item v-if="actionMode !== 'approve'" label="审批人" required>
          <RecordChangeUserSelect v-model="actionForm.approverUserId" :load-user-options="getEdhrRecordChangeApprovers" exclude-current-user placeholder="请选择本次变更审批人" />
        </el-form-item>
        <el-form-item label="签名密码" required><el-input v-model="actionForm.password" type="password" show-password autocomplete="new-password" placeholder="请输入当前账号密码" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="actionForm.comment" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionVisible = false">取消</el-button>
        <el-button v-if="actionMode === 'request' && actionForm.changeType === 'SUPPLEMENT'" :loading="actionLoading" @click="performAction(true)">保存草稿</el-button>
        <el-button type="primary" :loading="actionLoading" @click="performAction(false)">{{ actionMode === 'approve' ? '签名确认决定' : '签名提交' }}</el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  edhrDateTimeFormatter,
  formatEdhrDateTime
} from '@/views/mes/pro/edhr/shared/dateTime'
import {
  EDHR_CHANGE_STATUS_APPROVED,
  EDHR_CHANGE_STATUS_DRAFT,
  EDHR_CHANGE_STATUS_EFFECTIVE,
  EDHR_CHANGE_STATUS_REJECTED,
  EDHR_CHANGE_STATUS_SUBMITTED,
  EDHR_CHANGE_TYPE_REOPEN,
  EDHR_CHANGE_TYPE_SUPPLEMENT,
  EDHR_CHANGE_TYPE_VOID,
  getEdhrRecordChange,
  getEdhrRecordChangePage,
  type EdhrRecordChangePageReqVO,
  type EdhrRecordChangeRespVO
} from '@/api/mes/pro/edhr/change'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { useUserStore } from '@/store/modules/user'
import RecordChangeUserSelect from '@/views/system/user/components/UserSelect.vue'
import { decideEdhrRecordChange, resubmitEdhrRecordChange, getEdhrRecordChangeApprovers, requestVoidExecution, approveVoidExecution, requestReopenBatch, requestReopenExecution, requestSupplement, saveSupplementDraft, submitSupplement, approveReopenBatch, approveReopenExecution, approveSupplement } from '@/api/mes/pro/edhr/change'

defineOptions({ name: 'MesProFeedbackEdhrRecordChange' })

const currentUserId = computed(() => useUserStore().getUser.id)
const actionVisible = ref(false)
const actionLoading = ref(false)
const actionError = ref('')
const decision = ref<'APPROVE' | 'REJECTED' | 'RETURNED'>('APPROVE')
const decisionReason = ref('')
const actionMode = ref<'request' | 'submit' | 'approve' | 'resubmit'>('request')
const actionRecord = ref<EdhrRecordChangeRespVO>()
const actionForm = reactive({
  changeType: 'REOPEN', targetScope: 'BATCH', batchExecutionId: undefined as string | undefined,
  executionId: undefined as string | undefined, approverUserId: undefined as string | number | undefined,
  reasonCategory: '', reasonText: '', password: '', comment: ''
})
watch(() => actionForm.changeType, type => { if (['SUPPLEMENT', 'VOID'].includes(type)) actionForm.targetScope = 'EXECUTION' })
const resetSigning = () => { actionForm.password = ''; actionForm.comment = ''; actionForm.approverUserId = undefined; actionError.value = '' }
const openRequest = () => {
  resetSigning()
  actionMode.value = 'request'
  actionForm.batchExecutionId = parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined
  actionForm.executionId = parsePositiveRouteQueryId(queryParams.executionId) || undefined
  actionForm.reasonCategory = ''; actionForm.reasonText = ''
  actionVisible.value = true
}
const openDecision = (row: EdhrRecordChangeRespVO, mode: 'submit' | 'approve' | 'resubmit') => {
  resetSigning(); decision.value = 'APPROVE'; decisionReason.value = '';
  actionForm.reasonCategory = row.reasonCategory || ''; actionForm.reasonText = row.reasonText || '';
  actionRecord.value = row; actionMode.value = mode; actionVisible.value = true
}
const performAction = async (draft: boolean) => {
  if (actionLoading.value) return
  if (!actionForm.password.trim()) { actionError.value = '请输入本人签名密码。'; return }
  if (!draft && actionMode.value !== 'approve' && !actionForm.approverUserId) { actionError.value = '请选择本次变更审批人。'; return }
  if (actionMode.value === 'request' && (!parsePositiveRouteQueryId(actionForm.batchExecutionId) || !actionForm.reasonCategory.trim() || !actionForm.reasonText.trim() || (actionForm.targetScope === 'EXECUTION' && !parsePositiveRouteQueryId(actionForm.executionId)))) {
    actionError.value = '请填写完整的变更对象与变更原因。'; return
  }
  if (actionMode.value === 'resubmit' && (!actionForm.reasonCategory.trim() || !actionForm.reasonText.trim())) { actionError.value = '请填写原因分类与补正说明。'; return }
  if (actionMode.value === 'approve' && decision.value !== 'APPROVE' && !decisionReason.value.trim()) { actionError.value = '请填写决定原因。'; return }
  actionLoading.value = true; actionError.value = ''
  try {
    if (actionMode.value === 'request') {
      const payload = { ...actionForm, approverUserId: actionForm.approverUserId ? actionForm.approverUserId : undefined,
        executionId: actionForm.targetScope === 'EXECUTION' ? actionForm.executionId : undefined }
      if (draft) await saveSupplementDraft(payload)
      else if (actionForm.changeType === 'VOID') await requestVoidExecution(payload)
      else if (actionForm.changeType === 'SUPPLEMENT') await requestSupplement(payload)
      else if (actionForm.targetScope === 'BATCH') await requestReopenBatch(payload)
      else await requestReopenExecution(payload)
    } else {
      if (!actionRecord.value?.id) throw new Error('变更记录身份缺失，请重新加载。')
      const payload = { changeEventId: actionRecord.value.id, password: actionForm.password, comment: actionForm.comment,
        approverUserId: actionForm.approverUserId ? actionForm.approverUserId : undefined }
      if (actionMode.value === 'resubmit') await resubmitEdhrRecordChange({ ...payload, reasonCategory: actionForm.reasonCategory, reasonText: actionForm.reasonText })
      else if (actionMode.value === 'approve' && decision.value !== 'APPROVE') await decideEdhrRecordChange({ changeEventId: actionRecord.value.id, password: actionForm.password, decision: decision.value, reason: decisionReason.value })
      else if (actionMode.value === 'submit') await submitSupplement(payload)
      else if (actionRecord.value.changeType === 'VOID' && actionRecord.value.targetScope === 'EXECUTION') await approveVoidExecution(payload)
      else if (actionRecord.value.changeType === 'SUPPLEMENT') await approveSupplement(payload)
      else if (actionRecord.value.targetScope === 'BATCH') await approveReopenBatch(payload)
      else await approveReopenExecution(payload)
    }
    actionVisible.value = false; actionForm.password = ''
    message.success(draft ? '补录草稿已保存' : actionMode.value === 'approve' ? (decision.value === 'APPROVE' ? '变更已批准，原申请人已收到结果通知' : decision.value === 'RETURNED' ? '已退回，原申请人已收到补正待办与通知' : '已驳回终止，原申请人已收到结果通知') : '申请已提交，指定审批人已收到待办及通知')
    await getList()
    if (detailDialogVisible.value && selectedChange.value?.id) await openDetail(selectedChange.value)
  } catch (error) { actionError.value = resolveErrorMessage(error, '记录变更操作失败。') }
  finally { actionLoading.value = false }
}

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrRecordChangeRespVO[]>([])
const total = ref(0)
const detailDialogVisible = ref(false)
const selectedChange = ref<EdhrRecordChangeRespVO>()
const detailEvidenceNames = ref<string[]>([])

const queryParams = reactive<EdhrRecordChangePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  changeType: typeof route.query.changeType === 'string' ? route.query.changeType : undefined,
  targetScope: typeof route.query.targetScope === 'string' ? route.query.targetScope : undefined,
  batchExecutionId: parsePositiveRouteQueryId(route.query.batchExecutionId) || undefined,
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  changeStatus: typeof route.query.changeStatus === 'string' ? route.query.changeStatus : undefined
})

const changeTypeOptions = [
  { label: '作废', value: EDHR_CHANGE_TYPE_VOID },
  { label: '重开', value: EDHR_CHANGE_TYPE_REOPEN },
  { label: '补录', value: EDHR_CHANGE_TYPE_SUPPLEMENT }
]

const changeStatusOptions = [
  { label: '草稿', value: EDHR_CHANGE_STATUS_DRAFT },
  { label: '已提交', value: EDHR_CHANGE_STATUS_SUBMITTED },
  { label: '已批准', value: EDHR_CHANGE_STATUS_APPROVED },
  { label: '已拒绝', value: EDHR_CHANGE_STATUS_REJECTED },
  { label: '待补正', value: 'RETURNED' },
  { label: '已补正重提', value: 'RESUBMITTED' },
  { label: '已生效', value: EDHR_CHANGE_STATUS_EFFECTIVE }
]

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const resolveChangeTypeLabel = (type?: string) => {
  const labels: Record<string, string> = {
    [EDHR_CHANGE_TYPE_VOID]: '作废',
    [EDHR_CHANGE_TYPE_REOPEN]: '重开',
    [EDHR_CHANGE_TYPE_SUPPLEMENT]: '补录'
  }
  return type ? labels[type] || type : '--'
}

const resolveChangeTypeTag = (type?: string) => {
  if (type === EDHR_CHANGE_TYPE_VOID) return 'danger'
  if (type === EDHR_CHANGE_TYPE_REOPEN) return 'warning'
  if (type === EDHR_CHANGE_TYPE_SUPPLEMENT) return 'primary'
  return 'info'
}

const resolveChangeStatusLabel = (status?: string) => {
  const labels: Record<string, string> = {
    [EDHR_CHANGE_STATUS_DRAFT]: '草稿',
    [EDHR_CHANGE_STATUS_SUBMITTED]: '已提交',
    [EDHR_CHANGE_STATUS_APPROVED]: '已批准',
    [EDHR_CHANGE_STATUS_REJECTED]: '已拒绝',
    RETURNED: '待补正', RESUBMITTED: '已补正重提',
    [EDHR_CHANGE_STATUS_EFFECTIVE]: '已生效'
  }
  return status ? labels[status] || status : '--'
}

const resolveChangeStatusTag = (status?: string) => {
  if (status === EDHR_CHANGE_STATUS_EFFECTIVE) return 'success'
  if (status === EDHR_CHANGE_STATUS_REJECTED) return 'danger'
  if (status === EDHR_CHANGE_STATUS_SUBMITTED || status === EDHR_CHANGE_STATUS_APPROVED) return 'warning'
  return 'info'
}

const resolveTargetScopeLabel = (scope?: string) => {
  if (scope === 'BATCH') return '批次'
  if (scope === 'EXECUTION') return '执行记录'
  return scope || '--'
}

const resolveExecutionStatusLabel = (status?: string) => {
  if (!status) return '--'
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    SUBMITTED: '待审批',
    APPROVED: '已关闭',
    REJECTED: '已驳回',
    VOIDED: '已作废',
    REOPENED: '已重开'
  }
  return labels[status] || status
}

const isPositiveId = (value?: string | number) => Number.isFinite(Number(value)) && Number(value) > 0

const formatObjectId = (value?: string | number) => (isPositiveId(value) ? `#${value}` : '--')

const canOpenBatchExecution = (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) =>
  isPositiveId(row?.batchExecutionId)

const canOpenExecution = (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => isPositiveId(row?.executionId)

const normalizeQueryParams = () => {
  queryParams.executionId = parsePositiveRouteQueryId(queryParams.executionId) || undefined
  queryParams.batchExecutionId = parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined
}

const getList = async () => {
  normalizeQueryParams()
  loading.value = true
  loadError.value = ''
  try {
    const pageResult = (await getEdhrRecordChangePage(queryParams)) as {
      list?: EdhrRecordChangeRespVO[]
      total?: number
    }
    list.value = pageResult.list ?? []
    total.value = pageResult.total ?? 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 变更记录加载失败。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const openDetail = async (row: EdhrRecordChangeRespVO) => {
  if (!row.id) {
    message.error('当前变更记录缺少ID，无法查看详情。')
    return
  }
  try {
    selectedChange.value = await getEdhrRecordChange(row.id)
    detailDialogVisible.value = true
    detailEvidenceNames.value = []
  } catch (error) {
    message.error(resolveErrorMessage(error, 'eDHR 变更详情加载失败。'))
  }
}

const openBatchExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) => {
  if (!canOpenBatchExecution(row)) {
    message.error('当前变更记录缺少批次执行 ID，无法打开批次详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/detail',
    query: { id: String(row!.batchExecutionId) }
  })
}

const openExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => {
  if (!canOpenExecution(row)) {
    message.error('当前变更记录缺少执行 ID，无法打开执行表单。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row!.executionId) }
  })
}

watch(() => route.query.changeEventId, async (value) => {
  const eventId = parsePositiveRouteQueryId(value)
  if (eventId) await openDetail({ id: eventId })
})

onMounted(async () => {
  await getList()
  const eventId = parsePositiveRouteQueryId(route.query.changeEventId)
  if (eventId) await openDetail({ id: eventId })
})
</script>

<style scoped>
.edhr-record-change {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-record-change__toolbar,
.edhr-record-change__table {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-record-change__toolbar {
  margin-bottom: 0;
  padding: 12px 12px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-record-change__table {
  padding: 0 12px 12px;
  border-radius: 0 0 8px 8px;
}

.edhr-record-change__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-record-change__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-record-change__evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-record-change__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-record-change__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-record-change__evidence-item,
.edhr-record-change__detail-summary-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-record-change__evidence-item span,
.edhr-record-change__detail-summary-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-record-change__evidence-item strong,
.edhr-record-change__detail-summary-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-record-change__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-record-change__object-link {
  height: auto;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  vertical-align: baseline;
}

.edhr-record-change__object-link--disabled {
  color: #8a94a6;
}

.edhr-record-change__detail-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.edhr-record-change__evidence-collapse {
  margin-top: 12px;
}

.edhr-record-change__detail {
  margin-top: 4px;
}
</style>
