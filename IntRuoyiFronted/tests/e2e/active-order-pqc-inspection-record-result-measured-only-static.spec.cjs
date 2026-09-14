const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'),
  'utf8'
)

const formatterStart = panel.indexOf('const formatPqcInspectionRecordResultText =')
assert.ok(formatterStart > 0, '必须存在 PQC 检测结果格式化函数')
const formatterEnd = panel.indexOf('const applyPqcInspectionRecordRowSpan', formatterStart)
assert.ok(formatterEnd > formatterStart, '必须能定位 PQC 检测结果格式化函数边界')
const formatter = panel.slice(formatterStart, formatterEnd)

assert.match(
  formatter,
  /return\s+measuredValuesText\s*&&\s*measuredValuesText\s*!==\s*'-'\s*\?\s*measuredValuesText\s*:\s*'-'/,
  'PQC 检测结果列只能显示实测内容；无实测时显示 -。'
)
assert.doesNotMatch(formatter, /标准：/, 'PQC 检测结果列不得再拼接标准内容。')
assert.doesNotMatch(formatter, /样本：/, 'PQC 检测结果列不得再拼接样本内容。')
assert.doesNotMatch(formatter, /实测：/, 'PQC 检测结果列不得再显示“实测：”前缀。')

console.log('PASS: active order PQC inspection result measured-only static contract')
