<template>
  <ContentWrap>
    <div v-loading="loading" class="edhr-field-audit-detail">
      <div class="edhr-field-audit-detail__header">
        <div>
          <div class="edhr-field-audit-detail__title">字段审计详情</div>
          <div class="edhr-field-audit-detail__subtitle">
            展示字段变更、签名结果和校验结论，技术证据在展开区查看
          </div>
        </div>
        <div class="edhr-field-audit-detail__actions">
          <el-button @click="backToPage">
            <Icon icon="ep:arrow-left" class="mr-5px" />
            返回
          </el-button>
          <el-button
            v-hasPermi="['mes:pro-batch-record-execution:field-audit-verify']"
            type="primary"
            :loading="verifyLoading"
            :disabled="!detail"
            @click="handleVerify"
          >
            校验链
          </el-button>
        </div>
      </div>

      <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
      <el-alert
        v-if="verificationError"
        :title="verificationError"
        type="error"
        :closable="false"
        show-icon
      />

      <template v-if="detail">
        <div class="edhr-field-audit-detail__section">
          <div class="edhr-field-audit-detail__section-title">审计摘要</div>
          <div class="edhr-field-audit-detail__summary-grid">
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">执行记录</div>
              <div class="edhr-field-audit-detail__value">
                {{ detail.executionCode || detail.executionId || '--' }}
              </div>
            </div>
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">审计区间</div>
              <div class="edhr-field-audit-detail__value">
                {{ detail.auditBatch?.beforeFieldAuditRevision ?? '--' }}
                →
                {{ detail.auditBatch?.afterFieldAuditRevision ?? '--' }}
              </div>
            </div>
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">变更时间</div>
              <div class="edhr-field-audit-detail__value">
                {{ formatEdhrDateTime(detail.auditBatch?.changedAt || detail.signature?.signedAt) }}
              </div>
            </div>
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">签名人</div>
              <div class="edhr-field-audit-detail__value">
                {{ detail.signature?.actorName || '--' }}
              </div>
            </div>
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">签名结果</div>
              <div class="edhr-field-audit-detail__value">
                <el-tag :type="detail.signature?.passwordVerified === true ? 'success' : 'danger'">
                  {{ detail.signature?.passwordVerified === true ? '已验证' : '未通过' }}
                </el-tag>
              </div>
            </div>
            <div class="edhr-field-audit-detail__summary-item">
              <div class="edhr-field-audit-detail__label">校验结果</div>
              <div class="edhr-field-audit-detail__value">
                <el-tag :type="resolveHashStatusType(detail.hashVerification?.status)">
                  {{ resolveHashStatusLabel(detail.hashVerification?.status) }}
                </el-tag>
              </div>
            </div>
            <div
              v-if="detail.hashVerification?.failedReason"
              class="edhr-field-audit-detail__summary-item edhr-field-audit-detail__summary-item--wide"
            >
              <div class="edhr-field-audit-detail__label">异常原因</div>
              <div class="edhr-field-audit-detail__value">
                {{ detail.hashVerification?.failedReason }}
              </div>
            </div>
          </div>
        </div>

        <div class="edhr-field-audit-detail__section">
          <div class="edhr-field-audit-detail__section-title">变更明细</div>
          <el-table
            :data="detail.items"
            border
            :show-overflow-tooltip="true"
            empty-text="暂无字段变更明细"
          >
            <el-table-column type="expand" width="44">
              <template #default="{ row }">
                <div class="edhr-field-audit-detail__evidence">
                  <div class="edhr-field-audit-detail__evidence-title">字段证据</div>
                  <div class="edhr-field-audit-detail__evidence-grid">
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">字段路径</div>
                      <div class="edhr-field-audit-detail__value">{{ row.fieldPath || '--' }}</div>
                    </div>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">字段标识</div>
                      <div class="edhr-field-audit-detail__value">{{ row.fieldKey || '--' }}</div>
                    </div>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">组件</div>
                      <div class="edhr-field-audit-detail__value">{{ row.component || '--' }}</div>
                    </div>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">定位</div>
                      <div class="edhr-field-audit-detail__value">
                        rowIndex={{ row.rowIndex }} / columnIndex={{ row.columnIndex }}
                      </div>
                    </div>
                    <template v-if="isRecordbookSyncAudit(row)">
                      <div class="edhr-field-audit-detail__evidence-item">
                        <div class="edhr-field-audit-detail__label">记录本填写值 JSON</div>
                        <div class="edhr-field-audit-detail__value">{{ formatJson(row.recordbookValueJson) }}</div>
                      </div>
                      <div class="edhr-field-audit-detail__evidence-item">
                        <div class="edhr-field-audit-detail__label">批记录存储值 JSON</div>
                        <div class="edhr-field-audit-detail__value">{{ formatJson(row.batchRecordValueJson) }}</div>
                      </div>
                    </template>
                    <template v-else>
                      <div class="edhr-field-audit-detail__evidence-item">
                        <div class="edhr-field-audit-detail__label">旧值 JSON</div>
                        <div class="edhr-field-audit-detail__value">{{ formatJson(row.oldValueJson) }}</div>
                      </div>
                      <div class="edhr-field-audit-detail__evidence-item">
                        <div class="edhr-field-audit-detail__label">旧值 hash</div>
                        <div class="edhr-field-audit-detail__value">{{ row.oldValueHash || '--' }}</div>
                      </div>
                      <div class="edhr-field-audit-detail__evidence-item">
                        <div class="edhr-field-audit-detail__label">新值 JSON</div>
                        <div class="edhr-field-audit-detail__value">{{ formatJson(row.newValueJson) }}</div>
                      </div>
                    </template>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">新值 hash</div>
                      <div class="edhr-field-audit-detail__value">{{ row.newValueHash || '--' }}</div>
                    </div>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">前序审计哈希</div>
                      <div class="edhr-field-audit-detail__value">{{ row.previousHash || '--' }}</div>
                    </div>
                    <div class="edhr-field-audit-detail__evidence-item">
                      <div class="edhr-field-audit-detail__label">当前审计哈希</div>
                      <div class="edhr-field-audit-detail__value">{{ row.auditHash || '--' }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="字段" min-width="220">
              <template #default="{ row }">
                <div class="edhr-field-audit-detail__strong">{{ row.fieldLabel || row.fieldKey || '--' }}</div>
                <div class="edhr-field-audit-detail__muted">{{ row.fieldPath || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="变更值" min-width="260">
              <template #default="{ row }">
                <template v-if="isRecordbookSyncAudit(row)">
                  <div class="edhr-field-audit-detail__muted">记录本填写值</div>
                  <div class="edhr-field-audit-detail__change-value">
                    {{ row.recordbookValueDisplay ?? '--' }}
                  </div>
                  <div class="edhr-field-audit-detail__muted">批记录存储值</div>
                  <div class="edhr-field-audit-detail__change-value">
                    {{ row.batchRecordValueDisplay ?? '--' }}
                  </div>
                </template>
                <template v-else>
                  <div class="edhr-field-audit-detail__change-value">
                    {{ row.oldValueDisplay || '--' }}
                  </div>
                  <div class="edhr-field-audit-detail__change-arrow">→</div>
                  <div class="edhr-field-audit-detail__change-value">
                    {{ row.newValueDisplay || '--' }}
                  </div>
                </template>
              </template>
            </el-table-column>
            <el-table-column label="原因" min-width="220">
              <template #default="{ row }">
                <div>{{ row.reasonCategory || '--' }}</div>
                <div class="edhr-field-audit-detail__muted">{{ row.reasonText || '--' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="修改人 / 时间" min-width="180">
              <template #default="{ row }">
                <div class="edhr-field-audit-detail__strong">{{ row.actorName || '--' }}</div>
                <div class="edhr-field-audit-detail__muted">{{ formatEdhrDateTime(row.changedAt) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="签名" width="130">
              <template #default="{ row }">
                <div>{{ row.signatureId || '--' }}</div>
                <div class="edhr-field-audit-detail__muted">{{ FIELD_CHANGE_ACTION_LABEL }}</div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-collapse
          v-model="fieldAuditDetailTechnicalEvidenceNames"
          class="edhr-field-audit-detail__technical-evidence"
        >
          <el-collapse-item title="技术证据" name="technical-evidence">
            <div class="edhr-field-audit-detail__technical-content">
              <div>
                <div class="edhr-field-audit-detail__technical-title">签名与校验明细</div>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="签名编号">
                    {{ detail.signature?.signatureId || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="签名动作">{{ FIELD_CHANGE_ACTION_LABEL }}</el-descriptions-item>
                  <el-descriptions-item label="动作码">{{ FIELD_CHANGE_ACTION_CODE }}</el-descriptions-item>
                  <el-descriptions-item label="签名方式">
                    {{ detail.signature?.signatureMode || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="签名人">
                    {{ detail.signature?.actorName || '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="密码验证">
                    {{ detail.signature?.passwordVerified === true ? '通过' : '未通过' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="签名时间">
                    {{ formatEdhrDateTime(detail.signature?.signedAt) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="校验状态">
                    <el-tag :type="resolveHashStatusType(detail.hashVerification?.status)">
                      {{ resolveHashStatusLabel(detail.hashVerification?.status) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="校验时间">
                    {{ formatEdhrDateTime(detail.hashVerification?.checkedAt) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="校验批次数">
                    {{ detail.hashVerification?.checkedBatchCount ?? '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="校验明细数">
                    {{ detail.hashVerification?.checkedItemCount ?? '--' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="失败原因" :span="2">
                    {{ detail.hashVerification?.failedReason || '--' }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <div>
                <div class="edhr-field-audit-detail__technical-title">链路证据</div>
                <div class="edhr-field-audit-detail__evidence-grid">
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">批次 ID</div>
                    <div class="edhr-field-audit-detail__value">{{ detail.auditBatch?.id || '--' }}</div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">基础字段审计头哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.auditBatch?.baseFieldAuditHeadHash || '--' }}
                    </div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">前序头哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.auditBatch?.previousHeadHash || '--' }}
                    </div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">新头哈希</div>
                    <div class="edhr-field-audit-detail__value">{{ detail.auditBatch?.newHeadHash || '--' }}</div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">签名挑战哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.signature?.signatureChallengeHash || detail.auditBatch?.signatureChallengeHash || '--' }}
                    </div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">签名投影哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.auditBatch?.signatureProjectionHash || '--' }}
                    </div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">计算头哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.hashVerification?.calculatedHeadHash || '--' }}
                    </div>
                  </div>
                  <div class="edhr-field-audit-detail__evidence-item">
                    <div class="edhr-field-audit-detail__label">存储头哈希</div>
                    <div class="edhr-field-audit-detail__value">
                      {{ detail.hashVerification?.storedHeadHash || '--' }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </template>
    </div>
  </ContentWrap>
</template>

<script setup lang="ts">
import {
  EDHR_HASH_STATUS_LABEL_MAP,
  EDHR_HASH_STATUS_TAG_TYPE_MAP,
  getEdhrFieldAuditDetail,
  verifyEdhrFieldAuditChain,
  type EdhrFieldAuditDetailReqVO,
  type EdhrFieldAuditDetailRespVO,
  type EdhrFieldAuditEntryVO
} from '@/api/mes/pro/edhr/fieldAudit'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import { formatEdhrDateTime } from '@/views/mes/pro/edhr/shared/dateTime'

defineOptions({ name: 'MesProFeedbackEdhrFieldAuditDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const loading = ref(false)
const verifyLoading = ref(false)
const loadError = ref('')
const verificationError = ref('')
const detail = ref<EdhrFieldAuditDetailRespVO>()
const loadedDetailQueryKey = ref('')
const fieldAuditDetailTechnicalEvidenceNames = ref<string[]>([])
const FIELD_CHANGE_ACTION_LABEL = '字段变更'
const FIELD_CHANGE_ACTION_CODE = 'FIELD_CHANGE'

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const parseDetailQuery = (): EdhrFieldAuditDetailReqVO | undefined => {
  const executionId = parsePositiveRouteQueryId(route.query.executionId)
  const auditBatchId = parsePositiveRouteQueryId(route.query.auditBatchId) || undefined
  const auditItemId = parsePositiveRouteQueryId(route.query.auditItemId) || undefined
  if (!executionId) {
    loadError.value = '缺少 executionId，无法加载字段审计详情。'
    return undefined
  }
  if (!auditBatchId && !auditItemId) {
    loadError.value = '缺少 auditBatchId 或 auditItemId，无法加载字段审计详情。'
    return undefined
  }
  return { executionId, auditBatchId, auditItemId }
}

const resolveDetailQueryKey = (detailQuery: EdhrFieldAuditDetailReqVO) =>
  `${detailQuery.executionId}:${detailQuery.auditBatchId || ''}:${detailQuery.auditItemId || ''}`

const resolveHashStatusLabel = (status?: string) => {
  return status && status in EDHR_HASH_STATUS_LABEL_MAP
    ? EDHR_HASH_STATUS_LABEL_MAP[status as keyof typeof EDHR_HASH_STATUS_LABEL_MAP]
    : status || '--'
}

const resolveHashStatusType = (status?: string) => {
  return status && status in EDHR_HASH_STATUS_TAG_TYPE_MAP
    ? EDHR_HASH_STATUS_TAG_TYPE_MAP[status as keyof typeof EDHR_HASH_STATUS_TAG_TYPE_MAP]
    : 'info'
}

const formatJson = (value: unknown) => (value === undefined ? '--' : JSON.stringify(value))
const isRecordbookSyncAudit = (row: EdhrFieldAuditEntryVO) =>
  row.recordbookValueJson !== undefined ||
  row.recordbookValueDisplay !== undefined ||
  row.batchRecordValueJson !== undefined ||
  row.batchRecordValueDisplay !== undefined

const loadDetail = async () => {
  const detailQuery = parseDetailQuery()
  if (!detailQuery) {
    detail.value = undefined
    loadedDetailQueryKey.value = ''
    return
  }
  loading.value = true
  loadError.value = ''
  verificationError.value = ''
  try {
    detail.value = await getEdhrFieldAuditDetail(detailQuery)
    loadedDetailQueryKey.value = resolveDetailQueryKey(detailQuery)
    if (detail.value.hashVerification?.status && detail.value.hashVerification.status !== 'VALID') {
      verificationError.value = `字段审计链校验未通过：${resolveHashStatusLabel(detail.value.hashVerification.status)}`
    }
  } catch (error) {
    detail.value = undefined
    loadedDetailQueryKey.value = ''
    loadError.value = resolveErrorMessage(error, '字段审计详情加载失败，请联系管理员。')
  } finally {
    loading.value = false
  }
}

const handleVerify = async () => {
  const detailQuery = parseDetailQuery()
  if (!detailQuery || !detail.value) {
    message.error('当前字段审计详情不存在，无法校验。')
    return
  }
  const auditBatch = detail.value.auditBatch
  if (!auditBatch?.newHeadHash || !auditBatch.afterCellValuesHash) {
    verificationError.value = '字段审计批次缺少 newHeadHash 或 afterCellValuesHash，无法校验。'
    message.error(verificationError.value)
    return
  }
  verifyLoading.value = true
  verificationError.value = ''
  try {
    const result = await verifyEdhrFieldAuditChain({
      executionId: detailQuery.executionId,
      toFieldAuditRevision: detail.value.auditBatch?.afterFieldAuditRevision,
      expectedFieldAuditHeadHash: detail.value.auditBatch?.newHeadHash,
      expectedCellValuesHash: detail.value.auditBatch?.afterCellValuesHash,
      includeBrokenItem: true
    })
    if (result.hashVerification.status !== 'VALID') {
      verificationError.value = `字段审计链校验未通过：${resolveHashStatusLabel(result.hashVerification.status)}`
      return
    }
    message.success('字段审计链校验通过')
    await loadDetail()
  } catch (error) {
    verificationError.value = resolveErrorMessage(error, '字段审计链校验失败，请联系管理员。')
  } finally {
    verifyLoading.value = false
  }
}

const backToPage = async () => {
  await router.push({
    path: '/mes/pro/feedback/edhr-field-audit',
    query: detail.value?.executionId
      ? {
          executionId: String(detail.value.executionId)
        }
      : undefined
  })
}

watch(
  () =>
    [
      route.name,
      route.query.executionId,
      route.query.auditBatchId,
      route.query.auditItemId
    ] as const,
  ([routeName]) => {
    if (routeName !== 'MesProFeedbackEdhrFieldAuditDetail') {
      return
    }
    const nextDetailQuery = parseDetailQuery()
    if (!nextDetailQuery) {
      return
    }
    const nextDetailQueryKey = resolveDetailQueryKey(nextDetailQuery)
    if (loadedDetailQueryKey.value === nextDetailQueryKey) {
      return
    }
    loadDetail()
  }
)

onMounted(() => loadDetail())
</script>

<style scoped>
.edhr-field-audit-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edhr-field-audit-detail__header,
.edhr-field-audit-detail__section {
  padding: 16px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-field-audit-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.edhr-field-audit-detail__title {
  color: #172033;
  font-size: 18px;
  font-weight: 600;
}

.edhr-field-audit-detail__subtitle {
  margin-top: 4px;
  color: #4b5563;
  font-size: 13px;
}

.edhr-field-audit-detail__actions {
  display: flex;
  gap: 12px;
}

.edhr-field-audit-detail__section-title {
  margin-bottom: 12px;
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit-detail__summary-grid,
.edhr-field-audit-detail__evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.edhr-field-audit-detail__summary-item,
.edhr-field-audit-detail__evidence-item {
  min-width: 0;
}

.edhr-field-audit-detail__summary-item--wide {
  grid-column: 1 / -1;
}

.edhr-field-audit-detail__label {
  color: #4b5563;
  font-size: 12px;
}

.edhr-field-audit-detail__value {
  margin-top: 4px;
  color: #172033;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.edhr-field-audit-detail__evidence {
  padding: 12px 16px;
  background: #fafcff;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
}

.edhr-field-audit-detail__evidence-title {
  margin-bottom: 10px;
  color: #172033;
  font-size: 13px;
  font-weight: 600;
}

.edhr-field-audit-detail__technical-evidence {
  padding: 0 16px 12px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
}

.edhr-field-audit-detail__technical-evidence :deep(.el-collapse-item__header) {
  min-height: 48px;
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit-detail__technical-evidence :deep(.el-collapse-item__wrap) {
  border-bottom: 0;
}

.edhr-field-audit-detail__technical-evidence :deep(.el-collapse-item__content) {
  padding-bottom: 4px;
}

.edhr-field-audit-detail__technical-content {
  display: grid;
  gap: 16px;
}

.edhr-field-audit-detail__technical-title {
  margin-bottom: 12px;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
}

.edhr-field-audit-detail__strong {
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit-detail__muted {
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}

.edhr-field-audit-detail__change-value {
  color: #172033;
  font-weight: 600;
}

.edhr-field-audit-detail__change-arrow {
  margin: 2px 0;
  color: #4b5563;
  font-size: 12px;
}
</style>
