const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const dialog = read('src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue')

const requiredTones = ['text', 'number', 'date', 'datetime', 'select', 'radio', 'checkbox', 'signature', 'attachment']
const requiredTextBackground = '#dbeafe'
const typeDisplayStart = dialog.indexOf('const resolveRuleEditorCellTypeTone')
const typeDisplayEnd = dialog.indexOf('\nconst resolveErrorMessage', typeDisplayStart)
assert.ok(typeDisplayStart >= 0 && typeDisplayEnd > typeDisplayStart, '必须能定位字段类型颜色转换函数。')
const typeDisplayBlock = dialog.slice(typeDisplayStart, typeDisplayEnd)

assert.match(
  dialog,
  /v-else-if="!cell\.rule" class="batch-record-cell-rules-editor__cell-placeholder"[\s\S]*第 \{\{ cell\.rowIndex \+ 1 \}\} 行第 \{\{ cell\.columnIndex \+ 1 \}\} 列/,
  '行列位置只能作为非规则空格子的兜底显示，可填写规则格子不得显示行列占位。'
)

assert.match(
  dialog,
  /const resolveRuleEditorCellTypeTone = \(rule: BatchRecordReportCellRuleVO\)/,
  '必须有专门的字段类型颜色转换函数，避免把坐标、映射身份或保存载荷混入展示。'
)

assert.doesNotMatch(
  dialog,
  /batch-record-cell-rules-editor__cell-type-label|fillableTypeLabel|\{\{\s*cell\.fillableTypeLabel\s*\}\}/,
  '可填写空白格子不应再渲染“文本/数字/日期”等类型文字标签。'
)

for (const tone of requiredTones) {
  assert.ok(
    dialog.includes(`return '${tone}'`) || dialog.includes(`is-rule-type-${tone}`),
    `缺少字段类型颜色标识：${tone}`
  )
  assert.match(
    dialog,
    new RegExp(`\\.batch-record-cell-rules-editor__cell\\.is-rule\\.is-rule-type-${tone}\\s*\\{[\\s\\S]*?background:\\s*#[0-9a-fA-F]{6};[\\s\\S]*?\\}`),
    `缺少字段类型背景颜色样式：${tone}`
  )
}

assert.match(
  dialog,
  new RegExp(
    `\\.batch-record-cell-rules-editor__cell\\.is-rule\\.is-rule-type-text\\s*\\{[\\s\\S]*?background:\\s*${requiredTextBackground};[\\s\\S]*?\\}`
  ),
  '文本类型背景色必须改为明显蓝色，不能继续使用接近白色的浅灰。'
)

assert.doesNotMatch(
  dialog,
  /\.batch-record-cell-rules-editor__cell\.is-rule\.is-rule-type-text\s*\{[\s\S]*?background:\s*#f8fafc;[\s\S]*?\}/,
  '文本类型背景色不能继续使用接近白色的 #f8fafc。'
)

assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__cell\s*\{[\s\S]*?background:\s*#fff;[\s\S]*?\}/,
  '不可填写普通格子的基础背景必须是白色。'
)

assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__cell\.is-empty\s*\{[\s\S]*?background:\s*#fff;[\s\S]*?\}/,
  '不可填写空格子的背景必须是白色，不能使用接近可填写状态的浅色。'
)

assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__cell:not\(\.is-rule\)[\s\S]*?background:\s*#fff;[\s\S]*?\}/,
  '只要不是正式可填写规则格子，背景必须被统一锁定为白色。'
)

assert.match(
  dialog,
  /\.batch-record-cell-rules-editor__cell:not\(\.is-rule\) \.batch-record-cell-rules-editor__cell-button:hover\s*\{[\s\S]*?background:\s*transparent;[\s\S]*?\}/,
  '不可填写格子悬停时也不能染成接近可填写状态的蓝色。'
)

assert.doesNotMatch(
  dialog,
  /\.batch-record-cell-rules-editor__cell\.is-empty\s*\{[\s\S]*?background:\s*#fbfcfe;[\s\S]*?\}/,
  '不可填写空格子不能继续使用 #fbfcfe。'
)

assert.match(
  dialog,
  /const cellTypeTone = rule \? resolveRuleEditorCellTypeTone\(rule\) : ''/,
  '字段类型颜色只能从正式规则格子解析，不可填写格子不能附加类型颜色。'
)

assert.ok(
  dialog.includes('[typeClassName]: Boolean(typeClassName)'),
  '规则单元格必须按类型 tone 附加 class，让不同类型能有不同底色。'
)

assert.match(
  dialog,
  /if \(selectionMode === 'single' \|\| rawComponent\.includes\('select'\) \|\| rawComponent\.includes\('dropdown'\)\)[\s\S]*return 'select'/,
  'STRING + 下拉配置必须使用下拉框颜色，不能仍使用文本颜色。'
)

assert.match(
  dialog,
  /if \(rawComponent\.includes\('radio'\)\)[\s\S]*return 'radio'/,
  '单选控件必须使用单选框颜色。'
)

assert.ok(
  typeDisplayBlock.indexOf("rawComponent.includes('radio')") <
    typeDisplayBlock.indexOf("selectionMode === 'single'"),
  'radio 控件必须先于 generic single 选择模式识别，避免单选框误显示为下拉框。'
)

assert.match(
  dialog,
  /if \(rule\.valueType === 'BOOLEAN' \|\| rawComponent\.includes\('checkbox'\) \|\| selectionMode === 'multiple'\)[\s\S]*return 'checkbox'/,
  'BOOLEAN、checkbox 或多选控件必须使用复选框颜色。'
)

assert.match(
  dialog,
  /rawComponent\.includes\('upload-file'\)[\s\S]*rawComponent\.includes\('attachment'\)[\s\S]*Boolean\(rule\.attachmentRule\)[\s\S]*return 'attachment'/,
  '上传或附件规则必须使用附件颜色，不能混成普通文本颜色。'
)

console.log('PASS batch-record-fillable-cell-type-label-static')
