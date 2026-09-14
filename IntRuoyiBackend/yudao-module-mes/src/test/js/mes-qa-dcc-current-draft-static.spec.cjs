const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')

const mapperSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/qa/regulation/MesQaInspectionRegulationVersionMapper.java'
)
const serviceSource = read(
  'src/main/java/cn/iocoder/yudao/module/mes/service/qa/regulation/MesQaInspectionRegulationServiceImpl.java'
)

assert.match(mapperSource, /selectLatestDraftByRegulationId/)
assert.match(mapperSource, /getLifecycleStatus,\s*"DRAFT"/)
assert.match(serviceSource, /getCurrent\(Long dccProjectCodeId\)[\s\S]*selectLatestDraftByRegulationId/)
assert.match(serviceSource, /latestDraft\s*!=\s*null\s*\?\s*latestDraft/)

console.log('PASS: DCC QA current configuration restores the latest saved draft')
