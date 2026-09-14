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

function extractBetween(source, startNeedle, endNeedle, label) {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker`)
  return source.slice(start, end)
}

const queryService = read('src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java')
const queryServiceTest = read('src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java')

const checkout = methodBody(queryService, 'private DccControlledFileRespVO doCheckoutControlledFile')
const activeCheckoutBranch = extractBetween(
  checkout,
  'if (active != null) {',
  'String baseSourceSha256',
  'active checkout branch'
)

assert.match(
  activeCheckoutBranch,
  /Objects\.equals\(active\.getActorId\(\), userId\)[\s\S]*&& Objects\.equals\(active\.getBaseIterationId\(\), id\)[\s\S]*return toBrowserRespVO\(userId, file\)/,
  'same-actor checkout replay must return success only for the same base iteration'
)
assert.match(
  activeCheckoutBranch,
  /throw exception\(CONTROLLED_FILE_ALREADY_CHECKED_OUT, active\.getActorId\(\)\)/,
  'same actor with a different base iteration must receive the real active-lock conflict'
)
assert.match(
  queryServiceTest,
  /void checkoutSameActorDifferentBaseIterationDoesNotReportRequestedVersionCheckedOut\(\)/,
  'checkout regression for same actor / different base iteration is missing'
)
assert.match(
  queryServiceTest,
  /verify\(controlledFileMapper, never\(\)\)\.checkoutByIdAndTenantWhenAvailable/,
  'regression must prove the requested A/2 row is not marked checked out when A/1 owns the active lock'
)

console.log('DCC-STATIC-015 checkout base iteration replay contract PASS')
