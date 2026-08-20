import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')

const config = {
  baseUrl: (process.env.BATCH_RECORD_PICK_LIST_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  routeId: Number(process.env.BATCH_RECORD_PICK_LIST_ROUTE_ID || 922119),
  routeProcessId: Number(process.env.BATCH_RECORD_PICK_LIST_ROUTE_PROCESS_ID || 9908090160),
  timeout: Number(process.env.BATCH_RECORD_PICK_LIST_TIMEOUT || 90000),
  headed: process.env.BATCH_RECORD_PICK_LIST_HEADED === '1',
  taskDir:
    process.env.BATCH_RECORD_PICK_LIST_TASK_DIR ||
    path.join(repoRoot, 'doc', 'tasks', '20260817-batch-record-pick-list-process-material-fix', 'e2e-artifacts')
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
        return [key.trim(), rest.join('=').trim().replace(/^["']|["']$/g, '')]
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
  const targetPath = `/mes/pro/route/edit/${config.routeId}?tab=process&routeProcessId=${config.routeProcessId}`
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

async function openExactProcessLink(page) {
  const routeUrl =
    `${config.baseUrl}/mes/pro/route/edit/${config.routeId}` +
    `?tab=process&routeProcessId=${config.routeProcessId}`
  await page.goto(routeUrl, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.locator('.route-flow-graph-designer').waitFor({ state: 'visible', timeout: config.timeout })
  await page
    .locator(`[data-flow-node="route-process"][data-route-process-id="${config.routeProcessId}"]`)
    .waitFor({ state: 'visible', timeout: config.timeout })

  const batchRecordField = page.locator('[data-flow-detail-field="batchRecordFormNames"]').first()
  await batchRecordField.waitFor({ state: 'visible', timeout: config.timeout })
  await batchRecordField.locator('[data-flow-action="select-process-detail-field"]').click()

  const reportLink = page
    .locator(
      '[data-flow-action="open-process-detail-link"]' +
        '[data-flow-detail-link-field="batchRecordFormNames"]'
    )
    .first()
  await reportLink.waitFor({ state: 'visible', timeout: config.timeout })
  const reportName = (await reportLink.innerText()).trim()
  assert.ok(reportName, 'current process batch record report link is empty')
  await reportLink.click()
  await page.waitForURL((url) => url.pathname.includes('/mes/pro/batch-record-form-list'), {
    timeout: config.timeout
  })
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
  return { context: contextPayload.data, reportName }
}

async function verifyProductionPickListSource(page, context) {
  const pickListFields = (context.sourceFields || []).filter(
    (field) => field.sourceType === 'PRODUCTION_PICK_LIST' && Number(field.routeProcessId) === config.routeProcessId
  )
  assert.ok(pickListFields.length > 0, 'current process has no production pick-list source fields')
  assert.ok(
    pickListFields.every((field) => /^materialCode\.[A-Za-z0-9_-]+\.[A-Za-z]+$/.test(field.fieldCode)),
    `production pick-list fields must use stable material codes: ${JSON.stringify(pickListFields.slice(0, 3))}`
  )

  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  await sourceSelect.click()
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /^领料单数据$/ }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await settle(page)

  const sourcePanel = page.locator('[data-production-pick-list-source-fields="true"]').first()
  await sourcePanel.waitFor({ state: 'visible', timeout: config.timeout })
  const declaredCount = Number(await sourcePanel.getAttribute('data-production-pick-list-field-count'))
  const visibleCount = await sourcePanel.locator('.batch-record-cell-link-sheet__cell.is-source-selectable').count()
  assert.equal(declaredCount, pickListFields.length, 'declared production pick-list field count is inconsistent')
  assert.ok(visibleCount > 0, 'production pick-list source fields are not visible')

  return {
    declaredCount,
    visibleCount,
    sampleFields: pickListFields.slice(0, 3).map(({ fieldCode, fieldName }) => ({ fieldCode, fieldName }))
  }
}

async function main() {
  assert.ok(Number.isSafeInteger(config.routeId) && config.routeId > 0, 'routeId must be a positive safe integer')
  assert.ok(
    Number.isSafeInteger(config.routeProcessId) && config.routeProcessId > 0,
    'routeProcessId must be a positive safe integer'
  )
  fs.mkdirSync(config.taskDir, { recursive: true })
  const credentials = readLoginDefaults()
  assert.equal(credentials.tenant, '芋道源码', `readonly tenant must be 芋道源码, got ${credentials.tenant}`)
  assert.equal(credentials.username, 'admin', `readonly username must be admin, got ${credentials.username}`)

  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(request.method())) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  const screenshot = path.join(config.taskDir, 'production-pick-list-readonly-passed.png')
  const failedScreenshot = path.join(config.taskDir, 'production-pick-list-readonly-failed.png')
  try {
    await login(page, credentials)
    const { context, reportName } = await openExactProcessLink(page)
    const evidence = await verifyProductionPickListSource(page, context)
    assert.equal(mesWriteRequests.length, 0, `readonly E2E sent MES writes: ${mesWriteRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join(' | ')}`)
    await page.screenshot({ path: screenshot, fullPage: true })
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          identity: `${credentials.tenant}/${credentials.username}`,
          routeId: config.routeId,
          routeProcessId: config.routeProcessId,
          reportName,
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
