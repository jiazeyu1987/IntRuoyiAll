<template>
  <ContentWrap>
    <div v-loading="loading" class="edhr-batch-template">
      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <section class="edhr-batch-template__panel">
        <div class="edhr-batch-template__section-title">模板说明</div>
        <el-empty v-if="!templateTasks.length && !loading" description="当前批次暂无可查看的模板表格" />
        <div v-else class="edhr-batch-template__workbench">
          <nav class="edhr-batch-template__process-list" aria-label="批次模板工序">
            <div
              v-for="task in templateTasks"
              :key="String(task.id)"
              class="edhr-batch-template__process-item"
              :class="{ 'is-active': String(task.id) === selectedTaskId }"
            >
              <div class="edhr-batch-template__process-head">
                <button
                  type="button"
                  class="edhr-batch-template__process-button"
                  @click="selectTask(task)"
                >
                  <span class="edhr-batch-template__process-main">
                    <span class="edhr-batch-template__process-sort">{{ task.routeProcessSort || '--' }}</span>
                    <span class="edhr-batch-template__process-name">
                      {{ task.processCode || '--' }} {{ task.processName || '--' }}
                    </span>
                  </span>
                </button>
                <el-button
                  size="small"
                  link
                  type="primary"
                  class="edhr-batch-template__simulate-action"
                  @click.stop="openSimulate(task)"
                >
                  模拟填写
                </el-button>
              </div>
              <button type="button" class="edhr-batch-template__process-button" @click="selectTask(task)">
                <span class="edhr-batch-template__process-report">
                  {{ task.batchRecordReportName || task.batchRecordReportId || '--' }}
                </span>
                <el-tag size="small" type="info">
                  顺序 {{ task.batchRecordSort || '--' }}
                </el-tag>
              </button>
            </div>
          </nav>

          <div class="edhr-batch-template__preview">
            <el-empty v-if="!selectedTask" description="请选择左侧模板工序" />
            <div v-else class="edhr-batch-template__preview-card">
              <div class="edhr-batch-template__preview-header">
                <div>
                  <div class="edhr-batch-template__preview-title">
                    {{ selectedTask.routeProcessSort || '--' }}.
                    {{ selectedTask.processCode || '--' }}
                    {{ selectedTask.processName || '--' }}
                  </div>
                  <div class="edhr-batch-template__preview-subtitle">
                    {{ selectedTask.batchRecordReportName || selectedTask.batchRecordReportId || '--' }}
                  </div>
                </div>
                <el-tag type="primary">模板说明</el-tag>
              </div>

              <el-descriptions :column="4" border>
                <el-descriptions-item label="任务编号">{{ selectedTask.id || '--' }}</el-descriptions-item>
                <el-descriptions-item label="表格顺序">{{ selectedTask.batchRecordSort || '--' }}</el-descriptions-item>
                <el-descriptions-item label="模板ID">
                  {{ selectedTask.batchRecordReportId || '--' }}
                </el-descriptions-item>
                <el-descriptions-item label="记录属性">
                  {{ selectedTask.recordCategory || '--' }}
                </el-descriptions-item>
              </el-descriptions>

              <el-alert
                v-if="templateLoadError"
                :title="templateLoadError"
                type="error"
                :closable="false"
                show-icon
              />

              <EdhrExecutionTemplateGuide
                v-else-if="selectedTemplate"
                :sheet-layout-json="selectedTemplate.sheetLayoutJson"
                :cell-rules="selectedTemplate.rules"
                :signature-markers="selectedTemplate.markers"
              />
            </div>
          </div>
        </div>
      </section>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  BatchRecordReportApi,
  type BatchRecordReportCellRuleVO,
  type BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  getEdhrBatchExecution,
  type EdhrBatchExecutionRespVO,
  type EdhrBatchExecutionTaskRespVO
} from '@/api/mes/pro/edhr/batchExecution'
import { normalizeCellRule } from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'
import EdhrExecutionTemplateGuide from '@/views/mes/pro/edhr/components/EdhrExecutionTemplateGuide.vue'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProEdhrBatchExecutionTemplate' })

type TemplateCacheItem = {
  sheetLayoutJson: string
  rules: BatchRecordReportCellRuleVO[]
  markers: BatchRecordReportSignatureCellMarkerVO[]
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const detail = ref<EdhrBatchExecutionRespVO>()
const selectedTaskId = ref('')
const templateLoadError = ref('')
const templateCache = reactive<Record<string, TemplateCacheItem>>({})

const batchExecutionId = computed(() => parsePositiveRouteQueryId(route.query.id))

const assertBatchExecutionId = () => {
  if (!batchExecutionId.value) {
    throw new Error('缺少有效批次执行ID，无法查看模板说明。')
  }
  return batchExecutionId.value
}

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const templateTasks = computed<EdhrBatchExecutionTaskRespVO[]>(() =>
  [...(detail.value?.tasks || [])]
    .filter((task) => Boolean(task.batchRecordReportId))
    .sort(
      (left, right) =>
        (left.routeProcessSort || 0) - (right.routeProcessSort || 0) ||
        (left.batchRecordSort || 0) - (right.batchRecordSort || 0) ||
        (left.id || 0) - (right.id || 0)
    )
)

const selectedTask = computed(() =>
  templateTasks.value.find((task) => String(task.id) === selectedTaskId.value)
)

const selectedTemplate = computed(() => {
  const reportId = selectedTask.value?.batchRecordReportId
  return reportId ? templateCache[reportId] : undefined
})

const loadTemplateByTask = async (task: EdhrBatchExecutionTaskRespVO) => {
  const reportId = String(task.batchRecordReportId || '').trim()
  templateLoadError.value = ''
  if (!reportId) {
    throw new Error('当前任务缺少模板ID，无法查看模板说明。')
  }
  if (templateCache[reportId]) return
  try {
    const [cellRuleResp, markerResp] = await Promise.all([
      BatchRecordReportApi.getCellRules(reportId),
      BatchRecordReportApi.getSignatureCellMarkers(reportId)
    ])
    const rawRules = cellRuleResp.suggestions?.length ? cellRuleResp.suggestions : cellRuleResp.rules || []
    const rules = rawRules.map(normalizeCellRule).sort(
      (left, right) => left.rowIndex - right.rowIndex || left.columnIndex - right.columnIndex
    )
    const sheetLayoutJson = cellRuleResp.sheetLayoutJson || markerResp.sheetLayoutJson || ''
    if (!sheetLayoutJson.trim()) {
      throw new Error('缺少电子批记录模板布局，无法显示模板说明。')
    }
    if (!rules.length) {
      throw new Error('模板缺少单元格规则，无法显示模板说明。')
    }
    templateCache[reportId] = {
      sheetLayoutJson,
      rules,
      markers: markerResp.markers || []
    }
  } catch (error) {
    templateLoadError.value = resolveErrorMessage(error, '模板规则加载失败。')
  }
}

const selectTask = async (task: EdhrBatchExecutionTaskRespVO) => {
  selectedTaskId.value = String(task.id || '')
  await loadTemplateByTask(task)
}

const openSimulate = async (task: EdhrBatchExecutionTaskRespVO) => {
  if (!task.id) {
    throw new Error('当前任务缺少有效 taskId，无法进入模拟填写。')
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/template-simulate',
    query: {
      id: String(assertBatchExecutionId()),
      taskId: String(task.id),
      returnTo: route.fullPath,
      returnLabel: '返回模板说明'
    }
  })
}

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''
  selectedTaskId.value = ''
  templateLoadError.value = ''
  detail.value = undefined
  try {
    detail.value = await getEdhrBatchExecution(assertBatchExecutionId())
    const firstTask = templateTasks.value[0]
    if (firstTask) {
      selectedTaskId.value = String(firstTask.id || '')
      await loadTemplateByTask(firstTask)
    }
  } catch (error) {
    detail.value = undefined
    loadError.value = resolveErrorMessage(error, 'eDHR 批次模板说明加载失败。')
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.edhr-batch-template {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-batch-template__panel {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
}

.edhr-batch-template__section-title {
  color: #172033;
  font-weight: 600;
}

.edhr-batch-template__workbench {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  gap: 16px;
  margin-top: 12px;
  min-height: 520px;
}

.edhr-batch-template__process-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 240px);
  min-height: 360px;
  overflow: auto;
  padding-right: 4px;
}

.edhr-batch-template__process-item {
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 6px;
  transition: border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}

.edhr-batch-template__process-item:hover,
.edhr-batch-template__process-item:focus-within {
  border-color: #1677ff;
  box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.12);
  outline: none;
}

.edhr-batch-template__process-item.is-active {
  border-color: #1677ff;
  background: #f5f9ff;
  box-shadow: inset 3px 0 0 #1677ff;
}

.edhr-batch-template__process-head {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px 0;
}

.edhr-batch-template__process-button {
  width: 100%;
  border: none;
  background: transparent;
  color: #172033;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 12px 10px;
  text-align: left;
}

.edhr-batch-template__process-head > .edhr-batch-template__process-button {
  padding: 0;
}

.edhr-batch-template__process-button:focus-visible {
  outline: none;
}

.edhr-batch-template__process-main {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.edhr-batch-template__process-sort {
  color: #1677ff;
  flex: none;
  font-weight: 700;
  min-width: 24px;
}

.edhr-batch-template__process-name {
  font-weight: 600;
  line-height: 1.4;
}

.edhr-batch-template__process-report {
  color: #4b5563;
  font-size: 12px;
  line-height: 1.4;
}

.edhr-batch-template__simulate-action {
  flex: none;
  padding-top: 1px;
}

.edhr-batch-template__preview {
  min-width: 0;
}

.edhr-batch-template__preview-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-batch-template__preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.edhr-batch-template__preview-title {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-batch-template__preview-subtitle {
  color: #4b5563;
  font-size: 13px;
  margin-top: 4px;
}

@media (max-width: 960px) {
  .edhr-batch-template__workbench {
    grid-template-columns: 1fr;
  }

  .edhr-batch-template__process-list {
    flex-direction: row;
    max-height: none;
    min-height: 0;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .edhr-batch-template__process-item {
    flex: 0 0 240px;
  }
}
</style>
