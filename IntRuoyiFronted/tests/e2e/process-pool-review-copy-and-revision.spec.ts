import { chromium, expect, test, type Browser, type Locator, type Page } from 'playwright/test'

test.describe.configure({ mode: 'serial' })
test.setTimeout(240000)

const REVIEW_PATH = '/mes/pro/process-pool/review-copy'
const REVISION_PATH = '/mes/pro/process-pool/event-revision'
const REVIEW_ENDPOINT = '/mes/pro/process-pool/review-copy/generate-submit'
const REVISION_ENDPOINT = '/mes/pro/process-pool/event-revision/update-original'

const requireEnv = (name: string) => {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value.trim()
}

const BASE_URL = requireEnv('PROCESS_POOL_E2E_BASE_URL')
const TENANT_NAME = requireEnv('PROCESS_POOL_E2E_TENANT')
const USERNAME = requireEnv('PROCESS_POOL_E2E_USERNAME')
const PASSWORD = requireEnv('PROCESS_POOL_E2E_PASSWORD')
const CHROME_EXECUTABLE = requireEnv('PROCESS_POOL_E2E_CHROME_EXECUTABLE')

const REVIEW_EVENT_ID = requireEnv('PROCESS_POOL_E2E_REVIEW_EVENT_ID')
const REVIEWER_USER_ID = requireEnv('PROCESS_POOL_E2E_REVIEWER_USER_ID')
const REVIEW_SIGNATURE_ID = requireEnv('PROCESS_POOL_E2E_REVIEW_SIGNATURE_ID')
const REVIEW_SIGNATURE_USER_ID = requireEnv('PROCESS_POOL_E2E_REVIEW_SIGNATURE_USER_ID')
const REVIEW_SIGNATURE_SNAPSHOT = requireEnv('PROCESS_POOL_E2E_REVIEW_SIGNATURE_SNAPSHOT')
const REVIEW_FIELD_MAPPINGS = requireEnv('PROCESS_POOL_E2E_REVIEW_FIELD_MAPPINGS')

const REVISION_EVENT_ID = requireEnv('PROCESS_POOL_E2E_REVISION_EVENT_ID')
const REVISION_MODIFIED_BY_USER_ID = requireEnv('PROCESS_POOL_E2E_REVISION_MODIFIED_BY_USER_ID')
const REVISION_SIGNATURE_ID = requireEnv('PROCESS_POOL_E2E_REVISION_SIGNATURE_ID')
const REVISION_SIGNATURE_USER_ID = requireEnv('PROCESS_POOL_E2E_REVISION_SIGNATURE_USER_ID')
const REVISION_SIGNATURE_SNAPSHOT = requireEnv('PROCESS_POOL_E2E_REVISION_SIGNATURE_SNAPSHOT')
const REVISION_AFTER_PAYLOAD = requireEnv('PROCESS_POOL_E2E_REVISION_AFTER_PAYLOAD')
const REVISION_CHANGED_FIELDS = requireEnv('PROCESS_POOL_E2E_REVISION_CHANGED_FIELDS')
const REVISION_CHANGE_REASON = requireEnv('PROCESS_POOL_E2E_REVISION_CHANGE_REASON')

function expectJson(value: string, label: string) {
  expect(() => JSON.parse(value), `${label} must be valid JSON`).not.toThrow()
}

function isSuccessPayload(payload: any) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function login(page: Page) {
  await page.context().clearCookies()
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(REVIEW_PATH)}`, {
    waitUntil: 'domcontentloaded'
  })

  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
    .first()
  await tenantInput.fill(TENANT_NAME)
  await tenantInput.press('Enter')

  const usernameInput = loginForm.locator('input[placeholder="请输入用户名"]').first()
  if (await usernameInput.count()) {
    await usernameInput.fill('')
    await usernameInput.fill(USERNAME)
  } else {
    const textboxes = loginForm.getByRole('textbox')
    const textboxCount = await textboxes.count()
    if (textboxCount < 2) {
      throw new Error('No visible username input found')
    }
    const target = textboxes.nth(textboxCount >= 3 ? 1 : 0)
    await target.fill('')
    await target.fill(USERNAME)
  }
  await fillFirstVisible(loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'), PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  expect(loginResponse.ok(), `login HTTP status ${loginResponse.status()}`).toBeTruthy()
  expect(isSuccessPayload(loginPayload), `login code ${loginPayload.code}: ${loginPayload.msg || ''}`).toBeTruthy()
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
}

async function fillFirstVisible(locator: Locator, value: string) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for value ${value}`)
}

function formItem(page: Page, label: string) {
  return page.locator('.el-form-item').filter({ hasText: label }).first()
}

async function fillFormInput(page: Page, label: string, value: string) {
  const target = formItem(page, label).locator('input').first()
  await target.waitFor({ state: 'visible', timeout: 30000 })
  await target.fill('')
  await target.fill(value)
}

async function fillFormTextarea(page: Page, label: string, value: string) {
  const target = formItem(page, label).locator('textarea').first()
  await target.waitFor({ state: 'visible', timeout: 30000 })
  await target.fill('')
  await target.fill(value)
}

test.beforeAll(() => {
  if (!CHROME_EXECUTABLE.toLowerCase().endsWith('chrome.exe')) {
    throw new Error('PROCESS_POOL_E2E_CHROME_EXECUTABLE must point to chrome.exe')
  }
  expectJson(REVIEW_SIGNATURE_SNAPSHOT, 'PROCESS_POOL_E2E_REVIEW_SIGNATURE_SNAPSHOT')
  expectJson(REVIEW_FIELD_MAPPINGS, 'PROCESS_POOL_E2E_REVIEW_FIELD_MAPPINGS')
  expectJson(REVISION_SIGNATURE_SNAPSHOT, 'PROCESS_POOL_E2E_REVISION_SIGNATURE_SNAPSHOT')
  expectJson(REVISION_AFTER_PAYLOAD, 'PROCESS_POOL_E2E_REVISION_AFTER_PAYLOAD')
  expectJson(REVISION_CHANGED_FIELDS, 'PROCESS_POOL_E2E_REVISION_CHANGED_FIELDS')
})

async function withRealPage(action: (page: Page) => Promise<void>) {
  const browser: Browser = await chromium.launch({
    executablePath: CHROME_EXECUTABLE,
    headless: true
  })
  const context = await browser.newContext()
  const page = await context.newPage()
  try {
    await action(page)
  } finally {
    await context.close()
    await browser.close()
  }
}

test('F5 generates and submits review copy from the independent page', async () => {
  await withRealPage(async (page) => {
    await login(page)
    await page.goto(`${BASE_URL}${REVIEW_PATH}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('.process-pool-write__title').filter({ hasText: '审核副本处理' })).toBeVisible()

    await fillFormInput(page, '工序池提交事件ID', REVIEW_EVENT_ID)
    await fillFormInput(page, '审核人用户ID', REVIEWER_USER_ID)
    await fillFormInput(page, '审核签名ID', REVIEW_SIGNATURE_ID)
    await fillFormInput(page, '签名员工用户ID', REVIEW_SIGNATURE_USER_ID)
    await fillFormTextarea(page, '审核签名快照JSON', REVIEW_SIGNATURE_SNAPSHOT)
    await fillFormTextarea(page, '字段上下限映射JSON', REVIEW_FIELD_MAPPINGS)

    const responsePromise = page.waitForResponse(
      (response) => response.url().includes(REVIEW_ENDPOINT) && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: '生成并提交审核副本' }).click()
    const response = await responsePromise
    const payload = await response.json()
    expect(response.ok(), `review copy HTTP status ${response.status()}`).toBeTruthy()
    expect(isSuccessPayload(payload), `review copy code ${payload.code}: ${payload.msg || ''}`).toBeTruthy()
    await expect(page.getByText('审核副本编号：')).toBeVisible()
  })
})

test('F6 updates original record from the independent page with a new signature', async () => {
  await withRealPage(async (page) => {
    await login(page)
    await page.goto(`${BASE_URL}${REVISION_PATH}`, { waitUntil: 'domcontentloaded' })
    await expect(page.locator('.process-pool-write__title').filter({ hasText: '原始记录修改' })).toBeVisible()

    await fillFormInput(page, '工序池提交事件ID', REVISION_EVENT_ID)
    await fillFormInput(page, '修改人用户ID', REVISION_MODIFIED_BY_USER_ID)
    await fillFormInput(page, '修改签名ID', REVISION_SIGNATURE_ID)
    await fillFormInput(page, '签名员工用户ID', REVISION_SIGNATURE_USER_ID)
    await fillFormInput(page, '变更原因', REVISION_CHANGE_REASON)
    await fillFormTextarea(page, '修改后payload JSON', REVISION_AFTER_PAYLOAD)
    await fillFormTextarea(page, '修改签名快照JSON', REVISION_SIGNATURE_SNAPSHOT)
    await fillFormTextarea(page, '字段变更JSON', REVISION_CHANGED_FIELDS)

    const responsePromise = page.waitForResponse(
      (response) => response.url().includes(REVISION_ENDPOINT) && response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: '修改并重新签名' }).click()
    const response = await responsePromise
    const payload = await response.json()
    expect(response.ok(), `event revision HTTP status ${response.status()}`).toBeTruthy()
    expect(isSuccessPayload(payload), `event revision code ${payload.code}: ${payload.msg || ''}`).toBeTruthy()
    await expect(page.getByText('原始记录修改编号：')).toBeVisible()
  })
})
