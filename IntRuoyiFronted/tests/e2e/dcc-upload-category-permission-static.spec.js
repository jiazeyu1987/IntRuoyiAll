const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const externalReviewPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/external-review/index.vue')

const uploadPageSource = fs.readFileSync(uploadPagePath, 'utf8')
const externalReviewPageSource = fs.readFileSync(externalReviewPagePath, 'utf8')

const readonlyCategoryBlockMatch = uploadPageSource.match(
  /<el-form-item v-else label="文件类别" prop="categoryId">([\s\S]*?)<el-form-item v-if="uploadDirectoryTree"/
)
assert.ok(readonlyCategoryBlockMatch, 'DCC upload page must keep the readonly file-category form item')
const readonlyCategoryBlock = readonlyCategoryBlockMatch[1]

assert.match(
  uploadPageSource,
  /const availableCategories = computed\(\(\) =>[\s\S]*selectedFileTypeTaxonomyBoundCategories\.value\.filter\(\(category\) => category\.active\)/,
  'DCC upload page must expose every active category without category upload-permission filtering'
)

assert.doesNotMatch(
  uploadPageSource,
  /const availableCategories = computed\(\(\) =>[\s\S]*Boolean\(category\.directoryId\)/,
  'DCC upload page must not reject otherwise uploadable categories without a bound upload directory; backend resolves them to 未分类'
)

assert.match(
  uploadPageSource,
  /未绑定提交目录[\s\S]*未分类目录/,
  'DCC upload page must tell users that an unbound category is automatically landed in 未分类 instead of asking them to bind manually'
)

assert.doesNotMatch(
  uploadPageSource,
  /请先在 DCC 文件类别维护目录绑定/,
  'DCC upload page must not ask submitters to maintain category-directory bindings manually'
)

assert.doesNotMatch(
  uploadPageSource,
  /categoryUploadPermissionMessage|category\.canUpload|分类上传权限|UPLOAD 权限/,
  'DCC upload page must not block or warn during upload based on category UPLOAD permission'
)

assert.doesNotMatch(
  externalReviewPageSource,
  /categoryUploadPermissionMessage|category\.canUpload|UPLOAD 权限/,
  'DCC external-review upload page must not block category selection based on category UPLOAD permission'
)

assert.match(
  readonlyCategoryBlock,
  /data-testid="dcc-upload-category-leaf-display"/,
  'DCC upload page must keep the readonly file-category value'
)

assert.doesNotMatch(
  readonlyCategoryBlock,
  /自动取文件分类最后一级/,
  'DCC upload page must not render the taxonomy path helper below the readonly file category'
)

assert.doesNotMatch(
  readonlyCategoryBlock,
  /<el-alert\b|categoryPermissionPreflightMessage/,
  'DCC upload page must not render the permission preflight alert below the readonly file category'
)

console.log('PASS: DCC upload category permission static contract')
