<template>
  <el-dialog
    v-model="dialogVisible"
    title="产品语音"
    class="showroom-product-audio-dialog"
    destroy-on-close
    width="760px"
  >
    <div v-loading="loading || generating" class="showroom-product-audio-dialog__body">
      <div class="showroom-product-audio-dialog__summary">
        <div>
          <span class="showroom-product-audio-dialog__summary-label">产品编码</span>
          <strong>{{ productCode || '未提供' }}</strong>
        </div>
        <div>
          <span class="showroom-product-audio-dialog__summary-label">中文名称</span>
          <strong>{{ productName || '未命名' }}</strong>
        </div>
        <div>
          <span class="showroom-product-audio-dialog__summary-label">来源版本</span>
          <strong>{{ sourceRevisionSummary }}</strong>
        </div>
      </div>

      <el-alert
        v-if="loadError"
        :closable="false"
        class="showroom-product-audio-dialog__alert"
        show-icon
        type="error"
        :title="loadError"
      />

      <div class="showroom-product-audio-dialog__grid">
        <section v-for="card in audioCards" :key="card.language" class="showroom-product-audio-dialog__card">
          <div class="showroom-product-audio-dialog__card-header">
            <h4>{{ card.label }}</h4>
            <el-tag :type="card.statusTagType" effect="plain">
              {{ card.statusText }}
            </el-tag>
          </div>

          <div class="showroom-product-audio-dialog__meta">
            <div class="showroom-product-audio-dialog__meta-item">
              <span class="showroom-product-audio-dialog__meta-label">语音版本</span>
              <strong>{{ card.versionText }}</strong>
            </div>
            <div class="showroom-product-audio-dialog__meta-item">
              <span class="showroom-product-audio-dialog__meta-label">发布状态</span>
              <el-tag v-if="card.version" :type="card.narrationStatusTagType" effect="plain">
                {{ card.narrationStatusText }}
              </el-tag>
              <span v-else class="showroom-product-audio-dialog__meta-empty">未生成</span>
            </div>
            <div class="showroom-product-audio-dialog__meta-item">
              <span class="showroom-product-audio-dialog__meta-label">生成状态</span>
              <el-tag v-if="card.version" :type="card.generationStatusTagType" effect="plain">
                {{ card.generationStatusText }}
              </el-tag>
              <span v-else class="showroom-product-audio-dialog__meta-empty">未生成</span>
            </div>
            <div class="showroom-product-audio-dialog__meta-item">
              <span class="showroom-product-audio-dialog__meta-label">音色</span>
              <strong>{{ card.voiceText }}</strong>
            </div>
          </div>

          <audio
            v-if="card.audioUrl"
            :src="card.audioUrl"
            class="showroom-product-audio-dialog__audio"
            controls
            preload="none"
          ></audio>
          <div v-else class="showroom-product-audio-dialog__empty">
            当前暂无可播放的{{ card.label }}。
          </div>
        </section>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button type="primary" :loading="generating" @click="handleGenerate">
        生成中英文语音
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ShowroomAdminApi } from '@/api/showroom-admin'
import {
  normalizeNarrationVersion,
  resolveGenerationStatusTagType,
  resolveGenerationStatusText,
  resolveNarrationAudioUrl,
  resolveNarrationStatusTagType,
  resolveNarrationStatusText,
  type ShowroomNarrationVersionRecord
} from '@/views/showroom-admin/narration/contracts'
import { formatShowroomStructuredError } from '@/views/showroom-admin/shared/structuredError'

defineOptions({ name: 'ShowroomProductAudioDialog' })

const props = withDefaults(
  defineProps<{
    generateHandler: (product: Record<string, unknown>) => Promise<void>
    modelValue: boolean
    productId?: number | null
    productCode?: string
    productName?: string
    sourceRevisionId?: number | null
  }>(),
  {
    productId: null,
    productCode: '',
    productName: '',
    sourceRevisionId: null
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  generated: []
}>()

const message = useMessage()
const loading = ref(false)
const generating = ref(false)
const loadError = ref('')
const zhNarration = ref<ShowroomNarrationVersionRecord | null>(null)
const enNarration = ref<ShowroomNarrationVersionRecord | null>(null)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const sourceRevisionSummary = computed(() => {
  return Number.isFinite(props.sourceRevisionId) ? `Revision ${props.sourceRevisionId}` : '缺少来源版本'
})

const isNarrationNotFoundError = (messageText: string) => {
  return messageText.includes('SHOWROOM_TARGET_NOT_FOUND: narration not found')
}

const resolveError = (error: unknown) => {
  return error instanceof Error ? error : new Error(String(error))
}

const requireProductId = () => {
  if (!Number.isFinite(props.productId)) {
    throw new Error('产品语音弹框缺少产品标识，无法继续')
  }
  return Number(props.productId)
}

const requireSourceRevisionId = () => {
  if (!Number.isFinite(props.sourceRevisionId)) {
    throw new Error('产品缺少来源版本，无法打开语音弹框')
  }
  return Number(props.sourceRevisionId)
}

const resolveAudioUrl = (version: ShowroomNarrationVersionRecord | null) => {
  return resolveNarrationAudioUrl(version)
}

const loadNarrationByLanguage = async (language: 'ZH' | 'EN') => {
  const productId = requireProductId()
  try {
    const version = normalizeNarrationVersion(
      await ShowroomAdminApi.getNarration({
        targetType: 'PRODUCT',
        targetId: productId,
        audienceType: 'PUBLIC',
        language
      })
    )
    resolveNarrationAudioUrl(version)
    return version
  } catch (error) {
    const resolved = resolveError(error)
    if (isNarrationNotFoundError(resolved.message)) {
      return null
    }
    throw resolved
  }
}

const loadDialogState = async () => {
  if (!dialogVisible.value) {
    return
  }
  loading.value = true
  loadError.value = ''
  zhNarration.value = null
  enNarration.value = null

  const errors: string[] = []

  try {
    requireProductId()
    const [zhVersion, enVersion] = await Promise.all(
      (['ZH', 'EN'] as const).map(async (language) => {
        try {
          return await loadNarrationByLanguage(language)
        } catch (error) {
          const formatted = formatShowroomStructuredError(
            error,
            language === 'ZH' ? '中文语音读取' : '英文语音读取'
          )
          errors.push(formatted)
          return null
        }
      })
    )
    zhNarration.value = zhVersion
    enNarration.value = enVersion
    if (errors.length) {
      loadError.value = errors.join('；')
    }
  } catch (error) {
    const resolved = resolveError(error)
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const audioCards = computed(() => {
  return [
    {
      language: 'ZH',
      label: '中文语音',
      version: zhNarration.value
    },
    {
      language: 'EN',
      label: '英文语音',
      version: enNarration.value
    }
  ].map((item) => ({
    ...item,
    statusText: item.version?.audioFileId ? '可播放' : '未生成',
    statusTagType: item.version?.audioFileId ? 'success' : 'info',
    versionText: item.version ? `V${item.version.versionNo}` : '未生成',
    narrationStatusText: item.version ? resolveNarrationStatusText(item.version.status) : '未生成',
    narrationStatusTagType: item.version ? resolveNarrationStatusTagType(item.version.status) : 'info',
    generationStatusText: item.version
      ? resolveGenerationStatusText(item.version.generationStatus)
      : '未生成',
    generationStatusTagType: item.version
      ? resolveGenerationStatusTagType(item.version.generationStatus)
      : 'info',
    voiceText: item.version?.voice || '未记录',
    audioUrl: resolveAudioUrl(item.version)
  }))
})

const handleGenerate = async () => {
  const productId = requireProductId()
  const sourceRevisionId = requireSourceRevisionId()
  generating.value = true
  try {
    await props.generateHandler({
      productId,
      sourceRevisionId
    })
    await loadDialogState()
    emit('generated')
    message.success('中英文讲解音频已生成')
  } catch (error) {
    const formatted = formatShowroomStructuredError(error, '产品音频生成')
    message.error(formatted)
    throw (error instanceof Error ? error : new Error(formatted))
  } finally {
    generating.value = false
  }
}

watch(
  () => [props.modelValue, props.productId, props.sourceRevisionId] as const,
  ([visible]) => {
    if (!visible) {
      return
    }
    void loadDialogState()
  },
  { immediate: true }
)
</script>

<style scoped>
.showroom-product-audio-dialog__body {
  min-height: 260px;
}

.showroom-product-audio-dialog__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-audio-dialog__summary-label {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-product-audio-dialog__alert {
  margin-bottom: 16px;
}

.showroom-product-audio-dialog__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.showroom-product-audio-dialog__card {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-product-audio-dialog__card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.showroom-product-audio-dialog__card-header h4 {
  margin: 0;
  font-size: 1rem;
  color: #172033;
}

.showroom-product-audio-dialog__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.showroom-product-audio-dialog__meta-item {
  min-width: 0;
}

.showroom-product-audio-dialog__meta-label {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 0.85rem;
}

.showroom-product-audio-dialog__meta-empty {
  color: #8a94a6;
}

.showroom-product-audio-dialog__audio {
  width: 100%;
}

.showroom-product-audio-dialog__empty {
  min-height: 40px;
  padding: 12px;
  color: #8a94a6;
  background: #f7f9fc;
  border: 1px dashed #dbe3ef;
  border-radius: 6px;
}

@media (max-width: 820px) {
  .showroom-product-audio-dialog__summary,
  .showroom-product-audio-dialog__grid,
  .showroom-product-audio-dialog__meta {
    grid-template-columns: 1fr;
  }
}
</style>
