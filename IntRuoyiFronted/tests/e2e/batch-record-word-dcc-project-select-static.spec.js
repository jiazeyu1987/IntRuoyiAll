const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(__dirname, '../../src/views/mes/pro/batchrecordformlist/index.vue')
const pageSource = fs.readFileSync(pagePath, 'utf8')

assert(
  pageSource.includes("@/api/dcc/controlledFile/projectCodes"),
  '导入 Word 项目候选必须来自 DCC 项目代码接口，而不是生产工单产品名称候选。'
)

assert(
  pageSource.includes('selectedDccProjectCodeId'),
  '导入 Word 弹窗必须用单个 DCC 项目代码 ID 选择值承载正式 DCC 记录。'
)

assert(
  pageSource.includes('loadWordImportProjectOptions'),
  '导入 Word 弹窗必须加载 DCC 项目名称下拉候选。'
)

assert(
  !pageSource.includes('ProWorkOrderApi.getWorkOrderProductNameOptions'),
  '导入 Word 不得继续使用生产工单产品名称 remote combobox 候选。'
)

assert(
  !pageSource.includes('wordImportDialog.productNames'),
  '导入 Word 不得继续维护多选产品名称字段。'
)

assert(
  !pageSource.includes('wordImportDialog.batchRecordName'),
  '导入 Word 不得继续维护独立批记录名称输入字段。'
)

assert(
  !pageSource.includes('multiple\n            filterable\n            remote'),
  '导入 Word 不得继续使用 remote multiple combobox 形态选择对应产品名称。'
)

const dccSelectPattern =
  /<el-form-item v-if="isMainWordImport" label="产品名称" required>[\s\S]*?<el-select[\s\S]*?v-model="wordImportDialog\.selectedDccProjectCodeId"[\s\S]*?>[\s\S]*?<el-option[\s\S]*?:label="item\.projectName"[\s\S]*?:value="item\.id"[\s\S]*?>[\s\S]*?item\.projectCode[\s\S]*?<\/el-option>[\s\S]*?<\/el-select>[\s\S]*?<\/el-form-item>/

assert(
  dccSelectPattern.test(pageSource),
  '导入 Word 弹窗必须提供来自 DCC 项目代码的产品名称单选下拉，并以 DCC 记录 ID 作为选项值。'
)

assert(
  pageSource.includes('const batchRecordName = selectedSubjectName'),
  '提交导入时批记录名称必须直接使用选中的 DCC 项目名称。'
)

assert(
  pageSource.includes('const productNames = [selectedSubjectName]'),
  '提交导入时对应产品名称必须与选中的 DCC 项目名称合并为同一条。'
)

assert(
  !pageSource.includes('wordImportDialog.selectedProjectName'),
  '导入 Word 不得维护脱离 DCC 记录 ID 的项目名称选择状态。'
)

console.log('PASS: batch-record Word import DCC project select static contract')
