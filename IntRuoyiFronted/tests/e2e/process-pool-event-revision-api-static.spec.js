const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const eventRevisionApiPath = path.join(root, 'src/api/mes/pro/processpool/eventRevision.ts')
const timelineApiPath = path.join(root, 'src/api/mes/pro/processpool/index.ts')

const eventRevisionApi = fs.readFileSync(eventRevisionApiPath, 'utf8')
const timelineApi = fs.readFileSync(timelineApiPath, 'utf8')

assert.match(
  eventRevisionApi,
  /export interface ProcessPoolEventRevisionUpdateReqVO/,
  'F6 event revision API wrapper must expose the update request contract.'
)
for (const field of [
  'eventId',
  'afterPayload',
  'changeReason',
  'revisionSignatureId',
  'revisionSignatureUserId',
  'revisionSignatureSnapshot',
  'changedFields',
  'sourceQuantityFragmentId',
  'originalField'
]) {
  assert.match(eventRevisionApi, new RegExp(`${field}\\??:`), `F6 event revision wrapper must include ${field}.`)
}
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
