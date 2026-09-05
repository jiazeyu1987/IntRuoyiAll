<template>
  <div v-loading="loading" class="team-leader-workbench__active-order-detail">
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
      show-icon
      data-team-leader-active-order-detail-error
    >
      <template #default>
        <el-button link type="primary" @click="$emit('retry')">重新加载</el-button>
      </template>
    </el-alert>
    <template v-else-if="detail">
      <el-alert
        v-if="stage1SourceWorkOrderCode"
        class="mb-12px"
        type="info"
        :closable="false"
        show-icon
        data-team-leader-stage1-generated-detail-source
        :title="`你点击的是来源订单 ${stage1SourceWorkOrderCode}；当前展示的是 Stage1 新生成测试订单 ${detail.workOrderCode} 的模拟提交结果。`"
      />
      <div class="team-leader-workbench__active-order-detail-summary">
        <div>
          <span>生产订单</span>
          <strong>{{ detail.workOrderCode }}</strong>
        </div>
        <div>
          <span>工艺路线</span>
          <strong>{{ detail.routeName }}</strong>
        </div>
        <div>
          <span>工序数</span>
          <strong>{{ detail.processes.length }}</strong>
        </div>
      </div>

      <el-tabs
        v-model="activeTab"
        data-team-leader-active-order-detail-main-tabs
        class="team-leader-workbench__active-order-detail-tabs"
      >
        <el-tab-pane label="生产提交" name="productionSubmissions">
          <el-tabs
            v-model="productionActiveTab"
            data-team-leader-active-order-detail-production-process-tabs
            class="team-leader-workbench__active-order-detail-inner-tabs"
          >
            <el-tab-pane
              v-for="(process, processIndex) in detail.processes"
              :key="`${process.routeProcessId}-${process.processId}`"
              :name="activeOrderDetailProcessTabName(process, processIndex)"
            >
              <template #label>
                <span
                  data-team-leader-active-order-detail-production-process-tab
                  :title="process.processName"
                >
                  {{ processIndex + 1 }}. {{ process.processName }}
                </span>
              </template>
              <section
                class="team-leader-workbench__active-order-process-detail"
                :class="{ 'is-quantity-conflict': process.quantityConflict }"
              >
                <div class="team-leader-workbench__active-order-process-header">
                  <div class="team-leader-workbench__active-order-process-title">
                    <strong>{{ process.processName }}</strong>
                    <span v-if="process.processCode">{{ process.processCode }}</span>
                  </div>
                  <div class="team-leader-workbench__active-order-process-metrics">
                    <div>
                      <span>应提数量</span>
                      <strong>{{ formatTraceQuantity(process.requiredQuantity) }}</strong>
                    </div>
                    <div>
                      <span>已提交</span>
                      <strong>{{ formatTraceQuantity(process.submittedQuantity) }}</strong>
                    </div>
                    <div>
                      <span>提交记录</span>
                      <strong>{{ process.submissionCount }}</strong>
                    </div>
                    <div v-if="process.quantityConflict">
                      <span>超出数量</span>
                      <strong class="team-leader-workbench__quantity-conflict-text">
                        {{ formatTraceQuantity(process.overageQuantity) }}
                      </strong>
                    </div>
                  </div>
                </div>

                <el-table
                  v-if="process.submissions?.length"
                  :data="process.submissions"
                  size="small"
                  border
                  :row-class-name="resolveActiveOrderSubmissionRowClassName"
                  class="team-leader-workbench__active-order-submission-table"
                >
                  <el-table-column label="提交数量" min-width="120">
                    <template #default="{ row: submission }">
                      {{ formatTraceQuantity(submission.submittedQuantity) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="设备" min-width="220">
                    <template #default="{ row: submission }">
                      {{ formatActiveOrderSubmissionDevices(submission.devices) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="提交人" prop="submitterName" min-width="140" />
                  <el-table-column label="审核人" min-width="140">
                    <template #default="{ row: submission }">
                      <span :class="{ 'is-pending': !submission.reviewerName }">
                        {{ submission.reviewerName || '未审核' }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column label="提交时间" min-width="180">
                    <template #default="{ row: submission }">
                      {{ formatDateTime(submission.submittedAt) }}
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-else :image-size="56" description="暂无一线生产提交" />
              </section>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
        <el-tab-pane label="PQC提交" name="pqcSubmissions">
          <el-tabs
            v-if="pqcProcessGroups.length"
            v-model="pqcActiveTab"
            data-team-leader-active-order-detail-pqc-process-tabs
            class="team-leader-workbench__active-order-detail-inner-tabs"
          >
            <el-tab-pane
              v-for="(pqcProcess, pqcProcessIndex) in pqcProcessGroups"
              :key="pqcProcess.key"
              :name="resolveActiveOrderPqcProcessTabName(pqcProcess, pqcProcessIndex)"
            >
              <template #label>
                <span
                  data-team-leader-active-order-detail-pqc-process-tab
                  :title="pqcProcess.qaProcessName"
                >
                  {{ pqcProcessIndex + 1 }}. {{ pqcProcess.qaProcessName }}
                </span>
              </template>
              <section class="team-leader-workbench__active-order-process-detail">
                <div class="team-leader-workbench__active-order-process-header">
                  <div class="team-leader-workbench__active-order-process-title">
                    <strong>{{ pqcProcess.qaProcessName }}</strong>
                    <span v-if="pqcProcess.qaProcessCode">{{ pqcProcess.qaProcessCode }}</span>
                  </div>
                  <div class="team-leader-workbench__active-order-process-metrics">
                    <div>
                      <span>PQC提交</span>
                      <strong>{{ pqcProcess.submissions.length }}</strong>
                    </div>
                  </div>
                </div>
                <div
                  v-for="pqcSubmission in pqcProcess.submissions"
                  :key="pqcSubmission.pqcTaskId"
                  class="team-leader-workbench__active-order-pqc-card"
                >
                  <div class="team-leader-workbench__active-order-pqc-title">
                    <strong>
                      {{ resolvePqcInspectionTypeText(pqcSubmission.inspectionType) }}
                      <template v-if="pqcSubmission.roundNo"> / 第 {{ pqcSubmission.roundNo }} 轮</template>
                    </strong>
                    <span>实检 {{ pqcSubmission.actualInspectionQuantity ?? '-' }} 件</span>
                    <span v-if="formatActiveOrderPqcEventIds(pqcSubmission) !== '-'">
                      事件 {{ formatActiveOrderPqcEventIds(pqcSubmission) }}
                    </span>
                  </div>
                  <el-table
                    v-if="pqcSubmission.items?.length"
                    :data="buildActiveOrderPqcItemRows(pqcSubmission)"
                    size="small"
                    border
                    class="team-leader-workbench__active-order-submission-table"
                  >
                    <el-table-column label="检验项" min-width="180">
                      <template #default="{ row: item }">{{ item.itemNameText }}</template>
                    </el-table-column>
                    <el-table-column label="样本" min-width="160">
                      <template #default="{ row: item }">{{ item.sampleSummaryText }}</template>
                    </el-table-column>
                    <el-table-column label="结果汇总" min-width="180">
                      <template #default="{ row: item }">{{ item.resultSummaryText }}</template>
                    </el-table-column>
                    <el-table-column label="判定" min-width="100">
                      <template #default="{ row: item }">{{ item.judgementSummaryText }}</template>
                    </el-table-column>
                    <el-table-column label="设备" min-width="160">
                      <template #default="{ row: item }">{{ item.equipmentSummaryText }}</template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else :image-size="56" description="暂无PQC检验明细" />
                </div>
              </section>
            </el-tab-pane>
          </el-tabs>
          <el-empty v-else :image-size="56" description="暂无一线PQC提交" />
        </el-tab-pane>
        <el-tab-pane
          label="领料单"
          name="materials"
          data-team-leader-active-order-detail-material-tab
        >
          <el-table
            v-if="pickListMaterials.length"
            :data="pickListMaterials"
            size="small"
            border
            class="team-leader-workbench__active-order-submission-table"
          >
            <el-table-column label="领料单" min-width="180">
              <template #default="{ row: material }">
                {{ formatActiveOrderPickListNos(material.sourcePickListNos) }}
              </template>
            </el-table-column>
            <el-table-column label="物料编码" prop="materialCode" min-width="140" />
            <el-table-column label="物料名称" prop="materialName" min-width="180" />
            <el-table-column label="规格型号" min-width="160">
              <template #default="{ row: material }">
                {{ material.materialSpecification || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="批号" min-width="220">
              <template #default="{ row: material }">
                {{ formatActiveOrderBatchCodes(material.batchCodes) }}
              </template>
            </el-table-column>
            <el-table-column label="实发数量" min-width="120">
              <template #default="{ row: material }">
                {{ formatTraceQuantity(material.actualQuantity) }}
              </template>
            </el-table-column>
            <el-table-column label="对应工序" min-width="180">
              <template #default="{ row: material }">
                {{ material.sourceProcessNames.join('、') || '-' }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else :image-size="56" description="暂无领料单物料批号" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type {
  TeamLeaderActiveOrderDetailRespVO,
  TeamLeaderActiveOrderInputMaterialDetailRespVO,
  TeamLeaderActiveOrderPqcSubmissionDetailRespVO,
  TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO,
  TeamLeaderActiveOrderProcessDetailRespVO,
  TeamLeaderActiveOrderSubmissionDeviceDetailRespVO
} from '@/api/mes/pro/processpool/teamLeader'
import { formatDateTimeValue } from '@/utils/formatTime'

const props = defineProps<{
  detail?: TeamLeaderActiveOrderDetailRespVO
  loading?: boolean
  error?: string
  stage1SourceWorkOrderCode?: string
}>()

defineEmits<{
  retry: []
}>()

const activeTab = ref('')
const productionActiveTab = ref('')
const pqcActiveTab = ref('')

const formatDateTime = (value?: string | number | Date) => formatDateTimeValue(value)

const formatTraceQuantity = (value: number | string | undefined) => {
  if (value === undefined || value === null || value === '') return '-'
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(3) : String(value)
}

const formatActiveOrderBatchCodes = (batchCodes?: string[]) => {
  const normalized = (batchCodes ?? []).map((code) => String(code).trim()).filter(Boolean)
  return normalized.length ? normalized.join('、') : '-'
}

const formatActiveOrderPickListNos = (sourceNos?: string[]) => {
  const normalized = (sourceNos ?? []).map((sourceNo) => String(sourceNo).trim()).filter(Boolean)
  return normalized.length ? normalized.join('、') : '-'
}

const formatActiveOrderSubmissionDevices = (
  devices?: TeamLeaderActiveOrderSubmissionDeviceDetailRespVO[]
) => {
  const normalized = (devices ?? [])
    .map((device) =>
      [device.deviceName, device.deviceCode]
        .map((value) => String(value || '').trim())
        .filter(Boolean)
        .join(' / ')
    )
    .filter(Boolean)
  return normalized.length ? normalized.join('、') : '-'
}

const formatActiveOrderPqcEventIds = (
  pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO
) => {
  const ids = (pqcSubmission.submittedEventIds?.length
    ? pqcSubmission.submittedEventIds
    : pqcSubmission.submittedEventId
      ? [pqcSubmission.submittedEventId]
      : []
  )
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0)
    .sort((left, right) => left - right)
  const uniqueIds = Array.from(new Set(ids))
  return uniqueIds.length ? uniqueIds.join('、') : '-'
}

interface ActiveOrderPqcItemAggregateRow {
  key: string
  itemNameText: string
  sampleSummaryText: string
  resultSummaryText: string
  judgementSummaryText: string
  equipmentSummaryText: string
}

const normalizeActiveOrderPqcText = (value?: string | number | null) => {
  if (value === undefined || value === null) return ''
  return String(value).trim()
}

const formatActiveOrderPqcItemCountSummary = (values: string[]) => {
  const counts = new Map<string, number>()
  for (const value of values.map((item) => item.trim()).filter(Boolean)) {
    counts.set(value, (counts.get(value) ?? 0) + 1)
  }
  const parts = Array.from(counts.entries())
    .sort(([left], [right]) => left.localeCompare(right, 'zh-Hans-CN'))
    .map(([value, count]) => (count > 1 ? `${value} × ${count}` : value))
  return parts.length ? parts.join('；') : '-'
}

const formatActiveOrderPqcItemSampleSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const sampleNos = Array.from(
    new Set(
      items
        .map((item) => Number(item.sampleNo))
        .filter((sampleNo) => Number.isFinite(sampleNo) && sampleNo > 0)
    )
  ).sort((left, right) => left - right)
  if (!sampleNos.length) return `${items.length} 件`
  const visibleSamples = sampleNos.slice(0, 8).join('、')
  const suffix = sampleNos.length > 8 ? `…共 ${sampleNos.length} 件` : `共 ${sampleNos.length} 件`
  return `样本 ${visibleSamples}，${suffix}`
}

const formatActiveOrderPqcItemResultSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) =>
  formatActiveOrderPqcItemCountSummary(
    items.map((item) => normalizeActiveOrderPqcText(item.measuredValue || item.itemResult))
  )

const formatActiveOrderPqcItemJudgementSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => formatActiveOrderPqcItemCountSummary(items.map((item) => normalizeActiveOrderPqcText(item.judgement)))

const formatActiveOrderPqcEquipmentSummary = (
  items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
) => {
  const equipments = Array.from(
    new Set(
      items
        .map((item) =>
          normalizeActiveOrderPqcText(item.selectedEquipmentNumber || item.selectedEquipmentName)
        )
        .filter(Boolean)
    )
  )
  return equipments.length ? equipments.join('、') : '-'
}

const buildActiveOrderPqcItemRows = (
  pqcSubmission: TeamLeaderActiveOrderPqcSubmissionDetailRespVO
): ActiveOrderPqcItemAggregateRow[] => {
  const rowsByItem = new Map<
    string,
    {
      itemNameText: string
      items: TeamLeaderActiveOrderPqcSubmissionItemDetailRespVO[]
    }
  >()
  for (const item of pqcSubmission.items ?? []) {
    const itemCode = normalizeActiveOrderPqcText(item.itemCode)
    const itemName = normalizeActiveOrderPqcText(item.itemName)
    const key = itemName || itemCode || `aggregate-${item.aggregateDetailId}`
    const itemNameText = itemName || itemCode || '-'
    const existed = rowsByItem.get(key)
    if (existed) {
      existed.items.push(item)
      continue
    }
    rowsByItem.set(key, { itemNameText, items: [item] })
  }
  return Array.from(rowsByItem.entries()).map(([key, row]) => ({
    key,
    itemNameText: row.itemNameText,
    sampleSummaryText: formatActiveOrderPqcItemSampleSummary(row.items),
    resultSummaryText: formatActiveOrderPqcItemResultSummary(row.items),
    judgementSummaryText: formatActiveOrderPqcItemJudgementSummary(row.items),
    equipmentSummaryText: formatActiveOrderPqcEquipmentSummary(row.items)
  }))
}

const activeOrderDetailProcessTabName = (
  process: TeamLeaderActiveOrderProcessDetailRespVO,
  index: number
) => `process-${process.routeProcessId}-${process.processId}-${index}`

interface ActiveOrderDetailPqcProcessGroup {
  key: string
  qaProcessId: number
  qaProcessCode?: string
  qaProcessName: string
  submissions: TeamLeaderActiveOrderPqcSubmissionDetailRespVO[]
}

const resolveActiveOrderPqcProcessTabName = (
  pqcProcess: ActiveOrderDetailPqcProcessGroup,
  index: number
) => `pqc-process-${pqcProcess.qaProcessId}-${index}`

const pqcProcessGroups = computed<ActiveOrderDetailPqcProcessGroup[]>(() => {
  const detailResult = props.detail
  if (!detailResult?.processes?.length) return []
  const groupsByQaProcessId = new Map<number, ActiveOrderDetailPqcProcessGroup>()
  for (const process of detailResult.processes) {
    for (const submission of process.pqcSubmissions ?? []) {
      const qaProcessId = Number(submission.qaProcessId)
      if (!Number.isFinite(qaProcessId) || qaProcessId <= 0 || !submission.qaProcessName?.trim()) {
        throw new Error('PQC提交缺少正式检验工序身份，无法按PQC工序展示')
      }
      const existed = groupsByQaProcessId.get(qaProcessId)
      if (existed) {
        existed.submissions.push(submission)
        continue
      }
      groupsByQaProcessId.set(qaProcessId, {
        key: `pqc-process-${qaProcessId}`,
        qaProcessId,
        qaProcessCode: submission.qaProcessCode,
        qaProcessName: submission.qaProcessName.trim(),
        submissions: [submission]
      })
    }
  }
  return Array.from(groupsByQaProcessId.values())
})

type ActiveOrderDetailPickListMaterialRow = TeamLeaderActiveOrderInputMaterialDetailRespVO & {
  sourceProcessNames: string[]
}

const pickListMaterials = computed<ActiveOrderDetailPickListMaterialRow[]>(() => {
  const detailResult = props.detail
  if (!detailResult?.processes?.length) return []
  const rowsByKey = new Map<string, ActiveOrderDetailPickListMaterialRow>()
  for (const process of detailResult.processes) {
    for (const material of process.inputMaterials ?? []) {
      const sourcePickListNos = (material.sourcePickListNos ?? [])
        .map((sourceNo) => String(sourceNo).trim())
        .filter(Boolean)
        .sort()
      const batchCodes = (material.batchCodes ?? [])
        .map((batchCode) => String(batchCode).trim())
        .filter(Boolean)
        .sort()
      const rowKey = [
        sourcePickListNos.join('|'),
        material.materialCode,
        batchCodes.join('|'),
        material.actualQuantity ?? '',
        material.baseActualQuantity ?? ''
      ].join('::')
      const existed = rowsByKey.get(rowKey)
      if (existed) {
        if (!existed.sourceProcessNames.includes(process.processName)) {
          existed.sourceProcessNames.push(process.processName)
        }
        continue
      }
      rowsByKey.set(rowKey, {
        ...material,
        sourcePickListNos,
        batchCodes,
        sourceProcessNames: [process.processName]
      })
    }
  }
  return Array.from(rowsByKey.values())
})

const resolveActiveOrderSubmissionRowClassName = ({
  row
}: {
  row: { quantityConflict?: boolean }
}) => (row.quantityConflict ? 'team-leader-workbench__quantity-conflict-row' : '')

const resolvePqcInspectionTypeText = (inspectionType?: string) => {
  if (inspectionType === 'FIRST') return '首检'
  if (inspectionType === 'FINAL') return '末检'
  if (inspectionType === 'PROCESS') return '巡检'
  return inspectionType || '检验'
}

const resetTabs = async () => {
  await nextTick()
  const firstProcess = props.detail?.processes?.[0]
  activeTab.value = firstProcess ? 'productionSubmissions' : 'materials'
  productionActiveTab.value = firstProcess ? activeOrderDetailProcessTabName(firstProcess, 0) : ''
  const firstPqcProcess = pqcProcessGroups.value[0]
  pqcActiveTab.value = firstPqcProcess
    ? resolveActiveOrderPqcProcessTabName(firstPqcProcess, 0)
    : ''
}

watch(
  () => props.detail,
  () => {
    void resetTabs()
  },
  { immediate: true }
)
</script>
