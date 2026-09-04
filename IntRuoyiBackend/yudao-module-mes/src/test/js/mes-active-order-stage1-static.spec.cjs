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
const activeOrderSimulationService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderSimulationService.java'
);
const stage1Migration = read(
  'IntRuoyiBackend/sql/mysql/20260825_mes_stage1_simulation_metadata.sql'
);

assert.match(controller, /active-order\/simulation\/stage1/,
  'Stage1 must expose a dedicated endpoint');
assert.match(controller, /MesStage1ActiveOrderCompleteSimulationService/,
  'controller must delegate Stage1 orchestration to its own service');
assert.match(stage1Service, /cleanupOwnedRuns\([^)]*validated\.getActorUserId\(\),\s*validated\.getSimulationRunId\(\)\)/,
  'Stage1 must clean previous owned runs after the new fixture is verified, excluding the current run');
assert.match(stage1Service, /createFixture/,
  'Stage1 must create an independent active-order fixture');
assert.match(stage1Service, /createSimulationPickLists/,
  'Stage1 must resolve all formal pick-list sources instead of requiring a template binding');
assert.match(stage1Service, /selectListByProductionOrderNo\(\s*workOrder\.getCode\(\)\s*\)/,
  'Stage1 must resolve the formal pick-list source with the work-order code, matching the active-order add flow.');
assert.doesNotMatch(stage1Service, /selectPickListIdsByProductionOrderNo\(workOrder\.getOrderSourceCode\(\)\)/,
  'Stage1 must not resolve pick lists from orderSourceCode because real pick-list lines are bound to workOrder.code.');
assert.match(stage1Service, /resolveTemplateBindings[\s\S]*Boolean\.TRUE\.equals\(activeOrder\.getSimulated\(\)\)/,
  'Stage1 rerun must accept a prior simulated order and read its persisted pick-list binding');
assert.match(stage1Service, /bindingMapper\s*\.\s*selectListByActiveOrderId\(activeOrder\.getId\(\)\)/,
  'Stage1 rerun must read all persisted pick-list bindings before cleanup');
assert.doesNotMatch(stage1Service, /createSyntheticPickList|SYNTHETIC_PICK_LIST_SOURCE_PREFIX/,
  'Stage1 must not fabricate a pick-list source when formal production pick lists are absent');
assert.doesNotMatch(stage1Service, /headers\.get\(0\)|模拟汇集全部生产领料单/,
  'Stage1 must not merge multiple formal pick lists into the first header');
assert.doesNotMatch(stage1Service, /bindingMapper\.selectByActiveOrderId\(/,
  'Stage1 multi-source facts must not be reduced to the first active-order binding');
assert.match(stage1Service, /for \(MesProcessPoolActiveOrderPickListBindingDO templateBinding : templateBindings\)/,
  'Stage1 must clone and bind every resolved formal pick list independently');
assert.match(stage1Service, /createFormalProductIssueForBinding/,
  'Stage1 must create formal product issue evidence per pick-list binding');
assert.match(stage1Service, /STAGE1-ISSUE-[\s\S]*binding\.getId\(\)/,
  'Stage1 product issue code must retain the source pick-list binding identity');
assert.doesNotMatch(stage1Service, /List<MesProcessPoolActiveOrderPickListBindingItemDO> bindingItems = new ArrayList<>\(\);[\s\S]*bindingItems\.addAll\(items\);[\s\S]*MesWmProductIssueDO issue/s,
  'Stage1 must not merge items from multiple pick-list bindings into one product issue');
assert.match(stage1Service, /"STAGE1-" \+ safe \+ "-PL-" \+ source\.getPickListId\(\)/,
  'each copied pick-list source identity must include its formal pick-list ID');
assert.match(stage1Service, /cleanupCopiedPickLists\(runId\)/,
  'Stage1 cleanup must remove every copied pick list for an owned simulation run');
assert.match(stage1Service, /likeRight\(ErpKingdeeProductionPickListDO::getSourceFid,\s*"STAGE1-" \+ shortRunId\(runId\) \+ "-PL-"\)/,
  'Stage1 cleanup must scope copied pick-list deletion to the exact run identity prefix');
assert.match(stage1Service, /selectByActiveOrderIdAndPickListId\(target\.getId\(\), header\.getId\(\)\)/,
  'Stage1 must use the active-order and copied-pick-list identity when checking a binding source');
assert.match(stage1Service, /pickListSources/,
  'Stage1 snapshots must carry every pick-list source, not one source field');
assert.match(stage1Service, /bindingId|IdUtil\.getSnowflake\(\)\.nextId\(\)/,
  'Stage1 binding persistence must use an explicit primary key for the non-auto-increment binding tables');
assert.match(stage1Service, /simulateActiveOrderCompletion[\s\S]*?STAGE/,
  'Stage1 must reuse the formal active-order simulation service');
assert.match(stage1Service, /calculateStage1PersistedProgress/,
  'Stage1 must recompute persisted progress after writing formal facts and before returning success');
assert.doesNotMatch(stage1Service, /\.setProductionProgress100\(true\)|\.setInspectionProgress100\(true\)/,
  'Stage1 must not hardcode progress booleans to true in the response');
assert.doesNotMatch(stage1Service, /"productionPercent",\s*100[\s\S]*"inspectionPercent",\s*100/,
  'Stage1 snapshot progress must use the recomputed persisted progress, not a hardcoded 100%');
assert.match(stage1Service, /activeOrderCompleteSnapshot\.v2/,
  'Stage1 must return the v2 completion snapshot contract');
assert.match(stage1Service, /assertNoDownstreamSideEffects/,
  'Stage1 must verify no completion/backfill/batch/upload/release side effects');
assert.match(stage1Service, /assertFormalOrderProcessCompletionFacts/,
  'Stage1 must keep and validate formal per-process completion facts produced by production leader review');
assert.match(stage1Service, /STAGE1_FORMAL_ORDER_PROCESS_COMPLETION_REQUIRED/,
  'Stage1 must fail fast when the reviewed production facts do not produce process completion summaries');
assert.doesNotMatch(stage1Service, /STAGE1_COMPLETION_SIDE_EFFECT/,
  'Stage1 must not treat per-process completion summaries as downstream active-order completion side effects');
assert.match(activeOrderSimulationService, /PRODUCTION_FEEDBACK_SOURCE_TYPE\s*=\s*"MES_PRO_FEEDBACK"/,
  'Stage1 production submit events must point to the formal production-feedback source type');
assert.match(activeOrderSimulationService, /createZeroLossProductionFeedback/,
  'Stage1 must create a formal zero-loss production feedback row for each simulated production submit');
assert.match(activeOrderSimulationService, /payload\.put\("lossDetails",\s*List\.of\(\)\)/,
  'Stage1 formal production submit payload must carry an explicit empty lossDetails array for zero-loss evidence');
assert.match(activeOrderSimulationService, /routeSnapshotJson/,
  'Stage1 production simulation must read materials from the active order frozen route snapshot.');
assert.match(activeOrderSimulationService, /inputMaterialIds/,
  'Stage1 must carry frozen input materials as system-managed balance evidence.');
assert.match(activeOrderSimulationService, /outputMaterialIds/,
  'Stage1 must create production material facts only from frozen output materials.');
assert.match(activeOrderSimulationService, /inputMaterialDetails/,
  'Stage1 must persist input material batch trace details separately from output quantities.');
assert.match(activeOrderSimulationService, /listBatchCodes/,
  'Stage1 input material evidence must obtain batch codes from the formal synchronized source.');
assert.match(activeOrderSimulationService, /PLACEHOLDER_MATERIAL_CODE\s*=\s*"\/"/,
  'Stage1 must explicitly recognize slash as a placeholder material code');
assert.match(activeOrderSimulationService, /isPlaceholderMaterialCode\(material\.getCode\(\)\)[\s\S]*detail\.put\("batchCodes",\s*List\.of\(\)\)/,
  'Stage1 must not query formal pick-list batches for slash placeholder input material codes');
assert.match(stage1Service, /isPlaceholderMaterialCode\(item\.getMaterialNumber\(\)\)/,
  'Stage1 product issue materialization must not match slash placeholder pick-list lines against material masters');
assert.match(activeOrderSimulationService, /resolveDefaultSimulationDevice/,
  'Stage1 must resolve a formal default device for simulated output material facts.');
assert.match(activeOrderSimulationService, /orderByAsc\(MesProcessPoolTeamProcessDeviceDO::getId\)/,
  'Stage1 must use the stable formal binding order when selecting its default device.');
assert.match(activeOrderSimulationService, /selectedDevice/,
  'Stage1 output material facts must persist the selected formal device.');
assert.match(activeOrderSimulationService, /getDeviceStatus\(\)/,
  'Stage1 must exclude inactive formal devices before selecting a default.');
assert.doesNotMatch(activeOrderSimulationService, /frontlineReportMaterialIds|workOrderBomMapper/,
  'Stage1 must not infer simulation materials from the retired batch-record field or product BOM.');
assert.doesNotMatch(activeOrderSimulationService, /\.feedbackSourceType\(SIMULATION_SOURCE_TYPE\)/,
  'Stage1 production submit events must not use a simulation-only feedback source');
assert.match(stage1Service, /STAGE1_FORMAL_PRODUCTION_FEEDBACK_REQUIRED/,
  'Stage1 must fail fast when formal production feedback links are missing');
assert.match(stage1Service, /feedbackMapper\.delete/,
  'Stage1 cleanup must remove task-owned formal feedback rows for prior simulation runs');
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
assert.match(frontendApi, /productionProgressPercent/,
  'frontend Stage1 response type must expose the persisted production progress percent');
assert.match(frontendApi, /inspectionProgressPercent/,
  'frontend Stage1 response type must expose the persisted inspection progress percent');
assert.match(workbench, /data-team-leader-simulate-active-order-stage1/,
  'real active-order page must expose the Stage1 button');
assert.match(workbench, /simulateStage1ActiveOrderCompletion\(/,
  'Stage1 button must call the dedicated API');
assert.doesNotMatch(workbench, /生产和检验进度均为100%/,
  'Stage1 success message must not hardcode 100%; it must display persisted response progress');
assert.match(workbench, /formatActiveOrderProgressPercent\(result\.productionProgressPercent\)[\s\S]*formatActiveOrderProgressPercent\(result\.inspectionProgressPercent\)/,
  'Stage1 success message must display the recomputed persisted progress values');
assert.match(workbench, /activeOrderDetailActiveOrderId\.value\s*=\s*requirePositiveNumber\(\s*result\.activeOrderId[\s\S]*activeOrderDetailVisible\.value\s*=\s*true[\s\S]*await loadActiveOrderSubmissionDetail\(activeOrderDetailActiveOrderId\.value\)/,
  'Stage1 completion must open the generated active order detail so PQC submissions are read from the order that Stage1 actually submitted');
assert.match(workbench, /Stage1模拟详情[\s\S]*activeOrderDetailStage1SourceWorkOrderCode[\s\S]*→[\s\S]*activeOrderSubmissionDetail\.workOrderCode/,
  'Stage1 generated detail dialog title must visibly show source to generated order mapping');
assert.match(workbench, /activeOrderDetailStage1SourceWorkOrderCode\.value\s*=\s*row\.workOrderCode\s*\|\|\s*''/,
  'Stage1 generated detail dialog must visibly show the clicked source order and the generated test order');

console.log('mes-active-order-stage1-static: PASS');
