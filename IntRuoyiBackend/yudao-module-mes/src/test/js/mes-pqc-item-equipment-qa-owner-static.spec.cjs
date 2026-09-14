const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const backendRoot = path.resolve(__dirname, '../../../..')
const read = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8').replace(/\r\n/g, '\n')

const controller = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/pqc/MesPqcItemEquipmentConfigController.java'
)
const service = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/pqc/MesPqcItemEquipmentConfigServiceImpl.java'
)
const response = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/pqc/vo/MesPqcItemEquipmentItemRespVO.java'
)
const batchSaveRequest = read(
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/pqc/vo/MesPqcItemEquipmentBatchConfigSaveReqVO.java'
)

assert.match(controller, /getConfigurableItems\([\s\S]*@RequestParam\("dccProjectCodeId"\) Long dccProjectCodeId\)/)
assert.match(controller, /getConfigurableItems[\s\S]*mes:qc-template:query/)
assert.match(controller, /mes:qc-template:query[\s\S]*public CommonResult<MesPqcItemEquipmentConfigRespVO> getConfig\(/)
assert.match(controller, /mes:qc-template:update[\s\S]*public CommonResult<MesPqcItemEquipmentConfigRespVO> saveConfig\(/)
assert.doesNotMatch(controller, /mes:pro-process-pool-team-leader:(query|maintain)/)
assert.match(response, /private Long dccProjectCodeId;/)
assert.match(response, /private List<String> itemCodes;/)
assert.match(controller, /@GetMapping\("\/config\/batch"\)/)
assert.match(controller, /dccProjectCodeId[\s\S]*itemCodes/)
assert.match(controller, /@PostMapping\("\/config\/batch"\)/)
assert.match(controller, /MesPqcItemEquipmentBatchConfigSaveReqVO/)
assert.match(batchSaveRequest, /private Long dccProjectCodeId;/)
assert.match(batchSaveRequest, /private List<String> itemCodes;/)
assert.match(service, /getItemConfig\(Long dccProjectCodeId, Collection<String> itemCodes\)/)
assert.match(service, /replaceItemConfigs\(MesPqcItemEquipmentBatchConfigSaveReqVO reqVO\)/)
assert.match(service, /@Transactional\(rollbackFor = Exception\.class\)[\s\S]*replaceItemConfigs/)
assert.match(service, /loadConfigurableItemMap\(reqVO\.getDccProjectCodeId\(\)\)/)
assert.match(service, /listConfigurableItems\(Long dccProjectCodeId\)/)
assert.match(service, /dccProjectCodeId != null[\s\S]*!Objects\.equals\(configurableItem\.dccProjectCodeId\(\), dccProjectCodeId\)[\s\S]*continue/)
assert.match(
  service,
  /if \(dccProjectCodeId != null\)[\s\S]*regulationById[\s\S]*!Objects\.equals\(regulation\.getDccProjectCodeId\(\), dccProjectCodeId\)[\s\S]*continue[\s\S]*toConfigurableItem\(/,
  'project-scoped loading must filter unrelated regulation rows before strict item parsing'
)
assert.match(service, /\.setDccProjectCodeId\(item\.dccProjectCodeId\(\)\)/)
assert.match(service, /dccProjectCodeId\(\),/)

console.log('PASS: PQC equipment configuration API is QA-owned and scoped by DCC project ID')
