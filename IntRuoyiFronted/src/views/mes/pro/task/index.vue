<!-- MES 生产排产 -->
<template>
  <ContentWrap
    class="production-schedule-gantt-page"
    title="当前排产甘特图"
    :body-style="{ padding: '0' }"
  >
    <template #header>
      <div class="production-gantt-toolbar">
        <el-button size="small" @click="handleCollapseAllProjects">
          <Icon icon="ep:fold" class="mr-4px" />
          全部折叠
        </el-button>
        <el-button size="small" @click="handleExpandAllProjects">
          <Icon icon="ep:expand" class="mr-4px" />
          全部展开
        </el-button>
        <el-button size="small" @click="toggleGanttFullscreen">
          <Icon :icon="isFullscreen ? 'ep:close' : 'ep:full-screen'" class="mr-4px" />
          {{ isFullscreen ? '恢复' : '最大化' }}
        </el-button>
        <div class="production-gantt-interval-control">
          <span class="production-gantt-interval-label">日期间隔</span>
          <el-slider
            v-model="ganttDateIntervalDays"
            :min="1"
            :max="15"
            :step="1"
            :marks="ganttDateIntervalMarks"
            :show-tooltip="false"
          />
          <span class="production-gantt-interval-value">{{ ganttDateIntervalDays }} 天/格</span>
        </div>
      </div>
    </template>
    <div
      ref="ganttFullscreenHostRef"
      class="production-gantt-fullscreen-host"
      :class="{ 'production-gantt-fullscreen-host--active': isFullscreen }"
    >
      <aside v-if="isFullscreen" class="production-gantt-fullscreen-rail">
        <el-button type="primary" size="small" class="production-gantt-restore-button" @click="exitGanttFullscreen">
          恢复
        </el-button>
        <div class="production-gantt-fullscreen-rail__title">视图控制</div>
        <div class="production-gantt-fullscreen-rail__hint">订单</div>
        <div class="production-gantt-fullscreen-rail__hint">工序</div>
      </aside>
      <div class="production-gantt-chart-shell">
        <GanttChart
          ref="ganttChartRef"
          :tasks="ganttTasks"
          :links="ganttLinks"
          :readonly="true"
          :height="ganttHeight"
          :date-interval-days="ganttDateIntervalDays"
        />
      </div>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ProTaskApi } from '@/api/mes/pro/task'
import { ProTaskAutoScheduleApi } from '@/api/mes/pro/task/autoSchedule'
import { useEmitt } from '@/hooks/web/useEmitt'
import { MES_PRO_TASK_GANTT_REFRESH_EVENT } from '../shared/scheduleEvents'
import GanttChart from './components/GanttChart.vue'

defineOptions({ name: 'MesProTask' })

const message = useMessage()
const ganttTasks = ref<any[]>([])
const ganttLinks = ref<any[]>([])
const ganttHeight = ref(520)
const ganttChartRef = ref<InstanceType<typeof GanttChart>>()
const ganttFullscreenHostRef = ref<HTMLElement>()
const isFullscreen = ref(false)
const GANTT_DATE_INTERVAL_STORAGE_KEY = 'mes.pro.task.gantt.dateIntervalDays'
const DEFAULT_GANTT_DATE_INTERVAL_DAYS = 1
let hasMountedGantt = false
let ganttHeightAnimationFrame: number | null = null

const normalizeGanttDateIntervalDays = (value: unknown) => {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 1 && parsed <= 15
    ? parsed
    : DEFAULT_GANTT_DATE_INTERVAL_DAYS
}

const readStoredGanttDateIntervalDays = () => {
  if (typeof window === 'undefined') {
    return DEFAULT_GANTT_DATE_INTERVAL_DAYS
  }
  try {
    return normalizeGanttDateIntervalDays(
      window.localStorage.getItem(GANTT_DATE_INTERVAL_STORAGE_KEY)
    )
  } catch (error) {
    console.error('[MES] 读取甘特图日期间隔本地配置失败', error)
    return DEFAULT_GANTT_DATE_INTERVAL_DAYS
  }
}

const persistGanttDateIntervalDays = (days: number) => {
  try {
    window.localStorage.setItem(GANTT_DATE_INTERVAL_STORAGE_KEY, String(days))
  } catch (error) {
    console.error('[MES] 保存甘特图日期间隔本地配置失败', error)
    message.error('日期间隔保存失败，请检查浏览器本地存储权限')
  }
}

const ganttDateIntervalDays = ref(readStoredGanttDateIntervalDays())
const ganttDateIntervalMarks = {
  1: '1',
  5: '5',
  10: '10',
  15: '15'
}

const updateGanttHeight = () => {
  if (typeof window === 'undefined') {
    return
  }
  if (ganttHeightAnimationFrame) {
    window.cancelAnimationFrame(ganttHeightAnimationFrame)
  }
  ganttHeightAnimationFrame = window.requestAnimationFrame(() => {
    const host = ganttFullscreenHostRef.value
    if (isFullscreen.value && host) {
      const hostStyle = window.getComputedStyle(host)
      const verticalPadding =
        Number.parseFloat(hostStyle.paddingTop || '0') +
        Number.parseFloat(hostStyle.paddingBottom || '0')
      ganttHeight.value = Math.max(host.clientHeight - verticalPadding, 420)
    } else {
      const hostTop = host?.getBoundingClientRect().top ?? 170
      ganttHeight.value = Math.max(window.innerHeight - hostTop - 24, 420)
    }
    ganttHeightAnimationFrame = null
  })
}

const loadCurrentGantt = async () => {
  const tasks = await ProTaskApi.getGanttTaskList({})
  ganttTasks.value = tasks
  const taskIds = tasks
    .filter((item: any) => item.type === 303 && item.originalId)
    .map((item: any) => item.originalId)
  ganttLinks.value = taskIds.length
    ? await ProTaskAutoScheduleApi.getDependencies({ taskIds })
    : []
}

const handleCollapseAllProjects = () => {
  ganttChartRef.value?.collapseAllProjects()
}

const handleExpandAllProjects = () => {
  ganttChartRef.value?.expandAllProjects()
}

const syncGanttFullscreenState = () => {
  isFullscreen.value = document.fullscreenElement === ganttFullscreenHostRef.value
  updateGanttHeight()
}

const enterGanttFullscreen = async () => {
  const host = ganttFullscreenHostRef.value
  if (!host) {
    throw new Error('当前排产甘特图容器尚未初始化')
  }
  if (document.fullscreenElement === host) {
    return
  }
  await host.requestFullscreen()
}

const exitGanttFullscreen = async () => {
  if (document.fullscreenElement === ganttFullscreenHostRef.value) {
    await document.exitFullscreen()
  }
}

const toggleGanttFullscreen = async () => {
  try {
    if (document.fullscreenElement === ganttFullscreenHostRef.value) {
      await exitGanttFullscreen()
    } else {
      await enterGanttFullscreen()
    }
  } catch (error) {
    console.error('[MES] 切换当前排产甘特图全屏失败', error)
    message.error(error instanceof Error ? error.message : '切换甘特图最大化失败')
  }
}

watch(ganttDateIntervalDays, (value) => {
  const normalized = normalizeGanttDateIntervalDays(value)
  if (normalized !== value) {
    ganttDateIntervalDays.value = normalized
    return
  }
  persistGanttDateIntervalDays(normalized)
})

useEmitt({
  name: MES_PRO_TASK_GANTT_REFRESH_EVENT,
  callback: loadCurrentGantt
})

onMounted(async () => {
  updateGanttHeight()
  window.addEventListener('resize', updateGanttHeight)
  document.addEventListener('fullscreenchange', syncGanttFullscreenState)
  await loadCurrentGantt()
  hasMountedGantt = true
})

onActivated(async () => {
  if (!hasMountedGantt) {
    return
  }
  await loadCurrentGantt()
  updateGanttHeight()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateGanttHeight)
  document.removeEventListener('fullscreenchange', syncGanttFullscreenState)
  if (ganttHeightAnimationFrame) {
    window.cancelAnimationFrame(ganttHeightAnimationFrame)
    ganttHeightAnimationFrame = null
  }
})
</script>

<style scoped>
.production-schedule-gantt-page {
  min-height: calc(100vh - 96px);
  margin-bottom: 0 !important;
}

.production-schedule-gantt-page :deep(.el-card__body) {
  height: calc(100% - 50px);
}

.production-gantt-toolbar {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.production-gantt-interval-control {
  display: grid;
  grid-template-columns: auto 180px auto;
  align-items: center;
  gap: 10px;
  min-width: 290px;
  padding-left: 6px;
}

.production-gantt-interval-label,
.production-gantt-interval-value {
  color: #4b5563;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.production-gantt-interval-value {
  min-width: 50px;
  color: #1677ff;
  text-align: right;
}

.production-gantt-interval-control :deep(.el-slider) {
  --el-slider-main-bg-color: #1677ff;
  --el-slider-stop-bg-color: #dbe3ef;
}

.production-gantt-interval-control :deep(.el-slider__marks-text) {
  color: #64748b;
  font-size: 11px;
}

.production-gantt-fullscreen-host {
  display: flex;
  width: 100%;
  min-height: 0;
  background: #fff;
}

.production-gantt-fullscreen-host--active {
  box-sizing: border-box;
  width: 100vw;
  height: 100vh;
  gap: 14px;
  padding: 14px;
  background: #f5f8fc;
}

.production-gantt-chart-shell {
  min-width: 0;
  flex: 1;
}

.production-gantt-fullscreen-host--active .production-gantt-chart-shell {
  height: 100%;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 12%);
}

.production-gantt-fullscreen-rail {
  box-sizing: border-box;
  display: flex;
  width: 132px;
  flex: none;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #172033;
  color: #fff;
  box-shadow: 0 12px 28px rgb(15 23 42 / 16%);
}

.production-gantt-restore-button {
  width: 100%;
}

.production-gantt-fullscreen-rail__title {
  margin-top: 4px;
  color: #cbd5e1;
  font-size: 12px;
  font-weight: 700;
}

.production-gantt-fullscreen-rail__hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 8px;
  background: rgb(255 255 255 / 10%);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}
</style>
