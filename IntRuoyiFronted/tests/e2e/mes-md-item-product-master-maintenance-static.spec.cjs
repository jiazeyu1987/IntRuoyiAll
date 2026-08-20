const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')

const form = read('src/views/mes/md/item/MdItemForm.vue')
const api = read('src/api/mes/md/item/index.ts')

assert.ok(
  form.includes('data-md-item-product-master-select'),
  'MES 物料新增、修改和详情必须使用正式 MDM 产品选择器。'
)
assert.ok(
  form.includes('v-model="formData.productMasterId"'),
  'MDM 产品选择器必须持久化唯一正式 productMasterId。'
)
assert.ok(
  form.includes('MdItemApi.getMdmProductOptions()'),
  '物料表单必须从 MES 正式边界加载 MDM 产品候选。'
)
for (const field of ['productCode', 'dccProductCode', 'nameCn']) {
  assert.ok(form.includes(field), `MDM 产品选项必须展示业务可识别字段 ${field}。`)
}
assert.match(api, /productMasterId\?:\s*number/, 'MES 物料写合同必须携带可选 MDM 产品主档 ID。')
assert.ok(
  api.includes('/mes/md/item/mdm-product-options'),
  'MES 物料页面必须通过自身权限边界读取 MDM 产品候选。'
)

console.log('mes-md-item-product-master-maintenance-static.spec: PASS')
