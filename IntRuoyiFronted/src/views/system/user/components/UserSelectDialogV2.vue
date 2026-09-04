<!--
  系统用户弹窗选择器（V2，支持单选/多选）

  对齐 MdVendorSelectDialog 架构模式 + userSelect 左侧部门树
  搜索字段 & 展示字段：用户名称、用户昵称、部门、手机号码

  Props:
    multiple — true 多选（checkbox），false 单选（radio）；默认 true
    deptId   — 部门 ID
  Events:
    selected(rows: UserVO[]) — 确认选择后触发，单选时数组长度为 1
  Expose:
    open(selectedIds?: number[]) — 打开弹窗，可传入已选 ID 用于预选高亮
-->
<template>
  <Dialog :title="title" v-model="dialogVisible" width="80%" align-center append-to-body>
    <el-row class="h-[calc(100vh-196px)]" :gutter="15">
      <!-- 左侧部门树 -->
      <el-col class="h-full" :span="5" :xs="24">
        <ContentWrap class="h-full" :body-style="{ height: '100%', '--el-card-padding': '0px' }">
          <DeptTreeSelect ref="deptTreeRef" @node-click="handleDeptNodeClick" />
        </ContentWrap>
      </el-col>
      <!-- 右侧：搜索表单 + 用户表格 -->
      <el-col class="h-full overflow-auto" :span="19" :xs="24">
        <ContentWrap
          class="h-full !mb-0 user-select-dialog__list-wrap"
          :body-style="{ height: '100%' }"
        >
          <UnifiedListTemplate
            class="user-select-dialog__list-template"
            table-key="system.user.selectDialog"
            :query-model="queryParams"
            label-width="72px"
            :filter-definitions="userSelectQuickFilterDefinitions"
            :show-quick-filter="false"
            :quick-filter-state="userSelectQuickFilterState"
            :operator-options="userSelectQuickFilterOperatorOptions"
            :columns="userSelectColumns"
            :column-saving="userSelectColumnSaving"
            :show-column-reset="true"
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @column-change="saveUserSelectColumnConfig"
            @column-reset="resetUserSelectColumnConfig"
            @pagination="getList"
          >
            <template #extra-filters>
              <el-form-item label="用户名称">
                <el-input
                  v-model="queryParams.username"
                  placeholder="请输入用户名称"
                  clearable
                  @keyup.enter="handleQuery"
                  class="!w-240px"
                />
              </el-form-item>
              <el-form-item label="用户昵称">
                <el-input
                  v-model="queryParams.nickname"
                  placeholder="请输入用户昵称"
                  clearable
                  @keyup.enter="handleQuery"
                  class="!w-240px"
                />
              </el-form-item>
              <el-form-item label="手机号码">
                <el-input
                  v-model="queryParams.mobile"
                  placeholder="请输入手机号码"
                  clearable
                  @keyup.enter="handleQuery"
                  class="!w-240px"
                />
              </el-form-item>
              <el-form-item label="状态">
                <el-select
                  v-model="queryParams.status"
                  placeholder="请选择状态"
                  clearable
                  class="!w-240px"
                >
                  <el-option
                    v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </template>

            <template #actions>
              <el-button @click="handleQuery">
                <Icon icon="ep:search" class="mr-5px" /> 搜索
              </el-button>
              <el-button @click="resetQuery">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
            </template>

            <template #table>
              <!-- 数据表格：单选 radio / 多选 checkbox 统一在一个 table 内 -->
              <el-table
                ref="tableRef"
                class="user-select-dialog__table"
                data-user-table-column-explicit
                data-user-table-key="system.user.selectDialog"
                v-loading="loading"
                :data="list"
                border
                :stripe="true"
                :show-overflow-tooltip="true"
                row-key="id"
                :highlight-current-row="!multiple"
                @header-dragend="handleUserSelectHeaderDragend"
                @selection-change="handleSelectionChange"
                @row-click="handleRowClick"
                @row-dblclick="handleRowDblClick"
              >
                <!-- 多选：checkbox（reserve-selection 保证跨页勾选不丢失） -->
                <el-table-column
                  v-if="multiple"
                  type="selection"
                  :selectable="selectable"
                  :reserve-selection="true"
                  width="50"
                  align="center"
                />
                <!-- 单选：radio -->
                <el-table-column v-else width="50" align="center">
                  <template #default="{ row }">
                    <el-radio
                      v-model="selectedRadioId"
                      :value="row.id"
                      class="radio-no-label"
                      :disabled="row.disabled"
                      @change="handleRadioChange(row)"
                    />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isUserSelectColumnVisible('id')"
                  label="用户编号"
                  align="center"
                  prop="id"
                  :width="getUserSelectColumnWidthString('id', 150)"
                />
                <el-table-column
                  v-if="isUserSelectColumnVisible('username')"
                  label="用户名称"
                  align="center"
                  prop="username"
                  :width="getUserSelectColumnWidthString('username', 150)"
                />
                <el-table-column
                  v-if="isUserSelectColumnVisible('nickname')"
                  label="用户昵称"
                  align="left"
                  prop="nickname"
                  :min-width="getUserSelectColumnMinWidthString('nickname', 150)"
                />
                <el-table-column
                  v-if="isUserSelectColumnVisible('deptName')"
                  label="部门"
                  align="center"
                  prop="deptName"
                  :width="getUserSelectColumnWidthString('deptName', 150)"
                />
                <el-table-column
                  v-if="isUserSelectColumnVisible('mobile')"
                  label="手机号码"
                  align="center"
                  prop="mobile"
                  :width="getUserSelectColumnWidthString('mobile', 130)"
                />
                <el-table-column
                  v-if="isUserSelectColumnVisible('status')"
                  label="状态"
                  align="center"
                  prop="status"
                  :width="getUserSelectColumnWidthString('status', 80)"
                >
                  <template #default="scope">
                    <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
                  </template>
                </el-table-column>
                <el-table-column
                  v-if="isUserSelectColumnVisible('createTime')"
                  label="创建时间"
                  align="center"
                  prop="createTime"
                  :formatter="dateFormatter"
                  :width="getUserSelectColumnWidthString('createTime', 180)"
                />
              </el-table>
            </template>
          </UnifiedListTemplate>
        </ContentWrap>
      </el-col>
    </el-row>
    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import * as UserApi from '@/api/system/user'
import DeptTreeSelect from '@/views/system/dept/components/DeptTreeSelect.vue'
import { dateFormatter } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import type {
  TableQuickFilterDefinition,
  TableQuickFilterOperator
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'UserSelectDialogV2' })

const props = withDefaults(
  defineProps<{
    title?: string
    multiple?: boolean // true 多选（checkbox），false 单选（radio）
    deptId?: number // 部门 ID
    userOptions?: UserApi.UserVO[] // 由业务页面预加载的用户选项
  }>(),
  {
    title: '人员选择',
    multiple: true
  }
)

const message = useMessage()
const emit = defineEmits<{
  selected: [rows: UserApi.UserVO[], activityId?: any]
}>()

type UserSelectQuickFilterState = {
  fieldKey?: string
  operator?: TableQuickFilterOperator
  value?: string | number | boolean | Array<string | number>
}

const USER_SELECT_TABLE_KEY = 'system.user.selectDialog'

const userSelectDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '用户编号', width: 150 },
  { key: 'username', label: '用户名称', width: 150 },
  { key: 'nickname', label: '用户昵称', minWidth: 150 },
  { key: 'deptName', label: '部门', width: 150 },
  { key: 'mobile', label: '手机号码', width: 130 },
  { key: 'status', label: '状态', width: 80 },
  { key: 'createTime', label: '创建时间', width: 180 }
]

const {
  columns: userSelectColumns,
  saving: userSelectColumnSaving,
  isColumnVisible: isUserSelectColumnVisible,
  getColumnWidthString: getUserSelectColumnWidthString,
  getColumnMinWidthString: getUserSelectColumnMinWidthString,
  handleHeaderDragend: handleUserSelectHeaderDragend,
  saveConfig: saveUserSelectColumnConfig,
  resetConfig: resetUserSelectColumnConfig
} = useUserTableColumns(USER_SELECT_TABLE_KEY, userSelectDefaultColumns)

const userSelectQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [])
const userSelectQuickFilterState = ref<UserSelectQuickFilterState>({})
const userSelectQuickFilterOperatorOptions: TableQuickFilterOperator[] = []

const dialogVisible = ref(false) // 弹窗是否展示
const loading = ref(false) // 列表加载中
const list = ref<UserApi.UserVO[]>([]) // 用户列表
const total = ref(0) // 总条数
const activityId = ref()

// ==================== 部门树 ====================
const deptTreeRef = ref() // 部门树 Ref

/** 部门节点点击 */
const handleDeptNodeClick = (deptId: number | undefined) => {
  queryParams.deptId = deptId
  handleQuery()
}

// ==================== 选中状态 ====================
const tableRef = ref() // 表格 Ref
const selectedRows = ref<UserApi.UserVO[]>([]) // 多选模式：选中行
const selectedRadioId = ref<number>() // 单选模式：选中 ID
const currentRadioRow = ref<UserApi.UserVO>() // 单选模式：选中行对象
const preSelectedIds = ref<number[]>([]) // 打开弹窗时传入的已选 ID
const preDisabledIds = ref<number[]>([]) // 打开弹窗时传入的禁选 ID

/** 多选：是否可以选中 */
const selectable = (row: UserApi.UserVO) => {
  return !preDisabledIds.value.includes(row.id)
}

/** 多选：checkbox 变化 */
const handleSelectionChange = (rows: UserApi.UserVO[]) => {
  if (props.multiple) {
    selectedRows.value = rows
  }
}

/** 单选：radio 变化 */
const handleRadioChange = (row: UserApi.UserVO) => {
  currentRadioRow.value = row
}

/** 单击行：单选模式下点击整行即选中（降低操作成本），多选不处理（避免和 dblclick 冲突） */
const handleRowClick = (row: UserApi.UserVO) => {
  if (row.disabled) {
    return
  }
  if (props.multiple) {
    return
  }
  selectedRadioId.value = row.id
  currentRadioRow.value = row
}

/** 双击行：多选模式切换勾选，单选模式直接确认 */
const handleRowDblClick = (row: UserApi.UserVO) => {
  if (row.disabled) {
    return
  }
  if (props.multiple) {
    tableRef.value?.toggleRowSelection(row)
    return
  }
  selectedRadioId.value = row.id
  currentRadioRow.value = row
  confirmSelect()
}

// ==================== 用户查询 ====================
const queryParams = reactive({
  pageNo: 1, // 页码
  pageSize: 10, // 每页条数
  username: undefined as string | undefined, // 用户名称
  nickname: undefined as string | undefined, // 用户昵称
  mobile: undefined as string | undefined, // 手机号码
  status: CommonStatusEnum.ENABLE as number | undefined, // 状态：默认只查启用
  deptId: undefined as number | undefined // 部门 ID（从左侧树选择）
})

/** 查询用户列表 */
const getList = async () => {
  loading.value = true
  try {
    if (props.userOptions !== undefined) {
      const username = (queryParams.username || '').trim().toLowerCase()
      const nickname = (queryParams.nickname || '').trim().toLowerCase()
      const mobile = (queryParams.mobile || '').trim().toLowerCase()
      const filtered = props.userOptions.filter((row) => {
        if (row.disabled === true) return false
        if (queryParams.deptId !== undefined && row.deptId !== queryParams.deptId) return false
        if (username && !row.username.toLowerCase().includes(username)) return false
        if (nickname && !row.nickname.toLowerCase().includes(nickname)) return false
        if (mobile && !(row.mobile || '').toLowerCase().includes(mobile)) return false
        return true
      })
      total.value = filtered.length
      const start = (queryParams.pageNo - 1) * queryParams.pageSize
      list.value = filtered.slice(start, start + queryParams.pageSize)
    } else {
      const data = await UserApi.getUserPage(queryParams)
      list.value = data.list
      total.value = data.total
    }
    list.value.forEach((row) => {
      row.disabled = preDisabledIds.value.includes(row.id)
    })
    await nextTick()
    applyPreSelection()
  } finally {
    loading.value = false
  }
}

/** 恢复预选状态（当前页可见范围内） */
const applyPreSelection = () => {
  if (preSelectedIds.value.length === 0) {
    return
  }
  if (props.multiple) {
    const table = tableRef.value
    if (!table) {
      return
    }
    list.value.forEach((row) => {
      if (preSelectedIds.value.includes(row.id)) {
        table.toggleRowSelection(row, true)
      }
    })
  } else {
    const match = list.value.find((row) => preSelectedIds.value.includes(row.id))
    if (match) {
      selectedRadioId.value = match.id
      currentRadioRow.value = match
    }
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置查询条件 */
const resetQuery = () => {
  queryParams.username = undefined
  queryParams.mobile = undefined
  queryParams.status = CommonStatusEnum.ENABLE
  queryParams.deptId = undefined
  // 清空部门树选中
  deptTreeRef.value?.reset()
  handleQuery()
}

/** 确认选择 */
const confirmSelect = () => {
  if (props.multiple) {
    if (selectedRows.value.length === 0) {
      message.warning('请至少选择一条数据')
      return
    }
    emit('selected', selectedRows.value, activityId.value)
  } else {
    if (!currentRadioRow.value) {
      message.warning('请选择一条数据')
      return
    }
    emit('selected', [currentRadioRow.value], activityId.value)
  }
  dialogVisible.value = false
}

// ==================== 打开弹窗 ====================

/** 打开弹窗，可传入已选 ID 用于预选高亮 */
const open = async (selectedIds?: number[], disabledIds?: number[], _activityId?: any) => {
  preDisabledIds.value = disabledIds ?? []
  activityId.value = _activityId
  dialogVisible.value = true
  // 重置查询条件 + 页码，避免二次打开继承上次过滤上下文
  queryParams.username = undefined
  queryParams.mobile = undefined
  queryParams.status = CommonStatusEnum.ENABLE
  queryParams.deptId = props.deptId
  queryParams.pageNo = 1
  // 清空上一次的选中状态
  selectedRows.value = []
  selectedRadioId.value = undefined
  currentRadioRow.value = undefined
  preSelectedIds.value = (selectedIds ?? []).filter((id) => !preDisabledIds.value.includes(id))
  // 清空部门树选中 + 多选模式清空跨页缓存的勾选
  await nextTick()
  deptTreeRef.value?.reset()
  tableRef.value?.clearSelection()
  await getList()
  if (queryParams.deptId) {
    deptTreeRef.value?.setCurrent(queryParams.deptId)
  }
}
defineExpose({ open })
</script>

<style lang="scss" scoped>
.user-select-dialog__list-wrap {
  :deep(.el-card__body) {
    min-height: 0;
  }
}

.user-select-dialog__list-template {
  min-height: 0;
  height: 100%;

  :deep(.unified-list-template__table-shell) {
    display: flex;
    min-height: 0;
    flex: 1;
  }

  :deep(.el-table) {
    flex: 1;
  }
}

.user-select-dialog__table {
  height: 100%;
}

/* 隐藏 radio 的 label 文字，只保留圆圈 */
.radio-no-label {
  :deep(.el-radio__label) {
    display: none;
  }
}
</style>
