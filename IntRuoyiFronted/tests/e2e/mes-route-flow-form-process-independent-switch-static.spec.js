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
  'syncGlobalRecordBindingGroupFromSource',
  'getOrCreateRouteProcessAttributeDraft'
]) {
  assert.ok(component.includes(expected), `工序独立开关必须具备显式全局组联动 helper: ${expected}`)
}

assert.match(
  component,
  /handleRecordBindingProcessIndependentChange[\s\S]*applyRecordBindingInstanceScope\(binding, processIndependent \? 'PROCESS' : 'BATCH_SHARED'\)[\s\S]*syncGlobalRecordBindingGroupFromSource\(binding\)/,
  '切换开关时仅显式 globalSyncKey 组联动；未开启全局的同模板表单必须保持独立。'
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
