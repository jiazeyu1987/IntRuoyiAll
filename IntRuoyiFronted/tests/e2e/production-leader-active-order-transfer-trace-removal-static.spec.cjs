const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.join(process.cwd(), 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const forbiddenTokens = [
  '调拨库存追溯',
  'data-team-leader-active-order-transfer-trace',
  'activeOrderTransferTraceRows',
  'activeOrderTransferTraceLoading',
  'activeOrderTransferTraceError',
  'loadActiveOrderTransferTraces',
  'getTeamLeaderActiveOrderTransferTrace',
  'TeamLeaderActiveOrderTransferTraceRespVO',
  'team-leader-workbench__transfer-trace',
  'let listLoaded = false'
]

for (const token of forbiddenTokens) {
  assert.equal(
    source.includes(token),
    false,
    `The active-order page must remove the transfer-trace panel and its page-only chain: ${token}`
  )
}

for (const retainedToken of [
  'data-team-leader-active-order-list',
  'data-team-leader-open-active-order-dialog',
  'data-team-leader-remove-active-order',
  'data-team-leader-report-active-order-abnormal',
  'data-team-leader-active-order-release-apply'
]) {
  assert.ok(source.includes(retainedToken), `The active-order page must retain ${retainedToken}`)
}

console.log('PASS: production leader active-order transfer-trace panel removal static contract')
