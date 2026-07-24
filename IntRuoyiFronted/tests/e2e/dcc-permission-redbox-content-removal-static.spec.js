const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')

const readSource = (relativePath) => {
  const absolutePath = path.join(root, relativePath)
  assert.equal(fs.existsSync(absolutePath), true, `missing required file: ${relativePath}`)
  return fs.readFileSync(absolutePath, 'utf8')
}

const extractTemplate = (source, tableKey, label) => {
  const pattern = new RegExp(
    `<UnifiedListTemplate[\\s\\S]*?table-key="${tableKey.replaceAll('.', '\\.')}"[\\s\\S]*?<\\/UnifiedListTemplate>`
  )
  const match = source.match(pattern)
  assert.ok(match, `${label} 必须继续使用标准列表模板。`)
  return match[0]
}

const assertActionTextRemoved = (template, label, text) => {
  assert.doesNotMatch(
    template,
    new RegExp(`>\\s*${text}\\s*<`),
    `${label} 必须删除红框中的“${text}”。`
  )
}

const categoriesPage = readSource('src/views/dcc/controlled-file/categories/index.vue')
const reviewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue'
)
const viewMatrixPage = readSource(
  'src/views/dcc/controlled-file/categories/components/CategoryViewMatrixTable.vue'
)
const directoryAuthorizationPage = readSource(
  'src/views/dcc/controlled-file/components/DirectoryAuthorizationTabPanel.vue'
)
const uploadPolicyStaticSpec = readSource('tests/e2e/dcc-upload-size-policy-frontend-static.spec.js')

const categoryTemplate = extractTemplate(
  categoriesPage,
  'dcc.controlledFile.permission.categories',
  '类别列表'
)
const reviewMatrixTemplate = extractTemplate(
  reviewMatrixPage,
  'dcc.controlledFile.permission.reviewMatrix',
  '审阅矩阵'
)
const viewMatrixTemplate = extractTemplate(
  viewMatrixPage,
  'dcc.controlledFile.permission.viewMatrix',
  '查看矩阵'
)
const directoryAuthorizationTemplate = extractTemplate(
  directoryAuthorizationPage,
  'dcc.controlledFile.permission.directoryAuthorization',
  '目录授权'
)
const directoryAuthorizationTemplateOnly = directoryAuthorizationPage.slice(
  0,
  directoryAuthorizationPage.indexOf('</template>')
)

for (const text of ['重置', '刷新列表', '上传大小策略']) {
  assertActionTextRemoved(categoryTemplate, '类别列表', text)
}
assert.match(categoryTemplate, />\s*新增类别\s*</, '类别列表必须保留新增类别入口。')
assert.match(categoryTemplate, />\s*上传策略\s*</, '类别列表必须保留行内上传策略入口。')
assert.match(categoryTemplate, /:show-column-reset="false"/, '类别列表必须删除红框中的重置列入口。')
assert.doesNotMatch(categoryTemplate, /@column-reset=/, '类别列表删除重置列后不应继续绑定列重置事件。')
assert.doesNotMatch(categoriesPage, /openUploadPolicyDialog|uploadPolicyDialogRef|<UploadSizePolicyDialog\b/, '类别列表必须清理红框上传大小策略入口的废弃引用。')
assert.doesNotMatch(categoriesPage, /resetCategoryColumnConfig/, '类别列表删除重置列后不应保留废弃 resetCategoryColumnConfig。')

assert.doesNotMatch(reviewMatrixPage, /第 1 \/ 4 层文控继续固定/, '审阅矩阵必须删除红框顶部说明。')
for (const text of ['重置', '刷新列表']) {
  assertActionTextRemoved(reviewMatrixTemplate, '审阅矩阵', text)
}
assert.match(reviewMatrixTemplate, />\s*按人反查\s*</, '审阅矩阵必须保留按人反查入口。')
assert.match(reviewMatrixTemplate, /:show-column-reset="false"/, '审阅矩阵必须删除红框中的重置列入口。')
assert.doesNotMatch(reviewMatrixTemplate, /@column-reset=/, '审阅矩阵删除重置列后不应继续绑定列重置事件。')
assert.doesNotMatch(reviewMatrixPage, /resetReviewMatrixColumnConfig/, '审阅矩阵删除重置列后不应保留废弃 resetReviewMatrixColumnConfig。')

assert.doesNotMatch(viewMatrixPage, /查看矩阵是发布后浏览/, '查看矩阵必须删除红框顶部说明。')
for (const text of ['重置', '刷新列表']) {
  assertActionTextRemoved(viewMatrixTemplate, '查看矩阵', text)
}
assert.match(viewMatrixTemplate, />\s*按人反查\s*</, '查看矩阵必须保留按人反查入口。')
assert.match(viewMatrixTemplate, /:show-column-reset="false"/, '查看矩阵必须删除红框中的重置列入口。')
assert.doesNotMatch(viewMatrixTemplate, /@column-reset=/, '查看矩阵删除重置列后不应继续绑定列重置事件。')

for (const text of ['重置', '刷新规则']) {
  assertActionTextRemoved(directoryAuthorizationTemplate, '目录授权', text)
}
assert.match(directoryAuthorizationTemplate, />\s*新增目录\s*</, '目录授权必须保留新增目录入口。')
assert.match(directoryAuthorizationTemplate, />\s*新增规则\s*</, '目录授权必须保留新增规则入口。')
assert.match(directoryAuthorizationTemplate, />\s*保存规则\s*</, '目录授权必须保留保存规则入口。')
assert.match(directoryAuthorizationTemplate, /:show-column-reset="false"/, '目录授权必须删除红框中的重置列入口。')
assert.doesNotMatch(directoryAuthorizationTemplate, /@column-reset=/, '目录授权删除重置列后不应继续绑定列重置事件。')
assert.doesNotMatch(directoryAuthorizationPage, /const reloadCurrentRules = /, '目录授权必须清理红框刷新规则入口的废弃方法。')
assert.doesNotMatch(directoryAuthorizationTemplateOnly, /access-rule-header|可看详情|仍需单独授权/, '目录授权必须删除红框目录说明块。')
assert.doesNotMatch(directoryAuthorizationPage, /selectedDirectoryDisplayName|accessRuleHeaderContextText|const selectedDirectory = computed/, '目录授权必须清理目录说明块的废弃计算属性。')

assert.doesNotMatch(
  uploadPolicyStaticSpec,
  /categoryPage\.includes\('UploadSizePolicyDialog'\) && categoryPage\.includes\('上传大小策略'\)/,
  '上传大小策略静态合同必须更新为行内上传策略入口，不得要求已删除的页面级红框按钮。'
)

console.log('PASS: DCC permission red-box content is removed')
