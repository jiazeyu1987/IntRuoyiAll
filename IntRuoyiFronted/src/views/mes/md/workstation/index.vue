<template>
  <doc-alert title="【基础】车间设置、工作站设置" url="https://doc.iocoder.cn/mes/md/workshop/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.md.workstation.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="workstationQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="workstationQuickFilter.state"
      :selected-filter-definition="workstationQuickFilter.selectedDefinition.value"
      :operator-options="workstationQuickFilter.operatorOptions.value"
      :columns="workstationColumns"
      :column-saving="workstationColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="workstationQuickFilter.updateState"
      @quick-filter-query="workstationQuickFilter.applyQuickFilter"
      @column-change="saveWorkstationColumnConfig"
      @column-reset="resetWorkstationColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:md-workstation:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:md-workstation:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="workstation-main-table"
          data-user-table-column-explicit
          data-user-table-key="mes.md.workstation.main"
          :data="list"
          :height="workstationMainTableHeight"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleWorkstationHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isWorkstationColumnVisible('code')"
            label="工作站编码"
            align="center"
            prop="code"
            :width="getWorkstationColumnWidthString('code', 150)"
            v-bind="sortColumnAttrs('code')"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('name')"
            label="工作站名称"
            align="center"
            prop="name"
            :width="getWorkstationColumnWidthString('name', 170)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isWorkstationColumnVisible('address')"
            label="工作站地点"
            align="center"
            prop="address"
            :width="getWorkstationColumnWidthString('address', 170)"
            v-bind="sortColumnAttrs('address')"
          />
          <el-table-column
            v-if="isWorkstationColumnVisible('workshopName')"
            label="所在车间"
            align="center"
            prop="workshopName"
            :width="getWorkstationColumnWidthString('workshopName', 140)"
            v-bind="sortColumnAttrs('workshopName')"
          />
          <el-table-column
            v-if="isWorkstationColumnVisible('processName')"
            label="所属工序"
            align="center"
            prop="processName"
            :width="getWorkstationColumnWidthString('processName', 140)"
            v-bind="sortColumnAttrs('processName')"
          />
          <el-table-column
            v-if="isWorkstationColumnVisible('machineryCount')"
            label="绑定设备"
            align="center"
            prop="machineryCount"
            :width="getWorkstationColumnWidthString('machineryCount', 110)"
            v-bind="sortColumnAttrs('machineryCount')"
          >
            <template #default="scope">
              <el-button
                v-if="scope.row.machineryCount > 0"
                link
                type="primary"
                @click="openWorkstationMachineDialog(scope.row)"
              >
                {{ scope.row.machineryCount }}
              </el-button>
              <span v-else>0</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('configuredWorkerCount')"
            label="理论配置人数"
            align="center"
            prop="configuredWorkerCount"
            :width="getWorkstationColumnWidthString('configuredWorkerCount', 120)"
            v-bind="sortColumnAttrs('configuredWorkerCount')"
          >
            <template #default="scope">
              {{ scope.row.configuredWorkerCount ?? 0 }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('currentWorkerCount')"
            label="当前在岗人数"
            align="center"
            prop="currentWorkerCount"
            :width="getWorkstationColumnWidthString('currentWorkerCount', 120)"
            v-bind="sortColumnAttrs('currentWorkerCount')"
          >
            <template #default="scope">
              {{ scope.row.currentWorkerCount ?? 0 }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('singleStandardHourlyCapacity')"
            label="人工标准小时产能"
            align="center"
            prop="singleStandardHourlyCapacity"
            :width="getWorkstationColumnWidthString('singleStandardHourlyCapacity', 150)"
            v-bind="sortColumnAttrs('singleStandardHourlyCapacity')"
          >
            <template #default="scope">
              {{ formatWorkstationIntegerCapacity(scope.row.singleStandardHourlyCapacity, '-') }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('machineryStandardHourlyCapacity')"
            label="设备标准小时产能"
            align="center"
            prop="machineryStandardHourlyCapacity"
            :width="getWorkstationColumnWidthString('machineryStandardHourlyCapacity', 150)"
            v-bind="sortColumnAttrs('machineryStandardHourlyCapacity')"
          >
            <template #default="scope">
              {{ formatWorkstationIntegerCapacity(scope.row.machineryStandardHourlyCapacity) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('shiftHours')"
            label="班次小时"
            align="center"
            prop="shiftHours"
            :width="getWorkstationColumnWidthString('shiftHours', 120)"
            v-bind="sortColumnAttrs('shiftHours')"
          >
            <template #default="scope">
              {{ scope.row.shiftHours ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('todayCapacity')"
            label="班次产能"
            align="center"
            prop="todayCapacity"
            :width="getWorkstationColumnWidthString('todayCapacity', 120)"
            v-bind="sortColumnAttrs('todayCapacity')"
          >
            <template #default="scope">
              {{ formatWorkstationIntegerCapacity(scope.row.todayCapacity) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getWorkstationColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isWorkstationColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getWorkstationColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isWorkstationColumnVisible('operation')"
            label="操作"
            align="center"
            prop="operation"
            fixed="right"
            :width="getWorkstationColumnWidthString('operation', 190)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id)"
                v-hasPermi="['mes:md-workstation:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:md-workstation:delete']"
              >
                删除
              </el-button>
              <el-button
                link
                type="primary"
                @click="handleBarcode(scope.row)"
                v-hasPermi="['mes:md-workstation:query']"
              >
                条码
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog
    :title="workstationMachineDialogTitle"
    v-model="workstationMachineDialogVisible"
    width="760px"
    @closed="handleWorkstationMachineDialogClosed"
  >
    <el-table
      v-loading="workstationMachineLoading"
      :data="workstationMachineList"
      :stripe="true"
      :show-overflow-tooltip="true"
      border
    >
      <el-table-column label="设备编码" align="center" prop="machineryCode" min-width="150">
        <template #default="scope">
          <el-link type="primary" @click="openMachineryLedger(scope.row.machineryId)">
            {{ scope.row.machineryCode }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="设备名称" align="center" prop="machineryName" min-width="180">
        <template #default="scope">
          <el-link type="primary" @click="openMachineryLedger(scope.row.machineryId)">
            {{ scope.row.machineryName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="数量" align="center" prop="quantity" width="100" />
      <el-table-column label="备注" align="center" prop="remark" min-width="180" />
    </el-table>
    <template #footer>
      <el-button @click="workstationMachineDialogVisible = false">关闭</el-button>
    </template>
  </Dialog>

  <!-- 表单弹窗：添加/修改 -->
  <WorkstationForm ref="formRef" @success="getList" />
  <!-- 条码详情弹窗 -->
  <BarcodeDetail ref="barcodeDetailRef" />
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import { MdWorkstationApi, type MdWorkstationVO } from '@/api/mes/md/workstation'
import {
  MdWorkstationMachineApi,
  type MdWorkstationMachineVO
} from '@/api/mes/md/workstation/machine'
import WorkstationForm from './WorkstationForm.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { BarcodeDetail } from '@/views/mes/wm/barcode/components'
import { BarcodeBizTypeEnum } from '@/views/mes/utils/constants'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesMdWorkstation' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()

type WorkstationQuickFilterFieldKey = 'code' | 'name' | 'status'

const WORKSTATION_ROUTE_PATH = '/mes/md/workstation'
const workstationMainTableHeight = 'max(360px, calc(100vh - 300px))'
const workstationDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '工作站编码', width: 150, hideable: false },
  { key: 'name', label: '工作站名称', width: 170 },
  { key: 'address', label: '工作站地点', width: 170 },
  { key: 'workshopName', label: '所在车间', width: 140 },
  { key: 'processName', label: '所属工序', width: 140 },
  { key: 'machineryCount', label: '绑定设备', width: 110 },
  { key: 'configuredWorkerCount', label: '理论配置人数', width: 120 },
  { key: 'currentWorkerCount', label: '当前在岗人数', width: 120 },
  { key: 'singleStandardHourlyCapacity', label: '人工标准小时产能', width: 150 },
  { key: 'machineryStandardHourlyCapacity', label: '设备标准小时产能', width: 150 },
  { key: 'shiftHours', label: '班次小时', width: 120 },
  { key: 'todayCapacity', label: '班次产能', width: 120 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 190, hideable: false, business: false }
]
const {
  columns: workstationColumns,
  saving: workstationColumnSaving,
  isColumnVisible: isWorkstationColumnVisible,
  getColumnWidthString: getWorkstationColumnWidthString,
  handleHeaderDragend: handleWorkstationHeaderDragend,
  saveConfig: saveWorkstationColumnConfig,
  resetConfig: resetWorkstationColumnConfig
} = useUserTableColumns('mes.md.workstation.main', workstationDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<MdWorkstationVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const openedWorkstationDetailId = ref('')
const exportLoading = ref(false) // 导出的加载中
const workstationMachineDialogVisible = ref(false)
const workstationMachineDialogTitle = ref('绑定设备')
const workstationMachineLoading = ref(false)
const workstationMachineList = ref<MdWorkstationMachineVO[]>([])
const pendingMachineryLedgerId = ref<number | undefined>()
const routeQuickFilterApplied = ref(false)
const lastAppliedWorkstationRouteQuerySignature = ref('')
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  workshopId: undefined as number | undefined,
  processId: undefined as number | undefined,
  status: undefined as number | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const normalizePositiveProcessId = (value?: number | string | null) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}

const formatWorkstationIntegerCapacity = (value?: number | string | null, emptyText = '0') => {
  if (value == null || value === '') {
    return emptyText
  }
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    return String(value)
  }
  return Math.round(parsed).toString()
}

const workstationQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'code', label: '工作站编码', type: 'text', placeholder: '请输入工作站编码' },
  { key: 'name', label: '工作站名称', type: 'text', placeholder: '请输入工作站名称' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])

const workstationQuickFilter = useTableQuickFilter(
  'mes.md.workstation.main',
  workstationQuickFilterDefinitions,
  queryParams,
  getList
)

const clearWorkstationQuickFilterFields = () => {
  queryParams.code = undefined
  queryParams.name = undefined
  queryParams.status = undefined
}

const applyWorkstationQuickFilterToQueryParams = () => {
  clearWorkstationQuickFilterFields()
  const quickFilter = queryParams.quickFilter
  if (!quickFilter) return

  const fieldKey = quickFilter.fieldKey as WorkstationQuickFilterFieldKey
  if (fieldKey === 'code') {
    queryParams.code = String(quickFilter.value ?? '').trim() || undefined
    return
  }
  if (fieldKey === 'name') {
    queryParams.name = String(quickFilter.value ?? '').trim() || undefined
    return
  }
  if (fieldKey === 'status') {
    const parsed = Number(quickFilter.value)
    if (!Number.isFinite(parsed)) {
      const errorMessage = '状态必须是有效数字。'
      message.error(errorMessage)
      throw new Error(errorMessage)
    }
    queryParams.status = parsed
  }
}

const buildWorkstationPageParams = () => {
  applyWorkstationQuickFilterToQueryParams()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    code: queryParams.code,
    name: queryParams.name,
    workshopId: queryParams.workshopId,
    processId: normalizePositiveProcessId(queryParams.processId),
    status: queryParams.status
  }
}

/** 查询列表 */
async function getList() {
  loading.value = true
  try {
    const data = await MdWorkstationApi.getWorkstationPage(buildWorkstationPageParams())
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

const openWorkstationMachineDialog = async (row: MdWorkstationVO) => {
  if (!row.id) {
    throw new Error('工作站缺少编号，无法查看绑定设备。')
  }
  workstationMachineDialogTitle.value = `${row.code || row.name} 绑定设备`
  workstationMachineDialogVisible.value = true
  workstationMachineLoading.value = true
  try {
    workstationMachineList.value = await MdWorkstationMachineApi.getWorkstationMachineList(row.id)
  } finally {
    workstationMachineLoading.value = false
  }
}

const openMachineryLedger = async (machineryId?: number) => {
  const normalizedMachineryId = Number(machineryId)
  if (!Number.isFinite(normalizedMachineryId) || normalizedMachineryId <= 0) {
    throw new Error('设备绑定缺少设备编号，无法跳转设备台账。')
  }
  pendingMachineryLedgerId.value = normalizedMachineryId
  workstationMachineDialogVisible.value = false
}

const handleWorkstationMachineDialogClosed = async () => {
  const machineryId = pendingMachineryLedgerId.value
  if (!machineryId) {
    return
  }
  pendingMachineryLedgerId.value = undefined
  await router.push({ path: '/mes/dv/machinery', query: { openId: String(machineryId) } })
}

const clearRouteQuickFilter = () => {
  queryParams.quickFilter = undefined
  queryParams.code = undefined
  queryParams.name = undefined
  workstationQuickFilter.updateState({ fieldKey: 'code', operator: 'contains', value: undefined })
  routeQuickFilterApplied.value = false
}

const applyRouteQuickFilter = (fieldKey: 'code' | 'name', value: string) => {
  const quickFilter = { fieldKey, operator: 'contains' as const, value }
  queryParams.quickFilter = quickFilter
  workstationQuickFilter.updateState(quickFilter)
  routeQuickFilterApplied.value = true
}

const syncQueryParamsFromRoute = () => {
  const code = typeof route.query.code === 'string' ? route.query.code : undefined
  const name = typeof route.query.name === 'string' ? route.query.name : undefined
  const processIdText = typeof route.query.processId === 'string' ? route.query.processId : ''
  queryParams.processId = normalizePositiveProcessId(processIdText)

  if (code) {
    applyRouteQuickFilter('code', code)
    return
  }
  if (name) {
    applyRouteQuickFilter('name', name)
    return
  }
  if (routeQuickFilterApplied.value || 'code' in route.query || 'name' in route.query) {
    clearRouteQuickFilter()
  }
}

const buildWorkstationRouteQuerySignature = () =>
  JSON.stringify({
    code: typeof route.query.code === 'string' ? route.query.code : '',
    name: typeof route.query.name === 'string' ? route.query.name : '',
    processId: typeof route.query.processId === 'string' ? route.query.processId : '',
    openId: typeof route.query.openId === 'string' ? route.query.openId : ''
  })

const tryOpenDetailFromRoute = () => {
  const openId = typeof route.query.openId === 'string' ? route.query.openId : ''
  if (!openId) {
    openedWorkstationDetailId.value = ''
    return
  }
  if (openedWorkstationDetailId.value === openId) {
    return
  }
  openedWorkstationDetailId.value = openId
  openForm('detail', Number(openId))
}

const isUserCancel = (error: unknown) => error === 'cancel' || error === 'close'

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await MdWorkstationApi.deleteWorkstation(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (!isUserCancel(error)) throw error
  }
}

/** 查看工位条码 */
const barcodeDetailRef = ref()
const handleBarcode = async (row: MdWorkstationVO) => {
  await barcodeDetailRef.value.openByBusiness(
    row.id,
    BarcodeBizTypeEnum.WORKSTATION,
    row.code,
    row.name
  )
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await MdWorkstationApi.exportWorkstation(buildWorkstationPageParams())
    download.excel(data, '工作站.xls')
  } catch (error) {
    if (!isUserCancel(error)) throw error
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(async () => {
  lastAppliedWorkstationRouteQuerySignature.value = buildWorkstationRouteQuerySignature()
  syncQueryParamsFromRoute()
  await getList()
  tryOpenDetailFromRoute()
})

watch(
  () => route.fullPath,
  async () => {
    if (route.path !== WORKSTATION_ROUTE_PATH) {
      return
    }
    const nextSignature = buildWorkstationRouteQuerySignature()
    if (nextSignature === lastAppliedWorkstationRouteQuerySignature.value) {
      return
    }
    lastAppliedWorkstationRouteQuerySignature.value = nextSignature
    syncQueryParamsFromRoute()
    await getList()
    tryOpenDetailFromRoute()
  }
)
</script>

<style scoped>
.workstation-main-table {
  width: 100%;
}

.workstation-main-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.workstation-main-table :deep(.el-scrollbar__bar.is-horizontal) {
  display: block;
  height: 8px;
  opacity: 1;
}

.workstation-main-table :deep(.el-scrollbar__bar.is-horizontal > div) {
  background-color: #9caec4;
}
</style>
