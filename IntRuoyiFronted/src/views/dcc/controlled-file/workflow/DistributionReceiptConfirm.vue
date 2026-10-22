<template>
  <section data-testid="dcc-exact-distribution-receipt">
    <p>本次下发版本：{{ versionNo }}。电子签名确认收到此版本。</p>
    <p>打开正文、签名收到分别记录；下载、打印和文件维护仍按各自权限办理。</p>
    <el-input v-model="password" type="password" show-password autocomplete="off" placeholder="当前账号密码" :disabled="busy || !canAcknowledge" />
    <el-input v-model="comment" type="textarea" maxlength="1000" placeholder="签收意见（选填）" :disabled="busy || !canAcknowledge" />
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="saved">电子签名已保存，请刷新读取正式签名时间。</p>
    <el-button type="primary" :loading="busy" :disabled="!canAcknowledge || saved" @click="confirmReceipt">电子签名确认收到</el-button>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import type { WorkflowId } from './workflow-actions'
import { submitDistributionReceipt, type DistributionReceiptCommand } from './distribution-facts'
const props = defineProps<{
  controlledFileId: WorkflowId; distributionId: WorkflowId; recipientId: WorkflowId; versionNo: string
  contextKey: string; canAcknowledge: boolean; save: (request: DistributionReceiptCommand) => Promise<boolean>
}>()
const emit = defineEmits<{ saved: [request: { controlledFileId: WorkflowId; distributionId: WorkflowId; recipientId: WorkflowId }] }>()
const password = ref(''), comment = ref(''), error = ref(''), busy = ref(false), saved = ref(false)
let generation = 0, mounted = true
watch(() => [props.contextKey, String(props.controlledFileId), String(props.distributionId), String(props.recipientId), props.versionNo], () => {
  generation++; password.value = ''; comment.value = ''; error.value = ''; busy.value = false; saved.value = false
}, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; generation++; password.value = '' })
const confirmReceipt = async () => {
  if (busy.value || saved.value || !props.canAcknowledge) return
  const current = generation
  const request = { controlledFileId: props.controlledFileId, distributionId: props.distributionId, recipientId: props.recipientId,
    versionNo: props.versionNo, password: password.value, comment: comment.value }
  busy.value = true; error.value = ''
  try {
    await submitDistributionReceipt(request, props.save)
    if (!mounted || current !== generation) return
    password.value = ''; saved.value = true
    emit('saved', { controlledFileId: request.controlledFileId, distributionId: request.distributionId, recipientId: request.recipientId })
  } catch (cause) {
    if (mounted && current === generation) error.value = cause instanceof Error ? cause.message : '电子签名保存失败'
  } finally { if (mounted && current === generation) busy.value = false }
}
</script>
