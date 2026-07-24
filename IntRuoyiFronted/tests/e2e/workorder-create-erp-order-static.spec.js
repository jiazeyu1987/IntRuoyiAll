const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/workorder/index.ts')
const listPath = path.resolve(process.cwd(), 'src/views/mes/pro/workorder/index.vue')
const configApiPath = path.resolve(process.cwd(), 'src/api/erp/config/index.ts')
const configPagePath = path.resolve(process.cwd(), 'src/views/erp/config/index.vue')

const apiSource = fs.readFileSync(apiPath, 'utf8')
const listSource = fs.readFileSync(listPath, 'utf8')
const configApiSource = fs.readFileSync(configApiPath, 'utf8')
const configPageSource = fs.readFileSync(configPagePath, 'utf8')

assert(
  apiSource.includes('KingdeeProductionOrderCreateRespVO') &&
    apiSource.includes('createKingdeeProductionOrder') &&
    apiSource.includes('/create-kingdee-production-order'),
  'Production work order API must expose createKingdeeProductionOrder endpoint and response VO.'
)

assert(
  listSource.includes('创建 ERP 测试单') &&
    listSource.includes('v-if="isAdminUser"') &&
    listSource.includes("mes:pro-work-order:create-erp") &&
    listSource.includes('erpCreateLoadingId') &&
    listSource.includes('确认根据生产工单'),
  'Production work order list must render an admin-only test-clone ERP action with row-level loading and explicit confirmation.'
)

assert(
  listSource.includes('ERP 测试生产订单已创建') &&
    !listSource.includes('catch {}'),
  'Production work order test ERP action must show real success and must not silently swallow backend failures.'
)

assert(
  configApiSource.includes('templateBillNo') &&
    configPageSource.includes('生产订单模板单号') &&
    configPageSource.includes('productionOrder.templateBillNo'),
  'ERP config production tab must expose required productionOrder.templateBillNo.'
)

console.log('PASS: production work order create ERP order static contract')
