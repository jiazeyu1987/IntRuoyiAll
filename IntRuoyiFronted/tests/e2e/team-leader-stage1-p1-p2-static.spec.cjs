const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const activeOrderStart = page.indexOf('data-team-leader-active-order-pool-tab')
assert.notEqual(activeOrderStart, -1, 'Expected active-order pool tab marker.')
const activeOrderEnd = page.indexOf('</ContentWrap>', activeOrderStart)
assert.notEqual(activeOrderEnd, -1, 'Expected active-order pool tab end.')
const activeOrderBlock = page.slice(activeOrderStart, activeOrderEnd)

assert.match(
  activeOrderBlock,
  /<el-table[\s\S]*:data="pagedActiveOrderRows"[\s\S]*row-key="id"[\s\S]*data-team-leader-active-order-list/,
  'Active-order table must keep a stable row key after P1 refreshes row data.'
)

const p1Button = activeOrderBlock.match(
  /<el-button[\s\S]*?data-team-leader-simulate-active-order-stage1-p1[\s\S]*?<\/el-button>/
)
assert.ok(p1Button, 'P1 must be exposed as a separate Stage1 simulation button.')
assert.match(p1Button[0], /@click="handleSimulateStage1\(row\)"/, 'P1 must keep the Stage1 simulation handler.')
assert.match(p1Button[0], />\s*<Icon icon="ep:refresh" \/>\s*P1双100\s*<\/el-button>/, 'P1 label must be P1双100.')

const p2Button = activeOrderBlock.match(
  /<el-button[\s\S]*?data-team-leader-generate-active-order-stage1-p2[\s\S]*?<\/el-button>/
)
assert.ok(p2Button, 'P2 must be exposed as a separate generated form button.')
assert.match(p2Button[0], /@click="handleGenerateStage1Forms\(row\)"/, 'P2 must use its own generate handler.')
assert.match(p2Button[0], /!canGenerateStage1Forms\(row\)/, 'P2 must be gated by Stage1 double-100 readiness.')
assert.match(p2Button[0], />\s*<Icon icon="ep:document" \/>\s*P2生成\s*<\/el-button>/, 'P2 label must be P2生成.')

const p1Handler = page.match(/const\s+handleSimulateStage1\s*=\s*async\s*\(row:[\s\S]*?\r?\n}\r?\n\r?\nconst\s+handleGenerateStage1Forms/)
assert.ok(p1Handler, 'Expected P1 handler before P2 handler.')
assert.match(
  p1Handler[0],
  /simulateStage1ActiveOrderCompletion\(\{[\s\S]*activeOrderId/,
  'P1 must call the Stage1 simulation API.'
)
assert.match(
  p1Handler[0],
  /const result = await simulateStage1ActiveOrderCompletion[\s\S]*assertStage1SimulationDouble100\(result\)[\s\S]*ElMessage\.success/,
  'P1 must fail fast unless the Stage1 API result is production/inspection double 100%.'
)
assert.doesNotMatch(
  p1Handler[0],
  /navigateActiveOrderSubmissionDetail/,
  'P1 must not auto-open the generated form detail.'
)

const p2Handler = page.match(/const\s+handleGenerateStage1Forms\s*=\s*async\s*\(row:[\s\S]*?\r?\n}\r?\n\r?\nconst\s+handleSimulateStage2_5/)
assert.ok(p2Handler, 'Expected P2 handler before Stage2.5 handler.')
assert.match(p2Handler[0], /canGenerateStage1Forms\(row\)/, 'P2 must re-check readiness before generation.')
assert.match(
  p2Handler[0],
  /const\s+activeOrderId\s*=\s*requirePositiveNumber\(row\.id[\s\S]*simulateStage2_5BackfillBatchExecution\(\{[\s\S]*activeOrderId[\s\S]*expectedVersion:\s*row\.version[\s\S]*\}/,
  'P2 must call the formal Stage2.5 backfill API with the clicked active order and version.'
)
assert.doesNotMatch(p2Handler[0], /router\.push|router\.replace|navigateActiveOrderSubmissionDetail/, 'P2 must stay on the current list.')
assert.match(p2Handler[0], /await loadActiveOrders\(\)/, 'P2 must refresh the current list after generation.')
assert.doesNotMatch(
  p2Handler[0],
  /simulateStage1ActiveOrderCompletion|submitActiveOrderReleaseApplication/,
  'P2 must not rerun P1 or push release.'
)

assert.match(
  page,
  /const\s+canGenerateStage1Forms\s*=\s*\(row:[\s\S]*Boolean\(row\.simulated\)\s*&&[\s\S]*row\.simulationStage\s*===\s*'STAGE1'[\s\S]*isActiveOrderProgressComplete\(row\.productionProgressPercent\)[\s\S]*isActiveOrderProgressComplete\(row\.inspectionProgressPercent\)/,
  'P2 readiness must require STAGE1 simulation and production/inspection double 100%.'
)
assert.match(
  page,
  /const\s+assertStage1SimulationDouble100\s*=\s*\(result:[\s\S]*result\.productionProgress100[\s\S]*result\.inspectionProgress100[\s\S]*P1 未完成双100/,
  'P1 must validate returned productionProgress100 and inspectionProgress100 flags.'
)

console.log('PASS: team-leader Stage1 P1/P2 split static contract')
