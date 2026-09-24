const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const panelPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8')

const signatureFormatter = panel.match(
  /const formatOperationFactSignatureText = \([\s\S]*?\r?\n\r?\nconst formatMarketReleaseSignatureCellText/
)?.[0]
assert(signatureFormatter, '操作事实电子签名格式化函数必须存在')
assert.doesNotMatch(
  signatureFormatter,
  /qaSignature/,
  '操作事实电子签名不得优先显示遗留 qaSignature 文本'
)
assert.match(
  signatureFormatter,
  /formatActiveOrderSignatureCellText\(toOperationFactSignature\(fact\)\)/,
  '操作事实电子签名必须复用正式签名人和签名时间格式'
)

const evidenceBlockStart = panel.indexOf('data-active-order-summary-operation-signature')
const evidenceBlockEnd = panel.indexOf('</button>', evidenceBlockStart)
assert(evidenceBlockStart >= 0 && evidenceBlockEnd >= 0, '操作事实电子签名入口必须存在')
const evidenceBlock = panel.slice(evidenceBlockStart, evidenceBlockEnd)
assert.match(evidenceBlock, /:disabled="!fact\.signatureId"/, '缺少签名 ID 时必须禁用跳转')
assert.match(
  evidenceBlock,
  /openActiveOrderSignatureRecord\(toOperationFactSignature\(fact\)\)/,
  '操作事实电子签名必须按正式签名 ID 跳转'
)

console.log('PASS: active-order signature display consistency static contract')
