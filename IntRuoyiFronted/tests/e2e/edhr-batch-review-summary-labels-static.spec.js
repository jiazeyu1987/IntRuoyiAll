const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const reviewPage = read('src/views/mes/pro/edhr-batch/BatchExecutionReviewPage.vue')
const historyPage = read('src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue')
const presentationPath = path.join(repoRoot, 'src/views/mes/pro/edhr-batch/executionReviewPresentation.ts')
assert.ok(fs.existsSync(presentationPath), '批次复盘必须提供统一展示辅助文件 executionReviewPresentation.ts')
const presentation = fs.readFileSync(presentationPath, 'utf8')
const pages = [
  ['批次复盘页', reviewPage],
  ['历史批记录页', historyPage]
]

const assertIncludes = (content, token, message) => {
  assert.ok(content.includes(token), message)
}

const assertExcludes = (content, token, message) => {
  assert.ok(!content.includes(token), message)
}

for (const [name, page] of pages) {
  for (const token of [
    'resolveExecutionSummaryItems(selectedExecution)',
    'resolveExecutionStatusText(selectedExecution.status)'
  ]) {
    assertIncludes(page, token, `${name}必须使用中文复盘摘要和中文状态：${token}`)
  }

  assertExcludes(
    page,
    '{{ selectedExecution.status ?? \'--\' }}',
    `${name}执行明细不得直接展示原始数字状态。`
  )
}

for (const token of ['字段变更', '表单复核', '提交签名', '审批签名']) {
  assertIncludes(presentation, token, `统一复盘展示辅助必须提供中文签名摘要：${token}`)
}

for (const forbiddenSummaryCode of [
  'FIELD_CHANGE {{',
  'FORM_REVIEW {{',
  'SUBMIT {{',
  'APPROVE {{'
]) {
  assertExcludes(reviewPage, forbiddenSummaryCode, `批次复盘摘要不得展示英文动作码：${forbiddenSummaryCode}`)
  assertExcludes(historyPage, forbiddenSummaryCode, `历史批记录摘要不得展示英文动作码：${forbiddenSummaryCode}`)
}

console.log('PASS: EDHR batch review summary labels static contract is satisfied.')
