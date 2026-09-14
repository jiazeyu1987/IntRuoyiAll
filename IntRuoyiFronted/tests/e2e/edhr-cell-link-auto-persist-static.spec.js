const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '../..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')

assert.ok(
  !executionPage.includes('BatchRecordCellLinkApi.getPrefill'),
  '执行页不得再调用 /batch-record-cell-link/prefill 把未落库值注入本地草稿。'
)

assert.ok(
  !executionPage.includes('normalizeCellLinkPrefillDraftValue'),
  '执行页不得保留把 cell link prefill 转成 draft 值的路径。'
)

assert.ok(
  !/hydrateDraftState\s*=\s*\([^)]*prefills/s.test(executionPage),
  'hydrateDraftState 的正式输入应来自已保存详情，不应接受 prefills 参数。'
)

assert.ok(
  executionPage.includes('hydrateDraftState(detail)'),
  '执行详情加载后应只用已保存的 detail.cellValues hydrate 草稿状态。'
)

console.log('PASS: eDHR cell link auto-persist frontend static contract')
