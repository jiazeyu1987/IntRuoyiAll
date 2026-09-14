const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const page = readFileSync(
  resolve(process.cwd(), 'src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
const api = readFileSync(
  resolve(process.cwd(), 'src/api/mes/pro/edhr/batchExecution.ts'),
  'utf8'
).replace(/\r\n/g, '\n')

const handler = page.slice(
  page.indexOf('const handleStage5FinalReleaseSimulation'),
  page.indexOf('const handleStage6IdiSimulation')
)

assert.ok(handler.includes('route.query.stage4SimulationRunId'), 'Stage5 must consume the explicit Stage4 run id')
assert.equal(
  /route\.query\.simulationRunId\s*\|\|/.test(handler),
  false,
  'Stage5 must not read the ambiguous shared simulationRunId as its upstream contract'
)
assert.ok(handler.includes('simulateEdhrStage5FinalRelease'), 'Stage5 page must call the formal Stage5 API')
assert.ok(page.includes('data-batch-simulate-stage5-final-release'), 'Stage5 page must expose a stable E2E button marker')
assert.ok(handler.includes('previousSimulationRunId'), 'Stage5 rerun must send the previous Stage5 run id for scoped cleanup')
assert.ok(handler.includes('managerSignoffEvidenceHash'), 'Stage5 must preserve manager signoff evidence for later approval')
assert.ok(!handler.includes('simulateStage6IdiData'), 'Stage5 must not trigger Stage6 traceability directly')
assert.ok(api.includes('/simulation/stage5/final-release'), 'Stage5 API endpoint must remain explicit')
assert.ok(api.includes('stage4SimulationRunId'), 'Stage5 API request must carry the Stage4 run id')

const managerTaskPage = readFileSync(
  resolve(process.cwd(), 'src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue'),
  'utf8'
).replace(/\r\n/g, '\n')
assert.ok(managerTaskPage.includes('snapshot.fourMaterialEvidence'), 'Stage5 release snapshot UI must read fourMaterialEvidence')
assert.ok(managerTaskPage.includes('evidence.length !== 4'), 'Stage5 release snapshot UI must require all four materials')
assert.equal(
  managerTaskPage.includes('snapshot.threeFileEvidence'),
  false,
  'Stage5 release snapshot UI must not use the obsolete three-file field'
)

console.log('PASS: Stage5 final release frontend static contract')
