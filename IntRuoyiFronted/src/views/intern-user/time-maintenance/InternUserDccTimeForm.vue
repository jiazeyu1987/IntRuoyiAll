<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item :label="timeLabel" prop="targetTime">
        <el-date-picker
          v-model="formData.targetTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          :placeholder="`请选择${timeLabel}`"
          class="!w-full"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">保存</el-button>
      <el-button :disabled="formLoading" @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import * as TimeMaintenanceApi from '@/api/intern-user/timeMaintenance'
import { formatDateTimeValue } from '@/utils/formatTime'

defineOptions({ name: 'InternUserDccTimeForm' })

type DccTimeKind = 'publishedTime' | 'obsoletedTime'

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const currentKind = ref<DccTimeKind>('publishedTime')
const formData = reactive<TimeMaintenanceApi.InternUserDccTimeUpdateReqVO>({
  controlledFileId: '',
  targetTime: ''
})
const timeLabel = computed(() => (currentKind.value === 'publishedTime' ? '升版时间' : '作废时间'))
const dialogTitle = computed(() => `修改${timeLabel.value}`)
const formRules = {
  targetTime: [{ required: true, message: '目标时间不能为空', trigger: 'change' }]
}

const emit = defineEmits<{
  success: []
}>()

const open = (
  kind: DccTimeKind,
  row: { id: string; publishedTime?: string | number; obsoletedTime?: string | number }
) => {
  currentKind.value = kind
  formData.controlledFileId = row.id
  const currentValue = kind === 'publishedTime' ? row.publishedTime : row.obsoletedTime
  formData.targetTime = currentValue ? formatDateTimeValue(currentValue, '') : ''
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    if (currentKind.value === 'publishedTime') {
      await TimeMaintenanceApi.updateDccPublishedTime(formData)
    } else {
      await TimeMaintenanceApi.updateDccObsoletedTime(formData)
    }
    message.success(`${timeLabel.value}修改成功`)
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    message.error(error instanceof Error ? error.message : `${timeLabel.value}修改失败`)
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
