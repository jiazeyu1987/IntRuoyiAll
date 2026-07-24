const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue')
const source = fs.readFileSync(pagePath, 'utf8')

for (const fragment of [
  'READINESS_BUSINESS_ACTIONS',
  'BPM_DEFINITION_MISSING',
  'BPM_NOTIFY_TEMPLATE_MISSING',
  'TEMPLATE_CELL_RULE_UNREVIEWED',
  'SIGNATURE_AUTH_MISSING',
  'PERMISSION_RULE_MISSING',
  'PERMISSION_SCOPE_MISSING'
]) {
  assert.ok(source.includes(fragment), `Readiness business action mapping must include ${fragment}.`)
}

for (const label of ['流程配置', '模板规则', '电子签名', '权限矩阵']) {
  assert.ok(source.includes(label), `Readiness blocker result must show business group ${label}.`)
}

assert(
  source.includes('resolveReadinessBusinessAction') &&
    source.includes('resolveReadinessBusinessGroup') &&
    source.includes('resolveReadinessBusinessNextStep'),
  'Readiness result table must resolve blocker code into business group and next-step actions.'
)

assert(
  source.includes('业务动作') &&
    source.includes('下一步') &&
    source.includes('row.suggestion'),
  'Readiness table must display a business action column while preserving backend suggestion detail.'
)

console.log('PASS: eDHR readiness business action static contract')
