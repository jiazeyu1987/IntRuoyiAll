const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(frontendRoot, 'src/views/infra/runtime-control/index.vue')
const apiPath = path.join(frontendRoot, 'src/api/infra/runtimeControl/index.ts')
const page = fs.readFileSync(pagePath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

for (const token of [
  '发布到审查服务器',
  'previewRuntimeControlBackupPublish',
  'authorizeRuntimeControlBackupPublish',
  'publishRuntimeControlBackup',
  'backupPublishDialog',
  'infra:runtime-control:publish-backup',
  'sourceSelectionId',
  'idempotencyKey',
  'prodConfirmText',
  '审查服务器',
  '固定源码',
  'stateVersion',
  'createdAt',
  'updatedAt',
  'releaseWorkflowDurationText'
]) {
  assert.ok(page.includes(token) || api.includes(token), `missing review publish contract token: ${token}`)
}

assert.match(page, /row\.backupIntent|row\.targetEnvironment === 'backup'|row\.automaticPublish/)
assert.match(page, /confirm.*publish|publish.*confirm/i)
assert.match(page, /@closed="clearBackupPublishPreview"/)
assert.doesNotMatch(page, /catch\s*\{\s*\}/)
assert.match(api, /\/release-workflows\/backup-preview/)
assert.match(api, /\/release-workflows\/backup-authorization/)
assert.match(api, /\/release-workflows\/publish-backup/)

console.log('review server publish button static contract: PASS')
