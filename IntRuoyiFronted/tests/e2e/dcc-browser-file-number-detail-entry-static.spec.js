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

const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

const fileNumberColumn = extractBetween(
  browserPage,
  'v-if="isDccBrowserColumnVisible(\'fileNumber\')"',
  'v-if="isDccBrowserColumnVisible(\'directory\')"',
  'browser file number column'
)

assert.match(
  fileNumberColumn,
  /data-testid="dcc-browser-file-number-detail-link"/,
  'browser file number column must expose a stable detail-link test id'
)
assert.match(
  fileNumberColumn,
  /@click="openDetail\(getSelectedVersion\(row\)\.id\)"/,
  'browser file number link must open the currently selected version viewer'
)
assert.match(
  fileNumberColumn,
  /getSelectedVersion\(row\)\.fileNumber/,
  'browser file number column must render the current selected version file number'
)
assert.match(
  fileNumberColumn,
  /v-if="getSelectedVersion\(row\)\.fileNumber"/,
  'browser file number column must only show a link when a file number exists'
)
assert.match(
  fileNumberColumn,
  /<span v-else class="browser-file-number">-<\/span>/,
  'browser file number column must keep a plain placeholder when no number exists'
)

const actionColumn = extractBetween(
  browserPage,
  'v-if="isDccBrowserColumnVisible(\'operation\')"',
  '</el-table-column>',
  'browser action column'
)
assert.doesNotMatch(actionColumn, />\s*详情\s*</, 'browser action column must not show the detail action')
assert.doesNotMatch(actionColumn, /openDetail\(/, 'browser action column must not route to detail')
assert.match(actionColumn, />\s*预览\s*</, 'browser action column must keep the preview action')
assert.match(actionColumn, />\s*下载\s*</, 'browser action column must keep the download action')
assert.match(actionColumn, />\s*更多\s*</, 'browser action column must keep the more action')
assert.match(actionColumn, /hasBrowserRowActions\(row\)/, 'browser action column must detect empty row action state')
assert.match(actionColumn, /暂无可用操作/, 'browser action column must render an explicit empty state')

assert.match(
  browserPage,
  /openControlledFileViewer\(router,\s*route,\s*id,\s*'browser'\)/,
  'browser openDetail must route to the shared controlled file viewer helper'
)
assert.doesNotMatch(browserPage, /name:\s*'DccControlledFileDetail'/, 'browser file number link must not route to the normal detail page')
assert.match(
  browserPage,
  /const hasBrowserRowActions = \(row: ControlledFileBrowserRow\) =>/,
  'browser page must keep row action availability in a named helper'
)
assert.doesNotMatch(
  fileNumberColumn,
  /mock|placeholder data|fallback|降级|吞异常|默认成功/i,
  'browser file number detail link must not introduce mock, fallback, downgrade, swallowed errors, or default success'
)

console.log('PASS: DCC browser file number detail entry static contract')
