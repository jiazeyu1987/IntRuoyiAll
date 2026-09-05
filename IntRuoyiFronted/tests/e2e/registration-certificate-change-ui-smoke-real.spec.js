const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260904-registration-change-e2e-sync'
)
const ARTIFACT_DIR = path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-change-ui-smoke-result.json')

function readDotEnvValue(name) {
  for (const fileName of ['.env.local', '.env']) {
    const filePath = path.join(FRONTEND_ROOT, fileName)
    if (!fs.existsSync(filePath)) continue
    const lines = fs.readFileSync(filePath, 'utf8').split(/\r?\n/)
    for (const line of lines) {
      const match = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*?)\s*$/)
      if (match && match[1] === name) return match[2].replace(/^['"]|['"]$/g, '')
    }
  }
  return ''
}

const config = {
  baseUrl: (process.env.REG_CERT_CHANGE_E2E_BASE_URL || 'http://127.0.0.1:8154').replace(/\/+$/, ''),
  tenant: process.env.REG_CERT_CHANGE_E2E_TENANT || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT'),
  username:
    process.env.REG_CERT_CHANGE_E2E_USERNAME || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME'),
  password:
    process.env.REG_CERT_CHANGE_E2E_PASSWORD || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
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
  expect(config.tenant, 'tenant must be configured').toBeTruthy()
  expect(config.username, 'username must be configured').toBeTruthy()
  expect(config.password, 'password must be configured without logging it').toBeTruthy()

  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    await page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first().click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const tenantResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/tenant/get-id-by-name') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const tenantResponse = await tenantResponsePromise
  const tenantPayload = await readJsonResponse(tenantResponse)
  expect(tenantResponse.ok(), `tenant HTTP status ${tenantResponse.status()}`).toBe(true)
  expect(isBusinessOk(tenantPayload), `tenant business code ${tenantPayload.code}`).toBe(true)
  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(isBusinessOk(loginPayload), `login business code ${loginPayload.code}`).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

test.describe('registration certificate change MVP real UI smoke', () => {
  test('opens change upload form and exposes required MVP controls without submitting', async ({
    page
  }) => {
    test.setTimeout(180000)

    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      selectedCertificateId: '',
      responses: [],
      writeRequests: [],
      failedResponses: [],
      consoleErrors: [],
      pageErrors: []
    }

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') evidence.consoleErrors.push(message.text())
    })
    page.on('request', (request) => {
      const method = request.method()
      const url = request.url()
      if (
        !['GET', 'HEAD', 'OPTIONS'].includes(method) &&
        url.includes('/admin-api/dcc/registration-certificates')
      ) {
        evidence.writeRequests.push({ method, path: new URL(url).pathname })
      }
    })
    page.on('response', async (response) => {
      const url = response.url()
      if (response.status() >= 400) {
        evidence.failedResponses.push({
          method: response.request().method(),
          path: new URL(url).pathname,
          status: response.status()
        })
      }
      if (!url.includes('/admin-api/dcc/registration-certificates')) return
      const payload = await readJsonResponse(response)
      evidence.responses.push({
        method: response.request().method(),
        path: new URL(url).pathname,
        status: response.status(),
        code: payload.code,
        message: payload.msg || payload.message || ''
      })
    })

    try {
      await login(page)
      const pageResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/admin-api/dcc/registration-certificates/page') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, {
        waitUntil: 'commit',
        timeout: 60000
      })
      await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
        timeout: 60000
      })
      const pagePayload = await readJsonResponse(await pageResponsePromise)
      expect(isBusinessOk(pagePayload), `page business code ${pagePayload.code}`).toBe(true)
      const firstCertificate = pagePayload.data?.list?.[0]
      expect(firstCertificate?.certificateId, 'current certificate fixture must exist').toBeTruthy()
      evidence.selectedCertificateId = String(firstCertificate.certificateId)

      const firstCurrentRow = page
        .locator('.el-table:visible .el-table__row')
        .filter({ has: page.getByRole('button', { name: '变更' }) })
        .first()
      await expect(firstCurrentRow.getByRole('button', { name: '变更' })).toBeVisible({
        timeout: 60000
      })

      await firstCurrentRow.getByRole('button', { name: '变更' }).click()
      await expect(page.locator('[data-testid="registration-certificate-change-dialog"]')).toBeVisible({
        timeout: 60000
      })
      const actionPanel = page.locator('[data-testid="registration-certificate-change-form"]')
      await expect(actionPanel).toBeVisible({ timeout: 60000 })
      await expect(page.locator('[data-testid="registration-certificate-access-request-action"]')).toHaveCount(0)
      await expect(actionPanel.getByText('批准日期', { exact: true })).toBeVisible()
      await expect(actionPanel.getByText('变更内容', { exact: true })).toBeVisible()
      await expect(actionPanel.locator('[data-testid="registration-certificate-change-approval-file"]')).toBeVisible()
      await expect(page.locator('[data-testid="registration-certificate-change-dialog"]').getByRole('button', { name: '确认' })).toBeVisible()

      const changeSelect = actionPanel
        .locator('.el-form-item')
        .filter({ hasText: '变更内容' })
        .locator('.el-select')
        .first()
      await changeSelect.click()
      for (const optionName of [
        '产品名称',
        '型号规格',
        '结构组成',
        '适用范围',
        '产品技术要求',
        '注册人名称',
        '住所',
        '生产地址',
        '其他内容'
      ]) {
        await expect(
          page.locator('.el-select-dropdown__item:visible').filter({ hasText: optionName }).first(),
          `${optionName} option must be present`
        ).toBeVisible({ timeout: 30000 })
      }
      for (const optionName of ['产品名称', '注册人名称', '生产地址', '其他内容']) {
        await page.locator('.el-select-dropdown__item:visible').filter({ hasText: optionName }).first().click()
      }
      await page.keyboard.press('Escape')

      await expect(actionPanel.locator('input[placeholder="变更后的产品名称"]')).toBeVisible()
      await expect(actionPanel.locator('input[placeholder="变更后的注册人名称"]')).toBeVisible()
      await expect(actionPanel.getByText('其他说明')).toBeVisible()
      await expect(actionPanel.getByText('是否委托生产')).toBeVisible()
      await expect(actionPanel.getByText('是否自行生产')).toBeVisible()

      evidence.status = 'PASS'
      writeResult(evidence)
      expect(evidence.writeRequests, 'smoke test must not submit registration writes').toEqual([])
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error instanceof Error ? error.message : String(error)
      writeResult(evidence)
      throw error
    }
  })
})

