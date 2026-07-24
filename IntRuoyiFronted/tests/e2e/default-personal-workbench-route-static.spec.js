const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const remainingRouter = read('src/router/modules/remaining.ts')
const loginForm = read('src/views/Login/components/LoginForm.vue')
const userInfo = read('src/layout/components/UserInfo/src/UserInfo.vue')
const permissionGuard = read('src/permission.ts')

const homeRouteMatch = remainingRouter.match(
  /path:\s*'\/'[\s\S]{0,180}component:\s*Layout[\s\S]{0,120}redirect:\s*'([^']+)'/
)

assert.ok(homeRouteMatch, '根路由必须定义 Layout 和 redirect。')
assert.equal(
  homeRouteMatch[1],
  '/user/profile',
  '进入系统默认根路由必须进入个人中心的个人工作台。'
)
assert.doesNotMatch(
  homeRouteMatch[0],
  /redirect:\s*'\/index'/,
  '进入系统默认根路由不得继续进入首页 /index。'
)

assert.match(
  remainingRouter,
  /path:\s*'\/user'[\s\S]*path:\s*'profile'[\s\S]*component:\s*\(\)\s*=>\s*import\('@\/views\/Profile\/Index\.vue'\)[\s\S]*personalWorkbenchTodoBadge:\s*true/,
  '个人中心个人工作台路由必须继续指向 Profile/Index.vue 并保留待办徽标。'
)

assert.match(
  loginForm,
  /if\s*\(!redirect\.value\)\s*\{\s*redirect\.value\s*=\s*'\/'\s*\}/,
  '登录页无 redirect 参数时必须进入根路由，由根路由统一转到个人工作台。'
)
assert.doesNotMatch(
  loginForm,
  /redirect\.value\s*=\s*'\/index'|path:\s*redirect\.value\s*\|\|\s*['"]\/index['"]/,
  '登录页无 redirect 参数时不得硬编码进入首页 /index。'
)

assert.match(
  permissionGuard,
  /if\s*\(to\.path\s*===\s*'\/login'\)\s*\{\s*next\(\{\s*path:\s*'\/'\s*\}\)/,
  '已登录用户访问登录页时必须进入根路由，由根路由统一转到个人工作台。'
)

assert.match(
  userInfo,
  /replace\('\/login\?redirect=\/user\/profile'\)/,
  '退出登录后的默认登录入口必须带回个人工作台，不得带回首页。'
)
assert.doesNotMatch(
  userInfo,
  /redirect=\/index/,
  '退出登录后的默认登录入口不得继续带 redirect=/index。'
)

console.log('PASS: default personal workbench route static contract')
