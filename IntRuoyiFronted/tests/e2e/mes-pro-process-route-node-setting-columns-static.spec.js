const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/process/index.ts'), 'utf8')

const extractProcessDefaultColumns = () => {
  const start = pageSource.indexOf('const processDefaultColumns')
  const end = pageSource.indexOf('const {', start)
  assert.notEqual(start, -1, '工序设置表缺少 processDefaultColumns 定义')
  assert.notEqual(end, -1, '工序设置表 processDefaultColumns 结构异常')
  return pageSource.slice(start, end)
}

const extractProcessMainTable = () => {
  const tableClass = pageSource.indexOf('class="process-main-table"')
  const start = pageSource.lastIndexOf('<el-table', tableClass)
  const end = pageSource.indexOf('</el-table>', tableClass)
  assert.notEqual(tableClass, -1, '工序设置表缺少主列表 table 标记')
  assert.notEqual(start, -1, '工序设置表主列表结构异常')
  assert.notEqual(end, -1, '工序设置表主列表结束标签缺失')
  return pageSource.slice(start, end)
}

const processDefaultColumnsSource = extractProcessDefaultColumns()
const processMainTableSource = extractProcessMainTable()

const requiredColumns = [
  ['productionQuantityFactor', '生产系数', 'number'],
  ['batchRecordFormNames', '批记录表单', 'string']
]

const hiddenColumns = [
  ['lossReportFormNames', '损耗单', 'string'],
  ['processInspectionFormNames', '过程检验单', 'string'],
  ['parameterRecordFormNames', '参数记录表', 'string']
]

for (const [field, label, type] of requiredColumns) {
  assert.match(
    pageSource,
    new RegExp(`key:\\s*'${field}'[\\s\\S]*label:\\s*'${label}'[\\s\\S]*hideable:\\s*false`),
    `工序设置表默认列必须强制展示 ${label}`
  )
  assert.match(
    processMainTableSource,
    new RegExp(`label="${label}"[\\s\\S]*prop="${field}"`),
    `工序设置表必须渲染 ${label} 列`
  )
  assert.match(apiSource, new RegExp(`${field}\\?:\\s*${type}`), `工序 API 类型必须暴露 ${field}`)
}

for (const [field, label, type] of hiddenColumns) {
  assert.doesNotMatch(
    processDefaultColumnsSource,
    new RegExp(`key:\\s*'${field}'`),
    `工序设置表默认列不得继续包含 ${label}`
  )
  assert.doesNotMatch(
    processMainTableSource,
    new RegExp(`label="${label}"[\\s\\S]*prop="${field}"`),
    `工序设置表不得继续渲染 ${label} 列`
  )
  assert.match(apiSource, new RegExp(`${field}\\?:\\s*${type}`), `工序 API 类型必须继续暴露 ${field}`)
}

assert.match(
  pageSource,
  /formatCapacity\(scope\.row\.productionQuantityFactor\)/,
  '生产系数列必须展示后端返回值'
)

assert.doesNotMatch(
  processDefaultColumnsSource,
  /key:\s*'shiftCapacity'/,
  '工序设置主列表默认列必须删除旧 shiftCapacity 班次产能列'
)
assert.doesNotMatch(
  processMainTableSource,
  /prop="shiftCapacity"/,
  '工序设置主列表不得继续渲染旧 shiftCapacity 班次产能列'
)
assert.doesNotMatch(
  processDefaultColumnsSource,
  /key:\s*'availableShiftCapacityTotal'/,
  '工序设置主列表默认列必须删除工作站派生的班次产能列'
)
assert.doesNotMatch(
  processMainTableSource,
  /prop="availableShiftCapacityTotal"/,
  '工序设置主列表不得继续渲染工作站派生的班次产能列'
)
assert.doesNotMatch(
  processDefaultColumnsSource,
  /key:\s*'machineryQuantityTotal'/,
  '工序设置主列表默认列必须删除设备列'
)
assert.doesNotMatch(
  processMainTableSource,
  /prop="machineryQuantityTotal"/,
  '工序设置主列表不得继续渲染设备列'
)
assert.match(
  apiSource,
  /availableShiftCapacityTotal\?:\s*number/,
  '工序 API 类型必须暴露 availableShiftCapacityTotal'
)
assert.doesNotMatch(pageSource, /<RouteFlowConfigPanel/, '工序设置表不得恢复旧独立配置面板')

console.log('PASS: mes pro process route node setting columns static contract')
