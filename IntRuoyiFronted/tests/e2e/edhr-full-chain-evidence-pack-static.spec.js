const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const scriptPath = path.join(repoRoot, 'tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js')
const source = fs.readFileSync(scriptPath, 'utf8')

assert.match(
  source,
  /const MIN_DISTINCT_ACTORS = Number\(process\.env\.EDHR_FULL_E2E_MIN_DISTINCT_ACTORS \|\| 4\)/,
  '完整演练默认至少需要 4 个不同真实账号，避免三账号证据冒充职责分离。'
)

for (const token of [
  "require('node:child_process')",
  "require('node:os')",
  'EDHR_FULL_E2E_EVIDENCE_DIR',
  'ensureEvidenceDir',
  'writeEvidenceJson',
  'captureEvidence',
  'run-config.json',
  'final-summary.json',
  'archive-${targetBatchExecutionId}.pdf',
  '01-owner-batch-entry',
  '02-created-or-opened-batch',
  '03-batch-review-page'
]) {
  assert.ok(source.includes(token), `完整演练必须保留证据包能力：${token}`)
}

for (const token of ['EDHR_PDF_TEXT_PYTHON', 'from pypdf import PdfReader', '最终 PDF 文本解析失败']) {
  assert.ok(source.includes(token), `最终 PDF 证据校验必须使用标准 PDF 文本解析并 fail-fast：${token}`)
}

assert.match(
  source,
  /page\.screenshot\(\{ path: screenshotPath, fullPage: true \}\)/,
  '证据包必须保存关键页面截图，不能只依赖控制台 PASS。'
)

console.log('PASS: eDHR full-chain evidence pack static contract')
