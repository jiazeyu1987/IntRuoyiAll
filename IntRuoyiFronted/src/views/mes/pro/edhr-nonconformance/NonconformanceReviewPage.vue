<template>
  <ContentWrap>
    <ActiveOrderDetailLayout v-if="activeOrderDetailVisible" data-edhr-ncr-active-order-detail>
      <el-button
        data-edhr-ncr-active-order-detail-back
        @click="activeOrderDetailVisible = false"
      >
        返回
      </el-button>
      <ActiveOrderSubmissionDetailPanel
        :detail="activeOrderDetail"
        :production-material-lists="[]"
        :loading="activeOrderDetailLoading"
        :error="activeOrderDetailError"
        @retry="retryActiveOrderDetail"
      />
    </ActiveOrderDetailLayout>
    <div v-show="!activeOrderDetailVisible" class="edhr-ncr" data-edhr-ncr-page>
      <EdhrBatchRecordTabs active-tab="nonconformanceReview" />
      <div class="edhr-ncr__header">
        <div>
          <div class="edhr-ncr__title">不合格评审</div>
          <div class="edhr-ncr__subtitle">冻结后禁止报工、PQC提交、PQC放行</div>
        </div>
        <el-button @click="loadPendingReviews">刷新</el-button>
      </div>

      <el-alert v-if="errorText" :title="errorText" type="error" :closable="false" show-icon />

      <div v-if="canCreateEntry" class="edhr-ncr__section">
        <div class="edhr-ncr__section-title">发起评审</div>
        <el-form label-width="110px" :model="entryForm">
          <el-form-item label="来源">
            <el-tag>{{ resolveSourceTypeLabel(entryForm.sourceType) }}</el-tag>
          </el-form-item>
          <el-form-item :label="entryBatchExecutionId ? '批次执行ID' : '放行申请ID'">
            <el-input :model-value="String(entryBatchExecutionId || entrySourceId)" disabled />
          </el-form-item>
          <el-form-item label="不合格原因" required>
            <el-input
              v-model="entryForm.nonconformanceReason"
              data-edhr-ncr-create-reason
              type="textarea"
              :rows="3"
              placeholder="请输入不合格原因"
            />
          </el-form-item>
          <el-form-item label="电子签名密码" required>
            <el-input
              v-model="entryForm.signaturePassword"
              data-edhr-ncr-create-signature-password
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="请输入本人电子签名密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="danger"
              :loading="createLoading"
              data-edhr-ncr-create-submit
              @click="submitCreateReview"
            >
              提交不合格评审
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="edhr-ncr__layout">
        <div class="edhr-ncr__section">
          <div class="edhr-ncr__section-head">
            <div class="edhr-ncr__section-title">QA冻结批次列表</div>
            <el-tag type="warning">{{ total }} 个待评审</el-tag>
          </div>
          <el-table
            v-loading="listLoading"
            :data="pendingReviews"
            stripe
            :show-overflow-tooltip="true"
            empty-text="暂无不合格冻结批次"
            @row-click="selectReview"
          >
            <el-table-column label="评审单" min-width="210">
              <template #default="{ row }">
                <div class="edhr-ncr__strong">{{ row.reviewCode || '--' }}</div>
                <div class="edhr-ncr__muted">{{ resolveSourceTypeLabel(row.sourceType) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="批次" min-width="220">
              <template #default="{ row }">
                <div class="edhr-ncr__strong">{{ row.batchExecutionCode || '--' }}</div>
                <div class="edhr-ncr__muted">
                  {{ row.workOrderCode || '--' }} / {{ row.batchCode || '--' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="冻结时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.frozenAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  data-edhr-ncr-active-order-detail
                  @click.stop="openActiveOrderDetail(row)"
                >
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @pagination="loadPendingReviews"
          />
        </div>

        <div class="edhr-ncr__section edhr-ncr__detail">
          <div class="edhr-ncr__section-title">评审详情</div>
          <template v-if="selectedReview">
            <div class="edhr-ncr__summary">
              <div>
                <span class="edhr-ncr__label">不合格原因</span>
                <span>{{ selectedReview.nonconformanceReason || '--' }}</span>
              </div>
              <div>
                <span class="edhr-ncr__label">批次</span>
                <span>{{
                  selectedReview.batchExecutionCode || selectedReview.batchExecutionId || '--'
                }}</span>
              </div>
              <div>
                <span class="edhr-ncr__label">冻结时间</span>
                <span>{{ formatDateTime(selectedReview.frozenAt) }}</span>
              </div>
            </div>

            <template v-if="selectedReview.reviewStatus === REVIEW_STATUS_PENDING_REVIEW">
              <el-form label-width="110px" :model="disposeForm" class="edhr-ncr__dispose-form">
                <el-form-item label="评审材料" required data-edhr-ncr-review-material>
                  <UploadFile
                    :is-show-tip="false"
                    v-model="disposeForm.reviewMaterialUrls"
                    directory="dcc/controlled-file/unclassified/nonconformance-review"
                    :limit="5"
                  />
                </el-form-item>
                <el-form-item label="评审意见" required>
                  <el-input
                    v-model="disposeForm.reviewOpinion"
                    data-edhr-ncr-review-opinion
                    type="textarea"
                    :rows="4"
                    placeholder="请输入评审意见"
                  />
                </el-form-item>
                <el-form-item label="电子签名密码" required>
                  <el-input
                    v-model="disposeForm.signaturePassword"
                    data-edhr-ncr-signature-password
                    type="password"
                    show-password
                    autocomplete="new-password"
                    placeholder="请输入本人电子签名密码"
                  />
                </el-form-item>
                <el-form-item>
                  <div class="edhr-ncr__buttons">
                    <el-button
                      type="success"
                      :loading="disposeLoading"
                      data-edhr-ncr-concession-release
                      @click="handleDispose(DISPOSITION_CONCESSION_RELEASE)"
                    >
                      让步放行
                    </el-button>
                    <el-button
                      type="warning"
                      :loading="disposeLoading"
                      @click="handleDispose(DISPOSITION_REWORK)"
                    >
                      返工
                    </el-button>
                    <el-button
                      type="danger"
                      :loading="disposeLoading"
                      @click="handleDispose(DISPOSITION_VOID)"
                    >
                      作废
                    </el-button>
                  </div>
                </el-form-item>
              </el-form>
            </template>

            <template v-else>
              <el-result
                icon="success"
                data-edhr-ncr-disposition-result
                :title="resolveDispositionLabel(selectedReview.disposition)"
                :sub-title="resolveDispositionNote(selectedReview.disposition)"
              />
              <div class="edhr-ncr__summary">
                <div>
                  <span class="edhr-ncr__label">评审材料</span>
                  <span>{{ resolveReviewMaterialDisplay(selectedReview) }}</span>
                </div>
                <div>
                  <span class="edhr-ncr__label">评审意见</span>
                  <span>{{ selectedReview.reviewOpinion || '--' }}</span>
                </div>
                <div>
                  <span class="edhr-ncr__label">电子签名</span>
                  <span data-edhr-ncr-qa-signature>{{ selectedReview.qaSignature || '--' }}</span>
                </div>
                <div>
                  <span class="edhr-ncr__label">完成时间</span>
                  <span>{{ formatDateTime(selectedReview.closedAt) }}</span>
                </div>
              </div>
            </template>
          </template>
          <el-empty v-else description="请选择一个待评审批次" />
        </div>
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { UploadFile } from '@/components/UploadFile'
import ActiveOrderSubmissionDetailPanel from '@/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import ActiveOrderDetailLayout from '@/views/mes/pro/processpool/components/ActiveOrderDetailLayout.vue'
import EdhrBatchRecordTabs from '../edhr-batch/EdhrBatchRecordTabs.vue'
import {
  DISPOSITION_CONCESSION_RELEASE,
  DISPOSITION_REWORK,
  DISPOSITION_VOID,
  REVIEW_STATUS_PENDING_REVIEW,
  SOURCE_TYPE_PQC_RELEASE,
  SOURCE_TYPE_PQC_SUBMISSION,
  createNonconformanceReview,
  disposeNonconformanceReview,
  getNonconformanceReview,
  getNonconformanceReviewActiveOrderDetail,
  getPendingNonconformanceReviewPage,
  type EdhrNonconformanceReviewMaterial,
  type EdhrNonconformanceReviewMaterialEvent,
  type EdhrNonconformanceReviewDisposition,
  type EdhrNonconformanceReviewPageReqVO,
  type EdhrNonconformanceReviewRespVO,
  type EdhrNonconformanceReviewSourceType
} from '@/api/mes/pro/edhr/nonconformanceReview'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProFeedbackEdhrNonconformanceReview' })

const route = useRoute()
const message = useMessage()

const listLoading = ref(false)
const createLoading = ref(false)
const disposeLoading = ref(false)
const errorText = ref('')
const pendingReviews = ref<EdhrNonconformanceReviewRespVO[]>([])
const selectedReview = ref<EdhrNonconformanceReviewRespVO>()
const total = ref(0)
const activeOrderDetailVisible = ref(false)
const activeOrderDetailLoading = ref(false)
const activeOrderDetailError = ref('')
const activeOrderDetail = ref<Awaited<ReturnType<typeof getNonconformanceReviewActiveOrderDetail>>>()
const activeOrderDetailRow = ref<EdhrNonconformanceReviewRespVO>()
let activeOrderDetailRequestSequence = 0

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10
})

const entryBatchExecutionId = computed(() =>
  parsePositiveRouteQueryId(route.query.batchExecutionId)
)
const entrySourceId = computed(() => parsePositiveRouteQueryId(route.query.sourceId))
const entrySourceType = computed<EdhrNonconformanceReviewSourceType>(() =>
  route.query.sourceType === SOURCE_TYPE_PQC_RELEASE
    ? SOURCE_TYPE_PQC_RELEASE
    : SOURCE_TYPE_PQC_SUBMISSION
)
const canCreateEntry = computed(
  () =>
    Boolean(entryBatchExecutionId.value) ||
    Boolean(entrySourceId.value)
)

const entryForm = reactive({
  sourceType: SOURCE_TYPE_PQC_SUBMISSION as EdhrNonconformanceReviewSourceType,
  nonconformanceReason: '',
  signaturePassword: ''
})

const disposeForm = reactive({
  reviewMaterialUrls: [] as string[],
  reviewMaterialEvents: [] as EdhrNonconformanceReviewMaterialEvent[],
  reviewOpinion: '',
  signaturePassword: ''
})
let materialTrackingEnabled = true
let materialEventSequence = 0

type ReviewMaterialDisplay = {
  url: string
  fileName?: string
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const formatDateTime = (value?: string | number) => formatEdhrDateTime(value)

const resolveSourceTypeLabel = (sourceType?: string) => {
  if (sourceType === SOURCE_TYPE_PQC_RELEASE) return 'PQC生产放行'
  if (sourceType === SOURCE_TYPE_PQC_SUBMISSION) return 'PQC提交记录'
  return '未知来源'
}

const resolveDispositionLabel = (disposition?: string) => {
  if (disposition === DISPOSITION_CONCESSION_RELEASE) return '让步放行'
  if (disposition === DISPOSITION_REWORK) return '返工'
  if (disposition === DISPOSITION_VOID) return '作废'
  return '未处置'
}

const resolveDispositionNote = (disposition?: string) => {
  if (disposition === DISPOSITION_CONCESSION_RELEASE) return '批次已解冻，继续主流程。'
  if (disposition === DISPOSITION_REWORK) return 'QA已确认返工，MVP直接回到主流程。'
  if (disposition === DISPOSITION_VOID) return '批次已作废，后续只允许只读追溯。'
  return ''
}

const decodeFileName = (value: string) => {
  let current = value.replace(/\+/g, ' ')
  let decoded = decodeURIComponent(current)
  while (decoded !== current) {
    current = decoded.replace(/\+/g, ' ')
    decoded = decodeURIComponent(current)
  }
  return decoded
}

const resolveFileName = (url: string) => {
  const pathname = new URL(url, window.location.origin).pathname
  const fileName = pathname.substring(pathname.lastIndexOf('/') + 1)
  return decodeFileName(fileName)
}

const parseReviewMaterialsJson = (review?: EdhrNonconformanceReviewRespVO): ReviewMaterialDisplay[] => {
  if (!review?.reviewMaterialsJson) {
    return review?.reviewMaterialUrl
      ? review.reviewMaterialUrl.split(',').filter(Boolean).map((url) => ({ url }))
      : []
  }
  try {
    const payload = JSON.parse(review.reviewMaterialsJson)
    const activeMaterials = Array.isArray(payload?.activeMaterials) ? payload.activeMaterials : []
    return activeMaterials
      .map((material) => ({
        url: material?.url,
        fileName: typeof material?.fileName === 'string' ? material.fileName.trim() : undefined
      }))
      .filter(
        (material): material is ReviewMaterialDisplay =>
          typeof material.url === 'string' && material.url.trim().length > 0
      )
  } catch (error) {
    throw new Error('评审材料清单格式无效，无法继续显示。')
  }
}

const parseReviewMaterialUrls = (review?: EdhrNonconformanceReviewRespVO) =>
  parseReviewMaterialsJson(review).map((material) => material.url)

const resolveReviewMaterialName = (material: ReviewMaterialDisplay) => {
  const persistedName = material.fileName?.trim()
  if (persistedName) {
    return persistedName.includes('%') ? decodeFileName(persistedName) : persistedName
  }
  return resolveFileName(material.url)
}

const resolveReviewMaterialDisplay = (review?: EdhrNonconformanceReviewRespVO) => {
  const materials = parseReviewMaterialsJson(review)
  if (!materials.length) return '--'
  return materials.map(resolveReviewMaterialName).join('、')
}

const buildReviewMaterials = (): EdhrNonconformanceReviewMaterial[] =>
  disposeForm.reviewMaterialUrls.map((url, index) => ({
    url,
    fileName: resolveFileName(url),
    sortNo: index + 1
  }))

const resetDisposeForm = () => {
  materialTrackingEnabled = false
  disposeForm.reviewMaterialUrls = []
  disposeForm.reviewMaterialEvents = []
  materialEventSequence = 0
  nextTick(() => {
    materialTrackingEnabled = true
  })
  disposeForm.reviewOpinion = ''
  disposeForm.signaturePassword = ''
}

const buildPendingReviewQuery = () => {
  const query: EdhrNonconformanceReviewPageReqVO = { ...queryParams }
  if (entrySourceId.value) {
    query.sourceType = entryForm.sourceType
    query.sourceId = entrySourceId.value
    return query
  }
  if (entryBatchExecutionId.value) {
    query.batchExecutionId = entryBatchExecutionId.value
  }
  return query
}

const loadPendingReviews = async () => {
  listLoading.value = true
  errorText.value = ''
  try {
    const data = await getPendingNonconformanceReviewPage(buildPendingReviewQuery())
    pendingReviews.value = data.list || []
    total.value = data.total || 0
    const selectedStillVisible =
      selectedReview.value?.id &&
      pendingReviews.value.some((review) => review.id === selectedReview.value?.id)
    if (pendingReviews.value.length === 0) {
      selectedReview.value = undefined
      resetDisposeForm()
    } else if (!selectedStillVisible) {
      selectReview(pendingReviews.value[0])
    }
  } catch (error) {
    pendingReviews.value = []
    total.value = 0
    selectedReview.value = undefined
    errorText.value = resolveErrorMessage(error, '不合格评审列表加载失败。')
  } finally {
    listLoading.value = false
  }
}

const loadReviewFromRoute = async () => {
  const reviewId = parsePositiveRouteQueryId(route.query.reviewId)
  if (!reviewId) return
  try {
    selectedReview.value = await getNonconformanceReview(reviewId)
    fillDisposeForm(selectedReview.value)
  } catch (error) {
    errorText.value = resolveErrorMessage(error, '不合格评审详情加载失败。')
  }
}

const selectReview = (review: EdhrNonconformanceReviewRespVO) => {
  selectedReview.value = review
  fillDisposeForm(review)
}

const openActiveOrderDetail = async (review: EdhrNonconformanceReviewRespVO) => {
  if (!review.id || !review.activeOrderId) {
    message.error('缺少正式活跃订单来源，无法查看详情。')
    return
  }
  const sequence = ++activeOrderDetailRequestSequence
  activeOrderDetailRow.value = review
  activeOrderDetailVisible.value = true
  activeOrderDetailLoading.value = true
  activeOrderDetailError.value = ''
  activeOrderDetail.value = undefined
  try {
    const result = await getNonconformanceReviewActiveOrderDetail(review.id)
    if (sequence !== activeOrderDetailRequestSequence) return
    activeOrderDetail.value = result
  } catch (error) {
    if (sequence !== activeOrderDetailRequestSequence) return
    activeOrderDetailError.value = resolveErrorMessage(error, '活跃订单详情加载失败。')
  } finally {
    if (sequence === activeOrderDetailRequestSequence) {
      activeOrderDetailLoading.value = false
    }
  }
}

const retryActiveOrderDetail = () => {
  if (activeOrderDetailRow.value) void openActiveOrderDetail(activeOrderDetailRow.value)
}

const fillDisposeForm = (review: EdhrNonconformanceReviewRespVO) => {
  materialTrackingEnabled = false
  disposeForm.reviewMaterialUrls = parseReviewMaterialUrls(review)
  disposeForm.reviewMaterialEvents = []
  materialEventSequence = 0
  nextTick(() => {
    materialTrackingEnabled = true
  })
  disposeForm.reviewOpinion = review.reviewOpinion || ''
  disposeForm.signaturePassword = ''
}

const submitCreateReview = async () => {
  const batchExecutionId = entryBatchExecutionId.value
  if (
    !batchExecutionId &&
    !entrySourceId.value
  ) {
    message.error('缺少批次执行或来源记录，无法发起不合格评审。')
    return
  }
  const reason = entryForm.nonconformanceReason.trim()
  if (!reason) {
    message.error('不合格原因不能为空。')
    return
  }
  const signaturePassword = entryForm.signaturePassword.trim()
  if (!signaturePassword) {
    message.error('电子签名密码不能为空。')
    return
  }
  createLoading.value = true
  errorText.value = ''
  try {
    const review = await createNonconformanceReview({
      sourceType: entryForm.sourceType,
      sourceId: entrySourceId.value,
      batchExecutionId: batchExecutionId || undefined,
      nonconformanceReason: reason,
      signaturePassword
    })
    selectedReview.value = review
    resetDisposeForm()
    entryForm.nonconformanceReason = ''
    entryForm.signaturePassword = ''
    message.success(
      batchExecutionId ? '不合格评审已创建，批次已冻结' : '不合格评审已创建，工单已冻结'
    )
    await loadPendingReviews()
  } catch (error) {
    errorText.value = resolveErrorMessage(error, '不合格评审创建失败。')
    message.error(errorText.value)
  } finally {
    createLoading.value = false
  }
}

const handleDispose = async (disposition: EdhrNonconformanceReviewDisposition) => {
  if (!selectedReview.value?.id) {
    message.error('请选择待处置评审单。')
    return
  }
  if (
    disposeForm.reviewMaterialUrls.length === 0 ||
    !disposeForm.reviewOpinion.trim() ||
    !disposeForm.signaturePassword.trim()
  ) {
    message.error('评审材料、评审意见和电子签名密码均不能为空。')
    return
  }
  disposeLoading.value = true
  errorText.value = ''
  try {
    const review = await disposeNonconformanceReview({
      id: selectedReview.value.id,
      disposition,
      reviewMaterialUrl: disposeForm.reviewMaterialUrls.join(','),
      reviewMaterials: buildReviewMaterials(),
      reviewMaterialEvents: disposeForm.reviewMaterialEvents,
      reviewOpinion: disposeForm.reviewOpinion.trim(),
      signaturePassword: disposeForm.signaturePassword.trim()
    })
    selectedReview.value = review
    message.success(`已${resolveDispositionLabel(disposition)}`)
    await loadPendingReviews()
  } catch (error) {
    errorText.value = resolveErrorMessage(error, '不合格评审处置失败。')
    message.error(errorText.value)
  } finally {
    disposeLoading.value = false
  }
}

watch(
  () => [...disposeForm.reviewMaterialUrls],
  (nextUrls, previousUrls) => {
    if (!materialTrackingEnabled) return
    const previous = previousUrls || []
    const additions = [...nextUrls]
    const removals = [...previous]
    for (const url of previous) {
      const index = additions.indexOf(url)
      if (index >= 0) additions.splice(index, 1)
    }
    for (const url of nextUrls) {
      const index = removals.indexOf(url)
      if (index >= 0) removals.splice(index, 1)
    }
    additions.forEach((url) => {
      disposeForm.reviewMaterialEvents.push({
        action: 'UPLOAD',
        url,
        fileName: resolveFileName(url),
        sequence: ++materialEventSequence
      })
    })
    removals.forEach((url) => {
      disposeForm.reviewMaterialEvents.push({
        action: 'DELETE',
        url,
        fileName: resolveFileName(url),
        sequence: ++materialEventSequence
      })
    })
  }
)

watch(
  () =>
    [
      route.name,
      route.query.batchExecutionId,
      route.query.sourceType,
      route.query.sourceId,
      route.query.reviewId
    ] as const,
  ([routeName]) => {
    if (routeName !== 'MesProFeedbackEdhrNonconformanceReview') return
    entryForm.sourceType = entrySourceType.value
    loadReviewFromRoute()
  }
)

onMounted(async () => {
  entryForm.sourceType = entrySourceType.value
  await loadPendingReviews()
  await loadReviewFromRoute()
})
</script>

<style scoped>
.edhr-ncr {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-ncr__header,
.edhr-ncr__section {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-ncr__header,
.edhr-ncr__section-head,
.edhr-ncr__buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edhr-ncr__title {
  color: #172033;
  font-size: 18px;
  font-weight: 700;
}

.edhr-ncr__subtitle,
.edhr-ncr__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-ncr__layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 16px;
}

.edhr-ncr__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-weight: 700;
}

.edhr-ncr__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-ncr__detail {
  align-self: start;
}

.edhr-ncr__summary {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
  color: #263247;
  font-size: 13px;
}

.edhr-ncr__label {
  display: inline-block;
  min-width: 86px;
  color: #4b5563;
}

.edhr-ncr__dispose-form {
  margin-top: 12px;
}

.edhr-ncr__buttons {
  justify-content: flex-start;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .edhr-ncr__layout {
    grid-template-columns: 1fr;
  }
}
</style>
