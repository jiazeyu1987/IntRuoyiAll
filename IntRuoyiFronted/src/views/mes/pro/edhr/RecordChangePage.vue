<template>
  <ContentWrap>
    <div class="edhr-record-change">
      <el-form :inline="true" :model="queryParams" class="edhr-record-change__toolbar" @submit.prevent>
        <el-form-item label="变更类型">
          <el-select v-model="queryParams.changeType" clearable class="!w-150px">
            <el-option
              v-for="option in changeTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.changeStatus" clearable class="!w-150px">
            <el-option
              v-for="option in changeStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="queryParams.targetScope" clearable class="!w-150px">
            <el-option label="执行记录" value="EXECUTION" />
            <el-option label="批次" value="BATCH" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-record-change__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无变更记录"
        >
          <el-table-column type="expand" width="40">
            <template #default="{ row }">
              <div class="edhr-record-change__evidence">
                <div class="edhr-record-change__evidence-title">变更证据</div>
                <div class="edhr-record-change__evidence-grid">
                  <div class="edhr-record-change__evidence-item">
                    <span>申请签名</span>
                    <strong>{{ row.requestSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>审批签名</span>
                    <strong>{{ row.approvalSignatureId || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>原 Head Hash</span>
                    <strong>{{ row.previousHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>新 Head Hash</span>
                    <strong>{{ row.newHeadHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>原归档 Hash</span>
                    <strong>{{ row.previousArchiveHash || '--' }}</strong>
                  </div>
                  <div class="edhr-record-change__evidence-item">
                    <span>新归档 Hash</span>
                    <strong>{{ row.newArchiveHash || '--' }}</strong>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="变更编号" prop="changeCode" min-width="210" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="resolveChangeTypeTag(row.changeType)">
                {{ resolveChangeTypeLabel(row.changeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="resolveChangeStatusTag(row.changeStatus)">
                {{ resolveChangeStatusLabel(row.changeStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象" min-width="190">
            <template #default="{ row }">
              <div>{{ resolveTargetScopeLabel(row.targetScope) }}</div>
              <div class="edhr-record-change__muted">
                批次ID：
                <el-button
                  v-if="canOpenBatchExecution(row)"
                  link
                  type="primary"
                  class="edhr-record-change__object-link"
                  @click="openBatchExecution(row)"
                >
                  {{ formatObjectId(row.batchExecutionId) }}
                </el-button>
                <span v-else class="edhr-record-change__object-link--disabled">--</span>
              </div>
              <div class="edhr-record-change__muted">
                执行ID：
                <el-button
                  v-if="canOpenExecution(row)"
                  link
                  type="primary"
                  class="edhr-record-change__object-link"
                  @click="openExecution(row)"
                >
                  {{ formatObjectId(row.executionId) }}
                </el-button>
                <span v-else class="edhr-record-change__object-link--disabled">--</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态变化" min-width="160">
            <template #default="{ row }">
              <div>{{ resolveExecutionStatusLabel(row.previousStatus) }}</div>
              <div class="edhr-record-change__muted">到 {{ resolveExecutionStatusLabel(row.newStatus) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="原因" min-width="240">
            <template #default="{ row }">
              <div>{{ row.reasonCategory || '--' }}</div>
              <div class="edhr-record-change__muted">{{ row.reasonText || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" prop="requestedAt" width="180" />
          <el-table-column label="生效时间" prop="effectiveAt" width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="total"
          v-model:page="queryParams.pageNo"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </div>

    <Dialog title="eDHR 变更详情" v-model="detailDialogVisible" width="760px">
      <div class="edhr-record-change__detail-summary">
        <div class="edhr-record-change__detail-summary-item">
          <span>变更编号</span>
          <strong>{{ selectedChange?.changeCode || '--' }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>变更对象</span>
          <strong>{{ resolveTargetScopeLabel(selectedChange?.targetScope) }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>状态</span>
          <strong>{{ resolveChangeStatusLabel(selectedChange?.changeStatus) }}</strong>
        </div>
        <div class="edhr-record-change__detail-summary-item">
          <span>生效时间</span>
          <strong>{{ selectedChange?.effectiveAt || '--' }}</strong>
        </div>
      </div>

      <el-descriptions :column="2" border class="edhr-record-change__detail">
        <el-descriptions-item label="变更类型">{{ resolveChangeTypeLabel(selectedChange?.changeType) }}</el-descriptions-item>
        <el-descriptions-item label="状态变化">
          {{ resolveExecutionStatusLabel(selectedChange?.previousStatus) }} -> {{
            resolveExecutionStatusLabel(selectedChange?.newStatus)
          }}
        </el-descriptions-item>
        <el-descriptions-item label="批次ID">
          <el-button
            v-if="canOpenBatchExecution(selectedChange)"
            link
            type="primary"
            class="edhr-record-change__object-link"
            @click="openBatchExecution(selectedChange)"
          >
            {{ formatObjectId(selectedChange?.batchExecutionId) }}
          </el-button>
          <span v-else class="edhr-record-change__object-link--disabled">--</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行ID">
          <el-button
            v-if="canOpenExecution(selectedChange)"
            link
            type="primary"
            class="edhr-record-change__object-link"
            @click="openExecution(selectedChange)"
          >
            {{ formatObjectId(selectedChange?.executionId) }}
          </el-button>
          <span v-else class="edhr-record-change__object-link--disabled">--</span>
        </el-descriptions-item>
        <el-descriptions-item label="原因分类">{{ selectedChange?.reasonCategory || '--' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ selectedChange?.requestedAt || '--' }}</el-descriptions-item>
        <el-descriptions-item label="原因说明" :span="2">{{ selectedChange?.reasonText || '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-collapse v-model="detailEvidenceNames" class="edhr-record-change__evidence-collapse">
        <el-collapse-item title="链路证据" name="chain-evidence">
          <div class="edhr-record-change__evidence-grid">
            <div class="edhr-record-change__evidence-item">
              <span>申请签名</span>
              <strong>{{ selectedChange?.requestSignatureId || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>审批签名</span>
              <strong>{{ selectedChange?.approvalSignatureId || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>原 Head Hash</span>
              <strong>{{ selectedChange?.previousHeadHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>新 Head Hash</span>
              <strong>{{ selectedChange?.newHeadHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>原归档 Hash</span>
              <strong>{{ selectedChange?.previousArchiveHash || '--' }}</strong>
            </div>
            <div class="edhr-record-change__evidence-item">
              <span>新归档 Hash</span>
              <strong>{{ selectedChange?.newArchiveHash || '--' }}</strong>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_CHANGE_STATUS_APPROVED,
  EDHR_CHANGE_STATUS_DRAFT,
  EDHR_CHANGE_STATUS_EFFECTIVE,
  EDHR_CHANGE_STATUS_REJECTED,
  EDHR_CHANGE_STATUS_SUBMITTED,
  EDHR_CHANGE_TYPE_REOPEN,
  EDHR_CHANGE_TYPE_SUPPLEMENT,
  EDHR_CHANGE_TYPE_VOID,
  getEdhrRecordChange,
  getEdhrRecordChangePage,
  type EdhrRecordChangePageReqVO,
  type EdhrRecordChangeRespVO
} from '@/api/mes/pro/edhr/change'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrRecordChange' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrRecordChangeRespVO[]>([])
const total = ref(0)
const detailDialogVisible = ref(false)
const selectedChange = ref<EdhrRecordChangeRespVO>()
const detailEvidenceNames = ref<string[]>([])

const queryParams = reactive<EdhrRecordChangePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  changeType: typeof route.query.changeType === 'string' ? route.query.changeType : undefined,
  targetScope: typeof route.query.targetScope === 'string' ? route.query.targetScope : undefined,
  batchExecutionId: parsePositiveRouteQueryId(route.query.batchExecutionId) || undefined,
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  changeStatus: typeof route.query.changeStatus === 'string' ? route.query.changeStatus : undefined
})

const changeTypeOptions = [
  { label: '作废', value: EDHR_CHANGE_TYPE_VOID },
  { label: '重开', value: EDHR_CHANGE_TYPE_REOPEN },
  { label: '补录', value: EDHR_CHANGE_TYPE_SUPPLEMENT }
]

const changeStatusOptions = [
  { label: '草稿', value: EDHR_CHANGE_STATUS_DRAFT },
  { label: '已提交', value: EDHR_CHANGE_STATUS_SUBMITTED },
  { label: '已批准', value: EDHR_CHANGE_STATUS_APPROVED },
  { label: '已拒绝', value: EDHR_CHANGE_STATUS_REJECTED },
  { label: '已生效', value: EDHR_CHANGE_STATUS_EFFECTIVE }
]

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const resolveChangeTypeLabel = (type?: string) => {
  const labels: Record<string, string> = {
    [EDHR_CHANGE_TYPE_VOID]: '作废',
    [EDHR_CHANGE_TYPE_REOPEN]: '重开',
    [EDHR_CHANGE_TYPE_SUPPLEMENT]: '补录'
  }
  return type ? labels[type] || type : '--'
}

const resolveChangeTypeTag = (type?: string) => {
  if (type === EDHR_CHANGE_TYPE_VOID) return 'danger'
  if (type === EDHR_CHANGE_TYPE_REOPEN) return 'warning'
  if (type === EDHR_CHANGE_TYPE_SUPPLEMENT) return 'primary'
  return 'info'
}

const resolveChangeStatusLabel = (status?: string) => {
  const labels: Record<string, string> = {
    [EDHR_CHANGE_STATUS_DRAFT]: '草稿',
    [EDHR_CHANGE_STATUS_SUBMITTED]: '已提交',
    [EDHR_CHANGE_STATUS_APPROVED]: '已批准',
    [EDHR_CHANGE_STATUS_REJECTED]: '已拒绝',
    [EDHR_CHANGE_STATUS_EFFECTIVE]: '已生效'
  }
  return status ? labels[status] || status : '--'
}

const resolveChangeStatusTag = (status?: string) => {
  if (status === EDHR_CHANGE_STATUS_EFFECTIVE) return 'success'
  if (status === EDHR_CHANGE_STATUS_REJECTED) return 'danger'
  if (status === EDHR_CHANGE_STATUS_SUBMITTED || status === EDHR_CHANGE_STATUS_APPROVED) return 'warning'
  return 'info'
}

const resolveTargetScopeLabel = (scope?: string) => {
  if (scope === 'BATCH') return '批次'
  if (scope === 'EXECUTION') return '执行记录'
  return scope || '--'
}

const resolveExecutionStatusLabel = (status?: string) => {
  if (!status) return '--'
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    SUBMITTED: '待审批',
    APPROVED: '已关闭',
    REJECTED: '已驳回',
    VOIDED: '已作废',
    REOPENED: '已重开'
  }
  return labels[status] || status
}

const isPositiveId = (value?: number) => Number.isFinite(Number(value)) && Number(value) > 0

const formatObjectId = (value?: number) => (isPositiveId(value) ? `#${Number(value)}` : '--')

const canOpenBatchExecution = (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) =>
  isPositiveId(row?.batchExecutionId)

const canOpenExecution = (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => isPositiveId(row?.executionId)

const normalizeQueryParams = () => {
  queryParams.executionId = parsePositiveRouteQueryId(queryParams.executionId) || undefined
  queryParams.batchExecutionId = parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined
}

const getList = async () => {
  normalizeQueryParams()
  loading.value = true
  loadError.value = ''
  try {
    const pageResult = (await getEdhrRecordChangePage(queryParams)) as {
      list?: EdhrRecordChangeRespVO[]
      total?: number
    }
    list.value = pageResult.list ?? []
    total.value = pageResult.total ?? 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, 'eDHR 变更记录加载失败。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const openDetail = async (row: EdhrRecordChangeRespVO) => {
  if (!row.id) {
    message.error('当前变更记录缺少ID，无法查看详情。')
    return
  }
  try {
    selectedChange.value = await getEdhrRecordChange(row.id)
    detailDialogVisible.value = true
    detailEvidenceNames.value = []
  } catch (error) {
    message.error(resolveErrorMessage(error, 'eDHR 变更详情加载失败。'))
  }
}

const openBatchExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'batchExecutionId'>) => {
  if (!canOpenBatchExecution(row)) {
    message.error('当前变更记录缺少批次执行 ID，无法打开批次详情。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-batch-execution/detail',
    query: { id: String(row!.batchExecutionId) }
  })
}

const openExecution = async (row?: Pick<EdhrRecordChangeRespVO, 'executionId'>) => {
  if (!canOpenExecution(row)) {
    message.error('当前变更记录缺少执行 ID，无法打开执行表单。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(row!.executionId) }
  })
}

onMounted(getList)
</script>

<style scoped>
.edhr-record-change {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-record-change__toolbar,
.edhr-record-change__table {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-record-change__toolbar {
  margin-bottom: 0;
  padding: 12px 12px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-record-change__table {
  padding: 0 12px 12px;
  border-radius: 0 0 8px 8px;
}

.edhr-record-change__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-record-change__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-record-change__evidence {
  padding: 14px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.edhr-record-change__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-record-change__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.edhr-record-change__evidence-item,
.edhr-record-change__detail-summary-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-record-change__evidence-item span,
.edhr-record-change__detail-summary-item span {
  display: block;
  margin-bottom: 6px;
  color: #4b5563;
  font-size: 12px;
}

.edhr-record-change__evidence-item strong,
.edhr-record-change__detail-summary-item strong {
  color: #172033;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.edhr-record-change__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-record-change__object-link {
  height: auto;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  vertical-align: baseline;
}

.edhr-record-change__object-link--disabled {
  color: #8a94a6;
}

.edhr-record-change__detail-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.edhr-record-change__evidence-collapse {
  margin-top: 12px;
}

.edhr-record-change__detail {
  margin-top: 4px;
}
</style>
