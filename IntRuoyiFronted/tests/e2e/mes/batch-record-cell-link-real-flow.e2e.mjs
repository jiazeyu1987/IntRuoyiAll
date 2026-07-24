import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { chromium } from 'playwright'

const zh = {
  tenant: '测试租户',
  login: '登录',
  tenantPlaceholder: '请输入租户名称',
  usernamePlaceholder: '请输入用户名',
  passwordPlaceholder: '请输入密码',
  pageTitle: '批记录单元格链接'
}

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_E2E_BASE_URL || 'http://127.0.0.1:8098').replace(/\/+$/, ''),
  backendUrl: (process.env.BATCH_RECORD_CELL_LINK_E2E_BACKEND_URL || 'http://127.0.0.1:48098').replace(/\/+$/, ''),
  tenant: process.env.BATCH_RECORD_CELL_LINK_E2E_TENANT || zh.tenant,
  username: process.env.BATCH_RECORD_CELL_LINK_E2E_USERNAME || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_LINK_E2E_PASSWORD || '111111',
  sourceReportId:
    process.env.BATCH_RECORD_CELL_LINK_E2E_SOURCE_REPORT_ID ||
    'c37d43d73f484c77a8dc5b19c4c5bd86',
  headed: process.env.BATCH_RECORD_CELL_LINK_E2E_HEADED === '1',
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_E2E_TASK_DIR ||
    'D:/ProjectPackage/Int/IntRuoyi/doc/tasks/20260711-batch-record-cross-form-cell-link-implementation/e2e-artifacts'
}

const screenshots = {
  loginFailed: path.join(config.taskDir, 'batch-record-cell-link-login-failed.png'),
  pageFailed: path.join(config.taskDir, 'batch-record-cell-link-page-failed.png'),
  saved: path.join(config.taskDir, 'batch-record-cell-link-saved.png')
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

function extractData(payload) {
  assert.ok(isSuccessPayload(payload), `api payload failed: ${JSON.stringify(payload)}`)
  return payload.data
}

function assertExpectedApiOrigin(response, label) {
  const responseUrl = response.url()
  const allowedOrigins = [config.backendUrl, config.baseUrl]
  assert.ok(
    allowedOrigins.some((origin) => responseUrl.startsWith(origin)),
    `${label} request did not hit target backend or configured frontend proxy: ${responseUrl}`
  )
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`visible_input_missing:${label}`)
}

async function selectTenant(page, loginForm) {
  const tenantInput = loginForm
    .locator(`.el-select input[role="combobox"], input.el-select__input, input[placeholder="${zh.tenantPlaceholder}"]`)
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponsePromise
}

async function getPageProbe(page) {
  return page.evaluate(() => ({
    url: window.location.href,
    body: (document.body.innerText || '').slice(0, 1500),
    selectableSourceCells: document.querySelectorAll(
      '.batch-record-cell-link__pane.is-source .batch-record-cell-link-sheet__cell.is-source-selectable'
    ).length,
    selectableTargetCells: document.querySelectorAll(
      '.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable'
    ).length,
    buttons: Array.from(document.querySelectorAll('button')).map((button) => ({
      text: (button.textContent || '').replace(/\s+/g, ' ').trim(),
      disabled: button.disabled,
      visible: Boolean(button.offsetParent)
    }))
  }))
}

async function login(page) {
  const targetPath = `/mes/pro/batch-record-cell-link?sourceReportId=${encodeURIComponent(config.sourceReportId)}`
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, loginForm)

  const textboxes = loginForm.getByRole('textbox')
  const textboxCount = await textboxes.count()
  if (textboxCount >= 2) {
    const usernameInput = textboxes.nth(textboxCount >= 3 ? 1 : 0)
    await usernameInput.fill('')
    await usernameInput.fill(config.username)
  } else {
    await fillFirstVisible(loginForm.locator(`input[placeholder="${zh.usernamePlaceholder}"]`), config.username, 'username')
  }
  await fillFirstVisible(
    loginForm.locator(`input[type="password"], input[placeholder="${zh.passwordPlaceholder}"]`),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const loginButton = loginForm.getByRole('button', { name: zh.login }).first()
  await loginButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await loginButton.isEnabled(), true, 'login button must be enabled')
  await loginButton.click()

  let loginResponse
  try {
    loginResponse = await loginResponsePromise
  } catch (error) {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    const probe = await getPageProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`login_response_timeout:${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.loginFailed}`)
  }
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openWorkbench(page) {
  const targetPath = `/mes/pro/batch-record-cell-link?sourceReportId=${encodeURIComponent(config.sourceReportId)}`
  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}${targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(zh.pageTitle).first().waitFor({ state: 'visible', timeout: 60000 })
  const contextResponse = await contextPromise
  assertExpectedApiOrigin(contextResponse, 'workbench')
  const contextPayload = await contextResponse.json()
  const context = extractData(contextPayload)
  assert.ok(Array.isArray(context.forms) && context.forms.length >= 2, `need at least two real forms: ${JSON.stringify(context)}`)
  await settle(page, 30000)
  return context
}

async function clickFirstSelectable(page, selector, label) {
  const locator = page.locator(selector)
  await locator.first().waitFor({ state: 'visible', timeout: 60000 })
  const count = await locator.count()
  assert.ok(count > 0, `selectable_${label}_cell_missing`)
  await locator.first().click()
  await settle(page)
}

async function createAndSaveRule(page) {
  await clickFirstSelectable(
    page,
    '.batch-record-cell-link__pane.is-source .batch-record-cell-link-sheet__cell.is-source-selectable:not(.is-linked)',
    'source'
  )
  await clickFirstSelectable(
    page,
    '.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable:not(.is-linked)',
    'target'
  )

  const createButton = page.getByRole('button', { name: '建立链接' }).first()
  await createButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await createButton.isEnabled(), true, 'create link button must be enabled after selecting source and target')
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/rules/save') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await createButton.click()
  const saveResponse = await saveResponsePromise
  assertExpectedApiOrigin(saveResponse, 'save')
  assert.equal(saveResponse.ok(), true, `save response must be HTTP OK: ${saveResponse.status()}`)
  await page.getByText('单元格链接已建立并保存', { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 30000
  })

  const sourceLinkCountButton = page.locator('.batch-record-cell-link__source-link-count').first()
  await sourceLinkCountButton.waitFor({ state: 'visible', timeout: 30000 })
  const sourceLinkCountText = (await sourceLinkCountButton.textContent()) || ''
  const sourceLinkCount = Number(sourceLinkCountText.match(/\d+/)?.[0] || 0)
  assert.ok(sourceLinkCount >= 1, `source link count must update after creating a rule: ${sourceLinkCountText}`)
  const result = { savedCount: sourceLinkCount, rules: new Array(sourceLinkCount), ruleVersion: null }

  await sourceLinkCountButton.click()
  await page.getByText('源表单链接详情').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('.batch-record-cell-link__detail-dialog .el-table__body-wrapper tbody tr').first().waitFor({
    state: 'visible',
    timeout: 30000
  })
  await page.keyboard.press('Escape')
  await page.getByText('源表单链接详情').first().waitFor({ state: 'hidden', timeout: 30000 }).catch(() => null)

  assert.ok(Number(result.savedCount) >= 1, `savedCount must be positive: ${JSON.stringify(result)}`)
  assert.ok(Array.isArray(result.rules) && result.rules.length >= 1, `saved rules missing: ${JSON.stringify(result)}`)
  await page.screenshot({ path: screenshots.saved, fullPage: true }).catch(() => null)
  return result
}

async function main() {
  if (config.tenant !== zh.tenant || config.username !== 'aoteman') {
    throw new Error(`batch_record_cell_link_e2e_must_use_test_tenant_aoteman:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`)
  }
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } })
  try {
    await login(page)
    const context = await openWorkbench(page)
    const result = await createAndSaveRule(page)
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          forms: context.forms.length,
          savedCount: result.savedCount,
          ruleVersion: result.ruleVersion,
          screenshot: screenshots.saved
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: screenshots.pageFailed, fullPage: true }).catch(() => null)
    const probe = await getPageProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.pageFailed}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
