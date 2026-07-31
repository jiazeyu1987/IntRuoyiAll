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
  /const\s+assistGridVisibleColumnIndexes\s*=\s*computed\(\(\)\s*=>[\s\S]*new Set[\s\S]*assistGridColumnIndex[\s\S]*sort/,
  '执行页必须只用存在映射字段的辅助表格列生成可见列集合。'
)
assert.match(
  executionPage,
  /const\s+assistGridColumnDisplayIndexMap\s*=\s*computed\(\(\)\s*=>[\s\S]*new Map<number,\s*number>[\s\S]*assistGridVisibleColumnIndexes\.value\.forEach[\s\S]*map\.set\(columnIndex,\s*displayIndex\)/,
  '执行页必须把原始辅助表格列号压缩成连续的可见列号。'
)
assert.match(
  executionPage,
  /const\s+assistGridColumnCount\s*=\s*computed\(\(\)\s*=>[\s\S]*return\s+assistGridVisibleColumnIndexes\.value\.length/,
  '辅助表格列数必须等于实际存在映射字段的列数，而不是最大原始列号。'
)
assert.match(
  executionPage,
  /assistGridColumnDisplayIndexMap\.value\.get\(field\.assistGridColumnIndex\)/,
  '辅助字段定位必须使用压缩后的可见列号，避免未映射空列占宽。'
)
assert.doesNotMatch(
  executionPage,
  /Math\.max\([\s\S]*assistFillFields\.value\.map\(\(field\)\s*=>\s*Number\(field\.assistGridColumnIndex\)\s*\+\s*1\)/,
  '辅助表格不能再按最大原始列号撑开可见列数。'
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
