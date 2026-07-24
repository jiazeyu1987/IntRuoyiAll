const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

if (!listSource.includes("'toggle-freeze': [product: Record<string, unknown>]")) {
  throw new Error(`missing product freeze emit contract in ${listPath}`)
}

if (!listSource.includes("@click=\"emit('toggle-freeze', row.raw)\"")) {
  throw new Error(`missing product freeze row action binding in ${listPath}`)
}

if (!listSource.includes("row.frozen ? '解冻' : '冻结'")) {
  throw new Error(`missing frozen-state action label in ${listPath}`)
}

if (!listSource.includes("freezingProductId")) {
  throw new Error(`missing product freeze loading state in ${listPath}`)
}

const versionButtonIndex = listSource.indexOf("emit('version-center'")
const freezeButtonIndex = listSource.indexOf("emit('toggle-freeze', row.raw)")
const publishButtonIndex = listSource.indexOf("emit('publish', row.raw)")

if (versionButtonIndex === -1 || freezeButtonIndex === -1 || publishButtonIndex === -1) {
  throw new Error(`missing version/freeze/publish button block in ${listPath}`)
}

if (!(versionButtonIndex < freezeButtonIndex && freezeButtonIndex < publishButtonIndex)) {
  throw new Error(`freeze action must be placed between version center and publish in ${listPath}`)
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes('@toggle-freeze="handleToggleProductFreeze"')) {
  throw new Error(`missing product freeze binding in ${indexPath}`)
}

if (!indexSource.includes('const handleToggleProductFreeze = async (product: Record<string, unknown>) =>')) {
  throw new Error(`missing product freeze handler in ${indexPath}`)
}

if (!indexSource.includes('ShowroomAdminApi.freezeProduct(productId)')) {
  throw new Error(`missing freezeProduct API call in ${indexPath}`)
}

if (!indexSource.includes('ShowroomAdminApi.unfreezeProduct(productId)')) {
  throw new Error(`missing unfreezeProduct API call in ${indexPath}`)
}

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

for (const snippet of [
  "url: '/showroom/product/freeze'",
  "url: '/showroom/product/unfreeze'",
  'freezeProduct: async (productId: number)',
  'unfreezeProduct: async (productId: number)'
]) {
  if (!apiSource.includes(snippet)) {
    throw new Error(`missing product freeze API snippet ${snippet} in ${apiPath}`)
  }
}

console.log('PASS: showroom product freeze action is wired in list, workbench, and API client')
