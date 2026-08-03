const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const uploadPagePath = path.join(repoRoot, 'src/views/dcc/controlled-file/upload/index.vue')
const categoryApiPath = path.join(repoRoot, 'src/api/dcc/controlledFile/fileCategories.ts')

const uploadPageSource = fs.readFileSync(uploadPagePath, 'utf8')
const categoryApiSource = fs.readFileSync(categoryApiPath, 'utf8')

assert.match(
  categoryApiSource,
  /canUpload\?:\s*boolean/,
  'DCC category API type must expose current-user upload permission as canUpload'
)

assert.match(
  uploadPageSource,
  /const availableCategories = computed\(\(\) =>[\s\S]*selectedFileTypeTaxonomyBoundCategories\.value\.filter\(\(category\) => \{[\s\S]*category\.canUpload === false[\s\S]*return false/,
  'DCC upload page must hide auto-resolved categories where current user lacks category UPLOAD permission'
)

assert.match(
  uploadPageSource,
  /const availableCategories = computed\(\(\) =>[\s\S]*Boolean\(category\.directoryId\)/,
  'DCC upload page must still reject auto-resolved categories without a bound upload directory'
)

assert.match(
  uploadPageSource,
  /if\s*\(category\.canUpload\s*===\s*false\)\s*\{[\s\S]*callback\(new Error\(categoryUploadPermissionMessage\)\)/,
  'DCC upload form validation must reject stale selections with canUpload=false before file upload'
)

console.log('PASS: DCC upload category permission static contract')
