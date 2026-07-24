import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('controlled browser requests latest-version-only rows from the backend', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /latestVersionOnly:\s*true/)
})

test('controlled browser version column renders a selector backed by version history', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /<el-select[\s\S]*v-model="row\.selectedVersionId"/)
  assert.match(source, /v-for="item in getVersionOptions\(row\)"/)
  assert.match(source, /@change="handleVersionChange\(row\)"/)
})

test('controlled browser row display and actions follow the currently selected version', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /getSelectedVersion\(row\)\.title/)
  assert.match(source, /getSelectedVersion\(row\)\.fileNumber/)
  assert.match(source, /getSelectedVersion\(row\)\.status/)
  assert.match(source, /openDetail\(getSelectedVersion\(row\)\.id\)/)
  assert.match(source, /openPreview\(getSelectedVersion\(row\)\.id\)/)
  assert.match(source, /openDownload\(getSelectedVersion\(row\)\.id\)/)
})
