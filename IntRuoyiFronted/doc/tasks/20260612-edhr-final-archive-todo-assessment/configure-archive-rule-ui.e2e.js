const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.EDHR_ARCHIVE_RULE_BASE_URL || 'http://localhost:8081',
  tenant: '测试租户',
  username: 'aoteman',
  password: process.env.EDHR_ARCHIVE_RULE_PASSWORD || 'admin123',
  routeCode: process.env.EDHR_ARCHIVE_RULE_ROUTE_CODE || 'ROUTE-YXN.069.001.1001',
  assigneeUsername: process.env.EDHR_ARCHIVE_RULE_ASSIGNEE_USERNAME || 'aoteman',
  dueMinutes: Number(process.env.EDHR_ARCHIVE_RULE_DUE_MINUTES || 240),
  remark: process.env.EDHR_ARCHIVE_RULE_REMARK || 'P4真实E2E最终归档责任规则'
}

const fillFirstVisible = async (locator, value, label) => {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可见输入框：${label}`)
}

const selectTenant = async (page) => {
  const tenantInput = page.locator('input.el-select__input:visible').first()
  if (await tenantInput.isVisible().catch(() => false)) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible', timeout: 15000 })
    await option.click()
    return
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), config.tenant, '租户名称')
}

const login = async (page) => {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/feedback/edhr-work-task')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return
  await page.locator('input[placeholder="请输入用户名"]:visible').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  if ((await page.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible').count()) > 0) {
    throw new Error('登录启用了验证码，不能无人值守完成真实 UI 配置。')
  }
  await selectTenant(page)
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, '用户名')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, '密码')
  const [response] = await Promise.all([
    page.waitForResponse((res) => res.url().includes('/admin-api/system/auth/login') && res.status() === 200, {
      timeout: 60000
    }),
    page.locator('button:has-text("登录")').first().click()
  ])
  const body = await response.json().catch(() => ({}))
  if (body.code !== 0) {
    throw new Error(`登录失败：${body.msg || body.code}`)
  }
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

const selectOptionByText = async (page, formItem, text) => {
  const input = formItem.locator('input').first()
  await input.click()
  await input.fill('')
  await input.fill(text)
  const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: text })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

const run = async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const apiResponses = []
  page.on('response', async (response) => {
    if (response.url().includes('/admin-api/mes/pro/edhr-work-task/route-archive-rule')) {
      apiResponses.push({
        url: response.url(),
        method: response.request().method(),
        status: response.status(),
        body: await response.text().catch(() => '')
      })
    }
  })
  try {
    await login(page)
    await page.goto(`${config.baseUrl}/mes/pro/feedback/edhr-work-task`, {
      waitUntil: 'networkidle',
      timeout: 60000
    })
    await page.locator('.edhr-work-task-page').waitFor({ state: 'visible', timeout: 60000 })
    await page.getByRole('button', { name: '归档规则' }).click()
    const dialog = page.locator('.el-dialog:visible').filter({ hasText: '归档规则' }).last()
    await dialog.waitFor({ state: 'visible', timeout: 30000 })
    await selectOptionByText(page, dialog.locator('.el-form-item').filter({ hasText: '工艺路线' }).first(), config.routeCode)
    await selectOptionByText(page, dialog.locator('.el-form-item').filter({ hasText: '归档责任人' }).first(), config.assigneeUsername)
    const dueInput = dialog.locator('.el-form-item').filter({ hasText: '处理时限' }).locator('input').first()
    await dueInput.click()
    await dueInput.fill('')
    await dueInput.fill(String(config.dueMinutes))
    const enabledSwitch = dialog.locator('.el-form-item').filter({ hasText: '启用' }).locator('.el-switch').first()
    const switchClass = (await enabledSwitch.getAttribute('class')) || ''
    if (!switchClass.includes('is-checked')) {
      await enabledSwitch.click()
      await page.waitForFunction(
        (element) => element.classList.contains('is-checked'),
        await enabledSwitch.elementHandle(),
        { timeout: 10000 }
      )
    }
    await dialog.locator('textarea').fill(config.remark)
    const saveResponsePromise = page.waitForResponse(
      (res) => res.url().includes('/admin-api/mes/pro/edhr-work-task/route-archive-rule') && res.request().method() === 'POST',
      { timeout: 60000 }
    )
    await dialog.getByRole('button', { name: '保存' }).click()
    const saveResponse = await saveResponsePromise
    const saveBody = await saveResponse.json().catch(() => ({}))
    if (saveResponse.status() !== 200 || saveBody.code !== 0) {
      throw new Error(`保存归档规则失败：status=${saveResponse.status()} body=${JSON.stringify(saveBody)}`)
    }
    await page.locator('.el-message--success').filter({ hasText: '保存成功' }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })
    console.log(JSON.stringify({ status: 'PASS', save: saveBody.data, apiResponses }, null, 2))
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
