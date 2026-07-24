const assert = require('node:assert/strict')
const { chromium } = require('playwright')

const BASE_URL = process.env.MES_REPLAN_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.MES_REPLAN_E2E_TENANT || '测试租户'
const USERNAME = process.env.MES_REPLAN_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.MES_REPLAN_E2E_PASSWORD || '111111'
const TARGET_CODE = process.env.MES_REPLAN_E2E_SCHEDULE_ORDER_CODE || 'SCH-881MO090863-20260612-0001'

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(1000)
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
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }
  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible())) {
    await tenantSelect.click()
    await page.locator('.login-form .el-select__input').first().fill(TENANT)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), TENANT, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), USERNAME, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), PASSWORD, 'password')
  await page.locator('.login-form .el-button--primary').first().click()
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 30000 })
  await settle(page)
}

async function run() {
  const browser = await chromium.launch({ headless: process.env.MES_REPLAN_E2E_HEADED !== '1' })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const apiResponses = []
  const approvalResponses = []
  const pageErrors = []
  const consoleErrors = []
  const toastTexts = []

  page.on('pageerror', (error) => {
    pageErrors.push(String(error))
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/admin-api/bpm/')) {
      approvalResponses.push({
        url,
        status: response.status()
      })
    }
    if (!url.includes('/admin-api/mes/pro/')) {
      return
    }
    if (
      url.includes('/schedule-order/preflight') ||
      url.includes('/auto-schedule/replan/preview') ||
      url.includes('/auto-schedule/replan/apply')
    ) {
      let payload
      try {
        payload = await response.json()
      } catch {
        payload = await response.text()
      }
      apiResponses.push({
        url,
        status: response.status(),
        payload
      })
    }
  })

  try {
    await login(page)
    await page.goto(`${BASE_URL}/mes/pro/schedule-order`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    await settle(page)

    await fillFirstVisible(page.locator('input[placeholder="请输入排产工单号"]'), TARGET_CODE, 'schedule order code')
    await page.locator('.table-quick-filter[data-table-key="mes.pro.scheduleOrder.main"]').getByRole('button', { name: '查询' }).click()
    await settle(page)

    const targetRow = page.locator('.el-table__row').filter({ hasText: TARGET_CODE }).first()
    const targetVisible = await targetRow.isVisible({ timeout: 30000 }).catch(() => false)
    if (!targetVisible) {
      const tableText = await page.locator('.schedule-order-pool').innerText({ timeout: 10000 }).catch(() => '')
      const blocker = {
        status: 'BLOCKED',
        reason: 'MES_REPLAN_TARGET_SCHEDULE_ORDER_MISSING',
        targetCode: TARGET_CODE,
        tableTextSample: tableText.slice(0, 1200),
        pageErrors,
        consoleErrors
      }
      console.log(JSON.stringify(blocker, null, 2))
      throw new Error(`mes_schedule_replan_target_missing:${JSON.stringify(blocker)}`)
    }
    const selectionCell = targetRow.locator('.el-checkbox').first()
    await selectionCell.click()
    await page.getByRole('button', { name: /手动重排/ }).first().click()

    const drawer = page.locator('.el-drawer').filter({ hasText: '排产前检查 / 手动重排' }).first()
    await drawer.waitFor({ state: 'visible', timeout: 30000 })
    const previewButton = drawer.getByRole('button', { name: /生成重排预览|重新检查/ }).first()
    if ((await previewButton.count()) > 0 && await previewButton.isVisible().catch(() => false)) {
      await previewButton.click()
      await settle(page)
    }

    const maybeToasts = page.locator('.el-message')
    const toastCount = await maybeToasts.count()
    for (let index = 0; index < toastCount; index += 1) {
      const text = (await maybeToasts.nth(index).innerText()).trim()
      if (text) toastTexts.push(text)
    }

    const blockedAlert = drawer.getByText('存在阻断问题，不能应用重排。')
    const blockedVisible = await blockedAlert.isVisible().catch(() => false)
    const applyButton = drawer.getByRole('button', { name: /开始重排/ }).first()
    const applyDisabled = await applyButton.isDisabled().catch(() => true)

    if (!blockedVisible && !applyDisabled) {
      const applyResponsePromise = page
        .waitForResponse((response) => response.url().includes('/auto-schedule/replan/apply'), { timeout: 60000 })
        .catch(() => null)
      await applyButton.click()
      await settle(page)
      const confirmApplyButton = page.getByRole('button', { name: /确认应用重排|确定/ }).last()
      if ((await confirmApplyButton.count()) > 0 && await confirmApplyButton.isVisible().catch(() => false)) {
        await confirmApplyButton.click()
      }
      await applyResponsePromise
      await settle(page)
      const postApplyToasts = page.locator('.el-message')
      const postCount = await postApplyToasts.count()
      for (let index = 0; index < postCount; index += 1) {
        const text = (await postApplyToasts.nth(index).innerText()).trim()
        if (text) toastTexts.push(text)
      }
    }

    const materialRequiredHit = JSON.stringify(apiResponses).includes('工单缺少生产用料清单') ||
      toastTexts.some((text) => text.includes('工单缺少生产用料清单'))

    console.log(JSON.stringify({
      status: materialRequiredHit ? 'FAIL' : 'PASS',
      targetCode: TARGET_CODE,
      blockedVisible,
      applyDisabled,
      toastTexts,
      pageErrors,
      consoleErrors,
      apiResponses,
      approvalResponses
    }, null, 2))

    assert.equal(materialRequiredHit, false, 'replan still reports 工单缺少生产用料清单')
    assert.ok(
      apiResponses.some((item) => item.url.includes('/auto-schedule/replan/apply') && item.payload?.code === 0),
      'direct replan apply response was not observed'
    )
    assert.equal(approvalResponses.length, 0, 'manual replan must not call BPM/form-center approval APIs')
  } finally {
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
