const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/erp/production/material-list/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/erp/production/material-list/index.vue')

assert(fs.existsSync(apiPath), 'ERP production material list API module must exist.')
assert(fs.existsSync(pagePath), 'ERP production material list page must exist.')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of [
  'ErpProductionMaterialListGroupVO',
  'ErpProductionMaterialListDetailVO',
  '/erp/production-material-list/group-page',
  '/erp/production-material-list/detail-list',
  'getGroupPage',
  'getDetailList'
]) {
  assert(apiSource.includes(token), `ERP production material list API must expose ${token}.`)
}

for (const token of [
  'ErpProductionMaterialListApi.getGroupPage',
  'ErpProductionMaterialListApi.getDetailList',
  'openDetailDialog',
  'detailDialogVisible',
  'detailDialogTitle',
  '单据编号',
  '子项数量',
  'ERP修改时间',
  '最后同步时间',
  '子项物料编码',
  '子项物料名称',
  '规格型号',
  '子项类型',
  '分子',
  '分母',
  '子项单位',
  'destroy-on-close'
]) {
  assert(pageSource.includes(token), `ERP production material list page must contain ${token}.`)
}

assert(
  /<el-link[\s\S]*?@click="openDetailDialog\(row\)"/.test(pageSource),
  'ERP production material list bill number must render as a clickable link.'
)

assert(
  /<el-dialog[\s\S]*?v-model="detailDialogVisible"[\s\S]*?:title="detailDialogTitle"/.test(pageSource),
  'ERP production material list page must render a detail dialog bound to detailDialogVisible and detailDialogTitle.'
)

assert(
  /detailList\.value = await ErpProductionMaterialListApi\.getDetailList\(row\.sourceBillNo\)/.test(pageSource),
  'ERP production material list page must lazy load detail rows from the detail API when the bill link is clicked.'
)

assert(
  !/ErpProductionMaterialListApi\.getPage\(/.test(pageSource),
  'ERP production material list page must stop using the old detail page API.'
)

assert(
  !/catch\s*\{\s*\}/.test(pageSource),
  'ERP production material list page must not silently swallow detail loading errors.'
)

console.log('PASS: ERP production material list grouped popup static contract')
