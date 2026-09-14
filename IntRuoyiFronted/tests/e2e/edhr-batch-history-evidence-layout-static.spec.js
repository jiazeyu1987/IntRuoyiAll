const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue'),
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
  'edhr-form-trace-batch-trace__technical-proof',
  'edhr-form-trace-batch-trace__proof-grid',
  'resolveAttachmentTechnicalProofs(attachment)',
  '文件 SHA-256',
  '附件账本哈希'
]) {
  assertIncludes(token, `表单追溯批记录详情必须把哈希证据收敛到技术校验区：${token}`)
}

for (const forbiddenDefaultFact of [
  '<dt>sourceDocHash</dt>',
  '<dt>sha256</dt>',
  '<dt>attachmentHash</dt>'
]) {
  assertExcludes(forbiddenDefaultFact, `表单追溯批记录详情默认摘要不得直接展示技术哈希字段：${forbiddenDefaultFact}`)
}

for (const businessFact of [
  '<dt>上传人</dt>',
  '<dt>上传时间</dt>'
]) {
  assertIncludes(businessFact, `表单追溯批记录详情默认摘要必须保留业务证据：${businessFact}`)
}

console.log('PASS: EDHR form trace visual record evidence layout static contract is satisfied.')
