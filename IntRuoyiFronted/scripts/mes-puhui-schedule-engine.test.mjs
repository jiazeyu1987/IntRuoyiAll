import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import vm from 'node:vm'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const repoRoot = resolve(import.meta.dirname, '..')
const schedulerPath = resolve(repoRoot, 'src/views/mes/pro/puhui-schedule/scheduler.ts')
assert.ok(existsSync(schedulerPath), 'missing scheduler.ts')

const source = readFileSync(schedulerPath, 'utf8')
const compiled = ts.transpileModule(source, {
  compilerOptions: {
    module: ts.ModuleKind.CommonJS,
    target: ts.ScriptTarget.ES2020,
    esModuleInterop: true
  }
}).outputText

const module = { exports: {} }
vm.runInNewContext(compiled, {
  module,
  exports: module.exports,
  require,
  console,
  Date,
  Math,
  Number,
  String,
  Object,
  Array,
  Set,
  Map,
  JSON,
  Blob: class Blob {},
  URL
})

const {
  PLANNING_MODE,
  WEEKEND_REST_MODE,
  DATE_WORK_MODE,
  createDefaultLiteScenario,
  buildLiteSchedule,
  buildDateRange,
  advanceLiteScenarioOneDay,
  buildScheduledOrdersExportPayload,
  collectScheduledRows,
  makeLineOrderKey
} = module.exports

const baseScenario = {
  ...createDefaultLiteScenario('2026-01-05'),
  horizonDays: 3,
  lines: [{ id: 'line-1', name: '导管产线', baseCapacity: 10, capacityOverrides: {}, enabled: true }],
  orders: [
    {
      id: 'order-1',
      orderNo: 'PO-0001',
      productName: '产品A',
      spec: 'S1',
      batchNo: 'B1',
      orderSeq: 1,
      workloadDays: 10,
      completedDays: 0,
      dueDate: '2026-01-06',
      releaseDate: '2026-01-05',
      priority: 'NORMAL',
      lineWorkloads: { 'line-1': 10 },
      linePlanDays: {},
      linePlanQuantities: {}
    },
    {
      id: 'order-2',
      orderNo: 'PO-0002',
      productName: '产品B',
      spec: '',
      batchNo: '',
      orderSeq: 2,
      workloadDays: 15,
      completedDays: 0,
      dueDate: '2026-01-10',
      releaseDate: '2026-01-05',
      priority: 'NORMAL',
      lineWorkloads: { 'line-1': 15 },
      linePlanDays: {},
      linePlanQuantities: {}
    }
  ]
}

const quantityPlan = buildLiteSchedule(baseScenario)
assert.equal(quantityPlan.summary.totalAssigned, 25)
assert.equal(quantityPlan.orderRows[0].completionDate, '2026-01-05')
assert.equal(quantityPlan.orderRows[1].completionDate, '2026-01-07')
assert.equal(quantityPlan.lineRows[0].daily['2026-01-05'].assigned, 10)

const skipped = buildDateRange('2026-01-01', 2, true, WEEKEND_REST_MODE.DOUBLE, {})
assert.deepEqual(Array.from(skipped), ['2026-01-05', '2026-01-06'])
const overridden = buildDateRange('2026-01-01', 2, true, WEEKEND_REST_MODE.DOUBLE, {
  '2026-01-01': DATE_WORK_MODE.WORK
})
assert.equal(overridden[0], '2026-01-01')

const durationPlan = buildLiteSchedule({
  ...baseScenario,
  planningMode: PLANNING_MODE.DURATION_MANUAL_FINISH,
  manualFinishByLineOrder: { [makeLineOrderKey('line-1', 'order-1')]: '2026-01-05' },
  orders: [
    {
      ...baseScenario.orders[0],
      workloadDays: 2,
      lineWorkloads: {},
      linePlanDays: { 'line-1': 2 },
      linePlanQuantities: { 'line-1': 100 }
    },
    {
      ...baseScenario.orders[1],
      workloadDays: 1,
      lineWorkloads: {},
      linePlanDays: { 'line-1': 1 },
      linePlanQuantities: { 'line-1': 50 }
    }
  ]
})
assert.equal(durationPlan.orderRows[0].finishStatus, '已结束')
assert.equal(durationPlan.orderRows[0].actualFinishDate, '2026-01-05')
assert.equal(durationPlan.orderRows[1].completionDate, '2026-01-06')

const advanced = advanceLiteScenarioOneDay(baseScenario)
assert.equal(advanced.nextScenario.horizonStart, '2026-01-06')
assert.equal(advanced.daySummary.completedWorkload, 10)
assert.equal(advanced.nextScenario.orders[0].completedDays, 10)

const scheduledRows = collectScheduledRows({
  allocations: quantityPlan.allocations,
  compareDate: module.exports.compareDate,
  lineNameMap: { 'line-1': '导管产线' },
  orderMetaMap: { 'order-1': { orderNo: 'PO-0001' }, 'order-2': { orderNo: 'PO-0002' } }
})
const payload = buildScheduledOrdersExportPayload({
  scheduledRows,
  orderMetaMap: { 'order-1': { orderNo: 'PO-0001' }, 'order-2': { orderNo: 'PO-0002' } },
  lineNameMap: { 'line-1': '导管产线' },
  manualFinishByLineOrder: {},
  makeLineOrderKey,
  isDurationMode: false,
  stamp: '2026-01-05'
})
assert.equal(payload.rows.length, 3)
assert.equal(payload.fileName, 'lite排产订单_2026-01-05.xls')

console.log('mes-puhui-schedule engine behavior passed')
