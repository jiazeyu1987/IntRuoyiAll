<template>
  <ContentWrap v-loading="loading" :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <el-alert
      v-if="!assistantBaseUrl"
      title="分贝通费用报销助手地址未配置，请配置 VITE_FENBEITONG_ASSISTANT_URL。"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <div v-else-if="assistantNeedsLaunch" class="fenbeitong-assistant-launch">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        :title="assistantStatusMessage || '分贝通费用报销助手尚未启动，请点击启动助手。'"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="fenbeitong-assistant-launch__actions">
        <el-button type="primary" :loading="assistantStarting" @click="handleStartAssistant">
          启动助手
        </el-button>
      </div>
    </div>
    <el-alert
      v-else-if="errorMessage"
      :title="errorMessage"
      type="error"
      :closable="false"
      show-icon
      class="m-16px"
    />
    <iframe
      v-else-if="assistantAccessUrl"
      :src="assistantAccessUrl"
      class="fenbeitong-assistant-frame"
      title="分贝通费用报销"
      frameborder="0"
      allow="clipboard-read; clipboard-write; fullscreen"
    ></iframe>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  createFenbeitongAssistantTicket,
  getFenbeitongAssistantStatus,
  startFenbeitongAssistant
} from '@/api/login'

defineOptions({ name: 'ErpFenbeitongVoucher' })

const loading = ref(false)
const errorMessage = ref('')
const assistantNeedsLaunch = ref(false)
const assistantStarting = ref(false)
const assistantStatusMessage = ref('')
const assistantAccessUrl = ref('')

const assistantBaseUrl = computed(() => {
  return import.meta.env.VITE_FENBEITONG_ASSISTANT_URL?.trim() || ''
})

const buildAssistantAccessUrl = (baseUrl: string, ticket: string) => {
  if (!ticket) {
    throw new Error('分贝通费用报销助手授权失败：未取得访问票据。')
  }
  const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
  const url = new URL('auth/callback', normalizedBaseUrl)
  url.searchParams.set('ticket', ticket)
  return url.toString()
}

const openAssistant = async () => {
  const data = await createFenbeitongAssistantTicket()
  assistantAccessUrl.value = buildAssistantAccessUrl(assistantBaseUrl.value, data.ticket)
  assistantNeedsLaunch.value = false
}

const loadAssistantEntry = async () => {
  if (!assistantBaseUrl.value) {
    assistantAccessUrl.value = ''
    return
  }
  loading.value = true
  errorMessage.value = ''
  assistantNeedsLaunch.value = false
  assistantStatusMessage.value = ''
  assistantAccessUrl.value = ''
  try {
    const status = await getFenbeitongAssistantStatus()
    assistantStatusMessage.value = status.message || ''
    if (status.running) {
      await openAssistant()
      return
    }
    if (status.launchable) {
      assistantNeedsLaunch.value = true
      return
    }
    errorMessage.value = assistantStatusMessage.value || '分贝通费用报销助手启动配置缺失。'
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '分贝通费用报销助手状态获取失败，请确认当前账号权限。'
  } finally {
    loading.value = false
  }
}

const handleStartAssistant = async () => {
  if (!assistantBaseUrl.value) return
  assistantStarting.value = true
  loading.value = true
  errorMessage.value = ''
  try {
    const status = await startFenbeitongAssistant()
    assistantStatusMessage.value = status.message || ''
    if (!status.running) {
      throw new Error(status.message || '分贝通费用报销助手启动失败。')
    }
    await openAssistant()
  } catch (error) {
    assistantAccessUrl.value = ''
    assistantNeedsLaunch.value = true
    errorMessage.value =
      error instanceof Error ? error.message : '分贝通费用报销助手启动失败，请稍后重试。'
  } finally {
    assistantStarting.value = false
    loading.value = false
  }
}

onMounted(loadAssistantEntry)
</script>

<style scoped>
.fenbeitong-assistant-launch {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.fenbeitong-assistant-launch__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fenbeitong-assistant-frame {
  display: block;
  width: 100%;
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding) -
      var(--app-content-padding) - 2px
  );
  border: 0;
}
</style>
