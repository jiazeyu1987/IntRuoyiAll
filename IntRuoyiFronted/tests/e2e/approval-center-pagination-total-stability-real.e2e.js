const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_TOTAL_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_TOTAL_TENANT || '芋道源码',
  username: process.env.APPROVAL_CENTER_TOTAL_USERNAME || 'admin',
  password: process.env.APPROVAL_CENTER_TOTAL_PASSWORD || 'admin123',
  targetPath: process.env.APPROVAL_CENTER_TOTAL_TARGET_PATH || '/approval-center/todo',
  pageSize: Number(process.env.APPROVAL_CENTER_TOTAL_PAGE_SIZE || '20'),
  taskDir:
    process.env.APPROVAL_CENTER_TOTAL_TASK_DIR ||
    path.resolve(
      __dirname,
      '..',
      '..',
      '..',
      'doc/tasks/20260720-approval-pagination-total-inconsistent/e2e-artifacts'
    )
}

const artifacts = {
  result: path.join(config.taskDir, 'approval-center-pagination-total-stability-result.json'),
  page1: path.join(config.taskDir, 'approval-center-pagination-total-page1.png'),
  page2: path.join(config.taskDir, 'approval-center-pagination-total-page2.png'),
  failure: path.join(config.taskDir, 'approval-center-pagination-total-failed.png')
}

assert.ok(Number.isInteger(config.pageSize) && config.pageSize > 0, `invalid pageSize=${config.pageSize}`)

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => null)
  await page.waitForTimeout(800)
}

async function login(page) {
  const loginPath = '/index'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(loginPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(loginPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(
    loginPayload && (loginPayload.code === 0 || loginPayload.code === 200),
    `login failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function isTodoGlobalPageResponse(response) {
  if (!response.url().includes('/admin-api/approval-center/tasks/page') || response.request().method() !== 'GET') {
    return false
  }
  const url = new URL(response.url())
  return (
    url.searchParams.get('viewType') === 'TODO' &&
    !url.searchParams.get('moduleCode') &&
    url.searchParams.get('pageSize') === String(config.pageSize)
  )
}

function responsePageNo(response) {
  return new URL(response.url()).searchParams.get('pageNo')
}

async function extractPageData(response) {
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `page api failed: ${JSON.stringify(payload)}`)
  assert.ok(payload.data && typeof payload.data.total === 'number', `page api missing total: ${JSON.stringify(payload)}`)
  assert.ok(Array.isArray(payload.data.list), `page api missing list: ${JSON.stringify(payload)}`)
  return payload.data
}

async function visiblePageNumbers(page) {
  return page.locator('.el-pagination .number').evaluateAll((nodes) =>
    nodes
      .map((node) => Number((node.textContent || '').trim()))
      .filter((value) => Number.isInteger(value))
  )
}

async function activePageNumber(page) {
  const text = await page.locator('.el-pagination .is-active').first().textContent({ timeout: 10000 })
  return Number(String(text || '').trim())
}

async function main() {
  fs.mkdirSync(config.taskDir, { recursive: true })
  const launchOptions = { headless: process.env.APPROVAL_CENTER_TOTAL_HEADED !== '1' }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const responses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    if (!isTodoGlobalPageResponse(response)) return
    const url = new URL(response.url())
    responses.push({
      pageNo: url.searchParams.get('pageNo'),
      pageSize: url.searchParams.get('pageSize'),
      status: response.status(),
      url: response.url()
    })
  })

  try {
    await login(page)
    await page.evaluate(
      ({ pageSize }) => localStorage.setItem('int:list:page-size:approval.center.todo', String(pageSize)),
      { pageSize: config.pageSize }
    )
    const page1ResponsePromise = page.waitForResponse(
      (response) => isTodoGlobalPageResponse(response) && responsePageNo(response) === '1',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const page1Data = await extractPageData(await page1ResponsePromise)
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)
    const page1VisiblePages = await visiblePageNumbers(page)
    await page.screenshot({ path: artifacts.page1, fullPage: true })

    const totalPages = Math.ceil(page1Data.total / config.pageSize)
    assert.ok(
      totalPages >= 2,
      `approval center TODO requires at least two pages, total=${page1Data.total}, pageSize=${config.pageSize}`
    )
    assert.ok(page1VisiblePages.includes(2), `page 1 must expose page 2, visible=${page1VisiblePages.join(',')}`)

    const page2ResponsePromise = page.waitForResponse(
      (response) => isTodoGlobalPageResponse(response) && responsePageNo(response) === '2',
      { timeout: 60000 }
    )
    await page.locator('.el-pagination .number').filter({ hasText: /^2$/ }).first().click()
    const page2Data = await extractPageData(await page2ResponsePromise)
    await page.waitForFunction(() => {
      const active = document.querySelector('.el-pagination .is-active')
      return active && active.textContent && active.textContent.trim() === '2'
    }, { timeout: 60000 })
    await settle(page)
    const page2VisiblePages = await visiblePageNumbers(page)
    await page.screenshot({ path: artifacts.page2, fullPage: true })

    assert.equal(page2Data.total, page1Data.total, 'page 2 total must remain equal to page 1 total')
    assert.equal(Math.ceil(page2Data.total / config.pageSize), totalPages, 'page 2 total pages must remain stable')
    if (totalPages <= 7) {
      for (let pageNo = 1; pageNo <= totalPages; pageNo++) {
        assert.ok(
          page2VisiblePages.includes(pageNo),
          `page 2 must still expose page ${pageNo}, visible=${page2VisiblePages.join(',')}`
        )
      }
    }
    assert.equal(await activePageNumber(page), 2, 'active page must stay on page 2')
    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join('\n')}`)

    const result = {
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetPath: config.targetPath,
      pageSize: config.pageSize,
      page1Total: page1Data.total,
      page2Total: page2Data.total,
      totalPages,
      page1VisiblePages,
      page2VisiblePages,
      page2ListSize: page2Data.list.length,
      responses,
      screenshots: {
        page1: artifacts.page1,
        page2: artifacts.page2
      }
    }
    fs.writeFileSync(artifacts.result, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`PASS: approval center pagination total stability real e2e\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: artifacts.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await context.close().catch(() => null)
    await browser.close().catch(() => null)
  }
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`)
  process.exit(1)
})
