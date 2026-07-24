const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const readSource = (relativePath) => {
  const absolutePath = path.join(repoRoot, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const printTemplatePage = readSource('src/views/dcc/controlled-file/print-template/index.vue')

const extractBetween = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `Missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `Missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

const pageHeader = extractBetween(
  printTemplatePage,
  '<div class="dcc-print-template-page__header">',
  '<div class="dcc-print-template-page__body">'
)
const pageBody = extractBetween(
  printTemplatePage,
  '<div class="dcc-print-template-page__body">',
  '<el-form class="mt-18px"'
)

assert.match(
  pageHeader,
  /data-testid="dcc-print-template-required-context"/,
  '模板配置页头必须提供稳定的必填占位符上下文测试标识'
)

assert.match(
  pageHeader,
  /requiredPlaceholderContextText/,
  '页头必须展示由必填占位符派生的上下文文案'
)

assert.match(
  printTemplatePage,
  /const requiredPlaceholderContextText = computed/,
  '必填占位符上下文必须由 computed 派生，避免重复维护固定清单'
)

assert.match(
  printTemplatePage,
  /requiredPlaceholderRows\.value\.map/,
  '页头上下文必须复用 requiredPlaceholderRows'
)

assert.match(
  printTemplatePage,
  /后端保存时校验 Word 包和必填占位符/,
  '页头上下文必须保留后端校验提示'
)

assert.doesNotMatch(
  pageHeader,
  /<el-alert[\s\S]*模板必须包含/,
  '页头不应继续用独立 info alert 显示必填占位符说明'
)

assert.doesNotMatch(
  pageBody,
  /<el-alert[\s\S]*模板必须包含/,
  '模板上传区域前不应继续显示重复的必填占位符 info alert'
)

for (const behaviorToken of [
  'data-testid="dcc-print-template-required-summary"',
  'data-testid="dcc-print-template-placeholder-view-mode"',
  'data-testid="dcc-print-template-placeholder-table"',
  "const placeholderViewMode = ref<PlaceholderViewMode>('required')",
  'displayedPlaceholderRows',
  'handleFileChange',
  'saveTemplate',
  'resolveUploadedTemplateFile',
  'saveActiveApprovalPrintTemplate'
]) {
  assert.ok(printTemplatePage.includes(behaviorToken), `模板配置原有行为必须保留：${behaviorToken}`)
}

assert.doesNotMatch(
  `${pageHeader}\n${pageBody}`,
  /mock|placeholder data|fallback|降级|吞异常/i,
  '模板配置必填上下文不得引入 mock、fallback、降级或吞异常'
)

console.log('PASS: DCC print template required context static contract')
