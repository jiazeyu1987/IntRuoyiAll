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
  path.join(
    root,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)
const frontendApi = fs.readFileSync(
  path.join(root, 'IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)
const detailReadMapperXml = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml'
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
  detailModel.includes('List<Long> pqcTaskIds') &&
    detailModel.includes('List<Long> submittedEventIds') &&
    detailModel.includes('private Long qaProcessId') &&
    detailModel.includes('private String qaProcessName'),
  'detail model must expose merged PQC task/event id lists and formal PQC process identity'
)
assert(
  detailService.includes('MesFrontlineProcessMaterialService') &&
    detailService.includes('listFrozenMaterials(activeOrderId, activeOrder.getRouteId()'),
  'detail service must resolve input material batches from active order formal pick-list sources'
)
assert(
  /COALESCE\(\s*NULLIF\(reviewer\.nickname,\s*''\),\s*NULLIF\(reviewer_profile_by_user\.display_name,\s*''\),\s*NULLIF\(reviewer_profile_by_user\.employee_name,\s*''\),\s*NULLIF\(reviewer_profile_by_id\.display_name,\s*''\),\s*NULLIF\(reviewer_profile_by_id\.employee_name,\s*''\),\s*NULLIF\(reviewer\.username,\s*''\)\s*\)\s+AS reviewerName/s.test(detailReadMapperXml),
  'active-order detail reviewerName must display the current simulation actor from formal user or team employee profile data'
)
assert(
  detailReadMapperXml.includes('reviewer_profile_by_user.system_user_id = latest_review.leader_user_id') &&
    detailReadMapperXml.includes('reviewer_profile_by_id.id = latest_review.leader_user_id'),
  'active-order detail reviewer lookup must support both formal system user ids and team employee profile ids'
)
assert(
  detailService.includes('MesPqcInspectionTaskMapper') &&
    detailService.includes('MesPqcProcessInspectionAggregateDetailMapper') &&
    detailService.includes('MesQaInspectionRegulationProcessMapper') &&
    detailService.includes('selectListByActiveOrderId(activeOrderId)') &&
    detailService.includes('selectBatchIds(distinctIds(tasks, MesPqcInspectionTaskDO::getQaProcessId))') &&
    detailService.includes('PqcSubmissionIdentity') &&
    detailService.includes('PqcSubmissionAccumulator'),
  'detail service must aggregate PQC submitted task details by active order and attach formal QA process identity'
)
assert(
  detailVo.includes('List<InputMaterialDetail> inputMaterials') &&
    detailVo.includes('List<PqcSubmissionDetail> pqcSubmissions') &&
    detailVo.includes('List<String> sourcePickListNos') &&
    detailVo.includes('List<Long> pqcTaskIds') &&
    detailVo.includes('List<Long> submittedEventIds') &&
    detailVo.includes('private Long qaProcessId') &&
    detailVo.includes('private String qaProcessName'),
  'response VO must preserve input material, merged PQC submission sections, and PQC process identity'
)
assert(
  controller.includes('toActiveOrderInputMaterialDetailRespVO') &&
    controller.includes('toActiveOrderPqcSubmissionDetailRespVO') &&
    controller.includes('.setSourcePickListNos(material.getSourcePickListNos())') &&
    controller.includes('.setPqcTaskIds(submission.getPqcTaskIds())') &&
    controller.includes('.setSubmittedEventIds(submission.getSubmittedEventIds())') &&
    controller.includes('.setQaProcessId(submission.getQaProcessId())') &&
    controller.includes('.setQaProcessName(submission.getQaProcessName())'),
  'controller must map all new detail sections to the frontend contract'
)
assert(
  frontendApi.includes('inputMaterials: TeamLeaderActiveOrderInputMaterialDetailRespVO[]') &&
    frontendApi.includes('pqcSubmissions: TeamLeaderActiveOrderPqcSubmissionDetailRespVO[]') &&
    frontendApi.includes('sourcePickListNos: string[]') &&
    frontendApi.includes('pqcTaskIds?: number[]') &&
    frontendApi.includes('submittedEventIds?: number[]') &&
    frontendApi.includes('qaProcessId?: number') &&
    frontendApi.includes('qaProcessName?: string'),
  'frontend API contract must include input materials and PQC submissions'
)
assert(
  frontend.includes('生产提交') &&
    frontend.includes('PQC提交') &&
    frontend.includes('领料单') &&
    frontend.includes('formatActiveOrderPickListNos(material.sourcePickListNos)') &&
    !frontend.includes('formatActiveOrderSourceIds(material.sourcePickListIds)'),
  'active order detail panel must show process production/PQC tabs and a final pick-list tab with exact bill numbers'
)
assert(
  frontend.includes('data-team-leader-active-order-detail-main-tabs') &&
    frontend.includes('data-team-leader-active-order-detail-production-process-tabs') &&
    frontend.includes('data-team-leader-active-order-detail-pqc-process-tabs') &&
    frontend.includes('data-team-leader-active-order-detail-production-process-tab') &&
    frontend.includes('data-team-leader-active-order-detail-pqc-process-tab') &&
    frontend.includes('data-team-leader-active-order-detail-material-tab') &&
    frontend.includes('const activeTab = ref') &&
    frontend.includes('const productionActiveTab = ref') &&
    frontend.includes('const pqcActiveTab = ref') &&
    frontend.includes('const pickListMaterials = computed'),
  'active order detail panel must use production/PQC/material main tabs and separate production/PQC process tabs'
)
assert(
  frontend.includes('data-team-leader-active-order-detail-main-tabs') &&
    frontend.includes('name="productionSubmissions"') &&
    frontend.includes('name="pqcSubmissions"') &&
    frontend.includes('label="生产提交"') &&
    frontend.includes('label="PQC提交"'),
  'active order detail first-level tabs must separate production and PQC'
)
const activeOrderPqcTabStart = frontend.indexOf('<el-tab-pane label="PQC提交" name="pqcSubmissions">')
const activeOrderPqcTabEnd = frontend.indexOf('</el-tab-pane>', activeOrderPqcTabStart)
const activeOrderPqcTab = frontend.slice(activeOrderPqcTabStart, activeOrderPqcTabEnd)
assert(
  activeOrderPqcTabStart >= 0 &&
    activeOrderPqcTabEnd > activeOrderPqcTabStart &&
    !activeOrderPqcTab.includes('label="标准"'),
  'active order detail PQC tab must not show the standard column'
)
assert(
  activeOrderPqcTab.includes(':data="buildActiveOrderPqcItemRows(pqcSubmission)"'),
  'active order detail PQC table must aggregate same inspection item samples into one visible row'
)
assert(
  !activeOrderPqcTab.includes(':data="pqcSubmission.items"'),
  'active order detail PQC table must not render one row per raw sample item'
)
assert(
  frontend.includes('interface ActiveOrderPqcItemAggregateRow') &&
    frontend.includes('const buildActiveOrderPqcItemRows =') &&
    frontend.includes('formatActiveOrderPqcItemResultSummary') &&
    frontend.includes('formatActiveOrderPqcItemSampleSummary'),
  'active order detail PQC aggregation helpers must summarize sample count, item results, and judgement'
)
assert(
  frontend.includes('formatActiveOrderPqcEventIds(pqcSubmission)') &&
    frontend.includes('pqcSubmission.submittedEventIds?.length'),
  'active order detail PQC tab must show merged submitted event id lists'
)
assert(
  frontend.includes('pqcProcessGroups') &&
    frontend.includes('submission.qaProcessId') &&
    frontend.includes('submission.qaProcessName') &&
    !frontend.includes('activeOrderDetailInnerTabs'),
  'PQC tab must group by its own QA process identity instead of nesting under production processes'
)
assert(
  frontend.includes('label="领料单"') &&
    frontend.includes('pickListMaterials') &&
    frontend.includes('sourceProcessNames'),
  'the final material tab must aggregate pick-list material batches and show related process names'
)

console.log('mes-active-order-submission-overview-static.spec.cjs PASS')
