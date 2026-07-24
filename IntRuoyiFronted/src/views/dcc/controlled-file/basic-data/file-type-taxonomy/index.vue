<template>
  <ContentWrap>
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">DCC文件分类</span>
      <el-tag effect="plain" size="small">五级分类</el-tag>
    </div>

    <UnifiedListTemplate
      class="dcc-file-type-taxonomy-list-template"
      table-key="dcc.fileTypeTaxonomy.main"
      :query-model="query"
      label-width="76px"
      :filter-definitions="taxonomyQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="taxonomyQuickFilter.state"
      :selected-filter-definition="taxonomyQuickFilter.selectedDefinition.value"
      :operator-options="taxonomyQuickFilter.operatorOptions.value"
      :columns="taxonomyColumns"
      :column-saving="taxonomyColumnSaving"
      :show-column-reset="false"
      :total="taxonomyTotal"
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      @update:quick-filter-state="taxonomyQuickFilter.updateState"
      @quick-filter-query="taxonomyQuickFilter.applyQuickFilter"
      @column-change="saveTaxonomyColumnConfig"
      @pagination="handlePagination"
    >
      <template #actions>
        <el-button type="primary" plain @click="openForm('create')" v-hasPermi="['dcc:controlled-file:category:manage']">
          <Icon icon="ep:plus" class="mr-5px" />
          新增一级
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="dcc-file-type-taxonomy-resizable-table"
          data-user-table-column-explicit
          data-user-table-key="dcc.fileTypeTaxonomy.main"
          :data="paginatedTreeRows"
          border
          :allow-drag-last-column="true"
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          default-expand-all
          :tree-props="taxonomyTreeProps"
          @header-dragend="handleTaxonomyHeaderDragend"
        >
          <el-table-column
            label="分类名称"
            prop="name"
            :width="getTaxonomyColumnWidthString('name')"
            :min-width="getTaxonomyColumnMinWidthString('name', 220)"
          />
          <el-table-column
            v-if="isTaxonomyColumnVisible('code')"
            label="分类编码"
            prop="code"
            :width="getTaxonomyColumnWidthString('code')"
            :min-width="getTaxonomyColumnMinWidthString('code', 150)"
          />
          <el-table-column
            v-if="isTaxonomyColumnVisible('levelNo')"
            label="层级"
            prop="levelNo"
            :width="getTaxonomyColumnWidthString('levelNo', 82)"
            align="center"
          >
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.levelNo }}级</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTaxonomyColumnVisible('taxonomyPath')"
            label="完整路径"
            prop="taxonomyPath"
            :width="getTaxonomyColumnWidthString('taxonomyPath')"
            :min-width="getTaxonomyColumnMinWidthString('taxonomyPath', 300)"
          >
            <template #default="{ row }">
              {{ row.id ? taxonomyPathMap.get(row.id) || row.name : row.name }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTaxonomyColumnVisible('active')"
            label="状态"
            prop="active"
            :width="getTaxonomyColumnWidthString('active', 96)"
            align="center"
          >
            <template #default="{ row }">
              <el-tag :type="row.active ? 'success' : 'info'" size="small">
                {{ row.active ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isTaxonomyColumnVisible('sort')"
            label="排序"
            prop="sort"
            :width="getTaxonomyColumnWidthString('sort', 90)"
            align="right"
          />
          <el-table-column
            v-if="isTaxonomyColumnVisible('remark')"
            label="备注"
            prop="remark"
            :width="getTaxonomyColumnWidthString('remark')"
            :min-width="getTaxonomyColumnMinWidthString('remark', 180)"
          />
          <el-table-column
            label="操作"
            prop="actions"
            fixed="right"
            :width="getTaxonomyColumnWidthString('actions', 210)"
            align="center"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                :disabled="row.levelNo >= 5"
                @click="openForm('create', undefined, row)"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                新增下级
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
  </ContentWrap>

  <Dialog v-model="formVisible" :title="formTitle" width="640px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="104px"
    >
      <el-form-item label="上级分类" prop="parentId">
        <el-cascader
          v-model="formData.parentId"
          class="!w-100%"
          :disabled="formType === 'update' || rootCreateMode"
          :options="parentOptions"
          :props="taxonomyCascaderProps"
          clearable
          filterable
          placeholder="不选择则创建一级分类"
        />
      </el-form-item>
      <el-form-item label="分类编码" prop="code">
        <el-input v-model="formData.code" maxlength="64" :disabled="rootCreateMode" placeholder="请输入分类编码" />
      </el-form-item>
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="formData.name" maxlength="128" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item label="启用状态" prop="active">
        <el-radio-group v-model="formData.active">
          <el-radio-button :label="true">启用</el-radio-button>
          <el-radio-button :label="false">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" class="!w-220px" :disabled="rootCreateMode" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" maxlength="255" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="submitForm">保存</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import type { FormRules } from 'element-plus'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { handleTree } from '@/utils/tree'
import {
  createFileTypeTaxonomy,
  deleteFileTypeTaxonomy,
  getFileTypeTaxonomyList,
  updateFileTypeTaxonomy,
  type DccFileTypeTaxonomyVO
} from '@/api/dcc/controlledFile/fileTypeTaxonomies'

defineOptions({ name: 'DccFileTypeTaxonomyBasicDataPage' })

const taxonomyQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    operators: ['contains'],
    placeholder: '搜索编码、名称或路径'
  }
]

const taxonomyDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'name', label: '分类名称', minWidth: 220, hideable: false },
  { key: 'code', label: '分类编码', minWidth: 150 },
  { key: 'levelNo', label: '层级', width: 82 },
  { key: 'taxonomyPath', label: '完整路径', minWidth: 300 },
  { key: 'active', label: '状态', width: 96 },
  { key: 'sort', label: '排序', width: 90 },
  { key: 'remark', label: '备注', minWidth: 180 },
  { key: 'actions', label: '操作', width: 210, hideable: false, business: false }
]

const {
  columns: taxonomyColumns,
  saving: taxonomyColumnSaving,
  isColumnVisible: isTaxonomyColumnVisible,
  getColumnWidthString: getTaxonomyColumnWidthString,
  getColumnMinWidthString: getTaxonomyColumnMinWidthString,
  handleHeaderDragend: handleTaxonomyHeaderDragend,
  saveConfig: saveTaxonomyColumnConfig
} = useUserTableColumns('dcc.fileTypeTaxonomy.main', taxonomyDefaultColumns)

interface TaxonomyFormData {
  id?: number
  parentId?: number | null
  code: string
  name: string
  active: boolean
  sort: number
  remark?: string
}

const message = useMessage()
const loading = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const rootCreateMode = ref(false)
const formRef = ref()
const rows = ref<DccFileTypeTaxonomyVO[]>([])
const query = reactive<{
  pageNo: number
  pageSize: number
  keyword: string
}>({
  pageNo: 1,
  pageSize: 20,
  keyword: ''
})

const handleQuery = () => {
  query.pageNo = 1
}

const handlePagination = () => undefined

const taxonomyQuickFilter = useTableQuickFilter(
  'dcc.fileTypeTaxonomy.main',
  taxonomyQuickFilterDefinitions,
  query,
  handleQuery
)

const formData = reactive<TaxonomyFormData>({
  parentId: undefined,
  code: '',
  name: '',
  active: true,
  sort: 0,
  remark: ''
})

const formRules = reactive<FormRules>({
  code: [{ required: true, message: '分类编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }],
  active: [{ required: true, message: '启用状态不能为空', trigger: 'change' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'change' }]
})

const taxonomyCascaderProps = {
  value: 'id',
  label: 'name',
  children: 'children',
  checkStrictly: true,
  emitPath: false
}

const taxonomyTreeProps = {
  children: 'children'
}

const padNumber = (value: number, length = 2) => String(value).padStart(length, '0')

const buildRootTaxonomyCode = () => {
  const now = new Date()
  const codeBase = [
    'FT_ROOT_',
    now.getFullYear(),
    padNumber(now.getMonth() + 1),
    padNumber(now.getDate()),
    padNumber(now.getHours()),
    padNumber(now.getMinutes()),
    padNumber(now.getSeconds()),
    padNumber(now.getMilliseconds(), 3)
  ].join('')
  const existingCodes = new Set(rows.value.map((row) => String(row.code || '').toUpperCase()))
  if (!existingCodes.has(codeBase)) {
    return codeBase
  }
  let suffix = 2
  while (existingCodes.has(`${codeBase}_${suffix}`)) {
    suffix += 1
  }
  return `${codeBase}_${suffix}`
}

const isRootTaxonomy = (row: DccFileTypeTaxonomyVO) => !row.parentId || row.parentId <= 0

const resolveNextRootTaxonomySort = () => {
  const rootSorts = rows.value
    .filter(isRootTaxonomy)
    .map((row) => Number(row.sort))
    .filter((sort) => Number.isFinite(sort))
  return (rootSorts.length > 0 ? Math.max(...rootSorts) : 0) + 10
}

const applyRootCreateDefaults = () => {
  formData.parentId = undefined
  formData.code = buildRootTaxonomyCode()
  formData.sort = resolveNextRootTaxonomySort()
}

const ensureRootCreateDefaults = () => {
  formData.parentId = undefined
  if (!formData.code.trim()) {
    formData.code = buildRootTaxonomyCode()
  }
  if (!Number.isFinite(formData.sort)) {
    formData.sort = resolveNextRootTaxonomySort()
  }
}

const taxonomyPathMap = computed(() => {
  const pathMap = new Map<number, string>()
  const sortedRows = [...rows.value].sort((left, right) => (left.levelNo || 0) - (right.levelNo || 0))
  sortedRows.forEach((row) => {
    if (!row.id) {
      return
    }
    const parentPath = row.parentId ? pathMap.get(row.parentId) : ''
    pathMap.set(row.id, parentPath ? `${parentPath}/${row.name}` : row.name)
  })
  return pathMap
})

const taxonomyRowMatchesQuery = (row: DccFileTypeTaxonomyVO) => {
  const keyword = query.keyword.trim().toLowerCase()
  const path = row.id ? taxonomyPathMap.value.get(row.id) || '' : ''
  const keywordMatched =
    !keyword ||
    String(row.code || '').toLowerCase().includes(keyword) ||
    String(row.name || '').toLowerCase().includes(keyword) ||
    path.toLowerCase().includes(keyword)
  return keywordMatched
}

const buildTaxonomyTreeRows = (sourceRows: DccFileTypeTaxonomyVO[]) =>
  handleTree(sourceRows.map((row) => ({ ...row, children: undefined }))) as DccFileTypeTaxonomyVO[]

const filterTaxonomyTreeRows = (treeRows: DccFileTypeTaxonomyVO[]): DccFileTypeTaxonomyVO[] =>
  treeRows.reduce<DccFileTypeTaxonomyVO[]>((result, row) => {
    const matchedChildren = filterTaxonomyTreeRows(row.children || [])
    const rowMatched = taxonomyRowMatchesQuery(row)
    if (rowMatched || matchedChildren.length > 0) {
      result.push({
        ...row,
        children: matchedChildren.length > 0 ? matchedChildren : undefined
      })
    }
    return result
  }, [])

const filteredTreeRows = computed(() => filterTaxonomyTreeRows(buildTaxonomyTreeRows(rows.value)))

const taxonomyTotal = computed(() => filteredTreeRows.value.length)

const paginatedTreeRows = computed(() => {
  const start = Math.max(query.pageNo - 1, 0) * query.pageSize
  return filteredTreeRows.value.slice(start, start + query.pageSize)
})

watch(taxonomyTotal, (total) => {
  if (total > 0 && (query.pageNo - 1) * query.pageSize >= total) {
    query.pageNo = 1
  }
})

const parentOptions = computed(() =>
  handleTree(
    rows.value
      .filter((row) => row.id !== formData.id)
      .filter((row) => (row.levelNo || 1) < 5)
      .map((row) => ({ ...row }))
  ) as DccFileTypeTaxonomyVO[]
)

const formTitle = computed(() => (formType.value === 'create' ? '新增文件分类' : '编辑文件分类'))

const resetForm = () => {
  formData.id = undefined
  formData.parentId = undefined
  formData.code = ''
  formData.name = ''
  formData.active = true
  formData.sort = 0
  formData.remark = ''
  rootCreateMode.value = false
  formRef.value?.resetFields()
}

const loadData = async () => {
  loading.value = true
  try {
    rows.value = await getFileTypeTaxonomyList()
  } finally {
    loading.value = false
  }
}

const openForm = (
  type: 'create' | 'update',
  row?: DccFileTypeTaxonomyVO,
  parent?: DccFileTypeTaxonomyVO
) => {
  if (parent && (parent.levelNo || 1) >= 5) {
    message.warning('五级分类下不能继续新增下级')
    return
  }
  resetForm()
  formType.value = type
  rootCreateMode.value = type === 'create' && !parent
  if (type === 'update' && row) {
    formData.id = row.id
    formData.parentId = row.parentId && row.parentId > 0 ? row.parentId : undefined
    formData.code = row.code
    formData.name = row.name
    formData.active = row.active
    formData.sort = row.sort
    formData.remark = row.remark
  } else if (rootCreateMode.value) {
    applyRootCreateDefaults()
  } else if (parent?.id) {
    formData.parentId = parent.id
  }
  formVisible.value = true
}

const submitForm = async () => {
  if (rootCreateMode.value) {
    ensureRootCreateDefaults()
  }
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    const payload = {
      id: formData.id,
      parentId: formData.parentId || null,
      code: formData.code.trim(),
      name: formData.name.trim(),
      active: formData.active,
      sort: formData.sort,
      remark: formData.remark?.trim() || undefined
    }
    if (formType.value === 'create') {
      await createFileTypeTaxonomy(payload)
      message.success('新增成功')
    } else if (formData.id) {
      await updateFileTypeTaxonomy(formData.id, payload)
      message.success('更新成功')
    }
    formVisible.value = false
    await loadData()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (row: DccFileTypeTaxonomyVO) => {
  if (!row.id) {
    return
  }
  try {
    await message.delConfirm(`确认删除文件分类“${row.name}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await deleteFileTypeTaxonomy(row.id)
    message.success('删除成功')
    await loadData()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
:deep(.dcc-file-type-taxonomy-resizable-table .el-table__header-wrapper th.el-table__cell) {
  position: relative;
}

:deep(.dcc-file-type-taxonomy-resizable-table .el-table__header-wrapper th.el-table__cell::after) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  width: 8px;
  height: 100%;
  content: '';
  cursor: col-resize;
  border-right: 2px solid transparent;
}

:deep(.dcc-file-type-taxonomy-resizable-table .el-table__header-wrapper th.el-table__cell:hover::after) {
  border-right-color: #1677ff;
}
</style>
