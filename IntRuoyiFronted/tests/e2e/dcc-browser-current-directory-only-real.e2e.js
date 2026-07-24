const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260711-dcc-browser-current-directory-only'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-current-directory-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-current-directory-final.png')

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

function readRows(payload) {
  if (!payload) {
    return []
  }
  if (Array.isArray(payload?.data?.list)) {
    return payload.data.list
  }
  if (Array.isArray(payload?.data?.records)) {
    return payload.data.records
  }
  if (Array.isArray(payload?.list)) {
    return payload.list
  }
  return []
}

function readRowDirectoryId(row) {
  return row.directoryId ?? row.fileDirectoryId ?? row.directory?.id
}

async function assertDirectoryClickUsesCurrentOnly(page) {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser?scope=global&pageNo=1&pageSize=20`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.waitForSelector('.browser-directory-scroll .el-tree-node__content', {
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)

  const nodes = page.locator('.browser-directory-scroll .el-tree-node__content')
  const count = await nodes.count()
  if (!count) {
    throw new Error('directory tree nodes missing')
  }

  let captured
  for (let index = 0; index < Math.min(count, 16); index += 1) {
    const requestPromise = page
      .waitForRequest(
        (request) =>
          request.method() === 'GET' &&
          request.url().includes('/dcc/controlled-files/browser-page') &&
          request.url().includes('directoryId='),
        { timeout: 8000 }
      )
      .catch(() => undefined)
    const responsePromise = page
      .waitForResponse(
        (response) =>
          response.request().method() === 'GET' &&
          response.url().includes('/dcc/controlled-files/browser-page') &&
          response.url().includes('directoryId='),
        { timeout: 8000 }
      )
      .catch(() => undefined)

    await nodes.nth(index).click({ position: { x: 140, y: 14 } }).catch(() => undefined)
    const request = await requestPromise
    const response = await responsePromise
    if (!request) {
      continue
    }
    const url = new URL(request.url())
    const includeDescendantDirectories = url.searchParams.get('includeDescendantDirectories')
    if (includeDescendantDirectories !== 'false') {
      throw new Error(
        `directory click should request direct files only, got includeDescendantDirectories=${includeDescendantDirectories}`
      )
    }

    let responsePayload
    try {
      responsePayload = response ? await response.json() : undefined
    } catch (error) {
      responsePayload = { parseError: error.message }
    }
    captured = {
      clickedIndex: index,
      requestUrl: request.url(),
      directoryId: url.searchParams.get('directoryId'),
      includeDescendantDirectories,
      responsePayload
    }
    break
  }

  if (!captured) {
    throw new Error('no directory click browser-page request captured')
  }

  const rows = readRows(captured.responsePayload)
  const rowsWithDirectoryId = rows.filter((row) => readRowDirectoryId(row) !== undefined)
  const mismatchedRows = rowsWithDirectoryId.filter((row) => String(readRowDirectoryId(row)) !== String(captured.directoryId))
  if (mismatchedRows.length) {
    throw new Error(
      `direct-only response contains other directory rows: ${mismatchedRows
        .slice(0, 3)
        .map((row) => row.directoryId)
        .join(', ')}`
    )
  }

  evidence.directoryRequest = {
    clickedIndex: captured.clickedIndex,
    directoryId: captured.directoryId,
    includeDescendantDirectories: captured.includeDescendantDirectories,
    rowCount: rows.length,
    rowsWithDirectoryIdCount: rowsWithDirectoryId.length,
    mismatchedRowCount: mismatchedRows.length
  }
  record('PASS', 'directory click current-only request', evidence.directoryRequest)
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
    await assertDirectoryClickUsesCurrentOnly(page)
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
  } finally {
    await browser.close()
  }
}

main()
  .then(() => {
    console.log('PASS: dcc browser directory click requests current directory only')
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
