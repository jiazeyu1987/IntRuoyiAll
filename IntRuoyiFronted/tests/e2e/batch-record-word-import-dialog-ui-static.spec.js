const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const readPage = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const formListSource = readPage('src/views/mes/pro/batchrecordformlist/index.vue')
const templateSource = readPage('src/views/mes/pro/batchrecordformlist/index.vue')

assert.ok(
  formListSource.includes('handleWordImportFileSelect') &&
    formListSource.includes('wordImportDialog.file = file'),
  '批记录表单列表导入弹窗仍必须通过按钮选择并保存真实 Word 文件。'
)

assert.ok(
  formListSource.includes('batch-record-word-import-form__file-state') &&
    formListSource.includes('wordImportDialog.file?.name') &&
    formListSource.includes('已选择 Word 文件'),
  'Word 文件选择后必须在按钮右侧显示已选文件名，避免用户误判为无响应。'
)

assert.ok(
  formListSource.includes('wordImportDialog.preflightLoading') &&
    formListSource.includes('正在预检 Word 文件') &&
    formListSource.includes('wordImportDialog.preflightErrorMessage'),
  '主批记录 Word 选择后必须显示预检中或预检错误状态。'
)

assert.ok(
  !formListSource.includes('batch-record-word-import-form__file-tip') &&
    !formListSource.includes('批记录仅支持 .doc 文件') &&
    !formListSource.includes('附加表单支持 .doc、.docx 文件'),
  'Word 文件行不得继续显示黄色框内的格式提示文本。'
)

assert.ok(
  formListSource.includes('<el-form-item v-if="isMainWordImport" label="导入内容">') &&
    formListSource.includes('批记录表单') &&
    formListSource.includes('<div class="batch-record-word-import-form__route-title">工艺流程</div>') &&
    formListSource.includes('wordImportDialog.preflight.routeProductOptions'),
  '批记录表单列表导入弹窗必须恢复批记录表单与工艺流程选择。'
)

for (const [pageName, pageSource] of [
  ['批记录表单列表', formListSource],
  ['批记录模板页', templateSource]
]) {
  assert.ok(
    pageSource.includes('selectedRouteProductOptionKeys: [] as string[]'),
    `${pageName} 导入弹窗必须保留用户显式勾选产线的状态容器。`
  )
  assert.ok(
    !pageSource.includes('wordImportDialog.selectedRouteProductOptionKeys = preflight.routeProductOptions.map((item) => item.optionKey)'),
    `${pageName} 预检成功后不得默认勾选重建产线候选。`
  )
  assert.match(
    pageSource,
    /wordImportDialog\.preflight = preflight[\s\S]*?wordImportDialog\.selectedRouteProductOptionKeys = \[\]/,
    `${pageName} 预检成功后必须显式保持重建产线候选未选中。`
  )
}

console.log('PASS: batch-record Word import dialog UI static contract')
