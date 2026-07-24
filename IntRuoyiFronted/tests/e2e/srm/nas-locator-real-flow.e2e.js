const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = process.cwd()
const outputDir = path.resolve(frontendRoot, 'output/playwright/srm-nas-locator')

const config = {
  baseUrl: (process.env.SRM_NAS_LOCATOR_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.SRM_NAS_LOCATOR_E2E_TENANT || '测试租户',
  username: process.env.SRM_NAS_LOCATOR_E2E_USERNAME || 'aoteman',
  password: process.env.SRM_NAS_LOCATOR_E2E_PASSWORD || '111111',
  headless: process.env.SRM_NAS_LOCATOR_E2E_HEADED !== '1'
}

function isSuccessPayload(payload) {
  return Boolean(payload) && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 30000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(800)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const target = locator.nth(index)
    if (await target.isVisible()) {
      await target.fill('')
      await target.fill(value)
      return target
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/srm/nas-locator`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.fill('')
  await tenantInput.fill(config.tenant)
  await tenantInput.press('Enter')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(
    loginForm.locator('input[type="password"], input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert.ok(isSuccessPayload(loginPayload), `login failed: ${JSON.stringify(loginPayload)}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function openNasLocator(page) {
  await page.goto(`${config.baseUrl}/srm/nas-locator`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  await page.getByText('关键词', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
  await page.screenshot({ path: path.join(outputDir, 'nas-locator-page.png'), fullPage: true })
}

async function openStatusDialog(page) {
  await page.getByRole('button', { name: '详情' }).click()
  await page.locator('.nas-locator-status-dialog').waitFor({ state: 'visible', timeout: 30000 })
}

async function waitForRefreshCompletion(page) {
  const deadline = Date.now() + 180000
  let sawRunning = false

  while (Date.now() < deadline) {
    await page.getByText('关键词', { exact: true }).waitFor({ state: 'visible', timeout: 30000 })
    await openStatusDialog(page)
    const helperText =
      (await page.locator('.nas-locator-status-dialog .nas-locator-status-helper').first().textContent()) || ''
    if (helperText.includes('RUNNING')) {
      sawRunning = true
    }
    if (helperText.includes('FAILED')) {
      assert.fail(`NAS refresh finished with FAILED state: ${helperText}`)
    }
    if (helperText.includes('SUCCESS') && (sawRunning || !helperText.includes('RUNNING'))) {
      await page.keyboard.press('Escape').catch(() => null)
      return
    }
    await page.keyboard.press('Escape').catch(() => null)
    await page.reload({ waitUntil: 'domcontentloaded', timeout: 60000 })
    await settle(page)
  }

  assert.fail('Timed out waiting for NAS refresh completion without frontend auto polling')
}

async function searchFiles(page, keyword) {
  const searchResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/srm/nas-locator/page') && response.request().method() === 'GET',
    { timeout: 60000 }
  )
  const input = page.locator('input[placeholder="请输入关键词"]').first()
  await input.fill('')
  if (keyword) {
    await input.fill(keyword)
  }
  await page.getByRole('button', { name: '搜索' }).click()
  const response = await searchResponse
  const payload = await response.json().catch(() => null)
  assert.ok(isSuccessPayload(payload), `search failed: ${JSON.stringify(payload)}`)
  await settle(page)
  return payload
}

async function main() {
  assert.equal(config.tenant, '测试租户', `write E2E must use 测试租户, got ${config.tenant}`)
  assert.equal(config.username, 'aoteman', `write E2E must use aoteman, got ${config.username}`)
  fs.mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({ headless: config.headless })
  const context = await browser.newContext({
    viewport: { width: 1440, height: 960 },
    locale: 'zh-CN',
    acceptDownloads: true
  })
  const page = await context.newPage()
  const consoleErrors = []
  const pageErrors = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    pageErrors.push(error.message)
  })

  try {
    await login(page)
    await openNasLocator(page)

    const refreshResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/srm/nas-locator/refresh') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: '刷新' }).click()
    const refreshResponse = await refreshResponsePromise
    const refreshPayload = await refreshResponse.json().catch(() => null)
    assert.ok(isSuccessPayload(refreshPayload), `refresh start failed: ${JSON.stringify(refreshPayload)}`)

    await waitForRefreshCompletion(page)
    await page.screenshot({ path: path.join(outputDir, 'nas-locator-refresh-success.png'), fullPage: true })

    const allFilesPayload = await searchFiles(page, '')
    const allFiles = allFilesPayload?.data?.list || []
    assert.ok(allFiles.length > 0, 'blank keyword search should return at least one file')

    const firstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await firstRow.waitFor({ state: 'visible', timeout: 30000 })
    const firstFileName = (await firstRow.locator('td').nth(0).innerText()).trim()
    const firstDirectory = (await firstRow.locator('td').nth(1).innerText()).trim()
    assert.ok(firstFileName, 'first file name should not be empty')
    assert.ok(firstDirectory, 'first directory path should not be empty')

    const keyword = firstFileName.slice(0, Math.min(4, firstFileName.length))
    const fuzzyPayload = await searchFiles(page, keyword)
    const fuzzyFiles = fuzzyPayload?.data?.list || []
    assert.ok(fuzzyFiles.length > 0, 'fuzzy keyword search should return files')
    const fuzzyText = await page.locator('.el-table__body-wrapper').innerText()
    assert.ok(fuzzyText.includes(keyword), `search result should contain keyword ${keyword}`)
    assert.ok(fuzzyText.includes('根目录') || /[\\/]/.test(fuzzyText) || fuzzyText.includes(firstDirectory), 'search result should expose NAS directory path')
    const fuzzyFirstRow = page.locator('.el-table__body-wrapper tbody tr').first()
    await fuzzyFirstRow.waitFor({ state: 'visible', timeout: 30000 })
    const fuzzyFirstFileName = (await fuzzyFirstRow.locator('td').nth(0).innerText()).trim()
    assert.ok(fuzzyFirstFileName, 'fuzzy search first row file name should not be empty')

    const download = await Promise.all([
      page.waitForEvent('download', { timeout: 60000 }),
      fuzzyFirstRow.getByRole('button', { name: '下载' }).click()
    ]).then((result) => result[0])
    const suggestedFilename = download.suggestedFilename()
    const savedFilePath = path.join(outputDir, suggestedFilename)
    await download.saveAs(savedFilePath)
    assert.ok(fs.existsSync(savedFilePath), `downloaded file should exist: ${savedFilePath}`)
    assert.equal(suggestedFilename, fuzzyFirstFileName, 'downloaded filename should match table file name')

    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `console errors detected: ${consoleErrors.join('\n')}`)

    console.log(
      JSON.stringify(
        {
          ok: true,
          baseUrl: config.baseUrl,
          tenant: config.tenant,
          username: config.username,
          keyword,
          fileName: fuzzyFirstFileName,
          directoryPath: firstDirectory,
          downloadFile: savedFilePath,
          screenshots: [
            path.join(outputDir, 'nas-locator-page.png'),
            path.join(outputDir, 'nas-locator-refresh-success.png')
          ]
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
