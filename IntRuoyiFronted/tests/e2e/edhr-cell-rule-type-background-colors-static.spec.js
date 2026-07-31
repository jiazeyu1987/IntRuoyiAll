const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../..')
const dialog = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue'),
  'utf8'
)

const typeCases = [
  ['text', 'is-rule-type-string', '#eff6ff'],
  ['number', 'is-rule-type-number', '#ecfdf3'],
  ['date', 'is-rule-type-date', '#fff7ed'],
  ['datetime', 'is-rule-type-datetime', '#f5f3ff'],
  ['boolean', 'is-rule-type-boolean', '#f0fdfa'],
  ['signature', 'is-rule-type-signature', '#fef2f2'],
  ['select', 'is-rule-type-select', '#fdf4ff']
]

assert.match(
  dialog,
  /resolveCellRuleTypeClass/,
  '单元格规则弹窗必须通过统一函数把字段类型解析为稳定背景色类。'
)

assert.match(
  dialog,
  /isSelectRule\(rule\)[\s\S]*is-rule-type-select/,
  '下拉框必须优先按 componentFlag=select 映射为专用背景色，不能只显示普通文本色。'
)

typeCases.forEach(([label, className, color]) => {
  assert.match(
    dialog,
    new RegExp(`['"]${className}['"]\\s*:`),
    `${label} 类型必须写入 ${className} 单元格类。`
  )
  assert.match(
    dialog,
    new RegExp(`\\.${className}\\s*\\{[\\s\\S]*background:\\s*${color.replace('#', '\\#')}`),
    `${label} 类型必须使用 ${color} 背景色。`
  )
})

assert.equal(
  new Set(typeCases.map(([, , color]) => color)).size,
  typeCases.length,
  '不同字段类型的背景色不能复用同一个颜色。'
)

assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__cell\.is-selected\s*\{[\s\S]*outline:/,
  '类型背景色不能覆盖选中态，选中单元格仍必须有可见 outline。'
)

console.log('PASS: eDHR cell rule type background colors static contract')
