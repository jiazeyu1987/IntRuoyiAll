<!-- MES 工艺路线列表 -->
<template>
  <doc-alert
    title="【生产】工序设置、工艺流程"
    url="https://doc.iocoder.cn/mes/pro/process-route/"
  />

  <ContentWrap class="route-list-page">
    <UnifiedListTemplate
      single-line-toolbar
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
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="routeQuickFilter.updateState"
      @quick-filter-query="routeQuickFilter.applyQuickFilter"
      @column-change="saveRouteColumnConfig"
      @pagination="getList"
      :load-filter-rows="loadColumnFilterRows0"
      @filtered-rows="list = $event"
      :list-rows="list"
      @column-filter-reset="getList"
      :column-filter-definitions="listColumnFilterDefinitions0"
    >
      <template #actions>
        <el-dropdown
          v-if="checkPermi(['mes:pro-route:create', 'mes:pro-route:export'])"
          trigger="click"
        >
          <el-button type="primary" plain :loading="exportLoading">
            高级 <Icon icon="ep:arrow-down" class="ml-5px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-if="checkPermi(['mes:pro-route:create'])"
                @click="openForm('create')"
              >
                <Icon icon="ep:plus" class="mr-5px" /> 新增
              </el-dropdown-item>
              <el-dropdown-item
                v-if="checkPermi(['mes:pro-route:create'])"
                @click="handleRouteWorkbookExcelImport"
              >
                <Icon icon="ep:document-add" class="mr-5px" /> 导入
              </el-dropdown-item>
              <el-dropdown-item
                v-if="checkPermi(['mes:pro-route:export'])"
                :disabled="exportLoading"
                @click="handleExport"
              >
                <Icon icon="ep:download" class="mr-5px" /> 导出
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template #table="{ resolveRows, sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          :data-user-table-key="ROUTE_LIST_TABLE_KEY"
          :data="resolveRows(list)"
          height="100%"
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
              <el-button link type="primary" @click="openEditPage(scope.row.id, 'flow')">
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
            v-if="isRouteColumnVisible('productCount')"
            label="关联产品"
            align="center"
            prop="productCount"
            :min-width="getRouteColumnMinWidthString('productCount', 110)"
            v-bind="sortColumnAttrs('productCount')"
          />
          <el-table-column
            v-if="isRouteColumnVisible('dccProjectCode')"
            label="关联项目"
            align="center"
            prop="dccProjectCode"
            :min-width="getRouteColumnMinWidthString('dccProjectCode', 180)"
            v-bind="sortColumnAttrs('dccProjectCode')"
            show-overflow-tooltip
          >
            <template #default="scope">
              {{ scope.row.dccProjectCode || '未关联' }}
            </template>
          </el-table-column>
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
            :width="getRouteColumnWidthString('actions', 280)"
            fixed="right"
          >
            <template #default="scope">
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
        <el-table-column label="最后保存" prop="lastSavedUserName" min-width="140">
          <template #default="{ row: version }">
            {{
              version.lastSavedUserName ||
              (version.lastSavedBy ? `未找到用户（${version.lastSavedBy}）` : '-')
            }}
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="publishedTime" min-width="170">
          <template #default="{ row: version }">
            {{ formatDateTimeValue(version.publishedTime, '-') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
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
import { loadAllListPages, type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import { until } from '@vueuse/core'
import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { dateFormatter, formatDateTimeValue } from '@/utils/formatTime'
import { CommonStatusEnum } from '@/utils/constants'
import { checkPermi } from '@/utils/permission'
import download from '@/utils/download'
import {
  ProRouteApi,
  type ProRouteVO,
  type ProRouteVersionLifecycleStatus,
  type ProRouteVersionVO
} from '@/api/mes/pro/route'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
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
const RouteWorkbookExcelImportForm = defineAsyncComponent(
  () => import('./RouteWorkbookExcelImportForm.vue')
)

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()
const MES_PRO_ROUTE_LIST_PATH = '/mes/pro/route'
const isMesProRouteListPath = () => route.path === MES_PRO_ROUTE_LIST_PATH
const buildMesProRouteListStateKey = () =>
  JSON.stringify({
    code: typeof route.query.code === 'string' ? route.query.code : '',
    name: typeof route.query.name === 'string' ? route.query.name : '',
    openId: typeof route.query.openId === 'string' ? route.query.openId : ''
  })

const loading = ref(true) // 列表的加载中
const list = ref<ProRouteVO[]>([]) // 列表的数据
const total = ref(0) // 列表的总页数
const mesProRouteListHasLoadedRouteState = ref(false)
let mesProRouteListLastLoadedStateKey = ''
const shouldKeepMesProRouteListLoadedState = (targetStateKey: string) =>
  mesProRouteListHasLoadedRouteState.value &&
  mesProRouteListLastLoadedStateKey === targetStateKey &&
  !loading.value
const exportLoading = ref(false) // 导出的加载中
const openedRouteDetailId = ref('')
const copyDialogVisible = ref(false)
const copySaving = ref(false)
const copySourceRoute = ref<ProRouteVO>()
const routeVersionDialogVisible = ref(false)
const routeVersionLoading = ref(false)
const routeVersionActionLoadingId = ref<number>()
const routeVersionRoute = ref<ProRouteVO>()
const routeVersions = ref<ProRouteVersionVO[]>([])
const routeVersionNoticeMessage = ref('')
const routeVersionErrorMessage = ref('')
const routeCandidateEditLoadingId = ref<number | undefined>()
const OPEN_CANDIDATE_CONFLICT_NOTICE =
  '当前路线存在多个打开中的候选版本，请通过待发布版本或编辑入口处理打开候选；版本列表仅展示草稿及已生效历史版本。'
const ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET = new Set(['DRAFT', 'ACTIVE', 'SUPERSEDED'])
const isVisibleRouteVersionInWorkspace = (version: ProRouteVersionVO) =>
  version.active || ROUTE_VERSION_WORKSPACE_VISIBLE_STATUS_SET.has(String(version.lifecycleStatus))
const visibleRouteVersions = computed(() =>
  routeVersions.value.filter(isVisibleRouteVersionInWorkspace)
)
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
  { key: 'productCount', label: '关联产品', minWidth: 110 },
  { key: 'dccProjectCode', label: '关联项目', minWidth: 180 },
  { key: 'createTime', label: '创建时间', width: 180 },
  { key: 'actions', label: '操作', width: 280, hideable: false, business: false }
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
  const targetStateKey = buildMesProRouteListStateKey()
  queryParams.code = typeof route.query.code === 'string' ? route.query.code : undefined
  queryParams.name = typeof route.query.name === 'string' ? route.query.name : undefined
  queryParams.pageNo = 1
  await getList()
  mesProRouteListLastLoadedStateKey = targetStateKey
  mesProRouteListHasLoadedRouteState.value = true
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
      message.error(
        resolveRouteVersionErrorMessage(error, '更新工艺路线状态失败，请查看后端返回错误')
      )
    }
  }
}

/** 添加/修改操作 */
const openForm = async (type: string, id?: number) => {
  await until(() => typeof formRef.value?.open === 'function').toBeTruthy({
    timeout: 60000,
    throwOnTimeout: true
  })
  await formRef.value.open(type, id)
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
      if (
        !row.pendingRouteVersionId ||
        !row.pendingRouteVersionNo ||
        !row.pendingRouteVersionStatus
      ) {
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
    message.error(
      resolveRouteVersionErrorMessage(error, '进入候选版本编辑失败，请查看后端返回错误')
    )
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
    query: buildRouteCandidateEditQuery(version, { tab: 'flow' })
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
  const currentRoute = list.value.find(
    (item) => normalizeRouteName(item.name) === normalizedRouteName
  )
  if (currentRoute?.id) {
    openEditPage(currentRoute.id, 'basic')
    return
  }
  const data = await ProRouteApi.getRoutePage({
    pageNo: 1,
    pageSize: 10,
    name: normalizedRouteName
  })
  const targetRoute = data.list.find(
    (item: ProRouteVO) => normalizeRouteName(item.name) === normalizedRouteName
  )
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
    if (!isMesProRouteListPath()) {
      return
    }
    const targetStateKey = buildMesProRouteListStateKey()
    if (shouldKeepMesProRouteListLoadedState(targetStateKey)) {
      return
    }
    await loadListFromRoute()
  }
)

onMounted(async () => {
  if (!isMesProRouteListPath()) {
    return
  }
  await loadListFromRoute()
})

const loadColumnFilterRows0 = async () => {
  const params = { ...queryParams }
  const rows = await loadAllListPages<any>((pagination) =>
    ProRouteApi.getRoutePage({ ...params, ...pagination })
  )
  return rows
}

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'flowGraphConfigured',
    label: '关系图',
    type: 'text',
    getValue: (row) => [row.flowGraphConfigured ? '已设' : '未设'].join(' ')
  },
  {
    key: 'pendingRouteVersionNo',
    label: '待发布版本',
    type: 'text',
    getValue: (row) => [formatPendingRouteVersion(row)].join(' ')
  }
])
</script>

<style scoped>
.route-list-page {
  height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding));
  margin-bottom: 0;
}

.route-list-page :deep(.el-card__body) {
  display: flex;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  flex-direction: column;
}

.route-list-page :deep(.unified-list-template),
.route-list-page :deep(.unified-list-template__table-shell) {
  flex: 1;
  min-height: 0;
}

.route-list-page :deep(.unified-list-template__table-shell) {
  overflow: hidden;
}

.route-list-page :deep(.el-form),
.route-list-page :deep(.el-pagination) {
  flex-shrink: 0;
}

.route-list-page :deep(.el-pagination) {
  margin-bottom: 0;
}

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
