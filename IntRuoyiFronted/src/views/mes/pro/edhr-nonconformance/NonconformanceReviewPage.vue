<template>
  <ContentWrap>
    <div class="edhr-ncr" data-edhr-ncr-page>
      <EdhrBatchRecordTabs active-tab="nonconformanceReview" />

      <div class="edhr-ncr__header">
        <div>
          <div class="edhr-ncr__title">不合格评审</div>
          <div class="edhr-ncr__subtitle">冻结后禁止报工、PQC提交、PQC放行</div>
        </div>
        <div class="edhr-ncr__header-actions">
          <el-button type="primary" data-edhr-ncr-open-create @click="openCreateDialog">
            新建
          </el-button>
          <el-button data-edhr-ncr-refresh @click="loadReviews">刷新</el-button>
        </div>
      </div>

      <el-alert v-if="errorText" :title="errorText" type="error" :closable="false" show-icon />

      <el-tabs
        v-model="activeTab"
        data-edhr-ncr-review-tabs
        @tab-change="handleTabChange"
      >
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="进行中" name="pending" />
      </el-tabs>

      <div class="edhr-ncr__section">
        <div class="edhr-ncr__section-head">
          <div class="edhr-ncr__section-title">
            {{ activeTab === 'pending' ? '进行中的不合格评审' : '全部不合格评审' }}
          </div>
          <el-tag type="warning">{{ total }} 条</el-tag>
        </div>

        <el-table
          v-loading="listLoading"
          :data="reviews"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无不合格评审"
        >
          <el-table-column label="评审单" min-width="220">
            <template #default="{ row }">
              <div class="edhr-ncr__strong">{{ row.reviewCode || '--' }}</div>
              <div class="edhr-ncr__muted">{{ resolveSourceTypeLabel(row.sourceType) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="生产工单" min-width="210">
            <template #default="{ row }">
              <div>{{ row.workOrderCode || '--' }}</div>
              <div class="edhr-ncr__muted">{{ row.batchCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="不合格原因" min-width="220" prop="nonconformanceReason" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.reviewStatus === REVIEW_STATUS_PENDING_REVIEW ? 'warning' : 'success'">
                {{ resolveReviewStatusLabel(row.reviewStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处置结论" width="120">
            <template #default="{ row }">{{ resolveDispositionLabel(row.disposition) }}</template>
          </el-table-column>
          <el-table-column label="冻结时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.frozenAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                data-edhr-ncr-review-process
                @click.stop="openReviewDialog(row)"
              >
                {{ row.reviewStatus === REVIEW_STATUS_PENDING_REVIEW ? '处理' : '查看' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="loadReviews"
        />
      </div>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新建不合格评审"
      width="720px"
      destroy-on-close
      data-edhr-ncr-create-dialog
      :data-entry-active-order-id="entryActiveOrderId || ''"
      :data-selected-active-order-id="selectedActiveOrderId || ''"
    >
      <el-form label-width="110px" :model="entryForm">
        <el-form-item label="活跃订单" required>
          <el-select
            v-model="selectedActiveOrderId"
            data-edhr-ncr-active-order
            filterable
            :clearable="!entryActiveOrderId"
            :disabled="Boolean(entryActiveOrderId)"
            :loading="activeOrderCandidatesLoading"
            placeholder="请选择活跃订单"
          >
            <el-option
              v-for="order in activeOrderCandidates"
              :key="order.id"
              :label="formatActiveOrderLabel(order)"
              :value="order.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedActiveOrder" label="订单信息">
          <div class="edhr-ncr__source-summary">
            <div><span class="edhr-ncr__label">生产工单</span>{{ selectedActiveOrder.workOrderCode || '--' }}</div>
            <div><span class="edhr-ncr__label">批号</span>{{ selectedActiveOrder.batchCode || '--' }}</div>
            <div><span class="edhr-ncr__label">订单状态</span>{{ selectedActiveOrder.businessStatus || selectedActiveOrder.activeStatus || '--' }}</div>
          </div>
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
      </el-form>
      <template #footer>
        <el-button @click="closeCreateDialog">取消</el-button>
        <el-button
          type="danger"
          :loading="createLoading"
          data-edhr-ncr-create-submit
          @click="submitCreateReview"
        >
          提交不合格评审
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewDialogTitle"
      width="820px"
      destroy-on-close
      data-edhr-ncr-review-dialog
    >
      <template v-if="selectedReview">
        <div class="edhr-ncr__summary">
          <div><span class="edhr-ncr__label">评审单号</span><span>{{ selectedReview.reviewCode || '--' }}</span></div>
          <div><span class="edhr-ncr__label">来源</span><span>{{ resolveSourceTypeLabel(selectedReview.sourceType) }}</span></div>
          <div><span class="edhr-ncr__label">生产工单</span><span>{{ selectedReview.workOrderCode || '--' }}</span></div>
          <div><span class="edhr-ncr__label">批号</span><span>{{ selectedReview.batchCode || '--' }}</span></div>
          <div><span class="edhr-ncr__label">不合格原因</span><span>{{ selectedReview.nonconformanceReason || '--' }}</span></div>
          <div><span class="edhr-ncr__label">冻结时间</span><span>{{ formatDateTime(selectedReview.frozenAt) }}</span></div>
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
          <div class="edhr-ncr__summary edhr-ncr__summary--readonly">
            <div><span class="edhr-ncr__label">评审材料</span><span>{{ resolveReviewMaterialDisplay(selectedReview) }}</span></div>
            <div><span class="edhr-ncr__label">评审意见</span><span>{{ selectedReview.reviewOpinion || '--' }}</span></div>
            <div><span class="edhr-ncr__label">电子签名</span><span data-edhr-ncr-qa-signature>{{ selectedReview.qaSignature || '--' }}</span></div>
            <div><span class="edhr-ncr__label">完成时间</span><span>{{ formatDateTime(selectedReview.closedAt) }}</span></div>
          </div>
        </template>
      </template>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { UploadFile } from '@/components/UploadFile'
import EdhrBatchRecordTabs from '../edhr-batch/EdhrBatchRecordTabs.vue'
import {
  DISPOSITION_CONCESSION_RELEASE,
  DISPOSITION_REWORK,
  DISPOSITION_VOID,
  REVIEW_STATUS_PENDING_REVIEW,
  SOURCE_TYPE_ACTIVE_ORDER,
  SOURCE_TYPE_PQC_RELEASE,
  SOURCE_TYPE_PQC_SUBMISSION,
  createNonconformanceReview,
  disposeNonconformanceReview,
  getNonconformanceReviewActiveOrderList,
  getNonconformanceReviewPage,
  type EdhrNonconformanceReviewActiveOrderRespVO,
  type EdhrNonconformanceReviewDisposition,
  type EdhrNonconformanceReviewMaterial,
  type EdhrNonconformanceReviewMaterialEvent,
  type EdhrNonconformanceReviewPageReqVO,
  type EdhrNonconformanceReviewRespVO
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
const reviews = ref<EdhrNonconformanceReviewRespVO[]>([])
const selectedReview = ref<EdhrNonconformanceReviewRespVO>()
const total = ref(0)
const activeTab = ref<'all' | 'pending'>('all')
const createDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const activeOrderCandidates = ref<EdhrNonconformanceReviewActiveOrderRespVO[]>([])
const activeOrderCandidatesLoading = ref(false)
const selectedActiveOrderId = ref<number>()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10
})

const entryActiveOrderId = computed<number | undefined>(() => {
  const routeId = parsePositiveRouteQueryId(route.query.activeOrderId)
  return routeId ? Number(routeId) : undefined
})
const autoCreate = computed(() => route.query.autoCreate === '1')
const entryForm = reactive({ nonconformanceReason: '' })

const selectedActiveOrder = computed(() =>
  activeOrderCandidates.value.find((order) => order.id === selectedActiveOrderId.value)
)
const reviewDialogTitle = computed(() =>
  selectedReview.value?.reviewStatus === REVIEW_STATUS_PENDING_REVIEW ? '处理不合格评审' : '评审详情'
)
const formatActiveOrderLabel = (order: EdhrNonconformanceReviewActiveOrderRespVO) =>
  (order.workOrderCode || '活跃订单 ' + order.id) + ' · ' + (order.batchCode || '--')

const disposeForm = reactive({
  reviewMaterialUrls: [] as string[],
  reviewMaterialEvents: [] as EdhrNonconformanceReviewMaterialEvent[],
  reviewOpinion: '',
  signaturePassword: ''
})
let materialTrackingEnabled = true
let materialEventSequence = 0

type ReviewMaterialDisplay = { url: string; fileName?: string }

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const formatDateTime = (value?: string | number) => formatEdhrDateTime(value)

const resolveSourceTypeLabel = (sourceType?: string) => {
  if (sourceType === SOURCE_TYPE_ACTIVE_ORDER) return '活跃订单'
  if (sourceType === SOURCE_TYPE_PQC_RELEASE) return 'PQC生产放行'
  if (sourceType === SOURCE_TYPE_PQC_SUBMISSION) return 'PQC提交记录'
  return '未知来源'
}

const resolveReviewStatusLabel = (status?: string) =>
  status === REVIEW_STATUS_PENDING_REVIEW ? '进行中' : '已关闭'

const resolveDispositionLabel = (disposition?: string) => {
  if (disposition === DISPOSITION_CONCESSION_RELEASE) return '让步放行'
  if (disposition === DISPOSITION_REWORK) return '返工'
  if (disposition === DISPOSITION_VOID) return '作废'
  return '未处置'
}

const resolveDispositionNote = (disposition?: string) => {
  if (disposition === DISPOSITION_CONCESSION_RELEASE) return '批次已解冻，继续主流程。'
  if (disposition === DISPOSITION_REWORK) return 'QA已确认返工，返回主流程。'
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
  } catch {
    throw new Error('评审材料清单格式无效，无法继续显示。')
  }
}

const resolveReviewMaterialName = (material: ReviewMaterialDisplay) => {
  const persistedName = material.fileName?.trim()
  if (persistedName) return persistedName.includes('%') ? decodeFileName(persistedName) : persistedName
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

const fillDisposeForm = (review: EdhrNonconformanceReviewRespVO) => {
  materialTrackingEnabled = false
  disposeForm.reviewMaterialUrls = parseReviewMaterialsJson(review).map((material) => material.url)
  disposeForm.reviewMaterialEvents = []
  materialEventSequence = 0
  nextTick(() => {
    materialTrackingEnabled = true
  })
  disposeForm.reviewOpinion = review.reviewOpinion || ''
  disposeForm.signaturePassword = ''
}

const buildReviewQuery = (): EdhrNonconformanceReviewPageReqVO => {
  const query: EdhrNonconformanceReviewPageReqVO = { ...queryParams }
  if (activeTab.value === 'pending') query.reviewStatus = REVIEW_STATUS_PENDING_REVIEW
  return query
}

const loadReviews = async () => {
  listLoading.value = true
  errorText.value = ''
  try {
    const data = await getNonconformanceReviewPage(buildReviewQuery())
    reviews.value = data.list || []
    total.value = data.total || 0
    if (selectedReview.value?.id) {
      const refreshed = reviews.value.find((review) => review.id === selectedReview.value?.id)
      if (refreshed) selectedReview.value = refreshed
    }
  } catch (error) {
    reviews.value = []
    total.value = 0
    errorText.value = resolveErrorMessage(error, '不合格评审列表加载失败。')
  } finally {
    listLoading.value = false
  }
}

const handleTabChange = () => {
  queryParams.pageNo = 1
  void loadReviews()
}

const openCreateDialog = async () => {
  errorText.value = ''
  createDialogVisible.value = true
  activeOrderCandidatesLoading.value = true
  try {
    const candidates = await getNonconformanceReviewActiveOrderList()
    activeOrderCandidates.value = entryActiveOrderId.value
      ? candidates.filter((order) => order.id === entryActiveOrderId.value)
      : candidates
    if (entryActiveOrderId.value && activeOrderCandidates.value.length === 0) {
      throw new Error('当前 PQC 放行记录关联的活跃订单不存在或已失效。')
    }
    selectedActiveOrderId.value = entryActiveOrderId.value || activeOrderCandidates.value[0]?.id
  } catch (error) {
    errorText.value = resolveErrorMessage(error, '活跃订单加载失败。')
  } finally {
    activeOrderCandidatesLoading.value = false
  }
}

const closeCreateDialog = () => {
  createDialogVisible.value = false
  selectedActiveOrderId.value = undefined
  entryForm.nonconformanceReason = ''
}

const openReviewDialog = (review: EdhrNonconformanceReviewRespVO) => {
  selectedReview.value = review
  fillDisposeForm(review)
  reviewDialogVisible.value = true
}

const submitCreateReview = async () => {
  if (!selectedActiveOrderId.value) {
    message.error('请选择活跃订单后再发起不合格评审。')
    return
  }
  const reason = entryForm.nonconformanceReason.trim()
  if (!reason) {
    message.error('不合格原因不能为空。')
    return
  }
  createLoading.value = true
  errorText.value = ''
  try {
    const review = await createNonconformanceReview({
      activeOrderId: selectedActiveOrderId.value,
      nonconformanceReason: reason
    })
    selectedReview.value = review
    resetDisposeForm()
    closeCreateDialog()
    activeTab.value = 'all'
    queryParams.pageNo = 1
    message.success('不合格评审已创建，编号 ' + (review.reviewCode || '--'))
    await loadReviews()
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
    message.success('已' + resolveDispositionLabel(disposition))
    await loadReviews()
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
  () => [route.name, route.query.activeOrderId, route.query.autoCreate] as const,
  ([routeName]) => {
    if (routeName !== 'MesProFeedbackEdhrNonconformanceReview') return
    void loadReviews()
    if (autoCreate.value && entryActiveOrderId.value) {
      void openCreateDialog()
    }
  }
)

onMounted(() => {
  void loadReviews()
  if (autoCreate.value && entryActiveOrderId.value) {
    void openCreateDialog()
  }
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
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.edhr-ncr__header,
.edhr-ncr__section-head,
.edhr-ncr__buttons {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edhr-ncr__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.edhr-ncr__title {
  font-size: 18px;
  font-weight: 700;
  color: #172033;
}

.edhr-ncr__subtitle,
.edhr-ncr__muted {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
}

.edhr-ncr__section-title {
  margin-bottom: 12px;
  font-weight: 700;
  color: #172033;
}

.edhr-ncr__strong {
  font-weight: 600;
  line-height: 1.5;
  color: #172033;
}

.edhr-ncr__source-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  width: 100%;
  font-size: 13px;
}

.edhr-ncr__summary {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #263247;
}

.edhr-ncr__summary--readonly {
  margin-top: 8px;
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

@media (width <= 960px) {
  .edhr-ncr__source-summary {
    grid-template-columns: 1fr;
  }
}
</style>
