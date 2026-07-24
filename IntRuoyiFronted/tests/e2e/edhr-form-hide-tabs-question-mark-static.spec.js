const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const readonlyForm = read('src/views/mes/pro/edhr/components/EdhrExecutionReadonlyForm.vue')
const templatePage = read('src/views/mes/pro/batchrecordformlist/index.vue')

assert.ok(
  !detailPage.includes('<div class="edhr-batch-detail__rail-actions">'),
  '批次详情右侧栏顶部不得显示“基础 / 详情”操作区'
)
assert.ok(
  !detailPage.includes('@click="basicInfoDialogVisible = true">基础</el-button>'),
  '批次详情右侧栏不得显示“基础”按钮'
)
assert.ok(
  !detailPage.includes('@click="processDetailDialogVisible = true">详情</el-button>'),
  '批次详情右侧栏不得显示“详情”按钮'
)
assert.ok(
  readonlyForm.includes("placeholderText === '请填写' ? '?' : placeholderText"),
  '统一只读表单必须把“请填写”提示替换为“?”'
)
assert.ok(
  readonlyForm.includes("return placeholderText || '?'"),
  '空填写格没有自定义提示时必须显示“?”'
)
assert.ok(
  !readonlyForm.includes(": '请填写'"),
  '统一只读表单不得继续使用“请填写”作为默认提示'
)
assert.ok(
  templatePage.includes("value: '?'") && templatePage.includes("valueDisplay: '?'"),
  '表单模板预览必须同步使用“?”标识空填写格'
)
assert.ok(
  !templatePage.includes("value: '请填写'") && !templatePage.includes("valueDisplay: '请填写'"),
  '表单模板预览不得继续注入“请填写”'
)

console.log('PASS: EDHR form hides rail tabs and uses question-mark fill prompt')
