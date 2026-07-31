<template>
  <ContentWrap>
    <div class="edhr-work-task-page">
      <div class="edhr-work-task-page__summary">
        <div class="edhr-work-task-page__metric">
          <span>待处理</span>
          <strong>{{ stats.todoCount }}</strong>
        </div>
        <div class="edhr-work-task-page__metric">
          <span>待填写</span>
          <strong>{{ stats.fillCount }}</strong>
        </div>
        <div class="edhr-work-task-page__metric">
          <span>待审核</span>
          <strong>{{ stats.reviewCount }}</strong>
        </div>
        <div class="edhr-work-task-page__metric">
          <span>驳回待改</span>
          <strong>{{ stats.reworkCount }}</strong>
        </div>
        <div class="edhr-work-task-page__metric">
          <span>待归档</span>
          <strong>{{ stats.archiveCount }}</strong>
        </div>
        <div class="edhr-work-task-page__metric">
          <span>逾期</span>
          <strong>{{ stats.overdueCount }}</strong>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="edhr-work-task-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="我的待办" name="todo" />
        <el-tab-pane label="候选审核" name="candidate" />
        <el-tab-pane label="逾期任务" name="overdue" />
        <el-tab-pane label="我已处理" name="done" />
      </el-tabs>

      <el-form :inline="true" :model="queryParams" class="edhr-work-task-page__toolbar">
        <el-form-item label="任务类型">
          <el-select v-model="queryParams.taskType" clearable class="!w-150px">
            <el-option label="填写" :value="EDHR_WORK_TASK_TYPE_FILL" />
            <el-option label="审核" :value="EDHR_WORK_TASK_TYPE_REVIEW" />
            <el-option label="批准" :value="EDHR_WORK_TASK_TYPE_APPROVE" />
            <el-option label="驳回修改" :value="EDHR_WORK_TASK_TYPE_REWORK" />
            <el-option label="最终归档" :value="EDHR_WORK_TASK_TYPE_ARCHIVE" />
            <el-option label="最终放行审批" :value="EDHR_WORK_TASK_TYPE_RELEASE_APPROVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="工单">
          <el-input v-model="queryParams.workOrderCode" clearable class="!w-170px" />
        </el-form-item>
        <el-form-item label="批次">
          <el-input v-model="queryParams.batchCode" clearable class="!w-150px" />
        </el-form-item>
        <el-form-item label="工序">
          <el-input v-model="queryParams.processName" clearable class="!w-150px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button v-hasPermi="['mes:pro-edhr-work-task-rule:update']" @click="openCloseRuleDialog">
            关闭规则
          </el-button>
          <el-button v-hasPermi="['mes:pro-edhr-work-task-rule:update']" @click="openArchiveRuleDialog">
            归档规则
          </el-button>
          <el-button v-hasPermi="['mes:pro-edhr-work-task-rule:update']" @click="openReleaseApprovalRuleDialog">
            放行规则
          </el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无工作任务"
      >
        <el-table-column type="expand" width="40">
          <template #default="{ row }">
            <div class="edhr-work-task-page__evidence">
              <div class="edhr-work-task-page__evidence-title">派工证据</div>
              <div class="edhr-work-task-page__evidence-grid">
                <div class="edhr-work-task-page__evidence-item">
                  <span>工作任务</span>
                  <strong>{{ row.taskCode || row.id || '--' }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>执行记录</span>
                  <strong>{{ row.executionId || '--' }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>审核签字格</span>
                  <strong>{{ resolveReviewSignatureCellLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>审核来源</span>
                  <strong>{{ row.reviewSourceName || '--' }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>候选来源</span>
                  <strong>{{ resolveCandidateSourceLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>责任来源</span>
                  <strong>{{ resolveResponsibilitySourceLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>候选池名称</span>
                  <strong>{{ resolveCandidatePoolNameLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>候选快照</span>
                  <strong>{{ resolveCandidateSnapshotLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>当前用户不可操作原因</span>
                  <strong>{{ resolveInactionReasonLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>返工来源</span>
                  <strong>{{ resolveReworkSourceLabel(row) }}</strong>
                </div>
                <div class="edhr-work-task-page__evidence-item">
                  <span>BPM 任务</span>
                  <strong>{{ row.bpmTaskId || '--' }}</strong>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务" min-width="210">
          <template #default="{ row }">
            <div class="edhr-work-task-page__task-title">
              <el-tag :type="resolveTaskTypeTag(row.taskType)">{{ resolveTaskTypeLabel(row.taskType) }}</el-tag>
              <span>{{ row.taskCode || row.id }}</span>
            </div>
            <div class="edhr-work-task-page__muted">{{ resolveStatusLabel(row.status) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="生产上下文" min-width="260">
          <template #default="{ row }">
            <div class="edhr-work-task-page__strong">{{ row.workOrderCode || '--' }}</div>
            <div class="edhr-work-task-page__muted">批次：{{ row.batchCode || '--' }}</div>
            <div class="edhr-work-task-page__muted">工序：{{ row.processName || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="处理提示" min-width="260">
          <template #default="{ row }">
            <div class="edhr-work-task-page__strong">{{ resolveTaskPrompt(row) }}</div>
            <div class="edhr-work-task-page__muted">{{ resolveResponsibilitySourceLabel(row) }}</div>
            <div class="edhr-work-task-page__muted">{{ row.reason || row.remark || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间状态" min-width="220">
          <template #default="{ row }">
            <div>到期时间：{{ resolveTaskTimeSummary(row) }}</div>
            <div class="edhr-work-task-page__muted">创建：{{ formatEdhrDateTime(row.createTime) }}</div>
            <div class="edhr-work-task-page__muted">完成：{{ formatEdhrDateTime(row.completedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canCompleteCandidateSignature(row)"
              link
              type="success"
              @click="handleCompleteCandidateSignature(row)"
            >
              完成签名
            </el-button>
            <el-button v-else link type="primary" :disabled="row.status === EDHR_WORK_TASK_STATUS_DONE" @click="openTask(row)">
              处理
            </el-button>
            <div class="edhr-work-task-page__muted">{{ resolveInactionReasonLabel(row) }}</div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />

      <Dialog v-model="archiveRuleDialogVisible" :title="ruleDialogTitle" width="560px">
        <el-form
          ref="archiveRuleFormRef"
          v-loading="archiveRuleLoading"
          :model="archiveRuleForm"
          :rules="archiveRuleRules"
          label-width="110px"
        >
          <el-form-item label="工艺路线" prop="routeId">
            <el-select
              v-model="archiveRuleForm.routeId"
              class="!w-100%"
              filterable
              @change="loadArchiveRuleByRoute"
            >
              <el-option
                v-for="route in routeOptions"
                :key="route.id"
                :label="`${route.code} ${route.name}`"
                :value="route.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="ruleAssigneeLabel" prop="assigneeUserId">
            <el-select v-model="archiveRuleForm.assigneeUserId" class="!w-100%" filterable>
              <el-option
                v-for="user in userOptions"
                :key="user.id"
                :label="formatUserOptionLabel(user)"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="实际派发源">
            <div class="edhr-work-task-page__dispatch-source">
              <el-tag :type="isArchiveRuleDispatchAligned ? 'success' : 'warning'">
                {{ resolveArchiveRuleDispatchSourceLabel() }}
              </el-tag>
              <span class="edhr-work-task-page__muted">{{ resolveArchiveRuleDispatchHint() }}</span>
            </div>
          </el-form-item>
          <el-form-item label="处理时限" prop="dueMinutes">
            <el-input-number
              v-model="archiveRuleForm.dueMinutes"
              class="!w-100%"
              :min="1"
              :precision="0"
              controls-position="right"
            />
          </el-form-item>
          <el-form-item label="启用" prop="enabled">
            <el-switch v-model="archiveRuleForm.enabled" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="archiveRuleForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="archiveRuleSaving" @click="archiveRuleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="archiveRuleSaving" @click="submitArchiveRule">保存</el-button>
        </template>
      </Dialog>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_WORK_TASK_STATUS_DONE,
  EDHR_WORK_TASK_STATUS_OVERDUE,
  EDHR_WORK_TASK_STATUS_TODO,
  EDHR_WORK_TASK_TYPE_FILL,
  EDHR_WORK_TASK_TYPE_APPROVE,
  EDHR_WORK_TASK_TYPE_ARCHIVE,
  EDHR_WORK_TASK_TYPE_RELEASE_APPROVE,
  EDHR_WORK_TASK_TYPE_REVIEW,
  EDHR_WORK_TASK_TYPE_REWORK,
  completeEdhrCandidateSignatureTask,
  getEdhrWorkTaskCandidateTodoPage,
  getEdhrRouteArchiveRule,
  getEdhrRouteCloseRule,
  getEdhrRouteReleaseApprovalRule,
  getEdhrWorkTaskDonePage,
  getEdhrWorkTaskMyPage,
  getEdhrWorkTaskStats,
  saveEdhrRouteArchiveRule,
  saveEdhrRouteCloseRule,
  saveEdhrRouteReleaseApprovalRule,
  type EdhrWorkTaskArchiveRuleReqVO,
  type EdhrWorkTaskCloseRuleReqVO,
  type EdhrWorkTaskReleaseApprovalRuleReqVO,
  type EdhrWorkTaskAssignmentRuleRespVO,
  type EdhrWorkTaskRespVO,
  type EdhrWorkTaskStatsRespVO
} from '@/api/mes/pro/edhr/workTask'
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'
import type { FormInstance, FormRules } from 'element-plus'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrWorkTaskBoardPage' })

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const activeTab = ref('todo')
const list = ref<EdhrWorkTaskRespVO[]>([])
const total = ref(0)
const archiveRuleDialogVisible = ref(false)
const archiveRuleLoading = ref(false)
const archiveRuleSaving = ref(false)
const ruleDialogMode = ref<'CLOSE' | 'ARCHIVE' | 'RELEASE_APPROVE'>('ARCHIVE')
const archiveRuleFormRef = ref<FormInstance>()
const routeOptions = ref<ProRouteVO[]>([])
const userOptions = ref<UserVO[]>([])
const currentArchiveRule = ref<EdhrWorkTaskAssignmentRuleRespVO | null>(null)
const stats = reactive<EdhrWorkTaskStatsRespVO>({
  todoCount: 0,
  fillCount: 0,
  reviewCount: 0,
  reworkCount: 0,
  archiveCount: 0,
  overdueCount: 0,
  doneCount: 0
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskType: '',
  workOrderCode: '',
  batchCode: '',
  processName: ''
})
const archiveRuleForm = reactive<
  Partial<
    EdhrWorkTaskArchiveRuleReqVO &
      EdhrWorkTaskCloseRuleReqVO &
      EdhrWorkTaskReleaseApprovalRuleReqVO
  >
>({
  routeId: undefined,
  assigneeUserId: undefined,
  dueMinutes: undefined,
  enabled: true,
  remark: ''
})
const archiveRuleRules: FormRules<
  Partial<
    EdhrWorkTaskArchiveRuleReqVO &
      EdhrWorkTaskCloseRuleReqVO &
      EdhrWorkTaskReleaseApprovalRuleReqVO
  >
> = {
  routeId: [{ required: true, message: '请选择工艺路线', trigger: 'change' }],
  assigneeUserId: [{ required: true, message: '请选择责任人', trigger: 'change' }],
  dueMinutes: [{ required: true, message: '请输入处理时限', trigger: 'blur' }],
  enabled: [{ required: true, message: '请选择启用状态', trigger: 'change' }]
}

const ruleDialogTitle = computed(() => {
  if (ruleDialogMode.value === 'CLOSE') return '关闭规则'
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '放行规则'
  return '归档规则'
})
const ruleAssigneeLabel = computed(() => {
  if (ruleDialogMode.value === 'CLOSE') return '关闭责任人'
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '放行审批责任人'
  return '归档责任人'
})
const ruleActionLabel = computed(() => {
  if (ruleDialogMode.value === 'CLOSE') return '批次关闭'
  if (ruleDialogMode.value === 'RELEASE_APPROVE') return '最终放行审批'
  return '最终归档'
})

const isArchiveRuleDispatchAligned = computed(() => {
  if (!archiveRuleForm.assigneeUserId || !currentArchiveRule.value?.candidateSourceId) return true
  return Number(archiveRuleForm.assigneeUserId) === Number(currentArchiveRule.value.candidateSourceId)
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  taskType: queryParams.taskType || undefined,
  workOrderCode: queryParams.workOrderCode.trim() || undefined,
  batchCode: queryParams.batchCode.trim() || undefined,
  processName: queryParams.processName.trim() || undefined
})

const resolveMyPageStatus = () =>
  activeTab.value === 'overdue' ? EDHR_WORK_TASK_STATUS_OVERDUE : EDHR_WORK_TASK_STATUS_TODO

const resolveTaskTypeLabel = (taskType: string) => {
  const labels: Record<string, string> = {
    [EDHR_WORK_TASK_TYPE_FILL]: '填写',
    [EDHR_WORK_TASK_TYPE_REVIEW]: 'REVIEW 审核',
    [EDHR_WORK_TASK_TYPE_APPROVE]: 'APPROVE 批准',
    [EDHR_WORK_TASK_TYPE_REWORK]: 'REWORK 修改',
    [EDHR_WORK_TASK_TYPE_ARCHIVE]: '最终归档',
    [EDHR_WORK_TASK_TYPE_RELEASE_APPROVE]: '最终放行审批'
  }
  return labels[taskType] || taskType
}

const resolveTaskTypeTag = (taskType: string) => {
  if (taskType === EDHR_WORK_TASK_TYPE_REVIEW) return 'warning'
  if (taskType === EDHR_WORK_TASK_TYPE_APPROVE) return 'warning'
  if (taskType === EDHR_WORK_TASK_TYPE_REWORK) return 'danger'
  if (taskType === EDHR_WORK_TASK_TYPE_ARCHIVE) return 'success'
  if (taskType === EDHR_WORK_TASK_TYPE_RELEASE_APPROVE) return 'success'
  return 'primary'
}

const resolveStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    TODO: '待处理',
    DOING: '处理中',
    DONE: '已完成',
    CANCELED: '已取消',
    OVERDUE: '已逾期'
  }
  return labels[status] || status
}

const resolveReviewSignatureCellLabel = (row: EdhrWorkTaskRespVO) => {
  if (row.taskType !== EDHR_WORK_TASK_TYPE_REVIEW) return '--'
  if (typeof row.signatureRowIndex === 'number' && typeof row.signatureColumnIndex === 'number') {
    return `第 ${row.signatureRowIndex + 1} 行 / 第 ${row.signatureColumnIndex + 1} 列`
  }
  return row.signatureCellKey || '--'
}

const resolveCandidateSourceLabel = (row: EdhrWorkTaskRespVO) => {
  if (!row.candidateSourceType) return '--'
  const labels: Record<string, string> = {
    USER: '用户',
    USER_GROUP: '用户组',
    ROLE_GROUP: '角色组',
    DEPT_GROUP: '部门组'
  }
  const label = labels[row.candidateSourceType] || row.candidateSourceType
  return row.candidateSourceId ? `${label} ${row.candidateSourceId}` : label
}

const resolveCandidateSnapshotLabel = (row: EdhrWorkTaskRespVO) => {
  if (row.candidateSnapshotDisplay?.trim()) return row.candidateSnapshotDisplay
  if (!row.candidateUserSnapshot?.trim()) return '--'
  return row.candidateUserSnapshot
}

const resolveResponsibilitySourceLabel = (row: EdhrWorkTaskRespVO) =>
  row.responsibilitySource || row.remark || '--'

const resolveCandidatePoolNameLabel = (row: EdhrWorkTaskRespVO) =>
  row.candidatePoolName || resolveCandidateSourceLabel(row)

const resolveInactionReasonLabel = (row: EdhrWorkTaskRespVO) =>
  row.inactionReason || '当前用户可按既有入口处理'

const resolveReworkSourceLabel = (row: EdhrWorkTaskRespVO) => {
  if (row.taskType !== EDHR_WORK_TASK_TYPE_REWORK) return '--'
  return row.sourceExecutionId ? `来源执行 ${row.sourceExecutionId}` : '--'
}

const resolveTaskPrompt = (row: EdhrWorkTaskRespVO) => {
  if (row.taskType === EDHR_WORK_TASK_TYPE_RELEASE_APPROVE) return '审批最终放行并完成电子签名'
  if (row.taskType === EDHR_WORK_TASK_TYPE_ARCHIVE) return '生成并封存最终批记录'
  if (row.taskType === EDHR_WORK_TASK_TYPE_REWORK) return '按驳回意见修订后重新提交'
  if (row.taskType === EDHR_WORK_TASK_TYPE_APPROVE) return '批准填写内容并完成审批签名'
  if (row.taskType === EDHR_WORK_TASK_TYPE_REVIEW) return '复核填写内容并完成审批签名'
  return '填写批记录字段并提交'
}

const resolveTaskTimeSummary = (row: EdhrWorkTaskRespVO) => {
  if (row.overdueAt) return `逾期时间：${formatEdhrDateTime(row.overdueAt)}`
  if (row.dueTime) return `到期时间：${formatEdhrDateTime(row.dueTime)}`
  return row.status === EDHR_WORK_TASK_STATUS_DONE ? '已完成' : '无明确时限'
}

const canCompleteCandidateSignature = (row: EdhrWorkTaskRespVO) =>
  activeTab.value === 'candidate' &&
  row.taskType === EDHR_WORK_TASK_TYPE_REVIEW &&
  row.status !== EDHR_WORK_TASK_STATUS_DONE &&
  Boolean(row.executionId)

const isFillWorkspaceTask = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_FILL || row.taskType === EDHR_WORK_TASK_TYPE_REWORK

const resolveTaskActionUrl = (row: EdhrWorkTaskRespVO) => {
  if (!row.actionUrl) {
    throw new Error('工作任务缺少处理入口，无法打开。')
  }
  const url = new URL(row.actionUrl, window.location.origin)
  if (row.taskType === EDHR_WORK_TASK_TYPE_REWORK) {
    if (!row.executionId || !row.sourceExecutionId) {
      throw new Error('返工任务缺少修订草稿或来源执行记录，无法处理。')
    }
    if (row.executionId === row.sourceExecutionId) {
      throw new Error('返工任务指向被驳回原版本，无法处理。')
    }
    const targetExecutionId = Number(url.searchParams.get('id') || url.searchParams.get('executionId'))
    if (targetExecutionId !== row.executionId) {
      throw new Error('返工任务处理入口未指向修订草稿，无法处理。')
    }
  }
  return url
}

const refreshStats = async () => {
  const data = await getEdhrWorkTaskStats()
  Object.assign(stats, data)
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data =
      activeTab.value === 'done'
        ? await getEdhrWorkTaskDonePage(buildQuery())
        : activeTab.value === 'candidate'
          ? await getEdhrWorkTaskCandidateTodoPage({
              ...buildQuery(),
              taskType: EDHR_WORK_TASK_TYPE_REVIEW,
              status: resolveMyPageStatus()
            })
        : await getEdhrWorkTaskMyPage({
            ...buildQuery(),
            status: resolveMyPageStatus()
          })
    list.value = data.list || []
    total.value = data.total || 0
    await refreshStats()
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 工作任务加载失败。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.taskType = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.processName = ''
  getList()
}

const handleTabChange = () => {
  queryParams.pageNo = 1
  getList()
}

const resetArchiveRuleForm = () => {
  currentArchiveRule.value = null
  archiveRuleForm.routeId = undefined
  archiveRuleForm.assigneeUserId = undefined
  archiveRuleForm.dueMinutes = undefined
  archiveRuleForm.enabled = true
  archiveRuleForm.remark = ''
  archiveRuleFormRef.value?.clearValidate()
}

const fillArchiveRuleForm = (
  rule: EdhrWorkTaskAssignmentRuleRespVO | null,
  routeId: number
) => {
  currentArchiveRule.value = rule
  archiveRuleForm.routeId = routeId
  archiveRuleForm.assigneeUserId =
    ruleDialogMode.value === 'RELEASE_APPROVE' && rule?.candidateSourceType === 'USER'
      ? rule.candidateSourceId
      : rule?.assigneeUserId
  archiveRuleForm.dueMinutes = rule?.dueMinutes
  archiveRuleForm.enabled = typeof rule?.enabled === 'boolean' ? rule.enabled : true
  archiveRuleForm.remark = rule?.remark || ''
}

const resolveArchiveRuleDispatchSourceLabel = () => {
  const sourceType = currentArchiveRule.value?.candidateSourceType
  const sourceId = currentArchiveRule.value?.candidateSourceId
  if (!sourceType && !sourceId) return '保存后由责任人生成派发源'
  const labels: Record<string, string> = {
    USER: '用户',
    USER_GROUP: '用户组',
    ROLE_GROUP: '角色组',
    DEPT_GROUP: '部门组'
  }
  return `${labels[sourceType || ''] || sourceType || '未知来源'} ${sourceId || '--'}`
}

const resolveArchiveRuleDispatchHint = () => {
  if (!currentArchiveRule.value?.candidateSourceId) {
    return `新规则保存后将写入真实责任源，${ruleActionLabel.value}按该字段校验。`
  }
  return isArchiveRuleDispatchAligned.value
    ? `责任人与实际责任源一致，${ruleActionLabel.value}会由该责任人处理。`
    : '责任人与实际责任源不一致，请保存规则后重新加载确认。'
}

const formatUserOptionLabel = (user: UserVO) => {
  if (user.nickname && user.username) return `${user.nickname}（${user.username}）`
  return user.nickname || user.username || String(user.id)
}

const openArchiveRuleDialog = async () => {
  ruleDialogMode.value = 'ARCHIVE'
  await openRuleDialog()
}

const openCloseRuleDialog = async () => {
  ruleDialogMode.value = 'CLOSE'
  await openRuleDialog()
}

const openReleaseApprovalRuleDialog = async () => {
  ruleDialogMode.value = 'RELEASE_APPROVE'
  await openRuleDialog()
}

const openRuleDialog = async () => {
  resetArchiveRuleForm()
  archiveRuleDialogVisible.value = true
  archiveRuleLoading.value = true
  try {
    const [routes, users] = await Promise.all([ProRouteApi.getRouteSimpleList(), getSimpleUserList()])
    routeOptions.value = routes || []
    userOptions.value = users || []
  } catch (error) {
    archiveRuleDialogVisible.value = false
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}基础数据加载失败。`))
  } finally {
    archiveRuleLoading.value = false
  }
}

const loadArchiveRuleByRoute = async () => {
  if (!archiveRuleForm.routeId) return
  archiveRuleLoading.value = true
  try {
    const rule =
      ruleDialogMode.value === 'CLOSE'
        ? await getEdhrRouteCloseRule(archiveRuleForm.routeId)
        : ruleDialogMode.value === 'RELEASE_APPROVE'
          ? await getEdhrRouteReleaseApprovalRule(archiveRuleForm.routeId)
        : await getEdhrRouteArchiveRule(archiveRuleForm.routeId)
    fillArchiveRuleForm(rule || null, archiveRuleForm.routeId)
  } catch (error) {
    currentArchiveRule.value = null
    archiveRuleForm.assigneeUserId = undefined
    archiveRuleForm.dueMinutes = undefined
    archiveRuleForm.remark = ''
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}加载失败。`))
  } finally {
    archiveRuleLoading.value = false
  }
}

const submitArchiveRule = async () => {
  await archiveRuleFormRef.value?.validate()
  if (
    !archiveRuleForm.routeId ||
    !archiveRuleForm.assigneeUserId ||
    !archiveRuleForm.dueMinutes ||
    typeof archiveRuleForm.enabled !== 'boolean'
  ) {
    throw new Error(`${ruleDialogTitle.value}表单未填写完整。`)
  }
  archiveRuleSaving.value = true
  try {
    const payload: EdhrWorkTaskArchiveRuleReqVO & EdhrWorkTaskCloseRuleReqVO = {
      routeId: archiveRuleForm.routeId,
      assigneeUserId: archiveRuleForm.assigneeUserId,
      dueMinutes: archiveRuleForm.dueMinutes,
      enabled: archiveRuleForm.enabled,
      remark: archiveRuleForm.remark?.trim() || undefined
    }
    const savedRule =
      ruleDialogMode.value === 'CLOSE'
        ? await saveEdhrRouteCloseRule(payload)
        : ruleDialogMode.value === 'RELEASE_APPROVE'
          ? await saveEdhrRouteReleaseApprovalRule({
              routeId: archiveRuleForm.routeId,
              candidateSourceType: 'USER',
              candidateSourceId: archiveRuleForm.assigneeUserId,
              enabled: archiveRuleForm.enabled,
              remark: archiveRuleForm.remark?.trim() || undefined
            })
        : await saveEdhrRouteArchiveRule(payload)
    fillArchiveRuleForm(savedRule, archiveRuleForm.routeId)
    if (!isArchiveRuleDispatchAligned.value) {
      throw new Error(`${ruleDialogTitle.value}已保存，但责任人与实际责任源不一致，请重新检查路线配置。`)
    }
    message.success(`${ruleDialogTitle.value}已保存，责任人与实际责任源一致`)
    archiveRuleDialogVisible.value = false
  } catch (error) {
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}保存失败。`))
  } finally {
    archiveRuleSaving.value = false
  }
}

const openTask = async (row: EdhrWorkTaskRespVO) => {
  try {
    if (isFillWorkspaceTask(row)) {
      await navigateToEdhrWorkTask(router, row)
      return
    }
    const url = resolveTaskActionUrl(row)
    url.searchParams.set('workTaskId', String(row.id))
    await router.push(`${url.pathname}${url.search}`)
  } catch (error) {
    message.error(resolveErrorMessage(error, '工作任务入口打开失败。'))
  }
}

const handleCompleteCandidateSignature = async (row: EdhrWorkTaskRespVO) => {
  if (!row.executionId) {
    message.error('候选审核任务缺少执行记录，无法完成签名。')
    return
  }
  try {
    await message.confirm(`确认完成「${row.processName || row.taskCode || row.id}」的候选审核签名？`)
    await completeEdhrCandidateSignatureTask(row.id, row.executionId)
    message.success('签名完成')
    await getList()
  } catch (error) {
    if (error === 'cancel') return
    message.error(resolveErrorMessage(error, '候选审核签名失败。'))
  }
}

onMounted(getList)
</script>

<style scoped>
.edhr-work-task-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-work-task-page__summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.edhr-work-task-page__metric {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.edhr-work-task-page__metric span {
  display: block;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.edhr-work-task-page__metric strong {
  color: #172033;
  font-size: 24px;
  line-height: 32px;
}

.edhr-work-task-page__dispatch-source {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.edhr-work-task-page__tabs {
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 0 16px;
}

.edhr-work-task-page__toolbar {
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-bottom: 0;
  background: #ffffff;
  padding: 16px 16px 0;
}

.edhr-work-task-page__task-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edhr-work-task-page__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-work-task-page__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-work-task-page__evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-work-task-page__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-work-task-page__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 10px;
}

.edhr-work-task-page__evidence-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-work-task-page__evidence-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-work-task-page__evidence-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

@media (max-width: 960px) {
  .edhr-work-task-page__summary {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
