const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const tableStart = page.indexOf('<el-table')
const tableEnd = page.indexOf('</el-table>', tableStart)

assert.notEqual(tableStart, -1, '批记录表单列表必须保留左侧表格。')
assert.notEqual(tableEnd, -1, '批记录表单列表左侧表格必须完整闭合。')

const listTable = page.slice(tableStart, tableEnd)

assert.doesNotMatch(listTable, /label="填写人"/, '左侧批记录表单列表不得再显示“填写人”列。')
assert.doesNotMatch(listTable, /prop="fillRule"/, '左侧批记录表单列表不得再绑定填写人列字段。')
assert.doesNotMatch(
  listTable,
  /batch-record-form-filler-cell/,
  '左侧批记录表单列表不得再显示“未配置 / 配置填写人”入口。'
)
assert.doesNotMatch(
  page,
  /\{\s*key:\s*'fillRule'[\s\S]*?label:\s*'填写人'/,
  '批记录表单列表默认列配置不得再注册“填写人”。'
)
assert.doesNotMatch(
  page,
  /EdhrProcessFormPermissionRuleApi/,
  '删除列表“填写人”列后，当前页面不应再为不可见列延迟加载填写人规则。'
)
assert.match(
  page,
  /<el-button link type="primary" @click="openTemplateAction\(selectedReport, 'cellRules'\)">填写配置<\/el-button>/,
  '右侧预览顶部必须继续保留“填写配置”入口。'
)
assert.match(
  page,
  /class="batch-record-form-preview__actions"/,
  '右侧预览顶部操作区必须保留。'
)

console.log('PASS: eDHR batch record form list hides filler column static contract')
