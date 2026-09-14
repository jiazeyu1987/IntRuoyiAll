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
  'v-model:visible="processFormBindingCopyPopoverVisible"',
  ':teleported="false"',
  '@hide="handleProcessFormBindingCopyPopoverHide"',
  'getProcessFormBindingCopySourceOptions',
  'handleProcessFormBindingCopySourceChange',
  'handleProcessFormBindingCopyPopoverHide',
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
  /copySelectedProcessFormBindingsFromSource[\s\S]*currentGlobalBindings[\s\S]*copiedLocalBindings[\s\S]*copyRecordBindingForSelectedProcess[\s\S]*createLocalFormBindingKey/,
  '整组复制必须保留当前工序的全局绑定，只替换非全局绑定并生成新的本地绑定 key。'
)

assert.match(
  component,
  /copySelectedProcessFormBindingsFromSource[\s\S]*syncSelectedRecordBindingsToDraft\(\)[\s\S]*message\.success\('已复制工序表单绑定关系'\)/,
  '整组复制完成后必须同步当前工序草稿并给出成功反馈。'
)

assert.match(
  component,
  /const processFormBindingCopyPopoverVisible\s*=\s*ref\(false\)[\s\S]*const processFormBindingCopySourceRouteProcessId\s*=\s*ref<number \| null>\(null\)/,
  '复制弹层必须使用显式可见状态，避免内部下拉选择和 click-outside 竞争。'
)

assert.match(
  component,
  /copySelectedProcessFormBindingsFromSource[\s\S]*processFormBindingCopySourceRouteProcessId\.value\s*=\s*null[\s\S]*processFormBindingCopyPopoverVisible\.value\s*=\s*false[\s\S]*syncSelectedRecordBindingsToDraft\(\)/,
  '整组复制成功后必须清空来源选择并显式关闭复制弹层。'
)

console.log('mes-route-flow-copy-process-form-bindings-static PASS')
