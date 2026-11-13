<template>
  <doc-alert title="用户体系" url="https://doc.iocoder.cn/user-center/" />
  <doc-alert title="三方登陆" url="https://doc.iocoder.cn/social-user/" />
  <doc-alert title="Excel 导入导出" url="https://doc.iocoder.cn/excel-import-and-export/" />

  <el-row :gutter="20">
    <!-- 左侧部门树 -->
    <el-col :span="4" :xs="24">
      <ContentWrap class="h-1/1">
        <div class="flex flex-wrap gap-8px px-15px pt-15px" v-if="checkPermi(['system:dept:create', 'system:dept:update'])">
          <el-button v-if="checkPermi(['system:dept:create'])" type="primary" plain size="small" @click="openDeptForm('create')">
            新增部门
          </el-button>
          <el-button v-if="checkPermi(['system:dept:update']) && checkPermi(['system:dept:query'])" plain size="small" :disabled="selectedDeptId === undefined" @click="openDeptForm('update')">
            编辑部门
          </el-button>
        </div>
        <DeptTreeSelect :key="deptTreeRenderKey" :departments="departments" show-supervisor @node-click="handleDeptNodeClick" />
      </ContentWrap>
    </el-col>
    <el-col :span="20" :xs="24">
      <ContentWrap>
        <UnifiedListTemplate
          ref="userListTemplateRef"
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
          :rows="list"
          :sort-state="userSortState"
          @update:quick-filter-state="userQuickFilter.updateState"
          @quick-filter-query="handleQuery"
          @column-change="saveUserColumnConfig"
          @pagination="handleUserPagination"
          @sort-change="handleUserSortChange"
          server-filter
          :server-filter-query="getList"

          :column-filter-definitions="listColumnFilterDefinitions0"
        >
          <template #actions>
            <el-dropdown
              class="system-user-advanced-actions"
              trigger="click"
              @command="handleAdvancedCommand"
              v-if="checkPermi([
                'system:user:create',
                'system:user:import',
                'system:user:delete',
                'system:dept:delete',
                'system:user:export'
              ])"
            >
              <el-button type="primary" plain>
                <span>高级</span>
                <Icon icon="ep:arrow-down" class="ml-5px" />
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="exportUsers" v-if="checkPermi(['system:user:export'])" :disabled="exportLoading">
                    <Icon icon="ep:download" />
                    <span class="ml-5px">导出用户</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="exportGeneric" v-if="checkPermi(['system:user:export'])" :disabled="exportLoading">
                    <Icon icon="ep:download" />
                    <span class="ml-5px">导出不合规清单</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="create" v-if="checkPermi(['system:user:create'])">
                    <Icon icon="ep:plus" />
                    <span class="ml-5px">新增</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="import" v-if="checkPermi(['system:user:import'])">
                    <Icon icon="ep:upload" />
                    <span class="ml-5px">导入</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="teamRoster" v-if="checkPermi(['system:user:import']) && checkPermi(['system:user:create']) && checkPermi(['system:permission:assign-user-role'])">
                    <Icon icon="ep:upload" />
                    <span class="ml-5px">从车间名单导入生产组长</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="dingTalkImport" v-if="checkPermi(['system:user:import'])">
                    <Icon icon="ep:office-building" />
                    <span class="ml-5px">钉钉导入</span>
                  </el-dropdown-item>
                  <el-dropdown-item command="deleteBatch" v-if="checkPermi(['system:user:delete'])">
                    <Icon icon="ep:delete" />
                    <span class="ml-5px">批量删除</span>
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="deleteDept"
                    :disabled="selectedDeptId === undefined"
                    v-if="checkPermi(['system:dept:delete'])"
                  >
                    <Icon icon="ep:delete-filled" />
                    <span class="ml-5px">删除组织</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template #table="{ rows, resolveRows, sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              v-loading="loading"
              class="system-user-resizable-table"
              data-user-table-column-explicit
              data-user-table-key="system.user.main"
              :data="rows ?? resolveRows(list)"
              row-key="id"
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
                    :disabled="
                      !checkPermi(['system:user:update']) ||
                      Boolean(scope.row.lifecycleDeactivatedTime)
                    "
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
                v-if="isUserColumnVisible('lifecycleDocumentNo')"
                label="离职/转岗单号"
                align="center"
                key="lifecycleDocumentNo"
                prop="lifecycleDocumentNo"
                :width="getUserColumnWidthString('lifecycleDocumentNo')"
                :min-width="getUserColumnMinWidthString('lifecycleDocumentNo', 160)"
                :show-overflow-tooltip="true"
                v-bind="sortColumnAttrs('lifecycleDocumentNo')"
              />
              <el-table-column
                v-if="isUserColumnVisible('lifecycleEffectiveTime')"
                label="停用生效时间"
                align="center"
                key="lifecycleEffectiveTime"
                prop="lifecycleEffectiveTime"
                :formatter="dateFormatter"
                :width="getUserColumnWidthString('lifecycleEffectiveTime', 180)"
                v-bind="sortColumnAttrs('lifecycleEffectiveTime')"
              />
              <el-table-column
                v-if="isUserColumnVisible('lifecycleDeactivatedTime')"
                label="联动停用时间"
                align="center"
                key="lifecycleDeactivatedTime"
                prop="lifecycleDeactivatedTime"
                :formatter="dateFormatter"
                :width="getUserColumnWidthString('lifecycleDeactivatedTime', 180)"
                v-bind="sortColumnAttrs('lifecycleDeactivatedTime')"
              />
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
                :width="getUserColumnWidthString('actions', 230)"
              >
                <template #default="scope">
                  <div class="system-user-row-actions">
                    <div class="system-user-row-actions__row">
                      <el-button v-hasPermi="['system:user:update']" type="primary" link @click="openForm('update', scope.row.id)">修改</el-button>
                      <el-button v-hasPermi="['system:permission:assign-user-role']" type="primary" link @click="handleRole(scope.row)">分配</el-button>
                      <el-dropdown v-if="checkPermi(['system:user:delete', 'system:user:update-password', 'system:user:update', 'bpm:task:manager-query'])" trigger="click">
                        <el-button type="primary" link aria-label="更多操作">...</el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item v-if="checkPermi(['system:user:delete'])" :disabled="deleteLoading" @click="handleDelete(scope.row)">删除</el-dropdown-item>
                            <el-dropdown-item v-if="checkPermi(['system:user:update-password'])" @click="handleResetPwd(scope.row)">重置密码</el-dropdown-item>
                            <el-dropdown-item v-if="scope.row.loginLocked !== 1 && checkPermi(['system:user:update'])" @click="handleLock(scope.row)">锁定</el-dropdown-item>
                            <el-dropdown-item v-if="scope.row.loginLocked === 1 && checkPermi(['system:user:update'])" @click="handleUnlock(scope.row)">解锁</el-dropdown-item>
                            <el-dropdown-item v-if="checkPermi(['system:user:update'])" :disabled="scope.row.status !== CommonStatusEnum.ENABLE" @click="directSupervisorRef?.open(scope.row.id)">设置直属主管</el-dropdown-item>
                            <el-dropdown-item v-if="checkPermi(['system:user:update'])" :disabled="Boolean(scope.row.lifecycleDeactivatedTime)" @click="openLifecycleDeactivate(scope.row)">离职/转岗</el-dropdown-item>
                            <el-dropdown-item v-if="checkPermi(['bpm:task:manager-query'])" @click="openAccountHandover(scope.row)">交接待办</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
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

  <SupervisorHandoverDialog ref="handoverRef" />
  <DeptForm ref="deptFormRef" @success="refreshAfterDeptChange" />
  <!-- 添加或修改用户对话框 -->
  <UserForm ref="formRef" @success="getList" />
  <!-- 用户导入对话框 -->
  <UserImportForm ref="importFormRef" @success="refreshAfterLifecycleChange" />
  <TeamRosterImportDialog ref="teamRosterImportRef" @success="refreshAfterLifecycleChange" />
  <!-- 钉钉导入对话框 -->
  <UserDingTalkImportForm ref="dingTalkImportFormRef" @success="refreshAfterLifecycleChange" />
  <!-- 分配角色 -->
  <UserAssignRoleForm ref="assignRoleFormRef" @success="getList" />
  <!-- 离职/转岗联动停用 -->
  <UserLifecycleDeactivateForm ref="lifecycleDeactivateFormRef" @success="refreshAfterLifecycleChange" @handover="openLifecycleHandover" />
  <UserDirectSupervisorDialog ref="directSupervisorRef" />
</template>
<script lang="ts" setup>
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { getAccessToken, getTenantId, getVisitTenantId } from '@/utils/auth'
import { checkPermi } from '@/utils/permission'
import { isNavigationFailure } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { dateFormatter } from '@/utils/formatTime'
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
import * as UserApi from '@/api/system/user'
import UserForm from './UserForm.vue'
import SupervisorHandoverDialog from './SupervisorHandoverDialog.vue'
import UserImportForm from './UserImportForm.vue'
import TeamRosterImportDialog from '@/components/TeamRosterImportDialog.vue'
import UserDingTalkImportForm from './UserDingTalkImportForm.vue'
import UserAssignRoleForm from './UserAssignRoleForm.vue'
import UserLifecycleDeactivateForm from './UserLifecycleDeactivateForm.vue'
import UserDirectSupervisorDialog from './UserDirectSupervisorDialog.vue'
import DeptTreeSelect from '@/views/system/dept/components/DeptTreeSelect.vue'
import DeptForm from '@/views/system/dept/DeptForm.vue'
import { isSystemPasswordStrong, SYSTEM_PASSWORD_MESSAGE } from './systemPasswordPolicy'
import {
  buildDeptLeaderLookup,
  formatDisplayNames,
  isDeptLeader,
  type UserTableSortOrder
} from './utils'

defineOptions({ name: 'SystemUser' })

const handoverRef = ref<InstanceType<typeof SupervisorHandoverDialog>>()
const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const { push } = useRouter()
const userStore = useUserStore()

interface UserTableRow extends UserApi.UserListVO {
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
  { key: 'id', label: '用户编号', width: 100, sortable: 'custom' },
  { key: 'username', label: '用户名称', minWidth: 120, sortable: 'custom' },
  { key: 'nickname', label: '用户昵称', minWidth: 120, sortable: 'custom' },
  { key: 'deptName', label: '部门', minWidth: 120, sortable: 'custom' },
  { key: 'roleNamesText', label: '角色', minWidth: 180, sortable: 'custom' },
  { key: 'postNamesText', label: '岗位', minWidth: 180, sortable: 'custom' },
  { key: 'mobile', label: '手机号码', width: 120, sortable: 'custom' },
  { key: 'status', label: '状态', width: 100, sortable: 'custom' },
  { key: 'loginLocked', label: '锁定状态', width: 100, sortable: 'custom' },
  { key: 'lifecycleDocumentNo', label: '离职/转岗单号', minWidth: 160, visible: false, sortable: 'custom' },
  { key: 'lifecycleEffectiveTime', label: '停用生效时间', width: 180, visible: false, sortable: 'custom' },
  { key: 'lifecycleDeactivatedTime', label: '联动停用时间', width: 180, visible: false, sortable: 'custom' },
  { key: 'createTime', label: '创建时间', width: 180, sortable: 'custom' },
  { key: 'actions', label: '操作', width: 230, hideable: false, business: false, sortable: false }
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
const userSortState = ref<{ key?: keyof UserTableRow; order: UserTableSortOrder }>({ order: null })
const deptLeaderByDeptId = ref(new Map<number, number>())
const deptNameById = ref(new Map<number, string>())
const departments = ref<DeptApi.DeptVO[]>([])
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

const selectedDeptId = computed(() => queryParams.deptId)
const selectedDeptName = computed(() =>
  selectedDeptId.value === undefined ? '' : (deptNameById.value.get(selectedDeptId.value) ?? '')
)

const loadDeptMetadata = async () => {
  const deptList = await UserApi.getDepartmentTree()
  departments.value = deptList
  deptLeaderByDeptId.value = buildDeptLeaderLookup(deptList)
  deptNameById.value = new Map(deptList.map((dept) => [dept.id, dept.name]))
}

const deptFormRef = ref<InstanceType<typeof DeptForm>>()
const openDeptForm = (type: 'create' | 'update') => {
  if (type === 'update' && selectedDeptId.value === undefined) return
  return deptFormRef.value?.open(type, type === 'update' ? selectedDeptId.value : undefined,
    type === 'create' ? selectedDeptId.value : undefined)
}
const refreshAfterDeptChange = async () => {
  await loadDeptMetadata()
  if (selectedDeptId.value !== undefined && !deptNameById.value.has(selectedDeptId.value)) {
    queryParams.deptId = undefined
    deptTreeRenderKey.value += 1
  }
  queryParams.pageNo = 1
  await getList()
}

const buildUserTableRows = (users: UserApi.UserListVO[]): UserTableRow[] =>
  users.map((user) => ({
    ...user,
    postNamesText: formatDisplayNames(user.postNames),
    roleNamesText: formatDisplayNames(user.roleNames)
  }))

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
let listRequestId = 0
const getList = async () => {
  const requestId = ++listRequestId
  const params = { ...queryParams, sortKey: userSortState.value.order ? userSortState.value.key : undefined, sortOrder: userSortState.value.order }
  loading.value = true
  try {
    const data = await UserApi.getUserPage(params)
    if (requestId !== listRequestId) return
    const pageList = data.list as UserApi.UserListVO[]
    list.value = buildUserTableRows(pageList)
    total.value = data.total
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

const handleUserSortChange = async ({ key, order }: { key?: keyof UserTableRow; order: UserTableSortOrder }) => {
  userSortState.value = { key, order }
  queryParams.pageNo = 1
  await getList()
}

const handleUserPagination = () => {
  return getList()
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
  checkedUsers.value = []
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

const supervisorImpactText = async (ids: number[], operation: 'disable' | 'delete' = 'disable') => {
  const affected = await UserApi.getSupervisorImpact(ids)
  const supervisorImpact = affected.length
    ? '将解除以下部门的主管配置：' + affected.map((dept) => dept.name).join('、') +
      '。下级部门将重新按上级主管路由；没有上级主管的审批会被阻止。'
    : ''
  return supervisorImpact + (operation === 'delete'
    ? '将删除用户及其角色、岗位关联，已有待办不会自动转移；如需交接，请取消并先办理“交接待办”。'
    : '停用后将自动打开待办交接确认，已有待办经确认后才转移。')
}
const refreshAfterLifecycleChange = async () => {
  await loadDeptMetadata()
  await getList()
}

/** 修改用户状态 */
const handleStatusChange = async (row: UserApi.UserVO) => {
  let saved = false
  try {
    // 修改状态的二次确认
    const text = row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'
    const impact = row.status === CommonStatusEnum.DISABLE ? await supervisorImpactText([row.id]) : ''
    await message.confirm('确认要“' + text + '”用户“' + row.username + '”吗？' + impact)
    // 发起修改状态
    await UserApi.updateUserStatus(row.id, row.status)
    saved = true
    if (row.status === CommonStatusEnum.DISABLE) await openAccountHandover(row, '账号已停用，请确认未完成待办的接收人。')
    await loadDeptMetadata()
    await getList()
  } catch (error) {
    // 仅在修改未成功时恢复开关，刷新失败不能伪造账号状态。
    if (!saved) row.status =
      row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
    if (error !== 'cancel' && error !== 'close') throw error
  }
}

/** 删除按钮操作 */
type DeleteTarget = Pick<UserApi.UserVO, 'id' | 'username' | 'nickname'>
const deleteLoading = ref(false)
const describeDeleteTargets = (targets: DeleteTarget[]) => targets
  .map((user) => `账号“${user.username}”（${user.nickname}，编号 ${user.id}）`).join('；')
const handleDelete = async (row: DeleteTarget) => {
  if (deleteLoading.value) return
  const target = { id: row.id, username: row.username, nickname: row.nickname }
  deleteLoading.value = true
  try {
    const impact = await supervisorImpactText([target.id], 'delete')
    await message.delConfirm(`确认删除以下 1 位用户吗？${describeDeleteTargets([target])}。${impact}`)
    // 发起删除
    await UserApi.deleteUser(target.id)
    await loadDeptMetadata()
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  } finally {
    deleteLoading.value = false
  }
}

/** 批量删除按钮操作 */
const checkedUsers = ref<DeleteTarget[]>([])
const handleRowCheckboxChange = (rows: UserApi.UserVO[]) => {
  checkedUsers.value = rows.map(({ id, username, nickname }) => ({ id, username, nickname }))
}

const handleDeleteBatch = async () => {
  if (deleteLoading.value) return
  if (checkedUsers.value.length === 0) {
    message.warning('请选择需要删除的用户')
    return
  }
  const targets = checkedUsers.value.map((user) => ({ ...user }))
  const ids = targets.map((user) => user.id)
  deleteLoading.value = true
  try {
    // 删除的二次确认
    const impact = await supervisorImpactText(ids, 'delete')
    await message.delConfirm(`确认删除以下 ${targets.length} 位用户吗？${describeDeleteTargets(targets)}。${impact}`)
    // 发起批量删除
    await UserApi.deleteUserList(ids)
    await loadDeptMetadata()
    checkedUsers.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  } finally {
    deleteLoading.value = false
  }
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

const userListTemplateRef = ref()
const exportLoading = ref(false)
const handleUserExport = async (generic: boolean) => {
  if (exportLoading.value) return
  exportLoading.value = true
  try {
    const tenant = getTenantId()
    const visit = getVisitTenantId()
    const token = getAccessToken()
    const query = JSON.stringify(queryParams)
    const scope = generic ? undefined : userListTemplateRef.value.getExportScope()
    const scopeKey = scope ? JSON.stringify(scope) : ''
    const params = JSON.parse(query)
    params.sortKey = userSortState.value.order ? userSortState.value.key : undefined
    params.sortOrder = userSortState.value.order
    delete params.pageNo
    delete params.pageSize
    if (scope?.mode === 'local' && scope.active) {
      if (!scope.ids.length) throw new Error('当前筛选结果为空，无用户可导出')
      params.ids = scope.ids
    }
    const range = generic
      ? '当前租户全部通用账户不合规记录，不应用页面部门、查询或列筛选。'
      : `当前已应用筛选的全部匹配用户（所有分页），不是仅当前页。${scope?.active ? `列筛选匹配 ${total.value} 位用户。` : ''}筛选参数：${JSON.stringify(params)}`
    try {
      await message.confirm(`导出类型：${generic ? '通用账户不合规清单' : '用户数据'}\n租户 ID：${visit || tenant}\n范围：${range}`, '确认导出类型及范围')
    } catch (error) {
      if (error === 'cancel' || error === 'close') return
      throw error
    }
    const assertExportContext = () => {
      if (getTenantId() !== tenant || getVisitTenantId() !== visit || getAccessToken() !== token ||
        (!generic && (JSON.stringify(queryParams) !== query || JSON.stringify(userListTemplateRef.value.getExportScope()) !== scopeKey))) {
        throw new Error('租户、登录状态或筛选范围已变化，请重新确认导出')
      }
    }
    assertExportContext()
    const data = generic ? await UserApi.exportGenericAccountUsers() : await UserApi.exportUser(params)
    assertExportContext()
    download.excel(data, generic ? '通用账户不合规清单.xlsx' : '用户数据.xlsx')
  } catch (error) {
    message.error(error instanceof Error ? error.message : String(error))
    throw error
  } finally {
    exportLoading.value = false
  }
}

const teamRosterImportRef = ref<InstanceType<typeof TeamRosterImportDialog>>()
const handleAdvancedCommand = async (command: string) => {
  switch (command) {
    case 'exportUsers':
      await handleUserExport(false)
      break
    case 'exportGeneric':
      await handleUserExport(true)
      break
    case 'create':
      openForm('create')
      break
    case 'import':
      handleImport()
      break
    case 'teamRoster':
      teamRosterImportRef.value?.open('leader')
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
  let password: string
  try {
    const result = await message.prompt(
      '请输入"' + row.username + '"的新密码。重置成功后，该用户的现有登录会话将失效，须重新登录并修改密码；如为当前账号，本次操作后将退出登录。',
      t('common.reminder'),
      { inputType: 'password' }
    )
    password = result.value
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      message.error('无法打开密码重置，请重试')
    }
    return
  }
  if (!isSystemPasswordStrong(password)) {
    message.warning(SYSTEM_PASSWORD_MESSAGE)
    return
  }
  try {
    await UserApi.resetUserPassword(row.id, password)
  } catch {
    message.error('密码重置失败，请重试')
    return
  }
  message.success('密码已重置')
  if (row.id === userStore.getUser.id) {
    userStore.clearSession()
    try {
      const failure = await push({ path: '/login' })
      if (isNavigationFailure(failure)) {
        message.error('密码已重置，请重新登录')
      }
    } catch {
      message.error('密码已重置，请重新登录')
    }
    return
  }
  try {
    await getList()
  } catch {
    message.error('密码已重置，列表刷新失败，请重新刷新列表')
  }
}

/** 锁定用户 */
const handleLock = async (row: UserApi.UserVO) => {
  let reason: string
  try {
    const result = await message.prompt(
      '锁定后30分钟内无法通过密码登录，可由管理员提前解锁。请输入锁定"' + row.username + '"用户的原因',
      '锁定用户',
      {
        inputPlaceholder: '请输入锁定原因',
        inputValidator: (value) => !!value?.trim() || '锁定原因不能为空'
      }
    )
    reason = result.value.trim()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  await UserApi.lockUser(row.id, reason)
  message.success('锁定成功')
  await getList()
}

/** 解锁用户 */
const handleUnlock = async (row: UserApi.UserVO) => {
  let reason: string
  try {
    const result = await message.prompt(
      '请输入解锁"' + row.username + '"用户的原因',
      '解锁用户',
      {
        inputPlaceholder: '请输入解锁原因',
        inputValidator: (value) => !!value?.trim() || '解锁原因不能为空'
      }
    )
    reason = result.value.trim()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
  await UserApi.unlockUser(row.id, reason)
  message.success('解锁成功')
  await getList()
}

/** 分配角色 */
const assignRoleFormRef = ref()
const directSupervisorRef = ref<InstanceType<typeof UserDirectSupervisorDialog>>()
const handleRole = (row: UserApi.UserVO) => {
  assignRoleFormRef.value.open(row)
}

/** 登记离职/转岗联动停用 */
const lifecycleDeactivateFormRef = ref()
const openLifecycleDeactivate = (row: UserApi.UserVO) => {
  lifecycleDeactivateFormRef.value.open(row)
}

const openAccountHandover = (row: Pick<UserApi.UserVO, 'id' | 'username' | 'nickname'>, notice = '请选择未完成待办并确认接收人；未交接事项可从本账号的“交接待办”继续办理。') =>
  handoverRef.value?.open({ assigneeUserId: row.id, assigneeName: row.nickname || row.username,
    reason: '', notice })
const openLifecycleHandover = (row: Pick<UserApi.UserVO, 'id' | 'username' | 'nickname'>) =>
  openAccountHandover(row, '离职/转岗停用已登记，请办理现有待办交接；定时生效前新增的待办仍需另行交接。')

/** 初始化 */
onMounted(async () => {
  await loadDeptMetadata()
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

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'loginLocked',
    label: '锁定状态',
    type: 'text',
    getValue: (row) => [row.loginLocked === 1 ? '已锁定' : '未锁定'].join(' ')
  }
])
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
