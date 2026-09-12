const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/dcc/controlledFile/workflow.ts')
const page = read('src/views/dcc/controlled-file/browser/index.vue')

assert.match(
  api,
  /export const submitControlledFileWorkingIteration[\s\S]*`\/dcc\/controlled-files\/\$\{id\}\/submit`/,
  'the browser must submit the existing WORKING iteration through its identity-owned endpoint'
)
assert.match(page, /data-testid="dcc-controlled-browser-submit-approval"/)
assert.match(page, /canSubmitLatestWorkingIteration\(row, getSelectedVersion\(row\)\)/)
assert.match(
  page,
  /file\.status === 'WORKING'[\s\S]*isLatestWorkingIteration\(row, file\)[\s\S]*!file\.checkedOutBy/,
  'only the latest unlocked WORKING iteration may expose submit approval'
)
assert.match(page, /submitControlledFileWorkingIteration\(id,[\s\S]*idempotencyKey:/)
assert.match(
  page,
  /const browserMutationIdempotencyKeys = reactive<Record<string, string>>\(\{\}\)/,
  'browser-side DCC mutations must cache idempotency keys per action target for retry replay'
)
assert.match(
  page,
  /const getOrCreateBrowserMutationIdempotencyKey[\s\S]*browserMutationIdempotencyKeys\[cacheKey\][\s\S]*createBrowserMutationIdempotencyKey\(action, id\)/,
  'working submit and major revision actions must reuse the same key after transient failures'
)
assert.match(
  page,
  /deleteBrowserMutationIdempotencyKey\('working-submit', id\)/,
  'successful working submit must release its cached idempotency key after the server has accepted it'
)
assert.match(
  page,
  /deleteBrowserMutationIdempotencyKey\('major-revision', file\.id\)/,
  'successful major revision creation must release its cached idempotency key after the server has accepted it'
)
assert.match(page, /message\.success\(`版本 \$\{file\.versionNo\} 已提交审批`\)/)
assert.match(page, /confirmButtonText: '创建修订版'/)
assert.match(page, /message\.success\(`大版本已创建，工作版本记录编号 \$\{newId\}`\)/)
assert.doesNotMatch(page, /创建并送审|大版本已创建并送审/)

console.log('PASS: DCC WORKING iteration submit static contract')
