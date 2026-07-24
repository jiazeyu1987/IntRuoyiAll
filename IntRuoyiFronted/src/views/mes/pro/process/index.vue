<!-- MES 生产工序列表 -->
<template>
  <doc-alert title="【生产】工序设置、工艺流程" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <ContentWrap>
    <UnifiedListTemplate
      table-key="mes.pro.process.main"
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
    >
      <template #actions>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['mes:pro-process:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:pro-process:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="process-main-table"
          data-user-table-column-explicit
          data-user-table-key="mes.pro.process.main"
          :data="list"
          :height="processMainTableHeight"
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
            v-if="isProcessColumnVisible('routeList')"
            label="所属工艺路线"
            align="center"
            prop="routeList"
            :width="getProcessColumnWidthString('routeList', 240)"
            v-bind="sortColumnAttrs('routeList')"
          >
            <template #default="scope">
              <div class="process-route-cell">
                <div class="process-route-tags">
                  <el-tag
                    v-for="routeItem in resolveProcessRouteTags(scope.row.routeList)"
                    :key="routeItem.routeProcessId || routeItem.id"
                    size="small"
                    effect="plain"
                    class="process-route-tag"
                    :title="formatProcessRouteTag(routeItem)"
                  >
                    {{ formatProcessRouteTag(routeItem) }}
                  </el-tag>
                  <span
                    v-if="!resolveProcessRouteTags(scope.row.routeList).length"
                    class="process-unconfigured"
                  >
                    未配置
                  </span>
                </div>
                <el-button
                  v-if="scope.row.routeCapacityConflict"
                  link
                  type="warning"
                  class="process-route-capacity-link"
                  :title="
                    scope.row.routeCapacityConflictMessage ||
                    '多条工艺路线的排产产能不一致，请进入工艺流程使用覆盖产能处理。'
                  "
                  @click="openRouteCapacityOverride(scope.row)"
                >
                  产能不一致，去覆盖
                </el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isProcessColumnVisible('relationList')"
            label="关系清单"
            align="center"
            prop="relationList"
            :width="getProcessColumnWidthString('relationList', 320)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('relationList')"
          >
            <template #default="scope">
              <span
                class="process-relation-summary"
                :title="buildProcessRelationListSummary(scope.row)"
              >
                {{ buildProcessRelationListSummary(scope.row) }}
              </span>
            </template>
          </el-table-column>
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
            v-if="isProcessColumnVisible('batchRecordFormNames')"
            label="批记录表单"
            align="center"
            prop="batchRecordFormNames"
            :width="getProcessColumnWidthString('batchRecordFormNames', 260)"
            show-overflow-tooltip
            v-bind="sortColumnAttrs('batchRecordFormNames')"
          >
            <template #default="scope">
              <div v-if="scope.row.batchRecordForms?.length" class="process-link-tags">
                <el-button
                  v-for="form in scope.row.batchRecordForms"
                  :key="form.reportId"
                  link
                  type="primary"
                  class="process-link-tag"
                  @click="openBatchRecordForm(form)"
                >
                  {{ form.reportName }}
                </el-button>
              </div>
              <span v-else class="process-unconfigured">未配置</span>
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
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import download from '@/utils/download'
import {
  ProProcessApi,
  ProProcessBatchRecordFormLinkVO,
  ProProcessMachineryVO,
  ProProcessRouteVO,
  ProProcessVO,
  ProProcessWorkstationVO
} from '@/api/mes/pro/process'
import {
  ProRouteApi,
  type ProRouteVO,
  type RouteFlowBoundaryEdgeVO,
  type RouteFlowBoundaryType,
  type RouteFlowEdgeVO,
  type RouteFlowGraphVO,
  type RouteFlowNodeVO
} from '@/api/mes/pro/route'
import ProProcessForm from './ProProcessForm.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterOption,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  buildRouteCandidateEditQuery,
  ensureSameSourceDraftCandidateForProductionConfig
} from '../route/routeCandidateEntry'

defineOptions({ name: 'MesProProcess' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()

type ProcessQuickFilterFieldKey = 'code' | 'name' | 'routeId' | 'status'

const PROCESS_ROUTE_PATH = '/mes/pro/process'
const CAPACITY_OVERRIDE_CANDIDATE_CHANGE_REASON = '生产工序产能不一致入口创建候选版本'
const processMainTableHeight = 'max(360px, calc(100vh - 300px))'

const processDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '工序编码', width: 150 },
  { key: 'name', label: '工序名称', width: 200 },
  { key: 'routeList', label: '所属工艺路线', width: 240 },
  { key: 'relationList', label: '关系清单', width: 320 },
  { key: 'workstationNames', label: '工作站', width: 240, hideable: false },
  { key: 'productionQuantityFactor', label: '生产系数', width: 120, hideable: false },
  { key: 'batchRecordFormNames', label: '批记录表单', width: 260, hideable: false },
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
const exportLoading = ref(false) // 导出的加载中
const openedProcessDetailId = ref('')
const lastAppliedProcessRouteQuerySignature = ref('')
const processRouteQuickFilterApplied = ref(false)
const processMachineryDialogVisible = ref(false)
const processMachineryDialogTitle = ref('关联设备产能明细')
const processMachineryLoading = ref(false)
const processMachineryList = ref<ProProcessMachineryVO[]>([])
const routeOptions = ref<ProRouteVO[]>([])
const routeRelationGraphByRouteId = reactive<Record<number, RouteFlowGraphVO>>({})
const routeRelationGraphLoadingByRouteId = reactive<Record<number, boolean>>({})
const routeRelationGraphErrorByRouteId = reactive<Record<number, string>>({})
const routeQuickFilterOptions = computed<TableQuickFilterOption[]>(() =>
  routeOptions.value.map((item) => ({
    label: item.name,
    value: item.id!
  }))
)
const resolveProcessRouteTags = (routeList?: ProProcessVO['routeList']) => {
  return (
    routeList?.filter((item) => item && (item.name?.trim() || item.code?.trim() || item.id)) || []
  )
}

const formatProcessRouteTag = (routeItem: ProProcessRouteVO) => {
  const name = routeItem.name?.trim()
  const code = routeItem.code?.trim()
  if (name && code) return `${name}（${code}）`
  return name || code || String(routeItem.id)
}

const normalizeRelationId = (value?: number | string | null) => {
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : undefined
}

const boundaryRelationLabel = (boundaryType: RouteFlowBoundaryType) =>
  boundaryType === 'START' ? '工序开始' : '工序结束'

const formatRouteNodeLabel = (node?: RouteFlowNodeVO) => {
  if (!node) return '-'
  const code = node.processCode?.trim()
  const name = node.processName?.trim()
  if (code && name) return `${code} ${name}`
  return name || code || String(node.routeProcessId)
}

const buildRouteNodeMap = (nodes?: RouteFlowNodeVO[]) =>
  new Map((nodes || []).map((node) => [Number(node.routeProcessId), node]))

const isRelationEdgeMatched = (edge: RouteFlowEdgeVO, routeProcessId: number) =>
  Number(edge.sourceRouteProcessId) === routeProcessId ||
  Number(edge.targetRouteProcessId) === routeProcessId

const formatRouteRelationEdge = (
  edge: RouteFlowEdgeVO,
  nodeMap: Map<number, RouteFlowNodeVO>
) => {
  const source = formatRouteNodeLabel(nodeMap.get(Number(edge.sourceRouteProcessId)))
  const target = formatRouteNodeLabel(nodeMap.get(Number(edge.targetRouteProcessId)))
  return `${source} -> ${target}`
}

const isBoundaryEdgeMatched = (edge: RouteFlowBoundaryEdgeVO, routeProcessId: number) =>
  Number(edge.routeProcessId) === routeProcessId

const formatBoundaryRelationEdge = (
  edge: RouteFlowBoundaryEdgeVO,
  nodeMap: Map<number, RouteFlowNodeVO>
) => {
  const processLabel = formatRouteNodeLabel(nodeMap.get(Number(edge.routeProcessId)))
  return edge.boundaryType === 'START'
    ? `${boundaryRelationLabel('START')} -> ${processLabel}`
    : `${processLabel} -> ${boundaryRelationLabel('END')}`
}

const buildRouteProcessRelationSummaryFromGraph = (
  graph: RouteFlowGraphVO,
  routeProcessId: number
) => {
  const nodeMap = buildRouteNodeMap(graph.nodes)
  const boundaryRelations = (graph.boundaryEdges || [])
    .filter((edge) => isBoundaryEdgeMatched(edge, routeProcessId))
    .map((edge) => formatBoundaryRelationEdge(edge, nodeMap))
  const routeRelations = (graph.edges || [])
    .filter((edge) => isRelationEdgeMatched(edge, routeProcessId))
    .map((edge) => formatRouteRelationEdge(edge, nodeMap))
  const relationTexts = [...boundaryRelations, ...routeRelations].filter((text) => text.trim())
  return relationTexts.length > 0 ? relationTexts.join('；') : '暂无关系'
}

const collectRouteIdsForRelationList = (rows: ProProcessVO[]) => {
  const routeIds = new Set<number>()
  rows.forEach((row) => {
    resolveProcessRouteTags(row.routeList).forEach((routeItem) => {
      const routeId = normalizeRelationId(routeItem.id)
      const routeProcessId = normalizeRelationId(routeItem.routeProcessId)
      if (routeId && routeProcessId) {
        routeIds.add(routeId)
      }
    })
  })
  return [...routeIds]
}

const loadRelationGraphByRouteId = async (routeId: number) => {
  if (routeRelationGraphByRouteId[routeId] || routeRelationGraphLoadingByRouteId[routeId]) return
  routeRelationGraphLoadingByRouteId[routeId] = true
  delete routeRelationGraphErrorByRouteId[routeId]
  try {
    routeRelationGraphByRouteId[routeId] = await ProRouteApi.getRouteProcessFlowGraph(routeId)
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '关系清单加载失败'
    routeRelationGraphErrorByRouteId[routeId] = errorMessage
    message.error(`关系清单加载失败：${errorMessage}`)
  } finally {
    routeRelationGraphLoadingByRouteId[routeId] = false
  }
}

const loadRelationGraphsForVisibleProcesses = async (rows: ProProcessVO[] = list.value) => {
  if (!isProcessColumnVisible('relationList')) return
  const routeIds = collectRouteIdsForRelationList(rows)
  await Promise.all(routeIds.map((routeId) => loadRelationGraphByRouteId(routeId)))
}

const buildProcessRelationListSummary = (row: ProProcessVO) => {
  const routeItems = resolveProcessRouteTags(row.routeList).filter(
    (routeItem) => normalizeRelationId(routeItem.id) && normalizeRelationId(routeItem.routeProcessId)
  )
  if (!routeItems.length) return '暂无关系'

  let hasLoadingRelation = false
  const summaries = routeItems
    .map((routeItem) => {
      const routeId = normalizeRelationId(routeItem.id)!
      const routeProcessId = normalizeRelationId(routeItem.routeProcessId)!
      const routeLabel = formatProcessRouteTag(routeItem)
      if (routeRelationGraphLoadingByRouteId[routeId]) {
        hasLoadingRelation = true
        return ''
      }
      if (routeRelationGraphErrorByRouteId[routeId]) {
        return `${routeLabel}：关系加载失败`
      }
      const graph = routeRelationGraphByRouteId[routeId]
      if (!graph) {
        hasLoadingRelation = true
        return ''
      }
      const relationSummary = buildRouteProcessRelationSummaryFromGraph(graph, routeProcessId)
      return routeItems.length > 1 ? `${routeLabel}：${relationSummary}` : relationSummary
    })
    .filter((summary) => summary.trim())

  if (summaries.length > 0) return summaries.join('；')
  return hasLoadingRelation ? '加载中' : '暂无关系'
}

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
    await loadRelationGraphsForVisibleProcesses(data.list)
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

/** 重置按钮操作 */
const resetQuery = () => {
  Object.assign(queryParams, resetProcessQueryState())
  processQuickFilter.resetQuickFilter()
}

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

const openBatchRecordForm = (form: ProProcessBatchRecordFormLinkVO) => {
  router.push({
    path: '/mes/pro/batch-record-form-list',
    query: { reportId: form.reportId }
  })
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

const openRouteCapacityOverride = async (row: ProProcessVO) => {
  try {
    const routeItem = resolveProcessRouteTags(row.routeList).find(
      (item) => item.id && item.routeProcessId
    )
    if (!routeItem?.id || !routeItem.routeProcessId) {
      throw new Error(`产能覆盖跳转缺少路线或路线工序标识: processId=${row.id}`)
    }
    const routeLabel = formatProcessRouteTag(routeItem)
    const candidateResult = await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: routeItem.id,
      actionName: '产能覆盖跳转',
      changeReason: CAPACITY_OVERRIDE_CANDIDATE_CHANGE_REASON,
      confirm: (content, title) => message.confirm(content, title),
      success: (content) => message.success(content),
      existingConfirmMessage: `产能覆盖需要在路线「${routeLabel}」候选版本中编辑。确认后会进入已有候选版本，不会直接影响当前生产版本。是否继续？`,
      existingConfirmTitle: '进入候选版本',
      createConfirmMessage: `产能覆盖需要先为路线「${routeLabel}」创建候选版本。确认后会创建候选版本并进入编辑，不会直接影响当前生产版本。是否继续？`,
      createConfirmTitle: '创建候选版本',
      existingSuccessMessage: '正在进入候选版本产能覆盖编辑',
      createdSuccessMessage: '候选版本已创建，正在进入产能覆盖编辑'
    })
    if (!candidateResult) return
    await router.push({
      name: 'MesProRouteEdit',
      params: { id: routeItem.id },
      query: buildRouteCandidateEditQuery(candidateResult.candidate, {
        tab: 'flow',
        routeProcessId: String(routeItem.routeProcessId),
        capacitySourceFocus: 'schedule',
        source: 'process-list-route-capacity-conflict',
        capacityOverride: '1'
      })
    })
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '产能覆盖跳转失败，请查看后端返回错误。'
    message.error(errorMessage)
    throw error
  }
}

const openMachineryDialog = async (row: ProProcessVO) => {
  processMachineryDialogVisible.value = true
  processMachineryDialogTitle.value = `${row.code} 关联设备产能明细`
  processMachineryLoading.value = true
  try {
    processMachineryList.value = await ProProcessApi.getProcessMachineryList(row.id!)
  } finally {
    processMachineryLoading.value = false
  }
}

/** 删除按钮操作 */
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

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await ProProcessApi.exportProcess(buildProcessPageParams())
    download.excel(data, '生产工序.xls')
  } catch {
  } finally {
    exportLoading.value = false
  }
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

watch(
  () => isProcessColumnVisible('relationList'),
  async (visible) => {
    if (visible) {
      await loadRelationGraphsForVisibleProcesses()
    }
  }
)
</script>

<style scoped>
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

.process-route-cell {
  display: flex;
  max-width: 100%;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.process-route-tags {
  display: flex;
  max-width: 100%;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 4px;
  vertical-align: middle;
}

.process-route-tag {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.process-route-tag :deep(.el-tag__content) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-relation-summary {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.process-route-capacity-link {
  min-height: 20px;
  padding: 0;
  font-size: 12px;
  line-height: 20px;
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

.process-link-tags {
  display: inline-flex;
  max-width: 100%;
  gap: 4px;
  overflow: hidden;
  vertical-align: middle;
}

.process-link-tag {
  max-width: 130px;
  padding: 0;
}

.process-unconfigured {
  color: var(--el-text-color-placeholder);
}
</style>
