<template>
  <el-drawer
    :model-value="visible"
    title="反查关联批次"
    size="min(100vw, 960px)"
    destroy-on-close
    data-edhr-reverse-trace-panel
    @close="close"
    @opened="restoreScroll"
    @scroll.capture="rememberScroll"
  >
    <div ref="panelBody" v-loading="loadingCatalog || querying" class="batch-reverse-trace-panel">
      <el-alert v-if="catalogError" :title="catalogError" type="error" :closable="false" show-icon />
      <el-button v-if="catalogError || (!catalog && !loadingCatalog)" @click="loadCatalog()">重新加载目录</el-button>
      <el-alert v-if="blockedReason" :title="blockedReason" type="warning" :closable="false" show-icon />

      <div class="batch-reverse-trace-panel__anchor">
        <span>锚点批次执行编号</span>
        <el-tag>{{ anchorBatchExecutionId }}</el-tag>
      </div>

      <div class="batch-reverse-trace-panel__scope">
        <span class="batch-reverse-trace-panel__section-title">查询范围</span>
        <el-date-picker
          v-model="releaseApprovedTime"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始放行时间"
          end-placeholder="结束放行时间"
          data-edhr-reverse-trace-release-scope
        />
      </div>

      <div>
        <div class="batch-reverse-trace-panel__section-title">选择当前批次已保存信息</div>
        <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
          <el-tab-pane
            v-for="category in categories"
            :key="category"
            :label="categoryLabels[category]"
            :name="category"
          >
            <el-alert
              v-if="categoryStatus(category) && categoryStatus(category)?.status !== 'AVAILABLE'"
              :title="categoryStatus(category)?.reason || '该类别来源暂不可用'"
              :type="categoryStatus(category)?.status === 'BLOCKED' ? 'warning' : 'info'"
              :closable="false"
              show-icon
            />
            <div v-if="categoryItems(category).length" class="batch-reverse-trace-panel__items">
              <el-button
                v-for="(item, itemIndex) in categoryItems(category)"
                :key="`${item.evidenceKey}:${item.sourceRef || itemIndex}`"
                text
                class="batch-reverse-trace-panel__catalog-item"
                :data-edhr-reverse-trace-catalog-item="item.evidenceKey"
                :disabled="conditions.length >= 10 || loadingCatalog"
                @click="addConditionFromItem(item)"
              >
                <span>{{ item.label || item.evidenceKey }}<small class="batch-reverse-trace-panel__context">{{ itemContext(item) }}</small></span>
                <span class="batch-reverse-trace-panel__catalog-value">
                  {{ item.savedValue ?? '已记录' }}{{ item.unit ? ' ' + item.unit : '' }}
                </span>
              </el-button>
            </div>
            <el-empty v-else description="当前批次暂无可用记录" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <div>
        <div class="batch-reverse-trace-panel__conditions-header">
          <span class="batch-reverse-trace-panel__section-title">查询条件</span>
          <el-button
            type="primary"
            link
            :disabled="conditions.length >= 10"
            data-edhr-reverse-trace-add-condition
            @click="addCondition"
          >
            添加条件
          </el-button>
        </div>

        <el-empty v-if="!conditions.length" description="请从上方目录选择一条记录" :image-size="70" />

        <div
          v-for="(condition, index) in conditions"
          :key="condition.conditionId"
          class="batch-reverse-trace-panel__condition"
          :data-edhr-reverse-trace-condition="index"
        >
          <el-tag>{{ categoryLabels[condition.category] }}</el-tag>
          <div class="batch-reverse-trace-panel__condition-main">
            <div class="batch-reverse-trace-panel__condition-label">
              {{ condition.item.label || condition.item.evidenceKey }}
              <small class="batch-reverse-trace-panel__context">{{ itemContext(condition.item) }}</small>
              <small v-if="conditionErrors[index]" role="alert" class="batch-reverse-trace-panel__context">{{ conditionErrors[index] }}</small>
            </div>
            <el-select v-model="condition.operator" class="!w-120px" data-edhr-reverse-trace-operator>
              <el-option
                v-for="operator in condition.item.allowedOperators"
                :key="operator"
                :label="operatorLabel(operator)"
                :value="operator"
              />
            </el-select>
            <el-select
              v-if="condition.operator !== 'OUT_OF_LIMIT' && condition.item.allowedValues?.length"
              v-model="condition.value"
              class="batch-reverse-trace-panel__condition-value"
              data-edhr-reverse-trace-value
            >
              <el-option v-for="value in condition.item.allowedValues" :key="value" :label="value" :value="value" />
            </el-select>
            <el-input
              v-else-if="condition.operator !== 'OUT_OF_LIMIT'"
              v-model="condition.value"
              class="batch-reverse-trace-panel__condition-value"
              :placeholder="conditionPlaceholder(condition)"
              data-edhr-reverse-trace-value
            />
          </div>
          <el-button link type="danger" data-edhr-reverse-trace-remove-condition @click="removeCondition(index)">
            删除
          </el-button>
        </div>
      </div>

      <div class="batch-reverse-trace-panel__logic">
        <span>组合方式</span>
        <el-tag type="info">同时满足全部条件</el-tag>
      </div>

      <el-alert
        v-if="dirty"
        title="条件已修改，请重新查询。"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="batch-reverse-trace-panel__actions">
        <el-button data-edhr-reverse-trace-reset @click="resetConditions">重置</el-button>
        <el-button type="primary" :disabled="!conditions.length || querying || loadingCatalog || !catalog?.catalogVersion || !!catalogError || conditionErrors.some(Boolean) || !authorized" data-edhr-reverse-trace-query @click="runQuery()">
          查询
        </el-button>
      </div>

      <div v-if="queryResponse" class="batch-reverse-trace-panel__results" data-edhr-reverse-trace-results>
        <div class="batch-reverse-trace-panel__results-header">
          <span class="batch-reverse-trace-panel__section-title">查询结果</span>
          <el-tag :type="statusTagType(queryResponse.queryStatus)">
            {{ statusLabel(queryResponse.queryStatus) }}
          </el-tag>
        </div>
        <el-alert
          v-if="queryResponse.reason"
          :title="queryResponse.reason"
          :type="queryResponse.queryStatus === 'BLOCKED' ? 'warning' : 'info'"
          :closable="false"
          show-icon
        />
        <el-table v-if="queryResponse.list?.length" :data="queryResponse.list" border stripe data-edhr-reverse-trace-result-table>
          <el-table-column prop="batchExecutionCode" label="批次执行" min-width="150" />
          <el-table-column prop="workOrderCode" label="工单号" min-width="130" />
          <el-table-column prop="productName" label="产品" min-width="120" show-overflow-tooltip />
          <el-table-column label="命中条件" min-width="90">
            <template #default="{ row }">{{ row.matchCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTargetDetail(row.batchExecutionId)">详情</el-button>
              <el-button link type="primary" @click="loadEvidence(row.batchExecutionId)">命中记录</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-else-if="queryResponse.queryStatus === 'NO_MATCH'"
          description="所选范围内未找到匹配批次"
          :image-size="70"
        />
        <el-pagination
          v-if="queryResponse.coverageStatus === 'COMPLETE' && queryResponse.total != null"
          :current-page="resultPage" :page-size="resultPageSize" :total="queryResponse.total"
          :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
          :disabled="querying" aria-label="反查批次分页"
          @current-change="changeResultPage" @size-change="changeResultPageSize"
        />
      </div>

      <el-collapse v-if="evidenceResponse || loadingEvidence" v-model="evidenceOpen" v-loading="loadingEvidence" class="batch-reverse-trace-panel__evidence">
        <el-collapse-item name="evidence" title="命中记录">
          <template v-if="evidenceResponse">
          <el-alert
            v-if="evidenceResponse.reason"
            :title="evidenceResponse.reason"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-descriptions v-if="evidenceResponse.chainContext" :column="2" border size="small">
            <el-descriptions-item label="活跃订单">{{ evidenceResponse.chainContext.activeOrderId || '--' }}</el-descriptions-item>
            <el-descriptions-item label="工单">{{ evidenceResponse.chainContext.workOrderId || '--' }}</el-descriptions-item>
            <el-descriptions-item label="完工回执">{{ evidenceResponse.chainContext.completionReceiptId || '--' }}</el-descriptions-item>
            <el-descriptions-item label="PQC申请">{{ evidenceResponse.chainContext.releaseApplicationId || '--' }}</el-descriptions-item>
            <el-descriptions-item label="上市放行事务">{{ evidenceResponse.chainContext.releaseTransactionId || '--' }}</el-descriptions-item>
            <el-descriptions-item label="来源状态">{{ evidenceResponse.chainContext.sourceStatus || '--' }}</el-descriptions-item>
          </el-descriptions>
          <el-table v-if="evidenceResponse.items?.length" :data="evidenceResponse.items" border>
            <el-table-column prop="conditionId" label="条件" width="80" />
            <el-table-column prop="sourceStage" label="来源环节" width="110" />
            <el-table-column prop="sourceAction" label="来源动作" width="150" />
            <el-table-column prop="sourceIdentity" label="来源身份" min-width="150" show-overflow-tooltip />
            <el-table-column prop="sourceRef" label="来源引用" min-width="180" show-overflow-tooltip />
            <el-table-column label="记录上下文" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ formatEvidenceContext(row.sourceContext) || '--' }}</template>
            </el-table-column>
            <el-table-column prop="actualValue" label="实际值" min-width="120" />
            <el-table-column prop="recordedStandard" label="当时标准" min-width="150" show-overflow-tooltip />
            <el-table-column prop="recordStatus" label="记录状态" width="100" />
            <el-table-column prop="occurredAt" label="发生时间" width="170" />
            <el-table-column prop="recordedAt" label="记录时间" width="170" />
          </el-table>
          <el-empty v-else description="没有可展示的命中凭证" :image-size="60" />
          <el-pagination
            v-if="evidenceResponse.evidenceStatus === 'MATCHED' && evidenceResponse.total != null"
            :current-page="evidencePage" :page-size="evidencePageSize" :total="evidenceResponse.total"
            :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
            :disabled="loadingEvidence" aria-label="命中记录分页"
            @current-change="changeEvidencePage" @size-change="changeEvidencePageSize"
          />
          </template>
        </el-collapse-item>
      </el-collapse>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import type { EdhrBatchExecutionPageReqVO } from '@/api/mes/pro/edhr/batchExecution'
import {
  canReverseTrace, reverseTraceIdentity, copyReverseTraceState, revokeReverseTraceSnapshot,
  readReverseTraceState, saveReverseTraceState, reverseTraceHistoryPath, reverseTraceDetailPath,
  type ReverseTraceConditionRow as ConditionRow, type ReverseTraceSavedState
} from '../reverseTraceState'

import {
  getReverseTraceCatalog,
  getReverseTraceEvidence,
  queryReverseTrace,
  type ReverseTraceCatalogItem,
  type ReverseTraceCategory,
  type ReverseTraceCondition,
  type ReverseTraceEvidenceResponse,
  type ReverseTraceQueryResponse,
  type ReverseTraceQueryRequest,
  type ReverseTraceTargetScope
} from '@/api/mes/pro/edhr/reverseTrace'

const props = defineProps<{
  visible: boolean
  anchorBatchExecutionId: string
  restoreKey?: string
  historyListState?: EdhrBatchExecutionPageReqVO
}>()
const emit = defineEmits<{ (event: 'update:visible', visible: boolean): void }>()
const router = useRouter()
const route = useRoute()
const authorized = computed(canReverseTrace)

const categories: ReverseTraceCategory[] = ['FIELD', 'PARAMETER', 'EQUIPMENT', 'PERSON', 'INSPECTION', 'MATERIAL']
const categoryLabels: Record<ReverseTraceCategory, string> = {
  FIELD: '批记录字段',
  PARAMETER: '工艺参数',
  EQUIPMENT: '设备',
  PERSON: '人员',
  INSPECTION: '检验结果',
  MATERIAL: '物料批次'
}

const loadingCatalog = ref(false)
const querying = ref(false)
const loadingEvidence = ref(false)
const catalogError = ref('')
const blockedReason = ref('')
const activeCategory = ref<ReverseTraceCategory>('FIELD')
const catalog = ref<Awaited<ReturnType<typeof getReverseTraceCatalog>> | undefined>()
const conditions = ref<ConditionRow[]>([])
const releaseApprovedTime = ref<string[] | undefined>()
const queryResponse = ref<ReverseTraceQueryResponse>()
const evidenceResponse = ref<ReverseTraceEvidenceResponse>()
const evidenceOpen = ref<string[]>([])
const dirty = ref(false)
const requestSequence = ref(0)
const conditionSequence = ref(0)
const resultPage = ref(1)
const resultPageSize = ref(20)
const evidencePage = ref(1)
const evidencePageSize = ref(100)
const evidenceTarget = ref('')
const successfulQuery = ref<ReverseTraceQueryRequest>()
const panelBody = ref<HTMLElement>()
const scrollTop = ref(0)
let querySequence = 0
let evidenceSequence = 0
let settingConditions = false
let rememberedKey: string | undefined
const drawerBody = () => panelBody.value?.closest<HTMLElement>('.el-drawer__body')
const rememberScroll = () => { scrollTop.value = drawerBody()?.scrollTop ?? 0 }
const restoreScroll = () => { const body = drawerBody(); if (body) body.scrollTop = scrollTop.value }

const targetScope = computed<ReverseTraceTargetScope>(() => ({
  kind: 'RELEASED_HISTORY',
  releaseApprovedFrom: releaseApprovedTime.value?.[0],
  releaseApprovedTo: releaseApprovedTime.value?.[1]
}))

const categoryItems = (category: ReverseTraceCategory) =>
  (catalog.value?.items || []).filter((item) => item.category === category)

const categoryStatus = (category: ReverseTraceCategory) =>
  catalog.value?.categories?.find((item) => item.category === category)

const operatorLabel = (operator: string) =>
  ({ EQ: '等于', NE: '不等于', GT: '大于', GE: '大于等于', LT: '小于', LE: '小于等于', BETWEEN: '范围内', OUT_OF_LIMIT: '超出当时标准', JUDGEMENT_EQ: '判定等于' })[operator] || operator

const itemContext = (item: ReverseTraceCatalogItem) => {
  const identity = item.qualifiers || {}
  return [item.sourceView === 'FIELD_CHANGE' ? '字段修改记录' : item.category === 'FIELD' ? '已保存记录' : '',
    identity.templateVersionNo && `表单版本 ${identity.templateVersionNo}`,
    identity.templateVersionId && `表单版本 ${identity.templateVersionId}`,
    identity.regulationVersionId && `规程版本 ${identity.regulationVersionId}`,
    identity.sampleNo && `样本 ${identity.sampleNo}`, item.recordedStandard,
    item.parameterStatus && `记录状态 ${item.parameterStatus}`].filter(Boolean).join(' · ')
}

const formatEvidenceContext = (context?: Record<string, string>) => {
  const labels: Record<string, string> = {
    equipmentNamespace: '设备类型',
    deviceId: '设备ID',
    deviceCode: '设备编码',
    materialId: '物料',
    regulationVersionId: '规程版本',
    routeProcessId: '工序',
    sampleNo: '样本',
    itemCode: '检验项目',
    selectedEquipmentId: '设备ID',
    selectedEquipmentCode: '设备编码',
    selectedEquipmentNumber: '设备编号'
  }
  return Object.entries(context || {})
    .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== '')
    .map(([key, value]) => `${labels[key] || key}：${value}`)
    .join(' · ')
}

const statusLabel = (status?: string) =>
  ({ MATCHED: '已命中', NO_MATCH: '无命中', BLOCKED: '已阻断' })[status || ''] || status || '未知'

const statusTagType = (status?: string) => (status === 'MATCHED' ? 'success' : status === 'NO_MATCH' ? 'info' : 'warning')

const conditionPlaceholder = (condition: ConditionRow) =>
  condition.operator === 'BETWEEN' ? '输入范围，例如 10,20' : '输入比较值'

const hasConditionValue = (condition: ConditionRow) =>
  condition.operator === 'OUT_OF_LIMIT'
    || (condition.value !== null && condition.value !== undefined && String(condition.value).trim() !== '')

const validateConditions = () => {
  if (conditionErrors.value.some(Boolean)) {
    blockedReason.value = conditionErrors.value.filter(Boolean).join('；')
    return false
  }
  const invalid = conditions.value.find((condition) => !hasConditionValue(condition))
  if (!invalid) return true
  blockedReason.value = '除超出当时标准外的查询条件必须填写条件值。'
  return false
}

const toCondition = (row: ConditionRow): ReverseTraceCondition => {
  const isEquipment = row.category === 'EQUIPMENT' || row.item.category === 'EQUIPMENT'
  const qualifiers = isEquipment
    ? undefined
    : Object.fromEntries(Object.entries(row.item.qualifiers || {}).filter(([key]) =>
      row.category !== 'PARAMETER' && row.item.category !== 'PARAMETER' || !['parameterStatus', 'lowerLimit', 'upperLimit'].includes(key)))
  return {
    conditionId: row.conditionId,
    evidenceKey: row.item.evidenceKey,
    sourceView: row.item.sourceView,
    operator: row.operator,
    value: row.operator === 'OUT_OF_LIMIT' ? null : row.value,
    ...(qualifiers && Object.keys(qualifiers).length ? { qualifiers } : {})
  }
}

const clearResults = () => {
  queryResponse.value = undefined
  evidenceResponse.value = undefined
  evidenceOpen.value = []
  successfulQuery.value = undefined
  evidenceTarget.value = ''
  resultPage.value = 1
  evidencePage.value = 1
}

let conditionRevision = 0
const invalidateResults = () => {
  querySequence++
  evidenceSequence++
  querying.value = false
  loadingEvidence.value = false
  clearResults()
}
const invalidate = () => {
  requestSequence.value++
  loadingCatalog.value = false
  invalidateResults()
}
const current = (sequence: number, owner: string) => sequence === requestSequence.value && !!owner && owner === reverseTraceIdentity()

const saveState = () => {
  if (!reverseTraceIdentity()) return undefined
  return saveReverseTraceState({
    anchorBatchExecutionId: props.anchorBatchExecutionId,
    catalogVersion: catalog.value?.catalogVersion || '',
    releaseApprovedTime: releaseApprovedTime.value,
    activeCategory: activeCategory.value,
    conditions: conditions.value,
    queryResponse: queryResponse.value,
    evidenceResponse: evidenceResponse.value,
    evidenceOpen: evidenceOpen.value,
    evidenceTarget: evidenceTarget.value,
    successfulQuery: successfulQuery.value,
    resultPage: resultPage.value,
    resultPageSize: resultPageSize.value,
    evidencePage: evidencePage.value,
    evidencePageSize: evidencePageSize.value,
    scrollTop: scrollTop.value,
    dirty: dirty.value,
    origin: { path: route.path, query: route.query },
    historyListState: props.historyListState
  })
}

const restoreState = async (saved: ReverseTraceSavedState) => {
  if (saved.anchorBatchExecutionId !== props.anchorBatchExecutionId) return false
  settingConditions = true
  conditions.value = saved.conditions
  revalidateConditions()
  settingConditions = false
  conditionSequence.value = saved.conditions.reduce((max, condition) => {
    const numericId = Number(condition.conditionId.replace(/^C/, ''))
    return Number.isFinite(numericId) ? Math.max(max, numericId) : max
  }, conditionSequence.value)
  activeCategory.value = saved.activeCategory
  if (!saved.catalogVersion || saved.catalogVersion !== catalog.value?.catalogVersion || conditionErrors.value.some(Boolean)) {
    dirty.value = true
    blockedReason.value = conditionErrors.value.filter(Boolean).join('；') || '原反查结果已失效，请重新查询。'
    return true
  }
  queryResponse.value = saved.queryResponse
  evidenceResponse.value = saved.evidenceResponse
  evidenceOpen.value = saved.evidenceOpen
  dirty.value = saved.dirty
  if (saved.dirty && !saved.queryResponse) blockedReason.value = '原反查结果已失效，请重新查询。'
  successfulQuery.value = saved.successfulQuery
  evidenceTarget.value = saved.evidenceTarget
  resultPage.value = saved.resultPage
  resultPageSize.value = saved.resultPageSize
  evidencePage.value = saved.evidencePage
  evidencePageSize.value = saved.evidencePageSize
  scrollTop.value = saved.scrollTop
  await nextTick()
  restoreScroll()
  return true
}

const itemKey = (item: ReverseTraceCatalogItem) => [item.category, item.sourceView, item.evidenceKey, item.sourceRef || ''].join('|')

const sameConditionIdentity = (row: ConditionRow, item: ReverseTraceCatalogItem) => {
  const qualifiers = (candidate: ReverseTraceCatalogItem) => Object.entries(toCondition({ ...row, item: candidate }).qualifiers || {})
    .sort(([a], [b]) => a.localeCompare(b))
  const old = qualifiers(row.item), fresh = qualifiers(item)
  return row.category === item.category && itemKey(row.item) === itemKey(item)
    && row.item.semanticIdentity === item.semanticIdentity && row.item.valueType === item.valueType
    && row.item.unit === item.unit && old.length === fresh.length
    && old.every(([key, value], index) => key === fresh[index][0] && value === fresh[index][1])
}
const currentConditionItem = (row: ConditionRow) => catalog.value?.items.find(item => sameConditionIdentity(row, item))
const conditionErrors = computed(() => conditions.value.map(row => {
  const item = currentConditionItem(row)
  if (!catalog.value?.catalogVersion) return `条件 ${row.conditionId} 等待目录校验，请重新加载目录。`
  if (categoryStatus(row.category)?.status !== 'AVAILABLE' || !item) return `条件 ${row.conditionId} 的正式来源已失效，请删除后重新选择。`
  if (!item.allowedOperators?.includes(row.operator)) return `条件 ${row.conditionId} 的比较方式已失效，请修改或删除。`
  if (row.operator !== 'OUT_OF_LIMIT' && item.allowedValues?.length && !item.allowedValues.includes(row.value)) return `条件 ${row.conditionId} 的比较值已失效，请修改或删除。`
  return ''
}))
const revalidateConditions = () => {
  const previous = settingConditions
  settingConditions = true
  conditions.value = conditions.value.map(row => ({ ...row, item: currentConditionItem(row) || row.item }))
  settingConditions = previous
}

const loadCatalog = async (saved?: ReverseTraceSavedState, preserve = true) => {
  if (!props.anchorBatchExecutionId) return
  invalidate()
  const sequence = ++requestSequence.value
  settingConditions = true
  if (saved?.anchorBatchExecutionId === props.anchorBatchExecutionId) conditions.value = copyReverseTraceState(saved.conditions)
  else if (!preserve) conditions.value = []
  settingConditions = false
  const draftRevision = conditionRevision
  const owner = reverseTraceIdentity()
  if (!owner) { blockedReason.value = '当前身份或权限不可用，请重新查询。'; catalog.value = undefined; return }
  const anchor = props.anchorBatchExecutionId
  const scope = copyReverseTraceState(targetScope.value)
  loadingCatalog.value = true
  querying.value = false
  catalogError.value = ''
  blockedReason.value = ''
  catalog.value = undefined
  clearResults()
  dirty.value = preserve || !!saved
  try {
    let pageNo = 1
    let firstPage: Awaited<ReturnType<typeof getReverseTraceCatalog>> | undefined
    const items: ReverseTraceCatalogItem[] = []
    const seen = new Set<string>()
    do {
      const page = await getReverseTraceCatalog({
        anchorBatchExecutionId: anchor,
        targetScope: scope,
        pageNo,
        pageSize: 100
      })
      if (!current(sequence, owner)) return
      if (page.anchorBatchExecutionId !== anchor || !Array.isArray(page.items) || !Array.isArray(page.categories)) {
        throw new Error('反查目录缺少完整身份或类别信息。')
      }
      if (page.total == null && page.items.length === 0 && page.categories.length === categories.length
        && categories.every((category) => page.categories.some((status) => status.category === category
          && status.status === 'BLOCKED' && status.reasonCode?.trim() && status.reason?.trim()))) {
        catalog.value = page
        blockedReason.value = conditionErrors.value.filter(Boolean).join('；')
        return
      }
      if (!page.catalogVersion || !Number.isSafeInteger(page.total) || page.total! < 0) {
        throw new Error('反查目录缺少完整版本、身份或总数。')
      }
      if (firstPage && (firstPage.catalogVersion !== page.catalogVersion || firstPage.total !== page.total)) {
        throw new Error('反查目录版本或总数已变化，请重新加载。')
      }
      firstPage ||= page
      if (page.items.length !== Math.min(100, page.total! - items.length)) throw new Error('反查目录分页不完整，请重新加载。')
      for (const item of page.items) {
        const key = itemKey(item)
        if (seen.has(key)) throw new Error('反查目录分页包含重复记录，请重新加载。')
        seen.add(key)
      }
      items.push(...page.items)
      if (items.length === page.total) break
      pageNo += 1
    } while (true)
    if (!firstPage) return
    catalog.value = { ...firstPage, items }
    if (saved?.anchorBatchExecutionId === anchor && draftRevision === conditionRevision) {
      await restoreState(saved)
    } else if (preserve || draftRevision !== conditionRevision) {
      revalidateConditions()
      blockedReason.value = conditionErrors.value.filter(Boolean).join('；')
    } else {
      settingConditions = true
      const firstAvailable = items[0]
      if (firstAvailable) addConditionFromItem(firstAvailable)
      settingConditions = false
      dirty.value = false
      if (saved || props.restoreKey) blockedReason.value = '原反查状态已失效或目录发生变化，请重新查询。'
    }
  } catch (errorValue) {
    if (!current(sequence, owner)) return
    catalog.value = undefined
    catalogError.value = errorValue instanceof Error ? errorValue.message : '反查目录加载失败，请检查接口和权限。'
  } finally {
    if (sequence === requestSequence.value) loadingCatalog.value = false
  }
}

const addConditionFromItem = (item: ReverseTraceCatalogItem) => {
  if (conditions.value.length >= 10) { ElMessage.warning('最多添加10条查询条件。'); return }
  if (!item.allowedOperators?.length) { ElMessage.warning('该记录缺少正式比较方式，请重新加载目录。'); return }
  if (conditions.value.some((condition) => condition.item.evidenceKey === item.evidenceKey
    && condition.item.sourceRef === item.sourceRef)) return
  conditionSequence.value += 1
  conditions.value.push({
    conditionId: 'C' + conditionSequence.value,
    category: item.category,
    item,
    operator: item.allowedOperators[0],
    value: item.savedValue ?? ''
  })
  activeCategory.value = item.category
  dirty.value = true
  clearResults()
}

const addCondition = () => {
  const item = categoryItems(activeCategory.value)[0]
  if (!item) {
    ElMessage.warning('当前类别暂无可用记录。')
    return
  }
  addConditionFromItem(item)
}

const removeCondition = (index: number) => {
  conditions.value.splice(index, 1)
  dirty.value = true
  clearResults()
}

const resetConditions = () => {
  conditions.value = []
  clearResults()
  dirty.value = false
  const firstAvailable = catalog.value?.items?.[0]
  if (firstAvailable) addConditionFromItem(firstAvailable)
  dirty.value = false
}

const runQuery = async (page = 1, reuse = false) => {
  if (loadingCatalog.value || catalogError.value || !catalog.value?.catalogVersion || !conditions.value.length || !reverseTraceIdentity()) return
  if (!validateConditions()) return
  const snapshot = successfulQuery.value
  const previousHash = queryResponse.value?.queryHash
  if (reuse && (!snapshot || !previousHash || dirty.value)) return
  const request = copyReverseTraceState(reuse ? { ...snapshot!, pageNo: page, pageSize: resultPageSize.value } : {
    anchorBatchExecutionId: props.anchorBatchExecutionId,
    catalogVersion: catalog.value.catalogVersion,
    targetScope: targetScope.value,
    logic: 'AND' as const,
    conditions: conditions.value.map(toCondition),
    pageNo: 1,
    pageSize: resultPageSize.value
  })
  // A fresh query owns a new generation; evidence from the previous query cannot revoke it.
  if (!reuse) requestSequence.value++
  const sequence = requestSequence.value
  const task = ++querySequence
  const owner = reverseTraceIdentity()
  evidenceSequence++
  loadingEvidence.value = false
  evidenceResponse.value = undefined
  evidenceOpen.value = []
  evidenceTarget.value = ''
  evidencePage.value = 1
  if (!reuse) { queryResponse.value = undefined; successfulQuery.value = undefined }
  querying.value = true
  blockedReason.value = ''
  try {
    const response = await queryReverseTrace(request)
    if (!current(sequence, owner) || task !== querySequence) return
    if (response.queryStatus !== 'BLOCKED') {
      if (response.coverageStatus !== 'COMPLETE' || !response.normalizedQuery || !response.queryHash
        || !Number.isSafeInteger(response.total) || response.total! < 0
        || response.catalogVersion !== request.catalogVersion
        || response.normalizedQuery.anchorBatchExecutionId !== request.anchorBatchExecutionId
        || (reuse && response.queryHash !== previousHash)) throw new Error('查询来源或结果快照已变化，请重新查询。')
      successfulQuery.value = copyReverseTraceState(response.normalizedQuery)
    } else {
      clearResults()
    }
    queryResponse.value = copyReverseTraceState(response)
    resultPage.value = request.pageNo
    dirty.value = false
    if (queryResponse.value.reason) blockedReason.value = queryResponse.value.reason
  } catch (errorValue) {
    if (!current(sequence, owner) || task !== querySequence) return
    clearResults()
    blockedReason.value = errorValue instanceof Error ? errorValue.message : '反查查询失败，请重试。'
  } finally {
    if (sequence === requestSequence.value && task === querySequence) querying.value = false
  }
}

const changeResultPage = (page: number) => runQuery(page, true)
const changeResultPageSize = (size: number) => { resultPageSize.value = size; return runQuery(1, true) }

const loadEvidence = async (targetBatchExecutionId: string, page = 1) => {
  if (!queryResponse.value?.queryHash || !successfulQuery.value || dirty.value || querying.value || !reverseTraceIdentity()) return
  const sequence = requestSequence.value
  const task = ++evidenceSequence
  const owner = reverseTraceIdentity()
  const request = copyReverseTraceState({ ...successfulQuery.value,
    queryHash: queryResponse.value.queryHash, targetBatchExecutionId,
    pageNo: page, pageSize: evidencePageSize.value })
  evidenceResponse.value = undefined
  evidenceTarget.value = targetBatchExecutionId
  evidenceOpen.value = ['evidence']
  loadingEvidence.value = true
  blockedReason.value = ''
  try {
    const response = await getReverseTraceEvidence(request)
    if (!current(sequence, owner)) return
    const snapshotInvalid = response.queryHash !== request.queryHash
      || response.catalogVersion !== request.catalogVersion || response.targetBatchExecutionId !== targetBatchExecutionId
      || !Number.isSafeInteger(response.total) || response.total! < 0
    // A formal rejection invalidates the shared snapshot even if a newer page request is pending.
    if (response.evidenceStatus === 'BLOCKED' || snapshotInvalid) {
      revokeReverseTraceSnapshot(request, request.queryHash)
      rememberedKey = undefined
      invalidate()
      dirty.value = true
      blockedReason.value = `${response.evidenceStatus === 'BLOCKED' && response.reason
        ? response.reason : '命中记录来源或快照已失效'}，请重新查询。`
      return
    }
    if (task !== evidenceSequence) return
    evidenceResponse.value = copyReverseTraceState(response)
    evidencePage.value = page
  } catch (errorValue) {
    if (!current(sequence, owner) || task !== evidenceSequence) return
    evidenceResponse.value = undefined
    blockedReason.value = errorValue instanceof Error ? errorValue.message : '命中记录加载失败，请重试。'
  } finally {
    if (sequence === requestSequence.value && task === evidenceSequence) loadingEvidence.value = false
  }
}
const changeEvidencePage = (page: number) => loadEvidence(evidenceTarget.value, page)
const changeEvidencePageSize = (size: number) => { evidencePageSize.value = size; return changeEvidencePage(1) }

const openTargetDetail = async (targetBatchExecutionId: string) => {
  if (dirty.value || !successfulQuery.value || !queryResponse.value?.list.some((row) => row.batchExecutionId === targetBatchExecutionId)) return
  const key = saveState()
  if (!key) return
  invalidate()
  emit('update:visible', false)
  await router.push({
    path: reverseTraceDetailPath,
    query: {
      batchExecutionId: String(targetBatchExecutionId),
      from: reverseTraceHistoryPath,
      reverseTraceReturn: key
    }
  })
}

const handleCategoryChange = () => undefined
const close = () => {
  rememberedKey = saveState()
  invalidate()
  emit('update:visible', false)
}

watch(conditions, () => {
  if (settingConditions) return
  conditionRevision++
  if (loadingCatalog.value) invalidateResults()
  else invalidate()
  dirty.value = true
}, { deep: true, flush: 'sync' })

watch(() => [props.visible, props.anchorBatchExecutionId, props.restoreKey, reverseTraceIdentity()], (values, previous) => {
  if (previous && (values[1] !== previous[1] || values[2] !== previous[2] || values[3] !== previous[3])) rememberedKey = undefined
  invalidate()
  if (!props.visible) return
  const saved = readReverseTraceState(rememberedKey || props.restoreKey)
  settingConditions = true
  releaseApprovedTime.value = saved?.anchorBatchExecutionId === props.anchorBatchExecutionId ? saved.releaseApprovedTime : undefined
  settingConditions = false
  void loadCatalog(saved, false)
}, { immediate: true })

watch(releaseApprovedTime, () => {
  if (settingConditions) return
  if (successfulQuery.value && queryResponse.value?.queryHash) revokeReverseTraceSnapshot(successfulQuery.value, queryResponse.value.queryHash)
  rememberedKey = undefined
  if (props.visible) void loadCatalog()
}, { deep: true, flush: 'sync' })

onBeforeUnmount(invalidate)
</script>

<style scoped>
.batch-reverse-trace-panel { display: grid; gap: 14px; }
.batch-reverse-trace-panel__context { display: block; color: #64748b; white-space: normal; }
.batch-reverse-trace-panel__anchor,
.batch-reverse-trace-panel__scope,
.batch-reverse-trace-panel__logic,
.batch-reverse-trace-panel__conditions-header,
.batch-reverse-trace-panel__results-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.batch-reverse-trace-panel__section-title { color: #1f2d3d; font-weight: 600; }
.batch-reverse-trace-panel__items { display: grid; gap: 6px; }
.batch-reverse-trace-panel__catalog-item { display: flex; justify-content: space-between; width: 100%; padding: 8px 10px; border: 1px solid #e5e7eb; border-radius: 6px; }
.batch-reverse-trace-panel__catalog-value { color: #64748b; }
.batch-reverse-trace-panel__condition { display: flex; align-items: center; gap: 8px; padding: 10px; border: 1px solid #e5e7eb; border-radius: 6px; }
.batch-reverse-trace-panel__condition-main { display: flex; flex: 1; align-items: center; gap: 8px; min-width: 0; }
.batch-reverse-trace-panel__condition-label { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.batch-reverse-trace-panel__condition-value { max-width: 180px; }
.batch-reverse-trace-panel__actions { display: flex; justify-content: flex-end; gap: 8px; }
@media (max-width: 720px) {
  .batch-reverse-trace-panel__condition-main, .batch-reverse-trace-panel__scope { flex-wrap: wrap; }
  .batch-reverse-trace-panel__condition-label { flex-basis: 100%; }
  .batch-reverse-trace-panel :deep(.el-pagination) { flex-wrap: wrap; }
}
</style>
