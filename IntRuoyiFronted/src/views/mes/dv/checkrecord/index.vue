<template>
  <doc-alert title="【设备】点检记录、保养记录、维修单" url="https://doc.iocoder.cn/mes/dv/check-record/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.checkRecord.main"
      :query-model="queryParams"
      label-width="90px"
      :filter-definitions="checkRecordQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="checkRecordQuickFilter.state"
      :selected-filter-definition="checkRecordQuickFilter.selectedDefinition.value"
      :operator-options="checkRecordQuickFilter.operatorOptions.value"
      :columns="checkRecordColumns"
      :column-saving="checkRecordColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="checkRecordQuickFilter.updateState"
      @quick-filter-query="handleQuickFilterQuery"
      @column-change="saveCheckRecordColumnConfig"
      @column-reset="resetCheckRecordColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-check-record:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:dv-check-record:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="check-record-standard-list-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.checkRecord.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleCheckRecordHeaderDragend"
        >
          <el-table-column
            v-if="isCheckRecordColumnVisible('machineryCode')"
            label="设备编码"
            align="center"
            prop="machineryCode"
            :min-width="getCheckRecordColumnMinWidthString('machineryCode', 140)"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.machineryCode }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckRecordColumnVisible('machineryName')"
            label="设备名称"
            align="center"
            prop="machineryName"
            :min-width="getCheckRecordColumnMinWidthString('machineryName', 120)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('machineryBrand')"
            label="品牌"
            align="center"
            prop="machineryBrand"
            :width="getCheckRecordColumnWidthString('machineryBrand', 100)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('machinerySpecification')"
            label="规格型号"
            align="center"
            prop="machinerySpecification"
            :min-width="getCheckRecordColumnMinWidthString('machinerySpecification', 120)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('planCode')"
            label="计划编码"
            align="center"
            prop="planCode"
            :min-width="getCheckRecordColumnMinWidthString('planCode', 120)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('planName')"
            label="计划名称"
            align="center"
            prop="planName"
            :min-width="getCheckRecordColumnMinWidthString('planName', 120)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('checkTime')"
            label="点检时间"
            align="center"
            prop="checkTime"
            :formatter="dateFormatter"
            :width="getCheckRecordColumnWidthString('checkTime', 180)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('nickname')"
            label="点检人"
            align="center"
            prop="nickname"
            :width="getCheckRecordColumnWidthString('nickname', 100)"
          />
          <el-table-column
            v-if="isCheckRecordColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getCheckRecordColumnWidthString('status', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_DV_CHECK_RECORD_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCheckRecordColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            fixed="right"
            :width="getCheckRecordColumnWidthString('operation', 200)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-check-record:update']"
                v-if="scope.row.status === MesDvCheckRecordStatusEnum.DRAFT"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-check-record:delete']"
                v-if="scope.row.status === MesDvCheckRecordStatusEnum.DRAFT"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <CheckRecordForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import download from '@/utils/download'
import { DvCheckRecordApi } from '@/api/mes/dv/checkrecord'
import CheckRecordForm from './CheckRecordForm.vue'
import { MesDvCheckRecordStatusEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvCheckRecord' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const CHECK_RECORD_TABLE_KEY = 'mes.dv.checkRecord.main'

const checkRecordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'machineryCode', label: '设备编码', minWidth: 140 },
  { key: 'machineryName', label: '设备名称', minWidth: 120 },
  { key: 'machineryBrand', label: '品牌', width: 100 },
  { key: 'machinerySpecification', label: '规格型号', minWidth: 120 },
  { key: 'planCode', label: '计划编码', minWidth: 120 },
  { key: 'planName', label: '计划名称', minWidth: 120 },
  { key: 'checkTime', label: '点检时间', width: 180 },
  { key: 'nickname', label: '点检人', width: 100 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'operation', label: '操作', width: 200, hideable: false, business: false }
]

const {
  columns: checkRecordColumns,
  saving: checkRecordColumnSaving,
  isColumnVisible: isCheckRecordColumnVisible,
  getColumnWidthString: getCheckRecordColumnWidthString,
  getColumnMinWidthString: getCheckRecordColumnMinWidthString,
  handleHeaderDragend: handleCheckRecordHeaderDragend,
  saveConfig: saveCheckRecordColumnConfig,
  resetConfig: resetCheckRecordColumnConfig
} = useUserTableColumns(CHECK_RECORD_TABLE_KEY, checkRecordDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<any[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const exportLoading = ref(false) // 导出的加载中
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  status: undefined as number | string | undefined,
  checkTime: [] as string[]
})
const formRef = ref() // 表单弹窗

const checkRecordQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.MES_DV_CHECK_RECORD_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  },
  {
    key: 'checkTime',
    label: '点检时间',
    type: 'dateRange',
    queryParamKey: 'checkTime'
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvCheckRecordApi.getCheckRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const checkRecordQuickFilter = useTableQuickFilter(
  CHECK_RECORD_TABLE_KEY,
  checkRecordQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuickFilterQuery = async () => {
  queryParams.pageNo = 1
  await checkRecordQuickFilter.applyQuickFilter()
}

/** 添加/修改操作 */
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvCheckRecordApi.deleteCheckRecord(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvCheckRecordApi.exportCheckRecord(queryParams)
    download.excel(data, '设备点检记录.xls')
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
.check-record-standard-list-table {
  width: 100%;
}
</style>
