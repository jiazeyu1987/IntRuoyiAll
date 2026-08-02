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
const TEST_PLAN_PATH = path.resolve(
  WORKSPACE_ROOT,
  'doc/tasks/20260801-role-requirement-matrix-excel/test-plan.md'
)

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
const ACTIVE_ORDER_TRANSFER_TRACE_SQL = path.resolve(
  BACKEND_ROOT,
  'sql/mysql/20260802_mes_process_pool_active_order_transfer_trace.sql'
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
  ['RRM_UNAUTHORIZED_USERNAME', '错误角色账号标签，用于证明活跃订单写入权限隔离。'],
  ['RRM_UNAUTHORIZED_PASSWORD', '错误角色密码，通过环境变量注入，不写入证据。'],
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

const M6_REAL_FLOW_PHASES = [
  {
    key: 'productionLeaderWorkbench',
    roleKey: 'productionLeader',
    label: '生产组长工作台与日结/分配表面',
    targetPath: '/mes/pro/process-pool/team-leader',
    selectors: [
      '[data-team-leader-report-workbench]',
      '[data-team-leader-config-center]',
      '[data-team-leader-active-order-config]',
      '[data-role-matrix-daily-close]'
    ],
    actionKey: 'joinActiveOrder',
    acceptanceIds: ['AC-M04', 'AC-M16', 'AC-M17', 'AC-M18', 'AC-D09', 'AC-D12', 'AC-D14', 'AC-D38']
  },
  {
    key: 'pqcLeaderWorkbench',
    roleKey: 'pqcLeader',
    label: 'PQC 组长复核表面',
    targetPath: '/mes/pro/process-pool/team-leader',
    tabText: 'PQC 组长',
    selectors: [
      '[data-team-leader-type-tabs]',
      '[data-team-leader-report-workbench]',
      '[data-role-matrix-daily-close]'
    ],
    acceptanceIds: ['AC-M20', 'AC-D30', 'AC-D32', 'AC-D33', 'AC-D34', 'AC-D35', 'AC-D37']
  },
  {
    key: 'productionEmployeeEntry',
    roleKey: 'productionEmployee',
    label: '生产员工入口登录与前端应用加载',
    targetPath: '/index',
    selectors: ['#app'],
    acceptanceIds: ['AC-M10', 'AC-M11']
  },
  {
    key: 'pqcInspectorEntry',
    roleKey: 'pqcInspector',
    label: 'PQC 检验员入口登录与前端应用加载',
    targetPath: '/index',
    selectors: ['#app'],
    actionKey: 'verifyPqcActiveOrderReadOnly',
    acceptanceIds: ['AC-M12', 'AC-M13', 'AC-M14', 'AC-M15', 'AC-D24', 'AC-D25', 'AC-D26', 'AC-D27', 'AC-D28', 'AC-D29', 'AC-D31']
  },
  {
    key: 'qaRegulationEntry',
    roleKey: 'qa',
    label: 'QA 规程维护入口',
    targetPath: '/mes/qc/template',
    selectors: ['#app'],
    acceptanceIds: ['AC-M09', 'AC-D15', 'AC-D16', 'AC-D17', 'AC-D18', 'AC-D19', 'AC-D20', 'AC-D21', 'AC-D22', 'AC-D23']
  },
  {
    key: 'releaseOwnerEntry',
    roleKey: 'releaseOwner',
    label: '放行负责人入口',
    targetPath: '/mes/pro/feedback/edhr-release',
    selectors: ['#app'],
    actionKey: 'verifyActiveOrderUnauthorizedMutationBlocked',
    acceptanceIds: ['AC-M22', 'AC-M23']
  }
]

const FORBIDDEN_TENANT_FRAGMENTS = ['prod', 'production', '正式', '生产', '芋道源码', 'admin']
const LOCAL_BASELINE_TENANT_AUTHORIZATION = 'USER_APPROVED_YUDAO_SOURCE_20260802'
const SENSITIVE_KEY_PATTERN = /PASSWORD|TOKEN|SECRET|SIGNATURE_IDS_JSON/i
const ACTIVE_ORDER_MAINTAIN_PERMISSION = 'mes:pro-process-pool-team-leader:maintain'

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
    workOrderId: Number(envValue('RRM_PRODUCTION_ORDER_ID')),
    routeId: Number(envValue('RRM_ROUTE_ID')),
    routeVersionId: Number(envValue('RRM_ROUTE_VERSION_ID')),
    qaRegulationVersionId: Number(envValue('RRM_QA_REGULATION_VERSION_ID')),
    unauthorizedActor: {
      username: envValue('RRM_UNAUTHORIZED_USERNAME'),
      password: envValue('RRM_UNAUTHORIZED_PASSWORD'),
      targetPath: '/index'
    },
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

  if (config.unauthorizedActor.username) {
    const matchedRole = Object.entries(config.roles)
      .find(([, role]) => role.username === config.unauthorizedActor.username)
    if (matchedRole) {
      blockers.push({
        key: 'RRM_UNAUTHORIZED_USERNAME',
        category: 'ENV',
        description: `错误角色夹具不能复用业务角色 ${matchedRole[0]}；必须使用不含活跃订单维护权限的独立账号。`
      })
    }
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
  const activeOrderTransferTraceSql = readText(ACTIVE_ORDER_TRANSFER_TRACE_SQL)

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
    if (!pattern.test(activeOrderSql)
        && !pattern.test(mesBaseSchema)
        && !pattern.test(kingdeeMaterialSql)
        && !pattern.test(activeOrderTransferTraceSql)) {
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
  const batchRecordValueBlock = routeDesignerSource.match(
    /const buildBatchRecordFormValue = \(\) => \{([\s\S]*?)\n\}/
  )
  const batchRecordLinksBlock = routeDesignerSource.match(
    /const buildBatchRecordFormLinks = \(\): ProcessDetailLinkItem\[\] =>([\s\S]*?)\n\n/
  )
  const batchRecordNodeStatusBlock = routeDesignerSource.match(
    /const isRouteNodeBatchRecordFormConfigured = \(node: RouteFlowNodeVO\) =>([\s\S]*?)\n\n/
  )
  const batchRecordSeparated = Boolean(
    batchRecordValueBlock
      && batchRecordLinksBlock
      && batchRecordNodeStatusBlock
      && /getSelectedBatchRecordForms\(\)/.test(batchRecordValueBlock[1])
      && /getSelectedBatchRecordForms\(\)/.test(batchRecordLinksBlock[1])
      && /getRouteNodeBatchRecordForms\(node\)/.test(batchRecordNodeStatusBlock[1])
      && !/selectedRecordBindings|getRecordBindingsBySlotType|formBindings/.test(batchRecordValueBlock[1])
      && !/selectedRecordBindings|getRecordBindingsBySlotType|formBindings/.test(batchRecordLinksBlock[1])
      && !/getRouteNodeBatchRecordBindings\(node\)|formBindings/.test(batchRecordNodeStatusBlock[1])
  )
  if (!batchRecordSeparated) {
    blockers.push({
      key: 'batchRecordFormNamesFormBindingsSeparation',
      category: 'SOURCE',
      description: '批记录表单字段和 formBindings 同屏存在，尚未证明二者使用独立 value/link/border 来源。'
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

function canProbeActiveOrderRuntime(config) {
  return Boolean(
    config.backendUrl
    && config.tenant
    && config.roles.productionLeader.username
    && config.roles.productionLeader.password
  )
}

async function readJsonResponse(response, label) {
  const text = await response.text()
  try {
    return text ? JSON.parse(text) : {}
  } catch (error) {
    throw new Error(`${label} 返回非 JSON 响应：${error.message}`)
  }
}

function isBusinessSuccess(body) {
  return body && (body.code === 0 || body.code === 200)
}

function responseMessage(body) {
  if (!body || typeof body !== 'object') return 'empty response'
  return body.msg || body.message || body.error || `code=${body.code}`
}

async function fetchBusinessJson(url, options, label) {
  const response = await fetch(url, {
    ...options,
    signal: AbortSignal.timeout(options.timeoutMs || 15000)
  })
  const body = await readJsonResponse(response, label)
  if (!response.ok || !isBusinessSuccess(body)) {
    throw new Error(`${label} 失败，HTTP=${response.status}，业务=${responseMessage(body)}`)
  }
  return body
}

async function probeActiveOrderListRuntime(config) {
  if (!canProbeActiveOrderRuntime(config)) return []
  try {
    const tenantUrl = new URL('/admin-api/system/tenant/get-id-by-name', config.backendUrl)
    tenantUrl.searchParams.set('name', config.tenant)
    const tenantBody = await fetchBusinessJson(tenantUrl, {}, '租户解析')
    const tenantId = Number(tenantBody.data)
    if (!Number.isFinite(tenantId) || tenantId <= 0) {
      throw new Error('租户解析失败，未返回有效 tenant-id。')
    }

    const loginBody = await fetchBusinessJson(new URL('/admin-api/system/auth/login', config.backendUrl), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'tenant-id': String(tenantId)
      },
      body: JSON.stringify({
        username: config.roles.productionLeader.username,
        password: config.roles.productionLeader.password
      })
    }, '生产组长 API 登录')
    const accessToken = loginBody.data?.accessToken || loginBody.data?.access_token || loginBody.data?.token
    if (!accessToken) {
      throw new Error('生产组长 API 登录成功但未返回 accessToken。')
    }

    const listBody = await fetchBusinessJson(
      new URL('/admin-api/mes/pro/process-pool/team-leader/active-order/list', config.backendUrl),
      {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'tenant-id': String(tenantId)
        }
      },
      'activeOrderListRuntime'
    )
    const rows = Array.isArray(listBody.data) ? listBody.data : []
    const fixtureRows = rows.filter((row) => Number(row.workOrderId) === Number(config.workOrderId))
    for (const row of fixtureRows) {
      for (const field of ['routeId', 'routeVersionId', 'erpFixedQuantitySnapshot', 'businessStatus']) {
        if (row[field] === null || row[field] === undefined || row[field] === '') {
          throw new Error(`activeOrderListRuntime 返回测试工单 ${config.workOrderId}，但缺少 ${field}。`)
        }
      }
    }
    return []
  } catch (error) {
    return [{
      key: 'activeOrderListRuntime',
      category: 'RUNTIME',
      description: `登录态活跃订单列表接口运行态前置失败：${error.message}；需先应用 M1/M2/M4 正式迁移并补齐任务夹具的排产/活跃订单快照。`
    }]
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

  blockers.push(...(await probeActiveOrderListRuntime(config)))

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

function loadAcceptanceMatrix() {
  const testPlan = readText(TEST_PLAN_PATH)
  const matrixMatch = testPlan.match(/<!-- ACCEPTANCE_TEST_MATRIX_START -->([\s\S]*?)<!-- ACCEPTANCE_TEST_MATRIX_END -->/)
  if (!matrixMatch) {
    failFast('测试计划缺少 ACCEPTANCE_TEST_MATRIX，不能构建 M6 AC 覆盖账本。')
  }
  const rows = matrixMatch[1]
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => /^\| AC-[MD]\d+ \|/.test(line))
    .map((line) => line
      .slice(1, -1)
      .split('|')
      .map((cell) => cell.trim().replace(/^`|`$/g, '')))
    .map(([ac, bdd, testCase, layers, positiveAssertion, failureAssertion]) => ({
      ac,
      bdd,
      testCase,
      layers,
      positiveAssertion,
      failureAssertion
    }))
  const uniqueAcIds = new Set(rows.map((row) => row.ac))
  if (rows.length !== 62 || uniqueAcIds.size !== 62) {
    failFast(`测试计划 AC 矩阵必须为 62 项且唯一，当前 rows=${rows.length}, unique=${uniqueAcIds.size}。`)
  }
  return rows
}

function buildAcceptanceCoverage(acceptanceMatrix, phaseEvidence, actionEvidence = []) {
  const phaseByAc = new Map()
  for (const phase of phaseEvidence.filter((item) => item.status === 'PASS')) {
    for (const ac of phase.acceptanceIds || []) {
      if (!phaseByAc.has(ac)) {
        phaseByAc.set(ac, [])
      }
      phaseByAc.get(ac).push(phase.key)
    }
  }
  const actionByAc = new Map()
  for (const action of actionEvidence.filter((item) => item.status === 'PASS')) {
    for (const ac of action.acceptanceIds || []) {
      if (!actionByAc.has(ac)) {
        actionByAc.set(ac, [])
      }
      actionByAc.get(ac).push(action.key)
    }
  }
  const rows = acceptanceMatrix.map((item) => {
    const phaseKeys = phaseByAc.get(item.ac) || []
    const actionKeys = actionByAc.get(item.ac) || []
    return {
      ...item,
      status: actionKeys.length
        ? 'ACTION_OBSERVED_NEEDS_FAILURE_E2E'
        : (phaseKeys.length ? 'SURFACE_OBSERVED_NEEDS_ACTION_E2E' : 'UNCOVERED_BY_REAL_E2E'),
      phaseKeys,
      actionKeys
    }
  })
  return {
    total: rows.length,
    accepted: rows.filter((row) => row.status === 'ACCEPTED').length,
    actionObserved: rows.filter((row) => row.status === 'ACTION_OBSERVED_NEEDS_FAILURE_E2E').length,
    surfaceObserved: rows.filter((row) => row.status === 'SURFACE_OBSERVED_NEEDS_ACTION_E2E').length,
    uncovered: rows.filter((row) => row.status === 'UNCOVERED_BY_REAL_E2E').length,
    pending: rows.filter((row) => row.status !== 'ACCEPTED').length,
    rows
  }
}

function assertAcceptanceCoverage(acceptanceCoverage) {
  const blockers = []
  if (acceptanceCoverage.total !== 62) {
    blockers.push({
      key: 'acceptanceMatrix',
      category: 'E2E_COVERAGE',
      description: `M6 必须加载 62 个 AC，当前为 ${acceptanceCoverage.total}。`
    })
  }
  for (const row of acceptanceCoverage.rows) {
    if (row.status !== 'ACCEPTED') {
      const evidenceParts = []
      if (row.phaseKeys.length) evidenceParts.push(`已观察页面阶段：${row.phaseKeys.join(', ')}`)
      if (row.actionKeys?.length) evidenceParts.push(`已执行真实动作：${row.actionKeys.join(', ')}`)
      const phaseText = evidenceParts.length ? evidenceParts.join('；') : '尚无真实页面阶段覆盖'
      blockers.push({
        key: row.ac,
        category: 'E2E_COVERAGE',
        description: `${row.testCase} 仍未达到真实动作/失败路径/只读核验 ACCEPTED；${phaseText}。`
      })
    }
  }
  return blockers
}

function buildActionBlockers(actionEvidence) {
  return actionEvidence
    .filter((action) => action.status === 'BLOCKED')
    .map((action) => ({
      key: action.key,
      category: action.category || 'E2E_PERMISSION',
      description: action.description || `${action.label} 尚未满足 M6 权限隔离验收。`
    }))
}

function acceptanceIdsByLayer(acceptanceMatrix, layer) {
  return acceptanceMatrix
    .filter((row) => row.layers.split('、').map((item) => item.trim()).includes(layer))
    .map((row) => row.ac)
}

function buildM6ConcurrencyPerformanceGateEvidence(acceptanceMatrix, actionEvidence) {
  const concurrencyAcceptanceIds = acceptanceIdsByLayer(acceptanceMatrix, 'CONC')
  const performanceAcceptanceIds = acceptanceIdsByLayer(acceptanceMatrix, 'PERF')
  const activeOrderActionKeys = actionEvidence
    .filter((action) => action.status === 'PASS'
      && ['joinActiveOrder', 'activeOrderConflictRouteRejected', 'activeOrderCrossRoleReadOnly'].includes(action.key))
    .map((action) => action.key)
  const performanceActions = actionEvidence
    .filter((action) => action.status === 'PASS'
      && (action.acceptanceIds || []).some((ac) => performanceAcceptanceIds.includes(ac)))
  const performanceActionKeys = performanceActions.map((action) => action.key)
  const observedPerformanceAcceptanceIds = [
    ...new Set(performanceActions.flatMap((action) => action.acceptanceIds || []))
  ].filter((ac) => performanceAcceptanceIds.includes(ac))

  return [
    {
      key: 'm6ConcurrencyGateDeferred',
      label: 'M6 并发门禁结构化',
      roleKey: 'system',
      status: 'BLOCKED',
      category: 'E2E_CONCURRENCY',
      acceptanceIds: concurrencyAcceptanceIds,
      observedActionKeys: activeOrderActionKeys,
      description: `测试矩阵中 ${concurrencyAcceptanceIds.length} 个 CONC AC 仍需逐项完成真实并发或服务级并发证据；当前仅 AC-M04 已有活跃订单重复/冲突/跨角色动作和后端唯一键并发回归，不能替代报工分配、PQC 提交/确认、过程检验、放行和批记录回填并发门禁。`
    },
    {
      key: 'm6PerformanceGateDeferred',
      label: 'M6 性能门禁结构化',
      roleKey: 'system',
      status: 'BLOCKED',
      category: 'E2E_PERFORMANCE',
      acceptanceIds: performanceAcceptanceIds,
      observedActionKeys: performanceActionKeys,
      observedAcceptanceIds: observedPerformanceAcceptanceIds,
      description: `测试矩阵中 ${performanceAcceptanceIds.length} 个 PERF AC 仍需分页总数、索引或查询计数证据；当前已观察 ${observedPerformanceAcceptanceIds.length} 个 PERF AC（${observedPerformanceAcceptanceIds.join(', ') || '无'}），尚未完成日结、PQC 列表和逐件明细的完整 N+1 或分页漂移证明。`
    }
  ]
}

function buildGateBlockers(gateEvidence) {
  return gateEvidence
    .filter((gate) => gate.status === 'BLOCKED')
    .map((gate) => ({
      key: gate.key,
      category: gate.category,
      description: gate.description
    }))
}

function redactConfig(config) {
  const redacted = {
    frontendUrl: config.frontendUrl,
    backendUrl: config.backendUrl,
    tenant: config.tenant,
    dataPrefix: config.dataPrefix,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId,
    qaRegulationVersionId: config.qaRegulationVersionId,
    unauthorizedActor: {
      username: config.unauthorizedActor?.username || '',
      password: config.unauthorizedActor?.password ? '<redacted>' : ''
    },
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
    lines.push(`- BLOCKED: ${blockers.length} blockers remain.`)
    for (const blocker of blockers) {
      lines.push(`- ${blocker.category}:${blocker.key} -> ${blocker.description}`)
    }
  } else {
    lines.push('- PASS: preflight prerequisites and full AC coverage are present.')
  }
  if (Array.isArray(result.phaseEvidence) && result.phaseEvidence.length) {
    lines.push('', '## Phase Evidence', '')
    for (const phase of result.phaseEvidence) {
      lines.push(`- ${phase.status}: ${phase.key} -> ${phase.label}; role=${phase.roleKey}; selectors=${phase.selectors.join(', ')}`)
    }
  }
  if (Array.isArray(result.actionEvidence) && result.actionEvidence.length) {
    lines.push('', '## Action Evidence', '')
    for (const action of result.actionEvidence) {
      lines.push(`- ${action.status}: ${action.key} -> ${action.label}; role=${action.roleKey}; acceptance=${action.acceptanceIds.join(', ')}`)
    }
  }
  if (Array.isArray(result.gateEvidence) && result.gateEvidence.length) {
    lines.push('', '## Gate Evidence', '')
    for (const gate of result.gateEvidence) {
      lines.push(`- ${gate.status}: ${gate.key} -> ${gate.label}; category=${gate.category}; acceptance=${gate.acceptanceIds.join(', ') || '--'}`)
    }
  }
  if (result.acceptanceCoverage) {
    lines.push('', '## Acceptance Coverage', '')
    lines.push(`- Total: ${result.acceptanceCoverage.total}`)
    lines.push(`- Accepted: ${result.acceptanceCoverage.accepted}`)
    lines.push(`- Action Observed: ${result.acceptanceCoverage.actionObserved}`)
    lines.push(`- Surface Observed: ${result.acceptanceCoverage.surfaceObserved}`)
    lines.push(`- Uncovered: ${result.acceptanceCoverage.uncovered}`)
    lines.push(`- Pending: ${result.acceptanceCoverage.pending}`)
    for (const row of result.acceptanceCoverage.rows) {
      lines.push(`- ${row.ac}/${row.testCase}: ${row.status}; phases=${row.phaseKeys.join(', ') || '--'}; actions=${row.actionKeys?.join(', ') || '--'}`)
    }
  }
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function printBlockers(blockers) {
  console.error(`BLOCKED: ${blockers.length} blockers remain.`)
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

async function fillFormItem(section, label, value) {
  const item = section.locator('.el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirstVisible(item.locator('input'), String(value), label)
}

function formForAction(section, actionText) {
  return section.getByRole('button', { name: actionText }).first()
    .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form ")][1]')
}

async function fillFormItemForAction(section, actionText, label, value) {
  await fillFormItem(formForAction(section, actionText), label, value)
}

async function clickButtonAndWaitForSuccess(section, text, endpointFragment) {
  const page = section.page()
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes(endpointFragment)
      && ['POST', 'PUT', 'DELETE'].includes(response.request().method())
  , { timeout: 30000 })
  await section.getByRole('button', { name: text }).click()
  const response = await responsePromise
  assert.ok(response.ok(), `${text} 写入接口 HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `${text} 写入接口业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function clickButtonAndWaitForBusinessFailure(section, text, endpointFragment) {
  const page = section.page()
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes(endpointFragment)
      && ['POST', 'PUT', 'DELETE'].includes(response.request().method())
  , { timeout: 30000 })
  await section.getByRole('button', { name: text }).click()
  const response = await responsePromise
  assert.ok(response.ok(), `${text} 失败路径接口 HTTP 异常：${response.status()}`)
  const body = await response.json()
  assert.notEqual(body.code, 0, `${text} 失败路径不能返回业务成功。`)
  assert.notEqual(body.code, 200, `${text} 失败路径不能返回兼容成功码。`)
  return body
}

async function parseJsonResponse(response, label) {
  assert.ok(response.ok(), `${label} HTTP 失败：${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `${label} 业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function fetchWithPageAuth(page, endpoint, options = {}) {
  return page.evaluate(async ({ endpoint: requestEndpoint, options: requestOptions }) => {
    const readCacheValue = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      const item = JSON.parse(raw)
      return JSON.parse(item.v)
    }
    const token = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const response = await window.fetch(requestEndpoint, {
      method: requestOptions.method || 'GET',
      headers: {
        ...(requestOptions.headers || {}),
        Authorization: `Bearer ${token}`,
        'tenant-id': String(tenantId)
      },
      body: requestOptions.body
    })
    let body = null
    try {
      body = await response.json()
    } catch (error) {
      body = { code: 'NON_JSON', message: error.message }
    }
    return {
      ok: response.ok,
      status: response.status,
      body
    }
  }, { endpoint, options })
}

async function getCurrentPermissionInfo(page) {
  const result = await fetchWithPageAuth(page, '/admin-api/system/auth/get-permission-info')
  assert.ok(result.ok, `当前角色权限信息 HTTP 失败：${result.status}`)
  assert.equal(result.body.code, 0, `当前角色权限信息业务失败：${responseMessage(result.body)}`)
  return result.body.data || {}
}

function extractPermissions(permissionInfo) {
  if (Array.isArray(permissionInfo.permissions)) return permissionInfo.permissions
  if (Array.isArray(permissionInfo.permissionList)) return permissionInfo.permissionList
  return []
}

function hasActiveOrderMaintainPermission(permissions) {
  return permissions.includes('*:*:*') || permissions.includes(ACTIVE_ORDER_MAINTAIN_PERMISSION)
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
  ).catch((error) => ({ loginResponseTimeout: error }))
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  if (response.loginResponseTimeout) {
    failFast(`${roleKey} 登录未捕获登录接口响应：${response.loginResponseTimeout.message}`, [{
      key: 'loginResponseTimeout',
      category: 'E2E_RUNTIME',
      description: `${roleKey} 登录页已提交但 90 秒内未捕获 /system/auth/login 响应；需检查前端会话、后端登录链路或本机运行态请求阻塞。`
    }])
  }
  const body = await response.json()
  assert.ok(response.ok(), `${roleKey} 登录 HTTP 失败：${response.status()}`)
  assert.ok(body.code === 0 || body.code === 200, `${roleKey} 登录业务失败：${body.msg || body.code}`)
}

function findTargetActiveOrder(rows, config) {
  return rows.find((row) =>
    Number(row.workOrderId) === Number(config.workOrderId)
      && Number(row.routeId) === Number(config.routeId)
  )
}

async function reloadActiveOrderRows(page) {
  const result = await page.evaluate(async () => {
    const readCacheValue = (key) => {
      const raw = window.localStorage.getItem(key)
      if (!raw) return undefined
      const item = JSON.parse(raw)
      return JSON.parse(item.v)
    }
    const token = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    const response = await window.fetch('/admin-api/mes/pro/process-pool/team-leader/active-order/list', {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        'tenant-id': String(tenantId)
      }
    })
    return {
      ok: response.ok,
      status: response.status,
      body: await response.json()
    }
  })
  assert.ok(result.ok, `活跃订单列表失败路径复核 HTTP 失败：${result.status}`)
  assert.equal(result.body.code, 0, `活跃订单列表失败路径复核业务失败：${result.body.msg || result.body.message || 'unknown'}`)
  return result.body.data
}

async function performActiveOrderJoin(page, config) {
  const section = page.locator('[data-team-leader-active-order-config]').first()
  await fillFormItemForAction(section, '加入活跃订单', '生产订单ID', config.workOrderId)
  await fillFormItemForAction(section, '加入活跃订单', '路线ID', config.routeId)
  await fillFormItemForAction(section, '加入活跃订单', '路线版本ID', config.routeVersionId)
  const listResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/process-pool/team-leader/active-order/list')
      && response.request().method() === 'GET'
  , { timeout: 30000 }).catch((error) => ({ activeOrderListResponseError: error }))
  const activeOrderId = await clickButtonAndWaitForSuccess(
    section,
    '加入活跃订单',
    '/mes/pro/process-pool/team-leader/active-order/add'
  )
  const listResponse = await listResponsePromise
  if (listResponse.activeOrderListResponseError) {
    failFast(`加入活跃订单后未捕获到活跃订单列表刷新响应：${listResponse.activeOrderListResponseError.message}`, [{
      key: 'activeOrderListResponseError',
      category: 'E2E',
      description: '真实页面加入活跃订单后必须刷新并返回同一 activeOrderId，不能让页面关闭或响应缺失变成未结构化异常。'
    }])
  }
  assert.ok(listResponse.ok(), `活跃订单列表刷新 HTTP 失败：${listResponse.status()}`)
  const listBody = await listResponse.json()
  assert.equal(listBody.code, 0, `活跃订单列表刷新业务失败：${listBody.msg || listBody.message || 'unknown'}`)
  const rows = Array.isArray(listBody.data) ? listBody.data : []
  assert.ok(
    rows.some((row) => Number(row.id) === Number(activeOrderId) && Number(row.workOrderId) === Number(config.workOrderId)),
    '加入活跃订单后，刷新列表必须返回同一 activeOrderId 和 workOrderId。'
  )
  return {
    key: 'joinActiveOrder',
    label: '生产组长通过真实页面加入带路线版本的活跃订单',
    roleKey: 'productionLeader',
    status: 'PASS',
    acceptanceIds: ['AC-M04'],
    activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId
  }
}

async function verifyActiveOrderConflictRouteFailure(page, config, joinEvidence) {
  const conflictRouteId = config.routeId + 1
  const section = page.locator('[data-team-leader-active-order-config]').first()
  await fillFormItemForAction(section, '加入活跃订单', '生产订单ID', config.workOrderId)
  await fillFormItemForAction(section, '加入活跃订单', '路线ID', conflictRouteId)
  await fillFormItemForAction(section, '加入活跃订单', '路线版本ID', config.routeVersionId)
  const body = await clickButtonAndWaitForBusinessFailure(
    section,
    '加入活跃订单',
    '/mes/pro/process-pool/team-leader/active-order/add'
  )
  const messageText = body.msg || body.message || ''
  assert.match(messageText, /活跃订单|工序|路线|目标数量|快照/, '冲突路线必须返回可诊断的业务失败原因。')
  await page.locator('.el-message, .el-notification').filter({
    hasText: /活跃订单|工序|路线|目标数量|快照/
  }).first().waitFor({ state: 'visible', timeout: 10000 })
  const rows = await reloadActiveOrderRows(page)
  assert.ok(Array.isArray(rows), '冲突路线失败后活跃订单列表必须仍可读取。')
  assert.ok(
    rows.some((row) => Number(row.id) === Number(joinEvidence.activeOrderId)
      && Number(row.workOrderId) === Number(config.workOrderId)
      && Number(row.routeId) === Number(config.routeId)),
    '冲突路线失败后必须保留原合法 activeOrderId。'
  )
  assert.ok(
    !rows.some((row) => Number(row.workOrderId) === Number(config.workOrderId)
      && Number(row.routeId) === Number(conflictRouteId)
      && Number(row.routeVersionId) === Number(config.routeVersionId)),
    '冲突路线失败路径不得新增错误路线的 activeOrder。'
  )
  return {
    key: 'activeOrderConflictRouteRejected',
    label: '生产组长通过真实页面提交冲突路线并被 fail-fast 拒绝',
    roleKey: 'productionLeader',
    status: 'PASS',
    acceptanceIds: ['AC-M04'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    expectedRouteId: config.routeId,
    rejectedRouteId: conflictRouteId,
    routeVersionId: config.routeVersionId,
    responseCode: body.code,
    responseMessage: messageText
  }
}

async function verifyPqcActiveOrderReadOnly(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId) {
    failFast('PQC 跨角色只读核验前缺少生产组长 joinActiveOrder 动作证据。', [{
      key: 'activeOrderCrossRolePrereq',
      category: 'E2E',
      description: '必须先由生产组长通过真实页面加入活跃订单，再用 PQC 页面只读核验同一 activeOrderId。'
    }])
  }

  const targetUrl = new URL('/mes/pro/feedback/edhr-batch-pqc-fill', config.frontendUrl)
  targetUrl.searchParams.set('workOrderId', String(config.workOrderId))
  targetUrl.searchParams.set('routeId', String(config.routeId))
  await page.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('[data-frontline-pqc-operator]').first().waitFor({ state: 'visible', timeout: 60000 })
  const activeOrders = await loadPqcActiveOrdersViaAuth(page, 'PQC 活跃订单只读列表')
  assert.ok(Array.isArray(activeOrders), 'PQC 活跃订单只读列表必须返回数组。')
  const activeOrder = findTargetActiveOrder(activeOrders, config)
  assert.ok(activeOrder, 'PQC 活跃订单只读列表必须包含生产组长加入的同一工单和路线。')

  const processes = await loadPqcProcessesViaAuth(page, config, 'PQC 活跃订单工序只读列表')
  assert.ok(Array.isArray(processes), 'PQC 活跃订单工序只读列表必须返回数组。')
  assert.ok(
    processes.some((process) => Number(process.activeOrderId) === Number(joinEvidence.activeOrderId)),
    'PQC 活跃订单工序只读列表必须返回与生产组长加入结果一致的 activeOrderId。'
  )

  return {
    key: 'activeOrderCrossRoleReadOnly',
    label: 'PQC 检验员通过真实页面只读同一 activeOrderId',
    roleKey: 'pqcInspector',
    status: 'PASS',
    acceptanceIds: ['AC-M04', 'AC-D13', 'AC-D24'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    sourceActionKey: joinEvidence.key
  }
}

async function loadPqcActiveOrdersViaAuth(page, label) {
  const result = await fetchWithPageAuth(page, '/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-orders')
  assert.ok(result.ok, `${label} HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `${label} 业务失败：${responseMessage(result.body)}`)
  assert.ok(Array.isArray(result.body.data), `${label} 必须返回数组。`)
  return result.body.data
}

async function loadPqcProcessesViaAuth(page, config, label) {
  const endpoint = `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes?workOrderId=${encodeURIComponent(config.workOrderId)}&routeId=${encodeURIComponent(config.routeId)}`
  const result = await fetchWithPageAuth(page, endpoint)
  assert.ok(result.ok, `${label} HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `${label} 业务失败：${responseMessage(result.body)}`)
  assert.ok(Array.isArray(result.body.data), `${label} 必须返回数组。`)
  return result.body.data
}

async function verifyPqcRegulationItemsRendered(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const readOnlyEvidence = actionEvidence.find((item) => item.key === 'activeOrderCrossRoleReadOnly' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId || !readOnlyEvidence) {
    return {
      key: 'pqcRegulationItemsRendered',
      label: 'PQC 页面按已发布 QA 规程渲染检验项目',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_QA_REGULATION',
      acceptanceIds: ['AC-D17', 'AC-D19', 'AC-D24', 'AC-D31'],
      description: 'PQC 规程项目渲染核验前缺少 activeOrder 或跨角色只读动作证据，不能证明页面读取正式 QA 规程快照。'
    }
  }

  const processes = await loadPqcProcessesViaAuth(page, config, 'PQC 已发布规程项目只读列表')
  const matchingProcesses = processes.filter((process) =>
    Number(process.activeOrderId) === Number(joinEvidence.activeOrderId)
      && Number(process.regulationVersionId) > 0
      && Number(process.pqcTaskId) > 0
      && Array.isArray(process.inspectionItems)
      && process.inspectionItems.length > 0
  )
  if (!matchingProcesses.length) {
    return {
      key: 'pqcRegulationItemsRendered',
      label: 'PQC 页面按已发布 QA 规程渲染检验项目',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_QA_REGULATION',
      acceptanceIds: ['AC-D17', 'AC-D19', 'AC-D24', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      processCount: processes.length,
      description: 'PQC 工序列表没有返回带 regulationVersionId、pqcTaskId 和 inspectionItems 的正式规程快照；不能证明页面按 QA 规程动态渲染。'
    }
  }

  const invalidItem = matchingProcesses
    .flatMap((process) => process.inspectionItems.map((item) => ({ process, item })))
    .find(({ item }) => !item.itemCode || !item.itemName || !item.inspectionMethod || !item.standardText || !item.resultType)
  assert.equal(invalidItem, undefined, 'PQC 检验项目必须包含项目编码、名称、方法、标准和结果类型。')

  const itemCount = matchingProcesses.reduce((sum, process) => sum + process.inspectionItems.length, 0)
  const inspectionTypes = [...new Set(matchingProcesses.map((process) => process.inspectionType).filter(Boolean))]
  const regulationVersionIds = [...new Set(matchingProcesses.map((process) => process.regulationVersionId))]
  const configuredVersionId = Number(config.qaRegulationVersionId)
  const configuredVersionObserved = Number.isFinite(configuredVersionId) && configuredVersionId > 0
    ? regulationVersionIds.some((versionId) => Number(versionId) === configuredVersionId)
    : false
  const plannedQuantities = matchingProcesses
    .map((process) => Number(process.plannedInspectionQuantity))
    .filter((value) => Number.isFinite(value) && value > 0)
  assert.ok(plannedQuantities.length > 0, 'PQC 任务必须带出大于 0 的计划检验数量。')

  return {
    key: 'pqcRegulationItemsRendered',
    label: 'PQC 页面按已发布 QA 规程渲染检验项目',
    roleKey: 'pqcInspector',
    status: 'PASS',
    acceptanceIds: ['AC-D17', 'AC-D19', 'AC-D24', 'AC-D31'],
    activeOrderId: joinEvidence.activeOrderId,
    processCount: matchingProcesses.length,
    itemCount,
    inspectionTypes,
    regulationVersionIds,
    configuredVersionId,
    configuredVersionObserved,
    plannedQuantities
  }
}

async function verifyPqcPieceDetailQuantityPrepared(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const regulationEvidence = actionEvidence.find((item) => item.key === 'pqcRegulationItemsRendered' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId || !regulationEvidence) {
    return {
      key: 'pqcPieceDetailQuantityPrepared',
      label: 'PQC 逐件明细数量按计划数量准备',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PIECE_DETAIL',
      acceptanceIds: ['AC-D27'],
      description: '逐件明细数量核验前缺少 activeOrder 或正式 QA 规程项目动作证据，不能证明逐件行来源于正式 PQC task plannedInspectionQuantity。'
    }
  }

  const plannedQuantities = [...new Set((regulationEvidence.plannedQuantities || [])
    .map((quantity) => Number(quantity))
    .filter((quantity) => Number.isFinite(quantity) && quantity > 0))]
  if (!plannedQuantities.length) {
    return {
      key: 'pqcPieceDetailQuantityPrepared',
      label: 'PQC 逐件明细数量按计划数量准备',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PIECE_DETAIL',
      acceptanceIds: ['AC-D27'],
      activeOrderId: joinEvidence.activeOrderId,
      description: 'PQC 规程动作证据没有计划检验数量，不能证明逐件明细数量来源。'
    }
  }

  await page.locator('[data-frontline-pqc-inspection-content]').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  const quantityInput = page.locator('#frontlinePqcInspectionQuantity').first()
  await quantityInput.waitFor({ state: 'visible', timeout: 30000 })
  const uiQuantity = Number(await quantityInput.inputValue())
  assert.ok(
    plannedQuantities.includes(uiQuantity),
    `PQC 页面检验数量 ${uiQuantity} 必须来自正式 task plannedInspectionQuantity：${plannedQuantities.join(', ')}。`
  )

  const firstEntry = page.locator('.frontline-pqc-content-item, .frontline-pqc-choice-item .manual').first()
  if (await firstEntry.count() === 0) {
    return {
      key: 'pqcPieceDetailQuantityPrepared',
      label: 'PQC 逐件明细数量按计划数量准备',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PIECE_DETAIL',
      acceptanceIds: ['AC-D27'],
      activeOrderId: joinEvidence.activeOrderId,
      plannedQuantities,
      uiQuantity,
      description: 'PQC 页面没有可打开的逐件检验项目，不能证明计划数量对应完整逐件明细。'
    }
  }
  const entryLabel = (await firstEntry.innerText()).replace(/\s+/g, ' ').trim()
  await firstEntry.click()
  const modal = page.locator('[data-pqc-piece-modal]').first()
  await modal.waitFor({ state: 'visible', timeout: 30000 })
  await modal.locator('.frontline-pqc-piece-row').nth(uiQuantity - 1).waitFor({
    state: 'visible',
    timeout: 30000
  })
  const rowCount = await modal.locator('.frontline-pqc-piece-row').count()
  assert.equal(rowCount, uiQuantity, 'PQC 逐件弹窗行数必须等于页面计划检验数量。')
  const modalTitle = (await modal.locator('h3').first().innerText()).replace(/\s+/g, ' ').trim()
  assert.ok(
    modalTitle.includes(`（${uiQuantity}件）`),
    `PQC 逐件弹窗标题必须展示计划数量 ${uiQuantity} 件。`
  )
  await modal.getByRole('button', { name: '返回' }).click()
  await modal.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})

  return {
    key: 'pqcPieceDetailQuantityPrepared',
    label: 'PQC 逐件明细数量按计划数量准备',
    roleKey: 'pqcInspector',
    status: 'PASS',
    acceptanceIds: ['AC-D27'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    plannedQuantities,
    uiQuantity,
    pieceRowCount: rowCount,
    inspectionEntry: entryLabel,
    sourceActionKey: regulationEvidence.key
  }
}

function resolveCurrentUserId(permissionInfo) {
  const candidates = [
    permissionInfo?.user?.id,
    permissionInfo?.user?.userId,
    permissionInfo?.userId,
    permissionInfo?.id
  ]
  for (const candidate of candidates) {
    const value = Number(candidate)
    if (Number.isFinite(value) && value > 0) return value
  }
  return undefined
}

function formatPqcPersonnelLabel(candidate) {
  return candidate?.nickname || candidate?.employeeName || candidate?.username ||
    candidate?.employeeCode || String(candidate?.userId || '')
}

async function loadPqcPersonnelViaAuth(page, label) {
  const result = await fetchWithPageAuth(
    page,
    '/admin-api/mes/pro/feedback/frontline/device-account/pqc/personnel'
  )
  if (!result.ok) {
    return {
      blocked: true,
      description: `${label} HTTP 失败：${result.status}`
    }
  }
  if (result.body?.code !== 0) {
    return {
      blocked: true,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body),
      description: `${label} 业务失败：${responseMessage(result.body)}`
    }
  }
  if (!Array.isArray(result.body.data)) {
    return {
      blocked: true,
      description: `${label} 必须返回数组。`
    }
  }
  return {
    blocked: false,
    candidates: result.body.data
  }
}

async function verifyPqcActualEmployeeSwitch(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const readOnlyEvidence = actionEvidence.find((item) => item.key === 'activeOrderCrossRoleReadOnly' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId || !readOnlyEvidence) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      description: 'PQC 实际检验人选择前缺少同一 activeOrderId 的页面只读证据，不能证明人员选择绑定到正式活跃订单。'
    }
  }

  const permissionInfo = await getCurrentPermissionInfo(page)
  const loginUserId = resolveCurrentUserId(permissionInfo)
  const personnelResult = await loadPqcPersonnelViaAuth(page, 'PQC 人员范围')
  if (personnelResult.blocked) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      loginUserId,
      responseCode: personnelResult.responseCode,
      responseMessage: personnelResult.responseMessage,
      description: `${personnelResult.description}；需要在本机测试租户补齐正式 PQC 组长/员工 EMPLOYEE scope 后，才能证明 actualEmployeeId 不默认登录人。`
    }
  }
  const candidates = personnelResult.candidates
  const selectableCandidates = candidates
    .map((candidate) => ({
      ...candidate,
      userId: Number(candidate.userId),
      label: formatPqcPersonnelLabel(candidate)
    }))
    .filter((candidate) => Number.isFinite(candidate.userId) && candidate.userId > 0 && candidate.label)
  if (!selectableCandidates.length) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      description: 'PQC 人员范围接口没有返回可选择人员，不能证明共享账号下实际检验人来源。'
    }
  }
  const targetCandidate = selectableCandidates.find((candidate) => candidate.userId !== loginUserId) ||
    selectableCandidates[0]
  if (loginUserId && targetCandidate.userId === loginUserId) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      loginUserId,
      candidateCount: selectableCandidates.length,
      description: 'PQC 人员范围只返回当前登录账号，不能证明 actualEmployeeId 不默认等于登录人。'
    }
  }

  const employeeCard = page
    .locator('.frontline-operator-top.is-pqc .frontline-top-card')
    .filter({ hasText: '员工' })
    .first()
  await employeeCard.waitFor({ state: 'visible', timeout: 30000 })
  await employeeCard.click()
  const targetOption = page
    .locator('.frontline-picker__options button')
    .filter({ hasText: targetCandidate.label })
    .first()
  await targetOption.waitFor({ state: 'visible', timeout: 30000 })
  const switchResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/feedback/frontline/device-account/pqc/switch-employee')
      && response.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ pqcSwitchEmployeeResponseError: error }))
  await targetOption.click()
  const switchResponse = await switchResponsePromise
  if (switchResponse.pqcSwitchEmployeeResponseError) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      actualEmployeeId: targetCandidate.userId,
      description: `页面点击实际 PQC 人员后未捕获 switch-employee 响应：${switchResponse.pqcSwitchEmployeeResponseError.message}`
    }
  }
  assert.ok(switchResponse.ok(), `PQC 实际人员切换 HTTP 失败：${switchResponse.status()}`)
  const switchBody = await switchResponse.json()
  assert.equal(switchBody.code, 0, `PQC 实际人员切换业务失败：${responseMessage(switchBody)}`)
  assert.equal(
    Number(switchBody.data?.actualEmployeeId),
    targetCandidate.userId,
    'PQC 实际人员切换响应必须保存页面选择的 actualEmployeeId。'
  )
  if (loginUserId) {
    assert.notEqual(
      Number(switchBody.data?.actualEmployeeId),
      loginUserId,
      '共享账号 PQC 实际检验人不能默认等于当前登录用户。'
    )
  }
  await employeeCard.filter({ hasText: targetCandidate.label }).waitFor({ state: 'visible', timeout: 30000 })

  return {
    key: 'pqcActualEmployeeSelected',
    label: '共享账号下选择实际 PQC 检验人员',
    roleKey: 'pqcInspector',
    status: 'PASS',
    acceptanceIds: ['AC-D25', 'AC-D31'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    loginUserId,
    actualEmployeeId: targetCandidate.userId,
    candidateCount: selectableCandidates.length,
    employeeLabel: targetCandidate.label,
    templateNo: switchBody.data?.template?.templateNo,
    sourceActionKey: readOnlyEvidence.key
  }
}

async function verifyActiveOrderUnauthorizedMutationBlocked(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId) {
    return {
      key: 'activeOrderUnauthorizedMutationBlocked',
      label: '错误角色活跃订单写入权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'BLOCKED',
      category: 'E2E_PERMISSION',
      acceptanceIds: ['AC-M04', 'AC-D09', 'AC-D13'],
      description: '权限隔离核验前缺少生产组长 joinActiveOrder 动作证据，不能证明错误角色无法复用同一 activeOrderId。'
    }
  }

  const unauthorizedActor = config.unauthorizedActor || {}
  if (!unauthorizedActor.username || !unauthorizedActor.password) {
    return {
      key: 'activeOrderUnauthorizedMutationBlocked',
      label: '错误角色活跃订单写入权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'BLOCKED',
      category: 'E2E_PERMISSION',
      acceptanceIds: ['AC-M04', 'AC-D09', 'AC-D13'],
      activeOrderId: joinEvidence.activeOrderId,
      description: '缺少 RRM_UNAUTHORIZED_USERNAME / RRM_UNAUTHORIZED_PASSWORD，不能执行错误角色真实登录与后端写入拒绝核验。'
    }
  }

  const browser = page.context().browser()
  assert.ok(browser, '错误角色权限隔离必须能创建独立浏览器上下文。')
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const unauthorizedPage = await context.newPage()
  try {
    await login(unauthorizedPage, config, 'unauthorizedActor', unauthorizedActor)
    const permissionInfo = await getCurrentPermissionInfo(unauthorizedPage)
    const permissions = extractPermissions(permissionInfo)
    if (hasActiveOrderMaintainPermission(permissions)) {
      const blockedPermission = permissions.includes('*:*:*') ? '*:*:*' : ACTIVE_ORDER_MAINTAIN_PERMISSION
      return {
        key: 'activeOrderUnauthorizedMutationBlocked',
        label: '错误角色活跃订单写入权限隔离',
        roleKey: 'unauthorizedActor',
        status: 'BLOCKED',
        category: 'E2E_PERMISSION',
        acceptanceIds: ['AC-M04', 'AC-D09', 'AC-D13'],
        activeOrderId: joinEvidence.activeOrderId,
        blockedPermission,
        username: unauthorizedActor.username,
        description: `当前错误角色夹具 ${unauthorizedActor.username} 仍具备 ${blockedPermission}，无法证明错误角色会被后端拒绝；需改为不含活跃订单维护权限的正式角色夹具后再执行写入拒绝核验。`
      }
    }

    const targetUrl = new URL('/mes/pro/process-pool/team-leader', config.frontendUrl)
    await unauthorizedPage.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
    await unauthorizedPage.locator('#app').waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await unauthorizedPage.locator('[data-team-leader-active-order-config]').count(),
      0,
      '错误角色不应看到活跃订单维护表单。'
    )

    const result = await fetchWithPageAuth(unauthorizedPage, '/admin-api/mes/pro/process-pool/team-leader/active-order/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        workOrderId: config.workOrderId,
        routeId: config.routeId,
        routeVersionId: config.routeVersionId
      })
    })
    assert.ok(
      !result.ok || !isBusinessSuccess(result.body),
      '错误角色调用活跃订单写入接口必须被后端拒绝，不能返回业务成功。'
    )
    return {
      key: 'activeOrderUnauthorizedMutationBlocked',
      label: '错误角色活跃订单写入权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'PASS',
      acceptanceIds: ['AC-M04', 'AC-D09', 'AC-D13'],
      activeOrderId: joinEvidence.activeOrderId,
      username: unauthorizedActor.username,
      responseStatus: result.status,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body)
    }
  } finally {
    await context.close()
  }
}

async function verifyActiveOrderCleanupTraceability(page, config, joinEvidence) {
  if (!joinEvidence?.activeOrderId) {
    return {
      key: 'activeOrderCleanupDeferred',
      label: '活跃订单清理闭环风险记录',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_CLEANUP',
      acceptanceIds: ['AC-M04'],
      description: '清理核验前缺少生产组长 joinActiveOrder 动作证据，不能判断任务夹具是否可安全删除。'
    }
  }

  const rows = await reloadActiveOrderRows(page)
  const activeOrder = rows.find((row) => Number(row.id) === Number(joinEvidence.activeOrderId))
  assert.ok(activeOrder, '清理闭环必须能重新定位本轮 activeOrderId。')
  assert.equal(Number(activeOrder.workOrderId), Number(config.workOrderId), '清理闭环定位到的 activeOrder 必须属于任务工单。')
  assert.equal(Number(activeOrder.routeId), Number(config.routeId), '清理闭环定位到的 activeOrder 必须属于任务路线。')
  assert.equal(Number(activeOrder.routeVersionId), Number(config.routeVersionId), '清理闭环定位到的 activeOrder 必须属于任务路线版本。')

  return {
    key: 'activeOrderCleanupDeferred',
    label: '活跃订单清理闭环风险记录',
    roleKey: 'productionLeader',
    status: 'BLOCKED',
    category: 'E2E_CLEANUP',
    acceptanceIds: ['AC-M04'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId,
    description: '本轮 activeOrderId 仍是 M6 后续真实 E2E 共享夹具；直接移除会破坏 PQC、放行、日结和后续验证链路。需要一次性可重建夹具或明确清理窗口后再执行删除验证。'
  }
}

async function verifyRealFlowPhase(page, config, phase) {
  await page.goto(new URL(phase.targetPath, config.frontendUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  assert.ok(!page.url().includes('/login'), `${phase.label} 被重定向到登录页，角色权限或会话无效。`)
  if (phase.tabText) {
    const tab = page.getByRole('tab', { name: phase.tabText }).first()
    await tab.waitFor({ state: 'visible', timeout: 60000 })
    await tab.click()
  }
  const selectorEvidence = []
  for (const selector of phase.selectors) {
    const locator = page.locator(selector).first()
    await locator.waitFor({ state: 'visible', timeout: 60000 })
    selectorEvidence.push(selector)
  }
  return {
    key: phase.key,
    label: phase.label,
    roleKey: phase.roleKey,
    targetPath: phase.targetPath,
    status: 'PASS',
    selectors: selectorEvidence,
    acceptanceIds: phase.acceptanceIds
  }
}

async function runPhaseAction(page, config, phase, actionEvidence) {
  if (phase.actionKey === 'joinActiveOrder') {
    const joinEvidence = await performActiveOrderJoin(page, config)
    const conflictRouteEvidence = await verifyActiveOrderConflictRouteFailure(page, config, joinEvidence)
    const cleanupEvidence = await verifyActiveOrderCleanupTraceability(page, config, joinEvidence)
    return [joinEvidence, conflictRouteEvidence, cleanupEvidence]
  }
  if (phase.actionKey === 'verifyPqcActiveOrderReadOnly') {
    const readOnlyEvidence = await verifyPqcActiveOrderReadOnly(page, config, actionEvidence)
    const regulationEvidence = await verifyPqcRegulationItemsRendered(page, config, [...actionEvidence, readOnlyEvidence])
    const pieceDetailEvidence = await verifyPqcPieceDetailQuantityPrepared(page, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence
    ])
    const employeeEvidence = await verifyPqcActualEmployeeSwitch(page, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence
    ])
    return [readOnlyEvidence, regulationEvidence, pieceDetailEvidence, employeeEvidence]
  }
  if (phase.actionKey === 'verifyActiveOrderUnauthorizedMutationBlocked') {
    return verifyActiveOrderUnauthorizedMutationBlocked(page, config, actionEvidence)
  }
  return undefined
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
  const acceptanceMatrix = loadAcceptanceMatrix()
  const phaseEvidence = []
  const actionEvidence = []
  try {
    for (const [roleKey] of ROLE_CONFIGS) {
      const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
      const page = await context.newPage()
      try {
        await login(page, config, roleKey, config.roles[roleKey])
        const phases = M6_REAL_FLOW_PHASES.filter((phase) => phase.roleKey === roleKey)
        if (!phases.length) {
          await page.goto(new URL(config.roles[roleKey].targetPath, config.frontendUrl).toString(), {
            waitUntil: 'domcontentloaded',
            timeout: 90000
          })
        }
        for (const phase of phases) {
          phaseEvidence.push(await verifyRealFlowPhase(page, config, phase))
          const action = await runPhaseAction(page, config, phase, actionEvidence)
          if (action) {
            for (const item of Array.isArray(action) ? action : [action]) {
              actionEvidence.push(item)
            }
          }
        }
      } finally {
        await context.close()
      }
    }
  } finally {
    await browser.close()
  }

  const acceptanceCoverage = buildAcceptanceCoverage(acceptanceMatrix, phaseEvidence, actionEvidence)
  const coverageBlockers = assertAcceptanceCoverage(acceptanceCoverage)
  const actionBlockers = buildActionBlockers(actionEvidence)
  const gateEvidence = buildM6ConcurrencyPerformanceGateEvidence(acceptanceMatrix, actionEvidence)
  const gateBlockers = buildGateBlockers(gateEvidence)
  const result = {
    status: coverageBlockers.length || actionBlockers.length || gateBlockers.length ? 'BLOCKED' : 'PASS',
    mode: 'real',
    config: redactConfig(config),
    phaseEvidence,
    actionEvidence,
    gateEvidence,
    acceptanceCoverage,
    blockers: [...actionBlockers, ...gateBlockers, ...coverageBlockers]
  }
  writeEvidence(result)
  if (result.blockers.length) {
    printBlockers(result.blockers)
    process.exitCode = 2
    return
  }
  console.log('PASS role-requirement-matrix full real E2E')
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
