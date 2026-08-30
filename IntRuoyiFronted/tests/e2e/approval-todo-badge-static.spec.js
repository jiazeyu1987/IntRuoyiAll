const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const routes = readSource('src/router/modules/remaining.ts')
const approvalPage = readSource('src/views/approval-center/index.vue')
const badgeStore = readSource('src/store/modules/approvalTodoBadge.ts')
const menuTitle = readSource('src/layout/components/Menu/src/components/useRenderMenuTitle.tsx')
const menu = readSource('src/layout/components/Menu/src/Menu.vue')
const tabMenu = readSource('src/layout/components/TabMenu/src/TabMenu.vue')
const tagsView = readSource('src/layout/components/TagsView/src/TagsView.vue')
const routerTypes = readSource('types/router.d.ts')

const assertTwoDigitBadgeStyle = (source, label) => {
  const match = source.match(/\.approval-todo-badge\s*\{([\s\S]*?)\n\s*\}/)
  assert.ok(match, `${label} must define approval todo badge styles`)
  const body = match[1]
  assert.match(body, /min-width:\s*24px/, `${label} badge must be wide enough for two-digit counts`)
  assert.match(body, /max-width:\s*none/, `${label} badge must not cap two-digit badge width`)
  assert.match(body, /white-space:\s*nowrap/, `${label} badge must keep two-digit counts on one line`)
  assert.match(body, /overflow:\s*visible/, `${label} badge must not clip the second digit`)
  assert.match(body, /box-sizing:\s*border-box/, `${label} badge width must include padding predictably`)
}

assert.match(
  routerTypes,
  /approvalTodoBadge\?:\s*boolean/,
  'route meta must explicitly type approval todo badge support'
)

assert.match(
  routes,
  /path:\s*'\/approval-center'[\s\S]*?title:\s*'审批中心'[\s\S]*?approvalTodoBadge:\s*true/,
  'approval center top-level route must opt in to the todo count badge'
)

assert.match(
  routes,
  /path:\s*'todo'[\s\S]*?title:\s*'待办'[\s\S]*?approvalTodoBadge:\s*true/,
  'approval center todo child route must opt in to the todo count badge'
)

assert.match(
  menuTitle,
  /class="approval-menu-title"/,
  'left menu title renderer must keep the todo badge in the same inline group as the title'
)
assert.doesNotMatch(
  menuTitle,
  /v-menu__title[^"]*\bflex-1\b/,
  'left menu title text must not use flex-1 because it pushes the todo badge to the far right'
)
assert.match(
  menuTitle,
  /approval-menu-title[\s\S]*v-menu__title[\s\S]*renderApprovalTodoBadge\(meta\)/,
  'left menu title renderer must render the todo badge immediately after the title text'
)

assert.match(
  badgeStore,
  /const\s+APPROVAL_TODO_BADGE_PAGE_SIZE\s*=\s*10/,
  'badge store must share the approval center list page size so provider aggregation uses the same count source'
)
assert.match(
  badgeStore,
  /getApprovalTaskPage\(\{\s*pageNo:\s*1,\s*pageSize:\s*APPROVAL_TODO_BADGE_PAGE_SIZE,\s*viewType:\s*'TODO'\s*\}\)/,
  'badge store must load the unfiltered TODO total with the same page size as the approval-center TODO list'
)
assert.doesNotMatch(
  badgeStore,
  /pageSize:\s*1/,
  'badge store must not use pageSize 1 because it can under-count aggregated provider totals'
)
assert.match(badgeStore, /throw error/, 'badge store must rethrow load failures instead of swallowing them')
assert.doesNotMatch(badgeStore, /mock|fallback|降级|吞异常/i, 'badge store must not use mock or fallback counts')
assert.match(
  badgeStore,
  /getHasVisibleTodoBadge:\s*\(state\)\s*=>\s*state\.loaded\s*&&\s*state\.todoTotal\s*>\s*0/,
  'badge store must expose a positive-count visibility getter so zero does not render'
)

for (const [source, label] of [
  [menuTitle, 'left menu title renderer'],
  [tabMenu, 'approval center top-level tab menu'],
  [tagsView, 'top tags view']
]) {
  assert.match(source, /useApprovalTodoBadgeStore/, `${label} must consume the approval todo badge store`)
  assert.match(source, /approvalTodoBadge/, `${label} must check route meta approvalTodoBadge`)
  assert.match(source, /approval-todo-badge/, `${label} must render the compact todo count badge`)
}

assert.match(
  tagsView,
  /const\s+APPROVAL_TODO_BADGE_TAG_PATH\s*=\s*'\/approval-center\/todo'/,
  'top tags view must scope the todo badge to the canonical TODO route path'
)
assert.match(
  tagsView,
  /const\s+isApprovalTodoBadgeTagsViewItem\s*=\s*\(item:\s*RouteLocationNormalizedLoaded\)\s*=>[\s\S]*item\.path\s*===\s*APPROVAL_TODO_BADGE_TAG_PATH/,
  'top tags view must use an exact route guard instead of inherited parent approvalTodoBadge meta'
)
assert.match(
  tagsView,
  /isApprovalTodoBadgeTagsViewItem\(item\)[\s\S]*item\?\.meta\?\.approvalTodoBadge[\s\S]*approvalTodoBadgeStore\.getHasVisibleTodoBadge/,
  'top tags view must require the exact TODO route and a positive count before rendering the todo badge'
)
assert.doesNotMatch(
  tagsView,
  /const\s+shouldShowApprovalTodoBadge\s*=\s*\(item:\s*RouteLocationNormalizedLoaded\)\s*=>\s*\n\s*Boolean\(item\?\.meta\?\.approvalTodoBadge\s*&&\s*approvalTodoBadgeStore\.getHasVisibleTodoBadge\)/,
  'top tags view must not render badges from parent-merged approvalTodoBadge meta on manager pages'
)

for (const [source, label] of [
  [menu, 'left menu badge style'],
  [tabMenu, 'approval center top-level tab menu'],
  [tagsView, 'top tags view']
]) {
  assertTwoDigitBadgeStyle(source, label)
}

assert.match(
  approvalPage,
  /approvalTodoBadgeStore\.applyTodoTotal\(data\.total \|\| 0\)/,
  'approval center TODO list must sync the badge count from the current unfiltered TODO page response'
)
assert.match(
  approvalPage,
  /approvalTodoBadgeStore\.refreshTodoTotal\(\)/,
  'approval center review completion must refresh the global todo badge count'
)

console.log('PASS: approval todo badge static contract')
