const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const FRONTEND_ROOT = path.resolve(__dirname, '../..')
const WORKSPACE_ROOT = path.resolve(FRONTEND_ROOT, '..')
const BASE_URL = (process.env.EDHR_FRONTLINE_PQC_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
  /\/+$/,
  ''
)
const TARGET_PATH = '/mes/pro/feedback/edhr-batch-pqc-fill'
const RESULT_DIR = path.resolve(WORKSPACE_ROOT, 'output', 'playwright', '20260805-frontline-pqc-tab')
const RESULT_PATH = path.join(RESULT_DIR, 'edhr-frontline-pqc-menu-real-e2e.json')
const SCREENSHOT_PATH = path.join(RESULT_DIR, 'edhr-frontline-pqc-menu-real-e2e.png')

function parseEnvValue(value) {
  const trimmed = (value || '').trim()
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1)
  }
  return trimmed
}

function readEnvFile(filePath) {
  if (!fs.existsSync(filePath)) {
    return {}
  }
  const env = {}
  for (const line of fs.readFileSync(filePath, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) {
      continue
    }
    const equalsIndex = trimmed.indexOf('=')
    if (equalsIndex <= 0) {
      continue
    }
    env[trimmed.slice(0, equalsIndex).trim()] = parseEnvValue(trimmed.slice(equalsIndex + 1))
  }
  return env
}

function collectLoginConfig() {
  const env = {
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env')),
    ...readEnvFile(path.join(FRONTEND_ROOT, '.env.local')),
    ...process.env
  }
  return {
    tenant: env.EDHR_FRONTLINE_PQC_E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: env.EDHR_FRONTLINE_PQC_E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: env.EDHR_FRONTLINE_PQC_E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
}

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function menuLabelPattern(text) {
  const compact = text.replace(/\s+/g, '')
  return new RegExp(`^\\s*${compact.split('').map(escapeRegExp).join('\\s*')}\\s*$`)
}

async function login(page, config) {
  assert.ok(config.tenant, 'local default tenant is required')
  assert.ok(config.username, 'local default username is required')
  assert.ok(config.password, 'local default password is required')

  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: config.tenant
    }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])')
    .first()
    .fill(config.username)
  await form.locator('input[type="password"], input[placeholder="请输入密码"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/get-permission-info') && response.status() === 200,
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  const permissionResponse = await permissionResponsePromise
  const permissionPayload = await permissionResponse.json()
  assert.ok([0, 200].includes(permissionPayload.code), `permission business code ${permissionPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
  return permissionPayload
}

function visibleMenuItem(page, text) {
  return page
    .locator('.el-menu-item:visible, .el-sub-menu__title:visible, [role="menuitem"]:visible')
    .filter({ hasText: menuLabelPattern(text) })
    .first()
}

function visibleSubMenuTitle(page, text) {
  return page.locator('.el-sub-menu__title:visible').filter({ hasText: menuLabelPattern(text) }).first()
}

async function isVisibleMenuItem(page, text) {
  return visibleMenuItem(page, text)
    .isVisible({ timeout: 1000 })
    .catch(() => false)
}

async function expandMenuSection(page, sectionText, expectedChildText) {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    if (await isVisibleMenuItem(page, expectedChildText)) {
      return
    }
    const sectionTitle = visibleSubMenuTitle(page, sectionText)
    await sectionTitle.waitFor({ state: 'visible', timeout: 60000 })
    await sectionTitle.scrollIntoViewIfNeeded()
    const box = await sectionTitle.boundingBox()
    assert.ok(box, `${sectionText} menu title must have a visible bounding box`)
    await sectionTitle.click({ position: { x: Math.max(1, box.width - 16), y: box.height / 2 } })
    await page.waitForTimeout(700)
    if (await isVisibleMenuItem(page, expectedChildText)) {
      return
    }
    await sectionTitle.focus()
    await page.keyboard.press('Enter')
    await page.waitForTimeout(700)
  }
  throw new Error(`${sectionText} menu did not expose ${expectedChildText}`)
}

async function openEdhrMenu(page) {
  await expandMenuSection(page, 'MES系统', 'eDHR批记录')
  await expandMenuSection(page, 'eDHR批记录', '一线PQC')
}

async function collectMenuDiagnostics(page) {
  return page
    .locator('.el-menu-item, .el-sub-menu__title')
    .evaluateAll((elements) =>
      elements.slice(0, 240).map((element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        return {
          text: (element.textContent || '').replace(/\s+/g, ' ').trim(),
          visible:
            rect.width > 0 &&
            rect.height > 0 &&
            style.display !== 'none' &&
            style.visibility !== 'hidden',
          rect: {
            x: Math.round(rect.x),
            y: Math.round(rect.y),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
          }
        }
      })
    )
}

function containsMenuText(permissionPayload, text) {
  return JSON.stringify(permissionPayload?.data || {}).includes(text)
}

async function main() {
  const config = collectLoginConfig()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const consoleErrors = []
  const pageErrors = []
  const writeRequests = []
  let captureWrites = false
  let page

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('console', (message) => {
      if (message.type() === 'error') {
        consoleErrors.push(message.text())
      }
    })
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!captureWrites) {
        return
      }
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/')) {
        writeRequests.push({ method: request.method(), url: new URL(request.url()).pathname })
      }
    })

    const permissionPayload = await login(page, config)
    assert.equal(
      containsMenuText(permissionPayload, '一线PQC'),
      true,
      'admin permission response must include 一线PQC dynamic menu'
    )
    captureWrites = true

    await page.goto(`${BASE_URL}/index`, { waitUntil: 'domcontentloaded' })
    await openEdhrMenu(page)

    const frontlinePqcMenu = visibleMenuItem(page, '一线PQC')
    await frontlinePqcMenu.waitFor({ state: 'visible' })
    await frontlinePqcMenu.click()
    await page.waitForURL((current) => current.pathname === TARGET_PATH, {
      timeout: 60000,
      waitUntil: 'domcontentloaded'
    })

    await page.locator('[data-edhr-frontline-pqc-page-title]').first().waitFor({ state: 'visible' })
    await page.getByText('一线PQC', { exact: true }).first().waitFor({ state: 'visible' })
    await page.locator('[data-frontline-pqc-operator]').first().waitFor({ state: 'visible' })

    const internalTabCount = await page.locator('[data-edhr-batch-record-tabs]').count()
    assert.equal(internalTabCount, 0, 'standalone 一线PQC page must not render batch execution internal tabs')
    const legacyPqcTabCount = await page
      .locator('[data-edhr-batch-record-tabs] .el-tabs__item')
      .filter({ hasText: 'PQC填写' })
      .count()
    assert.equal(legacyPqcTabCount, 0, 'PQC填写 must not remain as an internal batch execution tab')

    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })

    assert.deepEqual(writeRequests, [], 'admin 一线PQC visibility check must stay read-only after login')
    assert.deepEqual(consoleErrors, [], 'admin 一线PQC visibility check must not emit console errors')
    assert.deepEqual(pageErrors, [], 'admin 一线PQC visibility check must not emit page errors')

    const result = {
      ok: true,
      baseUrl: BASE_URL,
      actor: `${config.tenant}/${config.username}`,
      targetPath: TARGET_PATH,
      permissionMenuVisible: true,
      pageMenuVisible: true,
      internalTabCount,
      legacyPqcTabCount,
      writeRequests,
      consoleErrors,
      pageErrors,
      screenshotPath: SCREENSHOT_PATH
    }
    writeResult(result)
    console.log(`PASS eDHR frontline PQC admin menu real E2E ${JSON.stringify(result)}`)
  } catch (error) {
    const menuDiagnostics = page ? await collectMenuDiagnostics(page).catch(() => []) : []
    writeResult({
      ok: false,
      baseUrl: BASE_URL,
      actor: config.tenant && config.username ? `${config.tenant}/${config.username}` : 'missing-local-default-login',
      targetPath: TARGET_PATH,
      currentUrl: page ? page.url() : undefined,
      error: error.message,
      menuDiagnostics,
      writeRequests,
      consoleErrors,
      pageErrors
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
