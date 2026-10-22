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

const service = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceImpl.java'
)
const serviceTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicationFollowupServiceTest.java'
)
const policy = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicy.java'
)
const policyProperties = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicyProperties.java'
)
const policyTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileVersionPolicyTest.java'
)

const recorder = methodBody(service, 'public void recordPublishedRevision')
assert.match(
  recorder,
  /if\s*\(!isMajorRevisionChange\(publishedFile,\s*previousActiveFile\)\)\s*\{\s*return;\s*\}/s,
  'publication follow-up must return before creating a batch when the revision is not major'
)
assert.ok(
  recorder.indexOf('isMajorRevisionChange(publishedFile, previousActiveFile)') <
    recorder.indexOf('batchMapper.insertOrKeepExisting'),
  'major-version gate must execute before follow-up batch creation'
)
assert.match(
  service,
  /versionPolicy\.isMajorVersionChange\(publishedFile,\s*previousActiveFile\)/s,
  'major-version detection must delegate to the configurable version policy'
)
assert.match(
  policy,
  /if\s*\(previousActiveFile\s*==\s*null\)\s*\{\s*return false;\s*\}/s,
  'first publication without a previous active version must not be treated as a version change'
)
assert.match(
  policyProperties,
  /private\s+Integer\s+majorIdentitySegmentCount\s*=\s*1/s,
  'version policy must default to first-segment major identity for current A/1, A/2 behavior'
)
assert.match(
  policy,
  /majorIdentity\(\)\.equals\(previousVersion\.majorIdentity\(\)\)/s,
  'major-version detection must compare configured major identities and ignore minor-only changes'
)
assert.match(
  serviceTest,
  /recordPublishedRevision_minorIterationDoesNotCreateFollowupBatch/,
  'unit regression for minor iterations is missing'
)
assert.match(
  serviceTest,
  /recordPublishedRevision_configuredTwoSegmentMajorIdentityTreatsThirdSegmentAsMinor/,
  'unit regression for configured two-segment major identity is missing'
)
assert.match(
  serviceTest,
  /recordPublishedRevision_firstPublicationDoesNotCreateFollowupBatch/,
  'unit regression for first publication is missing'
)
assert.match(
  policyTest,
  /configurablePolicyTreatsFirstTwoSlashSegmentsAsMajorIdentity/,
  'policy regression for A/1/1 -> A/1/2 minor changes is missing'
)

console.log('DCC-STATIC-028 major-version notification gate contract PASS')
