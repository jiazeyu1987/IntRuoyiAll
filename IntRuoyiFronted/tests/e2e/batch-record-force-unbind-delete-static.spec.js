const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/index.vue')
const apiPath = path.resolve(__dirname, '../../src/api/mes/pro/batchrecordreport/index.ts')

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')

const removedPageSnippets = [
  ['batch delete button label', '批量删除'],
  ['batch delete click binding', '@click="handleBatchDelete"'],
  ['selection column', 'type="selection"'],
  ['selection state handler', '@selection-change="handleSelectionChange"'],
  ['batch unbind confirmation title', '批量解绑后删除'],
  ['user-facing unbind explanation', '选中的批记录表单中存在已绑定工艺路线工序的报表，是否批量解绑后删除？'],
  ['force delete invocation', 'await deleteSelectedReports(candidates, true)'],
  ['force delete success message', '已批量解绑并删除'],
  ['batch delete handler', 'const handleBatchDelete = async'],
  ['batch delete selected rows state', 'selectedRows'],
  ['batch delete loading state', 'batchDeleteLoading'],
  ['bound error detection helper', 'isRouteProcessBoundDeleteError']
]

for (const [label, snippet] of removedPageSnippets) {
  assert.ok(!pageSource.includes(snippet), `Batch-record form list must remove obsolete batch-delete UI: ${label}`)
}

const retainedApiSnippets = [
  ['forceUnbind batch request field', 'forceUnbind?: boolean'],
  ['route process unbind response field', 'unboundRouteProcessCount?: number'],
  ['route flow config unbind response field', 'unboundRouteFlowProcessConfigCount?: number'],
  ['route flow binding delete response field', 'deletedRouteFlowBindingCount?: number']
]

for (const [label, snippet] of retainedApiSnippets) {
  assert.ok(apiSource.includes(snippet), `Batch delete API contract should remain available for backend compatibility: ${label}`)
}

assert.ok(!pageSource.includes('catch {}'), 'Delete flows must not silently swallow errors.')

console.log('PASS: batch-record batch delete removal static contract')
