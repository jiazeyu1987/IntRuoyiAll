const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.DCC_CONTROLLED_FILE_LOGS_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_CONTROLLED_FILE_LOGS_E2E_TENANT || '测试租户'
const USERNAME = process.env.DCC_CONTROLLED_FILE_LOGS_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.DCC_CONTROLLED_FILE_LOGS_E2E_PASSWORD || '111111'
const TARGET_PATH = '/dcc/controlled-file/logs'
const RESULT_DIR = path.resolve(process.cwd(), 'test-results', 'dcc-controlled-file-logs')
const FORBIDDEN_TENANTS = new Set(['芋道源码', 'yudao', 'Yudao', 'YUDAO'])

function writeResult(result) {
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  fs.writeFileSync(path.join(RESULT_DIR, 'real-e2e-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function requirePrerequisites() {
  assert.equal(TENANT, '测试租户', 'DCC controlled-file logs real E2E must use 测试租户')
  assert.equal(USERNAME, 'aoteman', 'DCC controlled-file logs real E2E must use aoteman')
  assert.equal(FORBIDDEN_TENANTS.has(TENANT), false, 'Real E2E must not target a protected tenant')
}

async function login(page) {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

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
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function selectLogTypeAndQuery(page, logTypeLabel, expectedLogType) {
  const form = page.locator('[data-testid="dcc-controlled-file-logs-filter-form"]').first()
  await form.waitFor({ state: 'visible' })
  await form.locator('.table-quick-filter__field').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: '日志类型' }).first().click()
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/controlled-file-logs/page') &&
      response.url().includes(`logType=${expectedLogType}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await form.locator('.table-quick-filter__value').click()
  await page.locator('.el-select-dropdown__item:visible').filter({ hasText: logTypeLabel }).first().click()
  return responsePromise
}

async function readVisibleDccMenuText(page) {
  const dccMenuTitle = page.locator('.el-sub-menu__title').filter({ hasText: '文控中心' }).first()
  await dccMenuTitle.waitFor({ state: 'visible' })
  if ((await page.locator('.v-menu__title:visible').filter({ hasText: '文件提交' }).count()) === 0) {
    await dccMenuTitle.click()
    await page.locator('.v-menu__title:visible').filter({ hasText: '文件提交' }).first().waitFor({ state: 'visible' })
  }
  return (await page.locator('.v-menu__title:visible').allTextContents()).join('|')
}

async function main() {
  requirePrerequisites()
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)

    page.on('request', (request) => {
      const method = request.method()
      if (!['GET', 'HEAD', 'OPTIONS'].includes(method) && request.url().includes('/admin-api/dcc/')) {
        writeRequests.push({ method, url: request.url() })
      }
    })

    const initialResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/dcc/controlled-file-logs/page') && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded' })
    const initialResponse = await initialResponsePromise
    assert.equal(initialResponse.ok(), true, `log page HTTP status ${initialResponse.status()}`)
    const initialPayload = await initialResponse.json()
    assert.ok([0, 200].includes(initialPayload.code), `log page business code ${initialPayload.code}`)

    const table = page.locator('[data-user-table-key="dcc.controlledFile.logs"]').first()
    await table.waitFor({ state: 'visible' })
    await page.getByText('发生时间', { exact: false }).first().waitFor({ state: 'visible' })
    await page.getByText('日志类型', { exact: false }).first().waitFor({ state: 'visible' })
    const filterForm = page.locator('[data-testid="dcc-controlled-file-logs-filter-form"]').first()
    assert.equal(await filterForm.getByRole('button', { name: '查询' }).count(), 1)
    assert.equal(await filterForm.getByRole('button', { name: '重置' }).count(), 0)

    const visibleMenuText = await readVisibleDccMenuText(page)
    for (const retained of ['文件提交', '文件查阅']) {
      assert.ok(visibleMenuText.includes(retained), `文控中心核心入口必须保留：${retained}`)
    }
    assert.ok(visibleMenuText.includes('文控日志'), '文控中心必须显示文控日志入口')
    for (const retired of ['文件审计', '项目代码修正追溯', '我的DCC修正']) {
      assert.equal(visibleMenuText.includes(retired), false, `旧日志/追溯入口不得显示：${retired}`)
    }

    const filteredResponse = await selectLogTypeAndQuery(page, '访问', 'CONTROLLED_FILE_AUDIT')
    assert.equal(filteredResponse.ok(), true, `filtered log page HTTP status ${filteredResponse.status()}`)
    const filteredPayload = await filteredResponse.json()
    assert.ok([0, 200].includes(filteredPayload.code), `filtered log page business code ${filteredPayload.code}`)
    const filteredRows = filteredPayload.data?.list || []
    assert.equal(
      filteredRows.every((row) => row.logType === 'CONTROLLED_FILE_AUDIT'),
      true,
      '日志类型筛选后只应返回文件访问审计记录'
    )
    const distributionResponse = await selectLogTypeAndQuery(page, '分发', 'FILE_DISTRIBUTION')
    assert.equal(distributionResponse.ok(), true, `distribution log page HTTP status ${distributionResponse.status()}`)
    const distributionPayload = await distributionResponse.json()
    assert.ok([0, 200].includes(distributionPayload.code), `distribution log page business code ${distributionPayload.code}`)
    const distributionRows = distributionPayload.data?.list || []
    assert.equal(
      distributionRows.every((row) => row.logType === 'FILE_DISTRIBUTION'),
      true,
      '日志类型筛选后只应返回文件分发记录'
    )

    const initialRows = initialPayload.data?.list || []
    const result = {
      status: 'PASS',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      initialTotal: initialPayload.data?.total ?? 0,
      initialRows: initialRows.length,
      filteredTotal: filteredPayload.data?.total ?? 0,
      filteredRows: filteredRows.length,
      distributionTotal: distributionPayload.data?.total ?? 0,
      distributionRows: distributionRows.length,
      writeRequests
    }
    assert.deepEqual(writeRequests, [], '文控日志只读 E2E 不得发起 DCC 写请求')
    writeResult(result)
    console.log(
      `PASS: dcc controlled-file logs real E2E, initialTotal=${result.initialTotal}, filteredTotal=${result.filteredTotal}`
    )
  } catch (error) {
    writeResult({
      status: 'FAIL',
      baseUrl: BASE_URL,
      tenant: TENANT,
      username: USERNAME,
      targetPath: TARGET_PATH,
      error: error.message,
      writeRequests
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
