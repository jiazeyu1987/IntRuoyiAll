import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const frontendRoot = path.resolve(__dirname, '..', '..', '..')
const repoRoot = path.resolve(frontendRoot, '..')
const frontendRequire = createRequire(path.join(frontendRoot, 'package.json'))
const { chromium } = frontendRequire('playwright')

const config = {
  baseUrl: (process.env.BATCH_RECORD_CELL_LINK_PQC_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  routeId: Number(process.env.BATCH_RECORD_CELL_LINK_PQC_ROUTE_ID || 922119),
  processNames: (process.env.BATCH_RECORD_CELL_LINK_PQC_PROCESS_NAMES || '粗洗工序,精洗工序')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean),
  taskDir:
    process.env.BATCH_RECORD_CELL_LINK_PQC_TASK_DIR ||
    path.join(repoRoot, 'doc', 'tasks', '20260820-pqc-shared-process-inspection-mapping', 'e2e-artifacts'),
  timeout: Number(process.env.BATCH_RECORD_CELL_LINK_PQC_TIMEOUT || 90000),
  headed: process.env.BATCH_RECORD_CELL_LINK_PQC_HEADED === '1'
}

function readLoginDefaults() {
  const envPath = path.join(frontendRoot, '.env')
  assert.ok(fs.existsSync(envPath), `frontend .env missing: ${envPath}`)
  const entries = Object.fromEntries(
    fs
      .readFileSync(envPath, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#') && line.includes('='))
      .map((line) => {
        const [key, ...rest] = line.split('=')
        return [key.trim(), rest.join('=').trim().replace(/^['"]|['"]$/g, '')]
      })
  )
  const credentials = {
    tenant: process.env.BATCH_RECORD_CELL_LINK_PQC_TENANT || entries.VITE_APP_DEFAULT_LOGIN_TENANT,
    username: process.env.BATCH_RECORD_CELL_LINK_PQC_USERNAME || entries.VITE_APP_DEFAULT_LOGIN_USERNAME,
    password: process.env.BATCH_RECORD_CELL_LINK_PQC_PASSWORD || entries.VITE_APP_DEFAULT_LOGIN_PASSWORD
  }
  assert.ok(credentials.tenant && credentials.username && credentials.password, 'default login values are incomplete')
  return credentials
}

function isSuccessPayload(payload) {
  return payload && (payload.code === 0 || payload.code === 200)
}

async function settle(page, timeout = 15000) {
  await page.waitForLoadState('networkidle', { timeout }).catch(() => null)
  await page.waitForTimeout(500)
}

async function selectTenant(page, form, tenant) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(tenant)
    const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await option.waitFor({ state: 'visible', timeout: 30000 })
    await option.click()
    return
  }
  await form.locator('input[placeholder="请输入租户名称"]').first().fill(tenant)
}

async function login(page, credentials) {
  const targetPath = '/mes/pro/batch-record-form-list'
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await settle(page)
  if (!page.url().includes('/login')) return null

  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: config.timeout })
  await selectTenant(page, form, credentials.tenant)
  await form
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"]):visible')
    .first()
    .fill(credentials.username)
  await form.locator('input[type="password"]:visible').first().fill(credentials.password)

  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: config.timeout }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const response = await responsePromise
  const payload = await response.json().catch(() => null)
  assert.ok(response.ok() && isSuccessPayload(payload), `login failed: ${payload?.msg || response.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: config.timeout })
  return payload.data || null
}

function resolveAccessToken(loginData) {
  return loginData?.accessToken || loginData?.access_token || loginData?.token || null
}

async function apiGet(page, pathAndQuery, token) {
  const response = await page.evaluate(
    async ({ pathAndQuery: innerPathAndQuery, token: innerToken }) => {
      const headers = innerToken ? { Authorization: `Bearer ${innerToken}` } : {}
      const result = await fetch(`/admin-api${innerPathAndQuery}`, { headers })
      const json = await result.json().catch(() => null)
      return {
        ok: result.ok,
        status: result.status,
        payload: json
      }
    },
    { pathAndQuery, token }
  )
  assert.ok(response.ok && isSuccessPayload(response.payload), `GET ${pathAndQuery} failed: ${response.payload?.msg || response.status}`)
  return response.payload.data
}

function collectFlowProcesses(flowConfig) {
  const direct = Array.isArray(flowConfig) ? flowConfig : []
  const nested = [
    ...(Array.isArray(flowConfig?.processes) ? flowConfig.processes : []),
    ...(Array.isArray(flowConfig?.routeProcesses) ? flowConfig.routeProcesses : []),
    ...(Array.isArray(flowConfig?.nodes) ? flowConfig.nodes : [])
  ]
  return [...direct, ...nested]
    .map((item) => ({
      routeProcessId: Number(item.routeProcessId || item.id),
      processName: String(item.processName || item.name || item.label || '').trim(),
      batchRecords: Array.isArray(item.batchRecords)
        ? item.batchRecords
        : Array.isArray(item.batchRecordReports)
          ? item.batchRecordReports
          : [],
      formBindings: Array.isArray(item.formBindings) ? item.formBindings : []
    }))
    .filter((item) => Number.isFinite(item.routeProcessId) && item.processName)
}

function pickTargetProcesses(processes) {
  const picked = config.processNames.map((name) => {
    const item = processes.find((candidate) => candidate.processName === name || candidate.processName.includes(name))
    assert.ok(item, `route process missing: ${name}; visible=${processes.map((candidate) => candidate.processName).join(', ')}`)
    const processInspectionReport = item.batchRecords.find((report) => report.formSlotType === 'PROCESS_INSPECTION')
    const processInspectionBinding = item.formBindings.find((binding) => binding.formSlotType === 'PROCESS_INSPECTION')
    if (processInspectionReport) {
      assert.ok(
        processInspectionReport.batchRecordDefinitionId && processInspectionReport.batchRecordVersionId,
        `process inspection report misses definition/version: ${JSON.stringify(processInspectionReport)}`
      )
      return {
        ...item,
        targetKind: 'BATCH_RECORD_REPORT',
        targetReportId: processInspectionReport.batchRecordReportId,
        definitionId: Number(processInspectionReport.batchRecordDefinitionId),
        versionId: Number(processInspectionReport.batchRecordVersionId)
      }
    }
    assert.ok(
      processInspectionBinding,
      `process inspection binding missing for ${item.processName}/${item.routeProcessId}`
    )
    assert.ok(
      processInspectionBinding.formTemplateId && processInspectionBinding.lastPublishedTemplateVersionNo,
      `process inspection binding misses template/version: ${JSON.stringify(processInspectionBinding)}`
    )
    return {
      ...item,
      targetKind: 'FORM_TEMPLATE_VERSION',
      templateId: Number(processInspectionBinding.formTemplateId),
      versionNo: String(processInspectionBinding.lastPublishedTemplateVersionNo)
    }
  })
  assert.ok(picked.length >= 1, 'at least one target process is required')
  return picked
}

async function openPqcWorkbench(page, processInfo) {
  const contextPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/batch-record-cell-link/workbench-context') &&
      response.request().method() === 'GET',
    { timeout: config.timeout }
  )
  const query = new URLSearchParams({
    routeId: String(config.routeId),
    routeProcessId: String(processInfo.routeProcessId),
    sourceReportId: 'PQC_AGGREGATE_DETAIL'
  })
  if (processInfo.targetKind === 'FORM_TEMPLATE_VERSION') {
    query.set('templateId', String(processInfo.templateId))
    query.set('versionNo', String(processInfo.versionNo))
  } else {
    query.set('definitionId', String(processInfo.definitionId))
    query.set('versionId', String(processInfo.versionId))
    query.set('targetReportId', String(processInfo.targetReportId))
  }
  await page.goto(`${config.baseUrl}/mes/pro/batch-record-cell-link?${query.toString()}`, {
    waitUntil: 'domcontentloaded',
    timeout: config.timeout
  })
  await page.getByText('批记录单元格链接').first().waitFor({ state: 'visible', timeout: config.timeout })
  const contextPayload = await (await contextPromise).json()
  assert.ok(isSuccessPayload(contextPayload), `workbench context failed: ${contextPayload?.msg || contextPayload?.code}`)
  await settle(page, 30000)
  return contextPayload.data
}

async function verifyPqcSource(page, context, processInfo) {
  const sourceFields = (context.sourceFields || []).filter((field) => field.sourceType === 'PQC_AGGREGATE_DETAIL')
  const currentProcessFields = sourceFields.filter((field) => Number(field.routeProcessId) === Number(processInfo.routeProcessId))
  assert.ok(currentProcessFields.length > 0, `PQC fields missing for routeProcessId=${processInfo.routeProcessId}`)
  assert.ok(
    sourceFields.every(
      (field) => field.routeProcessId === undefined || field.routeProcessId === null || Number(field.routeProcessId) === Number(processInfo.routeProcessId)
    ),
    `PQC source fields must be scoped to selected process: ${JSON.stringify(sourceFields)}`
  )

  const sourceSelect = page.locator('.batch-record-cell-link__source-select').first()
  await sourceSelect.waitFor({ state: 'visible', timeout: config.timeout })
  const sourceSelectText = (await sourceSelect.innerText()).replace(/\s+/g, ' ').trim()
  if (!sourceSelectText.includes('一线PQC数据')) {
    await sourceSelect.click()
    const pqcOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: /^一线PQC数据$/ }).first()
    await pqcOption.waitFor({ state: 'visible', timeout: 30000 })
    await pqcOption.click()
    await settle(page, 30000)
  }

  const sourcePanel = page.locator('.batch-record-cell-link__work-order-field-panel').first()
  await sourcePanel.waitFor({ state: 'visible', timeout: config.timeout })
  const panelText = (await sourcePanel.innerText()).replace(/\s+/g, ' ')
  assert.ok(panelText.includes('一线PQC字段'), `source panel should identify PQC fields: ${panelText}`)
  for (const field of currentProcessFields.slice(0, Math.min(5, currentProcessFields.length))) {
    assert.ok(panelText.includes(field.fieldName), `PQC field not visible: ${field.fieldName}`)
  }

  const targetForm = (context.forms || []).find((form) => form.reportId === context.defaultTargetReportId) || (context.forms || [])[0]
  assert.ok(targetForm, 'target process inspection form missing')
  assert.ok(
    targetForm.routeProcessId === undefined ||
      targetForm.routeProcessId === null ||
      Number(targetForm.routeProcessId) === Number(processInfo.routeProcessId),
    `target form must belong to selected process or be shared: ${JSON.stringify(targetForm)}`
  )
  assert.ok(
    page.locator('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable').first(),
    'target form selector must exist'
  )
  const targetSelectableCount = await page
    .locator('.batch-record-cell-link__pane.is-target .batch-record-cell-link-sheet__cell.is-target-selectable')
    .count()
  assert.ok(targetSelectableCount > 0, 'process inspection target form must expose selectable cells')
  return {
    processName: processInfo.processName,
    routeProcessId: processInfo.routeProcessId,
    fieldCount: currentProcessFields.length,
    fieldNames: currentProcessFields.map((field) => field.fieldName).slice(0, 12),
    targetReportId: targetForm.reportId,
    targetReportName: targetForm.reportName,
    targetSelectableCount
  }
}

async function main() {
  assert.ok(Number.isFinite(config.routeId) && config.routeId > 0, 'routeId must be positive')
  assert.ok(Number.isFinite(config.timeout) && config.timeout > 0, 'timeout must be positive')
  fs.mkdirSync(config.taskDir, { recursive: true })
  const credentials = readLoginDefaults()
  assert.equal(credentials.tenant, '芋道源码', `readonly tenant must be 芋道源码, got ${credentials.tenant}`)
  assert.equal(credentials.username, 'admin', `readonly username must be admin, got ${credentials.username}`)

  const browser = await chromium.launch({
    headless: !config.headed,
    executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || undefined
  })
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, locale: 'zh-CN' })
  const mesWriteRequests = []
  const pageErrors = []
  page.on('request', (request) => {
    if (request.url().includes('/admin-api/mes/') && !['GET', 'HEAD'].includes(request.method())) {
      mesWriteRequests.push(`${request.method()} ${new URL(request.url()).pathname}`)
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))

  const screenshot = path.join(config.taskDir, 'pqc-aggregate-readonly-passed.png')
  const failedScreenshot = path.join(config.taskDir, 'pqc-aggregate-readonly-failed.png')
  try {
    const loginData = await login(page, credentials)
    const token = resolveAccessToken(loginData)
    assert.ok(token, 'login response did not include access token')
    const flowConfig = await apiGet(page, `/mes/pro/route/flow-config?routeId=${encodeURIComponent(config.routeId)}&useType=BATCH`, token)
    const processes = collectFlowProcesses(flowConfig)
    const pickedProcesses = pickTargetProcesses(processes)
    const verifications = []
    for (const processInfo of pickedProcesses) {
      const context = await openPqcWorkbench(page, processInfo)
      verifications.push(await verifyPqcSource(page, context, processInfo))
    }
    assert.equal(mesWriteRequests.length, 0, `readonly E2E sent MES writes: ${mesWriteRequests.join(', ')}`)
    assert.deepEqual(pageErrors, [], `page errors detected: ${pageErrors.join(' | ')}`)
    await page.screenshot({ path: screenshot, fullPage: true })
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          identity: `${credentials.tenant}/${credentials.username}`,
          routeId: config.routeId,
          checkedProcesses: verifications,
          sharedTargetReport:
            new Set(verifications.map((item) => item.targetReportId)).size === 1
              ? verifications[0]?.targetReportId
              : null,
          mesWriteRequests: mesWriteRequests.length,
          pageErrors,
          screenshot
        },
        null,
        2
      )
    )
  } catch (error) {
    await page.screenshot({ path: failedScreenshot, fullPage: true }).catch(() => null)
    throw new Error(`${error.message}; screenshot=${failedScreenshot}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
