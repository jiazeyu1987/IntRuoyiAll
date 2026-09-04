const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '../../../../..')
const read = (file) => fs.readFileSync(path.join(root, file), 'utf8')

const detailModel = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const detailService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java'
)
const detailVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const controller = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const frontend = fs.readFileSync(
  path.join(root, 'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const frontendApi = fs.readFileSync(
  path.join(root, 'IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)

assert(
  detailModel.includes('List<InputMaterialDetail> inputMaterials'),
  'detail model must expose per-process input material batch evidence'
)
assert(
  detailModel.includes('List<String> sourcePickListNos'),
  'detail model must expose exact formal pick-list bill numbers to avoid frontend Long precision loss'
)
assert(
  detailModel.includes('List<PqcSubmissionDetail> pqcSubmissions'),
  'detail model must expose per-process frontline PQC submissions'
)
assert(
  detailService.includes('MesFrontlineProcessMaterialService') &&
    detailService.includes('listFrozenMaterials(activeOrderId, activeOrder.getRouteId()'),
  'detail service must resolve input material batches from active order formal pick-list sources'
)
assert(
  detailService.includes('MesPqcInspectionTaskMapper') &&
    detailService.includes('MesPqcProcessInspectionAggregateDetailMapper') &&
    detailService.includes('selectListByActiveOrderId(activeOrderId)'),
  'detail service must aggregate PQC submitted task details by active order'
)
assert(
  detailVo.includes('List<InputMaterialDetail> inputMaterials') &&
    detailVo.includes('List<PqcSubmissionDetail> pqcSubmissions') &&
    detailVo.includes('List<String> sourcePickListNos'),
  'response VO must preserve input material and PQC submission sections'
)
assert(
  controller.includes('toActiveOrderInputMaterialDetailRespVO') &&
    controller.includes('toActiveOrderPqcSubmissionDetailRespVO') &&
    controller.includes('.setSourcePickListNos(material.getSourcePickListNos())'),
  'controller must map all new detail sections to the frontend contract'
)
assert(
  frontendApi.includes('inputMaterials: TeamLeaderActiveOrderInputMaterialDetailRespVO[]') &&
    frontendApi.includes('pqcSubmissions: TeamLeaderActiveOrderPqcSubmissionDetailRespVO[]') &&
    frontendApi.includes('sourcePickListNos: string[]'),
  'frontend API contract must include input materials and PQC submissions'
)
assert(
  frontend.includes('生产提交') &&
    frontend.includes('PQC提交') &&
    frontend.includes('领料单') &&
    frontend.includes('formatActiveOrderPickListNos(material.sourcePickListNos)') &&
    !frontend.includes('formatActiveOrderSourceIds(material.sourcePickListIds)'),
  'active order detail dialog must show process production/PQC tabs and a final pick-list tab with exact bill numbers'
)
assert(
  frontend.includes('data-team-leader-active-order-detail-process-tabs') &&
    frontend.includes('data-team-leader-active-order-detail-process-tab') &&
    frontend.includes('data-team-leader-active-order-detail-material-tab') &&
    frontend.includes('activeOrderDetailActiveTab') &&
    frontend.includes('activeOrderDetailInnerTabs') &&
    frontend.includes('activeOrderDetailPickListMaterials'),
  'active order detail dialog must use process tabs plus one material tab, with per-process production/PQC inner tabs'
)
assert(
  frontend.includes('name="production"') &&
    frontend.includes('name="pqc"') &&
    frontend.includes('label="生产提交"') &&
    frontend.includes('label="PQC提交"'),
  'each process tab must contain production and PQC inner tabs'
)
assert(
  frontend.includes('label="领料单"') &&
    frontend.includes('activeOrderDetailPickListMaterials') &&
    frontend.includes('sourceProcessNames'),
  'the final material tab must aggregate pick-list material batches and show related process names'
)

console.log('mes-active-order-submission-overview-static.spec.cjs PASS')
