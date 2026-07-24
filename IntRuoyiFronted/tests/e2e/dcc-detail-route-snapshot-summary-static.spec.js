const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const routeSnapshotSection = extractBetween(
  detailPage,
  '<ContentWrap data-testid="dcc-detail-route-snapshot-section"',
  '<ContentWrap v-if="isVersionHistoryVisibleToReader',
  'detail route snapshot section'
)

assert.equal(
  packageJson.scripts['e2e:dcc:detail-route-snapshot-summary:static'],
  'node tests/e2e/dcc-detail-route-snapshot-summary-static.spec.js',
  'package.json must expose the DCC detail route snapshot summary static contract'
)

assert.match(
  routeSnapshotSection,
  /data-testid="dcc-detail-route-snapshot-section"/,
  'detail route snapshot section must expose a stable test id'
)

for (const label of ['阶段', '候选摘要', '审批要求', '解析审批人']) {
  assert.match(routeSnapshotSection, new RegExp(`label="${label}"`), `route snapshot table must show ${label}`)
}

for (const label of ['版本', '阶段号', '阶段编码', '候选来源', '候选对象', '审批方式', '通过比例']) {
  assert.doesNotMatch(
    routeSnapshotSection,
    new RegExp(`label="${label}"`),
    `route snapshot table must not keep raw split column ${label}`
  )
}

for (const rawProp of [
  'prop="routeVersionNo"',
  'prop="stageNo"',
  'prop="stageCode"',
  'prop="candidateSourceType"',
  'prop="candidateSourceId"',
  'prop="approveMethod"',
  'prop="approveRatio"'
]) {
  assert.doesNotMatch(
    routeSnapshotSection,
    new RegExp(rawProp),
    `route snapshot table must not render raw prop-only column ${rawProp}`
  )
}

assert.match(
  routeSnapshotSection,
  /routeSnapshotRows/,
  'route snapshot table must use mapped presentation rows'
)
assert.match(
  detailPage,
  /ROUTE_CANDIDATE_SOURCE_OPTIONS/,
  'detail page must use shared candidate source labels'
)
assert.match(
  detailPage,
  /getOptionLabel\(ROUTE_CANDIDATE_SOURCE_OPTIONS,\s*snapshot\.candidateSourceType\)/,
  'route snapshot rows must map candidate source to readable labels'
)
assert.match(
  detailPage,
  /getOptionLabel\(ROUTE_APPROVE_METHOD_OPTIONS,\s*snapshot\.approveMethod\)/,
  'route snapshot rows must map approve method to readable labels'
)
assert.match(
  detailPage,
  /getRouteSnapshotCandidateText/,
  'route snapshot rows must compute a readable candidate summary'
)
assert.match(
  detailPage,
  /resolveUserNames\(row\.resolvedUserIds\)/,
  'route snapshot table must keep resolved approver names visible'
)
assert.match(
  routeSnapshotSection,
  /data-testid="dcc-detail-route-snapshot-candidate"/,
  'candidate summary cell must expose a stable test id'
)
assert.match(
  routeSnapshotSection,
  /data-testid="dcc-detail-route-snapshot-requirement"/,
  'approval requirement cell must expose a stable test id'
)
assert.doesNotMatch(
  routeSnapshotSection,
  /mock|placeholder|fallback|降级|吞异常/i,
  'route snapshot summary must not introduce mock, fallback, downgrade, or swallowed errors'
)

console.log('PASS: DCC detail route snapshot summary static contract')
