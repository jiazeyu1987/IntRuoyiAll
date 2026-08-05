const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const approvalCenter = fs.readFileSync(path.join(root, 'src/views/approval-center/index.vue'), 'utf8')

const tableStart = approvalCenter.indexOf('<el-table')
const tableEnd = approvalCenter.indexOf('</el-table>', tableStart)
assert.notEqual(tableStart, -1, '审批中心必须保留审批任务表格。')
assert.notEqual(tableEnd, -1, '审批中心表格模板必须完整。')

const tableTemplate = approvalCenter.slice(tableStart, tableEnd)
const businessSummaryColumnIndex = tableTemplate.indexOf("isApprovalColumnVisible('businessSummary')")
const applicantColumnIndex = tableTemplate.indexOf("isApprovalColumnVisible('applicant')")
const nodeColumnIndex = tableTemplate.indexOf("isApprovalColumnVisible('node')")

assert.ok(businessSummaryColumnIndex >= 0, '审批中心必须保留业务摘要列。')
assert.ok(applicantColumnIndex > businessSummaryColumnIndex, '申请人列必须位于业务摘要列之后。')
assert.ok(nodeColumnIndex > applicantColumnIndex, '申请人列必须位于节点列之前。')

assert.match(
  tableTemplate,
  /v-if="isApprovalColumnVisible\('applicant'\)"[\s\S]*?label="申请人"[\s\S]*?prop="applicant"[\s\S]*?getApprovalColumnWidthString\('applicant',\s*140\)[\s\S]*?resolveApplicantLabel\(row\)/,
  '审批中心必须通过统一用户列配置渲染独立申请人列。'
)

assert.match(
  approvalCenter,
  /\{\s*key:\s*'applicant',\s*label:\s*'申请人',\s*width:\s*140\s*\}/,
  '审批中心默认列集合必须包含申请人。'
)
assert.match(
  approvalCenter,
  /const resolveApplicantLabel = \(row: ApprovalTaskSummaryVO\) =>\s*row\.initiatorUserId\s*\?\s*`用户 #\$\{row\.initiatorUserId\}`\s*:\s*EMPTY_APPROVAL_DISPLAY/,
  '申请人列必须读取正式 initiatorUserId，缺失时使用既有空值语义。'
)

for (const tableKey of [
  'approval.center.todo.applicant.v1',
  'approval.center.done.applicant.v1',
  'approval.center.myInitiated.applicant.v1',
  'approval.center.cc.applicant.v1'
]) {
  assert.ok(approvalCenter.includes(`'${tableKey}'`), `审批中心必须使用升级后的表格配置键：${tableKey}`)
}

const dccKeyFieldsMatch = approvalCenter.match(
  /const resolveDccKeyFields = \(row: ApprovalTaskSummaryVO\) => \[[\s\S]*?\n\]/
)
assert.ok(dccKeyFieldsMatch, '审批中心必须保留 DCC 关键字段摘要。')
assert.doesNotMatch(
  dccKeyFieldsMatch[0],
  /label:\s*'申请人'/,
  'DCC 业务摘要不得与独立申请人列重复显示申请人。'
)

console.log('PASS: approval center applicant column static contract')
