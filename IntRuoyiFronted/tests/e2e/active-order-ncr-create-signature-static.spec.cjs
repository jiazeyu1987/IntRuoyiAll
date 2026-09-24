const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..', '..')
const reviewPage = fs.readFileSync(
  path.join(repoRoot, 'IntRuoyiFronted/src/views/mes/pro/edhr-nonconformance/NonconformanceReviewPage.vue'),
  'utf8'
)
const detailPanel = fs.readFileSync(
  path.join(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)
const createBlock = reviewPage.match(
  /<div v-if="canCreateEntry"[\s\S]*?(?=\n      <div class="edhr-ncr__layout")/
)?.[0]
assert(createBlock, '创建评审表单必须存在')
assert.match(createBlock, /label="电子签名密码"/, '创建评审必须要求电子签名密码')
assert.match(createBlock, /v-model="entryForm\.signaturePassword"/, '创建签名密码必须绑定创建表单')
assert.match(
  reviewPage,
  /createNonconformanceReview\(\{[\s\S]*signaturePassword\s*\}/,
  '创建请求必须提交签名密码'
)

const evidenceStart = detailPanel.indexOf('data-active-order-operation-ncr-evidence')
const evidenceEnd = detailPanel.indexOf('</tr>', evidenceStart)
const evidenceBlock =
  evidenceStart >= 0 && evidenceEnd >= 0 ? detailPanel.slice(evidenceStart, evidenceEnd) : ''
assert(evidenceBlock, '不合格事实详情区块必须存在')
assert.match(evidenceBlock, /不合格原因/, '详情须保留不合格原因')
assert.match(evidenceBlock, /电子签名/, '详情须使用电子签名名称')
assert.match(
  evidenceBlock,
  /v-if="fact\.operationType !== 'NONCONFORMANCE_REVIEW_CREATE'"[\s\S]*评审意见/,
  '创建事实不得显示评审意见'
)
assert.match(
  evidenceBlock,
  /v-if="fact\.operationType !== 'NONCONFORMANCE_REVIEW_CREATE'"[\s\S]*处置结果/,
  '创建事实不得显示处置结果'
)
assert.match(
  evidenceBlock,
  /fact\.operationType !== 'NONCONFORMANCE_REVIEW_CREATE'[\s\S]*data-active-order-ncr-review-material-preview/,
  '创建事实不得显示评审材料'
)
assert.doesNotMatch(evidenceBlock, /QA签名/, '详情不得再使用 QA 签名文案')
assert.match(
  evidenceBlock,
  /fact\.operationType\s*!==\s*'NONCONFORMANCE_REVIEW_CREATE'/,
  '处置字段和材料只应显示在处置事实中'
)

console.log('PASS: active-order NCR create signature static contract')
