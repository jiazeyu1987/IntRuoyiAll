const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const config = {
  baseUrl: (process.env.PROFILE_ERP_SYNC_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.PROFILE_ERP_SYNC_E2E_TENANT || '芋道源码',
  username: process.env.PROFILE_ERP_SYNC_E2E_USERNAME || 'admin',
  password: process.env.PROFILE_ERP_SYNC_E2E_PASSWORD,
  headed: process.env.PROFILE_ERP_SYNC_E2E_HEADED === '1',
  artifactDir: path.resolve(
    process.env.PROFILE_ERP_SYNC_E2E_ARTIFACT_DIR ||
      path.join(frontendRoot, 'output', 'playwright', 'profile-erp-table-auto-sync-replenishment-real')
  )
}

assert.ok(config.password, 'PROFILE_ERP_SYNC_E2E_PASSWORD is required')

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/user/profile?tab=config')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.status(), 200, 'login http status')
  assert.ok([0, 200].includes(loginPayload.code), `login failed: ${loginPayload.msg || loginPayload.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function waitForJson(responsePromise, label) {
  const response = await responsePromise
  let payload
  try {
    payload = await response.json()
  } catch {
    payload = { raw: await response.text().catch(() => '') }
  }
  assert.equal(response.status(), 200, `${label} http status: ${JSON.stringify(payload).slice(0, 500)}`)
  assert.ok([0, 200].includes(payload.code), `${label} business code: ${JSON.stringify(payload).slice(0, 1000)}`)
  return payload
}

function collectEvents(page) {
  const events = {
    profileWrites: [],
    consoleErrors: [],
    requestFailures: [],
    pageErrors: []
  }
  page.on('console', (message) => {
    if (message.type() === 'error') {
      events.consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => events.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    events.requestFailures.push({
      method: request.method(),
      url: request.url(),
      failure: request.failure()?.errorText || ''
    })
  })
  page.on('request', (request) => {
    const method = request.method()
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      return
    }
    const url = request.url()
    if (url.includes('/admin-api/erp/kingdee-sync') || url.includes('/admin-api/infra/job')) {
      events.profileWrites.push({ method, url })
    }
  })
  return events
}

async function openProfileErpSyncTab(page) {
  const activeConnectionPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/erp/kingdee-config/active-connection') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )

  await page.goto(`${config.baseUrl}/user/profile?tab=config`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  await page.getByRole('tab', { name: '配置', exact: true }).click()
  await page.getByRole('tab', { name: 'ERP表格自动同步', exact: true }).click()
  await waitForJson(activeConnectionPromise, 'active Kingdee connection')

  const syncTable = page.locator('.profile-erp-table-sync__select-table')
  await syncTable.waitFor({ state: 'visible', timeout: 60000 })
  await syncTable.getByText('生产补料单列表', { exact: true }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })

  for (const rowLabel of ['生产领料单列表', '生产补料单列表', '生产用料清单']) {
    await syncTable.getByText(rowLabel, { exact: true }).first().waitFor({ state: 'visible', timeout: 60000 })
  }

  await page.screenshot({
    path: path.join(config.artifactDir, 'profile-erp-table-auto-sync-replenishment-row.png'),
    fullPage: true
  })
}

async function main() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({
      viewport: { width: 1600, height: 900 },
      locale: 'zh-CN'
    })
    const page = await context.newPage()
    const events = collectEvents(page)

    await login(page)
    await openProfileErpSyncTab(page)

    assert.deepEqual(events.pageErrors, [], `page errors: ${JSON.stringify(events.pageErrors)}`)
    assert.deepEqual(events.profileWrites, [], `profile visibility check must not write: ${JSON.stringify(events.profileWrites)}`)

    const report = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      path: '/user/profile?tab=config',
      verifiedText: '生产补料单列表',
      screenshot: path.join(config.artifactDir, 'profile-erp-table-auto-sync-replenishment-row.png'),
      observedNonTargetResourceFailures: events.requestFailures,
      observedConsoleErrors: events.consoleErrors
    }

    fs.writeFileSync(
      path.join(config.artifactDir, 'profile-erp-table-auto-sync-replenishment-real-report.json'),
      `${JSON.stringify(report, null, 2)}\n`,
      'utf8'
    )

    await context.close()
    console.log(JSON.stringify(report, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
