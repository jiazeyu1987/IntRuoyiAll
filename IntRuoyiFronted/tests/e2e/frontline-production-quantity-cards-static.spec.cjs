const fs = require('fs')
const path = require('path')
const assert = require('assert')

const root = path.resolve(__dirname, '..', '..')
const panelPath = path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const apiPath = path.join(root, 'src/api/mes/pro/feedback/index.ts')

const panel = fs.readFileSync(panelPath, 'utf8')
const api = fs.readFileSync(apiPath, 'utf8')

assert(
  /data-frontline-production-order-quantity/.test(panel),
  'production order top card must expose a production quantity field'
)
assert(
  /生产数量/.test(panel) && /selectedProductionOrderQuantityLabel/.test(panel),
  'production order quantity must be rendered from the selected active order quantity'
)
assert(
  /data-frontline-production-process-submitted-quantity/.test(panel),
  'production process card must expose the current process submitted quantity field'
)
assert(
  /已提交/.test(panel) && /selectedProductionProcessSubmittedQuantityLabel/.test(panel),
  'process submitted quantity must be shown in the process navigation card'
)
assert(
  /submittedQuantity:\s*number/.test(api),
  'FrontlineDeviceRouteProcessVO must include submittedQuantity from the backend contract'
)
assert(
  /selectedProductionProcessSubmittedQuantityLabel[\s\S]{0,400}submittedQuantity/.test(panel),
  'process submitted quantity label must read selectedProcess.submittedQuantity'
)
assert(
  !/selectedProductionProcessSubmittedQuantityLabel[\s\S]{0,400}(outputQuantity|productionOutputQuantity|resolveProductionProgressQuantity)/.test(panel),
  'process submitted quantity must not read the current completion input draft'
)
assert(
  /\.frontline-operator-top\s*\{[\s\S]{0,220}grid-template-columns:\s*minmax\(0,\s*1\.45fr\)\s+minmax\(0,\s*1\.55fr\)\s+minmax\(0,\s*0\.6fr\)\s+240px/.test(panel),
  'production top grid must widen the order card and reduce the employee card'
)
assert(
  /\.frontline-production-employee-card\s*\{[\s\S]{0,160}padding:\s*18px 20px/.test(panel),
  'production employee card must use compact padding after width reduction'
)

console.log('PASS frontline production quantity cards static contract')
