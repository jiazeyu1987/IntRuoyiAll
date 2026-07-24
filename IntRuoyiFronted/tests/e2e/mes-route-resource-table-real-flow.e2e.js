const assert = require('node:assert/strict')
const path = require('node:path')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES route resource E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_ROUTE_RESOURCE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_ROUTE_RESOURCE_E2E_TENANT || '测试租户',
  username: process.env.MES_ROUTE_RESOURCE_E2E_USERNAME || 'aoteman',
  password: process.env.MES_ROUTE_RESOURCE_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_ROUTE_RESOURCE_E2E_HEADED === '1'
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
  await page.goto(config.baseUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), config.password, 'password')
  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    loginForm.locator('.el-button--primary').first().click()
  ])
  const loginBody = await loginResponse.json()
  if (loginBody.code !== 0) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  if (page.url().includes('/login')) {
    await page.goto(`${config.baseUrl}/mes/pro/route`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openResourceWorkbench(page) {
  await page.goto(`${config.baseUrl}/mes/pro/route`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.waitForSelector('.route-view-switch', { timeout: 30000 })

  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route-resource/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.locator('.route-view-switch').getByText('资源大表').click()
  ])
  await page.waitForSelector('.route-resource-workbench', { timeout: 30000 })
  await page.waitForSelector('text=单台产能/h', { timeout: 10000 })
  await page.waitForSelector('text=预算/日', { timeout: 10000 })
}

async function searchResource(page, keyword) {
  const workbench = page.locator('.route-resource-workbench')
  await workbench.locator('input[placeholder="产品 / 路线 / 工序 / 设备"]').fill(keyword)
  await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/mes/pro/route-resource/page') && response.status() === 200,
      { timeout: 60000 }
    ),
    workbench.getByRole('button', { name: /搜索/ }).click()
  ])
  await settle(page)
}

async function getWorkerRow(page, processName) {
  const rows = page.locator('.resource-table tr.el-table__row').filter({ hasText: processName }).filter({ hasText: '人工' })
  const count = await rows.count()
  assert.ok(count > 0, `资源大表缺少人工工序行: ${processName}`)
  return rows.first()
}

async function verifyResourceWorkbenchIsReadOnly(page) {
  await searchResource(page, '穿显影环')
  const row = await getWorkerRow(page, '穿显影环')
  const workbench = page.locator('.route-resource-workbench')
  assert.equal(await row.locator('.el-input-number input').count(), 0, '人工资源行不得提供数量编辑框。')
  assert.equal(await row.getByRole('button', { name: /^保存$/ }).count(), 0, '人工资源行不得提供保存按钮。')
  assert.equal(await workbench.locator('.el-input-number input').count(), 0, '资源大表不得出现路线级资源编辑控件。')
  assert.equal(await workbench.getByRole('button', { name: /^保存$/ }).count(), 0, '资源大表不得出现路线级资源保存按钮。')
}

async function verifyMachineProcessCapacityConsistency(page) {
  const rows = await page.evaluate(async () => {
    const readCacheValue = (key) => {
      const raw = localStorage.getItem(key)
      if (!raw) {
        return undefined
      }
      const readNestedJson = (value) => {
        try {
          return JSON.parse(value)
        } catch (error) {
          return value
        }
      }
      try {
        const parsed = JSON.parse(raw)
        if (parsed && Object.prototype.hasOwnProperty.call(parsed, 'v')) {
          return readNestedJson(parsed.v)
        }
        if (parsed && Object.prototype.hasOwnProperty.call(parsed, 'value')) {
          return readNestedJson(parsed.value)
        }
        return parsed
      } catch (error) {
        return raw
      }
    }
    const accessToken = readCacheValue('ACCESS_TOKEN')
    const tenantId = readCacheValue('tenantId')
    if (!accessToken || !tenantId) {
      throw new Error('已登录上下文缺少 ACCESS_TOKEN 或 tenantId。')
    }
    const response = await fetch('/admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=200&keyword=A03388', {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
    })
    if (!response.ok) {
      throw new Error(`资源大表 API 返回 HTTP ${response.status}`)
    }
    const body = await response.json()
    if (body.code !== 0) {
      throw new Error(`资源大表 API 返回业务错误: ${body.msg || body.code}`)
    }
    return body.data.list
  })

  const groups = new Map()
  for (const row of rows.filter((item) => item.resourceType === 'MACHINE' && item.machineryCode === 'A03388')) {
    const key = `${row.machineryCode}:${row.processCode}:${row.processName}`
    const values = groups.get(key) || new Set()
    values.add(String(row.machineryStandardHourlyCapacity))
    groups.set(key, values)
  }

  assert.ok(groups.size > 0, '资源大表缺少 A03388 设备行，无法验证同设备同工序产能一致性。')
  for (const [key, values] of groups.entries()) {
    assert.equal(values.size, 1, `${key} 存在多个设备工序产能: ${Array.from(values).join(', ')}`)
  }
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    acceptDownloads: true
  })
  const page = await context.newPage()
  const pageErrors = []
  const consoleErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      consoleErrors.push(`${message.type()}: ${message.text()}`)
    }
  })

  try {
    await login(page)
    await openResourceWorkbench(page)
    await verifyResourceWorkbenchIsReadOnly(page)
    await verifyMachineProcessCapacityConsistency(page)
    assert.deepEqual(pageErrors, [])
    await page.screenshot({
      path: path.resolve(__dirname, '../../output/playwright/mes-route-resource-table-real-flow.png'),
      fullPage: true
    })
    console.log('PASS: MES route resource table real UI E2E')
  } catch (error) {
    console.error('E2E_FAILURE_URL:', page.url())
    console.error('E2E_PAGE_ERRORS:', JSON.stringify(pageErrors))
    console.error('E2E_CONSOLE_ERRORS:', JSON.stringify(consoleErrors.slice(-20)))
    console.error('E2E_FAILURE_BODY:', (await page.locator('body').innerText().catch((innerError) => String(innerError))).slice(0, 4000))
    await page.screenshot({
      path: path.resolve(__dirname, '../../output/playwright/mes-route-resource-table-failure.png'),
      fullPage: true
    }).catch(() => {})
    throw error
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
