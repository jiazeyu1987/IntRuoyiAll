const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const template = component

for (const removedUi of [
  'data-route-process-setting-field="shared-form-instance-scope"',
  'data-route-process-setting-field="required-policy"',
  'data-route-process-setting-field="shared-form-key"',
  'data-route-process-setting-field="fillable-scope-json"',
  'label="批次共享表单"',
  'label="必填"',
  '共享 key',
  '填写范围 JSON',
  'placeholder="填写策略"',
  'data-route-process-setting-field="candidate-summary"'
]) {
  assert.ok(!template.includes(removedUi), `共享表单选择后不应继续渲染红框配置项: ${removedUi}`)
}

for (const retainedUi of [
  'data-route-process-setting-field="form-template"',
  'data-route-process-setting-field="process-independent-switch"',
  'data-route-process-setting-field="candidate-source-type"',
  'data-route-process-setting-field="candidate-source-id"'
]) {
  assert.ok(template.includes(retainedUi), `精简共享表单配置时必须保留必要选择控件: ${retainedUi}`)
}

assert.match(
  component,
  /const SHARED_FORM_FILLABLE_SCOPE_JSON\s*=[\s\S]*sourceTableIndex[\s\S]*startRow:\s*0[\s\S]*endRow:\s*99999/,
  '保存共享表单时必须由前端派生全表填写范围 JSON，不再依赖用户手填。'
)
assert.match(
  component,
  /const buildSharedRecordBindingKey[\s\S]*formSlotType[\s\S]*formTemplateId/,
  '保存共享表单时必须由表单槽位和模板 ID 派生稳定共享 key。'
)
assert.match(
  component,
  /createEmptyRecordBinding[\s\S]*instanceScope:\s*'BATCH_SHARED'/,
  '新增表单槽位默认关闭工序独立，继续使用批次共享表单。'
)
assert.match(
  component,
  /const instanceScope = normalizeRecordBindingInstanceScope\(binding\.instanceScope\)[\s\S]*sharedFormKey: instanceScope === 'BATCH_SHARED' \? buildSharedRecordBindingKey\(binding\) : null[\s\S]*fillableScopeJson:[\s\S]*instanceScope === 'BATCH_SHARED' \? SHARED_FORM_FILLABLE_SCOPE_JSON : null[\s\S]*requiredPolicy:\s*'REQUIRED'/,
  '保存 payload 必须按开关写入实例范围，且仅共享模式派生共享 key 和全表填写范围。'
)

console.log('PASS: MES route flow shared form simplified static contract')
