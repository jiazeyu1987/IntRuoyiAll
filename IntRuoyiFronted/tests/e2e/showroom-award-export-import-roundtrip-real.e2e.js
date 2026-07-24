const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { Readable } = require('node:stream')
const { finished } = require('node:stream/promises')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error('Playwright is required for showroom award export/import real E2E.')
  }
}

const config = {
  baseUrl: (process.env.SHOWROOM_AWARD_ROUNDTRIP_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SHOWROOM_AWARD_ROUNDTRIP_TENANT || '测试租户',
  username: process.env.SHOWROOM_AWARD_ROUNDTRIP_USERNAME || 'aoteman',
  password: process.env.SHOWROOM_AWARD_ROUNDTRIP_PASSWORD || 'admin123',
  headed: process.env.SHOWROOM_AWARD_ROUNDTRIP_HEADED === '1'
}

const taskDir = path.resolve(
  __dirname,
  '../../doc/tasks/20260615-showroom-award-export-import-real-e2e'
)
const downloadPath = path.join(taskDir, '产品资料修改版-补充产品资料.xlsx')

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
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')
    const responsePromise = page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ).catch(() => null)
    const navigationPromise = page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
      .then(() => null)
      .catch(() => null)
    await loginForm.getByRole('button', { name: /登录/ }).click()
    const response = await Promise.race([responsePromise, navigationPromise])
    if (response) {
      const payload = await response.json().catch(() => null)
      assert.ok(payload && (payload.code === 0 || payload.code === 200), `login failed: ${JSON.stringify(payload)}`)
    }
  }

  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 }).catch(async (error) => {
    const probe = await page.evaluate(() =>
      Array.from(document.querySelectorAll('input')).map((input) => ({
        type: input.getAttribute('type'),
        placeholder: input.getAttribute('placeholder'),
        value: input.value,
        visible: Boolean(input.offsetParent)
      }))
    )
    const bodyText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
    throw new Error(`登录后未离开 /login：${error.message}; inputs=${JSON.stringify(probe)}; body=${bodyText.slice(0, 1000)}`)
  })
  await page.goto(`${config.baseUrl}/showroom/product`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByText('产品管理').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.getByRole('tab', { name: '奖项' }).waitFor({ state: 'visible', timeout: 30000 })
}

function normalizeAward(row) {
  const revision =
    row && typeof row === 'object'
      ? row.displayRevision && typeof row.displayRevision === 'object'
        ? row.displayRevision
        : row.revision && typeof row.revision === 'object'
          ? row.revision
          : row
      : {}
  return {
    awardCode: String(row.awardCode || revision.awardCode || ''),
    nameCn: String(revision.nameCn || ''),
    nameEn: String(revision.nameEn || ''),
    issuer: String(revision.issuer || ''),
    awardDateText: String(revision.awardDateText || ''),
    coverImage: String(revision.coverImage || ''),
    status: String(revision.status || '')
  }
}

async function fetchAwardSnapshot(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/showroom/award/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.getByRole('tab', { name: '奖项' }).click()
  const response = await responsePromise
  assert.equal(response.status(), 200, `award page http status must be 200, got ${response.status()}`)
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `award page failed: ${JSON.stringify(payload)}`)
  const list = payload.data?.list || payload.data?.records || []
  assert.ok(Array.isArray(list), `award page list must be an array: ${JSON.stringify(payload.data)}`)
  const normalized = list.map(normalizeAward).sort((a, b) => a.awardCode.localeCompare(b.awardCode))
  assert.ok(normalized.length > 0, '真实回导前置失败：当前测试租户没有奖项数据')
  for (const award of normalized) {
    assert.ok(award.coverImage, `真实回导前置失败：奖项缺少封面 ${award.awardCode} ${award.nameCn}`)
  }
  return normalized
}

async function exportWorkbook(page) {
  await page.getByRole('tab', { name: '产品' }).click()
  await settle(page)
  const exportButton = page.locator('.showroom-product-list__actions').getByRole('button', { name: /^导出$/ }).first()
  await exportButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.ok(await exportButton.isEnabled(), '产品管理导出按钮必须可点击')
  const requestPromise = page.waitForRequest(
    (request) => request.url().includes('/admin-api/showroom/product/export-excel'),
    { timeout: 30000 }
  )
  await exportButton.click({ force: true })
  const confirmDialog = page.locator('.el-message-box:visible').last()
  await confirmDialog.waitFor({ state: 'visible', timeout: 10000 })
  await confirmDialog.getByRole('button', { name: /确认|确 定|确定/ }).click()
  const request = await requestPromise.catch(async (error) => {
    const bodyText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '')
    throw new Error(`等待导出请求超时：${error.message}; visibleText=${bodyText.slice(0, 1000)}`)
  })
  const headers = request.headers()
  const downloadResponse = await fetch(request.url(), {
    method: 'GET',
    headers: {
      Authorization: headers.authorization || headers.Authorization || '',
      'tenant-id': headers['tenant-id'] || '',
      ...(headers['visit-tenant-id'] ? { 'visit-tenant-id': headers['visit-tenant-id'] } : {})
    }
  })
  assert.equal(
    downloadResponse.status,
    200,
    `export excel stream http status must be 200, got ${downloadResponse.status}`
  )
  fs.mkdirSync(taskDir, { recursive: true })
  await finished(Readable.fromWeb(downloadResponse.body).pipe(fs.createWriteStream(downloadPath)))
  assert.ok(fs.existsSync(downloadPath), `downloaded workbook is missing: ${downloadPath}`)
  assert.ok(fs.statSync(downloadPath).size > 0, 'downloaded workbook must not be empty')
  return downloadPath
}

async function importWorkbook(page, workbookPath) {
  await page.getByRole('button', { name: /^导入$/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '产品 Excel 导入' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('input[type="file"]').setInputFiles(workbookPath)
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/showroom/product/import-excel'),
    { timeout: 180000 }
  )
  await dialog.getByRole('button', { name: /^确 定$/ }).click()
  const response = await responsePromise
  assert.equal(response.status(), 200, `import excel http status must be 200, got ${response.status()}`)
  const payload = await response.json()
  assert.ok(payload && (payload.code === 0 || payload.code === 200), `import excel failed: ${JSON.stringify(payload)}`)
  const data = payload.data || {}
  assert.ok(Number(data.awardTotalRows) > 0, `import must report award rows: ${JSON.stringify(data)}`)
  assert.ok(Number(data.awardFailureCount || 0) === 0, `award import failures must be 0: ${JSON.stringify(data)}`)
  await page.getByRole('button', { name: /^确定$/ }).click().catch(async () => {
    await page.getByRole('button', { name: /^OK$/ }).click({ timeout: 5000 })
  })
  await settle(page)
  return data
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const context = await browser.newContext({
    acceptDownloads: true,
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const pageErrors = []
  page.on('pageerror', (error) => pageErrors.push(error.message))
  const observedRequests = []
  page.on('request', (request) => {
    const url = request.url()
    if (url.includes('export') || url.includes('showroom/product')) {
      observedRequests.push(`${request.method()} ${url}`)
    }
  })

  try {
    await login(page)
    const beforeAwards = await fetchAwardSnapshot(page)
    const workbookPath = await exportWorkbook(page).catch((error) => {
      throw new Error(`${error.message}; observedRequests=${observedRequests.join(' || ')}`)
    })
    const importResult = await importWorkbook(page, workbookPath)
    const afterAwards = await fetchAwardSnapshot(page)
    assert.deepEqual(afterAwards, beforeAwards, 'award snapshot must be unchanged after export/import roundtrip')
    assert.deepEqual(pageErrors, [], `page errors were emitted: ${pageErrors.join(' | ')}`)
    console.log(
      `PASS: showroom award export/import roundtrip awards=${afterAwards.length} awardSuccess=${importResult.awardSuccessCount} workbook=${workbookPath}`
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
