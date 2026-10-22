const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const profileIndexPath = 'src/views/Profile/Index.vue'
const componentIndexPath = 'src/views/Profile/components/index.ts'
const basicConfigPath = 'src/views/infra/config/basic/components/IdleLogoutSetting.vue'

assert(exists(profileIndexPath), 'Profile index must exist')
assert(exists(componentIndexPath), 'Profile component barrel must exist')
assert(exists(basicConfigPath), 'Profile basic config component must exist')

const profileIndex = read(profileIndexPath)
const componentIndex = read(componentIndexPath)
const basicConfig = read(basicConfigPath)

const basicPage = read('src/views/infra/config/basic/index.vue')
const access = read('src/utils/businessConfigAccess.ts')
assert(basicPage.includes('<IdleLogoutSetting v-if="canQuery" :can-update="canUpdate"'), 'infra page mounts only authorized idle configuration')
assert(basicPage.includes("hasInfraConfigPermission('infra:config:query')"), 'query permission remains independent')
assert(basicPage.includes("hasInfraConfigPermission('infra:config:update')"), 'update permission remains independent')
assert(access.includes("checkPermi([permission])"), 'infra policy retains shared permission evaluation')
assert(access.includes("user.getRoles.includes('super_admin')") && access.includes("user.getUser.username === 'admin'"), 'infra administrator behavior is preserved')
assert(!profileIndex.includes('ProfileBasicConfig') && !componentIndex.includes('ProfileBasicConfig'), 'Profile no longer imports business configuration')
assert(read('src/router/modules/businessConfig.ts').includes("name: 'InfraBasicConfig'"), 'basic config has named independent route')
assert(read('src/views/system/config-migration/BusinessConfigMigrationNotice.vue').includes("name: 'InfraBasicConfig'"), 'old config URL offers explicit migration target')

for (const token of [
  'data-testid="profile-basic-config"',
  '登录空闲退出时间',
  'system.login.idle-timeout-minutes',
  "import * as ConfigApi from '@/api/infra/config'",
  'ConfigApi.getConfigPage',
  'ConfigApi.updateConfig',
  'idleLogoutMinutes',
  '保存',
  'handleSave',
  'canUpdate'
]) {
  assert(basicConfig.includes(token), `ProfileBasicConfig must contain ${token}`)
}

assert(
  /<el-input-number[\s\S]{0,260}v-model="form\.idleLogoutMinutes"[\s\S]{0,260}:min="1"[\s\S]{0,260}:max="1440"/.test(
    basicConfig
  ),
  'ProfileBasicConfig must expose one numeric input constrained to 1..1440 minutes'
)
assert(
  /<el-button[\s\S]{0,180}type="primary"[\s\S]{0,180}@click="handleSave"[\s\S]{0,180}>[\s\S]{0,80}保存/.test(
    basicConfig
  ),
  'ProfileBasicConfig must expose one primary save button'
)
assert(
  /const\s+list\s*=\s*data\.list\s*\|\|\s*\[\][\s\S]{0,260}list\.length\s*!==\s*1/.test(
    basicConfig
  ),
  'ProfileBasicConfig must fail fast unless exactly one idle logout config row is returned'
)
assert(
  !/createConfig|localStorage|sessionStorage|mock|placeholder|defaultSuccess|catch\s*\(\s*\)\s*\{\s*\}/.test(
    basicConfig
  ),
  'ProfileBasicConfig must not create fallback config, mock, persist locally, default-success, or swallow errors'
)

console.log('PASS: profile basic idle logout config static contract')
