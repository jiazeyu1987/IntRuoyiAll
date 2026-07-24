import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const root = process.cwd()
const readText = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

test('controlled browser preloads directory structure instead of lazy child loading', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')
  const apiSource = readText('src/api/dcc/controlledFile/directories.ts')

  assert.match(apiSource, /getDirectoryTree/)
  assert.match(source, /getDirectoryTree/)
  assert.doesNotMatch(source, /getDirectoryChildren/)
  assert.doesNotMatch(source, /\blazy\b/)
  assert.doesNotMatch(source, /:load="loadDirectoryNode"/)
})

test('controlled browser rebuilds a directory children cache from the full tree', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /directoryChildrenCache/)
  assert.match(source, /const cacheDirectoryTreeStructure = \(nodes: ControlledFileDirectoryNode\[]\) => \{/)
  assert.match(source, /nextCache\.set\('root', nodes\)/)
  assert.match(source, /nextCache\.set\(node\.id, node\.children \|\| \[\]\)/)
  assert.match(source, /persistBrowserMetadataCache\(\)/)
})

test('controlled browser can expand selected directory ancestors from cached structure', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /const rememberDirectoryAncestorChain = \(directoryId\?: number\) => \{/)
  assert.match(source, /while \(currentDirectoryId\)/)
  assert.match(source, /rememberExpandedDirectoryId\(currentDirectoryId\)/)
  assert.match(source, /:default-expanded-keys="expandedDirectoryKeys"/)
})

test('controlled browser still waits for directory selection before loading file rows', () => {
  const source = readText('src/views/dcc/controlled-file/browser/index.vue')

  assert.match(source, /getControlledFileBrowserPage/)
  assert.match(source, /selectedDirectoryId\.value\s*=\s*undefined/)
  assert.doesNotMatch(source, /selectedDirectoryId\.value\s*=\s*rootDirectories\[0\]\.id/)
  assert.match(
    source,
    /const getList = async \(\) => \{[\s\S]*if \(isCurrentDirectorySearch\.value && !selectedDirectoryId\.value\)[\s\S]*return[\s\S]*getControlledFileBrowserPage/
  )
  assert.match(source, /tableEmptyText[\s\S]*请先选择目录/)
})
