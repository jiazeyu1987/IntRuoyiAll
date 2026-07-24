<template>
  <div class="showroom-product-list">
    <div class="showroom-product-list__toolbar">
      <div class="showroom-product-list__search-group">
        <el-input
          v-model="draftFilters.keyword"
          class="showroom-product-list__search"
          clearable
          placeholder="搜索产品名称"
          @keyup.enter="emitSearch"
        />
        <el-button type="primary" :loading="loading" @click="emitSearch">
          查询
        </el-button>
        <el-button @click="resetFilters">
          重置
        </el-button>
      </div>
      <div class="showroom-product-list__actions">
        <el-button v-if="manageProducts" type="primary" @click="emit('create')">新增</el-button>
        <el-button
          v-if="manageProducts"
          type="warning"
          plain
          :loading="Boolean(importingExcel)"
          @click="emit('import-excel')"
        >
          导入
        </el-button>
        <el-button
          v-if="manageProducts"
          type="warning"
          plain
          :loading="Boolean(importingExcel)"
          @click="emit('import-base-workbook')"
        >
          导入无产品图底表
        </el-button>
        <el-button
          v-if="manageProducts"
          type="success"
          plain
          :loading="Boolean(exportingExcel)"
          @click="emit('export-excel')"
        >
          导出
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :loading="Boolean(batchPublishing)"
          @click="emit('batch-publish')"
        >
          全部发布
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :loading="Boolean(batchGeneratingSalesCountries)"
          @click="emit('batch-generate-sales-countries')"
        >
          一键在售国家
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :disabled="Boolean(narrationScriptTaskActive)"
          :loading="Boolean(batchGeneratingNarrationScript)"
          @click="handleBatchGenerateNarrationScriptClick"
        >
          一键讲解
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :disabled="Boolean(batchTranslatePublishTaskStatus?.active || batchTranslatePublishTaskStatus?.running)"
          :loading="Boolean(batchTranslatingPublishing)"
          @click="handleBatchTranslatePublishClick"
        >
          一键翻译
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :loading="Boolean(batchGeneratingAudio)"
          @click="emit('batch-generate-audio')"
        >
          一键语音
        </el-button>
        <el-button
          v-if="canBatchGeneratePublishedMedia"
          type="primary"
          plain
          :loading="Boolean(batchGeneratingCover)"
          @click="handleBatchGenerateCoverClick"
        >
          一键封面
        </el-button>
      </div>
    </div>

    <div v-if="showNarrationTaskBanner" class="showroom-product-list__task-banner">
      <div class="showroom-product-list__task-header">
        <span class="showroom-product-list__task-title">一键讲解任务</span>
        <div class="showroom-product-list__task-meta">
          <el-tag :type="narrationScriptTaskActive ? 'warning' : 'info'" effect="plain">
            {{ narrationScriptTaskStateLabel }}
          </el-tag>
          <el-button
            link
            type="info"
            class="showroom-product-list__task-close"
            @click="dismissNarrationTaskBanner"
          >
            关闭
          </el-button>
        </div>
      </div>
      <p class="showroom-product-list__task-line">
        筛选快照：{{ narrationScriptTaskFilterSummary }}
      </p>
      <p v-if="narrationScriptTaskCurrentProductText" class="showroom-product-list__task-line">
        当前执行产品：{{ narrationScriptTaskCurrentProductText }}
      </p>
      <p class="showroom-product-list__task-line">
        命中 {{ narrationScriptTaskStatus?.matchedCount || 0 }}，整条跳过
        {{ narrationScriptTaskStatus?.skippedCompletedCount || 0 }}，本轮补齐
        {{ narrationScriptTaskStatus?.generatedLanguageCount || 0 }} 个语言讲解稿，失败
        {{ narrationScriptTaskStatus?.failedCount || 0 }}，剩余缺口
        {{ narrationScriptTaskStatus?.remainingCount || 0 }}
      </p>
      <p v-if="narrationScriptTaskTimeSummary" class="showroom-product-list__task-line">
        {{ narrationScriptTaskTimeSummary }}
      </p>
      <p
        v-if="narrationScriptTaskFailureText"
        class="showroom-product-list__task-line showroom-product-list__task-line--danger"
      >
        最近失败：{{ narrationScriptTaskFailureText }}
      </p>
    </div>

    <div v-if="showTranslatePublishTaskBanner" class="showroom-product-list__task-banner">
      <div class="showroom-product-list__task-header">
        <span class="showroom-product-list__task-title">一键翻译任务</span>
        <div class="showroom-product-list__task-meta">
          <el-tag :type="translatePublishTaskTagType" effect="plain">
            {{ translatePublishTaskStateLabel }}
          </el-tag>
          <el-button
            link
            type="info"
            class="showroom-product-list__task-close"
            @click="dismissTranslatePublishTaskBanner"
          >
            关闭
          </el-button>
        </div>
      </div>
      <p class="showroom-product-list__task-line">
        筛选快照：{{ translatePublishTaskFilterSummary }}
      </p>
      <p class="showroom-product-list__task-line">
        当前执行产品：{{ translatePublishTaskCurrentProductText }}
      </p>
      <p class="showroom-product-list__task-line">
        命中 {{ batchTranslatePublishTaskStatus?.matchedCount || 0 }}，成功发布
        {{ batchTranslatePublishTaskStatus?.succeededCount || 0 }}，失败
        {{ batchTranslatePublishTaskStatus?.failedCount || 0 }}，剩余
        {{ batchTranslatePublishTaskStatus?.remainingCount || 0 }}
      </p>
      <p v-if="translatePublishTaskTimeSummary" class="showroom-product-list__task-line">
        {{ translatePublishTaskTimeSummary }}
      </p>
      <p
        v-if="translatePublishTaskFailureText"
        class="showroom-product-list__task-line showroom-product-list__task-line--danger"
      >
        最近失败：{{ translatePublishTaskFailureText }}
      </p>
    </div>

    <div v-if="showCoverTaskBanner" class="showroom-product-list__task-banner">
      <div class="showroom-product-list__task-header">
        <span class="showroom-product-list__task-title">一键封面任务</span>
        <div class="showroom-product-list__task-meta">
          <el-tag :type="coverTaskTagType" effect="plain">
            {{ coverTaskStateLabel }}
          </el-tag>
          <el-button
            link
            type="info"
            class="showroom-product-list__task-close"
            @click="dismissCoverTaskBanner"
          >
            关闭
          </el-button>
        </div>
      </div>
      <p class="showroom-product-list__task-line">
        允许状态：{{ coverTaskAllowStatusLabel }}
      </p>
      <p class="showroom-product-list__task-line">
        筛选快照：{{ coverTaskFilterSummary }}
      </p>
      <p class="showroom-product-list__task-line">
        当前执行产品：{{ coverTaskCurrentProductText }}
      </p>
      <p class="showroom-product-list__task-line">
        命中 {{ coverTaskSummary?.matchedCount || 0 }}，已发布 {{ coverTaskSummary?.publishedCount || 0 }}，跳过未发布
        {{ coverTaskSummary?.skippedUnpublishedCount || 0 }}，跳过已有封面
        {{ coverTaskSummary?.skippedExistingCount || 0 }}，成功生成
        {{ coverTaskSummary?.succeededCount || 0 }}，失败 {{ coverTaskSummary?.failedCount || 0 }}
        <template v-if="(coverTaskSummary?.remainingPendingCount || 0) > 0">
          ，剩余未完成 {{ coverTaskSummary?.remainingPendingCount || 0 }}
        </template>
      </p>
      <p v-if="coverTaskTimeSummary" class="showroom-product-list__task-line">
        {{ coverTaskTimeSummary }}
      </p>
      <p
        v-if="coverTaskFailureText"
        class="showroom-product-list__task-line showroom-product-list__task-line--danger"
      >
        最近失败：{{ coverTaskFailureText }}
      </p>
    </div>

    <div class="showroom-product-list__table-shell">
      <el-table
        v-loading="loading"
        :data="normalizedRows"
        border
        class="showroom-product-list__table"
        :row-class-name="resolveProductRowClassName"
        row-key="productId"
      >
        <el-table-column label="中文名称" min-width="160" prop="nameCn" show-overflow-tooltip />
        <el-table-column
          label="旧产品编号"
          min-width="130"
          prop="legacyProductCode"
          show-overflow-tooltip
        />
        <el-table-column label="当前版本" width="96">
          <template #default="{ row }">
            {{ row.revisionNo }}
          </template>
        </el-table-column>
        <el-table-column label="审批状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.approvalTagType">{{ row.approvalText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="BU" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.pipelineLayout || '未填写' }}
          </template>
        </el-table-column>
        <el-table-column
          label="持证人"
          min-width="180"
          prop="ownerTypeText"
          show-overflow-tooltip
        />
        <el-table-column label="获证状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.lifecycleTagType">{{ row.lifecycleText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="在售国家"
          min-width="170"
          prop="targetMarket"
          show-overflow-tooltip
        />
        <el-table-column label="封面" width="104">
          <template #default="{ row }">
            <div class="showroom-product-list__cover-cell">
              <el-image
                v-if="row.coverImageUrl"
                :preview-src-list="[row.coverImageUrl]"
                :src="row.coverImageUrl"
                class="showroom-product-list__cover"
                fit="cover"
                preview-teleported
              />
              <span v-else class="showroom-product-list__cover-empty">未上传</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="220">
          <template #default="{ row }">
            <div class="showroom-product-list__status-cell">
              <div class="showroom-product-list__status-grid">
                <span
                  v-for="item in row.contentStatusItems"
                  :key="item.key"
                  :class="[
                    'showroom-product-list__status-pill',
                    item.ready
                      ? 'showroom-product-list__status-pill--ready'
                      : 'showroom-product-list__status-pill--missing'
                  ]"
                >
                  <span class="showroom-product-list__status-marker">
                    {{ item.ready ? 'OK' : 'MISS' }}
                  </span>
                  <span>{{ item.shortLabel }}</span>
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          label="操作"
          :width="manageProducts ? 370 : rowEditableExists ? 240 : 180"
        >
          <template #default="{ row }">
            <el-button v-if="manageProducts" link type="primary" @click="emit('assign', row.raw)">
              指派
            </el-button>
            <el-button
              v-if="manageProducts"
              link
              type="primary"
              @click="emit('open-audio-dialog', row.raw)"
            >
              语音
            </el-button>
            <el-button v-if="row.editable" link type="primary" @click="emit('edit', row.raw)">基础</el-button>
            <el-button link type="primary" @click="emit('detail', row.raw)">详细信息</el-button>
            <el-button
              link
              type="primary"
              @click="emit('version-center', { productId: row.productId, displayRevisionId: row.displayRevisionId })"
            >
              版本中心
            </el-button>
            <el-button
              v-if="manageProducts"
              link
              type="primary"
              :disabled="!isPublishableStatus(row.approvalStatus)"
              :loading="String(publishingProductId ?? '') === row.productId"
              @click="emit('publish', row.raw)"
            >
              发布
            </el-button>
            <el-button v-if="manageProducts" link type="danger" @click="emit('delete', row.raw)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="showroom-product-list__footer">
        <div class="showroom-product-list__summary">共 {{ pageTotal }} 条，共 {{ totalPages }} 页</div>
        <el-pagination
          v-if="pageTotal > 0"
          :background="true"
          :current-page="pageNo"
          :page-size="pageSize"
          :total="pageTotal"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'ProductListTable' })

type ProductOwnerType = 'YINGTAI' | 'SUBSIDIARY' | 'INCOMPLETE'
type LifecycleStage = 'REGISTERED' | 'R_AND_D' | 'UNKNOWN'
type IncompleteStatus = 'COMPLETE' | 'INCOMPLETE'
type ApprovalStatus =
  | 'IN_FILLING'
  | 'DRAFT'
  | 'PENDING_SUPERVISOR_APPROVAL'
  | 'PENDING_GAOXIN_APPROVAL'
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'PUBLISHED'

type ProductTagType = 'success' | 'warning' | 'info' | 'danger'

interface ProductListFilters {
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
}

interface ProductListRow {
  productId: string
  productCode: string
  legacyProductCode: string
  currentRevisionId: string
  displayRevisionId: string
  revisionNo: string
  live: boolean
  editable: boolean
  nameCn: string
  nameEn: string
  pipelineLayout: string
  targetMarket: string
  ownerCompanyId: string
  ownerType: ProductOwnerType
  ownerTypeText: string
  ownerTypeTagType: ProductTagType
  lifecycleStage: LifecycleStage
  lifecycleText: string
  lifecycleTagType: ProductTagType
  incompleteStatus: IncompleteStatus
  incompleteText: string
  incompleteTagType: ProductTagType
  approvalStatus: ApprovalStatus
  approvalText: string
  approvalTagType: ProductTagType
  coverImageUrl: string
  activeAssignmentAssigneeLabel: string
  hasNarrationScript: boolean
  latestNarrationAudioUrl: string
  latestNarrationVoice: string
  contentStatusItems: ProductStatusItem[]
  raw: Record<string, unknown>
}

interface LatestNarrationSummary {
  narrationVersionId: string
  language: string
  audienceType: string
  status: string
  live: boolean
  audioReady: boolean
  audioUrl: string
  voice: string
}

interface ActiveAssignmentSummary {
  assignmentId: string
  assigneeUserId: string
  status: string
}

interface NarrationAvailabilitySummary {
  language: string
  audienceType: string
  status: string
  live: boolean
  audioReady: boolean
}

interface ProductStatusItem {
  key: 'zh_countries_on_sale' | 'en_countries_on_sale' | 'zh_audio' | 'en_audio'
  shortLabel: string
  ready: boolean
}

interface NarrationScriptTaskFailure {
  productId: number
  productCode: string
  nameCn: string
  reason: string
}

interface NarrationScriptTaskCurrentProduct {
  productId: number
  productCode: string
  nameCn: string
}

interface CoverTaskCurrentProduct {
  productId: number
  productCode: string
  nameCn: string
}

interface BatchGenerateFailure {
  productId: number
  productCode: string
  nameCn: string
  reason: string
}

interface NarrationScriptTaskStatus {
  active: boolean
  running: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  skippedCompletedCount: number
  generatedLanguageCount: number
  failedCount: number
  remainingCount: number
  startedAt?: number | null
  lastRunAt?: number | null
  completedAt?: number | null
  currentProduct?: NarrationScriptTaskCurrentProduct | null
  lastFailure?: NarrationScriptTaskFailure | null
  lastFailureAt?: number | null
}

interface CoverTaskSummary {
  startAllowed?: boolean
  active?: boolean
  running?: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  updatedAt?: number | null
  matchedCount: number
  publishedCount: number
  skippedUnpublishedCount: number
  skippedExistingCount: number
  succeededCount: number
  failedCount: number
  taskId?: number | null
  taskStatus?: string | null
  remainingPendingCount?: number
  nextCheckAt?: string | null
  lastRunAt?: number | null
  completedAt?: number | null
  lastFailureMessage?: string | null
  currentProduct?: CoverTaskCurrentProduct | null
  failures: BatchGenerateFailure[]
}

interface TranslatePublishTaskStatus {
  active: boolean
  running: boolean
  keyword: string
  lifecycleStage: string
  incompleteStatus: string
  approvalStatus: string
  matchedCount: number
  succeededCount: number
  failedCount: number
  remainingCount: number
  startedAt?: number | null
  lastRunAt?: number | null
  completedAt?: number | null
  currentProduct?: CoverTaskCurrentProduct | null
  lastFailure?: BatchGenerateFailure | null
  lastFailureAt?: number | null
  failures: BatchGenerateFailure[]
}

const props = defineProps<{
  products: unknown[]
  loading?: boolean
  manageProducts?: boolean
  canBatchGeneratePublishedMedia?: boolean
  batchGeneratingSalesCountries?: boolean
  batchGeneratingNarrationScript?: boolean
  batchPublishing?: boolean
  narrationScriptTaskActive?: boolean
  narrationScriptTaskStatus?: NarrationScriptTaskStatus | null
  coverTaskSummary?: CoverTaskSummary | null
  batchGeneratingAudio?: boolean
  batchGeneratingCover?: boolean
  batchTranslatingPublishing?: boolean
  batchTranslatePublishTaskStatus?: TranslatePublishTaskStatus | null
  batchAudioAutoCheckEnabled?: boolean
  batchAudioAutoCheckLabel?: string
  publishingProductId?: number | null
  exportingExcel?: boolean
  importingExcel?: boolean
  filters: ProductListFilters
  pageNo: number
  pageSize: number
  pageTotal: number
  userOptions?: Array<{ id: number; username: string; nickname: string }>
}>()

const manageProducts = computed(() => Boolean(props.manageProducts))
const canBatchGeneratePublishedMedia = computed(() => Boolean(props.canBatchGeneratePublishedMedia))
const batchGeneratingSalesCountries = computed(() => Boolean(props.batchGeneratingSalesCountries))
const batchGeneratingNarrationScript = computed(() => Boolean(props.batchGeneratingNarrationScript))
const batchPublishing = computed(() => Boolean(props.batchPublishing))
const narrationScriptTaskActive = computed(() => Boolean(props.narrationScriptTaskActive))
const narrationScriptTaskStatus = computed(() => props.narrationScriptTaskStatus ?? null)
const coverTaskSummary = computed(() => props.coverTaskSummary ?? null)
const batchGeneratingAudio = computed(() => Boolean(props.batchGeneratingAudio))
const batchGeneratingCover = computed(() => Boolean(props.batchGeneratingCover))
const batchTranslatingPublishing = computed(() => Boolean(props.batchTranslatingPublishing))
const batchTranslatePublishTaskStatus = computed(() => props.batchTranslatePublishTaskStatus ?? null)
const publishingProductId = computed(() => props.publishingProductId ?? null)
const exportingExcel = computed(() => Boolean(props.exportingExcel))
const importingExcel = computed(() => Boolean(props.importingExcel))
const rowEditableExists = computed(() => normalizedRows.value.some((row) => row.editable))

const emit = defineEmits<{
  create: []
  'batch-publish': []
  'batch-generate-sales-countries': []
  'batch-generate-narration-script': []
  'batch-translate-publish': []
  'batch-generate-audio': []
  'batch-generate-cover': []
  'export-excel': []
  'import-excel': []
  'import-base-workbook': []
  assign: [product: Record<string, unknown>]
  'open-audio-dialog': [product: Record<string, unknown>]
  edit: [product: Record<string, unknown>]
  detail: [product: Record<string, unknown>]
  'version-center': [payload: { productId: string; displayRevisionId: string }]
  publish: [product: Record<string, unknown>]
  'page-change': [pagination: { pageNo: number; pageSize: number }]
  search: [filters: ProductListFilters]
  delete: [product: Record<string, unknown>]
}>()

const isPublishableStatus = (status: ApprovalStatus) => {
  return status === 'DRAFT' || status === 'REJECTED'
}

const createEmptyFilters = (): ProductListFilters => ({
  keyword: '',
  lifecycleStage: '',
  incompleteStatus: '',
  approvalStatus: ''
})

const draftFilters = reactive(createEmptyFilters())
const bannerDismissState = reactive({
  narration: false,
  translatePublish: false,
  cover: false
})

const formatTaskTime = (value?: number | null) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString('zh-CN', { hour12: false })
}

const narrationScriptTaskVisible = computed(() => {
  const state = narrationScriptTaskStatus.value
  if (!state) {
    return false
  }
  return Boolean(state.active || state.running)
})

const narrationScriptTaskStateLabel = computed(() => {
  const state = narrationScriptTaskStatus.value
  if (!state) {
    return ''
  }
  if (state.running) {
    return state.remainingCount > 0 ? `执行中（剩${state.remainingCount}）` : '执行中'
  }
  if (state.active) {
    return state.remainingCount > 0 ? `定时续跑中（剩${state.remainingCount}）` : '定时续跑中'
  }
  return '已停止'
})

const narrationScriptTaskFilterSummary = computed(() => {
  const state = narrationScriptTaskStatus.value
  if (!state) {
    return '当前无任务'
  }
  return [
    `关键词：${state.keyword || '全部'}`,
    `生命周期：${state.lifecycleStage || '全部'}`,
    `资料状态：${state.incompleteStatus || '全部'}`,
    `审批状态：${state.approvalStatus || '全部'}`
  ].join(' / ')
})

const narrationScriptTaskTimeSummary = computed(() => {
  const state = narrationScriptTaskStatus.value
  if (!state) {
    return ''
  }
  return [
    state.startedAt ? `启动于 ${formatTaskTime(state.startedAt)}` : '',
    state.lastRunAt ? `最近执行 ${formatTaskTime(state.lastRunAt)}` : '',
    state.completedAt ? `完成于 ${formatTaskTime(state.completedAt)}` : ''
  ]
    .filter(Boolean)
    .join(' / ')
})

const narrationScriptTaskCurrentProductText = computed(() => {
  const currentProduct = narrationScriptTaskStatus.value?.currentProduct
  if (!currentProduct) {
    return ''
  }
  return [currentProduct.productCode, currentProduct.nameCn].filter(Boolean).join(' / ')
})

const narrationScriptTaskFailureText = computed(() => {
  const failure = narrationScriptTaskStatus.value?.lastFailure
  if (!failure) {
    return ''
  }
  const identity = failure.productCode || failure.nameCn || `产品ID ${failure.productId}`
  return `${identity}：${failure.reason || '未知失败'}`
})

const showNarrationTaskBanner = computed(() => {
  return narrationScriptTaskVisible.value && !bannerDismissState.narration
})

const translatePublishTaskVisible = computed(() => {
  const state = batchTranslatePublishTaskStatus.value
  if (!state) {
    return false
  }
  return Boolean(state.active || state.running || state.completedAt || state.failedCount > 0)
})

const translatePublishTaskStateLabel = computed(() => {
  const state = batchTranslatePublishTaskStatus.value
  if (!state) {
    return ''
  }
  if (state.running) {
    return state.remainingCount > 0 ? `执行中（剩${state.remainingCount}）` : '执行中'
  }
  if (state.active) {
    return '等待执行'
  }
  if (state.failedCount > 0) {
    return '部分失败'
  }
  return state.completedAt ? '已完成' : '未启动'
})

const translatePublishTaskTagType = computed(() => {
  const state = batchTranslatePublishTaskStatus.value
  if (!state) {
    return 'info'
  }
  if (state.active || state.running) {
    return 'warning'
  }
  if (state.failedCount > 0) {
    return 'danger'
  }
  return 'success'
})

const translatePublishTaskFilterSummary = computed(() => {
  const state = batchTranslatePublishTaskStatus.value
  if (!state) {
    return '当前无任务'
  }
  return [
    `关键词：${state.keyword || '全部'}`,
    `生命周期：${state.lifecycleStage || '全部'}`,
    `资料状态：${state.incompleteStatus || '全部'}`,
    `审批状态：${state.approvalStatus || '全部'}`
  ].join(' / ')
})

const translatePublishTaskCurrentProductText = computed(() => {
  const currentProduct = batchTranslatePublishTaskStatus.value?.currentProduct
  if (!currentProduct) {
    return '当前无运行中的产品'
  }
  return [currentProduct.productCode, currentProduct.nameCn].filter(Boolean).join(' / ')
})

const translatePublishTaskTimeSummary = computed(() => {
  const state = batchTranslatePublishTaskStatus.value
  if (!state) {
    return ''
  }
  return [
    state.startedAt ? `启动于 ${formatTaskTime(state.startedAt)}` : '',
    state.lastRunAt ? `最近执行 ${formatTaskTime(state.lastRunAt)}` : '',
    state.completedAt ? `完成于 ${formatTaskTime(state.completedAt)}` : ''
  ].filter(Boolean).join(' / ')
})

const translatePublishTaskFailureText = computed(() => {
  const failure = batchTranslatePublishTaskStatus.value?.lastFailure
  if (!failure) {
    return ''
  }
  const identity = failure.productCode || failure.nameCn || `产品ID ${failure.productId}`
  return `${identity}：${failure.reason || '未知失败'}`
})

const showTranslatePublishTaskBanner = computed(() => {
  return translatePublishTaskVisible.value && !bannerDismissState.translatePublish
})

const coverTaskVisible = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return false
  }
  const normalizedTaskStatus = String(state.taskStatus || '').trim().toUpperCase()
  return (
    Boolean(state.active) ||
    Boolean(state.running) ||
    normalizedTaskStatus === 'WAITING' ||
    normalizedTaskStatus === 'RUNNING'
  )
})

const coverTaskStateLabel = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return ''
  }
  if (!state.active && state.startAllowed) {
    return '允许执行'
  }
  const normalizedTaskStatus = String(state.taskStatus || '').trim().toUpperCase()
  if (normalizedTaskStatus === 'RUNNING') {
    return '执行中'
  }
  if (normalizedTaskStatus === 'WAITING') {
    return (state.remainingPendingCount || 0) > 0 ? `定时续跑中（剩${state.remainingPendingCount || 0}）` : '定时续跑中'
  }
  if (state.failedCount > 0) {
    return '部分失败'
  }
  if (normalizedTaskStatus === 'COMPLETED') {
    return '已完成'
  }
  return state.matchedCount === 0 ? '已停止' : '已完成'
})

const coverTaskTagType = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return 'info'
  }
  if (!state.active && state.startAllowed) {
    return 'success'
  }
  const normalizedTaskStatus = String(state.taskStatus || '').trim().toUpperCase()
  if (normalizedTaskStatus === 'WAITING' || normalizedTaskStatus === 'RUNNING') {
    return 'warning'
  }
  if (state.failedCount > 0) {
    return 'danger'
  }
  return 'info'
})

const coverTaskFilterSummary = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return '当前无任务'
  }
  return [
    `关键词：${state.keyword || '全部'}`,
    `生命周期：${state.lifecycleStage || '全部'}`,
    `资料状态：${state.incompleteStatus || '全部'}`,
    `审批状态：${state.approvalStatus || '全部'}`
  ].join(' / ')
})

const coverTaskTimeSummary = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return ''
  }
  const segments = [
    state.lastRunAt ? `最近执行 ${formatTaskTime(state.lastRunAt)}` : '',
    state.completedAt ? `完成于 ${formatTaskTime(state.completedAt)}` : ''
  ].filter(Boolean)
  const nextCheckAt = String(state.nextCheckAt || '').trim()
  if (nextCheckAt) {
    segments.push(`下一次检查 ${nextCheckAt}`)
  }
  return segments.join(' / ')
})

const coverTaskFailureText = computed(() => {
  const state = coverTaskSummary.value
  const failure = state?.failures?.[0]
  if (!failure) {
    return state?.lastFailureMessage || ''
  }
  const identity = failure.productCode || failure.nameCn || `产品ID ${failure.productId}`
  return `${identity}：${failure.reason || '未知失败'}`
})

const coverTaskAllowStatusLabel = computed(() => {
  const state = coverTaskSummary.value
  if (!state) {
    return '未知'
  }
  return state.startAllowed ? '允许执行' : '当前任务占用中'
})

const coverTaskCurrentProductText = computed(() => {
  const currentProduct = coverTaskSummary.value?.currentProduct
  if (!currentProduct) {
    return '当前无运行中的产品'
  }
  return [currentProduct.productCode, currentProduct.nameCn].filter(Boolean).join(' / ')
})

const showCoverTaskBanner = computed(() => {
  return coverTaskVisible.value && !bannerDismissState.cover
})

const dismissNarrationTaskBanner = () => {
  bannerDismissState.narration = true
}

const dismissTranslatePublishTaskBanner = () => {
  bannerDismissState.translatePublish = true
}

const dismissCoverTaskBanner = () => {
  bannerDismissState.cover = true
}

const handleBatchGenerateNarrationScriptClick = () => {
  bannerDismissState.narration = false
  emit('batch-generate-narration-script')
}

const handleBatchTranslatePublishClick = () => {
  bannerDismissState.translatePublish = false
  emit('batch-translate-publish')
}

const handleBatchGenerateCoverClick = () => {
  bannerDismissState.cover = false
  emit('batch-generate-cover')
}

watch(
  () => props.filters,
  (nextFilters) => {
    Object.assign(draftFilters, createEmptyFilters(), nextFilters)
  },
  { deep: true, immediate: true }
)

const snapshotAliases = {
  productId: ['productId', 'product_id'],
  productCode: ['productCode', 'product_code'],
  legacyProductCode: ['legacyProductCode', 'legacy_product_code'],
  currentRevisionId: ['currentRevisionId', 'current_revision_id'],
  editable: ['editable'],
  incomplete: ['incomplete', 'incompleteFlag', 'incomplete_flag'],
  live: ['live']
} as const

const revisionAliases = {
  revision: ['revision', 'currentRevision', 'productRevision'],
  nameCn: ['nameCn', 'name_cn'],
  nameEn: ['nameEn', 'name_en'],
  revisionNo: ['revisionNo', 'revision_no'],
  status: ['status']
} as const

const displayRevisionAliases = {
  revision: ['displayRevision'],
  revisionId: ['revisionId', 'revision_id'],
  nameCn: ['nameCn', 'name_cn'],
  nameEn: ['nameEn', 'name_en'],
  revisionNo: ['revisionNo', 'revision_no']
} as const

const fieldAliases = {
  ownerCompanyId: ['owner_company_id', 'ownerCompanyId'],
  productOwnerType: ['product_owner_type', 'productOwnerType'],
  lifecycleStage: ['lifecycle_stage', 'lifecycleStage'],
  coverImage: ['cover_image', 'coverImage']
} as const

const requireObject = (value: unknown, index: number, label: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`产品列表第 ${index + 1} 行${label}不是有效对象`)
  }
  return value as Record<string, unknown>
}

const resolveField = (
  source: Record<string, unknown>,
  aliases: readonly string[],
  fieldLabel: string,
  index: number
) => {
  const fieldName = aliases.find((alias) => source[alias] !== undefined && source[alias] !== null)
  if (!fieldName) {
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：${fieldLabel}`)
  }
  return source[fieldName]
}

const resolveOptionalField = (source: Record<string, unknown>, aliases: readonly string[]) => {
  const fieldName = aliases.find((alias) => source[alias] !== undefined && source[alias] !== null)
  if (!fieldName) {
    return undefined
  }
  return source[fieldName]
}

const resolveStringField = (
  source: Record<string, unknown>,
  aliases: readonly string[],
  fieldLabel: string,
  index: number
) => {
  const value = resolveField(source, aliases, fieldLabel, index)
  if (typeof value !== 'string' && typeof value !== 'number') {
    throw new Error(`产品列表第 ${index + 1} 行字段类型错误：${fieldLabel}`)
  }
  const text = String(value).trim()
  if (!text) {
    throw new Error(`产品列表第 ${index + 1} 行字段为空：${fieldLabel}`)
  }
  return text
}

const resolveBlankableStringField = (
  source: Record<string, unknown>,
  aliases: readonly string[],
  fieldLabel: string,
  index: number
) => {
  const value = resolveField(source, aliases, fieldLabel, index)
  if (typeof value !== 'string' && typeof value !== 'number') {
    throw new Error(`产品列表第 ${index + 1} 行字段类型错误：${fieldLabel}`)
  }
  return String(value).trim()
}

const resolveOptionalStringField = (
  source: Record<string, unknown>,
  aliases: readonly string[],
  fieldLabel: string,
  index: number
) => {
  const value = resolveOptionalField(source, aliases)
  if (value === undefined) {
    return ''
  }
  if (typeof value !== 'string' && typeof value !== 'number') {
    throw new Error(`产品列表第 ${index + 1} 行字段类型错误：${fieldLabel}`)
  }
  return String(value).trim()
}

const resolveBooleanField = (
  source: Record<string, unknown>,
  aliases: readonly string[],
  fieldLabel: string,
  index: number
) => {
  const value = resolveField(source, aliases, fieldLabel, index)
  if (value === true || value === 1 || value === '1' || value === 'true') {
    return true
  }
  if (value === false || value === 0 || value === '0' || value === 'false') {
    return false
  }
  throw new Error(`产品列表第 ${index + 1} 行字段无法识别：${fieldLabel}`)
}

const resolveRevision = (product: Record<string, unknown>, index: number) => {
  const revisionValue = resolveField(product, revisionAliases.revision, 'revision', index)
  return requireObject(revisionValue, index, 'revision')
}

const resolveDisplayRevision = (product: Record<string, unknown>, index: number) => {
  const revisionValue = resolveField(
    product,
    displayRevisionAliases.revision,
    'displayRevision',
    index
  )
  return requireObject(revisionValue, index, 'displayRevision')
}

const resolveRevisionFields = (
  revision: Record<string, unknown>,
  index: number,
  allowMissing: boolean
) => {
  const fieldsValue = resolveOptionalField(revision, ['fields'])
  if (fieldsValue === undefined) {
    if (allowMissing) {
      return null
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：fields`)
  }
  return requireObject(fieldsValue, index, 'fields')
}

const resolveProductOwnerType = (
  fields: Record<string, unknown> | null,
  incomplete: boolean,
  index: number
): Pick<ProductListRow, 'ownerType' | 'ownerTypeText' | 'ownerTypeTagType'> => {
  if (!fields) {
    if (incomplete) {
      return { ownerType: 'INCOMPLETE', ownerTypeText: '未完善', ownerTypeTagType: 'warning' }
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：product_owner_type`)
  }
  const missingValue = resolveOptionalField(fields, fieldAliases.productOwnerType)
  if (missingValue === undefined) {
    if (incomplete) {
      return { ownerType: 'INCOMPLETE', ownerTypeText: '未完善', ownerTypeTagType: 'warning' }
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：product_owner_type`)
  }
  const value = resolveStringField(fields, fieldAliases.productOwnerType, 'product_owner_type', index)
  if (value === 'YINGTAI') {
    return {
      ownerType: value,
      ownerTypeText: '瑛泰医疗',
      ownerTypeTagType: 'success'
    }
  }
  if (value === 'SUBSIDIARY') {
    return {
      ownerType: value,
      ownerTypeText: '瑛泰医疗',
      ownerTypeTagType: 'info'
    }
  }
  throw new Error(`产品列表第 ${index + 1} 行 product_owner_type 未知：${value}`)
}

const resolveLifecycleStage = (
  fields: Record<string, unknown> | null,
  incomplete: boolean,
  index: number
): Pick<ProductListRow, 'lifecycleStage' | 'lifecycleText' | 'lifecycleTagType'> => {
  if (!fields) {
    if (incomplete) {
      return { lifecycleStage: 'UNKNOWN', lifecycleText: '未填写', lifecycleTagType: 'warning' }
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：lifecycle_stage`)
  }
  const missingValue = resolveOptionalField(fields, fieldAliases.lifecycleStage)
  if (missingValue === undefined) {
    if (incomplete) {
      return { lifecycleStage: 'UNKNOWN', lifecycleText: '未填写', lifecycleTagType: 'warning' }
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：lifecycle_stage`)
  }
  const value = resolveStringField(fields, fieldAliases.lifecycleStage, 'lifecycle_stage', index)
  if (value === 'REGISTERED') {
    return { lifecycleStage: value, lifecycleText: '已注册', lifecycleTagType: 'success' }
  }
  if (value === 'R_AND_D') {
    return { lifecycleStage: value, lifecycleText: '研发中', lifecycleTagType: 'warning' }
  }
  throw new Error(`产品列表第 ${index + 1} 行 lifecycle_stage 未知：${value}`)
}

const resolveIncompleteStatus = (
  product: Record<string, unknown>,
  index: number
): Pick<ProductListRow, 'incompleteStatus' | 'incompleteText' | 'incompleteTagType'> => {
  const incomplete = resolveBooleanField(product, snapshotAliases.incomplete, 'incomplete', index)
  if (incomplete) {
    return {
      incompleteStatus: 'INCOMPLETE',
      incompleteText: '资料未完善',
      incompleteTagType: 'warning'
    }
  }
  return { incompleteStatus: 'COMPLETE', incompleteText: '资料完整', incompleteTagType: 'success' }
}

const resolveOwnerCompanyId = (
  fields: Record<string, unknown> | null,
  incomplete: boolean,
  index: number
) => {
  if (!fields) {
    if (incomplete) {
      return ''
    }
    throw new Error(`产品列表第 ${index + 1} 行缺少字段：owner_company_id`)
  }
  const value = resolveOptionalStringField(
    fields,
    fieldAliases.ownerCompanyId,
    'owner_company_id',
    index
  )
  if (!value && !incomplete) {
    throw new Error(`产品列表第 ${index + 1} 行字段为空：owner_company_id`)
  }
  return value
}

const resolveCoverImageUrl = (
  latestFields: Record<string, unknown> | null,
  displayFields: Record<string, unknown> | null,
  index: number
) => {
  const latestCoverImageUrl = latestFields
    ? resolveOptionalStringField(latestFields, fieldAliases.coverImage, 'cover_image', index)
    : ''
  if (latestCoverImageUrl) {
    return latestCoverImageUrl
  }
  if (!displayFields) {
    return ''
  }
  return resolveOptionalStringField(displayFields, fieldAliases.coverImage, 'cover_image', index)
}

const resolveApprovalStatus = (
  revision: Record<string, unknown>,
  index: number
): Pick<ProductListRow, 'approvalStatus' | 'approvalText' | 'approvalTagType'> => {
  const value = resolveStringField(revision, revisionAliases.status, 'status', index)
  const statusMap: Record<ApprovalStatus, { text: string; tagType: ProductTagType }> = {
    IN_FILLING: { text: '指派中', tagType: 'warning' },
    DRAFT: { text: '草稿', tagType: 'info' },
    PENDING_SUPERVISOR_APPROVAL: { text: '审核中', tagType: 'warning' },
    PENDING_GAOXIN_APPROVAL: { text: '审核中', tagType: 'warning' },
    PENDING: { text: '审核中', tagType: 'warning' },
    APPROVED: { text: '已批准', tagType: 'success' },
    REJECTED: { text: '已驳回', tagType: 'danger' },
    PUBLISHED: { text: '已发布', tagType: 'success' }
  }

  if (value in statusMap) {
    const approvalStatus = value as ApprovalStatus
    return {
      approvalStatus,
      approvalText: statusMap[approvalStatus].text,
      approvalTagType: statusMap[approvalStatus].tagType
    }
  }
  throw new Error(`产品列表第 ${index + 1} 行 status 未知：${value}`)
}

const resolveLatestNarration = (product: Record<string, unknown>, index: number): LatestNarrationSummary | null => {
  const narrationValue = resolveOptionalField(product, ['latestNarration'])
  if (narrationValue === undefined) {
    return null
  }
  if (narrationValue === null) {
    return null
  }
  const narration = requireObject(narrationValue, index, 'latestNarration')
  const audioReady = resolveBooleanField(narration, ['audioReady'], 'latestNarration.audioReady', index)
  const audioUrl = resolveOptionalStringField(narration, ['audioUrl'], 'latestNarration.audioUrl', index)
  const voice = resolveOptionalStringField(narration, ['voice'], 'latestNarration.voice', index)
  if (audioReady && !audioUrl) {
    throw new Error(`产品列表第 ${index + 1} 行字段为空：latestNarration.audioUrl`)
  }
  return {
    narrationVersionId: resolveStringField(
      narration,
      ['narrationVersionId'],
      'latestNarration.narrationVersionId',
      index
    ),
    language: resolveStringField(narration, ['language'], 'latestNarration.language', index),
    audienceType: resolveStringField(
      narration,
      ['audienceType'],
      'latestNarration.audienceType',
      index
    ),
    status: resolveStringField(narration, ['status'], 'latestNarration.status', index),
    live: resolveBooleanField(narration, ['live'], 'latestNarration.live', index),
    audioReady,
    audioUrl,
    voice
  }
}

const resolveNarrationAvailabilities = (
  revision: Record<string, unknown>,
  index: number
): NarrationAvailabilitySummary[] => {
  const narrationsValue = resolveOptionalField(revision, ['narrations'])
  if (narrationsValue === undefined || narrationsValue === null) {
    return []
  }
  if (!Array.isArray(narrationsValue)) {
    throw new Error(`产品列表第 ${index + 1} 行字段类型错误：narrations`)
  }
  return narrationsValue.map((item, narrationIndex) => {
    const narration = requireObject(item, index, `narrations[${narrationIndex}]`)
    return {
      language: resolveStringField(
        narration,
        ['language'],
        `narrations[${narrationIndex}].language`,
        index
      ),
      audienceType: resolveStringField(
        narration,
        ['audienceType'],
        `narrations[${narrationIndex}].audienceType`,
        index
      ),
      status: resolveStringField(
        narration,
        ['status'],
        `narrations[${narrationIndex}].status`,
        index
      ),
      live: resolveBooleanField(narration, ['live'], `narrations[${narrationIndex}].live`, index),
      audioReady: resolveBooleanField(
        narration,
        ['audioReady'],
        `narrations[${narrationIndex}].audioReady`,
        index
      )
    }
  })
}

const resolveContentStatusItems = (
  fields: Record<string, unknown> | null,
  displayRevision: Record<string, unknown>,
  latestNarration: LatestNarrationSummary | null,
  index: number
) => {
  const narrations = resolveNarrationAvailabilities(displayRevision, index)
  const zhAudioReady =
    narrations.some((item) => item.language === 'ZH' && item.audioReady) ||
    Boolean(latestNarration?.language === 'ZH' && latestNarration.audioReady)
  const enAudioReady = narrations.some((item) => item.language === 'EN' && item.audioReady)
  const zhCountriesOnSaleReady = Boolean(
    resolveOptionalStringField(fields || {}, ['target_market'], 'target_market', index)
  )
  const enCountriesOnSaleReady = Boolean(
    resolveOptionalStringField(
      fields || {},
      ['target_market_en'],
      'target_market_en',
      index
    )
  )

  const items: ProductStatusItem[] = [
    {
      key: 'zh_countries_on_sale',
      shortLabel: '在售国家',
      ready: zhCountriesOnSaleReady
    },
    {
      key: 'en_countries_on_sale',
      shortLabel: '在售国家(英)',
      ready: enCountriesOnSaleReady
    },
    {
      key: 'zh_audio',
      shortLabel: '中音频',
      ready: zhAudioReady
    },
    {
      key: 'en_audio',
      shortLabel: '英音频',
      ready: enAudioReady
    }
  ]

  return {
    items
  }
}

const resolveActiveAssignment = (
  revision: Record<string, unknown>,
  index: number
): ActiveAssignmentSummary | null => {
  const assignmentValue = resolveOptionalField(revision, ['activeAssignment'])
  if (assignmentValue === undefined || assignmentValue === null) {
    return null
  }
  const assignment = requireObject(assignmentValue, index, 'activeAssignment')
  return {
    assignmentId: resolveStringField(assignment, ['assignmentId'], 'activeAssignment.assignmentId', index),
    assigneeUserId: resolveStringField(
      assignment,
      ['assigneeUserId'],
      'activeAssignment.assigneeUserId',
      index
    ),
    status: resolveStringField(assignment, ['status'], 'activeAssignment.status', index)
  }
}

const normalizeProductRows = (products: unknown[]): ProductListRow[] => {
  if (!Array.isArray(products)) {
    throw new Error('产品列表数据必须是数组')
  }
  const userLabelMap = new Map(
    (props.userOptions || []).map((user) => [String(user.id), `${user.nickname} / ${user.username}`])
  )

  return products.map((value, index) => {
    const product = requireObject(value, index, '')
    const revision = resolveRevision(product, index)
    const displayRevision = resolveDisplayRevision(product, index)
    const incomplete = resolveIncompleteStatus(product, index)
    const latestFields = resolveRevisionFields(
      revision,
      index,
      incomplete.incompleteStatus === 'INCOMPLETE'
    )
    const fields = resolveRevisionFields(
      displayRevision,
      index,
      incomplete.incompleteStatus === 'INCOMPLETE'
    )
    const ownerCompanyId = resolveOwnerCompanyId(fields, incomplete.incompleteStatus === 'INCOMPLETE', index)
    const coverImageUrl = resolveCoverImageUrl(latestFields, fields, index)
    const ownerType = resolveProductOwnerType(fields, incomplete.incompleteStatus === 'INCOMPLETE', index)
    const lifecycle = resolveLifecycleStage(fields, incomplete.incompleteStatus === 'INCOMPLETE', index)
    const approval = resolveApprovalStatus(revision, index)
    const activeAssignment = resolveActiveAssignment(revision, index)
    const latestNarration = resolveLatestNarration(product, index)
    const contentStatus = resolveContentStatusItems(fields, displayRevision, latestNarration, index)
    const allowIncompleteFields = incomplete.incompleteStatus === 'INCOMPLETE'

    return {
      productId: resolveStringField(product, snapshotAliases.productId, 'productId', index),
      productCode: resolveStringField(product, snapshotAliases.productCode, 'productCode', index),
      legacyProductCode: resolveOptionalStringField(
        product,
        snapshotAliases.legacyProductCode,
        'legacyProductCode',
        index
      ),
      currentRevisionId: resolveStringField(
        product,
        snapshotAliases.currentRevisionId,
        'currentRevisionId',
        index
      ),
      displayRevisionId: resolveStringField(
        displayRevision,
        displayRevisionAliases.revisionId,
        'displayRevision.revisionId',
        index
      ),
      revisionNo: `V${resolveStringField(displayRevision, displayRevisionAliases.revisionNo, 'revisionNo', index)}`,
      editable: resolveBooleanField(product, snapshotAliases.editable, 'editable', index),
      live: resolveBooleanField(product, snapshotAliases.live, 'live', index),
      nameCn: resolveBlankableStringField(displayRevision, displayRevisionAliases.nameCn, 'nameCn', index),
      nameEn: allowIncompleteFields
        ? resolveOptionalStringField(displayRevision, displayRevisionAliases.nameEn, 'nameEn', index)
        : resolveStringField(displayRevision, displayRevisionAliases.nameEn, 'nameEn', index),
      pipelineLayout: resolveOptionalStringField(fields ?? {}, ['pipeline_layout', 'pipelineLayout'], 'pipeline_layout', index),
      targetMarket: resolveOptionalStringField(fields ?? {}, ['target_market'], 'target_market', index),
      ownerCompanyId,
      coverImageUrl,
      activeAssignmentAssigneeLabel: activeAssignment
        ? userLabelMap.get(activeAssignment.assigneeUserId) || `用户 #${activeAssignment.assigneeUserId}`
        : '',
      hasNarrationScript: Boolean(latestNarration),
      latestNarrationAudioUrl: latestNarration?.audioUrl || '',
      latestNarrationVoice: latestNarration?.audioUrl ? latestNarration.voice : '',
      contentStatusItems: contentStatus.items,
      raw: product,
      ...ownerType,
      ...lifecycle,
      ...incomplete,
      ...approval
    }
  })
}

const normalizedRows = computed(() => normalizeProductRows(props.products))

const totalPages = computed(() => {
  if (props.pageTotal <= 0) {
    return 0
  }
  return Math.ceil(props.pageTotal / props.pageSize)
})

const emitSearch = () => {
  emit('search', {
    keyword: draftFilters.keyword.trim(),
    lifecycleStage: '',
    incompleteStatus: '',
    approvalStatus: ''
  })
}

const handlePageChange = (pageNo: number) => {
  emit('page-change', { pageNo, pageSize: props.pageSize })
}

const resolveProductRowClassName = () => ''

const resetFilters = () => {
  draftFilters.keyword = ''
  draftFilters.lifecycleStage = ''
  draftFilters.incompleteStatus = ''
  draftFilters.approvalStatus = ''
  emitSearch()
}
</script>

<style scoped>
.showroom-product-list {
  color: #172033;
}

.showroom-product-list__toolbar {
  display: grid;
  grid-template-columns: minmax(360px, 520px) minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.showroom-product-list__toolbar > * {
  min-width: 0;
}

.showroom-product-list__search-group {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.showroom-product-list__actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: nowrap;
  gap: 8px;
  min-width: 0;
  overflow-x: auto;
}

.showroom-product-list__search {
  min-width: 0;
}

.showroom-product-list__toolbar :deep(.el-input__wrapper) {
  min-height: 40px;
}

.showroom-product-list__actions :deep(.el-button) {
  min-height: 40px;
  padding: 0 12px;
  white-space: nowrap;
}

.showroom-product-list__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.showroom-product-list__task-banner {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  padding: 12px 14px;
  background: #f7f9fc;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-list__task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.showroom-product-list__task-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.showroom-product-list__task-title {
  color: #172033;
  font-size: 0.92rem;
  font-weight: 600;
}

.showroom-product-list__task-close {
  padding: 0;
  min-height: auto;
  font-size: 0.82rem;
}

.showroom-product-list__task-line {
  margin: 0;
  color: #4b5563;
  font-size: 0.82rem;
  line-height: 1.55;
}

.showroom-product-list__task-line--danger {
  color: #c0392b;
}

.showroom-product-list__table-shell {
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 0 0 8px 8px;
}

.showroom-product-list__table {
  width: 100%;
  font-size: 0.9rem;
}

.showroom-product-list__table :deep(.el-table__header th) {
  height: 46px;
  padding: 7px 10px;
  background: #f7f9fc;
  color: #263247;
}

.showroom-product-list__table :deep(.el-table__row) {
  min-height: 56px;
  height: auto;
}

.showroom-product-list__table :deep(.el-table__cell) {
  padding: 7px 10px;
  border-color: #edf1f6;
  vertical-align: top;
}

.showroom-product-list__table :deep(.el-table__row:hover > td.el-table__cell) {
  background: #fafcff;
}

.showroom-product-list__cover-cell {
  display: flex;
  align-items: center;
  min-height: 52px;
}

.showroom-product-list__cover {
  width: 52px;
  height: 52px;
  overflow: hidden;
  background: #f7f9fc;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
}

.showroom-product-list__cover-empty {
  color: #4b5563;
  white-space: nowrap;
}

.showroom-product-list__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  color: #4b5563;
  font-size: 13px;
  border-top: 1px solid #edf1f6;
}

.showroom-product-list__summary {
  white-space: nowrap;
}

.showroom-product-list__status-cell {
  min-width: 0;
}

.showroom-product-list__status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
}

.showroom-product-list__status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 0.78rem;
  line-height: 1.4;
  white-space: nowrap;
}

.showroom-product-list__status-pill--ready {
  color: #1f8f55;
  background: #f2fbf5;
  border-color: #b9e6c8;
}

.showroom-product-list__status-pill--missing {
  color: #c0392b;
  background: #fff4f2;
  border-color: #f0c2bb;
}

.showroom-product-list__status-marker {
  font-size: 0.72rem;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .showroom-product-list__toolbar {
    grid-template-columns: 1fr;
  }

  .showroom-product-list__actions {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .showroom-product-list__toolbar {
    grid-template-columns: 1fr;
  }

  .showroom-product-list__search-group {
    grid-template-columns: 1fr;
  }
}
</style>
