const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const api = read('src/api/mes/pro/edhr/batchExecution.ts')
const detailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')
const traceDrawer = read('src/views/mes/pro/edhr/form-trace/BatchExecutionTraceDrawer.vue')

for (const field of ['pdfaProfile?: string', 'pdfaValidationStatus?: string', 'pdfaValidatedAt?: string']) {
  assert.ok(api.includes(field), `批次归档 API 必须声明 ${field}`)
}

assert.match(detailPage, /const archiveGenerationLoading = ref\(false\)/,
  '归档生成必须有同步加载态，防止重复提交')
assert.match(detailPage, /:loading="archiveGenerationLoading"/,
  '归档生成按钮必须展示加载态')
assert.match(detailPage, /archive\.pdfaValidationStatus !== 'VALID'/,
  '生成成功后必须检查后端 PDF\/A 校验状态')
assert.match(detailPage, /PDF\/A 校验结果缺失或无效/,
  '后端缺少合规结果时前端必须明确失败')

assert.match(traceDrawer, /const isValidPdfAArchive =/,
  '历史追溯必须集中判断 PDF\/A 合规状态')
assert.match(traceDrawer, /archive\.pdfaValidationStatus === 'VALID'/,
  '历史追溯只允许 VALID 归档显示合规状态')
assert.match(traceDrawer, /PDF\/A 未验证/,
  '历史普通 PDF 必须显示中性未验证状态')
assert.doesNotMatch(traceDrawer, /pdfaProfile\s*\|\|\s*['"]PDF\/A-1b['"]/,
  '历史普通 PDF 不得用默认值冒充 PDF/A-1b')

console.log('PASS: eDHR batch PDF/A archive frontend contract')
