const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260711-dcc-browser-fill-blank-with-rows'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-page-size-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-page-size-final.png')

const config = {
  baseUrl: process.env.DCC_BROWSER_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.DCC_BROWSER_E2E_TENANT || '测试租户',
  username: process.env.DCC_BROWSER_E2E_USERNAME || 'aoteman',
  password: process.env.DCC_BROWSER_E2E_PASSWORD || '111111',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  timeout: Number(process.env.DCC_BROWSER_E2E_TIMEOUT || 120000)
}

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  baseUrl: config.baseUrl,
  tenant: config.tenant,
  username: config.username,
  targetPath: '/dcc/controlled-file/browser',
  actions: [],
  console: [],
  pageErrors: []
}

function record(status, step, detail = {}) {
  evidence.actions.push({
    status,
    step,
    detail,
    at: new Date().toISOString()
  })
}

function writeEvidence() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  fs.writeFileSync(evidencePath, JSON.stringify(evidence, null, 2), 'utf8')
}

async function settle(page, timeout = 1000) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
  await page.waitForTimeout(timeout)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/dcc/controlled-file/browser`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"])').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const payload = await loginResponse.json()
  if (!loginResponse.ok() || ![0, 200].includes(payload.code)) {
    throw new Error(`login failed: HTTP ${loginResponse.status()} ${payload.msg || JSON.stringify(payload)}`)
  }
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  record('PASS', 'login', { url: page.url() })
}

async function assertBrowserListUsesMoreRows(page) {
  const targetUrl = `${config.baseUrl}/dcc/controlled-file/browser?pageNo=1&pageSize=10&scope=global`
  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: config.timeout })
  await page.waitForSelector('.browser-list-template .el-table__body-wrapper tbody tr', {
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)

  await page.waitForFunction(() => {
    const rows = document.querySelectorAll('.browser-list-template .el-table__body-wrapper tbody tr')
    return rows.length >= 20
  }, null, { timeout: config.timeout })

  const result = await page.evaluate(() => {
    const rows = Array.from(
      document.querySelectorAll('.browser-list-template .el-table__body-wrapper tbody tr')
    )
    const pagerText = (document.querySelector('.browser-list-template .el-pagination')?.textContent || '')
      .replace(/\s+/g, ' ')
      .trim()
    const tableRect = document.querySelector('.browser-list-template .el-table')?.getBoundingClientRect()
    const bodyRect = document.querySelector('.browser-list-template .el-table__body-wrapper')?.getBoundingClientRect()
    const firstFileNames = rows.slice(0, 3).map((row) => (row.textContent || '').replace(/\s+/g, ' ').trim())
    return {
      rowCount: rows.length,
      pagerText,
      tableHeight: tableRect?.height || 0,
      bodyHeight: bodyRect?.height || 0,
      firstFileNames
    }
  })
  if (result.rowCount < 20) {
    throw new Error(`browser list row count should fill blank area, got ${result.rowCount}`)
  }
  if (!result.pagerText.includes('20条/页')) {
    throw new Error(`browser list page size should normalize to 20条/页, got "${result.pagerText}"`)
  }
  evidence.listResult = result
  record('PASS', 'browser list uses 20 rows', result)
  await page.screenshot({ path: screenshotPath, fullPage: true })
}

async function main() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const page = await browser.newPage({ viewport: { width: 1680, height: 920 } })
  page.on('console', (message) => {
    evidence.console.push({ type: message.type(), text: message.text() })
  })
  page.on('pageerror', (error) => {
    evidence.pageErrors.push({ message: error.message, stack: error.stack })
  })

  try {
    await login(page)
    await assertBrowserListUsesMoreRows(page)
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
  } finally {
    await browser.close()
  }
}

main()
  .then(() => {
    console.log('PASS: dcc browser fills blank area with 20 list rows')
  })
  .catch((error) => {
    evidence.error = {
      message: error.message,
      stack: error.stack
    }
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
    console.error(error)
    process.exit(1)
  })
