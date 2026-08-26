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
            <el-option label="PQC生产放行" :value="EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE" />
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
          <el-button
            v-hasPermi="['mes:pro-edhr-work-task-rule:update']"
            @click="openCloseRuleDialog"
          >
            关闭规则
          </el-button>
          <el-button
            v-hasPermi="['mes:pro-edhr-work-task-rule:update']"
            @click="openArchiveRuleDialog"
          >
            归档规则
          </el-button>
          <el-button
            v-hasPermi="['mes:pro-edhr-work-task-rule:update']"
            @click="openReleaseApprovalRuleDialog"
          >
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
              <el-tag :type="resolveTaskTypeTag(row.taskType)">{{
                resolveTaskTypeLabel(row.taskType)
              }}</el-tag>
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
            <div class="edhr-work-task-page__muted">{{
              resolveResponsibilitySourceLabel(row)
            }}</div>
            <div class="edhr-work-task-page__muted">{{ row.reason || row.remark || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="时间状态" min-width="220">
          <template #default="{ row }">
            <div>到期时间：{{ resolveTaskTimeSummary(row) }}</div>
            <div class="edhr-work-task-page__muted"
              >创建：{{ formatEdhrDateTime(row.createTime) }}</div
            >
            <div class="edhr-work-task-page__muted"
              >完成：{{ formatEdhrDateTime(row.completedAt) }}</div
            >
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <div v-if="isPqcProductionReleaseTask(row)">
              <template v-if="canHandlePqcProductionRelease(row)">
                <el-button
                  v-hasPermi="['mes:pro-production-release:pqc-approve']"
                  link
                  type="success"
                  :data-pqc-release-approve="row.id"
                  :disabled="isPqcDecisionLocked(row)"
                  @click="openPqcDecisionDialog(row, 'APPROVE')"
                >
                  PQC通过
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-production-release:pqc-approve']"
                  link
                  type="danger"
                  :data-pqc-release-reject="row.id"
                  :disabled="isPqcDecisionLocked(row)"
                  @click="openPqcDecisionDialog(row, 'REJECT')"
                >
                  PQC拒绝
                </el-button>
              </template>
            </div>
            <div v-else-if="isManagerReleaseTask(row)">
              <el-button
                v-if="canHandleManagerRelease(row)"
                v-hasPermi="['mes:pro-edhr-release:approve']"
                link
                type="success"
                :data-manager-release-approve="row.id"
                :disabled="isManagerReleaseLocked(row)"
                @click="openManagerReleaseDialog(row)"
              >
                最终放行
              </el-button>
            </div>
            <div v-else-if="isProductionReleaseReportTask(row)">
              <el-button
                link
                type="primary"
                :data-production-release-report-open="row.id"
                :disabled="!canHandleProductionReleaseReport(row)"
                @click="openProductionReleaseReportTask(row)"
              >
                上传报告
              </el-button>
            </div>
            <template v-else>
              <el-button
                v-if="canCompleteCandidateSignature(row)"
                link
                type="success"
                @click="handleCompleteCandidateSignature(row)"
              >
                完成签名
              </el-button>
              <el-button
                v-else
                link
                type="primary"
                :disabled="row.status === EDHR_WORK_TASK_STATUS_DONE"
                @click="openTask(row)"
              >
                处理
              </el-button>
            </template>
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

      <Dialog
        v-model="pqcDecisionDialogVisible"
        :title="pqcDecisionDialogTitle"
        width="720px"
        @closed="resetPqcDecisionDialog"
      >
        <div v-loading="pqcDecisionLoading" class="edhr-work-task-page__pqc-dialog">
          <el-alert
            v-if="pqcDecisionUncertainMessage"
            :title="pqcDecisionUncertainMessage"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-if="pqcDecisionBlockers.length" class="edhr-work-task-page__pqc-blockers">
            <el-alert
              v-for="blocker in pqcDecisionBlockers"
              :key="`${blocker.blockerType}-${blocker.objectType}-${blocker.objectId || blocker.objectCode || ''}`"
              :title="`${blocker.blockerType}：${blocker.reason}`"
              :description="blocker.suggestion"
              type="error"
              :closable="false"
              show-icon
            />
          </div>

          <el-descriptions v-if="pqcDecisionTask" :column="2" border>
            <el-descriptions-item label="申请编号">
              {{ pqcDecisionTask.businessScopeId }}
            </el-descriptions-item>
            <el-descriptions-item label="PQC待办">
              {{ pqcDecisionTask.id }}
            </el-descriptions-item>
            <el-descriptions-item label="版本">
              {{ pqcDecisionReceipt?.version || pqcDecisionTask.version }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ resolvePqcReleaseStatusLabel(pqcDecisionReceipt?.status) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-form
            v-if="!pqcDecisionCompleted"
            ref="pqcDecisionFormRef"
            :model="pqcDecisionForm"
            :rules="pqcDecisionRules"
            label-width="96px"
          >
            <el-form-item
              v-if="pqcDecisionAction === 'APPROVE'"
              label="审批意见"
              prop="approvalOpinion"
            >
              <el-input
                v-model="pqcDecisionForm.approvalOpinion"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item v-else label="拒绝原因" prop="rejectReason">
              <el-input
                v-model="pqcDecisionForm.rejectReason"
                type="textarea"
                :rows="4"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>

          <template v-if="pqcDecisionReceipt?.status === 'REPORT_UPLOAD_PENDING'">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="生产批次">
                {{ pqcDecisionReceipt.batchExecutionId }}
              </el-descriptions-item>
            </el-descriptions>
            <el-table :data="pqcDecisionReceipt.reportUploadTasks" size="small" border>
              <el-table-column label="报告任务" min-width="170">
                <template #default="{ row }">{{
                  resolveReportNodeTypeLabel(row.nodeType)
                }}</template>
              </el-table-column>
              <el-table-column prop="workTaskId" label="工作待办" min-width="150" />
              <el-table-column prop="status" label="状态" width="110" />
            </el-table>
          </template>
          <el-alert
            v-else-if="pqcDecisionReceipt?.status === 'PQC_RELEASE_REJECTED'"
            :title="pqcDecisionReceipt.rejectReason || 'PQC已拒绝'"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>
        <template #footer>
          <el-button :disabled="pqcDecisionSubmitting" @click="pqcDecisionDialogVisible = false">
            {{ pqcDecisionCompleted ? '关闭' : '取消' }}
          </el-button>
          <el-button
            v-if="!pqcDecisionCompleted"
            :type="pqcDecisionAction === 'APPROVE' ? 'success' : 'danger'"
            :loading="pqcDecisionSubmitting"
            :disabled="
              pqcDecisionLoading ||
              Boolean(pqcDecisionUncertainMessage) ||
              Boolean(pqcDecisionBlockers.length)
            "
            @click="submitPqcProductionReleaseDecision"
          >
            {{ pqcDecisionAction === 'APPROVE' ? '确认通过' : '确认拒绝' }}
          </el-button>
        </template>
      </Dialog>

      <Dialog
        v-model="managerReleaseDialogVisible"
        title="管理者代表最终放行"
        width="720px"
        @closed="resetManagerReleaseDialog"
      >
        <div v-loading="managerReleaseLoading" class="edhr-work-task-page__pqc-dialog">
          <el-alert
            v-if="managerReleaseUncertainMessage"
            :title="managerReleaseUncertainMessage"
            type="error"
            :closable="false"
            show-icon
          />
          <div v-if="managerReleaseBlockers.length" class="edhr-work-task-page__pqc-blockers">
            <el-alert
              v-for="blocker in managerReleaseBlockers"
              :key="`${blocker.blockerType}-${blocker.objectType}-${blocker.objectId || blocker.objectCode || ''}`"
              :title="`${blocker.blockerType}：${blocker.reason}`"
              :description="blocker.suggestion"
              type="error"
              :closable="false"
              show-icon
            />
          </div>

          <el-descriptions v-if="managerReleaseTask" :column="2" border>
            <el-descriptions-item label="放行事务">
              {{ managerReleaseTask.businessScopeId }}
            </el-descriptions-item>
            <el-descriptions-item label="管理者待办">
              {{ managerReleaseTask.id }}
            </el-descriptions-item>
            <el-descriptions-item label="版本">
              {{ managerReleaseReceipt?.version || managerReleaseTask.version }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              {{ resolveManagerReleaseStatusLabel(managerReleaseReceipt?.releaseStatus) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-form
            v-if="!managerReleaseCompleted"
            ref="managerReleaseFormRef"
            :model="managerReleaseForm"
            :rules="managerReleaseRules"
            label-width="112px"
          >
            <el-form-item label="签核证据" prop="signoffEvidenceHash">
              <el-input
                v-model="managerReleaseForm.signoffEvidenceHash"
                maxlength="128"
                placeholder="请输入正式电子签名证据哈希"
              />
            </el-form-item>
            <el-form-item label="审批意见" prop="approvalOpinion">
              <el-input
                v-model="managerReleaseForm.approvalOpinion"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
          </el-form>
          <el-alert
            v-else
            title="该放行事务已正式放行。"
            type="success"
            :closable="false"
            show-icon
          />
          <el-alert
            v-if="managerReleaseSnapshot"
            title="Stage5正式放行追溯回执已生成"
            :description="`回执 ${String(managerReleaseSnapshot.releaseReceiptId)}，三类文件证据已固化。`"
            type="success"
            :closable="false"
            show-icon
          />
        </div>
        <template #footer>
          <el-button
            :disabled="managerReleaseSubmitting"
            @click="managerReleaseDialogVisible = false"
          >
            {{ managerReleaseCompleted ? '关闭' : '取消' }}
          </el-button>
          <el-button
            v-if="!managerReleaseCompleted"
            type="success"
            :loading="managerReleaseSubmitting"
            :disabled="
              managerReleaseLoading ||
              Boolean(managerReleaseUncertainMessage) ||
              Boolean(managerReleaseBlockers.length)
            "
            @click="submitManagerReleaseApproval"
          >
            确认最终放行
          </el-button>
        </template>
      </Dialog>

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
            <el-input
              v-model="archiveRuleForm.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button :disabled="archiveRuleSaving" @click="archiveRuleDialogVisible = false"
            >取消</el-button
          >
          <el-button type="primary" :loading="archiveRuleSaving" @click="submitArchiveRule"
            >保存</el-button
          >
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
  EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE,
  EDHR_WORK_TASK_TYPE_RELEASE_APPROVE,
  EDHR_PRODUCTION_RELEASE_REPORT_NODE_TYPES,
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
import {
  approvePqcProductionRelease,
  getPqcProductionRelease,
  rejectPqcProductionRelease,
  type MesPqcProductionReleaseDecisionRespVO,
  type MesProductionReleaseBlockerRespVO,
  type MesProductionReleaseFailureRespVO
} from '@/api/mes/pro/productionRelease'
import {
  approveEdhrRelease,
  getEdhrRelease,
  type EdhrReleaseRowVO
} from '@/api/mes/pro/edhr/release'
import { getEdhrStage5ReleaseSnapshot } from '@/api/mes/pro/edhr/batchExecution'
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'
import type { FormInstance, FormRules } from 'element-plus'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrWorkTaskBoardPage' })

const router = useRouter()
const route = useRoute()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const activeTab = ref('todo')
const list = ref<EdhrWorkTaskRespVO[]>([])
const total = ref(0)
const pqcDecisionDialogVisible = ref(false)
const pqcDecisionLoading = ref(false)
const pqcDecisionSubmitting = ref(false)
const pqcDecisionAction = ref<'APPROVE' | 'REJECT'>('APPROVE')
const pqcDecisionTask = ref<EdhrWorkTaskRespVO | null>(null)
const pqcDecisionReceipt = ref<MesPqcProductionReleaseDecisionRespVO | null>(null)
const pqcDecisionBlockers = ref<MesProductionReleaseBlockerRespVO[]>([])
const pqcDecisionUncertainMessage = ref('')
const pqcDecisionIdempotencyKey = ref('')
const pqcDecisionLockedApplicationIds = reactive(new Set<string>())
const pqcDecisionFormRef = ref<FormInstance>()
const managerReleaseDialogVisible = ref(false)
const managerReleaseLoading = ref(false)
const managerReleaseSubmitting = ref(false)
const managerReleaseTask = ref<EdhrWorkTaskRespVO | null>(null)
const managerReleaseReceipt = ref<EdhrReleaseRowVO | null>(null)
const managerReleaseSnapshot = ref<Record<string, unknown> | null>(null)
const managerReleaseBlockers = ref<MesProductionReleaseBlockerRespVO[]>([])
const managerReleaseUncertainMessage = ref('')
const managerReleaseIdempotencyKey = ref('')
const managerReleaseLockedTransactionIds = reactive(new Set<string>())
const managerReleaseFormRef = ref<FormInstance>()
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
  batchExecutionId: '',
  workOrderCode: '',
  batchCode: '',
  processName: ''
})
const pqcDecisionForm = reactive({
  approvalOpinion: '',
  rejectReason: ''
})
const pqcDecisionRules: FormRules<typeof pqcDecisionForm> = {
  rejectReason: [
    { required: true, message: '请输入拒绝原因', trigger: 'blur' },
    { max: 500, message: '拒绝原因不能超过500个字符', trigger: 'blur' }
  ],
  approvalOpinion: [{ max: 500, message: '审批意见不能超过500个字符', trigger: 'blur' }]
}
const managerReleaseForm = reactive({
  signoffEvidenceHash: '',
  approvalOpinion: ''
})
const STAGE5_SIMULATION_SIGNOFF_STORAGE_KEY = 'mes:stage5-final-release:signoff-evidence-hash'
const managerReleaseRules: FormRules<typeof managerReleaseForm> = {
  signoffEvidenceHash: [
    { required: true, message: '请输入正式电子签名证据哈希', trigger: 'blur' },
    { max: 128, message: '签核证据哈希不能超过128个字符', trigger: 'blur' }
  ],
  approvalOpinion: [{ max: 500, message: '审批意见不能超过500个字符', trigger: 'blur' }]
}
const archiveRuleForm = reactive<
  Partial<
    EdhrWorkTaskArchiveRuleReqVO & EdhrWorkTaskCloseRuleReqVO & EdhrWorkTaskReleaseApprovalRuleReqVO
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
    EdhrWorkTaskArchiveRuleReqVO & EdhrWorkTaskCloseRuleReqVO & EdhrWorkTaskReleaseApprovalRuleReqVO
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

const pqcDecisionDialogTitle = computed(() =>
  pqcDecisionAction.value === 'APPROVE' ? 'PQC生产放行通过' : 'PQC生产放行拒绝'
)
const pqcDecisionCompleted = computed(() => {
  const status = pqcDecisionReceipt.value?.status
  return Boolean(status && status !== 'PQC_RELEASE_PENDING')
})
const managerReleaseCompleted = computed(
  () => managerReleaseReceipt.value?.releaseStatus === 'RELEASED'
)

const isArchiveRuleDispatchAligned = computed(() => {
  if (!archiveRuleForm.assigneeUserId || !currentArchiveRule.value?.candidateSourceId) return true
  return (
    Number(archiveRuleForm.assigneeUserId) === Number(currentArchiveRule.value.candidateSourceId)
  )
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const requireJsonLongId = (value: unknown, label: string) => {
  const normalized = typeof value === 'string' ? value.trim() : ''
  if (!/^[1-9]\d*$/.test(normalized)) {
    throw new Error(`${label}缺失或格式无效。`)
  }
  return normalized
}

const requirePositiveVersion = (value: unknown, label: string) => {
  if (!Number.isInteger(value) || Number(value) < 1) {
    throw new Error(`${label}缺失或格式无效。`)
  }
  return Number(value)
}

const createPqcDecisionIdempotencyKey = (action: 'APPROVE' | 'REJECT', applicationId: string) => {
  if (!globalThis.crypto || typeof globalThis.crypto.randomUUID !== 'function') {
    throw new Error('当前浏览器不支持安全请求标识，无法提交PQC决定。')
  }
  return `pqc-${action.toLowerCase()}-${applicationId}-${globalThis.crypto.randomUUID()}`
}

const createManagerReleaseIdempotencyKey = (releaseTransactionId: string) => {
  if (!globalThis.crypto || typeof globalThis.crypto.randomUUID !== 'function') {
    throw new Error('当前浏览器不支持安全请求标识，无法提交最终放行。')
  }
  return `manager-release-${releaseTransactionId}-${globalThis.crypto.randomUUID()}`
}

const resolvePqcProductionReleaseFailure = (
  error: unknown
): MesProductionReleaseFailureRespVO | undefined => {
  const candidate = (error as any)?.details ?? (error as any)?.response?.data?.data
  if (!candidate || typeof candidate !== 'object' || Array.isArray(candidate)) return undefined
  if (!Array.isArray(candidate.blockers) || candidate.blockers.length === 0) return undefined
  const blockersAreComplete = candidate.blockers.every((blocker: unknown) => {
    if (!blocker || typeof blocker !== 'object' || Array.isArray(blocker)) return false
    const record = blocker as Record<string, unknown>
    return (
      typeof record.blockerType === 'string' &&
      record.blockerType.trim().length > 0 &&
      typeof record.objectType === 'string' &&
      record.objectType.trim().length > 0 &&
      typeof record.reason === 'string' &&
      record.reason.trim().length > 0 &&
      typeof record.suggestion === 'string' &&
      record.suggestion.trim().length > 0
    )
  })
  return blockersAreComplete ? (candidate as MesProductionReleaseFailureRespVO) : undefined
}

const resolvePqcReleaseStatusLabel = (status?: string) => {
  const labels: Record<string, string> = {
    PQC_RELEASE_PENDING: '待PQC放行',
    PQC_RELEASE_REJECTED: 'PQC已拒绝',
    REPORT_UPLOAD_PENDING: '待上传放行报告',
    MANAGER_RELEASE_PENDING: '待管理者代表放行',
    RELEASED: '已放行'
  }
  return status ? labels[status] || status : '--'
}

const resolveReportNodeTypeLabel = (nodeType: string) => {
  const labels: Record<string, string> = {
    INCOMING_INSPECTION_REPORT: '来料检验报告',
    STERILIZATION_REPORT: '灭菌报告',
    FINISHED_PRODUCT_INSPECTION_REPORT: '成品检验报告',
    FINISHED_PRODUCT_INSPECTION_RECORD: '成品检验记录'
  }
  return labels[nodeType] || nodeType
}

const isPqcProductionReleaseTask = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE

const hasPqcDecisionTaskContext = (row: EdhrWorkTaskRespVO) =>
  row.businessScopeType === 'RELEASE_APPLICATION' &&
  /^[1-9]\d*$/.test(row.businessScopeId || '') &&
  /^[1-9]\d*$/.test(row.id || '') &&
  Number.isInteger(row.version) &&
  Number(row.version) > 0

const canHandlePqcProductionRelease = (row: EdhrWorkTaskRespVO) =>
  activeTab.value === 'candidate' &&
  isPqcProductionReleaseTask(row) &&
  row.status === EDHR_WORK_TASK_STATUS_TODO &&
  hasPqcDecisionTaskContext(row)

const isManagerReleaseTask = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_RELEASE_APPROVE &&
  row.businessScopeType === 'RELEASE_TRANSACTION'

const hasManagerReleaseTaskContext = (row: EdhrWorkTaskRespVO) =>
  /^[1-9]\d*$/.test(row.businessScopeId || '') &&
  /^[1-9]\d*$/.test(row.id || '') &&
  Number.isInteger(row.version) &&
  Number(row.version) > 0

const canHandleManagerRelease = (row: EdhrWorkTaskRespVO) =>
  activeTab.value === 'candidate' &&
  isManagerReleaseTask(row) &&
  row.status === EDHR_WORK_TASK_STATUS_TODO &&
  hasManagerReleaseTaskContext(row)

const productionReleaseReportNodeTypeSet = new Set<string>(
  EDHR_PRODUCTION_RELEASE_REPORT_NODE_TYPES
)

const isProductionReleaseReportTask = (row: EdhrWorkTaskRespVO) =>
  row.taskType === EDHR_WORK_TASK_TYPE_FILL &&
  row.businessScopeType === 'RELEASE_REPORT_NODE' &&
  Boolean(row.nodeType && productionReleaseReportNodeTypeSet.has(row.nodeType))

const hasProductionReleaseReportTaskContext = (row: EdhrWorkTaskRespVO) =>
  Boolean(
    row.batchExecutionId &&
      row.batchTaskId &&
      row.nodeType &&
      Number.isInteger(row.version) &&
      Number(row.version) > 0
  )

const canHandleProductionReleaseReport = (row: EdhrWorkTaskRespVO) =>
  activeTab.value === 'candidate' &&
  isProductionReleaseReportTask(row) &&
  row.status === EDHR_WORK_TASK_STATUS_TODO &&
  hasProductionReleaseReportTaskContext(row)

const isPqcDecisionLocked = (row: EdhrWorkTaskRespVO) =>
  Boolean(row.businessScopeId && pqcDecisionLockedApplicationIds.has(row.businessScopeId))

const isManagerReleaseLocked = (row: EdhrWorkTaskRespVO) =>
  Boolean(
    row.businessScopeId && managerReleaseLockedTransactionIds.has(row.businessScopeId)
  )

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  taskType: queryParams.taskType || undefined,
  batchExecutionId: queryParams.batchExecutionId || undefined,
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
    [EDHR_WORK_TASK_TYPE_RELEASE_APPROVE]: '最终放行审批',
    [EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE]: 'PQC生产放行'
  }
  return labels[taskType] || taskType
}

const resolveTaskTypeTag = (taskType: string) => {
  if (taskType === EDHR_WORK_TASK_TYPE_REVIEW) return 'warning'
  if (taskType === EDHR_WORK_TASK_TYPE_APPROVE) return 'warning'
  if (taskType === EDHR_WORK_TASK_TYPE_REWORK) return 'danger'
  if (taskType === EDHR_WORK_TASK_TYPE_ARCHIVE) return 'success'
  if (taskType === EDHR_WORK_TASK_TYPE_RELEASE_APPROVE) return 'success'
  if (taskType === EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE) return 'warning'
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

const resolveInactionReasonLabel = (row: EdhrWorkTaskRespVO) => {
  if (isManagerReleaseTask(row)) {
    if (isManagerReleaseLocked(row)) return '最终放行结果尚未确认，请人工核对后刷新'
    if (row.inactionReason?.trim()) return row.inactionReason
    if (activeTab.value !== 'candidate') return '仅冻结候选可处理管理者代表最终放行'
    if (row.status !== EDHR_WORK_TASK_STATUS_TODO) return '该最终放行待办已结束'
    if (!hasManagerReleaseTaskContext(row)) return '最终放行待办缺少事务或版本信息'
    return '当前用户是冻结候选，可执行最终放行'
  }
  if (isProductionReleaseReportTask(row)) {
    if (row.inactionReason?.trim()) return row.inactionReason
    if (activeTab.value !== 'candidate') return '仅冻结候选可处理生产放行报告'
    if (row.status !== EDHR_WORK_TASK_STATUS_TODO) return '该报告待办已结束'
    if (!hasProductionReleaseReportTaskContext(row)) return '报告待办缺少批次、节点或版本信息'
    return '当前用户是冻结候选，可上传并完成报告'
  }
  if (!isPqcProductionReleaseTask(row)) {
    return row.inactionReason || '当前用户可按既有入口处理'
  }
  if (isPqcDecisionLocked(row)) return '决定结果尚未确认，请人工核对后刷新'
  if (row.inactionReason?.trim()) return row.inactionReason
  if (activeTab.value !== 'candidate') return '仅冻结候选可处理PQC放行'
  if (row.status !== EDHR_WORK_TASK_STATUS_TODO) return '该PQC放行待办已结束'
  if (!hasPqcDecisionTaskContext(row)) return 'PQC放行待办缺少申请或版本信息'
  return '当前用户是冻结候选，可处理PQC放行'
}

const resolveReworkSourceLabel = (row: EdhrWorkTaskRespVO) => {
  if (row.taskType !== EDHR_WORK_TASK_TYPE_REWORK) return '--'
  return row.sourceExecutionId ? `来源执行 ${row.sourceExecutionId}` : '--'
}

const resolveTaskPrompt = (row: EdhrWorkTaskRespVO) => {
  if (isProductionReleaseReportTask(row)) {
    return `上传并完成${row.nodeName || '生产放行报告'}`
  }
  if (row.taskType === EDHR_WORK_TASK_TYPE_PQC_PRODUCTION_RELEASE) {
    return '核对正式生产证据后决定通过或拒绝'
  }
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
    const targetExecutionId = url.searchParams.get('id') || url.searchParams.get('executionId')
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
  queryParams.batchExecutionId = ''
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

const fillArchiveRuleForm = (rule: EdhrWorkTaskAssignmentRuleRespVO | null, routeId: number) => {
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
    const [routes, users] = await Promise.all([
      ProRouteApi.getRouteSimpleList(),
      getSimpleUserList()
    ])
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
      throw new Error(
        `${ruleDialogTitle.value}已保存，但责任人与实际责任源不一致，请重新检查路线配置。`
      )
    }
    message.success(`${ruleDialogTitle.value}已保存，责任人与实际责任源一致`)
    archiveRuleDialogVisible.value = false
  } catch (error) {
    message.error(resolveErrorMessage(error, `${ruleDialogTitle.value}保存失败。`))
  } finally {
    archiveRuleSaving.value = false
  }
}

const resetPqcDecisionDialog = () => {
  pqcDecisionLoading.value = false
  pqcDecisionSubmitting.value = false
  pqcDecisionAction.value = 'APPROVE'
  pqcDecisionTask.value = null
  pqcDecisionReceipt.value = null
  pqcDecisionBlockers.value = []
  pqcDecisionUncertainMessage.value = ''
  pqcDecisionIdempotencyKey.value = ''
  pqcDecisionForm.approvalOpinion = ''
  pqcDecisionForm.rejectReason = ''
  pqcDecisionFormRef.value?.clearValidate()
}

const requirePqcDecisionTaskContext = (row: EdhrWorkTaskRespVO) => ({
  applicationId: requireJsonLongId(row.businessScopeId, '生产放行申请编号'),
  workTaskId: requireJsonLongId(row.id, 'PQC放行待办编号'),
  version: requirePositiveVersion(row.version, 'PQC放行待办版本')
})

const assertPqcDecisionReceiptMatchesTask = (
  result: MesPqcProductionReleaseDecisionRespVO,
  row: EdhrWorkTaskRespVO
) => {
  const context = requirePqcDecisionTaskContext(row)
  if (requireJsonLongId(result.applicationId, '生产放行回执申请编号') !== context.applicationId) {
    throw new Error('生产放行回执与当前申请不一致。')
  }
  if (
    requireJsonLongId(result.pqcReleaseWorkTaskId, '生产放行回执待办编号') !== context.workTaskId
  ) {
    throw new Error('生产放行回执与当前PQC待办不一致。')
  }
  requirePositiveVersion(result.version, '生产放行回执版本')
}

const assertPqcDecisionResult = (
  result: MesPqcProductionReleaseDecisionRespVO,
  row: EdhrWorkTaskRespVO,
  action: 'APPROVE' | 'REJECT'
) => {
  assertPqcDecisionReceiptMatchesTask(result, row)
  if (action === 'APPROVE') {
    if (result.decision !== 'APPROVE' || result.status !== 'REPORT_UPLOAD_PENDING') {
      throw new Error('PQC通过回执状态与本次决定不一致。')
    }
    requireJsonLongId(result.batchExecutionId, 'PQC通过回执生产批次编号')
    if (!Array.isArray(result.reportUploadTasks) || result.reportUploadTasks.length !== 4) {
      throw new Error('PQC通过回执必须包含四个正式报告上传任务。')
    }
    result.reportUploadTasks.forEach((task) => {
      requireJsonLongId(task.batchTaskId, `${task.nodeType}批次任务编号`)
      requireJsonLongId(task.workTaskId, `${task.nodeType}工作待办编号`)
    })
    return
  }
  if (result.decision !== 'REJECT' || result.status !== 'PQC_RELEASE_REJECTED') {
    throw new Error('PQC拒绝回执状态与本次决定不一致。')
  }
  if (!result.rejectReason?.trim()) {
    throw new Error('PQC拒绝回执缺少拒绝原因。')
  }
}

const openPqcDecisionDialog = async (row: EdhrWorkTaskRespVO, action: 'APPROVE' | 'REJECT') => {
  resetPqcDecisionDialog()
  pqcDecisionAction.value = action
  pqcDecisionTask.value = row
  pqcDecisionDialogVisible.value = true
  pqcDecisionLoading.value = true
  try {
    if (!canHandlePqcProductionRelease(row)) {
      throw new Error(resolveInactionReasonLabel(row))
    }
    const { applicationId } = requirePqcDecisionTaskContext(row)
    const receipt = await getPqcProductionRelease(applicationId)
    assertPqcDecisionReceiptMatchesTask(receipt, row)
    pqcDecisionReceipt.value = receipt
    if (receipt.status !== 'PQC_RELEASE_PENDING') {
      message.warning(
        `该申请当前状态为${resolvePqcReleaseStatusLabel(receipt.status)}，不能重复处理。`
      )
    }
  } catch (error) {
    const failure = resolvePqcProductionReleaseFailure(error)
    if (failure) {
      pqcDecisionBlockers.value = failure.blockers
      message.error(resolveErrorMessage(error, failure.blockers[0].reason))
    } else {
      pqcDecisionUncertainMessage.value = resolveErrorMessage(
        error,
        '生产放行权威回执加载失败，无法提交PQC决定。'
      )
      message.error(pqcDecisionUncertainMessage.value)
    }
  } finally {
    pqcDecisionLoading.value = false
  }
}

const recoverUncertainPqcProductionReleaseDecision = async (
  row: EdhrWorkTaskRespVO,
  action: 'APPROVE' | 'REJECT',
  writeError: unknown
) => {
  const { applicationId } = requirePqcDecisionTaskContext(row)
  try {
    const receipt = await getPqcProductionRelease(applicationId)
    assertPqcDecisionReceiptMatchesTask(receipt, row)
    const expectedStatus = action === 'APPROVE' ? 'REPORT_UPLOAD_PENDING' : 'PQC_RELEASE_REJECTED'
    if (receipt.status !== expectedStatus) {
      throw new Error(`权威回执仍为${resolvePqcReleaseStatusLabel(receipt.status)}。`)
    }
    assertPqcDecisionResult(receipt, row, action)
    pqcDecisionReceipt.value = receipt
    pqcDecisionIdempotencyKey.value = ''
    message.warning(
      `决定响应异常，但权威回执已确认：${resolvePqcReleaseStatusLabel(receipt.status)}`
    )
    return true
  } catch (confirmationError) {
    pqcDecisionLockedApplicationIds.add(applicationId)
    pqcDecisionUncertainMessage.value =
      `PQC决定结果不确定，请人工核对后刷新页面：写入错误 ` +
      `${resolveErrorMessage(writeError, '请求响应异常')}；回执错误 ` +
      resolveErrorMessage(confirmationError, '权威回执确认失败')
    message.error(pqcDecisionUncertainMessage.value)
    return false
  }
}

const submitPqcProductionReleaseDecision = async () => {
  const row = pqcDecisionTask.value
  if (!row || !pqcDecisionReceipt.value) {
    message.error('PQC决定缺少权威申请回执，无法提交。')
    return
  }
  if (pqcDecisionReceipt.value.status !== 'PQC_RELEASE_PENDING') {
    message.error('该生产放行申请已不是待PQC处理状态。')
    return
  }
  await pqcDecisionFormRef.value?.validate()
  const context = requirePqcDecisionTaskContext(row)
  const expectedVersion = requirePositiveVersion(
    pqcDecisionReceipt.value.version,
    '生产放行权威版本'
  )
  if (!pqcDecisionIdempotencyKey.value) {
    pqcDecisionIdempotencyKey.value = createPqcDecisionIdempotencyKey(
      pqcDecisionAction.value,
      context.applicationId
    )
  }
  pqcDecisionSubmitting.value = true
  pqcDecisionBlockers.value = []
  let result: MesPqcProductionReleaseDecisionRespVO
  try {
    result =
      pqcDecisionAction.value === 'APPROVE'
        ? await approvePqcProductionRelease({
            applicationId: context.applicationId,
            pqcReleaseWorkTaskId: context.workTaskId,
            expectedVersion,
            idempotencyKey: pqcDecisionIdempotencyKey.value,
            approvalOpinion: pqcDecisionForm.approvalOpinion.trim() || undefined
          })
        : await rejectPqcProductionRelease({
            applicationId: context.applicationId,
            pqcReleaseWorkTaskId: context.workTaskId,
            expectedVersion,
            idempotencyKey: pqcDecisionIdempotencyKey.value,
            rejectReason: pqcDecisionForm.rejectReason.trim()
          })
  } catch (writeError) {
    const failure = resolvePqcProductionReleaseFailure(writeError)
    if (failure) {
      pqcDecisionBlockers.value = failure.blockers
      message.error(resolveErrorMessage(writeError, failure.blockers[0].reason))
    } else {
      await recoverUncertainPqcProductionReleaseDecision(row, pqcDecisionAction.value, writeError)
    }
    pqcDecisionSubmitting.value = false
    return
  }

  try {
    assertPqcDecisionResult(result, row, pqcDecisionAction.value)
  } catch (receiptError) {
    await recoverUncertainPqcProductionReleaseDecision(row, pqcDecisionAction.value, receiptError)
    pqcDecisionSubmitting.value = false
    return
  }

  pqcDecisionReceipt.value = result
  pqcDecisionIdempotencyKey.value = ''
  message.success(
    pqcDecisionAction.value === 'APPROVE'
      ? `PQC已通过，批次 ${result.batchExecutionId} 已创建，${result.reportUploadTasks.length} 个报告任务待上传。`
      : 'PQC已拒绝，申请已终止。'
  )
  await getList()
  if (loadError.value) {
    message.warning(`PQC决定已确认，但待办列表刷新失败：${loadError.value}`)
  }
  pqcDecisionSubmitting.value = false
}

const resetManagerReleaseDialog = () => {
  managerReleaseLoading.value = false
  managerReleaseSubmitting.value = false
  managerReleaseTask.value = null
  managerReleaseReceipt.value = null
  managerReleaseSnapshot.value = null
  managerReleaseBlockers.value = []
  managerReleaseUncertainMessage.value = ''
  managerReleaseIdempotencyKey.value = ''
  managerReleaseForm.signoffEvidenceHash = ''
  managerReleaseForm.approvalOpinion = ''
  managerReleaseFormRef.value?.clearValidate()
}

const requireManagerReleaseTaskContext = (row: EdhrWorkTaskRespVO) => ({
  releaseTransactionId: requireJsonLongId(row.businessScopeId, '放行事务编号'),
  workTaskId: requireJsonLongId(row.id, '管理者代表待办编号')
})

const assertManagerReleaseReceiptMatchesTask = (
  receipt: EdhrReleaseRowVO,
  row: EdhrWorkTaskRespVO
) => {
  const context = requireManagerReleaseTaskContext(row)
  if (
    requireJsonLongId(receipt.releaseTransactionId, '最终放行回执事务编号') !==
    context.releaseTransactionId
  ) {
    throw new Error('最终放行回执与当前事务不一致。')
  }
  if (
    requireJsonLongId(receipt.releaseApprovalWorkTaskId, '最终放行回执待办编号') !==
    context.workTaskId
  ) {
    throw new Error('最终放行回执与当前管理者代表待办不一致。')
  }
  requireJsonLongId(receipt.batchExecutionId, '最终放行回执生产批次编号')
  requirePositiveVersion(receipt.version, '最终放行回执版本')
}

const assertManagerReleaseApprovalResult = (
  receipt: EdhrReleaseRowVO,
  row: EdhrWorkTaskRespVO,
  signoffEvidenceHash?: string
) => {
  assertManagerReleaseReceiptMatchesTask(receipt, row)
  if (receipt.releaseStatus !== 'RELEASED') {
    throw new Error(
      `最终放行回执状态为${resolveManagerReleaseStatusLabel(receipt.releaseStatus)}，不是已放行。`
    )
  }
  if (!receipt.approvedAt || !receipt.approvedBy) {
    throw new Error('最终放行回执缺少审批人或审批时间。')
  }
  if (
    signoffEvidenceHash &&
    receipt.approvalSignoffEvidenceHash !== signoffEvidenceHash
  ) {
    throw new Error('最终放行回执的签核证据与本次提交不一致。')
  }
}

const openManagerReleaseDialog = async (row: EdhrWorkTaskRespVO) => {
  resetManagerReleaseDialog()
  managerReleaseTask.value = row
  managerReleaseDialogVisible.value = true
  managerReleaseLoading.value = true
  try {
    const simulationRunId =
      typeof route.query.simulationRunId === 'string' ? route.query.simulationRunId.trim() : ''
    if (simulationRunId) {
      const signoffEvidenceHash = window.localStorage.getItem(
        `${STAGE5_SIMULATION_SIGNOFF_STORAGE_KEY}:${simulationRunId}`
      )?.trim()
      if (!signoffEvidenceHash) {
        throw new Error('Stage5模拟缺少管理者电子签名证据哈希，无法提交正式放行。')
      }
      managerReleaseForm.signoffEvidenceHash = signoffEvidenceHash
    }
    if (!canHandleManagerRelease(row)) {
      throw new Error(resolveInactionReasonLabel(row))
    }
    const { releaseTransactionId } = requireManagerReleaseTaskContext(row)
    const receipt = await getEdhrRelease(releaseTransactionId)
    assertManagerReleaseReceiptMatchesTask(receipt, row)
    managerReleaseReceipt.value = receipt
    if (receipt.releaseStatus !== 'PENDING_APPROVAL') {
      message.warning(
        `该事务当前状态为${resolveManagerReleaseStatusLabel(receipt.releaseStatus)}，不能重复处理。`
      )
    }
  } catch (error) {
    const failure = resolvePqcProductionReleaseFailure(error)
    if (failure) {
      managerReleaseBlockers.value = failure.blockers
      message.error(resolveErrorMessage(error, failure.blockers[0].reason))
    } else {
      managerReleaseUncertainMessage.value = resolveErrorMessage(
        error,
        '最终放行权威回执加载失败，无法提交审批。'
      )
      message.error(managerReleaseUncertainMessage.value)
    }
  } finally {
    managerReleaseLoading.value = false
  }
}

const recoverUncertainManagerReleaseApproval = async (
  row: EdhrWorkTaskRespVO,
  writeError: unknown
) => {
  const { releaseTransactionId } = requireManagerReleaseTaskContext(row)
  try {
    const receipt = await getEdhrRelease(releaseTransactionId)
    assertManagerReleaseReceiptMatchesTask(receipt, row)
    if (receipt.releaseStatus !== 'RELEASED') {
      throw new Error(`权威回执仍为${resolveManagerReleaseStatusLabel(receipt.releaseStatus)}。`)
    }
    assertManagerReleaseApprovalResult(
      receipt,
      row,
      managerReleaseForm.signoffEvidenceHash.trim()
    )
    await loadStage5ReleaseSnapshot(receipt)
    managerReleaseReceipt.value = receipt
    managerReleaseIdempotencyKey.value = ''
    message.warning('最终放行响应异常，但权威回执已确认事务为已放行。')
    return true
  } catch (confirmationError) {
    managerReleaseLockedTransactionIds.add(releaseTransactionId)
    managerReleaseUncertainMessage.value =
      `最终放行结果不确定，请人工核对后刷新页面：写入错误 ` +
      `${resolveErrorMessage(writeError, '请求响应异常')}；回执错误 ` +
      resolveErrorMessage(confirmationError, '权威回执确认失败')
    message.error(managerReleaseUncertainMessage.value)
    return false
  }
}

const refreshManagerReleaseListAfterSuccess = async () => {
  await getList()
  if (loadError.value) {
    message.warning(`最终放行已确认，但待办列表刷新失败：${loadError.value}`)
  }
}

const loadStage5ReleaseSnapshot = async (receipt: EdhrReleaseRowVO) => {
  const simulationRunId =
    typeof route.query.simulationRunId === 'string' ? route.query.simulationRunId.trim() : ''
  if (!simulationRunId) return
  const snapshot = await getEdhrStage5ReleaseSnapshot(simulationRunId, receipt.batchExecutionId)
  const evidence = snapshot.threeFileEvidence
  if (
    snapshot.releaseStatus !== 'RELEASED' ||
    !snapshot.releaseReceiptId ||
    !snapshot.releaseDecisionId ||
    !Array.isArray(evidence) ||
    evidence.length !== 3
  ) {
    throw new Error('Stage5正式放行回执缺少完整追溯证据。')
  }
  managerReleaseSnapshot.value = snapshot
}

const submitManagerReleaseApproval = async () => {
  const row = managerReleaseTask.value
  const currentReceipt = managerReleaseReceipt.value
  if (!row || !currentReceipt) {
    message.error('最终放行缺少权威事务回执，无法提交。')
    return
  }
  if (currentReceipt.releaseStatus !== 'PENDING_APPROVAL') {
    message.error('该事务已不是待管理者代表放行状态。')
    return
  }
  await managerReleaseFormRef.value?.validate()
  const context = requireManagerReleaseTaskContext(row)
  const expectedVersion = requirePositiveVersion(currentReceipt.version, '最终放行权威版本')
  if (!managerReleaseIdempotencyKey.value) {
    managerReleaseIdempotencyKey.value = createManagerReleaseIdempotencyKey(
      context.releaseTransactionId
    )
  }
  managerReleaseSubmitting.value = true
  managerReleaseBlockers.value = []
  let result: EdhrReleaseRowVO
  try {
    result = await approveEdhrRelease({
      releaseTransactionId: context.releaseTransactionId,
      workTaskId: context.workTaskId,
      expectedVersion,
      idempotencyKey: managerReleaseIdempotencyKey.value,
      signoffEvidenceHash: managerReleaseForm.signoffEvidenceHash.trim(),
      approvalOpinion: managerReleaseForm.approvalOpinion.trim() || undefined
    })
  } catch (writeError) {
    const failure = resolvePqcProductionReleaseFailure(writeError)
    if (failure) {
      managerReleaseBlockers.value = failure.blockers
      message.error(resolveErrorMessage(writeError, failure.blockers[0].reason))
    } else if (await recoverUncertainManagerReleaseApproval(row, writeError)) {
      await refreshManagerReleaseListAfterSuccess()
    }
    managerReleaseSubmitting.value = false
    return
  }

  try {
    assertManagerReleaseApprovalResult(
      result,
      row,
      managerReleaseForm.signoffEvidenceHash.trim()
    )
  } catch (receiptError) {
    if (await recoverUncertainManagerReleaseApproval(row, receiptError)) {
      await refreshManagerReleaseListAfterSuccess()
    }
    managerReleaseSubmitting.value = false
    return
  }

  managerReleaseReceipt.value = result
  managerReleaseIdempotencyKey.value = ''
  try {
    await loadStage5ReleaseSnapshot(result)
  } catch (snapshotError) {
    managerReleaseUncertainMessage.value = `最终放行已确认，但 Stage5 追溯回执读取失败：${resolveErrorMessage(
      snapshotError,
      '请人工核对后刷新页面'
    )}`
    message.error(managerReleaseUncertainMessage.value)
    managerReleaseSubmitting.value = false
    return
  }
  message.success(`最终放行已确认，生产批次 ${result.batchExecutionId} 已进入可追溯列表。`)
  await getList()
  if (loadError.value) {
    message.warning(`最终放行已确认，但待办列表刷新失败：${loadError.value}`)
  }
  managerReleaseSubmitting.value = false
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

const resolveManagerReleaseStatusLabel = (status?: string) => {
  const labels: Record<string, string> = {
    PENDING_APPROVAL: '待管理者代表放行',
    RELEASED: '已放行'
  }
  return status ? labels[status] || status : '--'
}

const openProductionReleaseReportTask = async (row: EdhrWorkTaskRespVO) => {
  try {
    if (!canHandleProductionReleaseReport(row)) {
      throw new Error(resolveInactionReasonLabel(row))
    }
    const batchExecutionId = requireJsonLongId(row.batchExecutionId, '报告批次编号')
    const batchTaskId = requireJsonLongId(row.batchTaskId, '报告节点任务编号')
    const workTaskId = requireJsonLongId(row.id, '报告工作待办编号')
    const expectedVersion = requirePositiveVersion(row.version, '生产放行申请版本')
    const url = resolveTaskActionUrl(row)
    url.searchParams.set('batchExecutionId', batchExecutionId)
    url.searchParams.set('batchTaskId', batchTaskId)
    url.searchParams.set('workTaskId', workTaskId)
    url.searchParams.set('nodeType', String(row.nodeType))
    url.searchParams.set('expectedVersion', String(expectedVersion))
    await router.push(`${url.pathname}${url.search}`)
  } catch (error) {
    message.error(resolveErrorMessage(error, '生产放行报告入口打开失败。'))
  }
}

const handleCompleteCandidateSignature = async (row: EdhrWorkTaskRespVO) => {
  if (!row.executionId) {
    message.error('候选审核任务缺少执行记录，无法完成签名。')
    return
  }
  try {
    await message.confirm(
      `确认完成「${row.processName || row.taskCode || row.id}」的候选审核签名？`
    )
    await completeEdhrCandidateSignatureTask(row.id, row.executionId)
    message.success('签名完成')
    await getList()
  } catch (error) {
    if (error === 'cancel') return
    message.error(resolveErrorMessage(error, '候选审核签名失败。'))
  }
}

const initializeRouteFilters = () => {
  const taskType = typeof route.query.taskType === 'string' ? route.query.taskType.trim() : ''
  const batchExecutionId =
    typeof route.query.batchExecutionId === 'string' ? route.query.batchExecutionId.trim() : ''
  if (taskType) {
    queryParams.taskType = taskType
    activeTab.value = 'candidate'
  }
  if (batchExecutionId) queryParams.batchExecutionId = batchExecutionId
}

onMounted(() => {
  initializeRouteFilters()
  getList()
})
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

.edhr-work-task-page__pqc-dialog,
.edhr-work-task-page__pqc-blockers {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
