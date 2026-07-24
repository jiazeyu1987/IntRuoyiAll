<template>
  <div class="showroom-narration-workspace">
    <div class="showroom-narration-workspace__toolbar">
      <div>
        <h3 class="showroom-narration-workspace__title">讲解工作台</h3>
        <p class="showroom-narration-workspace__subtitle">
          同步维护讲解稿、讲解音频和预览资产状态，所有展示都读取真实 live 数据。
        </p>
      </div>
      <div class="showroom-narration-workspace__actions">
        <el-select v-model="form.targetType" placeholder="目标类型">
          <el-option label="公司" value="COMPANY" />
          <el-option label="展柜" value="HALL" />
          <el-option label="产品" value="PRODUCT" />
        </el-select>
        <el-select v-model="form.targetId" filterable placeholder="目标对象">
          <el-option
            v-for="option in currentTargetOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-select v-model="form.language" placeholder="讲解语言">
          <el-option label="中文" value="ZH" />
          <el-option label="English" value="EN" />
        </el-select>
        <el-button :loading="loading" @click="refreshWorkspace">
          <Icon class="mr-5px" icon="ep:refresh" />
          刷新
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :closable="false"
      show-icon
      title="讲解工作台加载失败"
      type="error"
      :description="loadError"
    />

    <div class="showroom-narration-workspace__status-grid">
      <div class="showroom-narration-workspace__status-item">
        <span class="label">讲解状态</span>
        <el-tag :type="resolveNarrationStatusTagType(currentVersion?.status || 'DRAFT')">
          {{ resolveNarrationStatusText(currentVersion?.status || 'DRAFT') }}
        </el-tag>
      </div>
      <div class="showroom-narration-workspace__status-item">
        <span class="label">音频状态</span>
        <el-tag :type="resolveGenerationStatusTagType(currentVersion?.generationStatus || 'NOT_GENERATED')">
          {{ resolveGenerationStatusText(currentVersion?.generationStatus || 'NOT_GENERATED') }}
        </el-tag>
      </div>
      <div class="showroom-narration-workspace__status-item">
        <span class="label">Live</span>
        <span>{{ currentVersion?.live ? '已生效' : '未生效' }}</span>
      </div>
      <div class="showroom-narration-workspace__status-item">
        <span class="label">版本</span>
        <span>{{ currentVersion?.versionNo || '未创建' }}</span>
      </div>
    </div>

    <div class="showroom-narration-workspace__body">
      <div class="showroom-narration-workspace__editor-shell">
        <div class="showroom-narration-workspace__section-title">讲解稿</div>
        <el-form label-width="110px">
          <el-form-item label="来源版本ID">
            <el-input-number v-model="form.sourceRevisionId" :min="1" />
          </el-form-item>
          <el-form-item label="Audience">
            <el-input v-model="form.audienceType" disabled />
          </el-form-item>
          <el-form-item label="讲解稿">
            <el-input
              v-model="form.scriptText"
              :rows="12"
              placeholder="输入真实讲解稿后再保存草稿或保存 AI 草稿"
              type="textarea"
            />
          </el-form-item>
        </el-form>

        <div class="showroom-narration-workspace__detail-actions">
          <el-button :loading="actionLoading" @click="handleSaveDraft">保存草稿</el-button>
          <el-button :loading="actionLoading" type="primary" @click="handleGenerateScript">
            保存 AI 草稿
          </el-button>
          <el-button
            :disabled="!currentVersion?.id"
            :loading="actionLoading"
            type="success"
            @click="handleGenerateAudio"
          >
            生成讲解音频
          </el-button>
        </div>
      </div>

      <div class="showroom-narration-workspace__asset-shell">
        <div class="showroom-narration-workspace__section-title">讲解音频</div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="讲解版本ID">
            {{ currentVersion?.id || '未创建' }}
          </el-descriptions-item>
          <el-descriptions-item label="音频文件ID">
            {{ currentVersion?.audioFileId || '未生成' }}
          </el-descriptions-item>
          <el-descriptions-item label="音频时长">
            {{ currentVersion?.audioDurationSeconds || '未生成' }}
          </el-descriptions-item>
          <el-descriptions-item label="AI 标记">
            {{ currentVersion?.generatedByAi ? 'AI 草稿' : '人工草稿' }}
          </el-descriptions-item>
        </el-descriptions>

        <audio v-if="audioUrl" class="showroom-narration-workspace__audio" controls :src="audioUrl"></audio>
        <el-alert
          v-else
          :closable="false"
          show-icon
          title="讲解音频尚未生成"
          type="warning"
          description="当前区域不会伪造音频成功，必须等真实 audioFileId 生成后才可播放。"
        />

        <div class="showroom-narration-workspace__section-title mt-16px">预览资产</div>
        <div class="showroom-narration-workspace__preview-card">
          <div class="showroom-narration-workspace__preview-title">{{ previewState.title }}</div>
          <div class="showroom-narration-workspace__preview-description">
            {{ previewState.description }}
          </div>
          <img
            v-if="previewState.previewImageUrl"
            :src="previewState.previewImageUrl"
            alt="previewImageUrl"
            class="showroom-narration-workspace__preview-image"
          />
          <el-empty v-else description="当前目标没有可读取的 live previewImageUrl" />
        </div>

        <el-alert
          v-if="canSubmit"
          class="mt-16px"
          :closable="false"
          show-icon
          title="人工确认"
          type="info"
          description="讲解稿、讲解音频和预览资产生成后必须人工确认，再提交审批；未提交前前台仍只读取当前 live 资产。"
        />

        <el-form class="mt-16px" label-width="110px">
          <el-form-item v-if="canSubmit" label="人工确认">
            <el-checkbox v-model="manualConfirmed">
              我已人工确认当前讲解稿、讲解音频和预览资产，可以提交审批
            </el-checkbox>
          </el-form-item>
          <el-form-item label="主管审批人">
            <el-select v-model="form.supervisorUserId" filterable placeholder="请选择主管审批人">
              <el-option
                v-for="user in userOptions"
                :key="user.id"
                :label="`${user.nickname} #${user.id}`"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="审批人">
            <el-input model-value="企宣角色" disabled />
          </el-form-item>
          <el-form-item>
            <el-button
              v-if="canSubmit"
              :disabled="!currentVersion?.id || !manualConfirmed"
              :loading="actionLoading"
              type="primary"
              @click="handleSubmit"
            >
              提交审批
            </el-button>
            <el-button
              v-if="canSupervisorApprove"
              :disabled="!currentVersion?.id"
              :loading="actionLoading"
              type="warning"
              @click="handleSupervisorApprove"
            >
              主管审批通过
            </el-button>
            <el-button
              v-if="canGaoxinApprove"
              :disabled="!currentVersion?.id"
              :loading="actionLoading"
              type="warning"
              @click="handleGaoxinApprove"
            >
              企宣审批通过
            </el-button>
            <el-button
              v-if="canPublish"
              :disabled="!currentVersion?.id"
              :loading="actionLoading"
              type="success"
              @click="handlePublish"
            >
              确认发布
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import request from '@/config/axios'
import { ShowroomAdminApi } from '@/api/showroom-admin'
import { ShowroomFrontstageApi } from '@/api/showroom-frontstage'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import {
  buildTargetOptions,
  normalizeNarrationVersion,
  resolveNarrationAudioUrl,
  resolveGenerationStatusTagType,
  resolveGenerationStatusText,
  resolveNarrationStatusTagType,
  resolveNarrationStatusText,
  resolveTargetTypeText,
  type PreviewAssetState,
  type ShowroomNarrationVersionRecord
} from './contracts'

defineOptions({ name: 'NarrationWorkspace' })

const props = withDefaults(
  defineProps<{
    companyCurrent?: Record<string, unknown> | null
    halls?: unknown[]
    products?: unknown[]
  }>(),
  {
    companyCurrent: null,
    halls: () => [],
    products: () => []
  }
)

const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const actionLoading = ref(false)
const loadError = ref('')
const currentVersion = ref<ShowroomNarrationVersionRecord | null>(null)
const userOptions = ref<UserVO[]>([])
const previewState = ref<PreviewAssetState>({
  title: '预览资产',
  description: '当前目标还没有加载 live 预览资产状态。',
  previewImageUrl: ''
})
const manualConfirmed = ref(false)

type NarrationTargetType = 'COMPANY' | 'HALL' | 'PRODUCT'
type NarrationLanguage = 'ZH' | 'EN'

interface NarrationWorkspaceForm {
  targetType: NarrationTargetType
  targetId: number | null
  sourceRevisionId: number | null
  audienceType: 'PUBLIC'
  language: NarrationLanguage
  scriptText: string
  supervisorUserId: number | null
}

const form = reactive<NarrationWorkspaceForm>({
  targetType: 'PRODUCT',
  targetId: null,
  sourceRevisionId: null,
  audienceType: 'PUBLIC',
  language: 'ZH',
  scriptText: '',
  supervisorUserId: null
})

const targetOptions = computed(() =>
  buildTargetOptions(props.companyCurrent, props.halls, props.products)
)

const currentTargetOptions = computed(() => targetOptions.value[form.targetType])

const audioUrl = computed(() => resolveNarrationAudioUrl(currentVersion.value))
const currentStatus = computed(() => currentVersion.value?.status || 'DRAFT')
const canSubmit = computed(() => currentStatus.value === 'DRAFT')
const canSupervisorApprove = computed(() => currentStatus.value === 'PENDING_SUPERVISOR_REVIEW')
const canGaoxinApprove = computed(() => currentStatus.value === 'PENDING_GAOXIN_APPROVAL')
const canPublish = computed(() => currentStatus.value === 'APPROVED')

const resetManualConfirmation = () => {
  manualConfirmed.value = false
}

const syncTargetSelection = () => {
  if (!currentTargetOptions.value.find((item) => item.value === form.targetId)) {
    form.targetId = currentTargetOptions.value[0]?.value ?? null
  }
  const activeOption = currentTargetOptions.value.find((item) => item.value === form.targetId)
  if (form.targetType === 'HALL') {
    form.sourceRevisionId = form.sourceRevisionId || null
    return
  }
  form.sourceRevisionId = activeOption?.sourceRevisionId ?? null
}

watch(
  currentTargetOptions,
  () => {
    syncTargetSelection()
  },
  { immediate: true }
)

watch(
  () => [form.targetType, form.targetId, form.language],
  () => {
    syncTargetSelection()
    void refreshWorkspace()
  }
)

watch(
  () => form.scriptText,
  () => {
    if (currentStatus.value === 'DRAFT') {
      resetManualConfirmation()
    }
  }
)

const loadUserOptions = async () => {
  userOptions.value = await getSimpleUserList()
}

const loadNarration = async () => {
  if (!form.targetId) {
    currentVersion.value = null
    form.scriptText = ''
    loadError.value = ''
    resetManualConfirmation()
    return
  }
  loading.value = true
  try {
    const version = await request.get({
      url: '/showroom/narration/get',
      params: {
        targetType: form.targetType,
        targetId: form.targetId,
        audienceType: form.audienceType,
        language: form.language
      }
    })
    currentVersion.value = normalizeNarrationVersion(version)
    form.scriptText = currentVersion.value.scriptText
    if (form.targetType === 'HALL' && !form.sourceRevisionId) {
      form.sourceRevisionId = currentVersion.value.sourceRevisionId
    }
    loadError.value = ''
    resetManualConfirmation()
  } catch (error) {
    currentVersion.value = null
    const resolved = error instanceof Error ? error : new Error(String(error))
    loadError.value = resolved.message
    resetManualConfirmation()
  } finally {
    loading.value = false
  }
}

const readProductPreviewImageUrl = async (productId: number) => {
  const payload = (await ShowroomFrontstageApi.getDisplayProduct(productId)) as unknown as Record<string, unknown>
  const productCard =
    payload.productCard && typeof payload.productCard === 'object'
      ? (payload.productCard as Record<string, unknown>)
      : {}
  return typeof productCard.previewImageUrl === 'string' ? productCard.previewImageUrl : ''
}

const readHallPreviewImageUrl = async (hallId: number) => {
  const payload = (await ShowroomFrontstageApi.getDisplayHome()) as unknown as Record<string, unknown>
  const hallEntries = Array.isArray(payload.hallEntries) ? payload.hallEntries : []
  const hallEntry = hallEntries.find((item) => {
    return item && typeof item === 'object' && (item as Record<string, unknown>).id === hallId
  }) as Record<string, unknown> | undefined
  return hallEntry && typeof hallEntry.previewImageUrl === 'string' ? hallEntry.previewImageUrl : ''
}

const loadPreviewAssetState = async () => {
  if (!form.targetId) {
    previewState.value = {
      title: '预览资产',
      description: '当前未选择讲解对象。',
      previewImageUrl: ''
    }
    return
  }
  if (form.targetType === 'COMPANY') {
    previewState.value = {
      title: `${resolveTargetTypeText(form.targetType)}预览资产`,
      description: '当前后台与前台契约未提供公司级 previewImageUrl，工作台仅保留只读说明。',
      previewImageUrl: ''
    }
    return
  }
  const previewImageUrl =
    form.targetType === 'PRODUCT'
      ? await readProductPreviewImageUrl(form.targetId)
      : await readHallPreviewImageUrl(form.targetId)
  previewState.value = {
    title: `${resolveTargetTypeText(form.targetType)}预览资产`,
    description: previewImageUrl
      ? '当前区域直接读取真实 live previewImageUrl。'
      : '当前目标还没有发布可读取的 live previewImageUrl。',
    previewImageUrl
  }
}

const refreshWorkspace = async () => {
  await Promise.all([loadNarration(), loadPreviewAssetState()])
}

const ensureDraftPayload = (generatedByAi: boolean) => {
  if (!form.targetId) {
    throw new Error('请选择讲解对象')
  }
  if (!form.sourceRevisionId) {
    throw new Error('来源版本ID不能为空')
  }
  if (!form.scriptText.trim()) {
    throw new Error('讲解稿不能为空')
  }
  return {
    targetType: form.targetType,
    targetId: form.targetId,
    sourceRevisionId: form.sourceRevisionId,
    audienceType: form.audienceType,
    language: form.language,
    scriptText: form.scriptText.trim(),
    audioFileId: currentVersion.value?.audioFileId || null,
    audioDurationSeconds: currentVersion.value?.audioDurationSeconds || null,
    generatedByAi
  }
}

const handleSaveDraft = async () => {
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.saveNarrationDraft(ensureDraftPayload(false))
    currentVersion.value = normalizeNarrationVersion(version)
    form.scriptText = currentVersion.value.scriptText
    resetManualConfirmation()
    message.success('讲解草稿已保存')
  } finally {
    actionLoading.value = false
  }
}

const handleGenerateScript = async () => {
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.generateNarrationScript(ensureDraftPayload(true))
    currentVersion.value = normalizeNarrationVersion(version)
    form.scriptText = currentVersion.value.scriptText
    resetManualConfirmation()
    message.success('AI 草稿已保存')
  } finally {
    actionLoading.value = false
  }
}

const handleGenerateAudio = async () => {
  if (!currentVersion.value) {
    throw new Error('请先保存讲解草稿再生成讲解音频')
  }
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.generateNarrationAudio({
      narrationVersionId: currentVersion.value.id
    })
    currentVersion.value = normalizeNarrationVersion(version)
    resetManualConfirmation()
    message.success('讲解音频已生成')
  } finally {
    actionLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!currentVersion.value?.id) {
    throw new Error('当前没有可提交的讲解版本')
  }
  if (!manualConfirmed.value) {
    throw new Error('请先完成人工确认再提交审批')
  }
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.submitNarration({
      narrationVersionId: currentVersion.value.id,
      supervisorUserId: form.supervisorUserId,
      manualConfirmed: manualConfirmed.value
    })
    currentVersion.value = normalizeNarrationVersion(version)
    resetManualConfirmation()
    message.success('讲解资产已提交审批')
    await refreshWorkspace()
  } finally {
    actionLoading.value = false
  }
}

const handleSupervisorApprove = async () => {
  if (!currentVersion.value?.id) {
    throw new Error('当前没有可审批的讲解版本')
  }
  if (!form.supervisorUserId) {
    throw new Error('主管审批人为必填项')
  }
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.supervisorApproveNarration({
      narrationVersionId: currentVersion.value.id,
      reviewerUserId: form.supervisorUserId
    })
    currentVersion.value = normalizeNarrationVersion(version)
    message.success('主管审批已完成')
    await refreshWorkspace()
  } finally {
    actionLoading.value = false
  }
}

const handleGaoxinApprove = async () => {
  if (!currentVersion.value?.id) {
    throw new Error('当前没有可审批的讲解版本')
  }
  if (!userStore.getUser.id) {
    throw new Error('当前登录用户缺失，无法执行企宣审批')
  }
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.gaoxinApproveNarration({
      narrationVersionId: currentVersion.value.id,
      reviewerUserId: userStore.getUser.id
    })
    currentVersion.value = normalizeNarrationVersion(version)
    message.success('企宣审批已完成')
    await refreshWorkspace()
  } finally {
    actionLoading.value = false
  }
}

const handlePublish = async () => {
  if (!currentVersion.value?.id) {
    throw new Error('当前没有可发布的讲解版本')
  }
  actionLoading.value = true
  try {
    const version = await ShowroomAdminApi.publishNarration({
      narrationVersionId: currentVersion.value.id
    })
    currentVersion.value = normalizeNarrationVersion(version)
    message.success('讲解资产已确认发布')
    await refreshWorkspace()
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  await loadUserOptions()
  await refreshWorkspace()
})
</script>

<style scoped>
.showroom-narration-workspace {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.showroom-narration-workspace__toolbar,
.showroom-narration-workspace__editor-shell,
.showroom-narration-workspace__asset-shell,
.showroom-narration-workspace__status-item {
  background: #ffffff;
  border: 1px solid #dbe3ef;
}

.showroom-narration-workspace__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 8px 8px 0 0;
}

.showroom-narration-workspace__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-narration-workspace__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-narration-workspace__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showroom-narration-workspace__status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.showroom-narration-workspace__status-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 70px;
  padding: 10px 12px;
  border-radius: 6px;
  color: #263247;
}

.showroom-narration-workspace__status-item .label {
  color: #4b5563;
  font-size: 0.8rem;
}

.showroom-narration-workspace__body {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 12px;
}

.showroom-narration-workspace__editor-shell,
.showroom-narration-workspace__asset-shell {
  padding: 12px;
  border-radius: 0 0 8px 8px;
}

.showroom-narration-workspace__section-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 0.95rem;
  font-weight: 600;
}

.showroom-narration-workspace__detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.showroom-narration-workspace__audio {
  width: 100%;
  margin-top: 12px;
}

.showroom-narration-workspace__preview-card {
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.showroom-narration-workspace__preview-title {
  color: #172033;
  font-size: 0.92rem;
  font-weight: 600;
}

.showroom-narration-workspace__preview-description {
  margin-top: 6px;
  color: #4b5563;
  font-size: 0.84rem;
  line-height: 1.6;
}

.showroom-narration-workspace__preview-image {
  width: 100%;
  max-height: 260px;
  margin-top: 12px;
  object-fit: contain;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
}

@media (max-width: 1100px) {
  .showroom-narration-workspace__status-grid,
  .showroom-narration-workspace__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .showroom-narration-workspace__toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
