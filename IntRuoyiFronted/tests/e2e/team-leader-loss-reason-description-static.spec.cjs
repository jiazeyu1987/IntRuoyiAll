const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const page = fs.readFileSync(
  path.join(repoRoot, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'),
  'utf8'
)

const lossReasonDisplayBlock = page.match(
  /<div\s+class="team-leader-workbench__loss-reasons"\s+data-team-leader-process-config-loss-reasons>[\s\S]*?<\/div>/
)?.[0]

assert(lossReasonDisplayBlock, 'Unified process config must keep the loss reason display block.')
assert(
  lossReasonDisplayBlock.includes(
    "{{ reason.reasonName }}{{ reason.enabled ? '' : '（停用）' }}"
  ),
  'Loss reason tags must display the formal reason description.'
)
assert.doesNotMatch(
  lossReasonDisplayBlock,
  /\{\{\s*reason\.reasonCode\s*\}\}/,
  'Loss reason tags must not display the internal reason code.'
)

console.log('PASS: team leader loss reason tags display descriptions without codes')
