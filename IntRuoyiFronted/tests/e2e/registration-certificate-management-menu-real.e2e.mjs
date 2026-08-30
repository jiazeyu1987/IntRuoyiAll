import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const currentFile = fileURLToPath(import.meta.url)
const frontendRoot = path.resolve(path.dirname(currentFile), '..', '..')
const workspaceRoot = path.resolve(frontendRoot, '..')
const taskDir = path.join(workspaceRoot, 'doc', 'tasks', '20260829-registration-certificate-menu-hierarchy')
const outputDir = path.join(taskDir, 'e2e-output')

function parseEnvFile(filePath) {
  assert.ok(fs.existsSync(filePath), `Missing frontend env file: ${filePath}`)
  const entries = {}
  const content = fs.readFileSync(filePath, 'utf8')
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) {
      continue
    }
    const match = line.match(/^([^=]+?)=(.*)$/)
    if (!match) {
      continue
    }
    const key = match[1].trim()
    let value = match[2].trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    entries[key] = value
  }
  return entries
}

function browserExecutablePath() {
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    assert.ok(
      fs.existsSync(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH),
      `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH does not exist: ${process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH}`,
    )
    return process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }

  const candidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
  ]
  const executable = candidates.find((candidate) => fs.existsSync(candidate))
  assert.ok(executable, 'Chrome or Edge executable is required for real-path E2E verification')
  return executable
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function menuLabelPattern(text) {
  const compact = text.replace(/\s+/g, '')
  return new RegExp(`^\\s*${compact.split('').map(escapeRegExp).join('\\s*')}\\s*$`)
}

function visibleMenuEntries(page, label) {
  return page
    .locator('.el-menu-item:visible, .el-sub-menu__title:visible, [role="menuitem"]:visible')
    .filter({ hasText: menuLabelPattern(label) })
}

function visibleSubMenuTitles(page, label) {
  return page.locator('.el-sub-menu__title:visible').filter({ hasText: menuLabelPattern(label) })
}

async function expandMenuSection(page, sectionText, expectedChildText) {
  for (let attempt = 0; attempt < 5; attempt += 1) {
    if ((await visibleMenuEntries(page, expectedChildText).count()) > 0) {
      return
    }
    const sectionTitle = visibleSubMenuTitles(page, sectionText).first()
    await sectionTitle.waitFor({ state: 'visible', timeout: 60_000 })
    await sectionTitle.scrollIntoViewIfNeeded()
    const box = await sectionTitle.boundingBox()
    assert.ok(box, `${sectionText}菜单应有可点击区域`)
    await sectionTitle.click({ position: { x: Math.max(1, box.width - 16), y: box.height / 2 } })
    await page.waitForTimeout(500)
  }
  throw new Error(`${sectionText}菜单未展开出${expectedChildText}`)
}

async function selectTenant(page, form, tenantName) {
  const tenantInput = form.locator('input[placeholder="请输入租户名称"], .el-select input').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30_000 })
  await tenantInput.fill('')
  await tenantInput.fill(tenantName)
  const tenantOption = page
    .locator('.el-select-dropdown__item:visible')
    .filter({ hasText: tenantName })
    .first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30_000 })
  await tenantOption.click()
}

async function login(page, config) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60_000 })

  const form = page.locator('.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60_000 })
  await selectTenant(page, form, config.tenant)
  await form.locator('input[placeholder="请输入用户名"]').fill(config.username)
  await form.locator('input[placeholder="请输入密码"]').fill(config.password)

  const permissionInfoResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 90_000 },
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60_000 },
  )

  await form.getByRole('button', { name: '登录' }).click()

  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.status(), 200, `Login HTTP status should be 200, got ${loginResponse.status()}`)
  const loginPayload = await loginResponse.json()
  assert.equal(loginPayload.code, 0, `Login API should succeed, got code ${loginPayload.code}`)

  const permissionInfoResponse = await permissionInfoResponsePromise
  assert.equal(
    permissionInfoResponse.status(),
    200,
    `Permission info HTTP status should be 200, got ${permissionInfoResponse.status()}`,
  )
  const permissionInfoPayload = await permissionInfoResponse.json()
  assert.equal(
    permissionInfoPayload.code,
    0,
    `Permission info API should succeed, got code ${permissionInfoPayload.code}`,
  )

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60_000 })
  return permissionInfoPayload
}

async function waitForMenu(page) {
  await page.locator('.el-menu:visible').first().waitFor({ state: 'visible', timeout: 60_000 })
  await expandMenuSection(page, '基础数据', '注册证管理')
}

async function expandRegistrationCertificateMenu(page) {
  await expandMenuSection(page, '基础数据', '注册证管理')
  await expandMenuSection(page, '注册证管理', '关联公司')
  await visibleMenuEntries(page, '注册证').first().waitFor({ state: 'visible', timeout: 30_000 })
}

async function assertMenuShape(page) {
  await expandRegistrationCertificateMenu(page)
  assert.equal(await visibleMenuEntries(page, '关联公司').count(), 1, '关联公司 should be visible')
  assert.equal(await visibleMenuEntries(page, '注册证').count(), 1, '注册证 should be visible')
  assert.equal(
    await visibleMenuEntries(page, '企业公司范围').count(),
    0,
    '企业公司范围 should not be visible',
  )
  assert.equal(
    await visibleMenuEntries(page, '注册证历史导入').count(),
    0,
    '注册证历史导入 should not be visible',
  )
}

async function openMenuPage(page, label, pathFragment) {
  await expandRegistrationCertificateMenu(page)
  const item = visibleMenuEntries(page, label).first()
  await item.scrollIntoViewIfNeeded()
  await item.click()
  await page.waitForURL((url) => url.href.includes(pathFragment), { timeout: 60_000 })
  await page.locator('body').getByText(label, { exact: true }).first().waitFor({ state: 'visible', timeout: 60_000 })
}

async function collectMenuDiagnostics(page) {
  return page
    .locator('.el-menu-item, .el-sub-menu__title, [role="menuitem"]')
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
            height: Math.round(rect.height),
          },
        }
      }),
    )
}

async function main() {
  const env = parseEnvFile(path.join(frontendRoot, '.env'))
  const config = {
    baseUrl: process.env.E2E_BASE_URL || 'http://127.0.0.1:8081',
    tenant: process.env.E2E_TENANT || env.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: process.env.E2E_USERNAME || env.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: process.env.E2E_PASSWORD || env.VITE_APP_DEFAULT_LOGIN_PASSWORD,
  }
  assert.ok(config.tenant, 'VITE_APP_DEFAULT_LOGIN_TENANT is required')
  assert.ok(config.username, 'VITE_APP_DEFAULT_LOGIN_USERNAME is required')
  assert.ok(config.password, 'VITE_APP_DEFAULT_LOGIN_PASSWORD is required')

  fs.mkdirSync(outputDir, { recursive: true })
  const browser = await chromium.launch({
    executablePath: browserExecutablePath(),
    headless: true,
  })
  const context = await browser.newContext({
    baseURL: config.baseUrl,
    locale: 'zh-CN',
    viewport: { width: 1440, height: 960 },
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    const permissionInfoPayload = await login(page, config)
    await waitForMenu(page)
    await assertMenuShape(page)
    await openMenuPage(page, '关联公司', '/mdm/enterprise')
    await waitForMenu(page)
    await openMenuPage(page, '注册证', '/mdm/registration-certificate')
    await waitForMenu(page)
    await assertMenuShape(page)

    assert.deepEqual(pageErrors, [], `Unexpected page errors: ${pageErrors.join('\n')}`)

    const screenshotPath = path.join(outputDir, 'registration-certificate-management-menu.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    fs.writeFileSync(
      path.join(outputDir, 'registration-certificate-management-menu-result.json'),
      JSON.stringify(
        {
          checkedAt: new Date().toISOString(),
          baseUrl: config.baseUrl,
          tenant: config.tenant,
          username: config.username,
          parentMenu: '注册证管理',
          visibleChildren: ['关联公司', '注册证'],
          removedChildren: ['企业公司范围', '注册证历史导入'],
          currentUrl: page.url(),
          permissionInfoCode: permissionInfoPayload.code,
          screenshotPath,
        },
        null,
        2,
      ),
      'utf8',
    )
    console.log('PASS: 注册证管理菜单仅显示 关联公司、注册证，且两个保留页签均可通过真实菜单打开')
  } catch (error) {
    const diagnostics = await collectMenuDiagnostics(page)
    const failureScreenshotPath = path.join(outputDir, 'registration-certificate-management-menu-failure.png')
    await page.screenshot({ path: failureScreenshotPath, fullPage: true })
    fs.writeFileSync(
      path.join(outputDir, 'registration-certificate-management-menu-failure.json'),
      JSON.stringify(
        {
          checkedAt: new Date().toISOString(),
          error: error.message,
          visibleMenuEntries: diagnostics.filter((item) => item.visible).map((item) => item.text),
          hiddenMatchingEntries: diagnostics
            .filter((item) => !item.visible && item.text.includes('注册证'))
            .map((item) => item.text),
          screenshotPath: failureScreenshotPath,
        },
        null,
        2,
      ),
      'utf8',
    )
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
