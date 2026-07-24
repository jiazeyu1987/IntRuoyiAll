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

for (const apiToken of [
  'getMyDistributionTaskPage',
  'getMyTrainingTaskPage',
  'getEdhrWorkTaskMyPage',
  'ProWorkOrderApi.getWorkOrderPage',
  '/showroom/assignment/page'
]) {
  assert.match(
    badgeStore,
    new RegExp(apiToken.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `badge store must use the same real todo source as personal workbench: ${apiToken}`
  )
}

assert.match(
  badgeStore,
  /checkPermi\(\['mes:pro-edhr-work-task:query'\]\)[\s\S]*checkPermi\(\['mes:pro-edhr-batch-execution:query'\]\)/,
  'badge store must count eDHR tasks for dynamic batch-execution-only assignees'
)
assert.doesNotMatch(
  badgeStore,
  /getEdhrWorkTaskStats/,
  'badge store must not use stats endpoint because it has a narrower permission contract than the workbench list'
)
assert.match(
  badgeStore,
  /normalizePageTotal/,
  'badge store must validate PageResult.total before applying the count'
)
assert.match(
  badgeStore,
  /normalizeAssignmentPage/,
  'badge store must use the showroom assignment List contract instead of reading a missing PageResult.total'
)
assert.match(
  badgeStore,
  /SHOWROOM_ASSIGNMENT_TODO_BADGE_PAGE_SIZE[\s\S]*assignments\.length\s*===\s*SHOWROOM_ASSIGNMENT_TODO_BADGE_PAGE_SIZE/,
  'showroom assignment badge count must fail fast when the list endpoint reaches its page-size cap'
)
assert.match(badgeStore, /throw error/, 'badge store must rethrow load failures instead of swallowing them')
assert.doesNotMatch(badgeStore, /mock|fallback|降级|吞异常/i, 'badge store must not use mock or fallback counts')

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
  /profileWorkbenchTodoBadgeStore\.refreshTodoTotal\(\)/,
  'manual refresh inside personal workbench must refresh the global badge count'
)

console.log('PASS: profile workbench todo badge static contract')
