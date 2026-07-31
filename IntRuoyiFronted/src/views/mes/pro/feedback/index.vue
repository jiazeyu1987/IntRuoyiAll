<template>
  <doc-alert title="【生产】生产报工" url="https://doc.iocoder.cn/mes/pro/feedback/" />

  <ContentWrap v-if="activeTab === 'import-record'">
    <el-form
      class="feedback-import-query-form -mb-15px"
      :model="importQueryParams"
      ref="importQueryFormRef"
      :inline="true"
      label-width="100px"
    >
      <el-form-item label="记录编号" prop="id">
        <el-input
          v-model="importQueryParams.id"
          placeholder="请输入记录编号"
          clearable
          @keyup.enter="handleImportQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="正式报工编号" prop="feedbackId">
        <el-input
          v-model="importQueryParams.feedbackId"
          placeholder="请输入正式报工编号"
          clearable
          @keyup.enter="handleImportQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="归属状态" prop="attributionStatus">
        <el-select
          v-model="importQueryParams.attributionStatus"
          placeholder="请选择归属状态"
          clearable
          class="!w-240px"
        >
          <el-option label="待归属" value="PENDING" />
          <el-option label="已归属" value="ATTRIBUTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleImportQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetImportQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
      <el-form-item class="feedback-import-toolbar">
        <el-button
          type="primary"
          plain
          @click="handleThirdPartyImport"
          v-hasPermi="['mes:pro-feedback:create']"
        >
          <Icon icon="ep:upload-filled" class="mr-5px" /> 第三方导入
        </el-button>
        <el-button
          type="warning"
          plain
          :loading="simulateImportLoading"
          @click="handleSimulateImport"
          v-hasPermi="['mes:pro-feedback:create']"
        >
          <Icon icon="ep:magic-stick" class="mr-5px" /> 模拟报工
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="activeTab === 'feedback'">
    <FrontlineFixedTemplatePanel class="mb-12px" />
    <el-alert
      v-if="isApprovalReviewFilterActive"
      title="A5 审批职责：归属会按所选订单工序生成草稿正式报工；提交正式报工后回写排产进度；审批用于质量/合规确认，不再重复归属同一导入记录。"
      type="info"
      :closable="false"
      show-icon
      class="mb-12px"
    />
    <UnifiedListTemplate
      class="feedback-fixed-list"
      table-key="mes.pro.feedback.main"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="feedbackQuickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="feedbackQuickFilter.state"
      :selected-filter-definition="feedbackQuickFilter.selectedDefinition.value"
      :operator-options="feedbackQuickFilter.operatorOptions.value"
      :columns="feedbackColumns"
      :column-saving="feedbackColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="feedbackQuickFilter.updateState"
      @quick-filter-query="feedbackQuickFilter.applyQuickFilter"
      @column-change="saveFeedbackColumnConfig"
      @column-reset="resetFeedbackColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <div class="feedback-filter-action-relocation">
          <el-button
            type="primary"
            plain
            @click="handleThirdPartyImport"
            v-hasPermi="['mes:pro-feedback:create']"
          >
            <Icon icon="ep:upload-filled" class="mr-5px" /> 第三方导入
          </el-button>
          <el-button
            type="success"
            plain
            @click="handleExport"
            :loading="exportLoading"
            v-hasPermi="['mes:pro-feedback:export']"
          >
            <Icon icon="ep:download" class="mr-5px" /> 导出
          </el-button>
        </div>
        <div class="feedback-filter-reset-action">
          <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        </div>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <div class="feedback-main-table-scroll-region">
          <el-table
            v-loading="loading"
            class="feedback-main-table"
            data-user-table-column-explicit
            data-user-table-key="mes.pro.feedback.main"
            :data="list"
            height="100%"
            border
            :stripe="true"
            :show-overflow-tooltip="true"
            row-key="id"
            @header-dragend="handleFeedbackHeaderDragend"
            @sort-change="handleTemplateSortChange"
          >
            <el-table-column
              v-if="isFeedbackColumnVisible('excelProductCode')"
              label="产品代码"
              align="center"
              prop="excelProductCode"
              :width="getFeedbackColumnLayoutWidthString('excelProductCode', 170)"
              :min-width="getFeedbackColumnMinWidthString('excelProductCode', 170)"
              v-bind="sortColumnAttrs('excelProductCode')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelProductName')"
              label="产品名称"
              align="center"
              prop="excelProductName"
              :width="getFeedbackColumnLayoutWidthString('excelProductName', 260)"
              :min-width="getFeedbackColumnMinWidthString('excelProductName', 260)"
              v-bind="sortColumnAttrs('excelProductName')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelProcessCode')"
              label="工序编码"
              align="center"
              prop="excelProcessCode"
              :width="getFeedbackColumnLayoutWidthString('excelProcessCode', 110)"
              :min-width="getFeedbackColumnMinWidthString('excelProcessCode', 110)"
              v-bind="sortColumnAttrs('excelProcessCode')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelProcessName')"
              label="工序名称"
              align="center"
              prop="excelProcessName"
              :width="getFeedbackColumnLayoutWidthString('excelProcessName', 140)"
              :min-width="getFeedbackColumnMinWidthString('excelProcessName', 140)"
              v-bind="sortColumnAttrs('excelProcessName')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelDepartment')"
              label="部门"
              align="center"
              prop="excelDepartment"
              :width="getFeedbackColumnLayoutWidthString('excelDepartment', 90)"
              :min-width="getFeedbackColumnMinWidthString('excelDepartment', 90)"
              v-bind="sortColumnAttrs('excelDepartment')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelEmployeeNo')"
              label="人员工号"
              align="center"
              prop="excelEmployeeNo"
              :width="getFeedbackColumnLayoutWidthString('excelEmployeeNo', 120)"
              :min-width="getFeedbackColumnMinWidthString('excelEmployeeNo', 120)"
              v-bind="sortColumnAttrs('excelEmployeeNo')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelEmployeeName')"
              label="人员名称"
              align="center"
              prop="excelEmployeeName"
              :width="getFeedbackColumnLayoutWidthString('excelEmployeeName', 110)"
              :min-width="getFeedbackColumnMinWidthString('excelEmployeeName', 110)"
              v-bind="sortColumnAttrs('excelEmployeeName')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelSectionLeader')"
              label="工段长"
              align="center"
              prop="excelSectionLeader"
              :width="getFeedbackColumnLayoutWidthString('excelSectionLeader', 100)"
              :min-width="getFeedbackColumnMinWidthString('excelSectionLeader', 100)"
              v-bind="sortColumnAttrs('excelSectionLeader')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('feedbackQuantity')"
              label="报工个数"
              align="right"
              prop="feedbackQuantity"
              :width="getFeedbackColumnLayoutWidthString('feedbackQuantity', 110)"
              :min-width="getFeedbackColumnMinWidthString('feedbackQuantity', 110)"
              v-bind="sortColumnAttrs('feedbackQuantity')"
            />
            <el-table-column
              v-if="isFeedbackColumnVisible('excelFeedbackTime')"
              label="日期"
              align="center"
              prop="excelFeedbackTime"
              :formatter="dateFormatter"
              :width="getFeedbackColumnLayoutWidthString('excelFeedbackTime', 180)"
              :min-width="getFeedbackColumnMinWidthString('excelFeedbackTime', 180)"
              v-bind="sortColumnAttrs('excelFeedbackTime')"
            />
          </el-table>
        </div>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <ContentWrap v-else>
    <div class="feedback-import-batch-summary mb-12px">
      <div class="feedback-import-batch-summary__header">
        <div>
          <div class="feedback-import-batch-summary__title">当前导入批次</div>
          <div class="feedback-import-batch-summary__subtitle">
            归属会按所选订单工序生成草稿正式报工；字段补齐后通过页面顶部确认报工一次性提交本批；提交正式报工后回写排产进度。
          </div>
        </div>
        <div class="feedback-import-batch-summary__actions">
          <el-button
            v-hasPermi="['mes:pro-feedback:update']"
            type="primary"
            :disabled="!currentImportRecordIds.length"
            :loading="confirmBatchLoading"
            @click="handleConfirmBatch"
          >
            确认报工
          </el-button>
        </div>
      </div>
      <div class="feedback-import-batch-summary__metrics">
        <div class="feedback-import-batch-summary__metric">
          <span>来源文件</span>
          <strong>{{ currentImportBatchSummary.sourceFileName || '未锁定批次' }}</strong>
        </div>
        <div class="feedback-import-batch-summary__metric">
          <span>总条数</span>
          <strong>{{ currentImportBatchSummary.totalCount || 0 }}</strong>
        </div>
        <div class="feedback-import-batch-summary__metric">
          <span>已归属数</span>
          <strong>{{ currentImportBatchSummary.attributedCount || 0 }}</strong>
        </div>
        <div class="feedback-import-batch-summary__metric">
          <span>未归属数</span>
          <strong>{{ currentImportBatchSummary.pendingCount || 0 }}</strong>
        </div>
        <div class="feedback-import-batch-summary__metric">
          <span>可确认草稿数</span>
          <strong>{{ currentImportBatchSummary.confirmableCount || 0 }}</strong>
        </div>
        <div class="feedback-import-batch-summary__metric">
          <span>本批跳过</span>
          <strong>{{ currentImportBatchSummary.skippedOtherOrderCount || 0 }}</strong>
        </div>
      </div>
      <el-alert
        v-if="confirmBatchBlockReasons.length"
        type="warning"
        :closable="false"
        show-icon
        class="mt-12px"
      >
        <template #title>
          <div class="feedback-import-batch-summary__blockers">
            <div v-for="reason in confirmBatchBlockReasons" :key="reason">{{ reason }}</div>
          </div>
        </template>
      </el-alert>
    </div>
    <el-table
      v-loading="importLoading"
      :data="importRecordList"
      stripe
      row-key="id"
      :cell-class-name="resolveImportRecordCellClassName"
    >
      <el-table-column label="工单" prop="workOrderCode" min-width="150" />
      <el-table-column label="产品编码" prop="itemCode" min-width="140" />
      <el-table-column label="产品名称" prop="itemName" min-width="160" />
      <el-table-column label="规格" prop="specification" min-width="120" />
      <el-table-column label="工序" min-width="180">
        <template #default="scope">
          <div class="feedback-import-process">
            <span class="feedback-import-process__line">{{ scope.row.processCode || '--' }}</span>
            <span class="feedback-import-process__line">{{ scope.row.processName || '--' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="报工数量" prop="feedbackQuantity" width="100" align="right" />
      <el-table-column label="缓存池数量" prop="surplusPoolQuantity" width="110" align="right" />
      <el-table-column
        label="报工时间"
        prop="feedbackTime"
        width="180"
        :formatter="dateFormatter"
        align="center"
      />
      <el-table-column label="归属结果" min-width="220">
        <template #default="scope">
          <div class="feedback-import-result">
            <strong
              :class="
                scope.row.attributionStatus === 'ATTRIBUTED'
                  ? 'feedback-import-result__status feedback-import-result__status--success'
                  : 'feedback-import-result__status feedback-import-result__status--warning'
              "
            >
              {{
                scope.row.attributionStatus === 'ATTRIBUTED'
                  ? isImportRecordSkippedExternalOtherOrder(scope.row)
                    ? '其他订单'
                    : '已归属'
                  : '待归属'
              }}
            </strong>
            <small v-if="scope.row.feedbackId">正式报工编号 #{{ scope.row.feedbackId }}</small>
            <small v-if="scope.row.attributionStatus === 'ATTRIBUTED'">
              归属时间：{{ formatImportAttributionTime(scope.row.attributionTime) }}
            </small>
            <small v-if="scope.row.linkedFeedbackCount">
              关联正式报工：{{ scope.row.linkedFeedbackCount }} 条
            </small>
            <small v-if="isImportRecordSkippedExternalOtherOrder(scope.row)">
              该行未生成正式报工草稿，确认报工时将跳过
            </small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="报工人" min-width="180">
        <template #default="scope">
          <UserSelectV2
            v-if="isImportRecordEditable(scope.row)"
            v-model="scope.row.feedbackUserId"
            placeholder="请选择报工人"
          />
          <span v-else class="text-[#909399]">本批跳过</span>
        </template>
      </el-table-column>
      <el-table-column label="报工时间" min-width="210">
        <template #default="scope">
          <el-date-picker
            v-if="isImportRecordEditable(scope.row)"
            v-model="scope.row.feedbackTime"
            type="datetime"
            value-format="x"
            placeholder="请选择报工时间"
            class="!w-1/1"
          />
          <span v-else class="text-[#909399]">本批跳过</span>
        </template>
      </el-table-column>
      <el-table-column label="当前审批人" min-width="180">
        <template #default="scope">
          <UserSelectV2
            v-if="isImportRecordEditable(scope.row)"
            v-model="scope.row.approveUserId"
            placeholder="请选择当前审批人"
          />
          <span v-else class="text-[#909399]">本批跳过</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="220">
        <template #default="scope">
          <el-input
            v-if="isImportRecordEditable(scope.row)"
            v-model="scope.row.remark"
            placeholder="请输入备注"
            clearable
          />
          <span v-else class="text-[#909399]">确认报工时将跳过</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.attributionStatus === 'PENDING'"
            v-hasPermi="['mes:pro-feedback:update']"
            link
            type="primary"
            @click="openAttribution(scope.row)"
          >
            选择归属
          </el-button>
          <el-button
            v-if="
              scope.row.attributionStatus === 'ATTRIBUTED' &&
              scope.row.canModifyAttribution
            "
            v-hasPermi="['mes:pro-feedback:update']"
            link
            type="warning"
            @click="openAttribution(scope.row)"
          >
            修改归属
          </el-button>
          <span
            v-if="
              scope.row.attributionStatus === 'ATTRIBUTED' &&
              !scope.row.canModifyAttribution &&
              scope.row.modifyBlockedReason
            "
            class="text-[#909399]"
          >
            {{ scope.row.modifyBlockedReason }}
          </span>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="importTotal"
      v-model:page="importQueryParams.pageNo"
      v-model:limit="importQueryParams.pageSize"
      @pagination="getImportRecordList"
    />
  </ContentWrap>

  <component
    :is="thirdPartyImportFormComponent"
    v-if="thirdPartyImportFormComponent"
    ref="thirdPartyImportFormRef"
    @success="handleImportSuccess"
  />
  <component
    :is="importAttributionDialogComponent"
    v-if="importAttributionDialogComponent"
    ref="importAttributionDialogRef"
    @success="handleAttributionSuccess"
  />
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { shallowRef } from 'vue'
import { dateFormatter, formatDateTimeValue } from '@/utils/formatTime'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import {
  ProFeedbackApi,
  type ThirdPartyFeedbackImportResultVO,
  type ProFeedbackVO,
  type ProFeedbackImportBatchSummaryVO,
  type ProFeedbackImportConfirmBatchRowReqVO,
  type ProFeedbackImportConfirmBatchReqVO,
  type ProFeedbackImportRecordVO
} from '@/api/mes/pro/feedback'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { MesProFeedbackStatusEnum } from '@/views/mes/utils/constants'
import { useEmitt } from '@/hooks/web/useEmitt'
import { checkPermi } from '@/utils/permission'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import FrontlineFixedTemplatePanel from './FrontlineFixedTemplatePanel.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition,
  type TableQuickFilterValue
} from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'MesProFeedback' })

type FeedbackQuickFilterFieldKey =
  | 'id'
  | 'code'
  | 'type'
  | 'status'
  | 'feedbackTime'

const message = useMessage()
const route = useRoute()
const { emitter } = useEmitt()
const canUpdateImportRecord = checkPermi(['mes:pro-feedback:update'])

const MES_SCHEDULE_ORDER_REFRESH_EVENT = 'mes-schedule-order-refresh'

interface MesScheduleOrderRefreshPayload {
  source?: 'DIRECT_WORK_REPORT' | 'FORMAL_FEEDBACK'
  scheduleOrderCodes: string[]
  workOrderCodes: string[]
}

const normalizeScheduleOrderRefreshCode = (value: unknown) => String(value ?? '').trim()

const uniqueScheduleOrderRefreshCodes = (values: unknown[]) =>
  Array.from(new Set(values.map(normalizeScheduleOrderRefreshCode).filter(Boolean)))

const buildDirectWorkReportScheduleOrderRefreshPayload = (
  result: ThirdPartyFeedbackImportResultVO
): MesScheduleOrderRefreshPayload | undefined => {
  const details = result.directWorkReportDetails || []
  const scheduleOrderCodes = uniqueScheduleOrderRefreshCodes(
    details.map((detail) => detail.scheduleOrderCode)
  )
  const workOrderCodes = uniqueScheduleOrderRefreshCodes(details.map((detail) => detail.workOrderCode))
  if (!scheduleOrderCodes.length && !workOrderCodes.length) {
    return undefined
  }
  return {
    source: 'DIRECT_WORK_REPORT',
    scheduleOrderCodes,
    workOrderCodes
  }
}

const emitScheduleOrderRefresh = (payload?: MesScheduleOrderRefreshPayload) => {
  emitter.emit(MES_SCHEDULE_ORDER_REFRESH_EVENT, payload)
}

const emitDirectWorkReportScheduleOrderRefresh = (result: ThirdPartyFeedbackImportResultVO) => {
  const payload = buildDirectWorkReportScheduleOrderRefreshPayload(result)
  if (!payload) {
    return
  }
  emitScheduleOrderRefresh(payload)
}

const feedbackDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'excelProductCode', label: '产品代码', width: 170 },
  { key: 'excelProductName', label: '产品名称', width: 260 },
  { key: 'excelProcessCode', label: '工序编码', width: 110 },
  { key: 'excelProcessName', label: '工序名称', width: 140 },
  { key: 'excelDepartment', label: '部门', width: 90 },
  { key: 'excelEmployeeNo', label: '人员工号', width: 120 },
  { key: 'excelEmployeeName', label: '人员名称', width: 110 },
  { key: 'excelSectionLeader', label: '工段长', width: 100 },
  { key: 'feedbackQuantity', label: '报工个数', width: 110 },
  { key: 'excelFeedbackTime', label: '日期', width: 180 }
]
const {
  columns: feedbackColumns,
  saving: feedbackColumnSaving,
  isColumnVisible: isFeedbackColumnVisible,
  getColumnWidthString: getFeedbackColumnWidthString,
  getColumnMinWidthString: getFeedbackColumnMinWidthString,
  handleHeaderDragend: handleFeedbackHeaderDragend,
  saveConfig: saveFeedbackColumnConfig,
  resetConfig: resetFeedbackColumnConfig
} = useUserTableColumns('mes.pro.feedback.main', feedbackDefaultColumns)
const feedbackFlexibleColumnKey = computed(() => {
  const preferredKeys = ['excelFeedbackTime', 'excelProductName', 'excelProductCode']
  const visibleKeys = feedbackColumns.value.filter((column) => column.visible).map((column) => column.key)
  return preferredKeys.find((key) => visibleKeys.includes(key)) || visibleKeys.at(-1)
})
const getFeedbackColumnLayoutWidthString = (key: string, fallback?: number) => {
  if (key === feedbackFlexibleColumnKey.value) {
    return undefined
  }
  return getFeedbackColumnWidthString(key, fallback)
}

const activeTab = ref<'feedback' | 'import-record'>('feedback')
const loading = ref(true)
const list = ref<ProFeedbackVO[]>([])
const total = ref(0)
const exportLoading = ref(false)
const simulateImportLoading = ref(false)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  id: undefined as number | undefined,
  code: undefined as string | undefined,
  type: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  itemId: undefined as number | undefined,
  feedbackUserId: undefined as number | undefined,
  creator: undefined as number | undefined,
  status: undefined as number | undefined,
  feedbackTime: undefined as string[] | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})
const importLoading = ref(false)
const confirmBatchLoading = ref(false)
const importRecordList = ref<ProFeedbackImportRecordVO[]>([])
const importTotal = ref(0)
const importRecordDraftMap = ref<Record<number, ProFeedbackImportConfirmBatchRowReqVO>>({})
const currentImportRecordIds = ref<number[]>([])
const currentImportBatchSummary = reactive<ProFeedbackImportBatchSummaryVO>({
  sourceFileName: undefined,
  totalCount: 0,
  pendingCount: 0,
  attributedCount: 0,
  confirmableCount: 0,
  skippedOtherOrderCount: 0
})
const importQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  id: undefined as number | undefined,
  feedbackId: undefined as number | undefined,
  attributionStatus: undefined as string | undefined
})
const feedbackQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'code', label: '报工单号', type: 'text', placeholder: '请输入报工单号' },
  { key: 'id', label: '报工编号', type: 'text', operators: ['eq'], placeholder: '请输入报工编号' },
  {
    key: 'type',
    label: '报工类型',
    type: 'select',
    options: getIntDictOptions(DICT_TYPE.MES_PRO_FEEDBACK_TYPE)
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: getIntDictOptions(DICT_TYPE.MES_PRO_FEEDBACK_STATUS)
  },
  { key: 'feedbackTime', label: '报工时间', type: 'dateRange' }
]
const confirmBatchBlockReasons = computed(() => buildConfirmBatchBlockReasons(importRecordList.value))
const isApprovalReviewFilterActive = computed(
  () => activeTab.value === 'feedback' && queryParams.status === MesProFeedbackStatusEnum.APPROVING
)

const importQueryFormRef = ref()
const thirdPartyImportFormRef = ref()
const importAttributionDialogRef = ref()
const thirdPartyImportFormComponent = shallowRef<Component | null>(null)
const importAttributionDialogComponent = shallowRef<Component | null>(null)

const clearFeedbackQueryFields = () => {
  queryParams.id = undefined
  queryParams.code = undefined
  queryParams.type = undefined
  queryParams.status = undefined
  queryParams.feedbackTime = undefined
}

const parseQuickFilterNumber = (value: unknown, label: string) => {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) {
    const errorMessage = `${label}必须是有效数字。`
    message.error(errorMessage)
    throw new Error(errorMessage)
  }
  return parsed
}

const normalizeFeedbackDateBoundary = (value: unknown, endOfDay = false) => {
  const text = String(value ?? '').trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return `${text} ${endOfDay ? '23:59:59' : '00:00:00'}`
  }
  return text
}

const applyFeedbackQuickFilterToQueryParams = () => {
  clearFeedbackQueryFields()
  const quickFilter = queryParams.quickFilter
  if (!quickFilter) return
  const fieldKey = quickFilter.fieldKey as FeedbackQuickFilterFieldKey
  if (fieldKey === 'code') {
    queryParams.code = String(quickFilter.value ?? '').trim() || undefined
    return
  }
  if (fieldKey === 'id') {
    queryParams.id = parseQuickFilterNumber(quickFilter.value, '报工编号')
    return
  }
  if (fieldKey === 'type') {
    queryParams.type = parseQuickFilterNumber(quickFilter.value, '报工类型')
    return
  }
  if (fieldKey === 'status') {
    queryParams.status = parseQuickFilterNumber(quickFilter.value, '状态')
    return
  }
  if (fieldKey === 'feedbackTime') {
    queryParams.feedbackTime = [
      normalizeFeedbackDateBoundary(quickFilter.value),
      normalizeFeedbackDateBoundary(quickFilter.valueEnd, true)
    ]
  }
}

const buildFeedbackPageParams = () => {
  applyFeedbackQuickFilterToQueryParams()
  return {
    pageNo: queryParams.pageNo,
    pageSize: queryParams.pageSize,
    id: queryParams.id,
    code: queryParams.code,
    type: queryParams.type,
    status: queryParams.status,
    feedbackTime: queryParams.feedbackTime
  }
}

const getList = async () => {
  loading.value = true
  try {
    const data = await ProFeedbackApi.getFeedbackPage(buildFeedbackPageParams())
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}
const feedbackQuickFilter = useTableQuickFilter(
  'mes.pro.feedback.main',
  feedbackQuickFilterDefinitions,
  queryParams,
  getList
)

const applyFeedbackQuickFilterPreset = async (
  fieldKey: FeedbackQuickFilterFieldKey,
  operator: 'eq',
  value: string | number
) => {
  feedbackQuickFilter.updateState({ fieldKey })
  await nextTick()
  feedbackQuickFilter.updateState({ operator, value })
}

const getImportRecordList = async () => {
  importLoading.value = true
  try {
    persistCurrentPageImportRecordDrafts()
    const data = await ProFeedbackApi.getImportRecordPage(buildImportRecordPageParams())
    importRecordList.value = applyImportRecordDrafts(data.list)
    importTotal.value = data.total
    if (currentImportRecordIds.value.length) {
      syncCurrentImportBatchSummary(await ProFeedbackApi.getImportRecordBatchSummary(currentImportRecordIds.value))
    } else {
      syncCurrentImportBatchSummary(undefined)
    }
  } finally {
    importLoading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryParams, resetFeedbackQuery())
  feedbackQuickFilter.resetQuickFilter()
}
const handleImportQuery = () => {
  importQueryParams.pageNo = 1
  getImportRecordList()
}

const resetImportQuery = () => {
  importQueryFormRef.value.resetFields()
  importRecordDraftMap.value = {}
  currentImportRecordIds.value = []
  importQueryParams.id = undefined
  importQueryParams.feedbackId = undefined
  importQueryParams.attributionStatus = undefined
  handleImportQuery()
}

const loadThirdPartyImportFormComponent = async () => {
  if (!thirdPartyImportFormComponent.value) {
    thirdPartyImportFormComponent.value = (
      await import('./ThirdPartyFeedbackImportForm.vue')
    ).default
    await nextTick()
  }
}

const loadImportAttributionDialogComponent = async () => {
  if (!importAttributionDialogComponent.value) {
    importAttributionDialogComponent.value = (await import('./ImportAttributionDialog.vue')).default
    await nextTick()
  }
}

const handleThirdPartyImport = async () => {
  await loadThirdPartyImportFormComponent()
  const dialog = thirdPartyImportFormRef.value
  if (!dialog?.open) {
    throw new Error('报工导入弹窗加载失败：缺少 open 方法')
  }
  dialog.open()
}

const SIMULATED_PROCESS_COUNT_MIN = 1
const SIMULATED_PROCESS_COUNT_MAX = 20

const promptSimulatedProcessCount = async () => {
  try {
    const { value } = await message.prompt(
      `请输入模拟工序数量（${SIMULATED_PROCESS_COUNT_MIN}-${SIMULATED_PROCESS_COUNT_MAX}）`,
      '模拟报工'
    )
    const processCount = Number(value)
    if (
      !Number.isInteger(processCount) ||
      processCount < SIMULATED_PROCESS_COUNT_MIN ||
      processCount > SIMULATED_PROCESS_COUNT_MAX
    ) {
      throw new Error(`模拟工序数量必须是 ${SIMULATED_PROCESS_COUNT_MIN} 到 ${SIMULATED_PROCESS_COUNT_MAX} 的整数`)
    }
    return processCount
  } catch (error) {
    if (isCancelError(error)) {
      return undefined
    }
    throw error
  }
}

const handleSimulateImport = async () => {
  try {
    const processCount = await promptSimulatedProcessCount()
    if (!processCount) {
      return
    }
    simulateImportLoading.value = true
    const result = await ProFeedbackApi.simulateThirdPartyXlsxImport(processCount)
    const importRecordIdsText = result.importRecordIds?.length ? result.importRecordIds.join('、') : '无'
    message.alert(
      `模拟报工完成；模拟工序数：${processCount}；工作表数：${result.sheetCount}；导入条数：${result.importedCount}；待归属条数：${result.pendingCount}；记录编号：${importRecordIdsText}`
    )
    await handleImportSuccess(result, '模拟报工')
  } catch (error) {
    message.error(resolveErrorMessage(error, '模拟报工失败，请检查后端接口。'))
    throw error
  } finally {
    simulateImportLoading.value = false
  }
}

const resetFeedbackQuery = () => ({
  id: undefined as number | undefined,
  code: undefined as string | undefined,
  type: undefined as number | undefined,
  workOrderId: undefined as number | undefined,
  itemId: undefined as number | undefined,
  feedbackUserId: undefined as number | undefined,
  creator: undefined as number | undefined,
  status: undefined as number | undefined,
  feedbackTime: undefined as string[] | undefined,
  quickFilter: undefined as TableQuickFilterValue | undefined
})

const resetImportQueryState = () => ({
  id: undefined as number | undefined,
  feedbackId: undefined as number | undefined,
  attributionStatus: undefined as string | undefined
})

const buildImportRecordPageParams = () => ({
  ...importQueryParams,
  importRecordIds: currentImportRecordIds.value
})

const persistCurrentPageImportRecordDrafts = (rows: ProFeedbackImportRecordVO[] = importRecordList.value) => {
  const nextDraftMap = { ...importRecordDraftMap.value }
  rows.forEach((row) => {
    if (!isImportRecordConfirmable(row)) {
      delete nextDraftMap[row.id]
      return
    }
    nextDraftMap[row.id] = {
      importRecordId: row.id,
      feedbackUserId: row.feedbackUserId as number,
      feedbackTime: row.feedbackTime as string | number | Date,
      approveUserId: row.approveUserId as number,
      remark: row.remark
    }
  })
  importRecordDraftMap.value = nextDraftMap
}

const applyImportRecordDrafts = (rows: ProFeedbackImportRecordVO[]) =>
  rows.map((row) => {
    if (!isImportRecordConfirmable(row)) {
      return row
    }
    const draft = importRecordDraftMap.value[row.id]
    if (!draft) {
      return row
    }
    return {
      ...row,
      feedbackUserId: draft.feedbackUserId,
      feedbackTime: draft.feedbackTime,
      approveUserId: draft.approveUserId,
      remark: draft.remark
    }
  })

const handleImportSuccess = async (result: ThirdPartyFeedbackImportResultVO, sourceLabel = '第三方导入') => {
  if (sourceLabel === '李萍报工单') {
    activeTab.value = 'feedback'
    Object.assign(queryParams, resetFeedbackQuery(), {
      quickFilter: {
        fieldKey: 'status',
        operator: 'eq',
        value: MesProFeedbackStatusEnum.APPROVING
      }
    })
    await applyFeedbackQuickFilterPreset('status', 'eq', MesProFeedbackStatusEnum.APPROVING)
    emitDirectWorkReportScheduleOrderRefresh(result)
    await getList()
    return
  }
  activeTab.value = 'import-record'
  currentImportRecordIds.value = [...result.importRecordIds]
  importRecordDraftMap.value = {}
  Object.assign(importQueryParams, resetImportQueryState())
  await getImportRecordList()
}

const openAttribution = async (row: ProFeedbackImportRecordVO) => {
  if (!canUpdateImportRecord) {
    message.error('缺少生产报工更新权限，不能执行归属操作。')
    return
  }
  await loadImportAttributionDialogComponent()
  const dialog = importAttributionDialogRef.value
  if (!dialog?.open) {
    throw new Error('报工归属弹窗加载失败：缺少 open 方法')
  }
  dialog.open(row)
}

const handleAttributionSuccess = async () => {
  activeTab.value = 'import-record'
  importQueryParams.id = undefined
  importQueryParams.feedbackId = undefined
  await getImportRecordList()
}

const handleExport = async () => {
  try {
    await message.exportConfirm()
    exportLoading.value = true
    const data = await ProFeedbackApi.exportFeedback(buildFeedbackPageParams())
    download.excel(data, '生产报工.xls')
  } finally {
    exportLoading.value = false
  }
}

const handleTabChange = async () => {
  if (activeTab.value === 'feedback') {
    await getList()
  } else {
    await getImportRecordList()
  }
}

const formatImportAttributionTime = (value?: string | number | Date) => {
  return formatDateTimeValue(value, '-')
}

const syncCurrentImportBatchSummary = (summary?: ProFeedbackImportBatchSummaryVO) => {
  currentImportBatchSummary.sourceFileName = summary?.sourceFileName
  currentImportBatchSummary.totalCount = summary?.totalCount ?? 0
  currentImportBatchSummary.pendingCount = summary?.pendingCount ?? 0
  currentImportBatchSummary.attributedCount = summary?.attributedCount ?? 0
  currentImportBatchSummary.confirmableCount = summary?.confirmableCount ?? 0
  currentImportBatchSummary.skippedOtherOrderCount = summary?.skippedOtherOrderCount ?? 0
}

const isImportRecordSkippedExternalOtherOrder = (row: ProFeedbackImportRecordVO) =>
  row.attributionTargetType === 'EXTERNAL_OTHER_ORDER'

const isImportRecordConfirmable = (row: ProFeedbackImportRecordVO) =>
  row.attributionStatus === 'ATTRIBUTED' && !isImportRecordSkippedExternalOtherOrder(row)

const isImportRecordEditable = (row: ProFeedbackImportRecordVO) =>
  canUpdateImportRecord && isImportRecordConfirmable(row)

const describeImportRecord = (row: ProFeedbackImportRecordVO) =>
  `#${row.id}(${row.workOrderCode || '-'} / ${row.processName || row.processCode || '-'})`

const importRecordWrapColumnProps = new Set([
  'workOrderCode',
  'itemCode',
  'itemName',
  'specification',
  'feedbackResult'
])

const resolveImportRecordCellClassName = ({
  column
}: {
  column: { property?: string; label?: string }
}) => {
  if (importRecordWrapColumnProps.has(column.property || '') || column.label === '工序') {
    return 'feedback-import-table__cell--wrap'
  }
  return ''
}

const buildConfirmBatchBlockReasons = (rows: ProFeedbackImportRecordVO[]) => {
  if (!currentImportRecordIds.value.length) {
    return ['当前未锁定导入批次，请先完成一次 Excel 导入或模拟报工。']
  }
  const pendingRows = rows
    .filter((row) => row.attributionStatus === 'PENDING')
    .map(describeImportRecord)
  const requiredFieldMissingRows = rows
    .filter(
      (row) =>
        isImportRecordConfirmable(row) &&
        (!row.feedbackUserId || !row.feedbackTime || !row.approveUserId)
    )
    .map(describeImportRecord)
  const notPrepareRows = rows
    .filter(
      (row) =>
        isImportRecordConfirmable(row) &&
        row.generatedFeedbackDraft &&
        row.linkedFeedbackStatus !== MesProFeedbackStatusEnum.PREPARE
    )
    .map(describeImportRecord)
  const linkIncompleteRows = rows
    .filter((row) => isImportRecordConfirmable(row) && !row.generatedFeedbackDraft)
    .map(describeImportRecord)
  const skippedRows = rows
    .filter((row) => isImportRecordSkippedExternalOtherOrder(row))
    .map(describeImportRecord)
  const reasons: string[] = []
  if (pendingRows.length) {
    reasons.push(`未归属行：${pendingRows.join('、')}`)
  }
  if (requiredFieldMissingRows.length) {
    reasons.push(`漏填字段行：${requiredFieldMissingRows.join('、')}`)
  }
  if (notPrepareRows.length) {
    reasons.push(`非草稿状态行：${notPrepareRows.join('、')}`)
  }
  if (linkIncompleteRows.length) {
    reasons.push(`未生成正式报工草稿行：${linkIncompleteRows.join('、')}`)
  }
  if (skippedRows.length) {
    reasons.push(`已跳过的“其他订单”行：${skippedRows.join('、')}`)
  }
  return reasons
}

const buildConfirmBatchPayload = (rows: ProFeedbackImportRecordVO[]): ProFeedbackImportConfirmBatchReqVO => ({
  importRecordIds: [...currentImportRecordIds.value],
  rows: rows
    .filter((row) => isImportRecordConfirmable(row))
    .map((row) => ({
      importRecordId: row.id,
      feedbackUserId: Number(row.feedbackUserId),
      feedbackTime: row.feedbackTime!,
      approveUserId: Number(row.approveUserId),
      remark: row.remark
    }))
})

const getCurrentImportBatchAllRecords = async () => {
  persistCurrentPageImportRecordDrafts()
  const pageSize = Math.max(currentImportRecordIds.value.length, 1)
  const data = await ProFeedbackApi.getImportRecordPage({
    pageNo: 1,
    pageSize,
    importRecordIds: [...currentImportRecordIds.value]
  })
  return applyImportRecordDrafts(data.list)
}

const handleConfirmBatch = async () => {
  if (!canUpdateImportRecord) {
    message.error('缺少生产报工更新权限，不能确认报工。')
    return
  }
  const confirmRows = await getCurrentImportBatchAllRecords()
  const blockReasons = buildConfirmBatchBlockReasons(confirmRows).filter(
    (reason) => !reason.startsWith('已跳过的“其他订单”行')
  )
  if (blockReasons.length) {
    await message.alertWarning(blockReasons.join('\n'))
    return
  }
  const payload = buildConfirmBatchPayload(confirmRows)
  if (!payload.rows.length) {
    await message.alertWarning('当前批次没有可确认的真实工序草稿。')
    return
  }
  try {
    await message.confirm('确认提交当前导入批次内全部真实工序草稿？提交后将统一进入审批中。')
    confirmBatchLoading.value = true
    await ProFeedbackApi.confirmImportRecordBatch(payload)
    await getImportRecordList()
    await getList()
    emitScheduleOrderRefresh()
    activeTab.value = 'feedback'
    await message.alertSuccess('报工成功')
  } catch (error) {
    if (isCancelError(error)) {
      return
    }
    message.error(resolveErrorMessage(error, '确认报工失败，请检查后端接口。'))
    throw error
  } finally {
    confirmBatchLoading.value = false
  }
}

const firstQueryValue = (value: unknown) => (Array.isArray(value) ? value[0] : value)

const toNumberQuery = (value: unknown) => {
  const firstValue = firstQueryValue(value)
  if (firstValue === undefined || firstValue === null || firstValue === '') {
    return undefined
  }
  const parsed = Number(firstValue)
  return Number.isFinite(parsed) ? parsed : undefined
}

const applyRouteQuery = async () => {
  const tab = firstQueryValue(route.query.tab)
  if (tab === 'feedback' || tab === 'import-record') {
    activeTab.value = tab
  }
  const status = toNumberQuery(route.query.status)
  if (status !== undefined) {
    Object.assign(queryParams, resetFeedbackQuery(), {
      quickFilter: { fieldKey: 'status', operator: 'eq', value: status }
    })
    await applyFeedbackQuickFilterPreset('status', 'eq', status)
  }
  const feedbackId = toNumberQuery(route.query.feedbackId)
  const importRecordId = toNumberQuery(route.query.importRecordId)
  if (feedbackId !== undefined) {
    activeTab.value = 'feedback'
    Object.assign(queryParams, resetFeedbackQuery(), {
      quickFilter: { fieldKey: 'id', operator: 'eq', value: feedbackId }
    })
    await applyFeedbackQuickFilterPreset('id', 'eq', feedbackId)
  }
  if (importRecordId !== undefined) {
    activeTab.value = 'import-record'
    Object.assign(importQueryParams, resetImportQueryState(), { id: importRecordId })
  }
}

const isCancelError = (error: unknown) => error === 'cancel' || error === 'close'

const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  if (typeof error === 'object' && error !== null && 'msg' in error) {
    const messageText = String((error as { msg?: unknown }).msg || '').trim()
    if (messageText) {
      return messageText
    }
  }
  return fallback
}

watch(
  () => route.fullPath,
  async () => {
    await applyRouteQuery()
    await handleTabChange()
  }
)

onMounted(async () => {
  await applyRouteQuery()
  await handleTabChange()
})
</script>

<style scoped>
.feedback-import-query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.feedback-import-toolbar {
  margin-left: auto;
}

.feedback-import-toolbar :deep(.el-form-item__content) {
  display: flex;
  gap: 8px;
}

.feedback-filter-action-relocation {
  display: flex;
  flex: 1 1 auto;
  justify-content: flex-end;
  gap: 8px;
  min-width: 240px;
}

.feedback-filter-reset-action {
  display: flex;
  flex: 0 0 auto;
}

.feedback-fixed-list {
  height: calc(100vh - 180px);
  min-height: 520px;
  overflow: hidden;
}

.feedback-fixed-list :deep(.unified-list-template__query-form) {
  flex: 0 0 auto;
}

.feedback-fixed-list :deep(.unified-list-template__table-shell) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.feedback-fixed-list :deep(.el-pagination) {
  flex: 0 0 auto;
}

.feedback-main-table-scroll-region {
  display: flex;
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.feedback-main-table {
  width: 100%;
  height: 100%;
}

.feedback-main-table :deep(.el-table__inner-wrapper),
.feedback-main-table :deep(.el-table__header-wrapper),
.feedback-main-table :deep(.el-table__body-wrapper) {
  width: 100%;
}

.feedback-approval-impact,
.feedback-import-result {
  display: grid;
  gap: 2px;
}

.feedback-import-process {
  display: grid;
  gap: 2px;
}

.feedback-import-process__line {
  display: block;
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  line-height: 18px;
}

.feedback-import-table__cell--wrap :deep(.cell) {
  white-space: normal;
  word-break: break-all;
  overflow-wrap: anywhere;
  text-overflow: clip;
  line-height: 18px;
}

.feedback-import-batch-summary {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.feedback-import-batch-summary__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #e5ebf3;
  background: #f7f9fc;
}

.feedback-import-batch-summary__title {
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.feedback-import-batch-summary__subtitle {
  color: #4b5563;
  font-size: 12px;
  line-height: 18px;
}

.feedback-import-batch-summary__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.feedback-import-batch-summary__metrics {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  padding: 12px 14px;
}

.feedback-import-batch-summary__metric {
  min-width: 0;
  padding: 8px 10px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fafcff;
}

.feedback-import-batch-summary__metric span {
  display: block;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.feedback-import-batch-summary__metric strong {
  display: block;
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feedback-import-batch-summary__blockers {
  display: grid;
  gap: 4px;
  line-height: 18px;
}

.feedback-approval-impact strong,
.feedback-import-result strong {
  color: #263247;
  font-size: 13px;
  font-weight: 600;
}

.feedback-approval-impact small,
.feedback-import-result small {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
}

.feedback-import-result__status {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
}

.feedback-import-result__status--warning {
  color: #d97706;
}

.feedback-import-result__status--success {
  color: #15803d;
}
</style>
