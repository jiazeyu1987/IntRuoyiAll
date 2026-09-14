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

assert.doesNotMatch(listTable, /label="填写人"/, '删除“填写人”列后，左侧表格不得再存在填写人错误态单元格。')
assert.doesNotMatch(listTable, /加载失败/, '删除“填写人”列后，左侧表格不得再显示填写人规则加载失败状态。')
assert.doesNotMatch(listTable, /permissionRuleErrorMessage/, '删除“填写人”列后，左侧表格不得再绑定填写人规则错误文本。')
assert.doesNotMatch(page, /批记录表单填写人规则加载失败/, '当前页面不得再为已删除列维护填写人规则加载错误文案。')

console.log('PASS: eDHR batch record filler error entry removed with filler column.')
