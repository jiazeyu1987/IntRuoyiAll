const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
const requireToken = (source, token, message) => {
  assert.match(source, new RegExp(escapeRegExp(token)), message)
}

const extractBetween = (source, startToken, endToken, label) => {
  const start = source.indexOf(startToken)
  assert.notEqual(start, -1, `${label} missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start + startToken.length)
  assert.notEqual(end, -1, `${label} missing end token: ${endToken}`)
  return source.slice(start, end)
}

const packageJson = JSON.parse(readSource('package.json'))
const uploadPage = readSource('src/views/dcc/controlled-file/upload/index.vue')
const submitter = readSource('src/views/dcc/controlled-file/upload/submitter.ts')

assert.equal(
  packageJson.scripts['e2e:dcc:upload-optimization:static'],
  'node tests/e2e/dcc-upload-optimization-static.spec.js',
  'package.json must expose the DCC upload optimization static contract'
)

requireToken(
  uploadPage,
  '该类别未配置正式默认目录，请联系文控管理员配置后再提交。',
  'missing default directory must be shown as a blocking controlled-save configuration error'
)
assert.doesNotMatch(
  uploadPage,
  /系统将自动提交到未分类目录|按规则发布到“未分类”/,
  'missing default directory must not be presented as an automatic unclassified landing'
)

requireToken(
  uploadPage,
  'const isVersionNoFormatValid = computed',
  'upload page must compute frontend version format validity'
)
requireToken(
  uploadPage,
  'const VERSION_NO_FORMAT_MESSAGE',
  'upload page must use a single explicit version format message'
)
requireToken(
  uploadPage,
  'const WINDCHILL_VERSION_PATTERN = /^[A-Z]+(?:\\/[1-9]\\d*)+$/i',
  'controlled-file initial version pattern must accept configured multi-segment Windchill versions'
)
assert.match(
  uploadPage,
  /versionNo:\s*\[\s*\{[\s\S]*validator:[\s\S]*VERSION_NO_FORMAT_MESSAGE[\s\S]*trigger:\s*'blur'/,
  'versionNo form rule must reject invalid formats before submit'
)
assert.doesNotMatch(
  uploadPage,
  /versionNo:\s*\[\s*\{[\s\S]*validator:[\s\S]*if \(!isExternalReview\.value\) \{\s*callback\(\)\s*return\s*\}/,
  'ordinary controlled-file uploads must not bypass initial version validation in the form rule'
)

requireToken(
  uploadPage,
  '新文件须使用修订版/1格式；后续大小版本统一通过文件检出、检入生成。',
  'upload page must direct later version changes to checkout/checkin instead of upload revision'
)
assert.doesNotMatch(
  uploadPage,
  /已选择历史文件名称，将按升版提交；当前版本号|按升版提交/,
  'upload page must not promise upload-based revision'
)

const currentVersionPanel = extractBetween(
  uploadPage,
  'data-testid="dcc-upload-current-version-panel"',
  '</el-form-item>',
  'current version panel'
)
assert.match(
  currentVersionPanel,
  /currentVersionLookupError[\s\S]*currentVersionInfo\?\.matched[\s\S]*该逻辑文件已存在；请到文件浏览中检出后再检入新版本。/,
  'current version panel must show lookup errors and block existing logical identities before new upload submission'
)
requireToken(
  currentVersionPanel,
  '请到文件浏览中检出后再检入新版本',
  'existing logical identity copy must direct users to checkout/checkin'
)

const preflightBlock = extractBetween(
  uploadPage,
  'const uploadPreflightChecks = computed',
  'const loadCurrentVersionByFileNumber',
  'upload preflight computed block'
)
for (const token of [
  'currentVersionLookupError.value',
  'existingUploadIdentityBlockReason.value',
  'isVersionNoFormatValid.value',
  'effectiveDatePreflightText.value'
]) {
  requireToken(preflightBlock, token, `preflight must include ${token}`)
}
requireToken(preflightBlock, "label: '生效日期'", 'preflight must include an explicit effective date status card')
requireToken(uploadPage, '允许补录历史生效日期', 'past effective dates must be explicitly described when allowed')

const submitFormStart = uploadPage.indexOf('const submitForm = async () => {')
assert.notEqual(submitFormStart, -1, 'submit form block missing start token')
const submitFormBlock = uploadPage.slice(submitFormStart, submitFormStart + 12000)
assert.match(
  submitFormBlock,
  /await loadCurrentVersionByFileNumber(?:\(\)|\([^)]*\))[\s\S]*currentVersionLookupError\.value[\s\S]*existingUploadIdentityBlockReason\.value/,
  'submit must refresh current-version state and block existing logical identities before sending write request'
)
assert.match(
  submitFormBlock,
  /if \(!isVersionNoFormatValid\.value\) \{[\s\S]*submitFieldErrors\.versionNo = versionFormatPreflightMessage\.value[\s\S]*message\.warning\(versionFormatPreflightMessage\.value\)[\s\S]*return[\s\S]*\}/,
  'submit must block invalid ordinary controlled-file initial versions before sending the write request'
)

for (const token of [
  'Controlled file number conflicts with the existing logical document chain',
  '该文件编号存在版本链冲突',
  'CONTROLLED_FILE_VERSION_INVALID',
  '版本号格式不正确',
  'FILE_VERSION_FIELD_ERROR_PATTERN'
]) {
  requireToken(submitter, token, `submitter must normalize submit error token: ${token}`)
}

console.log('PASS: DCC upload optimization static contract')
