<template>
  <el-card class="profile-basic-config" data-testid="profile-basic-config" shadow="never">
    <template #header>
      <div class="profile-basic-config__header">
        <div>
          <div class="profile-basic-config__title">基础配置</div>
          <div class="profile-basic-config__subtitle">
            配置密码有效期、登录空闲退出时间和测试入口的显示方式。
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
      data-testid="profile-password-max-age-setting"
      label-width="150px"
      :disabled="passwordMaxAgeLoading || passwordMaxAgeSaving"
    >
      <el-form-item label="密码有效期">
        <div>
          <div class="profile-basic-config__password-controls">
            <el-input
              v-model="passwordMaxAgeDays"
              class="profile-basic-config__password-input"
              inputmode="numeric"
              aria-label="密码有效期（天）"
              :disabled="!canUpdate || !passwordMaxAgeRow"
            >
              <template #append>天</template>
            </el-input>
            <el-button
              type="primary"
              :loading="passwordMaxAgeSaving"
              :disabled="!canUpdate || !passwordMaxAgeRow"
              @click="savePasswordMaxAge"
            >
              保存密码有效期
            </el-button>
            <el-button :loading="passwordMaxAgeLoading" @click="loadPasswordMaxAge">
              刷新密码有效期
            </el-button>
          </div>
          <div class="profile-basic-config__subtitle">
            对所有租户的系统账号统一生效，从上次修改密码的时间起算；请输入正整数天数。
            缩短有效期可能使现有密码立即过期，下次登录或身份校验时将要求修改密码。
          </div>
          <el-alert
            v-if="passwordMaxAgeError"
            :title="passwordMaxAgeError"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
      </el-form-item>
    </el-form>

    <el-form
      class="profile-basic-config__form"
      label-width="150px"
      :disabled="loading || saving"
    >
      <el-form-item label="测试模式">
        <div>
          <el-switch
            v-model="testModeValue"
            :loading="testModeSaving"
            :disabled="!canUpdate || !testModeRow || testModeSaving"
            active-text="开启"
            inactive-text="关闭"
            aria-label="测试模式"
            data-testid="profile-test-mode-switch"
            @change="saveTestMode"
          />
          <div class="profile-basic-config__subtitle">
            开启后显示测试、模拟及测试数据清理入口；关闭后隐藏。对系统所有账号生效，其他会话刷新后生效；仍须满足原有操作权限。
          </div>
          <el-alert v-if="testModeError" :title="testModeError" type="error" :closable="false" show-icon />
        </div>
      </el-form-item>
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
        <el-button :loading="loading" @click="loadConfig(); loadTestMode()">刷新</el-button>
        <el-button
          type="primary"
          :loading="saving"
          :disabled="!canUpdate || !loaded"
          @click="handleSave"
        >
          保存空闲退出时间
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import * as ConfigApi from '@/api/infra/config'
import { parseTestMode, TEST_MODE_CONFIG_KEY, useTestModeStore } from '@/store/modules/testMode'

const props = defineProps<{
  canUpdate: boolean
}>()

const message = useMessage()
const testModeStore = useTestModeStore()
const testModeRow = ref<ConfigApi.ConfigVO | null>(null)
const testModeValue = ref(false)
const testModeSaving = ref(false)
const testModeError = ref('')

const loadTestMode = async () => {
  testModeRow.value = null
  testModeError.value = ''
  testModeStore.apply(null)
  try {
    const data = await ConfigApi.getConfigPage({
      pageNo: 1, pageSize: 2, key: TEST_MODE_CONFIG_KEY
    } as PageParam & { key: string })
    const rows = data.list.filter((item: ConfigApi.ConfigVO) => item.key === TEST_MODE_CONFIG_KEY)
    if (rows.length !== 1 || !rows[0].visible) {
      throw new Error('未找到唯一且可见的测试模式配置，请先执行测试模式配置迁移。')
    }
    testModeValue.value = parseTestMode(rows[0].value)
    testModeStore.apply(testModeValue.value)
    testModeRow.value = rows[0]
  } catch (error) {
    testModeError.value = resolveErrorMessage(error, '测试模式配置读取失败。')
    message.error(testModeError.value)
  }
}

const saveTestMode = async () => {
  if (!testModeRow.value || testModeSaving.value) return
  const previousValue = parseTestMode(testModeRow.value.value)
  testModeSaving.value = true
  testModeError.value = ''
  try {
    const value = String(testModeValue.value)
    await ConfigApi.updateConfig({ ...testModeRow.value, value })
    testModeRow.value = { ...testModeRow.value, value }
    testModeStore.apply(testModeValue.value)
    message.success(testModeValue.value ? '测试模式已开启' : '测试模式已关闭')
  } catch (error) {
    testModeValue.value = previousValue
    testModeError.value = resolveErrorMessage(error, '测试模式保存失败。')
    message.error(testModeError.value)
  } finally {
    testModeSaving.value = false
  }
}

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

const PASSWORD_MAX_AGE_CONFIG_KEY = 'system.user.password-max-age-days'
const passwordMaxAgeRow = ref<ConfigApi.ConfigVO | null>(null)
const passwordMaxAgeDays = ref('')
const passwordMaxAgeLoading = ref(false)
const passwordMaxAgeSaving = ref(false)
const passwordMaxAgeError = ref('')

const validatePasswordMaxAge = (value: string) => {
  if (
    typeof value !== 'string' ||
    value !== value.trim() ||
    !/^[1-9][0-9]{0,9}$/.test(value) ||
    Number(value) > 2147483647
  ) {
    throw new Error('密码有效期必须为正整数天数，且不能超过 2147483647 天。')
  }
}

const loadPasswordMaxAge = async () => {
  if (passwordMaxAgeLoading.value || passwordMaxAgeSaving.value) return
  passwordMaxAgeLoading.value = true
  passwordMaxAgeRow.value = null
  passwordMaxAgeDays.value = ''
  passwordMaxAgeError.value = ''
  try {
    const data = await ConfigApi.getConfigPage({
      pageNo: 1,
      pageSize: 2,
      key: PASSWORD_MAX_AGE_CONFIG_KEY
    } as PageParam & { key: string })
    const rows = data.list.filter(
      (item: ConfigApi.ConfigVO) => item.key === PASSWORD_MAX_AGE_CONFIG_KEY
    )
    if (rows.length !== 1) {
      throw new Error('未找到唯一的密码有效期配置，请联系管理员检查配置并完成初始化。')
    }
    validatePasswordMaxAge(rows[0].value)
    passwordMaxAgeRow.value = rows[0]
    passwordMaxAgeDays.value = rows[0].value
  } catch (error) {
    passwordMaxAgeError.value = resolveErrorMessage(error, '密码有效期配置加载失败。')
    message.error(passwordMaxAgeError.value)
  } finally {
    passwordMaxAgeLoading.value = false
  }
}

const savePasswordMaxAge = async () => {
  if (passwordMaxAgeLoading.value || passwordMaxAgeSaving.value) return
  passwordMaxAgeError.value = ''
  if (!props.canUpdate) {
    message.error('当前账号没有基础配置修改权限。')
    return
  }
  if (!passwordMaxAgeRow.value) {
    message.error('请先成功加载密码有效期配置。')
    return
  }
  try {
    validatePasswordMaxAge(passwordMaxAgeDays.value)
    passwordMaxAgeSaving.value = true
    const updated = { ...passwordMaxAgeRow.value, value: passwordMaxAgeDays.value }
    await ConfigApi.updateConfig(updated)
    passwordMaxAgeRow.value = updated
    message.success('密码有效期已保存')
  } catch (error) {
    passwordMaxAgeError.value = resolveErrorMessage(error, '密码有效期保存失败。')
    message.error(passwordMaxAgeError.value)
  } finally {
    passwordMaxAgeSaving.value = false
  }
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
  if (!props.canUpdate) {
    message.error('无权修改基础配置。')
    return
  }
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
    message.success('登录空闲退出时间已保存')
    await loadConfig()
  } catch (error) {
    message.error(resolveErrorMessage(error, '基础配置保存失败。'))
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
onMounted(loadTestMode)
onMounted(loadPasswordMaxAge)
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

.profile-basic-config__password-controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.profile-basic-config__password-input {
  width: 180px;
}
</style>
