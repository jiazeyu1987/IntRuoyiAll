import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const sqlPath = '../IntRuoyiBackend/sql/mysql/20260816_dcc_registration_certificate_menu.sql'
const profileIndexPath = 'src/views/Profile/Index.vue'
const componentIndexPath = 'src/views/Profile/components/index.ts'
const configComponentPath = 'src/views/Profile/components/RegistrationCertificateConfig.vue'
const configApiPath = 'src/api/dcc/registrationCertificate/reminderConfig.ts'
const listPagePath = 'src/views/dcc/registration-certificate/index/index.vue'
const configControllerPath =
  '../IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/controller/admin/config/DccRegistrationCertificateReminderConfigController.java'

for (const file of [
  sqlPath,
  profileIndexPath,
  componentIndexPath,
  configComponentPath,
  configApiPath,
  listPagePath,
  configControllerPath
]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const sql = read(sqlPath)
assert.match(sql, /release-migration: allowedEnvironments=test,backup,prod;/, 'menu migration must declare release metadata')
assert.match(sql, /dependsOn=20260818_dcc_registration_certificate_reminder/, 'menu migration must depend on reminder/API schema')
assert.match(sql, /dependsOn=.*20260626_dcc_basic_data_global_submenu/, 'menu migration must depend on 基础数据 parent menu')
for (const token of [
  '基础数据',
  '注册证',
  'registration-certificate',
  'registration-certificate/detail/:id',
  'registration-certificate/history/:id',
  'dcc/registration-certificate/index/index',
  'dcc/registration-certificate/detail/index',
  'dcc/registration-certificate/history/index',
  'DccRegistrationCertificateIndex',
  'DccRegistrationCertificateDetail',
  'DccRegistrationCertificateHistory',
  'dcc:registration-certificate:query-current',
  'dcc:registration-certificate:create',
  'dcc:registration-certificate:update',
  'dcc:registration-certificate:delete-draft',
  'dcc:registration-certificate:formalize',
  'dcc:registration-certificate:access-request:create',
  'dcc:registration-certificate:config:query',
  'dcc:registration-certificate:config:update'
]) {
  assert.match(sql, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `menu SQL must contain ${token}`)
}
assert.doesNotMatch(sql, /golden-finger|mes:pro-batch-record-execution:golden-finger/, 'registration certificate menu must not reuse golden-finger permission')
assert.match(sql, /visible`\s*=\s*b'0'|b'0'\s+AS `visible`|,\s*b'0'\s*,\s*b'1'/, 'detail/history route rows must be hidden dynamic routes')

const profileIndex = read(profileIndexPath)
for (const token of [
  'RegistrationCertificateConfig',
  'REGISTRATION_CERTIFICATE_CONFIG_QUERY_PERMISSION',
  'REGISTRATION_CERTIFICATE_CONFIG_UPDATE_PERMISSION',
  'hasAnyProfileConfigPermission',
  'hasRegistrationCertificateConfigPermission',
  'hasRegistrationCertificateConfigUpdatePermission',
  'name="registrationCertificate"',
  ':can-update="hasRegistrationCertificateConfigUpdatePermission"'
]) {
  assert.match(profileIndex, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Profile index must contain ${token}`)
}
assert.doesNotMatch(
  profileIndex,
  /<el-tab-pane\s+label="配置"\s+name="config"\s+v-if="hasGoldenFingerPermission"/,
  'Profile config tab must not be gated only by golden-finger permission'
)
assert.match(
  profileIndex,
  /<el-tab-pane[\s\S]{0,180}label="注册证配置"[\s\S]{0,160}v-if="hasRegistrationCertificateConfigPermission"/,
  'registration certificate config pane must mount only with its own query permission'
)

const componentIndex = read(componentIndexPath)
assert.match(componentIndex, /RegistrationCertificateConfig/, 'Profile component barrel must export registration certificate config')

const configApi = read(configApiPath)
for (const token of [
  'DccRegistrationCertificateReminderConfigRespVO',
  'DccRegistrationCertificateReminderConfigUpdateReqVO',
  'getRegistrationCertificateReminderConfig',
  'updateRegistrationCertificateReminderConfig',
  '/dcc/registration-certificates/reminder-config',
  'expectedRowVersion'
]) {
  assert.match(configApi, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `config API must contain ${token}`)
}
assert.doesNotMatch(configApi, /localStorage|sessionStorage|mock|placeholder|defaultSuccess/, 'config API must not persist, mock or default-success')

const configController = read(configControllerPath)
assert.match(
  configController,
  /@RequestMapping\("\/dcc\/registration-certificates\/reminder-config"\)/,
  'config controller must expose the exact reminder-config API route'
)
assert.match(
  configController,
  /@PreAuthorize\("@ss\.hasPermission\('dcc:registration-certificate:config:query'\)"\)[\s\S]{0,220}getConfig/,
  'config query API must require the independent registration-certificate config query permission'
)
assert.match(
  configController,
  /@PreAuthorize\("@ss\.hasPermission\('dcc:registration-certificate:config:update'\)"\)[\s\S]{0,220}updateConfig/,
  'config update API must require the independent registration-certificate config update permission'
)
assert.doesNotMatch(
  configController,
  /golden-finger|mes:pro-batch-record-execution:golden-finger|hasRole\('doc_control'\)|isAuthenticated\(\)/,
  'config APIs must not inherit golden-finger, doc-control role or authenticated-only authorization'
)

const configComponent = read(configComponentPath)
for (const token of [
  'data-testid="registration-certificate-config"',
  'getRegistrationCertificateReminderConfig',
  'updateRegistrationCertificateReminderConfig',
  'dailyRunTime',
  'Asia/Shanghai',
  'expectedRowVersion',
  'canUpdate',
  '接收规则',
  '注册证文控'
]) {
  assert.match(configComponent, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `config component must contain ${token}`)
}
assert.match(configComponent, /onMounted\(loadConfig\)/, 'config component must load only after the authorized pane mounts')
assert.doesNotMatch(configComponent, /catch\s*\(\s*\)\s*\{\s*\}|mock|placeholder|defaultSuccess|admin/, 'config component must not swallow errors, mock, or default to admin recipients')

const listPage = read(listPagePath)
assert.match(listPage, /\/mdm\/registration-certificate\/detail\/\$\{certificateId\}/, 'list detail navigation must match the 基础数据 dynamic route')
assert.doesNotMatch(listPage, /\/dcc\/registration-certificate\/detail\/\$\{certificateId\}/, 'list page must not navigate to an unowned /dcc static route')
