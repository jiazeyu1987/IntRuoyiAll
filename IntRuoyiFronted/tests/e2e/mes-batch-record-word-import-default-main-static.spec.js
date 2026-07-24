const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecordformlist/index.vue'
)
const source = fs.readFileSync(componentPath, 'utf8')

const dialogStart = source.indexOf('title="导入 Word"')
const dialogEnd = source.indexOf('</el-dialog>', dialogStart)

assert.ok(dialogStart > 0, '必须存在导入 Word 弹窗。')
assert.ok(dialogEnd > dialogStart, '必须能定位导入 Word 弹窗结束位置。')

const importDialogTemplate = source.slice(dialogStart, dialogEnd)

assert.match(
  importDialogTemplate,
  /<el-form-item label="表单类型" required>[\s\S]*?v-model="wordImportDialog\.selectedFormSlotType"[\s\S]*?batch-record-word-import-form__slot-select/,
  '导入 Word 弹窗必须展示表单类型选择项，以便触发附加槽位上传。'
)

assert.match(
  source,
  /const formSlotTypeOptions[\s\S]*?MAIN[\s\S]*?LOSS_REPORT[\s\S]*?PROCESS_INSPECTION[\s\S]*?PARAMETER_RECORD/,
  '导入 Word 弹窗必须提供批记录、损耗单、过程检验单和参数记录表。'
)

assert.match(
  source,
  /const DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE:\s*BatchRecordFormSlotType\s*=\s*'MAIN'/,
  '导入 Word 必须用 MAIN 作为默认批记录表单类型。'
)

assert.match(
  source,
  /selectedFormSlotType:\s*DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE/,
  '导入 Word 弹窗状态必须默认选中批记录表单类型。'
)

assert.match(
  source,
  /wordImportDialog\.selectedFormSlotType\s*=\s*DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE/,
  '打开或重置导入 Word 弹窗时必须恢复批记录表单类型。'
)

assert.doesNotMatch(
  importDialogTemplate,
  /:disabled="[^"]*selectedFormSlotType/,
  '导入 Word 弹窗中的产品名称与文件按钮不得再被隐藏的表单类型禁用。'
)

assert.match(
  source,
  /if \(!wordImportDialog\.selectedFormSlotType\)[\s\S]*?请选择表单类型/,
  '导入 Word 交互必须在确认时校验表单类型。'
)

console.log('PASS: batch record Word import defaults to main form with visible type selector')
