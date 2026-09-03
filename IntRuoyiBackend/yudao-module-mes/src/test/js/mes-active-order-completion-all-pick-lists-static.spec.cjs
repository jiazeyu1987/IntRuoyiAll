const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../..')
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8')

const completion = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderCompletionServiceImpl.java')
const resolver = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderPickListCompletionSourceService.java')
const formalResolver = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFormalProductionPickListSourceResolver.java')
const batchQuery = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFeedbackMaterialBatchQueryServiceImpl.java')
const processMaterials = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineProcessMaterialServiceImpl.java')
const runtimeConfig = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java')
const frontlineSubmit = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/feedback/frontline/MesProFrontlineFeedbackSubmitServiceImpl.java')
const hasher = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher.java')
const dossier = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseDossierPortImpl.java')
const authoritativeContext = read('src/main/java/cn/iocoder/yudao/module/mes/productionrelease/core/MesReleaseAuthoritativeContextPortImpl.java')
const command = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderReleaseBatchRecordPlanCommand.java')
const request = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderAddReqVO.java')
const bo = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderAddReqBO.java')
const controller = read('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
const activeOrderService = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderService.java')
const activeOrderServiceImpl = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java')
const frontendApi = fs.readFileSync(path.resolve(root, '../../IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const activeOrderStatic = fs.readFileSync(path.resolve(root,
  '../../IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js'), 'utf8')
const releaseRealPath = path.resolve(root,
  '../../IntRuoyiFronted/tests/e2e/pqc-production-release-write-flow-real.e2e.js')
const releaseReal = fs.readFileSync(releaseRealPath, 'utf8')
const stage4 = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage4/MesStage4DossierUploadSimulationServiceImpl.java')
const stage5 = read('src/main/java/cn/iocoder/yudao/module/mes/service/pro/simulation/stage5/MesStage5FinalReleaseSimulationServiceImpl.java')
const page = fs.readFileSync(path.resolve(root, '../../IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const frontlinePage = fs.readFileSync(path.resolve(root, '../../IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'), 'utf8')

function requireMatch(value, pattern, message) {
  if (!pattern.test(value)) throw new Error(message)
}
function forbid(value, pattern, message) {
  if (pattern.test(value)) throw new Error(message)
}

requireMatch(completion, /pickListCompletionSourceService\.freezeAll\(activeOrder,\s*leaderUserId,\s*command\.getIdempotencyKey\(\)\)/s,
  'completion transaction must freeze all formal pick-list sources')
requireMatch(resolver, /sourceResolver\.resolve\(activeOrder\.getWorkOrderId\(\)\)/,
  'completion freezing must use the shared formal source resolver')
requireMatch(formalResolver, /selectListByProductionOrderNo\(productionOrderNo\)/,
  'formal sources must be discovered by productionOrderNo')
requireMatch(formalResolver, /groupingBy\(ErpKingdeeProductionPickListItemDO::getProductionPickListId/,
  'pick-list items must be grouped by pickListId')
requireMatch(formalResolver, /!"C"\.equalsIgnoreCase\(header\.getDocumentStatus\(\)\)/,
  'shared resolver must reject non-approved headers')
requireMatch(formalResolver, /FORMAL_PICK_LIST_REQUIRED/,
  'shared resolver must fail when no formal pick list exists')
requireMatch(batchQuery, /sourceResolver\.resolve\(workOrderId\)/,
  'frontline batch query must use the shared formal source resolver')
forbid(batchQuery, /bindingMapper|bindingItemMapper|\.insert\(|\.update|\.delete/,
  'frontline batch query must remain read-only and never write bindings')
requireMatch(processMaterials, /INPUT_MATERIAL_IDS_KEY[\s\S]*OUTPUT_MATERIAL_IDS_KEY[\s\S]*parseRoleMaterialIds/,
  'frozen process materials must parse explicit input and output material ids')
requireMatch(processMaterials, /ROLE_INPUT\.equals\(materialRole\)[\s\S]*batchQueryService\.resolveEvidence/,
  'only input materials may resolve formal pick-list evidence')
requireMatch(processMaterials, /ROLE_OUTPUT\.equals\(materialRole\)\)\s*\?\s*batchQueryService\.resolveEvidence|ROLE_INPUT\.equals\(materialRole\)\s*\?\s*batchQueryService\.resolveEvidence/,
  'output materials must not query input batch evidence')
requireMatch(runtimeConfig, /ROLE_INPUT\.equals\(material\.materialRole\(\)\)[\s\S]*ROLE_OUTPUT\.equals\(material\.materialRole\(\)\)/,
  'runtime config must separate input evidence from output tabs')
requireMatch(frontlineSubmit, /inputMaterialDetails[\s\S]*sourcePickListIds[\s\S]*sourceSnapshotHash|sourcePickListIds[\s\S]*inputMaterialDetails/,
  'production submit raw payload must retain server-managed input material evidence')
requireMatch(frontlinePage, /runtimeConfig\?\.materials[\s\S]*configuredProductionMaterials/,
  'production material tabs must continue to use output materials only')
forbid(frontlinePage, /runtimeConfig\?\.inputMaterials[^\n]*(map|forEach)|configuredProductionMaterials[^\n]*inputMaterials/,
  'input materials must not generate user completion tabs')
requireMatch(resolver, /selectListByActiveOrderId\(activeOrder\.getId\(\)\)/,
  'existing bindings must be validated as a collection')
requireMatch(hasher, /pickListBindings/, 'release source hash must include all pick-list bindings')
requireMatch(command, /List<Long> pickListBindingIds/, 'batch-record plan must carry all binding ids')
forbid(dossier, /selectByActiveOrderId\(/, 'PQC dossier must not choose the first pick-list binding')
forbid(dossier, /setPickListBindingId\(/, 'PQC dossier must not pass a scalar binding')
forbid(authoritativeContext, /batchRecordIds\.size\(\)\s*!=\s*1|inspectionIds\.size\(\)\s*!=\s*1|batchRecordIds\.get\(0\)|inspectionIds\.get\(0\)/,
  'completion backfill materialized ids must not be inferred from the first source evidence id')
requireMatch(authoritativeContext, /setBatchRecordId\(receipt\.getBatchRecordId\(\)\)[\s\S]*setProcessInspectionId\(receipt\.getProcessInspectionId\(\)\)/,
  'completion context must use the materialized backfill ids from the receipt')
forbid(stage4, /setBatchRecordSourceIdsJson\(JSON\.toJSONString\(List\.of\(batchRecordBackfill\.getId\(\)\)\)\)/,
  'Stage4 must not replace source evidence ids with the materialized backfill id')
requireMatch(stage5, /setBatchRecordId\(batchRecordBackfill\.getId\(\)\)[\s\S]*setProcessInspectionId\(processInspectionBackfill\.getId\(\)\)/,
  'Stage5 completion receipt must carry materialized backfill ids separately from source evidence ids')
forbid(request, /pickListId|pickListCandidateSnapshotHash/, 'add request must not expose pre-binding fields')
forbid(bo, /pickListId|pickListCandidateSnapshotHash/, 'add command must not expose pre-binding fields')
forbid(controller, /reqVO\.getPickListId\(|reqVO\.getPickListCandidateSnapshotHash\(/,
  'controller must not submit pre-binding fields')
forbid(controller, /pick-list-options|listPickListOptions/, 'obsolete pick-list candidate endpoint must be removed')
forbid(activeOrderService, /listPickListOptions/, 'obsolete pick-list candidate service contract must be removed')
forbid(activeOrderServiceImpl, /listPickListOptions|buildPickListOption/, 'obsolete pick-list candidate service must be removed')
forbid(frontendApi, /getTeamLeaderActiveOrderPickListOptions|TeamLeaderPickListOptionRespVO|pick-list-options/,
  'obsolete pick-list candidate frontend API must be removed')
requireMatch(activeOrderStatic, /add request type must contain only workOrderId/,
  'formal active-order static regression must enforce the no-prebinding request')
forbid(releaseReal, /pick-list-options|pickListDiagnostics|selected\.pickList|data-team-leader-active-order-pick-list/,
  'formal release real path must not use the removed pick-list prebinding flow')
forbid(page, /activeOrderForm\.pickListId|loadActiveOrderPickListOptions|data-team-leader-active-order-pick-list/,
  'add-active-order UI must not select a pick list')

console.log('mes-active-order-completion-all-pick-lists-static: PASS')
