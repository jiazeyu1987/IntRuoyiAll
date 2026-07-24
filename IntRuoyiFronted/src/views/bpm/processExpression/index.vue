<template>
  <doc-alert title="流程表达式" url="https://doc.iocoder.cn/bpm/expression/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="bpm.process-expression.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="processExpressionQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="processExpressionQuickFilter.state"
      :selected-filter-definition="processExpressionQuickFilter.selectedDefinition.value"
      :operator-options="processExpressionQuickFilter.operatorOptions.value"
      :columns="processExpressionColumns"
      :column-saving="processExpressionColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="processExpressionQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveProcessExpressionColumnConfig"
      @column-reset="resetProcessExpressionColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['bpm:process-expression:create']"
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
          data-user-table-key="bpm.process-expression.main"
          @header-dragend="handleProcessExpressionHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isProcessExpressionColumnVisible('id')"
            label="编号"
            align="center"
            prop="id"
            :width="getProcessExpressionColumnWidthString('id', 100)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isProcessExpressionColumnVisible('name')"
            label="名字"
            align="center"
            prop="name"
            :width="getProcessExpressionColumnWidthString('name')"
            :min-width="getProcessExpressionColumnMinWidthString('name', 180)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isProcessExpressionColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getProcessExpressionColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessExpressionColumnVisible('expression')"
            label="表达式"
            align="center"
            prop="expression"
            :width="getProcessExpressionColumnWidthString('expression')"
            :min-width="getProcessExpressionColumnMinWidthString('expression', 260)"
            v-bind="sortColumnAttrs('expression')"
          />
          <el-table-column
            v-if="isProcessExpressionColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getProcessExpressionColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isProcessExpressionColumnVisible('actions')"
            label="操作"
            align="center"
            prop="actions"
            fixed="right"
            :width="getProcessExpressionColumnWidthString('actions', 140)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['bpm:process-expression:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['bpm:process-expression:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <ProcessExpressionForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { ProcessExpressionApi, ProcessExpressionVO } from '@/api/bpm/processExpression'
import ProcessExpressionForm from './ProcessExpressionForm.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

/** BPM 流程表达式列表 */
defineOptions({ name: 'BpmProcessExpression' })

const message = useMessage()
const { t } = useI18n()

const processExpressionDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '编号', width: 100 },
  { key: 'name', label: '名字', minWidth: 180 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'expression', label: '表达式', minWidth: 260 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 140, hideable: false, business: false }
]

const {
  columns: processExpressionColumns,
  saving: processExpressionColumnSaving,
  isColumnVisible: isProcessExpressionColumnVisible,
  getColumnWidthString: getProcessExpressionColumnWidthString,
  getColumnMinWidthString: getProcessExpressionColumnMinWidthString,
  handleHeaderDragend: handleProcessExpressionHeaderDragend,
  saveConfig: saveProcessExpressionColumnConfig,
  resetConfig: resetProcessExpressionColumnConfig
} = useUserTableColumns('bpm.process-expression.main', processExpressionDefaultColumns)

const loading = ref(true)
const list = ref<ProcessExpressionVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  status: undefined as number | undefined,
  createTime: [] as string[]
})

const processExpressionQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'name', label: '名字', type: 'text', queryParamKey: 'name', placeholder: '请输入名字' },
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
    const data = await ProcessExpressionApi.getProcessExpressionPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const processExpressionQuickFilter = useTableQuickFilter(
  'bpm.process-expression.main',
  processExpressionQuickFilterDefinitions,
  queryParams,
  getList
)

/** 搜索按钮操作 */
const handleQuery = async () => {
  await processExpressionQuickFilter.applyQuickFilter()
}

/** 重置按钮操作 */
const resetQuery = async () => {
  await processExpressionQuickFilter.resetQuickFilter()
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
    await ProcessExpressionApi.deleteProcessExpression(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    throw error
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
