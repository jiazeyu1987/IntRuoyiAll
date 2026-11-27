const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const repoRoot = path.resolve(__dirname, '../..')
const readUtf8 = (relativePath) =>
  fs.readFileSync(path.join(repoRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const page = readUtf8('src/views/mes/pro/processpool/QaRegulationPage.vue')

const service = fs.readFileSync(path.join(repoRoot, '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'), 'utf8')
const flatten = page.slice(page.indexOf('const flattenQaRegulationProcesses'), page.indexOf('const resetQaRegulationConfiguration'))
assert.match(flatten, /equipmentOptions:\s*\(item\.equipmentOptions \|\| \[\]\)\.map\(/,
  'Historical equipment returned by the version API must survive page mapping.')
const publish = service.slice(service.indexOf('public MesQaInspectionRegulationPublishedVersionRespVO publish('),
  service.indexOf('public MesQaInspectionRegulationPublishedVersionRespVO getPublishedVersion('))
assert.match(publish, /listEnabledEquipmentOptionsByProjectVersionAndItemCodes\(/,
  'Publication must freeze the effective configuration for this project and version.')
assert.match(publish, /item\.setEquipmentOptions\(/)
assert.match(publish, /setSnapshotJson\(JSON\.toJSONString\(reqVO\)\)/)
assert.match(publish, /setSnapshotJson\(context\.version\(\)\.getSnapshotJson\(\)\)/,
  'The equipment snapshot must be persisted with the published version.')

console.log('qa-regulation-equipment-options-static PASS')
