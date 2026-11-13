<template>
  <div ref="spacer" :style="{ height: `${height}px` }" aria-hidden="true"></div>
  <div ref="bar" class="supervisor-review-actions" :style="{ left: `${left}px`, width: `${width}px` }" data-testid="dcc-supervisor-review-actions" role="region" :aria-label="`${taskName || '审核'}操作`">
    <div class="supervisor-review-actions__task">
      <strong>当前任务：{{ taskName || '直属主管审核' }}</strong>
      <div v-if="blockedReason || loading || error || blockerText" role="status" aria-live="polite">{{ blockedReason || (loading ? '正在校验审核条件' : error || blockerText) }}</div>
      <el-button v-if="!blockedReason && !loading && (error || !readiness?.ready)" link type="primary" :disabled="submitting" @click="loadReadiness">处理后重新校验</el-button>
    </div>
    <div class="supervisor-review-actions__buttons">
      <el-button type="primary" :loading="submitting" :disabled="!canApprove" @click="$emit('approve')">{{ approveText || '审核通过' }}</el-button>
      <el-button type="danger" plain :disabled="!canReject" @click="$emit('reject')">驳回</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getControlledFileTaskActionReadiness, type ControlledFileTaskReadinessVO } from '@/api/dcc/controlledFile/workflow'
import { resolveReadSideErrorMessage } from './presentation'

const props = defineProps<{
  fileId: string
  taskId: string
  taskName: string
  approveText?: string
  contextKey: string
  blockedReason: string
  submitting: boolean
}>()
defineEmits<{ approve: []; reject: [] }>()
const bar = ref<HTMLElement>()
const spacer = ref<HTMLElement>()
const height = ref(120)
const left = ref(0), width = ref(0)
const readiness = ref<ControlledFileTaskReadinessVO>()
const loading = ref(false)
const error = ref('')
const canReject = computed(() => !props.blockedReason && !!props.taskId && !props.submitting)
const canApprove = computed(() => canReject.value && !loading.value && !error.value && readiness.value?.ready === true)
const blockerText = computed(() => readiness.value?.ready ? '' :
  `${readiness.value?.blockers.map(item => item.message).join('；') || '审核条件尚未满足'}。请按提示处理后重新校验。`)
let generation = 0
const loadReadiness = async () => {
  const current = ++generation
  readiness.value = undefined
  error.value = ''
  loading.value = false
  if (props.blockedReason || !props.taskId) return
  loading.value = true
  try {
    const result = await getControlledFileTaskActionReadiness(props.fileId, { taskId: props.taskId })
    if (current === generation) readiness.value = result
  } catch (cause) {
    if (current === generation) error.value = `${resolveReadSideErrorMessage(cause, '审核条件校验失败')}；请处理错误后重新校验。`
  } finally {
    if (current === generation) loading.value = false
  }
}
watch(() => [props.contextKey, props.fileId, props.taskId, props.blockedReason], loadReadiness, { immediate: true })
let observer: ResizeObserver | undefined
onMounted(() => {
  observer = new ResizeObserver(() => {
    const bounds = spacer.value!.getBoundingClientRect()
    left.value = bounds.left
    width.value = bounds.width
    height.value = bar.value!.getBoundingClientRect().height + 16
  })
  observer.observe(bar.value!)
  observer.observe(spacer.value!)
})
onBeforeUnmount(() => { generation++; observer?.disconnect() })
</script>

<style scoped>
.supervisor-review-actions {
  position: fixed;
  z-index: 100;
  bottom: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px calc(16px + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  box-shadow: 0 -2px 12px rgb(0 0 0 / 8%);
}
.supervisor-review-actions__task { min-width: 0; line-height: 1.7; overflow-wrap: anywhere; }
.supervisor-review-actions__buttons { display: flex; flex-shrink: 0; }
@media (max-width: 640px) {
  .supervisor-review-actions { flex-wrap: wrap; padding: 12px 12px calc(12px + env(safe-area-inset-bottom, 0px)); }
}
</style>
