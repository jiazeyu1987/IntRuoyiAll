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
  /const selectedFileTypeTaxonomyCategoryIds = computed/,
  'upload page must derive DCC category candidates from the selected file taxonomy'
)

assert.match(
  uploadPage,
  /selectedFileTypeTaxonomyCategoryIds\.value\.has\(category\.fileTypeTaxonomyId\)/,
  'upload category options must only include categories bound to the selected file taxonomy'
)

assert.match(
  uploadPage,
  /const resetCategorySelectionForFileTypeTaxonomyChange = \(\) => \{[\s\S]*formData\.categoryId = null[\s\S]*resetUploadDirectoryContext\(\)[\s\S]*resetSelectedPreview\(\)[\s\S]*resetDrawingPdfUpload\(\)/,
  'changing file taxonomy must clear stale category, directory, and upload-preview context'
)

assert.match(
  uploadPage,
  /const handleFileTypeTaxonomyChange = async \(\) => \{[\s\S]*resetCategorySelectionForFileTypeTaxonomyChange\(\)[\s\S]*validateField\?\.\('fileTypeTaxonomyId'\)/,
  'file taxonomy change handler must reset dependent category context before validating taxonomy'
)

assert.doesNotMatch(
  uploadPage,
  /const handleFileTypeTaxonomyChange = async \(\) => \{[\s\S]*refreshUploadNameOptionsForProjectTaxonomy\(\)/,
  'file taxonomy change must not eagerly call upload-name-options before the user opens file name suggestions'
)

console.log('PASS: DCC upload category taxonomy binding static contract')
