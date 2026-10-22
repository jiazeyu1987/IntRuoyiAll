const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')

const completeMarker = page.indexOf('data-team-leader-active-order-release-apply')
assert.ok(completeMarker >= 0, 'The general complete button must remain visible in the active-order list.')
const completeStart = page.lastIndexOf('<el-button', completeMarker)
const completeEnd = page.indexOf('</el-button>', completeMarker)
assert.ok(completeStart >= 0 && completeEnd > completeStart, 'Expected complete button block.')
const completeButton = page.slice(completeStart, completeEnd + '</el-button>'.length)
assert.match(completeButton, />\s*完工\s*<\/el-button>/, 'The business action label must stay 完工.')
assert.match(
  completeButton,
  /!canApplyActiveOrderRelease\(row\)/,
  'The complete button must use formal active-order release gates.'
)
assert.doesNotMatch(
  completeButton,
  /canGenerateStage1Forms|simulationStage|simulated|STAGE1|data-team-leader-generate-active-order-stage1-p2/,
  'The complete button must not depend on Stage1/P2 simulation gates.'
)

const handlerStart = page.indexOf('const submitActiveOrderReleaseApplication =')
assert.ok(handlerStart >= 0, 'Expected complete handler.')
const handlerEnd = page.indexOf('\nconst ', handlerStart + 1)
assert.ok(handlerEnd > handlerStart, 'Expected complete handler end.')
const handler = page.slice(handlerStart, handlerEnd)
assert.match(handler, /applyTeamLeaderActiveOrderRelease\(/, 'Complete must call the formal release apply endpoint.')
assert.match(handler, /assertActiveOrderReleaseApplicationReceipt\(result,\s*row\.id,\s*true\)/)
assert.match(handler, /await loadActiveOrders\(\)/, 'Complete must refresh active orders after success.')
assert.doesNotMatch(
  handler,
  /simulateStage2_5BackfillBatchExecution|simulateStage1ActiveOrderCompletion|pushGeneratedTeamLeaderActiveOrderRelease|canGenerateStage1Forms|simulationStage|STAGE1/,
  'Complete must not call simulation endpoints or the Stage1-gated standalone P3 handler.'
)
assert.match(
  handler,
  /P2[\s\S]*P3|P3[\s\S]*P2/,
  'The confirmation or user-facing copy must make the P2 plus P3 business action explicit.'
)

const receiptAssertionStart = page.indexOf('const assertActiveOrderReleaseApplicationReceipt =')
assert.ok(receiptAssertionStart >= 0, 'Expected release receipt assertion helper.')
const receiptAssertionEnd = page.indexOf('\nconst ', receiptAssertionStart + 1)
assert.ok(receiptAssertionEnd > receiptAssertionStart, 'Expected release receipt assertion helper end.')
const receiptAssertion = page.slice(receiptAssertionStart, receiptAssertionEnd)
assert.match(
  receiptAssertion,
  /requirePositiveNumber\(result\.batchExecutionId,\s*'放行申请回执缺少P2批次执行ID'\)/,
  'Formal complete must verify the P2 batch execution returned by the P3 receipt.'
)

assert.match(api, /batchExecutionId:\s*string/, 'Release apply response type must expose P2 batchExecutionId.')
assert.match(api, /active-order\/release\/apply/, 'Formal complete must keep the authoritative apply endpoint.')

console.log('PASS: complete button uses general formal P2+P3 flow without Stage1 simulation gates')
