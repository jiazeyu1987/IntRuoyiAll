<template>
  <doc-alert title="用户体系" url="https://doc.iocoder.cn/user-center/" />
  <doc-alert title="三方登陆" url="https://doc.iocoder.cn/social-user/" />
  <doc-alert title="Excel 导入导出" url="https://doc.iocoder.cn/excel-import-and-export/" />

  <el-row :gutter="20">
    <!-- 左侧部门树 -->
    <el-col :span="4" :xs="24">
      <ContentWrap class="h-1/1">
        <DeptTreeSelect :key="deptTreeRenderKey" @node-click="handleDeptNodeClick" />
      </ContentWrap>
    </el-col>
    <el-col :span="20" :xs="24">
      <ContentWrap>
        <UnifiedListTemplate
          table-key="system.user.main"
          :query-model="queryParams"
          label-width="68px"
          :filter-definitions="userQuickFilterDefinitions"
          :quick-filter-state="userQuickFilter.state"
          :selected-filter-definition="userQuickFilter.selectedDefinition.value"
          :operator-options="userQuickFilter.operatorOptions.value"
          :columns="userColumns"
          :column-saving="userColumnSaving"
          :show-column-reset="false"
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="userQuickFilter.updateState"
          @quick-filter-query="handleQuery"
          @column-change="saveUserColumnConfig"
          @pagination="getList"
        >
          <template #actions>
            <el-dropdown
              class="system-user-advanced-actions"
              trigger="click"
              @command="handleAdvancedCommand"
              v-hasPermi="[
                'system:user:create',
                'system:user:import',
                'system:user:export',
                'system:user:delete',
                'system:dept:delete'
              ]"
            >
              <el-button type="primary" plain>
                <span>高级</span>
                <Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="create" v-hasPermi="['system:user:create']">
                    <Icon icon="ep:plus" />
                    <span class="ml-5px">新增</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="import" v-hasPermi="['system:user:import']">
                    <Icon icon="ep:upload" />
                    <span class="ml-5px">导入</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="export"
                    :disabled="exportLoading"
                    v-hasPermi="['system:user:export']"
                  >
                    <Icon icon="ep:download" />
                    <span class="ml-5px">导出</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="dingTalkImport" v-hasPermi="['system:user:import']">
                    <Icon icon="ep:office-building" />
                    <span class="ml-5px">钉钉导入</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="deleteBatch" v-hasPermi="['system:user:delete']">
                    <Icon icon="ep:delete" />
                    <span class="ml-5px">批量删除</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="deleteDept"
                    :disabled="selectedDeptId === undefined"
                    v-hasPermi="['system:dept:delete']"
                  >
                    <Icon icon="ep:delete-filled" />
                    <span class="ml-5px">删除组织</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              v-loading="loading"
              class="system-user-resizable-table"
              data-user-table-column-explicit
              data-user-table-key="system.user.main"
              :data="list"
              border
              :allow-drag-last-column="true"
              :row-class-name="getUserRowClassName"
              @selection-change="handleRowCheckboxChange"
              @header-dragend="handleUserHeaderDragend"
              @sort-change="handleTemplateSortChange"
            >
              <el-table-column
                type="selection"
                key="selection"
                :width="getUserColumnWidthString('selection', 55)"
              />
              <el-table-column
                v-if="isUserColumnVisible('id')"
                label="用户编号"
                align="center"
                key="id"
                prop="id"
                :width="getUserColumnWidthString('id', 100)"
                v-bind="sortColumnAttrs('id')"
              />
              <el-table-column
                v-if="isUserColumnVisible('username')"
                label="用户名称"
                align="center"
                key="username"
                prop="username"
                :width="getUserColumnWidthString('username')"
                :min-width="getUserColumnMinWidthString('username', 120)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('username')"
              >
                <template #default="{ row }">
                  <span
                    class="system-user-username"
                    :class="{ 'system-user-username--dept-leader': isDeptLeaderUser(row) }"
                  >
                    {{ row.username }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isUserColumnVisible('nickname')"
                label="用户昵称"
                align="center"
                key="nickname"
                prop="nickname"
                :width="getUserColumnWidthString('nickname')"
                :min-width="getUserColumnMinWidthString('nickname', 120)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('nickname')"
              />
              <el-table-column
                v-if="isUserColumnVisible('deptName')"
                label="部门"
                align="center"
                key="deptName"
                prop="deptName"
                :width="getUserColumnWidthString('deptName')"
                :min-width="getUserColumnMinWidthString('deptName', 120)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('deptName')"
              />
              <el-table-column
                v-if="isUserColumnVisible('roleNamesText')"
                label="角色"
                align="center"
                key="roleNamesText"
                prop="roleNamesText"
                :width="getUserColumnWidthString('roleNamesText')"
                :min-width="getUserColumnMinWidthString('roleNamesText', 180)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('roleNamesText')"
              >
                <template #default="{ row }">
                  {{ row.roleNamesText }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isUserColumnVisible('postNamesText')"
                label="岗位"
                align="center"
                key="postNamesText"
                prop="postNamesText"
                :width="getUserColumnWidthString('postNamesText')"
                :min-width="getUserColumnMinWidthString('postNamesText', 180)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('postNamesText')"
              >
                <template #default="{ row }">
                  {{ row.postNamesText }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isUserColumnVisible('mobile')"
                label="手机号码"
                align="center"
                key="mobile"
                prop="mobile"
                :width="getUserColumnWidthString('mobile', 120)"
                v-bind="sortColumnAttrs('mobile')"
              />
              <el-table-column
                v-if="isUserColumnVisible('status')"
                label="状态"
                key="status"
                prop="status"
                :width="getUserColumnWidthString('status', 100)"
                v-bind="sortColumnAttrs('status')"
              >
                <template #default="scope">
                  <el-switch
                    v-model="scope.row.status"
                    :active-value="0"
                    :inactive-value="1"
                    @change="handleStatusChange(scope.row)"
                    :disabled="!checkPermi(['system:user:update'])"
                  />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isUserColumnVisible('loginLocked')"
                label="锁定状态"
                key="loginLocked"
                prop="loginLocked"
                :width="getUserColumnWidthString('loginLocked', 100)"
                v-bind="sortColumnAttrs('loginLocked')"
              >
                <template #default="{ row }">
                  {{ row.loginLocked === 1 ? '已锁定' : '未锁定' }}
                </template>
              </el-table-column>
              <el-table-column
                v-if="isUserColumnVisible('createTime')"
                label="创建时间"
                align="center"
                key="createTime"
                prop="createTime"
                :formatter="dateFormatter"
                :width="getUserColumnWidthString('createTime', 180)"
                v-bind="sortColumnAttrs('createTime')"
              />
              <el-table-column
                label="操作"
                align="center"
                key="actions"
                prop="actions"
                fixed="right"
                :width="getUserColumnWidthString('actions', 190)"
              >
                <template #default="scope">
                  <div class="system-user-row-actions">
                    <div class="system-user-row-actions__row">
                      <el-button
                        type="primary"
                        link
                        @click="openForm('update', scope.row.id)"
                        v-hasPermi="['system:user:update']"
                      >
                        修改
                      </el-button>
                      <el-button
                        type="danger"
                        link
                        @click="handleDelete(scope.row.id)"
                        v-hasPermi="['system:user:delete']"
                      >
                        删除
                      </el-button>
                    </div>
                    <div class="system-user-row-actions__row">
                      <el-button
                        type="warning"
                        link
                        @click="handleResetPwd(scope.row)"
                        v-hasPermi="['system:user:update-password']"
                      >
                        重置密码
                      </el-button>
                      <el-button
                        type="primary"
                        link
                        @click="handleRole(scope.row)"
                        v-hasPermi="['system:permission:assign-user-role']"
                      >
                        分配角色
                      </el-button>
                      <el-button
                        v-if="scope.row.loginLocked === 1"
                        type="success"
                        link
                        @click="handleUnlock(scope.row)"
                        v-hasPermi="['system:user:update']"
                      >
                        解锁
                      </el-button>
                    </div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </ContentWrap>
    </el-col>
  </el-row>

  <!-- 添加或修改用户对话框 -->
  <UserForm ref="formRef" @success="getList" />
  <!-- 用户导入对话框 -->
  <UserImportForm ref="importFormRef" @success="getList" />
  <!-- 钉钉导入对话框 -->
  <UserDingTalkImportForm ref="dingTalkImportFormRef" @success="getList" />
  <!-- 分配角色 -->
  <UserAssignRoleForm ref="assignRoleFormRef" @success="getList" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { CommonStatusEnum } from '@/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import * as DeptApi from '@/api/system/dept'
import * as PermissionApi from '@/api/system/permission'
import * as PostApi from '@/api/system/post'
import * as RoleApi from '@/api/system/role'
import * as UserApi from '@/api/system/user'
import UserForm from './UserForm.vue'
import UserImportForm from './UserImportForm.vue'
import UserDingTalkImportForm from './UserDingTalkImportForm.vue'
import UserAssignRoleForm from './UserAssignRoleForm.vue'
import DeptTreeSelect from '@/views/system/dept/components/DeptTreeSelect.vue'
import { isSystemPasswordStrong, SYSTEM_PASSWORD_MESSAGE } from './systemPasswordPolicy'
import {
  buildDeptLeaderLookup,
  buildDisplayNameLookup,
  formatDisplayNames,
  isDeptLeader,
  resolveDisplayNames,
  sortUsersByDeptLeader
} from './utils'

defineOptions({ name: 'SystemUser' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const lookupPageSize = 200

interface UserTableRow extends UserApi.UserVO {
  postNamesText: string
  roleNamesText: string
}

const userQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'username',
    label: '用户名称',
    type: 'text',
    queryParamKey: 'username',
    placeholder: '请输入用户名称'
  },
  {
    key: 'mobile',
    label: '手机号码',
    type: 'text',
    queryParamKey: 'mobile',
    placeholder: '请输入手机号码'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  },
  { key: 'createTime', label: '创建时间', type: 'dateRange', queryParamKey: 'createTime' }
])

const userDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'selection', label: '选择', width: 55, hideable: false, business: false, sortable: false },
  { key: 'id', label: '用户编号', width: 100 },
  { key: 'username', label: '用户名称', minWidth: 120 },
  { key: 'nickname', label: '用户昵称', minWidth: 120 },
  { key: 'deptName', label: '部门', minWidth: 120 },
  { key: 'roleNamesText', label: '角色', minWidth: 180 },
  { key: 'postNamesText', label: '岗位', minWidth: 180 },
  { key: 'mobile', label: '手机号码', width: 120 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'loginLocked', label: '锁定状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false, sortable: false }
]

const {
  columns: userColumns,
  saving: userColumnSaving,
  isColumnVisible: isUserColumnVisible,
  getColumnWidthString: getUserColumnWidthString,
  getColumnMinWidthString: getUserColumnMinWidthString,
  handleHeaderDragend: handleUserHeaderDragend,
  saveConfig: saveUserColumnConfig
} = useUserTableColumns('system.user.main', userDefaultColumns)

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref<UserTableRow[]>([]) // 列表的数据
const deptLeaderByDeptId = ref(new Map<number, number>())
const deptNameById = ref(new Map<number, string>())
const postNameById = ref(new Map<number, string>())
const roleNameById = ref(new Map<number, string>())
const deptTreeRenderKey = ref(0)
const highlightedUserId = ref<number>()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  username: undefined as string | undefined,
  mobile: undefined as string | undefined,
  status: undefined as number | undefined,
  deptId: undefined as number | undefined,
  createTime: [] as string[]
})

const shouldPrioritizeDeptLeaders = computed(() => queryParams.deptId !== undefined)
const selectedDeptId = computed(() => queryParams.deptId)
const selectedDeptName = computed(() =>
  selectedDeptId.value === undefined ? '' : (deptNameById.value.get(selectedDeptId.value) ?? '')
)

const loadDeptMetadata = async () => {
  const deptList = (await DeptApi.getDeptList({})) as DeptApi.DeptVO[]
  deptLeaderByDeptId.value = buildDeptLeaderLookup(deptList)
  deptNameById.value = new Map(deptList.map((dept) => [dept.id, dept.name]))
}

const loadPagedLookupItems = async <T,>(
  fetchPage: (params: PageParam) => Promise<{ list: T[]; total: number }>
) => {
  const items: T[] = []
  let pageNo = 1
  let totalCount = 0
  do {
    const page = await fetchPage({ pageNo, pageSize: lookupPageSize })
    const pageItems = (page.list ?? []) as T[]
    items.push(...pageItems)
    totalCount = page.total ?? pageItems.length
    pageNo += 1
  } while (items.length < totalCount)
  return items
}

const loadUserDisplayMetadata = async () => {
  const [postList, roleList] = await Promise.all([
    loadPagedLookupItems<PostApi.PostVO>(PostApi.getPostPage),
    loadPagedLookupItems<RoleApi.RoleVO>(RoleApi.getRolePage)
  ])
  postNameById.value = buildDisplayNameLookup(postList)
  roleNameById.value = buildDisplayNameLookup(roleList)
}

const loadRoleNamesByUserId = async (users: UserApi.UserVO[]) => {
  const roleEntries = await Promise.all(
    users.map(async (user) => {
      const roleIds = (await PermissionApi.getUserRoleList(user.id)) as Array<number | string>
      return [user.id, resolveDisplayNames(roleIds, roleNameById.value)] as const
    })
  )
  return new Map<number, string[]>(roleEntries)
}

const buildUserTableRows = async (users: UserApi.UserVO[]): Promise<UserTableRow[]> => {
  const roleNamesByUserId = await loadRoleNamesByUserId(users)
  return users.map((user) => ({
    ...user,
    postNamesText: formatDisplayNames(resolveDisplayNames(user.postIds, postNameById.value)),
    roleNamesText: formatDisplayNames(roleNamesByUserId.get(user.id) ?? [])
  }))
}

const isDeptLeaderUser = (user: UserTableRow) => isDeptLeader(user, deptLeaderByDeptId.value)

const syncUserQueryFromRoute = async () => {
  highlightedUserId.value = undefined
  const userId = Number(route.query.userId)
  if (!Number.isFinite(userId)) {
    return false
  }
  const user = (await UserApi.getUser(Number(route.query.userId))) as UserApi.UserVO
  highlightedUserId.value = user.id
  queryParams.username = user.username
  queryParams.deptId = user.deptId
  queryParams.pageNo = 1
  deptTreeRenderKey.value += 1
  return true
}

const getUserRowClassName = ({ row }: { row: UserTableRow }) =>
  row.id === highlightedUserId.value ? 'system-user-table__row--target' : ''

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await UserApi.getUserPage(queryParams)
    const pageList = data.list as UserApi.UserVO[]
    const orderedList = shouldPrioritizeDeptLeaders.value
      ? sortUsersByDeptLeader(pageList, deptLeaderByDeptId.value, queryParams.deptId)
      : pageList
    list.value = await buildUserTableRows(orderedList)
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const userQuickFilter = useTableQuickFilter(
  'system.user.main',
  userQuickFilterDefinitions,
  queryParams,
  getList
)

/** 搜索按钮操作 */
const handleQuery = async () => {
  await userQuickFilter.applyQuickFilter()
}

/** 处理部门被点击 */
const handleDeptNodeClick = async (deptId: number | undefined) => {
  queryParams.deptId = deptId
  await getList()
}

const refreshDeptTreeAndList = async () => {
  queryParams.deptId = undefined
  checkedIds.value = []
  deptTreeRenderKey.value += 1
  await loadDeptMetadata()
  await getList()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 用户导入 */
const importFormRef = ref()
const handleImport = () => {
  importFormRef.value.open()
}

/** 钉钉导入 */
const dingTalkImportFormRef = ref()
const handleDingTalkImport = () => {
  dingTalkImportFormRef.value.open()
}

/** 修改用户状态 */
const handleStatusChange = async (row: UserApi.UserVO) => {
  try {
    // 修改状态的二次确认
    const text = row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'
    await message.confirm('确认要"' + text + '""' + row.username + '"用户吗?')
    // 发起修改状态
    await UserApi.updateUserStatus(row.id, row.status)
    // 刷新列表
    await getList()
  } catch {
    // 取消后，进行恢复按钮
    row.status =
      row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  }
}

/** 导出按钮操作 */
const exportLoading = ref(false)
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await UserApi.exportUser(queryParams)
    download.excel(data, '用户数据.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await UserApi.deleteUser(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: UserApi.UserVO[]) => {
  checkedIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  if (checkedIds.value.length === 0) {
    message.warning('请选择需要删除的用户')
    return
  }
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起批量删除
    await UserApi.deleteUserList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 删除当前选中组织 */
const handleDeleteSelectedDept = async () => {
  if (selectedDeptId.value === undefined) {
    message.warning('请选择需要删除的公司或部门')
    return
  }
  try {
    const targetName = selectedDeptName.value ? `“${selectedDeptName.value}”` : '当前选中组织'
    await message.delConfirm(
      `确认删除${targetName}吗？删除后会同步删除其下所有空部门；如果任一层仍有员工，系统会直接阻止删除。`
    )
    await DeptApi.deleteDept(selectedDeptId.value)
    message.success(t('common.delSuccess'))
    await refreshDeptTreeAndList()
  } catch {}
}

const handleAdvancedCommand = async (command: string) => {
  switch (command) {
    case 'create':
      openForm('create')
      break
    case 'import':
      handleImport()
      break
    case 'export':
      await handleExport()
      break
    case 'dingTalkImport':
      handleDingTalkImport()
      break
    case 'deleteBatch':
      await handleDeleteBatch()
      break
    case 'deleteDept':
      await handleDeleteSelectedDept()
      break
    default:
      throw new Error(`Unsupported system user advanced command: ${command}`)
  }
}

/** 重置密码 */
const handleResetPwd = async (row: UserApi.UserVO) => {
  try {
    // 重置的二次确认
    const result = await message.prompt(
      '请输入"' + row.username + '"的新密码',
      t('common.reminder')
    )
    const password = result.value
    if (!isSystemPasswordStrong(password)) {
      message.warning(SYSTEM_PASSWORD_MESSAGE)
      return
    }
    // 发起重置
    await UserApi.resetUserPassword(row.id, password)
    message.success('修改成功，新密码是：' + password)
  } catch {}
}

/** 解锁用户 */
const handleUnlock = async (row: UserApi.UserVO) => {
  try {
    await message.confirm('确认要解锁"' + row.username + '"用户吗?')
    await UserApi.unlockUser(row.id)
    message.success('解锁成功')
    await getList()
  } catch {}
}

/** 分配角色 */
const assignRoleFormRef = ref()
const handleRole = (row: UserApi.UserVO) => {
  assignRoleFormRef.value.open(row)
}

/** 初始化 */
onMounted(async () => {
  await Promise.all([loadDeptMetadata(), loadUserDisplayMetadata()])
  await syncUserQueryFromRoute()
  await getList()
})

watch(
  () => route.query.userId,
  async () => {
    await syncUserQueryFromRoute()
    await getList()
  }
)
</script>

<style scoped>
.system-user-username--dept-leader {
  color: #2f9e44;
  font-weight: 600;
}

.system-user-row-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  line-height: 1;
  white-space: nowrap;
}

.system-user-row-actions__row {
  display: grid;
  grid-template-columns: repeat(3, max-content);
  align-items: center;
  justify-content: center;
  column-gap: 10px;
}

.system-user-row-actions :deep(.el-button) {
  margin-left: 0;
  min-height: 18px;
  height: 18px;
  padding: 0;
  font-size: 12px;
}

:deep(.system-user-table__row--target td) {
  background: #ecf5ff !important;
}
</style>
