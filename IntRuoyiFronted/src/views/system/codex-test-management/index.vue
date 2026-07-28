<template>
  <el-tabs v-model="activeTab" class="codex-test-tabs" @tab-change="handleTabChange">
    <el-tab-pane label="测试项" name="cases">
      <ContentWrap>
        <div class="codex-runner-status">
          <div class="codex-runner-status__main">
            <span class="codex-runner-status__label">Runner 状态</span>
            <el-tag :type="runnerStatusTagType" effect="plain">
              {{ runnerStatusLabel }}
            </el-tag>
            <span class="codex-runner-status__message">{{ runnerStatusMessage }}</span>
          </div>
          <div class="codex-runner-status__meta">
            <span>运行方式：点击执行时按需拉起本机受控 Runner</span>
            <el-button :loading="runnerStatusLoading" link type="primary" @click="refreshRunnerStatus">
              刷新状态
            </el-button>
          </div>
        </div>
        <UnifiedListTemplate
          class="codex-test-list-template"
          table-key="system.codexTestManagement.cases"
          :query-model="queryParams"
          label-width="76px"
          :filter-definitions="caseQuickFilterDefinitions"
          :quick-filter-state="caseQuickFilter.state"
          :selected-filter-definition="caseQuickFilter.selectedDefinition.value"
          :operator-options="caseQuickFilter.operatorOptions.value"
          :columns="caseColumns"
          :column-saving="caseColumnSaving"
          :show-column-reset="false"
          :total="caseTotal"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @update:quick-filter-state="caseQuickFilter.updateState"
          @quick-filter-query="caseQuickFilter.applyQuickFilter"
          @column-change="saveCaseColumnConfig"
          @pagination="handleCasePagination"
        >
          <template #extra-filters>
            <el-form-item class="codex-test-tenant-filter" label="测试租户">
              <el-select v-model="selectedTenantId" class="!w-240px" placeholder="请选择测试租户">
                <el-option
                  v-for="tenant in tenantOptions"
                  :key="tenant.id"
                  :label="tenant.name"
                  :value="tenant.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item class="codex-test-node-chain-filter" label="串行路线">
              <el-select
                v-model="queryParams.nodeChainName"
                class="!w-240px"
                clearable
                filterable
                placeholder="全部串行路线"
                @change="handleNodeChainFilterChange"
              >
                <el-option
                  v-for="option in nodeChainFilterOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </template>

          <template #actions>
            <el-button v-hasPermi="['system:codex-test:create']" plain type="primary" @click="openCreate">
              <Icon class="mr-5px" icon="ep:plus" />
              新增测试项
            </el-button>
            <el-button
              v-hasPermi="['system:codex-test:execute']"
              :disabled="selectedCaseIds.length === 0 || !selectedTenantId"
              :loading="executeLoading"
              plain
              type="success"
              @click="startExecution('SEQUENTIAL')"
            >
              顺序执行
            </el-button>
            <el-button
              v-hasPermi="['system:codex-test:execute']"
              :disabled="
                selectedCaseIds.length === 0 || !selectedTenantId || selectedCasesContainNodeChain
              "
              :loading="executeLoading"
              plain
              type="warning"
              @click="startExecution('PARALLEL')"
            >
              并行执行
            </el-button>
          </template>

          <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
            <el-table
              v-loading="caseLoading"
              data-user-table-column-explicit
              data-user-table-key="system.codexTestManagement.cases"
              :data="caseList"
              border
              row-key="id"
              :show-overflow-tooltip="true"
              stripe
              @header-dragend="handleCaseHeaderDragend"
              @selection-change="handleCaseSelectionChange"
              @sort-change="handleTemplateSortChange"
            >
          <el-table-column
            v-if="isCaseColumnVisible('selection')"
            type="selection"
            width="55"
          />
          <el-table-column
            v-if="isCaseColumnVisible('name')"
            label="测试项"
            prop="name"
            :width="getCaseColumnWidthString('name')"
            :min-width="getCaseColumnMinWidthString('name', 220)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isCaseColumnVisible('project')"
            label="项目"
            prop="project"
            :width="getCaseColumnWidthString('project', 110)"
            v-bind="sortColumnAttrs('project')"
          >
            <template #default="{ row }">
              <el-tag :type="getProjectTagType(resolveCaseProject(row))" effect="plain">
                {{ resolveCaseProject(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('nodeChain')"
            label="节点串"
            prop="nodeChainName"
            :width="getCaseColumnWidthString('nodeChain')"
            :min-width="getCaseColumnMinWidthString('nodeChain', 190)"
          >
            <template #default="{ row }">
              <div v-if="row.nodeChainName" class="codex-test-node-chain">
                <span>{{ row.nodeChainName }}</span>
                <el-tag effect="plain" size="small">第 {{ row.nodeChainSort }} 节点</el-tag>
              </div>
              <span v-else class="codex-test-node-chain__independent">独立测试项</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('methodText')"
            label="测试方法项"
            prop="methodText"
            :width="getCaseColumnWidthString('methodText')"
            :min-width="getCaseColumnMinWidthString('methodText', 320)"
          >
            <template #default="{ row }">
              <ol class="codex-test-item-list">
                <li v-for="(item, index) in formatMethodItems(row.methodText)" :key="`method-${row.id}-${index}`">
                  {{ item }}
                </li>
              </ol>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('targetItems')"
            label="测试目标项"
            prop="targetItems"
            :width="getCaseColumnWidthString('targetItems')"
            :min-width="getCaseColumnMinWidthString('targetItems', 360)"
          >
            <template #default="{ row }">
              <ol class="codex-test-item-list">
                <li v-for="(item, index) in formatTargetItems(row.checkpoints)" :key="`target-${row.id}-${index}`">
                  {{ item }}
                </li>
              </ol>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('checkpointCount')"
            label="检查点"
            prop="checkpointCount"
            :width="getCaseColumnWidthString('checkpointCount', 90)"
            v-bind="sortColumnAttrs('checkpointCount')"
          />
          <el-table-column
            v-if="isCaseColumnVisible('defaultExecutionMode')"
            label="默认方法"
            prop="defaultExecutionMode"
            :width="getCaseColumnWidthString('defaultExecutionMode', 120)"
            v-bind="sortColumnAttrs('defaultExecutionMode')"
          />
          <el-table-column
            v-if="isCaseColumnVisible('parallelSafe')"
            label="并行安全"
            prop="parallelSafe"
            :width="getCaseColumnWidthString('parallelSafe', 100)"
            v-bind="sortColumnAttrs('parallelSafe')"
          >
            <template #default="{ row }">
              <el-tag :type="row.parallelSafe ? 'success' : 'info'" effect="plain">
                {{ row.parallelSafe ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getCaseColumnWidthString('status', 90)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'" effect="plain">
                {{ row.status === 'ENABLE' ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCaseColumnVisible('actions')"
            fixed="right"
            label="操作"
            prop="actions"
            :width="getCaseColumnWidthString('actions', 220)"
          >
            <template #default="{ row }">
              <el-button
                v-hasPermi="['system:codex-test:execute']"
                :disabled="!selectedTenantId || executeLoading || !row.id"
                :loading="executeLoading"
                link
                type="success"
                @click="startSingleCaseExecution(row)"
              >
                执行
              </el-button>
              <el-button
                v-hasPermi="['system:codex-test:update']"
                link
                type="primary"
                @click="openEdit(row)"
              >
                修改
              </el-button>
              <el-button
                v-hasPermi="['system:codex-test:delete']"
                link
                type="danger"
                @click="deleteCase(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
            </el-table>
          </template>
        </UnifiedListTemplate>
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="运行监控" name="monitor">
      <ContentWrap class="codex-run-monitor">
        <div class="codex-run-monitor__toolbar">
          <div>
            <div class="codex-run-monitor__title">运行监控</div>
            <div class="codex-run-monitor__summary">
              当前正在运行 {{ monitorRunningCount }} 个测试任务
            </div>
          </div>
          <el-button :loading="monitorLoading" @click="getMonitorList">刷新</el-button>
        </div>
        <el-alert
          v-if="monitorLoadError"
          class="mb-12px"
          type="error"
          :closable="false"
          :title="monitorLoadError"
        />
        <div v-loading="monitorLoading" class="codex-run-monitor__body">
          <el-empty v-if="monitorList.length === 0" description="暂无运行中的测试任务" />
          <el-card
            v-for="execution in monitorList"
            :key="execution.id"
            class="codex-run-monitor__execution"
            shadow="never"
          >
            <template #header>
              <div class="codex-run-monitor__execution-header">
                <span>批次 {{ execution.id }} · {{ formatTenantLabel(execution) }}</span>
                <el-tag :type="execution.status === 'RUNNING' ? 'warning' : 'info'" effect="plain">
                  {{ statusText(execution.status) }}
                </el-tag>
              </div>
            </template>
            <div
              v-for="caseResult in execution.cases || []"
              :key="caseResult.id"
              class="codex-run-monitor__case"
            >
              <div class="codex-run-monitor__case-header">
                <span class="codex-run-monitor__case-name">{{ caseResult.caseNameSnapshot }}</span>
                <span class="codex-run-monitor__case-message">
                  {{ caseResult.progressMessage || statusText(caseResult.status) }}
                </span>
              </div>
              <div class="codex-run-monitor__section">
                <div class="codex-run-monitor__section-title">测试方法项</div>
                <div class="codex-run-monitor__steps">
                  <span
                    v-for="(item, index) in formatMethodItems(caseResult.methodTextSnapshot)"
                    :key="`monitor-method-${caseResult.id}-${index}`"
                    class="codex-run-monitor-step"
                    :class="resolveMethodStepState(caseResult, index)"
                  >
                    {{ index + 1 }}. {{ item }}
                  </span>
                </div>
              </div>
              <div class="codex-run-monitor__section">
                <div class="codex-run-monitor__section-title">测试目标项</div>
                <div class="codex-run-monitor__steps">
                  <button
                    v-for="checkpoint in caseResult.checkpointResults || []"
                    :key="`monitor-target-${caseResult.id}-${checkpoint.id}`"
                    type="button"
                    class="codex-run-monitor-step codex-run-monitor-step--button"
                    :class="resolveCheckpointStepState(caseResult, checkpoint)"
                    @click="openFailedCheckpointReason(caseResult, checkpoint)"
                  >
                    {{ checkpoint.checkpointSort }}.
                    {{ checkpoint.checkpointNameSnapshot || checkpoint.expectedTextSnapshot }}
                  </button>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>

  <el-dialog v-model="caseDialogVisible" :title="caseForm.id ? '修改测试项' : '新增测试项'" width="860px">
    <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-width="120px">
      <el-form-item label="测试项名称" prop="name">
        <el-input v-model="caseForm.name" placeholder="例如：排产手动重排工单校验" />
      </el-form-item>
      <el-form-item label="项目" prop="project">
        <el-select v-model="caseForm.project" class="!w-240px" placeholder="请选择项目">
          <el-option
            v-for="project in caseProjectOptions"
            :key="project.value"
            :label="project.label"
            :value="project.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="节点串" prop="nodeChainName">
        <el-select
          v-model="caseForm.nodeChainName"
          allow-create
          class="!w-420px"
          clearable
          filterable
          placeholder="可选；选择已有节点串或输入新名称"
          @change="enforceNodeChainExecutionControl"
        >
          <el-option
            v-for="option in nodeChainOptions"
            :key="option.name"
            :label="`${option.name}（${option.project}，${option.nodeCount} 个节点）`"
            :value="option.name"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="串内序号" prop="nodeChainSort">
        <el-input-number
          v-model="caseForm.nodeChainSort"
          :disabled="!caseForm.nodeChainName"
          :min="1"
          controls-position="right"
          placeholder="节点在串内的执行顺序"
        />
      </el-form-item>
      <el-form-item label="测试方法项" prop="methodText">
        <div class="codex-test-methods">
          <div
            v-for="(methodItem, index) in methodItems"
            :key="index"
            class="codex-test-method"
          >
            <el-input-number
              v-model="methodItem.sort"
              class="codex-test-method__sort"
              :min="1"
              controls-position="right"
            />
            <el-input
              v-model="methodItem.text"
              class="codex-test-method__text"
              placeholder="测试方法，例如：打开排产工单页"
            />
            <el-button
              :disabled="methodItems.length === 1"
              link
              type="danger"
              @click="removeMethodItem(index)"
            >
              删除
            </el-button>
          </div>
          <el-button plain type="primary" @click="addMethodItem">新增方法项</el-button>
        </div>
      </el-form-item>
      <el-form-item label="测试数据">
        <el-input
          v-model="caseForm.testDataText"
          :rows="3"
          placeholder="用户手写数据，例如：来源生产工单号=881MO093613,881MO093615"
          type="textarea"
        />
      </el-form-item>
      <el-form-item label="默认方法">
        <el-radio-group
          v-model="caseForm.defaultExecutionMode"
          :disabled="Boolean(caseForm.nodeChainName)"
        >
          <el-radio-button label="SEQUENTIAL">顺序执行</el-radio-button>
          <el-radio-button label="PARALLEL">并行执行</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="执行控制">
        <el-switch
          v-model="caseForm.parallelSafe"
          active-text="允许并行"
          :disabled="Boolean(caseForm.nodeChainName)"
          inactive-text="不允许并行"
        />
        <el-switch
          v-model="caseForm.status"
          active-text="启用"
          active-value="ENABLE"
          class="ml-24px"
          inactive-text="禁用"
          inactive-value="DISABLE"
        />
      </el-form-item>
      <el-form-item label="测试目标项">
        <div class="codex-test-checkpoints">
          <div
            v-for="(checkpoint, index) in caseForm.checkpoints"
            :key="index"
            class="codex-test-checkpoint"
          >
            <el-input-number
              v-model="checkpoint.sort"
              class="codex-test-checkpoint__sort"
              :min="1"
              controls-position="right"
            />
            <el-input
              v-model="checkpoint.name"
              class="codex-test-checkpoint__name"
              placeholder="目标项名称"
            />
            <el-input
              v-model="checkpoint.expectedText"
              class="codex-test-checkpoint__target"
              placeholder="按行录入测试目标，例如：a. 两个排产工单被筛选出"
              type="textarea"
            />
            <el-button
              :disabled="caseForm.checkpoints.length === 1"
              link
              type="danger"
              @click="removeCheckpoint(index)"
            >
              删除
            </el-button>
          </div>
          <el-button plain type="primary" @click="addCheckpoint">新增目标项</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="caseDialogVisible = false">取消</el-button>
      <el-button v-hasPermi="['system:codex-test:create', 'system:codex-test:update']" type="primary" @click="saveCase">
        保存
      </el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="failedCheckpointDialogVisible" title="目标失败原因" width="640px">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="测试项">
        {{ failedCheckpointContext?.caseName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="目标项">
        {{ failedCheckpointContext?.checkpointName || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="期望结果">
        {{ failedCheckpointContext?.expectedText || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="实际结果">
        {{ failedCheckpointContext?.actualText || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="失败原因">
        {{ failedCheckpointContext?.reason || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>

</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition,
  type UserTableColumnState
} from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import * as CodexTestApi from '@/api/system/codexTestManagement'
import * as TenantApi from '@/api/system/tenant'

defineOptions({ name: 'SystemCodexTestManagement' })

const message = useMessage()

const activeTab = ref<'cases' | 'monitor'>('cases')
const caseLoading = ref(false)
const executeLoading = ref(false)
const runnerStatusLoading = ref(false)
const monitorLoading = ref(false)
const monitorLoadError = ref('')
const caseDialogVisible = ref(false)
const caseFormRef = ref<FormInstance>()
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const selectedCaseIds = ref<number[]>([])
const selectedCases = ref<CodexTestApi.CodexTestCaseVO[]>([])
const caseList = ref<CodexTestApi.CodexTestCaseVO[]>([])
const caseTotal = ref(0)
const nodeChainOptions = ref<CodexTestApi.CodexTestNodeChainOptionVO[]>([])
const monitorList = ref<CodexTestApi.CodexTestExecutionVO[]>([])
const runnerStatus = ref<CodexTestApi.CodexTestRunnerStatusVO>()
const runnerStatusError = ref('')
const monitorRefreshTimer = ref<number>()
const failedCheckpointDialogVisible = ref(false)
const failedCheckpointContext = ref<{
  caseName: string
  checkpointName: string
  expectedText: string
  actualText?: string
  reason?: string
}>()

type PaginationPayload = {
  page?: number
  limit?: number
}

type CodexTestMethodItem = {
  sort: number
  text: string
}

const MONITOR_REFRESH_INTERVAL_MS = 3000
const runningExecutionStatuses = ['PENDING', 'CLAIMED', 'RUNNING']

const queryParams = reactive<CodexTestApi.CodexTestCasePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  name: '',
  project: undefined,
  nodeChainName: undefined,
  status: undefined,
  executionMode: undefined
})

const CASE_TABLE_KEY = 'system.codexTestManagement.cases'
const caseProjectOptions = CodexTestApi.CODEX_TEST_PROJECT_OPTIONS
const nodeChainFilterOptions = computed(() =>
  nodeChainOptions.value.map((option) => ({
    label: `${option.name}（${option.project}，${option.nodeCount} 个节点）`,
    value: option.name
  }))
)
const selectedCasesContainNodeChain = computed(() =>
  selectedCases.value.some((testCase) => Boolean(testCase.nodeChainName))
)

const caseDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'selection', label: '选择', width: 55, hideable: false, business: false, sortable: false },
  { key: 'name', label: '测试项', minWidth: 220 },
  { key: 'project', label: '项目', width: 110 },
  { key: 'nodeChain', label: '节点串', minWidth: 190, sortable: false },
  { key: 'methodText', label: '测试方法项', minWidth: 320, sortable: false },
  { key: 'targetItems', label: '测试目标项', minWidth: 360, sortable: false },
  { key: 'checkpointCount', label: '检查点', width: 90 },
  { key: 'defaultExecutionMode', label: '默认方法', width: 120 },
  { key: 'parallelSafe', label: '并行安全', width: 100 },
  { key: 'status', label: '状态', width: 90 },
  { key: 'actions', label: '操作', width: 220, hideable: false, business: false, sortable: false }
]

const caseColumnControl = useUserTableColumns(CASE_TABLE_KEY, caseDefaultColumns)
const caseColumns = computed(() => caseColumnControl.columns.value)
const caseColumnSaving = computed(() => caseColumnControl.saving.value)
const isCaseColumnVisible = (key: string) => caseColumnControl.isColumnVisible(key)
const getCaseColumnWidthString = (key: string, fallback?: number) =>
  caseColumnControl.getColumnWidthString(key, fallback)
const getCaseColumnMinWidthString = (key: string, fallback?: number) =>
  caseColumnControl.getColumnMinWidthString(key, fallback)
const handleCaseHeaderDragend = async (newWidth: number, oldWidth: number, column: any) => {
  await caseColumnControl.handleHeaderDragend(newWidth, oldWidth, column)
}
const saveCaseColumnConfig = async (columns: UserTableColumnState[]) => {
  await caseColumnControl.saveConfig(columns)
}

const caseQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'name',
    label: '测试项',
    type: 'text',
    queryParamKey: 'name',
    placeholder: '输入测试项名称'
  },
  {
    key: 'project',
    label: '项目',
    type: 'select',
    queryParamKey: 'project',
    options: caseProjectOptions,
    placeholder: '全部'
  },
  {
    key: 'nodeChainName',
    label: '节点串',
    type: 'select',
    queryParamKey: 'nodeChainName',
    options: nodeChainFilterOptions.value,
    placeholder: '全部'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '启用', value: 'ENABLE' },
      { label: '禁用', value: 'DISABLE' }
    ],
    placeholder: '全部'
  }
])

const caseQuickFilter = useTableQuickFilter(
  CASE_TABLE_KEY,
  caseQuickFilterDefinitions,
  queryParams,
  getCaseList
)

const monitorRunningCount = computed(() =>
  monitorList.value.reduce(
    (total, execution) =>
      total +
      (execution.cases || []).filter((caseResult) => runningExecutionStatuses.includes(caseResult.status)).length,
    0
  )
)

const runnerStatusLabel = computed(() => {
  if (executeLoading.value) return '启动中'
  if (runnerStatus.value?.online) return '可用'
  if (runnerStatus.value?.status === 'CAPABILITY_MISSING') return '配置异常'
  if (runnerStatusError.value) return '诊断失败'
  return '按需启动'
})

const runnerStatusMessage = computed(() => {
  if (executeLoading.value) return '正在提交执行，后端会按需启动受控 Runner'
  if (runnerStatus.value?.online) return 'Runner 可用，可领取测试任务'
  if (runnerStatus.value?.status === 'CAPABILITY_MISSING') return runnerStatus.value.message
  if (runnerStatusError.value) return runnerStatusError.value
  return '无需常驻在线；点击执行时会自动拉起本机受控 Runner'
})

const runnerStatusTagType = computed(() => {
  if (executeLoading.value) return 'warning'
  if (runnerStatus.value?.online) return 'success'
  if (runnerStatus.value?.status === 'CAPABILITY_MISSING' || runnerStatusError.value) return 'danger'
  return 'warning'
})

const defaultCaseForm = (): CodexTestApi.CodexTestCaseVO => ({
  name: '',
  project: undefined,
  nodeChainName: undefined,
  nodeChainSort: undefined,
  methodText: '',
  testDataText: '',
  defaultExecutionMode: 'SEQUENTIAL',
  parallelSafe: false,
  status: 'ENABLE',
  sort: 0,
  checkpoints: [newCheckpoint(1)]
})

const caseForm = reactive<CodexTestApi.CodexTestCaseVO>(defaultCaseForm())
const methodItems = ref<CodexTestMethodItem[]>([newMethodItem(1)])

const caseRules: FormRules = {
  name: [{ required: true, message: '测试项名称不能为空', trigger: 'blur' }],
  project: [{ required: true, message: '项目不能为空', trigger: 'change' }],
  nodeChainSort: [
    {
      validator: (_rule, value, callback) => {
        if (caseForm.nodeChainName && (!value || value < 1)) {
          callback(new Error('节点串测试项必须填写大于 0 的串内序号'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  methodText: [{ required: true, message: '测试方法项不能为空', trigger: 'blur' }]
}

function newCheckpoint(sort: number): CodexTestApi.CodexTestCheckpointVO {
  return {
    sort,
    name: `检查点 ${sort}`,
    expectedText: '',
    severity: 'MAJOR'
  }
}

function newMethodItem(sort: number): CodexTestMethodItem {
  return {
    sort,
    text: ''
  }
}

function resetCaseForm() {
  Object.assign(caseForm, defaultCaseForm())
  methodItems.value = [newMethodItem(1)]
}

function enforceNodeChainExecutionControl(value?: string, preserveSort = false) {
  const nodeChainName = value?.trim()
  caseForm.nodeChainName = nodeChainName || undefined
  if (!nodeChainName) {
    caseForm.nodeChainSort = undefined
    return
  }
  caseForm.defaultExecutionMode = 'SEQUENTIAL'
  caseForm.parallelSafe = false
  const existingOption = nodeChainOptions.value.find((option) => option.name === nodeChainName)
  if (existingOption) {
    caseForm.project = existingOption.project
  }
  if (!preserveSort || !caseForm.nodeChainSort || caseForm.nodeChainSort < 1) {
    caseForm.nodeChainSort = existingOption ? existingOption.nextNodeSort : 1
  }
}

function splitDisplayItems(text?: string, fallback?: string) {
  const source = text?.trim() || fallback?.trim() || ''
  if (!source) return ['-']
  return source
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function formatMethodItems(methodText?: string) {
  return splitDisplayItems(methodText)
}

function parseMethodItems(methodText?: string) {
  const items = (methodText || '')
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
    .map((text, index) => ({
      sort: index + 1,
      text
    }))
  return items.length > 0 ? items : [newMethodItem(1)]
}

function normalizeCheckpointItems(checkpoints?: CodexTestApi.CodexTestCheckpointVO[]) {
  const normalized = [...(checkpoints || [])]
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .flatMap((checkpoint) =>
      splitDisplayItems(checkpoint.expectedText, checkpoint.name)
        .filter((expectedText) => expectedText !== '-')
        .map((expectedText) => ({
          name: checkpoint.name?.trim() || '',
          expectedText,
          severity: checkpoint.severity || 'MAJOR',
          remark: checkpoint.remark
        }))
    )
    .map((checkpoint, index) => ({
      ...checkpoint,
      sort: index + 1,
      name: checkpoint.name || `检查点 ${index + 1}`
    }))
  return normalized.length > 0 ? normalized : [newCheckpoint(1)]
}

function serializeMethodItems() {
  return [...methodItems.value]
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .map((item) => item.text.trim())
    .filter(Boolean)
    .join('\n')
}

function addMethodItem() {
  methodItems.value.push(newMethodItem(methodItems.value.length + 1))
}

function removeMethodItem(index: number) {
  methodItems.value.splice(index, 1)
  methodItems.value.forEach((methodItem, methodIndex) => {
    methodItem.sort = methodIndex + 1
  })
}

function formatTargetItems(checkpoints?: CodexTestApi.CodexTestCheckpointVO[]) {
  const targetItems = [...(checkpoints || [])]
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .flatMap((checkpoint) => splitDisplayItems(checkpoint.expectedText, checkpoint.name))
  return targetItems.length > 0 ? targetItems : ['-']
}

function isFinishedCase(caseResult: CodexTestApi.CodexTestExecutionCaseVO) {
  return !runningExecutionStatuses.includes(caseResult.status)
}

function resolveMethodStepState(caseResult: CodexTestApi.CodexTestExecutionCaseVO, index: number) {
  const sort = index + 1
  if (isFinishedCase(caseResult) || caseResult.progressPhase === 'CHECKPOINT' || caseResult.progressPhase === 'DONE') {
    return 'codex-run-monitor-step--success'
  }
  if (caseResult.progressPhase === 'METHOD' && caseResult.currentMethodSort) {
    if (sort < caseResult.currentMethodSort) return 'codex-run-monitor-step--success'
    if (sort === caseResult.currentMethodSort) return 'codex-run-monitor-step--running'
  }
  return 'codex-run-monitor-step--pending'
}

function resolveCheckpointStepState(
  caseResult: CodexTestApi.CodexTestExecutionCaseVO,
  checkpoint: CodexTestApi.CodexTestCheckpointResultVO
) {
  if (checkpoint.status === 'PASS') return 'codex-run-monitor-step--success'
  if (checkpoint.status === 'FAIL' || checkpoint.status === 'BLOCKED') return 'codex-run-monitor-step--failed'
  if (
    caseResult.progressPhase === 'CHECKPOINT' &&
    caseResult.currentCheckpointSort === checkpoint.checkpointSort
  ) {
    return 'codex-run-monitor-step--running'
  }
  return 'codex-run-monitor-step--pending'
}

function openFailedCheckpointReason(
  caseResult: CodexTestApi.CodexTestExecutionCaseVO,
  checkpoint: CodexTestApi.CodexTestCheckpointResultVO
) {
  if (resolveCheckpointStepState(caseResult, checkpoint) !== 'codex-run-monitor-step--failed') return
  failedCheckpointContext.value = {
    caseName: caseResult.caseNameSnapshot,
    checkpointName: checkpoint.checkpointNameSnapshot,
    expectedText: checkpoint.expectedTextSnapshot,
    actualText: checkpoint.actualText,
    reason: checkpoint.mismatchDescription || caseResult.failureReason
  }
  failedCheckpointDialogVisible.value = true
}

function resolveCaseProject(row: CodexTestApi.CodexTestCaseVO): CodexTestApi.CodexTestProject {
  if (caseProjectOptions.some((project) => project.value === row.project)) {
    return row.project as CodexTestApi.CodexTestProject
  }
  const source = [
    row.name,
    row.methodText,
    row.testDataText,
    ...(row.checkpoints || []).flatMap((checkpoint) => [checkpoint.name, checkpoint.expectedText])
  ].join('\n')
  const normalized = source.toLowerCase()
  if (
    source.includes('批记录') ||
    source.includes('记录本') ||
    normalized.includes('edhr') ||
    normalized.includes('batch-record') ||
    normalized.includes('recordbook')
  ) {
    return '批记录'
  }
  if (
    source.includes('文控') ||
    normalized.includes('/dcc/') ||
    normalized.includes('controlled-file') ||
    normalized.includes('dcc-')
  ) {
    return '文控'
  }
  if (
    source.includes('工艺路线') ||
    normalized.includes('/mes/pro/route') ||
    normalized.includes('pro-route')
  ) {
    return '工艺路线'
  }
  return '智能排产'
}

function getProjectTagType(project?: CodexTestApi.CodexTestProject) {
  if (project === '智能排产') return 'success'
  if (project === '批记录') return 'primary'
  if (project === '文控') return 'warning'
  if (project === '工艺路线') return 'danger'
  return 'info'
}

const formatTenantLabel = (row: CodexTestApi.CodexTestExecutionVO) =>
  row.targetTenantName || tenantOptions.value.find((tenant) => tenant.id === row.targetTenantId)?.name || row.targetTenantId

function statusText(status?: string) {
  const labels: Record<string, string> = {
    PENDING: '待执行',
    CLAIMED: '已领取',
    RUNNING: '执行中',
    PASS: '通过',
    FAIL: '失败',
    BLOCKED: '阻塞',
    CANCELED: '已取消',
    TIMEOUT: '超时',
    NOT_RUN: '未执行'
  }
  return status ? labels[status] || status : '-'
}

function showRequestError(error: unknown, defaultMessage: string) {
  const text = error instanceof Error ? error.message : typeof error === 'string' ? error : defaultMessage
  message.error(text || defaultMessage)
}

async function getTenantOptions() {
  try {
    tenantOptions.value = await TenantApi.getTenantList()
    selectedTenantId.value = tenantOptions.value[0]?.id
  } catch (error) {
    showRequestError(error, '测试租户加载失败')
  }
}

async function getCaseList() {
  caseLoading.value = true
  try {
    const data = await CodexTestApi.getCodexTestCasePage(queryParams)
    caseList.value = data.list
    caseTotal.value = data.total
  } catch (error) {
    showRequestError(error, '测试项加载失败')
  } finally {
    caseLoading.value = false
  }
}

async function getNodeChainOptions() {
  try {
    nodeChainOptions.value = await CodexTestApi.getCodexTestNodeChainOptions()
  } catch (error) {
    nodeChainOptions.value = []
    showRequestError(error, '节点串选项加载失败')
  }
}

async function refreshRunnerStatus() {
  runnerStatusLoading.value = true
  try {
    runnerStatusError.value = ''
    runnerStatus.value = await CodexTestApi.getCodexTestRunnerStatus()
    return runnerStatus.value
  } catch (error) {
    const text = error instanceof Error ? error.message : 'Runner 状态加载失败'
    runnerStatus.value = undefined
    runnerStatusError.value = text
    return undefined
  } finally {
    runnerStatusLoading.value = false
  }
}

async function getMonitorList() {
  monitorLoading.value = true
  monitorLoadError.value = ''
  try {
    monitorList.value = await CodexTestApi.getCodexTestExecutionMonitor()
  } catch (error) {
    const text = error instanceof Error ? error.message : '运行监控加载失败'
    monitorLoadError.value = text
    monitorList.value = []
    message.error(text)
  } finally {
    monitorLoading.value = false
  }
}

function stopMonitorRefresh() {
  if (monitorRefreshTimer.value) {
    window.clearInterval(monitorRefreshTimer.value)
    monitorRefreshTimer.value = undefined
  }
}

function startMonitorRefresh() {
  stopMonitorRefresh()
  monitorRefreshTimer.value = window.setInterval(() => {
    getMonitorList()
  }, MONITOR_REFRESH_INTERVAL_MS)
}

async function handleTabChange(name: string | number) {
  if (name === 'monitor') {
    await getMonitorList()
    startMonitorRefresh()
    return
  }
  stopMonitorRefresh()
}

async function handleCasePagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') {
    queryParams.pageNo = payload.page
  }
  if (typeof payload?.limit === 'number') {
    queryParams.pageSize = payload.limit
  }
  await getCaseList()
}

async function handleNodeChainFilterChange() {
  queryParams.pageNo = 1
  selectedCases.value = []
  selectedCaseIds.value = []
  await getCaseList()
}

function handleCaseSelectionChange(rows: CodexTestApi.CodexTestCaseVO[]) {
  selectedCases.value = rows
  selectedCaseIds.value = Array.from(
    new Set(rows.map((row) => row.id).filter((id): id is number => Boolean(id)))
  )
}

function openCreate() {
  resetCaseForm()
  caseDialogVisible.value = true
}

function applyCaseFormForEdit(data: CodexTestApi.CodexTestCaseVO) {
  Object.assign(caseForm, data)
  caseForm.checkpoints = normalizeCheckpointItems(data.checkpoints)
  methodItems.value = parseMethodItems(data.methodText)
  enforceNodeChainExecutionControl(caseForm.nodeChainName, true)
}

async function openEdit(row: CodexTestApi.CodexTestCaseVO) {
  const id = row.id
  if (!id) return
  try {
    const data = await CodexTestApi.getCodexTestCase(id)
    applyCaseFormForEdit({
      ...row,
      ...data,
      checkpoints: data.checkpoints?.length ? data.checkpoints : row.checkpoints
    })
    caseDialogVisible.value = true
  } catch (error) {
    showRequestError(error, '测试项详情加载失败')
  }
}

function addCheckpoint() {
  caseForm.checkpoints.push(newCheckpoint(caseForm.checkpoints.length + 1))
}

function removeCheckpoint(index: number) {
  caseForm.checkpoints.splice(index, 1)
  caseForm.checkpoints.forEach((checkpoint, checkpointIndex) => {
    checkpoint.sort = checkpointIndex + 1
  })
}

async function saveCase() {
  enforceNodeChainExecutionControl(caseForm.nodeChainName, true)
  caseForm.methodText = serializeMethodItems()
  await caseFormRef.value?.validate()
  if (!caseForm.methodText.trim()) {
    message.error('测试方法项不能为空')
    return
  }
  if (caseForm.checkpoints.some((checkpoint) => !checkpoint.expectedText?.trim())) {
    message.error('测试目标项不能为空')
    return
  }
  try {
    if (caseForm.id) {
      await CodexTestApi.updateCodexTestCase(caseForm)
    } else {
      await CodexTestApi.createCodexTestCase(caseForm)
    }
    message.success('保存成功')
    caseDialogVisible.value = false
    await getNodeChainOptions()
    await getCaseList()
  } catch (error) {
    showRequestError(error, '保存失败')
  }
}

async function deleteCase(id?: number) {
  if (!id) return
  try {
    await message.confirm('确认删除该测试项吗？')
    await CodexTestApi.deleteCodexTestCase(id)
    message.success('删除成功')
    await getNodeChainOptions()
    await getCaseList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      showRequestError(error, '删除失败')
    }
  }
}

async function startExecution(mode: 'SEQUENTIAL' | 'PARALLEL') {
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  const selectedNodeChainNames = new Set(
    selectedCases.value
      .map((testCase) => testCase.nodeChainName)
      .filter((name): name is string => Boolean(name))
  )
  const selectedNodeChainCaseCount = selectedCases.value.filter((testCase) =>
    Boolean(testCase.nodeChainName)
  ).length
  if (selectedNodeChainNames.size > 1) {
    message.error('一次只能执行一个节点串，请先按节点串筛选后再选择')
    return
  }
  if (selectedNodeChainNames.size === 1 && selectedNodeChainCaseCount !== selectedCases.value.length) {
    message.error('节点串测试项不能与独立测试项混合执行')
    return
  }
  if (mode === 'PARALLEL' && selectedNodeChainNames.size > 0) {
    message.error('节点串只能使用顺序执行')
    return
  }
  executeLoading.value = true
  try {
    runnerStatusError.value = ''
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: mode,
      caseIds: selectedCaseIds.value
    })
    message.success(`已创建执行批次 ${executionId}，Runner 将按需启动并领取任务`)
    activeTab.value = 'monitor'
    await getMonitorList()
    startMonitorRefresh()
    await refreshRunnerStatus()
  } catch (error) {
    showRequestError(error, mode === 'PARALLEL' ? '并行执行失败' : '顺序执行失败')
  } finally {
    executeLoading.value = false
  }
}

async function startSingleCaseExecution(row: CodexTestApi.CodexTestCaseVO) {
  const caseId = row.id
  if (!caseId) return
  if (!selectedTenantId.value) {
    message.error('请选择测试租户')
    return
  }
  executeLoading.value = true
  try {
    runnerStatusError.value = ''
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: row.defaultExecutionMode,
      caseIds: [caseId]
    })
    message.success(`已创建执行批次 ${executionId}，Runner 将按需启动并领取任务`)
    activeTab.value = 'monitor'
    await getMonitorList()
    startMonitorRefresh()
    await refreshRunnerStatus()
  } catch (error) {
    showRequestError(error, '执行失败')
  } finally {
    executeLoading.value = false
  }
}

onMounted(async () => {
  await getTenantOptions()
  await refreshRunnerStatus()
  await getNodeChainOptions()
  await getCaseList()
})

onBeforeUnmount(() => {
  stopMonitorRefresh()
})
</script>

<style lang="scss" scoped>
.codex-test-tabs {
  :deep(.el-tabs__header) {
    margin: 0 0 12px;
  }
}

.codex-test-list-template {
  :deep(.unified-list-template__table-shell .el-table__header th) {
    background: #f7f9fc;
    color: #172033;
    font-weight: 600;
  }

  :deep(.el-table__row) {
    min-height: 52px;
  }

  :deep(.el-table .cell) {
    line-height: 1.45;
  }
}

.codex-runner-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  margin-bottom: 12px;
  border: 1px solid #e5eaf3;
  border-radius: 8px;
  background: #f8fafc;
}

.codex-runner-status__main,
.codex-runner-status__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.codex-runner-status__label {
  font-weight: 600;
  color: #172033;
}

.codex-runner-status__message,
.codex-runner-status__meta {
  color: #4e5969;
}

.codex-test-item-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  line-height: 1.45;
}

.codex-test-item-list li {
  white-space: normal;
  word-break: break-word;
}

.codex-test-node-chain {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #172033;
  font-weight: 600;
}

.codex-test-node-chain__independent {
  color: var(--el-text-color-secondary);
}

.codex-test-methods {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 10px;
}

.codex-test-method {
  display: grid;
  grid-template-columns: 112px minmax(420px, 1fr) 48px;
  gap: 10px;
  align-items: flex-start;
}

.codex-test-method__sort {
  min-width: 0;
  width: 100%;
}

.codex-test-method__text {
  min-width: 0;
  width: 100%;
}

.codex-test-checkpoints {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 10px;
}

.codex-test-checkpoint {
  display: grid;
  grid-template-columns: 112px minmax(220px, 0.8fr) minmax(260px, 1fr) 48px;
  gap: 10px;
  align-items: flex-start;
}

.codex-test-checkpoint__sort {
  min-width: 0;
  width: 100%;
}

.codex-test-checkpoint__name,
.codex-test-checkpoint__target {
  min-width: 0;
  width: 100%;
}

.codex-run-monitor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.codex-run-monitor__title {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

.codex-run-monitor__summary {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}

.codex-run-monitor__body {
  min-height: 180px;
}

.codex-run-monitor__execution {
  margin-bottom: 12px;
  border-color: #dfe5ef;
}

.codex-run-monitor__execution-header,
.codex-run-monitor__case-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.codex-run-monitor__case {
  padding: 12px 0;
  border-bottom: 1px solid #edf0f5;
}

.codex-run-monitor__case:last-child {
  border-bottom: 0;
}

.codex-run-monitor__case-name {
  color: #172033;
  font-weight: 700;
}

.codex-run-monitor__case-message {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.codex-run-monitor__section {
  margin-top: 10px;
}

.codex-run-monitor__section-title {
  margin-bottom: 8px;
  color: #596275;
  font-weight: 600;
}

.codex-run-monitor__steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.codex-run-monitor-step {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 8px 10px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #f7f9fc;
  color: #344054;
  line-height: 1.45;
  text-align: left;
  white-space: normal;
  word-break: break-word;
}

.codex-run-monitor-step--button {
  cursor: default;
  font: inherit;
}

.codex-run-monitor-step--success {
  border-color: #95d475;
  background: #f0f9eb;
  color: #2f6b20;
}

.codex-run-monitor-step--running {
  border-color: #eebe77;
  background: #fdf6ec;
  color: #9a5b13;
  font-weight: 700;
}

.codex-run-monitor-step--failed {
  border-color: #f89898;
  background: #fef0f0;
  color: #a51e1e;
  cursor: pointer;
  font-weight: 700;
}

.codex-run-monitor-step--pending {
  border-color: #d8dee8;
  background: #f7f9fc;
  color: #667085;
}

</style>
