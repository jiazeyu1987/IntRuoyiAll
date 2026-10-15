<template>
  <section data-testid="dcc-delayed-effective-confirmation" aria-label="延迟生效确认">
    <p>文件生效时间由编制人根据文件输入、输出及来源之间的衔接关系确定。</p>
    <p>生效时间不得早于本次确认时间。选择时间后须完成电子签名。</p>
    <el-form label-width="120px" :disabled="busy || saved || !context.canConfirm">
      <el-form-item label="生效时间" required>
        <el-date-picker v-model="selectedEffectiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss" placeholder="请选择生效时间" />
        <span>北京时间（Asia/Shanghai）</span>
      </el-form-item>
      <el-form-item label="签名原因" required>
        <el-input v-model="reason" type="textarea" maxlength="500" placeholder="请填写本次确认原因" />
      </el-form-item>
      <el-form-item label="电子签名密码" required>
        <el-input v-model="password" type="password" show-password autocomplete="off" placeholder="当前账号密码" />
      </el-form-item>
    </el-form>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="!context.canConfirm">{{ context.unavailableReason || '当前用户不可办理本轮延迟生效确认' }}</p>
    <p v-if="saved">延迟生效确认及电子签名已保存。</p>
    <el-button type="primary" :loading="busy" :disabled="saved || !context.canConfirm" @click="confirmEffective">电子签名确认生效时间</el-button>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { generateUUID } from '@/utils'
import { buildDelayedEffectiveConfirmationCommand, validateDelayedEffectiveConfirmationResult,
  type DelayedEffectiveConfirmationContext, type DelayedEffectiveConfirmationCommand,
  type DelayedEffectiveConfirmationResult } from '@/api/dcc/controlledFile/applicationRead'
const props = defineProps<{
  context: DelayedEffectiveConfirmationContext
  save: (command: DelayedEffectiveConfirmationCommand) => Promise<DelayedEffectiveConfirmationResult>
}>()
const emit = defineEmits<{ saved: [result: DelayedEffectiveConfirmationResult] }>()
const selectedEffectiveTime = ref(''), password = ref(''), reason = ref(''), error = ref('')
const busy = ref(false), saved = ref(false), idempotencyKey = ref(generateUUID())
let generation = 0, mounted = true
watch(() => [props.context.controlledFileId, props.context.taskId, props.context.processInstanceId,
  props.context.attributeRound, props.context.processDefinitionId, props.context.definitionDigest, props.context.versionNo], () => {
  generation++; selectedEffectiveTime.value = ''; password.value = ''; reason.value = ''; error.value = ''
  busy.value = false; saved.value = false; idempotencyKey.value = generateUUID()
}, { flush: 'sync' })
onBeforeUnmount(() => { mounted = false; generation++; password.value = '' })
const confirmEffective = async () => {
  if (busy.value || saved.value) return
  const token = generation, context = { ...props.context }
  busy.value = true; error.value = ''
  try {
    const command = buildDelayedEffectiveConfirmationCommand(context, selectedEffectiveTime.value,
      password.value, reason.value, idempotencyKey.value)
    const result = await props.save(command)
    if (!mounted || token !== generation) return
    validateDelayedEffectiveConfirmationResult(context, command, result)
    password.value = ''; saved.value = true; emit('saved', result)
  } catch (cause) {
    if (mounted && token === generation) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (mounted && token === generation) busy.value = false }
}
</script>
