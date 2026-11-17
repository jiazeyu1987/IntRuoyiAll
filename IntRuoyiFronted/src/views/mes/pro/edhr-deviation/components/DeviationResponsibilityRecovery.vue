<template>
  <el-button size="default" :disabled="disabled" @click="open">{{ mode === 'assign' ? '补录调查接收人' : '撤回调查调整责任人' }}</el-button>
  <el-dialog v-model="visible" :title="mode === 'assign' ? '补录调查接收人' : '撤回调查'" width="540px" append-to-body destroy-on-close :close-on-click-modal="!submitting" :close-on-press-escape="!submitting" :show-close="!submitting">
    <el-alert :title="mode === 'assign' ? '请依据实际责任分工选择接收人，系统不会自动指定历史责任人。' : '撤回后，当前版本的验证、签署及关闭任务失效。调查接收人调整并重新提交后，须重新整改及验证。历史证据仍保留。'" type="warning" :closable="false" class="mb-12px" />
    <el-form label-width="100px" :disabled="submitting" @submit.prevent>
      <el-form-item v-if="mode === 'assign'" label="调查接收人" required>
        <el-select v-model="form.receiverId" :loading="loading" :disabled="loading || !!candidateError" filterable placeholder="请选择实际调查接收人" class="w-full">
          <el-option v-for="user in candidates" :key="user.id" :value="user.id" :label="`${user.nickname}（${user.username}）`" />
        </el-select>
        <el-alert v-if="candidateError" :title="candidateError" type="error" :closable="false" />
        <el-alert v-else-if="loaded && !candidates.length" title="暂无合格偏差管理员，请先配置有效接收人。" type="warning" :closable="false" />
        <el-button v-if="!loading" link type="primary" @click="loadCandidates">刷新候选人</el-button>
      </el-form-item>
      <el-form-item :label="mode === 'assign' ? '补录原因' : '撤回原因'" required>
        <el-input v-model="form.reason" type="textarea" :rows="4" maxlength="1000" show-word-limit />
      </el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <template #footer><el-button :disabled="submitting" @click="visible = false">取消</el-button><el-button :loading="submitting" :disabled="mode === 'assign' && (loading || !!candidateError || !candidates.length)" @click="submit">{{ mode === 'assign' ? '确认补录' : '确认撤回' }}</el-button></template>
  </el-dialog>
</template>
<script setup lang="ts">
import { assignDeviationReceiver, getDeviationRecoveryReceivers, withdrawDeviationInvestigation, type DeviationRecoveryReceiver } from '@/api/mes/pro/edhr/deviationRecovery'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { createFormalActionGuard } from '@/store/modules/approvalCenterInvalidationState'

const props = defineProps<{
  mode: 'assign' | 'withdraw'
  deviationId: number
  deviationVersion?: number
  contentVersion?: number
  contentHash?: string
  disabled?: boolean
}>()
const emit = defineEmits<{ changed: [] }>()
const route = useRoute()
const session = useApprovalCenterInvalidationStore()
const message = useMessage()
const visible = ref(false), submitting = ref(false), loading = ref(false), loaded = ref(false)
const error = ref(''), candidateError = ref('')
const candidates = ref<DeviationRecoveryReceiver[]>([])
const form = reactive({ receiverId: undefined as number | string | undefined, reason: '' })
const context = () => JSON.stringify([session.syncSession().key, route.fullPath, props.mode, props.deviationId, props.deviationVersion, props.contentVersion, props.contentHash])
const guard = createFormalActionGuard(context)
let candidateGeneration = 0
const reset = () => {
  guard.invalidate(); candidateGeneration++; visible.value = false; submitting.value = false; loading.value = false
  error.value = ''; candidateError.value = ''; candidates.value = []; loaded.value = false
  form.receiverId = undefined; form.reason = ''
}
watch(context, reset, { flush: 'sync' })
watch(visible, value => { if (!value) { guard.invalidate(); candidateGeneration++; loading.value = false } })
onBeforeUnmount(() => { guard.invalidate(); candidateGeneration++ })
const loadCandidates = async () => {
  const generation = ++candidateGeneration, key = context()
  const current = () => visible.value && generation === candidateGeneration && key === context()
  loading.value = true; loaded.value = false; candidateError.value = ''; candidates.value = []
  try {
    const result = await getDeviationRecoveryReceivers()
    if (!current()) return
    if (!Array.isArray(result)) throw new Error('接收人候选列表格式无效')
    candidates.value = result; loaded.value = true
  } catch (cause: any) {
    if (current()) candidateError.value = cause?.response?.data?.msg || cause?.message || '接收人候选列表加载失败'
  } finally { if (current()) loading.value = false }
}
const open = () => {
  if (props.disabled) return
  reset(); visible.value = true
  if (props.mode === 'assign') void loadCandidates()
}
const submit = async () => {
  if (submitting.value || props.disabled || !visible.value) return
  const reason = form.reason.trim()
  if (!reason) { error.value = '请填写原因'; return }
  if (props.mode === 'assign' && (loading.value || candidateError.value || !props.deviationVersion
    || !candidates.value.some(user => String(user.id) === String(form.receiverId)))) {
    error.value = '请选择有效接收人，并确认偏差版本有效'; return
  }
  if (props.mode === 'withdraw' && (!props.contentVersion || !props.contentHash)) {
    error.value = '调查版本已变化，请刷新详情'; return
  }
  const current = guard.capture()
  submitting.value = true; error.value = ''
  try {
    if (props.mode === 'assign') {
      const assigned = await assignDeviationReceiver({ deviationId: props.deviationId, expectedDeviationVersion: props.deviationVersion!, receiverId: form.receiverId!, reason })
      if (assigned !== true) throw new Error('接收人补录未成功，请刷新确认')
    } else {
      await withdrawDeviationInvestigation({ deviationId: props.deviationId, expectedContentVersion: props.contentVersion!, expectedContentHash: props.contentHash!, reason })
    }
    if (!current()) return
    submitting.value = false; visible.value = false
    message.success(props.mode === 'assign' ? '已补录调查接收人并派发待办' : '已撤回调查，请调整后重新提交')
    emit('changed')
  } catch (cause: any) {
    if (current()) error.value = cause?.response?.data?.msg || cause?.message || '办理失败，请重试'
  } finally { if (current()) submitting.value = false }
}
</script>
