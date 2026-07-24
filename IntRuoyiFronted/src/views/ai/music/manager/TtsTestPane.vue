<template>
  <div class="grid gap-16px">
    <el-alert
      title="当前测试页由系统内部 TTS 服务直接生成音频，不再依赖外部 RagInt 代理。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-form ref="formRef" :model="formData" label-position="top">
      <el-form-item label="TTS 提供方">
        <el-radio-group v-model="formData.provider">
          <el-radio-button
            v-for="item in TTS_PROVIDER_OPTIONS"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="selectedVoiceOptions.length > 0" :label="selectedVoiceLabel">
        <div v-if="formData.provider === 'aliyun_nls'" class="tts-voice-default-panel">
          <div class="tts-voice-default-row">
            <el-select
              v-model="formData.voice"
              :placeholder="`请选择${selectedVoiceLabel}`"
              class="!w-320px"
            >
              <el-option
                v-for="item in selectedVoiceOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button
              type="primary"
              :loading="aliyunNlsVoiceSaving"
              @click="handleSaveAliyunNlsVoice"
            >
              保存默认音色
            </el-button>
          </div>
          <div class="tts-voice-default-tip">{{ aliyunNlsVoiceStatusHint }}</div>
        </div>
        <el-select
          v-else
          v-model="formData.voice"
          :placeholder="`请选择${selectedVoiceLabel}`"
          class="!w-320px"
        >
          <el-option
            v-for="item in selectedVoiceOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-card v-if="formData.provider === 'aliyun_nls'" shadow="never" class="tts-token-panel">
        <template #header>
          <div class="flex items-center justify-between">
            <span>阿里云 NLS 凭证</span>
            <el-button text type="primary" :loading="aliyunNlsDefaultsLoading" @click="loadAliyunNlsDefaults">
              刷新状态
            </el-button>
          </div>
        </template>
        <div v-loading="aliyunNlsDefaultsLoading" class="tts-token-panel__body">
          <div class="tts-token-status">
            <el-tag :type="aliyunNlsAppKeyStatusTagType" effect="light">
              {{ aliyunNlsAppKeyStatusText }}
            </el-tag>
            <span>{{ aliyunNlsAppKeyStatusHint }}</span>
          </div>
          <div class="tts-token-save-row">
            <el-input
              v-model="aliyunNlsAppKeyInput"
              show-password
              clearable
              maxlength="500"
              placeholder="请输入新的 AppKey，保存后立即用于阿里云 NLS 合成"
              @keyup.enter="handleSaveAliyunNlsAppKey"
            />
            <el-button
              type="primary"
              :loading="aliyunNlsAppKeySaving"
              @click="handleSaveAliyunNlsAppKey"
            >
              保存 AppKey
            </el-button>
          </div>

          <div class="tts-token-status">
            <el-tag :type="aliyunNlsTokenStatusTagType" effect="light">
              {{ aliyunNlsTokenStatusText }}
            </el-tag>
            <span>{{ aliyunNlsTokenStatusHint }}</span>
          </div>
          <div class="tts-token-save-row">
            <el-input
              v-model="aliyunNlsTokenInput"
              show-password
              clearable
              maxlength="500"
              placeholder="请输入新的 AccessToken，保存后立即用于阿里云 NLS 合成"
              @keyup.enter="handleSaveAliyunNlsToken"
            />
            <el-button
              type="primary"
              :loading="aliyunNlsTokenSaving"
              @click="handleSaveAliyunNlsToken"
            >
              保存 AccessToken
            </el-button>
          </div>
          <div class="tts-token-tip">
            系统只显示脱敏 AppKey / Token；保存新值后，TTS 测试会优先使用保存值。
          </div>
        </div>
      </el-card>

      <el-form-item
        label="测试文本"
        prop="text"
        :rules="[{ required: true, message: '请输入待合成文本', trigger: 'blur' }]"
      >
        <el-input
          v-model="formData.text"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入一句要合成的文本，例如：你好，这是一条 TTS 测试语音。"
        />
      </el-form-item>

      <div class="flex flex-wrap gap-12px">
        <el-button type="primary" :loading="generating" @click="handleGenerate">
          生成音频
        </el-button>
        <el-button :disabled="!audioUrl" @click="togglePlayback">
          {{ isPlaying ? '暂停播放' : '播放音频' }}
        </el-button>
        <el-button :disabled="!audioUrl" @click="resetAudio">清空音频</el-button>
      </div>
    </el-form>

    <el-card shadow="never">
      <template #header>
        <div class="flex items-center justify-between">
          <span>生成结果</span>
          <span class="text-12px text-[var(--el-text-color-secondary)]">{{ audioHint }}</span>
        </div>
      </template>

      <audio
        ref="audioRef"
        :src="audioUrl || undefined"
        controls
        class="w-full"
        @play="isPlaying = true"
        @pause="isPlaying = false"
        @ended="isPlaying = false"
      ></audio>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance } from 'element-plus'
import {
  ALIYUN_NLS_VOICE_OPTIONS,
  TtsTestApi,
  type AliyunNlsDefaultsVO
} from '@/api/ai/tts'

defineOptions({ name: 'AiMusicManagerTtsTestPane' })

interface VoiceOption {
  value: string
  label: string
}

type TokenStatusTagType = 'success' | 'warning' | 'info'

const TTS_PROVIDER_OPTIONS = [
  { value: 'windows', label: 'Windows 本地' },
  { value: 'dashscope', label: 'DashScope CosyVoice' },
  { value: 'aliyun_nls', label: '阿里云 NLS' }
]

const DASHSCOPE_VOICE_OPTIONS: VoiceOption[] = [
  { value: 'longyang', label: 'longyang 男声' },
  { value: 'longxiaochun', label: 'longxiaochun 女声' },
  { value: 'longxiaoxia', label: 'longxiaoxia 女声' },
  { value: 'longxiaobei', label: 'longxiaobei 女声' }
]

const VOICE_OPTIONS_BY_PROVIDER: Record<string, VoiceOption[]> = {
  dashscope: DASHSCOPE_VOICE_OPTIONS,
  aliyun_nls: ALIYUN_NLS_VOICE_OPTIONS
}

const message = useMessage()

const formRef = ref<FormInstance>()
const audioRef = ref<Nullable<HTMLAudioElement>>(null)
const generating = ref(false)
const isPlaying = ref(false)
const audioUrl = ref('')
const aliyunNlsAppKeyInput = ref('')
const aliyunNlsTokenInput = ref('')
const aliyunNlsDefaults = ref<AliyunNlsDefaultsVO | null>(null)
const aliyunNlsDefaultsLoading = ref(false)
const aliyunNlsAppKeySaving = ref(false)
const aliyunNlsTokenSaving = ref(false)
const aliyunNlsVoiceSaving = ref(false)
const formData = reactive({
  provider: 'windows',
  voice: 'longyang',
  text: ''
})

const audioHint = computed(() =>
  audioUrl.value ? '音频已生成，可以使用上方按钮或播放器继续播放。' : '尚未生成音频。'
)
const selectedVoiceOptions = computed(() => VOICE_OPTIONS_BY_PROVIDER[formData.provider] || [])
const selectedVoiceLabel = computed(() =>
  formData.provider === 'aliyun_nls' ? '阿里云 NLS 默认音色' : 'DashScope 音色'
)
const voiceRequired = computed(() => selectedVoiceOptions.value.length > 0)
const maskedAppKey = computed(() => aliyunNlsDefaults.value?.maskedAppKey || '未配置')
const maskedAccessToken = computed(() => aliyunNlsDefaults.value?.maskedAccessToken || '未配置')

const aliyunNlsVoiceStatusHint = computed(() => {
  if (!aliyunNlsDefaults.value) {
    return '正在等待读取默认音色状态。'
  }
  if (aliyunNlsDefaults.value.voiceSource === 'saved') {
    return `当前保存值：${aliyunNlsDefaults.value.defaultVoice || '未配置'}`
  }
  if (aliyunNlsDefaults.value.voiceSource === 'runtime') {
    return `当前运行时值：${aliyunNlsDefaults.value.defaultVoice || '未配置'}；保存新值后将优先使用保存值。`
  }
  return '尚未配置默认音色，请先保存后再生成阿里云 NLS 音频。'
})

const aliyunNlsAppKeyStatusText = computed(() => {
  if (!aliyunNlsDefaults.value) {
    return '未读取'
  }
  if (aliyunNlsDefaults.value.appKeySource === 'saved') {
    return '已保存'
  }
  if (aliyunNlsDefaults.value.appKeyConfigured) {
    return '运行时配置'
  }
  return '未配置'
})
const aliyunNlsAppKeyStatusTagType = computed<TokenStatusTagType>(() => {
  if (aliyunNlsDefaults.value?.appKeySource === 'saved') {
    return 'success'
  }
  if (aliyunNlsDefaults.value?.appKeyConfigured) {
    return 'info'
  }
  return 'warning'
})
const aliyunNlsAppKeyStatusHint = computed(() => {
  if (!aliyunNlsDefaults.value) {
    return '正在等待读取 AppKey 状态。'
  }
  if (aliyunNlsDefaults.value.appKeySource === 'saved') {
    return `当前保存值：${maskedAppKey.value}`
  }
  if (aliyunNlsDefaults.value.appKeySource === 'runtime') {
    return `当前运行时值：${maskedAppKey.value}；保存新值后将优先使用保存值。`
  }
  return '尚未配置 AppKey，请先保存后再生成阿里云 NLS 音频。'
})

const aliyunNlsTokenStatusText = computed(() => {
  if (!aliyunNlsDefaults.value) {
    return '未读取'
  }
  if (aliyunNlsDefaults.value.tokenSource === 'saved') {
    return '已保存'
  }
  if (aliyunNlsDefaults.value.tokenConfigured) {
    return '运行时配置'
  }
  return '未配置'
})
const aliyunNlsTokenStatusTagType = computed<TokenStatusTagType>(() => {
  if (aliyunNlsDefaults.value?.tokenSource === 'saved') {
    return 'success'
  }
  if (aliyunNlsDefaults.value?.tokenConfigured) {
    return 'info'
  }
  return 'warning'
})
const aliyunNlsTokenStatusHint = computed(() => {
  if (!aliyunNlsDefaults.value) {
    return '正在等待读取 Token 状态。'
  }
  if (aliyunNlsDefaults.value.tokenSource === 'saved') {
    return `当前保存值：${maskedAccessToken.value}`
  }
  if (aliyunNlsDefaults.value.tokenSource === 'runtime') {
    return `当前运行时值：${maskedAccessToken.value}；保存新值后将优先使用保存值。`
  }
  return '尚未配置 AccessToken，请先保存后再生成阿里云 NLS 音频。'
})

const revokeAudioUrl = () => {
  if (!audioUrl.value) {
    return
  }
  URL.revokeObjectURL(audioUrl.value)
  audioUrl.value = ''
}

const stopPlayback = () => {
  if (!audioRef.value) {
    isPlaying.value = false
    return
  }
  audioRef.value.pause()
  audioRef.value.currentTime = 0
  isPlaying.value = false
}

const resetAudio = () => {
  stopPlayback()
  revokeAudioUrl()
}

const syncAliyunNlsDefaultVoice = () => {
  const voice = aliyunNlsDefaults.value?.defaultVoice
  if (voice && ALIYUN_NLS_VOICE_OPTIONS.some((item) => item.value === voice)) {
    formData.voice = voice
    return
  }
  if (!ALIYUN_NLS_VOICE_OPTIONS.some((item) => item.value === formData.voice)) {
    formData.voice = ALIYUN_NLS_VOICE_OPTIONS[0]?.value || ''
  }
}

const loadAliyunNlsDefaults = async () => {
  aliyunNlsDefaultsLoading.value = true
  try {
    aliyunNlsDefaults.value = await TtsTestApi.getAliyunNlsDefaults()
    syncAliyunNlsDefaultVoice()
  } finally {
    aliyunNlsDefaultsLoading.value = false
  }
}

const handleSaveAliyunNlsVoice = async () => {
  if (!formData.voice) {
    message.warning('请选择默认音色')
    return
  }
  aliyunNlsVoiceSaving.value = true
  try {
    await TtsTestApi.saveAliyunNlsDefaultVoice({ voice: formData.voice })
    await loadAliyunNlsDefaults()
    message.success('默认音色已保存')
  } finally {
    aliyunNlsVoiceSaving.value = false
  }
}

const handleSaveAliyunNlsAppKey = async () => {
  const appKey = aliyunNlsAppKeyInput.value.trim()
  if (!appKey) {
    message.warning('请输入 AppKey')
    return
  }
  aliyunNlsAppKeySaving.value = true
  try {
    await TtsTestApi.saveAliyunNlsAppKey({ appKey })
    aliyunNlsAppKeyInput.value = ''
    await loadAliyunNlsDefaults()
    message.success('AppKey 已保存')
  } finally {
    aliyunNlsAppKeySaving.value = false
  }
}

const handleSaveAliyunNlsToken = async () => {
  const accessToken = aliyunNlsTokenInput.value.trim()
  if (!accessToken) {
    message.warning('请输入 AccessToken')
    return
  }
  aliyunNlsTokenSaving.value = true
  try {
    await TtsTestApi.saveAliyunNlsToken({ accessToken })
    aliyunNlsTokenInput.value = ''
    await loadAliyunNlsDefaults()
    message.success('AccessToken 已保存')
  } finally {
    aliyunNlsTokenSaving.value = false
  }
}

const handleGenerate = async () => {
  await formRef.value?.validate()
  if (voiceRequired.value && !formData.voice) {
    message.warning(`请选择${selectedVoiceLabel.value}`)
    return
  }
  generating.value = true
  try {
    resetAudio()
    const blob = (await TtsTestApi.generateAudio({
      text: formData.text.trim(),
      provider: formData.provider,
      voice: voiceRequired.value ? formData.voice : ''
    })) as Blob
    if (!(blob instanceof Blob) || blob.size === 0) {
      throw new Error('empty_audio_blob')
    }
    audioUrl.value = URL.createObjectURL(blob)
    await nextTick()
    audioRef.value?.load()
    message.success('音频生成成功')
  } finally {
    generating.value = false
  }
}

const togglePlayback = async () => {
  if (!audioUrl.value) {
    message.warning('请先生成音频')
    return
  }
  if (!audioRef.value) {
    return
  }
  if (audioRef.value.paused) {
    await audioRef.value.play()
    isPlaying.value = true
    return
  }
  audioRef.value.pause()
  isPlaying.value = false
}

watch(
  () => formData.provider,
  async (provider) => {
    const options = VOICE_OPTIONS_BY_PROVIDER[provider] || []
    if (options.length === 0) {
      formData.voice = ''
      return
    }
    if (!options.some((item) => item.value === formData.voice)) {
      formData.voice = options[0].value
    }
    if (provider === 'aliyun_nls') {
      await loadAliyunNlsDefaults()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  resetAudio()
})
</script>

<style scoped>
.tts-token-panel {
  margin-bottom: 16px;
  border-color: #dbe3ef;
  border-radius: 8px;
}

.tts-token-panel__body {
  display: grid;
  gap: 12px;
}

.tts-token-status {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: #263247;
  font-size: 13px;
}

.tts-token-save-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 10px;
}

.tts-voice-default-panel {
  display: grid;
  gap: 8px;
}

.tts-voice-default-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 10px;
}

.tts-voice-default-tip {
  color: #6b7280;
  font-size: 12px;
}

.tts-token-tip {
  color: #6b7280;
  font-size: 12px;
}

@media (max-width: 768px) {
  .tts-voice-default-row,
  .tts-token-save-row {
    grid-template-columns: 1fr;
  }
}
</style>
