<template>
  <ContentWrap>
    <div class="pqc-release-page" data-pqc-production-release-page>
      <div class="pqc-release-page__header">
        <div>
          <h2>PQC生产放行</h2>
        </div>
        <el-button :icon="Refresh" :loading="loading" circle title="刷新" @click="getList" />
      </div>

      <el-tabs v-model="activeView" class="pqc-release-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="待放行" :name="PQC_RELEASE_VIEW_PENDING" />
        <el-tab-pane label="已放行" :name="PQC_RELEASE_VIEW_RELEASED" />
        <el-tab-pane label="已作废" :name="PQC_RELEASE_VIEW_VOIDED" />
        <el-tab-pane label="已返工" :name="PQC_RELEASE_VIEW_REWORKED" />
        <el-tab-pane label="已让步放行" :name="PQC_RELEASE_VIEW_CONCESSION_RELEASED" />
      </el-tabs>

      <el-form :inline="true" :model="queryParams" class="pqc-release-page__filters">
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderCode" clearable class="!w-200px" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable class="!w-200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">查询</el-button>
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
            <div class="pqc-release-page__primary">{{ row.workOrderCode || '--' }}</div>
            <div class="pqc-release-page__secondary">批次：{{ row.batchCode || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="申请" min-width="180">
          <template #default="{ row }">
            <div>申请编号：{{ row.applicationId }}</div>
            <div class="pqc-release-page__secondary">{{ formatDateTime(row.appliedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
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
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <div class="pqc-release-page__actions">
              <template v-if="activeView === PQC_RELEASE_VIEW_PENDING">
                <el-button
                  v-hasPermi="['mes:pro-production-release:pqc-approve']"
                  link
                  type="success"
                  :disabled="row.underReview || !row.approvalReady"
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
                  @click="openNonconformanceReview(row)"
                >
                  不合格审查
                </el-button>
              </template>
              <el-button
                v-else-if="row.batchExecutionId"
                link
                type="primary"
                @click="openBatchRecord(row.batchExecutionId)"
              >
                查看批记录
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <Dialog
      v-model="releaseDialogVisible"
      data-pqc-production-release-dialog
      title="确认生产放行"
      width="540px"
      @closed="resetReleaseDialog"
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

        <el-result
          v-if="releaseResult"
          icon="success"
          title="生产放行完成"
          :sub-title="`批次执行 ${releaseResult.batchExecutionId || '--'} 已创建，后续进入资料上传。`"
        >
          <template #extra>
            <el-button
              v-if="releaseResult.batchExecutionId"
              type="primary"
              @click="openBatchRecord(releaseResult.batchExecutionId)"
            >
              查看批记录
            </el-button>
          </template>
        </el-result>

        <el-form v-else label-width="118px" class="pqc-release-dialog__form">
          <el-form-item label="电子签名密码" required>
            <el-input
              v-model="releaseForm.signaturePassword"
              type="password"
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
          @click="submitRelease"
        >
          确认放行
        </el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { Refresh, RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  PQC_RELEASE_VIEW_CONCESSION_RELEASED,
  PQC_RELEASE_VIEW_PENDING,
  PQC_RELEASE_VIEW_RELEASED,
  PQC_RELEASE_VIEW_REWORKED,
  PQC_RELEASE_VIEW_VOIDED,
  approvePqcProductionRelease,
  getPqcProductionRelease,
  getPqcProductionReleasePage,
  type MesPqcProductionReleaseDecisionRespVO,
  type MesPqcProductionReleasePageItemRespVO,
  type MesPqcProductionReleaseViewStatus
} from '@/api/mes/pro/productionRelease'
import { SOURCE_TYPE_PQC_RELEASE } from '@/api/mes/pro/edhr/nonconformanceReview'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesPqcProductionRelease' })

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const list = ref<MesPqcProductionReleasePageItemRespVO[]>([])
const total = ref(0)
const activeView = ref<MesPqcProductionReleaseViewStatus>(PQC_RELEASE_VIEW_PENDING)
const releaseDialogVisible = ref(false)
const releaseSubmitting = ref(false)
const releaseError = ref('')
const selectedRow = ref<MesPqcProductionReleasePageItemRespVO>()
const releaseResult = ref<MesPqcProductionReleaseDecisionRespVO>()
const releaseOutcomeUncertain = ref(false)
const releaseIdempotencyKeys = new Map<string, string>()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  workOrderCode: '',
  batchCode: ''
})

const releaseForm = reactive({
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

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getPqcProductionReleasePage({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      viewStatus: activeView.value,
      workOrderCode: queryParams.workOrderCode.trim() || undefined,
      batchCode: queryParams.batchCode.trim() || undefined
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'PQC生产放行列表加载失败。')
  } finally {
    loading.value = false
  }
}

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
  if (row.underReview) return '评审中'
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
  if (row.underReview) return 'warning'
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

const openReleaseDialog = (row: MesPqcProductionReleasePageItemRespVO) => {
  selectedRow.value = row
  releaseError.value = ''
  releaseResult.value = undefined
  releaseOutcomeUncertain.value = false
  releaseForm.signaturePassword = ''
  releaseForm.approvalOpinion = ''
  releaseDialogVisible.value = true
}

const resetReleaseDialog = () => {
  releaseForm.signaturePassword = ''
  releaseForm.approvalOpinion = ''
  releaseError.value = ''
  releaseResult.value = undefined
  releaseOutcomeUncertain.value = false
  selectedRow.value = undefined
}

const getOrCreateReleaseIdempotencyKey = (applicationId: string) => {
  const existingKey = releaseIdempotencyKeys.get(applicationId)
  if (existingKey) return existingKey
  const key = `pqc-release-${applicationId}-${Date.now()}-${crypto.randomUUID()}`
  releaseIdempotencyKeys.set(applicationId, key)
  return key
}

const isDefinitiveReleaseBusinessFailure = (error: unknown) => {
  const code = (error as { code?: unknown } | null)?.code
  return typeof code === 'number' && Number.isFinite(code)
}

const assertReleasedReceipt = (result: MesPqcProductionReleaseDecisionRespVO) => {
  if (
    result.decision !== 'APPROVE' ||
    !['REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING', 'RELEASED'].includes(result.status) ||
    !result.batchExecutionId ||
    !result.signatureId
  ) {
    throw new Error('生产放行回执不完整，请刷新后核对。')
  }
}

const applyReleaseSuccess = async (
  row: MesPqcProductionReleasePageItemRespVO,
  result: MesPqcProductionReleaseDecisionRespVO,
  recovered: boolean
) => {
  assertReleasedReceipt(result)
  releaseIdempotencyKeys.delete(row.applicationId)
  releaseForm.signaturePassword = ''
  releaseOutcomeUncertain.value = false
  releaseResult.value = result
  activeView.value =
    row.nonconformanceDisposition === 'concession_release'
      ? PQC_RELEASE_VIEW_CONCESSION_RELEASED
      : PQC_RELEASE_VIEW_RELEASED
  queryParams.pageNo = 1
  await getList()
  recovered
    ? message.warning('响应异常，但权威回执已确认生产放行完成')
    : message.success('生产放行完成')
}

const recoverUncertainRelease = async (
  row: MesPqcProductionReleasePageItemRespVO,
  writeError: unknown
) => {
  let receipt: MesPqcProductionReleaseDecisionRespVO
  try {
    receipt = await getPqcProductionRelease(row.applicationId)
  } catch (receiptError) {
    releaseOutcomeUncertain.value = true
    releaseError.value =
      `生产放行响应不确定，权威回执查询失败。请刷新核对后再操作。` +
      `写入错误：${resolveErrorMessage(writeError, '响应异常')}；` +
      `回执错误：${resolveErrorMessage(receiptError, '查询失败')}`
    return
  }
  if (receipt.status === 'PQC_RELEASE_PENDING') {
    releaseOutcomeUncertain.value = false
    releaseError.value =
      `权威回执显示申请仍待放行，可重新输入签名密码并使用同一请求重试。` +
      `原错误：${resolveErrorMessage(writeError, '响应异常')}`
    return
  }
  if (receipt.status === 'PQC_RELEASE_REJECTED') {
    releaseIdempotencyKeys.delete(row.applicationId)
    releaseOutcomeUncertain.value = false
    releaseError.value =
      receipt.decision === 'NONCONFORMANCE_REWORK'
        ? '权威回执显示该申请已返工，不能继续放行。'
        : receipt.decision === 'NONCONFORMANCE_VOID'
          ? '权威回执显示该申请已作废，不能继续放行。'
          : '权威回执显示该申请已终结，不能继续放行。'
    if (receipt.decision === 'NONCONFORMANCE_REWORK') activeView.value = PQC_RELEASE_VIEW_REWORKED
    if (receipt.decision === 'NONCONFORMANCE_VOID') activeView.value = PQC_RELEASE_VIEW_VOIDED
    queryParams.pageNo = 1
    await getList()
    return
  }
  try {
    await applyReleaseSuccess(row, receipt, true)
  } catch (receiptError) {
    releaseOutcomeUncertain.value = true
    releaseError.value = resolveErrorMessage(receiptError, '权威回执状态无法确认。')
  }
}

const submitRelease = async () => {
  const row = selectedRow.value
  if (!row) return
  const signaturePassword = releaseForm.signaturePassword.trim()
  if (!signaturePassword) {
    releaseError.value = '电子签名密码不能为空。'
    return
  }
  releaseSubmitting.value = true
  releaseError.value = ''
  releaseOutcomeUncertain.value = false
  const idempotencyKey = getOrCreateReleaseIdempotencyKey(row.applicationId)
  try {
    const result = await approvePqcProductionRelease({
      applicationId: row.applicationId,
      pqcReleaseWorkTaskId: row.pqcReleaseWorkTaskId,
      expectedVersion: row.version,
      idempotencyKey,
      signaturePassword,
      approvalOpinion: releaseForm.approvalOpinion.trim() || undefined
    })
    await applyReleaseSuccess(row, result, false)
  } catch (error) {
    releaseForm.signaturePassword = ''
    if (isDefinitiveReleaseBusinessFailure(error)) {
      releaseError.value = resolveErrorMessage(error, '生产放行失败。')
    } else {
      await recoverUncertainRelease(row, error)
    }
  } finally {
    releaseSubmitting.value = false
  }
}

const openNonconformanceReview = (row: MesPqcProductionReleasePageItemRespVO) => {
  router.push({
    name: 'MesProFeedbackEdhrNonconformanceReview',
    query: {
      sourceType: SOURCE_TYPE_PQC_RELEASE,
      sourceId: row.applicationId,
      batchExecutionId: row.batchExecutionId || undefined
    }
  })
}

const openBatchRecord = (batchExecutionId: string) => {
  router.push({ name: 'MesProEdhrBatchExecutionDetail', query: { id: batchExecutionId } })
}

onMounted(getList)
</script>

<style scoped>
.pqc-release-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pqc-release-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.pqc-release-page__header h2 {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
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
  color: var(--el-text-color-secondary);
  font-size: 12px;
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
