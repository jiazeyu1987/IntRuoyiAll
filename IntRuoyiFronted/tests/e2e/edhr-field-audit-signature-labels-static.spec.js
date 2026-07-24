const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/edhr/FieldAuditDetailPage.vue'),
  'utf8'
)

const assertIncludes = (token, message) => {
  assert.ok(page.includes(token), message)
}

const assertExcludes = (token, message) => {
  assert.ok(!page.includes(token), message)
}

for (const token of [
  'FIELD_CHANGE_ACTION_LABEL',
  'FIELD_CHANGE_ACTION_CODE',
  '字段变更',
  '动作码',
  'FIELD_CHANGE'
]) {
  assertIncludes(token, `字段审计详情必须同时提供中文签名动作和技术动作码证据：${token}`)
}

assertExcludes(
  '<div class="edhr-field-audit-detail__muted">FIELD_CHANGE</div>',
  '变更明细签名列不得把 FIELD_CHANGE 作为主文案。'
)
assertExcludes(
  '<el-descriptions-item label="签名动作">FIELD_CHANGE</el-descriptions-item>',
  '签名与链路校验区不得把 FIELD_CHANGE 作为签名动作主文案。'
)

assertIncludes(
  '<el-descriptions-item label="动作码">{{ FIELD_CHANGE_ACTION_CODE }}</el-descriptions-item>',
  '签名与链路校验区必须保留 FIELD_CHANGE 动作码证据。'
)

console.log('PASS: EDHR field audit signature labels static contract is satisfied.')
