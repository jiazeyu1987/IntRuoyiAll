import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('auto schedule preview api exposes work-order line analysis payload', () => {
  const source = readText('src/api/mes/pro/task/autoSchedule/index.ts')

  assert.match(source, /ProScheduleCalendarWorkOrderAnalysisVO/)
  assert.match(source, /workOrderAnalyses:\s*[A-Za-z0-9_]+\[\]/)
})

test('schedule calendar api exposes current schedule work-order analysis query', () => {
  const source = readText('src/api/mes/pro/scheduleCalendar/index.ts')

  assert.match(source, /export interface ProScheduleCalendarWorkOrderAnalysisVO/)
  assert.match(source, /conflict\??:\s*boolean/)
  assert.match(source, /getWorkOrderAnalysis:\s*async/)
  assert.match(source, /url:\s*'\/mes\/pro\/schedule-calendar\/work-order-analysis'/)
})

test('schedule calendar opens analysis panel from work-order clicks instead of direct navigation', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /workOrderAnalysisVisible = ref\(false\)/)
  assert.match(source, /openWorkOrderAnalysis\(/)
  assert.match(source, /@click="openWorkOrderAnalysis\(task\.workOrderId, task\.workOrderCode\)"/)
  assert.match(source, /workOrderAnalysisDialogTitle|工单产线分析/)
  assert.match(source, /查看工单主数据/)
  assert.doesNotMatch(source, /@click="openWorkOrderDetail\(task\.workOrderId, task\.workOrderCode\)"/)
})

test('schedule calendar reads preview analysis locally and current schedule analysis remotely', () => {
  const source = readText('src/views/mes/pro/task/calendar/index.vue')

  assert.match(source, /autoSchedulePreview\.value\?\.workOrderAnalyses/)
  assert.match(source, /ProScheduleCalendarApi\.getWorkOrderAnalysis\(/)
  assert.match(source, /workOrderAnalysisErrorMessage/)
  assert.match(source, /conflictMessage/)
})
