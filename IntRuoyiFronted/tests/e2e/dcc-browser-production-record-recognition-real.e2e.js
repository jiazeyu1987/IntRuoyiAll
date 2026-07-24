const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const taskId = '20260703-dcc-browser-production-record-recognition-e2e'
const evidenceDir = path.join(workspaceRoot, 'doc', 'tasks', taskId)
const evidencePath = path.join(evidenceDir, 'real-e2e-evidence.json')
const screenshotPath = path.join(evidenceDir, 'real-e2e-final.png')

const config = {
  baseUrl: process.env.DCC_E2E_BASE_URL || 'http://localhost:8081',
  tenant: process.env.DCC_E2E_TEST_TENANT || '测试租户',
  username: process.env.DCC_E2E_TEST_USERNAME || 'aoteman',
  password: process.env.DCC_E2E_TEST_PASSWORD || '111111',
  executablePath:
    process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  headless: process.env.DCC_E2E_HEADLESS !== 'false',
  timeout: Number(process.env.DCC_E2E_TIMEOUT || 120000)
}

const evidence = {
  taskId,
  startedAt: new Date().toISOString(),
  baseUrl: config.baseUrl,
  tenant: config.tenant,
  username: config.username,
  targetPath: '/dcc/controlled-file/browser',
  actions: [],
  api: [],
  console: [],
  pageErrors: [],
  authContext: {
    authorization: '',
    tenantId: ''
  }
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

function assertNoRuntimeFailures() {
  const failedApis = evidence.api.filter((entry) => entry.type === 'response' && entry.status >= 400)
  const failedRequests = evidence.api.filter((entry) => {
    if (entry.type !== 'requestfailed') {
      return false
    }
    if (
      entry.failure === 'net::ERR_ABORTED' &&
      entry.method === 'GET' &&
      (
        entry.url.includes('/system/notify-message/get-unread-count') ||
        entry.url.includes('/dcc/directories/tree') ||
        entry.url.includes('/dcc/file-categories')
      )
    ) {
      return false
    }
    return true
  })
  const severeConsole = evidence.console.filter((entry) => ['error'].includes(entry.type))
  if (failedApis.length || failedRequests.length || severeConsole.length || evidence.pageErrors.length) {
    throw new Error(
      `runtime failures: api=${failedApis.length}, request=${failedRequests.length}, console=${severeConsole.length}, page=${evidence.pageErrors.length}`
    )
  }
}

async function settle(page, timeout = 800) {
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
  await captureAuthContext(page)
  record('PASS', 'login', { url: page.url() })
}

async function captureAuthContext(page) {
  const authContext = await page.evaluate(() => {
    const parseCacheValue = (key) => {
      const direct = window.localStorage.getItem(key) || window.sessionStorage.getItem(key)
      if (direct) {
        return direct
      }
      for (const storage of [window.localStorage, window.sessionStorage]) {
        for (let index = 0; index < storage.length; index += 1) {
          const storageKey = storage.key(index)
          if (!storageKey) {
            continue
          }
          const rawValue = storage.getItem(storageKey)
          if (!rawValue || !rawValue.includes(key)) {
            continue
          }
          try {
            const parsed = JSON.parse(rawValue)
            const data = parsed?.value || parsed?.data || parsed
            if (data && typeof data === 'object' && data[key]) {
              return String(data[key])
            }
          } catch (error) {
            const match = rawValue.match(new RegExp(`"${key}"\\s*:\\s*"?([^",}]+)"?`))
            if (match) {
              return match[1]
            }
          }
        }
      }
      return ''
    }
    return {
      accessToken: parseCacheValue('ACCESS_TOKEN'),
      tenantId: parseCacheValue('tenantId')
    }
  })
  if (!authContext.accessToken) {
    throw new Error('ACCESS_TOKEN is missing after login')
  }
  if (!authContext.tenantId) {
    throw new Error('tenantId is missing after login')
  }
  evidence.authContext = {
    authorization: `Bearer ${authContext.accessToken}`,
    tenantId: String(authContext.tenantId)
  }
  record('PASS', 'capture auth context', {
    hasAuthorization: true,
    tenantId: evidence.authContext.tenantId
  })
}

async function selectProductionRecordDirectory(page) {
  await page.goto(`${config.baseUrl}/dcc/controlled-file/browser`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.waitForSelector('.browser-directory-scroll', { state: 'visible', timeout: config.timeout })
  await settle(page)

  const search = page.locator('.browser-directory-wrap input[placeholder="搜索目录"]').first()
  await search.fill('08.生产记录')
  await page.waitForSelector('.browser-directory-search__item', { state: 'visible', timeout: config.timeout })

  const candidates = await page.locator('.browser-directory-search__item').evaluateAll((items) =>
    items.map((item, index) => ({
      index,
      text: (item.innerText || '').trim().replace(/\s+/g, ' ')
    }))
  )
  evidence.directorySearchCandidates = candidates
  const match = candidates.find((item) => item.text.includes('3.DMR') && item.text.includes('08.生产记录')) ||
    candidates.find((item) => item.text.includes('08.生产记录'))
  if (!match) {
    throw new Error(`08.生产记录 directory not found; candidates=${candidates.map((item) => item.text).join(' | ')}`)
  }

  await page.locator('.browser-directory-search__item').nth(match.index).click()
  await page.waitForFunction(() => {
    const title = document.querySelector('.browser-list-title')?.textContent || ''
    const subtitle = document.querySelector('.browser-list-subtitle')?.textContent || ''
    return title.includes('08.生产记录') || subtitle.includes('08.生产记录')
  }, null, { timeout: config.timeout })
  await settle(page)
  const context = await page.locator('.browser-list-header').innerText({ timeout: config.timeout })
  record('PASS', 'select directory', { selected: match.text, context })
}

async function startRecognition(page) {
  const trigger = page.getByTestId('dcc-browser-batch-recognition-trigger')
  await trigger.waitFor({ state: 'visible', timeout: config.timeout })
  await trigger.click()
  await page.getByRole('dialog', { name: '识别当前文件夹及子文件夹' }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const dialogText = await page.getByRole('dialog', { name: '识别当前文件夹及子文件夹' }).innerText()
  if (!dialogText.includes('当前目录 + 子目录') || !dialogText.includes('08.生产记录')) {
    throw new Error(`recognition dialog context mismatch: ${dialogText}`)
  }
  record('PASS', 'open recognition dialog', { dialogText })

  const createTaskResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/dcc/controlled-files/batch-recognition/tasks') &&
      response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await page.getByTestId('dcc-browser-batch-recognition-confirm').click()
  const createTaskResponse = await createTaskResponsePromise
  captureAuthContextFromRequest(createTaskResponse.request())
  let createTaskPayload = null
  try {
    createTaskPayload = await createTaskResponse.json()
  } catch (error) {
    throw new Error(`create task returned non-json: HTTP ${createTaskResponse.status()} ${error.message}`)
  }
  evidence.createTaskResponse = {
    status: createTaskResponse.status(),
    payload: createTaskPayload
  }
  if (!createTaskResponse.ok() || ![0, 200].includes(createTaskPayload.code)) {
    throw new Error(`create recognition task failed: HTTP ${createTaskResponse.status()} ${createTaskPayload.msg || JSON.stringify(createTaskPayload)}`)
  }
  const task = createTaskPayload.data || createTaskPayload
  if (!task.taskId) {
    throw new Error(`create recognition task response missing taskId: ${JSON.stringify(createTaskPayload)}`)
  }
  record('PASS', 'create recognition task', { task })
  return task.taskId
}

function captureAuthContextFromRequest(request) {
  const headers = request.headers()
  const authorization = headers.authorization || headers.Authorization || ''
  const tenantId = headers['tenant-id'] || headers['Tenant-Id'] || ''
  if (!authorization) {
    throw new Error('创建识别任务请求缺少 Authorization 请求头，无法复用真实登录态轮询。')
  }
  if (!tenantId) {
    throw new Error('创建识别任务请求缺少 tenant-id 请求头，无法复用真实租户轮询。')
  }
  evidence.authContext = {
    authorization,
    tenantId: String(tenantId)
  }
  record('PASS', 'capture recognition request auth context', {
    hasAuthorization: true,
    tenantId: evidence.authContext.tenantId
  })
}

async function waitForRecognitionTerminal(page, taskId) {
  await page.getByRole('dialog', { name: '批量识别进度' }).waitFor({
    state: 'visible',
    timeout: config.timeout
  })
  const deadline = Date.now() + Number(process.env.DCC_E2E_RECOGNITION_TIMEOUT || 15 * 60 * 1000)
  let lastTask = null
  while (Date.now() < deadline) {
    const response = await page.request.get(`${config.baseUrl}/admin-api/dcc/controlled-files/batch-recognition/tasks/${taskId}`, {
      headers: {
        Authorization: evidence.authContext.authorization,
        'tenant-id': evidence.authContext.tenantId
      }
    })
    const payload = await response.json().catch(() => null)
    evidence.lastTaskPoll = {
      status: response.status(),
      payload
    }
    if (!response.ok() || !payload || ![0, 200].includes(payload.code)) {
      throw new Error(`poll recognition task failed: HTTP ${response.status()} ${payload?.msg || JSON.stringify(payload)}`)
    }
    lastTask = payload.data || payload
    evidence.taskSnapshots = evidence.taskSnapshots || []
    evidence.taskSnapshots.push({
      at: new Date().toISOString(),
      task: lastTask
    })
    if (!['WAITING', 'RUNNING'].includes(lastTask.status)) {
      break
    }
    await page.waitForTimeout(3000)
  }
  if (!lastTask) {
    throw new Error('recognition task was never loaded')
  }
  if (['WAITING', 'RUNNING'].includes(lastTask.status)) {
    throw new Error(`recognition task timed out before terminal state: ${JSON.stringify(lastTask)}`)
  }
  const progressText = await page.getByRole('dialog', { name: '批量识别进度' }).innerText().catch(() => '')
  record('PASS', 'recognition terminal', { task: lastTask, progressText })
  if (lastTask.status !== 'COMPLETED' || Number(lastTask.failedCount || 0) > 0) {
    throw new Error(`recognition completed with errors: ${JSON.stringify(lastTask)}`)
  }
}

async function main() {
  fs.mkdirSync(evidenceDir, { recursive: true })
  if (!fs.existsSync(config.executablePath)) {
    throw new Error(`browser executable missing: ${config.executablePath}`)
  }
  const browser = await chromium.launch({
    headless: config.headless,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 960 },
      locale: 'zh-CN',
      acceptDownloads: true
    })
    const page = await context.newPage()
    page.setDefaultTimeout(config.timeout)
    page.setDefaultNavigationTimeout(config.timeout)

    page.on('console', (message) => {
      evidence.console.push({
        type: message.type(),
        text: message.text(),
        location: message.location()
      })
    })
    page.on('pageerror', (error) => {
      evidence.pageErrors.push({ message: error.message, stack: error.stack })
    })
    page.on('requestfailed', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/') || url.includes('/system/')) {
        evidence.api.push({
          type: 'requestfailed',
          method: request.method(),
          url,
          failure: request.failure()?.errorText
        })
      }
    })
    page.on('response', (response) => {
      const url = response.url()
      if (url.includes('/admin-api/') || url.includes('/system/')) {
        evidence.api.push({
          type: 'response',
          method: response.request().method(),
          url,
          status: response.status()
        })
      }
    })

    await login(page)
    await selectProductionRecordDirectory(page)
    const taskId = await startRecognition(page)
    await waitForRecognitionTerminal(page, taskId)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    assertNoRuntimeFailures()
    record('PASS', 'final assertion', { screenshotPath })
  } catch (error) {
    evidence.failure = {
      message: error.message,
      stack: error.stack
    }
    record('FAIL', 'real e2e', { message: error.message })
    throw error
  } finally {
    evidence.finishedAt = new Date().toISOString()
    writeEvidence()
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
