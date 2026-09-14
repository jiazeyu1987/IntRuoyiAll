const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const frontlinePanel = readUtf8('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')

const extractCssBlock = (selector) => {
  const selectorIndex = frontlinePanel.indexOf(selector)
  assert.ok(selectorIndex >= 0, selector + ' 样式块必须存在。')
  const blockStart = frontlinePanel.indexOf('{', selectorIndex)
  assert.ok(blockStart > selectorIndex, selector + ' 样式块必须包含声明。')

  let depth = 0
  for (let index = blockStart; index < frontlinePanel.length; index += 1) {
    const char = frontlinePanel[index]
    if (char === '{') {
      depth += 1
    } else if (char === '}') {
      depth -= 1
      if (depth === 0) {
        return frontlinePanel.slice(blockStart + 1, index)
      }
    }
  }

  assert.fail(selector + ' 样式块未闭合。')
}

const devicePanelStart = frontlinePanel.indexOf(
  'class="frontline-work-panel panel device-panel frontline-production-device-panel"'
)
const submitBarStart = frontlinePanel.indexOf('class="frontline-production-submit-bar bottom"', devicePanelStart)

assert.ok(devicePanelStart >= 0, '一线生产页面必须保留右侧设备面板。')
assert.ok(submitBarStart > devicePanelStart, '一线生产右侧设备面板必须在提交栏之前结束。')

const devicePanelTemplate = frontlinePanel.slice(devicePanelStart, submitBarStart)

assert.ok(
  devicePanelTemplate.includes('data-frontline-production-no-device-empty'),
  '无设备工序必须渲染正式无设备空态。'
)
assert.ok(
  devicePanelTemplate.indexOf('data-frontline-production-no-device-empty') <
    devicePanelTemplate.indexOf('data-production-clearance-confirmations'),
  '无设备空态必须位于清场确认控件之前，由 CSS 控制上方区域与底部控件位置。'
)

const devicePanelCss = extractCssBlock('.frontline-production-device-panel')
const noDeviceEmptyCss = extractCssBlock('.frontline-production-device-empty')
const clearanceConfirmationsCss = extractCssBlock('.frontline-production-clearance-confirmations')

assert.ok(
  devicePanelCss.includes('grid-template-rows: 118px minmax(0, 1fr) auto;'),
  '右侧设备面板必须保留设备标签、设备内容、底部确认控件三行布局。'
)
assert.ok(
  noDeviceEmptyCss.includes('grid-row: 1 / 3;'),
  '无设备空态必须占据设备标签和设备内容两行，不能覆盖底部确认控件行。'
)
assert.ok(
  !noDeviceEmptyCss.includes('grid-row: 2 / span 2;'),
  '无设备空态不得继续跨到底部确认控件行。'
)
assert.ok(
  clearanceConfirmationsCss.includes('grid-row: 3;'),
  '清场、物料、效期、清洁四组确认控件必须固定在右侧设备面板底部行。'
)
assert.ok(
  clearanceConfirmationsCss.includes('grid-template-columns: repeat(2, minmax(0, 1fr));') &&
    clearanceConfirmationsCss.includes('grid-template-rows: repeat(2, minmax(0, auto));'),
  '四组确认控件必须保持有设备场景的两列两行尺寸结构。'
)

console.log('frontline-no-device-clearance-layout-static PASS')
