<template>
  <doc-alert title="工作流手册" url="https://doc.iocoder.cn/bpm/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="bpm.user-group.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="userGroupQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="userGroupQuickFilter.state"
      :selected-filter-definition="userGroupQuickFilter.selectedDefinition.value"
      :operator-options="userGroupQuickFilter.operatorOptions.value"
      :columns="userGroupColumns"
      :column-saving="userGroupColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="userGroupQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveUserGroupColumnConfig"
      @column-reset="resetUserGroupColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['bpm:user-group:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          data-user-table-column-explicit
          data-user-table-key="bpm.user-group.main"
          @header-dragend="handleUserGroupHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isUserGroupColumnVisible('id')"
            label="编号"
            align="center"
            prop="id"
            :width="getUserGroupColumnWidthString('id', 100)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isUserGroupColumnVisible('name')"
            label="组名"
            align="center"
            prop="name"
            :width="getUserGroupColumnWidthString('name')"
            :min-width="getUserGroupColumnMinWidthString('name', 160)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isUserGroupColumnVisible('description')"
            label="描述"
            align="center"
            prop="description"
            :width="getUserGroupColumnWidthString('description')"
            :min-width="getUserGroupColumnMinWidthString('description', 200)"
            v-bind="sortColumnAttrs('description')"
          />
          <el-table-column
            v-if="isUserGroupColumnVisible('members')"
            label="成员"
            align="center"
            prop="members"
            :width="getUserGroupColumnWidthString('members')"
            :min-width="getUserGroupColumnMinWidthString('members', 220)"
            v-bind="sortColumnAttrs('members')"
          >
            <template #default="scope">
              <span v-for="userId in scope.row.userIds" :key="userId" class="pr-5px">
                {{ userList.find((user) => user.id === userId)?.nickname }}
              </span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isUserGroupColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getUserGroupColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isUserGroupColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getUserGroupColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isUserGroupColumnVisible('actions')"
            label="操作"
            align="center"
            prop="actions"
            fixed="right"
            :width="getUserGroupColumnWidthString('actions', 140)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['bpm:user-group:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['bpm:user-group:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <UserGroupForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as UserGroupApi from '@/api/bpm/userGroup'
import * as UserApi from '@/api/system/user'
import UserGroupForm from './UserGroupForm.vue'
import { UserVO } from '@/api/system/user'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'BpmUserGroup' })

const message = useMessage()
const { t } = useI18n()

const userGroupDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '编号', width: 100 },
  { key: 'name', label: '组名', minWidth: 160 },
  { key: 'description', label: '描述', minWidth: 200 },
  { key: 'members', label: '成员', minWidth: 220 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 140, hideable: false, business: false }
]

const {
  columns: userGroupColumns,
  saving: userGroupColumnSaving,
  isColumnVisible: isUserGroupColumnVisible,
  getColumnWidthString: getUserGroupColumnWidthString,
  getColumnMinWidthString: getUserGroupColumnMinWidthString,
  handleHeaderDragend: handleUserGroupHeaderDragend,
  saveConfig: saveUserGroupColumnConfig,
  resetConfig: resetUserGroupColumnConfig
} = useUserTableColumns('bpm.user-group.main', userGroupDefaultColumns)

const loading = ref(true)
const total = ref(0)
const list = ref<UserGroupApi.UserGroupVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  status: undefined as number | undefined,
  createTime: [] as string[]
})
const userList = ref<UserVO[]>([])

const userGroupQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'name', label: '组名', type: 'text', queryParamKey: 'name', placeholder: '请输入组名' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  },
  { key: 'createTime', label: '创建时间', type: 'dateRange', queryParamKey: 'createTime' }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await UserGroupApi.getUserGroupPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const userGroupQuickFilter = useTableQuickFilter(
  'bpm.user-group.main',
  userGroupQuickFilterDefinitions,
  queryParams,
  getList
)

/** 搜索按钮操作 */
const handleQuery = async () => {
  await userGroupQuickFilter.applyQuickFilter()
}

/** 重置按钮操作 */
const resetQuery = async () => {
  await userGroupQuickFilter.resetQuickFilter()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await UserGroupApi.deleteUserGroup(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
  userList.value = await UserApi.getSimpleUserList()
})
</script>
