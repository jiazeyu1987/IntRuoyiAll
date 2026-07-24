<template>
  <ContentWrap>
    <div class="dcc-workbench-header">
      <div class="dcc-workbench-header__main">
        <div class="dcc-workbench-title">DCC 工作台</div>
        <div class="dcc-workbench-updated">更新时间：{{ lastLoadedAt || '-' }}</div>
      </div>
      <div class="dcc-workbench-actions">
        <el-button :loading="loading" @click="loadWorkbench">
          <Icon icon="ep:refresh-right" class="mr-5px" />
          刷新
        </el-button>
        <el-button type="primary" @click="openPath('/dcc/controlled-file/upload')">
          <Icon icon="ep:upload" class="mr-5px" />
          上传文件
        </el-button>
        <el-button plain type="primary" @click="openPath('/dcc/controlled-file/browser')">
          <Icon icon="ep:folder-opened" class="mr-5px" />
          受控浏览
        </el-button>
      </div>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-alert
      v-if="loadErrorMessage"
      class="mb-12px"
      :closable="false"
      data-testid="dcc-workbench-load-error"
      show-icon
      title="DCC 工作台加载失败"
      type="error"
    >
      <template #default>
        {{ loadErrorMessage }}
      </template>
    </el-alert>

    <div v-loading="loading" class="dcc-workbench-metric-grid">
      <button
        v-for="metric in metricItems"
        :key="metric.key"
        class="dcc-workbench-metric"
        :class="`dcc-workbench-metric--${metric.tone}`"
        type="button"
        @click="openPath(metric.routePath)"
      >
        <span class="dcc-workbench-metric__label">{{ metric.label }}</span>
        <span class="dcc-workbench-metric__count">{{ metric.count }}</span>
      </button>
    </div>
  </ContentWrap>

  <div class="dcc-workbench-panel-grid">
    <ContentWrap class="dcc-workbench-panel">
      <div class="dcc-workbench-panel__header">
        <div class="dcc-workbench-panel__title">我的审批待办</div>
        <el-button link type="primary" @click="openPath('/approval-center?moduleCode=DCC&viewType=TODO')">
          全部
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="approvalTodoRows"
        empty-text="暂无审批待办"
        size="small"
      >
        <el-table-column label="文件" min-width="240">
          <template #default="{ row }">
            <div class="dcc-workbench-file-context" data-testid="dcc-workbench-approval-file-context">
              <div
                class="dcc-workbench-file-context__title"
                :title="row.controlledFile?.title || row.name || '-'"
              >
                {{ row.controlledFile?.title || row.name || '-' }}
              </div>
              <div class="dcc-workbench-file-context__meta">
                <span>编号：{{ row.controlledFile?.fileNumber || '-' }}</span>
                <span>版本：{{ row.controlledFile?.versionNo || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="130">
          <template #default="{ row }">{{ row.currentStageLabel }}</template>
        </el-table-column>
        <el-table-column label="处理提示" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handlingHint }}</template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openApproval(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap class="dcc-workbench-panel">
      <div class="dcc-workbench-panel__header">
        <div class="dcc-workbench-panel__title">待文控下发</div>
        <el-button link type="primary" @click="openPath('/dcc/controlled-file/browser')">全部</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="pendingDistributionRows"
        empty-text="暂无待下发文件"
        size="small"
      >
        <el-table-column label="文件" min-width="220" prop="title" show-overflow-tooltip />
        <el-table-column label="编号" min-width="150" prop="fileNumber" show-overflow-tooltip />
        <el-table-column label="时间" width="170" prop="timeText" />
        <el-table-column label="操作" align="center" width="90">
          <template #default="{ row }">
            <el-button link type="primary" :title="row.actionBlockReason" @click="openFileDetail(row.id)">
              {{ row.primaryActionText }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap class="dcc-workbench-panel">
      <div class="dcc-workbench-panel__header">
        <div class="dcc-workbench-panel__title">待培训确认</div>
        <el-button link type="primary" @click="openPath('/dcc/controlled-file/training-mine')">
          全部
        </el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="trainingTodoRows"
        empty-text="暂无培训任务"
        size="small"
      >
        <el-table-column label="文件" min-width="240">
          <template #default="{ row }">
            <div class="dcc-workbench-file-context" data-testid="dcc-workbench-training-file-context">
              <div class="dcc-workbench-file-context__title" :title="row.title">{{ row.title }}</div>
              <div class="dcc-workbench-file-context__meta">
                <span>编号：{{ row.fileNumber }}</span>
                <span>版本：{{ row.versionNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'READY_TO_ACKNOWLEDGE' ? 'success' : 'warning'" size="small">
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="130" prop="progressText" />
        <el-table-column label="操作" align="center" width="150">
          <template #default="{ row }">
            <div class="dcc-workbench-row-actions">
              <el-button
                link
                type="primary"
                data-testid="dcc-workbench-training-file-detail-link"
                @click="openFileDetail(row.controlledFileId)"
              >
                文件详情
              </el-button>
              <el-button link type="primary" @click="openTrainingTask(row.progressId)">进入</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>

    <ContentWrap class="dcc-workbench-panel dcc-workbench-panel--wide">
      <div class="dcc-workbench-panel__header">
        <div class="dcc-workbench-panel__title">发布失败</div>
        <el-button link type="primary" @click="openPath('/dcc/controlled-file/browser')">全部</el-button>
      </div>
      <el-table
        v-loading="loading"
        :data="finalizationFailedRows"
        empty-text="暂无发布失败"
        size="small"
      >
        <el-table-column label="文件" min-width="260" prop="title" show-overflow-tooltip />
        <el-table-column label="编号" min-width="160" prop="fileNumber" show-overflow-tooltip />
        <el-table-column label="阻塞提示" min-width="240" prop="responsibilityHint" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="90">
          <template #default="{ row }">
            <el-button link type="primary" :title="row.actionBlockReason" @click="openFileDetail(row.id)">
              {{ row.primaryActionText }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </ContentWrap>
  </div>
</template>

<script lang="ts" setup>
import * as TaskApi from '@/api/bpm/task'
import { getProcessInstance, type ProcessInstanceVO } from '@/api/bpm/processInstance'
import {
  CONTROLLED_FILE_PROCESS_DEFINITION_KEY,
  EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY,
  getControlledFile,
  getControlledFileBrowserPage,
  type ControlledFileVO
} from '@/api/dcc/controlledFile/workflow'
import { getMyTrainingTaskPage, type TrainingTaskProgressVO } from '@/api/dcc/controlledFile/training'
import { buildDccTaskCenterRowView, type DccTaskLike } from '../shared/approval'
import { openControlledFileViewer } from '../shared/viewer-navigation'
import {
  buildDccWorkbenchMetricItems,
  resolveWorkbenchErrorMessage,
  toWorkbenchFileRow,
  toWorkbenchTrainingRow,
  type DccWorkbenchFileRow,
  type DccWorkbenchMetricItem,
  type DccWorkbenchTrainingRow
} from './presentation'

defineOptions({ name: 'DccControlledFileWorkbench' })

interface DccWorkbenchTaskRow {
  id: string
  name: string
  createTime?: string
  processInstanceId: string
  processInstance?: {
    id: string
    businessKey?: string
    startUser?: {
      nickname?: string
    }
  }
  controlledFile?: ControlledFileVO
  taskList?: DccTaskLike[]
  currentStageLabel: string
  sameLayerProgressText: string
  sameLayerHint: string
  handlingHint: string
  responsibilityHint: string
  primaryActionText: string
  secondaryActionText: string
}

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadErrorMessage = ref('')
const lastLoadedAt = ref('')
const metricItems = ref<DccWorkbenchMetricItem[]>(buildDccWorkbenchMetricItems({
  approvalTodoTotal: 0,
  pendingDistributionTotal: 0,
  trainingTodoTotal: 0,
  finalizationFailedTotal: 0
}))
const approvalTodoRows = ref<DccWorkbenchTaskRow[]>([])
const pendingDistributionRows = ref<DccWorkbenchFileRow[]>([])
const trainingTodoRows = ref<DccWorkbenchTrainingRow[]>([])
const finalizationFailedRows = ref<DccWorkbenchFileRow[]>([])

const DCC_APPROVAL_PROCESS_DEFINITION_KEYS = [
  CONTROLLED_FILE_PROCESS_DEFINITION_KEY,
  EXTERNAL_FILE_REVIEW_PROCESS_DEFINITION_KEY
]

const openPath = (path: string) => {
  router.push(path)
}

const openFileDetail = (id: number | string) => {
  openControlledFileViewer(router, route, id, 'workbench')
}

const openTrainingTask = (progressId: number | string) => {
  router.push({
    name: 'DccTrainingTask',
    params: { progressId }
  })
}

const openApproval = (row: DccWorkbenchTaskRow) => {
  if (!row.controlledFile?.id) {
    throw new Error('DCC 工作台审批待办缺少受控文件 ID')
  }
  openControlledFileViewer(router, route, row.controlledFile.id, 'workbench-approval')
}

const buildTaskRows = async () => {
  const pages = await Promise.all(
    DCC_APPROVAL_PROCESS_DEFINITION_KEYS.map((processDefinitionKey) =>
      TaskApi.getTaskTodoPage({
        pageNo: 1,
        pageSize: 5,
        processDefinitionKey
      })
    )
  )
  const taskRows = pages
    .flatMap((page) => (page.list || []) as DccWorkbenchTaskRow[])
    .sort((left, right) => String(right.createTime || '').localeCompare(String(left.createTime || '')))
    .slice(0, 5)
  if (taskRows.length === 0) {
    return {
      rows: [],
      total: pages.reduce((sum, page) => sum + Number(page.total || 0), 0)
    }
  }

  const processInstances = await Promise.all(
    taskRows.map((item) => getProcessInstance(item.processInstanceId))
  )
  const businessKeys = processInstances.map((item: ProcessInstanceVO) => item.businessKey)
  if (businessKeys.some((item) => !item || !/^\d+$/.test(item))) {
    throw new Error('DCC 工作台审批待办缺少 businessKey，无法定位受控文件')
  }
  const [files, taskLists] = await Promise.all([
    Promise.all(businessKeys.map((id) => getControlledFile(id as string))),
    Promise.all(taskRows.map((item) => TaskApi.getTaskListByProcessInstanceId(item.processInstanceId)))
  ])

  return {
    rows: taskRows.map((item, index) => {
      const controlledFile = files[index]
      const rowView = buildDccTaskCenterRowView({
        fileStatus: controlledFile?.status,
        routeSnapshots: controlledFile?.routeSnapshots,
        taskList: taskLists[index] as DccTaskLike[],
        taskName: item.name,
        processInstanceId: item.processInstanceId
      })
      return {
        ...item,
        processInstance: {
          ...item.processInstance,
          id: item.processInstance?.id ?? item.processInstanceId,
          businessKey: processInstances[index].businessKey,
          startUser: processInstances[index].startUser
        },
        controlledFile,
        taskList: taskLists[index] as DccTaskLike[],
        ...rowView
      }
    }),
    total: pages.reduce((sum, page) => sum + Number(page.total || 0), 0)
  }
}

const loadFilePageByStatus = async (status: string) => {
  const data = await getControlledFileBrowserPage({
    pageNo: 1,
    pageSize: 5,
    status
  })
  return {
    rows: data.list.map(toWorkbenchFileRow),
    total: data.total
  }
}

const loadTrainingTodos = async () => {
  const [pendingView, readyToAcknowledge] = await Promise.all([
    getMyTrainingTaskPage({ pageNo: 1, pageSize: 5, status: 'PENDING_VIEW' }),
    getMyTrainingTaskPage({ pageNo: 1, pageSize: 5, status: 'READY_TO_ACKNOWLEDGE' })
  ])
  const rows = [
    ...(readyToAcknowledge.list || []),
    ...(pendingView.list || [])
  ] as TrainingTaskProgressVO[]
  return {
    rows: rows.slice(0, 5).map(toWorkbenchTrainingRow),
    total: Number(pendingView.total || 0) + Number(readyToAcknowledge.total || 0)
  }
}

const loadWorkbench = async () => {
  loading.value = true
  loadErrorMessage.value = ''
  try {
    const [approvalTodos, pendingDistribution, trainingTodos, finalizationFailed] = await Promise.all([
      buildTaskRows(),
      loadFilePageByStatus('PENDING_MANUAL_DISTRIBUTION'),
      loadTrainingTodos(),
      loadFilePageByStatus('FINALIZATION_FAILED')
    ])

    approvalTodoRows.value = approvalTodos.rows
    pendingDistributionRows.value = pendingDistribution.rows
    trainingTodoRows.value = trainingTodos.rows
    finalizationFailedRows.value = finalizationFailed.rows
    metricItems.value = buildDccWorkbenchMetricItems({
      approvalTodoTotal: approvalTodos.total,
      pendingDistributionTotal: pendingDistribution.total,
      trainingTodoTotal: trainingTodos.total,
      finalizationFailedTotal: finalizationFailed.total
    })
    lastLoadedAt.value = new Date().toLocaleString()
  } catch (error) {
    approvalTodoRows.value = []
    pendingDistributionRows.value = []
    trainingTodoRows.value = []
    finalizationFailedRows.value = []
    metricItems.value = buildDccWorkbenchMetricItems({
      approvalTodoTotal: 0,
      pendingDistributionTotal: 0,
      trainingTodoTotal: 0,
      finalizationFailedTotal: 0
    })
    loadErrorMessage.value = resolveWorkbenchErrorMessage(
      error,
      'DCC 工作台加载失败，请查看接口错误后重试。'
    )
  } finally {
    loading.value = false
  }
}

onMounted(loadWorkbench)
</script>

<style scoped>
.dcc-workbench-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dcc-workbench-title {
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
}

.dcc-workbench-updated {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.dcc-workbench-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dcc-workbench-metric-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.dcc-workbench-metric {
  display: flex;
  min-height: 72px;
  cursor: pointer;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
  color: #172033;
  text-align: left;
}

.dcc-workbench-metric:hover,
.dcc-workbench-metric:focus-visible {
  border-color: var(--el-color-primary);
  outline: none;
}

.dcc-workbench-metric__label {
  font-size: 13px;
  font-weight: 600;
}

.dcc-workbench-metric__count {
  font-variant-numeric: tabular-nums;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
}

.dcc-workbench-metric--primary .dcc-workbench-metric__count {
  color: var(--el-color-primary);
}

.dcc-workbench-metric--warning .dcc-workbench-metric__count {
  color: var(--el-color-warning);
}

.dcc-workbench-metric--danger .dcc-workbench-metric__count {
  color: var(--el-color-danger);
}

.dcc-workbench-metric--success .dcc-workbench-metric__count {
  color: var(--el-color-success);
}

.dcc-workbench-panel-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dcc-workbench-panel {
  min-width: 0;
}

.dcc-workbench-panel--wide {
  grid-column: 1 / -1;
}

.dcc-workbench-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.dcc-workbench-panel__title {
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
}

.dcc-workbench-panel :deep(.el-table) {
  font-size: 13px;
}

.dcc-workbench-file-context {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.dcc-workbench-file-context__title {
  overflow: hidden;
  color: #172033;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dcc-workbench-file-context__meta {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px 10px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.3;
}

.dcc-workbench-row-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2px 8px;
}

.dcc-workbench-row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .dcc-workbench-panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dcc-workbench-header {
    flex-direction: column;
  }

  .dcc-workbench-actions {
    justify-content: flex-start;
  }
}
</style>
