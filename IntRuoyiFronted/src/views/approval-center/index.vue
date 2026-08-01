<template>
  <ContentWrap>
    <div class="approval-center">
      <div class="approval-center__toolbar">
        <div class="approval-center__title-group">
          <h3 class="approval-center__title">审批中心</h3>
          <div class="approval-center__meta">
            <el-tag size="small" type="info">{{ modules.length }} 个模块</el-tag>
            <span>{{ activeTabLabel }}</span>
          </div>
        </div>
      </div>

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
              border
              stripe
              :show-overflow-tooltip="true"
              empty-text="暂无审批任务"
              @header-dragend="handleApprovalHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                v-if="isApprovalColumnVisible('source')"
                label="来源"
                prop="source"
                :width="getApprovalColumnWidthString('source', 150)"
                v-bind="sortColumnAttrs('source')"
              >
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ resolveModuleName(row.moduleCode) }}</el-tag>
                  <div class="approval-center__muted">{{ row.sourceTaskType }}</div>
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
                    <span class="approval-center__primary">{{ row.businessTitle || '--' }}</span>
                    <el-tag v-if="row.businessDeleted" size="small" type="danger" effect="plain">已删除</el-tag>
                  </div>
                  <div class="approval-center__muted">{{ row.businessCode || row.businessKey || row.sourceTaskId || '--' }}</div>
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
                  <div>{{ row.currentNodeName || row.currentNodeCode || '--' }}</div>
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
                  <el-tag
                    v-if="row.approvalResult"
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
                  <span class="approval-center__remark" :title="resolveApprovalRemark(row)">
                    {{ resolveApprovalRemark(row) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('capabilities')"
                label="能力"
                prop="capabilities"
                :width="getApprovalColumnWidthString('capabilities', 260)"
                v-bind="sortColumnAttrs('capabilities')"
              >
                <template #default="{ row }">
                  <el-tag
                    v-if="row.requiresSignature"
                    size="small"
                    type="warning"
                    class="mr-6px"
                  >
                    签名
                  </el-tag>
                  <el-tag
                    v-for="capability in row.capabilities || []"
                    :key="capability"
                    size="small"
                    class="mr-6px mb-4px"
                  >
                    {{ resolveCapabilityLabel(capability) }}
                  </el-tag>
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
                  <div>{{ formatApprovalTime(row.taskCreatedAt || row.initiatedAt) }}</div>
                  <div v-if="row.taskCompletedAt" class="approval-center__muted">{{ formatApprovalTime(row.taskCompletedAt) }}</div>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isApprovalColumnVisible('actions')"
                label="操作"
                fixed="right"
                prop="actions"
                :width="getApprovalColumnWidthString('actions', 230)"
              >
                <template #default="{ row }">
                  <el-button
                    v-if="canReview(row)"
                    link
                    type="primary"
                    :disabled="row.businessDeleted"
                    @click="openReviewDialog(row)"
                  >
                    审核
                  </el-button>
                  <el-tooltip
                    :disabled="canOpenDecisionDetail(row)"
                    :content="resolveDecisionDetailDisabledReason(row)"
                    placement="top"
                  >
                    <span>
                      <el-button
                        link
                        type="primary"
                        :disabled="!canOpenDecisionDetail(row)"
                        @click="openDecisionDetail(row)"
                      >
                        {{ resolveDecisionActionLabel(row) }}
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-button link type="primary" :disabled="row.businessDeleted" @click="openModuleDetail(row)">
                    {{ resolveModuleOpenLabel(row) }}
                  </el-button>
                  <el-tooltip
                    :content="hasTimelineCapability(row) ? '查看统一审批轨迹' : '该模块暂未接入统一轨迹'"
                    placement="top"
                  >
                    <span>
                      <el-button
                        link
                        type="primary"
                        :disabled="!hasTimelineCapability(row)"
                        @click="openTimeline(row)"
                      >
                        轨迹
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
      title="审批轨迹"
      size="560px"
      append-to-body
      destroy-on-close
      class="approval-center__timeline-drawer"
    >
      <div v-if="timelineTask" class="approval-center__timeline-summary">
        <el-tag size="small" type="info">{{ resolveModuleName(timelineTask.moduleCode) }}</el-tag>
        <span class="approval-center__timeline-title">{{ timelineTask.businessTitle || '--' }}</span>
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
          description="暂无审批轨迹"
        />
        <el-timeline v-else>
          <el-timeline-item
            v-for="entry in timelineRows"
            :key="entry.id"
            :timestamp="formatTimelineTime(entry.actedAt)"
            :type="resolveTimelineType(entry.status)"
            placement="top"
          >
            <div class="approval-center__timeline-item">
              <div class="approval-center__timeline-node">
                <span>{{ entry.nodeName || entry.nodeCode || '--' }}</span>
                <el-tag size="small" effect="plain">{{ entry.actionLabel || entry.status || '--' }}</el-tag>
              </div>
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
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <el-dialog
      v-model="reviewDialogVisible"
      title="审核确认"
      width="560px"
      append-to-body
      destroy-on-close
      class="approval-center__review-dialog"
    >
      <div v-if="reviewTask" class="approval-center__review-summary-card">
        <div class="approval-center__review-summary-main">
          <el-tag size="small" type="info">{{ resolveModuleName(reviewTask.moduleCode) }}</el-tag>
          <span>{{ reviewTask.businessTitle || '--' }}</span>
        </div>
        <div class="approval-center__review-summary-hint">
          {{ reviewForm.result === 'APPROVE' ? '审核通过需完成电子签名确认。' : '审核不通过需填写原因并完成电子签名确认。' }}
        </div>
      </div>
      <el-form label-width="96px" class="approval-center__review-form">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="reviewForm.result">
            <el-radio-button label="APPROVE">审核通过</el-radio-button>
            <el-radio-button label="REJECT">审核不通过</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reviewForm.result === 'REJECT'" label="不通过原因" required>
          <el-input
            v-model="reviewForm.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="选择审核不通过时必须填写原因"
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
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">
          确认审核
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ElMessage } from 'element-plus'
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

defineOptions({ name: 'ApprovalCenterWorkbench' })

const router = useRouter()
const route = useRoute()

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
const reviewSubmitting = ref(false)
const reviewTask = ref<ApprovalTaskSummaryVO>()
const approvalTodoBadgeStore = useApprovalTodoBadgeStore()
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
  TODO: 'approval.center.todo',
  DONE: 'approval.center.done',
  MY_INITIATED: 'approval.center.myInitiated',
  CC: 'approval.center.cc'
}

const approvalDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'source', label: '来源', width: 150 },
  { key: 'businessSummary', label: '业务摘要', width: 300 },
  { key: 'node', label: '节点', width: 190 },
  { key: 'reviewer', label: '审核人', width: 140 },
  { key: 'approvalResult', label: '审批结果', width: 120 },
  { key: 'approvalRemark', label: '备注', minWidth: 220 },
  { key: 'capabilities', label: '能力', width: 260 },
  { key: 'time', label: '时间', width: 190 },
  { key: 'actions', label: '操作', width: 230, hideable: false, business: false }
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
    label: '模块',
    type: 'select',
    queryParamKey: 'moduleCode',
    options: modules.value.map((item) => ({
      label: item.moduleName,
      value: item.moduleCode
    })),
    placeholder: '选择模块'
  },
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '标题、编号或批次'
  }
])


const activeTabLabel = computed(() => {
  return viewTabs.find((item) => item.value === queryParams.viewType)?.label || '待办'
})

const isUnfilteredTodoQuery = () =>
  queryParams.viewType === 'TODO' && !queryParams.moduleCode && !queryParams.keyword.trim()

const loadModules = async () => {
  try {
    modules.value = await getApprovalCenterModules()
  } catch (error) {
    const message = resolveErrorMessage(error)
    loadError.value = message
    ElMessage.error(message)
  }
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getApprovalTaskPage(queryParams)
    list.value = data.list || []
    total.value = data.total || 0
    if (isUnfilteredTodoQuery()) {
      approvalTodoBadgeStore.applyTodoTotal(data.total || 0)
    }
  } catch (error) {
    const message = resolveErrorMessage(error)
    list.value = []
    total.value = 0
    loadError.value = message
    ElMessage.error(message)
  } finally {
    loading.value = false
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

const refreshAll = async () => {
  await loadModules()
  await getList()
}

const normalizeRouteQueryValue = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' && rawValue.trim() ? rawValue.trim() : undefined
}

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

const applyRouteQueryAndLoad = async () => {
  try {
    applyRouteQuery()
    syncRouteToCanonicalPath(queryParams.viewType)
    await getList()
  } catch (error) {
    const message = resolveErrorMessage(error)
    list.value = []
    total.value = 0
    loadError.value = message
    ElMessage.error(message)
  }
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
  return row.decisionDetailRoute || row.detailRoute
}

const resolveDecisionDetailQuery = (row: ApprovalTaskSummaryVO) => {
  if (row.decisionDetailRoute) {
    return row.decisionDetailQuery || {}
  }
  return row.detailQuery || {}
}

const canOpenDecisionDetail = (row: ApprovalTaskSummaryVO) => {
  return !row.businessDeleted && Boolean(resolveDecisionDetailRoute(row))
}

const resolveDecisionActionLabel = (row: ApprovalTaskSummaryVO) => {
  const actions = row.availableActions || []
  if (canReview(row)) {
    return '详情'
  }
  if (queryParams.viewType === 'TODO' && actions.includes('REVIEW_IN_MODULE')) {
    return '审核'
  }
  if (queryParams.viewType === 'TODO' && actions.includes('APPROVE_IN_MODULE')) {
    return '批准'
  }
  if (queryParams.viewType === 'TODO' && actions.includes('PROCESS_IN_MODULE')) {
    return '处理'
  }
  return '详情'
}

const resolveDecisionDetailDisabledReason = (row: ApprovalTaskSummaryVO) => {
  if (row.businessDeleted) {
    return '审批历史对应业务记录已删除'
  }
  return '审批任务缺少业务详情入口'
}

const openDecisionDetail = (row: ApprovalTaskSummaryVO) => {
  if (!canOpenDecisionDetail(row)) {
    ElMessage.error(resolveDecisionDetailDisabledReason(row))
    return
  }
  router.push(resolveDccApprovalDetailLocation(row, resolveDecisionDetailRoute(row), resolveDecisionDetailQuery(row)))
}

const resolveModuleOpenLabel = (row: ApprovalTaskSummaryVO) => {
  return isBpmProcessDetailOnly(row) ? '流程' : '打开'
}

const openModuleDetail = (row: ApprovalTaskSummaryVO) => {
  if (row.businessDeleted) {
    ElMessage.error('审批历史对应业务记录已删除')
    return
  }
  if (!row.detailRoute) {
    ElMessage.error('审批任务缺少模块详情入口')
    return
  }
  router.push(resolveDccApprovalDetailLocation(row, row.detailRoute, row.detailQuery || {}))
}

const openTimeline = async (row: ApprovalTaskSummaryVO) => {
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
  try {
    const data = await getApprovalTaskTimeline({
      moduleCode: row.moduleCode,
      sourceTaskType: row.sourceTaskType,
      sourceTaskId: row.sourceTaskId,
      businessKey: row.businessKey,
      processInstanceId: row.processInstanceId
    })
    if (!data || data.length === 0) {
      const message = '审批轨迹为空，请检查模块 adapter 轨迹实现'
      timelineError.value = message
      ElMessage.error(message)
      return
    }
    timelineRows.value = data
  } catch (error) {
    const message = resolveErrorMessage(error)
    timelineError.value = message
    ElMessage.error(message)
  } finally {
    timelineLoading.value = false
  }
}

const hasTimelineCapability = (row: ApprovalTaskSummaryVO) => {
  return (row.capabilities || []).includes('TIMELINE')
}

const canReview = (row: ApprovalTaskSummaryVO) => {
  const actions = row.availableActions || []
  return queryParams.viewType === 'TODO'
    && !row.businessDeleted
    && actions.includes('APPROVE')
    && actions.includes('REJECT')
}

const openReviewDialog = (row: ApprovalTaskSummaryVO) => {
  if (!canReview(row)) {
    ElMessage.error('该审批任务暂不支持在审批中心直接审核')
    return
  }
  reviewTask.value = row
  reviewForm.result = 'APPROVE'
  reviewForm.reason = ''
  reviewForm.signaturePassword = ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  if (!reviewTask.value) {
    ElMessage.error('审批任务上下文缺失')
    return
  }
  if (reviewForm.result === 'REJECT' && !reviewForm.reason.trim()) {
    ElMessage.error('审核不通过必须填写原因')
    return
  }
  if (!reviewForm.signaturePassword.trim()) {
    ElMessage.error('请输入电子签名密码')
    return
  }
  reviewSubmitting.value = true
  try {
    await reviewApprovalTask({
      moduleCode: reviewTask.value.moduleCode,
      sourceTaskType: reviewTask.value.sourceTaskType,
      sourceTaskId: reviewTask.value.sourceTaskId,
      businessKey: reviewTask.value.businessKey,
      processInstanceId: reviewTask.value.processInstanceId,
      result: reviewForm.result,
      reason: reviewForm.result === 'REJECT' ? reviewForm.reason.trim() : undefined,
      signaturePassword: reviewForm.signaturePassword.trim()
    })
    ElMessage.success(reviewForm.result === 'APPROVE' ? '审核已通过' : '审核不通过已提交')
    reviewDialogVisible.value = false
    await getList()
    await approvalTodoBadgeStore.refreshTodoTotal()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error))
  } finally {
    reviewSubmitting.value = false
  }
}

const resolveModuleName = (moduleCode: ApprovalModuleCode) => {
  return modules.value.find((item) => item.moduleCode === moduleCode)?.moduleName || moduleCode
}

const resolveReviewerLabel = (row: ApprovalTaskSummaryVO) =>
  row.assigneeUserName || (row.assigneeUserId ? `用户 #${row.assigneeUserId}` : '--')

const resolveNodeSubLabel = (row: ApprovalTaskSummaryVO) => {
  const reviewerLabel = resolveReviewerLabel(row)
  return reviewerLabel !== '--' ? `审核人：${reviewerLabel}` : row.businessStatus || '--'
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

onMounted(async () => {
  applyRouteQuery()
  syncRouteToCanonicalPath(queryParams.viewType)
  await refreshAll()
})

watch(
  () => [route.path, route.query],
  () => {
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

.approval-center__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.approval-center__title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.approval-center__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.approval-center__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
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

.approval-center__remark {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #4b5563;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

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
