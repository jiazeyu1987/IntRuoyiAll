const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const source = fs
  .readFileSync(path.join(root, 'src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue'), 'utf8')
  .replace(/\r\n/g, '\n')

assert.doesNotMatch(
  source,
  /PQC_SUBMISSION_CONTENT_DEFINITIONS[\s\S]*length[\s\S]*appearance[\s\S]*seal[\s\S]*pressure/,
  'PQC leader page must not use fixed length/appearance/seal/pressure as the formal detail path.'
)
assert.match(
  source,
  /resolvePqcItemSnapshotDetails/,
  'PQC leader page must resolve dynamic item snapshot details from submitted PQC payload.'
)
assert.match(
  source,
  /pqcItemDetails|itemResults/,
  'PQC leader page must prefer structured pqcItemDetails/itemResults before raw payload fragments.'
)
assert.ok(
  source.includes('data-pqc-leader-item-snapshot-table'),
  'PQC leader detail drawer must expose an item snapshot table.'
)
for (const label of ['检验项目', '检验设备', '设备编号', '接收标准', '检验方法', '样本值', '判定']) {
  assert.ok(source.includes(label), `PQC leader item snapshot must display ${label}.`)
}
assert.match(
  source,
  /PQC提交内容缺少正式项目明细/,
  'PQC leader page must show a blocking message when the submitted event lacks item snapshots.'
)
assert.doesNotMatch(
  source,
  /findPqcItemCandidate[\s\S]*pieceKey\.endsWith\(`:\$\{itemKey\}`\)/,
  'PQC leader page must not infer official details from legacy fixed-item piece keys.'
)

console.log('PASS: PQC leader item snapshot static contract')
