const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const source = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const activeOrderStart = source.indexOf('data-team-leader-active-order-pool-tab')
assert.notEqual(activeOrderStart, -1, 'Expected the active-order pool tab.')
const activeOrderEnd = source.indexOf('</ContentWrap>', activeOrderStart)
assert.notEqual(activeOrderEnd, -1, 'Expected the active-order pool tab end.')
const activeOrderBlock = source.slice(activeOrderStart, activeOrderEnd)

const stage1Button = activeOrderBlock.match(
  /data-team-leader-simulate-active-order-stage1[\s\S]*?<\/el-button>/
)
assert.ok(stage1Button, 'The active-order list must expose the Stage1 simulation button.')
assert.match(
  stage1Button[0],
  /@click="handleSimulateStage1\(row\)"/,
  'The Stage1 simulation button must keep the independent Stage1 handler.'
)
assert.match(
  stage1Button[0],
  />\s*<Icon icon="ep:refresh" \/>\s*Stage1模拟\s*<\/el-button>/,
  'The Stage1 simulation button must display Stage1模拟.'
)
assert.doesNotMatch(
  activeOrderBlock,
  /data-team-leader-simulate-active-order-completion|handleSimulateActiveOrderCompletion|ep:magic-stick|>\s*模拟完成\s*<\/el-button>/,
  'The active-order list must not render the generic magic-wand simulation button.'
)

console.log('PASS: team-leader active-order simulation button static contract')
