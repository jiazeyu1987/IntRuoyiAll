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
                {{ row.sourceLabel || sourceLabel(row.sourceCode) }}
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
                {{ row.businessRecordName || row.sourceTable }}
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
                    {{ row.meaningLabel || row.actionLabel || row.actionCode || '签名' }}
                  </el-tag>
                  <span class="signature-governance-records__summary-text">
                    {{ row.comment || row.meaningCode || '-' }}
                  </span>
                </div>
                <div class="signature-governance-records__meta">
                  来源表：{{ row.sourceTable }}
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
                    {{ row.evidenceStatus || '未记录' }}
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
  { label: 'BPM审批', value: 'BPM' },
  { label: '报工审批', value: 'MES_FEEDBACK' },
  { label: '排产', value: 'SCHEDULING' },
  { label: '文控', value: 'DOCUMENT_CONTROL' }
] as const

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

const sourceLabel = (sourceCode: SignatureGovernanceRecordSourceCode) =>
  sourceOptions.find((item) => item.value === sourceCode)?.label || sourceCode

const sourceTagType = (sourceCode: SignatureGovernanceRecordSourceCode) => {
  if (sourceCode === 'FILE') return 'success'
  if (sourceCode === 'BATCH_RECORD') return 'primary'
  if (sourceCode === 'SHOWROOM') return 'warning'
  if (sourceCode === 'MES_FEEDBACK') return 'warning'
  return 'info'
}

const evidenceTagType = (status?: string) => {
  if (status === 'VALID' || status === 'PASSED' || status === 'READY') return 'success'
  if (status === 'INVALID' || status === 'FAILED' || status === 'BLOCKED') return 'danger'
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
