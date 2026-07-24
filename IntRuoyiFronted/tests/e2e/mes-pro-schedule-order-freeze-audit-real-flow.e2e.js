const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for MES schedule order freeze audit real E2E.')
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_ORDER_FREEZE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULE_ORDER_FREEZE_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULE_ORDER_FREEZE_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_ORDER_FREEZE_E2E_PASSWORD || '111111',
  headed: process.env.MES_SCHEDULE_ORDER_FREEZE_E2E_HEADED === '1'
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch((error) => {
    if (!String(error?.message || '').includes('Timeout')) {
      throw error
    }
  })
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
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }
  if ((await page.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible').count()) > 0) {
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
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.el-button--primary:visible').first().click()
  ])
  const loginBody = await loginResponse.json()
  assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openScheduleOrderPage(page) {
  const pageResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const response = await pageResponsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `排产工单列表接口业务错误: ${body.msg || body.code}`)
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  return body.data?.list || []
}

function isFalseBit(value) {
  return value === false || value === 0 || value === '0' || value === null || value === undefined
}

function pickCandidates(rows) {
  const candidates = rows.filter((row) => {
    const completedQuantity = Number(row.completedQuantity || 0)
    const reportedQuantity = Number(row.reportedQuantity || 0)
    return row.id && row.code && isFalseBit(row.frozen) && completedQuantity === 0 && reportedQuantity === 0
  })
  assert.ok(
    candidates.length >= 2,
    `BLOCKED: 测试租户至少需要 2 条未冻结、未报工、未完成的真实排产工单；当前候选 ${candidates.length} 条。`
  )
  return candidates.slice(0, 2)
}

async function findRowByCode(page, code) {
  const row = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr', { hasText: code }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  return row
}

async function freezeByRowAction(page, code, reason) {
  const row = await findRowByCode(page, code)
  const [freezeResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/freeze') && response.status() === 200,
      { timeout: 60000 }
    ),
    (async () => {
      await row.getByRole('button', { name: /^冻结$/ }).click()
      const dialog = page.locator('.el-dialog:visible', { hasText: '冻结排产工单' }).first()
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      await dialog.locator('textarea').fill(reason)
      await dialog.getByRole('button', { name: /^冻结$/ }).click()
    })()
  ])
  const body = await freezeResponse.json()
  assert.equal(body.code, 0, `冻结接口业务错误: ${body.msg || body.code}`)
  await page.getByText('排产工单已冻结').waitFor({ state: 'visible', timeout: 30000 })
}

async function verifyTrace(page, code, reason) {
  const row = await findRowByCode(page, code)
  await row.getByRole('button', { name: /^更多$/ }).click()
  const [logResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/operation-log') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.el-dropdown-menu:visible .el-dropdown-menu__item', { hasText: '追溯' }).first().click()
  ])
  const body = await logResponse.json()
  assert.equal(body.code, 0, `追溯接口业务错误: ${body.msg || body.code}`)
  assert.ok(
    Array.isArray(body.data) && body.data.some((item) => item.operationType === 'FREEZE' && item.reason === reason),
    `追溯记录必须包含本次冻结原因: ${JSON.stringify(body.data)}`
  )
  const dialog = page.locator('.el-dialog:visible', { hasText: '排产工单追溯' }).first()
  await dialog.getByText('冻结').first().waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText(reason).first().waitFor({ state: 'visible', timeout: 30000 })
  await page.keyboard.press('Escape')
}

async function deleteByBatchAction(page, code, reason) {
  const row = await findRowByCode(page, code)
  await row.locator('.el-checkbox__input').first().click()
  const [deleteResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/schedule-order/batch-delete') && response.status() === 200,
      { timeout: 60000 }
    ),
    (async () => {
      await page.getByRole('button', { name: /批量删除/ }).click()
      const dialog = page.locator('.el-dialog:visible', { hasText: '删除排产工单' }).first()
      await dialog.waitFor({ state: 'visible', timeout: 30000 })
      await dialog.locator('textarea').fill(reason)
      await dialog.getByRole('button', { name: /^删除$/ }).click()
    })()
  ])
  const body = await deleteResponse.json()
  assert.equal(body.code, 0, `删除接口业务错误: ${body.msg || body.code}`)
  await page.getByText('排产工单已删除').waitFor({ state: 'visible', timeout: 30000 })
}

async function getAuthHeaders(page) {
  const accessToken = await page.evaluate(() => {
    const raw = localStorage.getItem('ACCESS_TOKEN')
    if (!raw) return ''
    const cached = JSON.parse(raw)
    return typeof cached?.v === 'string' ? JSON.parse(cached.v) : cached?.v || raw
  })
  assert.ok(accessToken, '登录后必须存在 ACCESS_TOKEN。')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': config.tenant === '芋道源码' ? '1' : '122'
  }
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    await login(page)
    const rows = await openScheduleOrderPage(page)
    const [freezeTarget, deleteTarget] = pickCandidates(rows)
    const stamp = new Date().toISOString().replace(/[:.]/g, '-')
    const freezeReason = `E2E冻结验证-${stamp}`
    const deleteReason = `E2E删除验证-${stamp}`

    await freezeByRowAction(page, freezeTarget.code, freezeReason)
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
    await verifyTrace(page, freezeTarget.code, freezeReason)
    await deleteByBatchAction(page, deleteTarget.code, deleteReason)

    const logResponse = await page.request.get(
      `${config.baseUrl}/admin-api/mes/pro/schedule-order/operation-log?scheduleOrderId=${deleteTarget.id}`,
      { headers: await getAuthHeaders(page) }
    )
    assert.equal(logResponse.status(), 200, `删除追溯查询 HTTP 异常: ${logResponse.status()}`)
    const logBody = await logResponse.json()
    assert.equal(logBody.code, 0, `删除追溯接口业务错误: ${logBody.msg || logBody.code}`)
    assert.ok(
      Array.isArray(logBody.data) && logBody.data.some((item) => item.operationType === 'DELETE' && item.reason === deleteReason),
      `删除追溯记录必须保留: ${JSON.stringify(logBody.data)}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
