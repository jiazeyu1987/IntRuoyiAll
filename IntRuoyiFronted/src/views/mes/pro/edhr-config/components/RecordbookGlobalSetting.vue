<template>
  <div class="edhr-recordbook-global-setting">
    <el-card shadow="never">
      <template #header>
        <div class="edhr-recordbook-global-setting__header">
          <div>
            <div class="edhr-recordbook-global-setting__title">eDHR 记录本全局开关</div>
            <div class="edhr-recordbook-global-setting__subtitle">
              仅金手指用户可配置；关闭后所有批次统一走批记录流程。
            </div>
          </div>
          <div
            class="edhr-recordbook-global-setting__toggle"
            :class="{ 'is-disabled': loading || saving || !loaded }"
            role="button"
            tabindex="0"
            :aria-pressed="settingEnabled"
            aria-label="切换 eDHR 记录本全局开关"
            @click="handleToggleClick"
            @keydown.enter.space.prevent="handleToggleClick"
          >
            <span class="edhr-recordbook-global-setting__toggle-label">关闭记录本</span>
            <el-switch
              v-model="settingEnabled"
              :loading="loading || saving || !loaded"
              :disabled="loading || saving || !loaded"
              aria-hidden="true"
              @click.stop
              @change="handleEnabledChange"
            />
            <span class="edhr-recordbook-global-setting__toggle-label">打开记录本</span>
          </div>
        </div>
      </template>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-button :disabled="loading || saving" @click="loadSetting">刷新</el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { hasExplicitConfigPermission } from '@/utils/businessConfigAccess'
import { ElMessageBox } from 'element-plus'
import {
  getEdhrRecordbookGlobalSetting,
  updateEdhrRecordbookGlobalSetting
} from '@/api/mes/pro/edhr/recordbookGlobalSetting'

const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const loaded = ref(false)
const validateSetting = (result: { enabled: boolean; configKey: string }) => {
  if (typeof result?.enabled !== 'boolean' || result.configKey !== 'mes.edhr.recordbook.global.enabled') {
    throw new Error('记录本全局开关返回配置无效。')
  }
}
const settingEnabled = ref(true)
const previousValue = ref(true)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseMessage =
    (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim()
    ? responseMessage
    : defaultMessage
}

const loadSetting = async () => {
  if (saving.value || loading.value) return
  loaded.value = false
  loading.value = true
  loadError.value = ''
  try {
    const result = await getEdhrRecordbookGlobalSetting()
    validateSetting(result)
    settingEnabled.value = result.enabled
    previousValue.value = settingEnabled.value
    loaded.value = true
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '记录本全局开关加载失败。')
  } finally {
    loading.value = false
  }
}

const requestEnabledChange = async (nextValue: boolean) => {
  if (loading.value || saving.value || !loaded.value) {
    settingEnabled.value = previousValue.value
    return
  }
  if (!hasExplicitConfigPermission('mes:pro-batch-record-execution:golden-finger')) {
    settingEnabled.value = previousValue.value
    message.error('无权修改记录本全局开关。')
    return
  }
  saving.value = true
  settingEnabled.value = nextValue
  try {
    await ElMessageBox.confirm(
      nextValue
        ? '确认打开记录本？打开后，原本任务配置允许记录本的批次工序可再次选择记录本。'
        : '确认关闭记录本？关闭后，所有批次详情将隐藏“批记录/记录本”切换，并禁止记录本写入。',
      nextValue ? '打开记录本' : '关闭记录本',
      { type: nextValue ? 'info' : 'warning' }
    )
  } catch (error) {
    settingEnabled.value = previousValue.value
    saving.value = false
    if (error !== 'cancel' && error !== 'close') {
      message.error(resolveErrorMessage(error, '记录本开关确认失败。'))
    }
    return
  }

  if (!loaded.value || !hasExplicitConfigPermission('mes:pro-batch-record-execution:golden-finger')) {
    settingEnabled.value = previousValue.value
    saving.value = false
    message.error('无权修改记录本全局开关。')
    return
  }
  try {
    const result = await updateEdhrRecordbookGlobalSetting({ enabled: nextValue })
    validateSetting(result)
    settingEnabled.value = result.enabled
    previousValue.value = settingEnabled.value
    message.success(settingEnabled.value ? '记录本全局开关已打开' : '记录本全局开关已关闭')
  } catch (error) {
    loaded.value = false
    settingEnabled.value = previousValue.value
    message.error(resolveErrorMessage(error, '记录本全局开关接口保存失败。'))
  } finally {
    saving.value = false
  }
}

const handleEnabledChange = async (value: string | number | boolean) => {
  await requestEnabledChange(value === true)
}

const handleToggleClick = async () => {
  if (loading.value || saving.value) return
  await requestEnabledChange(!settingEnabled.value)
}

onMounted(loadSetting)
</script>

<style scoped>
.edhr-recordbook-global-setting {
  max-width: 860px;
}

.edhr-recordbook-global-setting__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.edhr-recordbook-global-setting__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.edhr-recordbook-global-setting__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.edhr-recordbook-global-setting__toggle {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  padding: 4px 10px;
  border-radius: 6px;
  color: #4b5563;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.edhr-recordbook-global-setting__toggle:hover,
.edhr-recordbook-global-setting__toggle:focus-visible {
  background-color: #f3f8ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.18);
  outline: none;
}

.edhr-recordbook-global-setting__toggle.is-disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.edhr-recordbook-global-setting__toggle-label {
  font-size: 14px;
  line-height: 1;
  user-select: none;
}
</style>
