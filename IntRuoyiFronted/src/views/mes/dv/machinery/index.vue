<!-- MES 设备台账列表 -->
<template>
  <doc-alert title="【设备】设备类型、设备台账" url="https://doc.iocoder.cn/mes/dv/device/" />

  <el-row :gutter="20">
    <el-col :span="4" :xs="24">
      <ContentWrap class="h-1/1">
        <MachineryTypeTree @node-click="handleTypeNodeClick" />
      </ContentWrap>
    </el-col>
    <el-col :span="20" :xs="24">
      <ContentWrap>
        <UnifiedListTemplate
          table-key="mes.dv.machinery.main"
          :query-model="queryParams"
          label-width="100px"
          :filter-definitions="machineryQuickFilterDefinitions"
          :show-quick-filter-label="false"
          :quick-filter-state="machineryQuickFilter.state"
          :selected-filter-definition="machineryQuickFilter.selectedDefinition.value"
          :operator-options="machineryQuickFilter.operatorOptions.value"
          :columns="machineryColumns"
          :column-saving="machineryColumnSaving"
          :show-column-reset="false"
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="machineryQuickFilter.updateState"
          @quick-filter-query="handleQuery"
          @column-change="saveMachineryColumnConfig"
          @column-reset="resetMachineryColumnConfig"
          @pagination="getList"
        >
          <template #actions>
            <el-button
              v-hasPermi="['mes:dv-machinery:create']"
              type="primary"
              plain
              @click="openForm('create')"
            >
              <Icon icon="ep:plus" class="mr-5px" /> 新增
            </el-button>
            <el-button
              v-hasPermi="['mes:dv-machinery:import']"
              type="warning"
              plain
              @click="handleImport"
            >
              <Icon icon="ep:upload" class="mr-5px" /> 导入
            </el-button>
            <el-button
              v-hasPermi="['mes:dv-machinery:export']"
              type="success"
              plain
              :loading="exportLoading"
              @click="handleExport"
            >
              <Icon icon="ep:download" class="mr-5px" /> 导出
            </el-button>
          </template>

          <template #table>
            <el-table
              v-loading="loading"
              class="machinery-main-table"
              data-user-table-column-explicit
              data-user-table-key="mes.dv.machinery.main"
              :data="list"
              border
              :stripe="true"
              :show-overflow-tooltip="true"
              row-key="id"
              @header-dragend="handleMachineryHeaderDragend"
            >
              <el-table-column
                v-if="isMachineryColumnVisible('code')"
                label="设备编码"
                align="center"
                prop="code"
                :width="getMachineryColumnWidthString('code', 120)"
              >
                <template #default="scope">
                  <el-link type="primary" @click="openForm('detail', scope.row.id)">
                    {{ scope.row.code }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column
                v-if="isMachineryColumnVisible('name')"
                label="设备名称"
                align="center"
                prop="name"
                :min-width="getMachineryColumnMinWidthString('name', 150)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('brand')"
                label="品牌"
                align="center"
                prop="brand"
                :width="getMachineryColumnWidthString('brand', 100)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('specification')"
                label="规格型号"
                align="center"
                prop="specification"
                :width="getMachineryColumnWidthString('specification', 120)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('machineryTypeName')"
                label="设备类型"
                align="center"
                prop="machineryTypeName"
                :width="getMachineryColumnWidthString('machineryTypeName', 120)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('workshopName')"
                label="所属车间"
                align="center"
                prop="workshopName"
                :width="getMachineryColumnWidthString('workshopName', 120)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('processName')"
                label="工序名称"
                align="center"
                prop="processName"
                :min-width="getMachineryColumnMinWidthString('processName', 160)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('standardHourlyCapacity')"
                label="设备标准小时产能"
                align="center"
                prop="standardHourlyCapacity"
                :width="getMachineryColumnWidthString('standardHourlyCapacity', 140)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('status')"
                label="设备状态"
                align="center"
                prop="status"
                :width="getMachineryColumnWidthString('status', 100)"
              >
                <template #default="scope">
                  <dict-tag :type="DICT_TYPE.MES_DV_MACHINERY_STATUS" :value="scope.row.status" />
                </template>
              </el-table-column>
              <el-table-column
                v-if="isMachineryColumnVisible('createTime')"
                label="创建时间"
                align="center"
                prop="createTime"
                :formatter="dateFormatter"
                :width="getMachineryColumnWidthString('createTime', 180)"
              />
              <el-table-column
                v-if="isMachineryColumnVisible('operation')"
                label="操作"
                align="center"
                prop="operation"
                :width="getMachineryColumnWidthString('operation', 170)"
              >
                <template #default="scope">
                  <el-button
                    v-hasPermi="['mes:dv-machinery:update']"
                    link
                    type="primary"
                    @click="openForm('update', scope.row.id)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-hasPermi="['mes:dv-machinery:delete']"
                    link
                    type="danger"
                    @click="handleDelete(scope.row.id)"
                  >
                    删除
                  </el-button>
                  <el-button
                    v-hasPermi="['mes:dv-machinery:query']"
                    link
                    type="primary"
                    @click="handleBarcode(scope.row)"
                  >
                    条码
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </ContentWrap>
    </el-col>
  </el-row>

  <MachineryForm ref="formRef" @success="getList" />
  <MachineryImportForm ref="importFormRef" @success="getList" />
  <BarcodeDetail ref="barcodeDetailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { DvMachineryApi, DvMachineryVO } from '@/api/mes/dv/machinery'
import MachineryForm from './MachineryForm.vue'
import MachineryTypeTree from './type/components/MachineryTypeTree.vue'
import MachineryImportForm from './MachineryImportForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { BarcodeDetail } from '@/views/mes/wm/barcode/components'
import { BarcodeBizTypeEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesDvMachinery' })

const message = useMessage()
const { t } = useI18n()
const route = useRoute()

const MACHINERY_TABLE_KEY = 'mes.dv.machinery.main'

const machineryDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '设备编码', width: 120 },
  { key: 'name', label: '设备名称', minWidth: 150 },
  { key: 'brand', label: '品牌', width: 100 },
  { key: 'specification', label: '规格型号', width: 120 },
  { key: 'machineryTypeName', label: '设备类型', width: 120 },
  { key: 'workshopName', label: '所属车间', width: 120 },
  { key: 'processName', label: '工序名称', minWidth: 160 },
  { key: 'standardHourlyCapacity', label: '设备标准小时产能', width: 140 },
  { key: 'status', label: '设备状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 170, hideable: false, business: false }
]
const {
  columns: machineryColumns,
  saving: machineryColumnSaving,
  isColumnVisible: isMachineryColumnVisible,
  getColumnWidthString: getMachineryColumnWidthString,
  getColumnMinWidthString: getMachineryColumnMinWidthString,
  handleHeaderDragend: handleMachineryHeaderDragend,
  saveConfig: saveMachineryColumnConfig,
  resetConfig: resetMachineryColumnConfig
} = useUserTableColumns(MACHINERY_TABLE_KEY, machineryDefaultColumns)

const loading = ref(true)
const list = ref<DvMachineryVO[]>([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  machineryTypeId: undefined as number | undefined,
  status: undefined as number | string | undefined
})
const exportLoading = ref(false)

const machineryQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'status',
    label: '设备状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.MES_DV_MACHINERY_STATUS).map((dict) => ({
      label: dict.label,
      value: dict.value as string | number | boolean
    }))
  }
])

const getList = async () => {
  loading.value = true
  try {
    const data = await DvMachineryApi.getMachineryPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const machineryQuickFilter = useTableQuickFilter(
  MACHINERY_TABLE_KEY,
  machineryQuickFilterDefinitions,
  queryParams,
  getList
)

const handleQuery = async () => {
  queryParams.pageNo = 1
  await machineryQuickFilter.applyQuickFilter()
}

const handleTypeNodeClick = (row: any) => {
  queryParams.machineryTypeId = row?.id
  handleQuery()
}

const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const openedMachineryDetailId = ref('')
const tryOpenDetailFromRoute = () => {
  const openId = Array.isArray(route.query.openId) ? route.query.openId[0] : route.query.openId
  if (!openId) {
    openedMachineryDetailId.value = ''
    return
  }
  if (openedMachineryDetailId.value === openId) return
  const machineryId = Number(openId)
  if (!Number.isFinite(machineryId)) {
    throw new Error(`设备详情定位参数无效：${openId}`)
  }
  openedMachineryDetailId.value = openId
  openForm('detail', Number(openId))
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await DvMachineryApi.deleteMachinery(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const barcodeDetailRef = ref()
const handleBarcode = async (row: DvMachineryVO) => {
  await barcodeDetailRef.value.openByBusiness(
    row.id,
    BarcodeBizTypeEnum.MACHINERY,
    row.code,
    row.name
  )
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await DvMachineryApi.exportMachinery(queryParams)
    download.excel(data, '设备台账.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
}

const importFormRef = ref()
const handleImport = () => {
  importFormRef.value.open()
}

onMounted(async () => {
  await getList()
  tryOpenDetailFromRoute()
})

watch(
  () => route.query.openId,
  () => {
    tryOpenDetailFromRoute()
  }
)
</script>

<style scoped>
.machinery-main-table {
  width: 100%;
}
</style>
