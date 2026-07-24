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
const remainingRouter = readSource('src/router/modules/remaining.ts')
const workbenchPage = readSource('src/views/dcc/controlled-file/workbench/index.vue')
const workbenchPresentation = readSource('src/views/dcc/controlled-file/workbench/presentation.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:workbench:static'],
  'node tests/e2e/dcc-workbench-static.spec.js',
  'package.json must expose a dedicated DCC workbench static contract'
)

assert.match(remainingRouter, /controlled-file\/workbench/, 'DCC workbench route must be registered')
assert.match(
  remainingRouter,
  /DccControlledFileWorkbench/,
  'DCC workbench route must have a stable route name'
)
assert.match(
  remainingRouter,
  /views\/dcc\/controlled-file\/workbench\/index\.vue/,
  'DCC workbench route must lazy-load the workbench component'
)

assert.match(
  workbenchPage,
  /defineOptions\(\{\s*name:\s*'DccControlledFileWorkbench'\s*\}\)/,
  'workbench page must expose a stable component name'
)
assert.match(workbenchPage, /DCC 工作台/, 'workbench page must identify itself as the DCC workbench')
assert.match(workbenchPage, /我的审批待办/, 'workbench must show approval todo scope')
assert.match(workbenchPage, /待文控下发/, 'workbench must show document-control release scope')
assert.match(workbenchPage, /待培训确认/, 'workbench must show training confirmation scope')
assert.match(workbenchPage, /发布失败/, 'workbench must show finalization failure scope')
assert.match(workbenchPage, /getControlledFileBrowserPage/, 'workbench must reuse controlled browser page API')
assert.match(workbenchPage, /getTaskTodoPage/, 'workbench must reuse existing BPM todo API')
assert.match(workbenchPage, /getMyTrainingTaskPage/, 'workbench must reuse existing DCC training task API')
assert.match(workbenchPage, /buildDccTaskCenterRowView/, 'workbench must reuse existing approval presentation')
assert.match(
  workbenchPresentation,
  /getControlledFileHandlingSummary/,
  'workbench presentation must reuse shared controlled-file handling hints'
)
assert.match(workbenchPage, /data-testid="dcc-workbench-load-error"/, 'workbench must expose API load errors')
assert.match(workbenchPage, /resolveWorkbenchErrorMessage/, 'workbench must normalize and display API errors')
assert.match(workbenchPage, /router\.push/, 'workbench rows must navigate through real frontend routes')
assert.doesNotMatch(workbenchPage, /mock|placeholder/i, 'workbench must not use mock or placeholder data')

assert.match(
  workbenchPresentation,
  /export const DCC_WORKBENCH_STATUS_SECTIONS/,
  'presentation must own the workbench status section metadata'
)
assert.match(
  workbenchPresentation,
  /export const buildDccWorkbenchMetricItems/,
  'presentation must build workbench metrics from real loaded lists'
)
assert.match(
  workbenchPresentation,
  /export const resolveWorkbenchErrorMessage/,
  'presentation must expose error message resolution'
)
assert.match(
  workbenchPresentation,
  /export const toWorkbenchFileRow/,
  'presentation must convert DCC files into workbench file rows'
)

console.log('PASS: DCC workbench static contract')
