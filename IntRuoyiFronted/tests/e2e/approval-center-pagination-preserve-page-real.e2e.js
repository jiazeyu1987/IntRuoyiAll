const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.APPROVAL_CENTER_PAGINATION_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.APPROVAL_CENTER_PAGINATION_TENANT || '测试租户',
  username: process.env.APPROVAL_CENTER_PAGINATION_USERNAME || 'aoteman',
  password: process.env.APPROVAL_CENTER_PAGINATION_PASSWORD || '111111',
  targetPath: process.env.APPROVAL_CENTER_PAGINATION_TARGET_PATH || '/approval-center/todo?moduleCode=EDHR',
  pageSize: Number(process.env.APPROVAL_CENTER_PAGINATION_PAGE_SIZE || '20'),
  taskDir:
    process.env.APPROVAL_CENTER_PAGINATION_TASK_DIR ||
    path.resolve(__dirname, '..', '..', '..', 'doc/tasks/20260720-edhr-approval-pagination-reset/e2e-artifacts')
}

const artifacts = {
  result: path.join(config.taskDir, 'approval-center-pagination-result.json'),
  screenshot: path.join(config.taskDir, 'approval-center-pagination-page3.png'),
  failure: path.join(config.taskDir, 'approval-center-pagination-failed.png')
}

const pathToViewType = {
  '/approval-center/todo': 'TODO',
  '/approval-center/done': 'DONE',
  '/approval-center/my-initiated': 'MY_INITIATED',
  '/approval-center/cc': 'CC'
}

const tableKeyByViewType = {
  TODO: 'approval.center.todo',
  DONE: 'approval.center.done',
  MY_INITIATED: 'approval.center.myInitiated',
  CC: 'approval.center.cc'
}

const targetUrl = new URL(config.targetPath, config.baseUrl)
const expectedViewType = pathToViewType[targetUrl.pathname]
const expectedModuleCode = targetUrl.searchParams.get('moduleCode') || ''
assert.ok(expectedViewType, `unsupported approval center target path: ${config.targetPath}`)
assert.ok(Number.isInteger(config.pageSize) && config.pageSize > 0, `invalid page size: ${config.pageSize}`)

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

function isApprovalPageResponse(response) {
  if (!response.url().includes('/admin-api/approval-center/tasks/page') || response.request().method() !== 'GET') {
    return false
  }
  const url = new URL(response.url())
  return url.searchParams.get('viewType') === expectedViewType &&
    (url.searchParams.get('moduleCode') || '') === expectedModuleCode &&
    url.searchParams.get('pageSize') === String(config.pageSize)
}

function responsePageNo(response) {
  return new URL(response.url()).searchParams.get('pageNo')
}

async function extractPageData(response) {
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `page api failed: ${JSON.stringify(payload)}`)
  assert.ok(payload.data && typeof payload.data.total === 'number', `page api missing total: ${JSON.stringify(payload)}`)
  return payload.data
}

async function activePageNumber(page) {
  const text = await page.locator('.el-pagination .is-active').first().textContent({ timeout: 10000 })
  return String(text || '').trim()
}

async function visiblePageNumbers(page) {
  return await page.locator('.el-pagination .number').evaluateAll((nodes) =>
    nodes
      .map((node) => Number((node.textContent || '').trim()))
      .filter((value) => Number.isInteger(value))
  )
}

async function clickAndAssertPage(page, pageNo, responses) {
  const visiblePages = await visiblePageNumbers(page)
  if (!visiblePages.includes(pageNo)) {
    return false
  }
  const beforeCount = responses.length
  await page.locator('.el-pagination .number').filter({ hasText: new RegExp(`^${pageNo}$`) }).first().click()
  await page.waitForFunction(
    (expectedPageNo) => {
      const active = document.querySelector('.el-pagination .is-active')
      return active && active.textContent && active.textContent.trim() === String(expectedPageNo)
    },
    pageNo,
    { timeout: 60000 }
  )
  await settle(page)

  const active = await activePageNumber(page)
  assert.equal(active, String(pageNo), `pagination active page should stay on ${pageNo}, got ${active}`)

  const clickedIndex = responses.findIndex((entry, index) => index >= beforeCount && entry.pageNo === String(pageNo))
  if (clickedIndex !== -1) {
    const resetAfterClick = responses.slice(clickedIndex + 1).find((entry) => entry.pageNo === '1')
    assert.equal(resetAfterClick, undefined, `page ${pageNo} click must not be followed by pageNo=1 refresh`)
  }
  return true
}

async function main() {
  fs.mkdirSync(config.taskDir, { recursive: true })
  const browser = await chromium.launch({ headless: process.env.APPROVAL_CENTER_PAGINATION_HEADED !== '1' })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
  const page = await context.newPage()
  const pageErrors = []
  const responses = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('response', (response) => {
    if (!isApprovalPageResponse(response)) return
    const url = new URL(response.url())
    responses.push({
      pageNo: url.searchParams.get('pageNo'),
      pageSize: url.searchParams.get('pageSize'),
      moduleCode: url.searchParams.get('moduleCode'),
      viewType: url.searchParams.get('viewType'),
      status: response.status()
    })
  })

  try {
    await login(page)
    await page.evaluate((payload) => {
      localStorage.setItem(`int:list:page-size:${payload.tableKey}`, String(payload.pageSize))
    }, { tableKey: tableKeyByViewType[expectedViewType], pageSize: config.pageSize })
    const initialResponsePromise = page.waitForResponse(isApprovalPageResponse, { timeout: 60000 })
    await page.goto(`${config.baseUrl}${config.targetPath}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    const initialData = await extractPageData(await initialResponsePromise)
    await page.locator('.approval-center__table').waitFor({ state: 'visible', timeout: 60000 })
    await settle(page)

    const totalPages = Math.ceil(initialData.total / config.pageSize)
    assert.ok(
      totalPages >= 2,
      `approval page requires at least 2 pages to validate pagination, got total=${initialData.total}, pageSize=${config.pageSize}`
    )
    assert.equal(await activePageNumber(page), '1', 'initial active page must be 1')

    const candidatePages = totalPages >= 3 ? [2, 3] : [2]
    const visiblePages = await visiblePageNumbers(page)
    const pagesToVerify = candidatePages.filter((pageNo) => visiblePages.includes(pageNo))
    assert.deepEqual(
      pagesToVerify.length > 0,
      true,
      `approval page must expose page 2 or page 3 for validation, visible pages=${visiblePages.join(',')}`
    )
    const verifiedPages = []
    const skippedPages = []
    for (const pageNo of pagesToVerify) {
      const verified = await clickAndAssertPage(page, pageNo, responses)
      if (verified) {
        verifiedPages.push(pageNo)
      } else {
        skippedPages.push(pageNo)
      }
    }
    assert.ok(verifiedPages.includes(2), `page 2 must be verified, verified=${verifiedPages.join(',')}, skipped=${skippedPages.join(',')}`)

    assert.deepEqual(pageErrors, [], `page errors: ${pageErrors.join('\n')}`)
    await page.screenshot({ path: artifacts.screenshot, fullPage: true })
    const result = {
      tenant: config.tenant,
      username: config.username,
      targetPath: config.targetPath,
      total: initialData.total,
      totalPages,
      pagesVerified: verifiedPages,
      pagesSkipped: skippedPages,
      responses,
      activePage: await activePageNumber(page),
      screenshot: artifacts.screenshot
    }
    fs.writeFileSync(artifacts.result, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    process.stdout.write(`approval center pagination real e2e passed\n${JSON.stringify(result, null, 2)}\n`)
  } catch (error) {
    await page.screenshot({ path: artifacts.failure, fullPage: true }).catch(() => null)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
