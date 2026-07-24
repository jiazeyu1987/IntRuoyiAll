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
  /return reviewerLabel !== '--' \? `审核人：\$\{reviewerLabel\}` : row\.businessStatus \|\| '--'/,
  'node sub label must prefer the reviewer name over the raw TODO status.'
)
assert.match(
  approvalPage,
  /row\.assigneeUserName\s*\|\|\s*\(row\.assigneeUserId\s*\?\s*`用户 #\$\{row\.assigneeUserId\}`\s*:\s*'--'\)/,
  'reviewer label must show the resolved name and fail visibly to the user id when name data is absent.'
)

console.log('PASS: approval center reviewer column static contract')
