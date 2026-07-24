const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(path.join(root, 'src/views/mes/pro/process/index.vue'), 'utf8')
const apiSource = fs.readFileSync(path.join(root, 'src/api/mes/pro/process/index.ts'), 'utf8')

const expectedExtraSlotColumns = [
  ['lossReportFormNames', 'lossReportForms', '损耗单'],
  ['processInspectionFormNames', 'processInspectionForms', '过程检验单'],
  ['parameterRecordFormNames', 'parameterRecordForms', '参数记录表']
]

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

for (const [nameField, linkField, label] of expectedExtraSlotColumns) {
  assert.doesNotMatch(
    processDefaultColumnsSource,
    new RegExp(`key:\\s*'${nameField}'`),
    `default column config must not include ${nameField}`
  )
  assert.doesNotMatch(
    processMainTableSource,
    new RegExp(`label="${label}"[\\s\\S]*prop="${nameField}"`),
    `table must not render ${label}`
  )
  assert.match(apiSource, new RegExp(`${nameField}\\?:\\s*string`), `process API model must expose ${nameField}`)
  assert.match(
    apiSource,
    new RegExp(`${linkField}\\?:\\s*ProProcessBatchRecordFormLinkVO\\[\\]`),
    `process API model must expose ${linkField}`
  )
}

assert.match(pageSource, /process-unconfigured/, 'empty extra slot cells must keep the 未配置 treatment')
assert.doesNotMatch(pageSource, /mock|fallback/i, 'extra slot columns must not add mock or fallback logic')

console.log('PASS: mes pro process hidden extra form slot columns static contract')
