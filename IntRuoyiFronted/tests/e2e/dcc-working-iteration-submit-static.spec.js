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
  'the full application keeps its selected WORKING iteration identity endpoint'
)
assert.match(page, /data-testid="dcc-controlled-browser-submit-approval"/)
assert.match(page, /canSubmitWorkingIteration\(row, getSelectedVersion\(row\)\)/)
assert.match(
  page,
  /file\.status === 'WORKING'[\s\S]*canEditVersion\(file\)[\s\S]*!file\.checkedOutBy/,
  'an owned unlocked selected WORKING iteration may open its application'
)
assert.match(page, /const handleSubmitWorkingIteration[\s\S]*canSubmitWorkingIteration\(row, file\)[\s\S]*openManagement\(id\)/)
assert.doesNotMatch(page, /await submitControlledFileWorkingIteration\(id,/)
assert.doesNotMatch(
  page,
  /checkControlledFileRouteReadiness\(\{[\s\S]*selectedSignoffUserIds/,
  'working iteration route readiness must not send deprecated manual signoff users'
)
assert.doesNotMatch(
  page,
  /submitControlledFileWorkingIteration\(id,[\s\S]*selectedSignoffUserIds/,
  'working iteration submit must not send deprecated manual signoff users'
)
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
  /const canSubmitWorkingIteration = \([\s\S]*?canEditVersion\(file\)[\s\S]*?\)/,
  'only the requester who owns the working iteration may see the submit-for-approval action'
)
assert.match(
  page,
  /<el-radio value="MAJOR" :disabled="!canMajorCheckin">大版本（下一修订版，需项目所有者）<\/el-radio>/,
  'major version creation must be selected inside the controlled check-in flow and gated by project owner capability'
)
assert.doesNotMatch(page, /message\.success\(`版本 \$\{file\.versionNo\} 已提交审批`\)/)
assert.match(page, /checkinForm\.versionChangeType === 'MAJOR'[\s\S]*'大版本检入必须上传新的源文件'/)
assert.match(page, /checkinControlledFile\(baseId,[\s\S]*versionChangeType:\s*checkinForm\.versionChangeType/)
assert.doesNotMatch(page, /创建并送审|大版本已创建并送审/)

console.log('PASS: DCC WORKING iteration submit static contract')
