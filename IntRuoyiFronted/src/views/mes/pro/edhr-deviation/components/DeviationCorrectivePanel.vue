<template>
  <el-card shadow="never">
    <template #header>{{ record.correctiveOwnerId ? '整改办理与验证' : '调查验证' }}</template>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="当前状态">{{ statusLabel }}</el-descriptions-item>
      <el-descriptions-item v-if="record.correctiveOwnerId" label="整改责任人">{{ ownerName }}</el-descriptions-item>
      <el-descriptions-item v-if="record.correctiveDueAt" label="整改期限">{{ formatTime(record.correctiveDueAt) }}</el-descriptions-item>
      <el-descriptions-item v-if="record.correctiveResult" label="整改结果">{{ record.correctiveResult }}</el-descriptions-item>
      <el-descriptions-item v-if="record.completedAt" label="实际完成时间">{{ formatTime(record.completedAt) }}</el-descriptions-item>
      <el-descriptions-item v-if="evidenceUrls.length" label="整改证据">
        <div v-for="(url, index) in evidenceUrls" :key="url"><el-link :href="url" target="_blank" rel="noopener noreferrer" type="primary">证据 {{ index + 1 }}</el-link></div>
      </el-descriptions-item>
      <el-descriptions-item v-if="record.verificationContent" :label="record.verificationResult === 'FAIL' ? '退回原因' : '验证意见'">{{ record.verificationContent }}</el-descriptions-item>
    </el-descriptions>
    <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" class="mt-12px" />
    <el-form v-if="!readonly && !closed && record.canSubmitCorrective" ref="formRef" :model="form" :rules="rules" label-width="120px" class="mt-16px" @submit.prevent>
      <el-form-item label="整改结果" prop="result"><el-input v-model="form.result" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="说明实际采取的整改措施及完成情况" /></el-form-item>
      <el-form-item label="实际完成时间" prop="completedAt"><el-date-picker v-model="form.completedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="整改证据"><UploadFile v-model="form.evidenceUrls" :limit="10" :file-type="['pdf', 'doc', 'docx', 'xls', 'xlsx', 'png', 'jpg', 'jpeg', 'txt']" /></el-form-item>
      <el-form-item><el-button size="default" :loading="busy" @click="submit">提交整改</el-button></el-form-item>
    </el-form>
    <el-form v-if="!readonly && !closed && record.canVerifyCorrective" label-width="120px" class="mt-16px" @submit.prevent>
      <el-form-item label="验证意见" required><el-input v-model="comment" type="textarea" :rows="4" maxlength="4000" show-word-limit placeholder="填写验证依据；退回时请说明需补充或修改的内容" /></el-form-item>
      <el-form-item>
        <el-button size="default" :loading="busy" @click="verify(true)">验证通过</el-button>
        <el-button size="default" :loading="busy" @click="verify(false)">{{ record.correctiveOwnerId ? '退回整改' : '退回调查' }}</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { UploadFile } from '@/components/UploadFile'
import { submitDeviationCorrective, verifyDeviationCorrective, type DeviationHandlingRespVO } from '@/api/mes/pro/edhr/deviation'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const props = defineProps<{ record: DeviationHandlingRespVO; ownerName: string; readonly?: boolean; closed?: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const route = useRoute()
const session = useApprovalCenterInvalidationStore()
const busy = ref(false)
const actionError = ref('')
const comment = ref('')
const formRef = ref<FormInstance>()
const evidenceUrls = computed<string[]>(() => {
  if (!props.record.correctiveEvidenceUrlsJson) return []
  const urls: unknown = JSON.parse(props.record.correctiveEvidenceUrlsJson)
  if (!Array.isArray(urls) || urls.some(url => typeof url !== 'string' || !/^https?:\/\//i.test(url))) throw new Error('整改证据记录格式无效')
  return urls
})
const form = reactive({ result: '', completedAt: '', evidenceUrls: [] as string[] })
watch(() => props.record, record => {
  form.result = record.correctiveResult || ''
  form.completedAt = record.completedAt ? dayjs(record.completedAt).format('YYYY-MM-DDTHH:mm:ss') : ''
  form.evidenceUrls = [...evidenceUrls.value]
  comment.value = ''
  actionError.value = ''
}, { immediate: true })
const rules: FormRules = {
  result: [{ required: true, whitespace: true, message: '请填写整改结果', trigger: 'blur' }],
  completedAt: [{ required: true, message: '请选择实际完成时间', trigger: 'change' }]
}
const statusLabel = computed(() => props.closed && !props.record.correctiveStatus ? '历史记录（已关闭）' : props.closed ? '已关闭' : !props.record.investigationSubmitted ? '调查草稿' : ({ DRAFT: '待整改', SUBMITTED: '待验证', RETURNED: '已退回整改', VERIFIED: '验证通过' }[props.record.correctiveStatus || ''] || '尚未提交'))
const formatTime = (value: string | number) => dayjs(value).format('YYYY-MM-DD HH:mm:ss')
const perform = async (action: () => Promise<unknown>, success: string) => {
  if (busy.value) return
  const key = JSON.stringify([session.syncSession().key, route.fullPath, props.record.deviationId, props.record.contentVersion])
  const current = () => key === JSON.stringify([session.syncSession().key, route.fullPath, props.record.deviationId, props.record.contentVersion])
  busy.value = true
  actionError.value = ''
  try {
    await action()
    if (!current()) return
    ElMessage.success(success)
    emit('changed')
  } catch (error: any) {
    if (current()) actionError.value = error?.response?.data?.msg || error?.message || '办理失败，请重试'
  } finally { busy.value = false }
}
const versionPayload = () => ({ deviationId: props.record.deviationId, expectedContentVersion: props.record.contentVersion!, expectedContentHash: props.record.contentHash! })
const submit = async () => {
  if (props.readonly || props.closed || !props.record.canSubmitCorrective || !await formRef.value?.validate().catch(() => false)) return
  await perform(() => submitDeviationCorrective({ ...versionPayload(), result: form.result.trim(), completedAt: form.completedAt, evidenceUrls: form.evidenceUrls.map(url => new URL(url, window.location.origin).href) }), '整改已提交，等待验证')
}
const verify = async (passed: boolean) => {
  if (props.readonly || props.closed || !props.record.canVerifyCorrective) return
  if (!comment.value.trim()) { actionError.value = '请填写验证意见或退回原因'; return }
  await perform(() => verifyDeviationCorrective({ ...versionPayload(), passed, comment: comment.value.trim() }), passed ? '验证通过' : '已退回')
}
</script>
