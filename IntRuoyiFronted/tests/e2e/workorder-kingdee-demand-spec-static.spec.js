const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/workorder/index.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/workorder/index.ts'), 'utf8')

const tableStart = page.indexOf('<el-table')
const tableEnd = page.indexOf('</el-table>', tableStart)
assert.notEqual(tableStart, -1, '生产工单页面必须包含列表表格。')
assert.notEqual(tableEnd, -1, '生产工单页面表格必须正常闭合。')
const tableSource = page.slice(tableStart, tableEnd)

assert.match(api, /demandBillNo:\s*string/, '生产工单 API 类型必须声明需求单据字段。')
assert.match(api, /productSpecification:\s*string/, '生产工单 API 类型必须声明规格型号字段。')
assert.match(page, /key:\s*'demandBillNo'[\s\S]*label:\s*'需求单据'/, '默认列配置必须包含需求单据。')
assert.match(page, /key:\s*'productSpecification'[\s\S]*label:\s*'规格型号'/, '默认列配置必须包含规格型号。')
assert.match(tableSource, /label="需求单据"[\s\S]*prop="demandBillNo"/, '列表必须展示需求单据列并绑定 demandBillNo。')
assert.match(
  tableSource,
  /label="规格型号"[\s\S]*prop="productSpecification"/,
  '列表规格型号列必须绑定后端返回的金蝶规格型号字段 productSpecification。'
)

const columnIndex = (label) => tableSource.indexOf(`label="${label}"`)
for (const label of ['工单编号', '产品编码', '产品名称', '需求单据', '规格型号', '计划数量']) {
  assert.notEqual(columnIndex(label), -1, `生产工单列表必须包含${label}列。`)
}
assert(columnIndex('产品名称') < columnIndex('需求单据'), '需求单据列必须位于产品名称之后。')
assert(columnIndex('需求单据') < columnIndex('规格型号'), '规格型号列必须位于需求单据之后。')
assert(columnIndex('规格型号') < columnIndex('计划数量'), '规格型号列必须位于计划数量之前。')

console.log('PASS: workorder kingdee demand/spec frontend contract')
