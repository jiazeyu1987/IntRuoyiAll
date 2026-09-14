const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../..')
const component = fs.readFileSync(
  path.join(root, 'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
)

const selectStart = component.indexOf('data-frontline-select-parameter')
assert.ok(selectStart >= 0, '一线生产设备参数必须保留下拉参数控件。')

const selectBlockStart = component.lastIndexOf('<el-select', selectStart)
const selectBlockEnd = component.indexOf('</el-select>', selectStart)
assert.ok(selectBlockStart >= 0 && selectBlockEnd > selectBlockStart, '必须能定位设备参数 el-select 块。')
const selectBlock = component.slice(selectBlockStart, selectBlockEnd)

assert.match(
  selectBlock,
  /class="[^"]*frontline-production-device-param-select[^"]*"/,
  '下拉参数必须使用一线生产设备参数专用样式类，不能退回 Element Plus 默认窄控件。'
)
assert.match(selectBlock, /\ballow-create\b/, '下拉参数必须保留手动输入能力。')
assert.match(selectBlock, /parameter\.optionValues/, '下拉参数必须继续使用正式参数选项。')

const styleStart = component.indexOf('.frontline-production-device-param {')
assert.ok(styleStart >= 0, '必须保留设备参数控件样式块。')
const styleBlock = component.slice(styleStart)

assert.match(
  styleBlock,
  /\.frontline-production-device-param-select\s*\{[\s\S]*grid-column:\s*2\s*\/\s*5/,
  '下拉参数应占用与数字值区域一致的中间控件宽度。'
)
assert.match(
  styleBlock,
  /\.frontline-production-device-param-select\s*\{[\s\S]*:deep\(\.el-select__wrapper\)\s*\{[\s\S]*height:\s*72px[\s\S]*border:\s*3px\s+solid\s+var\(--frontline-line\)[\s\S]*border-radius:\s*14px/,
  '下拉参数内部输入框高度、边框和圆角必须与数字参数控件一致。'
)
assert.match(
  styleBlock,
  /\.frontline-production-device-param-select\s*\{[\s\S]*box-sizing:\s*border-box[\s\S]*:deep\(\.el-select__wrapper\)\s*\{[\s\S]*box-sizing:\s*border-box/,
  '下拉参数根节点和内部 wrapper 必须使用 border-box，避免 72px 高度叠加边框后被裁切。'
)
assert.match(
  styleBlock,
  /\.frontline-production-device-param-select\s*\{[\s\S]*:deep\(\.el-select__input\)[\s\S]*height:\s*auto[\s\S]*border:\s*0[\s\S]*background:\s*transparent/,
  '下拉参数内部搜索输入不能继承设备参数通用 input 的 72px 边框样式。'
)
assert.match(
  styleBlock,
  /\.frontline-production-device-param-select\s*\{[\s\S]*:deep\(\.el-select__placeholder\)[\s\S]*font-size:\s*32px/,
  '下拉参数展示文字字号必须与大号参数输入节奏一致。'
)

console.log('PASS: frontline device select style static contract')
