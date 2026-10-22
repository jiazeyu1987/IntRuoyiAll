const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

const read = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routes = read('src/router/modules/remaining.ts')
const routerTypes = read('types/router.d.ts')
const badgeStore = read('src/store/modules/profileWorkbenchTodoBadge.ts')
const menuTitle = read('src/layout/components/Menu/src/components/useRenderMenuTitle.tsx')
const menu = read('src/layout/components/Menu/src/Menu.vue')
const tabMenu = read('src/layout/components/TabMenu/src/TabMenu.vue')
const tagsView = read('src/layout/components/TagsView/src/TagsView.vue')
const profileIndex = read('src/views/Profile/Index.vue')
const profileWorkbench = read('src/views/Profile/components/ProfileWorkbench.vue')

assert.match(
  routerTypes,
  /personalWorkbenchTodoBadge\?:\s*boolean/,
  'route meta must explicitly type personal workbench todo badge support'
)

assert.match(
  routes,
  /path:\s*'\/user'[\s\S]*?title:\s*t\('common\.profile'\)[\s\S]*?personalWorkbenchTodoBadge:\s*true/,
  'personal center top-level route must opt in to the workbench todo count badge'
)

assert.match(
  routes,
  /path:\s*'profile'[\s\S]*?title:\s*t\('common\.profile'\)[\s\S]*?personalWorkbenchTodoBadge:\s*true/,
  'profile child route must opt in so top tags view can show the workbench todo count'
)

assert.match(badgeStore, /getProfileWorkbenchTodoCount\(context\)/)
assert.doesNotMatch(badgeStore, /getMyDistributionTaskPage|getMyTrainingTaskPage|getEdhrWorkTaskMyPage|normalizeAssignmentPage|assignments\.length/)

assert.match(
  badgeStore,
  /authorizedWorkbenchSources\(\)\.map\(source => source\.sourceId\)/,
  'badge store must consume the same authoritative source directory as the workbench'
)
assert.doesNotMatch(
  badgeStore,
  /getEdhrWorkTaskStats/,
  'badge store must not use stats endpoint because it has a narrower permission contract than the workbench list'
)
assert.match(badgeStore, /requireProfileWorkbenchTodoTotal\(total\)/)
assert.match(badgeStore, /epoch: \+\+this\.badgeUpdateEpoch/)
assert.match(badgeStore, /tryCommit\(token: ProfileWorkbenchBadgeToken, patch: BadgePatch\)[\s\S]*if \(!this\.isCurrentToken\(token\)\) return false/)
assert.match(badgeStore, /isWorkbenchReadCurrent\(token\.lease\)/, 'shared lifetime must guard each badge read')

assert.match(badgeStore, /throw error/, 'badge store must rethrow load failures instead of swallowing them')
assert.doesNotMatch(badgeStore, /mock|fallback|降级|吞异常/i, 'badge store must not use mock or fallback counts')
assert.match(
  badgeStore,
  /getHasVisibleTodoBadge:\s*state\s*=>\s*state\.loaded\s*&&\s*state\.todoTotal !== undefined\s*&&\s*state\.todoTotal > 0/,
  'badge store must expose a positive-count visibility getter so zero does not render'
)

for (const [source, label] of [
  [menuTitle, 'left menu title renderer'],
  [menu, 'left menu'],
  [tabMenu, 'tab menu'],
  [tagsView, 'top tags view'],
  [profileIndex, 'profile page tab']
]) {
  assert.match(
    source,
    /profileWorkbenchTodoBadge|ProfileWorkbenchTodoBadge|personalWorkbenchTodoBadge/,
    `${label} must consume or render the personal workbench todo badge`
  )
  assert.match(
    source,
    /personal-workbench-todo-badge/,
    `${label} must render the compact personal workbench todo badge class`
  )
}

assert.match(
  tagsView,
  /const\s+PERSONAL_WORKBENCH_TODO_BADGE_TAG_PATH\s*=\s*'\/user\/profile'/,
  'top tags view must scope the personal workbench badge to the canonical profile route path'
)

assert.match(
  profileWorkbench,
  /profileWorkbenchTodoBadgeStore\.applyTodoTotal\(badge, page\.businessTotal\)/,
  'page refresh must supply businessTotal from its exact page response'
)

console.log('PASS: profile workbench todo badge static contract')
