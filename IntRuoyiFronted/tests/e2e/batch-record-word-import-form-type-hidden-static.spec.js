const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecordformlist/index.vue'
)
const source = fs.readFileSync(componentPath, 'utf8').replace(/\r\n/g, '\n')

const dialogStart = source.indexOf('title="导入 Word"')
const dialogEnd = source.indexOf('</el-dialog>', dialogStart)

assert.ok(dialogStart > 0, '必须存在导入 Word 弹窗。')
assert.ok(dialogEnd > dialogStart, '必须能定位导入 Word 弹窗结束位置。')

const importDialogTemplate = source.slice(dialogStart, dialogEnd)

assert.doesNotMatch(
  importDialogTemplate,
  /<el-form-item label="表单类型"/,
  '导入 Word 弹窗不得显示表单类型整行。'
)

assert.match(
  importDialogTemplate,
  /<el-form-item label="产品名称" required>/,
  '隐藏表单类型后必须保留产品名称。'
)

assert.match(
  importDialogTemplate,
  /<el-form-item label="Word 文件" required>/,
  '隐藏表单类型后必须保留 Word 文件。'
)

assert.match(
  source,
  /const DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE:\s*BatchRecordFormSlotType\s*=\s*'MAIN'/,
  '隐藏表单类型后内部导入类型必须继续固定为 MAIN。'
)

assert.match(
  source,
  /selectedFormSlotType:\s*DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE/,
  '导入弹窗状态必须继续初始化为 MAIN。'
)

console.log('PASS: batch record Word import form type is hidden and defaults to main')

