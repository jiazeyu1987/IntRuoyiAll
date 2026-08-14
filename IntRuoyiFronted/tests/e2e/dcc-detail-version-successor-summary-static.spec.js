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
const mainVersionSection = extractBetween(
  detailPage,
  '<ContentWrap v-if="isVersionHistoryVisibleToReader',
  '<div class="mb-12px flex items-center justify-between gap-12px">',
  'main version history section'
)
const versionDialogSection = extractBetween(
  detailPage,
  'data-testid="dcc-controlled-preview-version-dialog"',
  'data-testid="dcc-controlled-print-dialog"',
  'version preview dialog'
)

assert.equal(
  packageJson.scripts['e2e:dcc:detail-version-successor-summary:static'],
  'node tests/e2e/dcc-detail-version-successor-summary-static.spec.js',
  'package.json must expose the DCC detail version successor summary static contract'
)

for (const section of [mainVersionSection, versionDialogSection]) {
  assert.match(section, /label="后继版本"/, 'version history must keep successor version column')
  assert.doesNotMatch(
    section,
    /label="后继版本"[^>\n]*prop="supersededByFileId"/,
    'successor version column must not render raw supersededByFileId'
  )
  assert.match(
    section,
    /data-testid="dcc-detail-version-successor-summary"/,
    'successor version cell must expose a stable test id'
  )
  assert.match(
    section,
    /getSuccessorVersionSummary\(row\)/,
    'successor version column must use readable summary helper'
  )
}

assert.match(
  detailPage,
  /const versionHistoryById = computed/,
  'detail page must index version history by id'
)
assert.match(
  detailPage,
  /const getVersionHistoryIdentityText/,
  'detail page must build a readable version identity'
)
assert.match(
  detailPage,
  /const getSuccessorVersionSummary/,
  'detail page must build successor summary from version history'
)
assert.match(
  detailPage,
  /后继记录缺失/,
  'missing successor references must be explicitly surfaced'
)
assert.match(detailPage, /无后继版本/, 'rows without successor must be readable')
assert.doesNotMatch(
  `${mainVersionSection}\n${versionDialogSection}`,
  /mock|placeholder|fallback|降级|吞异常/i,
  'version successor summary must not introduce mock, fallback, downgrade, or swallowed errors'
)

console.log('PASS: DCC detail version successor summary static contract')
