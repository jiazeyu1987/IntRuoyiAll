const { readFileSync } = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const ts = require('typescript')

const source = readFileSync(path.resolve(__dirname,
  '../../src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8').replace(/\r\n/g, '\n')
const queryBuilder = source.match(/const buildSubmissionParams = [\s\S]*?\n}\n/)[0]
const context = {
  queryParams: { pageNo: 2, pageSize: 20, submissionReviewStatus: 'APPROVED' },
  activeLeaderTab: { value: 'PQC' },
  isPqcFormHistoryTab: { value: false },
  isProductionLeader: { value: false },
  isProductionReportHistoryTab: { value: false },
  resolveCurrentLeaderType: () => context.activeLeaderTab.value,
  normalizePositiveNumber: (value) => Number(value) > 0 ? Number(value) : undefined
}
vm.createContext(context)
vm.runInContext(ts.transpileModule(queryBuilder + '\nglobalThis.build = buildSubmissionParams', {
  compilerOptions: { target: ts.ScriptTarget.ES2022 }
}).outputText, context)

for (const savedStatus of [undefined, 'PENDING', 'APPROVED', 'REJECTED']) {
  context.queryParams.submissionReviewStatus = savedStatus
  const params = context.build()
  assert.equal(params.submissionReviewStatus, 'PENDING', 'Saved filters must not expose reviewed PQC rows')
  assert.equal(params.pqcFormView, 'CURRENT')
  assert.equal(params.pageNo, 2)
  assert.equal(params.pageSize, 20)
}
context.isPqcFormHistoryTab.value = true
assert.equal(context.build().submissionReviewStatus, 'APPROVED')
assert.equal(context.build().pqcFormView, 'HISTORY')
context.isPqcFormHistoryTab.value = false
assert.equal(context.build().submissionReviewStatus, 'PENDING', 'Returning to management restores pending scope')
context.activeLeaderTab.value = 'PRODUCTION'
context.isProductionLeader.value = true
assert.equal(context.build().submissionReviewStatus, 'REJECTED', 'Production filtering remains independent')
assert.equal(context.build().allocationView, 'WORKBENCH')
context.isProductionReportHistoryTab.value = true
assert.equal(context.build().allocationView, 'HISTORY')
console.log('PASS PQC pending query scope, saved filters, pagination and tab switching')
