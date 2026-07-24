const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const args = new Set(process.argv.slice(2))
const preflightOnly = args.has('--preflight-only')
const repoRoot = path.resolve(__dirname, '../..')
const projectRoot = path.resolve(repoRoot, '..')
const releaseArtifactName = 'controlled-content-mes-route-version-full-flow-real.json'

const allowedApprovalModes = new Set(['BPM_REQUIRED', 'DIRECT'])
function normalizeApprovalMode(mode) {
  if (mode === 'BPM') return 'BPM_REQUIRED'
  return mode
}

const config = {
  baseUrl: (process.env.ROUTE_VERSION_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.ROUTE_VERSION_E2E_TENANT || '测试租户',
  username: process.env.ROUTE_VERSION_E2E_USERNAME || 'aoteman',
  password: process.env.ROUTE_VERSION_E2E_PASSWORD || '111111',
  submitter: {
    username: process.env.ROUTE_VERSION_E2E_USERNAME || 'aoteman',
    password: process.env.ROUTE_VERSION_E2E_PASSWORD || '111111'
  },
  approver: {
    username: process.env.ROUTE_VERSION_E2E_APPROVER_USERNAME,
    password: process.env.ROUTE_VERSION_E2E_APPROVER_PASSWORD,
    userId: process.env.ROUTE_VERSION_E2E_APPROVER_USER_ID,
    signaturePassword:
      process.env.ROUTE_VERSION_E2E_APPROVER_SIGNATURE_PASSWORD ||
      process.env.ROUTE_VERSION_E2E_APPROVER_PASSWORD
  },
  tenantId: process.env.ROUTE_VERSION_E2E_TENANT_ID || '122',
  approvalMode: normalizeApprovalMode(process.env.ROUTE_VERSION_E2E_APPROVAL_MODE),
  approvalProcessKey: process.env.ROUTE_VERSION_E2E_APPROVAL_PROCESS_KEY,
  approverUsername: process.env.ROUTE_VERSION_E2E_APPROVER_USERNAME,
  safeRouteId: process.env.ROUTE_VERSION_E2E_SAFE_ROUTE_ID,
  allowWrites: process.env.ROUTE_VERSION_E2E_ALLOW_WRITES === '1',
  headed: process.env.ROUTE_VERSION_E2E_HEADED === '1',
  timeout: Number(process.env.ROUTE_VERSION_E2E_TIMEOUT || '90000'),
  artifactDir: path.resolve(
    process.env.CONTROLLED_CONTENT_E2E_ARTIFACT_DIR ||
      process.env.ROUTE_VERSION_E2E_ARTIFACT_DIR ||
      path.join(projectRoot, 'doc/tasks/20260719-controlled-content-full-objective-completion-audit/e2e-artifacts')
  )
}

const requiredFiles = [
  'src/views/mes/pro/route/index.vue',
  'src/api/mes/pro/route/index.ts',
  'tests/e2e/mes-pro-route-version-workspace-static.spec.js',
  'tests/e2e/mes-pro-route-version-candidate-edit-static.spec.js'
]

const requiredPermissions = [
  'mes:pro-route:create',
  'mes:pro-route:delete',
  'mes:pro-route:version-query',
  'mes:pro-route:version-create',
  'mes:pro-route:version-submit',
  'mes:pro-route:version-withdraw',
  'mes:pro-route:version-cancel'
]
const allPermission = '*:*:*'
const openCandidateStatuses = new Set(['DRAFT', 'PENDING_APPROVAL', 'READY_TO_PUBLISH', 'FINALIZING', 'FINALIZATION_FAILED'])

const blockers = []
const evidence = {
  steps: [],
  writeRequests: []
}

function addBlocker(message) {
  blockers.push(message)
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

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function writeArtifact(name, payload) {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const filePath = path.join(config.artifactDir, name)
  fs.writeFileSync(filePath, `${JSON.stringify(payload, null, 2)}\n`, 'utf8')
  return filePath
}

async function waitFor(description, probe, timeout = config.timeout) {
  const startedAt = Date.now()
  let lastError
  while (Date.now() - startedAt < timeout) {
    try {
      const result = await probe()
      if (result) return result
    } catch (error) {
      lastError = error
    }
    await sleep(1000)
  }
  const detail = lastError ? `: ${lastError.message || String(lastError)}` : ''
  throw new Error(`timeout waiting for ${description}${detail}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

function buildRouteListUrl(query = {}) {
  const url = new URL('/mes/pro/route', config.baseUrl)
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && String(value) !== '') {
      url.searchParams.set(key, String(value))
    }
  }
  return url.toString()
}

function visibleDialog(page, title) {
  return page.locator('.el-dialog:visible').filter({ hasText: title }).last()
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

async function clickEnabledDialogAction(scope, actionName) {
  const button = scope
    .locator('button:visible:not(.is-disabled):not([disabled])')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(actionName)}\\s*$`) })
    .first()
  await button.waitFor({ state: 'visible', timeout: config.timeout })
  await button.click()
}

async function closeDialogIfVisible(page, dialog) {
  if (!(await dialog.isVisible().catch(() => false))) return
  const elementPlusClose = dialog.locator('.el-dialog__headerbtn').first()
  if (await elementPlusClose.isVisible().catch(() => false)) {
    await elementPlusClose.click({ timeout: 5000 })
  } else {
    const customClose = dialog.locator('.is-hover.cursor-pointer').last()
    if (await customClose.isVisible().catch(() => false)) {
      await customClose.click({ timeout: 5000 })
    } else {
      await page.keyboard.press('Escape')
    }
  }
  await dialog.waitFor({ state: 'hidden', timeout: config.timeout })
}

async function gotoRouteList(page, query = {}) {
  await page.goto(buildRouteListUrl(query), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.getByText('工艺流程', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  await page.locator('.el-table').first().waitFor({ state: 'visible', timeout: config.timeout })
}

async function waitForRouteCode(page, code) {
  await page.getByRole('button', { name: code, exact: true }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
}

async function clickRouteAction(page, actionName) {
  const button =
    actionName === '版本'
      ? page.locator('[data-testid="route-version-workspace"]:visible').first()
      : page.locator('button:visible').filter({ hasText: actionName }).first()
  await button.waitFor({ state: 'visible', timeout: config.timeout })
  await button.click()
}

function versionRow(workspace, versionNo) {
  return workspace.locator('.el-table__body-wrapper tbody tr').filter({ hasText: versionNo }).first()
}

async function clickVersionRowAction(workspace, versionNo, actionName) {
  const row = versionRow(workspace, versionNo)
  await row.waitFor({ state: 'visible', timeout: config.timeout })
  const actionButton = row.getByRole('button', { name: actionName, exact: true }).first()
  await actionButton.waitFor({ state: 'visible', timeout: config.timeout })
  await actionButton.click()
}

function recordUserPath(step) {
  evidence.steps.push(step)
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

async function login(page, account = config.submitter, landingPath = '/mes/pro/route', landingText = '工艺流程') {
  if (!account?.username || !account?.password) {
    throw new Error('login account username and password are required')
  }
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', landingPath)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  if (
    (await form
      .locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]')
      .count()) > 0
  ) {
    throw new Error('Login captcha is enabled; real E2E cannot continue without manual input.')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(account.username)
  await form.locator('input[type="password"]').first().fill(account.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(loginPayload.code)) {
    throw new Error(`login failed: HTTP ${loginResponse.status()} ${loginPayload.msg || JSON.stringify(loginPayload)}`)
  }
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: config.timeout,
    waitUntil: 'commit'
  })
  await page.goto(new URL(landingPath, config.baseUrl).toString(), { waitUntil: 'domcontentloaded' })
  if (landingText) {
    await page.getByText(landingText, { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  }
}

async function switchUser(page, account, landingPath = '/mes/pro/route', landingText = '工艺流程') {
  await page.context().clearCookies()
  await page.evaluate(() => {
    window.localStorage.clear()
    window.sessionStorage.clear()
  }).catch(() => {})
  await login(page, account, landingPath, landingText)
}

async function apiGet(page, apiPath) {
  const accessToken = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'ACCESS_TOKEN')))
  if (!accessToken) throw new Error('missing ACCESS_TOKEN after login')
  const tenantId = normalizeCacheString(unwrapCacheValue(await readBrowserCache(page, 'tenantId'))) || config.tenantId
  const response = await page.evaluate(
    async ({ requestPath, headers }) => {
      const res = await fetch(`/admin-api${requestPath}`, { method: 'GET', credentials: 'omit', headers })
      return { status: res.status, body: await res.json() }
    },
    {
      requestPath: apiPath,
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId),
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
    }
  )
  if (response.status !== 200 || ![0, 200].includes(response.body.code)) {
    throw new Error(`api GET failed: ${apiPath} HTTP ${response.status} ${response.body.msg || JSON.stringify(response.body)}`)
  }
  evidence.tenantId = String(tenantId)
  return response.body.data
}

function extractCommonData(payload, source) {
  if (!payload || ![0, 200].includes(payload.code)) {
    throw new Error(`${source} failed: ${payload?.msg || JSON.stringify(payload)}`)
  }
  return payload.data
}

async function verifyPermissions(page) {
  const cachedUser = unwrapCacheValue(await readBrowserCache(page, 'user'))
  const userInfo = typeof cachedUser === 'string' ? JSON.parse(cachedUser) : cachedUser
  const permissions = new Set(userInfo?.permissions || [])
  const missing = requiredPermissions.filter(
    (permission) => !permissions.has(permission) && !permissions.has(allPermission)
  )
  if (missing.length > 0) {
    throw new Error(`missing route-version permissions: ${missing.join(', ')}`)
  }
  evidence.permissions = requiredPermissions
}

async function verifySafeRoute(page) {
  const route = await apiGet(page, `/mes/pro/route/get?id=${encodeURIComponent(config.safeRouteId)}`)
  if (!route || Number(route.id) !== Number(config.safeRouteId)) {
    throw new Error(`safe route not found: ${config.safeRouteId}`)
  }
  if (!route.activeRouteVersionId || !route.activeRouteVersionNo) {
    throw new Error(`safe route has no active version: routeId=${config.safeRouteId}`)
  }
  const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(config.safeRouteId)}`)
  const activeVersions = (versions || []).filter((version) => version.active === true)
  const openCandidates = (versions || []).filter(
    (version) => !version.active && openCandidateStatuses.has(version.lifecycleStatus)
  )
  if (activeVersions.length !== 1) {
    throw new Error(`safe route must have exactly one active version, actual=${activeVersions.length}`)
  }
  if (openCandidates.length > 0) {
    throw new Error(`safe route must not have open candidates, actual=${openCandidates.length}`)
  }
  if (Number(activeVersions[0].id) !== Number(route.activeRouteVersionId)) {
    throw new Error('route activeRouteVersionId does not match version list active id')
  }
  evidence.safeRoute = {
    routeId: route.id,
    routeCode: route.code,
    routeName: route.name,
    activeRouteVersionId: route.activeRouteVersionId,
    activeRouteVersionNo: route.activeRouteVersionNo,
    openCandidateCount: openCandidates.length
  }
}

async function waitForRouteByCode(page, code) {
  return waitFor(`route copy ${code}`, async () => {
    const pageData = await apiGet(
      page,
      `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(code)}`
    )
    const matched = (pageData?.list || []).find((route) => route.code === code)
    return matched || undefined
  })
}

async function waitForRouteDeleted(page, code) {
  await waitFor(`route deletion ${code}`, async () => {
    const pageData = await apiGet(
      page,
      `/mes/pro/route/page?pageNo=1&pageSize=10&code=${encodeURIComponent(code)}`
    )
    return !(pageData?.list || []).some((route) => route.code === code)
  })
}

async function waitForDraftCandidate(page, routeId, previousActiveVersionId) {
  return waitFor(`draft candidate for route ${routeId}`, async () => {
    const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(routeId)}`)
    return (versions || []).find(
      (version) =>
        !version.active &&
        version.lifecycleStatus === 'DRAFT' &&
        Number(version.sourceRouteVersionId) === Number(previousActiveVersionId)
    )
  })
}

async function waitForDraftCandidateByCreatedResponse(page, routeId, previousActiveVersionId, createdCandidate) {
  return waitFor(`draft candidate for route ${routeId}`, async () => {
    const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(routeId)}`)
    return (versions || []).find((version) => {
      const sameCreatedId = createdCandidate?.id && Number(version.id) === Number(createdCandidate.id)
      const sameSource = Number(version.sourceRouteVersionId) === Number(previousActiveVersionId)
      return !version.active && version.lifecycleStatus === 'DRAFT' && (sameCreatedId || sameSource)
    })
  })
}

async function waitForVersionStatus(page, routeId, versionId, status) {
  return waitFor(`route version ${versionId} status ${status}`, async () => {
    const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(routeId)}`)
    return (versions || []).find(
      (version) => Number(version.id) === Number(versionId) && version.lifecycleStatus === status
    )
  })
}

async function openApprovalCenterBpmTodo(page) {
  const url = new URL('/approval-center/todo', config.baseUrl)
  url.searchParams.set('moduleCode', 'BPM')
  const tasksResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/page') &&
      response.url().includes('moduleCode=BPM') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  await page.goto(url.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: config.timeout })
  const tasksResponse = await tasksResponsePromise
  const pageData = extractCommonData(await tasksResponse.json(), 'approval-center BPM todo page')
  await settle(page)
  return pageData
}

function requireConfiguredApprover() {
  const missing = []
  if (!config.approver.username) missing.push('ROUTE_VERSION_E2E_APPROVER_USERNAME')
  if (!config.approver.password) missing.push('ROUTE_VERSION_E2E_APPROVER_PASSWORD')
  if (!config.approver.userId) missing.push('ROUTE_VERSION_E2E_APPROVER_USER_ID')
  if (missing.length > 0) {
    throw new Error(`${missing.join(', ')} are required for BPM approval user switch`)
  }
}

async function approveRouteVersionCandidateThroughApprovalCenter(page, candidate) {
  const processInstanceId = candidate?.approvalProcessInstanceId
  if (!processInstanceId) {
    throw new Error(`submitted route version is missing approvalProcessInstanceId: ${JSON.stringify(candidate)}`)
  }
  requireConfiguredApprover()
  await switchUser(page, config.approver, '/approval-center/todo?moduleCode=BPM', '审批中心')
  const targetProcessInstanceId = String(processInstanceId)
  const matched = await waitFor(`approval center BPM todo ${targetProcessInstanceId}`, async () => {
    const pageData = await openApprovalCenterBpmTodo(page)
    const list = Array.isArray(pageData?.list) ? pageData.list : []
    const rowIndex = list.findIndex(
      (item) =>
        String(item.processInstanceId || '') === targetProcessInstanceId ||
        String(item.businessKey || '') === targetProcessInstanceId
    )
    if (rowIndex < 0) return undefined
    return { rowIndex, task: list[rowIndex] }
  })
  evidence.approvalAssigneeUserId = matched.task.assigneeUserId
  evidence.approvalAssigneeUserName = matched.task.assigneeUserName
  evidence.approver = {
    username: config.approver.username,
    userId: config.approver.userId
  }
  if (Number(config.approver.userId) !== Number(matched.task.assigneeUserId)) {
    throw new Error(
      `configured approver must match actual approval assignee: configured=${config.approver.userId}, actual=${matched.task.assigneeUserId}`
    )
  }

  const rows = page.locator('.el-table__body-wrapper tbody tr')
  await rows.nth(matched.rowIndex).waitFor({ state: 'visible', timeout: config.timeout })
  await rows.nth(matched.rowIndex).getByRole('button', { name: '审核' }).click()
  const dialog = visibleDialog(page, '审核确认')
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  await dialog.locator('input[type="password"]').fill(config.approver.signaturePassword)
  const reviewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/approval-center/tasks/review') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await dialog.getByRole('button', { name: '确认审核', exact: true }).click()
  const reviewResponse = await reviewResponsePromise
  extractCommonData(await reviewResponse.json(), 'approval-center BPM review')
  await dialog.waitFor({ state: 'hidden', timeout: config.timeout })
  await settle(page)
  return matched.task
}

async function waitForPublishedActiveVersion(page, routeId, versionId) {
  return waitFor(`route version ${versionId} active`, async () => {
    const route = await apiGet(page, `/mes/pro/route/get?id=${encodeURIComponent(routeId)}`)
    const versions = await apiGet(page, `/mes/pro/route-version/list-by-route?routeId=${encodeURIComponent(routeId)}`)
    const activeVersions = (versions || []).filter((version) => version.active === true)
    if (
      Number(route.activeRouteVersionId) === Number(versionId) &&
      activeVersions.length === 1 &&
      Number(activeVersions[0].id) === Number(versionId)
    ) {
      return { route, activeVersions, versions }
    }
    return undefined
  })
}

async function copyRouteThroughUi(page, targetCode, targetName) {
  await gotoRouteList(page, { code: evidence.safeRoute.routeCode })
  await waitForRouteCode(page, evidence.safeRoute.routeCode)
  await clickRouteAction(page, '复制')

  const dialog = visibleDialog(page, '复制工艺路线')
  await dialog.waitFor({ state: 'visible', timeout: config.timeout })
  await dialog.locator('.el-form-item').filter({ hasText: '副本编码' }).locator('input').fill(targetCode)
  await dialog.locator('.el-form-item').filter({ hasText: '副本名称' }).locator('input').fill(targetName)
  const copyResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/mes/pro/route/copy') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await dialog.getByRole('button', { name: '确认复制', exact: true }).click()
  const copyResponse = await copyResponsePromise
  const copyBody = await copyResponse.json().catch(() => ({}))
  if (!copyResponse.ok() || ![0, 200].includes(copyBody.code)) {
    throw new Error(
      `copy route failed: HTTP ${copyResponse.status()} ${copyBody.msg || JSON.stringify(copyBody)}`
    )
  }
  await dialog.waitFor({ state: 'hidden', timeout: 10000 })

  await gotoRouteList(page, { code: targetCode })
  await waitForRouteCode(page, targetCode)
  return waitForRouteByCode(page, targetCode)
}

async function submitCandidateThroughWorkspace(page, workspace, route, candidate) {
  const submitResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/submit-publish') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await clickVersionRowAction(workspace, candidate.versionNo, '提交发布')
  const submitResponse = await submitResponsePromise
  const submittedCandidate = extractCommonData(await submitResponse.json(), 'route version submit-publish')
  const activeCandidate = await waitForVersionStatus(page, route.id, candidate.id, 'ACTIVE')
  if (Number(activeCandidate.id) !== Number(candidate.id)) {
    throw new Error('route version submit-publish must keep the same candidate version id')
  }
  const row = versionRow(workspace, activeCandidate.versionNo)
  await row.getByText('生效', { exact: false }).waitFor({ state: 'visible', timeout: config.timeout })
  if ((await row.getByRole('button', { name: '编辑', exact: true }).count()) > 0) {
    throw new Error('ACTIVE route version must not expose candidate edit action')
  }
  return { submittedCandidate, pendingCandidate: activeCandidate }
}

async function withdrawCandidateThroughWorkspace(page, workspace, route, candidate) {
  const withdrawResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/withdraw') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await clickVersionRowAction(workspace, candidate.versionNo, '撤回')
  const withdrawResponse = await withdrawResponsePromise
  extractCommonData(await withdrawResponse.json(), 'route version withdraw')
  const draftCandidate = await waitForVersionStatus(page, route.id, candidate.id, 'DRAFT')
  if (Number(draftCandidate.id) !== Number(candidate.id)) {
    throw new Error('route version withdraw must reopen the same candidate version id')
  }
  if (draftCandidate.approvalProcessInstanceId) {
    throw new Error('route version withdraw must clear approvalProcessInstanceId')
  }
  const row = versionRow(workspace, draftCandidate.versionNo)
  await row.getByText('草稿', { exact: false }).waitFor({ state: 'visible', timeout: config.timeout })
  await row.getByRole('button', { name: '编辑', exact: true }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await row.getByRole('button', { name: '提交发布', exact: true }).first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  return draftCandidate
}

async function verifyDraftCanEnterEditor(page, workspace, route, draftCandidate) {
  await clickVersionRowAction(workspace, draftCandidate.versionNo, '编辑')
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/route/edit/'), { timeout: config.timeout })
  await page.getByText('候选版本', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  await page.getByText('草稿', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  const editorUrl = page.url()
  if (!editorUrl.includes(`routeVersionId=${draftCandidate.id}`)) {
    throw new Error('draft editor URL must carry the same routeVersionId after withdraw')
  }
  await gotoRouteList(page, { code: route.code })
  return editorUrl
}

async function openRouteVersionWorkspace(page, route) {
  await gotoRouteList(page, { code: route.code })
  await waitForRouteCode(page, route.code)
  await clickRouteAction(page, '版本')

  const workspace = visibleDialog(page, '工艺路线版本')
  await workspace.waitFor({ state: 'visible', timeout: config.timeout })
  await workspace.getByText('当前 ACTIVE：', { exact: false }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  return workspace
}

async function createSubmitPublishCandidateThroughUi(page, copiedRoute) {
  const workspace = await openRouteVersionWorkspace(page, copiedRoute)

  const activeBefore = await apiGet(page, `/mes/pro/route/get?id=${encodeURIComponent(copiedRoute.id)}`)
  if (!activeBefore.activeRouteVersionId) {
    throw new Error(`copied route has no active version: ${copiedRoute.id}`)
  }

  const createCandidateResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route-version/create-candidate') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await workspace.getByRole('button', { name: '创建候选版本', exact: true }).click()
  const createCandidateResponse = await createCandidateResponsePromise
  const createdCandidate = extractCommonData(
    await createCandidateResponse.json(),
    'route version create candidate'
  )
  const draftCandidate = await waitForDraftCandidateByCreatedResponse(
    page,
    copiedRoute.id,
    activeBefore.activeRouteVersionId,
    createdCandidate
  )
  await workspace.getByText(draftCandidate.versionNo, { exact: true }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  recordUserPath('create-draft-candidate')

  let submitPublishResponsePromise
  let submitPublishResponse
  try {
    submitPublishResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/route-version/submit-publish') &&
        response.request().method() === 'POST',
      { timeout: config.timeout }
    )
    await clickVersionRowAction(workspace, draftCandidate.versionNo, '提交发布')
    submitPublishResponse = await submitPublishResponsePromise
  } catch (error) {
    if (submitPublishResponsePromise) {
      submitPublishResponsePromise.catch(() => null)
    }
    throw new Error(`route version submit-publish interaction failed: ${error.message || String(error)}`)
  }
  const policyPublishedCandidate = extractCommonData(
    await submitPublishResponse.json(),
    'route version submit-publish'
  )
  const submittedCandidate = policyPublishedCandidate
  const readyCandidate = policyPublishedCandidate
  recordUserPath('submit-publish-route-version')

  let approvalTask
  if (config.approvalMode === 'BPM_REQUIRED') {
    const pendingCandidate = await waitForVersionStatus(page, copiedRoute.id, draftCandidate.id, 'PENDING_APPROVAL')
    const approvalProcessInstanceId =
      policyPublishedCandidate?.approvalProcessInstanceId || pendingCandidate?.approvalProcessInstanceId
    if (!approvalProcessInstanceId) {
      throw new Error(`BPM_REQUIRED publish must return approval process instance: ${JSON.stringify(policyPublishedCandidate)}`)
    }
    await closeDialogIfVisible(page, workspace)
    approvalTask = await approveRouteVersionCandidateThroughApprovalCenter(page, {
      ...pendingCandidate,
      approvalProcessInstanceId
    })
    recordUserPath('approve-route-version-in-approval-center')
  } else {
    if (policyPublishedCandidate?.approvalProcessInstanceId) {
      throw new Error(`${config.approvalMode} publish must not create approval process instance: ${JSON.stringify(policyPublishedCandidate)}`)
    }
    await closeDialogIfVisible(page, workspace)
    recordUserPath('direct-route-version-publish')
  }
  const published = await waitForPublishedActiveVersion(page, copiedRoute.id, draftCandidate.id)
  recordUserPath('publish-approved-route-version')
  const previousActiveVersion = (published.versions || []).find(
    (version) => Number(version.id) === Number(activeBefore.activeRouteVersionId)
  )
  const newActiveVersion = (published.versions || []).find(
    (version) => Number(version.id) === Number(draftCandidate.id)
  )
  if (previousActiveVersion?.lifecycleStatus !== 'SUPERSEDED') {
    throw new Error(
      `previous active version must become SUPERSEDED, actual=${previousActiveVersion?.lifecycleStatus}`
    )
  }
  if (newActiveVersion?.lifecycleStatus !== 'ACTIVE') {
    throw new Error(`new route version must become ACTIVE, actual=${newActiveVersion?.lifecycleStatus}`)
  }

  return {
    activeBefore,
    draftCandidate,
    submittedCandidate,
    readyCandidate,
    policyPublishedCandidate,
    approvalTask,
    activeAfter: published.route,
    activeVersions: published.activeVersions,
    previousActiveVersion,
    newActiveVersion
  }
}

async function deleteRouteThroughUi(page, routeCode) {
  await gotoRouteList(page, { code: routeCode })
  await waitForRouteCode(page, routeCode)
  await clickRouteAction(page, '删除')
  const confirm = page.locator('.el-message-box:visible').last()
  await confirm.waitFor({ state: 'visible', timeout: config.timeout })
  await confirm.locator('button').filter({ hasText: '确定' }).last().click()
  await waitForRouteDeleted(page, routeCode)
}

async function runFullFlow(page) {
  if (!allowedApprovalModes.has(config.approvalMode)) {
    throw new Error('full-flow requires ROUTE_VERSION_E2E_APPROVAL_MODE=BPM_REQUIRED or DIRECT')
  }

  const timestamp = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)
  const targetCode = `E2E-RV-${timestamp}`
  const targetName = `路线版本E2E-${timestamp}`
  const copiedRoute = await copyRouteThroughUi(page, targetCode, targetName)
  let cleanupStatus = 'not-started'
  let publishEvidence

  try {
    publishEvidence = await createSubmitPublishCandidateThroughUi(page, copiedRoute)
    cleanupStatus = 'ready'
  } finally {
    try {
      await switchUser(page, config.submitter, '/mes/pro/route', '工艺流程')
      await deleteRouteThroughUi(page, targetCode)
      cleanupStatus = 'deleted'
    } catch (error) {
      cleanupStatus = `cleanup-blocked: ${error.message || String(error)}`
    }
  }

  evidence.fullFlow = {
    copiedRouteId: copiedRoute.id,
    copiedRouteCode: targetCode,
    copiedRouteName: targetName,
    previousActiveRouteVersionId: publishEvidence?.activeBefore?.activeRouteVersionId,
    candidateRouteVersionId: publishEvidence?.draftCandidate?.id,
    candidateRouteVersionNo: publishEvidence?.draftCandidate?.versionNo,
    readyRouteVersionStatus: publishEvidence?.readyCandidate?.lifecycleStatus,
    policyPublishStatus: publishEvidence?.policyPublishedCandidate?.lifecycleStatus,
    approvalProcessInstanceId: publishEvidence?.policyPublishedCandidate?.approvalProcessInstanceId,
    approvalTaskId: publishEvidence?.approvalTask?.sourceTaskId,
    approvalAssigneeUserId: evidence.approvalAssigneeUserId,
    approvalAssigneeUserName: evidence.approvalAssigneeUserName,
    activeRouteVersionIdAfterPublish: publishEvidence?.activeAfter?.activeRouteVersionId,
    previousActiveVersionStatusAfterPublish: publishEvidence?.previousActiveVersion?.lifecycleStatus,
    newActiveVersionStatusAfterPublish: publishEvidence?.newActiveVersion?.lifecycleStatus,
    activeVersionCountAfterPublish: publishEvidence?.activeVersions?.length,
    cleanupStatus
  }

  if (cleanupStatus !== 'deleted') {
    throw new Error(`full-flow cleanup did not finish for ${targetCode}: ${cleanupStatus}`)
  }
}

for (const relativePath of requiredFiles) {
  if (!fs.existsSync(path.join(repoRoot, relativePath))) {
    addBlocker(`missing required frontend artifact: ${relativePath}`)
  }
}

if (!allowedApprovalModes.has(config.approvalMode)) {
  addBlocker('ROUTE_VERSION_E2E_APPROVAL_MODE must be BPM_REQUIRED or DIRECT before route-version real E2E')
}

if (!preflightOnly && !config.allowWrites) {
  addBlocker('ROUTE_VERSION_E2E_ALLOW_WRITES=1 is required for task-owned write full-flow')
}

if (!preflightOnly && !allowedApprovalModes.has(config.approvalMode)) {
  addBlocker('full-flow requires ROUTE_VERSION_E2E_APPROVAL_MODE=BPM_REQUIRED or DIRECT')
}

if (!preflightOnly && config.approvalMode === 'BPM_REQUIRED') {
  const missingApproverEnv = []
  if (!config.approver.username) missingApproverEnv.push('ROUTE_VERSION_E2E_APPROVER_USERNAME')
  if (!config.approver.password) missingApproverEnv.push('ROUTE_VERSION_E2E_APPROVER_PASSWORD')
  if (!config.approver.userId) missingApproverEnv.push('ROUTE_VERSION_E2E_APPROVER_USER_ID')
  if (missingApproverEnv.length > 0) {
    addBlocker(`${missingApproverEnv.join(', ')} are required for BPM approval user switch`)
  }
}

if (!config.safeRouteId) {
  addBlocker('ROUTE_VERSION_E2E_SAFE_ROUTE_ID is required to avoid modifying shared route data')
}

function trackUiWriteRequest(request) {
  const method = request.method()
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return
  const requestUrl = request.url()
  if (!requestUrl.includes('/admin-api/')) return
  const parsedUrl = new URL(requestUrl)
  if (parsedUrl.pathname.includes('/admin-api/system/auth/login')) return
  const entry = {
    method,
    path: parsedUrl.pathname.replace('/admin-api', '') + parsedUrl.search
  }
  if (entry.path.includes('/mes/pro/route-version/submit-publish')) {
    const postData = request.postData()
    if (postData) {
      try {
        const body = JSON.parse(postData)
        entry.requestBodyKeys = Object.keys(body)
      } catch (error) {
        entry.requestBodyParseError = error.message
      }
    }
  }
  evidence.writeRequests.push(entry)
}

function buildReleaseArtifact(result) {
  const fullFlow = evidence.fullFlow || {}
  const approvalProcessExpected = config.approvalMode === 'BPM_REQUIRED'
  const finalAssertions = {
    createdCandidate: Boolean(fullFlow.candidateRouteVersionId),
    submitted: Boolean(fullFlow.policyPublishStatus || fullFlow.approvalProcessInstanceId),
    approvalProcessMatchesMode: approvalProcessExpected
      ? Boolean(fullFlow.approvalProcessInstanceId)
      : !fullFlow.approvalProcessInstanceId,
    published: Boolean(fullFlow.activeRouteVersionIdAfterPublish),
    oldActiveStatus: fullFlow.previousActiveVersionStatusAfterPublish,
    newActiveStatus: fullFlow.newActiveVersionStatusAfterPublish,
    activeVersionCount: fullFlow.activeVersionCountAfterPublish,
    cleanupStatus: fullFlow.cleanupStatus
  }

  return {
    scenario: 'mes-pro-route-version-real-flow',
    domain: 'MES_ROUTE',
    status: result.status,
    tenant: config.tenant,
    username: config.submitter.username,
    approverUsername: config.approver.username,
    tenantId: evidence.tenantId,
    approvalAssigneeUserId: fullFlow.approvalAssigneeUserId,
    approvalAssigneeUserName: fullFlow.approvalAssigneeUserName,
    executionMode: 'playwright-ui',
    writeChannel: 'frontend-ui',
    directApiWrites: 0,
    sqlBusinessDataWritePerformed: false,
    mockDataUsed: false,
    userPath: evidence.steps,
    writeRequests: evidence.writeRequests,
    finalAssertions,
    blockers: result.blockers,
    evidence
  }
}

async function main() {
  if (blockers.length === 0) {
    const browser = await chromium.launch({
      headless: !config.headed,
      args: ['--disable-dev-shm-usage']
    })
    try {
      const page = await (await browser.newContext({ viewport: { width: 1440, height: 960 } })).newPage()
      page.setDefaultTimeout(config.timeout)
      await login(page, config.submitter)
      page.on('request', trackUiWriteRequest)
      await verifyPermissions(page)
      await verifySafeRoute(page)
      evidence.approvalMode = config.approvalMode
      if (!preflightOnly) {
        await runFullFlow(page)
      }
    } catch (error) {
      addBlocker(error.message)
    } finally {
      await browser.close()
    }
  }

  const result = {
    scenario: 'mes-pro-route-version-real-flow',
    mode: preflightOnly ? 'preflight-only' : 'full-flow',
    status: blockers.length === 0 ? (preflightOnly ? 'READY' : 'PASS') : 'BLOCKED',
    blockers,
    evidence
  }
  if (!preflightOnly) {
    result.artifactPath = writeArtifact(releaseArtifactName, buildReleaseArtifact(result))
  }
  console.log(JSON.stringify(result, null, 2))
  if (!['READY', 'PASS'].includes(result.status)) process.exit(2)
}

main().catch((error) => {
  console.error(error.stack || String(error))
  process.exit(1)
})
