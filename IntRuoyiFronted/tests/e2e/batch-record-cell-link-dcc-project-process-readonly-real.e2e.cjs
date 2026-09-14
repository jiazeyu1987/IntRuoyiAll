const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

function readFrontendDefaultLogin() {
  const envPath = path.resolve(__dirname, '../..', '.env')
  const parsed = {}
  if (!fs.existsSync(envPath)) return parsed
  for (const rawLine of fs.readFileSync(envPath, 'utf8').split(/\r?\n/)) {
    const trimmed = rawLine.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const equalsIndex = trimmed.indexOf('=')
    if (equalsIndex < 0) continue
    const key = trimmed.slice(0, equalsIndex).trim()
    let value = trimmed.slice(equalsIndex + 1).trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    parsed[key] = value
  }
  return {
    tenant: parsed.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: parsed.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: parsed.VITE_APP_DEFAULT_LOGIN_PASSWORD || ''
  }
}

const defaultLogin = process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_USE_DEFAULT_LOGIN === '1'
  ? readFrontendDefaultLogin()
  : {}

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_BASE_URL || 'http://127.0.0.1:8306').replace(/\/+$/, ''),
  tenant: process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_TENANT || defaultLogin.tenant || '测试租户',
  username: process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_USERNAME || defaultLogin.username || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_PASSWORD || defaultLogin.password || '',
  headed: process.env.BATCH_RECORD_CELL_LINK_DCC_PROCESS_E2E_HEADED === '1'
}

const allowedReadonlyIdentity = (
  (config.tenant === '测试租户' && config.username === 'aoteman') ||
  (config.tenant === '芋道源码' && config.username === 'admin')
)

if (!allowedReadonlyIdentity) {
  throw new Error(
    `batch_record_cell_link_dcc_process_e2e_requires_documented_local_readonly_identity:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

if (!config.password) {
  throw new Error(
    `batch_record_cell_link_dcc_process_e2e_password_required_from_environment_or_frontend_env:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

const taskRoot = path.resolve(
  __dirname,
  '../../..',
  'doc/tasks/20260829-dcc-project-process-cell-link/e2e-artifacts'
)
fs.mkdirSync(taskRoot, { recursive: true })

const isWriteMethod = (method) => !['GET', 'HEAD', 'OPTIONS'].includes(String(method || '').toUpperCase())
const isScopedWriteRequest = (url) =>
  url.includes('/admin-api/mes/pro/batch-record-cell-link/') || url.includes('/admin-api/dcc/')

function writeResult(result) {
  fs.writeFileSync(
    path.join(taskRoot, 'batch-record-cell-link-dcc-project-process-readonly-result.json'),
    `${JSON.stringify(result, null, 2)}\n`,
    'utf8'
  )
}

async function selectTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: config.tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
    return
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.tenant)
}

async function login(page) {
  const loginUrl = new URL('/login', config.baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  await selectTenant(page, form)
  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)
  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login_http_failed:${loginResponse.status()}`)
  assert.ok([0, 200].includes(Number(loginPayload.code)), `login_payload_failed:${loginPayload.msg || loginPayload.message || loginPayload.code}`)
  await page.waitForURL((current) => !current.pathname.includes('/login'), { timeout: 60000, waitUntil: 'commit' })
}

async function waitForNoLoading(page, timeoutMs = 30000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const visibleLoadingCount = await page
      .locator('.el-loading-mask:visible')
      .count()
      .catch(() => 0)
    if (visibleLoadingCount === 0) return
    await page.waitForTimeout(250)
  }
  assert.fail('element_plus_loading_mask_timeout')
}

async function apiGet(page, apiPath, params = {}) {
  return page.evaluate(
    async ({ apiPath: innerApiPath, params: innerParams }) => {
      function parseStoredValue(raw) {
        if (!raw) return undefined
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object') {
            if (
              Object.prototype.hasOwnProperty.call(parsed, 'c') &&
              Object.prototype.hasOwnProperty.call(parsed, 'e') &&
              Object.prototype.hasOwnProperty.call(parsed, 'v')
            ) {
              return parseStoredValue(parsed.v)
            }
            if (Object.prototype.hasOwnProperty.call(parsed, 'v')) return parsed.v
            if (Object.prototype.hasOwnProperty.call(parsed, 'value')) return parsed.value
            if (Object.prototype.hasOwnProperty.call(parsed, 'data')) return parsed.data
          }
          return parsed
        } catch {
          return raw
        }
      }

      function readCacheValue(targetKey) {
        const exact = parseStoredValue(window.localStorage.getItem(targetKey))
        if (exact !== undefined && exact !== null && exact !== '') return exact
        for (let index = 0; index < window.localStorage.length; index += 1) {
          const key = window.localStorage.key(index)
          if (!key || !key.endsWith(targetKey)) continue
          const value = parseStoredValue(window.localStorage.getItem(key))
          if (value !== undefined && value !== null && value !== '') return value
        }
        return undefined
      }

      const accessToken = readCacheValue('ACCESS_TOKEN')
      const tenantId = readCacheValue('tenantId')
      if (!accessToken) {
        throw new Error('access_token_missing_after_login')
      }
      const url = new URL(`/admin-api${innerApiPath}`, window.location.origin)
      for (const [key, value] of Object.entries(innerParams || {})) {
        if (value === undefined || value === null || value === '') continue
        url.searchParams.set(key, String(value))
      }
      const headers = {
        Authorization: `Bearer ${accessToken}`,
        'Cache-Control': 'no-cache',
        Pragma: 'no-cache'
      }
      if (tenantId !== undefined && tenantId !== null && tenantId !== '') {
        headers['tenant-id'] = String(tenantId)
      }
      const response = await fetch(url.toString(), { method: 'GET', headers })
      const payload = await response.json().catch(() => null)
      return { ok: response.ok, status: response.status, payload }
    },
    { apiPath, params }
  )
}

function unwrapCommonResult(result, label) {
  assert.ok(result.ok, `${label}_http_failed:${result.status}`)
  assert.ok(result.payload, `${label}_payload_missing`)
  assert.ok([0, 200].includes(Number(result.payload.code)), `${label}_business_failed:${result.payload.msg || result.payload.message || result.payload.code}`)
  return result.payload.data
}

function normalizeText(value) {
  return String(value || '').replace(/\s+/g, ' ').trim()
}

function formatDccProjectCodeOption(projectCode) {
  return [projectCode.projectCode, projectCode.projectName, projectCode.id].filter(Boolean).join(' / ')
}

function formatRouteProcessOption(process) {
  return `${process.sort ?? '-'}. ${process.processName || '未命名工序'}`
}

function buildWorkbenchResponseSummary(response, payload) {
  const url = new URL(response.url())
  const data = payload?.data || {}
  return {
    path: `${url.pathname}${url.search}`,
    status: response.status(),
    code: payload?.code,
    message: payload?.msg || payload?.message || '',
    sourceReportId: url.searchParams.get('sourceReportId') || '',
    dccProjectCodeId: data.dccProjectCodeId,
    routeId: data.routeId,
    routeProcessId: data.routeProcessId,
    routeProcessCount: Array.isArray(data.routeProcesses) ? data.routeProcesses.length : undefined,
    sourceFieldCount: Array.isArray(data.sourceFields) ? data.sourceFields.length : undefined
  }
}

async function findCandidate(page) {
  const diagnostics = []
  for (let pageNo = 1; pageNo <= 5; pageNo += 1) {
    const projectPage = unwrapCommonResult(
      await apiGet(page, '/dcc/project-codes/page', {
        pageNo,
        pageSize: 20,
        status: 'ENABLE',
        routeConfigured: true,
        mainBatchRecordConfigured: true
      }),
      `project_code_page_${pageNo}`
    )
    const projectCodes = projectPage?.list || []
    for (const projectCode of projectCodes) {
      const projectCodeId = Number(projectCode.id)
      if (!Number.isFinite(projectCodeId)) continue
      const contextResult = await apiGet(page, '/mes/pro/batch-record-cell-link/workbench-context', {
        sourceReportId: 'PROCESS_POOL_REPORT',
        dccProjectCodeId: projectCodeId
      })
      if (!contextResult.ok || ![0, 200].includes(Number(contextResult.payload?.code))) {
        diagnostics.push({
          projectCodeId,
          code: contextResult.payload?.code,
          message: contextResult.payload?.msg || contextResult.payload?.message || `HTTP ${contextResult.status}`
        })
        continue
      }
      const baseContext = contextResult.payload.data || {}
      for (const process of baseContext.routeProcesses || []) {
        const processId = Number(process.id)
        if (!Number.isFinite(processId)) continue
        const processContextResult = await apiGet(page, '/mes/pro/batch-record-cell-link/workbench-context', {
          sourceReportId: 'PROCESS_POOL_REPORT',
          dccProjectCodeId: projectCodeId,
          routeProcessId: processId
        })
        if (!processContextResult.ok || ![0, 200].includes(Number(processContextResult.payload?.code))) {
          diagnostics.push({
            projectCodeId,
            processId,
            code: processContextResult.payload?.code,
            message: processContextResult.payload?.msg || processContextResult.payload?.message || `HTTP ${processContextResult.status}`
          })
          continue
        }
        const processContext = processContextResult.payload.data || {}
        const sourceFields = (processContext.sourceFields || []).filter(
          (field) =>
            field.sourceType === 'PROCESS_POOL_REPORT' &&
            (field.routeProcessId === undefined ||
              field.routeProcessId === null ||
              Number(field.routeProcessId) === processId)
        )
        if (!sourceFields.length) {
          diagnostics.push({ projectCodeId, processId, message: 'no_process_pool_source_fields' })
          continue
        }
        const forms = processContext.forms || []
        const targetReport =
          forms.find((form) => String(form.reportId) === String(process.batchRecordReportId)) ||
          forms.find((form) => Number(form.routeProcessId) === processId) ||
          forms[0]
        if (!targetReport?.reportId) {
          diagnostics.push({ projectCodeId, processId, message: 'no_target_report' })
          continue
        }
        return {
          projectCode,
          process,
          context: processContext,
          targetReport,
          sourceFields
        }
      }
    }
  }
  const summary = diagnostics.slice(0, 8)
  throw new Error(`no_real_dcc_project_process_candidate:${JSON.stringify(summary)}`)
}

async function chooseVisibleOption(page, matcher, timeoutMs = 30000) {
  const exactText = normalizeText(typeof matcher === 'string' ? matcher : matcher.exactText)
  const requiredParts = (typeof matcher === 'string' ? [matcher] : matcher.requiredParts || [])
    .filter((part) => part !== undefined && part !== null && String(part).trim() !== '')
    .map(normalizeText)
  const deadline = Date.now() + timeoutMs
  let lastVisibleText = ''
  while (Date.now() < deadline) {
    const options = page.locator('.el-select-dropdown__item:visible, [role="option"]:visible')
    const count = await options.count().catch(() => 0)
    for (let index = 0; index < count; index += 1) {
      const option = options.nth(index)
      const text = (await option.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
      if (text) lastVisibleText = text
      const normalized = normalizeText(text)
      const exactMatched = exactText && normalized === exactText
      const partsMatched = requiredParts.length > 0 && requiredParts.every((part) => normalized.includes(part))
      if (exactMatched || partsMatched) {
        await option.click()
        return text
      }
    }
    await page.waitForTimeout(250)
  }
  throw new Error(`select_option_not_found:${JSON.stringify({ matcher: { exactText, requiredParts }, lastVisibleText })}`)
}

async function openSelectAndSearch(selectLocator, searchText = '') {
  await selectLocator.waitFor({ state: 'visible', timeout: 60000 })
  await selectLocator.click()
  const input = selectLocator.locator('input[role="combobox"], input.el-input__inner, input').first()
  if (searchText) {
    await input.fill(searchText)
  }
}

async function waitForElementPlusSelectEnabled(selectLocator, label, timeoutMs = 60000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const enabled = await selectLocator.evaluate((element) => {
      const disabledByClass = element.classList.contains('is-disabled')
      const disabledByAria = element.getAttribute('aria-disabled') === 'true'
      const input = element.querySelector('input')
      return !disabledByClass && !disabledByAria && !input?.disabled
    }).catch(() => false)
    if (enabled) return
    await selectLocator.page().waitForTimeout(250)
  }
  throw new Error(`${label}_select_not_enabled`)
}

async function main() {
  const browser = await chromium.launch({ headless: !config.headed, args: ['--disable-dev-shm-usage'] })
  const writeRequests = []
  const workbenchResponses = []
  let candidate

  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    candidate = await findCandidate(page)

    page.on('response', async (response) => {
      if (!response.url().includes('/mes/pro/batch-record-cell-link/workbench-context')) return
      const payload = await response.json().catch(() => null)
      workbenchResponses.push(buildWorkbenchResponseSummary(response, payload))
    })

    page.on('request', (request) => {
      if (isWriteMethod(request.method()) && isScopedWriteRequest(request.url())) {
        writeRequests.push({ method: request.method(), url: request.url() })
      }
    })

    const targetUrl = new URL('/mes/pro/batch-record-cell-link', config.baseUrl)
    targetUrl.searchParams.set('sourceReportId', candidate.targetReport.reportId)
    if (candidate.context.batchRecordDefinitionId) {
      targetUrl.searchParams.set('definitionId', String(candidate.context.batchRecordDefinitionId))
    }
    if (candidate.context.batchRecordVersionId) {
      targetUrl.searchParams.set('versionId', String(candidate.context.batchRecordVersionId))
    }
    targetUrl.searchParams.set('targetReportId', candidate.targetReport.reportId)

    await page.goto(targetUrl.toString(), { waitUntil: 'domcontentloaded' })
    await page.locator('.batch-record-cell-link').first().waitFor({ state: 'visible', timeout: 60000 })
    await waitForNoLoading(page)

    const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
    await openSelectAndSearch(sourceSelect)
    const sourceOptionText = await chooseVisibleOption(page, '报工数据')
    assert.ok(sourceOptionText.includes('报工数据'), 'process_pool_report_option_visible_and_selected')

    await page.locator('[data-process-pool-context-selector]').first().waitFor({ state: 'visible', timeout: 60000 })
    const dccSelect = page.locator('[data-process-pool-dcc-project-select]').first()
    const dccOptionLabel = formatDccProjectCodeOption(candidate.projectCode)
    const projectSearchText = String(candidate.projectCode.projectCode || candidate.projectCode.projectName || '')
    assert.ok(projectSearchText, 'dcc_project_code_or_name_missing')
    await openSelectAndSearch(dccSelect, projectSearchText)
    const dccOptionText = await chooseVisibleOption(page, { exactText: dccOptionLabel }, 60000)
    await waitForNoLoading(page)

    const processSelect = page.locator('[data-process-pool-route-process-select]').first()
    await waitForElementPlusSelectEnabled(processSelect, 'route_process')
    const processOptionLabel = formatRouteProcessOption(candidate.process)
    const processSearchText = String(candidate.process.processName || candidate.process.processCode || candidate.process.sort || '')
    assert.ok(processSearchText, 'route_process_name_or_code_missing')
    await openSelectAndSearch(processSelect, processSearchText)
    const processOptionText = await chooseVisibleOption(page, { exactText: processOptionLabel }, 60000)
    await waitForNoLoading(page)

    const sourcePane = page.locator('.batch-record-cell-link__pane.is-source').first()
    await sourcePane.waitFor({ state: 'visible', timeout: 60000 })
    const fieldCountAttribute = await sourcePane.getAttribute('data-process-pool-report-field-count')
    const fieldCount = Number(fieldCountAttribute)
    assert.ok(fieldCount > 0, `source_field_count_invalid:${fieldCountAttribute}`)
    await page.getByText('一线生产字段', { exact: false }).first().waitFor({ state: 'visible', timeout: 30000 })
    const sourceCells = sourcePane.locator('.batch-record-cell-link-sheet__cell')
    assert.ok((await sourceCells.count()) > 0, 'process_pool_source_cells_not_rendered')
    assert.deepEqual(writeRequests, [], 'readonly E2E must not write DCC or cell-link data')

    const screenshot = path.join(taskRoot, 'batch-record-cell-link-dcc-project-process-readonly-pass.png')
    await page.screenshot({ path: screenshot, fullPage: true })
    const result = {
      status: 'PASS',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      targetUrl: targetUrl.toString(),
      projectCodeId: candidate.projectCode.id,
      projectCode: candidate.projectCode.projectCode || '',
      projectName: candidate.projectCode.projectName || '',
      routeId: candidate.context.routeId,
      routeProcessId: candidate.process.id,
      routeProcessName: candidate.process.processName || '',
      targetReportId: candidate.targetReport.reportId,
      targetReportName: candidate.targetReport.reportName || '',
      sourceFieldCount: fieldCount,
      sampledSourceFieldNames: candidate.sourceFields.slice(0, 5).map((field) => field.fieldName || field.fieldCode),
      dccOptionText,
      processOptionText,
      workbenchResponses,
      writeRequests,
      screenshot
    }
    writeResult(result)
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    const screenshot = path.join(taskRoot, 'batch-record-cell-link-dcc-project-process-readonly-fail.png')
    try {
      const pages = browser.contexts().flatMap((browserContext) => browserContext.pages())
      const page = pages[0]
      if (page) await page.screenshot({ path: screenshot, fullPage: true })
    } catch {
      // Ignore screenshot failure so the real E2E error remains primary.
    }
    writeResult({
      status: 'FAIL',
      baseUrl: config.baseUrl,
      tenant: config.tenant,
      username: config.username,
      projectCodeId: candidate?.projectCode?.id,
      routeProcessId: candidate?.process?.id,
      error: error.message,
      workbenchResponses,
      writeRequests,
      screenshot: fs.existsSync(screenshot) ? screenshot : undefined
    })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error.message)
  process.exit(1)
})
