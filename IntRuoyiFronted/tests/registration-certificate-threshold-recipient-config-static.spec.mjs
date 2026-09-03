import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import test from 'node:test'

const pagePath = resolve('src/views/dcc/registration-certificate/index/index.vue')
const apiPath = resolve('src/api/dcc/registrationCertificate/reminderConfig.ts')
const dialogPath = resolve('src/views/dcc/registration-certificate/config/ReminderConfigDialog.vue')

test('registration certificate page configures all fixed expiry thresholds with multi-select recipients', async () => {
  const [page, api, dialog] = await Promise.all([
    readFile(pagePath, 'utf8'),
    readFile(apiPath, 'utf8'),
    readFile(dialogPath, 'utf8')
  ])

  assert.match(page, /通知设置/)
  assert.match(page, /dcc:registration-certificate:config:query/)
  assert.match(dialog, /dcc:registration-certificate:config:update/)
  assert.match(dialog, /30 个月/)
  assert.match(dialog, /8 个月/)
  assert.match(dialog, /2 个月/)
  assert.match(dialog, /1 个月/)
  assert.match(dialog, /thresholdRecipientUserIds\[threshold\]\.push\(user\.id\)/)
  assert.match(dialog, /getSimpleUserList/)
  assert.match(dialog, /selected-recipient-tags/)
  assert.match(dialog, /<UserSelect/)
  assert.match(dialog, /addRecipient/)
  assert.doesNotMatch(dialog, /recipient-candidate-dialog/)
  assert.doesNotMatch(dialog, /UserSelectDialogV2/)
  assert.match(api, /thresholdRecipientUserIds/)
})
