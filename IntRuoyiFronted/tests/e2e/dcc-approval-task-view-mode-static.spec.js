const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const approvalTaskPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')
const approvalCenterPage = readSource('src/views/approval-center/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:approval-task-view-mode:static'],
  'node tests/e2e/dcc-approval-task-view-mode-static.spec.js',
  'package.json must expose the DCC approval task view mode static contract'
)

for (const redirectToken of [
  "path: '/approval-center'",
  "moduleCode: 'DCC'",
  "viewType: 'TODO'"
]) {
  assert.match(approvalTaskPage, new RegExp(redirectToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `DCC approval task legacy route must redirect to unified approval center: ${redirectToken}`)
}

for (const centerToken of [
  'data-testid="approval-center-dcc-key-fields"',
  'data-testid="approval-center-dcc-business-context"',
  'openReviewAction(row)',
  'openModuleDetail(row)'
]) {
  assert.match(approvalCenterPage, new RegExp(centerToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `unified approval center must keep DCC task handling capability: ${centerToken}`)
}

assert.doesNotMatch(
  approvalTaskPage,
  /截止|超期|\bSLA\b|deadline|overdue|mock|placeholder|fallback|降级|吞异常/i,
  'approval task redirect must not invent deadline/SLA data or introduce mock/fallback behavior'
)

console.log('PASS: DCC approval task redirect static contract')
