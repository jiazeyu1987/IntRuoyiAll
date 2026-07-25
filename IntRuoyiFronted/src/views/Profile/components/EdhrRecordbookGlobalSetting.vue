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
          <el-switch
            v-model="settingEnabled"
            :loading="loading || saving"
            active-text="打开记录本"
            inactive-text="关闭记录本"
            @change="handleEnabledChange"
          />
        </div>
      </template>

      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        :closable="false"
        show-icon
      />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="配置键">{{ CONFIG_KEY }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="settingEnabled ? 'success' : 'warning'">
            {{ settingEnabled ? '记录本已打开' : '记录本已关闭' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后更新人">{{ setting?.updatedBy || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最后更新时间">{{ setting?.updatedAt || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import {
  getEdhrRecordbookGlobalSetting,
  updateEdhrRecordbookGlobalSetting,
  type EdhrRecordbookGlobalSettingRespVO
} from '@/api/mes/pro/edhr/recordbookGlobalSetting'

const CONFIG_KEY = 'mes.edhr.recordbook.global.enabled'
const message = useMessage()

const loading = ref(false)
const saving = ref(false)
const loadError = ref('')
const setting = ref<EdhrRecordbookGlobalSettingRespVO>()
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
    setting.value = result
    settingEnabled.value = result.enabled === true
    previousValue.value = settingEnabled.value
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '记录本全局开关加载失败。')
  } finally {
    loading.value = false
  }
}

const handleEnabledChange = async (value: string | number | boolean) => {
  const nextValue = value === true
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
    setting.value = result
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
</style>
