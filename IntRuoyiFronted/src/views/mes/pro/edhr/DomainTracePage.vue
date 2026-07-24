<template>
  <ContentWrap>
    <div class="edhr-domain-trace">
      <el-form :inline="true" :model="queryParams" class="edhr-domain-trace__toolbar">
        <el-form-item label="执行编号">
          <el-input v-model="queryParams.executionCode" clearable class="!w-180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable class="!w-150px">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="校验时间">
          <el-date-picker
            v-model="verifiedAtRange"
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
          <el-button :loading="loading" @click="getList">刷新</el-button>
        </el-form-item>
        <el-form-item class="edhr-domain-trace__advanced">
          <el-collapse v-model="domainTraceAdvancedFilterNames">
            <el-collapse-item title="高级筛选" name="advanced">
              <div class="edhr-domain-trace__advanced-grid">
                <el-form-item label="工单号">
                  <el-input v-model="queryParams.workOrderCode" clearable class="!w-180px" />
                </el-form-item>
                <el-form-item label="批次号">
                  <el-input v-model="queryParams.batchCode" clearable class="!w-180px" />
                </el-form-item>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-form-item>
      </el-form>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div class="edhr-domain-trace__table">
        <el-table
          v-loading="loading"
          :data="list"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无主数据追溯记录"
        >
          <el-table-column type="expand" width="44">
            <template #default="{ row }">
              <div class="edhr-domain-trace__evidence">
                <div class="edhr-domain-trace__evidence-title">追溯证据</div>
                <div class="edhr-domain-trace__evidence-grid">
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">追溯哈希</div>
                    <div class="edhr-domain-trace__value">{{ row.domainTraceHash || '--' }}</div>
                  </div>
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">快照编号</div>
                    <div class="edhr-domain-trace__value">{{ row.domainTraceSnapshotId || '--' }}</div>
                  </div>
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">阻塞数量</div>
                    <div class="edhr-domain-trace__value">{{ resolveBlockerCount(row) }} 项</div>
                  </div>
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">追溯项数量</div>
                    <div class="edhr-domain-trace__value">{{ resolveItemCount(row) }} 项</div>
                  </div>
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">首项对象</div>
                    <div class="edhr-domain-trace__value">{{ resolveFirstItemObject(row) }}</div>
                  </div>
                  <div class="edhr-domain-trace__evidence-item">
                    <div class="edhr-domain-trace__label">首项来源</div>
                    <div class="edhr-domain-trace__value">{{ resolveFirstItemSource(row) }}</div>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="追溯概况" min-width="260">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">
                {{ row.executionCode || row.executionId }}
              </el-button>
              <div class="edhr-domain-trace__muted">
                {{ row.workOrderCode || '未关联工单' }} / {{ row.batchCode || '未关联批次' }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="追溯状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="resolveTraceStatusType(row.status, row)">
                {{ resolveTraceStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阻塞摘要" min-width="280">
            <template #default="{ row }">
              <div :class="{ 'edhr-domain-trace__danger': rowHasBlockers(row) }">
                {{ resolveBlockerSummary(row) }}
              </div>
              <div class="edhr-domain-trace__muted">
                {{ resolveBlockerCount(row) }} 项阻塞
              </div>
            </template>
          </el-table-column>
          <el-table-column label="追溯项" width="110" align="center">
            <template #default="{ row }">
              <el-tag type="info">{{ resolveItemCount(row) }} 项</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近校验" width="180">
            <template #default="{ row }">
              {{ row.verifiedAt || '未校验' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
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
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_DOMAIN_TRACE_QUERY_PERMISSION,
  EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP,
  EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP,
  getEdhrDomainTracePage,
  type EdhrDomainTracePageReqVO,
  type EdhrDomainTracePageRowVO,
  type EdhrDomainTraceStatus
} from '@/api/mes/pro/edhr/domainTrace'
import { hasPermission } from '@/directives/permission/hasPermi'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrDomainTrace' })

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const list = ref<EdhrDomainTracePageRowVO[]>([])
const total = ref(0)
const verifiedAtRange = ref<string[]>([])
const domainTraceAdvancedFilterNames = ref<string[]>([])

const statusOptions: Array<{ label: string; value: EdhrDomainTraceStatus }> = [
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.VERIFIED, value: 'VERIFIED' },
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.BLOCKED, value: 'BLOCKED' },
  { label: EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP.UNVERIFIED, value: 'UNVERIFIED' }
]

const queryParams = reactive<EdhrDomainTracePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  executionId: parsePositiveRouteQueryId(route.query.executionId) || undefined,
  executionCode: typeof route.query.executionCode === 'string' ? route.query.executionCode : '',
  workOrderCode: '',
  batchCode: '',
  status: undefined
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const buildQuery = (): EdhrDomainTracePageReqVO => ({
  ...queryParams,
  executionCode: queryParams.executionCode?.trim() || undefined,
  workOrderCode: queryParams.workOrderCode?.trim() || undefined,
  batchCode: queryParams.batchCode?.trim() || undefined,
  verifiedAtStart: verifiedAtRange.value[0] || undefined,
  verifiedAtEnd: verifiedAtRange.value[1] || undefined
})

const rowHasBlockers = (row: EdhrDomainTracePageRowVO) => {
  return row.status === 'BLOCKED' || Number(row.blockerCount || 0) > 0 || Boolean(row.blockers?.length)
}

const resolveBlockerCount = (row: EdhrDomainTracePageRowVO) => {
  return row.blockerCount ?? row.blockers?.length ?? 0
}

const resolveItemCount = (row: EdhrDomainTracePageRowVO) => {
  return row.itemCount ?? row.items?.length ?? 0
}

const resolveTraceStatusLabel = (status?: string) => {
  return status && status in EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP[status as EdhrDomainTraceStatus]
    : status || '未知'
}

const resolveTraceStatusType = (status?: string, row?: EdhrDomainTracePageRowVO) => {
  if (row && rowHasBlockers(row)) return 'danger'
  return status && status in EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP[status as EdhrDomainTraceStatus]
    : 'warning'
}

const resolveBlockerSummary = (row: EdhrDomainTracePageRowVO) => {
  const firstBlocker = row.blockers?.[0]
  if (firstBlocker?.blockerMessage) {
    return [firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerMessage]
      .filter(Boolean)
      .join(' / ')
  }
  if (firstBlocker?.blockerCode) {
    return [firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerCode]
      .filter(Boolean)
      .join(' / ')
  }
  if (row.status === 'BLOCKED') return '主数据追溯已阻塞，后端未返回阻塞明细。'
  return '无阻塞项'
}

const resolveFirstItemObject = (row: EdhrDomainTracePageRowVO) => {
  const firstItem = row.items?.[0]
  if (!firstItem) return '--'
  return [
    firstItem.itemType,
    firstItem.itemKey,
    firstItem.itemName
  ]
    .filter(Boolean)
    .join(' / ')
}

const resolveFirstItemSource = (row: EdhrDomainTracePageRowVO) => {
  const firstItem = row.items?.[0]
  if (!firstItem) return '--'
  return [
    firstItem.sourceCode || firstItem.sourceId,
    firstItem.sourceVersion,
    firstItem.blockerReason
  ]
    .filter(Boolean)
    .join(' / ')
}

const getList = async () => {
  if (!hasPermission([EDHR_DOMAIN_TRACE_QUERY_PERMISSION])) {
    list.value = []
    total.value = 0
    loadError.value = '当前账号没有主数据追溯查询权限。'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const pageData = await getEdhrDomainTracePage(buildQuery())
    list.value = pageData.list || []
    total.value = pageData.total || 0
  } catch (error) {
    list.value = []
    total.value = 0
    loadError.value = resolveErrorMessage(error, '主数据追溯列表加载失败，请联系管理员。')
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
  queryParams.executionId = undefined
  queryParams.executionCode = ''
  queryParams.workOrderCode = ''
  queryParams.batchCode = ''
  queryParams.status = undefined
  verifiedAtRange.value = []
  getList()
}

const openDetail = async (row: EdhrDomainTracePageRowVO) => {
  await router.push({
    path: '/mes/pro/feedback/edhr-domain-trace/detail',
    query: {
      executionId: String(row.executionId)
    }
  })
}

onMounted(() => getList())
</script>

<style scoped>
.edhr-domain-trace__toolbar,
.edhr-domain-trace__table {
  padding: 16px;
  border: 1px solid #dbe3ef;
  background: #ffffff;
}

.edhr-domain-trace__toolbar {
  border-bottom: 0;
  border-radius: 8px 8px 0 0;
  padding-bottom: 0;
}

.edhr-domain-trace__table {
  border-radius: 0 0 8px 8px;
}

.edhr-domain-trace__advanced {
  display: block;
  width: 100%;
  margin-right: 0;
}

.edhr-domain-trace__advanced :deep(.el-form-item__content) {
  width: 100%;
}

.edhr-domain-trace__advanced :deep(.el-collapse) {
  width: 100%;
  border-top: 1px solid #edf1f6;
  border-bottom: 0;
}

.edhr-domain-trace__advanced :deep(.el-collapse-item__header) {
  min-height: 40px;
  color: #172033;
  font-weight: 600;
}

.edhr-domain-trace__advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, max-content));
  gap: 0 12px;
}

.edhr-domain-trace__table :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-domain-trace__table :deep(.el-table__row) {
  height: 52px;
}

.edhr-domain-trace__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-domain-trace__danger {
  color: #c73636;
  font-weight: 600;
}

.edhr-domain-trace__evidence {
  padding: 12px 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fafcff;
}

.edhr-domain-trace__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-domain-trace__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.edhr-domain-trace__evidence-item {
  min-width: 0;
}

.edhr-domain-trace__label {
  color: #4b5563;
  font-size: 12px;
}

.edhr-domain-trace__value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}
</style>
