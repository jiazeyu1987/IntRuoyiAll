<template>
  <section class="signature-governance-list-pane">
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
  getDccElectronicSignaturePage,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'
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

defineOptions({ name: 'CsvPackageGovernanceListPane' })

const DCC_SIGNATURE_MANAGE_PERMISSION = 'dcc:controlled-file:signature:manage'
const ALL_PERMISSION = '*:*:*'
const userStore = useUserStore()
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

const message = useMessage()

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
.signature-governance-list-pane__select {
  width: 320px;
}
</style>
