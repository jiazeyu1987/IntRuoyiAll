const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.PROFILE_TODO_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.PROFILE_TODO_E2E_TENANT || '测试租户',
  username: process.env.PROFILE_TODO_E2E_USERNAME || 'aoteman',
  password: process.env.PROFILE_TODO_E2E_PASSWORD || '111111'
}

async function login(page, targetPath) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', targetPath)
  await page.goto(loginUrl.toString(), { waitUntil: 'domcontentloaded' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(
    loginResponse.ok() && [0, 200].includes(loginPayload.code),
    `login failed: HTTP ${loginResponse.status()} ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function isVisibleText(page, text) {
  return await page.getByText(text, { exact: false }).first().isVisible().catch(() => false)
}

async function main() {
  assert.equal(config.tenant, '测试租户', 'Profile unified todo E2E must use 测试租户')
  assert.equal(config.username, 'aoteman', 'Profile unified todo E2E must use aoteman')

  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
    page.setDefaultTimeout(60000)
    await login(page, '/user/profile')
    await page.goto(new URL('/user/profile', config.baseUrl).toString(), { waitUntil: 'domcontentloaded' })

    const table = page.locator('[data-testid="profile-unified-todo-list"]')
    await table.waitFor({ state: 'visible' })
    await page.waitForTimeout(2000)

    assert.equal(await table.count(), 1, '个人工作台必须只有一个统一待办列表')
    assert.equal(await page.locator('.profile-workbench .el-table:visible').count(), 1, '个人工作台只能显示一张表格')
    assert.equal(
      await page.locator('.profile-workbench .unified-list-template[data-table-key="profile.workbench.todo"]').count(),
      1,
      '个人工作台必须通过标准列表模板渲染'
    )
    assert.equal(await isVisibleText(page, '个人信息'), false, '左侧个人信息卡片不应可见')
    assert.equal(await isVisibleText(page, '快速过滤'), true, '标准列表模板必须显示快速过滤')
    assert.equal(await isVisibleText(page, '显示字段'), true, '标准列表模板必须显示字段配置')

    const tableText = await page.locator('.profile-workbench .unified-list-template__table-shell').textContent()
    for (const text of ['任务类型', '来源', '待办详情', '状态/时间', '操作']) {
      assert.ok(tableText.includes(text), `统一待办列表缺少列：${text}`)
    }
    await page.locator('.profile-workbench .table-quick-filter__value .el-select__wrapper').click()
    const dropdownOptions = page.locator('.el-select-dropdown__item:visible')
    await dropdownOptions.filter({ hasText: '行政' }).first().waitFor({ state: 'visible' })
    const optionText = (await dropdownOptions.allTextContents()).join('|')
    for (const text of ['文控', '批记录', '排产', '展厅', '行政']) {
      assert.ok(optionText.includes(text), `任务类型筛选缺少：${text}`)
    }
    await page.keyboard.press('Escape')
    for (const forbidden of ['BPM 审批中心', '审批任务', '待办审批', '已办审批', '我的申请']) {
      assert.equal(await isVisibleText(page, forbidden), false, `个人中心不应显示审批内容：${forbidden}`)
    }
    const loadErrorVisible = await isVisibleText(page, '待办任务加载失败')
    if (loadErrorVisible) {
      console.error(await page.locator('.profile-workbench__alert').textContent())
    }
    assert.equal(loadErrorVisible, false, '真实待办接口不应出现加载失败提示')

    const actionButtons = page.locator('[data-testid="profile-unified-todo-list"] .el-table__body-wrapper button:visible')
    const actionCount = await actionButtons.count()
    if (actionCount > 0) {
      const beforePath = new URL(page.url()).pathname
      await actionButtons.first().click()
      await page.waitForURL((current) => current.pathname !== beforePath, { timeout: 60000 })
      assert.notEqual(new URL(page.url()).pathname, '/user/profile', '待办导航按钮必须进入真实业务页面')
    } else {
      assert.equal(await isVisibleText(page, '当前没有待办任务'), true, '无待办时必须展示空态')
    }

    console.log('PASS: profile unified todo list real E2E')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
