<template>
  <el-card v-if="certificateType === 'DOMESTIC' || certificateType === 'CE'" class="mt-4">
    <template #header>延续支持材料</template>
    <el-alert v-if="visualState === 'T_8' || visualState === 'CE_T_12'" title="请上传延续支持材料，上传成功后将刷新提醒状态" type="warning" :closable="false" />
    <p>{{ certificateType === 'CE' ? '上传合同或报价单，作为CE延续支持材料。' : '上传受理单或发补单，作为国内延续支持材料。' }}</p>
    <el-select v-model="documentType" aria-label="材料类型" :disabled="loading">
      <el-option v-for="item in options" :key="item.value" :value="item.value" :label="item.label" />
    </el-select>
    <input ref="input" type="file" aria-label="选择支持材料" :disabled="loading" @change="selectFile" />
    <el-button v-hasPermi="['dcc:registration-certificate:renewal:upload']" type="primary"
      :disabled="!file || !documentType" :loading="loading" @click="submit">上传支持材料</el-button>
  </el-card>
</template>
<script setup lang="ts">
import { createSecureUuid } from '@/utils/uuid'
import { uploadSupportingDocument } from '@/api/dcc/registrationCertificate/supportingDocuments'
const props = defineProps<{ certificateId: string | number; versionId: string | number; certificateType: string; visualState?: string }>()
const emit = defineEmits<{ uploaded: [] }>()
const message = useMessage()
const loading = ref(false)
const file = ref<File>()
const input = ref<HTMLInputElement>()
const documentType = ref('')
const options = computed(() => props.certificateType === 'CE'
  ? [{ value: 'CE_RENEWAL_CONTRACT', label: '合同' }, { value: 'CE_RENEWAL_QUOTATION', label: '报价单' }]
  : [{ value: 'RENEWAL_ACCEPTANCE_RECEIPT', label: '受理单' }, { value: 'RENEWAL_SUPPLEMENT_NOTICE', label: '发补单' }])
watch(() => [props.certificateId, props.versionId, props.certificateType], () => {
  file.value = undefined; documentType.value = ''; if (input.value) input.value.value = ''
})
const selectFile = (event: Event) => { file.value = (event.target as HTMLInputElement).files?.[0] }
const submit = async () => {
  if (loading.value || !file.value || !documentType.value) return
  if (file.value.size === 0) { message.error('不能上传空文件'); return }
  loading.value = true
  try {
    const data = new FormData()
    data.append('versionId', String(props.versionId)); data.append('documentType', documentType.value)
    data.append('file', file.value)
    await uploadSupportingDocument(props.certificateId, data, createSecureUuid())
    message.success('支持材料已上传'); file.value = undefined
    if (input.value) input.value.value = ''
    emit('uploaded')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '支持材料上传失败，请检查后重试')
  } finally { loading.value = false }
}
</script>
