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
            :class="{ 'is-disabled': loading || saving }"
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
              :loading="loading || saving"
              :disabled="loading || saving"
              aria-hidden="true"
              @click.stop
              @change="handleEnabledChange"
            />
            <span class="edhr-recordbook-global-setting__toggle-label">打开记录本</span>
          </div>
        </div>
      </template>

      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        :closable="false"
        show-icon
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  getEdhrRecordbookGlobalSetting,
  updateEdhrRecordbookGlobalSetting
} from '@/api/mes/pro/edhr/recordbookGlobalSetting'

const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const settingEnabled = ref(true)
const previousValue = ref(true)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage : defaultMessage
}

const loadSetting = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const result = await getEdhrRecordbookGlobalSetting()
    settingEnabled.value = result.enabled === true
    previousValue.value = settingEnabled.value
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '记录本全局开关加载失败。')
  } finally {
    loading.value = false
  }
}

const requestEnabledChange = async (nextValue: boolean) => {
  settingEnabled.value = nextValue
  try {
    await ElMessageBox.confirm(
      nextValue
        ? '确认打开记录本？打开后，原本任务配置允许记录本的批次工序可再次选择记录本。'
        : '确认关闭记录本？关闭后，所有批次详情将隐藏“批记录/记录本”切换，并禁止记录本写入。',
      nextValue ? '打开记录本' : '关闭记录本',
      { type: nextValue ? 'info' : 'warning' }
    )
  } catch {
    settingEnabled.value = previousValue.value
    return
  }

  saving.value = true
  try {
    const result = await updateEdhrRecordbookGlobalSetting({ enabled: nextValue })
    settingEnabled.value = result.enabled === true
    previousValue.value = settingEnabled.value
    message.success(settingEnabled.value ? '记录本全局开关已打开' : '记录本全局开关已关闭')
  } catch (error) {
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
