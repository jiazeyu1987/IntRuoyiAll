<template>
  <ContentWrap>
    <div class="flex items-start justify-between gap-16px">
      <div>
        <div class="flex items-center gap-10px">
          <div class="text-24px font-700">{{ task?.title || '培训任务' }}</div>
          <el-tag :type="getTrainingProgressStatusTagType(task?.status)">
            {{ getTrainingProgressStatusLabel(task?.status) }}
          </el-tag>
        </div>
        <div class="mt-8px flex flex-wrap items-center gap-10px text-13px text-[var(--el-text-color-secondary)]">
          <span>文件编号：{{ task?.fileNumber || '-' }}</span>
          <span>版本：{{ task?.versionNo || '-' }}</span>
          <span>来源部门：{{ buildDepartmentNames(task?.departmentIds, deptNameMap) }}</span>
        </div>
      </div>
      <div class="training-task-action-panel">
        <div class="flex flex-wrap justify-end gap-8px">
          <el-button @click="router.back()">
            <Icon icon="ep:back" class="mr-5px" />
            返回
          </el-button>
          <el-button type="primary" plain @click="openDetail">
            <Icon icon="ep:document" class="mr-5px" />
            文件详情
          </el-button>
          <el-button
            type="success"
            :disabled="!task?.eligibleToAcknowledge"
            :loading="ackLoading"
            @click="handleAcknowledge"
          >
            <Icon icon="ep:select" class="mr-5px" />
            确认培训完成
          </el-button>
        </div>
        <div
          class="training-task-action-panel__reason"
          data-testid="dcc-training-task-acknowledge-reason"
        >
          {{ acknowledgeDisabledReason }}
        </div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <div class="mb-14px flex flex-wrap items-center justify-between gap-12px">
      <div>
        <div class="text-15px font-600">培训进度</div>
        <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
          仅在培训预览页位于前台且窗口聚焦时累计时长，达到 10 分钟后才可确认完成。
        </div>
      </div>
      <div class="training-task-countability-state" data-testid="dcc-training-task-countability-state">
        <el-tag :type="trainingCountabilityState.tagType">
          {{ trainingCountabilityState.label }}
        </el-tag>
        <span>{{ trainingCountabilityState.description }}</span>
      </div>
    </div>

    <el-progress
      :percentage="progressPercent"
      :stroke-width="18"
      :status="task?.eligibleToAcknowledge ? 'success' : undefined"
    />

    <el-alert
      class="mt-12px"
      :type="trainingCountabilityState.alertType"
      :closable="false"
      show-icon
      :title="trainingCountabilityState.title"
      :description="acknowledgeDisabledReason"
    />

    <div class="mt-12px training-task-metrics">
      <div class="training-task-metric">
        <div class="training-task-metric__label">累计时长</div>
        <div class="training-task-metric__value">
          {{ formatTrainingSeconds(task?.accumulatedViewSeconds) }}
        </div>
      </div>
      <div class="training-task-metric">
        <div class="training-task-metric__label">达标门槛</div>
        <div class="training-task-metric__value">
          {{ formatTrainingSeconds(task?.requiredViewSeconds) }}
        </div>
      </div>
      <div class="training-task-metric">
        <div class="training-task-metric__label">剩余时长</div>
        <div class="training-task-metric__value">
          {{ formatTrainingSeconds(remainingSeconds) }}
        </div>
      </div>
      <div class="training-task-metric">
        <div class="training-task-metric__label">计时状态</div>
        <div class="training-task-metric__value training-task-metric__value--status">
          {{ trainingCountabilityState.label }}
        </div>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="loadError"
      class="mb-16px"
      type="error"
      :closable="false"
      :title="loadError"
    />
    <ProtectedPdfViewer
      v-else
      :preview-blob="previewBlob"
      :watermark="watermark"
      :title="task?.title || '培训文件预览'"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import type { ControlledPreviewWatermark } from '@/api/dcc/controlledFile/workflow'
import {
  acknowledgeTrainingTask,
  getTrainingTask,
  heartbeatTrainingViewSession,
  previewTrainingTaskWithWatermark,
  startTrainingViewSession,
  stopTrainingViewSession,
  stopTrainingViewSessionKeepalive,
  type TrainingTaskProgressVO
} from '@/api/dcc/controlledFile/training'
import { getSimpleDeptList, type DeptVO } from '@/api/system/dept'
import ProtectedPdfViewer from '../../view/index.vue'
import {
  buildDepartmentNames,
  buildTrainingProgressPercent,
  formatTrainingSeconds,
  getTrainingProgressStatusLabel,
  getTrainingProgressStatusTagType,
  resolveTrainingPageErrorMessage
} from '../presentation'
import { openControlledFileViewer } from '../../shared/viewer-navigation'

defineOptions({ name: 'DccTrainingTask' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const task = ref<TrainingTaskProgressVO>()
const previewBlob = ref<Blob | null>(null)
const watermark = ref<ControlledPreviewWatermark | null>(null)
const departments = ref<DeptVO[]>([])
const loadError = ref('')
const ackLoading = ref(false)
const sessionActive = ref(false)
const documentVisible = ref(document.visibilityState === 'visible')
const windowFocused = ref(window.document.hasFocus())

let heartbeatTimer: number | undefined
let activeClientSessionId = ''

const progressId = computed(() => String(route.params.progressId || ''))
const deptNameMap = computed(() => new Map(departments.value.map((item) => [item.id, item.name])))
const progressPercent = computed(() =>
  buildTrainingProgressPercent(task.value?.accumulatedViewSeconds, task.value?.requiredViewSeconds)
)
const remainingSeconds = computed(() =>
  Math.max(
    0,
    Number(task.value?.requiredViewSeconds || 600) - Number(task.value?.accumulatedViewSeconds || 0)
  )
)

const updateBrowserAttentionState = () => {
  documentVisible.value = document.visibilityState === 'visible'
  windowFocused.value = window.document.hasFocus()
}

const acknowledgeDisabledReason = computed(() => {
  if (!task.value) {
    return '培训任务加载中，暂不能确认。'
  }
  if (task.value.acknowledgedAt) {
    return `已于 ${task.value.acknowledgedAt} 确认完成。`
  }
  if (!previewBlob.value) {
    return '文件预览未加载完成，加载成功后才开始计时。'
  }
  if (!documentVisible.value || !windowFocused.value) {
    return '页面不在前台或窗口未聚焦，培训计时已暂停。'
  }
  if (remainingSeconds.value > 0) {
    return `还需阅读 ${formatTrainingSeconds(remainingSeconds.value)} 后才可确认。`
  }
  if (!task.value.eligibleToAcknowledge) {
    return '阅读时长已达标，服务器状态刷新后即可确认。'
  }
  return '已达标，可确认培训完成。'
})

const trainingCountabilityState = computed(() => {
  if (task.value?.acknowledgedAt) {
    return {
      label: '已完成',
      title: '培训确认已完成',
      description: '该培训任务已有确认时间。',
      tagType: 'success' as const,
      alertType: 'success' as const
    }
  }
  if (!previewBlob.value) {
    return {
      label: '等待预览',
      title: '文件预览未加载完成',
      description: '预览加载成功后才会开始累计阅读时长。',
      tagType: 'info' as const,
      alertType: 'info' as const
    }
  }
  if (!documentVisible.value || !windowFocused.value) {
    return {
      label: '暂停计时',
      title: '页面不在前台或窗口未聚焦',
      description: '回到当前页并保持窗口聚焦后继续计时。',
      tagType: 'warning' as const,
      alertType: 'warning' as const
    }
  }
  if (task.value?.eligibleToAcknowledge) {
    return {
      label: '可确认',
      title: '已达标，可确认培训完成',
      description: '阅读时长已满足当前文件培训要求。',
      tagType: 'success' as const,
      alertType: 'success' as const
    }
  }
  return {
    label: sessionActive.value ? '计时中' : '准备计时',
    title: sessionActive.value ? '当前会话正在计时' : '等待培训计时启动',
    description: `还需阅读 ${formatTrainingSeconds(remainingSeconds.value)}。`,
    tagType: sessionActive.value ? ('success' as const) : ('info' as const),
    alertType: 'info' as const
  }
})

const clearHeartbeatTimer = () => {
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer)
    heartbeatTimer = undefined
  }
}

const isSessionCountable = () =>
  Boolean(previewBlob.value) &&
  Boolean(task.value) &&
  !task.value?.acknowledgedAt &&
  documentVisible.value &&
  windowFocused.value

const loadBaseData = async () => {
  departments.value = await getSimpleDeptList()
}

const refreshTask = async () => {
  task.value = await getTrainingTask(progressId.value)
}

const loadPreview = async () => {
  const preview = await previewTrainingTaskWithWatermark(progressId.value)
  previewBlob.value = preview.blob
  watermark.value = preview.watermark
}

const openDetail = () => {
  if (!task.value?.controlledFileId) {
    return
  }
  openControlledFileViewer(router, route, task.value.controlledFileId, 'training-task')
}

const startSessionIfNeeded = async () => {
  if (!isSessionCountable() || sessionActive.value) {
    return
  }
  activeClientSessionId = crypto.randomUUID()
  task.value = await startTrainingViewSession(progressId.value, {
    clientSessionId: activeClientSessionId
  })
  sessionActive.value = true
  heartbeatTimer = window.setInterval(async () => {
    if (!sessionActive.value || !activeClientSessionId) {
      return
    }
    try {
      task.value = await heartbeatTrainingViewSession(progressId.value, {
        clientSessionId: activeClientSessionId
      })
    } catch (error) {
      clearHeartbeatTimer()
      sessionActive.value = false
      loadError.value = resolveTrainingPageErrorMessage(
        error,
        '培训计时心跳上报失败，请刷新页面后重试。'
      )
    }
  }, 5000)
}

const stopSession = async (useKeepalive = false) => {
  if (!sessionActive.value || !activeClientSessionId) {
    return
  }
  const currentSessionId = activeClientSessionId
  sessionActive.value = false
  activeClientSessionId = ''
  clearHeartbeatTimer()
  try {
    if (useKeepalive) {
      await stopTrainingViewSessionKeepalive(progressId.value, { clientSessionId: currentSessionId })
    } else {
      task.value = await stopTrainingViewSession(progressId.value, { clientSessionId: currentSessionId })
    }
  } catch (error) {
    if (!useKeepalive) {
      loadError.value = resolveTrainingPageErrorMessage(
        error,
        '培训计时结束失败，请刷新页面后重试。'
      )
    }
  }
}

const syncSessionState = async (useKeepalive = false) => {
  updateBrowserAttentionState()
  if (isSessionCountable()) {
    await startSessionIfNeeded()
    return
  }
  await stopSession(useKeepalive)
}

const handleVisibilityChange = async () => {
  await syncSessionState()
}

const handleWindowFocus = async () => {
  await syncSessionState()
}

const handleWindowBlur = async () => {
  await syncSessionState()
}

const handlePageHide = () => {
  updateBrowserAttentionState()
  void syncSessionState(true)
}

const handleAcknowledge = async () => {
  if (!task.value?.eligibleToAcknowledge) {
    return
  }
  try {
    await message.confirm('确认已完成该文件培训并提交结果吗？')
    ackLoading.value = true
    await acknowledgeTrainingTask(progressId.value)
    message.success('培训完成确认已提交')
    await refreshTask()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      message.error(resolveTrainingPageErrorMessage(error, '培训完成确认失败，请查看错误提示后重试。'))
    }
  } finally {
    ackLoading.value = false
  }
}

onMounted(async () => {
  try {
    updateBrowserAttentionState()
    await loadBaseData()
    await refreshTask()
    await loadPreview()
    await syncSessionState()
  } catch (error) {
    loadError.value = resolveTrainingPageErrorMessage(
      error,
      '培训任务加载失败，请查看错误提示后重试。'
    )
  }
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('focus', handleWindowFocus)
  window.addEventListener('blur', handleWindowBlur)
  window.addEventListener('pagehide', handlePageHide)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('focus', handleWindowFocus)
  window.removeEventListener('blur', handleWindowBlur)
  window.removeEventListener('pagehide', handlePageHide)
  void stopSession(true)
})
</script>

<style scoped>
.training-task-metrics {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.training-task-action-panel {
  display: grid;
  gap: 6px;
  justify-items: end;
}

.training-task-action-panel__reason {
  max-width: 360px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-align: right;
}

.training-task-countability-state {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.training-task-metric {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  background: #f8fbff;
}

.training-task-metric__label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
}

.training-task-metric__value {
  margin-top: 6px;
  color: #172033;
  font-size: 20px;
  font-weight: 700;
  line-height: 28px;
}

.training-task-metric__value--status {
  color: #1677ff;
  font-size: 18px;
}
</style>
