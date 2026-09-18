const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const apiPath = path.join(repoRoot, 'src/api/mes/pro/edhr/labelPrint.ts')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/edhr-label-print/LabelPrintQueuePage.vue')
const routePath = path.join(repoRoot, 'src/router/modules/remaining.ts')
const scheduleTsconfigPath = path.join(repoRoot, 'tsconfig.schedule-relaxed.json')

assert(!fs.existsSync(apiPath), 'eDHR 标签与打印 API 文件必须已经移除。')
assert(!fs.existsSync(pagePath), 'eDHR 标签与打印页面必须已经移除。')

const route = fs.readFileSync(routePath, 'utf8')
const scheduleTsconfig = fs.readFileSync(scheduleTsconfigPath, 'utf8')

for (const forbidden of [
  'pro/feedback/edhr-label',
  'pro/feedback/edhr-print-task',
  'pro/feedback/edhr-print-policy',
  'edhr-label-print',
  'MesProFeedbackEdhrLabelPrint',
  'MesProFeedbackEdhrPrintTask',
  'MesProFeedbackEdhrPrintPolicy',
  'mes:pro-edhr-label:query',
  'mes:pro-edhr-print-task:query',
  'mes:pro-edhr-print-policy:query'
]) {
  assert.doesNotMatch(route, new RegExp(forbidden.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `路由不得保留已下线入口：${forbidden}`)
}

assert.doesNotMatch(scheduleTsconfig, /edhr-label-print/, '定向 TypeScript 配置不得保留已删除页面路径。')

console.log('PASS: eDHR label print UI retirement contract')
