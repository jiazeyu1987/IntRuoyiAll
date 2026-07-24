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

const hiddenColumns = [
  ['lossReportFormNames', 'lossReportForms', '损耗单'],
  ['processInspectionFormNames', 'processInspectionForms', '过程检验单'],
  ['parameterRecordFormNames', 'parameterRecordForms', '参数记录表']
]

for (const [nameField, linkField, label] of hiddenColumns) {
  assert.doesNotMatch(
    processDefaultColumnsSource,
    new RegExp(`key:\\s*'${nameField}'`),
    `工序设置列表默认列不得继续包含 ${label}`
  )
  assert.doesNotMatch(
    processMainTableSource,
    new RegExp(`label="${label}"[\\s\\S]*prop="${nameField}"`),
    `工序设置列表不得继续渲染 ${label} 列`
  )
  assert.match(apiSource, new RegExp(`${nameField}\\?:\\s*string`), `工序 API 类型仍需保留 ${nameField}`)
  assert.match(
    apiSource,
    new RegExp(`${linkField}\\?:\\s*ProProcessBatchRecordFormLinkVO\\[\\]`),
    `工序 API 类型仍需保留 ${linkField}`
  )
}

assert.match(processDefaultColumnsSource, /key:\s*'batchRecordFormNames'/, '批记录表单列必须保留')
assert.match(processMainTableSource, /label="批记录表单"[\s\S]*prop="batchRecordFormNames"/, '批记录表单列必须继续渲染')
assert.doesNotMatch(pageSource, /mock|fallback/i, '隐藏列不得引入 mock 或 fallback 逻辑')

console.log('PASS: mes pro process hidden extra form columns static contract')
