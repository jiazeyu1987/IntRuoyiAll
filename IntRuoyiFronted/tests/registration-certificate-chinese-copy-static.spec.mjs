import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const root = process.cwd()
const read = (relativePath) => readFileSync(join(root, relativePath), 'utf8')
const viewsRoot = 'IntRuoyiFronted/src/views/dcc/registration-certificate'
const state = read(`${viewsRoot}/shared/state.ts`)
const actionPanel = read(`${viewsRoot}/workflow/ActionPanel.vue`)

for (const [stateValue, label] of [
  ['T_30', '到期前 30 个月'],
  ['T_8', '到期前 8 个月'],
  ['T_2', '到期前 2 个月'],
  ['T_1', '到期前 1 个月']
]) {
  assert.match(state, new RegExp(`value:\\s*'${stateValue}',\\s*label:\\s*'${label}'`),
    `${stateValue} reminder option must display Chinese copy`)
  assert.match(state, new RegExp(`${stateValue}:\\s*'${label}'`),
    `${stateValue} reminder formatter must display Chinese copy`)
}
assert.doesNotMatch(state, /return\s+state\.replace\(/,
  'reminder formatter must not expose internal reminder codes')
assert.doesNotMatch(state, /\?\.label\s*\|\|\s*status/,
  'status formatter must not expose internal status codes')

for (const englishCopy of ['BPM Native', 'BPM 实例', 'BPM 状态', '后端结果：']) {
  assert.doesNotMatch(actionPanel, new RegExp(englishCopy),
    `registration certificate UI must not expose ${englishCopy}`)
}
for (const removedApprovalResultToken of [
  'label="审批结果"',
  'registration-certificate-approval-result-action',
  'handleDownloadGrant',
  'handleRevokeGrant',
  '撤销授权'
]) {
  assert.doesNotMatch(actionPanel, new RegExp(removedApprovalResultToken),
    `registration certificate access request panel must not expose removed approval-result control ${removedApprovalResultToken}`)
}
assert.match(actionPanel, /lastActionResult\.value\s*=\s*`\$\{name\}成功`/,
  'success feedback must not expose raw backend result values')

console.log('registration certificate Chinese copy static contract passed')
