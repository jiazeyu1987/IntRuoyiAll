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
  feedbackApi,
  /FrontlineProductionSubmitContextVO[\s\S]*productionSubmitContext/s,
  'runtime config API must expose server-resolved production submit context instead of URL-only formal fields.'
)
assert.match(
  feedbackApi,
  /signaturePassword:\s*string/,
  'frontline production submit request must carry the employee signature password for server-side signature creation.'
)
assert.match(
  feedbackApi,
  /ProFrontlineFeedbackMaterialReqVO[\s\S]*materialId:\s*number[\s\S]*outputQuantity:\s*number[\s\S]*lossQuantity:\s*number[\s\S]*deviceParameterReadings/,
  'formal submission API must carry one independent fact for every process material.'
)
assert.match(
  feedbackApi,
  /ProFrontlineFeedbackSubmitReqVO[\s\S]*materialDetails:\s*ProFrontlineFeedbackMaterialReqVO\[\]/,
  'formal submission must send all material facts in one request.'
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
  /deviceState\.runtimeConfig\?\.productionSubmitContext/,
  'formal production context must be read from backend runtime config.'
)
assert.doesNotMatch(
  panel,
  /recordbookId:\s*firstRouteQueryNumber|signatureId:\s*firstRouteQueryNumber|taskId:\s*firstRouteQueryNumber/,
  'formal production context must not depend on URL query for recordbook, signature, or task identity.'
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
  /const progressQuantity = Math\.min\(\.\.\.materialDetails\.map\(\(material\) => material\.outputQuantity\)\)/,
  'the frontend summary must calculate process progress from the minimum material output quantity.'
)
assert.match(
  panel,
  /materialDetails,\s*\n\s*feedbackPayload:/,
  'all process material facts must be included in the same formal submit request.'
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
assert.doesNotMatch(
  formalContextAssertion,
  /\['workOrderId', '订单上下文'\]|\['taskId', '生产任务'\]|\['itemId', '产品物料'\]|\['recordbookId', '记录本'\]/,
  'formal submit context must not make work order, task, item, or recordbook mandatory for frontline production.'
)
assert.match(
  panel,
  /deviceId:\s*formalContext\.deviceId,\s*\n\s*deviceAccountUserId:/,
  'process-pool payload must preserve an optional deviceId without a non-null assertion.'
)
assert.match(
  panel,
  /const recordbookPayload = formalContext\.recordbookId[\s\S]*: undefined/,
  'recordbook payload must be optional when frontline production has no recordbook context.'
)
assert.match(
  panel,
  /data-production-submit-confirmation-dialog[\s\S]*确认正式提交/,
  'production submit must show an explicit irreversible confirmation with the formal summary inside the component.'
)
assert.match(
  panel,
  /data-production-submit-signature-password[\s\S]*productionSignaturePassword/,
  'production submit confirmation must collect the actual employee signature password before calling the formal endpoint.'
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
  /await\s+ProFeedbackApi\.frontlineSubmit\(formalPayload\)[\s\S]*resetProductionSubmissionDraft\(\)/,
  'submit action must start a clean independent draft only after the formal endpoint succeeds.'
)
assert.match(
  panel,
  /feedbackPayload[\s\S]*recordbookPayload[\s\S]*processPoolContext[\s\S]*actualEmployeeId[\s\S]*signatureEmployeeId[\s\S]*signaturePassword[\s\S]*rawPayload/,
  'formal submit payload must include feedback, optional recordbook, process-pool, signature password, employee, and raw payload sections.'
)
assert.doesNotMatch(
  panel,
  /signatureId:\s*formalContext\.signatureId/,
  'production submit must not require a pre-supplied signature id; the backend must create it from the signature password.'
)
assert.match(
  panel,
  /deviceAccountUserId:\s*Number\(userStore\.getUser\?\.id/,
  'process-pool context must use the current login user as the device account user.'
)
const submitButtonBlock = panel.match(
  /<button\s+class="frontline-production-submit-button submit-btn"[\s\S]*?<\/button>/
)?.[0]
assert.ok(submitButtonBlock, 'the production submit button must be implemented.')
assert.match(
  panel,
  /const productionSubmitSuccessText\s*=\s*computed\(\(\)\s*=>\s*`\$\{selectedEmployeeLabel\.value\}提交成功`\)/,
  'the production success prompt must identify the actual selected employee.'
)
assert.doesNotMatch(
  submitButtonBlock,
  /isProductionSubmitted|is-submitted|data-formal-|已正式提交|<small|报工\s|记录本\s|工序池\s/,
  'the submit button must return to its original state for the next employee and process.'
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
assert.match(
  productionSubmitHandler,
  /resetProductionSubmissionDraft\(\)[\s\S]*openProductionSubmitSuccessDialog\(\)/,
  'the fullscreen-root success dialog must open only after the next independent draft is ready.'
)
assert.doesNotMatch(
  productionSubmitHandler,
  /message\.success/,
  'production success must not depend on a body-mounted toast that fullscreen can cover.'
)

console.log('PASS: frontline formal submit static contract is wired')
