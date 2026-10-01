<template>
  <div v-loading="loading">
    <el-alert v-if="error" type="error" :closable="false" :title="error" />
    <template v-else-if="detail">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="偏差编号">{{ detail.deviationCode }}</el-descriptions-item>
        <el-descriptions-item label="关联批记录"><el-button v-if="detail.batchExecutionId" link type="primary" @click="openBatch">{{ detail.batchExecutionCode || detail.batchExecutionId }}</el-button><span v-else>—</span></el-descriptions-item>
        <el-descriptions-item label="批号">{{ detail.batchCode || '—' }}</el-descriptions-item>
        <el-descriptions-item label="等级">{{ detail.level === 'CRITICAL' ? '重大（关键）' : '普通' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status === 'OPEN' ? '未处理' : '已处理' }}</el-descriptions-item>
        <el-descriptions-item label="发现部门">{{ detail.discoveryDepartmentName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发现人">{{ detail.discovererName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发现时间">{{ formatTime(detail.discoveredAt) }}</el-descriptions-item>
        <el-descriptions-item label="发现地点">{{ detail.discoveryLocation || '—' }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.productName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="产品规格">{{ detail.productSpecification || '—' }}</el-descriptions-item>
        <el-descriptions-item label="设备或系统">{{ detail.equipmentOrSystem || '—' }}</el-descriptions-item>
        <el-descriptions-item label="报告时间">{{ formatTime(detail.reportedAt) }}</el-descriptions-item>
        <el-descriptions-item label="接收人">{{ detail.receiverName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="偏差类别">{{ detail.categoryCodesJson || '—' }}</el-descriptions-item>
        <el-descriptions-item label="偏差描述">{{ detail.description || '—' }}</el-descriptions-item>
        <el-descriptions-item label="紧急措施">{{ detail.emergencyAction || '—' }}</el-descriptions-item>
        <el-descriptions-item label="等级依据">{{ detail.levelBasis || '—' }}</el-descriptions-item>
        <el-descriptions-item label="关闭方式">{{ detail.closeReason === 'TRANSFERRED_TO_NCR' ? '转不合格审批关闭' : detail.closeReason === 'NORMAL_COMPLETED' ? '常规闭环' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="关联不合格评审"><el-button v-if="detail.nonconformanceReviewId" link type="primary" @click="openReview">{{ detail.nonconformanceReviewCode || `#${detail.nonconformanceReviewId}` }}</el-button><span v-else>—</span></el-descriptions-item>
        <el-descriptions-item label="评审处置">{{ detail.nonconformanceDisposition || detail.nonconformanceReviewStatus || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发起电子签名">{{ detail.initiatorSignatureId ? `已签署 #${detail.initiatorSignatureId}` : '待签署' }}</el-descriptions-item>
        <el-descriptions-item label="签名正文摘要">{{ detail.initiatorContentHash || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发起时间">{{ formatTime(detail.initiatedAt) }}</el-descriptions-item>
      </el-descriptions>
      <section
        v-if="props.readonly && detail.closeReason === 'TRANSFERRED_TO_NCR' && detail.nonconformanceReviewId"
        class="ncr-disposition-evidence"
        data-deviation-ncr-disposition-evidence
      >
        <el-divider content-position="left">关联不合格审批处置</el-divider>
        <el-alert v-if="ncrReviewError" type="error" :closable="false" :title="ncrReviewError">
          <el-button link type="primary" @click="loadNcrReviewDetail">重试</el-button>
        </el-alert>
        <el-alert v-else-if="ncrReviewLoading" type="info" :closable="false" title="正在读取不合格审批处置记录" />
        <el-alert v-else-if="ncrSignatureEvidenceError" type="error" :closable="false" :title="ncrSignatureEvidenceError" />
        <el-descriptions v-else-if="ncrReviewDetail?.reviewStatus === 'pending_review'" :column="1" border>
          <el-descriptions-item label="评审状态">待QA处置</el-descriptions-item>
          <el-descriptions-item label="不合格原因">{{ ncrReviewDetail.nonconformanceReason || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-else-if="ncrReviewDetail && ncrSignatureEvidence" :column="1" border>
          <el-descriptions-item label="评审状态">已处置</el-descriptions-item>
          <el-descriptions-item label="不合格原因">{{ ncrReviewDetail.nonconformanceReason || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处置方式">{{ resolveNcrDisposition(ncrReviewDetail.disposition) }}</el-descriptions-item>
          <el-descriptions-item label="评审实际意见">{{ ncrReviewDetail.reviewOpinion || '—' }}</el-descriptions-item>
          <el-descriptions-item label="QA处置签署人">{{ ncrReviewDetail.qaUserId ? `用户 #${ncrReviewDetail.qaUserId}` : '—' }}</el-descriptions-item>
          <el-descriptions-item label="处置签名记录">{{ ncrReviewDetail.qaSignature || `#${ncrSignatureEvidence.signatureId}` }}</el-descriptions-item>
          <el-descriptions-item label="签名记录编号">{{ ncrSignatureEvidence.signatureId }}</el-descriptions-item>
          <el-descriptions-item label="处置签署时间">{{ formatTime(ncrSignatureEvidence.signedAt) }}</el-descriptions-item>
          <el-descriptions-item label="签名正文摘要">{{ ncrSignatureEvidence.aggregateHash }}</el-descriptions-item>
        </el-descriptions>
      </section>
      <div v-if="detail.status === 'OPEN' && detail.level === 'CRITICAL' && !props.readonly" class="mt-12px">
        <el-button
          v-hasPermi="['mes:pro-edhr-nonconformance-review:deviation-create']"
          type="danger"
          :disabled="detail.canTransferToNcr !== true"
          @click="openNcrCreateDialog"
        >
          发起不合格审批
        </el-button>
        <el-button link type="primary" @click="refreshTransferEligibility">刷新转审状态</el-button>
        <el-alert v-if="detail.canTransferToNcr === false" class="mt-8px" type="warning" :closable="false" :title="detail.transferToNcrBlockedReason" />
      </div>
      <el-divider />
      <el-alert v-if="handlingError" type="error" :closable="false" :title="handlingError">
        <el-button link type="primary" @click="loadHandling">重试</el-button>
      </el-alert>
      <el-empty v-else-if="detail.status === 'OPEN' && !handling" description="尚未填写处理内容">
        <el-button v-if="!props.readonly" v-hasPermi="['mes:pro-edhr-deviation:handle']" type="primary" @click="startHandling">开始处理</el-button>
      </el-empty>
      <el-card v-else-if="handling" v-loading="handlingLoading" shadow="never">
        <template #header>
          <div class="handling-card__header">
            <span>唯一处理记录</span>
            <div v-if="detail.status === 'OPEN' && !props.readonly" class="handling-card__actions">
              <el-button size="small" type="primary" :loading="handlingSaving" @click="saveHandling">保存处理</el-button>
              <el-button size="small" :disabled="!handling.id" @click="openSignDialog(signForm.node)">签署当前节点</el-button>
              <el-button size="small" type="success" :loading="closing" :disabled="!handling.id" @click="closeHandling">常规关闭</el-button>
            </div>
            </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="内容版本">{{ handling.contentVersion || '—' }}</el-descriptions-item>
          <el-descriptions-item label="处理结论">{{ handling.handlingConclusion || '—' }}</el-descriptions-item>
          <el-descriptions-item label="验证结果">{{ handling.verificationResult || '—' }}</el-descriptions-item>
          <el-descriptions-item label="关联评审">{{ handling.nonconformanceReviewCode || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-if="props.readonly" :column="1" border class="handling-readonly">
          <el-descriptions-item label="调查开始">{{ handling.investigationStartedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="计划完成">{{ handling.plannedCompletedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="实际完成">{{ handling.completedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="调查成员">{{ handling.investigationMembersJson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="根因分析">{{ handling.rootCauseAnalysis || '—' }}</el-descriptions-item>
          <el-descriptions-item label="影响范围">{{ handling.impactScope || '—' }}</el-descriptions-item>
          <el-descriptions-item label="风险评估">{{ handling.riskAssessment || '—' }}</el-descriptions-item>
          <el-descriptions-item label="产品处置">{{ handling.productDisposition || '—' }}</el-descriptions-item>
          <el-descriptions-item label="整改责任人">{{ handling.correctiveOwnerName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="整改期限">{{ handling.correctiveDueAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="CAPA">{{ handling.capaRequired ? '是' : '否' }}{{ handling.capaCode ? ` / ${handling.capaCode}` : '' }}</el-descriptions-item>
          <el-descriptions-item label="CAPA附件">{{ handling.capaAttachmentsJson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="验证内容">{{ handling.verificationContent || '—' }}</el-descriptions-item>
        </el-descriptions>
        <el-form v-if="detail.status === 'OPEN' && !props.readonly" :model="handlingForm" label-width="110px" class="handling-form">
          <el-form-item v-if="handlingForm.expectedContentVersion > 0" label="修订原因" required><el-input v-model="handlingForm.revisionReason" type="textarea" maxlength="500" show-word-limit placeholder="请说明本次修改原因" /></el-form-item>
          <el-form-item label="根因分析"><el-input v-model="handlingForm.rootCauseAnalysis" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="影响范围"><el-input v-model="handlingForm.impactScope" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="风险评估"><el-input v-model="handlingForm.riskAssessment" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="产品处置"><el-input v-model="handlingForm.productDisposition" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="调查开始"><el-date-picker v-model="handlingForm.investigationStartedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="计划完成"><el-date-picker v-model="handlingForm.plannedCompletedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="实际完成"><el-date-picker v-model="handlingForm.completedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="调查成员"><el-input v-model="handlingForm.investigationMembersJson" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="整改责任人"><el-input v-model="handlingForm.correctiveOwnerName" /></el-form-item>
          <el-form-item label="整改期限"><el-date-picker v-model="handlingForm.correctiveDueAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
          <el-form-item label="CAPA"><el-switch v-model="handlingForm.capaRequired" /><el-input v-if="handlingForm.capaRequired" v-model="handlingForm.capaCode" class="ml-8px" placeholder="CAPA编号" /></el-form-item>
          <el-form-item label="处理结论"><el-select v-model="handlingForm.handlingConclusion"><el-option label="闭环完成" value="CLOSED_LOOP" /></el-select></el-form-item>
          <el-form-item label="验证结果"><el-select v-model="handlingForm.verificationResult"><el-option label="通过" value="PASS" /></el-select></el-form-item>
          <el-form-item label="验证内容"><el-input v-model="handlingForm.verificationContent" type="textarea" :rows="2" /></el-form-item>
        </el-form>
        <el-divider v-if="handling.signatureEvidence?.length" />
        <div v-if="handling.signatureEvidence?.length" class="signature-evidence">
          <div class="signature-evidence__title">当前版本电子签名证据</div>
          <el-table :data="handling.signatureEvidence" size="small" border>
            <el-table-column prop="node" label="签名节点" width="150" />
            <el-table-column label="签名人" min-width="160">
              <template #default="scope">{{ scope.row.actorDisplayName || `用户 #${scope.row.actorId || '—'}` }}</template>
            </el-table-column>
            <el-table-column prop="meaningLabel" label="签名含义" min-width="140" />
            <el-table-column label="服务器签名时间" min-width="220"><template #default="scope">{{ scope.row.signedAt || '—' }} {{ scope.row.timeZone ? `(${scope.row.timeZone})` : '' }}</template></el-table-column>
            <el-table-column prop="timeEvidenceId" label="时间证据" min-width="220" show-overflow-tooltip />
            <el-table-column prop="verificationStatus" label="验证状态" width="120" />
            <el-table-column prop="policyVersion" label="策略版本" min-width="180" />
            <el-table-column prop="contentHash" label="正文摘要" min-width="220" show-overflow-tooltip />
            <el-table-column prop="evidenceHash" label="证据摘要" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
        <el-divider v-if="handling.signatureHistory?.length" />
        <div v-if="handling.signatureHistory?.length" class="signature-history">
          <div class="signature-evidence__title">历史电子签名</div>
          <el-table :data="handling.signatureHistory" size="small" border>
            <el-table-column prop="node" label="节点" width="140" />
            <el-table-column label="签名人" min-width="150"><template #default="scope">{{ scope.row.actorDisplayName || `用户 #${scope.row.actorId || '—'}` }}</template></el-table-column>
            <el-table-column prop="validityStatus" label="有效性" width="150" />
            <el-table-column prop="meaningLabel" label="签名含义" min-width="140" />
            <el-table-column label="签署时间" min-width="220"><template #default="scope">{{ scope.row.signedAt || '—' }} {{ scope.row.timeZone ? `(${scope.row.timeZone})` : '' }}</template></el-table-column>
            <el-table-column prop="timeEvidenceId" label="时间证据" min-width="220" show-overflow-tooltip />
            <el-table-column prop="verificationStatus" label="验证状态" width="120" />
            <el-table-column prop="policyVersion" label="策略版本" min-width="180" />
            <el-table-column prop="subjectVersion" label="主题版本" min-width="220" show-overflow-tooltip />
            <el-table-column prop="contentHash" label="正文摘要" min-width="220" show-overflow-tooltip />
            <el-table-column prop="evidenceHash" label="证据摘要" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
        <el-divider v-if="handling.revisionHistory?.length" />
        <div v-if="handling.revisionHistory?.length" class="revision-history">
          <div class="signature-evidence__title">处理修订历史</div>
          <el-timeline>
            <el-timeline-item v-for="revision in handling.revisionHistory" :key="revision.auditId" :timestamp="revision.occurredAt">
              <div>修订原因：{{ revision.revisionReason || '—' }}</div>
              <div>版本：{{ revision.beforeContentVersion || '—' }} → {{ revision.afterContentVersion || '—' }}</div>
              <div>操作人：{{ revision.actorUsername || revision.actorUserId || '—' }}</div>
              <div class="revision-history__hash">审计摘要：{{ revision.auditHash || '—' }}</div>
              <details class="revision-history__content"><summary>查看修订前后正文</summary><pre>修订前：{{ revision.beforeContent || '—' }}
修订后：{{ revision.afterContent || '—' }}</pre></details>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-card>
      <el-alert v-else type="info" :closable="false" :title="closedMessage" />
    </template>
    <el-dialog v-model="signVisible" title="电子签名" width="480px">
      <el-form :model="signForm" label-width="100px">
        <el-form-item label="签名节点"><el-select v-model="signForm.node"><el-option v-for="node in signNodeOptions" :key="node" :label="node" :value="node" /></el-select></el-form-item>
        <el-form-item label="密码"><el-input v-model="signForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="签名意见"><el-input v-model="signForm.comment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="signVisible = false">取消</el-button><el-button type="primary" :loading="signing" @click="signHandling">确认签名</el-button></template>
    </el-dialog>
    <el-dialog v-model="ncrCreateVisible" title="发起不合格审批" width="520px" destroy-on-close>
      <el-alert v-if="ncrCreateError" :title="ncrCreateError" type="error" :closable="false" show-icon class="mb-12px" />
      <el-form :model="ncrCreateForm" label-width="110px">
        <el-form-item label="不合格原因" required>
          <el-input v-model="ncrCreateForm.nonconformanceReason" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ncrCreateForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="签名密码" required>
          <el-input v-model="ncrCreateForm.signaturePassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ncrCreateVisible = false">取消</el-button>
        <el-button type="danger" :loading="ncrCreateLoading" @click="submitNcrCreate">签名并发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { closeDeviationHandling, getDeviation, getDeviationHandling, saveDeviationHandling, signDeviationHandling, type DeviationHandlingRespVO, type DeviationRespVO } from '@/api/mes/pro/edhr/deviation'
import { createCriticalDeviationReview, getNonconformanceReview, type EdhrNonconformanceReviewRespVO } from '@/api/mes/pro/edhr/nonconformanceReview'
import { ElMessage } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import dayjs from 'dayjs'
const message = useMessage()
const props = withDefaults(defineProps<{ id: number; readonly?: boolean }>(), { readonly: false })
const loading = ref(false)
const detail = ref<DeviationRespVO>()
const handling = ref<DeviationHandlingRespVO>()
const ncrReviewDetail = ref<EdhrNonconformanceReviewRespVO>()
const ncrSignatureEvidence = ref<{ signatureId: number; actionType: string; signedAt: string; aggregateHash: string }>()
const error = ref('')
const handlingError = ref('')
const ncrReviewError = ref('')
const ncrSignatureEvidenceError = ref('')
const handlingLoading = ref(false)
const ncrReviewLoading = ref(false)
const handlingSaving = ref(false)
const signing = ref(false)
const closing = ref(false)
const signVisible = ref(false)
const ncrCreateVisible = ref(false)
const ncrCreateLoading = ref(false)
const ncrCreateError = ref('')
const createHandlingForm = () => ({ expectedContentVersion: 0, revisionReason: '', investigationStartedAt: '', plannedCompletedAt: '', completedAt: '', investigationMembersJson: '', rootCauseAnalysis: '', impactScope: '', riskAssessment: '', productDisposition: '', nonconformanceReviewCode: '', correctiveOwnerName: '', correctiveDueAt: '', capaRequired: false, capaCode: '', capaAttachmentsJson: '', handlingConclusion: 'CLOSED_LOOP', verificationResult: 'PASS', verificationContent: '' })
const handlingForm = reactive(createHandlingForm())
let savedFormSnapshot = ''
let loadSequence = 0
let transferEligibilitySequence = 0
let handlingSequence = 0
let reviewSequence = 0
const formSnapshot = () => JSON.stringify({ ...handlingForm, revisionReason: '' })
const resetHandling = () => {
  handling.value = undefined
  Object.assign(handlingForm, createHandlingForm())
  savedFormSnapshot = ''
}

const signForm = reactive({ node: 'PREPARER', password: '', comment: '' })
const ncrCreateForm = reactive({ nonconformanceReason: '', remark: '', signaturePassword: '', idempotencyKey: '' })
const signNodeOptions = computed(() => {
  const nodes = ['PREPARER', 'VERIFIER', 'DEPARTMENT_OWNER', 'QA', 'QUALITY_OWNER']
  if (detail.value?.level === 'CRITICAL') nodes.push('MANAGEMENT_REP')
  return nodes
})
const router = useRouter()
const formatTime = (value?: string | number) => {
  if (value == null || value === '') return '—'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
const closedMessage = computed(() => detail.value?.closeReason === 'TRANSFERRED_TO_NCR'
  ? '已转不合格审批关闭，处置结果见关联评审'
  : detail.value?.closeReason === 'NORMAL_COMPLETED'
    ? '偏差已常规闭环'
    : '偏差已关闭')
const resolveNcrDisposition = (disposition?: string) => {
  if (disposition === 'concession_release') return '让步放行'
  if (disposition === 'rework') return '返工'
  if (disposition === 'void') return '作废'
  return disposition || '—'
}
const readNcrSignatureEvidence = (review: EdhrNonconformanceReviewRespVO) => {
  if (!review.traceSnapshotJson) throw new Error('不合格评审未返回处置签名快照')
  const snapshot = JSON.parse(review.traceSnapshotJson) as {
    qaSignatureSnapshotJson?: {
      reviewId?: number
      signatureId?: number
      actionType?: string
      disposition?: string
      signedAt?: string
      aggregateHash?: string
    }
  }
  const evidence = snapshot.qaSignatureSnapshotJson
  if (!evidence
    || typeof evidence.signatureId !== 'number'
    || !Number.isSafeInteger(evidence.signatureId)
    || evidence.reviewId !== review.id
    || evidence.actionType !== 'QA_DISPOSITION'
    || evidence.disposition !== review.disposition
    || !evidence.signedAt
    || !evidence.aggregateHash) {
    throw new Error('不合格评审处置签名快照缺少有效签名证据')
  }
  return {
    signatureId: evidence.signatureId,
    actionType: evidence.actionType,
    signedAt: evidence.signedAt,
    aggregateHash: evidence.aggregateHash
  }
}
const openBatch = () => {
  if (detail.value?.batchExecutionId) {
    void router.push({
      path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail',
      query: { batchExecutionId: String(detail.value.batchExecutionId), from: 'execution' }
    })
  }
}
const openReview = () => {
  if (detail.value?.nonconformanceReviewId) {
    void router.push({ path: '/mes/pro/feedback/edhr-nonconformance-review', query: { reviewId: String(detail.value.nonconformanceReviewId), from: 'deviation' } })
  }
}
const refreshTransferEligibility = async () => {
  const id = props.id
  const sequence = loadSequence
  const refreshSequence = ++transferEligibilitySequence
  try {
    const latest = await getDeviation(id)
    if (id !== props.id || sequence !== loadSequence || refreshSequence !== transferEligibilitySequence || !detail.value) return
    detail.value.canTransferToNcr = latest.canTransferToNcr
    detail.value.transferToNcrBlockedReason = latest.transferToNcrBlockedReason
  } catch (cause: any) {
    if (id === props.id && sequence === loadSequence && refreshSequence === transferEligibilitySequence) {
      ElMessage.error(cause?.response?.data?.msg || cause?.message || '转审状态读取失败，请重试')
    }
  }
}
const openNcrCreateDialog = () => {
  if (props.readonly || !detail.value || detail.value.status !== 'OPEN' || detail.value.level !== 'CRITICAL') return
  if (detail.value.canTransferToNcr !== true) return
  ncrCreateError.value = ''
  ncrCreateForm.nonconformanceReason = ''
  ncrCreateForm.remark = ''
  ncrCreateForm.signaturePassword = ''
  ncrCreateForm.idempotencyKey = `EDHR_DEVIATION_NCR_${detail.value.id}_${Date.now()}_${Math.random().toString(36).slice(2)}`
  ncrCreateVisible.value = true
}
const submitNcrCreate = async () => {
  if (props.readonly) return
  const current = detail.value
  const id = props.id
  const sequence = loadSequence
  const nonconformanceReason = ncrCreateForm.nonconformanceReason.trim()
  const signaturePassword = ncrCreateForm.signaturePassword.trim()
  if (!current || current.status !== 'OPEN' || current.level !== 'CRITICAL' || !current.batchExecutionId) {
    ncrCreateError.value = '当前偏差不满足发起不合格审批条件，请刷新后重试'
    return
  }
  if (current.canTransferToNcr !== true) {
    ncrCreateError.value = current.transferToNcrBlockedReason || '转审资格未就绪，请刷新转审状态'
    return
  }
  if (!nonconformanceReason || !signaturePassword) {
    ncrCreateError.value = '不合格原因和签名密码不能为空'
    return
  }
  ncrCreateLoading.value = true
  ncrCreateError.value = ''
  try {
    await createCriticalDeviationReview({
      batchExecutionId: current.batchExecutionId,
      deviationIds: [current.id],
      nonconformanceReason,
      remark: ncrCreateForm.remark.trim() || undefined,
      idempotencyKey: ncrCreateForm.idempotencyKey,
      signaturePassword
    })
    if (id !== props.id || sequence !== loadSequence) return
    ncrCreateVisible.value = false
    await load()
    ElMessage.success('不合格审批已发起，偏差已转审关闭')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ncrCreateError.value = error?.response?.data?.msg || '不合格审批发起失败，请重试'
  } finally {
    if (id === props.id && sequence === loadSequence) ncrCreateLoading.value = false
  }
}
const applyHandling = (record?: DeviationHandlingRespVO) => {
  resetHandling()
  handling.value = record
  if (record) {
  Object.assign(handlingForm, {
        expectedContentVersion: record.contentVersion || 0,
        investigationStartedAt: record.investigationStartedAt || '',
        plannedCompletedAt: record.plannedCompletedAt || '',
        completedAt: record.completedAt || '',
        investigationMembersJson: record.investigationMembersJson || '',
        rootCauseAnalysis: record.rootCauseAnalysis || '',
        impactScope: record.impactScope || '',
        riskAssessment: record.riskAssessment || '',
        productDisposition: record.productDisposition || '',
        nonconformanceReviewCode: record.nonconformanceReviewCode || '',
        correctiveOwnerName: record.correctiveOwnerName || '',
        correctiveDueAt: record.correctiveDueAt || '',
        capaRequired: record.capaRequired || false,
        capaCode: record.capaCode || '',
        capaAttachmentsJson: record.capaAttachmentsJson || '',
        handlingConclusion: record.handlingConclusion || 'CLOSED_LOOP',
        verificationResult: record.verificationResult || 'PASS',
        verificationContent: record.verificationContent || ''
      })
  }
  savedFormSnapshot = formSnapshot()
}
const loadHandling = async () => {
  if (!detail.value) return
  const id = props.id
  const sequence = ++handlingSequence
  handlingLoading.value = true
  handlingError.value = ''
  try {
    const record = await getDeviationHandling(id)
    if (id !== props.id || sequence !== handlingSequence) return
    applyHandling(record)
  } catch {
    if (id !== props.id || sequence !== handlingSequence) return
    resetHandling()
    handlingError.value = '偏差处理记录加载失败，请重试'
  } finally {
    if (id === props.id && sequence === handlingSequence) handlingLoading.value = false
  }
}
const loadNcrReviewDetail = async () => {
  const id = props.id
  const sequence = ++reviewSequence
  const reviewId = detail.value?.nonconformanceReviewId
  if (!props.readonly || detail.value?.closeReason !== 'TRANSFERRED_TO_NCR' || !reviewId) {
    ncrReviewDetail.value = undefined
    ncrSignatureEvidence.value = undefined
    ncrReviewError.value = ''
    ncrSignatureEvidenceError.value = ''
    return
  }
  ncrReviewLoading.value = true
  ncrReviewError.value = ''
  ncrSignatureEvidenceError.value = ''
  ncrReviewDetail.value = undefined
  ncrSignatureEvidence.value = undefined
  try {
    const review = await getNonconformanceReview(reviewId)
    if (id !== props.id || sequence !== reviewSequence) return
    ncrReviewDetail.value = review
    if (review.reviewStatus !== 'pending_review') {
      try {
        ncrSignatureEvidence.value = readNcrSignatureEvidence(review)
      } catch (cause) {
        ncrSignatureEvidenceError.value = cause instanceof Error
          ? cause.message
          : '不合格评审处置签名证据无法解析'
      }
    }
  } catch (cause: any) {
    if (id !== props.id || sequence !== reviewSequence) return
    ncrReviewError.value = cause?.response?.data?.msg || '关联不合格评审详情加载失败，请重试'
  } finally {
    if (id === props.id && sequence === reviewSequence) ncrReviewLoading.value = false
  }
}
const saveHandling = async () => {
  if (props.readonly) return
  if (handlingForm.expectedContentVersion > 0 && !handlingForm.revisionReason.trim()) {
    ElMessage.error('请填写修订原因后保存')
    return
  }
  const id = props.id
  const sequence = loadSequence
  handlingSaving.value = true
  try {
    const record = await saveDeviationHandling(id, {
      ...handlingForm,
      investigationStartedAt: normalizeOptionalDateTime(handlingForm.investigationStartedAt),
      plannedCompletedAt: normalizeOptionalDateTime(handlingForm.plannedCompletedAt),
      completedAt: normalizeOptionalDateTime(handlingForm.completedAt),
      correctiveDueAt: normalizeOptionalDateTime(handlingForm.correctiveDueAt),
      investigationMembersJson: normalizeOptionalJson(handlingForm.investigationMembersJson),
      capaAttachmentsJson: normalizeOptionalJson(handlingForm.capaAttachmentsJson)
    })
    if (id !== props.id || sequence !== loadSequence) return
    applyHandling(record)
    ElMessage.success('处理记录已保存')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || '处理记录保存失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) handlingSaving.value = false
  }
}
const canSignSavedHandling = () => {
  if (!handling.value?.id || !handling.value.contentVersion || !handling.value.contentHash || formSnapshot() !== savedFormSnapshot || handlingSaving.value) {
    ElMessage.error('请先保存当前处理内容，再审阅并签名')
    return false
  }
  return true
}
const openSignDialog = (node: string) => {
  if (props.readonly || !canSignSavedHandling()) return
  signForm.node = node
  signForm.password = ''
  signForm.comment = ''
  signVisible.value = true
}
const startHandling = () => {
  if (props.readonly || !detail.value || detail.value.status !== 'OPEN') return
  resetHandling()
  handling.value = { id: 0, deviationId: props.id, contentVersion: 0, signatureEvidence: [], signatureHistory: [], revisionHistory: [] }
}
const normalizeOptionalJson = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) return null
  try {
    JSON.parse(trimmed)
    return trimmed
  } catch {
    return JSON.stringify(trimmed)
  }
}
const normalizeOptionalDateTime = (value: string | null | undefined) => value?.trim() ? value : null
const signHandling = async () => {
  if (props.readonly || !canSignSavedHandling()) return
  const id = props.id
  const sequence = loadSequence
  const reviewed = handling.value!
  signing.value = true
  try {
    await signDeviationHandling({ deviationId: id, ...signForm, expectedContentVersion: reviewed.contentVersion!, expectedContentHash: reviewed.contentHash! })
    if (id !== props.id || sequence !== loadSequence) return
    signVisible.value = false
    await loadHandling()
    ElMessage.success('电子签名已记录')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || '电子签名失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) signing.value = false
  }
}
const closeHandling = async () => {
  if (props.readonly || !canSignSavedHandling()) return
  const id = props.id
  const sequence = loadSequence
  try {
    await message.confirm('确认按当前处理内容常规关闭该偏差吗？', '关闭确认')
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    throw action
  }
  if (id !== props.id || sequence !== loadSequence || !canSignSavedHandling()) return
  closing.value = true
  try {
    await closeDeviationHandling(id)
    if (id !== props.id || sequence !== loadSequence) return
    await load()
    ElMessage.success('偏差已关闭')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || '偏差关闭失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) closing.value = false
  }
}
const load = async () => {
  const id = props.id
  const sequence = ++loadSequence
  handlingSequence++
  reviewSequence++
  detail.value = undefined
  resetHandling()
  signVisible.value = false
  ncrCreateVisible.value = false
  Object.assign(signForm, { node: 'PREPARER', password: '', comment: '' })
  ncrReviewDetail.value = undefined
  ncrSignatureEvidence.value = undefined
  handlingLoading.value = false
  handlingSaving.value = false
  signing.value = false
  closing.value = false
  ncrCreateLoading.value = false
  ncrCreateError.value = ''
  ncrReviewError.value = ''
  ncrSignatureEvidenceError.value = ''
  Object.assign(ncrCreateForm, { nonconformanceReason: '', remark: '', signaturePassword: '', idempotencyKey: '' })
  loading.value = true
  error.value = ''
  handlingError.value = ''
  try {
    const record = await getDeviation(id)
    if (id !== props.id || sequence !== loadSequence) return
    detail.value = record
    await Promise.all([loadHandling(), loadNcrReviewDetail()])
  } catch {
    if (id === props.id && sequence === loadSequence) error.value = '偏差详情加载失败，请重试'
  } finally {
    if (id === props.id && sequence === loadSequence) loading.value = false
  }
}
watch(() => props.id, load, { immediate: true })
</script>

<style scoped>
.handling-readonly :deep(.el-descriptions__content) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ncr-disposition-evidence :deep(.el-descriptions__content) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
