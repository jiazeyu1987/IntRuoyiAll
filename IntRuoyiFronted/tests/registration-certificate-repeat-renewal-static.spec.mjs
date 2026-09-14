import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')

const renewalServicePath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/renewal/DccRegistrationCertificateRenewalService.java'
const listPath =
  'IntRuoyiFronted/src/views/dcc/registration-certificate/index/index.vue'

const renewalService = read(renewalServicePath)
const list = read(listPath)

assert.match(
  renewalService,
  /private final DccRegistrationCertificateActivationService activationService;/,
  'renewal formalization must reuse the formal activation service'
)
assert.match(
  renewalService,
  /if \(!command\.effectiveDate\(\)\.isAfter\(businessClock\.businessDate\(\)\)\) \{[\s\S]*activationService\.activateDueCandidate\(/,
  'a renewal due on or before the business date must activate in the formalization transaction'
)
assert.match(
  renewalService,
  /new DccRegistrationCertificateActivationCommand\([\s\S]*command\.certificateId\(\)[\s\S]*Math\.addExact\(command\.expectedRowVersion\(\), 1\)[\s\S]*currentVersion\.getId\(\)[\s\S]*renewalVersion\.getId\(\)/,
  'immediate activation must use the newly persisted candidate and incremented aggregate revision'
)
assert.match(
  renewalService,
  /return new DccRegistrationCertificateRenewalResult\([\s\S]*STATUS_CURRENT, false\);/,
  'immediate renewal result must report the renewed version as current'
)
assert.match(
  renewalService,
  /return new DccRegistrationCertificateRenewalResult\([\s\S]*STATUS_PENDING, false\);/,
  'future-effective renewals must remain pending'
)

const renewalPermission = `v-hasPermi="['dcc:registration-certificate:renewal:upload']"`
const renewalPermissionIndex = list.indexOf(renewalPermission)
assert.notEqual(renewalPermissionIndex, -1, 'the formal renewal permission must exist')
const renewalButtonStart = list.lastIndexOf('<el-button', renewalPermissionIndex)
const renewalButtonEnd = list.indexOf('</el-button>', renewalPermissionIndex)
assert.ok(renewalButtonStart >= 0 && renewalButtonEnd > renewalPermissionIndex,
  'the renewal button block must be complete')
const renewalButton = list.slice(renewalButtonStart, renewalButtonEnd + '</el-button>'.length)
assert.match(
  renewalButton,
  /v-if="row\.status === 'CURRENT'"/,
  'the list must expose renewal for every formally current version'
)
assert.doesNotMatch(
  renewalButton,
  /versionNo/,
  'a renewed current version must not lose renewal because its version number is greater than one'
)

console.log('registration certificate repeat renewal static contract passed')
