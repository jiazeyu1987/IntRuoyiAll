const DEFAULT_TENANT_NAME = '测试租户'
const DEFAULT_USERNAME = 'aoteman'
const DEFAULT_PASSWORD = 'admin123'
const REQUIRED_BASE_URL_MESSAGE = 'RUNTIME_CONTROL_E2E_BASE_URL is required'
const REQUIRED_ACTION_ORIGIN_MESSAGE = 'RUNTIME_CONTROL_E2E_ACTION_ORIGIN is required'

function requireTrimmedEnv(name, requiredMessage, guidance) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    throw new Error(`${requiredMessage}; ${guidance}`)
  }
  return value.trim().replace(/\/+$/, '')
}

function getRuntimeControlBaseUrl() {
  return requireTrimmedEnv(
    'RUNTIME_CONTROL_E2E_BASE_URL',
    REQUIRED_BASE_URL_MESSAGE,
    'use the current worktree frontend URL for runtime-control E2E evidence.'
  )
}

function getRuntimeControlActionOrigin() {
  return requireTrimmedEnv(
    'RUNTIME_CONTROL_E2E_ACTION_ORIGIN',
    REQUIRED_ACTION_ORIGIN_MESSAGE,
    'use the current-code backend origin that will receive runtime-control actions.'
  )
}

function assertRuntimeControlTestAccountBoundary(label = 'runtime-control E2E') {
  const tenantName = process.env.RUNTIME_CONTROL_E2E_TENANT || DEFAULT_TENANT_NAME
  const username = process.env.RUNTIME_CONTROL_E2E_USERNAME || DEFAULT_USERNAME
  if (tenantName !== DEFAULT_TENANT_NAME) {
    throw new Error(`${label} must use ${DEFAULT_TENANT_NAME}, got ${tenantName}`)
  }
  if (username !== DEFAULT_USERNAME) {
    throw new Error(`${label} must use ${DEFAULT_USERNAME}, got ${username}`)
  }
}

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for runtime-control E2E tests. Run with a Node environment that can resolve the 'playwright' package."
    )
  }
}

async function fillFirstVisible(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  throw new Error(`No visible input found for locator: ${locator}`)
}

async function fillFirstVisibleIfPresent(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return true
    }
  }
  return false
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    return false
  }
  await tenantSelect.click()
  const input = page.locator('.login-form .el-select__input').first()
  await input.fill(tenantName)
  await page.keyboard.press('Enter')
  return true
}

async function loginRuntimeControl(page, baseUrl = getRuntimeControlBaseUrl()) {
  const tenantName = process.env.RUNTIME_CONTROL_E2E_TENANT || DEFAULT_TENANT_NAME
  const username = process.env.RUNTIME_CONTROL_E2E_USERNAME || DEFAULT_USERNAME
  const password = process.env.RUNTIME_CONTROL_E2E_PASSWORD || DEFAULT_PASSWORD
  await page.goto(`${baseUrl}/login?redirect=/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded'
  })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)

  if (page.url().includes('/login')) {
    const tenantSelected = await selectTenant(page, tenantName)
    if (!tenantSelected) {
      await fillFirstVisibleIfPresent(page.locator('input[placeholder="请输入租户名称"]'), tenantName)
    }
    await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), username)
    await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), password)
    await page.locator('button:has-text("登录")').first().click()
    await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  }

  if (!page.url().includes('/infra/monitors/runtime-control')) {
    await page.goto(`${baseUrl}/infra/monitors/runtime-control`, { waitUntil: 'domcontentloaded' })
  }
  await page.waitForSelector('text=运行控制台', { timeout: 30000 })
  await page.locator('button:has-text("部署发布包到测试服")').waitFor({ state: 'visible', timeout: 30000 })
}

function watchOperationRequests(page) {
  const requests = []
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/infra/runtime-control/actions')) {
      requests.push(request.postData() || '')
    }
  })
  return requests
}

async function openOperationDialog(page, buttonText) {
  await page.locator(`button:has-text("${buttonText}")`).first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: buttonText }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  return dialog
}

async function fillDialogReason(dialog, reason) {
  await dialog.locator('textarea').first().fill(reason)
}

async function fillProdConfirm(dialog) {
  await dialog.locator('input[placeholder="输入 PROD"]').fill('PROD')
}

async function assertNoOperationRequest(requests, page, label) {
  await page.waitForTimeout(600)
  if (requests.length !== 0) {
    throw new Error(`${label} should not submit an operation request, got ${requests.length}`)
  }
}

async function runRuntimeControlE2E(name, testBody) {
  const baseUrl = getRuntimeControlBaseUrl()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } })
  const page = await context.newPage()
  try {
    await loginRuntimeControl(page, baseUrl)
    const requests = watchOperationRequests(page)
    await testBody({ page, requests })
    console.log(`PASS: ${name}`)
  } finally {
    await browser.close()
  }
}

module.exports = {
  assertNoOperationRequest,
  assertRuntimeControlTestAccountBoundary,
  fillDialogReason,
  fillProdConfirm,
  getRuntimeControlActionOrigin,
  getRuntimeControlBaseUrl,
  openOperationDialog,
  runRuntimeControlE2E
}
