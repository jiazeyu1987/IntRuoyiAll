const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const panel = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const feedbackApi = read('src/api/mes/pro/feedback/index.ts')

assert.match(
  feedbackApi,
  /frontlineSubmit:\s*async\s*\(data:\s*ProFrontlineFeedbackSubmitReqVO\)/,
  'feedback API must expose the formal frontlineSubmit wrapper.'
)
assert.match(
  feedbackApi,
  /resolveFrontlineSubmitContext:\s*async\s*\(params:\s*FrontlineSubmitContextReqVO\)/,
  'feedback API must expose the formal frontline submit context resolver.'
)

assert.match(
  panel,
  /ProFeedbackApi/,
  'frontline panel must import and use ProFeedbackApi for real submit.'
)
assert.match(
  panel,
  /ProFrontlineFeedbackSubmitReqVO/,
  'frontline panel must build the typed formal submit request.'
)

assert.match(
  panel,
  /await\s+ProFeedbackApi\.frontlineSubmit\(/,
  'frontline submit flow must call ProFeedbackApi.frontlineSubmit.'
)
const submitIndex = panel.indexOf('await ProFeedbackApi.frontlineSubmit(')
const successIndex = panel.indexOf("message.success('已提交')")
assert.ok(submitIndex >= 0, 'frontlineSubmit call must exist.')
assert.ok(successIndex > submitIndex, 'success message must be shown only after frontlineSubmit returns.')

const validateIndex = panel.indexOf('await FrontlineTemplateApi.validatePayload(')
assert.ok(
  validateIndex < 0 || validateIndex < submitIndex,
  'template payload validation may run only before the real submit call.'
)
assert.doesNotMatch(
  panel,
  /PQC 详细检验内容尚未纳入正式模板字段/,
  'PQC submit path must not be hard-blocked after simplified fields are available.'
)

assert.match(
  panel,
  /buildFrontlineSubmitRequest/,
  'frontline panel must centralize formal submit request construction.'
)
assert.match(
  panel,
  /ProTaskSelectDialog/,
  'frontline panel must select a real production task before building submit context.'
)
assert.match(
  panel,
  /resolveFrontlineSubmitContext/,
  'frontline panel must resolve report-work context from the formal backend resolver.'
)
assert.match(
  panel,
  /buildPqcFieldValues/,
  'frontline panel must convert simplified PQC inputs into formal payload fields.'
)

assert.match(
  panel,
  /const\s+requireNonNegativeNumber\s*=/,
  'frontline submit must use a dedicated non-negative quantity validator.'
)
assert.match(
  panel,
  /lossQuantity:\s*requireNonNegativeNumber\(lossQuantity,\s*'损耗数量'\)/,
  'frontline submit must accept zero loss while rejecting missing or negative loss.'
)
assert.doesNotMatch(
  panel,
  /laborScrapQuantity:\s*isPqcMode\.value\s*\?\s*undefined\s*:\s*lossQuantity/,
  'simplified total loss must not be duplicated into labor scrap.'
)
assert.match(
  panel,
  /laborScrapQuantity:\s*undefined/,
  'classified labor scrap must remain empty when the fixed template captures total loss only.'
)

for (const [field, pattern] of [
  ['production task', /submitContext\.value\?\.taskId/],
  ['item', /submitContext\.value\?\.itemId/],
  ['approver', /submitContext\.value\?\.approveUserId/],
  ['recordbook', /submitContext\.value\?\.recordbookId/],
  ['feedback type', /submitContext\.value\?\.feedbackType/]
]) {
  assert.match(panel, pattern, `${field} must come from formal backend submit context.`)
}
assert.doesNotMatch(
  panel,
  /(?:taskId|itemId|approveUserId|recordbookId|feedbackType)\s*[:=][^\n]*(?:\|\||\?\?)\s*[1-9]\d*/,
  'formal report-work context must not use numeric defaults or fallback IDs.'
)
assert.doesNotMatch(
  panel,
  /firstRouteQueryNumber\(\['taskId'[\s\S]*?recordbookId[\s\S]*?feedbackType/,
  'frontline panel must not stitch report-work context only from route query fields.'
)
assert.match(
  panel,
  /formalSubmitContextMissingFields/,
  'frontline panel must expose missing formal report-work context before signature submit.'
)
assert.match(
  panel,
  /formalSubmitContextMissingFields\.value\.length\s*>\s*0/,
  'frontline submit must remain blocked while formal report-work context is incomplete.'
)
assert.match(
  panel,
  /请从正式报工入口进入/,
  'missing formal context must be visible to the operator instead of failing after a signature prompt.'
)

assert.match(
  panel,
  /import\s*\{\s*sameRouteQueryId\s*\}\s*from\s*'@\/utils\/routeQueryId'/,
  'frontline process selection must use the shared route-query ID comparison helper.'
)
for (const [label, pattern] of [
  ['route', /sameRouteQueryId\(process\.routeId,\s*context\.routeId\)/],
  [
    'route process',
    /sameRouteQueryId\(process\.routeProcessId,\s*context\.routeProcessId\)/
  ],
  ['process', /sameRouteQueryId\(process\.processId,\s*context\.processId\)/]
]) {
  assert.match(
    panel,
    pattern,
    `initial ${label} selection must treat numeric and string IDs as the same identity.`
  )
}
assert.doesNotMatch(
  panel,
  /process\.(?:routeId|routeProcessId|processId)\s*===\s*context\.(?:routeId|routeProcessId|processId)/,
  'initial process selection must not compare route-query IDs with strict mixed-type equality.'
)
assert.match(
  panel,
  /\.frontline-operator-top\s*\{[\s\S]*?grid-template-columns:\s*repeat\(4,\s*minmax\(0,\s*1fr\)\)/,
  'production top cards must share the available container width without fixed-width overflow.'
)
assert.match(
  panel,
  /&\.is-pqc\s*\{[\s\S]*?grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/,
  'PQC top cards must share the available container width without clipping the employee card.'
)
assert.doesNotMatch(
  panel,
  /grid-template-columns:\s*320px\s+320px\s+420px\s+1fr\s+240px/,
  'PQC top cards must not use the fixed-width layout that overflows the page content container.'
)

console.log('PASS: frontline real submit static contract')
