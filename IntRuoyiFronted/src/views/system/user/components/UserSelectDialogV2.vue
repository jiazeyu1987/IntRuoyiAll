<template>
  <Dialog v-model="visible" :title="title" width="min(1100px, 94vw)" append-to-body>
    <el-alert v-if="error || candidateError" :title="error || candidateError" type="error" :closable="false" show-icon class="mb-12px" />
    <el-tabs v-if="allowRelationships" v-model="activeTab">
      <el-tab-pane label="人员" name="users" />
      <el-tab-pane label="关系" name="relationships" />
    </el-tabs>
    <div v-if="allowRelationships && activeTab === 'relationships'" class="py-16px">
      <p class="mb-16px">以申请人/发起人为关系对象，申请发起时读取正式关系，不保存为固定人员。</p>
      <el-checkbox-group v-model="chosenRelationships">
        <el-checkbox v-for="relation in USER_RELATIONSHIPS" :key="relation.value" :value="relation.value">
          {{ relation.label }}
        </el-checkbox>
      </el-checkbox-group>
      <p class="mt-16px text-12px">直接主管来自用户管理的直属主管关系；部门负责人来自申请人所属部门的负责人配置。</p>
    </div>
    <div v-show="!allowRelationships || activeTab === 'users'">
    <UnifiedListTemplate
      table-key="system.user.selectDialog"
      label-width="68px"
      :query-model="queryParams"
      :show-quick-filter="false" :quick-filter-state="{}" :operator-options="[]"
      :filter-definitions="userSelectQuickFilterDefinitions"
      :columns="userSelectColumns"
      :column-saving="userSelectColumnSaving"
      :show-column-reset="true"
      @column-change="saveUserSelectColumnConfig"
      @column-reset="resetUserSelectColumnConfig"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    >
      <template #extra-filters>
        <el-form-item label="用户昵称"
          ><el-input v-model="queryParams.nickname" clearable @keyup.enter="handleQuery"
        /></el-form-item>
        <el-form-item label="用户名称"
          ><el-input v-model="queryParams.username" clearable @keyup.enter="handleQuery"
        /></el-form-item>
        <el-form-item v-if="showPrivateFields" label="手机号码"
          ><el-input v-model="queryParams.mobile" clearable @keyup.enter="handleQuery"
        /></el-form-item>
        <el-form-item v-if="departments.length" label="部门">
          <el-tree-select
            v-model="queryParams.deptIds"
            :data="departmentTree"
            :props="{ label: 'name', value: 'id' }"
            check-strictly
            multiple
            show-checkbox
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择部门（可多选）"
            clearable
            filterable
            style="width: 240px"
          />
          <el-checkbox v-if="!businessMode || departmentOptions !== undefined" v-model="queryParams.includeChildDepts" class="ml-8px"
            >包含子部门</el-checkbox
          >
        </el-form-item>
        <el-form-item v-if="queryParams.deptIds.length" label="人员范围">
          <el-radio-group v-model="queryParams.departmentMemberType">
            <el-radio-button value="ALL">全部</el-radio-button>
            <el-radio-button value="MEMBER" :disabled="!hasLeaderMetadata">组员</el-radio-button>
            <el-radio-button value="LEADER" :disabled="!hasLeaderMetadata">主管</el-radio-button>
          </el-radio-group>
          <span v-if="!hasLeaderMetadata" class="ml-8px text-12px">此业务未提供部门主管配置</span>
        </el-form-item>
        <el-form-item v-if="roles.length" label="权限角色"
          ><el-select v-model="queryParams.roleId" clearable filterable style="width: 160px"
            ><el-option
              v-for="item in roles"
              :key="item.id"
              :label="item.name"
              :value="item.id" /></el-select
        ></el-form-item>
        <el-form-item v-if="posts.length" label="岗位"
          ><el-select v-model="queryParams.postId" clearable filterable style="width: 160px"
            ><el-option
              v-for="item in posts"
              :key="item.id"
              :label="item.name"
              :value="item.id" /></el-select
        ></el-form-item>
        <el-form-item
          v-if="!businessMode || candidates.some((row) => row.status != null)"
          label="状态"
          ><el-select v-model="queryParams.status" clearable style="width: 100px"
            ><el-option label="启用" :value="0" /><el-option label="停用" :value="1" /></el-select
        ></el-form-item>
      </template>
      <template #actions><el-button @click="handleQuery">搜索</el-button
        ><el-button @click="resetQuery">重置</el-button
        ><el-button
          v-if="multiple && queryParams.deptIds.length"
          :disabled="loading || candidateLoading || !!candidateError || !!error"
          @click="selectAllFiltered">选择筛选结果全部人员</el-button
        ><el-button
          v-if="error"
          @click="
            open(
              selected.map((row) => row.id),
              openDisabled,
              activityId,
              selected
            )
          "
          >重新加载</el-button
        ></template>
      <template #table>
        <div class="mb-12px flex flex-wrap gap-8px">
          <span>已选 {{ selected.length }} 人</span>
          <el-tag v-for="row in selected" :key="String(row.id)" closable @close="remove(row.id)">{{
            row.nickname || row.username || row.id
          }}</el-tag>
          <el-button v-if="selected.length" link @click="selected = []">清空已选</el-button>
        </div>
        <el-table
          data-user-table-column-explicit
          data-user-table-key="system.user.selectDialog"
          @header-dragend="handleUserSelectHeaderDragend"
          v-loading="loading || candidateLoading"
          :data="list"
          height="390"
          border
          @row-click="toggle"
        >
          <el-table-column label="选择" width="65" align="center"
            ><template #default="{ row }">
              <el-checkbox
                v-if="multiple"
                :model-value="isSelected(row.id)"
                :disabled="!canSelect(row)"
                @click.stop
                @change="toggle(row)"
              />
              <el-radio
                v-else
                :model-value="selected[0]?.id"
                :value="row.id"
                :disabled="!canSelect(row)"
                @click.stop
                @change="toggle(row)"
                ><span></span></el-radio> </template></el-table-column>
          <el-table-column
            v-if="isUserSelectColumnVisible('id')"
            prop="id"
            label="用户编号"
            :width="getUserSelectColumnWidthString('id', 150)"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('mobile') && showPrivateFields"
            prop="mobile"
            label="手机号码"
            :width="getUserSelectColumnWidthString('mobile', 130)"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('createTime') && showPrivateFields"
            prop="createTime"
            label="创建时间"
            :formatter="dateFormatter"
            :width="getUserSelectColumnWidthString('createTime', 180)"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('nickname')"
            :width="getUserSelectColumnWidthString('nickname', 150)"
            prop="nickname"
            label="姓名"
            min-width="150"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('username')"
            :width="getUserSelectColumnWidthString('username', 150)"
            prop="username"
            label="账号"
            min-width="150"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('deptName')"
            :width="getUserSelectColumnWidthString('deptName', 150)"
            prop="deptName"
            label="部门"
            min-width="160"
          />
          <el-table-column
            v-if="isUserSelectColumnVisible('status')"
            label="状态"
            :width="getUserSelectColumnWidthString('status', 100)"
            ><template #default="{ row }">{{
              row.status === 1 ? '停用' : row.status === 0 ? '启用' : '—'
            }}</template></el-table-column
          >
          <template #empty>{{
            error || candidateError ? '候选人员加载失败' : '当前范围内无可选人员'
          }}</template>
        </el-table>
      </template></UnifiedListTemplate>
    </div>
    <template #footer
      ><el-button
        type="primary"
        :disabled="loading || candidateLoading || !!error || !!candidateError"
        @click="confirm"
        >确定</el-button
      ><el-button @click="visible = false">取消</el-button></template>
  </Dialog>
</template>
<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { hasPermission } from '@/directives/permission/hasPermi'
import * as UserApi from '@/api/system/user'
import * as DeptApi from '@/api/system/dept'
import * as RoleApi from '@/api/system/role'
import * as PostApi from '@/api/system/post'
import { handleTree } from '@/utils/tree'
import { dateFormatter } from '@/utils/formatTime'
import { useUserStoreWithOut } from '@/store/modules/user'
import {
  departmentIds,
  candidateDepartments,
  validateSelectionLimits,
  normalizeOptions,
  selectionError,
  type UserPickerProps,
  type UserOption,
  type UserId,
  type UserFilterOption
} from './userPicker'
import { USER_RELATIONSHIPS, type UserRelationship } from './userPicker'
defineOptions({ name: 'UserSelectDialogV2', inheritAttrs: false })
const props = withDefaults(defineProps<UserPickerProps & { title?: string }>(), {
  title: '人员选择',
  multiple: true,
  min: 0
})
const emit = defineEmits<{
  selected: [rows: any[], activityId?: any, relationships?: UserRelationship[]]
  'visible-change': [visible: boolean]
}>()
const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const candidates = ref<UserOption[]>([])
const list = ref<UserOption[]>([])
const selected = ref<UserOption[]>([])
const activeTab = ref('users')
const chosenRelationships = ref<UserRelationship[]>([])
const total = ref(0)
const activityId = ref<any>()
const openDisabled = ref<UserId[]>([])
const departments = ref<UserFilterOption[]>([])
const roles = ref<UserFilterOption[]>([])
const posts = ref<UserFilterOption[]>([])
const departmentTree = computed(() => handleTree(departments.value))
const businessMode = computed(
  () => props.userOptions !== undefined || props.loadUserOptions !== undefined
)
const hasLeaderMetadata = computed(() => !businessMode.value || (props.departmentOptions !== undefined &&
  departments.value.every(dept => Object.prototype.hasOwnProperty.call(dept, 'leaderUserId'))))
const canQueryUsers = computed(() => hasPermission(['system:user:query']))
const showPrivateFields = computed(() =>
  businessMode.value
    ? candidates.value.some((row) => row.mobile || row.createTime)
    : canQueryUsers.value
)
const USER_SELECT_TABLE_KEY = 'system.user.selectDialog'
const userSelectDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '用户编号', width: 150 },
  { key: 'username', label: '用户名称', width: 150 },
  { key: 'nickname', label: '用户昵称', width: 150 },
  { key: 'deptName', label: '部门', width: 150 },
  { key: 'mobile', label: '手机号码', width: 130 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 }
]
const {
  columns: userSelectColumns,
  saving: userSelectColumnSaving,
  isColumnVisible: isUserSelectColumnVisible,
  getColumnWidthString: getUserSelectColumnWidthString,
  handleHeaderDragend: handleUserSelectHeaderDragend,
  saveConfig: saveUserSelectColumnConfig,
  resetConfig: resetUserSelectColumnConfig
} = useUserTableColumns(USER_SELECT_TABLE_KEY, userSelectDefaultColumns)
const userSelectQuickFilterDefinitions = []
const disabled = computed(() => [
  ...(props.disabledIds || []),
  ...openDisabled.value,
  ...(props.excludeCurrentUser ? [useUserStoreWithOut().getUser.id] : [])
])
const limit = computed(() => (props.multiple ? (props.max ?? Infinity) : 1))
const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  username: '',
  mobile: '',
  nickname: '',
  deptIds: [] as number[],
  departmentMemberType: 'ALL' as 'ALL' | 'MEMBER' | 'LEADER',
  roleId: undefined as number | undefined,
  postId: undefined as number | undefined,
  status: 0 as number | undefined,
  includeChildDepts: true
})
let requestId = 0
const isSelected = (id: UserId) => selected.value.some((row) => String(row.id) === String(id))
const blocked = (row: UserOption) =>
  row.disabled || (row.status === 1 && !props.allowInactive) || disabled.value.some((id) => String(id) === String(row.id))
const canSelect = (row: UserOption) =>
  !blocked(row) && (!props.multiple || isSelected(row.id) || selected.value.length + chosenRelationships.value.length < limit.value)
const remove = (id: UserId) => {
  selected.value = selected.value.filter((row) => String(row.id) !== String(id))
}
const toggle = (row: UserOption) => {
  if (isSelected(row.id) && props.multiple) {
    remove(row.id)
    return
  }
  if (!canSelect(row)) return
  selected.value = props.multiple ? [...selected.value, row] : [row]
}
const filteredBusinessCandidates = () => {
      const scope = props.deptId == null ? null : departmentIds(props.deptId, true, departments.value)
      const depts = queryParams.deptIds.length === 0 ? null
        : [...new Set(queryParams.deptIds.flatMap(id => departmentIds(id, queryParams.includeChildDepts, departments.value)))]
      const memberType = depts ? queryParams.departmentMemberType : 'ALL'
      if (memberType !== 'ALL' && !hasLeaderMetadata.value) throw new Error('此业务未提供部门主管配置')
      const leaders = new Set(departments.value.filter(dept => depts?.includes(dept.id) && dept.leaderUserId != null)
        .map(dept => String(dept.leaderUserId)))
      const keyword = queryParams.nickname.trim().toLowerCase()
      return candidates.value.filter(row =>
        (!scope || (row.deptId != null && scope.includes(row.deptId))) &&
        (props.scopeRoleId == null || row.roleIds?.includes(props.scopeRoleId)) &&
        (props.scopePostId == null || row.postIds?.some(id => String(id) === String(props.scopePostId))) &&
        (memberType === 'LEADER' ? leaders.has(String(row.id))
          : ((!depts || (row.deptId != null && depts.includes(row.deptId))) && (memberType !== 'MEMBER' || !leaders.has(String(row.id))))) &&
        (!keyword || row.nickname?.toLowerCase().includes(keyword)) &&
        (!queryParams.username.trim() || row.username?.toLowerCase().includes(queryParams.username.trim().toLowerCase())) &&
        (!queryParams.mobile.trim() || row.mobile?.includes(queryParams.mobile.trim())) &&
        (queryParams.status == null || row.status == null || row.status === queryParams.status) &&
        (queryParams.roleId == null || row.roleIds?.includes(queryParams.roleId)) &&
        (queryParams.postId == null || row.postIds?.some(id => String(id) === String(queryParams.postId))))
}
const remoteQuery = () => {
  const { departmentMemberType, ...params } = queryParams
  return { ...params, deptIds: [...params.deptIds], deptLeaderMode: params.deptIds.length ? departmentMemberType : 'ALL',
    scopeDeptId: props.deptId, scopeRoleId: props.scopeRoleId, scopePostId: props.scopePostId }
}
const getList = async () => {
  const token = ++requestId
  loading.value = true
  error.value = ''
  try {
    if (props.candidateError) throw new Error(props.candidateError)
    if (businessMode.value) {
      const filtered = filteredBusinessCandidates()
      total.value = filtered.length
      list.value = filtered.slice(
        (queryParams.pageNo - 1) * queryParams.pageSize,
        queryParams.pageNo * queryParams.pageSize
      )
    } else {
      const loadPage = canQueryUsers.value ? UserApi.getUserPage : UserApi.getSelectionPage
      const data = await loadPage(remoteQuery())
      if (token !== requestId) return
      list.value = normalizeOptions(data.list)
      total.value = data.total
    }
  } catch (cause) {
    if (token !== requestId) return
    list.value = []
    total.value = 0
    error.value = cause instanceof Error ? cause.message : '候选人员加载失败，请重新加载'
  } finally {
    if (token === requestId) loading.value = false
  }
}
const selectAllFiltered = async () => {
  if (loading.value || props.candidateLoading || props.candidateError || error.value) return
  const token = ++requestId
  loading.value = true
  const union = new Map(selected.value.map(row => [String(row.id), row]))
  try {
    const collect = (rows: UserOption[]) => {
      for (const row of rows) if (!blocked(row)) union.set(String(row.id), row)
      if (union.size + chosenRelationships.value.length > limit.value) throw new Error(`最多选择 ${limit.value} 项人员或关系，请缩小筛选范围`)
    }
    if (businessMode.value) collect(filteredBusinessCandidates())
    else {
      const query = { ...remoteQuery(), pageNo: 1, pageSize: 200 }
      const loadPage = canQueryUsers.value ? UserApi.getUserPage : UserApi.getSelectionPage
      let fetched = 0
      while (true) {
        const page = await loadPage(query)
        if (token !== requestId) return
        if (!page.list.length && fetched < page.total) throw new Error('候选人员分页发生变化，请重新查询')
        collect(normalizeOptions(page.list))
        fetched += page.list.length
        if (fetched >= page.total) break
        query.pageNo++
      }
    }
    if (token === requestId) selected.value = [...union.values()]
  } catch (cause) {
    if (token === requestId) message.warning(cause instanceof Error ? cause.message : '批量选择人员失败')
  } finally {
    if (token === requestId) loading.value = false
  }
}
const handleQuery = () => {
  queryParams.pageNo = 1
  return getList()
}
const resetFilters = () =>
  Object.assign(queryParams, {
    pageNo: 1,
    username: '',
    mobile: '',
    nickname: '',
    deptIds: props.deptId == null ? [] : [props.deptId],
    departmentMemberType: 'ALL',
    roleId: undefined,
    postId: undefined,
    status: 0,
    includeChildDepts: !businessMode.value || props.departmentOptions !== undefined
  })
const resetQuery = () => {
  resetFilters()
  return getList()
}
const open = async (
  ids: UserId[] = [],
  disabledIds: UserId[] = [],
  context?: any,
  seed: UserOption[] = []
) => {
  const token = ++requestId
  visible.value = true
  loading.value = true
  error.value = ''
  list.value = []
  selected.value = []
  activeTab.value = 'users'
  chosenRelationships.value = props.allowRelationships ? [...(props.relationships || [])] : []
  activityId.value = context
  openDisabled.value = disabledIds
  resetFilters()
  try {
    const limitError = validateSelectionLimits(props.min, props.max, props.multiple)
    if (limitError) throw new Error(limitError)
    if (props.candidateError) throw new Error(props.candidateError)
    if (businessMode.value) {
      const loaded = normalizeOptions(
        props.loadUserOptions ? await props.loadUserOptions() : props.userOptions!,
        props.optionMapper
      )
      if (token !== requestId) return
      candidates.value = loaded
      departments.value = props.departmentOptions ?? candidateDepartments(loaded)
      roles.value = props.roleOptions || []
      posts.value = props.postOptions || []
    } else {
      const metadata = await Promise.all([
        DeptApi.getSimpleDeptList(),
        RoleApi.getSimpleRoleList(),
        PostApi.getSimplePostList()
      ])
      if (token !== requestId) return
      departments.value = metadata[0]
      roles.value = metadata[1]
      posts.value = metadata[2] as UserFilterOption[]
    }
    if (token !== requestId) return
    let known = [...candidates.value, ...seed, ...(props.selectedUsers || [])]
    if (ids.length && !businessMode.value) known = await UserApi.getSelectionList(ids, { scopeDeptId: props.deptId, scopeRoleId: props.scopeRoleId, scopePostId: props.scopePostId })
    if (token !== requestId) return
    selected.value = ids.map((id) => {
      const row = known.find((item) => String(item.id) === String(id))
      return row ? { ...row, id } : { id, nickname: `失效人员 ${id}`, disabled: true }
    })
    await getList()
  } catch (cause) {
    if (token !== requestId) return
    error.value = cause instanceof Error ? cause.message : '人员选择初始化失败'
    loading.value = false
  }
}
const confirm = () => {
  if (loading.value || props.candidateLoading || error.value || props.candidateError) return
  const configError = validateSelectionLimits(props.min, props.max, props.multiple)
  if (configError) { message.warning(configError); return }
  const relationCount = props.allowRelationships ? chosenRelationships.value.length : 0
  if (chosenRelationships.value.some(value => !USER_RELATIONSHIPS.some(option => option.value === value))) {
    message.warning('存在不支持的关系规则，请重新选择')
    return
  }
  if (selected.value.length + relationCount > limit.value) {
    message.warning(`最多选择 ${limit.value} 项人员或关系`)
    return
  }
  const invalid = selectionError(selected.value, Math.max(0, props.min - relationCount), limit.value - relationCount, disabled.value)
  if (invalid) {
    message.warning(invalid)
    return
  }
  if (
    props.deptId != null &&
    selected.value.some(
      (row) =>
        row.deptId == null ||
        !departmentIds(props.deptId!, true, departments.value).includes(row.deptId)
    )
  ) {
    message.warning('已选人员不在固定部门范围，请移除')
    return
  }
  if (
    businessMode.value &&
    selected.value.some((row) => {
      const latest = candidates.value.find((item) => String(item.id) === String(row.id))
      return !latest || blocked(latest) || (props.scopeRoleId != null && !latest.roleIds?.includes(props.scopeRoleId)) || (props.scopePostId != null && !latest.postIds?.some(id => String(id) === String(props.scopePostId)))
    })
  ) {
    message.warning('业务候选已变更，包含不可选人员，请移除')
    return
  }
  if (selected.value.some(blocked)) {
    message.warning('已选人员包含停用或不可选账号，请移除')
    return
  }
  emit('selected', selected.value, activityId.value, [...chosenRelationships.value])
  visible.value = false
}
watch(
  () => props.userOptions,
  () => {
    if (!visible.value || props.loadUserOptions) return
    try {
      candidates.value = normalizeOptions(props.userOptions || [], props.optionMapper)
      departments.value = props.departmentOptions ?? candidateDepartments(candidates.value)
      selected.value = selected.value.map(row => {
        const latest = candidates.value.find(item => String(item.id) === String(row.id))
        return latest ? { ...latest, id: row.id } : row
      })
      void getList()
    } catch (cause) {
      error.value = String(cause)
      list.value = []
    }
  },
  { deep: true }
)
watch(visible, (value) => {
  if (!value) ++requestId
  emit('visible-change', value)
})
defineExpose({ open })
</script>
