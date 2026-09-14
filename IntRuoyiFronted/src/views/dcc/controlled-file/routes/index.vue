<template>
  <ContentWrap>
    <UnifiedListTemplate
      table-key="dcc.controlledFile.routes.main"
      :query-model="queryParams"
      label-width="82px"
      :filter-definitions="routeQuickFilterDefinitions"
      :quick-filter-state="routeQuickFilter.state"
      :selected-filter-definition="routeQuickFilter.selectedDefinition.value"
      :operator-options="routeQuickFilter.operatorOptions.value"
      :columns="routeColumns"
      :column-saving="routeColumnSaving"
      :show-column-reset="false"
      :total="routeTotal"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="routeQuickFilter.updateState"
      @quick-filter-query="routeQuickFilter.applyQuickFilter"
      @column-change="saveRouteColumnConfig"
      @column-reset="resetRouteColumnConfig"
      @pagination="handleRoutePagination"
    >
      <template #actions>
        <el-button type="primary" @click="handleCreateRoute">
          <Icon icon="ep:plus" class="mr-5px" />
          新增路线
        </el-button>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" />
          查询路线
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.routes.main"
          :data="paginatedRoutes"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          empty-text="暂无审批路线"
          @header-dragend="handleRouteHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRouteColumnVisible('categoryName')"
            label="文件类别"
            prop="categoryName"
            :width="getRouteColumnWidthString('categoryName')"
            :min-width="getRouteColumnMinWidthString('categoryName', 220)"
            v-bind="sortColumnAttrs('categoryName')"
          >
            <template #default="{ row }">
              <div class="route-category">
                <span class="route-category__name">{{ row.categoryName || '未解析类别' }}</span>
                <span class="route-category__meta">类别ID：{{ row.categoryId || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('node1')"
            label="节点1"
            prop="node1"
            :width="getRouteColumnWidthString('node1')"
            :min-width="getRouteColumnMinWidthString('node1', 180)"
            v-bind="sortColumnAttrs('node1')"
          >
            <template #default="{ row }">
              <span class="route-node-assignees">{{ formatRouteNodeAssignees(row, 1) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('node2')"
            label="节点2"
            prop="node2"
            :width="getRouteColumnWidthString('node2')"
            :min-width="getRouteColumnMinWidthString('node2', 260)"
            v-bind="sortColumnAttrs('node2')"
          >
            <template #default="{ row }">
              <span class="route-node-assignees">{{ formatRouteNodeAssignees(row, 2) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('node3')"
            label="节点3"
            prop="node3"
            :width="getRouteColumnWidthString('node3')"
            :min-width="getRouteColumnMinWidthString('node3', 220)"
            v-bind="sortColumnAttrs('node3')"
          >
            <template #default="{ row }">
              <span class="route-node-assignees">{{ formatRouteNodeAssignees(row, 3) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('node4')"
            label="节点4"
            prop="node4"
            :width="getRouteColumnWidthString('node4')"
            :min-width="getRouteColumnMinWidthString('node4', 180)"
            v-bind="sortColumnAttrs('node4')"
          >
            <template #default="{ row }">
              <span class="route-node-assignees">{{ formatRouteNodeAssignees(row, 4) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="128" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleEditRoute(row)">修改</el-button>
              <el-button link type="danger" @click="handleDeleteRoute(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <ContentWrap v-if="queryParams.categoryId">
    <el-alert
      v-if="routePreviewError"
      class="mb-12px"
      :title="routePreviewError"
      type="error"
      :closable="false"
    />
    <UnifiedListTemplate
      table-key="dcc.controlledFile.routes.preview"
      :query-model="previewQueryParams"
      :filter-definitions="routePreviewQuickFilterDefinitions"
      :quick-filter-state="routePreviewQuickFilterState"
      :selected-filter-definition="undefined"
      :operator-options="[]"
      :columns="routePreviewColumns"
      :column-saving="routePreviewColumnSaving"
      :show-query-form="false"
      :total="routePreviewTotal"
      v-model:page="previewQueryParams.pageNo"
      v-model:limit="previewQueryParams.pageSize"
      @column-change="saveRoutePreviewColumnConfig"
      @column-reset="resetRoutePreviewColumnConfig"
      @pagination="handleRoutePreviewPagination"
    >
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.routes.preview"
          :data="paginatedPreviewRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          empty-text="当前尚未生成可用预览"
          @header-dragend="handleRoutePreviewHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRoutePreviewColumnVisible('stageNo')"
            label="阶段号"
            align="center"
            prop="stageNo"
            :width="getRoutePreviewColumnWidthString('stageNo', 90)"
            v-bind="sortColumnAttrs('stageNo')"
          />
          <el-table-column
            v-if="isRoutePreviewColumnVisible('stageName')"
            label="阶段名称"
            prop="stageName"
            :width="getRoutePreviewColumnWidthString('stageName')"
            :min-width="getRoutePreviewColumnMinWidthString('stageName', 180)"
            v-bind="sortColumnAttrs('stageName')"
          />
          <el-table-column
            v-if="isRoutePreviewColumnVisible('approvalMode')"
            label="审批方式"
            prop="approvalMode"
            :width="getRoutePreviewColumnWidthString('approvalMode', 120)"
            v-bind="sortColumnAttrs('approvalMode')"
          >
            <template #default="{ row }">
              {{ getOptionLabel(ROUTE_PREVIEW_MODE_OPTIONS, row.approvalMode) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRoutePreviewColumnVisible('candidateSourceIds')"
            label="岗位集合"
            prop="candidateSourceIds"
            :width="getRoutePreviewColumnWidthString('candidateSourceIds')"
            :min-width="getRoutePreviewColumnMinWidthString('candidateSourceIds', 280)"
            v-bind="sortColumnAttrs('candidateSourceIds')"
          >
            <template #default="{ row }">
              {{ resolvePositionNames(row.candidateSourceIds) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRoutePreviewColumnVisible('resolvedUserIds')"
            label="解析审批人"
            prop="resolvedUserIds"
            :width="getRoutePreviewColumnWidthString('resolvedUserIds')"
            :min-width="getRoutePreviewColumnMinWidthString('resolvedUserIds', 300)"
            v-bind="sortColumnAttrs('resolvedUserIds')"
          >
            <template #default="{ row }">
              {{ resolveUserNames(row.resolvedUserIds) }}
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
  <RouteForm ref="routeFormRef" @success="handleRouteFormSuccess" />
</template>

<script lang="ts" setup>
import {
  deleteApprovalRoute,
  getApprovalRoutePage,
  previewApprovalRoute,
  type ControlledFileApprovalRouteNodeVO,
  type ControlledFileApprovalRoutePreviewVO,
  type ControlledFileApprovalRouteVO
} from '@/api/dcc/controlledFile/approvalRoutes'
import {
  getApprovalPositionList,
  type ControlledFileApprovalPositionVO
} from '@/api/dcc/controlledFile/approvalPositions'
import {
  getFileCategoryList,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { ROUTE_PREVIEW_MODE_OPTIONS, getOptionLabel } from '../shared/options'
import { formatDccSimpleUserLabel, resolveDccPositionName } from '../shared/utils'
import RouteForm from './components/RouteForm.vue'

defineOptions({ name: 'DccControlledFileRoutes' })

const message = useMessage()
const loading = ref(false)
const previewLoading = ref(false)
const routeSubjectLookupsLoaded = ref(false)
const categories = ref<ControlledFileCategoryVO[]>([])
const positions = ref<ControlledFileApprovalPositionVO[]>([])
const users = ref<UserVO[]>([])
const routes = ref<ControlledFileApprovalRouteVO[]>([])
const previewRows = ref<ControlledFileApprovalRoutePreviewVO[]>([])
const routePreviewError = ref('')
const routeTotal = ref(0)
const routeFormRef = ref<InstanceType<typeof RouteForm>>()

const queryParams = reactive<{
  pageNo: number
  pageSize: number
  categoryId?: number
}>({
  pageNo: 1,
  pageSize: 10,
  categoryId: undefined
})

const routeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'categoryName', label: '文件类别', minWidth: 220 },
  { key: 'node1', label: '节点1', minWidth: 180 },
  { key: 'node2', label: '节点2', minWidth: 260 },
  { key: 'node3', label: '节点3', minWidth: 220 },
  { key: 'node4', label: '节点4', minWidth: 180 }
]

const routePreviewDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'stageNo', label: '阶段号', width: 90 },
  { key: 'stageName', label: '阶段名称', minWidth: 180 },
  { key: 'approvalMode', label: '审批方式', width: 120 },
  { key: 'candidateSourceIds', label: '岗位集合', minWidth: 280 },
  { key: 'resolvedUserIds', label: '解析审批人', minWidth: 300 }
]

const {
  saving: routeColumnSaving,
  columns: routeColumns,
  isColumnVisible: isRouteColumnVisible,
  getColumnWidthString: getRouteColumnWidthString,
  getColumnMinWidthString: getRouteColumnMinWidthString,
  handleHeaderDragend: handleRouteHeaderDragend,
  saveConfig: saveRouteColumnConfig,
  resetConfig: resetRouteColumnConfig
} = useUserTableColumns('dcc.controlledFile.routes.main', routeDefaultColumns)

const {
  saving: routePreviewColumnSaving,
  columns: routePreviewColumns,
  isColumnVisible: isRoutePreviewColumnVisible,
  getColumnWidthString: getRoutePreviewColumnWidthString,
  getColumnMinWidthString: getRoutePreviewColumnMinWidthString,
  handleHeaderDragend: handleRoutePreviewHeaderDragend,
  saveConfig: saveRoutePreviewColumnConfig,
  resetConfig: resetRoutePreviewColumnConfig
} = useUserTableColumns('dcc.controlledFile.routes.preview', routePreviewDefaultColumns)

const categoryOptions = computed(() =>
  categories.value.filter(
    (item): item is ControlledFileCategoryVO & { id: number } =>
      item.active && item.id !== undefined
  )
)
const activePositions = computed(() => positions.value.filter((item) => item.active))

const routeQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'categoryId',
    label: '文件类别',
    type: 'select',
    queryParamKey: 'categoryId',
    options: categoryOptions.value.map((item) => ({
      label: item.name || `类别#${item.id}`,
      value: item.id
    })),
    placeholder: '请选择文件类别'
  }
])

const routePreviewQuickFilterDefinitions: TableQuickFilterDefinition[] = []
const routePreviewQuickFilterState = reactive({})
const previewQueryParams = reactive({
  pageNo: 1,
  pageSize: 10
})

const paginatedRoutes = computed(() => routes.value)

const routePreviewTotal = computed(() => previewRows.value.length)
const paginatedPreviewRows = computed(() => {
  const start = (previewQueryParams.pageNo - 1) * previewQueryParams.pageSize
  return previewRows.value.slice(start, start + previewQueryParams.pageSize)
})

const loadInitialCategoryOptions = async () => {
  categories.value = await getFileCategoryList()
}

const loadRouteSubjectLookups = async () => {
  if (routeSubjectLookupsLoaded.value) {
    return
  }
  const [positionList, userList] = await Promise.all([
    getApprovalPositionList(),
    getSimpleUserList()
  ])
  positions.value = positionList
  users.value = userList
  routeSubjectLookupsLoaded.value = true
}

const handleQuery = async (resetPage = true) => {
  if (resetPage) {
    queryParams.pageNo = 1
  }
  loading.value = true
  try {
    await loadRouteSubjectLookups()
    const pageResult = await getApprovalRoutePage(queryParams)
    routes.value = pageResult.list ?? []
    routeTotal.value = pageResult.total ?? 0
    if (!queryParams.categoryId) {
      previewRows.value = []
      routePreviewError.value = ''
      return
    }
    await handlePreview()
  } finally {
    loading.value = false
  }
}

const handleRoutePagination = () => {
  handleQuery(false)
}

const handleRoutePreviewPagination = () => {}

const handleRouteFormSuccess = async () => {
  await handleQuery(false)
}

const resolveSelectedCategory = () =>
  queryParams.categoryId
    ? categoryOptions.value.find((item) => item.id === queryParams.categoryId)
    : undefined

const resolveRouteCategory = (row: ControlledFileApprovalRouteVO) => {
  if (!row.categoryId) {
    return undefined
  }
  return categories.value.find((item) => item.id === row.categoryId)
}

const handleCreateRoute = async () => {
  await loadRouteSubjectLookups()
  routeFormRef.value?.open({
    category: resolveSelectedCategory(),
    categories: categoryOptions.value,
    users: users.value,
    positions: activePositions.value
  })
}

const handleEditRoute = async (row: ControlledFileApprovalRouteVO) => {
  await loadRouteSubjectLookups()
  const category = resolveRouteCategory(row)
  if (!category?.id) {
    message.error('缺少文件类别，无法修改路线')
    return
  }
  routeFormRef.value?.open({
    category: category,
    categories: categoryOptions.value,
    route: row,
    users: users.value,
    positions: activePositions.value
  })
}

const handleDeleteRoute = async (row: ControlledFileApprovalRouteVO) => {
  if (!row.id) {
    message.error('缺少路线版本，无法删除')
    return
  }
  try {
    await message.delConfirm()
  } catch (confirmError) {
    return
  }
  await deleteApprovalRoute(row.id)
  message.success('删除成功')
  await handleQuery(false)
}

const handlePreview = async () => {
  if (!queryParams.categoryId || routes.value.length === 0) {
    previewRows.value = []
    routePreviewError.value = ''
    return
  }
  previewLoading.value = true
  routePreviewError.value = ''
  try {
    await loadRouteSubjectLookups()
    previewRows.value = await previewApprovalRoute({ categoryId: queryParams.categoryId })
    previewQueryParams.pageNo = 1
  } catch (error) {
    previewRows.value = []
    previewQueryParams.pageNo = 1
    routePreviewError.value = resolveErrorMessage(
      error,
      '派生预览失败，请检查文控岗位、会签岗位、批准岗位及其分配。'
    )
  } finally {
    previewLoading.value = false
  }
}

const routeQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.routes.main',
  routeQuickFilterDefinitions,
  queryParams,
  handleQuery
)

const normalizeRouteNodeText = (value?: string | number | null) => {
  if (value === undefined || value === null) {
    return ''
  }
  return String(value).trim()
}

const TECHNICAL_ROUTE_NODE_LABEL_PATTERN = /^(?:审批角色#\d+|[a-z]+(?:-[a-z0-9]+)+)$/i

const isRouteNodeTechnicalLabel = (value: string) =>
  TECHNICAL_ROUTE_NODE_LABEL_PATTERN.test(value.trim())

const getRouteNodeCandidateIds = (node: ControlledFileApprovalRouteNodeVO) => {
  if (node.candidateSourceIds?.length) {
    return node.candidateSourceIds
  }
  return node.candidateSourceId ? [node.candidateSourceId] : []
}

const resolveRouteNodeUserName = (userId: number) => {
  const user = users.value.find((candidate) => candidate.id === userId)
  return user ? formatDccSimpleUserLabel(user) : `用户#${userId}`
}

const resolveRouteNodePositionName = (positionId: number) => {
  const positionName = resolveDccPositionName(positionId, positions.value)
  return isRouteNodeTechnicalLabel(positionName) ? '' : positionName
}

const resolveRouteNodePositionNames = (node: ControlledFileApprovalRouteNodeVO) => {
  const positionNames = getRouteNodeCandidateIds(node)
    .map(resolveRouteNodePositionName)
    .filter(Boolean)
  return positionNames.join('、')
}

const formatRouteNodeSubject = (node: ControlledFileApprovalRouteNodeVO) => {
  if (node.candidateSourceType === 'POSITION') {
    const positionNames = resolveRouteNodePositionNames(node)
    if (positionNames) {
      return positionNames
    }
  }
  const explicitLabel =
    normalizeRouteNodeText(node.subjectLabel) ||
    normalizeRouteNodeText(node.subjectName) ||
    normalizeRouteNodeText(node.subjectDepartmentPath)
  if (explicitLabel && !isRouteNodeTechnicalLabel(explicitLabel)) {
    return explicitLabel
  }
  const candidateIds = getRouteNodeCandidateIds(node)
  if (!candidateIds.length) {
    return '-'
  }
  if (node.candidateSourceType === 'USER') {
    return candidateIds.map(resolveRouteNodeUserName).join('、')
  }
  if (node.candidateSourceType === 'POSITION') {
    return '-'
  }
  return candidateIds.join('、')
}

const formatRouteNodeAssignees = (row: ControlledFileApprovalRouteVO, stageNo: number) => {
  const stageNodes =
    row.nodes?.filter((node) => node.stageNo === stageNo).sort(
      (left, right) =>
        (left.stageOrder ?? left.stageNo) - (right.stageOrder ?? right.stageNo) ||
        left.sort - right.sort
    ) ?? []
  const subjects = [...new Set(stageNodes.map(formatRouteNodeSubject).filter(Boolean))]
  return subjects.length ? subjects.join('、') : '-'
}

const resolvePositionNames = (ids: number[]) => {
  if (!ids?.length) {
    return '-'
  }
  return ids.map((id) => resolveDccPositionName(id, positions.value)).join(' / ')
}

const resolveUserNames = (userIds: number[]) => {
  if (!userIds?.length) {
    return '-'
  }
  return userIds
    .map((id) => {
      const user = users.value.find((candidate) => candidate.id === id)
      return user ? formatDccSimpleUserLabel(user) : `用户#${id}`
    })
    .join(' / ')
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return defaultMessage
}

onMounted(async () => {
  await loadInitialCategoryOptions()
  await handleQuery()
})
</script>

<style scoped>
.route-category {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.route-category__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.route-category__name {
  color: #172033;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-node-assignees {
  color: #263247;
  line-height: 20px;
}
</style>
