const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { spawn } = require('node:child_process')

const TASK_ID = '20260803-p0-production-execution-loop-implementation'
const DATA_PREFIX = 'P0-EXEC-'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const FRONTEND_ROOT = path.resolve(WORKSPACE_ROOT, 'IntRuoyiFronted')
const BACKEND_ROOT = path.resolve(WORKSPACE_ROOT, 'IntRuoyiBackend')
const RESULT_DIR = path.resolve(FRONTEND_ROOT, 'test-results', 'p0-production-execution-loop-real')
const EVIDENCE_FILE = path.resolve(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'p0-real-e2e-evidence.md')
const RUNTIME_MIGRATION_VERIFIER_SCRIPT = path.resolve(
  BACKEND_ROOT,
  'script',
  'p0',
  'verify_p0_runtime_migration.py'
)

const TEAM_LEADER_ROUTE = '/mes/pro/process-pool/production-leader'
const PQC_LEADER_ROUTE = '/mes/pro/process-pool/pqc-leader'
const PRODUCTION_FILL_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const PQC_FILL_ROUTE = '/mes/pro/feedback/edhr-batch-pqc-fill'
const TIMELINE_ROUTE = '/mes/pro/process-pool/timeline'

const SYSTEM_AUTH_LOGIN_ENDPOINT = '/system/auth/login'
const SYSTEM_AUTH_GET_PERMISSION_INFO_ENDPOINT = '/system/auth/get-permission-info'
const FRONTLINE_SUBMIT_ENDPOINT = '/mes/pro/feedback/frontline/submit'
const PQC_SUBMIT_ENDPOINT = '/mes/pro/feedback/frontline/device-account/pqc/submit'
const TEAM_LEADER_REVIEW_ENDPOINT = '/mes/pro/process-pool/team-leader/submission/review'
const TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT =
  '/mes/pro/process-pool/team-leader/submission/allocation/confirm'
const PRODUCTION_EXECUTION_TRACE_ENDPOINT =
  '/mes/pro/process-pool/team-leader/production-execution/trace'
const PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE = 1040760315
const TARGET_REQUEST_BOUNDARIES = [
  { label: 'FRONTLINE_SUBMIT_ENDPOINT', endpoint: FRONTLINE_SUBMIT_ENDPOINT },
  { label: 'PQC_SUBMIT_ENDPOINT', endpoint: PQC_SUBMIT_ENDPOINT },
  { label: 'TEAM_LEADER_REVIEW_ENDPOINT', endpoint: TEAM_LEADER_REVIEW_ENDPOINT },
  { label: 'TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT', endpoint: TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT },
  { label: 'PRODUCTION_EXECUTION_TRACE_ENDPOINT', endpoint: PRODUCTION_EXECUTION_TRACE_ENDPOINT }
]

const REQUIRED_ENV = [
  ['P0_FRONTEND_URL', '真实前端入口，例如当前 worktree http://127.0.0.1:8092。'],
  ['P0_BACKEND_URL', '真实后端入口，例如当前 worktree http://127.0.0.1:48092。'],
  ['P0_RUN_ID', '本次真实 E2E 的任务数据 runId，用于生成 P0-EXEC-<runId> 数据前缀。'],
  ['P0_TENANT', '可写测试租户，禁止生产或 admin 基线租户。'],
  ['P0_USERNAME', '拥有一线提交、PQC、班组长复核和批记录追溯路径权限的测试账号。'],
  ['P0_PASSWORD', '测试账号密码，只能通过进程环境注入。'],
  ['P0_WORK_ORDER_ID', '任务自有生产工单 ID。'],
  ['P0_WORK_ORDER_CODE', '任务自有生产工单编码。'],
  ['P0_ROUTE_PROCESS_ID', '正式路线工序 ID。'],
  ['P0_PROCESS_ID', '正式工序 ID。'],
  ['P0_DEVICE_ACCOUNT_ID', '真实设备账号 ID，必须等于当前登录用户，避免设备账号上下文隐式漂移。'],
  ['P0_DEVICE_ID', '真实设备 ID。'],
  ['P0_WORKSTATION_ID', '真实工作站 ID。'],
  ['P0_SIGNATURE_ID', '一线提交真实电子签名 ID。'],
  ['P0_SIGNATURE_EMPLOYEE_ID', '一线签名员工 ID，必须等于实际填写员工。'],
  ['P0_SUBMIT_IDEMPOTENCY_KEY', '一线生产提交幂等键，本次 run 内固定，用于重复提交验证。'],
  ['P0_SUBMIT_QUANTITY', '一线本次提交数量，必须大于 0。'],
  ['P0_CONFIRM_QUANTITY', '生产组长本次 FIFO 确认数量，必须大于 0 且不超过质量可分配数量。'],
  ['P0_PQC_TASK_ID', 'PQC 正式任务 ID。'],
  ['P0_QA_REGULATION_VERSION_ID', 'PQC QA 规程版本 ID。'],
  ['P0_PQC_SIGNATURE_ID', 'PQC 提交真实电子签名 ID。'],
  ['P0_PQC_SIGNATURE_EMPLOYEE_ID', 'PQC 签名员工 ID，必须等于实际 PQC 员工。'],
  ['P0_PQC_IDEMPOTENCY_KEY', 'PQC 检验提交幂等键，本次 run 内固定，用于重复提交验证。'],
  ['P0_PQC_INSPECTION_QUANTITY', 'PQC 检验数量，必须覆盖本次确认数量。'],
  ['P0_PQC_QUALIFIED_QUANTITY', 'PQC 合格数量，必须覆盖本次确认数量。'],
  ['P0_PQC_ALLOCATABLE_QUANTITY', 'PQC 可分配数量，必须覆盖本次确认数量。'],
  ['P0_PQC_REVIEW_SIGNATURE_ID', 'PQC 组长复核真实电子签名 ID，必须与生产组长 FIFO 确认签名分开。'],
  ['P0_PQC_REVIEW_SIGNATURE_EMPLOYEE_ID', 'PQC 组长复核签名员工 ID。'],
  ['P0_REVIEW_SIGNATURE_ID', '班组长复核真实电子签名 ID。'],
  ['P0_REVIEW_SIGNATURE_EMPLOYEE_ID', '班组长复核签名员工 ID。'],
  ['P0_CONFIRM_IDEMPOTENCY_KEY', '生产组长 FIFO 确认幂等键，本次 run 内固定；若后端未暴露请求字段，仅作为重复确认证据前置。'],
  ['P0_BATCH_RECORD_REPORT_ID', '当前工序正式批记录报表 ID。'],
  ['P0_BATCH_RECORD_DEFINITION_ID', '当前工序正式批记录定义 ID，必须来自工序设置逐工序绑定。'],
  ['P0_BATCH_RECORD_VERSION_ID', '当前工序正式批记录版本 ID，必须来自工序设置逐工序绑定。'],
  ['P0_SCHEMA_MIGRATION_ID', '运行态已应用 P0 schema 迁移的版本或变更 ID。'],
  ['P0_MIGRATION_POLICY_EVIDENCE', '本次 run 使用的 release migration policy gate 证据路径。'],
  ['P0_RUNTIME_DB_HOST', '真实运行态 MySQL host，用于浏览器写入前只读核验 P0 schema 迁移。'],
  ['P0_RUNTIME_DB_PORT', '真实运行态 MySQL port，用于浏览器写入前只读核验 P0 schema 迁移。'],
  ['P0_RUNTIME_DB_NAME', '真实运行态 MySQL database，用于浏览器写入前只读核验 P0 schema 迁移。'],
  ['P0_RUNTIME_DB_USER', '真实运行态 MySQL 只读核验用户。'],
  ['P0_RUNTIME_DB_PASSWORD', '真实运行态 MySQL 密码，只能通过进程环境注入且不得写入证据。']
]

const NUMERIC_ENV = [
  'P0_WORK_ORDER_ID',
  'P0_ROUTE_PROCESS_ID',
  'P0_PROCESS_ID',
  'P0_DEVICE_ACCOUNT_ID',
  'P0_DEVICE_ID',
  'P0_WORKSTATION_ID',
  'P0_SIGNATURE_ID',
  'P0_SIGNATURE_EMPLOYEE_ID',
  'P0_SUBMIT_QUANTITY',
  'P0_CONFIRM_QUANTITY',
  'P0_PQC_TASK_ID',
  'P0_QA_REGULATION_VERSION_ID',
  'P0_PQC_SIGNATURE_ID',
  'P0_PQC_SIGNATURE_EMPLOYEE_ID',
  'P0_PQC_INSPECTION_QUANTITY',
  'P0_PQC_QUALIFIED_QUANTITY',
  'P0_PQC_ALLOCATABLE_QUANTITY',
  'P0_PQC_REVIEW_SIGNATURE_ID',
  'P0_PQC_REVIEW_SIGNATURE_EMPLOYEE_ID',
  'P0_REVIEW_SIGNATURE_ID',
  'P0_REVIEW_SIGNATURE_EMPLOYEE_ID',
  'P0_BATCH_RECORD_REPORT_ID',
  'P0_BATCH_RECORD_DEFINITION_ID',
  'P0_BATCH_RECORD_VERSION_ID',
  'P0_RUNTIME_DB_PORT'
]

const CLOSURE_EVIDENCE_REQUIRED_ANSWERS = [
  'who',
  'device',
  'process',
  'quantity',
  'quality',
  'signature',
  'workOrder',
  'review',
  'batchRecord'
]

const CLOSURE_EVIDENCE_MISSING_SOURCE = 'CLOSURE_EVIDENCE_MISSING_SOURCE'

const FORBIDDEN_TENANTS = ['芋道源码', 'yudao', 'prod', 'production', 'admin']

function envValue(key) {
  return (process.env[key] || '').trim()
}

function numberEnv(key) {
  const value = Number(envValue(key))
  return Number.isFinite(value) && value > 0 ? value : undefined
}

function sanitizeUrl(value) {
  return value ? value.replace(/\/+$/, '') : value
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function buildDataPrefix(runId) {
  return runId ? `${DATA_PREFIX}${runId}` : DATA_PREFIX
}

function resolveWorkspacePath(value) {
  return path.isAbsolute(value) ? value : path.resolve(WORKSPACE_ROOT, value)
}

function validateMigrationPolicyEvidence(value) {
  const evidencePath = resolveWorkspacePath(value)
  if (!fs.existsSync(evidencePath)) {
    return {
      key: 'P0_MIGRATION_POLICY_EVIDENCE',
      description: `迁移发布策略证据文件不存在：${value}。`
    }
  }
  const evidence = fs.readFileSync(evidencePath, 'utf8')
  const hasExplicitPass = /\bPASS\b/.test(evidence)
  const hasFailureMarker = /\b(BLOCKED|FAIL|FAILED)\b/.test(evidence)
  if (!hasExplicitPass || hasFailureMarker) {
    return {
      key: 'P0_MIGRATION_POLICY_EVIDENCE_NOT_PASS',
      description: '迁移发布策略证据必须包含明确 PASS 且不得包含 BLOCKED/FAIL/FAILED；浏览器写入前必须阻塞。'
    }
  }
  return undefined
}

function collectConfig() {
  const runId = envValue('P0_RUN_ID')
  const config = {
    frontendUrl: sanitizeUrl(envValue('P0_FRONTEND_URL')),
    backendUrl: sanitizeUrl(envValue('P0_BACKEND_URL')),
    tenant: envValue('P0_TENANT'),
    username: envValue('P0_USERNAME'),
    password: envValue('P0_PASSWORD'),
    runId,
    dataPrefix: buildDataPrefix(runId),
    workOrderId: numberEnv('P0_WORK_ORDER_ID'),
    workOrderCode: envValue('P0_WORK_ORDER_CODE'),
    routeProcessId: numberEnv('P0_ROUTE_PROCESS_ID'),
    processId: numberEnv('P0_PROCESS_ID'),
    deviceAccountId: numberEnv('P0_DEVICE_ACCOUNT_ID'),
    deviceId: numberEnv('P0_DEVICE_ID'),
    signatureId: numberEnv('P0_SIGNATURE_ID'),
    signatureEmployeeId: numberEnv('P0_SIGNATURE_EMPLOYEE_ID'),
    submitIdempotencyKey: envValue('P0_SUBMIT_IDEMPOTENCY_KEY'),
    submitQuantity: numberEnv('P0_SUBMIT_QUANTITY'),
    confirmQuantity: numberEnv('P0_CONFIRM_QUANTITY'),
    pqcTaskId: numberEnv('P0_PQC_TASK_ID'),
    qaRegulationVersionId: numberEnv('P0_QA_REGULATION_VERSION_ID'),
    pqcSignatureId: numberEnv('P0_PQC_SIGNATURE_ID'),
    pqcSignatureEmployeeId: numberEnv('P0_PQC_SIGNATURE_EMPLOYEE_ID'),
    pqcIdempotencyKey: envValue('P0_PQC_IDEMPOTENCY_KEY'),
    pqcInspectionQuantity: numberEnv('P0_PQC_INSPECTION_QUANTITY'),
    pqcQualifiedQuantity: numberEnv('P0_PQC_QUALIFIED_QUANTITY'),
    pqcAllocatableQuantity: numberEnv('P0_PQC_ALLOCATABLE_QUANTITY'),
    pqcReviewSignatureId: numberEnv('P0_PQC_REVIEW_SIGNATURE_ID'),
    pqcReviewSignatureEmployeeId: numberEnv('P0_PQC_REVIEW_SIGNATURE_EMPLOYEE_ID'),
    reviewSignatureId: numberEnv('P0_REVIEW_SIGNATURE_ID'),
    reviewSignatureEmployeeId: numberEnv('P0_REVIEW_SIGNATURE_EMPLOYEE_ID'),
    confirmIdempotencyKey: envValue('P0_CONFIRM_IDEMPOTENCY_KEY'),
    batchRecordReportId: numberEnv('P0_BATCH_RECORD_REPORT_ID'),
    batchRecordDefinitionId: numberEnv('P0_BATCH_RECORD_DEFINITION_ID'),
    batchRecordVersionId: numberEnv('P0_BATCH_RECORD_VERSION_ID'),
    schemaMigrationId: envValue('P0_SCHEMA_MIGRATION_ID'),
    migrationPolicyEvidence: envValue('P0_MIGRATION_POLICY_EVIDENCE'),
    routeId: numberEnv('P0_ROUTE_ID'),
    taskId: numberEnv('P0_TASK_ID'),
    itemId: numberEnv('P0_ITEM_ID'),
    workstationId: numberEnv('P0_WORKSTATION_ID'),
    approveUserId: numberEnv('P0_APPROVE_USER_ID'),
    recordbookId: numberEnv('P0_RECORDBOOK_ID'),
    feedbackType: numberEnv('P0_FEEDBACK_TYPE'),
    runtimeDbHost: envValue('P0_RUNTIME_DB_HOST'),
    runtimeDbPort: numberEnv('P0_RUNTIME_DB_PORT'),
    runtimeDbName: envValue('P0_RUNTIME_DB_NAME'),
    runtimeDbUser: envValue('P0_RUNTIME_DB_USER'),
    runtimeDbPasswordConfigured: Boolean(envValue('P0_RUNTIME_DB_PASSWORD')),
    runtimeMigrationVerifierScript: RUNTIME_MIGRATION_VERIFIER_SCRIPT,
    headed: envValue('P0_HEADED') === '1'
  }
  return {
    ...config,
    missing: collectMissingConfig(config)
  }
}

function collectMissingConfig(config) {
  const missing = []
  for (const [key, description] of REQUIRED_ENV) {
    if (!envValue(key)) {
      missing.push({ key, description })
    }
  }
  for (const key of NUMERIC_ENV) {
    if (!numberEnv(key)) {
      missing.push({ key, description: '必须是大于 0 的真实数字 ID，不能使用占位值。' })
    }
  }
  if (config.runId && config.dataPrefix !== `${DATA_PREFIX}${config.runId}`) {
    missing.push({
      key: 'P0_RUN_ID',
      description: `写入型数据前缀必须精确使用 ${DATA_PREFIX}<runId>，不能复用历史固定前缀。`
    })
  }
  if (config.migrationPolicyEvidence) {
    const migrationPolicyEvidenceIssue = validateMigrationPolicyEvidence(config.migrationPolicyEvidence)
    if (migrationPolicyEvidenceIssue) {
      missing.push(migrationPolicyEvidenceIssue)
    }
  }
  if (config.tenant && FORBIDDEN_TENANTS.some((tenant) => config.tenant.toLowerCase().includes(tenant))) {
    missing.push({
      key: 'P0_TENANT',
      description: '命中禁止的生产、admin 或基线租户口径，不能执行写入型 E2E。'
    })
  }
  if (!isAllowedRuntimePair(config.frontendUrl, config.backendUrl)) {
    missing.push({
      key: 'P0_FRONTEND_URL/P0_BACKEND_URL',
      description: '前后端 URL 必须成对使用当前 worktree 8092/48092，或融合后 int_main 8081/48081。'
    })
  }
  if (!fs.existsSync(RUNTIME_MIGRATION_VERIFIER_SCRIPT)) {
    missing.push({
      key: 'RUNTIME_MIGRATION_VERIFIER_SCRIPT',
      description: `缺少只读运行态迁移验证器：${RUNTIME_MIGRATION_VERIFIER_SCRIPT}。`
    })
  }
  return missing
}

function isAllowedRuntimePair(frontendUrl, backendUrl) {
  const pairs = new Map([
    ['http://127.0.0.1:8092', 'http://127.0.0.1:48092'],
    ['http://localhost:8092', 'http://127.0.0.1:48092'],
    ['http://127.0.0.1:8081', 'http://127.0.0.1:48081'],
    ['http://localhost:8081', 'http://127.0.0.1:48081']
  ])
  return pairs.get(frontendUrl) === backendUrl
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    const blocked = new Error('缺少 Playwright runtime；请先在 IntRuoyiFronted 执行 pnpm install。')
    blocked.blocked = true
    blocked.cause = error
    throw blocked
  }
}

function resolveChromiumLaunchOptions(config) {
  const executablePath = envValue('PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (!executablePath) {
    return launchOptions
  }
  if (!fs.existsSync(executablePath)) {
    const blocked = new Error(`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH 指向的浏览器不存在：${executablePath}`)
    blocked.blocked = true
    throw blocked
  }
  return { ...launchOptions, executablePath }
}

function redactResult(result) {
  const copy = {
    closureEvidenceRequiredAnswers: CLOSURE_EVIDENCE_REQUIRED_ANSWERS,
    ...result
  }
  delete copy.password
  delete copy.runtimeDbPassword
  return copy
}

function sanitizeRuntimeMigration(payload) {
  if (!payload) return undefined
  return {
    status: payload.status,
    runtime: payload.runtime,
    blockers: (payload.blockers || []).map((blocker) => ({
      code: blocker.code,
      message: blocker.message,
      table: blocker.table,
      column: blocker.column,
      index: blocker.index,
      check: blocker.check,
      count: blocker.count,
      expected: blocker.expected,
      actual: blocker.actual
    })),
    requiredEnv: payload.requiredEnv || [],
    requiredColumns: payload.requiredColumns || [],
    requiredIndexes: payload.requiredIndexes || [],
    historicalChecks: payload.historicalChecks || []
  }
}

function appendRuntimeMigrationLines(lines, result) {
  lines.push('## Runtime Migration', '')
  if (!result.runtimeMigration) {
    lines.push('- Status: `BLOCKED`')
    lines.push('- Reason: 真实 E2E 前置条件未齐备，尚未调用只读运行态迁移验证器。')
    lines.push('- Required Proof: 浏览器写入前必须运行 `verify_p0_runtime_migration.py` 并返回 `PASS`。')
    return
  }
  lines.push(`- Status: \`${result.runtimeMigration.status || 'UNKNOWN'}\``)
  lines.push(`- Runtime: \`${JSON.stringify(result.runtimeMigration.runtime || {})}\``)
  lines.push(`- Required Columns: \`${(result.runtimeMigration.requiredColumns || []).length}\``)
  lines.push(`- Required Indexes: \`${(result.runtimeMigration.requiredIndexes || []).length}\``)
  lines.push(`- Historical Checks: \`${(result.runtimeMigration.historicalChecks || []).length}\``)
  for (const blocker of result.runtimeMigration.blockers || []) {
    lines.push(`- Blocker: \`${blocker.code || 'P0_RUNTIME_SCHEMA_BLOCKED'}\` - ${blocker.message || JSON.stringify(blocker)}`)
  }
}

function redactClosureEvidenceValue(value) {
  if (value === null || value === undefined) return value
  if (typeof value === 'number' || typeof value === 'boolean') return value
  const text = typeof value === 'string' ? value : JSON.stringify(value)
  return text.length > 160 ? `${text.slice(0, 160)}...` : text
}

function sanitizeClosureEvidencePacket(closureEvidence) {
  if (!closureEvidence) return undefined
  const answers = {}
  for (const answerKey of CLOSURE_EVIDENCE_REQUIRED_ANSWERS) {
    const answer = closureEvidence.answers?.[answerKey]
    if (!answer) continue
    answers[answerKey] = {
      value: redactClosureEvidenceValue(answer.value),
      section: answer.section,
      sameSource: answer.sameSource,
      sourceIds: answer.sourceIds || {},
      readOnlyVerificationEntries: (answer.readOnlyVerificationEntries || []).map((entry) => ({
        verificationKey: entry.verificationKey,
        method: entry.method,
        path: entry.path,
        params: entry.params || {}
      })),
      blockers: answer.blockers || []
    }
  }
  return {
    processPoolEventId: closureEvidence.processPoolEventId,
    complete: closureEvidence.complete,
    answers,
    sameSourceChecks: closureEvidence.sameSourceChecks || [],
    blockers: closureEvidence.blockers || []
  }
}

function validateClosureEvidencePacket(closureEvidence) {
  const issues = []
  if (!closureEvidence) {
    return ['后端 trace 未返回 closureEvidence。']
  }
  if (closureEvidence.complete !== true) {
    issues.push('CLOSURE_EVIDENCE_NOT_COMPLETE: closureEvidence.complete 不是 true。')
  }
  for (const answerKey of CLOSURE_EVIDENCE_REQUIRED_ANSWERS) {
    const answer = closureEvidence.answers?.[answerKey]
    if (!answer) {
      issues.push(`${CLOSURE_EVIDENCE_MISSING_SOURCE}: answers.${answerKey} 缺失。`)
      continue
    }
    const sourceCount = Object.values(answer.sourceIds || {}).filter(
      (value) => value !== null && value !== undefined && value !== ''
    ).length
    if (sourceCount === 0) {
      issues.push(`${CLOSURE_EVIDENCE_MISSING_SOURCE}: answers.${answerKey}.sourceIds 缺少正式来源。`)
    }
    if (!Array.isArray(answer.readOnlyVerificationEntries) || answer.readOnlyVerificationEntries.length === 0) {
      issues.push(`${CLOSURE_EVIDENCE_MISSING_SOURCE}: answers.${answerKey}.readOnlyVerificationEntries 缺少只读复验入口。`)
    }
    if (answer.sameSource !== true) {
      issues.push(`CLOSURE_EVIDENCE_SAME_SOURCE_FAILED: answers.${answerKey}.sameSource 不是 true。`)
    }
    for (const blocker of answer.blockers || []) {
      issues.push(`${blocker.code || 'CLOSURE_EVIDENCE_BLOCKED'}: answers.${answerKey} ${blocker.message || '闭环证据 answer 阻塞。'}`)
    }
  }
  const failedSameSourceChecks = (closureEvidence.sameSourceChecks || []).filter((check) => check.passed !== true)
  for (const check of failedSameSourceChecks) {
    issues.push(`CLOSURE_EVIDENCE_SAME_SOURCE_FAILED: sameSourceChecks.${check.checkKey} 未通过。`)
  }
  for (const blocker of closureEvidence.blockers || []) {
    issues.push(`${blocker.code || 'CLOSURE_EVIDENCE_BLOCKED'}: ${blocker.message || '闭环证据包阻塞。'}`)
  }
  return issues
}

function requirePositiveId(value, label) {
  const id = Number(value)
  assert.equal(Number.isFinite(id) && id > 0, true, `${label} 必须是大于 0 的正式 ID。`)
  return id
}

async function parseCommonResultData(response, label) {
  const body = await parseCommonResultBody(response, label)
  assert.equal(Number(body.code), 0, `${label} 业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function parseCommonResultBody(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP 失败：${response.status()}`)
  const body = await response.json()
  return body
}

function extractProcessPoolEventIdFromFrontlineResponse(data) {
  return requirePositiveId(data?.processPoolEventId, '一线生产提交响应 processPoolEventId')
}

function extractPqcEventIdFromResponse(data) {
  return requirePositiveId(data, 'PQC 提交响应 pqcEventId')
}

async function waitForEndpointResponse(page, endpoint, action, label, method) {
  const body = await waitForEndpointCommonResult(page, endpoint, action, label, method)
  assert.equal(Number(body.code), 0, `${label} 业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function waitForEndpointCommonResult(page, endpoint, action, label, method) {
  const responsePromise = page.waitForResponse((response) => {
    const methodMatched = method ? response.request().method() === method : true
    return methodMatched && response.url().includes(endpoint)
  }, { timeout: 60000 })
  await action()
  const response = await responsePromise
  return parseCommonResultBody(response, label)
}

async function validateClosureEvidence(closureEvidence, processPoolEventId) {
  const issues = validateClosureEvidencePacket(closureEvidence)
  if (closureEvidence) {
    assert.equal(
      Number(closureEvidence.processPoolEventId),
      Number(processPoolEventId),
      'closureEvidence.processPoolEventId 必须等于本轮一线提交响应返回的新 processPoolEventId。'
    )
  }
  assert.deepEqual(issues, [], `P0 闭环证据包未完成：${issues.join('；')}`)
  return { issues }
}

function appendClosureEvidenceLines(lines, result) {
  lines.push('## Closure Evidence Packet', '')
  if (!result.closureEvidence) {
    lines.push('- Status: `BLOCKED`')
    lines.push('- Reason: 真实页面 run 尚未捕获新的 `processPoolEventId` 和后端 `closureEvidence`，不得用历史 ID、页面文案或脚本常量补齐。')
    lines.push(`- Missing Rule: \`${CLOSURE_EVIDENCE_MISSING_SOURCE}\` 任一正式来源缺失时真实 E2E 不得 PASS。`)
    lines.push(`- Required Answers: \`${CLOSURE_EVIDENCE_REQUIRED_ANSWERS.join('`, `')}\``)
    lines.push('- Required Proof: 每个 answer 必须包含 `sourceIds`、`sameSource=true` 和 `readOnlyVerificationEntries`。')
    return
  }
  lines.push(`- processPoolEventId: \`${result.closureEvidence.processPoolEventId || '--'}\``)
  lines.push(`- complete: \`${result.closureEvidence.complete === true ? 'true' : 'false'}\``)
  lines.push(`- sameSourceChecks: \`${(result.closureEvidence.sameSourceChecks || []).length}\``)
  lines.push(`- blockers: \`${(result.closureEvidence.blockers || []).length}\``)
  for (const answerKey of CLOSURE_EVIDENCE_REQUIRED_ANSWERS) {
    const answer = result.closureEvidence.answers?.[answerKey]
    const sourceCount = Object.values(answer?.sourceIds || {}).filter(
      (value) => value !== null && value !== undefined && value !== ''
    ).length
    const verificationCount = (answer?.readOnlyVerificationEntries || []).length
    lines.push(
      `- answers.${answerKey}: sourceIds=\`${sourceCount}\`, sameSource=\`${answer?.sameSource === true}\`, readOnlyVerificationEntries=\`${verificationCount}\``
    )
  }
  for (const issue of result.closureEvidenceIssues || []) {
    lines.push(`- Closure Issue: ${issue}`)
  }
}

function writeEvidence(result) {
  ensureDir(RESULT_DIR)
  ensureDir(path.dirname(EVIDENCE_FILE))
  const generatedAt = new Date().toISOString()
  const evidenceResult = {
    ...result,
    generatedAt,
    runtimeMigration: result.runtimeMigration,
    browserDiagnostics: normalizeBrowserDiagnostics(result),
    targetResponseIdentities: buildTargetResponseIdentityEvidence(result)
  }
  fs.writeFileSync(
    path.join(RESULT_DIR, 'result.json'),
    `${JSON.stringify(redactResult(evidenceResult), null, 2)}\n`,
    'utf8'
  )

  const lines = [
    '# P0 生产执行主闭环真实 E2E 证据',
    '',
    `- Task ID: \`${TASK_ID}\``,
    `- Generated At: \`${generatedAt}\``,
    `- Status: \`${result.status}\``,
    `- Frontend: \`${result.frontendUrl || '--'}\``,
    `- Backend: \`${result.backendUrl || '--'}\``,
    `- Tenant: \`${result.tenant || '--'}\``,
    `- User: \`${result.username || '--'}\``,
    `- Run ID: \`${result.runId || '--'}\``,
    `- Data Prefix: \`${result.dataPrefix || DATA_PREFIX}\``,
    `- Device Account ID: \`${result.deviceAccountId || '--'}\``,
    `- Batch Record Binding: report=\`${result.batchRecordReportId || '--'}\`, definition=\`${result.batchRecordDefinitionId || '--'}\`, version=\`${result.batchRecordVersionId || '--'}\``,
    `- Schema Migration ID: \`${result.schemaMigrationId || '--'}\``,
    `- Migration Policy Evidence: \`${result.migrationPolicyEvidence || '--'}\``,
    `- Submit Idempotency Key Configured: \`${Boolean(result.submitIdempotencyKey)}\``,
    `- PQC Idempotency Key Configured: \`${Boolean(result.pqcIdempotencyKey)}\``,
    `- Confirm Idempotency Key Configured: \`${Boolean(result.confirmIdempotencyKey)}\``,
    `- processPoolEventId: \`${result.processPoolEventId || '--'}\``,
    `- Duplicate Production Submit Verified: \`${(result.duplicateProductionSubmitVerified || result.browserPreflight?.duplicateProductionSubmitVerified) === true ? 'true' : 'false'}\``,
    `- Duplicate PQC Submit Verified: \`${(result.duplicatePqcSubmitVerified || result.browserPreflight?.duplicatePqcSubmitVerified) === true ? 'true' : 'false'}\``,
    `- Duplicate FIFO Confirm Rejected: \`${(result.duplicateConfirmRejected || result.browserPreflight?.duplicateConfirmRejected) === true ? 'true' : 'false'}\``,
    `- Browser Preflight: \`${result.browserPreflight?.currentUrl || '--'}\``,
    `- Route Preflight Steps: \`${(result.browserPreflight?.routeSteps || []).length}\``,
    ...buildTargetRequestEvidenceLines(result),
    ...buildTargetResponseIdentityEvidenceLines(result),
    ...buildBrowserDiagnosticEvidenceLines(result),
    '',
    '## BDD',
    '',
    '- BDD: P0 生产执行主闭环 -> Given 真实测试租户、工单、设备、PQC、电子签名、班组长和正式批记录绑定齐备 When 一线提交后经过 PQC、复核、FIFO 分配和批记录回填 Then trace 必须以 processPoolEventId 返回完整闭环。',
    ''
  ]

  appendClosureEvidenceLines(lines, result)
  lines.push('')
  appendRuntimeMigrationLines(lines, result)
  lines.push('')

  if (result.status === 'BLOCKED') {
    lines.push('## BLOCKED', '')
    lines.push(`- E2E: \`pnpm --dir IntRuoyiFronted e2e:p0-production-execution-loop:real\` -> BLOCKED, ${result.reason}`)
    for (const item of result.missing || []) {
      lines.push(`- Missing: \`${item.key}\` - ${item.description}`)
    }
    lines.push('- Impact: 未执行写入型真实 E2E；没有用静态合同、API-only 或默认成功冒充闭环通过。')
  } else if (result.status === 'FAIL') {
    lines.push('## RED', '')
    lines.push(`- RED: \`pnpm --dir IntRuoyiFronted e2e:p0-production-execution-loop:real\` -> FAIL, ${result.reason}`)
  }

  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

function buildTargetRequestEvidenceLines(result) {
  const targetRequests = result.targetRequests || result.browserPreflight?.targetRequests || []
  return TARGET_REQUEST_BOUNDARIES.flatMap((boundary) => {
    const request = targetRequests.find(
      (item) => item.label === boundary.label && item.url.includes(boundary.endpoint)
    )
    return [
      `- Target Request ${boundary.label} Hit: \`${request ? 'true' : 'false'}\``,
      `- Target Request ${boundary.label} URL: \`${request?.url || '--'}\``,
      `- Target Request ${boundary.label} Method: \`${request?.method || '--'}\``,
      `- Target Request ${boundary.label} HTTP Status: \`${request?.httpStatus || '--'}\``,
      `- Target Request ${boundary.label} Business Code: \`${request?.businessCode ?? '--'}\``
    ]
  })
}

function buildTargetResponseIdentityEvidence(result) {
  const source = result.browserPreflight || result
  const identities = [
    {
      label: 'FRONTLINE_SUBMIT_ENDPOINT',
      field: 'processPoolEventId',
      value: source.frontlineResponse?.processPoolEventId
    },
    {
      label: 'PQC_SUBMIT_ENDPOINT',
      field: 'pqcEventId',
      value: source.pqcEventId || source.pqcResponse
    },
    {
      label: 'TEAM_LEADER_REVIEW_ENDPOINT',
      field: 'reviewId',
      value: source.reviewResponse
    },
    {
      label: 'TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT',
      field: 'reviewId',
      value: source.allocationResponse
    },
    {
      label: 'PRODUCTION_EXECUTION_TRACE_ENDPOINT',
      field: 'processPoolEventId',
      value: source.trace?.processPoolEventId
    }
  ]
  return Object.fromEntries(
    identities.map((identity) => [
      identity.label,
      {
        field: identity.field,
        value: identity.value || null,
        sourceRequestLabel: identity.label
      }
    ])
  )
}

function buildTargetResponseIdentityEvidenceLines(result) {
  const targetResponseIdentities = buildTargetResponseIdentityEvidence(result)
  return Object.entries(targetResponseIdentities).map(([label, identity]) =>
    `- Target Response ${label} ${identity.field}: \`${identity.value || '--'}\``
  )
}

function normalizeBrowserDiagnostics(result) {
  const diagnostics = result.browserDiagnostics || result.browserPreflight?.browserDiagnostics
  const strict = result.status === 'PASS'
  if (!diagnostics) {
    if (strict) {
      throw new Error('P0 PASS evidence requires browserDiagnostics captured from the real browser page.')
    }
    return {
      pageErrors: [],
      consoleErrors: [],
      targetRequestFailures: []
    }
  }
  const normalized = {}
  for (const key of ['pageErrors', 'consoleErrors', 'targetRequestFailures']) {
    if (!Array.isArray(diagnostics[key])) {
      if (strict) {
        throw new Error(`P0 PASS evidence requires browserDiagnostics.${key} to be an array.`)
      }
      normalized[key] = []
    } else {
      normalized[key] = diagnostics[key]
    }
  }
  return normalized
}

function buildBrowserDiagnosticEvidenceLines(result) {
  const diagnostics = normalizeBrowserDiagnostics(result)
  return [
    `- Browser Page Errors: \`${diagnostics.pageErrors.length}\``,
    `- Browser Console Errors: \`${diagnostics.consoleErrors.length}\``,
    `- Target Request Failures: \`${diagnostics.targetRequestFailures.length}\``
  ]
}

async function assertHttpOk(url, label) {
  const response = await fetch(url)
  assert.equal(response.ok, true, `${label} 不可用：${url} -> HTTP ${response.status}`)
}

function parseRuntimeMigrationPayload(stdout, stderr) {
  try {
    return sanitizeRuntimeMigration(JSON.parse(stdout || '{}'))
  } catch (error) {
    return {
      status: 'FAIL',
      blockers: [{
        code: 'P0_RUNTIME_VERIFIER_OUTPUT_INVALID',
        message: `运行态迁移验证器未输出合法 JSON：${error.message || String(error)}；stderr=${stderr || '--'}`
      }],
      requiredEnv: [],
      requiredColumns: [],
      requiredIndexes: [],
      historicalChecks: []
    }
  }
}

function spawnRuntimeMigrationVerifier() {
  return new Promise((resolve) => {
    const child = spawn('python', ['-X', 'utf8', RUNTIME_MIGRATION_VERIFIER_SCRIPT], {
      cwd: WORKSPACE_ROOT,
      env: process.env,
      windowsHide: true
    })
    let stdout = ''
    let stderr = ''
    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString('utf8')
    })
    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString('utf8')
    })
    child.on('error', (error) => {
      resolve({
        exitCode: null,
        stdout,
        stderr: `${stderr}${stderr ? '\n' : ''}${error.message || String(error)}`
      })
    })
    child.on('close', (exitCode) => {
      resolve({ exitCode, stdout, stderr })
    })
  })
}

async function runRuntimeMigrationVerifier() {
  const result = await spawnRuntimeMigrationVerifier()
  const payload = parseRuntimeMigrationPayload(result.stdout, result.stderr)
  if (result.exitCode !== 0 || payload.status !== 'PASS') {
    const blockerCodes = (payload.blockers || []).map((blocker) => blocker.code).filter(Boolean)
    const blocked = new Error(
      `运行态迁移核验未通过：${blockerCodes.length ? blockerCodes.join(', ') : 'P0_RUNTIME_SCHEMA_BLOCKED'}`
    )
    blocked.blocked = true
    blocked.runtimeMigration = payload
    throw blocked
  }
  return payload
}

function appendQueryValue(query, key, value) {
  if (value === undefined || value === null || value === '') {
    return
  }
  query.set(key, String(value))
}

function buildProductionFillUrl(config) {
  const query = new URLSearchParams()
  appendQueryValue(query, 'workOrderId', config.workOrderId)
  appendQueryValue(query, 'productionOrderCode', config.workOrderCode)
  appendQueryValue(query, 'routeId', config.routeId)
  appendQueryValue(query, 'routeProcessId', config.routeProcessId)
  appendQueryValue(query, 'processId', config.processId)
  appendQueryValue(query, 'taskId', config.taskId)
  appendQueryValue(query, 'itemId', config.itemId)
  appendQueryValue(query, 'feedbackCode', `${config.dataPrefix}${Date.now()}`)
  appendQueryValue(query, 'feedbackType', config.feedbackType)
  appendQueryValue(query, 'approveUserId', config.approveUserId)
  appendQueryValue(query, 'recordbookId', config.recordbookId)
  appendQueryValue(query, 'workstationId', config.workstationId)
  appendQueryValue(query, 'deviceId', config.deviceId)
  appendQueryValue(query, 'signatureId', config.signatureId)
  appendQueryValue(query, 'signatureEmployeeId', config.signatureEmployeeId)
  appendQueryValue(query, 'actualEmployeeId', config.signatureEmployeeId)
  appendQueryValue(query, 'outputQuantity', config.submitQuantity)
  appendQueryValue(query, 'idempotencyKey', config.submitIdempotencyKey)
  return `${config.frontendUrl}${PRODUCTION_FILL_ROUTE}?${query.toString()}`
}

function buildPqcFillUrl(config) {
  const query = new URLSearchParams()
  appendQueryValue(query, 'workOrderId', config.workOrderId)
  appendQueryValue(query, 'workOrderCode', config.workOrderCode)
  appendQueryValue(query, 'routeId', config.routeId)
  appendQueryValue(query, 'routeProcessId', config.routeProcessId)
  appendQueryValue(query, 'processId', config.processId)
  appendQueryValue(query, 'deviceId', config.deviceId)
  appendQueryValue(query, 'signatureId', config.pqcSignatureId)
  appendQueryValue(query, 'signatureEmployeeId', config.pqcSignatureEmployeeId)
  appendQueryValue(query, 'actualEmployeeId', config.pqcSignatureEmployeeId)
  appendQueryValue(query, 'pqcTaskId', config.pqcTaskId)
  appendQueryValue(query, 'regulationVersionId', config.qaRegulationVersionId)
  appendQueryValue(query, 'pqcInspectionQuantity', config.pqcInspectionQuantity)
  appendQueryValue(query, 'pqcQualifiedQuantity', config.pqcQualifiedQuantity)
  appendQueryValue(query, 'pqcAllocatableQuantity', config.pqcAllocatableQuantity)
  appendQueryValue(query, 'productionSubmitEventId', config.processPoolEventId)
  appendQueryValue(query, 'processPoolEventId', config.processPoolEventId)
  appendQueryValue(query, 'pqcSubmissionIdempotencyKey', config.pqcIdempotencyKey)
  return `${config.frontendUrl}${PQC_FILL_ROUTE}?${query.toString()}`
}

function buildTimelineUrl(config) {
  const query = new URLSearchParams()
  appendQueryValue(query, 'workOrderCode', config.workOrderCode)
  appendQueryValue(query, 'workOrderId', config.workOrderId)
  appendQueryValue(query, 'routeProcessId', config.routeProcessId)
  appendQueryValue(query, 'processId', config.processId)
  return `${config.frontendUrl}${TIMELINE_ROUTE}?${query.toString()}`
}

async function fillFirst(pageOrLocator, selectors, value) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function clickFirst(pageOrLocator, selectors) {
  for (const selector of selectors) {
    const locator = pageOrLocator.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.click()
      return
    }
  }
  throw new Error(`找不到可点击控件：${selectors.join(', ')}`)
}

async function waitForVisible(page, selectors, label) {
  for (const selector of selectors) {
    const locator = page.locator(selector).first()
    if (await locator.count()) {
      await locator.waitFor({ state: 'visible', timeout: 30000 })
      return locator
    }
  }
  throw new Error(`${label} 未渲染：${selectors.join(', ')}`)
}

async function selectLoginTenant(page, tenantName) {
  const tenantSelectInput = page.locator('.el-select input:visible').first()
  await tenantSelectInput.waitFor({ state: 'visible', timeout: 15000 })
  await tenantSelectInput.click()
  await tenantSelectInput.fill(tenantName)
  const tenantOption = page.locator('.el-select-dropdown__item:visible', { hasText: tenantName }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 15000 })
  await tenantOption.click()
  await page.getByText(tenantName, { exact: true }).first().waitFor({ state: 'visible', timeout: 15000 })
}

async function login(page, config) {
  await page.goto(`${config.frontendUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await selectLoginTenant(page, config.tenant)
  await fillFirst(page, [
    'input[placeholder*="账号"]',
    'input[placeholder*="用户名"]',
    'input[name="username"]'
  ], config.username)
  await fillFirst(page, [
    'input[placeholder*="密码"]',
    'input[type="password"]',
    'input[name="password"]'
  ], config.password)
  const loginResponsePromise = page.waitForResponse((response) =>
    response.url().includes(SYSTEM_AUTH_LOGIN_ENDPOINT) && response.request().method() === 'POST'
  , { timeout: 30000 })
  const permissionInfoResponsePromise = page.waitForResponse((response) =>
    response.url().includes(SYSTEM_AUTH_GET_PERMISSION_INFO_ENDPOINT) && response.request().method() === 'GET'
  , { timeout: 30000 })
  await clickFirst(page, [
    'button:has-text("登录")',
    '.login-form button[type="submit"]'
  ])
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `登录接口 HTTP 失败：${loginResponse.status()}`)
  const body = await loginResponse.json()
  assert.equal(Number(body.code), 0, `登录接口业务失败：${body.msg || body.message || 'unknown'}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
  const permissionInfo = await parseCommonResultData(
    await permissionInfoResponsePromise,
    '当前登录用户权限信息'
  )
  const currentUserId = requirePositiveId(permissionInfo?.user?.id, '当前登录用户 ID')
  assert.equal(
    currentUserId,
    config.deviceAccountId,
    '当前登录用户必须等于 P0_DEVICE_ACCOUNT_ID，不能用未核验登录用户隐式替代设备账号。'
  )
  await page.waitForLoadState('networkidle')
  return { currentUrl: page.url(), title: await page.title(), deviceAccountId: currentUserId }
}

async function openTeamLeaderWorkbench(page, config, routeSteps) {
  await page.goto(`${config.frontendUrl}${TEAM_LEADER_ROUTE}`, { waitUntil: 'networkidle', timeout: 60000 })
  await waitForVisible(page, ['[data-team-leader-report-workbench]'], '班组长工作台')
  routeSteps.push({ route: TEAM_LEADER_ROUTE, status: 'visible' })
}

async function openProductionFill(page, config, routeSteps, targetUrl = buildProductionFillUrl(config)) {
  await page.goto(targetUrl, { waitUntil: 'networkidle', timeout: 60000 })
  await waitForVisible(page, ['[data-frontline-production-operator]'], '生产填写页面')
  routeSteps.push({ route: PRODUCTION_FILL_ROUTE, status: 'visible' })
}

async function openPqcFill(page, config, routeSteps, targetUrl = buildPqcFillUrl(config)) {
  await page.goto(targetUrl, { waitUntil: 'networkidle', timeout: 60000 })
  await waitForVisible(page, ['[data-frontline-pqc-operator]'], 'PQC 填写页面')
  routeSteps.push({ route: PQC_FILL_ROUTE, status: 'visible' })
}

async function openProductionExecutionTrace(page, config, routeSteps) {
  await page.goto(buildTimelineUrl(config), { waitUntil: 'networkidle', timeout: 60000 })
  await waitForVisible(page, ['.process-pool-timeline', '[data-p0-production-execution-trace]'], '工序池时间轴页面')
  routeSteps.push({ route: TIMELINE_ROUTE, status: 'visible' })
}

function hasSameTargetRequestEvidence(targetRequests, targetRequest) {
  return targetRequests.some(
    (item) => item.label === targetRequest.label && item.url === targetRequest.url
  )
}

function resolveTargetRequestBoundary(url) {
  const pathname = new URL(url).pathname.replace(/\/$/, '') || '/'
  return TARGET_REQUEST_BOUNDARIES.find((item) => pathname === item.endpoint)
}

function attachTargetRequestTracker(page) {
  const targetRequests = []
  const targetResponseBodyParses = []
  const browserDiagnostics = {
    pageErrors: [],
    consoleErrors: [],
    targetRequestFailures: []
  }
  page.on('pageerror', (error) => {
    browserDiagnostics.pageErrors.push({
      message: String(error?.message || error).slice(0, 500)
    })
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      browserDiagnostics.consoleErrors.push({
        text: message.text().slice(0, 500)
      })
    }
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (resolveTargetRequestBoundary(url)) {
      browserDiagnostics.targetRequestFailures.push({
        method: request.method(),
        url: url.replace(/\?.*$/, ''),
        failure: request.failure()?.errorText || 'unknown'
      })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    const boundary = resolveTargetRequestBoundary(url)
    if (boundary) {
      const httpStatus = response.status()
      const targetRequest = {
        label: boundary.label,
        method: response.request().method(),
        url: url.replace(/\?.*$/, ''),
        httpStatus
      }
      if (!hasSameTargetRequestEvidence(targetRequests, targetRequest)) {
        targetRequests.push(targetRequest)
      }
      const parseBusinessCode = response.json()
        .then((body) => {
          const parsedCode = Number(body?.code)
          targetRequest.businessCode = Number.isFinite(parsedCode) ? parsedCode : 'MISSING'
        })
        .catch(() => {
          targetRequest.businessCode = 'MISSING'
        })
      targetResponseBodyParses.push(parseBusinessCode)
      if (httpStatus < 200 || httpStatus >= 300) {
        browserDiagnostics.targetRequestFailures.push({
          ...targetRequest,
          failure: `HTTP ${httpStatus}`
        })
      }
    }
  })
  return {
    targetRequests,
    browserDiagnostics,
    flush: () => Promise.allSettled(targetResponseBodyParses)
  }
}

async function submitFrontlineProduction(page, config) {
  const frontlineResponse = await waitForEndpointResponse(
    page,
    FRONTLINE_SUBMIT_ENDPOINT,
    async () => {
      await clickFirst(page, [
        '[data-frontline-production-operator] .frontline-production-submit-button',
        '[data-frontline-production-operator] button:has-text("提交")'
      ])
    },
    '一线生产提交',
    'POST'
  )
  const processPoolEventId = extractProcessPoolEventIdFromFrontlineResponse(frontlineResponse)
  return { processPoolEventId, frontlineResponse }
}

async function duplicateFrontlineProduction(page, config, routeSteps, productionFillUrl, processPoolEventId) {
  await openProductionFill(page, config, routeSteps, productionFillUrl)
  const {
    processPoolEventId: duplicateProcessPoolEventId,
    frontlineResponse: duplicateFrontlineResponse
  } = await submitFrontlineProduction(page, config)
  assert.equal(
    Number(duplicateProcessPoolEventId),
    Number(processPoolEventId),
    '重复一线生产提交必须通过相同幂等键返回原始 processPoolEventId，不得创建第二个生产提交根事件。'
  )
  return {
    duplicateProcessPoolEventId,
    duplicateFrontlineResponse,
    duplicateProductionSubmitVerified: true
  }
}

async function submitPqcInspection(page, config) {
  const pqcResponse = await waitForEndpointResponse(
    page,
    PQC_SUBMIT_ENDPOINT,
    async () => {
      await clickFirst(page, [
        '[data-frontline-pqc-operator] .frontline-pqc-submit-button',
        '[data-frontline-pqc-operator] button:has-text("提交")'
      ])
    },
    'PQC 检验提交',
    'POST'
  )
  return { pqcEventId: extractPqcEventIdFromResponse(pqcResponse), pqcResponse }
}

async function duplicatePqcInspection(page, config, routeSteps, pqcFillUrl, pqcEventId) {
  await openPqcFill(page, config, routeSteps, pqcFillUrl)
  const {
    pqcEventId: duplicatePqcEventId,
    pqcResponse: duplicatePqcResponse
  } = await submitPqcInspection(page, config)
  assert.equal(
    Number(duplicatePqcEventId),
    Number(pqcEventId),
    '重复 PQC 提交必须通过相同幂等键返回原始 PQC 结果，不得创建第二组 PQC 明细或事件。'
  )
  return {
    duplicatePqcEventId,
    duplicatePqcResponse,
    duplicatePqcSubmitVerified: true
  }
}

async function selectTeamLeaderTab(page, label) {
  const targetRoute = label.includes('PQC') ? PQC_LEADER_ROUTE : TEAM_LEADER_ROUTE
  await page.goto(new URL(targetRoute, page.url()).toString(), {
    waitUntil: 'networkidle',
    timeout: 60000
  })
  await waitForVisible(page, ['[data-team-leader-report-workbench]'], `${label}工作台`)
  await page.waitForLoadState('networkidle')
}

async function filterTeamLeaderWorkOrder(page, config) {
  const workbench = page.locator('[data-team-leader-report-workbench]').first()
  await workbench.waitFor({ state: 'visible', timeout: 30000 })
  await fillFirst(workbench, ['input[placeholder="工单编码"]', 'input[placeholder*="工单"]'], config.workOrderCode)
  await clickFirst(workbench, ['button:has-text("搜索")'])
  await page.waitForLoadState('networkidle')
}

async function openReviewDialogForWorkOrder(page, config) {
  await filterTeamLeaderWorkOrder(page, config)
  const row = page.locator('.el-table__body-wrapper tbody tr', { hasText: config.workOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await clickFirst(row, ['button:has-text("复核")'])
  const dialog = page.locator('.el-dialog:visible', { hasText: '复核员工提交' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  return dialog
}

async function fillReviewSignatureForm(dialog, signature) {
  await fillFirst(dialog, ['[data-team-leader-review-signature] input'], signature.reviewSignatureId)
  await fillFirst(dialog, ['.el-form-item:has-text("签名员工ID") input'], signature.reviewSignatureEmployeeId)
  await fillFirst(dialog, ['.el-form-item:has-text("签名快照") textarea'], JSON.stringify({
    source: TASK_ID,
    signaturePurpose: signature.signaturePurpose,
    signatureId: signature.reviewSignatureId,
    signatureEmployeeUserId: signature.reviewSignatureEmployeeId
  }))
}

async function reviewTeamLeaderSubmission(page, config) {
  await selectTeamLeaderTab(page, 'PQC 组长')
  const dialog = await openReviewDialogForWorkOrder(page, config)
  await fillReviewSignatureForm(dialog, {
    reviewSignatureId: config.pqcReviewSignatureId,
    reviewSignatureEmployeeId: config.pqcReviewSignatureEmployeeId,
    signaturePurpose: 'PQC_REVIEW'
  })
  const reviewResponse = await waitForEndpointResponse(
    page,
    TEAM_LEADER_REVIEW_ENDPOINT,
    async () => {
      await clickFirst(dialog, ['button:has-text("提交复核")'])
    },
    'PQC 班组长复核',
    'POST'
  )
  return { reviewResponse }
}

async function confirmTeamLeaderAllocation(page, config) {
  await selectTeamLeaderTab(page, '生产组长')
  const dialog = await openReviewDialogForWorkOrder(page, config)
  await fillReviewSignatureForm(dialog, {
    reviewSignatureId: config.reviewSignatureId,
    reviewSignatureEmployeeId: config.reviewSignatureEmployeeId,
    signaturePurpose: 'FIFO_CONFIRM'
  })
  await clickFirst(dialog, ['[data-team-leader-fifo-allocation]'])
  await dialog.locator('[data-team-leader-allocation-table] .el-table__body-wrapper tbody tr')
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  const allocationResponse = await waitForEndpointResponse(
    page,
    TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT,
    async () => {
      await clickFirst(dialog, ['button:has-text("提交复核")'])
    },
    '生产班组长 FIFO 分配确认',
    'POST'
  )
  return { allocationResponse }
}

async function duplicateTeamLeaderAllocationConfirm(page, config, routeSteps, allocationResponse) {
  assert.ok(allocationResponse, '首个 FIFO 分配确认响应必须保留，重复确认证据不能脱离原始确认结果。')
  await openTeamLeaderWorkbench(page, config, routeSteps)
  await selectTeamLeaderTab(page, '生产组长')
  const dialog = await openReviewDialogForWorkOrder(page, config)
  await fillReviewSignatureForm(dialog, {
    reviewSignatureId: config.reviewSignatureId,
    reviewSignatureEmployeeId: config.reviewSignatureEmployeeId,
    signaturePurpose: 'FIFO_CONFIRM_DUPLICATE'
  })
  await clickFirst(dialog, ['[data-team-leader-fifo-allocation]'])
  await dialog.locator('[data-team-leader-allocation-table] .el-table__body-wrapper tbody tr')
    .first()
    .waitFor({ state: 'visible', timeout: 30000 })
  const duplicateConfirmResponse = await waitForEndpointCommonResult(
    page,
    TEAM_LEADER_ALLOCATION_CONFIRM_ENDPOINT,
    async () => {
      await clickFirst(dialog, ['button:has-text("提交复核")'])
    },
    '生产班组长 FIFO 重复分配确认',
    'POST'
  )
  assert.equal(
    Number(duplicateConfirmResponse.code),
    PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE,
    `重复 FIFO 确认必须返回 PRO_PROCESS_POOL_REPORT_ALLOCATION_DUPLICATE，不得依赖前端按钮禁用或创建第二组终态事实：${duplicateConfirmResponse.msg || duplicateConfirmResponse.message || 'unknown'}`
  )
  return {
    duplicateConfirmResponse,
    allocationResponse,
    duplicateConfirmRejected: true
  }
}

async function fetchProductionExecutionTrace(page, config, routeSteps) {
  await openProductionExecutionTrace(page, config, routeSteps)
  const trace = await waitForEndpointResponse(
    page,
    PRODUCTION_EXECUTION_TRACE_ENDPOINT,
    async () => {
      const eventButton = page.locator('.process-pool-event', { hasText: config.workOrderCode }).first()
      await eventButton.waitFor({ state: 'visible', timeout: 30000 })
      await eventButton.click()
    },
    'P0 生产执行闭环 trace',
    'GET'
  )
  assert.equal(
    Number(trace.processPoolEventId),
    Number(config.processPoolEventId),
    'trace.processPoolEventId 必须等于本轮一线提交响应返回的新 processPoolEventId。'
  )
  return trace
}

async function runProductionExecutionLoop(config) {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch(resolveChromiumLaunchOptions(config))
  try {
    const context = await browser.newContext({
      ignoreHTTPSErrors: true,
      locale: 'zh-CN',
      viewport: { width: 1440, height: 960 }
    })
    try {
      const page = await context.newPage()
      const requestTracking = attachTargetRequestTracker(page)
      const loginResult = await login(page, config)
      const routeSteps = [{ route: '/login', status: 'authenticated' }]
      await openTeamLeaderWorkbench(page, config, routeSteps)
      const productionFillUrl = buildProductionFillUrl(config)
      await openProductionFill(page, config, routeSteps, productionFillUrl)
      const { processPoolEventId, frontlineResponse } = await submitFrontlineProduction(page, config)
      const {
        duplicateFrontlineResponse,
        duplicateProductionSubmitVerified
      } = await duplicateFrontlineProduction(page, config, routeSteps, productionFillUrl, processPoolEventId)
      const executionConfig = { ...config, processPoolEventId }
      const pqcFillUrl = buildPqcFillUrl(executionConfig)
      await openPqcFill(page, executionConfig, routeSteps, pqcFillUrl)
      const { pqcEventId, pqcResponse } = await submitPqcInspection(page, executionConfig)
      const {
        duplicatePqcResponse,
        duplicatePqcSubmitVerified
      } = await duplicatePqcInspection(page, executionConfig, routeSteps, pqcFillUrl, pqcEventId)
      await openTeamLeaderWorkbench(page, executionConfig, routeSteps)
      const { reviewResponse } = await reviewTeamLeaderSubmission(page, executionConfig)
      const { allocationResponse } = await confirmTeamLeaderAllocation(page, executionConfig)
      const {
        duplicateConfirmResponse,
        duplicateConfirmRejected
      } = await duplicateTeamLeaderAllocationConfirm(page, executionConfig, routeSteps, allocationResponse)
      const trace = await fetchProductionExecutionTrace(page, executionConfig, routeSteps)
      await validateClosureEvidence(trace.closureEvidence, processPoolEventId)
      await requestTracking.flush()
      return {
        currentUrl: page.url(),
        loginUrl: loginResult.currentUrl,
        title: await page.title(),
        routeSteps,
        targetRequests: requestTracking.targetRequests,
        targetRequestEvidenceFlushed: true,
        browserDiagnostics: requestTracking.browserDiagnostics,
        processPoolEventId,
        duplicateProductionSubmitVerified,
        pqcEventId,
        duplicatePqcSubmitVerified,
        duplicateConfirmRejected,
        frontlineResponse,
        duplicateFrontlineResponse,
        pqcResponse,
        duplicatePqcResponse,
        reviewResponse,
        allocationResponse,
        duplicateConfirmResponse,
        closureEvidence: trace.closureEvidence,
        trace
      }
    } finally {
      await context.close()
    }
  } finally {
    await browser.close()
  }
}

async function main() {
  const config = collectConfig()
  if (config.missing.length) {
    writeEvidence({
      status: 'BLOCKED',
      reason: '缺少真实写入型 E2E 前置条件。',
      runtimeMigration: null,
      closureEvidence: null,
      closureEvidenceIssues: [`${CLOSURE_EVIDENCE_MISSING_SOURCE}: 真实页面 run 尚未捕获新的 processPoolEventId。`],
      ...config
    })
    process.exitCode = 2
    return
  }

  let runtimeMigration
  try {
    runtimeMigration = await runRuntimeMigrationVerifier(config)
    await assertHttpOk(`${config.frontendUrl}/`, '前端入口')
    await assertHttpOk(`${config.backendUrl}/actuator/health`, '后端健康检查')
    const browserPreflight = await runProductionExecutionLoop(config)
    const closureEvidence = sanitizeClosureEvidencePacket(browserPreflight.closureEvidence)
    writeEvidence({
      ...config,
      status: 'PASS',
      reason: '真实页面生产执行主闭环已通过。',
      runtimeMigration,
      browserPreflight,
      processPoolEventId: browserPreflight.processPoolEventId,
      duplicateProductionSubmitVerified: browserPreflight.duplicateProductionSubmitVerified,
      duplicatePqcSubmitVerified: browserPreflight.duplicatePqcSubmitVerified,
      duplicateConfirmRejected: browserPreflight.duplicateConfirmRejected,
      closureEvidence,
      closureEvidenceIssues: []
    })
  } catch (error) {
    const closureEvidence = sanitizeClosureEvidencePacket(error.closureEvidence) || null
    const status = error.blocked === true ? 'BLOCKED' : 'FAIL'
    writeEvidence({
      ...config,
      status,
      reason: error.message || String(error),
      runtimeMigration: error.runtimeMigration || runtimeMigration,
      browserPreflight: error.browserPreflight,
      processPoolEventId: error.processPoolEventId,
      closureEvidence,
      closureEvidenceIssues: validateClosureEvidencePacket(closureEvidence),
      error: {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      }
    })
    process.exitCode = status === 'BLOCKED' ? 2 : 1
  }
}

void main()
