const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/system/temporary-role-grant/index.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/system/temporaryRoleGrant/index.ts'), 'utf8')

function assertIncludes(text, snippet) {
  if (!text.includes(snippet)) throw new Error(`Missing snippet: ${snippet}`)
}

assertIncludes(page, '临时角色授权')
assertIncludes(page, '有效截止时间')
assertIncludes(page, '提醒时间')
assertIncludes(page, '审查分类')
assertIncludes(page, '仍有效')
assertIncludes(page, '即将到期')
assertIncludes(page, '异常逾期')
assertIncludes(page, '授权原因')
assertIncludes(page, '审批通过')
assertIncludes(page, '撤销')
assertIncludes(page, '审计记录')
assertIncludes(page, "v-hasPermi=\"['system:temporary-role-grant:create']\"")
assertIncludes(page, "v-hasPermi=\"['system:temporary-role-grant:approve']\"")
assertIncludes(page, "v-hasPermi=\"['system:temporary-role-grant:revoke']\"")
assertIncludes(page, "v-hasPermi=\"['system:temporary-role-grant:query']\"")

for (const endpoint of [
  '/system/temporary-role-grant/page',
  '/system/temporary-role-grant/review-summary',
  '/system/temporary-role-grant/create',
  '/system/temporary-role-grant/approve',
  '/system/temporary-role-grant/revoke',
  '/system/temporary-role-grant/audit-list',
]) {
  assertIncludes(api, endpoint)
}

for (const state of ['PENDING', 'ACTIVE', 'REVOKED', 'EXPIRED']) {
  assertIncludes(api, state)
}

for (const auditState of ['REMIND', 'reviewCategory', 'remindTime']) {
  assertIncludes(api, auditState)
}
