<template>
  <ContentWrap>
    <UnifiedListTemplate
      table-key="system.codex-test.records"
      :query-model="recordQueryParams"
      label-width="82px"
      query-form-test-id="system-codex-test-record-filter-form"
      :filter-definitions="recordQuickFilterDefinitions"
      :quick-filter-state="recordQuickFilter.state"
      :selected-filter-definition="recordQuickFilter.selectedDefinition.value"
      :operator-options="recordQuickFilter.operatorOptions.value"
      :columns="recordColumns"
      :column-saving="recordColumnSaving"
      :show-column-reset="false"
      :total="recordTotal"
      v-model:page="recordQueryParams.pageNo"
      v-model:limit="recordQueryParams.pageSize"
      @update:quick-filter-state="recordQuickFilter.updateState"
      @quick-filter-query="recordQuickFilter.applyQuickFilter"
      @column-change="saveRecordColumnConfig"
      @pagination="getRecordList"
    >
      <template #actions>
        <el-button :loading="recordLoading" @click="getRecordList">刷新</el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert
          v-if="recordLoadError"
          class="codex-test-record-error"
          type="error"
          :closable="false"
          :title="recordLoadError"
        />
        <el-table
          v-loading="recordLoading"
          data-user-table-column-explicit
          data-user-table-key="system.codex-test.records"
          :data="recordList"
          border
          :stripe="true"
          empty-text="暂无测试记录"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleRecordHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRecordColumnVisible('id')"
            label="批次"
            prop="id"
            align="center"
            :width="getRecordColumnWidthString('id', 100)"
            v-bind="sortColumnAttrs('id')"
          />
          <el-table-column
            v-if="isRecordColumnVisible('targetTenant')"
            label="测试租户"
            prop="targetTenantId"
            :width="getRecordColumnWidthString('targetTenant')"
            :min-width="getRecordColumnMinWidthString('targetTenant', 140)"
            v-bind="sortColumnAttrs({ key: 'targetTenant', prop: 'targetTenantId' })"
          >
            <template #default="{ row }">
              {{ formatTenantLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('executionMode')"
            label="方法"
            prop="executionMode"
            align="center"
            :width="getRecordColumnWidthString('executionMode', 120)"
            v-bind="sortColumnAttrs('executionMode')"
          >
            <template #default="{ row }">
              {{ formatExecutionMode(row.executionMode) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('status')"
            label="结果"
            prop="status"
            align="center"
            :width="getRecordColumnWidthString('status', 120)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="executionTagType(row.status)" effect="plain">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('startedAt')"
            label="开始时间"
            prop="startedAt"
            align="center"
            :width="getRecordColumnWidthString('startedAt', 180)"
            v-bind="sortColumnAttrs('startedAt')"
          >
            <template #default="{ row }">
              {{ formatDateTimeValue(row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('finishedAt')"
            label="完成时间"
            prop="finishedAt"
            align="center"
            :width="getRecordColumnWidthString('finishedAt', 180)"
            v-bind="sortColumnAttrs('finishedAt')"
          >
            <template #default="{ row }">
              {{ formatDateTimeValue(row.finishedAt) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('summary')"
            label="摘要"
            prop="summary"
            :width="getRecordColumnWidthString('summary')"
            :min-width="getRecordColumnMinWidthString('summary', 220)"
            v-bind="sortColumnAttrs('summary')"
          >
            <template #default="{ row }">
              {{ row.summary || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('actions')"
            fixed="right"
            label="操作"
            prop="actions"
            align="center"
            :width="getRecordColumnWidthString('actions', 180)"
          >
            <template #default="{ row }">
              <el-button
                v-hasPermi="['system:codex-test:artifact']"
                link
                type="primary"
                @click="openRecord(row.id)"
              >
                查看结果
              </el-button>
              <el-button
                v-if="['PENDING', 'CLAIMED', 'RUNNING'].includes(row.status)"
                v-hasPermi="['system:codex-test:cancel']"
                link
                type="danger"
                @click="cancelRecord(row.id)"
              >
                取消
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-drawer v-model="recordDrawerVisible" size="70%" title="执行结果">
    <template v-if="recordDetail">
      <el-alert
        :closable="false"
        :title="`批次 ${recordDetail.id}：${statusText(recordDetail.status)}`"
        class="mb-12px"
        show-icon
        :type="executionTagType(recordDetail.status)"
      />
      <el-collapse>
        <el-collapse-item
          v-for="caseResult in recordDetail.cases || []"
          :key="caseResult.id"
          :title="`${caseResult.caseNameSnapshot} - ${statusText(caseResult.status)}`"
        >
          <el-descriptions :column="1" border>
            <el-descriptions-item label="测试方法项">
              {{ caseResult.methodTextSnapshot }}
            </el-descriptions-item>
            <el-descriptions-item label="测试数据">
              {{ caseResult.testDataTextSnapshot || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="失败描述">
              {{ caseResult.failureReason || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-table :data="caseResult.checkpointResults" class="mt-12px" stripe>
            <el-table-column label="检查点" min-width="180" prop="checkpointNameSnapshot" />
            <el-table-column label="期待结果" min-width="220" prop="expectedTextSnapshot" />
            <el-table-column label="实际结果" min-width="220" prop="actualText" />
            <el-table-column label="判定" width="110">
              <template #default="{ row }">
                <el-tag :type="checkpointTagType(row.status)" effect="plain">
                  {{
                    row.status === 'PASS'
                      ? '绿色勾通过'
                      : row.status === 'FAIL'
                        ? '红色叉失败'
                        : statusText(row.status)
                  }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="为什么不同" min-width="220" prop="mismatchDescription" />
            <el-table-column label="失败截图" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="row.screenshotArtifactId"
                  v-hasPermi="['system:codex-test:artifact']"
                  link
                  type="primary"
                  @click="previewArtifact(row.screenshotArtifactId)"
                >
                  查看
                </el-button>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
      <el-image v-if="artifactPreviewUrl" class="codex-test-artifact" :src="artifactPreviewUrl" fit="contain" />
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import * as CodexTestApi from '@/api/system/codexTestManagement'
import * as TenantApi from '@/api/system/tenant'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'SystemCodexTestRecord' })

const message = useMessage()

const statusOptions = [
  { label: '待执行', value: 'PENDING' },
  { label: '已领取', value: 'CLAIMED' },
  { label: '执行中', value: 'RUNNING' },
  { label: '通过', value: 'PASS' },
  { label: '失败', value: 'FAIL' },
  { label: '阻塞', value: 'BLOCKED' },
  { label: '已取消', value: 'CANCELED' },
  { label: '超时', value: 'TIMEOUT' }
]

const recordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'id', label: '批次', width: 100 },
  { key: 'targetTenant', label: '测试租户', minWidth: 140, sortProp: 'targetTenantId' },
  { key: 'executionMode', label: '方法', width: 120 },
  { key: 'status', label: '结果', width: 120 },
  { key: 'startedAt', label: '开始时间', width: 180 },
  { key: 'finishedAt', label: '完成时间', width: 180 },
  { key: 'summary', label: '摘要', minWidth: 220 },
  { key: 'actions', label: '操作', width: 180, hideable: false, business: false, sortable: false }
]

const {
  columns: recordColumns,
  saving: recordColumnSaving,
  isColumnVisible: isRecordColumnVisible,
  getColumnWidthString: getRecordColumnWidthString,
  getColumnMinWidthString: getRecordColumnMinWidthString,
  handleHeaderDragend: handleRecordHeaderDragend,
  saveConfig: saveRecordColumnConfig
} = useUserTableColumns('system.codex-test.records', recordDefaultColumns)

const tenantOptions = ref<TenantApi.TenantVO[]>([])
const recordLoading = ref(false)
const recordLoadError = ref('')
const recordList = ref<CodexTestApi.CodexTestExecutionVO[]>([])
const recordTotal = ref(0)
const recordDrawerVisible = ref(false)
const recordDetail = ref<CodexTestApi.CodexTestExecutionVO>()
const artifactPreviewUrl = ref('')

const recordQueryParams = reactive<CodexTestApi.CodexTestExecutionPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  targetTenantId: undefined,
  status: undefined,
  createTime: undefined
})

const recordQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'targetTenantId',
    label: '测试租户',
    type: 'select',
    queryParamKey: 'targetTenantId',
    options: tenantOptions.value.map((tenant) => ({
      label: tenant.name,
      value: tenant.id
    }))
  },
  {
    key: 'status',
    label: '结果',
    type: 'select',
    queryParamKey: 'status',
    options: statusOptions
  },
  {
    key: 'createTime',
    label: '创建时间',
    type: 'dateRange',
    queryParamKey: 'createTime'
  }
])

const recordQuickFilter = useTableQuickFilter(
  'system.codex-test.records',
  recordQuickFilterDefinitions,
  recordQueryParams,
  getRecordList
)

const resolveRequestError = (error: unknown, defaultMessage: string) => {
  const record = error as {
    message?: string
    msg?: string
    response?: { data?: { msg?: string; message?: string } }
  }
  return (
    record?.response?.data?.msg ||
    record?.response?.data?.message ||
    record?.msg ||
    record?.message ||
    defaultMessage
  )
}

const formatTenantLabel = (row: CodexTestApi.CodexTestExecutionVO) =>
  row.targetTenantName || tenantOptions.value.find((tenant) => tenant.id === row.targetTenantId)?.name || row.targetTenantId

const formatExecutionMode = (mode?: string) => {
  if (mode === 'SEQUENTIAL') return '顺序执行'
  if (mode === 'PARALLEL') return '并行执行'
  return mode || '-'
}

function executionTagType(status?: string) {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL' || status === 'TIMEOUT') return 'danger'
  if (status === 'BLOCKED' || status === 'CANCELED') return 'warning'
  return 'info'
}

function checkpointTagType(status?: string) {
  if (status === 'PASS') return 'success'
  if (status === 'FAIL') return 'danger'
  if (status === 'BLOCKED') return 'warning'
  return 'info'
}

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

async function getTenantOptions() {
  try {
    tenantOptions.value = await TenantApi.getTenantList()
    recordQueryParams.targetTenantId = tenantOptions.value[0]?.id
  } catch (error) {
    const errorMessage = resolveRequestError(error, '测试租户加载失败')
    recordLoadError.value = errorMessage
    message.error(errorMessage)
  }
}

async function getRecordList() {
  recordLoading.value = true
  recordLoadError.value = ''
  try {
    const data = await CodexTestApi.getCodexTestExecutionPage(recordQueryParams)
    recordList.value = data.list
    recordTotal.value = data.total
  } catch (error) {
    const errorMessage = resolveRequestError(error, '测试记录加载失败')
    recordLoadError.value = errorMessage
    recordList.value = []
    recordTotal.value = 0
    message.error(errorMessage)
  } finally {
    recordLoading.value = false
  }
}

async function cancelRecord(id: number) {
  try {
    await CodexTestApi.cancelCodexTestExecution(id)
    message.success('已取消执行')
    await getRecordList()
  } catch (error) {
    message.error(resolveRequestError(error, '取消执行失败'))
  }
}

async function openRecord(id: number) {
  try {
    recordDetail.value = await CodexTestApi.getCodexTestExecution(id)
    recordDrawerVisible.value = true
  } catch (error) {
    message.error(resolveRequestError(error, '执行详情加载失败'))
  }
}

async function previewArtifact(id: number) {
  try {
    const data = await CodexTestApi.downloadCodexTestArtifact(id)
    if (artifactPreviewUrl.value) {
      URL.revokeObjectURL(artifactPreviewUrl.value)
    }
    artifactPreviewUrl.value = URL.createObjectURL(new Blob([data as BlobPart]))
  } catch (error) {
    message.error(resolveRequestError(error, '失败截图加载失败'))
  }
}

onMounted(async () => {
  await getTenantOptions()
  await getRecordList()
})
</script>

<style lang="scss" scoped>
.codex-test-record-error {
  margin: 12px;
  width: auto;
}

.codex-test-artifact {
  width: 100%;
  max-height: 480px;
  margin-top: 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
}
</style>
