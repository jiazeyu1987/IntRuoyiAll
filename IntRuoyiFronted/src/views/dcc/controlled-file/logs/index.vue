<template>
  <ContentWrap>
    <UnifiedListTemplate
      table-key="dcc.controlledFile.logs"
      :query-model="queryParams"
      label-width="82px"
      query-form-test-id="dcc-controlled-file-logs-filter-form"
      :filter-definitions="logQuickFilterDefinitions"
      :quick-filter-state="logQuickFilter.state"
      :selected-filter-definition="logQuickFilter.selectedDefinition.value"
      :operator-options="logQuickFilter.operatorOptions.value"
      :columns="logColumns"
      :column-saving="logColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="logQuickFilter.updateState"
      @quick-filter-query="logQuickFilter.applyQuickFilter"
      @column-change="saveLogColumnConfig"
      @pagination="getList"
    >
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-alert
          v-if="loadError"
          class="logs-load-error"
          type="error"
          :closable="false"
          :title="loadError"
        />
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="dcc.controlledFile.logs"
          :data="list"
          border
          :stripe="true"
          :empty-text="controlledFileLogEmptyText"
          :show-overflow-tooltip="true"
          row-key="id"
          @header-dragend="handleLogHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isLogColumnVisible('occurredAt')"
            label="发生时间"
            prop="occurredAt"
            align="center"
            :formatter="dateFormatter"
            :width="getLogColumnWidthString('occurredAt', 180)"
            v-bind="sortColumnAttrs('occurredAt')"
          />
          <el-table-column
            v-if="isLogColumnVisible('logType')"
            label="日志类型"
            prop="logType"
            align="center"
            :width="getLogColumnWidthString('logType', 120)"
            v-bind="sortColumnAttrs('logType')"
          >
            <template #default="{ row }">
              <el-tag>{{ getLogTypeLabel(row.logType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('actionLabel')"
            label="动作"
            prop="actionLabel"
            min-width="130"
            :width="getLogColumnWidthString('actionLabel')"
            v-bind="sortColumnAttrs('actionLabel')"
          >
            <template #default="{ row }">
              {{ displayText(row.actionLabel) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('fileNumber')"
            label="文件编号"
            prop="fileNumber"
            min-width="150"
            :width="getLogColumnWidthString('fileNumber')"
            v-bind="sortColumnAttrs('fileNumber')"
          >
            <template #default="{ row }">
              {{ displayText(row.fileNumber) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('fileName')"
            label="文件名称"
            prop="fileName"
            min-width="220"
            :width="getLogColumnWidthString('fileName')"
            v-bind="sortColumnAttrs('fileName')"
          >
            <template #default="{ row }">
              {{ displayText(row.fileName) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('operatorName')"
            label="操作人"
            prop="operatorName"
            min-width="120"
            :width="getLogColumnWidthString('operatorName')"
            v-bind="sortColumnAttrs('operatorName')"
          >
            <template #default="{ row }">
              {{ displayOperator(row) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('relatedObject')"
            label="关联对象"
            prop="relatedObject"
            min-width="150"
            :width="getLogColumnWidthString('relatedObject')"
            v-bind="sortColumnAttrs('relatedObject')"
          >
            <template #default="{ row }">
              {{ displayText(row.relatedObject) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('summary')"
            label="摘要"
            prop="summary"
            min-width="260"
            :width="getLogColumnWidthString('summary')"
            v-bind="sortColumnAttrs('summary')"
          >
            <template #default="{ row }">
              {{ displayText(row.summary) }}
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('resultLabel')"
            label="结果"
            prop="resultLabel"
            align="center"
            :width="getLogColumnWidthString('resultLabel', 110)"
            v-bind="sortColumnAttrs('resultLabel')"
          >
            <template #default="{ row }">
              <el-tag :type="getResultTagType(row.resultLabel)">
                {{ displayText(row.resultLabel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isLogColumnVisible('actions')"
            label="操作"
            prop="actions"
            align="center"
            fixed="right"
            :width="getLogColumnWidthString('actions', 96)"
          >
            <template #default="{ row }">
              <el-button link type="primary" @click="openLogDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <Dialog v-model="detailVisible" title="文控日志详情" width="760px">
    <el-descriptions
      v-if="currentLog"
      data-testid="dcc-controlled-file-log-detail"
      :column="2"
      border
    >
      <el-descriptions-item label="发生时间">
        {{ currentLog.occurredAt ? dateFormatter(currentLog, undefined as any, currentLog.occurredAt) : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="日志类型">
        {{ getLogTypeLabel(currentLog.logType) }}
      </el-descriptions-item>
      <el-descriptions-item label="动作">
        {{ displayText(currentLog.actionLabel) }}
      </el-descriptions-item>
      <el-descriptions-item label="结果">
        {{ displayText(currentLog.resultLabel) }}
      </el-descriptions-item>
      <el-descriptions-item label="文件编号">
        {{ displayText(currentLog.fileNumber) }}
      </el-descriptions-item>
      <el-descriptions-item label="版本">
        {{ displayText(currentLog.versionNo) }}
      </el-descriptions-item>
      <el-descriptions-item label="操作人">
        {{ displayOperator(currentLog) }}
      </el-descriptions-item>
      <el-descriptions-item label="关联对象">
        {{ displayText(currentLog.relatedObject) }}
      </el-descriptions-item>
      <el-descriptions-item label="文件名称" :span="2">
        {{ displayText(currentLog.fileName) }}
      </el-descriptions-item>
      <el-descriptions-item label="摘要" :span="2">
        {{ displayText(currentLog.summary) }}
      </el-descriptions-item>
      <el-descriptions-item label="旧值" :span="2">
        {{ displayText(currentLog.oldValueText) }}
      </el-descriptions-item>
      <el-descriptions-item label="新值" :span="2">
        {{ displayText(currentLog.newValueText) }}
      </el-descriptions-item>
      <el-descriptions-item label="原因" :span="2">
        {{ displayText(currentLog.reason) }}
      </el-descriptions-item>
      <el-descriptions-item label="详情 JSON" :span="2">
        <pre class="log-detail-json">{{ displayText(currentLog.detailJson) }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
import {
  getControlledFileLogPage,
  type DccControlledFileLogPageReqVO,
  type DccControlledFileLogRespVO
} from '@/api/dcc/controlledFile/logs'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { dateFormatter } from '@/utils/formatTime'

defineOptions({ name: 'DccControlledFileLogs' })

const route = useRoute()
const message = useMessage()

const logTypeOptions = [
  { label: '访问', value: 'CONTROLLED_FILE_AUDIT' },
  { label: '提交', value: 'FILE_SUBMISSION' },
  { label: '审批', value: 'FILE_APPROVAL' },
  { label: '放行', value: 'FILE_RELEASE' },
  { label: '分发', value: 'FILE_DISTRIBUTION' },
  { label: '升版', value: 'FILE_REVISION' },
  { label: '作废', value: 'FILE_OBSOLETE' },
  { label: '修正任务', value: 'PROJECT_CODE_ASSIGNMENT' },
  { label: '修正追溯', value: 'PROJECT_CODE_CHANGE' },
  { label: '培训', value: 'TRAINING_EXECUTION' }
]

const logTypeLabelMap = new Map(logTypeOptions.map((item) => [item.value, item.label]))

const logDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'occurredAt', label: '发生时间', width: 180 },
  { key: 'logType', label: '日志类型', width: 120 },
  { key: 'actionLabel', label: '动作', minWidth: 130 },
  { key: 'fileNumber', label: '文件编号', minWidth: 150 },
  { key: 'fileName', label: '文件名称', minWidth: 220 },
  { key: 'operatorName', label: '操作人', minWidth: 120 },
  { key: 'relatedObject', label: '关联对象', minWidth: 150 },
  { key: 'summary', label: '摘要', minWidth: 260 },
  { key: 'resultLabel', label: '结果', width: 110 },
  { key: 'actions', label: '操作', width: 96, hideable: false, business: false, sortable: false }
]

const {
  columns: logColumns,
  saving: logColumnSaving,
  isColumnVisible: isLogColumnVisible,
  getColumnWidthString: getLogColumnWidthString,
  handleHeaderDragend: handleLogHeaderDragend,
  saveConfig: saveLogColumnConfig
} = useUserTableColumns('dcc.controlledFile.logs', logDefaultColumns)

const getFirstQueryValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return value[0] == null ? undefined : String(value[0])
  }
  return value == null ? undefined : String(value)
}

const getNumberQueryValue = (value: unknown) => {
  const text = getFirstQueryValue(value)
  if (!text) return undefined
  const numberValue = Number(text)
  return Number.isFinite(numberValue) ? numberValue : undefined
}

const queryParams = reactive<DccControlledFileLogPageReqVO & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 10,
  logType: getFirstQueryValue(route.query.logType),
  projectCodeId: getNumberQueryValue(route.query.projectCodeId),
  assignmentId: getNumberQueryValue(route.query.assignmentId),
  controlledFileId: getNumberQueryValue(route.query.controlledFileId)
})

const loading = ref(false)
const total = ref(0)
const list = ref<DccControlledFileLogRespVO[]>([])
const loadError = ref('')
const detailVisible = ref(false)
const currentLog = ref<DccControlledFileLogRespVO>()

const controlledFileLogEmptyText = computed(() =>
  queryParams.controlledFileId
    ? '暂无操作日志，签核证据请见签核追溯/生命周期。'
    : '当前暂无文控日志'
)

const logQuickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'keyword',
    label: '关键字',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '输入文件、人员、摘要'
  },
  {
    key: 'logType',
    label: '日志类型',
    type: 'select',
    queryParamKey: 'logType',
    options: logTypeOptions
  },
  {
    key: 'actionType',
    label: '动作',
    type: 'text',
    queryParamKey: 'actionType',
    placeholder: '输入动作编码'
  },
  {
    key: 'result',
    label: '结果',
    type: 'text',
    queryParamKey: 'result',
    placeholder: '输入结果编码'
  }
])

const logQuickFilter = useTableQuickFilter(
  'dcc.controlledFile.logs',
  logQuickFilterDefinitions,
  queryParams,
  async () => {
    await getList()
  }
)

const displayText = (value?: string | number | null) => {
  if (value === undefined || value === null || value === '') {
    return '-'
  }
  return String(value)
}

const displayOperator = (row: DccControlledFileLogRespVO) =>
  row.operatorName || (row.operatorUserId == null ? '-' : `用户#${row.operatorUserId}`)

const getLogTypeLabel = (logType?: string | null) => {
  if (!logType) return '-'
  return logTypeLabelMap.get(logType) || logType
}

const getResultTagType = (resultLabel?: string | null) => {
  const label = resultLabel || ''
  if (/成功|允许|生效|已确认|已阅读|已发送|已审批/.test(label)) {
    return 'success'
  }
  if (/失败|拒绝|驳回|撤回|作废|过期/.test(label)) {
    return 'danger'
  }
  if (/待|进行|评审|审批/.test(label)) {
    return 'warning'
  }
  return 'info'
}

const resolveLogPageErrorMessage = (error: unknown) => {
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
    '文控日志加载失败，请查看网络或后端错误后重试。'
  )
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getControlledFileLogPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    const errorMessage = resolveLogPageErrorMessage(error)
    loadError.value = errorMessage
    list.value = []
    total.value = 0
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const openLogDetail = (row: DccControlledFileLogRespVO) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.logs-load-error {
  margin: 12px;
  width: auto;
}

.log-detail-json {
  max-height: 180px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
