const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const dccPresentation = readSource('src/views/dcc/controlled-file/detail/presentation.ts')
const dccDetail = readSource('src/views/dcc/controlled-file/detail/index.vue')
const edhrList = readSource('src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const edhrDetail = readSource('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const scheduleOrder = readSource('src/views/mes/pro/scheduleorder/index.vue')

for (const [name, source] of [
  ['DCC controlled file detail presentation', dccPresentation],
  ['DCC controlled file detail page', dccDetail],
  ['eDHR batch execution list', edhrList],
  ['eDHR batch execution detail', edhrDetail],
  ['MES schedule order page', scheduleOrder]
]) {
  assert.ok(
    source.includes('@/api/form-center/actionProjection') ||
      source.includes('resolveControlledActionProjection'),
    `${name} must consume the shared form-center action projection helper.`
  )
}

assert.ok(
  dccPresentation.includes('resolveDccDetailActionProjection'),
  'DCC detail projection must convert backend action flags into shared projection states.'
)
assert.ok(
  dccDetail.includes('detailActionProjectionMessages'),
  'DCC detail page must surface projection blocker messages instead of silently hiding actions.'
)

assert.ok(
  edhrList.includes('resolveEdhrTaskActionProjection'),
  'eDHR list task actions must convert backend allowedActions into shared projection states.'
)
assert.ok(
  edhrDetail.includes('resolveEdhrBatchActionProjection'),
  'eDHR detail batch actions must convert backend pending/release/void locks into shared projection states.'
)
assert.ok(
  edhrDetail.includes('edhrReleaseActionProjection') &&
    edhrDetail.includes('edhrVoidActionProjection'),
  'eDHR detail page must expose release and void projection states.'
)

assert.ok(
  scheduleOrder.includes('resolveScheduleReplanProjection'),
  'Schedule order replan action must expose a shared projection state.'
)
assert.ok(
  scheduleOrder.includes('replanProjectionState') &&
    scheduleOrder.includes('replanProjectionState.value.blockerMessage'),
  'Schedule order replan action must display the shared projection blocker reason.'
)
assert.ok(
  scheduleOrder.includes('scheduleReplanActionProjection'),
  'Schedule order page must keep a stable projection alias for contract tests and callers.'
)

assert.doesNotMatch(
  scheduleOrder,
  /catch\s*\{\s*return false\s*\}/,
  'Schedule replan confirmation must not silently swallow a user-visible failure path.'
)

console.log('form-center official page projection static contract passed')
