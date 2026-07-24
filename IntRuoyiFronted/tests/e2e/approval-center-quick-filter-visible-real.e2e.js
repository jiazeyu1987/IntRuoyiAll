const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_QUICK_FILTER_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_QUICK_FILTER_TENANT || '测试租户',
  username: process.env.APPROVAL_CENTER_QUICK_FILTER_USERNAME || 'aoteman',
  password: process.env.APPROVAL_CENTER_QUICK_FILTER_PASSWORD || '111111',
  targetPath: '/approval-center/todo',
  taskDir:
    process.env.APPROVAL_CENTER_QUICK_FILTER_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260714-approval-center-quick-filter-regression/e2e-artifacts')
}

const screenshots = {
  page: path.join(config.taskDir, 'approval-center-quick-filter-visible.png'),
  failure: path.join(config.taskDir, 'approval-center-quick-filter-visible-failed.png')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
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
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `login failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function collectQuickFilterState(page) {
  return page.evaluate(() => {
    const form = document.querySelector('[data-testid="approval-center-filter-form"]')
    const isVisible = (element) => {
      if (!(element instanceof HTMLElement)) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    }
    const rectOf = (element) => {
      if (!element) return null
      const rect = element.getBoundingClientRect()
      return {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
        visible: isVisible(element)
      }
    }
    const visibleButtons = form
      ? Array.from(form.querySelectorAll('button'))
          .filter(isVisible)
          .map((button) => (button.textContent || '').replace(/\s+/g, ' ').trim())
      : []
    const toolbarButtons = Array.from(document.querySelectorAll('.approval-center__toolbar button'))
      .filter(isVisible)
      .map((button) => (button.textContent || '').replace(/\s+/g, ' ').trim())
    return {
      form: rectOf(form),
      quickFilter: rectOf(form?.querySelector('.table-quick-filter')),
      quickFilterLabel: form?.querySelector('.table-quick-filter__label')?.textContent?.trim() || '',
      field: rectOf(form?.querySelector('.table-quick-filter__field')),
      operator: rectOf(form?.querySelector('.table-quick-filter__operator')),
      value: rectOf(form?.querySelector('.table-quick-filter__value')),
      visibleButtons,
      toolbarButtons,
      bodyText: document.body.innerText
    }
  })
}

async function main() {
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.APPROVAL_CENTER_QUICK_FILTER_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 }, locale: 'zh-CN' })
  const page = await context.newPage()

  try {
    await login(page)
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await page.getByRole('heading', { name: '审批中心' }).waitFor({ state: 'visible', timeout: 60000 })
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    const state = await collectQuickFilterState(page)
    await page.screenshot({ path: screenshots.page, fullPage: true })

    assert.ok(state.form?.visible, `approval center filter form must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.quickFilter?.visible, `quick filter root must be visible: ${JSON.stringify(state)}`)
    assert.equal(state.quickFilterLabel, '快速过滤', `quick filter label must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.field?.visible, `quick filter field select must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.operator?.visible, `quick filter operator select must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.value?.visible, `quick filter value control must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.visibleButtons.includes('查询'), `quick filter query button must be visible: ${JSON.stringify(state)}`)
    assert.ok(state.visibleButtons.includes('显示字段'), `display field button must stay visible: ${JSON.stringify(state)}`)
    assert.equal(state.visibleButtons.includes('重置列'), false, `reset column button must be hidden by default: ${JSON.stringify(state)}`)
    assert.equal(state.toolbarButtons.includes('刷新'), false, `approval center toolbar must not show refresh: ${JSON.stringify(state)}`)

    process.stdout.write(`PASS: approval center quick filter visible\n${JSON.stringify(state, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: screenshots.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
