<!-- MES 设备类型列表 -->
<template>
  <doc-alert title="【设备】设备类型、设备台账" url="https://doc.iocoder.cn/mes/dv/device/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.machineryType.main"
      :query-model="queryParams"
      label-width="80px"
      :filter-definitions="machineryTypeQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="machineryTypeQuickFilter.state"
      :selected-filter-definition="machineryTypeQuickFilter.selectedDefinition.value"
      :operator-options="machineryTypeQuickFilter.operatorOptions.value"
      :columns="machineryTypeColumns"
      :column-saving="machineryTypeColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="machineryTypeQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveMachineryTypeColumnConfig"
      @column-reset="resetMachineryTypeColumnConfig"
      @pagination="handlePagination"
    >
      <template #actions>
        <el-button @click="machineryTypeQuickFilter.resetQuickFilter">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-machinery-type:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button type="danger" plain @click="toggleExpandAll">
          <Icon icon="ep:sort" class="mr-5px" /> 展开/折叠
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="machinery-type-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.machineryType.main"
          :data="paginatedMachineryTypeRows"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          :default-expand-all="isExpandAll"
          v-if="refreshTable"
          @header-dragend="handleMachineryTypeHeaderDragend"
        >
          <el-table-column
            v-if="isMachineryTypeColumnVisible('code')"
            label="设备类型编码"
            align="center"
            prop="code"
            :width="getMachineryTypeColumnWidthString('code', 140)"
          />
          <el-table-column
            label="设备类型名称"
            align="left"
            prop="name"
            :min-width="getMachineryTypeColumnMinWidthString('name', 180)"
          />
          <el-table-column
            v-if="isMachineryTypeColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getMachineryTypeColumnWidthString('status', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isMachineryTypeColumnVisible('sort')"
            label="排序"
            align="center"
            prop="sort"
            :width="getMachineryTypeColumnWidthString('sort', 100)"
          />
          <el-table-column
            v-if="isMachineryTypeColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getMachineryTypeColumnWidthString('createTime', 180)"
          />
          <el-table-column
            label="操作"
            align="center"
            :width="getMachineryTypeColumnWidthString('operation', 220)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('create', undefined, scope.row.id)"
                v-hasPermi="['mes:dv-machinery-type:create']"
              >
                新增子类型
              </el-button>
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-machinery-type:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-machinery-type:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <MachineryTypeForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { handleTree } from '@/utils/tree'
import { DvMachineryTypeApi, DvMachineryTypeVO } from '@/api/mes/dv/machinery/type'
import MachineryTypeForm from './MachineryTypeForm.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvMachineryType' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const MACHINERY_TYPE_TABLE_KEY = 'mes.dv.machineryType.main'

const machineryTypeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '设备类型编码', width: 140 },
  { key: 'name', label: '设备类型名称', minWidth: 180, hideable: false },
  { key: 'status', label: '状态', width: 100 },
  { key: 'sort', label: '排序', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 220, hideable: false, business: false }
]

const {
  columns: machineryTypeColumns,
  saving: machineryTypeColumnSaving,
  isColumnVisible: isMachineryTypeColumnVisible,
  getColumnWidthString: getMachineryTypeColumnWidthString,
  getColumnMinWidthString: getMachineryTypeColumnMinWidthString,
  handleHeaderDragend: handleMachineryTypeHeaderDragend,
  saveConfig: saveMachineryTypeColumnConfig,
  resetConfig: resetMachineryTypeColumnConfig
} = useUserTableColumns(MACHINERY_TYPE_TABLE_KEY, machineryTypeDefaultColumns)

const loading = ref(true) // 列表的加载中
const machineryTypeTreeRows = ref<DvMachineryTypeVO[]>([]) // 树形列表数据
const total = computed(() => machineryTypeTreeRows.value.length)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined as string | undefined,
  status: undefined as number | string | undefined
})

const machineryTypeQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'name',
    label: '类型名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入类型名称'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  }
])

const paginatedMachineryTypeRows = computed(() => {
  const start = Math.max(queryParams.pageNo - 1, 0) * queryParams.pageSize
  return machineryTypeTreeRows.value.slice(start, start + queryParams.pageSize)
})

const buildMachineryTypeListParams = () => ({
  name: queryParams.name,
  status: queryParams.status
})

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvMachineryTypeApi.getMachineryTypeList(buildMachineryTypeListParams())
    machineryTypeTreeRows.value = handleTree(data)
  } finally {
    loading.value = false
  }
}

const machineryTypeQuickFilter = useTableQuickFilter(
  MACHINERY_TYPE_TABLE_KEY,
  machineryTypeQuickFilterDefinitions,
  queryParams,
  getList
)

/** 搜索按钮操作 */
const handleQuery = async () => {
  queryParams.pageNo = 1
  await machineryTypeQuickFilter.applyQuickFilter()
}

const handlePagination = () => undefined

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number, parentId?: number) => {
  formRef.value.open(type, id, parentId)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await DvMachineryTypeApi.deleteMachineryType(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 展开/折叠操作 */
const isExpandAll = ref(true) // 是否展开，默认全部展开
const refreshTable = ref(true) // 重新渲染表格状态
const toggleExpandAll = async () => {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  await nextTick()
  refreshTable.value = true
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>

<style scoped>
.machinery-type-table {
  width: 100%;
}
</style>
