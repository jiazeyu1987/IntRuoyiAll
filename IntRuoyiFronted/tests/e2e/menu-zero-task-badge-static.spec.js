const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const approvalStore = read('src/store/modules/approvalTodoBadge.ts')
const profileStore = read('src/store/modules/profileWorkbenchTodoBadge.ts')
const menuTitle = read('src/layout/components/Menu/src/components/useRenderMenuTitle.tsx')
const tabMenu = read('src/layout/components/TabMenu/src/TabMenu.vue')
const tagsView = read('src/layout/components/TagsView/src/TagsView.vue')
const profileIndex = read('src/views/Profile/Index.vue')

const assertStoreHidesZero = (source, label) => {
  assert.match(
    source,
    /getHasVisibleTodoBadge:\s*\(state\)\s*=>\s*state\.loaded\s*&&\s*state\.todoTotal\s*>\s*0/,
    `${label} store must expose a positive-count visibility getter`
  )
  assert.match(
    source,
    /getTodoBadgeText:\s*\(state\)\s*=>\s*(?:\(\s*)?state\.loaded\s*&&\s*state\.todoTotal\s*>\s*0\s*\?\s*String\(state\.todoTotal\)\s*:\s*''/,
    `${label} store badge text must be empty when the count is zero`
  )
}

assertStoreHidesZero(approvalStore, 'approval todo')
assertStoreHidesZero(profileStore, 'profile workbench todo')

for (const [source, label, storeName] of [
  [menuTitle, 'left menu title renderer', 'approvalTodoBadgeStore'],
  [tabMenu, 'top tab menu', 'approvalTodoBadgeStore'],
  [tagsView, 'tags view', 'approvalTodoBadgeStore']
]) {
  assert.match(
    source,
    new RegExp(`${storeName}\\.getHasVisibleTodoBadge`),
    `${label} must hide approval badges when the count is zero`
  )
}

for (const [source, label, storeName] of [
  [menuTitle, 'left menu title renderer', 'profileWorkbenchTodoBadgeStore'],
  [tabMenu, 'top tab menu', 'profileWorkbenchTodoBadgeStore'],
  [tagsView, 'tags view', 'profileWorkbenchTodoBadgeStore'],
  [profileIndex, 'profile page workbench tab', 'profileWorkbenchTodoBadgeStore']
]) {
  assert.match(
    source,
    new RegExp(`${storeName}\\.getHasVisibleTodoBadge`),
    `${label} must hide personal workbench badges when the count is zero`
  )
}

assert.doesNotMatch(
  menuTitle,
  /approvalTodoBadgeStore\.loaded[\s\S]{0,120}<span class="approval-todo-badge"/,
  'left menu approval badge must not render from loaded-only state'
)
assert.doesNotMatch(
  menuTitle,
  /profileWorkbenchTodoBadgeStore\.loaded[\s\S]{0,160}<span[\s\S]*personal-workbench-todo-badge/,
  'left menu personal badge must not render from loaded-only state'
)

console.log('PASS: menu zero task badge static contract')
