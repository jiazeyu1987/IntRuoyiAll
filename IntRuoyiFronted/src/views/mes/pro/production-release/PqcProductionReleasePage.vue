<template>
  <ContentWrap>
    <ActiveOrderDetailLayout v-if="detailVisible" data-pqc-production-release-order-detail>
      <el-button data-pqc-production-release-detail-back @click="detailVisible = false">
        返回
      </el-button>
      <el-descriptions
        v-if="detailRow && orderDetail && !detailLoading && !detailError"
        :column="1"
        border
        data-pqc-production-release-detail-identity
      >
        <el-descriptions-item label="生产放行申请编号">
          <span data-pqc-production-release-detail-application-id>{{ detailRow.applicationId }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="生产订单号">
          <span data-pqc-production-release-detail-work-order>{{ detailRow.workOrderCode || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="批次执行编号">
          <span data-pqc-production-release-detail-batch-execution-id>{{ detailRow.batchExecutionId || '--' }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <ActiveOrderSubmissionDetailPanel
        :detail="orderDetail?.detail"
        :production-material-lists="orderDetail?.productionMaterialLists || []"
        :loading="detailLoading"
        :error="detailError"
        :pqc-release-application-id="detailApplicationId"
        :read-only="detailReadOnly"
        @retry="retryOrderDetail"
      />
    </ActiveOrderDetailLayout>
    <div v-show="!detailVisible" class="pqc-release-page" data-pqc-production-release-page>
      <el-tabs v-model="activeView" class="pqc-release-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="待放行" :name="PQC_RELEASE_VIEW_PENDING" />
        <el-tab-pane label="已处理" :name="PQC_RELEASE_VIEW_COMPLETED" />
      </el-tabs>

      <el-form :inline="true" :model="queryParams" class="pqc-release-page__filters">
        <el-form-item label="工单号">
          <el-input
            v-model="queryParams.workOrderCode"
            clearable
            class="!w-200px"
            data-pqc-production-release-work-order-filter
          />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable class="!w-200px" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :icon="Search"
            data-pqc-production-release-query
            @click="handleQuery"
          >
            查询
          </el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <el-table
        v-loading="loading"
        :data="list"
        data-pqc-production-release-list
        stripe
        row-key="applicationId"
        empty-text="当前状态暂无生产放行记录"
      >
        <el-table-column label="生产对象" min-width="240">
          <template #default="{ row }">
            <div class="pqc-release-page__primary" data-pqc-production-release-work-order-code>
              {{ row.workOrderCode || '--' }}
            </div>
            <div class="pqc-release-page__secondary">批次：{{ row.batchCode || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="申请" min-width="180">
          <template #default="{ row }">
            <div>申请编号：{{ row.applicationId }}</div>
            <div class="pqc-release-page__secondary">{{ formatDateTime(row.appliedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="activeView === PQC_RELEASE_VIEW_COMPLETED ? '放行结果' : '状态'" width="150">
          <template #default="{ row }">
            <el-tag :type="resolveStatusTagType(row)">{{ resolveStatusLabel(row) }}</el-tag>
            <div v-if="row.underReview" class="pqc-release-page__secondary">QA评审中</div>
          </template>
        </el-table-column>
        <el-table-column label="处置信息" min-width="240">
          <template #default="{ row }">
            <template v-if="row.nonconformanceReason">
              <div>{{ row.nonconformanceReason }}</div>
              <div class="pqc-release-page__secondary">
                {{ formatDateTime(row.nonconformanceClosedAt) }}
              </div>
            </template>
            <template v-else-if="row.approvalReady === false">
              <div>{{ row.approvalBlockerReason || '放行资料尚未就绪' }}</div>
              <div v-if="row.approvalBlockerSuggestion" class="pqc-release-page__secondary">
                {{ row.approvalBlockerSuggestion }}
              </div>
            </template>
            <span v-else class="pqc-release-page__secondary">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="pqc-release-page__actions">
              <ReleaseTaskNotificationEntry
                v-hasPermi="['mes:pro-edhr-work-task:query']"
                :work-task-id="row.pqcReleaseWorkTaskId"
              />
              <el-button
                link
                type="primary"
                :disabled="!hasActiveOrderDetail(row)"
                :title="hasActiveOrderDetail(row) ? '查看订单详情' : '缺少正式活跃订单来源，无法查看详情'"
                data-pqc-production-release-detail
                @click="openActiveOrderDetail(row)"
              >
                详情
              </el-button>
              <template v-if="activeView === PQC_RELEASE_VIEW_PENDING">
                <el-button
                  v-hasPermi="['mes:pro-production-release:pqc-approve']"
                  link
                  type="success"
                  :disabled="row.underReview || row.approvalReady === false"
                  :title="row.approvalBlockerReason || '放行'"
                  data-pqc-production-release-approve
                  @click="openReleaseDialog(row)"
                >
                  放行
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-nonconformance-review:create']"
                  link
                  type="danger"
                  :disabled="row.underReview || Boolean(row.nonconformanceDisposition)"
                  data-pqc-production-release-nonconformance
                  :data-pqc-production-release-application-id="String(row.applicationId)"
                  @click="openNonconformanceReview(row)"
                >
                  不合格审查
                </el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList()"
      />
    </div>

    <Dialog
      v-model="releaseDialogVisible"
      data-pqc-production-release-dialog
      title="确认生产放行"
      width="540px"
    >
      <div v-if="selectedRow" class="pqc-release-dialog">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="工单号">{{
            selectedRow.workOrderCode || '--'
          }}</el-descriptions-item>
          <el-descriptions-item label="批次号">{{
            selectedRow.batchCode || '--'
          }}</el-descriptions-item>
          <el-descriptions-item label="申请编号">{{
            selectedRow.applicationId
          }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="releaseError"
          :title="releaseError"
          type="error"
          :closable="false"
          show-icon
        />

        <el-result v-if="releaseResult" icon="success" title="生产放行完成">
          <template #sub-title>
            <span data-pqc-production-release-batch-execution-id>
              批次执行 {{ releaseResult.batchExecutionId || '--' }} 已创建，后续进入资料上传。
            </span>
          </template>
        </el-result>

        <el-form v-else label-width="118px" class="pqc-release-dialog__form">
          <el-form-item label="UDI编号" required>
            <el-input
              v-model="releaseForm.udiControlDocumentNo"
              data-pqc-production-release-udi-document-no
              maxlength="128"
              show-word-limit
              placeholder="请输入UDI编号"
              @keyup.enter="submitRelease"
            />
          </el-form-item>
          <el-form-item label="电子签名密码" required>
            <el-input
              v-model="releaseForm.signaturePassword"
              type="password"
              data-pqc-production-release-signature-password
              show-password
              autocomplete="current-password"
              placeholder="请输入当前账号电子签名密码"
              @keyup.enter="submitRelease"
            />
          </el-form-item>
          <el-form-item label="放行意见">
            <el-input
              v-model="releaseForm.approvalOpinion"
              type="textarea"
              data-pqc-production-release-approval-opinion
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="releaseDialogVisible = false">{{
          releaseResult ? '关闭' : '取消'
        }}</el-button>
        <el-button
          v-if="!releaseResult"
          type="primary"
          :loading="releaseSubmitting"
          :disabled="releaseOutcomeUncertain"
          data-pqc-production-release-confirm
          @click="submitRelease"
        >
          确认放行
        </el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import ReleaseTaskNotificationEntry from './components/ReleaseTaskNotificationEntry.vue'
import CryptoJS from 'crypto-js'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import ActiveOrderSubmissionDetailPanel from '../processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from '../processpool/components/ActiveOrderDetailLayout.vue'
import {
  PQC_RELEASE_VIEW_CONCESSION_RELEASED,
  PQC_RELEASE_VIEW_COMPLETED,
  PQC_RELEASE_VIEW_PENDING,
  PQC_RELEASE_VIEW_RELEASED,
  PQC_RELEASE_VIEW_REWORKED,
  PQC_RELEASE_VIEW_VOIDED,
  approvePqcProductionRelease,
  getPqcProductionRelease,
  getPqcProductionReleaseOrderDetail,
  getPqcProductionReleasePage,
  type MesPqcProductionReleaseDecisionRespVO,
  type MesPqcProductionReleasePageItemRespVO,
  type MesPqcProductionReleasePageReqVO
} from '@/api/mes/pro/productionRelease'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import { useUserStore } from '@/store/modules/user'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const approvalInvalidation = useApprovalCenterInvalidationStore()

defineOptions({ name: 'MesPqcProductionRelease' })

const router = useRouter()
const route = useRoute()
const message = useMessage()
const userStore = useUserStore()
const loading = ref(false)
const loadError = ref('')
const list = ref<MesPqcProductionReleasePageItemRespVO[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailRow = ref<MesPqcProductionReleasePageItemRespVO>()
const detailApplicationId = ref<string>()
const detailReadOnly = ref(false)
const orderDetail = ref<Awaited<ReturnType<typeof getPqcProductionReleaseOrderDetail>>>()
let detailRequestSequence = 0
const activeView = ref<MesPqcProductionReleasePageReqVO['viewStatus']>(PQC_RELEASE_VIEW_PENDING)
const releaseDialogVisible = ref(false)
const releaseSubmitting = ref(false)
const releaseError = ref('')
const selectedRow = ref<MesPqcProductionReleasePageItemRespVO>()
const releaseResult = ref<MesPqcProductionReleaseDecisionRespVO>()
const releaseOutcomeUncertain = ref(false)
// The Pinia owner survives page instances; every entry is keyed by the exact session/application/task.
const releaseRequestOwner = approvalInvalidation as typeof approvalInvalidation & {
  pqcReleaseRequests?: {
    keys: Map<string, string>
    states: Map<string, 'WRITING' | 'UNCERTAIN' | 'CONFIRMED'>
    requests: Map<string, Readonly<{
      applicationId: string
      pqcReleaseWorkTaskId: string
      expectedVersion: number
      actorId: string
      scopeKey: string
      session: ReturnType<typeof approvalInvalidation.syncSession>
      idempotencyKey: string
      udiControlDocumentNo: string
      approvalOpinion: string | undefined
      payloadHash: string
      event: ReturnType<typeof approvalInvalidation.captureEdhrSuccess>
    }>>
  }
}
if (!releaseRequestOwner.pqcReleaseRequests) {
  releaseRequestOwner.pqcReleaseRequests = {
    keys: new Map(), states: new Map(), requests: new Map()
  }
}
const releaseIdempotencyKeys = releaseRequestOwner.pqcReleaseRequests.keys
const releaseRequestStates = releaseRequestOwner.pqcReleaseRequests.states
let listRequestSequence = 0

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  applicationId: undefined as string | undefined,
  pqcReleaseWorkTaskId: undefined as string | undefined,
  workOrderCode: typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : '',
  batchCode: ''
})

const releaseForm = reactive({
  udiControlDocumentNo: '',
  signaturePassword: '',
  approvalOpinion: ''
})

const formatDateTime = (value?: string | number) => formatEdhrDateTime(value)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const getList = async (isCurrent: () => boolean = () => true) => {
  const requestId = ++listRequestSequence
  const contextKey = JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath])
  const current = () => requestId === listRequestSequence && isCurrent() && contextKey === JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath])
  loading.value = true
  loadError.value = ''
  try {
    const data = await getPqcProductionReleasePage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      viewStatus: activeView.value,
      applicationId: queryParams.applicationId,
      pqcReleaseWorkTaskId: queryParams.pqcReleaseWorkTaskId,
      workOrderCode: queryParams.workOrderCode.trim() || undefined,
      batchCode: queryParams.batchCode.trim() || undefined
    }, { ignoreErrorMessage: true, isRequestContextCurrent: current })
    if (!current()) return
    if (queryParams.applicationId && (
      data.list?.length !== 1 || data.total !== 1 ||
      String(data.list[0].applicationId) !== queryParams.applicationId ||
      String(data.list[0].pqcReleaseWorkTaskId) !== queryParams.pqcReleaseWorkTaskId
    )) {
      throw new Error('未查询到唯一匹配当前申请和工作任务的生产放行记录。')
    }
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    if (!current()) return
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'PQC生产放行列表加载失败。')
  } finally {
    if (current()) loading.value = false
  }
}

type PqcReleaseRouteContext = { applicationId: string; pqcReleaseWorkTaskId: string }
let pqcReleaseRouteGeneration = 0

const readPqcReleaseRouteContext = (): PqcReleaseRouteContext | undefined => {
  const applicationId = route.query.applicationId
  const workTaskId = route.query.workTaskId
  if (applicationId === undefined && workTaskId === undefined) return undefined
  if (typeof applicationId !== 'string' || !/^[1-9]\d*$/.test(applicationId) ||
    typeof workTaskId !== 'string' || !/^[1-9]\d*$/.test(workTaskId)) {
    throw new Error('生产放行入口必须提供唯一且精确的申请和工作任务编号。')
  }
  return { applicationId, pqcReleaseWorkTaskId: workTaskId }
}

const clearOrderDetail = () => {
  detailRequestSequence++
  detailVisible.value = false
  detailLoading.value = false
  detailError.value = ''
  detailRow.value = undefined
  detailApplicationId.value = undefined
  detailReadOnly.value = false
  orderDetail.value = undefined
}

const isExactPositiveId = (value: unknown) =>
  (typeof value === 'string' && /^[1-9]\d*$/.test(value)) ||
  (typeof value === 'number' && Number.isSafeInteger(value) && value > 0)

const isFormalSignedTime = (value: unknown) =>
  (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) ||
  (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Date.parse(value)))

const requirePqcReleaseAuthority = (
  result: MesPqcProductionReleaseDecisionRespVO,
  context: PqcReleaseRouteContext
) => {
  if (!result || !isExactPositiveId(result.applicationId) || !isExactPositiveId(result.pqcReleaseWorkTaskId) ||
    String(result.applicationId) !== context.applicationId || String(result.pqcReleaseWorkTaskId) !== context.pqcReleaseWorkTaskId) {
    throw new Error('正式生产放行回执与当前申请或工作任务不一致。')
  }
  if (result.status === 'PQC_RELEASE_PENDING' && result.decision == null) return 'PENDING'
  if (result.decision !== 'APPROVE' || !['REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING', 'RELEASED'].includes(result.status) ||
    !isExactPositiveId(result.signatureId) || !isExactPositiveId(result.decidedBy) || !isExactPositiveId(result.batchExecutionId) ||
    !isFormalSignedTime(result.decidedAt) || !Number.isSafeInteger(result.version) || result.version <= 0 ||
    !/^[a-f0-9]{64}$/.test(result.sourceSnapshotHash)) {
    throw new Error('当前任务缺少完整且已签署的生产放行批准回执。')
  }
  return 'APPROVED'
}

const requireCompletedPqcReleaseDetail = (
  result: Awaited<ReturnType<typeof getPqcProductionReleaseOrderDetail>>,
  decision: MesPqcProductionReleaseDecisionRespVO
) => {
  const summary = result?.detail?.pqcProductionRelease
  const signature = summary?.signature
  const verification = signature?.verification
  const exactId = (value: unknown) => isExactPositiveId(value) && BigInt(String(value)) <= 9223372036854775807n
  if (!exactId(result?.detail?.activeOrderId) || !exactId(result?.detail?.workOrderId) ||
    summary?.status !== decision.status || !exactId(signature?.signatureId) ||
    String(signature?.signatureId) !== String(decision.signatureId) || signature?.role !== 'PQC_RELEASE' ||
    !verification || !exactId(verification.signatureId) ||
    String(verification.signatureId) !== String(signature.signatureId)) {
    throw new Error('订单详情签名关联与正式生产放行回执不一致。')
  }
  const dimensions = [verification.digestStatus, verification.timeConsistencyStatus, verification.auditBindingStatus]
  const overall = dimensions.includes('MISMATCH') ? 'MISMATCH' : dimensions.includes('MISSING') ? 'INCOMPLETE' : 'VALID'
  if (dimensions.some((status) => !['CONSISTENT', 'MISSING', 'MISMATCH'].includes(status)) ||
    overall !== verification.verificationStatus || verification.verificationScope !== 'LOCAL_DIGEST_TIME_AND_CREATION_AUDIT' ||
    verification.timeTrustStatus !== 'NOT_BOUND' || !['SERVER_CLOCK', 'UNKNOWN'].includes(verification.timeSource) ||
    !['V1', 'UNSUPPORTED'].includes(verification.evidenceFormatVersion) || !Array.isArray(verification.reasonCodes) ||
    !isFormalSignedTime(verification.verifiedAt) ||
    (verification.digestStatus === 'CONSISTENT' && (!verification.storedContentHash || !verification.storedEvidenceHash ||
      verification.storedContentHash !== verification.calculatedContentHash || verification.storedEvidenceHash !== verification.calculatedEvidenceHash)) ||
    (verification.timeConsistencyStatus === 'CONSISTENT' && verification.timeSource !== 'SERVER_CLOCK') ||
    (verification.timeConsistencyStatus === 'CONSISTENT' && !isFormalSignedTime(signature.signedAt)) ||
    (verification.evidenceFormatVersion === 'UNSUPPORTED' && verification.digestStatus !== 'MISMATCH') ||
    !['VERIFIED_SNAPSHOT', 'NOT_RECORDED', 'UNVERIFIED_SNAPSHOT'].includes(signature.signerNameStatus || '') ||
    (signature.signerNameStatus === 'VERIFIED_SNAPSHOT' && (overall !== 'VALID' || !signature.signerName?.trim())) ||
    (signature.signerNameStatus === 'UNVERIFIED_SNAPSHOT' && (overall === 'VALID' || signature.signerName !== '签时姓名未核验')) ||
    (signature.signerNameStatus === 'NOT_RECORDED' && signature.signerName !== '未记录')) {
    throw new Error('订单详情签名核验状态或姓名投影不一致。')
  }
}

const openCompletedPqcReleaseDetail = async (
  context: PqcReleaseRouteContext,
  decision: MesPqcProductionReleaseDecisionRespVO,
  isCurrent: () => boolean
) => {
  detailApplicationId.value = context.applicationId
  detailReadOnly.value = true
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  try {
    const result = await getPqcProductionReleaseOrderDetail(context.applicationId, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    requireCompletedPqcReleaseDetail(result, decision)
    orderDetail.value = result
  } catch (error) {
    if (!isCurrent()) return
    detailError.value = resolveErrorMessage(error, '已办生产放行详情加载失败。')
  } finally {
    if (isCurrent()) detailLoading.value = false
  }
}

const openPqcReleaseFromRoute = async () => {
  const generation = ++pqcReleaseRouteGeneration
  clearOrderDetail()
  const pageContext = JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath])
  const detailSequence = detailRequestSequence
  listRequestSequence++
  loading.value = false
  list.value = []
  total.value = 0
  loadError.value = ''
  releaseDialogVisible.value = false
  resetReleaseDialog()
  const dialogGeneration = releaseDialogGeneration
  const isCurrent = () => pageContext === JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath]) && generation === pqcReleaseRouteGeneration && dialogGeneration === releaseDialogGeneration && detailSequence === detailRequestSequence
  queryParams.applicationId = undefined
  queryParams.pqcReleaseWorkTaskId = undefined
  queryParams.workOrderCode = typeof route.query.workOrderCode === 'string' ? route.query.workOrderCode : ''
  queryParams.pageNo = 1
  try {
    const context = readPqcReleaseRouteContext()
    if (context) {
      if (!userStore.permissions.has('mes:pro-production-release:query') && !userStore.permissions.has('*:*:*')) {
        throw new Error('当前账号没有生产放行查询权限。')
      }
      queryParams.applicationId = context.applicationId
      queryParams.pqcReleaseWorkTaskId = context.pqcReleaseWorkTaskId
      loading.value = true
      const decision = await getPqcProductionRelease(context.applicationId, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
      if (!isCurrent()) return
      const state = requirePqcReleaseAuthority(decision, context)
      if (state === 'APPROVED') {
        loading.value = false
        await openCompletedPqcReleaseDetail(context, decision, isCurrent)
        return
      }
      activeView.value = PQC_RELEASE_VIEW_PENDING
    }
    await getList(isCurrent)
    if (!isCurrent() || loadError.value || !context) return
    const row = list.value[0]
    if (!row || row.viewStatus !== PQC_RELEASE_VIEW_PENDING ||
      row.applicationStatus !== 'PQC_RELEASE_PENDING' || row.underReview || row.approvalReady === false) {
      throw new Error(row?.approvalBlockerReason || '当前生产放行申请不符合待放行任务状态。')
    }
    if (!userStore.permissions.has('mes:pro-production-release:pqc-approve') && !userStore.permissions.has('*:*:*')) {
      throw new Error('当前账号没有生产放行权限。')
    }
    openReleaseDialog(row)
  } catch (error) {
    if (!isCurrent()) return
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '生产放行待办加载失败。')
  } finally {
    if (isCurrent()) loading.value = false
  }
}

watch(
  () => route.fullPath,
  () => { invalidatePqcReleasePage(); void openPqcReleaseFromRoute() },
  { flush: 'sync' }
)

const handleTabChange = () => {
  queryParams.pageNo = 1
  getList()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  getList()
}

const resolveStatusLabel = (row: MesPqcProductionReleasePageItemRespVO) => {
  if (row.underReview && row.viewStatus === PQC_RELEASE_VIEW_PENDING) return '评审中'
  if (
    row.viewStatus === PQC_RELEASE_VIEW_PENDING &&
    row.nonconformanceDisposition === 'concession_release'
  ) {
    return '待让步签字'
  }
  return {
    [PQC_RELEASE_VIEW_PENDING]: '待放行',
    [PQC_RELEASE_VIEW_RELEASED]: '已放行',
    [PQC_RELEASE_VIEW_VOIDED]: '已作废',
    [PQC_RELEASE_VIEW_REWORKED]: '已返工',
    [PQC_RELEASE_VIEW_CONCESSION_RELEASED]: '已让步放行'
  }[row.viewStatus]
}

const resolveStatusTagType = (row: MesPqcProductionReleasePageItemRespVO) => {
  if (row.underReview && row.viewStatus === PQC_RELEASE_VIEW_PENDING) return 'warning'
  if (
    row.viewStatus === PQC_RELEASE_VIEW_RELEASED ||
    row.viewStatus === PQC_RELEASE_VIEW_CONCESSION_RELEASED
  ) {
    return 'success'
  }
  if (row.viewStatus === PQC_RELEASE_VIEW_VOIDED) return 'info'
  if (row.viewStatus === PQC_RELEASE_VIEW_REWORKED) return 'warning'
  return 'primary'
}

let releaseDialogGeneration = 0
const releaseRequestScopeKey = (row: MesPqcProductionReleasePageItemRespVO) =>
  JSON.stringify([approvalInvalidation.syncSession().key, row.applicationId, row.pqcReleaseWorkTaskId])

const captureReleaseContext = () => {
  const generation = releaseDialogGeneration
  const key = JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath, selectedRow.value?.applicationId, selectedRow.value?.pqcReleaseWorkTaskId, selectedRow.value?.version])
  return () => releaseDialogVisible.value && generation === releaseDialogGeneration &&
    key === JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath, selectedRow.value?.applicationId, selectedRow.value?.pqcReleaseWorkTaskId, selectedRow.value?.version])
}
const invalidatePqcReleasePage = () => {
  pqcReleaseRouteGeneration++
  listRequestSequence++
  loading.value = false
  loadError.value = ''
  list.value = []
  total.value = 0
  releaseDialogVisible.value = false
  resetReleaseDialog()
  clearOrderDetail()
}
watch(releaseDialogVisible, (visible) => {
  if (!visible) resetReleaseDialog()
}, { flush: 'sync' })
watch(() => approvalInvalidation.syncSession().key, () => {
  invalidatePqcReleasePage()
  void openPqcReleaseFromRoute()
}, { flush: 'sync' })
onBeforeUnmount(invalidatePqcReleasePage)
onDeactivated(invalidatePqcReleasePage)

const openReleaseDialog = (row: MesPqcProductionReleasePageItemRespVO) => {
  resetReleaseDialog()
  selectedRow.value = row
  releaseError.value = ''
  releaseResult.value = undefined
  releaseOutcomeUncertain.value = releaseRequestStates.has(releaseRequestScopeKey(row))
  if (releaseOutcomeUncertain.value) releaseError.value = '本次申请已提交或结果尚未确认，请只读刷新正式回执后核对；禁止再次提交。'
  releaseForm.signaturePassword = ''
  releaseForm.udiControlDocumentNo = ''
  releaseForm.approvalOpinion = ''
  releaseDialogVisible.value = true
}

const resetReleaseDialog = () => {
  releaseDialogGeneration++
  releaseSubmitting.value = false
  releaseForm.signaturePassword = ''
  releaseForm.udiControlDocumentNo = ''
  releaseForm.approvalOpinion = ''
  releaseError.value = ''
  releaseResult.value = undefined
  releaseOutcomeUncertain.value = false
  selectedRow.value = undefined
}

const getOrCreateReleaseIdempotencyKey = (scopeKey: string, applicationId: string) => {
  const existingKey = releaseIdempotencyKeys.get(scopeKey)
  if (existingKey) return existingKey
  const nonce = crypto.getRandomValues(new Uint8Array(16))
  const key = `pqc-release-${applicationId}-${Date.now()}-${Array.from(nonce, value => value.toString(16).padStart(2, '0')).join('')}`
  releaseIdempotencyKeys.set(scopeKey, key)
  return key
}

const isReleaseTransportTimeout = (error: unknown) => {
  if (!error || typeof error !== 'object') return false
  const candidate = error as { isAxiosError?: unknown; code?: unknown; request?: unknown; response?: unknown }
  return candidate.isAxiosError === true && candidate.request != null && candidate.response == null &&
    (candidate.code === 'ECONNABORTED' || candidate.code === 'ETIMEDOUT')
}

// Matches MesReleaseFlowIdempotency.payloadHash: signed big-endian length + UTF-8, null length -1.
const pqcReleasePayloadHash = async (parts: ReadonlyArray<string | null>) => {
  const encoder = new TextEncoder()
  const encoded = parts.map(part => part === null ? null : encoder.encode(part))
  const bytes = new Uint8Array(encoded.reduce((total, part) => total + 4 + (part?.length ?? 0), 0))
  const view = new DataView(bytes.buffer)
  let offset = 0
  for (const part of encoded) {
    view.setInt32(offset, part === null ? -1 : part.length, false)
    offset += 4
    if (part !== null) { bytes.set(part, offset); offset += part.length }
  }
  return CryptoJS.SHA256(CryptoJS.lib.WordArray.create(bytes)).toString(CryptoJS.enc.Hex)
}

const freezeReleaseRequest = (row: MesPqcProductionReleasePageItemRespVO) => {
  const session = approvalInvalidation.syncSession()
  if (!isExactPositiveId(row.applicationId) || !isExactPositiveId(row.pqcReleaseWorkTaskId) ||
    !isExactPositiveId(session.loginUserId) || !Number.isSafeInteger(row.version) || row.version <= 0) {
    throw new Error('生产放行缺少精确申请、任务、账号或正式版本身份。')
  }
  const scopeKey = releaseRequestScopeKey(row)
  return Object.freeze({
    applicationId: String(row.applicationId), pqcReleaseWorkTaskId: String(row.pqcReleaseWorkTaskId),
    expectedVersion: row.version, actorId: session.loginUserId, scopeKey, session,
    idempotencyKey: getOrCreateReleaseIdempotencyKey(scopeKey, row.applicationId),
    signaturePassword: releaseForm.signaturePassword.trim(),
    udiControlDocumentNo: releaseForm.udiControlDocumentNo.trim(),
    approvalOpinion: releaseForm.approvalOpinion.trim() || undefined,
    event: approvalInvalidation.captureEdhrSuccess({ sourceTaskType: 'EDHR_WORK_TASK', sourceTaskId: String(row.pqcReleaseWorkTaskId) })
  })
}

type FrozenPqcReleaseRequest = ReturnType<typeof freezeReleaseRequest> & { payloadHash: string }

const assertReleasedReceipt = (result: MesPqcProductionReleaseDecisionRespVO, request: FrozenPqcReleaseRequest) => {
  if (requirePqcReleaseAuthority(result, request) !== 'APPROVED' ||
    result.decisionIdempotencyKey !== request.idempotencyKey ||
    !/^[a-f0-9]{64}$/.test(result.decisionPayloadHash ?? '') || result.decisionPayloadHash !== request.payloadHash ||
    String(result.decidedBy) !== request.actorId || result.udiControlDocumentNo !== request.udiControlDocumentNo) {
    throw new Error('正式决策回执缺少本次申请、任务、操作者、请求键或载荷关联；不能确认本次放行成功。')
  }
  const decisionVersion = request.expectedVersion + 2
  if (!Number.isSafeInteger(decisionVersion) || result.version !== decisionVersion) {
    throw new Error('正式决策回执版本与本次申请的生产放行版本不一致；不能确认本次放行成功。')
  }
}

const confirmReleasedReceipt = (result: MesPqcProductionReleaseDecisionRespVO, request: FrozenPqcReleaseRequest) => {
  assertReleasedReceipt(result, request)
  if (releaseRequestStates.get(request.scopeKey) === 'CONFIRMED') return
  releaseRequestStates.set(request.scopeKey, 'CONFIRMED')
  releaseIdempotencyKeys.delete(request.scopeKey)
  // Formal success belongs to the verified original request, even after this UI leaves.
  approvalInvalidation.publishEdhrSuccess(request.event)
}

const applyReleaseSuccess = async (
  row: MesPqcProductionReleasePageItemRespVO,
  result: MesPqcProductionReleaseDecisionRespVO,
  request: FrozenPqcReleaseRequest,
  recovered: boolean,
  isCurrent: () => boolean
) => {
  if (!isCurrent()) return
  releaseIdempotencyKeys.delete(request.scopeKey)
  releaseForm.signaturePassword = ''
  releaseForm.udiControlDocumentNo = ''
  releaseOutcomeUncertain.value = false
  releaseResult.value = result
  activeView.value = PQC_RELEASE_VIEW_COMPLETED
  queryParams.pageNo = 1
  recovered ? message.warning('正式回执已确认本次生产放行完成') : message.success('生产放行完成')
  await getList(isCurrent)
  if (!isCurrent()) return
  if (loadError.value) {
    releaseError.value = `生产放行已成功，列表刷新失败：${loadError.value}`
    message.warning(releaseError.value)
  }
}

const recoverUncertainRelease = async (
  request: FrozenPqcReleaseRequest,
  writeError: unknown,
  isCurrent: () => boolean
): Promise<MesPqcProductionReleaseDecisionRespVO | undefined> => {
  if (!isReleaseTransportTimeout(writeError)) {
    throw new Error('当前错误不符合无服务端响应的传输超时回执恢复条件。')
  }
  try {
    const receipt = await getPqcProductionRelease(request.applicationId, {
      ignoreErrorMessage: true,
      isRequestContextCurrent: isCurrent,
      isSuccessReceiptContextCurrent: () => approvalInvalidation.isCurrent(request.session.key)
    })
    confirmReleasedReceipt(receipt, request)
    return receipt
  } catch (receiptError) {
    if (!isCurrent()) return
    releaseOutcomeUncertain.value = true
    releaseError.value = `生产放行响应不确定，正式回执无法证明本次请求成功，请只读刷新核对。` +
      `写入错误：${resolveErrorMessage(writeError, '响应异常')}；回执错误：${resolveErrorMessage(receiptError, '查询失败')}`
    return
  }
}

const submitRelease = async () => {
  const row = selectedRow.value
  if (!row || !releaseDialogVisible.value || releaseSubmitting.value || releaseOutcomeUncertain.value ||
    releaseRequestStates.has(releaseRequestScopeKey(row))) return
  const isCurrent = captureReleaseContext()
  const signaturePassword = releaseForm.signaturePassword.trim()
  const udiControlDocumentNo = releaseForm.udiControlDocumentNo.trim()
  if (!udiControlDocumentNo || udiControlDocumentNo.length > 128 || !signaturePassword) {
    releaseError.value = !udiControlDocumentNo ? 'UDI编号不能为空。' :
      udiControlDocumentNo.length > 128 ? 'UDI编号长度不能超过128个字符。' : '电子签名密码不能为空。'
    return
  }
  const scopeKey = releaseRequestScopeKey(row)
  releaseRequestStates.set(scopeKey, 'WRITING')
  let frozen: ReturnType<typeof freezeReleaseRequest>
  try { frozen = freezeReleaseRequest(row) }
  catch (error) {
    releaseRequestStates.delete(scopeKey)
    if (isCurrent()) releaseError.value = resolveErrorMessage(error, '正式来源身份无效。')
    return
  }
  releaseSubmitting.value = true
  releaseError.value = ''
  let request: FrozenPqcReleaseRequest
  try {
    const payloadHash = await pqcReleasePayloadHash(['APPROVE', frozen.applicationId, frozen.pqcReleaseWorkTaskId,
      String(frozen.expectedVersion), frozen.actorId, frozen.approvalOpinion ?? null, frozen.udiControlDocumentNo])
    if (!isCurrent()) { releaseRequestStates.delete(frozen.scopeKey); return }
    request = Object.freeze({ ...frozen, payloadHash })
    // Preserve receipt identity across remounts without retaining the signature password.
    const { signaturePassword: _signaturePassword, ...receiptIdentity } = request
    releaseRequestOwner.pqcReleaseRequests!.requests.set(request.scopeKey, Object.freeze(receiptIdentity))
  } catch (error) {
    releaseRequestStates.delete(frozen.scopeKey)
    if (isCurrent()) { releaseSubmitting.value = false; releaseError.value = resolveErrorMessage(error, '请求载荷计算失败。') }
    return
  }
  let result: MesPqcProductionReleaseDecisionRespVO
  let recovered = false
  try {
    result = await approvePqcProductionRelease({
      applicationId: request.applicationId, pqcReleaseWorkTaskId: request.pqcReleaseWorkTaskId,
      expectedVersion: request.expectedVersion, idempotencyKey: request.idempotencyKey,
      signaturePassword: request.signaturePassword, udiControlDocumentNo: request.udiControlDocumentNo,
      approvalOpinion: request.approvalOpinion
    }, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent , isSuccessReceiptContextCurrent: (() => { const receiptSessionKey = approvalInvalidation.syncSession().key; return () => approvalInvalidation.isCurrent(receiptSessionKey) })()})
    confirmReleasedReceipt(result, request)
  } catch (error) {
    if (isReleaseTransportTimeout(error)) {
      releaseRequestStates.set(request.scopeKey, 'UNCERTAIN')
      const receipt = await recoverUncertainRelease(request, error, isCurrent)
      if (!isCurrent()) return
      if (!receipt) { releaseSubmitting.value = false; return }
      result = receipt
      recovered = true
    } else {
      // A response/status/error code is not an authoritative pre-commit rejection receipt.
      releaseRequestStates.set(request.scopeKey, 'UNCERTAIN')
      if (!isCurrent()) return
      releaseForm.signaturePassword = ''
      releaseOutcomeUncertain.value = true
      releaseError.value =
        `生产放行结果无法确认，当前错误不符合自动回执恢复条件，请只读核对；禁止再次提交：${resolveErrorMessage(error, '响应异常')}`
      releaseSubmitting.value = false
      return
    }
  }
  try {
    await applyReleaseSuccess(row, result, request, recovered, isCurrent)
  } catch (error) {
    if (!isCurrent()) return
    releaseOutcomeUncertain.value = true
    releaseError.value = `生产放行写入已完成，回执或列表读取未完成，请只读刷新核对：${resolveErrorMessage(error, '回执或读取失败。')}`
    message.warning(releaseError.value)
  } finally {
    if (isCurrent()) releaseSubmitting.value = false
  }
}

const openNonconformanceReview = (row: MesPqcProductionReleasePageItemRespVO) => {
  if (!hasActiveOrderDetail(row)) {
    message.error('缺少正式活跃订单来源，无法发起不合格审查')
    return
  }
  router.push({
    name: 'MesProFeedbackEdhrNonconformanceReview',
    query: {
      activeOrderId: row.activeOrderId,
      autoCreate: '1'
    }
  })
}

const hasActiveOrderDetail = (row: MesPqcProductionReleasePageItemRespVO) =>
  /^[1-9]\d*$/.test(String(row.activeOrderId)) &&
  Number.isSafeInteger(Number(row.activeOrderId))

const openActiveOrderDetail = async (row: MesPqcProductionReleasePageItemRespVO) => {
  if (!hasActiveOrderDetail(row)) {
    message.error('缺少正式活跃订单来源，无法查看详情')
    return
  }
  clearOrderDetail()
  const sequence = detailRequestSequence
  const pageContext = JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath])
  const current = () => sequence === detailRequestSequence && pageContext === JSON.stringify([approvalInvalidation.syncSession().key, route.fullPath])
  listRequestSequence++
  loading.value = false
  detailRow.value = row
  detailApplicationId.value = row.applicationId
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  orderDetail.value = undefined
  try {
    const result = await getPqcProductionReleaseOrderDetail(row.applicationId, { ignoreErrorMessage: true, isRequestContextCurrent: current })
    if (!current()) return
    orderDetail.value = result
  } catch (error) {
    if (!current()) return
    detailError.value = resolveErrorMessage(error, '订单详情加载失败')
  } finally {
    if (current()) detailLoading.value = false
  }
}

const retryOrderDetail = () => {
  if (detailReadOnly.value) void openPqcReleaseFromRoute()
  else if (detailRow.value) void openActiveOrderDetail(detailRow.value)
}

onMounted(() => openPqcReleaseFromRoute())
</script>

<style scoped>
.pqc-release-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pqc-release-page__tabs,
.pqc-release-page__filters {
  margin-bottom: 0;
}

.pqc-release-page__primary {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.pqc-release-page__secondary {
  margin-top: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.pqc-release-page__actions {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.pqc-release-dialog {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pqc-release-dialog__form {
  padding-top: 4px;
}
</style>
