const assert = require('assert/strict')
const childProcess = require('child_process')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..', '..')
const sourceMode = process.env.PQC_RELEASE_STATIC_SOURCE || 'WORKTREE'

function read(relativePath) {
  if (sourceMode === 'HEAD') {
    const gitPath = `IntRuoyiFronted/${relativePath.replace(/\\/g, '/')}`
    return childProcess.execFileSync('git', ['show', `HEAD:${gitPath}`], {
      cwd: root,
      encoding: 'utf8'
    })
  }
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function blockBetween(source, startText, endText) {
  const start = source.indexOf(startText)
  assert.notEqual(start, -1, `missing start anchor: ${startText}`)
  const end = source.indexOf(endText, start)
  assert.notEqual(end, -1, `missing end anchor: ${endText}`)
  return source.slice(start, end)
}

const api = read('src/api/mes/pro/productionRelease/index.ts')
const page = read('src/views/mes/pro/production-release/PqcProductionReleasePage.vue')

assert.match(api, /approvalReady\?:\s*boolean/, 'approvalReady must remain optional because the fast PQC page can omit dossier readiness')

const actionBlock = blockBetween(
  page,
  `v-hasPermi="['mes:pro-production-release:pqc-approve']"`,
  'data-pqc-production-release-nonconformance'
)
assert.match(
  actionBlock,
  /:disabled="row\.underReview \|\| row\.approvalReady === false"/,
  'pending PQC release rows may disable approval only for under-review rows or explicit approvalReady=false'
)
assert.doesNotMatch(
  actionBlock,
  /!row\.approvalReady/,
  'approvalReady=null/undefined must not be treated as a disabled approval button'
)

const dispositionBlock = blockBetween(page, 'label="处置信息"', 'label="操作"')
assert.match(
  dispositionBlock,
  /v-else-if="row\.approvalReady === false"/,
  'blocker copy should display only for explicit approvalReady=false'
)

const openDialogBlock = blockBetween(page, 'const openReleaseDialog', 'const resetReleaseDialog')
assert.doesNotMatch(
  openDialogBlock,
  /approvalReady/,
  'opening the release dialog must not re-block rows where approvalReady is omitted by the page API'
)

console.log(`pqc-production-release-approval-ready-null-static (${sourceMode}): PASS`)
