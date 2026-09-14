const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260830-registration-certificate-reminder-sort-filter-regression'
)
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-reminder-sort-runtime-result.json')

function readDotEnvValue(name) {
  for (const fileName of ['.env.local', '.env']) {
    const filePath = path.join(FRONTEND_ROOT, fileName)
    if (!fs.existsSync(filePath)) continue
    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
    for (const line of lines) {
      const match = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/)
      if (match && match[1] === name) {
        return match[2].replace(/^['"]|['"]$/g, '')
      }
    }
  }
  return ''
}

const config = {
  baseUrl: (process.env.REG_CERT_SORT_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.REG_CERT_SORT_E2E_TENANT || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT'),
  username: process.env.REG_CERT_SORT_E2E_USERNAME || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME'),
  password: process.env.REG_CERT_SORT_E2E_PASSWORD || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page) {
  expect(config.tenant, 'login tenant must be available').toBeTruthy()
  expect(config.username, 'login username must be available').toBeTruthy()
  expect(config.password, 'login password must be available without logging it').toBeTruthy()

  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const tenantResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/tenant/get-id-by-name') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()

  const tenantResponse = await tenantResponsePromise
  const tenantPayload = await readJsonResponse(tenantResponse)
  expect(tenantResponse.ok(), `tenant HTTP status ${tenantResponse.status()}`).toBe(true)
  expect(isBusinessOk(tenantPayload), `tenant business code ${tenantPayload.code}`).toBe(true)
  expect(tenantPayload.data, 'tenant id must be resolved before login').toBeTruthy()

  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(isBusinessOk(loginPayload), `login business code ${loginPayload.code}`).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function isReminderSortPageResponse(response, sortOrder) {
  if (response.request().method() !== 'GET') return false
  const url = new URL(response.url())
  return (
    url.pathname.endsWith('/admin-api/dcc/registration-certificates/page') &&
    url.searchParams.get('sortField') === 'reminder' &&
    url.searchParams.get('sortOrder') === sortOrder
  )
}

async function waitForReminderSortPage(page, sortOrder) {
  const response = await page.waitForResponse((item) => isReminderSortPageResponse(item, sortOrder), {
    timeout: 60000
  })
  const payload = await readJsonResponse(response)
  expect(response.ok(), `reminder sorted HTTP status ${response.status()}`).toBe(true)
  expect(isBusinessOk(payload), `reminder sorted business code ${payload.code}; message=${payload.msg || ''}`).toBe(
    true
  )
  return {
    status: response.status(),
    code: payload.code,
    total: Number(payload.data?.total || 0),
    reminders: Array.isArray(payload.data?.list)
      ? payload.data.list.map((item) => String(item.reminderState || item.reminder || ''))
      : []
  }
}

test.describe('registration certificate reminder sort runtime', () => {
  test('current list reminder header sends ascending and descending sort without system exception', async ({ page }) => {
    test.setTimeout(180000)
    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      identity: `${config.tenant}/${config.username}`,
      writeRequests: [],
      consoleErrors: [],
      pageErrors: []
    }

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') evidence.consoleErrors.push(message.text())
    })
    page.on('request', (request) => {
      const url = request.url()
      if (
        url.includes('/admin-api/dcc/registration-certificates') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
      ) {
        evidence.writeRequests.push({ method: request.method(), path: new URL(url).pathname })
      }
    })

    try {
      await login(page)

      const initialPagePromise = page.waitForResponse(
        (response) =>
          response.request().method() === 'GET' &&
          new URL(response.url()).pathname.endsWith('/admin-api/dcc/registration-certificates/page'),
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
      const initialPayload = await readJsonResponse(await initialPagePromise)
      expect(isBusinessOk(initialPayload), `initial page business code ${initialPayload.code}`).toBe(true)

      const currentTab = page.locator('[data-testid="registration-certificate-current-tab"]')
      await expect(currentTab).toBeVisible({ timeout: 60000 })
      const reminderHeader = currentTab.locator('.el-table__header-wrapper th').filter({ hasText: '提醒状态' }).first()
      await expect(reminderHeader, 'reminder header must be visible').toBeVisible({ timeout: 60000 })

      const ascPromise = waitForReminderSortPage(page, 'asc')
      await reminderHeader.click()
      evidence.asc = await ascPromise

      const descPromise = waitForReminderSortPage(page, 'desc')
      await reminderHeader.click()
      evidence.desc = await descPromise

      expect(evidence.asc.total, 'ascending reminder sort must return the current list total').toBeGreaterThan(0)
      expect(evidence.desc.total, 'descending reminder sort must return the current list total').toBeGreaterThan(0)
      expect(evidence.writeRequests, 'sort verification must not send registration-certificate write requests').toEqual(
        []
      )
      expect(evidence.pageErrors, 'page must not raise runtime errors').toEqual([])

      evidence.status = 'PASS'
      writeResult(evidence)
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error.message
      writeResult(evidence)
      throw error
    }
  })
})
