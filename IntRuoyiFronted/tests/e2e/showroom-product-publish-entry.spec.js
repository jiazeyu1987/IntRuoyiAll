const fs = require('fs')
const path = require('path')

const listPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/components/ProductListTable.vue'
)
const listSource = fs.readFileSync(listPath, 'utf8')

if (!listSource.includes("publish: [product: Record<string, unknown>]")) {
  throw new Error(`missing publish emit contract in ${listPath}`)
}

if (!listSource.includes("@click=\"emit('publish', row.raw)\"")) {
  throw new Error(`missing publish action binding in ${listPath}`)
}

const publishButtonMatch = listSource.match(
  /<el-button[\s\S]*?@click="emit\('publish', row\.raw\)"[\s\S]*?>\s*发布\s*<\/el-button>/
)
const deleteButtonMatch = listSource.match(
  /<el-button[\s\S]*?@click="emit\('delete', row\.raw\)"[\s\S]*?>\s*删除\s*<\/el-button>/
)

const publishButtonIndex = publishButtonMatch ? listSource.indexOf(publishButtonMatch[0]) : -1
const deleteButtonIndex = deleteButtonMatch ? listSource.indexOf(deleteButtonMatch[0]) : -1

if (publishButtonIndex === -1) {
  throw new Error(`missing publish action label in ${listPath}`)
}

if (deleteButtonIndex === -1) {
  throw new Error(`missing delete action label in ${listPath}`)
}

if (publishButtonIndex > deleteButtonIndex) {
  throw new Error(`publish action is not placed before delete in ${listPath}`)
}

const indexPath = path.resolve(__dirname, '../../src/views/showroom-admin/index.vue')
const indexSource = fs.readFileSync(indexPath, 'utf8')

if (!indexSource.includes('@publish="handlePublishListProduct"')) {
  throw new Error(`missing product list publish binding in ${indexPath}`)
}

if (!indexSource.includes('const handlePublishListProduct = async (product: Record<string, unknown>) =>')) {
  throw new Error(`missing list publish handler in ${indexPath}`)
}

if (indexSource.includes('保存并发布')) {
  throw new Error(`found obsolete publish text in basic info dialog source: ${indexPath}`)
}

const detailDialogPath = path.resolve(
  __dirname,
  '../../src/views/showroom-admin/product/ProductDetailDialog.vue'
)
const detailDialogSource = fs.readFileSync(detailDialogPath, 'utf8')

if (detailDialogSource.includes('保存并发布')) {
  throw new Error(`found obsolete publish text in detail dialog source: ${detailDialogPath}`)
}

console.log('PASS: showroom product publish entry is split to the list and removed from dialogs')
