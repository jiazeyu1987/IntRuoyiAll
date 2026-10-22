const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const pagePath = path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const page = fs.readFileSync(pagePath, 'utf8').replace(/\r\n/g, '\n')

const detailMarker = 'data-pqc-leader-detail-tab'
const detailMarkerStart = page.indexOf(detailMarker)
assert.ok(detailMarkerStart >= 0, 'PQC详情页签必须保留稳定标记。')

const detailTabStart = page.lastIndexOf('<ContentWrap', detailMarkerStart)
const detailTabEnd = page.indexOf('</ContentWrap>', detailMarkerStart)
assert.ok(
  detailTabStart >= 0 && detailTabEnd > detailTabStart,
  'PQC详情页签内容块必须可定位。'
)

const detailTabBlock = page.slice(detailTabStart, detailTabEnd)
assert.match(
  detailTabBlock,
  /<el-descriptions-item[\s\S]*label="损耗数量"[\s\S]*resolveSubmissionLossQuantity\(detail\)/,
  'PQC详情必须显示损耗数量，并复用PQC提交损耗数量解析器。'
)

const lossResolverStart = page.indexOf('const resolveSubmissionLossQuantityValue')
const lossResolverEnd = page.indexOf('const resolveSubmissionLossQuantity =', lossResolverStart)
assert.ok(
  lossResolverStart >= 0 && lossResolverEnd > lossResolverStart,
  'PQC损耗数量解析器必须可定位。'
)
const lossResolverBlock = page.slice(lossResolverStart, lossResolverEnd)
assert.match(
  lossResolverBlock,
  /isPqcSubmissionRow\(row\)[\s\S]*\['scrapQuantity', 'lossQuantity', 'SCRAP_QUANTITY'\]/,
  'PQC损耗数量必须优先读取 scrapQuantity 正式字段。'
)

console.log('PASS: PQC组长详情显示损耗数量静态合同')
