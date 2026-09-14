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
  'normalizeGlobalFormBindingSyncKey',
  'syncGlobalRecordBindingGroupFromSource'
]) {
  assert.ok(component.includes(expected), `全局表单填写人必须具备显式联动组 helper: ${expected}`)
}

assert.match(
  component,
  /const applyRecordBindingFillerOverride[\s\S]*candidateSourceType[\s\S]*candidateSourceIds[\s\S]*candidateSourceNames/,
  '填写人覆盖必须通过统一 helper 同步来源、ID 和显示名。'
)

assert.match(
  component,
  /const syncGlobalRecordBindingGroupFromSource[\s\S]*normalizeGlobalFormBindingSyncKey\(sourceBinding\.globalSyncKey\)[\s\S]*normalizeGlobalFormBindingSyncKey\(binding\.globalSyncKey\) === globalSyncKey/,
  '路线内填写人同步必须只覆盖相同 globalSyncKey，不得按模板隐式联动。'
)

assert.match(
  component,
  /const getAllRouteProcessAttributeDrafts[\s\S]*routeNodes\.value\.map[\s\S]*getOrCreateRouteProcessAttributeDraft\(node\.routeProcessId\)/,
  '全局填写人联动必须覆盖尚未打开过详情的所有普通工序草稿。'
)

assert.match(
  component,
  /handleSelectedRecordBindingCandidateSourceTypeChange[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncGlobalRecordBindingGroupFromSource\(binding\)/,
  '更换填写人来源时必须同步显式全局组。'
)

assert.match(
  component,
  /handleSelectedRecordBindingCandidateIdChange[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncGlobalRecordBindingGroupFromSource\(binding\)/,
  '更换填写人时必须同步显式全局组。'
)

assert.match(
  component,
  /clearSelectedRecordBindingFillerOverride[\s\S]*applyRecordBindingFillerOverride\(binding,[\s\S]*syncGlobalRecordBindingGroupFromSource\(binding\)/,
  '恢复默认填写人时必须同步显式全局组。'
)

assert.doesNotMatch(
  component,
  /syncRouteWideRecordBindingFillerByTemplate|applyRouteWideRecordBindingFillerByTemplate/,
  '未开启全局的同模板表单不得再自动同步填写人。'
)

console.log('mes-route-flow-shared-form-filler-sync-static PASS')
