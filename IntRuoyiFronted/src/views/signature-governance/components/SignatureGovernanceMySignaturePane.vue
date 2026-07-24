<template>
  <section class="signature-my-pane">
    <el-alert
      v-if="inlineError"
      :closable="false"
      class="signature-my-pane__alert"
      show-icon
      type="error"
      :title="inlineError"
    />

    <div class="signature-my-pane__toolbar">
      <div class="signature-my-pane__title">签名图片</div>
      <div class="signature-my-pane__actions" data-testid="dcc-my-signature-image-actions">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept="image/png,image/jpeg"
          :on-change="handleSignatureImageFileChange"
        >
          <el-button :loading="signatureImageState.uploading" type="primary">
            上传图片
          </el-button>
        </el-upload>
        <el-button
          :disabled="!mySignatureImage || mySignatureImage.active"
          :loading="signatureImageState.enabling"
          @click="handleEnableSignatureImage"
        >
          启用图片
        </el-button>
        <el-button
          :disabled="!mySignatureImage?.active"
          :loading="signatureImageState.disabling"
          @click="handleDisableSignatureImage"
        >
          停用图片
        </el-button>
      </div>
    </div>

    <div v-loading="signatureImageState.loading" class="signature-my-pane__body">
      <el-empty v-if="!mySignatureImage" description="当前未启用签名图片" />
      <div v-else class="signature-my-pane__detail">
        <div class="signature-my-pane__preview">
          <el-image
            v-if="signatureImagePreviewUrl"
            :src="signatureImagePreviewUrl"
            :preview-src-list="signatureImagePreviewList"
            fit="contain"
            preview-teleported
          />
          <span v-else>无预览</span>
        </div>
        <div class="signature-my-pane__fields">
          <div class="signature-my-pane__field">
            <span>状态</span>
            <el-tag :type="mySignatureImage.active ? 'success' : 'info'" size="small">
              {{ mySignatureImage.active ? '已启用' : '未启用' }}
            </el-tag>
          </div>
          <div class="signature-my-pane__field">
            <span>版本</span>
            <strong>{{ mySignatureImage.versionNo || '-' }}</strong>
          </div>
          <div class="signature-my-pane__field">
            <span>文件</span>
            <strong>{{ mySignatureImage.fileName || mySignatureImage.fileId || '-' }}</strong>
          </div>
          <div class="signature-my-pane__field">
            <span>图片 hash</span>
            <strong>{{ formatDccHashShort(mySignatureImage.sha256Short || mySignatureImage.sha256) }}</strong>
          </div>
          <div class="signature-my-pane__field">
            <span>上传时间</span>
            <strong>{{ formatSignatureImageDateTime(mySignatureImage.uploadedAt) }}</strong>
          </div>
          <div class="signature-my-pane__field">
            <span>启用时间</span>
            <strong>{{ formatSignatureImageDateTime(mySignatureImage.enabledAt) }}</strong>
          </div>
        </div>
      </div>
    </div>

  </section>
</template>

<script lang="ts" setup>
import type { UploadFile } from 'element-plus'
import {
  disableDccElectronicSignatureImage,
  enableDccElectronicSignatureImage,
  getMyDccElectronicSignatureImage,
  uploadDccElectronicSignatureImage,
  type DccElectronicSignatureImageVO
} from '@/api/dcc/controlledFile/signatures'
import { formatDateTimeValue } from '@/utils/formatTime'
import { formatDccHashShort } from '@/views/dcc/controlled-file/shared/signature-evidence'

defineOptions({ name: 'SignatureGovernanceMySignaturePane' })

const message = useMessage()

const mySignatureImage = ref<DccElectronicSignatureImageVO | null>(null)
const inlineError = ref('')
const signatureImageState = reactive({
  loading: false,
  uploading: false,
  enabling: false,
  disabling: false
})

const resolveSignatureImagePreviewUrl = (fileUrl?: string) => {
  const value = fileUrl?.trim()
  if (!value) return ''
  if (/^(https?:|blob:|data:)/i.test(value)) return value
  return value.startsWith('/') ? value : `/${value}`
}

const signatureImagePreviewUrl = computed(() =>
  resolveSignatureImagePreviewUrl(mySignatureImage.value?.fileUrl)
)

const signatureImagePreviewList = computed(() =>
  signatureImagePreviewUrl.value ? [signatureImagePreviewUrl.value] : []
)

const formatSignatureImageDateTime = (value?: string | number | Date | null) => {
  return formatDateTimeValue(value, '-', '时间格式错误')
}

const resolveSignatureImageErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim() && error.message !== 'error') return error.message
  if (typeof error === 'string' && error.trim() && error !== 'error') return error
  return defaultMessage
}

const setInlineError = (error: unknown, defaultMessage: string) => {
  inlineError.value = resolveSignatureImageErrorMessage(error, defaultMessage)
  message.error(inlineError.value)
}

const loadMySignatureImage = async () => {
  signatureImageState.loading = true
  inlineError.value = ''
  try {
    mySignatureImage.value = await getMyDccElectronicSignatureImage()
  } catch (error) {
    setInlineError(error, '签名图片加载失败，请查看错误提示后重试。')
  } finally {
    signatureImageState.loading = false
  }
}

const isAllowedSignatureImageFile = (file: File) => ['image/png', 'image/jpeg'].includes(file.type)

const handleSignatureImageFileChange = async (uploadFile: UploadFile) => {
  const rawFile = uploadFile.raw
  if (!rawFile) {
    message.error('上传签名图片缺少文件')
    return
  }
  if (!isAllowedSignatureImageFile(rawFile)) {
    message.error('签名图片仅支持 PNG/JPEG')
    return
  }
  signatureImageState.uploading = true
  inlineError.value = ''
  try {
    const uploaded = await uploadDccElectronicSignatureImage(rawFile, '用户上传签名图片')
    mySignatureImage.value = await enableDccElectronicSignatureImage(uploaded.id, '用户启用签名图片')
    message.success('签名图片已上传并启用')
  } catch (error) {
    setInlineError(error, '签名图片上传失败，请查看错误提示后重试。')
  } finally {
    signatureImageState.uploading = false
  }
}

const handleEnableSignatureImage = async () => {
  if (!mySignatureImage.value?.id) {
    message.error('启用签名图片缺少图片 ID')
    return
  }
  signatureImageState.enabling = true
  inlineError.value = ''
  try {
    mySignatureImage.value = await enableDccElectronicSignatureImage(
      mySignatureImage.value.id,
      '用户启用签名图片'
    )
    message.success('签名图片已启用')
  } catch (error) {
    setInlineError(error, '启用签名图片失败，请查看错误提示后重试。')
  } finally {
    signatureImageState.enabling = false
  }
}

const handleDisableSignatureImage = async () => {
  signatureImageState.disabling = true
  inlineError.value = ''
  try {
    mySignatureImage.value = await disableDccElectronicSignatureImage('用户停用签名图片')
    message.success('签名图片已停用')
  } catch (error) {
    setInlineError(error, '停用签名图片失败，请查看错误提示后重试。')
  } finally {
    signatureImageState.disabling = false
  }
}

onMounted(() => {
  void loadMySignatureImage()
})
</script>

<style scoped>
.signature-my-pane {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.signature-my-pane__alert {
  margin-bottom: 0;
}

.signature-my-pane__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0 0 12px;
}

.signature-my-pane__title {
  color: #172033;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
}

.signature-my-pane__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.signature-my-pane__body {
  padding: 16px;
  background: #ffffff;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.signature-my-pane__body {
  min-height: 220px;
}

.signature-my-pane__detail {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.signature-my-pane__preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 136px;
  padding: 10px;
  overflow: hidden;
  color: #4b5563;
  background: #f7f9fc;
  border: 1px dashed #c8d3e3;
  border-radius: 8px;
}

.signature-my-pane__preview :deep(.el-image) {
  width: 100%;
  height: 136px;
}

.signature-my-pane__fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
}

.signature-my-pane__field {
  min-width: 0;
  padding: 10px 12px;
  background: #fafcff;
  border: 1px solid #edf1f6;
  border-radius: 6px;
}

.signature-my-pane__field span,
.signature-my-pane__field strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.signature-my-pane__field span {
  margin-bottom: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.signature-my-pane__field strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 20px;
}

@media (max-width: 760px) {
  .signature-my-pane__toolbar {
    align-items: flex-start;
  }

  .signature-my-pane__actions {
    justify-content: flex-start;
  }

  .signature-my-pane__detail,
  .signature-my-pane__fields {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
