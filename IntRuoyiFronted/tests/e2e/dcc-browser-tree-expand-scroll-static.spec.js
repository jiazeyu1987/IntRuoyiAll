const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const appView = readSource('src/layout/components/AppView.vue')
const routerIndex = readSource('src/router/index.ts')
const browserPage = readSource('src/views/dcc/controlled-file/browser/index.vue')

assert.match(
  appView,
  /const resolveRouteViewKey = \(route: \{ path: string \}\) => route\.path/,
  'AppView must keep the same page instance stable across query-only route updates'
)
assert.match(
  appView,
  /<component :is="Component" :key="resolveRouteViewKey\(route\)" \/>/,
  'AppView must key routed views by stable path rather than fullPath'
)
assert.doesNotMatch(
  appView,
  /<component :is="Component" :key="route\.fullPath" \/>/,
  'AppView must no longer remount the whole view on every query update'
)

assert.match(
  routerIndex,
  /scrollBehavior: \(to, from, savedPosition\) => \{[\s\S]*if \(savedPosition\) \{[\s\S]*return savedPosition[\s\S]*\}[\s\S]*if \(to\.path === from\.path\) \{[\s\S]*return false[\s\S]*\}/,
  'router scrollBehavior must preserve scroll position during same-path query synchronization'
)

assert.doesNotMatch(
  browserPage,
  /:default-expanded-keys="expandedDirectoryKeys"/,
  'browser tree must not use reactive default-expanded-keys that rebuild the tree on each expand-state update'
)
assert.match(
  browserPage,
  /const syncDirectoryTreeExpandedState = \(\) => \{[\s\S]*treeStoreNodesMap[\s\S]*expandedDirectoryIds\.value\.has\(directoryId\)[\s\S]*treeNode\.expand\(\)[\s\S]*treeNode\.collapse\(\)/,
  'browser tree must reconcile expanded nodes imperatively against the remembered expanded-id set'
)
assert.match(
  browserPage,
  /let browserRouteSyncing = false[\s\S]*watch\(\s*\(\) => route\.fullPath,[\s\S]*if \(!previousFullPath \|\| nextFullPath === previousFullPath \|\| browserRouteSyncing\) \{[\s\S]*return[\s\S]*\}/,
  'browser page must react to external query navigation without re-entering its own route synchronization loop'
)
assert.match(
  browserPage,
  /async function withBrowserRouteSyncGuard\(action: \(\) => Promise<unknown>\) \{[\s\S]*browserRouteSyncing = true[\s\S]*finally \{[\s\S]*browserRouteSyncing = false[\s\S]*\}/,
  'browser route synchronization must bracket its own query writes with an in-flight guard'
)
assert.match(
  browserPage,
  /if \(JSON\.stringify\(browserRouteQuery\) === JSON\.stringify\(buildBrowserRouteQueryFromRoute\(\)\)\) \{[\s\S]*return[\s\S]*\}[\s\S]*await withBrowserRouteSyncGuard\(\(\) =>[\s\S]*router\.replace\(/,
  'browser route synchronization must skip no-op query writes and mark in-flight updates before replace'
)

assert.match(
  browserPage,
  /<el-row :gutter="16" class="browser-page-layout">/,
  'browser page must own a fixed-height layout shell so the directory column cannot stretch the whole page'
)
assert.match(
  browserPage,
  /\.browser-page-layout \{[\s\S]*height: calc\(100vh - 120px\);[\s\S]*min-height: 520px;[\s\S]*overflow: hidden;[\s\S]*\}/,
  'browser page layout must isolate overflow instead of letting the left tree expand the document'
)
assert.match(
  browserPage,
  /\.browser-directory-wrap \{[\s\S]*height: 100%;[\s\S]*overflow: hidden;[\s\S]*:deep\(\.el-card__body\) \{[\s\S]*display: flex;[\s\S]*height: 100%;[\s\S]*min-height: 0;[\s\S]*flex-direction: column;[\s\S]*\}/,
  'directory card body must be a bounded flex column for independent left-side scrolling'
)
assert.match(
  browserPage,
  /\.browser-directory-scroll \{[\s\S]*min-height: 0;[\s\S]*overflow-y: auto;[\s\S]*overscroll-behavior: contain;[\s\S]*\}/,
  'directory tree and search results must scroll inside the left directory column only'
)
assert.doesNotMatch(
  browserPage,
  /\.browser-directory-wrap[\s\S]*min-height: 640px|\.browser-list-wrap[\s\S]*min-height: 640px/,
  'browser page must not rely on unbounded min-height cards that let a long tree drive page scrolling'
)

console.log('PASS: DCC browser tree expand and scroll static contract')
