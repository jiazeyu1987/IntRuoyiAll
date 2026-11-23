<template>
  <section ref="formElement" class="signoff-assignment-form" aria-label="部门负责人签名指派" tabindex="-1">
    <h3>部门负责人签名指派</h3>
    <slot name="frozen-evidence" ></slot>
    <el-form label-position="left" label-width="180px" class="signoff-assignment-fields">
      <el-form-item label="所属部门">
        <span>{{ departmentLabel }}</span>
        <el-tag v-if="departmentName?.trim() && departmentCode?.trim()" class="department-code" type="info">{{ departmentCode }}</el-tag>
      </el-form-item>
      <el-form-item label="会签人" required :error="fieldErrors.selected" data-assignment-field="selected">
        <div class="w-full">
          <div class="assignee-picker">
            <div class="assignee-select">
            <el-select v-model="selected" filterable placeholder="搜索并选择本部门会签人" :disabled="busy || assigned" aria-label="会签人">
              <el-option v-for="user in users" :key="user.id" :value="user.id" :label="`${userLabel(user)}${String(user.id) === String(currentUserId) ? '（本人）' : ''}`" />
            </el-select>
            <el-button v-if="selected && !busy && !assigned" class="assignee-clear" text aria-label="清空会签人" @mousedown.prevent @click.stop="selected = ''">×</el-button>
            </div>
            <el-button v-if="selfCandidate" :type="isSelfAssigned ? 'success' : 'primary'" plain :disabled="busy || assigned" @click="selected = selfCandidate.id">{{ isSelfAssigned ? '已指派本人' : '指派本人' }}</el-button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="关联文件与整改" :error="fieldErrors.arrangements" data-assignment-field="arrangements">
        <div class="w-full"><slot name="relation-arrangements" :disabled="busy || assigned"></slot></div>
      </el-form-item>
      <el-form-item label="指派意见（选填）">
        <el-input v-model="reason" type="textarea" :rows="2" name="dcc-signoff-assignment-opinion" autocomplete="off" :readonly="!reasonFocused" @focus="reasonFocused = true" aria-label="指派意见" placeholder="请输入指派意见" :disabled="busy || assigned" />
      </el-form-item>
      <el-form-item label="电子签名" required :error="fieldErrors.password" data-assignment-field="password">
        <div class="signature-area">
          <SignatureCredentialInput v-model="password" name="dcc-signoff-signature-password" allow-pin-visibility assignment-presentation :disabled="busy || assigned" />
          <div class="signature-flow-note">
            <p>下一节点：实际会签人会签。指派本人时同样进入该环节。</p>
            <p>本次补充的文件和整改安排随指派签名提交。</p>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <p v-if="error" data-assignment-field="context" role="alert" tabindex="-1">{{ error }}</p>
    <Teleport to="body">
      <div class="signoff-assignment-actions" aria-label="当前任务操作" :aria-busy="busy">
        <span class="assignment-summary" aria-live="polite">指派给：<strong>{{ selectedUser ? `${userLabel(selectedUser)}${isSelfAssigned ? '（本人）' : ''}` : '未选择' }}</strong> · 补充文件 {{ supplementalCount }} 份 · 需整改 {{ arrangementCount }} 项</span>
        <el-button type="primary" :loading="busy" :disabled="assigned || blocked" @click="submit">{{ busy ? '处理中' : '确认指派并签名' }}</el-button>
        <slot name="actions" :disabled="busy || assigned || blocked"></slot>
      </div>
    </Teleport>
    <p v-if="assigned">指派已保存，等待该部门实际会签人独立签名。</p>
  </section>
</template>
<script setup lang="ts">
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'
import { isWorkflowId, submitSignoffAssignment, type SignoffAssignment, type WorkflowId, type ArrangementCommand } from './workflow-actions'
const props = withDefaults(defineProps<{
  fileId: WorkflowId
  processInstanceId: string
  taskId: string
  departmentName: string
  departmentId?: WorkflowId
  departmentCode?: string
  currentUserId?: WorkflowId
  supplementalCount?: number
  arrangementCount?: number
  users: Array<{ id: WorkflowId; name?: string; username?: string; departmentName?: string }>
  assigned: boolean
  relationArrangements?: ArrangementCommand[]
  arrangementsReady?: boolean
  blocked?: boolean
  save: (request: SignoffAssignment) => Promise<boolean>
}>(), { arrangementsReady: true, supplementalCount: 0, arrangementCount: 0 })
const departmentLabel = computed(() => props.departmentName?.trim() || (props.departmentCode?.trim() ? `部门编码：${props.departmentCode.trim()}` : '部门名称及业务编码未提供'))
const userLabel = (user: (typeof props.users)[number]) => {
  const name = user.name?.trim()
  if (!name) return user.username || '姓名未提供'
  return user.username && user.username !== name && props.users.some(other => String(other.id) !== String(user.id) && other.name?.trim() === name)
    ? `${name}（${user.username}）` : name
}
const emit = defineEmits<{ saved: [context: { fileId: WorkflowId; processInstanceId: string; taskId: string }] }>()
const selected = ref<WorkflowId>('')
const selectedUser = computed(() => props.users.find(user => String(user.id) === String(selected.value)))
const selfCandidate = computed(() => props.users.find(user => String(user.id) === String(props.currentUserId)))
const isSelfAssigned = computed(() => Boolean(selfCandidate.value && String(selected.value) === String(selfCandidate.value.id)))
const password = ref('')
const reason = ref('')
// 签名输入仅在本人聚焦后开放，阻止浏览器把登录账密回填到指派表单。
const reasonFocused = ref(false), passwordFocused = ref(false)
const error = ref('')
const busy = ref(false)
const formElement = ref<HTMLElement>()
const fieldErrors = ref<{ selected?: string; password?: string; arrangements?: string }>({})
const locate = async (field?: string) => {
  await nextTick()
  const target = field ? formElement.value?.querySelector<HTMLElement>(`[data-assignment-field="${field}"]`) : formElement.value
  target?.scrollIntoView({ behavior: 'smooth', block: field ? 'center' : 'start' })
  const input = target?.querySelector<HTMLElement>('input, textarea, button')
  const focusTarget = input || target
  focusTarget?.focus({ preventScroll: true })
}
const showError = (message: string, field: 'selected' | 'password' | 'arrangements' | 'context') => {
  error.value = message
  if (field !== 'context') fieldErrors.value[field] = message
  void locate(field)
}
defineExpose({ scrollToForm: () => locate() })
let requestSequence = 0
let mounted = true
watch(() => [String(props.fileId), props.processInstanceId, props.taskId], () => {
  requestSequence++
  selected.value = ''
  password.value = ''
  reason.value = ''
  reasonFocused.value = false
  passwordFocused.value = false
  error.value = ''
  fieldErrors.value = {}
  busy.value = false
}, { flush: 'sync' })
onBeforeUnmount(() => {
  mounted = false
  requestSequence++
  password.value = ''
})
const submit = async () => {
  if (busy.value || props.assigned || props.blocked) return
  error.value = ''
  fieldErrors.value = {}
  if (!props.users.some(user => String(user.id) === String(selected.value))) {
    showError('请从当前本部门正式候选目录选择会签人', 'selected')
    return
  }
  if (props.arrangementsReady === false) { showError('补充关联或整改安排尚未加载、确认完成，请核对后再签名', 'arrangements'); return }
  if (!password.value.trim()) { showError('请输入完整签名凭据（六位 PIN 或当前登录密码）', 'password'); return }
  if (!isWorkflowId(props.fileId) || !props.processInstanceId?.trim() || !props.taskId?.trim()) {
    showError('会签任务上下文缺失，请刷新任务后重新办理', 'context')
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
    if (result.error) showError(result.error,
      /密码|口令|PIN|凭据/i.test(result.error) ? 'password' : /会签人|候选/.test(result.error) ? 'selected'
        : /整改|关联/.test(result.error) ? 'arrangements' : 'context')
    if (result.success) { password.value = ''; emit('saved', context) }
  } finally { if (mounted && sequence === requestSequence) busy.value = false }
}
</script>

<style scoped>
.signoff-assignment-form { max-width: 1120px; margin: 0 auto; padding-bottom: calc(112px + env(safe-area-inset-bottom)); scroll-margin-top: 72px; }
.department-code { margin-left: 12px; }
.assignee-picker { display: flex; flex-wrap: wrap; gap: 10px; width: 100%; }
.assignee-select { position: relative; flex: 1; min-width: 220px; }
.assignee-select .el-select { width: 100%; }
.assignee-select :deep(.el-select__wrapper) { padding-right: 52px; }
.assignee-clear { position: absolute; top: 50%; right: 28px; transform: translateY(-50%); width: 24px; min-height: 24px; padding: 0; font-size: 18px; }
.signature-area { box-sizing: border-box; width: 100%; padding: 18px; border: 1px solid var(--el-border-color-light); border-radius: 10px; background: var(--el-fill-color-light); }
.signature-flow-note { margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--el-border-color-light); font-size: 13px; color: var(--el-text-color-secondary); }
.signature-flow-note p { margin: 0; }
.signature-flow-note p + p { margin-top: 4px; }
.signoff-assignment-form h3 { margin: 0 0 24px; font-size: 18px; }
.signoff-assignment-fields :deep(.el-form-item) { margin-bottom: 24px; }
.signoff-assignment-actions {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1000;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 24px calc(14px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  box-shadow: 0 -3px 12px rgb(0 0 0 / 6%);
}
.signoff-assignment-actions > span { margin-right: auto; }
@media (max-width: 640px) {
  .signoff-assignment-form { padding-bottom: calc(156px + env(safe-area-inset-bottom)); }
  .signoff-assignment-fields :deep(.el-form-item) { display: block; }
  .signoff-assignment-fields :deep(.el-form-item__label) { width: auto !important; display: block; }
  .signoff-assignment-actions > span { flex-basis: 100%; }
}
</style>
