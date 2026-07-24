const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

for (const expected of [
  'data-flow-action="copy-process-form-bindings"',
  'data-route-process-setting-field="copy-process-form-bindings-source"',
  'data-flow-action="confirm-copy-process-form-bindings"',
  'getProcessFormBindingCopySourceOptions',
  'handleProcessFormBindingCopySourceChange',
  'copySelectedProcessFormBindingsFromSource'
]) {
  assert.ok(component.includes(expected), `动态表单列表必须支持从其他工序复制整组绑定关系: ${expected}`)
}

assert.match(
  component,
  /getProcessFormBindingCopySourceOptions[\s\S]*routeNodes\.value[\s\S]*node\.routeProcessId !== currentRouteProcessId[\s\S]*getRouteNodeBatchRecordBindings\(node\)[\s\S]*isRecordBindingConfigured/,
  '复制来源必须来自同一路线的其他工序，并且只展示已有表单绑定关系的工序。'
)

assert.match(
  component,
  /copySelectedProcessFormBindingsFromSource[\s\S]*selectedRecordBindings\.value\s*=\s*sourceBindings[\s\S]*copyRecordBindingForSelectedProcess[\s\S]*createLocalFormBindingKey/,
  '整组复制必须替换当前工序列表，并为当前工序生成新的本地绑定 key，不能复用来源工序 key。'
)

assert.match(
  component,
  /copySelectedProcessFormBindingsFromSource[\s\S]*syncSelectedRecordBindingsToDraft\(\)[\s\S]*message\.success\('已复制工序表单绑定关系'\)/,
  '整组复制完成后必须同步当前工序草稿并给出成功反馈。'
)

console.log('mes-route-flow-copy-process-form-bindings-static PASS')
