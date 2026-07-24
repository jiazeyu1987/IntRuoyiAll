<template>
  <div class="showroom-narration-workbench" v-loading="loading">
    <el-alert v-if="loadError" :closable="false" show-icon type="error" :title="loadError" />

    <template v-else>
      <div class="showroom-narration-workbench__toolbar">
        <div>
          <h3 class="showroom-narration-workbench__title">讲解工作台</h3>
          <p class="showroom-narration-workbench__subtitle">
            展柜讲解音频统一走共享阿里云 NLS；在这里设置默认音色、AppKey 和 AccessToken 后，后续展柜生成都使用这套默认值。
          </p>
        </div>
        <div class="showroom-narration-workbench__actions">
          <el-button :loading="loading" @click="loadDefaults">刷新状态</el-button>
        </div>
      </div>

      <div class="showroom-narration-workbench__grid">
        <section class="showroom-narration-workbench__panel">
          <div class="showroom-narration-workbench__panel-header">
            <h4>默认音色</h4>
            <el-tag :type="voiceStatusTagType" effect="light">
              {{ voiceStatusText }}
            </el-tag>
          </div>
          <p class="showroom-narration-workbench__panel-tip">{{ voiceStatusHint }}</p>
          <div class="showroom-narration-workbench__save-row">
            <el-select v-model="defaultVoice" class="showroom-narration-workbench__control">
              <el-option
                v-for="item in ALIYUN_NLS_VOICE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button type="primary" :loading="savingVoice" @click="handleSaveVoice">
              保存默认音色
            </el-button>
          </div>
        </section>

        <section class="showroom-narration-workbench__panel">
          <div class="showroom-narration-workbench__panel-header">
            <h4>阿里云 NLS AppKey</h4>
            <el-tag :type="appKeyStatusTagType" effect="light">
              {{ appKeyStatusText }}
            </el-tag>
          </div>
          <p class="showroom-narration-workbench__panel-tip">{{ appKeyStatusHint }}</p>
          <div class="showroom-narration-workbench__save-row">
            <el-input
              v-model="appKeyInput"
              class="showroom-narration-workbench__control"
              show-password
              clearable
              maxlength="500"
              placeholder="请输入新的 AppKey，保存后立即用于展柜讲解音频生成"
              @keyup.enter="handleSaveAppKey"
            />
            <el-button type="primary" :loading="savingAppKey" @click="handleSaveAppKey">
              保存 AppKey
            </el-button>
          </div>
          <div class="showroom-narration-workbench__token-tip">
            系统只显示脱敏 AppKey；展柜和音乐管理会共用这一套保存值。
          </div>
        </section>

        <section class="showroom-narration-workbench__panel">
          <div class="showroom-narration-workbench__panel-header">
            <h4>阿里云 NLS AccessToken</h4>
            <el-tag :type="tokenStatusTagType" effect="light">
              {{ tokenStatusText }}
            </el-tag>
          </div>
          <p class="showroom-narration-workbench__panel-tip">{{ tokenStatusHint }}</p>
          <div class="showroom-narration-workbench__save-row">
            <el-input
              v-model="accessTokenInput"
              class="showroom-narration-workbench__control"
              show-password
              clearable
              maxlength="500"
              placeholder="请输入新的 AccessToken，保存后立即用于展柜讲解音频生成"
              @keyup.enter="handleSaveToken"
            />
            <el-button type="primary" :loading="savingToken" @click="handleSaveToken">
              保存 AccessToken
            </el-button>
          </div>
          <div class="showroom-narration-workbench__token-tip">
            系统只显示脱敏 Token；展柜和音乐管理会共用这一套保存值。
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ShowroomAdminApi, type ShowroomNarrationTtsDefaultsRespVO } from '@/api/showroom-admin'
import { ALIYUN_NLS_VOICE_OPTIONS } from '@/api/ai/tts'

defineOptions({ name: 'ShowroomNarrationWorkbench' })

type StatusTagType = 'success' | 'warning' | 'info'

const message = useMessage()
const loading = ref(false)
const savingVoice = ref(false)
const savingAppKey = ref(false)
const savingToken = ref(false)
const loadError = ref('')
const defaults = ref<ShowroomNarrationTtsDefaultsRespVO | null>(null)
const defaultVoice = ref(ALIYUN_NLS_VOICE_OPTIONS[0]?.value || 'xiaoyun')
const appKeyInput = ref('')
const accessTokenInput = ref('')

const voiceStatusText = computed(() => {
  if (!defaults.value) {
    return '未读取'
  }
  if (defaults.value.voiceSource === 'saved') {
    return '已保存'
  }
  if (defaults.value.voiceConfigured) {
    return '运行时配置'
  }
  return '未配置'
})

const voiceStatusTagType = computed<StatusTagType>(() => {
  if (defaults.value?.voiceSource === 'saved') {
    return 'success'
  }
  if (defaults.value?.voiceConfigured) {
    return 'info'
  }
  return 'warning'
})

const voiceStatusHint = computed(() => {
  if (!defaults.value) {
    return '正在等待读取共享默认音色状态。'
  }
  if (defaults.value.voiceSource === 'saved') {
    return `当前保存值：${defaults.value.defaultVoice || '未配置'}`
  }
  if (defaults.value.voiceSource === 'runtime') {
    return `当前运行时值：${defaults.value.defaultVoice || '未配置'}；保存新值后将优先使用保存值。`
  }
  return '尚未配置默认音色，请先保存后再生成展柜讲解音频。'
})

const appKeyStatusText = computed(() => {
  if (!defaults.value) {
    return '未读取'
  }
  if (defaults.value.appKeySource === 'saved') {
    return '已保存'
  }
  if (defaults.value.appKeyConfigured) {
    return '运行时配置'
  }
  return '未配置'
})

const appKeyStatusTagType = computed<StatusTagType>(() => {
  if (defaults.value?.appKeySource === 'saved') {
    return 'success'
  }
  if (defaults.value?.appKeyConfigured) {
    return 'info'
  }
  return 'warning'
})

const appKeyStatusHint = computed(() => {
  if (!defaults.value) {
    return '正在等待读取共享 AppKey 状态。'
  }
  if (defaults.value.appKeySource === 'saved') {
    return `当前保存值：${defaults.value.maskedAppKey || '未配置'}`
  }
  if (defaults.value.appKeySource === 'runtime') {
    return `当前运行时值：${defaults.value.maskedAppKey || '未配置'}；保存新值后将优先使用保存值。`
  }
  return '尚未配置 AppKey，请先保存后再生成展柜讲解音频。'
})

const tokenStatusText = computed(() => {
  if (!defaults.value) {
    return '未读取'
  }
  if (defaults.value.tokenSource === 'saved') {
    return '已保存'
  }
  if (defaults.value.tokenConfigured) {
    return '运行时配置'
  }
  return '未配置'
})

const tokenStatusTagType = computed<StatusTagType>(() => {
  if (defaults.value?.tokenSource === 'saved') {
    return 'success'
  }
  if (defaults.value?.tokenConfigured) {
    return 'info'
  }
  return 'warning'
})

const tokenStatusHint = computed(() => {
  if (!defaults.value) {
    return '正在等待读取共享 Token 状态。'
  }
  if (defaults.value.tokenSource === 'saved') {
    return `当前保存值：${defaults.value.maskedAccessToken || '未配置'}`
  }
  if (defaults.value.tokenSource === 'runtime') {
    return `当前运行时值：${defaults.value.maskedAccessToken || '未配置'}；保存新值后将优先使用保存值。`
  }
  return '尚未配置 AccessToken，请先保存后再生成展柜讲解音频。'
})

const syncDefaultVoice = () => {
  const currentVoice = defaults.value?.defaultVoice
  if (currentVoice && ALIYUN_NLS_VOICE_OPTIONS.some((item) => item.value === currentVoice)) {
    defaultVoice.value = currentVoice
    return
  }
  defaultVoice.value = ALIYUN_NLS_VOICE_OPTIONS[0]?.value || 'xiaoyun'
}

const loadDefaults = async () => {
  loading.value = true
  loadError.value = ''
  try {
    defaults.value = await ShowroomAdminApi.getNarrationTtsDefaults()
    syncDefaultVoice()
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    defaults.value = null
    loadError.value = resolved.message
  } finally {
    loading.value = false
  }
}

const handleSaveVoice = async () => {
  if (!defaultVoice.value) {
    message.warning('请选择默认音色')
    return
  }
  savingVoice.value = true
  try {
    await ShowroomAdminApi.saveNarrationTtsDefaultVoice({ voice: defaultVoice.value })
    await loadDefaults()
    message.success('默认音色已保存')
  } finally {
    savingVoice.value = false
  }
}

const handleSaveAppKey = async () => {
  const appKey = appKeyInput.value.trim()
  if (!appKey) {
    message.warning('请输入 AppKey')
    return
  }
  savingAppKey.value = true
  try {
    await ShowroomAdminApi.saveNarrationTtsDefaultAppKey({ appKey })
    appKeyInput.value = ''
    await loadDefaults()
    message.success('AppKey 已保存')
  } finally {
    savingAppKey.value = false
  }
}

const handleSaveToken = async () => {
  const accessToken = accessTokenInput.value.trim()
  if (!accessToken) {
    message.warning('请输入 AccessToken')
    return
  }
  savingToken.value = true
  try {
    await ShowroomAdminApi.saveNarrationTtsDefaultToken({ accessToken })
    accessTokenInput.value = ''
    await loadDefaults()
    message.success('AccessToken 已保存')
  } finally {
    savingToken.value = false
  }
}

onMounted(() => {
  void loadDefaults()
})
</script>

<style scoped>
.showroom-narration-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.showroom-narration-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-narration-workbench__title {
  margin: 0;
  color: #172033;
  font-size: 1.05rem;
}

.showroom-narration-workbench__subtitle {
  margin: 4px 0 0;
  color: #4b5563;
  font-size: 0.9rem;
}

.showroom-narration-workbench__actions {
  display: flex;
  gap: 8px;
}

.showroom-narration-workbench__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.showroom-narration-workbench__panel {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.showroom-narration-workbench__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.showroom-narration-workbench__panel-header h4 {
  margin: 0;
  color: #172033;
  font-size: 0.95rem;
}

.showroom-narration-workbench__panel-tip,
.showroom-narration-workbench__token-tip {
  margin: 0 0 12px;
  color: #4b5563;
  font-size: 0.85rem;
  line-height: 1.6;
}

.showroom-narration-workbench__save-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.showroom-narration-workbench__control {
  width: 100%;
}

@media (max-width: 1200px) {
  .showroom-narration-workbench__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .showroom-narration-workbench__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .showroom-narration-workbench__actions {
    flex-wrap: wrap;
  }

  .showroom-narration-workbench__save-row {
    grid-template-columns: 1fr;
  }
}
</style>
