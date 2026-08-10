const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const panel = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const resetDraftHandler = panel.match(
  /const resetProductionSubmissionDraft\s*=\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst handleResetProduction)/
)?.[0]
assert.ok(
  resetDraftHandler,
  'production must own an explicit single-submission draft reset before exposing manual reset.'
)
assert.match(
  resetDraftHandler,
  /productionDraft\.outputQuantity\s*=\s*undefined/,
  'the next production submission must start without the previous output quantity.'
)
assert.match(
  resetDraftHandler,
  /productionDefectDraft\[defect\.key\]\s*=\s*0/,
  'the next production submission must start without the previous loss quantities.'
)
assert.match(
  resetDraftHandler,
  /delete deviceParameterDraft\[deviceKey\]/,
  'the next production submission must start without the previous device readings.'
)
assert.match(
  resetDraftHandler,
  /Object\.assign\(draft\.fieldValues,\s*createFrontlineDefaultValues\(context\.templateCode\)\)/,
  'the formal template payload draft must return to its initial values after success.'
)
assert.match(
  resetDraftHandler,
  /productionSubmitDraftKey\.value\s*=\s*createProductionSubmitDraftKey\(\)/,
  'a completed submission must rotate the client draft key for the next independent submission.'
)

assert.match(
  panel,
  /const FRONTLINE_PRODUCTION_IDEMPOTENCY_KEY_MAX_LENGTH\s*=\s*128/,
  'the frontend must own the formal event idempotency-key storage limit.'
)
const idempotencyBuilder = panel.match(
  /const buildFrontlineProductionSubmitIdempotencyKey\s*=\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst buildFrontlineFormalSubmitPayload)/
)?.[0]
assert.ok(idempotencyBuilder, 'production must build one bounded key from the active draft token.')
assert.match(
  idempotencyBuilder,
  /`frontline-submit-\$\{productionSubmitDraftKey\.value\}`/,
  'the key must use the session token instead of repeating the full route context.'
)
assert.match(
  idempotencyBuilder,
  /submitIdempotencyKey\.length\s*>\s*FRONTLINE_PRODUCTION_IDEMPOTENCY_KEY_MAX_LENGTH[\s\S]*throw new Error/,
  'an unexpected overlength key must fail before the formal request.'
)
assert.doesNotMatch(
  idempotencyBuilder,
  /route-|route-process-|workstation-|employee-/,
  'verbose context labels must not consume the 128-character event key budget.'
)

const submitHandler = panel.match(
  /const handleProductionFormalSubmit\s*=\s*async\s*\(\)\s*=>\s*\{[\s\S]*?(?=\nconst handleValidate)/
)?.[0]
assert.ok(submitHandler, 'production formal submit handler must exist.')
assert.strictEqual(
  (submitHandler.match(/ProFeedbackApi\.frontlineSubmit\(/g) || []).length,
  1,
  'one confirmation must continue to invoke the transactional endpoint exactly once.'
)
const submitRequestIndex = submitHandler.indexOf('await ProFeedbackApi.frontlineSubmit(formalPayload)')
const resetDraftIndex = submitHandler.indexOf('resetProductionSubmissionDraft()')
assert.ok(submitRequestIndex >= 0, 'the formal endpoint must be awaited before starting another session.')
assert.ok(
  resetDraftIndex > submitRequestIndex,
  'the draft and idempotency key may reset only after the formal endpoint explicitly succeeds.'
)
const finallyBlock = submitHandler.match(/finally\s*\{[\s\S]*?\}/)?.[0] || ''
assert.doesNotMatch(
  finallyBlock,
  /resetProductionSubmissionDraft|productionSubmitDraftKey/,
  'failed or uncertain requests must preserve the current draft and idempotency key.'
)
assert.doesNotMatch(
  submitHandler,
  /formalSubmitResult|isProductionSubmitted/,
  'a successful submission must not leave the production page in a persistent submitted lock.'
)

const submitButton = panel.match(
  /<button\s+class="frontline-production-submit-button submit-btn"[\s\S]*?<\/button>/
)?.[0]
assert.ok(submitButton, 'production submit button must exist.')
assert.doesNotMatch(
  submitButton,
  /isProductionSubmitted|is-submitted|data-formal-/,
  'the next operator must see the original submit control instead of the prior receipt state.'
)
assert.doesNotMatch(
  panel,
  /const isProductionSubmitted|formalSubmitResult/,
  'production controls must not be permanently disabled by the previous successful receipt.'
)

const selectionHeader = panel.match(
  /<header[\s\S]*?data-frontline-production-selection-grid[\s\S]*?<\/header>/
)?.[0]
assert.ok(selectionHeader, 'production process and employee selection header must exist.')
assert.strictEqual(
  (selectionHeader.match(/:disabled="payloadLoading \|\| submitConfirmationOpen \|\| productionSubmitSuccessOpen \|\| productionSubmitFailureOpen"/g) || []).length,
  2,
  'process and employee switching must remain locked through success or password-failure acknowledgement, then reopen.'
)

console.log('PASS: frontline production resets for independent repeated submissions')
