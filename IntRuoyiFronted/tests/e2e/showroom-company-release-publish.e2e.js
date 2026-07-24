const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for showroom company release publish E2E. Run in a workspace where the 'playwright' package is installed."
    )
  }
}

const config = {
  baseUrl: (process.env.SHOWROOM_COMPANY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.SHOWROOM_COMPANY_E2E_TENANT || '测试租户',
  username: process.env.SHOWROOM_COMPANY_E2E_USERNAME || 'aoteman',
  password: process.env.SHOWROOM_COMPANY_E2E_PASSWORD || 'admin123',
  headed: process.env.SHOWROOM_COMPANY_E2E_HEADED === '1'
}

const EXPECTED_SCOPE = {
  siteKey: 'yingtai-showroom',
  stage: 'TEST'
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
  throw new Error(`No visible input found for ${label}`)
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function waitForWorkbenchReady(page) {
  const title = page.locator('.showroom-company-workbench__title').filter({ hasText: '公司信息' }).first()
  await title.waitFor({ state: 'visible', timeout: 30000 })

  const editButton = page.getByRole('button', { name: '编辑公司' })
  await editButton.waitFor({ state: 'visible', timeout: 30000 })

  const publishButton = page.getByRole('button', { name: '手动发布展厅' })
  await publishButton.waitFor({ state: 'visible', timeout: 30000 })

  const bodyText = await page.locator('body').innerText({ timeout: 10000 })
  assert.ok(!bodyText.includes('公司工作台尚未加载完成'), 'company workbench must finish loading before publish')
  assert.ok(!bodyText.includes('Access Denied'), 'company workbench must not land on access denied state')
  assert.ok(!bodyText.includes('权限不足'), 'company workbench must not land on permission denied state')
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/showroom/company`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
    if ((await tenantInput.count()) > 0) {
      await tenantInput.click()
      await tenantInput.fill(config.tenant)
      await tenantInput.press('Enter')
    } else {
      await fillFirstVisible(
        loginForm.locator('input[placeholder="请输入租户名称"]'),
        config.tenant,
        'tenant'
      )
    }
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
    await Promise.all([
      page.waitForResponse(
        (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
        { timeout: 60000 }
      ),
      loginForm.locator('.el-button--primary').first().click()
    ])
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}/showroom/company`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
}

function parseJsonBody(request) {
  const body = request.postData()
  assert.ok(body, 'publish request must include a JSON body')
  return JSON.parse(body)
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()

  let pageErrors = []
  page.on('pageerror', (error) => {
    pageErrors.push(error.message)
  })

  let sawCompanyCurrentRequest = false
  let sawCompanyCurrentSuccess = false
  page.on('response', async (response) => {
    if (
      response.url().includes('/admin-api/showroom/company/current') &&
      response.request().method() === 'GET'
    ) {
      sawCompanyCurrentRequest = true
      if (response.ok()) {
        sawCompanyCurrentSuccess = true
      }
    }
  })

  let capturedPublishRequest = null
  await page.route('**/admin-api/showroom/release/publish', async (route) => {
    const request = route.request()
    capturedPublishRequest = {
      method: request.method(),
      url: request.url(),
      payload: parseJsonBody(request)
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 0,
        data: {
          releaseId: 'e2e-release-verify',
          manifestHash: 'e2e-manifest',
          rootDocumentId: 'e2e-root',
          documentCount: 1,
          assetCount: 1,
          installBytes: 1,
          publishedAt: '2026-05-29T00:00:00Z'
        }
      })
    })
  })

  try {
    await login(page)
    assert.equal(new URL(page.url()).pathname, '/showroom/company')
    await waitForWorkbenchReady(page)

    const publishButton = page.getByRole('button', { name: '手动发布展厅' })
    await publishButton.click()

    const confirmButton = page.getByRole('button', { name: '确定' }).or(
      page.getByRole('button', { name: '确认' })
    )
    await confirmButton.waitFor({ state: 'visible', timeout: 10000 })
    await confirmButton.click()
    await page.waitForTimeout(800)

    assert.ok(capturedPublishRequest, 'manual publish confirm must emit the showroom publish request')
    assert.equal(capturedPublishRequest.method, 'POST')
    assert.equal(
      new URL(capturedPublishRequest.url).pathname,
      '/admin-api/showroom/release/publish'
    )
    assert.equal(
      Object.prototype.hasOwnProperty.call(capturedPublishRequest.payload, 'siteKey'),
      true,
      'publish payload must carry siteKey explicitly instead of relying on implicit scope'
    )
    assert.equal(
      Object.prototype.hasOwnProperty.call(capturedPublishRequest.payload, 'stage'),
      true,
      'publish payload must carry stage explicitly instead of relying on implicit scope'
    )
    assert.equal(capturedPublishRequest.payload.siteKey, EXPECTED_SCOPE.siteKey)
    assert.equal(capturedPublishRequest.payload.stage, EXPECTED_SCOPE.stage)

    await page.waitForTimeout(1000)
    const releaseErrorPanel = page.locator('.showroom-company-workbench__error-panel').first()
    assert.equal(
      await releaseErrorPanel.isVisible().catch(() => false),
      false,
      'manual publish should not surface a release error panel when the intercepted backend returns success'
    )

    assert.deepEqual(pageErrors, [], `page raised runtime errors: ${pageErrors.join('; ')}`)
    console.log(
      `PASS: showroom company manual publish emits explicit scope ${capturedPublishRequest.payload.siteKey}/${capturedPublishRequest.payload.stage}; companyCurrentSeen=${sawCompanyCurrentRequest}; companyCurrentOk=${sawCompanyCurrentSuccess}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
