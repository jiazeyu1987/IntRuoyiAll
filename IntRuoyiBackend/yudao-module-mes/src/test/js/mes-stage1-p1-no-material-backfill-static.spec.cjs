const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '../../../../..');
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8');

const stage1Service = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage1/MesStage1ActiveOrderCompleteSimulationServiceImpl.java'
);
const detailService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
);

const simulateStart = stage1Service.indexOf(
  'public MesStage1ActiveOrderCompleteSimulationResult simulate('
);
const fixtureStart = stage1Service.indexOf('private MesProcessPoolActiveOrderDO createFixture', simulateStart);
assert.ok(simulateStart >= 0 && fixtureStart > simulateStart, 'Stage1 simulate method must be discoverable');
const simulateBody = stage1Service.slice(simulateStart, fixtureStart);

assert.match(
  simulateBody,
  /simulateActiveOrderCompletion/,
  'P1 must still execute the formal frontline production and PQC simulation chain.'
);
assert.doesNotMatch(
  simulateBody,
  /ensureActiveOrderPickListBindings|ensureFormalProductIssue|requireBindings\(templateActiveOrder\)|createFormalProductIssue|setPickListId\(activeOrderBindings|getPickListId\(\)/,
  'P1 must not create/read/freeze pick-list bindings, product issue facts, or return material pick-list IDs.'
);
assert.match(
  simulateBody,
  /\.setPickListId\(null\)[\s\S]*\.setPickListIds\(List\.of\(\)\)/,
  'P1 response must make the material source boundary explicit until P2 runs.'
);

const sideEffectStart = stage1Service.indexOf('private void assertNoDownstreamSideEffects(');
const snapshotStart = stage1Service.indexOf('private Map<String, Object> buildSnapshot', sideEffectStart);
assert.ok(sideEffectStart >= 0 && snapshotStart > sideEffectStart, 'Stage1 side-effect guard must be discoverable');
const sideEffectGuard = stage1Service.slice(sideEffectStart, snapshotStart);
for (const code of [
  'STAGE1_BACKFILL_SIDE_EFFECT',
  'STAGE1_COMPLETION_RECEIPT_SIDE_EFFECT',
  'STAGE1_PROCESS_INSPECTION_AGGREGATE_SIDE_EFFECT',
  'STAGE1_BATCH_EXECUTION_SIDE_EFFECT'
]) {
  assert.match(sideEffectGuard, new RegExp(code), `P1 must fail fast on ${code}.`);
}
assert.doesNotMatch(
  sideEffectGuard,
  /STAGE1_PICK_LIST_BINDING_SIDE_EFFECT|STAGE1_PRODUCT_ISSUE_SIDE_EFFECT/,
  'Existing formal material sources may exist after reset; P1 must not reject them as side effects.'
);

assert.doesNotMatch(
  detailService,
  /readBoundPickListSources/,
  'Detail must not read bound pick-list lots before P2 BATCH_RECORD backfill exists.'
);

console.log('PASS: Stage1 P1 does not perform material backfill static contract');
