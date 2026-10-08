const fs = require('fs')
const path = require('path')
const assert = require('assert')

const repoRoot = path.resolve(__dirname, '..', '..')
const profileWorkbenchPath = path.join(
  repoRoot,
  'src',
  'views',
  'Profile',
  'components',
  'ProfileWorkbench.vue'
)
const visibilityApiPath = path.join(
  repoRoot,
  'src',
  'api',
  'system',
  'profileWorkbenchTaskVisibility',
  'index.ts'
)

const profileWorkbench = fs.readFileSync(profileWorkbenchPath, 'utf8')
const visibilityApi = fs.existsSync(visibilityApiPath)
  ? fs.readFileSync(visibilityApiPath, 'utf8')
  : ''

assert(
  visibilityApi.includes('/system/profile-workbench-task-visibility/hidden-keys'),
  'profile workbench must load persisted hidden task keys from system API'
)
assert(
  visibilityApi.includes('/system/profile-workbench-task-visibility/hide'),
  'profile workbench must persist hide actions through system API'
)
assert(
  visibilityApi.includes('/system/profile-workbench-task-visibility/restore'),
  'profile workbench must persist restore actions through system API'
)
assert(
  profileWorkbench.includes('hideProfileWorkbenchTask') &&
    profileWorkbench.includes('restoreProfileWorkbenchTask'),
  'ProfileWorkbench must retain formal hide and restore APIs'
)
assert(
  profileWorkbench.includes('activeVisibilityTab') &&
    profileWorkbench.includes('hiddenTotal') &&
    profileWorkbench.includes('visibility: activeVisibilityTab.value'),
  'server owns complete visible and hidden filtering'
)
assert(
  profileWorkbench.includes('handleHideTodo') && profileWorkbench.includes('handleRestoreTodo'),
  'ProfileWorkbench must expose hide and restore handlers'
)
assert(
  profileWorkbench.includes('已隐藏') && profileWorkbench.includes('恢复'),
  'ProfileWorkbench must provide a hidden-task view and restore action'
)
assert(
  profileWorkbench.includes('profileWorkbenchTodoBadgeStore.refreshTodoTotal()'),
  'ProfileWorkbench must refresh the badge after hide/restore changes'
)


assert(!profileWorkbench.includes('hiddenTaskKeys'), 'no local hidden-key intersection')
assert.match(profileWorkbench, /actionTaskKey\.value = row\.taskKey[\s\S]*beginBadgeUpdate\(\)[\s\S]*invalidatePage\(\)[\s\S]*await restoreProfileWorkbenchTask/)
assert.match(profileWorkbench, /writeSucceeded = true[\s\S]*await loadWorkbench\(\)/)
assert.match(profileWorkbench, /任务已恢复，但列表刷新失败|任务已隐藏，但列表刷新失败/)
