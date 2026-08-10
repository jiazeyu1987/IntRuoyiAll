const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) => fs.readFileSync(path.join(repoRoot, relativePath), 'utf8')

const page = readUtf8('src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue')
const api = readUtf8('src/api/mes/pro/processpool/index.ts')
const teamLeaderApi = readUtf8('src/api/mes/pro/processpool/teamLeader.ts')

assert.match(api, /allocationView\?:\s*'WORKBENCH'\s*\|\s*'HISTORY'/)
assert.match(api, /reportAllocations\?:\s*ProcessPoolTimelineReportAllocationVO\[\]/)
assert.match(api, /reportUnallocatedQuantity\?:\s*number/)
assert.match(page, /key:\s*'reportAllocations',\s*label:\s*'分配订单'/)
assert.match(page, /data-team-leader-report-allocations/)
assert.match(page, /:type="item\.released\s*\?\s*'success'\s*:\s*'warning'"[\s\S]*已放行/)
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
assert.match(page, /<el-dialog\s+v-model="reviewVisible"[\s\S]*width="988px"/)
assert.match(page, /<el-table-column\s+label="要生产数量"\s+width="120"[\s\S]*formatAllocationOrderProductionQuantity\(row\)/)
assert.match(page, /<el-table-column\s+label="生产系数"\s+width="110"[\s\S]*formatAllocationOrderProductionCoefficient\(row\)/)
assert.match(teamLeaderApi, /productionCoefficient\?:\s*number\s*\|\s*string/)
assert.match(page, /const\s+formatAllocationOrderProductionQuantity[\s\S]*erpFixedQuantitySnapshot\s*\?\?\s*order\.quantity/)
assert.match(page, /const\s+formatAllocationOrderProductionCoefficient[\s\S]*order\.productionCoefficient/)
assert.doesNotMatch(
  page.match(/const\s+buildAllocationSubmitLines[\s\S]*?\n\}/)?.[0] || '',
  /productionCoefficient|erpFixedQuantitySnapshot|quantity/
)

console.log('PASS: shared report allocation list and edit-lock contract is wired')
