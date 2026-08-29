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
  '20260829-invoice-voucher-print-assistant-auth-gate'
)
const adminScreenshotPath = path.join(evidenceDir, 'invoice-voucher-print-admin-menu.png')
const unauthorizedScreenshotPath = path.join(
  evidenceDir,
  'invoice-voucher-print-unauthorized-menu.png'
)
const directAssistantScreenshotPath = path.join(
  evidenceDir,
  'invoice-voucher-print-assistant-direct-blocked.png'
)
const baseUrl = process.env.INVOICE_VOUCHER_PRINT_E2E_BASE_URL || 'http://127.0.0.1:8081'
const targetPermission = 'erp:invoice-voucher-print:query'
const targetPath = '/erp/finance/invoice-voucher-print'
const targetComponent = 'erp/finance/invoice-voucher-print/index'
const targetComponentName = 'ErpInvoiceVoucherPrint'

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
      },
      unauthorized: {
        label: 'aoteman',
        username: values.INVOICE_VOUCHER_PRINT_UNAUTHORIZED_USERNAME || 'aoteman',
        password: values.INVOICE_VOUCHER_PRINT_UNAUTHORIZED_PASSWORD ||
          values.VITE_APP_DEFAULT_LOGIN_PASSWORD
      }
    }
  }
}

function flattenMenus(menus, parentNames = []) {
  const rows = []
  for (const menu of menus || []) {
    const names = [...parentNames, menu.name].filter(Boolean)
    rows.push({
      id: menu.id,
      name: menu.name,
      path: menu.path,
      component: menu.component,
      componentName: menu.componentName,
      parentNames,
      names
    })
    rows.push(...flattenMenus(menu.children, names))
  }
  return rows
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
  return permissionJson.data
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

function findInvoiceMenu(permissionInfo) {
  return flattenMenus(permissionInfo?.menus || []).find(
    (menu) =>
      menu.name === '发票凭证打印' &&
      menu.path === 'invoice-voucher-print' &&
      menu.component === targetComponent &&
      menu.componentName === targetComponentName
  )
}

async function createPage(browser, consoleErrors, pageErrors, actorLabel) {
  const context = await browser.newContext({ viewport: { width: 1366, height: 768 } })
  const page = await context.newPage()

  page.on('console', (message) => {
    if (message.type() === 'error') {
      const text = message.text()
      if (!/favicon|ERR_CONNECTION_REFUSED|chrome-error:\/\//i.test(text)) {
        if (actorLabel === 'direct-assistant' && /403 \(Forbidden\)|status of 403/i.test(text)) {
          return
        }
        consoleErrors.push(`${actorLabel}: ${text}`)
      }
    }
  })
  page.on('pageerror', (error) => pageErrors.push(`${actorLabel}: ${error.message}`))
  return { context, page }
}

async function verifyAdmin(browser, defaults, consoleErrors, pageErrors) {
  const { context, page } = await createPage(browser, consoleErrors, pageErrors, 'admin')
  try {
    const permissionInfo = await login(page, defaults, defaults.actors.admin)
    const invoiceMenu = findInvoiceMenu(permissionInfo)

    assert.ok(
      permissionInfo?.permissions?.includes(targetPermission),
      `${targetPermission} must be returned for admin after finance print role grant`
    )
    assert.ok(invoiceMenu, 'invoice voucher print menu must be returned for admin')
    assert.deepEqual(invoiceMenu.names.slice(-3), ['ERP 系统', '财务管理', '发票凭证打印'])
    assert.equal(invoiceMenu.component, targetComponent)
    assert.equal(invoiceMenu.componentName, targetComponentName)

    await page.waitForURL(/\/index|\/erp|\/dashboard|\/home/, { timeout: 20000 }).catch(() => {})
    await clickMenuLabel(page, 'ERP 系统')
    await clickMenuLabel(page, '财务管理')
    const ticketResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/invoice-voucher-print-ticket') &&
        response.request().method() === 'POST',
      { timeout: 30000 }
    )
    await clickMenuLabel(page, '发票凭证打印')
    await page.waitForURL((url) => url.pathname === targetPath, { timeout: 20000 })
    const ticketResponse = await ticketResponsePromise
    const ticketJson = await ticketResponse.json().catch(() => null)
    assert.equal(ticketResponse.status(), 200, 'ticket HTTP status must be 200')
    assert.equal(ticketJson?.code, 0, `ticket business code must be 0, got ${ticketJson?.code}`)
    assert.ok(ticketJson?.data?.ticket, 'ticket response must include a short lived ticket')

    const iframe = page.locator('iframe.invoice-voucher-print-frame').first()
    const iframeCount = await page.locator('iframe.invoice-voucher-print-frame').count()
    const alertCount = await page.locator('.el-alert:has-text("发票凭证打印助手地址未配置")').count()
    assert.ok(iframeCount > 0 || alertCount > 0, 'invoice print page must render iframe or explicit config blocker')
    if (iframeCount > 0) {
      const assistantUrl = defaults.VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL || ''
      const iframeSrc = await iframe.getAttribute('src')
      assert.match(iframeSrc || '', /\/auth\/callback\?ticket=/, 'iframe must enter assistant through ticket callback')
      const iframeHandle = await iframe.elementHandle()
      const frame = await iframeHandle.contentFrame()
      assert.ok(frame, 'invoice print iframe must expose a content frame')
      await frame.waitForLoadState('domcontentloaded', { timeout: 15000 })
      if (assistantUrl) {
        assert.ok(
          frame.url().startsWith(assistantUrl.replace(/\/+$/, '')),
          `invoice print iframe must load configured assistant URL, got ${frame.url()}`
        )
      }
      await frame.locator('body').waitFor({ state: 'visible', timeout: 15000 })
      const frameText = (await frame.locator('body').innerText()).replace(/\s+/g, ' ').trim()
      assert.match(frameText, /发票与对应凭证一键打印|一键查询并生成打印包/)
      assert.doesNotMatch(frameText, /分贝通凭证/)
    }

    await page.screenshot({ path: adminScreenshotPath, fullPage: true })
    return {
      usernameLabel: defaults.actors.admin.label,
      permissionHasTarget: true,
      menuChain: invoiceMenu.names,
      finalUrl: page.url(),
      rendered: iframeCount > 0 ? 'iframe' : 'config-blocker-alert',
      ticketIssued: Boolean(ticketJson?.data?.ticket),
      screenshotPath: adminScreenshotPath
    }
  } finally {
    await context.close()
  }
}

async function verifyDirectAssistantAccess(browser, defaults, consoleErrors, pageErrors) {
  const assistantUrl = defaults.VITE_INVOICE_VOUCHER_PRINT_ASSISTANT_URL || ''
  assert.ok(assistantUrl, 'assistant URL must be configured before direct access E2E')
  const { context, page } = await createPage(browser, consoleErrors, pageErrors, 'direct-assistant')
  try {
    const response = await page.goto(assistantUrl, { waitUntil: 'domcontentloaded', timeout: 30000 })
    assert.equal(response?.status(), 403, 'direct assistant access without ERP ticket must return HTTP 403')
    const bodyText = (await page.locator('body').innerText()).replace(/\s+/g, ' ').trim()
    assert.match(bodyText, /无权访问发票凭证打印助手|没有发票凭证打印权限/)
    assert.doesNotMatch(bodyText, /一键查询并生成打印包|发票与对应凭证一键打印/)
    await page.screenshot({ path: directAssistantScreenshotPath, fullPage: true })
    return {
      assistantUrl,
      status: response?.status(),
      blocked: true,
      screenshotPath: directAssistantScreenshotPath
    }
  } finally {
    await context.close()
  }
}

async function verifyUnauthorized(browser, defaults, consoleErrors, pageErrors) {
  const { context, page } = await createPage(browser, consoleErrors, pageErrors, 'unauthorized')
  try {
    const permissionInfo = await login(page, defaults, defaults.actors.unauthorized)
    const invoiceMenu = findInvoiceMenu(permissionInfo)

    assert.ok(
      !permissionInfo?.permissions?.includes(targetPermission),
      `${targetPermission} must not be returned for user without finance print role`
    )
    assert.equal(invoiceMenu, undefined, 'invoice voucher print menu must not be returned for unauthorized user')
    await page.waitForURL(/\/index|\/erp|\/dashboard|\/home/, { timeout: 20000 }).catch(() => {})
    await clickMenuLabel(page, 'ERP 系统').catch(() => {})
    await clickMenuLabel(page, '财务管理').catch(() => {})
    const visibleInvoiceEntry = await page
      .locator('.el-menu-item:visible, .el-sub-menu__title:visible')
      .filter({ hasText: '发票凭证打印' })
      .count()
    assert.equal(visibleInvoiceEntry, 0, 'unauthorized sidebar must not show invoice voucher print entry')
    await page.screenshot({ path: unauthorizedScreenshotPath, fullPage: true })
    return {
      usernameLabel: defaults.actors.unauthorized.label,
      permissionHasTarget: false,
      menuVisible: false,
      screenshotPath: unauthorizedScreenshotPath
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
    const directAssistantResult = await verifyDirectAssistantAccess(browser, defaults, consoleErrors, pageErrors)
    const adminResult = await verifyAdmin(browser, defaults, consoleErrors, pageErrors)
    const unauthorizedResult = await verifyUnauthorized(browser, defaults, consoleErrors, pageErrors)
    assert.deepEqual(consoleErrors, [])
    assert.deepEqual(pageErrors, [])

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenantLabel: defaults.VITE_APP_DEFAULT_LOGIN_TENANT || 'default-local-tenant',
          targetPermission,
          directAssistant: directAssistantResult,
          admin: adminResult,
          unauthorized: unauthorizedResult
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
