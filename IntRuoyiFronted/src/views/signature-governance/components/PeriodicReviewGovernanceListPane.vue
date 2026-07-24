<template>
  <section class="signature-governance-list-pane">
    <UnifiedListTemplate
      table-key="signature.governance.periodicReview.list"
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
        <el-form-item label="审阅样本">
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
        <el-button :loading="refreshLoading" @click="refreshReviewSources">刷新</el-button>
        <el-button type="primary" @click="applySelectedDccSignatureCandidate">应用</el-button>
        <el-button
          v-hasPermi="[SIGNATURE_GOVERNANCE_PERMISSIONS.PERIODIC_REVIEW_MANAGE]"
          type="primary"
          :loading="reviewLoading"
          @click="createReviewBatch"
        >
          创建
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          data-user-table-column-explicit
          data-user-table-key="signature.governance.periodicReview.list"
          :data="pagedRows"
          :empty-text="reviewError || candidateAutoFillError || '暂无周期复核记录'"
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
          <el-table-column v-if="isColumnVisible('operation')" label="操作" fixed="right" :width="getColumnWidthString('operation', 120)">
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
  createSignaturePeriodicReviewBatch,
  type SignatureGovernanceReviewBatchRespVO,
  type SignatureGovernanceReviewFindingCode
} from '@/api/signature-governance/periodicReview'
import {
  getCurrentSignatureGovernancePolicy,
  type SignatureGovernancePolicyCurrentRespVO
} from '@/api/signature-governance/policy'
import {
  SIGNATURE_GOVERNANCE_PERMISSIONS,
  type SignatureGovernanceBlocker,
  type SignatureGovernanceModuleCode
} from '@/api/signature-governance/shared'
import { useUserStore } from '@/store/modules/user'
import {
  getDccElectronicSignaturePage,
  type DccElectronicSignatureVO
} from '@/api/dcc/controlledFile/signatures'

defineOptions({ name: 'PeriodicReviewGovernanceListPane' })

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
  { key: 'operation', label: '操作', width: 120 }
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
} = useUserTableColumns('signature.governance.periodicReview.list', defaultColumns)

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

const refreshLoading = ref(false)
const dccSignatureCandidateLoading = ref(false)
const dccSignatureCandidates = ref<DccElectronicSignatureVO[]>([])
const selectedDccSignatureId = ref<number>()
const candidateAutoFillError = ref('')
const reviewLoading = ref(false)
const reviewError = ref('')
const reviewResult = ref<SignatureGovernanceReviewBatchRespVO>()
const policyResult = ref<SignatureGovernancePolicyCurrentRespVO>()

const reviewForm = reactive({
  reviewOwner: '',
  periodCode: '',
  ruleVersion: '',
  dueDate: '',
  scopeModules: [] as SignatureGovernanceModuleCode[],
  permittedModules: [] as SignatureGovernanceModuleCode[],
  projections: [
    {
      moduleCode: 'DCC' as SignatureGovernanceModuleCode,
      sourceTable: '',
      sourceId: '',
      sourceHash: '',
      actionCode: '',
      meaningCode: '',
      findingCode: 'VALID' as SignatureGovernanceReviewFindingCode
    }
  ],
  reviewSignatureStrategyConfigured: false
})

const displayValue = (value: unknown) => {
  if (value === undefined || value === null) return '等待来源'
  const text = String(value).trim()
  return text || '等待来源'
}

const arrayText = (value: unknown[]) => value.length > 0 ? value.join('、') : '等待来源'

const statusTagType = (status?: string) => {
  if (['READY', 'GO', 'COLLECTED', 'ALLOWED', 'RECORDED', 'PASSED', 'SIGNED', 'CLOSED'].includes(String(status))) return 'success'
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

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const currentQuarterCode = () => {
  const now = new Date()
  return `${now.getFullYear()}Q${Math.floor(now.getMonth() / 3) + 1}`
}

const currentQuarterEndDate = () => {
  const now = new Date()
  const quarterEndMonth = Math.floor(now.getMonth() / 3) * 3 + 2
  return formatDate(new Date(now.getFullYear(), quarterEndMonth + 1, 0))
}

const applyPolicyGeneratedDefaults = () => {
  const modules = policyResult.value?.modules || (policyResult.value?.moduleStatuses || []).map((item) => item.moduleCode)
  const moduleStatuses = policyResult.value?.moduleStatuses || []
  const policyVersions = moduleStatuses
    .map((item) => item.policyVersion || item.adapterVersion || item.policySourceCode || '')
    .filter(Boolean)
  reviewForm.periodCode = currentQuarterCode()
  reviewForm.ruleVersion = policyVersions[0] || ''
  reviewForm.dueDate = currentQuarterEndDate()
  reviewForm.scopeModules = [...modules]
  reviewForm.permittedModules = moduleStatuses.filter((item) => item.authorityConfirmed).map((item) => item.moduleCode)
  reviewForm.reviewSignatureStrategyConfigured = moduleStatuses.length > 0
    ? moduleStatuses.every((item) => item.policySourcePresent && item.authorityConfirmed)
    : false
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

const resolveCandidateEvidenceHash = (candidate: DccElectronicSignatureVO) =>
  candidate.evidenceHash || candidate.controlledCopyHash || candidate.sourceFileHash || ''

const loadCurrentPolicy = async () => {
  policyResult.value = await getCurrentSignatureGovernancePolicy()
  applyPolicyGeneratedDefaults()
}

const applyDccSignatureCandidate = (candidate: DccElectronicSignatureVO) => {
  const projection = reviewForm.projections[0]
  projection.moduleCode = 'DCC'
  projection.sourceTable = 'dcc_controlled_file_signature'
  projection.sourceId = String(candidate.id)
  projection.sourceHash = resolveCandidateEvidenceHash(candidate)
  projection.actionCode = candidate.taskActionResult || ''
  projection.meaningCode = candidate.meaningCode || ''
  const missing = [
    ['审阅来源Hash', projection.sourceHash],
    ['审阅动作', projection.actionCode],
    ['审阅含义', projection.meaningCode]
  ].filter(([, value]) => !String(value || '').trim()).map(([label]) => label)
  candidateAutoFillError.value = missing.length > 0
    ? `当前签名记录缺少${missing.join('、')}，请先补齐真实审阅证据`
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

const refreshReviewSources = async () => {
  refreshLoading.value = true
  reviewError.value = ''
  try {
    await loadCurrentPolicy()
    await loadDccSignatureCandidates()
  } catch (error) {
    reviewError.value = resolveErrorMessage(error, '周期复核来源刷新失败')
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

const buildReviewProjections = () => {
  const projection = reviewForm.projections[0]
  return [{
    moduleCode: projection.moduleCode,
    sourceTable: requireText(projection.sourceTable, '审阅来源表'),
    sourceId: requireText(projection.sourceId, '审阅来源ID'),
    sourceHash: requireText(projection.sourceHash, '审阅来源Hash'),
    actionCode: requireText(projection.actionCode, '审阅动作'),
    meaningCode: requireText(projection.meaningCode, '审阅含义'),
    findingCode: projection.findingCode
  }]
}

const validateReviewForm = () => {
  try {
    buildReviewProjections()
    return true
  } catch (error) {
    failFast(resolveErrorMessage(error, '周期审阅缺少真实投影样本'), reviewError)
    return false
  }
}

const createReviewBatch = async () => {
  if (!validateReviewForm()) return
  reviewLoading.value = true
  reviewError.value = ''
  try {
    reviewResult.value = await createSignaturePeriodicReviewBatch({
      reviewOwner: trimOptional(reviewForm.reviewOwner),
      periodCode: trimOptional(reviewForm.periodCode),
      ruleVersion: trimOptional(reviewForm.ruleVersion),
      dueDate: trimOptional(reviewForm.dueDate),
      scopeModules: reviewForm.scopeModules,
      permittedModules: reviewForm.permittedModules,
      projections: buildReviewProjections(),
      reviewSignatureStrategyConfigured: reviewForm.reviewSignatureStrategyConfigured
    })
  } catch (error) {
    reviewError.value = resolveErrorMessage(error, '周期审阅批次创建失败')
  } finally {
    reviewLoading.value = false
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
  const projection = reviewForm.projections[0]
  const blockerRows = buildBlockerRows(reviewResult.value?.blockers || [])
  return [
    {
      id: 'review-plan',
      item: '复核计划',
      source: arrayText(reviewForm.scopeModules),
      status: reviewForm.reviewSignatureStrategyConfigured ? 'READY' : '等待策略',
      statusType: reviewForm.reviewSignatureStrategyConfigured ? 'success' : 'warning',
      keyFields: `周期 ${displayValue(reviewForm.periodCode)}；规则 ${displayValue(reviewForm.ruleVersion)}；到期 ${displayValue(reviewForm.dueDate)}`,
      evidence: `授权模块 ${arrayText(reviewForm.permittedModules)}`,
      blockerImpact: reviewError.value || candidateAutoFillError.value || '—',
      actions: []
    },
    {
      id: 'review-projection',
      item: '审阅投影',
      source: `${displayValue(projection.sourceTable)} / ${displayValue(projection.sourceId)}`,
      status: projection.sourceHash ? 'READY' : '等待来源',
      statusType: projection.sourceHash ? 'success' : 'warning',
      keyFields: `模块 ${displayValue(projection.moduleCode)}；动作 ${displayValue(projection.actionCode)}；发现 ${displayValue(projection.findingCode)}`,
      evidence: displayValue(projection.sourceHash),
      blockerImpact: '—',
      actions: []
    },
    {
      id: 'review-batch',
      item: '复核批次',
      source: displayValue(reviewResult.value?.batchId),
      status: reviewResult.value?.status || '未执行',
      statusType: statusTagType(reviewResult.value?.status),
      keyFields: `快照 ${displayValue(reviewResult.value?.snapshotHash)}`,
      evidence: `样本 ${reviewResult.value?.snapshotItems?.length || 0}`,
      blockerImpact: reviewResult.value?.blockers?.[0]?.impact || '—',
      actions: [{ label: '创建', loading: reviewLoading.value, handler: createReviewBatch }]
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
  'signature.governance.periodicReview.list',
  filterDefinitions,
  queryParams,
  handlePagination
)

watch(rows, () => handlePagination())

onMounted(() => {
  void refreshReviewSources()
})
</script>

<style scoped>
.signature-governance-list-pane__select {
  width: 320px;
}
</style>
