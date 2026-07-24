const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/feedback/index.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(path.join(repoRoot, 'src/api/mes/pro/feedback/index.ts'), 'utf8')

const tableStart = source.indexOf('<ContentWrap v-if="activeTab === \'feedback\'">')
const tableEnd = source.indexOf('</el-table>', tableStart)
assert(tableStart >= 0 && tableEnd > tableStart, '必须保留正式报工列表表格。')
const feedbackTable = source.slice(tableStart, tableEnd)

const expectedColumns = [
  ['产品代码', 'excelProductCode'],
  ['产品名称', 'excelProductName'],
  ['工序编码', 'excelProcessCode'],
  ['工序名称', 'excelProcessName'],
  ['部门', 'excelDepartment'],
  ['人员工号', 'excelEmployeeNo'],
  ['人员名称', 'excelEmployeeName'],
  ['工段长', 'excelSectionLeader'],
  ['报工个数', 'feedbackQuantity'],
  ['日期', 'excelFeedbackTime']
]

let lastIndex = -1
for (const [label, prop] of expectedColumns) {
  const labelIndex = feedbackTable.indexOf(`label="${label}"`)
  assert(labelIndex > lastIndex, `正式报工列表必须按截图顺序展示列：${label}`)
  lastIndex = labelIndex
  assert(
    feedbackTable.includes(`prop="${prop}"`),
    `正式报工列表列 ${label} 必须绑定后端 Excel 展示字段 ${prop}。`
  )
  assert(apiSource.includes(`${prop}`), `ProFeedbackVO 必须声明字段 ${prop}。`)
}

for (const oldLabel of [
  '报工单号',
  '报工类型',
  '工作站',
  '生产工单编码',
  '产品物料编码',
  '规格型号',
  '报工数量',
  '报工人',
  '当前审批人',
  '状态',
  '审批影响',
  '操作'
]) {
  assert(
    !feedbackTable.includes(`label="${oldLabel}"`),
    `正式报工列表已重设计，不应继续展示旧列：${oldLabel}`
  )
}

console.log('mes-feedback-list-excel-columns-static: PASS')
