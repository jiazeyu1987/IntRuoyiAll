import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const exists = (relativePath) => existsSync(join(root, relativePath))

const notifierPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/notification/event/DccRegistrationCertificateBusinessEventNotifier.java'
const configPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/notification/event/DccRegistrationCertificateBusinessEventNotificationConfigService.java'
const uploadPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/upload/DccRegistrationCertificateUploadService.java'
const renewalPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/renewal/DccRegistrationCertificateRenewalService.java'
const activationPath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/activation/DccRegistrationCertificateActivationService.java'
const changePath =
  'IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/registrationcertificate/service/change/DccRegistrationCertificateChangeService.java'

for (const file of [notifierPath, configPath, uploadPath, renewalPath, activationPath, changePath]) {
  assert.equal(exists(file), true, `${file} must exist`)
}

const notifier = read(notifierPath)
assert.match(notifier, /DccRegistrationCertificateBusinessEventNotificationService/,
  'business event notifier must use the existing notification sender')
assert.match(notifier, /DccRegistrationCertificateBusinessEventNotificationConfigService/,
  'business event notifier must load the configured recipient scope')
for (const method of [
  'notifyNewCertificateFormalized',
  'notifyRenewalCandidateUploaded',
  'notifyRenewalCandidateActivated',
  'notifyChangeApprovalRecorded'
]) {
  assert.match(notifier, new RegExp(`\\b${method}\\b`),
    `business event notifier must expose ${method}`)
}

const config = read(configPath)
assert.match(config, /registrationCertificateReminderDailyJob/,
  'notification recipient config must share the registration certificate reminder job recipient scope')
assert.match(config, /roleIds/, 'notification recipient config must require document-control role ids')
assert.match(config, /permission/, 'notification recipient config must require recipient permission')
assert.match(config, /REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED/,
  'missing notification recipient config must fail fast')

const upload = read(uploadPath)
assert.match(upload, /notifyNewCertificateFormalized/,
  'new certificate approval must notify authorized recipients')

const renewal = read(renewalPath)
assert.match(renewal, /notifyRenewalCandidateUploaded/,
  'approved renewal candidate upload must notify authorized recipients')

const activation = read(activationPath)
assert.match(activation, /notifyRenewalCandidateActivated/,
  'renewal activation must notify authorized recipients')

const change = read(changePath)
assert.match(change, /notifyChangeApprovalRecorded/,
  'approved change record must notify authorized recipients')

console.log('registration certificate business event notification static contract passed')
