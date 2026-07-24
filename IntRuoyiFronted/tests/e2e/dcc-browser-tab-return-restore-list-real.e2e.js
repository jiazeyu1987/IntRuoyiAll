const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260712-dcc-browser-tab-return-restore-list'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-tab-return-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-tab-return-final.png')

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
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/dcc/controlled-file/browser')
  await page.goto(loginUrl.toString(), {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await tenantOption.waitFor({ state: 'visible', timeout: 5000 }).catch(() => undefined)
    if (await tenantOption.count()) {
      await tenantOption.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
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
    ).filter((row) => row.textContent && row.textContent.trim())
    const currentTreeNode = document.querySelector('.browser-directory-scroll .el-tree-node.is-current')
    return {
      url: location.href,
      rowCount: rows.length,
      hasSelectDirectoryPrompt: text.includes('请先选择目录'),
      hasAnyFileAction: text.includes('预览') || text.includes('下载'),
      currentDirectoryText: (currentTreeNode?.textContent || '').replace(/\s+/g, ' ').trim()
    }
  })
}

async function assertListRestored(page, step, expectedDirectoryId, responsePromise) {
  const response = await responsePromise
  const url = new URL(response.url())
  const directoryId = url.searchParams.get('directoryId')
  const includeDescendantDirectories = url.searchParams.get('includeDescendantDirectories')
  await settle(page)
  const state = await readListState(page)
  if (directoryId !== String(expectedDirectoryId)) {
    throw new Error(`${step} requested different directory: ${response.url()}`)
  }
  if (includeDescendantDirectories !== 'false') {
    throw new Error(`${step} did not keep current-directory-only query: ${response.url()}`)
  }
  if (state.hasSelectDirectoryPrompt) {
    throw new Error(`${step} still shows select-directory prompt: ${JSON.stringify(state)}`)
  }
  if (!state.currentDirectoryText) {
    throw new Error(`${step} did not highlight selected directory: ${JSON.stringify(state)}`)
  }
  const result = {
    directoryId,
    includeDescendantDirectories,
    rowCount: state.rowCount,
    hasAnyFileAction: state.hasAnyFileAction,
    currentDirectoryText: state.currentDirectoryText,
    url: state.url
  }
  record('PASS', step, result)
  return result
}

async function selectNonEmptyDirectory(page) {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.waitForSelector('.browser-directory-scroll .el-tree-node__content', {
    state: 'visible',
    timeout: config.timeout
  })
  await settle(page)

  const directTarget = page
    .locator('.browser-directory-scroll .el-tree-node__content')
    .filter({ hasText: 'Codex Local DCC Documents' })
    .first()
  const targetCount = await directTarget.count()
  const target = targetCount
    ? directTarget
    : page.locator('.browser-directory-scroll .el-tree-node__content').first()
  const responsePromise = waitForDirectoryListResponse(page)
  await target.click({ position: { x: 140, y: 14 } })
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
  const selected = {
    directoryId,
    includeDescendantDirectories: url.searchParams.get('includeDescendantDirectories'),
    rowCount: state.rowCount,
    hasAnyFileAction: state.hasAnyFileAction,
    currentDirectoryText: state.currentDirectoryText,
    url: state.url
  }
  record('PASS', 'select directory before tab switch', selected)
  return selected
}

async function clickSidebarMenu(page, text) {
  const menuItem = page.locator('.el-menu').getByText(text, { exact: true }).first()
  await menuItem.waitFor({ state: 'visible', timeout: config.timeout })
  await menuItem.click()
}

async function assertTabReturnRestore(page) {
  const selected = await selectNonEmptyDirectory(page)
  await clickSidebarMenu(page, '文件提交')
  await page.waitForURL((url) => url.pathname.includes('/dcc/controlled-file/upload'), {
    timeout: config.timeout
  })
  await settle(page)
  record('PASS', 'navigate to submit tab', { url: page.url() })

  const returnResponsePromise = waitForDirectoryListResponse(page, selected.directoryId)
  await clickSidebarMenu(page, '文件查阅')
  await page.waitForURL((url) => url.pathname.includes('/dcc/controlled-file/browser'), {
    timeout: config.timeout
  })
  const restored = await assertListRestored(
    page,
    'return from submit restores selected directory list',
    selected.directoryId,
    returnResponsePromise
  )
  evidence.restoreResult = { selected, restored }
  await page.screenshot({ path: screenshotPath, fullPage: true })
}

async function main() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1680, height: 920 } })
  const page = await context.newPage()
  page.setDefaultTimeout(config.timeout)
  page.setDefaultNavigationTimeout(config.timeout)
  page.on('console', (message) => {
    evidence.console.push({ type: message.type(), text: message.text() })
  })
  page.on('pageerror', (error) => {
    evidence.pageErrors.push({ message: error.message, stack: error.stack })
  })

  try {
    await login(page)
    await assertTabReturnRestore(page)
    evidence.finishedAt = new Date().toISOString()
    evidence.result = 'PASS'
    writeEvidence()
  } catch (error) {
    evidence.finishedAt = new Date().toISOString()
    evidence.result = 'FAIL'
    evidence.error = {
      message: error.message,
      stack: error.stack
    }
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    writeEvidence()
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
