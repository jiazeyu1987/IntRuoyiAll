const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../../..')
const readUtf8 = (relativePath) => fs
  .readFileSync(path.join(workspaceRoot, relativePath), 'utf8')
  .replace(/\r\n/g, '\n')

const pageReq = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelinePageReqVO.java')
const timelineMapper = readUtf8('yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml')
const readDO = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/ProcessPoolTimelineEventReadDO.java')
const respVO = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/vo/ProcessPoolTimelineEventRespVO.java')
const timelineService = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineServiceImpl.java')
const reviewService = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java')
const revisionController = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/MesProProcessPoolEventRevisionController.java')
const pqcCorrectionService = readUtf8('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/MesProcessPoolPqcInspectionCorrectionService.java')

assert.match(
  pageReq,
  /private String pqcFormView;/,
  'Timeline page request must expose pqcFormView for PQC current/history semantics.'
)
assert.match(
  timelineMapper,
  /CASE WHEN EXISTS \([\s\S]*mes_pro_process_pool_active_order_release_application[\s\S]*mes_pro_edhr_release_transaction[\s\S]*release_transaction\.release_status = 'RELEASED'[\s\S]*\) THEN TRUE ELSE FALSE END AS released/,
  'Timeline mapper must calculate released from the formal active-order release transaction.'
)
assert.match(
  timelineMapper,
  /pqc_task\.active_order_id AS activeOrderId/,
  'PQC timeline rows must expose the formal active order id from the PQC inspection task.'
)
assert.match(
  timelineMapper,
  /<if test="reqVO\.pqcFormView == 'CURRENT'">[\s\S]*pool_event\.event_type = 'PQC_INSPECTION'[\s\S]*NOT EXISTS \([\s\S]*release_transaction\.release_status = 'RELEASED'[\s\S]*<\/if>/,
  'PQC current form view must exclude rows whose active order has already been released.'
)
assert.match(
  timelineMapper,
  /<if test="reqVO\.pqcFormView == 'HISTORY'">[\s\S]*pool_event\.event_type = 'PQC_INSPECTION'[\s\S]*latest_submission_review\.review_status = 'APPROVED'[\s\S]*<\/if>/,
  'PQC history view must retain approved forms and therefore keep released forms queryable.'
)
assert.match(readDO, /private Long activeOrderId;[\s\S]*private Boolean released;/, 'Timeline read model must carry activeOrderId and released.')
assert.match(respVO, /private Long activeOrderId;[\s\S]*private Boolean released;/, 'Timeline response VO must carry activeOrderId and released.')
assert.match(
  timelineService,
  /\.setActiveOrderId\(event\.getActiveOrderId\(\)\)[\s\S]*\.setReleased\(Boolean\.TRUE\.equals\(event\.getReleased\(\)\)\)/,
  'Timeline service must copy activeOrderId and normalized released state into the response.'
)
assert.match(
  reviewService,
  /MesProcessPoolSubmissionReviewDO\.STATUS_APPROVED\.equals\(reqBO\.getReviewStatus\(\)\)[\s\S]*aggregateApprovedPqcSubmission\(reqBO\.getEventId\(\), review\.getId\(\)\)/,
  'Approved PQC review must keep updating process-inspection aggregation/active-order inspection progress chain.'
)
assert.match(
  revisionController,
  /@PostMapping\("\/correct-pqc-inspection"\)[\s\S]*pqcInspectionCorrectionService\.correct\([\s\S]*setActorUserId\(getLoginUserId\(\)\)/,
  'The event revision controller must expose the authenticated PQC correction endpoint.'
)
assert.match(
  pqcCorrectionService,
  /findReleasedActiveOrderIdsForUpdate\(List\.of\(task\.getActiveOrderId\(\)\)\)[\s\S]*releasedPqcInspectionForm/,
  'PQC correction must reject a form after its active order has been formally released.'
)
assert.match(
  pqcCorrectionService,
  /revisionService\.updatePqcInspectionRecord\([\s\S]*updateFormalPqcTables\([\s\S]*aggregateApprovedPqcSubmission\(event\.getId\(\), record\.getProcessInspectionReviewId\(\)\)/,
  'PQC correction must audit the change, update formal PQC tables, and recompute an already-approved aggregation.'
)

console.log('PASS: MES PQC leader form release flow backend static contract')
