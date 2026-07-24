const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'),
  'utf8'
)

const assertIncludes = (content, expected, label) => {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing: ${expected}`)
  }
}

const assertNotIncludes = (content, expected, label) => {
  if (content.includes(expected)) {
    throw new Error(`${label} must not include: ${expected}`)
  }
}

assertIncludes(
  component,
  'routeProcessSettingColumns.value',
  'redbox fields must start from process setting column order'
)
assertIncludes(
  component,
  '.filter((column) => column.visible && isRouteProcessSettingsDetailColumnKey(column.key))',
  'redbox fields must only include visible shared process setting columns'
)
assertIncludes(
  component,
  'processDetailFieldOptionMap.value.get(column.key as RouteProcessSettingColumnKey)',
  'redbox field resolution must keep shared column order'
)
assertIncludes(
  component,
  '工序设置未选择显示字段',
  'empty hint must point users back to process setting display fields'
)
assertNotIncludes(
  component,
  "const selectedProcessDetailFieldToAdd = ref<ProcessDetailFieldKey>('code')",
  'redbox must not keep an independent first field picker'
)
assertNotIncludes(
  component,
  'syncSelectedProcessDetailFieldToAdd',
  'redbox must not sync an independent add-field picker'
)
assertNotIncludes(
  component,
  'watch(\n  availableProcessDetailFieldOptions',
  'redbox must not watch independent available fields'
)
assertNotIncludes(
  component,
  'selectedProcessDetailFieldToAdd.value = undefined',
  'redbox must not maintain independent picker state'
)

console.log('mes-route-flow-default-first-field-static PASS')
