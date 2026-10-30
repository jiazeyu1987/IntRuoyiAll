<!-- MES 生产工序列表 -->
<template>
  <doc-alert title="【生产】工序设置、工艺流程" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <ContentWrap class="process-list-page">
    <UnifiedListTemplate
      table-key="mes.pro.process.main"
      single-line-toolbar
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="processQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="processQuickFilter.state"
      :selected-filter-definition="processQuickFilter.selectedDefinition.value"
      :operator-options="processQuickFilter.operatorOptions.value"
      :columns="processColumns"
      :column-saving="processColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="processQuickFilter.updateState"
      @quick-filter-query="processQuickFilter.applyQuickFilter"
      @column-change="saveProcessColumnConfig"
      @column-reset="resetProcessColumnConfig"
      @pagination="getList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:pro-process:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </template>
      <template #table="{ resolveRows, sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="process-main-table"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.process.main"
          :data="resolveRows(list)"
          height="100%"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleProcessHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isProcessColumnVisible('code')"
            label="工序编码"
            align="center"
            prop="code"
            :width="getProcessColumnWidthString('code', 150)"
            v-bind="sortColumnAttrs('code')"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id, scope.row)">
                {{ scope.row.code }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessColumnVisible('name')"
            label="工序名称"
            align="center"
            prop="name"
            :width="getProcessColumnWidthString('name', 200)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isProcessColumnVisible('workstationNames')"
            label="工作站"
            align="center"
            prop="workstationNames"
            :width="getProcessColumnWidthString('workstationNames', 240)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('workstationNames')"
          >
            <template #default="scope">
              <div v-if="scope.row.workstations?.length" class="process-workstation-tags">
                <el-tag
                  v-for="workstation in scope.row.workstations"
                  :key="workstation.id"
                  size="small"
                  effect="plain"
                  class="process-workstation-tag process-workstation-link-tag"
                  :title="formatProcessWorkstation(workstation)"
                  role="link"
                  tabindex="0"
                  @click="openProcessWorkstation(workstation, scope.row)"
                  @keydown.enter="openProcessWorkstation(workstation, scope.row)"
                  @keydown.space.prevent="openProcessWorkstation(workstation, scope.row)"
                >
                  {{ formatProcessWorkstation(workstation) }}
                </el-tag>
              </div>
              <span v-else class="process-unconfigured">未配置</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessColumnVisible('productionQuantityFactor')"
            label="生产系数"
            align="center"
            prop="productionQuantityFactor"
            :width="getProcessColumnWidthString('productionQuantityFactor', 120)"
            v-bind="sortColumnAttrs('productionQuantityFactor')"
          >
            <template #default="scope">
              <span>{{ formatCapacity(scope.row.productionQuantityFactor) }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getProcessColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessColumnVisible('remark')"
            label="备注"
            align="center"
            prop="remark"
            :width="getProcessColumnWidthString('remark', 360)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('remark')"
          />
          <el-table-column
            v-if="isProcessColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getProcessColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            v-if="isProcessColumnVisible('operation')"
            label="操作"
            align="center"
            fixed="right"
            :width="getProcessColumnWidthString('operation', 150)"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                @click="openForm('update', scope.row.id, scope.row)"
                v-hasPermi="['mes:pro-process:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:pro-process:delete']"
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
  <ProProcessForm ref="formRef" @success="getList" />

  <Dialog :title="processMachineryDialogTitle" v-model="processMachineryDialogVisible" width="920px">
    <el-table
      v-loading="processMachineryLoading"
      :data="processMachineryList"
      :stripe="true"
      :show-overflow-tooltip="true"
    >
      <el-table-column label="设备编码" align="center" prop="machineryCode" width="130" />
      <el-table-column label="设备名称" align="center" prop="machineryName" min-width="220" />
      <el-table-column label="设备状态" align="center" prop="machineryStatus" width="100" />
      <el-table-column label="维修状态" align="center" prop="availabilityStatus" width="130">
        <template #default="scope">
          <el-tag :type="scope.row.underRepair ? 'danger' : 'success'">
            {{ formatAvailabilityStatus(scope.row.availabilityStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="单台班产能" align="center" prop="shiftCapacity" width="130">
        <template #default="scope">
          <span>{{ formatCapacity(scope.row.shiftCapacity) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="当前可用班产能"
        align="center"
        prop="availableShiftCapacity"
        width="150"
      >
        <template #default="scope">
          <span>{{ formatCapacity(scope.row.availableShiftCapacity) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="说明" align="center" prop="availabilityReason" min-width="180" />
    </el-table>
  </Dialog>
</template>

<script setup lang="ts">
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'
import { getDictOptions as listColumnDictOptions } from '@/utils/dict'

import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import {
  ProProcessApi,
  ProProcessMachineryVO,
  ProProcessVO,
  ProProcessWorkstationVO
} from '@/api/mes/pro/process'
import { ProRouteApi, type ProRouteVO } from '@/api/mes/pro/route'
import ProProcessForm from './ProProcessForm.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterOption,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
defineOptions({ name: 'MesProProcess' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()

type ProcessQuickFilterFieldKey = 'code' | 'name' | 'routeId' | 'status'

const PROCESS_ROUTE_PATH = '/mes/pro/process'

const processDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '工序编码', width: 150 },
  { key: 'name', label: '工序名称', width: 200 },
  { key: 'workstationNames', label: '工作站', width: 240, hideable: false },
  { key: 'productionQuantityFactor', label: '生产系数', width: 120, hideable: false },
  { key: 'status', label: '状态', width: 100 },
  { key: 'remark', label: '备注', width: 360 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'operation', label: '操作', width: 150, hideable: false, business: false }
]
const {
  columns: processColumns,
  saving: processColumnSaving,
  isColumnVisible: isProcessColumnVisible,
  getColumnWidthString: getProcessColumnWidthString,
  handleHeaderDragend: handleProcessHeaderDragend,
  saveConfig: saveProcessColumnConfig,
  resetConfig: resetProcessColumnConfig
} = useUserTableColumns('mes.pro.process.main', processDefaultColumns)

const loading = ref(true) // 列表的加载中
const list = ref<ProProcessVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const openedProcessDetailId = ref('')
const lastAppliedProcessRouteQuerySignature = ref('')
const processRouteQuickFilterApplied = ref(false)
const processMachineryDialogVisible = ref(false)
const processMachineryDialogTitle = ref('关联设备产能明细')
const processMachineryLoading = ref(false)
const processMachineryList = ref<ProProcessMachineryVO[]>([])
const routeOptions = ref<ProRouteVO[]>([])
const routeQuickFilterOptions = computed<TableQuickFilterOption[]>(() =>
  routeOptions.value.map((item) => ({
    label: item.name,
    value: item.id!
  }))
)
const formatProcessWorkstation = (workstation: ProProcessWorkstationVO) => {
  const code = workstation.code?.trim()
  const name = workstation.name?.trim()
  if (code && name) return `${code} ${name}`
  return name || code || String(workstation.id)
}

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  routeId: undefined as number | undefined,
  status: undefined as number | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})
const formRef = ref() // 表单弹窗

const processQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  { key: 'code', label: '工序编码', type: 'text', placeholder: '请输入工序编码' },
  { key: 'name', label: '工序名称', type: 'text', placeholder: '请输入工序名称' },
  {
    key: 'routeId',
    label: '工艺路线',
    type: 'select',
    options: routeQuickFilterOptions.value
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])

const resetProcessQueryState = () => ({
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  routeId: undefined as number | undefined,
  status: undefined as number | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const clearProcessQueryFields = () => {
  queryParams.code = undefined
  queryParams.name = undefined
  queryParams.routeId = undefined
  queryParams.status = undefined
}

const applyProcessQuickFilterToQueryParams = () => {
  clearProcessQueryFields()
  const quickFilter = queryParams.quickFilter
  if (!quickFilter) return
  const fieldKey = quickFilter.fieldKey as ProcessQuickFilterFieldKey
  if (fieldKey === 'code') {
    queryParams.code = String(quickFilter.value ?? '').trim() || undefined
    return
  }
  if (fieldKey === 'name') {
    queryParams.name = String(quickFilter.value ?? '').trim() || undefined
    return
  }
  if (fieldKey === 'routeId') {
    const parsed = Number(quickFilter.value)
    if (!Number.isFinite(parsed)) {
      const errorMessage = '工艺路线必须是有效数字。'
      message.error(errorMessage)
      throw new Error(errorMessage)
    }
    queryParams.routeId = parsed
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

const buildProcessPageParams = () => {
  applyProcessQuickFilterToQueryParams()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    code: queryParams.code,
    name: queryParams.name,
    routeId: queryParams.routeId,
    status: queryParams.status
  }
}

const loadRouteOptions = async () => {
  routeOptions.value = await ProRouteApi.getRouteSimpleList()
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ProProcessApi.getProcessPage(buildProcessPageParams())
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const processQuickFilter = useTableQuickFilter(
  'mes.pro.process.main',
  processQuickFilterDefinitions,
  queryParams,
  getList
)

/** 添加/修改操作 */
const openForm = (type: string, id?: number, row?: ProProcessVO) => {
  formRef.value.open(type, id, {
    row,
    routeId: queryParams.routeId
  })
}

const syncQueryParamsFromRoute = () => {
  const code = typeof route.query.code === 'string' ? route.query.code : undefined
  const name = typeof route.query.name === 'string' ? route.query.name : undefined
  if (code) {
    Object.assign(queryParams, resetProcessQueryState(), {
      quickFilter: { fieldKey: 'code', operator: 'contains', value: code }
    })
    processQuickFilter.updateState({ fieldKey: 'code', operator: 'contains', value: code })
    processRouteQuickFilterApplied.value = true
    return
  }
  if (name) {
    Object.assign(queryParams, resetProcessQueryState(), {
      quickFilter: { fieldKey: 'name', operator: 'contains', value: name }
    })
    processQuickFilter.updateState({ fieldKey: 'name', operator: 'contains', value: name })
    processRouteQuickFilterApplied.value = true
    return
  }
  if (processRouteQuickFilterApplied.value || 'code' in route.query || 'name' in route.query) {
    Object.assign(queryParams, resetProcessQueryState())
    processQuickFilter.updateState({ fieldKey: 'code', operator: 'contains', value: undefined })
    processRouteQuickFilterApplied.value = false
  }
}

const buildProcessRouteQuerySignature = () =>
  JSON.stringify({
    code: typeof route.query.code === 'string' ? route.query.code : '',
    name: typeof route.query.name === 'string' ? route.query.name : '',
    openId: typeof route.query.openId === 'string' ? route.query.openId : ''
  })

const tryOpenDetailFromRoute = () => {
  const openId = typeof route.query.openId === 'string' ? route.query.openId : ''
  if (!openId) {
    openedProcessDetailId.value = ''
    return
  }
  if (openedProcessDetailId.value === openId) {
    return
  }
  openedProcessDetailId.value = openId
  openForm('detail', Number(openId))
}

const formatCapacity = (value?: number | string | null) => {
  return value === undefined || value === null || value === '' ? '' : value
}

const formatAvailabilityStatus = (status?: string) => {
  return status === 'REPAIR' ? '维修中/待验收' : '可用'
}

const openProcessWorkstation = async (workstation: ProProcessWorkstationVO, row: ProProcessVO) => {
  const workstationCode = workstation.code?.trim()
  if (!workstation.id || !workstationCode) {
    throw new Error(
      `工作站跳转缺少工作站编码: processId=${row.id}, workstationId=${workstation.id}`
    )
  }
  if (!row.id) {
    throw new Error(`工作站跳转缺少工序编号: workstationId=${workstation.id}`)
  }
  await router.push({
    path: '/mes/md/workstation',
    query: {
      code: workstationCode,
      processId: String(row.id)
    }
  })
}

const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await ProProcessApi.deleteProcess(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(async () => {
  await loadRouteOptions()
  lastAppliedProcessRouteQuerySignature.value = buildProcessRouteQuerySignature()
  syncQueryParamsFromRoute()
  await getList()
  tryOpenDetailFromRoute()
})

watch(
  () => route.fullPath,
  async () => {
    if (route.path !== PROCESS_ROUTE_PATH) {
      return
    }
    const nextSignature = buildProcessRouteQuerySignature()
    if (nextSignature === lastAppliedProcessRouteQuerySignature.value) {
      return
    }
    lastAppliedProcessRouteQuerySignature.value = nextSignature
    syncQueryParamsFromRoute()
    await getList()
    tryOpenDetailFromRoute()
  }
)


const loadColumnFilterRows0 = async () => {
  const params = buildProcessPageParams()
  const rows = await loadAllListPages<any>((pagination) =>
    ProProcessApi.getProcessPage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'productionQuantityFactor',
    label: '生产系数',
    type: 'text',
    getValue: (row) => [formatCapacity(row.productionQuantityFactor)].join(' ')
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: listColumnDictOptions(DICT_TYPE.COMMON_STATUS)
  }
])
</script>

<style scoped>
.process-list-page {
  height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding));
  margin-bottom: 0;
}

.process-list-page :deep(.el-card__body) {
  display: flex;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  flex-direction: column;
}

.process-list-page :deep(.unified-list-template),
.process-list-page :deep(.unified-list-template__table-shell) {
  flex: 1;
  min-height: 0;
}

.process-list-page :deep(.unified-list-template__table-shell) {
  overflow: hidden;
}

.process-list-page :deep(.el-form),
.process-list-page :deep(.el-pagination) {
  flex-shrink: 0;
}

.process-list-page :deep(.el-pagination) {
  margin-bottom: 0;
}

.process-main-table {
  width: 100%;
}

.process-main-table :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}

.process-main-table :deep(.el-scrollbar__bar.is-horizontal) {
  display: block;
  height: 8px;
  opacity: 1;
}

.process-main-table :deep(.el-scrollbar__bar.is-horizontal > div) {
  background-color: #9caec4;
}

.process-workstation-tags {
  display: inline-flex;
  max-width: 100%;
  gap: 4px;
  overflow: hidden;
  vertical-align: middle;
}

.process-workstation-tag {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.process-workstation-tag :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-workstation-link-tag {
  cursor: pointer;
}

.process-workstation-link-tag:hover,
.process-workstation-link-tag:focus-visible {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

.process-workstation-link-tag:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 2px;
}

.process-unconfigured {
  color: var(--el-text-color-placeholder);
}
</style>
