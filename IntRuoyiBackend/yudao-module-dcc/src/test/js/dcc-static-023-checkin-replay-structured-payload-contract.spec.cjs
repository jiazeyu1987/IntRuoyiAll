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

const legacyNormalize = (changeDescription, remark) =>
  `changeDescription=${changeDescription ?? ''}\nremark=${remark ?? ''}`

const payloadA = {
  changeDescription: '核对\nremark=旧说明',
  remark: '最终说明'
}
const payloadB = {
  changeDescription: '核对',
  remark: '旧说明\nremark=最终说明'
}

assert.equal(
  legacyNormalize(payloadA.changeDescription, payloadA.remark),
  legacyNormalize(payloadB.changeDescription, payloadB.remark),
  'fixture must reproduce the legacy delimiter collision'
)
assert.notDeepEqual(
  [payloadA.changeDescription, payloadA.remark],
  [payloadB.changeDescription, payloadB.remark],
  'fixture payloads must remain different as business fields'
)

const queryService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const replayPayload = extract(
  queryService,
  'private boolean matchesCheckinReplayPayload',
  'private DccWindchillVersionNumber resolveNextIteration',
  'checkin replay payload'
)

assert.match(
  replayPayload,
  /Objects\.equals\(normalizeCheckinReplayPayload\(base, existing\),\s*normalizeCheckinReplayPayload\(base, reqVO\)\)/,
  'checkin replay must still compare normalized existing and incoming payloads'
)
assert.doesNotMatch(
  replayPayload,
  /private\s+String\s+normalizeCheckinReplayPayload\s*\(\s*String\s+changeDescription,\s*String\s+remark\s*\)/,
  'normalized replay payload must not collapse fields into an ambiguous String'
)
assert.doesNotMatch(
  replayPayload,
  /"changeDescription="\s*\+|\\nremark=/,
  'normalized replay payload must not use unescaped text delimiters'
)
assert.match(
  replayPayload,
  /record\s+CheckinReplayPayload\s*\(\s*String\s+changeDescription,\s*String\s+remark\s*\)/,
  'normalized replay payload must use a structured field tuple'
)
assert.match(
  replayPayload,
  /new\s+CheckinReplayPayload\s*\(\s*StrUtil\.trim\(reqVO\.getChangeDescription\(\)\),\s*effectiveRemark\s*\)/,
  'incoming replay payload must preserve normalized fields separately'
)
assert.match(
  replayPayload,
  /StrUtil\.isBlank\(reqVO\.getRemark\(\)\)\s*\?\s*StrUtil\.trim\(base\.getRemark\(\)\)\s*:\s*StrUtil\.trim\(reqVO\.getRemark\(\)\)/,
  'incoming blank remark must retain existing base remark semantics'
)

console.log('DCC-STATIC-023 checkin replay structured payload contract passed')
