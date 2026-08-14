const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('../../../IntRuoyiFronted/node_modules/playwright')

const TASK_ID = '20260808-dcc-upload-optimization-fixes'
const BASE_URL = (process.env.DCC_UPLOAD_HISTORY_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const TENANT = process.env.DCC_UPLOAD_HISTORY_E2E_TENANT || '芋道源码'
const USERNAME = process.env.DCC_UPLOAD_HISTORY_E2E_USERNAME || 'zhaohaichen'
const PASSWORD = process.env.DCC_UPLOAD_HISTORY_E2E_PASSWORD
const TARGET_PATH = '/dcc/controlled-file/upload'
const RESULT_PATH = path.resolve(__dirname, 'dcc-upload-history-revision-readonly-result.json')
const SCREENSHOT_PATH = path.resolve(process.cwd(), 'output', 'playwright', TASK_ID, 'dcc-upload-history-revision-readonly.png')

const PROJECT = {
  keyword: 'IDI',
  code: 'IDI',
  name: '按压式球囊扩充压力泵',
  docControlNo: '1'
}
const TAXONOMY_PATH = ['技术文档', '设计和开发策划阶段', '技术调研报告']
const TARGET_HISTORY_FILE_NAME = '按压式球囊扩充压力泵技术调研报告.pdf'
const TARGET_HISTORY_VERSION = 'V1.0'

function writeResult(result) {
  fs.mkdirSync(path.dirname(RESULT_PATH), { recursive: true })
  fs.mkdirSync(path.dirname(SCREENSHOT_PATH), { recursive: true })
  fs.writeFileSync(RESULT_PATH, `${JSON.stringify(result, null, 2)}\n`, 'utf8')
}

function redactUrl(rawUrl) {
  try {
    const parsed = new URL(rawUrl)
    for (const key of [...parsed.searchParams.keys()]) {
      if (/token|password|secret|authorization/i.test(key)) {
        parsed.searchParams.set(key, '<redacted>')
      }
    }
    return parsed.toString()
  } catch {
    return String(rawUrl || '').replace(/(token|password|secret)=([^&\s]+)/gi, '$1=<redacted>')
  }
}

function assertPrerequisites() {
  assert.match(new URL(BASE_URL).hostname, /^(localhost|127\.0\.0\.1)$/i, 'E2E must target local frontend')
  assert.equal(TENANT, '芋道源码', `readonly zhaohaichen E2E must use 芋道源码, got ${TENANT}`)
  assert.equal(USERNAME, 'zhaohaichen', `readonly upload E2E must use zhaohaichen, got ${USERNAME}`)
  assert.ok(PASSWORD, 'DCC_UPLOAD_HISTORY_E2E_PASSWORD is required')
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    assert.ok(
      fs.existsSync(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH),
      `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH does not exist: ${process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH}`
    )
  }
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(TARGET_PATH)}`, {
    waitUntil: 'commit',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: TENANT }).first().click()
  } else {
    await form.locator('input[placeholder="请输入租户名称"]').first().fill(TENANT)
  }
  await form.locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible').first().fill(USERNAME)
  await form.locator('input[type="password"]:visible').first().fill(PASSWORD)

  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const payload = await response.json()
  assert.equal(response.ok(), true, `login HTTP ${response.status()}`)
  assert.ok([0, 200].includes(payload.code), `login business code ${payload.code}: ${payload.msg || ''}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function dismissKnownOverlay(page, evidence) {
  const overlay = page.locator('vite-error-overlay, .vite-error-overlay').first()
  if ((await overlay.count()) > 0) {
    evidence.viteOverlayText = (await overlay.textContent().catch(() => ''))?.replace(/\s+/g, ' ').trim().slice(0, 500)
    await page.keyboard.press('Escape').catch(() => undefined)
    await page.waitForTimeout(500)
  }
}

function formItem(page, label) {
  return page
    .locator('.el-form-item')
    .filter({ has: page.locator('.el-form-item__label').filter({ hasText: label }) })
    .first()
}

function readWsCacheScript() {
  return `(key) => {
    const raw = window.localStorage.getItem(key)
    if (!raw) return undefined
    let current = raw
    for (let index = 0; index < 8; index += 1) {
      if (typeof current === 'string') {
        const trimmed = current.trim()
        if (!trimmed) return undefined
        try {
          current = JSON.parse(trimmed)
          continue
        } catch {
          return trimmed.replace(/^"(.*)"$/, '$1')
        }
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) {
          current = current.accessToken
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'v')) {
          current = current.v
          continue
        }
        if (Object.prototype.hasOwnProperty.call(current, 'value')) {
          current = current.value
          continue
        }
      }
      return current
    }
    return current
  }`
}

async function apiGet(page, url) {
  const result = await page.evaluate(
    async ({ url, readWsCacheSource }) => {
      const readWsCache = eval(readWsCacheSource)
      const accessToken = readWsCache('ACCESS_TOKEN')
      const tenantId = readWsCache('tenantId')
      const visitTenantId = readWsCache('visitTenantId')
      if (!accessToken || !tenantId) {
        throw new Error('missing authenticated browser cache')
      }
      const headers = {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
        'tenant-id': String(tenantId)
      }
      if (visitTenantId) {
        headers['visit-tenant-id'] = String(visitTenantId)
      }
      const response = await fetch(`/admin-api${url}`, { method: 'GET', headers })
      return { ok: response.ok, status: response.status, payload: await response.json() }
    },
    { url, readWsCacheSource: readWsCacheScript() }
  )
  assert.equal(result.ok, true, `GET ${url} HTTP ${result.status}`)
  assert.ok([0, 200].includes(result.payload.code), `GET ${url} business code ${result.payload.code}: ${result.payload.msg || ''}`)
  return result.payload.data
}

function normalizeText(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

function resolveTaxonomyByPath(rows, segments) {
  let parentId = 0
  let current = undefined
  for (const segment of segments) {
    const matches = rows.filter(
      (row) =>
        row &&
        row.active !== false &&
        normalizeText(row.name) === segment &&
        Number(row.parentId || 0) === parentId
    )
    if (matches.length !== 1) {
      return { blocked: true, reason: `taxonomy segment not unique: ${segment}`, matched: matches.length }
    }
    current = matches[0]
    parentId = Number(current.id)
  }
  return { id: Number(current.id), path: segments }
}

async function preflightTargetData(page) {
  const projectPage = await apiGet(
    page,
    `/dcc/project-codes/page?pageNo=1&pageSize=50&status=ENABLE&keyword=${encodeURIComponent(PROJECT.keyword)}`
  )
  const projects = projectPage?.list || []
  const project = projects.find(
    (item) =>
      normalizeText(item.projectCode) === PROJECT.code &&
      normalizeText(item.projectName).includes(PROJECT.name) &&
      normalizeText(item.docControlNo) === PROJECT.docControlNo
  )
  if (!project) {
    return {
      blocked: true,
      reason: 'target DCC project is missing in local data',
      projectMatches: projects.map((item) => ({ id: item.id, code: item.projectCode, name: item.projectName, docControlNo: item.docControlNo })).slice(0, 10)
    }
  }

  const taxonomyRows = await apiGet(page, '/dcc/file-type-taxonomies/upload-options')
  const taxonomy = resolveTaxonomyByPath(taxonomyRows || [], TAXONOMY_PATH)
  if (taxonomy.blocked) {
    return { blocked: true, reason: taxonomy.reason, taxonomyPath: TAXONOMY_PATH, matched: taxonomy.matched }
  }

  const nameOptions = await apiGet(
    page,
    `/dcc/controlled-files/upload-name-options?dccProjectCodeId=${encodeURIComponent(project.id)}&fileTypeTaxonomyId=${encodeURIComponent(taxonomy.id)}`
  )
  const targetNameOption = (nameOptions || []).find(
    (item) =>
      normalizeText(item.fileName) === TARGET_HISTORY_FILE_NAME &&
      normalizeText(item.currentVersionNo).toUpperCase() === TARGET_HISTORY_VERSION
  )
  if (!targetNameOption) {
    return {
      blocked: true,
      reason: 'target history file option is missing in local data',
      project: { id: project.id, code: project.projectCode, name: project.projectName, docControlNo: project.docControlNo },
      taxonomy,
      optionCount: (nameOptions || []).length,
      optionSamples: (nameOptions || [])
        .map((item) => ({ fileName: item.fileName, currentVersionNo: item.currentVersionNo, fileNumber: item.fileNumber }))
        .slice(0, 10)
    }
  }

  const candidates = await apiGet(
    page,
    `/dcc/controlled-files/upload-revision-candidates?dccProjectCodeId=${encodeURIComponent(project.id)}` +
      `&fileTypeTaxonomyId=${encodeURIComponent(taxonomy.id)}&keyword=${encodeURIComponent(TARGET_HISTORY_FILE_NAME)}` +
      '&pageNo=1&pageSize=20'
  )
  const targetCandidate = (candidates?.list || []).find((item) => normalizeText(item.fileName || item.title) === TARGET_HISTORY_FILE_NAME)
  if (!targetCandidate) {
    return {
      blocked: true,
      reason: 'target revision candidate is missing in local data',
      project: { id: project.id, code: project.projectCode, name: project.projectName, docControlNo: project.docControlNo },
      taxonomy,
      targetNameOption,
      candidateTotal: candidates?.total || 0
    }
  }

  return {
    blocked: false,
    project: { id: project.id, code: project.projectCode, name: project.projectName, docControlNo: project.docControlNo },
    taxonomy,
    targetNameOption,
    targetCandidate: {
      id: targetCandidate.id,
      fileName: targetCandidate.fileName || targetCandidate.title,
      fileNumber: targetCandidate.fileNumber,
      versionNo: targetCandidate.versionNo,
      status: targetCandidate.status
    }
  }
}

async function selectProject(page) {
  const item = formItem(page, 'DCC项目')
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-select').first().click()
  await item.locator('input').first().fill(PROJECT.keyword)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: PROJECT.name })
    .filter({ hasText: PROJECT.code })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function selectCascaderPath(page, label, segments) {
  const item = formItem(page, label)
  await item.waitFor({ state: 'visible', timeout: 30000 })
  await item.locator('.el-cascader').first().click()
  for (const [index, segment] of segments.entries()) {
    const node = page.locator('.el-cascader-node:visible').filter({ hasText: segment }).first()
    await node.waitFor({ state: 'visible', timeout: 30000 })
    if (index === segments.length - 1) {
      const selector = node.locator('.el-radio__input, .el-checkbox__input').first()
      if ((await selector.count()) > 0) {
        await selector.click({ force: true })
      } else {
        await node.click()
      }
    } else {
      await node.click()
    }
    await page.waitForTimeout(250)
  }
  await page.keyboard.press('Escape').catch(() => undefined)
}

async function selectHistoryFile(page) {
  const item = formItem(page, '文件名称')
  await item.waitFor({ state: 'visible', timeout: 30000 })
  const input = item.locator('input').first()
  const nameOptionsResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/upload-name-options') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => undefined)
  await input.click()
  await nameOptionsResponsePromise
  await input.fill(TARGET_HISTORY_FILE_NAME)
  const option = page
    .locator('.el-autocomplete-suggestion:visible li')
    .filter({ hasText: TARGET_HISTORY_FILE_NAME })
    .filter({ hasText: `当前版本：${TARGET_HISTORY_VERSION}` })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const currentVersionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/dcc/controlled-files/current-version') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await option.click()
  const currentVersionResponse = await currentVersionResponsePromise
  const currentVersionPayload = await currentVersionResponse.json()
  assert.equal(currentVersionResponse.ok(), true, `current-version HTTP ${currentVersionResponse.status()}`)
  assert.ok(
    [0, 200].includes(currentVersionPayload.code),
    `current-version business code ${currentVersionPayload.code}: ${currentVersionPayload.msg || ''}`
  )
  return currentVersionPayload.data
}

async function runUiAssertions(page, currentVersion) {
  const panel = page.locator('[data-testid="dcc-upload-current-version-panel"]').first()
  await panel.waitFor({ state: 'visible', timeout: 30000 })
  await page.waitForTimeout(500)
  const panelText = normalizeText(await panel.innerText())
  const preflight = page.locator('[data-testid="dcc-upload-preflight-panel"]').first()
  const preflightText = normalizeText(await preflight.innerText())
  const fileVersionCardText = normalizeText(
    await preflight.locator('.upload-preflight-card').filter({ hasText: '文件编号/版本' }).first().innerText()
  )
  const versionNo = await formItem(page, '版本号').locator('input').first().inputValue()

  assert.ok(panelText.includes(TARGET_HISTORY_FILE_NAME) || panelText.includes(currentVersion?.fileName || ''), 'current-version panel must identify selected file')
  assert.ok(!panelText.includes('将创建新的 master 主档'), 'revision upload must not show new master creation in current-version panel')
  assert.ok(!preflightText.includes('将创建新的 master 主档'), 'revision upload must not show new master creation in preflight')
  assert.ok(versionNo.toUpperCase() === 'V2.0', `selected V1.0 history file must auto-generate V2.0, got ${versionNo}`)
  assert.ok(panelText.includes('文件编号：'), 'current-version panel must show matched file number or blocking state')

  const hasConflictOrRevisionBlock =
    panelText.includes('版本链冲突') ||
    panelText.includes('不会创建新的 master 主档') ||
    fileVersionCardText.includes('版本链冲突') ||
    fileVersionCardText.includes('不会创建新的 master 主档')
  if (hasConflictOrRevisionBlock) {
    assert.ok(fileVersionCardText.includes('需处理'), 'conflict/blocking state must mark 文件编号/版本 as 需处理')
    assert.ok(!fileVersionCardText.includes('可提交'), 'conflict/blocking state must not mark 文件编号/版本 as 可提交')
  }

  return {
    panelText,
    preflightText,
    fileVersionCardText,
    versionNo
  }
}

async function main() {
  assertPrerequisites()
  const launchOptions = {
    headless: true,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const evidence = {
    status: 'RUNNING',
    baseUrl: BASE_URL,
    tenant: TENANT,
    username: USERNAME,
    targetPath: TARGET_PATH,
    target: {
      project: PROJECT,
      taxonomyPath: TAXONOMY_PATH,
      historyFileName: TARGET_HISTORY_FILE_NAME,
      historyVersion: TARGET_HISTORY_VERSION
    },
    pageErrors: [],
    consoleErrors: [],
    requestFailures: [],
    dccWriteRequests: [],
    targetResponses: []
  }

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 }, locale: 'zh-CN' })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    page.on('pageerror', (error) => evidence.pageErrors.push(error.message))
    page.on('console', (message) => {
      if (message.type() === 'error') {
        evidence.consoleErrors.push(message.text())
      }
    })
    page.on('requestfailed', (request) => {
      evidence.requestFailures.push({
        method: request.method(),
        url: redactUrl(request.url()),
        failure: request.failure()?.errorText || ''
      })
    })
    page.on('request', (request) => {
      if (request.url().includes('/admin-api/dcc/') && !['GET', 'HEAD', 'OPTIONS'].includes(request.method())) {
        evidence.dccWriteRequests.push({ method: request.method(), url: redactUrl(request.url()) })
      }
    })
    page.on('response', async (response) => {
      const url = response.url()
      if (
        url.includes('/admin-api/dcc/project-codes/page') ||
        url.includes('/admin-api/dcc/file-type-taxonomies/upload-options') ||
        url.includes('/admin-api/dcc/controlled-files/upload-name-options') ||
        url.includes('/admin-api/dcc/controlled-files/upload-revision-candidates') ||
        url.includes('/admin-api/dcc/controlled-files/current-version')
      ) {
        let businessCode = undefined
        try {
          const payload = await response.clone().json()
          businessCode = payload?.code
        } catch {
          businessCode = undefined
        }
        evidence.targetResponses.push({
          method: response.request().method(),
          url: redactUrl(url),
          httpStatus: response.status(),
          businessCode
        })
      }
    })

    await login(page)
    await page.goto(`${BASE_URL}${TARGET_PATH}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
    await dismissKnownOverlay(page, evidence)
    await page.getByText('受控文件提交', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })

    const dataPreflight = await preflightTargetData(page)
    evidence.dataPreflight = dataPreflight
    if (dataPreflight.blocked) {
      evidence.status = 'BLOCKED'
      evidence.blockedReason = dataPreflight.reason
      await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true }).catch(() => undefined)
      evidence.screenshotPath = SCREENSHOT_PATH
      writeResult(evidence)
      console.log(`BLOCKED: ${dataPreflight.reason}`)
      process.exitCode = 2
      return
    }

    await selectProject(page)
    await selectCascaderPath(page, '文件分类', TAXONOMY_PATH)
    await page.getByText(TAXONOMY_PATH.join(' / '), { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const currentVersion = await selectHistoryFile(page)
    evidence.currentVersion = currentVersion
    evidence.ui = await runUiAssertions(page, currentVersion)

    assert.deepEqual(evidence.dccWriteRequests, [], 'readonly E2E must not send DCC write requests')
    await page.screenshot({ path: SCREENSHOT_PATH, fullPage: true })
    evidence.screenshotPath = SCREENSHOT_PATH
    evidence.status = 'PASS'
    writeResult(evidence)
    console.log(`PASS: DCC upload history revision readonly E2E file=${TARGET_HISTORY_FILE_NAME}`)
  } catch (error) {
    evidence.status = 'FAIL'
    evidence.error = error && error.stack ? error.stack : String(error)
    writeResult(evidence)
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exitCode = 1
})
