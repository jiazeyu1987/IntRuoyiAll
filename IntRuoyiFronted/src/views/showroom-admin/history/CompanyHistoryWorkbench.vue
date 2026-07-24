<template>
  <div class="showroom-version-browser" v-loading="baseLoading || contentLoading">
    <el-alert
      v-if="baseLoadError"
      :closable="false"
      show-icon
      type="error"
      :title="baseLoadError"
    />

    <template v-else>
      <div class="showroom-version-browser__toolbar">
        <div>
          <h3 class="showroom-version-browser__title">版本浏览器</h3>
          <p class="showroom-version-browser__subtitle">
            {{ currentSubtitle }}
          </p>
        </div>

        <div class="showroom-version-browser__filters">
          <el-select v-model="filters.scope" class="showroom-version-browser__select" placeholder="目标类型">
            <el-option
              v-for="item in scopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <el-select
            v-if="filters.scope === 'PRODUCT'"
            v-model="filters.productId"
            class="showroom-version-browser__select"
            placeholder="目标对象"
          >
            <el-option
              v-for="item in productOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

          <template v-if="needsAssetTargetSelectors">
            <el-select
              v-model="filters.assetTargetType"
              class="showroom-version-browser__select"
              placeholder="目标对象"
            >
              <el-option
                v-for="item in assetTargetTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>

            <el-select
              v-model="filters.assetTargetId"
              class="showroom-version-browser__select"
              placeholder="目标实例"
            >
              <el-option
                v-for="item in currentAssetTargetOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </template>

          <el-select
            v-if="filters.scope === 'NARRATION'"
            v-model="filters.language"
            class="showroom-version-browser__select"
            placeholder="讲解语言"
          >
            <el-option
              v-for="item in narrationLanguageOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
      </div>

      <div class="showroom-version-browser__summary-card">
        <div class="showroom-version-browser__summary-title">
          {{ currentSummary.title }}
        </div>
        <div class="showroom-version-browser__summary-description">
          {{ currentSummary.description }}
        </div>
      </div>

      <div v-if="isHistoryScope" class="showroom-version-browser__table-shell">
        <el-alert
          v-if="contentLoadError"
          :closable="false"
          show-icon
          type="error"
          :title="contentLoadError"
        />

        <div v-else-if="historyRows.length === 0" class="showroom-version-browser__empty-shell">
          {{ historyEmptyText }}
        </div>

        <el-table v-else :data="historyRows" row-key="revisionId">
          <el-table-column label="对象" min-width="180">
            <template #default="{ row }">
              <strong>{{ row.targetLabel }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="110">
            <template #default="{ row }">
              <strong>V{{ row.revisionNo }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="resolveHistoryStatusTagType(row)">
                {{ resolveHistoryStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="差异预览" min-width="420">
            <template #default="{ row }">
              {{ resolveRevisionPreview(row) }}
            </template>
          </el-table-column>
          <el-table-column label="差异条数" width="120">
            <template #default="{ row }">
              {{ row.diffItems.length }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleOpenRevision(row)">查看差异</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else-if="filters.scope === 'NARRATION'" class="showroom-version-browser__snapshot-shell">
        <el-alert
          :closable="false"
          show-icon
          type="warning"
          title="后端未提供讲解历史列表接口"
        />
        <div class="showroom-version-browser__note">
          当前只支持读取最新讲解快照，无法浏览历史版本链路。
        </div>

        <el-alert
          v-if="contentLoadError"
          :closable="false"
          show-icon
          type="error"
          :title="contentLoadError"
        />

        <template v-else-if="narrationSnapshot">
          <el-descriptions :column="2" border class="showroom-version-browser__descriptions">
            <el-descriptions-item label="目标对象">
              {{ currentAssetTargetLabel }}
            </el-descriptions-item>
            <el-descriptions-item label="对象类型">
              {{ resolveTargetTypeText(narrationSnapshot.key.targetType) }}
            </el-descriptions-item>
            <el-descriptions-item label="版本">
              V{{ narrationSnapshot.versionNo }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="resolveNarrationStatusTagType(narrationSnapshot.status)">
                {{ resolveNarrationStatusText(narrationSnapshot.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="生成状态">
              <el-tag :type="resolveGenerationStatusTagType(narrationSnapshot.generationStatus)">
                {{ resolveGenerationStatusText(narrationSnapshot.generationStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="语言">
              {{ narrationSnapshot.key.language }}
            </el-descriptions-item>
            <el-descriptions-item label="来源版本">
              {{ narrationSnapshot.sourceRevisionId }}
            </el-descriptions-item>
            <el-descriptions-item label="音频文件">
              {{ narrationSnapshot.audioFileId || '未生成' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="showroom-version-browser__text-block">
            <span class="showroom-version-browser__block-label">讲解稿</span>
            <pre class="showroom-version-browser__script">{{ narrationSnapshot.scriptText || '无内容' }}</pre>
          </div>
        </template>

        <div v-else class="showroom-version-browser__empty-shell">
          当前目标暂无可读取的讲解快照
        </div>
      </div>

      <div v-else class="showroom-version-browser__snapshot-shell">
        <el-alert
          :closable="false"
          show-icon
          type="warning"
          title="后端未提供预览资产历史列表接口"
        />
        <div class="showroom-version-browser__note">
          当前只支持读取 live 预览资产快照，无法浏览历史版本链路。
        </div>

        <el-alert
          v-if="contentLoadError"
          :closable="false"
          show-icon
          type="error"
          :title="contentLoadError"
        />

        <template v-else-if="previewSnapshot">
          <el-descriptions :column="2" border class="showroom-version-browser__descriptions">
            <el-descriptions-item label="目标对象">
              {{ previewSnapshot.title }}
            </el-descriptions-item>
            <el-descriptions-item label="目标类型">
              {{ resolveTargetTypeText(previewSnapshot.targetType) }}
            </el-descriptions-item>
            <el-descriptions-item label="目标编号">
              {{ previewSnapshot.targetId }}
            </el-descriptions-item>
            <el-descriptions-item label="预览图">
              {{ previewSnapshot.previewImageUrl ? '已读取' : '未发布' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="showroom-version-browser__text-block">
            <span class="showroom-version-browser__block-label">说明</span>
            <p class="showroom-version-browser__note">
              {{ previewSnapshot.description }}
            </p>
          </div>

          <div v-if="previewSnapshot.previewImageUrl" class="showroom-version-browser__preview-card">
            <img
              :alt="previewSnapshot.title"
              :src="previewSnapshot.previewImageUrl"
              class="showroom-version-browser__preview-image"
            />
          </div>
        </template>

        <div v-else class="showroom-version-browser__empty-shell">
          当前目标暂无可读取的预览资产快照
        </div>
      </div>
    </template>

    <VersionDiffDrawer
      v-model="drawerVisible"
      :revision="activeRevision"
      :status-text="activeRevisionStatusText"
      :status-tag-type="activeRevisionStatusTagType"
    />
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { ShowroomFrontstageApi } from '@/api/showroom-frontstage'
import { normalizeCompanyCurrent, type ShowroomCompanyCurrent } from '@/views/showroom-admin/company/contracts'
import {
  buildTargetOptions,
  normalizeNarrationVersion,
  resolveGenerationStatusTagType,
  resolveGenerationStatusText,
  resolveNarrationStatusTagType,
  resolveNarrationStatusText,
  resolveTargetTypeText,
  type NarrationTargetOption,
  type ShowroomNarrationVersionRecord
} from '@/views/showroom-admin/narration/contracts'
import {
  normalizeProductHistory,
  resolveProductStatusTagType,
  resolveProductStatusText
} from '@/views/showroom-admin/product/contracts'
import VersionDiffDrawer from './VersionDiffDrawer.vue'
import {
  formatHistoryDiffValue,
  normalizeCompanyHistory,
  resolveCompanyHistoryStatusTagType,
  resolveCompanyHistoryStatusText,
  type VersionBrowserContentTargetType,
  type VersionBrowserScope,
  type VersionHistoryRecord,
  type VersionPreviewSnapshot
} from './contracts'

defineOptions({ name: 'CompanyHistoryWorkbench' })

const scopeOptions: Array<{ label: string; value: VersionBrowserScope }> = [
  { label: '公司内容', value: 'COMPANY' },
  { label: '产品内容', value: 'PRODUCT' },
  { label: '讲解资产', value: 'NARRATION' },
  { label: '预览资产', value: 'PREVIEW_ASSET' }
]

const assetTargetTypeOptions: Array<{ label: string; value: VersionBrowserContentTargetType }> = [
  { label: '公司', value: 'COMPANY' },
  { label: '产品', value: 'PRODUCT' },
  { label: '展柜', value: 'HALL' }
]

const narrationLanguageOptions = [
  { label: '中文', value: 'ZH' },
  { label: '英文', value: 'EN' }
] as const

const filters = reactive({
  scope: 'COMPANY' as VersionBrowserScope,
  productId: null as number | null,
  assetTargetType: 'COMPANY' as VersionBrowserContentTargetType,
  assetTargetId: null as number | null,
  language: 'ZH' as 'ZH' | 'EN'
})

const baseLoading = ref(false)
const contentLoading = ref(false)
const baseLoadError = ref('')
const contentLoadError = ref('')
const baseReady = ref(false)

const companyCurrent = ref<ShowroomCompanyCurrent | null>(null)
const companyCurrentRaw = ref<Record<string, unknown> | null>(null)
const productRows = ref<unknown[]>([])
const hallRows = ref<unknown[]>([])
const targetOptions = ref<Record<VersionBrowserContentTargetType, NarrationTargetOption[]>>({
  COMPANY: [],
  PRODUCT: [],
  HALL: []
})
const historyRows = ref<VersionHistoryRecord[]>([])
const narrationSnapshot = ref<ShowroomNarrationVersionRecord | null>(null)
const previewSnapshot = ref<VersionPreviewSnapshot | null>(null)
const drawerVisible = ref(false)
const activeRevision = ref<VersionHistoryRecord | null>(null)

const productOptions = computed(() => targetOptions.value.PRODUCT)
const currentAssetTargetOptions = computed(() => targetOptions.value[filters.assetTargetType])
const needsAssetTargetSelectors = computed(() => {
  return filters.scope === 'NARRATION' || filters.scope === 'PREVIEW_ASSET'
})
const isHistoryScope = computed(() => {
  return filters.scope === 'COMPANY' || filters.scope === 'PRODUCT'
})

const currentProductLabel = computed(() => {
  const option = productOptions.value.find((item) => item.value === filters.productId)
  return option?.label || '未选择产品'
})

const currentAssetTargetLabel = computed(() => {
  const option = currentAssetTargetOptions.value.find((item) => item.value === filters.assetTargetId)
  return option?.label || '未选择目标对象'
})

const currentSubtitle = computed(() => {
  if (baseLoading.value) {
    return '正在加载版本浏览上下文'
  }
  if (filters.scope === 'COMPANY') {
    return `${companyCurrent.value?.displayName || '当前公司'} · 公司内容版本链路`
  }
  if (filters.scope === 'PRODUCT') {
    return `${currentProductLabel.value} · 产品内容版本链路`
  }
  if (filters.scope === 'NARRATION') {
    return `${resolveTargetTypeText(filters.assetTargetType)} · 讲解快照`
  }
  return `${resolveTargetTypeText(filters.assetTargetType)} · 预览资产快照`
})

const currentSummary = computed(() => {
  if (filters.scope === 'COMPANY') {
    return {
      title: companyCurrent.value?.displayName || '公司内容',
      description: '公司与产品已经具备真实版本链路，版本页统一承接 diff 浏览。'
    }
  }
  if (filters.scope === 'PRODUCT') {
    return {
      title: currentProductLabel.value,
      description: '产品历史不再只藏在详情抽屉里，当前页直接承接产品 revision 与字段差异。'
    }
  }
  if (filters.scope === 'NARRATION') {
    return {
      title: `${currentAssetTargetLabel.value} · 讲解资产`,
      description: '当前只支持读取最新讲解快照，历史列表接口尚未补齐。'
    }
  }
  return {
    title: `${currentAssetTargetLabel.value} · 预览资产`,
    description: '当前只支持读取 live 预览资产快照，历史列表接口尚未补齐。'
  }
})

const historyEmptyText = computed(() => {
  if (filters.scope === 'COMPANY') {
    return '当前公司暂无可查看的版本历史'
  }
  if (productOptions.value.length === 0) {
    return '当前没有可浏览历史的产品'
  }
  return '当前产品暂无可查看的版本历史'
})

const resolveRecord = (value: unknown, fieldName: string): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜版本页缺少对象字段：${fieldName}`)
  }
  return value as Record<string, unknown>
}

const resolveArray = (value: unknown, fieldName: string) => {
  if (!Array.isArray(value)) {
    throw new Error(`展柜版本页缺少列表字段：${fieldName}`)
  }
  return value
}

const resolveError = (error: unknown) => {
  return error instanceof Error ? error : new Error(String(error))
}

const syncTargetOptions = () => {
  targetOptions.value = buildTargetOptions(companyCurrentRaw.value, hallRows.value, productRows.value)
}

const ensureDefaultSelections = () => {
  if (!productOptions.value.some((item) => item.value === filters.productId)) {
    filters.productId = productOptions.value[0]?.value ?? null
  }
  if (!currentAssetTargetOptions.value.some((item) => item.value === filters.assetTargetId)) {
    filters.assetTargetId = currentAssetTargetOptions.value[0]?.value ?? null
  }
}

const loadBaseContext = async () => {
  baseLoading.value = true
  baseLoadError.value = ''
  baseReady.value = false
  try {
    const currentPayload = await ShowroomAdminApi.getCompanyCurrent()
    companyCurrentRaw.value = resolveRecord(currentPayload, 'companyCurrent')
    companyCurrent.value = normalizeCompanyCurrent(currentPayload)
    productRows.value = resolveArray(
      await ShowroomAdminApi.getProductPage({ pageNo: 1, pageSize: 200, keyword: '' }),
      'productPage'
    )
    hallRows.value = resolveArray(
      await ShowroomAdminApi.getHallPage({ pageNo: 1, pageSize: 200, keyword: '' }),
      'hallPage'
    )
    syncTargetOptions()
    ensureDefaultSelections()
    baseReady.value = true
  } catch (error) {
    const resolved = resolveError(error)
    baseLoadError.value = resolved.message
    throw resolved
  } finally {
    baseLoading.value = false
  }
}

const mapCompanyHistoryRows = (
  rows: ReturnType<typeof normalizeCompanyHistory>,
  targetId: number,
  targetLabel: string
): VersionHistoryRecord[] => {
  return rows.map((row) => ({
    revisionId: row.revisionId,
    revisionNo: row.revisionNo,
    status: row.status,
    diffItems: row.diffItems,
    targetType: 'COMPANY',
    targetId,
    targetLabel
  }))
}

const mapProductHistoryRows = (
  rows: ReturnType<typeof normalizeProductHistory>,
  targetId: number,
  targetLabel: string
): VersionHistoryRecord[] => {
  return rows.map((row) => ({
    revisionId: row.revisionId,
    revisionNo: row.revisionNo,
    status: row.status,
    diffItems: row.diffItems,
    targetType: 'PRODUCT',
    targetId,
    targetLabel
  }))
}

const resolveHistoryStatusText = (row: VersionHistoryRecord) => {
  return row.targetType === 'PRODUCT'
    ? resolveProductStatusText(row.status)
    : resolveCompanyHistoryStatusText(row.status)
}

const resolveHistoryStatusTagType = (row: VersionHistoryRecord) => {
  return row.targetType === 'PRODUCT'
    ? resolveProductStatusTagType(row.status)
    : resolveCompanyHistoryStatusTagType(row.status)
}

const activeRevisionStatusText = computed(() => {
  return activeRevision.value ? resolveHistoryStatusText(activeRevision.value) : ''
})

const activeRevisionStatusTagType = computed(() => {
  return activeRevision.value ? resolveHistoryStatusTagType(activeRevision.value) : 'info'
})

const resolveRevisionPreview = (row: VersionHistoryRecord) => {
  const firstItem = row.diffItems[0]
  if (!firstItem) {
    return '该版本没有字段差异记录'
  }
  return `${firstItem.label} · ${firstItem.fieldCode} · ${formatHistoryDiffValue(firstItem.oldValue)} -> ${formatHistoryDiffValue(firstItem.newValue)} · ${firstItem.operatorAction} #${firstItem.operatorId} · ${firstItem.createdAt}`
}

const loadCompanyScope = async () => {
  if (!companyCurrent.value?.companyId) {
    historyRows.value = []
    return
  }
  historyRows.value = mapCompanyHistoryRows(
    normalizeCompanyHistory(await ShowroomAdminApi.getCompanyHistory({ id: companyCurrent.value.companyId })),
    companyCurrent.value.companyId,
    companyCurrent.value.displayName || '公司内容'
  )
}

const loadProductScope = async () => {
  if (!filters.productId) {
    historyRows.value = []
    return
  }
  historyRows.value = mapProductHistoryRows(
    normalizeProductHistory(await ShowroomAdminApi.getProductHistory(filters.productId)),
    filters.productId,
    currentProductLabel.value
  )
}

const loadNarrationScope = async () => {
  if (!filters.assetTargetId) {
    narrationSnapshot.value = null
    return
  }
  const version = await request.get({
    url: '/showroom/narration/get',
    params: {
      targetType: filters.assetTargetType,
      targetId: filters.assetTargetId,
      audienceType: 'PUBLIC',
      language: filters.language
    }
  })
  narrationSnapshot.value = normalizeNarrationVersion(version)
}

const extractPreviewImageUrl = (payload: unknown, fieldName: string) => {
  const record = resolveRecord(payload, fieldName)
  return typeof record.previewImageUrl === 'string' ? record.previewImageUrl : ''
}

const readProductPreviewImageUrl = async (productId: number) => {
  const payload = resolveRecord(await ShowroomFrontstageApi.getDisplayProduct(productId), 'displayProduct')
  return extractPreviewImageUrl(payload.productCard ?? {}, 'productCard')
}

const readHallPreviewImageUrl = async (hallId: number) => {
  const payload = resolveRecord(await ShowroomFrontstageApi.getDisplayHome(), 'displayHome')
  const hallEntries = resolveArray(payload.hallEntries ?? [], 'hallEntries')
  const entry = hallEntries.find((item) => {
    if (!item || typeof item !== 'object' || Array.isArray(item)) {
      return false
    }
    const record = item as Record<string, unknown>
    return Number(record.id) === hallId
  })
  if (!entry) {
    return ''
  }
  return extractPreviewImageUrl(entry, 'hallEntry')
}

const loadPreviewScope = async () => {
  if (!filters.assetTargetId) {
    previewSnapshot.value = null
    return
  }
  if (filters.assetTargetType === 'COMPANY') {
    previewSnapshot.value = {
      targetType: 'COMPANY',
      targetId: filters.assetTargetId,
      title: `${resolveTargetTypeText(filters.assetTargetType)}预览资产`,
      description: '当前只支持读取 live 预览资产快照，但公司级 previewImageUrl 与历史列表接口都未补齐。',
      previewImageUrl: ''
    }
    return
  }
  const previewImageUrl =
    filters.assetTargetType === 'PRODUCT'
      ? await readProductPreviewImageUrl(filters.assetTargetId)
      : await readHallPreviewImageUrl(filters.assetTargetId)
  previewSnapshot.value = {
    targetType: filters.assetTargetType,
    targetId: filters.assetTargetId,
    title: currentAssetTargetLabel.value,
    description: previewImageUrl
      ? '当前只支持读取 live 预览资产快照，历史列表接口尚未补齐。'
      : '当前只支持读取 live 预览资产快照，且该目标尚未发布可读取的预览图。',
    previewImageUrl
  }
}

const clearViewState = () => {
  historyRows.value = []
  narrationSnapshot.value = null
  previewSnapshot.value = null
  drawerVisible.value = false
  activeRevision.value = null
}

const loadScopeContent = async () => {
  if (!baseReady.value) {
    return
  }
  contentLoading.value = true
  contentLoadError.value = ''
  clearViewState()
  try {
    if (filters.scope === 'COMPANY') {
      await loadCompanyScope()
    } else if (filters.scope === 'PRODUCT') {
      await loadProductScope()
    } else if (filters.scope === 'NARRATION') {
      await loadNarrationScope()
    } else {
      await loadPreviewScope()
    }
  } catch (error) {
    const resolved = resolveError(error)
    contentLoadError.value = resolved.message
  } finally {
    contentLoading.value = false
  }
}

const handleOpenRevision = (revision: VersionHistoryRecord) => {
  activeRevision.value = revision
  drawerVisible.value = true
}

const bootstrap = async () => {
  try {
    await loadBaseContext()
    if (baseReady.value) {
      await loadScopeContent()
    }
  } catch {
    // loadBaseContext already recorded the fail-fast error state
  }
}

watch(
  currentAssetTargetOptions,
  () => {
    ensureDefaultSelections()
  },
  { immediate: true }
)

watch(
  productOptions,
  () => {
    ensureDefaultSelections()
  },
  { immediate: true }
)

watch(
  () => [filters.scope, filters.productId, filters.assetTargetType, filters.assetTargetId, filters.language] as const,
  () => {
    if (!baseReady.value) {
      return
    }
    void loadScopeContent()
  }
)

onMounted(() => {
  void bootstrap()
})
</script>

<style scoped>
.showroom-version-browser {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-version-browser__toolbar,
.showroom-version-browser__summary-card,
.showroom-version-browser__empty-shell,
.showroom-version-browser__table-shell,
.showroom-version-browser__snapshot-shell {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-browser__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.showroom-version-browser__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showroom-version-browser__select {
  min-width: 180px;
}

.showroom-version-browser__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-version-browser__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-version-browser__summary-title {
  color: #172033;
  font-size: 0.98rem;
  font-weight: 600;
}

.showroom-version-browser__summary-description,
.showroom-version-browser__empty-shell,
.showroom-version-browser__note {
  margin-top: 6px;
  color: #4b5563;
  font-size: 0.92rem;
}

.showroom-version-browser__descriptions {
  margin-top: 12px;
}

.showroom-version-browser__text-block {
  margin-top: 16px;
  padding: 14px 16px;
  background: #fafcff;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
}

.showroom-version-browser__block-label {
  display: block;
  margin-bottom: 8px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-version-browser__script {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: #172033;
  font-size: 0.9rem;
  line-height: 1.7;
}

.showroom-version-browser__preview-card {
  margin-top: 16px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-version-browser__preview-image {
  display: block;
  width: 100%;
  height: auto;
}

@media (max-width: 960px) {
  .showroom-version-browser__toolbar {
    flex-direction: column;
  }
}
</style>
