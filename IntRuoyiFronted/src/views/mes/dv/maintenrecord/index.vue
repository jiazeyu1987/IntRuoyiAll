<template>
  <doc-alert title="【设备】点检记录、保养记录、维修单" url="https://doc.iocoder.cn/mes/dv/check-record/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.dv.maintenRecord.main"
      :query-model="queryParams"
      label-width="90px"
      :filter-definitions="maintenRecordQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="maintenRecordQuickFilter.state"
      :selected-filter-definition="maintenRecordQuickFilter.selectedDefinition.value"
      :operator-options="maintenRecordQuickFilter.operatorOptions.value"
      :columns="maintenRecordColumns"
      :column-saving="maintenRecordColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="maintenRecordQuickFilter.updateState"
      @quick-filter-query="handleQuickFilterQuery"
      @column-change="saveMaintenRecordColumnConfig"
      @column-reset="resetMaintenRecordColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:dv-mainten-record:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:dv-mainten-record:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table>
        <el-table
          v-loading="loading"
          class="mainten-record-standard-list-table"
          data-user-table-column-explicit
          data-user-table-key="mes.dv.maintenRecord.main"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleMaintenRecordHeaderDragend"
        >
          <el-table-column
            v-if="isMaintenRecordColumnVisible('machineryCode')"
            label="设备编码"
            align="center"
            prop="machineryCode"
            :min-width="getMaintenRecordColumnMinWidthString('machineryCode', 160)"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.machineryCode }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isMaintenRecordColumnVisible('machineryName')"
            label="设备名称"
            align="center"
            prop="machineryName"
            :min-width="getMaintenRecordColumnMinWidthString('machineryName', 120)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('machineryBrand')"
            label="品牌"
            align="center"
            prop="machineryBrand"
            :width="getMaintenRecordColumnWidthString('machineryBrand', 100)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('machinerySpecification')"
            label="规格型号"
            align="center"
            prop="machinerySpecification"
            :min-width="getMaintenRecordColumnMinWidthString('machinerySpecification', 120)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('planName')"
            label="计划名称"
            align="center"
            prop="planName"
            :min-width="getMaintenRecordColumnMinWidthString('planName', 120)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('maintenTime')"
            label="保养时间"
            align="center"
            prop="maintenTime"
            :formatter="dateFormatter"
            :width="getMaintenRecordColumnWidthString('maintenTime', 180)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('nickname')"
            label="保养人"
            align="center"
            prop="nickname"
            :width="getMaintenRecordColumnWidthString('nickname', 120)"
          />
          <el-table-column
            v-if="isMaintenRecordColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getMaintenRecordColumnWidthString('status', 100)"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.MES_MAINTEN_RECORD_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isMaintenRecordColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            fixed="right"
            :width="getMaintenRecordColumnWidthString('operation', 160)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:dv-mainten-record:update']"
                v-if="scope.row.status === MesDvMaintenRecordStatusEnum.PREPARE"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:dv-mainten-record:delete']"
                v-if="scope.row.status === MesDvMaintenRecordStatusEnum.PREPARE"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <MaintenRecordForm ref="formRef" @success="getList" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import { DICT_TYPE } from '@/utils/dict'
import download from '@/utils/download'
import { DvMaintenRecordApi } from '@/api/mes/dv/maintenrecord'
import MaintenRecordForm from './MaintenRecordForm.vue'
import { MesDvMaintenRecordStatusEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvMaintenRecord' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const MAINTEN_RECORD_TABLE_KEY = 'mes.dv.maintenRecord.main'

const maintenRecordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'machineryCode', label: '设备编码', minWidth: 160 },
  { key: 'machineryName', label: '设备名称', minWidth: 120 },
  { key: 'machineryBrand', label: '品牌', width: 100 },
  { key: 'machinerySpecification', label: '规格型号', minWidth: 120 },
  { key: 'planName', label: '计划名称', minWidth: 120 },
  { key: 'maintenTime', label: '保养时间', width: 180 },
  { key: 'nickname', label: '保养人', width: 120 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'operation', label: '操作', width: 160, hideable: false, business: false }
]

const {
  columns: maintenRecordColumns,
  saving: maintenRecordColumnSaving,
  isColumnVisible: isMaintenRecordColumnVisible,
  getColumnWidthString: getMaintenRecordColumnWidthString,
  getColumnMinWidthString: getMaintenRecordColumnMinWidthString,
  handleHeaderDragend: handleMaintenRecordHeaderDragend,
  saveConfig: saveMaintenRecordColumnConfig,
  resetConfig: resetMaintenRecordColumnConfig
} = useUserTableColumns(MAINTEN_RECORD_TABLE_KEY, maintenRecordDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<any[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const exportLoading = ref(false) // 导出的加载中
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  maintenTime: [] as string[]
})
const formRef = ref() // 表单弹窗

const maintenRecordQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'maintenTime',
    label: '保养时间',
    type: 'dateRange',
    queryParamKey: 'maintenTime'
  }
])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DvMaintenRecordApi.getMaintenRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const maintenRecordQuickFilter = useTableQuickFilter(
  MAINTEN_RECORD_TABLE_KEY,
  maintenRecordQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuickFilterQuery = async () => {
  queryParams.pageNo = 1
  await maintenRecordQuickFilter.applyQuickFilter()
}

/** 添加/修改操作 */
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvMaintenRecordApi.deleteMaintenRecord(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvMaintenRecordApi.exportMaintenRecord(queryParams)
    download.excel(data, '设备保养记录.xls')
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
.mainten-record-standard-list-table {
  width: 100%;
}
</style>
