const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const packageJson = JSON.parse(readUtf8('package.json'))
const apiSource = readUtf8('src/api/dcc/controlledFile/uploadSizePolicies.ts')
const categoryPage = readUtf8('src/views/dcc/controlled-file/categories/index.vue')
const dialogSource = readUtf8('src/views/dcc/controlled-file/categories/components/CategoryUploadSizePolicyDialog.vue')
const setupScript = readUtf8('tests/e2e/dcc-upload-size-policy-real-setup.e2e.js')
const sharedEnvSource = readUtf8('scripts/dcc-write-control-env.mjs')
const packageSetup = readUtf8('tests/e2e/mdm-tenant-package-real-setup.e2e.js')
const roleSetup = readUtf8('tests/e2e/mdm-role-menu-real-setup.e2e.js')

assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-policy:management:check'],
  'node tests/e2e/dcc-upload-size-policy-management-static.spec.js',
  'package.json must expose the upload size policy management static gate'
)
assert.equal(
  packageJson.scripts?.['e2e:dcc:upload-policy:setup'],
  'node tests/e2e/dcc-upload-size-policy-real-setup.e2e.js',
  'package.json must expose the real frontend upload size policy setup path'
)

for (const fragment of [
  '/dcc/protection/upload-size-policies',
  '/dcc/protection/upload-size-policies/effective',
  'getUploadSizePolicyList',
  'createUploadSizePolicy',
  'updateUploadSizePolicy',
  'getEffectiveUploadSizePolicy',
  'CATEGORY_PURPOSE',
  'SOURCE',
  'DRAWING_PDF',
  'maxBytes'
]) {
  assert.ok(apiSource.includes(fragment), `upload size policy API must contain ${fragment}`)
}

for (const fragment of [
  'CategoryUploadSizePolicyDialog',
  'categoryUploadPolicyDialogRef',
  'openCategoryUploadPolicyDialog',
  "String(item.code || '').toLowerCase()",
  "String(item.name || '').toLowerCase()",
  '上传策略',
  "v-hasPermi=\"['dcc:controlled-file:category:manage']\""
]) {
  assert.ok(categoryPage.includes(fragment), `category page must expose upload policy entry: ${fragment}`)
}

for (const fragment of [
  '上传大小策略',
  'getUploadSizePolicyList',
  'createUploadSizePolicy',
  'updateUploadSizePolicy',
  "scopeType: 'CATEGORY_PURPOSE'",
  "purpose: 'SOURCE'",
  '最大大小',
  '保存策略',
  'formatPolicySize',
  'formatExactBytes',
  'DCC 上传大小策略已保存'
]) {
  assert.ok(dialogSource.includes(fragment), `upload policy dialog must contain ${fragment}`)
}

for (const fragment of [
  "require('playwright')",
  'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
  'DCC_UPLOAD_POLICY_E2E_APPROVAL',
  'ALLOW_TEST_DCC_UPLOAD_POLICY_WRITE',
  'DCC_UPLOAD_POLICY_E2E_BASE_URL',
  '172.30.30.57',
  '测试租户',
  'aoteman',
  '/dcc/controlled-file/categories',
  'Codex Local DCC Category',
  'findTargetCategoryRow',
  '.el-pagination .btn-next',
  '上传策略',
  '上传大小策略',
  '保存策略',
  'DCC_UPLOAD_SIZE_POLICY_SETUP_RESULT',
  'readOnly: true',
  'explicit user approval'
]) {
  assert.ok(setupScript.includes(fragment), `upload policy setup script must contain ${fragment}`)
}

for (const fragment of [
  ".filter({ hasText: targetName })",
  "row.getByRole('button', { name: actionName })",
  "missing target category row"
]) {
  assert.ok(setupScript.includes(fragment), `upload policy setup script must click the target category row action: ${fragment}`)
}

assert.ok(
  !setupScript.includes('page.request') &&
    !setupScript.includes('fetch(') &&
    !setupScript.includes('axios.') &&
    !setupScript.includes('request.post') &&
    !setupScript.includes('request.put'),
  'upload policy setup must use frontend pages and only observe browser responses, not direct admin API writes'
)
assert.ok(
  setupScript.includes('if (!ALLOW_WRITE)') &&
    setupScript.includes('set DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE=true') &&
    setupScript.includes('after explicit user approval'),
  'upload policy setup must default to read-only fail-fast before create/update'
)

for (const fragment of [
  'DCC_UPLOAD_POLICY_E2E_ALLOW_WRITE',
  'DCC_UPLOAD_POLICY_E2E_APPROVAL'
]) {
  assert.ok(sharedEnvSource.includes(fragment), `shared write-control env module must include ${fragment}`)
}

for (const fragment of ['文控权限', '文控中心']) {
  assert.ok(packageSetup.includes(fragment), `tenant package setup must include ${fragment}`)
  assert.ok(roleSetup.includes(fragment), `role menu setup must include ${fragment}`)
}

console.log('PASS: DCC upload size policy management static contract is present')
