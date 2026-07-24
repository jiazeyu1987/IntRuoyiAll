const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

function requiredEnv(name) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`${name} is required for runtime-control all-buttons E2E`)
  }
  return value.trim().replace(/\/+$/, '')
}

function optionalEnv(name, defaultValue) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : defaultValue
}

const BASE_URL = requiredEnv('RUNTIME_CONTROL_E2E_BASE_URL')
const ACTION_ORIGIN = requiredEnv('RUNTIME_CONTROL_E2E_ACTION_ORIGIN')
const TEST_TENANT = optionalEnv('RUNTIME_CONTROL_E2E_TENANT', '测试租户')
const TEST_USERNAME = optionalEnv('RUNTIME_CONTROL_E2E_USERNAME', 'aoteman')
const TEST_PASSWORD = optionalEnv('RUNTIME_CONTROL_E2E_PASSWORD', 'admin123')
const VERIFY_TENANT = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_TENANT', '芋道源码')
const VERIFY_USERNAME = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_USERNAME', 'admin')
const VERIFY_PASSWORD = optionalEnv('RUNTIME_CONTROL_E2E_VERIFY_PASSWORD', 'admin123')
const ACTION_API_BASE = `${ACTION_ORIGIN}/admin-api`
const ARTIFACT_DIR = optionalEnv(
  'RUNTIME_CONTROL_E2E_ARTIFACT_DIR',
  path.resolve(__dirname, '../../doc/tasks/20260529-runtime-control-all-buttons-e2e/artifacts')
)

const results = []

function record(id, tenant, status, evidence) {
  const item = { id, tenant, status, evidence, at: new Date().toISOString() }
  results.push(item)
  console.log(`${status}: ${id} [${tenant}] ${evidence}`)
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

function unwrapResponse(payload) {
  if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
    return payload.data
  }
  return payload
}

function pageList(payload) {
  const data = unwrapResponse(payload)
  if (Array.isArray(data)) return data
  if (data && Array.isArray(data.list)) return data.list
  if (data && Array.isArray(data.records)) return data.records
  return []
}

function runtimeControlPath(url) {
  const basePath = '/admin-api/infra/runtime-control'
  const pathname = new URL(url).pathname
  if (!pathname.startsWith(basePath)) return ''
  return pathname.slice(basePath.length) || '/'
}

function collectRuntimeTraffic(page) {
  const responses = []
  const writeRequests = []
  page.on('request', (request) => {
    const url = request.url()
    if (!url.includes('/admin-api/infra/runtime-control/')) return
    if (request.method() !== 'GET') {
      writeRequests.push({
        method: request.method(),
        path: runtimeControlPath(url),
        postData: request.postData() || '',
        at: Date.now()
      })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (!url.includes('/admin-api/infra/runtime-control/')) return
    let body
    try {
      body = await response.json()
    } catch (error) {
      body = { parseError: error.message }
    }
    responses.push({
      status: response.status(),
      path: runtimeControlPath(url),
      url,
      body,
      data: unwrapResponse(body),
      at: Date.now()
    })
  })
  return { responses, writeRequests }
}

function findResponse(responses, matcher, minAt = 0) {
  for (let index = responses.length - 1; index >= 0; index -= 1) {
    const response = responses[index]
    if (response.at >= minAt && matcher(response)) return response
  }
  return undefined
}

async function waitForResponse(responses, matcher, label, minAt = 0, timeoutMs = 30000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    const response = findResponse(responses, matcher, minAt)
    if (response) return response
    await new Promise((resolve) => setTimeout(resolve, 150))
  }
  throw new Error(`Timed out waiting for ${label}`)
}

async function waitForPath(responses, path, minAt = 0, timeoutMs = 30000) {
  return waitForResponse(responses, (response) => response.path === path, path, minAt, timeoutMs)
}

async function waitForPathSuffix(responses, suffix, minAt = 0, timeoutMs = 30000) {
  return waitForResponse(
    responses,
    (response) => response.path.endsWith(suffix),
    `*${suffix}`,
    minAt,
    timeoutMs
  )
}

function assertOkResponse(response, label) {
  assert(response.status >= 200 && response.status < 300, `${label} HTTP ${response.status}`)
  if (typeof response.body?.code === 'number') {
    assert(response.body.code === 0, `${label} business code ${response.body.code}: ${response.body.msg || ''}`)
  }
}

function assertExplicitBlock(response, label) {
  assert(response.status >= 200 && response.status < 500, `${label} unexpected HTTP ${response.status}`)
  if (typeof response.body?.code === 'number') {
    assert(response.body.code !== 0, `${label} expected business block, got success`)
    assert(response.body.msg || response.body.message, `${label} blocked response must include message`)
    return
  }
  assert(response.status >= 400, `${label} expected HTTP or business block`)
}

async function fillFirstVisible(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input for ${locator}`)
}

async function fillFirstVisibleIfPresent(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) return false
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(tenantName)
  await page.keyboard.press('Enter')
  return true
}

async function loginRuntimeControl(page, credentials) {
  await page.goto(`${BASE_URL}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded'
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (page.url().includes('/login')) {
    const selected = await selectTenant(page, credentials.tenant)
    if (!selected) {
      await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), credentials.tenant)
    }
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), credentials.username)
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), credentials.password)
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }

  if (!page.url().includes('/infra/monitors/runtime-control')) {
    await page.goto(`${BASE_URL}/infra/monitors/runtime-control`, { waitUntil: 'domcontentloaded' })
  }
  await page.waitForSelector('text=运行控制台', { timeout: 30000 })
  await page.locator('text=探针状态').waitFor({ state: 'visible', timeout: 30000 })
}

async function requestRuntimeJson(pathname, options = {}) {
  const response = await fetch(`${ACTION_API_BASE}${pathname}`, options)
  let body
  try {
    body = await response.json()
  } catch (error) {
    throw new Error(`Invalid JSON from ${pathname}: HTTP ${response.status}, ${error.message}`)
  }
  assert(response.status >= 200 && response.status < 300, `${pathname} HTTP ${response.status}`)
  if (typeof body.code === 'number') {
    assert(body.code === 0, `${pathname} business code ${body.code}: ${body.msg || ''}`)
  }
  return body.data
}

async function loginForSetup() {
  const tenantId = await requestRuntimeJson(`/system/tenant/get-id-by-name?name=${encodeURIComponent(TEST_TENANT)}`)
  const token = await requestRuntimeJson('/system/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'tenant-id': String(tenantId)
    },
    body: JSON.stringify({
      username: TEST_USERNAME,
      password: TEST_PASSWORD,
      captchaVerification: ''
    })
  })
  assert(token?.accessToken, 'setup login must return access token')
  return {
    tenantId,
    userId: token.userId,
    headers: {
      Authorization: `Bearer ${token.accessToken}`,
      'tenant-id': String(tenantId),
      'Content-Type': 'application/json'
    }
  }
}

async function ensureTestTenantData() {
  const setup = await loginForSetup()
  const rows = await requestRuntimeJson('/infra/runtime-control/owner-matrix', { headers: setup.headers })
  const existingRows = Array.isArray(rows) ? rows : []
  const requiredRows = [
    ['local', 'storage-capacity-warning', 'capacity-owner'],
    ['prod', 'promote-prod', 'release-owner'],
    ['prod', 'backup-now', 'backup-owner'],
    ['prod', 'rollback-app', 'release-owner'],
    ['prod', 'restore-data', 'data-owner']
  ]
  let ownerCreated = 0
  for (const [environment, action, role] of requiredRows) {
    const exists = existingRows.some(
      (item) => item.environment === environment && item.action === action && item.role === role
    )
    if (exists) continue
    await requestRuntimeJson('/infra/runtime-control/owner-matrix', {
      method: 'POST',
      headers: setup.headers,
      body: JSON.stringify({
        environment,
        action,
        role,
        required: true,
        ownerUserId: setup.userId,
        ownerName: TEST_USERNAME,
        escalationPath: `E2E测试租户 ${action} ${role}`
      })
    })
    ownerCreated += 1
  }

  const alertTitle = `E2E运行控制台告警-${Date.now()}`
  await requestRuntimeJson('/infra/runtime-control/alerts', {
    method: 'POST',
    headers: setup.headers,
    body: JSON.stringify({
      environment: 'local',
      action: 'storage-capacity-warning',
      severity: 'WARN',
      title: alertTitle,
      content: 'E2E测试租户全按钮验证告警',
      notifyTemplateCode: 'RUNTIME_OPS_ALERT',
      templateParams: {
        environment: 'local',
        action: 'storage-capacity-warning',
        severity: 'WARN',
        title: alertTitle,
        content: 'E2E测试租户全按钮验证告警'
      }
    })
  })
  record('setup-test-data', TEST_TENANT, 'PASS', `ownerCreated=${ownerCreated}; alert=${alertTitle}`)
}

function card(page, title) {
  return page.locator('.ops-card').filter({ hasText: title }).first()
}

async function visibleDialog(page, title) {
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: title }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  return dialog
}

async function closeVisibleDialog(page) {
  const cancel = page.locator('.el-dialog:visible').getByRole('button', { name: '取消' }).last()
  if ((await cancel.count()) > 0 && (await cancel.isVisible())) {
    await cancel.click()
    return
  }
  await page.keyboard.press('Escape')
}

async function closeAllDialogs(page) {
  for (let index = 0; index < 6; index += 1) {
    const closed = await page.evaluate(() => {
      const dialogs = Array.from(document.querySelectorAll('.el-dialog'))
        .filter((dialog) => {
          const style = window.getComputedStyle(dialog)
          const rect = dialog.getBoundingClientRect()
          return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
        })
      if (!dialogs.length) return 0
      for (const dialog of dialogs.reverse()) {
        const button = dialog.querySelector('.el-dialog__headerbtn')
        if (button instanceof HTMLElement) {
          button.click()
        }
      }
      return dialogs.length
    })
    if (closed === 0) return
    await page.waitForTimeout(500)
  }
  assert((await page.locator('.el-dialog:visible').count()) === 0, 'visible dialogs remained after closeAllDialogs')
}

async function waitUntilEnabled(locator, label, timeoutMs = 90000) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    if ((await locator.count()) > 0 && (await locator.first().isVisible()) && (await locator.first().isEnabled())) {
      return locator.first()
    }
    await new Promise((resolve) => setTimeout(resolve, 250))
  }
  throw new Error(`${label} did not become enabled`)
}

async function assertNoNewWrites(writeRequests, startedAt, label, blockedPaths = ['/actions', '/restart']) {
  await new Promise((resolve) => setTimeout(resolve, 500))
  const writes = writeRequests.filter(
    (request) => request.at >= startedAt && blockedPaths.some((pathPart) => request.path.includes(pathPart))
  )
  assert(writes.length === 0, `${label} must not submit writes, got ${writes.map((item) => item.path).join(', ')}`)
}

async function coverToolbarRefresh(page, traffic, tenant) {
  const refreshButton = await waitUntilEnabled(
    page.locator('.runtime-toolbar').getByRole('button', { name: '刷新' }),
    'toolbar refresh'
  )
  const overviewPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/infra/runtime-control/overview'),
    { timeout: 70000 }
  )
  const operationsPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/infra/runtime-control/operations'),
    { timeout: 70000 }
  )
  await refreshButton.click()
  const [overviewResponse, operationsResponse] = await Promise.all([overviewPromise, operationsPromise])
  assert(overviewResponse.status() >= 200 && overviewResponse.status() < 300, 'toolbar refresh overview HTTP failed')
  assert(operationsResponse.status() >= 200 && operationsResponse.status() < 300, 'toolbar refresh operations HTTP failed')
  record('toolbar-refresh', tenant, 'PASS', 'overview and operations reloaded')
}

async function coverRestartButtons(page, traffic, tenant) {
  const buttons = page.locator('button:has-text("重启")')
  const count = await buttons.count()
  assert(count > 0, 'restart buttons must exist')
  let opened = 0
  for (let index = 0; index < count; index += 1) {
    const button = buttons.nth(index)
    if (!(await button.isVisible()) || !(await button.isEnabled())) continue
    const startedAt = Date.now()
    await button.click()
    const messageBox = page.locator('.el-message-box:visible').last()
    if ((await messageBox.count()) > 0 && (await messageBox.isVisible())) {
      await messageBox.getByRole('button', { name: '取消' }).click()
      await assertNoNewWrites(traffic.writeRequests, startedAt, `restart messagebox ${index}`, ['/restart'])
      opened += 1
      continue
    }
    const dialog = await visibleDialog(page, '确认重启')
    await dialog.getByRole('button', { name: '确认重启' }).click()
    await assertNoNewWrites(traffic.writeRequests, startedAt, `restart dialog ${index}`, ['/restart'])
    await dialog.getByRole('button', { name: '取消' }).click()
    opened += 1
  }
  assert(opened > 0, 'at least one enabled restart control must open its guard')
  record('status-matrix-restart', tenant, 'LOGICALLY_BLOCKED', `opened=${opened}; missing confirm/reason did not submit`)
}

async function openOperation(page, label) {
  await closeAllDialogs(page)
  await page.locator('.ops-toolbar').getByRole('button', { name: label }).click()
  return visibleDialog(page, label)
}

async function submitOperationExpectResponse(page, traffic, dialog, responseLabel) {
  const startedAt = Date.now()
  await dialog.getByRole('button', { name: '确认执行' }).click()
  return waitForPath(traffic.responses, '/actions', startedAt, 30000).then((response) => {
    if (response.body?.code === 0) {
      record(responseLabel, TEST_TENANT, 'PASS', `operation accepted; action=${response.data?.action || ''}`)
    } else {
      assertExplicitBlock(response, responseLabel)
      record(responseLabel, TEST_TENANT, 'LOGICALLY_BLOCKED', response.body?.msg || response.body?.message || 'blocked')
    }
    return response
  })
}

async function settleOperationUi(page) {
  const logDialog = page.locator('.runtime-log-dialog:visible')
  try {
    await logDialog.waitFor({ state: 'visible', timeout: 20000 })
    await closeAllDialogs(page)
  } catch (error) {
    const dialog = page.locator('.el-dialog:visible')
    if ((await dialog.count()) > 0) {
      await closeAllDialogs(page)
    }
  }
  await waitUntilEnabled(
    page.locator('.ops-toolbar').getByRole('button', { name: '构建发布包' }),
    'operation toolbar after submit',
    90000
  )
}

async function coverBuildRelease(page, traffic) {
  const dialog = await openOperation(page, '构建发布包')
  await dialog.getByText('带数据发布').click()
  await dialog.getByText('只发代码').click()
  await dialog.locator('textarea').fill('E2E invalid release tag guard')
  await dialog.getByPlaceholder(/NAS 发布包|发布包编号/).fill('bad/tag')
  const response = await submitOperationExpectResponse(page, traffic, dialog, 'operation-build-release')
  assertExplicitBlock(response, 'operation-build-release invalid tag')
  await closeVisibleDialog(page)
}

async function coverMissingReleaseAction(page, traffic, label, id, needsProd = false) {
  const dialog = await openOperation(page, label)
  await dialog.locator('textarea').fill(`E2E ${id} missing release guard`)
  const selector = dialog.locator('.el-select').first()
  await selector.waitFor({ state: 'visible', timeout: 10000 })
  if (needsProd) {
    await dialog.getByPlaceholder('输入 PROD').fill('PROD')
  }
  const confirmButton = dialog.getByRole('button', { name: '确认执行' })
  if (!(await confirmButton.isEnabled())) {
    const blockText = await dialog.locator('.operation-block-alert').innerText().catch(() => 'confirm disabled by guard')
    await closeVisibleDialog(page)
    record(id, TEST_TENANT, 'LOGICALLY_BLOCKED', blockText.replace(/\s+/g, ' ').trim())
    return
  }
  const startedAt = Date.now()
  await confirmButton.click()
  await assertNoNewWrites(traffic.writeRequests, startedAt, id, ['/actions'])
  const blockText = await dialog.locator('.operation-block-alert').innerText().catch(() => 'release package selector guard blocked before submit')
  await closeVisibleDialog(page)
  record(id, TEST_TENANT, 'LOGICALLY_BLOCKED', blockText.replace(/\s+/g, ' ').trim())
}

async function coverMarkReleaseTested(page, traffic) {
  const dialog = await openOperation(page, '标记测试通过')
  await dialog.locator('textarea').first().fill('E2E mark-release-tested current release guard')
  const currentReleaseText = await dialog
    .locator('.el-form-item')
    .filter({ hasText: '当前测试服发布包' })
    .locator('input')
    .inputValue()
  const responsePromise = waitForPath(traffic.responses, '/actions', Date.now(), 30000).catch(() => undefined)
  const startedAt = Date.now()
  await dialog.getByRole('button', { name: '确认执行' }).click()
  const response = await responsePromise
  if (currentReleaseText && currentReleaseText !== '无' && response) {
    if (response.body?.code === 0) {
      record('operation-mark-release-tested', TEST_TENANT, 'PASS', `currentRelease=${currentReleaseText}`)
    } else {
      assertExplicitBlock(response, 'operation-mark-release-tested')
      record(
        'operation-mark-release-tested',
        TEST_TENANT,
        'LOGICALLY_BLOCKED',
        response.body?.msg || response.body?.message || 'blocked'
      )
    }
    await settleOperationUi(page)
    return
  }
  await assertNoNewWrites(traffic.writeRequests, startedAt, 'operation-mark-release-tested', ['/actions'])
  await closeVisibleDialog(page)
  record('operation-mark-release-tested', TEST_TENANT, 'LOGICALLY_BLOCKED', 'no current test release')
}

async function coverProdGuardOnly(page, traffic, label, id) {
  const dialog = await openOperation(page, label)
  await dialog.locator('textarea').fill(`E2E ${id} prod guard`)
  const confirmButton = dialog.getByRole('button', { name: '确认执行' })
  if (!(await confirmButton.isEnabled())) {
    const blockText = await dialog.locator('.operation-block-alert').innerText().catch(() => 'confirm disabled by guard')
    await closeVisibleDialog(page)
    record(id, TEST_TENANT, 'LOGICALLY_BLOCKED', blockText.replace(/\s+/g, ' ').trim())
    return
  }
  const startedAt = Date.now()
  await confirmButton.click()
  await assertNoNewWrites(traffic.writeRequests, startedAt, id, ['/actions'])
  await closeVisibleDialog(page)
  record(id, TEST_TENANT, 'LOGICALLY_BLOCKED', 'missing PROD confirmation blocked before submit')
}

async function coverOperationButtons(page, traffic) {
  await coverBuildRelease(page, traffic)
  await coverMissingReleaseAction(page, traffic, '部署发布包到测试服', 'operation-publish-test')
  await coverMarkReleaseTested(page, traffic)
  await coverMissingReleaseAction(page, traffic, '上线已验证发布包', 'operation-promote-prod')
  await coverProdGuardOnly(page, traffic, '立即备份', 'operation-backup-now')
  await coverProdGuardOnly(page, traffic, '回滚版本', 'operation-rollback-app')
  await coverProdGuardOnly(page, traffic, '恢复数据', 'operation-restore-data')
}

async function coverLogButtons(page, traffic, tenant) {
  const operationsStartedAt = Date.now()
  const refreshButton = await waitUntilEnabled(
    page.locator('.runtime-toolbar').getByRole('button', { name: '刷新' }),
    'toolbar refresh before log verification',
    90000
  )
  await refreshButton.click()
  const operationsResponse = await waitForPath(traffic.responses, '/operations', operationsStartedAt)
  assertOkResponse(operationsResponse, 'operations for log')
  const logButton = page.locator('.operation-panel button:has-text("查看日志")').first()
  await logButton.waitFor({ state: 'visible', timeout: 10000 })
  await logButton.click()
  const logDialog = page.locator('.runtime-log-dialog:visible')
  await logDialog.waitFor({ state: 'visible', timeout: 10000 })
  await logDialog.getByRole('button', { name: '刷新' }).click()
  await waitForPathSuffix(traffic.responses, '/log', Date.now() - 1000)
  await page.keyboard.press('Escape')
  record('operation-log-view-refresh', tenant, 'PASS', 'log dialog opened and refreshed')
}

async function coverOpsCards(page, traffic) {
  let startedAt = Date.now()
  await card(page, '探针状态').getByRole('button', { name: '执行探针' }).click()
  await waitForPath(traffic.responses, '/probes/run', startedAt, 70000)
  record('probe-run', TEST_TENANT, 'PASS', 'real probes executed')
}

async function coverIncidentButtons(page, traffic) {
  const title = `E2E全按钮事故-${Date.now()}`
  await page.getByRole('button', { name: '事故闭环' }).click()
  const drawer = page.locator('.ops-incident-drawer')
  await drawer.waitFor({ state: 'visible', timeout: 10000 })
  record('incident-open', TEST_TENANT, 'PASS', 'incident drawer opened')

  let startedAt = Date.now()
  await drawer.getByRole('button', { name: '刷新' }).click()
  await waitForPath(traffic.responses, '/incidents/page', startedAt)
  record('incident-refresh', TEST_TENANT, 'PASS', 'incident page reloaded')

  const createForm = drawer.locator('.incident-form').filter({ hasText: '新建事故' }).last()
  await createForm.locator('.el-form-item').filter({ hasText: '标题' }).locator('input').fill(title)
  await createForm.locator('.el-form-item').filter({ hasText: '描述' }).locator('textarea').fill('测试租户全按钮真实 E2E')
  startedAt = Date.now()
  await createForm.getByRole('button', { name: '新建事故' }).click()
  await waitForPath(traffic.responses, '/incidents', startedAt)
  await drawer.locator(`text=${title}`).first().waitFor({ state: 'visible', timeout: 15000 })
  record('incident-create', TEST_TENANT, 'PASS', `incident=${title}`)

  await drawer.locator(`text=${title}`).first().click()
  const actionForm = drawer.locator('.incident-detail .incident-form').first()
  await actionForm.locator('.el-form-item').filter({ hasText: '处置动作' }).locator('input').fill('E2E处置')
  await actionForm.locator('.el-form-item').filter({ hasText: '证据' }).locator('textarea').fill('真实页面按钮记录')
  startedAt = Date.now()
  await actionForm.getByRole('button', { name: '记录处置' }).click()
  await waitForPathSuffix(traffic.responses, '/actions', startedAt)
  record('incident-record-action', TEST_TENANT, 'PASS', 'incident action recorded')

  await drawer.locator(`text=${title}`).first().waitFor({ state: 'visible', timeout: 15000 })
  await drawer.locator(`text=${title}`).first().click()
  const closeForm = drawer.locator('.incident-form--gate').first()
  await closeForm.locator('.el-form-item').filter({ hasText: '剩余风险' }).locator('textarea').fill('无剩余风险')
  await closeForm.locator('.el-form-item').filter({ hasText: '关闭原因' }).locator('textarea').fill('E2E全按钮验证关闭')
  startedAt = Date.now()
  await closeForm.getByRole('button', { name: '关闭事故' }).click()
  await waitForPathSuffix(traffic.responses, '/close', startedAt)
  record('incident-close', TEST_TENANT, 'PASS', 'incident closed')
  await page.keyboard.press('Escape')
}

async function runTestTenantFlow(page, traffic) {
  await loginRuntimeControl(page, {
    tenant: TEST_TENANT,
    username: TEST_USERNAME,
    password: TEST_PASSWORD
  })
  await coverToolbarRefresh(page, traffic, TEST_TENANT)
  await coverRestartButtons(page, traffic, TEST_TENANT)
  await coverOperationButtons(page, traffic)
  await coverLogButtons(page, traffic, TEST_TENANT)
  await coverOpsCards(page, traffic)
  await coverIncidentButtons(page, traffic)
}

async function runAdminVerification(page, traffic) {
  await loginRuntimeControl(page, {
    tenant: VERIFY_TENANT,
    username: VERIFY_USERNAME,
    password: VERIFY_PASSWORD
  })
  await coverToolbarRefresh(page, traffic, VERIFY_TENANT)
  await coverRestartButtons(page, traffic, VERIFY_TENANT)
  for (const label of [
    '构建发布包',
    '部署发布包到测试服',
    '标记测试通过',
    '上线已验证发布包',
    '立即备份',
    '回滚版本',
    '恢复数据'
  ]) {
    const startedAt = Date.now()
    const dialog = await openOperation(page, label)
    await assertNoNewWrites(traffic.writeRequests, startedAt, `admin ${label}`, ['/actions'])
    await dialog.getByRole('button', { name: '取消' }).click()
    record(`admin-open-${label}`, VERIFY_TENANT, 'PASS', 'dialog opened and canceled without write')
  }
  await coverLogButtons(page, traffic, VERIFY_TENANT)
  await page.getByRole('button', { name: '事故闭环' }).click()
  await page.locator('.ops-incident-drawer').waitFor({ state: 'visible', timeout: 10000 })
  const incidentStartedAt = Date.now()
  await page.locator('.ops-incident-drawer').getByRole('button', { name: '刷新' }).click()
  await waitForPath(traffic.responses, '/incidents/page', incidentStartedAt)
  await page.keyboard.press('Escape')
  record('admin-incident-open-refresh', VERIFY_TENANT, 'PASS', 'incident drawer opened, refreshed, and closed without write')
}

async function writeArtifacts() {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(
    path.join(ARTIFACT_DIR, 'runtime-control-all-buttons-results.json'),
    JSON.stringify({ results }, null, 2),
    'utf8'
  )
}

async function main() {
  await ensureTestTenantData()
  const browser = await chromium.launch({ headless: true })
  try {
    const testContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const testPage = await testContext.newPage()
    const testTraffic = collectRuntimeTraffic(testPage)
    await runTestTenantFlow(testPage, testTraffic)
    await testContext.close()

    const adminContext = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const adminPage = await adminContext.newPage()
    const adminTraffic = collectRuntimeTraffic(adminPage)
    await runAdminVerification(adminPage, adminTraffic)
    await adminContext.close()

    await writeArtifacts()
    console.log(`PASS: runtime-control all-buttons E2E covered ${results.length} checks`)
  } catch (error) {
    await writeArtifacts().catch(() => undefined)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
