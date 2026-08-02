const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')

const TASK_ID = '20260801-role-requirement-matrix-implementation'
const DATA_PREFIX = 'RRM-20260801-'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const FRONTEND_ROOT = path.resolve(WORKSPACE_ROOT, 'IntRuoyiFronted')
const BACKEND_ROOT = path.resolve(WORKSPACE_ROOT, 'IntRuoyiBackend')
const TASK_DIR = path.resolve(WORKSPACE_ROOT, 'doc/tasks', TASK_ID)
const RESULT_DIR = path.resolve(FRONTEND_ROOT, 'test-results', 'role-requirement-matrix-real-flow')
const EVIDENCE_FILE = path.resolve(TASK_DIR, 'role-requirement-matrix-real-e2e-evidence.md')

const ACTIVE_ORDER_DO = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolActiveOrderDO.java'
)
const ACTIVE_ORDER_SQL = path.resolve(
  BACKEND_ROOT,
  'sql/mysql/20260731_mes_process_pool_team_leader_p1_runtime_config.sql'
)
const ACTIVE_ORDER_AUTHORITY_SQL = path.resolve(
  BACKEND_ROOT,
  'sql/mysql/20260802_mes_process_pool_active_order_authority.sql'
)
const ACTIVE_ORDER_PROCESS_SNAPSHOT_DO = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/team/MesProcessPoolActiveOrderProcessSnapshotDO.java'
)
const ACTIVE_ORDER_PROCESS_SNAPSHOT_SQL = path.resolve(
  BACKEND_ROOT,
  'sql/mysql/20260802_mes_process_pool_active_order_process_snapshot.sql'
)
const PQC_CONTEXT_SERVICE = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java'
)
const FRONTLINE_FIXED_TEMPLATE_PANEL = path.resolve(
  FRONTEND_ROOT,
  'src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue'
)
const RELEASE_SERVICE = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImpl.java'
)
const ROUTE_FLOW_PROCESS_CONFIG_DO = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/route/MesProRouteFlowProcessConfigDO.java'
)
const SCHEDULE_ORDER_PROCESS_DO = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/scheduleorder/MesProScheduleOrderProcessDO.java'
)
const SCHEDULE_ORDER_SERVICE = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/scheduleorder/MesProScheduleOrderServiceImpl.java'
)
const AUTO_SCHEDULE_SERVICE = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/schedule/MesProAutoScheduleServiceImpl.java'
)
const BATCH_RECORD_BINDING_DO = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/route/MesProRouteFlowProcessBatchRecordDO.java'
)
const EDHR_BATCH_EXECUTION_SERVICE = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
)
const ROUTE_FLOW_DESIGNER = path.resolve(
  FRONTEND_ROOT,
  'src/views/mes/pro/route/RouteFlowGraphDesigner.vue'
)
const MES_BASE_SCHEMA = path.resolve(BACKEND_ROOT, 'sql/mysql/20260512_mes_base_schema.sql')
const KINGDEE_MATERIAL_LIST_SQL = path.resolve(BACKEND_ROOT, 'sql/mysql/20260613_mes_kingdee_production_material_list.sql')

const REQUIRED_ENV = [
  ['RRM_FRONTEND_URL', '真实前端入口，例如 http://127.0.0.1:8081。'],
  ['RRM_BACKEND_URL', '真实后端入口，例如 http://127.0.0.1:48081。'],
  ['RRM_TENANT', '任务专用非生产测试租户。'],
  ['RRM_PRODUCTION_EMPLOYEE_USERNAME', '生产员工账号标签。'],
  ['RRM_PRODUCTION_EMPLOYEE_PASSWORD', '生产员工密码，通过环境变量注入，不写入证据。'],
  ['RRM_PRODUCTION_LEADER_USERNAME', '生产组长账号标签。'],
  ['RRM_PRODUCTION_LEADER_PASSWORD', '生产组长密码，通过环境变量注入，不写入证据。'],
  ['RRM_QA_USERNAME', 'QA 账号标签。'],
  ['RRM_QA_PASSWORD', 'QA 密码，通过环境变量注入，不写入证据。'],
  ['RRM_PQC_INSPECTOR_USERNAME', 'PQC 检验员账号标签。'],
  ['RRM_PQC_INSPECTOR_PASSWORD', 'PQC 检验员密码，通过环境变量注入，不写入证据。'],
  ['RRM_PQC_LEADER_USERNAME', 'PQC 组长账号标签。'],
  ['RRM_PQC_LEADER_PASSWORD', 'PQC 组长密码，通过环境变量注入，不写入证据。'],
  ['RRM_RELEASE_OWNER_USERNAME', '放行负责人账号标签。'],
  ['RRM_RELEASE_OWNER_PASSWORD', '放行负责人密码，通过环境变量注入，不写入证据。'],
  ['RRM_SIGNATURE_IDS_JSON', '六类角色正式电子签名 ID 映射 JSON。'],
  ['RRM_PRODUCTION_ORDER_ID', '任务专用 ERP/MES 生产订单 ID。'],
  ['RRM_PRODUCTION_ORDER_CODE', `任务专用生产订单编码，建议以 ${DATA_PREFIX} 开头。`],
  ['RRM_ROUTE_ID', '正式工艺路线 ID。'],
  ['RRM_ROUTE_VERSION_ID', '正式工艺路线版本 ID。'],
  ['RRM_ROUTE_PROCESS_ID_1', '系数 1.0 的正式路线工序 ID。'],
  ['RRM_ROUTE_PROCESS_ID_2', '系数 3.0 的正式路线工序 ID。'],
  ['RRM_TRANSFER_IDS', '任务专用调拨/发货/补料/退料正式 ID 列表。'],
  ['RRM_BATCH_RECORD_REPORT_ID', '正式逐工序批记录报表 ID。'],
  ['RRM_QA_REGULATION_VERSION_ID', '已发布 QA 规程版本 ID。']
]

const ROLE_CONFIGS = [
  ['productionEmployee', 'RRM_PRODUCTION_EMPLOYEE_USERNAME', 'RRM_PRODUCTION_EMPLOYEE_PASSWORD', '/index'],
  ['productionLeader', 'RRM_PRODUCTION_LEADER_USERNAME', 'RRM_PRODUCTION_LEADER_PASSWORD', '/mes/pro/process-pool/team-leader'],
  ['qa', 'RRM_QA_USERNAME', 'RRM_QA_PASSWORD', '/mes/qc/template'],
  ['pqcInspector', 'RRM_PQC_INSPECTOR_USERNAME', 'RRM_PQC_INSPECTOR_PASSWORD', '/index'],
  ['pqcLeader', 'RRM_PQC_LEADER_USERNAME', 'RRM_PQC_LEADER_PASSWORD', '/mes/pro/process-pool/team-leader'],
  ['releaseOwner', 'RRM_RELEASE_OWNER_USERNAME', 'RRM_RELEASE_OWNER_PASSWORD', '/mes/pro/feedback/edhr-release']
]

const FORBIDDEN_TENANT_FRAGMENTS = ['prod', 'production', '正式', '生产', '芋道源码', 'admin']
const LOCAL_BASELINE_TENANT_AUTHORIZATION = 'USER_APPROVED_YUDAO_SOURCE_20260802'
const SENSITIVE_KEY_PATTERN = /PASSWORD|TOKEN|SECRET|SIGNATURE_IDS_JSON/i

function parseArgs(argv) {
  return {
    checkOnly: argv.includes('--check'),
    headed: argv.includes('--headed') || process.env.RRM_HEADED === '1'
  }
}

function envValue(key) {
  return (process.env[key] || '').trim()
}

function readText(filePath) {
  if (!fs.existsSync(filePath)) {
    failFast(`缺少源文件：${path.relative(WORKSPACE_ROOT, filePath)}`)
  }
  return fs.readFileSync(filePath, 'utf8')
}

function failFast(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  throw error
}

function sanitizeUrl(value) {
  return value.replace(/\/+$/, '')
}

function collectConfig() {
  const frontendUrl = sanitizeUrl(envValue('RRM_FRONTEND_URL'))
  const backendUrl = sanitizeUrl(envValue('RRM_BACKEND_URL'))
  return {
    frontendUrl,
    backendUrl,
    tenant: envValue('RRM_TENANT'),
    dataPrefix: envValue('RRM_DATA_PREFIX') || DATA_PREFIX,
    headed: process.env.RRM_HEADED === '1',
    browserPath: envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH'),
    roles: Object.fromEntries(
      ROLE_CONFIGS.map(([roleKey, usernameKey, passwordKey, targetPath]) => [
        roleKey,
        {
          username: envValue(usernameKey),
          password: envValue(passwordKey),
          targetPath
        }
      ])
    )
  }
}

function collectEnvBlockers(config) {
  const blockers = []
  for (const [key, description] of REQUIRED_ENV) {
    if (!envValue(key)) {
      blockers.push({
        key,
        category: 'ENV',
        description
      })
    }
  }

  for (const key of [
    'RRM_PRODUCTION_ORDER_ID',
    'RRM_ROUTE_ID',
    'RRM_ROUTE_VERSION_ID',
    'RRM_ROUTE_PROCESS_ID_1',
    'RRM_ROUTE_PROCESS_ID_2'
  ]) {
    const value = Number(envValue(key))
    if (!Number.isFinite(value) || value <= 0) {
      blockers.push({
        key,
        category: 'ENV',
        description: `${key} 必须是大于 0 的正式业务 ID。`
      })
    }
  }

  if (config.dataPrefix !== DATA_PREFIX) {
    blockers.push({
      key: 'RRM_DATA_PREFIX',
      category: 'ENV',
      description: `任务数据前缀必须固定为 ${DATA_PREFIX}，不能改成不可追踪前缀。`
    })
  }

  if (
    config.tenant
    && FORBIDDEN_TENANT_FRAGMENTS.some((fragment) => config.tenant.toLowerCase().includes(fragment.toLowerCase()))
    && !isAuthorizedLocalBaselineTenant(config)
  ) {
    blockers.push({
      key: 'RRM_TENANT',
      category: 'ENV',
      description: '测试租户命中生产或 admin 基线口径；若使用芋道源码本地租户，必须提供用户显式授权令牌。'
    })
  }

  if (!isAllowedRuntimePair(config.frontendUrl, config.backendUrl)) {
    blockers.push({
      key: 'RRM_FRONTEND_URL/RRM_BACKEND_URL',
      category: 'ENV',
      description: '前后端 URL 必须成对使用 int_main 8081/48081，或同一 worktree slot 的 8082-8100/48082-48100。'
    })
  }

  return blockers
}

function isAuthorizedLocalBaselineTenant(config) {
  return config.tenant === '芋道源码'
    && envValue('RRM_LOCAL_BASELINE_TENANT_AUTHORIZATION') === LOCAL_BASELINE_TENANT_AUTHORIZATION
}

function isAllowedRuntimePair(frontendUrl, backendUrl) {
  if (!frontendUrl || !backendUrl) return false
  let frontend
  let backend
  try {
    frontend = new URL(frontendUrl)
    backend = new URL(backendUrl)
  } catch {
    return false
  }
  const frontendPort = Number(frontend.port)
  const backendPort = Number(backend.port)
  const frontendHostAllowed = ['127.0.0.1', 'localhost'].includes(frontend.hostname)
  const backendHostAllowed = ['127.0.0.1', 'localhost'].includes(backend.hostname)
  if (!frontendHostAllowed || !backendHostAllowed) return false
  if (frontendPort === 8081 && backendPort === 48081) return true
  return frontendPort >= 8082 && frontendPort <= 8100 && backendPort === frontendPort + 40000
}

function collectSourceBlockers() {
  const blockers = []
  const activeOrderSource = readText(ACTIVE_ORDER_DO)
  const activeOrderSql = readText(ACTIVE_ORDER_SQL)
  const activeOrderAuthoritySql = readText(ACTIVE_ORDER_AUTHORITY_SQL)
  const pqcSource = readText(PQC_CONTEXT_SERVICE)
  const releaseSource = readText(RELEASE_SERVICE)

  if (!activeOrderSource.includes('@TableName("mes_pro_process_pool_active_order")')) {
    blockers.push({
      key: 'mes_pro_process_pool_active_order',
      category: 'SOURCE',
      description: '活跃订单 DO 未绑定正式活跃订单表。'
    })
  }
  for (const field of ['routeId', 'routeVersionId', 'erpFixedQuantitySnapshot', 'businessStatus', 'version']) {
    if (!new RegExp(`private\\s+.*\\s+${field}\\b`).test(activeOrderSource)) {
      blockers.push({
        key: `MesProcessPoolActiveOrderDO.${field}`,
        category: 'SOURCE',
        description: `统一 activeOrderId 仍缺 ${field} 字段。`
      })
    }
  }
  const oldLeaderScopedActiveOrderKey =
    /UNIQUE KEY `uk_mes_pp_active_order` \(`tenant_id`, `leader_user_id`, `work_order_id`, `deleted`\)/
  const newRouteVersionActiveOrderKey =
    /UNIQUE KEY `uk_mes_pp_active_order` \(`tenant_id`, `work_order_id`, `route_id`, `route_version_id`, `deleted`\)/
  const dropsOldActiveOrderKey = /DROP INDEX `uk_mes_pp_active_order`/
  if (oldLeaderScopedActiveOrderKey.test(activeOrderSql)
    && (!newRouteVersionActiveOrderKey.test(activeOrderAuthoritySql)
      || !dropsOldActiveOrderKey.test(activeOrderAuthoritySql))) {
    blockers.push({
      key: 'uk_mes_pp_active_order',
      category: 'SOURCE',
      description: '活跃订单唯一键仍绑定 leader_user_id，不能作为跨角色统一订单身份。'
    })
  }
  if (pqcSource.includes('processPoolMapper.selectActiveList')) {
    blockers.push({
      key: 'processPoolMapper.selectActiveList',
      category: 'SOURCE',
      description: 'PQC 仍通过 mes_pro_process_pool 活跃行读取订单，未切换统一 activeOrderId。'
    })
  }
  for (const checkCode of [
    'CHECK_INSPECTION_RESULT',
    'CHECK_DEVIATION_CLOSED',
    'CHECK_REWORK_CLOSED',
    'CHECK_SCRAP_RECORDED',
    'CHECK_INVENTORY_CONSISTENCY'
  ]) {
    const pattern = new RegExp(`buildSourceNotIntegratedItem\\([^\\n]+${checkCode}`)
    if (pattern.test(releaseSource)) {
      blockers.push({
        key: checkCode,
        category: 'SOURCE',
        description: `${checkCode} 仍由 buildSourceNotIntegratedItem 生成，放行正式来源未接入。`
      })
    }
  }
  return [
    ...blockers,
    ...collectErpRelationBlockers(),
    ...collectQaRegulationBlockers(),
    ...collectPqcSubmissionBlockers(),
    ...collectPqcFrontendBlockers(),
    ...collectProductionCoefficientBlockers(),
    ...collectBatchRecordBindingBlockers()
  ]
}

function collectErpRelationBlockers() {
  const blockers = []
  const mesBaseSchema = readText(MES_BASE_SCHEMA)
  const kingdeeMaterialSql = readText(KINGDEE_MATERIAL_LIST_SQL)
  const activeOrderSql = readText(ACTIVE_ORDER_SQL)

  for (const tableName of [
    'mes_kingdee_production_material_list',
    'mes_wm_transfer',
    'mes_wm_transfer_detail',
    'mes_wm_transfer_line',
    'mes_wm_batch',
    'mes_wm_material_stock'
  ]) {
    if (!mesBaseSchema.includes(tableName) && !kingdeeMaterialSql.includes(tableName)) {
      blockers.push({
        key: tableName,
        category: 'SOURCE',
        description: `M0 source map 未找到 ${tableName}，无法证明 ERP/物料/调拨/批次基础来源存在。`
      })
    }
  }

  for (const relation of [
    ['activeOrderTransferRelation', /active_order.*transfer|transfer.*active_order|mes_pro_process_pool_active_order_transfer/i, '缺少 activeOrderId 与调拨头/行的正式关系表或迁移。'],
    ['activeOrderShipmentSource', /active_order.*shipment|shipment.*active_order|delivery.*active_order|active_order.*delivery/i, '缺少 activeOrderId 与发货/交付事实的正式关系源。'],
    ['activeOrderReplenishmentReturnSource', /active_order.*replenish|replenish.*active_order|active_order.*return|return.*active_order/i, '缺少 activeOrderId 与补料/退料事实的正式关系源。'],
    ['activeOrderBatchTraceSource', /active_order.*batch|batch.*active_order|active_order.*material_stock|material_stock.*active_order/i, '缺少 activeOrderId 与物料批次/库存追溯的正式关系源。']
  ]) {
    const [key, pattern, description] = relation
    if (!pattern.test(activeOrderSql) && !pattern.test(mesBaseSchema) && !pattern.test(kingdeeMaterialSql)) {
      blockers.push({
        key,
        category: 'SOURCE',
        description
      })
    }
  }

  return blockers
}

function collectQaRegulationBlockers() {
  const blockers = []
  const mesBaseSchema = readText(MES_BASE_SCHEMA)
  const pqcSource = readText(PQC_CONTEXT_SERVICE)
  const activeOrderSource = readText(ACTIVE_ORDER_DO)
  const sourceBundle = `${mesBaseSchema}\n${pqcSource}\n${activeOrderSource}`

  for (const check of [
    ['qaRegulationOwnership', /qa.*inspection.*regulation|inspection_regulation|mes_qa_regulation|qms.*regulation/i, 'QA 规程唯一所有权和正式表/API 未冻结。'],
    ['qaRegulationVersionModel', /regulation_version|regulationVersionId|qa.*version|inspection.*version/i, 'QA 规程发布版本模型未确认，不能证明发布后不可原地修改。'],
    ['pqcTaskModel', /pqc_task|PqcTask|inspectionType|businessDate|shiftCode|roundNo|regulationVersionId/i, 'PQC 任务身份模型未具备检验类型、日期、班次、轮次和规程版本。'],
    ['pqcPieceDetailModel', /pqc.*detail|piece.*detail|inspection.*detail|sampleNo|itemResult/i, 'PQC 逐件明细正式模型未确认，不能证明逐件可还原。']
  ]) {
    const [key, pattern, description] = check
    if (!pattern.test(sourceBundle)) {
      blockers.push({
        key,
        category: 'SOURCE',
        description
      })
    }
  }

  return blockers
}

function collectPqcSubmissionBlockers() {
  const blockers = []
  const pqcSource = readText(PQC_CONTEXT_SERVICE)
  if (pqcSource.includes('selectActiveByWorkOrderRouteProcess')) {
    blockers.push({
      key: 'selectActiveByWorkOrderRouteProcess',
      category: 'SOURCE',
      description: 'PQC 提交仍依赖最新 mes_pro_process_pool 生产事件，未按统一 activeOrderId 和发布规程任务独立提交。'
    })
  }
  return blockers
}

function collectPqcFrontendBlockers() {
  const blockers = []
  const frontendSource = readText(FRONTLINE_FIXED_TEMPLATE_PANEL)
  const hardcodedItemPattern =
    /type\s+PqcInspectionItemKey\s*=\s*'length'\s*\|\s*'appearance'\s*\|\s*'seal'\s*\|\s*'pressure'|PQC_INSPECTION_ITEMS\s*=\s*\{|length:\s*\{|appearance:\s*\{|seal:\s*\{|pressure:\s*\{/
  if (hardcodedItemPattern.test(frontendSource)) {
    blockers.push({
      key: 'hardcodedPqcInspectionItems',
      category: 'SOURCE',
      description: 'PQC 前端仍硬编码 length/appearance/seal/pressure 检验项目，未按发布规程动态渲染。'
    })
  }
  if (/inspectionType:\s*'PATROL'/.test(frontendSource)) {
    blockers.push({
      key: 'defaultPqcInspectionType',
      category: 'SOURCE',
      description: 'PQC 前端仍默认 PATROL，未从规程任务身份读取检验类型。'
    })
  }
  if (/inspectionQuantity:\s*30\b/.test(frontendSource)) {
    blockers.push({
      key: 'defaultPqcInspectionQuantity',
      category: 'SOURCE',
      description: 'PQC 前端仍默认检验数量 30，未从规程任务计划数量读取。'
    })
  }
  if (/scrapQuantity:\s*1\b/.test(frontendSource)) {
    blockers.push({
      key: 'defaultPqcScrapQuantity',
      category: 'SOURCE',
      description: 'PQC 前端仍默认损耗数量 1，未由实际检验结果或规程规则驱动。'
    })
  }
  return blockers
}

function collectProductionCoefficientBlockers() {
  const blockers = []
  const routeFlowProcessConfigSource = readText(ROUTE_FLOW_PROCESS_CONFIG_DO)
  const scheduleOrderProcessSource = readText(SCHEDULE_ORDER_PROCESS_DO)
  const activeOrderSource = readText(ACTIVE_ORDER_DO)
  const activeOrderSql = readText(ACTIVE_ORDER_SQL)
  const activeOrderProcessSnapshotSource = readText(ACTIVE_ORDER_PROCESS_SNAPSHOT_DO)
  const activeOrderProcessSnapshotSql = readText(ACTIVE_ORDER_PROCESS_SNAPSHOT_SQL)
  const scheduleOrderServiceSource = readText(SCHEDULE_ORDER_SERVICE)
  const autoScheduleServiceSource = readText(AUTO_SCHEDULE_SERVICE)

  if (!routeFlowProcessConfigSource.includes('productionQuantityFactor')) {
    blockers.push({
      key: 'MesProRouteFlowProcessConfigDO.productionQuantityFactor',
      category: 'SOURCE',
      description: '工艺路线工序配置缺少 productionQuantityFactor，无法冻结生产数量系数来源。'
    })
  }
  if (!scheduleOrderProcessSource.includes('productionQuantityFactor') || !scheduleOrderProcessSource.includes('plannedQuantity')) {
    blockers.push({
      key: 'MesProScheduleOrderProcessDO.productionQuantityFactor',
      category: 'SOURCE',
      description: '排产工序快照缺少生产系数或计划数量字段，无法证明系数分配已冻结。'
    })
  }
  const activeOrderTargetSnapshotSource = activeOrderSource + activeOrderSql + activeOrderProcessSnapshotSource + activeOrderProcessSnapshotSql
  if (!/productionQuantityFactor|production_quantity_factor/.test(activeOrderTargetSnapshotSource)) {
    blockers.push({
      key: 'activeOrderProductionQuantityFactorSnapshot',
      category: 'SOURCE',
      description: '统一 activeOrderId 模型未保存生产系数快照，后续分配/完成/PQC 无法以同一订单身份复核系数。'
    })
  }
  if (!/plannedQuantity|planned_quantity/.test(activeOrderTargetSnapshotSource)) {
    blockers.push({
      key: 'activeOrderPlannedQuantitySnapshot',
      category: 'SOURCE',
      description: '统一 activeOrderId 模型未保存按生产系数计算后的计划数量快照。'
    })
  }
  if (/productionQuantityFactor\s*==\s*null\s*\?[\s\S]{0,120}DEFAULT_PRODUCTION_QUANTITY_FACTOR/.test(autoScheduleServiceSource)) {
    blockers.push({
      key: 'defaultProductionQuantityFactorInAutoSchedule',
      category: 'SOURCE',
      description: '自动排产仍在生产系数缺失时默认使用 DEFAULT_PRODUCTION_QUANTITY_FACTOR，M2 前必须改为正式配置缺失即失败。'
    })
  }
  if (!scheduleOrderServiceSource.includes('configuredSnapshot.payload.put("productionQuantityFactor"')) {
    blockers.push({
      key: 'scheduleOrderProductionQuantityFactorSnapshot',
      category: 'SOURCE',
      description: '排产订单生成未写入生产系数快照，无法审计系数来源。'
    })
  }
  return blockers
}

function collectBatchRecordBindingBlockers() {
  const blockers = []
  const batchRecordSource = readText(BATCH_RECORD_BINDING_DO)
  const routeDesignerSource = readText(ROUTE_FLOW_DESIGNER)
  const edhrBatchExecutionSource = readText(EDHR_BATCH_EXECUTION_SERVICE)

  if (!batchRecordSource.includes('@TableName("mes_pro_route_flow_process_batch_record")')) {
    blockers.push({
      key: 'MesProRouteFlowProcessBatchRecordDO',
      category: 'SOURCE',
      description: '正式逐工序批记录绑定 DO 未绑定 mes_pro_route_flow_process_batch_record。'
    })
  }
  for (const field of ['routeProcessId', 'batchRecordReportId', 'batchRecordDefinitionId', 'batchRecordVersionId', 'formSlotType']) {
    if (!new RegExp(`private\\s+.*\\s+${field}\\b`).test(batchRecordSource)) {
      blockers.push({
        key: `MesProRouteFlowProcessBatchRecordDO.${field}`,
        category: 'SOURCE',
        description: `正式逐工序批记录绑定缺少 ${field}，不能支撑批记录表单字段。`
      })
    }
  }
  if (/const\s+normalizeRecordBindingSlotType[\s\S]{0,500}return\s+'MAIN'/.test(routeDesignerSource)) {
    blockers.push({
      key: 'normalizeRecordBindingSlotTypeDefaultMain',
      category: 'SOURCE',
      description: '工艺路线前端 normalizeRecordBindingSlotType 对缺失槽位默认 MAIN，存在把 formBindings/旧字段误归为批记录表单的风险。'
    })
  }
  if (/const\s+getRouteNodeBatchRecordForms[\s\S]{0,300}getRouteNodeLegacyBatchRecords/.test(routeDesignerSource)
      && routeDesignerSource.includes('batchRecordFormNames')
      && routeDesignerSource.includes('formBindings')) {
    blockers.push({
      key: 'batchRecordFormNamesFormBindingsSeparation',
      category: 'SOURCE',
      description: '批记录表单字段和 formBindings 同屏存在，M0 尚未用真实 E2E 证明二者不会互相替代。'
    })
  }
  if (/resolveRouteFormSlotType[\s\S]{0,180}blankToDefault[\s\S]{0,80}FORM_SLOT_MAIN/.test(edhrBatchExecutionSource)) {
    blockers.push({
      key: 'edhrRuntimeDefaultMainSlot',
      category: 'SOURCE',
      description: 'eDHR 运行态仍在缺失 formSlotType 时默认 MAIN，正式批记录绑定缺失时应 fail-fast。'
    })
  }
  return blockers
}

function resolveBrowserExecutable(config) {
  const candidates = [
    config.browserPath,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
  ].filter(Boolean)
  return candidates.find((candidate) => fs.existsSync(candidate)) || ''
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    failFast('缺少 Playwright runtime；请先在 IntRuoyiFronted 执行 pnpm install。')
  }
}

async function collectRuntimeBlockers(config) {
  const blockers = []
  if (config.backendUrl) {
    try {
      const response = await fetch(`${config.backendUrl}/actuator/health`, { signal: AbortSignal.timeout(5000) })
      const body = await response.text()
      if (!response.ok || !body.includes('UP')) {
        blockers.push({
          key: 'backendHealth',
          category: 'RUNTIME',
          description: `后端 health 未返回 UP，HTTP=${response.status}。`
        })
      }
    } catch (error) {
      blockers.push({
        key: 'backendHealth',
        category: 'RUNTIME',
        description: `后端 health 不可达：${error.message}`
      })
    }
  }

  if (config.frontendUrl) {
    try {
      const response = await fetch(`${config.frontendUrl}/login?redirect=/index`, { signal: AbortSignal.timeout(5000) })
      const body = await response.text()
      if (!response.ok || !body.includes('<div id="app">')) {
        blockers.push({
          key: 'frontendLogin',
          category: 'RUNTIME',
          description: `前端登录页未正常返回，HTTP=${response.status}。`
        })
      }
    } catch (error) {
      blockers.push({
        key: 'frontendLogin',
        category: 'RUNTIME',
        description: `前端登录页不可达：${error.message}`
      })
    }
  }

  loadPlaywright()
  if (!resolveBrowserExecutable(config)) {
    blockers.push({
      key: 'browser',
      category: 'RUNTIME',
      description: '未找到可用 Chrome/Edge，且 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 未指向有效文件。'
    })
  }

  return blockers
}

function collectMilestoneBlockers() {
  const taskStatePath = path.resolve(TASK_DIR, 'task-state.json')
  if (!fs.existsSync(taskStatePath)) {
    return [{
      key: 'task-state.json',
      category: 'MILESTONE',
      description: '实现任务状态文件不存在，不能执行全链路真实 E2E。'
    }]
  }
  const state = JSON.parse(fs.readFileSync(taskStatePath, 'utf8'))
  const incomplete = (state.milestones || [])
    .filter((milestone) => milestone.id !== 'M6')
    .filter((milestone) => milestone.status !== 'accepted' && milestone.status !== 'completed')
    .map((milestone) => `${milestone.id}:${milestone.status}`)
  if (!incomplete.length) return []
  return [{
    key: 'milestones',
    category: 'MILESTONE',
    description: `M1-M5 尚未全部 ACCEPTED，禁止执行 M6 全链路真实 E2E：${incomplete.join(', ')}。`
  }]
}

function redactConfig(config) {
  const redacted = {
    frontendUrl: config.frontendUrl,
    backendUrl: config.backendUrl,
    tenant: config.tenant,
    dataPrefix: config.dataPrefix,
    roles: {}
  }
  for (const [roleKey, role] of Object.entries(config.roles)) {
    redacted.roles[roleKey] = {
      username: role.username || '',
      password: role.password ? '<redacted>' : ''
    }
  }
  return redacted
}

function writeEvidence(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')

  const blockers = result.blockers || []
  const lines = [
    '# 岗位需求分解矩阵真实 E2E 前置证据',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated At: \`${new Date().toISOString()}\``,
    `- Status: \`${result.status}\``,
    `- Frontend: \`${result.config.frontendUrl || '--'}\``,
    `- Backend: \`${result.config.backendUrl || '--'}\``,
    `- Tenant: \`${result.config.tenant || '--'}\``,
    `- Data Prefix: \`${result.config.dataPrefix || DATA_PREFIX}\``,
    '',
    '## Result',
    ''
  ]
  if (blockers.length) {
    lines.push(`- BLOCKED: ${blockers.length} prerequisite blockers remain.`)
    for (const blocker of blockers) {
      lines.push(`- ${blocker.category}:${blocker.key} -> ${blocker.description}`)
    }
  } else {
    lines.push('- PASS: preflight prerequisites are present.')
  }
  lines.push('')
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function printBlockers(blockers) {
  console.error(`BLOCKED: ${blockers.length} prerequisite blockers remain.`)
  for (const blocker of blockers) {
    console.error(`- ${blocker.category}:${blocker.key} -> ${blocker.description}`)
  }
}

async function runPreflight(config) {
  const blockers = [
    ...collectEnvBlockers(config),
    ...collectSourceBlockers(),
    ...(await collectRuntimeBlockers(config))
  ]
  const result = {
    status: blockers.length ? 'BLOCKED' : 'PASS',
    mode: 'check',
    config: redactConfig(config),
    blockers
  }
  writeEvidence(result)
  if (blockers.length) {
    printBlockers(blockers)
    process.exitCode = 2
    return false
  }
  console.log('PASS role-requirement-matrix real E2E preflight')
  return true
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  failFast(`缺少可填写登录控件：${label}`)
}

async function login(page, config, roleKey, role) {
  const loginUrl = new URL('/login', config.frontendUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 90000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    failFast(`${roleKey} 登录页启用了验证码，无法执行无人值守真实 E2E。`)
  }
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').filter({ visible: true }).first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  }
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'), role.username, `${roleKey} 账号`)
  await fillFirstVisible(form.locator('input[type="password"]'), role.password, `${roleKey} 密码`)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.ok(response.ok(), `${roleKey} 登录 HTTP 失败：${response.status()}`)
  assert.ok(body.code === 0 || body.code === 200, `${roleKey} 登录业务失败：${body.msg || body.code}`)
}

async function runRealFlow(config) {
  const preflightPass = await runPreflight(config)
  if (!preflightPass) return
  const milestoneBlockers = collectMilestoneBlockers()
  if (milestoneBlockers.length) {
    const result = {
      status: 'BLOCKED',
      mode: 'real',
      config: redactConfig(config),
      blockers: milestoneBlockers
    }
    writeEvidence(result)
    printBlockers(milestoneBlockers)
    process.exitCode = 2
    return
  }

  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: resolveBrowserExecutable(config) || undefined
  })
  try {
    for (const [roleKey] of ROLE_CONFIGS) {
      const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
      const page = await context.newPage()
      try {
        await login(page, config, roleKey, config.roles[roleKey])
        await page.goto(new URL(config.roles[roleKey].targetPath, config.frontendUrl).toString(), {
          waitUntil: 'domcontentloaded',
          timeout: 90000
        })
      } finally {
        await context.close()
      }
    }
  } finally {
    await browser.close()
  }

  failFast('M6 全链路真实 E2E 尚未实现；完成 M1-M5 ACCEPTED 后必须扩展本脚本覆盖 62 个 AC。')
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  const config = {
    ...collectConfig(),
    headed: args.headed || process.env.RRM_HEADED === '1'
  }
  if (args.checkOnly) {
    await runPreflight(config)
    return
  }
  await runRealFlow(config)
}

main().catch((error) => {
  const result = {
    status: error.blocked ? 'BLOCKED' : 'FAILED',
    mode: process.argv.includes('--check') ? 'check' : 'real',
    config: redactConfig(collectConfig()),
    blockers: error.blocked
      ? [{ key: 'failFast', category: 'E2E', description: error.message }, ...(error.details || [])]
      : [{ key: 'error', category: 'E2E', description: error.message }]
  }
  writeEvidence(result)
  if (error.blocked) {
    printBlockers(result.blockers)
    process.exitCode = 2
    return
  }
  console.error(error)
  process.exitCode = 1
})
