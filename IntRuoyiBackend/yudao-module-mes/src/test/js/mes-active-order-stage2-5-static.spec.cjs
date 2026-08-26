const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../../../../..')
const moduleRoot = path.join(root, 'IntRuoyiBackend/yudao-module-mes')
const controller = fs.readFileSync(path.join(moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'), 'utf8')
const simulationService = fs.readFileSync(path.join(moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage2_5/MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java'), 'utf8')
const frontendApi = fs.readFileSync(path.join(root, 'IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const workbench = fs.readFileSync(path.join(root, 'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')

assert.match(controller, /active-order\/simulation\/stage2-5/,
  'Stage2.5 must expose a dedicated simulation endpoint')
assert.match(controller, /MesStage2_5BackfillBatchExecutionSimulationService/,
  'controller must delegate Stage2.5 orchestration to a dedicated service')
assert.match(simulationService, /cleanupOwnedRuns/,
  'Stage2.5 must clean only its own simulation runs before creating a fixture')
assert.match(simulationService, /createFixture/,
  'Stage2.5 must create an independent active-order fixture')
assert.match(simulationService, /activeOrderMapper\.insert\(activeOrder\)/,
  'Stage2.5 must not mutate the selected real active order')
assert.match(simulationService, /simulateActiveOrderCompletion\(validated\.getActorUserId\(\), activeOrder\.getId\(\),[\s\S]*"2\.5", validated\.getSimulationRunId\(\)/,
  'Stage2.5 must persist simulation stage metadata through the real simulation service')
assert.match(simulationService, /Objects\.equals\(activeOrder\.getTenantId\(\), TenantContextHolder\.getTenantId\(\)/,
  'Stage2.5 must enforce current-tenant ownership before cloning the fixture')
assert.match(simulationService, /MesStage4DossierUploadSimulationContractValidator\.validateInput\(snapshot\)/,
  'Stage2.5 output must be validated against the formal Stage4 input contract')
assert.match(simulationService, /lossRequirement\.put\(\"required\"/,
  'Stage2.5 loss requirement must use the Stage4 object structure')
assert.match(simulationService, /result\.put\(\"overallStatus\", \"PENDING_UPLOAD\"\)/,
  'Stage2.5 must expose the Stage4 pending upload aggregate status')
assert.match(simulationService, /setCleanedSimulationRunId\(cleanedRunId\)/,
  'Stage2.5 must report the cleanup receipt')
assert.match(frontendApi, /simulateStage2_5BackfillBatchExecution/,
  'frontend API must expose the Stage2.5 orchestration endpoint')
assert.match(frontendApi, /active-order\/simulation\/stage2-5/,
  'frontend API must call the Stage2.5 endpoint')
assert.match(workbench, /data-team-leader-simulate-active-order-stage2-5/,
  'active-order page must expose the Stage2.5 acceptance button')
assert.match(workbench, />\s*模拟完工\s*</,
  'Stage2.5 acceptance button must be labelled 模拟完工')
assert.match(workbench, /MesStage2_5BackfillBatchExecutionSimulation|simulateStage2_5BackfillBatchExecution/,
  'button must call the Stage2.5 orchestration API')
assert.match(workbench, /router\.push\(result\.detailPath\)/,
  'successful Stage2.5 flow must navigate using the backend-provided real batch detail path')

console.log('mes-active-order-stage2-5-static: PASS')
