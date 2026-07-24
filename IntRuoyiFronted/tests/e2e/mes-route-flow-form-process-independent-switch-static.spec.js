const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

assert.ok(
  component.includes('data-route-process-setting-field="process-independent-switch"'),
  '每个动态表单槽位必须渲染工序独立开关。'
)
assert.ok(component.includes('工序独立'), '工序独立开关必须使用明确中文标签。')
assert.match(
  component,
  /:model-value="isRecordBindingProcessIndependent\(binding\)"[\s\S]*@change="\(value\) => handleRecordBindingProcessIndependentChange\(binding, Boolean\(value\)\)"/,
  '工序独立开关必须由 instanceScope 派生展示，并通过专用处理器切换。'
)

for (const expected of [
  'isRecordBindingProcessIndependent',
  'handleRecordBindingProcessIndependentChange',
  'applyRecordBindingProcessIndependentByTemplate',
  'syncRouteWideRecordBindingProcessIndependent',
  'getOrCreateRouteProcessAttributeDraft'
]) {
  assert.ok(component.includes(expected), `工序独立开关必须具备同路线同表单联动 helper: ${expected}`)
}

assert.match(
  component,
  /handleRecordBindingProcessIndependentChange[\s\S]*const formTemplateId = Number\(binding\.formTemplateId \|\| 0\)[\s\S]*syncRouteWideRecordBindingProcessIndependent\(formTemplateId, processIndependent\)/,
  '切换开关时必须以 formTemplateId 为同表单身份触发路线内联动。'
)
assert.match(
  component,
  /syncRouteWideRecordBindingProcessIndependent[\s\S]*routeNodes\.value\.forEach[\s\S]*getOrCreateRouteProcessAttributeDraft\(node\.routeProcessId\)[\s\S]*applyRecordBindingProcessIndependentByTemplate\(draft\.recordBindings, formTemplateId, processIndependent\)/,
  '路线内联动必须覆盖所有工序草稿，包括尚未打开过详情的工序。'
)
assert.match(
  component,
  /applyRecordBindingProcessIndependentByTemplate[\s\S]*Number\(binding\.formTemplateId \|\| 0\) === formTemplateId[\s\S]*applyRecordBindingInstanceScope\(binding, instanceScope\)/,
  '联动必须只按相同 formTemplateId 更新，不得影响其他表单。'
)
assert.match(
  component,
  /const instanceScope = normalizeRecordBindingInstanceScope\(binding\.instanceScope\)[\s\S]*instanceScope: instanceScope[\s\S]*sharedFormKey: instanceScope === 'BATCH_SHARED' \? buildSharedRecordBindingKey\(binding\) : null[\s\S]*fillableScopeJson:[\s\S]*instanceScope === 'BATCH_SHARED' \? SHARED_FORM_FILLABLE_SCOPE_JSON : null/,
  '保存 payload 必须按开关写入 PROCESS/BATCH_SHARED，并只在共享模式写入 sharedFormKey 与填写范围。'
)
assert.match(
  component,
  /const validateBatchSharedRecordBinding = \(binding: RouteFlowRecordBinding\) => \{[\s\S]*if \(!isBatchSharedBinding\(binding\)\) return/,
  '共享配置校验只能约束 BATCH_SHARED，PROCESS 工序独立表单不能被共享 key 或填写范围阻断。'
)

console.log('mes-route-flow-form-process-independent-switch-static PASS')
