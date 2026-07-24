const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const ZH = {
  tenant: '\u6d4b\u8bd5\u79df\u6237',
  title: '\u6392\u4ea7\u5de5\u5355',
  action: '\u8bbe\u7f6e\u4ea4\u671f',
  dialogTitle: '\u8bbe\u7f6e\u627f\u8bfa\u4ea4\u671f',
  reason: '\u771f\u5b9eE2E\u9a8c\u8bc1\u518d\u6b21\u8bbe\u7f6e\u627f\u8bfa\u4ea4\u671f',
  success: '\u627f\u8bfa\u4ea4\u671f\u5df2\u66f4\u65b0'
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_ORDER_E2E_BASE_URL || 'http://localhost:8081').replace(/\/$/, ''),
  tenant: process.env.MES_SCHEDULE_ORDER_E2E_TENANT || ZH.tenant,
  username: process.env.MES_SCHEDULE_ORDER_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_ORDER_E2E_PASSWORD || '111111',
  headless: process.env.MES_SCHEDULE_ORDER_E2E_HEADLESS !== '0',
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
}

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  const allowedHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])
  assert.ok(allowedHosts.has(parsed.hostname), `E2E only supports local base URL, got ${baseUrl}`)
}

function nextPromiseDate(current) {
  const base = new Date(`${current || new Date().toISOString().slice(0, 10)}T00:00:00`)
  if (Number.isNaN(base.getTime())) {
    throw new Error(`Invalid current promise date: ${current}`)
  }
  base.setDate(base.getDate() + 1)
  return `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, '0')}-${String(
    base.getDate()
  ).padStart(2, '0')}`
}

function dayOf(dateText) {
  return String(Number(dateText.slice(8, 10)))
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

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponse = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(config.tenant)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.click()
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await tenantResponse
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, form)
  await fillFirstVisible(form.locator('input.el-input__inner:not([role="combobox"])'), config.username, 'username')
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /\u767b\u5f55/ }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000 })
}

async function waitForScheduleOrderPage(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText(ZH.title, { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `schedule-order page API failed: ${body.msg || body.code}`)
  assert.ok(Array.isArray(body.data?.list), 'schedule-order page API must return a list')
  return body.data.list
}

async function openVisiblePromiseDateDialog(page) {
  const buttons = page
    .locator('.schedule-order-pool .el-table__fixed-right, .schedule-order-pool .el-table__body-wrapper')
    .getByRole('button', { name: ZH.action })
  await buttons.first().waitFor({ state: 'visible', timeout: 30000 })
  const buttonCount = await buttons.count()
  let button
  for (let index = 0; index < buttonCount; index += 1) {
    const candidate = buttons.nth(index)
    if ((await candidate.isVisible()) && (await candidate.isEnabled())) {
      button = candidate
      break
    }
  }
  assert.ok(button, '当前可见排产工单没有可用的设置交期按钮，无法验证再次设置承诺交期')
  await button.click()
  const dialog = page.locator('.el-dialog').filter({ hasText: ZH.dialogTitle }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  const code = (await dialog.locator('.el-form-item').filter({ hasText: /\u6392\u4ea7\u7f16\u7801/ }).first().textContent())
    ?.replace(/\s+/g, '')
    .replace(ZH.dialogTitle, '')
    .replace(/\u6392\u4ea7\u7f16\u7801/, '')
  assert.ok(code, 'promise date dialog must show schedule order code')
  return { dialog, code }
}

async function expectInputValue(input, expected, label) {
  const actual = await input.inputValue()
  assert.equal(actual, expected, `${label} must contain ${expected}, got ${actual}`)
}

async function submitPromiseDate(page, dialog, newPromiseDate) {
  const dateInput = dialog.locator('.el-date-editor input').first()
  await dateInput.waitFor({ state: 'visible', timeout: 30000 })
  await dateInput.click()
  const targetDay = dayOf(newPromiseDate)
  const dateCell = page
    .locator('.el-picker-panel:visible .el-date-table td.available:not(.prev-month):not(.next-month)')
    .filter({ hasText: new RegExp(`^\s*${targetDay}\s*$`) })
    .first()
  await dateCell.waitFor({ state: 'visible', timeout: 30000 })
  await dateCell.click()
  await expectInputValue(dateInput, newPromiseDate, 'promise date input')

  const reasonInput = dialog.locator('textarea.el-textarea__inner').first()
  try {
    await reasonInput.waitFor({ state: 'visible', timeout: 30000 })
    await reasonInput.click()
    await reasonInput.fill(ZH.reason)
    const reasonValue = await reasonInput.inputValue()
    assert.equal(reasonValue, ZH.reason, 'promise date dialog reason must be typed through the UI')
  } catch (error) {
    const diagnostics = await dialog.evaluate((node) => ({
      text: node.textContent,
      html: node.innerHTML,
      inputs: Array.from(node.querySelectorAll('input, textarea')).map((item) => ({
        tag: item.tagName,
        type: item.getAttribute('type'),
        value: item.value,
        placeholder: item.getAttribute('placeholder'),
        className: item.className
      }))
    }))
    throw new Error(`reason input not found in promise date dialog: ${JSON.stringify(diagnostics)}`)
  }

  let updatePayload
  const updateResponsePromise = page.waitForResponse(
    (response) => {
      if (
        response.url().includes('/admin-api/mes/pro/schedule-order/update') &&
        response.request().method() === 'PUT'
      ) {
        updatePayload = JSON.parse(response.request().postData() || '{}')
        return true
      }
      return false
    },
    { timeout: 60000 }
  )
  const refreshedListPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') &&
      response.request().method() === 'GET' &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await dialog.getByRole('button', { name: /^\u4fdd\u5b58$/ }).click()
  const updateResponse = await updateResponsePromise
  assert.equal(updatePayload.promiseDate, newPromiseDate, 'schedule-order update payload must use selected promiseDate')
  assert.equal(updatePayload.reason, ZH.reason, 'schedule-order update payload must include typed reason')
  const payload = await updateResponse.json()
  assert.equal(updateResponse.status(), 200, `schedule-order update HTTP failed: ${updateResponse.status()}`)
  assert.equal(payload.code, 0, `schedule-order update business failed: ${payload.msg || payload.code}`)
  const refreshedListResponse = await refreshedListPromise
  const refreshedListPayload = await refreshedListResponse.json()
  assert.equal(
    refreshedListPayload.code,
    0,
    `schedule-order refreshed page failed: ${refreshedListPayload.msg || refreshedListPayload.code}`
  )
  await page.getByText(ZH.success, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
  return refreshedListPayload.data?.list || []
}

async function main() {
  assertLocalOnly(config.baseUrl)
  assert.equal(config.tenant, ZH.tenant, 'real write E2E must use 测试租户')
  assert.equal(config.username, 'aoteman', 'real write E2E must use aoteman')

  const browser = await chromium.launch({
    headless: config.headless,
    ...(config.executablePath ? { executablePath: config.executablePath } : {})
  })
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)
    await login(page)
    const list = await waitForScheduleOrderPage(page)
    const { dialog, code } = await openVisiblePromiseDateDialog(page)
    const scheduleOrder = list.find((item) => item.id && item.code === code && item.frozen === false)
    assert.ok(scheduleOrder, `测试租户当前可见行 ${code} 未在接口未冻结排产工单中找到，无法验证再次设置承诺交期`)
    const newPromiseDate = nextPromiseDate(scheduleOrder.promiseDate)
    const refreshedList = await submitPromiseDate(page, dialog, newPromiseDate)
    const after = refreshedList.find((item) => String(item.id) === String(scheduleOrder.id))
    assert.ok(after, `schedule order ${scheduleOrder.id} must remain in refreshed list after UI save`)
    assert.equal(after.promiseDate, newPromiseDate, 'schedule order promiseDate must be updated after UI save')
    assert.equal(after.priorityNo, scheduleOrder.priorityNo, 'promise date reset must preserve priorityNo')
    assert.equal(after.remark || '', scheduleOrder.remark || '', 'promise date reset must preserve remark')
    console.log(
      `PASS: MES schedule order promise date reset real E2E id=${scheduleOrder.id} code=${scheduleOrder.code} promiseDate=${newPromiseDate}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
