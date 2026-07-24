<template>
  <div class="puhui-calendar">
    <div class="puhui-calendar-toolbar">
      <el-button @click="$emit('moveMonth', -1)">
        <Icon icon="ep:arrow-left" class="mr-5px" /> 上个月
      </el-button>
      <el-date-picker
        :model-value="calendarMonth"
        type="month"
        value-format="YYYY-MM"
        format="YYYY-MM"
        class="!w-160px"
        @update:model-value="(value) => $emit('updateMonth', value)"
      />
      <el-button @click="$emit('moveMonth', 1)">
        下个月 <Icon icon="ep:arrow-right" class="ml-5px" />
      </el-button>
      <el-button type="primary" plain @click="$emit('exportExcel')">
        <Icon icon="ep:download" class="mr-5px" /> 导出已排订单
      </el-button>
    </div>

    <div class="puhui-calendar-head">
      <div v-for="label in weekdayLabels" :key="label" class="puhui-calendar-weekday">
        {{ label }}
      </div>
    </div>

    <div class="puhui-calendar-grid">
      <article
        v-for="(date, index) in calendarCells"
        :key="`${date || 'empty'}-${index}`"
        :class="calendarCellClass(date)"
      >
        <template v-if="date">
          <div class="puhui-calendar-date">{{ date.slice(8) }}</div>
          <el-tag v-if="restReason(date)" type="info" effect="plain" size="small">
            {{ restReason(date) }}
          </el-tag>
          <div class="puhui-calendar-summary">
            <span>{{ isDurationMode ? '总排线 / 产线数' : '总排产 / 产能' }}</span>
            <strong>{{ formatNumber(totalAssigned(date)) }} / {{ formatCapacityNumber(totalCapacity(date)) }}</strong>
          </div>

          <div v-if="!beforeStart(date)" class="puhui-calendar-actions">
            <el-button
              size="small"
              link
              :type="isInSchedule(date) ? 'warning' : 'primary'"
              @click="$emit('setDateWorkMode', date, isInSchedule(date) ? DATE_WORK_MODE.REST : DATE_WORK_MODE.WORK)"
            >
              {{ isInSchedule(date) ? '设为非排产日' : '设为排产日' }}
            </el-button>
            <el-button
              v-if="scenario.dateWorkModeByDate?.[date]"
              size="small"
              link
              @click="$emit('setDateWorkMode', date, null)"
            >
              恢复默认
            </el-button>
          </div>

          <div class="puhui-calendar-lines">
            <span v-if="lineAssignments(date).length === 0" class="puhui-empty">无分配</span>
            <div v-for="line in lineAssignments(date)" :key="line.lineId" class="puhui-calendar-line">
              <div class="puhui-calendar-line-name">{{ line.lineName }}</div>
              <div class="puhui-calendar-orders">
                <button
                  v-for="(order, orderIndex) in line.orders"
                  :key="`${order.orderId}-${orderIndex}`"
                  type="button"
                  :class="['puhui-order-chip', `chip-${order.colorIndex}`]"
                  @click="isDurationMode ? $emit('openFinishModal', order) : undefined"
                >
                  <span>{{ order.text }}</span>
                  <small v-if="order.metaText">{{ order.metaText }}</small>
                  <small v-if="order.manualFinishDate">已结束: {{ order.manualFinishDate }}</small>
                </button>
              </div>
            </div>
          </div>
        </template>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  DATE_WORK_MODE,
  WEEKEND_REST_MODE,
  buildCalendarWeeksByMonth,
  compareDate,
  formatCapacityNumber,
  formatNumber,
  isCnStatutoryHoliday,
  makeLineOrderKey,
  parseIsoDate,
  type LiteSchedulePlan,
  type LiteScenario
} from '../scheduler'

const props = defineProps<{
  plan: LiteSchedulePlan
  calendarMonth: string
  scenario: LiteScenario
  isDurationMode: boolean
  orderMetaMap: Record<string, any>
}>()

defineEmits<{
  updateMonth: [value: string]
  moveMonth: [offset: number]
  exportExcel: []
  setDateWorkMode: [date: string, mode: string | null]
  openFinishModal: [entry: any]
}>()

const weekdayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const calendarCells = computed(() => buildCalendarWeeksByMonth(props.calendarMonth).flat())
const scheduleDateSet = computed(() => new Set(props.plan.dates))

function isInSchedule(date: string) {
  return scheduleDateSet.value.has(date)
}

function beforeStart(date: string) {
  return compareDate(date, props.scenario.horizonStart) < 0
}

function weekday(date: string) {
  const parsed = parseIsoDate(date)
  return parsed ? new Date(parsed.ts).getUTCDay() : -1
}

function restReason(date: string) {
  if (isInSchedule(date)) {
    return ''
  }
  const weekDay = weekday(date)
  if (props.scenario.skipStatutoryHolidays && isCnStatutoryHoliday(date)) {
    return '法定节假日'
  }
  if (
    props.scenario.skipStatutoryHolidays &&
    (props.scenario.weekendRestMode === WEEKEND_REST_MODE.DOUBLE
      ? weekDay === 0 || weekDay === 6
      : props.scenario.weekendRestMode === WEEKEND_REST_MODE.SINGLE
        ? weekDay === 0
        : false)
  ) {
    return '周末休息'
  }
  return ''
}

function calendarCellClass(date: string | null) {
  if (!date) {
    return 'puhui-calendar-cell is-empty'
  }
  return [
    'puhui-calendar-cell',
    isInSchedule(date) ? 'is-active' : 'is-out',
    restReason(date) ? 'is-rest' : '',
    props.scenario.dateWorkModeByDate?.[date] ? 'is-manual' : ''
  ].filter(Boolean)
}

function totalAssigned(date: string) {
  return props.plan.lineRows.reduce((sum, line) => sum + (line.daily[date]?.assigned || 0), 0)
}

function totalCapacity(date: string) {
  return props.plan.lineRows.reduce((sum, line) => sum + (line.daily[date]?.capacity || 0), 0)
}

function colorIndex(lineId: string, orderId: string) {
  const key = makeLineOrderKey(lineId, orderId)
  let hash = 0
  for (let idx = 0; idx < key.length; idx += 1) {
    hash = (hash * 31 + key.charCodeAt(idx)) % 997
  }
  return hash % 6
}

function lineAssignments(date: string) {
  if (!isInSchedule(date)) {
    return []
  }
  return props.plan.lineRows
    .map((line) => {
      const items = line.daily[date]?.items || []
      if (items.length === 0) {
        return null
      }
      return {
        lineId: line.lineId,
        lineName: line.lineName,
        orders: items.map((item) => {
          const orderMeta = props.orderMetaMap[item.orderId] || {}
          const orderNo = orderMeta.orderNo || item.orderId
          const productName = String(orderMeta.productName || '').trim()
          const label = productName ? `${orderNo}/${productName}` : orderNo
          const spec = String(orderMeta.spec || '').trim()
          const batchNo = String(orderMeta.batchNo || '').trim()
          const metaParts: string[] = []
          if (spec) {
            metaParts.push(`规格:${spec}`)
          }
          if (batchNo) {
            metaParts.push(`批号:${batchNo}`)
          }
          const linePlanQty = Math.max(0, Math.round(Number(orderMeta.linePlanQuantities?.[line.lineId] || 0)))
          const lineWorkloadQty = Math.max(0, Number(orderMeta.lineWorkloads?.[line.lineId] || 0))
          const lineTotalQty = props.isDurationMode ? linePlanQty : lineWorkloadQty
          metaParts.push(`数量:${formatNumber(lineTotalQty)}`)
          const lineOrderKey = makeLineOrderKey(line.lineId, item.orderId)
          const manualFinishDate = props.scenario.manualFinishByLineOrder?.[lineOrderKey] || ''
          return {
            lineId: line.lineId,
            lineName: line.lineName,
            orderId: item.orderId,
            orderLabel: label,
            text: props.isDurationMode ? label : `${label}(${formatNumber(item.workloadDays)})`,
            metaText: metaParts.join(' '),
            segmentStartDate: item.segmentStartDate || date,
            manualFinishDate,
            colorIndex: colorIndex(line.lineId, item.orderId)
          }
        })
      }
    })
    .filter(Boolean) as any[]
}
</script>

<style scoped>
.puhui-calendar {
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.puhui-calendar-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #dbe3ef;
  background: #fafcff;
}

.puhui-calendar-head,
.puhui-calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
}

.puhui-calendar-weekday {
  padding: 10px;
  background: #f7f9fc;
  border-right: 1px solid #edf1f6;
  color: #263247;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
}

.puhui-calendar-cell {
  min-height: 172px;
  padding: 10px;
  border-top: 1px solid #edf1f6;
  border-right: 1px solid #edf1f6;
  background: #fff;
}

.puhui-calendar-cell.is-empty {
  background: #f7f9fc;
}

.puhui-calendar-cell.is-out {
  background: #fbfcfe;
  color: #8b95a5;
}

.puhui-calendar-cell.is-rest {
  background: #f8fafc;
}

.puhui-calendar-cell.is-manual {
  box-shadow: inset 3px 0 0 #1677ff;
}

.puhui-calendar-date {
  color: #172033;
  font-weight: 600;
  line-height: 20px;
}

.puhui-calendar-summary {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
}

.puhui-calendar-summary strong {
  color: #172033;
  font-variant-numeric: tabular-nums;
}

.puhui-calendar-actions {
  display: flex;
  gap: 8px;
  margin-top: 6px;
}

.puhui-calendar-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.puhui-empty {
  color: #8b95a5;
  font-size: 12px;
}

.puhui-calendar-line-name {
  margin-bottom: 4px;
  color: #263247;
  font-size: 12px;
  font-weight: 600;
}

.puhui-calendar-orders {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.puhui-order-chip {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  min-height: 34px;
  padding: 5px 7px;
  border: 1px solid #dbe3ef;
  border-left-width: 4px;
  border-radius: 6px;
  background: #fff;
  color: #172033;
  font-size: 12px;
  line-height: 1.35;
  text-align: left;
}

.puhui-order-chip small {
  color: #4b5563;
}

.chip-0 {
  border-left-color: #1677ff;
}

.chip-1 {
  border-left-color: #16a34a;
}

.chip-2 {
  border-left-color: #ea580c;
}

.chip-3 {
  border-left-color: #9333ea;
}

.chip-4 {
  border-left-color: #0891b2;
}

.chip-5 {
  border-left-color: #dc2626;
}

@media (max-width: 960px) {
  .puhui-calendar-head,
  .puhui-calendar-grid {
    grid-template-columns: repeat(7, minmax(96px, 1fr));
    overflow-x: auto;
  }

  .puhui-calendar {
    overflow-x: auto;
  }
}
</style>
