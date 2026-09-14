const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const panel = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

const blockBetween = (startToken, endToken) => {
  const start = panel.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = panel.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return panel.slice(start, end)
}

const piecePayloadBlock = blockBetween(
  'const buildPqcPieceValuesPayloadForTask = (taskOption: PqcTaskOptionSnapshot) => {',
  'const buildPqcPieceValuesPayload = () => {'
)
assert.match(
  piecePayloadBlock,
  /values\[item\.key\]\s*=\s*getPqcExactPieceValuesForTask\(item\.key,\s*taskOption\)/,
  'Raw payload piece values must use exact task samples so untouched methods submit the planned default samples.'
)
assert.doesNotMatch(
  piecePayloadBlock,
  /getPqcRelaxedPieceValuesForTask/,
  'Raw payload piece values must not use relaxed samples that can hide empty arrays.'
)

const itemResultsBlock = blockBetween(
  'const buildPqcItemResultsPayload = (',
  'const buildPqcItemDetailsPayload = ('
)
assert.match(
  itemResultsBlock,
  /sampleValues:\s*getPqcExactPieceValuesForTask\(item\.key,\s*taskOption\)/,
  'itemResults.sampleValues must carry exact task samples for every submitted inspection-method task.'
)
assert.doesNotMatch(
  itemResultsBlock,
  /getPqcRelaxedPieceValuesForTask/,
  'itemResults.sampleValues must not use relaxed samples that can be filtered out before backend validation.'
)

const itemDetailsBlock = blockBetween(
  'const buildPqcItemDetailsPayload = (',
  'const assertPqcCurrentProcessAllMethodSubmissionReady = () => {'
)
assert.match(
  itemDetailsBlock,
  /sampleValues:\s*getPqcExactPieceValuesForTask\(item\.key,\s*taskOption\)/,
  'rawPayload.pqcItemDetails must mirror exact task samples for every submitted inspection-method task.'
)
assert.doesNotMatch(
  itemDetailsBlock,
  /getPqcRelaxedPieceValuesForTask/,
  'rawPayload.pqcItemDetails must not use relaxed samples that can hide missing planned defaults.'
)

console.log('frontline-pqc-default-sample-payload-static: PASS')
