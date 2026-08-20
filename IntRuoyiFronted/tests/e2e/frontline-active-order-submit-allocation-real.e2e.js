const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { spawnSync } = require('node:child_process')
const fs = require('node:fs')
const http = require('node:http')
const https = require('node:https')
const path = require('node:path')

const TASK_ID = '20260814-frontline-active-order-submit-allocation-docs'
const DATA_PREFIX = 'FAS-20260814-'
const FRONTLINE_ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const LEADER_ROUTE = '/mes/pro/process-pool/production-leader'
const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const REQUESTED_RUNTIME_MODE = (String(process.env.FAS_RUNTIME_MODE || '').trim() || 'WORKTREE').toUpperCase()
const REQUESTED_EVIDENCE_RUN_ID = String(process.env.FAS_EVIDENCE_RUN_ID || '').trim()
const REQUESTED_ARTIFACT_DIR = String(process.env.FAS_ARTIFACT_DIR || '').trim()
const ARTIFACT_VARIANTS = Object.freeze({
  WORKTREE: 'worktree',
  POST_MERGE_INT_MAIN: 'post-merge-int-main',
  ADMIN_TENANT1_INT_MAIN: 'admin-tenant1-int-main'
})
const ARTIFACT_VARIANT = ARTIFACT_VARIANTS[REQUESTED_RUNTIME_MODE] || 'unsupported-runtime-mode'

function artifactDirFor(runtimeMode, evidenceRunId = '') {
  if (REQUESTED_ARTIFACT_DIR) {
    if (!path.isAbsolute(REQUESTED_ARTIFACT_DIR)) {
      throw new Error('FAS_ARTIFACT_DIR 必须是绝对路径')
    }
    return path.resolve(REQUESTED_ARTIFACT_DIR)
  }
  const mode = String(runtimeMode || '').trim().toUpperCase()
  const variant = ARTIFACT_VARIANTS[mode] || 'unsupported-runtime-mode'
  const baseDir = path.join(WORKSPACE_ROOT, 'doc', 'tasks', TASK_ID, 'e2e-artifacts', variant)
  if (mode !== 'ADMIN_TENANT1_INT_MAIN') return baseDir
  const runId = String(evidenceRunId || '').trim()
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/.test(runId)) {
    throw new Error('FAS_EVIDENCE_RUN_ID 必须是 1-64 位字母、数字、点、下划线或连字符，且不能包含路径片段')
  }
  return path.join(baseDir, runId)
}

let ARTIFACT_DIR
try {
  ARTIFACT_DIR = artifactDirFor(REQUESTED_RUNTIME_MODE, REQUESTED_EVIDENCE_RUN_ID)
} catch {
  ARTIFACT_DIR = path.join(
    WORKSPACE_ROOT,
    'doc',
    'tasks',
    TASK_ID,
    'e2e-artifacts',
    ARTIFACT_VARIANT,
    `invalid-evidence-run-id-${process.pid}`
  )
}
const SCENARIO_STATE_FILE = path.join(ARTIFACT_DIR, 'scenario-state.json')
const WORKTREE_ROOT = 'D:\\IntRuoyiWorktree\\20260814-frontline-active-order-submit-allocation'
const FIXED_TEST_TENANT = Object.freeze({ id: '122', name: '测试租户', fixtureMode: 'STANDARD_TENANT122' })
const ADMIN_SUPPLEMENT_TENANT = Object.freeze({ id: '1', name: '芋道源码', fixtureMode: 'ADMIN_TENANT1' })
const SENSITIVE_KEY_PATTERN = /password|passphrase|secret|token|credential|authorization|cookie|private[_-]?key|api[_-]?key|access[_-]?key|hash/i

const BLOCKED_CATEGORIES = Object.freeze({
  SERVICE_UNREACHABLE: 'SERVICE_UNREACHABLE',
  BROWSER_UNAVAILABLE: 'BROWSER_UNAVAILABLE',
  LOGIN_PREREQUISITE: 'LOGIN_PREREQUISITE',
  PERMISSION_PREREQUISITE: 'PERMISSION_PREREQUISITE',
  TASK_DATA_PREREQUISITE: 'TASK_DATA_PREREQUISITE',
  RUNTIME_EVIDENCE_PREREQUISITE: 'RUNTIME_EVIDENCE_PREREQUISITE',
  CLEANUP_PREREQUISITE: 'CLEANUP_PREREQUISITE'
})

const WORKTREE_RUNTIME = Object.freeze({
  workspaceRoot: WORKTREE_ROOT,
  frontendUrls: ['http://127.0.0.1:8099', 'http://localhost:8099'],
  backendUrl: 'http://127.0.0.1:48099',
  frontendPort: 8099,
  backendPort: 48099
})

const POST_MERGE_INT_MAIN_RUNTIME = Object.freeze({
  workspaceRoot: WORKSPACE_ROOT,
  frontendUrls: ['http://127.0.0.1:8081', 'http://localhost:8081'],
  backendUrl: 'http://127.0.0.1:48081',
  frontendPort: 8081,
  backendPort: 48081
})

const RUNTIME_PROFILES = Object.freeze({
  WORKTREE: WORKTREE_RUNTIME,
  POST_MERGE_INT_MAIN: POST_MERGE_INT_MAIN_RUNTIME,
  ADMIN_TENANT1_INT_MAIN: POST_MERGE_INT_MAIN_RUNTIME
})

const TENANT_PROFILES = Object.freeze({
  WORKTREE: FIXED_TEST_TENANT,
  POST_MERGE_INT_MAIN: FIXED_TEST_TENANT,
  ADMIN_TENANT1_INT_MAIN: ADMIN_SUPPLEMENT_TENANT
})

const REQUIRED_ENV = [
  'FAS_FRONTEND_URL',
  'FAS_BACKEND_URL',
  'FAS_ALLOWED_TEST_TENANT_IDS',
  'FAS_ALLOWED_TEST_TENANT_NAMES',
  'FAS_RUNTIME_EVIDENCE_FILE',
  'FAS_FIXTURE_MANIFEST',
  'FAS_ORCHESTRATOR_EXECUTABLE',
  'FAS_ORCHESTRATOR_SCRIPT',
  'FAS_FRONTLINE_USERNAME',
  'FAS_FRONTLINE_PASSWORD',
  'FAS_LEADER_USERNAME',
  'FAS_LEADER_PASSWORD',
  'FAS_SIGNATURE_PASSWORD'
]

class E2EBlockedError extends Error {
  constructor(category, message, cause) {
    super(message, cause ? { cause } : undefined)
    this.name = 'E2EBlockedError'
    this.category = category
  }
}

function blocked(category, message, cause) {
  return new E2EBlockedError(category, message, cause)
}

function errorText(error) {
  return error?.message || String(error)
}

async function blockedPrerequisite(category, label, action) {
  try {
    return await action()
  } catch (error) {
    if (error instanceof E2EBlockedError) throw error
    throw blocked(category, `${label}：${errorText(error)}`, error)
  }
}

function envValue(key) {
  return String(process.env[key] || '').trim()
}

function positiveLong(value, label) {
  const text = String(value ?? '').trim()
  if (!/^[1-9]\d*$/.test(text)) {
    throw blocked(BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE, `${label} 必须是大于 0 的 Java Long 十进制字符串`)
  }
  return BigInt(text).toString()
}

function exactLongId(value, label) {
  const text = String(value ?? '').trim()
  assert.match(text, /^[1-9]\d*$/, `${label} 必须是大于 0 的 Java Long 十进制字符串`)
  return BigInt(text).toString()
}

function sameLongId(left, right) {
  return exactLongId(left, '左侧 Long ID') === exactLongId(right, '右侧 Long ID')
}

function samePrerequisiteLongId(left, right) {
  return positiveLong(left, '左侧前置 Long ID') === positiveLong(right, '右侧前置 Long ID')
}

function positiveQuantity(value, label) {
  const quantity = Number(value)
  if (!Number.isSafeInteger(quantity) || quantity <= 0) {
    throw blocked(BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE, `${label} 必须是大于 0 的安全整数数量`)
  }
  return quantity
}

function normalizeLongIdJsonText(text) {
  const source = String(text)
  const contexts = []
  let output = ''
  let index = 0

  const clearParentValue = () => {
    const parent = contexts[contexts.length - 1]
    if (parent?.type === 'object') parent.pendingKey = undefined
  }

  while (index < source.length) {
    const character = source[index]
    if (character === '"') {
      const start = index
      index += 1
      let escaped = false
      while (index < source.length) {
        const current = source[index]
        index += 1
        if (escaped) {
          escaped = false
        } else if (current === '\\') {
          escaped = true
        } else if (current === '"') {
          break
        }
      }
      const token = source.slice(start, index)
      output += token
      let lookahead = index
      while (/\s/.test(source[lookahead] || '')) lookahead += 1
      const context = contexts[contexts.length - 1]
      if (context?.type === 'object' && source[lookahead] === ':') {
        context.pendingKey = JSON.parse(token)
      } else {
        clearParentValue()
      }
      continue
    }
    if (character === '{') {
      clearParentValue()
      contexts.push({ type: 'object', pendingKey: undefined })
      output += character
      index += 1
      continue
    }
    if (character === '[') {
      clearParentValue()
      contexts.push({ type: 'array' })
      output += character
      index += 1
      continue
    }
    if (character === '}' || character === ']') {
      contexts.pop()
      output += character
      index += 1
      continue
    }
    if (character === ',') {
      const context = contexts[contexts.length - 1]
      if (context?.type === 'object') context.pendingKey = undefined
      output += character
      index += 1
      continue
    }
    if (character === '-' || /\d/.test(character)) {
      const match = source.slice(index).match(/^-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/)
      if (match) {
        const token = match[0]
        const context = contexts[contexts.length - 1]
        const isLongId = context?.type === 'object'
          && /(?:^id$|Id$|ID$)/.test(context.pendingKey || '')
          && /^-?\d+$/.test(token)
        output += isLongId ? JSON.stringify(BigInt(token).toString()) : token
        if (context?.type === 'object') context.pendingKey = undefined
        index += token.length
        continue
      }
    }
    if (source.startsWith('true', index)
        || source.startsWith('false', index)
        || source.startsWith('null', index)) {
      clearParentValue()
    }
    output += character
    index += 1
  }
  return output
}

function parseJsonPreservingLongIds(text, label, blockedCategory) {
  try {
    return JSON.parse(normalizeLongIdJsonText(text))
  } catch (error) {
    if (blockedCategory) throw blocked(blockedCategory, `${label} 不是有效 JSON`, error)
    throw new Error(`${label} 不是有效 JSON`, { cause: error })
  }
}

function normalizeUrl(value) {
  return value.replace(/\/+$/, '')
}

function parseExactList(value) {
  return [...new Set(String(value || '').split(',').map((item) => item.trim()).filter(Boolean))]
}

function loadJsonFile(filePath, label, category) {
  if (!filePath || !fs.existsSync(filePath)) {
    throw blocked(category, `${label}不存在：${filePath || '--'}`)
  }
  try {
    return parseJsonPreservingLongIds(fs.readFileSync(filePath, 'utf8'), label, category)
  } catch (error) {
    if (error instanceof E2EBlockedError) throw blocked(category, error.message, error)
    throw blocked(category, `${label}读取失败：${errorText(error)}`, error)
  }
}

function loadFixtureManifest(filePath) {
  const manifest = loadJsonFile(
    filePath,
    '外部 fixture manifest',
    BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE
  )
  const required = [
    ['schemaVersion', manifest.schemaVersion],
    ['taskId', manifest.taskId],
    ['runId', manifest.runId],
    ['fixtureMode', manifest.fixtureMode],
    ['tenant.id', manifest.tenant?.id],
    ['tenant.name', manifest.tenant?.name],
    ['accounts.frontlineUsername', manifest.accounts?.frontlineUsername],
    ['accounts.leaderUsername', manifest.accounts?.leaderUsername],
    ['orders.o1.activeOrderId', manifest.orders?.o1?.activeOrderId],
    ['orders.o1.workOrderId', manifest.orders?.o1?.workOrderId],
    ['orders.o1.workOrderCode', manifest.orders?.o1?.workOrderCode],
    ['orders.o1.plannedQuantity', manifest.orders?.o1?.plannedQuantity],
    ['orders.o2.activeOrderId', manifest.orders?.o2?.activeOrderId],
    ['orders.o2.workOrderId', manifest.orders?.o2?.workOrderId],
    ['orders.o2.workOrderCode', manifest.orders?.o2?.workOrderCode],
    ['orders.o2.plannedQuantity', manifest.orders?.o2?.plannedQuantity],
    ['context.routeId', manifest.context?.routeId],
    ['context.routeProcessId', manifest.context?.routeProcessId],
    ['context.processId', manifest.context?.processId],
    ['context.actualEmployeeId', manifest.context?.actualEmployeeId],
    ['context.submitQuantity', manifest.context?.submitQuantity],
    ['context.feedbackCode', manifest.context?.feedbackCode],
    ['cleanupContract', manifest.cleanupContract]
  ]
  const missing = required.filter(([, value]) => value === undefined || value === null || String(value).trim() === '')
  if (missing.length) {
    throw blocked(
      BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE,
      `外部 fixture manifest 缺少字段：${missing.map(([key]) => key).join(', ')}`
    )
  }
  if (manifest.schemaVersion !== 'fas-fixture-v1' || manifest.cleanupContract !== 'fas-cleanup-v1') {
    throw blocked(BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE, '外部 fixture/cleanup 合同版本不受支持')
  }
  if (manifest.taskId !== TASK_ID || !String(manifest.runId).startsWith(DATA_PREFIX)) {
    throw blocked(BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE, 'fixture manifest 不属于当前任务或运行标识不合规')
  }
  assertManifestContainsNoSecrets(manifest)
  return manifest
}

function assertManifestContainsNoSecrets(value, pathParts = []) {
  if (!value || typeof value !== 'object') return
  for (const [key, child] of Object.entries(value)) {
    const currentPath = [...pathParts, key]
    if (SENSITIVE_KEY_PATTERN.test(key)) {
      throw blocked(
        BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE,
        `fixture manifest 禁止携带敏感字段：${currentPath.join('.')}`
      )
    }
    assertManifestContainsNoSecrets(child, currentPath)
  }
}

function collectCleanupConfig() {
  const fixtureManifestPath = envValue('FAS_FIXTURE_MANIFEST')
  const orchestratorExecutable = envValue('FAS_ORCHESTRATOR_EXECUTABLE')
  const orchestratorScript = envValue('FAS_ORCHESTRATOR_SCRIPT')
  if (!fixtureManifestPath || !orchestratorExecutable || !orchestratorScript) return undefined
  const fixture = loadFixtureManifest(fixtureManifestPath)
  return {
    fixtureManifestPath,
    orchestratorExecutable,
    orchestratorScript,
    runId: String(fixture.runId),
    tenantId: positiveLong(fixture.tenant.id, 'fixture tenant.id'),
    feedbackCode: String(fixture.context.feedbackCode || '')
  }
}

function collectConfig() {
  const missing = REQUIRED_ENV.filter((key) => !envValue(key)).map((key) => `${key} 未提供`)
  const fixtureManifestPath = envValue('FAS_FIXTURE_MANIFEST')
  const fixture = fixtureManifestPath && fs.existsSync(fixtureManifestPath)
    ? loadFixtureManifest(fixtureManifestPath)
    : undefined
  const runtimeMode = (envValue('FAS_RUNTIME_MODE') || 'WORKTREE').toUpperCase()

  const config = {
    frontendUrl: normalizeUrl(envValue('FAS_FRONTEND_URL')),
    backendUrl: normalizeUrl(envValue('FAS_BACKEND_URL')),
    runtimeMode,
    evidenceRunId: envValue('FAS_EVIDENCE_RUN_ID'),
    runtimeEvidenceFile: envValue('FAS_RUNTIME_EVIDENCE_FILE'),
    fixtureManifestPath,
    orchestratorExecutable: envValue('FAS_ORCHESTRATOR_EXECUTABLE'),
    orchestratorScript: envValue('FAS_ORCHESTRATOR_SCRIPT'),
    allowedTestTenantIds: parseExactList(envValue('FAS_ALLOWED_TEST_TENANT_IDS')).map((value) => positiveLong(value, '测试租户白名单 ID')),
    allowedTestTenantNames: parseExactList(envValue('FAS_ALLOWED_TEST_TENANT_NAMES')),
    tenantId: fixture ? positiveLong(fixture.tenant.id, 'fixture tenant.id') : undefined,
    tenant: String(fixture?.tenant?.name || '').trim(),
    fixtureMode: String(fixture?.fixtureMode || '').trim(),
    runId: String(fixture?.runId || '').trim(),
    frontlineUsername: envValue('FAS_FRONTLINE_USERNAME'),
    frontlinePassword: envValue('FAS_FRONTLINE_PASSWORD'),
    leaderUsername: envValue('FAS_LEADER_USERNAME'),
    leaderPassword: envValue('FAS_LEADER_PASSWORD'),
    o1ActiveOrderId: fixture ? positiveLong(fixture.orders.o1.activeOrderId, 'fixture O1 activeOrderId') : undefined,
    o1WorkOrderId: fixture ? positiveLong(fixture.orders.o1.workOrderId, 'fixture O1 workOrderId') : undefined,
    o1WorkOrderCode: String(fixture?.orders?.o1?.workOrderCode || '').trim(),
    o2ActiveOrderId: fixture ? positiveLong(fixture.orders.o2.activeOrderId, 'fixture O2 activeOrderId') : undefined,
    o2WorkOrderId: fixture ? positiveLong(fixture.orders.o2.workOrderId, 'fixture O2 workOrderId') : undefined,
    o2WorkOrderCode: String(fixture?.orders?.o2?.workOrderCode || '').trim(),
    routeId: fixture ? positiveLong(fixture.context.routeId, 'fixture routeId') : undefined,
    routeProcessId: fixture ? positiveLong(fixture.context.routeProcessId, 'fixture routeProcessId') : undefined,
    processId: fixture ? positiveLong(fixture.context.processId, 'fixture processId') : undefined,
    actualEmployeeId: fixture ? positiveLong(fixture.context.actualEmployeeId, 'fixture actualEmployeeId') : undefined,
    submitQuantity: fixture ? positiveQuantity(fixture.context.submitQuantity, 'fixture submitQuantity') : undefined,
    o1PlannedQuantity: fixture ? positiveQuantity(fixture.orders.o1.plannedQuantity, 'fixture O1 plannedQuantity') : undefined,
    o2PlannedQuantity: fixture ? positiveQuantity(fixture.orders.o2.plannedQuantity, 'fixture O2 plannedQuantity') : undefined,
    signaturePassword: envValue('FAS_SIGNATURE_PASSWORD'),
    feedbackCode: String(fixture?.context?.feedbackCode || '').trim(),
    headed: envValue('FAS_HEADED') === '1'
  }

  validateConfig(config, fixture, missing)
  return { ...config, fixture, missing }
}

function observeWait(promise) {
  const observed = promise.then(
    (value) => ({ value }),
    (error) => ({ error })
  )
  return async () => {
    const outcome = await observed
    if (outcome.error) throw outcome.error
    return outcome.value
  }
}

function runtimeForMode(runtimeMode) {
  return RUNTIME_PROFILES[String(runtimeMode || '').toUpperCase()]
}

function tenantForMode(runtimeMode) {
  return TENANT_PROFILES[String(runtimeMode || '').toUpperCase()]
}

function validateConfig(config, fixture, missing) {
  const runtime = runtimeForMode(config.runtimeMode)
  const tenantProfile = tenantForMode(config.runtimeMode)
  if (!runtime || !tenantProfile) {
    missing.push('FAS_RUNTIME_MODE 只能是 WORKTREE、POST_MERGE_INT_MAIN 或 ADMIN_TENANT1_INT_MAIN')
  }
  if ((config.frontendUrl || config.backendUrl)
      && runtime
      && (!runtime.frontendUrls.includes(config.frontendUrl)
        || config.backendUrl !== runtime.backendUrl)) {
    missing.push(`${config.runtimeMode} 只允许 ${runtime.frontendUrls.join(' / ')} 与 ${runtime.backendUrl}`)
  }
  if (!fixture) {
    missing.push('FAS_FIXTURE_MANIFEST 必须指向已存在的外部 fixture manifest')
  }
  if (config.tenantId && !config.allowedTestTenantIds.includes(config.tenantId)) {
    missing.push('fixture tenant.id 未命中 FAS_ALLOWED_TEST_TENANT_IDS 显式白名单')
  }
  if (config.tenant && !config.allowedTestTenantNames.includes(config.tenant)) {
    missing.push('fixture tenant.name 未命中 FAS_ALLOWED_TEST_TENANT_NAMES 显式白名单')
  }
  if (tenantProfile && (config.allowedTestTenantIds.length !== 1
      || !samePrerequisiteLongId(config.allowedTestTenantIds[0], tenantProfile.id))) {
    missing.push(`FAS_ALLOWED_TEST_TENANT_IDS 必须且只能是当前模式租户 ${tenantProfile.id}`)
  }
  if (tenantProfile && (config.allowedTestTenantNames.length !== 1
      || config.allowedTestTenantNames[0] !== tenantProfile.name)) {
    missing.push(`FAS_ALLOWED_TEST_TENANT_NAMES 必须且只能是当前模式租户 ${tenantProfile.name}`)
  }
  if (tenantProfile && config.tenantId && !samePrerequisiteLongId(config.tenantId, tenantProfile.id)) {
    missing.push(`fixture tenant.id 必须是当前模式租户 ${tenantProfile.id}`)
  }
  if (tenantProfile && config.tenant && config.tenant !== tenantProfile.name) {
    missing.push(`fixture tenant.name 必须是当前模式租户 ${tenantProfile.name}`)
  }
  if (tenantProfile && config.fixtureMode !== tenantProfile.fixtureMode) {
    missing.push(`fixture.fixtureMode 必须是 ${tenantProfile.fixtureMode}`)
  }
  if (fixture && fixture.accounts.frontlineUsername !== config.frontlineUsername) {
    missing.push('一线登录账号与 fixture manifest 不一致')
  }
  if (fixture && fixture.accounts.leaderUsername !== config.leaderUsername) {
    missing.push('生产组长登录账号与 fixture manifest 不一致')
  }
  const adminSupplementMode = config.runtimeMode === 'ADMIN_TENANT1_INT_MAIN'
  if (adminSupplementMode) {
    try {
      if (!config.evidenceRunId) throw new Error('missing')
      if (artifactDirFor(config.runtimeMode, config.evidenceRunId) !== ARTIFACT_DIR) throw new Error('mismatch')
    } catch {
      missing.push('ADMIN_TENANT1_INT_MAIN 必须提供合法的 FAS_EVIDENCE_RUN_ID 并使用本轮独立证据目录')
    }
    if (config.frontlineUsername !== 'admin' || config.leaderUsername !== 'admin') {
      missing.push('ADMIN_TENANT1_INT_MAIN 的一线与组长登录账号必须且只能是 admin')
    }
    if (!fixture?.protectedBaseline?.fingerprint) {
      missing.push('ADMIN_TENANT1_INT_MAIN fixture 必须携带不泄密的 admin 受保护基线指纹')
    }
  } else {
    for (const [key, username] of [
      ['FAS_FRONTLINE_USERNAME', config.frontlineUsername],
      ['FAS_LEADER_USERNAME', config.leaderUsername]
    ]) {
      if (username && (!/^[A-Za-z0-9]+$/.test(username) || username.toLowerCase() === 'admin')) {
        missing.push(`${key} 必须是非 admin 的数字字母测试账号`)
      }
    }
    if (config.frontlineUsername && config.frontlineUsername === config.leaderUsername) {
      missing.push('一线账号与生产组长账号必须独立')
    }
  }
  if (config.o1ActiveOrderId && samePrerequisiteLongId(config.o1ActiveOrderId, config.o2ActiveOrderId)) {
    missing.push('O1 与 O2 的 activeOrderId 必须不同')
  }
  if (config.o1WorkOrderId && samePrerequisiteLongId(config.o1WorkOrderId, config.o2WorkOrderId)) {
    missing.push('O1 与 O2 的 workOrderId 必须不同')
  }
  if (config.submitQuantity && config.o1PlannedQuantity && config.submitQuantity <= config.o1PlannedQuantity) {
    missing.push('FAS_SUBMIT_QUANTITY 必须大于 FAS_O1_PLANNED_QUANTITY，以形成订单级超量')
  }
  const o2Target = (config.submitQuantity || 0) - (config.o1PlannedQuantity || 0)
  if (config.o2PlannedQuantity && o2Target > config.o2PlannedQuantity) {
    missing.push('FAS_O2_PLANNED_QUANTITY 必须足以承接从 O1 调出的数量')
  }
  for (const [key, value] of [
    ['fixture O1 workOrderCode', config.o1WorkOrderCode],
    ['fixture O2 workOrderCode', config.o2WorkOrderCode],
    ['fixture feedbackCode', config.feedbackCode]
  ]) {
    if (value && (!value.startsWith(DATA_PREFIX) || !value.includes(config.runId))) {
      missing.push(`${key} 必须包含当前任务 runId=${config.runId}，证明数据归当前任务所有`)
    }
  }
  if (config.orchestratorScript && !fs.existsSync(config.orchestratorScript)) {
    missing.push(`FAS_ORCHESTRATOR_SCRIPT 不存在：${config.orchestratorScript}`)
  }
}

function normalizeWindowsPath(value) {
  return path.resolve(String(value || '')).replace(/\\+$/, '').toLowerCase()
}

function isPathInside(root, candidate) {
  const relative = path.relative(path.resolve(root), path.resolve(candidate))
  return relative === '' || (!relative.startsWith('..') && !path.isAbsolute(relative))
}

function runCommand(executable, args, label, category, options = {}) {
  const result = spawnSync(executable, args, {
    cwd: options.cwd,
    encoding: 'utf8',
    timeout: options.timeout || 30000,
    windowsHide: true
  })
  if (result.error || result.status !== 0) {
    const detail = String(result.stderr || result.stdout || result.error?.message || '').trim()
    throw blocked(category, `${label}失败${detail ? `：${detail}` : ''}`, result.error)
  }
  return String(result.stdout || '').trim()
}

function readSourceVersionEvidence(workspaceRoot) {
  const sourcePaths = [
    'IntRuoyiBackend/yudao-module-mes/src/main',
    'IntRuoyiBackend/yudao-module-mes/src/main/resources',
    'IntRuoyiFronted/src'
  ]
  const category = BLOCKED_CATEGORIES.RUNTIME_EVIDENCE_PREREQUISITE
  const sourceRevision = runCommand(
    'git.exe',
    ['-C', workspaceRoot, 'rev-parse', 'HEAD'],
    '读取运行源码 revision',
    category
  )
  const sourceDiff = runCommand(
    'git.exe',
    ['-C', workspaceRoot, 'diff', '--binary', 'HEAD', '--', ...sourcePaths],
    '读取运行源码差异',
    category
  )
  const sourceStatus = runCommand(
    'git.exe',
    ['-C', workspaceRoot, 'status', '--porcelain=v1', '--', ...sourcePaths],
    '读取运行源码状态',
    category
  )
  const sourceFingerprintSha256 = crypto.createHash('sha256')
    .update(sourceRevision)
    .update('\0')
    .update(sourceStatus)
    .update('\0')
    .update(sourceDiff)
    .digest('hex')
  return { sourceRevision, sourceFingerprintSha256 }
}

function readWindowsPortOwner(port) {
  const powershell = [
    `$connection = Get-NetTCPConnection -State Listen -LocalPort ${port} -ErrorAction Stop | Select-Object -First 1`,
    'if (-not $connection) { exit 3 }',
    '$process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction Stop',
    '[pscustomobject]@{ pid = [int]$connection.OwningProcess; commandLine = [string]$process.CommandLine; executablePath = [string]$process.ExecutablePath } | ConvertTo-Json -Compress'
  ].join('; ')
  const output = runCommand(
    'powershell.exe',
    ['-NoProfile', '-NonInteractive', '-Command', powershell],
    `核验端口 ${port} 监听归属`,
    BLOCKED_CATEGORIES.RUNTIME_EVIDENCE_PREREQUISITE
  )
  return parseJsonPreservingLongIds(
    output,
    `端口 ${port} 监听归属`,
    BLOCKED_CATEGORIES.RUNTIME_EVIDENCE_PREREQUISITE
  )
}

function sha256File(filePath) {
  if (!filePath || !fs.existsSync(filePath)) {
    throw blocked(BLOCKED_CATEGORIES.RUNTIME_EVIDENCE_PREREQUISITE, `运行后端产物不存在：${filePath || '--'}`)
  }
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function validateRuntimeEvidence(config) {
  const category = BLOCKED_CATEGORIES.RUNTIME_EVIDENCE_PREREQUISITE
  const runtime = runtimeForMode(config.runtimeMode)
  if (!runtime) throw blocked(category, `不支持的运行模式：${config.runtimeMode}`)
  const evidence = loadJsonFile(config.runtimeEvidenceFile, '运行态证据', category)
  const required = [
    ['schemaVersion', evidence.schemaVersion],
    ['mode', evidence.mode],
    ['workspaceRoot', evidence.workspaceRoot],
    ['frontendUrl', evidence.frontendUrl],
    ['backendUrl', evidence.backendUrl],
    ['frontendPid', evidence.frontendPid],
    ['backendPid', evidence.backendPid],
    ['sourceRevision', evidence.sourceRevision],
    ['sourceFingerprintSha256', evidence.sourceFingerprintSha256],
    ['backendArtifactPath', evidence.backendArtifactPath],
    ['backendArtifactSha256', evidence.backendArtifactSha256]
  ]
  const missing = required.filter(([, value]) => value === undefined || value === null || String(value).trim() === '')
  if (missing.length) throw blocked(category, `运行态证据缺少字段：${missing.map(([key]) => key).join(', ')}`)
  if (evidence.schemaVersion !== 'fas-runtime-evidence-v1') {
    throw blocked(category, `运行态证据版本不受支持：${evidence.schemaVersion}`)
  }
  if (evidence.mode !== config.runtimeMode) {
    throw blocked(category, `运行态证据 mode 必须是 ${config.runtimeMode}`)
  }
  if (normalizeWindowsPath(evidence.workspaceRoot) !== normalizeWindowsPath(runtime.workspaceRoot)) {
    throw blocked(category, `运行态证据 workspaceRoot 不属于 ${runtime.workspaceRoot}`)
  }
  if (evidence.frontendUrl !== config.frontendUrl || evidence.backendUrl !== config.backendUrl) {
    throw blocked(category, '运行态证据 URL 与本次成对 URL 不一致')
  }
  const currentSource = readSourceVersionEvidence(runtime.workspaceRoot)
  if (evidence.sourceRevision !== currentSource.sourceRevision
      || evidence.sourceFingerprintSha256 !== currentSource.sourceFingerprintSha256) {
    throw blocked(category, '运行态证据与当前源码 revision/工作树指纹不一致')
  }
  const frontendOwner = readWindowsPortOwner(runtime.frontendPort)
  const backendOwner = readWindowsPortOwner(runtime.backendPort)
  if (frontendOwner.pid !== Number(evidence.frontendPid) || backendOwner.pid !== Number(evidence.backendPid)) {
    throw blocked(category, '运行态证据 PID 与当前监听 PID 不一致')
  }
  const expectedRoot = normalizeWindowsPath(runtime.workspaceRoot)
  if (!String(frontendOwner.commandLine || '').toLowerCase().includes(expectedRoot)) {
    throw blocked(category, '前端监听进程命令行不属于目标 workspaceRoot')
  }
  if (!isPathInside(runtime.workspaceRoot, evidence.backendArtifactPath)) {
    throw blocked(category, '后端运行产物不在目标 workspaceRoot 内')
  }
  const artifactPath = normalizeWindowsPath(evidence.backendArtifactPath)
  if (!String(backendOwner.commandLine || '').toLowerCase().includes(artifactPath)) {
    throw blocked(category, '后端监听进程未运行证据声明的产物')
  }
  if (sha256File(evidence.backendArtifactPath) !== String(evidence.backendArtifactSha256).toLowerCase()) {
    throw blocked(category, '后端运行产物 SHA-256 与运行态证据不一致')
  }
  return {
    mode: evidence.mode,
    workspaceRoot: evidence.workspaceRoot,
    frontendPid: frontendOwner.pid,
    backendPid: backendOwner.pid,
    sourceRevision: currentSource.sourceRevision,
    sourceFingerprintSha256: currentSource.sourceFingerprintSha256,
    backendArtifactSha256: String(evidence.backendArtifactSha256).toLowerCase()
  }
}

function writeScenarioState(config, state) {
  ensureArtifactDir()
  const payload = {
    schemaVersion: 'fas-scenario-state-v1',
    taskId: TASK_ID,
    runId: config.runId,
    tenantId: config.tenantId,
    feedbackCode: config.feedbackCode,
    eventId: state.eventId || null,
    stage: state.stage,
    updatedAt: new Date().toISOString()
  }
  fs.writeFileSync(SCENARIO_STATE_FILE, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return payload
}

function runExternalOrchestrator(config, action, resultName, category) {
  ensureArtifactDir()
  const resultFile = path.join(ARTIFACT_DIR, resultName)
  if (fs.existsSync(resultFile)) fs.unlinkSync(resultFile)
  const args = [
    config.orchestratorScript,
    action,
    '--manifest', config.fixtureManifestPath,
    '--scenario-state', SCENARIO_STATE_FILE,
    '--result', resultFile
  ]
  runCommand(
    config.orchestratorExecutable,
    args,
    `外部编排 ${action}`,
    category,
    { timeout: 300000 }
  )
  return loadJsonFile(resultFile, `外部编排 ${action} 结果`, category)
}

function verifyExternalFixture(config) {
  writeScenarioState(config, { stage: 'FIXTURE_VERIFY' })
  const result = runExternalOrchestrator(
    config,
    'verify',
    'fixture-verification.json',
    BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE
  )
  const protectedBaselineReady = config.runtimeMode !== 'ADMIN_TENANT1_INT_MAIN'
    || (result.protectedBaselineVerified === true
      && result.protectedBaselineFingerprint === config.fixture.protectedBaseline.fingerprint)
  if (result.status !== 'READY'
      || result.fixtureVerified !== true
      || result.permissionsVerified !== true
      || result.taskDataVerified !== true
      || result.cleanupReady !== true
      || result.taskId !== TASK_ID
      || result.runId !== config.runId
      || !samePrerequisiteLongId(result.tenantId, config.tenantId)
      || !protectedBaselineReady) {
    throw blocked(BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE, '外部 fixture 验证未证明账号、权限、任务数据和清理前置全部 READY')
  }
  return result
}

function runExternalCleanup(config) {
  const result = runExternalOrchestrator(
    config,
    'cleanup',
    'cleanup-result.json',
    BLOCKED_CATEGORIES.CLEANUP_PREREQUISITE
  )
  const remainingTaskDataCount = Number(result.remainingTaskDataCount)
  const protectedBaselineClean = config.runtimeMode !== 'ADMIN_TENANT1_INT_MAIN'
    || (result.protectedBaselineVerified === true
      && result.protectedBaselineFingerprintBefore === config.fixture.protectedBaseline.fingerprint
      && result.protectedBaselineFingerprintAfter === config.fixture.protectedBaseline.fingerprint)
  if (!(result.status === 'CLEAN')
      || result.cleanupPerformed !== true
      || result.cleanupVerified !== true
      || !Number.isSafeInteger(remainingTaskDataCount)
      || remainingTaskDataCount !== 0
      || result.taskId !== TASK_ID
      || result.runId !== config.runId
      || !samePrerequisiteLongId(result.tenantId, config.tenantId)
      || !protectedBaselineClean) {
    throw blocked(BLOCKED_CATEGORIES.CLEANUP_PREREQUISITE, '外部清理未证明 cleanupVerified=true 且 remainingTaskDataCount=0')
  }
  return { ...result, remainingTaskDataCount }
}

function isVerifiedCleanCleanup(result) {
  const remainingTaskDataCount = Number(result?.remainingTaskDataCount)
  return result?.status === 'CLEAN'
    && result.cleanupPerformed === true
    && result.cleanupVerified === true
    && Number.isSafeInteger(remainingTaskDataCount)
    && remainingTaskDataCount === 0
}

function ensureArtifactDir() {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
}

function collectSensitiveValues(value, values = new Set(), sensitiveContext = false) {
  if (!value || typeof value !== 'object') return values
  for (const [key, child] of Object.entries(value)) {
    const childSensitive = sensitiveContext || SENSITIVE_KEY_PATTERN.test(key)
    if (child && typeof child === 'object') {
      collectSensitiveValues(child, values, childSensitive)
    } else if (childSensitive) {
      if ((typeof child === 'string' || typeof child === 'number') && String(child).length >= 4) {
        values.add(String(child))
      }
    }
  }
  return values
}

function redactEvidence(value) {
  const sensitiveValues = [...collectSensitiveValues(value)].sort((left, right) => right.length - left.length)

  function visit(child, key) {
    if (SENSITIVE_KEY_PATTERN.test(String(key || ''))) return '[REDACTED]'
    if (Array.isArray(child)) return child.map((item) => visit(item, ''))
    if (child && typeof child === 'object') {
      return Object.fromEntries(Object.entries(child).map(([childKey, item]) => [childKey, visit(item, childKey)]))
    }
    if (typeof child !== 'string') return child
    return sensitiveValues.reduce(
      (redacted, sensitiveValue) => redacted.split(sensitiveValue).join('[REDACTED]'),
      child
    )
  }

  return visit(value, '')
}

function writeEvidence(result) {
  ensureArtifactDir()
  const safe = redactEvidence({ ...result, config: result.config || {} })
  fs.writeFileSync(path.join(ARTIFACT_DIR, 'result.json'), `${JSON.stringify(safe, null, 2)}\n`, 'utf8')
  const lines = [
    '# 一线活跃订单自动分配真实 E2E 证据',
    '',
    `- Status: \`${safe.status}\``,
    `- Generated At: \`${new Date().toISOString()}\``,
    `- Reason: ${safe.reason || '--'}`,
    `- Event ID: \`${safe.eventId || '--'}\``,
    `- O1: \`${safe.config.o1WorkOrderCode || '--'}\``,
    `- O2: \`${safe.config.o2WorkOrderCode || '--'}\``,
    `- Blocked Category: \`${safe.blockedCategory || '--'}\``,
    `- Target Business Write Requests: \`${safe.writeRequestCount ?? 0}\``,
    `- Cleanup Status: \`${safe.cleanupResult?.status || '--'}\``,
    `- Remaining Task Data: \`${safe.cleanupResult?.remainingTaskDataCount ?? '--'}\``,
    '',
    '## BDD',
    '',
    '- Given 一线员工选择任务自有活跃订单 O1，O1 正式计划数量小于提交数量。',
    '- When 一线员工通过真实页面正式提交，生产组长打开报工管理并把超量部分改配至 O2。',
    '- Then 初始分配必须是 O1 全量且红色待调整，改配后必须是 O1/O2 两行、红色消失并留下版本审计。',
    '',
    '## Steps',
    ''
  ]
  for (const step of safe.steps || []) lines.push(`- ${step}`)
  if (safe.missing?.length) {
    lines.push('', '## Missing Prerequisites', '')
    for (const item of safe.missing) lines.push(`- ${item}`)
  }
  fs.writeFileSync(path.join(ARTIFACT_DIR, 'evidence.md'), `${lines.join('\n')}\n`, 'utf8')
}

function requestText(url, options = {}) {
  return new Promise((resolve, reject) => {
    const target = new URL(url)
    const client = target.protocol === 'https:' ? https : http
    const request = client.request(target, {
      method: options.method || 'GET',
      headers: options.headers || {}
    }, (response) => {
      const chunks = []
      response.on('data', (chunk) => chunks.push(Buffer.from(chunk)))
      response.on('end', () => {
        const status = Number(response.statusCode || 0)
        resolve({
          ok: status >= 200 && status < 300,
          status,
          text: Buffer.concat(chunks).toString('utf8')
        })
      })
    })
    request.setTimeout(options.timeout || 30000, () => request.destroy(new Error(`HTTP 请求超时：${url}`)))
    request.on('error', reject)
    if (options.body !== undefined) request.write(options.body)
    request.end()
  })
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw blocked(
      BLOCKED_CATEGORIES.BROWSER_UNAVAILABLE,
      '缺少 Playwright runtime；请在 IntRuoyiFronted 按锁文件安装依赖',
      error
    )
  }
}

async function assertHttpOk(url, label) {
  try {
    const response = await requestText(url)
    if (!response.ok) {
      throw blocked(
        BLOCKED_CATEGORIES.SERVICE_UNREACHABLE,
        `${label}不可用：${url} -> HTTP ${response.status}`
      )
    }
  } catch (error) {
    if (error instanceof E2EBlockedError) throw error
    throw blocked(BLOCKED_CATEGORIES.SERVICE_UNREACHABLE, `${label}不可达：${url}`, error)
  }
}

async function readPlaywrightJson(response, label) {
  return parseJsonPreservingLongIds(await response.text(), label)
}

async function fillFirst(root, selectors, value) {
  for (const selector of selectors) {
    const locator = root.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`找不到可填写控件：${selectors.join(', ')}`)
}

async function clickFirst(root, selectors) {
  for (const selector of selectors) {
    const locator = root.locator(`${selector}:visible`).first()
    if (await locator.count()) {
      await locator.click()
      return
    }
  }
  throw new Error(`找不到可点击控件：${selectors.join(', ')}`)
}

async function selectLoginTenant(page, tenant) {
  const input = page.locator('.el-select input:visible').first()
  await input.waitFor({ state: 'visible', timeout: 15000 })
  await input.click()
  await input.fill(tenant)
  const option = page.locator('.el-select-dropdown__item:visible', { hasText: tenant }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function waitForLoginFormShell(page) {
  await page.locator('.login-form').first().waitFor({ state: 'visible', timeout: 60000 })
}

async function login(page, frontendUrl, tenant, username, password) {
  await page.goto(`${frontendUrl}/login?redirect=/index`, { waitUntil: 'domcontentloaded' })
  await waitForLoginFormShell(page)
  await selectLoginTenant(page, tenant)
  await fillFirst(page, [
    'input[placeholder*="账号"]',
    'input[placeholder*="用户名"]',
    'input[name="username"]'
  ], username)
  await fillFirst(page, [
    'input[placeholder*="密码"]',
    'input[type="password"]',
    'input[name="password"]'
  ], password)
  const loginResponseWait = observeWait(page.waitForResponse((response) =>
    response.url().includes('/admin-api/system/auth/login')
      && response.request().method() === 'POST'
  , { timeout: 60000 }))
  await clickFirst(page, ['button:has-text("登录")', '.login-form button[type="submit"]'])
  const loginResponse = await loginResponseWait()
  assert.equal(loginResponse.ok(), true, `登录 HTTP 失败：${loginResponse.status()}`)
  const loginBody = await readPlaywrightJson(loginResponse, '登录响应')
  assert.equal(loginBody.code, 0, `登录业务失败：${loginBody.msg || loginBody.message || 'unknown'}`)
  if (new URL(page.url()).pathname.includes('/login')) {
    await page.waitForURL((url) => !url.pathname.includes('/login'), {
      timeout: 30000,
      waitUntil: 'commit'
    })
  }
  assert.equal(new URL(page.url()).pathname.includes('/login'), false, '登录成功后页面必须离开登录路由')
}

async function loginWithPrerequisiteClassification(page, frontendUrl, tenant, username, password, actor) {
  return blockedPrerequisite(
    BLOCKED_CATEGORIES.LOGIN_PREREQUISITE,
    `${actor}真实登录前置不满足`,
    () => login(page, frontendUrl, tenant, username, password)
  )
}

function attachDiagnostics(page, diagnostics) {
  page.on('pageerror', (error) => diagnostics.pageErrors.push(error.message || String(error)))
  page.on('console', (message) => {
    if (message.type() === 'error') diagnostics.consoleErrors.push(message.text())
  })
  page.on('requestfailed', (request) => {
    const failure = {
      method: request.method(),
      url: request.url(),
      errorText: request.failure()?.errorText || 'unknown request failure'
    }
    diagnostics.requestFailures.push(failure)
    if (request.url().includes('/admin-api/mes/pro/feedback/frontline/submit')
        || request.url().includes('/admin-api/mes/pro/process-pool/team-leader/')) {
      diagnostics.targetRequestFailures.push(failure)
    }
  })
  page.on('response', (response) => {
    if (response.status() >= 400) {
      const responseError = {
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
        statusText: response.statusText()
      }
      diagnostics.responseErrors.push(responseError)
      if (response.url().includes('/admin-api/mes/pro/')) {
        diagnostics.targetResponseErrors.push(`${responseError.method} ${responseError.url} -> ${responseError.status}`)
      }
    }
  })
  page.on('request', (request) => {
    const targetBusinessWrite = request.method() === 'POST'
      && (request.url().includes('/mes/pro/feedback/frontline/submit')
        || request.url().includes('/mes/pro/process-pool/team-leader/submission/allocation/confirm'))
    if (targetBusinessWrite) {
      diagnostics.writeRequests.push({ method: request.method(), pathname: new URL(request.url()).pathname })
    }
  })
}

async function waitForVisible(locator, timeout = 1000) {
  try {
    await locator.waitFor({ state: 'visible', timeout })
    return true
  } catch {
    return false
  }
}

async function ensureFrontlineProductionProcessSelected(page) {
  const selectedProcess = page.locator('[data-frontline-production-process-nav-card] .frontline-production-process-current')
    .filter({ hasNotText: '未选择' })
  if (await waitForVisible(selectedProcess, 30000)) {
    return
  }

  await page.locator('[data-frontline-production-process-nav-card]').click()
  const picker = page.locator('[aria-label="选择工序"]')
  await picker.waitFor({ state: 'visible', timeout: 15000 })
  const processOption = picker.getByRole('button').filter({ hasText: 'FAS E2E工序' }).first()
  await processOption.waitFor({ state: 'visible', timeout: 30000 })
  await processOption.click()
  await selectedProcess.waitFor({ state: 'visible', timeout: 30000 })
}

async function ensureFrontlineProductionEmployeeSelected(page) {
  const selectedEmployee = page.locator('[data-frontline-production-employee-card]')
    .filter({ hasNotText: '未选择' })
  if (await waitForVisible(selectedEmployee, 30000)) {
    return
  }

  await page.locator('[data-frontline-production-employee-card]').click()
  const picker = page.locator('[aria-label="选择员工"]')
  await picker.waitFor({ state: 'visible', timeout: 15000 })
  const employeeOption = picker.getByRole('button').filter({ hasText: 'FAS E2E一线员工' }).first()
  await employeeOption.waitFor({ state: 'visible', timeout: 30000 })
  await employeeOption.click()
  await selectedEmployee.waitFor({ state: 'visible', timeout: 30000 })
}

async function selectFrontlineActiveOrder(page, config) {
  const selectedOrder = page.locator('[data-frontline-production-order-code]')
    .filter({ hasText: config.o1WorkOrderCode })
    .first()
  await selectedOrder.waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-process-nav-card] .frontline-production-process-current')
    .filter({ hasNotText: '未选择' })
    .waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-employee-card]')
    .filter({ hasNotText: '未选择' })
    .waitFor({ state: 'visible', timeout: 30000 })
  const activeOrderCard = page.locator('[data-frontline-production-active-order-card]')
  await activeOrderCard.waitFor({ state: 'visible', timeout: 30000 })
  await activeOrderCard.click()
  const picker = page.locator('[aria-label="选择活跃订单"]')
  await picker.waitFor({ state: 'visible', timeout: 15000 })
  await picker.locator('[data-frontline-production-order-search-input]').fill(config.o1WorkOrderCode)
  const option = picker.getByRole('button').filter({ hasText: config.o1WorkOrderCode }).first()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
  await selectedOrder.waitFor({ state: 'visible', timeout: 15000 })
  await ensureFrontlineProductionProcessSelected(page)
  await ensureFrontlineProductionEmployeeSelected(page)
}

async function submitFrontlineReport(page, config, steps) {
  const query = new URLSearchParams({
    workOrderId: String(config.o1WorkOrderId),
    routeId: String(config.routeId),
    routeProcessId: String(config.routeProcessId),
    processId: String(config.processId),
    actualEmployeeId: String(config.actualEmployeeId),
    outputQuantity: String(config.submitQuantity)
  })
  await page.goto(`${config.frontendUrl}${FRONTLINE_ROUTE}?${query}`, { waitUntil: 'domcontentloaded' })
  await page.locator('[data-frontline-production-operator]').waitFor({ state: 'visible', timeout: 20000 })
  await selectFrontlineActiveOrder(page, config)
  steps.push(`一线页面明确选择 O1：${config.o1WorkOrderCode}`)

  const output = page.locator('#frontlineProductionOutputQuantity')
  await output.fill(String(config.submitQuantity))
  await page.locator('.frontline-production-submit-button').click()
  const dialog = page.locator('[data-production-submit-confirmation-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 15000 })
  await dialog.locator('[data-production-submit-signature-password]').fill(config.signaturePassword)

  const requestPromise = page.waitForRequest((request) =>
    request.url().includes('/mes/pro/feedback/frontline/submit') && request.method() === 'POST'
  , { timeout: 30000 })
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/mes/pro/feedback/frontline/submit') && response.request().method() === 'POST'
  , { timeout: 30000 })
  await dialog.locator('[data-production-submit-confirm-accept]').click()
  const [request, response] = await Promise.all([requestPromise, responsePromise])
  const payload = parseJsonPreservingLongIds(request.postData() || '{}', '一线正式提交请求')
  assert.equal(
    exactLongId(payload.processPoolContext.activeOrderId, '提交载荷 activeOrderId'),
    config.o1ActiveOrderId,
    '一线正式提交必须精确携带 O1 activeOrderId'
  )
  assert.equal(response.ok(), true, `一线正式提交 HTTP 失败：${response.status()}`)
  const body = await readPlaywrightJson(response, '一线正式提交响应')
  assert.equal(body.code, 0, `一线正式提交业务失败：${body.msg || body.message || 'unknown'}`)
  const eventId = exactLongId(body.data?.processPoolEventId, 'processPoolEventId')
  writeScenarioState(config, { stage: 'FRONTLINE_SUBMITTED', eventId })
  await page.locator('[data-production-submit-success-dialog]').waitFor({ state: 'visible', timeout: 20000 })
  steps.push(`一线超量提交成功，事件 ID=${eventId}`)
  return eventId
}

function assertSnapshotLine(snapshot, activeOrderId, expectedQuantity, expectedOverage, expectedNeedsAdjustment) {
  const line = snapshot.lines.find((item) => sameLongId(item.activeOrderId, activeOrderId))
  assert.ok(line, `正式分配快照缺少 activeOrderId=${activeOrderId}`)
  assert.equal(Number(line.allocatedQuantity), expectedQuantity, '正式分配数量不符合预期')
  assert.equal(Number(line.overageQuantity), expectedOverage, '正式订单超量数量不符合预期')
  assert.equal(line.needsAdjustment, expectedNeedsAdjustment, '正式订单待调整状态不符合预期')
  return line
}

async function assertInitialAllocation(row, snapshot, config, steps) {
  const expectedOverage = config.submitQuantity - config.o1PlannedQuantity
  assert.equal(Number(snapshot.version), 1, '初始分配版本必须为 1')
  assert.equal(Number(snapshot.poolQuantity), config.submitQuantity, '初始分配池总量必须等于提交数量')
  assert.equal(snapshot.lines.length, 1, '初始分配只能包含一线选中的 O1')
  assert.equal(snapshot.lines[0].allocationMode, 'FRONTLINE_SELECTED', '初始分配模式必须为 FRONTLINE_SELECTED')
  assertSnapshotLine(snapshot, config.o1ActiveOrderId, config.submitQuantity, expectedOverage, true)

  const allocationTag = row.locator('[data-team-leader-report-allocations] .el-tag')
    .filter({ hasText: config.o1WorkOrderCode }).first()
  await allocationTag.waitFor({ state: 'visible', timeout: 15000 })
  const text = await allocationTag.innerText()
  assert.match(text, new RegExp(`${config.submitQuantity}`), '组长列表必须显示 O1 全量初始分配')
  assert.match(await allocationTag.getAttribute('class'), /el-tag--warning/, 'O1 初始分配标签必须表示未放行 warning')
  const allocationOverage = row.page().locator('[data-team-leader-allocation-overage]')
  await allocationOverage.waitFor({ state: 'visible', timeout: 15000 })
  assert.match(await allocationOverage.innerText(), new RegExp(`待调整\\s*${expectedOverage}`))
  assert.match(await allocationOverage.getAttribute('class'), /el-tag--danger/, '分配弹窗必须用红色标识正式超量待调整')
  steps.push(`组长分配弹窗确认 O1=${config.submitQuantity}，红色待调整 ${expectedOverage}`)
}

async function assertActiveOrderQuantityConflict(page, config, steps) {
  const activeOrderTab = page.getByRole('tab', { name: '活跃订单池', exact: true }).first()
  await activeOrderTab.waitFor({ state: 'visible', timeout: 15000 })
  await activeOrderTab.click()

  const list = page.locator('[data-team-leader-active-order-list]')
  await list.waitFor({ state: 'visible', timeout: 30000 })
  const row = list.locator('tbody tr').filter({ hasText: config.o1WorkOrderCode }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })

  const conflictTag = row.locator('[data-team-leader-active-order-quantity-conflict]')
  await conflictTag.waitFor({ state: 'visible', timeout: 30000 })
  assert.match(await conflictTag.innerText(), /数量冲突/)
  assert.match(
    await conflictTag.innerText(),
    new RegExp(String(config.submitQuantity - config.o1PlannedQuantity))
  )
  assert.match(
    await row.getAttribute('class'),
    /team-leader-workbench__active-order-row--quantity-conflict/,
    '数量冲突时活跃订单整行必须标红'
  )

  await row.locator('[data-team-leader-active-order-detail]').click()
  const detail = page.locator('[data-team-leader-active-order-detail-dialog]')
  await detail.waitFor({ state: 'visible', timeout: 30000 })
  const process = detail.locator('section.is-quantity-conflict').filter({ hasText: 'FAS E2E工序' }).first()
  await process.waitFor({ state: 'visible', timeout: 30000 })
  assert.match(await process.innerText(), /超出数量/)
  const submissionRows = process.locator('.team-leader-workbench__active-order-submission-table tbody tr')
  const submissionCount = await submissionRows.count()
  assert.ok(submissionCount > 0, '数量冲突工序必须展示相关提交明细')
  for (let index = 0; index < submissionCount; index += 1) {
    assert.match(
      await submissionRows.nth(index).getAttribute('class'),
      /team-leader-workbench__active-order-submission-row--quantity-conflict/,
      '数量冲突工序的全部提交明细必须标红'
    )
  }

  const releaseButton = row.locator('[data-team-leader-active-order-release-apply]').first()
  assert.equal(await releaseButton.isDisabled(), true, '数量冲突时完工按钮必须禁用')
  assert.match(await releaseButton.getAttribute('title'), /生产数量冲突未解决/)
  await detail.locator('.el-dialog__headerbtn').click()
  await detail.waitFor({ state: 'hidden', timeout: 15000 })
  steps.push('活跃订单池确认整行、冲突工序、全部提交明细均已标红，完工按钮已被数量冲突禁用')
}

async function assertActiveOrderQuantityConflictResolved(page, config, steps) {
  const activeOrderTab = page.getByRole('tab', { name: '活跃订单池', exact: true }).first()
  await activeOrderTab.click()
  const list = page.locator('[data-team-leader-active-order-list]')
  await list.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction((workOrderCode) => {
    const rows = [...document.querySelectorAll('[data-team-leader-active-order-list] tbody tr')]
    const row = rows.find((candidate) => candidate.textContent?.includes(workOrderCode))
    return Boolean(
      row
        && !row.querySelector('[data-team-leader-active-order-quantity-conflict]')
        && !row.className.includes('team-leader-workbench__active-order-row--quantity-conflict')
    )
  }, config.o1WorkOrderCode, { timeout: 30000 })

  const row = list.locator('tbody tr').filter({ hasText: config.o1WorkOrderCode }).first()
  await row.locator('[data-team-leader-active-order-detail]').click()
  const detail = page.locator('[data-team-leader-active-order-detail-dialog]')
  await detail.waitFor({ state: 'visible', timeout: 30000 })
  const process = detail.locator('section').filter({ hasText: 'FAS E2E工序' }).first()
  await process.waitFor({ state: 'visible', timeout: 30000 })
  assert.doesNotMatch(
    await process.getAttribute('class') || '',
    /is-quantity-conflict/,
    '组长改配到目标数量后工序冲突标识必须消失'
  )
  await detail.locator('.el-dialog__headerbtn').click()
  await detail.waitFor({ state: 'hidden', timeout: 15000 })
  steps.push('组长改配后活跃订单池已刷新，O1 数量冲突标识和工序冲突样式均已消失')
}

async function openInitialAllocation(page, eventId) {
  const button = page.locator(`[data-production-report-allocation-event-id="${eventId}"]`)
  await button.waitFor({ state: 'visible', timeout: 30000 })
  const currentPromise = page.waitForResponse((response) =>
    response.url().includes('/submission/allocation/current')
      && response.url().includes(`eventId=${eventId}`)
      && response.request().method() === 'GET'
  , { timeout: 30000 })
  await button.click()
  const response = await currentPromise
  assert.equal(response.ok(), true, `当前分配快照 HTTP 失败：${response.status()}`)
  const body = await readPlaywrightJson(response, '当前分配快照响应')
  assert.equal(body.code, 0, `当前分配快照业务失败：${body.msg || body.message || 'unknown'}`)
  return {
    row: button.locator('xpath=ancestor::tr[1]'),
    dialog: page.locator('.team-leader-workbench__review-dialog:visible'),
    snapshot: body.data
  }
}

async function selectAllocationOrder(page, row, workOrderCode) {
  await row.locator('.el-select__wrapper').click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: workOrderCode }).last()
  await option.waitFor({ state: 'visible', timeout: 15000 })
  await option.click()
}

async function fillAllocationQuantity(row, quantity) {
  const input = row.locator('.el-input-number input')
  await input.fill(String(quantity))
  await input.press('Tab')
}

async function reallocateToSecondOrder(page, dialog, eventId, config, steps) {
  const o2Quantity = config.submitQuantity - config.o1PlannedQuantity
  let rows = dialog.locator('[data-team-leader-allocation-table] tbody tr')
  await rows.first().waitFor({ state: 'visible', timeout: 15000 })
  assert.match(await rows.first().innerText(), new RegExp(config.o1WorkOrderCode))
  await fillAllocationQuantity(rows.first(), config.o1PlannedQuantity)
  await dialog.getByRole('button', { name: '新增分配行', exact: true }).click()
  rows = dialog.locator('[data-team-leader-allocation-table] tbody tr')
  await rows.nth(1).waitFor({ state: 'visible', timeout: 15000 })
  await selectAllocationOrder(page, rows.nth(1), config.o2WorkOrderCode)
  await fillAllocationQuantity(rows.nth(1), o2Quantity)

  const requestWait = observeWait(page.waitForRequest((request) =>
    request.url().includes('/submission/allocation/confirm') && request.method() === 'POST'
  , { timeout: 30000 }))
  const responseWait = observeWait(page.waitForResponse((response) =>
    response.url().includes('/submission/allocation/confirm') && response.request().method() === 'POST'
  , { timeout: 30000 }))
  const postSavePageResponseWait = observeWait(page.waitForResponse((response) =>
    response.url().includes('/submission/page') && response.request().method() === 'GET'
  , { timeout: 30000 }))
  await dialog.getByRole('button', { name: '确认分配', exact: true }).click()
  const [request, response] = await Promise.all([requestWait(), responseWait()])
  const payload = parseJsonPreservingLongIds(request.postData() || '{}', '组长改配请求')
  assert.equal(exactLongId(payload.eventId, '组长改配 eventId'), eventId)
  assert.equal(payload.allocationMode, 'MANUAL')
  assert.equal(Number(payload.expectedVersion), 1)
  const byOrder = new Map(payload.allocations.map((line) => [
    exactLongId(line.activeOrderId, '组长改配 activeOrderId'),
    Number(line.allocatedQuantity)
  ]))
  assert.equal(byOrder.get(config.o1ActiveOrderId), config.o1PlannedQuantity)
  assert.equal(byOrder.get(config.o2ActiveOrderId), o2Quantity)
  assert.equal(response.ok(), true, `组长改配 HTTP 失败：${response.status()}`)
  const body = await readPlaywrightJson(response, '组长改配响应')
  assert.equal(body.code, 0, `组长改配业务失败：${body.msg || body.message || 'unknown'}`)
  assert.equal(Number(body.data.version), 2, '组长第一次改配后版本必须为 2')
  assertSnapshotLine(body.data, config.o1ActiveOrderId, config.o1PlannedQuantity, 0, false)
  assertSnapshotLine(body.data, config.o2ActiveOrderId, o2Quantity, 0, false)
  const postSavePageResponse = await postSavePageResponseWait()
  assert.equal(postSavePageResponse.ok(), true, `改配后报工管理列表 HTTP 失败：${postSavePageResponse.status()}`)
  const postSavePageBody = await readPlaywrightJson(postSavePageResponse, '改配后报工管理列表响应')
  assert.equal(postSavePageBody.code, 0, `改配后报工管理列表业务失败：${postSavePageBody.msg || postSavePageBody.message || 'unknown'}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  steps.push(`组长通过页面改配为 O1=${config.o1PlannedQuantity}、O2=${o2Quantity}`)
  return { snapshot: body.data, listData: postSavePageBody.data }
}

async function getAccessToken(page) {
  const raw = await page.evaluate(() => localStorage.getItem('ACCESS_TOKEN'))
  let token = String(raw || '').trim()
  for (let index = 0; index < 2; index += 1) {
    try {
      const parsed = JSON.parse(token)
      token = typeof parsed === 'string'
        ? parsed
        : String(parsed?.v || parsed?.value || parsed?.accessToken || parsed?.token || token)
    } catch {
      break
    }
  }
  token = token.replace(/^Bearer\s+/i, '').trim()
  assert.ok(token, '登录后必须能读取访问 token 进行最终只读核验')
  return token
}

async function fetchBusinessData(url, token, label) {
  const response = await requestText(url, { headers: { Authorization: `Bearer ${token}` } })
  assert.equal(response.ok, true, `${label} HTTP 失败：${response.status}`)
  const body = parseJsonPreservingLongIds(response.text, `${label}响应`)
  assert.equal(body.code, 0, `${label}业务失败：${body.msg || body.message || 'unknown'}`)
  return body.data
}

async function assertAuditTrail(page, eventId, config, steps) {
  const token = await getAccessToken(page)
  const params = new URLSearchParams({ eventId: String(eventId), leaderType: 'PRODUCTION' })
  const audits = await fetchBusinessData(
    `${config.backendUrl}/admin-api/mes/pro/process-pool/team-leader/submission/allocation/audit?${params}`,
    token,
    '分配审计只读核验'
  )
  assert.ok(Array.isArray(audits), '分配审计必须返回数组')
  const initial = audits.find((item) =>
    Number(item.allocationVersion) === 1
      && item.changeSource === 'INITIAL_BASELINE'
      && sameLongId(item.activeOrderId, config.o1ActiveOrderId)
  )
  assert.ok(initial, '分配审计必须保留 O1 初始分配基线')
  assert.equal(Number(initial.beforeQuantity), 0)
  assert.equal(Number(initial.afterQuantity), config.submitQuantity)
  const manualO1 = audits.find((item) =>
    Number(item.allocationVersion) === 2
      && item.changeSource === 'MANUAL'
      && sameLongId(item.activeOrderId, config.o1ActiveOrderId)
  )
  const manualO2 = audits.find((item) =>
    Number(item.allocationVersion) === 2
      && item.changeSource === 'MANUAL'
      && sameLongId(item.activeOrderId, config.o2ActiveOrderId)
  )
  assert.ok(manualO1 && manualO2, '分配审计必须保留版本 2 的 O1/O2 手工改配记录')
  assert.equal(Number(manualO1.beforeQuantity), config.submitQuantity)
  assert.equal(Number(manualO1.afterQuantity), config.o1PlannedQuantity)
  assert.equal(Number(manualO2.beforeQuantity), 0)
  assert.equal(Number(manualO2.afterQuantity), config.submitQuantity - config.o1PlannedQuantity)
  steps.push('只读核验确认版本 1 初始基线与版本 2 手工改配审计完整')
  return audits
}

function assertAdjustedResponse(listData, eventId, config) {
  assert.ok(Array.isArray(listData?.list), '改配后报工管理列表必须返回分页明细')
  const event = listData.list.find((item) => sameLongId(item.id, eventId))
  assert.ok(event, `改配后报工管理列表缺少事件 ${eventId}`)
  assert.equal(event.reportAllocations.length, 2, '改配后正式列表必须仅包含当前 O1/O2 两条分配')
  assertSnapshotLine({ lines: event.reportAllocations }, config.o1ActiveOrderId, config.o1PlannedQuantity, 0, false)
  assertSnapshotLine({ lines: event.reportAllocations }, config.o2ActiveOrderId, config.submitQuantity - config.o1PlannedQuantity, 0, false)
}

async function assertAdjustedList(page, eventId, config, listData, steps) {
  assertAdjustedResponse(listData, eventId, config)
  const button = page.locator(`[data-production-report-allocation-event-id="${eventId}"]`)
  await button.waitFor({ state: 'visible', timeout: 30000 })
  const row = button.locator('xpath=ancestor::tr[1]')
  const allocations = row.locator('[data-team-leader-report-allocations]')
  await allocations.filter({ hasText: config.o2WorkOrderCode }).waitFor({ state: 'visible', timeout: 30000 })
  const text = await allocations.innerText()
  assert.match(text, new RegExp(`${config.o1WorkOrderCode}[^\n]*${config.o1PlannedQuantity}`))
  assert.match(text, new RegExp(`${config.o2WorkOrderCode}[^\n]*${config.submitQuantity - config.o1PlannedQuantity}`))
  await row.locator('[data-team-leader-report-overage]').waitFor({ state: 'hidden', timeout: 30000 })
  assert.equal(await row.locator('[data-team-leader-report-overage]').count(), 0, '改配完成后列表红色待调整标识必须消失')
  steps.push('组长列表刷新后显示 O1/O2 当前分配，红色待调整标识已消失')
}

function classifyConsoleErrors(consoleErrors, requestFailures, responseErrors = []) {
  const externalFailureMessages = requestFailures.flatMap((failure) => {
    try {
      const url = new URL(failure.url)
      if (!['http:', 'https:'].includes(url.protocol)
          || ['127.0.0.1', 'localhost'].includes(url.hostname)) {
        return []
      }
      return [`Failed to load resource: ${failure.errorText}`]
    } catch {
      return []
    }
  })
  for (const failure of responseErrors) {
    try {
      const url = new URL(failure.url)
      if (!['http:', 'https:'].includes(url.protocol)
          || ['127.0.0.1', 'localhost'].includes(url.hostname)) {
        continue
      }
      externalFailureMessages.push(
        `Failed to load resource: the server responded with a status of ${failure.status} (${failure.statusText})`
      )
    } catch {
      // An unparseable response URL cannot prove that a console error is external.
    }
  }
  const externalMessageCounts = new Map()
  for (const message of externalFailureMessages) {
    externalMessageCounts.set(message, (externalMessageCounts.get(message) || 0) + 1)
  }
  const targetConsoleErrors = []
  const externalResourceConsoleErrors = []
  for (const message of consoleErrors) {
    const available = externalMessageCounts.get(message) || 0
    if (available > 0) {
      externalResourceConsoleErrors.push(message)
      externalMessageCounts.set(message, available - 1)
    } else {
      targetConsoleErrors.push(message)
    }
  }
  return { targetConsoleErrors, externalResourceConsoleErrors }
}

function assertNoTargetErrors(diagnostics) {
  const classifiedConsoleErrors = classifyConsoleErrors(
    diagnostics.consoleErrors,
    diagnostics.requestFailures,
    diagnostics.responseErrors
  )
  diagnostics.targetConsoleErrors = classifiedConsoleErrors.targetConsoleErrors
  diagnostics.externalResourceConsoleErrors = classifiedConsoleErrors.externalResourceConsoleErrors
  assert.deepEqual(diagnostics.pageErrors, [], '真实 E2E 不得出现 pageerror')
  assert.deepEqual(diagnostics.targetConsoleErrors, [], '当前业务页面不得出现 console error')
  assert.deepEqual(diagnostics.targetRequestFailures, [], '当前 MES 业务路径不得出现网络请求失败')
  assert.deepEqual(diagnostics.targetResponseErrors, [], '目标 MES 接口不得出现 HTTP 错误')
}

async function runScenario(config) {
  const { chromium } = loadPlaywright()
  const browser = await blockedPrerequisite(
    BLOCKED_CATEGORIES.BROWSER_UNAVAILABLE,
    'Chromium 浏览器不可启动',
    () => chromium.launch({ headless: !config.headed })
  )
  const steps = []
  const diagnostics = {
    pageErrors: [],
    consoleErrors: [],
    requestFailures: [],
    responseErrors: [],
    targetRequestFailures: [],
    targetResponseErrors: [],
    writeRequests: []
  }
  let eventId
  try {
    const frontlineContext = await browser.newContext({ ignoreHTTPSErrors: true })
    const frontlinePage = await frontlineContext.newPage()
    attachDiagnostics(frontlinePage, diagnostics)
    await loginWithPrerequisiteClassification(
      frontlinePage,
      config.frontendUrl,
      config.tenant,
      config.frontlineUsername,
      config.frontlinePassword,
      '一线账号'
    )
    steps.push('一线测试账号通过真实登录页登录')
    eventId = await submitFrontlineReport(frontlinePage, config, steps)
    await frontlineContext.close()

    const leaderContext = await browser.newContext({ ignoreHTTPSErrors: true })
    const leaderPage = await leaderContext.newPage()
    attachDiagnostics(leaderPage, diagnostics)
    await loginWithPrerequisiteClassification(
      leaderPage,
      config.frontendUrl,
      config.tenant,
      config.leaderUsername,
      config.leaderPassword,
      '生产组长账号'
    )
    steps.push('生产组长测试账号通过真实登录页登录')
    await leaderPage.goto(`${config.frontendUrl}${LEADER_ROUTE}`, { waitUntil: 'domcontentloaded' })
    if (leaderPage.url().includes('/login') || leaderPage.url().includes('/403')) {
      throw blocked(
        BLOCKED_CATEGORIES.PERMISSION_PREREQUISITE,
        `访问组长报工管理后被重定向到 ${leaderPage.url()}`
      )
    }
    const reportTab = leaderPage.getByRole('tab', { name: '报工管理', exact: true }).first()
    await reportTab.waitFor({ state: 'visible', timeout: 15000 })
    await reportTab.click()
    await assertActiveOrderQuantityConflict(leaderPage, config, steps)
    await reportTab.click()
    const opened = await openInitialAllocation(leaderPage, eventId)
    await assertInitialAllocation(opened.row, opened.snapshot, config, steps)
    ensureArtifactDir()
    await leaderPage.screenshot({ path: path.join(ARTIFACT_DIR, 'initial-overage-red.png'), fullPage: true })
    const adjusted = await reallocateToSecondOrder(
      leaderPage,
      opened.dialog,
      eventId,
      config,
      steps
    )
    await assertAdjustedList(leaderPage, eventId, config, adjusted.listData, steps)
    await assertActiveOrderQuantityConflictResolved(leaderPage, config, steps)
    const audits = await assertAuditTrail(leaderPage, eventId, config, steps)
    assertNoTargetErrors(diagnostics)
    await leaderPage.screenshot({ path: path.join(ARTIFACT_DIR, 'after-manual-reallocation.png'), fullPage: true })
    await leaderContext.close()
    return {
      eventId,
      steps,
      diagnostics,
      writeRequestCount: diagnostics.writeRequests.length,
      adjustedSnapshot: adjusted.snapshot,
      auditCount: audits.length
    }
  } catch (error) {
    try {
      ensureArtifactDir()
      const activePage = browser.contexts().find((context) => context.pages().length)?.pages()[0]
      if (activePage) {
        await activePage.screenshot({ path: path.join(ARTIFACT_DIR, 'failure-state.png'), fullPage: true })
      }
    } catch {
      // The original scenario failure remains authoritative.
    }
    error.scenarioEvidence = { eventId, steps: [...steps], diagnostics }
    throw error
  } finally {
    await browser.close()
  }
}

function statusForError(error) {
  return error instanceof E2EBlockedError ? 'BLOCKED' : 'FAIL'
}

function noWriteCleanupEvidence(reason) {
  return {
    status: 'NOT_REQUIRED',
    cleanupPerformed: false,
    cleanupVerified: true,
    remainingTaskDataCount: 0,
    reason
  }
}

function attemptEarlyCleanup(cleanupConfig) {
  if (!cleanupConfig) {
    return {
      cleanupResult: noWriteCleanupEvidence('本轮未识别 fixture，浏览器和目标业务写请求均未启动')
    }
  }
  try {
    writeScenarioState(cleanupConfig, { stage: 'CONFIG_BLOCKED_CLEANUP' })
    return { cleanupResult: runExternalCleanup(cleanupConfig) }
  } catch (error) {
    return { cleanupError: error }
  }
}

async function main() {
  let cleanupConfig
  let cleanupIdentityError
  try {
    cleanupConfig = collectCleanupConfig()
  } catch (error) {
    cleanupIdentityError = error
  }

  let config
  try {
    config = collectConfig()
  } catch (error) {
    const earlyCleanup = cleanupIdentityError
      ? { cleanupError: cleanupIdentityError }
      : attemptEarlyCleanup(cleanupConfig)
    writeEvidence({
      status: 'BLOCKED',
      blockedCategory: error instanceof E2EBlockedError
        ? error.category
        : BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE,
      reason: `真实 E2E 配置无效：${errorText(error)}`,
      config: {},
      steps: [],
      writeRequestCount: 0,
      ...earlyCleanup,
      error: { name: error.name || 'Error', message: errorText(error) }
    })
    process.exitCode = 2
    return
  }
  if (config.missing.length) {
    const earlyCleanup = cleanupIdentityError
      ? { cleanupError: cleanupIdentityError }
      : attemptEarlyCleanup(cleanupConfig)
    writeEvidence({
      status: 'BLOCKED',
      blockedCategory: BLOCKED_CATEGORIES.TASK_DATA_PREREQUISITE,
      reason: '缺少真实写入型 E2E 前置条件',
      missing: config.missing,
      config,
      steps: [],
      writeRequestCount: 0,
      ...earlyCleanup
    })
    process.exitCode = 2
    return
  }

  let runtimeEvidence
  let fixtureVerification
  let scenarioResult
  let executionError
  let cleanupResult
  let cleanupError

  try {
    writeScenarioState(config, { stage: 'PRECONDITION_CHECK' })
    runtimeEvidence = validateRuntimeEvidence(config)
    await assertHttpOk(`${config.frontendUrl}/`, '前端入口')
    await assertHttpOk(`${config.backendUrl}/actuator/health`, '后端健康检查')
    fixtureVerification = verifyExternalFixture(config)
    writeScenarioState(config, { stage: 'SCENARIO_READY' })
    scenarioResult = await runScenario(config)
    writeScenarioState(config, { stage: 'SCENARIO_COMPLETE', eventId: scenarioResult.eventId })
  } catch (error) {
    executionError = error
  } finally {
    try {
      cleanupResult = runExternalCleanup(config)
    } catch (error) {
      cleanupError = error
    }
  }

  if (!executionError && !cleanupError) {
    if (!isVerifiedCleanCleanup(cleanupResult)) {
      cleanupError = blocked(
        BLOCKED_CATEGORIES.CLEANUP_PREREQUISITE,
        'PASS 门禁拒绝未明确证明 CLEAN 且 remainingTaskDataCount=0 的清理结果'
      )
    }
  }

  if (!executionError && !cleanupError) {
    writeEvidence({
      status: 'PASS',
      reason: '真实页面完成一线 O1 超量提交、组长红色识别及改配 O2，外部编排已清理并核验任务数据零残留',
      config,
      runtimeEvidence,
      fixtureVerification,
      cleanupResult,
      ...scenarioResult
    })
    return
  }

  const executionStatus = executionError ? statusForError(executionError) : undefined
  const status = executionStatus === 'FAIL' ? 'FAIL' : 'BLOCKED'
  const primaryError = executionError || cleanupError
  const scenarioEvidence = executionError?.scenarioEvidence || {}
  const reasons = []
  if (executionError) reasons.push(`执行未完成：${errorText(executionError)}`)
  if (cleanupError) reasons.push(`清理未通过：${errorText(cleanupError)}`)
  writeEvidence({
    status,
    blockedCategory: status === 'BLOCKED' && primaryError instanceof E2EBlockedError
      ? primaryError.category
      : undefined,
    reason: reasons.join('；'),
    config,
    runtimeEvidence,
    fixtureVerification,
    cleanupResult,
    cleanupError: cleanupError
      ? { name: cleanupError.name || 'Error', message: errorText(cleanupError) }
      : undefined,
    eventId: scenarioResult?.eventId || scenarioEvidence.eventId,
    steps: scenarioResult?.steps || scenarioEvidence.steps || [],
    diagnostics: scenarioResult?.diagnostics || scenarioEvidence.diagnostics,
    writeRequestCount: scenarioResult?.writeRequestCount
      ?? scenarioEvidence.diagnostics?.writeRequests?.length
      ?? 0,
    error: primaryError
      ? { name: primaryError.name || 'Error', message: errorText(primaryError) }
      : undefined
  })
  process.exitCode = status === 'FAIL' ? 1 : 2
}

if (require.main === module) {
  void main()
} else {
  module.exports = {
    E2EBlockedError,
    exactLongId,
    parseJsonPreservingLongIds,
    sameLongId,
    statusForError,
    assertManifestContainsNoSecrets,
    redactEvidence,
    isVerifiedCleanCleanup,
    classifyConsoleErrors,
    runtimeForMode,
    tenantForMode,
    artifactDirFor
  }
}
