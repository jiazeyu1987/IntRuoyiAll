const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = readUtf8('src/api/mes/pro/feedback/index.ts')

assert.match(
  feedbackApi,
  /frontlineSubmit\s*:\s*async/,
  'frontline feedback API must expose the formal frontlineSubmit wrapper.'
)
assert.match(
  feedbackApi,
  /\/mes\/pro\/feedback\/frontline\/submit/,
  'frontlineSubmit must call the formal one-step frontline submit endpoint.'
)

assert.match(
  panel,
  /import\s+\{[\s\S]*ProFeedbackApi[\s\S]*\}\s+from\s+'@\/api\/mes\/pro\/feedback'/,
  'employee fill panel must import the formal ProFeedbackApi, not only the template validation API.'
)
assert.match(
  panel,
  /buildFrontlineFormalSubmitPayload/,
  'employee fill panel must build an explicit formal submit payload.'
)
assert.match(
  panel,
  /assertFrontlineFormalSubmitContext/,
  'employee fill panel must fail fast when formal submit prerequisites are missing.'
)
assert.match(
  panel,
  /assertProductionSubmissionReady/,
  'production submit must run local business readiness validation before opening confirmation.'
)
assert.match(
  panel,
  /请填写完成数量/,
  'production submit must give a precise validation message when output quantity is missing.'
)
assert.match(
  panel,
  /请填写设备参数/,
  'production submit must reject missing numeric device parameter readings before sending a request.'
)
const productionReadiness = panel.match(
  /const assertProductionSubmissionReady\s*=\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst buildProductionFormalSubmitConfirmation)/
)?.[0]
assert.ok(productionReadiness, 'production submission readiness validator must exist.')
assert.doesNotMatch(
  productionReadiness,
  /当前工序缺少正式设备配置，无法提交/,
  'a process without formal equipment must not be rejected only because no device is configured.'
)
assert.match(
  productionReadiness,
  /const device = activeProductionDevice\.value[\s\S]*if \(!device\) \{\s*return\s*\}/,
  'device parameter validation must be skipped only when the current process has no formal device.'
)
const formalContextAssertion = panel.match(
  /const assertFrontlineFormalSubmitContext\s*=\s*\([\s\S]*?(?=\nconst buildFrontlineFormalSubmitPayload)/
)?.[0]
assert.ok(formalContextAssertion, 'formal submit context assertion must exist.')
assert.doesNotMatch(
  formalContextAssertion,
  /\['deviceId',\s*'设备'\]/,
  'formal submit context must not make deviceId mandatory for device-free processes.'
)
assert.match(
  panel,
  /deviceId:\s*formalContext\.deviceId,\s*\n\s*deviceAccountUserId:/,
  'process-pool payload must preserve an optional deviceId without a non-null assertion.'
)
assert.match(
  panel,
  /data-production-submit-confirmation-dialog[\s\S]*确认正式提交/,
  'production submit must show an explicit irreversible confirmation with the formal summary inside the component.'
)
assert.doesNotMatch(
  panel.match(/const handleProductionFormalSubmit\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst assertPqcFormalSubmissionReady)/)?.[0] || '',
  /message\.confirm|ElMessageBox/,
  'production formal submit must not rely on body-mounted global MessageBox in fullscreen mode.'
)
assert.match(
  panel,
  /正式提交后不可修改/,
  'formal confirmation content must state that a successful formal submission cannot be edited.'
)
assert.match(
  panel,
  /formalSubmitResult\.value\s*=\s*await\s+ProFeedbackApi\.frontlineSubmit\(/,
  'submit action must persist the resolved formal submit result instead of discarding it.'
)
assert.match(
  panel,
  /feedbackPayload[\s\S]*recordbookPayload[\s\S]*processPoolContext[\s\S]*actualEmployeeId[\s\S]*signatureId[\s\S]*signatureEmployeeId[\s\S]*rawPayload/,
  'formal submit payload must include feedback, recordbook, process-pool, signature, employee, and raw payload sections.'
)
assert.match(
  panel,
  /deviceAccountUserId:\s*Number\(userStore\.getUser\?\.id/,
  'process-pool context must use the current login user as the device account user.'
)
assert.match(
  panel,
  /feedbackId[\s\S]*recordbookEntryId[\s\S]*processPoolEventId/,
  'the submitted state must retain the formal feedback, recordbook, and process-pool identifiers.'
)
assert.match(
  panel,
  /isProductionSubmitted[\s\S]*已正式提交/,
  'the production controls must enter a persistent submitted state after success.'
)
const productionSubmitHandler = panel.match(
  /const handleProductionFormalSubmit\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst handleValidate)/
)?.[0]
assert.ok(productionSubmitHandler, 'production formal submit handler must be implemented.')
assert.doesNotMatch(
  productionSubmitHandler,
  /FrontlineTemplateApi\.validatePayload/,
  'production formal submit must not send a separate pre-validation write request before the transactional endpoint.'
)
assert.strictEqual(
  (productionSubmitHandler.match(/ProFeedbackApi\.frontlineSubmit\(/g) || []).length,
  1,
  'one confirmed action must invoke the formal submit endpoint exactly once.'
)

console.log('PASS: frontline formal submit static contract is wired')
