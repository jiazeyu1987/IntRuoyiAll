const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const workspace = path.resolve(__dirname, '../..')
const pageSource = fs.readFileSync(
  path.join(workspace, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)
const apiSource = fs.readFileSync(
  path.join(workspace, 'src/api/mes/pro/processpool/eventRevision.ts'),
  'utf8'
)
const teamLeaderApiSource = fs.readFileSync(
  path.join(workspace, 'src/api/mes/pro/processpool/teamLeader.ts'),
  'utf8'
)

const correctionDialog = () => {
  const start = pageSource.indexOf('data-production-report-correction-dialog')
  assert.notEqual(start, -1, '修改报工弹窗应提供稳定的真实路径定位标记')
  const end = pageSource.indexOf('</el-dialog>', start)
  assert.notEqual(end, -1, '修改报工弹窗模板应完整闭合')
  return pageSource.slice(start, end)
}

test('production report correction dialog exposes business fields instead of internal protocol fields', () => {
  const dialog = correctionDialog()

  for (const expected of [
    '报工信息',
    '完成数量',
    '损耗明细',
    '设备参数',
    '变更预览',
    '修改原因',
    '签名密码',
    '确认修改'
  ]) {
    assert.match(dialog, new RegExp(expected))
  }

  for (const forbidden of [
    '提交事件编号',
    '修改人用户ID',
    '修改签名ID',
    '签名用户ID',
    'payload JSON',
    '签名快照JSON',
    '字段变更JSON'
  ]) {
    assert.doesNotMatch(dialog, new RegExp(forbidden))
  }
})

test('production leader correction uses the business endpoint and current-user password signature', () => {
  assert.match(apiSource, /ProcessPoolProductionReportCorrectionReqVO/)
  assert.match(apiSource, /signaturePassword:\s*string/)
  assert.match(apiSource, /\/event-revision\/correct-production-report/)
  assert.match(pageSource, /correctProcessPoolProductionReport\(buildCorrectionRequest\(\)\)/)

  const requestContract = apiSource.slice(
    apiSource.indexOf('export interface ProcessPoolProductionReportCorrectionReqVO'),
    apiSource.indexOf('export const correctProcessPoolProductionReport')
  )
  assert.doesNotMatch(requestContract, /modifiedByUserId/)
  assert.doesNotMatch(requestContract, /revisionSignatureId/)
  assert.doesNotMatch(requestContract, /revisionSignatureUserId/)
  assert.doesNotMatch(requestContract, /revisionSignatureSnapshot/)
  assert.doesNotMatch(requestContract, /changedFields/)
  assert.doesNotMatch(requestContract, /afterPayload/)
})

test('production report correction blocks an unchanged submission before calling the API', () => {
  assert.match(pageSource, /correctionChangePreview/)
  assert.match(pageSource, /没有可提交的变更/)
  assert.match(pageSource, /data-production-report-correction-change-preview/)
  assert.match(
    pageSource,
    /const canCorrectSubmission = \(row: ProcessPoolTimelineEventVO\) =>[\s\S]*!\(isProductionReportHistoryTab\.value \|\| isPqcFormHistoryTab\.value\)[\s\S]*\(isProductionLeader\.value \|\| row\.submissionReviewStatus === 'REJECTED'\)/
  )
})

test('production report row action moves revision history out of the workbench', () => {
  assert.doesNotMatch(pageSource, /data-production-report-revision-log-dialog/)
  assert.doesNotMatch(pageSource, /data-production-report-revision-log-event-id/)
  assert.doesNotMatch(pageSource, /openRevisionLogs\(row\)/)
  assert.doesNotMatch(pageSource, /getProcessPoolProductionReportRevisionLogs/)
  assert.doesNotMatch(pageSource, />\s*修改记录\s*</)
})

test('production report row action exposes allocation against formal active orders', () => {
  assert.match(pageSource, /data-production-report-allocation-event-id/)
  assert.match(pageSource, />\s*分配\s*</)
  assert.match(pageSource, /@click="openAllocation\(row\)"/)
  assert.match(pageSource, /const canAllocateSubmission = \(row: ProcessPoolTimelineEventVO\)/)
  assert.match(pageSource, /const openAllocation = async \(event: ProcessPoolTimelineEventVO\)/)
  assert.match(pageSource, /reviewDialogMode\.value\s*=\s*'ALLOCATION'/)
  assert.match(pageSource, /reviewForm\.reviewStatus\s*=\s*'APPROVED'/)
  assert.match(pageSource, /confirmTeamLeaderReportAllocation\(/)
  assert.match(teamLeaderApiSource, /\/submission\/allocation\/confirm/)
})

test('production report revision log readable contract remains service-side only', () => {
  assert.match(apiSource, /ProcessPoolProductionReportRevisionLogVO/)
  assert.match(apiSource, /modifiedByName:\s*string/)
  assert.match(apiSource, /signatureConfirmed:\s*boolean/)
  assert.match(apiSource, /\/event-revision\/production-report-logs/)

  const responseContract = apiSource.slice(
    apiSource.indexOf('export interface ProcessPoolProductionReportRevisionLogVO'),
    apiSource.indexOf('export const getProcessPoolProductionReportRevisionLogs')
  )
  for (const forbidden of [
    /eventId/,
    /revisionId/,
    /modifiedByUserId/,
    /revisionSignatureId/,
    /revisionSignatureSnapshot/,
    /fieldCode/,
    /beforePayload/,
    /afterPayload/
  ]) {
    assert.doesNotMatch(responseContract, forbidden)
  }
})
