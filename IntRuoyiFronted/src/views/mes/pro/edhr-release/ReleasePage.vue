<template>
  <ContentWrap>
    <div class="edhr-release-page">
      <el-form :inline="true" :model="queryParams" class="edhr-release-page__toolbar">
        <div class="edhr-release-page__title">电子批记录放行追溯</div>
        <el-form-item label="批次执行">
          <el-input v-model="queryParams.batchExecutionCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="工单号">
          <el-input v-model="queryParams.workOrderCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="queryParams.batchCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="放行状态">
          <el-select v-model="queryParams.releaseStatus" clearable class="!w-170px">
            <el-option label="待预检" value="PRECHECK_REQUIRED" />
            <el-option label="预检失败" value="PRECHECK_FAILED" />
            <el-option label="预检通过" value="PRECHECK_PASSED" />
            <el-option label="待审批" value="PENDING_APPROVAL" />
            <el-option label="已放行" value="RELEASED" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="已撤回" value="WITHDRAWN" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert v-if="actionError" :title="actionError" type="error" :closable="false" show-icon />

      <div class="edhr-release-page__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无放行追溯记录"
        >
          <el-table-column label="放行对象" min-width="250">
            <template #default="{ row }">
              <div class="edhr-release-page__strong">{{ row.batchExecutionCode || '--' }}</div>
              <div class="edhr-release-page__muted">工单：{{ row.workOrderCode || '--' }}</div>
              <div class="edhr-release-page__muted">批次：{{ row.batchCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="产品/路线" min-width="230">
            <template #default="{ row }">
              <div class="edhr-release-page__strong">{{ row.productName || '--' }}</div>
              <div class="edhr-release-page__muted">产品：{{ row.productCode || '--' }}</div>
              <div class="edhr-release-page__muted">路线：{{ row.routeName || row.routeCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="放行状态" width="145">
            <template #default="{ row }">
              <el-tag :type="resolveReleaseTagType(row.releaseStatus)">
                {{ resolveReleaseStatusLabel(row.releaseStatus) }}
              </el-tag>
              <div class="edhr-release-page__muted">{{ row.precheckSummary || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="检查摘要" width="170">
            <template #default="{ row }">
              <div class="edhr-release-page__metric">
                阻塞 <strong>{{ row.blockingCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-release-page__metric">
                失败 <strong>{{ row.failedCheckCount || 0 }}</strong>
              </div>
              <div class="edhr-release-page__muted">最后：{{ formatDateTime(row.lastPrecheckAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="质量门禁" min-width="260">
            <template #default="{ row }">
              <div class="edhr-release-page__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.dhrStatus)">DHR {{ resolveReleaseCheckResultLabel(row.dhrStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inspectionStatus)">检验 {{ resolveReleaseCheckResultLabel(row.inspectionStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.inventoryStatus)">库存 {{ resolveReleaseCheckResultLabel(row.inventoryStatus) }}</el-tag>
              </div>
              <div class="edhr-release-page__gate-row">
                <el-tag :type="resolveReleaseCheckResultTagType(row.deviationStatus)">偏差 {{ resolveReleaseCheckResultLabel(row.deviationStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.reworkStatus)">返工 {{ resolveReleaseCheckResultLabel(row.reworkStatus) }}</el-tag>
                <el-tag :type="resolveReleaseCheckResultTagType(row.scrapStatus)">报废 {{ resolveReleaseCheckResultLabel(row.scrapStatus) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="事务时间" min-width="180">
            <template #default="{ row }">
              <div class="edhr-release-page__muted">提交：{{ formatDateTime(row.submittedAt) }}</div>
              <div class="edhr-release-page__muted">批准：{{ formatDateTime(row.approvedAt) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="追溯" width="160" fixed="right">
            <template #default="{ row }">
              <div class="edhr-release-page__actions">
                <el-button link type="primary" :disabled="!row.releaseTransactionId" @click="openCheckItems(row)">
                  检查项
                </el-button>
                <el-button
                  v-hasPermi="['mes:pro-edhr-release:event-query']"
                  link
                  type="primary"
                  :disabled="!row.releaseTransactionId"
                  @click="openEventDrawer(row)"
                >
                  事务事件
                </el-button>
              </div>
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

    <el-drawer v-model="drawerVisible" title="电子批记录放行检查项" size="82%" class="edhr-release-page__drawer">
      <el-alert
        v-if="currentRow"
        :title="`${currentRow.batchExecutionCode} / ${currentRow.precheckSummary || '检查项'}`"
        type="info"
        :closable="false"
        show-icon
        class="edhr-release-page__drawer-alert"
      />
      <el-table
        v-loading="checkItemLoading"
        :data="checkItems"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无检查项"
      >
        <el-table-column label="检查项" min-width="210">
          <template #default="{ row }">
            <div class="edhr-release-page__strong">{{ resolveReleaseCheckCodeLabel(row.checkCode) }}</div>
            <div class="edhr-release-page__muted">
              分类：{{ resolveReleaseCheckCategoryLabel(row.checkCategory) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="115">
          <template #default="{ row }">
            <el-tag :type="resolveReleaseCheckResultTagType(row.checkResult)">
              {{ resolveReleaseCheckResultLabel(row.checkResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="责任模块" width="110" prop="responsibilityModule" />
        <el-table-column label="源对象" min-width="190">
          <template #default="{ row }">
            <div>{{ row.sourceObjectCode || '--' }}</div>
            <div class="edhr-release-page__muted">{{ resolveReleaseCheckSourceObjectTypeLabel(row.sourceObjectType) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="260" prop="failureReason" />
        <el-table-column label="下一步动作" min-width="260" prop="remediationSuggestion" />
      </el-table>
      <Pagination
        :total="checkItemTotal"
        v-model:page="checkItemQuery.pageNo"
        v-model:limit="checkItemQuery.pageSize"
        @pagination="getCheckItems"
      />
    </el-drawer>

    <el-drawer v-model="eventDrawerVisible" title="电子批记录放行事务事件" size="82%" class="edhr-release-page__drawer">
      <el-table
        v-loading="eventLoading"
        :data="eventList"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无事务事件"
      >
        <el-table-column label="事件" width="120">
          <template #default="{ row }">
            <el-tag>{{ resolveReleaseEventLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态变化" width="190">
          <template #default="{ row }">
            {{ resolveReleaseStatusLabel(row.fromStatus) }} → {{ resolveReleaseStatusLabel(row.toStatus) }}
          </template>
        </el-table-column>
        <el-table-column label="幂等键" min-width="230" prop="idempotencyKey" />
        <el-table-column label="签核证据" min-width="230" prop="signoffEvidenceHash" />
        <el-table-column label="原因/意见" min-width="260">
          <template #default="{ row }">
            <div>{{ row.reason || row.opinion || '--' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="110" prop="actorUserId" />
        <el-table-column label="发生时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="eventTotal"
        v-model:page="eventQuery.pageNo"
        v-model:limit="eventQuery.pageSize"
        @pagination="loadEventList"
      />
    </el-drawer>

  </ContentWrap>
</template>

<script setup lang="ts">
import {
  getEdhrReleaseCheckItemPage,
  getEdhrReleaseEventPage,
  getEdhrReleasePage,
  type EdhrReleaseCheckItemPageReqVO,
  type EdhrReleaseCheckItemVO,
  type EdhrReleaseEventPageReqVO,
  type EdhrReleaseEventRespVO,
  type EdhrReleaseRowVO,
  type EdhrReleaseStatus
} from '@/api/mes/pro/edhr/release'
import {
  resolveReleaseCheckCategoryLabel,
  resolveReleaseCheckCodeLabel,
  resolveReleaseCheckResultLabel,
  resolveReleaseCheckResultTagType,
  resolveReleaseCheckSourceObjectTypeLabel,
  resolveReleaseEventLabel,
  resolveReleaseStatusLabel,
  resolveReleaseTagType
} from '@/views/mes/pro/edhr/shared/releaseCheckPresentation'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProEdhrReleasePage' })

const message = useMessage()

const loading = ref(false)
const checkItemLoading = ref(false)
const eventLoading = ref(false)
const loadError = ref('')
const actionError = ref('')
const list = ref<EdhrReleaseRowVO[]>([])
const total = ref(0)
const drawerVisible = ref(false)
const eventDrawerVisible = ref(false)
const currentRow = ref<EdhrReleaseRowVO>()
const checkItems = ref<EdhrReleaseCheckItemVO[]>([])
const eventList = ref<EdhrReleaseEventRespVO[]>([])
const checkItemTotal = ref(0)
const eventTotal = ref(0)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  batchExecutionCode: '',
  workOrderCode: '',
  batchCode: '',
  productCode: '',
  releaseStatus: '' as EdhrReleaseStatus | ''
})

const checkItemQuery = reactive<EdhrReleaseCheckItemPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  releaseTransactionId: 0,
  itemStatus: 'OPEN' as const,
  checkResult: ''
})

const eventQuery = reactive<EdhrReleaseEventPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  releaseTransactionId: 0,
  eventType: ''
})

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage
  }
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return fallback
}

const buildQuery = () => ({
  pageNo: queryParams.pageNo,
  pageSize: queryParams.pageSize,
  batchExecutionCode: queryParams.batchExecutionCode.trim() || undefined,
  workOrderCode: queryParams.workOrderCode.trim() || undefined,
  batchCode: queryParams.batchCode.trim() || undefined,
  productCode: queryParams.productCode.trim() || undefined,
  releaseStatus: queryParams.releaseStatus || undefined
})

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrReleasePage(buildQuery())
    list.value = data.list || []
    total.value = data.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '电子批记录放行追溯列表加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.pageSize = 10
  queryParams.batchExecutionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.productCode = ''
  queryParams.releaseStatus = ''
  getList()
}

const openCheckItems = async (row: EdhrReleaseRowVO) => {
  if (!row.releaseTransactionId) {
    actionError.value = '该放行记录尚未生成放行事务，暂无检查项。'
    return
  }
  currentRow.value = row
  drawerVisible.value = true
  checkItemQuery.pageNo = 1
  checkItemQuery.releaseTransactionId = row.releaseTransactionId
  await getCheckItems()
}

const getCheckItems = async () => {
  if (!checkItemQuery.releaseTransactionId) {
    checkItems.value = []
    checkItemTotal.value = 0
    return
  }
  checkItemLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrReleaseCheckItemPage(checkItemQuery)
    checkItems.value = data.list || []
    checkItemTotal.value = data.total || 0
  } catch (error) {
    checkItems.value = []
    checkItemTotal.value = 0
    actionError.value = resolveErrorMessage(error, '电子批记录放行检查项加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    checkItemLoading.value = false
  }
}

const openEventDrawer = async (row: EdhrReleaseRowVO) => {
  if (!row.releaseTransactionId) {
    actionError.value = '该放行记录尚未生成放行事务，暂无事务事件。'
    return
  }
  currentRow.value = row
  eventDrawerVisible.value = true
  eventQuery.pageNo = 1
  eventQuery.releaseTransactionId = row.releaseTransactionId
  await loadEventList()
}

const loadEventList = async () => {
  if (!eventQuery.releaseTransactionId) {
    eventList.value = []
    eventTotal.value = 0
    return
  }
  eventLoading.value = true
  actionError.value = ''
  try {
    const data = await getEdhrReleaseEventPage(eventQuery)
    eventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    eventList.value = []
    eventTotal.value = 0
    actionError.value = resolveErrorMessage(error, '电子批记录放行事务事件加载失败。')
    message.error(resolveErrorMessage(error, actionError.value))
  } finally {
    eventLoading.value = false
  }
}

const formatDateTime = (value?: string | number) => {
  return formatEdhrDateTime(value)
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.edhr-release-page {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.edhr-release-page__toolbar,
.edhr-release-page__table {
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-release-page__toolbar {
  padding: 16px 16px 0;
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
}

.edhr-release-page__title {
  width: 100%;
  margin-bottom: 12px;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.edhr-release-page__table {
  padding: 16px;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-release-page__strong {
  color: #172033;
  font-weight: 600;
  line-height: 1.5;
}

.edhr-release-page__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
}

.edhr-release-page__metric {
  color: #263247;
  font-size: 13px;
  line-height: 1.6;
}

.edhr-release-page__metric strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.edhr-release-page__gate-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 6px;
}

.edhr-release-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.edhr-release-page__drawer :deep(.el-drawer__body) {
  padding-top: 8px;
}

.edhr-release-page__drawer-alert,
.edhr-release-page__dialog-alert {
  margin-bottom: 12px;
}

.edhr-release-page__dialog-form {
  padding-top: 4px;
}
</style>
