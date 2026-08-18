const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/index.ts')
const teamLeaderApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')

assert.match(api, /allocationView\?:\s*'WORKBENCH'\s*\|\s*'HISTORY'/)
assert.match(api, /reportAllocations:\s*ProcessPoolTimelineReportAllocationVO\[\]/)
assert.match(api, /reportUnallocatedQuantity\?:\s*number/)
assert.match(page, /key:\s*'reportAllocations',\s*label:\s*'分配订单'/)
assert.match(page, /data-team-leader-report-allocations/)
assert.match(page, /:type="item\.needsAdjustment\s*\?\s*'danger'\s*:\s*item\.released\s*\?\s*'success'\s*:\s*'warning'"[\s\S]*已放行/)
assert.match(
  page,
  /allocationView:\s*isProductionLeader\.value[\s\S]*isProductionReportHistoryTab\.value[\s\S]*'HISTORY'[\s\S]*'WORKBENCH'/
)
assert.doesNotMatch(
  page.match(/const\s+buildSubmissionParams[\s\S]*?\n\}/)?.[0] || '',
  /isProductionReportHistoryTab\.value[\s\S]*'APPROVED'/
)
assert.match(page, /startBlankAllocation[\s\S]*line\.editable\s*===\s*false/)
assert.match(page, /row\.editable\s*===\s*false[\s\S]*已放行/)
assert.match(page, /const\s+getOrCreateAllocationSaveIdempotencyKey[\s\S]*allocationSaveRequestIdentity/)
assert.match(page, /idempotencyKey:\s*getOrCreateAllocationSaveIdempotencyKey\(/)
assert.doesNotMatch(
  page.match(/confirmTeamLeaderReportAllocation\(\{[\s\S]*?\n\s*\}\)/)?.[0] || '',
  /idempotencyKey:\s*crypto\.randomUUID\(\)/
)
assert.match(page, /PRO_REPORT_ALLOCATION_VERSION_CONFLICT_CODE\s*=\s*1040760357/)
assert.match(page, /isReportAllocationVersionConflict[\s\S]*getCurrentTeamLeaderReportAllocation/)
assert.match(page, /分配版本已更新，已加载最新分配/)
assert.match(page, /<el-dialog\s+v-model="reviewVisible"[\s\S]*width="min\(1480px, calc\(100vw - 24px\)\)"[\s\S]*class="team-leader-workbench__review-dialog"/)
assert.match(page, /data-team-leader-allocation-table[\s\S]*class="team-leader-workbench__allocation-table"[\s\S]*table-layout="fixed"/)
assert.match(page, /<el-table-column\s+label="生产订单号"\s+min-width="145"[\s\S]*class="team-leader-workbench__allocation-order-select"/)
assert.match(page, /<el-table-column\s+label="产品名称"\s+min-width="150"[\s\S]*formatAllocationOrderProductName\(row\)/)
assert.match(page, /<el-table-column\s+label="产品编码"\s+min-width="140"[\s\S]*formatAllocationOrderProductCode\(row\)/)
assert.match(page, /<el-table-column\s+label="订单数量"\s+width="90"[\s\S]*formatAllocationOrderQuantity\(row\)/)
assert.match(page, /<el-table-column\s+label="要生产数量"\s+width="100"[\s\S]*align="right"[\s\S]*formatAllocationOrderProductionQuantity\(row\)/)
assert.match(page, /<el-table-column\s+label="生产系数"\s+width="80"[\s\S]*align="right"[\s\S]*formatAllocationOrderProductionCoefficient\(row\)/)
assert.match(page, /<el-table-column\s+label="分配数量"\s+min-width="270"/)
assert.doesNotMatch(page.match(/data-team-leader-allocation-table[\s\S]*?data-team-leader-allocation-summary/)?.[0] || '', /label="FIFO 剩余"/)
assert.match(page, /<el-table-column\s+label="状态"\s+width="80"[\s\S]*align="center"/)
assert.match(page, /<el-table-column\s+label="操作"\s+width="64"[\s\S]*align="center"/)
assert.match(page, /\.team-leader-workbench__allocation-table[\s\S]*font-size:\s*13px/)
assert.match(page, /\.team-leader-workbench__allocation-quantity-cell[\s\S]*grid-template-columns:\s*minmax\(88px, 1fr\) repeat\(3, max-content\)/)
assert.match(teamLeaderApi, /productionCoefficient\?:\s*number\s*\|\s*string/)
assert.match(page, /const\s+formatAllocationOrderProductionQuantity[\s\S]*erpFixedQuantitySnapshot\s*\?\?\s*order\.quantity/)
assert.match(page, /const\s+formatAllocationOrderProductionCoefficient[\s\S]*order\.productionCoefficient/)
assert.match(
  page.match(/const\s+formatAllocationOrderProductionCoefficient[\s\S]*?\n\}/)?.[0] || '',
  /value\s*===\s*undefined\s*\|\|\s*value\s*===\s*null\s*\|\|\s*String\(value\)\.trim\(\)\s*===\s*''[\s\S]*return\s+'1\.0'/
)
assert.doesNotMatch(
  page.match(/const\s+buildAllocationSubmitLines[\s\S]*?\n\}/)?.[0] || '',
  /productionCoefficient|erpFixedQuantitySnapshot|quantity/
)

console.log('PASS: shared report allocation list and edit-lock contract is wired')
