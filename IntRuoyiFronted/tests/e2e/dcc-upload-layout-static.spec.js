const fs = require('fs')
const path = require('path')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const packageJsonPath = path.join(repoRoot, 'package.json')

const uploadPage = fs.readFileSync(uploadPagePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const extractBetween = (source, startToken, endToken) => {
  const startIndex = source.indexOf(startToken)
  const endIndex = source.indexOf(endToken, startIndex + startToken.length)
  assert(startIndex >= 0 && endIndex > startIndex, `无法提取 ${startToken} 到 ${endToken} 内容`)
  return source.slice(startIndex, endIndex)
}

const template = extractBetween(uploadPage, '<template>', '<script')
const style = extractBetween(uploadPage, '<style scoped>', '</style>')

assert(
  packageJson.scripts['e2e:dcc:upload-layout:static'] ===
    'node tests/e2e/dcc-upload-layout-static.spec.js',
  'package.json 必须提供 e2e:dcc:upload-layout:static 脚本'
)

assert(!template.includes('label="绑定目录"'), '受控文件提交页不应额外显示独立“绑定目录”表单行')
assert(template.includes('label="提交目录"'), '受控文件提交页必须保留“提交目录”决策点')

const expectedSections = [
  {
    testId: 'dcc-upload-section-scope',
    className: 'upload-section--scope',
    title: '提交范围',
    labels: ['文件类别', '提交目录']
  },
  {
    testId: 'dcc-upload-section-file',
    className: 'upload-section--file',
    title: '文件信息',
    labels: ['文件名称', '文件编号', '产品编号', '版本号', '生效日期', '提交备注']
  },
  {
    testId: 'dcc-upload-preflight-panel',
    className: 'upload-section--preflight',
    title: '提交前校验',
    labels: [],
    texts: ['文件编号/版本 · 文件类别 · 审批人链路 · 受控浏览目录 · 浏览权限范围']
  },
  {
    testId: 'dcc-upload-section-approval',
    className: 'upload-section--approval',
    title: '审批要求',
    labels: ['培训要求', '会签人员']
  },
  {
    testId: 'dcc-upload-section-attachment',
    className: 'upload-section--attachment',
    title: '附件上传',
    labels: ['受控文件', '图纸 PDF']
  },
  {
    testId: 'dcc-upload-section-submit',
    className: 'upload-submit-bar',
    labels: [],
    texts: ['submitButtonText']
  }
]

assert(
  template.includes('data-testid="dcc-upload-single-page-workbench"'),
  '上传页必须使用单页工作台容器承载全部内容'
)
assert(
  template.includes('data-testid="dcc-upload-single-page-grid"'),
  '上传页必须使用单页网格承载提交范围、文件信息、审批要求和附件上传'
)
assert(
  template.includes('data-testid="dcc-upload-left-column"'),
  '上传页必须提供左侧主列承载提交范围、文件信息和提交前校验'
)
assert(
  template.includes('data-testid="dcc-upload-right-column"'),
  '上传页必须提供右侧附件列承载审批要求和附件上传'
)

const assertSectionOrder = (source, sectionIds, scopeName) => {
  let previousIndex = -1
  for (const sectionId of sectionIds) {
    const sectionIndex = source.indexOf(`data-testid="${sectionId}"`)
    assert(sectionIndex > previousIndex, `${scopeName} 分组缺失或顺序错误：${sectionId}`)
    previousIndex = sectionIndex
  }
}

const leftColumnStart = template.indexOf('data-testid="dcc-upload-left-column"')
const rightColumnStart = template.indexOf('data-testid="dcc-upload-right-column"')
const submitSectionStart = template.indexOf('data-testid="dcc-upload-section-submit"')
assert(
  leftColumnStart >= 0 && rightColumnStart > leftColumnStart,
  '提交前校验必须位于左侧主列，不能排在右侧附件预览之后'
)
assert(submitSectionStart > rightColumnStart, '提交按钮区必须仍位于双列工作台之后')

const leftColumn = template.slice(leftColumnStart, rightColumnStart)
const rightColumn = template.slice(rightColumnStart, submitSectionStart)
assertSectionOrder(
  leftColumn,
  ['dcc-upload-section-scope', 'dcc-upload-section-file', 'dcc-upload-preflight-panel'],
  '左侧主列'
)
assertSectionOrder(
  rightColumn,
  ['dcc-upload-section-approval', 'dcc-upload-section-attachment'],
  '右侧附件列'
)
assert(
  !rightColumn.includes('data-testid="dcc-upload-preflight-panel"'),
  '提交前校验不得留在右侧附件预览流中'
)
assert(
  !leftColumn.includes('data-testid="dcc-upload-section-attachment"'),
  '附件上传不得进入左侧主列挤占提交前校验位置'
)

let lastIndex = -1
for (const section of expectedSections) {
  const testIdIndex = template.indexOf(`data-testid="${section.testId}"`)
  assert(testIdIndex > lastIndex, `缺少分组或顺序错误：${section.testId}`)
  lastIndex = testIdIndex
  assert(template.includes(section.className), `分组必须具备单页排版类名：${section.className}`)
  if (section.title) {
    assert(template.includes(`upload-section__title">${section.title}`), `缺少分组标题：${section.title}`)
  }
  for (const label of section.labels || []) {
    assert(template.includes(`label="${label}"`), `缺少字段标签：${label}`)
  }
  for (const text of section.texts || []) {
    assert(template.includes(text), `缺少分组内操作文本：${text}`)
  }
}

const behaviorHooks = [
  'handleCategoryChange',
  'loadUploadDirectoryTree',
  'queryUploadNameSuggestions',
  'handleHistoryFileNameSelect',
  'handleProjectCodeChange',
  'handleFileChange',
  'handleDrawingPdfChange',
  'cleanupCurrentUploadSession',
  'validateDrawingPdfUpload',
  'submitControlledFile',
  'submitForm'
]

for (const hook of behaviorHooks) {
  assert(uploadPage.includes(hook), `上传页必须保留行为函数或依赖：${hook}`)
}

for (const removed of [
  '预览路线',
  '审批路线预览',
  '请选择文件类别后预览审批路线',
  'handleRoutePreview',
  'routePreviewLoading',
  'previewRows',
  'previewControlledFileRoute'
]) {
  assert(!uploadPage.includes(removed), `上传页必须移除路线预览黄框内容：${removed}`)
}

const requiredLayoutRules = [
  '.upload-workbench',
  '.upload-workbench__grid',
  '.upload-workbench__column',
  'grid-template-columns: minmax(0, 1fr) minmax(0, 1fr)',
  '.upload-submit-bar',
  'margin-bottom: 10px',
  'width: 100% !important',
  '@media (max-width: 1280px)'
]

for (const rule of requiredLayoutRules) {
  assert(style.includes(rule), `上传页缺少单页紧凑排版样式：${rule}`)
}

assert(
  !style.includes('grid-template-areas:'),
  '提交前校验不能继续依赖共享网格行，否则长附件预览会把左侧校验区向下推'
)

const forbiddenTerms = ['mock', 'placeholder data', 'fallback', '降级', '吞异常']
for (const term of forbiddenTerms) {
  assert(!uploadPage.toLowerCase().includes(term.toLowerCase()), `上传布局优化不得引入 ${term}`)
}

console.log('DCC upload layout static contract passed.')
