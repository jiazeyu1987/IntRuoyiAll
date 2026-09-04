import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const list = read('src/views/dcc/registration-certificate/index/index.vue')

assert.match(
  list,
  /import\s+\{\s*checkPermi,\s*checkRole\s*\}\s+from\s+'@\/utils\/permission'/,
  'registration certificate list must use shared permission and role helpers'
)

assert.match(
  list,
  /const REGISTRATION_CERTIFICATE_MANAGER_ROLE = 'dcc_registration_certificate_approver'/,
  'registration certificate list must name the manager role code explicitly'
)

for (const permission of [
  'dcc:registration-certificate:upload:create',
  'dcc:registration-certificate:renewal:upload',
  'dcc:registration-certificate:change:submit'
]) {
  assert.match(
    list,
    new RegExp(`checkPermi\\(\\['${permission.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}'\\]\\)`),
    `${permission} must remain an allowed direct permission path`
  )
}

for (const computedName of [
  'canUploadRegistrationCertificate',
  'canRenewRegistrationCertificate',
  'canChangeRegistrationCertificate'
]) {
  assert.match(
    list,
    new RegExp(`const ${computedName} = computed\\(\\(\\) =>[\\s\\S]{0,160}checkRole\\(\\[REGISTRATION_CERTIFICATE_MANAGER_ROLE\\]\\)`),
    `${computedName} must allow the registration manager role`
  )
}

assert.match(
  list,
  /v-if="canUploadRegistrationCertificate"[\s\S]{0,180}>\s*[\s\S]{0,80}上传注册证\s*<\/el-button>/,
  'upload button must be visible when the manager-role computed permission is true'
)
assert.match(
  list,
  /v-if="row\.status === 'CURRENT' && canRenewRegistrationCertificate"[\s\S]{0,220}>\s*延续\s*<\/el-button>/,
  'renewal button must keep CURRENT status gating and allow manager role visibility'
)
assert.match(
  list,
  /v-if="row\.status === 'CURRENT' && row\.hasPendingChange === false && canChangeRegistrationCertificate"[\s\S]{0,240}>\s*变更\s*<\/el-button>/,
  'change button must keep CURRENT and pending-change gating while allowing manager role visibility'
)

for (const forbidden of [
  /<el-button\s+v-hasPermi="\['dcc:registration-certificate:upload:create'\]"/,
  /v-hasPermi="\['dcc:registration-certificate:renewal:upload'\]"/,
  /v-hasPermi="\['dcc:registration-certificate:change:submit'\]"/
]) {
  assert.doesNotMatch(list, forbidden, 'manager-visible actions must not be hidden by permission-only directives')
}

console.log('PASS: registration certificate manager action visibility static contract')
