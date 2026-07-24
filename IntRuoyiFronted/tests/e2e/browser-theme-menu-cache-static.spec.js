const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const readSource = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const userStoreSource = readSource('src/store/modules/user.ts')
const authSource = readSource('src/utils/auth.ts')

const setUserInfoActionMatch = userStoreSource.match(
  /async\s+setUserInfoAction\(\)\s*\{([\s\S]*?)\r?\n\s{4}\},\r?\n\s{4}async\s+setUserAvatarAction/
)

assert.ok(setUserInfoActionMatch, '用户 store 必须保留 setUserInfoAction 动作。')

const setUserInfoActionBody = setUserInfoActionMatch[1]

assert.match(
  setUserInfoActionBody,
  /const\s+userInfo\s*=\s*await\s+getInfo\(\)/,
  '登录后菜单和权限必须始终来自当前 token 的 get-permission-info 响应。'
)

assert.doesNotMatch(
  setUserInfoActionBody,
  /wsCache\.get\(CACHE_KEY\.USER\)/,
  'setUserInfoAction 不得读取旧 USER 缓存作为当前账号信息来源。'
)

assert.doesNotMatch(
  setUserInfoActionBody,
  /catch\s*\([^)]*\)\s*\{\s*\}/,
  'get-permission-info 失败不得被空 catch 吞掉，否则旧浏览器会继续显示旧菜单。'
)

assert.match(
  setUserInfoActionBody,
  /wsCache\.set\(CACHE_KEY\.ROLE_ROUTERS,\s*userInfo\.menus\)/,
  '当前账号菜单必须在 get-permission-info 成功后写入 roleRouters。'
)

const setTokenMatch = authSource.match(/export\s+const\s+setToken\s*=\s*\([^)]*\)\s*=>\s*\{([\s\S]*?)\n\}/)
assert.ok(setTokenMatch, 'auth.ts 必须声明 setToken。')
const setTokenBody = setTokenMatch[1]

assert.match(
  setTokenBody,
  /clearAuthenticatedUserCache\(\)/,
  '写入新 token 前必须清理旧用户与菜单缓存，避免跨账号/跨浏览器本地状态污染。'
)

assert.match(
  authSource,
  /const\s+clearAuthenticatedUserCache\s*=\s*\(\)\s*=>\s*\{[\s\S]*wsCache\.delete\(CACHE_KEY\.USER\)[\s\S]*wsCache\.delete\(CACHE_KEY\.ROLE_ROUTERS\)[\s\S]*wsCache\.delete\(CACHE_KEY\.VisitTenantId\)[\s\S]*\}/,
  '认证缓存清理必须覆盖 USER、ROLE_ROUTERS 和 VisitTenantId。'
)

console.log('PASS: browser theme and menu cache static contract')
