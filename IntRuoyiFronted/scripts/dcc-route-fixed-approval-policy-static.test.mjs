import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('BDD: DCC route form renders fixed approval policy instead of editable unsupported controls', () => {
  const source = readText('src/views/dcc/controlled-file/routes/components/RouteForm.vue')

  assert.match(source, /FIXED_ROUTE_APPROVAL_POLICY/)
  assert.match(source, /getFixedRouteApprovalPolicy/)
  assert.match(source, /固定四阶段审批策略/)
  assert.doesNotMatch(source, /v-model="row\.approveMethod"/)
  assert.doesNotMatch(source, /v-model="row\.approveRatio"/)
  assert.doesNotMatch(source, /v-model="row\.required"/)
  assert.doesNotMatch(source, /handleApproveMethodChange/)
})

test('BDD: DCC route save payload uses only the fixed BPMN policy fields', () => {
  const source = readText('src/views/dcc/controlled-file/routes/components/RouteForm.vue')

  assert.match(source, /normalizeRouteNodeFixedApprovalPolicy/)
  assert.match(source, /approveMethod:\s*fixedPolicy\.approveMethod/)
  assert.match(source, /approveRatio:\s*fixedPolicy\.approveRatio\s*\?\?\s*undefined/)
  assert.match(source, /required:\s*fixedPolicy\.required/)
})
