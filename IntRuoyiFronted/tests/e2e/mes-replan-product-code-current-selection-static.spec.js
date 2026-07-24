const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
assert(fs.existsSync(pagePath), 'Schedule order pool page must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')

const participantStateStart = pageSource.indexOf('const lastReplanParticipatingScheduleOrderIds')
const participantStateEnd = pageSource.indexOf('const getLastReplanParticipatingScheduleOrderIdSet')
const participantResolverStart = pageSource.indexOf(
  'const resolveReplanParticipatingScheduleOrderIds'
)
const participantResolverEnd = pageSource.indexOf(
  'const isScheduleOrderParticipatingInLastReplan',
  participantResolverStart
)
const resolverStart = pageSource.indexOf('const getScheduleOrderProductCodeClass')
const resolverEnd = pageSource.indexOf('const isScheduleOrderSelectable', resolverStart)
const applyStart = pageSource.indexOf('const confirmApplyReplanStartChoice')
const applyEnd = pageSource.indexOf('const openDailyCompareDialog', applyStart)

assert(
  participantStateStart >= 0 && participantStateEnd > participantStateStart,
  'Schedule order page must keep the latest actual replan participant state.'
)
assert(
  participantResolverStart >= 0 && participantResolverEnd > participantResolverStart,
  'Schedule order page must resolve actual replan participants from preview/apply results.'
)
assert(
  resolverStart >= 0 && resolverEnd > resolverStart,
  'Schedule order page must keep the main product code class resolver.'
)
assert(applyStart >= 0 && applyEnd > applyStart, 'Schedule order apply flow must exist.')

const participantStateSource = pageSource.slice(participantStateStart, participantStateEnd)
const participantResolverSource = pageSource.slice(participantResolverStart, participantResolverEnd)
const resolverSource = pageSource.slice(resolverStart, resolverEnd)
const applySource = pageSource.slice(applyStart, applyEnd)

assert(
  participantStateSource.includes('ref<number[]>([])'),
  'Actual participant state must start empty so stale orange rows do not appear before a real replan.'
)
assert(
  participantResolverSource.includes('preview.tasks') &&
    participantResolverSource.includes('preview.workOrderAnalyses') &&
    participantResolverSource.includes('preview.protectedTasks'),
  'Actual participants must be resolved from generated tasks, analyses, and protected tasks, not from selected rows only.'
)
assert(
  resolverSource.includes('isScheduleOrderParticipatingInLastReplan'),
  'Main product code orange state must be derived from latest actual replan participants.'
)
assert(
  !resolverSource.includes('isScheduleOrderParticipating(row)') &&
    !resolverSource.includes('SCHEDULE_ORDER_PARTICIPATING_STATUSES') &&
    !resolverSource.includes('selectedScheduleOrderIdSet') &&
    !resolverSource.includes('isScheduleOrderSelectedForReplan'),
  'Main product code orange state must not be derived from schedule status, historical selection, or current checkbox selection.'
)
assert(
  applySource.includes('updateLastReplanParticipatingScheduleOrders(freshPreview)') &&
    applySource.indexOf('updateLastReplanParticipatingScheduleOrders(freshPreview)') >
      applySource.indexOf('await ProTaskAutoScheduleApi.replanApply'),
  'Apply success must replace the latest participant set after the real replan apply call.'
)

console.log('PASS: MES schedule order product code actual replan participant highlight static contract')
