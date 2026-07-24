const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

assert(
  source.includes('建议先预检') &&
    source.includes('openCreateRouteReadinessDialog') &&
    source.includes('@click="openCreateRouteReadinessDialog"'),
  'Open/create dialog must expose a visible readiness hint action before creating a batch.'
)

assert(
  source.includes('createRouteReadinessHintVisible') &&
    source.includes('createForm.routeId') &&
    source.includes('parseOptionalPositiveNumber(createForm.routeId, \'路线ID\')'),
  'Open/create dialog readiness hint must be driven by the current routeId field.'
)

assert(
  source.includes('readinessForm.routeId = String(routeId)') &&
    source.includes('createDialogVisible.value = false') &&
    source.includes('loadReadinessUsers()'),
  'Open/create readiness action must reuse the current routeId and open the formal readiness dialog.'
)

assert(
  source.includes('请输入路线ID后再预检') &&
    source.includes('createError.value'),
  'Open/create readiness action must fail visibly when routeId is missing or invalid.'
)

console.log('PASS: eDHR open/create readiness hint static contract')
