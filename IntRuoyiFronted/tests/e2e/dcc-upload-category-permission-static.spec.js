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
  /categories\.value\.filter\(\s*\(category\)\s*=>\s*category\.active\s*&&\s*Boolean\(category\.directoryId\)\s*&&\s*category\.canUpload\s*!==\s*false\s*\)/,
  'DCC upload page must hide categories where current user lacks category UPLOAD permission'
)

assert.match(
  uploadPageSource,
  /if\s*\(category\.canUpload\s*===\s*false\)\s*\{[\s\S]*callback\(new Error\(categoryUploadPermissionMessage\)\)/,
  'DCC upload form validation must reject stale selections with canUpload=false before file upload'
)

console.log('PASS: DCC upload category permission static contract')
