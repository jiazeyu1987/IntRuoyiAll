const assert = require('node:assert/strict')
const fs = require('node:fs')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.FORM_TEMPLATE_FILL_OPEN_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.FORM_TEMPLATE_FILL_OPEN_TENANT || '芋道源码',
  username: process.env.FORM_TEMPLATE_FILL_OPEN_USERNAME || 'admin',
  password: process.env.FORM_TEMPLATE_FILL_OPEN_PASSWORD || 'admin123',
  templateName: process.env.FORM_TEMPLATE_FILL_OPEN_TEMPLATE || '按压式压力泵过程检验记录',
  versionNo: process.env.FORM_TEMPLATE_FILL_OPEN_VERSION || 'V21.0',
  timeout: Number(process.env.FORM_TEMPLATE_FILL_OPEN_TIMEOUT_MS || 120000),
  headed: process.env.FORM_TEMPLATE_FILL_OPEN_HEADED === '1'
}

const executablePath =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function assertPrerequisites() {
  const hostname = new URL(config.baseUrl).hostname
  assert.ok(
    ['127.0.0.1', 'localhost', '::1', '[::1]'].includes(hostname),
    `real E2E must stay local, got ${config.baseUrl}`
  )
  assert.ok(config.password, 'FORM_TEMPLATE_FILL_OPEN_PASSWORD is required')
  assert.ok(fs.existsSync(executablePath), `Chrome executable is missing: ${executablePath}`)
}

async function selectTenant(page, form) {
  const tenantSelect = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantSelect.isVisible().catch(() => false)) {
    await tenantSelect.click()
    await tenantSelect.fill(config.tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function fillFirstVisible(scope, selectors, value) {
  for (const selector of selectors) {
    const locator = scope.locator(selector).first()
    if (await locator.isVisible().catch(() => false)) {
      await locator.fill(String(value))
      return
    }
  }
  throw new Error(`missing visible input: ${selectors.join(', ')}`)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded', timeout: config.timeout })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form)
  await fillFirstVisible(
    form,
    [
      'input[placeholder="请输入用户名"]',
      'input[placeholder*="用户名"]',
      'input[placeholder*="账号"]',
      'input.el-input__inner:not([type="password"]):not([role="combobox"])'
    ],
    config.username
  )
  await fillFirstVisible(
    form,
    ['input[type="password"]', 'input[placeholder="请输入密码"]', 'input[placeholder*="密码"]'],
    config.password
  )
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok(loginPayload && [0, 200].includes(loginPayload.code), `login_business_failed:${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.href.includes('/login'), { timeout: config.timeout })
}

async function readTemplateListFromPageResponse(page) {
  const responses = []
  page.on('response', async (response) => {
    if (!response.url().includes('/form-center/template-pool')) return
    responses.push({
      status: response.status(),
      body: await response.text().catch(() => '')
    })
  })
  await page.goto(new URL('/mdm/form-center/template', config.baseUrl).toString(), {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
  for (let index = 0; index < 60 && responses.length === 0; index += 1) {
    await page.waitForTimeout(1000)
  }
  const latest = responses.at(-1)
  assert.ok(latest?.body, `template_list_response_missing: ${JSON.stringify(responses)}`)
  const payload = JSON.parse(latest.body)
  assert.ok([0, 200].includes(payload?.code), `template_list_business_failed:${latest.body.slice(0, 1000)}`)
  return payload?.data?.list || []
}

async function findTargetRow(page, templateRow) {
  const rows = [
    page
      .getByRole('row')
      .filter({ hasText: templateRow.templateName })
      .filter({ hasText: templateRow.versionNo })
      .filter({ hasText: '已发布' })
      .first(),
    page
      .locator('.el-table__body-wrapper tbody tr, tr.el-table__row')
      .filter({ hasText: templateRow.templateName })
      .filter({ hasText: templateRow.versionNo })
      .filter({ hasText: '已发布' })
      .first()
  ]
  for (const row of rows) {
    if (await row.isVisible().catch(() => false)) {
      return row
    }
  }
  const visibleRows = await page
    .locator('.el-table__body-wrapper tbody tr, tr.el-table__row, [role="row"]')
    .evaluateAll((items) => items.map((item) => item.textContent?.replace(/\s+/g, ' ').trim()).filter(Boolean).slice(0, 20))
    .catch(() => [])
  throw new Error(`target_row_missing:${JSON.stringify({ templateRow, visibleRows })}`)
}

async function main() {
  assertPrerequisites()
  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)
  const autoDetectRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/fill-rule-auto-detect') && request.method() === 'POST') {
      autoDetectRequests.push(request.url())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    const rows = await readTemplateListFromPageResponse(page)
    const target = rows.find(
      (row) =>
        row.templateName?.includes(config.templateName) &&
        row.versionNo === config.versionNo &&
        row.status === 'PUBLISHED'
    )
    assert.ok(target, `published_template_missing:${JSON.stringify(rows.slice(0, 20))}`)

    const row = await findTargetRow(page, target)
    await row.click()
    const fillConfigButton = page.getByRole('button', { name: '填写配置' }).first()
    await fillConfigButton.waitFor({ state: 'visible', timeout: config.timeout })
    await fillConfigButton.click()

    const editor = page.locator('.batch-record-cell-rules-editor').first()
    await editor.waitFor({ state: 'visible', timeout: config.timeout })
    await page.waitForTimeout(2000)
    const editorText = await editor.innerText()
    assert.match(editorText, /填写配置/, 'fill_config_sidebar_missing')
    assert.match(editorText, /规则\s+\d+/, 'fill_rule_count_missing')
    assert.match(editorText, /只读/, 'published_template_readonly_badge_missing')
    assert.equal(autoDetectRequests.length, 0, `fill_config_click_must_not_auto_detect:${autoDetectRequests.join(',')}`)
    assert.equal(pageErrors.length, 0, `page_errors:${JSON.stringify(pageErrors)}`)
    console.log(
      `PASS form-template-fill-config-open-published-real template=${target.templateName} version=${target.versionNo} status=${target.status}`
    )
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
