<template>
  <ContentWrap>
    <div v-loading="loading" class="edhr-domain-trace-detail">
      <div class="edhr-domain-trace-detail__header">
        <div>
          <div class="edhr-domain-trace-detail__title">主数据追溯详情</div>
          <div class="edhr-domain-trace-detail__subtitle">
            展示追溯状态、阻塞原因和追溯明细，技术证据在展开区查看
          </div>
        </div>
        <div class="edhr-domain-trace-detail__actions">
          <el-button @click="backToPage">返回列表</el-button>
          <el-button @click="openExecution">执行表单</el-button>
          <el-button
            v-hasPermi="[EDHR_DOMAIN_TRACE_VERIFY_PERMISSION]"
            type="primary"
            :loading="verifyLoading"
            :disabled="!detail"
            @click="handleVerify"
          >
            校验
          </el-button>
        </div>
      </div>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <template v-if="detail">
        <el-alert
          v-if="blockedMessage"
          :title="blockedMessage"
          type="error"
          :closable="false"
          show-icon
        />

        <div class="edhr-domain-trace-detail__section">
          <div class="edhr-domain-trace-detail__section-title">追溯摘要</div>
          <div class="edhr-domain-trace-detail__summary-grid">
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">执行记录</div>
              <div class="edhr-domain-trace-detail__value">
                {{ detail.executionCode || detail.executionId || '--' }}
              </div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">生产信息</div>
              <div class="edhr-domain-trace-detail__value">
                {{ detail.workOrderCode || '未关联工单' }} / {{ detail.batchCode || '未关联批次' }}
              </div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">追溯状态</div>
              <div class="edhr-domain-trace-detail__value">
                <el-tag :type="resolveTraceStatusType(detail.status)">
                  {{ resolveTraceStatusLabel(detail.status) }}
                </el-tag>
              </div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">填写方式</div>
              <div class="edhr-domain-trace-detail__value">
                <el-tag :type="resolveFillCarrierTagType(route.query.fillCarrier)">
                  {{ resolveFillCarrierLabel(route.query.fillCarrier) }}
                </el-tag>
              </div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">最近校验</div>
              <div class="edhr-domain-trace-detail__value">
                {{ detail.verifiedAt || '未校验' }}
              </div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">阻塞数量</div>
              <div class="edhr-domain-trace-detail__value">{{ resolveBlockerCount(detail) }} 项</div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">追溯项数量</div>
              <div class="edhr-domain-trace-detail__value">{{ resolveItemCount(detail) }} 项</div>
            </div>
            <div class="edhr-domain-trace-detail__summary-item">
              <div class="edhr-domain-trace-detail__label">校验人</div>
              <div class="edhr-domain-trace-detail__value">
                {{ detail.verifiedByName || detail.verifiedBy || '--' }}
              </div>
            </div>
          </div>
        </div>

        <el-collapse
          v-model="domainTraceDetailTechnicalEvidenceNames"
          class="edhr-domain-trace-detail__technical-evidence"
        >
          <el-collapse-item title="技术证据" name="technical-evidence">
            <div class="edhr-domain-trace-detail__technical-content">
              <div class="edhr-domain-trace-detail__technical-title">追溯证据</div>
              <div class="edhr-domain-trace-detail__evidence-grid">
                <div class="edhr-domain-trace-detail__evidence-item">
                  <div class="edhr-domain-trace-detail__label">追溯哈希</div>
                  <div class="edhr-domain-trace-detail__value">{{ detail.domainTraceHash || '--' }}</div>
                </div>
                <div class="edhr-domain-trace-detail__evidence-item">
                  <div class="edhr-domain-trace-detail__label">快照编号</div>
                  <div class="edhr-domain-trace-detail__value">
                    {{ detail.domainTraceSnapshotId || '--' }}
                  </div>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>

        <div class="edhr-domain-trace-detail__section">
          <div class="edhr-domain-trace-detail__section-title">阻塞原因</div>
          <el-table
            :data="detail.blockers"
            border
            :show-overflow-tooltip="true"
            empty-text="暂无阻塞项"
          >
            <el-table-column label="追溯对象" min-width="220">
              <template #default="{ row }">
                <div class="edhr-domain-trace-detail__strong">{{ row.itemKey || '--' }}</div>
                <div class="edhr-domain-trace-detail__muted">{{ row.itemType || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="阻塞码" prop="blockerCode" width="180" />
            <el-table-column label="阻塞说明" prop="blockerMessage" min-width="260" />
          </el-table>
        </div>

        <div class="edhr-domain-trace-detail__section">
          <div class="edhr-domain-trace-detail__section-title">追溯明细</div>
          <el-table
            :data="detail.items"
            border
            :show-overflow-tooltip="true"
            empty-text="暂无追溯项"
          >
            <el-table-column type="expand" width="44">
              <template #default="{ row }">
                <div class="edhr-domain-trace-detail__evidence">
                  <div class="edhr-domain-trace-detail__evidence-title">追溯项证据</div>
                  <div class="edhr-domain-trace-detail__evidence-grid">
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">对象类型</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.itemType || '--' }}</div>
                    </div>
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">对象标识</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.itemKey || '--' }}</div>
                    </div>
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">来源 ID</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.sourceId || '--' }}</div>
                    </div>
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">来源版本</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.sourceVersion || '--' }}</div>
                    </div>
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">快照哈希</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.snapshotHash || '--' }}</div>
                    </div>
                    <div class="edhr-domain-trace-detail__evidence-item">
                      <div class="edhr-domain-trace-detail__label">快照 JSON</div>
                      <div class="edhr-domain-trace-detail__value">{{ row.snapshotJson || '--' }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="追溯对象" min-width="220">
              <template #default="{ row }">
                <div class="edhr-domain-trace-detail__strong">{{ row.itemName || row.itemKey || '--' }}</div>
                <div class="edhr-domain-trace-detail__muted">{{ row.itemType || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="来源" min-width="220">
              <template #default="{ row }">
                <div>{{ row.sourceCode || row.sourceId || '--' }}</div>
                <div class="edhr-domain-trace-detail__muted">{{ row.sourceVersion || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="resolveItemStatusType(row.status)">
                  {{ resolveItemStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="处理结果" min-width="260">
              <template #default="{ row }">
                {{ row.blockerReason || '已匹配主数据来源' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-empty v-else-if="!loadError && !loading" description="暂无主数据追溯详情" />
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_DOMAIN_TRACE_QUERY_PERMISSION,
  EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP,
  EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP,
  EDHR_DOMAIN_TRACE_VERIFY_PERMISSION,
  getEdhrDomainTraceDetail,
  verifyEdhrDomainTrace,
  type EdhrDomainTraceDetailRespVO,
  type EdhrDomainTraceItemVO,
  type EdhrDomainTraceStatus
} from '@/api/mes/pro/edhr/domainTrace'
import { hasPermission } from '@/directives/permission/hasPermi'
import { parsePositiveRouteQueryId, sameRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'MesProFeedbackEdhrDomainTraceDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const verifyLoading = ref(false)
const loadError = ref('')
const detail = ref<EdhrDomainTraceDetailRespVO>()
const domainTraceDetailTechnicalEvidenceNames = ref<string[]>([])

const ITEM_STATUS_LABEL_MAP: Record<string, string> = {
  VERIFIED: '已校验',
  BLOCKED: '已阻塞',
  MISSING: '缺少主数据',
  NOT_APPLICABLE: '不适用'
}

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const parseExecutionId = () => {
  const executionId = parsePositiveRouteQueryId(route.query.executionId)
  if (!executionId) {
    loadError.value = '缺少 executionId，无法加载主数据追溯详情。'
    return undefined
  }
  return executionId
}

const resolveTraceStatusLabel = (status?: string) => {
  return status && status in EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_LABEL_MAP[status as EdhrDomainTraceStatus]
    : status || '未知'
}

const resolveTraceStatusType = (status?: string) => {
  return status && status in EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP
    ? EDHR_DOMAIN_TRACE_STATUS_TAG_TYPE_MAP[status as EdhrDomainTraceStatus]
    : 'warning'
}

const normalizeRouteQueryValue = (value: unknown) => {
  const rawValue = Array.isArray(value) ? value[0] : value
  return typeof rawValue === 'string' ? rawValue : ''
}

const resolveFillCarrierLabel = (value: unknown) => {
  const fillCarrier = normalizeRouteQueryValue(value)
  if (fillCarrier === 'FORM') return '表单'
  if (fillCarrier === 'RECORDBOOK') return '记录本'
  if (fillCarrier === 'UNCONFIGURED') return '未配置'
  return '未指定'
}

const resolveFillCarrierTagType = (value: unknown) => {
  const fillCarrier = normalizeRouteQueryValue(value)
  if (fillCarrier === 'FORM') return 'primary'
  if (fillCarrier === 'RECORDBOOK') return 'success'
  return 'warning'
}

const resolveItemStatusType = (status?: string) => {
  if (status === 'VERIFIED') return 'success'
  if (status === 'BLOCKED' || status === 'MISSING') return 'danger'
  if (status === 'NOT_APPLICABLE') return 'info'
  return 'warning'
}

const resolveItemStatusLabel = (status?: EdhrDomainTraceItemVO['status']) => {
  const rawStatus = status ? String(status) : ''
  return ITEM_STATUS_LABEL_MAP[rawStatus] || rawStatus || '--'
}

const resolveBlockerCount = (current: EdhrDomainTraceDetailRespVO) => {
  return current.blockers?.length || 0
}

const resolveItemCount = (current: EdhrDomainTraceDetailRespVO) => {
  return current.items?.length || 0
}

const blockedMessage = computed(() => {
  if (!detail.value || detail.value.status !== 'BLOCKED') return ''
  const firstBlocker = detail.value.blockers?.[0]
  if (firstBlocker?.blockerMessage) {
    return `主数据追溯校验未通过：${[firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerMessage].filter(Boolean).join(' / ')}`
  }
  if (firstBlocker?.blockerCode) {
    return `主数据追溯校验未通过：${[firstBlocker.itemType, firstBlocker.itemKey, firstBlocker.blockerCode].filter(Boolean).join(' / ')}`
  }
  return '主数据追溯校验未通过：状态为 BLOCKED，但后端未返回阻塞项。'
})

const loadDetail = async () => {
  if (!hasPermission([EDHR_DOMAIN_TRACE_QUERY_PERMISSION])) {
    detail.value = undefined
    loadError.value = '当前账号没有主数据追溯查询权限。'
    return
  }
  const executionId = parseExecutionId()
  if (!executionId) {
    detail.value = undefined
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrDomainTraceDetail({ executionId })
    if (!data?.executionId) {
      throw new Error('主数据追溯详情未返回有效 executionId。')
    }
    detail.value = data
  } catch (error) {
    detail.value = undefined
    loadError.value = resolveErrorMessage(error, '主数据追溯详情加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleVerify = async () => {
  if (!hasPermission([EDHR_DOMAIN_TRACE_VERIFY_PERMISSION])) {
    message.error('当前账号没有主数据追溯校验权限。')
    return
  }
  if (!detail.value?.executionId) {
    message.error('当前主数据追溯详情不存在，无法校验。')
    return
  }
  verifyLoading.value = true
  loadError.value = ''
  try {
    const result = await verifyEdhrDomainTrace({
      executionId: detail.value.executionId,
      expectedDomainTraceHash: detail.value.domainTraceHash
    })
    detail.value = result
    if (result.status !== 'VERIFIED') {
      loadError.value = blockedMessage.value || '主数据追溯校验未通过。'
      message.error(loadError.value)
      return
    }
    message.success('主数据追溯校验通过')
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '主数据追溯校验失败，请联系管理员。')
    message.error(loadError.value)
  } finally {
    verifyLoading.value = false
  }
}

const backToPage = async () => {
  await router.push({
    path: '/mes/pro/feedback/edhr-domain-trace',
    query: detail.value?.executionId ? { executionId: String(detail.value.executionId) } : undefined
  })
}

const openExecution = async () => {
  const executionId = detail.value?.executionId || parseExecutionId()
  if (!executionId) {
    message.error('缺少执行ID，无法打开执行表单。')
    return
  }
  await router.push({
    path: '/mes/pro/feedback/edhr-execution/form',
    query: { id: String(executionId) }
  })
}

watch(
  () => [route.name, route.query.executionId] as const,
  ([routeName, routeExecutionId]) => {
    if (routeName !== 'MesProFeedbackEdhrDomainTraceDetail') {
      return
    }
    const nextExecutionId = parsePositiveRouteQueryId(routeExecutionId)
    if (!nextExecutionId || sameRouteQueryId(nextExecutionId, detail.value?.executionId)) {
      return
    }
    loadDetail()
  }
)

onMounted(() => loadDetail())
</script>

<style scoped>
.edhr-domain-trace-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-domain-trace-detail__header,
.edhr-domain-trace-detail__section {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-domain-trace-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.edhr-domain-trace-detail__title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-domain-trace-detail__subtitle {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
}

.edhr-domain-trace-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.edhr-domain-trace-detail__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-weight: 600;
}

.edhr-domain-trace-detail__technical-evidence {
  padding: 0 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-domain-trace-detail__technical-evidence :deep(.el-collapse-item__header) {
  min-height: 48px;
  color: #172033;
  font-weight: 600;
}

.edhr-domain-trace-detail__technical-evidence :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.edhr-domain-trace-detail__technical-evidence :deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}

.edhr-domain-trace-detail__technical-content {
  display: grid;
  gap: 16px;
}

.edhr-domain-trace-detail__technical-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-domain-trace-detail__summary-grid,
.edhr-domain-trace-detail__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.edhr-domain-trace-detail__summary-item,
.edhr-domain-trace-detail__evidence-item {
  min-width: 0;
}

.edhr-domain-trace-detail__label {
  color: #4b5563;
  font-size: 12px;
}

.edhr-domain-trace-detail__value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-domain-trace-detail__evidence {
  padding: 12px 16px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fafcff;
}

.edhr-domain-trace-detail__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-domain-trace-detail__section :deep(.el-table__header th) {
  height: 46px;
  background: #f7f9fc;
}

.edhr-domain-trace-detail__section :deep(.el-table__row) {
  height: 52px;
}

.edhr-domain-trace-detail__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-domain-trace-detail__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}
</style>
