<template>
  <ContentWrap>
    <el-tabs v-model="activeTab" class="dcc-category-tabs">
      <el-tab-pane label="类别列表" name="list" lazy>
        <div class="category-tab-pane">
          <UnifiedListTemplate
            v-if="isTabPaneMounted('list')"
            class="category-list-template"
            table-key="dcc.controlledFile.permission.categories"
            :query-model="queryParams"
            label-width="76px"
            :filter-definitions="categoryQuickFilterDefinitions"
            :quick-filter-state="categoryQuickFilter.state"
            :selected-filter-definition="categoryQuickFilter.selectedDefinition.value"
            :operator-options="categoryQuickFilter.operatorOptions.value"
            :columns="categoryColumns"
            :column-saving="categoryColumnSaving"
            :show-column-reset="false"
            :total="categoryTotal"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @update:quick-filter-state="categoryQuickFilter.updateState"
            @quick-filter-query="categoryQuickFilter.applyQuickFilter"
            @column-change="saveCategoryColumnConfig"
            @pagination="handleCategoryPagination"
          >
            <template #actions>
              <el-button
                type="primary"
                plain
                @click="openForm('create')"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                <Icon icon="ep:plus" class="mr-5px" />
                新增类别
              </el-button>
            </template>
            <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
              <el-table
                v-loading="loading"
                data-user-table-column-explicit
                data-user-table-key="dcc.controlledFile.permission.categories"
                :data="paginatedCategories"
                border
                :stripe="true"
                :show-overflow-tooltip="true"
                row-key="id"
                @header-dragend="handleCategoryHeaderDragend"
                @sort-change="handleTemplateSortChange"
              >
                <el-table-column
                  v-if="isCategoryColumnVisible('code')"
                  label="类别编码"
                  prop="code"
                  :width="getCategoryColumnWidthString('code')"
                  :min-width="getCategoryColumnMinWidthString('code', 150)"
                  show-overflow-tooltip
                  v-bind="sortColumnAttrs('code')"
                />
                <el-table-column
                  v-if="isCategoryColumnVisible('name')"
                  label="类别名称"
                  prop="name"
                  :width="getCategoryColumnWidthString('name')"
                  :min-width="getCategoryColumnMinWidthString('name', 180)"
                  show-overflow-tooltip
                  v-bind="sortColumnAttrs('name')"
                />
                <el-table-column
                  v-if="isCategoryColumnVisible('lifecycleStage')"
                  label="阶段"
                  prop="taxonomyStageName"
                  :width="getCategoryColumnWidthString('lifecycleStage')"
                  :min-width="getCategoryColumnMinWidthString('lifecycleStage', 150)"
                  v-bind="sortColumnAttrs('lifecycleStage')"
                >
                  <template #default="{ row }">
                    <el-tag :type="getCategoryTaxonomyStageTagType(row)" size="small">
                      {{ formatCategoryTaxonomyStageLabel(row) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCategoryColumnVisible('directory')"
                  label="绑定目录"
                  prop="directory"
                  :width="getCategoryColumnWidthString('directory')"
                  :min-width="getCategoryColumnMinWidthString('directory', 180)"
                  show-overflow-tooltip
                  v-bind="sortColumnAttrs('directory')"
                >
                  <template #default="{ row }">
                    {{ row.directoryId ? directoryPathMap.get(row.directoryId) || '-' : '-' }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCategoryColumnVisible('fileTypeTaxonomy')"
                  label="默认文件分类"
                  prop="fileTypeTaxonomy"
                  :width="getCategoryColumnWidthString('fileTypeTaxonomy')"
                  :min-width="getCategoryColumnMinWidthString('fileTypeTaxonomy', 240)"
                  show-overflow-tooltip
                  v-bind="sortColumnAttrs('fileTypeTaxonomy')"
                >
                  <template #default="{ row }">
                    {{ row.fileTypeTaxonomyId ? taxonomyPathMap.get(row.fileTypeTaxonomyId) || '-' : '-' }}
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCategoryColumnVisible('governance')"
                  label="治理摘要"
                  prop="governance"
                  :width="getCategoryColumnWidthString('governance')"
                  :min-width="getCategoryColumnMinWidthString('governance', 260)"
                  v-bind="sortColumnAttrs('governance')"
                >
                  <template #default="{ row }">
                    <div
                      class="category-governance-summary"
                      data-testid="dcc-category-governance-summary"
                    >
                      <div class="category-governance-summary__line">
                        <el-tag :type="getBooleanTagType(row.active)" size="small">
                          启用：{{ formatBooleanLabel(row.active) }}
                        </el-tag>
                        <el-tag
                          :type="getRequirementTagType(row.distributionRequired, 'primary')"
                          size="small"
                        >
                          分发：{{ formatRequirementLabel(row.distributionRequired) }}
                        </el-tag>
                        <el-tag
                          :type="getRequirementTagType(row.trainingRequired, 'warning')"
                          size="small"
                        >
                          培训：{{ formatRequirementLabel(row.trainingRequired) }}
                        </el-tag>
                      </div>
                      <div class="category-governance-summary__meta">
                        创建：{{ formatDateTimeValue(row.createTime, '-') }}
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isCategoryColumnVisible('actions')"
                  label="操作"
                  prop="actions"
                  align="center"
                  fixed="right"
                  :width="getCategoryColumnWidthString('actions', 220)"
                >
                  <template #default="{ row }">
                    <el-button
                      link
                      type="primary"
                      @click="openCategoryUploadPolicyDialog(row)"
                      v-hasPermi="['dcc:controlled-file:category:manage']"
                    >
                      上传策略
                    </el-button>
                    <el-button
                      link
                      type="primary"
                      @click="openForm('update', row)"
                      v-hasPermi="['dcc:controlled-file:category:manage']"
                    >
                      编辑
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      @click="handleDelete(row)"
                      v-hasPermi="['dcc:controlled-file:category:manage']"
                    >
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </div>
      </el-tab-pane>

      <el-tab-pane label="审阅矩阵" name="review-matrix" lazy>
        <CategoryReviewMatrixTable
          v-if="isTabPaneMounted('review-matrix')"
          :active="activeTab === 'review-matrix'"
          :category-revision="categoryRevision"
        />
      </el-tab-pane>

      <el-tab-pane label="查看矩阵" name="view-matrix" lazy>
        <CategoryViewMatrixTable
          v-if="isTabPaneMounted('view-matrix')"
          :active="activeTab === 'view-matrix'"
          :category-revision="categoryRevision"
        />
      </el-tab-pane>

      <el-tab-pane label="目录授权" name="directory-auth" lazy>
        <DirectoryAuthorizationTabPanel
          v-if="isTabPaneMounted('directory-auth')"
          :initial-directory-id="currentDirectoryId"
          :active="activeTab === 'directory-auth'"
          :category-revision="categoryRevision"
        />
      </el-tab-pane>

      <el-tab-pane label="分发规则" name="distribution-rules" lazy>
        <CategoryDistributionRulesTab
          v-if="isTabPaneMounted('distribution-rules')"
          :active="activeTab === 'distribution-rules'"
          :category-revision="categoryRevision"
        />
      </el-tab-pane>

      <el-tab-pane label="培训规则" name="training-rules" lazy>
        <CategoryTrainingRulesTab
          v-if="isTabPaneMounted('training-rules')"
          :active="activeTab === 'training-rules'"
          :category-revision="categoryRevision"
        />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <CategoryForm v-if="categoryFormMounted" ref="formRef" @success="loadData" />
  <CategoryUploadSizePolicyDialog
    v-if="categoryUploadPolicyDialogMounted"
    ref="categoryUploadPolicyDialogRef"
  />
</template>

<script lang="ts" setup>
import { formatDateTimeValue } from '@/utils/formatTime'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { handleTree } from '@/utils/tree'
import type { ControlledFileDirectoryVO } from '@/api/dcc/controlledFile/directories'
import { getDirectoryTree } from '@/api/dcc/controlledFile/directories'
import {
  deleteFileCategory,
  getFileCategoryList,
  type ControlledFileCategoryVO
} from '@/api/dcc/controlledFile/fileCategories'
import {
  getFileTypeTaxonomyList,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import { ACTIVE_STATUS_OPTIONS } from '../shared/options'
import {
  buildDccFileTypeTaxonomyPathMap,
  buildDccFileTypeTaxonomyStageNameMap,
  getDccFileTypeTaxonomyStageRows,
  getDccFileTypeTaxonomyStageTagType,
  resolveDccFileTypeTaxonomyStageName,
  toDccFileTypeTaxonomyStageOptions
} from '../shared/file-type-taxonomy-stage'
import {
  formatRequirementLabel,
  formatBooleanLabel,
  getRequirementTagType,
  getBooleanTagType
} from '../shared/utils'

defineOptions({ name: 'DccControlledFileCategories' })

const CategoryForm = defineAsyncComponent(() => import('./components/CategoryForm.vue'))
const CategoryUploadSizePolicyDialog = defineAsyncComponent(
  () => import('./components/CategoryUploadSizePolicyDialog.vue')
)
const CategoryReviewMatrixTable = defineAsyncComponent(
  () => import('./components/CategoryReviewMatrixTable.vue')
)
const CategoryViewMatrixTable = defineAsyncComponent(
  () => import('./components/CategoryViewMatrixTable.vue')
)
const DirectoryAuthorizationTabPanel = defineAsyncComponent(
  () => import('../components/DirectoryAuthorizationTabPanel.vue')
)
const CategoryDistributionRulesTab = defineAsyncComponent(
  () => import('./components/CategoryDistributionRulesTab.vue')
)
const CategoryTrainingRulesTab = defineAsyncComponent(
  () => import('./components/CategoryTrainingRulesTab.vue')
)

interface CategoryTreeNode extends ControlledFileCategoryVO {
  children?: CategoryTreeNode[]
}

interface CategoryListRow extends ControlledFileCategoryVO {
  taxonomyStageName?: string
}

const route = useRoute()
const router = useRouter()
const TAB_NAMES = [
  'list',
  'review-matrix',
  'view-matrix',
  'directory-auth',
  'distribution-rules',
  'training-rules'
] as const
type PermissionTabName = (typeof TAB_NAMES)[number]

const resolveActiveTab = (tab: unknown): PermissionTabName => {
  return TAB_NAMES.includes(tab as PermissionTabName) ? (tab as PermissionTabName) : 'list'
}

const activeTab = ref<PermissionTabName>(resolveActiveTab(route.query.tab))
const loadedTabNames = ref<Set<PermissionTabName>>(new Set([activeTab.value]))
const loading = ref(false)
const formRef = ref()
const categoryUploadPolicyDialogRef = ref()
const categoryFormMounted = ref(false)
const categoryUploadPolicyDialogMounted = ref(false)
const categoryListLoaded = ref(false)
const categories = ref<ControlledFileCategoryVO[]>([])
const categoryRevision = ref(0)
const directories = ref<ControlledFileDirectoryVO[]>([])
const fileTypeTaxonomies = ref<DccFileTypeTaxonomyVO[]>([])
const message = useMessage()
const currentDirectoryId = computed(() => parsePositiveRouteQueryId(route.query.directoryId) || undefined)

const queryParams = reactive<{
  code?: string
  name?: string
  taxonomyStageName?: string
  active?: boolean
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  code: '',
  name: '',
  taxonomyStageName: undefined,
  active: undefined,
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined
})

const markTabPaneMounted = (tab: PermissionTabName) => {
  if (loadedTabNames.value.has(tab)) {
    return
  }
  loadedTabNames.value = new Set([...loadedTabNames.value, tab])
}

const isTabPaneMounted = (tab: PermissionTabName) => loadedTabNames.value.has(tab)

const buildDirectoryPathMap = (
  nodes: ControlledFileDirectoryVO[],
  parentPath = ''
): Map<number, string> => {
  const pathMap = new Map<number, string>()
  nodes.forEach((node) => {
    if (!node.id) {
      return
    }
    const currentPath = parentPath ? `${parentPath}/${node.name}` : node.name
    pathMap.set(node.id, currentPath)
    if (node.children?.length) {
      buildDirectoryPathMap(node.children, currentPath).forEach((value, key) => {
        pathMap.set(key, value)
      })
    }
  })
  return pathMap
}

const directoryPathMap = computed(() => buildDirectoryPathMap(directories.value))
const taxonomyPathMap = computed(() => buildDccFileTypeTaxonomyPathMap(fileTypeTaxonomies.value))
const taxonomyStageRows = computed(() => getDccFileTypeTaxonomyStageRows(fileTypeTaxonomies.value))
const taxonomyStageNameMap = computed(() =>
  buildDccFileTypeTaxonomyStageNameMap(fileTypeTaxonomies.value)
)
const categoryTaxonomyStageOptions = computed(() =>
  toDccFileTypeTaxonomyStageOptions(taxonomyStageRows.value)
)
const resolveCategoryTaxonomyStageName = (row: ControlledFileCategoryVO | CategoryListRow) => {
  return resolveDccFileTypeTaxonomyStageName(row, taxonomyStageNameMap.value)
}
const categoryRowsWithTaxonomyStage = computed<CategoryListRow[]>(() =>
  categories.value.map((item) => ({
    ...item,
    taxonomyStageName: resolveCategoryTaxonomyStageName(item)
  }))
)
const categoryTreeOptions = computed(
  () => handleTree(categories.value.map((item) => ({ ...item }))) as CategoryTreeNode[]
)

const categoryQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
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
  },
  {
    key: 'taxonomyStageName',
    label: '阶段',
    type: 'select',
    queryParamKey: 'taxonomyStageName',
    options: categoryTaxonomyStageOptions.value
  },
  {
    key: 'active',
    label: '启用状态',
    type: 'select',
    queryParamKey: 'active',
    options: ACTIVE_STATUS_OPTIONS.map((item) => ({
      label: item.label,
      value: item.value
    }))
  }
])

const categoryDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '类别编码', minWidth: 150 },
  { key: 'name', label: '类别名称', minWidth: 180 },
  { key: 'lifecycleStage', label: '阶段', minWidth: 150 },
  { key: 'directory', label: '绑定目录', minWidth: 180 },
  { key: 'fileTypeTaxonomy', label: '默认文件分类', minWidth: 240 },
  { key: 'governance', label: '治理摘要', minWidth: 260 },
  { key: 'actions', label: '操作', width: 220, hideable: false }
]

const {
  columns: categoryColumns,
  saving: categoryColumnSaving,
  isColumnVisible: isCategoryColumnVisible,
  getColumnWidthString: getCategoryColumnWidthString,
  getColumnMinWidthString: getCategoryColumnMinWidthString,
  handleHeaderDragend: handleCategoryHeaderDragend,
  saveConfig: saveCategoryColumnConfig
} = useUserTableColumns('dcc.controlledFile.permission.categories', categoryDefaultColumns)

const filteredCategories = computed(() => {
  const codeKeyword = String(queryParams.code || '').trim().toLowerCase()
  const nameKeyword = String(queryParams.name || '').trim().toLowerCase()
  return categoryRowsWithTaxonomyStage.value.filter((item) => {
    const itemCode = String(item.code || '').toLowerCase()
    const itemName = String(item.name || '').toLowerCase()
    const codeMatch = !codeKeyword || itemCode.includes(codeKeyword)
    const nameMatch = !nameKeyword || itemName.includes(nameKeyword)
    const lifecycleStageMatch =
      !queryParams.taxonomyStageName ||
      resolveCategoryTaxonomyStageName(item) === queryParams.taxonomyStageName
    const activeMatch = queryParams.active === undefined || item.active === queryParams.active
    return codeMatch && nameMatch && lifecycleStageMatch && activeMatch
  })
})

const categoryTotal = computed(() => filteredCategories.value.length)
const paginatedCategories = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredCategories.value.slice(start, start + queryParams.pageSize)
})

const formatCategoryTaxonomyStageLabel = (row: ControlledFileCategoryVO | CategoryListRow) => {
  return resolveCategoryTaxonomyStageName(row) || '-'
}

const getCategoryTaxonomyStageTagType = (row: ControlledFileCategoryVO | CategoryListRow) => {
  const stageName = resolveCategoryTaxonomyStageName(row)
  return getDccFileTypeTaxonomyStageTagType(stageName)
}

const loadData = async () => {
  loading.value = true
  try {
    const [categoryList, directoryTree, taxonomyList] = await Promise.all([
      getFileCategoryList(),
      getDirectoryTree(),
      getFileTypeTaxonomyList()
    ])
    categories.value = categoryList
    categoryListLoaded.value = true
    categoryRevision.value += 1
    directories.value = directoryTree
    fileTypeTaxonomies.value = taxonomyList
  } finally {
    loading.value = false
  }
}

const ensureActiveTabLoaded = async (tab: PermissionTabName) => {
  markTabPaneMounted(tab)
  if (tab !== 'list') {
    return
  }
  if (!categoryListLoaded.value) {
    await loadData()
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
}

const handleCategoryPagination = () => undefined

const categoryQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.permission.categories',
  categoryQuickFilterDefinitions,
  queryParams,
  handleQuery
)

const openForm = async (type: 'create' | 'update', row?: ControlledFileCategoryVO) => {
  categoryFormMounted.value = true
  loading.value = true
  try {
    await import('./components/CategoryForm.vue')
    const latestDirectoryTree = await getDirectoryTree()
    directories.value = latestDirectoryTree
    await nextTick()
    if (!formRef.value?.open) {
      throw new Error('文件类别表单组件加载失败')
    }
    formRef.value.open(type, {
      row,
      categories: categoryTreeOptions.value,
      directories: latestDirectoryTree
    })
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '受控目录加载失败'
    message.error(errorMessage)
    throw error
  } finally {
    loading.value = false
  }
}

const waitForCategoryUploadPolicyDialogRef = async () => {
  for (let index = 0; index < 10; index += 1) {
    await nextTick()
    if (categoryUploadPolicyDialogRef.value?.open) {
      return categoryUploadPolicyDialogRef.value
    }
    await new Promise((resolve) => window.setTimeout(resolve, 0))
  }
  return categoryUploadPolicyDialogRef.value
}

const openCategoryUploadPolicyDialog = async (row: ControlledFileCategoryVO) => {
  categoryUploadPolicyDialogMounted.value = true
  await import('./components/CategoryUploadSizePolicyDialog.vue')
  const dialogRef = await waitForCategoryUploadPolicyDialogRef()
  if (!dialogRef?.open) {
    throw new Error('类别上传策略弹窗组件加载失败')
  }
  dialogRef.open(row)
}


const handleDelete = async (row: ControlledFileCategoryVO) => {
  if (!row.id) {
    return
  }
  try {
    await message.delConfirm(`确认删除文件类别“${row.name}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteFileCategory(row.id)
    message.success('删除成功')
    await loadData()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await ensureActiveTabLoaded(activeTab.value)
})

watch(
  () => route.query.tab,
  (tab) => {
    const resolvedTab = resolveActiveTab(tab)
    if (activeTab.value !== resolvedTab) {
      activeTab.value = resolvedTab
    }
  }
)

watch(activeTab, async (tab) => {
  await ensureActiveTabLoaded(tab)
  if (route.query.tab === tab) {
    return
  }
  await router.replace({
    path: route.path,
    query: {
      ...route.query,
      tab
    }
  })
})
</script>

<style scoped>
.dcc-category-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.category-tab-pane {
  display: grid;
  gap: 0;
}

.category-toolbar {
  border: 1px solid #dbe3ef;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  background: #fff;
  padding: 16px 16px 1px;
}

.category-toolbar__form {
  margin-bottom: -15px;
}

.category-table-shell {
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-top: none;
  border-radius: 0 0 8px 8px;
  background: #fff;
}

.category-governance-summary {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.category-governance-summary__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.category-governance-summary__meta {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}
</style>
