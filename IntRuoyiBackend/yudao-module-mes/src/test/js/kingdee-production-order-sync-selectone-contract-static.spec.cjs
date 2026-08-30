const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const moduleRoot = path.resolve(__dirname, '../../..')
const servicePath = path.join(
  moduleRoot,
  'src/main/java/cn/iocoder/yudao/module/mes/service/pro/workorder/sync/MesKingdeeProductionOrderSyncServiceImpl.java'
)
const source = fs.readFileSync(servicePath, 'utf8')

assert.match(
  source,
  /import org\.apache\.ibatis\.exceptions\.TooManyResultsException;/,
  '生产工单同步必须识别 MyBatis selectOne 多结果异常'
)
assert.match(
  source,
  /KINGDEE_PRODUCTION_ORDER_RESPONSE_INVALID/,
  '生产工单同步必须使用生产订单响应错误码'
)
assert.doesNotMatch(
  source,
  /KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID/,
  '生产工单同步不能继续使用采购订单响应错误码'
)
assert.match(
  source,
  /catch\s*\(\s*TooManyResultsException\s+\w+\s*\)/,
  '重复唯一键必须转为业务可读异常，而不是透出底层 selectOne 异常'
)
assert.match(source, /生产订单同步记录重复/, '同步记录重复必须有业务可读 blocker')
assert.match(source, /生产工单编码重复/, '生产工单编码重复必须有业务可读 blocker')
assert.match(source, /物料编码重复/, '物料编码重复必须有业务可读 blocker')
assert.match(source, /物料分类编码重复/, '物料分类编码重复必须有业务可读 blocker')
assert.match(source, /计量单位编码重复/, '计量单位编码重复必须有业务可读 blocker')
assert.match(source, /计量单位名称重复/, '计量单位名称重复必须有业务可读 blocker')
assert.match(source, /排产工单有效记录重复/, '排产工单有效记录重复必须有业务可读 blocker')
assert.doesNotMatch(
  source,
  /selectFirstOne\s*\(/,
  '禁止通过 selectFirstOne 或任意第一条绕过重复数据'
)

console.log('kingdee-production-order-sync-selectone-contract-static PASS')
