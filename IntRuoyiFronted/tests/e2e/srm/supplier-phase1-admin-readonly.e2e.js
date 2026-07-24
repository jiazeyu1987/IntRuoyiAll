const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-phase1')
fs.mkdirSync(outputDir, { recursive: true })

const config = {
  baseUrl: process.env.SRM_PHASE1_ADMIN_BASE_URL || 'http://127.0.0.1:8118',
  tenant: process.env.SRM_PHASE1_ADMIN_TENANT || '芋道源码',
  username: process.env.SRM_PHASE1_ADMIN_USERNAME || 'admin',
  password: process.env.SRM_PHASE1_ADMIN_PASSWORD || 'admin123',
  sampleSupplierName:
    process.env.SRM_PHASE1_ADMIN_SAMPLE_SUPPLIER_NAME || 'SRM Portal E2E 20260620183546'
}

function assertSuccess(payload, action) {
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `${action} failed: ${JSON.stringify(payload)}`)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/srm/supplier/access`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form
    .locator(
      'input[placeholder="请输入租户名称"], input[placeholder="租户名称"], .el-select input[role="combobox"], input.el-select__input'
    )
    .first()
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponsePromise

  const textInputs = form.locator('input.el-input__inner')
  await textInputs.nth(0).fill('')
  await textInputs.nth(0).fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json()
  assertSuccess(loginPayload, 'admin login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)

  const permissionResponse = await permissionPromise
  const permissionPayload = await permissionResponse.json()
  assertSuccess(permissionPayload, 'admin permission')

  return {
    permissionPayload,
    authorization:
      permissionResponse.request().headers()['authorization'] ||
      permissionResponse.request().headers()['Authorization'] ||
      '',
    tenantId:
      permissionResponse.request().headers()['tenant-id'] ||
      permissionResponse.request().headers()['Tenant-Id'] ||
      ''
  }
}

async function fetchJson(page, relativeUrl, authContext) {
  const payload = await page.evaluate(
    async ({ url, authHeader, tenantHeader }) => {
      const response = await fetch(url, {
        method: 'GET',
        credentials: 'include',
        headers: {
          Accept: 'application/json, text/plain, */*',
          Authorization: authHeader,
          'tenant-id': tenantHeader
        }
      })
      return {
        status: response.status,
        text: await response.text()
      }
    },
    {
      url: `${config.baseUrl}${relativeUrl}`,
      authHeader: authContext.authorization,
      tenantHeader: authContext.tenantId
    }
  )

  assert.equal(payload.status, 200, `GET ${relativeUrl} should return HTTP 200`)
  return JSON.parse(payload.text)
}

async function main() {
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()

    page.on('request', (request) => {
      const url = request.url()
      if (!url.includes('/admin-api/srm/')) {
        return
      }
      const method = request.method().toUpperCase()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        writeRequests.push({ method, url })
      }
    })

    const authContext = await login(page)
    assert.equal(authContext.tenantId, '1', 'readonly verification must use tenant-id=1')

    const permissions = authContext.permissionPayload?.data?.permissions || []
    assert.ok(
      permissions.includes('srm:supplier-access:query'),
      'admin readonly verification should include srm:supplier-access:query permission'
    )

    await page.goto(`${config.baseUrl}/srm/supplier/access`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page, 30000)
    await page.getByText('供应商准入').first().waitFor({ state: 'visible', timeout: 30000 })

    const accessPagePayload = await fetchJson(
      page,
      `/admin-api/srm/supplier-access/page?pageNo=1&pageSize=20&supplierName=${encodeURIComponent(config.sampleSupplierName)}`,
      authContext
    )
    assertSuccess(accessPagePayload, 'query admin readonly supplier access page')
    assert.equal(accessPagePayload.data?.total || 0, 0, 'test-tenant sample must stay invisible to admin tenant')

    await page.screenshot({
      path: path.join(outputDir, 'supplier-phase1-admin-readonly.png'),
      fullPage: true
    })

    assert.deepEqual(writeRequests, [], 'readonly verification must not issue SRM write requests')

    const summary = {
      ok: true,
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      tenantId: authContext.tenantId,
      sampleSupplierName: config.sampleSupplierName,
      invisibleCount: accessPagePayload.data?.total || 0,
      writeRequests,
      screenshot: path.join(outputDir, 'supplier-phase1-admin-readonly.png')
    }
    fs.writeFileSync(
      path.join(outputDir, 'supplier-phase1-admin-readonly-summary.json'),
      `${JSON.stringify(summary, null, 2)}\n`,
      'utf8'
    )
    console.log(JSON.stringify(summary, null, 2))
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
