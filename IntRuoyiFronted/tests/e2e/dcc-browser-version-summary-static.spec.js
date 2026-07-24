const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const browserPresentation = readSource('src/views/dcc/controlled-file/browser/presentation.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-version-summary:static'],
  'node tests/e2e/dcc-browser-version-summary-static.spec.js',
  'package.json must expose the DCC browser version summary static contract'
)

assert.match(browserPage, /label="版本摘要"/, 'browser list must show a version summary column')
assert.match(
  browserPage,
  /data-testid="dcc-browser-version-summary"/,
  'browser version summary cells must have a stable test id'
)
assert.match(
  browserPage,
  /getBrowserVersionSummary\(/,
  'browser list must render the version summary from a presentation helper'
)
assert.match(
  browserPresentation,
  /export const getBrowserVersionSummary/,
  'browser presentation must export the version summary helper'
)

const browserTableSource = extractBetween(browserPage, '<el-table', '</el-table>')
for (const label of ['版本', '状态', '版本属性', '生效日期', '发布时间']) {
  assert.doesNotMatch(
    browserTableSource,
    new RegExp(`label="${label}"`),
    `browser list must replace the standalone ${label} column`
  )
}

const summaryTemplate = extractBetween(
  browserPage,
  '<el-table-column label="版本摘要"',
  '<el-table-column label="备注"'
)
assert.match(summaryTemplate, /<el-select/, 'version summary must keep the version selector')
assert.match(summaryTemplate, /v-model="row\.selectedVersionId"/, 'version selector must keep row selected version')
assert.match(summaryTemplate, /getVersionOptions\(row\)/, 'version selector must keep all version options')
assert.match(summaryTemplate, /handleVersionChange\(row\)/, 'version selector must keep selected version sync')
assert.match(
  browserPage,
  /const isValidBrowserOptionId = /,
  'browser filters every ElOption id through a shared validity guard'
)
assert.match(
  browserPage,
  /selectedVersionId: resolveInitialSelectedVersionId\(item\)/,
  'browser rows must initialize selected version from a validated option id'
)
assert.match(
  browserPage,
  /item\.active && isValidBrowserOptionId\(item\.id\)/,
  'category options must not render undefined or null ElOption values'
)
assert.match(
  browserPage,
  /filter\(\s*\(item\): item is ControlledFileBrowserVersion => isValidBrowserOptionId\(item\.id\)\s*\)/,
  'version options must not render undefined or null ElOption values'
)
assert.doesNotMatch(
  browserPage,
  /selectedVersionId: item\.id/,
  'browser rows must not copy an unchecked API id into the ElSelect model'
)
assert.match(summaryTemplate, /browser-version-summary__main/, 'version summary must have a main line')
assert.match(summaryTemplate, /browser-version-summary__tags/, 'version summary must show compact tags')
assert.match(summaryTemplate, /browser-version-summary__dates/, 'version summary must show key dates')
assert.match(summaryTemplate, /summary\.statusLabel/, 'version summary must show status text')
assert.match(summaryTemplate, /summary\.statusTagType/, 'version summary must expose status tag type')
assert.match(summaryTemplate, /summary\.versionKindText/, 'version summary must show latest or history text')
assert.match(summaryTemplate, /summary\.versionKindTagType/, 'version summary must expose version kind tag type')
assert.match(summaryTemplate, /summary\.effectiveText/, 'version summary must show effective date text')
assert.match(summaryTemplate, /summary\.publishedText/, 'version summary must show published time text')

const summaryHelper = extractBetween(
  browserPresentation,
  'interface BrowserVersionSummarySource',
  'export const getBrowserRowActionState'
)
const requiredSourceFields = [
  'versionNo',
  'status',
  'effectiveDate',
  'publishedTime',
  'modifying'
]
for (const field of requiredSourceFields) {
  assert.match(summaryHelper, new RegExp(field), `browser version summary helper must use ${field}`)
}
for (const helper of ['getBrowserStatusLabel', 'getBrowserStatusTagType']) {
  assert.match(summaryHelper, new RegExp(helper), `browser version summary helper must use ${helper}`)
}
assert.doesNotMatch(
  summaryHelper,
  /mock|placeholder|deadline|\bSLA\b|接口造数|fallback|降级|吞异常/i,
  'browser version summary must not invent mock data, deadlines, SLA fields, or fallback behavior'
)

console.log('PASS: DCC browser version summary static contract')
