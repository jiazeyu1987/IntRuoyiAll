<template>
  <Dialog v-model="visible" title="维护设备工序产能" width="620px">
    <el-alert
      title="此产能由使用该设备与工序的工作站共享。模拟数据请在维护说明中注明“模拟测试”。"
      type="warning"
      :closable="false"
      class="mb-16px"
    />
    <el-form ref="formRef" :model="formData" :rules="rules" label-width="130px" v-loading="loading">
      <el-form-item label="工序" prop="processId">
        <el-select v-model="formData.processId" filterable :disabled="fixedProcess" class="!w-1/1">
          <el-option
            v-for="process in processes"
            :key="process.id"
            :label="`${process.name}（${process.code}）`"
            :value="process.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单台标准小时产能" prop="standardHourlyCapacity">
        <el-input-number
          v-model="formData.standardHourlyCapacity"
          :min="0.000001"
          :precision="6"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item label="维护说明" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :maxlength="500"
          show-word-limit
          placeholder="请说明产能依据；模拟数据请注明模拟测试"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="loading || saving" :loading="saving" @click="submit"
        >保存产能</el-button
      >
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { DvMachineryProcessApi, type DvMachineryProcessVO } from '@/api/mes/dv/machinery/process'
import { ProProcessApi, type ProProcessVO } from '@/api/mes/pro/process'

const message = useMessage()
const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const fixedProcess = ref(false)
const machineryId = ref<number | string>()
const processes = ref<ProProcessVO[]>([])
const rows = ref<DvMachineryProcessVO[]>([])
const formRef = ref()
const formData = ref({
  processId: undefined as number | string | undefined,
  standardHourlyCapacity: undefined as number | undefined,
  remark: ''
})
const rules = {
  processId: [{ required: true, message: '请选择工序', trigger: 'change' }],
  standardHourlyCapacity: [
    { required: true, message: '请填写大于0的单台标准小时产能', trigger: 'blur' }
  ],
  remark: [{ required: true, whitespace: true, message: '请填写产能维护说明', trigger: 'blur' }]
}
const emit = defineEmits(['success'])
watch(
  () => formData.value.processId,
  (id) => {
    const row = rows.value.find((item) => String(item.processId) === String(id))
    formData.value.standardHourlyCapacity = row?.standardHourlyCapacity
    formData.value.remark = row?.remark || ''
  }
)

const open = async (deviceId: number | string, processId?: number | string) => {
  visible.value = true
  loading.value = true
  machineryId.value = deviceId
  fixedProcess.value = processId != null
  rows.value = []
  formData.value = { processId: undefined, standardHourlyCapacity: undefined, remark: '' }
  formRef.value?.clearValidate()
  try {
    const [processList, capacityRows] = await Promise.all([
      ProProcessApi.getProcessSimpleList(),
      DvMachineryProcessApi.getMachineryProcessList(deviceId)
    ])
    processes.value = processList
    rows.value = capacityRows
    if (processId != null) {
      const target = processes.value.find((item) => String(item.id) === String(processId))
      if (!target) {
        const error = new Error('当前工作站工序不在可维护工序列表中，请先核对工序是否启用')
        message.error(error.message)
        throw error
      }
      formData.value.processId = target.id
    }
  } catch (error) {
    visible.value = false
    throw error
  } finally {
    loading.value = false
  }
}
const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    await DvMachineryProcessApi.saveCapacity({
      machineryId: machineryId.value!,
      processId: formData.value.processId!,
      standardHourlyCapacity: formData.value.standardHourlyCapacity!,
      remark: formData.value.remark
    })
    message.success('设备工序产能已保存')
    visible.value = false
    emit('success')
  } finally {
    saving.value = false
  }
}
defineExpose({ open })
</script>
