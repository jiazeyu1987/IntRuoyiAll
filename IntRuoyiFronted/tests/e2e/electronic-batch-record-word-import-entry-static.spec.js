const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue'),
  'utf8'
)

assert.ok(
  !page.includes('batch-record-toolbar-shell'),
  '电子批记录页不应恢复旧的顶部大工具栏'
)

assert.match(
  page,
  /<el-button[\s\S]*?class="batch-record-form-toolbar__import-button"[\s\S]*?@click="openWordImportDialog"[\s\S]*?>[\s\S]*?导入[\s\S]*?<\/el-button>/,
  '电子批记录页必须在标准列表工具栏中提供导入入口按钮'
)

assert.ok(page.includes(':loading="wordImporting"'), '导入按钮必须复用现有导入中状态')
assert.ok(page.includes("'.doc'") && page.includes("'.doc,.docx'"), '导入入口必须按表单类型限制为 Word 文件')
assert.ok(page.includes('const openWordImportDialog = async () => {'), '页面必须保留导入弹窗触发函数')
assert.ok(page.includes('const handleImportFileChange = async (event: Event) => {'), '页面必须保留现有文件导入处理函数')
assert.ok(
  page.includes('BatchRecordReportApi.recognizeUploadedRoute(') &&
    page.includes('runUploadedWordImport(file, batchRecordName, productNames'),
  '导入入口必须携带工艺路线对应产品名称复用现有 Word 识别导入接口'
)

assert.ok(page.includes('产品名称'), '导入确认弹窗必须要求选择产品名称')
assert.ok(page.includes("@/api/dcc/controlledFile/projectCodes"), '产品名称候选必须来自 DCC 项目代码接口')

console.log('PASS: electronic batch record word import entry static contract')
