<template>
  <ContentWrap v-loading="loading" :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <div v-if="assistantNeedsLaunch" class="invoice-voucher-print-launch">
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        show-icon
      />
      <el-alert
        :title="assistantStatusMessage || '发票凭证打印助手尚未启动，请点击启动助手。'"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="invoice-voucher-print-launch__actions">
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
      class="invoice-voucher-print-frame"
      title="发票凭证打印"
      frameborder="0"
      allow="clipboard-read; clipboard-write; fullscreen"
    ></iframe>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  createInvoiceVoucherPrintTicket,
  getInvoiceVoucherPrintAssistantStatus,
  startInvoiceVoucherPrintAssistant
} from '@/api/login'

defineOptions({ name: 'ErpInvoiceVoucherPrint' })

const loading = ref(false)
const errorMessage = ref('')
const assistantNeedsLaunch = ref(false)
const assistantStarting = ref(false)
const assistantStatusMessage = ref('')
const assistantAccessUrl = ref('')

const assistantBaseUrl = ref('')

const applyPublicUrl = (value: string) => {
  if (!value?.trim()) throw new Error('发票凭证打印助手地址未配置，请联系管理员。')
  const url = new URL(value)
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.search || url.hash) {
    throw new Error('发票凭证打印助手公开地址配置无效。')
  }
  assistantBaseUrl.value = url.href.endsWith('/') ? url.href : `${url.href}/`
}

const buildAssistantAccessUrl = (baseUrl: string, ticket: string) => {
  if (!ticket) {
    throw new Error('发票凭证打印助手授权失败：未取得访问票据。')
  }
  const normalizedBaseUrl = baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`
  const url = new URL('auth/callback', normalizedBaseUrl)
  url.searchParams.set('ticket', ticket)
  return url.toString()
}

const openAssistant = async () => {
  const data = await createInvoiceVoucherPrintTicket()
  assistantAccessUrl.value = buildAssistantAccessUrl(assistantBaseUrl.value, data.ticket)
  assistantNeedsLaunch.value = false
}

const loadAssistantEntry = async () => {
  assistantBaseUrl.value = ''
  loading.value = true
  errorMessage.value = ''
  assistantNeedsLaunch.value = false
  assistantStatusMessage.value = ''
  assistantAccessUrl.value = ''
  try {
    const status = await getInvoiceVoucherPrintAssistantStatus()
    applyPublicUrl(status.publicUrl)
    assistantStatusMessage.value = status.message || ''
    if (status.running) {
      await openAssistant()
      return
    }
    if (status.launchable) {
      assistantNeedsLaunch.value = true
      return
    }
    errorMessage.value = assistantStatusMessage.value || '发票凭证打印助手启动配置缺失。'
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '发票凭证打印助手状态获取失败，请确认当前账号权限。'
  } finally {
    loading.value = false
  }
}

const handleStartAssistant = async () => {
  if (!assistantBaseUrl.value) {
    return
  }
  assistantStarting.value = true
  loading.value = true
  errorMessage.value = ''
  try {
    const status = await startInvoiceVoucherPrintAssistant()
    assistantStatusMessage.value = status.message || ''
    applyPublicUrl(status.publicUrl)
    if (!status.running) {
      throw new Error(status.message || '发票凭证打印助手启动失败。')
    }
    await openAssistant()
  } catch (error) {
    assistantAccessUrl.value = ''
    assistantNeedsLaunch.value = true
    errorMessage.value =
      error instanceof Error ? error.message : '发票凭证打印助手启动失败，请稍后重试。'
  } finally {
    assistantStarting.value = false
    loading.value = false
  }
}

onMounted(() => {
  loadAssistantEntry()
})
</script>

<style scoped>
.invoice-voucher-print-launch {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.invoice-voucher-print-launch__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.invoice-voucher-print-frame {
  display: block;
  width: 100%;
  height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding) -
      var(--app-content-padding) - 2px
  );
  border: 0;
}
</style>
