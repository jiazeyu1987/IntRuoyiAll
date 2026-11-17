<template>
  <template v-if="nodes.length">
    <el-button size="default" :disabled="disabled" @click="open">退回调查</el-button>
    <el-dialog v-model="visible" title="退回偏差调查" width="520px" destroy-on-close append-to-body>
      <el-alert title="退回后，当前版本的签署和关闭任务失效，调查接收人须修订后重新提交。" type="warning" :closable="false" class="mb-12px" />
      <el-form label-width="100px" :disabled="submitting">
        <el-form-item label="本人职责" required>
          <el-select v-model="form.node"><el-option v-for="item in nodes" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        </el-form-item>
        <el-form-item label="退回原因" required><el-input v-model="form.reason" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
      <template #footer><el-button size="default" :disabled="submitting" @click="visible = false">取消</el-button><el-button size="default" :loading="submitting" @click="submit">确认退回</el-button></template>
    </el-dialog>
  </template>
</template>
<script setup lang="ts">
import { returnDeviationToInvestigation } from '@/api/mes/pro/edhr/deviationReview'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { createFormalActionGuard } from '@/store/modules/approvalCenterInvalidationState'

const props = defineProps<{
  deviationId: number
  contentVersion?: number
  contentHash?: string
  nodes: Array<{ value: string; label: string }>
  disabled?: boolean
}>()
const emit = defineEmits<{ returned: [] }>()
const session = useApprovalCenterInvalidationStore()
const route = useRoute()
const message = useMessage()
const visible = ref(false)
const submitting = ref(false)
const error = ref('')
const form = reactive({ node: '', reason: '' })
const guard = createFormalActionGuard(() => JSON.stringify([
  session.syncSession().key, route.fullPath, props.deviationId, props.contentVersion, props.contentHash
]))
const reset = () => { guard.invalidate(); submitting.value = false; error.value = ''; visible.value = false }
watch(() => [props.deviationId, props.contentVersion, props.contentHash, route.fullPath, session.syncSession().key], reset, { flush: 'sync' })
onBeforeUnmount(() => guard.invalidate())
const open = () => {
  if (props.disabled || !props.nodes.length) return
  guard.invalidate()
  form.node = props.nodes[0].value
  form.reason = ''
  error.value = ''
  visible.value = true
}
const submit = async () => {
  if (submitting.value || props.disabled) return
  const reason = form.reason.trim()
  if (!reason) { error.value = '请填写退回原因'; return }
  if (!props.contentVersion || !props.contentHash || !props.nodes.some(node => node.value === form.node)) {
    error.value = '当前版本或本人审核职责已变化，请刷新详情'
    return
  }
  const isCurrent = guard.capture()
  submitting.value = true
  error.value = ''
  try {
    await returnDeviationToInvestigation({ deviationId: props.deviationId, expectedContentVersion: props.contentVersion,
      expectedContentHash: props.contentHash, node: form.node, reason })
    if (!isCurrent()) return
    visible.value = false
    message.success('已退回调查，原版本审核任务已失效')
    emit('returned')
  } catch (cause: any) {
    if (isCurrent()) error.value = cause?.response?.data?.msg || cause?.message || '退回调查失败，请重试'
  } finally {
    if (isCurrent()) submitting.value = false
  }
}
</script>
