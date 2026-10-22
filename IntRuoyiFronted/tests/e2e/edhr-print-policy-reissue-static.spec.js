const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/labelPrint.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue')
const routePath = path.join(repoRoot, 'src/router/modules/remaining.ts')
const scheduleTsconfigPath = path.join(repoRoot, 'tsconfig.schedule-relaxed.json')

assert(!fs.existsSync(apiPath), 'eDHR label/print API file must be removed.')
assert(!fs.existsSync(pagePath), 'eDHR label/print page must be removed.')

const route = fs.readFileSync(routePath, 'utf8')
const scheduleTsconfig = fs.readFileSync(scheduleTsconfigPath, 'utf8')

for (const forbidden of [
  'pro/feedback/edhr-print-policy',
  'pro/feedback/edhr-print-task',
  'edhr-label-print',
  'MesProFeedbackEdhrPrintPolicy',
  'MesProFeedbackEdhrPrintTask',
  'mes:pro-edhr-print-policy:query',
  'mes:pro-edhr-print-task:query'
]) {
  assert.doesNotMatch(
    route,
    new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')),
    `route must not retain retired print entry: ${forbidden}`
  )
}

assert.doesNotMatch(
  scheduleTsconfig,
  /edhr-label-print/,
  'schedule TypeScript config must not retain the retired page path.'
)

console.log('PASS: eDHR print policy reissue/void UI retirement contract')
