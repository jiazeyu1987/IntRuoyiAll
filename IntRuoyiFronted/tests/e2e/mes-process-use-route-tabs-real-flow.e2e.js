const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_PROCESS_USE_ROUTE_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_PROCESS_USE_ROUTE_E2E_TENANT || '测试租户',
  username: process.env.MES_PROCESS_USE_ROUTE_E2E_USERNAME || 'aoteman',
  password: process.env.MES_PROCESS_USE_ROUTE_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_PROCESS_USE_ROUTE_E2E_HEADED === '1'
}

if (config.tenant !== '测试租户' || config.username !== 'aoteman') {
  throw new Error(
    `MES process use route E2E must use 测试租户/aoteman, got ${config.tenant}/${config.username}`
  )
}

function routePath(url) {
  const marker = '/admin-api/'
  const index = url.indexOf(marker)
  if (index < 0) return ''
  return url.slice(index + marker.length).split('?')[0]
}

function collectForbiddenMasterWrites(page) {
  const writes = []
  page.on('request', (request) => {
    const method = request.method()
    if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) return
    const path = routePath(request.url())
    if (
      /^mes\/pro\/route\/(create|update|delete)/.test(path) ||
      /^mes\/pro\/route-process\/(create|update|delete)/.test(path)
    ) {
      writes.push(`${method} ${path}`)
    }
  })
  return writes
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(300)
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

async function selectLoginTenant(page, loginForm) {
  const tenantSelectInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantSelectInput.count()) > 0 && (await tenantSelectInput.isVisible())) {
    await tenantSelectInput.click()
    await tenantSelectInput.fill(config.tenant)
    await tenantSelectInput.press('Enter')
    return
  }
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入租户名称"]'),
    config.tenant,
    'tenant'
  )
}

async function login(page, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  await selectLoginTenant(page, loginForm)
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
    { timeout: 30000 }
  )
  await loginForm.locator('.el-button--primary').first().click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  assert.equal(payload.code, 0, `login failed: ${payload.msg || JSON.stringify(payload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function ensureMenuEntry(page, parentText, childText) {
  const child = page.getByText(childText, { exact: true }).first()
  if ((await child.count()) > 0 && (await child.isVisible())) return

  const parent = page.getByText(parentText, { exact: true }).first()
  if ((await parent.count()) > 0 && (await parent.isVisible())) {
    await parent.click()
    await settle(page)
    if ((await child.count()) > 0 && (await child.isVisible())) return
  }

  await page.waitForFunction(
    (text) =>
      Array.from(document.querySelectorAll('.v-menu__title')).some(
        (element) => element.textContent && element.textContent.trim() === text
      ),
    childText,
    { timeout: 30000 }
  )
}

async function assertMenuVisible(page) {
  await page.goto(`${config.baseUrl}/index`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await ensureMenuEntry(page, '智能排产', '工艺流程排产配置')
  await ensureMenuEntry(page, 'eDHR批记录', '工艺流程批记录配置')
}

async function assertNoLoadError(page, context) {
  const alert = page.locator('.route-flow-config-panel-alert.el-alert--error:visible').first()
  if ((await alert.count()) > 0 && (await alert.isVisible())) {
    throw new Error(`${context} failed: ${await alert.innerText()}`)
  }
}

async function waitForRouteRows(page, context) {
  await page.locator('.route-flow-config-panel-page').waitFor({ state: 'visible', timeout: 30000 })
  const firstRow = page.locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row').first()
  await firstRow.waitFor({ state: 'visible', timeout: 30000 }).catch(async () => {
    await assertNoLoadError(page, context)
    throw new Error(`${context} has no visible source route rows`)
  })
  await assertNoLoadError(page, context)
}

async function openFirstRouteConfig(page, context) {
  await waitForRouteRows(page, context)
  const firstRow = page.locator('.route-flow-config-panel-table .el-table__body-wrapper .el-table__row').first()
  const codeButton = firstRow.locator('button').first()
  const routeCode = (await codeButton.innerText()).trim()
  assert.ok(routeCode, `${context} first route code is empty`)
  await codeButton.click()
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const firstProcessRow = dialog.locator('.route/flow-config-table .el-table__body-wrapper .el-table__row').first()
  await firstProcessRow.waitFor({ state: 'visible', timeout: 30000 }).catch(async () => {
    await assertNoLoadError(page, `${context} config`)
    throw new Error(`${context} source route ${routeCode} has no visible process config rows`)
  })
  await assertNoLoadError(page, `${context} config`)
  return { dialog, routeCode }
}

async function saveAndAssert(page, dialog, context) {
  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/flow-config/save') &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await dialog.getByRole('button', { name: '保存用途配置' }).click()
  const saveResponse = await saveResponsePromise
  assert.equal(saveResponse.status(), 200, `${context} save HTTP status must be 200`)
  const payload = await saveResponse.json()
  assert.equal(payload.code, 0, `${context} save failed: ${payload.msg || JSON.stringify(payload)}`)
  await page.getByText('用途配置保存成功').first().waitFor({ state: 'visible', timeout: 10000 })
  await settle(page)
}

async function chooseFirstBatchReport(page, dialog) {
  const reportSelect = dialog.locator('.route-flow-config-panel-report-select').first()
  await reportSelect.waitFor({ state: 'visible', timeout: 30000 })
  await reportSelect.click()
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item:not(.is-disabled)')
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const optionText = (await option.innerText()).trim()
  assert.ok(optionText, '批记录报表下拉没有可选报表')
  await option.click()
  return optionText
}

async function filterRouteByCode(page, routeCode) {
  const codeInput = page.locator('input[placeholder="请输入工艺路线编码"]').first()
  await codeInput.fill(routeCode)
  const pageResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/route/page') &&
      response.request().method() === 'GET',
    { timeout: 30000 }
  )
  await page.getByRole('button', { name: '搜索' }).first().click()
  const pageResponse = await pageResponsePromise
  assert.equal(pageResponse.status(), 200, 'route page search HTTP status must be 200')
  await waitForRouteRows(page, `route ${routeCode} refresh`)
}

async function runUseRouteFlow(page, route, uniqueRemark) {
  await page.goto(`${config.baseUrl}${route.path}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)

  const { dialog, routeCode } = await openFirstRouteConfig(page, route.title)
  if (route.useType === 'BATCH') {
    await chooseFirstBatchReport(page, dialog)
    assert.equal(
      await dialog.locator('input[placeholder="请输入用途备注"]').count(),
      0,
      `${route.title} must not display process remark input`
    )
  } else {
    await fillFirstVisible(
      dialog.locator('input[placeholder="请输入用途备注"]'),
      uniqueRemark,
      `${route.title} remark`
    )
  }
  await saveAndAssert(page, dialog, route.title)
  await dialog.getByRole('button', { name: '关闭' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })

  await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await settle(page)
  await filterRouteByCode(page, routeCode)
  const reopened = await openFirstRouteConfig(page, `${route.title} refreshed`)
  if (route.useType === 'BATCH') {
    assert.equal(
      await reopened.dialog.locator('input[placeholder="请输入用途备注"]').count(),
      0,
      `${route.title} must keep process remark input hidden after refresh`
    )
  } else {
    const persistedRemark = await reopened.dialog
      .locator('input[placeholder="请输入用途备注"]')
      .first()
      .inputValue()
    assert.equal(persistedRemark, uniqueRemark, `${route.title} remark must persist after refresh`)
  }
  await reopened.dialog.getByRole('button', { name: '关闭' }).click()
  await reopened.dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return routeCode
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
  const forbiddenMasterWrites = collectForbiddenMasterWrites(page)
  const suffix = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14)

  try {
    await login(page, '/index')
    await assertMenuVisible(page)
    const scheduleRouteCode = await runUseRouteFlow(
      page,
      {
        title: '工艺流程排产配置',
        path: '/mes/pro/route?tab=schedule-config',
        useType: 'SCHEDULE'
      },
      `E2E排产用途${suffix}`
    )
    const batchRouteCode = await runUseRouteFlow(
      page,
      {
        title: '工艺流程批记录配置',
        path: '/mes/pro/route?tab=batch-record-config',
        useType: 'BATCH'
      },
      `E2E批记录用途${suffix}`
    )

    assert.deepEqual(forbiddenMasterWrites, [], `original route/process CRUD was called: ${forbiddenMasterWrites.join(', ')}`)
    console.log(
      `PASS: MES process use route tabs real flow. scheduleRoute=${scheduleRouteCode}, batchRoute=${batchRouteCode}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
