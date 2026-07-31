const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const files = {
  router: 'src/router/modules/remaining.ts',
  api: 'src/api/mes/pro/processpool/teamLeaderWorkbench.ts',
  page: 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
  timelinePage: 'src/views/mes/pro/processpool/TimelinePage.vue'
}

for (const [name, relativePath] of Object.entries(files)) {
  assert.ok(exists(relativePath), `${name} must exist for the team-leader workbench contract.`)
}

const router = read(files.router)
const api = read(files.api)
const page = read(files.page)
const timelinePage = read(files.timelinePage)

for (const token of [
  "path: 'pro/process-pool/team-leader-workbench'",
  "component: () => import('@/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')",
  "name: 'MesProProcessPoolTeamLeaderWorkbench'",
  "title: '工序池班组长工作台'",
  "permission: ['mes:pro-process-pool-team-leader:query']"
]) {
  assert.ok(router.includes(token), `remaining router must include ${token}.`)
}

for (const token of [
  'getProcessPoolTeamLeaderWorkbenchPage',
  'getProcessPoolTeamLeaderWorkbenchDetail',
  '/mes/pro/process-pool/team-leader-workbench/page',
  '/mes/pro/process-pool/team-leader-workbench/detail',
  'ProcessPoolTeamLeaderWorkbenchVO',
  'ProcessPoolTeamLeaderWorkbenchSummaryVO'
]) {
  assert.ok(api.includes(token), `team leader API wrapper must include ${token}.`)
}

for (const token of [
  "defineOptions({ name: 'MesProProcessPoolTeamLeaderWorkbench' })",
  '班组长工作台',
  '生产工单',
  '提交摘要',
  'PQC',
  'FIFO',
  '审核副本',
  '原始记录修改',
  'getProcessPoolTeamLeaderWorkbenchPage',
  'getProcessPoolTeamLeaderWorkbenchDetail',
  'visibleEventCount',
  'pqcFailureCount',
  'fifoPendingCount',
  'auditCopyPendingCount',
  'modifiedRecordCount'
]) {
  assert.ok(page.includes(token), `team leader page must include ${token}.`)
}

assert.doesNotMatch(page, /mock|localStorage|sessionStorage|generateSubmitProcessPoolReviewCopy|updateProcessPoolOriginalRecord/i,
  'team leader page must be a real read-only workbench without mocks, browser storage, or write actions.')
assert.doesNotMatch(api, /timeline\/page|timeline\/detail/,
  'team leader API wrapper must call the dedicated team-leader endpoint, not the generic timeline endpoint.')
assert.doesNotMatch(timelinePage, /班组长工作台|team-leader-workbench/,
  'generic timeline page must remain separate from the team-leader workbench.')

console.log('PASS: process-pool team-leader workbench static contract')
