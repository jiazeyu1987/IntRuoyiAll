const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue'),
  'utf8'
)

const assertIncludes = (token, message) => {
  assert.ok(page.includes(token), message)
}

const assertExcludes = (token, message) => {
  assert.ok(!page.includes(token), message)
}

for (const token of [
  '技术校验',
  'edhr-batch-history__technical-proof',
  'edhr-batch-history__proof-grid',
  'resolveDossierTechnicalProofs(item)',
  'resolveAttachmentTechnicalProofs(attachment)',
  '来源校验哈希',
  '文件 SHA-256',
  '附件账本哈希'
]) {
  assertIncludes(token, `历史批记录必须把哈希证据收敛到技术校验区：${token}`)
}

for (const forbiddenDefaultFact of [
  '<dt>sourceDocHash</dt>',
  '<dt>sha256</dt>',
  '<dt>attachmentHash</dt>'
]) {
  assertExcludes(forbiddenDefaultFact, `历史批记录默认摘要不得直接展示技术哈希字段：${forbiddenDefaultFact}`)
}

for (const businessFact of [
  '<dt>来源单据</dt>',
  '<dt>检验结果</dt>',
  '<dt>完成时间</dt>',
  '<dt>上传人</dt>',
  '<dt>上传时间</dt>'
]) {
  assertIncludes(businessFact, `历史批记录默认摘要必须保留业务证据：${businessFact}`)
}

console.log('PASS: EDHR batch history evidence layout static contract is satisfied.')
