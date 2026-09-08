<template>
  <section class="signature-governance-list-pane">
    <div class="csv-evidence-entrypoints">
      <div class="csv-evidence-entrypoints__header">
        <div>
          <div class="csv-evidence-entrypoints__title">审批证据导出入口</div>
          <div class="csv-evidence-entrypoints__hint">
            CSV 页签统一收口导出按钮；查看类证据仍可跳转正式页面核对明细。
          </div>
        </div>
      </div>
      <div class="csv-evidence-entrypoints__grid">
        <div
          v-for="entrypoint in approvalEvidenceEntrypoints"
          :key="entrypoint.id"
          class="csv-evidence-entrypoints__item"
        >
          <div class="csv-evidence-entrypoints__item-title">{{ entrypoint.label }}</div>
          <div class="csv-evidence-entrypoints__item-desc">{{ entrypoint.description }}</div>
          <div class="csv-evidence-entrypoints__item-meta">
            <span>来源：{{ entrypoint.sourcePage }}</span>
            <span>权限：{{ entrypoint.permission }}</span>
          </div>
          <el-button
            link
            type="primary"
            v-hasPermi="[entrypoint.permission]"
            :loading="approvalEvidenceExportingId === entrypoint.id"
            @click="handleApprovalEvidenceEntrypoint(entrypoint)"
          >
            {{ entrypoint.actionType === 'export' ? '导出' : '查看' }}
          </el-button>
        </div>
      </div>
    </div>
    <UnifiedListTemplate
      table-key="signature.governance.csvPackage.list"
      :query-model="queryParams"
      :filter-definitions="filterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="columns"
      :column-saving="columnSaving"
      :total="filteredRows.length"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @column-change="saveColumnConfig"
      @column-reset="resetColumnConfig"
      @pagination="handlePagination"
    >
      <template #extra-filters>
        <el-form-item label="签名样本">
          <el-select
            v-model="selectedDccSignatureId"
            class="signature-governance-list-pane__select"
            filterable
            clearable
            :loading="dccSignatureCandidateLoading"
          >
            <el-option
              v-for="candidate in dccSignatureCandidates"
              :key="candidate.id"
              :label="formatDccSignatureCandidate(candidate)"
              :value="candidate.id"
            />
          </el-select>
        </el-form-item>
      </template>

      <template #actions>
        <el-button :loading="dccSignatureCandidateLoading" @click="loadDccSignatureCandidates">刷新</el-button>
        <el-button type="primary" @click="applySelectedDccSignatureCandidate">应用</el-button>
        <el-button :loading="csvSourceLoading" @click="loadCsvSourceCandidates">来源</el-button>
        <el-button
          v-hasPermi="[SIGNATURE_GOVERNANCE_PERMISSIONS.CSV_PACKAGE_MANAGE]"
          type="primary"
          :loading="csvLoading"
          @click="evaluateCsvGate"
        >
          评估
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          data-user-table-column-explicit
          data-user-table-key="signature.governance.csvPackage.list"
          :data="pagedRows"
          :empty-text="csvError || candidateAutoFillError || '暂无CSV质量包记录'"
          :show-overflow-tooltip="true"
          @header-dragend="handleHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column v-if="isColumnVisible('item')" label="事项" prop="item" :width="getColumnWidthString('item', 130)" v-bind="sortColumnAttrs('item')" />
          <el-table-column v-if="isColumnVisible('source')" label="来源" prop="source" :min-width="getColumnMinWidthString('source', 190)" v-bind="sortColumnAttrs('source')" />
          <el-table-column v-if="isColumnVisible('status')" label="状态" prop="status" :width="getColumnWidthString('status', 130)" v-bind="sortColumnAttrs('status')">
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColumnVisible('keyFields')" label="关键字段" prop="keyFields" :min-width="getColumnMinWidthString('keyFields', 300)" v-bind="sortColumnAttrs('keyFields')" />
          <el-table-column v-if="isColumnVisible('evidence')" label="证据" prop="evidence" :min-width="getColumnMinWidthString('evidence', 240)" v-bind="sortColumnAttrs('evidence')" />
          <el-table-column v-if="isColumnVisible('blockerImpact')" label="阻断影响" prop="blockerImpact" :min-width="getColumnMinWidthString('blockerImpact', 260)" v-bind="sortColumnAttrs('blockerImpact')" />
          <el-table-column v-if="isColumnVisible('operation')" label="操作" fixed="right" :width="getColumnWidthString('operation', 140)">
            <template #default="{ row }">
              <template v-if="row.actions.length > 0">
                <el-button
                  v-for="action in row.actions"
                  :key="action.label"
                  link
                  type="primary"
                  :loading="action.loading"
                  @click="action.handler"
                >
                  {{ action.label }}
                </el-button>
              </template>
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </section>
</template>

<script lang="ts" setup>
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'
import {
  evaluateSignatureCsvReleaseGate,
  type SignatureGovernanceCsvMaterialStatus,
  type SignatureGovernanceCsvMaterialType,
  type SignatureGovernanceCsvReleaseGateRespVO
} from '@/api/signature-governance/csvPackage'
import {
  SIGNATURE_GOVERNANCE_PERMISSIONS,
  type SignatureGovernanceBlocker
} from '@/api/signature-governance/shared'
import { useUserStore } from '@/store/modules/user'
import {
  downloadDccSignatureEvidenceExport,
  getDccElectronicSignaturePage,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'
import * as RuntimeControlApi from '@/api/infra/runtimeControl'
import { downloadBackupEvidence } from '@/api/system/backupPlan'
import * as UserApi from '@/api/system/user'
import {
  getEdhrValidationPackagePage,
  getEdhrValidationRequirementItemPage,
  type EdhrValidationRequirementItemRespVO
} from '@/api/mes/pro/edhr/validation'
import { getTrainingExecutionPage } from '@/api/dcc/controlledFile/training'
import { getEdhrRecordChangePage } from '@/api/mes/pro/edhr/change'
import {
  getEdhrReleaseCheckItemPage,
  getEdhrReleaseEventPage,
  getEdhrReleasePage
} from '@/api/mes/pro/edhr/release'
import {
  exportEdhrFieldAudit,
  getEdhrFieldAuditPage,
  type EdhrFieldAuditExportRespVO
} from '@/api/mes/pro/edhr/fieldAudit'
import {
  downloadEdhrBatchArchive,
  EDHR_BATCH_STATUS_ARCHIVED,
  getEdhrBatchExecutionPage,
  getLatestEdhrBatchArchive
} from '@/api/mes/pro/edhr/batchExecution'
import download from '@/utils/download'
import { downloadByData } from '@/utils/filt'

defineOptions({ name: 'CsvPackageGovernanceListPane' })

const DCC_SIGNATURE_MANAGE_PERMISSION = 'dcc:controlled-file:signature:manage'
const ALL_PERMISSION = '*:*:*'
const userStore = useUserStore()
const router = useRouter()
const hasDccSignatureManagePermission = computed(
  () =>
    userStore.getPermissions.has(ALL_PERMISSION) ||
    userStore.getPermissions.has(DCC_SIGNATURE_MANAGE_PERMISSION)
)

type GovernanceRowAction = {
  label: string
  loading?: boolean
  handler: () => void
}

type GovernanceRow = {
  id: string
  item: string
  source: string
  status: string
  statusType: string
  keyFields: string
  evidence: string
  blockerImpact: string
  actions: GovernanceRowAction[]
}

type ApprovalEvidenceEntrypoint = {
  id: string
  label: string
  description: string
  sourcePage: string
  routePath: string
  permission: string
  actionType: 'export' | 'view'
}

const approvalEvidenceEntrypoints: ApprovalEvidenceEntrypoint[] = [
  {
    id: 'dcc-signature-evidence',
    label: 'DCC签名证据PDF',
    description: '导出 DCC 受控文件审批/签名记录证据 PDF，包含签名人、时间、含义和证据 hash。',
    sourcePage: '电子签名记录',
    routePath: '/signature-governance/signature-records',
    permission: 'dcc:controlled-file:signature:manage',
    actionType: 'export'
  },
  {
    id: 'trusted-time-evidence',
    label: '可信时间戳证据ZIP',
    description: '导出已保存运行巡检中的可信时间证据包，用于证明签名时间由系统可信生成。',
    sourcePage: '运行控制台',
    routePath: '/infra/runtime-control',
    permission: 'infra:runtime-control:operate',
    actionType: 'export'
  },
  {
    id: 'backup-review-evidence',
    label: '备份审查证据ZIP',
    description: '导出备份链、恢复演练和审查结论证据包，用于 CSV 和数据完整性审查。',
    sourcePage: '备份计划',
    routePath: '/system/backup-plan',
    permission: 'system:backup-plan:evidence-export',
    actionType: 'export'
  },
  {
    id: 'account-security-evidence',
    label: '账号安全与通用账号证据',
    description: '导出用户清单和通用账号不合规清单，用于核对账户唯一性、共享账号和账号控制证据。',
    sourcePage: '用户管理',
    routePath: '/system/user',
    permission: 'system:user:export',
    actionType: 'export'
  },
  {
    id: 'edhr-signature-records',
    label: 'eDHR签名记录',
    description: '查看 eDHR 正式签名记录，用于核对签名人、签名时间、认证方式、内容 hash 和签名含义。',
    sourcePage: 'eDHR签名记录',
    routePath: '/mes/pro/feedback/edhr-signatures',
    permission: 'mes:pro-batch-record-execution:signature-query',
    actionType: 'view'
  },
  {
    id: 'edhr-field-audit-evidence',
    label: 'eDHR字段审计证据',
    description: '进入字段审计链导出字段变更证据，用于核对变更前后内容、修改原因和审计追踪完整性。',
    sourcePage: '字段审计链',
    routePath: '/mes/pro/feedback/edhr-field-audit',
    permission: 'mes:pro-batch-record-execution:field-audit-query',
    actionType: 'export'
  },
  {
    id: 'approval-sequence-evidence',
    label: '审批中心顺序证据',
    description: '进入审批中心已办列表查看流程节点、处理人和审批时间线，用于核对多人审批顺序。',
    sourcePage: '审批中心',
    routePath: '/approval-center/done',
    permission: 'bpm:task:query',
    actionType: 'view'
  },
  {
    id: 'edhr-archive-evidence',
    label: 'eDHR批记录归档',
    description: '进入 eDHR 历史追溯下载已密封批记录归档，用于核对批记录、审批、签名和归档证据。',
    sourcePage: 'eDHR历史追溯',
    routePath: '/mes/pro/feedback/edhr-batch-history',
    permission: 'mes:pro-edhr-batch-execution:query',
    actionType: 'export'
  },
  {
    id: 'edhr-permission-matrix-evidence',
    label: 'eDHR权限矩阵证据',
    description: '进入对象权限矩阵评估页，用于核对签名人职责、业务对象权限和账号控制边界。',
    sourcePage: 'eDHR对象权限',
    routePath: '/mes/pro/feedback/edhr-permission-matrix',
    permission: 'mes:pro-edhr-permission-scope:evaluate',
    actionType: 'view'
  }
]

const message = useMessage()
const approvalEvidenceExportingId = ref<string>()

const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'item', label: '事项', width: 130 },
  { key: 'source', label: '来源', minWidth: 190 },
  { key: 'status', label: '状态', width: 130 },
  { key: 'keyFields', label: '关键字段', minWidth: 300 },
  { key: 'evidence', label: '证据', minWidth: 240 },
  { key: 'blockerImpact', label: '阻断影响', minWidth: 260 },
  { key: 'operation', label: '操作', width: 140 }
]

const {
  columns,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidthString,
  getColumnMinWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig,
  resetConfig: resetColumnConfig
} = useUserTableColumns('signature.governance.csvPackage.list', defaultColumns)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const filterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'item', label: '事项', type: 'text', placeholder: '请输入事项' },
  { key: 'source', label: '来源', type: 'text', placeholder: '请输入来源' },
  { key: 'status', label: '状态', type: 'text', placeholder: '请输入状态' },
  { key: 'keyFields', label: '关键字段', type: 'text', placeholder: '请输入关键字段' },
  { key: 'evidence', label: '证据', type: 'text', placeholder: '请输入证据' },
  { key: 'blockerImpact', label: '阻断影响', type: 'text', placeholder: '请输入阻断影响' }
]

const dccSignatureCandidateLoading = ref(false)
const dccSignatureCandidates = ref<DccElectronicSignatureVO[]>([])
const selectedDccSignatureId = ref<number>()
const candidateAutoFillError = ref('')
const csvSourceLoading = ref(false)
const csvLoading = ref(false)
const csvError = ref('')
const csvResult = ref<SignatureGovernanceCsvReleaseGateRespVO>()

const csvForm = reactive({
  releaseId: '',
  qualityOwner: '',
  recoveryEvidenceRef: '',
  engineeringVerificationPassed: false,
  materials: [{
    type: 'URS' as SignatureGovernanceCsvMaterialType,
    documentId: '',
    version: '',
    status: 'APPROVED' as SignatureGovernanceCsvMaterialStatus,
    owner: '',
    sourceEvidence: ''
  }],
  traceRelations: [{
    requirementRef: '',
    designRef: '',
    testRef: '',
    evidenceRef: '',
    status: 'APPROVED' as SignatureGovernanceCsvMaterialStatus
  }],
  trainingRecords: [{
    trainingId: '',
    userId: '',
    sopDocumentId: '',
    evidenceRef: '',
    effective: true
  }],
  changeControls: [{
    changeControlId: '',
    status: 'APPROVED' as SignatureGovernanceCsvMaterialStatus,
    evidenceRef: ''
  }],
  qaApproval: {
    approvalRef: '',
    approver: '',
    status: 'APPROVED' as SignatureGovernanceCsvMaterialStatus,
    signatureEvidenceRef: ''
  }
})

const displayValue = (value: unknown) => {
  if (value === undefined || value === null) return '等待来源'
  const text = String(value).trim()
  return text || '等待来源'
}

const statusTagType = (status?: string) => {
  if (['READY', 'GO', 'COLLECTED', 'ALLOWED', 'RECORDED', 'PASSED', 'APPROVED'].includes(String(status))) return 'success'
  if (status === 'BLOCKED' || status === 'REJECTED') return 'danger'
  if (!status || status === '等待来源') return 'warning'
  return 'info'
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const trimOptional = (value: string) => {
  const trimmed = value.trim()
  return trimmed || undefined
}

const requireText = (value: string, label: string) => {
  const trimmed = value.trim()
  if (!trimmed) throw new Error(`${label}不能为空`)
  return trimmed
}

const failFast = (messageText: string, errorRef: { value: string }) => {
  errorRef.value = messageText
  message.error(messageText)
}

const openApprovalEvidenceEntrypoint = async (entrypoint: ApprovalEvidenceEntrypoint) => {
  try {
    await router.push({ path: entrypoint.routePath })
  } catch (error) {
    failFast(resolveErrorMessage(error, '证据导出入口跳转失败'), csvError)
  }
}

const downloadFieldAuditExportPayload = (exportPayload: EdhrFieldAuditExportRespVO) => {
  if (!exportPayload.fileName?.trim()) throw new Error('字段审计导出响应缺少 fileName，无法下载。')
  if (!exportPayload.contentType?.trim()) throw new Error('字段审计导出响应缺少 contentType，无法下载。')
  const { content } = exportPayload
  let bytes: Uint8Array
  if (Array.isArray(content)) {
    if (!content.length) throw new Error('字段审计导出响应 content 为空，无法下载。')
    bytes = Uint8Array.from(content)
  } else if (typeof content === 'string' && content.trim()) {
    const base64Content = content.includes(',') ? content.slice(content.indexOf(',') + 1) : content
    const binary = window.atob(base64Content)
    if (!binary.length) throw new Error('字段审计导出响应 content 为空，无法下载。')
    bytes = new Uint8Array(binary.length)
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index)
    }
  } else {
    throw new Error('字段审计导出响应缺少 content，无法下载。')
  }
  downloadByData(new Blob([bytes], { type: exportPayload.contentType }), exportPayload.fileName, exportPayload.contentType)
}

const exportDccSignatureEvidenceFromCsv = async () => {
  if (!selectedDccSignatureCandidate.value) {
    await loadDccSignatureCandidates()
  }
  const candidate = selectedDccSignatureCandidate.value
  if (!candidate?.controlledFileId) {
    throw new Error('请先在 CSV 质量包选择包含受控文件 ID 的 DCC 签名样本。')
  }
  await downloadDccSignatureEvidenceExport(candidate.controlledFileId)
}

const exportTrustedTimeEvidenceFromCsv = async () => {
  const inspectionRun = await RuntimeControlApi.runRuntimeControlInspection()
  if (!inspectionRun?.id) {
    throw new Error('运行巡检未返回巡检编号，无法导出可信时间戳证据。')
  }
  const data = await RuntimeControlApi.downloadRuntimeControlTimeEvidence(inspectionRun.id)
  download.zip(data, `可信时间证据_巡检${inspectionRun.id}.zip`)
}

const exportBackupReviewEvidenceFromCsv = async () => {
  const blob = await downloadBackupEvidence()
  downloadByData(blob, 'IntRuoyi-备份审查证据.zip', blob.type || 'application/zip')
}

const exportAccountSecurityEvidenceFromCsv = async () => {
  const users = await UserApi.exportUser({ pageNo: 1, pageSize: 10 })
  download.excel(users, '用户数据.xls')
  const genericAccounts = await UserApi.exportGenericAccountUsers()
  download.excel(genericAccounts, '通用账户不合规清单.xls')
}

const exportFieldAuditEvidenceFromCsv = async () => {
  const page = await getEdhrFieldAuditPage({ pageNo: 1, pageSize: 1 })
  const firstAudit = (page.list || [])[0]
  if (!firstAudit?.executionId) {
    throw new Error('当前没有可导出的 eDHR 字段审计记录。')
  }
  const exportPayload = await exportEdhrFieldAudit({
    pageNo: 1,
    pageSize: 1000,
    executionId: firstAudit.executionId,
    format: 'XLSX'
  })
  downloadFieldAuditExportPayload(exportPayload)
}

const exportEdhrArchiveEvidenceFromCsv = async () => {
  const page = await getEdhrBatchExecutionPage({
    pageNo: 1,
    pageSize: 1,
    status: EDHR_BATCH_STATUS_ARCHIVED
  })
  const batch = (page.list || [])[0]
  if (!batch?.id) {
    throw new Error('当前没有可导出的已归档 eDHR 批记录。')
  }
  const archive = await getLatestEdhrBatchArchive(batch.id)
  if (!archive?.id) {
    throw new Error('当前已归档批记录缺少可下载归档。')
  }
  await downloadEdhrBatchArchive(archive.id, archive.fileName, archive.artifactType)
}

const downloadApprovalEvidenceEntrypoint = async (entrypoint: ApprovalEvidenceEntrypoint) => {
  approvalEvidenceExportingId.value = entrypoint.id
  csvError.value = ''
  try {
    switch (entrypoint.id) {
      case 'dcc-signature-evidence':
        await exportDccSignatureEvidenceFromCsv()
        break
      case 'trusted-time-evidence':
        await exportTrustedTimeEvidenceFromCsv()
        break
      case 'backup-review-evidence':
        await exportBackupReviewEvidenceFromCsv()
        break
      case 'account-security-evidence':
        await exportAccountSecurityEvidenceFromCsv()
        break
      case 'edhr-field-audit-evidence':
        await exportFieldAuditEvidenceFromCsv()
        break
      case 'edhr-archive-evidence':
        await exportEdhrArchiveEvidenceFromCsv()
        break
      default:
        throw new Error(`未配置CSV质量包导出动作：${entrypoint.id}`)
    }
    message.success(`${entrypoint.label}已导出`)
  } catch (error) {
    failFast(resolveErrorMessage(error, `${entrypoint.label}导出失败`), csvError)
  } finally {
    approvalEvidenceExportingId.value = undefined
  }
}

const handleApprovalEvidenceEntrypoint = async (entrypoint: ApprovalEvidenceEntrypoint) => {
  if (entrypoint.actionType === 'view') {
    await openApprovalEvidenceEntrypoint(entrypoint)
    return
  }
  await downloadApprovalEvidenceEntrypoint(entrypoint)
}

const formatDccSignatureCandidate = (candidate: DccElectronicSignatureVO) => {
  const fileNumber = candidate.fileNumber || `文件ID ${candidate.controlledFileId}`
  const signer = candidate.signerName || `用户ID ${candidate.signerUserId}`
  const action = candidate.taskActionResult || candidate.meaningCode || '签名'
  const signedAt = candidate.signedAt || '无签名时间'
  return `${fileNumber} / ${candidate.versionNo || '-'} / ${action} / ${signer} / ${signedAt}`
}

const selectedDccSignatureCandidate = computed(() =>
  dccSignatureCandidates.value.find((candidate) => candidate.id === selectedDccSignatureId.value)
)

const resolveCandidateVersionId = (candidate: DccElectronicSignatureVO) =>
  candidate.controlledCopyVersionId || candidate.sourceVersionId || candidate.versionNo || ''

const resolveCandidateEvidenceHash = (candidate: DccElectronicSignatureVO) =>
  candidate.evidenceHash || candidate.controlledCopyHash || candidate.sourceFileHash || ''

const applyDccSignatureCandidate = (candidate: DccElectronicSignatureVO) => {
  const evidenceHash = resolveCandidateEvidenceHash(candidate)
  const signer = candidate.signerName || ''
  const fileNumber = candidate.fileNumber || String(candidate.controlledFileId)
  csvForm.releaseId = fileNumber
  csvForm.qualityOwner = signer
  csvForm.recoveryEvidenceRef = evidenceHash
  csvForm.materials[0].documentId = fileNumber
  csvForm.materials[0].version = resolveCandidateVersionId(candidate)
  csvForm.materials[0].owner = signer
  csvForm.materials[0].sourceEvidence = evidenceHash
  csvForm.traceRelations[0].evidenceRef = evidenceHash
  csvForm.qaApproval.approvalRef = candidate.taskId || ''
  csvForm.qaApproval.approver = signer
  csvForm.qaApproval.signatureEvidenceRef = evidenceHash
  candidateAutoFillError.value = evidenceHash ? '' : '当前签名记录缺少可回填的签名证据'
}

const loadDccSignatureCandidates = async () => {
  dccSignatureCandidateLoading.value = true
  candidateAutoFillError.value = ''
  try {
    if (!hasDccSignatureManagePermission.value) {
      dccSignatureCandidates.value = []
      selectedDccSignatureId.value = undefined
      candidateAutoFillError.value =
        '当前账号没有DCC电子签名管理权限，不能自动加载真实文件签名样本。'
      return
    }
    const page = await getDccElectronicSignaturePage({ pageNo: 1, pageSize: 20 })
    dccSignatureCandidates.value = page.list || []
    if (dccSignatureCandidates.value.length === 0) {
      candidateAutoFillError.value = '当前没有可用于自动回填的真实文件签名样本'
      selectedDccSignatureId.value = undefined
      return
    }
    if (!selectedDccSignatureId.value) {
      selectedDccSignatureId.value = dccSignatureCandidates.value[0].id
      applyDccSignatureCandidate(dccSignatureCandidates.value[0])
    }
  } catch (error) {
    candidateAutoFillError.value = resolveErrorMessage(error, '真实文件签名样本加载失败')
  } finally {
    dccSignatureCandidateLoading.value = false
  }
}

const applySelectedDccSignatureCandidate = () => {
  const candidate = selectedDccSignatureCandidate.value
  if (!candidate) {
    failFast('请先选择真实文件签名样本', candidateAutoFillError)
    return
  }
  applyDccSignatureCandidate(candidate)
  if (!candidateAutoFillError.value) message.success('已根据真实文件签名样本自动回填')
}

const firstValidationItemCode = (
  items: EdhrValidationRequirementItemRespVO[],
  acceptedTypes: string[]
) => {
  const item = items.find((current) => acceptedTypes.includes(String(current.itemType || '').toUpperCase()))
  return item ? `${item.itemCode}@${item.itemVersion}` : ''
}

const loadCsvReleaseCandidate = async () => {
  const page = await getEdhrReleasePage({ pageNo: 1, pageSize: 1 })
  const release = (page.list || [])[0]
  if (!release) return '当前没有可用于自动回填的真实发布记录'
  const releaseTransactionId = release.releaseTransactionId
  csvForm.releaseId = release.releaseCode || String(releaseTransactionId || release.batchExecutionCode)
  csvForm.qualityOwner = release.approvedBy ? String(release.approvedBy) : csvForm.qualityOwner
  csvForm.materials[0].documentId = release.batchExecutionCode
  csvForm.materials[0].version = release.releaseStatus
  csvForm.qaApproval.approvalRef = release.approvalIdempotencyKey || csvForm.releaseId
  csvForm.qaApproval.approver = release.approvedBy ? String(release.approvedBy) : csvForm.qaApproval.approver
  csvForm.qaApproval.signatureEvidenceRef = release.approvalSignoffEvidenceHash || ''
  csvForm.recoveryEvidenceRef = release.approvalSignoffEvidenceHash || csvForm.recoveryEvidenceRef
  if (releaseTransactionId) {
    const checkPage = await getEdhrReleaseCheckItemPage({ pageNo: 1, pageSize: 1, releaseTransactionId })
    const checkItem = (checkPage.list || [])[0]
    if (checkItem) {
      csvForm.materials[0].sourceEvidence = checkItem.evidenceHash || csvForm.materials[0].sourceEvidence
      csvForm.traceRelations[0].evidenceRef = checkItem.evidenceHash || csvForm.traceRelations[0].evidenceRef
    }
    const eventPage = await getEdhrReleaseEventPage({ pageNo: 1, pageSize: 1, releaseTransactionId })
    const event = (eventPage.list || [])[0]
    if (event) {
      csvForm.qaApproval.approvalRef = event.idempotencyKey || csvForm.qaApproval.approvalRef
      csvForm.qaApproval.signatureEvidenceRef = event.signoffEvidenceHash || event.evidenceHash || csvForm.qaApproval.signatureEvidenceRef
    }
  }
  return ''
}

const loadCsvValidationCandidate = async () => {
  const packagePage = await getEdhrValidationPackagePage({ pageNo: 1, pageSize: 1 })
  const validationPackage = (packagePage.list || [])[0]
  if (!validationPackage) return '当前没有可用于自动回填的真实验证包'
  csvForm.releaseId = csvForm.releaseId || validationPackage.releaseTag
  csvForm.qualityOwner = csvForm.qualityOwner || validationPackage.qaOwnerName || validationPackage.validationOwnerName
  const itemPage = await getEdhrValidationRequirementItemPage({ pageNo: 1, pageSize: 50, packageId: validationPackage.id })
  const items = itemPage.list || []
  csvForm.traceRelations[0].requirementRef = firstValidationItemCode(items, ['URS']) || csvForm.traceRelations[0].requirementRef
  csvForm.traceRelations[0].designRef = firstValidationItemCode(items, ['FRS', 'RISK']) || csvForm.traceRelations[0].designRef
  csvForm.traceRelations[0].testRef = firstValidationItemCode(items, ['IQ', 'OQ', 'PQ']) || csvForm.traceRelations[0].testRef
  return ''
}

const loadCsvTrainingCandidate = async () => {
  const page = await getTrainingExecutionPage({ pageNo: 1, pageSize: 1 })
  const training = (page.list || [])[0]
  if (!training) return '当前没有可用于自动回填的真实培训执行记录'
  csvForm.trainingRecords[0].trainingId = String(training.progressId)
  csvForm.trainingRecords[0].userId = String(training.userId)
  csvForm.trainingRecords[0].sopDocumentId = String(training.controlledFileId)
  csvForm.trainingRecords[0].evidenceRef = [training.fileNumber || training.title, training.versionNo, training.acknowledgedAt || training.status].filter(Boolean).join('/')
  csvForm.trainingRecords[0].effective = training.status === 'ACKNOWLEDGED'
  return ''
}

const loadCsvChangeCandidate = async () => {
  const page = await getEdhrRecordChangePage({ pageNo: 1, pageSize: 1 })
  const change = (page.list || [])[0]
  if (!change) return '当前没有可用于自动回填的真实变更记录'
  csvForm.changeControls[0].changeControlId = change.changeCode || String(change.id)
  csvForm.changeControls[0].status = change.changeStatus === 'EFFECTIVE' ? 'APPROVED' : csvForm.changeControls[0].status
  csvForm.changeControls[0].evidenceRef = change.newArchiveHash || change.previousArchiveHash || change.newHeadHash || change.previousHeadHash || ''
  return ''
}

const loadCsvSourceCandidates = async () => {
  csvSourceLoading.value = true
  csvError.value = ''
  try {
    const messages = [
      await loadCsvReleaseCandidate(),
      await loadCsvValidationCandidate(),
      await loadCsvTrainingCandidate(),
      await loadCsvChangeCandidate()
    ].filter(Boolean)
    const missing = [
      ['Release ID', csvForm.releaseId],
      ['材料证据', csvForm.materials[0].sourceEvidence],
      ['追溯需求', csvForm.traceRelations[0].requirementRef],
      ['追溯设计', csvForm.traceRelations[0].designRef],
      ['追溯测试', csvForm.traceRelations[0].testRef],
      ['培训证据', csvForm.trainingRecords[0].evidenceRef],
      ['变更证据', csvForm.changeControls[0].evidenceRef],
      ['QA签名证据', csvForm.qaApproval.signatureEvidenceRef]
    ].filter(([, value]) => !String(value || '').trim()).map(([label]) => label)
    if (missing.length > 0) messages.push(`真实来源缺少${missing.join('、')}`)
    csvError.value = messages.join('；')
  } catch (error) {
    csvError.value = resolveErrorMessage(error, 'CSV来源样本加载失败')
  } finally {
    csvSourceLoading.value = false
  }
}

const buildCsvMaterials = () => {
  const material = csvForm.materials[0]
  return [{
    type: material.type,
    documentId: requireText(material.documentId, 'CSV材料文档ID'),
    version: requireText(material.version, 'CSV材料版本'),
    status: material.status,
    owner: trimOptional(material.owner),
    sourceEvidence: requireText(material.sourceEvidence, 'CSV材料证据')
  }]
}

const buildCsvTraceRelations = () => {
  const traceRelation = csvForm.traceRelations[0]
  return [{
    requirementRef: requireText(traceRelation.requirementRef, 'CSV追溯需求'),
    designRef: requireText(traceRelation.designRef, 'CSV追溯设计'),
    testRef: requireText(traceRelation.testRef, 'CSV追溯测试'),
    evidenceRef: requireText(traceRelation.evidenceRef, 'CSV追溯证据'),
    status: traceRelation.status
  }]
}

const buildCsvTrainingRecords = () => {
  const trainingRecord = csvForm.trainingRecords[0]
  return [{
    trainingId: requireText(trainingRecord.trainingId, 'CSV培训ID'),
    userId: requireText(trainingRecord.userId, 'CSV培训用户ID'),
    sopDocumentId: requireText(trainingRecord.sopDocumentId, 'CSV培训SOP文档'),
    evidenceRef: requireText(trainingRecord.evidenceRef, 'CSV培训证据'),
    effective: trainingRecord.effective
  }]
}

const buildCsvChangeControls = () => {
  const changeControl = csvForm.changeControls[0]
  return [{
    changeControlId: requireText(changeControl.changeControlId, 'CSV变更控制ID'),
    status: changeControl.status,
    evidenceRef: requireText(changeControl.evidenceRef, 'CSV变更控制证据')
  }]
}

const buildCsvQaApproval = () => ({
  approvalRef: requireText(csvForm.qaApproval.approvalRef, 'CSV QA批准Ref'),
  approver: requireText(csvForm.qaApproval.approver, 'CSV QA批准人'),
  status: csvForm.qaApproval.status,
  signatureEvidenceRef: requireText(csvForm.qaApproval.signatureEvidenceRef, 'CSV QA签名证据')
})

const validateCsvForm = () => {
  try {
    requireText(csvForm.releaseId, 'Release ID')
    buildCsvMaterials()
    buildCsvTraceRelations()
    buildCsvTrainingRecords()
    buildCsvChangeControls()
    buildCsvQaApproval()
    return true
  } catch (error) {
    failFast(resolveErrorMessage(error, 'CSV 发布门禁缺少真实质量包样本'), csvError)
    return false
  }
}

const evaluateCsvGate = async () => {
  const releaseId = csvForm.releaseId.trim()
  if (!validateCsvForm()) return
  csvLoading.value = true
  csvError.value = ''
  try {
    csvResult.value = await evaluateSignatureCsvReleaseGate(releaseId, {
      qualityOwner: trimOptional(csvForm.qualityOwner),
      materials: buildCsvMaterials(),
      traceRelations: buildCsvTraceRelations(),
      trainingRecords: buildCsvTrainingRecords(),
      changeControls: buildCsvChangeControls(),
      qaApproval: buildCsvQaApproval(),
      recoveryEvidenceRef: trimOptional(csvForm.recoveryEvidenceRef),
      engineeringVerificationPassed: csvForm.engineeringVerificationPassed
    })
  } catch (error) {
    csvError.value = resolveErrorMessage(error, 'CSV 发布门禁评估失败')
  } finally {
    csvLoading.value = false
  }
}

const buildBlockerRows = (blockers: SignatureGovernanceBlocker[]) =>
  blockers.map((blocker, index): GovernanceRow => ({
    id: `blocker-${index}-${blocker.code}`,
    item: '阻断项',
    source: blocker.code,
    status: 'BLOCKED',
    statusType: 'danger',
    keyFields: blocker.message,
    evidence: '—',
    blockerImpact: blocker.impact,
    actions: []
  }))

const buildClearBlockerRow = (): GovernanceRow => ({
  id: 'blocker-clear',
  item: '阻断项',
  source: '检查结果',
  status: 'CLEAR',
  statusType: 'success',
  keyFields: '暂无阻断',
  evidence: '—',
  blockerImpact: '—',
  actions: []
})

const rows = computed<GovernanceRow[]>(() => {
  const blockerRows = buildBlockerRows(csvResult.value?.blockers || [])
  return [
    {
      id: 'release-gate',
      item: '发布门禁',
      source: displayValue(csvForm.releaseId),
      status: csvResult.value?.status || '未执行',
      statusType: statusTagType(csvResult.value?.status),
      keyFields: `质量负责人 ${displayValue(csvForm.qualityOwner)}；工程验证 ${csvForm.engineeringVerificationPassed ? 'PASS' : 'BLOCKED'}`,
      evidence: displayValue(csvForm.recoveryEvidenceRef || csvForm.qaApproval.signatureEvidenceRef),
      blockerImpact: csvError.value || csvResult.value?.blockers?.[0]?.impact || '—',
      actions: [{ label: '评估', loading: csvLoading.value, handler: evaluateCsvGate }]
    },
    {
      id: 'trace-materials',
      item: '材料追溯',
      source: displayValue(csvForm.materials[0].documentId),
      status: csvForm.materials[0].sourceEvidence && csvForm.traceRelations[0].testRef ? 'READY' : '等待来源',
      statusType: csvForm.materials[0].sourceEvidence && csvForm.traceRelations[0].testRef ? 'success' : 'warning',
      keyFields: `类型 ${displayValue(csvForm.materials[0].type)}；版本 ${displayValue(csvForm.materials[0].version)}；测试 ${displayValue(csvForm.traceRelations[0].testRef)}`,
      evidence: displayValue(csvForm.materials[0].sourceEvidence || csvForm.traceRelations[0].evidenceRef),
      blockerImpact: '—',
      actions: [{ label: '来源', loading: csvSourceLoading.value, handler: loadCsvSourceCandidates }]
    },
    {
      id: 'training-change',
      item: '培训变更',
      source: `培训 ${displayValue(csvForm.trainingRecords[0].trainingId)} / 变更 ${displayValue(csvForm.changeControls[0].changeControlId)}`,
      status: csvForm.trainingRecords[0].evidenceRef && csvForm.changeControls[0].evidenceRef ? 'READY' : '等待来源',
      statusType: csvForm.trainingRecords[0].evidenceRef && csvForm.changeControls[0].evidenceRef ? 'success' : 'warning',
      keyFields: `SOP ${displayValue(csvForm.trainingRecords[0].sopDocumentId)}；变更状态 ${displayValue(csvForm.changeControls[0].status)}`,
      evidence: displayValue(csvForm.trainingRecords[0].evidenceRef || csvForm.changeControls[0].evidenceRef),
      blockerImpact: '—',
      actions: [{ label: '来源', loading: csvSourceLoading.value, handler: loadCsvSourceCandidates }]
    },
    {
      id: 'qa-approval',
      item: 'QA批准',
      source: displayValue(csvForm.qaApproval.approvalRef),
      status: csvResult.value?.qaApproved ? 'APPROVED' : displayValue(csvForm.qaApproval.status),
      statusType: statusTagType(csvResult.value?.qaApproved ? 'APPROVED' : csvForm.qaApproval.status),
      keyFields: `批准人 ${displayValue(csvForm.qaApproval.approver)}`,
      evidence: displayValue(csvForm.qaApproval.signatureEvidenceRef),
      blockerImpact: csvResult.value?.qaApproved === false ? 'QA批准未通过' : '—',
      actions: [{ label: '来源', loading: csvSourceLoading.value, handler: loadCsvSourceCandidates }]
    },
    ...(blockerRows.length > 0 ? blockerRows : [buildClearBlockerRow()])
  ]
})

const normalizeFilterText = (value: unknown) => String(value ?? '').trim().toLowerCase()

const isMatchedRow = (row: GovernanceRow, quickFilter?: TableQuickFilterValue) => {
  if (!quickFilter) return true
  const actual = normalizeFilterText(row[quickFilter.fieldKey as keyof GovernanceRow])
  const expected = normalizeFilterText(quickFilter.value)
  return quickFilter.operator === 'eq' ? actual === expected : actual.includes(expected)
}

const filteredRows = computed(() => rows.value.filter((row) => isMatchedRow(row, queryParams.quickFilter)))

const pagedRows = computed(() => {
  const start = (queryParams.pageNo - 1) * queryParams.pageSize
  return filteredRows.value.slice(start, start + queryParams.pageSize)
})

const handlePagination = () => {
  if ((queryParams.pageNo - 1) * queryParams.pageSize >= filteredRows.value.length) {
    queryParams.pageNo = 1
  }
}

const quickFilter = useTableQuickFilter(
  'signature.governance.csvPackage.list',
  filterDefinitions,
  queryParams,
  handlePagination
)

watch(rows, () => handlePagination())

onMounted(() => {
  void loadDccSignatureCandidates()
})
</script>

<style scoped>
.signature-governance-list-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.csv-evidence-entrypoints {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 14px 16px;
  background: var(--el-bg-color);
}

.csv-evidence-entrypoints__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.csv-evidence-entrypoints__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.csv-evidence-entrypoints__hint {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.csv-evidence-entrypoints__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.csv-evidence-entrypoints__item {
  display: flex;
  min-height: 126px;
  flex-direction: column;
  gap: 8px;
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 8px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
}

.csv-evidence-entrypoints__item-title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.csv-evidence-entrypoints__item-desc {
  flex: 1;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.5;
}

.csv-evidence-entrypoints__item-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.signature-governance-list-pane__select {
  width: 320px;
}

@media (max-width: 1200px) {
  .csv-evidence-entrypoints__grid {
    grid-template-columns: 1fr;
  }
}
</style>
