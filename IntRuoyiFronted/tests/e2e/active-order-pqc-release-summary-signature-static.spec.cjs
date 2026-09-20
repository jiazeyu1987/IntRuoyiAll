const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const read = (relativePath) => fs.readFileSync(path.resolve(repoRoot, relativePath), 'utf8')

const panel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const teamLeaderApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts')
const detailModel = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const detailRespVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const teamLeaderController = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java'
)
const releaseDetailService = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/pqc/MesPqcReleaseOrderDetailService.java'
)

assert.match(
  teamLeaderApi,
  /export interface TeamLeaderActiveOrderPqcProductionReleaseSummaryRespVO[\s\S]*status: string[\s\S]*statusLabel: string[\s\S]*signature\?: TeamLeaderActiveOrderSignatureDetailRespVO/,
  '前端详情 API 类型必须暴露 PQC 生产放行摘要和正式电子签名对象。'
)
assert.match(
  teamLeaderApi,
  /pqcProductionRelease\?: TeamLeaderActiveOrderPqcProductionReleaseSummaryRespVO/,
  '活跃订单详情类型必须包含 pqcProductionRelease。'
)

assert.match(
  detailModel,
  /private PqcProductionReleaseSummary pqcProductionRelease;[\s\S]*class PqcProductionReleaseSummary[\s\S]*private String status;[\s\S]*private String statusLabel;[\s\S]*private SignatureDetail signature;/,
  '后端领域详情必须携带 PQC 生产放行摘要，不得只在前端拼接。'
)
assert.match(
  detailRespVo,
  /private PqcProductionReleaseSummary pqcProductionRelease;[\s\S]*class PqcProductionReleaseSummary[\s\S]*private String status;[\s\S]*private String statusLabel;[\s\S]*private SignatureDetail signature;/,
  '后端响应 VO 必须透出 PQC 生产放行摘要。'
)
assert.match(
  teamLeaderController,
  /setPqcProductionRelease\(\s*toActiveOrderPqcProductionReleaseSummaryRespVO\(\s*detail\.getPqcProductionRelease\(\)\s*\)\s*\)/,
  'Controller 必须把领域详情中的 PQC 生产放行摘要映射给前端。'
)

assert.match(
  releaseDetailService,
  /ElectronicSignatureQueryService/,
  'PQC 放行详情服务必须通过正式电子签名查询服务读取签名记录。'
)
assert.match(
  releaseDetailService,
  /signatureQueryService\.getById\(signatureId\)/,
  'PQC 放行详情服务必须使用放行回执 signatureId 读取正式签名。'
)
assert.match(
  releaseDetailService,
  /signatureQueryService\.verifyEvidence\(signatureId\)/,
  'PQC 放行详情服务必须校验证据哈希和签名有效性。'
)
assert.match(
  releaseDetailService,
  /ACTION_PQC_RELEASE/,
  'PQC 放行详情服务必须校验签名动作为 PQC_RELEASE。'
)
assert.match(
  releaseDetailService,
  /setStatus\(decision\.getStatus\(\)\)[\s\S]*setStatusLabel\("已生产放行"\)/,
  'PQC 放行成功后的总表状态必须保留正式回执状态并提供“已生产放行”文案。'
)

assert.match(
  panel,
  /data-active-order-summary-pqc-release-table/,
  '总表必须包含 PQC 生产放行摘要表格。'
)
assert.match(
  panel,
  /生产放行状态[\s\S]*summaryPqcProductionRelease\.statusLabel/,
  '总表必须显示“已生产放行”等后端状态文案。'
)
assert.match(
  panel,
  /生产放行电子签名[\s\S]*data-active-order-summary-pqc-release-signature[\s\S]*formatActiveOrderSignatureCellText\(summaryPqcProductionRelease\.signature\)/,
  '总表必须显示生产放行人的正式电子签名。'
)
assert.match(
  panel,
  /data-active-order-summary-pqc-release-signature[\s\S]*openActiveOrderSignatureRecord\(summaryPqcProductionRelease\.signature\)/,
  '总表生产放行电子签名必须链接到正式签名记录。'
)

console.log('PASS: active-order PQC production release summary signature static contract')
