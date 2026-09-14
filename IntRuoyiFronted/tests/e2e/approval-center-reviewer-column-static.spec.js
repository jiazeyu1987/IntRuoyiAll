const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const approvalPage = read('src/views/approval-center/index.vue')
const approvalApi = read('src/api/approval-center/index.ts')

assert.match(
  approvalApi,
  /assigneeUserName\?:\s*string/,
  'approval task API type must expose the resolved reviewer display name.'
)
assert.match(
  approvalApi,
  /assigneeRoleCode\?:\s*string[\s\S]*assigneeRoleName\?:\s*string/,
  'approval task API type must expose the formal reviewer role identity and display name.'
)
assert.match(
  approvalPage,
  /\{\s*key:\s*'reviewer',\s*label:\s*'审核人'/,
  'approval center default columns must include a reviewer column.'
)
assert.match(
  approvalPage,
  /isApprovalColumnVisible\('reviewer'\)[\s\S]*label="审核人"[\s\S]*resolveReviewerLabel\(row\)/,
  'approval center list must render reviewer through the standard user column controls.'
)
assert.match(
  approvalPage,
  /resolveNodeSubLabel\(row\)/,
  'approval center node column must also show reviewer context where users currently look for the pending assignee.'
)
assert.match(
  approvalPage,
  /return reviewerLabel !== EMPTY_APPROVAL_DISPLAY\s*\?\s*`审核人：\$\{reviewerLabel\}`\s*:\s*resolveMappedApprovalText\(row\.businessStatus,\s*APPROVAL_STATUS_LABELS,\s*'未配置中文状态'\)/,
  'node sub label must prefer the reviewer name over the localized TODO status.'
)
assert.match(
  approvalPage,
  /row\.assigneeRoleName\s*\?[\s\S]*`审批角色：\$\{row\.assigneeRoleName\}`[\s\S]*:\s*row\.assigneeUserName\s*\|\|\s*\(row\.assigneeUserId\s*\?\s*`用户 #\$\{row\.assigneeUserId\}`\s*:\s*EMPTY_APPROVAL_DISPLAY\)/,
  'reviewer label must prefer the formal role name and keep the personal assignee display for user-owned tasks.'
)

console.log('PASS: approval center reviewer column static contract')
