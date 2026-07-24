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
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')
const stateCache = readSource('src/views/dcc/controlled-file/browser/state-cache.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:browser-cache-write-failure:static'],
  'node tests/e2e/dcc-browser-cache-write-failure-static.spec.js',
  'package.json must expose the DCC browser cache write failure static contract'
)

assert.match(
  stateCache,
  /export interface DccBrowserMetadataDirectoryNode/,
  'state cache must define a lightweight directory metadata cache node contract'
)
assert.match(
  stateCache,
  /directoryChildrenByParentKey\?: Record<string, DccBrowserMetadataDirectoryNode\[\]>/,
  'metadata cache must persist lightweight directory metadata nodes by parent key'
)
assert.doesNotMatch(
  stateCache,
  /directoryChildrenByParentKey\?: Record<string, ControlledFileDirectoryVO\[\]>/,
  'metadata cache must not persist full directory VO arrays with nested runtime shape'
)

const toDirectoryCacheNodeStart = browserPage.indexOf('const toDirectoryCacheNode =')
const toDirectoryCacheNodeEnd = browserPage.indexOf('const buildDirectoryChildrenCacheRecord =', toDirectoryCacheNodeStart)
assert.notEqual(toDirectoryCacheNodeStart, -1, 'browser page must define a dedicated directory cache serializer')
assert.notEqual(toDirectoryCacheNodeEnd, -1, 'browser page must define a directory cache record builder')
const toDirectoryCacheNodeBlock = browserPage.slice(toDirectoryCacheNodeStart, toDirectoryCacheNodeEnd)
assert.match(
  toDirectoryCacheNodeBlock,
  /DccBrowserMetadataDirectoryNode/,
  'directory cache serializer must emit the lightweight metadata node contract'
)
assert.doesNotMatch(
  toDirectoryCacheNodeBlock,
  /children\s*:/,
  'directory cache serializer must not persist nested children arrays'
)

const buildDirectoryChildrenCacheRecordStart = browserPage.indexOf(
  'const buildDirectoryChildrenCacheRecord ='
)
const buildDirectoryChildrenCacheRecordEnd = browserPage.indexOf(
  'const buildExpandedDirectoryIdsCacheRecord =',
  buildDirectoryChildrenCacheRecordStart
)
assert.notEqual(
  buildDirectoryChildrenCacheRecordStart,
  -1,
  'browser page must define a directory cache record builder'
)
assert.notEqual(
  buildDirectoryChildrenCacheRecordEnd,
  -1,
  'browser page must keep expanded directory cache builder after the directory cache record builder'
)
const buildDirectoryChildrenCacheRecordBlock = browserPage.slice(
  buildDirectoryChildrenCacheRecordStart,
  buildDirectoryChildrenCacheRecordEnd
)
assert.match(
  buildDirectoryChildrenCacheRecordBlock,
  /toDirectoryCacheNode/,
  'directory cache record builder must serialize nodes through the lightweight cache serializer'
)
assert.doesNotMatch(
  buildDirectoryChildrenCacheRecordBlock,
  /cache\[String\(key\)\]\s*=\s*value\s*(?:\r?\n|;)/,
  'directory cache record builder must not dump raw cached runtime node arrays into localStorage'
)

assert.match(
  browserPage,
  /const normalizeDirectoryChildrenCacheRecord = \([\s\S]*DccBrowserMetadataDirectoryNode\[\]/,
  'browser page must normalize the lightweight cached directory metadata record'
)
assert.match(
  browserPage,
  /const buildDirectoryTreeFromCacheRecord = \([\s\S]*children:/,
  'browser page must rebuild nested runtime directory nodes from the cached parent-child record'
)
assert.match(
  browserPage,
  /const restoreBrowserMetadataCache = \(\) => \{[\s\S]*normalizeDirectoryChildrenCacheRecord\([\s\S]*buildDirectoryTreeFromCacheRecord\([\s\S]*applyDirectoryTree\(rootDirectories\)/,
  'browser page must restore the runtime directory tree from the lightweight cached parent-child record'
)
assert.match(
  browserPage,
  /message\.error\('DCC 受控浏览本地缓存写入失败，请检查浏览器本地存储权限。'\)/,
  'browser page must keep surfacing real local storage write failures'
)

console.log('PASS: DCC browser cache write failure static contract')
