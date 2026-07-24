const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_FORM_TRACE_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_FORM_TRACE_TENANT || '测试租户'
const USERNAME = process.env.EDHR_FORM_TRACE_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_FORM_TRACE_PASSWORD || '111111'
const TARGET_PATH = '/mes/pro/feedback/edhr-form-trace'
const RESULT_DIR = path.resolve(process.cwd(), 'tests/output/20260714-edhr-form-trace-tabs-real')

if (BASE_URL !== 'http://localhost:8081') {
  throw new Error(`form_trace_e2e_base_url_must_be_localhost_8081:${BASE_URL}`)
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

const visibleText = async (locator) => (await locator.textContent({ timeout: 5000 })) || ''

const main = async () => {
  ensureDir(RESULT_DIR)
  const browser = await chromium.launch({ headless: process.env.EDHR_FORM_TRACE_HEADED === '1' ? false : true })
  const evidence = {
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    targetPath: TARGET_PATH,
    auditDefaultVisible: false,
    changeListTotal: 0,
    releaseListTotal: 0,
    changeRows: [],
    releaseRows: [],
    enabledCheckButtonCount: 0,
    enabledEventButtonCount: 0,
    checkDrawerOpened: false,
    eventDrawerOpened: false
  }
  let page
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)

    const auditResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/batch-record-execution/tracking-page'),
      { timeout: 60000 }
    )
    await page.goto(new URL(TARGET_PATH, BASE_URL).toString(), { waitUntil: 'domcontentloaded' })
    await auditResponsePromise

    const activeTab = page.locator('.el-tabs__item.is-active').filter({ hasText: '审计' }).first()
    await activeTab.waitFor({ state: 'visible' })
    evidence.auditDefaultVisible = true
    for (const label of ['执行编号', '生产上下文', '当前阶段', '最后处理', '归档状态']) {
      await page.getByText(label, { exact: true }).first().waitFor({ state: 'visible' })
    }

    const changeResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-change/page'),
      { timeout: 60000 }
    )
    await page.getByRole('tab', { name: '变更' }).click()
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

    const releaseResponsePromise = page.waitForResponse(
      (response) => response.url().includes('/mes/pro/edhr-release/page'),
      { timeout: 60000 }
    )
    await page.getByRole('tab', { name: '放行' }).click()
    const releaseResponse = await releaseResponsePromise
    const releasePayload = await releaseResponse.json()
    evidence.releaseListTotal = Number(releasePayload?.data?.total || 0)
    evidence.releaseRows = (releasePayload?.data?.list || []).slice(0, 10).map((row) => ({
      releaseTransactionId: row.releaseTransactionId || null,
      batchExecutionCode: row.batchExecutionCode || null,
      releaseStatus: row.releaseStatus || null
    }))

    for (const label of ['放行对象', '产品/路线', '放行状态', '检查摘要', '质量门禁', '事务时间', '追溯']) {
      await page.getByText(label, { exact: true }).first().waitFor({ state: 'visible' })
    }

    assert.ok(evidence.releaseListTotal > 0, 'release_trace_real_data_required')
    const enabledCheckButton = page.locator('button:has-text("检查项"):not([disabled])').first()
    evidence.enabledCheckButtonCount = await page.locator('button:has-text("检查项"):not([disabled])').count()
    evidence.enabledEventButtonCount = await page.locator('button:has-text("事务事件"):not([disabled])').count()
    fs.writeFileSync(path.join(RESULT_DIR, 'evidence.json'), JSON.stringify(evidence, null, 2), 'utf8')
    if (evidence.enabledCheckButtonCount < 1 || evidence.enabledEventButtonCount < 1) {
      throw new Error(`release_trace_drawer_real_data_blocked:${JSON.stringify(evidence.releaseRows)}`)
    }
    await enabledCheckButton.waitFor({ state: 'visible' })
    await enabledCheckButton.click()
    await page.getByText('eDHR放行检查项', { exact: false }).first().waitFor({ state: 'visible' })
    evidence.checkDrawerOpened = true
    await page.keyboard.press('Escape')

    const enabledEventButton = page.locator('button:has-text("事务事件"):not([disabled])').first()
    await enabledEventButton.waitFor({ state: 'visible' })
    await enabledEventButton.click()
    await page.getByText('eDHR放行事务事件', { exact: false }).first().waitFor({ state: 'visible' })
    evidence.eventDrawerOpened = true

    await page.screenshot({ path: path.join(RESULT_DIR, 'form-trace-release-tab.png'), fullPage: true })
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
