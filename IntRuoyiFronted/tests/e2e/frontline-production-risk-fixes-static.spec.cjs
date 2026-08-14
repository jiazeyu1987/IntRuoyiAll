const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const panelPath = path.resolve(
  __dirname,
  '../../src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const panel = fs.readFileSync(panelPath, 'utf8')

assert.doesNotMatch(
  panel,
  /请输入当前登录账号的电子签名密码/,
  '一线生产签名提示必须指向所选员工，不能提示当前登录账号。'
)

assert.match(
  panel,
  /throw new Error\('请输入所选员工的电子签名密码。'\)/,
  '正式提交构建 payload 前必须校验所选员工电子签名密码。'
)

assert.doesNotMatch(
  panel,
  /deviceId:\s*activeProductionDevice\.value\?\.key[\s\S]{0,160}selectedProcess\?\.deviceId/,
  '无班组设备卡片时不得把路线/工作站候选 deviceId 回填到正式提交上下文。'
)

assert.match(
  panel,
  /deviceId:\s*activeProductionDevice\.value\?\.key\s*\?\s*Number\(activeProductionDevice\.value\.key\)\s*:\s*undefined/,
  '正式提交上下文的 deviceId 只能来自当前可见的已选择班组设备卡片。'
)

assert.doesNotMatch(
  panel,
  /configuredDeviceCards\.value\.slice\(0,\s*3\)/,
  '一线生产设备卡片必须展示运行态返回的全部设备，不能只截取前三台。'
)

assert.match(
  panel,
  /const visibleDeviceCards = computed\(\(\) => configuredDeviceCards\.value\)/,
  '设备卡片可见集合必须直接使用全部 configuredDeviceCards。'
)
