const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = (relativePath) => fs.existsSync(path.join(root, relativePath))

const files = {
  router: 'src/router/modules/remaining.ts',
  api: 'src/api/mes/pro/processpool/fifoOrchestration.ts',
  page: 'src/views/mes/pro/processpool/FifoOrchestrationPage.vue',
  teamLeaderPage: 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'
}

for (const [name, relativePath] of Object.entries(files)) {
  assert.ok(exists(relativePath), `${name} must exist for the FIFO orchestration contract.`)
}

const router = read(files.router)
const api = read(files.api)
const page = read(files.page)
const teamLeaderPage = read(files.teamLeaderPage)

for (const token of [
  "path: 'pro/process-pool/fifo-orchestration'",
  "component: () => import('@/views/mes/pro/processpool/FifoOrchestrationPage.vue')",
  "name: 'MesProProcessPoolFifoOrchestration'",
  "permission: ['mes:pro-process-pool-fifo:allocate']"
]) {
  assert.ok(router.includes(token), `remaining router must include ${token}.`)
}

for (const token of [
  'allocateAvailableProcessPoolOutput',
  '/mes/pro/process-pool/fifo-orchestration/allocate-available-output',
  'targetWorkOrderIds',
  'totalAllocatedQuantity'
]) {
  assert.ok(api.includes(token), `FIFO API wrapper must include ${token}.`)
}

for (const token of [
  "defineOptions({ name: 'MesProProcessPoolFifoOrchestration' })",
  'FIFO 编排',
  '来源工序ID',
  '目标路线工序ID',
  '目标工序ID',
  '目标生产工单ID',
  '执行 FIFO 分配',
  'allocateAvailableProcessPoolOutput',
  'totalAllocatedQuantity',
  'targetWorkOrderCode',
  'allocatedQuantity'
]) {
  assert.ok(page.includes(token), `FIFO orchestration page must include ${token}.`)
}

assert.doesNotMatch(page, /mock|localStorage|sessionStorage|scheduleOrder/i,
  'FIFO orchestration page must use formal production work orders without mocks, storage, or schedule-order fallback.')
assert.doesNotMatch(teamLeaderPage, /allocateAvailableProcessPoolOutput|执行 FIFO 分配/,
  'team leader workbench must remain read-only and separate from FIFO write orchestration.')

console.log('PASS: process-pool FIFO orchestration frontend contract')
