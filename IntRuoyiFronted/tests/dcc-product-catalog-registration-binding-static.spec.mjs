import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const root = resolve(process.cwd())
const page = readFileSync(join(root, 'src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue'), 'utf8')
const relationApi = readFileSync(join(root, 'src/api/dcc/dataRelations.ts'), 'utf8')

assert.match(page, /data-testid="dcc-product-catalog-bind-registration"/)
assert.match(page, /title="绑定项目代码和注册证"/)
assert.match(page, /getProjectCodePage/)
assert.match(page, /getRegistrationCertificatePage/)
assert.match(page, /projectCodeId/)
assert.match(page, /registrationCertificateId/)
assert.match(page, /createDccDataRelation/)
assert.match(page, /await getList\(\)/)
assert.match(relationApi, /url: '\/dcc\/data-relations\/create'/)

console.log('PASS dcc product catalog registration binding contract')
