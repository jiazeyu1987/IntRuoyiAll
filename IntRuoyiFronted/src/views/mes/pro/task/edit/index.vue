<!-- MES 甘特图编辑 -->
<template>
  <doc-alert title="【生产】生产排产、工序流转卡" url="https://doc.iocoder.cn/mes/pro/schedule-card/" />

  <ContentWrap>
    <div class="mb-10px flex items-center justify-between">
      <span class="text-14px text-gray-500">
        可直接拖拽、拉伸任务条，或双击编辑开始时间和时长，修改后点击“批量保存”
      </span>
      <div>
        <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="mr-10px">
          <el-button
            type="primary"
            @click="handleSave"
            :loading="formLoading"
            :disabled="pendingCount === 0"
          >
            批量保存
          </el-button>
        </el-badge>
        <el-button @click="handleRefresh">刷新</el-button>
      </div>
    </div>
    <GanttChart
      :tasks="taskList"
      :links="ganttLinks"
      :readonly="false"
      :height="ganttHeight"
      @task-update="handleTaskUpdate"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { ProTaskApi } from '@/api/mes/pro/task'
import { ProTaskAutoScheduleApi } from '@/api/mes/pro/task/autoSchedule'
import GanttChart from '../components/GanttChart.vue'

defineOptions({ name: 'MesProTaskGanttEdit' })

const message = useMessage()

const formLoading = ref(false)
const taskList = ref<any[]>([])
const ganttLinks = ref<any[]>([])

const pendingChanges = ref(new Map<number, any>())
const pendingCount = computed(() => pendingChanges.value.size)
const ganttHeight = computed(() => window.innerHeight - 180)

const loadGanttData = async () => {
  taskList.value = await ProTaskApi.getGanttTaskList({})
  const taskIds = taskList.value
    .filter((item: any) => item.type === 303 && item.originalId)
    .map((item: any) => item.originalId)
  ganttLinks.value = taskIds.length
    ? await ProTaskAutoScheduleApi.getDependencies({ taskIds })
    : []
}

const handleTaskUpdate = (change: any) => {
  pendingChanges.value.set(change.id, change)
}

const handleSave = async () => {
  if (pendingChanges.value.size === 0) {
    return
  }
  formLoading.value = true
  try {
    const promises = Array.from(pendingChanges.value.values()).map((change) =>
      ProTaskApi.updateTask({
        id: change.id,
        startTime: change.startTime,
        endTime: change.endTime,
        duration: change.duration
      } as any)
    )
    await Promise.all(promises)
    message.success(`已保存 ${pendingChanges.value.size} 条修改`)
    pendingChanges.value = new Map()
    await loadGanttData()
  } finally {
    formLoading.value = false
  }
}

const handleRefresh = async () => {
  pendingChanges.value = new Map()
  await loadGanttData()
}

onMounted(async () => {
  await loadGanttData()
})
</script>
