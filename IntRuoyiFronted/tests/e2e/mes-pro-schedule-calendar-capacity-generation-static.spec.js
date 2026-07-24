const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleCalendar/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/task/calendar/index.vue')

assert(fs.existsSync(apiPath), 'Schedule calendar API module must exist.')
assert(fs.existsSync(pagePath), 'Schedule calendar page must exist.')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  'ProScheduleCalendarCapacityGenerateReqVO',
  'ProScheduleCalendarCapacityGenerateRespVO',
  'generateCapacityPlans',
  '/mes/pro/schedule-calendar/capacity/generate',
  'generatedCount',
  'skippedExistingCount',
  'skippedRestCount',
  'skippedNoShiftCount'
]) {
  assert(apiSource.includes(token), `Schedule calendar API must expose ${token}.`)
}

for (const token of [
  '产能生成',
  '生成未来产能',
  'capacityGenerateDays',
  'capacityGenerateLoading',
  'capacityGenerateSummary',
  'capacityGenerateSummaryText',
  'handleGenerateCapacityPlans',
  'ProScheduleCalendarApi.generateCapacityPlans'
]) {
  assert(pageSource.includes(token), `Schedule calendar page must render or handle ${token}.`)
}

assert(
  /v-model="capacityGenerateDays"[\s\S]*?:min="1"[\s\S]*?:max="366"/.test(pageSource),
  'Capacity generation days input must constrain the request range to 1-366 days.'
)
assert(
  /:loading="capacityGenerateLoading"[\s\S]*@click="handleGenerateCapacityPlans"/.test(pageSource),
  'Capacity generation button must bind loading state and explicit click handler.'
)
assert(
  /startDate:\s*simulationDateLabel\.value[\s\S]*days:\s*capacityGenerateDays\.value/.test(pageSource),
  'Capacity generation request must start from the visible simulation date and use the operator-selected day count.'
)
assert(
  !/catch\s*\{\s*\}/.test(pageSource),
  'Schedule calendar capacity generation must not silently swallow frontend errors.'
)
assert(pageSource.includes('capacityGenerateSeverity'), 'Capacity generation must distinguish warning and success feedback.')
assert(pageSource.includes('未生成产能'), 'Capacity generation must explain zero-generation results instead of showing a plain success.')
assert(pageSource.includes('去排产工单预览'), 'Calendar auto-schedule entry must clarify it navigates to schedule orders for preview.')
assert(pageSource.includes('去排产工单发布'), 'Calendar auto-schedule entry must clarify it navigates to schedule orders for publish.')

console.log('PASS: MES schedule calendar capacity generation static contract')
