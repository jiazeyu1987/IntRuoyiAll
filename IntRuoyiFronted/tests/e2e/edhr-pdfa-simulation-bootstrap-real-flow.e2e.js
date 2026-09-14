const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

function envValue(key) {
  return (process.env[key] || '').trim()
}

const BASE_URL = envValue('EDHR_ARCHIVE_TASK_E2E_BASE_URL') || 'http://localhost:8081'
const TENANT = envValue('EDHR_ARCHIVE_TASK_E2E_TENANT') || '测试租户'
const USERNAME = envValue('EDHR_ARCHIVE_TASK_E2E_ARCHIVER_USERNAME') || 'aoteman'
const MANAGER_USERNAME = envValue('EDHR_ARCHIVE_TASK_E2E_MANAGER_USERNAME') || 'xujianhai'
const STAGE5_SIMULATION_SIGNOFF_STORAGE_KEY = 'mes:stage5-final-release:signoff-evidence-hash'
const SOURCE_BATCH_EXECUTION_ID =
  envValue('EDHR_ARCHIVE_TASK_E2E_SOURCE_BATCH_EXECUTION_ID') || '900000000708'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'edhr-pdfa-simulation-bootstrap')

function requiredEnv(key) {
  const value = (process.env[key] || '').trim()
  if (!value) throw new Error(`Missing required environment variable: ${key}`)
  return value
}

function ensureDir() {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
}

async function fillVisible(root, selector, value, label) {
  const inputs = root.locator(selector)
  const count = await inputs.count()
  for (let index = 0; index < count; index += 1) {
    const input = inputs.nth(index)
    if (await input.isVisible()) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`Cannot find visible ${label} input`)
}

async function clickEnabled(root, name, label) {
  const deadline = Date.now() + 60000
  let visibleButtonNames = []
  while (Date.now() < deadline) {
    const buttons = root.getByRole('button', { name })
    const count = await buttons.count()
    for (let index = 0; index < count; index += 1) {
      const button = buttons.nth(index)
      if ((await button.isVisible()) && !(await button.isDisabled())) {
        await button.click()
        return
      }
    }
    visibleButtonNames = await root.getByRole('button').evaluateAll((items) =>
      items
        .filter((item) => {
          const rect = item.getBoundingClientRect()
          return rect.width > 0 && rect.height > 0 && getComputedStyle(item).visibility !== 'hidden'
        })
        .map((item) => item.textContent?.replace(/\s+/g, ' ').trim() || item.getAttribute('aria-label') || '<empty>')
        .filter(Boolean)
    )
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Cannot find enabled ${label} button. Visible buttons: ${visibleButtonNames.join(' / ')}`)
}

async function selectTenant(page) {
  const loginForm = page.locator('.login-form:visible').first()
  const tenantSelect = loginForm.locator('.el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) return
  await tenantSelect.click()
  const tenantInput = tenantSelect.locator('input').first()
  await tenantInput.fill(TENANT)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function login(page, username, password) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  if (!page.url().includes('/login')) return
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('Captcha is enabled; real E2E bootstrap cannot continue.')
  }
  await selectTenant(page)
  await fillVisible(loginForm, 'input[placeholder="请输入用户名"], input[placeholder="请输入账号"]', username, 'username')
  await fillVisible(loginForm, 'input[placeholder="请输入密码"]', password, 'password')
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickEnabled(loginForm, /^登录$/, 'login')
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.equal(loginResponse.status(), 200, `Login HTTP status must be 200, got ${loginResponse.status()}.`)
  assert.equal(loginBody.code, 0, `Login must succeed for ${username}: ${loginBody.msg || ''}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function switchLoggedInUser(page, context, username, password, preservedLocalStorageItems = []) {
  await page.evaluate((items) => {
    const preserved = new Map(items.map((item) => [item.key, item.value]))
    window.localStorage.clear()
    window.sessionStorage.clear()
    for (const [key, value] of preserved.entries()) {
      if (typeof value === 'string' && value.trim()) window.localStorage.setItem(key, value)
    }
  }, preservedLocalStorageItems)
  await context.clearCookies()
  await login(page, username, password)
}

async function readAuthHeaders(page) {
  return page.evaluate(() => {
    function unwrap(raw) {
      if (!raw) return ''
      try {
        const parsed = JSON.parse(raw)
        if (parsed && typeof parsed === 'object' && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          try {
            return JSON.parse(parsed.v)
          } catch {
            return parsed.v
          }
        }
        return parsed
      } catch {
        return raw
      }
    }
    const accessToken = unwrap(window.localStorage.getItem('ACCESS_TOKEN') || window.sessionStorage.getItem('ACCESS_TOKEN'))
    const tenantId = unwrap(window.localStorage.getItem('tenantId') || window.sessionStorage.getItem('tenantId'))
    const headers = { Accept: 'application/json' }
    if (accessToken) headers.Authorization = String(accessToken).startsWith('Bearer ') ? String(accessToken) : `Bearer ${accessToken}`
    if (tenantId) headers['tenant-id'] = String(tenantId)
    return headers
  })
}

async function getJson(page, url) {
  const headers = await readAuthHeaders(page)
  assert.ok(headers.Authorization, 'Missing authenticated Authorization header.')
  assert.ok(headers['tenant-id'], 'Missing authenticated tenant header.')
  return page.evaluate(
    async ({ targetUrl, requestHeaders }) => {
      const response = await fetch(targetUrl, { credentials: 'include', headers: requestHeaders })
      if (!response.ok) throw new Error(`${targetUrl} HTTP ${response.status}`)
      const json = await response.json()
      if (json?.code !== undefined && json.code !== 0) throw new Error(`${targetUrl} business ${json.code}: ${json.msg || ''}`)
      return json.data ?? json
    },
    { targetUrl: url, requestHeaders: headers }
  )
}

async function setTaskTypeFilter(page, label) {
  const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
  const select = toolbar.locator('.el-select').first()
  await select.click()
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: label }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function waitForWorkTaskPageIdle(page) {
  await page.locator('.edhr-work-task-page .el-table').first().waitFor({ state: 'visible', timeout: 60000 })
  await page
    .waitForFunction(
      () => !document.querySelector('.edhr-work-task-page .el-loading-mask'),
      undefined,
      { timeout: 60000 }
    )
    .catch(() => undefined)
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => undefined)
}

async function findVisibleTaskRowByTexts(page, texts, timeout = 60000) {
  const needles = texts.map((item) => String(item || '').trim()).filter(Boolean)
  const rows = page.locator('.edhr-work-task-page .el-table__body-wrapper tr')
  const deadline = Date.now() + timeout
  let lastVisibleText = ''
  while (Date.now() < deadline) {
    const count = await rows.count().catch(() => 0)
    for (let index = 0; index < count; index += 1) {
      const row = rows.nth(index)
      if (!(await row.isVisible().catch(() => false))) continue
      const text = (await row.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      if (text) lastVisibleText = text
      if (needles.every((needle) => text.includes(needle))) {
        return row
      }
    }
    await page.waitForTimeout(500)
  }
  throw new Error(`Cannot find visible work-task row: ${needles.join(' / ')}. Last visible row: ${lastVisibleText || '<empty>'}`)
}

async function clickWorkTaskRowAction(page, row, name, label) {
  const directButtons = row.getByRole('button', { name })
  const directCount = await directButtons.count()
  for (let index = 0; index < directCount; index += 1) {
    const button = directButtons.nth(index)
    if ((await button.isVisible()) && !(await button.isDisabled())) {
      await button.click()
      return
    }
  }

  const rowHandle = await row.elementHandle()
  if (!rowHandle) throw new Error(`Cannot resolve table row for ${label}.`)
  const rows = page.locator('.edhr-work-task-page .el-table__body-wrapper tr')
  const rowCount = await rows.count()
  let rowIndex = -1
  for (let index = 0; index < rowCount; index += 1) {
    const sameRow = await rows.nth(index).evaluate((element, target) => element === target, rowHandle)
    if (sameRow) {
      rowIndex = index
      break
    }
  }
  await rowHandle.dispose()
  if (rowIndex < 0) throw new Error(`Cannot match fixed action row for ${label}.`)

  const fixedRows = page.locator('.edhr-work-task-page .el-table__fixed-right .el-table__body-wrapper tr')
  if ((await fixedRows.count()) > rowIndex) {
    await clickEnabled(fixedRows.nth(rowIndex), name, label)
    return
  }
  await clickEnabled(page.locator('.edhr-work-task-page').first(), name, label)
}

async function readManagerReleaseDialogDiagnostic(dialog, releaseRequests, browserMessages) {
  return dialog.evaluate(
    (element, data) => {
      const buttons = Array.from(element.querySelectorAll('button')).map((button) => ({
        text: button.textContent?.replace(/\s+/g, ' ').trim(),
        disabled: button.disabled,
        className: button.className
      }))
      const validation = Array.from(element.querySelectorAll('.el-form-item__error'))
        .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
      const alerts = Array.from(element.querySelectorAll('.el-alert__title, .el-alert__description'))
        .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
      const descriptions = Array.from(element.querySelectorAll('.el-descriptions__cell'))
        .map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
        .filter(Boolean)
      const signoffInput = Array.from(element.querySelectorAll('.el-form-item'))
        .find((item) => item.textContent?.includes('签核证据'))
        ?.querySelector('input')
      return {
        signoffValueLength: signoffInput?.value?.length || 0,
        validation,
        alerts,
        descriptions,
        buttons,
        releaseRequests: data.releaseRequests,
        browserMessages: data.browserMessages
      }
    },
    {
      releaseRequests,
      browserMessages: browserMessages.slice(-8)
    }
  )
}

async function waitForManagerReleaseDialogReady(page, dialog, releaseRequests, browserMessages) {
  const confirmButton = dialog
    .locator('.el-dialog__footer button')
    .filter({ hasText: '确认最终放行' })
    .first()
  await confirmButton.waitFor({ state: 'visible', timeout: 60000 })
  const deadline = Date.now() + 60000
  while (Date.now() < deadline) {
    await page.waitForFunction(
      () =>
        Array.from(document.querySelectorAll('.el-dialog .el-loading-mask')).every((element) => {
          const rect = element.getBoundingClientRect()
          return rect.width === 0 || rect.height === 0 || getComputedStyle(element).visibility === 'hidden'
        }),
      undefined,
      { timeout: 60000 }
    )
    const diagnostic = await readManagerReleaseDialogDiagnostic(dialog, releaseRequests, browserMessages)
    const statusText = diagnostic.descriptions.join(' ')
    if (
      statusText.includes('待管理者代表放行') &&
      diagnostic.signoffValueLength > 0 &&
      diagnostic.validation.length === 0 &&
      !(await confirmButton.isDisabled())
    ) {
      return confirmButton
    }
    await page.waitForTimeout(500)
  }
  const diagnostic = await readManagerReleaseDialogDiagnostic(dialog, releaseRequests, browserMessages)
  throw new Error(`Manager release dialog never became submittable: ${JSON.stringify(diagnostic)}`)
}

async function main() {
  const password = requiredEnv('EDHR_ARCHIVE_TASK_E2E_ARCHIVER_PASSWORD')
  ensureDir()
  const browser = await chromium.launch({ headless: true })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  await context.tracing.start({ screenshots: true, snapshots: true })
  const page = await context.newPage()
  const releaseRequests = []
  const browserMessages = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/pro/edhr-release')) {
      releaseRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      browserMessages.push(`${message.type()}: ${message.text()}`)
    }
  })
  page.on('pageerror', (error) => {
    browserMessages.push(`pageerror: ${error.message}`)
  })
  try {
    await login(page, USERNAME, password)
    await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${SOURCE_BATCH_EXECUTION_ID}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await page.locator('[data-batch-simulate-stage4-dossier]').waitFor({ state: 'visible', timeout: 60000 })
    await clickEnabled(page, /批次执行四份材料上传模拟/, 'Stage4 simulation')
    await page.waitForURL((url) => url.searchParams.has('stage4SimulationRunId') && url.searchParams.get('id') !== SOURCE_BATCH_EXECUTION_ID, {
      timeout: 120000
    })
    const batchExecutionId = new URL(page.url()).searchParams.get('id')
    const stage4SimulationRunId = new URL(page.url()).searchParams.get('stage4SimulationRunId')
    assert.ok(batchExecutionId, 'Stage4 must navigate to a new batch execution.')
    assert.ok(stage4SimulationRunId, 'Stage4 must provide its run identifier.')
    const batch = await getJson(page, `/admin-api/mes/pro/edhr-batch-execution/get?id=${encodeURIComponent(batchExecutionId)}`)
    assert.ok(batch.batchCode, 'Stage4 batch must have a batch code.')

    await clickEnabled(page, /最终放行模拟准备/, 'Stage5 simulation')
    await page.waitForURL((url) => url.pathname.endsWith('/edhr-work-task') && url.searchParams.get('taskType') === 'RELEASE_APPROVE', {
      timeout: 120000
    })
    const releaseTaskUrl = page.url()
    const stage5SimulationRunId = new URL(releaseTaskUrl).searchParams.get('simulationRunId')
    assert.ok(stage5SimulationRunId, 'Stage5 must provide its simulation run identifier.')
    const stage5SignoffStorageKey = `${STAGE5_SIMULATION_SIGNOFF_STORAGE_KEY}:${stage5SimulationRunId}`
    const signoffEvidenceHash = await page.evaluate((key) => window.localStorage.getItem(key), stage5SignoffStorageKey)
    assert.ok(signoffEvidenceHash?.trim(), 'Stage5 simulation must preserve manager signoff evidence hash.')

    await switchLoggedInUser(page, context, MANAGER_USERNAME, password, [
      { key: stage5SignoffStorageKey, value: signoffEvidenceHash.trim() }
    ])
    await page.goto(releaseTaskUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => undefined)
    await page.getByRole('tab', { name: '候选审核' }).click()
    await waitForWorkTaskPageIdle(page)
    await setTaskTypeFilter(page, '最终放行审批')
    const toolbar = page.locator('.edhr-work-task-page__toolbar').first()
    await fillVisible(toolbar, '.el-form-item:has-text("批次") input', String(batch.batchCode), 'batch code')
    await clickEnabled(toolbar, /^查询$/, 'query')
    await waitForWorkTaskPageIdle(page)
    const releaseRow = await findVisibleTaskRowByTexts(page, [String(batch.batchCode), '最终放行审批'])
    await clickWorkTaskRowAction(page, releaseRow, /最终放行/, 'manager release')
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '管理者代表最终放行' }).first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await fillVisible(
      dialog,
      '.el-form-item:has-text("签核证据") input',
      signoffEvidenceHash.trim(),
      'manager signoff evidence hash'
    )
    await fillVisible(dialog, 'textarea', 'Task-owned PDF/A archive E2E simulation release.', 'approval opinion')
    const confirmButton = await waitForManagerReleaseDialogReady(page, dialog, releaseRequests, browserMessages)
    const approvalRequest = page.waitForRequest(
      (request) => request.url().includes('/admin-api/mes/pro/edhr-release/approve') && request.method() === 'POST',
      { timeout: 60000 }
    )
    const approvalResponse = page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/edhr-release/approve') && response.request().method() === 'POST',
      { timeout: 60000 }
    ).catch(async (error) => {
      const diagnostic = await readManagerReleaseDialogDiagnostic(dialog, releaseRequests, browserMessages)
      throw new Error(
        `Manager release submit did not send approve request: ${error.message}; ` +
          `releaseRequests=${JSON.stringify(releaseRequests)}; ` +
          `dialog=${JSON.stringify(diagnostic)}`
      )
    })
    await confirmButton.click()
    await approvalRequest
    const response = await approvalResponse
    assert.equal(response.status(), 200, `Manager release HTTP status must be 200, got ${response.status()}.`)
    const responseBody = await response.json()
    assert.equal(responseBody.code, 0, `Manager release must succeed: ${responseBody.msg || ''}`)

    const releasedBatch = await getJson(page, `/admin-api/mes/pro/edhr-batch-execution/get?id=${encodeURIComponent(batchExecutionId)}`)
    assert.equal(releasedBatch.status, 30, `Released bootstrap batch must be CLOSED(30), got ${releasedBatch.status}.`)
    assert.equal(releasedBatch.canArchive, true, 'Released bootstrap batch must be archive eligible.')

    await switchLoggedInUser(page, context, USERNAME, password)
    const archivePage = await getJson(
      page,
      `/admin-api/mes/pro/edhr-work-task/my-page?pageNo=1&pageSize=20&taskType=ARCHIVE&batchCode=${encodeURIComponent(batch.batchCode)}&status=TODO`
    )
    const archiveTask = (archivePage.list || []).find((item) => String(item.batchExecutionId) === String(batchExecutionId))
    assert.ok(archiveTask, 'Released bootstrap batch must create an ARCHIVE/TODO work task.')
    const result = {
      status: 'PASS',
      batchExecutionId: String(batchExecutionId),
      batchCode: String(batch.batchCode),
      workTaskId: String(archiveTask.id),
      stage4SimulationRunId,
      stage5SimulationRunId
    }
    fs.writeFileSync(path.join(RESULT_DIR, 'result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    await context.tracing.stop({ path: path.join(RESULT_DIR, 'trace.zip') })
    await browser.close()
    console.log(JSON.stringify(result))
  } catch (error) {
    await context.tracing.stop({ path: path.join(RESULT_DIR, 'trace-failure.zip') }).catch(() => undefined)
    await browser.close().catch(() => undefined)
    throw error
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
