const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const frontendRoot = path.resolve(__dirname, '../..')
const qaPagePath = path.join(
  frontendRoot,
  'src/views/mes/pro/processpool/QaRegulationPage.vue'
)
const qaSource = fs.readFileSync(qaPagePath, 'utf8')

const start = qaSource.indexOf('const createPressurePumpQaRegulationItems')
const end =
  start >= 0
    ? qaSource.indexOf('const createBalloonPressurePumpQaRegulationItems', start)
    : -1
assert.ok(start >= 0 && end > start, 'Pressure-pump QA regulation item template must exist.')
const source = qaSource.slice(start, end)

function extractItemByCode(itemCode) {
  const codeIndex = source.indexOf(`itemCode: '${itemCode}'`)
  assert.ok(codeIndex >= 0, `Missing pressure-pump PDF item ${itemCode}.`)
  const itemStart = source.lastIndexOf('\n  {', codeIndex)
  const itemEnd = source.indexOf('\n  }', codeIndex)
  assert.ok(itemStart >= 0 && itemEnd > itemStart, `Item block ${itemCode} must be parseable.`)
  return source.slice(itemStart, itemEnd)
}

function readStringField(itemSource, fieldName) {
  const match = itemSource.match(new RegExp("^\\s*" + fieldName + ": '([^']*)',?$", 'm'))
  assert.ok(match, `Field ${fieldName} must exist.`)
  return match[1]
}

function readNumberField(itemSource, fieldName) {
  const match = itemSource.match(new RegExp("^\\s*" + fieldName + ': (\\d+),?$', 'm'))
  assert.ok(match, `Field ${fieldName} must exist.`)
  return Number(match[1])
}

const expectedRows = [
  {
    itemCode: 'PP-001-WASH-APP',
    page: 3,
    screenshotPage: 1,
    processName: '清洗',
    itemName: '外观',
    standardText: '活塞环、弹簧、杠杆、螺纹块、杠杆架、按钮、活塞清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洗 / 外观'
  },
  {
    itemCode: 'PP-002-CLEAN-APP',
    page: 3,
    screenshotPage: 1,
    processName: '清洁',
    itemName: '外观',
    standardText: '压力表、外套、后盖、螺杆清洁后表面应清洁，无黑点、无浮尘、无异物等。',
    inspectionMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洁 / 外观'
  },
  {
    itemCode: 'PP-003-SCREW-APP',
    page: 3,
    screenshotPage: 1,
    processName: '组装螺杆八组件',
    itemName: '外观',
    standardText: '1）硅化后杠杆架表面应无多余硅油；2）组装杠杆架后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；3）组装后芯杆应无多余毛屑；4）硅化后螺杆表面应无成滴的硅油汇聚；5）组装螺杆后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；表面无成滴的硅油。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '组装螺杆八组件 / 外观'
  },
  {
    itemCode: 'PP-004-SCREW-NOJUMP',
    page: 3,
    screenshotPage: 1,
    processName: '组装螺杆八组件',
    itemName: '无跳压',
    standardText: '20atm 压力打至 20atm 应无跳压现象；30atm 压力打至 30atm 应无跳压现象；40atm 压力泵需打压至 40atm 无跳压现象。',
    inspectionMethod: '将推杆装到检测专用泵筒（吸入 10ML 水）上，将压力打至 20atm/30atm/40atm 应无跳压现象。',
    inspectionTool: '检测专用泵筒',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '组装螺杆八组件 / 无跳压'
  },
  {
    itemCode: 'PP-005-UV-SWIVEL-APP',
    page: 4,
    screenshotPage: 2,
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 外观',
    standardText: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 外观'
  },
  {
    itemCode: 'PP-006-UV-SWIVEL-STRENGTH',
    page: 4,
    screenshotPage: 2,
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 牢固度'
  },
  {
    itemCode: 'PP-007-UV-GAUGE-APP',
    page: 4,
    screenshotPage: 2,
    processName: '光固外套四组件',
    itemName: '光固压力表 / 外观',
    standardText: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 外观'
  },
  {
    itemCode: 'PP-008-UV-GAUGE-STRENGTH',
    page: 4,
    screenshotPage: 2,
    processName: '光固外套四组件',
    itemName: '光固压力表 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 牢固度'
  },
  {
    itemCode: 'PP-009-UV-TUBE-APP',
    page: 4,
    screenshotPage: 2,
    processName: '光固外套四组件',
    itemName: '光固延长管 / 外观',
    standardText: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 外观'
  },
  {
    itemCode: 'PP-010-UV-TUBE-STRENGTH',
    page: 5,
    screenshotPage: 3,
    processName: '光固外套四组件',
    itemName: '光固延长管 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 牢固度'
  },
  {
    itemCode: 'PP-011-ASSEMBLE-PISTON-APP',
    page: 5,
    screenshotPage: 3,
    processName: '装配',
    itemName: '装配活塞 / 外观',
    standardText: '1）表面清洁、无黑点、异物、无划伤、无注塑缺陷；2）放置活塞时应注意活塞是否放正，避免压偏侧倒，活塞应卡紧。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞 / 外观'
  },
  {
    itemCode: 'PP-012-SILICONE-RING-APP',
    page: 5,
    screenshotPage: 3,
    processName: '装配',
    itemName: '硅化活塞环 / 外观',
    standardText: '硅化后活塞环表面无成滴的硅油汇聚。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 硅化活塞环 / 外观'
  },
  {
    itemCode: 'PP-013-ASSEMBLE-RING-APP',
    page: 5,
    screenshotPage: 3,
    processName: '装配',
    itemName: '装配活塞环 / 外观',
    standardText: '活塞环表面应干净无异物，边缘无缺损。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞环 / 外观'
  },
  {
    itemCode: 'PP-014-ASSEMBLE-RING-FIT',
    page: 5,
    screenshotPage: 3,
    processName: '装配',
    itemName: '装配活塞环 / 配合',
    standardText: '活塞环套进活塞的槽中，套上后活塞环应轻松适度。',
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞环 / 配合'
  },
  {
    itemCode: 'PP-015-ASSEMBLE-SLEEVE-APP',
    page: 6,
    screenshotPage: 4,
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 外观',
    standardText: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 外观'
  },
  {
    itemCode: 'PP-016-ASSEMBLE-SLEEVE-FIT',
    page: 6,
    screenshotPage: 4,
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 配合',
    standardText: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 配合'
  },
  {
    itemCode: 'PP-017-BOND-AIRTIGHT-APP',
    page: 6,
    screenshotPage: 4,
    processName: '整体粘结',
    itemName: '外观',
    standardText: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 外观'
  },
  {
    itemCode: 'PP-018-BOND-NO-BLOCK',
    page: 6,
    screenshotPage: 4,
    processName: '整体粘结',
    itemName: '无卡阻',
    standardText: '将粘接完成 12 小时后按压按钮应无卡死现象，来回抽拉推杆应顺畅无卡阻。',
    inspectionMethod: '手感。',
    inspectionTool: '手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 无卡阻'
  },
  {
    itemCode: 'PP-019-BOND-STRENGTH',
    page: 6,
    screenshotPage: 4,
    processName: '整体粘结',
    itemName: '牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 牢固度'
  },
  {
    itemCode: 'PP-020-AIRTIGHT-NEGATIVE',
    page: 7,
    screenshotPage: 5,
    processName: '整体粘结',
    itemName: '气密性 / 负压检测',
    standardText: '负压检测：抽负压-80±5kpa，不应有泄漏。',
    inspectionMethod: '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 负压检测'
  },
  {
    itemCode: 'PP-021-AIRTIGHT-HIGH',
    page: 7,
    screenshotPage: 5,
    processName: '整体粘结',
    itemName: '气密性 / 高压检测',
    standardText: '高压检测：将负压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上气源（其中 20atm 量程：20atm 气源；30atm 量程：30atm 气源；40atm 量程：40atm 气源），打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将负压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 高压检测'
  },
  {
    itemCode: 'PP-022-AIRTIGHT-LOW',
    page: 7,
    screenshotPage: 5,
    processName: '整体粘结',
    itemName: '气密性 / 低压检测',
    standardText: '低压检测：将高压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将高压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 低压检测'
  }
]

const expectedPageCounts = new Map([
  [3, 4],
  [4, 5],
  [5, 5],
  [6, 5],
  [7, 3]
])

const itemCodeMatches = [...source.matchAll(/itemCode: '/g)]
assert.equal(itemCodeMatches.length, 22, 'Pressure-pump PDF template must contain all 22 inspection rows from the 5 screenshot pages.')

const actualPageCounts = new Map()

for (const row of expectedRows) {
  const { itemCode, page, processName, itemName, standardText, inspectionMethod, inspectionTool, samplingPlanText, sourceOriginalItem } = row
  const itemSource = extractItemByCode(itemCode)
  assert.equal(readNumberField(itemSource, 'sourceOriginalPage'), page, `${itemCode} must preserve the PDF page.`)
  actualPageCounts.set(page, (actualPageCounts.get(page) || 0) + 1)
  assert.equal(readStringField(itemSource, 'processName'), processName, `${itemCode} process must match the screenshot row group.`)
  assert.equal(readStringField(itemSource, 'itemName'), itemName, `${itemCode} item name must match the screenshot.`)
  assert.equal(readStringField(itemSource, 'standardText'), standardText, `${itemCode} standard must match the screenshot.`)
  assert.equal(readStringField(itemSource, 'inspectionMethod'), inspectionMethod, `${itemCode} method must match the screenshot.`)
  assert.equal(readStringField(itemSource, 'inspectionTool'), inspectionTool, `${itemCode} tool must match the equipment column.`)
  assert.equal(readStringField(itemSource, 'samplingPlanText'), samplingPlanText, `${itemCode} sampling plan must match the screenshot.`)
  assert.equal(readStringField(itemSource, 'sourceOriginalItem'), sourceOriginalItem, `${itemCode} original item path must match the screenshot hierarchy.`)
}

for (const [page, expectedCount] of expectedPageCounts) {
  assert.equal(actualPageCounts.get(page), expectedCount, `PDF page ${page} must contain ${expectedCount} verified screenshot rows.`)
}

console.log('PASS qa-regulation-pressure-pump-complete-pdf-items-static')
