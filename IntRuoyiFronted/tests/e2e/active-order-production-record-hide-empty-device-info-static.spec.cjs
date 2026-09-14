const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')

const repoRoot = resolve(__dirname, '../../..')
const component = readFileSync(
  resolve(
    repoRoot,
    'IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue'
  ),
  'utf8'
)

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  component.includes('hasProductionRecordDeviceIdentity'),
  '生产记录表单必须用正式设备身份判断输出物料是否真的用到设备'
)

assert(
  /const buildProductionRecordMaterialDeviceGroups[\s\S]*if \(!devices\.length\) \{\s*continue\s*\}/.test(
    component
  ),
  '没有设备身份的输出物料不能生成“未记录/暂无设备参数”的伪设备信息块'
)

assert(
  !component.includes('missing-device-group') && !component.includes('missing-device'),
  '生产记录表单不能保留 missing-device 伪设备分组'
)

assert(
  /recordRow\.deviceParameters[\s\S]*hasProductionRecordDeviceIdentity\(parameter\)/.test(component),
  '仅当设备参数本身带设备 ID、编号或名称时，才允许从参数反建设备分组'
)

console.log('PASS: active order production record hides empty device info static contract')
