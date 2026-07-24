const assert = require('node:assert/strict')

const {
  fillDialogReason,
  fillProdConfirm,
  getRuntimeControlActionOrigin,
  openOperationDialog,
  runRuntimeControlE2E
} = require('./runtime-control-ops-e2e-helper')

const ACTION_ORIGIN = getRuntimeControlActionOrigin()
const PROMOTE_SCOPE = process.env.RUNTIME_CONTROL_REAL_PROMOTE_SCOPE || 'code-only'
const PROMOTE_REASON =
  process.env.RUNTIME_CONTROL_REAL_PROMOTE_REASON ||
  `E2E real ${PROMOTE_SCOPE} promote-prod ${new Date().toISOString()}`
const MAX_WAIT_MS = Number(process.env.RUNTIME_CONTROL_REAL_PROMOTE_TIMEOUT_MS || 2 * 60 * 60 * 1000)
const POLL_MS = Number(process.env.RUNTIME_CONTROL_REAL_PROMOTE_POLL_MS || 15 * 1000)
const APPROVAL_TOKEN = 'ALLOW_PROD_RUNTIME_PROMOTE_WRITE'
const APPROVAL = process.env.RUNTIME_CONTROL_REAL_PROMOTE_PROD_APPROVAL || ''
const PROD_BACKEND_HEALTH_URL_REQUIRED = 'RUNTIME_CONTROL_PROD_BACKEND_HEALTH_URL is required'
const PROD_FRONTEND_URL_REQUIRED = 'RUNTIME_CONTROL_PROD_FRONTEND_URL is required'
const PROD_WEBSITE_URL_REQUIRED = 'RUNTIME_CONTROL_PROD_WEBSITE_URL is required'
const PROD_SHOWROOM_URL_REQUIRED = 'RUNTIME_CONTROL_PROD_SHOWROOM_URL is required'
const PROD_LOGIN_URL_REQUIRED = 'RUNTIME_CONTROL_PROD_LOGIN_URL is required'
const PROD_EXPECTED_BACKEND_ORIGIN_REQUIRED =
  'RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN is required'
const PROD_FORBIDDEN_TEST_BACKEND_ORIGIN_REQUIRED =
  'RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN is required'
const PROD_BACKEND_HEALTH_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_PROD_BACKEND_HEALTH_URL',
  process.env.RUNTIME_CONTROL_PROD_BACKEND_HEALTH_URL,
  PROD_BACKEND_HEALTH_URL_REQUIRED
)
const PROD_FRONTEND_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_PROD_FRONTEND_URL',
  process.env.RUNTIME_CONTROL_PROD_FRONTEND_URL,
  PROD_FRONTEND_URL_REQUIRED
)
const PROD_WEBSITE_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_PROD_WEBSITE_URL',
  process.env.RUNTIME_CONTROL_PROD_WEBSITE_URL,
  PROD_WEBSITE_URL_REQUIRED
)
const PROD_SHOWROOM_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_PROD_SHOWROOM_URL',
  process.env.RUNTIME_CONTROL_PROD_SHOWROOM_URL,
  PROD_SHOWROOM_URL_REQUIRED
)
const PROD_LOGIN_URL = requireExplicitUrl(
  'RUNTIME_CONTROL_PROD_LOGIN_URL',
  process.env.RUNTIME_CONTROL_PROD_LOGIN_URL,
  PROD_LOGIN_URL_REQUIRED
)
const PROD_EXPECTED_BACKEND_ORIGIN = requireExplicitOrigin(
  'RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN',
  process.env.RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN,
  PROD_EXPECTED_BACKEND_ORIGIN_REQUIRED
)
const PROD_FORBIDDEN_TEST_BACKEND_ORIGIN = requireExplicitOrigin(
  'RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN',
  process.env.RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN,
  PROD_FORBIDDEN_TEST_BACKEND_ORIGIN_REQUIRED
)

if (PROD_EXPECTED_BACKEND_ORIGIN === PROD_FORBIDDEN_TEST_BACKEND_ORIGIN) {
  throw new Error(
    'RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN must differ from ' +
      'RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN'
  )
}

function requireExplicitUrl(name, value, missingMessage) {
  if (!value) {
    throw new Error(missingMessage)
  }
  try {
    return new URL(value).href
  } catch (error) {
    throw new Error(`${name} must be a valid absolute URL: ${value}`)
  }
}

function requireExplicitOrigin(name, value, missingMessage) {
  if (!value) {
    throw new Error(missingMessage)
  }
  try {
    return new URL(value).origin
  } catch (error) {
    throw new Error(`${name} must be a valid absolute URL origin: ${value}`)
  }
}

function requireExplicitApproval() {
  if (!['code-only', 'with-data'].includes(PROMOTE_SCOPE)) {
    throw new Error(`Invalid RUNTIME_CONTROL_REAL_PROMOTE_SCOPE: ${PROMOTE_SCOPE}`)
  }
  if (process.env.RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD !== '1') {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1 to run the real promote-prod E2E. ' +
        'This test submits a real promotion to the production server.'
    )
  }
  if (APPROVAL !== APPROVAL_TOKEN) {
    throw new Error(
      `Set RUNTIME_CONTROL_REAL_PROMOTE_PROD_APPROVAL=${APPROVAL_TOKEN} only after explicit user approval.`
    )
  }
  if (
    PROMOTE_SCOPE === 'with-data' &&
    process.env.RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA !== '1'
  ) {
    throw new Error(
      'Set RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA=1 to run a real with-data promotion. ' +
        'This overwrites the production MySQL database and MinIO yudao bucket from the test server.'
    )
  }
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function tail(value, maxLength = 1600) {
  if (!value) return ''
  return value.length > maxLength ? value.slice(value.length - maxLength) : value
}

async function fetchOk(url) {
  const response = await fetch(url)
  const body = await response.text()
  assert.ok(
    response.status >= 200 && response.status < 400,
    `${url} should return 2xx/3xx, got HTTP ${response.status}: ${tail(body, 500)}`
  )
  console.log(`HEALTH_OK ${url} HTTP ${response.status}`)
  return body
}

async function assertPublishScopeDefault(dialog) {
  const codeOnly = dialog.locator('.el-radio-button').filter({ hasText: '只发代码' }).first()
  await codeOnly.waitFor({ state: 'visible', timeout: 10000 })
  const checked = await codeOnly.locator('input[type="radio"]').evaluate((input) => input.checked)
  assert.equal(checked, true, 'promote-prod should default to code-only')
}

async function selectPublishScope(dialog) {
  await assertPublishScopeDefault(dialog)
  if (PROMOTE_SCOPE === 'code-only') return

  const withData = dialog.locator('.el-radio-button').filter({ hasText: '带数据发布' }).first()
  await withData.waitFor({ state: 'visible', timeout: 10000 })
  await withData.click()
  const checked = await withData.locator('input[type="radio"]').evaluate((input) => input.checked)
  assert.equal(checked, true, 'with-data radio should be selected before submit')
  await dialog.locator('text=带数据发布会覆盖目标环境数据库和文件对象').waitFor({
    state: 'visible',
    timeout: 10000
  })
}

async function waitForPromotionCompletion(page, operationId, latestLogRef) {
  const deadline = Date.now() + MAX_WAIT_MS
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: operationId }).last()
  await dialog.waitFor({ state: 'visible', timeout: 90000 })

  while (Date.now() < deadline) {
    if (latestLogRef.error) {
      throw latestLogRef.error
    }

    const latestLog = latestLogRef.value
    if (latestLog?.status === 'succeeded') {
      const content = latestLog.content || ''
      assert.match(content, /Promotion completed\./)
      if (PROMOTE_SCOPE === 'code-only') {
        assert.match(content, /Skipping database sync for code-only promotion/)
        assert.match(content, /Skipping MinIO sync for code-only promotion/)
      } else {
        assert.doesNotMatch(content, /-SkipDatabaseSync|-SkipMinioSync/)
        assert.match(content, /Dumping tested MySQL database/)
        assert.match(content, /Importing tested database/)
        assert.match(content, /Mirroring MinIO bucket yudao/)
        assert.match(content, /mc mirror --overwrite/)
      }
      assert.match(content, /Production frontend:/)
      assert.match(content, /Production backend health:/)
      console.log(`PROMOTE_SUCCEEDED operationId=${operationId}`)
      console.log(tail(content))
      return latestLog
    }
    if (latestLog?.status === 'failed') {
      throw new Error(`Promote operation failed: ${tail(latestLog.content || latestLog.status)}`)
    }

    const status = latestLog?.status || 'waiting-log'
    const length = latestLog?.length ?? 0
    console.log(`PROMOTE_WAIT operationId=${operationId} status=${status} logBytes=${length}`)

    const refresh = dialog.getByRole('button', { name: '刷新' }).first()
    if ((await refresh.count()) > 0) {
      await refresh.click()
    }
    await delay(POLL_MS)
  }

  throw new Error(`Promote operation did not complete within ${MAX_WAIT_MS}ms`)
}

async function verifyProductionHealth() {
  await fetchOk(PROD_BACKEND_HEALTH_URL)
  await fetchOk(PROD_FRONTEND_URL)
  await fetchOk(PROD_WEBSITE_URL)
  await fetchOk(PROD_SHOWROOM_URL)
}

async function verifyProductionWebsiteBrowser() {
  const { chromium } = require('playwright')
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } })
  const page = await context.newPage()
  const pageErrors = []
  const failedCriticalRequests = []

  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    const type = request.resourceType()
    if (['document', 'script', 'stylesheet', 'xhr', 'fetch'].includes(type)) {
      failedCriticalRequests.push(`${type} ${request.url()} ${request.failure()?.errorText || ''}`)
    }
  })

  try {
    for (const url of [PROD_WEBSITE_URL, PROD_SHOWROOM_URL]) {
      const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
      assert.ok(response, `${url} should return a document response`)
      assert.ok(response.status() >= 200 && response.status() < 400, `${url} returned ${response.status()}`)
      await page.locator('body').waitFor({ state: 'visible', timeout: 30000 })
      await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
      const bodyHtmlLength = (await page.locator('body').evaluate((body) => body.innerHTML.length))
      assert.ok(bodyHtmlLength > 100, `${url} should render non-empty Website content`)
      console.log(`WEBSITE_BROWSER_OK ${url} bodyHtmlLength=${bodyHtmlLength}`)
    }
    assert.deepEqual(pageErrors, [], `Website should not throw page errors: ${JSON.stringify(pageErrors)}`)
    assert.deepEqual(
      failedCriticalRequests,
      [],
      `Website should not have critical request failures: ${JSON.stringify(failedCriticalRequests)}`
    )
  } finally {
    await browser.close()
  }
}

async function verifyProductionLogin() {
  const { chromium } = require('playwright')
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } })
  const page = await context.newPage()
  const loginRequests = []

  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/admin-api/system/auth/login')) {
      loginRequests.push(request.url())
    }
  })

  try {
    await page.goto(PROD_LOGIN_URL, { waitUntil: 'domcontentloaded' })
    const tenant = page.locator('input[placeholder="请输入租户名称"]')
    if ((await tenant.count()) > 0 && (await tenant.first().isVisible())) {
      await tenant.first().fill('芋道源码')
    }
    await page.locator('input[placeholder="请输入用户名"]').first().fill('admin')
    await page.locator('input[placeholder="请输入密码"]').first().fill('admin123')
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
    assert.ok(
      loginRequests.some((url) =>
        url.startsWith(`${PROD_EXPECTED_BACKEND_ORIGIN}/admin-api/system/auth/login`)
      ),
      `production login should call ${PROD_EXPECTED_BACKEND_ORIGIN}, got ${JSON.stringify(
        loginRequests
      )}`
    )
    assert.equal(
      loginRequests.some((url) =>
        url.startsWith(`${PROD_FORBIDDEN_TEST_BACKEND_ORIGIN}/admin-api/system/auth/login`)
      ),
      false,
      `production login must not call ${PROD_FORBIDDEN_TEST_BACKEND_ORIGIN}, got ${JSON.stringify(
        loginRequests
      )}`
    )
    console.log(
      `PROD_LOGIN_OK loginUrl=${PROD_LOGIN_URL} expectedBackend=${PROD_EXPECTED_BACKEND_ORIGIN} ` +
        `forbiddenBackend=${PROD_FORBIDDEN_TEST_BACKEND_ORIGIN} requests=${JSON.stringify(
          loginRequests
        )}`
    )
  } finally {
    await browser.close()
  }
}

requireExplicitApproval()

runRuntimeControlE2E(`runtime control real ${PROMOTE_SCOPE} promote-prod flow`, async ({ page }) => {
  const latestLogRef = { value: null, error: null }
  page.on('response', async (response) => {
    if (!response.url().includes('/infra/runtime-control/operations/')) return
    if (!response.url().includes('/log')) return
    try {
      const payload = await response.json()
      if (payload?.code === 0 && payload?.data?.operationId) {
        latestLogRef.value = payload.data
      }
    } catch (error) {
      latestLogRef.error = error
    }
  })

  const actionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      response.url() === `${ACTION_ORIGIN}/admin-api/infra/runtime-control/actions`,
    { timeout: 60000 }
  )

  const dialog = await openOperationDialog(page, '上线已验证发布包')
  await selectPublishScope(dialog)
  await fillDialogReason(dialog, PROMOTE_REASON)
  await fillProdConfirm(dialog)
  await page.getByRole('button', { name: '确认执行' }).click()

  const actionResponse = await actionResponsePromise
  assert.equal(actionResponse.status(), 200)
  const actionPayload = await actionResponse.json()
  assert.equal(actionPayload.code, 0, actionPayload.msg || 'promote-prod action should be accepted')
  const operation = actionPayload.data
  assert.ok(operation?.operationId, 'promote-prod action should return operationId')
  assert.equal(operation.action, 'promote-prod')
  assert.equal(operation.parameters?.publishScope, PROMOTE_SCOPE)
  assert.equal(operation.reason, PROMOTE_REASON)
  console.log(`PROMOTE_DISPATCHED operationId=${operation.operationId}`)

  const latestLog = await waitForPromotionCompletion(page, operation.operationId, latestLogRef)
  assert.equal(latestLog.status, 'succeeded')
  await verifyProductionHealth()
  await verifyProductionWebsiteBrowser()
  await verifyProductionLogin()
})
