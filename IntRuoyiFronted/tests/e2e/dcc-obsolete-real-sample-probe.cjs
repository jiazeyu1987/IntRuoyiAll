const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_OBSOLETE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_OBSOLETE_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_OBSOLETE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_OBSOLETE_E2E_PASSWORD || '111111'
const TASK_DIR = process.env.DCC_OBSOLETE_E2E_TASK_DIR
  ? path.resolve(process.env.DCC_OBSOLETE_E2E_TASK_DIR)
  : path.resolve(__dirname, '../../../doc/tasks/20260720-form-center-controlled-state-machine-implementation/e2e-artifacts')
const CONTROLLED_FILE_ID = process.env.DCC_OBSOLETE_E2E_CONTROLLED_FILE_ID || ''
const RESULT_PATH = path.join(TASK_DIR, 'dcc-obsolete-real-sample-probe.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, JSON.stringify(result, null, 2) + '\n', 'utf8')
}

async function login(page) {
  await page.goto(BASE_URL + '/login?redirect=/index', { waitUntil: 'commit', timeout: 60000 })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.fill(TENANT)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 60000 })
  await tenantOption.click()

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, 'login HTTP status ' + loginResponse.status())
  assert.ok([0, 200].includes(loginPayload.code), 'login business code ' + loginPayload.code)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

async function main() {
  assert.equal(TENANT, process.env.DCC_OBSOLETE_E2E_EXPECT_TENANT || '测试租户', 'DCC obsolete real E2E probe tenant mismatch')
  assert.equal(USERNAME, process.env.DCC_OBSOLETE_E2E_EXPECT_USERNAME || 'aoteman', 'DCC obsolete real E2E probe username mismatch')

  const browser = await chromium.launch({
    headless: true,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined,
    args: ['--disable-dev-shm-usage']
  })
  const writeRequests = []
  const pageErrors = []
  const candidates = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method()) && request.url().includes('/admin-api/dcc/')) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    await login(page)

    if (CONTROLLED_FILE_ID) {
      const detailResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/dcc/controlled-files/' + CONTROLLED_FILE_ID) && response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(BASE_URL + '/dcc/controlled-file/detail/' + CONTROLLED_FILE_ID, { waitUntil: 'commit' })
      const detailResponse = await detailResponsePromise
      const detailPayload = await detailResponse.json()
      assert.equal(detailResponse.ok(), true, 'controlled detail HTTP status ' + detailResponse.status())
      assert.ok([0, 200].includes(detailPayload.code), 'controlled detail business code ' + detailPayload.code)
      const detail = detailPayload.data || {}
      const projection = detail.actionProjection || {}
      const allowedActions = projection.allowedActions || []
      const selected = {
        id: detail.id || CONTROLLED_FILE_ID,
        fileNumber: detail.fileNumber,
        fileName: detail.fileName || detail.title,
        versionNo: detail.versionNo,
        status: detail.status,
        actionLocked: projection.actionLocked,
        actionLockReason: projection.actionLockReason,
        pendingRequestId: projection.pendingRequestId,
        canWithdraw: projection.canWithdraw,
        allowedActions,
        obsoleteTextCount: await page.getByText('作废当前版本').count()
      }
      candidates.push(selected)
      const result = {
        status: selected.status === 'ACTIVE' && allowedActions.includes('OBSOLETE') && projection.actionLocked !== true ? 'PASS' : 'BLOCKED',
        baseUrl: BASE_URL,
        tenant: TENANT,
        username: USERNAME,
        browserRowCount: null,
        candidateCount: candidates.length,
        selected,
        candidates,
        writeRequests,
        pageErrors
      }
      writeResult(result)
      assert.deepEqual(writeRequests, [], 'sample probe must remain readonly')
      assert.deepEqual(pageErrors, [], 'sample probe page errors must be empty')
      assert.equal(result.status, 'PASS', 'controlled file must be ACTIVE and expose unlocked OBSOLETE')
      console.log('GREEN: dcc-obsolete-real-sample-probe -> PASS, controlledFileId=' + selected.id + ', artifact=' + RESULT_PATH)
      return
    }

    const browserResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/dcc/controlled-files/browser-page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(BASE_URL + '/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=50', { waitUntil: 'commit' })
    const browserResponse = await browserResponsePromise
    const browserPayload = await browserResponse.json()
    assert.equal(browserResponse.ok(), true, 'browser-page HTTP status ' + browserResponse.status())
    assert.ok([0, 200].includes(browserPayload.code), 'browser-page business code ' + browserPayload.code)
    const rows = browserPayload.data?.list || []

    for (const row of rows) {
      if (row.status !== 'ACTIVE' || !row.id) {
        continue
      }
      const detailResponsePromise = page.waitForResponse(
        (response) => response.url().includes('/dcc/controlled-files/' + row.id) && response.request().method() === 'GET',
        { timeout: 60000 }
      )
      await page.goto(BASE_URL + '/dcc/controlled-file/detail/' + row.id, { waitUntil: 'commit' })
      const detailResponse = await detailResponsePromise
      const detailPayload = await detailResponse.json()
      if (![0, 200].includes(detailPayload.code)) {
        continue
      }
      const detail = detailPayload.data || {}
      const projection = detail.actionProjection || {}
      const allowedActions = projection.allowedActions || []
      const obsoleteTextCount = await page.getByText('作废当前版本').count()
      const candidate = {
        id: detail.id || row.id,
        fileNumber: detail.fileNumber || row.fileNumber,
        fileName: detail.fileName || detail.title || row.fileName || row.title,
        versionNo: detail.versionNo || row.versionNo,
        status: detail.status || row.status,
        actionLocked: projection.actionLocked,
        actionLockReason: projection.actionLockReason,
        pendingRequestId: projection.pendingRequestId,
        canWithdraw: projection.canWithdraw,
        allowedActions,
        obsoleteTextCount
      }
      candidates.push(candidate)
      if (allowedActions.includes('OBSOLETE') && projection.actionLocked !== true) {
        break
      }
    }

    const selected = candidates.find((item) => item.allowedActions.includes('OBSOLETE') && item.actionLocked !== true) || null
    const result = {
      status: selected ? 'PASS' : 'BLOCKED',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      browserRowCount: rows.length,
      candidateCount: candidates.length,
      selected,
      candidates: candidates.slice(0, 10),
      writeRequests,
      pageErrors
    }
    writeResult(result)
    assert.deepEqual(writeRequests, [], 'sample probe must remain readonly')
    assert.deepEqual(pageErrors, [], 'sample probe page errors must be empty')
    if (!selected) {
      throw new Error('BLOCKER: no ACTIVE DCC controlled file exposes OBSOLETE in actionProjection; artifact=' + RESULT_PATH)
    }
    console.log('GREEN: dcc-obsolete-real-sample-probe -> PASS, controlledFileId=' + selected.id + ', artifact=' + RESULT_PATH)
  } catch (error) {
    if (!fs.existsSync(RESULT_PATH)) {
      writeResult({
        status: 'FAIL',
        baseUrl: BASE_URL,
        tenant: TENANT,
        username: USERNAME,
        candidates,
        writeRequests,
        pageErrors,
        error: error.stack || error.message
      })
    }
    console.error(error.stack || error.message)
    process.exit(1)
  } finally {
    await browser.close()
  }
}

main()
