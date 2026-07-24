const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260711-dcc-browser-sticky-header-footer-align-cards'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-layout-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-layout-final.png')

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

async function readLayout(page) {
  return page.evaluate(() => {
    const rectOf = (selector) => {
      const rect = document.querySelector(selector)?.getBoundingClientRect()
      if (!rect) {
        return undefined
      }
      return {
        top: rect.top,
        bottom: rect.bottom,
        height: rect.height
      }
    }
    const bodyWrapper = document.querySelector('.browser-list-template .el-table__body-wrapper')
    const scrollEl = bodyWrapper?.querySelector('.el-scrollbar__wrap') || bodyWrapper
    const rows = document.querySelectorAll('.browser-list-template .el-table__body-wrapper tbody tr')
    return {
      directory: rectOf('.browser-directory-wrap'),
      list: rectOf('.browser-list-wrap'),
      header: rectOf('.browser-list-template .el-table__header-wrapper'),
      pagination: rectOf('.browser-list-template .el-pagination'),
      rowCount: rows.length,
      windowScrollY: window.scrollY,
      tableScrollTop: scrollEl?.scrollTop || 0,
      tableClientHeight: scrollEl?.clientHeight || 0,
      tableScrollHeight: scrollEl?.scrollHeight || 0,
      pagerText: (document.querySelector('.browser-list-template .el-pagination')?.textContent || '')
        .replace(/\s+/g, ' ')
        .trim()
    }
  })
}

function assertClose(actual, expected, tolerance, message) {
  if (Math.abs(actual - expected) > tolerance) {
    throw new Error(`${message}: actual=${actual}, expected=${expected}, tolerance=${tolerance}`)
  }
}

async function assertStickyLayout(page) {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser?pageNo=1&pageSize=50&scope=global`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.waitForSelector('.browser-list-template .el-table__body-wrapper tbody tr', {
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)
  await page.waitForFunction(() => {
    const rows = document.querySelectorAll('.browser-list-template .el-table__body-wrapper tbody tr')
    return rows.length >= 30
  }, null, { timeout: config.timeout })

  const before = await readLayout(page)
  if (!before.directory || !before.list || !before.header || !before.pagination) {
    throw new Error(`layout elements missing: ${JSON.stringify(before)}`)
  }
  if (before.tableScrollHeight <= before.tableClientHeight) {
    throw new Error(`table body should be internally scrollable: ${JSON.stringify(before)}`)
  }
  assertClose(before.directory.height, before.list.height, 3, 'directory and list card heights should align')
  assertClose(before.directory.bottom, before.list.bottom, 3, 'directory and list card bottoms should align')

  await page.evaluate(() => {
    const bodyWrapper = document.querySelector('.browser-list-template .el-table__body-wrapper')
    const scrollEl = bodyWrapper?.querySelector('.el-scrollbar__wrap') || bodyWrapper
    if (!scrollEl) {
      throw new Error('table body scroll element missing')
    }
    scrollEl.scrollTop = scrollEl.scrollHeight
  })
  await page.waitForTimeout(500)
  const after = await readLayout(page)

  if (after.tableScrollTop <= 0) {
    throw new Error(`table body did not scroll: ${JSON.stringify({ before, after })}`)
  }
  assertClose(after.header.top, before.header.top, 2, 'table header should stay fixed while body scrolls')
  assertClose(after.pagination.top, before.pagination.top, 2, 'pagination footer should stay fixed while body scrolls')
  assertClose(after.windowScrollY, before.windowScrollY, 1, 'window should not scroll for list body scrolling')

  evidence.layout = { before, after }
  record('PASS', 'sticky header footer and aligned cards', evidence.layout)
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
    await assertStickyLayout(page)
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
  } finally {
    await browser.close()
  }
}

main()
  .then(() => {
    console.log('PASS: dcc browser sticky header footer and aligned cards real layout')
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
