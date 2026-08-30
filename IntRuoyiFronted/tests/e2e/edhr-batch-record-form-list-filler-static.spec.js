const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const processPagePath = path.join(repoRoot, 'src/views/mes/pro/process/index.vue')

const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')
const processPage = fs.readFileSync(processPagePath, 'utf8')

const tableStart = page.indexOf('<el-table')
const tableEnd = page.indexOf('</el-table>', tableStart)

assert.notEqual(tableStart, -1, '批记录表单列表必须保留左侧表格。')
assert.notEqual(tableEnd, -1, '批记录表单列表左侧表格必须完整闭合。')

const listTable = page.slice(tableStart, tableEnd)
const reportNameColumn = listTable.indexOf('label="表单名称"')
const formSlotTypeColumn = listTable.indexOf('label="类型"')

assert.notEqual(reportNameColumn, -1, '批记录表单列表必须保留“表单名称”列。')
assert.notEqual(formSlotTypeColumn, -1, '批记录表单列表必须保留“类型”列。')
assert.doesNotMatch(listTable, /label="填写人"/, '批记录表单列表不得再显示“填写人”列。')
assert.doesNotMatch(listTable, /prop="fillRule"/, '批记录表单列表不得再绑定填写人列字段。')
assert.doesNotMatch(listTable, /batch-record-form-filler-cell/, '批记录表单列表不得再显示“未配置 / 配置填写人”入口。')
assert.doesNotMatch(page, /批记录表单填写人设置/, '批记录表单列表不得再保留列表列专用填写人设置弹窗。')
assert.doesNotMatch(page, /openBatchRecordFormPermissionDialog/, '批记录表单列表不得再保留列表列专用填写人设置处理函数。')
assert.doesNotMatch(page, /EdhrProcessFormPermissionRuleApi/, '批记录表单列表不得再为已删除列请求填写人规则。')
assert.doesNotMatch(page, /\{\s*key:\s*'fillRule'[\s\S]*?label:\s*'填写人'/, '批记录表单列表默认列配置不得再注册“填写人”。')
assert.match(
  page,
  /<el-button link type="primary" @click="openTemplateAction\(selectedReport, 'cellRules'\)">填写配置<\/el-button>/,
  '右侧预览顶部必须继续通过“填写配置”进入批记录单元格填写规则。'
)

for (const removedLabel of ['生产填写人', '质量填写人', '设备填写人']) {
  assert(
    !processPage.includes(removedLabel),
    `工序设置页不得继续展示${removedLabel}。`
  )
}

console.log('PASS: eDHR batch record form list filler column removal static contract')
