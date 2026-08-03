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
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:download-entry:static'],
  'node tests/e2e/dcc-download-entry-browser-only-static.spec.js',
  'package.json must expose the DCC browser-only download entry static contract'
)

const actionColumn = extractBetween(
  browserPage,
  "v-if=\"isDccBrowserColumnVisible('operation')\"",
  '</el-table-column>',
  'browser operation column'
)

assert.match(actionColumn, />\s*下载\s*</, 'browser operation column must keep the download action')
assert.match(
  actionColumn,
  /@click="openDownload\(getSelectedVersion\(row\)\.id\)"/,
  'browser operation column must keep download wired to the selected version'
)
assert.match(
  actionColumn,
  /downloadLoadingId === getSelectedVersion\(row\)\.id/,
  'browser operation column must keep row-scoped download loading state'
)

for (const forbidden of [
  '下载当前受控副本',
  '下载受控文件',
  '@click="openDownload"',
  'detailActionState.canDownload',
  'const downloadLoading = ref(false)',
  'const openDownload = async',
  'triggerControlledFileDownload(controlledFileId.value)'
]) {
  assert.doesNotMatch(detailPage, new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `detail page must not keep direct controlled-file download entry: ${forbidden}`)
}

assert.doesNotMatch(
  detailPage,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'download entry consolidation must not introduce mock, fallback, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC controlled-file download entry is browser-row only')
