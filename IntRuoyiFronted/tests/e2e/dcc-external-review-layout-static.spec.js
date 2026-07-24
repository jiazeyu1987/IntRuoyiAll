const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractBetween = (source, start, end) => {
  const startIndex = source.indexOf(start)
  assert.notEqual(startIndex, -1, `missing source marker: ${start}`)
  const endIndex = source.indexOf(end, startIndex + start.length)
  assert.notEqual(endIndex, -1, `missing source marker: ${end}`)
  return source.slice(startIndex, endIndex)
}

const packageJson = JSON.parse(readSource('package.json'))
const externalReviewPage = readSource('src/views/dcc/controlled-file/external-review/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:external-review-layout:static'],
  'node tests/e2e/dcc-external-review-layout-static.spec.js',
  'package.json must expose the DCC external review layout static contract'
)

const templateSource = extractBetween(externalReviewPage, '<template>', '<script')
assert.doesNotMatch(
  templateSource,
  /dcc-external-file-review/,
  'external review template must not expose the technical process key'
)
assert.doesNotMatch(
  templateSource,
  /<el-tag[^>]*>\s*dcc-external-file-review\s*<\/el-tag>/,
  'external review header must remove the technical process tag'
)

const sectionContracts = [
  ['dcc-external-review-section-review', '评审信息', ['外来来源', '外来归属', '评审原因', '参与人']],
  ['dcc-external-review-section-file', '文件信息', ['文件类别', '提交目录', '文件名称', '文件编号', '产品编号', '版本号', '生效日期', '提交备注']],
  ['dcc-external-review-section-upload', '附件上传', ['外来文件', '图纸 PDF']],
  ['dcc-external-review-section-submit', '提交操作', ['提交评审']]
]

let previousIndex = -1
for (const [testId, title, labels] of sectionContracts) {
  const sectionIndex = templateSource.indexOf(`data-testid="${testId}"`)
  assert.notEqual(sectionIndex, -1, `missing section ${testId}`)
  assert.ok(sectionIndex > previousIndex, `${testId} must follow the business step order`)
  previousIndex = sectionIndex
  const sectionSource = templateSource.slice(sectionIndex, templateSource.indexOf('</section>', sectionIndex))
  assert.match(sectionSource, new RegExp(title), `${testId} must show title ${title}`)
  for (const label of labels) {
    assert.match(sectionSource, new RegExp(label), `${testId} must contain ${label}`)
  }
}

const requiredBehaviorHooks = [
  'handleCategoryChange',
  'handleFileChange',
  'handleDrawingPdfChange',
  'cleanupCurrentUploadSession',
  'validateDrawingPdfUpload',
  'submitExternalFileReview',
  'submitForm'
]
for (const hook of requiredBehaviorHooks) {
  assert.match(externalReviewPage, new RegExp(hook), `external review behavior must keep ${hook}`)
}

const layoutSource = `${templateSource}\n${extractBetween(externalReviewPage, '<style scoped>', '</style>')}`
assert.doesNotMatch(
  layoutSource,
  /mock|placeholder data|deadline|\bSLA\b|接口造数|fallback|降级|吞异常/i,
  'external review layout must not introduce mock data, deadlines, SLA fields, or fallback behavior'
)

console.log('PASS: DCC external review layout static contract')
