const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

for (const expected of [
  'applyRecordBindingFillerOverride',
  'applyRouteWideRecordBindingFillerByTemplate',
  'syncRouteWideRecordBindingFillerByTemplate'
]) {
  assert.ok(component.includes(expected), `共享表单填写人必须具备路线内联动 helper: ${expected}`)
}

assert.match(
  component,
  /const applyRecordBindingFillerOverride[\s\S]*candidateSourceType[\s\S]*candidateSourceIds[\s\S]*candidateSourceNames/,
  '填写人覆盖必须通过统一 helper 同步来源、ID 和显示名。'
)

assert.match(
  component,
  /const applyRouteWideRecordBindingFillerByTemplate[\s\S]*Number\(binding\.formTemplateId \|\| 0\) === formTemplateId[\s\S]*isBatchSharedBinding\(binding\)[\s\S]*applyRecordBindingFillerOverride\(binding, filler\)/,
  '路线内填写人同步必须只覆盖同 formTemplateId 且仍为批次共享的表单绑定。'
)

assert.match(
  component,
  /const syncRouteWideRecordBindingFillerByTemplate[\s\S]*routeNodes\.value\.forEach[\s\S]*getOrCreateRouteProcessAttributeDraft\(node\.routeProcessId\)[\s\S]*applyRouteWideRecordBindingFillerByTemplate\(draft\.recordBindings, formTemplateId, filler\)/,
  '共享填写人联动必须覆盖尚未打开过详情的所有工序草稿。'
)

assert.match(
  component,
  /handleSelectedRecordBindingCandidateSourceTypeChange[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncRouteWideRecordBindingFillerByTemplate\(binding\)/,
  '更换填写人来源时必须同步同路线同表单的共享绑定。'
)

assert.match(
  component,
  /handleSelectedRecordBindingCandidateIdChange[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncRouteWideRecordBindingFillerByTemplate\(binding\)/,
  '更换填写人时必须同步同路线同表单的共享绑定。'
)

assert.match(
  component,
  /clearSelectedRecordBindingFillerOverride[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncRouteWideRecordBindingFillerByTemplate\(binding\)/,
  '恢复默认填写人时必须同步同路线同表单的共享绑定。'
)

console.log('mes-route-flow-shared-form-filler-sync-static PASS')
