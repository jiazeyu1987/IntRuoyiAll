const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const moduleApi = require('node:module')

const TASK_ROOT = __dirname
const WORKSPACE_ROOT = path.resolve(TASK_ROOT, '../../..')
const requireFromFrontend = moduleApi.createRequire(
  path.join(WORKSPACE_ROOT, 'IntRuoyiFronted', 'package.json')
)
const { chromium } = requireFromFrontend('playwright')
const OUTPUT_DIR = path.join(TASK_ROOT, 'artifacts')
const RESULT_PATH = path.join(OUTPUT_DIR, 'team-leader-multi-dialogs-real-result.json')
const CORRECTION_SCREENSHOT = path.join(OUTPUT_DIR, 'production-report-correction-dialog.png')
const CORRECTION_MATERIALS_SCREENSHOT = path.join(OUTPUT_DIR, 'production-report-correction-materials.png')
const CORRECTION_PARAMETERS_SCREENSHOT = path.join(OUTPUT_DIR, 'production-report-correction-parameters.png')
const ALLOCATION_SCREENSHOT = path.join(OUTPUT_DIR, 'production-report-allocation-dialog.png')
const FAILURE_SCREENSHOT = path.join(OUTPUT_DIR, 'team-leader-multi-dialogs-real-failure.png')

const FRONTEND_URL = process.env.TLW_FRONTEND_URL || 'http://127.0.0.1:8092'
const TARGET_PATH = process.env.TLW_TARGET_PATH || '/mes/pro/process-pool/production-leader'
const FRONTEND_ENV_PATH = path.join(WORKSPACE_ROOT, 'IntRuoyiFronted', '.env')

function readFrontendEnvValue(name) {
  if (!fs.existsSync(FRONTEND_ENV_PATH)) return undefined
  const source = fs.readFileSync(FRONTEND_ENV_PATH, 'utf8')
  const match = source.match(new RegExp(`^\\s*${name}\\s*=\\s*(.*?)\\s*$`, 'm'))
  if (!match) return undefined
  return match[1].replace(/^['"]|['"]$/g, '').trim()
}

const TENANT = process.env.TLW_TENANT || readFrontendEnvValue('VITE_APP_DEFAULT_LOGIN_TENANT')
const USERNAME = process.env.TLW_USERNAME
const PASSWORD = process.env.TLW_PASSWORD

function requireEnv(name, value) {
  assert.ok(value && String(value).trim(), `${name} is required for real frontend E2E`)
  return String(value).trim()
}

function resolveChromiumExecutable() {
  for (const candidate of [
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe'
  ]) {
    if (candidate && fs.existsSync(candidate)) return candidate
  }
  return undefined
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) {
      await candidate.fill(value)
      return
    }
  }
  throw new Error(`${label} input is not visible`)
}

async function login(page, config) {
  await page.goto(`${config.frontendUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(
    form.locator('input[placeholder="请输入用户名"], input.el-input__inner:not([type="password"]):not([role="combobox"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const response = await responsePromise
  const body = await response.json()
  assert.equal(response.ok(), true, `login HTTP ${response.status()}`)
  assert.ok([0, 200].includes(body.code), `login business code ${body.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function firstVisible(locator, label) {
  await locator.first().waitFor({ state: 'visible', timeout: 30000 })
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const candidate = locator.nth(index)
    if (await candidate.isVisible().catch(() => false)) return candidate
  }
  throw new Error(`${label} is not visible`)
}

async function findProductionEvent(page) {
  const rows = page.locator('[data-team-leader-report-workbench] .el-table__body-wrapper tbody tr')
  await rows.first().waitFor({ state: 'visible', timeout: 60000 })
  const rowCount = await rows.count()
  for (let index = 0; index < rowCount; index += 1) {
    const row = rows.nth(index)
    const correctionButton = row.locator('[data-team-leader-correction-event-id]').first()
    const allocationButton = row.locator('[data-production-report-allocation-event-id]').first()
    if (
      await correctionButton.isVisible().catch(() => false) &&
      await allocationButton.isVisible().catch(() => false)
    ) {
      return {
        rowIndex: index,
        correctionEventId: await correctionButton.getAttribute('data-team-leader-correction-event-id'),
        allocationEventId: await allocationButton.getAttribute('data-production-report-allocation-event-id')
      }
    }
  }
  throw new Error('no visible production submission row has both correction and allocation actions')
}

async function assertNonEmptyText(locator, label) {
  await locator.waitFor({ state: 'attached', timeout: 30000 })
  await locator.scrollIntoViewIfNeeded({ timeout: 30000 }).catch(() => {})
  const text = (await locator.innerText()).replace(/\s+/g, ' ').trim()
  assert.ok(text, `${label} is empty`)
  return text
}

async function run() {
  const config = {
    tenant: requireEnv('TLW_TENANT', TENANT),
    username: requireEnv('TLW_USERNAME', USERNAME),
    password: requireEnv('TLW_PASSWORD', PASSWORD),
    frontendUrl: FRONTEND_URL
  }
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const result = {
    status: 'RUNNING',
    frontendUrl: config.frontendUrl,
    targetPath: TARGET_PATH,
    tenant: config.tenant,
    username: config.username,
    workspaceRoot: WORKSPACE_ROOT,
    event: null,
    correction: null,
    allocation: null,
    screenshots: {},
    mesWriteRequests: [],
    pageErrors: [],
    consoleErrors: []
  }

  const browser = await chromium.launch({
    headless: process.env.TLW_E2E_HEADED !== '1',
    executablePath: resolveChromiumExecutable()
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (url.pathname.startsWith('/admin-api/mes/') && ['POST', 'PUT', 'DELETE'].includes(request.method())) {
      result.mesWriteRequests.push({ method: request.method(), path: url.pathname })
    }
  })
  page.on('pageerror', (error) => result.pageErrors.push(error.message))
  page.on('console', (message) => {
    if (message.type() === 'error') result.consoleErrors.push(message.text())
  })

  try {
    const response = await fetch(config.frontendUrl)
    assert.equal(response.ok, true, `frontend HTTP ${response.status}`)
    await login(page, config)
    await page.goto(`${config.frontendUrl}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.locator('[data-team-leader-report-workbench]').waitFor({ state: 'visible', timeout: 60000 })
    const event = await findProductionEvent(page)
    result.event = event

    await page.locator(`[data-team-leader-correction-event-id="${event.correctionEventId}"]`).click()
    const correctionDialog = await firstVisible(page.locator('[data-production-report-correction-dialog]'), 'correction dialog')
    const correctionMaterialText = await assertNonEmptyText(
      correctionDialog.locator('[data-production-report-correction-materials]'),
      'correction material section'
    )
    const correctionDeviceText = await assertNonEmptyText(
      correctionDialog.locator('[data-production-report-correction-devices]'),
      'correction device section'
    )
    await correctionDialog.locator('[data-production-report-correction-materials]').screenshot({
      path: CORRECTION_MATERIALS_SCREENSHOT
    })
    const correctionParameterSection = correctionDialog
      .locator('.team-leader-workbench__correction-section')
      .filter({ hasText: '设备参数' })
      .first()
    const correctionParameterText = await assertNonEmptyText(
      correctionParameterSection,
      'correction parameter section'
    )
    await correctionDialog.screenshot({ path: CORRECTION_SCREENSHOT })
    await correctionParameterSection.screenshot({ path: CORRECTION_PARAMETERS_SCREENSHOT })
    result.correction = {
      materialText: correctionMaterialText,
      deviceText: correctionDeviceText,
      parameterText: correctionParameterText
    }
    result.screenshots.correction = CORRECTION_SCREENSHOT
    result.screenshots.correctionMaterials = CORRECTION_MATERIALS_SCREENSHOT
    result.screenshots.correctionParameters = CORRECTION_PARAMETERS_SCREENSHOT
    await correctionDialog.getByRole('button', { name: '取消' }).click()
    await correctionDialog.waitFor({ state: 'hidden', timeout: 30000 })

    await page.locator(`[data-production-report-allocation-event-id="${event.allocationEventId}"]`).click()
    const allocationDialog = await firstVisible(page.getByRole('dialog'), 'allocation dialog')
    const allocationMaterialText = await assertNonEmptyText(
      allocationDialog.locator('[data-team-leader-allocation-material-context]'),
      'allocation material context'
    )
    const allocationDeviceText = await assertNonEmptyText(
      allocationDialog.locator('[data-team-leader-allocation-devices]'),
      'allocation device context'
    )
    const allocationParameterText = await assertNonEmptyText(
      allocationDialog.locator('[data-team-leader-allocation-parameters]'),
      'allocation parameter context'
    )
    await allocationDialog.screenshot({ path: ALLOCATION_SCREENSHOT })
    result.allocation = {
      materialText: allocationMaterialText,
      deviceText: allocationDeviceText,
      parameterText: allocationParameterText
    }
    result.screenshots.allocation = ALLOCATION_SCREENSHOT
    assert.equal(result.mesWriteRequests.length, 0, 'read-only dialog E2E sent MES write requests')
    assert.equal(result.pageErrors.length, 0, 'page errors occurred during target flow')
    result.status = 'PASS'
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    console.log(`PASS: correction/allocation dialogs verified for event=${event.correctionEventId}`)
  } catch (error) {
    result.status = 'FAIL'
    result.error = { name: error.name || 'Error', message: error.message || String(error) }
    try {
      await page.screenshot({ path: FAILURE_SCREENSHOT, fullPage: true })
      result.screenshots.failure = FAILURE_SCREENSHOT
    } catch {}
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(`FAIL: team leader multi material/device dialogs real E2E ${error.message}`)
  process.exitCode = 1
})
