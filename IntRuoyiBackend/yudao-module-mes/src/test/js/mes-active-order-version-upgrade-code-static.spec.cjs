const fs = require('fs')
const path = require('path')
const assert = require('assert')

const moduleRoot = path.resolve(__dirname, '..', '..', '..')
const mainRoot = path.join(moduleRoot, 'src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'mes')
const controllerPath = path.join(
  mainRoot,
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolTeamLeaderController.java'
)
const servicePath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderVersionUpgradeService.java'
)
const serviceImplPath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderVersionUpgradeServiceImpl.java'
)
const applyResultPath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderVersionUpgradeApplyResult.java'
)
const addReqBoPath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderAddReqBO.java'
)
const activeOrderServiceImplPath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderServiceImpl.java'
)
const batchExecutionMapperPath = path.join(
  mainRoot,
  'dal',
  'mysql',
  'pro',
  'batchrecord',
  'MesProEdhrBatchExecutionMapper.java'
)
const requestDoPath = path.join(
  mainRoot,
  'dal',
  'dataobject',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolActiveOrderVersionUpgradeRequestDO.java'
)
const requestMapperPath = path.join(
  mainRoot,
  'dal',
  'mysql',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolActiveOrderVersionUpgradeRequestMapper.java'
)
const effectExecutorPath = path.join(
  mainRoot,
  'service',
  'pro',
  'processpool',
  'team',
  'MesTeamLeaderActiveOrderVersionUpgradeBusinessApprovalEffectExecutor.java'
)
const activeOrderMapperPath = path.join(
  mainRoot,
  'dal',
  'mysql',
  'pro',
  'processpool',
  'team',
  'MesProcessPoolActiveOrderMapper.java'
)
const reqVoPath = path.join(
  mainRoot,
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'vo',
  'MesTeamLeaderActiveOrderVersionUpgradeSubmitReqVO.java'
)
const respVoPath = path.join(
  mainRoot,
  'controller',
  'admin',
  'pro',
  'processpool',
  'team',
  'vo',
  'MesTeamLeaderActiveOrderVersionUpgradePreviewRespVO.java'
)
const errorPath = path.join(mainRoot, 'enums', 'ErrorCodeConstants.java')
const sqlPath = path.resolve(
  moduleRoot,
  '..',
  'sql',
  'mysql',
  '20260902_mes_active_order_version_upgrade_request.sql'
)

const controller = fs.readFileSync(controllerPath, 'utf8')
const errorCodes = fs.readFileSync(errorPath, 'utf8')
const bpmRoot = path.resolve(moduleRoot, '..', 'yudao-module-bpm', 'src', 'main', 'java', 'cn', 'iocoder', 'yudao', 'module', 'bpm')
const executorRegistryPath = path.join(bpmRoot, 'businessapproval', 'service', 'BusinessApprovalEffectExecutorRegistry.java')
const policyMapperPath = path.join(bpmRoot, 'dal', 'mysql', 'businessapproval', 'BusinessApprovalPolicyMapper.java')
const nativeApprovalTaskProviderPath = path.join(bpmRoot, 'approval', 'service', 'BpmNativeApprovalTaskProvider.java')

assert(fs.existsSync(servicePath), 'backend must declare active-order version-upgrade service contract')
assert(fs.existsSync(serviceImplPath), 'backend must implement active-order version-upgrade service')
assert(fs.existsSync(applyResultPath), 'backend must declare version-upgrade approval apply result')
assert(fs.existsSync(requestDoPath), 'backend must declare version-upgrade request DO')
assert(fs.existsSync(requestMapperPath), 'backend must declare version-upgrade request mapper')
assert(fs.existsSync(effectExecutorPath), 'backend must register a business approval effect executor for active-order version upgrade')
assert(fs.existsSync(sqlPath), 'backend must include version-upgrade request SQL migration')
assert(fs.existsSync(reqVoPath), 'backend must declare version-upgrade submit request VO')
assert(fs.existsSync(respVoPath), 'backend must declare version-upgrade preview response VO')

const service = fs.readFileSync(servicePath, 'utf8')
const serviceImpl = fs.readFileSync(serviceImplPath, 'utf8')
const applyResult = fs.readFileSync(applyResultPath, 'utf8')
const addReqBo = fs.readFileSync(addReqBoPath, 'utf8')
const activeOrderServiceImpl = fs.readFileSync(activeOrderServiceImplPath, 'utf8')
const batchExecutionMapper = fs.readFileSync(batchExecutionMapperPath, 'utf8')
const requestDo = fs.readFileSync(requestDoPath, 'utf8')
const requestMapper = fs.readFileSync(requestMapperPath, 'utf8')
const effectExecutor = fs.readFileSync(effectExecutorPath, 'utf8')
const activeOrderMapper = fs.readFileSync(activeOrderMapperPath, 'utf8')
const sql = fs.readFileSync(sqlPath, 'utf8')
const reqVo = fs.readFileSync(reqVoPath, 'utf8')
const respVo = fs.readFileSync(respVoPath, 'utf8')
const executorRegistry = fs.readFileSync(executorRegistryPath, 'utf8')
const policyMapper = fs.readFileSync(policyMapperPath, 'utf8')
const nativeApprovalTaskProvider = fs.readFileSync(nativeApprovalTaskProviderPath, 'utf8')

assert(
  controller.includes('@GetMapping("/active-order/version-upgrade/preview")') &&
    controller.includes('@PostMapping("/active-order/version-upgrade/submit")') &&
    controller.includes('mes:pro-process-pool-team-leader:version-upgrade'),
  'controller must expose protected preview and submit endpoints for active-order version upgrade'
)
assert(
  service.includes('preview(Long leaderUserId, Long activeOrderId)') &&
    /submit\s*\(\s*Long\s+leaderUserId\s*,\s*MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand\s+command\s*\)/.test(service) &&
    /applyApprovedUpgrade\s*\(\s*Long\s+requestId\s*,\s*Long\s+actorUserId\s*\)/.test(service),
  'service contract must expose preview, submit, and approved-application methods'
)
assert(
  reqVo.includes('private Long activeOrderId') &&
    reqVo.includes('private String idempotencyKey') &&
    reqVo.includes('private String upgradeReason') &&
    reqVo.includes('private Boolean confirmRestartFromBeginning'),
  'submit request must require activeOrderId, idempotencyKey, upgradeReason, and restart confirmation'
)
assert(
  respVo.includes('currentVersions') &&
    respVo.includes('targetVersions') &&
    respVo.includes('allLatestFormalVersions') &&
    respVo.includes('perVersionSelectionAllowed'),
  'preview response must expose current baseline, all-latest target baseline, and no-selection flag'
)
assert(
  serviceImpl.includes('selectActiveByRouteId') &&
    serviceImpl.includes('selectLatestPublishedByRegulationId') &&
    serviceImpl.includes('setAllLatestFormalVersions') &&
    serviceImpl.includes('setPerVersionSelectionAllowed(false)'),
  'service implementation must resolve route and QA targets from authoritative latest formal version sources'
)
assert(
  !serviceImpl.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_APPROVAL_SCHEMA_REQUIRED') &&
    serviceImpl.includes('BusinessApprovalOrchestrator') &&
    serviceImpl.includes('approvalOrchestrator.submit') &&
    serviceImpl.includes('versionUpgradeRequestMapper.insert') &&
    serviceImpl.includes('freezeForVersionUpgrade') &&
    serviceImpl.includes('PENDING_APPROVAL') &&
    serviceImpl.includes('VERSION_UPGRADE_PENDING'),
  'submit must persist a pending approval request and freeze the old active order instead of failing on missing schema'
)
assert(
  requestDo.includes('@TableName("mes_pro_process_pool_active_order_version_upgrade_request")') &&
    requestDo.includes('private Long sourceActiveOrderId') &&
    requestDo.includes('private String currentSnapshotJson') &&
    requestDo.includes('private String targetSnapshotJson') &&
    requestDo.includes('private String snapshotHash') &&
    requestDo.includes('private String idempotencyKey'),
  'request DO must preserve source active order, frozen current/target snapshots, hash, and idempotency key'
)
assert(
  requestMapper.includes('selectByIdempotencyKey') &&
    requestMapper.includes('selectOngoingBySourceActiveOrderId') &&
    requestMapper.includes('selectByIdForUpdate') &&
    requestMapper.includes('markApprovalPending') &&
    requestMapper.includes('markApplied') &&
    requestMapper.includes('markRejectedOrCancelled'),
  'request mapper must support idempotent submit, one ongoing request per source active order, approval locking, process binding, applied status update, and rejected/cancelled cleanup'
)
assert(
  activeOrderMapper.includes('freezeForVersionUpgrade') &&
    activeOrderMapper.includes('releaseVersionUpgradeFreeze') &&
    activeOrderMapper.includes('removePendingVersionUpgradeOrder') &&
    activeOrderMapper.includes("VERSION_UPGRADE_PENDING") &&
    activeOrderMapper.includes("active_status = 'ACTIVE'"),
  'active-order mapper must freeze only the expected ACTIVE row, release freeze on reject/cancel, and later remove only the pending version-upgrade row'
)
assert(
  batchExecutionMapper.includes('voidForVersionUpgrade') &&
    batchExecutionMapper.includes('BATCH_STATUS_VOIDED') &&
    batchExecutionMapper.includes('active_context_key'),
  'batch execution mapper must void old active-context batch execution during approved version upgrade'
)
assert(
  addReqBo.includes('private Boolean forceNewVersionUpgradeOrder') &&
    activeOrderServiceImpl.includes('!Boolean.TRUE.equals(reqBO.getForceNewVersionUpgradeOrder())') &&
    activeOrderServiceImpl.includes('resolveActiveOrderHistory(reqBO, pickList)'),
  'approved version upgrade must force a new active-order row instead of recovering the removed history row'
)
assert(
  serviceImpl.includes('applyApprovedUpgrade') &&
    serviceImpl.includes('selectByIdForUpdate(requestId)') &&
    serviceImpl.includes('cancelActiveTasksByBatch') &&
    serviceImpl.includes('voidForVersionUpgrade') &&
    serviceImpl.includes('removePendingVersionUpgradeOrder') &&
    serviceImpl.includes('forceNewVersionUpgradeOrder(Boolean.TRUE)') &&
    serviceImpl.includes('extractTargetVersionId(request, OBJECT_TYPE_ROUTE)') &&
    serviceImpl.includes('extractTargetVersionId(request, OBJECT_TYPE_QA)') &&
    serviceImpl.includes('targetRouteVersionId(targetRouteVersionId)') &&
    serviceImpl.includes('targetQaRegulationVersionId(targetQaRegulationVersionId)') &&
    serviceImpl.includes('addActiveOrder') &&
    serviceImpl.includes('markApplied'),
  'approved version upgrade must void old batch/tasks, remove old active order, create a new active order from frozen targetSnapshotJson versions, and mark the request applied'
)
assert(
  addReqBo.includes('private Long targetRouteVersionId') &&
    addReqBo.includes('private Long targetQaRegulationVersionId') &&
    activeOrderServiceImpl.includes('resolveVersionUpgradeRouteSource') &&
    activeOrderServiceImpl.includes('resolveVersionUpgradeQaSource') &&
    activeOrderServiceImpl.includes('reqBO.getTargetRouteVersionId()') &&
    activeOrderServiceImpl.includes('reqBO.getTargetQaRegulationVersionId()'),
  'active-order creation must accept and use frozen target route/QA versions for approved version-upgrade restart'
)
assert(
  serviceImpl.includes('markApprovalPending') &&
    serviceImpl.includes('rejectOrCancelApproval') &&
    serviceImpl.includes('releaseVersionUpgradeFreeze'),
  'version-upgrade service must bind the BPM process instance while pending and release the frozen old order when approval is rejected or cancelled'
)
assert(
  effectExecutor.includes('implements BusinessApprovalEffectExecutor') &&
    effectExecutor.includes('EXECUTOR_CODE = "MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART"') &&
    effectExecutor.includes('PROCESS_DEFINITION_KEY = "mes-active-order-version-upgrade-v1"') &&
    effectExecutor.includes('OBJECT_TYPE = "MES_ACTIVE_ORDER"') &&
    effectExecutor.includes('ACTION_CODE = "VERSION_UPGRADE_RESTART"') &&
    effectExecutor.includes('markApprovalPending') &&
    effectExecutor.includes('applyApprovedUpgrade') &&
    effectExecutor.includes('rejectOrCancelApproval'),
  'business approval executor must bind pending BPM state, apply approved restart, and undo freeze on reject/cancel'
)
assert(
  executorRegistry.includes('Map.entry("MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART", "mes-active-order-version-upgrade-v1")') &&
    policyMapper.includes('"MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART"'),
  'BPM business approval registry and policy switch scope must know the active-order version-upgrade executor'
)
assert(
  nativeApprovalTaskProvider.includes('MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART') &&
    nativeApprovalTaskProvider.includes('活跃订单升级重启') &&
    nativeApprovalTaskProvider.includes('sourceActiveOrderId') &&
    nativeApprovalTaskProvider.includes('targetVersionsSummary'),
  'native approval list must render a business title and tags for active-order version-upgrade approvals'
)
assert(
  applyResult.includes('private Long sourceActiveOrderId') &&
    applyResult.includes('private Long targetActiveOrderId') &&
    applyResult.includes('private Long voidedBatchExecutionId') &&
    applyResult.includes('private String requestStatus'),
  'apply result must expose source order, target order, voided batch, and final request status'
)
assert(
  sql.includes('mes_pro_process_pool_active_order_version_upgrade_request') &&
    sql.includes('bpm_business_approval_policy') &&
    sql.includes('mes:pro-process-pool-team-leader:version-upgrade') &&
    sql.includes('活跃订单版本升级') &&
    sql.includes('system_role_menu') &&
    sql.includes('MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART') &&
    sql.includes('mes-active-order-version-upgrade-v1') &&
    sql.includes('current_snapshot_json') &&
    sql.includes('target_snapshot_json') &&
    sql.includes('snapshot_hash') &&
    sql.includes('uk_mes_pp_active_order_upgrade_idempotency') &&
    sql.includes('idx_mes_pp_active_order_upgrade_source_status'),
  'migration must create durable version-upgrade request table with frozen snapshots, hash, idempotency, and source-status index'
)
assert(
  errorCodes.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_ONGOING_EXISTS') &&
    errorCodes.includes('PRO_PROCESS_POOL_ACTIVE_ORDER_VERSION_UPGRADE_FREEZE_CONFLICT'),
  'error codes must include explicit blockers for duplicate ongoing requests and freeze conflicts'
)

console.log('active-order version upgrade backend static contract PASS')
