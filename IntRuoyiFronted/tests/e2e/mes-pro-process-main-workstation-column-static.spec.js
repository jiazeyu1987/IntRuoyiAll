const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/process/index.ts'), 'utf8')

const extractProcessDefaultColumns = () => {
  const start = pageSource.indexOf('const processDefaultColumns')
  const end = pageSource.indexOf('const {', start)
  assert.notEqual(start, -1, '工序设置主列表缺少 processDefaultColumns 定义')
  assert.notEqual(end, -1, '工序设置主列表 processDefaultColumns 结构异常')
  return pageSource.slice(start, end)
}

const extractProcessMainTable = () => {
  const tableClass = pageSource.indexOf('class="process-main-table"')
  const start = pageSource.lastIndexOf('<el-table', tableClass)
  const end = pageSource.indexOf('</el-table>', tableClass)
  assert.notEqual(tableClass, -1, '工序设置主列表缺少 process-main-table 标记')
  assert.notEqual(start, -1, '工序设置主列表 table 结构异常')
  assert.notEqual(end, -1, '工序设置主列表 table 结束标签缺失')
  return pageSource.slice(start, end)
}

const assertAppearsBefore = (source, first, second, label) => {
  const firstIndex = source.indexOf(first)
  const secondIndex = source.indexOf(second)
  assert.ok(firstIndex >= 0, `${label}: missing first marker ${first}`)
  assert.ok(secondIndex >= 0, `${label}: missing second marker ${second}`)
  assert.ok(firstIndex < secondIndex, label)
}

const defaultColumnsSource = extractProcessDefaultColumns()
const mainTableSource = extractProcessMainTable()

assert.match(
  defaultColumnsSource,
  /key:\s*'workstationNames'[\s\S]{0,120}label:\s*'工作站'/,
  '显示字段配置必须包含工作站'
)
assertAppearsBefore(
  defaultColumnsSource,
  "key: 'routeList'",
  "key: 'workstationNames'",
  '工作站列应紧跟所属工艺路线，便于查看工序与工作站关系'
)
assertAppearsBefore(
  defaultColumnsSource,
  "key: 'workstationNames'",
  "key: 'productionQuantityFactor'",
  '工作站列必须在生产系数等排产字段前展示'
)
assert.match(
  mainTableSource,
  /v-if="isProcessColumnVisible\('workstationNames'\)"[\s\S]*label="工作站"[\s\S]*prop="workstationNames"/,
  '工序设置主列表必须渲染工作站列'
)
assert.match(
  mainTableSource,
  /scope\.row\.workstations\?\.length[\s\S]*formatProcessWorkstation/,
  '工作站列必须使用结构化 workstations 数据展示'
)
assert.match(
  mainTableSource,
  /@click="openProcessWorkstation\(workstation,\s*scope\.row\)"/,
  '工序设置主列表工作站标签必须可点击并传入当前工序行'
)
assert.match(
  pageSource,
  /path:\s*'\/mes\/md\/workstation'[\s\S]*code:\s*workstationCode[\s\S]*processId:\s*String\(row\.id\)/,
  '点击工序工作站必须跳转到工作站设置并按工作站编码和当前工序过滤'
)
assert.match(
  pageSource,
  /工作站跳转缺少工作站编码/,
  '工序工作站跳转缺少编码时必须失败并暴露原因'
)
assert.doesNotMatch(
  defaultColumnsSource,
  /key:\s*'machineryQuantityTotal'/,
  '工序设置主列表默认列必须移除设备列，设备资源统一在工作站维护'
)
assert.doesNotMatch(
  defaultColumnsSource,
  /key:\s*'availableShiftCapacityTotal'/,
  '工序设置主列表默认列必须移除班次产能列，标准产能统一在工作站维护'
)
assert.doesNotMatch(
  mainTableSource,
  /prop="machineryQuantityTotal"/,
  '工序设置主列表不得继续渲染设备列'
)
assert.doesNotMatch(
  mainTableSource,
  /prop="availableShiftCapacityTotal"/,
  '工序设置主列表不得继续渲染班次产能列'
)
assert.match(
  apiSource,
  /export interface ProProcessWorkstationVO[\s\S]*id:\s*number[\s\S]*code:\s*string[\s\S]*name:\s*string/,
  '工序 API 类型必须声明 ProProcessWorkstationVO'
)
assert.match(apiSource, /workstationNames\?:\s*string/, '工序 API 类型必须暴露 workstationNames')
assert.match(apiSource, /workstations\?:\s*ProProcessWorkstationVO\[\]/, '工序 API 类型必须暴露 workstations')

console.log('PASS: mes pro process main workstation column static contract')
