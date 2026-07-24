<template>
  <div class="review-matrix-tab">
    <UnifiedListTemplate
      class="review-matrix-list-template"
      table-key="dcc.controlledFile.permission.reviewMatrix"
      :query-model="queryParams"
      label-width="76px"
      :filter-definitions="reviewMatrixQuickFilterDefinitions"
      :quick-filter-state="reviewMatrixQuickFilter.state"
      :selected-filter-definition="reviewMatrixQuickFilter.selectedDefinition.value"
      :operator-options="reviewMatrixQuickFilter.operatorOptions.value"
      :columns="reviewMatrixColumns"
      :column-saving="reviewMatrixColumnSaving"
      :show-column-reset="false"
      :total="reviewMatrixTotal"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="reviewMatrixQuickFilter.updateState"
      @quick-filter-query="reviewMatrixQuickFilter.applyQuickFilter"
      @column-change="saveReviewMatrixColumnConfig"
      @pagination="handleReviewMatrixPagination"
    >
      <template #actions>
        <el-button plain type="primary" @click="openUserLookupDialog">
          <Icon icon="ep:user" class="mr-5px" />
          按人反查
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.permission.reviewMatrix"
          :data="paginatedReviewMatrixRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          class="review-matrix-table review-matrix-table--compact"
          row-key="categoryId"
          data-testid="dcc-review-matrix-table"
          @header-dragend="handleReviewMatrixHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isReviewMatrixColumnVisible('code')"
            label="类别编码"
            prop="code"
            :width="getReviewMatrixColumnWidthString('code')"
            :min-width="getReviewMatrixColumnMinWidthString('code', 140)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('code')"
          />
          <el-table-column
            v-if="isReviewMatrixColumnVisible('name')"
            label="类别名称"
            prop="name"
            :width="getReviewMatrixColumnWidthString('name')"
            :min-width="getReviewMatrixColumnMinWidthString('name', 180)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isReviewMatrixColumnVisible('signoffRules')"
            label="审核规则"
            prop="signoffRules"
            :width="getReviewMatrixColumnWidthString('signoffRules')"
            :min-width="getReviewMatrixColumnMinWidthString('signoffRules', 220)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('signoffRules')"
          >
            <template #default="{ row }">
              <el-tooltip :content="formatStageRuleSummary(row.rules, 'SIGNOFF')" placement="top">
                <span class="matrix-cell-ellipsis">
                  {{ formatStageRuleSummary(row.rules, 'SIGNOFF') }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReviewMatrixColumnVisible('approvalRules')"
            label="批准规则"
            prop="approvalRules"
            :width="getReviewMatrixColumnWidthString('approvalRules')"
            :min-width="getReviewMatrixColumnMinWidthString('approvalRules', 220)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('approvalRules')"
          >
            <template #default="{ row }">
              <el-tooltip :content="formatStageRuleSummary(row.rules, 'APPROVAL')" placement="top">
                <span class="matrix-cell-ellipsis">
                  {{ formatStageRuleSummary(row.rules, 'APPROVAL') }}
                </span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isReviewMatrixColumnVisible('actions')"
            label="操作"
            prop="actions"
            align="center"
            fixed="right"
            :width="getReviewMatrixColumnWidthString('actions', 240)"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openMatrixDialog(row, row.configured ? 'edit' : 'create')"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                {{ row.configured ? '编辑' : '新增' }}
              </el-button>
              <el-button
                link
                type="danger"
                :disabled="!row.configured"
                @click="handleDelete(row)"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                删除
              </el-button>
              <el-button
                link
                type="primary"
                :disabled="!row.configured"
                @click="openMatrixDialog(row, 'preview')"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                预览
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>

    <CategoryMatrixDialog ref="matrixDialogRef" @success="loadRows" />
    <CategoryReviewMatrixUserLookupDialog ref="userLookupDialogRef" />
  </div>
</template>

<script lang="ts" setup>
import {
  deleteCategoryApprovalMatrix,
  getCategoryReviewMatrixRows,
  type ControlledFileCategoryVO,
  type ControlledFileCategoryReviewMatrixRuleVO,
  type ControlledFileCategoryReviewMatrixRowVO
} from '@/api/dcc/controlledFile/fileCategories'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import CategoryMatrixDialog from './CategoryMatrixDialog.vue'
import CategoryReviewMatrixUserLookupDialog from './CategoryReviewMatrixUserLookupDialog.vue'

type MatrixDialogMode = 'create' | 'edit' | 'preview'

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
const matrixDialogRef = ref()
const userLookupDialogRef = ref()
const rows = ref<ControlledFileCategoryReviewMatrixRowVO[]>([])
const message = useMessage()

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

const reviewMatrixQuickFilterDefinitions: TableQuickFilterDefinition[] = [
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

const reviewMatrixDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '类别编码', minWidth: 140 },
  { key: 'name', label: '类别名称', minWidth: 180 },
  { key: 'signoffRules', label: '审核规则', minWidth: 220 },
  { key: 'approvalRules', label: '批准规则', minWidth: 220 },
  { key: 'actions', label: '操作', width: 240, hideable: false }
]

const {
  columns: reviewMatrixColumns,
  saving: reviewMatrixColumnSaving,
  isColumnVisible: isReviewMatrixColumnVisible,
  getColumnWidthString: getReviewMatrixColumnWidthString,
  getColumnMinWidthString: getReviewMatrixColumnMinWidthString,
  handleHeaderDragend: handleReviewMatrixHeaderDragend,
  saveConfig: saveReviewMatrixColumnConfig
} = useUserTableColumns('dcc.controlledFile.permission.reviewMatrix', reviewMatrixDefaultColumns)

const reviewMatrixTotal = computed(() => rows.value.length)
const paginatedReviewMatrixRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return rows.value.slice(start, start + queryParams.pageSize)
})

const formatStageRuleSummary = (
  rules: ControlledFileCategoryReviewMatrixRuleVO[] = [],
  stageType: 'SIGNOFF' | 'APPROVAL'
) => {
  const labels = rules
    .filter((rule) => rule.stageType === stageType && rule.active !== false)
    .map((rule) => {
      const main = rule.subjectLabel || rule.subjectName || rule.subjectDepartmentPath || '-'
      return `${main} ▲`.trim()
    })
  if (!labels.length) {
    return '-'
  }
  return labels.join(' / ')
}

const toCategoryVO = (row: ControlledFileCategoryReviewMatrixRowVO): ControlledFileCategoryVO => ({
  id: row.categoryId,
  code: row.code,
  name: row.name,
  lifecycleStage: row.lifecycleStage,
  active: row.active,
  sort: 0
})

const loadRows = async () => {
  loading.value = true
  try {
    rows.value = await getCategoryReviewMatrixRows({
      code: String(queryParams.code ?? '').trim() || undefined,
      name: String(queryParams.name ?? '').trim() || undefined
    })
  } finally {
    loading.value = false
  }
}

const handleReviewMatrixPagination = () => undefined

const reviewMatrixQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.reviewMatrix',
  reviewMatrixQuickFilterDefinitions,
  queryParams,
  loadRows
)

const openMatrixDialog = (row: ControlledFileCategoryReviewMatrixRowVO, mode: MatrixDialogMode) => {
  matrixDialogRef.value?.open(toCategoryVO(row), mode)
}

const openUserLookupDialog = () => {
  userLookupDialogRef.value?.open()
}

const handleDelete = async (row: ControlledFileCategoryReviewMatrixRowVO) => {
  if (!row.configured) {
    return
  }
  try {
    await message.delConfirm(
      `确认删除类别“${row.name}”当前生效的审阅矩阵吗？删除后该类别后续新提交将因未配置矩阵而失败。`
    )
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteCategoryApprovalMatrix(row.categoryId)
    message.success('审阅矩阵已删除')
    await loadRows()
  } finally {
    loading.value = false
  }
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
.review-matrix-tab {
  display: grid;
  gap: 16px;
}

.review-matrix-toolbar {
  border: 1px solid #dbe3ef;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  background: #fff;
  padding: 16px 16px 1px;
}

.review-matrix-toolbar__form {
  margin-bottom: -15px;
}

.review-matrix-table-shell {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-top: none;
  border-radius: 0 0 8px 8px;
  background: #fff;
}

.review-matrix-table--compact :deep(.el-table__header .el-table__cell) {
  height: 46px;
  padding: 7px 10px;
  background: #f7f9fc;
}

.review-matrix-table--compact :deep(.el-table__body .el-table__row) {
  height: 52px;
}

.review-matrix-table--compact :deep(.el-table__body .el-table__cell) {
  height: 52px;
  padding: 7px 10px;
  vertical-align: middle;
  border-bottom-color: #edf1f6;
}

.review-matrix-table--compact :deep(.el-table__body .el-table__cell .cell) {
  overflow: hidden;
  color: #263247;
  font-size: 0.9rem;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix-cell-ellipsis {
  display: inline-block;
  overflow: hidden;
  max-width: 100%;
  min-width: 0;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.matrix-subject-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
}

.matrix-subject-summary__line {
  flex: 0 1 auto;
  color: #172033;
  font-weight: 600;
}

.matrix-subject-summary__divider {
  flex: 0 0 auto;
  color: #9aa6b2;
}

.matrix-subject-summary__meta {
  flex: 1 1 auto;
  color: #4b5563;
}
</style>
