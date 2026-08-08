const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_UPLOAD_CURRENT_VERSION_E2E_BASE_URL || 'http://127.0.0.1:8086').replace(/\/+$/, '')
const TENANT = process.env.DCC_UPLOAD_CURRENT_VERSION_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_UPLOAD_CURRENT_VERSION_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_UPLOAD_CURRENT_VERSION_E2E_PASSWORD
const TARGET_PATH = '/dcc/controlled-file/upload'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-upload-current-version')
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, 'real-e2e-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requirePrerequisites() {
  assert.ok(BASE_URL, 'DCC_UPLOAD_CURRENT_VERSION_E2E_BASE_URL is required')
  const url = new URL(BASE_URL)
  assert.notEqual(url.hostname, '172.30.30.57', 'DCC upload current-version E2E must not target production')
  assert.equal(TENANT, '测试租户', 'DCC upload current-version E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'DCC upload current-version E2E must use aoteman')
  assert.ok(PASSWORD, 'DCC_UPLOAD_CURRENT_VERSION_E2E_PASSWORD is required')
  assert.equal(FORBIDDEN_TENANTS.has(TENANT), false, 'Real E2E must not target a protected tenant')
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', TARGET_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(TENANT)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

function unwrapPageRows(payload, label) {
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  const rows = payload.data?.list || []
  assert.ok((payload.data?.total ?? 0) > 0, `${label} must return real rows`)
  assert.ok(rows.length > 0, `${label} must return visible rows`)
  return rows
}

function normalizeText(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

function findActiveFile(rows) {
  const active = rows.find((row) => normalizeText(row.fileNumber) && row.status === 'ACTIVE')
  assert.ok(active, 'real browser page must contain an ACTIVE controlled file with fileNumber')
  return active
}

async function main() {
  requirePrerequisites()
  const launchOptions = { headless: true, args: ['--disable-dev-shm-usage'] }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const writeRequests = []
  let selectedFile = null
  let currentVersionPayload = null
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)

    page.on('request', (request) => {
      const method = request.method()
      const requestUrl = request.url()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && requestUrl.includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url: request.url() })
      }
    })

    const browserPayload = await page.evaluate(async () => {
      const readWsCache = (key) => {
        const raw = window.localStorage.getItem(key)
        if (!raw) {
          return undefined
        }
        const cacheItem = JSON.parse(raw)
        if (!cacheItem || typeof cacheItem !== 'object' || !('v' in cacheItem)) {
          return undefined
        }
        return JSON.parse(cacheItem.v)
      }
      const accessToken = readWsCache('ACCESS_TOKEN')
      const tenantId = readWsCache('tenantId')
      const visitTenantId = readWsCache('visitTenantId')
      if (!accessToken || !tenantId) {
        throw new Error('missing authenticated browser cache for read-only DCC list request')
      }
      const headers = {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
      if (visitTenantId) {
        headers['visit-tenant-id'] = String(visitTenantId)
      }
      const response = await fetch('/admin-api/dcc/controlled-files/browser-page?pageNo=1&pageSize=20&status=ACTIVE', {
        method: 'GET',
        headers
      })
      return await response.json()
    })
    selectedFile = findActiveFile(unwrapPageRows(browserPayload, 'browser ACTIVE page'))

    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible' })

    const fileNumberInput = page
      .locator('.el-form-item')
      .filter({ hasText: '文件编号' })
      .locator('input')
      .first()
    const currentVersionResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/dcc/controlled-files/current-version') &&
        response.url().includes(encodeURIComponent(selectedFile.fileNumber)) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await fileNumberInput.fill(selectedFile.fileNumber)
    await fileNumberInput.blur()

    const currentVersionResponse = await currentVersionResponsePromise
    assert.equal(currentVersionResponse.ok(), true, `current-version HTTP status ${currentVersionResponse.status()}`)
    currentVersionPayload = await currentVersionResponse.json()
    assert.ok([0, 200].includes(currentVersionPayload.code), `current-version business code ${currentVersionPayload.code}`)
    assert.equal(currentVersionPayload.data?.matched, true, 'current-version must match the selected active file')
    assert.equal(currentVersionPayload.data?.fileNumber, selectedFile.fileNumber, 'matched file number must echo request')
    assert.equal(currentVersionPayload.data?.currentVersionNo, selectedFile.versionNo, 'matched current version must equal active file')

    const panel = page.locator('[data-testid="dcc-upload-current-version-panel"]').first()
    await panel.waitFor({ state: 'visible' })
    const panelText = await panel.innerText()
    assert.ok(panelText.includes(selectedFile.fileNumber), 'current-version panel must show file number')
    assert.ok(panelText.includes(selectedFile.versionNo), 'current-version panel must show active version')
    assert.ok(panelText.includes('当前变更方式：升版'), 'current-version lookup must switch change type to revision')
    assert.equal(
      await page.getByRole('radio', { name: '升版' }).count(),
      0,
      'current-version real E2E must not expose manual change-type radios'
    )
    assert.deepEqual(writeRequests, [], 'current-version real E2E must not send DCC write requests')

    const result = {
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      selectedFile: {
        id: selectedFile.id,
        fileNumber: selectedFile.fileNumber,
        fileName: selectedFile.fileName,
        versionNo: selectedFile.versionNo,
        status: selectedFile.status
      },
      currentVersion: currentVersionPayload.data,
      writeRequests
    }
    writeResult(result)
    console.log(
      `PASS: dcc upload current-version real E2E, fileNumber=${selectedFile.fileNumber}, version=${selectedFile.versionNo}`
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      selectedFile,
      currentVersionPayload,
      writeRequests,
      error: error.message
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
