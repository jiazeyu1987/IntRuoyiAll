const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const evidenceDir = path.join(
  repoRoot,
  'doc',
  'tasks',
  '20260829-invoice-voucher-print-assistant-launch-button'
)
const launchWaitingScreenshotPath = path.join(
  evidenceDir,
  'invoice-voucher-print-assistant-launch-waiting.png'
)
const launchRunningScreenshotPath = path.join(
  evidenceDir,
  'invoice-voucher-print-assistant-launch-running.png'
)
const launchReloadedScreenshotPath = path.join(
  evidenceDir,
  'invoice-voucher-print-assistant-launch-reloaded.png'
)
const baseUrl = process.env.INVOICE_VOUCHER_PRINT_E2E_BASE_URL || 'http://127.0.0.1:8081'
const targetPath = '/erp/finance/invoice-voucher-print'

function parseEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  return Object.fromEntries(
    fs
      .readFileSync(filePath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const index = line.indexOf('=')
        const key = line.slice(0, index).trim()
        const value = line
          .slice(index + 1)
          .trim()
          .replace(/^['"]|['"]$/g, '')
        return [key, value]
      })
  )
}

function readLoginDefaults() {
  const values = {
    ...parseEnvFile(path.join(frontendRoot, '.env')),
    ...parseEnvFile(path.join(frontendRoot, '.env.local')),
    ...process.env
  }
  return {
    ...values,
    actors: {
      admin: {
        label: 'admin',
        username: values.INVOICE_VOUCHER_PRINT_ADMIN_USERNAME ||
          values.VITE_APP_DEFAULT_LOGIN_USERNAME ||
          'admin',
        password: values.INVOICE_VOUCHER_PRINT_ADMIN_PASSWORD ||
          values.VITE_APP_DEFAULT_LOGIN_PASSWORD
      }
    }
  }
}

async function fillFirstVisible(locator, value, label) {
  if (!value) {
    return false
  }
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      const actual = await item.inputValue().catch(() => '')
      assert.equal(actual, value, `${label} input did not keep expected value`)
      return true
    }
  }
  return false
}

async function login(page, defaults, actor) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded'
  })
  const form = page.locator('.login-form').first()
  await form.waitFor({ state: 'visible', timeout: 15000 })

  const tenant = defaults.VITE_APP_DEFAULT_LOGIN_TENANT || ''
  const username = actor.username
  const password = actor.password

  await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    username,
    'username'
  )
  const filledPassword = await fillFirstVisible(
    form.locator('input[type="password"], input[placeholder="请输入密码"]'),
    password,
    'password'
  )
  assert.ok(filledPassword, 'default local login password is missing')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/get-permission-info'),
    { timeout: 60000 }
  ).catch((error) => error)
  await form.getByRole('button', { name: /登录/ }).click()
  const loginResponse = await loginResponsePromise

  const loginJson = await loginResponse.json().catch(() => null)
  assert.equal(loginResponse.status(), 200, 'login HTTP status must be 200')
  assert.equal(
    loginJson?.code,
    0,
    `login business code must be 0, got ${loginJson?.code}: ${loginJson?.msg || loginJson?.message || ''}`
  )

  const permissionResponse = await permissionResponsePromise
  if (permissionResponse instanceof Error) {
    throw permissionResponse
  }
  const permissionJson = await permissionResponse.json().catch(() => null)
  assert.equal(permissionResponse.status(), 200, 'permission HTTP status must be 200')
  assert.equal(permissionJson?.code, 0, `permission business code must be 0, got ${permissionJson?.code}`)
}

async function clickMenuLabel(page, label) {
  const menuRoot = page.locator('.el-menu').first()
  await menuRoot.waitFor({ state: 'visible', timeout: 15000 })

  for (let attempt = 0; attempt < 8; attempt += 1) {
    const candidate = page
      .locator('.el-menu-item, .el-sub-menu__title')
      .filter({ hasText: label })
      .first()
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.click()
      return
    }
    await page.evaluate(() => {
      const scrollables = Array.from(document.querySelectorAll('.el-scrollbar__wrap, aside, .left-menu'))
      for (const item of scrollables) {
        item.scrollTop = item.scrollTop + 260
      }
    })
    await page.waitForTimeout(200)
  }

  throw new Error(`menu label not visible: ${label}`)
}

async function createPage(browser, consoleErrors, pageErrors, actorLabel) {
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 } })
  const page = await context.newPage()

  page.on('console', (message) => {
    if (message.type() === 'error') {
      const text = message.text()
      if (!/favicon|ERR_CONNECTION_REFUSED|chrome-error:\/\//i.test(text)) {
        consoleErrors.push(`${actorLabel}: ${text}`)
      }
    }
  })
  page.on('pageerror', (error) => pageErrors.push(`${actorLabel}: ${error.message}`))
  return { context, page }
}

async function verifyLaunchFlow(browser, defaults, consoleErrors, pageErrors) {
  const { context, page } = await createPage(browser, consoleErrors, pageErrors, 'launch')
  try {
    await login(page, defaults, defaults.actors.admin)
    await page.waitForURL(/\/index|\/erp|\/dashboard|\/home/, { timeout: 20000 }).catch(() => {})
    await clickMenuLabel(page, 'ERP 系统')
    await clickMenuLabel(page, '财务管理')
    await clickMenuLabel(page, '发票凭证打印')
    await page.waitForURL((url) => url.pathname === targetPath, { timeout: 20000 })

    const startButton = page.getByRole('button', { name: '启动助手' })
    await startButton.waitFor({ state: 'visible', timeout: 30000 })
    const iframeBefore = await page.locator('iframe.invoice-voucher-print-frame').count()
    assert.equal(iframeBefore, 0, 'assistant iframe must not render before launch when assistant is offline')

    const waitingAlert = page.locator('.el-alert').filter({ hasText: '尚未启动' }).first()
    await waitingAlert.waitFor({ state: 'visible', timeout: 30000 })
    await page.screenshot({ path: launchWaitingScreenshotPath, fullPage: true })

    const startResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/invoice-voucher-print-assistant/start') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    const ticketResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/invoice-voucher-print-ticket') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await startButton.click()
    const startResponse = await startResponsePromise
    const startJson = await startResponse.json().catch(() => null)
    assert.equal(startResponse.status(), 200, 'assistant start HTTP status must be 200')
    assert.equal(startJson?.code, 0, `assistant start business code must be 0, got ${startJson?.code}`)
    assert.equal(startJson?.data?.running, true, 'assistant start response must report running=true')

    const ticketResponse = await ticketResponsePromise
    const ticketJson = await ticketResponse.json().catch(() => null)
    assert.equal(ticketResponse.status(), 200, 'ticket HTTP status must be 200')
    assert.equal(ticketJson?.code, 0, `ticket business code must be 0, got ${ticketJson?.code}`)
    assert.ok(ticketJson?.data?.ticket, 'ticket response must include a short lived ticket')

    const iframe = page.locator('iframe.invoice-voucher-print-frame').first()
    await iframe.waitFor({ state: 'visible', timeout: 30000 })
    const iframeHandle = await iframe.elementHandle()
    const frame = await iframeHandle.contentFrame()
    assert.ok(frame, 'invoice print iframe must expose a content frame')
    await frame.waitForLoadState('domcontentloaded', { timeout: 15000 })
    await frame.locator('body').waitFor({ state: 'visible', timeout: 15000 })
    const frameText = (await frame.locator('body').innerText()).replace(/\s+/g, ' ').trim()
    assert.match(frameText, /发票与对应凭证一键打印|一键查询并生成打印包/)
    assert.doesNotMatch(frameText, /分贝通凭证/)
    await page.screenshot({ path: launchRunningScreenshotPath, fullPage: true })

    await page.reload({ waitUntil: 'domcontentloaded' })
    await page.waitForURL((url) => url.pathname === targetPath, { timeout: 20000 })
    const iframeAfterReload = page.locator('iframe.invoice-voucher-print-frame').first()
    await iframeAfterReload.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(
      await page.getByRole('button', { name: '启动助手' }).count(),
      0,
      'assistant launch button must disappear after the assistant is running'
    )
    const iframeAfterReloadHandle = await iframeAfterReload.elementHandle()
    const frameAfterReload = await iframeAfterReloadHandle.contentFrame()
    assert.ok(frameAfterReload, 'assistant iframe must still render after reload')
    await frameAfterReload.waitForLoadState('domcontentloaded', { timeout: 15000 })
    await page.screenshot({ path: launchReloadedScreenshotPath, fullPage: true })

    return {
      usernameLabel: defaults.actors.admin.label,
      launched: true,
      runningAfterReload: true,
      screenshots: [
        launchWaitingScreenshotPath,
        launchRunningScreenshotPath,
        launchReloadedScreenshotPath
      ]
    }
  } finally {
    await context.close()
  }
}

async function main() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  const defaults = readLoginDefaults()
  const consoleErrors = []
  const pageErrors = []
  const browser = await chromium.launch({ headless: true })

  try {
    const launchResult = await verifyLaunchFlow(browser, defaults, consoleErrors, pageErrors)
    assert.deepEqual(consoleErrors, [])
    assert.deepEqual(pageErrors, [])

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenantLabel: defaults.VITE_APP_DEFAULT_LOGIN_TENANT || 'default-local-tenant',
          launchResult
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
