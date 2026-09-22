<template>
  <el-card class="profile-basic-config" data-testid="profile-basic-config" shadow="never">
    <template #header>
      <div class="profile-basic-config__header">
        <div>
          <div class="profile-basic-config__title">基础配置</div>
          <div class="profile-basic-config__subtitle">
            配置后台登录后无操作自动退出的时间，保存后重新进入页面会按新时间计时。
          </div>
        </div>
        <el-tag type="info" effect="plain">infra/config</el-tag>
      </div>
    </template>

    <el-alert
      v-if="loadError"
      class="profile-basic-config__alert"
      :title="loadError"
      type="error"
      :closable="false"
      show-icon
    />
    <el-alert
      v-if="!canUpdate"
      class="profile-basic-config__alert"
      title="当前账号只有基础配置查看权限，不能保存修改。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form
      class="profile-basic-config__form"
      label-width="150px"
      :disabled="loading || saving"
    >
      <el-form-item label="登录空闲退出时间">
        <div class="profile-basic-config__input-line">
          <el-input-number
            v-model="form.idleLogoutMinutes"
            :min="1"
            :max="1440"
            :step="1"
            :precision="0"
            controls-position="right"
            :disabled="!canUpdate || !loaded"
          />
          <span class="profile-basic-config__unit">分钟</span>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button :loading="loading" @click="loadConfig">刷新</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!canUpdate || !loaded"
          @click="handleSave"
        >
          保存
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import * as ConfigApi from '@/api/infra/config'

defineProps<{
  canUpdate: boolean
}>()

const message = useMessage()

const IDLE_LOGOUT_MINUTES_CONFIG_KEY = 'system.login.idle-timeout-minutes'
const MIN_IDLE_LOGOUT_MINUTES = 1
const MAX_IDLE_LOGOUT_MINUTES = 1440

const loading = ref(false)
const saving = ref(false)
const loaded = ref(false)
const loadError = ref('')
const configRow = ref<ConfigApi.ConfigVO | null>(null)
const form = reactive({
  idleLogoutMinutes: 15
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

const parseIdleLogoutMinutes = (value: unknown) => {
  const normalized = typeof value === 'string' ? value : value == null ? '' : String(value)
  if (!/^[1-9]\d*$/.test(normalized)) {
    throw new Error('登录空闲退出时间必须是 1 到 1440 之间的整数。')
  }
  const minutes = Number(normalized)
  if (
    !Number.isSafeInteger(minutes) ||
    minutes < MIN_IDLE_LOGOUT_MINUTES ||
    minutes > MAX_IDLE_LOGOUT_MINUTES
  ) {
    throw new Error('登录空闲退出时间必须是 1 到 1440 之间的整数。')
  }
  return minutes
}

const applyConfig = (data: ConfigApi.ConfigVO) => {
  if (!data || data.key !== IDLE_LOGOUT_MINUTES_CONFIG_KEY) {
    throw new Error('登录空闲退出配置返回格式无效。')
  }
  if (data.visible !== true) {
    throw new Error('登录空闲退出配置不可见，请先在参数配置中设为可见。')
  }
  form.idleLogoutMinutes = parseIdleLogoutMinutes(data.value)
  configRow.value = data
  loaded.value = true
}

const loadConfig = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await ConfigApi.getConfigPage({
      pageNo: 1,
      pageSize: 2,
      key: IDLE_LOGOUT_MINUTES_CONFIG_KEY
    } as PageParam & { key: string })
    const list = data.list || []
    const rows = list.filter(
      (item: ConfigApi.ConfigVO) => item.key === IDLE_LOGOUT_MINUTES_CONFIG_KEY
    )
    if (list.length !== 1 || rows.length !== 1) {
      throw new Error('未找到唯一的登录空闲退出配置，请联系管理员检查参数配置。')
    }
    applyConfig(rows[0])
  } catch (error) {
    loaded.value = false
    configRow.value = null
    loadError.value = resolveErrorMessage(error, '基础配置加载失败。')
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!configRow.value || !loaded.value) {
    message.error('请先加载基础配置。')
    return
  }
  const minutes = parseIdleLogoutMinutes(form.idleLogoutMinutes)
  saving.value = true
  try {
    await ConfigApi.updateConfig({
      ...configRow.value,
      value: String(minutes)
    })
    message.success('基础配置已保存')
    await loadConfig()
  } catch (error) {
    message.error(resolveErrorMessage(error, '基础配置保存失败。'))
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.profile-basic-config {
  max-width: 920px;
}

.profile-basic-config__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-basic-config__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.profile-basic-config__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.profile-basic-config__alert {
  margin-bottom: 14px;
}

.profile-basic-config__form {
  margin-top: 4px;
}

.profile-basic-config__input-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.profile-basic-config__unit {
  color: #606266;
}
</style>
