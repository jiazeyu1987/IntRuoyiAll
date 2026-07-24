<template>
  <doc-alert title="【设备】点检保养项目、点检保养方案" url="https://doc.iocoder.cn/mes/dv/check-plan/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.checkPlan.main"
      :query-model="queryParams"
      label-width="85px"
      :filter-definitions="checkPlanQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="checkPlanQuickFilter.state"
      :selected-filter-definition="checkPlanQuickFilter.selectedDefinition.value"
      :operator-options="checkPlanQuickFilter.operatorOptions.value"
      :columns="checkPlanColumns"
      :column-saving="checkPlanColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="checkPlanQuickFilter.updateState"
      @quick-filter-query="handleQuickFilterQuery"
      @column-change="saveCheckPlanColumnConfig"
      @column-reset="resetCheckPlanColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-check-plan:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:dv-check-plan:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="check-plan-standard-list-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.checkPlan.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleCheckPlanHeaderDragend"
        >
          <el-table-column
            v-if="isCheckPlanColumnVisible('code')"
            label="方案编码"
            align="center"
            prop="code"
            :min-width="getCheckPlanColumnMinWidthString('code', 120)"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckPlanColumnVisible('name')"
            label="方案名称"
            align="center"
            prop="name"
            :min-width="getCheckPlanColumnMinWidthString('name', 150)"
          />
          <el-table-column
            v-if="isCheckPlanColumnVisible('type')"
            label="方案类型"
            align="center"
            prop="type"
            :min-width="getCheckPlanColumnMinWidthString('type', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_SUBJECT_TYPE" :value="scope.row.type" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckPlanColumnVisible('cycleCount')"
            label="周期数量"
            align="center"
            prop="cycleCount"
            :min-width="getCheckPlanColumnMinWidthString('cycleCount', 80)"
          />
          <el-table-column
            v-if="isCheckPlanColumnVisible('cycleType')"
            label="周期类型"
            align="center"
            prop="cycleType"
            :min-width="getCheckPlanColumnMinWidthString('cycleType', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_CYCLE_TYPE" :value="scope.row.cycleType" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckPlanColumnVisible('startDate')"
            label="开始日期"
            align="center"
            prop="startDate"
            :formatter="dateFormatter2"
            :width="getCheckPlanColumnWidthString('startDate', 180)"
          />
          <el-table-column
            v-if="isCheckPlanColumnVisible('endDate')"
            label="结束日期"
            align="center"
            prop="endDate"
            :formatter="dateFormatter2"
            :width="getCheckPlanColumnWidthString('endDate', 180)"
          />
          <el-table-column
            v-if="isCheckPlanColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :min-width="getCheckPlanColumnMinWidthString('status', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_CHECK_PLAN_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckPlanColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getCheckPlanColumnWidthString('createTime', 180)"
          />
          <el-table-column
            v-if="isCheckPlanColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            :width="getCheckPlanColumnWidthString('operation', 200)"
          >
            <template #default="scope">
              <el-button
                v-if="scope.row.status === MesDvCheckPlanStatusEnum.PREPARE"
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-check-plan:update']"
              >
                编辑
              </el-button>
              <el-button
                v-if="scope.row.status === MesDvCheckPlanStatusEnum.PREPARE"
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-check-plan:delete']"
              >
                删除
              </el-button>
              <el-button
                v-if="scope.row.status === MesDvCheckPlanStatusEnum.PREPARE"
                link
                type="success"
                @click="handleEnable(scope.row.id)"
                v-hasPermi="['mes:dv-check-plan:update']"
              >
                启用
              </el-button>
              <el-button
                v-if="scope.row.status === MesDvCheckPlanStatusEnum.ENABLED"
                link
                type="warning"
                @click="handleDisable(scope.row.id)"
                v-hasPermi="['mes:dv-check-plan:update']"
              >
                停用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <CheckPlanForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter, dateFormatter2 } from '@/utils/formatTime'
import download from '@/utils/download'
import { DvCheckPlanApi, DvCheckPlanVO } from '@/api/mes/dv/checkplan'
import CheckPlanForm from './CheckPlanForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { MesDvCheckPlanStatusEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvCheckPlan' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const CHECK_PLAN_TABLE_KEY = 'mes.dv.checkPlan.main'

const checkPlanDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '方案编码', minWidth: 120 },
  { key: 'name', label: '方案名称', minWidth: 150 },
  { key: 'type', label: '方案类型', minWidth: 100 },
  { key: 'cycleCount', label: '周期数量', minWidth: 80 },
  { key: 'cycleType', label: '周期类型', minWidth: 100 },
  { key: 'startDate', label: '开始日期', width: 180 },
  { key: 'endDate', label: '结束日期', width: 180 },
  { key: 'status', label: '状态', minWidth: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 200, hideable: false, business: false }
]

const {
  columns: checkPlanColumns,
  saving: checkPlanColumnSaving,
  isColumnVisible: isCheckPlanColumnVisible,
  getColumnWidthString: getCheckPlanColumnWidthString,
  getColumnMinWidthString: getCheckPlanColumnMinWidthString,
  handleHeaderDragend: handleCheckPlanHeaderDragend,
  saveConfig: saveCheckPlanColumnConfig,
  resetConfig: resetCheckPlanColumnConfig
} = useUserTableColumns(CHECK_PLAN_TABLE_KEY, checkPlanDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<DvCheckPlanVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  type: undefined as number | string | undefined,
  status: undefined as number | string | undefined
})
const exportLoading = ref(false) // 导出的加载中

const checkPlanQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'code',
    label: '方案编码',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入方案编码'
  },
  {
    key: 'name',
    label: '方案名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入方案名称'
  },
  {
    key: 'type',
    label: '方案类型',
    type: 'select',
    queryParamKey: 'type',
    options: getIntDictOptions(DICT_TYPE.MES_DV_SUBJECT_TYPE).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.MES_DV_CHECK_PLAN_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvCheckPlanApi.getCheckPlanPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const checkPlanQuickFilter = useTableQuickFilter(
  CHECK_PLAN_TABLE_KEY,
  checkPlanQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuickFilterQuery = async () => {
  queryParams.pageNo = 1
  await checkPlanQuickFilter.applyQuickFilter()
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
    await DvCheckPlanApi.deleteCheckPlan(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 启用按钮操作 */
const handleEnable = async (id: number) => {
  try {
    await message.confirm('确认启用该点检保养方案？启用后将不可修改或删除。')
    await DvCheckPlanApi.enableCheckPlan(id)
    message.success('启用成功')
    await getList()
  } catch {}
}

/** 停用按钮操作 */
const handleDisable = async (id: number) => {
  try {
    await message.confirm('确认停用该点检保养方案？')
    await DvCheckPlanApi.disableCheckPlan(id)
    message.success('停用成功')
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvCheckPlanApi.exportCheckPlan(queryParams)
    download.excel(data, '点检保养方案.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  await getList()
})
</script>

<style scoped>
.check-plan-standard-list-table {
  width: 100%;
}
</style>
