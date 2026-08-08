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
  '该类别未配置专属目录，按规则发布到“未分类”。',
  'unclassified directory landing must be shown as an allowed business rule'
)
assert.doesNotMatch(
  uploadPage,
  /系统将自动提交到未分类目录/,
  'unclassified landing copy must not look like an abnormal fallback'
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
  'const VERSION_NO_PATTERN = /^[Vv]?\\d+(?:\\.\\d+)*$/',
  'frontend version pattern must match backend parser shape: optional V + numeric dot segments'
)
assert.match(
  uploadPage,
  /versionNo:\s*\[\s*\{[\s\S]*validator:[\s\S]*VERSION_NO_FORMAT_MESSAGE[\s\S]*trigger:\s*'blur'/,
  'versionNo form rule must reject invalid formats before submit'
)

requireToken(
  uploadPage,
  '已选择历史文件名称，系统将先匹配现行主档；匹配成功后按升版提交',
  'history file copy must not promise revision before current master is matched'
)
assert.doesNotMatch(
  uploadPage,
  /已选择历史文件名称，将按升版提交；当前版本号/,
  'old unconditional revision promise must be removed'
)

const currentVersionPanel = extractBetween(
  uploadPage,
  'data-testid="dcc-upload-current-version-panel"',
  '</el-form-item>',
  'current version panel'
)
assert.match(
  currentVersionPanel,
  /currentVersionLookupError[\s\S]*currentVersionInfo\?\.matched[\s\S]*isRevisionUpload/,
  'current version panel must show lookup error first, matched master second, and revision-only block instead of new master'
)
requireToken(
  currentVersionPanel,
  '不会创建新的 master 主档',
  'revision failure must explicitly say it will not create a new master'
)

const preflightBlock = extractBetween(
  uploadPage,
  'const uploadPreflightChecks = computed',
  'const loadCurrentVersionByFileNumber',
  'upload preflight computed block'
)
for (const token of [
  'currentVersionLookupError.value',
  'revisionTargetPreflightBlockReason.value',
  'isVersionNoFormatValid.value',
  'effectiveDatePreflightText.value'
]) {
  requireToken(preflightBlock, token, `preflight must include ${token}`)
}
requireToken(preflightBlock, "label: '生效日期'", 'preflight must include an explicit effective date status card')
requireToken(uploadPage, '允许补录历史生效日期', 'past effective dates must be explicitly described when allowed')

const submitFormBlock = extractBetween(
  uploadPage,
  'const submitForm = async () => {',
  'watch(\n  () => formData.versionNo',
  'submit form block'
)
assert.match(
  submitFormBlock,
  /await loadCurrentVersionByFileNumber\(\)[\s\S]*currentVersionLookupError\.value[\s\S]*revisionTargetPreflightBlockReason\.value/,
  'submit must refresh current-version state and block revision conflicts before sending write request'
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
