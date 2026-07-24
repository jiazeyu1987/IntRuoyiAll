const fs = require('fs')
const path = require('path')
const assert = require('assert')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/scheduleorder/index.ts')
const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/scheduleorder/index.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const pageSource = fs.readFileSync(pagePath, 'utf8')

for (const token of ['productionMaterialListCount', 'productionMaterialListSummary']) {
  assert(
    apiSource.includes(token),
    `Schedule order API VO must expose ${token}.`
  )
}

for (const token of [
  '生产用料清单',
  'handleOpenProductionMaterialList',
  'productionMaterialListSummary',
  'productionMaterialListCount',
  '/erp/production/material-list',
  "query: { productionOrderNo: row.erpWorkOrderCode || row.code }",
  'schedule-order-pool__material-missing',
  '缺失'
]) {
  assert(
    pageSource.includes(token),
    `Schedule order page must contain ${token}.`
  )
}

assert(
  /<el-link[\s\S]*?@click="handleOpenProductionMaterialList\(row\)"/.test(pageSource),
  'Schedule order page must render a clickable production material list link.'
)

assert(
  /<span[\s\S]*?schedule-order-pool__material-missing[\s\S]*?>\s*缺失\s*<\/span>/.test(pageSource),
  'Schedule order page must render missing material list status in a dedicated red text span.'
)
