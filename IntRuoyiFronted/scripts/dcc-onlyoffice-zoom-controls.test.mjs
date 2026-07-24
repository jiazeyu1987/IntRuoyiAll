import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..')
const source = readFileSync(
  path.join(repoRoot, 'src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue'),
  'utf8'
)

test('dcc OnlyOffice viewer exposes visible zoom toolbar controls', () => {
  assert.match(source, /onlyoffice-viewer-toolbar/)
  assert.match(source, /aria-label="缩小预览"/)
  assert.match(source, /aria-label="放大预览"/)
  assert.match(source, /aria-label="重置缩放"/)
  assert.match(source, /previewZoomLabel/)
})

test('dcc OnlyOffice viewer tracks bounded zoom state with explicit steps', () => {
  assert.match(source, /const DEFAULT_ZOOM_VALUE =/)
  assert.match(source, /const MIN_ZOOM_VALUE =/)
  assert.match(source, /const MAX_ZOOM_VALUE =/)
  assert.match(source, /const ZOOM_STEP_VALUE =/)
  assert.match(source, /const previewZoomValue = ref\(/)
  assert.match(source, /const applyZoom =/)
})

test('dcc OnlyOffice viewer binds wheel gestures to zoom changes', () => {
  assert.match(source, /const handleWheelZoom =/)
  assert.match(source, /event\.ctrlKey/)
  assert.match(source, /deltaY/)
  assert.match(source, /preventDefault\(\)/)
  assert.match(source, /@wheel(?:\.prevent)?="handleWheelZoom"/)
})
