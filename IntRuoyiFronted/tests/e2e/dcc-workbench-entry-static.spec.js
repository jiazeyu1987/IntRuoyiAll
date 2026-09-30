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
const entryComponent = readSource('src/views/dcc/controlled-file/shared/ControlledFileWorkbenchEntry.vue')

const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const approvalTasksPage = readSource('src/views/dcc/controlled-file/approval-tasks/index.vue')
const trainingMinePage = readSource('src/views/dcc/controlled-file/training/mine/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:workbench-entry:static'],
  'node tests/e2e/dcc-workbench-entry-static.spec.js',
  'package.json must expose the DCC workbench entry static contract'
)

assert.match(
  entryComponent,
  /defineOptions\(\{\s*name:\s*'ControlledFileWorkbenchEntry'\s*\}\)/,
  'workbench entry component must expose a stable name'
)
assert.match(entryComponent, /DCC 工作台/, 'workbench entry must render the visible label')
assert.match(
  entryComponent,
  /DccControlledFileWorkbench/,
  'workbench entry must navigate to the real workbench route name'
)
assert.match(entryComponent, /router\.push/, 'workbench entry must use frontend routing')
assert.doesNotMatch(entryComponent, /mock|placeholder/i, 'workbench entry must not use mock or placeholder logic')

assert.doesNotMatch(
  browserPage,
  /ControlledFileWorkbenchEntry/,
  'controlled browser must not reintroduce the old header workbench entry'
)
assert.match(
  approvalTasksPage,
  /router\.replace\(\{[\s\S]*path:\s*'\/approval-center'[\s\S]*moduleCode:\s*'DCC'[\s\S]*viewType:\s*'TODO'/,
  'legacy DCC approval task page must redirect to unified approval center instead of rendering a duplicate entry'
)
assert.doesNotMatch(
  trainingMinePage,
  /ControlledFileWorkbenchEntry/,
  'training mine page must rely on the standard list toolbar and not render the removed workbench entry'
)

console.log('PASS: DCC workbench entry static contract')
