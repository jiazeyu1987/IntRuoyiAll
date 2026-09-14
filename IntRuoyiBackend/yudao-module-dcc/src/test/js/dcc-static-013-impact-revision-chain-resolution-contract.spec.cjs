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
  'src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceImpl.java'
)
const mapper = read(
  'src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccPublicationImpactTaskMapper.java'
)
const serviceTest = read(
  'src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccRelatedFileImpactAssessmentServiceTest.java'
)

const resolver = methodBody(service, 'public void resolveLinkedRevisionAfterPublication')
assert.match(
  resolver,
  /String revisionCode = resolveRevisionCode\(publishedRevision\)/,
  'publication resolver must derive the major revision code from the published iteration'
)
assert.match(
  resolver,
  /taskMapper\.selectListByLinkedRevisionChain\(tenantId,\s*publishedRevision\.getMasterId\(\),\s*revisionCode\)/,
  'publication resolver must query linked impact tasks by master + revision code chain, not exact old iteration id'
)
assert.doesNotMatch(
  resolver,
  /selectListByLinkedRevisionId\(/,
  'publication resolver must not close only tasks linked to the exact old B/1 row'
)
assert.match(
  resolver,
  /taskMapper\.resolveRevision\(tenantId,\s*task\.getId\(\),\s*task\.getRowVersion\(\),\s*publishedRevision\.getId\(\),\s*publishedRevision\.getVersionNo\(\)\)/,
  'publication resolver must record the actually published B/2 row and version snapshot'
)

assert.match(
  mapper,
  /INNER JOIN dcc_controlled_file f[\s\S]*ON f\.id = t\.linked_revision_controlled_file_id/,
  'linked revision chain query must join the originally linked row to recover its major revision identity'
)
assert.match(
  mapper,
  /COALESCE\(f\.revision_code, SUBSTRING_INDEX\(f\.version_no, '\/', 1\)\) = #\{revisionCode\}/,
  'linked revision chain query must match the same major revision code'
)
assert.match(
  mapper,
  /linked_revision_version_snapshot = #\{revisionVersion\}/,
  'resolve update must persist the final published version snapshot'
)

for (const testName of [
  'resolveLinkedRevisionAfterPublication_resolvesLinkedRevisionChainToPublishedIteration',
  'resolveLinkedRevisionAfterPublication_concurrentResolverWinnerDoesNotThrowOrDuplicateAudit',
  'resolveLinkedRevisionAfterPublication_unexplainedCasMissStillFailsFast'
]) {
  assert.match(serviceTest, new RegExp(`void ${testName}\\(`), `${testName} regression is missing`)
}

console.log('DCC-STATIC-013 impact revision chain resolution contract PASS')
