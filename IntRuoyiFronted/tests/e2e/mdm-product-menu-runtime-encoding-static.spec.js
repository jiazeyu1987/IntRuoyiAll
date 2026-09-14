const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')

function readUtf8(relativePath) {
  const filePath = path.join(repoRoot, relativePath)
  assert.ok(fs.existsSync(filePath), `missing required file: ${filePath}`)
  return fs.readFileSync(filePath, 'utf8')
}

const correctiveMigration = readUtf8(
  'IntRuoyiBackend/sql/mysql/20260728_fix_mdm_product_menu_utf8_name.sql'
)

assert.ok(
  correctiveMigration.includes("UNHEX('E5B195E58E85E4B8BBE695B0E68DAE')"),
  'corrective migration must write 展厅主数据 via UTF-8 hex to avoid mysql client charset mojibake'
)
assert.ok(
  correctiveMigration.includes("`id` = 990201") &&
    correctiveMigration.includes("`permission` = 'mdm:product:query'"),
  'corrective migration must lock the target menu by id and permission'
)
assert.ok(
  correctiveMigration.includes('20260728_rename_mdm_product_menu'),
  'corrective migration must depend on the original menu rename migration'
)

console.log('PASS: MDM product menu runtime encoding migration is charset-safe')
