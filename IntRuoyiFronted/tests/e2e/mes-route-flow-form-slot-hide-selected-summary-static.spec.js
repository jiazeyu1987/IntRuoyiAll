const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(frontendRoot, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

for (const removedUi of [
  'data-route-process-setting-field="candidate-summary"',
  'route-flow-graph-designer__record-binding-candidate-summary',
  'data-flow-action="copy-form-binding"',
  'data-flow-action="move-form-binding-up"',
  'data-flow-action="move-form-binding-down"'
]) {
  assert.ok(!component.includes(removedUi), `表单槽位卡片不应继续渲染截图红框内容: ${removedUi}`)
}

for (const retainedUi of [
  'data-route-process-setting-field="form-template"',
  'data-route-process-setting-field="candidate-source-type"',
  'data-route-process-setting-field="candidate-source-id"',
  'buildFormBindingSaveRows',
  'candidateSourceType: binding.candidateSourceType',
  'candidateSourceIds: binding.candidateSourceIds'
]) {
  assert.ok(component.includes(retainedUi), `隐藏红框内容后必须保留表单和填写人配置链路: ${retainedUi}`)
}

assert.ok(
  component.includes('data-flow-action="remove-form-binding"'),
  '隐藏红框内容后仍需保留删除槽位能力，避免无法移除误选表单。'
)

console.log('mes-route-flow-form-slot-hide-selected-summary-static PASS')
