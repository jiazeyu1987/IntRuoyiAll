const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260711-dcc-browser-refresh-restore-directory-list'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-refresh-restore-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-refresh-restore-final.png')

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
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 5000 }).catch(() => undefined)
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
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

function waitForDirectoryListResponse(page, expectedDirectoryId) {
  return page.waitForResponse(
    (response) => {
      if (
        response.request().method() !== 'GET' ||
        !response.url().includes('/dcc/controlled-files/browser-page') ||
        !response.url().includes('directoryId=')
      ) {
        return false
      }
      if (!expectedDirectoryId) {
        return true
      }
      const url = new URL(response.url())
      return url.searchParams.get('directoryId') === String(expectedDirectoryId)
    },
    { timeout: config.timeout }
  )
}

async function readListState(page) {
  return page.evaluate(() => {
    const text = (document.querySelector('.browser-list-template')?.textContent || '')
      .replace(/\s+/g, ' ')
      .trim()
    const rows = Array.from(
      document.querySelectorAll('.browser-list-template .el-table__body-wrapper tbody tr')
    )
    const currentTreeNode = document.querySelector('.browser-directory-scroll .el-tree-node.is-current')
    return {
      url: location.href,
      rowCount: rows.length,
      hasSelectDirectoryPrompt: text.includes('请先选择目录'),
      emptyText: text.includes('暂无受控文件') ? '暂无受控文件' : text.includes('暂无匹配受控文件') ? '暂无匹配受控文件' : '',
      currentDirectoryText: (currentTreeNode?.textContent || '').replace(/\s+/g, ' ').trim()
    }
  })
}

async function assertDirectoryListLoaded(page, step, expectedDirectoryId, responsePromise) {
  const response = await (responsePromise || waitForDirectoryListResponse(page, expectedDirectoryId))
  const url = new URL(response.url())
  const directoryId = url.searchParams.get('directoryId')
  const includeDescendantDirectories = url.searchParams.get('includeDescendantDirectories')
  await settle(page)
  const state = await readListState(page)
  if (state.hasSelectDirectoryPrompt) {
    throw new Error(`${step} still shows select-directory prompt: ${JSON.stringify(state)}`)
  }
  if (!state.currentDirectoryText) {
    throw new Error(`${step} did not highlight a directory in the tree: ${JSON.stringify(state)}`)
  }
  const result = {
    directoryId,
    includeDescendantDirectories,
    rowCount: state.rowCount,
    emptyText: state.emptyText,
    currentDirectoryText: state.currentDirectoryText,
    url: state.url
  }
  record('PASS', step, result)
  return result
}

async function selectFirstDirectory(page) {
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
  const responsePromise = waitForDirectoryListResponse(page)
  await nodes.nth(0).click({ position: { x: 140, y: 14 } })
  const response = await responsePromise
  const url = new URL(response.url())
  const directoryId = url.searchParams.get('directoryId')
  if (!directoryId) {
    throw new Error(`directory click did not request a directory: ${response.url()}`)
  }
  await settle(page)
  const state = await readListState(page)
  if (state.hasSelectDirectoryPrompt) {
    throw new Error(`directory click still shows select-directory prompt: ${JSON.stringify(state)}`)
  }
  const result = {
    directoryId,
    includeDescendantDirectories: url.searchParams.get('includeDescendantDirectories'),
    currentDirectoryText: state.currentDirectoryText,
    rowCount: state.rowCount,
    url: state.url
  }
  record('PASS', 'select directory', result)
  return result
}

async function assertRefreshAndReopenRestoreDirectory(page) {
  const selected = await selectFirstDirectory(page)

  const reloadResponsePromise = waitForDirectoryListResponse(page, selected.directoryId)
  await page.reload({ waitUntil: 'domcontentloaded', timeout: config.timeout })
  const afterReload = await assertDirectoryListLoaded(
    page,
    'reload restores selected directory list',
    selected.directoryId,
    reloadResponsePromise
  )

  const reopenResponsePromise = waitForDirectoryListResponse(page, selected.directoryId)
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const afterReopen = await assertDirectoryListLoaded(
    page,
    'no-query reopen restores remembered directory list',
    selected.directoryId,
    reopenResponsePromise
  )

  evidence.restoreResult = {
    selected,
    afterReload,
    afterReopen
  }
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
    await assertRefreshAndReopenRestoreDirectory(page)
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
  } finally {
    await browser.close()
  }
}

main()
  .then(() => {
    console.log('PASS: dcc browser refresh and reopen restore selected directory list')
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
