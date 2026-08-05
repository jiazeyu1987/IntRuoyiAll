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
const PRODUCTION_FILL_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const PQC_FILL_ROUTE = '/mes/pro/feedback/edhr-batch-pqc-fill'
const FRONTLINE_SUBMIT_ENDPOINT = '/mes/pro/feedback/frontline/submit'
const PRODUCTION_FEEDBACK_TYPE_SELF = 1

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
const PROCESS_POOL_TIMELINE_FILTER_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/ProcessPoolTimelineFilterTest.java'
)
const TEAM_LEADER_ACTIVE_ORDER_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceTest.java'
)
const FRONTLINE_PQC_CONTEXT_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java'
)
const TEAM_LEADER_REPORT_CONFIRMATION_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderReportConfirmationServiceTest.java'
)
const TEAM_LEADER_ORDER_PROCESS_COMPLETION_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderOrderProcessCompletionServiceTest.java'
)
const TEAM_LEADER_BATCH_RECORD_BACKFILL_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderBatchRecordBackfillServiceTest.java'
)
const TEAM_LEADER_SUBMISSION_REVIEW_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceTest.java'
)
const PQC_PROCESS_INSPECTION_AGGREGATION_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesPqcProcessInspectionAggregationServiceTest.java'
)
const ACTIVE_ORDER_TRANSFER_TRACE_SCHEMA_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesActiveOrderTransferTraceSchemaTest.java'
)
const ACTIVE_ORDER_TRANSFER_TRACE_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesActiveOrderTransferTraceServiceTest.java'
)
const EDHR_RELEASE_SERVICE_TEST = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrReleaseServiceImplTest.java'
)
const PROCESS_POOL_TIMELINE_PERFORMANCE_SQL = path.resolve(
  BACKEND_ROOT,
  'sql/mysql/20260804_mes_process_pool_timeline_performance_indexes.sql'
)
const PROCESS_POOL_TIMELINE_MAPPER_XML = path.resolve(
  BACKEND_ROOT,
  'yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml'
)

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
  ['pqcInspector', 'RRM_PQC_INSPECTOR_USERNAME', 'RRM_PQC_INSPECTOR_PASSWORD', '/index'],
  ['pqcLeader', 'RRM_PQC_LEADER_USERNAME', 'RRM_PQC_LEADER_PASSWORD', '/mes/pro/process-pool/pqc-leader'],
  ['qa', 'RRM_QA_USERNAME', 'RRM_QA_PASSWORD', '/mes/qc/template'],
  ['releaseOwner', 'RRM_RELEASE_OWNER_USERNAME', 'RRM_RELEASE_OWNER_PASSWORD', '/mes/pro/feedback/edhr-release']
]

const M6_REAL_FLOW_PHASES = [
  {
    key: 'productionLeaderWorkbench',
    roleKey: 'productionLeader',
    label: '生产组长工作台与日结/分配表面',
    targetPath: '/mes/pro/process-pool/team-leader',
    selectorGroups: [
      {
        tabText: '报工管理',
        selectors: ['[data-team-leader-report-workbench]', '[data-role-matrix-daily-close]']
      },
      {
        tabText: '班组配置',
        selectors: ['[data-team-leader-config-center]', '[data-team-leader-active-order-config]']
      }
    ],
    actionKey: 'joinActiveOrder',
    acceptanceIds: ['AC-M04', 'AC-M16', 'AC-M17', 'AC-M18', 'AC-D09', 'AC-D12', 'AC-D14', 'AC-D38']
  },
  {
    key: 'pqcLeaderWorkbench',
    roleKey: 'pqcLeader',
    label: 'PQC 组长复核表面',
    targetPath: '/mes/pro/process-pool/pqc-leader',
    selectorGroups: [
      {
        tabText: 'PQC管理',
        selectors: ['[data-pqc-leader-workbench-page]', '[data-team-leader-report-workbench]']
      },
      {
        tabText: '看板',
        selectors: ['[data-role-matrix-daily-close]']
      }
    ],
    actionKey: 'verifyPqcLeaderSubmissionFilterPaginationConsistency',
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
    actionKey: 'verifyQaRegulationPublishedVersionReadOnly',
    acceptanceIds: ['AC-M09', 'AC-D15', 'AC-D16', 'AC-D17', 'AC-D18', 'AC-D19', 'AC-D20', 'AC-D21', 'AC-D22', 'AC-D23']
  },
  {
    key: 'releaseOwnerEntry',
    roleKey: 'releaseOwner',
    label: '放行负责人入口',
    targetPath: '/mes/pro/feedback/edhr-release',
    selectors: ['#app'],
    actionKey: 'verifyEdhrReleaseTraceabilityReadOnly',
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

function parseSignatureIds(rawValue) {
  if (!rawValue) return {}
  try {
    const parsed = JSON.parse(rawValue)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      return { __parseError: 'RRM_SIGNATURE_IDS_JSON 必须是角色到签名ID的 JSON 对象。' }
    }
    return parsed
  } catch (error) {
    return { __parseError: error.message }
  }
}

function parsePositiveIntegerEnvList(rawValue, key) {
  if (!rawValue) return []
  const values = []
  for (const item of rawValue.split(/[,\s，]+/).filter(Boolean)) {
    const parsed = Number(item)
    if (!Number.isInteger(parsed) || parsed <= 0) {
      return { __parseError: `${key} 包含非法 ID：${item}` }
    }
    values.push(parsed)
  }
  return values
}

function readText(filePath) {
  if (!fs.existsSync(filePath)) {
    failFast(`缺少源文件：${path.relative(WORKSPACE_ROOT, filePath)}`)
  }
  return fs.readFileSync(filePath, 'utf8').replace(/\r\n/g, '\n')
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
    productionOrderCode: envValue('RRM_PRODUCTION_ORDER_CODE'),
    routeId: Number(envValue('RRM_ROUTE_ID')),
    routeVersionId: Number(envValue('RRM_ROUTE_VERSION_ID')),
    routeProcessIds: [
      Number(envValue('RRM_ROUTE_PROCESS_ID_1')),
      Number(envValue('RRM_ROUTE_PROCESS_ID_2'))
    ],
    primaryRouteProcessId: Number(envValue('RRM_ROUTE_PROCESS_ID_1')),
    transferIds: parsePositiveIntegerEnvList(envValue('RRM_TRANSFER_IDS'), 'RRM_TRANSFER_IDS'),
    qaRegulationVersionId: Number(envValue('RRM_QA_REGULATION_VERSION_ID')),
    signatureIds: parseSignatureIds(envValue('RRM_SIGNATURE_IDS_JSON')),
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

  if (config.signatureIds.__parseError) {
    blockers.push({
      key: 'RRM_SIGNATURE_IDS_JSON',
      category: 'ENV',
      description: `签名 ID 映射不是有效 JSON：${config.signatureIds.__parseError}`
    })
  }
  const pqcSignatureId = Number(config.signatureIds.pqcInspector)
  if (!Number.isFinite(pqcSignatureId) || pqcSignatureId <= 0) {
    blockers.push({
      key: 'RRM_SIGNATURE_IDS_JSON.pqcInspector',
      category: 'ENV',
      description: 'PQC 正式提交必须从 RRM_SIGNATURE_IDS_JSON.pqcInspector 读取大于 0 的正式电子签名 ID。'
    })
  }

  if (!Array.isArray(config.transferIds) || config.transferIds.length === 0 || config.transferIds.__parseError) {
    blockers.push({
      key: 'RRM_TRANSFER_IDS',
      category: 'ENV',
      description: config.transferIds.__parseError || 'RRM_TRANSFER_IDS 必须提供至少一个大于 0 的正式调拨/发货/补料/退料 ID。'
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
  if (pqcSource.includes('selectActiveByWorkOrderRouteProcess')
    && !pqcSource.includes('createPqcInspectionEvent')) {
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

function collectM6PerformanceProofs() {
  const timelineFilterTestSource = readText(PROCESS_POOL_TIMELINE_FILTER_TEST)
  const activeOrderServiceTestSource = readText(TEAM_LEADER_ACTIVE_ORDER_SERVICE_TEST)
  const frontlinePqcContextServiceTestSource = readText(FRONTLINE_PQC_CONTEXT_SERVICE_TEST)
  const timelinePerformanceSql = readText(PROCESS_POOL_TIMELINE_PERFORMANCE_SQL)
  const timelineMapperXml = readText(PROCESS_POOL_TIMELINE_MAPPER_XML)

  const proofs = {
    dailyCloseSubmissionQueryCount:
      /shouldUseCountAndPageQueriesWithoutDetailLookupsForDailyCloseSubmissionSummary[\s\S]*assertEquals\(1,\s*mapper\.getCountQueryCalls\(\)[\s\S]*assertEquals\(1,\s*mapper\.getPageQueryCalls\(\)[\s\S]*assertEquals\(0,\s*mapper\.getDetailQueryCalls\(\)/.test(timelineFilterTestSource),
    dailyCloseActiveOrderSingleQuery:
      /shouldListActiveOrdersWithSingleActiveOrderQueryForDailyClosePerformance[\s\S]*selectActiveList\(\)[\s\S]*never\(\)\)\.selectListByScheduleOrderId[\s\S]*never\(\)\)\.insertBatch/.test(activeOrderServiceTestSource),
    pieceDetailBulkQuery:
      /shouldPreparePqcPieceDetailContextWithBulkQueriesOnly[\s\S]*selectListByRouteId\(ROUTE_ID\)[\s\S]*selectListByActiveOrderId\(ACTIVE_ORDER_ID\)[\s\S]*selectListByVersionId\(REGULATION_VERSION_ID\)[\s\S]*never\(\)\)\.selectPendingByActiveOrderProcess[\s\S]*never\(\)\)\.selectById/.test(frontlinePqcContextServiceTestSource),
    d32GeneratedColumnAndIndexes:
      /pqc_task_id/.test(timelinePerformanceSql)
      && /idx_mes_pp_event_timeline_acd32/.test(timelinePerformanceSql)
      && /idx_mes_pqc_task_timeline_acd32/.test(timelinePerformanceSql)
      && /idx_mes_pp_review_latest_event/.test(timelinePerformanceSql),
    d32IndexedMapperJoin:
      /pqc_task\.id\s*=\s*pool_event\.pqc_task_id/.test(timelineMapperXml)
      && !/JSON_EXTRACT\(pool_event\.raw_payload,\s*'\$\.pqcTaskId'\)/.test(timelineMapperXml),
    d32TimelineQueryCount:
      /shouldUseCountAndPageQueriesWithoutPerRowDetailLookupsForPqcPagination[\s\S]*assertEquals\(1,\s*mapper\.getCountQueryCalls\(\)[\s\S]*assertEquals\(1,\s*mapper\.getPageQueryCalls\(\)[\s\S]*assertEquals\(0,\s*mapper\.getDetailQueryCalls\(\)[\s\S]*assertEquals\(2,\s*mapper\.getCountQueryCalls\(\)[\s\S]*assertEquals\(2,\s*mapper\.getPageQueryCalls\(\)[\s\S]*assertEquals\(0,\s*mapper\.getDetailQueryCalls\(\)/.test(timelineFilterTestSource)
  }

  return {
    ...proofs,
    complete: Object.values(proofs).every(Boolean)
  }
}

function collectM6ConcurrencyProofs(concurrencyAcceptanceIds) {
  const activeOrderServiceTestSource = readText(TEAM_LEADER_ACTIVE_ORDER_SERVICE_TEST)
  const reportConfirmationServiceTestSource = readText(TEAM_LEADER_REPORT_CONFIRMATION_SERVICE_TEST)
  const orderProcessCompletionServiceTestSource = readText(TEAM_LEADER_ORDER_PROCESS_COMPLETION_SERVICE_TEST)
  const batchRecordBackfillServiceTestSource = readText(TEAM_LEADER_BATCH_RECORD_BACKFILL_SERVICE_TEST)
  const frontlinePqcContextServiceTestSource = readText(FRONTLINE_PQC_CONTEXT_SERVICE_TEST)
  const submissionReviewServiceTestSource = readText(TEAM_LEADER_SUBMISSION_REVIEW_SERVICE_TEST)
  const processInspectionAggregationServiceTestSource = readText(PQC_PROCESS_INSPECTION_AGGREGATION_SERVICE_TEST)
  const transferTraceSchemaTestSource = readText(ACTIVE_ORDER_TRANSFER_TRACE_SCHEMA_TEST)
  const transferTraceServiceTestSource = readText(ACTIVE_ORDER_TRANSFER_TRACE_SERVICE_TEST)
  const releaseServiceTestSource = readText(EDHR_RELEASE_SERVICE_TEST)

  const proofCatalog = {
    'AC-M04': {
      proofKey: 'activeOrderConcurrentJoin',
      proved: /shouldReturnExistingActiveOrderWhenConcurrentInsertHitsUniqueKey[\s\S]*selectActiveByWorkOrderRouteVersion[\s\S]*DuplicateKeyException[\s\S]*uk_mes_pp_active_order[\s\S]*assertEquals\(8102L,\s*activeOrderId\)/.test(activeOrderServiceTestSource),
      evidence: 'MesTeamLeaderActiveOrderServiceTest proves duplicate active-order join handles the unique-key race by reloading the existing active order.'
    },
    'AC-M07': {
      proofKey: 'transferTraceConcurrentIdempotency',
      proved: /idempotencyKey/.test(transferTraceSchemaTestSource)
        && /UNIQUE KEY `uk_mes_pp_active_order_transfer_trace`/.test(readText(ACTIVE_ORDER_TRANSFER_TRACE_SQL))
        && /shouldReturnExistingTransferTraceWhenConcurrentInsertHitsUniqueKey[\s\S]*DuplicateKeyException[\s\S]*selectByIdempotencyKey/.test(transferTraceServiceTestSource)
        && /recordTransferTrace[\s\S]*shouldReturnExistingTransferTraceWhenSameIdempotencyKeyAlreadyRecorded/.test(transferTraceServiceTestSource),
      evidence: 'MesActiveOrderTransferTraceServiceTest proves duplicate/concurrent transfer-trace recording reloads the existing trace by idempotency key.'
    },
    'AC-M16': {
      proofKey: 'productionReviewDuplicateConfirmationBlocked',
      proved: /shouldBlockDuplicateConfirmationBeforeCreatingReview[\s\S]*selectByIdForUpdate[\s\S]*PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE[\s\S]*verify\(reviewMapper,\s*never\(\)\)\.insert/.test(reportConfirmationServiceTestSource),
      evidence: 'MesTeamLeaderReportConfirmationServiceTest proves duplicate production confirmation is blocked before creating review/allocation rows.'
    },
    'AC-M17': {
      proofKey: 'productionAllocationLockAndTotalGuard',
      proved: /selectListByWorkOrderIdsAndProcessForUpdate/.test(reportConfirmationServiceTestSource)
        && /shouldBlockWhenAllocationTotalDoesNotEqualSubmittedQuantity[\s\S]*PRO_PROCESS_POOL_REPORT_ALLOCATION_TOTAL_MISMATCH[\s\S]*verify\(reviewMapper,\s*never\(\)\)\.insert/.test(reportConfirmationServiceTestSource),
      evidence: 'MesTeamLeaderReportConfirmationServiceTest proves allocation reads existing quantities under lock and rejects mismatched allocation totals.'
    },
    'AC-M18': {
      proofKey: 'orderProgressCompletionLockOnly',
      proved: /selectByWorkOrderAndProcessForUpdate/.test(orderProcessCompletionServiceTestSource)
        && /shouldNotBackfillAgainWhenOrderProcessAlreadyCompleted[\s\S]*never\(\)\)\.backfillCompletedProcess/.test(orderProcessCompletionServiceTestSource)
        && /shouldRejectConcurrentProgressUpdate|shouldBlockConcurrentOrderProcessCompletion|shouldPreventOverTargetProgress/.test(orderProcessCompletionServiceTestSource),
      evidence: 'MesTeamLeaderOrderProcessCompletionServiceTest proves locked order-process progress reads, duplicate backfill suppression, and over-target concurrent progress rejection.'
    },
    'AC-M19': {
      proofKey: 'batchRecordBackfillConcurrentIdempotency',
      proved: /shouldBackfillCompletedProcessOnlyOnceWhenConcurrentAuditAlreadyApplied[\s\S]*PROCESS_POOL_REPORT_BACKFILL:1001:9001:5001[\s\S]*times\(2\)[\s\S]*saveSystemCellLinkChanges/.test(batchRecordBackfillServiceTestSource),
      evidence: 'MesTeamLeaderBatchRecordBackfillServiceTest proves repeated/concurrent batch-record backfill uses the same idempotency key and delegates duplicate suppression to field audit.'
    },
    'AC-M20': {
      proofKey: 'pqcReviewDuplicateTerminalBlocked',
      proved: /shouldRejectDuplicateTerminalReviewForSameSubmission[\s\S]*selectByIdForUpdate[\s\S]*PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS[\s\S]*verify\(reviewMapper,\s*never\(\)\)\.insert/.test(submissionReviewServiceTestSource),
      evidence: 'MesTeamLeaderSubmissionReviewServiceTest proves duplicate terminal PQC review is rejected before inserting another terminal review.'
    },
    'AC-M21': {
      proofKey: 'processInspectionAggregationConcurrentDuplicateBlocked',
      proved: /shouldRejectConcurrentDuplicateAggregationWhenPendingWasConsumed[\s\S]*updateProcessInspectionAggregatedIfPending[\s\S]*thenReturn\(0\)[\s\S]*PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED/.test(processInspectionAggregationServiceTestSource),
      evidence: 'MesPqcProcessInspectionAggregationServiceTest proves process-inspection aggregation fails when another transaction consumes the pending state.'
    },
    'AC-M23': {
      proofKey: 'releaseTerminalForUpdateConcurrency',
      proved: /shouldRejectConcurrentReleaseTerminalWhenPrecheckWasConsumedUnderForUpdateLock[\s\S]*STATUS_PRECHECK_PASSED[\s\S]*STATUS_RELEASED[\s\S]*selectByIdForUpdate[\s\S]*PRO_EDHR_RELEASE_PRECHECK_REQUIRED[\s\S]*assertEquals\(1,\s*batchSignatureMapper\.selectListByBatchExecutionId/.test(releaseServiceTestSource),
      evidence: 'MesProEdhrReleaseServiceImplTest proves duplicate/concurrent release terminal transition rereads the locked transaction and rejects a consumed precheck before another release signature can be created.'
    },
    'AC-D29': {
      proofKey: 'pqcSubmitConsumedPendingTaskBlocked',
      proved: /shouldRejectPqcInspectionWhenPendingTaskWasConsumedConcurrently[\s\S]*updateSubmittedIfPending[\s\S]*thenReturn\(0\)[\s\S]*PRO_FRONTLINE_PQC_TASK_STATUS_INVALID/.test(frontlinePqcContextServiceTestSource),
      evidence: 'MesFrontlinePqcContextServiceTest proves a consumed pending PQC task cannot create a second formal PQC event.'
    },
    'AC-D34': {
      proofKey: 'pqcDuplicateTerminalReviewBlocked',
      proved: /shouldRejectDuplicateTerminalReviewForSameSubmission[\s\S]*PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS[\s\S]*verify\(reviewMapper,\s*never\(\)\)\.insert/.test(submissionReviewServiceTestSource),
      evidence: 'MesTeamLeaderSubmissionReviewServiceTest proves duplicate terminal review is rejected before insert.'
    },
    'AC-D37': {
      proofKey: 'processInspectionAggregationConcurrentDuplicateBlocked',
      proved: /shouldRejectConcurrentDuplicateAggregationWhenPendingWasConsumed[\s\S]*PRO_PROCESS_POOL_PQC_PROCESS_INSPECTION_ALREADY_AGGREGATED/.test(processInspectionAggregationServiceTestSource),
      evidence: 'MesPqcProcessInspectionAggregationServiceTest proves duplicate process-inspection aggregation is rejected.'
    }
  }

  const byAcceptanceId = Object.fromEntries(concurrencyAcceptanceIds.map((ac) => [
    ac,
    proofCatalog[ac] || {
      proofKey: 'missingConcurrencyProof',
      proved: false,
      evidence: `No M6 concurrency proof catalog entry exists for ${ac}.`
    }
  ]))
  const provedAcceptanceIds = Object.entries(byAcceptanceId)
    .filter(([, proof]) => proof.proved)
    .map(([ac]) => ac)
  const missingAcceptanceIds = Object.entries(byAcceptanceId)
    .filter(([, proof]) => !proof.proved)
    .map(([ac]) => ac)

  return {
    byAcceptanceId,
    provedAcceptanceIds,
    missingAcceptanceIds,
    complete: missingAcceptanceIds.length === 0
  }
}

function getPassedAction(actionEvidence, key) {
  return actionEvidence.find((action) => action.key === key && action.status === 'PASS')
}

function hasCompleteM6ConcurrencyGateEvidence(concurrencyProofs) {
  return concurrencyProofs.complete && concurrencyProofs.missingAcceptanceIds.length === 0
}

function hasCompleteM6PerformanceGateEvidence(performanceActions, performanceAcceptanceIds, performanceProofs) {
  const observedAcceptanceIds = new Set(performanceActions.flatMap((action) => action.acceptanceIds || []))
  if (!performanceAcceptanceIds.every((ac) => observedAcceptanceIds.has(ac))) return false
  if (!performanceProofs.complete) return false

  const dailyClose = getPassedAction(performanceActions, 'dailyClosePerformanceReadOnly')
  const pieceDetail = getPassedAction(performanceActions, 'pqcPieceDetailQuantityPrepared')
  const leaderPagination = getPassedAction(performanceActions, 'pqcLeaderSubmissionFilterPaginationConsistent')
  if (!dailyClose || !pieceDetail || !leaderPagination) return false

  return dailyClose.requestBudget?.submissionPageRequests === 0
    && dailyClose.requestBudget?.activeOrderListRequests === 0
    && dailyClose.requestBudget?.submissionDetailRequests === 0
    && pieceDetail.requestBudget?.pieceDetailRequests === 0
    && pieceDetail.requestBudget?.processSnapshotRequests === 0
    && pieceDetail.requestBudget?.pqcPersonnelRequests === 0
    && leaderPagination.requestBudget?.submissionPageRequests >= 3
    && leaderPagination.requestBudget?.submissionPageRequests <= 4
    && leaderPagination.requestBudget?.submissionDetailRequests === 0
    && leaderPagination.requestBudget?.activeOrderListRequests === 0
    && leaderPagination.total >= 2
    && leaderPagination.firstEventId !== leaderPagination.secondEventId
}

function buildM6ConcurrencyPerformanceGateEvidence(acceptanceMatrix, actionEvidence) {
  const concurrencyAcceptanceIds = acceptanceIdsByLayer(acceptanceMatrix, 'CONC')
  const performanceAcceptanceIds = acceptanceIdsByLayer(acceptanceMatrix, 'PERF')
  const observedConcurrencyActionKeys = actionEvidence
    .filter((action) => action.status === 'PASS'
      && (action.acceptanceIds || []).some((ac) => concurrencyAcceptanceIds.includes(ac)))
    .map((action) => action.key)
  const observedConcurrencyAcceptanceIds = [
    ...new Set(actionEvidence
      .filter((action) => action.status === 'PASS')
      .flatMap((action) => action.acceptanceIds || []))
  ].filter((ac) => concurrencyAcceptanceIds.includes(ac))
  const concurrencyProofs = collectM6ConcurrencyProofs(concurrencyAcceptanceIds)
  const concurrencyGateStatus = hasCompleteM6ConcurrencyGateEvidence(concurrencyProofs)
  const concurrencyGateKey = concurrencyGateStatus ? 'm6ConcurrencyGateVerified' : 'm6ConcurrencyGateDeferred'
  const concurrencyGateDescription = concurrencyGateStatus
    ? `测试矩阵中 ${concurrencyAcceptanceIds.length} 个 CONC AC 已具备逐项服务级并发 proof；已证明 ${concurrencyProofs.provedAcceptanceIds.join(', ')}。AC 接受仍由 E2E_COVERAGE blocker 单独追踪。`
    : `测试矩阵中 ${concurrencyAcceptanceIds.length} 个 CONC AC 仍需逐项完成真实并发或服务级并发证据；当前已观察 ${observedConcurrencyAcceptanceIds.length} 个 CONC AC（${observedConcurrencyAcceptanceIds.join(', ') || '无'}），已有 proof ${concurrencyProofs.provedAcceptanceIds.join(', ') || '无'}，缺少 proof ${concurrencyProofs.missingAcceptanceIds.join(', ') || '无'}。`
  const performanceActions = actionEvidence
    .filter((action) => action.status === 'PASS'
      && (action.acceptanceIds || []).some((ac) => performanceAcceptanceIds.includes(ac)))
  const performanceActionKeys = performanceActions.map((action) => action.key)
  const observedPerformanceAcceptanceIds = [
    ...new Set(performanceActions.flatMap((action) => action.acceptanceIds || []))
  ].filter((ac) => performanceAcceptanceIds.includes(ac))
  const performanceProofs = collectM6PerformanceProofs()
  const performanceGateStatus = hasCompleteM6PerformanceGateEvidence(
    performanceActions,
    performanceAcceptanceIds,
    performanceProofs
  )
  const performanceGateKey = performanceGateStatus ? 'm6PerformanceGateVerified' : 'm6PerformanceGateDeferred'
  const performanceGateDescription = performanceGateStatus
    ? `测试矩阵中 ${performanceAcceptanceIds.length} 个 PERF AC 已具备真实页面 request-budget、后端查询计数、索引和分页总数证明；当前已观察 ${observedPerformanceAcceptanceIds.length} 个 PERF AC（${observedPerformanceAcceptanceIds.join(', ') || '无'}）。AC 接受仍由 E2E_COVERAGE blocker 单独追踪。`
    : `测试矩阵中 ${performanceAcceptanceIds.length} 个 PERF AC 仍需分页总数、索引或查询计数证据；当前已观察 ${observedPerformanceAcceptanceIds.length} 个 PERF AC（${observedPerformanceAcceptanceIds.join(', ') || '无'}），但 request-budget、后端查询计数、索引或分页总数证明尚未全部闭环。`

  return [
    {
      key: concurrencyGateKey,
      label: 'M6 并发门禁结构化',
      roleKey: 'system',
      status: concurrencyGateStatus ? 'PASS' : 'BLOCKED',
      category: 'E2E_CONCURRENCY',
      acceptanceIds: concurrencyAcceptanceIds,
      observedActionKeys: observedConcurrencyActionKeys,
      observedAcceptanceIds: observedConcurrencyAcceptanceIds,
      missingConcurrencyAcceptanceIds: concurrencyProofs.missingAcceptanceIds,
      provedConcurrencyAcceptanceIds: concurrencyProofs.provedAcceptanceIds,
      concurrencyProofs,
      description: concurrencyGateDescription
    },
    {
      key: performanceGateKey,
      label: 'M6 性能门禁结构化',
      roleKey: 'system',
      status: performanceGateStatus ? 'PASS' : 'BLOCKED',
      category: 'E2E_PERFORMANCE',
      acceptanceIds: performanceAcceptanceIds,
      observedActionKeys: performanceActionKeys,
      observedAcceptanceIds: observedPerformanceAcceptanceIds,
      performanceProofs,
      description: performanceGateDescription
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
    productionOrderCode: config.productionOrderCode,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId,
    routeProcessIds: Array.isArray(config.routeProcessIds) ? config.routeProcessIds : [],
    primaryRouteProcessId: config.primaryRouteProcessId,
    transferIds: Array.isArray(config.transferIds) ? config.transferIds : [],
    qaRegulationVersionId: config.qaRegulationVersionId,
    signatureIdRoles: Object.keys(config.signatureIds || {}).filter((key) => key !== '__parseError'),
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

async function fillElementPlusInput(section, selector, value) {
  const input = section.locator(`input${selector}, ${selector} input`).first()
  await input.waitFor({ state: 'visible', timeout: 30000 })
  await input.fill(String(value))
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

async function waitForPostLoginNavigationSettled(page, roleKey) {
  const leftLoginPage = await page.waitForURL(
    (url) => !url.pathname.includes('/login'),
    { timeout: 60000 }
  ).then(() => true).catch(() => false)
  if (!leftLoginPage && page.url().includes('/login')) {
    failFast(`${roleKey} 登录接口成功后仍停留在登录页，无法进入真实页面路径。`, [{
      key: 'loginPostRedirectTimeout',
      category: 'E2E_RUNTIME',
      description: `${roleKey} 登录接口已返回成功，但 60 秒内未完成前端登录后跳转；后续页面导航可能被登录重定向竞态中断。`
    }])
  }
  await page.locator('#app').waitFor({ state: 'visible', timeout: 60000 })
  assert.ok(!page.url().includes('/login'), `${roleKey} 登录后仍停留在登录页，不能继续真实 E2E。`)
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
  await waitForPostLoginNavigationSettled(page, roleKey)
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
  await fillFormItemForAction(section, '加入活跃订单', '调拨单ID列表', config.transferIds.join(','))
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
  const refreshedRows = Array.isArray(listBody.data) ? listBody.data : []
  const rows = refreshedRows.some((row) =>
    Number(row.id) === Number(activeOrderId) && Number(row.workOrderId) === Number(config.workOrderId)
  )
    ? refreshedRows
    : await reloadActiveOrderRows(page)
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
    routeVersionId: config.routeVersionId,
    transferIds: config.transferIds
  }
}

function extractAdmissionDiffRows(body) {
  const data = body?.data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data)) return data
  return []
}

async function verifyScheduleOrderErpCandidateAdmission(page, config) {
  const evidenceKey = 'scheduleOrderErpCandidateAdmission'
  const acceptanceIds = ['AC-M01']
  const pagePath = '/mes/pro/schedule-order'
  try {
    await page.goto(new URL(pagePath, config.frontendUrl).toString(), {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    assert.ok(!page.url().includes('/login'), 'AC-M01 排产工单页被重定向到登录页，生产组长会话无效。')
    const admissionResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/mes/pro/schedule-order/admission-diff')
        && response.request().method() === 'GET'
    , { timeout: 30000 }).catch((error) => ({ admissionResponseError: error }))
    const admissionTab = page.getByRole('tab', { name: '同步工单' }).first()
    await admissionTab.waitFor({ state: 'visible', timeout: 60000 })
    await admissionTab.click()
    await page.locator('.schedule-order-pool__admission-table').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    const admissionResponse = await admissionResponsePromise
    if (admissionResponse.admissionResponseError) {
      return {
        key: evidenceKey,
        label: '生产组长同步工单候选准入核验',
        roleKey: 'productionLeader',
        status: 'BLOCKED',
        category: 'E2E_AC_M01_PAGE',
        acceptanceIds,
        targetPath: pagePath,
        description: `真实页面打开“同步工单”页签后未捕获 admission-diff 响应：${admissionResponse.admissionResponseError.message}`
      }
    }
    assert.ok(admissionResponse.ok(), `同步工单页签 admission-diff HTTP 失败：${admissionResponse.status()}`)
    const admissionBody = await admissionResponse.json()
    assert.ok(isBusinessSuccess(admissionBody), `同步工单页签 admission-diff 业务失败：${responseMessage(admissionBody)}`)
  } catch (error) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_PAGE',
      acceptanceIds,
      targetPath: pagePath,
      description: `生产组长无法通过真实页面进入同步工单候选池：${error.message}`
    }
  }

  const admissionDiffEndpoint = '/admin-api/mes/pro/schedule-order/admission-diff'
  const targetQuery = {
    pageNo: 1,
    pageSize: 10,
    workOrderCode: config.productionOrderCode
  }
  const targetEndpoint = `${admissionDiffEndpoint}?${new URLSearchParams(
    Object.entries(targetQuery).map(([key, value]) => [key, String(value)])
  ).toString()}`
  const targetResult = await fetchWithPageAuth(page, targetEndpoint)
  if (!targetResult.ok || !isBusinessSuccess(targetResult.body)) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_DATA',
      acceptanceIds,
      workOrderCode: config.productionOrderCode,
      description: `按正式生产订单编号查询候选池失败，HTTP=${targetResult.status}，业务=${responseMessage(targetResult.body)}。`
    }
  }
  const targetRows = extractAdmissionDiffRows(targetResult.body)
  const targetRow = targetRows.find((row) =>
    Number(row.workOrderId) === Number(config.workOrderId)
      && String(row.workOrderCode || '') === String(config.productionOrderCode)
  )
  if (!targetRow) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_DATA',
      acceptanceIds,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      description: '候选池无法按正式生产订单编号返回任务专用工单，不能证明 ERP 已确认订单可按正式 ID/编号查询。'
    }
  }
  const targetIsAdmissible = targetRow.admissionStatus === 'READY_TO_ADMIT'
    ? targetRow.selectable === true
    : targetRow.admissionStatus === 'ALREADY_ADMITTED'
  if (!targetIsAdmissible) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_DATA',
      acceptanceIds,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      admissionStatus: targetRow.admissionStatus,
      selectable: targetRow.selectable,
      reasonCode: targetRow.reasonCode,
      description: '任务专用工单未处于 READY_TO_ADMIT 可选或 ALREADY_ADMITTED 已入池状态，不能证明已确认 ERP 订单进入候选。'
    }
  }

  const blockedQuery = {
    pageNo: 1,
    pageSize: 50,
    admissionStatus: 'BLOCKED'
  }
  const blockedEndpoint = `${admissionDiffEndpoint}?${new URLSearchParams(
    Object.entries(blockedQuery).map(([key, value]) => [key, String(value)])
  ).toString()}`
  const blockedResult = await fetchWithPageAuth(page, blockedEndpoint)
  if (!blockedResult.ok || !isBusinessSuccess(blockedResult.body)) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_DATA',
      acceptanceIds,
      description: `缺 ERP 正式订单阻断样本查询失败，HTTP=${blockedResult.status}，业务=${responseMessage(blockedResult.body)}。`
    }
  }
  const blockedFormalIdentityRows = extractAdmissionDiffRows(blockedResult.body)
    .filter((row) => row.reasonCode === 'BLOCKED_ERP_SYNC_RECORD_MISSING' && row.selectable === false)
  if (!blockedFormalIdentityRows.length) {
    return {
      key: evidenceKey,
      label: '生产组长同步工单候选准入核验',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_AC_M01_DATA',
      acceptanceIds,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      admissionStatus: targetRow.admissionStatus,
      selectable: targetRow.selectable,
      description: '当前真实候选池缺少 BLOCKED_ERP_SYNC_RECORD_MISSING 阻断样本，不能证明缺正式 ERP ID/编号的订单不会进入可选候选。'
    }
  }

  return {
    key: 'scheduleOrderErpCandidateAdmission',
    label: '生产组长同步工单候选准入核验',
    roleKey: 'productionLeader',
    status: 'PASS',
    acceptanceIds: ['AC-M01'],
    targetPath: pagePath,
    tableSelector: '.schedule-order-pool__admission-table',
    workOrderId: targetRow.workOrderId,
    workOrderCode: targetRow.workOrderCode,
    admissionStatus: targetRow.admissionStatus,
    selectable: targetRow.selectable,
    reasonCode: targetRow.reasonCode,
    blockedFormalIdentityRowCount: blockedFormalIdentityRows.length
  }
}

async function verifyActiveOrderConflictRouteFailure(page, config, joinEvidence) {
  const conflictRouteId = config.routeId + 1
  const section = page.locator('[data-team-leader-active-order-config]').first()
  await fillFormItemForAction(section, '加入活跃订单', '生产订单ID', config.workOrderId)
  await fillFormItemForAction(section, '加入活跃订单', '路线ID', conflictRouteId)
  await fillFormItemForAction(section, '加入活跃订单', '路线版本ID', config.routeVersionId)
  await fillFormItemForAction(section, '加入活跃订单', '调拨单ID列表', config.transferIds.join(','))
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

async function loadActiveOrderTransferTraceViaAuth(page, activeOrderId, label) {
  const endpoint = `/admin-api/mes/pro/process-pool/team-leader/active-order/transfer-trace?activeOrderId=${encodeURIComponent(activeOrderId)}`
  const result = await fetchWithPageAuth(page, endpoint)
  assert.ok(result.ok, `${label} HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `${label} 业务失败：${responseMessage(result.body)}`)
  assert.ok(Array.isArray(result.body.data), `${label} 必须返回数组。`)
  return result.body.data
}

function hasFormalTransferTraceSourceFields(row) {
  return Boolean(
    row?.sourceType
      && (row.sourceObjectCode || row.sourceObjectId)
      && row.sourceStatus
      && row.quantity !== undefined
      && row.quantity !== null
      && row.quantity !== ''
      && row.materialStockId
      && row.batchId
      && row.idempotencyKey
  )
}

async function verifyActiveOrderTransferTraceReadOnly(page, config, joinEvidence) {
  const acceptanceIds = ['AC-M02', 'AC-M05', 'AC-M07', 'AC-M08']
  const evidenceKey = 'activeOrderTransferTraceReadOnly'
  if (!joinEvidence?.activeOrderId) {
    failFast('活跃订单调拨追溯核验前缺少 joinActiveOrder 动作证据。', [{
      key: 'activeOrderTransferTracePrereq',
      category: 'E2E_TRANSFER_TRACE_DATA',
      description: '必须先通过生产组长真实页面加入活跃订单，再读取同一 activeOrderId 的调拨/发货/补料/退料追溯。'
    }])
  }

  const mutationRequests = []
  const trackMutationRequest = (request) => {
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) return
    if (request.url().includes('/mes/pro/process-pool/team-leader/active-order/')) {
      mutationRequests.push({ method: request.method(), url: request.url() })
    }
  }
  page.on('request', trackMutationRequest)
  try {
    const section = page.locator('[data-team-leader-active-order-transfer-trace]').first()
    await section.waitFor({ state: 'visible', timeout: 60000 })
    const visibleTraceForActiveOrder = await page
      .locator('[data-transfer-trace-active-order-id]')
      .filter({ hasText: String(joinEvidence.activeOrderId) })
      .first()
      .waitFor({ state: 'visible', timeout: 15000 })
      .then(() => true)
      .catch(() => false)

    const apiRows = await loadActiveOrderTransferTraceViaAuth(
      page,
      joinEvidence.activeOrderId,
      '活跃订单调拨库存追溯只读端点'
    )
    const traceRows = apiRows.filter((row) => Number(row.activeOrderId) === Number(joinEvidence.activeOrderId))
    if (!traceRows.length) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        '生产组长只读活跃订单调拨/库存追溯',
        'productionLeader',
        'E2E_TRANSFER_TRACE_DATA',
        acceptanceIds,
        '正式追溯端点未返回当前 activeOrderId 的调拨/发货/补料/退料/批次库存追溯行；需补齐正式来源数据或记录链路。',
        {
          activeOrderId: joinEvidence.activeOrderId,
          endpoint: '/mes/pro/process-pool/team-leader/active-order/transfer-trace',
          totalTraceRows: apiRows.length
        }
      )
    }

    const missingFormalFields = traceRows.filter((row) => !hasFormalTransferTraceSourceFields(row))
    if (missingFormalFields.length) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        '生产组长只读活跃订单调拨/库存追溯',
        'productionLeader',
        'E2E_TRANSFER_TRACE_DATA',
        acceptanceIds,
        '正式追溯行缺少 sourceType/sourceObjectCode/sourceStatus/quantity/materialStockId/batchId/idempotencyKey 等来源字段。',
        {
          activeOrderId: joinEvidence.activeOrderId,
          missingTraceIds: missingFormalFields.map((row) => row.id).filter(Boolean)
        }
      )
    }

    if (!visibleTraceForActiveOrder) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        '生产组长只读活跃订单调拨/库存追溯',
        'productionLeader',
        'E2E_TRANSFER_TRACE_PAGE',
        acceptanceIds,
        '正式追溯端点有数据，但班组长页面没有渲染同一 activeOrderId 的追溯行。',
        {
          activeOrderId: joinEvidence.activeOrderId,
          apiTraceCount: traceRows.length
        }
      )
    }

    const uiRows = await page.locator('[data-team-leader-active-order-transfer-trace]').evaluate((table) => {
      const readText = (selector) =>
        Array.from(table.querySelectorAll(selector)).map((node) => node.textContent?.replace(/\s+/g, ' ').trim() || '')
      return {
        activeOrderIds: readText('[data-transfer-trace-active-order-id]'),
        sourceType: readText('[data-transfer-trace-source-type]'),
        sourceObjectCode: readText('[data-transfer-trace-source-object-code]'),
        sourceStatus: readText('[data-transfer-trace-source-status]'),
        quantity: readText('[data-transfer-trace-quantity]'),
        materialStockId: readText('[data-transfer-trace-material-stock-id]'),
        batchId: readText('[data-transfer-trace-batch-id]'),
        idempotencyKey: readText('[data-transfer-trace-idempotency-key]')
      }
    })
    assert.equal(mutationRequests.length, 0, '活跃订单调拨追溯只读核验不得触发写请求。')
    const observedSourceTypes = [...new Set(traceRows.map((row) => row.sourceType).filter(Boolean))]
    assert.ok(
      observedSourceTypes.some((type) => ['TRANSFER', 'SHIPMENT', 'REPLENISHMENT', 'RETURN', 'BATCH_TRACE'].includes(type)),
      `活跃订单追溯来源类型不属于正式调拨/发货/补料/退料/批次库存集合：${observedSourceTypes.join(',')}`
    )

    return {
      key: evidenceKey,
      label: '生产组长只读活跃订单调拨/库存追溯',
      roleKey: 'productionLeader',
      status: 'PASS',
      category: 'E2E_TRANSFER_TRACE',
      acceptanceIds,
      activeOrderId: joinEvidence.activeOrderId,
      workOrderId: config.workOrderId,
      routeId: config.routeId,
      routeVersionId: config.routeVersionId,
      traceCount: traceRows.length,
      observedSourceTypes,
      sourceObjectCodes: [...new Set(traceRows.map((row) => row.sourceObjectCode || row.sourceObjectId).filter(Boolean))],
      sourceStatuses: [...new Set(traceRows.map((row) => row.sourceStatus).filter(Boolean))],
      materialStockIds: [...new Set(traceRows.map((row) => row.materialStockId).filter(Boolean))],
      batchIds: [...new Set(traceRows.map((row) => row.batchId).filter(Boolean))],
      idempotencyKeys: traceRows.map((row) => row.idempotencyKey).filter(Boolean),
      uiRows,
      mutationRequestCount: mutationRequests.length,
      endpoint: '/mes/pro/process-pool/team-leader/active-order/transfer-trace'
    }
  } finally {
    page.off('request', trackMutationRequest)
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

  const processResult = await loadPqcProcessesViaAuth(page, config, 'PQC 活跃订单工序只读列表', {
    key: 'activeOrderCrossRoleReadOnly',
    label: 'PQC 检验员通过真实页面只读同一 activeOrderId',
    roleKey: 'pqcInspector',
    acceptanceIds: ['AC-M04', 'AC-D13', 'AC-D24'],
    activeOrderId: joinEvidence.activeOrderId
  })
  if (!processResult.ok) {
    return processResult.blocker
  }
  const processes = processResult.data
  assert.ok(Array.isArray(processes), 'PQC 活跃订单工序只读列表必须返回数组。')
  const matchingActiveOrder = processes.some((process) => Number(process.activeOrderId) === Number(joinEvidence.activeOrderId))
  if (!matchingActiveOrder) {
    return buildPqcProcessSourceBlocker({
      key: 'activeOrderCrossRoleReadOnly',
      label: 'PQC 检验员通过真实页面只读同一 activeOrderId',
      roleKey: 'pqcInspector',
      acceptanceIds: ['AC-M04', 'AC-D13', 'AC-D24'],
      config,
      activeOrderId: joinEvidence.activeOrderId,
      processCount: processes.length,
      description: 'PQC 活跃订单工序只读列表未返回与生产组长加入结果一致的 activeOrderId；需补齐该 activeOrder 下待执行 PQC 任务或修复正式任务源映射。'
    })
  }

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

async function loadPqcProcessesViaAuth(page, config, label, structured) {
  const endpoint = `/admin-api/mes/pro/feedback/frontline/device-account/pqc/active-order/processes?workOrderId=${encodeURIComponent(config.workOrderId)}&routeId=${encodeURIComponent(config.routeId)}`
  const result = await fetchWithPageAuth(page, endpoint)
  if (!result.ok || !isBusinessSuccess(result.body) || !Array.isArray(result.body?.data)) {
    if (structured) {
      return {
        ok: false,
        blocker: buildPqcProcessSourceBlocker({
          ...structured,
          config,
          responseStatus: result.status,
          responseCode: result.body?.code,
          responseMessage: responseMessage(result.body),
          description: `${label} 未返回可用于真实 E2E 的待执行 PQC 工序任务，HTTP=${result.status}，业务=${responseMessage(result.body)}；需补齐正式 PQC task/source event 数据后才能证明该动作。`
        })
      }
    }
    throw new Error(`${label} 失败，HTTP=${result.status}，业务=${responseMessage(result.body)}`)
  }
  if (structured) {
    return {
      ok: true,
      data: result.body.data
    }
  }
  return result.body.data
}

function buildPqcProcessSourceBlocker({
  key,
  label,
  roleKey,
  acceptanceIds,
  config,
  activeOrderId,
  responseStatus,
  responseCode,
  responseMessage: businessMessage,
  processCount,
  description
}) {
  return {
    key,
    label,
    roleKey,
    status: 'BLOCKED',
    category: 'E2E_PQC_TASK_SOURCE',
    acceptanceIds,
    activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    responseStatus,
    responseCode,
    responseMessage: businessMessage,
    processCount,
    description
  }
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
  const visibleMetaTexts = (await page.locator('[data-pqc-inspection-meta]').allTextContents())
    .map((text) => text.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
  assert.ok(visibleMetaTexts.length > 0, 'PQC 页面必须可见渲染检验方法、标准和判定元信息。')
  const visibleMetaText = visibleMetaTexts.join('\n')
  const visibleStandardTexts = (await page.locator('[data-pqc-standard-button]').allTextContents())
    .map((text) => text.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
  const visibleMethodTexts = (await page.locator('[data-pqc-method-button]').allTextContents())
    .map((text) => text.replace(/\s+/g, ' ').trim())
    .filter(Boolean)
  assert.ok(visibleStandardTexts.length > 0, 'PQC 页面必须可见渲染 QA 规程接收标准。')
  assert.ok(visibleMethodTexts.length > 0, 'PQC 页面必须可见渲染 QA 规程检验方法。')
  const visibleStandardText = visibleStandardTexts.join('\n')
  const visibleMethodText = visibleMethodTexts.join('\n')
  const formatResultTypeLabel = (resultType) => {
    const normalized = String(resultType || '').trim().toUpperCase()
    if (normalized === 'NUMBER' || normalized === 'NUMERIC') return '数值'
    if (normalized === 'BOOLEAN' || normalized === 'CHOICE' || normalized === 'PASS_FAIL') return '合格/不合格'
    return String(resultType || '').trim()
  }
  const visibleFormalItem = matchingProcesses
    .flatMap((process) => process.inspectionItems.map((item) => ({ process, item })))
    .find(({ item }) => {
      const method = String(item.inspectionMethod || '').trim()
      const standard = String(item.standardText || '').trim()
      const resultTypeLabel = formatResultTypeLabel(item.resultType)
      return method
        && standard
        && resultTypeLabel
        && visibleMethodText.includes(method)
        && visibleStandardText.includes(standard)
        && visibleMetaText.includes(resultTypeLabel)
    })
  assert.ok(
    visibleFormalItem,
    'PQC 页面可见元信息必须至少匹配一条正式 QA 规程项目的方法、标准和判定类型。'
  )

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
    plannedQuantities,
    visibleMetadataCount: visibleMetaTexts.length,
    sampleInspectionMethod: visibleFormalItem.item.inspectionMethod,
    sampleStandardText: visibleFormalItem.item.standardText,
    sampleResultType: visibleFormalItem.item.resultType,
    sampleResultTypeLabel: formatResultTypeLabel(visibleFormalItem.item.resultType)
  }
}

async function waitForPqcInspectionQuantityHydrated(page, quantityInput, plannedQuantities, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs
  let rawValue = ''
  let uiQuantity = 0
  while (Date.now() < deadline) {
    rawValue = String(await quantityInput.inputValue().catch(() => ''))
    uiQuantity = Number(rawValue)
    if (plannedQuantities.includes(uiQuantity)) {
      return {
        status: 'PASS',
        rawValue,
        uiQuantity
      }
    }
    await page.waitForTimeout(250)
  }
  return {
    status: 'BLOCKED',
    rawValue,
    uiQuantity,
    timeoutMs
  }
}

async function verifyQaRegulationPublishedVersionReadOnly(page, config) {
  const initialQaSectionEvidence = await page.waitForSelector('[data-qa-regulation-section]', {
    state: 'attached',
    timeout: 5000
  }).then(() => ({
    status: 'ATTACHED'
  })).catch((error) => ({
    status: 'MISSING',
    message: error.message
  }))
  const qaRegulationApiPromise = page.waitForResponse((response) =>
    response.url().includes('inspection-regulation/published-version')
      && response.request().method() === 'GET'
  , { timeout: 20000 }).then(async (response) => {
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { jsonParseError: error.message }
    }
    return {
      apiStatus: response.status(),
      apiCode: body?.code,
      apiMessage: body?.msg || body?.message || body?.jsonParseError || '',
      hasData: Boolean(body?.data),
      publishedVersionId: body?.data?.publishedVersionId,
      firstInspectionRuleCount: body?.data?.firstInspectionRules?.length || 0,
      patrolInspectionRuleCount: body?.data?.patrolInspectionRules?.length || 0,
      finalInspectionRuleCount: body?.data?.finalInspectionRules?.length || 0,
      batchRecordBindingSummary: body?.data?.batchRecordBindingSummary || ''
    }
  }).catch((error) => ({
    apiStatus: null,
    apiCode: null,
    apiMessage: error.message,
    hasData: false
  }))
  await page.reload({ waitUntil: 'domcontentloaded', timeout: 90000 }).catch(() => undefined)
  const refreshedQaSectionEvidence = await page.waitForSelector('[data-qa-regulation-section]', {
    state: 'attached',
    timeout: 15000
  }).then(() => ({
    status: 'ATTACHED'
  })).catch((error) => ({
    status: 'MISSING',
    message: error.message
  }))
  const qaRegulationApiEvidence = await qaRegulationApiPromise
  const formalSelectorEvidence = []
  const selectorChecks = [
    ['publishedVersion', '[data-qa-regulation-published-version]'],
    ['routeVersion', '[data-qa-regulation-route-version]'],
    ['routeProcess', '[data-qa-regulation-route-process]'],
    ['firstInspectionRule', '[data-qa-regulation-first-inspection-rule]'],
    ['patrolInspectionRule', '[data-qa-regulation-patrol-inspection-rule]'],
    ['finalInspectionRule', '[data-qa-regulation-final-inspection-rule]'],
    ['batchRecordBinding', '[data-qa-regulation-batch-record-binding]'],
    ['immutability', '[data-qa-regulation-version-immutable]']
  ]
  for (const [key, selector] of selectorChecks) {
    const locator = page.locator(selector).first()
    const visible = await locator.isVisible({ timeout: 1000 }).catch(() => false)
    if (visible) {
      formalSelectorEvidence.push({ key, selector })
    }
  }

  const pageText = (await page.locator('#app').innerText({ timeout: 5000 }).catch(() => ''))
    .replace(/\s+/g, ' ')
    .trim()
  const visibleFormalTerms = ['产品', '路线', '版本', '工序', 'SOP', '首检', '巡检', '末检', '批记录', '发布']
    .filter((term) => pageText.includes(term))
  const acceptanceIds = ['AC-M09', 'AC-D15', 'AC-D16', 'AC-D17', 'AC-D18', 'AC-D19', 'AC-D20', 'AC-D21', 'AC-D22', 'AC-D23']
  const label = 'QA 规程发布版本维护页面证据'
  const baseEvidence = {
    key: 'qaRegulationPublishedVersionReadOnly',
    label,
    roleKey: 'qa',
    category: 'E2E_QA_REGULATION_PAGE',
    acceptanceIds,
    targetPath: '/mes/qc/template',
    currentUrl: page.url(),
    configuredVersionId: config.qaRegulationVersionId,
    initialQaSectionEvidence,
    refreshedQaSectionEvidence,
    qaRegulationApiEvidence,
    formalSelectorEvidence,
    visibleFormalTerms
  }
  const hasFormalLifecycleSurface = formalSelectorEvidence.length >= 6
    && ['产品', '路线', '版本', '工序', '首检', '巡检', '发布'].every((term) => visibleFormalTerms.includes(term))

  if (hasFormalLifecycleSurface) {
    return {
      ...baseEvidence,
      status: 'PASS',
      description: 'QA 规程入口已在真实页面展示正式发布版本、路线版本、工序规则和发布完整性证据。'
    }
  }

  return {
    ...baseEvidence,
    status: 'BLOCKED',
    description: '当前 QA 入口只证明页面 shell 可加载，未在真实页面观察到正式 QA 检验规程发布版本、产品/路线版本/工序、首检/巡检/末检完整性、逐工序批记录绑定和发布不可变证据；不能用旧质检方案页替代 BDD-07。'
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

  const requestBudgetTracker = createPqcPieceDetailRequestBudgetTracker(page)
  try {
    await page.locator('[data-frontline-pqc-inspection-content]').first().waitFor({
      state: 'visible',
      timeout: 30000
    })
    const quantityInput = page.locator('#frontlinePqcInspectionQuantity').first()
    await quantityInput.waitFor({ state: 'visible', timeout: 30000 })
    const quantityResolution = await waitForPqcInspectionQuantityHydrated(page, quantityInput, plannedQuantities)
    if (quantityResolution.status !== 'PASS') {
      return {
        key: 'pqcPieceDetailQuantityPrepared',
        label: 'PQC 逐件明细数量按计划数量准备',
        roleKey: 'pqcInspector',
        status: 'BLOCKED',
        category: 'E2E_PQC_PIECE_DETAIL',
        acceptanceIds: ['AC-D27'],
        activeOrderId: joinEvidence.activeOrderId,
        plannedQuantities,
        uiQuantity: quantityResolution.uiQuantity,
        rawQuantityValue: quantityResolution.rawValue,
        hydrationTimeoutMs: quantityResolution.timeoutMs,
        requestBudget: requestBudgetTracker.snapshot(),
        description: `PQC 页面检验数量未在 ${quantityResolution.timeoutMs}ms 内水合为正式 task plannedInspectionQuantity：${plannedQuantities.join(', ')}。`
      }
    }
    const uiQuantity = quantityResolution.uiQuantity

    const firstEntry = page.locator('[data-pqc-piece-open-button]').first()
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
        requestBudget: requestBudgetTracker.snapshot(),
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
    const firstEntryFilledCount = await fillVisiblePqcPieceModalValues(modal)
    await modal.getByRole('button', { name: '完成' }).click()
    await modal.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
    const completionEvidence = await completePqcPieceDetailsForSubmission(page, firstEntryFilledCount)
    const requestBudget = requestBudgetTracker.snapshot()
    assert.equal(requestBudget.pieceDetailRequests, 0, 'PQC 逐件弹窗打开和填写不得触发逐项明细 GET 请求。')
    assert.equal(requestBudget.processSnapshotRequests, 0, 'PQC 逐件弹窗填写不得重新加载活跃订单工序快照。')

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
      completedPieceValueCount: completionEvidence.completedPieceValueCount,
      completedChoiceItemCount: completionEvidence.completedChoiceItemCount,
      completedNumericItemCount: completionEvidence.completedNumericItemCount,
      inspectionEntry: entryLabel,
      requestBudget,
      sourceActionKey: regulationEvidence.key
    }
  } finally {
    requestBudgetTracker.stop()
  }
}

async function fillVisiblePqcPieceModalValues(modal) {
  const inputs = modal.locator('.frontline-pqc-piece-row input')
  const inputCount = await inputs.count()
  for (let index = 0; index < inputCount; index += 1) {
    const input = inputs.nth(index)
    const currentValue = (await input.inputValue()).trim()
    if (!currentValue) {
      await input.fill('1')
    }
  }
  const passButtons = modal.locator('.frontline-pqc-piece-row button', { hasText: '合格' })
  const passButtonCount = await passButtons.count()
  for (let index = 0; index < passButtonCount; index += 1) {
    await passButtons.nth(index).click()
  }
  return inputCount + passButtonCount
}

async function completePqcPieceDetailsForSubmission(page, alreadyCompletedCount = 0) {
  let completedPieceValueCount = alreadyCompletedCount
  let completedChoiceItemCount = 0
  let completedNumericItemCount = 0
  const tabs = page.locator('[data-pqc-inspection-tab]')
  const tabCount = await tabs.count()
  for (let index = 0; index < tabCount; index += 1) {
    await tabs.nth(index).click()
    const activePanel = page.locator('[data-pqc-active-inspection-panel]').first()
    await activePanel.waitFor({ state: 'visible', timeout: 30000 })
    const passButton = activePanel.getByRole('button', { name: /^全部合格$/ }).first()
    if (await passButton.count()) {
      await passButton.click()
      completedChoiceItemCount += 1
    }
    const pieceButton = activePanel.locator('[data-pqc-piece-open-button]').first()
    await pieceButton.waitFor({ state: 'visible', timeout: 30000 })
    await pieceButton.click()
    const modal = page.locator('[data-pqc-piece-modal]').first()
    await modal.waitFor({ state: 'visible', timeout: 30000 })
    completedPieceValueCount += await fillVisiblePqcPieceModalValues(modal)
    await modal.getByRole('button', { name: '完成' }).click()
    await modal.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})
    completedNumericItemCount += 1
  }
  return {
    completedPieceValueCount,
    completedChoiceItemCount,
    completedNumericItemCount
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

async function resolveRoleUserId(page, config, roleKey) {
  const role = config.roles?.[roleKey]
  const browser = page.context().browser()
  if (!role || !browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL_REVIEWER',
      description: `无法解析 ${roleKey} 用户上下文，不能排除后续复核人自我确认。`
    }
  }
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const rolePage = await context.newPage()
  try {
    await login(rolePage, config, roleKey, role)
    const permissionInfo = await getCurrentPermissionInfo(rolePage)
    const userId = resolveCurrentUserId(permissionInfo)
    if (!userId) {
      return {
        status: 'BLOCKED',
        category: 'E2E_PQC_PERSONNEL_REVIEWER',
        description: `${roleKey} 权限信息未返回可解析 userId，不能排除后续复核人自我确认。`
      }
    }
    return { status: 'PASS', userId }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL_REVIEWER',
      description: `解析 ${roleKey} 用户 ID 失败：${error.message}`
    }
  } finally {
    await context.close()
  }
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

function pqcEmployeeCardLocator(page) {
  return page
    .locator('.frontline-operator-top.is-pqc .frontline-top-card')
    .filter({ hasText: '员工' })
    .first()
}

async function isPqcEmployeeCardAlreadySelected(employeeCard, targetLabel) {
  const text = await employeeCard.innerText().catch(() => '')
  return text.includes(targetLabel)
}

async function clickPqcEmployeeOptionAndWaitForSwitch(page, employeeCard, targetLabel) {
  let lastClickError
  let lastResponseError
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    try {
      await employeeCard.waitFor({ state: 'visible', timeout: 30000 })
      const pickerVisible = await page.locator('.frontline-picker').first().isVisible().catch(() => false)
      if (!pickerVisible) {
        await employeeCard.click()
      }
      const targetOption = page
        .locator('.frontline-picker__options button')
        .filter({ hasText: targetLabel })
        .first()
      await targetOption.waitFor({ state: 'visible', timeout: 30000 })
      const switchResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/mes/pro/feedback/frontline/device-account/pqc/switch-employee')
          && response.request().method() === 'POST'
      , { timeout: 30000 }).catch((error) => ({ pqcSwitchEmployeeResponseError: error }))
      const clickError = await targetOption.click({ timeout: 10000 }).then(() => null).catch((error) => error)
      if (!clickError) {
        const switchResponse = await switchResponsePromise
        if (switchResponse.pqcSwitchEmployeeResponseError) {
          lastResponseError = switchResponse.pqcSwitchEmployeeResponseError
          return { pqcSwitchEmployeeResponseError: lastResponseError }
        }
        return { switchResponse }
      }
      lastClickError = clickError
    } catch (error) {
      lastClickError = error
    }
    await page.waitForTimeout(750)
  }
  return {
    pqcSwitchEmployeeResponseError: lastClickError || lastResponseError || new Error('PQC employee option click did not complete.')
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
  const reviewerResolution = await resolveRoleUserId(page, config, 'pqcLeader')
  if (reviewerResolution.status !== 'PASS') {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: reviewerResolution.category,
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      loginUserId,
      candidateCount: selectableCandidates.length,
      description: reviewerResolution.description
    }
  }
  const reviewerUserId = reviewerResolution.userId
  const excludedReviewerUserIds = [reviewerUserId]
  const reviewSafeCandidates = selectableCandidates.filter((candidate) =>
    !excludedReviewerUserIds.includes(candidate.userId))
  if (!reviewSafeCandidates.length) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL_REVIEWER',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      loginUserId,
      reviewerUserId,
      candidateCount: selectableCandidates.length,
      excludedReviewerUserIds,
      description: 'PQC 人员范围只返回后续 PQC 组长复核人，不能创建合法的非自我复核提交。'
    }
  }
  const nonLoginCandidate = loginUserId
    ? reviewSafeCandidates.find((candidate) => candidate.userId !== loginUserId)
    : reviewSafeCandidates[0]
  const targetCandidate = nonLoginCandidate || reviewSafeCandidates[0]
  const nonLoginCandidateAvailable = Boolean(nonLoginCandidate)

  const employeeCard = pqcEmployeeCardLocator(page)
  const switchResult = await clickPqcEmployeeOptionAndWaitForSwitch(page, employeeCard, targetCandidate.label)
  const switchResponse = switchResult.switchResponse
  if (switchResult.pqcSwitchEmployeeResponseError) {
    return {
      key: 'pqcActualEmployeeSelected',
      label: '共享账号下选择实际 PQC 检验人员',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds: ['AC-D25', 'AC-D31'],
      activeOrderId: joinEvidence.activeOrderId,
      actualEmployeeId: targetCandidate.userId,
      description: `页面点击实际 PQC 人员后未捕获 switch-employee 响应：${switchResult.pqcSwitchEmployeeResponseError.message}`
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
  if (loginUserId && nonLoginCandidateAvailable) {
    assert.notEqual(
      Number(switchBody.data?.actualEmployeeId),
      loginUserId,
      '共享账号 PQC 实际检验人不能默认等于当前登录用户。'
    )
  }
  assert.notEqual(
    Number(switchBody.data?.actualEmployeeId),
    reviewerUserId,
    'PQC 实际检验人不能等于后续 PQC 组长复核人。'
  )
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
    reviewerUserId,
    actualEmployeeId: targetCandidate.userId,
    candidateCount: selectableCandidates.length,
    nonLoginCandidateAvailable,
    excludedReviewerUserIds,
    employeeLabel: targetCandidate.label,
    templateNo: switchBody.data?.template?.templateNo,
    sourceActionKey: readOnlyEvidence.key
  }
}

async function switchPqcActualEmployeeToUser(page, config, actionEvidence, targetUserId, evidenceKey, label, acceptanceIds) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const readOnlyEvidence = actionEvidence.find((item) => item.key === 'activeOrderCrossRoleReadOnly' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId || !readOnlyEvidence) {
    return {
      key: evidenceKey,
      label,
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds,
      description: 'PQC 实际检验人定向选择前缺少同一 activeOrderId 的页面只读证据。'
    }
  }
  const personnelResult = await loadPqcPersonnelViaAuth(page, 'PQC 人员范围')
  if (personnelResult.blocked) {
    return {
      key: evidenceKey,
      label,
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds,
      activeOrderId: joinEvidence.activeOrderId,
      targetUserId,
      responseCode: personnelResult.responseCode,
      responseMessage: personnelResult.responseMessage,
      description: personnelResult.description
    }
  }
  const selectableCandidates = personnelResult.candidates
    .map((candidate) => ({
      ...candidate,
      userId: Number(candidate.userId),
      label: formatPqcPersonnelLabel(candidate)
    }))
    .filter((candidate) => Number.isFinite(candidate.userId) && candidate.userId > 0 && candidate.label)
  const targetCandidate = selectableCandidates.find((candidate) => candidate.userId === Number(targetUserId))
  if (!targetCandidate) {
    return {
      key: evidenceKey,
      label,
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_PERSONNEL',
      acceptanceIds,
      activeOrderId: joinEvidence.activeOrderId,
      targetUserId,
      candidateUserIds: selectableCandidates.map((candidate) => candidate.userId),
      description: 'PQC 人员范围没有返回目标实际检验人，不能通过真实页面准备该候选。'
    }
  }

  const employeeCard = pqcEmployeeCardLocator(page)
  await employeeCard.waitFor({ state: 'visible', timeout: 30000 })
  const alreadySelected = await isPqcEmployeeCardAlreadySelected(employeeCard, targetCandidate.label)
  let switchBody = { data: { actualEmployeeId: Number(targetUserId) } }
  if (!alreadySelected) {
    const switchResult = await clickPqcEmployeeOptionAndWaitForSwitch(page, employeeCard, targetCandidate.label)
    const switchResponse = switchResult.switchResponse
    if (switchResult.pqcSwitchEmployeeResponseError) {
      return {
        key: evidenceKey,
        label,
        roleKey: 'pqcInspector',
        status: 'BLOCKED',
        category: 'E2E_PQC_PERSONNEL',
        acceptanceIds,
        activeOrderId: joinEvidence.activeOrderId,
        targetUserId,
        description: `页面点击目标 PQC 人员后未捕获 switch-employee 响应：${switchResult.pqcSwitchEmployeeResponseError.message}`
      }
    }
    assert.ok(switchResponse.ok(), `PQC 目标人员切换 HTTP 失败：${switchResponse.status()}`)
    switchBody = await switchResponse.json()
    assert.equal(switchBody.code, 0, `PQC 目标人员切换业务失败：${responseMessage(switchBody)}`)
    assert.equal(Number(switchBody.data?.actualEmployeeId), Number(targetUserId), 'PQC 目标人员切换响应必须保存指定 actualEmployeeId。')
  }
  await employeeCard.filter({ hasText: targetCandidate.label }).waitFor({ state: 'visible', timeout: 30000 })

  return {
    key: evidenceKey,
    label,
    roleKey: 'pqcInspector',
    status: 'PASS',
    category: 'E2E_PQC_PERSONNEL',
    acceptanceIds,
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    actualEmployeeId: Number(targetUserId),
    employeeLabel: targetCandidate.label,
    candidateCount: selectableCandidates.length,
    templateNo: switchBody.data?.template?.templateNo,
    sourceActionKey: readOnlyEvidence.key
  }
}

function requireSignatureId(config, roleKey) {
  const signatureId = Number(config.signatureIds?.[roleKey])
  assert.ok(
    Number.isFinite(signatureId) && signatureId > 0,
    `${roleKey} 必须在 RRM_SIGNATURE_IDS_JSON 中配置大于 0 的正式电子签名 ID。`
  )
  return signatureId
}

function isPqcSignaturePoolRole(roleKey, preferredRoleKey) {
  return roleKey === preferredRoleKey || /^pqcExtra\d+$/.test(roleKey)
}

function collectConfiguredSignatureIds(config, preferredRoleKey) {
  const candidates = []
  const pushCandidate = (value) => {
    const signatureId = Number(value)
    if (Number.isFinite(signatureId) && signatureId > 0 && !candidates.includes(signatureId)) {
      candidates.push(signatureId)
    }
  }
  pushCandidate(config.signatureIds?.[preferredRoleKey])
  for (const [roleKey, signatureId] of Object.entries(config.signatureIds || {})) {
    if (roleKey !== '__parseError'
      && roleKey !== preferredRoleKey
      && isPqcSignaturePoolRole(roleKey, preferredRoleKey)) {
      pushCandidate(signatureId)
    }
  }
  assert.ok(candidates.length > 0, 'RRM_SIGNATURE_IDS_JSON 必须提供至少一个正式电子签名 ID。')
  return candidates
}

async function resolveUnusedPqcSignatureId(page, config, preferredRoleKey) {
  const candidateSignatureIds = collectConfiguredSignatureIds(config, preferredRoleKey)
  const browser = page.context().browser()
  if (!browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SIGNATURE_POOL',
      candidateSignatureIds,
      description: '无法创建 PQC 组长只读上下文来核对已占用电子签名 ID。'
    }
  }

  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const leaderPage = await context.newPage()
  const usedSignatureIds = new Set()
  let submissionTotal = 0
  try {
    await login(leaderPage, config, 'pqcLeader', config.roles.pqcLeader)
    const pageSize = 100
    const submitDate = localDateString()
    for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
      const submissionPage = await loadPqcLeaderSubmissionPage(leaderPage, { pageNo, pageSize, submitDate })
      submissionTotal = Math.max(submissionTotal, submissionPage.total)
      for (const row of submissionPage.list) {
        const signatureId = Number(row.electronicSignatureId ?? row.signatureId)
        if (Number.isFinite(signatureId) && signatureId > 0) {
          usedSignatureIds.add(signatureId)
        }
      }
      if (submissionPage.list.length === 0 || pageNo * pageSize >= submissionPage.total) {
        break
      }
    }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SIGNATURE_POOL',
      candidateSignatureIds,
      description: `无法读取 PQC 组长提交看板中的已用签名 ID：${error.message}`
    }
  } finally {
    await context.close()
  }

  const signatureId = candidateSignatureIds.find((candidate) => !usedSignatureIds.has(candidate))
  if (!signatureId) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SIGNATURE_POOL',
      candidateSignatureIds,
      usedSignatureIds: [...usedSignatureIds],
      submissionTotal,
      description: '用户提供的电子签名 ID 均已被工序池事件占用；需要补充新的正式签名 ID 后再执行 PQC 正式提交。'
    }
  }
  return {
    status: 'PASS',
    signatureId,
    candidateSignatureIds,
    usedSignatureIds: [...usedSignatureIds],
    submissionTotal
  }
}

function buildPqcFormalSubmissionBlocker(category, description, extra = {}) {
  return buildStructuredBlockerEvidence(
    'pqcFormalSubmissionCreated',
    'PQC 正式提交生成过程池检验事件',
    'pqcInspector',
    category,
    ['AC-D29', 'AC-D32'],
    description,
    extra
  )
}

function requirePositiveFormalId(value, label) {
  const numberValue = Number(value)
  assert.ok(Number.isFinite(numberValue) && numberValue > 0, `${label} 必须是大于 0 的正式 ID。`)
  return numberValue
}

function appendQueryValue(query, key, value) {
  if (value === undefined || value === null || value === '') {
    return
  }
  query.set(key, String(value))
}

function buildProductionFillUrl(config, context) {
  const query = new URLSearchParams()
  appendQueryValue(query, 'workOrderId', config.workOrderId)
  appendQueryValue(query, 'productionOrderCode', config.productionOrderCode)
  appendQueryValue(query, 'routeId', config.routeId)
  appendQueryValue(query, 'routeProcessId', context.routeProcessId)
  appendQueryValue(query, 'processId', context.processId)
  appendQueryValue(query, 'taskId', context.taskId)
  appendQueryValue(query, 'itemId', context.itemId)
  appendQueryValue(query, 'feedbackCode', context.feedbackCode)
  appendQueryValue(query, 'feedbackType', context.feedbackType)
  appendQueryValue(query, 'approveUserId', context.approveUserId)
  appendQueryValue(query, 'recordbookId', context.recordbookId)
  appendQueryValue(query, 'workstationId', context.workstationId)
  appendQueryValue(query, 'deviceId', context.deviceId)
  appendQueryValue(query, 'signatureId', context.signatureId)
  appendQueryValue(query, 'signatureEmployeeId', context.signatureEmployeeId)
  appendQueryValue(query, 'actualEmployeeId', context.actualEmployeeId)
  appendQueryValue(query, 'outputQuantity', context.outputQuantity)
  appendQueryValue(query, 'idempotencyKey', context.idempotencyKey)
  appendQueryValue(query, 'processPoolSubmissionIdempotencyKey', context.processPoolSubmissionIdempotencyKey)
  return `${config.frontendUrl}${PRODUCTION_FILL_ROUTE}?${query.toString()}`
}

function buildPqcFillUrl(config, context, employeeEvidence, signatureId) {
  const query = new URLSearchParams()
  appendQueryValue(query, 'workOrderId', config.workOrderId)
  appendQueryValue(query, 'workOrderCode', config.productionOrderCode)
  appendQueryValue(query, 'routeId', config.routeId)
  appendQueryValue(query, 'productionSubmitEventId', context.processPoolEventId)
  appendQueryValue(query, 'processPoolEventId', context.processPoolEventId)
  appendQueryValue(query, 'actualEmployeeId', employeeEvidence?.actualEmployeeId)
  appendQueryValue(query, 'signatureId', signatureId)
  appendQueryValue(query, 'regulationVersionId', config.qaRegulationVersionId)
  appendQueryValue(query, 'pqcSubmissionIdempotencyKey', `rrm-pqc-${context.processPoolEventId}-${Date.now()}`)
  return `${config.frontendUrl}${PQC_FILL_ROUTE}?${query.toString()}`
}

async function fetchPqcPrereqData(page, endpoint, label) {
  const result = await fetchWithPageAuth(page, endpoint)
  if (!result.ok || !isBusinessSuccess(result.body)) {
    return {
      status: 'BLOCKED',
      responseStatus: result.status,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body),
      description: `${label} 失败，HTTP=${result.status}，业务=${responseMessage(result.body)}。`
    }
  }
  return {
    status: 'PASS',
    data: result.body.data
  }
}

function normalizePageList(data) {
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  if (Array.isArray(data)) return data
  return []
}

async function loadProductionProcessForPqcPrereq(page, config) {
  const result = await fetchPqcPrereqData(
    page,
    '/admin-api/mes/pro/feedback/frontline/device-account/processes',
    '生产填写设备账号工序列表'
  )
  if (result.status !== 'PASS') return result
  const processes = Array.isArray(result.data) ? result.data : []
  const process = processes.find((item) =>
    Number(item.routeId) === Number(config.routeId)
      && Number(item.routeProcessId) === Number(config.primaryRouteProcessId)
  )
  if (!process) {
    return {
      status: 'BLOCKED',
      processCount: processes.length,
      routeProcessIds: processes.map((item) => Number(item.routeProcessId)).filter((value) => Number.isFinite(value)),
      description: '生产填写设备账号工序列表没有返回 RRM_ROUTE_PROCESS_ID_1 对应的正式主工序，不能生成与当前路线一致的生产提交事件。'
    }
  }
  return { status: 'PASS', process, processCount: processes.length }
}

async function loadRuntimeConfigForPqcPrereq(page, process) {
  const query = toQueryString({
    routeId: process.routeId,
    routeProcessId: process.routeProcessId,
    processId: process.processId
  })
  const result = await fetchPqcPrereqData(
    page,
    `/admin-api/mes/pro/feedback/frontline/device-account/runtime-config?${query}`,
    '生产填写运行态配置'
  )
  if (result.status !== 'PASS') return result
  return { status: 'PASS', runtimeConfig: result.data || {} }
}

async function loadProductionEmployeeForPqcPrereq(page, process) {
  const query = toQueryString({
    routeId: process.routeId,
    routeProcessId: process.routeProcessId,
    processId: process.processId
  })
  const result = await fetchPqcPrereqData(
    page,
    `/admin-api/mes/pro/feedback/frontline/device-account/employee-candidates?${query}`,
    '生产填写员工候选'
  )
  if (result.status !== 'PASS') return result
  const employees = Array.isArray(result.data) ? result.data : []
  const employee = employees.find((item) => Number(item.userId) > 0)
  if (!employee) {
    return {
      status: 'BLOCKED',
      employeeCount: employees.length,
      description: '生产填写员工候选没有返回可用于真实提交的正式员工 userId。'
    }
  }
  return { status: 'PASS', employee, employeeCount: employees.length }
}

async function loadProductionTaskForPqcPrereq(page, config, process) {
  const query = toQueryString({
    pageNo: 1,
    pageSize: 50,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    processId: process.processId
  })
  const result = await fetchPqcPrereqData(
    page,
    `/admin-api/mes/pro/task/page?${query}`,
    '生产任务分页'
  )
  if (result.status !== 'PASS') return result
  const tasks = normalizePageList(result.data)
  const task = tasks.find((item) =>
    Number(item.workOrderId) === Number(config.workOrderId)
      && Number(item.routeId) === Number(config.routeId)
      && Number(item.processId) === Number(process.processId)
  )
  if (!task) {
    return {
      status: 'BLOCKED',
      taskCount: tasks.length,
      description: '生产任务分页没有返回当前工单、路线和主工序对应的正式生产任务，不能构造生产填写提交上下文。'
    }
  }
  return { status: 'PASS', task, taskCount: tasks.length }
}

async function loadProductionRecordbookForPqcPrereq(page, config) {
  const query = toQueryString({
    pageNo: 1,
    pageSize: 20,
    businessObjectCode: config.productionOrderCode,
    recordbookType: 'PRODUCTION',
    status: 'OPEN'
  })
  const result = await fetchPqcPrereqData(
    page,
    `/admin-api/mes/pro/edhr-recordbook/page?${query}`,
    '生产记录本分页'
  )
  if (result.status !== 'PASS') return result
  const recordbooks = normalizePageList(result.data)
  const recordbook = recordbooks.find((item) =>
    Number(item.id) > 0
      && String(item.businessObjectCode || '') === String(config.productionOrderCode)
      && String(item.recordbookType || '') === 'PRODUCTION'
      && String(item.status || '') === 'OPEN'
  )
  if (!recordbook) {
    return {
      status: 'BLOCKED',
      recordbookCount: recordbooks.length,
      description: '生产记录本分页没有返回当前生产订单的 OPEN/PRODUCTION 正式记录本，不能生成生产提交 source event。'
    }
  }
  return { status: 'PASS', recordbook, recordbookCount: recordbooks.length }
}

function resolveProductionOutputQuantity(task) {
  const taskQuantity = Number(task.quantity)
  assert.ok(Number.isFinite(taskQuantity) && taskQuantity > 0, '生产任务 quantity 必须大于 0，才能生成生产提交 source event。')
  return taskQuantity >= 1 ? 1 : taskQuantity
}

function resolveProductionDeviceId(process, runtimeConfig) {
  const devices = Array.isArray(runtimeConfig.devices) ? runtimeConfig.devices : []
  const configuredDevice = devices.find((device) => Number(device.deviceId) > 0)
  return requirePositiveFormalId(configuredDevice?.deviceId || process.deviceId, '生产填写设备')
}

function extractProcessPoolEventIdFromFrontlineResponse(body) {
  const data = body?.data || body
  return requirePositiveFormalId(data?.processPoolEventId, '一线生产提交响应 processPoolEventId')
}

async function readFrontlineProductionSubmitState(page, submitButton) {
  const [disabled, topCards, visibleErrors] = await Promise.all([
    submitButton.isDisabled().catch(() => true),
    page.locator('[data-frontline-production-operator] .frontline-top-card')
      .allInnerTexts()
      .catch(() => []),
    page.locator('.el-message, .el-message__content')
      .allInnerTexts()
      .catch(() => [])
  ])
  return {
    disabled,
    statusText: [
      ...topCards.map((text) => text.replace(/\s+/g, ' ').trim()).filter(Boolean),
      ...visibleErrors.map((text) => text.replace(/\s+/g, ' ').trim()).filter(Boolean)
    ].join(' | ')
  }
}

async function waitForFrontlineProductionSubmitReady(page, submitButton) {
  const startedAt = Date.now()
  let latestState = { disabled: true, statusText: '' }
  while (Date.now() - startedAt < 60000) {
    latestState = await readFrontlineProductionSubmitState(page, submitButton)
    if (!latestState.disabled) {
      return { status: 'PASS', ...latestState }
    }
    await page.waitForTimeout(500)
  }
  return { status: 'BLOCKED', ...latestState }
}

async function readPqcSubmitState(page, submitButton) {
  const [disabled, topCards, inspectionPanels, visibleErrors, signatureIdQuery] = await Promise.all([
    submitButton.isDisabled().catch(() => true),
    page.locator('[data-frontline-pqc-operator] .frontline-top-card')
      .allInnerTexts()
      .catch(() => []),
    page.locator('[data-frontline-pqc-operator] [data-pqc-active-inspection-panel], [data-frontline-pqc-operator] [data-pqc-inspection-meta]')
      .allInnerTexts()
      .catch(() => []),
    page.locator('.el-message, .el-message__content')
      .allInnerTexts()
      .catch(() => []),
    page.evaluate(() => new URL(window.location.href).searchParams.get('signatureId'))
      .catch(() => null)
  ])
  return {
    disabled,
    signatureIdQuery,
    statusText: [
      ...topCards,
      ...inspectionPanels,
      ...visibleErrors
    ].map((text) => text.replace(/\s+/g, ' ').trim()).filter(Boolean).join(' | ')
  }
}

async function waitForPqcSubmitReady(page, submitButton, expectedSignatureId) {
  const startedAt = Date.now()
  let latestState = { disabled: true, signatureIdQuery: null, statusText: '' }
  while (Date.now() - startedAt < 60000) {
    latestState = await readPqcSubmitState(page, submitButton)
    if (!latestState.disabled && Number(latestState.signatureIdQuery) === Number(expectedSignatureId)) {
      return { status: 'PASS', ...latestState }
    }
    await page.waitForTimeout(500)
  }
  return { status: 'BLOCKED', ...latestState }
}

async function submitFrontlineProductionForPqcPrereq(page, config, context) {
  const submitButton = page.locator('[data-frontline-production-operator] .frontline-production-submit-button').first()
  await submitButton.waitFor({ state: 'visible', timeout: 30000 })
  const readyResult = await waitForFrontlineProductionSubmitReady(page, submitButton)
  if (readyResult.status !== 'PASS') {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      disabledState: readyResult.statusText,
      description: `生产填写页提交按钮仍为禁用状态，不能生成 PQC 正式提交所需 source event。当前页面状态：${readyResult.statusText || '未读取到上下文'}。`
    }
  }
  const submitResponsePromise = page.waitForResponse((response) =>
    response.url().includes(FRONTLINE_SUBMIT_ENDPOINT)
      && response.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ frontlineSubmitResponseError: error }))
  await submitButton.click()
  const submitResponse = await submitResponsePromise
  if (submitResponse.frontlineSubmitResponseError) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      description: `点击生产填写提交后未捕获 ${FRONTLINE_SUBMIT_ENDPOINT} 响应：${submitResponse.frontlineSubmitResponseError.message}`
    }
  }
  if (!submitResponse.ok()) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      responseStatus: submitResponse.status(),
      description: `生产填写提交 HTTP 失败：${submitResponse.status()}。`
    }
  }
  const submitBody = await submitResponse.json()
  if (!isBusinessSuccess(submitBody)) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      responseCode: submitBody.code,
      responseMessage: responseMessage(submitBody),
      description: `生产填写提交业务失败：${responseMessage(submitBody)}。`
    }
  }
  const processPoolEventId = extractProcessPoolEventIdFromFrontlineResponse(submitBody)
  return {
    status: 'PASS',
    processPoolEventId,
    frontlineResponse: submitBody.data,
    productionContext: context
  }
}

async function resolveProductionSubmitContextForPqcPrereq(page, config, actionEvidence) {
  const processResult = await loadProductionProcessForPqcPrereq(page, config)
  if (processResult.status !== 'PASS') return processResult
  const process = processResult.process
  const [runtimeResult, employeeResult, taskResult, recordbookResult] = await Promise.all([
    loadRuntimeConfigForPqcPrereq(page, process),
    loadProductionEmployeeForPqcPrereq(page, process),
    loadProductionTaskForPqcPrereq(page, config, process),
    loadProductionRecordbookForPqcPrereq(page, config)
  ])
  for (const result of [runtimeResult, employeeResult, taskResult, recordbookResult]) {
    if (result.status !== 'PASS') return result
  }
  const leaderResolution = await resolveRoleUserId(page, config, 'productionLeader')
  if (leaderResolution.status !== 'PASS') {
    return {
      status: 'BLOCKED',
      category: leaderResolution.category,
      description: `解析生产组长审批人 ID 失败：${leaderResolution.description}`
    }
  }
  const signatureId = requireSignatureId(config, 'productionEmployee')
  const actualEmployeeId = requirePositiveFormalId(employeeResult.employee.userId, '生产填写实际员工')
  const task = taskResult.task
  const runtimeConfig = runtimeResult.runtimeConfig
  const deviceId = resolveProductionDeviceId(process, runtimeConfig)
  const feedbackCode = `${config.dataPrefix}PQC-SRC-${Date.now()}`
  const idempotencyKey = `${config.dataPrefix}PQC-SRC-IDEMP-${Date.now()}`
  return {
    status: 'PASS',
    process,
    processCount: processResult.processCount,
    employeeCount: employeeResult.employeeCount,
    taskCount: taskResult.taskCount,
    recordbookCount: recordbookResult.recordbookCount,
    feedbackCode,
    feedbackType: PRODUCTION_FEEDBACK_TYPE_SELF,
    taskId: requirePositiveFormalId(task.id, '生产任务'),
    itemId: requirePositiveFormalId(task.itemId, '生产任务产品物料'),
    routeProcessId: requirePositiveFormalId(process.routeProcessId, '生产路线工序'),
    processId: requirePositiveFormalId(process.processId, '生产工序'),
    workstationId: requirePositiveFormalId(process.workstationId || task.workstationId, '生产工作站'),
    deviceId,
    approveUserId: requirePositiveFormalId(leaderResolution.userId, '生产组长审批人'),
    recordbookId: requirePositiveFormalId(recordbookResult.recordbook.id, '生产记录本'),
    signatureId,
    signatureEmployeeId: actualEmployeeId,
    actualEmployeeId,
    outputQuantity: resolveProductionOutputQuantity(task),
    idempotencyKey,
    processPoolSubmissionIdempotencyKey: `${idempotencyKey}-PROCESS-POOL`,
    sourceActionKeys: actionEvidence
      .filter((item) => item.status === 'PASS')
      .map((item) => item.key)
  }
}

async function preparePqcFormalSubmissionContext(page, config, actionEvidence) {
  const browser = page.context().browser()
  if (!browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      description: '无法创建生产填写上下文，不能生成 PQC 正式提交所需 source event。'
    }
  }
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const productionPage = await context.newPage()
  try {
    await login(productionPage, config, 'productionEmployee', config.roles.productionEmployee)
    const productionContext = await resolveProductionSubmitContextForPqcPrereq(
      productionPage,
      config,
      actionEvidence
    )
    if (productionContext.status !== 'PASS') return productionContext
    const productionFillUrl = buildProductionFillUrl(config, productionContext)
    await productionPage.goto(productionFillUrl, { waitUntil: 'domcontentloaded', timeout: 90000 })
    await productionPage.locator('[data-frontline-production-operator]').first()
      .waitFor({ state: 'visible', timeout: 60000 })
    const submission = await submitFrontlineProductionForPqcPrereq(
      productionPage,
      config,
      productionContext
    )
    if (submission.status !== 'PASS') return submission
    const processPoolEventId = submission.processPoolEventId
    return {
      ...productionContext,
      status: 'PASS',
      processPoolEventId,
      productionSubmitEventId: processPoolEventId,
      frontlineResponse: submission.frontlineResponse,
      productionFillRoute: PRODUCTION_FILL_ROUTE
    }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SOURCE_EVENT',
      description: `准备 PQC 正式提交生产 source event 失败：${error.message}`
    }
  } finally {
    await context.close()
  }
}

async function verifyPqcFormalSubmissionCreatesEvent(page, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const regulationEvidence = actionEvidence.find((item) => item.key === 'pqcRegulationItemsRendered' && item.status === 'PASS')
  const pieceDetailEvidence = actionEvidence.find((item) => item.key === 'pqcPieceDetailQuantityPrepared' && item.status === 'PASS')
  const employeeEvidence = [...actionEvidence].reverse().find((item) =>
    ['pqcActualEmployeeSelected', 'pqcSelfReviewActualEmployeeSelected'].includes(item.key)
    && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId || !regulationEvidence || !pieceDetailEvidence || !employeeEvidence) {
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D29', 'AC-D32'],
      description: 'PQC 正式提交前缺少 activeOrder、规程项目、逐件数量或实际检验人动作证据，不能创建正式 submitted 事件。'
    }
  }

  const formalSubmissionContext = await preparePqcFormalSubmissionContext(page, config, actionEvidence)
  if (formalSubmissionContext.status !== 'PASS') {
    return buildPqcFormalSubmissionBlocker(
      formalSubmissionContext.category || 'E2E_PQC_SOURCE_EVENT',
      formalSubmissionContext.description || 'PQC 正式提交前无法生成生产报工 source event。',
      {
        activeOrderId: joinEvidence.activeOrderId,
        workOrderId: config.workOrderId,
        routeId: config.routeId,
        responseStatus: formalSubmissionContext.responseStatus,
        responseCode: formalSubmissionContext.responseCode,
        responseMessage: formalSubmissionContext.responseMessage,
        processCount: formalSubmissionContext.processCount,
        employeeCount: formalSubmissionContext.employeeCount,
        taskCount: formalSubmissionContext.taskCount,
        recordbookCount: formalSubmissionContext.recordbookCount
      }
    )
  }
  requireSignatureId(config, 'pqcInspector')
  const signatureResolution = await resolveUnusedPqcSignatureId(page, config, 'pqcInspector')
  if (signatureResolution.status !== 'PASS') {
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: signatureResolution.category,
      acceptanceIds: ['AC-D29', 'AC-D32'],
      activeOrderId: joinEvidence.activeOrderId,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      submissionTotal: signatureResolution.submissionTotal,
      description: signatureResolution.description
    }
  }
  const signatureId = signatureResolution.signatureId

  const pqcFillUrl = buildPqcFillUrl(config, formalSubmissionContext, employeeEvidence, signatureId)
  await page.goto(pqcFillUrl, { waitUntil: 'domcontentloaded', timeout: 90000 })
  await page.locator('[data-frontline-pqc-operator]').first().waitFor({ state: 'visible', timeout: 60000 })
  const restoredEmployeeEvidence = await switchPqcActualEmployeeToUser(
    page,
    config,
    actionEvidence,
    employeeEvidence.actualEmployeeId,
    'pqcFormalSubmissionActualEmployeeRestored',
    'PQC 正式提交前恢复实际检验人',
    ['AC-D25', 'AC-D31']
  )
  if (restoredEmployeeEvidence.status !== 'PASS') {
    return buildPqcFormalSubmissionBlocker(
      restoredEmployeeEvidence.category || 'E2E_PQC_PERSONNEL',
      `重新打开携带 productionSubmitEventId 的 PQC 页面后，无法恢复同一实际检验人：${restoredEmployeeEvidence.description}`,
      {
        activeOrderId: joinEvidence.activeOrderId,
        productionSubmitEventId: formalSubmissionContext.productionSubmitEventId,
        processPoolEventId: formalSubmissionContext.processPoolEventId,
        targetActualEmployeeId: employeeEvidence.actualEmployeeId
      }
    )
  }
  let refreshedPieceCompletion
  try {
    refreshedPieceCompletion = await completePqcPieceDetailsForSubmission(page)
  } catch (error) {
    return buildPqcFormalSubmissionBlocker(
      'E2E_PQC_PIECE_DETAIL',
      `重新打开携带 productionSubmitEventId 的 PQC 页面后，无法恢复逐件明细：${error.message}`,
      {
        activeOrderId: joinEvidence.activeOrderId,
        productionSubmitEventId: formalSubmissionContext.productionSubmitEventId,
        processPoolEventId: formalSubmissionContext.processPoolEventId
      }
    )
  }

  const submitButton = page.locator('.frontline-pqc-submit-button').first()
  await submitButton.waitFor({ state: 'visible', timeout: 30000 })
  const submitReady = await waitForPqcSubmitReady(page, submitButton, signatureId)
  if (submitReady.status !== 'PASS') {
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_UI',
      acceptanceIds: ['AC-D29', 'AC-D32'],
      activeOrderId: joinEvidence.activeOrderId,
      signatureId,
      signatureIdQuery: submitReady.signatureIdQuery,
      disabledState: submitReady.statusText,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      description: `PQC 页面未通过 URL signatureId 恢复可提交状态，不能证明签名和正式提交上下文已满足。当前 query signatureId=${submitReady.signatureIdQuery || '空'}，页面状态：${submitReady.statusText || '未读取到上下文'}。`
    }
  }

  const submitResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/feedback/frontline/device-account/pqc/submit')
      && response.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ pqcSubmitResponseError: error }))
  await submitButton.click()
  const submitResponse = await submitResponsePromise
  if (submitResponse.pqcSubmitResponseError) {
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_UI',
      acceptanceIds: ['AC-D29', 'AC-D32'],
      activeOrderId: joinEvidence.activeOrderId,
      signatureId,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      description: `点击 PQC 提交后未捕获正式提交接口响应：${submitResponse.pqcSubmitResponseError.message}`
    }
  }
  assert.ok(submitResponse.ok(), `PQC 正式提交 HTTP 失败：${submitResponse.status()}`)
  const submitBody = await submitResponse.json()
  if (!isBusinessSuccess(submitBody)) {
    const message = responseMessage(submitBody)
    const missingSourceEvent = message.includes('processPool.latestEventId') ||
      message.includes('latestEventId')
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: missingSourceEvent ? 'E2E_PQC_SOURCE_EVENT' : 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D29', 'AC-D32'],
      activeOrderId: joinEvidence.activeOrderId,
      signatureId,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      responseCode: submitBody.code,
      responseMessage: message,
      description: missingSourceEvent
        ? 'PQC 正式提交依赖生产报工 source event，但当前 process pool 缺少 latestEventId；需要先通过真实生产报工路径生成 source event 后再复验 PQC 提交。'
        : `PQC 正式提交业务失败：${message}`
    }
  }
  const submittedTaskId = Number(submitBody.data)
  assert.ok(Number.isFinite(submittedTaskId) && submittedTaskId > 0, 'PQC 正式提交必须返回已提交的正式 taskId。')

  await page.locator('.el-message, .el-notification').filter({
    hasText: /已提交|提交成功/
  }).first().waitFor({ state: 'visible', timeout: 10000 }).catch(() => {})

  const browser = page.context().browser()
  assert.ok(browser, 'PQC 正式提交事件核验必须能创建 PQC 组长只读上下文。')
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const leaderPage = await context.newPage()
  let submittedRow
  let submissionTotal = 0
  let submissionPageNo
  let submissionScannedRows = 0
  const submitDate = localDateString()
  try {
    await login(leaderPage, config, 'pqcLeader', config.roles.pqcLeader)
    const submissionLookup = await findPqcLeaderSubmissionRowByTaskAndSignature(leaderPage, {
      submitDate,
      workOrderCode: config.productionOrderCode
    }, submittedTaskId, signatureId)
    submissionTotal = submissionLookup.total
    submittedRow = submissionLookup.row
    submissionPageNo = submissionLookup.pageNo
    submissionScannedRows = submissionLookup.scannedRows
  } finally {
    await context.close()
  }
  if (!submittedRow) {
    return {
      key: 'pqcFormalSubmissionCreated',
      label: 'PQC 正式提交生成过程池检验事件',
      roleKey: 'pqcInspector',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D29', 'AC-D32'],
      activeOrderId: joinEvidence.activeOrderId,
      submittedTaskId,
      submitDate,
      submissionTotal,
      submissionScannedRows,
      signatureId,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      description: 'PQC 正式提交接口已返回 taskId，但跨页扫描 PQC 组长提交看板仍没有出现同一 pqcTaskId + signatureId 的正式事件；需要核对后端 createPqcInspectionEvent 链路和读模型来源。'
    }
  }

  return {
    key: 'pqcFormalSubmissionCreated',
    label: 'PQC 正式提交生成过程池检验事件',
    roleKey: 'pqcInspector',
    status: 'PASS',
    acceptanceIds: ['AC-D29', 'AC-D32'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    productionSubmitEventId: formalSubmissionContext.productionSubmitEventId,
    processPoolEventId: formalSubmissionContext.processPoolEventId,
    productionTaskId: formalSubmissionContext.taskId,
    productionRecordbookId: formalSubmissionContext.recordbookId,
    productionFeedbackCode: formalSubmissionContext.feedbackCode,
    submittedTaskId,
    eventId: submittedRow.id,
    processId: Number(submittedRow.processId),
    productCode: submittedRow.productCode,
    productName: submittedRow.productName,
    inspectionType: submittedRow.inspectionType,
    roundNo: Number(submittedRow.roundNo),
    submitDate,
    submissionTotal,
    submissionPageNo,
    submissionScannedRows,
    actualEmployeeId: employeeEvidence.actualEmployeeId,
    signatureId,
    candidateSignatureIds: signatureResolution.candidateSignatureIds,
    usedSignatureIds: signatureResolution.usedSignatureIds,
    plannedQuantities: regulationEvidence.plannedQuantities,
    uiQuantity: pieceDetailEvidence.uiQuantity,
    refreshedPieceCompletion,
    sourceActionKeys: [
      ...formalSubmissionContext.sourceActionKeys,
      regulationEvidence.key,
      pieceDetailEvidence.key,
      employeeEvidence.key,
      restoredEmployeeEvidence.key
    ],
    endpoint: '/mes/pro/feedback/frontline/device-account/pqc/submit'
  }
}

function localDateString(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toQueryString(params) {
  const query = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  }
  return query.toString()
}

async function loadPqcLeaderSubmissionPage(page, params) {
  const query = toQueryString({
    leaderType: 'PQC',
    templateType: 'PQC_SIMPLIFIED',
    ...params
  })
  const result = await fetchWithPageAuth(
    page,
    `/admin-api/mes/pro/process-pool/team-leader/submission/page?${query}`
  )
  assert.ok(result.ok, `PQC 组长提交看板 HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `PQC 组长提交看板业务失败：${responseMessage(result.body)}`)
  const data = result.body.data || {}
  return {
    total: Number(data.total || 0),
    list: Array.isArray(data.list) ? data.list : []
  }
}

async function loadEdhrReleasePage(page, params) {
  const query = toQueryString(params)
  const result = await fetchWithPageAuth(
    page,
    `/admin-api/mes/pro/edhr-release/page?${query}`
  )
  assert.ok(result.ok, `eDHR 放行追溯列表 HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `eDHR 放行追溯列表业务失败：${responseMessage(result.body)}`)
  const data = result.body.data || {}
  return {
    total: Number(data.total || 0),
    list: Array.isArray(data.list) ? data.list : []
  }
}

async function loadEdhrReleaseCheckItemPage(page, params) {
  const query = toQueryString(params)
  const result = await fetchWithPageAuth(
    page,
    `/admin-api/mes/pro/edhr-release/check-item/page?${query}`
  )
  assert.ok(result.ok, `eDHR 放行检查项 HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `eDHR 放行检查项业务失败：${responseMessage(result.body)}`)
  const data = result.body.data || {}
  return {
    total: Number(data.total || 0),
    list: Array.isArray(data.list) ? data.list : []
  }
}

async function loadEdhrReleaseEventPage(page, params) {
  const query = toQueryString(params)
  const result = await fetchWithPageAuth(
    page,
    `/admin-api/mes/pro/edhr-release/event/page?${query}`
  )
  assert.ok(result.ok, `eDHR 放行事务事件 HTTP 失败：${result.status}`)
  assert.equal(result.body?.code, 0, `eDHR 放行事务事件业务失败：${responseMessage(result.body)}`)
  const data = result.body.data || {}
  return {
    total: Number(data.total || 0),
    list: Array.isArray(data.list) ? data.list : []
  }
}

async function findPqcLeaderSubmissionRowByTaskAndSignature(page, baseParams, submittedTaskId, signatureId) {
  const pageSize = 100
  let total = 0
  let scannedRows = 0
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
    const submissionPage = await loadPqcLeaderSubmissionPage(page, {
      ...baseParams,
      pageNo,
      pageSize
    })
    total = Math.max(total, submissionPage.total)
    scannedRows += submissionPage.list.length
    const row = submissionPage.list.find((item) =>
      Number(item.pqcTaskId) === Number(submittedTaskId)
        && Number(item.electronicSignatureId) === Number(signatureId))
    if (row) {
      return { row, total, pageNo, scannedRows }
    }
    if (submissionPage.list.length === 0 || pageNo * pageSize >= submissionPage.total) {
      break
    }
  }
  return { row: undefined, total, pageNo: undefined, scannedRows }
}

function includesText(source, keyword) {
  return String(source || '').toLowerCase().includes(String(keyword || '').toLowerCase())
}

async function expectTextIncludes(locator, keyword) {
  const text = await locator.textContent()
  assert.ok(includesText(text, keyword), `页面文本必须包含 ${keyword}，当前为：${text || ''}`)
}

function assertPqcLeaderSubmissionRowMatches(row, filters) {
  assert.equal(String(row.workOrderCode || ''), String(filters.workOrderCode || ''), 'PQC 提交行必须匹配工单编码筛选。')
  assert.equal(Number(row.processId), Number(filters.processId), 'PQC 提交行必须匹配工序筛选。')
  assert.equal(Number(row.actualEmployeeUserId), Number(filters.employeeUserId), 'PQC 提交行必须匹配人员筛选。')
  assert.ok(
    includesText(row.productCode, filters.productKeyword) || includesText(row.productName, filters.productKeyword),
    'PQC 提交行必须匹配产品编码或名称筛选。'
  )
  assert.equal(row.inspectionType, filters.inspectionType, 'PQC 提交行必须匹配检验类型筛选。')
  assert.equal(Number(row.roundNo), Number(filters.roundNo), 'PQC 提交行必须匹配轮次筛选。')
  if (filters.submissionReviewStatus === 'PENDING') {
    assert.ok(!row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING', '待判定筛选只允许空状态或 PENDING。')
  } else {
    assert.equal(row.submissionReviewStatus, filters.submissionReviewStatus, 'PQC 提交行必须匹配复核状态筛选。')
  }
}

async function selectElementPlusOption(page, selector, label) {
  await page.locator(selector).first().click()
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: label })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

function inspectionTypeLabel(type) {
  if (type === 'FIRST') return '首检'
  if (type === 'PATROL') return '巡检'
  if (type === 'FINAL') return '末检'
  return type
}

function reviewStatusLabel(status) {
  if (status === 'APPROVED') return '正确'
  if (status === 'REJECTED') return '不正确'
  return '待判定'
}

async function searchPqcLeaderSubmissionsOnPage(page, filters) {
  const section = page.locator('[data-team-leader-report-workbench]').first()
  await fillFormItem(section, '提交日期', filters.submitDate)
  await fillFormItem(section, '生产工单', filters.workOrderCode)
  await fillFormItem(section, 'PQC检验员', filters.employeeUserId)
  await fillFormItem(section, '工序', filters.processId)
  await fillElementPlusInput(section, '[data-pqc-leader-filter-product]', filters.productKeyword)
  await selectElementPlusOption(page, '[data-pqc-leader-filter-inspection-type]', inspectionTypeLabel(filters.inspectionType))
  await fillElementPlusInput(section, '[data-pqc-leader-filter-round]', filters.roundNo)
  await selectElementPlusOption(page, '[data-pqc-leader-filter-review-status]', reviewStatusLabel(filters.submissionReviewStatus))
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/process-pool/team-leader/submission/page')
      && response.request().method() === 'GET'
  , { timeout: 30000 }).catch((error) => ({ submissionFilterResponseError: error }))
  await section.getByRole('button', { name: '搜索' }).click()
  const response = await responsePromise
  return { section, response }
}

async function preparePqcPaginationCandidate(page, config, actionEvidence) {
  const browser = page.context().browser()
  if (!browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      description: '无法创建 PQC 检验员真实页面上下文来准备 D32 同条件分页候选。'
    }
  }
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const pqcPage = await context.newPage()
  try {
    await login(pqcPage, config, 'pqcInspector', config.roles.pqcInspector)
    const readOnlyEvidence = await verifyPqcActiveOrderReadOnly(pqcPage, config, actionEvidence)
    if (readOnlyEvidence.status !== 'PASS') return readOnlyEvidence
    const regulationEvidence = await verifyPqcRegulationItemsRendered(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence
    ])
    if (regulationEvidence.status !== 'PASS') return regulationEvidence
    const pieceDetailEvidence = await verifyPqcPieceDetailQuantityPrepared(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence
    ])
    if (pieceDetailEvidence.status !== 'PASS') return pieceDetailEvidence
    const employeeEvidence = await verifyPqcActualEmployeeSwitch(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence
    ])
    if (employeeEvidence.status !== 'PASS') return employeeEvidence
    const formalSubmissionEvidence = await verifyPqcFormalSubmissionCreatesEvent(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence,
      employeeEvidence
    ])
    if (formalSubmissionEvidence.status !== 'PASS') return formalSubmissionEvidence
    return {
      ...formalSubmissionEvidence,
      key: 'pqcPaginationCandidatePrepared',
      label: 'AC-D32 同条件分页候选准备',
      acceptanceIds: ['AC-D32'],
      sourceActionKey: formalSubmissionEvidence.key
    }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      description: `通过 PQC 检验员真实页面准备 D32 同条件分页候选失败：${error.message}`
    }
  } finally {
    await context.close()
  }
}

async function verifyPqcLeaderSubmissionFilterPaginationConsistency(page, config, actionEvidence = []) {
  const submitDate = localDateString()
  const baseParams = {
    pageNo: 1,
    pageSize: 20,
    submitDate,
    workOrderCode: config.productionOrderCode
  }
  const basePage = await loadPqcLeaderSubmissionPage(page, baseParams)
  if (basePage.total === 0) {
    return {
      key: 'pqcLeaderSubmissionFilterPaginationConsistent',
      label: 'PQC 组长提交看板筛选分页一致性',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D32'],
      submitDate,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      description: '当前本机租户没有当天球囊扩张压力泵任务的正式 PQC 提交事件，无法通过真实 PQC 组长页面证明多条件筛选和分页 total 一致性；需要先完成至少两笔可筛选的正式 PQC 提交夹具。'
    }
  }

  const candidates = basePage.list.filter((row) =>
    row.workOrderCode
    && row.productCode
    && row.productName
    && row.processId
    && row.actualEmployeeUserId
    && row.inspectionType
    && row.roundNo
  )
  if (candidates.length === 0) {
    return {
      key: 'pqcLeaderSubmissionFilterPaginationConsistent',
      label: 'PQC 组长提交看板筛选分页一致性',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D32'],
      submitDate,
      sampleCount: basePage.list.length,
      description: 'PQC 提交看板行缺少产品、工序、人员、检验类型或轮次正式字段；不能用不完整读模型证明 AC-D32 筛选。'
    }
  }

  let candidateWithStablePagination = null
  const candidateTotals = []
  const inspectCandidate = async (candidate, source) => {
    const candidateFilters = {
      pageNo: 1,
      pageSize: 1,
      submitDate,
      workOrderCode: candidate.workOrderCode,
      employeeUserId: Number(candidate.actualEmployeeUserId),
      processId: Number(candidate.processId),
      productKeyword: candidate.productCode,
      inspectionType: candidate.inspectionType,
      roundNo: Number(candidate.roundNo),
      submissionReviewStatus: candidate.submissionReviewStatus || 'PENDING'
    }
    const candidatePage = await loadPqcLeaderSubmissionPage(page, candidateFilters)
    candidateTotals.push({
      source,
      eventId: candidate.id,
      processId: candidate.processId,
      employeeUserId: candidate.actualEmployeeUserId,
      inspectionType: candidate.inspectionType,
      roundNo: candidate.roundNo,
      submissionReviewStatus: candidateFilters.submissionReviewStatus,
      total: candidatePage.total
    })
    if (candidatePage.total >= 2) {
      assert.equal(candidatePage.list.length, 1, 'pageSize=1 时稳定候选第一页必须只返回一条明细。')
      assertPqcLeaderSubmissionRowMatches(candidatePage.list[0], candidateFilters)
      candidateWithStablePagination = { candidate, filters: candidateFilters, firstPage: candidatePage }
      return true
    }
    return false
  }

  for (const candidate of candidates) {
    const stableCandidateFound = await inspectCandidate(candidate, 'base')
    if (stableCandidateFound) break
    if (candidateWithStablePagination) break
  }

  let preparedPaginationCandidate = null
  if (!candidateWithStablePagination) {
    preparedPaginationCandidate = await preparePqcPaginationCandidate(page, config, actionEvidence)
    if (preparedPaginationCandidate.status === 'PASS') {
      const refreshedBasePage = await loadPqcLeaderSubmissionPage(page, baseParams)
      const refreshedCandidates = refreshedBasePage.list.filter((row) =>
        row.workOrderCode
        && row.productCode
        && row.productName
        && row.processId
        && row.actualEmployeeUserId
        && row.inspectionType
        && row.roundNo
      )
      for (const candidate of refreshedCandidates) {
        const stableCandidateFound = await inspectCandidate(candidate, 'afterPreparedPaginationCandidate')
        if (stableCandidateFound) break
        if (candidateWithStablePagination) break
      }
    }
  }

  if (!candidateWithStablePagination) {
    return {
      key: 'pqcLeaderSubmissionFilterPaginationConsistent',
      label: 'PQC 组长提交看板筛选分页一致性',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_SUBMISSION_DATA',
      acceptanceIds: ['AC-D32'],
      submitDate,
      candidateTotals,
      preparedPaginationCandidate,
      description: '当前当天 PQC 提交看板存在候选行，但没有任何完整筛选组合命中至少两条正式提交；已尝试通过真实 PQC 页面补充一条同条件提交，仍无法证明第 1/2 页 total 稳定，需要补齐同条件正式 PQC 提交夹具。'
    }
  }

  const { filters } = candidateWithStablePagination
  const requestBudgetTracker = createPqcLeaderSubmissionListRequestBudgetTracker(page)
  try {
    const { response } = await searchPqcLeaderSubmissionsOnPage(page, filters)
    if (response.submissionFilterResponseError) {
      return {
        key: 'pqcLeaderSubmissionFilterPaginationConsistent',
        label: 'PQC 组长提交看板筛选分页一致性',
        roleKey: 'pqcLeader',
        status: 'BLOCKED',
        category: 'E2E_PQC_SUBMISSION_FILTER',
        acceptanceIds: ['AC-D32'],
        submitDate,
        requestBudget: requestBudgetTracker.snapshot(),
        description: `真实页面筛选后未捕获提交看板分页接口响应：${response.submissionFilterResponseError.message}`
      }
    }
    assert.ok(response.ok(), `PQC 组长页面筛选 HTTP 失败：${response.status()}`)

    const firstPage = await loadPqcLeaderSubmissionPage(page, filters)
    assert.ok(firstPage.total > 0, 'PQC 组长筛选后的 total 必须大于 0。')
    assert.equal(firstPage.list.length, 1, 'pageSize=1 时第一页必须只返回一条明细。')
    assertPqcLeaderSubmissionRowMatches(firstPage.list[0], filters)
    assert.ok(firstPage.total >= 2, '稳定候选在真实页面筛选后仍必须命中至少两条正式 PQC 提交。')

    const secondPage = await loadPqcLeaderSubmissionPage(page, { ...filters, pageNo: 2 })
    assert.equal(secondPage.total, firstPage.total, 'PQC 组长筛选第一页和第二页 total 必须一致。')
    assert.equal(secondPage.list.length, 1, 'pageSize=1 时第二页必须只返回一条明细。')
    assert.notEqual(secondPage.list[0].id, firstPage.list[0].id, '第二页不能重复返回第一页事件。')
    assertPqcLeaderSubmissionRowMatches(secondPage.list[0], filters)

    const requestBudget = requestBudgetTracker.snapshot()
    assert.equal(requestBudget.submissionDetailRequests, 0, 'PQC 组长列表筛选和翻页不得触发逐行提交详情请求。')
    assert.equal(requestBudget.activeOrderListRequests, 0, 'PQC 组长列表筛选和翻页不得额外读取活跃订单列表。')
    assert.ok(
      requestBudget.submissionPageRequests >= 3,
      'PQC 组长列表筛选和第 1/2 页核验必须至少记录搜索页、第一页和第二页三次分页请求。'
    )
    assert.ok(
      requestBudget.submissionPageRequests <= 4,
      `PQC 组长列表筛选和第 1/2 页核验分页请求数必须保持有界，当前为 ${requestBudget.submissionPageRequests}。`
    )

    return {
      key: 'pqcLeaderSubmissionFilterPaginationConsistent',
      label: 'PQC 组长提交看板筛选分页一致性',
      roleKey: 'pqcLeader',
      status: 'PASS',
      category: 'E2E_PERFORMANCE',
      acceptanceIds: ['AC-D32'],
      submitDate,
      filters,
      total: firstPage.total,
      firstEventId: firstPage.list[0].id,
      secondEventId: secondPage.list[0].id,
      requestBudget
    }
  } finally {
    requestBudgetTracker.stop()
  }
}

function parseOriginalPayloadJson(rawPayload, label) {
  assert.ok(rawPayload, `${label} 必须返回 originalPayloadJson。`)
  try {
    return JSON.parse(rawPayload)
  } catch (error) {
    throw new Error(`${label} originalPayloadJson 不是合法 JSON：${error.message}`)
  }
}

function countPqcPieceValueGroups(pqcPieceValues) {
  if (!pqcPieceValues || typeof pqcPieceValues !== 'object') return 0
  return Array.isArray(pqcPieceValues) ? pqcPieceValues.length : Object.keys(pqcPieceValues).length
}

async function verifyPqcLeaderSubmissionDetailTraceability(page, config, actionEvidence) {
  const formalSubmission = actionEvidence.find((item) =>
    item.key === 'pqcFormalSubmissionCreated' && item.status === 'PASS')
  if (!formalSubmission?.eventId || !formalSubmission?.submittedTaskId) {
    return {
      key: 'pqcLeaderSubmissionDetailTraceable',
      label: 'PQC 组长提交详情逐件追溯',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_DATA',
      acceptanceIds: ['AC-D33'],
      description: 'PQC 组长详情核验前缺少本轮正式 PQC 提交事件，不能用旧事件证明逐件明细和原始 payload 可追溯。'
    }
  }

  const pendingFilters = {
    pageNo: 1,
    pageSize: 20,
    submitDate: formalSubmission.submitDate || localDateString(),
    workOrderCode: config.productionOrderCode,
    employeeUserId: Number(formalSubmission.actualEmployeeId),
    processId: Number(formalSubmission.processId),
    productKeyword: formalSubmission.productCode,
    inspectionType: formalSubmission.inspectionType,
    roundNo: Number(formalSubmission.roundNo),
    submissionReviewStatus: 'PENDING'
  }
  const { section, response } = await searchPqcLeaderSubmissionsOnPage(page, pendingFilters)
  if (response.submissionFilterResponseError) {
    return {
      key: 'pqcLeaderSubmissionDetailTraceable',
      label: 'PQC 组长提交详情逐件追溯',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_PAGE',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      description: `真实页面定位 PQC 详情行时未捕获提交看板分页接口响应：${response.submissionFilterResponseError.message}`
    }
  }
  assert.ok(response.ok(), `PQC 组长详情前筛选 HTTP 失败：${response.status()}`)
  const pendingPage = await loadPqcLeaderSubmissionPage(page, pendingFilters)
  const pendingRow = pendingPage.list.find((row) => Number(row.id) === Number(formalSubmission.eventId))
  if (!pendingRow) {
    return {
      key: 'pqcLeaderSubmissionDetailTraceable',
      label: 'PQC 组长提交详情逐件追溯',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_DATA',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      pendingTotal: pendingPage.total,
      description: 'PQC 组长待判定筛选结果中未出现本轮新提交事件，不能打开同一事件详情。'
    }
  }

  const detailResponsePromise = page.waitForResponse((detailResponse) =>
    detailResponse.url().includes('/mes/pro/process-pool/team-leader/submission/detail')
      && detailResponse.request().method() === 'GET'
  , { timeout: 30000 }).catch((error) => ({ detailResponseError: error }))
  const detailButton = section
    .locator(`[data-team-leader-detail-event-id="${formalSubmission.eventId}"]`)
    .first()
  const detailButtonVisible = await detailButton
    .waitFor({ state: 'visible', timeout: 30000 })
    .then(() => true)
    .catch((error) => ({ detailButtonError: error }))
  if (detailButtonVisible.detailButtonError) {
    return {
      key: 'pqcLeaderSubmissionDetailTraceable',
      label: 'PQC 组长提交详情逐件追溯',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_PAGE',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      pendingTotal: pendingPage.total,
      processId: pendingRow.processId,
      productCode: pendingRow.productCode,
      inspectionType: pendingRow.inspectionType,
      roundNo: pendingRow.roundNo,
      description: `PQC 组长待判定读模型包含本轮事件，但真实页面未渲染详情按钮 data-team-leader-detail-event-id=${formalSubmission.eventId}：${detailButtonVisible.detailButtonError.message}`
    }
  }
  await detailButton.click()
  const detailResponse = await detailResponsePromise
  if (detailResponse.detailResponseError) {
    return {
      key: 'pqcLeaderSubmissionDetailTraceable',
      label: 'PQC 组长提交详情逐件追溯',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_PAGE',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      description: `打开 PQC 组长详情后未捕获详情接口响应：${detailResponse.detailResponseError.message}`
    }
  }
  assert.ok(detailResponse.ok(), `PQC 组长详情 HTTP 失败：${detailResponse.status()}`)
  const detailBody = await detailResponse.json()
  assert.equal(detailBody?.code, 0, `PQC 组长详情业务失败：${responseMessage(detailBody)}`)
  const detail = detailBody.data || {}
  assert.equal(Number(detail.id), Number(formalSubmission.eventId), 'PQC 详情必须返回同一提交事件。')
  assert.equal(Number(detail.pqcTaskId), Number(formalSubmission.submittedTaskId), 'PQC 详情必须绑定同一正式 taskId。')
  assert.equal(Number(detail.electronicSignatureId), Number(formalSubmission.signatureId), 'PQC 详情必须返回提交签名编号。')
  assert.equal(Number(detail.actualEmployeeUserId), Number(formalSubmission.actualEmployeeId), 'PQC 详情必须返回实际检验人员。')
  assert.equal(detail.inspectionType, formalSubmission.inspectionType, 'PQC 详情必须返回检验类型快照。')
  assert.equal(Number(detail.roundNo), Number(formalSubmission.roundNo), 'PQC 详情必须返回轮次快照。')

  const originalPayload = parseOriginalPayloadJson(detail.originalPayloadJson, 'PQC 组长详情')
  assert.equal(Number(originalPayload.pqcTaskId), Number(formalSubmission.submittedTaskId), '原始 payload 必须保留 PQC taskId。')
  assert.equal(Number(originalPayload.workOrderId), Number(config.workOrderId), '原始 payload 必须保留工单 ID。')
  assert.equal(Number(originalPayload.routeId), Number(config.routeId), '原始 payload 必须保留路线 ID。')
  assert.ok(Number(originalPayload.regulationVersionId) > 0, '原始 payload 必须保留 QA 规程版本 ID。')
  assert.ok(Number(originalPayload.pieceDetailCount) > 0, '原始 payload 必须保留逐件明细数量。')
  const pieceValueGroupCount = countPqcPieceValueGroups(originalPayload.pqcPieceValues)
  assert.ok(pieceValueGroupCount > 0, '原始 payload 必须保留 pqcPieceValues 逐件明细。')

  const drawer = page.locator('.el-drawer:visible', { hasText: 'PQC检验员提交详情' }).first()
  await drawer.locator('[data-team-leader-structured-detail]').waitFor({ state: 'visible', timeout: 30000 })
  await drawer.locator('[data-pqc-submission-log]').waitFor({ state: 'visible', timeout: 30000 })
  await expectTextIncludes(drawer.locator('[data-pqc-submission-original-payload]').first(), 'pqcPieceValues')
  await expectTextIncludes(drawer.locator('[data-pqc-submission-signature-id]').first(), String(formalSubmission.signatureId))
  await drawer.locator('.el-drawer__close-btn').first().click()
  await drawer.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {})

  return {
    key: 'pqcLeaderSubmissionDetailTraceable',
    label: 'PQC 组长提交详情逐件追溯',
    roleKey: 'pqcLeader',
    status: 'PASS',
    acceptanceIds: ['AC-D33'],
    eventId: formalSubmission.eventId,
    submittedTaskId: formalSubmission.submittedTaskId,
    signatureId: formalSubmission.signatureId,
    actualEmployeeId: formalSubmission.actualEmployeeId,
    regulationVersionId: Number(originalPayload.regulationVersionId),
    pieceDetailCount: Number(originalPayload.pieceDetailCount),
    pieceValueGroupCount,
    endpoint: '/mes/pro/process-pool/team-leader/submission/detail'
  }
}

async function verifyPqcLeaderSubmissionDetailUnauthorizedBlocked(page, config, actionEvidence) {
  const formalSubmission = actionEvidence.find((item) =>
    item.key === 'pqcFormalSubmissionCreated' && item.status === 'PASS')
  if (!formalSubmission?.eventId) {
    return {
      key: 'pqcLeaderSubmissionDetailUnauthorizedBlocked',
      label: 'PQC 组长提交详情权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_PERMISSION',
      acceptanceIds: ['AC-D33'],
      description: 'PQC 详情权限隔离核验前缺少本轮正式 PQC 提交事件，不能用旧事件或空事件证明详情读取会被拒绝。'
    }
  }

  const unauthorizedActor = config.unauthorizedActor || {}
  if (!unauthorizedActor.username || !unauthorizedActor.password) {
    return {
      key: 'pqcLeaderSubmissionDetailUnauthorizedBlocked',
      label: 'PQC 组长提交详情权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'BLOCKED',
      category: 'E2E_PQC_DETAIL_PERMISSION',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      description: '缺少 RRM_UNAUTHORIZED_USERNAME / RRM_UNAUTHORIZED_PASSWORD，不能执行错误角色真实登录与详情读取拒绝核验。'
    }
  }

  const browser = page.context().browser()
  assert.ok(browser, 'PQC 详情权限隔离必须能创建独立浏览器上下文。')
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const unauthorizedPage = await context.newPage()
  try {
    await login(unauthorizedPage, config, 'unauthorizedActor', unauthorizedActor)
    const targetUrl = new URL('/mes/pro/process-pool/team-leader', config.frontendUrl)
    await unauthorizedPage.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
    await unauthorizedPage.locator('#app').waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(
      await unauthorizedPage.locator(`[data-team-leader-detail-event-id="${formalSubmission.eventId}"]`).count(),
      0,
      '错误角色不应在真实页面看到 PQC 提交详情入口。'
    )

    const result = await fetchWithPageAuth(
      unauthorizedPage,
      `/admin-api/mes/pro/process-pool/team-leader/submission/detail?id=${encodeURIComponent(formalSubmission.eventId)}&leaderType=PQC_LEADER`
    )
    assert.ok(
      !result.ok || !isBusinessSuccess(result.body),
      '错误角色调用 PQC 组长提交详情接口必须被后端拒绝，不能返回业务成功。'
    )
    return {
      key: 'pqcLeaderSubmissionDetailUnauthorizedBlocked',
      label: 'PQC 组长提交详情权限隔离',
      roleKey: 'unauthorizedActor',
      status: 'PASS',
      category: 'E2E_PQC_DETAIL_PERMISSION',
      acceptanceIds: ['AC-D33'],
      eventId: formalSubmission.eventId,
      username: unauthorizedActor.username,
      responseStatus: result.status,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body),
      endpoint: '/mes/pro/process-pool/team-leader/submission/detail'
    }
  } finally {
    await context.close()
  }
}

async function verifyPqcLeaderReviewApprovalAggregatesProcessInspection(page, config, actionEvidence) {
  const formalSubmission = actionEvidence.find((item) =>
    item.key === 'pqcFormalSubmissionCreated' && item.status === 'PASS')
  if (!formalSubmission?.eventId || !formalSubmission?.submittedTaskId) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_DATA',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      description: 'PQC 组长复核前缺少本轮正式 PQC 提交事件证据，不能用旧数据替代真实复核汇集验证。'
    }
  }
  if (!formalSubmission.processId || !formalSubmission.productCode
    || !formalSubmission.inspectionType || !formalSubmission.roundNo
    || !formalSubmission.actualEmployeeId) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_DATA',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      submittedTaskId: formalSubmission.submittedTaskId,
      description: 'PQC 正式提交事件缺少 processId、productCode、inspectionType、roundNo 或 actualEmployeeId，无法通过真实页面精确定位同一待复核行。'
    }
  }

  const pendingFilters = {
    pageNo: 1,
    pageSize: 20,
    submitDate: formalSubmission.submitDate || localDateString(),
    workOrderCode: config.productionOrderCode,
    employeeUserId: Number(formalSubmission.actualEmployeeId),
    processId: Number(formalSubmission.processId),
    productKeyword: formalSubmission.productCode,
    inspectionType: formalSubmission.inspectionType,
    roundNo: Number(formalSubmission.roundNo),
    submissionReviewStatus: 'PENDING'
  }
  const { section, response } = await searchPqcLeaderSubmissionsOnPage(page, pendingFilters)
  if (response.submissionFilterResponseError) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_PAGE',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      description: `真实页面定位待复核 PQC 行时未捕获提交看板分页接口响应：${response.submissionFilterResponseError.message}`
    }
  }
  assert.ok(response.ok(), `PQC 组长复核前筛选 HTTP 失败：${response.status()}`)
  const pendingPage = await loadPqcLeaderSubmissionPage(page, pendingFilters)
  const pendingRow = pendingPage.list.find((row) => Number(row.id) === Number(formalSubmission.eventId))
  if (!pendingRow) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_DATA',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      submittedTaskId: formalSubmission.submittedTaskId,
      pendingTotal: pendingPage.total,
      description: 'PQC 组长待判定筛选结果中未出现本轮新提交事件，不能执行真实页面复核。'
    }
  }

  const reviewButton = section
    .locator(`[data-team-leader-review-event-id="${formalSubmission.eventId}"]`)
    .first()
  const reviewButtonVisible = await reviewButton
    .waitFor({ state: 'visible', timeout: 30000 })
    .then(() => true)
    .catch((error) => ({ reviewButtonError: error }))
  if (reviewButtonVisible.reviewButtonError) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_PAGE',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      submittedTaskId: formalSubmission.submittedTaskId,
      pendingTotal: pendingPage.total,
      processId: pendingRow.processId,
      productCode: pendingRow.productCode,
      inspectionType: pendingRow.inspectionType,
      roundNo: pendingRow.roundNo,
      description: `PQC 组长待判定读模型包含本轮提交事件，但真实页面未渲染复核按钮 data-team-leader-review-event-id=${formalSubmission.eventId}：${reviewButtonVisible.reviewButtonError.message}`
    }
  }
  await reviewButton.click()
  const dialog = page.locator('.el-dialog:visible', { hasText: '复核员工提交' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('textarea').first().fill('M6 AC-D37 real-page approved review aggregates process inspection.')
  const reviewResponsePromise = page.waitForResponse((reviewResponse) =>
    reviewResponse.url().includes('/mes/pro/process-pool/team-leader/submission/review')
      && reviewResponse.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ reviewResponseError: error }))
  await dialog.getByRole('button', { name: '提交复核' }).click()
  const reviewResponse = await reviewResponsePromise
  if (reviewResponse.reviewResponseError) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_PAGE',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      description: `提交 PQC 组长复核后未捕获正式复核接口响应：${reviewResponse.reviewResponseError.message}`
    }
  }
  assert.ok(reviewResponse.ok(), `PQC 组长复核 HTTP 失败：${reviewResponse.status()}`)
  const reviewBody = await reviewResponse.json()
  if (!isBusinessSuccess(reviewBody)) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_BUSINESS',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      responseCode: reviewBody.code,
      responseMessage: responseMessage(reviewBody),
      description: `PQC 组长复核接口业务失败：${responseMessage(reviewBody)}`
    }
  }
  const reviewId = Number(reviewBody.data)
  assert.ok(Number.isFinite(reviewId) && reviewId > 0, 'PQC 组长复核必须返回正式 reviewId。')

  const approvedFilters = {
    ...pendingFilters,
    submissionReviewStatus: 'APPROVED'
  }
  await searchPqcLeaderSubmissionsOnPage(page, approvedFilters)
  const approvedPage = await loadPqcLeaderSubmissionPage(page, approvedFilters)
  const reviewedRow = approvedPage.list.find((row) => Number(row.id) === Number(formalSubmission.eventId))
  if (!reviewedRow) {
    return {
      key: 'pqcLeaderReviewApprovedAndAggregated',
      label: 'PQC 组长确认后汇集过程检验记录',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_READ_MODEL',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: formalSubmission.eventId,
      reviewId,
      approvedTotal: approvedPage.total,
      description: '复核成功后，PQC 组长正确筛选看板未返回同一事件；不能证明最新复核状态进入正式读模型。'
    }
  }
  assert.equal(reviewedRow.submissionReviewStatus, 'APPROVED', 'PQC 复核后读模型必须显示 APPROVED。')
  assert.equal(
    reviewedRow.processInspectionAggregationStatus,
    'AGGREGATED',
    'PQC 复核后读模型必须显示过程检验汇集状态 AGGREGATED。'
  )
  assert.equal(
    Number(reviewedRow.processInspectionReviewId),
    reviewId,
    '过程检验汇集记录必须绑定触发汇集的正式 reviewId。'
  )
  assert.ok(reviewedRow.processInspectionAggregatedAt, '过程检验汇集读模型必须返回汇集时间。')
  const aggregationCell = page
    .locator(`[data-pqc-process-inspection-aggregation][data-pqc-process-inspection-event-id="${formalSubmission.eventId}"]`)
    .first()
  await aggregationCell.waitFor({ state: 'visible', timeout: 30000 })
  await expectTextIncludes(aggregationCell, '已汇集')

  return {
    key: 'pqcLeaderReviewApprovedAndAggregated',
    label: 'PQC 组长确认后汇集过程检验记录',
    roleKey: 'pqcLeader',
    status: 'PASS',
    acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
    eventId: formalSubmission.eventId,
    submittedTaskId: formalSubmission.submittedTaskId,
    submitDate: formalSubmission.submitDate || localDateString(),
    reviewId,
    processInspectionAggregationStatus: reviewedRow.processInspectionAggregationStatus,
    processInspectionReviewId: reviewedRow.processInspectionReviewId,
    processInspectionAggregatedAt: reviewedRow.processInspectionAggregatedAt,
    endpoint: '/mes/pro/process-pool/team-leader/submission/review'
  }
}

async function verifyPqcLeaderDuplicateTerminalReviewBlocked(page, config, actionEvidence) {
  const approvedReview = actionEvidence.find((item) =>
    item.key === 'pqcLeaderReviewApprovedAndAggregated' && item.status === 'PASS')
  if (!approvedReview?.eventId || !approvedReview?.reviewId) {
    return {
      key: 'pqcLeaderDuplicateTerminalReviewBlocked',
      label: 'PQC 组长重复终态复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_TERMINAL',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      description: '重复终态复核失败路径缺少本轮 pqcLeaderReviewApprovedAndAggregated PASS 证据，不能用旧事件替代同事件二次复核验证。'
    }
  }

  const expectedErrorKey = 'PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS'
  const expectedErrorCode = 1040760325
  const result = await fetchWithPageAuth(
    page,
    '/admin-api/mes/pro/process-pool/team-leader/submission/review',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        eventId: Number(approvedReview.eventId),
        leaderType: 'PQC',
        reviewStatus: 'APPROVED',
        reviewRemark: `M6 AC-D34 duplicate terminal review negative path; expect ${expectedErrorKey}.`
      })
    }
  )
  if (!result.ok) {
    return {
      key: 'pqcLeaderDuplicateTerminalReviewBlocked',
      label: 'PQC 组长重复终态复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_TERMINAL',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: approvedReview.eventId,
      reviewId: approvedReview.reviewId,
      expectedErrorKey,
      responseStatus: result.status,
      description: `重复终态复核接口 HTTP 非预期失败：${result.status}`
    }
  }
  if (isBusinessSuccess(result.body)) {
    return {
      key: 'pqcLeaderDuplicateTerminalReviewBlocked',
      label: 'PQC 组长重复终态复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_TERMINAL',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: approvedReview.eventId,
      reviewId: approvedReview.reviewId,
      expectedErrorKey,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body),
      description: '重复终态复核不能返回业务成功；否则同一 PQC 提交会生成多个终态复核。'
    }
  }
  const blockedMessage = responseMessage(result.body)
  if (Number(result.body?.code) !== expectedErrorCode && !blockedMessage.includes('已存在复核终态')) {
    return {
      key: 'pqcLeaderDuplicateTerminalReviewBlocked',
      label: 'PQC 组长重复终态复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_TERMINAL',
      acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
      eventId: approvedReview.eventId,
      reviewId: approvedReview.reviewId,
      expectedErrorKey,
      expectedErrorCode,
      responseCode: result.body?.code,
      responseMessage: blockedMessage,
      description: '重复终态复核虽被拒绝，但错误原因不是正式的重复终态守卫，不能证明 AC-D34 失败路径。'
    }
  }
  return {
    key: 'pqcLeaderDuplicateTerminalReviewBlocked',
    label: 'PQC 组长重复终态复核拒绝',
    roleKey: 'pqcLeader',
    status: 'PASS',
    category: 'E2E_PQC_REVIEW_TERMINAL',
    acceptanceIds: ['AC-M20', 'AC-D34', 'AC-D37'],
    eventId: approvedReview.eventId,
    reviewId: approvedReview.reviewId,
    expectedErrorKey,
    responseCode: result.body?.code,
    responseMessage: blockedMessage,
    endpoint: '/mes/pro/process-pool/team-leader/submission/review'
  }
}

async function preparePqcSelfReviewCandidate(page, config, actionEvidence, reviewerUserId) {
  const browser = page.context().browser()
  if (!browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      reviewerUserId,
      description: '无法创建 PQC 检验员真实页面上下文来准备自我复核候选。'
    }
  }
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const pqcPage = await context.newPage()
  try {
    await login(pqcPage, config, 'pqcInspector', config.roles.pqcInspector)
    const readOnlyEvidence = await verifyPqcActiveOrderReadOnly(pqcPage, config, actionEvidence)
    if (readOnlyEvidence.status !== 'PASS') return readOnlyEvidence
    const regulationEvidence = await verifyPqcRegulationItemsRendered(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence
    ])
    if (regulationEvidence.status !== 'PASS') return regulationEvidence
    const pieceDetailEvidence = await verifyPqcPieceDetailQuantityPrepared(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence
    ])
    if (pieceDetailEvidence.status !== 'PASS') return pieceDetailEvidence
    const employeeEvidence = await switchPqcActualEmployeeToUser(
      pqcPage,
      config,
      [
        ...actionEvidence,
        readOnlyEvidence,
        regulationEvidence,
        pieceDetailEvidence
      ],
      reviewerUserId,
      'pqcSelfReviewActualEmployeeSelected',
      'AC-D35 自我复核候选实际检验人选择',
      ['AC-D35']
    )
    if (employeeEvidence.status !== 'PASS') return employeeEvidence
    const formalSubmissionEvidence = await verifyPqcFormalSubmissionCreatesEvent(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence,
      employeeEvidence
    ])
    if (formalSubmissionEvidence.status !== 'PASS') return formalSubmissionEvidence
    return {
      ...formalSubmissionEvidence,
      key: 'pqcSelfReviewCandidatePrepared',
      label: 'AC-D35 自我复核候选准备',
      acceptanceIds: ['AC-D35'],
      reviewerUserId,
      actualEmployeeId: Number(reviewerUserId),
      sourceActionKey: formalSubmissionEvidence.key
    }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      reviewerUserId,
      description: `通过 PQC 检验员真实页面准备自我复核候选失败：${error.message}`
    }
  } finally {
    await context.close()
  }
}

async function verifyPqcLeaderSelfReviewBlocked(page, config, actionEvidence) {
  const reviewerResolution = await resolveRoleUserId(page, config, 'pqcLeader')
  if (reviewerResolution.status !== 'PASS') {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: reviewerResolution.category,
      acceptanceIds: ['AC-D35'],
      description: reviewerResolution.description
    }
  }
  const reviewerUserId = Number(reviewerResolution.userId)
  const submitDate = localDateString()
  const paginationEvidence = actionEvidence.find((item) =>
    item.key === 'pqcLeaderSubmissionFilterPaginationConsistent' && item.status === 'PASS')
  const candidateEventIds = [
    paginationEvidence?.firstEventId,
    paginationEvidence?.secondEventId
  ].map(Number).filter((value) => Number.isFinite(value) && value > 0)
  let basePage = await loadPqcLeaderSubmissionPage(page, {
    pageNo: 1,
    pageSize: 100,
    submitDate,
    workOrderCode: config.productionOrderCode,
    employeeUserId: reviewerUserId,
    submissionReviewStatus: 'PENDING'
  })
  const matchesSelfReviewRow = (row) =>
    Number(row.actualEmployeeUserId) === reviewerUserId
    && (!row.submissionReviewStatus || row.submissionReviewStatus === 'PENDING')
    && row.processId
    && row.productCode
    && row.inspectionType
    && row.roundNo
  const findSelfReviewRow = (rows, preferredEventIds = []) => {
    const preferredIds = new Set(preferredEventIds
      .map(Number)
      .filter((value) => Number.isFinite(value) && value > 0))
    return rows.find((row) => preferredIds.has(Number(row.id)) && matchesSelfReviewRow(row))
      || rows.find(matchesSelfReviewRow)
  }
  let preparedSelfReviewCandidate
  let selfReviewRow = findSelfReviewRow(basePage.list, candidateEventIds)
  if (!selfReviewRow) {
    preparedSelfReviewCandidate = await preparePqcSelfReviewCandidate(page, config, actionEvidence, reviewerUserId)
    if (preparedSelfReviewCandidate.status === 'PASS') {
      basePage = await loadPqcLeaderSubmissionPage(page, {
        pageNo: 1,
        pageSize: 100,
        submitDate,
        workOrderCode: config.productionOrderCode,
        employeeUserId: reviewerUserId,
        submissionReviewStatus: 'PENDING'
      })
      selfReviewRow = findSelfReviewRow(basePage.list, [preparedSelfReviewCandidate.eventId, ...candidateEventIds])
    }
  }
  if (!selfReviewRow) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      reviewerUserId,
      submitDate,
      candidateEventIds,
      total: basePage.total,
      preparedSelfReviewCandidate,
      description: 'PQC 组长提交看板没有待复核且 actualEmployeeUserId 等于当前 PQC 组长的任务自有事件，不能证明自我复核失败路径。'
    }
  }

  const filters = {
    pageNo: 1,
    pageSize: 20,
    submitDate,
    workOrderCode: selfReviewRow.workOrderCode || config.productionOrderCode,
    employeeUserId: reviewerUserId,
    processId: Number(selfReviewRow.processId),
    productKeyword: selfReviewRow.productCode,
    inspectionType: selfReviewRow.inspectionType,
    roundNo: Number(selfReviewRow.roundNo),
    submissionReviewStatus: 'PENDING'
  }
  const { response } = await searchPqcLeaderSubmissionsOnPage(page, filters)
  if (response.submissionFilterResponseError) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: selfReviewRow.id,
      reviewerUserId,
      description: `真实页面筛选自我复核待判定行时未捕获提交看板响应：${response.submissionFilterResponseError.message}`
    }
  }
  assert.ok(response.ok(), `PQC 组长自我复核筛选 HTTP 失败：${response.status()}`)
  const pendingPage = await loadPqcLeaderSubmissionPage(page, filters)
  const pendingRow = pendingPage.list.find((row) => Number(row.id) === Number(selfReviewRow.id))
  if (!pendingRow) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: selfReviewRow.id,
      reviewerUserId,
      pendingTotal: pendingPage.total,
      description: '真实页面待判定筛选结果中未出现 actualEmployeeUserId 等于复核人的同一事件，不能执行自我复核失败路径。'
    }
  }
  assert.equal(
    Number(pendingRow.actualEmployeeUserId),
    reviewerUserId,
    '自我复核负向用例必须使用 actualEmployeeUserId 等于当前 PQC 组长的正式事件。'
  )

  const expectedErrorKey = 'PRO_PROCESS_POOL_SUBMISSION_REVIEW_SELF_FORBIDDEN'
  const expectedErrorCode = 1040760326
  const result = await fetchWithPageAuth(
    page,
    '/admin-api/mes/pro/process-pool/team-leader/submission/review',
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        eventId: Number(pendingRow.id),
        leaderType: 'PQC',
        reviewStatus: 'APPROVED',
        reviewRemark: `M6 AC-D35 self-review negative path; expect ${expectedErrorKey}.`
      })
    }
  )
  if (!result.ok) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: pendingRow.id,
      reviewerUserId,
      expectedErrorKey,
      responseStatus: result.status,
      description: `自我复核接口 HTTP 非预期失败：${result.status}`
    }
  }
  if (isBusinessSuccess(result.body)) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: pendingRow.id,
      reviewerUserId,
      expectedErrorKey,
      responseCode: result.body?.code,
      responseMessage: responseMessage(result.body),
      description: '自我复核不能返回业务成功；否则同一实际检验人可以确认自己的 PQC 提交。'
    }
  }
  const blockedMessage = responseMessage(result.body)
  if (Number(result.body?.code) !== expectedErrorCode && !blockedMessage.includes('禁止自我复核')) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: pendingRow.id,
      reviewerUserId,
      expectedErrorKey,
      expectedErrorCode,
      responseCode: result.body?.code,
      responseMessage: blockedMessage,
      description: '自我复核虽被拒绝，但错误原因不是正式的自我复核守卫，不能证明 AC-D35 失败路径。'
    }
  }

  const stillPendingPage = await loadPqcLeaderSubmissionPage(page, filters)
  const stillPendingRow = stillPendingPage.list.find((row) => Number(row.id) === Number(pendingRow.id))
  if (!stillPendingRow) {
    return {
      key: 'pqcLeaderSelfReviewBlocked',
      label: 'PQC 组长自我复核拒绝',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_REVIEW_SELF',
      acceptanceIds: ['AC-D35'],
      eventId: pendingRow.id,
      reviewerUserId,
      responseCode: result.body?.code,
      responseMessage: blockedMessage,
      description: '自我复核拒绝后同一事件不再出现在待判定列表，不能证明失败路径未写入终态复核。'
    }
  }

  return {
    key: 'pqcLeaderSelfReviewBlocked',
    label: 'PQC 组长自我复核拒绝',
    roleKey: 'pqcLeader',
    status: 'PASS',
    category: 'E2E_PQC_REVIEW_SELF',
    acceptanceIds: ['AC-D35'],
    eventId: pendingRow.id,
    reviewerUserId,
    actualEmployeeId: Number(pendingRow.actualEmployeeUserId),
    processId: Number(pendingRow.processId),
    productCode: pendingRow.productCode,
    inspectionType: pendingRow.inspectionType,
    roundNo: Number(pendingRow.roundNo),
    submitDate,
    expectedErrorKey,
    responseCode: result.body?.code,
    responseMessage: blockedMessage,
    stillPending: true,
    endpoint: '/mes/pro/process-pool/team-leader/submission/review'
  }
}

function buildPqcRejectedCorrectionPayload(row, reviewId) {
  const beforePayload = parseOriginalPayloadJson(row.originalPayloadJson, 'PQC 退回补正候选')
  const marker = `M6-AC-D30-${Date.now()}`
  const afterPayload = {
    ...beforePayload,
    m6RejectedCorrectionProof: {
      marker,
      sourceEventId: Number(row.id),
      rejectionReviewId: Number(reviewId),
      correctedAt: new Date().toISOString()
    }
  }
  const beforeValue = beforePayload.m6RejectedCorrectionProof
    ? JSON.stringify(beforePayload.m6RejectedCorrectionProof)
    : 'MISSING'
  const afterValue = JSON.stringify(afterPayload.m6RejectedCorrectionProof)
  return {
    marker,
    afterPayloadJson: JSON.stringify(afterPayload, null, 2),
    revisionSignatureSnapshotJson: JSON.stringify({
      signType: 'PQC_REJECTED_CORRECTION',
      source: TASK_ID,
      sourceEventId: Number(row.id),
      signedAt: new Date().toISOString()
    }, null, 2),
    changedFieldsJson: JSON.stringify([{
      fieldCode: 'm6RejectedCorrectionProof',
      fieldName: 'M6退回补正证明',
      beforeValue,
      afterValue,
      affectsQuantityFragment: false,
      originalField: 'REMARK'
    }], null, 2)
  }
}

async function fillDialogFormField(dialog, label, value) {
  const item = dialog.locator('.el-form-item', { hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const control = item.locator('textarea, input').first()
  await control.waitFor({ state: 'visible', timeout: 30000 })
  await control.fill(String(value))
}

function isPqcRejectedCorrectionCandidate(row, excludedEventIds, reviewerUserId) {
  return !excludedEventIds.has(Number(row.id))
    && Number(row.actualEmployeeUserId) !== reviewerUserId
    && row.processId
    && row.productCode
    && row.inspectionType
    && row.roundNo
    && row.originalPayloadJson
}

async function preparePqcRejectedCorrectionCandidate(page, config, actionEvidence) {
  const browser = page.context().browser()
  if (!browser) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_REJECT_CORRECTION',
      description: '无法创建 PQC 检验员真实页面上下文来准备退回补正候选。'
    }
  }
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const pqcPage = await context.newPage()
  try {
    await login(pqcPage, config, 'pqcInspector', config.roles.pqcInspector)
    const readOnlyEvidence = await verifyPqcActiveOrderReadOnly(pqcPage, config, actionEvidence)
    if (readOnlyEvidence.status !== 'PASS') return readOnlyEvidence
    const regulationEvidence = await verifyPqcRegulationItemsRendered(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence
    ])
    if (regulationEvidence.status !== 'PASS') return regulationEvidence
    const pieceDetailEvidence = await verifyPqcPieceDetailQuantityPrepared(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence
    ])
    if (pieceDetailEvidence.status !== 'PASS') return pieceDetailEvidence
    const employeeEvidence = await verifyPqcActualEmployeeSwitch(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence
    ])
    if (employeeEvidence.status !== 'PASS') return employeeEvidence
    const formalSubmissionEvidence = await verifyPqcFormalSubmissionCreatesEvent(pqcPage, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence,
      employeeEvidence
    ])
    if (formalSubmissionEvidence.status !== 'PASS') return formalSubmissionEvidence
    return {
      ...formalSubmissionEvidence,
      key: 'pqcRejectedCorrectionCandidatePrepared',
      label: 'AC-D30 退回补正候选准备',
      acceptanceIds: ['AC-D30'],
      sourceActionKey: formalSubmissionEvidence.key
    }
  } catch (error) {
    return {
      status: 'BLOCKED',
      category: 'E2E_PQC_REJECT_CORRECTION',
      description: `通过 PQC 检验员真实页面准备退回补正候选失败：${error.message}`
    }
  } finally {
    await context.close()
  }
}

async function verifyPqcLeaderRejectedCorrectionChain(page, config, actionEvidence) {
  const reviewerResolution = await resolveRoleUserId(page, config, 'pqcLeader')
  if (reviewerResolution.status !== 'PASS') {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: reviewerResolution.category,
      description: reviewerResolution.description
    }
  }

  const reviewerUserId = Number(reviewerResolution.userId)
  const excludedEventIds = new Set(actionEvidence
    .filter((item) => [
      'pqcLeaderReviewApprovedAndAggregated',
      'pqcLeaderSelfReviewBlocked',
      'pqcProcessInspectionAggregationReadOnly'
    ].includes(item.key))
    .flatMap((item) => [item.eventId, item.approvedEventId, item.pendingEventId])
    .map(Number)
    .filter((value) => Number.isFinite(value) && value > 0))
  const submitDate = localDateString()
  let basePage = await loadPqcLeaderSubmissionPage(page, {
    pageNo: 1,
    pageSize: 100,
    submitDate,
    workOrderCode: config.productionOrderCode,
    submissionReviewStatus: 'PENDING'
  })
  let preparedCandidateEvidence
  let candidate = basePage.list.find((row) =>
    isPqcRejectedCorrectionCandidate(row, excludedEventIds, reviewerUserId))
  if (!candidate) {
    preparedCandidateEvidence = await preparePqcRejectedCorrectionCandidate(page, config, actionEvidence)
    if (preparedCandidateEvidence.status !== 'PASS') {
      return {
        key: 'pqcLeaderRejectedCorrectionChain',
        label: 'PQC 组长退回后补正修订链',
        roleKey: 'pqcLeader',
        status: 'BLOCKED',
        acceptanceIds: ['AC-D30'],
        category: preparedCandidateEvidence.category || 'E2E_PQC_REJECT_CORRECTION',
        reviewerUserId,
        submitDate,
        pendingTotal: basePage.total,
        preparedCandidateStatus: preparedCandidateEvidence.status,
        preparedCandidateDescription: preparedCandidateEvidence.description,
        description: `无法通过 PQC 检验员真实页面准备退回补正候选：${preparedCandidateEvidence.description}`
      }
    }
    basePage = await loadPqcLeaderSubmissionPage(page, {
      pageNo: 1,
      pageSize: 100,
      submitDate,
      workOrderCode: config.productionOrderCode,
      submissionReviewStatus: 'PENDING'
    })
    candidate = basePage.list.find((row) =>
      Number(row.id) === Number(preparedCandidateEvidence.eventId)
        && isPqcRejectedCorrectionCandidate(row, excludedEventIds, reviewerUserId))
  }
  if (!candidate) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      reviewerUserId,
      submitDate,
      pendingTotal: basePage.total,
      excludedEventIds: [...excludedEventIds],
      preparedEventId: preparedCandidateEvidence?.eventId,
      preparedSubmittedTaskId: preparedCandidateEvidence?.submittedTaskId,
      description: 'PQC 组长提交看板没有可用于退回补正的待判定事件；候选必须来自本工单、具备原始 payload，且实际检验人不能等于当前复核人。'
    }
  }

  const correctionSelector = `[data-team-leader-correction-event-id="${candidate.id}"]`
  const pendingFilters = {
    pageNo: 1,
    pageSize: 20,
    submitDate,
    workOrderCode: candidate.workOrderCode || config.productionOrderCode,
    employeeUserId: Number(candidate.actualEmployeeUserId),
    processId: Number(candidate.processId),
    productKeyword: candidate.productCode,
    inspectionType: candidate.inspectionType,
    roundNo: Number(candidate.roundNo),
    submissionReviewStatus: 'PENDING'
  }
  const { section, response } = await searchPqcLeaderSubmissionsOnPage(page, pendingFilters)
  if (response.submissionFilterResponseError) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      description: `真实页面定位退回补正候选时未捕获提交看板响应：${response.submissionFilterResponseError.message}`
    }
  }
  assert.ok(response.ok(), `PQC 退回补正候选筛选 HTTP 失败：${response.status()}`)
  const pendingPage = await loadPqcLeaderSubmissionPage(page, pendingFilters)
  const pendingRow = pendingPage.list.find((row) => Number(row.id) === Number(candidate.id))
  if (!pendingRow) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      pendingTotal: pendingPage.total,
      description: '真实页面待判定筛选结果中未出现退回补正候选事件，不能执行退回和页面修正。'
    }
  }

  const rejectedReviewPayload = { reviewStatus: 'REJECTED' }
  const rejectionReason = `M6 AC-D30 rejected correction chain ${Date.now()}`
  const reviewButton = section.locator(`[data-team-leader-review-event-id="${candidate.id}"]`).first()
  const reviewButtonVisible = await reviewButton
    .waitFor({ state: 'visible', timeout: 30000 })
    .then(() => true)
    .catch((error) => ({ reviewButtonError: error }))
  if (reviewButtonVisible.reviewButtonError) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      pendingTotal: pendingPage.total,
      processId: pendingRow.processId,
      productCode: pendingRow.productCode,
      inspectionType: pendingRow.inspectionType,
      roundNo: pendingRow.roundNo,
      description: `PQC 组长待判定读模型包含退回候选，但真实页面未渲染复核按钮 data-team-leader-review-event-id=${candidate.id}：${reviewButtonVisible.reviewButtonError.message}`
    }
  }
  await reviewButton.click()
  const reviewDialog = page.locator('.el-dialog:visible', { hasText: '复核员工提交' }).first()
  await reviewDialog.waitFor({ state: 'visible', timeout: 30000 })
  await selectElementPlusOption(page, '.el-dialog:visible .el-select', reviewStatusLabel(rejectedReviewPayload.reviewStatus))
  await reviewDialog.locator('textarea').first().fill(rejectionReason)
  const reviewResponsePromise = page.waitForResponse((reviewResponse) =>
    reviewResponse.url().includes('/mes/pro/process-pool/team-leader/submission/review')
      && reviewResponse.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ reviewResponseError: error }))
  await reviewDialog.getByRole('button', { name: '提交复核' }).click()
  const reviewResponse = await reviewResponsePromise
  if (reviewResponse.reviewResponseError) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      description: `提交 PQC 退回复核后未捕获正式复核接口响应：${reviewResponse.reviewResponseError.message}`
    }
  }
  assert.ok(reviewResponse.ok(), `PQC 退回复核 HTTP 失败：${reviewResponse.status()}`)
  const reviewBody = await reviewResponse.json()
  if (!isBusinessSuccess(reviewBody)) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      responseCode: reviewBody.code,
      responseMessage: responseMessage(reviewBody),
      description: `PQC 退回复核业务失败：${responseMessage(reviewBody)}`
    }
  }
  const rejectionReviewId = Number(reviewBody.data)
  assert.ok(Number.isFinite(rejectionReviewId) && rejectionReviewId > 0, 'PQC 退回复核必须返回正式 reviewId。')

  const rejectedFilters = {
    ...pendingFilters,
    submissionReviewStatus: 'REJECTED'
  }
  await searchPqcLeaderSubmissionsOnPage(page, rejectedFilters)
  const rejectedPage = await loadPqcLeaderSubmissionPage(page, rejectedFilters)
  const rejectedRow = rejectedPage.list.find((row) => Number(row.id) === Number(candidate.id))
  if (!rejectedRow) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      reviewId: rejectionReviewId,
      rejectedTotal: rejectedPage.total,
      description: '退回复核成功后同一事件未进入不正确筛选结果，不能证明退回原因留在正式读模型。'
    }
  }
  assert.equal(rejectedRow.submissionReviewStatus, 'REJECTED', '退回后读模型必须显示 REJECTED。')
  assert.ok(
    includesText(rejectedRow.submissionReviewRemark, rejectionReason),
    '退回原因必须保留在 PQC 组长提交看板读模型。'
  )

  const correctionPayload = buildPqcRejectedCorrectionPayload(rejectedRow, rejectionReviewId)
  const modifiedByUserId = Number(rejectedRow.actualEmployeeUserId)
  assert.ok(Number.isFinite(modifiedByUserId) && modifiedByUserId > 0, '退回补正必须记录正式修改人用户 ID。')
  const signatureResolution = await resolveUnusedPqcSignatureId(page, config, 'pqcInspector')
  if (signatureResolution.status !== 'PASS') {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: signatureResolution.category,
      eventId: candidate.id,
      reviewId: rejectionReviewId,
      candidateSignatureIds: signatureResolution.candidateSignatureIds,
      usedSignatureIds: signatureResolution.usedSignatureIds,
      description: signatureResolution.description
    }
  }
  const revisionSignatureId = signatureResolution.signatureId
  const correctionButton = section.locator(correctionSelector).first()
  const correctionButtonVisible = await correctionButton
    .waitFor({ state: 'visible', timeout: 30000 })
    .then(() => true)
    .catch((error) => ({ correctionButtonError: error }))
  if (correctionButtonVisible.correctionButtonError) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      reviewId: rejectionReviewId,
      revisionSignatureId,
      rejectedTotal: rejectedPage.total,
      description: `PQC 组长不正确读模型包含退回事件，但真实页面未渲染补正按钮 ${correctionSelector}：${correctionButtonVisible.correctionButtonError.message}`
    }
  }
  await correctionButton.click()
  const correctionDialog = page.locator('.el-dialog:visible', { hasText: '修正不正确内容' }).first()
  await correctionDialog.waitFor({ state: 'visible', timeout: 30000 })
  await fillDialogFormField(correctionDialog, '修改原因', `M6 AC-D30 rejected correction: ${correctionPayload.marker}`)
  await fillDialogFormField(correctionDialog, '修改人用户ID', modifiedByUserId)
  await fillDialogFormField(correctionDialog, '修正签名ID', revisionSignatureId)
  await fillDialogFormField(correctionDialog, '签名用户ID', modifiedByUserId)
  await fillDialogFormField(correctionDialog, '修改后payload JSON', correctionPayload.afterPayloadJson)
  await fillDialogFormField(correctionDialog, '修正签名快照JSON', correctionPayload.revisionSignatureSnapshotJson)
  await fillDialogFormField(correctionDialog, '字段变更JSON', correctionPayload.changedFieldsJson)

  const revisionResponsePromise = page.waitForResponse((revisionResponse) =>
    revisionResponse.url().includes('/mes/pro/process-pool/event-revision/update-original')
      && revisionResponse.request().method() === 'POST'
  , { timeout: 30000 }).catch((error) => ({ revisionResponseError: error }))
  await correctionDialog.getByRole('button', { name: '提交修正并记录日志' }).click()
  const revisionResponse = await revisionResponsePromise
  if (revisionResponse.revisionResponseError) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      reviewId: rejectionReviewId,
      revisionSignatureId,
      description: `提交退回补正后未捕获 update-original 响应：${revisionResponse.revisionResponseError.message}`
    }
  }
  assert.ok(revisionResponse.ok(), `PQC 退回补正 update-original HTTP 失败：${revisionResponse.status()}`)
  const revisionBody = await revisionResponse.json()
  if (!isBusinessSuccess(revisionBody)) {
    return {
      key: 'pqcLeaderRejectedCorrectionChain',
      label: 'PQC 组长退回后补正修订链',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      acceptanceIds: ['AC-D30'],
      category: 'E2E_PQC_REJECT_CORRECTION',
      eventId: candidate.id,
      reviewId: rejectionReviewId,
      revisionSignatureId,
      responseCode: revisionBody.code,
      responseMessage: responseMessage(revisionBody),
      description: `PQC 退回补正 update-original 业务失败：${responseMessage(revisionBody)}`
    }
  }
  const revisionId = Number(revisionBody.data)
  assert.ok(Number.isFinite(revisionId) && revisionId > 0, 'PQC 退回补正必须返回正式 revisionId。')

  const correctedPage = await loadPqcLeaderSubmissionPage(page, rejectedFilters)
  const correctedRow = correctedPage.list.find((row) => Number(row.id) === Number(candidate.id))
  assert.ok(correctedRow, '退回补正后仍必须能按 REJECTED 读模型查回同一事件。')
  const correctedPayload = parseOriginalPayloadJson(correctedRow.originalPayloadJson, 'PQC 退回补正后读模型')
  assert.equal(
    correctedPayload.m6RejectedCorrectionProof?.marker,
    correctionPayload.marker,
    '退回补正后读模型必须返回修正后的 afterPayload。'
  )
  assert.ok(
    includesText(correctedRow.submissionReviewRemark, rejectionReason),
    '退回补正后仍必须保留原退回原因。'
  )
  assert.ok(
    includesText(correctedRow.modificationHistorySummary, '原始记录已修改'),
    '退回补正后读模型必须返回 modificationHistorySummary 修改摘要。'
  )

  return {
    key: 'pqcLeaderRejectedCorrectionChain',
    label: 'PQC 组长退回后补正修订链',
    roleKey: 'pqcLeader',
    status: 'PASS',
    acceptanceIds: ['AC-D30'],
    eventId: candidate.id,
    reviewId: rejectionReviewId,
    revisionId,
    revisionSignatureId,
    modifiedByUserId,
    preparedEventId: preparedCandidateEvidence?.eventId,
    preparedSubmittedTaskId: preparedCandidateEvidence?.submittedTaskId,
    preparedSignatureId: preparedCandidateEvidence?.signatureId,
    correctionMarker: correctionPayload.marker,
    rejectionReason,
    modificationHistorySummary: correctedRow.modificationHistorySummary,
    endpoint: '/mes/pro/process-pool/event-revision/update-original',
    category: 'E2E_PQC_REJECT_CORRECTION'
  }
}

async function verifyPqcProcessInspectionAggregationReadOnly(page, config, actionEvidence) {
  const approvedReview = actionEvidence.find((item) =>
    item.key === 'pqcLeaderReviewApprovedAndAggregated' && item.status === 'PASS')
  const selfReview = actionEvidence.find((item) =>
    item.key === 'pqcLeaderSelfReviewBlocked' && item.status === 'PASS')
  const formalSubmission = actionEvidence.find((item) =>
    item.key === 'pqcFormalSubmissionCreated'
    && item.status === 'PASS'
    && Number(item.eventId) === Number(approvedReview?.eventId))
  if (!approvedReview?.eventId || !approvedReview?.reviewId || !formalSubmission) {
    return {
      key: 'pqcProcessInspectionAggregationReadOnly',
      label: '过程检验汇集只读核验',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_AGGREGATION_READONLY',
      acceptanceIds: ['AC-M21', 'AC-D37'],
      description: '过程检验汇集只读核验前缺少本轮已确认 PQC 复核和正式提交事件证据，不能用旧数据替代。'
    }
  }
  if (!selfReview?.eventId || !selfReview?.stillPending) {
    return {
      key: 'pqcProcessInspectionAggregationReadOnly',
      label: '过程检验汇集只读核验',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_AGGREGATION_READONLY',
      acceptanceIds: ['AC-M21', 'AC-D37'],
      approvedEventId: approvedReview.eventId,
      reviewId: approvedReview.reviewId,
      description: '缺少自我复核被拒且保持待判定的真实事件，不能证明未确认事件不会被过程检验汇集。'
    }
  }

  const approvedFilters = {
    pageNo: 1,
    pageSize: 20,
    submitDate: approvedReview.submitDate || formalSubmission.submitDate || localDateString(),
    workOrderCode: config.productionOrderCode,
    employeeUserId: Number(formalSubmission.actualEmployeeId),
    processId: Number(formalSubmission.processId),
    productKeyword: formalSubmission.productCode,
    inspectionType: formalSubmission.inspectionType,
    roundNo: Number(formalSubmission.roundNo),
    submissionReviewStatus: 'APPROVED'
  }
  const approvedPage = await loadPqcLeaderSubmissionPage(page, approvedFilters)
  const approvedRow = approvedPage.list.find((row) => Number(row.id) === Number(approvedReview.eventId))
  if (!approvedRow) {
    return {
      key: 'pqcProcessInspectionAggregationReadOnly',
      label: '过程检验汇集只读核验',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_AGGREGATION_READONLY',
      acceptanceIds: ['AC-M21', 'AC-D37'],
      approvedEventId: approvedReview.eventId,
      reviewId: approvedReview.reviewId,
      approvedTotal: approvedPage.total,
      description: '只读查询 APPROVED 状态时未返回本轮已确认事件，不能证明汇集状态进入正式读模型。'
    }
  }
  assert.equal(
    approvedRow.processInspectionAggregationStatus,
    'AGGREGATED',
    '已确认 PQC 事件只读读模型必须显示过程检验汇集状态 AGGREGATED。'
  )
  assert.equal(
    Number(approvedRow.processInspectionReviewId),
    Number(approvedReview.reviewId),
    '已汇集过程检验只读读模型必须绑定触发汇集的正式 reviewId。'
  )
  assert.ok(approvedRow.processInspectionAggregatedAt, '已汇集过程检验只读读模型必须返回汇集时间。')

  const pendingFilters = {
    pageNo: 1,
    pageSize: 20,
    submitDate: selfReview.submitDate || localDateString(),
    workOrderCode: config.productionOrderCode,
    employeeUserId: Number(selfReview.actualEmployeeId),
    processId: Number(selfReview.processId),
    productKeyword: selfReview.productCode,
    inspectionType: selfReview.inspectionType,
    roundNo: Number(selfReview.roundNo),
    submissionReviewStatus: 'PENDING'
  }
  const pendingPage = await loadPqcLeaderSubmissionPage(page, pendingFilters)
  const pendingRow = pendingPage.list.find((row) => Number(row.id) === Number(selfReview.eventId))
  if (!pendingRow) {
    return {
      key: 'pqcProcessInspectionAggregationReadOnly',
      label: '过程检验汇集只读核验',
      roleKey: 'pqcLeader',
      status: 'BLOCKED',
      category: 'E2E_PQC_AGGREGATION_READONLY',
      acceptanceIds: ['AC-M21', 'AC-D37'],
      approvedEventId: approvedReview.eventId,
      pendingEventId: selfReview.eventId,
      pendingTotal: pendingPage.total,
      description: '只读查询 PENDING 状态时未返回自我复核被拒的事件，不能证明未确认事件未被汇集。'
    }
  }
  assert.notEqual(
    pendingRow.processInspectionAggregationStatus,
    'AGGREGATED',
    '未确认 PQC 事件不能被只读读模型标记为过程检验已汇集。'
  )
  assert.ok(!pendingRow.processInspectionReviewId, '未确认 PQC 事件不能绑定过程检验汇集 reviewId。')

  return {
    key: 'pqcProcessInspectionAggregationReadOnly',
    label: '过程检验汇集只读核验',
    roleKey: 'pqcLeader',
    status: 'PASS',
    category: 'E2E_PQC_AGGREGATION_READONLY',
    acceptanceIds: ['AC-M21', 'AC-D37'],
    approvedEventId: approvedReview.eventId,
    pendingEventId: selfReview.eventId,
    reviewId: approvedReview.reviewId,
    processInspectionAggregationStatus: approvedRow.processInspectionAggregationStatus,
    pendingProcessInspectionAggregationStatus: pendingRow.processInspectionAggregationStatus || 'PENDING',
    endpoint: '/mes/pro/process-pool/team-leader/submission/page'
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
      key: 'activeOrderCleanupCompleted',
      label: '活跃订单最终清理闭环',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_CLEANUP',
      acceptanceIds: ['AC-M04'],
      description: '清理核验前缺少生产组长 joinActiveOrder 动作证据，不能判断任务夹具是否可安全删除。'
    }
  }

  await page.goto(new URL('/mes/pro/process-pool/team-leader', config.frontendUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const section = page.locator('[data-team-leader-active-order-config]').first()
  await section.waitFor({ state: 'visible', timeout: 60000 })
  const rows = await reloadActiveOrderRows(page)
  const activeOrder = rows.find((row) => Number(row.id) === Number(joinEvidence.activeOrderId))
  assert.ok(activeOrder, '清理闭环必须能重新定位本轮 activeOrderId。')
  assert.equal(Number(activeOrder.workOrderId), Number(config.workOrderId), '清理闭环定位到的 activeOrder 必须属于任务工单。')
  assert.equal(Number(activeOrder.routeId), Number(config.routeId), '清理闭环定位到的 activeOrder 必须属于任务路线。')
  assert.equal(Number(activeOrder.routeVersionId), Number(config.routeVersionId), '清理闭环定位到的 activeOrder 必须属于任务路线版本。')
  await fillFormItemForAction(section, '移出活跃订单', '活跃记录ID', joinEvidence.activeOrderId)
  const listResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/process-pool/team-leader/active-order/list')
      && response.request().method() === 'GET'
  , { timeout: 30000 }).catch((error) => ({ activeOrderListResponseError: error }))
  const removeResult = await clickButtonAndWaitForSuccess(
    section,
    '移出活跃订单',
    '/mes/pro/process-pool/team-leader/active-order/remove'
  )
  const listResponse = await listResponsePromise
  if (listResponse.activeOrderListResponseError) {
    failFast(`移出活跃订单后未捕获到活跃订单列表刷新响应：${listResponse.activeOrderListResponseError.message}`, [{
      key: 'activeOrderCleanupListResponseError',
      category: 'E2E_CLEANUP',
      description: '真实页面移出活跃订单后必须刷新列表并证明本轮 activeOrderId 不再处于 ACTIVE 状态。'
    }])
  }
  assert.ok(listResponse.ok(), `移出活跃订单后列表刷新 HTTP 失败：${listResponse.status()}`)
  const listBody = await listResponse.json()
  assert.equal(listBody.code, 0, `移出活跃订单后列表刷新业务失败：${listBody.msg || listBody.message || 'unknown'}`)
  const refreshedRows = Array.isArray(listBody.data) ? listBody.data : []
  assert.ok(
    !refreshedRows.some((row) => Number(row.id) === Number(joinEvidence.activeOrderId)),
    '移出活跃订单后，本轮 activeOrderId 不得继续出现在 ACTIVE 列表。'
  )

  return {
    key: 'activeOrderCleanupCompleted',
    label: '活跃订单最终清理闭环',
    roleKey: 'productionLeader',
    status: 'PASS',
    category: 'E2E_CLEANUP',
    acceptanceIds: ['AC-M04'],
    activeOrderId: joinEvidence.activeOrderId,
    workOrderId: config.workOrderId,
    routeId: config.routeId,
    routeVersionId: config.routeVersionId,
    cleanupWindow: 'AFTER_ALL_ROLE_ACTIONS',
    removeResult,
    refreshedActiveOrderCount: refreshedRows.length
  }
}

async function runFinalActiveOrderCleanup(browser, config, actionEvidence) {
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  try {
    await login(page, config, 'productionLeader', config.roles.productionLeader)
    return await verifyActiveOrderCleanupTraceability(page, config, joinEvidence)
  } finally {
    await context.close()
  }
}

async function closeVisibleEdhrReleaseDrawer(page) {
  const closeButton = page.locator('.el-drawer:visible .el-drawer__close-btn').first()
  await closeButton.waitFor({ state: 'visible', timeout: 10000 })
  await closeButton.click()
  await page.locator('.el-drawer:visible').waitFor({ state: 'hidden', timeout: 10000 }).catch(() => undefined)
}

function buildStructuredBlockerEvidence(key, label, roleKey, category, acceptanceIds, description, extra = {}) {
  return {
    key,
    label,
    roleKey,
    status: 'BLOCKED',
    category,
    acceptanceIds,
    description,
    ...extra
  }
}

async function prepareEdhrReleaseBatchExecutionViaRealPage(page, config) {
  const evidenceKey = 'edhrReleasePreparedViaBatchExecutionPage'
  const acceptanceIds = ['AC-M22', 'AC-M23']
  const browser = page.context().browser()
  if (!browser) {
    return buildStructuredBlockerEvidence(
      evidenceKey,
      'eDHR 放行批次准备真实页面动作',
      'productionLeader',
      'E2E_RELEASE_TRACEABILITY_PREP',
      acceptanceIds,
      '无法创建独立浏览器上下文，不能通过生产组长真实页面打开/创建目标批次并执行放行预检。'
    )
  }

  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const leaderPage = await context.newPage()
  try {
    await login(leaderPage, config, 'productionLeader', config.roles.productionLeader)
    const batchPageUrl =
      `${config.frontendUrl.replace(/\/$/, '')}/mes/pro/feedback/edhr-batch-execution?prefillWorkOrderCode=${encodeURIComponent(config.productionOrderCode)}`
    await leaderPage.goto(batchPageUrl, { waitUntil: 'domcontentloaded', timeout: 90000 })
    await leaderPage.locator('.edhr-batch-page').waitFor({ state: 'visible', timeout: 60000 })
    const dialog = leaderPage.locator('.el-dialog:visible').filter({ hasText: '打开或创建 eDHR 批次执行' }).first()
    const autoOpenedDialog = await dialog.waitFor({ state: 'visible', timeout: 5000 }).then(() => true).catch(() => false)
    if (!autoOpenedDialog) {
      await leaderPage.getByRole('button', { name: '打开/创建' }).first().click()
    }
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    const routeItem = dialog.locator('.el-form-item', { hasText: '工艺路线' }).first()
    await routeItem.locator('.el-select').click()
    const routeOption = leaderPage
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: `ID ${config.routeId}` })
      .first()
    await routeOption.waitFor({ state: 'visible', timeout: 30000 })
    await routeOption.click()

    const batchCodeInput = dialog.locator('.el-form-item', { hasText: '批次号' }).locator('input').first()
    await batchCodeInput.waitFor({ state: 'visible', timeout: 30000 })
    const batchCode = (await batchCodeInput.inputValue()).trim()
    if (!batchCode) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        '目标工单真实页面未预填批次号，不能猜填 batchCode 创建 eDHR 批次执行。',
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode }
      )
    }

    const openResponsePromise = leaderPage.waitForResponse((response) =>
      response.url().includes('/mes/pro/edhr-batch-execution/open-or-create')
        && response.request().method() === 'POST'
    , { timeout: 60000 }).catch((error) => ({ openResponseError: error }))
    await dialog.getByRole('button', { name: '确 认' }).click()
    const openResponse = await openResponsePromise
    if (openResponse.openResponseError) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        `生产组长页面确认打开/创建批次后未捕获正式响应：${openResponse.openResponseError.message}`,
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchCode }
      )
    }
    assert.ok(openResponse.ok(), `打开/创建 eDHR 批次执行 HTTP 失败：${openResponse.status()}`)
    const openBody = await openResponse.json()
    if (!isBusinessSuccess(openBody)) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        `生产组长页面打开/创建 eDHR 批次执行业务失败：${responseMessage(openBody)}`,
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchCode }
      )
    }

    const batchExecution = openBody.data || {}
    const batchExecutionId = Number(batchExecution.id || batchExecution.batchExecutionId)
    if (!Number.isFinite(batchExecutionId) || batchExecutionId <= 0) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        '生产组长页面打开/创建成功响应缺少正式 batchExecutionId，不能继续执行放行预检。',
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchCode }
      )
    }

    const detailUrl = new URL('/mes/pro/feedback/edhr-batch-execution/detail', config.frontendUrl)
    detailUrl.searchParams.set('id', String(batchExecutionId))
    detailUrl.searchParams.set('focus', 'precheck')
    await leaderPage.goto(detailUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 90000 })
    const precheckWorkspace = leaderPage.locator('[aria-label="放行预检工作区"]').first()
    await precheckWorkspace.waitFor({ state: 'visible', timeout: 90000 })
    const precheckButton = precheckWorkspace.getByRole('button', { name: '预检' }).first()
    await precheckButton.waitFor({ state: 'visible', timeout: 30000 })
    if (await precheckButton.isDisabled()) {
      const precheckSummary = (await precheckWorkspace.innerText()).replace(/\s+/g, ' ').trim().slice(0, 500)
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        '目标批次详情页放行预检按钮处于禁用状态，不能通过正式页面形成放行事务。',
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchExecutionId, batchCode, precheckSummary }
      )
    }

    const precheckResponsePromise = leaderPage.waitForResponse((response) =>
      response.url().includes('/mes/pro/edhr-release/precheck')
        && response.request().method() === 'POST'
    , { timeout: 60000 }).catch((error) => ({ precheckResponseError: error }))
    await precheckButton.click()
    const precheckResponse = await precheckResponsePromise
    if (precheckResponse.precheckResponseError) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        `生产组长页面执行放行预检后未捕获正式响应：${precheckResponse.precheckResponseError.message}`,
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchExecutionId, batchCode }
      )
    }
    assert.ok(precheckResponse.ok(), `放行预检 HTTP 失败：${precheckResponse.status()}`)
    const precheckBody = await precheckResponse.json()
    if (!isBusinessSuccess(precheckBody)) {
      return buildStructuredBlockerEvidence(
        evidenceKey,
        'eDHR 放行批次准备真实页面动作',
        'productionLeader',
        'E2E_RELEASE_TRACEABILITY_PREP',
        acceptanceIds,
        `生产组长页面放行预检业务失败：${responseMessage(precheckBody)}`,
        { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode, batchExecutionId, batchCode }
      )
    }
    const release = precheckBody.data || {}
    return {
      key: evidenceKey,
      label: 'eDHR 放行批次准备真实页面动作',
      roleKey: 'productionLeader',
      status: 'PASS',
      category: 'E2E_RELEASE_TRACEABILITY_PREP',
      acceptanceIds,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      batchExecutionId,
      batchExecutionCode: batchExecution.batchExecutionCode,
      batchCode,
      releaseTransactionId: release.releaseTransactionId,
      releaseStatus: release.releaseStatus,
      precheckSummary: release.precheckSummary || '',
      endpoints: [
        '/mes/pro/edhr-batch-execution/open-or-create',
        '/mes/pro/edhr-release/precheck'
      ]
    }
  } catch (error) {
    return buildStructuredBlockerEvidence(
      evidenceKey,
      'eDHR 放行批次准备真实页面动作',
      'productionLeader',
      'E2E_RELEASE_TRACEABILITY_PREP',
      acceptanceIds,
      `生产组长真实页面准备 eDHR 放行批次失败：${error.message}`,
      { workOrderId: config.workOrderId, workOrderCode: config.productionOrderCode }
    )
  } finally {
    await context.close()
  }
}

async function verifyEdhrReleaseTraceabilityReadOnly(page, config) {
  const evidenceKey = 'edhrReleaseTraceabilityReadOnly'
  const acceptanceIds = ['AC-M22', 'AC-M23']
  const mutationRequests = []
  const trackMutationRequest = (request) => {
    if (!['POST', 'PUT', 'DELETE'].includes(request.method())) return
    const url = request.url()
    if (url.includes('/mes/pro/edhr-release/')) {
      mutationRequests.push({
        method: request.method(),
        endpoint: new URL(url).pathname
      })
    }
  }
  page.on('request', trackMutationRequest)
  try {
    await page.goto(new URL('/mes/pro/feedback/edhr-release', config.frontendUrl).toString(), {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    await page.locator('.edhr-release-page__title').filter({ hasText: '电子批记录放行追溯' }).first()
      .waitFor({ state: 'visible', timeout: 60000 })

    const toolbar = page.locator('.edhr-release-page__toolbar').first()
    await fillFormItem(toolbar, '工单号', config.productionOrderCode)
    const releaseResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/mes/pro/edhr-release/page')
        && response.request().method() === 'GET'
    , { timeout: 30000 }).catch((error) => ({ releaseResponseError: error }))
    await toolbar.getByRole('button', { name: '查询' }).click()
    const releaseResponse = await releaseResponsePromise
    if (releaseResponse.releaseResponseError) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        workOrderCode: config.productionOrderCode,
        description: `放行负责人页面查询未捕获正式放行追溯列表响应：${releaseResponse.releaseResponseError.message}`
      }
    }

    const releasePageData = await parseJsonResponse(releaseResponse, 'eDHR 放行追溯列表查询')
    const releaseRows = Array.isArray(releasePageData.list) ? releasePageData.list : []
    const releaseRow = releaseRows.find((row) =>
      Number(row.workOrderId) === Number(config.workOrderId)
        || String(row.workOrderCode || '') === String(config.productionOrderCode))
    if (!releaseRow) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        workOrderId: config.workOrderId,
        workOrderCode: config.productionOrderCode,
        releaseTotal: Number(releasePageData.total || 0),
        description: '放行追溯页面按任务工单查询后未返回任务批次放行记录，不能证明 AC-M22/AC-M23 真实页面追溯链路。'
      }
    }
    if (!releaseRow.releaseTransactionId) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        workOrderId: config.workOrderId,
        workOrderCode: config.productionOrderCode,
        batchExecutionId: releaseRow.batchExecutionId,
        releaseStatus: releaseRow.releaseStatus,
        description: '任务批次放行记录尚未生成正式 releaseTransactionId；本切片不执行写入型预检，需先通过正式页面动作形成可追溯放行事务。'
      }
    }

    const releaseTransactionId = Number(releaseRow.releaseTransactionId)
    const rowLocator = page
      .locator('.edhr-release-page__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: config.productionOrderCode })
      .first()
    await rowLocator.waitFor({ state: 'visible', timeout: 30000 })

    const eventResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/mes/pro/edhr-release/event/page')
        && response.request().method() === 'GET'
    , { timeout: 30000 }).catch((error) => ({ eventResponseError: error }))
    await rowLocator.getByRole('button', { name: '事务事件' }).click()
    const eventResponse = await eventResponsePromise
    if (eventResponse.eventResponseError) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        releaseTransactionId,
        description: `放行负责人页面打开事务事件抽屉未捕获正式事件响应：${eventResponse.eventResponseError.message}`
      }
    }
    const eventPageData = await parseJsonResponse(eventResponse, 'eDHR 放行事务事件查询')
    await closeVisibleEdhrReleaseDrawer(page)

    const rowLocatorAfterEvent = page
      .locator('.edhr-release-page__table .el-table__body-wrapper tbody tr')
      .filter({ hasText: config.productionOrderCode })
      .first()
    const checkResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/mes/pro/edhr-release/check-item/page')
        && response.request().method() === 'GET'
    , { timeout: 30000 }).catch((error) => ({ checkResponseError: error }))
    await rowLocatorAfterEvent.getByRole('button', { name: '检查项' }).click()
    const checkResponse = await checkResponsePromise
    if (checkResponse.checkResponseError) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        releaseTransactionId,
        description: `放行负责人页面打开检查项抽屉未捕获正式检查项响应：${checkResponse.checkResponseError.message}`
      }
    }
    const checkPageData = await parseJsonResponse(checkResponse, 'eDHR 放行检查项查询')
    await closeVisibleEdhrReleaseDrawer(page)

    const releaseApiPage = await loadEdhrReleasePage(page, {
      pageNo: 1,
      pageSize: 10,
      workOrderCode: config.productionOrderCode
    })
    const releaseApiRow = releaseApiPage.list.find((row) =>
      Number(row.releaseTransactionId) === releaseTransactionId)
    assert.ok(releaseApiRow, '只读 API 核验必须返回同一个放行事务。')
    const checkApiPage = await loadEdhrReleaseCheckItemPage(page, {
      pageNo: 1,
      pageSize: 100,
      releaseTransactionId,
      itemStatus: 'OPEN',
      checkResult: ''
    })
    const eventApiPage = await loadEdhrReleaseEventPage(page, {
      pageNo: 1,
      pageSize: 100,
      releaseTransactionId,
      eventType: ''
    })

    const checkItems = Array.isArray(checkPageData.list) ? checkPageData.list : []
    const eventItems = Array.isArray(eventPageData.list) ? eventPageData.list : []
    if (!checkItems.length || !checkApiPage.list.length) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        releaseTransactionId,
        releaseStatus: releaseRow.releaseStatus,
        description: '放行事务缺少可见检查项，不能证明 DHR/检验/偏差/返工/报废/库存检查来源已进入正式追溯读模型。'
      }
    }
    if (!eventItems.length || !eventApiPage.list.length) {
      return {
        key: evidenceKey,
        label: 'eDHR 放行追溯只读核验',
        roleKey: 'releaseOwner',
        status: 'BLOCKED',
        category: 'E2E_RELEASE_TRACEABILITY',
        acceptanceIds,
        releaseTransactionId,
        releaseStatus: releaseRow.releaseStatus,
        checkItemCount: checkItems.length,
        description: '放行事务缺少可见事务事件，不能证明 AC-M23 终态或预检事件追溯读模型。'
      }
    }
    assert.deepEqual(mutationRequests, [], '放行追溯只读核验不得触发放行写接口。')

    return {
      key: evidenceKey,
      label: 'eDHR 放行追溯只读核验',
      roleKey: 'releaseOwner',
      status: 'PASS',
      category: 'E2E_RELEASE_TRACEABILITY',
      acceptanceIds,
      workOrderId: config.workOrderId,
      workOrderCode: config.productionOrderCode,
      batchExecutionId: releaseRow.batchExecutionId,
      batchExecutionCode: releaseRow.batchExecutionCode,
      releaseTransactionId,
      releaseStatus: releaseRow.releaseStatus,
      precheckSummary: releaseRow.precheckSummary || '',
      blockingCheckCount: Number(releaseRow.blockingCheckCount || 0),
      failedCheckCount: Number(releaseRow.failedCheckCount || 0),
      checkItemCount: checkItems.length,
      eventCount: eventItems.length,
      apiCheckItemCount: checkApiPage.list.length,
      apiEventCount: eventApiPage.list.length,
      checkCodes: [...new Set(checkItems.map((item) => item.checkCode).filter(Boolean))],
      checkResults: [...new Set(checkItems.map((item) => item.checkResult).filter(Boolean))],
      eventTypes: [...new Set(eventItems.map((item) => item.eventType).filter(Boolean))],
      mutationRequestCount: mutationRequests.length,
      endpoints: [
        '/mes/pro/edhr-release/page',
        '/mes/pro/edhr-release/check-item/page',
        '/mes/pro/edhr-release/event/page'
      ]
    }
  } finally {
    page.off('request', trackMutationRequest)
  }
}

function createDailyCloseRequestBudgetTracker(page) {
  const requestBudget = {
    submissionPageRequests: 0,
    activeOrderListRequests: 0,
    submissionDetailRequests: 0,
    observedRequestEndpoints: []
  }
  const trackRequest = (request) => {
    if (request.method() !== 'GET') return
    const url = request.url()
    if (url.includes('/mes/pro/process-pool/team-leader/submission/page')) {
      requestBudget.submissionPageRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/submission/page')
    }
    if (url.includes('/mes/pro/process-pool/team-leader/active-order/list')) {
      requestBudget.activeOrderListRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/active-order/list')
    }
    if (url.includes('/mes/pro/process-pool/team-leader/submission/detail')) {
      requestBudget.submissionDetailRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/submission/detail')
    }
  }
  page.on('request', trackRequest)
  return {
    snapshot: () => ({
      submissionPageRequests: requestBudget.submissionPageRequests,
      activeOrderListRequests: requestBudget.activeOrderListRequests,
      submissionDetailRequests: requestBudget.submissionDetailRequests,
      observedRequestEndpoints: [...requestBudget.observedRequestEndpoints]
    }),
    stop: () => page.off('request', trackRequest)
  }
}

function createPqcPieceDetailRequestBudgetTracker(page) {
  const requestBudget = {
    pieceDetailRequests: 0,
    processSnapshotRequests: 0,
    pqcPersonnelRequests: 0,
    observedRequestEndpoints: []
  }
  const trackRequest = (request) => {
    if (request.method() !== 'GET') return
    const url = request.url()
    if (
      url.includes('/mes/pro/feedback/frontline/device-account/pqc/piece') ||
      url.includes('/mes/pro/feedback/frontline/device-account/pqc/detail') ||
      url.includes('/mes/pro/feedback/frontline/device-account/pqc/inspection-detail')
    ) {
      requestBudget.pieceDetailRequests += 1
      requestBudget.observedRequestEndpoints.push('frontline/pqc/piece-detail')
    }
    if (url.includes('/mes/pro/feedback/frontline/device-account/pqc/active-order/processes')) {
      requestBudget.processSnapshotRequests += 1
      requestBudget.observedRequestEndpoints.push('frontline/pqc/active-order/processes')
    }
    if (url.includes('/mes/pro/feedback/frontline/device-account/pqc/personnel')) {
      requestBudget.pqcPersonnelRequests += 1
      requestBudget.observedRequestEndpoints.push('frontline/pqc/personnel')
    }
  }
  page.on('request', trackRequest)
  return {
    snapshot: () => ({
      pieceDetailRequests: requestBudget.pieceDetailRequests,
      processSnapshotRequests: requestBudget.processSnapshotRequests,
      pqcPersonnelRequests: requestBudget.pqcPersonnelRequests,
      observedRequestEndpoints: [...requestBudget.observedRequestEndpoints]
    }),
    stop: () => page.off('request', trackRequest)
  }
}

function createPqcLeaderSubmissionListRequestBudgetTracker(page) {
  const requestBudget = {
    submissionPageRequests: 0,
    submissionDetailRequests: 0,
    activeOrderListRequests: 0,
    observedRequestEndpoints: []
  }
  const trackRequest = (request) => {
    if (request.method() !== 'GET') return
    const url = request.url()
    if (url.includes('/mes/pro/process-pool/team-leader/submission/page')) {
      requestBudget.submissionPageRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/submission/page')
    }
    if (url.includes('/mes/pro/process-pool/team-leader/submission/detail')) {
      requestBudget.submissionDetailRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/submission/detail')
    }
    if (url.includes('/mes/pro/process-pool/team-leader/active-order/list')) {
      requestBudget.activeOrderListRequests += 1
      requestBudget.observedRequestEndpoints.push('team-leader/active-order/list')
    }
  }
  page.on('request', trackRequest)
  return {
    snapshot: () => ({
      submissionPageRequests: requestBudget.submissionPageRequests,
      submissionDetailRequests: requestBudget.submissionDetailRequests,
      activeOrderListRequests: requestBudget.activeOrderListRequests,
      observedRequestEndpoints: [...requestBudget.observedRequestEndpoints]
    }),
    stop: () => page.off('request', trackRequest)
  }
}

async function verifyDailyClosePerformanceReadOnly(page, config, actionEvidence) {
  const evidenceKey = 'dailyClosePerformanceReadOnly'
  const acceptanceIds = ['AC-D12', 'AC-D38']
  const joinEvidence = actionEvidence.find((item) => item.key === 'joinActiveOrder' && item.status === 'PASS')
  if (!joinEvidence?.activeOrderId) {
    return {
      key: evidenceKey,
      label: '生产组长日结看板只读性能证据',
      roleKey: 'productionLeader',
      status: 'BLOCKED',
      category: 'E2E_PERFORMANCE',
      acceptanceIds,
      description: '日结看板性能证据缺少本轮 activeOrderId，不能证明看板与活跃订单夹具一致。'
    }
  }

  const requestBudgetTracker = createDailyCloseRequestBudgetTracker(page)
  try {
    await page.locator('[data-role-matrix-daily-close]').first().waitFor({ state: 'visible', timeout: 30000 })
    await page.locator('[data-role-matrix-daily-close-summary]').first().waitFor({ state: 'visible', timeout: 30000 })
    const dailyCloseStatusText = (await page.locator('[data-role-matrix-daily-close-status]').first().innerText())
      .replace(/\s+/g, ' ')
      .trim()
    const expectedCardKeys = ['pending-review', 'rejected-review', 'active-orders', 'load-blocker']
    const dailyCloseCards = await page.locator('[data-role-matrix-daily-close-card]').evaluateAll((nodes) =>
      nodes.map((node) => ({
        key: node.getAttribute('data-role-matrix-daily-close-card'),
        label: node.querySelector('.team-leader-workbench__daily-close-label')?.textContent?.replace(/\s+/g, ' ').trim() || '',
        valueText: node.querySelector('.team-leader-workbench__daily-close-value')?.textContent?.replace(/\s+/g, ' ').trim() || '',
        hint: node.querySelector('.team-leader-workbench__daily-close-hint')?.textContent?.replace(/\s+/g, ' ').trim() || ''
      }))
    )
    await page.waitForTimeout(250)
    const requestBudget = requestBudgetTracker.snapshot()
    assert.equal(requestBudget.submissionDetailRequests, 0, '日结看板卡片读取不得触发逐行提交详情请求。')
    assert.equal(requestBudget.submissionPageRequests, 0, '日结看板卡片读取不得额外触发提交分页请求。')
    assert.equal(requestBudget.activeOrderListRequests, 0, '日结看板卡片读取不得额外触发活跃订单列表请求。')
    const observedCardKeys = dailyCloseCards.map((card) => card.key)
    assert.deepEqual(
      [...observedCardKeys].sort(),
      [...expectedCardKeys].sort(),
      '日结看板必须展示待复核、退回、活跃订单和加载阻塞四张正式卡片。'
    )
    assert.equal(new Set(observedCardKeys).size, observedCardKeys.length, '日结看板卡片 key 不得重复。')
    for (const card of dailyCloseCards) {
      assert.ok(card.label, `日结看板 ${card.key} 必须有业务标签。`)
      assert.match(card.valueText, /^\d+$/, `日结看板 ${card.key} 必须展示可解析的数量。`)
    }
    const activeOrderCard = dailyCloseCards.find((card) => card.key === 'active-orders')
    assert.ok(
      Number(activeOrderCard?.valueText || 0) >= 1,
      '日结看板活跃订单卡片必须包含本轮已加入的 activeOrder。'
    )
    assert.ok(['待处理', '可日结', '加载阻塞'].includes(dailyCloseStatusText), `日结看板状态文本不在正式枚举内：${dailyCloseStatusText}`)

    return {
      key: evidenceKey,
      label: '生产组长日结看板只读性能证据',
      roleKey: 'productionLeader',
      status: 'PASS',
      category: 'E2E_PERFORMANCE',
      acceptanceIds,
      activeOrderId: joinEvidence.activeOrderId,
      workOrderId: config.workOrderId,
      dailyCloseStatusText,
      cardCount: dailyCloseCards.length,
      observedCardKeys,
      requestBudget,
      dailyCloseCards
    }
  } finally {
    requestBudgetTracker.stop()
  }
}

async function selectRealFlowTab(page, tabText) {
  if (!tabText) return
  const tab = page.locator('.el-tabs__item').filter({ hasText: tabText }).first()
  if ((await tab.count()) === 0) return
  await tab.waitFor({ state: 'visible', timeout: 60000 })
  const isActive = await tab.evaluate((node) =>
    node.classList.contains('is-active') || node.getAttribute('aria-selected') === 'true'
  ).catch(() => false)
  if (!isActive) {
    await tab.click()
  }
}

async function verifyRealFlowPhase(page, config, phase) {
  await page.goto(new URL(phase.targetPath, config.frontendUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  assert.ok(!page.url().includes('/login'), `${phase.label} 被重定向到登录页，角色权限或会话无效。`)
  const selectorEvidence = []
  const selectorGroups = phase.selectorGroups || [
    {
      tabText: phase.tabText,
      selectors: phase.selectors || []
    }
  ]
  for (const group of selectorGroups) {
    await selectRealFlowTab(page, group.tabText)
    for (const selector of group.selectors) {
      const locator = page.locator(selector).first()
      await locator.waitFor({ state: 'visible', timeout: 60000 })
      selectorEvidence.push(selector)
    }
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
    const admissionEvidence = await verifyScheduleOrderErpCandidateAdmission(page, config)
    await page.goto(new URL(phase.targetPath, config.frontendUrl).toString(), {
      waitUntil: 'domcontentloaded',
      timeout: 90000
    })
    await selectRealFlowTab(page, '班组配置')
    await page.locator('[data-team-leader-active-order-config]').first().waitFor({
      state: 'visible',
      timeout: 60000
    })
    const joinEvidence = await performActiveOrderJoin(page, config)
    const conflictRouteEvidence = await verifyActiveOrderConflictRouteFailure(page, config, joinEvidence)
    const transferTraceEvidence = await verifyActiveOrderTransferTraceReadOnly(page, config, joinEvidence)
    await selectRealFlowTab(page, '报工管理')
    const dailyCloseEvidence = await verifyDailyClosePerformanceReadOnly(page, config, [
      joinEvidence,
      conflictRouteEvidence,
      transferTraceEvidence
    ])
    return [admissionEvidence, joinEvidence, conflictRouteEvidence, transferTraceEvidence, dailyCloseEvidence]
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
    const formalSubmissionEvidence = await verifyPqcFormalSubmissionCreatesEvent(page, config, [
      ...actionEvidence,
      readOnlyEvidence,
      regulationEvidence,
      pieceDetailEvidence,
      employeeEvidence
    ])
    return [readOnlyEvidence, regulationEvidence, pieceDetailEvidence, employeeEvidence, formalSubmissionEvidence]
  }
  if (phase.actionKey === 'verifyPqcLeaderSubmissionFilterPaginationConsistency') {
    await selectRealFlowTab(page, 'PQC管理')
    const paginationEvidence = await verifyPqcLeaderSubmissionFilterPaginationConsistency(page, config, actionEvidence)
    const detailEvidence = await verifyPqcLeaderSubmissionDetailTraceability(page, config, actionEvidence)
    const detailPermissionEvidence = await verifyPqcLeaderSubmissionDetailUnauthorizedBlocked(page, config, actionEvidence)
    const aggregationEvidence = await verifyPqcLeaderReviewApprovalAggregatesProcessInspection(
      page,
      config,
      actionEvidence
    )
    const duplicateTerminalEvidence = await verifyPqcLeaderDuplicateTerminalReviewBlocked(
      page,
      config,
      [...actionEvidence, aggregationEvidence]
    )
    const selfReviewEvidence = await verifyPqcLeaderSelfReviewBlocked(
      page,
      config,
      [...actionEvidence, paginationEvidence]
    )
    const rejectedCorrectionEvidence = await verifyPqcLeaderRejectedCorrectionChain(
      page,
      config,
      [...actionEvidence, aggregationEvidence, selfReviewEvidence]
    )
    const aggregationReadOnlyEvidence = await verifyPqcProcessInspectionAggregationReadOnly(
      page,
      config,
      [...actionEvidence, aggregationEvidence, selfReviewEvidence]
    )
    return [
      paginationEvidence,
      detailEvidence,
      detailPermissionEvidence,
      aggregationEvidence,
      duplicateTerminalEvidence,
      selfReviewEvidence,
      rejectedCorrectionEvidence,
      aggregationReadOnlyEvidence
    ]
  }
  if (phase.actionKey === 'verifyQaRegulationPublishedVersionReadOnly') {
    return verifyQaRegulationPublishedVersionReadOnly(page, config)
  }
  if (phase.actionKey === 'verifyActiveOrderUnauthorizedMutationBlocked') {
    return verifyActiveOrderUnauthorizedMutationBlocked(page, config, actionEvidence)
  }
  if (phase.actionKey === 'verifyEdhrReleaseTraceabilityReadOnly') {
    const preparationEvidence = await prepareEdhrReleaseBatchExecutionViaRealPage(page, config)
    const traceabilityEvidence = await verifyEdhrReleaseTraceabilityReadOnly(page, config)
    return [preparationEvidence, traceabilityEvidence]
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
    actionEvidence.push(await runFinalActiveOrderCleanup(browser, config, actionEvidence))
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
