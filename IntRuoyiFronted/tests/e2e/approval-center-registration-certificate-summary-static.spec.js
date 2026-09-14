const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenter = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')
const approvalApi = fs.readFileSync(path.join(root, 'src/api/approval-center/index.ts'), 'utf8')

assert.match(
  approvalCenter,
  /v-if="!row\.businessIdentifierHidden"[\s\S]{0,160}resolveBusinessIdentifierLabel\(row\)/,
  '注册证上传/延续审批行不得显示业务键或业务编号占位行。'
)

assert.match(
  approvalCenter,
  /v-if="resolveVisibleBusinessContextTags\(row\)\.length"[\s\S]*v-for="tag in resolveVisibleBusinessContextTags\(row\)"/,
  '审批中心摘要标签必须先过滤空值，注册证编号等缺失字段不得渲染为空标签。'
)
assert.match(
  approvalCenter,
  /const resolveVisibleBusinessContextTags = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*\.filter\(isBusinessContextTagVisible\)/,
  '审批中心业务摘要标签必须通过统一可见性函数过滤，不能只处理注册证。'
)
assert.match(
  approvalCenter,
  /const resolveVisibleDccKeyFields = \(row: ApprovalTaskSummaryVO\) =>[\s\S]*resolveDccKeyFields\(row\)[\s\S]*\.filter\(\(field\) => isApprovalDisplayValueVisible\(field\.value\)\)/,
  'DCC 固定摘要字段也必须按统一可见性过滤，缺失的版本、文件类型等不得显示为空值。'
)
assert.match(
  approvalCenter,
  /v-if="resolveVisibleDccKeyFields\(row\)\.length"[\s\S]*v-for="field in resolveVisibleDccKeyFields\(row\)"/,
  'DCC 固定摘要行必须只渲染过滤后的可见字段。'
)
assert.match(
  approvalCenter,
  /const EMPTY_CONTEXT_VALUE_PATTERN = \/\^\(\?:--\|null\|undefined\)\$\/i/,
  '摘要展示必须把空值 token 视为缺失字段，不能渲染 null 或 undefined 标签。'
)
assert.match(
  approvalApi,
  /businessIdentifierHidden\?: boolean/,
  '审批摘要接口必须显式声明是否隐藏业务编号行，前端不得通过标题或标签猜测。'
)
assert.doesNotMatch(
  approvalCenter,
  /const isRegistrationCertificateSummary/,
  '前端不得通过注册证标题和标签组合猜测摘要类型。'
)

console.log('PASS: approval center registration certificate summary static contract')
