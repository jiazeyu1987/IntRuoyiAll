<template>
  <section>
    <slot name="frozen-evidence" ></slot>
    <p>{{ departmentName }}：先签名指派，再由被指派人正式会签。可以指派本人。</p>
    <el-select v-model="selected" placeholder="选择本部门会签人" :disabled="busy || assigned">
      <el-option v-for="user in users" :key="String(user.id)" :label="user.name" :value="user.id" />
    </el-select>
    <el-input v-model="reason" placeholder="指派意见" :disabled="busy || assigned" />
    <el-input v-model="password" type="password" show-password placeholder="当前账号签名密码" :disabled="busy || assigned" />
    <slot name="relation-arrangements" :disabled="busy || assigned"></slot>
    <p v-if="error" role="alert">{{ error }}</p>
    <el-button :loading="busy" :disabled="assigned" @click="submit">签名确认指派</el-button>
    <p v-if="assigned">指派已保存，等待该部门实际会签人独立签名。</p>
  </section>
</template>
<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { isWorkflowId, submitSignoffAssignment, type SignoffAssignment, type WorkflowId, type ArrangementCommand } from './workflow-actions'
const props = withDefaults(defineProps<{
  fileId: WorkflowId
  processInstanceId: string
  taskId: string
  departmentName: string
  users: Array<{ id: WorkflowId; name: string }>
  assigned: boolean
  relationArrangements?: ArrangementCommand[]
  arrangementsReady?: boolean
  save: (request: SignoffAssignment) => Promise<boolean>
}>(), { arrangementsReady: true })
const emit = defineEmits<{ saved: [context: { fileId: WorkflowId; processInstanceId: string; taskId: string }] }>()
const selected = ref<WorkflowId>('')
const password = ref('')
const reason = ref('')
const error = ref('')
const busy = ref(false)
let requestSequence = 0
let mounted = true
watch(() => [String(props.fileId), props.processInstanceId, props.taskId], () => {
  requestSequence++
  selected.value = ''
  password.value = ''
  reason.value = ''
  error.value = ''
  busy.value = false
}, { flush: 'sync' })
onBeforeUnmount(() => {
  mounted = false
  requestSequence++
  password.value = ''
})
const submit = async () => {
  if (busy.value || props.assigned) return
  if (props.arrangementsReady === false) { error.value = '整改安排尚未加载或校验完成，请核对后再签名'; return }
  if (!isWorkflowId(props.fileId) || !props.processInstanceId?.trim() || !props.taskId?.trim()) {
    error.value = '会签任务上下文缺失，请刷新任务后重新办理'
    return
  }
  const sequence = ++requestSequence
  const context = { fileId: props.fileId, processInstanceId: props.processInstanceId, taskId: props.taskId }
  busy.value = true
  try {
    const result = await submitSignoffAssignment({ taskId: props.taskId, assigneeUserId: selected.value,
      password: password.value, reason: reason.value, relationArrangements: props.relationArrangements }, props.save)
    if (!mounted || sequence !== requestSequence || String(context.fileId) !== String(props.fileId)
      || context.processInstanceId !== props.processInstanceId || context.taskId !== props.taskId) return
    error.value = result.error || ''
    if (result.success) { password.value = ''; emit('saved', context) }
  } finally { if (mounted && sequence === requestSequence) busy.value = false }
}
</script>
