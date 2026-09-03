<template>
  <el-card
    class="registration-certificate-config"
    data-testid="registration-certificate-config"
    shadow="never"
  >
    <template #header>
      <div class="registration-certificate-config__header">
        <div>
          <div class="registration-certificate-config__title">注册证配置</div>
          <div class="registration-certificate-config__subtitle">
            仅拥有注册证配置权限的账号可查看；修改提醒启停和每日运行时间需要独立修改权限。
          </div>
        </div>
        <el-tag type="info" effect="plain">Asia/Shanghai</el-tag>
      </div>
    </template>

    <el-alert
      v-if="loadError"
      class="registration-certificate-config__alert"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="!canUpdate"
      class="registration-certificate-config__alert"
      title="当前账号只有注册证配置查看权限，不能保存修改。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form
      class="registration-certificate-config__form"
      label-width="120px"
      :disabled="loading || saving"
    >
      <el-form-item label="启用提醒">
        <el-switch v-model="form.enabled" :disabled="!canUpdate" />
      </el-form-item>
      <el-form-item label="每日运行时间">
        <el-time-picker
          v-model="form.dailyRunTime"
          value-format="HH:mm"
          format="HH:mm"
          :clearable="false"
          :disabled="!canUpdate"
        />
      </el-form-item>
      <el-form-item label="业务时区">
        <el-input v-model="form.timezone" disabled />
      </el-form-item>
      <el-form-item label="接收规则">
        <el-alert
          title="提醒收件人由注册部经理在注册证页面的“通知设置”中维护。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-form-item>
      <el-form-item label="阈值规则">
        <el-input
          v-model="form.thresholdDaysJson"
          type="textarea"
          :rows="3"
          readonly
        />
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" @click="loadConfig">刷新配置</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!canUpdate || !loaded"
          @click="handleSave"
        >
          保存配置
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import {
  getRegistrationCertificateReminderConfig,
  updateRegistrationCertificateReminderConfig,
  type DccRegistrationCertificateReminderConfigRespVO
} from '@/api/dcc/registrationCertificate/reminderConfig'

defineProps<{
  canUpdate: boolean
}>()

const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const loadError = ref('')
const form = reactive({
  enabled: true,
  dailyRunTime: '09:00',
  timezone: 'Asia/Shanghai',
  thresholdDaysJson: '',
  thresholdRecipientUserIds: { T_30: [], T_8: [], T_2: [], T_1: [] },
  rowVersion: 0
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseData =
    typeof error === 'object' && error !== null && 'response' in error
      ? (error as { response?: { data?: { msg?: unknown; message?: unknown } } }).response?.data
      : undefined
  const responseMessage = responseData?.msg || responseData?.message
  return typeof responseMessage === 'string' && responseMessage.trim()
    ? responseMessage
    : defaultMessage
}

const assertConfigShape = (data: DccRegistrationCertificateReminderConfigRespVO) => {
  if (!data || typeof data.rowVersion !== 'number') {
    throw new Error('注册证提醒配置返回格式无效：缺少版本号。')
  }
  if (typeof data.dailyRunTime !== 'string' || !/^\d{2}:\d{2}$/.test(data.dailyRunTime)) {
    throw new Error('注册证提醒配置返回格式无效：每日运行时间必须为 HH:mm。')
  }
  if (typeof data.timezone !== 'string' || !data.timezone.trim()) {
    throw new Error('注册证提醒配置返回格式无效：缺少业务时区。')
  }
  if (typeof data.thresholdDaysJson !== 'string' || !data.thresholdDaysJson.trim()) {
    throw new Error('注册证提醒配置返回格式无效：缺少阈值规则。')
  }
}

const applyConfig = (data: DccRegistrationCertificateReminderConfigRespVO) => {
  assertConfigShape(data)
  form.enabled = data.enabled === true
  form.dailyRunTime = data.dailyRunTime
  form.timezone = data.timezone
  form.thresholdDaysJson = data.thresholdDaysJson
  form.thresholdRecipientUserIds = {
    T_30: [...data.thresholdRecipientUserIds.T_30],
    T_8: [...data.thresholdRecipientUserIds.T_8],
    T_2: [...data.thresholdRecipientUserIds.T_2],
    T_1: [...data.thresholdRecipientUserIds.T_1]
  }
  form.rowVersion = data.rowVersion
  loaded.value = true
}

const loadConfig = async () => {
  loading.value = true
  loadError.value = ''
  try {
    applyConfig(await getRegistrationCertificateReminderConfig())
  } catch (error) {
    loaded.value = false
    loadError.value = resolveErrorMessage(error, '注册证提醒配置加载失败。')
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!loaded.value) {
    message.error('请先加载注册证提醒配置。')
    return
  }
  saving.value = true
  try {
    const updated = await updateRegistrationCertificateReminderConfig({
      enabled: form.enabled,
      dailyRunTime: form.dailyRunTime,
      thresholdRecipientUserIds: form.thresholdRecipientUserIds,
      expectedRowVersion: form.rowVersion
    })
    applyConfig(updated)
    message.success('注册证配置已保存')
  } catch (error) {
    message.error(resolveErrorMessage(error, '注册证提醒配置保存失败。'))
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.registration-certificate-config {
  max-width: 920px;
}

.registration-certificate-config__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.registration-certificate-config__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.registration-certificate-config__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.registration-certificate-config__alert {
  margin-bottom: 14px;
}

.registration-certificate-config__form {
  margin-top: 4px;
}
</style>
