import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'

const repoRoot = path.resolve(import.meta.dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const panel = read(
  'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
)
const teamLeaderApi = read('IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts')
const signatureRecordsPane = read(
  'IntRuoyiFronted/src/views/signature-governance/components/SignatureGovernanceRecordsPane.vue'
)
const responseVo = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/vo/MesTeamLeaderActiveOrderDetailRespVO.java'
)
const serviceDetail = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderDetail.java'
)
const mapperXml = read(
  'IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProcessPoolActiveOrderDetailReadMapper.xml'
)

assert.match(
  serviceDetail,
  /class SignatureDetail[\s\S]*signatureId[\s\S]*signerName[\s\S]*signedAt[\s\S]*role/,
  '后端业务详情必须提供签名行为对象，而不是只返回人员姓名。'
)

assert.match(
  responseVo,
  /class SignatureDetail[\s\S]*private Long signatureId[\s\S]*private String signerName[\s\S]*private LocalDateTime signedAt[\s\S]*private String role/,
  '详情响应 VO 必须暴露 signatureId、signerName、signedAt、role。'
)

assert.match(
  teamLeaderApi,
  /export interface TeamLeaderActiveOrderSignatureDetailRespVO[\s\S]*signatureId\?: number[\s\S]*signerName\?: string[\s\S]*signedAt\?: string \| number[\s\S]*role: string/,
  '前端 API 类型必须包含可点击签名对象。'
)

assert.match(
  mapperXml,
  /pool_event\.signature_id AS submitterSignatureId[\s\S]*latest_review\.review_signature_id AS reviewerSignatureId[\s\S]*latest_review\.reviewed_at AS reviewerSignedAt/,
  '生产提交详情查询必须读取提交与复核签名事实。'
)

assert.match(
  mapperXml,
  /pool_event\.signature_id AS submitterSignatureId[\s\S]*pool_event\.server_submit_time AS submitterSignedAt[\s\S]*latest_review\.review_signature_id AS reviewerSignatureId/,
  'PQC 事件参与人查询必须读取提交与复核签名事实。'
)

assert.match(
  panel,
  /formatActiveOrderSignatureCellText[\s\S]*未签名/,
  '表单签名单元格必须格式化为签名人（签名时间），缺失时显示未签名。'
)
assert.match(panel, /签名人未记录/, '签名单元格必须明确展示缺少签名人。')
assert.match(panel, /签名时间未记录/, '签名单元格必须明确展示缺少签名时间。')

assert.match(
  panel,
  /openActiveOrderSignatureRecord[\s\S]*\/signature-governance\/signature-records[\s\S]*quickFilterField[\s\S]*keyword[\s\S]*quickFilterValue/,
  '签名单元格点击必须跳转到签名记录页面并携带关键字筛选。'
)

assert.match(
  panel,
  /data-active-order-production-record-submitter-signature[\s\S]*openActiveOrderSignatureRecord/,
  '生产表单必须给提交人签名单元格提供点击入口。'
)

assert.match(
  panel,
  /data-active-order-pqc-inspection-record-inspector-signature[\s\S]*openActiveOrderSignatureRecord/,
  'PQC表单必须给检验人签名单元格提供点击入口。'
)

assert.match(
  signatureRecordsPane,
  /useRoute\(\)[\s\S]*quickFilterField[\s\S]*quickFilterValue[\s\S]*recordQuickFilter\.applyQuickFilter/,
  '签名记录页必须消费 URL query，支持从业务表单链接跳转后自动筛选。'
)

console.log('active-order-submission-form-signature-link-static: PASS')
