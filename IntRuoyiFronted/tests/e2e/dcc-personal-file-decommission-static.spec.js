const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const frontendRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(frontendRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const workflowApi = readSource('src/api/dcc/controlledFile/workflow.ts')
const remainingRouter = readSource('src/router/modules/remaining.ts')
const workbenchPage = readSource('src/views/dcc/controlled-file/workbench/index.vue')
const workbenchPresentation = readSource('src/views/dcc/controlled-file/workbench/presentation.ts')
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const externalReviewPage = readSource('src/views/dcc/controlled-file/external-review/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:personal-file-decommission:static'],
  'node tests/e2e/dcc-personal-file-decommission-static.spec.js',
  'package.json must expose the DCC personal-file decommission static contract'
)

assert.equal(
  fs.existsSync(path.join(frontendRoot, 'src/views/dcc/controlled-file/mine')),
  false,
  'personal-file frontend page directory must be removed'
)

const scannedSource = [
  workflowApi,
  remainingRouter,
  workbenchPage,
  workbenchPresentation,
  uploadPage,
  externalReviewPage
].join('\n')

for (const forbidden of [
  'controlled-file/mine',
  'DccControlledFileMine',
  '/dcc/controlled-files/page',
  'getControlledFilePage',
  '个人文件'
]) {
  assert.doesNotMatch(scannedSource, new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${forbidden} must be removed`)
}

assert.match(workflowApi, /getControlledFileBrowserPage/, 'controlled browsing API must remain available')
assert.match(workflowApi, /\/dcc\/controlled-files\/browser-page/, 'frontend must keep browser-page endpoint')
assert.match(remainingRouter, /DccControlledFileDetail/, 'controlled file detail route must remain available')
assert.match(remainingRouter, /activeMenu:\s*'\/dcc\/controlled-file\/browser'/, 'detail route must highlight controlled browsing')
assert.match(uploadPage, /DccControlledFileBrowser/, 'controlled-file submit completion must return to controlled browsing')
assert.match(externalReviewPage, /DccControlledFileBrowser/, 'external review submit completion must return to controlled browsing')

console.log('PASS: DCC personal-file decommission static contract')
