<!-- MES 璞慧排产 -->
<template>
  <ContentWrap>
    <div class="puhui-page-head">
      <div>
        <h2>璞慧排产</h2>
        <p>集团排产</p>
      </div>
      <el-tag type="primary" effect="plain">本地场景</el-tag>
    </div>

    <el-alert
      v-if="localStorageError"
      type="error"
      show-icon
      :closable="false"
      class="mb-12px"
      :title="localStorageError"
    >
      <template #default>
        <el-button type="danger" plain size="small" @click="clearCorruptLocalStorage">
          清空本地损坏数据并重置
        </el-button>
      </template>
    </el-alert>

    <el-form class="puhui-toolbar" :inline="true" label-width="92px" @submit.prevent>
      <el-form-item label="排产起始日">
        <el-date-picker
          :model-value="scenario.horizonStart"
          value-format="YYYY-MM-DD"
          type="date"
          class="!w-160px"
          :disabled="hasStorageError"
          @update:model-value="updateHorizonStart"
        />
      </el-form-item>
      <el-form-item label="排产模式">
        <el-radio-group
          :model-value="scenario.planningMode"
          :disabled="hasStorageError"
          @change="switchPlanningMode"
        >
          <el-radio-button :label="PLANNING_MODE.QTY_CAPACITY">按数量排产</el-radio-button>
          <el-radio-button :label="PLANNING_MODE.DURATION_MANUAL_FINISH">按天数排产</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="节假日">
        <el-switch
          :model-value="scenario.skipStatutoryHolidays"
          active-text="跳过法定节假日"
          :disabled="hasStorageError"
          @update:model-value="updateSkipHolidays"
        />
      </el-form-item>
      <el-form-item label="周末模式">
        <el-radio-group
          :model-value="scenario.weekendRestMode"
          :disabled="hasStorageError"
          @change="updateWeekendMode"
        >
          <el-radio-button :label="WEEKEND_REST_MODE.NONE">不休</el-radio-button>
          <el-radio-button :label="WEEKEND_REST_MODE.SINGLE">单休</el-radio-button>
          <el-radio-button :label="WEEKEND_REST_MODE.DOUBLE">双休</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <span class="puhui-hint">内置法定节假日年份：{{ holidayYearHintText }}</span>
      </el-form-item>
      <el-form-item class="puhui-actions">
        <el-button type="primary" :disabled="hasStorageError" @click="advanceOneDay">
          <Icon icon="ep:d-arrow-right" class="mr-5px" /> 推进1天
        </el-button>
        <el-button :disabled="hasStorageError" @click="replanFromToday">
          <Icon icon="ep:refresh-right" class="mr-5px" /> 从今天重排
        </el-button>
        <el-button plain :disabled="hasStorageError" @click="openSnapshotModal('save')">
          <Icon icon="ep:folder-add" class="mr-5px" /> 保存场景
        </el-button>
        <el-button plain :disabled="hasStorageError" @click="openSnapshotModal('load')">
          <Icon icon="ep:folder-opened" class="mr-5px" /> 读取场景
        </el-button>
        <el-button type="danger" plain :disabled="hasStorageError" @click="resetScenario">
          <Icon icon="ep:delete" class="mr-5px" /> 重置默认
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="puhui-kpi-grid">
      <div class="puhui-kpi">
        <span>订单数</span>
        <strong>{{ plan.summary.totalOrders }}</strong>
      </div>
      <div class="puhui-kpi">
        <span>{{ isDurationMode ? '总排线 / 线天' : '已分配 / 产能' }}</span>
        <strong>{{ formatNumber(plan.summary.totalAssigned) }} / {{ formatCapacityNumber(plan.summary.totalCapacity) }}</strong>
      </div>
      <div class="puhui-kpi">
        <span>利用率</span>
        <strong>{{ formatPercent(plan.summary.utilization) }}</strong>
      </div>
      <div class="puhui-kpi">
        <span>延期订单</span>
        <strong>{{ plan.summary.delayedOrders }}</strong>
      </div>
      <div class="puhui-kpi">
        <span>剩余工作量</span>
        <strong>{{ formatNumber(plan.summary.totalRemaining) }}</strong>
      </div>
    </div>

    <el-alert
      v-if="plan.warnings.length > 0"
      type="warning"
      show-icon
      :closable="false"
      class="mt-12px"
      :title="plan.warnings.join('；')"
    />
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab" class="puhui-tabs">
      <el-tab-pane label="订单录入" name="orders">
        <div class="puhui-tab-toolbar">
          <el-button type="primary" :disabled="hasStorageError" @click="openOrderModal">
            <Icon icon="ep:plus" class="mr-5px" /> 新增订单
          </el-button>
          <span class="puhui-hint">
            {{ isDurationMode ? '计划个数仅展示，不参与按天数排产计算。' : '产线工作量为订单总量，不是日工作量。' }}
          </span>
        </div>
        <el-table :data="orderRows" :stripe="true" :show-overflow-tooltip="true" border>
          <el-table-column label="订单号" prop="order_no" min-width="130" fixed="left" />
          <el-table-column label="产品名称" prop="product_name" min-width="130" />
          <el-table-column label="规格" prop="spec" min-width="100" />
          <el-table-column label="批号" prop="batch_no" min-width="100" />
          <el-table-column
            :label="isDurationMode ? '计划天数(天)' : '总工作量(个)'"
            prop="workload_qty"
            width="130"
          >
            <template #default="{ row }">{{ formatNumber(row.workload_qty) }}</template>
          </el-table-column>
          <el-table-column v-if="isDurationMode" label="计划个数(个)" prop="planned_qty" width="120">
            <template #default="{ row }">{{ formatNumber(row.planned_qty) }}</template>
          </el-table-column>
          <el-table-column v-if="!isDurationMode" label="已完成(个)" prop="completed_qty" width="110">
            <template #default="{ row }">{{ formatNumber(row.completed_qty) }}</template>
          </el-table-column>
          <el-table-column v-if="!isDurationMode" label="未排量(个)" prop="remaining_qty" width="110">
            <template #default="{ row }">{{ formatNumber(row.remaining_qty) }}</template>
          </el-table-column>
          <el-table-column v-if="!isDurationMode" label="约需天数" prop="remaining_plan_days" width="100">
            <template #default="{ row }">{{ formatNumber(row.remaining_plan_days) }}</template>
          </el-table-column>
          <el-table-column v-if="isDurationMode" label="结束状态" prop="finish_status" width="110" />
          <el-table-column v-if="isDurationMode" label="实际结束时间" prop="actual_finish_date" width="130" />
          <el-table-column v-if="!isDurationMode" label="预计完成" prop="completion_date" width="120" />
          <el-table-column label="排产说明" prop="reason" min-width="220" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" :disabled="hasStorageError" @click="openInsertModal(row.id)">插入</el-button>
              <el-button link type="primary" :disabled="hasStorageError" @click="openEditOrderModal(row.id)">编辑</el-button>
              <el-button link type="danger" :disabled="hasStorageError" @click="removeOrder(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="每日排产" name="schedule">
        <p class="puhui-panel-hint">
          {{ isDurationMode ? '按天数模式：在日历点击订单可手动报结束，未报结束会顺延后续订单。' : `瓶颈产线：${plan.summary.bottleneckLineName || '-'}` }}
        </p>
        <PuhuiScheduleCalendar
          :plan="calendarPlan"
          :calendar-month="calendarMonth"
          :scenario="scenario"
          :is-duration-mode="isDurationMode"
          :order-meta-map="orderMetaMap"
          @update-month="updateCalendarMonth"
          @move-month="moveCalendarMonth"
          @export-excel="exportScheduledOrdersExcel"
          @set-date-work-mode="setDateWorkMode"
          @open-finish-modal="openFinishModal"
        />
      </el-tab-pane>

      <el-tab-pane label="产能调整" name="capacity">
        <el-alert
          v-if="isDurationMode"
          type="info"
          show-icon
          :closable="false"
          title="当前为按天数模式，日产能设置不会参与排程计算。"
          class="mb-12px"
        />
        <div class="puhui-tab-toolbar">
          <el-select v-model="capacityForm.lineId" class="!w-220px" placeholder="产线" :disabled="hasStorageError">
            <el-option v-for="line in scenario.lines" :key="line.id" :label="line.name" :value="line.id" />
          </el-select>
          <el-input-number
            v-model="capacityForm.capacity"
            :min="0"
            :precision="1"
            :step="10"
            :disabled="hasStorageError"
            controls-position="right"
          />
          <el-button type="primary" :disabled="hasStorageError" @click="saveDailyCapacity">保存</el-button>
        </div>
        <el-table :data="lineDailyCapacityRows" border :stripe="true">
          <el-table-column label="产线" prop="line_name" min-width="160" />
          <el-table-column label="日产能(个/天)" prop="daily_capacity" width="160">
            <template #default="{ row }">{{ formatCapacityNumber(row.daily_capacity) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="产线管理" name="lines">
        <div class="puhui-tab-toolbar">
          <el-input v-model="lineForm.name" placeholder="产线名称" class="!w-220px" :disabled="hasStorageError" />
          <el-input-number
            v-model="lineForm.baseCapacity"
            :min="0"
            :precision="1"
            :step="10"
            :disabled="hasStorageError"
            controls-position="right"
          />
          <el-button type="primary" :disabled="hasStorageError" @click="addLine">
            <Icon icon="ep:plus" class="mr-5px" /> 新增产线
          </el-button>
        </div>
        <el-table :data="lineRows" border :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="产线" min-width="180">
            <template #default="{ row }">
              <el-input
                :model-value="row.line_name"
                :disabled="hasStorageError"
                @blur="(event) => updateLineName(row.id, (event.target as HTMLInputElement).value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="默认日产能(个/天)" width="190">
            <template #default="{ row }">
              <el-input-number
                :model-value="row.base_capacity"
                :min="0"
                :precision="1"
                :step="10"
                :disabled="hasStorageError"
                controls-position="right"
                @change="(value) => updateLineBaseCapacity(row.id, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="周期内分配(个)" prop="assigned_total" width="140">
            <template #default="{ row }">{{ formatNumber(row.assigned_total) }}</template>
          </el-table-column>
          <el-table-column label="周期最大产能(个)" prop="capacity_total" width="150">
            <template #default="{ row }">{{ formatCapacityNumber(row.capacity_total) }}</template>
          </el-table-column>
          <el-table-column label="利用率" prop="utilization" width="100">
            <template #default="{ row }">{{ formatPercent(row.utilization) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="danger" :disabled="hasStorageError" @click="removeLine(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <el-dialog v-model="showOrderModal" :title="editingOrderId ? '编辑订单' : '新增订单'" width="760px" destroy-on-close>
    <el-form label-width="90px">
      <div class="puhui-form-grid">
        <el-form-item label="订单号">
          <el-input v-model="orderModalForm.orderNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="orderModalForm.productName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="orderModalForm.spec" placeholder="可选" />
        </el-form-item>
        <el-form-item label="批号">
          <el-input v-model="orderModalForm.batchNo" placeholder="可选" />
        </el-form-item>
      </div>
      <div class="puhui-allocation-list">
        <div v-for="line in scenario.lines" :key="line.id" class="puhui-allocation-row">
          <span>{{ line.name }}</span>
          <template v-if="isDurationMode">
            <el-input-number v-model="orderModalForm.linePlanDays[line.id]" :min="0" :precision="0" controls-position="right" />
            <em>天</em>
            <el-input-number
              v-model="orderModalForm.linePlanQuantities[line.id]"
              :min="0"
              :precision="0"
              controls-position="right"
            />
            <em>个</em>
          </template>
          <template v-else>
            <el-input-number v-model="orderModalForm.lineTotals[line.id]" :min="0" :precision="1" controls-position="right" />
            <em>总量</em>
          </template>
        </div>
      </div>
      <div class="puhui-dialog-hint">
        {{ isDurationMode ? `订单计划总天数：${formatNumber(modalTotalWorkload)} 天，计划总个数：${formatNumber(modalTotalPlanQty)} 个` : `订单总工作量：${formatNumber(modalTotalWorkload)} 个` }}
      </div>
    </el-form>
    <template #footer>
      <el-button @click="closeOrderModal">取消</el-button>
      <el-button type="primary" @click="submitOrderFromModal">{{ editingOrderId ? '保存修改' : '创建订单' }}</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showInsertModal" title="插入订单" width="520px" destroy-on-close>
    <el-form label-width="90px">
      <el-form-item label="订单">
        <el-select v-model="insertForm.orderId" class="!w-1/1" placeholder="请选择">
          <el-option v-for="order in scenario.orders" :key="order.id" :label="order.orderNo" :value="order.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="插入日期">
        <el-date-picker v-model="insertForm.date" type="date" value-format="YYYY-MM-DD" class="!w-1/1" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeInsertModal">取消</el-button>
      <el-button type="primary" @click="submitInsertOrder">插入并重排</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="showSnapshotModal" :title="snapshotModalMode === 'save' ? '保存场景' : '读取场景'" width="760px" destroy-on-close>
    <div v-if="snapshotModalMode === 'save'" class="puhui-tab-toolbar">
      <el-input v-model="snapshotName" placeholder="场景名称" class="!w-260px" />
      <el-button type="primary" @click="saveSnapshotToLocal">保存到本地</el-button>
    </div>
    <el-table :data="snapshotRows" border :stripe="true">
      <el-table-column label="场景名称" min-width="180">
        <template #default="{ row }">
          <el-input v-model="row.name" @blur="renameSnapshot(row.id, row.name)" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updated_at" width="170" />
      <el-table-column label="创建时间" prop="created_at" width="170" />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button link type="primary" @click="loadSnapshot(row.id)">读取</el-button>
          <el-button link type="danger" @click="deleteSnapshot(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <el-dialog v-model="showFinishModal" title="手动报结束" width="520px" destroy-on-close>
    <el-descriptions :column="1" border size="small" class="mb-12px">
      <el-descriptions-item label="产线">{{ finishModalForm.lineName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单">{{ finishModalForm.orderLabel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="产线开始日期">{{ finishModalForm.startDate || '-' }}</el-descriptions-item>
    </el-descriptions>
    <el-date-picker
      v-model="finishModalForm.finishDate"
      type="date"
      value-format="YYYY-MM-DD"
      :min="finishModalForm.startDate || undefined"
      :max="scenario.horizonStart || undefined"
      class="!w-1/1"
    />
    <template #footer>
      <el-button @click="closeFinishModal">取消</el-button>
      <el-button v-if="hasSavedFinish" type="danger" plain @click="clearManualFinish">清除报结束</el-button>
      <el-button type="primary" @click="submitManualFinish">保存报结束</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import PuhuiScheduleCalendar from './components/PuhuiScheduleCalendar.vue'
import {
  DATE_WORK_MODE,
  PLANNING_MODE,
  WEEKEND_REST_MODE,
  addDays,
  advanceLiteScenarioOneDay,
  buildCalendarWeeksByMonth,
  buildExcelTableHtml,
  buildInsertOrderMutation,
  buildLiteSchedule,
  buildOrderUpsertResult,
  buildScheduledOrdersExportPayload,
  clampNumber,
  collectScheduledRows,
  compareDate,
  createDefaultLiteScenario,
  createOrderModalForm,
  createOrderModalFormFromOrder,
  downloadTextFile,
  formatCapacityNumber,
  formatNumber,
  formatPercent,
  formatSnapshotDisplay,
  formatSnapshotName,
  isoToday,
  makeId,
  makeLineOrderKey,
  monthTextFromDate,
  normalizeLiteScenario,
  parseMonthText,
  readLiteScenarioFromStorage,
  readLiteSnapshotsFromStorage,
  supportedCnHolidayYears,
  toNumber,
  writeLiteScenarioToStorage,
  writeLiteSnapshotsToStorage,
  type LiteScenario,
  type LiteSnapshot,
  type OrderModalForm
} from './scheduler'

defineOptions({ name: 'MesProPuhuiSchedule' })

const message = useMessage()
const activeTab = ref('orders')
const localStorageError = ref('')
const scenario = ref<LiteScenario>(createDefaultLiteScenario())
const calendarMonth = ref(monthTextFromDate(scenario.value.horizonStart) || '2026-01')
const lineForm = reactive({ name: '', baseCapacity: 300 })
const capacityForm = reactive({ lineId: '', capacity: 300 })
const showOrderModal = ref(false)
const editingOrderId = ref<string | null>(null)
const orderModalForm = ref<OrderModalForm>(createOrderModalForm(scenario.value))
const showInsertModal = ref(false)
const insertForm = reactive({ orderId: '', date: scenario.value.horizonStart })
const showSnapshotModal = ref(false)
const snapshotModalMode = ref<'save' | 'load'>('save')
const snapshotName = ref(formatSnapshotName(new Date()))
const snapshots = ref<LiteSnapshot[]>([])
const showFinishModal = ref(false)
const finishModalForm = reactive({
  key: '',
  lineId: '',
  lineName: '',
  orderId: '',
  orderLabel: '',
  startDate: '',
  finishDate: ''
})

const hasStorageError = computed(() => Boolean(localStorageError.value))
const plan = computed(() => buildLiteSchedule(scenario.value))
const isDurationMode = computed(() => scenario.value.planningMode === PLANNING_MODE.DURATION_MANUAL_FINISH)
const lineNameMap = computed(() => Object.fromEntries(scenario.value.lines.map((line) => [line.id, line.name])))
const orderMetaMap = computed(() =>
  Object.fromEntries(
    scenario.value.orders.map((order) => [
      order.id,
      {
        orderNo: order.orderNo,
        productName: order.productName || '',
        spec: order.spec || '',
        batchNo: order.batchNo || '',
        linePlanQuantities: order.linePlanQuantities || {},
        lineWorkloads: order.lineWorkloads || {}
      }
    ])
  )
)
const totalDailyCapacity = computed(() =>
  scenario.value.lines
    .filter((line) => line.enabled !== false)
    .reduce((sum, line) => sum + (isDurationMode.value ? 1 : Math.max(0, toNumber(line.baseCapacity, 0))), 0)
)
const orderRows = computed(() =>
  plan.value.orderRows.map((row) => {
    const plannedQty = Object.values(row.linePlanQuantities || {}).reduce(
      (sum, value) => sum + Math.max(0, Math.round(toNumber(value, 0))),
      0
    )
    return {
      id: row.id,
      order_no: row.orderNo,
      product_name: String(row.productName || '').trim() || '-',
      spec: String(row.spec || '').trim() || '-',
      batch_no: String(row.batchNo || '').trim() || '-',
      workload_qty: row.workloadDays,
      planned_qty: plannedQty,
      completed_qty: row.completedDays,
      remaining_qty: row.remainingDays,
      remaining_plan_days: isDurationMode.value
        ? row.remainingDays
        : totalDailyCapacity.value > 0
          ? row.remainingDays / totalDailyCapacity.value
          : 0,
      finish_status: row.finishStatus || '-',
      actual_finish_date: row.actualFinishDate || '-',
      completion_date: row.completionDate || '-',
      reason: row.reason
    }
  })
)
const lineRows = computed(() =>
  plan.value.lineRows.map((row) => ({
    id: row.lineId,
    line_name: row.lineName,
    base_capacity: scenario.value.lines.find((line) => line.id === row.lineId)?.baseCapacity ?? 0,
    assigned_total: row.assignedTotal,
    capacity_total: row.capacityTotal,
    utilization: row.utilization
  }))
)
const lineDailyCapacityRows = computed(() =>
  scenario.value.lines.map((line) => ({ id: line.id, line_name: line.name, daily_capacity: line.baseCapacity }))
)
const modalTotalWorkload = computed(() => {
  if (isDurationMode.value) {
    return Object.values(orderModalForm.value.linePlanDays || {}).reduce(
      (maxValue, value) => Math.max(maxValue, Math.max(0, Math.round(toNumber(value, 0)))),
      0
    )
  }
  return Object.values(orderModalForm.value.lineTotals || {}).reduce((sum, value) => sum + Math.max(0, toNumber(value, 0)), 0)
})
const modalTotalPlanQty = computed(() =>
  Object.values(orderModalForm.value.linePlanQuantities || {}).reduce(
    (sum, value) => sum + Math.max(0, Math.round(toNumber(value, 0))),
    0
  )
)
const holidayYearHintText = computed(() => {
  const startYear = Number(scenario.value.horizonStart.slice(0, 4))
  const years = supportedCnHolidayYears().filter((year) => !Number.isInteger(startYear) || year >= startYear)
  return years.length > 0 ? years.join('、') : '暂无'
})
const calendarPlan = computed(() => {
  const parsedMonth = parseMonthText(calendarMonth.value)
  if (!parsedMonth) {
    return plan.value
  }
  const daysInMonth = new Date(Date.UTC(parsedMonth.year, parsedMonth.month, 0)).getUTCDate()
  const monthEnd = `${calendarMonth.value}-${String(daysInMonth).padStart(2, '0')}`
  const daysToMonthEnd = compareDate(monthEnd, scenario.value.horizonStart) + 1
  const expandedDays = Math.max(1, scenario.value.horizonDays, daysToMonthEnd)
  return expandedDays > scenario.value.horizonDays
    ? buildLiteSchedule({ ...scenario.value, horizonDays: expandedDays })
    : plan.value
})
const snapshotRows = computed(() =>
  snapshots.value.map((row) => ({
    id: row.id,
    name: row.name,
    updated_at: formatSnapshotDisplay(row.updatedAt),
    created_at: formatSnapshotDisplay(row.createdAt)
  }))
)
const hasSavedFinish = computed(() => Boolean(scenario.value.manualFinishByLineOrder?.[finishModalForm.key]))

function storageWriteError(error: unknown) {
  localStorageError.value = `本地排产场景写入失败，当前操作未保存：${error instanceof Error ? error.message : String(error)}`
  message.error(localStorageError.value)
}

function persistScenario(nextScenario: LiteScenario, successMessage = '') {
  if (hasStorageError.value) {
    message.error('本地存储异常未处理，当前操作已阻止。')
    return false
  }
  const normalized = normalizeLiteScenario(nextScenario)
  try {
    writeLiteScenarioToStorage(normalized)
    scenario.value = normalized
    if (successMessage) {
      message.success(successMessage)
    }
    return true
  } catch (error) {
    storageWriteError(error)
    return false
  }
}

function applyScenario(mutator: (prev: LiteScenario) => LiteScenario, successMessage = '') {
  return persistScenario(mutator(scenario.value), successMessage)
}

async function confirmAction(text: string) {
  try {
    await message.confirm(text)
    return true
  } catch {
    return false
  }
}

function loadScenario() {
  const result = readLiteScenarioFromStorage()
  if (result.error || !result.value) {
    localStorageError.value = result.error || '本地排产场景读取失败。'
    return
  }
  scenario.value = result.value
  calendarMonth.value = monthTextFromDate(result.value.horizonStart) || calendarMonth.value
  orderModalForm.value = createOrderModalForm(result.value)
  insertForm.date = result.value.horizonStart
}

async function clearCorruptLocalStorage() {
  if (!(await confirmAction('确认清空本地损坏的璞慧排产数据并重置默认场景吗？'))) {
    return
  }
  try {
    window.localStorage.removeItem('liteScheduler.scenario.v1')
    window.localStorage.removeItem('liteScheduler.scenario.snapshots.v1')
    const nextScenario = createDefaultLiteScenario()
    writeLiteScenarioToStorage(nextScenario)
    scenario.value = nextScenario
    localStorageError.value = ''
    message.success('本地排产数据已重置。')
  } catch (error) {
    storageWriteError(error)
  }
}

function updateHorizonStart(value: string) {
  if (!value) {
    return
  }
  applyScenario((prev) => ({ ...prev, horizonStart: value }))
  calendarMonth.value = monthTextFromDate(value) || calendarMonth.value
}

function switchPlanningMode(value: string | number | boolean) {
  const nextMode = String(value) === PLANNING_MODE.DURATION_MANUAL_FINISH
    ? PLANNING_MODE.DURATION_MANUAL_FINISH
    : PLANNING_MODE.QTY_CAPACITY
  if (scenario.value.planningMode === nextMode) {
    return
  }
  applyScenario(
    (prev) => ({ ...prev, planningMode: nextMode }),
    nextMode === PLANNING_MODE.DURATION_MANUAL_FINISH ? '已切换为按天数排产。' : '已切换为按数量排产。'
  )
}

function updateSkipHolidays(value: boolean | string | number) {
  applyScenario(
    (prev) => ({ ...prev, skipStatutoryHolidays: value === true }),
    value === true ? '已开启：排产跳过法定节假日。' : '已关闭：排产包含法定节假日。'
  )
}

function updateWeekendMode(value: string | number | boolean) {
  const nextMode =
    String(value) === WEEKEND_REST_MODE.NONE
      ? WEEKEND_REST_MODE.NONE
      : String(value) === WEEKEND_REST_MODE.SINGLE
        ? WEEKEND_REST_MODE.SINGLE
        : WEEKEND_REST_MODE.DOUBLE
  applyScenario((prev) => ({ ...prev, weekendRestMode: nextMode }), '周末模式已更新。')
}

function advanceOneDay() {
  try {
    const result = advanceLiteScenarioOneDay(scenario.value)
    persistScenario(result.nextScenario, `已推进到 ${result.nextScenario.horizonStart}，当日完成：${formatNumber(result.daySummary.completedWorkload)}`)
  } catch (error) {
    message.error(error instanceof Error ? error.message : String(error))
  }
}

function replanFromToday() {
  const today = isoToday()
  applyScenario((prev) => ({ ...prev, horizonStart: today }), `已从今天 ${today} 开始重排。`)
  calendarMonth.value = monthTextFromDate(today) || calendarMonth.value
}

async function resetScenario() {
  if (!(await confirmAction('确认重置当前场景吗？'))) {
    return
  }
  const nextScenario = createDefaultLiteScenario(scenario.value.horizonStart)
  if (persistScenario(nextScenario, '场景已重置。')) {
    orderModalForm.value = createOrderModalForm(nextScenario)
    closeInsertModal()
    closeOrderModal()
    closeFinishModal()
    calendarMonth.value = monthTextFromDate(nextScenario.horizonStart) || calendarMonth.value
  }
}

function updateCalendarMonth(value: string) {
  if (value) {
    calendarMonth.value = value
  }
}

function moveCalendarMonth(offset: number) {
  const parsed = parseMonthText(calendarMonth.value)
  if (!parsed) {
    return
  }
  const base = new Date(Date.UTC(parsed.year, parsed.month - 1 + offset, 1))
  calendarMonth.value = `${base.getUTCFullYear()}-${String(base.getUTCMonth() + 1).padStart(2, '0')}`
}

function setDateWorkMode(dateText: string, mode: string | null) {
  if (!dateText) {
    return
  }
  applyScenario(
    (prev) => {
      const nextMap = { ...(prev.dateWorkModeByDate || {}) }
      if (!mode) {
        delete nextMap[dateText]
      } else {
        nextMap[dateText] = mode === DATE_WORK_MODE.REST ? DATE_WORK_MODE.REST : DATE_WORK_MODE.WORK
      }
      return { ...prev, dateWorkModeByDate: nextMap }
    },
    mode === DATE_WORK_MODE.REST
      ? `${dateText} 已设为休息。`
      : mode === DATE_WORK_MODE.WORK
        ? `${dateText} 已设为排产。`
        : `${dateText} 已恢复默认规则。`
  )
}

function exportScheduledOrdersExcel() {
  const scheduledRows = collectScheduledRows({
    allocations: calendarPlan.value.allocations,
    compareDate,
    lineNameMap: lineNameMap.value,
    orderMetaMap: orderMetaMap.value
  })
  const payload = buildScheduledOrdersExportPayload({
    scheduledRows,
    orderMetaMap: orderMetaMap.value,
    lineNameMap: lineNameMap.value,
    manualFinishByLineOrder: scenario.value.manualFinishByLineOrder,
    makeLineOrderKey,
    isDurationMode: isDurationMode.value,
    stamp: isoToday()
  })
  if (payload.error) {
    message.error(payload.error)
    return
  }
  downloadTextFile(buildExcelTableHtml(payload.headers, payload.rows), payload.fileName, 'application/vnd.ms-excel')
  message.success(`已导出排产订单：${scheduledRows.length} 条`)
}

function openOrderModal() {
  orderModalForm.value = createOrderModalForm(scenario.value)
  editingOrderId.value = null
  showOrderModal.value = true
}

function openEditOrderModal(orderId: string) {
  const order = scenario.value.orders.find((row) => row.id === orderId)
  if (!order) {
    message.error('未找到要编辑的订单。')
    return
  }
  orderModalForm.value = createOrderModalFormFromOrder(scenario.value, order)
  editingOrderId.value = order.id
  showOrderModal.value = true
}

function closeOrderModal() {
  showOrderModal.value = false
  editingOrderId.value = null
}

function submitOrderFromModal() {
  const result = buildOrderUpsertResult({
    scenario: scenario.value,
    orderModalForm: {
      ...orderModalForm.value,
      dueDate: orderModalForm.value.dueDate || addDays(scenario.value.horizonStart, 7),
      releaseDate: orderModalForm.value.releaseDate || scenario.value.horizonStart
    },
    editingOrderId: editingOrderId.value,
    isDurationMode: isDurationMode.value,
    lineNameMap: lineNameMap.value
  })
  if (result.error || !result.mutator) {
    message.error(result.error || '订单保存失败。')
    return
  }
  if (applyScenario(result.mutator, result.message || '订单已保存。')) {
    closeOrderModal()
  }
}

function openInsertModal(orderId: string) {
  insertForm.orderId = orderId
  insertForm.date = scenario.value.horizonStart
  showInsertModal.value = true
}

function closeInsertModal() {
  showInsertModal.value = false
  insertForm.orderId = ''
  insertForm.date = scenario.value.horizonStart
}

async function submitInsertOrder() {
  const order = scenario.value.orders.find((row) => row.id === insertForm.orderId)
  if (!order) {
    message.error('请选择要插入的订单。')
    return
  }
  const insertDate = String(insertForm.date || '').trim() || scenario.value.horizonStart
  if (!(await confirmAction(`确认将订单 ${order.orderNo} 从 ${insertDate} 开始插入排产，并顺延其余任务吗？`))) {
    return
  }
  if (applyScenario(buildInsertOrderMutation({ orderId: order.id, insertDate }), '订单已插入，并已顺延后续任务。')) {
    closeInsertModal()
  }
}

async function removeOrder(orderId: string) {
  const hasLock = scenario.value.locks.some((lock) => lock.orderId === orderId)
  if (hasLock) {
    message.error('该订单存在锁定片段，请先删除锁定后再删除订单。')
    return
  }
  if (!(await confirmAction('确认删除该订单吗？'))) {
    return
  }
  applyScenario((prev) => ({ ...prev, orders: prev.orders.filter((order) => order.id !== orderId) }), '订单已删除。')
}

function addLine() {
  const name = String(lineForm.name || '').trim() || `产线-${scenario.value.lines.length + 1}`
  const baseCapacity = Math.max(0, toNumber(lineForm.baseCapacity, 300))
  applyScenario(
    (prev) => ({
      ...prev,
      lines: [...prev.lines, { id: makeId('line'), name, baseCapacity, capacityOverrides: {}, enabled: true }]
    }),
    `已新增产线：${name}`
  )
  lineForm.name = ''
  lineForm.baseCapacity = 300
}

function updateLineName(lineId: string, nextValue: string) {
  const nextName = String(nextValue || '').trim()
  if (!nextName) {
    return
  }
  applyScenario((prev) => ({
    ...prev,
    lines: prev.lines.map((line) => (line.id === lineId ? { ...line, name: nextName } : line))
  }))
}

function updateLineBaseCapacity(lineId: string, nextValue: unknown) {
  const baseCapacity = Math.max(0, toNumber(nextValue, 0))
  applyScenario((prev) => ({
    ...prev,
    lines: prev.lines.map((line) => (line.id === lineId ? { ...line, baseCapacity } : line))
  }))
}

async function removeLine(lineId: string) {
  if (scenario.value.lines.length <= 1) {
    message.error('至少保留一条产线。')
    return
  }
  const lockUsingLine = scenario.value.locks.some((lock) => lock.lineId === lineId)
  if (lockUsingLine) {
    message.error('该产线存在锁定片段，请先删除锁定后再删除产线。')
    return
  }
  const orderUsingLine = scenario.value.orders.find(
    (order) => Number(order.lineWorkloads?.[lineId] || 0) > 0 || Number(order.linePlanDays?.[lineId] || 0) > 0
  )
  if (orderUsingLine) {
    message.error(`订单 ${orderUsingLine.orderNo} 仍有该产线工作量，请先清空再删除产线。`)
    return
  }
  if (!(await confirmAction('确认删除该产线吗？'))) {
    return
  }
  applyScenario((prev) => ({ ...prev, lines: prev.lines.filter((line) => line.id !== lineId) }), '产线已删除。')
}

async function saveDailyCapacity() {
  if (!capacityForm.lineId) {
    message.error('请先选择产线。')
    return
  }
  if (!(await confirmAction('确认保存该产线日产能并重排吗？'))) {
    return
  }
  const cap = Math.max(0, toNumber(capacityForm.capacity, 0))
  applyScenario(
    (prev) => ({
      ...prev,
      lines: prev.lines.map((line) =>
        line.id === capacityForm.lineId ? { ...line, baseCapacity: cap, capacityOverrides: {} } : line
      )
    }),
    '日产能已保存，排产已更新。'
  )
}

function syncCapacityForm() {
  const selected = scenario.value.lines.find((line) => line.id === capacityForm.lineId) || scenario.value.lines[0]
  if (!selected) {
    capacityForm.lineId = ''
    capacityForm.capacity = 0
    return
  }
  capacityForm.lineId = selected.id
  capacityForm.capacity = selected.baseCapacity
}

function openSnapshotModal(mode: 'save' | 'load') {
  const result = readLiteSnapshotsFromStorage()
  if (result.error || !result.value) {
    localStorageError.value = result.error || '本地场景快照读取失败。'
    message.error(localStorageError.value)
    return
  }
  snapshotModalMode.value = mode
  snapshotName.value = formatSnapshotName(new Date())
  snapshots.value = result.value
  showSnapshotModal.value = true
}

function saveSnapshotToLocal() {
  const name = String(snapshotName.value || '').trim() || formatSnapshotName(new Date())
  const now = Date.now()
  const row: LiteSnapshot = {
    id: makeId('snapshot'),
    name,
    createdAt: now,
    updatedAt: now,
    scenario: normalizeLiteScenario(scenario.value)
  }
  const result = readLiteSnapshotsFromStorage()
  if (result.error || !result.value) {
    localStorageError.value = result.error || '本地场景快照读取失败。'
    message.error(localStorageError.value)
    return
  }
  const nextRows = [row, ...result.value].sort((a, b) => b.updatedAt - a.updatedAt)
  try {
    writeLiteSnapshotsToStorage(nextRows)
    snapshots.value = nextRows
    snapshotName.value = formatSnapshotName(new Date())
    message.success(`场景已保存：${name}`)
  } catch (error) {
    storageWriteError(error)
  }
}

function renameSnapshot(snapshotId: string, nextName: string) {
  const safeName = String(nextName || '').trim()
  if (!safeName) {
    message.error('场景名称不能为空。')
    return
  }
  const nextRows = snapshots.value
    .map((row) => (row.id === snapshotId ? { ...row, name: safeName, updatedAt: Date.now() } : row))
    .sort((a, b) => b.updatedAt - a.updatedAt)
  try {
    writeLiteSnapshotsToStorage(nextRows)
    snapshots.value = nextRows
    message.success(`场景已改名：${safeName}`)
  } catch (error) {
    storageWriteError(error)
  }
}

async function deleteSnapshot(snapshotId: string) {
  const target = snapshots.value.find((row) => row.id === snapshotId)
  if (!target) {
    return
  }
  if (!(await confirmAction(`确认删除场景 "${target.name}" 吗？`))) {
    return
  }
  const nextRows = snapshots.value.filter((row) => row.id !== snapshotId)
  try {
    writeLiteSnapshotsToStorage(nextRows)
    snapshots.value = nextRows
    message.success(`场景已删除：${target.name}`)
  } catch (error) {
    storageWriteError(error)
  }
}

function loadSnapshot(snapshotId: string) {
  const target = snapshots.value.find((row) => row.id === snapshotId)
  if (!target) {
    message.error('未找到要读取的场景。')
    return
  }
  if (persistScenario(target.scenario, `已读取场景：${target.name}`)) {
    showSnapshotModal.value = false
  }
}

function openFinishModal(orderEntry: any) {
  if (!isDurationMode.value || !orderEntry?.orderId || !orderEntry?.lineId) {
    return
  }
  const key = makeLineOrderKey(orderEntry.lineId, orderEntry.orderId)
  const startDate = orderEntry.segmentStartDate || scenario.value.horizonStart
  const maxDate = scenario.value.horizonStart
  const existingDate = scenario.value.manualFinishByLineOrder?.[key] || ''
  let finishDate = existingDate || maxDate
  if (compareDate(finishDate, startDate) < 0) {
    finishDate = startDate
  }
  if (compareDate(finishDate, maxDate) > 0) {
    finishDate = maxDate
  }
  finishModalForm.key = key
  finishModalForm.lineId = orderEntry.lineId
  finishModalForm.lineName = orderEntry.lineName || orderEntry.lineId
  finishModalForm.orderId = orderEntry.orderId
  finishModalForm.orderLabel = orderEntry.orderLabel || orderEntry.orderId
  finishModalForm.startDate = startDate
  finishModalForm.finishDate = finishDate
  showFinishModal.value = true
}

function closeFinishModal() {
  showFinishModal.value = false
  finishModalForm.key = ''
  finishModalForm.lineId = ''
  finishModalForm.lineName = ''
  finishModalForm.orderId = ''
  finishModalForm.orderLabel = ''
  finishModalForm.startDate = ''
  finishModalForm.finishDate = ''
}

function submitManualFinish() {
  if (!finishModalForm.key || !finishModalForm.finishDate) {
    message.error('请先选择结束日期。')
    return
  }
  if (compareDate(finishModalForm.finishDate, finishModalForm.startDate) < 0) {
    message.error('结束日期不能早于该订单在产线上的开始日期。')
    return
  }
  if (compareDate(finishModalForm.finishDate, scenario.value.horizonStart) > 0) {
    message.error('结束日期不能晚于当前排产日期。')
    return
  }
  if (
    applyScenario(
      (prev) => ({
        ...prev,
        manualFinishByLineOrder: {
          ...(prev.manualFinishByLineOrder || {}),
          [finishModalForm.key]: finishModalForm.finishDate
        }
      }),
      `${finishModalForm.lineName} - ${finishModalForm.orderLabel} 已结束，实际结束时间：${finishModalForm.finishDate}`
    )
  ) {
    closeFinishModal()
  }
}

function clearManualFinish() {
  if (!finishModalForm.key) {
    return
  }
  if (
    applyScenario(
      (prev) => {
        const nextMap = { ...(prev.manualFinishByLineOrder || {}) }
        delete nextMap[finishModalForm.key]
        return { ...prev, manualFinishByLineOrder: nextMap }
      },
      `${finishModalForm.lineName} - ${finishModalForm.orderLabel} 已清除报结束。`
    )
  ) {
    closeFinishModal()
  }
}

watch(
  () => scenario.value.lines.map((line) => `${line.id}:${line.baseCapacity}`).join('|'),
  syncCapacityForm,
  { immediate: true }
)

watch(
  () => scenario.value.horizonStart,
  (value) => {
    insertForm.date = value
    if (!parseMonthText(calendarMonth.value)) {
      calendarMonth.value = monthTextFromDate(value) || calendarMonth.value
    }
  }
)

onMounted(() => {
  loadScenario()
  buildCalendarWeeksByMonth(calendarMonth.value)
  clampNumber(0)
})
</script>

<style scoped>
.puhui-page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.puhui-page-head h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  font-weight: 650;
  letter-spacing: 0;
}

.puhui-page-head p,
.puhui-hint,
.puhui-panel-hint,
.puhui-dialog-hint {
  margin: 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
}

.puhui-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 10px;
  padding: 12px 12px 0;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #fff;
}

.puhui-actions {
  flex: 1;
}

.puhui-kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
}

.puhui-kpi {
  min-height: 72px;
  padding: 12px;
  border: 1px solid #e5ebf3;
  border-radius: 8px;
  background: #fafcff;
}

.puhui-kpi span {
  display: block;
  color: #4b5563;
  font-size: 12px;
}

.puhui-kpi strong {
  display: block;
  margin-top: 8px;
  color: #172033;
  font-size: 20px;
  font-variant-numeric: tabular-nums;
}

.puhui-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.puhui-tab-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.puhui-panel-hint {
  margin-bottom: 10px;
}

.puhui-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.puhui-allocation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 8px 0 12px;
}

.puhui-allocation-row {
  display: grid;
  grid-template-columns: minmax(120px, 1fr) 170px 32px 170px 32px;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fafcff;
}

.puhui-allocation-row span {
  color: #263247;
  font-weight: 600;
}

.puhui-allocation-row em {
  color: #4b5563;
  font-style: normal;
}

.puhui-dialog-hint {
  padding-top: 4px;
}

@media (max-width: 1180px) {
  .puhui-kpi-grid {
    grid-template-columns: repeat(2, minmax(160px, 1fr));
  }
}

@media (max-width: 760px) {
  .puhui-page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .puhui-kpi-grid,
  .puhui-form-grid {
    grid-template-columns: 1fr;
  }

  .puhui-allocation-row {
    grid-template-columns: 1fr;
  }
}
</style>
