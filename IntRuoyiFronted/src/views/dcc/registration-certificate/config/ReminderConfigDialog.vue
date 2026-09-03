<template>
  <Dialog v-model="dialogVisible" title="通知设置" width="640px">
    <el-form
      ref="formRef"
      v-loading="loading"
      :model="formData"
      :rules="formRules"
      label-width="150px"
      data-testid="registration-certificate-notification-settings"
    >
      <el-form-item
        v-for="threshold in thresholds"
        :key="threshold.key"
        :label="threshold.label"
        :prop="`thresholdRecipientUserIds.${threshold.key}`"
      >
        <div class="recipient-editor">
          <div class="selected-recipient-tags" data-testid="selected-recipient-tags">
            <el-tag
              v-for="userId in formData.thresholdRecipientUserIds[threshold.key]"
              :key="userId"
              closable
              @close="removeRecipient(threshold.key, userId)"
            >
              {{ formatRecipientLabel(userId) }}
            </el-tag>
          </div>
          <UserSelect
            v-model="recipientPickerValues[threshold.key]"
            class="recipient-picker-trigger"
            placeholder="选择通知接收人"
            @change="(user) => addRecipient(threshold.key, user)"
          />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        v-hasPermi="['dcc:registration-certificate:config:update']"
        type="primary"
        :loading="saving"
        @click="submit"
      >
        保存
      </el-button>
    </template>
  </Dialog>

</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRegistrationCertificateReminderConfig,
  updateRegistrationCertificateReminderConfig,
  type RegistrationCertificateThresholdRecipientUserIds
} from '@/api/dcc/registrationCertificate/reminderConfig'
import { getSimpleUserList, type UserVO } from '@/api/system/user'
import UserSelect from '@/views/system/user/components/UserSelect.vue'

type ThresholdKey = keyof RegistrationCertificateThresholdRecipientUserIds

const thresholds: Array<{ key: ThresholdKey; label: string }> = [
  { key: 'T_30', label: '到期前 30 个月' },
  { key: 'T_8', label: '到期前 8 个月' },
  { key: 'T_2', label: '到期前 2 个月' },
  { key: 'T_1', label: '到期前 1 个月' }
]

const message = useMessage()
const dialogVisible = ref(false)
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const userOptions = ref<UserVO[]>([])
const formData = reactive({
  enabled: true,
  dailyRunTime: '09:00',
  rowVersion: 0,
  thresholdRecipientUserIds: createEmptyRecipients()
})
const recipientPickerValues = reactive<Record<ThresholdKey, number | undefined>>({
  T_30: undefined,
  T_8: undefined,
  T_2: undefined,
  T_1: undefined
})
const formRules: FormRules = Object.fromEntries(
  thresholds.map(({ key }) => [
    `thresholdRecipientUserIds.${key}`,
    [{ required: true, type: 'array', min: 1, message: '请至少选择一名通知接收人', trigger: 'change' }]
  ])
)

const open = async () => {
  dialogVisible.value = true
  loading.value = true
  try {
    const [config, users] = await Promise.all([
      getRegistrationCertificateReminderConfig(),
      getSimpleUserList()
    ])
    userOptions.value = users.filter((user) => user.disabled !== true && user.status === 0)
    formData.enabled = config.enabled
    formData.dailyRunTime = config.dailyRunTime
    formData.rowVersion = config.rowVersion
    for (const { key } of thresholds) {
      formData.thresholdRecipientUserIds[key] = [...(config.thresholdRecipientUserIds[key] || [])]
    }
  } catch (error) {
    dialogVisible.value = false
    throw error
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const updated = await updateRegistrationCertificateReminderConfig({
      enabled: formData.enabled,
      dailyRunTime: formData.dailyRunTime,
      expectedRowVersion: formData.rowVersion,
      thresholdRecipientUserIds: {
        T_30: [...formData.thresholdRecipientUserIds.T_30],
        T_8: [...formData.thresholdRecipientUserIds.T_8],
        T_2: [...formData.thresholdRecipientUserIds.T_2],
        T_1: [...formData.thresholdRecipientUserIds.T_1]
      }
    })
    formData.rowVersion = updated.rowVersion
    message.success('通知设置已保存')
    dialogVisible.value = false
  } finally {
    saving.value = false
  }
}

const addRecipient = (threshold: ThresholdKey, user: UserVO | undefined) => {
  if (!user) return
  if (!formData.thresholdRecipientUserIds[threshold].includes(user.id)) {
    formData.thresholdRecipientUserIds[threshold].push(user.id)
  }
  recipientPickerValues[threshold] = undefined
}

const removeRecipient = (threshold: ThresholdKey, userId: number) => {
  formData.thresholdRecipientUserIds[threshold] = formData.thresholdRecipientUserIds[threshold]
    .filter((currentUserId) => currentUserId !== userId)
}

function createEmptyRecipients(): RegistrationCertificateThresholdRecipientUserIds {
  return { T_30: [], T_8: [], T_2: [], T_1: [] }
}

function formatUserLabel(user: UserVO) {
  return user.deptName ? `${user.nickname}（${user.deptName}）` : user.nickname
}

function formatRecipientLabel(userId: number) {
  const user = userOptions.value.find((candidate) => candidate.id === userId)
  return user ? formatUserLabel(user) : `未识别用户（ID ${userId}）`
}

defineExpose({ open })
</script>

<style scoped>
.recipient-editor {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.selected-recipient-tags {
  display: flex;
  min-height: 24px;
  flex-wrap: wrap;
  gap: 6px;
}

.recipient-picker-trigger {
  width: 100%;
}
</style>
