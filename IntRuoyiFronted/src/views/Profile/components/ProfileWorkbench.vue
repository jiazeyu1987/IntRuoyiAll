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
      :total="total ?? 0"
      v-model:sort-state="sortState"
      @sort-change="handleTodoSortChange"
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
          :disabled="Boolean(actionTaskKey)"
          size="small"
          class="mr-8px"
          @change="handleVisibilityTabChange"
        >
          <el-radio-button label="visible">待办任务</el-radio-button>
          <el-radio-button label="hidden">已隐藏 {{ hiddenTotal === undefined ? '' : hiddenTotal }}</el-radio-button>
        </el-radio-group>
        <el-button :loading="loading" :disabled="Boolean(actionTaskKey)" @click="loadWorkbench">
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
          :data="todoRows"
          border
          :stripe="true"
          row-key="taskKey"
          height="520"
          :empty-text="loading ? '加载中' : total === undefined ? '待办列表尚未加载成功，请重试' : activeVisibilityTab === 'hidden' ? '暂无隐藏任务' : '当前没有待办任务'"
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
                :disabled="Boolean(actionTaskKey)"
                @click="openTodo(row)"
              >
                进入/处理
              </el-button>
              <el-button
                v-if="activeVisibilityTab !== 'hidden'"
                link
                type="warning"
                :loading="actionTaskKey === row.taskKey"
                :disabled="Boolean(actionTaskKey)"
                @click="handleHideTodo(row)"
              >
                隐藏
              </el-button>
              <el-button
                v-else
                link
                type="success"
                :loading="actionTaskKey === row.taskKey"
                :disabled="Boolean(actionTaskKey)"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProfileWorkbenchTodoPage,
  hideProfileWorkbenchTask,
  restoreProfileWorkbenchTask,
  type ProfileWorkbenchTodoRow,
  type ProfileWorkbenchTodoSourceId,
  type ProfileWorkbenchTodoQuery
} from '@/api/system/profileWorkbenchTodo'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useProfileWorkbenchTodoBadgeStore,
  normalizeProfileWorkbenchSources,
  profileWorkbenchBadgeScopeKey,
  resolveProfileWorkbenchBadgeScope,
  type ProfileWorkbenchBadgeToken
} from '@/store/modules/profileWorkbenchTodoBadge'
import { useUserStore } from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'
import { checkPermi } from '@/utils/permission'

defineOptions({ name: 'ProfileWorkbench' })

const TASK_TYPES = ['文控', '批记录', '排产', '展厅', '行政'] as const
type TodoTaskType = (typeof TASK_TYPES)[number]
const TODO_TABLE_KEY = 'profile.workbench.todo'
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()
const { wsCache } = useCache()

const loading = ref(false)
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


const canViewDccTraining = computed(() => checkPermi(['dcc:controlled-file:training:mine']))
const canViewDccDistribution = computed(() => checkPermi(['dcc:controlled-file:query']))
const canViewEdhrWorkTasks = computed(
  () =>
    checkPermi(['mes:pro-edhr-work-task:query']) ||
    checkPermi(['mes:pro-edhr-batch-execution:query'])
)
const canViewWorkOrders = computed(() => checkPermi(['mes:pro-work-order:query']))
const canViewShowroomAssignments = computed(
  () => {
    void permissionStore.getRouters
    return userStore.getRoles.includes('super_admin') || hasCachedRouteName('ShowroomAdminAssignment')
  }
)


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


const enabledSources = computed<ProfileWorkbenchTodoSourceId[]>(() => {
  // Route store is reactive; the showroom page gate still uses the existing cached route contract.
  void permissionStore.getRouters
  const sources: ProfileWorkbenchTodoSourceId[] = []
  if (canViewDccDistribution.value) sources.push('DCC_DISTRIBUTION')
  if (canViewDccTraining.value) sources.push('DCC_TRAINING')
  if (canViewEdhrWorkTasks.value) sources.push('EDHR_WORK_TASK')
  if (canViewWorkOrders.value) sources.push('WORK_ORDER')
  if (canViewShowroomAssignments.value) sources.push('SHOWROOM_ASSIGNMENT')
  return normalizeProfileWorkbenchSources(sources)
})

const pageScopeKey = () => JSON.stringify([
  String(userStore.getUser.id), String(getTenantId() ?? ''), String(getVisitTenantId() ?? ''), enabledSources.value
])

interface PageToken {
  generation: number
  scopeKey: string
  queryKey: string
  badge: ProfileWorkbenchBadgeToken
}

const buildPageQuery = (): ProfileWorkbenchTodoQuery => {
  const filter = queryParams.quickFilter
  if (filter && filter.operator !== 'contains' && filter.operator !== 'eq') {
    throw new Error('个人工作台不支持该过滤条件。')
  }
  const sortKey = sortState.value.prop || sortState.value.key
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    visibility: activeVisibilityTab.value,
    enabledSources: [...enabledSources.value],
    taskType: queryParams.taskType,
    quickFilter: filter ? {
      fieldKey: filter.fieldKey,
      operator: filter.operator as 'contains' | 'eq',
      value: filter.value === undefined ? undefined : String(filter.value)
    } : undefined,
    sort: sortState.value.order && sortKey ? {
      key: sortKey, order: sortState.value.order === 'ascending' ? 'asc' : 'desc'
    } : undefined
  }
}

const currentQueryKey = () => JSON.stringify(buildPageQuery())

const hasAuthenticatedUser = () => Boolean(getAccessToken()) && userStore.getUser.id > 0

const tryCommitPage = (token: PageToken, commit: () => void) => {
  if (
    disposed || !hasAuthenticatedUser() || token.generation !== requestGeneration || token.scopeKey !== pageScopeKey() ||
    token.queryKey !== currentQueryKey()
  ) return false
  commit()
  return true
}

const resolveErrorMessage = (error: unknown, message: string) => {
  const responseMessage = (error as any)?.response?.data?.message || (error as any)?.response?.data?.msg
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return message
}

const invalidatePage = () => {
  requestGeneration += 1
  todoRows.value = []
  total.value = undefined
  hiddenTotal.value = undefined
  loading.value = false
  loadErrorMessages.value = []
}

const pageMatchesBadgeSources = (query: ProfileWorkbenchTodoQuery, token: ProfileWorkbenchBadgeToken) =>
  JSON.stringify(normalizeProfileWorkbenchSources(query.enabledSources)) ===
  JSON.stringify(normalizeProfileWorkbenchSources(token.badgeSources))

const loadWorkbench = async (): Promise<boolean> => {
  if (disposed) return false
  if (!hasAuthenticatedUser()) {
    invalidatePage()
    profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
    return false
  }
  if (visibilityWritePending.value) return false
  const badge = profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
  invalidatePage()
  const query = buildPageQuery()
  const token: PageToken = {
    generation: requestGeneration, scopeKey: pageScopeKey(), queryKey: JSON.stringify(query), badge
  }
  const suppliesBadge = pageMatchesBadgeSources(query, badge)
  tryCommitPage(token, () => { loading.value = true })
  if (suppliesBadge) profileWorkbenchTodoBadgeStore.tryCommit(badge, { loading: true })
  // A page with a different source gate cannot supply the menu badge's count.
  const badgeRefresh = suppliesBadge ? undefined : profileWorkbenchTodoBadgeStore.refreshTodoTotal()
    .then(() => undefined, (error: unknown) => error)
  try {
    const page = await getProfileWorkbenchTodoPage(query)
    const committed = tryCommitPage(token, () => {
      todoRows.value = page.list
      total.value = page.total
      hiddenTotal.value = page.hiddenTotal
      queryParams.pageNo = page.effectivePageNo
      token.queryKey = currentQueryKey()
    })
    if (committed && suppliesBadge) profileWorkbenchTodoBadgeStore.applyTodoTotal(badge, page.businessTotal)
    return committed
  } catch (error) {
    tryCommitPage(token, () => {
      todoRows.value = []
      total.value = undefined
      hiddenTotal.value = undefined
      loadErrorMessages.value = [resolveErrorMessage(error, '待办任务加载失败，请重试。')]
    })
    if (suppliesBadge) tryCommitPage(token, () => {
      profileWorkbenchTodoBadgeStore.tryCommit(badge, {
        loaded: false, error: resolveErrorMessage(error, '待处理数量加载失败')
      })
    })
    return false
  } finally {
    tryCommitPage(token, () => { loading.value = false })
    if (suppliesBadge) tryCommitPage(token, () => {
      profileWorkbenchTodoBadgeStore.tryCommit(badge, { loading: false })
    })
    if (badgeRefresh) {
      const error = await badgeRefresh
      if (error !== undefined) {
        tryCommitPage(token, () => {
          loadErrorMessages.value.push(`待处理数量：${resolveErrorMessage(error, '刷新失败')}`)
        })
      }
    }
  }
}

const getTaskTypeTagType = (taskType: TodoTaskType) => {
  if (taskType === '文控') return 'success'
  if (taskType === '批记录') return 'warning'
  if (taskType === '排产') return 'primary'
  if (taskType === '展厅') return 'info'
  return ''
}

const requireNavigationId = (value: string | undefined, label: string) => {
  if (typeof value !== 'string' || !/^[1-9]\d*$/.test(value)) {
    throw new Error(`待办任务缺少有效${label}。`)
  }
  return value
}

const openTodo = async (row: ProfileWorkbenchTodoRow) => {
  try {
    const navigation = row.navigation
    if (row.sourceId === 'EDHR_WORK_TASK') {
      await navigateToEdhrWorkTask(router, navigation)
    } else if (row.sourceId === 'DCC_DISTRIBUTION') {
      await router.push({
        name: 'DccControlledFileDetail',
        params: { id: requireNavigationId(navigation.controlledFileId, '受控文件编号') },
        query: {
          viewer: '1', from: 'profile-distribution',
          distributionId: requireNavigationId(navigation.distributionId, '分发编号'),
          recipientId: requireNavigationId(navigation.recipientId, '收件人编号')
        }
      })
    } else if (row.sourceId === 'DCC_TRAINING') {
      await router.push({
        name: 'DccTrainingTask',
        params: { progressId: requireNavigationId(navigation.progressId, '培训进度编号') }
      })
    } else if (row.sourceId === 'WORK_ORDER') {
      await router.push({
        path: '/mes/pro/work-order', query: navigation.code ? { code: navigation.code } : undefined
      })
    } else if (row.sourceId === 'SHOWROOM_ASSIGNMENT') {
      await router.push({
        name: 'ShowroomAdminAssignment',
        query: { assignmentId: requireNavigationId(navigation.assignmentId, '指派编号') }
      })
    } else {
      throw new Error('待办任务来源不受支持。')
    }
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '进入任务失败'))
  }
}

const runVisibilityAction = async (row: ProfileWorkbenchTodoRow, restore: boolean) => {
  if (actionTaskKey.value) return
  actionTaskKey.value = row.taskKey
  visibilityWritePending.value = true
  const actionGeneration = ++visibilityActionGeneration
  const scopeKey = pageScopeKey()
  const actionBadge = profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
  invalidatePage()
  let writeSucceeded = false
  try {
    if (restore) {
      await restoreProfileWorkbenchTask(row.taskKey)
    } else {
      await hideProfileWorkbenchTask({
        taskKey: row.taskKey, taskType: row.taskType, source: row.source,
        businessId: row.businessId, detail: row.detail
      })
    }
    writeSucceeded = true
    if (disposed || actionGeneration !== visibilityActionGeneration || scopeKey !== pageScopeKey()) return
    visibilityWritePending.value = false
    const refreshGeneration = requestGeneration + 1
    const refreshed = await loadWorkbench()
    if (disposed || refreshGeneration !== requestGeneration || scopeKey !== pageScopeKey()) return
    if (refreshed) {
      ElMessage.success(restore ? '任务已恢复' : '任务已隐藏，可在已隐藏中恢复')
    } else {
      loadErrorMessages.value.unshift(restore
        ? '任务已恢复，但列表刷新失败，请重试。'
        : '任务已隐藏，但列表刷新失败，请重试。')
    }
  } catch (error) {
    if (disposed || actionGeneration !== visibilityActionGeneration || scopeKey !== pageScopeKey()) return
    profileWorkbenchTodoBadgeStore.tryCommit(actionBadge, {
      loaded: false, loading: false, error: resolveErrorMessage(error, '任务操作失败')
    })
    loadErrorMessages.value = [resolveErrorMessage(error, writeSucceeded
      ? '任务操作已成功，但列表刷新失败，请重试。'
      : restore ? '恢复任务失败，请重试。' : '隐藏任务失败，请重试。')]
  } finally {
    if (actionGeneration === visibilityActionGeneration) {
      visibilityWritePending.value = false
      actionTaskKey.value = ''
    }
  }
}

const handleHideTodo = async (row: ProfileWorkbenchTodoRow) => {
  if (actionTaskKey.value) return
  const confirmedScope = pageScopeKey()
  const confirmedGeneration = requestGeneration
  try {
    await ElMessageBox.confirm(
      `确认隐藏“${row.detail || row.source}”？可在“已隐藏”中恢复。`,
      '隐藏个人工作台任务',
      { confirmButtonText: '隐藏', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(resolveErrorMessage(error, '隐藏确认失败'))
    return
  }
  if (disposed || confirmedScope !== pageScopeKey() || confirmedGeneration !== requestGeneration) return
  await runVisibilityAction(row, false)
}

const handleRestoreTodo = (row: ProfileWorkbenchTodoRow) => runVisibilityAction(row, true)
const handleVisibilityTabChange = () => {
  queryParams.pageNo = 1
  void loadWorkbench()
}
const handleTodoSortChange = () => {
  queryParams.pageNo = 1
  void loadWorkbench()
}
const handleTodoPagination = () => { void loadWorkbench() }
const todoQuickFilter = useTableQuickFilter(
  TODO_TABLE_KEY, todoQuickFilterDefinitions, queryParams, async () => { await loadWorkbench() }
)

watch(
  () => JSON.stringify([pageScopeKey(), profileWorkbenchBadgeScopeKey(resolveProfileWorkbenchBadgeScope())]),
  () => {
    visibilityActionGeneration += 1
    visibilityWritePending.value = false
    actionTaskKey.value = ''
    queryParams.pageNo = 1
    void loadWorkbench()
  },
  { flush: 'sync' }
)
onBeforeUnmount(() => {
  disposed = true
  visibilityActionGeneration += 1
  invalidatePage()
  profileWorkbenchTodoBadgeStore.beginBadgeUpdate()
})
onMounted(() => { void loadWorkbench() })
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
