const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_FORM_TRACE_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_FORM_TRACE_TENANT || '测试租户'
const USERNAME = process.env.EDHR_FORM_TRACE_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_FORM_TRACE_PASSWORD || '111111'
const TARGET_PATH = '/mes/pro/feedback/edhr-form-trace'
const RESULT_DIR = path.resolve(process.cwd(), 'tests/output/20260715-edhr-form-trace-change-tab-real')

if (BASE_URL !== 'http://localhost:8081') {
  throw new Error(`form_trace_change_e2e_base_url_must_be_localhost_8081:${BASE_URL}`)
}
assert.equal(TENANT, '测试租户', 'real E2E must use 测试租户')
assert.equal(USERNAME, 'aoteman', 'real E2E must use aoteman')

const ensureDir = (dir) => fs.mkdirSync(dir, { recursive: true })

const selectTenant = async (page, form) => {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(TENANT)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(TENANT)
}

const login = async (page) => {
  const loginUrl = new URL('/login', BASE_URL)
  loginUrl.searchParams.set('redirect', TARGET_PATH)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login_api_failed:${loginPayload.msg || loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

const main = async () => {
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: process.env.EDHR_FORM_TRACE_HEADED === '1' ? false : true })
  const evidence = {
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    targetPath: TARGET_PATH,
    auditDefaultVisible: false,
    changeTabVisible: false,
    formTraceMenuVisible: false,
    legacyChangeMenuVisible: false,
    changeListTotal: 0,
    changeRows: [],
    writeRequestCount: 0
  }
  let page
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    page.on('request', (request) => {
      if (
        request.url().includes('/mes/pro/edhr-change/') &&
        ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())
      ) {
        evidence.writeRequestCount += 1
      }
    })

    await login(page)

    const auditResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/batch-record-execution/tracking-page'),
      { timeout: 60000 }
    )
    await page.goto(new URL(TARGET_PATH, BASE_URL).toString(), { waitUntil: 'domcontentloaded' })
    await auditResponsePromise
    await page.getByText('表单追溯', { exact: true }).first().waitFor({ state: 'visible' })
    evidence.formTraceMenuVisible = true
    evidence.legacyChangeMenuVisible = await page.getByText('变更与异常', { exact: true }).first().isVisible().catch(() => false)
    assert.equal(evidence.legacyChangeMenuVisible, false, 'legacy change menu must be hidden after merge')
    await page.locator('.el-tabs__item.is-active').filter({ hasText: '审计' }).first().waitFor({ state: 'visible' })
    evidence.auditDefaultVisible = true

    const changeResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-change/page'),
      { timeout: 60000 }
    )
    await page.getByRole('tab', { name: '变更' }).click()
    await page.locator('.el-tabs__item.is-active').filter({ hasText: '变更' }).first().waitFor({ state: 'visible' })
    evidence.changeTabVisible = true
    const changeResponse = await changeResponsePromise
    const changePayload = await changeResponse.json()
    evidence.changeListTotal = Number(changePayload?.data?.total || 0)
    evidence.changeRows = (changePayload?.data?.list || []).slice(0, 10).map((row) => ({
      id: row.id || null,
      changeCode: row.changeCode || null,
      changeStatus: row.changeStatus || null
    }))

    for (const label of ['变更编号', '类型', '状态', '对象', '状态变化', '原因', '申请时间', '生效时间', '操作']) {
      await page.getByText(label, { exact: true }).first().waitFor({ state: 'visible' })
    }

    assert.ok(evidence.changeListTotal > 0, 'change_trace_real_data_required')
    assert.equal(evidence.writeRequestCount, 0, 'change trace read-only path must not write MES data')
    await page.screenshot({ path: path.join(RESULT_DIR, 'form-trace-change-tab.png'), fullPage: true })
    fs.writeFileSync(path.join(RESULT_DIR, 'evidence.json'), JSON.stringify(evidence, null, 2), 'utf8')
  } catch (error) {
    if (page) {
      await page.screenshot({ path: path.join(RESULT_DIR, 'failure.png'), fullPage: true }).catch(() => undefined)
    }
    fs.writeFileSync(path.join(RESULT_DIR, 'evidence.json'), JSON.stringify(evidence, null, 2), 'utf8')
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  ensureDir(RESULT_DIR)
  fs.writeFileSync(path.join(RESULT_DIR, 'error.txt'), String(error.stack || error), 'utf8')
  console.error(error)
  process.exit(1)
})
