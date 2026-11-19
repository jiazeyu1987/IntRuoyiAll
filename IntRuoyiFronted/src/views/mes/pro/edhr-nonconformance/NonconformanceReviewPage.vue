<template>
  <ContentWrap :class="{ 'edhr-record-viewport': !orderDetailVisible && activeTab !== 'review', 'edhr-ncr-review-viewport': !orderDetailVisible && activeTab === 'review' }">
    <ActiveOrderDetailLayout v-if="orderDetailVisible" data-edhr-ncr-active-order-detail>
      <el-button data-edhr-ncr-active-order-detail-back @click="closeOrderDetail">返回</el-button>
      <ActiveOrderSubmissionDetailPanel
        :detail="orderDetail"
        :loading="orderDetailLoading"
        :error="orderDetailError"
        signature-scope-type="NCR_REVIEW"
        :signature-scope-id="orderDetailReview?.id"
        read-only
        @retry="retryOrderDetail"
      />
    </ActiveOrderDetailLayout>
    <div v-show="!orderDetailVisible" class="edhr-ncr" data-edhr-ncr-page>


      <el-alert v-if="errorText" :title="errorText" type="error" :closable="false" show-icon />

      <el-tabs
        v-model="activeTab"
        data-edhr-ncr-review-tabs
        @tab-change="handleTabChange"
        @tab-remove="closeReviewTab"
      >
        <el-tab-pane label="全部" name="all" :disabled="disposeLoading || assigningReviewer" />
        <el-tab-pane label="进行中" name="pending" :disabled="disposeLoading || assigningReviewer" />
        <el-tab-pane v-if="reviewTabOpen" :label="reviewTabTitle" name="review" :closable="!disposeLoading && !assigningReviewer">
          <NonconformanceReviewWorkspace
            v-if="selectedReview"
            :review="selectedReview"
            :source-label="resolveSourceTypeLabel(selectedReview.sourceType)"
            :status-label="resolveReviewStatusLabel(selectedReview.reviewStatus)"
            :frozen-at="formatDateTime(selectedReview.frozenAt)"
            :closed-at="formatDateTime(selectedReview.closedAt)"
            :disposition-label="resolveDispositionLabel(selectedReview.disposition)"
            :disposition-note="resolveDispositionNote(selectedReview.disposition)"
            :can-dispose="canDisposeSelectedReview"
            :read-only="selectedReviewReadOnly"
            :can-assign="selectedReview.reviewStatus === REVIEW_STATUS_PENDING_REVIEW && !selectedReview.qaReviewerUserId && canCreateReview && !selectedReviewReadOnly"
            v-model:reviewer-id="unassignedReviewerId"
            :assigning-reviewer="assigningReviewer"
            :submitting="disposeLoading"
            :materials="reviewWorkspaceMaterials"
            :attempts="materialUploadAttempts"
            :uploading-count="materialUploadsPending"
            v-model:opinion="disposeForm.reviewOpinion"
            v-model:signature="disposeForm.signaturePassword"
            :errors="reviewValidationErrors"
            @back="closeReviewTab"
            @assign="assignSelectedReviewer"
            @dispose="handleDispose"
            @upload="handleMaterialUpload"
            @remove-material="removeReviewMaterial"
            @dismiss-attempt="dismissMaterialUploadAttempt"
          />
        </el-tab-pane>
      </el-tabs>

      <UnifiedListTemplate
        v-show="activeTab !== 'review'"
        single-line-toolbar
        :table-key="REVIEW_TABLE_KEY"
        :query-model="queryParams"
        :filter-definitions="reviewFilterDefinitions"
        :quick-filter-state="reviewQuickFilter.state"
        :selected-filter-definition="reviewQuickFilter.selectedDefinition.value"
        :operator-options="reviewQuickFilter.operatorOptions.value"
        :columns="reviewColumns"
        :column-saving="columnSaving"
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @update:quick-filter-state="reviewQuickFilter.updateState"
        @quick-filter-query="reviewQuickFilter.applyQuickFilter"
        @column-change="saveColumnConfig"
        @pagination="loadReviews"
        server-filter
        :server-filter-query="loadReviews"
        :column-filter-definitions="listColumnFilterDefinitions0"
      >
        <template #actions>
          <el-button v-if="canCreateReview" type="primary" plain data-edhr-ncr-open-create @click="openCreateDialog">
            <Icon icon="ep:plus" class="mr-5px" /> 新建
          </el-button>
          <el-button data-edhr-ncr-refresh @click="loadReviews">刷新</el-button>
        </template>
        <template #table="{ resolveRows }">
          <el-table height="100%"
            v-loading="listLoading"
            :data="resolveRows(reviews)"
            border
            data-user-table-column-explicit
            :data-user-table-key="REVIEW_TABLE_KEY"
            @header-dragend="handleHeaderDragend"
            stripe
            :show-overflow-tooltip="true"
            empty-text="暂无不合格评审"
          >
            <el-table-column v-if="isColumnVisible('reviewCode')" label="评审单" prop="reviewCode" :width="getColumnWidth('reviewCode')" :min-width="220">
              <template #default="{ row }">
                <div class="edhr-ncr__strong">{{ row.reviewCode || '--' }}</div>
                <div class="edhr-ncr__muted">{{ resolveSourceTypeLabel(row.sourceType) }}</div>
              </template>
            </el-table-column>
            <el-table-column v-if="isColumnVisible('workOrderCode')" label="生产工单" prop="workOrderCode" :width="getColumnWidth('workOrderCode')" :min-width="210">
              <template #default="{ row }">
                <div><WorkOrderCodeLink :code="row.workOrderCode" :work-order-id="row.workOrderId" /></div>
                <div class="edhr-ncr__muted"><ProductionBatchCodeLink :code="row.batchCode" /></div>
              </template>
            </el-table-column>
            <el-table-column v-if="isColumnVisible('nonconformanceReason')" label="不合格原因" prop="nonconformanceReason" :width="getColumnWidth('nonconformanceReason')" :min-width="220" />
            <el-table-column v-if="isColumnVisible('reviewStatus')" label="状态" prop="reviewStatus" :width="getColumnWidth('reviewStatus')" :min-width="110">
              <template #default="{ row }">
                <el-tag :type="row.reviewStatus === REVIEW_STATUS_PENDING_REVIEW ? 'warning' : 'success'">
                  {{ resolveReviewStatusLabel(row.reviewStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="isColumnVisible('disposition')" label="处置结论" prop="disposition" :width="getColumnWidth('disposition')" :min-width="120">
              <template #default="{ row }">{{ resolveDispositionLabel(row.disposition) }}</template>
            </el-table-column>
            <el-table-column v-if="isColumnVisible('frozenAt')" label="冻结时间" prop="frozenAt" :width="getColumnWidth('frozenAt')" :min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.frozenAt) }}</template>
            </el-table-column>
            <el-table-column v-if="isColumnVisible('actions')" label="操作" prop="actions" :width="getColumnWidth('actions')" :min-width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" data-edhr-ncr-review-detail @click.stop="openActiveOrderDetail(row)">
                  详情
                </el-button>
                <el-button
                  v-if="canDisposeReview(row)"
                  link
                  type="primary"
                  data-edhr-ncr-review-process
                  @click.stop="openReviewTab(row)"
                >
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>

        </template>
      </UnifiedListTemplate>
    </div>

    <el-dialog
      v-model="createDialogVisible"
      title="新建不合格评审"
      width="720px"
      class="edhr-ncr-create-dialog"
      top="8vh"
      destroy-on-close
      data-edhr-ncr-create-dialog
      :data-entry-active-order-id="entryActiveOrderId || ''"
      :data-selected-active-order-id="selectedActiveOrderId || ''"
    >
      <el-form label-position="top" :model="entryForm">
        <el-form-item v-if="!entryActiveOrderId" label="活跃订单" required>
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
        <section v-if="selectedActiveOrder" class="edhr-ncr-create-dialog__order" aria-label="订单信息">
          <dl>
            <div><dt>生产工单</dt><dd><WorkOrderCodeLink :code="selectedActiveOrder.workOrderCode" :work-order-id="selectedActiveOrder.workOrderId" /></dd></div>
            <div><dt>批号</dt><dd><ProductionBatchCodeLink :code="selectedActiveOrder.batchCode" /></dd></div>
          </dl>
        </section>
        <el-form-item label="QA评审责任人" required>
          <QaReviewerSelect v-model="entryForm.qaReviewerUserId" placeholder="请选择具备QA处置权限的责任人" data-edhr-ncr-create-reviewer />
        </el-form-item>
        <el-form-item label="不合格原因" required>
          <el-input
            v-model="entryForm.nonconformanceReason"
            data-edhr-ncr-create-reason
            type="textarea"
            :rows="4"
            placeholder="请描述不合格现象、发生环节及相关情况"
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


  </ContentWrap>
</template>

<script setup lang="ts">
import ProductionBatchCodeLink from '@/components/ProductionBatchCodeLink.vue'
import WorkOrderCodeLink from '@/components/WorkOrderCodeLink.vue'
import { type ListColumnFilterDefinition } from '@/hooks/web/useListColumnFilter'

import '@/views/mes/pro/edhr-batch/record-viewport.css'
import QaReviewerSelect from '@/views/mes/pro/edhr-nonconformance/QaReviewerSelect.vue'
import ActiveOrderDetailLayout from '../processpool/components/ActiveOrderDetailLayout.vue'
import ActiveOrderSubmissionDetailPanel from '../processpool/components/ActiveOrderSubmissionDetailPanel.vue'
import { useUserStore } from '@/store/modules/user'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import NonconformanceReviewWorkspace from './NonconformanceReviewWorkspace.vue'
import type { ReviewUploadAttempt } from './reviewWorkspace'
import { useUserTableColumns } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const approvalInvalidation = useApprovalCenterInvalidationStore()
import { parseExactIntegerJson } from '@/utils/exactIntegerJson'
import {
  DISPOSITION_CONCESSION_RELEASE,
  DISPOSITION_REWORK,
  DISPOSITION_VOID,
  REVIEW_STATUS_PENDING_REVIEW,
  SOURCE_TYPE_ACTIVE_ORDER,
  SOURCE_TYPE_DEVIATION,
  SOURCE_TYPE_PQC_RELEASE,
  SOURCE_TYPE_PQC_SUBMISSION,
  createNonconformanceReview,
  assignNonconformanceReviewer,
  disposeNonconformanceReview,
  uploadNonconformanceReviewMaterial,
  getNonconformanceReviewActiveOrderList,
  getNonconformanceReviewActiveOrderDetail,
  getNonconformanceReviewPage,
  getNonconformanceReview,
  type EdhrNonconformanceReviewActiveOrderRespVO,
  type EdhrNonconformanceReviewDisposition,
  type EdhrNonconformanceReviewMaterial,
  type EdhrNonconformanceReviewMaterialEvent,
  type EdhrNonconformanceReviewPageReqVO,
  type EdhrNonconformanceReviewRespVO
} from '@/api/mes/pro/edhr/nonconformanceReview'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { hasPermission } from '@/directives/permission/hasPermi'
import { resolveUrlPathFileName } from '@/utils/fileName'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import { handoffNavigationContext } from '@/api/mes/pro/handoff'

defineOptions({ name: 'MesProFeedbackEdhrNonconformanceReview' })

const route = useRoute()
const message = useMessage()
const selectedReviewReadOnly = ref(false)
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetailError = ref('')
const orderDetail = ref<Awaited<ReturnType<typeof getNonconformanceReviewActiveOrderDetail>>>()
const orderDetailReview = ref<EdhrNonconformanceReviewRespVO>()
let orderDetailSequence = 0

const closeOrderDetail = () => {
  orderDetailSequence++
  orderDetailVisible.value = false
  orderDetailLoading.value = false
  orderDetail.value = undefined
  orderDetailReview.value = undefined
  orderDetailError.value = ''
}

const openActiveOrderDetail = async (review: EdhrNonconformanceReviewRespVO) => {
  closeOrderDetail()
  const sequence = orderDetailSequence
  const sessionKey = approvalInvalidation.syncSession().key
  const current = () => sequence === orderDetailSequence && sessionKey === approvalInvalidation.syncSession().key
  orderDetailReview.value = review
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  try {
    const result = await getNonconformanceReviewActiveOrderDetail(review.id, {
      ignoreErrorMessage: true, isRequestContextCurrent: current
    })
    if (!current()) return
    if (!result) throw new Error('关联活跃工单详情为空。')
    orderDetail.value = result
  } catch (error) {
    if (current()) orderDetailError.value = resolveErrorMessage(error, '活跃工单详情加载失败。')
  } finally {
    if (current()) orderDetailLoading.value = false
  }
}

const retryOrderDetail = () => {
  if (orderDetailReview.value) void openActiveOrderDetail(orderDetailReview.value)
}

const listLoading = ref(false)
const createLoading = ref(false)
const disposeLoading = ref(false)
const errorText = ref('')
const reviews = ref<EdhrNonconformanceReviewRespVO[]>([])
const selectedReview = ref<EdhrNonconformanceReviewRespVO>()
const total = ref(0)
const activeTab = ref<'all' | 'pending' | 'review'>('all')
const listTab = ref<'all' | 'pending'>('all')
const createDialogVisible = ref(false)
const reviewTabOpen = ref(false)
const activeOrderCandidates = ref<EdhrNonconformanceReviewActiveOrderRespVO[]>([])
const activeOrderCandidatesLoading = ref(false)
const selectedActiveOrderId = ref<number>()

const queryParams = reactive({
  listFilters: undefined as string | undefined,
  pageNo: 1,
  pageSize: 10
})

const REVIEW_TABLE_KEY = 'mes-pro-edhr-nonconformance-review'
const {
  columns: reviewColumns,
  saving: columnSaving,
  isColumnVisible,
  getColumnWidth,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns(REVIEW_TABLE_KEY, [
  { key: 'reviewCode', label: '评审单', minWidth: 220 },
  { key: 'workOrderCode', label: '生产工单', minWidth: 210 },
  { key: 'nonconformanceReason', label: '不合格原因', minWidth: 220 },
  { key: 'reviewStatus', label: '状态', minWidth: 110 },
  { key: 'disposition', label: '处置结论', minWidth: 120 },
  { key: 'frozenAt', label: '冻结时间', minWidth: 170 },
  { key: 'actions', label: '操作', minWidth: 110, hideable: false },
])
const reviewFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'reviewCode', label: '评审单号', type: 'text', queryParamKey: 'reviewCode', operators: ['contains'] },
  { key: 'workOrderCode', label: '生产工单', type: 'text', queryParamKey: 'workOrderCode', operators: ['contains'] },
  { key: 'batchCode', label: '批次号', type: 'text', queryParamKey: 'batchCode', operators: ['contains'] }
]
const reviewQuickFilter = useTableQuickFilter(
  REVIEW_TABLE_KEY, reviewFilterDefinitions, queryParams, () => loadReviews()
)

const entryActiveOrderId = computed<number | undefined>(() => {
  const routeId = parsePositiveRouteQueryId(route.query.activeOrderId)
  return routeId ? Number(routeId) : undefined
})
const autoCreate = computed(() => route.query.autoCreate === '1')
const canCreateReview = computed(() => hasPermission(['mes:pro-edhr-nonconformance-review:create']))
const entryForm = reactive({ nonconformanceReason: '', qaReviewerUserId: undefined as string | number | undefined })
const unassignedReviewerId = ref<string | number>()
const assigningReviewer = ref(false)
const isAssignedQaReviewer = (review?: EdhrNonconformanceReviewRespVO) => {
  const userId = useUserStore().getUser.id
  return userId != null && review?.qaReviewerUserId != null &&
    String(review.qaReviewerUserId) === String(userId)
}
const canDisposeReview = (review?: EdhrNonconformanceReviewRespVO) =>
  isAssignedQaReviewer(review) && review?.reviewStatus === REVIEW_STATUS_PENDING_REVIEW &&
  hasPermission(['mes:pro-edhr-nonconformance-review:dispose'])
const canDisposeSelectedReview = computed(() => canDisposeReview(selectedReview.value))

const selectedActiveOrder = computed(() =>
  activeOrderCandidates.value.find((order) => order.id === selectedActiveOrderId.value)
)
const reviewTabTitle = computed(() =>
  canDisposeSelectedReview.value && !selectedReviewReadOnly.value ? '处理不合格评审' : '评审详情'
)
const formatActiveOrderLabel = (order: EdhrNonconformanceReviewActiveOrderRespVO) =>
  (order.workOrderCode || '活跃订单 ' + order.id) + ' · ' + (order.batchCode || '--')

const disposeForm = reactive({
  reviewMaterialUrls: [] as string[],
  reviewMaterialNames: {} as Record<string, string>,
  reviewMaterialIds: {} as Record<string, string | number>,
  reviewMaterialEvents: [] as EdhrNonconformanceReviewMaterialEvent[],
  reviewOpinion: '',
  signaturePassword: ''
})
let materialTrackingEnabled = true
let materialEventSequence = 0
let materialContextGeneration = 0
let reviewContextGeneration = 0
let listRequestSequence = 0
let createContextGeneration = 0
const invalidateCreateContext = () => {
  createContextGeneration++
  listRequestSequence++
  listLoading.value = false
  createLoading.value = false
  activeOrderCandidatesLoading.value = false
}
const invalidateReviewContext = () => {
  selectedReviewReadOnly.value = false
  assigningReviewer.value = false
  invalidateCreateContext()
  reviewContextGeneration++
  materialContextGeneration++
  listRequestSequence++
  disposeLoading.value = false
  listLoading.value = false
  materialUploadsPending.value = 0
  materialUploadAttempts.value = []
  materialSelectionError.value = ''
  reviewValidationRequested.value = false
}
const materialUploadsPending = ref(0)

type ReviewMaterialDisplay = { url: string; fileName?: string; fileId?: string | number }

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
  if (sourceType === SOURCE_TYPE_DEVIATION) return '偏差'
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

const resolveFileName = (url: string) => {
  return resolveUrlPathFileName(url)
}

const parseReviewMaterialsJson = (review?: EdhrNonconformanceReviewRespVO): ReviewMaterialDisplay[] => {
  if (!review?.reviewMaterialsJson) {
    return review?.reviewMaterialUrl
      ? review.reviewMaterialUrl.split(',').filter(Boolean).map((url) => ({ url }))
      : []
  }
  try {
    const payload = parseExactIntegerJson(review.reviewMaterialsJson)
    const activeMaterials = Array.isArray(payload?.activeMaterials) ? payload.activeMaterials : []
    return activeMaterials
      .map((material) => ({
        url: material?.url,
        fileId: material?.fileId,
        fileName: typeof material?.fileName === 'string' ? material.fileName : undefined
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
  const persistedName = material.fileName
  if (persistedName?.trim()) return persistedName
  return resolveFileName(material.url)
}



const buildReviewMaterials = (): EdhrNonconformanceReviewMaterial[] =>
  disposeForm.reviewMaterialUrls.map((url, index) => {
    const fileName = disposeForm.reviewMaterialNames[url]
    const fileId = disposeForm.reviewMaterialIds[url]
    if (!fileName?.trim() || !fileId) throw new Error('评审材料缺少正式文件身份或原始文件名，请重新上传。')
    return { fileId, url, fileName, sortNo: index + 1 }
  })

const materialUploadAttempts = ref<ReviewUploadAttempt[]>([])
let materialUploadSequence = 0
const materialSelectionError = ref('')
const reviewValidationRequested = ref(false)
const reviewValidationErrors = computed(() => ({
  materials: materialSelectionError.value,
  opinion: reviewValidationRequested.value && !disposeForm.reviewOpinion.trim() ? '请输入评审意见。' : '',
  signature: reviewValidationRequested.value && !disposeForm.signaturePassword.trim() ? '请输入本人签名凭据。' : ''
}))
const reviewWorkspaceMaterials = computed(() => {
  const materials = canDisposeSelectedReview.value && !selectedReviewReadOnly.value
    ? disposeForm.reviewMaterialUrls.map((url) => ({ url, fileName: disposeForm.reviewMaterialNames[url] }))
    : parseReviewMaterialsJson(selectedReview.value)
  return materials.map((material) => ({ url: material.url, fileName: resolveReviewMaterialName(material) }))
})
const dismissMaterialUploadAttempt = (key: number) => {
  if (disposeLoading.value) return
  materialUploadAttempts.value = materialUploadAttempts.value.filter((attempt) => attempt.key !== key || attempt.status === 'uploading')
}
const handleMaterialUpload = async (files: File[]) => {
  const reviewId = selectedReview.value?.id
  if (!files.length || !reviewId || !reviewTabOpen.value || !canDisposeSelectedReview.value || disposeLoading.value || selectedReviewReadOnly.value) return
  materialSelectionError.value = ''
  if (disposeForm.reviewMaterialUrls.length + materialUploadsPending.value + files.length > 5) {
    materialSelectionError.value = '最多上传5份评审材料，请减少本次选择的文件。'
    message.error(materialSelectionError.value)
    return
  }
  const generation = materialContextGeneration
  const isCurrent = () => generation === materialContextGeneration
    && selectedReview.value?.id === reviewId && reviewTabOpen.value
  const uploads = files.map((file) => ({ file, key: ++materialUploadSequence }))
  materialUploadAttempts.value.push(...uploads.map(({ file, key }) => ({ key, fileName: file.name, status: 'uploading' as const })))
  materialUploadsPending.value += files.length
  await Promise.all(uploads.map(async ({ file, key }) => {
    try {
      if (file.size === 0) throw new Error('文件为空，请选择有内容的评审材料。')
      const material = await uploadNonconformanceReviewMaterial(reviewId, file, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
      if (!isCurrent()) return
      if (!material.fileId || !material.url || !material.fileName?.trim()) {
        throw new Error('上传结果缺少正式文件身份或原始名称，请重新上传。')
      }
      if (disposeForm.reviewMaterialUrls.includes(material.url)) {
        throw new Error('上传结果重复，无法确认材料独立性。')
      }
      disposeForm.reviewMaterialIds[material.url] = material.fileId
      disposeForm.reviewMaterialNames[material.url] = material.fileName
      disposeForm.reviewMaterialUrls.push(material.url)
      materialUploadAttempts.value = materialUploadAttempts.value.filter((attempt) => attempt.key !== key)
    } catch (error) {
      if (isCurrent()) {
        const reason = resolveErrorMessage(error, '上传失败，请重新选择文件。')
        const attempt = materialUploadAttempts.value.find((item) => item.key === key)
        if (attempt) { attempt.status = 'failed'; attempt.error = reason }
        message.error(`${file.name}：${reason}`)
      }
    } finally {
      if (isCurrent()) materialUploadsPending.value--
    }
  }))
}

const removeReviewMaterial = (url: string) => {
  if (disposeLoading.value || selectedReviewReadOnly.value) return
  disposeForm.reviewMaterialUrls = disposeForm.reviewMaterialUrls.filter((value) => value !== url)
  delete disposeForm.reviewMaterialIds[url]
  delete disposeForm.reviewMaterialNames[url]
}

const fillDisposeForm = (review: EdhrNonconformanceReviewRespVO) => {
  materialUploadAttempts.value = []
  materialSelectionError.value = ''
  reviewValidationRequested.value = false
  materialContextGeneration++
  materialUploadsPending.value = 0
  const materials = parseReviewMaterialsJson(review)
  materialTrackingEnabled = false
  disposeForm.reviewMaterialIds = Object.fromEntries(
    materials.flatMap((material) => material.fileId ? [[material.url, material.fileId]] : [])
  )
  disposeForm.reviewMaterialNames = Object.fromEntries(
    materials.flatMap((material) => (material.fileName ? [[material.url, material.fileName]] : []))
  )
  disposeForm.reviewMaterialUrls = materials.map((material) => material.url)
  disposeForm.reviewMaterialEvents = []
  materialEventSequence = 0
  const generation = materialContextGeneration
  nextTick(() => {
    if (generation === materialContextGeneration) materialTrackingEnabled = true
  })
  disposeForm.reviewOpinion = review.reviewOpinion || ''
  disposeForm.signaturePassword = ''
}

const buildReviewQuery = (): EdhrNonconformanceReviewPageReqVO => {
  const query: EdhrNonconformanceReviewPageReqVO = { ...queryParams }
  if (listTab.value === 'pending') query.reviewStatus = REVIEW_STATUS_PENDING_REVIEW
  return query
}

const loadReviews = async () => {
  const generation = reviewContextGeneration
  const request = ++listRequestSequence
  const isCurrent = () => generation === reviewContextGeneration && request === listRequestSequence
  listLoading.value = true
  errorText.value = ''
  try {
    const data = await getNonconformanceReviewPage(buildReviewQuery(), { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return false
    reviews.value = data.list || []
    total.value = data.total || 0
    if (selectedReview.value?.id && route.query.reviewId === undefined) {
      const refreshed = reviews.value.find((review) => review.id === selectedReview.value?.id)
      if (refreshed) selectedReview.value = refreshed
    }
    return true
  } catch (error) {
    if (!isCurrent()) return false
    reviews.value = []
    total.value = 0
    errorText.value = resolveErrorMessage(error, '不合格评审列表加载失败。')
    throw error
  } finally {
    if (isCurrent()) listLoading.value = false
  }
}

const handleTabChange = () => {
  if (activeTab.value === 'review') return
  listTab.value = activeTab.value
  queryParams.pageNo = 1
  void loadReviews()
}

const openCreateDialog = async () => {
  if (!canCreateReview.value) {
    errorText.value = '没有创建不合格评审的权限。'
    return
  }
  reviewTabOpen.value = false
  selectedReview.value = undefined
  invalidateReviewContext()
  invalidateCreateContext()
  const generation = createContextGeneration
  const reviewGeneration = reviewContextGeneration
  const isCurrent = () => generation === createContextGeneration &&
    reviewGeneration === reviewContextGeneration && createDialogVisible.value
  errorText.value = ''
  createDialogVisible.value = true
  activeOrderCandidatesLoading.value = true
  try {
    const candidates = await getNonconformanceReviewActiveOrderList({ ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    activeOrderCandidates.value = entryActiveOrderId.value
      ? candidates.filter((order) => order.id === entryActiveOrderId.value)
      : candidates
    if (entryActiveOrderId.value && activeOrderCandidates.value.length === 0) {
      throw new Error('当前 PQC 放行记录关联的活跃订单不存在或已失效。')
    }
    selectedActiveOrderId.value = entryActiveOrderId.value || activeOrderCandidates.value[0]?.id
  } catch (error) {
    if (!isCurrent()) return
    errorText.value = resolveErrorMessage(error, '活跃订单加载失败。')
  } finally {
    if (isCurrent()) activeOrderCandidatesLoading.value = false
  }
}

const closeCreateDialog = () => {
  createDialogVisible.value = false
  selectedActiveOrderId.value = undefined
  entryForm.nonconformanceReason = ''
  entryForm.qaReviewerUserId = undefined
}

const closeReviewTab = () => {
  if (disposeLoading.value || assigningReviewer.value) return
  reviewTabOpen.value = false
}

const openReviewTab = (review: EdhrNonconformanceReviewRespVO) => {
  if (disposeLoading.value || assigningReviewer.value) return
  invalidateReviewContext()
  errorText.value = ''
  selectedReview.value = review
  unassignedReviewerId.value = undefined
  fillDisposeForm(review)
  reviewTabOpen.value = true
  activeTab.value = 'review'
}

const submitCreateReview = async () => {
  if (createLoading.value || !createDialogVisible.value) return
  if (!selectedActiveOrderId.value) {
    message.error('请选择活跃订单后再发起不合格评审。')
    return
  }
  if (!entryForm.qaReviewerUserId) {
    message.error('请选择QA评审责任人。')
    return
  }
  const reason = entryForm.nonconformanceReason.trim()
  if (!reason) {
    message.error('不合格原因不能为空。')
    return
  }
  const generation = reviewContextGeneration
  const createGeneration = createContextGeneration
  const isCurrent = () => generation === reviewContextGeneration &&
    createGeneration === createContextGeneration && createDialogVisible.value
  createLoading.value = true
  errorText.value = ''
  const approvalEvent = approvalInvalidation.captureHandoffSourceSuccess('ACTIVE_ORDER', selectedActiveOrderId.value)
  let writeSucceeded = false
  try {
    const review = await createNonconformanceReview({
      activeOrderId: selectedActiveOrderId.value,
      qaReviewerUserId: entryForm.qaReviewerUserId,
      nonconformanceReason: reason
    }, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent , isSuccessReceiptContextCurrent: (() => { const receiptSessionKey = approvalInvalidation.syncSession().key; return () => approvalInvalidation.isCurrent(receiptSessionKey) })()})
    writeSucceeded = true
    approvalInvalidation.publishEdhrSuccess(approvalEvent)
    if (!isCurrent() || !approvalInvalidation.isCurrent(approvalEvent.session.key)) return
    closeCreateDialog()
    activeTab.value = 'all'
    queryParams.pageNo = 1
    message.success('不合格评审已创建，编号 ' + (review.reviewCode || '--'))
    try { await loadReviews() } catch (error) {
      if (approvalInvalidation.isCurrent(approvalEvent.session.key)) message.warning(`评审已创建，列表刷新失败：${resolveErrorMessage(error, '请刷新核对。')}`)
    }
  } catch (error) {
    if (writeSucceeded ? generation !== reviewContextGeneration : !isCurrent()) return
    errorText.value = writeSucceeded ? `评审已创建，刷新失败：${resolveErrorMessage(error, '请刷新核对。')}` : resolveErrorMessage(error, '不合格评审创建失败。')
    message.error(errorText.value)
  } finally {
    if (isCurrent()) createLoading.value = false
  }
}

const assignSelectedReviewer = async () => {
  const review = selectedReview.value
  const reviewer = unassignedReviewerId.value
  if (!review || !review.activeOrderId || !reviewer || assigningReviewer.value) return
  const generation = reviewContextGeneration
  const sessionKey = approvalInvalidation.syncSession().key
  const isCurrent = () => generation === reviewContextGeneration && reviewTabOpen.value && approvalInvalidation.isCurrent(sessionKey)
  const event = approvalInvalidation.captureHandoffSourceSuccess('ACTIVE_ORDER', review.activeOrderId)
  assigningReviewer.value = true
  try {
    const receipt = await assignNonconformanceReviewer(review.id, reviewer, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent, isSuccessReceiptContextCurrent: () => approvalInvalidation.isCurrent(sessionKey) })
    approvalInvalidation.publishEdhrSuccess(event)
    if (!isCurrent()) return
    selectedReview.value = receipt
    message.success('QA评审责任已指派，已生成评审待办。')
    await loadReviews()
  } catch (error) {
    if (isCurrent()) message.error(resolveErrorMessage(error, 'QA指派失败。'))
  } finally {
    if (isCurrent()) assigningReviewer.value = false
  }
}

const handleDispose = async (disposition: EdhrNonconformanceReviewDisposition) => {
  if (disposeLoading.value || !reviewTabOpen.value) return
  if (!canDisposeSelectedReview.value || selectedReviewReadOnly.value) {
    message.error('原轮次交接只允许查看，不能再次处置。')
    return
  }
  const reviewId = selectedReview.value?.id
  const generation = reviewContextGeneration
  const isCurrent = () =>
    generation === reviewContextGeneration &&
    reviewTabOpen.value && selectedReview.value?.id === reviewId
  if (materialUploadsPending.value) {
    message.error('请等待材料上传完成后再处置。')
    return
  }
  if (!reviewId) {
    message.error('请选择待处置评审单。')
    return
  }
  reviewValidationRequested.value = true
  if (
    !disposeForm.reviewOpinion.trim() ||
    !disposeForm.signaturePassword.trim()
  ) {
    message.error('评审意见和电子签名凭据（PIN 码或密码）均不能为空。')
    return
  }
  disposeLoading.value = true
  errorText.value = ''
  const approvalEvent = approvalInvalidation.captureHandoffSourceSuccess('NONCONFORMANCE_REVIEW', reviewId)
  let writeSucceeded = false
  try {
    const review = await disposeNonconformanceReview({
      id: reviewId,
      disposition,
      reviewMaterialUrl: disposeForm.reviewMaterialUrls.join(','),
      reviewMaterials: buildReviewMaterials(),
      reviewMaterialEvents: disposeForm.reviewMaterialEvents,
      reviewOpinion: disposeForm.reviewOpinion.trim(),
      signaturePassword: disposeForm.signaturePassword.trim()
    }, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent , isSuccessReceiptContextCurrent: (() => { const receiptSessionKey = approvalInvalidation.syncSession().key; return () => approvalInvalidation.isCurrent(receiptSessionKey) })()})
    writeSucceeded = true
    approvalInvalidation.publishEdhrSuccess(approvalEvent)
    if (!isCurrent() || !approvalInvalidation.isCurrent(approvalEvent.session.key)) return
    selectedReview.value = review
    message.success('已' + resolveDispositionLabel(disposition))
    await loadReviews()
  } catch (error) {
    if (!isCurrent()) return
    errorText.value = writeSucceeded ? `处置已成功，刷新失败：${resolveErrorMessage(error, '请刷新核对。')}` : resolveErrorMessage(error, '不合格评审处置失败。')
    message.error(errorText.value)
  } finally {
    if (isCurrent()) disposeLoading.value = false
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
      const fileName = disposeForm.reviewMaterialNames[url]
      if (!fileName) throw new Error('评审材料缺少原始文件名，请重新打开材料或重新上传。')
      disposeForm.reviewMaterialEvents.push({
        fileId: disposeForm.reviewMaterialIds[url],
        action: 'UPLOAD',
        url,
        fileName,
        sequence: ++materialEventSequence
      })
    })
    removals.forEach((url) => {
      const fileName = disposeForm.reviewMaterialNames[url]
      if (!fileName) throw new Error('评审材料缺少原始文件名，请重新打开材料或重新上传。')
      if (!disposeForm.reviewMaterialIds[url]) return
      disposeForm.reviewMaterialEvents.push({
        fileId: disposeForm.reviewMaterialIds[url],
        action: 'DELETE',
        url,
        fileName,
        sequence: ++materialEventSequence
      })
    })
  },
  { flush: 'sync' }
)

watch(createDialogVisible, (visible) => {
  if (!visible) invalidateCreateContext()
}, { flush: 'sync' })

watch(reviewTabOpen, (visible) => {
  if (!visible) {
    if (activeTab.value === 'review') activeTab.value = listTab.value
    invalidateReviewContext()
  }
}, { flush: 'sync' })

watch(() => approvalInvalidation.syncSession().key, () => {
  closeOrderDetail()
  invalidateReviewContext()
  createDialogVisible.value = false
  reviewTabOpen.value = false
}, { flush: 'sync' })

watch(() => route.fullPath, () => {
  closeOrderDetail()
  invalidateReviewContext()
  createDialogVisible.value = false
  reviewTabOpen.value = false
}, { flush: 'sync' })

onBeforeUnmount(() => {
  closeOrderDetail()
  invalidateReviewContext()
  invalidateCreateContext()
})

const requireReviewIdentity = (value: unknown): string => {
  if (typeof value === 'string' && /^[1-9]\d*$/.test(value)) return value
  if (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) return String(value)
  throw new Error('关联不合格评审身份无效，请从正式偏差详情重新进入。')
}

const loadReviewEntry = async () => {
  reviewTabOpen.value = false
  closeCreateDialog()
  invalidateReviewContext()
  selectedReview.value = undefined
  errorText.value = ''
  if (route.name !== 'MesProFeedbackEdhrNonconformanceReview') return
  const query = { ...route.query }
  const handoffEntry = ['handoffTaskId', 'handoffType', 'roundId', 'handoffReadOnly']
    .some(key => query[key] !== undefined)
  const exactEntry = query.reviewId !== undefined || query.from === 'deviation' || query.from === 'signature-governance' ||
    query.deviationId !== undefined || handoffEntry
  if (!exactEntry) {
    void loadReviews()
    if (autoCreate.value && entryActiveOrderId.value) void openCreateDialog()
    return
  }
  const generation = reviewContextGeneration
  const isCurrent = () => generation === reviewContextGeneration
  reviews.value = []
  total.value = 0
  listLoading.value = true
  try {
    const reviewId = requireReviewIdentity(query.reviewId)
    const fromSignatureGovernance = query.from === 'signature-governance'
    if (fromSignatureGovernance && Object.keys(query).some(key => key !== 'reviewId' && key !== 'from')) {
      throw new Error('签名治理评审详情入口不能混入其他业务参数。')
    }
    let readOnly = fromSignatureGovernance
    let handoffActiveOrderId: string | undefined
    if (handoffEntry) {
      const keys = ['reviewId', 'activeOrderId', 'handoffTaskId', 'roundId', 'handoffType']
      if (Object.keys(query).some(key => !keys.includes(key) && key !== 'handoffReadOnly') ||
        (query.handoffReadOnly !== undefined && query.handoffReadOnly !== '1')) {
        throw new Error('交接评审入口参数不完整或混入其他业务入口。')
      }
      const taskId = requireReviewIdentity(query.handoffTaskId)
      const roundId = requireReviewIdentity(query.roundId)
      handoffActiveOrderId = requireReviewIdentity(query.activeOrderId)
      if (query.handoffType !== 'QA_REVIEW' && query.handoffType !== 'QA_DECISION_HANDOFF') {
        throw new Error('交接类型不属于不合格评审。')
      }
      const context = await handoffNavigationContext(taskId)
      if (!isCurrent()) return
      const task = context?.task
      if (!task || requireReviewIdentity(task.id) !== taskId ||
        requireReviewIdentity(task.activeOrderId) !== handoffActiveOrderId ||
        requireReviewIdentity(task.roundId) !== roundId ||
        requireReviewIdentity(task.sourceId) !== reviewId || task.taskType !== query.handoffType) {
        throw new Error('交接与正式评审任务不一致，请重新进入。')
      }
      const url = new URL(task.actionUrl, 'http://handoff.invalid')
      const urlKeys: string[] = []
      url.searchParams.forEach((_value, key) => urlKeys.push(key))
      if (url.origin !== 'http://handoff.invalid' || url.hash ||
        url.pathname !== '/mes/pro/feedback/edhr-nonconformance-review' ||
        urlKeys.length !== keys.length || new Set(urlKeys).size !== keys.length ||
        keys.some(key => url.searchParams.get(key) !== query[key])) {
        throw new Error('交接链接与冻结的原轮次不一致。')
      }
      const voidResult = task.taskType === 'QA_DECISION_HANDOFF' &&
        task.status === 'DONE' && task.reason.startsWith('void：')
      if (task.status === 'CANCELED' || (!context.current && !voidResult) ||
        (context.processable && (task.status !== 'TODO' || task.taskType !== 'QA_REVIEW')) ||
        (query.handoffReadOnly === '1' && context.processable)) {
        throw new Error('原轮次交接已失效，不能办理当前工单。')
      }
      readOnly = !context.processable
    }
    if (query.from !== undefined && query.from !== 'deviation' && !fromSignatureGovernance) {
      throw new Error('关联不合格评审入口来源无效。')
    }
    if (query.autoCreate !== undefined || (!handoffEntry && query.from !== 'deviation' && query.activeOrderId !== undefined)) {
      throw new Error('关联评审详情不能同时发起新的评审。')
    }
    const fromDeviation = query.from === 'deviation'
    if (!fromDeviation && (query.deviationId !== undefined || query.batchExecutionId !== undefined)) {
      throw new Error('关联评审的偏差来源参数不完整。')
    }
    const deviationId = fromDeviation ? requireReviewIdentity(query.deviationId) : undefined
    const batchId = fromDeviation && query.batchExecutionId !== undefined ? requireReviewIdentity(query.batchExecutionId) : undefined
    const deviationOrderId = fromDeviation && query.activeOrderId !== undefined ? requireReviewIdentity(query.activeOrderId) : undefined
    if (fromDeviation && (batchId === undefined) === (deviationOrderId === undefined)) throw new Error('偏差入口必须提供唯一订单或批次身份。')
    const review = await getNonconformanceReview(reviewId, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    if (!review || requireReviewIdentity(review.id) !== reviewId) {
      throw new Error('关联评审单据身份不一致，不能打开。')
    }
    if (handoffEntry && (requireReviewIdentity(review.activeOrderId) !== handoffActiveOrderId ||
      (!readOnly && review.reviewStatus !== REVIEW_STATUS_PENDING_REVIEW))) {
      throw new Error('评审不属于原交接工单或已完成处置。')
    }
    if (fromDeviation) {
      if (review.sourceType !== SOURCE_TYPE_DEVIATION ||
        (batchId !== undefined ? (requireReviewIdentity(review.batchExecutionId) !== batchId ||
        requireReviewIdentity(review.sourceId) !== batchId) : requireReviewIdentity(review.activeOrderId) !== deviationOrderId)) {
        throw new Error('关联评审的正式偏差来源或批次不一致，不能打开。')
      }
      if (!review.deviationIdsJson) throw new Error('关联评审缺少正式偏差成员记录。')
      const deviationIds = parseExactIntegerJson(review.deviationIdsJson)
      if (!Array.isArray(deviationIds) || !deviationIds.map(requireReviewIdentity).some(id => id === deviationId)) {
        throw new Error('当前偏差不属于该关联评审，不能打开。')
      }
    }
    // Prepare materials before selecting the object: malformed data must not leave a writable dialog.
    fillDisposeForm(review)
    selectedReviewReadOnly.value = readOnly
    selectedReview.value = review
    reviewTabOpen.value = true
    activeTab.value = 'review'
    void loadReviews()
  } catch (error) {
    if (isCurrent()) errorText.value = resolveErrorMessage(error, '关联不合格评审加载失败。')
  } finally {
    if (isCurrent()) listLoading.value = false
  }
}

watch(
  () => [route.name, route.query.activeOrderId, route.query.autoCreate, route.query.reviewId,
    route.query.from, route.query.deviationId, route.query.batchExecutionId,
    route.query.handoffTaskId, route.query.handoffType, route.query.roundId, route.query.handoffReadOnly] as const,
  () => { void loadReviewEntry() },
  { flush: 'sync' }
)

onMounted(() => { void loadReviewEntry() })

const listColumnFilterDefinitions0 = computed<ListColumnFilterDefinition[]>(() => [
  {
    key: 'reviewCode',
    label: '评审单',
    type: 'text',
    getValue: (row) => [row.reviewCode || '--', resolveSourceTypeLabel(row.sourceType)].join(' ')
  },
  {
    key: 'workOrderCode',
    label: '生产工单',
    type: 'text',
    getValue: (row) => [row.workOrderCode || '--', row.batchCode || '--'].join(' ')
  },
  {
    key: 'reviewStatus',
    label: '状态',
    type: 'text',
    getValue: (row) => [resolveReviewStatusLabel(row.reviewStatus)].join(' ')
  },
  {
    key: 'disposition',
    label: '处置结论',
    type: 'text',
    getValue: (row) => [resolveDispositionLabel(row.disposition)].join(' ')
  }
])
</script>

<style scoped>
.edhr-ncr-review-viewport {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding));
  min-height: 0;
  margin-bottom: 0;
}
.edhr-ncr-review-viewport :deep(.el-card__body),
.edhr-ncr-review-viewport .edhr-ncr,
.edhr-ncr-review-viewport :deep(.el-tabs),
.edhr-ncr-review-viewport :deep(.el-tabs__content),
.edhr-ncr-review-viewport :deep(#pane-review) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.edhr-ncr-review-viewport :deep(.el-tabs__header),
.edhr-ncr-review-viewport .edhr-ncr > .el-alert { flex-shrink: 0; }
.edhr-ncr-review-viewport :deep(.el-tabs__content) { overflow: hidden; }
.edhr-ncr {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-ncr__muted {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
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

@media (width <= 960px) {
  .edhr-ncr__source-summary {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.el-dialog.edhr-ncr-create-dialog {
  width: min(720px, calc(100vw - 32px));
  padding: 0;
  overflow: hidden;
  border-radius: 12px;
}
.edhr-ncr-create-dialog .el-dialog__header {
  margin: 0;
  padding: 26px 32px 22px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.edhr-ncr-create-dialog .el-dialog__title {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.edhr-ncr-create-dialog .el-dialog__headerbtn { top: 12px; right: 12px; }
.edhr-ncr-create-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
  padding: 28px 32px 32px;
}
.edhr-ncr-create-dialog .el-form-item { margin-bottom: 30px; }
.edhr-ncr-create-dialog .el-form-item:last-child { margin-bottom: 0; }
.edhr-ncr-create-dialog .el-form-item__label {
  height: auto;
  margin-bottom: 12px;
  line-height: 22px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.edhr-ncr-create-dialog .el-form-item__content { min-width: 0; }
.edhr-ncr-create-dialog .el-select { width: 100%; }
.edhr-ncr-create-dialog .el-select__wrapper,
.edhr-ncr-create-dialog .el-input__wrapper { min-height: 40px; border-radius: 6px; }
.edhr-ncr-create-dialog .el-textarea__inner { padding: 12px 14px; line-height: 1.7; border-radius: 6px; }
.edhr-ncr-create-dialog__order {
  margin: 0 0 32px;
  padding: 22px 24px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.edhr-ncr-create-dialog__order dl { display: grid; gap: 22px; margin: 0; }
.edhr-ncr-create-dialog__order dl > div { display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: 16px; font-size: 14px; line-height: 1.8; }
.edhr-ncr-create-dialog__order dt { color: var(--el-text-color-secondary); }
.edhr-ncr-create-dialog__order dd { margin: 0; overflow-wrap: anywhere; color: var(--el-text-color-primary); font-variant-numeric: tabular-nums; }
.edhr-ncr-create-dialog .el-dialog__footer { padding: 20px 32px; border-top: 1px solid var(--el-border-color-lighter); }
.edhr-ncr-create-dialog .el-dialog__footer .el-button { height: 38px; min-width: 84px; border-radius: 6px; }
@media (width <= 520px) {
  .edhr-ncr-create-dialog .el-dialog__header { padding: 20px; }
  .edhr-ncr-create-dialog .el-dialog__body { padding: 18px 20px 22px; }
  .edhr-ncr-create-dialog .el-dialog__footer { padding: 14px 20px; }
  .edhr-ncr-create-dialog__order { padding: 14px; }
  .edhr-ncr-create-dialog__order dl > div { grid-template-columns: 1fr; gap: 2px; }
}
</style>
