const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
)
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const extractConstArray = (constName) => {
  const start = page.indexOf(`const ${constName}`)
  const arrayStart = page.indexOf('[', page.indexOf('=', start))
  const end = page.indexOf('\n]', arrayStart)
  assert.ok(start >= 0 && arrayStart > start && end > arrayStart, `${constName} must be declared.`)
  return page.slice(arrayStart, end + 2)
}

const extractColumnPairs = (arraySource) =>
  [...arraySource.matchAll(/\{\s*key:\s*'([^']+)'[\s\S]*?label:\s*'([^']+)'/g)].map(
    ([, key, label]) => ({ key, label })
  )

const pqcDefaultColumns = extractConstArray('pqcSubmissionDefaultColumns')
const pqcPairs = extractColumnPairs(pqcDefaultColumns)
const businessPairs = pqcPairs.filter((column) => column.key !== 'operation')
const expectedBusinessPairs = [
  ['submittedAt', '提交时间'],
  ['employeeUser', 'PQC检验员'],
  ['process', '工序'],
  ['workOrder', '生产工单'],
  ['completionQuantity', '检验数量'],
  ['lossQuantity', '损耗数量'],
  ['lossBreakdown', '损耗明细'],
  ['product', '产品'],
  ['inspectionTask', '检验类型/轮次']
]

assert.deepEqual(
  businessPairs.map((column) => [column.key, column.label]),
  expectedBusinessPairs,
  'PQC管理常规列表只能默认显示截图中的业务列。'
)
assert.match(
  pqcDefaultColumns,
  /key:\s*'operation'[\s\S]*label:\s*'操作'[\s\S]*hideable:\s*false/,
  'PQC管理必须保留不可隐藏的操作列用于进入详情。'
)

const detailOnlyKeys = [
  'inspectionItems',
  'equipmentSnapshot',
  'selectedDevice',
  'equipmentNumber',
  'acceptanceStandard',
  'inspectionMethod',
  'inspectionJudgement',
  'parameterSnapshot',
  'deviceParameterReadings'
]
for (const key of detailOnlyKeys) {
  assert.doesNotMatch(
    pqcDefaultColumns,
    new RegExp(`key:\\s*'${key}'`),
    `${key} must be removed from the PQC管理 regular default columns.`
  )
}

const detailTabMarker = 'data-pqc-leader-detail-tab'
const detailMarkerIndex = page.indexOf(detailTabMarker)
assert.ok(detailMarkerIndex >= 0, 'PQC详情页签必须存在。')
const detailStart = page.lastIndexOf('<ContentWrap', detailMarkerIndex)
const detailEnd = page.indexOf('</ContentWrap>', detailMarkerIndex)
assert.ok(detailStart >= 0 && detailEnd > detailStart, 'PQC详情页签内容块必须可定位。')
const detailBlock = page.slice(detailStart, detailEnd)

for (const label of ['检验项目', '检验设备', '设备编号', '接收标准', '检验方法', '样本值', '判定']) {
  assert.match(
    detailBlock,
    new RegExp(`label="${label}"`),
    `PQC详情必须继续显示${label}。`
  )
}
assert.match(
  detailBlock,
  /formatPqcSnapshotStandard\(row\)[\s\S]*formatPqcSnapshotSampleValues\(row\)/,
  'PQC详情必须继续展示标准和样本值的正式格式化结果。'
)

console.log('PASS: PQC管理常规列表只保留截图列，检验明细留在详情页')
