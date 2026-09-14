const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const root = path.resolve(__dirname, '../..')
const pagePath = path.join(root, 'src/views/erp/production/pick-list/index.vue')
assert(fs.existsSync(pagePath), 'production pick list page must exist: ' + pagePath)

const page = fs.readFileSync(pagePath, 'utf8')

for (const field of [
  ['productionOrderNo', '生产订单'],
  ['stockOrgName', '库存组织'],
  ['productionOrgName', '生产组织']
]) {
  const [key, label] = field
  assert(page.includes(`key: '${key}'`), `filter definition must include ${key}`)
  assert(page.includes(`queryParamKey: '${key}'`), `filter definition must map ${key}`)
  assert(page.includes(`label: '${label}'`), `filter definition must label ${label}`)
  assert(page.includes(`${key}: undefined`), `query model must include ${key}`)
}

for (const forbidden of ['formBindings', 'quickFilter: undefined']) {
  assert(!page.includes(forbidden), 'production pick list filters must not use ' + forbidden)
}

console.log('PASS: ERP production pick list filter fields static contract')
