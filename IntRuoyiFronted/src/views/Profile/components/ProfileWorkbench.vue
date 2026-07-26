<template>
  <div class="profile-workbench">
    <el-alert
      v-if="loadErrorMessages.length"
      class="profile-workbench__alert"
      :closable="false"
      title="待办任务加载失败"
      type="error"
      show-icon
    >
      <template #default>
        {{ loadErrorMessages.join('；') }}
      </template>
    </el-alert>

    <UnifiedListTemplate
      class="profile-workbench__list-template"
      table-key="profile.workbench.todo"
      :query-model="queryParams"
      label-width="76px"
      query-form-test-id="profile-workbench-todo-toolbar"
      :filter-definitions="todoQuickFilterDefinitions"
      :quick-filter-state="todoQuickFilter.state"
      :selected-filter-definition="todoQuickFilter.selectedDefinition.value"
      :operator-options="todoQuickFilter.operatorOptions.value"
      :columns="todoColumns"
      :column-saving="todoColumnSaving"
      :show-column-reset="false"
      :total="filteredRows.length"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="todoQuickFilter.updateState"
      @quick-filter-query="todoQuickFilter.applyQuickFilter"
      @column-change="saveTodoColumnConfig"
      @pagination="handleTodoPagination"
    >
      <template #actions>
        <el-radio-group
          v-model="activeVisibilityTab"
          size="small"
          class="mr-8px"
          @change="handleVisibilityTabChange"
        >
          <el-radio-button label="visible">待办任务</el-radio-button>
          <el-radio-button label="hidden">已隐藏 {{ hiddenRows.length }}</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" @click="loadWorkbench">
          <Icon icon="ep:refresh-right" class="mr-5px" />
          刷新
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-testid="profile-unified-todo-list"
          data-user-table-column-explicit
          data-user-table-key="profile.workbench.todo"
          :data="pagedRows"
          border
          :stripe="true"
          row-key="id"
          height="520"
          :empty-text="activeVisibilityTab === 'hidden' ? '暂无隐藏任务' : '当前没有待办任务'"
          :show-overflow-tooltip="true"
          @header-dragend="handleTodoHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isTodoColumnVisible('taskType')"
            label="任务类型"
            prop="taskType"
            :width="getTodoColumnWidthString('taskType', 120)"
            fixed="left"
            v-bind="sortColumnAttrs('taskType')"
          >
            <template #default="{ row }">
              <el-tag size="small" effect="light" :type="getTaskTypeTagType(row.taskType)">
                {{ row.taskType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTodoColumnVisible('source')"
            label="来源"
            prop="source"
            :width="getTodoColumnWidthString('source', 150)"
            v-bind="sortColumnAttrs('source')"
          />
          <el-table-column
            v-if="isTodoColumnVisible('detail')"
            label="待办详情"
            prop="detail"
            :min-width="getTodoColumnMinWidthString('detail', 360)"
            v-bind="sortColumnAttrs('detail')"
          >
            <template #default="{ row }">
              <span class="profile-workbench__detail">{{ row.detail }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTodoColumnVisible('statusTime')"
            label="状态/时间"
            prop="statusTime"
            :width="getTodoColumnWidthString('statusTime', 220)"
            v-bind="sortColumnAttrs('statusTime')"
          >
            <template #default="{ row }">
              <div class="profile-workbench__status">
                <span>{{ row.statusLabel }}</span>
              </div>
          </template>
        </el-table-column>
        <el-table-column
          v-if="isTodoColumnVisible('actions')"
          label="操作"
          prop="actions"
          :width="getTodoColumnWidthString('actions', 170)"
          fixed="right"
        >
          <template #default="{ row }">
              <el-button
                v-if="activeVisibilityTab !== 'hidden'"
                link
                type="primary"
                @click="openTodo(row)"
              >
                进入/处理
              </el-button>
              <el-button
                v-if="activeVisibilityTab !== 'hidden'"
                link
                type="warning"
                :loading="actionTaskKey === row.id"
                @click="handleHideTodo(row)"
              >
                隐藏
              </el-button>
              <el-button
                v-else
                link
                type="success"
                :loading="actionTaskKey === row.id"
                @click="handleRestoreTodo(row)"
              >
                恢复
              </el-button>
          </template>
        </el-table-column>
      </el-table>
      </template>
    </UnifiedListTemplate>
  </div>
</template>

<script lang="ts" setup>
import type { RouteLocationRaw } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/config/axios'
import {
  getMyDistributionTaskPage,
  type DistributionTaskVO
} from '@/api/dcc/controlledFile/distribution'
import {
  getMyTrainingTaskPage,
  type TrainingTaskProgressVO
} from '@/api/dcc/controlledFile/training'
import {
  getEdhrWorkTaskMyPage,
  type EdhrWorkTaskRespVO
} from '@/api/mes/pro/edhr/workTask'
import { ProWorkOrderApi, type ProWorkOrderVO } from '@/api/mes/pro/workorder'
import {
  getProfileWorkbenchHiddenTaskKeys,
  hideProfileWorkbenchTask,
  restoreProfileWorkbenchTask
} from '@/api/system/profileWorkbenchTaskVisibility'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useProfileWorkbenchTodoBadgeStore } from '@/store/modules/profileWorkbenchTodoBadge'
import { useUserStore } from '@/store/modules/user'
import { navigateToEdhrWorkTask, normalizeEdhrWorkTaskRoute } from '@/utils/edhrWorkTaskNavigation'
import { checkPermi } from '@/utils/permission'
import {
  MesProWorkOrderStatusEnum,
  MesProWorkOrderTypeEnum
} from '@/views/mes/utils/constants'
import {
  normalizeAssignmentPage,
  resolveAssignmentStatusText,
  resolveFieldLabel,
  resolveTargetTypeText,
  type ShowroomAssignmentRecord
} from '@/views/showroom-admin/assignment/contracts'

defineOptions({ name: 'ProfileWorkbench' })

const TASK_TYPES = ['文控', '批记录', '排产', '展厅', '行政'] as const
type TodoTaskType = (typeof TASK_TYPES)[number]

interface UnifiedTodoRow {
  id: string
  businessId: string | number
  taskType: TodoTaskType
  source: string
  detail: string
  statusLabel: string
  createdAt?: string
  dueAt?: string
  route: RouteLocationRaw
  edhrWorkTask?: EdhrWorkTaskRespVO
}

const TODO_PAGE_SIZE = 50
const TODO_TABLE_KEY = 'profile.workbench.todo'
const router = useRouter()
const userStore = useUserStore()
const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()
const { wsCache } = useCache()

const loading = ref(false)
const actionTaskKey = ref('')
const activeVisibilityTab = ref<'visible' | 'hidden'>('visible')
const hiddenTaskKeys = ref<Set<string>>(new Set())
const todoRows = ref<UnifiedTodoRow[]>([])
const loadErrorMessages = ref<string[]>([])

const todoDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'taskType', label: '任务类型', width: 120 },
  { key: 'source', label: '来源', width: 150 },
  { key: 'detail', label: '待办详情', minWidth: 360 },
  { key: 'statusTime', label: '状态/时间', width: 220 },
  { key: 'actions', label: '操作', width: 170, hideable: false, business: false }
]

const {
  columns: todoColumns,
  saving: todoColumnSaving,
  isColumnVisible: isTodoColumnVisible,
  getColumnWidthString: getTodoColumnWidthString,
  getColumnMinWidthString: getTodoColumnMinWidthString,
  handleHeaderDragend: handleTodoHeaderDragend,
  saveConfig: saveTodoColumnConfig
} = useUserTableColumns(TODO_TABLE_KEY, todoDefaultColumns)

const queryParams = reactive<{
  pageNo: number
  pageSize: number
  taskType?: TodoTaskType
  quickFilter?: {
    fieldKey: string
    operator: 'contains' | 'eq' | 'between'
    value?: string | number | boolean
    valueEnd?: string | number | boolean
  }
}>({
  pageNo: 1,
  pageSize: 10,
  taskType: undefined,
  quickFilter: undefined
})

const todoQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'taskType',
    label: '任务类型',
    type: 'select',
    queryParamKey: 'taskType',
    options: TASK_TYPES.map((taskType) => ({ label: taskType, value: taskType }))
  },
  { key: 'source', label: '来源', type: 'text', placeholder: '请输入来源' },
  { key: 'detail', label: '待办详情', type: 'text', placeholder: '请输入待办详情' },
  { key: 'statusLabel', label: '状态', type: 'text', placeholder: '请输入状态' }
]

const currentUserId = computed(() => {
  const rawId = userStore.getUser?.id ?? userStore.user?.id
  const id = Number(rawId)
  return Number.isFinite(id) && id > 0 ? id : undefined
})

const canViewDccTraining = computed(() => checkPermi(['dcc:controlled-file:training:mine']))
const canViewDccDistribution = computed(() => checkPermi(['dcc:controlled-file:query']))
const canViewEdhrWorkTasks = computed(
  () =>
    checkPermi(['mes:pro-edhr-work-task:query']) ||
    checkPermi(['mes:pro-edhr-batch-execution:query'])
)
const canViewWorkOrders = computed(() => checkPermi(['mes:pro-work-order:query']))
const canViewShowroomAssignments = computed(
  () => userStore.getRoles.includes('super_admin') || hasCachedRouteName('ShowroomAdminAssignment')
)

type TodoQuickFilterRow = Pick<UnifiedTodoRow, 'taskType' | 'source' | 'detail' | 'statusLabel'>

const matchesQuickFilter = (row: TodoQuickFilterRow) => {
  const filter = queryParams.quickFilter
  if (!filter?.fieldKey || filter.value === undefined || filter.value === null) {
    return true
  }
  const keyword = String(filter.value).trim()
  if (!keyword) {
    return true
  }
  const rawValue = row[filter.fieldKey as keyof TodoQuickFilterRow]
  const text = rawValue === undefined || rawValue === null ? '' : String(rawValue)
  return filter.operator === 'eq' ? text === keyword : text.includes(keyword)
}

const visibleRows = computed(() => todoRows.value.filter((row) => !hiddenTaskKeys.value.has(row.id)))
const hiddenRows = computed(() => todoRows.value.filter((row) => hiddenTaskKeys.value.has(row.id)))
const activeRows = computed(() =>
  activeVisibilityTab.value === 'hidden' ? hiddenRows.value : visibleRows.value
)

const filteredRows = computed(() => {
  return activeRows.value.filter((row) => {
    if (queryParams.taskType && row.taskType !== queryParams.taskType) {
      return false
    }
    return matchesQuickFilter(row)
  })
})

const pagedRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredRows.value.slice(start, start + queryParams.pageSize)
})

const ensureCurrentPageInRange = () => {
  const maxPage = Math.max(1, Math.ceil(filteredRows.value.length / queryParams.pageSize))
  if (queryParams.pageNo > maxPage) {
    queryParams.pageNo = maxPage
  }
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.message || (error as any)?.response?.data?.msg
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallback
}

function requirePageList<T>(page: PageResult<T[]> | undefined | null, source: string): T[] {
  if (!page || !Array.isArray(page.list)) {
    throw new Error(`${source}接口返回缺少待办列表。`)
  }
  return page.list
}

const hasCachedRouteName = (targetName: string) => {
  const visit = (items: unknown[]): boolean =>
    items.some((item) => {
      if (!item || typeof item !== 'object') {
        return false
      }
      const record = item as Record<string, unknown>
      if (record.name === targetName || record.componentName === targetName) {
        return true
      }
      return Array.isArray(record.children) ? visit(record.children) : false
    })
  const cachedRoutes = wsCache.get(CACHE_KEY.ROLE_ROUTERS)
  return Array.isArray(cachedRoutes) ? visit(cachedRoutes) : false
}

const compactJoin = (items: Array<string | number | null | undefined>) =>
  items
    .map((item) => (item === undefined || item === null ? '' : String(item).trim()))
    .filter(Boolean)
    .join(' · ')

const getTimeValue = (value?: string) => {
  if (!value) {
    return Number.NaN
  }
  const parsed = Date.parse(value)
  return Number.isFinite(parsed) ? parsed : Number.NaN
}

const sortTodoRows = (rows: UnifiedTodoRow[]) => {
  return [...rows].sort((left, right) => {
    const leftDue = getTimeValue(left.dueAt)
    const rightDue = getTimeValue(right.dueAt)
    if (Number.isFinite(leftDue) || Number.isFinite(rightDue)) {
      return (Number.isFinite(leftDue) ? leftDue : Number.MAX_SAFE_INTEGER) -
        (Number.isFinite(rightDue) ? rightDue : Number.MAX_SAFE_INTEGER)
    }
    const leftCreated = getTimeValue(left.createdAt)
    const rightCreated = getTimeValue(right.createdAt)
    return (Number.isFinite(rightCreated) ? rightCreated : 0) -
      (Number.isFinite(leftCreated) ? leftCreated : 0)
  })
}

const getDccTrainingStatusLabel = (status: TrainingTaskProgressVO['status']) => {
  if (status === 'PENDING_VIEW') {
    return '待学习'
  }
  if (status === 'READY_TO_ACKNOWLEDGE') {
    return '待确认'
  }
  return status
}

const getDccDistributionStatusLabel = (status: DistributionTaskVO['status']) => {
  if (status === 'READY_TO_ACKNOWLEDGE') {
    return '待签收'
  }
  return status
}

const mapDccDistributionRow = (item: DistributionTaskVO): UnifiedTodoRow => ({
  id: `文控分发:${item.recipientId}`,
  businessId: item.recipientId,
  taskType: '文控',
  source: '文控分发',
  detail: compactJoin([
    item.fileNumber,
    item.title || item.fileName,
    item.versionNo ? `版本 ${item.versionNo}` : undefined
  ]),
  statusLabel: getDccDistributionStatusLabel(item.status),
  createdAt: item.publishedTime,
  route: {
    name: 'DccControlledFileDetail',
    params: { id: item.controlledFileId },
    query: {
      viewer: '1',
      from: 'profile-distribution',
      distributionId: String(item.distributionId),
      recipientId: String(item.recipientId)
    }
  }
})

const mapDccTrainingRow = (item: TrainingTaskProgressVO): UnifiedTodoRow => ({
  id: `文控培训:${item.progressId}`,
  businessId: item.progressId,
  taskType: '文控',
  source: '文控培训',
  detail: compactJoin([
    item.fileNumber,
    item.title || item.fileName,
    item.versionNo ? `版本 ${item.versionNo}` : undefined
  ]),
  statusLabel: getDccTrainingStatusLabel(item.status),
  createdAt: item.publishedTime,
  route: {
    name: 'DccTrainingTask',
    params: { progressId: item.progressId }
  }
})

const edhrTaskTypeLabels: Record<string, string> = {
  FILL: '填写',
  REVIEW: '复核',
  REWORK: '返工',
  ARCHIVE: '归档'
}

const edhrStatusLabels: Record<string, string> = {
  TODO: '待处理',
  DOING: '处理中',
  OVERDUE: '已逾期',
  DONE: '已完成',
  CANCELED: '已取消'
}

const buildEdhrRoute = (item: EdhrWorkTaskRespVO): RouteLocationRaw => {
  return normalizeEdhrWorkTaskRoute(item)
}

const mapEdhrWorkTaskRow = (item: EdhrWorkTaskRespVO): UnifiedTodoRow => ({
  id: `eDHR工作任务:${item.id}`,
  businessId: item.id,
  taskType: '批记录',
  source: 'eDHR工作任务',
  detail: compactJoin([
    item.taskCode || `任务#${item.id}`,
    item.workOrderCode,
    item.batchCode,
    item.processName,
    edhrTaskTypeLabels[item.taskType] || item.taskType
  ]),
  statusLabel: edhrStatusLabels[item.status] || item.status,
  createdAt: item.createTime,
  dueAt: item.dueTime || item.overdueAt,
  route: buildEdhrRoute(item),
  edhrWorkTask: item
})

const mapWorkOrderRow = (item: ProWorkOrderVO): UnifiedTodoRow => ({
  id: `待排产工单:${item.id}`,
  businessId: item.id,
  taskType: '排产',
  source: '待排产工单',
  detail: compactJoin([
    item.code,
    item.productName,
    item.batchCode,
    item.quantity ? `数量 ${item.quantity}` : undefined
  ]),
  statusLabel: '已确认待排产',
  createdAt: item.requestDate ? String(item.requestDate) : undefined,
  dueAt: item.plannedStartTime ? String(item.plannedStartTime) : undefined,
  route: {
    path: '/mes/pro/work-order',
    query: item.code ? { code: item.code } : undefined
  }
})

const mapShowroomAssignmentRow = (item: ShowroomAssignmentRecord): UnifiedTodoRow => ({
  id: `展厅补充指派:${item.assignmentId}`,
  businessId: item.assignmentId,
  taskType: '展厅',
  source: '展厅补充指派',
  detail: compactJoin([
    `${resolveTargetTypeText(item.targetType)}#${item.targetId}`,
    resolveFieldLabel(item.targetType, item.fieldCode),
    item.notifyContent
  ]),
  statusLabel: resolveAssignmentStatusText(item.status),
  route: {
    name: 'ShowroomAdminAssignment',
    query: { assignmentId: item.assignmentId }
  }
})

const loadDccDistributionRows = async () => {
  const page = await getMyDistributionTaskPage({
    pageNo: 1,
    pageSize: TODO_PAGE_SIZE,
    status: 'READY_TO_ACKNOWLEDGE'
  })
  return requirePageList(page, '文控分发').map(mapDccDistributionRow)
}

const loadDccTrainingRows = async () => {
  const pages = await Promise.all([
    getMyTrainingTaskPage({ pageNo: 1, pageSize: TODO_PAGE_SIZE, status: 'PENDING_VIEW' }),
    getMyTrainingTaskPage({ pageNo: 1, pageSize: TODO_PAGE_SIZE, status: 'READY_TO_ACKNOWLEDGE' })
  ])
  return pages.flatMap((page) => requirePageList(page, '文控培训').map(mapDccTrainingRow))
}

const loadEdhrRows = async () => {
  const page = await getEdhrWorkTaskMyPage({ pageNo: 1, pageSize: TODO_PAGE_SIZE })
  return requirePageList(page, 'eDHR 工作任务').map(mapEdhrWorkTaskRow)
}

const loadWorkOrderRows = async () => {
  const page = await ProWorkOrderApi.getWorkOrderPage({
    pageNo: 1,
    pageSize: TODO_PAGE_SIZE,
    status: MesProWorkOrderStatusEnum.CONFIRMED,
    type: MesProWorkOrderTypeEnum.SELF,
    temporaryFrozen: false
  } as any)
  return requirePageList(page as PageResult<ProWorkOrderVO[]>, '排产工单').map(mapWorkOrderRow)
}

const loadShowroomRows = async () => {
  if (!currentUserId.value) {
    throw new Error('当前登录用户 ID 缺失，无法加载展厅补充指派。')
  }
  const page = await request.get({
    url: '/showroom/assignment/page',
    params: {
      status: 'OPEN',
      assigneeUserId: currentUserId.value,
      pageNo: 1,
      pageSize: TODO_PAGE_SIZE
    }
  })
  return normalizeAssignmentPage(page).map(mapShowroomAssignmentRow)
}

const loadHiddenTaskKeys = async () => {
  const keys = await getProfileWorkbenchHiddenTaskKeys()
  hiddenTaskKeys.value = new Set(keys)
}

const loadEnabledSource = async (
  sourceLabel: string,
  loader: () => Promise<UnifiedTodoRow[]>
) => {
  try {
    return await loader()
  } catch (error) {
    loadErrorMessages.value.push(`${sourceLabel}：${resolveErrorMessage(error, '加载失败')}`)
    return []
  }
}

const loadWorkbench = async () => {
  loading.value = true
  loadErrorMessages.value = []
  try {
    try {
      await loadHiddenTaskKeys()
    } catch (error) {
      loadErrorMessages.value.push(`隐藏任务状态：${resolveErrorMessage(error, '加载失败')}`)
      todoRows.value = []
      return
    }
    const loaders: Array<Promise<UnifiedTodoRow[]>> = []
    if (canViewDccDistribution.value) {
      loaders.push(loadEnabledSource('文控分发加载失败', loadDccDistributionRows))
    }
    if (canViewDccTraining.value) {
      loaders.push(loadEnabledSource('文控培训加载失败', loadDccTrainingRows))
    }
    if (canViewEdhrWorkTasks.value) {
      loaders.push(loadEnabledSource('批记录待办加载失败', loadEdhrRows))
    }
    if (canViewWorkOrders.value) {
      loaders.push(loadEnabledSource('排产工单加载失败', loadWorkOrderRows))
    }
    if (canViewShowroomAssignments.value) {
      loaders.push(loadEnabledSource('展厅补充指派加载失败', loadShowroomRows))
    }
    const rows = (await Promise.all(loaders)).flat()
    todoRows.value = sortTodoRows(rows)
    ensureCurrentPageInRange()
    try {
      await profileWorkbenchTodoBadgeStore.refreshTodoTotal()
    } catch (error) {
      loadErrorMessages.value.push(`待处理数量：${resolveErrorMessage(error, '刷新失败')}`)
    }
  } finally {
    loading.value = false
  }
}

const getTaskTypeTagType = (taskType: TodoTaskType) => {
  if (taskType === '文控') return 'success'
  if (taskType === '批记录') return 'warning'
  if (taskType === '排产') return 'primary'
  if (taskType === '展厅') return 'info'
  return ''
}

const openTodo = async (row: UnifiedTodoRow) => {
  if (row.edhrWorkTask) {
    await navigateToEdhrWorkTask(router, row.edhrWorkTask)
    return
  }
  await router.push(row.route)
}

const refreshTodoBadgeAfterVisibilityChange = async () => {
  try {
    await profileWorkbenchTodoBadgeStore.refreshTodoTotal()
  } catch (error) {
    loadErrorMessages.value.push(`待处理数量：${resolveErrorMessage(error, '刷新失败')}`)
  }
}

const addHiddenTaskKey = (taskKey: string) => {
  const nextKeys = new Set(hiddenTaskKeys.value)
  nextKeys.add(taskKey)
  hiddenTaskKeys.value = nextKeys
}

const removeHiddenTaskKey = (taskKey: string) => {
  const nextKeys = new Set(hiddenTaskKeys.value)
  nextKeys.delete(taskKey)
  hiddenTaskKeys.value = nextKeys
}

const handleHideTodo = async (row: UnifiedTodoRow) => {
  try {
    await ElMessageBox.confirm(
      `确认隐藏“${row.detail || row.source}”？可在“已隐藏”中恢复。`,
      '隐藏个人工作台任务',
      {
        confirmButtonText: '隐藏',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  actionTaskKey.value = row.id
  try {
    await hideProfileWorkbenchTask({
      taskKey: row.id,
      taskType: row.taskType,
      source: row.source,
      businessId: String(row.businessId),
      detail: row.detail
    })
    addHiddenTaskKey(row.id)
    ensureCurrentPageInRange()
    await refreshTodoBadgeAfterVisibilityChange()
    ElMessage.success('任务已隐藏，可在已隐藏中恢复')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '隐藏任务失败'))
  } finally {
    actionTaskKey.value = ''
  }
}

const handleRestoreTodo = async (row: UnifiedTodoRow) => {
  actionTaskKey.value = row.id
  try {
    await restoreProfileWorkbenchTask(row.id)
    removeHiddenTaskKey(row.id)
    ensureCurrentPageInRange()
    await refreshTodoBadgeAfterVisibilityChange()
    ElMessage.success('任务已恢复')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '恢复任务失败'))
  } finally {
    actionTaskKey.value = ''
  }
}

const handleVisibilityTabChange = () => {
  queryParams.pageNo = 1
  ensureCurrentPageInRange()
}

const handleTodoPagination = () => {
  ensureCurrentPageInRange()
}

const todoQuickFilter = useTableQuickFilter(
  TODO_TABLE_KEY,
  todoQuickFilterDefinitions,
  queryParams,
  loadWorkbench
)

watch(
  () => [filteredRows.value.length, queryParams.pageSize],
  () => ensureCurrentPageInRange()
)

onMounted(() => {
  loadWorkbench()
})
</script>

<style scoped>
.profile-workbench {
  color: #172033;
}

.profile-workbench__alert {
  margin: 12px 0;
}

.profile-workbench__list-template {
  width: 100%;
}

:deep(.profile-workbench__list-template .el-table) {
  font-size: 13px;
}

:deep(.profile-workbench__list-template .el-table__header th) {
  height: 46px;
  background: #f7f9fc;
  color: #263247;
  font-weight: 700;
}

:deep(.profile-workbench__list-template .el-table__row) {
  height: 52px;
}

:deep(.profile-workbench__list-template .el-table__cell) {
  padding: 7px 10px;
}

.profile-workbench__detail {
  color: #263247;
}

.profile-workbench__status {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
  line-height: 1.4;
}

.profile-workbench__status span {
  color: #172033;
  font-weight: 600;
}

.profile-workbench__status small {
  color: #6b7280;
  font-size: 12px;
}
</style>
