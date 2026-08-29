const fs = require('node:fs')
const path = require('node:path')
const { test, expect } = require('playwright/test')

test.setTimeout(240000)

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
    'admin123',
  uploadCompanyName:
    process.env.REG_CERT_E2E_UPLOAD_COMPANY_NAME ||
    '上海瑛泰医疗器械股份有限公司'
}

function optionTextPattern(text) {
  return new RegExp(`^\\s*${text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*$`)
}

async function selectVisibleOption(page, optionText) {
  const option = page
    .locator('.el-select-dropdown__item:visible:not(.is-disabled)')
    .filter({ hasText: optionTextPattern(optionText) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ timeout: 30000, force: true })
}

async function selectOptionFromSelect(page, select, optionText) {
  const combobox = select.locator('input[role="combobox"], .el-select__input').first()
  await select.click()
  const controls = await combobox.getAttribute('aria-controls')
  const scope = controls
    ? page.locator(`#${controls}`)
    : page.locator('.el-select-dropdown:visible').last()
  const option = scope
    .locator('.el-select-dropdown__item:not(.is-disabled)')
    .filter({ hasText: optionTextPattern(optionText) })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click({ timeout: 30000, force: true })
}

async function selectFirstOptionFromSelect(page, select) {
  const combobox = select.locator('input[role="combobox"], .el-select__input').first()
  await select.click()
  const controls = await combobox.getAttribute('aria-controls')
  const scope = controls
    ? page.locator(`#${controls}`)
    : page.locator('.el-select-dropdown:visible').last()
  const option = scope
    .locator('.el-select-dropdown__item:not(.is-disabled)')
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const text = (await option.textContent())?.trim() || ''
  await option.click({ timeout: 30000, force: true })
  return text
}

async function readJsonResponse(response) {
  try {
    return await response.json()
  } catch (error) {
    return { parseError: error.message }
  }
}

async function login(page, events) {
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
    await selectVisibleOption(page, config.tenant)
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
      response.url().includes('/system/tenant/get-id-by-name') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  ).catch((error) => {
    throw new Error(`login POST was not sent or did not return: ${error.message}; events=${JSON.stringify(events)}`)
  })
  await form.getByRole('button', { name: '登录' }).click()
  const tenantResponse = await tenantResponsePromise
  const tenantPayload = await readJsonResponse(tenantResponse)
  expect(tenantResponse.ok(), `tenant HTTP status ${tenantResponse.status()}`).toBe(true)
  expect(
    tenantPayload.code,
    `tenant business code ${tenantPayload.code}: ${tenantPayload.msg || ''}; events=${JSON.stringify(events)}`
  ).toBe(0)
  expect(tenantPayload.data, `tenant id missing; events=${JSON.stringify(events)}`).toBeTruthy()
  const loginResponse = await loginResponsePromise
  const loginPayload = await readJsonResponse(loginResponse)
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBe(true)
  expect(loginPayload.code, `login business code ${loginPayload.code}`).toBe(0)
}

async function selectDialogOption(page, dialog, label, optionText) {
  const labelNode = dialog
    .locator('.el-form-item__label')
    .filter({ hasText: optionTextPattern(label) })
    .first()
  const field = labelNode.locator(
    'xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-form-item ")][1]'
  )
  await selectOptionFromSelect(page, field.locator('.el-select'), optionText)
}

test('upload submit succeeds without SkyWalking trace id', async ({ page }, testInfo) => {
  const events = []
  let uploadPostCount = 0
  page.on('request', (request) => {
    if (
      request.url().includes('/dcc/registration-certificates/uploads') &&
      request.method() === 'POST'
    ) {
      uploadPostCount += 1
    }
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (
      url.includes('/dcc/registration-certificates/uploads') ||
      url.includes('/dcc/project-codes/page') ||
      url.includes('/system/tenant/') ||
      url.includes('/system/auth/login')
    ) {
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
    if (
      url.includes('/dcc/registration-certificates/uploads') ||
      url.includes('/dcc/project-codes/page') ||
      url.includes('/system/tenant/') ||
      url.includes('/system/auth/login')
    ) {
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
  await login(page, events)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await readJsonResponse(permissionResponse)
  expect(permissionPayload.code, `permission-info code ${permissionPayload.code}`).toBe(0)
  expect(JSON.stringify(permissionPayload.data || {})).toContain(
    'dcc:registration-certificate:upload:create'
  )
  await page.waitForURL((url) => url.pathname === '/index', { timeout: 60000 })

  await page.goto(`${config.baseUrl}/mdm/registration-certificate`, { waitUntil: 'commit' })
  await expect(page.locator('[data-testid="registration-certificate-read-page"]')).toBeVisible({
    timeout: 60000
  })
  await page.getByRole('button', { name: '上传注册证' }).click()
  await expect(page.locator('[data-testid="registration-certificate-upload-dialog"]')).toBeVisible({
    timeout: 60000
  })

  const dialog = page.locator('[data-testid="registration-certificate-upload-dialog"]')
  const visibleLabels = await dialog.locator('.el-form-item__label').evaluateAll((labels) =>
    labels.map((label) => label.textContent?.trim()).filter(Boolean)
  )
  const allowedLabels = [
    'DCC项目代码',
    '公司名称',
    '项目代码',
    '产品名称',
    '注册证号',
    '类别',
    '首次获证日期',
    '生效日期',
    '有效期至',
    '是否委托生产',
    '是否自行生产',
    '备注',
    '注册证文件'
  ]
  for (const label of allowedLabels) {
    expect(visibleLabels).toContain(label)
  }
  expect(visibleLabels.filter((label) => !allowedLabels.includes(label))).toEqual([])
  for (const legacyLabel of ['批注日期', '变更内容', '变更后内容', '注册人名称', '型号规格', '生产地址']) {
    expect(visibleLabels).not.toContain(legacyLabel)
  }
  const projectCodeLoad = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/project-codes/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await dialog.locator('.el-select input[role="combobox"], .el-select__input').first().click()
  await projectCodeLoad
  const projectCodeOption = page
    .locator(
      '.el-popper[aria-hidden="false"] .el-select-dropdown__item:not(.is-disabled), .el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)'
    )
    .first()
  await projectCodeOption.waitFor({ state: 'visible', timeout: 30000 })
  await projectCodeOption.click()

  const certificateNo = `REGCERT-NET-ERROR-${Date.now()}-${testInfo.workerIndex}`
  const pdfPath = testInfo.outputPath('registration-certificate-upload-repro.pdf')
  fs.writeFileSync(pdfPath, Buffer.from('%PDF-1.4\n% Codex repro file\n', 'utf8'))
  await dialog.locator('input[type="file"]').setInputFiles(pdfPath)
  await dialog.locator('input[placeholder="请输入公司名称"]').fill(config.uploadCompanyName)
  await dialog.locator('input[placeholder="请输入注册证号"]').fill(certificateNo)
  const dateInputs = dialog.locator('input[placeholder="请选择日期"]')
  await dateInputs.nth(0).fill('2025-01-01')
  await dateInputs.nth(1).fill('2025-01-02')
  await dateInputs.nth(2).fill('2026-01-01')
  await dialog.locator('input[placeholder="请输入类别"]').fill('A类')
  await dialog.locator('textarea[placeholder="请输入备注"]').fill('network error repro')
  await dialog.locator('textarea[placeholder="请输入备注"]').blur()

  const saveButton = dialog.getByRole('button', { name: '保存' })
  await selectDialogOption(page, dialog, '是否委托生产', '否')
  await selectDialogOption(page, dialog, '是否自行生产', '否')

  await expect(saveButton).toBeEnabled()
  const invalidUploadPostCount = uploadPostCount
  await saveButton.click()
  await expect(
    page.locator('.el-form-item__error').filter({ hasText: '是否委托生产和是否自行生产不能同时为否' }).first()
  ).toBeVisible({
    timeout: 10000
  })
  expect(uploadPostCount).toBe(invalidUploadPostCount)

  const entrustedEnterpriseLoadPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/registration-certificates/uploads/entrusted-enterprises') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await selectDialogOption(page, dialog, '是否委托生产', '是')
  const entrustedEnterpriseLoadResponse = await entrustedEnterpriseLoadPromise
  const entrustedEnterpriseLoadPayload = await readJsonResponse(entrustedEnterpriseLoadResponse)
  expect(
    entrustedEnterpriseLoadResponse.ok(),
    `entrusted enterprise HTTP status ${entrustedEnterpriseLoadResponse.status()}`
  ).toBe(true)
  expect(
    entrustedEnterpriseLoadPayload.code,
    `entrusted enterprise business code ${entrustedEnterpriseLoadPayload.code}: ${entrustedEnterpriseLoadPayload.msg || ''}`
  ).toBe(0)
  expect(
    entrustedEnterpriseLoadPayload.data?.length,
    'entrusted enterprise candidates must exist for entrusted production E2E'
  ).toBeGreaterThan(0)
  await expect(
    dialog.locator('.el-form-item__label').filter({ hasText: optionTextPattern('受托企业') }).first()
  ).toBeVisible({
    timeout: 10000
  })
  const missingEntrustedUploadPostCount = uploadPostCount
  await saveButton.click()
  await expect(
    page.locator('.el-form-item__error').filter({ hasText: '请选择受托企业' }).first()
  ).toBeVisible({
    timeout: 10000
  })
  expect(uploadPostCount).toBe(missingEntrustedUploadPostCount)

  const entrustedEnterpriseText = await selectFirstOptionFromSelect(
    page,
    dialog.locator('[data-testid="registration-certificate-upload-entrusted-enterprises"]')
  )
  expect(entrustedEnterpriseText).toBeTruthy()

  await expect(saveButton).toBeEnabled()
  const uploadResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/registration-certificates/uploads') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const approvalCenterLoadPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/approval-center/tasks/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await saveButton.click()
  const uploadResponse = await uploadResponsePromise
  const uploadPayload = await readJsonResponse(uploadResponse)
  const approvalCenterLoadResponse = await approvalCenterLoadPromise
  const approvalCenterLoadPayload = await readJsonResponse(approvalCenterLoadResponse)

  console.log(JSON.stringify(events, null, 2))
  expect(uploadResponse.ok(), `upload HTTP status ${uploadResponse.status()}`).toBe(true)
  expect(uploadPayload.code, `upload business code ${uploadPayload.code}: ${uploadPayload.msg || ''}`).toBe(0)
  expect(approvalCenterLoadResponse.ok(), `approval-center HTTP status ${approvalCenterLoadResponse.status()}`).toBe(true)
  expect(
    approvalCenterLoadPayload.code,
    `approval-center business code ${approvalCenterLoadPayload.code}: ${approvalCenterLoadPayload.msg || ''}`
  ).toBe(0)
  expect(events.filter((event) => event.kind === 'requestfailed')).toEqual([])
  await expect(page).toHaveURL(
    (url) =>
      url.pathname === '/approval-center/todo' &&
      url.searchParams.get('viewType') === 'TODO' &&
      !url.searchParams.has('moduleCode'),
    { timeout: 60000 }
  )
  await expect(page.locator('.approval-center')).toBeVisible({ timeout: 60000 })
})
