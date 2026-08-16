const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const browserPagePath = path.join(
  repoRoot,
  'src/views/dcc/controlled-file/browser/index.vue'
)
const browserPage = fs.readFileSync(browserPagePath, 'utf8').replace(/\r\n/g, '\n')

const extractBetween = (source, startNeedle, endNeedle, label) => {
  const start = source.indexOf(startNeedle)
  assert.notEqual(start, -1, `${label} missing start marker: ${startNeedle}`)
  const end = source.indexOf(endNeedle, start + startNeedle.length)
  assert.notEqual(end, -1, `${label} missing end marker: ${endNeedle}`)
  return source.slice(start, end)
}

const actionColumn = extractBetween(
  browserPage,
  "v-if=\"isDccBrowserColumnVisible('operation')\"",
  '</el-table-column>',
  'DCC browser action column'
)
const actionStyles = extractBetween(
  browserPage,
  '.browser-row-actions {',
  '.browser-row-actions__empty {',
  'DCC browser action layout styles'
)

assert.match(
  browserPage,
  /const DCC_BROWSER_COLUMN_TABLE_KEY = 'dcc\.controlledFile\.browser\.compactActionsV2'/,
  'the new compact layout must use a new table key so saved 320px widths do not override it'
)
assert.match(
  actionColumn,
  /:width="getDccBrowserColumnWidthString\('operation', 107\)"/,
  'the action column must be about one third of the former 320px width'
)
assert.match(
  browserPage,
  /\{ key: 'operation', label: '操作', width: 107, hideable: false, business: false \}/,
  'the persisted column definition must use the same 107px default width'
)
assert.match(
  actionStyles,
  /display:\s*grid;/,
  'the action panel must use a stable grid instead of free flex wrapping'
)
assert.match(
  actionStyles,
  /grid-template-columns:\s*repeat\(2, minmax\(0, 1fr\)\);/,
  'four common actions must form two columns and therefore two rows'
)
assert.match(
  actionStyles,
  /gap:\s*4px;/,
  'the compact grid must keep a stable gap between action controls'
)
assert.doesNotMatch(actionStyles, /flex-wrap:\s*wrap;/, 'free wrapping must not control row count')

for (const [label, handler] of [
  ['预览', 'openPreview'],
  ['追溯', 'openDetail'],
  ['签核', 'openSignatureEvidence'],
  ['下载', 'openDownload']
]) {
  assert.match(actionColumn, new RegExp(`>\\s*${label}\\s*</el-button>`), `must keep ${label}`)
  assert.match(actionColumn, new RegExp(`@click=\"${handler}\\(`), `must keep ${label} handler`)
}

for (const token of ['openControlledPrintDialog', 'hasBrowserMoreActions', 'handleBrowserRowCommand']) {
  assert.match(actionColumn, new RegExp(token), `must preserve secondary action contract: ${token}`)
}

console.log('PASS: DCC browser operation panel two-row compact layout static contract')
