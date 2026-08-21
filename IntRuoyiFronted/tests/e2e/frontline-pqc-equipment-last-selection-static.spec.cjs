const assert = require('node:assert')
const fs = require('node:fs')
const path = require('node:path')

const root = process.cwd()
const read = (relativePath) => fs.readFileSync(path.join(root, relativePath), 'utf8').replace(/\r\n/g, '\n')

const apiSource = read('src/api/mes/pro/feedback/index.ts')
const panelSource = read('src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue')
const backendRespSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcProcessRespVO.java'
)
const backendServiceSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const pieceMapperSource = read(
  '../IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/pqc/MesPqcInspectionPieceDetailMapper.java'
)

const between = (source, startToken, endToken) => {
  const start = source.indexOf(startToken)
  assert.ok(start >= 0, `missing start token: ${startToken}`)
  const end = source.indexOf(endToken, start)
  assert.ok(end > start, `missing end token after ${startToken}: ${endToken}`)
  return source.slice(start, end)
}

assert.match(
  backendRespSource,
  /private Long lastSelectedEquipmentId;[\s\S]*private String lastSelectedEquipmentNumber;/,
  'PQC process response item must expose the current employee last-selected equipment.'
)
assert.match(
  apiSource,
  /lastSelectedEquipmentId\?: number[\s\S]*lastSelectedEquipmentNumber\?: string/,
  'PQC frontend API item contract must include last-selected equipment fields.'
)
assert.match(
  pieceMapperSource,
  /selectLatestSelectedEquipmentByActualEmployeeAndItemCode/,
  'Backend must query the latest selected equipment by actual employee and inspection item.'
)
assert.match(
  pieceMapperSource,
  /e\.actual_employee_id = #\{actualEmployeeId\}[\s\S]*d\.item_code = #\{itemCode\}/,
  'Latest equipment query must be scoped by actual employee and inspection item code.'
)
assert.match(
  backendServiceSource,
  /applyLastSelectedEquipmentDefaults\(/,
  'PQC process response assembly must apply last selected equipment defaults.'
)
assert.match(
  backendServiceSource,
  /requirePqcEmployee\(loginUserId, actualEmployeeId\)/,
  'Last equipment default lookup must verify the requested actual PQC employee.'
)
assert.match(
  panelSource,
  /lastSelectedEquipmentId\?: number[\s\S]*lastSelectedEquipmentNumber\?: string/,
  'PQC local item model must preserve last-selected equipment fields.'
)
assert.match(
  panelSource,
  /applyPqcItemEquipmentDefaults\(/,
  'PQC page must apply backend-provided last selected equipment to item selections.'
)
const applyDefaultsBlock = between(
  panelSource,
  'const applyPqcItemEquipmentDefaults',
  'const getPqcItemSelection'
)
assert.match(
  applyDefaultsBlock,
  /item\.lastSelectedEquipmentId[\s\S]*item\.lastSelectedEquipmentNumber/,
  'Default application must read both last selected equipment id and number.'
)
assert.match(
  applyDefaultsBlock,
  /option\.equipmentId === item\.lastSelectedEquipmentId[\s\S]*option\.equipmentNumber === item\.lastSelectedEquipmentNumber/,
  'Default equipment must still be validated against current formal equipment options.'
)
assert.match(
  applyDefaultsBlock,
  /if \(selection\.selectedEquipmentId \|\| selection\.selectedEquipmentNumber\)/,
  'Backend defaults must not overwrite an operator selection already made in the current draft.'
)

console.log('PASS: frontline PQC equipment last selection static contract')
