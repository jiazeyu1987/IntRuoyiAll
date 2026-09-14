const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

const FRONTEND_ROOT = path.resolve(__dirname, '..', '..')

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
  password:
    process.env.REG_CERT_E2E_PASSWORD ||
    readDotEnvValue('VITE_APP_DEFAULT_LOGIN_PASSWORD') ||
    'admin123'
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input.el-input__inner:not([role="combobox"]):visible')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(loginPayload.code, `login business code ${loginPayload.code}`).toBe(0)
}

test('upload button is visible and opens the upload dialog', async ({ page }) => {
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )

  await login(page)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await readJsonResponse(permissionResponse)
  expect(permissionPayload.code, `permission-info code ${permissionPayload.code}`).toBe(0)
  expect(JSON.stringify(permissionPayload.data || {})).toContain(
    'dcc:registration-certificate:upload:create'
  )

  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
    timeout: 60000
  })

  const uploadButton = page.getByRole('button', { name: '上传注册证' })
  await expect(uploadButton).toBeVisible({ timeout: 60000 })
  await uploadButton.click()
  await expect(page.locator('[data-testid="registration-certificate-upload-dialog"]')).toBeVisible({
    timeout: 60000
  })
})
