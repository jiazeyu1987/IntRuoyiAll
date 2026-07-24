<template>
  <doc-alert title="审批接入（流程表单）" url="https://doc.iocoder.cn/bpm/use-bpm-form/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="bpm.form.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="formQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="formQuickFilter.state"
      :selected-filter-definition="formQuickFilter.selectedDefinition.value"
      :operator-options="formQuickFilter.operatorOptions.value"
      :columns="formColumns"
      :column-saving="formColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="formQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveFormColumnConfig"
      @column-reset="resetFormColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button
          v-hasPermi="['bpm:form:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" />
          新增
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
          data-user-table-key="bpm.form.main"
          @header-dragend="handleFormHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isFormColumnVisible('id')"
            align="center"
            label="编号"
            prop="id"
            :width="getFormColumnWidthString('id', 100)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isFormColumnVisible('name')"
            align="center"
            label="表单名"
            prop="name"
            :width="getFormColumnWidthString('name')"
            :min-width="getFormColumnMinWidthString('name', 180)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isFormColumnVisible('status')"
            align="center"
            label="状态"
            prop="status"
            :width="getFormColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isFormColumnVisible('remark')"
            align="center"
            label="备注"
            prop="remark"
            :width="getFormColumnWidthString('remark')"
            :min-width="getFormColumnMinWidthString('remark', 200)"
            v-bind="sortColumnAttrs('remark')"
          />
          <el-table-column
            v-if="isFormColumnVisible('createTime')"
            :formatter="dateFormatter"
            align="center"
            label="创建时间"
            prop="createTime"
            :width="getFormColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isFormColumnVisible('actions')"
            align="center"
            label="操作"
            prop="actions"
            fixed="right"
            :width="getFormColumnWidthString('actions', 220)"
          >
            <template #default="scope">
              <el-button
                v-hasPermi="['bpm:form:update']"
                link
                type="primary"
                @click="openForm('copy', scope.row.id)"
              >
                复制
              </el-button>
              <el-button
                v-hasPermi="['bpm:form:update']"
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
              >
                编辑
              </el-button>
              <el-button v-hasPermi="['bpm:form:query']" link @click="openDetail(scope.row.id)">
                详情
              </el-button>
              <el-button
                v-hasPermi="['bpm:form:delete']"
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog v-model="detailVisible" title="表单详情" width="800">
    <form-create :option="detailData.option" :rule="detailData.rule" />
  </Dialog>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as FormApi from '@/api/bpm/form'
import { setConfAndFields2 } from '@/utils/formCreate'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'BpmForm' })

const message = useMessage()
const { t } = useI18n()
const { currentRoute, push } = useRouter()

const formDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '编号', width: 100 },
  { key: 'name', label: '表单名', minWidth: 180 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'remark', label: '备注', minWidth: 200 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false }
]

const {
  columns: formColumns,
  saving: formColumnSaving,
  isColumnVisible: isFormColumnVisible,
  getColumnWidthString: getFormColumnWidthString,
  getColumnMinWidthString: getFormColumnMinWidthString,
  handleHeaderDragend: handleFormHeaderDragend,
  saveConfig: saveFormColumnConfig,
  resetConfig: resetFormColumnConfig
} = useUserTableColumns('bpm.form.main', formDefaultColumns)

const loading = ref(true)
const total = ref(0)
const list = ref<FormApi.FormVO[]>([])
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  status: undefined as number | undefined
})

const formQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'name', label: '表单名', type: 'text', queryParamKey: 'name', placeholder: '请输入表单名' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await FormApi.getFormPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const formQuickFilter = useTableQuickFilter('bpm.form.main', formQuickFilterDefinitions, queryParams, getList)

/** 搜索按钮操作 */
const handleQuery = async () => {
  await formQuickFilter.applyQuickFilter()
}

/** 重置按钮操作 */
const resetQuery = async () => {
  await formQuickFilter.resetQuickFilter()
}

/** 添加/修改操作 */
const openForm = (type: string, id?: number) => {
  const toRouter: { name: string; query: { type: string; id?: number } } = {
    name: 'BpmFormEditor',
    query: {
      type
    }
  }
  if (typeof id === 'number') {
    toRouter.query.id = id
  }
  push(toRouter)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await FormApi.deleteForm(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
}

/** 详情操作 */
const detailVisible = ref(false)
const detailData = ref({
  rule: [],
  option: {}
})
const openDetail = async (rowId: number) => {
  const data = await FormApi.getForm(rowId)
  setConfAndFields2(detailData, data.conf, data.fields)
  detailVisible.value = true
}

/** 表单保存返回后重新加载列表 */
watch(
  () => currentRoute.value,
  () => {
    getList()
  },
  {
    immediate: true
  }
)

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
