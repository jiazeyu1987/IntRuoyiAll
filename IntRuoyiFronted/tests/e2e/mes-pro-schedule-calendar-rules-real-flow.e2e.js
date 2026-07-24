const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for MES schedule calendar rules E2E. Run in a workspace where 'playwright' is installed."
    )
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_CALENDAR_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_SCHEDULE_CALENDAR_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULE_CALENDAR_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_CALENDAR_E2E_PASSWORD || '111111',
  headed: process.env.MES_SCHEDULE_CALENDAR_E2E_HEADED === '1'
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = page.locator('.el-select input[role="combobox"]:visible').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(config.tenant)
    await tenantInput.press('Enter')
    await tenantInput.press('Tab')
  } else {
    await fillFirstVisible(
      page.locator('input[placeholder="请输入租户名称"]'),
      config.tenant,
      'tenant'
    )
  }
  await fillFirstVisible(
    page.locator('input[placeholder="请输入用户名"]'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    page.locator('input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.el-button--primary:visible').first().click()
  ])
  const loginBody = await loginResponse.json()
  assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function authHeaders(page) {
  const cache = await page.evaluate(() => ({
    accessToken: localStorage.getItem('ACCESS_TOKEN'),
    tenantId: localStorage.getItem('tenantId') || localStorage.getItem('TENANT_ID')
  }))
  assert.ok(cache.accessToken, '已登录上下文缺少 ACCESS_TOKEN。')
  let accessToken = cache.accessToken
  try {
    const parsed = JSON.parse(cache.accessToken)
    accessToken =
      typeof parsed?.v === 'string' ? JSON.parse(parsed.v) : parsed?.v || cache.accessToken
  } catch (error) {
    accessToken = cache.accessToken
  }
  const headers = { Authorization: `Bearer ${accessToken}` }
  headers['tenant-id'] = cache.tenantId || (config.tenant === '芋道源码' ? '1' : '122')
  return headers
}

async function apiGetRules(page) {
  const response = await page.request.get(
    `${config.baseUrl}/admin-api/mes/pro/schedule-calendar/rules`,
    {
      headers: await authHeaders(page)
    }
  )
  assert.equal(response.status(), 200, `日历规则接口 HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `日历规则接口业务错误: ${body.msg || body.code}`)
  return body.data
}

async function apiSaveRules(page, rules) {
  const response = await page.request.put(
    `${config.baseUrl}/admin-api/mes/pro/schedule-calendar/rules`,
    {
      headers: {
        ...(await authHeaders(page)),
        'Content-Type': 'application/json'
      },
      data: {
        skipStatutoryHolidays: Boolean(rules.skipStatutoryHolidays),
        weekendRestMode: rules.weekendRestMode,
        dateShiftModeByDate: rules.dateShiftModeByDate || {},
        simulationCurrentDate: rules.simulationCurrentDate
      }
    }
  )
  assert.equal(response.status(), 200, `保存日历规则接口 HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `保存日历规则接口业务错误: ${body.msg || body.code}`)
  return body.data
}

function addDays(dateText, days) {
  const date = dateText ? new Date(`${dateText}T00:00:00+08:00`) : new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

async function openCalendar(page) {
  if (!page.url().includes('/mes/pro/schedule-calendar')) {
    await page.goto(`${config.baseUrl}/mes/pro/schedule-calendar`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
  }
  await page.locator('.schedule-calendar-page').waitFor({ state: 'visible', timeout: 30000 })
  const rulesTab = page.getByRole('tab', { name: '排程规则' })
  await rulesTab.waitFor({ state: 'visible', timeout: 30000 })
  await rulesTab.click()
  await page.getByText('跳过法定节假日').waitFor({ state: 'visible', timeout: 30000 })
  await page.getByText('周末模式').waitFor({ state: 'visible', timeout: 30000 })
  return await apiGetRules(page)
}

async function openRulesTab(page) {
  const rulesTab = page.getByRole('tab', { name: '排程规则' })
  await rulesTab.waitFor({ state: 'visible', timeout: 30000 })
  await rulesTab.click()
  await page.getByText('跳过法定节假日').waitFor({ state: 'visible', timeout: 30000 })
}

async function selectWeekendMode(page, label) {
  const formItem = page.locator('.el-form-item').filter({ hasText: '周末模式' }).first()
  await formItem.locator('.el-select').click()
  await page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: label })
    .first()
    .click()
}

async function verifyCardSelectsWithoutOpeningEditor(page, dateText) {
  const cell = page.locator(`.calendar-cell[data-date="${dateText}"]`).first()
  await cell.waitFor({ state: 'visible', timeout: 30000 })
  await cell.click()
  await page.waitForFunction(
    (targetDate) => {
      const title = document.querySelector('.sidebar-tabs .el-tabs__item.is-active')
      return Boolean(title && title.textContent && title.textContent.includes(targetDate))
    },
    dateText,
    { timeout: 30000 }
  )
  await cell.locator('.calendar-shift-editor').waitFor({ state: 'hidden', timeout: 3000 })
}

async function toggleDayModeFromCalendar(page, dateText, expectedLabelAfterClick) {
  const cell = page.locator(`.calendar-cell[data-date="${dateText}"]`).first()
  const button = cell.locator('.calendar-shift-toggle-button').first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.click()
  await expectButtonText(button, expectedLabelAfterClick)
}

async function expectButtonText(locator, expectedText) {
  await locator.waitFor({ state: 'visible', timeout: 30000 })
  await assertEventually(async () => {
    const text = (await locator.textContent())?.trim()
    assert.equal(text, expectedText, `按钮文案应为 ${expectedText}，实际为 ${text}`)
  })
}

async function assertEventually(assertion) {
  let lastError = null
  for (let attempt = 0; attempt < 20; attempt += 1) {
    try {
      await assertion()
      return
    } catch (error) {
      lastError = error
      await new Promise((resolve) => setTimeout(resolve, 250))
    }
  }
  throw lastError
}

async function clickSaveRules(page) {
  await openRulesTab(page)
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-calendar/rules') &&
      response.request().method() === 'PUT' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '保存规则' }).click()
  const response = await saveResponsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `保存规则失败: ${body.msg || body.code}`)
  await page.getByText('排程规则已更新').waitFor({ state: 'visible', timeout: 30000 })
}

async function main() {
  assert.notEqual(config.tenant, '芋道源码', '真实 E2E 写入/调试不能使用芋道源码租户')
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  let originalRules = null
  let targetDate = null
  try {
    await login(page)
    originalRules = await openCalendar(page)
    assert.equal(originalRules.weekendRestMode, 'SINGLE', '默认周末模式必须为单休。')
    assert.equal(originalRules.skipStatutoryHolidays, false, '默认节假日必须不放假。')
    targetDate = addDays(null, 7)

    await selectWeekendMode(page, '单休')
    await page.getByRole('button', { name: '本月' }).click()
    await verifyCardSelectsWithoutOpeningEditor(page, targetDate)
    await toggleDayModeFromCalendar(page, targetDate, '上班')
    await clickSaveRules(page)

    const savedRules = await apiGetRules(page)
    assert.equal(savedRules.weekendRestMode, 'SINGLE', '保存后周末模式必须仍为单休。')
    assert.equal(savedRules.skipStatutoryHolidays, false, '保存后节假日默认不放假。')
    assert.equal(
      savedRules.dateShiftModeByDate?.[targetDate],
      'REST',
      '手工维护休息日必须写入规则。'
    )

    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          weekendRestMode: savedRules.weekendRestMode,
          skipStatutoryHolidays: savedRules.skipStatutoryHolidays,
          manualRestDate: targetDate
        },
        null,
        2
      )
    )
  } finally {
    if (originalRules) {
      await apiSaveRules(page, originalRules).catch((error) => {
        console.error(`恢复原日历规则失败: ${error.message}`)
      })
    }
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
