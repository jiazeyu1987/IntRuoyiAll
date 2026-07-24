const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for showroom product one-click translate real E2E.')
  }
}

const config = {
  baseUrl: (process.env.SHOWROOM_TRANSLATE_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SHOWROOM_TRANSLATE_TENANT || '测试租户',
  username: process.env.SHOWROOM_TRANSLATE_USERNAME || 'aoteman',
  password: process.env.SHOWROOM_TRANSLATE_PASSWORD || '111111',
  keyword: process.env.SHOWROOM_TRANSLATE_KEYWORD || 'E2E-PUBLISH-1779353074651',
  headed: process.env.SHOWROOM_TRANSLATE_HEADED === '1'
}

function assertTestTenant() {
  assert.equal(config.tenant, '测试租户', 'write-path E2E must use 测试租户')
  assert.equal(config.username, 'aoteman', 'write-path E2E must use aoteman')
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
  await page.goto(`${config.baseUrl}/login?redirect=/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (page.url().includes('/login')) {
    const loginForm = page.locator('.login-form:visible').first()
    const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
    if ((await tenantInput.count()) > 0) {
      await tenantInput.click()
      await tenantInput.fill(config.tenant)
      await tenantInput.press('Enter')
    } else {
      await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
    }
    const textInputs = loginForm.locator('input.el-input__inner')
    await textInputs.nth(0).fill(config.username)
    await loginForm.locator('input[type="password"]').first().fill(config.password)
    const responsePromise = page
      .waitForResponse(
        (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
        { timeout: 60000 }
      )
      .catch(() => null)
    await loginForm.getByRole('button', { name: /登录/ }).click()
    const response = await responsePromise
    if (response) {
      const payload = await response.json().catch(() => null)
      assert.ok(payload && (payload.code === 0 || payload.code === 200), `login failed: ${JSON.stringify(payload)}`)
    }
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByText('产品管理').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function fetchJson(page, relativeUrl, options = {}) {
  return await page.evaluate(
    async ({ url, options }) => {
      const readCacheValue = (key) => {
        const raw = window.localStorage.getItem(key)
        if (!raw) {
          return ''
        }
        try {
          const parsed = JSON.parse(raw)
          return parsed?.c ?? parsed?.v ?? parsed?.value ?? parsed?.data ?? parsed ?? ''
        } catch (error) {
          return raw
        }
      }
      const token = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      const visitTenantId = readCacheValue('visitTenantId')
      const headers = {
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache',
        ...(options.headers || {})
      }
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }
      if (tenantId) {
        headers['tenant-id'] = tenantId
      }
      if (token && visitTenantId) {
        headers['visit-tenant-id'] = visitTenantId
      }
      const response = await fetch(url, {
        ...options,
        headers
      })
      return {
        ok: response.ok,
        status: response.status,
        json: await response.json().catch(() => null)
      }
    },
    { url: relativeUrl, options }
  )
}

async function authSnapshot(page) {
  return await page.evaluate(() => {
    const trimValue = (value) => {
      if (!value) {
        return ''
      }
      const text = String(value)
      return text.length > 40 ? `${text.slice(0, 16)}...${text.slice(-8)}` : text
    }
    return {
      url: location.href,
      localKeys: Object.keys(localStorage),
      sessionKeys: Object.keys(sessionStorage),
      accessTokenRaw: trimValue(localStorage.getItem('ACCESS_TOKEN')),
      tenantIdRaw: trimValue(localStorage.getItem('tenantId')),
      visitTenantIdRaw: trimValue(localStorage.getItem('visitTenantId'))
    }
  })
}

async function searchThroughUi(page) {
  const toolbar = page.locator('.showroom-product-list__toolbar').first()
  await toolbar.waitFor({ state: 'visible', timeout: 30000 })
  const input = toolbar.locator('input[placeholder="搜索产品名称"]').first()
  await input.fill(config.keyword)
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/showroom/product/page') &&
      response.url().includes(encodeURIComponent(config.keyword)),
    { timeout: 60000 }
  )
  await toolbar.getByRole('button', { name: /^查询$/ }).click()
  const response = await responsePromise
  assert.equal(response.status(), 200, `UI search status must be 200, got ${response.status()}`)
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), JSON.stringify(payload))
  const list = payload.data?.list || payload.data?.records || []
  assert.equal(list.length, 1, `keyword must match exactly one product, got ${list.length}`)
  assert.equal(String(list[0].productCode || ''), config.keyword, `matched product must be ${config.keyword}`)
  await page.getByText(list[0].revision?.nameCn || list[0].nameCn).first().waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function startTaskThroughUi(page, startResponses) {
  assert.equal(page.isClosed(), false, 'page must remain open before starting translate task')
  const button = page.locator('.showroom-product-list__actions').getByRole('button', { name: '一键翻译' }).first()
  await button.waitFor({ state: 'visible', timeout: 30000 })
  await button.click()
  const confirmDialog = page.locator('.el-message-box:visible').last()
  await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
  const confirmButton = confirmDialog.getByRole('button', { name: /^(确认|确定)$/ }).last()
  await confirmButton.click()
  const startedAt = Date.now()
  let payload = null
  while (Date.now() - startedAt < 60000) {
    payload = startResponses[startResponses.length - 1] || null
    if (payload) {
      break
    }
    await page.waitForTimeout(500)
  }
  assert.ok(payload, 'start task response must be captured after clicking one-click translate')
  assert.ok(payload && (payload.code === 0 || payload.code === 200), JSON.stringify(payload))
  await page.getByText('一键翻译任务').first().waitFor({ state: 'visible', timeout: 30000 })
  return payload.data
}

async function waitForTaskCompleted(page, statusStates) {
  const startedAt = Date.now()
  let lastState = null
  while (Date.now() - startedAt < 240000) {
    lastState = statusStates[statusStates.length - 1] || lastState
    if (lastState && !lastState.active && !lastState.running && lastState.completedAt) {
      return lastState
    }
    const banner = page.locator('.showroom-product-list__task-banner').filter({ hasText: '一键翻译任务' }).last()
    if (await banner.isVisible().catch(() => false)) {
      const text = await banner.innerText().catch(() => '')
      if (text.includes('已完成') || text.includes('部分失败')) {
        lastState = statusStates[statusStates.length - 1] || lastState
        if (lastState) {
          return lastState
        }
      }
    }
    await page.waitForTimeout(2000)
  }
  throw new Error(`Timed out waiting for translate task: ${JSON.stringify(lastState)}`)
}

async function verifyAdminReadonly(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await login(page)
  await page.locator('.showroom-product-list__actions').getByRole('button', { name: '一键翻译' }).waitFor({
    state: 'visible',
    timeout: 30000
  })
}

async function main() {
  assertTestTenant()
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const diagnostics = []
  page.on('close', () => diagnostics.push('page close'))
  page.on('crash', () => diagnostics.push('page crash'))
  page.on('pageerror', (error) => diagnostics.push(`pageerror: ${error.message}`))
  page.on('requestfailed', (request) => {
    const failure = request.failure()
    diagnostics.push(`requestfailed: ${request.url()} ${failure?.errorText || ''}`)
  })
  try {
    await login(page)
    const statusStates = []
    const statusErrors = []
    const startResponses = []
    page.on('response', async (response) => {
      if (response.url().includes('/admin-api/showroom/product/batch-translate-publish/start')) {
        const payload = await response.json().catch(() => null)
        if (payload) {
          startResponses.push(payload)
        }
        return
      }
      if (!response.url().includes('/admin-api/showroom/product/batch-translate-publish/status')) {
        return
      }
      if (response.status() !== 200) {
        statusErrors.push(`status polling failed: HTTP ${response.status()}`)
        return
      }
      const payload = await response.json().catch(() => null)
      if (payload && (payload.code === 0 || payload.code === 200) && payload.data) {
        statusStates.push(payload.data)
      } else if (payload) {
        statusErrors.push(`status polling returned business error: ${JSON.stringify(payload)}`)
      }
    })
    await searchThroughUi(page)
    assert.equal(page.isClosed(), false, `page closed after search: ${JSON.stringify(diagnostics)}`)
    const startState = await startTaskThroughUi(page, startResponses)
    assert.deepEqual(statusErrors, [], `status polling errors: ${JSON.stringify(statusErrors)}`)
    statusStates.push(startState)
    const completedState = await waitForTaskCompleted(page, statusStates)
    await settle(page)

    assert.equal(completedState.keyword, config.keyword, 'task keyword snapshot must match current filter')
    assert.equal(completedState.matchedCount, 1, 'task must process exactly one filtered product')
    assert.equal(completedState.remainingCount, 0, 'task must have no remaining products')
    assert.ok(completedState.succeededCount + completedState.failedCount >= 1, 'task must record a terminal item outcome')

    console.log(
      JSON.stringify({
        tenant: config.tenant,
        username: config.username,
        keyword: config.keyword,
        startState,
        completedState
      })
    )
  } finally {
    await context.close()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
