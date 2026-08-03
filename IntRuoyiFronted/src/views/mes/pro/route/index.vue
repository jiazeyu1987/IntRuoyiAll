<!-- MES 工艺路线列表 -->
<template>
  <doc-alert title="【生产】工序设置、工艺流程" url="https://doc.iocoder.cn/mes/pro/process-route/" />

  <ContentWrap>
    <UnifiedListTemplate
      :table-key="ROUTE_LIST_TABLE_KEY"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="routeQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="routeQuickFilter.state"
      :selected-filter-definition="routeQuickFilter.selectedDefinition.value"
      :operator-options="routeQuickFilter.operatorOptions.value"
      :columns="routeColumns"
      :column-saving="routeColumnSaving"
      :show-column-settings="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="routeQuickFilter.updateState"
      @quick-filter-query="routeQuickFilter.applyQuickFilter"
      @column-change="saveRouteColumnConfig"
      @pagination="getList"
    >
      <template #extra-filters>
        <el-form-item class="route-list__create-action">
          <el-button
            type="primary"
            plain
            @click="openForm('create')"
            v-hasPermi="['mes:pro-route:create']"
          >
            <Icon icon="ep:plus" class="mr-5px" /> 新增
          </el-button>
        </el-form-item>
        <el-form-item class="route-list__column-settings">
          <UserTableColumnSettings
            :columns="routeColumns"
            :saving="routeColumnSaving"
            :show-reset="false"
            @change="saveRouteColumnConfig"
          />
        </el-form-item>
      </template>
      <template #actions>
        <el-button
          type="primary"
          plain
          @click="handleRouteWorkbookExcelImport"
          v-hasPermi="['mes:pro-route:create']"
        >
          <Icon icon="ep:document-add" class="mr-5px" /> 导入
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['mes:pro-route:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> 导出
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          :data-user-table-key="ROUTE_LIST_TABLE_KEY"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleRouteHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRouteColumnVisible('code')"
            label="路线编码"
            align="center"
            prop="code"
            :min-width="getRouteColumnMinWidthString('code', 180)"
            v-bind="sortColumnAttrs('code')"
          >
            <template #default="scope">
              <el-button link type="primary" @click="openForm('detail', scope.row.id)">
                {{ scope.row.code }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('name')"
            label="路线名称"
            align="center"
            prop="name"
            :min-width="getRouteColumnMinWidthString('name', 200)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isRouteColumnVisible('ownerName')"
            label="负责人"
            align="center"
            prop="ownerName"
            :min-width="getRouteColumnMinWidthString('ownerName', 140)"
            v-bind="sortColumnAttrs('ownerName')"
          />
          <el-table-column
            v-if="isRouteColumnVisible('keyProcessName')"
            label="关键工序"
            align="center"
            prop="keyProcessName"
            :min-width="getRouteColumnMinWidthString('keyProcessName', 180)"
            v-bind="sortColumnAttrs('keyProcessName')"
          />
          <el-table-column
            v-if="isRouteColumnVisible('status')"
            label="状态"
            align="center"
            prop="status"
            :width="getRouteColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="scope">
              <el-switch
                v-model="scope.row.status"
                :active-value="0"
                :inactive-value="1"
                @change="handleStatusChange(scope.row)"
                :disabled="!checkPermi(['mes:pro-route:update'])"
              />
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('flowGraphConfigured')"
            label="关系图"
            align="center"
            prop="flowGraphConfigured"
            :width="getRouteColumnWidthString('flowGraphConfigured', 100)"
            v-bind="sortColumnAttrs('flowGraphConfigured')"
          >
            <template #default="scope">
              <el-tag :type="scope.row.flowGraphConfigured ? 'success' : 'info'">
                {{ scope.row.flowGraphConfigured ? '已设' : '未设' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('activeRouteVersionNo')"
            label="当前生效版本"
            align="center"
            prop="activeRouteVersionNo"
            :min-width="getRouteColumnMinWidthString('activeRouteVersionNo', 140)"
            v-bind="sortColumnAttrs('activeRouteVersionNo')"
          >
            <template #default="scope">
              <el-link
                v-if="scope.row.activeRouteVersionNo"
                class="route-list__version-link"
                type="primary"
                :underline="false"
                @click="openRouteVersionFromList(scope.row, 'active')"
              >
                {{ scope.row.activeRouteVersionNo }}
              </el-link>
              <span v-else class="route-list__muted">未生成版本</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('pendingRouteVersionNo')"
            label="待发布版本"
            align="center"
            prop="pendingRouteVersionNo"
            :min-width="getRouteColumnMinWidthString('pendingRouteVersionNo', 160)"
            v-bind="sortColumnAttrs('pendingRouteVersionNo')"
          >
            <template #default="scope">
              <el-link
                v-if="scope.row.pendingRouteVersionNo"
                class="route-list__version-tag-link"
                :underline="false"
                @click="openRouteVersionFromList(scope.row, 'pending')"
              >
                <el-tag
                  class="route-list__pending-version-tag"
                  :type="resolveRouteVersionStatusTagType(scope.row.pendingRouteVersionStatus)"
                  effect="plain"
                >
                  {{ formatPendingRouteVersion(scope.row) }}
                </el-tag>
              </el-link>
              <span v-else class="route-list__muted">无</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRouteColumnVisible('productCodes')"
            label="关联产品"
            align="center"
            prop="productCodes"
            :min-width="getRouteColumnMinWidthString('productCodes', 220)"
            v-bind="sortColumnAttrs('productCodes')"
          />
          <el-table-column
            v-if="isRouteColumnVisible('createTime')"
            label="创建时间"
            align="center"
            prop="createTime"
            :formatter="dateFormatter"
            :width="getRouteColumnWidthString('createTime', 180)"
            v-bind="sortColumnAttrs('createTime')"
          />
          <el-table-column
            label="操作"
            align="center"
            prop="actions"
            :width="getRouteColumnWidthString('actions', 220)"
            fixed="right"
          >
            <template #default="scope">
              <el-button
                link
                type="primary"
                :loading="routeProductBindLoadingId === scope.row.id"
                @click="handleBindRouteProducts(scope.row)"
                v-hasPermi="['mes:pro-route:update']"
              >
                产品
              </el-button>
              <el-button
                link
                type="primary"
                :loading="routeCandidateEditLoadingId === scope.row.id"
                @click="handleEditRouteProductionConfig(scope.row)"
                v-hasPermi="['mes:pro-route:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="primary"
                @click="openCopyRouteDialog(scope.row)"
                v-hasPermi="['mes:pro-route:create']"
              >
                复制
              </el-button>
              <el-button
                link
                type="primary"
                data-testid="route-version-workspace"
                @click="openRouteVersionWorkspace(scope.row)"
                v-hasPermi="['mes:pro-route:version-query']"
              >
                版本
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
                v-hasPermi="['mes:pro-route:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改/启用/详情 -->
  <RouteForm
    ref="formRef"
    @success="getList"
    @request-upgrade="handleDuplicateRouteVersionUpgrade"
  />
  <!-- 多 Sheet 路线 Excel 导入对话框 -->
  <RouteWorkbookExcelImportForm ref="routeWorkbookExcelImportFormRef" @success="getList" />
  <Dialog v-model="copyDialogVisible" title="复制工艺路线" width="520px">
    <el-form label-width="96px">
      <el-form-item label="源路线">
        <span>{{ copySourceRoute?.code || '-' }} {{ copySourceRoute?.name || '' }}</span>
      </el-form-item>
      <el-form-item label="副本编码" required>
        <el-input v-model="copyForm.targetCode" clearable placeholder="请输入副本路线编码" />
      </el-form-item>
      <el-form-item label="副本名称" required>
        <el-input v-model="copyForm.targetName" clearable placeholder="请输入副本路线名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="copyDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="copySaving" @click="submitCopyRoute">
        确认复制
      </el-button>
    </template>
  </Dialog>
  <Dialog v-model="routeVersionDialogVisible" title="工艺路线版本" width="920px">
    <div v-loading="routeVersionLoading" class="route-version-workspace__body">
      <div class="route-version-workspace__summary">
        <div>
          <div class="route-version-workspace__title">
            {{ routeVersionRoute?.code || '-' }} {{ routeVersionRoute?.name || '' }}
          </div>
          <div class="route-version-workspace__active-version">
            当前 ACTIVE：
            <el-tag type="success">{{ routeVersionRoute?.activeRouteVersionNo || '未生成版本' }}</el-tag>
            <span v-if="routeVersionRoute?.activeRouteVersionId" class="route-version-workspace__muted">
              #{{ routeVersionRoute.activeRouteVersionId }}
            </span>
          </div>
        </div>
        <el-button
          type="primary"
          plain
          :loading="routeVersionActionLoading"
          @click="createRouteCandidateFromActive"
          v-hasPermi="['mes:pro-route:version-create']"
        >
          创建候选版本
        </el-button>
      </div>
      <ControlledContentStateStrip
        v-if="routeVersionRoute"
        class="route-version-workspace__state-strip"
        test-id="mes-route-version-workspace-state-strip"
        title="候选版本工作区"
        :version-no="routeVersionRoute.activeRouteVersionNo || '未生成版本'"
        :status-label="routeVersionWorkspaceStatusLabel"
        :status-type="routeVersionWorkspaceStatusTagType"
        mode-label="MES 工艺路线"
        :candidate-count="routeVersionOpenCandidateCount"
        :hint="routeVersionWorkspaceHint"
        :blockers="routeVersionWorkspaceBlockers"
        :readonly="routeVersionOpenCandidateCount > 0 && !routeVersionEditableDraft"
        :editable="routeVersionEditableDraft"
      />
      <el-alert
        v-if="routeVersionNoticeMessage"
        class="route-version-workspace__notice"
        type="warning"
        :closable="false"
        :title="routeVersionNoticeMessage"
      />
      <el-alert
        v-if="routeVersionErrorMessage"
        class="route-version-workspace__error"
        type="error"
        :closable="false"
        :title="routeVersionErrorMessage"
      />
      <el-table
        class="route-version-workspace__candidate-list"
        :data="visibleRouteVersions"
        border
        :show-overflow-tooltip="true"
        empty-text="暂无版本记录"
      >
        <el-table-column label="版本" prop="versionNo" min-width="110" />
        <el-table-column label="状态" prop="lifecycleStatus" min-width="150">
          <template #default="{ row: version }">
            <el-tag :type="resolveRouteVersionStatusTagType(version.lifecycleStatus)">
              {{ resolveRouteVersionStatusLabel(version.lifecycleStatus) }}
            </el-tag>
            <el-tag v-if="version.active" class="ml-6px" type="success">ACTIVE</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源版本" prop="sourceRouteVersionId" min-width="110">
          <template #default="{ row: version }">
            {{ version.sourceRouteVersionId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="publishedTime" min-width="170">
          <template #default="{ row: version }">
            {{ formatDateTimeValue(version.publishedTime, '-') }}
          </template>
        </el-table-column>
        <el-table-column label="发布阻断项" min-width="260">
          <template #default="{ row: version }">
            <div v-if="routeVersionBlockersById[version.id]" class="route-version-workspace__blockers">
              <el-tag v-if="routeVersionBlockersById[version.id].publishable" type="success">
                可发布
              </el-tag>
              <template v-else>
                <el-tag
                  v-for="blocker in routeVersionBlockersById[version.id].blockers"
                  :key="blocker"
                  type="danger"
                  class="route-version-workspace__blocker-tag"
                >
                  {{ blocker }}
                </el-tag>
              </template>
            </div>
            <span v-else class="route-version-workspace__muted">未查询</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row: version }">
            <el-button
              v-if="canViewRouteVersion(version)"
              link
              type="primary"
              @click="openRouteVersionViewer(version)"
            >
              查看
            </el-button>
            <el-button
              v-if="canEditRouteCandidateVersion(version)"
              link
              type="primary"
              @click="openRouteCandidateVersionEditor(version)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              :loading="routeVersionBlockerLoadingId === version.id"
              @click="loadRouteVersionBlockers(version.id)"
            >
              查阻断项
            </el-button>
            <el-button
              v-if="canSubmitRouteVersion(version)"
              link
              type="primary"
              :loading="routeVersionActionLoadingId === version.id"
              @click="submitRouteCandidateVersion(version)"
              v-hasPermi="['mes:pro-route:version-submit']"
            >
              提交发布
            </el-button>
            <el-button
              v-if="canWithdrawRouteVersion(version)"
              link
              type="warning"
              :loading="routeVersionActionLoadingId === version.id"
              @click="withdrawRouteCandidateVersion(version.id)"
              v-hasPermi="['mes:pro-route:version-withdraw']"
            >
              撤回
            </el-button>
            <el-button
              v-if="canReopenRouteVersion(version)"
              link
              type="primary"
              :loading="routeVersionActionLoadingId === version.id"
              @click="reopenRouteCandidateVersion(version.id)"
              v-hasPermi="['mes:pro-route:version-reopen']"
            >
              按意见修改
            </el-button>
            <el-button
              v-if="canDeleteRouteDraftVersion(version)"
              link
              type="danger"
              :loading="routeVersionActionLoadingId === version.id"
              @click="deleteRouteDraftVersion(version)"
              v-hasPermi="['mes:pro-route:version-cancel']"
            >
              删除草稿
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDateTimeValue } from '@/utils/formatTime'
import { CommonStatusEnum } from '@/utils/constants'
import { checkPermi } from '@/utils/permission'
import download from '@/utils/download'
import {
  ProRouteApi,
  type ProRouteVO,
  type ProRouteVersionBlockerVO,
  type ProRouteVersionLifecycleStatus,
  type ProRouteVersionVO
} from '@/api/mes/pro/route'
import { ProRouteProductApi, type ProRouteProductBindFromWorkOrdersRespVO } from '@/api/mes/pro/route/product'
import ControlledContentStateStrip from '@/components/ControlledContent/ControlledContentStateStrip.vue'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import {
  buildRouteCandidateEditQuery,
  ensureSameSourceDraftCandidateForProductionConfig,
  isRouteCandidateConfirmCancel,
  isRouteMultipleDraftCandidateError
} from './routeCandidateEntry'

defineOptions({ name: 'MesProRoute' })

const RouteForm = defineAsyncComponent(() => import('./RouteForm.vue'))
const RouteWorkbookExcelImportForm = defineAsyncComponent(() => import('./RouteWorkbookExcelImportForm.vue'))

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()

const loading = ref(true) // 列表的加载中
const list = ref<ProRouteVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const exportLoading = ref(false) // 导出的加载中
const openedRouteDetailId = ref('')
const copyDialogVisible = ref(false)
const copySaving = ref(false)
const copySourceRoute = ref<ProRouteVO>()
const routeVersionDialogVisible = ref(false)
const routeVersionLoading = ref(false)
const routeVersionActionLoading = ref(false)
const routeVersionActionLoadingId = ref<number>()
const routeVersionBlockerLoadingId = ref<number>()
const routeVersionRoute = ref<ProRouteVO>()
const routeVersions = ref<ProRouteVersionVO[]>([])
const routeVersionBlockersById = reactive<Record<number, ProRouteVersionBlockerVO>>({})
const routeVersionNoticeMessage = ref('')
const routeVersionErrorMessage = ref('')
const routeProductBindLoadingId = ref<number | undefined>()
const routeCandidateEditLoadingId = ref<number | undefined>()
const OPEN_CANDIDATE_CONFLICT_NOTICE =
  '当前路线存在多个打开中的候选版本，请通过待发布版本或编辑入口处理打开候选；版本列表仅展示草稿及已生效历史版本。'
const ROUTE_OPEN_CANDIDATE_STATUS_SET = new Set([
  'DRAFT',
  'PENDING_APPROVAL',
  'READY_TO_PUBLISH',
  'REJECTED'
])
const ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET = new Set(['DRAFT', 'ACTIVE', 'SUPERSEDED'])
const isVisibleRouteVersionInWorkspace = (version: ProRouteVersionVO) =>
  version.active || ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET.has(String(version.lifecycleStatus))
const visibleRouteVersions = computed(() =>
  routeVersions.value.filter(isVisibleRouteVersionInWorkspace)
)
const routeVersionOpenCandidates = computed(() =>
  routeVersions.value.filter(
    (version) => !version.active && ROUTE_OPEN_CANDIDATE_STATUS_SET.has(String(version.lifecycleStatus))
  )
)
const routeVersionOpenCandidateCount = computed(() => {
  if (routeVersionOpenCandidates.value.length > 0) {
    return routeVersionOpenCandidates.value.length
  }
  return routeVersionRoute.value?.pendingRouteVersionCount || 0
})
const routeVersionEditableDraft = computed(() =>
  routeVersionOpenCandidates.value.some((version) => version.lifecycleStatus === 'DRAFT')
)
const routeVersionPrimaryOpenCandidate = computed(() => routeVersionOpenCandidates.value[0])
const routeVersionWorkspaceStatusLabel = computed(() => {
  if (routeVersionOpenCandidateCount.value > 1) {
    return '需关闭冲突候选'
  }
  const candidate = routeVersionPrimaryOpenCandidate.value
  if (candidate?.lifecycleStatus) {
    return resolveRouteVersionStatusLabel(candidate.lifecycleStatus)
  }
  return routeVersionRoute.value?.activeRouteVersionId ? '无打开候选' : '未生成版本'
})
const routeVersionWorkspaceStatusTagType = computed(() => {
  if (routeVersionOpenCandidateCount.value > 1) {
    return 'danger'
  }
  const candidate = routeVersionPrimaryOpenCandidate.value
  if (candidate?.lifecycleStatus) {
    return resolveRouteVersionStatusTagType(candidate.lifecycleStatus)
  }
  return routeVersionRoute.value?.activeRouteVersionId ? 'success' : 'warning'
})
const routeVersionWorkspaceHint = computed(() => {
  if (routeVersionOpenCandidateCount.value > 1) {
    return OPEN_CANDIDATE_CONFLICT_NOTICE
  }
  const status = String(routeVersionPrimaryOpenCandidate.value?.lifecycleStatus || '')
  if (status === 'DRAFT') {
    return '仅草稿候选可编辑；请通过待发布版本或编辑入口打开，提交后进入审核。'
  }
  if (status === 'PENDING_APPROVAL') {
    return '候选版本正在审核中，仅允许查看；需要修改请通过待发布版本入口撤回后再编辑。'
  }
  if (status === 'READY_TO_PUBLISH') {
    return '候选版本已通过审核，系统正在发布生效；该状态不需要人工签名发布。'
  }
  if (status === 'REJECTED') {
    return '候选版本已驳回，按意见修改会回到草稿后再编辑。'
  }
  if (!routeVersionRoute.value?.activeRouteVersionId) {
    return '当前路线暂无生效版本，请先完成路线配置并创建首版。'
  }
  return '当前只有一个生效版本；修改生产配置请先创建候选版本。'
})
const routeVersionWorkspaceBlockers = computed(() => {
  if (routeVersionOpenCandidateCount.value > 1) {
    return [`打开中的候选版本 ${routeVersionOpenCandidateCount.value} 个`]
  }
  return []
})
const ROUTE_LIST_TABLE_KEY = 'mes.pro.route.main.admin-layout-v1'
const routeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '路线编码', minWidth: 180 },
  { key: 'name', label: '路线名称', minWidth: 200 },
  { key: 'ownerName', label: '负责人', visible: false, minWidth: 140 },
  { key: 'keyProcessName', label: '关键工序', visible: false, minWidth: 180 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'flowGraphConfigured', label: '关系图', visible: false, width: 100 },
  { key: 'activeRouteVersionNo', label: '当前生效版本', minWidth: 140 },
  { key: 'pendingRouteVersionNo', label: '待发布版本', minWidth: 160 },
  { key: 'productCodes', label: '关联产品', minWidth: 220 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false }
]
const {
  saving: routeColumnSaving,
  columns: routeColumns,
  isColumnVisible: isRouteColumnVisible,
  getColumnWidthString: getRouteColumnWidthString,
  getColumnMinWidthString: getRouteColumnMinWidthString,
  handleHeaderDragend: handleRouteHeaderDragend,
  saveConfig: saveRouteColumnConfig
} = useUserTableColumns(ROUTE_LIST_TABLE_KEY, routeDefaultColumns)

const resetRouteQueryState = (pageSize = 10) => ({
  pageNo: 1,
  pageSize,
  code: undefined as string | undefined,
  name: undefined as string | undefined,
  status: undefined as number | undefined
})
const queryParams = reactive(resetRouteQueryState())
const copyForm = reactive({
  targetCode: '',
  targetName: ''
})
const formRef = ref() // 表单弹窗
const routeWorkbookExcelImportFormRef = ref()

const routeQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'code',
    label: '路线编码',
    type: 'text',
    queryParamKey: 'code',
    placeholder: '请输入路线编码'
  },
  {
    key: 'name',
    label: '路线名称',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '请输入路线名称'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: getIntDictOptions(DICT_TYPE.COMMON_STATUS)
  }
]

const loadListFromRoute = async () => {
  queryParams.code = typeof route.query.code === 'string' ? route.query.code : undefined
  queryParams.name = typeof route.query.name === 'string' ? route.query.name : undefined
  queryParams.pageNo = 1
  await getList()
  const openId = typeof route.query.openId === 'string' ? route.query.openId : ''
  if (!openId) {
    openedRouteDetailId.value = ''
    return
  }
  if (openedRouteDetailId.value === openId) {
    return
  }
  openedRouteDetailId.value = openId
  openForm('detail', Number(openId))
}

/** 查询列表 */
async function getList() {
  loading.value = true
  try {
    const data = await ProRouteApi.getRoutePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const routeQuickFilter = useTableQuickFilter(
  'mes.pro.route.main',
  routeQuickFilterDefinitions,
  queryParams,
  getList
)

/** 状态开关操作 */
const handleStatusChange = async (row: ProRouteVO) => {
  try {
    const text = row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'
    await message.confirm('确认要“' + text + '”“' + row.name + '”工艺路线吗?')
    await ProRouteApi.updateRouteStatus(row.id!, row.status!)
    await getList()
  } catch (error) {
    row.status =
      row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
    if (!isUserCancel(error)) {
      message.error(resolveRouteVersionErrorMessage(error, '更新工艺路线状态失败，请查看后端返回错误'))
    }
  }
}

/** 添加/修改操作 */
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

type RouteEditTab = 'basic' | 'flow' | 'product'

const openEditPage = (id?: number, tab?: RouteEditTab) => {
  if (!id) {
    throw new Error('打开编辑工艺路线失败：缺少路线编号')
  }
  const targetTab = tab ?? 'flow'
  router.push({
    name: 'MesProRouteEdit',
    params: { id },
    query: { tab: targetTab }
  })
}

const openRouteVersionFromList = async (row: ProRouteVO, target: 'active' | 'pending') => {
  try {
    if (!row.id) {
      throw new Error('跳转路线版本失败：缺少路线编号')
    }
    if (target === 'active') {
      if (!row.activeRouteVersionId || !row.activeRouteVersionNo) {
        throw new Error('跳转生效版本失败：缺少生效版本信息')
      }
      await router.push({
        name: 'MesProRouteEdit',
        params: { id: row.id },
        query: { tab: 'flow' }
      })
      return
    }
    if (target === 'pending') {
      if (!row.pendingRouteVersionId || !row.pendingRouteVersionNo || !row.pendingRouteVersionStatus) {
        throw new Error('跳转待发布版本失败：缺少候选版本信息')
      }
      await router.push({
        name: 'MesProRouteEdit',
        params: { id: row.id },
        query: {
          tab: 'flow',
          routeVersionId: String(row.pendingRouteVersionId),
          routeVersionNo: row.pendingRouteVersionNo,
          routeVersionStatus: row.pendingRouteVersionStatus
        }
      })
    }
  } catch (error) {
    message.error(resolveRouteVersionErrorMessage(error, '跳转路线版本失败，请查看后端返回错误'))
  }
}

const handleEditRouteProductionConfig = async (row: ProRouteVO) => {
  if (!row.id) {
    throw new Error('进入候选版本编辑失败：缺少路线编号')
  }
  if (!row.activeRouteVersionId) {
    throw new Error('进入候选版本编辑失败：当前路线缺少生效版本，无法创建候选版本')
  }
  routeCandidateEditLoadingId.value = row.id
  try {
    const candidateResult = await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: row.id,
      actionName: '进入候选版本编辑',
      changeReason: '列表编辑创建候选版本',
      existingSuccessMessage: '已进入当前草稿候选版本',
      createdSuccessMessage: '候选版本已创建，正在进入编辑'
    })
    if (!candidateResult) return
    const draftExitQuery = candidateResult.created
      ? {
          routeDraftOrigin: 'list-edit',
          discardOnUnsavedExit: '1'
        }
      : {}
    await router.push({
      name: 'MesProRouteEdit',
      params: { id: row.id },
      query: buildRouteCandidateEditQuery(candidateResult.candidate, {
        tab: 'flow',
        ...draftExitQuery
      })
    })
  } catch (error) {
    if (isRouteMultipleDraftCandidateError(error)) {
      await openRouteVersionWorkspace(row, OPEN_CANDIDATE_CONFLICT_NOTICE)
      return
    }
    if (isRouteCandidateConfirmCancel(error)) return
    message.error(resolveRouteVersionErrorMessage(error, '进入候选版本编辑失败，请查看后端返回错误'))
  } finally {
    routeCandidateEditLoadingId.value = undefined
  }
}

const openCopyRouteDialog = (row: ProRouteVO) => {
  if (!row.id) {
    throw new Error('复制工艺路线失败：缺少源路线编号')
  }
  copySourceRoute.value = row
  copyForm.targetCode = `${row.code}-COPY`
  copyForm.targetName = `${row.name}-副本`
  copyDialogVisible.value = true
}

const submitCopyRoute = async () => {
  if (!copySourceRoute.value?.id) {
    throw new Error('复制工艺路线失败：缺少源路线编号')
  }
  if (!copyForm.targetCode.trim() || !copyForm.targetName.trim()) {
    message.warning('请填写副本编码和副本名称')
    return
  }
  copySaving.value = true
  try {
    await ProRouteApi.copyRoute(
      {
        sourceRouteId: copySourceRoute.value.id,
        targetCode: copyForm.targetCode.trim(),
        targetName: copyForm.targetName.trim()
      },
      { ignoreErrorMessage: true }
    )
    message.success('工艺路线已复制')
    copyDialogVisible.value = false
    await getList()
  } catch (error) {
    if (isDuplicateRouteNameError(error)) {
      if (await confirmDuplicateRouteVersionUpgrade(copyForm.targetName)) {
        copyDialogVisible.value = false
        await openExistingRouteForVersionUpgrade(copyForm.targetName)
      }
      return
    }
    throw error
  } finally {
    copySaving.value = false
  }
}

const openRouteVersionWorkspace = async (row: ProRouteVO, noticeMessage = '') => {
  if (!row.id) {
    throw new Error('打开工艺路线版本工作区失败：缺少路线编号')
  }
  routeVersionRoute.value = row
  routeVersionNoticeMessage.value = noticeMessage
  routeVersionDialogVisible.value = true
  await loadRouteVersions(row.id)
}

const loadRouteVersions = async (routeId = routeVersionRoute.value?.id) => {
  if (!routeId) {
    throw new Error('加载工艺路线版本失败：缺少路线编号')
  }
  routeVersionLoading.value = true
  routeVersionErrorMessage.value = ''
  try {
    routeVersions.value = await ProRouteApi.getRouteVersionList(routeId)
  } catch (error) {
    routeVersionErrorMessage.value = resolveRouteVersionErrorMessage(
      error,
      '加载工艺路线版本失败，请查看后端返回错误'
    )
    message.error(routeVersionErrorMessage.value)
  } finally {
    routeVersionLoading.value = false
  }
}

const createRouteCandidateFromActive = async () => {
  const currentRoute = routeVersionRoute.value
  if (!currentRoute?.id) {
    throw new Error('创建候选版本失败：缺少路线编号')
  }
  routeVersionActionLoading.value = true
  routeVersionNoticeMessage.value = ''
  routeVersionErrorMessage.value = ''
  try {
    await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: currentRoute.id,
      actionName: '创建候选版本',
      changeReason: '前端版本工作区创建候选版本',
      success: (content) => message.success(content),
      existingSuccessMessage: '已存在草稿候选版本，请从待发布版本或编辑入口继续编辑',
      createdSuccessMessage: '候选版本已创建，请从待发布版本或编辑入口继续编辑'
    })
    await loadRouteVersions(currentRoute.id)
  } catch (error) {
    if (isRouteCandidateConfirmCancel(error)) return
    routeVersionErrorMessage.value = resolveRouteVersionErrorMessage(
      error,
      '创建候选版本失败，请查看后端返回错误'
    )
    message.error(routeVersionErrorMessage.value)
  } finally {
    routeVersionActionLoading.value = false
  }
}

const openRouteVersionViewer = async (version: ProRouteVersionVO) => {
  const currentRoute = routeVersionRoute.value
  if (!currentRoute?.id) {
    throw new Error('查看路线版本失败：缺少路线编号')
  }
  await router.push({
    name: 'MesProRouteEdit',
    params: { id: currentRoute.id },
    query: version.active
      ? { tab: 'flow' }
      : {
          tab: 'flow',
          routeVersionId: String(version.id),
          routeVersionNo: version.versionNo,
          routeVersionStatus: version.lifecycleStatus
        }
  })
  routeVersionDialogVisible.value = false
}

const canViewRouteVersion = (version: ProRouteVersionVO) =>
  version.active || version.lifecycleStatus !== 'DRAFT'

const loadRouteVersionBlockers = async (id: number) => {
  routeVersionBlockerLoadingId.value = id
  routeVersionErrorMessage.value = ''
  try {
    routeVersionBlockersById[id] = await ProRouteApi.getRouteVersionBlockers(id)
  } catch (error) {
    routeVersionErrorMessage.value = resolveRouteVersionErrorMessage(
      error,
      '查询发布阻断项失败，请查看后端返回错误'
    )
    message.error(routeVersionErrorMessage.value)
  } finally {
    routeVersionBlockerLoadingId.value = undefined
  }
}

const buildRouteProductBindPreviewMessage = (
  preview: ProRouteProductBindFromWorkOrdersRespVO
) => {
  return `将按路线“${preview.routeName}”扫描当前租户生产工单，产品名称完全一致的产品会加入该路线。\n新增 ${preview.createdCount} 个，跳过 ${preview.existingCount} 个，冲突 ${preview.conflictCount} 个。是否确认加入？`
}

const confirmRouteProductBindPreview = async (
  preview: ProRouteProductBindFromWorkOrdersRespVO
) => {
  await message.confirm(buildRouteProductBindPreviewMessage(preview), '产品补齐预览')
}

const handleBindRouteProducts = async (row: ProRouteVO) => {
  if (!row.id) {
    throw new Error('补齐工艺路线产品失败：缺少路线编号')
  }
  routeProductBindLoadingId.value = row.id
  try {
    const candidateResult = await ensureSameSourceDraftCandidateForProductionConfig({
      routeId: row.id,
      actionName: '产品补齐',
      changeReason: '产品补齐创建候选版本',
      confirm: (content, title) => message.confirm(content, title),
      success: (content) => message.success(content),
      existingConfirmMessage:
        '当前路线已有草稿候选版本。确认后会把产品补齐结果写入该候选版本，发布前不影响生效版本。是否继续？',
      existingConfirmTitle: '进入候选版本',
      createConfirmMessage:
        '生效版本为只读。确认后创建候选版本，并把产品补齐结果写入候选版本，发布前不影响生效版本。是否继续？',
      createConfirmTitle: '创建候选版本',
      existingSuccessMessage: '正在使用已有候选版本补齐产品',
      createdSuccessMessage: '候选版本已创建，正在补齐产品'
    })
    if (!candidateResult) return
    const routeVersionId = candidateResult.candidate.id
    const preview = await ProRouteProductApi.previewBindFromWorkOrders({ routeId: row.id, routeVersionId })
    await confirmRouteProductBindPreview(preview)
    const result = await ProRouteProductApi.bindFromWorkOrders({ routeId: row.id, routeVersionId })
    message.success(
      `产品补齐完成：新增 ${result.createdCount} 个，跳过 ${result.existingCount} 个，冲突 ${result.conflictCount} 个`
    )
    await getList()
  } catch (error) {
    if (isRouteCandidateConfirmCancel(error)) return
    if (isRouteMultipleDraftCandidateError(error)) {
      await openRouteVersionWorkspace(row, OPEN_CANDIDATE_CONFLICT_NOTICE)
      return
    }
    message.error(resolveRouteVersionErrorMessage(error, '产品补齐失败，请查看后端返回错误'))
  } finally {
    routeProductBindLoadingId.value = undefined
  }
}

const withdrawRouteCandidateVersion = async (id: number) => {
  await runRouteVersionAction(id, '撤回候选版本审核', async () => {
    await ProRouteApi.withdrawRouteCandidateVersion(id)
  })
}

const reopenRouteCandidateVersion = async (id: number) => {
  await runRouteVersionAction(id, '按意见修改候选版本', async () => {
    await ProRouteApi.reopenRouteCandidateVersion(id)
  })
}

const reloadRouteVersionWorkspace = async () => {
  if (routeVersionRoute.value?.id) {
    await loadRouteVersions(routeVersionRoute.value.id)
    await getList()
  }
}

const resolveRoutePublishSuccessMessage = (
  version: ProRouteVersionVO | undefined,
  fromSubmit?: boolean
) => {
  if (version?.lifecycleStatus === 'PENDING_APPROVAL') {
    return fromSubmit ? '候选版本已提交发布，等待审批' : '发布申请已提交，等待审批'
  }
  if (version?.lifecycleStatus === 'ACTIVE') {
    return fromSubmit ? '候选版本已提交并发布生效' : '发布候选版本成功'
  }
  return fromSubmit ? '候选版本已提交，发布策略已执行' : '发布候选版本成功'
}

const resolveLatestRouteVersionForSubmit = async (version: ProRouteVersionVO) => {
  const currentRoute = routeVersionRoute.value
  if (!currentRoute?.id) {
    throw new Error('提交发布失败：缺少路线编号')
  }
  const latestVersions = await ProRouteApi.getRouteVersionList(currentRoute.id)
  routeVersions.value = latestVersions
  const latestVersion = latestVersions.find((item) => item.id === version.id)
  if (!latestVersion) {
    throw new Error('提交发布失败：目标候选版本不存在或已关闭，请刷新版本工作区。')
  }
  if (latestVersion.lifecycleStatus !== 'DRAFT') {
    if (latestVersion.active || latestVersion.lifecycleStatus === 'ACTIVE') {
      message.success('候选版本已发布生效，无需重复提交')
    } else {
      message.warning(
        `提交发布已取消：当前版本状态为${resolveRouteVersionStatusLabel(latestVersion.lifecycleStatus)}，只有草稿候选版本允许提交。`
      )
    }
    await getList()
    return undefined
  }
  return latestVersion
}

const submitRouteCandidateVersion = async (version: ProRouteVersionVO) => {
  routeVersionActionLoadingId.value = version.id
  routeVersionNoticeMessage.value = ''
  routeVersionErrorMessage.value = ''
  try {
    const latestVersion = await resolveLatestRouteVersionForSubmit(version)
    if (!latestVersion) return
    const submittedVersion = await ProRouteApi.submitAndPublishRouteCandidateVersion({
      id: latestVersion.id
    })
    message.success(resolveRoutePublishSuccessMessage(submittedVersion, true))
    await reloadRouteVersionWorkspace()
  } catch (error) {
    if (isUserCancel(error)) return
    routeVersionErrorMessage.value = resolveRouteVersionErrorMessage(
      error,
      '提交发布失败，请查看后端返回错误'
    )
    message.error(routeVersionErrorMessage.value)
  } finally {
    routeVersionActionLoadingId.value = undefined
  }
}

const deleteRouteDraftVersion = async (version: ProRouteVersionVO) => {
  if (!canDeleteRouteDraftVersion(version)) {
    throw new Error('删除草稿失败：只有当前草稿候选版本允许删除')
  }
  try {
    await message.confirm(
      '删除后该草稿将关闭；再次点击编辑会基于当前已发布版本重新生成草稿。是否继续？',
      '删除草稿确认'
    )
  } catch (error) {
    if (isUserCancel(error)) return
    throw error
  }
  await runRouteVersionAction(version.id, '删除草稿', async () => {
    await ProRouteApi.cancelRouteCandidateVersion(version.id)
  })
}

const runRouteVersionAction = async (
  id: number,
  actionName: string,
  action: () => Promise<void>
) => {
  routeVersionActionLoadingId.value = id
  routeVersionNoticeMessage.value = ''
  routeVersionErrorMessage.value = ''
  try {
    await action()
    message.success(`${actionName}成功`)
    if (routeVersionRoute.value?.id) {
      await loadRouteVersions(routeVersionRoute.value.id)
      await getList()
    }
  } catch (error) {
    routeVersionErrorMessage.value = resolveRouteVersionErrorMessage(
      error,
      `${actionName}失败，请查看后端返回错误`
    )
    message.error(routeVersionErrorMessage.value)
  } finally {
    routeVersionActionLoadingId.value = undefined
  }
}

const canSubmitRouteVersion = (version: ProRouteVersionVO) =>
  !version.active && version.lifecycleStatus === 'DRAFT'

const canEditRouteCandidateVersion = (version: ProRouteVersionVO) =>
  !version.active && version.lifecycleStatus === 'DRAFT'

const canWithdrawRouteVersion = (version: ProRouteVersionVO) =>
  !version.active && version.lifecycleStatus === 'PENDING_APPROVAL'

const canReopenRouteVersion = (version: ProRouteVersionVO) =>
  !version.active && version.lifecycleStatus === 'REJECTED'

const openRouteCandidateVersionEditor = async (version: ProRouteVersionVO) => {
  const currentRoute = routeVersionRoute.value
  if (!currentRoute?.id) {
    throw new Error('打开候选版本编辑失败：缺少路线编号')
  }
  if (!canEditRouteCandidateVersion(version)) {
    throw new Error('打开候选版本编辑失败：只有草稿候选版本允许编辑')
  }
  await router.push({
    name: 'MesProRouteEdit',
    params: { id: currentRoute.id },
    query: {
      tab: 'flow',
      routeVersionId: String(version.id),
      routeVersionNo: version.versionNo,
      routeVersionStatus: version.lifecycleStatus
    }
  })
  routeVersionDialogVisible.value = false
}

const canDeleteRouteDraftVersion = (version: ProRouteVersionVO) =>
  !version.active && version.lifecycleStatus === 'DRAFT'

const formatPendingRouteVersion = (row: ProRouteVO) => {
  if (!row.pendingRouteVersionNo) {
    return '无'
  }
  const countSuffix =
    row.pendingRouteVersionCount && row.pendingRouteVersionCount > 1
      ? ` +${row.pendingRouteVersionCount - 1}`
      : ''
  return `${row.pendingRouteVersionNo} ${resolveRouteVersionStatusLabel(row.pendingRouteVersionStatus)}${countSuffix}`
}

const resolveRouteVersionStatusLabel = (status?: ProRouteVersionLifecycleStatus) => {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '审批中',
    READY_TO_PUBLISH: '待发布',
    ACTIVE: '已生效',
    SUPERSEDED: '已替代',
    REJECTED: '已驳回',
    CANCELLED: '已取消'
  }
  return labels[String(status)] || String(status || '-')
}

const resolveRouteVersionStatusTagType = (status?: ProRouteVersionLifecycleStatus) => {
  const types: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    READY_TO_PUBLISH: 'warning',
    ACTIVE: 'success',
    SUPERSEDED: 'info',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return types[String(status)] || 'info'
}

const normalizeRouteName = (routeName: string) => String(routeName || '').trim()

const confirmDuplicateRouteVersionUpgrade = async (routeName: string) => {
  const normalizedName = normalizeRouteName(routeName)
  try {
    await message.confirm(
      `同一个路线名称只能有一个工艺路线，已存在“${normalizedName}”。是否升版本？`,
      '升版本确认'
    )
    return true
  } catch (_cancel) {
    return false
  }
}

const handleDuplicateRouteVersionUpgrade = async (payload: { routeName: string }) => {
  await openExistingRouteForVersionUpgrade(payload.routeName)
}

const openExistingRouteForVersionUpgrade = async (routeName: string) => {
  const normalizedRouteName = normalizeRouteName(routeName)
  if (!normalizedRouteName) {
    throw new Error('打开已有工艺路线升版本失败：缺少路线名称')
  }
  const currentRoute = list.value.find((item) => normalizeRouteName(item.name) === normalizedRouteName)
  if (currentRoute?.id) {
    openEditPage(currentRoute.id, 'basic')
    return
  }
  const data = await ProRouteApi.getRoutePage({
    pageNo: 1,
    pageSize: 10,
    name: normalizedRouteName
  })
  const targetRoute = data.list.find((item: ProRouteVO) => normalizeRouteName(item.name) === normalizedRouteName)
  if (!targetRoute?.id) {
    throw new Error(`打开已有工艺路线升版本失败：未找到同名路线“${normalizedRouteName}”`)
  }
  openEditPage(targetRoute.id, 'basic')
}

const isDuplicateRouteNameError = (error: unknown) => {
  const apiError = error as { code?: number | string; message?: string }
  return Number(apiError?.code) === 1040501006 || apiError?.message === '工艺路线名称已存在'
}

/** 多 Sheet 路线 Excel 导入 */
const handleRouteWorkbookExcelImport = () => {
  routeWorkbookExcelImportFormRef.value.open()
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await ProRouteApi.deleteRoute(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch (error) {
    if (!isUserCancel(error)) {
      message.error(resolveRouteVersionErrorMessage(error, '删除工艺路线失败，请查看后端返回错误'))
    }
  }
}

/** 导出按钮操作 */
const handleExport = async () => {
  let exportConfirmed = false
  try {
    await message.exportConfirm()
    exportConfirmed = true
    exportLoading.value = true
    const data = await ProRouteApi.exportRouteImportWorkbook({})
    download.excel(data, '工艺路线全量导入导出.xlsx')
  } catch (error) {
    if (exportConfirmed) {
      message.error(getErrorMessage(error, '导出失败，请查看后端返回错误'))
    }
  } finally {
    exportLoading.value = false
  }
}

const getErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  if (typeof error === 'string' && error) {
    return error
  }
  return defaultMessage
}

const resolveRouteVersionErrorMessage = (error: unknown, defaultMessage: string) => {
  const apiError = error as { msg?: string; message?: string }
  if (apiError?.msg) {
    return apiError.msg
  }
  if (apiError?.message && !isUserCancel(error)) {
    return apiError.message
  }
  if (typeof error === 'string' && error && !isUserCancel(error)) {
    return error
  }
  return defaultMessage
}

const isUserCancel = (error: unknown) => {
  const text = error instanceof Error ? error.message : String(error || '')
  return ['cancel', 'close'].includes(text)
}

/** 初始化 */
watch(
  () => [route.query.code, route.query.name, route.query.openId],
  async () => {
    await loadListFromRoute()
  }
)

onMounted(async () => {
  await loadListFromRoute()
})
</script>

<style scoped>
.route-list__muted {
  color: #8a93a3;
}

.route-list__version-link {
  font-weight: 500;
}

.route-list__version-tag-link {
  max-width: 100%;
  vertical-align: middle;
}

.route-list__pending-version-tag {
  max-width: 100%;
  cursor: pointer;
}
</style>
