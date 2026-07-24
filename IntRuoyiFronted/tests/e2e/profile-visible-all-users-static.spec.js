const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const remainingRouterSource = readSource('src/router/modules/remaining.ts')
const userInfoSource = readSource('src/layout/components/UserInfo/src/UserInfo.vue')
const profileApiSource = readSource('src/api/system/user/profile.ts')

const userRouteStart = remainingRouterSource.indexOf("path: '/user'")
const userRouteEnd = remainingRouterSource.indexOf("path: '/dict'", userRouteStart)

assert.notEqual(userRouteStart, -1, 'remaining.ts must keep a /user route shell for personal routes')
assert.notEqual(userRouteEnd, -1, 'remaining.ts must keep the /dict route after /user')

const userRouteBlock = remainingRouterSource.slice(userRouteStart, userRouteEnd)
const profileRouteStart = userRouteBlock.indexOf("path: 'profile'")
const profileRouteEnd = userRouteBlock.indexOf("path: 'notify-message'", profileRouteStart)

assert.notEqual(profileRouteStart, -1, 'the /user route shell must contain the profile child route')
assert.notEqual(profileRouteEnd, -1, 'the notify-message route must remain after profile')

const userRouteMetaMatch = userRouteBlock.match(/meta:\s*{([\s\S]*?)\n\s*},\n\s*children:/)
const profileRouteBlock = userRouteBlock.slice(profileRouteStart, profileRouteEnd)
const profileRouteMatch = profileRouteBlock.match(/meta:\s*{([\s\S]*?)\n\s*}/)

assert.ok(profileRouteMatch, 'the profile route must keep explicit route meta')

const userRouteMeta = userRouteMetaMatch?.[1] || ''
const profileRouteMeta = profileRouteMatch[1]

assert.doesNotMatch(
  userRouteMeta,
  /hidden:\s*true/,
  'the /user shell must be visible so every logged-in user can see Personal Center in the menu'
)

assert.doesNotMatch(
  profileRouteMeta,
  /hidden:\s*true/,
  'the /user/profile route must be visible in the menu for every logged-in user'
)

assert.doesNotMatch(
  profileRouteMeta,
  /permission\s*:/,
  'the /user/profile route must not depend on business menu permissions'
)

assert.match(
  profileRouteMeta,
  /canTo:\s*true/,
  'the /user/profile route must remain directly reachable'
)

assert.match(
  userInfoSource,
  /push\('\/user\/profile'\)/,
  'the top-right user dropdown must keep the Personal Center shortcut'
)

assert.match(
  profileApiSource,
  /url:\s*'\/system\/user\/profile\/update-password'/,
  'Personal Center must keep the current-user password update API'
)

console.log('PASS: profile visible to all users static contract')
