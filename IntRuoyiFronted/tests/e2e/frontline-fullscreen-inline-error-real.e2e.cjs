const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = 'http://127.0.0.1:8081'
const OUTPUT_DIR = path.resolve(
  process.cwd(),
  'output/playwright/20260817-frontline-fullscreen-error-zone'
)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const CASES = [
  {
    name: 'production',
    route: '/mes/pro/feedback/edhr-batch-production-fill',
    screen: '[data-frontline-production-operator]',
    workPanel: '.frontline-production-quantity-panel',
    submit: '.frontline-production-submit-button',
    fullscreen: '[data-production-fullscreen-toggle]'
  },
  {
    name: 'pqc',
    route: '/mes/pro/feedback/edhr-batch-pqc-fill',
    screen: '[data-frontline-pqc-operator]',
    workPanel: '.frontline-pqc-content-panel',
    submit: '.frontline-pqc-submit-button',
    fullscreen: '[data-pqc-fullscreen-toggle]'
  }
]

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
  if (!page.url().includes('/login')) return

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

async function verifyInlineErrorInFullscreen(page, testCase) {
  await page.goto(`${BASE_URL}${testCase.route}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const screen = page.locator(testCase.screen).first()
  await screen.waitFor({ state: 'visible', timeout: 90000 })

  const fullscreenButton = screen.locator(testCase.fullscreen).first()
  await fullscreenButton.waitFor({ state: 'visible', timeout: 30000 })
  assert.match(
    String(await fullscreenButton.textContent()).replace(/\s+/g, ''),
    /最大化/,
    `${testCase.name} must expose the real fullscreen action`
  )
  await fullscreenButton.click()
  await page.waitForFunction(() => Boolean(document.fullscreenElement), undefined, {
    timeout: 30000
  })

  const submitButton = screen.locator(testCase.submit).first()
  await submitButton.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForFunction(
    (selector) => {
      const button = document.querySelector(selector)
      return button instanceof HTMLButtonElement && !button.disabled
    },
    `${testCase.screen} ${testCase.submit}`,
    { timeout: 30000 }
  )
  await submitButton.click()

  const errorSlot = screen.locator('[data-frontline-error-slot].is-visible').first()
  await errorSlot.waitFor({ state: 'visible', timeout: 30000 })
  const errorMessage = String(
    await errorSlot.locator('[data-frontline-error-message]').textContent()
  ).trim()
  assert.ok(errorMessage.length > 0, `${testCase.name} inline error must preserve the failure reason`)
  assert.equal(
    await errorSlot.evaluate((element) => Boolean(document.fullscreenElement?.contains(element))),
    true,
    `${testCase.name} inline error must be inside the browser fullscreen element`
  )
  assert.equal(
    await page.locator('.el-message--error:visible').count(),
    0,
    `${testCase.name} must not render a body-level error toast`
  )

  const workPanel = screen.locator(testCase.workPanel).first()
  const [slotBox, panelBox] = await Promise.all([errorSlot.boundingBox(), workPanel.boundingBox()])
  assert.ok(slotBox && panelBox, `${testCase.name} error zone must have visible geometry`)
  assert.ok(
    slotBox.x >= panelBox.x - 2 &&
      slotBox.x + slotBox.width <= panelBox.x + panelBox.width + 2 &&
      slotBox.y >= panelBox.y - 2 &&
      slotBox.y + slotBox.height <= panelBox.y + panelBox.height + 2,
    `${testCase.name} error zone must remain inside the left work panel`
  )

  await page.screenshot({
    path: path.join(OUTPUT_DIR, `${testCase.name}-fullscreen-inline-error.png`),
    fullPage: true
  })
  await fullscreenButton.click()
  await page.waitForFunction(() => !document.fullscreenElement, undefined, { timeout: 30000 })
  return errorMessage
}

async function run() {
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `Browser not found: ${BROWSER_EXECUTABLE}`)
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  const browser = await chromium.launch({
    headless: process.env.FRONTLINE_ERROR_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const submissionWriteRequests = []
  page.on('request', (request) => {
    const requestPath = new URL(request.url()).pathname
    if (request.method() === 'POST' && (
      requestPath.endsWith('/mes/pro/feedback/frontline/submit') ||
      requestPath.endsWith('/mes/pro/feedback/frontline/device-account/pqc/submit')
    )) {
      submissionWriteRequests.push(`${request.method()} ${requestPath}`)
    }
  })

  try {
    await login(page)
    const results = {}
    for (const testCase of CASES) {
      results[testCase.name] = await verifyInlineErrorInFullscreen(page, testCase)
    }
    assert.deepEqual(
      submissionWriteRequests,
      [],
      `Validation-only E2E must not send formal submissions: ${submissionWriteRequests.join(', ')}`
    )
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'result.json'),
      `${JSON.stringify({ status: 'PASS', results }, null, 2)}\n`,
      'utf8'
    )
    console.log('PASS: production and PQC fullscreen errors stay visible in the fixed left error zone')
  } finally {
    await context.close()
    await browser.close()
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
