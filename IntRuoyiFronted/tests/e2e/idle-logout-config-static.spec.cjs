const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const hookPath = path.join(root, 'src', 'hooks', 'web', 'useIdleLogout.ts')
const hook = fs.readFileSync(hookPath, 'utf8')

assert(
  hook.includes("import * as ConfigApi from '@/api/infra/config'"),
  'idle logout hook must read runtime config through infra config API'
)

assert(
  hook.includes("system.login.idle-timeout-minutes"),
  'idle logout hook must use the admin-maintained idle logout config key'
)

assert(
  /ConfigApi\.getConfigKey\(\s*IDLE_LOGOUT_MINUTES_CONFIG_KEY\s*\)/.test(hook),
  'idle logout hook must fetch the idle timeout value by config key'
)

assert(
  !/const\s+IDLE_TIMEOUT\s*=\s*15\s*\*\s*60\s*\*\s*1000/.test(hook),
  'idle logout hook must not keep the hard-coded 15 minute timeout'
)

assert(
  /parseIdleLogoutMinutes/.test(hook) &&
    /MIN_IDLE_LOGOUT_MINUTES/.test(hook) &&
    /MAX_IDLE_LOGOUT_MINUTES/.test(hook) &&
    /登录空闲退出配置无效/.test(hook),
  'idle logout hook must validate the configured minute value and surface invalid config'
)

assert(
  /handleIdleLogoutConfigError/.test(hook) &&
    /ElMessage\.error/.test(hook) &&
    /handleLogout/.test(hook),
  'idle logout hook must fail visibly and clear the session when config is unavailable'
)

console.log('PASS: idle logout config static contract')
