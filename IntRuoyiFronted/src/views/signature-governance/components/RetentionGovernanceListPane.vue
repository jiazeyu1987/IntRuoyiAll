<template>
  <section class="signature-governance-list-pane">
    <UnifiedListTemplate
      table-key="signature.governance.retention.list"
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
        <el-button :loading="refreshLoading" @click="refreshRetentionSources">刷新</el-button>
        <el-button type="primary" @click="applySelectedDccSignatureCandidate">应用</el-button>
        <el-button :loading="edhrArchiveCandidateLoading" @click="loadEdhrArchiveCandidate">归档</el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          data-user-table-column-explicit
          data-user-table-key="signature.governance.retention.list"
          :data="pagedRows"
          :empty-text="retentionError || candidateAutoFillError || '暂无长期留存记录'"
          :show-overflow-tooltip="true"
          @header-dragend="handleHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isColumnVisible('item')"
            label="事项"
            prop="item"
            :width="getColumnWidthString('item', 130)"
            v-bind="sortColumnAttrs('item')"
          />
          <el-table-column
            v-if="isColumnVisible('source')"
            label="来源"
            prop="source"
            :min-width="getColumnMinWidthString('source', 190)"
            v-bind="sortColumnAttrs('source')"
          />
          <el-table-column
            v-if="isColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getColumnWidthString('status', 130)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.statusType" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColumnVisible('keyFields')"
            label="关键字段"
            prop="keyFields"
            :min-width="getColumnMinWidthString('keyFields', 300)"
            v-bind="sortColumnAttrs('keyFields')"
          />
          <el-table-column
            v-if="isColumnVisible('evidence')"
            label="证据"
            prop="evidence"
            :min-width="getColumnMinWidthString('evidence', 240)"
            v-bind="sortColumnAttrs('evidence')"
          />
          <el-table-column
            v-if="isColumnVisible('blockerImpact')"
            label="阻断影响"
            prop="blockerImpact"
            :min-width="getColumnMinWidthString('blockerImpact', 260)"
            v-bind="sortColumnAttrs('blockerImpact')"
          />
          <el-table-column
            v-if="isColumnVisible('operation')"
            label="操作"
            fixed="right"
            :width="getColumnWidthString('operation', 150)"
          >
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
  createDccSignatureRetentionReceipt,
  createEdhrArchiveRetentionReceipt,
  precheckSignatureRetention,
  runSignatureRecoveryRehearsal,
  type SignatureGovernanceRecoveryRehearsalRespVO,
  type SignatureGovernanceRecoverySampleType,
  type SignatureGovernanceRetentionPrecheckRespVO,
  type SignatureGovernanceRetentionReceiptRespVO
} from '@/api/signature-governance/retention'
import {
  SIGNATURE_GOVERNANCE_PERMISSIONS,
  type SignatureGovernanceBlocker
} from '@/api/signature-governance/shared'
import { useUserStore } from '@/store/modules/user'
import {
  getDccElectronicSignaturePage,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'
import { getFileConfigPage, type FileConfigVO } from '@/api/infra/fileConfig'
import {
  getEdhrExecutionArchivePage,
  type ProFeedbackEdhrExecutionArchiveRespVO
} from '@/api/mes/pro/edhr/archive'

defineOptions({ name: 'RetentionGovernanceListPane' })

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
  { key: 'operation', label: '操作', width: 150 }
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
} = useUserTableColumns('signature.governance.retention.list', defaultColumns)

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
const edhrArchiveCandidateLoading = ref(false)
const refreshLoading = ref(false)

const retentionLoading = ref(false)
const retentionError = ref('')
const retentionResult = ref<SignatureGovernanceRetentionPrecheckRespVO>()
const dccReceiptLoading = ref(false)
const dccReceiptResult = ref<SignatureGovernanceRetentionReceiptRespVO>()
const edhrReceiptLoading = ref(false)
const edhrReceiptResult = ref<SignatureGovernanceRetentionReceiptRespVO>()
const recoveryLoading = ref(false)
const recoveryResult = ref<SignatureGovernanceRecoveryRehearsalRespVO>()

const retentionForm = reactive({
  endpoint: '',
  bucketName: '',
  objectLockEnabled: false,
  versioningEnabled: false,
  defaultRetentionEnabled: false,
  retentionMode: 'GOVERNANCE',
  permissionsVerified: false,
  ownerUserId: undefined as number | undefined,
  sampleDccSignatureId: undefined as number | undefined,
  sampleEdhrArchiveId: undefined as number | undefined
})

const dccReceiptForm = reactive({
  sourceId: undefined as number | undefined,
  objectKey: '',
  versionId: '',
  retentionMode: 'GOVERNANCE',
  retainUntil: '',
  sha256: '',
  evidenceHash: '',
  auditEventId: ''
})

const edhrReceiptForm = reactive({
  sourceId: undefined as number | undefined,
  objectKey: '',
  versionId: '',
  retentionMode: 'GOVERNANCE',
  retainUntil: '',
  sha256: '',
  archiveSha256: '',
  signatureHash: '',
  auditEventId: ''
})

const recoveryForm = reactive({
  backupId: '',
  recoveryRuntime: '',
  ownerReviewed: false,
  reportWritten: false,
  auditWritten: false,
  samples: [
    {
      sampleType: 'DCC_SIGNATURE' as SignatureGovernanceRecoverySampleType,
      objectKey: '',
      versionId: '',
      expectedSha256: '',
      restoredSha256: '',
      expectedDomainHash: '',
      restoredDomainHash: ''
    }
  ]
})

const displayValue = (value: unknown) => {
  if (value === undefined || value === null) return '等待来源'
  const text = String(value).trim()
  return text || '等待来源'
}

const boolText = (value: boolean) => value ? '是' : '否'

const statusTagType = (status?: string) => {
  if (['READY', 'GO', 'COLLECTED', 'ALLOWED', 'RECORDED', 'PASSED'].includes(String(status))) {
    return 'success'
  }
  if (status === 'BLOCKED') return 'danger'
  if (!status || status === '等待来源') return 'warning'
  return 'info'
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const requireText = (value: string, label: string) => {
  const trimmed = value.trim()
  if (!trimmed) throw new Error(`${label}不能为空`)
  return trimmed
}

const requireNumber = (value: number | undefined, label: string) => {
  if (!value) throw new Error(`${label}不能为空`)
  return value
}

const failFast = (messageText: string, errorRef: { value: string }) => {
  errorRef.value = messageText
  message.error(messageText)
}

const formatFileConfigEndpoint = (config: FileConfigVO) => {
  const clientConfig = config.config
  return clientConfig.endpoint || clientConfig.domain || clientConfig.host || clientConfig.basePath || ''
}

const loadRetentionStorageConfig = async () => {
  const page = await getFileConfigPage({ pageNo: 1, pageSize: 100 })
  const configs = (page.list || []) as FileConfigVO[]
  const config = configs.find((item) => item.master) || configs[0]
  if (!config) return '当前没有可用于自动生成留存配置的文件主配置'
  retentionForm.endpoint = formatFileConfigEndpoint(config)
  retentionForm.bucketName = config.config?.bucket || ''
  retentionForm.retentionMode = retentionForm.retentionMode || 'GOVERNANCE'
  const missing = [
    ['Endpoint', retentionForm.endpoint],
    ['Bucket', retentionForm.bucketName]
  ].filter(([, value]) => !String(value || '').trim()).map(([label]) => label)
  return missing.length > 0 ? `文件主配置缺少${missing.join('、')}` : ''
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

const resolveCandidateObjectKey = (candidate: DccElectronicSignatureVO) =>
  candidate.controlledCopyObjectKey || candidate.sourceObjectKey || ''

const resolveCandidateVersionId = (candidate: DccElectronicSignatureVO) =>
  candidate.controlledCopyVersionId || candidate.sourceVersionId || candidate.versionNo || ''

const resolveCandidateContentHash = (candidate: DccElectronicSignatureVO) =>
  candidate.controlledCopyHash || candidate.sourceFileHash || candidate.evidenceHash || ''

const resolveCandidateEvidenceHash = (candidate: DccElectronicSignatureVO) =>
  candidate.evidenceHash || candidate.controlledCopyHash || candidate.sourceFileHash || ''

const collectCandidateMissingFields = (candidate: DccElectronicSignatureVO) => {
  const missing: string[] = []
  if (!resolveCandidateObjectKey(candidate)) missing.push('存储对象Key')
  if (!resolveCandidateVersionId(candidate)) missing.push('版本ID')
  if (!resolveCandidateContentHash(candidate)) missing.push('内容Hash')
  if (!resolveCandidateEvidenceHash(candidate)) missing.push('证据Hash')
  if (!candidate.taskId) missing.push('审计事件')
  return missing
}

const applyDccSignatureCandidate = (candidate: DccElectronicSignatureVO) => {
  const objectKey = resolveCandidateObjectKey(candidate)
  const versionId = resolveCandidateVersionId(candidate)
  const contentHash = resolveCandidateContentHash(candidate)
  const evidenceHash = resolveCandidateEvidenceHash(candidate)
  retentionForm.ownerUserId = candidate.signerUserId
  retentionForm.sampleDccSignatureId = candidate.id
  dccReceiptForm.sourceId = candidate.id
  dccReceiptForm.objectKey = objectKey
  dccReceiptForm.versionId = versionId
  dccReceiptForm.sha256 = contentHash
  dccReceiptForm.evidenceHash = evidenceHash
  dccReceiptForm.auditEventId = candidate.taskId || ''
  const missing = collectCandidateMissingFields(candidate)
  candidateAutoFillError.value = missing.length > 0
    ? `当前签名记录缺少可回填的${missing.join('、')}，请先补齐真实留存证据后再记录回执`
    : ''
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

const loadEdhrArchiveCandidate = async () => {
  edhrArchiveCandidateLoading.value = true
  retentionError.value = ''
  try {
    const page = await getEdhrExecutionArchivePage({ pageNo: 1, pageSize: 1, archiveStatus: 'SEALED' })
    const archive = (page.list || [])[0] as ProFeedbackEdhrExecutionArchiveRespVO | undefined
    if (!archive) {
      failFast('当前没有可用于自动回填的真实eDHR归档样本', retentionError)
      return
    }
    edhrReceiptForm.sourceId = archive.id
    edhrReceiptForm.objectKey = archive.fileName || archive.archiveCode || String(archive.fileId || '')
    edhrReceiptForm.versionId = String(archive.archiveVersion || archive.renderSourceVersion || '')
    edhrReceiptForm.sha256 = archive.sha256 || ''
    edhrReceiptForm.archiveSha256 = archive.executionSnapshotHash || archive.sha256 || ''
    edhrReceiptForm.signatureHash = archive.signatureHash || archive.approvalSnapshotHash || ''
    edhrReceiptForm.auditEventId = String(archive.sealSignatureId || archive.approvalSnapshotId || archive.id)
    retentionForm.sampleEdhrArchiveId = archive.id
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, 'eDHR归档样本加载失败')
  } finally {
    edhrArchiveCandidateLoading.value = false
  }
}

const refreshRetentionSources = async () => {
  refreshLoading.value = true
  retentionError.value = ''
  const messages: string[] = []
  try {
    const storageMessage = await loadRetentionStorageConfig()
    if (storageMessage) messages.push(storageMessage)
    await loadDccSignatureCandidates()
    if (messages.length > 0) retentionError.value = messages.join('；')
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, '长期留存来源刷新失败')
  } finally {
    refreshLoading.value = false
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

const runRetentionPrecheck = async () => {
  retentionLoading.value = true
  retentionError.value = ''
  try {
    retentionResult.value = await precheckSignatureRetention({
      endpoint: retentionForm.endpoint.trim(),
      bucketName: retentionForm.bucketName.trim(),
      objectLockEnabled: retentionForm.objectLockEnabled,
      versioningEnabled: retentionForm.versioningEnabled,
      defaultRetentionEnabled: retentionForm.defaultRetentionEnabled,
      retentionMode: retentionForm.retentionMode.trim() || undefined,
      permissionsVerified: retentionForm.permissionsVerified,
      ownerUserId: retentionForm.ownerUserId,
      sampleDccSignatureId: retentionForm.sampleDccSignatureId,
      sampleEdhrArchiveId: retentionForm.sampleEdhrArchiveId
    })
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, '留存预检失败')
  } finally {
    retentionLoading.value = false
  }
}

const createDccRetentionReceipt = async () => {
  dccReceiptLoading.value = true
  retentionError.value = ''
  try {
    dccReceiptResult.value = await createDccSignatureRetentionReceipt({
      sourceId: requireNumber(dccReceiptForm.sourceId, 'DCC回执来源ID'),
      objectKey: requireText(dccReceiptForm.objectKey, 'DCC对象Key'),
      versionId: requireText(dccReceiptForm.versionId, 'DCC版本ID'),
      retentionMode: requireText(dccReceiptForm.retentionMode, 'DCC保留模式'),
      retainUntil: requireText(dccReceiptForm.retainUntil, 'DCC保留到'),
      sha256: requireText(dccReceiptForm.sha256, 'DCC SHA256'),
      evidenceHash: requireText(dccReceiptForm.evidenceHash, 'DCC证据Hash'),
      auditEventId: requireText(dccReceiptForm.auditEventId, 'DCC审计事件')
    })
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, 'DCC留存回执记录失败')
  } finally {
    dccReceiptLoading.value = false
  }
}

const createEdhrRetentionReceipt = async () => {
  edhrReceiptLoading.value = true
  retentionError.value = ''
  try {
    edhrReceiptResult.value = await createEdhrArchiveRetentionReceipt({
      sourceId: requireNumber(edhrReceiptForm.sourceId, 'eDHR回执来源ID'),
      objectKey: requireText(edhrReceiptForm.objectKey, 'eDHR对象Key'),
      versionId: requireText(edhrReceiptForm.versionId, 'eDHR版本ID'),
      retentionMode: requireText(edhrReceiptForm.retentionMode, 'eDHR保留模式'),
      retainUntil: requireText(edhrReceiptForm.retainUntil, 'eDHR保留到'),
      sha256: requireText(edhrReceiptForm.sha256, 'eDHR SHA256'),
      archiveSha256: requireText(edhrReceiptForm.archiveSha256, 'eDHR归档Hash'),
      signatureHash: requireText(edhrReceiptForm.signatureHash, 'eDHR签名Hash'),
      auditEventId: requireText(edhrReceiptForm.auditEventId, 'eDHR审计事件')
    })
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, 'eDHR留存回执记录失败')
  } finally {
    edhrReceiptLoading.value = false
  }
}

const runRecoveryRehearsal = async () => {
  recoveryLoading.value = true
  retentionError.value = ''
  try {
    const sample = recoveryForm.samples[0]
    recoveryResult.value = await runSignatureRecoveryRehearsal({
      backupId: requireText(recoveryForm.backupId, '恢复备份ID'),
      recoveryRuntime: requireText(recoveryForm.recoveryRuntime, '恢复环境'),
      ownerReviewed: recoveryForm.ownerReviewed,
      reportWritten: recoveryForm.reportWritten,
      auditWritten: recoveryForm.auditWritten,
      samples: [{
        sampleType: sample.sampleType,
        objectKey: requireText(sample.objectKey, '恢复对象Key'),
        versionId: requireText(sample.versionId, '恢复版本ID'),
        expectedSha256: requireText(sample.expectedSha256, '预期SHA256'),
        restoredSha256: requireText(sample.restoredSha256, '实际SHA256'),
        expectedDomainHash: requireText(sample.expectedDomainHash, '预期业务Hash'),
        restoredDomainHash: requireText(sample.restoredDomainHash, '实际业务Hash')
      }]
    })
  } catch (error) {
    retentionError.value = resolveErrorMessage(error, '恢复演练记录失败')
  } finally {
    recoveryLoading.value = false
  }
}

const buildBlockerRows = (blockers: SignatureGovernanceBlocker[], prefix: string) =>
  blockers.map((blocker, index): GovernanceRow => ({
    id: `${prefix}-${index}-${blocker.code}`,
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
  const result: GovernanceRow[] = [
    {
      id: 'retention-policy',
      item: '留存策略',
      source: `${displayValue(retentionForm.endpoint)} / ${displayValue(retentionForm.bucketName)}`,
      status: retentionResult.value?.status || '等待来源',
      statusType: statusTagType(retentionResult.value?.status),
      keyFields: `模式 ${displayValue(retentionForm.retentionMode)}；Object Lock ${boolText(retentionForm.objectLockEnabled)}；Versioning ${boolText(retentionForm.versioningEnabled)}`,
      evidence: `权限 ${retentionForm.permissionsVerified ? '已确认' : '等待确认'}`,
      blockerImpact: retentionError.value || candidateAutoFillError.value || '—',
      actions: [{ label: '预检', loading: retentionLoading.value, handler: runRetentionPrecheck }]
    },
    {
      id: 'dcc-receipt',
      item: 'DCC回执',
      source: `来源ID ${displayValue(dccReceiptForm.sourceId)}`,
      status: dccReceiptResult.value?.status || '等待来源',
      statusType: statusTagType(dccReceiptResult.value?.status),
      keyFields: `对象 ${displayValue(dccReceiptForm.objectKey)}；版本 ${displayValue(dccReceiptForm.versionId)}`,
      evidence: displayValue(dccReceiptForm.evidenceHash),
      blockerImpact: dccReceiptResult.value?.blockers?.[0]?.impact || '—',
      actions: [{ label: '记录', loading: dccReceiptLoading.value, handler: createDccRetentionReceipt }]
    },
    {
      id: 'edhr-receipt',
      item: 'eDHR回执',
      source: `来源ID ${displayValue(edhrReceiptForm.sourceId)}`,
      status: edhrReceiptResult.value?.status || '等待来源',
      statusType: statusTagType(edhrReceiptResult.value?.status),
      keyFields: `对象 ${displayValue(edhrReceiptForm.objectKey)}；版本 ${displayValue(edhrReceiptForm.versionId)}`,
      evidence: displayValue(edhrReceiptForm.archiveSha256 || edhrReceiptForm.signatureHash),
      blockerImpact: edhrReceiptResult.value?.blockers?.[0]?.impact || '—',
      actions: [{ label: '记录', loading: edhrReceiptLoading.value, handler: createEdhrRetentionReceipt }]
    },
    {
      id: 'recovery-rehearsal',
      item: '恢复演练',
      source: displayValue(recoveryForm.recoveryRuntime),
      status: recoveryResult.value?.status || '等待来源',
      statusType: statusTagType(recoveryResult.value?.status),
      keyFields: `备份 ${displayValue(recoveryForm.backupId)}；对象 ${displayValue(recoveryForm.samples[0].objectKey)}`,
      evidence: `预期 ${displayValue(recoveryForm.samples[0].expectedSha256)} / 实际 ${displayValue(recoveryForm.samples[0].restoredSha256)}`,
      blockerImpact: recoveryResult.value?.blockers?.[0]?.impact || '—',
      actions: [{ label: '演练', loading: recoveryLoading.value, handler: runRecoveryRehearsal }]
    }
  ]
  const blockerRows = [
    ...buildBlockerRows(retentionResult.value?.blockers || [], 'retention'),
    ...buildBlockerRows(dccReceiptResult.value?.blockers || [], 'dcc'),
    ...buildBlockerRows(edhrReceiptResult.value?.blockers || [], 'edhr'),
    ...buildBlockerRows(recoveryResult.value?.blockers || [], 'recovery')
  ]
  return [
    ...result,
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
  'signature.governance.retention.list',
  filterDefinitions,
  queryParams,
  handlePagination
)

watch(rows, () => handlePagination())

onMounted(() => {
  void refreshRetentionSources()
})
</script>

<style scoped>
.signature-governance-list-pane__select {
  width: 320px;
}
</style>
