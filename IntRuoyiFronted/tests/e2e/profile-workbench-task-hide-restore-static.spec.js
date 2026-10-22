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
const visibilityApi = fs.readFileSync(path.join(repoRoot, 'src/api/system/profileWorkbenchTodo/index.ts'), 'utf8')
assert(!fs.existsSync(visibilityApiPath), 'old hidden-keys/arbitrary-field API must be removed')
assert(visibilityApi.includes('/workbench/context') && visibilityApi.includes('/workbench/tasks/visibility'), 'context-bound write must be formal')
assert(!visibilityApi.includes('/system/profile-workbench-task-visibility/'), 'no legacy write URL')
assert(profileWorkbench.includes('setProfileWorkbenchTaskVisibility'), 'hide/restore use the same versioned command')
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
  profileWorkbench.includes('profileWorkbenchTodoBadgeStore.applyTodoTotal(badge, page.businessTotal)'),
  'ProfileWorkbench must refresh the badge after hide/restore changes'
)


assert(!profileWorkbench.includes('hiddenTaskKeys'), 'no local hidden-key intersection')
assert.match(profileWorkbench, /actionTaskKey\.value = row\.taskKey[\s\S]*beginBadgeUpdate\(\)[\s\S]*invalidatePage\(\)[\s\S]*await setProfileWorkbenchTaskVisibility/)
assert.match(profileWorkbench, /writeSucceeded = true[\s\S]*const refresh = loadWorkbench\(\)/)
assert.match(profileWorkbench, /任务已恢复，但列表刷新失败|任务已隐藏，但列表刷新失败/)
