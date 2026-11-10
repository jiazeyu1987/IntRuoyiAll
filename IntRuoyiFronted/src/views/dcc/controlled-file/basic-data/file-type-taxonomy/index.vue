<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--dcc-file-type-taxonomy">
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
      :filter-rows="filteredTreeRows"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #actions>
        <el-tag class="scheme-d-tag" effect="plain" size="small">五级分类</el-tag>
        <el-button
          class="scheme-d-btn"
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['dcc:controlled-file:category:manage']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增一级
        </el-button>
      </template>

      <template #table="{ resolveRows }">
        <el-table
          v-loading="loading"
          class="dcc-file-type-taxonomy-resizable-table"
          data-user-table-column-explicit
          data-user-table-key="dcc.fileTypeTaxonomy.main"
          :data="resolveRows(paginatedTreeRows)"
          border
          :allow-drag-last-column="true"
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          default-expand-all
          :expand-row-keys="taxonomyExpandedRowKeys"
          :tree-props="taxonomyTreeProps"
          @expand-change="handleTaxonomyExpandChange"
          @header-dragend="handleTaxonomyHeaderDragend"
        >
          <el-table-column label="序号" prop="rowNumber" type="index" :width="getTaxonomyColumnWidthString('rowNumber', 72)" align="center">
            <template #default="{ row }">{{ taxonomyRowNumberMap.get(row.id) }}</template>
          </el-table-column>
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
              <el-tag class="scheme-d-tag" size="small" effect="plain">{{ row.levelNo }}级</el-tag>
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
              <el-tag class="scheme-d-tag" :type="row.active ? 'success' : 'info'" size="small">
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
            class-name="taxonomy-actions-cell"
            fixed="right"
            :width="getTaxonomyColumnWidthString('actions', 290)"
            align="center"
          >
            <template #default="{ row }">
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--success"
                type="primary"
                :disabled="row.levelNo >= 5"
                @click="openForm('create', undefined, row)"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                新增下级
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openForm('update', row)"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                编辑
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--danger"
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['dcc:controlled-file:category:manage']"
              >
                删除
              </el-button>
              <el-button
                v-if="row.id && row.levelNo >= 3" link type="primary"
                v-hasPermi="['dcc:controlled-file:category:manage']"
                @click="mappingDialogRef?.open(row.id, row.name)">
                检查对应类别
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <FileTypeCategoryMappingDialog ref="mappingDialogRef" />
  <CategoryMatrixDialog ref="matrixDraftRef" @draft="matrixDraft = $event" />
  <Dialog v-model="formVisible" class="scheme-d-form-control taxonomy-form" :title="formTitle" width="min(640px, calc(100vw - 24px))">
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
      <el-form-item v-if="formType === 'update'" label="分类编码" prop="code">
        <el-input v-model="formData.code" readonly />
      </el-form-item>
      <el-form-item label="分类名称" prop="name">
        <el-input v-model="formData.name" maxlength="128" placeholder="请输入分类名称" />
      </el-form-item>
      <el-form-item v-if="formType === 'create' && newNodeLevel >= 3 && newNodeLevel < 5" label="节点用途">
        <el-checkbox v-model="pathNodeOnly">仅建立分类路径节点（不可用于文件申请）</el-checkbox>
      </el-form-item>
      <template v-if="requiresMatrix">
        <el-form-item label="存储目录" required>
          <el-tree-select v-model="storageDirectoryId" :data="storageDirectories" :props="storageDirectoryProps"
            node-key="id" check-strictly filterable default-expand-all placeholder="请选择启用的存储目录" />
          <p class="text-[var(--el-text-color-secondary)]">用于归档此文件类型的正式文件，项目文件夹仍在上传时选择。</p>
        </el-form-item>
        <el-form-item label="审批矩阵" required>
          <el-radio-group v-model="matrixMode">
            <el-radio-button label="existing">复制已有矩阵</el-radio-button>
            <el-radio-button label="new">新增矩阵</el-radio-button>
            <el-radio-button v-if="formData.name.trim() === '其他'" label="custom">本次自定义（其他）</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="matrixMode === 'existing'" label="已有矩阵" required>
          <el-select v-model="matrixSourceCategoryId" filterable placeholder="请选择已配置的生效矩阵">
            <el-option v-for="matrix in matrixOptions" :key="matrix.categoryId" :value="matrix.categoryId" :label="`${matrix.name} · V${matrix.routeVersionNo}`" />
          </el-select>
          <p class="text-[var(--el-text-color-secondary)]">复制规则并绑定新类型，后续由文控独立维护。</p>
        </el-form-item>
        <el-form-item v-else-if="matrixMode === 'new'" label="新矩阵" required>
          <el-button @click="matrixDraftRef?.openDraft(formData.name, matrixDraft)">配置审批矩阵</el-button>
          <el-tag v-if="matrixDraft" type="success">已配置 {{ matrixDraft.rules.length }} 条规则</el-tag>
        </el-form-item>
        <el-alert v-else title="后续每次申请填写本次自定义矩阵，不创建公共审批矩阵。" type="info" :closable="false" />
      </template>
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
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
        <el-button
          class="scheme-d-btn"
          type="primary"
          :loading="formLoading"
          @click="submitForm"
        >
          保存
        </el-button>
      </div>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import FileTypeCategoryMappingDialog from './FileTypeCategoryMappingDialog.vue'
import CategoryMatrixDialog from '../../categories/components/CategoryMatrixDialog.vue'
import { getCategoryReviewMatrixRows, type ControlledFileCategoryReviewMatrixRowVO, type ControlledFileCategoryApprovalMatrixSaveReqVO } from '@/api/dcc/controlledFile/fileCategories'
import type { FormRules } from 'element-plus'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { handleTree } from '@/utils/tree'
import { getDirectoryTree, type ControlledFileDirectoryVO } from '@/api/dcc/controlledFile/directories'
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
  { key: 'rowNumber', label: '序号', width: 72, hideable: false, business: false },
  { key: 'name', label: '分类名称', minWidth: 220, hideable: false },
  { key: 'code', label: '分类编码', minWidth: 150 },
  { key: 'levelNo', label: '层级', width: 82 },
  { key: 'taxonomyPath', label: '完整路径', minWidth: 300 },
  { key: 'active', label: '状态', width: 96 },
  { key: 'sort', label: '排序', width: 90 },
  { key: 'remark', label: '备注', minWidth: 180 },
  { key: 'actions', label: '操作', width: 290, hideable: false, business: false }
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
const matrixDraftRef = ref<InstanceType<typeof CategoryMatrixDialog>>()
const matrixMode = ref<'existing' | 'new' | 'custom'>('existing')
const matrixSourceCategoryId = ref<number>()
const matrixDraft = ref<ControlledFileCategoryApprovalMatrixSaveReqVO>()
const matrixOptions = ref<ControlledFileCategoryReviewMatrixRowVO[]>([])
const pathNodeOnly = ref(false)
const storageDirectoryId = ref<number>()
const storageDirectories = ref<ControlledFileDirectoryVO[]>([])
const storageDirectoryProps = { label: 'name', children: 'children', disabled: (row: ControlledFileDirectoryVO) => !row.active }
const newNodeLevel = computed(() => (rows.value.find(row => row.id === formData.parentId)?.levelNo || 0) + 1)
const requiresMatrix = computed(() => formType.value === 'create' && newNodeLevel.value >= 3 && !pathNodeOnly.value)
const mappingDialogRef = ref<InstanceType<typeof FileTypeCategoryMappingDialog>>()
const rows = ref<DccFileTypeTaxonomyVO[]>([])
const collapsedTaxonomyIds = reactive(new Set<number>())
const taxonomyExpandedRowKeys = computed(() =>
  rows.value.filter((row) => row.id != null && !collapsedTaxonomyIds.has(row.id)).map((row) => row.id!)
)
const handleTaxonomyExpandChange = (row: DccFileTypeTaxonomyVO, expanded: boolean) => {
  if (row.id == null) return
  if (expanded) collapsedTaxonomyIds.delete(row.id)
  else collapsedTaxonomyIds.add(row.id)
}
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
  formData.sort = resolveNextRootTaxonomySort()
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

const taxonomyRowNumberMap = computed(() => {
  const numbers = new Map<DccFileTypeTaxonomyVO['id'], number>()
  const visit = (treeRows: DccFileTypeTaxonomyVO[]) => {
    for (const row of treeRows) {
      numbers.set(row.id, numbers.size + 1)
      if (row.id != null && !collapsedTaxonomyIds.has(row.id)) visit(row.children || [])
    }
  }
  visit(paginatedTreeRows.value)
  return numbers
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
  storageDirectoryId.value = undefined
  pathNodeOnly.value = false
  matrixMode.value = 'existing'
  matrixSourceCategoryId.value = undefined
  matrixDraft.value = undefined
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

const openForm = async (
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
  if (requiresMatrix.value) {
    matrixOptions.value = []
    formLoading.value = true
    try {
      storageDirectories.value = handleTree(await getDirectoryTree())
      matrixOptions.value = (await getCategoryReviewMatrixRows({ active: true, configured: true }))
        .filter(row => row.routeVersionNo && row.effectiveTime && new Date(row.effectiveTime).getTime() <= Date.now())
    } finally { formLoading.value = false }
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  if (matrixMode.value === 'custom' && formData.name.trim() !== '其他') matrixMode.value = 'existing'
  if (requiresMatrix.value && !storageDirectoryId.value) {
    message.warning('请选择启用的存储目录')
    return
  }
  if (requiresMatrix.value && ((matrixMode.value === 'existing' && !matrixSourceCategoryId.value) ||
      (matrixMode.value === 'new' && !matrixDraft.value))) {
    message.warning('请先选择已有审批矩阵或配置新矩阵')
    return
  }
  formLoading.value = true
  try {
    const payload = {
      ...(formType.value === 'create' && pathNodeOnly.value ? { pathNodeOnly: true } : {}),
      ...(requiresMatrix.value ? {
        directoryBinding: { directoryId: storageDirectoryId.value!, active: true },
        approvalSourceMode: matrixMode.value === 'custom' ? 'APPLICATION_CUSTOM' as const : 'PUBLIC_MATRIX' as const,
        ...(matrixMode.value === 'existing' ? { matrixSourceCategoryId: matrixSourceCategoryId.value } : {}),
        ...(matrixMode.value === 'new' ? { approvalMatrix: matrixDraft.value } : {})
      } : {}),
      id: formData.id,
      parentId: formData.parentId || null,
      ...(formType.value === 'update' ? { code: formData.code } : {}),
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

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'taxonomyPath',
    label: '完整路径',
    type: 'text',
    getValue: (row) => [row.id ? taxonomyPathMap.value.get(row.id) || row.name : row.name].join(' ')
  },
  {
    key: 'active',
    label: '状态',
    type: 'text',
    getValue: (row) => [row.active ? '启用' : '停用'].join(' ')
  }
])
</script>

<style scoped>
.scheme-d-basic-data-page--dcc-file-type-taxonomy,
.taxonomy-form {
  --el-color-primary: #008f86;
  --el-color-primary-light-3: #4db1aa;
  --el-color-primary-light-5: #80c7c2;
  --el-color-primary-light-7: #b3ddd9;
  --el-color-primary-light-8: #cce9e7;
  --el-color-primary-light-9: #e6f5f2;
  --el-color-primary-dark-2: #00726b;
  --scheme-d-primary: #008f86;
  --scheme-d-border: #dbe3e7;
  min-width: 0;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.unified-list-template__query-form) {
  gap: 8px;
  padding: 0 0 8px;
  border: 0;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.unified-list-template__multi-filter) {
  flex: 1 1 520px;
  min-width: 0;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.unified-list-template__toolbar) {
  gap: 8px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.unified-list-template__table-shell) {
  min-width: 0;
  border-radius: 4px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-table th.el-table__cell) {
  height: 36px;
  padding: 4px 0;
  background: #f5f8f9;
  color: #52646e;
  font-size: 12px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-table td.el-table__cell) {
  height: 34px;
  padding: 4px 0;
  font-size: 13px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.taxonomy-actions-cell .cell) {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px 8px;
  white-space: normal;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.taxonomy-actions-cell .el-button) {
  margin: 0;
  padding: 0;
  min-height: 24px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-input__wrapper),
.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-select__wrapper),
.taxonomy-form :deep(.el-input__wrapper),
.taxonomy-form :deep(.el-select__wrapper) {
  min-height: 32px;
}

.taxonomy-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-pagination) {
  flex-wrap: wrap;
  gap: 8px;
  height: auto;
}

@media (max-width: 700px) {
  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-table-fixed-column--right) {
    position: static !important;
  }

  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.el-table-fixed-column--right::before) {
    box-shadow: none;
  }

  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.unified-list-template__toolbar-actions) {
    max-width: 100%;
  }

  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.table-multi-filter__condition-row),
  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.table-multi-filter-field) {
    flex-wrap: wrap;
    min-width: 0;
    max-width: 100%;
  }

  .scheme-d-basic-data-page--dcc-file-type-taxonomy :deep(.table-multi-filter-field__value) {
    flex: 1 1 180px;
    min-width: 0;
    max-width: 100%;
  }

  .taxonomy-form :deep(.el-input-number) {
    max-width: 100%;
  }
}

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
  border-right-color: var(--el-color-primary);
}
</style>
