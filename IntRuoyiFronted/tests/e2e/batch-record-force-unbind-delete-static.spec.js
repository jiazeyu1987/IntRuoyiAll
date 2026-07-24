const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/index.vue')
const apiPath = path.resolve(__dirname, '../../src/api/mes/pro/batchrecordreport/index.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const requiredPageSnippets = [
  ['bound error detection', 'isRouteProcessBoundDeleteError(error)'],
  ['batch unbind confirmation title', '批量解绑后删除'],
  ['user-facing unbind explanation', '选中的批记录表单中存在已绑定工艺路线工序的报表，是否批量解绑后删除？'],
  ['force delete invocation', 'await deleteSelectedReports(candidates, true)'],
  ['force delete success message', '已批量解绑并删除'],
  ['cancel keeps bound forms without deleting', "confirmError === 'cancel' || confirmError === 'close') return"]
]

const requiredApiSnippets = [
  ['forceUnbind batch request field', 'forceUnbind?: boolean'],
  ['batch delete request body', 'data'],
  ['route process unbind response field', 'unboundRouteProcessCount?: number'],
  ['route flow config unbind response field', 'unboundRouteFlowProcessConfigCount?: number'],
  ['route flow binding delete response field', 'deletedRouteFlowBindingCount?: number']
]

for (const [label, snippet] of requiredPageSnippets) {
  assert.ok(pageSource.includes(snippet), `Missing batch-record delete UI contract: ${label}`)
}

for (const [label, snippet] of requiredApiSnippets) {
  assert.ok(apiSource.includes(snippet), `Missing batch-record delete API contract: ${label}`)
}

const skippedBoundBranchIndex = pageSource.indexOf('isRouteProcessBoundDeleteError(error)')
const forceDeleteCallIndex = pageSource.indexOf(
  'await deleteSelectedReports(candidates, true)',
  skippedBoundBranchIndex + 1
)

assert.ok(forceDeleteCallIndex > skippedBoundBranchIndex, 'Force delete call must run after bound-template detection.')
assert.ok(
  skippedBoundBranchIndex < forceDeleteCallIndex,
  'Force delete must only run after detecting skipped bound templates and second confirmation.'
)

assert.ok(!pageSource.includes('catch {}'), 'Delete flow must not silently swallow errors.')

console.log('PASS: batch-record force unbind delete static contract')
