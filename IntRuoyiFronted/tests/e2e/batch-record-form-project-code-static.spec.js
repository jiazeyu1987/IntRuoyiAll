const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/batchrecordreport/index.ts')

const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert.match(api, /projectCode\?:\s*string/, '批记录表单列表接口 VO 必须返回 DCC 项目代码 projectCode。')

const tableStart = page.indexOf('<el-table')
const tableEnd = page.indexOf('</el-table>', tableStart)
assert.notEqual(tableStart, -1, '批记录表单列表必须存在表格。')
assert.notEqual(tableEnd, -1, '批记录表单列表表格源码必须完整。')
const table = page.slice(tableStart, tableEnd)

const productColumnIndex = table.indexOf("isRecordFormColumnVisible('productName')")
const projectCodeColumnIndex = table.indexOf("isRecordFormColumnVisible('projectCode')")
const reportNameColumnIndex = table.indexOf("isRecordFormColumnVisible('reportName')")
assert.notEqual(productColumnIndex, -1, '批记录表单列表必须保留产品名称列。')
assert.notEqual(projectCodeColumnIndex, -1, '批记录表单列表必须新增项目代码列。')
assert.notEqual(reportNameColumnIndex, -1, '批记录表单列表必须保留表单名称列。')
assert(
  projectCodeColumnIndex > productColumnIndex && projectCodeColumnIndex < reportNameColumnIndex,
  '项目代码列必须位于产品名称和表单名称之间，方便识别表单绑定的 DCC 项目代码。'
)

assert.match(
  table,
  /label="项目代码"[\s\S]{0,160}prop="projectCode"/,
  '项目代码列必须绑定后端 projectCode 字段。'
)
assert.match(
  table,
  /row\.projectCode\s*\|\|\s*'-'/,
  '项目代码列必须展示真实 projectCode；旧数据为空时只显示短横线，不得猜测补齐。'
)

const defaultColumnsStart = page.indexOf('const recordFormDefaultColumns')
const defaultColumnsEnd = page.indexOf('const {', defaultColumnsStart)
assert.notEqual(defaultColumnsStart, -1, '批记录表单列表必须定义默认列。')
assert.notEqual(defaultColumnsEnd, -1, '批记录表单列表默认列源码必须完整。')
const defaultColumns = page.slice(defaultColumnsStart, defaultColumnsEnd)
assert.match(
  defaultColumns,
  /\{\s*key:\s*'projectCode',\s*label:\s*'项目代码'/,
  '项目代码必须进入默认列集合，旧账号也能默认看到该列。'
)

assert.match(
  page,
  /table-key="mes\.pro\.edhrBatch\.recordFormList\.projectCodeV1"/,
  '新增默认列后必须升级标准列表 tableKey，避免旧列配置隐藏项目代码。'
)
assert.match(
  page,
  /useUserTableColumns\('mes\.pro\.edhrBatch\.recordFormList\.projectCodeV1',\s*recordFormDefaultColumns\)/,
  '新增默认列后必须同步升级用户列配置存储 key。'
)
assert.doesNotMatch(
  page,
  /table-key="mes\.pro\.edhrBatch\.recordFormList"/,
  '新增项目代码列后不得继续使用旧标准列表 tableKey。'
)
assert.doesNotMatch(
  page,
  /useUserTableColumns\('mes\.pro\.edhrBatch\.recordFormList',\s*recordFormDefaultColumns\)/,
  '新增项目代码列后不得继续使用旧用户列配置 key。'
)
assert.doesNotMatch(
  table,
  /formBindings[\s\S]{0,160}projectCode|projectCode[\s\S]{0,160}formBindings/i,
  '项目代码列不得从表单槽位 formBindings 推断。'
)

console.log('PASS: batch-record form project code static contract')
