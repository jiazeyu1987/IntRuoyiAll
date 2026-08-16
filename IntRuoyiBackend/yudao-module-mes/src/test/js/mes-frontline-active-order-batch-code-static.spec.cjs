const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const javaRoot = path.join(moduleRoot, 'src/main/java/cn/iocoder/yudao/module/mes')
const read = (relativePath) => fs.readFileSync(path.join(javaRoot, relativePath), 'utf8')

const workOrder = read('dal/dataobject/pro/workorder/MesProWorkOrderDO.java')
const activeOrderRow = read('service/pro/processpool/team/MesTeamLeaderActiveOrderRow.java')
const activeOrderService = read('service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java')
const candidate = read('service/pro/frontline/MesFrontlineActiveOrderCandidate.java')
const pqcContext = read('service/pro/frontline/MesFrontlinePqcContextServiceImpl.java')
const responseVo = read('controller/admin/pro/feedback/vo/frontline/MesFrontlineActiveOrderRespVO.java')
const controller = read('controller/admin/pro/feedback/MesFrontlineDeviceAccountController.java')

assert.match(workOrder, /private String batchCode;/, 'production work order must own the formal batch code.')
assert.match(activeOrderRow, /private String batchCode;/, 'active-order row must carry the work-order batch code.')
assert.match(
  activeOrderService,
  /\.setBatchCode\(workOrder\.getBatchCode\(\)\)/,
  'active-order service must project batchCode from the formal production work order.'
)
assert.match(candidate, /String batchCode,/, 'PQC active-order candidate must carry the formal batch code.')
assert.match(
  pqcContext,
  /workOrder\.getBatchCode\(\),\s*workOrder\.getQuantity\(\)/,
  'PQC candidate projection must preserve the same formal work-order batch code.'
)
assert.match(responseVo, /private String batchCode;/, 'frontline active-order response must expose batchCode.')
assert.match(
  controller,
  /respVO\.setBatchCode\(candidate\.batchCode\(\)\)/,
  'PQC response mapping must expose the candidate batch code.'
)
assert.match(
  controller,
  /\.setBatchCode\(activeOrder\.getBatchCode\(\)\)/,
  'production response mapping must expose the active-order batch code.'
)

console.log('PASS: frontline active-order batch code comes from the formal production work order')
