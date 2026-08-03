const fs = require('fs')
const path = require('path')
const assert = require('assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const packageJsonPath = path.join(repoRoot, 'package.json')

const uploadPage = fs.readFileSync(uploadPagePath, 'utf8')
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'))

assert.equal(
  packageJson.scripts['e2e:dcc:upload-category-taxonomy-binding:static'],
  'node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js',
  'package.json must expose the DCC upload category taxonomy binding static contract'
)

assert.match(
  uploadPage,
  /const selectedFileTypeTaxonomyLeafName = computed/,
  'upload page must derive the readonly file category display from the selected file taxonomy leaf'
)

assert.match(
  uploadPage,
  /<el-form-item v-else label="文件类别" prop="categoryId">[\s\S]*data-testid="dcc-upload-category-leaf-display"[\s\S]*selectedFileTypeTaxonomyLeafName/,
  'controlled-file upload category must be a readonly taxonomy leaf display, not a user-editable select'
)

assert.match(
  uploadPage,
  /<el-form-item v-if="isExternalReview" label="文件类别" prop="categoryId">[\s\S]*<el-select[\s\S]*v-model="formData\.categoryId"/,
  'external review must keep the legacy formal category select'
)

assert.match(
  uploadPage,
  /category\.fileTypeTaxonomyId === Number\(formData\.fileTypeTaxonomyId\)/,
  'controlled-file upload must resolve the formal DCC category by exact selected taxonomy leaf id'
)

assert.match(
  uploadPage,
  /const selectedFileTypeTaxonomyAutoCategory = computed/,
  'upload page must compute the unique auto-resolved formal category for the selected taxonomy leaf'
)

assert.doesNotMatch(
  uploadPage,
  /const selectedFileTypeTaxonomyAutoCategory = computed\(\(\) =>[\s\S]*Boolean\(category\.directoryId\)/,
  'auto-resolved formal categories must not require a category-directory binding because unbound categories land in 未分类'
)

assert.match(
  uploadPage,
  /const syncAutoCategoryFromSelectedFileTypeTaxonomy = async \(\) => \{[\s\S]*formData\.categoryId = selectedFileTypeTaxonomyAutoCategory\.value\?\.id \|\| null[\s\S]*await loadUploadDirectoryTree\(formData\.categoryId\)/,
  'taxonomy change must auto-write categoryId only from the unique formal category and load its directory tree'
)

assert.match(
  uploadPage,
  /const resetCategorySelectionForFileTypeTaxonomyChange = \(\) => \{[\s\S]*formData\.categoryId = null[\s\S]*resetUploadDirectoryContext\(\)[\s\S]*resetSelectedPreview\(\)[\s\S]*resetDrawingPdfUpload\(\)/,
  'changing file taxonomy must clear stale category, directory, and upload-preview context'
)

assert.match(
  uploadPage,
  /const handleFileTypeTaxonomyChange = async \(\) => \{[\s\S]*resetCategorySelectionForFileTypeTaxonomyChange\(\)[\s\S]*validateField\?\.\('fileTypeTaxonomyId'\)[\s\S]*await syncAutoCategoryFromSelectedFileTypeTaxonomy\(\)/,
  'file taxonomy change handler must reset dependent context and then auto-sync category from the taxonomy leaf'
)

assert.doesNotMatch(
  uploadPage,
  /const handleFileTypeTaxonomyChange = async \(\) => \{[\s\S]*refreshUploadNameOptionsForProjectTaxonomy\(\)/,
  'file taxonomy change must not eagerly call upload-name-options before the user opens file name suggestions'
)

console.log('PASS: DCC upload category taxonomy binding static contract')
