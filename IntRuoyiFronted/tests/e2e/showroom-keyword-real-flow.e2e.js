const fs = require('fs')
const path = require('path')
const { chromium } = require('playwright')

const BASE_URL = process.env.SHOWROOM_KEYWORD_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.SHOWROOM_KEYWORD_E2E_TENANT || '测试租户'
const USERNAME = process.env.SHOWROOM_KEYWORD_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.SHOWROOM_KEYWORD_E2E_PASSWORD || '111111'
const HEADED = process.env.SHOWROOM_KEYWORD_E2E_HEADED === '1'
const outputDir = path.resolve(__dirname, '../../output/playwright/showroom-keyword-real-flow')

fs.mkdirSync(outputDir, { recursive: true })

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

async function screenshot(page, name) {
  const target = path.join(outputDir, name)
  await page.screenshot({ path: target, fullPage: true })
  return target
}

async function fillVisible(locator, value) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const current = locator.nth(index)
    if (await current.isVisible()) {
      await current.fill(value)
      return
    }
  }
  throw new Error(`No visible locator found for value ${value}`)
}

async function login(page) {
  const loginUrl = `${BASE_URL}/login?redirect=/showroom/keyword`
  await page.goto(loginUrl, { waitUntil: 'domcontentloaded' })
  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 30000 })

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill('')
    await tenantInput.fill(TENANT)
    await tenantInput.press('Enter')
  }

  const textInputs = loginForm.locator('input.el-input__inner')
  await textInputs.nth(0).fill('')
  await textInputs.nth(0).fill(USERNAME)
  await loginForm.locator('input[type="password"]').first().fill('')
  await loginForm.locator('input[type="password"]').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      (response.url().includes('/system/auth/login') ||
        response.url().includes('/admin-api/system/auth/login')) &&
      response.request().method() === 'POST',
    { timeout: 30000 }
  )
  await loginForm.getByRole('button', { name: '登录' }).first().click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json().catch(() => null)
  assert(loginResponse.ok(), `Login HTTP failed: ${loginResponse.status()}`)
  assert(
    loginPayload && [0, 200].includes(loginPayload.code),
    `Login business failed: ${JSON.stringify(loginPayload)}`
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 30000 })
}

async function openKeywordPage(page, network) {
  await page.goto(`${BASE_URL}/showroom/keyword`, { waitUntil: 'domcontentloaded' })
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  const keywordPageResponse = network.responses.find((entry) => entry.url.includes('/showroom/keyword/page'))
  if (keywordPageResponse) {
    assert(
      keywordPageResponse.ok,
      `Keyword page API failed: ${keywordPageResponse.status} ${keywordPageResponse.body}`
    )
  }
  await page.getByRole('button', { name: '新增关键词' }).waitFor({ state: 'visible', timeout: 30000 })
}

async function createKeyword(page, nameZh, nameEn) {
  await page.getByRole('button', { name: '新增关键词' }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await dialog.getByLabel('中文关键词').fill(nameZh)
  await dialog.getByLabel('English Keyword').fill(nameEn)
  await dialog.getByRole('button', { name: '保存' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function searchKeyword(page, keyword) {
  const searchInput = page.getByPlaceholder('请输入中文关键词或 English Keyword').first()
  await searchInput.fill('')
  await searchInput.fill(keyword)
  await page.getByRole('button', { name: '查询' }).click()
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
}

async function editFirstRow(page, nextEnglishName) {
  const row = page.locator('.el-table__body-wrapper tbody tr').first()
  await row.getByRole('button', { name: '编辑' }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  const enInput = dialog.getByLabel('English Keyword')
  await enInput.fill('')
  await enInput.fill(nextEnglishName)
  await dialog.getByRole('button', { name: '保存' }).click()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function deleteFirstRow(page) {
  const row = page.locator('.el-table__body-wrapper tbody tr').first()
  await row.getByRole('button', { name: '删除' }).click()
  const confirmButton = page.locator('.el-message-box:visible .el-button--primary').last()
  await confirmButton.click()
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined)
}

async function verifyContains(page, text) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
}

async function verifyNotContains(page, text) {
  await page.waitForTimeout(1000)
  const locator = page.getByText(text, { exact: false }).first()
  const visible = await locator.isVisible().catch(() => false)
  assert(!visible, `Text should be absent after delete: ${text}`)
}

async function main() {
  const browser = await chromium.launch({ headless: !HEADED, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(30000)
  page.setDefaultNavigationTimeout(30000)

  const network = { responses: [] }
  page.on('response', async (response) => {
    if (!response.url().includes('/showroom/keyword')) {
      return
    }
    let body = ''
    try {
      body = JSON.stringify(await response.json())
    } catch (error) {
      body = `<non-json:${error.message}>`
    }
    network.responses.push({
      url: response.url(),
      status: response.status(),
      ok: response.ok(),
      body
    })
  })

  const uniqueSuffix = `${Date.now()}`
  const nameZh = `自动化关键词${uniqueSuffix}`
  const createdNameEn = `Automation Keyword ${uniqueSuffix}`
  const updatedNameEn = `Automation Keyword Updated ${uniqueSuffix}`

  try {
    await login(page)
    await screenshot(page, '01-after-login.png')

    await openKeywordPage(page, network)
    await screenshot(page, '02-keyword-page.png')

    await createKeyword(page, nameZh, createdNameEn)
    await screenshot(page, '03-created.png')

    await searchKeyword(page, nameZh)
    await verifyContains(page, nameZh)
    await screenshot(page, '04-search-zh.png')

    await editFirstRow(page, updatedNameEn)
    await searchKeyword(page, updatedNameEn)
    await verifyContains(page, updatedNameEn)
    await screenshot(page, '05-search-en.png')

    await deleteFirstRow(page)
    await searchKeyword(page, nameZh)
    await verifyNotContains(page, nameZh)
    await screenshot(page, '06-deleted.png')

    console.log(
      JSON.stringify({
        status: 'PASS',
        tenant: TENANT,
        username: USERNAME,
        targetPath: '/showroom/keyword',
        nameZh,
        createdNameEn,
        updatedNameEn,
        outputDir
      })
    )
  } catch (error) {
    let pageTitle = ''
    let bodyPreview = ''
    let sessionDiagnostics = null
    try {
      pageTitle = await page.title()
      bodyPreview = await page.locator('body').innerText()
      bodyPreview = bodyPreview.replace(/\s+/g, ' ').slice(0, 1200)
      sessionDiagnostics = await page.evaluate(() => ({
        roleRouters: localStorage.getItem('roleRouters') || '',
        user: localStorage.getItem('user') || '',
        tenantId: localStorage.getItem('tenantId') || '',
        visitTenantId: localStorage.getItem('visitTenantId') || '',
        sessionTenantId: sessionStorage.getItem('tenantId') || '',
        sessionVisitTenantId: sessionStorage.getItem('visitTenantId') || ''
      }))
    } catch (captureError) {
      bodyPreview = `<capture-failed:${captureError.message}>`
    }
    await screenshot(page, 'failure.png').catch(() => undefined)
    console.error(
      JSON.stringify(
        {
          status: 'FAIL',
          tenant: TENANT,
          username: USERNAME,
          currentUrl: page.url(),
          pageTitle,
          bodyPreview,
          outputDir,
          network,
          sessionDiagnostics: sessionDiagnostics
            ? {
                tenantId: sessionDiagnostics.tenantId,
                visitTenantId: sessionDiagnostics.visitTenantId,
                sessionTenantId: sessionDiagnostics.sessionTenantId,
                sessionVisitTenantId: sessionDiagnostics.sessionVisitTenantId,
                hasKeywordName: sessionDiagnostics.roleRouters.includes('ShowroomAdminKeyword'),
                hasKeywordPath: sessionDiagnostics.roleRouters.includes('keyword'),
                roleRoutersPreview: sessionDiagnostics.roleRouters.slice(0, 4000),
                userPreview: sessionDiagnostics.user.slice(0, 1200)
              }
            : null,
          error: error && error.stack ? error.stack : String(error)
        },
        null,
        2
      )
    )
    process.exitCode = 1
  } finally {
    await context.close()
    await browser.close()
  }
}

main()
