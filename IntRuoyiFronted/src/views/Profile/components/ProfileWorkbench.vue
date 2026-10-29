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
      single-line-toolbar
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
      :total="total ?? 0"
      v-model:sort-state="sortState"
      @sort-change="handleTodoSortChange"
      :page="queryParams.pageNo"
      @update:page="handleTodoPageChange"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="todoQuickFilter.updateState"
      @quick-filter-query="todoQuickFilter.applyQuickFilter"
      @column-change="saveTodoColumnConfig"
      @pagination="handleTodoPagination"
    >
      <template #actions>
        <el-button v-hasPermi="['mes:pro-edhr-work-task:query']" @click="productionRecordsRef?.open()">生产任务记录</el-button>
        <el-radio-group
          v-model="activeVisibilityTab"
          :disabled="Boolean(actionTaskKey)"
          size="small"
          class="mr-8px"
          @change="handleVisibilityTabChange"
        >
          <el-radio-button label="visible">待办任务</el-radio-button>
          <el-radio-button label="hidden">已隐藏 {{ hiddenTotal === undefined ? '' : hiddenTotal }}</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading && total === undefined && !silentLoading" :disabled="loading || Boolean(actionTaskKey)" @click="loadWorkbench()">
          <Icon icon="ep:refresh-right" class="mr-5px" />
          刷新
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading && total === undefined && !silentLoading"
          data-testid="profile-unified-todo-list"
          data-user-table-column-explicit
          data-user-table-key="profile.workbench.todo"
          :data="todoRows"
          border
          :stripe="true"
          row-key="taskKey"
          height="100%"
          :empty-text="loading ? silentLoading ? ' ' : '加载中' : total === undefined ? '待办列表尚未加载成功，请重试' : activeVisibilityTab === 'hidden' ? '暂无隐藏任务' : '当前没有待办任务'"
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
                :disabled="!actionsReady || Boolean(actionTaskKey) || Boolean(row.action.payload.blockedReason)"
                :title="row.action.payload.blockedReason"
                @click="openTodo(row)"
              >
                {{ row.action.payload.blockedReason ? '暂不可处理' : '进入/处理' }}
              </el-button>
              <el-button
                v-if="activeVisibilityTab !== 'hidden'"
                link
                type="warning"
                :loading="actionTaskKey === row.taskKey"
                :disabled="!actionsReady || Boolean(actionTaskKey)"
                @click="handleHideTodo(row)"
              >
                隐藏
              </el-button>
              <el-button
                v-else
                link
                type="success"
                :loading="actionTaskKey === row.taskKey"
                :disabled="!actionsReady || Boolean(actionTaskKey)"
                @click="handleRestoreTodo(row)"
              >
                恢复
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
    <ProductionTaskRecords ref="productionRecordsRef" />
  </div>
</template>

<script lang="ts" setup>
import ProductionTaskRecords from './ProductionTaskRecords.vue'
const productionRecordsRef = ref<InstanceType<typeof ProductionTaskRecords>>()
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProfileWorkbenchTodoPage,
  initializeWorkbenchContext,
  setProfileWorkbenchTaskVisibility,
  type WorkbenchContext,
  type ProfileWorkbenchTodoRow,
  type ProfileWorkbenchTodoSourceId,
  type ProfileWorkbenchTodoQuery
} from '@/api/system/profileWorkbenchTodo'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'

import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useProfileWorkbenchTodoBadgeStore,
  normalizeProfileWorkbenchSources,
  type ProfileWorkbenchBadgeToken
} from '@/store/modules/profileWorkbenchTodoBadge'
import { useUserStore } from '@/store/modules/user'

import { getAccessToken } from '@/utils/auth'
import { authorizedWorkbenchSources, refreshWorkbenchDirectory, openWorkbenchAction, workbenchIdentityKey } from '@/workbench/registry'


import { beginWorkbenchRead, isWorkbenchReadCurrent, invalidateWorkbenchContext, registerWorkbenchFocusRefresh, workbenchInvalidationReason, workbenchLifetime, type WorkbenchReadLease } from '@/workbench/queryContext'

defineOptions({ name: 'ProfileWorkbench' })

type TodoTaskType = string
const TODO_TABLE_KEY = 'profile.workbench.todo'
const router = useRouter()
const userStore = useUserStore()

const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()


const loading = ref(false)
const silentLoading = ref(false)
const actionsReady = ref(false)
const actionTaskKey = ref('')
const visibilityWritePending = ref(false)
const activeVisibilityTab = ref<'visible' | 'hidden'>('visible')
const todoRows = ref<ProfileWorkbenchTodoRow[]>([])
const loadErrorMessages = ref<string[]>([])
const total = ref<number | undefined>()
const hiddenTotal = ref<number | undefined>()
const sortState = ref<{ key?: string; prop?: string; order?: 'ascending' | 'descending' | null }>({})
let requestGeneration = 0
let disposed = false
let visibilityActionGeneration = 0
let refreshingAfterWrite = false
let preservingSnapshotDisplay = false

const todoDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'taskType', label: '任务类型', width: 120, sortable: 'custom' },
  { key: 'source', label: '来源', width: 150, sortable: 'custom' },
  { key: 'detail', label: '待办详情', minWidth: 360, sortable: 'custom' },
  { key: 'statusTime', label: '状态/时间', width: 220, sortProp: 'statusLabel', sortable: 'custom' },
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

const todoQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'taskType',
    label: '任务类型',
    type: 'select',
    queryParamKey: 'taskType',
    options: [...new Set(authorizedWorkbenchSources().map(source => source.taskType))].map(taskType => ({ label: taskType, value: taskType }))
  },
  { key: 'source', label: '来源', type: 'text', placeholder: '请输入来源' },
  { key: 'detail', label: '待办详情', type: 'text', placeholder: '请输入待办详情' },
  { key: 'statusLabel', label: '状态', type: 'text', placeholder: '请输入状态' }
])


const enabledSources = computed<ProfileWorkbenchTodoSourceId[]>(() => normalizeProfileWorkbenchSources(authorizedWorkbenchSources().map(source => source.sourceId)))

const rawQueryKey = () => JSON.stringify([queryParams, activeVisibilityTab.value, sortState.value])
interface PageToken {
  generation: number
  queryKey: string
  lease: WorkbenchReadLease
  badge: ProfileWorkbenchBadgeToken
}
let currentContext: WorkbenchContext | undefined
let currentLease: WorkbenchReadLease | undefined
const buildPageQuery = (): ProfileWorkbenchTodoQuery => {
  const filter = queryParams.quickFilter
  if (filter && filter.operator !== 'contains' && filter.operator !== 'eq') throw new Error('个人工作台不支持该过滤条件。')
  const sortKey = sortState.value.prop || sortState.value.key
  return { pageNo: queryParams.pageNo, pageSize: queryParams.pageSize, visibility: activeVisibilityTab.value,
    enabledSources: [...enabledSources.value], taskType: queryParams.taskType,
    quickFilter: filter ? { fieldKey: filter.fieldKey, operator: filter.operator as 'contains' | 'eq',
      value: filter.value === undefined ? '' : String(filter.value) } : undefined,
    sort: sortState.value.order && sortKey ? { key: sortKey, order: sortState.value.order === 'ascending' ? 'asc' : 'desc' } : undefined }
}
const hasAuthenticatedUser = () => Boolean(getAccessToken()) && userStore.getUser.id > 0
const tryCommitPage = (token: PageToken, commit: () => void) => {
  if (disposed || !hasAuthenticatedUser() || token.generation !== requestGeneration ||
    token.queryKey !== rawQueryKey() || !isWorkbenchReadCurrent(token.lease)) return false
  commit()
  return true
}
const resolveErrorMessage = (error: unknown, message: string) => {
  const responseMessage = (error as any)?.response?.data?.message || (error as any)?.response?.data?.msg
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return message
}
const invalidatePage = (preserveDisplay = false) => {
  requestGeneration += 1
  if (!preserveDisplay) {
    todoRows.value = []
    total.value = undefined
    hiddenTotal.value = undefined
  }
  actionsReady.value = false
  currentContext = undefined
  currentLease = undefined
  loading.value = false
  loadErrorMessages.value = []
}
const loadWorkbench = async (options?: { silent?: boolean }): Promise<boolean> => {
  if (disposed) return false
  silentLoading.value = Boolean(options?.silent)
  let snapshot: ReturnType<typeof profileWorkbenchTodoBadgeStore.readPageSnapshot>
  try {
    snapshot = profileWorkbenchTodoBadgeStore.readPageSnapshot(rawQueryKey())
  } catch (error) {
    invalidatePage()
    loadErrorMessages.value = [resolveErrorMessage(error, '个人工作台快照读取失败。')]
    return false
  }
  // Revoke both consumers before the first await; their subsequent read leases are independent.
  preservingSnapshotDisplay = Boolean(snapshot)
  try {
    invalidateWorkbenchContext()
    invalidatePage(preservingSnapshotDisplay)
  } finally {
    preservingSnapshotDisplay = false
  }
  if (snapshot) {
    todoRows.value = snapshot.rows
    total.value = snapshot.total
    hiddenTotal.value = snapshot.hiddenTotal
    profileWorkbenchTodoBadgeStore.savePageSnapshot(snapshot.queryKey, snapshot.rows, snapshot.total, snapshot.hiddenTotal)
  }
  const badge = profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
  const token: PageToken = { generation: requestGeneration, queryKey: rawQueryKey(), lease: beginWorkbenchRead(), badge }
  if (!hasAuthenticatedUser()) return false
  loading.value = true
  profileWorkbenchTodoBadgeStore.tryCommit(badge, { loading: true })
  try {
    const directory = await refreshWorkbenchDirectory()
    if (!tryCommitPage(token, () => {})) return false
    profileWorkbenchTodoBadgeStore.bindDirectory(badge)
    const query = buildPageQuery()
    const context = await initializeWorkbenchContext(directory.registryRevision, directory.sources, token.lease.contextEpoch, {
      visibility: query.visibility, taskType: query.taskType,
      quickFilter: query.quickFilter ? { ...query.quickFilter, value: query.quickFilter.value ?? '' } : undefined,
      sort: query.sort
    })
    if (!tryCommitPage(token, () => {})) return false
    const page = await getProfileWorkbenchTodoPage(context, query.pageNo, query.pageSize, directory.sources)
    const committed = tryCommitPage(token, () => {
      if (JSON.stringify(todoRows.value) !== JSON.stringify(page.rows)) todoRows.value = page.rows
      total.value = page.filteredTotal
      hiddenTotal.value = page.hiddenTotal
      queryParams.pageNo = page.effectivePageNo
      token.queryKey = rawQueryKey()
      currentContext = context
      currentLease = token.lease
      actionsReady.value = true
      profileWorkbenchTodoBadgeStore.savePageSnapshot(rawQueryKey(), todoRows.value, page.filteredTotal, page.hiddenTotal)
    })
    // Only this exact page response supplies its badge; never splice another count snapshot into it.
    if (committed) profileWorkbenchTodoBadgeStore.applyTodoTotal(badge, page.businessTotal)
    return committed
  } catch (error) {
    tryCommitPage(token, () => {
      actionsReady.value = false
      currentContext = undefined
      currentLease = undefined
      todoRows.value = []
      total.value = undefined
      hiddenTotal.value = undefined
      profileWorkbenchTodoBadgeStore.clearPageSnapshot()
      loadErrorMessages.value = [resolveErrorMessage(error, '待办任务加载失败，请重试。')]
      profileWorkbenchTodoBadgeStore.tryCommit(badge, { loaded: false, error: resolveErrorMessage(error, '待处理数量加载失败') })
    })
    return false
  } finally {
    tryCommitPage(token, () => {
      loading.value = false
      profileWorkbenchTodoBadgeStore.tryCommit(badge, { loading: false })
    })
  }
}

const getTaskTypeTagType = (taskType: TodoTaskType) => {
  if (taskType === '文控') return 'success'
  if (taskType === '批记录') return 'warning'
  if (taskType === '排产') return 'primary'
  if (taskType === '展厅') return 'info'
  return ''
}

const openTodo = async (row: ProfileWorkbenchTodoRow) => {
  const lease = currentLease
  const generation = requestGeneration
  if (!lease || !isWorkbenchReadCurrent(lease)) return
  const callerCurrent = () => !disposed && generation === requestGeneration && isWorkbenchReadCurrent(lease)
  try {
    await openWorkbenchAction(router, row.sourceId, row.action, callerCurrent)
  } catch (error) {
    if (!disposed && generation === requestGeneration && isWorkbenchReadCurrent(lease)) {
      ElMessage.error(resolveErrorMessage(error, '进入任务失败'))
    }
  }
}

const runVisibilityAction = async (row: ProfileWorkbenchTodoRow, restore: boolean, confirmed = false) => {
  if (visibilityWritePending.value || (actionTaskKey.value && !confirmed)) return
  if (!currentContext || !currentLease || !isWorkbenchReadCurrent(currentLease) ||
    !todoRows.value.some(item => item.taskKey === row.taskKey)) {
    ElMessage.error('任务上下文已失效，请重新加载。')
    actionTaskKey.value = ''
    return
  }
  const context = currentContext
  let lease = currentLease
  const identity = workbenchIdentityKey()
  const actionGeneration = ++visibilityActionGeneration
  const ownsAction = () => !disposed && actionGeneration === visibilityActionGeneration &&
    identity === workbenchIdentityKey() && isWorkbenchReadCurrent(lease)
  actionTaskKey.value = row.taskKey
  visibilityWritePending.value = true
  profileWorkbenchTodoBadgeStore.clearPageSnapshot()
  const badge = profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
  invalidatePage()
  let writeSucceeded = false
  try {
    await setProfileWorkbenchTaskVisibility(context, row.taskKey, restore ? 'restore' : 'hide')
    if (!ownsAction()) return
    writeSucceeded = true
    visibilityWritePending.value = false
    refreshingAfterWrite = true
    const refresh = loadWorkbench()
    refreshingAfterWrite = false
    lease = beginWorkbenchRead()
    const refreshGeneration = requestGeneration
    const refreshed = await refresh
    if (!ownsAction() || refreshGeneration !== requestGeneration) return
    if (refreshed) ElMessage.success(restore ? '任务已恢复' : '任务已隐藏，可在已隐藏中恢复')
    else loadErrorMessages.value.unshift(restore ? '任务已恢复，但列表刷新失败，请重试。' : '任务已隐藏，但列表刷新失败，请重试。')
  } catch (error) {
    if (!ownsAction()) return
    profileWorkbenchTodoBadgeStore.tryCommit(badge, { loaded: false, loading: false, error: resolveErrorMessage(error, '任务操作失败') })
    loadErrorMessages.value = [resolveErrorMessage(error, writeSucceeded ? '任务操作已成功，但列表刷新失败，请重试。' :
      restore ? '恢复任务失败，请重试。' : '隐藏任务失败，请重试。')]
  } finally {
    if (ownsAction()) {
      visibilityWritePending.value = false
      actionTaskKey.value = ''
    }
  }
}
const handleHideTodo = async (row: ProfileWorkbenchTodoRow) => {
  if (actionTaskKey.value) return
  const lease = beginWorkbenchRead()
  const generation = requestGeneration
  const actionGeneration = ++visibilityActionGeneration
  const ownsConfirmation = () => !disposed && actionGeneration === visibilityActionGeneration &&
    generation === requestGeneration && isWorkbenchReadCurrent(lease)
  actionTaskKey.value = row.taskKey
  try {
    await ElMessageBox.confirm(`确认隐藏“${row.detail || row.source}”？可在“已隐藏”中恢复。`, '隐藏个人工作台任务',
      { confirmButtonText: '隐藏', cancelButtonText: '取消', type: 'warning' })
    if (!ownsConfirmation()) return
    await runVisibilityAction(row, false, true)
  } catch (error) {
    if (ownsConfirmation() && error !== 'cancel' && error !== 'close') ElMessage.error(resolveErrorMessage(error, '隐藏确认失败'))
  } finally {
    if (ownsConfirmation()) actionTaskKey.value = ''
  }
}
const handleRestoreTodo = (row: ProfileWorkbenchTodoRow) => runVisibilityAction(row, true)
const handleVisibilityTabChange = () => {
  queryParams.pageNo = 1
  void loadWorkbench({ silent: true })
}
const handleTodoSortChange = () => {
  queryParams.pageNo = 1
  void loadWorkbench()
}
// Clearing a pending result temporarily gives the pager total=0. Ignore its
// automatic page-1 normalization until an authoritative result is available.
const handleTodoPageChange = (page: number) => {
  if (total.value !== undefined) queryParams.pageNo = page
}
const handleTodoPagination = () => {
  if (total.value !== undefined) void loadWorkbench()
}
const todoQuickFilter = useTableQuickFilter(
  TODO_TABLE_KEY, todoQuickFilterDefinitions, queryParams, async () => { await loadWorkbench() }
)

watch(
  () => workbenchIdentityKey(),
  () => {
    visibilityActionGeneration += 1
    visibilityWritePending.value = false
    actionTaskKey.value = ''
    queryParams.pageNo = 1
    void loadWorkbench()
  },
  { flush: 'sync' }
)
watch(workbenchLifetime, () => {
  invalidatePage(preservingSnapshotDisplay)
  if (workbenchInvalidationReason.value) loadErrorMessages.value = [workbenchInvalidationReason.value]
  if (!refreshingAfterWrite) {
    visibilityActionGeneration += 1
    visibilityWritePending.value = false
    actionTaskKey.value = ''
  }
}, { flush: 'sync' })
let releaseFocusRefresh: (() => void) | undefined
onBeforeUnmount(() => {
  releaseFocusRefresh?.()
  disposed = true
  visibilityActionGeneration += 1
  invalidatePage()
  profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
})
onMounted(() => {
  releaseFocusRefresh = registerWorkbenchFocusRefresh(true, () => {
    // focus and visibilitychange often arrive together; keep the active read.
    if (!loading.value && !visibilityWritePending.value) void loadWorkbench()
  })
  void loadWorkbench()
})
</script>

<style scoped>
.profile-workbench {
  color: #172033;
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.profile-workbench__alert {
  margin: 12px 0;
}

.profile-workbench__list-template {
  width: 100%;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

:deep(.profile-workbench__list-template > .unified-list-template__query-form),
:deep(.profile-workbench__list-template > .el-pagination) {
  flex-shrink: 0;
}

:deep(.profile-workbench__list-template > .unified-list-template__table-shell) {
  flex: 1;
  min-height: 0;
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
