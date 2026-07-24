const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const pagePath = path.resolve(process.cwd(), 'src/views/mes/pro/feedback/index.vue')
const apiPath = path.resolve(process.cwd(), 'src/api/mes/pro/feedback/index.ts')
const realE2ePath = path.resolve(process.cwd(), 'tests/e2e/mes-feedback-simulated-import-real-flow.e2e.js')

for (const filePath of [pagePath, apiPath, realE2ePath]) {
  assert(fs.existsSync(filePath), `模拟报工多工序相关文件必须存在：${filePath}`)
}

const pageSource = fs.readFileSync(pagePath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const realE2eSource = fs.readFileSync(realE2ePath, 'utf8')

for (const fragment of [
  'promptSimulatedProcessCount',
  '模拟工序数量',
  'SIMULATED_PROCESS_COUNT_MIN',
  'SIMULATED_PROCESS_COUNT_MAX',
  'processCount',
  'ProFeedbackApi.simulateThirdPartyXlsxImport(processCount)'
]) {
  assert(pageSource.includes(fragment), `模拟报工按钮必须输入并传递随机工序数量：${fragment}`)
}

assert(
  /simulateThirdPartyXlsxImport:\s*async\s*\(\s*processCount:\s*number\s*\)/.test(apiSource),
  '模拟导入 API 必须显式接收 processCount。'
)

assert(
  /params:\s*\{\s*processCount\s*\}/.test(apiSource),
  '模拟导入 API 必须把 processCount 作为请求参数发给后端。'
)

assert(!pageSource.includes('catch {}'), '模拟报工多工序入口不得用空 catch 吞掉后端错误。')

for (const fragment of [
  'MES_FEEDBACK_SIMULATED_E2E_PROCESS_COUNT',
  'config.processCount',
  "filter({ hasText: '模拟工序数量' })",
  'simulateBody.data.importedCount, config.processCount',
  'simulateBody.data.pendingCount, config.processCount',
  'importRecordIds.length, config.processCount'
]) {
  assert(realE2eSource.includes(fragment), `真实 E2E 必须按输入 X 个工序验证多条待归属记录：${fragment}`)
}

console.log('PASS: MES feedback simulated import multi-process static contract')
