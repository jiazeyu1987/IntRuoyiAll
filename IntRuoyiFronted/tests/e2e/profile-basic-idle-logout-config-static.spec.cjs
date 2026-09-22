const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const profileIndexPath = 'src/views/Profile/Index.vue'
const componentIndexPath = 'src/views/Profile/components/index.ts'
const basicConfigPath = 'src/views/Profile/components/ProfileBasicConfig.vue'

assert(exists(profileIndexPath), 'Profile index must exist')
assert(exists(componentIndexPath), 'Profile component barrel must exist')
assert(exists(basicConfigPath), 'Profile basic config component must exist')

const profileIndex = read(profileIndexPath)
const componentIndex = read(componentIndexPath)
const basicConfig = read(basicConfigPath)

assert(
  profileIndex.includes('ProfileBasicConfig'),
  'Profile config tab must render the basic config component'
)
assert(
  /<el-tab-pane[\s\S]{0,180}label="基础配置"[\s\S]{0,180}name="basicConfig"/.test(profileIndex),
  'Profile config tab strip must include a 基础配置 sub-tab in the red-box position'
)
assert(
  profileIndex.includes("INFRA_CONFIG_QUERY_PERMISSION = 'infra:config:query'") &&
    profileIndex.includes("INFRA_CONFIG_UPDATE_PERMISSION = 'infra:config:update'"),
  'Profile basic config must be permission-gated by infra config query/update permissions'
)
assert(
  profileIndex.includes("import { checkPermi } from '@/utils/permission'"),
  'Profile basic config permission checks must reuse the shared permission helper'
)
assert(
  /const\s+hasProfilePermission\s*=\s*\(permission:\s*string\)\s*=>[\s\S]{0,160}isAdminUser\.value[\s\S]{0,160}checkPermi\(\[permission\]\)/.test(
    profileIndex
  ),
  'Profile basic config permissions must treat super admin/admin as configurable while preserving normal permission checks'
)
assert(
  !/userStore\.permissions\.has\(INFRA_CONFIG_(QUERY|UPDATE)_PERMISSION\)/.test(profileIndex),
  'Profile basic config must not bypass shared permission handling for infra config permissions'
)
assert(
  /hasAnyProfileConfigPermission[\s\S]{0,220}hasInfraConfigQueryPermission/.test(profileIndex) ||
    /hasInfraConfigQueryPermission[\s\S]{0,220}hasAnyProfileConfigPermission/.test(profileIndex),
  'Profile config tab must be visible for infra config query permission'
)
assert(
  /route\.query\.config\s*===\s*'basicConfig'[\s\S]{0,260}hasInfraConfigQueryPermission/.test(
    profileIndex
  ),
  'Profile must allow direct navigation to the basic config sub-tab'
)
assert(
  componentIndex.includes('ProfileBasicConfig'),
  'Profile component barrel must export ProfileBasicConfig'
)

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
