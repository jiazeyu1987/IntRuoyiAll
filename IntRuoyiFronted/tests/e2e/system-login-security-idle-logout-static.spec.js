const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const hookSource = readSource('src/hooks/web/useIdleLogout.ts')
const userInfoSource = readSource('src/layout/components/UserInfo/src/UserInfo.vue')

for (const token of [
  'mousemove',
  'mousedown',
  'keydown',
  'touchstart',
  'scroll',
  'visibilitychange'
]) {
  assert.ok(hookSource.includes(token), `idle logout hook must listen for ${token}`)
}

assert.ok(
  hookSource.includes('15 * 60 * 1000') || hookSource.includes('900000'),
  'idle logout hook must use a 15 minute inactivity timeout'
)

for (const token of [
  'userStore.loginOut()',
  "console.error('Idle logout request failed before local session cleanup.'",
  "replace('/login",
  'onMounted',
  'onBeforeUnmount'
]) {
  assert.ok(hookSource.includes(token), `idle logout hook must keep logout lifecycle: ${token}`)
}

assert.equal(
  hookSource.includes('catch(() => {})'),
  false,
  'idle logout hook must not silently swallow logout failures'
)

assert.ok(
  userInfoSource.includes("useIdleLogout") && userInfoSource.includes("useIdleLogout()"),
  'UserInfo must install the idle logout hook in the authenticated layout'
)

console.log('PASS: system login idle logout static contract')
