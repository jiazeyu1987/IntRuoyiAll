const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

function methodBody(source, signature) {
  const start = source.indexOf(signature)
  assert.notEqual(start, -1, `Missing method ${signature}`)
  const bodyStart = source.indexOf('{', start)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1
    if (source[i] === '}') {
      depth -= 1
      if (depth === 0) return source.slice(bodyStart + 1, i)
    }
  }
  throw new Error(`Unterminated method ${signature}`)
}

const service = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java')
const test = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceTest.java')

const startViewSession = methodBody(service, 'public DccTrainingTaskRespVO startViewSession')
const closeIndex = startViewSession.indexOf(
  'closeOtherActiveSessions(progress, userId, reqVO.getClientSessionId(), now, true);'
)
assert.notEqual(closeIndex, -1, 'startViewSession must settle other active sessions before opening the new one')

const reloadIndex = startViewSession.indexOf('progress = loadOwnedProgressForUpdate(userId, progressId);', closeIndex)
assert.notEqual(
  reloadIndex,
  -1,
  'startViewSession must reload progress after closeOtherActiveSessions settles tail seconds'
)

const existingIndex = startViewSession.indexOf(
  'trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId',
  reloadIndex
)
assert.notEqual(existingIndex, -1, 'new/current session lookup must happen after the progress reload')

const updateIndex = startViewSession.indexOf('updateProgressMetadata(progress, now, false, 0);', reloadIndex)
assert.notEqual(updateIndex, -1, 'startViewSession must update metadata using the reloaded progress')

const closeOtherSessions = methodBody(service, 'private void closeOtherActiveSessions')
assert.match(
  closeOtherSessions,
  /updateProgressMetadata\(progress, now, false, increment\)/,
  'closing stale sessions must add tail seconds to the current persisted progress'
)

assert.match(
  test,
  /void startViewSession_locksProgressAndCountsOnlyOnePreviousTail\(\)/,
  'training task regression test for tail-second preservation is missing'
)
assert.match(
  test,
  /assertEquals\(115, updates\.get\(1\)\.getAccumulatedViewSeconds\(\)\)/,
  'regression must prove the second metadata write does not roll progress back to the old 100 seconds'
)

console.log('DCC-STATIC-012 training session tail seconds contract PASS')
