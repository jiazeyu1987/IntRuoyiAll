const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const tableMarkers = [...page.matchAll(/data-pqc-leader-item-snapshot-table/g)]
assert.equal(tableMarkers.length, 2, 'PQC详情的两个项目明细表入口必须保持一致。')

for (const marker of tableMarkers) {
  const tableStart = page.lastIndexOf('<el-table', marker.index)
  const tableEnd = page.indexOf('</el-table>', marker.index)
  assert.ok(tableStart >= 0 && tableEnd > tableStart, 'PQC项目明细表必须可定位。')
  const tableBlock = page.slice(tableStart, tableEnd)
  const equipmentNumberIndex = tableBlock.indexOf('label="设备编号"')
  const lossQuantityIndex = tableBlock.indexOf('label="损耗数量"')
  assert.ok(
    equipmentNumberIndex >= 0 && lossQuantityIndex > equipmentNumberIndex,
    '损耗数量列必须位于设备编号之后。'
  )
  assert.match(
    tableBlock,
    /label="损耗数量"[\s\S]*?formatPqcItemLossQuantity\(row\)/,
    '损耗数量列必须显示当前检验项目的损耗数量。'
  )
}

assert.match(
  page,
  /interface PqcItemSnapshotDetail[\s\S]*lossQuantity\?: number \| string/,
  'PQC项目快照行必须承载损耗数量。'
)
assert.match(
  page,
  /const resolvePqcItemSnapshotDetails[\s\S]*groupedOriginalPayloadJsons[\s\S]*\[\.\.\.row\.groupedOriginalPayloadJsons,\s*row\.originalPayloadJson\][\s\S]*scrapQuantity/,
  '项目损耗数量必须按原始PQC payload的scrapQuantity映射，不能使用分组汇总字符串。'
)
assert.match(
  page,
  /const formatPqcItemLossQuantity[\s\S]*lossQuantity[\s\S]*\?\? 0[\s\S]*件/,
  '项目损耗数量缺失时必须显示0件。'
)

console.log('PASS: PQC组长项目明细损耗数量静态合同')
