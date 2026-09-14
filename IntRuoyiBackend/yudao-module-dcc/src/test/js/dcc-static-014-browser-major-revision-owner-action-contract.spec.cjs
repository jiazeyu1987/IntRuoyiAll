const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(moduleRoot, '..', '..')
const readBackend = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readFrontend = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, 'IntRuoyiFronted', relativePath), 'utf8')

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

const queryService = readBackend(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java'
)
const queryServiceTest = readBackend(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java'
)
const workflowApi = readFrontend('src/api/dcc/controlledFile/workflow.ts')
const browserPage = readFrontend('src/views/dcc/controlled-file/browser/index.vue')
const lifecycle = readFrontend('src/views/dcc/controlled-file/shared/lifecycle.ts')

const projection = methodBody(queryService, 'private DccControlledFileActionProjectionRespVO buildActionProjection')
assert.match(
  projection,
  /if \(\(active \|\| superseded\) && canCreateMajorRevision\(userId, file\)\) \{[\s\S]*allowedActions\.add\(ACTION_MAJOR_REVISION\)/,
  'backend action projection must expose MAJOR_REVISION for active/superseded versions only after owner authorization'
)

const backendCanCreate = methodBody(queryService, 'private boolean canCreateMajorRevision')
assert.match(
  backendCanCreate,
  /projectAccessService\.hasProjectOwner\(userId, file\.getDccProjectCodeId\(\)\)/,
  'backend major-revision eligibility must use current formal project OWNER authorization'
)
assert.doesNotMatch(
  backendCanCreate,
  /getRequesterId\(\)/,
  'backend major-revision eligibility must not use requester identity as OWNER authority'
)

assert.match(workflowApi, /'MAJOR_REVISION'/, 'frontend action type list must include MAJOR_REVISION')
assert.match(
  lifecycle,
  /getDccControlledFileAllowedActions\(source\)\.includes\(action\)/,
  'shared lifecycle helper must resolve permissions from backend allowedActions'
)

const frontendCanCreate = extractBetween(
  browserPage,
  'const canCreateMajorRevision',
  'const parseWindchillVersion',
  'browser canCreateMajorRevision'
)
assert.match(
  frontendCanCreate,
  /isDccControlledFileActionAllowed\(file,\s*'MAJOR_REVISION'\)/,
  'browser page must display the button from backend MAJOR_REVISION projection'
)
assert.doesNotMatch(
  frontendCanCreate,
  /requesterId|WORKING|status/,
  'browser page must not guess major-revision eligibility from requester or WORKING status'
)

for (const testName of [
  'getControlledFile_projectOwnerDifferentFromRequesterCanCreateMajorRevisionFromActiveVersion',
  'getControlledFile_workingRequesterDoesNotReceiveMisleadingMajorRevisionAction'
]) {
  assert.match(queryServiceTest, new RegExp(`void ${testName}\\(`), `${testName} regression is missing`)
}

console.log('DCC-STATIC-014 browser major revision owner action contract PASS')
