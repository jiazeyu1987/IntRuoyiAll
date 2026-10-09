<template>
  <el-dialog
    v-model="visible"
    title="版本审核 / 迁移审查"
    width="min(1180px, calc(100vw - 32px))"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="!busy"
  >
    <section
      v-loading="loading"
      data-batch-record-version-review
      :data-version-id="target?.versionId"
    >
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
        data-version-review-error
      />
      <el-alert v-if="notice" :title="notice" type="info" :closable="false" show-icon />
      <el-descriptions v-if="version" :column="2" border>
        <el-descriptions-item label="表单">{{ version.reportName }}</el-descriptions-item>
        <el-descriptions-item label="批记录">{{ version.batchRecordName }}</el-descriptions-item>
        <el-descriptions-item label="正式版本"
          >{{ version.versionNo }}（{{ version.batchRecordVersionId }}）</el-descriptions-item
        >
        <el-descriptions-item label="版本状态">{{
          statusLabel(version.versionStatus)
        }}</el-descriptions-item>
      </el-descriptions>
      <template v-if="diff">
        <p data-version-review-summary>
          阻断项 {{ diff.blockerCount }} · 需确认项 {{ diff.confirmRequiredCount }} · 已确认项
          {{ diff.confirmedCount }}
        </p>
        <p v-if="!canConfirmVersion()">当前正式版本状态不允许迁移确认，仅可查看审查结果。</p>
        <el-table
          :data="diff.items"
          row-key="itemId"
          max-height="420"
          border
          data-version-review-items
        >
          <el-table-column label="选择" width="70">
            <template #default="{ row }">
              <el-checkbox
                :model-value="selectedIds.includes(String(row.itemId))"
                :disabled="!canSelectItem(row) || busy || Boolean(pendingAttempt)"
                :aria-label="`选择迁移项 ${row.itemId}`"
                :data-migration-item-id="row.itemId"
                @change="toggleItem(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="itemId" label="迁移项" width="90" />
          <el-table-column label="差异" min-width="160">
            <template #default="{ row }"
              >{{ diffLabel(row.diffGroup) }} / {{ diffLabel(row.diffType) }}</template
            >
          </el-table-column>
          <el-table-column label="风险" width="100">
            <template #default="{ row }">{{ riskLabel(row.riskLevel) }}</template>
          </el-table-column>
          <el-table-column label="责任方类型" width="120">
            <template #default="{ row }">{{ ownerLabel(row.businessOwnerType) }}</template>
          </el-table-column>
          <el-table-column
            prop="sourceLogicalKey"
            label="来源逻辑键"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column
            prop="targetLogicalKey"
            label="目标逻辑键"
            min-width="160"
            show-overflow-tooltip
          />
          <el-table-column prop="message" label="差异说明" min-width="220" />
          <el-table-column label="确认事实" min-width="180">
            <template #default="{ row }">
              <span>{{ row.confirmed ? '已确认' : '未确认' }}</span>
              <div v-if="row.confirmed">确认人：{{ row.confirmedBy }} · {{ row.confirmedAt }}</div>
              <div v-if="row.confirmed">{{ row.confirmComment }}</div>
            </template>
          </el-table-column>
          <el-table-column label="匹配证据" min-width="180">
            <template #default="{ row }">
              <span>{{ row.matchConfidence }}</span>
              <div>{{ row.ruleType }}</div>
              <pre class="version-review-evidence">{{ row.matchEvidenceJson }}</pre>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!diff.items.length" description="当前版本无迁移差异项" />
        <el-form label-position="top" class="mt-16px">
          <el-form-item label="确认意见">
            <el-input
              v-model="comment"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              :disabled="
                busy ||
                Boolean(pendingAttempt) ||
                !canConfirmVersion() ||
                !hasPermission([CONFIRM_PERMISSION])
              "
              placeholder="核对选中迁移项后填写确认意见"
              aria-label="迁移确认意见"
              data-version-review-comment
            />
          </el-form-item>
        </el-form>
        <p v-if="pendingAttempt" data-version-review-pending-request>
          原确认迁移项：{{ pendingAttempt.data.itemIds.join('、') }} · 幂等请求标识：
          {{ pendingAttempt.data.idempotencyKey }}
        </p>
        <p>责任方类型为迁移项的正式业务信息；确认与提交审批分别按已有权限执行。</p>
      </template>
    </section>
    <template #footer>
      <el-button :disabled="busy" @click="visible = false">关闭</el-button>
      <el-button :disabled="busy" @click="reload" data-version-review-refresh
        >刷新审查结果</el-button
      >
      <el-button
        v-if="hasPermission([CONFIRM_PERMISSION])"
        type="primary"
        :loading="confirming"
        :disabled="!canConfirm"
        @click="confirmSelected"
        data-version-review-confirm
      >
        {{ pendingAttempt ? '重试原确认请求' : '确认选中迁移项' }}
      </el-button>
      <el-button
        v-if="hasPermission([SUBMIT_PERMISSION])"
        type="primary"
        :loading="submitting"
        :disabled="!canSubmit"
        @click="submitApproval"
        data-version-review-submit
        >提交正式版本审批</el-button
      >
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { hasPermission } from '@/directives/permission/hasPermi'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { BatchRecordReportApi, type BatchRecordReportVO } from '@/api/mes/pro/batchrecordreport'
import {
  BatchRecordVersionGovernanceApi,
  type BatchRecordVersionMigrationDiffVO,
  type BatchRecordVersionMigrationItemVO,
  type BatchRecordVersionMigrationConfirmReqVO
} from '@/api/mes/pro/batchrecordreport/versionGovernance'

const emit = defineEmits<{ changed: [] }>()
const QUERY_PERMISSION = 'mes:pro-batch-record-version:query'
const CONFIRM_PERMISSION = 'mes:pro-batch-record-version:confirm'
const SUBMIT_PERMISSION = 'mes:pro-batch-record-template:version-approve'
const visible = ref(false)
const target = ref<{ versionId: string; reportId: string }>()
const version = ref<BatchRecordReportVO>()
const diff = ref<BatchRecordVersionMigrationDiffVO>()
const loading = ref(false)
const confirming = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const notice = ref('')
const selectedIds = ref<string[]>([])
const comment = ref('')
const pendingAttempt = ref<{ versionId: string; data: BatchRecordVersionMigrationConfirmReqVO }>()
const attempts = new Map<
  string,
  { versionId: string; data: BatchRecordVersionMigrationConfirmReqVO }
>()
const recheckRequired = ref(false)
let generation = 0
const busy = computed(() => loading.value || confirming.value || submitting.value)
const isCurrent = (token: number) => token === generation && visible.value
const errorText = (error: unknown) => {
  if (error instanceof Error) return error.message
  if (typeof error === 'string' && error) return error
  const payload = error as {
    msg?: string
    message?: string
    response?: { data?: { msg?: string } }
  }
  return payload?.response?.data?.msg || payload?.msg || payload?.message || '版本审查请求失败'
}
const label = (values: Record<string, string>, value?: string) =>
  value ? values[value] || value : '未记录'
const statusLabel = (status?: string) =>
  label(
    {
      DRAFT: '草稿',
      PRECHECK_FAILED: '预检未通过',
      PRECHECK_PASSED: '预检通过',
      PENDING_APPROVAL: '审批中',
      APPROVED: '已批准',
      REJECTED: '已驳回',
      VOIDED: '已作废',
      OBSOLETE: '已废止'
    },
    status
  )
const riskLabel = (risk: string) =>
  label({ BLOCKER: '阻断', CONFIRM_REQUIRED: '需确认', SAFE: '可迁移', INFO: '提示' }, risk)
const ownerLabel = (owner?: string) =>
  label(
    {
      DCC_OWNER: 'DCC文控责任方',
      PROCESS_OWNER: '工艺责任方',
      QUALITY_OWNER: '质量责任方',
      SYSTEM: '系统'
    },
    owner
  )
const diffLabel = (value?: string) =>
  label(
    {
      TABLE: '表单结构',
      PROCESS: '工艺路线',
      FIELD: '字段映射',
      SIGNATURE_CELL: '签名位',
      ATTACHMENT_RULE: '附件规则',
      CELL_RULE: '单元格规则',
      FIRST_IMPORT: '首次导入',
      TABLE_STRUCTURE_RECONCILED: '表单结构已核对',
      PROCESS_ROUTE_REBOUND: '工艺路线已重绑定',
      FIELD_MAPPING_REVIEWED: '字段映射已核对',
      SIGNATURE_CELL_REVIEW_REQUIRED: '签名位需确认',
      ATTACHMENT_RULE_RECONCILED: '附件规则已核对',
      CELL_RULE_RECONCILED: '单元格规则已核对',
      SIGNATURE: '签名',
      SIGNATURE_RULE: '签名规则',
      PERMISSION: '权限',
      PERMISSION_SCOPE: '权限范围',
      UNCHANGED: '未变化',
      ADDED: '新增',
      REMOVED: '移除',
      CHANGED: '变更',
      UNMATCHED: '未匹配'
    },
    value
  )
const canConfirmVersion = () =>
  version.value?.versionStatus === 'PRECHECK_FAILED' ||
  version.value?.versionStatus === 'PRECHECK_PASSED'
const canSelectItem = (item: BatchRecordVersionMigrationItemVO) =>
  canConfirmVersion() &&
  hasPermission([CONFIRM_PERMISSION]) &&
  item.riskLevel === 'CONFIRM_REQUIRED' &&
  item.confirmed === false
const toggleItem = (item: BatchRecordVersionMigrationItemVO, checked: unknown) => {
  if (busy.value || pendingAttempt.value || !canSelectItem(item)) return
  const id = parsePositiveRouteQueryId(item.itemId)
  if (!id) {
    errorMessage.value = '迁移项缺少正式身份'
    return
  }
  selectedIds.value =
    checked === true
      ? [...new Set([...selectedIds.value, id])]
      : selectedIds.value.filter((value) => value !== id)
}
const canConfirm = computed(
  () =>
    !busy.value &&
    !recheckRequired.value &&
    !errorMessage.value &&
    Boolean(diff.value) &&
    canConfirmVersion() &&
    hasPermission([CONFIRM_PERMISSION]) &&
    (pendingAttempt.value
      ? true
      : selectedIds.value.length > 0 &&
        comment.value.trim().length > 0 &&
        comment.value.trim().length <= 500)
)
const canSubmit = computed(
  () =>
    !busy.value &&
    !recheckRequired.value &&
    !pendingAttempt.value &&
    !errorMessage.value &&
    hasPermission([SUBMIT_PERMISSION]) &&
    version.value?.versionStatus === 'PRECHECK_PASSED' &&
    diff.value?.approvalReady === true
)

const reload = async () => {
  if (!target.value || confirming.value || submitting.value) return
  const identity = { ...target.value }
  const token = ++generation
  loading.value = true
  errorMessage.value = ''
  version.value = undefined
  diff.value = undefined
  selectedIds.value = []
  try {
    if (!hasPermission([QUERY_PERMISSION])) throw new Error('缺少批记录版本查询权限')
    const [page, result] = await Promise.all([
      BatchRecordReportApi.getGeneratedReportPage(
        { reportId: identity.reportId, pageNo: 1, pageSize: 100, latestVersionOnly: false },
        { ignoreErrorMessage: true }
      ),
      BatchRecordVersionGovernanceApi.getMigrationDiff(identity.versionId)
    ])
    if (!isCurrent(token)) return
    const pageRows: BatchRecordReportVO[] = page?.list
    if (!Array.isArray(pageRows) || Number(page.total) !== pageRows.length)
      throw new Error('表单正式版本查询未返回完整结果，请重新从列表选择版本')
    const rows = pageRows.filter(
      (row) =>
        row.reportId === identity.reportId &&
        parsePositiveRouteQueryId(row.batchRecordVersionId) === identity.versionId
    )
    if (
      !rows.length ||
      rows.some(
        (row) =>
          !row.versionNo ||
          !row.versionStatus ||
          row.versionStatus !== rows[0].versionStatus ||
          row.versionNo !== rows[0].versionNo
      )
    )
      throw new Error('表单当前正式版本缺失或已变化，请重新从列表选择版本')
    if (
      parsePositiveRouteQueryId(result?.versionId) !== identity.versionId ||
      !Array.isArray(result.items) ||
      typeof result.approvalReady !== 'boolean' ||
      [result.blockerCount, result.confirmRequiredCount, result.confirmedCount].some(
        (count) => !Number.isSafeInteger(count) || count < 0
      ) ||
      result.items.some(
        (item) =>
          !parsePositiveRouteQueryId(item.itemId) ||
          typeof item.confirmed !== 'boolean' ||
          typeof item.riskLevel !== 'string' ||
          !item.riskLevel
      ) ||
      new Set(result.items.map((item) => String(item.itemId))).size !== result.items.length
    )
      throw new Error('迁移审查响应与当前版本不一致')
    version.value = rows[0]
    diff.value = result
    if (pendingAttempt.value) {
      selectedIds.value = pendingAttempt.value.data.itemIds.map(String)
      comment.value = pendingAttempt.value.data.comment
      const selected = pendingAttempt.value.data.itemIds.map((id) =>
        result.items.find((item) => String(item.itemId) === String(id))
      )
      if (selected.some((item) => !item)) throw new Error('原确认项已变化，禁止重新提交')
      if (selected.every((item) => item?.confirmed === true)) {
        pendingAttempt.value = undefined
        attempts.delete(identity.versionId)
        comment.value = ''
        selectedIds.value = []
        notice.value = '原确认请求已完成，审查结果已重新核验。'
      } else if (
        selected.some((item) => item?.confirmed !== false || item.riskLevel !== 'CONFIRM_REQUIRED')
      ) {
        throw new Error('原确认集合部分已确认或风险已变化，禁止重新提交')
      } else if (!canConfirmVersion()) {
        notice.value = '当前版本状态不允许迁移确认，原确认请求保留且禁止重试。'
      } else notice.value = '原确认项仍未确认；可明确重试同一幂等请求，确认意见与选项已锁定。'
    }
    recheckRequired.value = false
  } catch (error) {
    if (isCurrent(token)) errorMessage.value = errorText(error)
  } finally {
    if (isCurrent(token)) loading.value = false
  }
}

const open = async (row: BatchRecordReportVO) => {
  if (confirming.value || submitting.value) throw new Error('版本操作进行中，不能切换版本')
  generation += 1
  loading.value = false
  visible.value = true
  target.value = undefined
  pendingAttempt.value = undefined
  recheckRequired.value = false
  notice.value = ''
  errorMessage.value = ''
  comment.value = ''
  version.value = undefined
  diff.value = undefined
  selectedIds.value = []
  const versionId = parsePositiveRouteQueryId(row.batchRecordVersionId)
  if (!versionId || !row.reportId || !row.versionNo || !row.versionStatus) {
    errorMessage.value = '当前表单缺少正式批记录版本，无法审查'
    return
  }
  target.value = { versionId, reportId: row.reportId }
  pendingAttempt.value = attempts.get(versionId)
  await reload()
}

const confirmSelected = async () => {
  if (!canConfirm.value || !target.value || !diff.value) return
  const token = generation
  const identity = { ...target.value }
  const newAttempt = !pendingAttempt.value
  if (!pendingAttempt.value) {
    const items = selectedIds.value.map((id) =>
      diff.value?.items.find((item) => String(item.itemId) === id)
    )
    if (items.some((item) => !item || !canSelectItem(item))) {
      errorMessage.value = '选中项已确认或不允许确认，请刷新审查结果'
      return
    }
    pendingAttempt.value = {
      versionId: identity.versionId,
      data: {
        itemIds: [...selectedIds.value],
        comment: comment.value.trim(),
        idempotencyKey: `batch-version-confirm-${crypto.randomUUID()}`
      }
    }
    attempts.set(identity.versionId, pendingAttempt.value)
  }
  const attempt = pendingAttempt.value
  confirming.value = true
  try {
    try {
      await ElMessageBox.confirm(
        `确认版本 ${version.value?.versionNo} 的 ${attempt.data.itemIds.length} 个迁移项？`,
        '确认迁移项',
        { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
      )
    } catch (action) {
      if (action === 'cancel' || action === 'close') {
        if (isCurrent(token) && newAttempt) {
          pendingAttempt.value = undefined
          attempts.delete(identity.versionId)
        }
        return
      }
      throw action
    }
    if (!isCurrent(token)) return
    const result = await BatchRecordVersionGovernanceApi.confirmMigration(
      identity.versionId,
      attempt.data
    )
    if (!isCurrent(token)) return
    if (
      String(result.versionId) !== identity.versionId ||
      result.idempotencyKey !== attempt.data.idempotencyKey ||
      !Array.isArray(result.confirmedItemIds) ||
      result.confirmedItemIds.map(String).sort().join(',') !==
        attempt.data.itemIds.map(String).sort().join(',')
    )
      throw new Error('迁移确认回执与原请求不一致，须刷新核验')
    notice.value = '迁移确认已提交，正在回读正式结果。提交版本审批仍需单独点击。'
    emit('changed')
  } catch (error) {
    if (isCurrent(token)) {
      errorMessage.value = errorText(error)
      recheckRequired.value = true
      notice.value = '确认未取得有效回执，禁止直接重复提交；请先刷新核验原确认项。'
    }
  } finally {
    if (isCurrent(token)) confirming.value = false
  }
  if (isCurrent(token) && !recheckRequired.value) await reload()
}

const submitApproval = async () => {
  if (!canSubmit.value || !target.value) return
  const token = generation
  const identity = { ...target.value }
  submitting.value = true
  try {
    try {
      await ElMessageBox.confirm(
        `提交正式版本 ${version.value?.versionNo} 审批？`,
        '提交版本审批',
        { type: 'warning', confirmButtonText: '提交审批', cancelButtonText: '取消' }
      )
    } catch (action) {
      if (action === 'cancel' || action === 'close') return
      throw action
    }
    if (!isCurrent(token)) return
    const result = await BatchRecordReportApi.submitBatchRecordVersionApproval(identity.versionId, {
      ignoreErrorMessage: true
    })
    if (!isCurrent(token)) return
    if (
      String(result.versionId) !== identity.versionId ||
      result.processedResult !== 'SUBMITTED' ||
      !result.versionStatus
    )
      throw new Error('版本审批回执不完整，请刷新核验状态')
    notice.value = '正式版本审批已提交，批准结果由现有业务审批流程处理。'
    emit('changed')
  } catch (error) {
    if (isCurrent(token)) {
      errorMessage.value = errorText(error)
      recheckRequired.value = true
    }
  } finally {
    if (isCurrent(token)) submitting.value = false
  }
  if (isCurrent(token) && !recheckRequired.value) await reload()
}

watch(visible, (value) => {
  if (!value) {
    generation += 1
    loading.value = false
  }
})
onBeforeUnmount(() => {
  generation += 1
})
defineExpose({ open })
</script>

<style scoped>
.version-review-evidence {
  max-width: 260px;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  font-size: 12px;
}
</style>
