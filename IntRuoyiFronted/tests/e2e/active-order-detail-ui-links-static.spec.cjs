const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailPage = read('src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue')
const panel = read('src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue')
const frontendApi = read('src/api/mes/pro/processpool/teamLeader.ts')
const backendModel = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java')
const backendVo = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java')
const backendController = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java')
const backendService = read('../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetailServiceImpl.java')
const pickList = read('src/views/erp/production/pick-list/index.vue')
const replenishmentList = read('src/views/erp/production/replenishment-list/index.vue')

assert.doesNotMatch(
  detailPage,
  /team-leader-workbench__active-order-detail-page-eyebrow|stage1SourceWorkOrderCode\s*\?\s*`Stage1模拟详情|data-team-leader-stage1-generated-detail-source/,
  '详情页不应继续展示截图红框内的 Stage1 顶部说明内容。'
)

assert.match(
  panel,
  /<span>提交人<\/span>[\s\S]*pqcSubmission\.submitterName/,
  'PQC 提交卡片必须显示提交人。'
)
assert.match(
  panel,
  /<span>审核人<\/span>[\s\S]*pqcSubmission\.reviewerName\s*\|\|\s*'未审核'/,
  'PQC 提交卡片必须显示审核人，未审核时显示未审核。'
)
assert.match(
  frontendApi,
  /TeamLeaderActiveOrderPqcSubmissionDetailRespVO[\s\S]*submitterName\?:\s*string[\s\S]*reviewerName\?:\s*string/,
  '前端 PQC 提交类型必须承载提交人和审核人。'
)
assert.match(
  backendModel,
  /class PqcSubmissionDetail[\s\S]*private String submitterName;[\s\S]*private String reviewerName;/,
  '后端详情模型必须承载 PQC 提交人和审核人。'
)
assert.match(
  backendVo,
  /class PqcSubmissionDetail[\s\S]*private String submitterName;[\s\S]*private String reviewerName;/,
  '后端详情 VO 必须返回 PQC 提交人和审核人。'
)
assert.match(
  backendController,
  /\.setSubmitterName\(submission\.getSubmitterName\(\)\)[\s\S]*\.setReviewerName\(submission\.getReviewerName\(\)\)/,
  '后端 Controller 必须映射 PQC 提交人和审核人。'
)
assert.match(
  backendService,
  /selectEventPartiesByEventIds\(eventIds\)[\s\S]*\.setSubmitterName\(joinDistinctTexts\(submitterNames\)\)[\s\S]*\.setReviewerName\(joinDistinctTexts\(reviewerNames\)\)/,
  '后端详情服务必须从 PQC 提交事件读取并聚合提交人和审核人。'
)

assert.match(
  panel,
  /renderActiveOrderDocumentLinks\(material\.sourcePickListNos,\s*material\.sourcePickListIds,\s*'pick'\)/,
  '领料单列必须按编号和 ID 渲染可点击链接。'
)
assert.match(
  panel,
  /renderActiveOrderDocumentLinks\(material\.sourceReplenishmentListNos,\s*material\.sourceReplenishmentListIds,\s*'replenishment'\)/,
  '补料单列必须按编号和 ID 渲染可点击链接。'
)
assert.match(
  panel,
  /router\.push\(\{[\s\S]*path:\s*'\/erp\/production\/pick-list'[\s\S]*query:\s*\{[\s\S]*sourceBillNo:/,
  '点击领料单编号必须跳转 ERP 生产领料单列表并带 sourceBillNo 查询。'
)
assert.match(
  panel,
  /router\.push\(\{[\s\S]*path:\s*'\/erp\/production\/replenishment-list'[\s\S]*query:\s*\{[\s\S]*sourceBillNo:/,
  '点击补料单编号必须跳转 ERP 补料单列表并带 sourceBillNo 查询。'
)

assert.match(
  pickList,
  /useRoute\(\)[\s\S]*queryParams\.sourceBillNo\s*=\s*String\(route\.query\.sourceBillNo/,
  'ERP 生产领料单列表必须读取 URL query.sourceBillNo 自动筛选。'
)
assert.match(
  replenishmentList,
  /useRoute\(\)[\s\S]*queryParams\.sourceBillNo\s*=\s*String\(route\.query\.sourceBillNo/,
  'ERP 补料单列表必须读取 URL query.sourceBillNo 自动筛选。'
)

console.log('PASS: active-order detail UI links static contract')
