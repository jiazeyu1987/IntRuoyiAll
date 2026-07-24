const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')

const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const packageJson = JSON.parse(readSource('package.json'))
const printTemplatePage = readSource('src/views/dcc/controlled-file/print-template/index.vue')

assert.equal(
  packageJson.scripts['e2e:dcc:print-template-placeholder-focus:static'],
  'node tests/e2e/dcc-print-template-placeholder-focus-static.spec.js',
  'package.json must expose the DCC print template placeholder focus static contract'
)

assert.match(
  printTemplatePage,
  /data-testid="dcc-print-template-required-summary"/,
  'print template page must show a required placeholder summary'
)
assert.match(
  printTemplatePage,
  /data-testid="dcc-print-template-placeholder-view-mode"/,
  'print template page must expose a stable placeholder view mode switch'
)
assert.match(
  printTemplatePage,
  /data-testid="dcc-print-template-placeholder-table"/,
  'print template page must expose a stable placeholder table'
)

assert.match(
  printTemplatePage,
  /type PlaceholderViewMode = 'required' \| 'all'/,
  'placeholder view mode type must explicitly distinguish required and all placeholders'
)
assert.match(
  printTemplatePage,
  /const placeholderViewMode = ref<PlaceholderViewMode>\('required'\)/,
  'print template page must default to required placeholders'
)
assert.match(
  printTemplatePage,
  /const requiredPlaceholderRows = computed\(\(\) =>\s*placeholderRows\.filter\(\(item\) => item\.required\)\)/,
  'required placeholder rows must be derived from the declared placeholder rows'
)
assert.match(
  printTemplatePage,
  /const displayedPlaceholderRows = computed\(\(\) =>\s*isAllPlaceholderView\.value\s*\?\s*placeholderRows\s*:\s*requiredPlaceholderRows\.value/,
  'placeholder table must render required rows by default and all rows only in all view'
)
assert.match(
  printTemplatePage,
  /<el-table[\s\S]*:data="displayedPlaceholderRows"[\s\S]*data-testid="dcc-print-template-placeholder-table"/,
  'placeholder table must use displayedPlaceholderRows'
)

for (const label of ['必填占位符', '全部占位符']) {
  assert.match(printTemplatePage, new RegExp(label), `placeholder view switch must show ${label}`)
}

assert.match(
  printTemplatePage,
  /<el-descriptions-item label="模板文件ID">\s*\{\{ activeTemplate\.templateFileId \}\}/,
  'active template metadata must label templateFileId as 模板文件ID'
)
assert.doesNotMatch(
  printTemplatePage,
  /<el-descriptions-item label="文件编号">\s*\{\{ activeTemplate\.templateFileId \}\}/,
  'active template metadata must not label templateFileId as 文件编号'
)

const placeholderFocusMatch = printTemplatePage.match(
  /type PlaceholderViewMode = 'required' \| 'all'[\s\S]*?const resolveErrorMessage/
)
assert.ok(placeholderFocusMatch, 'placeholder view mode logic must stay near placeholder rows')
assert.doesNotMatch(
  placeholderFocusMatch[0],
  /mock|降级|吞异常/i,
  'print template placeholder focus must not introduce mock, downgrade, or swallowed-error behavior'
)

console.log('PASS: DCC print template placeholder focus static contract')
