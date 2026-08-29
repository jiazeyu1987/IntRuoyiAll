const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const componentPath = path.join(
  frontendRoot,
  'src/views/mes/pro/batchrecordformlist/index.vue'
)
const source = fs.readFileSync(componentPath, 'utf8').replace(/\r\n/g, '\n')
const apiSource = fs.readFileSync(
  path.join(frontendRoot, 'src/api/mes/pro/batchrecordreport/index.ts'),
  'utf8'
)

const dialogStart = source.indexOf('title="导入 Word"')
const dialogEnd = source.indexOf('</el-dialog>', dialogStart)

assert.ok(dialogStart > 0, '必须存在导入 Word 弹窗。')
assert.ok(dialogEnd > dialogStart, '必须能定位导入 Word 弹窗结束位置。')

const importDialogTemplate = source.slice(dialogStart, dialogEnd)

assert.match(
  importDialogTemplate,
  /<el-form-item label="导入类型" required>[\s\S]*?<el-radio-group[\s\S]*?v-model="wordImportDialog\.selectedFormSlotType"[\s\S]*?<el-radio-button value="MAIN">[\s\S]*?批记录[\s\S]*?<el-radio-button value="FORM">[\s\S]*?表单/,
  '导入 Word 弹窗必须显示批记录/表单导入类型选择。'
)

assert.match(
  importDialogTemplate,
  /<el-form-item v-if="isMainWordImport" label="产品名称" required>[\s\S]*?v-model="wordImportDialog\.selectedDccProjectCodeId"/,
  '选择批记录时必须保留产品名称 DCC 下拉，保证原批记录流程不变。'
)

assert.match(
  importDialogTemplate,
  /<el-form-item v-else label="表单名称" required>[\s\S]*?<el-input[\s\S]*?v-model="wordImportDialog\.formName"/,
  '选择表单时必须用表单名称输入框承接普通表单归属，不得强制依赖 DCC 项目名称。'
)

assert.match(
  importDialogTemplate,
  /<el-form-item label="Word 文件" required>/,
  '导入类型切换后必须继续保留 Word 文件。'
)

assert.match(
  source,
  /const DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE:\s*BatchRecordFormSlotType\s*=\s*'MAIN'/,
  '默认导入类型必须继续是批记录，避免影响现有入口。'
)

assert.match(
  source,
  /const UNIFIED_FORM_WORD_IMPORT_FORM_SLOT_TYPE:\s*BatchRecordFormSlotType\s*=\s*'FORM'/,
  '普通表单导入必须使用正式 FORM 类型进入统一列表。'
)

assert.match(
  source,
  /selectedFormSlotType:\s*DEFAULT_WORD_IMPORT_FORM_SLOT_TYPE[\s\S]*?formName:\s*''/,
  '导入弹窗状态必须同时初始化类型和普通表单名称。'
)

assert.match(
  apiSource,
  /export type BatchRecordFormSlotType = 'MAIN' \| 'FORM' \| 'LOSS_REPORT' \| 'PROCESS_INSPECTION' \| 'PARAMETER_RECORD'/,
  '前端 API 类型必须包含 FORM，保证列表、筛选、删除和上传参数共用同一类型。'
)

console.log('PASS: batch record Word import form type is visible and defaults to main')
