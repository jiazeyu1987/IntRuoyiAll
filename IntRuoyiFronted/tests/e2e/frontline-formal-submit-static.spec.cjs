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
  /ProFeedbackApi\.frontlineSubmit\(\s*buildFrontlineFormalSubmitPayload\(/,
  'submit action must send the employee payload through ProFeedbackApi.frontlineSubmit.'
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
  /ProFeedbackApi\.frontlineSubmit\([\s\S]*?\)\s*[\r\n\s]*message\.success\('已提交'\)/,
  'success feedback must happen only after the formal frontlineSubmit call resolves.'
)

console.log('PASS: frontline formal submit static contract is wired')
