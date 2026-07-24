<template>
  <doc-alert title="功能权限" url="https://doc.iocoder.cn/resource-permission" />
  <doc-alert title="数据权限" url="https://doc.iocoder.cn/data-permission" />

  <ContentWrap>
    <div class="mb-12px text-12px text-[var(--el-text-color-secondary)]">
      权限角色：控制菜单与数据权限。
    </div>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :model="queryParams"
      class="permission-role-toolbar"
      label-position="top"
      @submit.prevent
    >
      <div class="permission-role-toolbar__filters">
        <el-form-item label="权限角色名称" prop="name">
          <el-input
            v-model="queryParams.name"
            clearable
            placeholder="请输入权限角色名称"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="权限角色标识" prop="code">
          <el-input
            v-model="queryParams.code"
            clearable
            placeholder="请输入权限角色标识"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="请选择状态">
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间" prop="createTime">
          <el-date-picker
            v-model="queryParams.createTime"
            :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
            end-placeholder="结束日期"
            start-placeholder="开始日期"
            type="daterange"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </div>
      <div class="permission-role-toolbar__actions">
        <el-button class="permission-role-toolbar__action" @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" />
          搜索
        </el-button>
        <el-button class="permission-role-toolbar__action" @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['system:role:create']"
          class="permission-role-toolbar__action"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增权限角色
        </el-button>
        <el-button
          v-hasPermi="['system:role:export']"
          :loading="exportLoading"
          class="permission-role-toolbar__action"
          plain
          type="success"
          @click="handleExport"
        >
          <Icon class="mr-5px" icon="ep:download" />
          导出配置包
        </el-button>
        <el-button
          v-hasPermi="['system:role:create', 'system:role:update']"
          :loading="importLoading"
          class="permission-role-toolbar__action"
          plain
          type="warning"
          @click="openImport"
        >
          <Icon class="mr-5px" icon="ep:upload" />
          导入配置包
        </el-button>
        <el-button
          v-hasPermi="['system:role:delete']"
          :disabled="checkedIds.length === 0"
          class="permission-role-toolbar__action"
          plain
          type="danger"
          @click="handleDeleteBatch"
        >
          <Icon class="mr-5px" icon="ep:delete" />
          批量删除
        </el-button>
      </div>
    </el-form>
  </ContentWrap>

  <ContentWrap>
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
        <div class="permission-role-table__title">
          <span>{{ selectedCategory?.name || '请选择分类' }}</span>
          <span class="permission-role-table__count">共 {{ total }} 个权限角色</span>
        </div>
        <el-table
          v-loading="loading"
          :data="list"
          :row-class-name="getRoleRowClassName"
          @selection-change="handleRowCheckboxChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column align="center" label="权限角色编号" prop="id" width="120" />
          <el-table-column align="center" label="权限角色名称" min-width="160" prop="name" />
          <el-table-column align="center" label="所属分类" min-width="120" prop="categoryName">
            <template #default="scope">
              <el-tag type="primary">{{ scope.row.categoryName }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="权限角色类型" align="center" prop="type" width="120">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.SYSTEM_ROLE_TYPE" :value="scope.row.type" />
            </template>
          </el-table-column>
          <el-table-column
            align="center"
            label="分配人数"
            prop="assignedUserCount"
            width="100"
          >
            <template #default="scope">
              <span class="permission-role-table__number">
                {{ scope.row.assignedUserCount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column align="center" label="权限角色标识" min-width="180" prop="code" />
          <el-table-column align="center" label="显示顺序" prop="sort" width="100" />
          <el-table-column align="center" label="备注" min-width="180" prop="remark" show-overflow-tooltip />
          <el-table-column align="center" label="状态" prop="status" width="100">
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            :formatter="dateFormatter"
            align="center"
            label="创建时间"
            prop="createTime"
            width="180"
          />
          <el-table-column :width="300" align="center" fixed="right" label="操作">
            <template #default="scope">
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
                preIcon="ep:coin"
                title="数据权限"
                type="primary"
                @click="openDataPermissionForm(scope.row)"
              >
                数据权限
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
        <Pagination
          v-model:limit="queryParams.pageSize"
          v-model:page="queryParams.pageNo"
          :total="total"
          @pagination="getList"
        />
      </section>
    </div>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <RoleForm ref="formRef" @success="getList" />
  <RoleCategoryForm ref="categoryFormRef" @success="refreshCategories" />
  <!-- 表单弹窗：菜单权限 -->
  <RoleAssignMenuForm ref="assignMenuFormRef" @success="getList" />
  <!-- 表单弹窗：数据权限 -->
  <RoleDataPermissionForm ref="dataPermissionFormRef" @success="getList" />
  <input ref="importInputRef" accept=".json" class="hidden" type="file" @change="handleImportFileChange" />
</template>
<script lang="ts" setup>
import { isSearchFormInputEmpty } from '@/utils/search'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import * as RoleApi from '@/api/system/role'
import RoleForm from './RoleForm.vue'
import RoleCategoryForm from './RoleCategoryForm.vue'
import RoleAssignMenuForm from './RoleAssignMenuForm.vue'
import RoleDataPermissionForm from './RoleDataPermissionForm.vue'

defineOptions({ name: 'SystemRole' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<RoleApi.RoleVO[]>([]) // 列表的数据
const categoryList = ref<RoleApi.RoleCategoryVO[]>([])
const highlightedRoleId = ref<number>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: '',
  name: '',
  categoryId: undefined as number | undefined,
  status: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const importLoading = ref(false)
const importInputRef = ref<HTMLInputElement>()
const selectedCategory = computed(() =>
  categoryList.value.find((category) => category.id === queryParams.categoryId)
)

/** 查询角色列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await RoleApi.getRolePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = (skipEmptyReset = false) => {
  if (skipEmptyReset !== true && isSearchFormInputEmpty(queryFormRef, queryParams)) {
    resetQuery()
    return
  }
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  queryParams.categoryId = selectedCategory.value?.id
  handleQuery(true)
}

/** 添加/修改操作 */
const formRef = ref()
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

/** 数据权限操作 */
const dataPermissionFormRef = ref()
const openDataPermissionForm = async (row: RoleApi.RoleVO) => {
  dataPermissionFormRef.value.open(row)
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

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: RoleApi.RoleVO[]) => {
  checkedIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  await message.delConfirm()
  await RoleApi.deleteRoleList(checkedIds.value)
  checkedIds.value = []
  message.success(t('common.delSuccess'))
  await getList()
}

/** 导出按钮操作 */
const handleExport = async () => {
  await message.exportConfirm()
  exportLoading.value = true
  try {
    const data = await RoleApi.exportRoleConfigPackage()
    download.json(data, '权限角色配置包.json')
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
    await getList()
  } finally {
    importLoading.value = false
  }
}

/** 初始化 */
onMounted(async () => {
  await getCategoryList()
  const hasRouteRole = await syncRoleQueryFromRoute()
  if (!hasRouteRole && categoryList.value.length > 0) {
    queryParams.categoryId = categoryList.value[0].id
  }
  await getList()
})

watch(
  () => route.query.roleId,
  async () => {
    await syncRoleQueryFromRoute()
    await getList()
  }
)
</script>
<style lang="scss" scoped>
.permission-role-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-role-toolbar__filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px 16px;
  align-items: end;
}

.permission-role-toolbar__filters :deep(.el-form-item) {
  margin-bottom: 0;
}

.permission-role-toolbar__filters :deep(.el-form-item__label) {
  margin-bottom: 6px;
  color: #263247;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
}

.permission-role-toolbar__filters :deep(.el-input),
.permission-role-toolbar__filters :deep(.el-select),
.permission-role-toolbar__filters :deep(.el-date-editor) {
  width: 100%;
}

.permission-role-toolbar__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
}

.permission-role-toolbar__action {
  min-width: 108px;
}

.permission-role-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
}

.permission-role-category {
  display: flex;
  min-height: 520px;
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
  .permission-role-toolbar__filters {
    grid-template-columns: 1fr;
  }

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
