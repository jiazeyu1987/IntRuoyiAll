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

const p1Button = activeOrderBlock.match(
  /data-team-leader-simulate-active-order-stage1-p1[\s\S]*?<\/el-button>/
)
assert.ok(p1Button, 'The active-order list must expose the P1 Stage1 simulation button.')
assert.match(
  p1Button[0],
  /@click="handleSimulateStage1\(row\)"/,
  'The P1 button must keep the independent Stage1 handler.'
)
assert.match(
  p1Button[0],
  />\s*<Icon icon="ep:refresh" \/>\s*P1双100\s*<\/el-button>/,
  'The P1 button must display P1双100.'
)
const p2Button = activeOrderBlock.match(
  /data-team-leader-generate-active-order-stage1-p2[\s\S]*?<\/el-button>/
)
assert.ok(p2Button, 'The active-order list must expose the P2 generated form button.')
assert.match(
  p2Button[0],
  /@click="handleGenerateStage1Forms\(row\)"/,
  'The P2 button must use the generated-form handler.'
)
assert.match(
  p2Button[0],
  />\s*<Icon icon="ep:document" \/>\s*P2生成\s*<\/el-button>/,
  'The P2 button must display P2生成.'
)
assert.doesNotMatch(
  activeOrderBlock,
  /data-team-leader-simulate-active-order-completion|handleSimulateActiveOrderCompletion|ep:magic-stick|>\s*Stage1模拟\s*<\/el-button>|>\s*模拟完成\s*<\/el-button>/,
  'The active-order list must not render the generic magic-wand simulation button.'
)

console.log('PASS: team-leader active-order simulation button static contract')
