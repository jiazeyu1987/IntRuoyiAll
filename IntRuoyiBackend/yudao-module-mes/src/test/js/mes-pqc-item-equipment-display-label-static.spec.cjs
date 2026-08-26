const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) =>
  fs.readFileSync(path.join(backendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const response = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/pqc/vo/MesPqcItemEquipmentItemRespVO.java'
)
const service = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/pqc/MesPqcItemEquipmentConfigServiceImpl.java'
)

assert.match(response, /private String projectName;/, 'The configurable-item API must expose the formal project name.')
assert.match(service, /import cn\.iocoder\.yudao\.module\.dcc\.dal\.mysql\.projectcode\.DccProjectCodeMapper;/)
assert.match(service, /import cn\.iocoder\.yudao\.module\.mes\.dal\.mysql\.qa\.regulation\.MesQaInspectionRegulationVersionMapper;/)
assert.match(service, /import cn\.iocoder\.yudao\.module\.mes\.dal\.mysql\.qa\.regulation\.MesQaInspectionRegulationMapper;/)
assert.match(service, /\.setProjectName\(item\.projectName\(\)\)/, 'The API must bind the project name from the selected item source.')
assert.match(service, /private record ConfigurableItem\(/, 'The service must keep display names and internal item identity together.')
assert.match(service, /DccProjectCodeDO project = projectById\.get\(regulation\.getDccProjectCodeId\(\)\);/)
assert.match(service, /requireDisplayName\(project\.getProjectName\(\)/, 'A missing formal project name must fail instead of falling back to an item code.')
assert.match(service, /requireDisplayName\(row\.getItemName\(\)/, 'A missing inspection name must fail instead of falling back to an item code.')
assert.match(service, /!Objects\.equals\(existing\.projectName\(\), configurableItem\.projectName\(\)\)/, 'An item code mapped to another project must fail rather than display another project name.')

console.log('PASS: PQC configurable item API supplies an unambiguous current-project display name')
