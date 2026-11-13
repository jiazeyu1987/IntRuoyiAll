const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const root = path.resolve(__dirname, '../..')
const page = fs.readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
assert.doesNotMatch(page, /applyWithNoReplenishmentConfirmation|确认无补料信息/)
assert.match(page, /result = await applyTeamLeaderActiveOrderRelease\(/)
// Execute the actual submission handler, including its single-confirmation success scenario.
const { spawnSync } = require('node:child_process')
const result = spawnSync(process.execPath, ['--test', '--test-name-pattern=actual leader no-replenishment',
  path.join(root, 'tests/approval-center-invalidation-round1.test.mjs')], { stdio: 'inherit' })
assert.equal(result.status, 0, '无补料订单应在主确认后一次提交成功')
