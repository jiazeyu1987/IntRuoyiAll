<template>
  <doc-alert title="【设备】点检记录、保养记录、维修单" url="https://doc.iocoder.cn/mes/dv/check-record/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.repair.main"
      :query-model="queryParams"
      label-width="90px"
      :filter-definitions="repairQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="repairQuickFilter.state"
      :selected-filter-definition="repairQuickFilter.selectedDefinition.value"
      :operator-options="repairQuickFilter.operatorOptions.value"
      :columns="repairColumns"
      :column-saving="repairColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="repairQuickFilter.updateState"
      @quick-filter-query="handleQuickFilterQuery"
      @column-change="saveRepairColumnConfig"
      @column-reset="resetRepairColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-repair:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:dv-repair:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="repair-standard-list-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.repair.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleRepairHeaderDragend"
        >
          <el-table-column
            v-if="isRepairColumnVisible('code')"
            label="维修单编号"
            align="center"
            prop="code"
            :min-width="getRepairColumnMinWidthString('code', 160)"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRepairColumnVisible('name')"
            label="维修单名称"
            align="center"
            prop="name"
            :min-width="getRepairColumnMinWidthString('name', 150)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('machineryCode')"
            label="设备编码"
            align="center"
            prop="machineryCode"
            :min-width="getRepairColumnMinWidthString('machineryCode', 120)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('machineryName')"
            label="设备名称"
            align="center"
            prop="machineryName"
            :min-width="getRepairColumnMinWidthString('machineryName', 120)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('requireDate')"
            label="报修日期"
            align="center"
            prop="requireDate"
            :formatter="dateFormatter"
            :width="getRepairColumnWidthString('requireDate', 180)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('finishDate')"
            label="维修完成日期"
            align="center"
            prop="finishDate"
            :formatter="dateFormatter"
            :width="getRepairColumnWidthString('finishDate', 180)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('confirmDate')"
            label="验收日期"
            align="center"
            prop="confirmDate"
            :formatter="dateFormatter"
            :width="getRepairColumnWidthString('confirmDate', 180)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('result')"
            label="维修结果"
            align="center"
            prop="result"
            :min-width="getRepairColumnMinWidthString('result', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_REPAIR_RESULT" :value="scope.row.result" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRepairColumnVisible('acceptedUserNickname')"
            label="维修人员"
            align="center"
            prop="acceptedUserNickname"
            :min-width="getRepairColumnMinWidthString('acceptedUserNickname', 100)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('confirmUserNickname')"
            label="验收人员"
            align="center"
            prop="confirmUserNickname"
            :min-width="getRepairColumnMinWidthString('confirmUserNickname', 100)"
          />
          <el-table-column
            v-if="isRepairColumnVisible('status')"
            label="单据状态"
            align="center"
            prop="status"
            :min-width="getRepairColumnMinWidthString('status', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_REPAIR_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRepairColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            fixed="right"
            :width="getRepairColumnWidthString('operation', 240)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-repair:update']"
                v-if="scope.row.status === MesDvRepairStatusEnum.PREPARE"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-repair:delete']"
                v-if="scope.row.status === MesDvRepairStatusEnum.PREPARE"
              >
                删除
              </el-button>
              <el-button
                link
                type="success"
                @click="openForm('confirm', scope.row.id)"
                v-hasPermi="['mes:dv-repair:update']"
                v-if="scope.row.status === MesDvRepairStatusEnum.CONFIRMED"
              >
                完成维修
              </el-button>
              <el-button
                link
                type="success"
                @click="openForm('finish', scope.row.id)"
                v-hasPermi="['mes:dv-repair:update']"
                v-if="scope.row.status === MesDvRepairStatusEnum.APPROVING"
              >
                验收
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <RepairForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import download from '@/utils/download'
import { DvRepairApi, DvRepairVO } from '@/api/mes/dv/repair'
import RepairForm from './RepairForm.vue'
import { MesDvRepairStatusEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvRepair' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const REPAIR_TABLE_KEY = 'mes.dv.repair.main'

const repairDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '维修单编号', minWidth: 160 },
  { key: 'name', label: '维修单名称', minWidth: 150 },
  { key: 'machineryCode', label: '设备编码', minWidth: 120 },
  { key: 'machineryName', label: '设备名称', minWidth: 120 },
  { key: 'requireDate', label: '报修日期', width: 180 },
  { key: 'finishDate', label: '维修完成日期', width: 180 },
  { key: 'confirmDate', label: '验收日期', width: 180 },
  { key: 'result', label: '维修结果', minWidth: 100 },
  { key: 'acceptedUserNickname', label: '维修人员', minWidth: 100 },
  { key: 'confirmUserNickname', label: '验收人员', minWidth: 100 },
  { key: 'status', label: '单据状态', minWidth: 100 },
  { key: 'operation', label: '操作', width: 240, hideable: false, business: false }
]

const {
  columns: repairColumns,
  saving: repairColumnSaving,
  isColumnVisible: isRepairColumnVisible,
  getColumnWidthString: getRepairColumnWidthString,
  getColumnMinWidthString: getRepairColumnMinWidthString,
  handleHeaderDragend: handleRepairHeaderDragend,
  saveConfig: saveRepairColumnConfig,
  resetConfig: resetRepairColumnConfig
} = useUserTableColumns(REPAIR_TABLE_KEY, repairDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<DvRepairVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const exportLoading = ref(false) // 导出的加载中
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  result: undefined as number | string | undefined,
  status: undefined as number | string | undefined
})
const formRef = ref() // 表单弹窗

const repairQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'code',
    label: '维修单编号',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入维修单编号'
  },
  {
    key: 'name',
    label: '维修单名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入维修单名称'
  },
  {
    key: 'result',
    label: '维修结果',
    type: 'select',
    queryParamKey: 'result',
    options: getIntDictOptions(DICT_TYPE.MES_DV_REPAIR_RESULT).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  },
  {
    key: 'status',
    label: '单据状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.MES_DV_REPAIR_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvRepairApi.getRepairPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const repairQuickFilter = useTableQuickFilter(
  REPAIR_TABLE_KEY,
  repairQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuickFilterQuery = async () => {
  queryParams.pageNo = 1
  await repairQuickFilter.applyQuickFilter()
}

/** 添加/修改/详情/完成维修/验收操作 */
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvRepairApi.deleteRepair(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvRepairApi.exportRepair(queryParams)
    download.excel(data, '维修工单.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style scoped>
.repair-standard-list-table {
  width: 100%;
}
</style>
