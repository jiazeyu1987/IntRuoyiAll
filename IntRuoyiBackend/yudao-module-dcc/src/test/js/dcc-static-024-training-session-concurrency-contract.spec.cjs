const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const servicePath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java'
)
const progressMapperPath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileTrainingProgressMapper.java'
)

const service = fs.readFileSync(servicePath, 'utf8')
const progressMapper = fs.readFileSync(progressMapperPath, 'utf8')

const extractMethod = (source, signatureNeedle, label) => {
  const start = source.indexOf(signatureNeedle)
  assert.notEqual(start, -1, `${label} missing method signature`)
  const bodyStart = source.indexOf('{', start)
  assert.notEqual(bodyStart, -1, `${label} missing method body`)
  let depth = 0
  for (let i = bodyStart; i < source.length; i += 1) {
    const char = source[i]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return source.slice(start, i + 1)
      }
    }
  }
  assert.fail(`${label} body was not closed`)
}

const assertOrder = (source, first, second, label) => {
  const firstIndex = source.indexOf(first)
  const secondIndex = source.indexOf(second)
  assert.notEqual(firstIndex, -1, `${label} missing first marker: ${first}`)
  assert.notEqual(secondIndex, -1, `${label} missing second marker: ${second}`)
  assert.ok(firstIndex < secondIndex, `${label} must place "${first}" before "${second}"`)
}

assert.match(
  progressMapper,
  /@Select\("SELECT \* FROM dcc_controlled_file_training_progress WHERE id = #\{id\} AND deleted = 0 FOR UPDATE"\)/,
  'DCC training progress mapper must expose a row-locking select for the progress row.'
)
assert.match(
  progressMapper,
  /DccControlledFileTrainingProgressDO\s+selectByIdForUpdate\(@Param\("id"\)\s+Long\s+id\)/,
  'DCC training progress mapper must declare selectByIdForUpdate(Long id).'
)

const lockLoader = extractMethod(service, 'private DccControlledFileTrainingProgressDO loadOwnedProgressForUpdate', 'training progress lock loader')
assert.match(
  lockLoader,
  /trainingProgressMapper\.selectByIdForUpdate\(progressId\)/,
  'The owned-progress lock loader must lock the same training progress row before mutation.'
)

const startMethod = extractMethod(service, 'public DccTrainingTaskRespVO startViewSession', 'startViewSession')
assert.match(startMethod, /loadOwnedProgressForUpdate\(userId, progressId\)/,
  'startViewSession must lock the progress row before closing or inserting sessions.')
assertOrder(startMethod, 'loadOwnedProgressForUpdate(userId, progressId)', 'closeOtherActiveSessions', 'startViewSession')
assert.match(startMethod, /closeOtherActiveSessions\(progress, userId, reqVO\.getClientSessionId\(\), now, true\)/,
  'Starting a new client session must settle exactly one previous effective session tail while under the progress lock.')

const heartbeatMethod = extractMethod(service, 'public DccTrainingTaskRespVO heartbeatViewSession', 'heartbeatViewSession')
assert.match(heartbeatMethod, /loadOwnedProgressForUpdate\(userId, progressId\)/,
  'heartbeatViewSession must lock the progress row before accumulating seconds.')
assertOrder(heartbeatMethod, 'loadOwnedProgressForUpdate(userId, progressId)', 'trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId', 'heartbeatViewSession')
assert.match(heartbeatMethod, /closeOtherActiveSessions\(progress, userId, reqVO\.getClientSessionId\(\), now, false\)/,
  'Heartbeat must close sibling active sessions without adding overlapping seconds to progress.')
assertOrder(heartbeatMethod, 'closeOtherActiveSessions(progress, userId, reqVO.getClientSessionId(), now, false)', 'updateProgressMetadata(progress, now, false, increment)', 'heartbeatViewSession')

const stopMethod = extractMethod(service, 'public DccTrainingTaskRespVO stopViewSession', 'stopViewSession')
assert.match(stopMethod, /loadOwnedProgressForUpdate\(userId, progressId\)/,
  'stopViewSession must lock the progress row before ending a session.')
assert.match(stopMethod, /closeOtherActiveSessions\(progress, userId, reqVO\.getClientSessionId\(\), now, false\)/,
  'Stopping one session must close duplicate sibling sessions without counting the overlap again.')

const ackMethod = extractMethod(service, 'public void acknowledgeTraining', 'acknowledgeTraining')
assert.match(ackMethod, /loadOwnedProgressForUpdate\(userId, progressId\)/,
  'acknowledgeTraining must read the serialized accumulated seconds from the locked progress row.')

const closeOtherMethod = extractMethod(service, 'private void closeOtherActiveSessions', 'closeOtherActiveSessions')
assert.match(closeOtherMethod, /boolean\s+countTailSeconds/,
  'closeOtherActiveSessions must distinguish tail settlement from duplicate-session cleanup.')
assert.match(closeOtherMethod, /trainingViewSessionMapper\.selectActiveListByProgressId\(progress\.getId\(\)\)/,
  'Duplicate session cleanup must operate inside the locked progress scope.')
assert.match(closeOtherMethod, /if\s*\(countedTail \|\| increment <= 0\)[\s\S]*?continue;/,
  'Only one previous effective session tail may be counted when replacing sessions.')
assert.match(closeOtherMethod, /updateProgressMetadata\(progress, now, false, increment\)/,
  'Only intentional session replacement should add closed-session tail seconds to progress.')
assert.match(closeOtherMethod, /if\s*\(!countTailSeconds\)[\s\S]*?continue;/,
  'Duplicate sibling cleanup must end the session without adding overlap seconds to progress.')

console.log('dcc static 024 training session concurrency contract passed')
