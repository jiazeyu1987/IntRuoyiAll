const fs = require('fs')
const path = require('path')

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes("normalizeObject(")) {
  throw new Error(`missing object-based product page parsing entry in ${indexPath}`)
}

if (!indexSource.includes("productPage.total")) {
  throw new Error(`missing product page total field access in ${indexPath}`)
}

if (!indexSource.includes("productPage.list")) {
  throw new Error(`missing product page list field access in ${indexPath}`)
}

if (indexSource.includes('const productRowsPage = normalizeArray(')) {
  throw new Error(`found obsolete array-based product page parsing entry in ${indexPath}`)
}

if (!indexSource.includes("await ShowroomAdminApi.getProductPage(buildProductPageParams(productPageNo.value, productPageSize.value))")) {
  throw new Error(`missing product page request binding in ${indexPath}`)
}

const apiPath = path.resolve(__dirname, '../../src/api/showroom-admin/index.ts')
const apiSource = fs.readFileSync(apiPath, 'utf8')

if (!apiSource.includes('getProductPage: async (params: ShowroomPageQuery): Promise<PageResult<unknown[]>> =>')) {
  throw new Error(`missing page result contract typing for getProductPage in ${apiPath}`)
}

console.log('PASS: showroom product page uses the product page result contract')
