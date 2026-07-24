<template>
  <el-dialog v-model="dialogVisible" title="新增升级任务" width="800px" append-to-body>
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="100px"
      v-loading="formLoading"
    >
      <el-form-item label="任务名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入任务名称" />
      </el-form-item>
      <el-form-item label="任务描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述"
        />
      </el-form-item>
      <el-form-item label="升级范围" prop="deviceScope">
        <el-select v-model="formData.deviceScope" placeholder="请选择升级范围" class="w-full">
          <el-option
            v-for="item in Object.values(IoTOtaTaskDeviceScopeEnum)"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="formData.deviceScope === IoTOtaTaskDeviceScopeEnum.SELECT.value"
        label="选择设备"
        prop="deviceIds"
      >
        <el-select
          v-model="formData.deviceIds"
          multiple
          placeholder="请选择设备"
          class="w-full"
          filterable
          reserve-keyword
        >
          <el-option
            v-for="device in devices"
            :key="device.id"
            :label="device.nickname ? `${device.deviceName} (${device.nickname})` : device.deviceName"
            :value="device.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确认</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus'
import { DeviceApi, DeviceVO } from '@/api/iot/device/device'
import { IoTOtaTaskApi, OtaTask } from '@/api/iot/ota/task'
import { IoTOtaTaskDeviceScopeEnum, IoTOtaTaskStatusEnum } from '@/views/iot/utils/constants'

defineOptions({ name: 'OtaTaskForm' })

const props = defineProps<{
  firmwareId: number
  productId: number
}>()

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const createDefaultFormData = (): OtaTask => ({
  name: '',
  deviceScope: IoTOtaTaskDeviceScopeEnum.ALL.value,
  firmwareId: props.firmwareId,
  status: IoTOtaTaskStatusEnum.IN_PROGRESS.value,
  description: '',
  deviceIds: [] as number[]
})
const formData = ref<OtaTask>(createDefaultFormData())
const formRef = ref()
const formRules = reactive<FormRules<OtaTask>>({
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  deviceScope: [{ required: true, message: '请选择升级范围', trigger: 'change' }],
  deviceIds: [{ required: true, message: '请至少选择一个设备', trigger: 'change' }]
})
const devices = ref<DeviceVO[]>([])

const open = async () => {
  dialogVisible.value = true
  resetForm()
  devices.value = (await DeviceApi.getDeviceListByProductId(props.productId)) || []
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    await IoTOtaTaskApi.createOtaTask(formData.value)
    message.success('创建成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
  formRef.value?.resetFields()
}
</script>
