const fs = require('node:fs')
const path = require('node:path')
const crypto = require('node:crypto')
const { chromium } = require('playwright')

const args = new Set(process.argv.slice(2))
const preflightOnly = args.has('--preflight-only')
const repoRoot = path.resolve(__dirname, '../..')
const projectRoot = path.resolve(repoRoot, '..')

const config = {
  baseUrl: (process.env.DCC_CONTROLLED_CONTENT_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.DCC_CONTROLLED_CONTENT_E2E_TENANT || '测试租户',
  username: process.env.DCC_CONTROLLED_CONTENT_E2E_USERNAME || 'aoteman',
  password: process.env.DCC_CONTROLLED_CONTENT_E2E_PASSWORD || '111111',
  tenantId: process.env.DCC_CONTROLLED_CONTENT_E2E_TENANT_ID || '122',
  allowWrites: process.env.DCC_CONTROLLED_CONTENT_E2E_ALLOW_WRITES === '1',
  approvalUsersJson: process.env.DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON,
  sourceFilePath: process.env.DCC_CONTROLLED_CONTENT_E2E_SOURCE_FILE,
  stampedPdfPath: process.env.DCC_CONTROLLED_CONTENT_E2E_STAMPED_PDF,
  distributionDepartmentLabel: process.env.DCC_CONTROLLED_CONTENT_E2E_DISTRIBUTION_DEPARTMENT_LABEL,
  projectKeyword: process.env.DCC_CONTROLLED_CONTENT_E2E_PROJECT_KEYWORD || '',
  productKeyword: process.env.DCC_CONTROLLED_CONTENT_E2E_PRODUCT_KEYWORD || '',
  headed: process.env.DCC_CONTROLLED_CONTENT_E2E_HEADED === '1',
  timeout: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_TIMEOUT || '90000'),
  submitResponseTimeout: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_SUBMIT_RESPONSE_TIMEOUT || '30000'),
  artifactDir: path.resolve(
    process.env.CONTROLLED_CONTENT_E2E_ARTIFACT_DIR ||
      process.env.DCC_CONTROLLED_CONTENT_E2E_ARTIFACT_DIR ||
      path.join(projectRoot, 'doc/tasks/20260720-controlled-state-machine-implementation/e2e-artifacts')
  )
}

const scenarioMatrix = [
  {
    key: 'sop-release',
    contentType: 'SOP',
    fileNumberCode: 'SOP',
    categoryId: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_SOP_CATEGORY_ID || '906101'),
    categoryName: process.env.DCC_CONTROLLED_CONTENT_E2E_SOP_CATEGORY_NAME || '体系文件',
    artifactName: 'controlled-content-dcc-sop-release-real.json',
    fullFlowPlan: 'release',
    expectedAssertions: {
      oldStatus: 'SUPERSEDED',
      newStatus: 'ACTIVE'
    }
  },
  {
    key: 'work-instruction-review-readonly',
    contentType: 'WORK_INSTRUCTION',
    fileNumberCode: 'WI',
    categoryId: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_WORK_INSTRUCTION_CATEGORY_ID || '907161'),
    categoryName: process.env.DCC_CONTROLLED_CONTENT_E2E_WORK_INSTRUCTION_CATEGORY_NAME || '工序卡/作业指导书',
    artifactName: 'controlled-content-dcc-work-instruction-review-readonly-real.json',
    fullFlowPlan: 'pending-readonly',
    expectedAssertions: {
      reviewStatus: 'IN_REVIEW',
      uiReadonly: true,
      directEditRejected: true
    }
  },
  {
    key: 'inspection-withdraw-draft',
    contentType: 'INSPECTION_PROCEDURE',
    fileNumberCode: 'IP',
    categoryId: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_INSPECTION_CATEGORY_ID || '907163'),
    categoryName: process.env.DCC_CONTROLLED_CONTENT_E2E_INSPECTION_CATEGORY_NAME || '来料检验规程',
    artifactName: 'controlled-content-dcc-inspection-withdraw-draft-real.json',
    fullFlowPlan: 'withdraw-resubmit',
    expectedAssertions: {
      withdrawnStatus: 'WITHDRAWN',
      newDraftCount: 1,
      sourceActiveUnchanged: true
    }
  },
  {
    key: 'drawing-obsolete',
    contentType: 'DRAWING',
    fileNumberCode: 'DWG',
    categoryId: Number(process.env.DCC_CONTROLLED_CONTENT_E2E_DRAWING_CATEGORY_ID || '907154'),
    categoryName: process.env.DCC_CONTROLLED_CONTENT_E2E_DRAWING_CATEGORY_NAME || '成品图纸',
    artifactName: 'controlled-content-dcc-drawing-obsolete-real.json',
    fullFlowPlan: 'obsolete',
    expectedAssertions: {
      masterCurrentActiveCleared: true,
      activeCount: 0,
      noFallbackToOldVersion: true
    }
  }
]

const blockers = []
const evidence = {
  steps: [],
  writeRequests: [],
  scenarios: {},
  submitterProfile: null
}
const MAX_DCC_FILE_NUMBER_LENGTH = 64
const readOnlyPostPaths = new Set(['/dcc/controlled-files/route-preview'])
let approvalUsersJsonParsed = null
let approvalUsersJsonParseAttempted = false

function uniqueMessages(messages) {
  return [...new Set((messages || []).filter(Boolean))]
}

const selectedScenarioKeys = uniqueMessages(
  (process.env.DCC_CONTROLLED_CONTENT_E2E_SCENARIOS || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
)
const selectedScenarioMatrix = selectedScenarioKeys.length
  ? scenarioMatrix.filter((scenario) => selectedScenarioKeys.includes(scenario.key) || selectedScenarioKeys.includes(scenario.contentType))
  : scenarioMatrix
const missingSelectedScenarioKeys = selectedScenarioKeys.filter(
  (key) => !scenarioMatrix.some((scenario) => scenario.key === key || scenario.contentType === key)
)

function addBlocker(message) {
  if (!blockers.includes(message)) {
    blockers.push(message)
  }
}

function writeArtifact(name, payload) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

async function parseApiResponsePayload(response, description) {
  const text = await response.text()
  if (!text.trim()) {
    throw new Error(`${description} returned empty response body: HTTP ${response.status()} ${response.url()}`)
  }
  try {
    return JSON.parse(text)
  } catch (error) {
    throw new Error(`${description} returned non-JSON response: HTTP ${response.status()} ${text.slice(0, 500)}`)
  }
}

function apiPayloadMessage(response) {
  if (response.body?.msg) return response.body.msg
  if (response.body) return JSON.stringify(response.body)
  return response.text || '<empty body>'
}

function scenarioWriteRequests(preflight) {
  const scenarioWriteStartIndex = Number.isInteger(preflight?.scenarioWriteStartIndex)
    ? preflight.scenarioWriteStartIndex
    : 0
  const scenarioWriteEndIndex = Number.isInteger(preflight?.scenarioWriteEndIndex)
    ? preflight.scenarioWriteEndIndex
    : evidence.writeRequests.length
  return evidence.writeRequests.slice(scenarioWriteStartIndex, scenarioWriteEndIndex)
}

function unwrapCacheValue(value) {
  if (!value || typeof value !== 'object') return value
  for (const field of ['accessToken', 'v', 'value', 'data']) {
    if (Object.prototype.hasOwnProperty.call(value, field)) {
      return unwrapCacheValue(value[field])
    }
  }
  return value
}

function normalizeCacheString(value) {
  if (typeof value === 'string' && value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1)
  }
  return value
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function login(page, actor = {}, targetPath = '/dcc/controlled-file/upload') {
  const credentials = {
    tenant: actor.tenant || config.tenant,
    username: actor.username || config.username,
    password: actor.password || config.password
  }
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: config.timeout })
  if (page.url().includes('/login')) {
    const form = page.locator('form.login-form:visible').first()
    await form.waitFor({ state: 'visible', timeout: config.timeout })
    const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
    if (await tenantInput.count()) {
      await tenantInput.fill(credentials.tenant)
      const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: credentials.tenant }).first()
      await tenantOption.waitFor({ state: 'visible', timeout: config.timeout })
      await tenantOption.click()
    } else {
      await form.locator('input.el-input__inner').nth(0).fill(credentials.tenant)
    }
    await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(credentials.username)
    await form.locator('input[type="password"]').first().fill(credentials.password)
    const loginResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: config.timeout }
    )
    await form.getByRole('button', { name: '登录' }).click()
    const loginResponse = await loginResponsePromise
    const payload = await parseApiResponsePayload(loginResponse, `login for ${credentials.username}`)
    if (!loginResponse.ok() || ![0, 200].includes(payload.code)) {
      throw new Error(`login failed for ${credentials.username}: HTTP ${loginResponse.status()} ${payload.msg || JSON.stringify(payload)}`)
    }
    await page.waitForURL((current) => !current.pathname.includes('/login'), {
      timeout: config.timeout,
      waitUntil: 'commit'
    })
  }
  await page.goto(new URL(targetPath, config.baseUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  if (targetPath.includes('/dcc/controlled-file/upload')) {
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  }
}

async function readBrowserCache(page, key) {
  return page.evaluate((cacheKey) => {
    const unwrap = (value) => {
      if (!value || typeof value !== 'object') return value
      for (const field of ['accessToken', 'v', 'value', 'data']) {
        if (Object.prototype.hasOwnProperty.call(value, field)) {
          return unwrap(value[field])
        }
      }
      return value
    }
    const storages = [localStorage, sessionStorage]
    for (const storage of storages) {
      let raw = storage.getItem(cacheKey)
      if (!raw) {
        const matchedKey = Object.keys(storage).find((item) => item === cacheKey || item.endsWith(cacheKey))
        if (matchedKey) raw = storage.getItem(matchedKey)
      }
      if (!raw) continue
      try {
        return unwrap(JSON.parse(raw))
      } catch (error) {
        return raw
      }
    }
    return undefined
  }, key)
}

async function apiGet(page, apiPath) {
  const headers = await buildApiHeaders(page)
  const response = await page.evaluate(
    async ({ requestPath, headers }) => {
      const res = await fetch(`/admin-api${requestPath}`, { headers, credentials: 'omit' })
      const text = await res.text()
      let body = null
      try {
        body = text ? JSON.parse(text) : null
      } catch (error) {
        return { status: res.status, body: null, text, parseError: error.message }
      }
      return { status: res.status, body, text }
    },
    { requestPath: apiPath, headers }
  )
  if (response.status !== 200 || !response.body || ![0, 200].includes(response.body.code)) {
    throw new Error(`api GET failed: ${apiPath} HTTP ${response.status} ${apiPayloadMessage(response)}`)
  }
  return response.body.data
}

async function apiPostReadOnly(page, apiPath, data) {
  if (!readOnlyPostPaths.has(apiPath)) {
    throw new Error(`read-only POST path is not allow-listed: ${apiPath}`)
  }
  const headers = await buildApiHeaders(page, { 'Content-Type': 'application/json' })
  const response = await page.evaluate(
    async ({ requestPath, payload, headers }) => {
      const res = await fetch(`/admin-api${requestPath}`, {
        method: 'POST',
        headers,
        credentials: 'omit',
        body: JSON.stringify(payload)
      })
      const text = await res.text()
      let body = null
      try {
        body = text ? JSON.parse(text) : null
      } catch (error) {
        return { status: res.status, body: null, text, parseError: error.message }
      }
      return { status: res.status, body, text }
    },
    { requestPath: apiPath, payload: data, headers }
  )
  if (response.status !== 200 || !response.body || ![0, 200].includes(response.body.code)) {
    throw new Error(`api POST readonly failed: ${apiPath} HTTP ${response.status} ${apiPayloadMessage(response)}`)
  }
  return response.body.data
}

async function buildApiHeaders(page, extraHeaders = {}) {
  const accessToken = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'ACCESS_TOKEN')))
  if (!accessToken) throw new Error('missing ACCESS_TOKEN after login')
  const tenantId = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'tenantId'))) || config.tenantId
  evidence.tenantId = String(tenantId)
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': String(tenantId),
    'Cache-Control': 'no-cache',
    Pragma: 'no-cache',
    ...extraHeaders
  }
}

function trackUiWriteRequest(request) {
  const method = request.method()
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return
  const requestUrl = request.url()
  if (!requestUrl.includes('/admin-api/')) return
  const parsedUrl = new URL(requestUrl)
  const apiPath = parsedUrl.pathname.replace('/admin-api', '')
  if (apiPath === '/system/auth/login') return
  if (method === 'POST' && readOnlyPostPaths.has(apiPath)) return
  evidence.writeRequests.push({
    method,
    path: apiPath + parsedUrl.search
  })
}

function parseApprovalUsersJson() {
  if (approvalUsersJsonParseAttempted) return approvalUsersJsonParsed
  approvalUsersJsonParseAttempted = true
  if (!config.approvalUsersJson) return null
  try {
    approvalUsersJsonParsed = JSON.parse(config.approvalUsersJson)
    return approvalUsersJsonParsed
  } catch (error) {
    addBlocker(`DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON is invalid JSON: ${error.message}`)
    return null
  }
}

function accountList(value) {
  if (!value) return []
  return Array.isArray(value) ? value : [value]
}

function normalizeSubjectType(subjectType) {
  const normalized = String(subjectType || '').toUpperCase()
  if (normalized === '1') return 'USER'
  if (normalized === '2') return 'DEPT'
  if (normalized === '3') return 'ROLE'
  if (normalized === '4') return 'POSITION'
  return normalized
}

function objectIds(values) {
  return (values || []).map((item) => Number(item?.id)).filter((id) => Number.isFinite(id))
}

function currentUserPermissionProfile(profile) {
  return {
    id: Number(profile?.id),
    username: profile?.username,
    deptId: Number(profile?.dept?.id) || null,
    roleIds: objectIds(profile?.roles),
    postIds: objectIds(profile?.posts)
  }
}

function permissionRuleMatchesSubmitter(rule, submitterProfile, actionType) {
  if (!submitterProfile || rule?.active === false || String(rule?.actionType || '').toUpperCase() !== actionType) {
    return false
  }
  const subjectId = Number(rule.subjectId)
  if (!Number.isFinite(subjectId)) return false
  switch (normalizeSubjectType(rule.subjectType)) {
    case 'USER':
      return Number(submitterProfile.id) === subjectId
    case 'DEPT':
      return Number(submitterProfile.deptId) === subjectId
    case 'ROLE':
      return submitterProfile.roleIds.includes(subjectId)
    case 'POSITION':
      return submitterProfile.postIds.includes(subjectId)
    default:
      return false
  }
}

function categoryActionPermissionEvidence(permissionRules, submitterProfile, actionType, permissionRuleError) {
  const action = String(actionType || '').toUpperCase()
  const actionKey = action.toLowerCase()
  const actionLabel = action.charAt(0) + action.slice(1).toLowerCase()
  if (permissionRuleError) {
    return {
      readable: false,
      error: permissionRuleError,
      submitter: submitterProfile,
      actionType: action,
      [`${actionKey}RuleCount`]: null,
      [`direct${actionLabel}RuleMatchCount`]: null,
      [`${actionKey}Rules`]: []
    }
  }
  const actionRules = (permissionRules || [])
    .filter((rule) => String(rule?.actionType || '').toUpperCase() === action && rule.active !== false)
  return {
    readable: true,
    error: null,
    submitter: submitterProfile,
    actionType: action,
    [`${actionKey}RuleCount`]: actionRules.length,
    [`direct${actionLabel}RuleMatchCount`]: actionRules.filter((rule) => permissionRuleMatchesSubmitter(rule, submitterProfile, action)).length,
    [`${actionKey}Rules`]: actionRules.map((rule) => ({
      subjectType: rule.subjectType,
      subjectId: rule.subjectId,
      scopeType: rule.scopeType,
      active: rule.active
    }))
  }
}

function categoryUploadPermissionEvidence(permissionRules, submitterProfile, permissionRuleError) {
  return categoryActionPermissionEvidence(permissionRules, submitterProfile, 'UPLOAD', permissionRuleError)
}

function accountIsComplete(account) {
  return Boolean(account && account.username && account.password && account.userId)
}

function routeCandidateUserIds(node) {
  const rawIds = Array.isArray(node.candidateSourceIds) && node.candidateSourceIds.length > 0
    ? node.candidateSourceIds
    : [node.candidateSourceId]
  return rawIds.map((id) => Number(id)).filter((id) => Number.isFinite(id))
}

function userIsActive(user) {
  if (!user) return false
  if (user.disabled === true) return false
  if (user.status === undefined || user.status === null) return true
  return Number(user.status) === 0
}

function userPostIds(user) {
  const postIds = user?.postIds
  if (Array.isArray(postIds)) {
    return postIds.map((id) => Number(id)).filter((id) => Number.isFinite(id))
  }
  if (typeof postIds === 'string') {
    return postIds
      .split(',')
      .map((id) => Number(id.trim()))
      .filter((id) => Number.isFinite(id))
  }
  return []
}

function userLabel(user) {
  return [user?.nickname, user?.username].filter(Boolean).join('/') || `user#${user?.id}`
}

function resolveApprovalPositionCandidateUsers(position, users) {
  const blockers = []
  const resolvedCandidateUsers = []
  if (!position) {
    return {
      activeAssignmentCount: 0,
      resolvedCandidateUsers,
      blockers
    }
  }
  const activeAssignments = (position.assignments || []).filter((assignment) => assignment.active)
  for (const assignment of activeAssignments) {
    if (assignment.assignmentType === 'USER') {
      const userId = Number(assignment.userId)
      const user = users.find((candidate) => Number(candidate.id) === userId)
      if (userIsActive(user)) {
        resolvedCandidateUsers.push(user)
      }
      continue
    }
    if (assignment.assignmentType === 'POST') {
      const postId = Number(assignment.systemPostId)
      const postUsers = users.filter((candidate) => userIsActive(candidate) && userPostIds(candidate).includes(postId))
      resolvedCandidateUsers.push(...postUsers)
      continue
    }
    blockers.push(`DCC approval position unsupported assignmentType for ${position.name}: ${assignment.assignmentType}`)
  }
  const uniqueUsers = Array.from(
    new Map(resolvedCandidateUsers.map((user) => [Number(user.id), user])).values()
  )
  return {
    activeAssignmentCount: activeAssignments.length,
    resolvedCandidateUsers: uniqueUsers,
    blockers
  }
}

function resolveApprovalCandidatesForRoute(activeRoute, approvalPositions, users) {
  const nodes = []
  const blockers = []
  for (const [index, node] of (activeRoute?.nodes || []).entries()) {
    const stageLabel = `${node.stageNo} ${node.stageName || ''}`.trim()
    const sourceIds = routeCandidateUserIds(node)
    const resolvedNode = {
      index,
      stageNo: node.stageNo,
      stageName: node.stageName,
      candidateSourceType: node.candidateSourceType,
      candidateSourceIds: sourceIds,
      activeAssignmentCount: null,
      resolvedCandidateUserIds: [],
      resolvedCandidateUsers: []
    }
    if (node.candidateSourceType === 'USER') {
      const resolvedUsers = sourceIds
        .map((userId) => users.find((candidate) => Number(candidate.id) === userId))
        .filter(userIsActive)
      resolvedNode.activeAssignmentCount = resolvedUsers.length
      resolvedNode.resolvedCandidateUsers = resolvedUsers
      resolvedNode.resolvedCandidateUserIds = resolvedUsers.map((user) => Number(user.id))
      if (sourceIds.length > 0 && resolvedNode.resolvedCandidateUserIds.length === 0) {
        blockers.push(`DCC approval USER candidate resolves to no active user for stage ${stageLabel}`)
      }
    } else if (node.candidateSourceType === 'POSITION') {
      const nodeUsers = []
      let activeAssignmentCount = 0
      for (const positionId of sourceIds) {
        const position = (approvalPositions || []).find((candidate) => Number(candidate.id) === positionId)
        if (!position) {
          blockers.push(`DCC approval position missing for stage ${stageLabel}: positionId=${positionId}`)
          continue
        }
        if (position.active === false) {
          blockers.push(`DCC approval position inactive for stage ${stageLabel}: ${position.name}#${positionId}`)
          continue
        }
        const resolution = resolveApprovalPositionCandidateUsers(position, users)
        activeAssignmentCount += resolution.activeAssignmentCount
        blockers.push(...resolution.blockers.map((message) => `${message} at stage ${stageLabel}`))
        if (resolution.activeAssignmentCount === 0) {
          blockers.push(`DCC approval position has no active assignment for stage ${stageLabel}: ${position.name}#${positionId}`)
        }
        if (resolution.activeAssignmentCount > 0 && resolution.resolvedCandidateUsers.length === 0) {
          blockers.push(`DCC approval position resolves to no active user for stage ${stageLabel}: ${position.name}#${positionId}`)
        }
        nodeUsers.push(...resolution.resolvedCandidateUsers)
      }
      const uniqueUsers = Array.from(new Map(nodeUsers.map((user) => [Number(user.id), user])).values())
      resolvedNode.activeAssignmentCount = activeAssignmentCount
      resolvedNode.resolvedCandidateUsers = uniqueUsers
      resolvedNode.resolvedCandidateUserIds = uniqueUsers.map((user) => Number(user.id))
    } else {
      blockers.push(`DCC approval candidateSourceType unsupported for stage ${stageLabel}: ${node.candidateSourceType}`)
    }
    nodes.push(resolvedNode)
  }
  return {
    nodes,
    blockers: uniqueMessages(blockers)
  }
}

function normalizeIdList(value) {
  if (!value) return []
  const rawIds = Array.isArray(value) ? value : [value]
  return rawIds.map((id) => Number(id)).filter((id) => Number.isFinite(id))
}

function normalizeOfficialRoutePreview(rows) {
  return (rows || []).map((row, index) => ({
    index,
    stageNo: row.stageNo,
    stageName: row.stageName,
    candidateSourceType: row.candidateSourceType,
    candidateSourceId: row.candidateSourceId,
    candidateSourceIds: normalizeIdList(row.candidateSourceIds),
    approveMethod: row.approveMethod,
    approvalMode: row.approvalMode,
    resolvedUserIds: normalizeIdList(row.resolvedUserIds)
  }))
}

function approvalCandidateResolutionFromOfficialRoutePreview(rows) {
  return {
    blockers: [],
    nodes: normalizeOfficialRoutePreview(rows).map((row) => ({
      index: row.index,
      stageNo: row.stageNo,
      stageName: row.stageName,
      candidateSourceType: row.candidateSourceType,
      candidateSourceIds: row.candidateSourceIds,
      activeAssignmentCount: null,
      resolvedCandidateUserIds: row.resolvedUserIds,
      resolvedCandidateUsers: []
    }))
  }
}

function approvalRouteForExecution(preflight) {
  const normalizedOfficialRoutePreview = preflight?.officialRoutePreview || []
  if (normalizedOfficialRoutePreview.length > 0) {
    return {
      ...(preflight.activeRoute || {}),
      nodes: normalizedOfficialRoutePreview.map((row) => ({
        stageNo: row.stageNo,
        stageName: row.stageName,
        candidateSourceType: row.candidateSourceType,
        candidateSourceId: row.candidateSourceId,
        candidateSourceIds: row.candidateSourceIds,
        approveMethod: row.approveMethod
      }))
    }
  }
  return preflight?.activeRoute
}

function resolveApprovalAccountReference(approvalUsers, stageAccount) {
  if (!stageAccount) return null
  if (Array.isArray(stageAccount)) {
    return stageAccount.map((account) => resolveApprovalAccountReference(approvalUsers, account))
  }
  if (typeof stageAccount === 'string') {
    stageAccount = { username: stageAccount }
  }
  if (stageAccount.username) {
    const namedAccount = approvalUsers?.users?.[stageAccount.username]
    if (namedAccount) {
      return { ...namedAccount, ...stageAccount }
    }
  }
  return stageAccount
}

function findApprovalStageAccount(scope, stageKeys, node) {
  if (Array.isArray(scope.stages)) {
    const nodeIndex = Number.isInteger(node?.index) ? node.index : null
    const matchedStage = scope.stages.find((stage) => {
      if (!stage || typeof stage !== 'object') return false
      if (nodeIndex !== null && Number(stage.node) === nodeIndex) return true
      if (node.stageNo !== undefined && Number(stage.stageNo) === Number(node.stageNo)) return true
      if (node.stageName && stage.stageName === node.stageName) return true
      return false
    })
    if (matchedStage) {
      return matchedStage.account || matchedStage.approver || matchedStage
    }
  }
  for (const stageKey of stageKeys) {
    const stageAccount = scope.stages?.[stageKey] || scope[stageKey]
    if (stageAccount) return stageAccount
  }
  return null
}

function approvalAccountForStage(approvalUsers, scenario, node) {
  const nodeIndex = Number.isInteger(node?.index) ? node.index : null
  const stageKeys = uniqueMessages([
    nodeIndex === null ? null : `node:${nodeIndex}`,
    nodeIndex === null ? null : `node:${nodeIndex + 1}`,
    nodeIndex === null ? null : `${scenario.key}:node:${nodeIndex}`,
    nodeIndex === null ? null : `${scenario.key}:node:${nodeIndex + 1}`,
    nodeIndex === null ? null : `${scenario.contentType}:node:${nodeIndex}`,
    nodeIndex === null ? null : `${scenario.contentType}:node:${nodeIndex + 1}`,
    String(node.stageNo),
    node.stageName,
    `${node.stageNo}:${node.stageName}`,
    `${scenario.contentType}:${node.stageNo}`,
    `${scenario.key}:${node.stageNo}`
  ])
  const scopes = [
    approvalUsers?.scenarios?.[scenario.key],
    approvalUsers?.contentTypes?.[scenario.contentType],
    approvalUsers?.[scenario.key],
    approvalUsers?.[scenario.contentType],
    approvalUsers
  ]
  for (const scope of scopes) {
    if (!scope || typeof scope !== 'object') continue
    const stageAccount = findApprovalStageAccount(scope, stageKeys, node)
    if (stageAccount) {
      return resolveApprovalAccountReference(approvalUsers, stageAccount)
    }
    if (scope.default) return resolveApprovalAccountReference(approvalUsers, scope.default)
  }
  return null
}

function validateApprovalUsersForRoute(scenario, activeRoute, approvalUsers, approvalCandidateResolution) {
  if (!approvalUsers || !activeRoute) return []
  const validationBlockers = []
  for (const [index, node] of (activeRoute.nodes || []).entries()) {
    const nodeWithIndex = { ...node, index }
    const stageLabel = `${scenario.contentType} stage ${node.stageNo} ${node.stageName || ''}`.trim()
    const accounts = accountList(approvalAccountForStage(approvalUsers, scenario, nodeWithIndex))
    if (accounts.length === 0) {
      validationBlockers.push(`DCC approval account missing for ${stageLabel}`)
      continue
    }
    const completeAccounts = accounts.filter(accountIsComplete)
    if (completeAccounts.length === 0) {
      validationBlockers.push(`DCC approval account missing required fields for ${stageLabel}: username/password/userId`)
      continue
    }
    if (node.candidateSourceType === 'USER') {
      const candidateUserIds = routeCandidateUserIds(node)
      const configuredUserIds = completeAccounts.map((account) => Number(account.userId))
      if (candidateUserIds.length > 0 && !configuredUserIds.some((userId) => candidateUserIds.includes(userId))) {
        validationBlockers.push(
          `configured approver userId is not in USER candidateSourceIds for ${stageLabel}: configured=${configuredUserIds.join(',')}, candidates=${candidateUserIds.join(',')}`
        )
      }
    } else {
      const resolvedNode = approvalCandidateResolution?.nodes?.[index]
      const resolvedCandidateUserIds = resolvedNode?.resolvedCandidateUserIds || []
      const configuredUserIds = completeAccounts.map((account) => Number(account.userId))
      if (resolvedCandidateUserIds.length > 0 && !configuredUserIds.some((userId) => resolvedCandidateUserIds.includes(userId))) {
        validationBlockers.push(
          `configured approver userId is not in resolved candidate users for ${stageLabel}: configured=${configuredUserIds.join(',')}, candidates=${resolvedCandidateUserIds.join(',')}`
        )
      }
    }
  }
  return uniqueMessages(validationBlockers)
}

function routePreview(activeRoute, approvalCandidateResolution) {
  return (activeRoute?.nodes || []).map((node, index) => ({
    stageNo: node.stageNo,
    stageName: node.stageName,
    candidateSourceType: node.candidateSourceType,
    candidateSourceId: node.candidateSourceId,
    candidateSourceIds: node.candidateSourceIds,
    approveMethod: node.approveMethod,
    activeAssignmentCount: approvalCandidateResolution?.nodes?.[index]?.activeAssignmentCount ?? null,
    resolvedCandidateUserIds: approvalCandidateResolution?.nodes?.[index]?.resolvedCandidateUserIds || [],
    resolvedCandidateUsers:
      approvalCandidateResolution?.nodes?.[index]?.resolvedCandidateUsers.map((user) => ({
        id: Number(user.id),
        username: user.username,
        nickname: user.nickname,
        label: userLabel(user)
      })) || []
  }))
}

function formatDate(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function createTraceableRunId() {
  const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, '')
  const randomSegment = crypto.randomBytes(4).toString('hex').toUpperCase()
  return `${timestamp}-${randomSegment}`
}

function normalizeFileNumberSuffix(suffix) {
  const normalized = String(suffix || '').trim().toUpperCase()
  if (!normalized) return null
  if (normalized === 'WITHDRAW-SOURCE') return 'WDSRC'
  if (normalized === 'OBSOLETE-SOURCE') return 'OBSRC'
  return normalized.replace(/[^A-Z0-9]+/g, '').slice(0, 12)
}

function buildDccE2eFileNumber(scenario, runId, suffix) {
  if (!scenario.fileNumberCode) {
    throw new Error(`DCC generated fileNumber requires fileNumberCode for ${scenario.contentType}`)
  }
  const suffixCode = normalizeFileNumberSuffix(suffix)
  const parts = ['CODEX', 'DCC', scenario.fileNumberCode]
  if (suffixCode) parts.push(suffixCode)
  parts.push(runId)
  const fileNumber = parts.join('-')
  if (fileNumber.length > MAX_DCC_FILE_NUMBER_LENGTH) {
    throw new Error(
      `DCC generated fileNumber exceeds database limit: length=${fileNumber.length}, ` +
      `max=${MAX_DCC_FILE_NUMBER_LENGTH}, value=${fileNumber}`
    )
  }
  return fileNumber
}

function firstLeafDirectoryPath(nodes = []) {
  for (const node of nodes || []) {
    const name = String(node?.name || '').trim()
    if (!name) continue
    if (Array.isArray(node.children) && node.children.length > 0) {
      const childPath = firstLeafDirectoryPath(node.children)
      if (childPath.length > 0) {
        return [name, ...childPath]
      }
      continue
    }
    return [name]
  }
  return []
}

async function selectUploadLeafDirectoryThroughUi(page, scenario) {
  const tree = await apiGet(page, `/dcc/controlled-files/upload-directory-tree?categoryId=${encodeURIComponent(scenario.categoryId)}`)
  if (tree?.leafBinding) {
    await page.getByText('当前绑定目录已经是最后一层目录', { exact: false })
      .first()
      .waitFor({ state: 'visible', timeout: config.timeout })
    return tree.bindingDirectoryPath
  }

  const directoryPath = firstLeafDirectoryPath(tree?.children || [])
  if (!directoryPath.length) {
    throw new Error(`DCC ${scenario.contentType} upload directory tree has no selectable leaf directory for categoryId=${scenario.categoryId}`)
  }

  const item = formItem(page, '提交目录')
  await item.getByPlaceholder('请选择绑定目录下的最后一层子目录').click()
  for (const label of directoryPath) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: label }).last()
    await node.waitFor({ state: 'visible', timeout: config.timeout })
    await node.click()
    await page.waitForTimeout(200)
  }
  await page.getByText('最终提交路径', { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  return directoryPath.join('/')
}

function scenarioRequiresApprovalUsers(scenario) {
  return ['release', 'withdraw-resubmit', 'obsolete'].includes(scenario.fullFlowPlan)
}

function scenarioRequiresFinalApproval(scenario) {
  return ['release', 'withdraw-resubmit', 'obsolete'].includes(scenario.fullFlowPlan)
}

function filePrerequisiteBlockers(scenario) {
  const prerequisiteBlockers = []
  if (!config.sourceFilePath) {
    prerequisiteBlockers.push('DCC_CONTROLLED_CONTENT_E2E_SOURCE_FILE is required for DCC write matrix full-flow source Office file')
  } else if (!fs.existsSync(config.sourceFilePath) || !fs.statSync(config.sourceFilePath).isFile()) {
    prerequisiteBlockers.push(`DCC_CONTROLLED_CONTENT_E2E_SOURCE_FILE must point to a readable file for DCC write matrix full-flow: ${config.sourceFilePath}`)
  }
  if (scenarioRequiresFinalApproval(scenario)) {
    if (!config.stampedPdfPath) {
      prerequisiteBlockers.push('DCC_CONTROLLED_CONTENT_E2E_STAMPED_PDF is required for DCC write matrix full-flow stamped PDF file')
    } else if (!fs.existsSync(config.stampedPdfPath) || !fs.statSync(config.stampedPdfPath).isFile()) {
      prerequisiteBlockers.push(`DCC_CONTROLLED_CONTENT_E2E_STAMPED_PDF must point to a readable file for DCC write matrix full-flow: ${config.stampedPdfPath}`)
    }
    if (!config.distributionDepartmentLabel) {
      prerequisiteBlockers.push('DCC_CONTROLLED_CONTENT_E2E_DISTRIBUTION_DEPARTMENT_LABEL is required for DCC doc-control final approval')
    }
  }
  return prerequisiteBlockers
}

function uploadSizePolicyChecks(scenario) {
  const checks = []
  if (config.sourceFilePath && fs.existsSync(config.sourceFilePath) && fs.statSync(config.sourceFilePath).isFile()) {
    checks.push({
      purpose: 'SOURCE',
      filePath: config.sourceFilePath,
      fileSize: fs.statSync(config.sourceFilePath).size
    })
  }
  if (
    scenarioRequiresFinalApproval(scenario) &&
    config.stampedPdfPath &&
    fs.existsSync(config.stampedPdfPath) &&
    fs.statSync(config.stampedPdfPath).isFile()
  ) {
    checks.push({
      purpose: 'DRAWING_PDF',
      filePath: config.stampedPdfPath,
      fileSize: fs.statSync(config.stampedPdfPath).size
    })
  }
  return checks
}

async function collectUploadSizePolicyEvidence(page, scenario) {
  const blockers = []
  const checks = []
  for (const check of uploadSizePolicyChecks(scenario)) {
    const apiPath =
      `/dcc/protection/upload-size-policies/effective?categoryId=${encodeURIComponent(scenario.categoryId)}` +
      `&purpose=${encodeURIComponent(check.purpose)}&fileSize=${encodeURIComponent(check.fileSize)}`
    try {
      const policy = await apiGet(page, apiPath)
      checks.push({
        purpose: check.purpose,
        fileName: path.basename(check.filePath),
        fileSize: check.fileSize,
        status: 'PASS',
        policy
      })
    } catch (error) {
      const message = `DCC upload size policy preflight failed for ${scenario.contentType} ${check.purpose}: ${error.message}`
      blockers.push(message)
      checks.push({
        purpose: check.purpose,
        fileName: path.basename(check.filePath),
        fileSize: check.fileSize,
        status: 'BLOCKED',
        error: error.message
      })
    }
  }
  return {
    checks,
    blockers: uniqueMessages(blockers)
  }
}

async function collectDistributionDepartmentEvidence(page, scenario) {
  if (!scenarioRequiresFinalApproval(scenario)) {
    return {
      required: false,
      label: config.distributionDepartmentLabel || null,
      matches: [],
      blockers: []
    }
  }
  if (!config.distributionDepartmentLabel) {
    return {
      required: true,
      label: null,
      matches: [],
      blockers: ['DCC_CONTROLLED_CONTENT_E2E_DISTRIBUTION_DEPARTMENT_LABEL is required for DCC doc-control final approval']
    }
  }
  const departments = await apiGet(page, '/system/dept/simple-list')
  const label = String(config.distributionDepartmentLabel).trim()
  const matches = (departments || [])
    .filter((department) => String(department?.name || '').trim() === label)
    .map((department) => ({
      id: Number(department.id),
      name: department.name,
      parentId: department.parentId,
      status: department.status
    }))
  const blockers = matches.length > 0
    ? []
    : [`DCC distribution department label not found for ${scenario.contentType}: ${label}`]
  return {
    required: true,
    label,
    matches,
    blockers
  }
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

async function selectOptionByFormLabel(page, label, optionText) {
  const item = formItem(page, label)
  await item.locator('.el-select, .el-cascader').first().click()
  const option = page.locator('.el-select-dropdown__item:visible, .el-cascader-node:visible')
    .filter({ hasText: optionText })
    .first()
  await option.waitFor({ state: 'visible', timeout: config.timeout })
  await option.click()
}

async function selectDccProjectThroughUi(page, scenario) {
  const query = new URLSearchParams({
    pageNo: '1',
    pageSize: '50',
    status: 'ENABLE'
  })
  if (config.projectKeyword) {
    query.set('keyword', config.projectKeyword)
  }
  const projectPage = await apiGet(page, `/dcc/project-codes/page?${query.toString()}`)
  const candidates = Array.isArray(projectPage?.list) ? projectPage.list : []
  const selectedProject = candidates[0]
  if (!selectedProject?.id) {
    throw new Error(
      `DCC project code option is required for ${scenario.contentType}: ` +
      `keyword=${config.projectKeyword || '<empty>'}, candidateCount=${candidates.length}`
    )
  }
  const item = formItem(page, 'DCC项目')
  await item.locator('.el-select').first().click()
  const input = item.locator('input[role="combobox"], input').first()
  if (config.projectKeyword) {
    await input.fill(config.projectKeyword)
  }
  const option = page.locator('.el-select-dropdown__item:visible')
    .filter({ hasText: selectedProject.projectName || selectedProject.projectCode || String(selectedProject.id) })
    .first()
  await clickVisibleDropdownOptionByPoint(page, option, 'DCC项目')
  await item.getByText(selectedProject.projectName || selectedProject.projectCode || '', { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  return {
    id: selectedProject.id,
    projectName: selectedProject.projectName,
    projectCode: selectedProject.projectCode
  }
}

function buildFileTypeTaxonomyPath(rows) {
  const activeRows = (rows || []).filter((row) => row?.active !== false && row?.id)
  const byParent = new Map()
  for (const row of activeRows) {
    const parentId = row.parentId == null ? 0 : Number(row.parentId)
    if (!byParent.has(parentId)) byParent.set(parentId, [])
    byParent.get(parentId).push(row)
  }
  for (const siblings of byParent.values()) {
    siblings.sort((a, b) => (Number(a.sort) || 0) - (Number(b.sort) || 0) || String(a.name).localeCompare(String(b.name)))
  }
  let selectedPath = null
  const walk = (parentId, path) => {
    if (selectedPath) return
    for (const row of byParent.get(parentId) || []) {
      const nextPath = [...path, row]
      const children = byParent.get(Number(row.id)) || []
      if (nextPath.length >= 3 && children.length === 0) {
        selectedPath = nextPath
        return
      }
      walk(Number(row.id), nextPath)
      if (selectedPath) return
    }
  }
  walk(0, [])
  if (!selectedPath) {
    throw new Error(`DCC file type taxonomy requires an active leaf with at least 3 levels; activeRowCount=${activeRows.length}`)
  }
  return selectedPath
}

async function selectFileTypeTaxonomyThroughUi(page, scenario) {
  const rows = await apiGet(page, '/dcc/file-type-taxonomies')
  const pathRows = buildFileTypeTaxonomyPath(rows)
  const item = formItem(page, '文件分类')
  await item.locator('.el-cascader').first().click()
  for (const row of pathRows) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: row.name }).last()
    await node.waitFor({ state: 'visible', timeout: config.timeout })
    await node.click()
    await page.waitForTimeout(200)
  }
  const pathLabel = pathRows.map((row) => row.name).join(' / ')
  await item.getByText(pathRows[pathRows.length - 1].name, { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  return {
    id: pathRows[pathRows.length - 1].id,
    pathLabel,
    depth: pathRows.length,
    contentType: scenario.contentType
  }
}

async function clickVisibleDropdownOptionByPoint(page, option, description) {
  await option.waitFor({ state: 'visible', timeout: config.timeout })
  const optionHandle = await option.elementHandle()
  if (!optionHandle) {
    throw new Error(`DCC dropdown option is not attached: ${description || '<unknown>'}`)
  }
  const { optionLabel, box } = await optionHandle.evaluate((element) => {
    const rect = element.getBoundingClientRect()
    return {
      optionLabel: (element.innerText || element.textContent || '').replace(/\s+/g, ' ').trim(),
      box: {
        x: rect.left,
        y: rect.top,
        width: rect.width,
        height: rect.height
      }
    }
  })
  if (!box || box.width <= 0 || box.height <= 0) {
    throw new Error(`DCC dropdown option is not clickable: ${description || optionLabel || '<empty>'}`)
  }
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  return optionLabel
}

async function fillFormInput(page, label, value) {
  await formItem(page, label).locator('input').first().fill(value)
}

async function fillFormTextarea(page, label, value) {
  await formItem(page, label).locator('textarea').first().fill(value)
}

async function chooseRadioButton(page, label) {
  await page.locator('.el-radio-button__inner').filter({ hasText: label }).first().click()
}

async function collectProductMasterDiagnostics(page, item) {
  const textFrom = async (locator, limit = 10) =>
    await locator.evaluateAll((nodes, max) =>
      nodes
        .map((node) => node.innerText || node.textContent || '')
        .map((text) => text.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
        .slice(0, max),
    limit).catch((error) => [`<unable to read locator: ${error.message}>`])

  return {
    url: page.url(),
    productFormTexts: await textFrom(item),
    visibleProductOptions: await textFrom(page.locator('.el-select-dropdown__item:visible')),
    formErrors: await textFrom(page.locator('.el-form-item__error:visible')),
    messages: await textFrom(page.locator('.el-message:visible')),
    alerts: await textFrom(page.locator('.el-alert:visible'))
  }
}

async function waitForProductMasterSelectionThroughUi(page, item, scenario, optionLabel) {
  const deadline = Date.now() + config.submitResponseTimeout
  let lastState = null
  while (Date.now() < deadline) {
    lastState = await item.locator('.el-select').first().evaluate((selectElement, expectedLabel) => {
      const selectedText = (selectElement.innerText || selectElement.textContent || '').replace(/\s+/g, ' ').trim()
      const inputValue = (selectElement.querySelector('input')?.value || '').replace(/\s+/g, ' ').trim()
      const values = [selectedText, inputValue].filter(Boolean)
      const matched = values.some((value) =>
        expectedLabel ? expectedLabel.includes(value) || value.includes(expectedLabel) : value.length > 0
      )
      return { selectedText, inputValue, matched }
    }, optionLabel).catch((error) => ({ error: error.message }))
    if (lastState?.matched) {
      return
    }
    await page.waitForTimeout(250)
  }

  const diagnostics = await collectProductMasterDiagnostics(page, item)
  throw new Error(
    `DCC product master selection did not stabilize for ${scenario.contentType} after selecting ${optionLabel || '<empty>'}: ` +
    `lastState=${JSON.stringify(lastState)}; diagnostics=${JSON.stringify(diagnostics)}`
  )
}

async function waitForProductMasterOptionThroughUi(page, item, scenario) {
  let option = page.locator('.el-select-dropdown__item:visible')
    .filter({ hasNotText: '无数据' })
    .first()
  if (config.productKeyword) {
    option = page.locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.productKeyword })
      .first()
  }
  try {
    await option.waitFor({ state: 'visible', timeout: config.submitResponseTimeout })
    return option
  } catch (error) {
    const diagnostics = await collectProductMasterDiagnostics(page, item)
    throw new Error(
      `DCC product master option did not appear for ${scenario.contentType} keyword=${config.productKeyword || '<empty>'}: ` +
      `diagnostics=${JSON.stringify(diagnostics)}; cause=${error.message}`
    )
  }
}

async function selectProductMasterThroughUi(page, scenario) {
  const item = formItem(page, '产品编号')
  const select = item.locator('.el-select').first()
  await select.click()
  const input = item.locator('input[role="combobox"], input').first()
  if (config.productKeyword) {
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/dcc/controlled-files/product-options')
        && response.url().includes(encodeURIComponent(config.productKeyword)),
      { timeout: config.submitResponseTimeout }
    ).catch(() => null)
    await input.fill(config.productKeyword)
    await responsePromise
  }
  const option = await waitForProductMasterOptionThroughUi(page, item, scenario)
  const optionLabel = await clickVisibleDropdownOptionByPoint(page, option, '产品编号')
  await waitForProductMasterSelectionThroughUi(page, item, scenario, optionLabel)
  return optionLabel
}

async function collectRevisionTargetDiagnostics(section) {
  return await section.evaluate((node) => ({
    text: (node.innerText || node.textContent || '').replace(/\s+/g, ' ').trim(),
    selectedText: Array.from(node.querySelectorAll('.el-table__row.current-row, .el-table__row.is-current'))
      .map((row) => (row.innerText || row.textContent || '').replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  })).catch((error) => ({ error: error.message }))
}

async function selectRevisionTargetThroughUi(page, scenario, fileNumber, revisionTargetControlledFileId = null) {
  const section = page.locator('[data-testid="dcc-upload-revision-candidates"]').first()
  await section.waitFor({ state: 'visible', timeout: config.timeout })
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/controlled-files/upload-revision-candidates') &&
      response.url().includes(encodeURIComponent(fileNumber)),
    { timeout: config.submitResponseTimeout }
  ).catch(() => null)
  await section.locator('input').first().fill(fileNumber)
  await section.getByRole('button', { name: '查询' }).click()
  const response = await responsePromise
  let revisionTargetRowIndex = 0
  if (response && revisionTargetControlledFileId) {
    const payload = await parseApiResponsePayload(response, `DCC ${scenario.contentType} revision candidates`)
    const candidates = Array.isArray(payload.data?.list)
      ? payload.data.list
      : Array.isArray(payload.data)
        ? payload.data
        : []
    revisionTargetRowIndex = candidates.findIndex((candidate) => Number(candidate.id) === Number(revisionTargetControlledFileId))
    if (revisionTargetRowIndex < 0) {
      throw new Error(
        `DCC revision targetControlledFileId was not returned for ${scenario.contentType}: ` +
        `revisionTargetControlledFileId=${revisionTargetControlledFileId}, fileNumber=${fileNumber}`
      )
    }
  }
  await section.locator('.el-loading-mask').first()
    .waitFor({ state: 'hidden', timeout: config.submitResponseTimeout })
    .catch(() => {})
  const row = section.locator('.el-table__row:visible').filter({ hasText: fileNumber }).nth(revisionTargetRowIndex)
  const rowVisible = await row.waitFor({ state: 'visible', timeout: config.submitResponseTimeout })
    .then(() => true)
    .catch(() => false)
  if (!rowVisible) {
    const diagnostics = await collectRevisionTargetDiagnostics(section)
    throw new Error(
      `DCC revision target option did not appear for ${scenario.contentType} fileNumber=${fileNumber}: ` +
      `diagnostics=${JSON.stringify(diagnostics)}`
    )
  }
  await row.getByRole('button', { name: '选择' }).click()
  await section.getByText(`已选择：${fileNumber}`, { exact: false })
    .waitFor({ state: 'visible', timeout: config.submitResponseTimeout })
}

async function submitButtonState(submitButton) {
  if (!submitButton) return null
  return await submitButton.evaluate((button) => ({
    text: (button.innerText || button.textContent || '').replace(/\s+/g, ' ').trim(),
    disabled: Boolean(button.disabled) ||
      button.getAttribute('aria-disabled') === 'true' ||
      button.classList.contains('is-disabled'),
    className: button.className,
    loading: Boolean(button.querySelector('.is-loading, .el-icon.is-loading'))
  })).catch((error) => ({ error: error.message }))
}

async function collectVisibleSubmitFeedback(page, submitButton) {
  const selectorMap = {
    formErrors: '.el-form-item__error:visible',
    messages: '.el-message:visible',
    alerts: '.el-alert:visible',
    dialogs: '.el-dialog:visible',
    currentVersion: '[data-testid="dcc-upload-current-version-panel"], [data-testid="dcc-current-version-panel"], [data-testid="dcc-current-version-projection"], [data-testid="dcc-current-version-block-reason"]'
  }
  const feedback = {
    url: page.url(),
    submitButtonState: await submitButtonState(submitButton)
  }
  for (const [key, selector] of Object.entries(selectorMap)) {
    const texts = await page.locator(selector).evaluateAll((nodes) =>
      nodes
        .map((node) => node.innerText || node.textContent || '')
        .map((text) => text.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
        .slice(0, 10)
    ).catch((error) => [`<unable to read ${selector}: ${error.message}>`])
    feedback[key] = uniqueMessages(texts)
  }
  return feedback
}

async function collectImmediateSubmitFeedback(page, submitButton) {
  await page.waitForTimeout(500)
  return await collectVisibleSubmitFeedback(page, submitButton)
}

async function waitForSubmitResponseAfterClick(page, scenario) {
  const submitButton = page
    .locator('section[data-testid="dcc-upload-section-submit"]')
    .getByRole('button', { name: '提交审批' })
    .first()
  await submitButton.waitFor({ state: 'visible', timeout: config.timeout })
  const beforeClickFeedback = await collectVisibleSubmitFeedback(page, submitButton)
  if (beforeClickFeedback.submitButtonState?.disabled) {
    throw new Error(
      `DCC submit button is disabled before click for ${scenario.contentType}: ` +
      `beforeClickFeedback=${JSON.stringify(beforeClickFeedback)}`
    )
  }
  let submitRequestObserved = false
  const observeSubmitRequest = (request) => {
    if (
      request.url().includes('/admin-api/dcc/controlled-files/submit') &&
      request.method() === 'POST'
    ) {
      submitRequestObserved = true
    }
  }
  page.on('request', observeSubmitRequest)
  const submitResponsePromise = page.waitForResponse(
    (response) => {
      const matched = response.url().includes('/admin-api/dcc/controlled-files/submit') &&
        response.request().method() === 'POST'
      if (matched) {
        submitRequestObserved = true
      }
      return matched
    },
    { timeout: config.submitResponseTimeout }
  ).then((response) => ({ response })).catch((error) => ({ error }))
  const clickResult = await submitButton.click()
    .then(() => ({ ok: true }))
    .catch((error) => ({ error }))
  if (clickResult.error) {
    const clickErrorFeedback = await collectVisibleSubmitFeedback(page, submitButton)
    page.off('request', observeSubmitRequest)
    throw new Error(
      `DCC submit click did not complete before a /submit request was observed for ${scenario.contentType}: ` +
      `submitRequestObserved=${submitRequestObserved}; ` +
      `error=${clickResult.error.message}; beforeClickFeedback=${JSON.stringify(beforeClickFeedback)}; ` +
      `clickErrorFeedback=${JSON.stringify(clickErrorFeedback)}`
    )
  }
  const immediateResult = await Promise.race([
    submitResponsePromise,
    collectImmediateSubmitFeedback(page, submitButton).then((feedback) => ({ feedback }))
  ])
  if (immediateResult.response) {
    page.off('request', observeSubmitRequest)
    return immediateResult.response
  }
  const submitResult = await submitResponsePromise
  if (submitResult.response) {
    page.off('request', observeSubmitRequest)
    return submitResult.response
  }
  const delayedFeedback = await collectVisibleSubmitFeedback(page, submitButton)
  page.off('request', observeSubmitRequest)
  throw new Error(
    `DCC submit did not reach /submit for ${scenario.contentType} within ${config.submitResponseTimeout}ms: ` +
    `${submitResult.error.message}; submitRequestObserved=${submitRequestObserved}; ` +
    `beforeClickFeedback=${JSON.stringify(beforeClickFeedback)}; immediateFeedback=${JSON.stringify(immediateResult.feedback || null)}; ` +
    `delayedFeedback=${JSON.stringify(delayedFeedback)}`
  )
}

async function waitForUploadPreviewReadyThroughUi(page, scenario) {
  const attachmentSection = page.locator('section[data-testid="dcc-upload-section-attachment"]').first()
  await attachmentSection
    .getByText('预览文件：', { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
  await attachmentSection
    .getByText(path.basename(config.sourceFilePath), { exact: false })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })

  const currentVersionPanel = page.locator('[data-testid="dcc-upload-current-version-panel"]').first()
  if (await currentVersionPanel.isVisible().catch(() => false)) {
    await currentVersionPanel
      .locator('.el-skeleton')
      .first()
      .waitFor({ state: 'hidden', timeout: config.timeout })
      .catch(() => {})
  }

  await page
    .locator('section[data-testid="dcc-upload-section-submit"]')
    .getByRole('button', { name: '提交审批' })
    .first()
    .waitFor({ state: 'visible', timeout: config.timeout })
}

async function submitControlledFileThroughUi(page, scenario, runId, options = {}) {
  await page.goto(new URL('/dcc/controlled-file/upload', config.baseUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  await selectDccProjectThroughUi(page, scenario)
  await selectFileTypeTaxonomyThroughUi(page, scenario)
  await selectOptionByFormLabel(page, '文件类别', scenario.categoryName)
  const leafReady = await page.getByText('当前绑定目录已经是最后一层目录', { exact: false })
    .first()
    .isVisible({ timeout: 5000 })
    .catch(() => false)
  if (!leafReady) {
    await selectUploadLeafDirectoryThroughUi(page, scenario)
  }
  await selectProductMasterThroughUi(page, scenario)

  const fileNumber = options.fileNumber || buildDccE2eFileNumber(scenario, runId, options.fileNumberSuffix)
  const fileName = options.fileName || `受控状态机E2E-${scenario.contentType}-${runId}`
  const versionNo = options.versionNo || 'V1.0'
  const changeType = options.changeType || 'NEW'
  const revisionTargetControlledFileId = options.revisionTargetControlledFileId || null
  await fillFormInput(page, '文件名称', fileName)
  await fillFormInput(page, '文件编号', fileNumber)
  await chooseRadioButton(page, changeType === 'REVISION' ? '升版' : changeType === 'OBSOLETE' ? '作废' : '新建')
  if (changeType === 'REVISION') {
    await selectRevisionTargetThroughUi(page, scenario, fileNumber, revisionTargetControlledFileId)
  }
  await fillFormInput(page, '版本号', versionNo)
  await fillFormInput(page, '生效日期', formatDate(new Date()))
  await fillFormTextarea(page, '提交备注', `Codex DCC controlled-content ${scenario.fullFlowPlan} ${runId}`)

  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await page
    .locator('section[data-testid="dcc-upload-section-attachment"] input[type="file"]')
    .first()
    .setInputFiles(config.sourceFilePath)
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await parseApiResponsePayload(uploadResponse, `DCC ${scenario.contentType} source upload`)
  if (!uploadResponse.ok() || ![0, 200].includes(uploadPayload.code)) {
    throw new Error(`DCC ${scenario.contentType} source upload failed: HTTP ${uploadResponse.status()} ${uploadPayload.msg || JSON.stringify(uploadPayload)}`)
  }
  await waitForUploadPreviewReadyThroughUi(page, scenario)

  const submitResponse = await waitForSubmitResponseAfterClick(page, scenario)
  const submitPayload = await parseApiResponsePayload(submitResponse, `DCC ${scenario.contentType} submit`)
  if (!submitResponse.ok() || ![0, 200].includes(submitPayload.code)) {
    const submitButton = page
      .locator('section[data-testid="dcc-upload-section-submit"]')
      .getByRole('button', { name: '提交审批' })
      .first()
    const afterSubmitFailureFeedback = await collectVisibleSubmitFeedback(page, submitButton)
    throw new Error(
      `DCC submit returned non-success payload for ${scenario.contentType}: HTTP ${submitResponse.status()} ` +
      `submitResponseBody=${JSON.stringify(submitPayload)}; ` +
      `afterSubmitFailureFeedback=${JSON.stringify(afterSubmitFailureFeedback)}`
    )
  }
  return {
    controlledFileId: submitPayload.data,
    fileNumber,
    fileName,
    versionNo,
    changeType
  }
}

async function openActorPage(browser, actor, targetPath) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.on('request', trackUiWriteRequest)
  await login(page, actor, targetPath)
  return { context, page }
}

function accountKey(account) {
  return String(account?.userId || account?.username || JSON.stringify(account))
}

function uniqueApprovalAccountsForRoute(scenario, activeRoute) {
  const approvalUsers = parseApprovalUsersJson()
  const byKey = new Map()
  for (const [index, node] of (activeRoute?.nodes || []).entries()) {
    const nodeWithIndex = { ...node, index }
    const accounts = accountList(approvalAccountForStage(approvalUsers, scenario, nodeWithIndex)).filter(accountIsComplete)
    if (!accounts.length) {
      throw new Error(`DCC approval account missing for ${scenario.contentType} stage ${node.stageNo} ${node.stageName || ''}`.trim())
    }
    for (const account of accounts) {
      byKey.set(accountKey(account), account)
    }
  }
  return [...byKey.values()]
}

function approvalAccountsForRoute(scenario, activeRoute) {
  return uniqueApprovalAccountsForRoute(scenario, activeRoute)
}

function currentApprovalTaskAssigneeId(task) {
  const value = task?.assigneeUser?.id ?? task?.assigneeUserId ?? task?.assignee
  const numeric = Number(value)
  return Number.isFinite(numeric) && numeric > 0 ? numeric : null
}

function currentApprovalTaskDefinitionKey(task) {
  return task?.taskDefinitionKey || task?.definitionKey || task?.taskDefKey || ''
}

function isCurrentRunningApprovalTask(task) {
  const status = task?.status
  return status === 1 || status === 7 || status === '1' || status === '7' ||
    status === 'RUNNING' || status === 'APPROVING'
}

async function getCurrentPendingApprovalAccount(page, controlledFileId, accounts) {
  const detail = await apiGet(page, `/dcc/controlled-files/${controlledFileId}`)
  if (!detail?.processInstanceId) {
    return null
  }
  const tasks = await apiGet(
    page,
    `/bpm/task/list-by-process-instance-id?processInstanceId=${encodeURIComponent(detail.processInstanceId)}`
  )
  const runningTasks = (Array.isArray(tasks) ? tasks : []).filter(isCurrentRunningApprovalTask)
  if (!runningTasks.length) {
    return null
  }
  for (const task of runningTasks) {
    const assigneeId = currentApprovalTaskAssigneeId(task)
    const account = accounts.find((item) => Number(item.userId) === assigneeId)
    if (account) {
      return {
        account,
        task,
        detail,
        finalApproval:
          currentApprovalTaskDefinitionKey(task) === 'DOC_CONTROL_APPROVAL' ||
          detail.status === 'PENDING_DOC_CONTROL_APPROVAL'
      }
    }
  }
  throw new Error(
    `DCC approval current task did not match configured approval accounts for controlledFileId=${controlledFileId}: ` +
    `runningTasks=${JSON.stringify(runningTasks.map((task) => ({
      id: task.id,
      name: task.name,
      status: task.status,
      taskDefinitionKey: currentApprovalTaskDefinitionKey(task),
      assigneeUserId: currentApprovalTaskAssigneeId(task)
    })))}; configuredUserIds=${accounts.map((account) => account.userId).join(',')}`
  )
}

async function collectApprovalActionDiagnostics(page, scenario, controlledFileId, actor, stageIndex, stageCount) {
  const textFrom = async (locator, limit = 12) =>
    await locator.evaluateAll((nodes, max) =>
      nodes
        .map((node) => node.innerText || node.textContent || '')
        .map((text) => text.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
        .slice(0, max),
    limit).catch((error) => [`<unable to read locator: ${error.message}>`])

  return {
    url: page.url(),
    contentType: scenario.contentType,
    controlledFileId,
    stageIndex,
    stageCount,
    actorUsername: actor.username,
    actorUserId: actor.userId,
    visibleButtons: await textFrom(page.locator('button:visible')),
    pageErrors: await textFrom(page.locator('.el-message:visible, .el-alert:visible, .el-result:visible, .el-empty:visible')),
    handlingSummary: await textFrom(page.locator('[data-testid="dcc-detail-handling-summary"]')),
    approvalStageProgress: await textFrom(page.locator('body'), 4)
  }
}

async function openApprovalDialogWithDiagnostics(page, scenario, controlledFileId, actor, stageIndex, stageCount) {
  const button = page.getByRole('button', { name: /审核通过|批准通过/ }).first()
  const visible = await button
    .waitFor({ state: 'visible', timeout: config.submitResponseTimeout })
    .then(() => true)
    .catch((error) => ({ error }))
  if (visible !== true) {
    const diagnostics = await collectApprovalActionDiagnostics(page, scenario, controlledFileId, actor, stageIndex, stageCount)
    throw new Error(
      `DCC approval action button is not visible for ${scenario.contentType} controlledFileId=${controlledFileId} ` +
      `stage=${stageIndex}/${stageCount} actor=${actor.username} actorUserId=${actor.userId}: ` +
      `${visible.error.message}; diagnostics=${JSON.stringify(diagnostics)}`
    )
  }
  await button.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '签名' }).first()
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  return dialog
}

async function selectDistributionDepartment(page, dialog) {
  const treeSelect = dialog.locator('[data-testid="dcc-doc-control-distribution-departments"]').first()
  await treeSelect.click()
  await treeSelect.locator('input').first().fill(config.distributionDepartmentLabel)
  const node = page.locator('.el-tree-node:visible').filter({ hasText: config.distributionDepartmentLabel }).first()
  await node.waitFor({ state: 'visible', timeout: config.timeout })
  await node.locator('.el-checkbox').first().click()
  await dialog.locator('.el-dialog__header').click({ force: true })
}

async function collectStampedPdfUploadDiagnostics(page, dialog, scenario) {
  const textFrom = async (locator, limit = 10) =>
    await locator.evaluateAll((nodes, max) =>
      nodes
        .map((node) => node.innerText || node.textContent || node.getAttribute('value') || '')
        .map((text) => text.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
        .slice(0, max),
    limit).catch((error) => [`<unable to read locator: ${error.message}>`])

  const stampedSection = dialog.locator('.el-form-item:visible').filter({ hasText: '盖章 PDF' }).first()
  return {
    url: page.url(),
    contentType: scenario.contentType,
    stampedPdfPath: config.stampedPdfPath,
    stampedPdfFileName: path.basename(config.stampedPdfPath || ''),
    stampedSectionText: await textFrom(stampedSection, 3),
    visibleButtons: await textFrom(dialog.locator('button:visible')),
    fileInputCount: await stampedSection.locator('input[type="file"]').count().catch((error) => `<count failed: ${error.message}>`),
    uploadedFileNames: await textFrom(stampedSection.locator('.el-upload-list__item-name, .el-upload-list__item, .el-upload__tip'), 6),
    pageErrors: await textFrom(page.locator('.el-message:visible, .el-alert:visible, .el-result:visible, .el-empty:visible'))
  }
}

async function uploadStampedPdfWithDiagnostics(page, dialog, scenario) {
  const stampedSection = dialog.locator('.el-form-item:visible').filter({ hasText: '盖章 PDF' }).first()
  await stampedSection.waitFor({ state: 'visible', timeout: config.timeout })
  const fileInput = stampedSection.locator('input[type="file"]').first()
  const [stampedResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/upload-preview') &&
        response.request().method() === 'POST',
      { timeout: config.timeout }
    ).catch(async (error) => {
      const diagnostics = await collectStampedPdfUploadDiagnostics(page, dialog, scenario)
      throw new Error(
        `DCC stamped PDF upload did not reach /upload-preview for ${scenario.contentType}: ` +
        `${error.message}; diagnostics=${JSON.stringify(diagnostics)}`
      )
    }),
    fileInput.setInputFiles(config.stampedPdfPath)
  ])
  const stampedPayload = await parseApiResponsePayload(stampedResponse, `DCC ${scenario.contentType} stamped PDF upload`)
  if (!stampedResponse.ok() || ![0, 200].includes(stampedPayload.code)) {
    const diagnostics = await collectStampedPdfUploadDiagnostics(page, dialog, scenario)
    throw new Error(
      `DCC ${scenario.contentType} stamped PDF upload failed: HTTP ${stampedResponse.status()} ` +
      `${stampedPayload.msg || JSON.stringify(stampedPayload)}; diagnostics=${JSON.stringify(diagnostics)}`
    )
  }
  await dialog.getByText(path.basename(config.stampedPdfPath), { exact: false }).last()
    .waitFor({ state: 'visible', timeout: config.timeout })
}

async function approveCurrentStageThroughUi(browser, scenario, controlledFileId, actor, stageIndex, stageCount, options = {}) {
  const { context, page } = await openActorPage(browser, actor, `/dcc/controlled-file/detail/${controlledFileId}`)
  try {
    await waitForDetailPageReady(page, controlledFileId)
    const dialog = await openApprovalDialogWithDiagnostics(page, scenario, controlledFileId, actor, stageIndex, stageCount)
    await dialog.locator('input[type="password"]').first().fill(actor.password)
    await dialog.locator('textarea').first().fill(`Codex DCC ${scenario.fullFlowPlan} approve stage ${stageIndex}`)
    if (options.finalApproval) {
      await uploadStampedPdfWithDiagnostics(page, dialog, scenario)
      await selectDistributionDepartment(page, dialog)
    }
    const [approveResponse] = await Promise.all([
      page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/dcc/controlled-files/') &&
          response.url().includes('/approve-task') &&
          response.request().method() === 'POST',
        { timeout: config.timeout }
      ),
      dialog.getByRole('button', { name: '确认签名' }).click()
    ])
    const approvePayload = await parseApiResponsePayload(approveResponse, `DCC ${scenario.contentType} approve stage ${stageIndex}`)
    if (!approveResponse.ok() || ![0, 200].includes(approvePayload.code)) {
      throw new Error(`DCC ${scenario.contentType} approve stage ${stageIndex} failed: HTTP ${approveResponse.status()} ${approvePayload.msg || JSON.stringify(approvePayload)}`)
    }
    return approvePayload.data
  } finally {
    await context.close()
  }
}

async function approveCurrentPendingTaskThroughUi(browser, scenario, controlledFileId, pending, stageIndex, stageCount) {
  return await approveCurrentStageThroughUi(
    browser,
    scenario,
    controlledFileId,
    pending.account,
    stageIndex,
    stageCount,
    { finalApproval: pending.finalApproval }
  )
}

async function approveAllStagesThroughUi(browser, page, scenario, controlledFileId, activeRoute) {
  const accounts = approvalAccountsForRoute(scenario, activeRoute)
  const maxApprovalSteps = Math.max(activeRoute?.nodes?.length || 0, accounts.length) + 2
  for (let index = 0; index < maxApprovalSteps; index += 1) {
    const pending = await getCurrentPendingApprovalAccount(page, controlledFileId, accounts)
    if (!pending) {
      return
    }
    await approveCurrentPendingTaskThroughUi(browser, scenario, controlledFileId, pending, index + 1, maxApprovalSteps)
  }
  const pendingAfterMaxApprovalSteps = await getCurrentPendingApprovalAccount(page, controlledFileId, accounts)
  if (!pendingAfterMaxApprovalSteps) {
    return
  }
  throw new Error(
    `DCC approval did not complete within expected current-task loop for ${scenario.contentType}: ` +
    `controlledFileId=${controlledFileId}, maxApprovalSteps=${maxApprovalSteps}`
  )
}

async function createReleasedControlledFileThroughUi(browser, scenario, preflight, runId, suffix) {
  const activeRoute = approvalRouteForExecution(preflight)
  if (!activeRoute || !activeRoute.nodes?.length) {
    throw new Error(`DCC ${scenario.contentType} full-flow requires an active route with nodes`)
  }
  const { context, page } = await openActorPage(browser, config, '/dcc/controlled-file/upload')
  try {
    const submitted = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'NEW',
      fileNumberSuffix: suffix,
      fileName: `受控状态机E2E-${scenario.contentType}-${suffix}-${runId}`,
      versionNo: 'V1.0'
    })
    await approveAllStagesThroughUi(browser, page, scenario, submitted.controlledFileId, activeRoute)
    return {
      ...submitted,
      detail: await apiGet(page, `/dcc/controlled-files/${submitted.controlledFileId}`)
    }
  } finally {
    await context.close()
  }
}

async function openDetailPage(page, controlledFileId) {
  await page.goto(new URL(`/dcc/controlled-file/detail/${controlledFileId}`, config.baseUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await waitForDetailPageReady(page, controlledFileId)
}

async function collectDetailPageDiagnostics(page, controlledFileId) {
  const selectorMap = {
    url: page.url(),
    handlingSummaryVisible: await page.locator('[data-testid="dcc-detail-handling-summary"]').first()
      .isVisible()
      .catch(() => false),
    approvalStageProgressVisible: await page.getByText('审批阶段进度', { exact: false }).first()
      .isVisible()
      .catch(() => false)
  }
  for (const [key, selector] of Object.entries({
    pageErrors: '.el-message:visible, .el-alert:visible, .el-result:visible, .el-empty:visible',
    detailTexts: 'body'
  })) {
    selectorMap[key] = await page.locator(selector).evaluateAll((nodes) =>
      nodes
        .map((node) => node.innerText || node.textContent || '')
        .map((text) => text.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
        .slice(0, 8)
    ).catch((error) => [`<unable to read ${selector}: ${error.message}>`])
  }
  selectorMap.controlledFileId = controlledFileId
  return selectorMap
}

async function waitForDetailPageReady(page, controlledFileId) {
  const readiness = await Promise.race([
    page.locator('[data-testid="dcc-detail-handling-summary"]').first()
      .waitFor({ state: 'visible', timeout: config.timeout })
      .then(() => 'handling-summary'),
    page.getByText('审批阶段进度', { exact: false }).first()
      .waitFor({ state: 'visible', timeout: config.timeout })
      .then(() => 'approval-stage-progress')
  ]).catch((error) => ({ error }))
  if (typeof readiness === 'string') {
    return readiness
  }
  const diagnostics = await collectDetailPageDiagnostics(page, controlledFileId)
  throw new Error(
    `DCC detail page did not expose handling summary or detail key text for controlledFileId=${controlledFileId}: ` +
    `${readiness.error.message}; diagnostics=${JSON.stringify(diagnostics)}`
  )
}

async function isDropdownActionVisible(page, buttonName, actionText) {
  const button = page.getByRole('button', { name: buttonName }).first()
  if (!(await button.isVisible().catch(() => false))) {
    return false
  }
  await button.click()
  const action = page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item')
    .filter({ hasText: actionText })
    .first()
  const visible = await action.isVisible({ timeout: 2000 }).catch(() => false)
  await page.keyboard.press('Escape').catch(() => {})
  return visible
}

async function clickDangerAction(page, actionText) {
  await page.getByRole('button', { name: '风险操作' }).first().click()
  const action = page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item')
    .filter({ hasText: actionText })
    .first()
  await action.waitFor({ state: 'visible', timeout: config.timeout })
  await action.click()
}

async function confirmElementMessageBox(page) {
  const messageBox = page.locator('.el-message-box:visible').last()
  await messageBox.waitFor({ state: 'visible', timeout: config.timeout })
  await messageBox.getByRole('button', { name: /确定|确认/ }).last().click()
}

async function withdrawControlledFileThroughUi(page, controlledFileId) {
  await openDetailPage(page, controlledFileId)
  await clickDangerAction(page, '撤回申请')
  const [withdrawResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/controlled-files/${controlledFileId}/withdraw`) &&
        response.request().method() === 'POST',
      { timeout: config.timeout }
    ),
    confirmElementMessageBox(page)
  ])
  const withdrawPayload = await parseApiResponsePayload(withdrawResponse, `DCC ${controlledFileId} withdraw`)
  if (!withdrawResponse.ok() || ![0, 200].includes(withdrawPayload.code)) {
    throw new Error(`DCC ${controlledFileId} withdraw failed: HTTP ${withdrawResponse.status()} ${withdrawPayload.msg || JSON.stringify(withdrawPayload)}`)
  }
}

async function resubmitWithdrawnControlledFileThroughUi(page, controlledFileId) {
  await openDetailPage(page, controlledFileId)
  await clickDangerAction(page, '重新提交')
  const [resubmitResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/dcc/controlled-files/${controlledFileId}/resubmit`) &&
        response.request().method() === 'POST',
      { timeout: config.timeout }
    ),
    confirmElementMessageBox(page)
  ])
  const resubmitPayload = await parseApiResponsePayload(resubmitResponse, `DCC ${controlledFileId} resubmit`)
  if (!resubmitResponse.ok() || ![0, 200].includes(resubmitPayload.code)) {
    throw new Error(`DCC ${controlledFileId} resubmit failed: HTTP ${resubmitResponse.status()} ${resubmitPayload.msg || JSON.stringify(resubmitPayload)}`)
  }
  await page.waitForURL((current) => current.pathname.includes(`/dcc/controlled-file/detail/${resubmitPayload.data}`), {
    timeout: config.timeout,
    waitUntil: 'commit'
  }).catch(() => {})
  return resubmitPayload.data
}

async function runReleaseFullFlow(browser, scenario, preflight) {
  const activeRoute = approvalRouteForExecution(preflight)
  if (!activeRoute || !activeRoute.nodes?.length) {
    throw new Error(`DCC ${scenario.contentType} full-flow requires an active route with nodes`)
  }
  const runId = createTraceableRunId()
  const { context, page } = await openActorPage(browser, config, '/dcc/controlled-file/upload')
  try {
    const initial = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'NEW',
      versionNo: 'V1.0'
    })
    await approveAllStagesThroughUi(browser, page, scenario, initial.controlledFileId, activeRoute)
    const initialActive = await apiGet(page, `/dcc/controlled-files/${initial.controlledFileId}`)
    const revision = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'REVISION',
      fileNumber: initial.fileNumber,
      fileName: initial.fileName,
      versionNo: 'V2.0'
    })
    await approveAllStagesThroughUi(browser, page, scenario, revision.controlledFileId, activeRoute)
    const oldDetail = await apiGet(page, `/dcc/controlled-files/${initial.controlledFileId}`)
    const newDetail = await apiGet(page, `/dcc/controlled-files/${revision.controlledFileId}`)
    return {
      status: 'PASS',
      fullFlowPlan: scenario.fullFlowPlan,
      initial: {
        controlledFileId: initial.controlledFileId,
        statusAfterFirstRelease: initialActive.status,
        versionNo: initial.versionNo
      },
      revision: {
        controlledFileId: revision.controlledFileId,
        versionNo: revision.versionNo
      },
      finalAssertions: {
        oldStatus: oldDetail.status,
        newStatus: newDetail.status,
        oldSuperseded: oldDetail.status === 'SUPERSEDED',
        newActive: newDetail.status === 'ACTIVE'
      }
    }
  } finally {
    await context.close()
  }
}

async function runPendingReadonlyFullFlow(browser, scenario) {
  const runId = createTraceableRunId()
  const { context, page } = await openActorPage(browser, config, '/dcc/controlled-file/upload')
  try {
    const pending = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'NEW',
      versionNo: 'V1.0'
    })
    await openDetailPage(page, pending.controlledFileId)
    const metadataEditVisible = await isDropdownActionVisible(page, '更多', '修改基础信息')
    const obsoleteVisible = await isDropdownActionVisible(page, '风险操作', '作废当前版本')
    const detail = await apiGet(page, `/dcc/controlled-files/${pending.controlledFileId}`)
    return {
      status: 'PASS',
      fullFlowPlan: scenario.fullFlowPlan,
      pending: {
        controlledFileId: pending.controlledFileId,
        fileNumber: pending.fileNumber,
        versionNo: pending.versionNo,
        status: detail.status
      },
      finalAssertions: {
        reviewStatus: detail.status,
        uiReadonly: !metadataEditVisible && !obsoleteVisible,
        directEditRejected: !metadataEditVisible
      }
    }
  } finally {
    await context.close()
  }
}

async function runWithdrawResubmitFullFlow(browser, scenario, preflight) {
  const activeRoute = approvalRouteForExecution(preflight)
  const runId = createTraceableRunId()
  const source = await createReleasedControlledFileThroughUi(browser, scenario, preflight, runId, 'WITHDRAW-SOURCE')
  const { context, page } = await openActorPage(browser, config, '/dcc/controlled-file/upload')
  try {
    const revision = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'REVISION',
      fileNumber: source.fileNumber,
      fileName: source.fileName,
      versionNo: 'V2.0'
    })
    await withdrawControlledFileThroughUi(page, revision.controlledFileId)
    const withdrawnDetail = await apiGet(page, `/dcc/controlled-files/${revision.controlledFileId}`)
    const successorId = await resubmitWithdrawnControlledFileThroughUi(page, revision.controlledFileId)
    const successorDetail = await apiGet(page, `/dcc/controlled-files/${successorId}`)
    const sourceAfter = await apiGet(page, `/dcc/controlled-files/${source.controlledFileId}`)
    return {
      status: 'PASS',
      fullFlowPlan: scenario.fullFlowPlan,
      source: {
        controlledFileId: source.controlledFileId,
        status: sourceAfter.status
      },
      withdrawn: {
        controlledFileId: revision.controlledFileId,
        status: withdrawnDetail.status
      },
      successor: {
        controlledFileId: successorId,
        status: successorDetail.status
      },
      finalAssertions: {
        withdrawnStatus: withdrawnDetail.status,
        newDraftCount: successorId ? 1 : 0,
        sourceActiveUnchanged: sourceAfter.status === 'ACTIVE'
      }
    }
  } finally {
    await context.close()
  }
}

async function runObsoleteFullFlow(browser, scenario, preflight) {
  const activeRoute = approvalRouteForExecution(preflight)
  const runId = createTraceableRunId()
  const source = await createReleasedControlledFileThroughUi(browser, scenario, preflight, runId, 'OBSOLETE-SOURCE')
  const { context, page } = await openActorPage(browser, config, '/dcc/controlled-file/upload')
  try {
    const obsoleteRequest = await submitControlledFileThroughUi(page, scenario, runId, {
      changeType: 'OBSOLETE',
      fileNumber: source.fileNumber,
      fileName: source.fileName,
      versionNo: 'V1.1'
    })
    await approveAllStagesThroughUi(browser, page, scenario, obsoleteRequest.controlledFileId, activeRoute)
    const sourceAfter = await apiGet(page, `/dcc/controlled-files/${source.controlledFileId}`)
    const obsoleteAfter = await apiGet(page, `/dcc/controlled-files/${obsoleteRequest.controlledFileId}`)
    const activeCount = await browserActiveCount(page, scenario, source.fileNumber)
    const finalAssertions = assertObsoleteTerminalState(sourceAfter, obsoleteAfter, activeCount)
    return {
      status: 'PASS',
      fullFlowPlan: scenario.fullFlowPlan,
      source: {
        controlledFileId: source.controlledFileId,
        status: sourceAfter.status
      },
      obsoleteRequest: {
        controlledFileId: obsoleteRequest.controlledFileId,
        status: obsoleteAfter.status
      },
      finalAssertions
    }
  } finally {
    await context.close()
  }
}

function assertObsoleteTerminalState(sourceAfter, obsoleteAfter, activeCount) {
  const finalAssertions = {
    sourceStatus: sourceAfter.status,
    obsoleteRequestStatus: obsoleteAfter.status,
    masterCurrentActiveCleared: activeCount === 0,
    activeCount,
    noFallbackToOldVersion: sourceAfter.status !== 'ACTIVE'
  }
  const failures = []
  if (sourceAfter.status === 'ACTIVE') {
    failures.push(`sourceStatus=${sourceAfter.status}`)
  }
  if (obsoleteAfter.status !== 'OBSOLETE') {
    failures.push(`obsoleteRequestStatus=${obsoleteAfter.status}`)
  }
  if (activeCount !== 0) {
    failures.push(`activeCount=${activeCount}`)
  }
  if (failures.length) {
    throw new Error(`DCC obsolete final assertions failed: ${failures.join(', ')}`)
  }
  return finalAssertions
}

async function runScenarioFullFlow(browser, scenario, preflight) {
  if (scenario.fullFlowPlan === 'release') {
    return await runReleaseFullFlow(browser, scenario, preflight)
  }
  if (scenario.fullFlowPlan === 'pending-readonly') {
    return await runPendingReadonlyFullFlow(browser, scenario, preflight)
  }
  if (scenario.fullFlowPlan === 'withdraw-resubmit') {
    return await runWithdrawResubmitFullFlow(browser, scenario, preflight)
  }
  if (scenario.fullFlowPlan === 'obsolete') {
    return await runObsoleteFullFlow(browser, scenario, preflight)
  }
  throw new Error(`DCC ${scenario.contentType} has unsupported full-flow plan: ${scenario.fullFlowPlan}`)
}

async function browserActiveCount(page, scenario, fileNumber = null) {
  const params = new URLSearchParams({
    pageNo: '1',
    pageSize: '50',
    categoryId: String(scenario.categoryId),
    status: 'ACTIVE'
  })
  if (fileNumber) {
    params.set('keyword', fileNumber)
  }
  const data = await apiGet(page, `/dcc/controlled-files/browser-page?${params.toString()}`)
  return Array.isArray(data?.list) ? data.list.length : 0
}

async function collectScenarioPreflight(page, scenario, submitterProfile) {
  const approvalUsers = parseApprovalUsersJson()
  const [categoryList, routes, approvalPositions, users] = await Promise.all([
    apiGet(page, '/dcc/file-categories'),
    apiGet(page, `/dcc/approval-routes?categoryId=${encodeURIComponent(scenario.categoryId)}`),
    apiGet(page, '/dcc/approval-positions'),
    apiGet(page, '/system/user/simple-list')
  ])
  const category = (categoryList || []).find((item) => Number(item.id) === Number(scenario.categoryId))
  const activeRoute = (routes || []).find((route) => route.active) || (routes || [])[0]
  const approvalCandidateResolution = resolveApprovalCandidatesForRoute(activeRoute, approvalPositions || [], users || [])
  let officialRoutePreview = []
  let officialRoutePreviewError = null
  let uploadSizePolicyEvidence = {
    checks: [],
    blockers: []
  }
  let distributionDepartmentEvidence = {
    required: false,
    label: config.distributionDepartmentLabel || null,
    matches: [],
    blockers: []
  }
  try {
    officialRoutePreview = await apiPostReadOnly(page, '/dcc/controlled-files/route-preview', {
      categoryId: scenario.categoryId
    })
  } catch (error) {
    officialRoutePreviewError = error.message
  }
  const normalizedOfficialRoutePreview = normalizeOfficialRoutePreview(officialRoutePreview)
  const officialApprovalCandidateResolution = normalizedOfficialRoutePreview.length > 0
    ? approvalCandidateResolutionFromOfficialRoutePreview(normalizedOfficialRoutePreview)
    : null
  const effectiveApprovalCandidateResolution = officialApprovalCandidateResolution || approvalCandidateResolution
  const approvalExecutionRoute = approvalRouteForExecution({
    activeRoute,
    officialRoutePreview: normalizedOfficialRoutePreview
  })
  let categoryPermissionRules = []
  let categoryPermissionRuleError = null
  try {
    categoryPermissionRules = await apiGet(page, `/dcc/file-categories/${scenario.categoryId}/permission-rules`)
  } catch (error) {
    categoryPermissionRuleError = error.message
  }
  const categoryUploadPermission = categoryUploadPermissionEvidence(
    categoryPermissionRules,
    submitterProfile,
    categoryPermissionRuleError
  )
  const categoryObsoletePermission = categoryActionPermissionEvidence(
    categoryPermissionRules,
    submitterProfile,
    'OBSOLETE',
    categoryPermissionRuleError
  )
  const activeCount = await browserActiveCount(page, scenario).catch(() => null)
  const scenarioBlockers = []
  if (!category) {
    scenarioBlockers.push(`DCC ${scenario.contentType} category not found: categoryId=${scenario.categoryId}`)
  } else if (category.active === false) {
    scenarioBlockers.push(`DCC ${scenario.contentType} category is inactive: categoryId=${scenario.categoryId}`)
  }
  if (!activeRoute) {
    scenarioBlockers.push(`DCC 审核矩阵 route missing for ${scenario.contentType}: categoryId=${scenario.categoryId}`)
  }
  if (officialRoutePreviewError) {
    scenarioBlockers.push(`DCC official route preview failed for ${scenario.contentType}: ${officialRoutePreviewError}`)
    if (officialRoutePreviewError.includes('Current user cannot access this controlled file')) {
      scenarioBlockers.push(
        `DCC submitter lacks category UPLOAD permission for ${scenario.contentType}: categoryId=${scenario.categoryId}, uploadRuleCount=${categoryUploadPermission.uploadRuleCount}, directUploadRuleMatchCount=${categoryUploadPermission.directUploadRuleMatchCount}`
      )
    }
  } else if (!normalizedOfficialRoutePreview.length) {
    scenarioBlockers.push(`DCC official route preview returned no nodes for ${scenario.contentType}: categoryId=${scenario.categoryId}`)
  } else {
    for (const row of normalizedOfficialRoutePreview) {
      if (row.resolvedUserIds.length === 0) {
        scenarioBlockers.push(`DCC official route preview resolves no approver for ${scenario.contentType} stage ${row.stageNo} ${row.stageName || ''}`.trim())
      }
    }
  }
  if (!preflightOnly && !config.allowWrites) {
    scenarioBlockers.push('DCC_CONTROLLED_CONTENT_E2E_ALLOW_WRITES=1 is required for DCC write matrix full-flow')
  }
  if (!preflightOnly && scenarioRequiresApprovalUsers(scenario) && !config.approvalUsersJson) {
    scenarioBlockers.push('DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON is required for DCC write matrix approval')
  }
  if (!preflightOnly && scenarioRequiresApprovalUsers(scenario) && config.approvalUsersJson && !approvalUsers) {
    scenarioBlockers.push('DCC_CONTROLLED_CONTENT_E2E_APPROVAL_USERS_JSON must parse before DCC write matrix approval')
  }
  if (!preflightOnly && scenarioRequiresApprovalUsers(scenario) && approvalUsers && approvalExecutionRoute) {
    scenarioBlockers.push(...validateApprovalUsersForRoute(scenario, approvalExecutionRoute, approvalUsers, effectiveApprovalCandidateResolution))
  }
  if (!preflightOnly && scenario.fullFlowPlan === 'obsolete' && categoryObsoletePermission.directObsoleteRuleMatchCount === 0) {
    scenarioBlockers.push(
      `DCC submitter lacks category OBSOLETE permission for ${scenario.contentType}: categoryId=${scenario.categoryId}, ` +
      `obsoleteRuleCount=${categoryObsoletePermission.obsoleteRuleCount}, ` +
      `directObsoleteRuleMatchCount=${categoryObsoletePermission.directObsoleteRuleMatchCount}`
    )
  }
  if (!preflightOnly) {
    scenarioBlockers.push(...filePrerequisiteBlockers(scenario))
    uploadSizePolicyEvidence = await collectUploadSizePolicyEvidence(page, scenario)
    scenarioBlockers.push(...uploadSizePolicyEvidence.blockers)
    distributionDepartmentEvidence = await collectDistributionDepartmentEvidence(page, scenario)
    scenarioBlockers.push(...distributionDepartmentEvidence.blockers)
  }
  if (!['release', 'pending-readonly', 'withdraw-resubmit', 'obsolete'].includes(scenario.fullFlowPlan)) {
    scenarioBlockers.push(`DCC ${scenario.contentType} has unsupported full-flow plan: ${scenario.fullFlowPlan}`)
  }

  return {
    key: scenario.key,
    contentType: scenario.contentType,
    categoryId: scenario.categoryId,
    categoryName: category?.name || scenario.categoryName,
    categoryFound: Boolean(category),
    routeId: activeRoute?.id,
    activeRoute: activeRoute
      ? {
          id: activeRoute.id,
          versionNo: activeRoute.versionNo,
          nodes: (activeRoute.nodes || []).map((node) => ({
            stageNo: node.stageNo,
            stageCode: node.stageCode,
            stageName: node.stageName,
            candidateSourceType: node.candidateSourceType,
            candidateSourceId: node.candidateSourceId,
            candidateSourceIds: node.candidateSourceIds,
            approveMethod: node.approveMethod
          }))
        }
      : null,
    routePreview: routePreview(activeRoute, effectiveApprovalCandidateResolution),
    officialRoutePreview: normalizedOfficialRoutePreview,
    officialRoutePreviewError,
    categoryUploadPermission,
    categoryObsoletePermission,
    approvalCandidateResolution: {
      blockers: approvalCandidateResolution.blockers,
      nodes: approvalCandidateResolution.nodes.map((node) => ({
        index: node.index,
        stageNo: node.stageNo,
        stageName: node.stageName,
        candidateSourceType: node.candidateSourceType,
        candidateSourceIds: node.candidateSourceIds,
        activeAssignmentCount: node.activeAssignmentCount,
        resolvedCandidateUserIds: node.resolvedCandidateUserIds,
        resolvedCandidateUsers: node.resolvedCandidateUsers.map((user) => ({
          id: Number(user.id),
          username: user.username,
          nickname: user.nickname,
          label: userLabel(user)
        }))
      }))
    },
    activeCount,
    openCandidateCount: null,
    uploadSizePolicyEvidence,
    distributionDepartmentEvidence,
    blockers: uniqueMessages(scenarioBlockers)
  }
}

function finalAssertionsFor(scenario, preflight, status) {
  if (preflight.fullFlow?.finalAssertions) {
    return preflight.fullFlow.finalAssertions
  }
  if (status === 'PASS') {
    return scenario.expectedAssertions
  }
  return Object.fromEntries(Object.keys(scenario.expectedAssertions).map((key) => [key, false]))
}

function buildScenarioArtifact(scenario, preflight) {
  const status = preflight.blockers.length === 0 && !preflightOnly ? 'PASS' : 'BLOCKED'
  return {
    scenario: `dcc-controlled-content-${scenario.key}`,
    domain: 'DCC_CONTROLLED_FILE',
    contentType: scenario.contentType,
    documentType: scenario.contentType,
    status,
    tenant: config.tenant,
    username: config.username,
    tenantId: evidence.tenantId,
    executionMode: 'playwright-ui',
    writeChannel: 'frontend-ui',
    directApiWrites: 0,
    sqlBusinessDataWritePerformed: false,
    mockDataUsed: false,
    userPath: evidence.steps,
    writeRequests: scenarioWriteRequests(preflight),
    finalAssertions: finalAssertionsFor(scenario, preflight, status),
    blockers: uniqueMessages(preflight.blockers),
    evidence: {
      ...preflight,
      blockers: uniqueMessages(preflight.blockers),
      writeRequests: scenarioWriteRequests(preflight)
    }
  }
}

async function main() {
  if (config.tenant !== '测试租户') {
    addBlocker(`DCC matrix E2E must use 测试租户, actual=${config.tenant}`)
  }
  if (config.username !== 'aoteman') {
    addBlocker(`DCC matrix E2E must use aoteman as submitter, actual=${config.username}`)
  }
  for (const key of missingSelectedScenarioKeys) {
    addBlocker(`DCC matrix selected scenario not found: ${key}`)
  }

  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const page = await (await browser.newContext({ viewport: { width: 1440, height: 960 } })).newPage()
    page.setDefaultTimeout(config.timeout)
    await login(page)
    page.on('request', trackUiWriteRequest)
    const submitterProfile = currentUserPermissionProfile(await apiGet(page, '/system/user/profile/get'))
    evidence.submitterProfile = submitterProfile
    for (const scenario of selectedScenarioMatrix) {
      const scenarioWriteStartIndex = evidence.writeRequests.length
      const preflight = await collectScenarioPreflight(page, scenario, submitterProfile)
      preflight.scenarioWriteStartIndex = scenarioWriteStartIndex
      if (!preflightOnly && preflight.blockers.length === 0) {
        try {
          preflight.fullFlow = await runScenarioFullFlow(browser, scenario, preflight)
        } catch (error) {
          preflight.blockers = uniqueMessages([...preflight.blockers, error.message])
        }
      }
      preflight.scenarioWriteEndIndex = evidence.writeRequests.length
      evidence.scenarios[scenario.key] = preflight
      if (!preflightOnly) {
        writeArtifact(scenario.artifactName, buildScenarioArtifact(scenario, preflight))
      }
    }
    if (preflightOnly && evidence.writeRequests.length > 0) {
      addBlocker(`DCC matrix preflight must be readonly, writeRequests=${JSON.stringify(evidence.writeRequests)}`)
    }
  } catch (error) {
    addBlocker(error.message)
    if (!preflightOnly) {
      for (const scenario of selectedScenarioMatrix) {
        const preflight = evidence.scenarios[scenario.key] || {
          key: scenario.key,
          contentType: scenario.contentType,
          categoryId: scenario.categoryId,
          categoryName: scenario.categoryName,
          routePreview: [],
          activeCount: null,
          openCandidateCount: null,
          blockers: [error.message]
        }
        writeArtifact(scenario.artifactName, buildScenarioArtifact(scenario, preflight))
      }
    }
  } finally {
    await browser.close()
  }

  const scenarioBlockers = uniqueMessages(Object.values(evidence.scenarios).flatMap((scenario) => scenario.blockers || []))
  const result = {
    scenario: 'dcc-controlled-content-matrix-real-flow',
    mode: preflightOnly ? 'preflight-only' : 'full-flow',
    status: blockers.length === 0 && scenarioBlockers.length === 0 ? (preflightOnly ? 'READY' : 'PASS') : 'BLOCKED',
    blockers: uniqueMessages([...blockers, ...scenarioBlockers]),
    evidence
  }
  console.log(JSON.stringify(result, null, 2))
  if (!['READY', 'PASS'].includes(result.status)) process.exit(2)
}

main().catch((error) => {
  console.error(error.stack || String(error))
  process.exit(1)
})
