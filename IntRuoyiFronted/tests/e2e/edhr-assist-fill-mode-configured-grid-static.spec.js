const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const repoRoot = path.resolve(__dirname, '..', '..')
const read = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const executionPage = read('src/views/mes/pro/edhr/ExecutionPage.vue')
const workTaskNavigation = read('src/utils/edhrWorkTaskNavigation.ts')
const batchDetailPage = read('src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue')

assert.match(
  executionPage,
  /const\s+parseAssistGridRowKey\s*=\s*\(rowKey:\s*string\):\s*AssistGridKey\s*\|\s*null\s*=>[\s\S]*ASSIST_GRID_U/,
  '执行页必须解析填写配置生成的个人辅助表格 rowKey。'
)
assert.match(
  executionPage,
  /assistGridRowIndex[\s\S]*assistGridColumnIndex[\s\S]*row\.rowKey/,
  '辅助模式字段必须保留配置的辅助表格行列位置。'
)
assert.match(
  executionPage,
  /edhr-fill-workspace__assist-grid/,
  '填写辅助模式必须按配置的辅助表格渲染网格容器。'
)
assert.match(
  executionPage,
  /data-assist-grid-cell/,
  '每个辅助表格格子必须暴露稳定的配置格子 rowKey。'
)
assert.match(
  executionPage,
  /resolveAssistFieldGridStyle\(field\)/,
  '辅助模式字段必须按配置的辅助表格行列定位，不能只扁平化成列表。'
)
assert.match(
  executionPage,
  /const\s+assistFillFields\s*=\s*computed<AssistFillField\[\]>\(\(\)\s*=>[\s\S]*hasConfiguredAssistGridRows\.value[\s\S]*return\s+sourceFields/,
  '配置为辅助表格时必须保留原始格子顺序和位置，不得先归并为普通列表。'
)

assert.match(
  workTaskNavigation,
  /export\s+const\s+stringifyEdhrExecutionPageQuery[\s\S]*key\s*===\s*'assistRows'[\s\S]*JSON\.stringify\(entryValue\)/,
  '统一工作任务导航必须把 openTask 返回的 assistRows 显式序列化进执行页 query。'
)
assert.match(
  executionPage,
  /stringifyEdhrExecutionPageQuery\(opened\.executionPageQuery\)/,
  '辅助模式工序/填写人切换必须保留后端确认的当前工序 assistRows。'
)
assert.match(
  batchDetailPage,
  /stringifyEdhrExecutionPageQuery\(opened\.executionPageQuery\)/,
  '批次详情打开填写页必须保留后端确认的当前工序 assistRows，不能直接展开原始对象。'
)

console.log('PASS: eDHR assist fill mode configured grid static contract')
