const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

function readFrontendDefaultLogin() {
  const envPath = path.resolve(__dirname, '../../..', '.env')
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

const defaultLogin = process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_USE_DEFAULT_LOGIN === '1'
  ? readFrontendDefaultLogin()
  : {}

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_BASE_URL || 'http://127.0.0.1:8308').replace(/\/+$/, ''),
  tenant: process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_TENANT || defaultLogin.tenant || '测试租户',
  username: process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_USERNAME || defaultLogin.username || 'aoteman',
  password: process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_PASSWORD || defaultLogin.password || '',
  targetProjectCodeId: Number(process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_PROJECT_CODE_ID || 0) || null,
  headed: process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_HEADED === '1'
}

const allowedReadonlyIdentity = (
  (config.tenant === '测试租户' && config.username === 'aoteman') ||
  (config.tenant === '芋道源码' && config.username === 'admin')
)

if (!allowedReadonlyIdentity) {
  throw new Error(
    `process_pool_real_device_e2e_requires_documented_local_readonly_identity:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

if (!config.password) {
  throw new Error(
    `process_pool_real_device_e2e_password_required_from_environment_or_frontend_env:${JSON.stringify({
      tenant: config.tenant,
      username: config.username
    })}`
  )
}

const taskRoot = path.resolve(
  process.env.BATCH_RECORD_CELL_LINK_REAL_DEVICE_TASK_ROOT ||
    path.resolve(__dirname, '../../../../doc/tasks/20260830-dcc-process-device-type-parameter-catalog/e2e-artifacts')
)
fs.mkdirSync(taskRoot, { recursive: true })

const isWriteMethod = (method) => !['GET', 'HEAD', 'OPTIONS'].includes(String(method || '').toUpperCase())
const isScopedWriteRequest = (url) =>
  url.includes('/admin-api/mes/pro/batch-record-cell-link/') || url.includes('/admin-api/dcc/')

function writeResult(result) {
  fs.writeFileSync(
    path.join(taskRoot, 'process-pool-real-device-readonly-result.json'),
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

async function waitForNoLoading(page, timeoutMs = 90000) {
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

function isWorkbenchContextResponse(response) {
  return (
    response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
    response.request().method() === 'GET'
  )
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
    routeProcessCount: Array.isArray(data.routeProcesses) ? data.routeProcesses.length : undefined,
    sourceFieldCount: Array.isArray(data.sourceFields) ? data.sourceFields.length : undefined
  }
}

function isDeviceGroupSourceField(field) {
  return (
    field &&
    field.sourceType === 'PROCESS_POOL_REPORT' &&
    typeof field.fieldCode === 'string' &&
    field.fieldCode.includes('@deviceGroup:') &&
    Boolean(field.deviceName) &&
    !String(field.fieldName || '').includes(` / `)
  )
}

function selectedProcessSourceFields(context, processId) {
  return (context.sourceFields || []).filter(
    (field) =>
      field.sourceType === 'PROCESS_POOL_REPORT' &&
      (field.routeProcessId === undefined ||
        field.routeProcessId === null ||
        Number(field.routeProcessId) === Number(processId))
  )
}

function requiredDeviceGroups(sourceFields) {
  const fields = sourceFields.filter(isDeviceGroupSourceField)
  const byDeviceName = new Map()
  for (const field of fields) {
    const key = String(field.deviceName)
    const current = byDeviceName.get(key) || {
      deviceName: field.deviceName,
      fieldNames: [],
      fieldCodes: []
    }
    current.fieldNames.push(field.fieldName)
    current.fieldCodes.push(field.fieldCode)
    byDeviceName.set(key, current)
  }
  return Array.from(byDeviceName.values()).filter((device) =>
    device.fieldCodes.some((fieldCode) => fieldCode.startsWith('selectedDevice.deviceCode@deviceGroup:')) &&
    device.fieldCodes.some((fieldCode) => fieldCode.startsWith('deviceMeteringValidity.inMeteringValidityPeriod@deviceGroup:'))
  )
}

function deviceGroupedParameterFields(sourceFields) {
  return sourceFields.filter(
    (field) =>
      isDeviceGroupSourceField(field) &&
      field.fieldCode.startsWith('deviceParameterReadings.') &&
      field.fieldCode.includes('.value@deviceGroup:')
  )
}

function unscopedParameterFields(sourceFields) {
  return sourceFields.filter(
    (field) =>
      field &&
      field.sourceType === 'PROCESS_POOL_REPORT' &&
      typeof field.fieldCode === 'string' &&
      field.fieldCode.startsWith('deviceParameterReadings.') &&
      !field.fieldCode.includes('@deviceGroup:')
  )
}

function deviceGroupedParameterMetadataFields(sourceFields) {
  return sourceFields.filter(
    (field) =>
      isDeviceGroupSourceField(field) &&
      typeof field.fieldCode === 'string' &&
      (field.fieldCode.startsWith('equipmentParameterRules.') ||
        (field.fieldCode.startsWith('deviceParameterReadings.') && !field.fieldCode.includes('.value@deviceGroup:')))
  )
}

async function findCandidate(page) {
  const diagnostics = []
  if (config.targetProjectCodeId) {
    const projectCode = unwrapCommonResult(
      await apiGet(page, `/dcc/project-codes/${config.targetProjectCodeId}`),
      'target_project_code'
    )
    const candidate = await findCandidateForProject(page, projectCode, diagnostics)
    if (candidate) return candidate
    throw new Error(`target_project_code_has_no_real_device_parameter_fields:${JSON.stringify(diagnostics)}`)
  }
  for (let pageNo = 1; pageNo <= 20; pageNo += 1) {
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
    if (!projectCodes.length) break
    for (const projectCode of projectCodes) {
      const candidate = await findCandidateForProject(page, projectCode, diagnostics)
      if (candidate) return candidate
    }
  }
  throw new Error(`no_real_device_process_pool_candidate:${JSON.stringify(diagnostics.slice(0, 20))}`)
}

async function findCandidateForProject(page, projectCode, diagnostics) {
  const projectCodeId = Number(projectCode.id)
  if (!Number.isFinite(projectCodeId)) return null
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
    return null
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
    const sourceFields = selectedProcessSourceFields(processContext, processId)
    const devices = requiredDeviceGroups(sourceFields)
    const parameterFields = deviceGroupedParameterFields(sourceFields)
    const metadataParameterFields = deviceGroupedParameterMetadataFields(sourceFields)
    const genericParameterFields = unscopedParameterFields(sourceFields)
    if (!devices.length) {
      diagnostics.push({
        projectCodeId,
        processId,
        sourceFieldCount: sourceFields.length,
        message: 'no_real_device_group_source_field'
      })
      continue
    }
    if (genericParameterFields.length) {
      diagnostics.push({
        projectCodeId,
        processId,
        sourceFieldCount: sourceFields.length,
        message: 'unscoped_device_group_parameter_fields_visible',
        fields: genericParameterFields.map((field) => field.fieldCode).slice(0, 5)
      })
      continue
    }
    if (!parameterFields.length) {
      diagnostics.push({
        projectCodeId,
        processId,
        sourceFieldCount: sourceFields.length,
        deviceCount: devices.length,
        message: 'no_device_scoped_parameter_fields'
      })
      continue
    }
    if (metadataParameterFields.length) {
      diagnostics.push({
        projectCodeId,
        processId,
        sourceFieldCount: sourceFields.length,
        message: 'device_parameter_metadata_fields_visible',
        fields: metadataParameterFields.map((field) => field.fieldCode).slice(0, 8)
      })
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
      sourceFields,
      devices,
      parameterFields
    }
  }
  return null
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
      if (!isWorkbenchContextResponse(response)) return
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
    const dccResponsePromise = page
      .waitForResponse(isWorkbenchContextResponse, { timeout: 60000 })
      .catch((error) => error)
    const dccOptionText = await chooseVisibleOption(page, { exactText: dccOptionLabel }, 60000)
    const dccResponseOrError = await dccResponsePromise
    if (dccResponseOrError instanceof Error) throw dccResponseOrError
    await waitForNoLoading(page)

    const processSelect = page.locator('[data-process-pool-route-process-select]').first()
    await waitForElementPlusSelectEnabled(processSelect, 'route_process')
    const processOptionLabel = formatRouteProcessOption(candidate.process)
    const processSearchText = String(candidate.process.processName || candidate.process.processCode || candidate.process.sort || '')
    assert.ok(processSearchText, 'route_process_name_or_code_missing')
    await openSelectAndSearch(processSelect, processSearchText)
    const processResponsePromise = page
      .waitForResponse(isWorkbenchContextResponse, { timeout: 60000 })
      .catch((error) => error)
    const processOptionText = await chooseVisibleOption(page, { exactText: processOptionLabel }, 60000)
    const processResponseOrError = await processResponsePromise
    if (processResponseOrError instanceof Error) throw processResponseOrError
    const processPayload = await processResponseOrError.json()
    assert.ok([0, 200].includes(Number(processPayload?.code)), `process_context_failed:${processPayload?.msg || processPayload?.message || processPayload?.code}`)
    await waitForNoLoading(page)

    const selectedSourceFields = selectedProcessSourceFields(processPayload.data || {}, candidate.process.id)
    const selectedDevices = requiredDeviceGroups(selectedSourceFields)
    const selectedParameterFields = deviceGroupedParameterFields(selectedSourceFields)
    const selectedParameterMetadataFields = deviceGroupedParameterMetadataFields(selectedSourceFields)
    const selectedGenericParameterFields = unscopedParameterFields(selectedSourceFields)
    assert.ok(selectedDevices.length > 0, 'selected_process_has_no_real_device_group_source_fields')
    assert.deepEqual(
      selectedGenericParameterFields.map((field) => field.fieldCode),
      [],
      'selected_process_must_not_expose_unscoped_device_group_parameter_fields'
    )
    assert.ok(selectedParameterFields.length > 0, 'selected_process_has_no_device_grouped_parameter_fields')
    assert.deepEqual(
      selectedParameterMetadataFields.map((field) => field.fieldCode),
      [],
      'selected_process_must_not_expose_device_parameter_metadata_fields'
    )

    const sourcePane = page.locator('.batch-record-cell-link__pane.is-source').first()
    await sourcePane.waitFor({ state: 'visible', timeout: 60000 })
    const fieldCountAttribute = await sourcePane.getAttribute('data-process-pool-report-field-count')
    const fieldCount = Number(fieldCountAttribute)
    assert.ok(fieldCount >= selectedDevices.length * 2, `source_field_count_invalid:${fieldCountAttribute}`)
    const sourceCells = sourcePane.locator('.batch-record-cell-link-sheet__cell')
    assert.ok((await sourceCells.count()) > 0, 'process_pool_source_cells_not_rendered')
    const panelText = normalizeText(await sourcePane.innerText())
    assert.ok(
      panelText.includes(`${processOptionText}的一线生产字段`),
      `source_panel_must_identify_selected_process:${panelText}`
    )
    assert.ok(!panelText.includes('设备编码 / 设备名称'), 'generic_device_placeholder_must_not_be_visible')
    assert.ok(!panelText.includes(' / B'), 'physical_device_code_suffix_must_not_be_visible')
    for (const device of selectedDevices) {
      const identity = `${device.deviceName}`
      assert.ok(panelText.includes(identity), `real_device_group_not_visible:${identity}`)
      assert.ok(
        selectedSourceFields.some((field) =>
          field.fieldCode.startsWith('selectedDevice.deviceCode@deviceGroup:') &&
          field.fieldName === `选用设备编码（${identity}）`
        ),
        `selected_device_code_field_missing_for_real_device_group:${identity}`
      )
      assert.ok(
        selectedSourceFields.some((field) =>
          field.fieldCode.startsWith('deviceMeteringValidity.inMeteringValidityPeriod@deviceGroup:') &&
          field.fieldName === `选用设备计量有效期内（${identity}）`
        ),
        `metering_validity_field_missing_for_real_device_group:${identity}`
      )
    }
    for (const field of selectedParameterFields.slice(0, 8)) {
      assert.ok(field.fieldCode.includes('@deviceGroup:'), `device_parameter_group_scope_mismatch:${field.fieldCode}`)
      assert.ok(field.fieldCode.includes('.value@deviceGroup:'), `device_parameter_must_be_actual_value_field:${field.fieldCode}`)
      assert.ok(!String(field.fieldName || '').includes(' / '), `device_parameter_physical_identity_leaked:${field.fieldName}`)
      assert.ok(panelText.includes(field.fieldName), `device_parameter_field_not_visible:${field.fieldName}`)
    }
    for (const hiddenText of ['单位', '下限', '上限', '状态', '参考标准', '默认文本', '默认值']) {
      assert.ok(!panelText.includes(hiddenText), `device_parameter_metadata_text_must_not_be_visible:${hiddenText}`)
    }
    const sourceCellForLink = sourcePane
      .locator('.batch-record-cell-link-sheet__cell.is-source-selectable')
      .filter({ hasText: selectedParameterFields[0].fieldName })
      .first()
    await sourceCellForLink.waitFor({ state: 'visible', timeout: 30000 })
    await sourceCellForLink.click()
    const aggregationSelectText = normalizeText(
      await page.locator('.batch-record-cell-link__aggregation-select').first().innerText()
    )
    assert.ok(
      aggregationSelectText.includes('最后一笔'),
      `process_pool_parameter_must_default_to_last_aggregation:${aggregationSelectText}`
    )
    const targetPane = page.locator('.batch-record-cell-link__pane.is-target').first()
    const targetCandidateCells = targetPane.locator('.batch-record-cell-link-sheet__cell.is-target-selectable')
    assert.ok((await targetCandidateCells.count()) > 0, 'target_linkable_cells_missing')
    await targetCandidateCells.first().click()
    assert.ok(
      await page.locator('.batch-record-cell-link__create-button').first().isEnabled(),
      'create_button_must_be_enabled_after_process_pool_source_and_target_selection'
    )
    assert.deepEqual(writeRequests, [], 'readonly E2E must not write DCC or cell-link data')

    const screenshot = path.join(taskRoot, 'process-pool-real-device-readonly-pass.png')
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
      dccOptionText,
      processOptionText,
      sourceFieldCount: fieldCount,
      devices: selectedDevices,
      sampledParameterFields: selectedParameterFields.slice(0, 8).map((field) => ({
        fieldCode: field.fieldCode,
        fieldName: field.fieldName,
        routeProcessId: field.routeProcessId,
        deviceId: field.deviceId,
        deviceCode: field.deviceCode,
        deviceName: field.deviceName
      })),
      aggregationSelectText,
      createButtonEnabledAfterSelection: true,
      workbenchResponses,
      writeRequests,
      screenshot
    }
    writeResult(result)
    console.log(JSON.stringify(result, null, 2))
  } catch (error) {
    const screenshot = path.join(taskRoot, 'process-pool-real-device-readonly-fail.png')
    try {
      const pages = browser.contexts().flatMap((browserContext) => browserContext.pages())
      const page = pages[0]
      if (page) await page.screenshot({ path: screenshot, fullPage: true })
    } catch {
      // Keep the original E2E error as the primary failure.
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
