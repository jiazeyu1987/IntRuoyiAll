<template>
  <div data-flow-panel="version-process-sop">
    <div class="mb-8px">
      <span>版本SOP：</span>
      <el-link v-if="fileUrl" :href="fileUrl" target="_blank" rel="noopener noreferrer" type="primary">
        {{ resolveUrlPathFileName(fileUrl) }}
      </el-link>
      <span v-else>该版本未绑定SOP文件</span>
    </div>
    <template v-if="editable">
      <el-upload
        :show-file-list="false"
        :disabled="disabled || uploading"
        :before-upload="beforeUpload"
        :http-request="uploadSop"
        accept=".pdf,.png,.jpg,.jpeg,.webp"
      >
        <el-button :disabled="disabled || uploading" :loading="uploading" size="small" type="primary" plain>
          {{ fileUrl ? '更换SOP' : '上传SOP' }}
        </el-button>
      </el-upload>
      <el-button v-if="fileUrl" class="mt-8px" :disabled="disabled || uploading" size="small" link type="danger" @click="emit('change', null)">
        解除绑定
      </el-button>
      <p class="text-12px text-[var(--el-text-color-secondary)]">
        支持 PDF、PNG、JPG、JPEG、WebP，小于 5MB。保存后仅修改当前候选版本，审批发布后生效。
      </p>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { UploadProps, UploadRequestOptions } from 'element-plus'
import { useUpload } from '@/components/UploadFile/src/useUpload'
import { resolveUrlPathFileName } from '@/utils/fileName'
import { createSecureUuid } from '@/utils/uuid'

const props = defineProps<{
  fileUrl?: string | null
  editable: boolean
  disabled: boolean
}>()
const emit = defineEmits<{
  change: [url: string | null]
  uploading: [value: boolean]
}>()
const message = useMessage()
const uploading = ref(false)
let mounted = true
// 每次组件使用独立文件目录，更换文件不会覆盖已发布版本引用的内容。
const setUploading = (value: boolean) => {
  uploading.value = value
  emit('uploading', value)
}
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  if (!props.editable || props.disabled || uploading.value) return false
  if (!/\.(pdf|png|jpe?g|webp)$/i.test(file.name)) {
    message.error('请选择 PDF 或 PNG、JPG、JPEG、WebP 图片')
    return false
  }
  if (file.size >= 5 * 1024 * 1024) {
    message.error('SOP 文件必须小于 5MB')
    return false
  }
  return true
}
const uploadSop = async (options: UploadRequestOptions) => {
  setUploading(true)
  try {
    const { httpRequest } = useUpload(`mes/route-sop/${createSecureUuid()}`)
    const result = await httpRequest(options)
    if (!mounted) throw new Error('工序已切换，请在目标工序重新上传')
    if (typeof result?.data !== 'string' || !result.data.trim()) throw new Error('上传响应缺少 SOP 文件地址')
    emit('change', result.data)
    message.success('SOP 已上传，请保存当前候选版本')
    return result
  } catch (error) {
    if (mounted) message.error(error instanceof Error ? error.message : 'SOP 上传失败')
    throw error
  } finally {
    if (mounted) setUploading(false)
  }
}
onBeforeUnmount(() => {
  mounted = false
  emit('uploading', false)
})
</script>
