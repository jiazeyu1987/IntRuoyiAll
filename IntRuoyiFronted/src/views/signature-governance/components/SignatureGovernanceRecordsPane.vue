<template>
  <section class="signature-governance-records">
    <el-alert
      v-if="recordError"
      :closable="false"
      show-icon
      type="error"
      :title="recordError"
    />

    <UnifiedListTemplate
      class="signature-governance-records__list"
      table-key="signature.governance.records"
      :query-model="queryParams"
      :filter-definitions="recordQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="recordQuickFilter.state"
      :selected-filter-definition="recordQuickFilter.selectedDefinition.value"
      :operator-options="recordQuickFilter.operatorOptions.value"
      :columns="recordColumns"
      :column-saving="recordColumnSaving"
      :show-column-reset="false"
      :total="recordTotal"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="recordQuickFilter.updateState"
      @quick-filter-query="recordQuickFilter.applyQuickFilter"
      @column-change="saveRecordColumnConfig"
      @pagination="loadRecordPage"
    >
      <template #actions>
        <el-button :loading="recordLoading" @click="resetRecordFilters">重置</el-button>
        <el-button type="primary" :loading="recordLoading" @click="loadRecordPage">查询</el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="recordLoading"
          data-user-table-column-explicit
          data-user-table-key="signature.governance.records"
          :data="recordList"
          :empty-text="recordError || '暂无签名记录'"
          :show-overflow-tooltip="true"
          @header-dragend="handleRecordHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isRecordColumnVisible('source')"
            label="来源"
            prop="sourceLabel"
            align="center"
            :width="getRecordColumnWidthString('source', 100)"
            v-bind="sortColumnAttrs('sourceLabel')"
          >
            <template #default="{ row }">
              <el-tag size="small" :type="sourceTagType(row.sourceCode)">
                {{ formatSourceLabel(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('businessRecord')"
            label="业务记录"
            prop="businessRecordCode"
            :min-width="getRecordColumnMinWidthString('businessRecord', 220)"
            v-bind="sortColumnAttrs('businessRecordCode')"
          >
            <template #default="{ row }">
              <div class="signature-governance-records__main">
                {{ row.businessRecordCode || row.globalId }}
              </div>
              <div class="signature-governance-records__meta">
                {{ formatBusinessRecordName(row) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('signer')"
            label="签名人"
            prop="signerName"
            :min-width="getRecordColumnMinWidthString('signer', 160)"
            v-bind="sortColumnAttrs('signerName')"
          >
            <template #default="{ row }">
              <div>{{ row.signerName || row.actorNicknameSnapshot || `用户#${row.signerUserId || '-'}` }}</div>
              <div class="signature-governance-records__meta">
                {{ row.actorUsernameSnapshot || '-' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('actorDeptPost')"
            label="部门/岗位"
            prop="actorDeptPost"
            :min-width="getRecordColumnMinWidthString('actorDeptPost', 180)"
            v-bind="sortColumnAttrs('actorDeptPost')"
          >
            <template #default="{ row }">
              <div>{{ formatValue(row.actorDeptNameSnapshot) }}</div>
              <div class="signature-governance-records__meta">
                {{ formatValue(row.actorPostNamesSnapshot) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('actorRoleNamesSnapshot')"
            label="角色"
            prop="actorRoleNamesSnapshot"
            :min-width="getRecordColumnMinWidthString('actorRoleNamesSnapshot', 150)"
            v-bind="sortColumnAttrs('actorRoleNamesSnapshot')"
          >
            <template #default="{ row }">{{ formatValue(row.actorRoleNamesSnapshot) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('signatureSummary')"
            label="签名摘要"
            prop="signatureSummary"
            :min-width="getRecordColumnMinWidthString('signatureSummary', 240)"
            v-bind="sortColumnAttrs('signatureSummary')"
          >
            <template #default="{ row }">
              <div class="signature-governance-records__summary">
                <div>
                  <el-tag size="small" type="primary">
                    {{ formatSignatureMeaningText(row) }}
                  </el-tag>
                  <span class="signature-governance-records__summary-text">
                    {{ formatSignatureSummaryText(row) }}
                  </span>
                </div>
                <div class="signature-governance-records__meta">
                  来源表：{{ formatSourceTableLabel(row.sourceTable) }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('evidenceSummary')"
            label="证据摘要"
            prop="evidenceSummary"
            :min-width="getRecordColumnMinWidthString('evidenceSummary', 220)"
            v-bind="sortColumnAttrs('evidenceSummary')"
          >
            <template #default="{ row }">
              <div class="signature-governance-records__summary">
                <div>
                  <span class="signature-governance-records__label">状态</span>
                  <el-tag size="small" :type="evidenceTagType(row.evidenceStatus)">
                    {{ formatEvidenceStatusText(row.evidenceStatus) }}
                  </el-tag>
                </div>
                <div class="signature-governance-records__hash">
                  {{ formatHash(row.evidenceHash) }}
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('signedAt')"
            label="签名时间"
            prop="signedAt"
            align="center"
            :width="getRecordColumnWidthString('signedAt', 180)"
            v-bind="sortColumnAttrs('signedAt')"
          >
            <template #default="{ row }">{{ formatRecordSignedAt(row.signedAt) }}</template>
          </el-table-column>
          <el-table-column
            v-if="isRecordColumnVisible('operation')"
            label="操作"
            prop="operation"
            align="center"
            fixed="right"
            :width="getRecordColumnWidthString('operation', 210)"
          >
            <template #default="{ row }">
              <div class="signature-governance-records__actions">
                <el-button
                  link
                  type="primary"
                  :loading="isPreviewingRecord(row.globalId)"
                  @click="openRecordPdfPreview(row)"
                >
                  预览
                </el-button>
                <el-button
                  link
                  type="primary"
                  :loading="isDownloadingRecord(row.globalId)"
                  @click="handleRecordPdfDownload(row)"
                >
                  PDF
                </el-button>
                <el-button link type="primary" @click="openRecordDetail(row)">详情</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </section>

  <el-dialog
    v-model="signatureRecordPdfPreviewDialog.visible"
    :title="signatureRecordPdfPreviewDialog.fileName || '签名证据 PDF'"
    width="920px"
    destroy-on-close
    @closed="clearRecordPdfPreview"
  >
    <el-alert
      v-if="signatureRecordPdfPreviewDialog.inlineError"
      :closable="false"
      class="mb-16px"
      show-icon
      type="error"
      :title="signatureRecordPdfPreviewDialog.inlineError"
    />
    <div v-loading="signatureRecordPdfPreviewDialog.loading" class="signature-record-pdf-preview">
      <iframe
        v-if="signatureRecordPdfPreviewDialog.objectUrl"
        :src="signatureRecordPdfPreviewDialog.objectUrl"
        class="signature-record-pdf-preview__frame"
        title="签名证据 PDF 预览"
      ></iframe>
      <el-empty
        v-else-if="
          !signatureRecordPdfPreviewDialog.loading && !signatureRecordPdfPreviewDialog.inlineError
        "
        description="暂无可预览 PDF"
      />
    </div>
    <template #footer>
      <el-button @click="signatureRecordPdfPreviewDialog.visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  downloadSignatureGovernanceRecordPdf,
  fetchSignatureGovernanceRecordPdfArtifact,
  getMySignatureGovernanceRecordPage,
  getSignatureGovernanceRecordPage,
  type SignatureGovernanceRecordPageReqVO,
  type SignatureGovernanceRecordRespVO,
  type SignatureGovernanceRecordSourceCode
} from '@/api/signature-governance/records'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'SignatureGovernanceRecordsPane' })

const tableKey = 'signature.governance.records'
const message = useMessage()
const router = useRouter()

const sourceOptions = [
  { label: '文件', value: 'FILE' },
  { label: '批记录', value: 'BATCH_RECORD' },
  { label: '展厅', value: 'SHOWROOM' },
  { label: '审批', value: 'BPM' },
  { label: '报工审批', value: 'MES_FEEDBACK' },
  { label: '排产', value: 'SCHEDULING' },
  { label: '文控', value: 'DOCUMENT_CONTROL' }
] as const

const sourceTableLabels: Record<string, string> = {
  dcc_controlled_file_signature: '受控文件签名记录',
  mes_pro_batch_record_execution_signature: '批记录执行签名记录',
  showroom_change_request_signature: '展厅变更签名记录',
  bpm_approval_signature_record: '审批签名记录'
}

const signatureActionLabels: Record<string, string> = {
  APPROVE: '审批通过',
  APPROVED: '审批通过',
  REJECT: '审批驳回',
  REJECTED: '审批驳回',
  RETURN: '退回',
  RETURNED: '退回',
  TRANSFER: '转办',
  TRANSFERRED: '转办',
  ADD_SIGN: '加签',
  SIGN_ADDED: '加签',
  SUBMIT: '提交',
  PQC_SUBMIT: 'PQC 检验提交',
  FORM_REVIEW: '表单复核',
  REVIEW_APPROVE: '审核通过',
  DISTRIBUTION_ACK: '发放签收',
  DISTRIBUTION_SIGN: '发放签发',
  ARCHIVE_SEAL: '归档盖章',
  OBSOLETE_CONFIRM: '作废确认'
}

const signatureMeaningLabels: Record<string, string> = {
  PASSWORD_VERIFIED: '签名密码已验证',
  PASSWORD_NOT_VERIFIED: '签名密码未验证',
  PQC_SUBMIT: 'PQC 检验提交',
  FORM_REVIEW: '表单复核',
  REVIEW_APPROVE: '审核通过',
  APPROVE: '审批通过',
  APPROVED: '审批通过',
  REJECT: '审批驳回',
  REJECTED: '审批驳回',
  SUPERVISOR: '主管审批',
  PUBLICITY: '企宣审批',
  CHANGE_REQUEST: '变更申请'
}

const evidenceStatusLabels: Record<string, string> = {
  PASSWORD_VERIFIED: '签名密码已验证',
  PASSWORD_NOT_VERIFIED: '签名密码未验证',
  CAPTURED: '已采集',
  VALID: '已校验',
  PASSED: '已通过',
  READY: '已就绪',
  RECORDED: '已记录',
  PENDING_VERIFY: '待校验',
  PENDING: '待处理',
  INVALID: '校验失败',
  FAILED: '失败',
  BLOCKED: '已阻断',
  HISTORICAL_UNBOUND: '历史未绑定'
}

const businessRecordTextReplacements: Array<[RegExp, string]> = [
  [/BPM审批/g, '审批'],
  [/MES_FEEDBACK\s*审批/g, '生产报工审批'],
  [/SHOWROOM/g, '展厅'],
  [/CHANGE_REQUEST/g, '变更申请'],
  [/PUBLICITY/g, '企宣'],
  [/SUPERVISOR/g, '主管'],
  [/APPROVED/g, '已批准'],
  [/REJECTED/g, '已驳回'],
  [/SUBMITTED/g, '已提交'],
  [/PENDING/g, '待处理']
]

const recordScopeOptions = [
  { label: '全部记录', value: 'ALL_SIGNATURES' },
  { label: '我的签名', value: 'MY_SIGNATURES' }
] as const

const recordDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'source', label: '来源', width: 100, hideable: false },
  { key: 'businessRecord', label: '业务记录', minWidth: 220, hideable: false },
  { key: 'signer', label: '签名人', minWidth: 160 },
  { key: 'actorDeptPost', label: '部门/岗位', minWidth: 180 },
  { key: 'actorRoleNamesSnapshot', label: '角色', minWidth: 150 },
  { key: 'signatureSummary', label: '签名摘要', minWidth: 240 },
  { key: 'evidenceSummary', label: '证据摘要', minWidth: 220 },
  { key: 'signedAt', label: '签名时间', width: 180 },
  { key: 'operation', label: '操作', width: 210, hideable: false }
]

const {
  columns: recordColumns,
  saving: recordColumnSaving,
  isColumnVisible: isRecordColumnVisible,
  getColumnWidthString: getRecordColumnWidthString,
  getColumnMinWidthString: getRecordColumnMinWidthString,
  handleHeaderDragend: handleRecordHeaderDragend,
  saveConfig: saveRecordColumnConfig
} = useUserTableColumns(tableKey, recordDefaultColumns)

const queryParams = reactive<SignatureGovernanceRecordPageReqVO & {
  pageNo: number
  pageSize: number
  quickFilter?: TableQuickFilterValue
}>({
  pageNo: 1,
  pageSize: 10
})

const recordList = ref<SignatureGovernanceRecordRespVO[]>([])
const recordTotal = ref(0)
const recordLoading = ref(false)
const recordError = ref('')
const previewingRecordIds = ref<string[]>([])
const downloadingRecordIds = ref<string[]>([])
const signatureRecordPdfPreviewDialog = reactive({
  visible: false,
  loading: false,
  inlineError: '',
  fileName: '',
  objectUrl: '',
  target: undefined as SignatureGovernanceRecordRespVO | undefined
})

const recordQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'recordScope', label: '记录范围', type: 'select', options: recordScopeOptions },
  { key: 'source', label: '来源', type: 'select', options: sourceOptions },
  { key: 'keyword', label: '关键字', type: 'text', placeholder: '业务编号/名称' },
  { key: 'signer', label: '签名人', type: 'text', placeholder: '签名人/账号' },
  { key: 'action', label: '动作', type: 'text', placeholder: '动作编码' },
  { key: 'signedAt', label: '签名时间', type: 'dateRange' }
]

const normalizeText = (value: unknown) => {
  const text = String(value ?? '').trim()
  return text || undefined
}

const hasChineseText = (value: string) => /[\u4e00-\u9fa5]/.test(value)

const isUpperCaseCode = (value: string) => /^[A-Z][A-Z0-9_:-]*$/.test(value)

const formatMappedText = (
  value: string | undefined | null,
  labels: Record<string, string>,
  unknownLabel: string
) => {
  const text = value?.trim()
  if (!text) return ''
  if (hasChineseText(text)) return text
  const label = labels[text]
  if (label) return label
  return isUpperCaseCode(text) ? `${unknownLabel}：${text}` : text
}

const buildPageParams = (): SignatureGovernanceRecordPageReqVO => {
  const params: SignatureGovernanceRecordPageReqVO = {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize
  }
  const quickFilter = queryParams.quickFilter
  if (!quickFilter) return params
  if (quickFilter.fieldKey === 'recordScope') {
    return params
  }
  if (quickFilter.fieldKey === 'source') {
    params.sourceCodes = [quickFilter.value as SignatureGovernanceRecordSourceCode]
  } else if (quickFilter.fieldKey === 'keyword') {
    params.keyword = normalizeText(quickFilter.value)
  } else if (quickFilter.fieldKey === 'signer') {
    params.signerKeyword = normalizeText(quickFilter.value)
  } else if (quickFilter.fieldKey === 'action') {
    params.actionCode = normalizeText(quickFilter.value)
  } else if (quickFilter.fieldKey === 'signedAt' && quickFilter.value && quickFilter.valueEnd) {
    params.signedAt = [String(quickFilter.value), String(quickFilter.valueEnd)]
  }
  return params
}

const resolveRecordPageLoader = () => {
  const quickFilter = queryParams.quickFilter
  if (quickFilter?.fieldKey === 'recordScope' && quickFilter.value === 'MY_SIGNATURES') {
    return getMySignatureGovernanceRecordPage
  }
  return getSignatureGovernanceRecordPage
}

const recordQuickFilter = useTableQuickFilter(
  tableKey,
  recordQuickFilterDefinitions,
  queryParams,
  async () => loadRecordPage()
)

const loadRecordPage = async () => {
  recordLoading.value = true
  recordError.value = ''
  try {
    const pageLoader = resolveRecordPageLoader()
    const page = await pageLoader(buildPageParams())
    recordList.value = page.list || []
    recordTotal.value = page.total || 0
  } catch (error) {
    const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
    recordError.value = responseMessage || (error instanceof Error ? error.message : '签名记录加载失败')
    recordList.value = []
    recordTotal.value = 0
  } finally {
    recordLoading.value = false
  }
}

const resetRecordFilters = async () => {
  await recordQuickFilter.resetQuickFilter()
}

const formatSourceLabel = (row: SignatureGovernanceRecordRespVO) => {
  const knownLabel = sourceOptions.find((item) => item.value === row.sourceCode)?.label
  if (knownLabel) return knownLabel
  const text = row.sourceLabel?.trim()
  if (!text) return `未识别来源：${row.sourceCode || '未记录'}`
  if (text === 'BPM审批') return '审批'
  return hasChineseText(text) ? text : `未识别来源：${text}`
}

const formatSourceTableLabel = (sourceTable?: string) => {
  const text = sourceTable?.trim()
  if (!text) return '未记录来源表'
  return sourceTableLabels[text] || `未识别来源表：${text}`
}

const formatBusinessRecordName = (row: SignatureGovernanceRecordRespVO) => {
  const text = row.businessRecordName?.trim()
  if (!text) {
    return formatSourceTableLabel(row.sourceTable)
  }
  return businessRecordTextReplacements.reduce(
    (result, [pattern, label]) => result.replace(pattern, label),
    text
  )
}

const formatSignatureActionText = (row: SignatureGovernanceRecordRespVO) => {
  return (
    formatMappedText(row.actionLabel, signatureActionLabels, '未识别签名动作') ||
    formatMappedText(row.actionCode, signatureActionLabels, '未识别签名动作') ||
    '签名'
  )
}

const formatSignatureMeaningText = (row: SignatureGovernanceRecordRespVO) => {
  return (
    formatMappedText(row.meaningLabel, signatureMeaningLabels, '未识别签名含义') ||
    formatMappedText(row.meaningCode, signatureMeaningLabels, '未识别签名含义') ||
    formatSignatureActionText(row)
  )
}

const formatSignatureSummaryText = (row: SignatureGovernanceRecordRespVO) => {
  const comment = row.comment?.trim()
  if (!comment) {
    return formatSignatureActionText(row)
  }
  return isUpperCaseCode(comment)
    ? formatMappedText(comment, { ...signatureActionLabels, ...signatureMeaningLabels }, '未识别签名内容')
    : comment
}

const formatEvidenceStatusText = (status?: string) => {
  return formatMappedText(status, evidenceStatusLabels, '未识别证据状态') || '未记录'
}

const sourceTagType = (sourceCode: SignatureGovernanceRecordSourceCode) => {
  if (sourceCode === 'FILE') return 'success'
  if (sourceCode === 'BATCH_RECORD') return 'primary'
  if (sourceCode === 'SHOWROOM') return 'warning'
  if (sourceCode === 'MES_FEEDBACK') return 'warning'
  return 'info'
}

const evidenceTagType = (status?: string) => {
  if (
    status === 'VALID' ||
    status === 'PASSED' ||
    status === 'READY' ||
    status === 'RECORDED' ||
    status === 'CAPTURED' ||
    status === 'PASSWORD_VERIFIED'
  ) {
    return 'success'
  }
  if (status === 'INVALID' || status === 'FAILED' || status === 'BLOCKED') return 'danger'
  if (status === 'PENDING_VERIFY' || status === 'PENDING' || status === 'PASSWORD_NOT_VERIFIED') {
    return 'warning'
  }
  return 'info'
}

const formatValue = (value?: string) => {
  const text = value?.trim()
  return text || '-'
}

const formatHash = (hash?: string) => {
  const text = hash?.trim()
  if (!text) return '未记录证据 hash'
  return text.length > 16 ? `${text.slice(0, 12)}...${text.slice(-4)}` : text
}

const formatRecordSignedAt = (signedAt?: SignatureGovernanceRecordRespVO['signedAt']) => {
  if (signedAt == null) {
    return '-'
  }
  return formatDate(new Date(signedAt))
}

const revokeRecordPdfPreviewUrl = () => {
  if (!signatureRecordPdfPreviewDialog.objectUrl) {
    return
  }
  URL.revokeObjectURL(signatureRecordPdfPreviewDialog.objectUrl)
  signatureRecordPdfPreviewDialog.objectUrl = ''
}

const clearRecordPdfPreview = () => {
  revokeRecordPdfPreviewUrl()
  signatureRecordPdfPreviewDialog.loading = false
  signatureRecordPdfPreviewDialog.inlineError = ''
  signatureRecordPdfPreviewDialog.fileName = ''
  signatureRecordPdfPreviewDialog.target = undefined
}

const isRecordActionLoading = (records: string[], globalId?: string) =>
  !!globalId && records.includes(globalId)

const isPreviewingRecord = (globalId?: string) =>
  isRecordActionLoading(previewingRecordIds.value, globalId)

const isDownloadingRecord = (globalId?: string) =>
  isRecordActionLoading(downloadingRecordIds.value, globalId)

const setRecordActionLoading = (
  target: typeof previewingRecordIds,
  globalId: string,
  loading: boolean
) => {
  if (loading) {
    target.value = [...target.value, globalId]
    return
  }
  target.value = target.value.filter((item) => item !== globalId)
}

const resolvePdfPreviewErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message && error.message !== 'error') {
    return error.message
  }
  if (typeof error === 'string' && error && error !== 'error') {
    return error
  }
  return '电子签名 PDF 处理失败，请查看错误提示后重试。'
}

const openRecordPdfPreview = async (row: SignatureGovernanceRecordRespVO) => {
  if (!row.globalId) {
    message.error('当前签名记录缺少全局记录 ID')
    return
  }
  signatureRecordPdfPreviewDialog.visible = true
  signatureRecordPdfPreviewDialog.loading = true
  signatureRecordPdfPreviewDialog.inlineError = ''
  signatureRecordPdfPreviewDialog.fileName = ''
  signatureRecordPdfPreviewDialog.target = row
  revokeRecordPdfPreviewUrl()
  setRecordActionLoading(previewingRecordIds, row.globalId, true)
  try {
    const artifact = await fetchSignatureGovernanceRecordPdfArtifact(row.globalId)
    signatureRecordPdfPreviewDialog.fileName = artifact.fileName
    signatureRecordPdfPreviewDialog.objectUrl = URL.createObjectURL(artifact.blob)
  } catch (error) {
    signatureRecordPdfPreviewDialog.inlineError = resolvePdfPreviewErrorMessage(error)
  } finally {
    signatureRecordPdfPreviewDialog.loading = false
    setRecordActionLoading(previewingRecordIds, row.globalId, false)
  }
}

const handleRecordPdfDownload = async (row: SignatureGovernanceRecordRespVO) => {
  if (!row.globalId) {
    message.error('当前签名记录缺少全局记录 ID')
    return
  }
  setRecordActionLoading(downloadingRecordIds, row.globalId, true)
  try {
    await downloadSignatureGovernanceRecordPdf(row.globalId)
  } catch (error) {
    message.error(resolvePdfPreviewErrorMessage(error))
  } finally {
    setRecordActionLoading(downloadingRecordIds, row.globalId, false)
  }
}

const openRecordDetail = (row: SignatureGovernanceRecordRespVO) => {
  if (!row.detailPath) {
    message.error('当前签名记录缺少详情路径')
    return
  }
  void router.push(row.detailPath)
}

onMounted(loadRecordPage)

onBeforeUnmount(() => {
  revokeRecordPdfPreviewUrl()
})
</script>

<style scoped>
.signature-governance-records {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.signature-governance-records__list :deep(.el-table) {
  font-size: 0.9rem;
}

.signature-governance-records__main {
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature-governance-records__meta,
.signature-governance-records__hash {
  overflow: hidden;
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature-governance-records__summary {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.signature-governance-records__summary-text {
  margin-left: 6px;
  color: #263247;
}

.signature-governance-records__label {
  margin-right: 6px;
  color: #4b5563;
  font-size: 0.82rem;
}

.signature-governance-records__actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.signature-record-pdf-preview {
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 640px;
  overflow: hidden;
  background: #f7f9fc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.signature-record-pdf-preview__frame {
  display: block;
  width: 100%;
  height: 640px;
  background: #ffffff;
  border: 0;
}

@media (max-width: 760px) {
  .signature-record-pdf-preview {
    min-height: 520px;
  }

  .signature-record-pdf-preview__frame {
    height: 520px;
  }
}
</style>
