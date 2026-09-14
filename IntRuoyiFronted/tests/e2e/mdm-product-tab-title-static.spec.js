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

const pageSource = readUtf8('IntRuoyiFronted/src/views/mdm/product/index.vue')
const productRealSetupSource = readUtf8('IntRuoyiFronted/tests/e2e/mdm-product-real-setup.e2e.js')
const roleSetupSource = readUtf8('IntRuoyiFronted/tests/e2e/mdm-role-menu-real-setup.e2e.js')
const tenantPackageSetupSource = readUtf8('IntRuoyiFronted/tests/e2e/mdm-tenant-package-real-setup.e2e.js')
const productMenuMigration = readUtf8('IntRuoyiBackend/sql/mysql/20260728_rename_mdm_product_menu.sql')

assert.ok(
  pageSource.includes('基础数据 / 展厅主数据'),
  '产品主数据页面标题必须展示为“基础数据 / 展厅主数据”'
)
assert.ok(
  !pageSource.includes('基础数据 / 产品主数据'),
  '产品主数据页面标题不得继续展示旧页签名“产品主数据”'
)

for (const [label, source] of [
  ['product real setup', productRealSetupSource],
  ['role menu setup', roleSetupSource],
  ['tenant package setup', tenantPackageSetupSource]
]) {
  assert.ok(source.includes('展厅主数据'), `${label} must look up the renamed MDM product menu label`)
}

for (const fragment of [
  'UPDATE `system_menu`',
  '`id` = 990201',
  "`permission` = 'mdm:product:query'",
  '展厅主数据'
]) {
  assert.ok(productMenuMigration.includes(fragment), `menu rename migration must contain ${fragment}`)
}

assert.ok(
  pageSource.includes('新增产品主数据') && pageSource.includes('产品主数据导入'),
  '业务对象和导入导出文案仍应保留“产品主数据”，本任务只改页签/入口名称'
)

console.log('PASS: MDM product tab title is renamed to 展厅主数据 without changing business copy')
