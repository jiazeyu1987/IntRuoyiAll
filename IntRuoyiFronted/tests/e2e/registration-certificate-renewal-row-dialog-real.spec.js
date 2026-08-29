const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const TASK_DIR = path.join(
  REPO_ROOT,
  'doc',
  'tasks',
  '20260829-registration-certificate-renewal-category-notify'
)
const ARTIFACT_DIR = process.env.REG_CERT_RENEWAL_E2E_ARTIFACT_DIR
  ? path.resolve(process.env.REG_CERT_RENEWAL_E2E_ARTIFACT_DIR)
  : path.join(TASK_DIR, 'e2e-artifacts')
const RESULT_PATH = path.join(ARTIFACT_DIR, 'registration-certificate-renewal-dialog-result.json')

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
  baseUrl: (
    process.env.REG_CERT_E2E_BASE_URL ||
    process.env.E2E_BASE_URL ||
    `http://127.0.0.1:${readDotEnvValue('VITE_PORT') || '8081'}`
  ).replace(/\/+$/, ''),
  tenant:
    process.env.REG_CERT_E2E_TENANT ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT') ||
    '芋道源码',
  username:
    process.env.REG_CERT_E2E_USERNAME ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_USERNAME') ||
    'admin',
  password: process.env.REG_CERT_E2E_PASSWORD || readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
}

function writeResult(result) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function isBusinessOk(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractPageResult(payload) {
  const data = payload && payload.data
  return {
    list: Array.isArray(data?.list) ? data.list : [],
    total: Number(data?.total || 0)
  }
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page) {
  expect(config.password, 'login password must be available without logging it').toBeTruthy()

  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    if (await tenantOption.isVisible({ timeout: 1000 }).catch(() => false)) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(config.username)
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
  expect(
    isBusinessOk(tenantPayload),
    `tenant business code ${tenantPayload.code}, message=${tenantPayload.msg || ''}`
  ).toBe(true)
  expect(tenantPayload.data, 'tenant id must be resolved before login').toBeTruthy()

  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(
    isBusinessOk(loginPayload),
    `login business code ${loginPayload.code}, message=${loginPayload.msg || ''}`
  ).toBe(true)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function registrationPath(response, suffix) {
  if (response.request().method() !== 'GET') return false
  const pathname = new URL(response.url()).pathname
  return pathname.endsWith(`/admin-api/dcc/registration-certificates${suffix}`)
}

async function selectDialogOption(page, dialog, label, optionText) {
  const field = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await field.locator('.el-select').click()
  const option = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: new RegExp(`^\\s*${optionText}\\s*$`) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

test.describe('registration certificate renewal row dialog real UI', () => {
  test('row renewal button opens the approval-bound renewal form with category change controls', async ({
    page
  }, testInfo) => {
    test.setTimeout(180000)

    const evidence = {
      status: 'RUNNING',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      responses: [],
      writeRequests: [],
      requestFailures: [],
      pageErrors: []
    }

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('requestfailed', (request) => {
      const url = request.url()
      if (
        url.includes('/admin-api/dcc/registration-certificates') ||
        url.includes('/admin-api/system/tenant') ||
        url.includes('/admin-api/system/auth')
      ) {
        evidence.requestFailures.push({
          method: request.method(),
          path: new URL(url).pathname,
          failure: request.failure()?.errorText || ''
        })
      }
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
      const permissionResponsePromise = page.waitForResponse(
        (response) =>
          response.url().includes('/system/auth/get-permission-info') &&
          response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await login(page)
      const permissionPayload = await readJsonResponse(await permissionResponsePromise)
      expect(
        isBusinessOk(permissionPayload),
        `permission-info code ${permissionPayload.code}`
      ).toBe(true)
      const permissions = JSON.stringify(permissionPayload.data || {})
      expect(permissions).toContain('dcc:registration-certificate:query-current')
      expect(permissions).toContain('dcc:registration-certificate:renewal:upload')

      const pageResponsePromise = page.waitForResponse(
        (response) => registrationPath(response, '/page'),
        { timeout: 60000 }
      )
      await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
      await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
        timeout: 60000
      })

      const pagePayload = await readJsonResponse(await pageResponsePromise)
      expect(
        isBusinessOk(pagePayload),
        `current page code ${pagePayload.code}, message=${pagePayload.msg || ''}`
      ).toBe(true)
      const currentPage = extractPageResult(pagePayload)
      expect(
        currentPage.list.length,
        'E2E requires at least one current registration certificate row'
      ).toBeGreaterThan(0)
      const selected = currentPage.list[0]
      evidence.currentCount = currentPage.total
      evidence.selectedCertificateId = selected.certificateId
      evidence.selectedVersionId = selected.versionId
      evidence.selectedCertificateNo = selected.certificateNo

      const firstRow = page.locator('.el-table:visible .el-table__row').first()
      await expect(firstRow, 'current registration certificate row must render').toBeVisible({
        timeout: 60000
      })
      await expect(firstRow.getByRole('button', { name: '详情' })).toBeVisible()
      await expect(firstRow.getByRole('button', { name: '延续' })).toBeVisible()
      await firstRow.getByRole('button', { name: '延续' }).click()

      const dialog = page.locator('[data-testid="registration-certificate-renewal-dialog"]')
      await expect(dialog).toBeVisible({ timeout: 60000 })
      await expect(dialog.getByText('批准日期')).toBeVisible()
      await expect(dialog.getByText('生效日期')).toBeVisible()
      await expect(dialog.getByText('有效期至')).toBeVisible()
      await expect(dialog.getByText('类别否变更')).toBeVisible()
      await expect(dialog.locator('[data-testid="registration-certificate-renewal-file"]')).toBeVisible()
      await expect(dialog.getByRole('button', { name: '提交审批' })).toBeVisible()

      await expect(dialog.locator('input[placeholder="请输入变更后的注册证号"]')).toHaveCount(0)
      await expect(dialog.locator('input[placeholder="请输入变更后的类别"]')).toHaveCount(0)
      await selectDialogOption(page, dialog, '类别否变更', '是')
      await expect(dialog.locator('input[placeholder="请输入变更后的注册证号"]')).toBeVisible()
      await expect(dialog.locator('input[placeholder="请输入变更后的类别"]')).toBeVisible()

      const screenshotPath = testInfo.outputPath('registration-certificate-renewal-dialog.png')
      await page.screenshot({ path: screenshotPath, fullPage: true })

      await selectDialogOption(page, dialog, '类别否变更', '否')
      await expect(dialog.locator('input[placeholder="请输入变更后的注册证号"]')).toHaveCount(0)
      await expect(dialog.locator('input[placeholder="请输入变更后的类别"]')).toHaveCount(0)

      expect(evidence.writeRequests, 'read-only dialog inspection must not submit writes').toEqual([])
      expect(evidence.requestFailures, 'targeted auth and registration requests must not fail').toEqual([])
      expect(evidence.pageErrors, 'real page must not emit page errors').toEqual([])

      evidence.status = 'PASS'
      evidence.dialog = {
        rowActions: ['详情', '延续'],
        fixedRenewalFields: ['批准日期', '生效日期', '有效期至', '延续注册证文件'],
        categoryChangeToggle: '类别否变更',
        categoryChangedYesFields: ['注册证号', '类别'],
        screenshotPath
      }
      writeResult(evidence)
    } catch (error) {
      evidence.status = 'FAIL'
      evidence.error = error.stack || error.message
      writeResult(evidence)
      throw error
    }
  })
})
