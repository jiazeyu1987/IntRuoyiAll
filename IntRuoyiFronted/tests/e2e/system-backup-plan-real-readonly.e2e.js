const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const envPath = path.join(frontendRoot, '.env')

function readLoginDefaults() {
  const defaults = new Map()
  const envText = fs.readFileSync(envPath, 'utf8')
  for (const line of envText.split(/\r?\n/)) {
    const match = line.match(/^\s*(VITE_APP_DEFAULT_LOGIN_[A-Z]+)\s*=\s*(.+?)\s*$/)
    if (match) defaults.set(match[1], match[2].trim())
  }
  return {
    tenant: defaults.get('VITE_APP_DEFAULT_LOGIN_TENANT'),
    username: defaults.get('VITE_APP_DEFAULT_LOGIN_USERNAME'),
    password: defaults.get('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`Missing visible login field: ${label}`)
}

async function login(page, config) {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })

  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }

  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    config.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.equal(loginResponse.ok(), true, `login HTTP ${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok(loginBody.code === 0 || loginBody.code === 200, `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
}

async function run() {
  const loginDefaults = readLoginDefaults()
  const config = {
    baseUrl: process.env.SYSTEM_BACKUP_PLAN_E2E_BASE_URL || 'http://127.0.0.1:8083',
    timeout: Number(process.env.SYSTEM_BACKUP_PLAN_E2E_TIMEOUT || 90000),
    ...loginDefaults
  }
  assert.ok(config.tenant && config.username && config.password, 'Missing login defaults in frontend .env')

  const browser = await chromium.launch({ headless: process.env.HEADLESS !== 'false' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const backupApiEvents = []
  page.on('response', (response) => {
    if (response.url().includes('/admin-api/infra/backup-plan/')) {
      backupApiEvents.push({
        method: response.request().method(),
        status: response.status(),
        url: response.url()
      })
    }
  })

  try {
    await login(page, config)
    const statusResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/infra/backup-plan/status') &&
        response.request().method() === 'GET',
      { timeout: config.timeout }
    )
    const historyResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/infra/backup-plan/history/page') &&
        response.request().method() === 'GET',
      { timeout: config.timeout }
    )
    await page.goto(`${config.baseUrl}/system/backup-plan`, {
      waitUntil: 'domcontentloaded',
      timeout: config.timeout
    })
    const [statusResponse, historyResponse] = await Promise.all([
      statusResponsePromise,
      historyResponsePromise
    ])
    assert.equal(statusResponse.status(), 200, `backup status HTTP ${statusResponse.status()}`)
    assert.equal(historyResponse.status(), 200, `backup history HTTP ${historyResponse.status()}`)

    const pageRoot = page.locator('.backup-plan-page')
    await pageRoot.getByText('当前自动备份计划', { exact: false }).waitFor({
      state: 'visible',
      timeout: config.timeout
    })
    await pageRoot.getByText('自动备份', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
    await pageRoot.getByText('每天', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
    await pageRoot.getByText('每周', { exact: false }).first().waitFor({ state: 'visible', timeout: config.timeout })
    await pageRoot.getByText('备份包历史', { exact: false }).waitFor({ state: 'visible', timeout: config.timeout })

    const header = pageRoot.locator('.el-table__header-wrapper').first()
    for (const text of ['备份编号', '生成时间', '类型', '结果', '是否可恢复', '保存位置', '操作']) {
      await header.getByText(text, { exact: true }).waitFor({ state: 'visible', timeout: config.timeout })
    }

    const bodyText = await pageRoot.innerText({ timeout: config.timeout })
    for (const blockedTerm of ['Cron', 'cron', '脚本路径', 'NAS', 'manifest']) {
      assert.equal(bodyText.includes(blockedTerm), false, `technical term should not be visible: ${blockedTerm}`)
    }
    assert.ok(backupApiEvents.some((event) => event.url.includes('/status') && event.status === 200))
    assert.ok(backupApiEvents.some((event) => event.url.includes('/history/page') && event.status === 200))

    const screenshotDir = path.join(repoRoot, 'output', 'playwright')
    fs.mkdirSync(screenshotDir, { recursive: true })
    await page.screenshot({
      path: path.join(screenshotDir, 'system-backup-plan-readonly.png'),
      fullPage: true
    })
    console.log(
      `PASS: system backup plan read-only real E2E baseUrl=${config.baseUrl} tenant=${config.tenant} username=${config.username}`
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
