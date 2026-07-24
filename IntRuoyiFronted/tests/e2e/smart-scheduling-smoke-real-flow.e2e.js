const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '../..')
const workspaceRoot = path.resolve(repoRoot, '..')
const smokeRunId =
  process.env.MES_SMOKE_RUN_ID ||
  `SMART-SCHED-${new Date()
    .toISOString()
    .replace(/[-:.TZ]/g, '')
    .slice(0, 14)}`

const RISK_GATES = [
  {
    id: 'GAP-01',
    gate: 'unapproved feedback must not finalize schedule progress before approval'
  },
  {
    id: 'GAP-02',
    gate: 'rejected feedback must roll schedule progress back to the pre-feedback snapshot'
  },
  {
    id: 'GAP-03',
    gate: 'concurrent attribution must verify affected row count before creating formal feedback'
  },
  {
    id: 'GAP-04',
    gate: 'duplicate Excel import must be blocked by sourceFileSha256, sheetName and rowNo'
  },
  {
    id: 'GAP-05',
    gate: 'material shortage is a warning only when blockingIssueCount remains zero'
  },
  {
    id: 'GAP-06',
    gate: 'approval UI and backend permission must both respect approveUserId and currentUserId'
  },
  {
    id: 'GAP-07',
    gate: 'capacity mode must not hide missing resource constraints'
  },
  {
    id: 'GAP-08',
    gate: 'stale preview and calendarContextToken mismatch must block publish'
  },
  {
    id: 'GAP-09',
    gate: 'cross-day day/night shift boundary must preserve task date and shift ownership'
  },
  {
    id: 'GAP-10',
    gate: 'admin or shared accounts must not mask role-specific failures'
  },
  {
    id: 'REG-01',
    gate: 'confirmed work order missing from admission diff blocks the smoke run'
  },
  {
    id: 'REG-08',
    gate: 'reported and locked tasks must be preserved when re-scheduling'
  }
]

const WORKBENCH_METRICS = [
  '待排工单',
  '今日已排任务',
  '今日可用产能',
  '今日报工数量',
  '报工偏差',
  '设备维修中',
  '全局治理风险',
  '物料短缺'
]

const FEEDBACK_HEADERS = [
  '报工日期',
  '报工人编码',
  '报工人名称',
  '工段长',
  '生产订单号',
  '生产资源组',
  '生产资源',
  '派工单号',
  '产品编码',
  '产品名称',
  '规格',
  '模具编码',
  '工序编码',
  '工序名称',
  '所属部门',
  '报工数量',
  '支数',
  '公斤数',
  '实腔数',
  '全程时间',
  '生产定额',
  '工作时长',
  '注塑合模/组装公斤数',
  '注塑个数/组装个重',
  '操作'
]

const events = []

const ROLE_ENV = {
  erpCreator: {
    tenant: 'MES_SMOKE_ERP_CREATOR_TENANT',
    username: 'MES_SMOKE_ERP_CREATOR_USERNAME',
    password: 'MES_SMOKE_ERP_CREATOR_PASSWORD'
  },
  planner: {
    tenant: 'MES_SMOKE_PLANNER_TENANT',
    username: 'MES_SMOKE_PLANNER_USERNAME',
    password: 'MES_SMOKE_PLANNER_PASSWORD'
  },
  supervisor: {
    tenant: 'MES_SMOKE_SUPERVISOR_TENANT',
    username: 'MES_SMOKE_SUPERVISOR_USERNAME',
    password: 'MES_SMOKE_SUPERVISOR_PASSWORD'
  },
  nonApprover: {
    tenant: 'MES_SMOKE_NON_APPROVER_TENANT',
    username: 'MES_SMOKE_NON_APPROVER_USERNAME',
    password: 'MES_SMOKE_NON_APPROVER_PASSWORD'
  }
}

function requireEnv(name) {
  const value = process.env[name]
  assert.ok(value && value.trim(), `${name} is required for the smart scheduling smoke test`)
  return value.trim()
}

function optionalEnv(name, defaultValue) {
  const value = process.env[name]
  return value && value.trim() ? value.trim() : defaultValue
}

function numberEnv(name, defaultValue) {
  const value = process.env[name]
  if (!value || !value.trim()) {
    return defaultValue
  }
  const parsed = Number(value)
  assert.ok(Number.isFinite(parsed), `${name} must be a finite number`)
  return parsed
}

function boolEnv(name, defaultValue) {
  const value = process.env[name]
  if (value === undefined || value === '') {
    return defaultValue
  }
  return ['1', 'true', 'TRUE', 'yes', 'YES'].includes(value)
}

function isoDateAfter(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return date.toISOString().slice(0, 10)
}

function formatLocalDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}:00`
}

function isoDateTimeAfter(hours) {
  const date = new Date()
  date.setHours(date.getHours() + hours)
  return formatLocalDateTime(date)
}

function isWeekend(date) {
  const day = date.getDay()
  return day === 0 || day === 6
}

function erpSyncEligibleDateTime() {
  const date = new Date()
  date.setHours(0, 0, 0, 0)
  while (isWeekend(date)) {
    date.setDate(date.getDate() - 1)
  }
  return formatLocalDateTime(date)
}

function nextWeekdayDateTimeAfter(hours) {
  const date = new Date()
  date.setHours(date.getHours() + hours)
  while (date.getDay() === 0 || date.getDay() === 6) {
    date.setDate(date.getDate() + 1)
  }
  date.setHours(17, 30, 0, 0)
  return formatLocalDateTime(date)
}

function assertLocalOnly(baseUrl) {
  const parsed = new URL(baseUrl)
  const allowedHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]'])
  assert.ok(
    allowedHosts.has(parsed.hostname),
    `MES_SMOKE_BASE_URL must be local for real write smoke runs, got ${baseUrl}`
  )
}

function buildAccount(role, envNames, defaultPassword) {
  return {
    role,
    tenant: requireEnv(envNames.tenant),
    username: requireEnv(envNames.username),
    password: optionalEnv(envNames.password, defaultPassword)
  }
}

function accountKey(account) {
  return `${account.tenant.trim().toLowerCase()}::${account.username.trim().toLowerCase()}`
}

function assertDistinctRoleAccounts(accounts) {
  const keys = accounts.map(accountKey)
  assert.equal(
    new Set(keys).size,
    keys.length,
    `each smoke role must use a distinct real account, got ${accounts
      .map((account) => `${account.role}:${account.tenant}/${account.username}`)
      .join(', ')}`
  )
}

function assertNoAdminRole(accounts) {
  const adminAccounts = accounts.filter(
    (account) => account.username.trim().toLowerCase() === 'admin'
  )
  assert.equal(
    adminAccounts.length,
    0,
    `admin/shared accounts are blocked for this smoke test: ${adminAccounts
      .map((account) => account.role)
      .join(', ')}`
  )
}

function resolveExcelFile(filePath) {
  const absolutePath = path.resolve(filePath)
  assert.ok(fs.existsSync(absolutePath), `MES_SMOKE_EXCEL_FILE does not exist: ${absolutePath}`)
  assert.equal(
    path.extname(absolutePath).toLowerCase(),
    '.xlsx',
    `MES_SMOKE_EXCEL_FILE must point to a .xlsx workbook: ${absolutePath}`
  )
  return absolutePath
}

function sha256File(filePath) {
  return crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
}

function loadConfig() {
  const baseUrl = requireEnv('MES_SMOKE_BASE_URL').replace(/\/$/, '')
  assertLocalOnly(baseUrl)
  const defaultPassword = optionalEnv('MES_SMOKE_DEFAULT_PASSWORD', '111111')

  const configuredExcelFile = optionalEnv('MES_SMOKE_EXCEL_FILE', '')
  const excelTemplateFile = configuredExcelFile ? resolveExcelFile(configuredExcelFile) : ''
  const artifactDir = path.resolve(
    optionalEnv(
      'MES_SMOKE_ARTIFACT_DIR',
      path.join(workspaceRoot, 'output', 'smart-scheduling-smoke', smokeRunId)
    )
  )
  const capacityMode = requireEnv('MES_SMOKE_CAPACITY_MODE').toUpperCase()
  assert.ok(
    ['PLANNED', 'ACTUAL'].includes(capacityMode),
    'MES_SMOKE_CAPACITY_MODE must be PLANNED or ACTUAL'
  )

  const roles = {
    erpCreator: buildAccount('erp-production-order-creator', ROLE_ENV.erpCreator, defaultPassword),
    planner: buildAccount('planner', ROLE_ENV.planner, defaultPassword),
    supervisor: buildAccount('supervisor', ROLE_ENV.supervisor, defaultPassword),
    nonApprover: buildAccount('non-approver', ROLE_ENV.nonApprover, defaultPassword)
  }
  const accounts = Object.values(roles)
  assertDistinctRoleAccounts(accounts)
  assertNoAdminRole(accounts)

  const config = {
    baseUrl,
    roles,
    smokeRunId,
    artifactDir,
    headless: boolEnv('MES_SMOKE_HEADLESS', true),
    productCode: requireEnv('MES_SMOKE_PRODUCT_CODE'),
    erpUnitNumber: requireEnv('MES_SMOKE_ERP_UNIT_NUMBER'),
    excelTemplateFile,
    excelFile: path.join(artifactDir, `feedback-workbook-${smokeRunId}.xlsx`),
    sourceFileSha256: null,
    capacityMode,
    preserveManualLockedTasks: boolEnv('MES_SMOKE_PRESERVE_MANUAL_LOCKED_TASKS', true),
    feedbackUserCode: optionalEnv('MES_SMOKE_FEEDBACK_USER_CODE', 'aoteman'),
    feedbackUserName: optionalEnv('MES_SMOKE_FEEDBACK_USER_NAME', '芋道1'),
    feedbackApproverName: optionalEnv(
      'MES_SMOKE_FEEDBACK_APPROVER_NAME',
      roles.supervisor.username
    ),
    feedbackResourceGroup: optionalEnv('MES_SMOKE_FEEDBACK_RESOURCE_GROUP', '导管工段'),
    feedbackResourceName: optionalEnv('MES_SMOKE_FEEDBACK_RESOURCE_NAME', 'B010'),
    feedbackDepartment: optionalEnv('MES_SMOKE_FEEDBACK_DEPARTMENT', '导管工段'),
    feedbackProcessCode: optionalEnv('MES_SMOKE_FEEDBACK_PROCESS_CODE', 'B010'),
    feedbackProcessName: optionalEnv('MES_SMOKE_FEEDBACK_PROCESS_NAME', '吹球囊成型'),
    feedbackQuantity: numberEnv('MES_SMOKE_FEEDBACK_QUANTITY', 1),
    operationReason: optionalEnv(
      'MES_SMOKE_OPERATION_REASON',
      `smart scheduling smoke publish ${smokeRunId}`
    ),
    stopAfterStage: optionalEnv('MES_SMOKE_STOP_AFTER_STAGE', '').toUpperCase(),
    workOrderCode: optionalEnv('MES_SMOKE_WORK_ORDER_CODE', `${smokeRunId}-MO`),
    batchNumber: requireEnv('MES_SMOKE_BATCH_NUMBER'),
    workOrderQuantity: numberEnv('MES_SMOKE_WORK_ORDER_QUANTITY', 10),
    erpSourceBillNo: optionalEnv('MES_SMOKE_ERP_SOURCE_BILL_NO', `${smokeRunId}-SO`),
    erpPlannedStartTime: optionalEnv('MES_SMOKE_ERP_PLANNED_START_TIME', erpSyncEligibleDateTime()),
    erpPlannedFinishTime: optionalEnv(
      'MES_SMOKE_ERP_PLANNED_FINISH_TIME',
      nextWeekdayDateTimeAfter(36)
    ),
    promiseDate: optionalEnv('MES_SMOKE_PROMISE_DATE', isoDateAfter(90)),
    scheduleStartTime: optionalEnv('MES_SMOKE_SCHEDULE_START_TIME', isoDateTimeAfter(12)),
    productionOrderSyncHandlerName: optionalEnv(
      'MES_SMOKE_PRODUCTION_ORDER_SYNC_HANDLER',
      'kingdeeProductionOrderSyncJob'
    ),
    productionMaterialListSyncHandlerName: optionalEnv(
      'MES_SMOKE_PRODUCTION_MATERIAL_LIST_SYNC_HANDLER',
      'kingdeeProductionMaterialListSyncJob'
    ),
    erpSyncWaitMs: numberEnv('MES_SMOKE_ERP_SYNC_WAIT_MS', 180000),
    erpSyncPollMs: numberEnv('MES_SMOKE_ERP_SYNC_POLL_MS', 5000),
    routes: {
      erpSync: optionalEnv('MES_SMOKE_ERP_SYNC_PATH', '/erp/kingdee-sync'),
      productionMaterialList: optionalEnv(
        'MES_SMOKE_PRODUCTION_MATERIAL_LIST_PATH',
        '/erp/production/material-list'
      ),
      workbench: optionalEnv('MES_SMOKE_WORKBENCH_PATH', '/mes/pro/scheduler-workbench'),
      workOrder: optionalEnv('MES_SMOKE_WORK_ORDER_PATH', '/mes/pro/work-order'),
      scheduleOrder: optionalEnv('MES_SMOKE_SCHEDULE_ORDER_PATH', '/mes/pro/schedule-order'),
      productionSchedule: optionalEnv('MES_SMOKE_PRODUCTION_SCHEDULE_PATH', '/mes/pro/task'),
      calendar: optionalEnv('MES_SMOKE_CALENDAR_PATH', '/mes/pro/schedule-calendar'),
      feedback: optionalEnv('MES_SMOKE_FEEDBACK_PATH', '/mes/pro/feedback')
    }
  }

  return config
}

const config = loadConfig()
assert.ok(
  ['', 'IMPORT', 'ATTRIBUTE'].includes(config.stopAfterStage),
  'MES_SMOKE_STOP_AFTER_STAGE must be empty, IMPORT or ATTRIBUTE'
)

function redactConfig(value) {
  return JSON.parse(
    JSON.stringify(value, (key, val) => {
      if (/password/i.test(key)) {
        return '***'
      }
      return val
    })
  )
}

function ensureArtifactDir() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
}

function writeJsonArtifact(name, payload) {
  ensureArtifactDir()
  const target = path.join(config.artifactDir, name)
  fs.writeFileSync(target, JSON.stringify(payload, null, 2), 'utf8')
  return target
}

function sortByProcessOrder(processes) {
  return [...processes].sort(
    (left, right) =>
      Number(left.sort ?? 0) - Number(right.sort ?? 0) ||
      Number(left.id ?? 0) - Number(right.id ?? 0)
  )
}

function isSerialBoundaryMarker(process) {
  return (
    Boolean(process.keyProcessFlag) ||
    String(process.capacityMode || '').toUpperCase() === 'INFINITE_FORMULA'
  )
}

function selectSerialProgressBoundaryProcesses(processes) {
  assert.ok(Array.isArray(processes), 'schedule order process snapshots must be an array')
  const enabledProcesses = sortByProcessOrder(
    processes.filter((process) => process.enabled === true)
  )
  assert.ok(enabledProcesses.length > 0, 'schedule order must contain enabled process snapshots')
  const markerSorts = enabledProcesses
    .filter(isSerialBoundaryMarker)
    .map((process) => Number(process.sort ?? 0))
  const lastBoundarySort = markerSorts.length > 0 ? Math.max(...markerSorts) : null
  const boundaryProcesses =
    lastBoundarySort === null
      ? enabledProcesses
      : enabledProcesses.filter(
          (process) => process.sort == null || Number(process.sort) <= lastBoundarySort
        )
  assert.ok(boundaryProcesses.length > 0, 'serial boundary process selection must not be empty')
  return boundaryProcesses
}

function buildFeedbackWorkbookRows(boundaryProcesses) {
  const feedbackTime = formatLocalDateTime(new Date())
  return [
    FEEDBACK_HEADERS,
    ...boundaryProcesses.map((process, index) => {
      assert.ok(
        process.processCode,
        `boundary process is missing processCode: ${JSON.stringify(process)}`
      )
      assert.ok(
        process.processName,
        `boundary process is missing processName: ${JSON.stringify(process)}`
      )
      return [
        feedbackTime,
        config.feedbackUserCode,
        config.feedbackUserName,
        config.feedbackApproverName,
        config.workOrderCode,
        config.feedbackResourceGroup,
        config.feedbackResourceName,
        `${config.workOrderCode}-${String(process.sort ?? index + 1).padStart(2, '0')}`,
        config.productCode,
        config.productCode,
        '',
        '',
        process.processCode,
        process.processName,
        config.feedbackDepartment,
        config.feedbackQuantity,
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        '',
        ''
      ]
    })
  ]
}

function selectPositiveSmokeBoundaryProcesses(boundaryProcesses) {
  assert.ok(
    Array.isArray(boundaryProcesses) && boundaryProcesses.length > 0,
    'positive smoke requires at least one serial boundary process'
  )
  // Positive smoke validates one real serial-boundary process end to end.
  return boundaryProcesses.slice(0, 1)
}

function prepareFeedbackExcelWorkbook(admitted) {
  ensureArtifactDir()
  assert.ok(
    admitted?.scheduleOrder?.id,
    'feedback workbook preparation requires admitted schedule order'
  )
  const boundaryProcesses = selectSerialProgressBoundaryProcesses(admitted.processes)
  const smokeBoundaryProcesses = selectPositiveSmokeBoundaryProcesses(boundaryProcesses)
  const XLSX = require('xlsx')
  const workbook = XLSX.utils.book_new()
  const rows = buildFeedbackWorkbookRows(smokeBoundaryProcesses)
  const sheet = XLSX.utils.aoa_to_sheet(rows)
  XLSX.utils.book_append_sheet(workbook, sheet, '导管报工')
  XLSX.writeFile(workbook, config.excelFile, { bookType: 'xlsx', compression: true })
  config.sourceFileSha256 = sha256File(config.excelFile)
  writeJsonArtifact('feedback-workbook.json', {
    smokeRunId: config.smokeRunId,
    excelFile: config.excelFile,
    sourceFileSha256: config.sourceFileSha256,
    workOrderCode: config.workOrderCode,
    productCode: config.productCode,
    feedbackQuantity: config.feedbackQuantity,
    boundaryProcessCount: smokeBoundaryProcesses.length,
    fullBoundaryProcessCount: boundaryProcesses.length,
    boundaryProcesses: smokeBoundaryProcesses.map((process) => ({
      scheduleOrderProcessId: process.id,
      sort: process.sort,
      processCode: process.processCode,
      processName: process.processName,
      capacityMode: process.capacityMode,
      keyProcessFlag: process.keyProcessFlag,
      plannedQuantity: process.plannedQuantity,
      reportedQuantity: process.reportedQuantity,
      remainingQuantity: process.remainingQuantity
    }))
  })
  return {
    excelFile: config.excelFile,
    sourceFileSha256: config.sourceFileSha256,
    boundaryProcesses: smokeBoundaryProcesses
  }
}

function slug(name) {
  return name
    .replace(/[^\w.-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80)
}

async function captureStep(page, name, payload = {}) {
  ensureArtifactDir()
  const index = String(events.length + 1).padStart(2, '0')
  const fileName = `${index}-${slug(name)}.png`
  const screenshotPath = path.join(config.artifactDir, fileName)
  if (page && !page.isClosed()) {
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => null)
  }
  const event = {
    index: events.length + 1,
    name,
    at: new Date().toISOString(),
    screenshotPath,
    ...payload
  }
  events.push(event)
  writeJsonArtifact('timeline.json', events)
  return event
}

async function runStep(page, name, fn) {
  const startedAt = Date.now()
  try {
    const result = await fn()
    await captureStep(page, name, {
      status: 'PASS',
      elapsedMs: Date.now() - startedAt,
      result
    })
    return result
  } catch (error) {
    await captureStep(page, name, {
      status: 'FAIL',
      elapsedMs: Date.now() - startedAt,
      error: error && error.stack ? error.stack : String(error)
    })
    throw error
  }
}

function assertApiOk(payload, urlPath) {
  assert.ok(payload && typeof payload === 'object', `empty API response from ${urlPath}`)
  if ('code' in payload) {
    assert.ok(
      payload.code === 0 || payload.code === 200,
      `API ${urlPath} failed: ${JSON.stringify(payload).slice(0, 1000)}`
    )
    return payload.data
  }
  return payload
}

async function apiGetJson(page, urlPath, params = {}) {
  const payload = await page.evaluate(
    async ({ urlPath: innerPath, params: innerParams }) => {
      function parseCacheValue(key) {
        const raw = window.localStorage.getItem(key)
        if (!raw) {
          return undefined
        }
        function unwrapCacheValue(value) {
          if (typeof value !== 'string') {
            return value
          }
          try {
            return JSON.parse(value)
          } catch (_) {
            return value
          }
        }
        try {
          const parsed = JSON.parse(raw)
          if (parsed && typeof parsed === 'object') {
            if (parsed.v !== undefined) return unwrapCacheValue(parsed.v)
            if (parsed.value !== undefined) return unwrapCacheValue(parsed.value)
            if (parsed.data !== undefined) return unwrapCacheValue(parsed.data)
          }
        } catch (_) {
          return raw
        }
        return raw
      }

      const accessToken = parseCacheValue('ACCESS_TOKEN')
      const tenantId = parseCacheValue('tenantId')
      if (!accessToken) {
        throw new Error('ACCESS_TOKEN is missing after login')
      }
      if (!tenantId) {
        throw new Error('tenantId is missing after login')
      }

      const target = new URL(`/admin-api${innerPath}`, window.location.origin)
      for (const [key, value] of Object.entries(innerParams || {})) {
        if (value !== undefined && value !== null && value !== '') {
          target.searchParams.set(key, String(value))
        }
      }
      const response = await fetch(target.toString(), {
        method: 'GET',
        headers: {
          Accept: 'application/json',
          Authorization: `Bearer ${accessToken}`,
          'tenant-id': String(tenantId)
        }
      })
      const text = await response.text()
      let body
      try {
        body = text ? JSON.parse(text) : null
      } catch (_) {
        body = text
      }
      return {
        status: response.status,
        ok: response.ok,
        body
      }
    },
    { urlPath, params }
  )
  assert.ok(
    payload.ok,
    `GET ${urlPath} failed with HTTP ${payload.status}: ${JSON.stringify(payload.body)}`
  )
  return assertApiOk(payload.body, urlPath)
}

async function successfulUiResponse(page, urlPart, action, allowedMethods = ['POST', 'PUT']) {
  const [response] = await Promise.all([
    page.waitForResponse(
      (candidate) =>
        candidate.url().includes(urlPart) && allowedMethods.includes(candidate.request().method()),
      { timeout: 60000 }
    ),
    action()
  ])
  const payload = await response
    .json()
    .catch(async () => ({ raw: await response.text().catch(() => '') }))
  assert.ok(
    response.ok(),
    `${urlPart} returned HTTP ${response.status()}: ${JSON.stringify(payload).slice(0, 1000)}`
  )
  return assertApiOk(payload, urlPart)
}

async function waitForAnyVisible(locator, label, timeout = 30000) {
  await locator
    .first()
    .waitFor({ state: 'visible', timeout })
    .catch(async (error) => {
      throw new Error(`${label} is not visible: ${error.message}`)
    })
  return locator.first()
}

async function fillFirstVisible(locator, value, label) {
  const target = await waitForAnyVisible(locator, label)
  await target.fill(String(value))
  return target
}

async function clickVisible(locator, label) {
  const target = await waitForAnyVisible(locator, label)
  await target.click()
  return target
}

async function clickButton(root, name, label = name) {
  return clickVisible(root.getByRole('button', { name }).first(), `button ${label}`)
}

async function switchFeedbackTab(page, tabName) {
  const tab = page.locator('.el-tabs__item').filter({ hasText: tabName }).first()
  await clickVisible(tab, `feedback tab ${tabName}`)
  await waitForAnyVisible(
    page.locator('.el-tabs__item.is-active').filter({ hasText: tabName }).first(),
    `active feedback tab ${tabName}`
  )
}

async function searchPendingImportRecord(page, record) {
  const recordId = record.id
  await switchFeedbackTab(page, '待归属')
  await fillFirstVisible(
    page.locator('input[placeholder="请输入记录编号"]').first(),
    String(recordId),
    `pending import record id ${recordId}`
  )
  const responsePromise = page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/admin-api/mes/pro/feedback/import-record/page') &&
      candidate.request().method() === 'GET',
    { timeout: 60000 }
  )
  await clickButton(page, /搜索/, 'search pending import record')
  const response = await responsePromise
  const payload = await response.json()
  assert.ok(
    response.ok(),
    `/mes/pro/feedback/import-record/page returned HTTP ${response.status()}: ${JSON.stringify(payload).slice(0, 1000)}`
  )
  assertApiOk(payload, '/mes/pro/feedback/import-record/page')
  const rows = payload.data?.list || payload.list || []
  assert.ok(rows.length > 0, `pending import record search returned no rows for ${recordId}`)
  const pageRecord = rows.find((item) => String(item.id) === String(recordId))
  assert.ok(pageRecord, `pending import record page is missing record ${recordId}`)
  const row = page
    .locator('.el-table__row')
    .first()
  await waitForAnyVisible(row, `import record row ${recordId}`)
  return row
}

async function confirmMessageBox(page, label) {
  const box = await waitForAnyVisible(page.locator('.el-message-box:visible').last(), label)
  const confirmButton = await waitForAnyVisible(
    box.getByRole('button', { name: /确认|确定|确 定/ }).first(),
    `${label} confirm button`
  )
  await confirmButton.focus()
  await page.keyboard.press('Enter')
  return confirmButton
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function openAppPath(page, routePath) {
  const url = `${config.baseUrl}${routePath}`
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.waitForLoadState('networkidle', { timeout: 60000 }).catch(() => null)
}

async function selectTenant(page, tenantName) {
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  await tenantInput.waitFor({ state: 'visible', timeout: 30000 })
  const tenantResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/system/tenant/get-id-by-name') &&
        response.url().includes(encodeURIComponent(tenantName)) &&
        response.ok(),
      { timeout: 30000 }
    )
    .catch(() => null)
  await tenantInput.click()
  await tenantInput.fill('')
  await tenantInput.fill(tenantName)
  await tenantInput.press('Enter')
  await tenantResponsePromise
  const matchedOption = page
    .locator('.el-select-dropdown__item')
    .filter({ hasText: tenantName })
    .first()
  if ((await matchedOption.count()) > 0) {
    await matchedOption.click({ timeout: 3000 }).catch(() => null)
  }
}

async function login(page, account, redirectPath) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(redirectPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  if (!page.url().includes('/login')) {
    await openAppPath(page, redirectPath)
    return {}
  }

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(
    await loginForm.locator('.verify-img-panel, .verify-bar-area').count(),
    0,
    'captcha is visible; configure a non-captcha test login path before running this smoke test'
  )
  await selectTenant(page, account.tenant)
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入用户名"]'),
    account.username,
    'username'
  )
  await fillFirstVisible(
    loginForm.locator('input[placeholder="请输入密码"]'),
    account.password,
    'password'
  )
  const loginResponsePromise = page.waitForResponse(
    (candidate) =>
      candidate.url().includes('/system/auth/login') && candidate.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '登录' }).click()
  const loginRawResponse = await loginResponsePromise
  const loginPayload = await loginRawResponse.json().catch(async () => ({
    raw: await loginRawResponse.text().catch(() => '')
  }))
  assert.ok(
    loginRawResponse.ok(),
    `/system/auth/login returned HTTP ${loginRawResponse.status()}: ${JSON.stringify(loginPayload).slice(0, 1000)}`
  )
  const loginResponse = assertApiOk(loginPayload, '/system/auth/login')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await openAppPath(page, redirectPath)
  const permission = await apiGetJson(page, '/system/auth/get-permission-info')
  writeJsonArtifact(`permission-${account.role}.json`, {
    account: { role: account.role, tenant: account.tenant, username: account.username },
    permission
  })
  return {
    loginResponse,
    permission
  }
}

async function formItem(dialog, label) {
  const item = dialog.locator('.el-form-item').filter({ hasText: label }).first()
  await item.waitFor({ state: 'visible', timeout: 30000 })
  return item
}

async function fillInputByLabel(dialog, label, value) {
  const item = await formItem(dialog, label)
  const input = item.locator('input:not([readonly]), textarea').first()
  await fillFirstVisible(input, value, label)
}

async function fillNumberByLabel(dialog, label, value) {
  const item = await formItem(dialog, label)
  const input = item.locator('input').first()
  await fillFirstVisible(input, value, label)
}

async function fillDateByPlaceholder(root, placeholder, value) {
  const input = root.locator(`input[placeholder="${placeholder}"]`).first()
  await fillFirstVisible(input, value, placeholder)
  await input.press('Enter')
}

async function selectOptionByLabel(page, dialog, label, optionText) {
  const item = await formItem(dialog, label)
  const trigger = item
    .locator('.el-select__wrapper, .el-input__wrapper, input[role="combobox"], .el-select input')
    .first()
  await trigger.click()
  await clickVisible(
    page.locator('.el-select-dropdown__item:visible').filter({ hasText: optionText }).first(),
    `${label} option ${optionText}`
  )
}

async function selectProduct(page, dialog, productCode) {
  const item = await formItem(dialog, '产品')
  const input = item.locator('input[role="combobox"], input').first()
  await input.click()
  await input.fill(productCode)
  await clickVisible(
    page
      .locator('.el-select-dropdown__item, .el-table__row')
      .filter({ hasText: productCode })
      .first(),
    `product ${productCode}`
  )
}

async function assertWorkbenchMetrics(page) {
  await openAppPath(page, config.routes.workbench)
  for (const label of WORKBENCH_METRICS) {
    await waitForAnyVisible(page.getByText(label, { exact: true }), `workbench metric ${label}`)
  }
  const summary = await apiGetJson(page, '/mes/pro/scheduler-workbench/summary', {
    date: new Date().toISOString().slice(0, 10)
  })
  writeJsonArtifact('workbench-summary.json', {
    smokeRunId,
    metrics: WORKBENCH_METRICS,
    summary
  })
  return summary
}

async function fillErpProductionOrderDialog(page, dialog) {
  await fillInputByLabel(dialog, 'ERP工单号', config.workOrderCode)
  await fillInputByLabel(dialog, '物料编码', config.productCode)
  await fillInputByLabel(dialog, '单位编码', config.erpUnitNumber)
  await fillNumberByLabel(dialog, '生产数量', config.workOrderQuantity)
  await fillDateByPlaceholder(dialog, '请选择计划开始时间', config.erpPlannedStartTime)
  await fillDateByPlaceholder(dialog, '请选择计划完成时间', config.erpPlannedFinishTime)
  await fillInputByLabel(dialog, '来源单号', config.erpSourceBillNo)
  await fillInputByLabel(dialog, '批次号', config.batchNumber)
}

async function createErpProductionOrder(page) {
  await openAppPath(page, config.routes.erpSync)
  await clickButton(page, /新增ERP工单/, '新增ERP工单')
  const dialog = page
    .locator('.el-dialog:visible, .el-overlay-dialog:visible')
    .filter({ hasText: '新增ERP工单' })
    .last()
  await waitForAnyVisible(dialog, 'ERP production order dialog')
  await fillErpProductionOrderDialog(page, dialog)
  const erpProductionOrder = await successfulUiResponse(
    page,
    '/admin-api/erp/kingdee-sync/production-order/create',
    async () => {
      await clickButton(dialog, /创建并提交ERP工单/, 'create and submit ERP production order')
    },
    ['POST']
  )
  assert.equal(
    String(erpProductionOrder.erpBillNo),
    config.workOrderCode,
    `ERP production order billNo mismatch: ${JSON.stringify(erpProductionOrder)}`
  )
  assert.equal(
    Boolean(erpProductionOrder.submitted),
    true,
    'ERP production order must be submitted before MES sync'
  )
  writeJsonArtifact('created-erp-production-order.json', {
    workOrderCode: config.workOrderCode,
    productCode: config.productCode,
    batchNumber: config.batchNumber,
    erpProductionOrder
  })
  return erpProductionOrder
}

async function triggerSyncJob(page, rowText, handlerName, artifactName) {
  await openAppPath(page, config.routes.erpSync)
  const row = page
    .locator('.el-table__row')
    .filter({ hasText: rowText })
    .filter({ hasText: handlerName })
    .first()
  await waitForAnyVisible(row, `sync row ${handlerName}`)
  const syncTrigger = await successfulUiResponse(
    page,
    '/admin-api/infra/job/trigger',
    async () => {
      await row.getByRole('button', { name: /执行一次/ }).click()
    },
    ['PUT']
  )
  writeJsonArtifact(artifactName, {
    handlerName,
    syncTrigger
  })
  return syncTrigger
}

async function triggerProductionOrderSync(page) {
  return triggerSyncJob(
    page,
    '生产工单',
    config.productionOrderSyncHandlerName,
    'production-order-sync-trigger.json'
  )
}

async function triggerProductionMaterialListSync(page) {
  return triggerSyncJob(
    page,
    '生产用料清单',
    config.productionMaterialListSyncHandlerName,
    'production-material-list-sync-trigger.json'
  )
}

async function findMesWorkOrderByCode(page) {
  const data = await apiGetJson(page, '/mes/pro/work-order/page', {
    pageNo: 1,
    pageSize: 20,
    code: config.workOrderCode
  })
  const rows = Array.isArray(data.list) ? data.list : []
  return {
    pageData: data,
    workOrder: rows.find((row) => String(row.code) === String(config.workOrderCode))
  }
}

async function findProductionMaterialListByWorkOrderCode(page) {
  const data = await apiGetJson(page, '/erp/production-material-list/page', {
    pageNo: 1,
    pageSize: 100,
    productionOrderNo: config.workOrderCode
  })
  const rows = Array.isArray(data.list) ? data.list : []
  return {
    pageData: data,
    rows: rows.filter((row) => String(row.productionOrderNo) === String(config.workOrderCode))
  }
}

async function waitForMesWorkOrderSync(page, erpProductionOrder) {
  const deadline = Date.now() + config.erpSyncWaitMs
  const attempts = []
  while (Date.now() <= deadline) {
    const snapshot = await findMesWorkOrderByCode(page)
    attempts.push({
      at: new Date().toISOString(),
      total: snapshot.pageData.total || 0,
      found: Boolean(snapshot.workOrder)
    })
    if (snapshot.workOrder) {
      assert.equal(
        String(snapshot.workOrder.productCode),
        String(config.productCode),
        `synced MES work order productCode mismatch: ${JSON.stringify(snapshot.workOrder)}`
      )
      assert.equal(
        String(snapshot.workOrder.batchCode),
        String(config.batchNumber),
        `synced MES work order batchCode mismatch: ${JSON.stringify(snapshot.workOrder)}`
      )
      writeJsonArtifact('synced-mes-work-order.json', {
        erpProductionOrder,
        batchNumber: config.batchNumber,
        workOrder: snapshot.workOrder,
        attempts
      })
      return {
        workOrderId: snapshot.workOrder.id,
        workOrder: snapshot.workOrder,
        erpProductionOrder
      }
    }
    await sleep(config.erpSyncPollMs)
  }
  writeJsonArtifact('mes-work-order-sync-timeout.json', {
    erpProductionOrder,
    attempts
  })
  throw new Error(
    `ERP production order ${config.workOrderCode} was not synchronized to MES within ${config.erpSyncWaitMs}ms`
  )
}

async function waitForProductionMaterialListSync(page, created) {
  await openAppPath(page, config.routes.productionMaterialList)
  const deadline = Date.now() + config.erpSyncWaitMs
  const attempts = []
  while (Date.now() <= deadline) {
    const snapshot = await findProductionMaterialListByWorkOrderCode(page)
    attempts.push({
      at: new Date().toISOString(),
      total: snapshot.pageData.total || 0,
      matchedRows: snapshot.rows.length
    })
    if (snapshot.rows.length > 0) {
      writeJsonArtifact('synced-production-material-list.json', {
        workOrderCode: config.workOrderCode,
        workOrderId: created.workOrderId,
        attempts,
        rows: snapshot.rows
      })
      return {
        rows: snapshot.rows,
        attempts
      }
    }
    await sleep(config.erpSyncPollMs)
  }
  writeJsonArtifact('production-material-list-sync-timeout.json', {
    workOrderCode: config.workOrderCode,
    workOrderId: created.workOrderId,
    attempts
  })
  throw new Error(
    `ERP production material list for ${config.workOrderCode} was not synchronized within ${config.erpSyncWaitMs}ms`
  )
}

async function createErpProductionOrderAndWaitForMesSync(erpPage, mesPage) {
  const erpProductionOrder = await createErpProductionOrder(erpPage)
  const productionOrderSyncTrigger = await triggerProductionOrderSync(erpPage)
  const synced = await waitForMesWorkOrderSync(mesPage, erpProductionOrder)
  const productionMaterialListSyncTrigger = await triggerProductionMaterialListSync(erpPage)
  const productionMaterialListSync = await waitForProductionMaterialListSync(erpPage, synced)
  return {
    ...synced,
    productionOrderSyncTrigger,
    productionMaterialListSyncTrigger,
    productionMaterialListSync
  }
}

async function admitWorkOrderToSchedulePool(page, created) {
  await openAppPath(page, config.routes.scheduleOrder)
  await clickButton(page, /同步工单/, 'open admission diff')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '待同步差异' }).last()
  await waitForAnyVisible(dialog, 'admission diff dialog')
  await fillFirstVisible(
    dialog.locator('input[placeholder="请输入工单编码"]').first(),
    config.workOrderCode,
    'admission work order code'
  )
  await clickButton(dialog, /搜索/, 'query admission diff')

  const admissionData = await apiGetJson(page, '/mes/pro/schedule-order/admission-diff', {
    pageNo: 1,
    pageSize: 20,
    workOrderCode: config.workOrderCode,
    admissionStatus: 'READY_TO_ADMIT'
  })
  const rows = Array.isArray(admissionData.list) ? admissionData.list : []
  const target = rows.find((row) => String(row.workOrderId) === String(created.workOrderId))
  assert.ok(
    target,
    `REG-01: confirmed work order ${config.workOrderCode} is missing from READY_TO_ADMIT admission diff`
  )
  assert.equal(
    target.admissionStatus,
    'READY_TO_ADMIT',
    `admissionStatus must be READY_TO_ADMIT: ${JSON.stringify(target)}`
  )
  assert.equal(
    Boolean(target.selectable),
    true,
    `work order must be selectable for admission: ${JSON.stringify(target)}`
  )

  const row = dialog.locator('.el-table__row').filter({ hasText: config.workOrderCode }).first()
  await waitForAnyVisible(row, `admission diff row ${config.workOrderCode}`)
  await row.locator('.el-checkbox').first().click()
  await fillFirstVisible(
    dialog.locator('input[placeholder="承诺交期"]').first(),
    config.promiseDate,
    'promise date'
  )
  await dialog.locator('input[placeholder="承诺交期"]').first().press('Enter')
  await successfulUiResponse(
    page,
    '/admin-api/mes/pro/schedule-order/create-from-work-order',
    async () => {
      await clickButton(dialog, /选中工单加入排产工单池/, '选中工单加入排产工单池')
    },
    ['POST']
  )

  const pageData = await apiGetJson(page, '/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 20,
    erpWorkOrderCode: config.workOrderCode
  })
  const scheduleOrder = (pageData.list || []).find(
    (row) => String(row.workOrderId) === String(created.workOrderId)
  )
  assert.ok(scheduleOrder, `schedule order was not created for work order ${config.workOrderCode}`)
  assert.ok(
    scheduleOrder.sourceSnapshotJson,
    'schedule order must keep sourceSnapshotJson for traceability'
  )
  const processes = await apiGetJson(page, '/mes/pro/schedule-order/process-list', {
    scheduleOrderId: scheduleOrder.id
  })
  assert.ok(
    Array.isArray(processes) && processes.length > 0,
    'schedule order must have process snapshots'
  )
  for (const process of processes) {
    assert.ok(
      process.resourceSnapshotJson,
      `process ${process.processCode || process.processName} missing resourceSnapshotJson`
    )
    assert.ok(
      Number(process.remainingQuantity) >= 0,
      `process remainingQuantity must be non-negative: ${JSON.stringify(process)}`
    )
  }

  writeJsonArtifact('admitted-schedule-order.json', {
    target,
    scheduleOrder,
    sourceSnapshotJson: scheduleOrder.sourceSnapshotJson,
    processes
  })
  return {
    scheduleOrder,
    processes
  }
}

async function autoSchedulePreviewAndPublish(page, admitted) {
  await openAppPath(page, config.routes.productionSchedule)
  await fillFirstVisible(
    page.locator('input[placeholder="请输入工单编码"]').first(),
    config.workOrderCode,
    'production schedule search'
  )
  await clickButton(page, /搜索|查询/, 'query production schedule')
  await clickButton(page, /自动排产/, 'open auto schedule drawer')
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '自动排产' }).last()
  await waitForAnyVisible(drawer, 'auto schedule drawer')
  const startItem = await formItem(drawer, '开始时间')
  const startInput = startItem.locator('input').first()
  await fillFirstVisible(startInput, config.scheduleStartTime, 'schedule start time')
  await startInput.press('Enter')
  await selectOptionByLabel(
    page,
    drawer,
    '产能口径',
    config.capacityMode === 'PLANNED' ? '计划产能' : '实际产能'
  )
  const reasonInput = drawer.locator('textarea[placeholder="请输入本次自动排产发布原因"]').first()
  await waitForAnyVisible(reasonInput, 'auto schedule operation reason input')
  await fillFirstVisible(reasonInput, config.operationReason, 'auto schedule operation reason')

  const preserveManualLockedTasks = config.preserveManualLockedTasks
  const switchInput = drawer.locator('.el-switch').first()
  const switchSelected = await switchInput
    .evaluate((element) => element.classList.contains('is-checked'))
    .catch(() => null)
  if (switchSelected !== null && switchSelected !== preserveManualLockedTasks) {
    await switchInput.click()
  }

  const preview = await successfulUiResponse(
    page,
    '/admin-api/mes/pro/auto-schedule/preview',
    async () => {
      await clickButton(drawer, /生成预览/, 'auto schedule preview')
    }
  )
  assert.ok(preview.calendarContextToken, 'calendarContextToken is required before publishing')
  assert.equal(
    Number(preview.summary?.blockingIssueCount || 0),
    0,
    `blockingIssueCount must be 0 before publish: ${JSON.stringify(preview.summary)}`
  )
  const shortageCount = Number(preview.summary?.shortageCount || 0)
  writeJsonArtifact('auto-schedule-preview.json', {
    calendarContextToken: preview.calendarContextToken,
    shortageCount,
    preview
  })

  await successfulUiResponse(page, '/admin-api/mes/pro/auto-schedule/apply', async () => {
    await clickButton(drawer, /确认发布/, 'auto schedule apply')
    await confirmMessageBox(page, 'confirm auto schedule apply')
  })

  const workOrder = await apiGetJson(page, '/mes/pro/work-order/get', {
    id: admitted.scheduleOrder.workOrderId
  })
  assert.ok(
    Number(workOrder.quantityScheduled) > 0,
    `quantityScheduled must be updated after publish: ${JSON.stringify(workOrder)}`
  )
  writeJsonArtifact('post-publish-work-order.json', {
    quantityScheduled: workOrder.quantityScheduled,
    workOrder
  })
  return {
    preview,
    workOrder
  }
}

function resolveCalendarMonthFromPublishResult(publishResult) {
  const previewStartDate =
    publishResult?.preview?.summary?.startTime || publishResult?.preview?.tasks?.find((task) => task?.startDate)?.startDate
  assert.ok(
    previewStartDate,
    `calendar verification requires publish preview startTime: ${JSON.stringify(publishResult)}`
  )
  return new Date(previewStartDate).toISOString().slice(0, 7)
}

async function verifyCalendar(page, admitted, publishResult) {
  await openAppPath(page, config.routes.calendar)
  for (const label of ['白班', '夜班', '短缺', '锁定']) {
    await waitForAnyVisible(page.getByText(label, { exact: true }), `calendar label ${label}`)
  }
  const calendarMonth = resolveCalendarMonthFromPublishResult(publishResult)
  const monthData = await apiGetJson(page, '/mes/pro/schedule-calendar/month', {
    month: calendarMonth
  })
  writeJsonArtifact('calendar-month-after-publish.json', {
    scheduleOrderId: admitted.scheduleOrder.id,
    scheduleOrderCode: admitted.scheduleOrder.code,
    calendarMonth,
    monthData
  })
  return monthData
}

async function uploadFeedbackExcel(page, feedbackWorkbook) {
  assert.ok(
    feedbackWorkbook?.boundaryProcesses?.length > 0,
    'feedback import requires generated boundary process rows'
  )
  await openAppPath(page, config.routes.feedback)
  await clickButton(page, /第三方导入/, '第三方导入')
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '导入第三方报工' }).last()
  await waitForAnyVisible(dialog, 'third party feedback import dialog')
  const fileInput = dialog.locator('input[type="file"]').first()
  await fileInput.setInputFiles(feedbackWorkbook.excelFile)
  const result = await successfulUiResponse(
    page,
    '/admin-api/mes/pro/feedback/import-third-party-xlsx',
    async () => {
      await clickButton(dialog, /确 定|确定/, 'submit third party import')
    },
    ['POST']
  )
  assert.ok(
    Number(result.importedCount || 0) > 0,
    `Excel import must produce records: ${JSON.stringify(result)}`
  )
  assert.equal(
    Number(result.importedCount || 0),
    feedbackWorkbook.boundaryProcesses.length,
    `Excel import must produce one record per serial boundary process: ${JSON.stringify(result)}`
  )
  await page
    .getByRole('button', { name: /确定|OK/ })
    .last()
    .click({ timeout: 10000 })
    .catch(() => null)
  writeJsonArtifact('feedback-import-result.json', {
    sourceFileSha256: feedbackWorkbook.sourceFileSha256,
    excelFile: feedbackWorkbook.excelFile,
    boundaryProcessCount: feedbackWorkbook.boundaryProcesses.length,
    result
  })
  return result
}

async function loadImportRecordsByIds(page, importRecordIds, extraParams = {}) {
  const data = await apiGetJson(page, '/mes/pro/feedback/import-record/page', {
    pageNo: 1,
    pageSize: Math.max(20, importRecordIds.length),
    ...extraParams
  })
  const idSet = new Set(importRecordIds.map((id) => String(id)))
  const records = (data.list || []).filter((row) => idSet.has(String(row.id)))
  records.sort(
    (left, right) =>
      Number(left.rowNo || 0) - Number(right.rowNo || 0) || Number(left.id) - Number(right.id)
  )
  assert.equal(
    records.length,
    importRecordIds.length,
    `not all imported records are visible in pending attribution list: expected ${importRecordIds.join(', ')}, got ${records
      .map((row) => row.id)
      .join(', ')}`
  )
  return records
}

async function loadScheduleOrderProcesses(page, scheduleOrderId) {
  const processes = await apiGetJson(page, '/mes/pro/schedule-order/process-list', {
    scheduleOrderId
  })
  assert.ok(
    Array.isArray(processes) && processes.length > 0,
    `schedule order ${scheduleOrderId} has no process snapshot`
  )
  return processes
}

function assertSequentialProcessContinuity(record, candidate, allProcesses) {
  const process = allProcesses.find(
    (item) => String(item.id) === String(candidate.scheduleOrderProcessId)
  )
  assert.ok(
    process,
    `candidate process is missing from schedule order process snapshot: ${JSON.stringify(candidate)}`
  )
  assert.ok(
    Number(record.feedbackQuantity) <= Number(candidate.remainingQuantity),
    `报工数量不能超过该工序剩余数量: record=${JSON.stringify(record)} candidate=${JSON.stringify(candidate)}`
  )
  const previousProcesses = allProcesses
    .filter((item) => Number(item.sort) < Number(process.sort))
    .sort((left, right) => Number(left.sort) - Number(right.sort))
  for (const previous of previousProcesses) {
    assert.ok(
      Number(previous.reportedQuantity || 0) >= Number(record.feedbackQuantity || 0),
      `后续工序不能早于前序工序完成: previous=${JSON.stringify(previous)} record=${JSON.stringify(record)}`
    )
  }
}

async function attributeOneImportRecord(page, record, admitted) {
  const candidates = await apiGetJson(page, '/mes/pro/feedback/import-record/candidates', {
    importRecordId: record.id
  })
  assert.ok(
    Array.isArray(candidates) && candidates.length > 0,
    `import record ${record.id} has no attribution candidates`
  )
  const exact = candidates.find(
    (candidate) =>
      String(candidate.scheduleOrderId) === String(admitted.scheduleOrder.id) &&
      (candidate.exactWorkOrderMatch === true || candidate.workOrderCode === config.workOrderCode)
  )
  assert.ok(exact, `import record ${record.id} has no exact candidate for ${config.workOrderCode}`)
  const latestProcesses = await loadScheduleOrderProcesses(page, admitted.scheduleOrder.id)
  assertSequentialProcessContinuity(record, exact, latestProcesses)

  const row = await searchPendingImportRecord(page, record)
  await row.getByRole('button', { name: /选择归属/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认归属' }).last()
  await waitForAnyVisible(dialog, `选择归属 dialog ${record.id}`)
  const candidateRow = dialog
    .locator('.el-table__row')
    .filter({ hasText: exact.scheduleOrderCode })
    .first()
  await waitForAnyVisible(candidateRow, `candidate row ${exact.scheduleOrderCode}`)
  await candidateRow.locator('.el-checkbox').first().click()
  const feedbackId = await successfulUiResponse(
    page,
    '/admin-api/mes/pro/feedback/import-record/attribute',
    async () => {
      await clickButton(dialog, /确认归属/, 'confirm attribution')
    },
    ['POST']
  )
  assert.ok(feedbackId, `attribute response must return feedback id for import record ${record.id}`)
  return {
    record,
    candidate: exact,
    feedbackId
  }
}

async function pickUserFromDialog(page, trigger, username, label) {
  await trigger.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '人员选择' }).last()
  await waitForAnyVisible(dialog, `${label} user dialog`)
  const usernameInput = dialog.locator('input[placeholder="请输入用户名称"]').first()
  await fillFirstVisible(usernameInput, username, `${label} username search`)
  await clickButton(dialog, /搜索/, `${label} search user`)
  const userRow = dialog.locator('.el-table__row').filter({ hasText: username }).first()
  await waitForAnyVisible(userRow, `${label} user row ${username}`)
  await userRow.dblclick()
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
}

async function fillImportRecordConfirmFields(page, record) {
  await switchFeedbackTab(page, '待归属')
  const row = await searchPendingImportRecord(page, record)
  const clickableInputs = row.locator('.is-select-clickable input[readonly]')
  assert.ok(
    (await clickableInputs.count()) >= 2,
    `pending import record ${record.id} must expose feedback user and approver selectors`
  )
  await pickUserFromDialog(
    page,
    clickableInputs.nth(0),
    config.feedbackUserCode,
    `feedback user for import record ${record.id}`
  )
  const feedbackTimeInput = row.locator('input[placeholder="请选择报工时间"]').first()
  await fillFirstVisible(
    feedbackTimeInput,
    String(record.feedbackTime || Date.now()),
    `feedback time for import record ${record.id}`
  )
  await feedbackTimeInput.press('Enter').catch(() => null)
  await pickUserFromDialog(
    page,
    clickableInputs.nth(1),
    config.roles.supervisor.username,
    `approver for import record ${record.id}`
  )
}

async function verifyNonApproverCannotApprove(page, feedbackId) {
  await openAppPath(page, config.routes.feedback)
  await page.getByRole('tab', { name: /正式报工/ }).click()
  const feedback = await apiGetJson(page, '/mes/pro/feedback/get', { id: feedbackId })
  const approveUserId = feedback.approveUserId
  const currentUser = await apiGetJson(page, '/system/auth/get-permission-info')
  const currentUserId = currentUser.user?.id || currentUser.userId || currentUser.id
  assert.notEqual(
    String(currentUserId),
    String(approveUserId),
    `GAP-06 requires non-approver account to differ from approveUserId=${approveUserId}`
  )
  await fillFirstVisible(
    page.locator('input[placeholder="请输入报工单号"]').first(),
    feedback.code,
    'feedback code search'
  )
  await clickButton(page, /搜索|查询/, 'query feedback as non-approver')
  const row = page.locator('.el-table__row').filter({ hasText: feedback.code }).first()
  await waitForAnyVisible(row, `feedback row ${feedback.code}`)
  assert.equal(
    await row.getByRole('button', { name: /审批/ }).count(),
    0,
    `non-approver must not see 审批 button for feedback ${feedback.code}`
  )
  writeJsonArtifact(`non-approver-${feedbackId}.json`, {
    feedbackId,
    feedbackCode: feedback.code,
    approveUserId,
    currentUserId
  })
}

async function approveFeedback(page, attributed) {
  await openAppPath(page, config.routes.feedback)
  await page.getByRole('tab', { name: /正式报工/ }).click()
  const feedback = await apiGetJson(page, '/mes/pro/feedback/get', { id: attributed.feedbackId })
  await fillFirstVisible(
    page.locator('input[placeholder="请输入报工单号"]').first(),
    feedback.code,
    'feedback code search'
  )
  await clickButton(page, /搜索|查询/, 'query feedback for approval')
  const row = page.locator('.el-table__row').filter({ hasText: feedback.code }).first()
  await waitForAnyVisible(row, `feedback approval row ${feedback.code}`)
  await row.getByRole('button', { name: /审批/ }).click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '审批生产报工' }).last()
  await waitForAnyVisible(dialog, `审批 dialog ${feedback.code}`)
  await successfulUiResponse(page, '/admin-api/mes/pro/feedback/approve', async () => {
    await clickButton(dialog, /通 过|通过/, '审批通过')
  })
  const approved = await apiGetJson(page, '/mes/pro/feedback/get', { id: attributed.feedbackId })
  writeJsonArtifact(`approved-feedback-${attributed.feedbackId}.json`, approved)
  return approved
}

async function attributeImportedFeedbackOnly(supervisorPage, importResult, admitted) {
  const importRecordIds = importResult.importRecordIds || []
  assert.ok(importRecordIds.length > 0, 'import result must contain importRecordIds')
  const records = await loadImportRecordsByIds(supervisorPage, importRecordIds)
  const attributed = []
  const serialReadyRecords = records.slice(0, 1)
  for (const record of serialReadyRecords) {
    attributed.push(await attributeOneImportRecord(supervisorPage, record, admitted))
  }
  assert.ok(attributed.length > 0, 'attribution must create at least one formal feedback')
  return {
    attributed,
    skippedUntilApprovalRecordIds: records
      .slice(serialReadyRecords.length)
      .map((record) => record.id)
  }
}

async function verifyAttributionWithoutApproval(
  page,
  admitted,
  attributedOnly,
  artifactName = 'post-attribution-process-snapshot.json'
) {
  const after = await apiGetJson(page, '/mes/pro/schedule-order/process-list', {
    scheduleOrderId: admitted.scheduleOrder.id
  })
  const observations = []
  for (const item of attributedOnly.attributed) {
    const feedback = await apiGetJson(page, '/mes/pro/feedback/get', { id: item.feedbackId })
    assert.notEqual(
      Number(feedback.status || 0),
      4,
      `feedback ${item.feedbackId} must remain pending before approval: ${JSON.stringify(feedback)}`
    )
    const previous = admitted.processes.find(
      (process) => String(process.id) === String(item.candidate.scheduleOrderProcessId)
    )
    const current = after.find(
      (process) => String(process.id) === String(item.candidate.scheduleOrderProcessId)
    )
    assert.ok(
      previous && current,
      `attributed process snapshot missing for feedback ${item.feedbackId}`
    )
    observations.push({
      feedbackId: item.feedbackId,
      feedbackStatus: feedback.status,
      approveUserId: feedback.approveUserId,
      scheduleOrderProcessId: item.candidate.scheduleOrderProcessId,
      beforeReportedQuantity: Number(previous.reportedQuantity),
      afterReportedQuantity: Number(current.reportedQuantity),
      beforeRemainingQuantity: Number(previous.remainingQuantity),
      afterRemainingQuantity: Number(current.remainingQuantity),
      progressedBeforeApproval:
        Number(current.reportedQuantity) !== Number(previous.reportedQuantity) ||
        Number(current.remainingQuantity) !== Number(previous.remainingQuantity)
    })
  }
  writeJsonArtifact(artifactName, {
    after,
    attributedOnly,
    observations
  })
  return { observations }
}

async function confirmImportedFeedbackBatch(page, importResult) {
  const importRecordIds = importResult.importRecordIds || []
  assert.ok(importRecordIds.length > 0, 'confirm batch requires imported record ids')
  await switchFeedbackTab(page, '待归属')
  const rows = await loadImportRecordsByIds(page, importRecordIds)
  assert.ok(rows.length > 0, 'confirm batch requires visible pending import records')
  const response = await successfulUiResponse(
    page,
    '/admin-api/mes/pro/feedback/import-record/confirm-batch',
    async () => {
      await clickButton(page, /确认报工/, 'confirm imported feedback batch')
      await confirmMessageBox(page, 'confirm imported feedback batch')
    },
    ['POST']
  )
  writeJsonArtifact('confirmed-import-batch.json', {
    importRecordIds,
    response
  })
  return response
}

async function processImportedFeedbackSequentially(
  supervisorPage,
  nonApproverPage,
  plannerPage,
  importResult,
  admitted
) {
  const importRecordIds = importResult.importRecordIds || []
  assert.ok(importRecordIds.length > 0, 'import result must contain importRecordIds')
  const records = await loadImportRecordsByIds(supervisorPage, importRecordIds, {
    attributionStatus: 'PENDING'
  })
  const attributed = []
  const beforeApprovalObservations = []

  for (const record of records) {
    const item = await attributeOneImportRecord(supervisorPage, record, admitted)
    await fillImportRecordConfirmFields(supervisorPage, record)
    attributed.push(item)
    const beforeApproval = await verifyAttributionWithoutApproval(
      plannerPage,
      admitted,
      { attributed: [item] },
      `post-attribution-process-snapshot-${item.feedbackId}.json`
    )
    beforeApprovalObservations.push(...beforeApproval.observations)
  }

  await confirmImportedFeedbackBatch(supervisorPage, importResult)

  const approved = []
  for (const item of attributed) {
    await verifyNonApproverCannotApprove(nonApproverPage, item.feedbackId)
    approved.push(await approveFeedback(supervisorPage, item))
  }

  writeJsonArtifact('sequential-feedback-approval.json', {
    attributedFeedbackIds: attributed.map((item) => item.feedbackId),
    approvedFeedbackIds: approved.map((item) => item.id),
    beforeApprovalObservations
  })
  return {
    attributed,
    approved,
    beforeApprovalObservations
  }
}

async function verifyReportedTasksPreserved(page, admitted, feedbackResult) {
  const before = admitted.processes
  const after = await apiGetJson(page, '/mes/pro/schedule-order/process-list', {
    scheduleOrderId: admitted.scheduleOrder.id
  })
  for (const item of feedbackResult.attributed) {
    const previous = before.find(
      (process) => String(process.id) === String(item.candidate.scheduleOrderProcessId)
    )
    const current = after.find(
      (process) => String(process.id) === String(item.candidate.scheduleOrderProcessId)
    )
    assert.ok(
      previous && current,
      `reported process snapshot missing for feedback ${item.feedbackId}`
    )
    assert.ok(
      Number(current.reportedQuantity) >=
        Number(previous.reportedQuantity) + Number(item.record.feedbackQuantity),
      `reportedQuantity did not increase by feedback quantity for feedback ${item.feedbackId}`
    )
    assert.ok(
      Number(current.remainingQuantity) <=
        Number(previous.remainingQuantity) - Number(item.record.feedbackQuantity),
      `remainingQuantity did not decrease by feedback quantity for feedback ${item.feedbackId}`
    )
  }
  writeJsonArtifact('post-feedback-process-snapshot.json', {
    before,
    after,
    feedbackResult
  })
}

async function verifyBoundaryFeedbackCoverage(page, admitted, feedbackResult) {
  const before = admitted.processes
  const after = await loadScheduleOrderProcesses(page, admitted.scheduleOrder.id)
  const boundaryProcesses = feedbackResult.attributed
    .map((item) =>
      before.find((process) => String(process.id) === String(item.candidate.scheduleOrderProcessId))
    )
    .filter(Boolean)
  const attributedProcessIds = new Set(
    (feedbackResult.attributed || []).map((item) => String(item.candidate.scheduleOrderProcessId))
  )
  for (const process of boundaryProcesses) {
    assert.ok(
      attributedProcessIds.has(String(process.id)),
      `serial boundary process was not covered by feedback attribution: ${JSON.stringify(process)}`
    )
    const current = after.find((item) => String(item.id) === String(process.id))
    assert.ok(
      current,
      `serial boundary process is missing after approval: ${JSON.stringify(process)}`
    )
    assert.ok(
      Number(current.reportedQuantity || 0) >=
        Number(process.reportedQuantity || 0) + config.feedbackQuantity,
      `serial boundary process did not receive feedback quantity: before=${JSON.stringify(process)} after=${JSON.stringify(
        current
      )}`
    )
  }

  const pageData = await apiGetJson(page, '/mes/pro/schedule-order/page', {
    pageNo: 1,
    pageSize: 20,
    erpWorkOrderCode: config.workOrderCode
  })
  const scheduleOrder = (pageData.list || []).find(
    (row) => String(row.id) === String(admitted.scheduleOrder.id)
  )
  assert.ok(
    scheduleOrder,
    `schedule order ${admitted.scheduleOrder.id} is missing from page after boundary feedback`
  )
  assert.ok(
    Number(scheduleOrder.progressPercent || 0) > 0,
    `schedule order progress must advance after all boundary feedback is approved: ${JSON.stringify(scheduleOrder)}`
  )
  assert.ok(
    Number(scheduleOrder.completedQuantity || 0) > 0,
    `schedule order completedQuantity must advance after all boundary feedback is approved: ${JSON.stringify(
      scheduleOrder
    )}`
  )
  writeJsonArtifact('boundary-feedback-coverage.json', {
    boundaryProcessCount: boundaryProcesses.length,
    attributedFeedbackIds: (feedbackResult.attributed || []).map((item) => item.feedbackId),
    scheduleOrder,
    before,
    after
  })
  return {
    boundaryProcessCount: boundaryProcesses.length,
    progressPercent: scheduleOrder.progressPercent,
    completedQuantity: scheduleOrder.completedQuantity
  }
}

async function recordRiskGatePlan() {
  writeJsonArtifact('risk-gates.json', {
    smokeRunId,
    gates: RISK_GATES,
    note: 'Positive smoke executes the production order, admission, schedule, calendar, import, attribution and approval chain. Destructive reject/concurrency/duplicate checks are listed as gates and must run in a dedicated regression pass with explicit data ownership.'
  })
}

async function run() {
  ensureArtifactDir()
  writeJsonArtifact('config.json', redactConfig(config))
  await recordRiskGatePlan()

  const browser = await chromium.launch({ headless: config.headless })
  const contexts = []
  try {
    const newRolePage = async (account, routePath) => {
      const context = await browser.newContext({
        locale: 'zh-CN',
        viewport: { width: 1600, height: 1100 },
        acceptDownloads: true
      })
      contexts.push(context)
      const page = await context.newPage()
      page.on('console', (message) => {
        if (['error', 'warning'].includes(message.type())) {
          events.push({
            index: events.length + 1,
            name: `console-${message.type()}`,
            at: new Date().toISOString(),
            role: account.role,
            text: message.text()
          })
        }
      })
      await login(page, account, routePath)
      return page
    }

    const erpCreatorPage = await newRolePage(config.roles.erpCreator, config.routes.erpSync)
    const plannerPage = await newRolePage(config.roles.planner, config.routes.workbench)
    const created = await runStep(
      erpCreatorPage,
      'create-erp-production-order-and-wait-mes-sync',
      async () => createErpProductionOrderAndWaitForMesSync(erpCreatorPage, plannerPage)
    )
    await runStep(plannerPage, 'planner-workbench-eight-metrics', async () =>
      assertWorkbenchMetrics(plannerPage)
    )
    const admitted = await runStep(
      plannerPage,
      'admit-confirmed-work-order-to-schedule-pool',
      async () => admitWorkOrderToSchedulePool(plannerPage, created)
    )
    const published = await runStep(plannerPage, 'auto-schedule-preview-and-publish', async () =>
      autoSchedulePreviewAndPublish(plannerPage, admitted)
    )

    await runStep(plannerPage, 'calendar-shift-shortage-lock-check', async () =>
      verifyCalendar(plannerPage, admitted, published)
    )

    const supervisorPage = await newRolePage(config.roles.supervisor, config.routes.calendar)
    const feedbackWorkbook = await runStep(
      supervisorPage,
      'prepare-boundary-feedback-workbook',
      async () => prepareFeedbackExcelWorkbook(admitted)
    )
    const importResult = await runStep(supervisorPage, 'third-party-feedback-import', async () =>
      uploadFeedbackExcel(supervisorPage, feedbackWorkbook)
    )

    if (config.stopAfterStage === 'IMPORT') {
      await runStep(supervisorPage, 'reduced-import-visible', async () => {
        const records = await loadImportRecordsByIds(
          supervisorPage,
          importResult.importRecordIds || []
        )
        assert.ok(records.length > 0, 'reduced mode requires imported records to remain visible')
        return { importRecordIds: importResult.importRecordIds, recordCount: records.length }
      })
      writeJsonArtifact('smoke-report.json', {
        status: 'PASS',
        smokeRunId,
        workOrderCode: config.workOrderCode,
        sourceFileSha256: config.sourceFileSha256,
        scope: 'A1-A2-A4',
        stopAfterStage: config.stopAfterStage,
        events
      })
      console.log(
        `PASS: reduced smart scheduling smoke ${smokeRunId}; artifacts=${config.artifactDir}`
      )
      return
    }

    if (config.stopAfterStage === 'ATTRIBUTE') {
      const attributedOnly = await runStep(
        supervisorPage,
        'feedback-attribution-without-approval',
        async () => attributeImportedFeedbackOnly(supervisorPage, importResult, admitted)
      )
      await runStep(plannerPage, 'attribution-state-before-approval', async () =>
        verifyAttributionWithoutApproval(plannerPage, admitted, attributedOnly)
      )
      writeJsonArtifact('smoke-report.json', {
        status: 'PASS',
        smokeRunId,
        workOrderCode: config.workOrderCode,
        sourceFileSha256: config.sourceFileSha256,
        scope: 'A1-A2-A4',
        stopAfterStage: config.stopAfterStage,
        events
      })
      console.log(
        `PASS: reduced smart scheduling smoke ${smokeRunId}; artifacts=${config.artifactDir}`
      )
      return
    }

    const nonApproverPage = await newRolePage(config.roles.nonApprover, config.routes.feedback)
    const feedbackResult = await runStep(
      supervisorPage,
      'sequential-feedback-attribution-and-approval',
      async () =>
        processImportedFeedbackSequentially(
          supervisorPage,
          nonApproverPage,
          plannerPage,
          importResult,
          admitted
        )
    )
    await runStep(plannerPage, 'reported-task-preserved-after-approval', async () =>
      verifyReportedTasksPreserved(plannerPage, admitted, feedbackResult)
    )
    await runStep(plannerPage, 'boundary-feedback-progress-after-approval', async () =>
      verifyBoundaryFeedbackCoverage(plannerPage, admitted, feedbackResult)
    )

    writeJsonArtifact('smoke-report.json', {
      status: 'PASS',
      smokeRunId,
      workOrderCode: config.workOrderCode,
      sourceFileSha256: config.sourceFileSha256,
      events
    })
    console.log(`PASS: smart scheduling smoke ${smokeRunId}; artifacts=${config.artifactDir}`)
  } catch (error) {
    writeJsonArtifact('smoke-report.json', {
      status: 'FAIL',
      smokeRunId,
      error: error && error.stack ? error.stack : String(error),
      events
    })
    throw error
  } finally {
    for (const context of contexts.reverse()) {
      await context.close().catch(() => null)
    }
    await browser.close().catch(() => null)
  }
}

run().catch((error) => {
  console.error(error && error.stack ? error.stack : error)
  process.exit(1)
})
