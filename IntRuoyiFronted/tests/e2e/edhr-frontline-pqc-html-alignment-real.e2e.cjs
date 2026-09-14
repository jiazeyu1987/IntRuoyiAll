const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = 'http://127.0.0.1:8081'
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const OUTPUT_DIR = path.resolve(
  process.env.EDHR_FRONTLINE_PQC_HTML_ALIGNMENT_OUTPUT_DIR ||
    path.join(process.cwd(), 'output/playwright/20260731-frontline-pqc-html-alignment')
)

function readDefaultLogin() {
  const lines = fs.readFileSync(path.resolve(process.cwd(), '.env'), 'utf8').split(/\r?\n/)
  const readValue = (name) => {
    const line = lines.find((entry) => new RegExp(`^\\s*${name}\\s*=`).test(entry))
    assert.ok(line, `Missing ${name} in .env`)
    return line.split('=').slice(1).join('=').trim().replace(/^['"]|['"]$/g, '')
  }
  return {
    tenant: readValue('VITE_APP_DEFAULT_LOGIN_TENANT'),
    username: readValue('VITE_APP_DEFAULT_LOGIN_USERNAME'),
    password: readValue('VITE_APP_DEFAULT_LOGIN_PASSWORD')
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`Missing visible input: ${label}`)
}

async function selectTenant(page, form, tenant) {
  const tenantInput = form
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .filter({ visible: true })
    .first()
  await tenantInput.click()
  await tenantInput.fill(tenant)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenant })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page) {
  const credentials = readDefaultLogin()
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) {
    return
  }
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30000 })
  await selectTenant(page, form, credentials.tenant)
  await fillFirstVisible(
    form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    credentials.username,
    'username'
  )
  await fillFirstVisible(form.locator('input[type="password"]'), credentials.password, 'password')
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await loginResponse
  const body = await response.json()
  assert.ok(response.ok(), `Login HTTP failed: ${response.status()}`)
  assert.ok(body.code === 0 || body.code === 200, `Login failed: ${body.msg || body.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function run() {
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Chrome not found: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })

  const browser = await chromium.launch({
    headless: process.env.EDHR_FRONTLINE_PQC_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  const mesWriteRequests = []

  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => {
    consoleErrors.push(error.message)
  })

  try {
    await login(page)
    page.on('request', (request) => {
      if (
        request.url().includes('/admin-api/mes/') &&
        !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
      ) {
        mesWriteRequests.push(`${request.method()} ${request.url()}`)
      }
    })

    await page.goto(
      `${BASE_URL}/mes/pro/feedback/edhr-batch-pqc-fill?productionOrderCode=CODX-PQC-VISUAL`,
      { waitUntil: 'domcontentloaded', timeout: 90000 }
    )
    const screen = page.locator('[data-frontline-pqc-operator]')
    await screen.waitFor({ state: 'visible', timeout: 90000 })
    await screen.getByText('检验内容', { exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })

    assert.equal(
      await screen.locator('[data-pqc-inspection-entry]').count(),
      4,
      'PQC page must render four target inspection entries.'
    )
    for (const groupKey of ['appearance', 'seal']) {
      const group = screen.locator(`[data-pqc-inspection-group="${groupKey}"]`)
      assert.equal(
        await group.locator('.frontline-pqc-choice-actions > button').count(),
        3,
        `${groupKey} must render three target actions.`
      )
    }
    const typeColumnCount = await screen.locator('.frontline-pqc-type-tabs').evaluate((element) =>
      getComputedStyle(element).gridTemplateColumns.split(' ').filter(Boolean).length
    )
    assert.equal(typeColumnCount, 3, 'PQC inspection type tabs must use three columns.')
    await page.screenshot({
      path: path.join(OUTPUT_DIR, 'pqc-main-1920.png'),
      fullPage: true
    })

    const processCard = screen.locator('.frontline-top-card').filter({ hasText: '工序' }).first()
    await processCard.click()
    const processOptions = page.locator('.frontline-picker__options button')
    await page.waitForTimeout(1000)
    if ((await processOptions.count()) === 0) {
      throw new Error(
        'E2E prerequisite missing: the formal frontline process API returned no selectable process for 芋道源码/admin.'
      )
    }
    const firstProcessOption = processOptions.first()
    await firstProcessOption.click()
    await page.waitForFunction(
      () => {
        const cards = Array.from(document.querySelectorAll('.frontline-top-card'))
        const process = cards.find((card) => card.textContent?.includes('工序'))
        const value = process?.querySelector('strong')?.textContent?.trim()
        return Boolean(value && value !== '未选择')
      },
      undefined,
      { timeout: 30000 }
    )

    const quantityInput = screen.locator('#frontlinePqcInspectionQuantity')
    await quantityInput.fill('30')
    await screen.getByText('已填 30/30', { exact: true }).first().waitFor({
      state: 'visible',
      timeout: 30000
    })

    await screen.locator('[data-pqc-inspection-entry="length"]').click()
    const modal = screen.locator('[data-pqc-piece-modal]')
    await modal.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(
      await modal.locator('.frontline-pqc-piece-row').count(),
      30,
      'Length modal must render 30 pieces.'
    )
    const columnCount = await modal.locator('[data-pqc-piece-list]').evaluate((element) =>
      getComputedStyle(element).gridTemplateColumns.split(' ').filter(Boolean).length
    )
    assert.equal(columnCount, 5, 'Length modal must use five columns at 1920px.')
    const firstLengthInput = modal.locator('.frontline-pqc-piece-value-control input').first()
    assert.equal(await firstLengthInput.inputValue(), '32.5', 'Length default must be 32.5.')
    await modal.getByRole('button', { name: '第 1 件长度增加' }).click()
    assert.equal(await firstLengthInput.inputValue(), '32.6', 'Length plus must use 0.1 step.')
    await firstLengthInput.fill('32.1')
    await page.screenshot({
      path: path.join(OUTPUT_DIR, 'pqc-length-grid-1920.png'),
      fullPage: true
    })
    await modal.getByRole('button', { name: '完成' }).click()

    const appearanceGroup = screen.locator('[data-pqc-inspection-group="appearance"]')
    await appearanceGroup.getByRole('button', { name: '全部合格' }).click()
    await appearanceGroup.getByText('已填 30/30', { exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })
    await appearanceGroup.getByRole('button', { name: '逐件选择' }).click()
    await modal.waitFor({ state: 'visible', timeout: 30000 })
    const pieceSwitches = modal.locator('[data-pqc-piece-choice-switch]')
    assert.equal(
      await pieceSwitches.count(),
      30,
      'Bulk pass must render one switch for each of the 30 piece choices.'
    )
    assert.equal(
      await modal.locator('.frontline-pqc-piece-switch.is-checked').count(),
      30,
      'Bulk pass must mark all 30 piece switches as checked.'
    )
    const firstPieceSwitch = pieceSwitches.first()
    await firstPieceSwitch.click()
    assert.equal(
      await firstPieceSwitch.getAttribute('aria-checked'),
      'false',
      'Switching the first piece off must set it to the inactive state.'
    )
    assert.match(
      (await firstPieceSwitch.getAttribute('aria-label')) || '',
      /不合格$/,
      'The inactive switch must expose the formal 不合格 label.'
    )
    await page.screenshot({
      path: path.join(OUTPUT_DIR, 'pqc-choice-grid-1920.png'),
      fullPage: true
    })
    await modal.getByRole('button', { name: '完成' }).click()
    assert.ok(
      await appearanceGroup.getByRole('button', { name: '逐件选择' }).evaluate((element) =>
        element.classList.contains('active')
      ),
      'Mixed appearance results must activate manual selection.'
    )

    await screen.getByRole('button', { name: '第 2 次' }).click()
    await appearanceGroup.getByText('已填 0/30', { exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })
    await screen.getByRole('button', { name: '第 1 次' }).click()
    await appearanceGroup.getByText('已填 30/30', { exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })

    const screenBox = await screen.boundingBox()
    assert.ok(screenBox && screenBox.width <= 1920, 'PQC screen must stay inside the viewport width.')
    assert.deepEqual(mesWriteRequests, [], 'Read-only PQC visual verification must not send MES writes.')
    assert.deepEqual(consoleErrors, [], `Browser console errors: ${consoleErrors.join(' | ')}`)
    console.log('PASS: eDHR frontline PQC HTML alignment real E2E')
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
