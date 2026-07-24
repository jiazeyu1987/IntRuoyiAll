<template>
  <Dialog v-model="dialogVisible" title="导入报工" width="560">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      action="#"
      :auto-upload="false"
      :disabled="formLoading"
      :limit="1"
      :on-remove="handleFileRemove"
      :on-change="handleFileChange"
      :on-exceed="handleExceed"
      accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      drag
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击选择</em></div>
    </el-upload>

    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <el-dialog
    v-model="directImportResultVisible"
    title="直接报工导入结果"
    width="min(96vw, 1280px)"
    class="direct-import-result-dialog"
  >
    <div v-if="directImportResult" class="direct-import-result">
      <div v-if="groupedDirectWorkOrders.length" class="direct-import-result__body">
        <div class="direct-import-result__work-orders">
          <button
            v-for="group in groupedDirectWorkOrders"
            :key="group.workOrderCode"
            type="button"
            class="direct-import-result__work-order-card"
            :class="{ 'is-active': group.workOrderCode === selectedWorkOrderCode }"
            @click="selectDirectWorkOrder(group.workOrderCode)"
          >
            <span class="direct-import-result__work-order-title">{{ group.workOrderCode }}</span>
            <span class="direct-import-result__work-order-meta">
              {{ group.processCount }} 道工序 / {{ formatQuantity(group.totalFeedbackQuantity) }} 件
            </span>
            <span class="direct-import-result__work-order-link">更新结果</span>
            <span class="direct-import-result__work-order-tags">
              <el-tag size="small" effect="light">本次 {{ formatQuantity(group.totalFeedbackQuantity) }}</el-tag>
              <el-tag size="small" type="success" effect="light">
                进度 {{ formatProgressDelta(group.totalProgressDeltaPercent) }}
              </el-tag>
              <el-tag
                v-if="group.warnings.length"
                size="small"
                type="warning"
                effect="light"
                :title="formatWorkOrderWarningReasons(group.warnings)"
              >
                未更新 {{ group.warnings.length }}
              </el-tag>
            </span>
          </button>
        </div>

        <div class="direct-import-result__detail-panel">
          <div class="direct-import-result__detail-header">
            <div>
              <div class="direct-import-result__detail-title">
                {{ selectedDirectWorkOrder?.workOrderCode }}
              </div>
              <div class="direct-import-result__sub-text">
                {{ selectedDirectWorkOrder?.productCode }} / {{ selectedDirectWorkOrder?.productName }}
              </div>
            </div>
            <el-tag type="primary" effect="light">
              {{ selectedDirectWorkOrder?.processCount || 0 }} 道工序
            </el-tag>
          </div>

          <el-table
            :data="selectedWorkOrderRows"
            border
            height="408"
            empty-text="本次导入未更新排产进度"
          >
            <el-table-column label="工序 / 产线" min-width="210" fixed>
              <template #default="{ row }">
                <div class="direct-import-result__main-text">{{ row.processName }}</div>
                <div class="direct-import-result__sub-text">{{ row.processCode }}</div>
                <div class="direct-import-result__sub-text">
                  {{ formatWorkstationText(row) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="产品" min-width="210">
              <template #default="{ row }">
                <div class="direct-import-result__main-text">{{ row.productCode }}</div>
                <div class="direct-import-result__sub-text" :title="row.productName">
                  {{ row.productName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="本次完成 / 已报工变化" min-width="220" align="right">
              <template #default="{ row }">
                <div v-if="row.resultType === 'SKIPPED'" class="direct-import-result__main-text">
                  本次报工 {{ formatQuantity(row.feedbackQuantity) }}
                </div>
                <div v-else class="direct-import-result__main-text">
                  本次完成 {{ formatQuantity(row.feedbackQuantity) }}
                </div>
                <div v-if="row.resultType === 'SKIPPED'" class="direct-import-result__sub-text">
                  已报 {{ formatQuantity(row.reportedQuantity) }} / 剩余 {{ formatQuantity(row.remainingQuantity) }}
                </div>
                <div v-else class="direct-import-result__sub-text">
                  已报工 {{ formatQuantity(row.beforeReportedQuantity) }} ->
                  {{ formatQuantity(row.afterReportedQuantity) }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="进度增加 / 来源行" min-width="190" align="right">
              <template #default="{ row }">
                <el-tag v-if="row.resultType === 'SKIPPED'" type="warning" effect="light">
                  未更新
                </el-tag>
                <el-tag
                  v-else
                  :type="isDirectWorkReportOverRemaining(row) ? 'warning' : 'success'"
                  effect="light"
                >
                  已更新 {{ formatProgressDelta(row.progressDeltaPercent) }}
                </el-tag>
                <div v-if="row.resultType === 'SKIPPED'" class="direct-import-result__sub-text">
                  当前进度 {{ formatPercent(row.progressPercent) }}
                </div>
                <div v-else class="direct-import-result__sub-text">
                  {{ formatPercent(row.beforeProgressPercent) }} -> {{ formatPercent(row.afterProgressPercent) }}
                </div>
                <div
                  v-if="isDirectWorkReportOverRemaining(row)"
                  class="direct-import-result__warning-text"
                  :title="row.resultMessage"
                >
                  {{ row.resultMessage || '本次报工数量超过当前剩余数量，已更新排产进度。' }}
                </div>
                <div class="direct-import-result__sub-text">
                  {{ row.resultType === 'SKIPPED' ? row.reason : formatImportRecordSource(row) }}
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <el-empty v-else description="本次导入未更新排产进度" />
    </div>
    <template #footer>
      <el-button type="primary" @click="directImportResultVisible = false">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ProFeedbackApi, type ThirdPartyFeedbackImportResultVO } from '@/api/mes/pro/feedback'
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus'

defineOptions({ name: 'ThirdPartyFeedbackImportForm' })

const message = useMessage()
const emits = defineEmits(['success'])
type ImportMode = 'THIRD_PARTY' | 'DIRECT_WORK_REPORT'
type DirectWorkReportDetail = NonNullable<ThirdPartyFeedbackImportResultVO['directWorkReportDetails']>[number]
type DirectWorkReportSkipWarning = NonNullable<ThirdPartyFeedbackImportResultVO['directWorkReportSkipWarnings']>[number]

interface DirectWorkOrderGroup {
  workOrderCode: string
  productCode?: string
  productName?: string
  processCount: number
  totalFeedbackQuantity: number
  totalProgressDeltaPercent: number
  details: DirectWorkReportDetail[]
  warnings: DirectWorkReportSkipWarning[]
}

type DirectWorkReportResultRow =
  | (DirectWorkReportDetail & { resultType: 'UPDATED' })
  | (DirectWorkReportSkipWarning & { resultType: 'SKIPPED' })

const NON_INTERSECTION_DIRECT_WORK_REPORT_WARNING_CODES = new Set([
  'WORK_ORDER_NOT_FOUND',
  'WORK_ORDER_NOT_UNIQUE',
  'SCHEDULE_ORDER_NOT_FOUND',
  'SCHEDULE_ORDER_NOT_UNIQUE',
  'PROCESS_NOT_FOUND',
  'PROCESS_NOT_ENABLED',
  'PROCESS_NOT_UNIQUE'
])
const DIRECT_WORK_REPORT_OVER_REMAINING_CODE = 'OVER_REMAINING_QUANTITY'

const DIRECT_WORK_REPORT_HEADERS = [
  '任务单',
  '生产订单',
  '产品代码',
  '产品名称',
  '工序编码',
  '工序名称',
  '部门',
  '人员工号',
  '人员名称',
  '工段长',
  '日期',
  '工序单价',
  '总产出',
  '总金额'
]

const THIRD_PARTY_HEADERS = [
  '报工日期',
  '报工人编码',
  '报工人名称',
  '工段长',
  '生产订单号',
  '生产资源组',
  '生产资源',
  '派工单号',
  '产品编码',
  '产品名称',
  '规格',
  '模具编码',
  '工序编码',
  '工序名称',
  '所属部门',
  '报工数量',
  '支数',
  '公斤数',
  '实腔数',
  '全程时间',
  '生产定额',
  '工作时长',
  '注塑合模/组装公斤数',
  '注塑个数/组装个重',
  '操作'
]

const IMPORT_MODE_LABELS: Record<ImportMode, string> = {
  THIRD_PARTY: '第三方报工待归属',
  DIRECT_WORK_REPORT: '李萍报工单'
}

const dialogVisible = ref(false)
const formLoading = ref(false)
const uploadRef = ref()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const importMode = ref<ImportMode>('DIRECT_WORK_REPORT')
const detectedImportMode = ref<ImportMode | null>(null)
const directImportResultVisible = ref(false)
const directImportResult = ref<ThirdPartyFeedbackImportResultVO | null>(null)
const selectedWorkOrderCode = ref('')

const open = () => {
  dialogVisible.value = true
  resetForm()
}

defineExpose({ open })

const submitForm = async () => {
  if (!selectedFile.value) {
    message.error('请上传 Excel 文件')
    return
  }
  if (detectedImportMode.value && detectedImportMode.value !== importMode.value) {
    message.error(
      `当前文件表头为“${IMPORT_MODE_LABELS[detectedImportMode.value]}”，请切换为该导入类型后再提交。`
    )
    return
  }
  try {
    formLoading.value = true
    const uploadFormData = new FormData()
    uploadFormData.append('file', selectedFile.value)
    const result =
      importMode.value === 'DIRECT_WORK_REPORT'
        ? await ProFeedbackApi.importDirectWorkReportXlsx(uploadFormData)
        : await ProFeedbackApi.importThirdPartyXlsx(uploadFormData)
    const importRecordIdsText = result.importRecordIds?.length ? result.importRecordIds.join('、') : '无'
    if (importMode.value === 'DIRECT_WORK_REPORT') {
      directImportResult.value = result
      directImportResultVisible.value = true
    } else {
      message.alert(
        `导入完成；工作表数：${result.sheetCount}；导入条数：${result.importedCount}；待归属条数：${result.pendingCount}；记录编号：${importRecordIdsText}`
      )
    }
    dialogVisible.value = false
    emits('success', result, importMode.value === 'DIRECT_WORK_REPORT' ? '李萍报工单' : '第三方导入')
  } finally {
    formLoading.value = false
  }
}

const normalizeHeader = (value: unknown) => String(value ?? '').trim()

const formatNumber = (value: unknown, fractionDigits: number) => {
  const numericValue = Number(value ?? 0)
  if (!Number.isFinite(numericValue)) {
    return '0'
  }
  return numericValue.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: fractionDigits
  })
}

const formatQuantity = (value: unknown) => formatNumber(value, 3)

const clampProgressPercent = (value: unknown) => {
  const numericValue = Number(value ?? 0)
  if (!Number.isFinite(numericValue)) {
    return 0
  }
  return Math.min(Math.max(numericValue, 0), 100)
}

const formatPercent = (value: unknown) => `${formatNumber(clampProgressPercent(value), 1)}%`

const formatProgressDelta = (value: unknown) => {
  const numericValue = clampProgressPercent(value)
  const prefix = numericValue > 0 ? '+' : ''
  return `${prefix}${formatNumber(numericValue, 1)}%`
}

const formatWorkOrderWarningReasons = (warnings: DirectWorkReportSkipWarning[]) =>
  warnings
    .map((warning) => {
      const source = [warning.sheetName, warning.rowNo ? `#${warning.rowNo}` : '']
        .filter(Boolean)
        .join(' ')
      return [source, warning.reason || '未更新'].filter(Boolean).join('：')
    })
    .filter(Boolean)
    .join('\n')

const formatWorkstationText = (row: DirectWorkReportResultRow) => {
  if (row.resultType === 'SKIPPED') {
    return row.scheduleOrderCode || '未更新排产进度'
  }
  return [row.workstationName, row.workstationCode].filter(Boolean).join(' / ') || '-'
}

const formatImportRecordSource = (row: DirectWorkReportDetail) => {
  const source = [row.sheetName, row.rowNo ? `#${row.rowNo}` : ''].filter(Boolean).join(' ')
  if (source) {
    return source
  }
  return row.importRecordId ? `来源记录 ${row.importRecordId}` : row.resultMessage || '已更新排产进度'
}

const hasDirectWorkReportText = (value: unknown) => String(value ?? '').trim().length > 0

const isDirectWorkReportDisplayableDetail = (detail: DirectWorkReportDetail) =>
  hasDirectWorkReportText(detail.workOrderCode) && hasDirectWorkReportText(detail.scheduleOrderCode)

const isDirectWorkReportDisplayableWarning = (warning: DirectWorkReportSkipWarning) => {
  if (
    !hasDirectWorkReportText(warning.workOrderCode) ||
    !hasDirectWorkReportText(warning.scheduleOrderCode)
  ) {
    return false
  }
  const reasonCode = String(warning.reasonCode ?? '').trim()
  if (NON_INTERSECTION_DIRECT_WORK_REPORT_WARNING_CODES.has(reasonCode)) {
    return false
  }
  return hasDirectWorkReportText(warning.processCode)
}

const isDirectWorkReportOverRemaining = (row: DirectWorkReportResultRow) =>
  row.resultType === 'UPDATED' &&
  String(row.resultCode ?? '').trim() === DIRECT_WORK_REPORT_OVER_REMAINING_CODE

const getVisibleDirectWorkReportDetails = () =>
  (directImportResult.value?.directWorkReportDetails || []).filter(isDirectWorkReportDisplayableDetail)

const getVisibleDirectWorkReportWarnings = () =>
  (directImportResult.value?.directWorkReportSkipWarnings || []).filter(
    isDirectWorkReportDisplayableWarning
  )

const sumNumeric = (value: unknown) => {
  const numericValue = Number(value ?? 0)
  return Number.isFinite(numericValue) ? numericValue : 0
}

const groupedDirectWorkOrders = computed<DirectWorkOrderGroup[]>(() => {
  const details = getVisibleDirectWorkReportDetails()
  const warnings = getVisibleDirectWorkReportWarnings()
  const groupMap = new Map<string, DirectWorkOrderGroup>()
  const ensureDirectWorkOrderGroup = (
    workOrderCode: string,
    productCode?: string,
    productName?: string
  ) => {
    let group = groupMap.get(workOrderCode)
    if (!group) {
      group = {
        workOrderCode,
        productCode,
        productName,
        processCount: 0,
        totalFeedbackQuantity: 0,
        totalProgressDeltaPercent: 0,
        details: [],
        warnings: []
      }
      groupMap.set(workOrderCode, group)
      return group
    }
    if (!group.productCode && productCode) {
      group.productCode = productCode
    }
    if (!group.productName && productName) {
      group.productName = productName
    }
    return group
  }
  for (const detail of details) {
    const workOrderCode = String(detail.workOrderCode || '').trim()
    const group = ensureDirectWorkOrderGroup(workOrderCode, detail.productCode, detail.productName)
    group.details.push(detail)
    group.totalFeedbackQuantity += sumNumeric(detail.feedbackQuantity)
    group.totalProgressDeltaPercent += sumNumeric(detail.progressDeltaPercent)
  }
  for (const warning of warnings) {
    const workOrderCode = String(warning.workOrderCode || '').trim()
    if (!workOrderCode) {
      continue
    }
    const group = ensureDirectWorkOrderGroup(workOrderCode, warning.productCode, warning.productName)
    group.warnings.push(warning)
    group.totalFeedbackQuantity += sumNumeric(warning.feedbackQuantity)
  }
  for (const group of groupMap.values()) {
    group.processCount = group.details.length + group.warnings.length
  }
  return Array.from(groupMap.values())
})

const selectedDirectWorkOrder = computed(() =>
  groupedDirectWorkOrders.value.find((group) => group.workOrderCode === selectedWorkOrderCode.value)
)

const selectedWorkOrderRows = computed<DirectWorkReportResultRow[]>(() => {
  const group = selectedDirectWorkOrder.value
  if (!group) {
    return []
  }
  return [
    ...group.details.map((detail) => ({
      ...detail,
      resultType: 'UPDATED' as const
    })),
    ...group.warnings.map((warning) => ({
      ...warning,
      resultType: 'SKIPPED' as const
    }))
  ]
})

const selectDirectWorkOrder = (workOrderCode: string) => {
  selectedWorkOrderCode.value = workOrderCode
}

watch(
  groupedDirectWorkOrders,
  (groups) => {
    if (!groups.length) {
      selectedWorkOrderCode.value = ''
      return
    }
    if (!groups.some((group) => group.workOrderCode === selectedWorkOrderCode.value)) {
      selectedWorkOrderCode.value = groups[0].workOrderCode
    }
  },
  { immediate: true }
)

const isSameHeaders = (actualHeaders: string[], expectedHeaders: string[]) =>
  expectedHeaders.every((expectedHeader, index) => actualHeaders[index] === expectedHeader)

const detectImportModeByHeaders = (headers: string[]): ImportMode | null => {
  if (isSameHeaders(headers, DIRECT_WORK_REPORT_HEADERS)) {
    return 'DIRECT_WORK_REPORT'
  }
  if (isSameHeaders(headers, THIRD_PARTY_HEADERS)) {
    return 'THIRD_PARTY'
  }
  return null
}

const readWorkbookHeaders = async (file: File): Promise<string[]> => {
  const { read, utils } = await import('xlsx')
  const workbook = read(await file.arrayBuffer(), { type: 'array' })
  const firstSheetName = workbook.SheetNames[0]
  if (!firstSheetName) {
    return []
  }
  const rows = utils.sheet_to_json<unknown[]>(workbook.Sheets[firstSheetName], {
    header: 1,
    blankrows: false
  })
  const firstRow = rows[0] ?? []
  return firstRow.map(normalizeHeader)
}

const handleFileChange = async (file: UploadFile, files: UploadFiles) => {
  if (file.name && !/\.xlsx$/i.test(file.name)) {
    message.error('仅允许上传 .xlsx 文件')
    selectedFile.value = null
    detectedImportMode.value = null
    fileList.value = []
    nextTick(() => uploadRef.value?.clearFiles())
    return
  }
  fileList.value = files.slice(-1) as UploadUserFile[]
  selectedFile.value = (file.raw as File) || null
  detectedImportMode.value = null
  if (!selectedFile.value) {
    return
  }
  try {
    const detectedMode = detectImportModeByHeaders(await readWorkbookHeaders(selectedFile.value))
    detectedImportMode.value = detectedMode
    if (!detectedMode) {
      message.warning('未识别 Excel 表头，请确认导入类型后再提交。')
      return
    }
    if (importMode.value !== detectedMode) {
      importMode.value = detectedMode
      message.warning(`检测到“${IMPORT_MODE_LABELS[detectedMode]}”表头，已自动切换导入类型。`)
    }
  } catch (error) {
    selectedFile.value = null
    detectedImportMode.value = null
    fileList.value = []
    nextTick(() => uploadRef.value?.clearFiles())
    message.error('读取 Excel 表头失败，请重新选择 .xlsx 文件')
    throw error
  }
}

const handleFileRemove = () => {
  selectedFile.value = null
  detectedImportMode.value = null
}

const handleExceed = () => {
  message.error('最多只能上传一个文件！')
}

const resetForm = async () => {
  formLoading.value = false
  selectedFile.value = null
  detectedImportMode.value = null
  importMode.value = 'DIRECT_WORK_REPORT'
  directImportResultVisible.value = false
  directImportResult.value = null
  selectedWorkOrderCode.value = ''
  fileList.value = []
  await nextTick()
  uploadRef.value?.clearFiles()
}
</script>

<style scoped>
.direct-import-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.direct-import-result__body {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 12px;
  height: 460px;
}

.direct-import-result__work-orders {
  height: 100%;
  min-height: 0;
  padding: 6px;
  overflow-y: auto;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f7f9fc;
}

.direct-import-result__work-order-card {
  display: flex;
  width: 100%;
  min-height: 108px;
  flex-direction: column;
  align-items: stretch;
  margin-bottom: 8px;
  padding: 12px;
  cursor: pointer;
  text-align: left;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  color: #263247;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.direct-import-result__work-order-card:hover,
.direct-import-result__work-order-card.is-active {
  border-color: #1677ff;
  background: #fafcff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.12);
}

.direct-import-result__work-order-title {
  color: #1677ff;
  font-weight: 700;
  line-height: 20px;
}

.direct-import-result__work-order-meta {
  margin-top: 6px;
  color: #263247;
  font-size: 13px;
  line-height: 18px;
}

.direct-import-result__work-order-link {
  margin-top: 4px;
  color: #1677ff;
  font-weight: 600;
  font-size: 13px;
  line-height: 18px;
  text-align: center;
}

.direct-import-result__work-order-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.direct-import-result__detail-panel {
  min-width: 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  overflow: hidden;
}

.direct-import-result__detail-panel :deep(.el-table .cell) {
  padding: 7px 10px;
}

.direct-import-result__detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 8px 12px;
  border-bottom: 1px solid #edf1f6;
  background: #fafcff;
}

.direct-import-result__detail-title {
  color: #172033;
  font-weight: 700;
  line-height: 20px;
}

.direct-import-result__main-text {
  color: #1f2937;
  line-height: 20px;
}

.direct-import-result__sub-text {
  color: #667085;
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.direct-import-result__warning-text {
  color: #d97706;
  font-size: 12px;
  line-height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
