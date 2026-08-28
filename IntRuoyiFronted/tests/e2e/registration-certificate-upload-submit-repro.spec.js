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

test('upload submit succeeds without SkyWalking trace id', async ({ page }, testInfo) => {
  test.setTimeout(240000)

  const events = []
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (url.includes('/dcc/registration-certificates/uploads') || url.includes('/dcc/project-codes/page')) {
      events.push({
        kind: 'requestfailed',
        url,
        method: request.method(),
        failure: request.failure()?.errorText || ''
      })
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/dcc/registration-certificates/uploads') || url.includes('/dcc/project-codes/page')) {
      const payload = await readJsonResponse(response)
      events.push({
        kind: 'response',
        url,
        method: response.request().method(),
        status: response.status(),
        code: payload.code,
        msg: payload.msg || payload.message || ''
      })
    }
  })

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
  await page.getByRole('button', { name: '上传注册证' }).click()
  await expect(page.locator('[data-testid="registration-certificate-upload-dialog"]')).toBeVisible({
    timeout: 60000
  })

  const dialog = page.locator('[data-testid="registration-certificate-upload-dialog"]')
  const projectCodeLoad = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/project-codes/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await dialog.locator('.el-select input[role="combobox"], .el-select__input').first().click()
  await projectCodeLoad
  const projectCodeOption = page.locator('.el-select-dropdown__item:visible').first()
  await projectCodeOption.waitFor({ state: 'visible', timeout: 30000 })
  await projectCodeOption.click()

  const certificateNo = `REGCERT-NET-ERROR-${Date.now()}-${testInfo.workerIndex}`
  const pdfPath = testInfo.outputPath('registration-certificate-upload-repro.pdf')
  fs.writeFileSync(pdfPath, Buffer.from('%PDF-1.4\n% Codex repro file\n', 'utf8'))
  await dialog.locator('input[type="file"]').setInputFiles(pdfPath)
  await dialog.locator('input[placeholder="请输入公司名称"]').fill('上海七木医疗器械有限公司')
  await dialog.locator('input[placeholder="请输入注册证号"]').fill(certificateNo)
  const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
  await dateInputs.nth(0).fill('2025-01-01')
  await dateInputs.nth(1).fill('2025-01-02')
  await dateInputs.nth(2).fill('2026-01-01')
  await dialog.locator('input[placeholder="请输入类别"]').fill('A类')
  await dialog.locator('textarea[placeholder="请输入备注"]').fill('network error repro')

  const saveButton = dialog.getByRole('button', { name: '保存' })
  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/registration-certificates/uploads') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await saveButton.click()
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await readJsonResponse(uploadResponse)

  console.log(JSON.stringify(events, null, 2))
  expect(uploadResponse.ok(), `upload HTTP status ${uploadResponse.status()}`).toBe(true)
  expect(uploadPayload.code, `upload business code ${uploadPayload.code}: ${uploadPayload.msg || ''}`).toBe(0)
  expect(events.filter((event) => event.kind === 'requestfailed')).toEqual([])
  await expect(page).toHaveURL(/approval-center\?moduleCode=DCC&viewType=TODO/, { timeout: 60000 })
})
