<template>
  <div class="showroom-company-version" v-loading="loading">
    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      type="error"
      :title="loadError"
    />

    <template v-else-if="current">
      <div class="showroom-company-version__toolbar">
        <div>
          <h3 class="showroom-company-version__title">公司版本</h3>
          <p class="showroom-company-version__subtitle">
            {{ current.displayName || '未命名公司' }}
          </p>
          <p class="showroom-company-version__subtitle showroom-company-version__subtitle--en">
            {{ current.displayNameEn || 'English company name not filled' }}
          </p>
          <p class="showroom-company-version__version-tip">
            当前版本：V{{ current.revisionNo }} · {{ current.live ? '当前生效版本' : '最新待生效版本' }}
          </p>
        </div>
        <div class="showroom-company-version__summary-tags">
          <el-tag :type="current.live ? 'success' : resolveCompanyStatusTagType(current.status)">
            {{ current.live ? '已发布' : resolveCompanyStatusText(current.status) }}
          </el-tag>
          <el-tag type="info">历史版本 {{ historyRows.length }}</el-tag>
        </div>
      </div>

      <el-alert
        v-if="!current.live"
        :closable="false"
        show-icon
        type="warning"
        title="当前还没有已发布的公司信息"
        description="通过编辑公司信息保存后会立即更新公司版本，不经过审批流。"
      />

      <section class="showroom-company-version__panel">
        <div class="showroom-company-version__panel-header">
          <div>
            <h4>版本历史</h4>
            <p class="showroom-company-version__panel-tip">
              当前页用于查看公司历史版本，并把旧版本复制为最新版本。
            </p>
          </div>
          <div class="showroom-company-version__summary-tags">
            <el-tag type="info">当前版本 V{{ current.revisionNo }}</el-tag>
          </div>
        </div>

        <el-alert
          v-if="historyLoadError"
          :closable="false"
          show-icon
          type="error"
          :title="historyLoadError"
        />

        <div v-else v-loading="historyLoading" class="showroom-company-version__history-table-shell">
          <el-table :data="historyRows" row-key="revisionId" empty-text="暂无历史版本">
            <el-table-column label="版本" width="110">
              <template #default="{ row }">
                <strong>V{{ row.revisionNo }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="resolveCompanyHistoryStatusTagType(row.status)">
                  {{ resolveCompanyHistoryStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="变更摘要" min-width="360">
              <template #default="{ row }">
                {{ resolveHistoryPreview(row) }}
              </template>
            </el-table-column>
            <el-table-column label="差异条数" width="100">
              <template #default="{ row }">
                {{ row.diffItems.length }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :loading="previewLoadingRevisionId === row.revisionId"
                  @click="handleViewHistoryRevision(row)"
                >
                  查看版本
                </el-button>
                <el-button
                  link
                  type="primary"
                  :disabled="current.revisionId === row.revisionId"
                  :loading="restoringRevisionId === row.revisionId"
                  @click="handleRestoreHistoryRevision(row)"
                >
                  复制为最新版本
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <el-dialog
        v-model="historyPreviewVisible"
        title="历史版本详情"
        width="1180px"
        destroy-on-close
      >
        <template v-if="historyPreviewRow && historyPreviewVersion">
          <div class="showroom-company-version__history-preview-meta">
            <el-tag type="info">V{{ historyPreviewRow.revisionNo }}</el-tag>
            <el-tag :type="resolveCompanyHistoryStatusTagType(historyPreviewRow.status)">
              {{ resolveCompanyHistoryStatusText(historyPreviewRow.status) }}
            </el-tag>
            <span class="showroom-company-version__history-preview-note">
              可先查看该版本内容，再决定是否复制为最新版本。
            </span>
          </div>

          <el-tabs
            v-model="historyPreviewLanguageTab"
            class="showroom-company-version__display-tabs"
          >
            <el-tab-pane
              v-for="language in displayLanguageTabs"
              :key="`history-${language.code}`"
              :label="language.label"
              :name="language.code"
            >
              <section class="showroom-company-version__panel">
                <div class="showroom-company-version__panel-header">
                  <div>
                    <h4>{{ language.code === 'zh' ? '公司内容' : 'Company Content' }}</h4>
                    <p
                      :class="resolveHistoryPreviewCompanyNameClass(language.code)"
                      data-company-history-display-name
                    >
                      {{ resolveHistoryPreviewCompanyName(language.code) }}
                    </p>
                  </div>
                </div>
                <div class="showroom-company-version__field-grid">
                  <article
                    class="showroom-company-version__field-card showroom-company-version__field-card--cover"
                  >
                    <h5>{{ language.code === 'zh' ? '公司封面' : 'Company Cover' }}</h5>
                    <el-image
                      v-if="historyPreviewVersion.image.contentImage.url"
                      :preview-src-list="historyPreviewCoverPreviewList"
                      :src="historyPreviewVersion.image.contentImage.url"
                      class="showroom-company-version__cover-image"
                      fit="cover"
                      preview-teleported
                    />
                    <div v-else class="showroom-company-version__cover-empty">
                      {{ language.code === 'zh' ? '未上传封面' : 'Cover image not uploaded' }}
                    </div>
                  </article>
                  <article
                    v-for="definition in companyFieldDefinitions"
                    :key="`history-${language.code}-${definition.key}`"
                    class="showroom-company-version__field-card"
                  >
                    <h5>{{ language.code === 'zh' ? definition.label : definition.labelEn }}</h5>
                    <p>{{ resolveHistoryPreviewFieldValue(definition.key, language.code) }}</p>
                  </article>
                </div>
                <article class="showroom-company-version__narration-card">
                  <h5>{{ language.code === 'zh' ? '讲解语音' : 'Narration Audio' }}</h5>
                  <template v-if="resolveHistoryPreviewNarration(language.code)">
                    <p>
                      语音版本：{{ resolveHistoryPreviewNarration(language.code)?.versionId ?? '未记录' }}
                    </p>
                    <p>Voice：{{ resolveHistoryPreviewNarration(language.code)?.voice || '未记录' }}</p>
                    <p class="showroom-company-version__narration-script">
                      {{
                        resolveHistoryPreviewNarration(language.code)?.scriptText ||
                        (language.code === 'zh' ? '当前没有讲解稿内容' : 'No narration script recorded')
                      }}
                    </p>
                    <audio
                      v-if="resolveHistoryPreviewNarration(language.code)?.audioUrl"
                      :src="resolveHistoryPreviewNarration(language.code)?.audioUrl || undefined"
                      controls
                      preload="none"
                    ></audio>
                    <span v-else class="showroom-company-version__audio-empty">
                      {{ language.code === 'zh' ? '未生成音频' : 'No audio generated' }}
                    </span>
                  </template>
                  <template v-else>
                    <p class="showroom-company-version__narration-script">
                      {{
                        language.code === 'zh' ? '当前没有讲解稿内容' : 'No narration script recorded'
                      }}
                    </p>
                    <span class="showroom-company-version__audio-empty">
                      {{ language.code === 'zh' ? '未生成音频' : 'No audio generated' }}
                    </span>
                  </template>
                </article>
              </section>
            </el-tab-pane>
          </el-tabs>
        </template>

        <template #footer>
          <el-button @click="historyPreviewVisible = false">关闭</el-button>
          <el-button
            type="primary"
            :disabled="!canRestorePreviewedRevision"
            :loading="historyPreviewRow ? restoringRevisionId === historyPreviewRow.revisionId : false"
            @click="historyPreviewRow && handleRestoreHistoryRevision(historyPreviewRow)"
          >
            复制为最新版本
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ShowroomAdminApi, type ShowroomCompanyRevisionRestoreReqVO } from '@/api/showroom-admin'
import { getVersionCenterDetail } from '@/api/showroom-admin/version-center'
import {
  formatHistoryDiffValue,
  normalizeCompanyHistory,
  resolveCompanyHistoryStatusTagType,
  resolveCompanyHistoryStatusText,
  type CompanyVersionHistory
} from '@/views/showroom-admin/history/contracts'
import {
  normalizeCompanyCurrent,
  resolveCompanyStatusTagType,
  resolveCompanyStatusText,
  companyFieldDefinitions,
  type ShowroomCompanyCurrent
} from '@/views/showroom-admin/company/contracts'
import {
  normalizeVersionCenterDetailResponse,
  type VersionCenterNarrationVO,
  type VersionCenterSnapshotVO
} from '@/views/showroom-admin/version-center/contracts'

defineOptions({ name: 'CompanyVersionWorkbench' })

type EditorLanguageCode = 'zh' | 'en'

const message = useMessage()
const loading = ref(false)
const historyLoading = ref(false)
const loadError = ref('')
const historyLoadError = ref('')
const current = ref<ShowroomCompanyCurrent | null>(null)
const historyRows = ref<CompanyVersionHistory[]>([])
const previewLoadingRevisionId = ref<number | null>(null)
const restoringRevisionId = ref<number | null>(null)
const historyPreviewVisible = ref(false)
const historyPreviewLanguageTab = ref<EditorLanguageCode>('zh')
const historyPreviewRow = ref<CompanyVersionHistory | null>(null)
const historyPreviewVersion = ref<VersionCenterSnapshotVO | null>(null)

const versionCenterReleaseScope = {
  siteKey: 'yingtai-showroom',
  stage: 'TEST' as const
}

const resolveVersionCenterReleaseScope = () => {
  if (!versionCenterReleaseScope.siteKey || !versionCenterReleaseScope.stage) {
    throw new Error('版本中心缺少 scope：siteKey/stage')
  }
  return {
    siteKey: versionCenterReleaseScope.siteKey,
    stage: versionCenterReleaseScope.stage
  }
}

const displayLanguageTabs = [
  { code: 'zh', label: '中文' },
  { code: 'en', label: 'English' }
] as const

const historyPreviewCoverPreviewList = computed(() =>
  historyPreviewVersion.value?.image.contentImage.url
    ? [historyPreviewVersion.value.image.contentImage.url]
    : []
)

const canRestorePreviewedRevision = computed(
  () =>
    Boolean(
      historyPreviewRow.value?.revisionId &&
        current.value?.revisionId &&
        historyPreviewRow.value.revisionId !== current.value.revisionId
    )
)

const clearHistoryPreview = () => {
  historyPreviewVisible.value = false
  historyPreviewRow.value = null
  historyPreviewVersion.value = null
  historyPreviewLanguageTab.value = 'zh'
}

const resolveHistoryPreviewCompanyName = (languageCode: EditorLanguageCode) =>
  languageCode === 'zh'
    ? historyPreviewVersion.value?.title || '未命名公司'
    : historyPreviewVersion.value?.titleEn || 'English company name not filled'

const resolveHistoryPreviewCompanyNameClass = (languageCode: EditorLanguageCode) =>
  languageCode === 'zh'
    ? 'showroom-company-version__subtitle'
    : 'showroom-company-version__subtitle showroom-company-version__subtitle--en'

const resolveHistoryPreviewFieldValue = (fieldKey: string, languageCode: EditorLanguageCode) => {
  if (!historyPreviewVersion.value) {
    return ''
  }
  const field = historyPreviewVersion.value.fields.find((item) => item.fieldCode === fieldKey)
  if (languageCode === 'zh') {
    return field?.valueZh || '暂无中文内容'
  }
  return field?.valueEn || 'English description not filled'
}

const resolveHistoryPreviewNarration = (
  languageCode: EditorLanguageCode
): VersionCenterNarrationVO | null => {
  if (!historyPreviewVersion.value) {
    return null
  }
  const narrationLanguage = languageCode === 'zh' ? 'ZH' : 'EN'
  return (
    historyPreviewVersion.value.narrations.find((item) => item.language === narrationLanguage) || null
  )
}

const resolveHistoryPreview = (row: CompanyVersionHistory) => {
  const firstItem = row.diffItems[0]
  if (!firstItem) {
    return '该版本没有字段差异记录'
  }
  return `${firstItem.label} · ${formatHistoryDiffValue(firstItem.oldValue)} -> ${formatHistoryDiffValue(firstItem.newValue)}`
}

const loadCompanyHistory = async (companyId: number) => {
  historyLoading.value = true
  historyLoadError.value = ''
  try {
    historyRows.value = normalizeCompanyHistory(
      await ShowroomAdminApi.getCompanyHistory({ id: companyId })
    )
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    historyRows.value = []
    historyLoadError.value = resolved.message
  } finally {
    historyLoading.value = false
  }
}

const loadCompanyCurrent = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const nextCurrent = normalizeCompanyCurrent(await ShowroomAdminApi.getCompanyCurrent())
    current.value = nextCurrent
    clearHistoryPreview()
    if (nextCurrent.companyId > 0) {
      await loadCompanyHistory(nextCurrent.companyId)
    } else {
      historyRows.value = []
      historyLoadError.value = ''
    }
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    current.value = null
    historyRows.value = []
    historyLoadError.value = ''
    clearHistoryPreview()
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const handleViewHistoryRevision = async (revision: CompanyVersionHistory) => {
  if (!current.value?.companyId) {
    throw new Error('公司版本页尚未加载完成')
  }
  previewLoadingRevisionId.value = revision.revisionId
  try {
    const detail = normalizeVersionCenterDetailResponse(
      await getVersionCenterDetail({
        ...resolveVersionCenterReleaseScope(),
        targetType: 'COMPANY',
        targetId: current.value.companyId,
        revisionId: revision.revisionId
      })
    )
    historyPreviewVersion.value = detail.selectedVersion
    historyPreviewRow.value = revision
    historyPreviewLanguageTab.value = 'zh'
    historyPreviewVisible.value = true
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    previewLoadingRevisionId.value = null
  }
}

const handleRestoreHistoryRevision = async (revision: CompanyVersionHistory) => {
  if (!current.value?.companyId) {
    throw new Error('公司版本页尚未加载完成')
  }
  if (current.value.revisionId === revision.revisionId) {
    return
  }
  try {
    await message.confirm(`确认将版本 V${revision.revisionNo} 复制为最新版本吗？`)
  } catch {
    return
  }
  restoringRevisionId.value = revision.revisionId
  try {
    const restored = normalizeCompanyCurrent(
      await ShowroomAdminApi.restoreCompanyRevision({
        companyId: current.value.companyId,
        sourceRevisionId: revision.revisionId
      } as ShowroomCompanyRevisionRestoreReqVO)
    )
    clearHistoryPreview()
    await loadCompanyCurrent()
    message.success(`版本 V${revision.revisionNo} 已复制为最新版本 V${restored.revisionNo}`)
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    restoringRevisionId.value = null
  }
}

onMounted(() => {
  void loadCompanyCurrent()
})
</script>

<style scoped>
.showroom-company-version {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-company-version__toolbar,
.showroom-company-version__panel {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-company-version__toolbar,
.showroom-company-version__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.showroom-company-version__title,
.showroom-company-version__panel h4 {
  margin: 0;
  color: #172033;
}

.showroom-company-version__title {
  font-size: 1.05rem;
}

.showroom-company-version__subtitle,
.showroom-company-version__version-tip,
.showroom-company-version__panel-tip {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-company-version__subtitle--en {
  color: #6b7280;
}

.showroom-company-version__version-tip {
  margin-top: 6px;
}

.showroom-company-version__panel-tip {
  line-height: 1.7;
}

.showroom-company-version__summary-tags,
.showroom-company-version__history-preview-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.showroom-company-version__history-table-shell {
  margin-top: 12px;
}

.showroom-company-version__display-tabs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-company-version__display-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.showroom-company-version__history-preview-meta {
  margin-bottom: 12px;
}

.showroom-company-version__history-preview-note {
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-company-version__field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 12px;
}

.showroom-company-version__field-card {
  min-height: 150px;
  padding: 14px 16px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-company-version__field-card h5 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 0.95rem;
}

.showroom-company-version__field-card p {
  margin: 0;
  color: #263247;
  line-height: 1.7;
  white-space: pre-wrap;
}

.showroom-company-version__narration-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 16px;
  padding: 14px 16px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-company-version__narration-card h5,
.showroom-company-version__narration-card p {
  margin: 0;
}

.showroom-company-version__narration-card p {
  color: #263247;
  line-height: 1.7;
}

.showroom-company-version__narration-script {
  white-space: pre-wrap;
}

.showroom-company-version__narration-card audio {
  width: 100%;
}

.showroom-company-version__audio-empty {
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-company-version__field-card--cover {
  grid-column: span 2;
}

.showroom-company-version__cover-image,
.showroom-company-version__cover-empty {
  width: 100%;
  min-height: 220px;
  border-radius: 8px;
}

.showroom-company-version__cover-image {
  overflow: hidden;
  border: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.showroom-company-version__cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  background: #f7f9fc;
  border: 1px dashed #dbe3ef;
}

@media (max-width: 960px) {
  .showroom-company-version__toolbar,
  .showroom-company-version__panel-header {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-company-version__field-grid {
    grid-template-columns: 1fr;
  }

  .showroom-company-version__field-card--cover {
    grid-column: auto;
  }
}
</style>
