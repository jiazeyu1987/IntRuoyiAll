<template>
  <ContentWrap>
    <div class="edhr-operation-audit">
      <el-form :inline="true" :model="queryParams" class="edhr-operation-audit__toolbar">
        <el-form-item label="对象类型" required>
          <el-input v-model="queryParams.objectType" clearable class="!w-160px" />
        </el-form-item>
        <el-form-item label="对象ID" required>
          <el-input v-model="queryParams.objectId" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="记录类型">
          <el-select v-model="queryParams.recordCategory" clearable class="!w-150px">
            <el-option label="批记录表" value="BATCH_RECORD" />
            <el-option v-if="!hideRecordbookMode" label="内部记录表" value="INTERNAL_RECORD" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作类型">
          <el-input v-model="queryParams.operationType" clearable class="!w-170px" />
        </el-form-item>
        <el-form-item label="权限决策">
          <el-select v-model="queryParams.permissionDecision" clearable class="!w-130px">
            <el-option
              v-for="option in permissionDecisionOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select v-model="queryParams.resultStatus" clearable class="!w-140px">
            <el-option
              v-for="option in operationResultOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="发生时间">
          <el-date-picker
            v-model="occurredAtRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="!w-360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
        <el-form-item class="edhr-operation-audit__advanced">
          <el-collapse v-model="operationAuditAdvancedFilterNames">
            <el-collapse-item title="高级筛选" name="advanced">
              <div class="edhr-operation-audit__advanced-grid">
                <el-form-item label="执行ID">
                  <el-input
                    v-model="queryParams.executionId"
                    clearable
                    class="!w-140px"
                  />
                </el-form-item>
                <el-form-item label="批次ID">
                  <el-input
                    v-model="queryParams.batchExecutionId"
                    clearable
                    class="!w-140px"
                  />
                </el-form-item>
                <el-form-item label="操作者ID">
                  <el-input-number
                    v-model="queryParams.actorUserId"
                    :min="1"
                    :controls="false"
                    class="!w-140px"
                  />
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-operation-audit__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无操作审计记录，请输入对象类型和对象ID后查询"
        >
          <el-table-column label="事件ID" prop="id" width="110" />
          <el-table-column label="对象" min-width="220">
            <template #default="{ row }">
              <div class="edhr-operation-audit__strong">
                {{ resolveOperationAuditObjectTypeLabel(row.objectType) }}（ID：{{ row.objectId || '--' }}）
              </div>
              <div class="edhr-operation-audit__muted">执行ID：{{ row.executionId || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="动作" min-width="180">
            <template #default="{ row }">
              <div>{{ resolveOperationActionLabel(row) }}</div>
              <div class="edhr-operation-audit__muted">动作类型：{{ resolveOperationTypeLabel(row.operationType) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="110">
            <template #default="{ row }">
              <el-tag :type="resolveResultTagType(row.resultStatus)">
                {{ resolveResultStatusLabel(row.resultStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="权限决策" width="120">
            <template #default="{ row }">
              <el-tag :type="resolvePermissionDecisionTagType(row.permissionDecision)">
                {{ resolvePermissionDecisionLabel(row.permissionDecision) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作者" min-width="150">
            <template #default="{ row }">
              {{ row.actorUsername || row.actorUserId || '--' }}
            </template>
          </el-table-column>
          <el-table-column label="发生时间" prop="occurredAt" width="180" :formatter="edhrDateTimeFormatter" />
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
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

    <Dialog title="操作审计详情" v-model="detailVisible" width="860px">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
      <div v-else-if="detail" class="edhr-operation-audit__detail">
        <el-descriptions title="事件摘要" :column="2" border>
          <el-descriptions-item label="事件ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="请求ID">{{ detail.requestId || '--' }}</el-descriptions-item>
          <el-descriptions-item label="对象">
            {{ resolveOperationAuditObjectTypeLabel(detail.objectType) }}（ID：{{ detail.objectId || '--' }}）
          </el-descriptions-item>
          <el-descriptions-item label="动作">
            {{ resolveOperationActionLabel(detail) }}
          </el-descriptions-item>
          <el-descriptions-item label="结果">
            <el-tag :type="resolveResultTagType(detail.resultStatus)">
              {{ resolveResultStatusLabel(detail.resultStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="权限决策">
            <el-tag :type="resolvePermissionDecisionTagType(detail.permissionDecision)">
              {{ resolvePermissionDecisionLabel(detail.permissionDecision) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作者">
            {{ detail.actorUsername || detail.actorUserId || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ formatEdhrDateTime(detail.occurredAt) }}</el-descriptions-item>
          <el-descriptions-item label="失败码">{{ detail.failureCode || '--' }}</el-descriptions-item>
          <el-descriptions-item label="失败说明">{{ detail.failureMessage || '--' }}</el-descriptions-item>
        </el-descriptions>

        <el-collapse
          v-model="operationAuditDetailTechnicalEvidenceNames"
          class="edhr-operation-audit__technical-evidence"
        >
          <el-collapse-item title="技术证据" name="technical-evidence">
            <div class="edhr-operation-audit__technical-title">审计证据</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="变更前摘要哈希">
                {{ detail.beforeSummaryHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="变更后摘要哈希">
                {{ detail.afterSummaryHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="前序审计哈希">
                {{ detail.previousAuditHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="当前审计哈希">
                {{ detail.auditHash || '--' }}
              </el-descriptions-item>
              <el-descriptions-item label="元数据" :span="2">
                <pre class="edhr-operation-audit__metadata">{{ detail.metadataJson || '--' }}</pre>
              </el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
        </el-collapse>
      </div>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EdhrOperationAuditApi,
  type EdhrOperationAuditPermissionDecision,
  type EdhrOperationAuditRespVO,
  type EdhrOperationAuditResultStatus
} from '@/api/mes/pro/edhr/operationAudit'
import type { EdhrRecordCategory } from '@/api/mes/pro/edhr/batchExecution'
import {
  OPERATION_AUDIT_PERMISSION_DECISION_OPTIONS as permissionDecisionOptions,
  OPERATION_AUDIT_RESULT_OPTIONS as operationResultOptions,
  resolveOperationActionLabel,
  resolveOperationAuditObjectTypeLabel,
  resolveOperationAuditPermissionDecisionLabel as resolvePermissionDecisionLabel,
  resolveOperationAuditPermissionDecisionTagType as resolvePermissionDecisionTagType,
  resolveOperationAuditResultStatusLabel as resolveResultStatusLabel,
  resolveOperationAuditResultTagType as resolveResultTagType,
  resolveOperationTypeLabel
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { edhrDateTimeFormatter, formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrOperationAudit' })

const route = useRoute()
const hideRecordbookMode = computed(() =>
  route.query.hideRecordbookMode === 'true' || route.query.hideRecordbookMode === '1'
)
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrOperationAuditRespVO[]>([])
const total = ref(0)
const occurredAtRange = ref<string[]>([])
const operationAuditAdvancedFilterNames = ref<string[]>([])
const operationAuditDetailTechnicalEvidenceNames = ref<string[]>([])
const detailVisible = ref(false)
const detail = ref<EdhrOperationAuditRespVO>()
const detailError = ref('')

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  objectType: typeof route.query.objectType === 'string' ? route.query.objectType : '',
  objectId: typeof route.query.objectId === 'string' ? route.query.objectId : '',
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  batchExecutionId: parsePositiveRouteQueryId(route.query.batchExecutionId) || undefined,
  workTaskId: parsePositiveRouteQueryId(route.query.workTaskId) || undefined,
  routeId: parsePositiveRouteQueryId(route.query.routeId) || undefined,
  routeProcessId: parsePositiveRouteQueryId(route.query.routeProcessId) || undefined,
  reportId: typeof route.query.reportId === 'string' ? route.query.reportId : '',
  recordCategory: undefined as EdhrRecordCategory | undefined,
  operationType: '',
  permissionDecision: undefined as EdhrOperationAuditPermissionDecision | undefined,
  resultStatus: undefined as EdhrOperationAuditResultStatus | undefined,
  actorUserId: undefined as number | undefined
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const assertObjectContext = () => {
  if (!queryParams.objectType.trim() || !queryParams.objectId.trim()) {
    throw new Error('对象类型和对象ID不能为空，不能查询对象级电子批记录操作审计。')
  }
}

const buildQuery = () => {
  assertObjectContext()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    objectType: queryParams.objectType.trim(),
    objectId: queryParams.objectId.trim(),
    batchExecutionId: parsePositiveRouteQueryId(queryParams.batchExecutionId) || undefined,
    executionId: parsePositiveRouteQueryId(queryParams.executionId) || undefined,
    workTaskId: parsePositiveRouteQueryId(queryParams.workTaskId) || undefined,
    routeId: parsePositiveRouteQueryId(queryParams.routeId) || undefined,
    routeProcessId: parsePositiveRouteQueryId(queryParams.routeProcessId) || undefined,
    reportId: queryParams.reportId.trim() || undefined,
    recordCategory:
      hideRecordbookMode.value && queryParams.recordCategory === 'INTERNAL_RECORD'
        ? undefined
        : queryParams.recordCategory,
    operationType: queryParams.operationType.trim() || undefined,
    actorUserId: queryParams.actorUserId || undefined,
    permissionDecision: queryParams.permissionDecision,
    resultStatus: queryParams.resultStatus,
    occurredAt: occurredAtRange.value.length ? occurredAtRange.value : undefined
  }
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await EdhrOperationAuditApi.getPage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录操作审计加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const openDetail = async (id: number) => {
  detailVisible.value = true
  detail.value = undefined
  detailError.value = ''
  operationAuditDetailTechnicalEvidenceNames.value = []
  try {
    detail.value = await EdhrOperationAuditApi.get(id)
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '电子批记录操作审计详情加载失败，请联系管理员。')
  }
}

const handleQuery = () => {
  if (hideRecordbookMode.value && queryParams.recordCategory === 'INTERNAL_RECORD') {
    queryParams.recordCategory = undefined
  }
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.objectType = ''
  queryParams.objectId = ''
  queryParams.executionId = undefined
  queryParams.batchExecutionId = undefined
  queryParams.workTaskId = undefined
  queryParams.routeId = undefined
  queryParams.routeProcessId = undefined
  queryParams.reportId = ''
  queryParams.recordCategory = undefined
  queryParams.operationType = ''
  queryParams.permissionDecision = undefined
  queryParams.resultStatus = undefined
  queryParams.actorUserId = undefined
  occurredAtRange.value = []
  list.value = []
  total.value = 0
  loadError.value = ''
}

onMounted(() => {
  if (queryParams.objectType && queryParams.objectId) {
    getList()
  }
})
</script>

<style scoped>
.edhr-operation-audit__toolbar,
.edhr-operation-audit__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-operation-audit__toolbar {
  border-radius: 8px 8px 0 0;
  border-bottom: 0;
  padding-bottom: 0;
}

.edhr-operation-audit__table {
  border-radius: 0 0 8px 8px;
}

.edhr-operation-audit__advanced {
  display: block;
  width: 100%;
  margin-right: 0;
}

.edhr-operation-audit__advanced :deep(.el-form-item__content) {
  width: 100%;
}

.edhr-operation-audit__advanced :deep(.el-collapse) {
  width: 100%;
  border-top: 1px solid #edf1f6;
  border-bottom: 0;
}

.edhr-operation-audit__advanced :deep(.el-collapse-item__header) {
  min-height: 40px;
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit__advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, max-content));
  gap: 0 12px;
}

.edhr-operation-audit__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-operation-audit__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-operation-audit__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-operation-audit__detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-operation-audit__technical-evidence {
  padding: 0 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-operation-audit__technical-evidence :deep(.el-collapse-item__header) {
  min-height: 48px;
  color: #172033;
  font-weight: 600;
}

.edhr-operation-audit__technical-evidence :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.edhr-operation-audit__technical-evidence :deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}

.edhr-operation-audit__technical-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-operation-audit__metadata {
  max-height: 180px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
