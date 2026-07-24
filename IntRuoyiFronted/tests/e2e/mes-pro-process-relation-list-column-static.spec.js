const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')
const processApiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/process/index.ts'), 'utf8')
const routeApiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/route/index.ts'), 'utf8')

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
  assert.ok(firstIndex >= 0, `${label}: missing ${first}`)
  assert.ok(secondIndex >= 0, `${label}: missing ${second}`)
  assert.ok(firstIndex < secondIndex, label)
}

const defaultColumnsSource = extractProcessDefaultColumns()
const mainTableSource = extractProcessMainTable()

assert.match(
  defaultColumnsSource,
  /key:\s*'relationList'[\s\S]{0,120}label:\s*'关系清单'/,
  '工序设置主列表默认列必须包含关系清单'
)
assertAppearsBefore(
  defaultColumnsSource,
  "key: 'routeList'",
  "key: 'relationList'",
  '关系清单列必须紧跟所属工艺路线之后，便于按路线理解关系摘要'
)
assertAppearsBefore(
  defaultColumnsSource,
  "key: 'relationList'",
  "key: 'workstationNames'",
  '关系清单列必须在工作站和生产配置字段之前展示'
)
assert.match(
  mainTableSource,
  /v-if="isProcessColumnVisible\('relationList'\)"[\s\S]*label="关系清单"[\s\S]*prop="relationList"/,
  '工序设置主列表必须渲染关系清单列'
)
assert.match(
  mainTableSource,
  /buildProcessRelationListSummary\(scope\.row\)/,
  '关系清单单元格必须从当前工序行生成摘要'
)
assert.match(
  pageSource,
  /ProRouteApi\.getRouteProcessFlowGraph/,
  '关系清单必须复用现有工艺路线流转关系图接口'
)
assert.match(
  routeApiSource,
  /getRouteProcessFlowGraph:\s*async/,
  '现有工艺路线 API 必须提供流转关系图读取能力'
)
assert.doesNotMatch(
  processApiSource,
  /relationList|relationSummary|relationGraph/,
  '本次不得为了工序设置主列表新增生产工序后端字段或第二套关系模型'
)
assert.match(
  pageSource,
  /routeProcessId/,
  '关系清单必须使用现有 routeList.routeProcessId 定位路线工序'
)
assert.match(
  pageSource,
  /工序开始[\s\S]*工序结束/,
  '关系清单摘要必须沿用关系图边界节点标签'
)
assert.match(pageSource, /暂无关系/, '无关系时必须显示暂无关系')
assert.match(
  pageSource,
  /isProcessColumnVisible\('relationList'\)[\s\S]*loadRelationGraphsForVisibleProcesses/,
  '只有关系清单列可见时才加载关系图数据'
)
assert.doesNotMatch(pageSource, /mock|伪成功|fallback/i, '关系清单列不得使用 mock、伪成功或 fallback')

console.log('PASS: mes pro process relation list column static contract')
