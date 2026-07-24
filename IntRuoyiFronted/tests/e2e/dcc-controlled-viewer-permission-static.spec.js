const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '..', '..')
const detailPath = path.join(root, 'src/views/dcc/controlled-file/detail/index.vue')
const detailSource = fs.readFileSync(detailPath, 'utf8')

const extractFunction = (name) => {
  const startToken = `const ${name} = async`
  const startIndex = detailSource.indexOf(startToken)
  assert.notEqual(startIndex, -1, `missing ${name}`)
  const nextFunctionIndex = detailSource.indexOf('\nconst ', startIndex + startToken.length)
  assert.notEqual(nextFunctionIndex, -1, `cannot find end of ${name}`)
  return detailSource.slice(startIndex, nextFunctionIndex)
}

const loadActiveObsoleteAction = extractFunction('loadActiveObsoleteAction')
const loadActivePublishAction = extractFunction('loadActivePublishAction')
const loadDccSignatureEvidenceList = extractFunction('loadDccSignatureEvidenceList')
const reloadAll = extractFunction('reloadAll')

for (const [name, block] of [
  ['loadActiveObsoleteAction', loadActiveObsoleteAction],
  ['loadActivePublishAction', loadActivePublishAction],
  ['loadDccSignatureEvidenceList', loadDccSignatureEvidenceList]
]) {
  assert.match(
    block,
    /if\s*\(\s*viewerMode\.value\s*\)\s*\{/,
    `${name} must return before loading management-only data in controlled-file viewer mode.`
  )
}

assert.ok(
  !loadDccSignatureEvidenceList.includes('Number(controlledFileId.value)'),
  'controlledFileId must not be converted with Number(), because DCC ids exceed JavaScript safe integer precision.'
)

assert.match(
  loadDccSignatureEvidenceList,
  /controlledFileId:\s*controlledFileId\.value/,
  'signature evidence query must preserve the controlledFileId string value.'
)

assert.match(
  reloadAll,
  /await\s+loadData\(\)[\s\S]*await\s+loadDccSignatureEvidenceList\(\)[\s\S]*await\s+loadApprovalDetail\(\)/,
  'reloadAll must keep the established load sequence; individual loaders own viewer-mode permission gating.'
)

assert.ok(
  !detailSource.includes('/admin-api/form-center/actions/active-instance'),
  'detail page must not hardcode form-center active-instance URLs.'
)

console.log('PASS: DCC controlled viewer permission static contract')
