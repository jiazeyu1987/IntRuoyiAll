const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/feedback/index.ts'), 'utf8')
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

for (const field of [
  'parameterAuditStatus',
  'parameterAuditTotalCount',
  'parameterAuditResolvedCount',
  'parameterAuditUnresolvedCount',
  'auditItems'
]) {
  assert.match(api, new RegExp(`\\b${field}\\b`), `feedback response must expose ${field}`)
}
assert.match(api, /snapshotSource:\s*'FROZEN'\s*\|\s*'MISSING_LEGACY'\s*\|\s*'CURRENT_ROUTE_PROCESS_AT_SUBMIT'/)
assert.match(panel, /const PARAMETER_AUDIT_REASON_TEXT\s*=\s*\{/)
assert.match(panel, /throw new Error\(`未知设备参数审计原因/)
assert.match(
  panel,
  /const submitResult = await ProFeedbackApi\.frontlineSubmit\(formalPayload\)[\s\S]*openProductionSubmitSuccessDialog\(\)[\s\S]*showParameterAuditWarning\(submitResult\)/,
  'formal feedback success must be shown before any UNRESOLVED parameter warning'
)
assert.match(panel, /if \(!unresolvedItems\.length\) \{\s*return\s*\}/)

console.log('mes-device-parameter-snapshot-static.spec.js passed')
