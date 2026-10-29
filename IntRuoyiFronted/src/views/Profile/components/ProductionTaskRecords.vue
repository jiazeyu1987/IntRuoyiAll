<template>
  <Dialog v-model="visible" title="生产任务记录" width="1100px">
    <el-tabs v-model="view" @tab-change="search">
      <el-tab-pane label="我已处理" name="done" />
      <el-tab-pane label="候选待办" name="candidate" />
      <el-tab-pane label="逾期任务" name="overdue" />
    </el-tabs>
    <el-form inline @submit.prevent="search">
      <el-form-item label="工单"><el-input v-model="query.workOrderCode" clearable /></el-form-item>
      <el-form-item label="批次"><el-input v-model="query.batchCode" clearable /></el-form-item>
      <el-form-item label="工序"><el-input v-model="query.processName" clearable /></el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-table v-loading="loading" :data="rows" max-height="500" row-key="id">
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="责任人">{{ row.assigneeUserName }}</el-descriptions-item>
            <el-descriptions-item label="候选人">{{ row.candidateSnapshotDisplay }}</el-descriptions-item>
            <el-descriptions-item label="责任来源">{{ row.responsibilitySource }}</el-descriptions-item>
            <el-descriptions-item label="审核来源">{{ row.reviewSourceName }}</el-descriptions-item>
            <el-descriptions-item label="处理说明">{{ row.inactionReason || row.reason || row.remark }}</el-descriptions-item>
            <el-descriptions-item label="逾期原因">{{ row.overdueReason }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-table-column>
      <el-table-column prop="taskCode" label="任务编号" min-width="150" />
      <el-table-column label="任务类型" min-width="120"><template #default="{ row }">{{ taskTypeLabel(row.taskType) }}</template></el-table-column>
      <el-table-column prop="workOrderCode" label="工单" min-width="150" />
      <el-table-column prop="batchCode" label="批次" min-width="140" />
      <el-table-column prop="processName" label="工序" min-width="120" />
      <el-table-column label="到期时间" min-width="170"><template #default="{ row }">{{ formatEdhrDateTime(row.dueTime) }}</template></el-table-column>
      <el-table-column label="完成时间" min-width="170"><template #default="{ row }">{{ formatEdhrDateTime(row.completedAt) }}</template></el-table-column>
      <el-table-column label="操作" fixed="right" width="100">
        <template #default="{ row }"><el-button link type="primary" :title="row.blockedReason" :disabled="view !== 'done' && row.actionable === false" @click="openTask(row)">{{ view === 'done' ? '查看' : row.actionable === false ? '暂不可处理' : '进入/处理' }}</el-button></template>
      </el-table-column>
    </el-table>
    <Pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="load" />
  </Dialog>
</template>
<script setup lang="ts">
import { getEdhrWorkTaskDonePage, getEdhrWorkTaskCandidateTodoPage, getEdhrWorkTaskMyPage, type EdhrWorkTaskRespVO } from '@/api/mes/pro/edhr/workTask'
import { navigateToEdhrWorkTask } from '@/utils/edhrWorkTaskNavigation'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
const router = useRouter()
const message = useMessage()
const invalidation = useApprovalCenterInvalidationStore()
const visible = ref(false)
const view = ref('done')
const query = reactive({ pageNo: 1, pageSize: 10, workOrderCode: '', batchCode: '', processName: '', batchExecutionId: '' })
const rows = ref<EdhrWorkTaskRespVO[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
let generation = 0
const context = () => JSON.stringify([invalidation.syncSession().key, view.value, query, visible.value])
const taskTypeLabel = (type: string) => ({ FILL: '填写', REVIEW: '审核', APPROVE: '批准', REWORK: '驳回修改', ARCHIVE: '最终归档', RELEASE_APPROVE: '最终放行审批', PQC_PRODUCTION_RELEASE: 'PQC生产放行', DEVIATION_RECEIVE: '偏差调查', DEVIATION_CORRECT: '偏差纠正', DEVIATION_SIGN: '偏差签署', DEVIATION_APPROVE: '偏差质量审核', DEVIATION_CLOSE: '偏差关闭', RECORD_CHANGE_APPROVE: '记录变更审批', RECORD_CHANGE_CORRECTION: '记录变更补正', DOSSIER_COMPLETE: '资料补齐', PQC_RELEASE_CORRECTION: '放行补正' }[type] || type)
const load = async () => {
  const currentGeneration = ++generation
  const key = context()
  const current = () => visible.value && currentGeneration === generation && key === context()
  rows.value = []
  total.value = 0
  error.value = ''
  loading.value = true
  try {
    const options = { ignoreErrorMessage: true, isRequestContextCurrent: current }
    const result = view.value === 'done'
      ? await getEdhrWorkTaskDonePage({ ...query }, options)
      : view.value === 'candidate'
        ? await getEdhrWorkTaskCandidateTodoPage({ ...query }, options)
        : await getEdhrWorkTaskMyPage({ ...query, status: 'OVERDUE' }, options)
    if (!current()) return
    rows.value = result.list
    total.value = result.total
  } catch (cause) {
    if (current()) error.value = cause instanceof Error ? cause.message : '生产任务记录加载失败，请重试。'
  } finally {
    if (current()) loading.value = false
  }
}
const search = () => { query.pageNo = 1; void load() }
const open = (batchExecutionId = '') => {
  Object.assign(query, { pageNo: 1, pageSize: 10, workOrderCode: '', batchCode: '', processName: '', batchExecutionId })
  view.value = 'done'
  visible.value = true
  void load()
}
const openTask = async (row: EdhrWorkTaskRespVO) => {
  const key = context()
  try {
    await navigateToEdhrWorkTask(router, row, window.location.origin, () => visible.value && key === context())
    visible.value = false
  } catch (cause) {
    message.error(cause instanceof Error ? cause.message : '任务入口打开失败。')
  }
}
watch(visible, (value) => { if (!value) { generation++; rows.value = []; error.value = ''; loading.value = false } })
watch(() => invalidation.syncSession().key, () => { visible.value = false })
onBeforeUnmount(() => { generation++ })
defineExpose({ open })
</script>
