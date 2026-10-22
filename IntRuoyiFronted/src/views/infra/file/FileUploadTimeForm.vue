<template>
  <Dialog v-model="dialogVisible" title="修改上传时间" width="420px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="上传时间" prop="createTime">
        <el-date-picker
          v-model="formData.createTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择上传时间"
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

defineOptions({ name: 'InfraFileUploadTimeForm' })

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = reactive<TimeMaintenanceApi.InternUserFileUploadTimeUpdateReqVO>({
  id: 0,
  createTime: ''
})
const formRules = {
  createTime: [{ required: true, message: '上传时间不能为空', trigger: 'change' }]
}

const emit = defineEmits<{
  success: []
}>()

const open = (row: { id: number; createTime: string }) => {
  formData.id = row.id
  formData.createTime = row.createTime
  dialogVisible.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    await TimeMaintenanceApi.updateFileUploadTime(formData)
    message.success('上传时间修改成功')
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '上传时间修改失败')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
