const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const routeForm = readSource('src/views/dcc/controlled-file/routes/components/RouteForm.vue')
const normalizedRouteForm = routeForm.replace(/\r\n/g, '\n')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const submitForm = extractBetween(normalizedRouteForm, 'const submitForm = async () => {', '\n</script>')
assert.ok(submitForm, 'RouteForm submitForm must remain inspectable for DCC-STATIC-019')

const uniquenessGuard = extractBetween(
  normalizedRouteForm,
  "const validateFixedRouteStageUniqueness = (nodes: ControlledFileApprovalRouteSaveReqVO['nodes']) => {",
  '\n\nconst submitForm'
)

assert.match(
  uniquenessGuard,
  /new Set<number>\(\)/,
  'The front-end fixed route guard must track stage numbers explicitly'
)
assert.match(
  uniquenessGuard,
  /expectedStageNos/,
  'The front-end fixed route guard must require the exact four fixed stages'
)
assert.match(
  uniquenessGuard,
  /固定四阶段审批路线要求每个阶段恰好一条/,
  'Duplicate or missing fixed route stages must show an explicit user-facing rejection'
)
assert.match(
  uniquenessGuard,
  /seenStageNos\.has\(node\.stageNo\)/,
  'The front-end fixed route guard must reject duplicate stage numbers before save'
)

assert.ok(
  submitForm.indexOf('validateFixedRouteStageUniqueness(fixedNodes)') >= 0,
  'submitForm must call the fixed route uniqueness guard before saving'
)
assert.ok(
  submitForm.indexOf('validateFixedRouteStageUniqueness(fixedNodes)') < submitForm.indexOf('saveApprovalRoute('),
  'The fixed route uniqueness guard must execute before saveApprovalRoute'
)

assert.ok(
  !/fallback|mock|placeholder data|默认成功|吞异常|降级/.test(uniquenessGuard),
  'DCC-STATIC-019 guard must not introduce fallback, mock, default success, swallowed errors, or downgrade behavior'
)

console.log('PASS: DCC-STATIC-019 approval route duplicate stage static contract')
