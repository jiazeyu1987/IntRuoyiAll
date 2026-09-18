const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const extract = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const queryService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const queryServiceTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java')

const replayIdentity = extract(
  queryService,
  'private boolean isSameCheckinReplay',
  'private boolean matchesCheckinReplayPayload',
  'checkin replay identity'
)
assert.match(
  replayIdentity,
  /Objects\.equals\(latest\.getActorId\(\), userId\)/,
  'checkin replay must bind the original checkout actor'
)
assert.match(
  replayIdentity,
  /Objects\.equals\(latest\.getBaseIterationId\(\), baseIterationId\)/,
  'checkin replay must bind the original base iteration'
)
assert.match(
  replayIdentity,
  /StrUtil\.equals\(latest\.getCheckinUploadTicket\(\), StrUtil\.trimToNull\(reqVO\.getUploadTicket\(\)\)\)/,
  'checkin replay must bind the normalized upload ticket'
)

const replayPayload = extract(
  queryService,
  'private boolean matchesCheckinReplayPayload',
  'private DccWindchillVersionNumber resolveNextIteration',
  'checkin replay payload'
)
assert.match(
  replayPayload,
  /Objects\.equals\(existing\.getPredecessorControlledFileId\(\), latest\.getBaseIterationId\(\)\)/,
  'checkin replay must verify the returned iteration belongs to the same base iteration'
)
assert.match(
  replayPayload,
  /normalizeCheckinReplayPayload\(base, existing\)/,
  'checkin replay must compare the existing iteration normalized payload'
)
assert.match(
  replayPayload,
  /normalizeCheckinReplayPayload\(base, reqVO\)/,
  'checkin replay must compare the incoming normalized payload'
)

for (const testName of [
  'checkinReplayRequiresSameBaseIterationBeforeReturningExistingResult',
  'checkinReplayReturnsExistingOnlyForSameActorBaseTicketAndNormalizedPayload',
  'checkinReplayRejectsDifferentActorEvenWhenUploadTicketIsEmpty',
  'checkinReplayRejectsDifferentNormalizedPayloadWhenUploadTicketIsEmpty'
]) {
  assert.match(queryServiceTest, new RegExp(`void ${testName}\\(`), `${testName} regression is missing`)
}

console.log('DCC-STATIC-009 checkin replay payload contract passed')
