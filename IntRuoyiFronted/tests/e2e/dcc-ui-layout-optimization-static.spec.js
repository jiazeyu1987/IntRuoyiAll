const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const approvalPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')
const approvalShared = readSource('src/views/dcc/controlled-file/shared/approval.ts')
const detailPage = readSource('src/views/dcc/controlled-file/detail/index.vue')
const packageJson = JSON.parse(readSource('package.json'))

const browserFileNumberColumn = extractBetween(
  browserPage,
  '<el-table-column label="文件编号"',
  '<el-table-column label="产品名称"'
)

assert.equal(
  packageJson.scripts['e2e:dcc:ui-layout:static'],
  'node tests/e2e/dcc-ui-layout-optimization-static.spec.js',
  'package.json must expose a dedicated IntDCC UI layout static check'
)

assert.match(browserPage, /label="所在目录"/, 'browser list must show the containing directory path')
assert.match(browserPage, /getBrowserDirectoryPath/, 'browser list must compute directory path from the real tree')
assert.doesNotMatch(
  browserFileNumberColumn,
  /@click="openPreview/,
  'browser file number column must not duplicate the title preview entry'
)
assert.match(
  browserPage,
  /handleBrowserRowCommand/,
  'browser row low-frequency actions must be routed through a compact more menu'
)
assert.doesNotMatch(
  browserPage,
  /<el-table-column label="操作" align="center" fixed="right" width="300"/,
  'browser operation column must be narrower after low-frequency actions move into a menu'
)

assert.match(approvalPage, /label="处理提示"/, 'approval task list must show handling hints')
assert.match(approvalPage, /handlingHint/, 'approval task rows must render handling hints')
assert.match(
  approvalShared,
  /buildDccTaskHandlingHint/,
  'approval shared presentation must derive handling hints from existing BPM and DCC state'
)

assert.match(detailPage, /detail-action-bar/, 'detail page must use a grouped action bar')
assert.match(detailPage, /detail-action-group--primary/, 'detail page must expose primary actions separately')
assert.match(detailPage, /detail-action-group--more/, 'detail page must expose low-frequency actions separately')
assert.match(detailPage, /detail-action-group--danger/, 'detail page must expose risky actions separately')
assert.match(detailPage, /handleDetailMoreCommand/, 'detail page more menu must route low-frequency actions')
assert.match(detailPage, /handleDetailDangerCommand/, 'detail page danger menu must route risky actions')

console.log('PASS: IntDCC UI layout optimization static contract')
