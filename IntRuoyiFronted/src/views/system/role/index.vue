<template>

  <ContentWrap class="standard-list-page">
    <UnifiedListTemplate
      ref="roleListRef"
      class="unified-list-template--contained"
      table-key="system.role.main"
      :query-model="queryParams"
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
      server-filter
      :server-filter-query="getList"

      :columns="listFilterColumns0"
      :show-column-settings="false"
      :column-filter-definitions="[...listColumnFilterDefinitions0, ...allColumnDisplayFilters0]"
    >
      <template #actions="{ filterLoading, filterError }">
        <el-button
          v-hasPermi="['system:role:create']"
          class="permission-role-toolbar__action"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
        </el-button>
        <el-button
          v-hasPermi="['system:role:export']"
          :loading="roleExcelExportLoading"
          :disabled="loading || filterLoading || !!filterError"
          class="permission-role-toolbar__action"
          plain
          type="success"
          title="导出当前分类和筛选条件内的全部匹配数据"
          @click="handleRoleExcelExport"
        >
          <Icon class="mr-5px" icon="ep:download" />
          导出列表
        </el-button>
        <el-button
          v-hasPermi="['system:role:delete']"
          :disabled="selectedRoleIds.length === 0"
          :loading="batchDeleteLoading"
          class="permission-role-toolbar__action"
          plain
          type="danger"
          @click="handleDeleteBatch"
        >
          <Icon class="mr-5px" icon="ep:delete" />
          批量删除
        </el-button>
        <EdhrPermissionTransfer
          :export-loading="exportLoading"
          :import-loading="importLoading"
          @export-role="openExport"
          @import-role="openImport"
          @success="refreshCategories"
        />
        <el-button
          v-hasPermi="['system:gxp-audit:query']"
          class="permission-role-toolbar__action"
          plain
          @click="permissionAuditRef?.open()"
        >
          权限审计
        </el-button>
        <UserTableColumnSettings
          :columns="roleColumns"
          :show-reset="false"
          :saving="roleColumnSaving"
          @change="saveRoleColumnConfig"
        />
      </template>
      <template #table="{ resolveRows }">
        <div class="permission-role-layout">
          <aside class="permission-role-category">
            <div class="permission-role-category__header">
              <div>
                <div class="permission-role-category__title">角色分类</div>
                <div class="permission-role-category__hint">像文件夹一样管理权限角色</div>
              </div>
              <el-button
                v-hasPermi="['system:role-category:create']"
                link
                type="primary"
                @click="openCategoryForm('create')"
              >
                新增
              </el-button>
            </div>
            <el-scrollbar class="permission-role-category__body">
              <button
                v-for="category in categoryList"
                :key="category.id"
                :class="[
                  'permission-role-category__item',
                  queryParams.categoryId === category.id ? 'is-active' : ''
                ]"
                type="button"
                @click="selectCategory(category)"
              >
                <span class="permission-role-category__name">
                  <Icon icon="ep:folder-opened" />
                  {{ category.name }}
                </span>
                <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="category.status" />
              </button>
            </el-scrollbar>
            <div v-if="selectedCategory" class="permission-role-category__actions">
              <el-button
                v-hasPermi="['system:role-category:update']"
                plain
                type="primary"
                @click="openCategoryForm('update', selectedCategory)"
              >
                编辑分类
              </el-button>
              <el-button
                v-hasPermi="['system:role-category:delete']"
                plain
                type="danger"
                @click="handleDeleteCategory(selectedCategory)"
              >
                删除分类
              </el-button>
            </div>
          </aside>

          <section class="permission-role-table">
            <el-table
              stripe
              border
              v-loading="loading"
              :data="resolveRows(list)"
              :row-class-name="getRoleRowClassName"
              ref="roleTableRef"
              data-user-table-column-explicit
              data-user-table-key="system.role.main"
              @header-dragend="handleRoleHeaderDragend"
              @selection-change="handleSelectionChange"
            >
              <el-table-column
                type="selection"
                column-key="selection"
                :width="getRoleColumnWidth('selection')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('id')"
                align="center"
                label="权限角色编号"
                prop="id"
                :width="getRoleColumnWidth('id')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('name')"
                align="center"
                label="权限角色名称"
                min-width="160"
                prop="name"
                :width="getRoleColumnWidth('name')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('categoryName')"
                align="center"
                label="所属分类"
                min-width="120"
                prop="categoryName"
                :width="getRoleColumnWidth('categoryName')"
              >
                <template #default="scope">
                  <el-tag type="primary">{{ scope.row.categoryName }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRoleColumnVisible('type')"
                label="权限角色类型"
                align="center"
                prop="type"
                :width="getRoleColumnWidth('type')"
              >
                <template #default="scope">
                  <dict-tag :type="DICT_TYPE.SYSTEM_ROLE_TYPE" :value="scope.row.type" />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRoleColumnVisible('assignedUserCount')"
                align="center"
                label="分配人数"
                prop="assignedUserCount"
                :width="getRoleColumnWidth('assignedUserCount')"
              >
                <template #default="scope">
                  <span class="permission-role-table__number">
                    {{ scope.row.assignedUserCount }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRoleColumnVisible('code')"
                align="center"
                label="权限角色标识"
                min-width="180"
                prop="code"
                :width="getRoleColumnWidth('code')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('sort')"
                align="center"
                label="显示顺序"
                prop="sort"
                :width="getRoleColumnWidth('sort')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('remark')"
                align="center"
                label="备注"
                min-width="180"
                prop="remark"
                show-overflow-tooltip
                :width="getRoleColumnWidth('remark')"
              />
              <el-table-column
                v-if="isRoleColumnVisible('status')"
                align="center"
                label="状态"
                prop="status"
                :width="getRoleColumnWidth('status')"
              >
                <template #default="scope">
                  <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isRoleColumnVisible('createTime')"
                :formatter="dateFormatter"
                align="center"
                label="创建时间"
                prop="createTime"
                :width="getRoleColumnWidth('createTime')"
              />
              <el-table-column
                :width="Math.max(getRoleColumnWidth('actions') ?? 0, 350)"
                column-key="actions"
                align="center"
                fixed="right"
                label="操作"
              >
                <template #default="scope">
                  <el-button
                    v-hasPermi="['system:user:query']"
                    link
                    type="primary"
                    @click="assignedUsersRef?.open(scope.row)"
                  >
                    查看
                  </el-button>
                  <el-button
                    v-hasPermi="['system:role:update']"
                    link
                    type="primary"
                    @click="openForm('update', scope.row.id)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-hasPermi="['system:permission:assign-role-menu']"
                    link
                    preIcon="ep:basketball"
                    title="菜单权限"
                    type="primary"
                    @click="openAssignMenuForm(scope.row)"
                  >
                    菜单权限
                  </el-button>
                  <el-button
                    v-hasPermi="['system:permission:assign-role-data-scope']"
                    link
                    type="primary"
                    @click="openDataPermissionForm(scope.row)"
                  >
                    数据权限
                  </el-button>
                  <el-button
                    v-hasPermi="['system:permission:assign-user-role']"
                    link
                    type="primary"
                    @click="assignUserFormRef?.open(scope.row)"
                  >
                    分配
                  </el-button>
                  <el-button
                    v-hasPermi="['system:role:delete']"
                    link
                    type="danger"
                    @click="handleDelete(scope.row.id)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>
      </template>
    </UnifiedListTemplate>
</ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <el-dialog v-model="exportDialogVisible" title="选择导出文件夹" width="480px">
    <el-select v-model="exportCategoryIds" multiple placeholder="请选择角色分类文件夹" class="w-full">
      <el-option v-for="category in categoryList" :key="category.id" :label="category.name" :value="category.id" />
    </el-select>
    <template #footer>
      <el-button @click="exportDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="exportLoading" :disabled="exportCategoryIds.length === 0" @click="handleExport">导出</el-button>
    </template>
  </el-dialog>
  <RoleForm ref="formRef" @success="getList" />
  <RoleAssignedUsers ref="assignedUsersRef" />
  <RoleAssignUserForm ref="assignUserFormRef" @success="getList" />
  <RoleCategoryForm ref="categoryFormRef" @success="refreshCategories" />
  <!-- 表单弹窗：菜单权限 -->
  <RoleAssignMenuForm ref="assignMenuFormRef" @success="getList" />
  <RoleDataPermissionForm ref="dataPermissionFormRef" @success="getList" />
  <PermissionAuditDialog ref="permissionAuditRef" />
  <input ref="importInputRef" accept=".json" class="hidden" type="file" @change="handleImportFileChange" />
</template>
<script lang="ts" setup>
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDictOptions as listColumnDictOptions } from '@/utils/dict'

import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as RoleApi from '@/api/system/role'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import RoleForm from './RoleForm.vue'
import RoleAssignedUsers from './RoleAssignedUsers.vue'
import RoleAssignUserForm from './RoleAssignUserForm.vue'
import RoleCategoryForm from './RoleCategoryForm.vue'
import RoleAssignMenuForm from './RoleAssignMenuForm.vue'
import RoleDataPermissionForm from './RoleDataPermissionForm.vue'
import EdhrPermissionTransfer from './EdhrPermissionTransfer.vue'
import PermissionAuditDialog from './PermissionAuditDialog.vue'

defineOptions({ name: 'SystemRole' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()

const roleDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'selection', label: '选择', width: 55, hideable: false, business: false },
  { key: 'id', label: '权限角色编号', width: 120 },
  { key: 'name', label: '权限角色名称', minWidth: 160 },
  { key: 'categoryName', label: '所属分类', minWidth: 120 },
  { key: 'type', label: '权限角色类型', width: 120, visible: false },
  { key: 'assignedUserCount', label: '分配人数', width: 100 },
  { key: 'code', label: '权限角色标识', minWidth: 180, visible: false },
  { key: 'sort', label: '显示顺序', width: 100, visible: false },
  { key: 'remark', label: '备注', minWidth: 180 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 350, hideable: false, business: false }
]
const {
  columns: roleColumns,
  saving: roleColumnSaving,
  isColumnVisible: isRoleColumnVisible,
  getColumnWidth: getRoleColumnWidth,
  handleHeaderDragend: handleRoleHeaderDragend,
  saveConfig: saveRoleColumnConfig
} = useUserTableColumns('system.role.main', roleDefaultColumns)

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<RoleApi.RoleVO[]>([]) // 列表的数据
const categoryList = ref<RoleApi.RoleCategoryVO[]>([])
const highlightedRoleId = ref<number>()
const permissionAuditRef = ref<InstanceType<typeof PermissionAuditDialog>>()
const roleTableRef = ref()
const roleListRef = ref<InstanceType<typeof UnifiedListTemplate>>()
const selectedRoleIds = ref<number[]>([])
const batchDeleteLoading = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: '',
  name: '',
  categoryId: undefined as number | undefined,
  status: undefined,
  createTime: []
})
const exportLoading = ref(false) // 导出的加载中
const roleExcelExportLoading = ref(false)
const importLoading = ref(false)
const importInputRef = ref<HTMLInputElement>()
const selectedCategory = computed(() =>
  categoryList.value.find((category) => category.id === queryParams.categoryId)
)

/** 查询角色列表 */
let listRequestId = 0
const getList = async () => {
  const requestId = ++listRequestId
  clearRoleSelection()
  list.value = []
  loading.value = true
  try {
    const data = await RoleApi.getRolePage({ ...queryParams })
    if (requestId !== listRequestId) return
    list.value = data.list
    total.value = data.total
  } catch (error) {
    if (requestId === listRequestId) total.value = 0
    throw error
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

/** 按当前分类查询角色 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 添加/修改操作 */
const formRef = ref()
const assignedUsersRef = ref<InstanceType<typeof RoleAssignedUsers>>()
const assignUserFormRef = ref<InstanceType<typeof RoleAssignUserForm>>()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const categoryFormRef = ref()
const openCategoryForm = (type: 'create' | 'update', row?: RoleApi.RoleCategoryVO) => {
  categoryFormRef.value.open(type, row)
}

const refreshCategories = async () => {
  await getCategoryList()
  if (!selectedCategory.value && categoryList.value.length > 0) {
    queryParams.categoryId = categoryList.value[0].id
  }
  await getList()
}

const getCategoryList = async () => {
  categoryList.value = await RoleApi.getRoleCategoryList()
}

const syncRoleQueryFromRoute = async () => {
  highlightedRoleId.value = undefined
  const roleId = Number(route.query.roleId)
  if (!Number.isFinite(roleId)) {
    return false
  }
  const role = (await RoleApi.getRole(Number(route.query.roleId))) as RoleApi.RoleVO
  highlightedRoleId.value = role.id
  queryParams.categoryId = role.categoryId
  queryParams.name = role.name
  queryParams.code = role.code
  queryParams.pageNo = 1
  return true
}

const getRoleRowClassName = ({ row }: { row: RoleApi.RoleVO }) =>
  row.id === highlightedRoleId.value ? 'permission-role-table__row--target' : ''

const selectCategory = (category: RoleApi.RoleCategoryVO) => {
  queryParams.categoryId = category.id
  handleQuery()
}

const handleDeleteCategory = async (category: RoleApi.RoleCategoryVO) => {
  await message.delConfirm()
  await RoleApi.deleteRoleCategory(category.id)
  message.success(t('common.delSuccess'))
  await refreshCategories()
}

/** 菜单权限操作 */
const assignMenuFormRef = ref()
const openAssignMenuForm = async (row: RoleApi.RoleVO) => {
  assignMenuFormRef.value.open(row)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  await message.delConfirm()
  await RoleApi.deleteRole(id)
  message.success(t('common.delSuccess'))
  await getList()
}

const clearRoleSelection = () => {
  selectedRoleIds.value = []
  roleTableRef.value?.clearSelection()
}

const handleSelectionChange = (rows: RoleApi.RoleVO[]) => {
  selectedRoleIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  const ids = [...selectedRoleIds.value]
  if (ids.length === 0) return
  try {
    await message.delConfirm(`是否删除选中的 ${ids.length} 个权限角色？`)
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    throw action
  }
  batchDeleteLoading.value = true
  try {
    await RoleApi.deleteRoleList(ids)
    message.success(t('common.delSuccess'))
    await getList()
  } finally {
    batchDeleteLoading.value = false
  }
}

const dataPermissionFormRef = ref<InstanceType<typeof RoleDataPermissionForm>>()
const openDataPermissionForm = (row: RoleApi.RoleVO) => {
  dataPermissionFormRef.value?.open(row)
}

const handleRoleExcelExport = async () => {
  roleListRef.value!.getExportScope()
  roleExcelExportLoading.value = true
  try {
    const data = await RoleApi.exportRole({
      ...queryParams,
      pageNo: 1
    })
    download.excel(data, '权限角色数据.xlsx')
  } finally {
    roleExcelExportLoading.value = false
  }
}

const exportDialogVisible = ref(false)
const exportCategoryIds = ref<number[]>([])
const openExport = () => {
  exportCategoryIds.value = queryParams.categoryId ? [queryParams.categoryId] : []
  exportDialogVisible.value = true
}

/** 导出按钮操作 */
const handleExport = async () => {
  if (exportCategoryIds.value.length === 0) return
  exportLoading.value = true
  try {
    const data = await RoleApi.exportRoleConfigPackage(exportCategoryIds.value)
    download.json(data, '权限角色配置包.json')
    exportDialogVisible.value = false
  } finally {
    exportLoading.value = false
  }
}

const openImport = () => {
  importInputRef.value?.click()
}

const handleImportFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }
  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await RoleApi.importRoleConfigPackage(formData)
    message.success('权限角色配置包导入成功')
    await refreshCategories()
  } finally {
    importLoading.value = false
  }
}

/** 初始化 */
onMounted(async () => {
  try {
    await getCategoryList()
    const hasRouteRole = await syncRoleQueryFromRoute()
    if (!hasRouteRole && categoryList.value.length > 0) {
      queryParams.categoryId = categoryList.value[0].id
    }
    await getList()
  } finally {
    loading.value = false
  }
})

watch(
  () => route.query.roleId,
  async () => {
    await syncRoleQueryFromRoute()
    await getList()
  }
)

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'type',
    label: '权限角色类型',
    type: 'select',
    options: listColumnDictOptions(DICT_TYPE.SYSTEM_ROLE_TYPE)
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: listColumnDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])

const listFilterColumns0 = [
  {
    key: 'id',
    label: '权限角色编号',
    visible: true
  },
  {
    key: 'name',
    label: '权限角色名称',
    visible: true
  },
  {
    key: 'categoryName',
    label: '所属分类',
    visible: true
  },
  {
    key: 'type',
    label: '权限角色类型',
    visible: true
  },
  {
    key: 'assignedUserCount',
    label: '分配人数',
    visible: true
  },
  {
    key: 'code',
    label: '权限角色标识',
    visible: true
  },
  {
    key: 'sort',
    label: '显示顺序',
    visible: true
  },
  {
    key: 'remark',
    label: '备注',
    visible: true
  },
  {
    key: 'status',
    label: '状态',
    visible: true
  },
  {
    key: 'createTime',
    label: '创建时间',
    visible: true
  }
]

const allColumnDisplayFilters0: ListColumnFilterDefinition[] = [
  {
    key: 'categoryName',
    label: '所属分类',
    type: 'text',
    getValue: (row) => [row.categoryName].join(' ')
  },
  {
    key: 'assignedUserCount',
    label: '分配人数',
    type: 'text',
    getValue: (row) => [row.assignedUserCount].join(' ')
  }
]
</script>
<style lang="scss" scoped>
.permission-role-toolbar__action {
  min-width: 108px;
}

.permission-role-layout {
  display: grid;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
}

.permission-role-category {
  display: flex;
  min-height: 0;
  flex-direction: column;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.permission-role-category__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 14px 10px;
  border-bottom: 1px solid #edf1f6;
}

.permission-role-category__title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.permission-role-category__hint {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.permission-role-category__body {
  flex: 1;
  min-height: 0;
  padding: 8px;
}

.permission-role-category__item {
  display: flex;
  width: 100%;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  padding: 8px 10px;
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #ffffff;
  color: #263247;
  text-align: left;
}

.permission-role-category__item:hover,
.permission-role-category__item.is-active {
  border-color: #dbeafe;
  background: #f5f9ff;
  color: #1677ff;
}

.permission-role-category__name {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-role-category__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #edf1f6;
}

.permission-role-table {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  min-width: 0;
}

.permission-role-table__title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 0 10px;
  color: #172033;
  font-size: 15px;
  font-weight: 600;
}

.permission-role-table__count {
  color: #6b7280;
  font-size: 12px;
  font-weight: 400;
}

.permission-role-table :deep(.permission-role-table__row--target td) {
  background: #ecf5ff !important;
}

.permission-role-table__number {
  color: #172033;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

@media (max-width: 960px) {
  .permission-role-toolbar__action {
    flex: 1 1 140px;
    min-width: 0;
  }

  .permission-role-layout {
    grid-template-columns: 1fr;
  }

  .permission-role-category {
    min-height: 280px;
  }
}
</style>
