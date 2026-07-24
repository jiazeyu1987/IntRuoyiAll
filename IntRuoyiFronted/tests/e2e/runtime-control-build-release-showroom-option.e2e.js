const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = (process.env.RUNTIME_CONTROL_E2E_BASE_URL || 'http://127.0.0.1:8095').replace(/\/+$/, '')
const TENANT = process.env.RUNTIME_CONTROL_E2E_TENANT || '测试租户'
const USERNAME = process.env.RUNTIME_CONTROL_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.RUNTIME_CONTROL_E2E_PASSWORD || 'admin123'
const HEADLESS = process.env.RUNTIME_CONTROL_E2E_HEADLESS !== 'false'

function assertTestTenant() {
  assert.equal(TENANT, '测试租户', 'runtime-control E2E must use the 测试租户 tenant')
  assert.equal(USERNAME, 'aoteman', 'runtime-control E2E must use the aoteman test account')
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function fillFirstVisible(pageOrLocator, selector, value, label) {
  const locator = pageOrLocator.locator(selector)
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`missing visible ${label}: ${selector}`)
}

async function selectTenant(page, tenantName) {
  const tenantSelect = page.locator('.login-form:visible .el-select').first()
  if ((await tenantSelect.count()) === 0 || !(await tenantSelect.isVisible())) {
    await fillFirstVisible(page, 'input[placeholder="请输入租户名称"]', tenantName, 'tenant input')
    return
  }

  await tenantSelect.click()
  const input = page.locator('.login-form:visible .el-select__input').first()
  await input.fill(tenantName)
  await page.keyboard.press('Enter')
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  await selectTenant(page, TENANT)
  await fillFirstVisible(page, 'input[placeholder="请输入用户名"]', USERNAME, 'username input')
  await fillFirstVisible(page, 'input[placeholder="请输入密码"]', PASSWORD, 'password input')

  const loginError = page.locator('.el-message, .el-notification').filter({ hasText: /错误|失败|不存在|无效|密码/ }).last()
  await Promise.all([
    page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/system/auth/login') &&
          response.request().method() === 'POST',
        { timeout: 30000 }
      )
      .catch((error) => {
        throw new Error(`login request did not complete: ${error.message}`)
      }),
    page.locator('.login-form:visible .el-button--primary').first().click()
  ])

  const loggedIn = await Promise.race([
    page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 }).then(() => true),
    loginError.waitFor({ state: 'visible', timeout: 30000 }).then(() => false)
  ])
  if (!loggedIn) {
    const message = await loginError.textContent().catch(() => '')
    throw new Error(`test tenant login failed: ${message || 'unknown login error'}`)
  }

  await settle(page)
}

async function openRuntimeControl(page) {
  await page.goto(`${BASE_URL}/infra/monitors/runtime-control`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  const buildButton = page.getByRole('button', { name: '构建发布包' }).first()
  await buildButton.waitFor({ state: 'visible', timeout: 30000 }).catch((error) => {
    throw new Error(`missing build-release button on runtime-control page: ${error.message}`)
  })
  await buildButton.click()

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '构建发布包' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 10000 })
  return dialog
}

async function isShowroomChecked(dialog) {
  const checkbox = dialog.locator('.el-checkbox').filter({ hasText: '发布展厅构筑包' }).first()
  await checkbox.waitFor({ state: 'visible', timeout: 10000 })
  const input = checkbox.locator('input[type="checkbox"]').first()
  return input.isChecked()
}

async function clickShowroomCheckbox(dialog) {
  const checkbox = dialog.locator('.el-checkbox').filter({ hasText: '发布展厅构筑包' }).first()
  await checkbox.click()
}

async function waitForShowroomConfirm(page) {
  const confirm = page
    .locator('.el-message-box:visible')
    .filter({ hasText: '当前选中的展厅构筑包会覆盖服务器的展厅数据，是否继续？' })
    .last()
  await confirm.waitFor({ state: 'visible', timeout: 10000 })
  return confirm
}

async function waitUntilUnchecked(dialog) {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    if (!(await isShowroomChecked(dialog))) {
      return
    }
    await dialog.page().waitForTimeout(100)
  }
  throw new Error('showroom package checkbox did not roll back to unchecked after cancel')
}

;(async () => {
  assertTestTenant()

  const browser = await chromium.launch({ headless: HEADLESS })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const actionRequests = []

  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/infra/runtime-control/action') &&
      request.method() === 'POST'
    ) {
      actionRequests.push(request.postData() || '')
    }
  })

  try {
    await login(page)
    const dialog = await openRuntimeControl(page)

    assert.equal(await isShowroomChecked(dialog), false, 'showroom package checkbox must default unchecked')

    await clickShowroomCheckbox(dialog)
    let confirm = await waitForShowroomConfirm(page)
    await confirm.getByRole('button', { name: '取消' }).click()
    await confirm.waitFor({ state: 'hidden', timeout: 10000 })
    await waitUntilUnchecked(dialog)

    await clickShowroomCheckbox(dialog)
    confirm = await waitForShowroomConfirm(page)
    await confirm.getByRole('button', { name: '确定' }).click()
    await confirm.waitFor({ state: 'hidden', timeout: 10000 })
    assert.equal(await isShowroomChecked(dialog), true, 'showroom package checkbox must stay checked after confirm')

    assert.equal(actionRequests.length, 0, 'dialog E2E must not submit or start build-release operation')
    console.log(`PASS: runtime-control build-release showroom option baseUrl=${BASE_URL} tenant=${TENANT}`)
  } finally {
    await browser.close()
  }
})().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
