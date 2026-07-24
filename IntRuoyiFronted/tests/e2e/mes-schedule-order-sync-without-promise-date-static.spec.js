const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')

assert(fs.existsSync(pagePath), 'Schedule order page must exist.')
assert(fs.existsSync(apiPath), 'Schedule order API module must exist.')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const admissionBarStart = pageSource.indexOf('schedule-order-pool__admission-bar')
const admissionBarEnd = pageSource.indexOf('schedule-order-pool__admission-table-shell')
assert(admissionBarStart >= 0, 'Work order admission action bar must exist.')
assert(admissionBarEnd > admissionBarStart, 'Work order admission action bar block must be bounded.')
const admissionDialogSource = pageSource.slice(admissionBarStart, admissionBarEnd)

assert(
  !admissionDialogSource.includes('label="承诺交期"'),
  'Sync work-order dialog must not render a promised-date field.'
)
assert(
  !admissionDialogSource.includes('workOrderAdmissionPromiseDate'),
  'Sync work-order dialog must not bind promised-date state.'
)

const admissionSubmitStart = pageSource.indexOf('const submitWorkOrderAdmission')
const admissionSubmitEnd = pageSource.indexOf('const openReplanDrawer')
assert(admissionSubmitStart >= 0 && admissionSubmitEnd > admissionSubmitStart, 'Admission submit handler must exist.')
const admissionSubmitSource = pageSource.slice(admissionSubmitStart, admissionSubmitEnd)

assert(
  !admissionSubmitSource.includes('请填写承诺交期'),
  'Sync work-order submit must not block when promised date is empty.'
)
assert(
  !admissionSubmitSource.includes('workOrderAdmissionPromiseDate'),
  'Sync work-order submit must not read promised-date state.'
)
assert(
  /createFromWorkOrders\(\{\s*workOrderIds:\s*rows\.map\(\(workOrder\) => workOrder\.workOrderId\)\s*\}\)/.test(
    admissionSubmitSource
  ),
  'Sync work-order submit payload must only include workOrderIds.'
)

const batchReqMatch = apiSource.match(/export interface MesProScheduleOrderCreateFromWorkOrdersReqVO \{[\s\S]*?\n\}/)
assert(batchReqMatch, 'Batch create request type must exist.')
assert(
  !batchReqMatch[0].includes('promiseDate:'),
  'Batch create request type must not require promiseDate.'
)

const singleReqMatch = apiSource.match(/export interface MesProScheduleOrderCreateFromWorkOrderReqVO \{[\s\S]*?\n\}/)
assert(singleReqMatch, 'Single create request type must exist.')
assert(
  singleReqMatch[0].includes('promiseDate?: string'),
  'Single create request type must mark promiseDate optional.'
)

assert(pageSource.includes('openPromiseDateDialog'), 'Existing promise-date maintenance dialog must be preserved.')
assert(pageSource.includes('submitPromiseDateReset'), 'Existing promise-date submit flow must be preserved.')

console.log('PASS: MES schedule order sync without promise date static contract')
