const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const api = fs.readFileSync(path.join(repoRoot, 'src/api/mes/pro/processpool/teamLeader.ts'), 'utf8')
const page = fs.readFileSync(path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')

assert.match(api, /resetFixedSimulationActiveOrder/)
assert.match(api, /active-order\/simulation\/test-reset/)
assert.match(page, /data-team-leader-reset-fixed-active-order/)
assert.match(page, /重置指定测试订单/)
const resetHandler = page.split('const handleResetFixedSimulationActiveOrder = async () => {')[1].split('const handleActiveOrderWorkOrderKeywordChange')[0]
assert.doesNotMatch(resetHandler, /ElMessageBox|targetCode/, 'reset must execute directly without confirmation or order-code checking')
assert.match(resetHandler, /await resetFixedSimulationActiveOrder\(\)/)
assert.match(page, /列表刷新失败/)
const resetButtonIndex = page.indexOf('data-team-leader-reset-fixed-active-order')
const activeOrderTableIndex = page.indexOf('data-team-leader-active-order-list')
assert.ok(resetButtonIndex >= 0 && resetButtonIndex < activeOrderTableIndex, 'reset button must be above the active-order list')
assert.match(page.slice(Math.max(0, resetButtonIndex - 300), resetButtonIndex), /mes:pro-process-pool-team-leader:maintain/)

console.log('active-order-test-reset-static: PASS')
