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
const workbenchPage = readSource('src/views/dcc/controlled-file/workbench/index.vue')
const workbenchPresentation = readSource('src/views/dcc/controlled-file/workbench/presentation.ts')
const pendingDistributionSection = workbenchPage.slice(
  workbenchPage.indexOf('待文控下发'),
  workbenchPage.indexOf('待培训确认')
)
const trainingSection = workbenchPage.slice(workbenchPage.indexOf('待培训确认'))

assert.equal(
  packageJson.scripts['e2e:dcc:workbench-file-context:static'],
  'node tests/e2e/dcc-workbench-file-context-static.spec.js',
  'package.json must expose the DCC workbench file context static contract'
)

assert.match(
  workbenchPage,
  /data-testid="dcc-workbench-approval-file-context"/,
  'approval todo rows must expose stable file context markup'
)
assert.match(
  workbenchPage,
  /row\.controlledFile\?\.fileNumber/,
  'approval todo rows must show the controlled file number from real loaded file data'
)
assert.match(
  workbenchPage,
  /row\.controlledFile\?\.versionNo/,
  'approval todo rows must show the controlled file version from real loaded file data'
)
assert.match(
  workbenchPage,
  /data-testid="dcc-workbench-training-file-detail-link"/,
  'training todo rows must expose a stable controlled-file viewer link'
)
assert.match(
  trainingSection,
  /data-testid="dcc-workbench-training-file-context"/,
  'training todo rows must expose stable file context markup'
)
assert.match(trainingSection, /row\.fileNumber/, 'training todo rows must show the file number')
assert.match(trainingSection, /row\.versionNo/, 'training todo rows must show the file version')
assert.match(
  workbenchPage,
  /@click="openFileDetail\(row\.controlledFileId\)"[\s\S]*文件详情/,
  'training todo rows must keep the file entry action'
)
assert.match(
  workbenchPage,
  /const openFileDetail = \(id: number \| string\) => \{[\s\S]*openControlledFileViewer\(router,\s*route,\s*id,\s*'workbench'\)/,
  'workbench file links must route through the shared controlled file viewer helper'
)
assert.match(
  workbenchPage,
  /const openApproval = \(row: DccWorkbenchTaskRow\) => \{[\s\S]*openControlledFileViewer\(router,\s*route,\s*row\.controlledFile\.id,\s*'workbench-approval'\)/,
  'workbench approval entry must route through the shared controlled file viewer helper'
)
assert.doesNotMatch(
  workbenchPage,
  /const openFileDetail = \(id: number \| string\) => \{[\s\S]*name:\s*'DccControlledFileDetail'/,
  'workbench file links must not route to the normal detail page'
)
assert.doesNotMatch(
  workbenchPage,
  /const openApproval = \(row: DccWorkbenchTaskRow\) => \{[\s\S]*name:\s*'DccControlledFileDetail'/,
  'workbench approval links must not route to the normal detail page'
)
assert.match(
  workbenchPage,
  /@click="openTrainingTask\(row\.progressId\)"[\s\S]*进入/,
  'training todo rows must keep the original training task entry'
)
assert.match(
  workbenchPresentation,
  /controlledFileId: item\.controlledFileId/,
  'training presentation must retain controlledFileId from the real training task API'
)
assert.doesNotMatch(
  pendingDistributionSection,
  /dcc-workbench-file-context/,
  'pending distribution must not duplicate file number because it already has a dedicated number column'
)
assert.doesNotMatch(workbenchPage, /mock|placeholder/i, 'workbench file context must not use mock or placeholder logic')

console.log('PASS: DCC workbench file context static contract')
