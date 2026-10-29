<template>
  <ContentWrap>
    <div class="approval-center">
      <UnifiedListTemplate
        :table-key="approvalCenterTableKey"
        :query-model="queryParams"
        query-form-test-id="approval-center-filter-form"
        label-width="72px"
        :filter-definitions="approvalQuickFilterDefinitions"
        :quick-filter-state="approvalQuickFilterState"
        :selected-filter-definition="approvalSelectedFilterDefinition"
        :operator-options="approvalOperatorOptions"
        :columns="approvalColumns"
        :column-saving="approvalColumnSaving"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="updateApprovalQuickFilterState"
        @quick-filter-query="applyApprovalQuickFilter"
        @column-change="saveApprovalColumnConfig"
        @column-reset="resetApprovalColumnConfig"
        @pagination="handlePagination"
      >
        <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
          <div class="approval-center__table-scope" data-user-table-column-explicit-scope>
            <el-alert
              v-if="loadError"
              :title="loadError"
              type="error"
              :closable="false"
              show-icon
              class="approval-center__error"
            />

            <el-table
              v-loading="loading"
              class="approval-center__table"
              data-user-table-column-explicit
              :data-user-table-key="approvalCenterTableKey"
              :data="list"
              row-key="id"
              border
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无审批任务"
              @header-dragend="handleApprovalHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isApprovalColumnVisible('source')"
                label="审批来源"
                prop="source"
                :width="getApprovalColumnWidthString('source', 150)"
                v-bind="sortColumnAttrs('source')"
              >
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ resolveModuleName(row.moduleCode) }}</el-tag>
                  <div class="approval-center__muted">{{ resolveSourceTaskTypeLabel(row) }}</div>
                  <div v-if="row.moduleCode === 'BPM'" class="approval-center__muted">由业务提交生成的流程任务</div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('businessSummary')"
                label="业务摘要"
                prop="businessSummary"
                :min-width="getApprovalColumnMinWidthString('businessSummary', 300)"
                v-bind="sortColumnAttrs('businessSummary')"
              >
                <template #default="{ row }">
                  <div class="approval-center__primary-row">
                    <span class="approval-center__primary">{{ resolveBusinessTitleLabel(row) }}</span>
                    <el-tag v-if="row.businessDeleted" size="small" type="danger" effect="plain">已删除</el-tag>
                  </div>
                  <div
                    v-if="!row.businessIdentifierHidden"
                    class="approval-center__muted"
                  >
                    {{ resolveBusinessIdentifierLabel(row) }}
                  </div>
                  <div v-if="row.moduleCode === 'EDHR'" class="approval-center__muted">
                    <div>任务：{{ edhrReadDisplay(row).taskLabel }}（{{ edhrReadDisplay(row).sourceStatus }}）</div>
                    <div>业务状态：{{ edhrReadDisplay(row).businessStatus }}</div>
                    <div v-if="queryParams.viewType !== 'DONE'">审批决定：{{ edhrReadDisplay(row).decisionLabel }}</div>
                    <template v-if="queryParams.viewType !== 'DONE'">
                      <div>决定原码：{{ edhrReadDisplay(row).decisionSourceStatus }}</div>
                      <div class="approval-center__stored-text">审批意见：{{ edhrReadDisplay(row).opinion }}</div>
                      <div v-if="row.sourceRemark" class="approval-center__stored-text">原始记录/来源说明：{{ row.sourceRemark }}</div>
                      <div v-if="row.terminationReason" class="approval-center__stored-text">终止原因：{{ row.terminationReason }}</div>
                    </template>
                  </div>
                  <div
                    v-if="resolveVisibleDccKeyFields(row).length"
                    class="approval-center__dcc-key-fields"
                    data-testid="approval-center-dcc-key-fields"
                  >
                    <span
                      v-for="field in resolveVisibleDccKeyFields(row)"
                      :key="field.label"
                    >
                      {{ field.label }}：{{ field.value }}
                    </span>
                  </div>
                  <div
                    v-if="resolveVisibleBusinessContextTags(row).length"
                    class="approval-center__dcc-context"
                    data-testid="approval-center-dcc-business-context"
                  >
                    <el-tag
                      v-for="tag in resolveVisibleBusinessContextTags(row)"
                      :key="tag"
                      size="small"
                      effect="plain"
                    >
                      {{ resolveBusinessContextTagLabel(tag) }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('applicant')"
                label="申请人"
                prop="applicant"
                :width="getApprovalColumnWidthString('applicant', 140)"
                v-bind="sortColumnAttrs('applicant')"
              >
                <template #default="{ row }">
                  <span class="approval-center__applicant" :title="resolveApplicantLabel(row)">
                    {{ resolveApplicantLabel(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('node')"
                label="节点"
                prop="node"
                :width="getApprovalColumnWidthString('node', 190)"
                v-bind="sortColumnAttrs('node')"
              >
                <template #default="{ row }">
                  <div>{{ resolveNodeNameLabel(row) }}</div>
                  <div class="approval-center__muted">{{ resolveNodeSubLabel(row) }}</div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('reviewer')"
                label="审核人"
                prop="reviewer"
                :width="getApprovalColumnWidthString('reviewer', 140)"
                v-bind="sortColumnAttrs('reviewer')"
              >
                <template #default="{ row }">
                  <span class="approval-center__reviewer" :title="resolveReviewerLabel(row)">
                    {{ resolveReviewerLabel(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="queryParams.viewType === 'DONE' && isApprovalColumnVisible('approvalResult')"
                label="审批结果"
                prop="approvalResult"
                :width="getApprovalColumnWidthString('approvalResult', 120)"
                v-bind="sortColumnAttrs('approvalResult')"
              >
                <template #default="{ row }">
                  <template v-if="row.moduleCode === 'EDHR'">
                    <el-tag size="small" :type="edhrReadDisplay(row).decisionType">{{ edhrReadDisplay(row).decisionLabel }}</el-tag>
                    <div class="approval-center__muted">决定原码：{{ edhrReadDisplay(row).decisionSourceStatus }}</div>
                  </template>
                  <el-tag
                    v-else-if="row.approvalResult"
                    size="small"
                    :type="resolveApprovalResultTagType(row.approvalResult)"
                    effect="light"
                  >
                    {{ resolveApprovalResultLabel(row.approvalResult) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="queryParams.viewType === 'DONE' && isApprovalColumnVisible('approvalRemark')"
                label="备注"
                prop="approvalRemark"
                :min-width="getApprovalColumnMinWidthString('approvalRemark', 220)"
                v-bind="sortColumnAttrs('approvalRemark')"
              >
                <template #default="{ row }">
                  <template v-if="row.moduleCode === 'EDHR'">
                    <div class="approval-center__stored-text">审批意见：{{ edhrReadDisplay(row).opinion }}</div>
                    <div v-if="row.sourceRemark" class="approval-center__stored-text approval-center__muted">原始记录/来源说明：{{ row.sourceRemark }}</div>
                    <div v-if="row.terminationReason" class="approval-center__stored-text approval-center__muted">终止原因：{{ row.terminationReason }}</div>
                    <div class="approval-center__muted">证据类型：{{ edhrReadDisplay(row).evidenceType }}</div>
                    <div class="approval-center__muted">证据 ID：{{ edhrReadDisplay(row).evidenceId }}</div>
                  </template>
                  <span v-else class="approval-center__remark" :title="resolveApprovalRemark(row)">
                    {{ resolveApprovalRemark(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('capabilities')"
                label="能力"
                prop="capabilities"
                :width="getApprovalColumnWidthString('capabilities', APPROVAL_CAPABILITIES_DEFAULT_WIDTH)"
                v-bind="sortColumnAttrs('capabilities')"
              >
                <template #default="{ row }">
                  <div class="approval-center__capabilities">
                    <el-tag
                      v-if="row.requiresSignature"
                      size="small"
                      type="warning"
                      class="mr-6px"
                    >
                      签名
                    </el-tag>
                    <el-tag
                      v-for="capability in compactApprovalCapabilities(row).visible"
                      :key="capability"
                      size="small"
                      class="mr-6px mb-4px"
                    >
                      {{ resolveCapabilityLabel(capability) }}
                    </el-tag>
                    <el-popover v-if="compactApprovalCapabilities(row).overflow.length" trigger="click" placement="bottom" :width="240">
                      <template #reference><el-button link type="primary" aria-label="查看全部审批能力">更多</el-button></template>
                      <div>全部能力</div>
                      <el-tag v-if="row.requiresSignature" size="small" class="mr-6px mb-4px">签名</el-tag>
                      <el-tag v-for="capability in row.capabilities || []" :key="capability" size="small" class="mr-6px mb-4px">{{ resolveCapabilityLabel(capability) }}</el-tag>
                    </el-popover>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('time')"
                label="时间"
                prop="time"
                :width="getApprovalColumnWidthString('time', 190)"
                v-bind="sortColumnAttrs('time')"
              >
                <template #default="{ row }">
                  <template v-if="row.moduleCode === 'EDHR'">
                    <div>任务创建：{{ formatEdhrTime(row.taskCreatedAt) }}</div>
                    <div class="approval-center__muted">任务完成：{{ formatEdhrTime(row.taskCompletedAt) }}</div>
                    <div v-if="row.taskCompletedAt" class="approval-center__muted">完成者：姓名未记录；{{ edhrReadDisplay(row).completionActorIdentity }}</div>
                    <div class="approval-center__muted">正式决定：{{ formatEdhrTime(row.decisionAt) }}</div>
                  </template>
                  <template v-else>
                    <div>{{ formatApprovalTime(row.taskCreatedAt || row.initiatedAt) }}</div>
                    <div v-if="row.taskCompletedAt" class="approval-center__muted">{{ formatApprovalTime(row.taskCompletedAt) }}</div>
                  </template>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('actions')"
                label="操作"
                fixed="right"
                prop="actions"
                :width="getApprovalColumnWidthString('actions', 180)"
              >
                <template #default="{ row }">
                  <el-tooltip
                    :disabled="canOpenView(row)"
                    :content="resolveViewDisabledReason(row)"
                    placement="top"
                  >
                    <span>
                      <el-button
                        link
                        type="primary"
                        data-approval-action="view"
                        :disabled="!canOpenView(row)"
                        @click="openModuleDetail(row)"
                      >
                        查看
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-button
                    v-if="canReviewAction(row)"
                    link
                    type="primary"
                    data-approval-action="review"
                    :disabled="row.businessDeleted"
                    @click="openReviewAction(row)"
                  >
                    {{ canReview(row) ? '审核' : '前往业务办理' }}
                  </el-button>
                  <el-tooltip
                    :disabled="canOpenFlow(row)"
                    :content="resolveFlowDisabledReason(row)"
                    placement="top"
                  >
                    <span>
                      <el-button
                        link
                        type="primary"
                        data-approval-action="flow"
                        :disabled="!canOpenFlow(row)"
                        @click="openTimeline(row)"
                      >
                        流程
                      </el-button>
                    </span>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </UnifiedListTemplate>
    </div>

    <el-drawer
      v-model="timelineDrawerVisible"
      title="审批流程"
      size="560px"
      append-to-body
      destroy-on-close
      class="approval-center__timeline-drawer"
    >
      <div v-if="timelineTask" class="approval-center__timeline-summary">
        <el-tag size="small" type="info">{{ resolveModuleName(timelineTask.moduleCode) }}</el-tag>
        <span class="approval-center__timeline-title">{{ resolveBusinessTitleLabel(timelineTask) }}</span>
      </div>

      <el-alert
        v-if="timelineError"
        :title="timelineError"
        type="error"
        :closable="false"
        show-icon
        class="approval-center__error"
      />

      <div v-loading="timelineLoading" class="approval-center__timeline-body">
        <el-empty
          v-if="!timelineLoading && !timelineError && timelineRows.length === 0"
          description="暂无审批流程记录"
        />
        <el-timeline v-else>
          <el-timeline-item
            v-for="entry in timelineRows"
            :key="entry.id"
            :timestamp="entry.moduleCode === 'EDHR' ? undefined : formatTimelineTime(entry.actedAt)"
            :type="entry.moduleCode === 'EDHR' ? edhrReadDisplay(entry).decisionType : resolveTimelineType(entry.status)"
            placement="top"
          >
            <div class="approval-center__timeline-item">
              <div class="approval-center__timeline-node">
                <span>{{ entry.nodeName || entry.nodeCode || '--' }}</span>
                <el-tag size="small" effect="plain">{{ entry.moduleCode === 'EDHR' ? edhrReadDisplay(entry).taskLabel : entry.actionLabel || entry.status || '--' }}</el-tag>
              </div>
              <template v-if="entry.moduleCode === 'EDHR'">
                <div>审批决定：{{ edhrReadDisplay(entry).decisionLabel }}</div>
                <div class="approval-center__muted">任务原码：{{ edhrReadDisplay(entry).sourceStatus }}；业务状态：{{ edhrReadDisplay(entry).businessStatus }}</div>
                <div class="approval-center__muted">决定原码：{{ edhrReadDisplay(entry).decisionSourceStatus }}</div>
                <div class="approval-center__muted">决定人：{{ edhrReadDisplay(entry).actorName }}；{{ edhrReadDisplay(entry).actorIdentity }}</div>
                <div v-if="entry.taskOutcome === 'CREATED'" class="approval-center__muted">创建人：姓名未记录；{{ edhrReadDisplay(entry).creationActorIdentity }}</div>
                <div v-if="entry.taskCompletedAt" class="approval-center__muted">完成者：姓名未记录；{{ edhrReadDisplay(entry).completionActorIdentity }}</div>
                <div class="approval-center__stored-text">审批意见：{{ edhrReadDisplay(entry).opinion }}</div>
                <div v-if="entry.sourceRemark" class="approval-center__stored-text approval-center__muted">原始记录/来源说明：{{ entry.sourceRemark }}</div>
                <div v-if="entry.terminationReason" class="approval-center__stored-text approval-center__muted">终止原因：{{ entry.terminationReason }}</div>
                <div class="approval-center__muted">任务创建：{{ formatEdhrTime(entry.taskCreatedAt) }}</div>
                <div class="approval-center__muted">任务完成：{{ formatEdhrTime(entry.taskCompletedAt) }}</div>
                <div class="approval-center__muted">正式决定：{{ formatEdhrTime(entry.decisionAt) }}</div>
                <div class="approval-center__muted">证据类型：{{ edhrReadDisplay(entry).evidenceType }}</div>
                <div class="approval-center__muted">证据 ID：{{ edhrReadDisplay(entry).evidenceId }}</div>
                <div class="approval-center__muted">领域记录引用：{{ edhrReadDisplay(entry).domainReferenceId }}</div>
              </template>
              <template v-else>
              <div class="approval-center__muted">
                处理人：{{ entry.actorUserId || '--' }}
              </div>
              <div v-if="entry.comment" class="approval-center__timeline-comment">
                {{ entry.comment }}
              </div>
              <div class="approval-center__muted">
                {{ entry.evidenceType || '未标识证据类型' }}
                <span v-if="entry.domainReferenceId"> / {{ entry.domainReferenceId }}</span>
              </div>
              </template>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <el-dialog
      v-model="reviewDialogVisible"
      title="审核确认"
      width="min(1000px, 95vw)"
      append-to-body
      destroy-on-close
      class="approval-center__review-dialog"
    >
      <div v-if="reviewTask" class="approval-center__review-summary-card">
        <div class="approval-center__review-summary-main">
          <el-tag size="small" type="info">{{ resolveModuleName(reviewTask.moduleCode) }}</el-tag>
          <span>{{ resolveBusinessTitleLabel(reviewTask) }}</span>
        </div>
        <div class="approval-center__review-summary-hint">
          {{ reviewForm.result === 'APPROVE' ? '审核通过需填写审批意见并完成电子签名确认。' : '审核不通过需填写原因并完成电子签名确认。' }}
        </div>
      </div>
      <section v-if="reviewTask?.moduleCode === 'EDHR'" v-loading="reviewPreviewLoading">
        <el-alert v-if="reviewPreviewError" :title="reviewPreviewError" type="error" :closable="false" />
        <template v-if="reviewPreview">
          <p>{{ reviewPreview.content.scopeText }}。签署本次展示的审批内容。</p>
          <p>正文摘要：{{ reviewPreview.content.contentHash }}</p>
          <div style="max-height: 380px; overflow: auto; white-space: pre-wrap; overflow-wrap: anywhere;">
            {{ JSON.stringify(JSON.parse(reviewPreview.content.canonicalBusinessContentJson), null, 2) }}
          </div>
          <p v-for="file in reviewPreview.content.attachments" :key="file.fileId">
            <a :href="file.readUrl" target="_blank" rel="noopener noreferrer">{{ file.fileName }}</a>
            · 版本 {{ file.version }} · {{ file.fileSize }} 字节 · SHA256 {{ file.sha256 }}
          </p>
        </template>
        <el-button :disabled="reviewSubmitting" @click="loadReviewPreview">重新预览</el-button>
      </section>
      <el-form label-width="96px" class="approval-center__review-form">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.result">
            <el-radio-button v-if="reviewTask?.availableActions?.includes('APPROVE')" label="APPROVE">审核通过</el-radio-button>
            <el-radio-button v-if="reviewTask?.availableActions?.includes('REJECT')" label="REJECT">审核不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          :label="reviewForm.result === 'REJECT' ? '不通过原因' : '审批意见'"
          required
        >
          <el-input
            v-model="reviewForm.reason"
            type="textarea"
            :rows="3"
            maxlength="4000"
            show-word-limit
            :placeholder="reviewForm.result === 'REJECT' ? '请输入审核不通过原因' : '请输入审批意见'"
          />
        </el-form-item>
        <el-form-item label="电子签名" required>
          <el-input
            v-model="reviewForm.signaturePassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="请输入当前登录密码完成电子签名"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" :disabled="reviewTask?.moduleCode === 'EDHR' && (!reviewPreview || reviewPreviewLoading)" @click="submitReview">
          确认审核
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
import { edhrReadDisplay, bpmMissingPermissionReason, canProcessBpmInModule, bpmModuleDecisionLocation, compactApprovalCapabilities, APPROVAL_CAPABILITIES_DEFAULT_WIDTH } from './approvalReadDisplay'
import { formatDate } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  getApprovalCenterModules,
  getApprovalTaskPage,
  getApprovalTaskTimeline,
  reviewApprovalTask,
  getApprovalReviewPreview,
  type ApprovalReviewPreview,
  type ApprovalModuleCode,
  type ApprovalProviderDescriptorVO,
  type ApprovalTaskCapability,
  type ApprovalTaskPageReqVO,
  type ApprovalTaskReviewResult,
  type ApprovalTaskSummaryVO,
  type ApprovalTaskTimelineEntryVO,
  type ApprovalTaskViewType
} from '@/api/approval-center'
import { useApprovalTodoBadgeStore } from '@/store/modules/approvalTodoBadge'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { createApprovalListLoader, completeApprovalWrite, approvalTaskIdentityKey } from '@/store/modules/approvalCenterInvalidationState'
import { DCC_OFFLINE_TRAINING_RECORD, resolveOfflineTrainingRecordLocation } from '@/utils/dccOfflineTrainingRecord'

defineOptions({ name: 'ApprovalCenterWorkbench' })

const router = useRouter()
const route = useRoute()
const approvalCenterRouteInstanceName = String(route.name || '')

type ApprovalCenterListViewType = ApprovalTaskViewType

type ApprovalCenterQueryParams = Omit<ApprovalTaskPageReqVO, 'pageNo' | 'pageSize' | 'viewType' | 'keyword'> & {
  pageNo: number
  pageSize: number
  viewType: ApprovalCenterListViewType
  keyword: string
}

type PaginationPayload = {
  page?: number
  limit?: number
}

const approvalTabNames = ['todo', 'done', 'my-initiated', 'cc'] as const
type ApprovalTabName = (typeof approvalTabNames)[number]

const viewTabs: Array<{ label: string; value: ApprovalCenterListViewType }> = [
  { label: '待办', value: 'TODO' },
  { label: '已办', value: 'DONE' },
  { label: '我发起的', value: 'MY_INITIATED' },
  { label: '抄送我的', value: 'CC' }
]

const routeTabToViewType: Record<ApprovalTabName, ApprovalCenterListViewType> = {
  todo: 'TODO',
  done: 'DONE',
  'my-initiated': 'MY_INITIATED',
  cc: 'CC'
}

const approvalTabRoutes: Record<ApprovalTabName, string> = {
  todo: '/approval-center/todo',
  done: '/approval-center/done',
  'my-initiated': '/approval-center/my-initiated',
  cc: '/approval-center/cc'
}

const viewTypeToRouteTab: Record<ApprovalCenterListViewType, ApprovalTabName> = {
  TODO: 'todo',
  DONE: 'done',
  MY_INITIATED: 'my-initiated',
  CC: 'cc'
}

const supportedViewTypes: ApprovalCenterListViewType[] = viewTabs.map((item) => item.value)
const supportedModuleCodes: ApprovalModuleCode[] = ['BPM', 'DCC', 'EDHR', 'SHOWROOM', 'SRM', 'MES_FEEDBACK']
const EMPTY_APPROVAL_DISPLAY = '--'

const APPROVAL_MODULE_LABELS: Record<ApprovalModuleCode, string> = {
  BPM: '流程审批',
  DCC: '文控审批',
  EDHR: '电子批记录审批',
  SHOWROOM: '展厅审批',
  SRM: '供应商协同审批',
  MES_FEEDBACK: '生产报工审批'
}

const APPROVAL_SOURCE_TASK_TYPE_LABELS: Record<string, string> = {
  BPM_TASK_TODO: '流程待办任务',
  BPM_TASK_DONE: '流程已办任务',
  BPM_PROCESS_INSTANCE: '流程实例',
  BPM_PROCESS_INSTANCE_COPY: '流程抄送实例',
  DCC_CONTROLLED_FILE_TASK: '文控受控文件任务',
  EDHR_WORK_TASK: '电子批记录工作任务',
  MES_PRO_FEEDBACK: '生产报工复核任务',
  DCC_PROJECT_PRODUCT_REVIEW: '项目产品审核',
  DCC_PROJECT_PRODUCT_APPROVAL: '项目产品批准',
  DCC_OFFLINE_TRAINING_RECORD: '文控上传线下培训记录'
}

const APPROVAL_BUSINESS_TITLE_LABELS: Record<string, string> = {
  'DCC Controlled File Approval': '文控受控文件审批',
  'Expense Dept Leader Approval': '费用部门负责人审批',
  'eDHR Approval V1': '电子批记录审批第一版'
}

const APPROVAL_STATUS_LABELS: Record<string, string> = {
  MY_INITIATED: '我发起的',
  TODO: '待办',
  DONE: '已办',
  CC: '抄送我的',
  APPROVE: '通过',
  APPROVED: '已通过',
  REJECT: '驳回',
  REJECTED: '已驳回',
  CANCELED: '已取消',
  CANCELLED: '已取消',
  RUNNING: '进行中',
  PENDING: '待处理',
  PENDING_SUPERVISOR_REVIEW: '待直属主管审核',
  PENDING_MATRIX_REVIEW: '待会签',
  PENDING_MATRIX_APPROVAL: '待批准',
  PENDING_DOC_CONTROL_REVIEW: '待文控审核',
  PENDING_APPLICANT_REWORK: '待申请人返工',
  PENDING_APPLICANT_TRAINING_RECORD: '待文控上传培训记录',
  PENDING_DELAYED_EFFECTIVE_CONFIRM: '待延迟生效确认',
  CONTROLLED_PENDING_EFFECTIVE: '受控（待生效）',
  WORKING: '工作小版本',
  DRAFT: '草稿',
  NEW: '新建',
  SUBMITTED: '已提交',
  APPROVING: '审批中',
  ACTIVE: '现行',
  SUPERSEDED: '已替代',
  LEGACY_INCOMPLETE: '历史不完整（不可执行）',
  OBSOLETE: '已作废',
  WITHDRAWN: '已撤回',
  DELETED: '已删除'
}

const APPROVAL_NODE_LABELS: Record<string, string> = {
  ...APPROVAL_STATUS_LABELS,
  APPROVE_USER_SELECT: '选择审批人',
  START: '开始',
  START_EVENT: '开始',
  END: '结束',
  END_EVENT: '结束'
}

const APPROVAL_BUSINESS_KEY_PREFIX_LABELS: Record<string, string> = {
  FORM_ACTION: '表单动作'
}

const ENGLISH_LETTER_PATTERN = /[A-Za-z]/
const CHINESE_CHARACTER_PATTERN = /[\u4e00-\u9fff]/
const BUSINESS_CONTEXT_CODE_PATTERN = /^(?=.*\d)[A-Za-z0-9][A-Za-z0-9._/-]*$/
const EMPTY_CONTEXT_VALUE_PATTERN = /^(?:--|null|undefined)$/i

const modules = ref<ApprovalProviderDescriptorVO[]>([])
const list = ref<ApprovalTaskSummaryVO[]>([])
const total = ref(0)
const loading = ref(false)
const loadError = ref('')
const timelineDrawerVisible = ref(false)
const timelineLoading = ref(false)
const timelineError = ref('')
const timelineRows = ref<ApprovalTaskTimelineEntryVO[]>([])
const timelineTask = ref<ApprovalTaskSummaryVO>()
const reviewDialogVisible = ref(false)
const reviewPreview = ref<ApprovalReviewPreview>()
const reviewPreviewLoading = ref(false)
const reviewPreviewError = ref('')
const reviewIdempotencyKey = ref('')
const reviewSubmitting = ref(false)
const reviewTask = ref<ApprovalTaskSummaryVO>()
let approvalModulesLoaded = false
let approvalLastLoadedRouteStateKey: string | undefined
const approvalTodoBadgeStore = useApprovalTodoBadgeStore()
const approvalInvalidation = useApprovalCenterInvalidationStore()
let listUiGeneration = 0
let routeLoadPromise: Promise<void> | undefined
let routeLoadKey: string | undefined
let timelineGeneration = 0
let reviewGeneration = 0
const reviewForm = reactive<{
  result: ApprovalTaskReviewResult
  reason: string
  signaturePassword: string
}>({
  result: 'APPROVE',
  reason: '',
  signaturePassword: ''
})

const queryParams = reactive<ApprovalCenterQueryParams>({
  pageNo: 1,
  pageSize: 10,
  viewType: 'TODO',
  moduleCode: undefined,
  keyword: ''
})

const approvalCenterTableKeys: Record<ApprovalCenterListViewType, string> = {
  TODO: 'approval.center.todo.applicant.v1',
  DONE: 'approval.center.done.applicant.v1',
  MY_INITIATED: 'approval.center.myInitiated.applicant.v1',
  CC: 'approval.center.cc.applicant.v1'
}

const approvalDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'source', label: '审批来源', width: 150 },
  { key: 'businessSummary', label: '业务摘要', width: 300 },
  { key: 'applicant', label: '申请人', width: 140 },
  { key: 'node', label: '节点', width: 190 },
  { key: 'reviewer', label: '审核人', width: 140 },
  { key: 'approvalResult', label: '审批结果', width: 120 },
  { key: 'approvalRemark', label: '备注', minWidth: 220 },
  { key: 'capabilities', label: '能力', width: APPROVAL_CAPABILITIES_DEFAULT_WIDTH },
  { key: 'time', label: '时间', width: 190 },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false }
]

const approvalColumnControls = {
  TODO: useUserTableColumns(approvalCenterTableKeys.TODO, approvalDefaultColumns),
  DONE: useUserTableColumns(approvalCenterTableKeys.DONE, approvalDefaultColumns),
  MY_INITIATED: useUserTableColumns(approvalCenterTableKeys.MY_INITIATED, approvalDefaultColumns),
  CC: useUserTableColumns(approvalCenterTableKeys.CC, approvalDefaultColumns)
}

const activeApprovalColumnControl = computed(() => approvalColumnControls[queryParams.viewType])
const approvalCenterTableKey = computed(() => approvalCenterTableKeys[queryParams.viewType])
const approvalColumns = computed(() => activeApprovalColumnControl.value.columns.value)
const approvalColumnSaving = computed(() => activeApprovalColumnControl.value.saving.value)
const isApprovalColumnVisible = (key: string) => activeApprovalColumnControl.value.isColumnVisible(key)
const getApprovalColumnWidthString = (key: string, fallback?: number) =>
  activeApprovalColumnControl.value.getColumnWidthString(key, fallback)
const getApprovalColumnMinWidthString = (key: string, fallback?: number) =>
  activeApprovalColumnControl.value.getColumnWidthString(key, fallback)
const handleApprovalHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await activeApprovalColumnControl.value.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveApprovalColumnConfig = async (columns: UserTableColumnState[]) => {
  await activeApprovalColumnControl.value.saveConfig(columns)
}
const resetApprovalColumnConfig = async () => {
  await activeApprovalColumnControl.value.resetConfig()
}

const approvalQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'moduleCode',
    label: '审批来源',
    type: 'select',
    queryParamKey: 'moduleCode',
    options: modules.value.map((item) => ({
      label: resolveModuleName(item.moduleCode),
      value: item.moduleCode
    })),
    placeholder: '选择审批来源'
  },
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '标题、编号或批次'
  }
])


const isUnfilteredTodoQuery = () =>
  queryParams.viewType === 'TODO' && !queryParams.moduleCode && !queryParams.keyword.trim()

const loadModules = async () => {
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = buildApprovalCenterRouteStateKey()
  const isCurrent = () => approvalInvalidation.isCurrent(sessionKey) && routeKey === buildApprovalCenterRouteStateKey()
  approvalModulesLoaded = false
  try {
    const data = await getApprovalCenterModules({ ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return false
    modules.value = data
    approvalModulesLoaded = true
    return true
  } catch (error) {
    if (!isCurrent()) return false
    const message = resolveErrorMessage(error)
    loadError.value = message
    ElMessage.error(message)
    return false
  }
}

const syncApprovalQuickFilterStateFromQuery = async () => {
  const quickFilter = approvalQuickFilter.value
  if (queryParams.moduleCode) {
    quickFilter.updateState({ fieldKey: 'moduleCode' })
    await nextTick()
    quickFilter.updateState({ operator: 'eq', value: queryParams.moduleCode })
    return
  }
  if (queryParams.keyword.trim()) {
    quickFilter.updateState({ fieldKey: 'keyword' })
    await nextTick()
    quickFilter.updateState({ operator: 'contains', value: queryParams.keyword.trim() })
    return
  }
  quickFilter.updateState({ fieldKey: approvalQuickFilterDefinitions.value[0]?.key || 'moduleCode' })
  await nextTick()
  quickFilter.updateState({ operator: 'eq', value: undefined })
}

const approvalListLoader = createApprovalListLoader<ApprovalCenterQueryParams, ApprovalTaskSummaryVO>({
  context: () => ({
    sessionKey: approvalInvalidation.syncSession().key,
    routeKey: buildApprovalCenterRouteStateKey() || '',
    query: { ...queryParams },
    revision: approvalInvalidation.revision(queryParams.moduleCode, queryParams.viewType)
  }),
  request: (query, isRequestContextCurrent) => {
    return getApprovalTaskPage(query, { ignoreErrorMessage: true, isRequestContextCurrent })
  },
  refreshBadge: () => approvalTodoBadgeStore.refreshTodoTotal(),
  commit: (data, query) => {
    list.value = data.list
    total.value = data.total
    queryParams.pageNo = query.pageNo
  }
})

const getList = async (refreshBadge = false) => {
  const requestRouteStateKey = buildApprovalCenterRouteStateKey()
  if (!requestRouteStateKey) return false
  const sessionKey = approvalInvalidation.syncSession().key
  const generation = ++listUiGeneration
  const isCurrent = () => generation === listUiGeneration && approvalInvalidation.isCurrent(sessionKey) && requestRouteStateKey === buildApprovalCenterRouteStateKey()
  loading.value = true
  loadError.value = ''
  try {
    const result = await approvalListLoader.load(refreshBadge || isUnfilteredTodoQuery())
    if (result === 'loaded' && isCurrent() && requestRouteStateKey) {
      approvalLastLoadedRouteStateKey = requestRouteStateKey
    }
    return result === 'loaded'
  } catch (error) {
    if (!isCurrent()) return false
    const message = resolveErrorMessage(error)
    list.value = []
    total.value = 0
    loadError.value = message
    ElMessage.error(message)
    return false
  } finally {
    if (isCurrent()) loading.value = false
  }
}

const handlePagination = async (payload?: PaginationPayload) => {
  if (typeof payload?.page === 'number') {
    queryParams.pageNo = payload.page
  }
  if (typeof payload?.limit === 'number') {
    queryParams.pageSize = payload.limit
  }
  await getList()
}

const normalizeRouteQueryValue = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : undefined
}

const buildApprovalCenterRouteStateKey = () => {
  if (String(route.name || '') !== approvalCenterRouteInstanceName) {
    return undefined
  }
  return JSON.stringify({
    path: route.path.replace(/\/+$/, ''),
    viewType: normalizeRouteQueryValue(route.query.viewType) || '',
    moduleCode: normalizeRouteQueryValue(route.query.moduleCode) || '',
    keyword: normalizeRouteQueryValue(route.query.keyword) || '',
    taskListRevision: approvalTodoBadgeStore.taskListRevision
  })
}

const shouldKeepApprovalCenterLoadedStateOnRouteReturn = (targetRouteStateKey: string) =>
  approvalModulesLoaded &&
  approvalLastLoadedRouteStateKey === targetRouteStateKey &&
  !loading.value &&
  !loadError.value &&
  !approvalListLoader.isDirty()

const isApprovalTaskViewType = (value: string): value is ApprovalCenterListViewType => {
  return supportedViewTypes.includes(value as ApprovalCenterListViewType)
}

const isApprovalModuleCode = (value: string): value is ApprovalModuleCode => {
  return supportedModuleCodes.includes(value as ApprovalModuleCode)
}

const isApprovalTabName = (value: string): value is ApprovalTabName => {
  return approvalTabNames.includes(value as ApprovalTabName)
}

const resolveRouteViewType = (value: string | undefined): ApprovalCenterListViewType => {
  if (!value) {
    return 'TODO'
  }
  if (!isApprovalTaskViewType(value)) {
    throw new Error(`审批中心不支持视图类型：${value}`)
  }
  return value
}

const resolveRouteModuleCode = (value: string | undefined) => {
  if (!value) {
    return undefined
  }
  if (!isApprovalModuleCode(value)) {
    throw new Error(`审批中心不支持模块来源：${value}`)
  }
  return value
}

const resolveRouteTab = (path: string) => {
  const normalizedPath = path.replace(/\/+$/, '') || '/approval-center'
  if (normalizedPath === '/approval-center') {
    return undefined
  }
  const routeTab = normalizedPath.replace('/approval-center/', '')
  if (!isApprovalTabName(routeTab)) {
    throw new Error(`未知审批中心子页签：${path}`)
  }
  return routeTab
}

const buildApprovalCenterPath = (viewType: ApprovalCenterListViewType) => {
  return approvalTabRoutes[viewTypeToRouteTab[viewType]]
}

const buildApprovalCenterQuery = () => ({
  ...(queryParams.moduleCode ? { moduleCode: queryParams.moduleCode } : {}),
  ...(queryParams.keyword ? { keyword: queryParams.keyword } : {})
})

const syncRouteToCanonicalPath = (viewType: ApprovalCenterListViewType) => {
  const targetPath = buildApprovalCenterPath(viewType)
  const currentPath = route.path.replace(/\/+$/, '')
  const normalizedModuleCode = resolveRouteModuleCode(normalizeRouteQueryValue(route.query.moduleCode))
  const normalizedKeyword = normalizeRouteQueryValue(route.query.keyword) || ''
  const currentQuery = {
    ...(normalizedModuleCode ? { moduleCode: normalizedModuleCode } : {}),
    ...(normalizedKeyword ? { keyword: normalizedKeyword } : {})
  }
  if (
    currentPath === targetPath &&
    (currentQuery.moduleCode || '') === (buildApprovalCenterQuery().moduleCode || '') &&
    (currentQuery.keyword || '') === (buildApprovalCenterQuery().keyword || '')
  ) {
    return
  }
  router.replace({
    path: targetPath,
    query: currentQuery
  })
}

const applyRouteQuery = () => {
  const queryViewType = normalizeRouteQueryValue(route.query.viewType)
  const routeTab = resolveRouteTab(route.path)
  const nextViewType = routeTab ? routeTabToViewType[routeTab] : resolveRouteViewType(queryViewType)
  const nextModuleCode = resolveRouteModuleCode(normalizeRouteQueryValue(route.query.moduleCode))
  const nextKeyword = normalizeRouteQueryValue(route.query.keyword) || ''
  const shouldResetPage =
    queryParams.viewType !== nextViewType ||
    (queryParams.moduleCode || '') !== (nextModuleCode || '') ||
    queryParams.keyword !== nextKeyword
  queryParams.viewType = nextViewType
  queryParams.moduleCode = nextModuleCode
  queryParams.keyword = nextKeyword
  if (shouldResetPage) {
    queryParams.pageNo = 1
  }
}

const loadRouteQuery = async () => {
  const targetRouteStateKey = buildApprovalCenterRouteStateKey()
  if (!targetRouteStateKey) {
    return
  }
  const sessionKey = approvalInvalidation.syncSession().key
  if (shouldKeepApprovalCenterLoadedStateOnRouteReturn(targetRouteStateKey)) {
    return
  }
  const isCurrent = () => approvalInvalidation.isCurrent(sessionKey) && targetRouteStateKey === buildApprovalCenterRouteStateKey()
  try {
    applyRouteQuery()
    syncRouteToCanonicalPath(queryParams.viewType)
    if (!approvalModulesLoaded && !await loadModules()) return
    if (!isCurrent()) return
    await syncApprovalQuickFilterStateFromQuery()
    if (!isCurrent()) return
    await getList()
  } catch (error) {
    if (!isCurrent()) return
    const message = resolveErrorMessage(error)
    list.value = []
    total.value = 0
    loadError.value = message
    ElMessage.error(message)
  }
}

const applyRouteQueryAndLoad = (): Promise<void> => {
  const key = buildApprovalCenterRouteStateKey()
  if (!key) return Promise.resolve()
  if (routeLoadPromise && routeLoadKey === key) return routeLoadPromise
  routeLoadKey = key
  const promise = loadRouteQuery().finally(() => {
    if (routeLoadPromise === promise) { routeLoadPromise = undefined; routeLoadKey = undefined }
  })
  routeLoadPromise = promise
  return promise
}


const handleQuery = () => {
  queryParams.pageNo = 1
  router.push({
    path: buildApprovalCenterPath(queryParams.viewType),
    query: buildApprovalCenterQuery()
  })
}

const approvalQuickFilterControls = {
  TODO: useTableQuickFilter(approvalCenterTableKeys.TODO, approvalQuickFilterDefinitions, queryParams, handleQuery),
  DONE: useTableQuickFilter(approvalCenterTableKeys.DONE, approvalQuickFilterDefinitions, queryParams, handleQuery),
  MY_INITIATED: useTableQuickFilter(
    approvalCenterTableKeys.MY_INITIATED,
    approvalQuickFilterDefinitions,
    queryParams,
    handleQuery
  ),
  CC: useTableQuickFilter(approvalCenterTableKeys.CC, approvalQuickFilterDefinitions, queryParams, handleQuery)
}

const approvalQuickFilter = computed(() => approvalQuickFilterControls[queryParams.viewType])
const approvalQuickFilterState = computed(() => approvalQuickFilter.value.state)
const approvalSelectedFilterDefinition = computed(() => approvalQuickFilter.value.selectedDefinition.value)
const approvalOperatorOptions = computed(() => approvalQuickFilter.value.operatorOptions.value)
const updateApprovalQuickFilterState = (state: Partial<typeof approvalQuickFilter.value.state>) => {
  approvalQuickFilter.value.updateState(state)
}
const applyApprovalQuickFilter = async () => {
  await approvalQuickFilter.value.applyQuickFilter()
}

const BPM_PROCESS_DETAIL_ROUTE = '/bpm/process-instance/detail'
const DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX = '/dcc/controlled-file/detail/'
const DCC_APPROVAL_HANDLING_MODE = 'approval'

const isBpmProcessDetailOnly = (row: ApprovalTaskSummaryVO) => {
  return row.moduleCode === 'BPM' && row.detailRoute === BPM_PROCESS_DETAIL_ROUTE
}

const isDccModuleHandlingAction = (row: ApprovalTaskSummaryVO) => {
  const actions = row.availableActions || []
  return queryParams.viewType === 'TODO'
    && row.moduleCode === 'DCC'
    && !row.businessDeleted
    && actions.includes('PROCESS_IN_MODULE')
}

const resolveDccApprovalDetailLocation = (
  row: ApprovalTaskSummaryVO,
  path: string,
  query: Record<string, string> = {}
) => {
  const normalizedPath = String(path || '').trim()
  const nextQuery = { ...query }
  if (row.sourceTaskType === DCC_OFFLINE_TRAINING_RECORD) return resolveOfflineTrainingRecordLocation(row, normalizedPath, nextQuery)
  const isDccModuleHandling = isDccModuleHandlingAction(row)
  if (row.moduleCode === 'DCC' && normalizedPath.startsWith(DCC_CONTROLLED_FILE_DETAIL_ROUTE_PREFIX)) {
    if (isDccModuleHandling) {
      return {
        path: normalizedPath,
        query: {
          ...nextQuery,
          handling: DCC_APPROVAL_HANDLING_MODE,
          from: nextQuery.from || 'approval-center',
          processInstanceId: nextQuery.processInstanceId || row.processInstanceId || '',
          taskId: nextQuery.taskId || row.sourceTaskId || ''
        }
      }
    }
    return {
      path: normalizedPath,
      query: {
        ...nextQuery,
        viewer: '1',
        from: nextQuery.from || 'approval-center'
      }
    }
  }
  return {
    path: normalizedPath,
    query: nextQuery
  }
}

const resolveDecisionDetailRoute = (row: ApprovalTaskSummaryVO) => {
  if (row.decisionDetailRoute) {
    return row.decisionDetailRoute
  }
  return row.moduleCode !== 'BPM' ? row.detailRoute : undefined
}

const resolveDecisionDetailQuery = (row: ApprovalTaskSummaryVO) => {
  if (row.decisionDetailRoute) {
    return row.decisionDetailQuery || {}
  }
  return row.moduleCode !== 'BPM' ? row.detailQuery || {} : {}
}

const canOpenView = (row: ApprovalTaskSummaryVO) => {
  return !row.businessDeleted && !bpmMissingPermissionReason(row) && Boolean(resolveDecisionDetailRoute(row))
}

const resolveViewDisabledReason = (row: ApprovalTaskSummaryVO) => {
  if (row.businessDeleted) {
    return '审批历史对应业务记录已删除'
  }
  const permissionReason = bpmMissingPermissionReason(row)
  if (permissionReason) return permissionReason
  if (row.moduleCode === 'BPM' && row.businessContextTags?.length) {
    return row.businessContextTags.join('；')
  }
  return '审批任务缺少业务详情入口'
}

const openModuleDetail = (row: ApprovalTaskSummaryVO) => {
  const detailRoute = resolveDecisionDetailRoute(row)
  if (!canOpenView(row) || !detailRoute) {
    ElMessage.error(resolveViewDisabledReason(row))
    return
  }
  router.push(resolveDccApprovalDetailLocation(row, detailRoute, resolveDecisionDetailQuery(row)))
}

const openProcessFlow = (row: ApprovalTaskSummaryVO) => {
  if (row.businessDeleted) {
    ElMessage.error('审批历史对应业务记录已删除')
    return
  }
  if (!row.detailRoute) {
    ElMessage.error('审批任务缺少流程详情入口')
    return
  }
  router.push(resolveDccApprovalDetailLocation(row, row.detailRoute, row.detailQuery || {}))
}

const hasTimelineCapability = (row: ApprovalTaskSummaryVO) => {
  return (row.capabilities || []).includes('TIMELINE')
}

const canOpenFlow = (row: ApprovalTaskSummaryVO) => {
  if (isBpmProcessDetailOnly(row)) {
    return !row.businessDeleted && Boolean(row.detailRoute)
  }
  return !row.businessDeleted && hasTimelineCapability(row)
}

const resolveFlowDisabledReason = (row: ApprovalTaskSummaryVO) => {
  if (row.businessDeleted) {
    return '审批历史对应业务记录已删除'
  }
  if (isBpmProcessDetailOnly(row)) {
    return row.detailRoute ? '' : '审批任务缺少流程详情入口'
  }
  return hasTimelineCapability(row) ? '' : '该模块暂未接入统一审批轨迹'
}

const openTimeline = async (row: ApprovalTaskSummaryVO) => {
  if (isBpmProcessDetailOnly(row)) {
    openProcessFlow(row)
    return
  }
  if (!hasTimelineCapability(row)) {
    const message = '该审批任务未声明统一轨迹能力'
    timelineError.value = message
    ElMessage.error(message)
    return
  }
  timelineTask.value = row
  timelineRows.value = []
  timelineError.value = ''
  timelineDrawerVisible.value = true
  timelineLoading.value = true
  const generation = ++timelineGeneration
  const sessionKey = approvalInvalidation.syncSession().key
  const requestRouteKey = buildApprovalCenterRouteStateKey()
  const isCurrent = () => generation === timelineGeneration && approvalInvalidation.isCurrent(sessionKey) && requestRouteKey === buildApprovalCenterRouteStateKey() && timelineDrawerVisible.value
  try {
    const data = await getApprovalTaskTimeline({
      moduleCode: row.moduleCode,
      sourceTaskType: row.sourceTaskType,
      sourceTaskId: row.sourceTaskId,
      businessKey: row.businessKey,
      processInstanceId: row.processInstanceId
    }, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    if (!data || data.length === 0) {
    const message = '审批流程记录为空，请检查模块适配器流程实现'
      timelineError.value = message
      ElMessage.error(message)
      return
    }
    timelineRows.value = data
  } catch (error) {
    if (!isCurrent()) return
    const message = resolveErrorMessage(error)
    timelineError.value = message
    ElMessage.error(message)
  } finally {
    if (isCurrent()) timelineLoading.value = false
  }
}

const canReview = (row: ApprovalTaskSummaryVO) => {
  const actions = row.availableActions || []
  return queryParams.viewType === 'TODO'
    && !row.businessDeleted
    && !bpmMissingPermissionReason(row)
    && actions.includes('APPROVE')
    && (row.moduleCode === 'EDHR' || actions.includes('REJECT'))
}

const canReviewInModule = (row: ApprovalTaskSummaryVO) => {
  const actions = row.availableActions || []
  return queryParams.viewType === 'TODO'
    && !row.businessDeleted
    && !bpmMissingPermissionReason(row)
    && (actions.includes('REVIEW_IN_MODULE') || actions.includes('APPROVE_IN_MODULE')
      || (row.moduleCode === 'EDHR' && row.sourceTaskType === 'ACTIVE_ORDER_HANDOFF'
        && actions.includes('PROCESS_IN_MODULE') && canOpenView(row))
      || canProcessBpmInModule(row, queryParams.viewType))
}

const canReviewAction = (row: ApprovalTaskSummaryVO) => {
  return canReview(row) || canReviewInModule(row)
}

const openReviewAction = (row: ApprovalTaskSummaryVO) => {
  if (canReview(row)) {
    openReviewDialog(row)
    return
  }
  if (canReviewInModule(row)) {
    openDecisionDetail(row)
    return
  }
  ElMessage.error('该审批任务暂不支持审核')
}

const openDecisionDetail = (row: ApprovalTaskSummaryVO) => {
  if (row.moduleCode === 'BPM') {
    const location = bpmModuleDecisionLocation(row, queryParams.viewType)
    if (!location) {
      ElMessage.error(resolveViewDisabledReason(row))
      return
    }
    router.push(location)
    return
  }
  const detailRoute = resolveDecisionDetailRoute(row)
  if (!canOpenView(row) || !detailRoute) {
    ElMessage.error(resolveViewDisabledReason(row))
    return
  }
  router.push(resolveDccApprovalDetailLocation(row, detailRoute, resolveDecisionDetailQuery(row)))
}

const resetReviewRequestContext = () => {
  reviewGeneration++
  reviewSubmitting.value = false
  reviewForm.signaturePassword = ''
}

watch(reviewDialogVisible, (visible) => {
  if (!visible) resetReviewRequestContext()
}, { flush: 'sync' })

const openReviewDialog = (row: ApprovalTaskSummaryVO) => {
  if (!canReview(row)) {
    ElMessage.error('该审批任务暂不支持在审批中心直接审核')
    return
  }
  resetReviewRequestContext()
  reviewTask.value = { ...row }
  reviewForm.result = 'APPROVE'
  reviewForm.reason = ''
  reviewForm.signaturePassword = ''
  reviewDialogVisible.value = true
  reviewPreview.value = undefined
  reviewIdempotencyKey.value = crypto.randomUUID()
  if (row.moduleCode === 'EDHR') void loadReviewPreview()
}

const loadReviewPreview = async () => {
  const task = reviewTask.value
  if (!task || task.moduleCode !== 'EDHR' || !task.sourceTaskId || reviewSubmitting.value) return
  const generation = reviewGeneration
  reviewPreview.value = undefined
  reviewPreviewError.value = ''
  reviewForm.signaturePassword = ''
  reviewPreviewLoading.value = true
  try {
    const preview = await getApprovalReviewPreview({ moduleCode: task.moduleCode, sourceTaskType: task.sourceTaskType, sourceTaskId: task.sourceTaskId, processInstanceId: task.processInstanceId })
    if (generation !== reviewGeneration || !reviewDialogVisible.value) return
    if (!preview.prepareId || !preview.content.canonicalBusinessContentJson || !preview.content.allowedActions.includes('APPROVE')) throw new Error('审批正文响应不完整')
    JSON.parse(preview.content.canonicalBusinessContentJson)
    reviewPreview.value = preview
    reviewIdempotencyKey.value = crypto.randomUUID()
  } catch (error) {
    if (generation === reviewGeneration) reviewPreviewError.value = resolveErrorMessage(error)
  } finally {
    if (generation === reviewGeneration) reviewPreviewLoading.value = false
  }
}

const submitReview = async () => {
  if (reviewSubmitting.value) return
  if (!reviewTask.value) {
    ElMessage.error('审批任务上下文缺失')
    return
  }
  if (!canReview(reviewTask.value) || !reviewTask.value.availableActions?.includes(reviewForm.result)) {
    ElMessage.error('该审批任务不支持所选审核动作，请刷新任务')
    return
  }
  if (!reviewForm.reason.trim()) {
    ElMessage.error(reviewForm.result === 'REJECT' ? '审核不通过必须填写原因' : '审核通过必须填写审批意见')
    return
  }
  if (!reviewForm.signaturePassword.trim()) {
    ElMessage.error('请输入电子签名密码')
    return
  }
  const sourceTaskId = reviewTask.value.sourceTaskId
  if (typeof sourceTaskId !== 'string' || sourceTaskId.trim().length === 0) {
    ElMessage.error('审批任务缺少正式来源任务 ID，请刷新后核对')
    return
  }
  const task = Object.freeze({ ...reviewTask.value, sourceTaskId })
  if (task.moduleCode === 'EDHR' && (!reviewPreview.value || !reviewPreview.value.content.allowedActions.includes(reviewForm.result))) {
    ElMessage.error('请先读取完整审批正文并核对可办理动作')
    return
  }
  const payload = {
    moduleCode: task.moduleCode,
    sourceTaskType: task.sourceTaskType,
    sourceTaskId: task.sourceTaskId,
    businessKey: task.businessKey,
    processInstanceId: task.processInstanceId,
    result: reviewForm.result,
    reason: reviewForm.reason.trim(),
    signaturePassword: reviewForm.signaturePassword.trim(),
    ...(task.moduleCode === 'EDHR' && reviewPreview.value ? {
      prepareId: reviewPreview.value.prepareId,
      idempotencyKey: reviewIdempotencyKey.value,
      expectedSubjectVersion: reviewPreview.value.content.subjectVersion,
      expectedContentHash: reviewPreview.value.content.contentHash,
      contentPolicyVersion: reviewPreview.value.content.contentPolicyVersion
    } : {})
  }
  const sessionKey = approvalInvalidation.syncSession().key
  const event = task.moduleCode === 'EDHR' ? approvalInvalidation.captureEdhrSuccess(task) : undefined
  const requestRouteKey = buildApprovalCenterRouteStateKey()
  const generation = reviewGeneration
  const taskIdentity = approvalTaskIdentityKey(task)
  const isPageCurrent = () => approvalInvalidation.isCurrent(sessionKey) && requestRouteKey === buildApprovalCenterRouteStateKey()
  const isCurrent = () => generation === reviewGeneration && isPageCurrent() && Boolean(reviewTask.value && reviewTask.value.sourceTaskId === task.sourceTaskId && approvalTaskIdentityKey({ ...reviewTask.value, sourceTaskId: task.sourceTaskId }) === taskIdentity)
  reviewSubmitting.value = true
  try {
    await completeApprovalWrite({
      write: async () => {
        const result = await reviewApprovalTask(payload, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent , isSuccessReceiptContextCurrent: (() => { const receiptSessionKey = approvalInvalidation.syncSession().key; return () => approvalInvalidation.isCurrent(receiptSessionKey) })()})
        if (task.moduleCode === 'EDHR') {
          if (!result || typeof result !== 'object' || result.idempotencyKey !== payload.idempotencyKey || result.prepareId !== payload.prepareId || !result.receiptId || !result.signatureId) throw new Error('审批回执未确认办理成功，请保留当前请求并核对正式结果')
        } else if (result !== true) throw new Error('审批接口未确认办理成功，请核对正式结果')
        return result
      },
      publish: () => { if (event) approvalInvalidation.publishEdhrSuccess(event) },
      success: () => {
        if (!isCurrent()) return
        reviewForm.signaturePassword = ''
        ElMessage.success(payload.result === 'APPROVE' ? '审核已通过' : '审核不通过已提交')
        reviewDialogVisible.value = false
      },
      refresh: async () => {
        if (!isPageCurrent()) return
        if (!await getList(true)) throw new Error(loadError.value || '当前页面已切换，请返回审批中心刷新')
      },
      refreshFailed: (error) => { if (isPageCurrent()) ElMessage.warning(`办理已成功，刷新失败：${resolveErrorMessage(error)}`) }
    })
  } catch (error) {
    if (isCurrent()) {
      const message = resolveErrorMessage(error)
      if (/STALE_CONTENT|APPROVAL_PREPARE_EXPIRED/.test(message)) {
        reviewForm.signaturePassword = ''
        reviewPreview.value = undefined
        reviewPreviewError.value = '审批内容已变化，请重新预览并确认'
      }
      ElMessage.error(message)
    }
  } finally {
    if (isCurrent()) reviewSubmitting.value = false
  }
}

const normalizeApprovalDisplayText = (value?: string | number | null) => {
  return value === undefined || value === null ? '' : String(value).trim()
}

const isApprovalDisplayValueVisible = (value?: string | number | null) => {
  const text = normalizeApprovalDisplayText(value)
  if (!text || EMPTY_CONTEXT_VALUE_PATTERN.test(text)) {
    return false
  }
  return !text.startsWith('未配置') && !text.endsWith('已配置')
}

const containsEnglishLetters = (value: string) => ENGLISH_LETTER_PATTERN.test(value)

const containsChineseCharacters = (value: string) => CHINESE_CHARACTER_PATTERN.test(value)

const isBusinessContextCode = (value: string) => BUSINESS_CONTEXT_CODE_PATTERN.test(value)

const resolveMappedApprovalText = (
  value: string | undefined | null,
  labels: Record<string, string>,
  unmappedLabel: string
) => {
  const text = normalizeApprovalDisplayText(value)
  if (!text) {
    return EMPTY_APPROVAL_DISPLAY
  }
  const knownLabel = labels[text]
  if (knownLabel) {
    return knownLabel
  }
  return containsEnglishLetters(text) ? unmappedLabel : text
}

const resolveModuleName = (moduleCode: ApprovalModuleCode) => {
  const moduleLabel = APPROVAL_MODULE_LABELS[moduleCode]
  if (moduleLabel) {
    return moduleLabel
  }
  const serverModuleName = modules.value.find((item) => item.moduleCode === moduleCode)?.moduleName
  return resolveMappedApprovalText(serverModuleName, APPROVAL_MODULE_LABELS, '未配置中文模块')
}

const resolveSourceTaskTypeLabel = (row: ApprovalTaskSummaryVO) => {
  return resolveMappedApprovalText(row.sourceTaskType, APPROVAL_SOURCE_TASK_TYPE_LABELS, '未配置中文任务来源')
}

const resolveBusinessTitleLabel = (row: ApprovalTaskSummaryVO | undefined) => {
  const rawTitle = normalizeApprovalDisplayText(row?.businessTitle)
  if (!rawTitle) {
    return EMPTY_APPROVAL_DISPLAY
  }
  const mappedTitle = APPROVAL_BUSINESS_TITLE_LABELS[rawTitle]
  if (mappedTitle) {
    return mappedTitle
  }
  const replacedTitle = Object.entries(APPROVAL_BUSINESS_TITLE_LABELS).reduce(
    (result, [englishTitle, chineseTitle]) => result.replaceAll(englishTitle, chineseTitle),
    rawTitle
  )
  return replacedTitle
}

const resolveBusinessIdentifierValueLabel = (row: ApprovalTaskSummaryVO) => {
  const businessCode = normalizeApprovalDisplayText(row.businessCode)
  if (businessCode) {
    return businessCode
  }
  const businessKey = normalizeApprovalDisplayText(row.businessKey)
  if (businessKey) {
    const [prefix] = businessKey.split(':')
    const prefixLabel = APPROVAL_BUSINESS_KEY_PREFIX_LABELS[prefix]
    if (prefixLabel) {
      return `${prefixLabel}实例`
    }
    return containsEnglishLetters(businessKey) ? '业务键已配置' : businessKey
  }
  const sourceTaskId = normalizeApprovalDisplayText(row.sourceTaskId)
  return containsEnglishLetters(sourceTaskId) ? '任务编号已配置' : sourceTaskId || EMPTY_APPROVAL_DISPLAY
}

const resolveBusinessIdentifierLabel = (row: ApprovalTaskSummaryVO) => {
  const businessCode = normalizeApprovalDisplayText(row.businessCode)
  if (businessCode) {
    return `业务编号：${businessCode}`
  }
  return resolveBusinessIdentifierValueLabel(row)
}

const resolveNodeNameLabel = (row: ApprovalTaskSummaryVO) => {
  const nodeName = normalizeApprovalDisplayText(row.currentNodeName)
  if (nodeName && !containsEnglishLetters(nodeName)) {
    return nodeName
  }
  const mappedNodeName = APPROVAL_NODE_LABELS[nodeName]
  if (mappedNodeName) {
    return mappedNodeName
  }
  return resolveMappedApprovalText(row.currentNodeCode, APPROVAL_NODE_LABELS, '未配置中文节点')
}

const resolveReviewerLabel = (row: ApprovalTaskSummaryVO) =>
  row.moduleCode === 'EDHR' && queryParams.viewType !== 'TODO'
    ? `${edhrReadDisplay(row).actorName}；${edhrReadDisplay(row).actorIdentity}`
    : row.assigneeRoleName
    ? `审批角色：${row.assigneeRoleName}`
    : row.assigneeUserName || (row.assigneeUserId ? `用户 #${row.assigneeUserId}` : EMPTY_APPROVAL_DISPLAY)

const resolveApplicantLabel = (row: ApprovalTaskSummaryVO) =>
  row.moduleCode === 'EDHR' && queryParams.viewType !== 'TODO'
    ? '姓名未记录'
    : row.initiatorUserName || (row.initiatorUserId ? `用户 #${row.initiatorUserId}` : EMPTY_APPROVAL_DISPLAY)

const resolveBusinessContextValueLabel = (value: string, missingLabel: string) => {
  const text = normalizeApprovalDisplayText(value)
  if (!text) {
    return EMPTY_APPROVAL_DISPLAY
  }
  const normalizedVersion = text.match(/^v(\d+(?:\.\d+)?)$/i)
  if (normalizedVersion) {
    return normalizedVersion[1]
  }
  if (/^[A-Z]\/[1-9]\d*(?:-[1-9]\d*)?$/.test(text)) return text
  const mappedStatus = APPROVAL_STATUS_LABELS[text] || APPROVAL_NODE_LABELS[text] || APPROVAL_BUSINESS_TITLE_LABELS[text]
  if (mappedStatus) {
    return mappedStatus
  }
  const replacedText = Object.entries(APPROVAL_BUSINESS_TITLE_LABELS).reduce(
    (result, [englishTitle, chineseTitle]) => result.replaceAll(englishTitle, chineseTitle),
    text
  )
  return containsEnglishLetters(replacedText)
    && !containsChineseCharacters(replacedText)
    && !isBusinessContextCode(replacedText)
    ? missingLabel
    : replacedText
}

const normalizeDccContextTag = (tag: string) => tag.replace(/^[^:：]+[:：]\s*/, '').trim()

const isBusinessContextTagVisible = (tag: string) => {
  const text = normalizeApprovalDisplayText(tag)
  if (!text) {
    return false
  }
  const parts = text.match(/^([^:：]+)([:：])\s*(.*)$/)
  const value = parts ? parts[3] : text
  const label = resolveBusinessContextValueLabel(value, '')
  return isApprovalDisplayValueVisible(label)
}

const resolveVisibleBusinessContextTags = (row: ApprovalTaskSummaryVO) =>
  (row.businessContextTags || []).filter(isBusinessContextTagVisible)

const resolveBusinessContextTagLabel = (tag: string) => {
  const text = normalizeApprovalDisplayText(tag)
  if (!text) {
    return EMPTY_APPROVAL_DISPLAY
  }
  const parts = text.match(/^([^:：]+)([:：])\s*(.*)$/)
  if (!parts) {
    return resolveBusinessContextValueLabel(text, '未配置中文标签')
  }
  const [, label, separator, value] = parts
  return `${label}${separator}${resolveBusinessContextValueLabel(value, '未配置中文值')}`
}

const findDccContextTagValue = (row: ApprovalTaskSummaryVO, patterns: RegExp[]) => {
  const tags = row.businessContextTags || []
  const matchedTag = tags.find((tag) => patterns.some((pattern) => pattern.test(tag)))
  return matchedTag
    ? resolveBusinessContextValueLabel(normalizeDccContextTag(matchedTag), '未配置中文值')
    : EMPTY_APPROVAL_DISPLAY
}

const resolveDccKeyFields = (row: ApprovalTaskSummaryVO) => {
  if (row.sourceTaskType === 'DCC_PROJECT_PRODUCT_REVIEW' || row.sourceTaskType === 'DCC_PROJECT_PRODUCT_APPROVAL') {
    return [
      { label: '项目代码', value: normalizeApprovalDisplayText(row.businessCode) || EMPTY_APPROVAL_DISPLAY },
      { label: '当前审批节点', value: resolveNodeNameLabel(row) }
    ]
  }
  return [
    {
      label: '文件编号',
      value: resolveBusinessIdentifierValueLabel(row)
    },
    {
      label: '版本',
      value: findDccContextTagValue(row, [/版本|version/i, /^v\d+(?:\.\d+)?$/i])
    },
    {
      label: '文件类型',
      value: findDccContextTagValue(row, [/文件类型|文件分类|类型|分类/])
    },
    {
      label: '当前审批节点',
      value: resolveNodeNameLabel(row)
    }
  ]
}

const resolveVisibleDccKeyFields = (row: ApprovalTaskSummaryVO) =>
  row.moduleCode === 'DCC'
    ? resolveDccKeyFields(row).filter((field) => isApprovalDisplayValueVisible(field.value))
    : []

const resolveNodeSubLabel = (row: ApprovalTaskSummaryVO) => {
  if (row.moduleCode === 'EDHR' && queryParams.viewType !== 'TODO') return '正式决定人见审核人列'
  const reviewerLabel = resolveReviewerLabel(row)
  return reviewerLabel !== EMPTY_APPROVAL_DISPLAY
    ? `审核人：${reviewerLabel}`
    : resolveMappedApprovalText(row.businessStatus, APPROVAL_STATUS_LABELS, '未配置中文状态')
}

const resolveCapabilityLabel = (capability: ApprovalTaskCapability) => {
  const labels: Record<ApprovalTaskCapability, string> = {
    TIMELINE: '轨迹',
    NOTIFICATION: '通知',
    REMINDER: '催办',
    AUDIT: '审计',
    SIGNATURE_AUTHORIZATION: '签名授权',
    EVIDENCE_LEDGER: '证据账本'
  }
  return labels[capability] || capability
}

const resolveTimelineType = (status?: string) => {
  if (status === 'APPROVED' || status === 'DONE') {
    return 'success'
  }
  if (status === 'REJECTED' || status === 'CANCELED') {
    return 'danger'
  }
  if (status === 'RUNNING' || status === 'PENDING') {
    return 'primary'
  }
  return 'info'
}

const resolveApprovalResultLabel = (result?: ApprovalTaskReviewResult) => {
  const labels: Record<ApprovalTaskReviewResult, string> = {
    APPROVE: '通过',
    REJECT: '驳回'
  }
  return result ? labels[result] : ''
}

const resolveApprovalResultTagType = (result?: ApprovalTaskReviewResult) => {
  const types: Record<ApprovalTaskReviewResult, 'success' | 'danger'> = {
    APPROVE: 'success',
    REJECT: 'danger'
  }
  return result ? types[result] : 'success'
}

const resolveApprovalRemark = (row: ApprovalTaskSummaryVO) => {
  return row.approvalResult === 'REJECT' ? row.approvalRemark || '--' : ''
}

const formatApprovalTime = (value?: string | number | Date | null) => {
  if (!value) {
    return '--'
  }
  const date = normalizeApprovalTime(value)
  if (!date || Number.isNaN(date.getTime())) {
    return `时间格式异常：${String(value)}`
  }
  return formatDate(date, 'YYYY-MM-DD HH:mm:ss')
}

const normalizeApprovalTime = (value: string | number | Date) => {
  if (value instanceof Date) {
    return value
  }
  if (typeof value === 'number') {
    return new Date(value)
  }
  const trimmed = value.trim()
  if (!trimmed) {
    return undefined
  }
  if (/^\d+$/.test(trimmed)) {
    return new Date(Number(trimmed))
  }
  return new Date(trimmed)
}

const formatEdhrTime = (value?: string) => value ? formatApprovalTime(value) : '未记录'

const formatTimelineTime = (value?: string) => {
  return value ? formatApprovalTime(value) : ''
}

const resolveErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string') {
    return error
  }
  return '审批中心请求失败'
}

onMounted(() => { void applyRouteQueryAndLoad() })
onActivated(() => { void applyRouteQueryAndLoad() })
onDeactivated(() => {
  listUiGeneration++
  approvalListLoader.invalidate()
  timelineGeneration++
  timelineLoading.value = false
  reviewDialogVisible.value = false
  resetReviewRequestContext()
  routeLoadPromise = undefined
  routeLoadKey = undefined
  loading.value = false
})
onBeforeUnmount(() => { listUiGeneration++; timelineGeneration++; approvalListLoader.invalidate() })
watch(() => approvalInvalidation.contextKey, () => {
  listUiGeneration++
  approvalListLoader.reset()
  timelineGeneration++
  timelineDrawerVisible.value = false
  timelineRows.value = []
  timelineTask.value = undefined
  timelineLoading.value = false
  approvalLastLoadedRouteStateKey = undefined
  approvalModulesLoaded = false
  modules.value = []
  list.value = []
  total.value = 0
  loading.value = false
  loadError.value = ''
  reviewDialogVisible.value = false
  resetReviewRequestContext()
  routeLoadPromise = undefined
  routeLoadKey = undefined
}, { flush: 'sync' })

watch(
  () => [route.path, route.query],
  () => {
    reviewDialogVisible.value = false
    resetReviewRequestContext()
    reviewTask.value = undefined
    applyRouteQueryAndLoad()
  }
)
</script>

<style scoped>
.approval-center {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-center :deep(.unified-list-template__query-form) {
  flex-wrap: nowrap;
}

.approval-center :deep(.unified-list-template__multi-filter) {
  flex: 1 1 0;
  min-width: 0;
}

.approval-center :deep(.unified-list-template__toolbar-actions) {
  margin-left: 0;
}

.approval-center__filters {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: -12px;
}

.approval-center__error {
  margin-bottom: 2px;
}

.approval-center__table-scope,
.approval-center__table {
  width: 100%;
}

.approval-center__primary {
  color: #1f2937;
  font-weight: 600;
}

.approval-center__primary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.approval-center__primary-row .approval-center__primary {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-center__muted {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.approval-center__dcc-context {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.approval-center__dcc-key-fields {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin-top: 6px;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
}

.approval-center__stored-text {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.approval-center__capabilities {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
}

.approval-center__remark {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.approval-center__applicant,
.approval-center__reviewer {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #263247;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.approval-center__timeline-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.approval-center__timeline-title {
  min-width: 0;
  overflow: hidden;
  color: #1f2937;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-center__timeline-body {
  min-height: 220px;
}

.approval-center__review-summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.approval-center__review-summary-main {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #172033;
  font-weight: 600;
}

.approval-center__review-summary-main span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-center__review-summary-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.approval-center__review-form :deep(.el-radio-group) {
  display: flex;
  width: 100%;
}

.approval-center__review-form :deep(.el-radio-button) {
  flex: 1;
}

.approval-center__review-form :deep(.el-radio-button__inner) {
  width: 100%;
}

.approval-center__timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.approval-center__timeline-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #1f2937;
  font-weight: 600;
}

.approval-center__timeline-comment {
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  line-height: 20px;
}

.approval-center__timeline-drawer :deep(.el-drawer__body) {
  padding: 16px 20px 20px;
}
</style>
