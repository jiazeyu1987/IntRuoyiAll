const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')
const badgeStore = read('src/store/modules/profileWorkbenchTodoBadge.ts')

assert.match(
  profileWorkbench,
  /getMyDistributionTaskPage/,
  'personal workbench must load real DCC distribution acknowledgement tasks'
)
assert.match(
  profileWorkbench,
  /source:\s*'文控分发'/,
  'personal workbench must label distribution acknowledgement rows as 文控分发'
)
assert.match(
  profileWorkbench,
  /name:\s*'DccControlledFileDetail'[\s\S]*distributionId[\s\S]*recipientId/,
  'distribution acknowledgement rows must navigate to the controlled file detail with distribution and recipient anchors'
)
assert.match(
  profileWorkbench,
  /loadEnabledSource\('文控分发加载失败',\s*loadDccDistributionRows\)/,
  'DCC distribution loader failures must be surfaced with the distribution source label'
)
assert.match(
  profileWorkbench,
  /getMyTrainingTaskPage/,
  'personal workbench must keep loading DCC training tasks'
)

assert.match(
  badgeStore,
  /getMyDistributionTaskPage/,
  'personal workbench badge must count DCC distribution acknowledgement tasks'
)
assert.match(
  badgeStore,
  /loadDccDistributionTodoTotal/,
  'personal workbench badge must keep distribution count logic explicit and testable'
)
assert.match(
  badgeStore,
  /checkPermi\(\['dcc:controlled-file:query'\]\)/,
  'distribution acknowledgement task count must use the same DCC permission as the acknowledgement endpoint'
)

console.log('PASS: DCC distribution and training personal workbench static contract')
