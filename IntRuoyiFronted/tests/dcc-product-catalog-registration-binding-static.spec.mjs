import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const root = resolve(process.cwd())
const page = readFileSync(join(root, 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'), 'utf8')
const relationApi = readFileSync(join(root, 'src/api/dcc/dataRelations.ts'), 'utf8')
const productCatalogApi = readFileSync(join(root, 'src/api/dcc/controlledFile/productCatalog.ts'), 'utf8')

assert.match(page, /data-testid="dcc-product-catalog-bind-registration"/)
assert.match(page, /const PRODUCT_CATALOG_ACTION_PANEL_WIDTH = 540/)
assert.match(page, /getProductCatalogActionColumnWidthString/)
assert.match(page, /Math\.max\(configuredWidth, PRODUCT_CATALOG_ACTION_PANEL_WIDTH\)/)
assert.match(
  page,
  /\{\s*key: 'actions',\s*label: '操作',\s*width: PRODUCT_CATALOG_ACTION_PANEL_WIDTH,\s*hideable: false,\s*business: false\s*\}/
)
assert.match(page, /title="绑定项目代码和注册证"/)
assert.match(page, /getProjectCodePage/)
assert.match(page, /getRegistrationCertificatePage/)
assert.match(page, /projectCodeId/)
assert.match(page, /registrationCertificateId/)
assert.match(page, /createDccDataRelation/)
assert.match(page, /await getList\(\)/)
assert.match(page, /const router = useRouter\(\)/)
assert.match(page, /data-testid="dcc-product-catalog-project-code-link"/)
assert.match(page, /@click="openLinkedProjectCode\(row\)"/)
assert.match(page, /path: '\/mes\/md\/dcc-project-code'/)
assert.match(page, /query:\s*\{\s*projectCodeId: String\(row\.projectCodeId\)\s*\}/)
assert.match(page, /data-testid="dcc-product-catalog-registration-certificate-link"/)
assert.match(page, /@click="openLinkedRegistrationCertificate\(row\)"/)
assert.match(page, /path: '\/mdm\/registration-certificate\/detail\/' \+ String\(row\.registrationCertificateId\)/)
assert.match(page, /projectCodeId: row\.projectCodeId \? String\(row\.projectCodeId\) : undefined/)
assert.match(relationApi, /url: '\/dcc\/data-relations\/create'/)
assert.match(productCatalogApi, /projectCodeId\?: number \| string \| null/)
assert.match(productCatalogApi, /registrationCertificateId\?: number \| string \| null/)

console.log('PASS dcc product catalog registration binding contract')
