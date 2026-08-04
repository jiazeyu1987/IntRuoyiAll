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

assert.match(
  tableTemplate,
  /resolveSourceTaskTypeLabel\(row\)/,
  '来源列必须把 sourceTaskType 转成中文显示，不能直出英文内部码。'
)
assert.doesNotMatch(
  tableTemplate,
  /\{\{\s*row\.sourceTaskType\s*\}\}/,
  '来源列不得直接显示 row.sourceTaskType。'
)

assert.match(
  tableTemplate,
  /resolveBusinessTitleLabel\(row\)/,
  '业务摘要标题必须把 BPM 英文流程名转成中文显示。'
)
assert.doesNotMatch(
  tableTemplate,
  /\{\{\s*row\.businessTitle\s*\|\|\s*'--'\s*\}\}/,
  '业务摘要标题不得直接显示 row.businessTitle。'
)

assert.match(
  tableTemplate,
  /resolveBusinessIdentifierLabel\(row\)/,
  '业务摘要编号行必须把 FORM_ACTION 等英文前缀转成中文显示。'
)
assert.doesNotMatch(
  tableTemplate,
  /\{\{\s*row\.businessCode\s*\|\|\s*row\.businessKey\s*\|\|\s*row\.sourceTaskId\s*\|\|\s*'--'\s*\}\}/,
  '业务摘要编号行不得直接显示英文业务键前缀。'
)

assert.match(
  tableTemplate,
  /resolveNodeNameLabel\(row\)/,
  '节点主行必须通过中文化函数显示，不能直出英文节点码。'
)
assert.doesNotMatch(
  tableTemplate,
  /\{\{\s*row\.currentNodeName\s*\|\|\s*row\.currentNodeCode\s*\|\|\s*'--'\s*\}\}/,
  '节点主行不得直接显示 currentNodeCode。'
)

assert.match(
  approvalCenter,
  /const APPROVAL_SOURCE_TASK_TYPE_LABELS:\s*Record<string,\s*string>\s*=\s*\{[\s\S]*BPM_PROCESS_INSTANCE:\s*'流程实例'[\s\S]*BPM_PROCESS_INSTANCE_COPY:\s*'流程抄送实例'[\s\S]*DCC_CONTROLLED_FILE_TASK:\s*'文控受控文件任务'/,
  '审批中心必须登记 BPM 和 DCC sourceTaskType 纯中文显示名。'
)
assert.match(
  approvalCenter,
  /const APPROVAL_BUSINESS_TITLE_LABELS:\s*Record<string,\s*string>\s*=\s*\{[\s\S]*'DCC Controlled File Approval':\s*'文控受控文件审批'/,
  '审批中心必须登记 DCC Controlled File Approval 的纯中文显示名。'
)
assert.match(
  approvalCenter,
  /const APPROVAL_STATUS_LABELS:\s*Record<string,\s*string>\s*=\s*\{[\s\S]*MY_INITIATED:\s*'我发起的'[\s\S]*TODO:\s*'待办'[\s\S]*DONE:\s*'已办'[\s\S]*CC:\s*'抄送我的'/,
  '审批中心必须登记 MY_INITIATED/TODO/DONE/CC 的中文显示名。'
)
assert.match(
  approvalCenter,
  /FORM_ACTION:\s*'表单动作'/,
  '审批中心必须把 FORM_ACTION 前缀显示为中文。'
)

console.log('PASS: approval center yellow-area Chinese copy static contract')
