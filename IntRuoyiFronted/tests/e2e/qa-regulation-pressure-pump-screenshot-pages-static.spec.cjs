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
assert.ok(start >= 0 && end > start, 'PQC-IDI-001 pressure-pump item template must exist.')

const source = qaSource.slice(start, end)

function extractItemByCode(itemCode) {
  const codeIndex = source.indexOf(`itemCode: '${itemCode}'`)
  assert.ok(codeIndex >= 0, `Missing screenshot item ${itemCode}.`)
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

const screenshotPages = [
  {
    screenshot: '图1',
    pdfPage: 3,
    codes: ['PP-001-WASH-APP', 'PP-002-CLEAN-APP', 'PP-003-SCREW-APP', 'PP-004-SCREW-NOJUMP']
  },
  {
    screenshot: '图2',
    pdfPage: 4,
    codes: ['PP-005-UV-SWIVEL-APP', 'PP-006-UV-SWIVEL-STRENGTH', 'PP-007-UV-GAUGE-APP', 'PP-008-UV-GAUGE-STRENGTH', 'PP-009-UV-TUBE-APP']
  },
  {
    screenshot: '图3',
    pdfPage: 5,
    codes: ['PP-010-UV-TUBE-STRENGTH', 'PP-011-ASSEMBLE-PISTON-APP', 'PP-012-SILICONE-RING-APP', 'PP-013-ASSEMBLE-RING-APP', 'PP-014-ASSEMBLE-RING-FIT']
  },
  {
    screenshot: '图4',
    pdfPage: 6,
    codes: ['PP-015-ASSEMBLE-SLEEVE-APP', 'PP-016-ASSEMBLE-SLEEVE-FIT', 'PP-017-BOND-AIRTIGHT-APP', 'PP-018-BOND-NO-BLOCK', 'PP-019-BOND-STRENGTH']
  },
  {
    screenshot: '图5',
    pdfPage: 7,
    codes: ['PP-020-AIRTIGHT-NEGATIVE', 'PP-021-AIRTIGHT-HIGH', 'PP-022-AIRTIGHT-LOW']
  }
]

const screenshotItems = new Map([
  ['PP-001-WASH-APP', {
    processName: '清洗',
    itemName: '外观',
    standardText: '活塞环、弹簧、杠杆、螺纹块、杠杆架、按钮、活塞清洗干燥后表面及内部应无液珠；表面应清洁，无黑点、无异物等。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洗 / 外观'
  }],
  ['PP-002-CLEAN-APP', {
    processName: '清洁',
    itemName: '外观',
    standardText: '压力表、外套、后盖、螺杆清洁后表面应清洁，无黑点、无浮尘、无异物等。',
    inspectionMethod: '用清洁、无尘布，蘸取 75% 酒精擦拭产品表面。正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: 'GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '清洁 / 外观'
  }],
  ['PP-003-SCREW-APP', {
    processName: '组装螺杆八组件',
    itemName: '外观',
    standardText: '1）硅化后杠杆架表面应无多余硅油；2）组装杠杆架后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；3）组装后芯杆应无多余毛屑；4）硅化后螺杆表面应无成滴的硅油汇聚；5）组装螺杆后组件表面应清洁、无黑点、异物、无划伤、无注塑缺陷；表面无成滴的硅油。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '组装螺杆八组件 / 外观'
  }],
  ['PP-004-SCREW-NOJUMP', {
    processName: '组装螺杆八组件',
    itemName: '无跳压',
    standardText: '20atm 压力打至 20atm 应无跳压现象；30atm 压力打至 30atm 应无跳压现象；40atm 压力泵需打压至 40atm 无跳压现象。',
    inspectionMethod: '将推杆装到检测专用泵筒（吸入 10ML 水）上，将压力打至 20atm/30atm/40atm 应无跳压现象。',
    inspectionTool: '检测专用泵筒',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '组装螺杆八组件 / 无跳压'
  }],
  ['PP-005-UV-SWIVEL-APP', {
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 外观',
    standardText: '延长管和旋转接头：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 外观'
  }],
  ['PP-006-UV-SWIVEL-STRENGTH', {
    processName: '光固外套四组件',
    itemName: '光固旋转接头 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固旋转接头 / 牢固度'
  }],
  ['PP-007-UV-GAUGE-APP', {
    processName: '光固外套四组件',
    itemName: '光固压力表 / 外观',
    standardText: '外套与压力表：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 外观'
  }],
  ['PP-008-UV-GAUGE-STRENGTH', {
    processName: '光固外套四组件',
    itemName: '光固压力表 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固压力表 / 牢固度'
  }],
  ['PP-009-UV-TUBE-APP', {
    processName: '光固外套四组件',
    itemName: '光固延长管 / 外观',
    standardText: '延长管与外套：光固位置应整洁均匀圆滑美观；胶水未污染其它地方。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 外观'
  }],
  ['PP-010-UV-TUBE-STRENGTH', {
    processName: '光固外套四组件',
    itemName: '光固延长管 / 牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '光固外套四组件 / 光固延长管 / 牢固度'
  }],
  ['PP-011-ASSEMBLE-PISTON-APP', {
    processName: '装配',
    itemName: '装配活塞 / 外观',
    standardText: '1）表面清洁、无黑点、异物、无划伤、无注塑缺陷；2）放置活塞时应注意活塞是否放正，避免压偏侧倒，活塞应卡紧。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞 / 外观'
  }],
  ['PP-012-SILICONE-RING-APP', {
    processName: '装配',
    itemName: '硅化活塞环 / 外观',
    standardText: '硅化后活塞环表面无成滴的硅油汇聚。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 硅化活塞环 / 外观'
  }],
  ['PP-013-ASSEMBLE-RING-APP', {
    processName: '装配',
    itemName: '装配活塞环 / 外观',
    standardText: '活塞环表面应干净无异物，边缘无缺损。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞环 / 外观'
  }],
  ['PP-014-ASSEMBLE-RING-FIT', {
    processName: '装配',
    itemName: '装配活塞环 / 配合',
    standardText: '活塞环套进活塞的槽中，套上后活塞环应轻松适度。',
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 装配活塞环 / 配合'
  }],
  ['PP-015-ASSEMBLE-SLEEVE-APP', {
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 外观',
    standardText: '压力泵整体外观应无黑点、杂质、花纹、划痕等外观缺陷；压力泵内腔无异物、毛丝等活动异物；压力泵外套应有足够的透明度，能清晰地看到基准线；压力泵的第一条刻度线（泵体排空时）应与活塞重合。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 外观'
  }],
  ['PP-016-ASSEMBLE-SLEEVE-FIT', {
    processName: '装配',
    itemName: '外套组件与套筒组件装配 / 配合',
    standardText: '1）推杆组件推入外套，后盖与外套的卡槽扣到位，旋转后盖使得后盖与外套的缺口完全一致，不能偏掉；2）旋转螺杆检查扭力不应偏大，按下按钮推拉螺杆看应无干涉及推拉力偏大。',
    inspectionMethod: '目测、手感。',
    inspectionTool: '目测、手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '装配 / 外套组件与套筒组件装配 / 配合'
  }],
  ['PP-017-BOND-AIRTIGHT-APP', {
    processName: '整体粘结',
    itemName: '外观',
    standardText: '对气密性检测合格的产品进行外观检查应无黑点、杂质、花纹、划痕、缺损、裂纹等外观缺陷；不应有多余胶水外露。',
    inspectionMethod: '正常或矫正视力，在 300~700lx 的照度下，离眼 30~40cm 处，观察约 5~10s，对气密性合格的产品进行观察。',
    inspectionTool: '目测',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 外观'
  }],
  ['PP-018-BOND-NO-BLOCK', {
    processName: '整体粘结',
    itemName: '无卡阻',
    standardText: '将粘接完成 12 小时后按压按钮应无卡死现象，来回抽拉推杆应顺畅无卡阻。',
    inspectionMethod: '手感。',
    inspectionTool: '手感',
    samplingPlanText: '首件：13 件；GB/T 2828.1，I，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 无卡阻'
  }],
  ['PP-019-BOND-STRENGTH', {
    processName: '整体粘结',
    itemName: '牢固度',
    standardText: '连接处施加 15N 的静态轴向拉力保持 15s，应不脱落。',
    inspectionMethod: '用 15N 的砝码悬挂，停留 15s。',
    inspectionTool: '15N 砝码',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 牢固度'
  }],
  ['PP-020-AIRTIGHT-NEGATIVE', {
    processName: '整体粘结',
    itemName: '气密性 / 负压检测',
    standardText: '负压检测：抽负压-80±5kpa，不应有泄漏。',
    inspectionMethod: '将粘接完成 12 小时后的压力泵接上气密性检测工装，抽负压-80±5kpa，观察有无泄漏。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 负压检测'
  }],
  ['PP-021-AIRTIGHT-HIGH', {
    processName: '整体粘结',
    itemName: '气密性 / 高压检测',
    standardText: '高压检测：将负压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上气源（其中 20atm 量程：20atm 气源；30atm 量程：30atm 气源；40atm 量程：40atm 气源），打开气源，观察压力表应能匀速上升到指定压力，到达最大压力后 10s 内压力表指针应无跳压、降压的现象，撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将负压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 高压检测'
  }],
  ['PP-022-AIRTIGHT-LOW', {
    processName: '整体粘结',
    itemName: '气密性 / 低压检测',
    standardText: '低压检测：将高压检测合格的压力泵装到气密性检测工装上，通过旋转接头接上 8atm 气源，打开气源，观察压力表指针，应可以匀速指示到测试压力值，不应有升压缓慢或直接从低压跳到 8atm 现象；撤掉气源后，压力表应可以迅速回零。',
    inspectionMethod: '将高压检测合格的压力泵装到气密性检测工装上，进行检测。',
    inspectionTool: '气密性检测工装',
    samplingPlanText: '首件：5 件；GB/T 2828.1，S-3，AQL=0.4',
    sourceOriginalItem: '整体粘结 / 气密性 / 低压检测'
  }]
])

const itemCodeMatches = [...source.matchAll(/itemCode: '/g)]
assert.equal(itemCodeMatches.length, 22, 'PQC-IDI-001 screenshot pages must map to 22 visible inspection rows.')

let previousIndex = -1
for (const page of screenshotPages) {
  for (const itemCode of page.codes) {
    const itemSource = extractItemByCode(itemCode)
    const itemIndex = source.indexOf(`itemCode: '${itemCode}'`)
    assert.ok(itemIndex > previousIndex, `${page.screenshot} item ${itemCode} must keep screenshot order.`)
    previousIndex = itemIndex

    const expected = screenshotItems.get(itemCode)
    assert.ok(expected, `Expected screenshot fixture must include ${itemCode}.`)
    assert.equal(readNumberField(itemSource, 'sourceOriginalPage'), page.pdfPage, `${itemCode} PDF page must match ${page.screenshot}.`)
    for (const fieldName of [
      'processName',
      'itemName',
      'standardText',
      'inspectionMethod',
      'inspectionTool',
      'samplingPlanText',
      'sourceOriginalItem'
    ]) {
      assert.equal(readStringField(itemSource, fieldName), expected[fieldName], `${itemCode} ${fieldName} must match ${page.screenshot}.`)
    }
    assert.equal(readStringField(itemSource, 'sourceOriginalExcerpt'), expected.standardText, `${itemCode} source excerpt must match ${page.screenshot}.`)
    assert.equal(readStringField(itemSource, 'sourceOriginalMethod'), expected.inspectionMethod, `${itemCode} source method must match ${page.screenshot}.`)
  }
}

const page4OverallAppearance = extractItemByCode('PP-017-BOND-AIRTIGHT-APP')
assert.notEqual(
  readStringField(page4OverallAppearance, 'itemName'),
  '气密性 / 外观',
  '图4 整体粘结外观不能归入图5 的气密性分组。'
)
assert.notEqual(
  readStringField(page4OverallAppearance, 'sourceOriginalItem'),
  '整体粘结 / 气密性 / 外观',
  '图4 整体粘结外观原始路径不能带气密性分组。'
)

console.log('PASS qa-regulation-pressure-pump-screenshot-pages-static')
