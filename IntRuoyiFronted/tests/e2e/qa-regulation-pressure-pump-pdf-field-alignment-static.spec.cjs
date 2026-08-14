const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const pressurePumpItemsStart = qaSource.indexOf('const createPressurePumpQaRegulationItems')
const pressurePumpItemsEnd =
  pressurePumpItemsStart >= 0
    ? qaSource.indexOf('const createBalloonPressurePumpQaRegulationItems', pressurePumpItemsStart)
    : -1

assert.ok(
  pressurePumpItemsStart >= 0 && pressurePumpItemsEnd > pressurePumpItemsStart,
  'Pressure-pump QA regulation items must remain defined in a local template.'
)

const pressurePumpItemsSource = qaSource.slice(pressurePumpItemsStart, pressurePumpItemsEnd)

function extractItemByCode(itemCode) {
  const codeIndex = pressurePumpItemsSource.indexOf(`itemCode: '${itemCode}'`)
  assert.ok(codeIndex >= 0, `QA regulation item ${itemCode} must exist.`)

  const itemStart = pressurePumpItemsSource.lastIndexOf('\n  {', codeIndex)
  const itemEnd = pressurePumpItemsSource.indexOf('\n  }', codeIndex)
  assert.ok(itemStart >= 0 && itemEnd > itemStart, `QA regulation item ${itemCode} block must be parseable.`)
  return pressurePumpItemsSource.slice(itemStart, itemEnd)
}

function readStringField(itemSource, fieldName) {
  const match = itemSource.match(new RegExp(`${fieldName}: '((?:\\\\'|[^'])*)'`))
  assert.ok(match, `Field ${fieldName} must exist in item block.`)
  return match[1].replace(/\\'/g, "'")
}

function assertPdfFieldAlignment(itemCode, expected) {
  const itemSource = extractItemByCode(itemCode)
  const itemName = readStringField(itemSource, 'itemName')
  const inspectionMethod = readStringField(itemSource, 'inspectionMethod')
  const standardText = readStringField(itemSource, 'standardText')
  const inspectionTool = readStringField(itemSource, 'inspectionTool')
  const samplingPlanText = readStringField(itemSource, 'samplingPlanText')
  const sourceOriginalExcerpt = readStringField(itemSource, 'sourceOriginalExcerpt')
  const sourceOriginalMethod = readStringField(itemSource, 'sourceOriginalMethod')

  assert.equal(itemName, expected.itemName, `${itemCode} item name must match the PDF row.`)
  assert.equal(inspectionMethod, expected.method, `${itemCode} inspection method must match the PDF method column.`)
  assert.equal(
    sourceOriginalMethod,
    expected.method,
    `${itemCode} source method must match the PDF method column.`
  )
  assert.equal(standardText, expected.standard, `${itemCode} standard must match the PDF acceptance-standard column.`)
  assert.equal(
    sourceOriginalExcerpt,
    expected.standard,
    `${itemCode} source excerpt must match the PDF acceptance-standard column.`
  )
  assert.equal(inspectionTool, '气密性检测工装', `${itemCode} inspection tool must match the PDF equipment column.`)
  assert.equal(
    samplingPlanText,
    '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    `${itemCode} sampling plan must match the PDF sampling column.`
  )
}

assertPdfFieldAlignment('PP-020-AIRTIGHT-NEGATIVE', {
  itemName: '气密性 / 负压检测',
  method: '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。',
  standard: '负压检测：抽负压-80±5kpa，不应有泄漏。'
})

assertPdfFieldAlignment('PP-021-AIRTIGHT-HIGH', {
  itemName: '气密性 / 高压检测',
  method: '将负压检测合格的压力泵装到气密性检测工装上，进行检测。',
  standard:
    '高压检测：将负压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上气源（其中 20atm 量程：20atm 气源；30atm 量程：30atm 气源；40atm 量程：40atm 气源），打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。'
})

assertPdfFieldAlignment('PP-022-AIRTIGHT-LOW', {
  itemName: '气密性 / 低压检测',
  method: '将高压检测合格的压力泵装到气密性检测工装上，进行检测。',
  standard:
    '低压检测：将高压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。'
})

console.log('PASS qa-regulation-pressure-pump-pdf-field-alignment-static')
