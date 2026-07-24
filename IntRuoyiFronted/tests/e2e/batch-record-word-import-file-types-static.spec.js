const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/batchrecordformlist/index.vue')
const page = fs.readFileSync(pagePath, 'utf8')

assert.match(
  page,
  /const\s+wordImportFileAccept\s*=\s*['"]\.doc,\.docx['"]/,
  'Word 导入文件选择器必须同时允许 .doc 和 .docx。'
)

assert.match(
  page,
  /lowerFileName\.endsWith\('\.doc'\)\s*\|\|\s*lowerFileName\.endsWith\('\.docx'\)/,
  'Word 导入前端文件校验必须同时接受 .doc 和 .docx。'
)

assert.doesNotMatch(
  page,
  /isMainWordImport\.value\s*\?\s*lowerFileName\.endsWith\('\.doc'\)/,
  '主批记录 Word 导入不得单独限制为 .doc。'
)

assert.doesNotMatch(
  page,
  /批记录仅支持选择 \.doc Word 文件/,
  '主批记录 Word 导入错误提示不得继续声称仅支持 .doc。'
)

assert.match(
  page,
  /仅支持选择 \.doc 或 \.docx Word 文件/,
  'Word 导入错误提示必须明确 .doc 和 .docx 都支持。'
)

console.log('PASS: batch-record Word import file type static contract')
