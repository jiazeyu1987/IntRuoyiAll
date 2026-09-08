const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.resolve(moduleRoot, '..')
const readModule = (relativePath) => fs.readFileSync(path.join(moduleRoot, relativePath), 'utf8')
const readBackend = (relativePath) => fs.readFileSync(path.join(backendRoot, relativePath), 'utf8')

const workOrderDo = readModule('src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/workorder/MesProWorkOrderDO.java')
const respVo = readModule('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/vo/MesProWorkOrderRespVO.java')
const controller = readModule('src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/MesProWorkOrderController.java')
const syncService = readModule(
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java'
)
const migration = readBackend('sql/mysql/20260908_mes_pro_work_order_kingdee_demand_spec.sql')

assert.match(workOrderDo, /private\s+String\s+demandBillNo;/, '工单 DO 必须保存金蝶原始需求单据号。')
assert.match(workOrderDo, /private\s+String\s+materialSpecification;/, '工单 DO 必须保存金蝶规格型号快照。')
assert.match(respVo, /private\s+String\s+demandBillNo;/, '工单列表响应必须返回需求单据字段。')
assert.match(respVo, /private\s+String\s+productSpecification;/, '工单列表响应必须保留规格型号字段。')
assert.match(
  controller,
  /vo\.setProductSpecification\s*\(\s*workOrder\.getMaterialSpecification\(\)\s*\)/,
  '工单列表规格型号必须来自工单上的金蝶规格型号快照。'
)
assert.match(
  syncService,
  /setDemandBillNo\s*\(\s*resolveDemandBillNo\s*\(\s*productionOrder\s*\)\s*\)/,
  '金蝶同步创建/更新工单时必须写入原始 FSrcBillNo。'
)
assert.match(
  syncService,
  /setMaterialSpecification\s*\(\s*resolveMaterialSpecification\s*\(\s*productionOrder\s*\)\s*\)/,
  '金蝶同步创建/更新工单时必须写入 FMaterialId.FSpecification。'
)
assert.match(syncService, /payload\.put\("demandBillNo"/, '排产差异 payload 必须纳入需求单据快照。')
assert.match(syncService, /payload\.put\("materialSpecification"/, '排产差异 payload 必须纳入规格型号快照。')
assert.match(migration, /ADD COLUMN `demand_bill_no`/, '迁移必须增加 demand_bill_no。')
assert.match(migration, /ADD COLUMN `material_specification`/, '迁移必须增加 material_specification。')

console.log('PASS: workorder kingdee demand/spec backend contract')
