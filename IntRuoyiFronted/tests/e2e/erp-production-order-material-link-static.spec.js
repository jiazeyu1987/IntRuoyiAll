const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const workOrderApiPath = path.resolve(process.cwd(), 'src/api/mes/pro/workorder/index.ts')
const workOrderPagePath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const materialApiPath = path.resolve(process.cwd(), 'src/api/erp/production/material-list/index.ts')
const materialPagePath = path.resolve(process.cwd(), 'src/views/erp/production/material-list/index.vue')

const workOrderApiSource = fs.readFileSync(workOrderApiPath, 'utf8')
const workOrderPageSource = fs.readFileSync(workOrderPagePath, 'utf8')
const materialApiSource = fs.readFileSync(materialApiPath, 'utf8')
const materialPageSource = fs.readFileSync(materialPagePath, 'utf8')

for (const token of [
  'productionMaterialListCount',
  'productionMaterialListSummary'
]) {
  assert(
    workOrderApiSource.includes(token),
    `Production work order API VO must expose ${token}.`
  )
}

for (const token of [
  'productionOrderCount',
  'productionOrderSummary',
  'productionOrderNo',
  'workOrderId',
  'workOrderCode'
]) {
  assert(
    materialApiSource.includes(token),
    `ERP production material list API VO must expose ${token}.`
  )
}

for (const token of [
  '生产用料清单',
  'handleOpenProductionMaterialList',
  'productionMaterialListSummary',
  'productionMaterialListCount',
  "/erp/production/material-list",
  "query: { productionOrderNo: row.code }"
]) {
  assert(
    workOrderPageSource.includes(token),
    `Production work order page must contain ${token}.`
  )
}

assert(
  /<el-link[\s\S]*?@click="handleOpenProductionMaterialList\(scope\.row\)"/.test(workOrderPageSource),
  'Production work order page must render a clickable production material list link.'
)

for (const token of [
  '生产工单',
  'handleOpenWorkOrder',
  'handleOpenGroupWorkOrder',
  'productionOrderSummary',
  'productionOrderCount',
  'workOrderCode',
  '/mes/pro/work-order',
  "query: { code: row.workOrderCode, openId: row.workOrderId }",
  "query: { code: row.productionOrderSummary }"
]) {
  assert(
    materialPageSource.includes(token),
    `ERP production material list page must contain ${token}.`
  )
}

assert(
  /<el-link[\s\S]*?v-if="row\.productionOrderCount === 1 && row\.productionOrderSummary"[\s\S]*?@click="handleOpenGroupWorkOrder\(row\)"/.test(
    materialPageSource
  ),
  'ERP production material list group row must render a clickable work order link when there is exactly one linked work order.'
)

assert(
  /<el-link[\s\S]*?@click="handleOpenWorkOrder\(row\)"/.test(materialPageSource),
  'ERP production material list page must render a clickable work order link.'
)

assert(
  !/catch\s*\{\s*\}/.test(workOrderPageSource) && !/catch\s*\{\s*\}/.test(materialPageSource),
  'Bidirectional link pages must not silently swallow failures.'
)

console.log('PASS: ERP production order/material list bidirectional link static contract')
