const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.resolve(frontendRoot, relativePath), 'utf8')

const releaseApi = read('src/api/mes/pro/edhr/release.ts')
const boardPage = read('src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue')
const traceTab = read('src/views/mes/pro/edhr/form-trace/FormTraceReleaseTab.vue')

for (const field of [
  'releaseTransactionId?: string',
  'batchExecutionId: string',
  'releaseApprovalWorkTaskId?: string',
  'version: number',
  'workTaskId: string',
  'expectedVersion: number'
]) {
  assert(releaseApi.includes(field), `Missing precision-safe manager release field: ${field}`)
}
assert.match(
  releaseApi,
  /export const getEdhrRelease = async \(id: string\)/,
  'Manager release receipt lookup must preserve the JSON Long identifier as a string'
)

for (const marker of [
  'data-manager-release-approve',
  'isManagerReleaseTask',
  'canHandleManagerRelease',
  "row.businessScopeType === 'RELEASE_TRANSACTION'",
  "v-hasPermi=\"['mes:pro-edhr-release:approve']\"",
  'openManagerReleaseDialog',
  'submitManagerReleaseApproval',
  'approveEdhrRelease',
  'recoverUncertainManagerReleaseApproval',
  'getEdhrRelease'
]) {
  assert(boardPage.includes(marker), `Missing manager release workbench behavior: ${marker}`)
}

assert.match(
  boardPage,
  /approveEdhrRelease\(\{[\s\S]*?releaseTransactionId:[\s\S]*?workTaskId:[\s\S]*?expectedVersion,[\s\S]*?idempotencyKey:[\s\S]*?signoffEvidenceHash:/,
  'Manager approval must submit the frozen task, optimistic version, idempotency key and signoff evidence'
)
assert.match(
  boardPage,
  /recoverUncertainManagerReleaseApproval[\s\S]*?getEdhrRelease\([\s\S]*?receipt\.releaseStatus !== 'RELEASED'/,
  'An uncertain approval response must be recovered only from the authoritative RELEASED receipt'
)
assert.match(
  boardPage,
  /最终放行已确认[\s\S]*?await getList\(\)[\s\S]*?待办列表刷新失败/,
  'A confirmed release must remain successful when the follow-up list refresh fails'
)

const managerActionBlock = boardPage.match(
  /<div v-else-if="isManagerReleaseTask\(row\)">([\s\S]*?)<div v-else-if="isProductionReleaseReportTask\(row\)">/
)
assert(managerActionBlock, 'Manager release tasks must have a dedicated action block')
assert.doesNotMatch(
  managerActionBlock[1],
  /reject|withdraw|驳回|退回|撤回/i,
  'The first manager release version must not expose reject, return or withdraw actions'
)

assert.match(
  traceTab,
  /completedTraceOnly:\s*true[\s\S]*?releaseStatus:\s*'RELEASED'|releaseStatus:\s*'RELEASED'[\s\S]*?completedTraceOnly:\s*true/,
  'The completed release trace query must always send completedTraceOnly=true and releaseStatus=RELEASED'
)

assert(
  !/default-success/i.test(`${releaseApi}\n${boardPage}\n${traceTab}`),
  'Default-success paths are forbidden'
)

console.log('PASS: SP-4 manager release and trace frontend contract')
