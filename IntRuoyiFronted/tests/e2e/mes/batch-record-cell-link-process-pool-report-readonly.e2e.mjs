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

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_REPORT_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_REPORT_TASK_DIR ||
    path.join(repoRoot, 'doc', 'tasks', '20260811-process-pool-report-cell-link-config', 'e2e-artifacts'),
  timeout: Number(process.env.BATCH_RECORD_CELL_LINK_REPORT_TIMEOUT || 90000),
  headed: process.env.BATCH_RECORD_CELL_LINK_REPORT_HEADED === '1'
}

function readLoginDefaults() {
  const envPath = path.join(frontendRoot, '.env')
  assert.ok(fs.existsSync(envPath), `frontend .env missing: ${envPath}`)
  const entries = Object.fromEntries(
    fs
      .readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const [key, ...rest] = line.split('=')
        return [key.trim(), rest.join('=').trim().replace(/^['"]|['"]$/g, '')]
      })
  )
  const credentials = {
    tenant: entries.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: entries.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: entries.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(credentials.tenant && credentials.username && credentials.password, 'default login values are incomplete')
  return credentials
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
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

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(credentials.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: credentials.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await form.locator('input[placeholder="请输入租户名称"]').first().fill(credentials.tenant)
  }
  await form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill(credentials.username)
  await form.locator('input[type="password"]:visible').first().fill(credentials.password)

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && isSuccessPayload(payload), `login failed: ${payload?.msg || response.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

async function openWorkbench(page) {
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
  const linkButton = page.locator('.batch-record-form-preview__actions button').filter({ hasText: /^链接$/ }).first()
  await linkButton.waitFor({ state: 'visible', timeout: config.timeout })
  await linkButton.click()
  await page.getByText('批记录单元格链接').first().waitFor({ state: 'visible', timeout: config.timeout })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${contextPayload?.msg || contextPayload?.code}`)
  await settle(page, 30000)
  return contextPayload.data
}

async function verifyProcessPoolReportSource(page, context) {
  const expectedFields = [
    ['allocatedQuantity', '放行分配数量'],
    ['outputQuantity', '本次报工产出数量'],
    ['lossQuantity', '本次报工损耗数量']
  ]
  const reportFields = (context.sourceFields || []).filter((field) => field.sourceType === 'PROCESS_POOL_REPORT')
  for (const [fieldCode, fieldName] of expectedFields) {
    assert.ok(
      reportFields.some((field) => field.fieldCode === fieldCode && field.fieldName === fieldName),
      `PROCESS_POOL_REPORT source field missing: ${fieldCode}/${fieldName}`
    )
  }

  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  await sourceSelect.click()
  const reportOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /^报工数据$/ }).first()
  await reportOption.waitFor({ state: 'visible', timeout: 30000 })
  await reportOption.click()
  await settle(page)

  const sourcePanel = page.locator('.batch-record-cell-link__work-order-field-panel').first()
  await sourcePanel.waitFor({ state: 'visible', timeout: config.timeout })
  const panelText = (await sourcePanel.innerText()).replace(/\s+/g, ' ')
  const targetForm = (context.forms || []).find((form) => form.reportId === context.defaultTargetReportId)
  assert.ok(targetForm, `default target form missing: ${JSON.stringify(context)}`)
  assert.match(panelText, /源字段/, 'source panel must identify source fields')
  assert.ok(
    panelText.includes(`${targetForm.reportName}的一线生产字段`),
    `source panel must identify the selected target process: ${panelText}`
  )
  for (const [, fieldName] of expectedFields) {
    assert.ok(panelText.includes(fieldName), `report data field is not visible: ${fieldName}`)
  }

  const aggregationSelect = page.locator('.batch-record-cell-link__aggregation-select').first()
  await aggregationSelect.waitFor({ state: 'visible', timeout: 30000 })
  const createButton = page.locator('.batch-record-cell-link__create-button').first()
  assert.equal(await createButton.isDisabled(), true, 'create button must remain disabled before target and aggregation')

  const targetCell = page
    .locator('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable')
    .first()
  await targetCell.waitFor({ state: 'visible', timeout: 30000 })
  await targetCell.click()
  assert.equal(await createButton.isDisabled(), true, 'create button must require an aggregation strategy')

  await aggregationSelect.click()
  const visibleOptions = page.locator('.el-select-dropdown__item:visible')
  await visibleOptions.filter({ hasText: /^求和$/ }).first().waitFor({ state: 'visible', timeout: 30000 })
  const aggregationLabels = await visibleOptions.allInnerTexts()
  for (const label of ['求和', '第一笔', '最后一笔', '最小值', '最大值']) {
    assert.ok(
      aggregationLabels.some((item) => item.trim() === label),
      `number aggregation option missing: ${label}; visible=${JSON.stringify(aggregationLabels)}`
    )
  }
  await visibleOptions.filter({ hasText: /^求和$/ }).first().click()
  await settle(page)
  assert.equal(await createButton.isEnabled(), true, 'create button must be enabled after source, target and aggregation')

  return {
    forms: context.forms?.length || 0,
    reportFields: reportFields.map(({ fieldCode, fieldName, valueType }) => ({ fieldCode, fieldName, valueType })),
    aggregationLabels: aggregationLabels.map((label) => label.trim()).filter(Boolean)
  }
}

async function main() {
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'timeout must be positive')
  fs.mkdirSync(config.taskDir, { recursive: true })
  const credentials = readLoginDefaults()
  assert.equal(credentials.tenant, '芋道源码', `readonly tenant must be 芋道源码, got ${credentials.tenant}`)
  assert.equal(credentials.username, 'admin', `readonly username must be admin, got ${credentials.username}`)

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(request.method())) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  const screenshot = path.join(config.taskDir, 'process-pool-report-readonly-passed.png')
  const failedScreenshot = path.join(config.taskDir, 'process-pool-report-readonly-failed.png')
  try {
    await login(page, credentials)
    const context = await openWorkbench(page)
    const evidence = await verifyProcessPoolReportSource(page, context)
    assert.equal(mesWriteRequests.length, 0, `readonly E2E sent MES writes: ${mesWriteRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join(' | ')}`)
    await page.screenshot({ path: screenshot, fullPage: true })
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          identity: `${credentials.tenant}/${credentials.username}`,
          ...evidence,
          mesWriteRequests: mesWriteRequests.length,
          pageErrors,
          screenshot
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: failedScreenshot, fullPage: true }).catch(() => null)
    throw new Error(`${error.message}; screenshot=${failedScreenshot}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
