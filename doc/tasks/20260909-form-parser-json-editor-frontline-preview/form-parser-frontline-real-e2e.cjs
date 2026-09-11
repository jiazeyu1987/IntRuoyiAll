const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const TASK_DIR = __dirname
const WORKSPACE_ROOT = path.resolve(TASK_DIR, '../../..')
const FRONTEND_ROOT = path.join(WORKSPACE_ROOT, 'IntRuoyiFronted')
const frontendRequire = createRequire(path.join(FRONTEND_ROOT, 'package.json'))
const { chromium } = frontendRequire('playwright')

const BASE_URL = (process.env.FORM_PARSER_E2E_FRONTEND_URL || '').replace(/\/+$/, '')
const BACKEND_URL = (process.env.FORM_PARSER_E2E_BACKEND_URL || '').replace(/\/+$/, '')
const TENANT = process.env.FORM_PARSER_E2E_TENANT || ''
const USERNAME = process.env.FORM_PARSER_E2E_USERNAME || ''
const PASSWORD = process.env.FORM_PARSER_E2E_PASSWORD || ''
const WORD_PATH = process.env.FORM_PARSER_E2E_WORD_PATH || ''
const RESULT_PATH =
  process.env.FORM_PARSER_E2E_RESULT_PATH || path.join(TASK_DIR, 'e2e-result.json')
const SCREENSHOT_DIR =
  process.env.FORM_PARSER_E2E_SCREENSHOT_DIR || path.join(TASK_DIR, 'screenshots')
const RUNTIME_PROFILE = process.env.FORM_PARSER_E2E_PROFILE || 'unknown'
const RUNTIME_SLOT = process.env.FORM_PARSER_E2E_SLOT || 'unknown'
const CHROME_PATH =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const PARSE_API =
  '/admin-api/mes/pro/batch-record-report/production-batch-record/total-recognition-json'
const TARGET_ROUTE = '/mdm/form-center/parser'
const TARGET_ROUTE_HINT = 'form-center/parser'

function requireConfig() {
  assert.ok(BASE_URL, 'FORM_PARSER_E2E_FRONTEND_URL is required')
  assert.ok(BACKEND_URL, 'FORM_PARSER_E2E_BACKEND_URL is required')
  assert.equal(TENANT, '芋道源码', 'E2E tenant must be 芋道源码')
  assert.equal(USERNAME, 'admin', 'E2E username must be admin')
  assert.ok(PASSWORD, 'FORM_PARSER_E2E_PASSWORD is required and must not be logged')
  assert.ok(fs.existsSync(WORD_PATH), `Word fixture is missing: ${WORD_PATH}`)
  assert.ok(fs.existsSync(CHROME_PATH), `Chrome executable is missing: ${CHROME_PATH}`)
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function sanitizeUrl(raw) {
  return String(raw || '')
    .replace(/accessToken=[^&]+/gi, 'accessToken=<redacted>')
    .replace(/refreshToken=[^&]+/gi, 'refreshToken=<redacted>')
}

function isAllowedMutation(url, method) {
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    return true
  }
  return (
    url.includes('/admin-api/system/auth/login') ||
    url.includes(PARSE_API) ||
    url.includes('/admin-api/system/auth/logout')
  )
}

function flattenMenus(nodes, result = []) {
  if (!Array.isArray(nodes)) {
    return result
  }
  for (const node of nodes) {
    if (!node || typeof node !== 'object') {
      continue
    }
    result.push(node)
    flattenMenus(node.children, result)
  }
  return result
}

function menuHasParser(permissionData) {
  const menus = flattenMenus(permissionData?.menus || permissionData?.menuList || [])
  return menus.some((menu) => {
    const values = [
      menu.name,
      menu.path,
      menu.component,
      menu.permission,
      menu.meta?.title,
      menu.title
    ]
      .filter(Boolean)
      .map((value) => String(value))
    return values.some(
      (value) => value.includes('表单解析') || value.includes(TARGET_ROUTE_HINT)
    )
  })
}

async function readBusinessPayload(response, label) {
  assert.equal(response.ok(), true, `${label} HTTP status ${response.status()}`)
  const payload = await response.json()
  assert.ok([0, 200].includes(payload.code), `${label} business code ${payload.code}`)
  return payload.data
}

async function clickVisible(locator, label) {
  await locator.first().waitFor({ state: 'visible', timeout: 60000 })
  await locator.first().click()
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form
    .locator('input[placeholder="请输入租户名称"], .el-select input[role="combobox"], input.el-select__input')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  await tenantInput.click()
  await tenantInput.fill(TENANT)
  await clickVisible(page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }), 'tenant option')

  await form.locator('input[placeholder="请输入用户名"]:visible').first().fill(USERNAME)
  await form.locator('input[type="password"]:visible').first().fill(PASSWORD)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 180000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  await readBusinessPayload(await loginResponsePromise, 'login')
  const permissionData = await readBusinessPayload(
    await permissionResponsePromise,
    'permission info'
  )
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  assert.equal(menuHasParser(permissionData), true, 'admin permission info must contain 表单解析 menu')
  return permissionData
}

async function openParserPage(page) {
  await page.goto(`${BASE_URL}${TARGET_ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.form-parser-page').waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(await page.getByText('系统异常').count(), 0, 'form parser page must not show 系统异常')
  await page.getByRole('heading', { name: '表单解析' }).waitFor({ state: 'visible' })
  await page.getByRole('button', { name: '生产批记录' }).waitFor({ state: 'visible' })
  await page.getByRole('button', { name: 'QA检验规程' }).waitFor({ state: 'visible' })
  await page.getByRole('button', { name: '过程检验记录' }).waitFor({ state: 'visible' })
}

function findFirstNumberParameter(processes) {
  for (const process of processes) {
    for (const group of process.equipmentGroups || []) {
      for (const parameter of group.parameters || []) {
        if (parameter?.ui?.control === 'number') {
          return { process, group, parameter }
        }
      }
    }
  }
  return null
}

function nextNumberValue(ui) {
  const current = Number(ui.defaultValue ?? ui.min ?? 0)
  const step = Number(ui.step || 1)
  const max = ui.max === undefined || ui.max === null ? undefined : Number(ui.max)
  const min = ui.min === undefined || ui.min === null ? undefined : Number(ui.min)
  if (max === undefined || current + step <= max) {
    return current + step
  }
  if (min === undefined || current - step >= min) {
    return current - step
  }
  return current
}

async function uploadAndReadJson(page) {
  const fileInput = page.locator('.form-parser-page input[type="file"]').first()
  await fileInput.waitFor({ state: 'attached', timeout: 30000 })

  const parseResponsePromise = page.waitForResponse(
    (response) => response.url().includes(PARSE_API) && response.request().method() === 'POST',
    { timeout: 600000 }
  )
  const downloadPromise = page.waitForEvent('download', { timeout: 600000 })
  await fileInput.setInputFiles(WORD_PATH)

  const parseData = await readBusinessPayload(await parseResponsePromise, 'production batch parse')
  const download = await downloadPromise
  const downloadedJsonPath = path.join(SCREENSHOT_DIR, download.suggestedFilename())
  await download.saveAs(downloadedJsonPath)

  await page.locator('.form-parser-workbench').waitFor({ state: 'visible', timeout: 60000 })
  await page
    .locator('.form-parser-json-editor textarea')
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })

  const editorText = await page.locator('.form-parser-json-editor textarea').first().inputValue()
  const recognitionJson = JSON.parse(editorText)
  assert.ok(
    recognitionJson.schemaVersion !== undefined && recognitionJson.schemaVersion !== null,
    'recognized JSON must contain schemaVersion'
  )
  assert.ok(recognitionJson.product?.name, 'recognized JSON must contain product name')
  assert.ok(Array.isArray(recognitionJson.processes), 'recognized JSON must contain processes')
  assert.ok(recognitionJson.processes.length > 0, 'recognized JSON must contain at least one process')
  assert.ok(findFirstNumberParameter(recognitionJson.processes), 'recognized JSON must contain numeric equipment parameter')

  return { recognitionJson, parseData, downloadedJsonPath }
}

async function assertPreviewDefaults(page, recognitionJson) {
  const firstProcess = recognitionJson.processes[0]
  const firstGroup = (firstProcess.equipmentGroups || [])[0]
  const firstDevice = (firstGroup?.equipmentOptions || [])[0]
  const firstNumber = findFirstNumberParameter([firstProcess])?.parameter

  await page
    .locator('.form-parser-frontline-preview')
    .filter({ hasText: recognitionJson.product.name })
    .waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.form-parser-process-card').filter({ hasText: firstProcess.name }).waitFor({
    state: 'visible',
    timeout: 60000
  })

  for (const material of [...(firstProcess.inputs || []), ...(firstProcess.outputs || [])].slice(0, 4)) {
    await page.getByText(material.name, { exact: false }).first().waitFor({ state: 'visible' })
  }
  if (firstDevice) {
    await page.getByText(firstDevice.code || firstDevice.name, { exact: false }).first().waitFor({
      state: 'visible'
    })
  }
  if (firstNumber) {
    const field = page
      .locator('.form-parser-parameter-field:visible')
      .filter({ hasText: firstNumber.name })
      .first()
    await field.waitFor({ state: 'visible' })
    await field
      .locator('input')
      .first()
      .evaluate((input, expected) => {
        if (String(input.value) !== String(expected)) {
          throw new Error(`default value mismatch: ${input.value} !== ${expected}`)
        }
      }, firstNumber.ui.defaultValue)
  }

  const disabledButtons = await page.locator('button[disabled]:visible').filter({
    hasText: /主页|重填|正式提交/
  }).count()
  assert.ok(disabledButtons >= 3, 'frontline readonly header/footer buttons must be disabled')
}

async function applyEditedJson(page, recognitionJson) {
  const edited = JSON.parse(JSON.stringify(recognitionJson))
  const originalProcessName = edited.processes[0].name
  const editedProcessName = `${originalProcessName}-E2E校验`
  edited.processes[0].name = editedProcessName

  const numberParameter = findFirstNumberParameter([edited.processes[0]])
  assert.ok(numberParameter, 'first process must contain numeric parameter for E2E edit')
  const editedValue = nextNumberValue(numberParameter.parameter.ui)
  numberParameter.parameter.ui.defaultValue = editedValue
  numberParameter.parameter.referenceValue = String(editedValue)

  const editor = page.locator('.form-parser-json-editor textarea').first()
  await editor.fill(JSON.stringify(edited, null, 2))
  await page.getByRole('button', { name: '应用' }).click()
  await page.locator('.form-parser-process-card').filter({ hasText: editedProcessName }).waitFor({
    state: 'visible',
    timeout: 30000
  })

  const field = page
    .locator('.form-parser-parameter-field:visible')
    .filter({ hasText: numberParameter.parameter.name })
    .first()
  await field.waitFor({ state: 'visible' })
  const input = field.locator('input').first()
  await input.evaluate((node, expected) => {
    if (String(node.value) !== String(expected)) {
      throw new Error(`edited default value mismatch: ${node.value} !== ${expected}`)
    }
  }, editedValue)

  const interactiveValue = nextNumberValue({ ...numberParameter.parameter.ui, defaultValue: editedValue })
  await input.fill(String(interactiveValue))
  await input.press('Tab')
  await input.evaluate((node, expected) => {
    if (String(node.value) !== String(expected)) {
      throw new Error(`interactive numeric value mismatch: ${node.value} !== ${expected}`)
    }
  }, interactiveValue)

  return { edited, editedProcessName, editedValue, interactiveValue }
}

async function verifyProcessControls(page, recognitionJson) {
  const processCount = recognitionJson.processes.length
  await page.locator('.form-parser-process-card').first().click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '选择工序' }).first()
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.getByText(`共 ${processCount} 个工序`, { exact: false }).waitFor({ state: 'visible' })

  if (processCount > 1) {
    const secondProcessName = recognitionJson.processes[1].name
    await dialog.locator('.form-parser-process-option').nth(1).click()
    await page.locator('.form-parser-process-card').filter({ hasText: secondProcessName }).waitFor({
      state: 'visible',
      timeout: 30000
    })
    await page.locator('.form-parser-nav-button').first().click()
    await page.locator('.form-parser-process-card').filter({ hasText: recognitionJson.processes[0].name }).waitFor({
      state: 'visible',
      timeout: 30000
    })
    await page.locator('.form-parser-nav-button').last().click()
    await page.locator('.form-parser-process-card').filter({ hasText: secondProcessName }).waitFor({
      state: 'visible',
      timeout: 30000
    })
  } else {
    await page.keyboard.press('Escape')
  }
}

async function downloadCurrentJson(page, expectedName) {
  const downloadPromise = page.waitForEvent('download', { timeout: 30000 })
  await page.getByRole('button', { name: '下载当前JSON' }).click()
  const download = await downloadPromise
  const savedPath = path.join(SCREENSHOT_DIR, `current-${download.suggestedFilename()}`)
  await download.saveAs(savedPath)
  const downloaded = JSON.parse(fs.readFileSync(savedPath, 'utf8'))
  assert.equal(downloaded.processes[0].name, expectedName)
  return savedPath
}

async function main() {
  requireConfig()
  ensureDir(SCREENSHOT_DIR)
  ensureDir(path.dirname(RESULT_PATH))

  const observed = {
    targetResponses: [],
    failedRequests: [],
    pageErrors: [],
    consoleErrors: [],
    unexpectedMutations: []
  }
  const browser = await chromium.launch({
    headless: true,
    executablePath: CHROME_PATH,
    args: ['--disable-dev-shm-usage']
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 980 }, acceptDownloads: true })

  page.on('pageerror', (error) => {
    observed.pageErrors.push(error.message)
  })
  page.on('console', (message) => {
    if (message.type() === 'error') {
      observed.consoleErrors.push(message.text())
    }
  })
  page.on('requestfailed', (request) => {
    const url = request.url()
    if (url.startsWith(BASE_URL) || url.startsWith(BACKEND_URL)) {
      observed.failedRequests.push({
        method: request.method(),
        url: sanitizeUrl(url),
        failure: request.failure()?.errorText
      })
    }
  })
  page.on('request', (request) => {
    const url = request.url()
    const method = request.method()
    if ((url.startsWith(BASE_URL) || url.startsWith(BACKEND_URL)) && !isAllowedMutation(url, method)) {
      observed.unexpectedMutations.push({ method, url: sanitizeUrl(url) })
    }
  })
  page.on('response', (response) => {
    const url = response.url()
    if (
      url.includes('/admin-api/system/auth/login') ||
      url.includes('/admin-api/system/auth/get-permission-info') ||
      url.includes(PARSE_API)
    ) {
      observed.targetResponses.push({
        method: response.request().method(),
        url: sanitizeUrl(url),
        status: response.status()
      })
    }
  })

  const result = {
    status: 'running',
    runtime: {
      frontendUrl: BASE_URL,
      backendUrl: BACKEND_URL,
      profile: RUNTIME_PROFILE,
      slot: RUNTIME_SLOT
    },
    actor: {
      tenant: TENANT,
      username: USERNAME
    },
    targetRoute: TARGET_ROUTE,
    wordFixture: WORD_PATH,
    startedAt: new Date().toISOString()
  }

  try {
    await login(page)
    await openParserPage(page)
    const { recognitionJson, downloadedJsonPath } = await uploadAndReadJson(page)
    await assertPreviewDefaults(page, recognitionJson)
    const editedResult = await applyEditedJson(page, recognitionJson)
    const currentJsonPath = await downloadCurrentJson(page, editedResult.editedProcessName)
    await verifyProcessControls(page, editedResult.edited)

    const screenshotPath = path.join(SCREENSHOT_DIR, 'form-parser-frontline-real-e2e.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })

    assert.deepEqual(observed.pageErrors, [], 'page errors must be empty')
    const criticalFailedRequests = observed.failedRequests.filter(
      (request) => !(request.method === 'GET' && request.failure === 'net::ERR_ABORTED')
    )
    assert.deepEqual(criticalFailedRequests, [], 'same-origin critical failed requests must be empty')
    assert.deepEqual(observed.unexpectedMutations, [], 'no unexpected business mutation request is allowed')

    result.status = 'passed'
    result.finishedAt = new Date().toISOString()
    result.summary = {
      productName: recognitionJson.product.name,
      processCount: recognitionJson.processes.length,
      downloadedJsonPath,
      currentJsonPath,
      screenshotPath,
      editedProcessName: editedResult.editedProcessName,
      editedDefaultValue: editedResult.editedValue,
      interactiveValue: editedResult.interactiveValue
    }
    result.observed = observed
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
  } catch (error) {
    const failureScreenshot = path.join(SCREENSHOT_DIR, 'form-parser-frontline-real-e2e-failed.png')
    await page.screenshot({ path: failureScreenshot, fullPage: true }).catch(() => undefined)
    result.status = 'failed'
    result.finishedAt = new Date().toISOString()
    result.error = error.stack || error.message
    result.failureScreenshot = failureScreenshot
    result.observed = observed
    fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
    throw error
  } finally {
    await browser.close()
  }

  console.log(JSON.stringify({ status: result.status, resultPath: RESULT_PATH }, null, 2))
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exitCode = 1
})
