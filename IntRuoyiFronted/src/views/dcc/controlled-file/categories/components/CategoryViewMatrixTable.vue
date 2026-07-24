<template>
  <div class="view-matrix-tab">
    <UnifiedListTemplate
      class="view-matrix-list-template"
      table-key="dcc.controlledFile.permission.viewMatrix"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="viewMatrixQuickFilterDefinitions"
      :quick-filter-state="viewMatrixQuickFilter.state"
      :selected-filter-definition="viewMatrixQuickFilter.selectedDefinition.value"
      :operator-options="viewMatrixQuickFilter.operatorOptions.value"
      :columns="viewMatrixColumns"
      :column-saving="viewMatrixColumnSaving"
      :show-column-reset="false"
      :total="viewMatrixTotal"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="viewMatrixQuickFilter.updateState"
      @quick-filter-query="viewMatrixQuickFilter.applyQuickFilter"
      @column-change="saveViewMatrixColumnConfig"
      @pagination="handleViewMatrixPagination"
    >
      <template #actions>
        <el-button
          plain
          type="primary"
          data-testid="dcc-view-matrix-user-lookup"
          @click="openUserLookupDialog"
        >
          <Icon icon="ep:user" class="mr-5px" />
          按人反查
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.permission.viewMatrix"
          :data="paginatedViewMatrixRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          class="view-matrix-table view-matrix-table--compact"
          row-key="categoryId"
          data-testid="dcc-view-matrix-table"
          @header-dragend="handleViewMatrixHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isViewMatrixColumnVisible('code')"
            label="类别编码"
            prop="code"
            :width="getViewMatrixColumnWidthString('code')"
            :min-width="getViewMatrixColumnMinWidthString('code', 140)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('code')"
          />
          <el-table-column
            v-if="isViewMatrixColumnVisible('name')"
            label="类别名称"
            prop="name"
            :width="getViewMatrixColumnWidthString('name')"
            :min-width="getViewMatrixColumnMinWidthString('name', 190)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('name')"
          >
            <template #default="{ row }">
              <span
                class="view-matrix-category-name"
                :class="getCategoryNameStatusClass(row)"
              >
                {{ row.name }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isViewMatrixColumnVisible('viewRules')"
            label="可查阅"
            prop="viewRules"
            :width="getViewMatrixColumnWidthString('viewRules')"
            :min-width="getViewMatrixColumnMinWidthString('viewRules', 300)"
            v-bind="sortColumnAttrs('viewRules')"
          >
            <template #default="{ row }">
              <el-tooltip :content="formatRules(row.rules)" placement="top">
                <span class="view-matrix-cell-ellipsis">
                  {{ formatRules(row.rules) }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isViewMatrixColumnVisible('actions')"
            label="操作"
            prop="actions"
            align="center"
            fixed="right"
            :width="getViewMatrixColumnWidthString('actions', 160)"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                data-testid="dcc-view-matrix-edit"
                @click="openMatrixDialog(row)"
              >
                {{ row.configured ? '编辑' : '新增' }}
              </el-button>
              <el-button
                link
                type="primary"
                data-testid="dcc-view-matrix-effective-preview"
                @click="openPreview(row)"
              >
                预览
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <Dialog v-model="previewVisible" title="有效权限预览" width="980px">
      <div v-if="previewData" class="view-matrix-preview">
        <el-alert
          v-if="previewData.blocking"
          class="mb-12px"
          title="当前查看矩阵存在阻塞风险，保存前必须修正。"
          type="error"
          :closable="false"
        />
        <el-table :data="previewData.viewSubjects || []" data-testid="dcc-view-matrix-effective-users">
          <el-table-column label="用户" min-width="160">
            <template #default="{ row }">
              {{ row.userName || `用户#${row.userId}` }}
            </template>
          </el-table-column>
          <el-table-column label="来源" min-width="260">
            <template #default="{ row }">
              {{ row.subjectLabel || row.subjectName || '-' }} / {{ row.reason || row.source }}
            </template>
          </el-table-column>
          <el-table-column label="查阅标记" width="120">
            <template #default="{ row }">
              {{ row.marker || '-' }}
            </template>
          </el-table-column>
        </el-table>
        <el-table
          class="mt-12px"
          :data="previewData.risks || []"
          data-testid="dcc-view-matrix-preview-risks"
        >
          <el-table-column label="风险码" width="220" prop="code" />
          <el-table-column label="说明" min-width="420" prop="message" show-overflow-tooltip />
          <el-table-column label="阻塞" width="100">
            <template #default="{ row }">
              <el-tag :type="row.blocking ? 'danger' : 'warning'" size="small">
                {{ row.blocking ? '阻塞' : '提示' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </Dialog>

    <CategoryViewMatrixDialog ref="matrixDialogRef" @success="loadRows" />
    <CategoryViewMatrixUserLookupDialog ref="userLookupDialogRef" />
  </div>
</template>

<script lang="ts" setup>
import {
  getCategoryViewMatrixRows,
  previewCategoryViewMatrixEffectiveAccess,
  type ControlledFileCategoryViewMatrixEffectivePreviewVO,
  type ControlledFileCategoryViewMatrixRowVO
} from '@/api/dcc/controlledFile/fileCategories'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import CategoryViewMatrixDialog from './CategoryViewMatrixDialog.vue'
import CategoryViewMatrixUserLookupDialog from './CategoryViewMatrixUserLookupDialog.vue'
import {
  resolveViewMatrixDepartmentRecognitionStatus,
  type ViewMatrixDepartmentRecognitionStatus
} from './viewMatrixDepartmentMatcher'

defineOptions({ name: 'CategoryViewMatrixTable' })

const props = withDefaults(
  defineProps<{
    active?: boolean
    categoryRevision?: number
  }>(),
  {
    active: true,
    categoryRevision: 0
  }
)

const loading = ref(false)
const rows = ref<ControlledFileCategoryViewMatrixRowVO[]>([])
const previewVisible = ref(false)
const previewData = ref<ControlledFileCategoryViewMatrixEffectivePreviewVO | null>(null)
const matrixDialogRef = ref<InstanceType<typeof CategoryViewMatrixDialog>>()
const userLookupDialogRef = ref<InstanceType<typeof CategoryViewMatrixUserLookupDialog>>()
const message = useMessage()

const departmentOptions = ref<DeptVO[]>([])

const queryParams = reactive<{
  code?: string
  name?: string
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  code: '',
  name: '',
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})

const viewMatrixQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'code',
    label: '类别编码',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入类别编码'
  },
  {
    key: 'name',
    label: '类别名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入类别名称'
  }
]

const viewMatrixDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '类别编码', minWidth: 140 },
  { key: 'name', label: '类别名称', minWidth: 190 },
  { key: 'viewRules', label: '可查阅', minWidth: 300 },
  { key: 'actions', label: '操作', width: 160, hideable: false }
]

const {
  columns: viewMatrixColumns,
  saving: viewMatrixColumnSaving,
  isColumnVisible: isViewMatrixColumnVisible,
  getColumnWidthString: getViewMatrixColumnWidthString,
  getColumnMinWidthString: getViewMatrixColumnMinWidthString,
  handleHeaderDragend: handleViewMatrixHeaderDragend,
  saveConfig: saveViewMatrixColumnConfig
} = useUserTableColumns('dcc.controlledFile.permission.viewMatrix', viewMatrixDefaultColumns)

const viewMatrixTotal = computed(() => rows.value.length)
const paginatedViewMatrixRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return rows.value.slice(start, start + queryParams.pageSize)
})

const formatRules = (rules?: ControlledFileCategoryViewMatrixRowVO['rules']) => {
  if (!rules?.length) {
    return '未配置查看矩阵规则'
  }
  return rules.map(formatRuleSubject).join(' / ')
}

const formatRuleSubject = (rule: NonNullable<ControlledFileCategoryViewMatrixRowVO['rules']>[number]) =>
  `${rule.subjectLabel || rule.subjectName || '-'} ${rule.marker || ''}`.trim()

const resolveCategoryNameRecognitionStatus = (
  row: ControlledFileCategoryViewMatrixRowVO
): ViewMatrixDepartmentRecognitionStatus => {
  if (!departmentOptions.value.length) {
    return 'recognized-none'
  }
  return resolveViewMatrixDepartmentRecognitionStatus(
    row.rules || [],
    departmentOptions.value
  )
}

const getCategoryNameStatusClass = (row: ControlledFileCategoryViewMatrixRowVO) => {
  return `view-matrix-category-name--${resolveCategoryNameRecognitionStatus(row)}`
}

const loadRows = async () => {
  loading.value = true
  try {
    const [nextRows, departments] = await Promise.all([
      getCategoryViewMatrixRows({
        code: String(queryParams.code ?? '').trim() || undefined,
        name: String(queryParams.name ?? '').trim() || undefined
      }),
      getSimpleDeptList()
    ])
    if (!departments.length) {
      throw new Error('当前租户没有可用于查看矩阵识别状态的部门树')
    }
    departmentOptions.value = departments
    rows.value = nextRows
  } catch (error) {
    rows.value = []
    message.error(
      error instanceof Error && error.message && error.message !== 'error'
        ? error.message
        : '查看矩阵加载失败，请根据后端错误提示修正后重试。'
    )
  } finally {
    loading.value = false
  }
}

const handleViewMatrixPagination = () => undefined

const viewMatrixQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.viewMatrix',
  viewMatrixQuickFilterDefinitions,
  queryParams,
  loadRows
)

const openPreview = async (row: ControlledFileCategoryViewMatrixRowVO) => {
  try {
    previewData.value = await previewCategoryViewMatrixEffectiveAccess(row.categoryId, {
      rules: row.rules || []
    })
    previewVisible.value = true
  } catch (error) {
    message.error(
      error instanceof Error && error.message && error.message !== 'error'
        ? error.message
        : '有效权限预览失败，请先修正查看矩阵规则。'
    )
  }
}

const openMatrixDialog = (row: ControlledFileCategoryViewMatrixRowVO) => {
  matrixDialogRef.value?.open(row)
}

const openUserLookupDialog = () => {
  userLookupDialogRef.value?.open()
}

watch(
  () => [props.active, props.categoryRevision] as const,
  async ([active]) => {
    if (!active) {
      return
    }
    await loadRows()
  }
)

onMounted(async () => {
  if (props.active) {
    await loadRows()
  }
})
</script>

<style scoped>
.view-matrix-tab {
  width: 100%;
}

.view-matrix-toolbar {
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 12px 12px 0;
}

.view-matrix-toolbar__form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
}

.view-matrix-table-shell {
  border: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
  background: #ffffff;
}

.view-matrix-table--compact :deep(.el-table__header .el-table__cell) {
  height: 46px;
  padding: 7px 10px;
  background: #f7f9fc;
}

.view-matrix-table--compact :deep(.el-table__body .el-table__row) {
  height: 52px;
}

.view-matrix-table--compact :deep(.el-table__body .el-table__cell) {
  padding: 7px 10px;
}

.view-matrix-cell-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.view-matrix-category-name {
  font-weight: 500;
}

.view-matrix-category-name--recognized-all {
  color: #15803d;
}

.view-matrix-category-name--recognized-partial {
  color: #b45309;
}

.view-matrix-category-name--recognized-none {
  color: #dc2626;
}

.view-matrix-preview {
  min-height: 260px;
}
</style>
