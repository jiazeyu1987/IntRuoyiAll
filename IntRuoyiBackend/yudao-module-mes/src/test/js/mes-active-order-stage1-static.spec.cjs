const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '../../../../..');
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8');

const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
);
const frontendApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts');
const workbench = read('IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue');
const stage1Service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImpl.java'
);
const stage1Migration = read(
  'IntRuoyiBackend/sql/mysql/20260825_mes_stage1_simulation_metadata.sql'
);

assert.match(controller, /active-order\/simulation\/stage1/,
  'Stage1 must expose a dedicated endpoint');
assert.match(controller, /MesStage1ActiveOrderCompleteSimulationService/,
  'controller must delegate Stage1 orchestration to its own service');
assert.match(stage1Service, /cleanupOwnedRuns/,
  'Stage1 must clean only its previous owned run before creating a fixture');
assert.match(stage1Service, /createFixture/,
  'Stage1 must create an independent active-order fixture');
assert.match(stage1Service, /createSimulationPickList/,
  'Stage1 must create its own pick-list fixture instead of requiring a template binding');
assert.doesNotMatch(stage1Service, /requireBinding\(templateBinding\)/,
  'Stage1 must not require the template active order to already have a pick-list binding');
assert.match(stage1Service, /bindingId|IdUtil\.getSnowflake\(\)\.nextId\(\)/,
  'Stage1 binding persistence must use an explicit primary key for the non-auto-increment binding tables');
assert.match(stage1Service, /simulateActiveOrderCompletion[\s\S]*?STAGE/,
  'Stage1 must reuse the formal active-order simulation service');
assert.match(stage1Service, /activeOrderCompleteSnapshot\.v2/,
  'Stage1 must return the v2 completion snapshot contract');
assert.match(stage1Service, /assertNoDownstreamSideEffects/,
  'Stage1 must verify no completion/backfill/batch/upload/release side effects');
for (const receiptColumn of ['completion_status', 'batch_record_id', 'process_inspection_id']) {
  assert.match(stage1Migration,
    new RegExp(`mes_pro_process_pool_active_order_completion_receipt[\\s\\S]*${receiptColumn}`),
    `Stage1 migration must align completion receipt column ${receiptColumn}`);
}
assert.match(stage1Service, /simulationRunId/,
  'Stage1 must carry a single simulationRunId through the whole run');
assert.match(frontendApi, /simulateStage1ActiveOrderCompletion/,
  'frontend API must expose the independent Stage1 action');
assert.match(frontendApi, /active-order\/simulation\/stage1/,
  'frontend API must call the dedicated Stage1 endpoint');
assert.match(workbench, /data-team-leader-simulate-active-order-stage1/,
  'real active-order page must expose the Stage1 button');
assert.match(workbench, /simulateStage1ActiveOrderCompletion\(/,
  'Stage1 button must call the dedicated API');

console.log('mes-active-order-stage1-static: PASS');
