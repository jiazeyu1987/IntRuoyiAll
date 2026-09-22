const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const root = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const service = read('yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java')

const sliceMethod = (source, signature) => {
  const start = source.indexOf(signature)
  assert.ok(start >= 0, `Missing method ${signature}`)
  const next = source.indexOf('\n    private ', start + signature.length)
  assert.ok(next > start, `Cannot slice method ${signature}`)
  return source.slice(start, next)
}

const copyConfiguredOptions = sliceMethod(
  service,
  'private static List<MesFrontlinePqcProcessRespVO.PqcInspectionItem> copyInspectionItemsWithDeviceParameters('
)
const resolveSubmittedItems = sliceMethod(
  service,
  'private List<MesFrontlinePqcInspectionItem> resolveSubmittedInspectionItems('
)

assert.match(
  copyConfiguredOptions,
  /source\.getEquipmentOptions\(\)[\s\S]*\.map\(option -> copyEquipmentOptionWithParameters\([\s\S]*routeDeviceContext\.parametersByDeviceCode\(\)/,
  'Frontline PQC payload must preserve configured equipment options and only attach route parameters when the code matches.'
)
assert.doesNotMatch(
  copyConfiguredOptions,
  /filter\(option -> routeDeviceContext\.deviceCodes\(\)\.contains\([\s\S]*normalizeEquipmentCode\(option\.getEquipmentCode\(\)\)/,
  'Frontline PQC display options must not be hidden just because the frozen production route devices use different formal equipment codes.'
)
assert.doesNotMatch(
  resolveSubmittedItems,
  /filter\(option -> routeDeviceContext\.deviceCodes\(\)\.contains\([\s\S]*normalizeEquipmentCode\(option\.equipmentCode\(\)\)/,
  'Frontline PQC submit validation must accept configured PQC item equipment even when it is not a production route device.'
)
assert.match(
  service,
  /copyEquipmentOptionWithParameters\([\s\S]*parametersByDeviceCode\.getOrDefault\([\s\S]*normalizeEquipmentCode\(source\.getEquipmentCode\(\)\), List\.of\(\)\)/,
  'Route-owned device parameters must remain attached only by matching formal equipment code.'
)

console.log('PASS: frontline PQC equipment visibility follows item equipment config')
