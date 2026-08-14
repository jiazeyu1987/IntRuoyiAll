const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '..', '..')
const detailPage = fs.readFileSync(
  path.join(repoRoot, 'src/views/dcc/controlled-file/detail/index.vue'),
  'utf8'
)
const packageJson = JSON.parse(fs.readFileSync(path.join(repoRoot, 'package.json'), 'utf8'))

function extractBetween(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker)
  assert.notEqual(start, -1, `missing start marker: ${startMarker}`)
  const end = source.indexOf(endMarker, start)
  assert.notEqual(end, -1, `missing end marker: ${endMarker}`)
  return source.slice(start, end)
}

const signatureSection = extractBetween(
  detailPage,
  'data-testid="dcc-detail-signature-section"',
  '</ContentWrap>'
)
const signatureLoader = extractBetween(
  detailPage,
  'const loadDccSignatureEvidenceList = async () => {',
  'const loadAccessExplanationOnly = async () => {'
)
const reloadAll = extractBetween(detailPage, 'const reloadAll = async () => {', 'const formatAccessExplanation')

assert.equal(
  packageJson.scripts['e2e:dcc:detail-signature-evidence-nonblocking:static'],
  'node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js',
  'package.json must expose the focused signature evidence non-blocking contract'
)
assert.match(
  detailPage,
  /const dccSignatureEvidenceError = ref\(''\)/,
  'detail page must keep an explicit signature evidence error state'
)
assert.match(
  signatureSection,
  /<el-alert[\s\S]*v-if="dccSignatureEvidenceError"[\s\S]*:title="dccSignatureEvidenceError"/,
  'signature evidence failures must be visible in the signature section'
)
assert.match(
  signatureLoader,
  /checkPermi\(\['dcc:controlled-file:signature:manage'\]\)/,
  'signature evidence loader must check the backend permission before calling the management API'
)
assert.match(
  signatureLoader,
  /dccSignatureEvidenceError\.value\s*=\s*'当前可查看签核追溯摘要；高级签名留痕需 DCC 电子签名管理权限。'[\s\S]*dccSignatureEvidenceList\.value\s*=\s*\[\][\s\S]*dccSignatureEvidenceTotal\.value\s*=\s*0/,
  'missing signature management permission must show an explicit read-side error and clear only the signature table'
)
assert.match(
  signatureLoader,
  /catch \(error\) \{[\s\S]*dccSignatureEvidenceList\.value\s*=\s*\[\][\s\S]*dccSignatureEvidenceTotal\.value\s*=\s*0[\s\S]*dccSignatureEvidenceError\.value\s*=\s*resolveReadSideErrorMessage\(\s*error,/,
  'signature evidence API failures must remain visible instead of being silently swallowed'
)
assert.doesNotMatch(
  signatureLoader,
  /throw error|Promise\.reject/,
  'signature evidence read-side failure must not stop approval task loading'
)
assert.match(
  reloadAll,
  /await loadData\(\)[\s\S]*await loadDccSignatureEvidenceList\(\)[\s\S]*await loadApprovalDetail\(\)/,
  'reloadAll must continue to load approval detail after the signature evidence loader handles its own visible error'
)

console.log('PASS: DCC detail signature evidence non-blocking static contract')
