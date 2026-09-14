const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')

const workspaceRoot = path.resolve(__dirname, '../../..')
const backendRoot = path.resolve(workspaceRoot, 'IntRuoyiBackend')

const schemaPath = path.join(backendRoot, 'sql/mysql/20260512_mes_base_schema.sql')
const activeOrderSqlPath = path.join(backendRoot, 'sql/mysql/20260731_mes_process_pool_team_leader_p1_runtime_config.sql')
const activeOrderTransferTraceSqlPath = path.join(
  backendRoot,
  'sql/mysql/20260802_mes_process_pool_active_order_transfer_trace.sql'
)
const releaseServicePath = path.join(
  backendRoot,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)

const schemaSource = fs.readFileSync(schemaPath, 'utf8')
const activeOrderSql = fs.readFileSync(activeOrderSqlPath, 'utf8')
const activeOrderTransferTraceSql = fs.readFileSync(activeOrderTransferTraceSqlPath, 'utf8')
const releaseSource = fs.readFileSync(releaseServicePath, 'utf8')
const sourceBundle = `${schemaSource}\n${activeOrderSql}\n${activeOrderTransferTraceSql}`

assert.match(
  sourceBundle,
  /active_order.*transfer|transfer.*active_order|mes_pro_process_pool_active_order_transfer/i,
  'M4 requires a formal activeOrderId to transfer relation source.'
)
assert.match(
  sourceBundle,
  /active_order.*batch|batch.*active_order|active_order.*material_stock|material_stock.*active_order/i,
  'M4 requires activeOrderId material batch and inventory traceability.'
)
assert.doesNotMatch(
  releaseSource,
  /buildSourceNotIntegratedItem\([\s\S]*?(CHECK_INSPECTION_RESULT|CHECK_DEVIATION_CLOSED|CHECK_REWORK_CLOSED|CHECK_SCRAP_RECORDED|CHECK_INVENTORY_CONSISTENCY)/,
  'Release checks must read real sources, not source-not-integrated placeholders.'
)

console.log('PASS role-matrix transfer/start-check static contract')
