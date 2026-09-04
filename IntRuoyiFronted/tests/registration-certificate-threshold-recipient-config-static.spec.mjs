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
  assert.match(dialog, /normalizeUserIds\(\s*config\.thresholdRecipientUserIds\[key\] \|\| \[\]\s*\)/)
  assert.match(dialog, /getSimpleUserList/)
  assert.match(dialog, /users\.filter\(\(user\) => user\.disabled !== true\)/)
  assert.doesNotMatch(dialog, /user\.status === 0/)
  assert.match(dialog, /selected-recipient-tags/)
  assert.match(dialog, /<UserSelectV2/)
  assert.match(dialog, /:multiple="true"/)
  assert.match(dialog, /:hide-selected-label="true"/)
  assert.match(dialog, /normalizeUserId\(candidate\.id\) === normalizeUserId\(userId\)/)
  assert.doesNotMatch(dialog, /recipient-candidate-dialog/)
  assert.match(api, /thresholdRecipientUserIds/)
})

test('user selector can hide selected names from input and resolve string long ids', async () => {
  const userSelectV2Path = resolve('src/views/system/user/components/UserSelectV2.vue')
  const selector = await readFile(userSelectV2Path, 'utf8')

  assert.match(selector, /hideSelectedLabel\?: boolean/)
  assert.match(selector, /if \(props\.hideSelectedLabel\)/)
  assert.match(selector, /const normalizeUserId = \(id: number \| string\) => Number\(id\)/)
  assert.match(selector, /normalizeUserId\(item\.id\) === currentId/)
  assert.match(selector, /rows\.map\(\(item\) => normalizeUserId\(item\.id\)\)/)
})

test('notification recipient selector uses preloaded enabled users without system user query permission', async () => {
  const [configDialog, selector, userDialog] = await Promise.all([
    readFile(resolve('src/views/dcc/registration-certificate/config/ReminderConfigDialog.vue'), 'utf8'),
    readFile(resolve('src/views/system/user/components/UserSelectV2.vue'), 'utf8'),
    readFile(resolve('src/views/system/user/components/UserSelectDialogV2.vue'), 'utf8')
  ])

  assert.match(configDialog, /:user-options="userOptions"/)
  assert.match(selector, /userOptions\?: UserApi\.UserVO\[\]/)
  assert.match(selector, /:user-options="userOptions"/)
  assert.match(userDialog, /userOptions\?: UserApi\.UserVO\[\]/)
  assert.match(userDialog, /props\.userOptions/)
  assert.match(userDialog, /getUserPage\(queryParams\)/)
})
