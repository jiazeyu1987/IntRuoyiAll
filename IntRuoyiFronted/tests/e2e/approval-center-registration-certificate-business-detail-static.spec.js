const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.ok(end > start, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const approvalCenter = readSource('src/views/approval-center/index.vue')
const registrationDetail = readSource('src/views/dcc/registration-certificate/detail/index.vue')
const backendProvider = fs.readFileSync(
  path.resolve(
    root,
    '../IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/BpmNativeApprovalTaskProvider.java'
  ),
  'utf8'
)

const openModuleDetail = extractBetween(
  approvalCenter,
  'const openModuleDetail = (row: ApprovalTaskSummaryVO) => {',
  'const openProcessFlow =',
  'approval center view action'
)
assert.match(
  openModuleDetail,
  /resolveDecisionDetailRoute\(row\)/,
  '审批中心“查看”必须优先打开后端返回的正式业务详情路由。'
)
assert.match(
  openModuleDetail,
  /resolveDecisionDetailQuery\(row\)/,
  '审批中心“查看”必须携带正式业务详情 query，保留 requestId/processInstanceId 等上下文。'
)
assert.doesNotMatch(
  openModuleDetail,
  /row\.detailRoute,\s*row\.detailQuery\s*\|\|\s*\{\}/,
  '审批中心“查看”不能继续固定进入 BPM 流程详情。'
)

const canOpenView = extractBetween(
  approvalCenter,
  'const canOpenView = (row: ApprovalTaskSummaryVO) => {',
  'const resolveViewDisabledReason =',
  'approval center view availability'
)
assert.match(
  canOpenView,
  /Boolean\(resolveDecisionDetailRoute\(row\)\)/,
  '审批中心“查看”必须按模块语义解析业务详情入口。'
)

const decisionRouteResolver = extractBetween(
  approvalCenter,
  'const resolveDecisionDetailRoute = (row: ApprovalTaskSummaryVO) => {',
  'const resolveDecisionDetailQuery =',
  'approval center decision detail route resolver'
)
assert.match(
  decisionRouteResolver,
  /return row\.decisionDetailRoute\s*$/m,
  '审批中心业务详情路由不得回退到流程详情路由。'
)
assert.doesNotMatch(
  decisionRouteResolver,
  /\|\|\s*row\.detailRoute/,
  '审批中心“查看”不得使用流程详情路由作为业务详情回退。'
)
assert.match(
  decisionRouteResolver,
  /row\.moduleCode\s*!==\s*'BPM'\s*\?\s*row\.detailRoute/,
  '非 BPM 模块的 detailRoute 是正式业务详情入口，必须保留。'
)

assert.match(
  backendProvider,
  /REGISTRATION_CERTIFICATE_DETAIL_ROUTE_PREFIX\s*=[\s\r\n]*"\/mdm\/registration-certificate\/detail\/"/,
  'BPM 原生审批 provider 必须声明注册证正式详情路由前缀。'
)
assert.match(
  backendProvider,
  /if\s*\(isRegistrationCertificateAccessApproval\(variables\)\)\s*\{[\s\S]*REGISTRATION_CERTIFICATE_DETAIL_ROUTE_PREFIX[\s\S]*certificateId/,
  'BPM 原生审批 provider 必须从正式 certificateId 生成注册证业务详情路由。'
)
assert.match(
  backendProvider,
  /putIfPresent\(query,\s*"requestId",\s*firstText\(variables\.get\("registrationCertificateAccessRequestId"\),[\s\r\n]*\s*variables\.get\("requestId"\)\)\)/,
  '注册证业务详情 query 必须保留访问申请 requestId。'
)

assert.match(
  registrationDetail,
  /const props = defineProps<\{[\s\S]*id\?:\s*string\s*\|\s*number/,
  '注册证详情页必须声明 BPM 自定义表单传入的 id prop。'
)
assert.match(
  registrationDetail,
  /REGISTRATION_CERTIFICATE_ACCESS_BUSINESS_KEY_PREFIX\s*=\s*'DCC_REG_CERT_ACCESS:'/,
  '注册证详情页必须识别注册证访问申请的正式 BPM businessKey 前缀。'
)
assert.match(
  registrationDetail,
  /getRegistrationCertificateAccessRequestStatus/,
  '注册证详情页在 BPM 嵌入场景必须通过访问申请正式接口读取 certificateId。'
)
const resolveCertificateIdForDetail = extractBetween(
  registrationDetail,
  'const resolveCertificateIdForDetail = async () => {',
  'const loadDetail = async () => {',
  'registration certificate BPM identity resolver'
)
assert.match(
  resolveCertificateIdForDetail,
  /getRegistrationCertificateAccessRequestStatus\(accessRequestId\.value\)/,
  'BPM 嵌入场景必须用访问申请 ID 读取正式 certificateId。'
)
assert.doesNotMatch(
  resolveCertificateIdForDetail,
  /parsePositiveRouteQueryId\(props\.id\)/,
  'BPM 嵌入场景不得把 businessKey 直接冒充注册证主键。'
)

console.log('approval center registration certificate business detail static contract passed')
