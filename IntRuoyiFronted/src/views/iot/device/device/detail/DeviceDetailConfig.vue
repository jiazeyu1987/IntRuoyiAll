<!-- 设备配置 -->
<template>
  <div>
    <el-alert
      title="支持远程更新设备的配置资料，可以在下方编辑配置模板，对设备的系统参数、网络参数等进行远程配置。配置完成后，需点击「下发」按钮，设备即可进行远程配置。"
      type="info"
      show-icon
      class="my-4"
      description="如需编辑文件，请点击下方编辑按钮"
    />
    <el-alert v-if="configError" :title="configError" type="error" :closable="false" />
    <BusinessJsonNode v-else-if="isEditing" v-model="config" ref="configEditor" />
    <ReadableData v-else-if="!configError" title="设备配置" :source="JSON.stringify(config)" />
    <div v-if="!configError" class="mt-5 text-center">
      <el-button v-if="isEditing" @click="cancelEdit">取消</el-button>
      <el-button v-if="isEditing" type="primary" @click="saveConfig" :disabled="hasJsonError">
        保存
      </el-button>
      <el-button v-else @click="enableEdit">编辑</el-button>
      <el-button v-if="!isEditing" type="success" @click="handleConfigPush" :loading="pushLoading">
        配置推送
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { DeviceApi, DeviceVO } from '@/api/iot/device/device'
import { IotDeviceMessageMethodEnum } from '@/views/iot/utils/constants'
import BusinessJsonNode from '@/components/BusinessData/BusinessJsonNode.vue'
import ReadableData from '@/components/BusinessData/ReadableData.vue'

defineOptions({ name: 'DeviceDetailConfig' })

const props = defineProps<{
  device: DeviceVO
}>()

const emit = defineEmits<{
  (e: 'success'): void // 定义 success 事件，不需要参数
}>()

const message = useMessage()
const loading = ref(false) // 加载中
const pushLoading = ref(false) // 推送加载中
const config = ref<unknown>()
const configError = ref('')
const configEditor = ref<InstanceType<typeof BusinessJsonNode>>()
const hasJsonError = ref(false)
const isEditing = ref(false)
const validateConfigNumbers = (value: unknown): void => {
  if (typeof value === 'number' && (!Number.isFinite(value) || (Number.isInteger(value) && !Number.isSafeInteger(value))))
    throw new Error('设备配置包含超出精度范围的数值，请核查源配置中的编号类型')
  if (value !== null && typeof value === 'object') Object.values(value).forEach(validateConfigNumbers)
}
const loadConfig = () => {
  configError.value = ''
  config.value = undefined
  isEditing.value = false
  try {
    if (!props.device.config?.trim()) throw new Error('设备尚未提供配置资料')
    const parsed = JSON.parse(props.device.config)
    validateConfigNumbers(parsed)
    config.value = parsed
  } catch (error) {
    configError.value =
      error instanceof SyntaxError ? '设备配置资料格式错误，请核查源配置' : (error as Error).message
  }
}
watch(() => props.device.config, loadConfig, { immediate: true })

/** 启用编辑模式的函数 */
const enableEdit = () => {
  if (configError.value) return
  isEditing.value = true
  hasJsonError.value = false // 重置错误状态
}

/** 取消编辑的函数 */
const cancelEdit = () => {
  loadConfig()
  isEditing.value = false
  hasJsonError.value = false // 重置错误状态
}

/** 保存配置的函数 */
const saveConfig = async () => {
  if (configError.value) return
  const error = configEditor.value?.validate()
  if (error) {
    message.error(error)
    return
  }
  if (hasJsonError.value) {
    message.error('JSON格式错误，请修正后再提交！')
    return
  }
  await updateDeviceConfig()
  isEditing.value = false
}

/** 配置推送处理函数 */
const handleConfigPush = async () => {
  if (configError.value) return
  try {
    // 二次确认
    await message.confirm('确定要推送配置到设备吗？此操作将远程更新设备配置。', '配置推送确认')

    pushLoading.value = true

    // 调用配置推送接口
    await DeviceApi.sendDeviceMessage({
      deviceId: props.device.id,
      method: IotDeviceMessageMethodEnum.CONFIG_PUSH.method,
      params: config.value
    })

    message.success('配置推送成功！')
  } catch (error) {
    if (error !== 'cancel') {
      message.error('配置推送失败！')
      console.error('配置推送错误:', error)
    }
  } finally {
    pushLoading.value = false
  }
}

/** 更新设备配置 */
const updateDeviceConfig = async () => {
  try {
    // 提交请求
    loading.value = true
    await DeviceApi.updateDevice({
      id: props.device.id,
      config: JSON.stringify(config.value)
    } as DeviceVO)
    message.success('更新成功！')
    // 触发 success 事件
    emit('success')
  } finally {
    loading.value = false
  }
}
</script>
