const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_FILL_AREA_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_FILL_AREA_TENANT || '测试租户',
  username: process.env.APPROVAL_CENTER_FILL_AREA_USERNAME || 'aoteman',
  password: process.env.APPROVAL_CENTER_FILL_AREA_PASSWORD || '111111',
  targetPath: '/approval-center/todo',
  taskDir:
    process.env.APPROVAL_CENTER_FILL_AREA_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260713-approval-center-fill-list-area/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'approval-center-fill-list-area.png'),
  loginFailed: path.join(config.taskDir, 'approval-center-fill-list-area-login-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(500)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(config.targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }

  const usernameInput = form.locator('input.el-input__inner:not([role="combobox"]):visible').first()
  await usernameInput.fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise.catch(async (error) => {
    await page.screenshot({ path: screenshots.loginFailed, fullPage: true }).catch(() => null)
    throw error
  })
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(loginPayload && (loginPayload.code === 0 || loginPayload.code === 200), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function collectLayoutMetrics(page) {
  return page.evaluate(() => {
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      return rect.width > 0 && rect.height > 0 && Boolean(element.offsetParent)
    }
    const rectOf = (selector) => {
      const element = document.querySelector(selector)
      if (!element) return null
      const rect = element.getBoundingClientRect()
      return {
        left: rect.left,
        right: rect.right,
        width: rect.width,
        height: rect.height
      }
    }
    const queryForm = document.querySelector('[data-testid="approval-center-filter-form"]')
    const queryButtons = queryForm
      ? Array.from(queryForm.querySelectorAll('button'))
          .filter(isVisible)
          .map((button) => (button.textContent || '').replace(/\s+/g, ' ').trim())
      : []
    const bodyText = document.body.innerText || ''
    return {
      shell: rectOf('.unified-list-template__table-shell'),
      table: rectOf('.approval-center__table'),
      header: rectOf('.approval-center__table .el-table__header'),
      body: rectOf('.approval-center__table .el-table__body'),
      queryButtons,
      hasDisplayFieldControl: bodyText.includes('显示字段'),
      hasResetColumnControl: bodyText.includes('重置列'),
      hasResetQueryButton: queryButtons.includes('重置')
    }
  })
}

async function main() {
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.APPROVAL_CENTER_FILL_AREA_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))

  try {
    await login(page)
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    await page.screenshot({ path: screenshots.page, fullPage: true })

    const metrics = await collectLayoutMetrics(page)
    assert.ok(metrics.shell && metrics.table && metrics.header && metrics.body, `missing table metrics: ${JSON.stringify(metrics)}`)
    assert.ok(metrics.header.width >= metrics.shell.width - 16, `table header must fill shell width: ${JSON.stringify(metrics)}`)
    assert.ok(metrics.body.width >= metrics.shell.width - 16, `table body must fill shell width: ${JSON.stringify(metrics)}`)
    assert.ok(metrics.queryButtons.includes('查询'), `query toolbar must keep quick-filter query button: ${JSON.stringify(metrics)}`)
    assert.ok(metrics.queryButtons.includes('显示字段'), `standard display-field control must stay visible: ${JSON.stringify(metrics)}`)
    assert.equal(metrics.queryButtons.includes('重置列'), false, `standard reset-column control must be hidden by default: ${JSON.stringify(metrics)}`)
    assert.equal(metrics.hasDisplayFieldControl, true, 'display field control must stay visible in the standard template')
    assert.equal(metrics.hasResetColumnControl, false, 'reset column control must be hidden by default in the standard template')
    assert.equal(metrics.hasResetQueryButton, false, 'duplicate reset query button must be removed from the visible page')
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join(' || ')}`)

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      metrics,
      screenshots
    }
    fs.writeFileSync(path.join(config.taskDir, 'approval-center-fill-list-area-result.json'), `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`approval-center fill list area real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
