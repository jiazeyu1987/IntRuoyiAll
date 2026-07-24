const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = read('src/views/approval-center/index.vue')
const api = read('src/api/approval-center/index.ts')

assert.match(api, /approvalResult\?:\s*ApprovalTaskReviewResult/, 'approval summary API must expose approvalResult')
assert.match(api, /approvalRemark\?:\s*string/, 'approval summary API must expose approvalRemark')

for (const column of [
  ["key: 'approvalResult'", "label: '审批结果'"],
  ["key: 'approvalRemark'", "label: '备注'"]
]) {
  assert.ok(page.includes(column[0]), `approval default columns must include ${column[0]}`)
  assert.ok(page.includes(column[1]), `approval default columns must include ${column[1]}`)
}

assert.match(
  page,
  /queryParams\.viewType === 'DONE'[\s\S]*isApprovalColumnVisible\('approvalResult'\)[\s\S]*label="审批结果"/,
  'approval result column must render only in DONE view'
)
assert.match(
  page,
  /queryParams\.viewType === 'DONE'[\s\S]*isApprovalColumnVisible\('approvalRemark'\)[\s\S]*label="备注"/,
  'approval remark column must render only in DONE view'
)
assert.match(page, /resolveApprovalResultLabel\(row\.approvalResult\)/, 'approval result label resolver must be used')
assert.match(page, /resolveApprovalResultTagType\(row\.approvalResult\)/, 'approval result tag color resolver must be used')
assert.match(page, /resolveApprovalRemark\(row\)/, 'approval remark resolver must be used')
assert.match(page, /APPROVE:\s*'通过'/, 'APPROVE must display as 通过')
assert.match(page, /REJECT:\s*'驳回'/, 'REJECT must display as 驳回')
assert.match(page, /APPROVE:\s*'success'/, 'APPROVE must use green success tag')
assert.match(page, /REJECT:\s*'danger'/, 'REJECT must use red danger tag')

console.log('PASS: approval center done result remark static contract')
