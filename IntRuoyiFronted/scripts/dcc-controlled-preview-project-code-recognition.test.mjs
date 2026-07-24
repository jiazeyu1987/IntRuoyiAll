import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const detailSource = readText('src/views/dcc/controlled-file/detail/index.vue')
const previewPanelSource =
  detailSource.match(/<ControlledFileBasicInfoPanel[\s\S]*?\/>/)?.[0] || ''

test('BDD: preview mode exposes the shared project-code recognition entry', () => {
  assert.ok(previewPanelSource, 'preview mode must render the shared basic info panel')
  assert.match(previewPanelSource, /:show-product-recognition="canEditMetadata && !!fileDetail"/)
  assert.match(previewPanelSource, /:project-code-recognition-loading="projectCodeRecognitionLoading"/)
  assert.match(previewPanelSource, /@recognize-project-code="handleRecognizeProjectCode"/)
  assert.match(previewPanelSource, /@open-dcc-project-code="openDccProjectCode"/)
})
