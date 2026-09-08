const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const source = fs.readFileSync(
  path.resolve(__dirname, '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'),
  'utf8'
).replace(/\r\n/g, '\n')

assert.match(
  source,
  /const FRONTLINE_OTHER_OPTION_VALUE = ['"]__FRONTLINE_OTHER__['"]/,
  '一线下拉框必须使用统一且不可提交的“其他”占位值。'
)
assert.match(
  source,
  /<el-option\s+:label="FRONTLINE_OTHER_OPTION_LABEL"\s+:value="FRONTLINE_OTHER_OPTION_VALUE"\s*\/>/,
  '生产设备枚举参数下拉框末尾必须提供“其他（手动输入）”。'
)
assert.match(
  source,
  /<el-select[\s\S]*?data-frontline-select-parameter[\s\S]*?placeholder="请选择"/,
  '生产设备枚举参数下拉框的空值提示必须显示“请选择”。'
)
assert.match(
  source,
  /placeholder="请输入"[\s\S]*data-frontline-production-select-custom-input[\s\S]*updateProductionDeviceSelectParameter/,
  '生产设备枚举参数选择其他后必须显示并保存手工输入。'
)
assert.match(
  source,
  /updateProductionDeviceSelectParameter[\s\S]*value === FRONTLINE_OTHER_OPTION_VALUE/,
  '生产下拉框必须显式处理“其他”模式。'
)
assert.match(
  source,
  /<option\s+:value="FRONTLINE_OTHER_OPTION_VALUE">\{\{ FRONTLINE_OTHER_OPTION_LABEL \}\}<\/option>/,
  'PQC 检验设备下拉框末尾必须提供“其他（手动输入）”。'
)
assert.match(
  source,
  /data-pqc-custom-equipment-input[\s\S]*updatePqcCustomEquipmentText/,
  'PQC 选择其他后必须显示手工设备输入框。'
)
assert.match(
  source,
  /const isPqcCustomEquipmentSelection[\s\S]*!selection\.selectedEquipmentId[\s\S]*selectedEquipmentNumber/,
  'PQC 手工设备必须使用空设备 ID 和非空设备文本表达。'
)
assert.match(
  source,
  /assertPqcItemEquipmentSelection[\s\S]*FRONTLINE_OTHER_OPTION_VALUE/,
  'PQC 提交前必须阻止空白“其他”占位值进入 payload。'
)

console.log('PASS: frontline production and PQC dropdowns support explicit other manual entry')
