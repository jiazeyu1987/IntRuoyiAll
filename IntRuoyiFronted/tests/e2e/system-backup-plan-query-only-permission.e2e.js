const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')

function requireSuccessPayload(payload, label) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `${label} failed: ${payload?.msg || payload?.code}`)
  return payload.data
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
  throw new Error(`Missing visible login field: ${label}`)
}

async function loginAndReadPermissions(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const permissionResponseResultPromise = permissionResponsePromise.then(
    (response) => ({ response }),
    (error) => ({ error })
  )
  await form.getByRole('button', { name: /^登录$/ }).click()

  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  requireSuccessPayload(await loginResponse.json(), 'login')
  const permissionResponseResult = await permissionResponseResultPromise
  if (permissionResponseResult.error) {
    throw permissionResponseResult.error
  }
  const permissionResponse = permissionResponseResult.response
  assert.equal(permissionResponse.ok(), true, `permission HTTP ${permissionResponse.status()}`)
  const permissionInfo = requireSuccessPayload(await permissionResponse.json(), 'permission')
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  return permissionInfo
}

function assertQueryOnlyPermissions(permissionInfo) {
  const permissions = new Set(Array.isArray(permissionInfo?.permissions) ? permissionInfo.permissions : [])
  assert.equal(permissions.has('*:*:*'), false, 'query-only account must not have wildcard permission')
  assert.equal(permissions.has('system:backup-plan:query'), true, 'query-only account is missing query permission')
  assert.equal(permissions.has('system:backup-plan:update'), false, 'query-only account unexpectedly has update permission')
  assert.equal(permissions.has('system:backup-plan:execute'), false, 'query-only account unexpectedly has execute permission')
}

async function assertWriteEndpointsForbidden(context, config, statusResponse) {
  const sourceHeaders = await statusResponse.request().allHeaders()
  const headers = {
    authorization: sourceHeaders.authorization,
    'tenant-id': sourceHeaders['tenant-id'],
    'content-type': 'application/json'
  }
  assert.ok(headers.authorization, 'missing authenticated Authorization header for final permission comparison')
  assert.ok(headers['tenant-id'], 'missing tenant-id header for final permission comparison')

  const checks = [
    {
      label: 'save schedule',
      method: 'PUT',
      path: '/admin-api/infra/backup-plan/schedule',
      data: { frequency: 'DAILY', time: '01:30', weekday: 'MON' }
    },
    { label: 'enable plan', method: 'POST', path: '/admin-api/infra/backup-plan/enable' },
    { label: 'disable plan', method: 'POST', path: '/admin-api/infra/backup-plan/disable' },
    { label: 'backup now', method: 'POST', path: '/admin-api/infra/backup-plan/backup-now' }
  ]
  for (const check of checks) {
    const response = await context.request.fetch(`${config.baseUrl}${check.path}`, {
      method: check.method,
      headers,
      data: check.data,
      timeout: config.timeout
    })
    assert.equal(
      response.status(),
      200,
      `${check.label} permission envelope expected HTTP 200, got ${response.status()}`
    )
    const payload = await response.json()
    assert.equal(payload?.code, 403, `${check.label} expected business code 403, got ${payload?.code}`)
  }
}

async function run() {
  const config = {
    baseUrl: process.env.SYSTEM_BACKUP_PLAN_QUERY_ONLY_BASE_URL || 'http://127.0.0.1:8081',
    timeout: Number(process.env.SYSTEM_BACKUP_PLAN_QUERY_ONLY_TIMEOUT || 120000),
    tenant: process.env.SYSTEM_BACKUP_PLAN_QUERY_ONLY_TENANT,
    username: process.env.SYSTEM_BACKUP_PLAN_QUERY_ONLY_USERNAME,
    password: process.env.SYSTEM_BACKUP_PLAN_QUERY_ONLY_PASSWORD
  }
  assert.ok(
    config.tenant && config.username && config.password,
    'Missing pre-approved query-only tenant, username, or password environment input'
  )

  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    const permissionInfo = await loginAndReadPermissions(page, config)
    assertQueryOnlyPermissions(permissionInfo)

    const statusResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/infra/backup-plan/status') &&
        response.request().method() === 'GET',
      { timeout: config.timeout }
    )
    const historyResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/infra/backup-plan/history/page') &&
        response.request().method() === 'GET',
      { timeout: config.timeout }
    )
    await page.goto(`${config.baseUrl}/system/backup-plan`, {
      waitUntil: 'domcontentloaded',
      timeout: config.timeout
    })
    const [statusResponse, historyResponse] = await Promise.all([
      statusResponsePromise,
      historyResponsePromise
    ])
    const statusData = requireSuccessPayload(await statusResponse.json(), 'backup status')
    requireSuccessPayload(await historyResponse.json(), 'backup history')

    const pageRoot = page.locator('.backup-plan-page')
    await pageRoot.getByText('当前账号只有查询权限', { exact: true }).waitFor({
      state: 'visible',
      timeout: config.timeout
    })
    await pageRoot.getByText('备份包历史', { exact: true }).waitFor({
      state: 'visible',
      timeout: config.timeout
    })
    assert.equal(await pageRoot.locator('.backup-plan-form').count(), 0, 'query-only page must not render editable plan form')
    assert.equal(
      await pageRoot.getByRole('button', { name: '现在备份一次', exact: true }).count(),
      0,
      'query-only page must not render backup-now button'
    )
    assert.ok(statusData?.healthStatus, 'query-only status response must expose healthStatus')
    await page.waitForFunction(
      () => !document.querySelector('.backup-plan-page .el-loading-mask'),
      undefined,
      { timeout: config.timeout }
    )

    const screenshotDir = path.join(repoRoot, 'output', 'playwright')
    fs.mkdirSync(screenshotDir, { recursive: true })
    await page.screenshot({
      path: path.join(screenshotDir, 'system-backup-plan-query-only.png'),
      fullPage: true
    })

    await assertWriteEndpointsForbidden(context, config, statusResponse)
    console.log(
      `PASS: system backup plan query-only E2E baseUrl=${config.baseUrl} tenant=${config.tenant} username=${config.username}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
