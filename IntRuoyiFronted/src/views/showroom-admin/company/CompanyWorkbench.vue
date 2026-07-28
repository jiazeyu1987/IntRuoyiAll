<template>
  <div class="showroom-company-workbench" v-loading="loading">
    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      type="error"
      :title="loadError"
    />

    <template v-else-if="current && form">
      <div class="showroom-company-workbench__toolbar">
        <div>
          <h3 class="showroom-company-workbench__title">公司信息</h3>
          <p
            :class="activeDisplayCompanyNameClass"
            data-company-display-name
            :data-company-display-language="activeDisplayLanguageTab"
          >
            {{ activeDisplayCompanyName }}
          </p>
          <p class="showroom-company-workbench__version-tip">
            当前版本：V{{ current.revisionNo }} · {{ current.live ? '当前生效版本' : '最新待生效版本' }}
          </p>
        </div>
        <div class="showroom-company-workbench__actions">
          <el-button
            :loading="downloadingAndroidClient"
            @click="handleDownloadAndroidClient"
          >
            <Icon icon="ep:download" />
            下载安卓客户端
          </el-button>
          <el-button type="primary" @click="openEditDialog">编辑公司</el-button>
          <el-button
            v-if="canPublishShowroomRelease"
            :loading="publishingShowroomRelease"
            @click="handlePublishShowroomRelease"
          >
            手动发布展厅
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="releasePublishError"
        :closable="false"
        class="showroom-company-workbench__error-panel"
        show-icon
        type="error"
        :title="releasePublishError"
      />

      <el-alert
        v-if="!current.live"
        :closable="false"
        show-icon
        type="warning"
        title="当前还没有已发布的公司信息"
        description="通过编辑弹框保存后会立即更新公司信息，不经过审批流。"
      />

      <el-tabs v-model="activeDisplayLanguageTab" class="showroom-company-workbench__display-tabs">
        <el-tab-pane
          v-for="language in displayLanguageTabs"
          :key="language.code"
          :label="language.label"
          :name="language.code"
        >
          <section class="showroom-company-workbench__panel">
            <div class="showroom-company-workbench__panel-header">
              <h4>{{ language.code === 'zh' ? '公司内容' : 'Company Content' }}</h4>
            </div>
            <div class="showroom-company-workbench__field-grid">
              <article
                class="showroom-company-workbench__field-card showroom-company-workbench__field-card--cover"
              >
                <h5>{{ language.code === 'zh' ? '公司封面' : 'Company Cover' }}</h5>
                <el-image
                  v-if="current.coverImage"
                  :preview-src-list="companyCoverPreviewList"
                  :src="current.coverImage"
                  class="showroom-company-workbench__cover-image"
                  fit="cover"
                  preview-teleported
                />
                <div v-else class="showroom-company-workbench__cover-empty">
                  {{ language.code === 'zh' ? '未上传封面' : 'Cover image not uploaded' }}
                </div>
              </article>
              <article
                v-for="definition in companyFieldDefinitions"
                :key="`${language.code}-${definition.key}`"
                class="showroom-company-workbench__field-card"
              >
                <h5>{{ language.code === 'zh' ? definition.label : definition.labelEn }}</h5>
                <p>
                  {{
                    language.code === 'zh'
                      ? current.fields[definition.key] || '暂无中文内容'
                      : current.fields[resolveCompanyEnglishFieldKey(definition.key)] ||
                        'English description not filled'
                  }}
                </p>
              </article>
            </div>
          </section>

          <section class="showroom-company-workbench__panel">
            <div class="showroom-company-workbench__panel-header">
              <h4>{{ language.code === 'zh' ? '语音介绍' : 'Narration' }}</h4>
              <el-tag :type="hasSavedBilingualNarration ? 'success' : 'warning'">
                {{
                  language.code === 'zh'
                    ? hasSavedBilingualNarration
                      ? '已保存双语音频'
                      : '未生成'
                    : hasSavedBilingualNarration
                      ? 'Bilingual audio saved'
                      : 'Not generated'
                }}
              </el-tag>
            </div>
            <p class="showroom-company-workbench__narration-summary">
              {{
                language.code === 'zh'
                  ? liveNarration.zhText || '当前还没有已保存的中文语音介绍。'
                  : liveNarration.enText || 'Current English narration has not been saved yet.'
              }}
            </p>
            <div class="showroom-company-workbench__audio-grid">
              <div class="showroom-company-workbench__audio-item">
                <span class="showroom-company-workbench__audio-label">
                  {{ language.code === 'zh' ? '中文音频' : 'English Audio' }}
                </span>
                <audio
                  v-if="language.code === 'zh' ? liveNarration.zhAudioUrl : liveNarration.enAudioUrl"
                  :src="language.code === 'zh' ? liveNarration.zhAudioUrl : liveNarration.enAudioUrl"
                  class="showroom-company-workbench__audio"
                  controls
                  preload="none"
                ></audio>
                <span v-else class="showroom-company-workbench__audio-empty">
                  {{ language.code === 'zh' ? '未生成' : 'Not generated' }}
                </span>
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>

      <el-dialog
        v-model="dialogVisible"
        title="编辑公司信息"
        width="1280px"
        destroy-on-close
      >
        <el-alert
          v-if="companySaveError"
          :closable="false"
          class="showroom-company-workbench__error-panel"
          show-icon
          type="error"
          :title="companySaveError"
        />

        <el-tabs v-model="activeEditorLanguageTab" class="showroom-company-workbench__editor-tabs">
          <el-tab-pane
            v-for="language in editorLanguageTabs"
            :key="language.code"
            :label="language.label"
            :name="language.code"
          >
            <div class="showroom-company-workbench__dialog-grid">
              <CompanyProfileForm
                :form="form"
                :language="language.code"
                :translating-english-fields="translatingCompanyFields"
                :can-translate-english-fields="canTranslateCompanyFields"
                @update:form="handleFormChange"
                @translate-english="handleTranslateCompanyFields"
              />

              <section class="showroom-company-workbench__panel">
                <div class="showroom-company-workbench__panel-header">
                  <h4>{{ language.code === 'zh' ? '语音介绍' : 'Narration' }}</h4>
                  <el-tag
                    v-if="language.code === 'zh' ? zhNarrationDraft.voice : enNarrationDraft.voice"
                    effect="plain"
                    type="info"
                  >
                    {{ language.code === 'zh' ? zhNarrationDraft.voice : enNarrationDraft.voice }}
                  </el-tag>
                </div>
                <p class="showroom-company-workbench__panel-tip">
                  {{
                    language.code === 'zh'
                      ? '中文 tab 只负责中文语音介绍文本和中文音频。'
                      : 'The English tab only manages the English narration text and English audio. Translate from the Chinese tab first, then edit the English content manually if needed.'
                  }}
                </p>

                <el-form label-position="top">
                  <el-form-item v-if="language.code === 'zh'" label="目标字数">
                    <div class="showroom-company-workbench__script-toolbar">
                      <el-input-number
                        v-model="narrationTargetLength"
                        :min="1"
                        :step="10"
                        controls-position="right"
                      />
                      <el-button
                        :disabled="!canGenerateNarrationScript"
                        :loading="generatingNarrationScript"
                        type="primary"
                        @click="handleGenerateNarrationScript"
                      >
                        AI生成中文介绍
                      </el-button>
                    </div>
                  </el-form-item>
                  <el-form-item
                    :label="language.code === 'zh' ? '中文语音介绍' : 'English narration'"
                  >
                    <el-input
                      :model-value="language.code === 'zh' ? zhNarrationDraft.introText : enNarrationDraft.introText"
                      @update:model-value="
                        (value) =>
                          language.code === 'zh'
                            ? (zhNarrationDraft.introText = value)
                            : (enNarrationDraft.introText = value)
                      "
                      :rows="8"
                      :placeholder="resolveNarrationPlaceholder(language.code)"
                      type="textarea"
                    />
                  </el-form-item>
                </el-form>

                <el-alert
                  v-if="language.code === 'en' && englishTranslationStale"
                  :closable="false"
                  show-icon
                  type="warning"
                  title="English content may be outdated. Please translate again."
                />

                <div class="showroom-company-workbench__narration-actions">
                  <el-button
                    v-if="language.code === 'zh'"
                    :disabled="!canGenerateZhNarration"
                    :loading="generatingZhNarration"
                    type="primary"
                    @click="handleGenerateNarration('zh')"
                  >
                    生成中文音频
                  </el-button>
                  <el-button
                    v-else
                    :disabled="!canGenerateEnNarration"
                    :loading="generatingEnNarration"
                    type="primary"
                    @click="handleGenerateNarration('en')"
                  >
                    Generate English Audio
                  </el-button>
                </div>

                <div class="showroom-company-workbench__audio-grid">
                  <div class="showroom-company-workbench__audio-item">
                    <span class="showroom-company-workbench__audio-label">
                      {{ language.code === 'zh' ? '中文音频' : '英文音频' }}
                    </span>
                    <audio
                      v-if="language.code === 'zh' ? draftZhAudioUrl : draftEnAudioUrl"
                      :src="language.code === 'zh' ? draftZhAudioUrl : draftEnAudioUrl"
                      class="showroom-company-workbench__audio"
                      controls
                      preload="none"
                    ></audio>
                    <span v-else class="showroom-company-workbench__audio-empty">
                      {{ language.code === 'zh' ? '未生成' : 'Not generated' }}
                    </span>
                  </div>
                </div>
              </section>
            </div>
          </el-tab-pane>
        </el-tabs>

        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!canPublishCompany"
            :loading="publishing"
            @click="handlePublish"
          >
            保存
          </el-button>
        </template>
      </el-dialog>

    </template>
  </div>
</template>

<script setup lang="ts">
import {
  SHOWROOM_ANDROID_CLIENT_FILE_NAME,
  ShowroomAdminApi,
  type ShowroomCompanyNarrationGenerateRespVO,
  type ShowroomCompanyNarrationScriptGenerateRespVO,
  type ShowroomReleasePublishRespVO
} from '@/api/showroom-admin'
import { ShowroomFrontstageApi } from '@/api/showroom-frontstage'
import { useUserStore } from '@/store/modules/user'
import { downloadByData } from '@/utils/filt'
import { formatShowroomStructuredError } from '@/views/showroom-admin/shared/structuredError'
import CompanyProfileForm from './CompanyProfileForm.vue'
import {
  buildCompanyDraftPayload,
  companyFieldDefinitions,
  createCompanyDraftForm,
  hasCompanyDraftChanges,
  normalizeCompanyCurrent,
  normalizeCompanyFieldValue,
  resolveCompanyEnglishFieldKey,
  type CompanyDraftForm,
  type ShowroomCompanyCurrent
} from './contracts'

defineOptions({ name: 'ShowroomCompanyWorkbench' })

interface CompanyLiveNarrationState {
  zhText: string
  zhAudioUrl: string
  enText: string
  enAudioUrl: string
}

type EditorLanguageCode = 'zh' | 'en'

interface CompanyLanguageNarrationDraftState {
  introText: string
  generatedFromIntroText: string
  narrationVersionId: number | null
  audioUrl: string
  voice: string
}

const message = useMessage()
const userStore = useUserStore()
const releaseScope = {
  siteKey: 'yingtai-showroom',
  stage: 'TEST' as const
}
const SHOWROOM_PUBLICITY_ROLE_CODE = 'showroom_publicity'
const SUPER_ADMIN_ROLE_CODE = 'super_admin'
const loading = ref(false)
const publishing = ref(false)
const publishingShowroomRelease = ref(false)
const downloadingAndroidClient = ref(false)
const generatingNarrationScript = ref(false)
const generatingZhNarration = ref(false)
const generatingEnNarration = ref(false)
const translatingCompanyFields = ref(false)
const loadError = ref('')
const releasePublishError = ref('')
const companySaveError = ref('')
const dialogVisible = ref(false)
const narrationTargetLength = ref(180)
const displayLanguageTabs = [
  { code: 'zh', label: '中文' },
  { code: 'en', label: 'English' }
] as const
const activeDisplayLanguageTab = ref<(typeof displayLanguageTabs)[number]['code']>('zh')
const editorLanguageTabs = displayLanguageTabs
const activeEditorLanguageTab = ref<EditorLanguageCode>('zh')

const resolveNarrationPlaceholder = (languageCode: EditorLanguageCode) =>
  languageCode === 'zh'
    ? '请输入中文语音介绍，或点击 AI生成中文介绍 自动生成'
    : 'Click Translate English Content first, then adjust the English narration manually.'
const current = ref<ShowroomCompanyCurrent | null>(null)
const form = ref<CompanyDraftForm | null>(null)
const liveNarration = reactive<CompanyLiveNarrationState>({
  zhText: '',
  zhAudioUrl: '',
  enText: '',
  enAudioUrl: ''
})
const zhNarrationDraft = reactive<CompanyLanguageNarrationDraftState>({
  introText: '',
  generatedFromIntroText: '',
  narrationVersionId: null,
  audioUrl: '',
  voice: ''
})
const enNarrationDraft = reactive<CompanyLanguageNarrationDraftState>({
  introText: '',
  generatedFromIntroText: '',
  narrationVersionId: null,
  audioUrl: '',
  voice: ''
})
const englishTranslationSourceFingerprint = ref('')

const canPublishCompany = computed(() => {
  if (!current.value || !form.value) {
    return false
  }
  return hasCompanyDraftChanges(current.value, form.value) || canSaveNarration.value
})

const canPublishShowroomRelease = computed(() => {
  const roles = userStore.getRoles
  return roles.includes(SHOWROOM_PUBLICITY_ROLE_CODE) || roles.includes(SUPER_ADMIN_ROLE_CODE)
})

const activeDisplayCompanyName = computed(() =>
  activeDisplayLanguageTab.value === 'zh'
    ? current.value?.displayName || '未命名公司'
    : current.value?.displayNameEn || 'English company name not filled'
)

const activeDisplayCompanyNameClass = computed(() =>
  activeDisplayLanguageTab.value === 'zh'
    ? 'showroom-company-workbench__subtitle'
    : 'showroom-company-workbench__subtitle showroom-company-workbench__subtitle--en'
)

const normalizeShowroomReleasePublishResult = (
  value: unknown,
  fieldName: string
): ShowroomReleasePublishRespVO => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`展柜接口缺少对象字段：${fieldName}`)
  }
  const record = value as Record<string, unknown>
  const releaseId =
    record.releaseId === undefined || record.releaseId === null ? '' : String(record.releaseId).trim()
  if (!releaseId) {
    throw new Error(`展柜接口缺少文本字段：${fieldName}.releaseId`)
  }
  return {
    releaseId,
    manifestHash:
      record.manifestHash === undefined || record.manifestHash === null
        ? ''
        : String(record.manifestHash).trim(),
    rootDocumentId:
      record.rootDocumentId === undefined || record.rootDocumentId === null
        ? ''
        : String(record.rootDocumentId).trim(),
    documentCount:
      typeof record.documentCount === 'number' && Number.isFinite(record.documentCount)
        ? record.documentCount
        : 0,
    assetCount:
      typeof record.assetCount === 'number' && Number.isFinite(record.assetCount)
        ? record.assetCount
        : 0,
    installBytes:
      typeof record.installBytes === 'number' && Number.isFinite(record.installBytes)
        ? record.installBytes
        : 0,
    publishedAt:
      record.publishedAt === undefined || record.publishedAt === null ? '' : String(record.publishedAt).trim()
  }
}

const buildReleasePublishPayload = () => {
  if (!releaseScope.siteKey || !releaseScope.stage) {
    throw new Error('展厅发布缺少 scope：siteKey/stage')
  }
  return {
    siteKey: releaseScope.siteKey,
    stage: releaseScope.stage
  }
}

const canGenerateNarrationScript = computed(() => {
  return Boolean(
    current.value?.companyId &&
      current.value?.revisionId &&
      form.value &&
      normalizeCompanyFieldValue(form.value.companyType) &&
      normalizeCompanyFieldValue(form.value.displayName) &&
      Number.isInteger(narrationTargetLength.value) &&
      narrationTargetLength.value > 0
  )
})

const canTranslateCompanyFields = computed(() => {
  if (!form.value) {
    return false
  }
  return Boolean(
    normalizeCompanyFieldValue(zhNarrationDraft.introText) ||
      companyFieldDefinitions.some((definition) =>
        Boolean(normalizeCompanyFieldValue(form.value?.fields[definition.key]))
      )
  )
})

const canGenerateZhNarration = computed(() =>
  Boolean(
    current.value?.companyId &&
      current.value?.revisionId &&
      normalizeCompanyFieldValue(zhNarrationDraft.introText)
  )
)

const canGenerateEnNarration = computed(() =>
  Boolean(
    current.value?.companyId &&
      current.value?.revisionId &&
      normalizeCompanyFieldValue(enNarrationDraft.introText)
  )
)

const canSaveNarration = computed(() =>
  Boolean(zhNarrationDraft.narrationVersionId || enNarrationDraft.narrationVersionId)
)

const hasSavedBilingualNarration = computed(() => Boolean(liveNarration.zhAudioUrl && liveNarration.enAudioUrl))
const companyCoverPreviewList = computed(() =>
  current.value?.coverImage ? [current.value.coverImage] : []
)
const zhDraftMatchesLiveNarration = computed(
  () =>
    normalizeCompanyFieldValue(zhNarrationDraft.introText) ===
    normalizeCompanyFieldValue(liveNarration.zhText)
)
const enDraftMatchesLiveNarration = computed(
  () =>
    normalizeCompanyFieldValue(enNarrationDraft.introText) ===
    normalizeCompanyFieldValue(liveNarration.enText)
)
const draftZhAudioUrl = computed(() =>
  zhNarrationDraft.audioUrl || (zhDraftMatchesLiveNarration.value ? liveNarration.zhAudioUrl : '')
)
const draftEnAudioUrl = computed(() =>
  enNarrationDraft.audioUrl || (enDraftMatchesLiveNarration.value ? liveNarration.enAudioUrl : '')
)
const currentZhFingerprint = computed(() =>
  form.value ? buildEnglishTranslationFingerprint(form.value, zhNarrationDraft.introText) : ''
)
const hasEnglishComparableContent = computed(() =>
  Boolean(
    normalizeCompanyFieldValue(enNarrationDraft.introText) ||
      companyFieldDefinitions.some((definition) =>
        Boolean(normalizeCompanyFieldValue(form.value?.fields[resolveCompanyEnglishFieldKey(definition.key)]))
      )
  )
)
const englishTranslationStale = computed(
  () =>
    hasEnglishComparableContent.value &&
    Boolean(englishTranslationSourceFingerprint.value) &&
    englishTranslationSourceFingerprint.value !== currentZhFingerprint.value
)

const assignCurrent = (nextCurrent: ShowroomCompanyCurrent) => {
  current.value = nextCurrent
  form.value = createCompanyDraftForm(nextCurrent)
}

const buildEnglishTranslationFingerprint = (formState: CompanyDraftForm, zhIntroText: string) => {
  return JSON.stringify({
    introTextZh: normalizeCompanyFieldValue(zhIntroText),
    fields: Object.fromEntries(
      companyFieldDefinitions.map((definition) => [
        definition.key,
        normalizeCompanyFieldValue(formState.fields[definition.key])
      ])
    )
  })
}

const resetLanguageNarrationDraft = (
  draft: CompanyLanguageNarrationDraftState,
  text: string
) => {
  draft.introText = text
  draft.generatedFromIntroText = ''
  draft.narrationVersionId = null
  draft.audioUrl = ''
  draft.voice = ''
}

const resetNarrationDrafts = () => {
  resetLanguageNarrationDraft(zhNarrationDraft, liveNarration.zhText)
  resetLanguageNarrationDraft(enNarrationDraft, liveNarration.enText)
  englishTranslationSourceFingerprint.value = currentZhFingerprint.value
}

const resetLiveNarration = () => {
  liveNarration.zhText = ''
  liveNarration.zhAudioUrl = ''
  liveNarration.enText = ''
  liveNarration.enAudioUrl = ''
}

const handleFormChange = (nextForm: CompanyDraftForm) => {
  form.value = nextForm
}

const buildCompanyTranslationSourceFields = (formState: CompanyDraftForm) => {
  return Object.fromEntries(
    companyFieldDefinitions.map((definition) => [
      definition.key,
      normalizeCompanyFieldValue(formState.fields[definition.key])
    ])
  )
}

const buildCompanyTranslationPayload = (state: { current: ShowroomCompanyCurrent; form: CompanyDraftForm }) => {
  return {
    companyId: state.current.companyId,
    fieldCodes: companyFieldDefinitions.map((definition) => definition.key),
    fields: buildCompanyTranslationSourceFields(state.form),
    introTextZh: normalizeCompanyFieldValue(zhNarrationDraft.introText)
  }
}

const fetchDisplayNarrationByLanguage = async (companyId: number, language: 'ZH' | 'EN') => {
  try {
    const payload = (await ShowroomFrontstageApi.getDisplayNarration({
      targetType: 'COMPANY',
      targetId: companyId,
      audienceType: 'PUBLIC',
      language
    })) as { text?: string; audioUrl?: string }
    return payload
  } catch {
    return null
  }
}

const loadLiveNarration = async (companyId: number) => {
  resetLiveNarration()
  const [zhPayload, enPayload] = await Promise.all([
    fetchDisplayNarrationByLanguage(companyId, 'ZH'),
    fetchDisplayNarrationByLanguage(companyId, 'EN')
  ])
  liveNarration.zhText = zhPayload?.text || ''
  liveNarration.zhAudioUrl = zhPayload?.audioUrl || ''
  liveNarration.enText = enPayload?.text || ''
  liveNarration.enAudioUrl = enPayload?.audioUrl || ''
}

const loadCompanyCurrent = async (options: { preserveNarrationDraft?: boolean } = {}) => {
  loading.value = true
  loadError.value = ''
  try {
    const nextCurrent = normalizeCompanyCurrent(await ShowroomAdminApi.getCompanyCurrent())
    assignCurrent(nextCurrent)
    if (nextCurrent.companyId > 0 && nextCurrent.live) {
      await loadLiveNarration(nextCurrent.companyId)
    } else {
      resetLiveNarration()
    }
    if (!options.preserveNarrationDraft) {
      resetNarrationDrafts()
    }
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    current.value = null
    form.value = null
    resetLiveNarration()
    resetNarrationDrafts()
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const requireState = () => {
  if (!current.value || !form.value) {
    throw new Error('公司工作台尚未加载完成')
  }
  return { current: current.value, form: form.value }
}

const openEditDialog = () => {
  if (current.value) {
    form.value = createCompanyDraftForm(current.value)
  }
  companySaveError.value = ''
  narrationTargetLength.value = 180
  activeEditorLanguageTab.value = 'zh'
  resetNarrationDrafts()
  dialogVisible.value = true
}

const handleDownloadAndroidClient = async () => {
  downloadingAndroidClient.value = true
  try {
    const data = await ShowroomAdminApi.downloadAndroidClient()
    downloadByData(data, SHOWROOM_ANDROID_CLIENT_FILE_NAME, 'application/vnd.android.package-archive')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '安卓客户端下载')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    downloadingAndroidClient.value = false
  }
}

const handlePublishShowroomRelease = async () => {
  try {
    await message.confirm('确认立即发布当前展厅内容吗？')
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    throw error
  }

  publishingShowroomRelease.value = true
  releasePublishError.value = ''
  try {
    const result = normalizeShowroomReleasePublishResult(
      await ShowroomAdminApi.publishRelease(buildReleasePublishPayload()),
      'showroomReleasePublishResult'
    )
    const releaseId = result.releaseId
    message.alertSuccess(`展厅发布成功：${releaseId}`)
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '展厅发布')
    releasePublishError.value = formatted
    message.alertError(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    publishingShowroomRelease.value = false
  }
}

const handleTranslateCompanyFields = async () => {
  translatingCompanyFields.value = true
  try {
    const state = requireState()
    const response = await ShowroomAdminApi.translateCompanyFieldsToEn(
      buildCompanyTranslationPayload(state)
    )
    form.value = {
      ...state.form,
      fields: {
        ...state.form.fields,
        ...response.translatedFields
      }
    }
    enNarrationDraft.introText = response.introTextEn
    enNarrationDraft.generatedFromIntroText = ''
    enNarrationDraft.narrationVersionId = null
    enNarrationDraft.audioUrl = ''
    enNarrationDraft.voice = ''
    englishTranslationSourceFingerprint.value = currentZhFingerprint.value
    message.success('英文卡片和英文介绍已按当前中文内容回填，可继续手工修改')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '公司翻译')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    translatingCompanyFields.value = false
  }
}

const applyGeneratedNarrationDraft = (
  draft: CompanyLanguageNarrationDraftState,
  response: ShowroomCompanyNarrationGenerateRespVO
) => {
  draft.generatedFromIntroText = response.scriptText
  draft.narrationVersionId = response.narration.narrationVersionId
  draft.audioUrl = response.narration.audioUrl
  draft.voice = response.voice
}

const applyGeneratedNarrationScript = (response: ShowroomCompanyNarrationScriptGenerateRespVO) => {
  zhNarrationDraft.introText = response.introTextZh
  zhNarrationDraft.generatedFromIntroText = ''
  zhNarrationDraft.narrationVersionId = null
  zhNarrationDraft.audioUrl = ''
  zhNarrationDraft.voice = ''
}

const invalidateGeneratedNarrationDraft = (draft: CompanyLanguageNarrationDraftState) => {
  draft.narrationVersionId = null
  draft.audioUrl = ''
  draft.voice = ''
}

const handleGenerateNarrationScript = async () => {
  generatingNarrationScript.value = true
  try {
    const state = requireState()
    if (!state.current.revisionId) {
      throw new Error('当前公司版本尚未发布，无法生成介绍')
    }
    const response = await ShowroomAdminApi.generateCompanyNarrationScript({
      companyId: state.current.companyId,
      sourceRevisionId: state.current.revisionId,
      companyType: normalizeCompanyFieldValue(state.form.companyType),
      displayName: normalizeCompanyFieldValue(state.form.displayName),
      fields: Object.fromEntries(
        companyFieldDefinitions.map((item) => [
          item.key,
          normalizeCompanyFieldValue(state.form.fields[item.key])
        ])
      ),
      targetLength: narrationTargetLength.value
    })
    applyGeneratedNarrationScript(response)
    message.success('中文介绍已生成，可继续微调后再生成中文音频')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, 'AI中文介绍')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    generatingNarrationScript.value = false
  }
}

const handleGenerateNarration = async (language: EditorLanguageCode) => {
  const generatingRef = language === 'zh' ? generatingZhNarration : generatingEnNarration
  generatingRef.value = true
  try {
    const state = requireState()
    if (!state.current.revisionId) {
      throw new Error('当前公司版本尚未发布，无法生成音频')
    }
    const draft = language === 'zh' ? zhNarrationDraft : enNarrationDraft
    const response = await ShowroomAdminApi.generateCompanyNarrationAudio({
      companyId: state.current.companyId,
      sourceRevisionId: state.current.revisionId,
      language: language === 'zh' ? 'ZH' : 'EN',
      scriptText: normalizeCompanyFieldValue(draft.introText)
    })
    applyGeneratedNarrationDraft(draft, response)
    message.success(language === 'zh' ? '中文音频已生成，可先试听再保存' : '英文音频已生成，可先试听再保存')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, language === 'zh' ? '中文音频生成' : '英文音频生成')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    generatingRef.value = false
  }
}

const publishNarrationDraftIfNeeded = async () => {
  if (!canSaveNarration.value) {
    return []
  }
  const response = await ShowroomAdminApi.publishCompanyNarration({
    zhNarrationVersionId: zhNarrationDraft.narrationVersionId,
    enNarrationVersionId: enNarrationDraft.narrationVersionId
  })
  return [response.zhNarrationVersionId ? 'ZH' : null, response.enNarrationVersionId ? 'EN' : null].filter(
    (value): value is 'ZH' | 'EN' => Boolean(value)
  )
}

const handlePublish = async () => {
  publishing.value = true
  let publishedLanguages: Array<'ZH' | 'EN'> = []
  companySaveError.value = ''
  try {
    const state = requireState()
    const hasCompanyChanges = hasCompanyDraftChanges(state.current, state.form)
    if (hasCompanyChanges) {
      await ShowroomAdminApi.publishCompany(buildCompanyDraftPayload(state.current, state.form))
    }
    publishedLanguages = await publishNarrationDraftIfNeeded()
    dialogVisible.value = false
    await loadCompanyCurrent()
    message.success(
      hasCompanyChanges && publishedLanguages.length
        ? '公司信息和语音已保存'
        : publishedLanguages.length
          ? '公司语音已保存'
          : '公司信息已保存'
    )
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '公司保存')
    companySaveError.value = formatted
    if (publishedLanguages.length && current.value?.companyId) {
      await loadLiveNarration(current.value.companyId)
      resetNarrationDrafts()
      message.error(`公司信息已保存，但语音状态刷新失败：${formatted}`)
      throw (error instanceof Error ? error : new Error(formatted))
    }
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    publishing.value = false
  }
}

watch(
  () => zhNarrationDraft.introText,
  (introTextZh) => {
    if (
      zhNarrationDraft.generatedFromIntroText &&
      normalizeCompanyFieldValue(introTextZh) !==
        normalizeCompanyFieldValue(zhNarrationDraft.generatedFromIntroText)
    ) {
      invalidateGeneratedNarrationDraft(zhNarrationDraft)
    }
  }
)

watch(
  () => enNarrationDraft.introText,
  (introTextEn) => {
    if (
      enNarrationDraft.generatedFromIntroText &&
      normalizeCompanyFieldValue(introTextEn) !==
        normalizeCompanyFieldValue(enNarrationDraft.generatedFromIntroText)
    ) {
      invalidateGeneratedNarrationDraft(enNarrationDraft)
    }
  }
)

onMounted(() => {
  void loadCompanyCurrent()
})
</script>

<style scoped>
.showroom-company-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-company-workbench__toolbar,
.showroom-company-workbench__panel {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-company-workbench__toolbar,
.showroom-company-workbench__panel-header,
.showroom-company-workbench__actions,
.showroom-company-workbench__narration-actions,
.showroom-company-workbench__script-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.showroom-company-workbench__toolbar,
.showroom-company-workbench__panel-header {
  justify-content: space-between;
}

.showroom-company-workbench__title,
.showroom-company-workbench__panel h4 {
  margin: 0;
  color: #172033;
}

.showroom-company-workbench__title {
  font-size: 1.05rem;
}

.showroom-company-workbench__subtitle,
.showroom-company-workbench__version-tip,
.showroom-company-workbench__panel-tip,
.showroom-company-workbench__narration-summary {
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-company-workbench__subtitle {
  margin: 4px 0 0;
}

.showroom-company-workbench__error-panel {
  white-space: pre-line;
}

.showroom-company-workbench__version-tip {
  margin: 6px 0 0;
}

.showroom-company-workbench__subtitle--en {
  color: #6b7280;
}

.showroom-company-workbench__display-tabs {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-company-workbench__display-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.showroom-company-workbench__editor-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.showroom-company-workbench__panel-tip,
.showroom-company-workbench__narration-summary {
  margin: 10px 0 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.showroom-company-workbench__field-grid,
.showroom-company-workbench__dialog-grid,
.showroom-company-workbench__audio-grid {
  display: grid;
  gap: 16px;
}

.showroom-company-workbench__field-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.showroom-company-workbench__dialog-grid {
  grid-template-columns: minmax(0, 1.3fr) minmax(360px, 0.9fr);
}

.showroom-company-workbench__field-card {
  min-height: 150px;
  padding: 14px 16px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.showroom-company-workbench__field-card h5 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 0.95rem;
}

.showroom-company-workbench__field-card--cover {
  grid-column: span 2;
}

.showroom-company-workbench__field-card p {
  margin: 0;
  color: #263247;
  line-height: 1.7;
  white-space: pre-wrap;
}

.showroom-company-workbench__cover-image,
.showroom-company-workbench__cover-empty {
  width: 100%;
  min-height: 220px;
  border-radius: 8px;
}

.showroom-company-workbench__cover-image {
  overflow: hidden;
  border: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.showroom-company-workbench__cover-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
  background: #f7f9fc;
  border: 1px dashed #dbe3ef;
}

.showroom-company-workbench__narration-actions {
  flex-wrap: wrap;
  margin-top: 12px;
}

.showroom-company-workbench__audio-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.showroom-company-workbench__audio-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.showroom-company-workbench__audio-label,
.showroom-company-workbench__audio-empty {
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-company-workbench__audio {
  display: block;
  width: 100%;
  max-width: 240px;
}

.showroom-company-workbench__script-toolbar {
  flex-wrap: wrap;
}

.showroom-company-workbench__script-toolbar :deep(.el-input-number) {
  width: 160px;
}

@media (max-width: 960px) {
  .showroom-company-workbench__toolbar,
  .showroom-company-workbench__panel-header,
  .showroom-company-workbench__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-company-workbench__dialog-grid,
  .showroom-company-workbench__field-grid,
  .showroom-company-workbench__audio-grid {
    grid-template-columns: 1fr;
  }

  .showroom-company-workbench__field-card--cover {
    grid-column: auto;
  }
}
</style>
