<template>
  <section data-testid="dcc-checkin-attachments">
    <el-upload
      v-model:file-list="fileList"
      :multiple="true"
      :auto-upload="true"
      :disabled="disabled || !validContext || cleanupLoading"
      :http-request="uploadAttachment"
      :before-remove="removeAttachment"
    >
      <el-button :disabled="disabled || !validContext || cleanupLoading">选择普通附件</el-button>
    </el-upload>
    <div class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
      本次普通附件全部上传并保存成功后才能检入；历史培训记录不随本次上传复制。
    </div>
    <el-alert v-if="error" class="mt-8px" type="error" :closable="false" :title="error" />
    <el-button v-if="cleanupFailures.length" class="mt-8px" :disabled="disabled" :loading="cleanupLoading" @click="retryCleanup">
      重试临时附件清理
    </el-button>
    <ul v-if="entries.length" class="mt-8px pl-16px">
      <li v-for="entry in entries" :key="entry.uid">{{ entry.fileName }}：{{ statusText(entry.status) }}</li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import type { UploadRequestOptions, UploadProps } from 'element-plus'
import {
  uploadControlledFilePreview, cleanupControlledFileUploadTicket, createControlledFileUploadSessionId,
  type ControlledFileUploadPreviewContext, type ControlledFileUploadRespVO
} from '@/api/dcc/controlledFile/workflow'

const props = defineProps<{
  context?: ControlledFileUploadPreviewContext
  contextKey: string
  disabled?: boolean
}>()
type Entry = { uid: string | number; fileName: string; status: 'UPLOADING' | 'READY' | 'FAILED' | 'CLEANING'; upload?: ControlledFileUploadRespVO }
type CleanupFailure = { fileName: string; upload: ControlledFileUploadRespVO }
const emit = defineEmits<{ state: [state: { ready: boolean; uploading: boolean; cleaning: boolean; error: string; count: number }] }>()
const fileList = ref<NonNullable<UploadProps['fileList']>>([])
const entries = ref<Entry[]>([]), error = ref(''), cleanupFailures = ref<CleanupFailure[]>([]), cleanupCount = ref(0)
let generation = 0, mounted = true
const identity = (value: unknown): string => {
  if ((typeof value !== 'string' && typeof value !== 'number') || (typeof value === 'number' && !Number.isSafeInteger(value))
      || !/^[1-9][0-9]*$/.test(String(value)) || BigInt(String(value)) > 9223372036854775807n)
    throw new Error('检入附件缺少准确的文件版本或类别身份')
  return String(value)
}
const contextFingerprint = () => JSON.stringify([props.contextKey, props.context])
const causeText = (cause: unknown): string => cause instanceof Error ? cause.message : String(cause)
const contextValidationError = computed(() => {
  try {
    if (!props.contextKey?.trim() || !props.context || props.context.uploadContext !== 'CHECKIN' || !props.context.sessionId?.trim())
      return '请先核对检入源文件的正式上下文'
    identity(props.context.controlledFileId); identity(props.context.categoryId); return ''
  } catch (cause) { return causeText(cause) }
})
const validContext = computed(() => !contextValidationError.value)
const cleanupLoading = computed(() => cleanupCount.value > 0)
const uploading = computed(() => entries.value.some(entry => entry.status === 'UPLOADING'))
const ready = computed(() => validContext.value && !error.value && !cleanupLoading.value && !cleanupFailures.value.length
  && entries.value.every(entry => entry.status === 'READY' && entry.upload))
const statusText = (status: Entry['status']) => ({ UPLOADING: '上传中', READY: '已上传', FAILED: '上传失败', CLEANING: '清理中' })[status]
watch(() => [ready.value, uploading.value, cleanupLoading.value, error.value, entries.value.length], () => {
  emit('state', { ready: ready.value, uploading: uploading.value, cleaning: cleanupLoading.value,
    error: error.value || contextValidationError.value, count: entries.value.length })
}, { immediate: true })

const clean = async (upload: ControlledFileUploadRespVO) => {
  if (!upload.uploadTicket?.trim() || !upload.sessionId?.trim() || !upload.requestId?.trim())
    throw new Error('临时附件缺少准确上传票据、会话或请求身份，无法清理')
  const result = await cleanupControlledFileUploadTicket(upload.sessionId, upload.uploadTicket, upload.requestId)
  if (result.cleanupStatus !== 'CLEANED' || result.bindable !== false) throw new Error('临时附件未确认清理，请重试')
}
const rememberCleanupFailure = (fileName: string, upload: ControlledFileUploadRespVO, cause: unknown) => {
  if (!cleanupFailures.value.some(row => row.upload.uploadTicket === upload.uploadTicket)) cleanupFailures.value.push({ fileName, upload })
  error.value = `${fileName} 清理失败：${causeText(cause)}`
}
const cleanRetired = async (rows: Entry[]) => {
  for (const row of rows) {
    if (!row.upload) continue
    cleanupCount.value++
    try { await clean(row.upload) }
    catch (cause) { rememberCleanupFailure(row.fileName, row.upload, cause) }
    finally { cleanupCount.value-- }
  }
}
watch(contextFingerprint, () => {
  generation++; const retired = entries.value; entries.value = []; fileList.value = []; error.value = ''
  if (props.disabled && retired.length) {
    error.value = '检入保存期间上下文已变化，原附件票据保留供原版本结果核对'
    return
  }
  void cleanRetired(retired)
})

const uploadAttachment = async (options: UploadRequestOptions) => {
  if (props.disabled) { options.onError(new Error('检入正在保存，不能变更普通附件') as Parameters<typeof options.onError>[0]); return }
  if (!validContext.value || cleanupLoading.value || cleanupFailures.value.length) {
    error.value = '请先核对检入源文件上下文并完成临时附件清理'
    options.onError(new Error(error.value) as Parameters<typeof options.onError>[0]); return
  }
  const currentGeneration = generation, fingerprint = contextFingerprint()
  const uid = options.file.uid
  if (entries.value.some(row => String(row.uid) === String(uid))) {
    error.value = '同一附件已在上传清单中，请先移除失败项后重试'; options.onError(new Error(error.value) as Parameters<typeof options.onError>[0]); return
  }
  const context = { ...props.context!, sessionId: createControlledFileUploadSessionId() }
  const row: Entry = { uid, fileName: options.file.name, status: 'UPLOADING' }
  entries.value.push(row); error.value = ''
  let uploaded: ControlledFileUploadRespVO | undefined
  const current = () => mounted && currentGeneration === generation && fingerprint === contextFingerprint()
  try {
    uploaded = await uploadControlledFilePreview(options.file, 'ATTACHMENT', context)
    if (!current()) {
      cleanupCount.value++
      try { await clean(uploaded) } catch (cause) { rememberCleanupFailure(row.fileName, uploaded, cause) }
      finally { cleanupCount.value-- }
      options.onError(new Error('检入上下文已改变，原附件已退出当前清单') as Parameters<typeof options.onError>[0]); return
    }
    if (!uploaded.uploadTicket?.trim() || !uploaded.requestId?.trim() || uploaded.fileName !== options.file.name
        || uploaded.fileSize !== options.file.size || !uploaded.contentType?.trim()
        || !new RegExp(`^dcc-checkin:${identity(context.controlledFileId)}:[a-f0-9]{32}$`).test(uploaded.sessionId))
      throw new Error('附件响应与本次文件、精确版本或正式票据会话不一致')
    if (entries.value.some(item => String(item.uid) !== String(uid)
      && (item.upload?.uploadTicket === uploaded!.uploadTicket || item.upload?.sessionId === uploaded!.sessionId)))
      throw new Error('普通附件响应返回重复票据或会话，请重新核对独立上传槽位')
    const actual = entries.value.find(item => String(item.uid) === String(uid))!
    actual.upload = uploaded; actual.status = 'READY'; options.onSuccess(uploaded)
  } catch (cause) {
    if (current()) {
      const actual = entries.value.find(item => String(item.uid) === String(uid))
      if (actual) { actual.status = 'FAILED'; actual.upload = uploaded }
      error.value = `${row.fileName} 上传失败：${causeText(cause)}`
    }
    options.onError(new Error(causeText(cause)) as Parameters<typeof options.onError>[0])
  }
}
const removeEntry = async (uid: Entry['uid']): Promise<boolean> => {
  if (props.disabled || cleanupLoading.value) return false
  const row = entries.value.find(item => String(item.uid) === String(uid))
  if (!row || row.status === 'UPLOADING') return false
  if (row.upload) {
    const oldStatus = row.status; row.status = 'CLEANING'; cleanupCount.value++
    try { await clean(row.upload) }
    catch (cause) { row.status = oldStatus; rememberCleanupFailure(row.fileName, row.upload, cause); return false }
    finally { cleanupCount.value-- }
    cleanupFailures.value = cleanupFailures.value.filter(item => item.upload.uploadTicket !== row.upload!.uploadTicket)
  }
  entries.value = entries.value.filter(item => item !== row)
  if (!cleanupFailures.value.length) error.value = entries.value.some(item => item.status === 'FAILED') ? '仍有普通附件上传失败，请移除后重新上传' : ''
  return true
}
const removeAttachment: NonNullable<UploadProps['beforeRemove']> = async (file, _uploadFiles) =>
  await removeEntry(file.uid)
const retryCleanup = async () => {
  if (props.disabled || cleanupLoading.value) return false
  const failed = [...cleanupFailures.value]
  for (const row of failed) {
    cleanupCount.value++
    try {
      await clean(row.upload); cleanupFailures.value = cleanupFailures.value.filter(item => item !== row)
      entries.value = entries.value.filter(item => item.upload?.uploadTicket !== row.upload.uploadTicket)
    } catch (cause) { rememberCleanupFailure(row.fileName, row.upload, cause) }
    finally { cleanupCount.value-- }
  }
  if (!cleanupFailures.value.length) error.value = entries.value.some(item => item.status === 'FAILED') ? '仍有普通附件上传失败' : ''
  return !cleanupFailures.value.length
}
const readReadyManifest = (): ControlledFileUploadRespVO[] => {
  if (!ready.value) throw new Error(error.value || entries.value.find(row => row.status !== 'READY')?.fileName + ' 普通附件尚未上传完成，不能检入')
  return entries.value.map(row => ({ ...row.upload! }))
}
const cleanupAll = async (): Promise<boolean> => {
  if (props.disabled || uploading.value || cleanupLoading.value) return false
  if (!(await retryCleanup())) return false
  for (const row of [...entries.value]) if (!(await removeEntry(row.uid))) return false
  fileList.value = []; return true
}
const markSaved = () => { generation++; entries.value = []; fileList.value = []; error.value = '' }
onBeforeUnmount(() => {
  mounted = false; generation++
  // A caller may already be binding these tickets. Do not delete a possibly committed selection.
  if (!props.disabled) void cleanRetired(entries.value)
  entries.value = []
})
defineExpose({ readReadyManifest, cleanupAll, markSaved })
</script>
