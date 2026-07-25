<template>
  <ContentWrap>
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
          :disabled="selectedCaseIds.length === 0 || !selectedTenantId"
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
                @click="openEdit(row.id)"
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

  <el-dialog v-model="caseDialogVisible" :title="caseForm.id ? '修改测试项' : '新增测试项'" width="860px">
    <el-form ref="caseFormRef" :model="caseForm" :rules="caseRules" label-width="120px">
      <el-form-item label="测试项名称" prop="name">
        <el-input v-model="caseForm.name" placeholder="例如：排产手动重排工单校验" />
      </el-form-item>
      <el-form-item label="测试方法项" prop="methodText">
        <el-input
          v-model="caseForm.methodText"
          :rows="5"
          placeholder="按行录入测试方法，例如：a. 打开排产工单页"
          type="textarea"
        />
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
        <el-radio-group v-model="caseForm.defaultExecutionMode">
          <el-radio-button label="SEQUENTIAL">顺序执行</el-radio-button>
          <el-radio-button label="PARALLEL">并行执行</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="执行控制">
        <el-switch v-model="caseForm.parallelSafe" active-text="允许并行" inactive-text="不允许并行" />
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
            <el-input-number v-model="checkpoint.sort" :min="1" controls-position="right" />
            <el-input v-model="checkpoint.name" placeholder="目标项名称" />
            <el-input
              v-model="checkpoint.expectedText"
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

const caseLoading = ref(false)
const executeLoading = ref(false)
const caseDialogVisible = ref(false)
const caseFormRef = ref<FormInstance>()
const tenantOptions = ref<TenantApi.TenantVO[]>([])
const selectedTenantId = ref<number>()
const selectedCaseIds = ref<number[]>([])
const caseList = ref<CodexTestApi.CodexTestCaseVO[]>([])
const caseTotal = ref(0)

type PaginationPayload = {
  page?: number
  limit?: number
}

const queryParams = reactive<CodexTestApi.CodexTestCasePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  name: '',
  status: undefined,
  executionMode: undefined
})

const CASE_TABLE_KEY = 'system.codexTestManagement.cases'

const caseDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'selection', label: '选择', width: 55, hideable: false, business: false, sortable: false },
  { key: 'name', label: '测试项', minWidth: 220 },
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

const defaultCaseForm = (): CodexTestApi.CodexTestCaseVO => ({
  name: '',
  methodText: '',
  testDataText: '',
  defaultExecutionMode: 'SEQUENTIAL',
  parallelSafe: false,
  status: 'ENABLE',
  sort: 0,
  checkpoints: [newCheckpoint(1)]
})

const caseForm = reactive<CodexTestApi.CodexTestCaseVO>(defaultCaseForm())

const caseRules: FormRules = {
  name: [{ required: true, message: '测试项名称不能为空', trigger: 'blur' }],
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

function resetCaseForm() {
  Object.assign(caseForm, defaultCaseForm())
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

function formatTargetItems(checkpoints?: CodexTestApi.CodexTestCheckpointVO[]) {
  const targetItems = [...(checkpoints || [])]
    .sort((left, right) => (left.sort || 0) - (right.sort || 0))
    .flatMap((checkpoint) => splitDisplayItems(checkpoint.expectedText, checkpoint.name))
  return targetItems.length > 0 ? targetItems : ['-']
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

async function handleCasePagination(payload?: PaginationPayload) {
  if (typeof payload?.page === 'number') {
    queryParams.pageNo = payload.page
  }
  if (typeof payload?.limit === 'number') {
    queryParams.pageSize = payload.limit
  }
  await getCaseList()
}

function handleCaseSelectionChange(rows: CodexTestApi.CodexTestCaseVO[]) {
  selectedCaseIds.value = Array.from(
    new Set(rows.map((row) => row.id).filter((id): id is number => Boolean(id)))
  )
}

function openCreate() {
  resetCaseForm()
  caseDialogVisible.value = true
}

async function openEdit(id?: number) {
  if (!id) return
  try {
    const data = await CodexTestApi.getCodexTestCase(id)
    Object.assign(caseForm, data)
    if (caseForm.checkpoints.length === 0) {
      caseForm.checkpoints = [newCheckpoint(1)]
    }
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
  await caseFormRef.value?.validate()
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
  executeLoading.value = true
  try {
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: mode,
      caseIds: selectedCaseIds.value
    })
    message.success(`已创建执行批次 ${executionId}，请到测试记录页查看结果`)
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
    const executionId = await CodexTestApi.startCodexTestExecution({
      targetTenantId: selectedTenantId.value,
      executionMode: row.defaultExecutionMode,
      caseIds: [caseId]
    })
    message.success(`已创建执行批次 ${executionId}，请到测试记录页查看结果`)
  } catch (error) {
    showRequestError(error, '执行失败')
  } finally {
    executeLoading.value = false
  }
}

onMounted(async () => {
  await getTenantOptions()
  await getCaseList()
})
</script>

<style lang="scss" scoped>
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

.codex-test-checkpoints {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 10px;
}

.codex-test-checkpoint {
  display: grid;
  grid-template-columns: 120px 180px 1fr 60px;
  gap: 10px;
  align-items: flex-start;
}

</style>
