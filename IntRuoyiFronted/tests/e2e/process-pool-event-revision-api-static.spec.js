const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const eventRevisionApiPath = path.join(root, 'src/api/mes/pro/processpool/eventRevision.ts')
const timelineApiPath = path.join(root, 'src/api/mes/pro/processpool/index.ts')

const eventRevisionApi = fs.readFileSync(eventRevisionApiPath, 'utf8')
const timelineApi = fs.readFileSync(timelineApiPath, 'utf8')
const eventRevisionPage = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/EventRevisionPage.vue'),
  'utf8'
)

function blockBetween(source, startPattern, endPattern) {
  const start = source.search(startPattern)
  assert.notEqual(start, -1, `Missing start pattern: ${startPattern}`)
  const rest = source.slice(start)
  const end = rest.search(endPattern)
  assert.notEqual(end, -1, `Missing end pattern: ${endPattern}`)
  return rest.slice(0, end)
}

const updateReqInterface = blockBetween(
  eventRevisionApi,
  /export interface ProcessPoolEventRevisionUpdateReqVO/,
  /export interface ProcessPoolProductionReportCorrectionLossDetailReqVO/
)
const fieldChangeInterface = blockBetween(
  eventRevisionApi,
  /export interface ProcessPoolEventRevisionFieldChangeVO/,
  /export interface ProcessPoolEventRevisionUpdateReqVO/
)
const buildRequestPayload = blockBetween(
  eventRevisionPage,
  /const buildRequestPayload = \(\): ProcessPoolEventRevisionUpdateReqVO => \{/,
  /const handleSubmit = async/
)

assert.match(
  eventRevisionApi,
  /export interface ProcessPoolEventRevisionUpdateReqVO/,
  'F6 event revision API wrapper must expose the update request contract.'
)
for (const field of [
  'eventId',
  'afterPayload',
  'changeReason',
  'signaturePassword',
  'changedFields'
]) {
  assert.match(updateReqInterface, new RegExp(`${field}\\??:`), `F6 event revision wrapper must include ${field}.`)
}
for (const field of ['sourceQuantityFragmentId', 'originalField']) {
  assert.match(fieldChangeInterface, new RegExp(`${field}\\??:`), `F6 event revision diff must include ${field}.`)
}
for (const forbidden of [
  'modifiedByUserId',
  'revisionSignatureId',
  'revisionSignatureUserId',
  'revisionSignatureSnapshot',
  'revisionSignatureSnapshotJson'
]) {
  assert.doesNotMatch(updateReqInterface, new RegExp(forbidden), `F6 request contract must not expose ${forbidden}.`)
  assert.doesNotMatch(buildRequestPayload, new RegExp(forbidden), `F6 submit payload must not send ${forbidden}.`)
}
assert.match(eventRevisionPage, /v-model="revisionForm\.signaturePassword"/,
  'F6 event revision page must ask only for the current account signature password.')
assert.doesNotMatch(eventRevisionPage, /修改人用户ID|修改签名ID|签名员工用户ID|修改签名快照JSON/,
  'F6 event revision page must not ask the user to type audit identity or signature evidence.')
assert.match(
  eventRevisionApi,
  /export const updateProcessPoolOriginalRecord = async \(data: ProcessPoolEventRevisionUpdateReqVO\)/,
  'F6 event revision API wrapper must expose a dedicated submit function.'
)
assert.match(
  eventRevisionApi,
  /request\.post<number>\(\{\s*url: '\/mes\/pro\/process-pool\/event-revision\/update-original',\s*data\s*\}\)/,
  'F6 original revision must use the approved dedicated POST endpoint.'
)
assert.doesNotMatch(
  eventRevisionApi,
  /\/mes\/pro\/process-pool\/timeline/,
  'F6 event revision write wrapper must not call timeline read APIs.'
)
assert.doesNotMatch(
  timelineApi,
  /request\.post|update-original|event-revision|updateProcessPoolOriginalRecord/,
  'Process-pool timeline API must remain read-only and must not expose F6 write operations.'
)

console.log('PASS: process-pool event revision API wrapper is separated from read-only timeline API')
