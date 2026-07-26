import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const DEFAULT_TASK_DIR = path.join(
  repoRoot,
  'doc',
  'tasks',
  '20260726-work-order-field-cell-link',
  'e2e-artifacts'
)

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_BASE_URL || 'http://127.0.0.1:8085').replace(/\/+$/, ''),
  taskDir: process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_TASK_DIR || DEFAULT_TASK_DIR,
  headed: process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_HEADED === '1',
  timeout: Number(process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_TIMEOUT || 90000)
}

const screenshots = {
  failed: path.join(config.taskDir, 'work-order-field-readonly-failed.png'),
  passed: path.join(config.taskDir, 'work-order-field-readonly-passed.png')
}

function readEnvDefaults() {
  const envPath = path.join(frontendRoot, '.env')
  assert.ok(fs.existsSync(envPath), `frontend .env missing: ${envPath}`)
  const entries = Object.fromEntries(
    fs.readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const [key, ...rest] = line.split('=')
        return [key.trim(), rest.join('=').trim().replace(/^['"]|['"]$/g, '')]
      })
  )
  const tenant = process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_TENANT || entries.VITE_APP_DEFAULT_LOGIN_TENANT
  const username = process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_USERNAME || entries.VITE_APP_DEFAULT_LOGIN_USERNAME
  const password = process.env.BATCH_RECORD_CELL_LINK_WORK_ORDER_PASSWORD || entries.VITE_APP_DEFAULT_LOGIN_PASSWORD
  assert.ok(tenant, 'missing default login tenant')
  assert.ok(username, 'missing default login username')
  assert.ok(password, 'missing default login password')
  return { tenant, username, password }
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function getPageProbe(page) {
  return page.evaluate(() => ({
    url: window.location.href,
    body: (document.body.innerText || '').slice(0, 2000),
    sourceSelectCount: document.querySelectorAll('.batch-record-cell-link__source-select').length,
    workOrderPanelCount: document.querySelectorAll('.batch-record-cell-link__work-order-field-panel').length,
    sourceSelectableCount: document.querySelectorAll(
      '.batch-record-cell-link__work-order-field-panel .batch-record-cell-link-sheet__cell.is-source-selectable'
    ).length,
    mesWriteRequestsVisibleHint: 'captured separately'
  }))
}

async function selectTenant(page, loginForm, tenant) {
  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await loginForm.locator('input[placeholder="请输入租户名称"]').first().fill(tenant)
}

async function login(page, credentials) {
  const targetPath = '/mes/pro/batch-record-form-list'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await settle(page)
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('form.login-form:visible, .login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, loginForm, credentials.tenant)
  await loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible').first()
    .fill(credentials.username)
  await loginForm.locator('input[type="password"]:visible').first().fill(credentials.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginPayload = await (await loginResponsePromise).json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${loginPayload?.msg || loginPayload?.code || 'unknown'}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: config.timeout })
}

async function openWorkbenchFromFormList(page) {
  await page.goto(`${config.baseUrl}/mes/pro/batch-record-form-list`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.getByText('批记录表单').first().waitFor({ state: 'visible', timeout: config.timeout })
  await settle(page, 30000)

  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const cellLinkEntry = page
    .locator('.batch-record-form-preview__actions button')
    .filter({ hasText: /^链接$/ })
    .first()
  await cellLinkEntry.waitFor({ state: 'visible', timeout: config.timeout })
  await cellLinkEntry.click()
  await page.getByText('批记录单元格链接').first().waitFor({ state: 'visible', timeout: config.timeout })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${JSON.stringify(contextPayload)}`)
  assert.ok(Array.isArray(contextPayload.data?.forms), 'workbench context must return forms')
  assert.ok(Array.isArray(contextPayload.data?.sourceFields), 'workbench context must return sourceFields')
  assert.ok(contextPayload.data.sourceFields.length >= 2, 'work order source fields must not be empty')
  await settle(page, 30000)
  return contextPayload.data
}

async function switchToWorkOrderSource(page) {
  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  await sourceSelect.waitFor({ state: 'visible', timeout: config.timeout })
  await sourceSelect.click()
  const dropdownItems = page.locator('.el-select-dropdown__item:visible')
  await dropdownItems.filter({ hasText: /^生产工单$/ }).first().waitFor({ state: 'visible', timeout: 30000 })
  await dropdownItems.filter({ hasText: /^生产工单$/ }).first().click()
  await page.locator('.batch-record-cell-link__work-order-field-panel').first().waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page, 30000)
}

async function assertWorkOrderFieldSource(page, context) {
  const expectedNames = ['生产工单编号', '生产数量']
  for (const name of expectedNames) {
    assert.ok(
      context.sourceFields.some((field) => field.fieldName === name),
      `workbench sourceFields missing ${name}: ${JSON.stringify(context.sourceFields)}`
    )
  }

  const probe = await page.evaluate(() => {
    const sourcePanel = document.querySelector('.batch-record-cell-link__work-order-field-panel')
    const cells = Array.from(
      sourcePanel?.querySelectorAll('.batch-record-cell-link-sheet__cell.is-source-selectable') || []
    ).map((cell) => (cell.textContent || '').replace(/\s+/g, ' ').trim())
    const selectedCells = Array.from(sourcePanel?.querySelectorAll('.batch-record-cell-link-sheet__cell.is-selected') || [])
      .map((cell) => (cell.textContent || '').replace(/\s+/g, ' ').trim())
    const sourceSelectText = (
      document.querySelector('.batch-record-cell-link__controls .batch-record-cell-link__source-select')?.textContent || ''
    ).replace(/\s+/g, ' ').trim()
    const targetSelectableCount = document.querySelectorAll(
      '.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable'
    ).length
    const sourceLinkCountText = (
      document.querySelector('.batch-record-cell-link__source-link-count')?.textContent || ''
    ).replace(/\s+/g, ' ').trim()
    const panelTitle = (
      document.querySelector('.batch-record-cell-link__work-order-field-panel .batch-record-cell-link__pane-title')?.textContent || ''
    ).replace(/\s+/g, ' ').trim()
    return {
      cells,
      selectedCells,
      sourceSelectText,
      targetSelectableCount,
      sourceLinkCountText,
      panelTitle
    }
  })

  assert.ok(probe.panelTitle.includes('源字段'), `source panel must switch to field mode: ${JSON.stringify(probe)}`)
  assert.ok(probe.panelTitle.includes('生产工单'), `source panel title must show production work order: ${JSON.stringify(probe)}`)
  for (const name of expectedNames) {
    assert.ok(probe.cells.includes(name), `source field matrix missing ${name}: ${JSON.stringify(probe)}`)
  }
  assert.ok(probe.selectedCells.length >= 1, `a work order source field should be selected: ${JSON.stringify(probe)}`)
  assert.ok(probe.targetSelectableCount > 0, `target form must remain selectable: ${JSON.stringify(probe)}`)
  assert.match(probe.sourceLinkCountText, /\d+\s*个链接/, `link count must stay visible: ${JSON.stringify(probe)}`)

  const quantityCell = page
    .locator('.batch-record-cell-link__work-order-field-panel .batch-record-cell-link-sheet__cell.is-source-selectable')
    .filter({ hasText: '生产数量' })
    .first()
  await quantityCell.waitFor({ state: 'visible', timeout: 30000 })
  await quantityCell.click()
  await settle(page)
  const selectedQuantity = await page.evaluate(() =>
    Array.from(
      document.querySelectorAll('.batch-record-cell-link__work-order-field-panel .batch-record-cell-link-sheet__cell.is-selected')
    )
      .map((cell) => (cell.textContent || '').replace(/\s+/g, ' ').trim())
      .includes('生产数量')
  )
  assert.equal(selectedQuantity, true, 'clicking 生产数量 must select it as the left source field')

  const targetCell = page
    .locator('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable:not(.is-linked)')
    .first()
  await targetCell.waitFor({ state: 'visible', timeout: 30000 })
  await targetCell.click()
  await settle(page)
  const targetSelected = await page.evaluate(
    () =>
      document.querySelectorAll('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-selected')
        .length > 0
  )
  assert.equal(targetSelected, true, 'clicking a target cell must select it for a work order source link')
  const createButton = page.locator('.batch-record-cell-link__create-button').first()
  await createButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await createButton.isEnabled(), true, 'create link button must be enabled after choosing work order source and target')
}

async function main() {
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'timeout must be positive')
  fs.mkdirSync(config.taskDir, { recursive: true })
  const credentials = readEnvDefaults()
  assert.equal(credentials.tenant, '芋道源码', `readonly E2E tenant must be 芋道源码, got ${credentials.tenant}`)
  assert.equal(credentials.username, 'admin', `readonly E2E username must be admin, got ${credentials.username}`)

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(request.method())) {
      mesWriteRequests.push(`${request.method()} ${request.url()}`)
    }
  })

  try {
    await login(page, credentials)
    const context = await openWorkbenchFromFormList(page)
    await switchToWorkOrderSource(page)
    await assertWorkOrderFieldSource(page, context)
    assert.equal(mesWriteRequests.length, 0, `readonly E2E must not send MES writes: ${mesWriteRequests.join(', ')}`)
    await page.screenshot({ path: screenshots.passed, fullPage: true }).catch(() => null)
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          baseUrl: config.baseUrl,
          identity: `${credentials.tenant}/${credentials.username}`,
          forms: context.forms.length,
          sourceFields: context.sourceFields.map((field) => ({
            fieldCode: field.fieldCode,
            fieldName: field.fieldName,
            sourceType: field.sourceType
          })),
          mesWriteRequests: mesWriteRequests.length,
          screenshot: screenshots.passed
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: screenshots.failed, fullPage: true }).catch(() => null)
    const probe = await getPageProbe(page).catch((probeError) => ({ probeError: probeError.message }))
    throw new Error(`${error.message}; probe=${JSON.stringify(probe)}; screenshot=${screenshots.failed}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
