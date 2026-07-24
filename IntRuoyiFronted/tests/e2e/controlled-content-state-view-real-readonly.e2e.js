const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.CONTROLLED_CONTENT_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.CONTROLLED_CONTENT_E2E_TENANT || '测试租户'
const USERNAME = process.env.CONTROLLED_CONTENT_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.CONTROLLED_CONTENT_E2E_PASSWORD || '111111'
const TASK_DIR = path.join(
  __dirname,
  '..',
  '..',
  '..',
  'doc',
  'tasks',
  '20260718-controlled-content-state-machine-implementation',
  'e2e-artifacts'
)
const RESULT_PATH = path.join(TASK_DIR, 'controlled-content-state-view-real-readonly.json')

function writeResult(result) {
  fs.mkdirSync(TASK_DIR, { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requirePrerequisites() {
  assert.equal(TENANT, '测试租户', 'controlled content state view real E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'controlled content state view real E2E must use aoteman')
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit', timeout: 60000 })

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
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.equal(loginResponse.ok(), true, `login HTTP status ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code ${loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), {
    timeout: 60000,
    waitUntil: 'commit'
  })
}

function extractPageRows(payload) {
  assert.ok([0, 200].includes(payload.code), `browser-page business code ${payload.code}`)
  const rows = payload.data?.list || []
  assert.ok(rows.length > 0, '测试租户 DCC 浏览页没有可见受控文件，无法做真实详情只读 E2E')
  return rows
}

function isExactControlledFileDetailResponse(response, selectedVersionId) {
  if (response.request().method() !== 'GET') {
    return false
  }
  const pathname = new URL(response.url()).pathname
  return pathname.endsWith(`/dcc/controlled-files/${selectedVersionId}`)
}

async function main() {
  requirePrerequisites()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []
  const pageErrors = []
  let selectedVersionId = null
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('pageerror', (error) => pageErrors.push(error.message))
    page.on('request', (request) => {
      const method = request.method()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && request.url().includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url: request.url() })
      }
    })

    await login(page)

    const browserResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/dcc/controlled-files/browser-page') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=20`, {
      waitUntil: 'commit'
    })
    const browserResponse = await browserResponsePromise
    assert.equal(browserResponse.ok(), true, `browser-page HTTP status ${browserResponse.status()}`)
    const rows = extractPageRows(await browserResponse.json())
    const firstRow = rows[0]
    assert.ok(firstRow.id, 'DCC 浏览页首条记录缺少受控文件 ID，无法进入详情')
    selectedVersionId = firstRow.id

    const detailLink = page.locator('[data-testid="dcc-browser-file-number-detail-link"]:visible').first()
    await detailLink.waitFor({ state: 'visible' })

    const detailResponsePromise = page.waitForResponse(
      (response) => isExactControlledFileDetailResponse(response, selectedVersionId),
      { timeout: 60000 }
    )
    await detailLink.click()
    await page.waitForURL((current) => current.pathname.includes('/dcc/controlled-file/detail/'), {
      timeout: 60000,
      waitUntil: 'commit'
    })
    const detailResponse = await detailResponsePromise
    assert.equal(detailResponse.ok(), true, `controlled-file detail HTTP status ${detailResponse.status()}`)
    const detailPayload = await detailResponse.json()
    assert.ok([0, 200].includes(detailPayload.code), `detail business code ${detailPayload.code}`)
    const detail = detailPayload.data || {}
    assert.ok(detail.id, 'DCC 详情接口缺少受控文件 ID')
    assert.ok(detail.versionNo, 'DCC 详情接口缺少版本号')

    const stateStrip = page.locator('[data-testid="dcc-detail-controlled-content-state"]').first()
    await stateStrip.waitFor({ state: 'visible' })
    const stateText = await stateStrip.innerText()
    assert.match(stateText, /受控文件版本状态/, 'DCC 详情页必须展示受控文件版本状态标题')
    assert.match(stateText, /DCC 受控文件/, 'DCC 详情页必须展示受控内容类型')
    assert.ok(stateText.includes(String(detail.versionNo)), `状态条必须展示当前版本号: ${detail.versionNo}`)
    assert.deepEqual(writeRequests, [], 'DCC 受控内容状态只读 E2E 不得发起 DCC 写请求')
    assert.deepEqual(pageErrors, [], 'DCC 受控内容状态只读 E2E 不得出现页面运行时错误')

    writeResult({
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      browserPath: '/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=20',
      detailPath: new URL(page.url()).pathname,
      controlledFileId: detail.id,
      fileNumber: detail.fileNumber,
      versionNo: detail.versionNo,
      lifecycleStatus: detail.status,
      stateText,
      writeRequests,
      pageErrors
    })
    console.log(
      `GREEN: controlled-content-state-view-real-readonly -> PASS, controlledFileId=${detail.id}, artifact=${RESULT_PATH}`
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      selectedVersionId,
      error: error.stack || error.message,
      writeRequests,
      pageErrors
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(`BLOCKER: controlled-content-state-view-real-readonly -> ${error.stack || error.message}`)
  process.exit(1)
})
