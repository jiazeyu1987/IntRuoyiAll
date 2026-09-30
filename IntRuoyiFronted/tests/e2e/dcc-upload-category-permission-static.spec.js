const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const externalReviewPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/external-review/index.vue')
const taxonomyApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/fileTypeTaxonomies.ts')

const uploadPageSource = fs.readFileSync(uploadPagePath, 'utf8')
const externalReviewPageSource = fs.readFileSync(externalReviewPagePath, 'utf8')
const taxonomyApiSource = fs.readFileSync(taxonomyApiPath, 'utf8')

assert.match(
  taxonomyApiSource,
  /export const getFileTypeTaxonomyUploadOptions[\s\S]*url: '\/dcc\/file-type-taxonomies\/upload-options'/,
  'DCC upload runtime must expose a submit-authorized taxonomy options API distinct from taxonomy management'
)

assert.match(
  uploadPageSource,
  /getProjectCodeFileTemplate[\s\S]*fileTypeTaxonomies\.value = template\.taxonomyOptions \|\| \[\]/,
  'DCC upload page must load taxonomy candidates from the selected project template contract'
)

assert.doesNotMatch(
  uploadPageSource,
  /getFileTypeTaxonomyList/,
  'DCC upload page must not require the taxonomy-management list permission'
)

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
  'DCC upload page must keep the formal category visible so the missing default-directory blocker is explicit'
)

assert.match(
  uploadPageSource,
  /该类别未配置正式默认目录，请联系文控管理员配置后再提交。/,
  'DCC upload page must require formal default-directory configuration before ordinary controlled-file submission'
)

assert.doesNotMatch(
  uploadPageSource,
  /自动提交到未分类目录|按规则发布到“未分类”|自动落位到未分类目录/,
  'DCC upload page must not present missing default-directory configuration as an automatic unclassified landing'
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
