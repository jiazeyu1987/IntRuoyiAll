const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const draftStart = qaSource.indexOf('const createBalloonPressurePumpQaRegulationDraft')
const itemsStart = qaSource.indexOf('const createBalloonPressurePumpQaRegulationItems')
const itemsEnd = itemsStart >= 0 ? qaSource.indexOf('const qaRegulationItems', itemsStart) : -1
const bindingStart = qaSource.indexOf('const registerPressurePumpProductBinding =')
const bindingEnd = bindingStart >= 0 ? qaSource.indexOf('const saveCurrentQaProductRuleDraft =', bindingStart) : -1
const loadStart = qaSource.indexOf('const loadQaProductRuleDraft =')
const loadEnd = loadStart >= 0 ? qaSource.indexOf('const formatDccProjectCodeOption =', loadStart) : -1

assert.ok(draftStart >= 0, 'ID balloon pressure-pump draft template must exist.')
assert.ok(
  itemsStart >= 0 && itemsEnd > itemsStart,
  'ID balloon pressure-pump PDF item template must exist.'
)
assert.ok(
  bindingStart >= 0 && bindingEnd > bindingStart,
  'QA product binding registration must be parseable.'
)
assert.ok(loadStart >= 0 && loadEnd > loadStart, 'QA product template loader must be parseable.')

const draftSource = qaSource.slice(draftStart, itemsStart)
const itemSource = qaSource.slice(itemsStart, itemsEnd)
const bindingSource = qaSource.slice(bindingStart, bindingEnd)
const loadSource = qaSource.slice(loadStart, loadEnd)

function extractItemByCode(itemCode) {
  const codeIndex = itemSource.indexOf(`itemCode: '${itemCode}'`)
  assert.ok(codeIndex >= 0, `Missing PQC-ID-001 PDF item ${itemCode}.`)
  const itemStart = itemSource.lastIndexOf('\n  {', codeIndex)
  const itemEnd = itemSource.indexOf('\n  }', codeIndex)
  assert.ok(itemStart >= 0 && itemEnd > itemStart, `Item block ${itemCode} must be parseable.`)
  return itemSource.slice(itemStart, itemEnd)
}

function readStringField(block, fieldName) {
  const match = block.match(new RegExp("^\\s*" + fieldName + ": '([^']*)',?$", 'm'))
  assert.ok(match, `Field ${fieldName} must exist.`)
  return match[1]
}

function readNumberField(block, fieldName) {
  const match = block.match(new RegExp("^\\s*" + fieldName + ': (\\d+),?$', 'm'))
  assert.ok(match, `Field ${fieldName} must exist.`)
  return Number(match[1])
}

assert.match(
  qaSource,
  /const BALLOON_PRESSURE_PUMP_PROJECT_CODE = 'ID'/,
  'ID product code must have a dedicated QA template selector.'
)
assert.match(draftSource, /regulationCode: 'PQC-ID-001'/, 'ID template must use PQC-ID-001.')
assert.match(
  draftSource,
  /regulationName: '（椎体）球囊扩张压力泵组装过程检验规程'/,
  'ID template must use the exact PDF regulation name.'
)
assert.match(draftSource, /versionNo: 'G\/0'/, 'ID template must use PDF version G/0.')
assert.match(draftSource, /effectiveDate: '2025-09-30'/, 'ID template must use PDF effective date.')
assert.match(
  bindingSource,
  /normalizeDccProjectCode\(project\.projectCode\) === BALLOON_PRESSURE_PUMP_PROJECT_CODE[\s\S]*balloonPressurePumpProductId\.value = productId/,
  'DCC project code ID must register the formal ID product id.'
)
assert.match(
  loadSource,
  /isBalloonPressurePumpProduct[\s\S]*createBalloonPressurePumpQaRegulationDraft\(\)[\s\S]*createBalloonPressurePumpQaRegulationItems\(\)/,
  'Product id matching the ID product must load the PQC-ID-001 draft and items.'
)
assert.doesNotMatch(itemSource, /PQC-IDI-001|按压式球囊扩充压力泵/, 'ID item source must not reuse the IDI PDF template.')

const expectedRows = [
  {
    itemCode: 'ID-001-WASH-APP',
    page: 4,
    processName: '清洗/精洗',
    itemName: '外观',
    standardText: '弹簧、胶塞、套筒、手柄、齿条、芯杆、螺盖清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洗/精洗 / 外观'
  },
  {
    itemCode: 'ID-002-CLEAN-APP',
    page: 4,
    processName: '清洁',
    itemName: '外观',
    standardText: '压力表等清洁后应清洁、无异物、浮尘。',
    inspectionMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洁 / 外观'
  },
  {
    itemCode: 'ID-003-ASSEMBLY-I-APP',
    page: 4,
    processName: '组装Ⅰ',
    itemName: '外观',
    standardText: '1）表面应清洁、无黑点、异物、无划伤、无注塑缺陷；2）硅化后齿条、螺盖表面应无成滴的多余硅油；3）组装后芯杆应无多余毛屑。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '组装Ⅰ / 外观'
  },
  {
    itemCode: 'ID-004-ASSEMBLY-I-RELEASE',
    page: 4,
    processName: '组装Ⅰ',
    itemName: '撤压',
    standardText: '将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上应能顺利撤压。',
    inspectionMethod: '将待检推杆与专用套筒（吸入 10ML 检测用纯化水）组装，将压力打至 25atm，放到撤压机（气压：2atm，缸径 20MM）上，观察能否顺利撤压。',
    inspectionTool: '撤压机',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '组装Ⅰ / 撤压'
  },
  {
    itemCode: 'ID-005-ASSEMBLY-I-NOJUMP',
    page: 4,
    processName: '组装Ⅰ',
    itemName: '无跳压',
    standardText: '30atm 的压力泵压力打至 30atm 应无跳压现象；40atm 的压力泵则压力打至 40 atm 无跳压现象。',
    inspectionMethod: '将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水）上，将压力打至 30 atm 应无跳压现象，加压泄压各 5 次；40atm 的压力泵则将推杆装到检测专用的泵筒（吸入 10 ml 和 20 ml 水），压力打至 40 atm 无跳压现象，加压泄压各 5 次。',
    inspectionTool: '/',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=1.0',
    sourceOriginalItem: '组装Ⅰ / 无跳压'
  },
  {
    itemCode: 'ID-006-UV-I-SWIVEL-APP',
    page: 5,
    processName: '光固Ⅰ',
    itemName: '光固旋转接头 / 外观',
    standardText: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固旋转接头 / 外观'
  },
  {
    itemCode: 'ID-007-UV-I-SWIVEL-STRENGTH',
    page: 5,
    processName: '光固Ⅰ',
    itemName: '光固旋转接头 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固旋转接头 / 牢固度'
  },
  {
    itemCode: 'ID-008-UV-I-GAUGE-APP',
    page: 5,
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 外观',
    standardText: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 外观'
  },
  {
    itemCode: 'ID-009-UV-I-GAUGE-STRENGTH',
    page: 5,
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 牢固度'
  },
  {
    itemCode: 'ID-010-UV-I-GAUGE-TORQUE',
    page: 5,
    processName: '光固Ⅰ',
    itemName: '光固压力表 / 扭力值',
    standardText: '压力表固化后扭力值＞5N·m。',
    inspectionMethod: '使用 5N·m 扭力扳手对连接处进行测试，无松动情况判定合格。',
    inspectionTool: '扭力扳手',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固压力表 / 扭力值'
  },
  {
    itemCode: 'ID-011-UV-I-TUBE-APP',
    page: 6,
    processName: '光固Ⅰ',
    itemName: '光固延长管 / 外观',
    standardText: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固Ⅰ / 光固延长管 / 外观'
  },
  {
    itemCode: 'ID-012-UV-I-TUBE-STRENGTH',
    page: 6,
    processName: '光固Ⅰ',
    itemName: '光固延长管 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=1.0',
    sourceOriginalItem: '光固Ⅰ / 光固延长管 / 牢固度'
  },
  {
    itemCode: 'ID-013-ASSEMBLY-II-APP',
    page: 6,
    processName: '组装Ⅱ / 硅化Ⅰ',
    itemName: '外观',
    standardText: '组装后产品表面应无黑点、杂质、花纹、划痕等外观缺陷；产品内腔无异物、毛丝等活动异物；配件组装后无挤压形成的多余料丝等现象；胶塞表面应无成滴的硅油汇聚。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=1.0',
    sourceOriginalItem: '组装Ⅱ / 硅化Ⅰ / 外观'
  },
  {
    itemCode: 'ID-014-TEST-HIGH-PRESSURE',
    page: 6,
    processName: '检测',
    itemName: '高压检测',
    standardText: '将整体组装产品装到气密性检测工装上，通过大脚接头接上 30atm（30atm 压力泵）/38atm（40atm 压力泵）气源，打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将组装产品装到气密性检测工装上进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '检测 / 高压检测'
  },
  {
    itemCode: 'ID-015-TEST-LOW-PRESSURE',
    page: 6,
    processName: '检测',
    itemName: '低压检测',
    standardText: '将高压检测合格的压力泵装到气密性检测工装上，通过大脚接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将高压检测合格的压力泵装到气密性检测工装上进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '检测 / 低压检测'
  },
  {
    itemCode: 'ID-016-UV-II-APP',
    page: 7,
    processName: '光固Ⅱ',
    itemName: '外观',
    standardText: '光固位置应整洁均匀圆滑美观；胶水没有污染到其它地方；压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；不应有多余胶水外露。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固Ⅱ / 外观'
  },
  {
    itemCode: 'ID-017-UV-II-STRENGTH',
    page: 7,
    processName: '光固Ⅱ',
    itemName: '牢固度',
    standardText: '对连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固Ⅱ / 牢固度'
  }
]

const expectedPageCounts = new Map([
  [4, 5],
  [5, 5],
  [6, 5],
  [7, 2]
])
const actualPageCounts = new Map()
const itemCodeMatches = [...itemSource.matchAll(/itemCode: '/g)]
assert.equal(itemCodeMatches.length, 17, 'PQC-ID-001 template must contain all 17 verified inspection rows.')

for (const row of expectedRows) {
  const block = extractItemByCode(row.itemCode)
  actualPageCounts.set(row.page, (actualPageCounts.get(row.page) || 0) + 1)
  assert.equal(readNumberField(block, 'sourceOriginalPage'), row.page, `${row.itemCode} PDF page must match.`)
  assert.equal(readStringField(block, 'processName'), row.processName, `${row.itemCode} process must match.`)
  assert.equal(readStringField(block, 'itemName'), row.itemName, `${row.itemCode} item name must match.`)
  assert.equal(readStringField(block, 'standardText'), row.standardText, `${row.itemCode} standard must match.`)
  assert.equal(readStringField(block, 'inspectionMethod'), row.inspectionMethod, `${row.itemCode} method must match.`)
  assert.equal(readStringField(block, 'inspectionTool'), row.inspectionTool, `${row.itemCode} tool must match.`)
  assert.equal(readStringField(block, 'samplingPlanText'), row.samplingPlanText, `${row.itemCode} sampling plan must match.`)
  assert.equal(readStringField(block, 'sourceOriginalItem'), row.sourceOriginalItem, `${row.itemCode} source item must match.`)
  assert.equal(readStringField(block, 'sourceOriginalExcerpt'), row.standardText, `${row.itemCode} source excerpt must mirror the standard column.`)
  assert.equal(readStringField(block, 'sourceOriginalMethod'), row.inspectionMethod, `${row.itemCode} source method must mirror the method column.`)
}

for (const [page, expectedCount] of expectedPageCounts) {
  assert.equal(actualPageCounts.get(page), expectedCount, `PDF page ${page} must contain ${expectedCount} verified rows.`)
}

console.log('PASS qa-regulation-id-balloon-pressure-pump-pdf-items-static')
