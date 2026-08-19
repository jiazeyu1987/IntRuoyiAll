const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
const api = fs.readFileSync(path.join(root, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')

assert.match(api, /previewTeamLeaderActiveOrderRebuild/, 'API wrapper must expose rebuild preview')
assert.match(api, /rebuildTeamLeaderActiveOrder/, 'API wrapper must expose confirmed rebuild')
assert.match(api, /\/active-order\/rebuild\/preview/, 'preview endpoint path must be wired')
assert.match(api, /\/active-order\/rebuild/, 'rebuild endpoint path must be wired')

const activeOrderColumns = page.slice(
  page.indexOf('data-user-table-key="mes.pro.processPool.teamLeader.activeOrders"'),
  page.indexOf('title="新增活跃订单"')
)
assert.match(activeOrderColumns, />\s*重建\s*</, 'active order row operation area must render rebuild button')
assert.match(activeOrderColumns, /handleRebuildActiveOrder/, 'rebuild button must call the row handler')
assert.ok(
  activeOrderColumns.indexOf('data-team-leader-rebuild-active-order')
    > activeOrderColumns.indexOf('data-team-leader-active-order-release-apply'),
  'rebuild button must be rendered to the right of the complete action'
)

const handler = page.slice(
  page.indexOf('const handleRebuildActiveOrder'),
  page.indexOf('const handleRemoveActiveOrder')
)
assert.match(handler, /previewTeamLeaderActiveOrderRebuild/, 'handler must preview before destructive rebuild')
assert.match(handler, /hasHistoricalRuntimeData/, 'handler must branch on historical runtime data')
assert.match(handler, /报工记录/, 'destructive confirmation must mention production reports')
assert.match(handler, /生产进度/, 'destructive confirmation must mention production progress')
assert.match(handler, /PQC 检验结果/, 'destructive confirmation must mention PQC results')
assert.match(handler, /生产快照/, 'confirmation must mention production snapshots')
assert.match(handler, /PQC 快照/, 'confirmation must mention PQC snapshots')
assert.match(handler, /rebuildTeamLeaderActiveOrder/, 'handler must call confirmed rebuild endpoint')
assert.match(handler, /await loadActiveOrders\(\)/, 'successful rebuild must refresh active order list')

console.log('PASS: production leader active-order rebuild frontend contract is explicit')
