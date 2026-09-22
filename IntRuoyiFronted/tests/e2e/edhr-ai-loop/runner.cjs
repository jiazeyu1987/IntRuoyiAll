#!/usr/bin/env node
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { createRunId, buildManifest, FIXED_RESET_WORK_ORDER_CODE } = require('./manifest.cjs')
const { EXIT_CODES, writeRunReport, writeFailureArtifacts, classifyError } = require('./reporter.cjs')
const { stageResults } = require('./stages.cjs')
const { freezeExecutionBaseline, assertCoverage, assertDouble100, resolveProductionIdentity, buildPqcTaskGroups, pqcTaskFormalIdentity } = require('./coverage.cjs')

function arg(name) {
  const index = process.argv.indexOf(`--${name}`)
  return index >= 0 ? process.argv[index + 1] : undefined
}

function ordersForMode(manifest) {
  return manifest.orders.filter((order) => order.slot === 'O01')
}

function stageCandidateCodePaths(stage) {
  const paths = {
    VERIFY_READY: [
      'IntRuoyiFronted/src/views/mes/pro/workorder/index.vue',
      'IntRuoyiFronted/src/api/erp/sync/index.ts',
      'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
    ],
    S01: [
      'IntRuoyiFronted/src/views/mes/pro/workorder/index.vue',
      'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/workorder/MesProWorkOrderController.java',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
    ],
    S02: [
      'IntRuoyiFronted/src/views/mes/pro/processpool/ActiveOrderSubmissionDetailPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
    ],
    S03: [
      'IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue',
      'IntRuoyiFronted/src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcSubmitServiceImpl.java',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderSubmissionReviewServiceImpl.java'
    ],
    S04: [
      'IntRuoyiFronted/src/views/mes/pro/processpool/activeOrderReplenishmentConfirmation.ts',
      'IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java'
    ],
    S05: [
      'IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/production-release/PqcProductionReleasePage.vue',
      'IntRuoyiFronted/src/api/mes/pro/productionRelease/index.ts',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/productionrelease/MesPqcProductionReleaseServiceImpl.java'
    ],
    S06: [
      'IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
      'IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts'
    ],
    S07: [
      'IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
      'IntRuoyiFronted/src/api/approval-center/index.ts',
      'IntRuoyiFronted/src/api/mes/pro/edhr/release.ts'
    ],
    S08: [
      'IntRuoyiFronted/src/views/mes/pro/edhr-work-task/WorkTaskBoardPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue',
      'IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordHistoryPage.vue',
      'IntRuoyiFronted/src/api/mes/pro/edhr/batchExecution.ts',
      'IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java'
    ]
  }
  return paths[stage] || [
    'IntRuoyiFronted/tests/e2e/edhr-ai-loop/runner.cjs',
    'IntRuoyiFronted/tests/e2e/edhr-ai-loop/reporter.cjs'
  ]
}

function targetRequestLabel(method, pathname) {
  const route = `${method} ${pathname}`
  if (route.includes('/system/auth/login')) return 'LOGIN'
  if (route.includes('/erp/kingdee-sync/incremental-sync')) return pathname.includes('/pick-list') ? 'PRODUCTION_PICK_LIST_SYNC' : 'ERP_PRODUCTION_ORDER_SYNC'
  if (route.includes('/active-order/list')) return 'ACTIVE_ORDER_LIST'
  if (route.includes('/active-order/simulation/test-reset')) return 'SIMULATION_TEST_RESET'
  if (route.includes('/active-order/release/apply')) return 'ACTIVE_ORDER_RELEASE_APPLY'
  if (route.includes('/frontline/submit')) return 'FRONTLINE_PRODUCTION_SUBMIT'
  if (route.includes('/device-account/pqc/submit')) return 'FRONTLINE_PQC_SUBMIT'
  if (route.includes('/submission/allocation/preview-fifo')) return 'PRODUCTION_REVIEW_FIFO_PREVIEW'
  if (route.includes('/submission/allocation/confirm')) return 'PRODUCTION_REVIEW_CONFIRM'
  if (route.includes('/submission/review')) return 'PQC_REVIEW_CONFIRM'
  if (route.includes('/edhr-work-task') && method === 'GET') return 'WORK_TASK_PAGE'
  if (route.includes('/pqc-production-release/page')) return 'PQC_RELEASE_PAGE'
  if (route.includes('/pqc-production-release/approve')) return 'PQC_RELEASE_APPROVE'
  if (route.includes('/edhr-nonconformance-review/create')) return 'NONCONFORMANCE_REVIEW_CREATE'
  if (route.includes('/edhr-nonconformance-review/dispose')) return 'NONCONFORMANCE_REVIEW_DISPOSE'
  if (route.includes('/special-node/attachment/prepare-upload')) return 'REPORT_PREPARE_UPLOAD'
  if (route.includes('/special-node/complete')) return 'REPORT_COMPLETE'
  if (route.includes('/approval-center/tasks/review')) return 'MANAGER_RELEASE_APPROVE'
  if (route.includes('/edhr-release/get')) return 'EDHR_RELEASE_GET'
  if (route.includes('/edhr-batch-execution-archive/generate')) return 'ARCHIVE_GENERATE'
  if (route.includes('/edhr-batch-execution/page')) return 'HISTORY_PAGE'
  if (route.includes('/edhr-batch-execution/review-timeline')) return 'HISTORY_TIMELINE'
  return 'ADMIN_API'
}

function trackTargetRequests(page) {
  const targetRequests = []
  const pending = new Set()
  page.on('response', (response) => {
    const capture = (async () => {
      try {
        const parsedUrl = new URL(response.url())
        if (!parsedUrl.pathname.includes('/admin-api/')) return
        const method = response.request().method()
        const entry = {
          label: targetRequestLabel(method, parsedUrl.pathname),
          method,
          url: parsedUrl.pathname,
          httpStatus: response.status(),
          businessCode: null
        }
        const contentType = response.headers()['content-type'] || ''
        if (contentType.includes('application/json')) {
          try {
            const body = await response.json()
            if (body && Object.prototype.hasOwnProperty.call(body, 'code')) entry.businessCode = Number(body.code)
            if (body?.msg) entry.msg = String(body.msg).slice(0, 300)
          } catch (parseError) {
            entry.parseError = true
            entry.parseErrorMessage = errorMessage(parseError).slice(0, 300)
          }
        }
        targetRequests.push(entry)
      } catch (captureError) {
        targetRequests.push({
          label: 'TARGET_REQUEST_CAPTURE_ERROR',
          method: response.request().method(),
          url: response.url(),
          httpStatus: response.status(),
          businessCode: null,
          parseError: true,
          msg: errorMessage(captureError).slice(0, 300)
        })
      }
    })()
    pending.add(capture)
    capture.finally(() => pending.delete(capture))
  })
  targetRequests.flush = async function flushTargetRequests() {
    await Promise.allSettled([...pending])
    return targetRequests
  }
  return targetRequests
}

const mode = arg('mode') || process.env.EDHR_AI_E2E_MODE || 'full'
const onlyS01 = process.argv.includes('--only-s01')
const throughS03 = process.argv.includes('--through-s03')
const runId = arg('run-id') || process.env.EDHR_AI_E2E_RUN_ID || createRunId()
const resetWorkOrderCode = arg('reset-work-order-code') || process.env.EDHR_AI_E2E_RESET_WORK_ORDER_CODE || FIXED_RESET_WORK_ORDER_CODE
const frontendUrl = arg('base-url') || process.env.EDHR_AI_E2E_BASE_URL || 'http://127.0.0.1:8081'
const reportRoot = path.resolve(arg('report-root') || process.env.EDHR_AI_E2E_REPORT_ROOT || 'test-results/edhr-ai-loop')
const username = process.env.EDHR_E2E_USERNAME || 'admin'
const password = process.env.EDHR_E2E_PASSWORD || 'admin123'
const signaturePassword = process.env.EDHR_E2E_SIGNATURE_PASSWORD || password
let productionIdentity
const headed = process.argv.includes('--headed')
const REPORT_UPLOAD_PLAN = Object.freeze([
  Object.freeze({ nodeType: 'INCOMING_INSPECTION_REPORT', label: '来料检验报告', fileName: '01-incoming.pdf' }),
  Object.freeze({ nodeType: 'STERILIZATION_REPORT', label: '灭菌报告', fileName: '02-sterilization.pdf' }),
  Object.freeze({ nodeType: 'FINISHED_PRODUCT_INSPECTION_REPORT', label: '成品检验报告', fileName: '03-finished-report.pdf' }),
  Object.freeze({ nodeType: 'FINISHED_PRODUCT_INSPECTION_RECORD', label: '成品检验记录', fileName: '04-finished-record.pdf' })
])

function exitWithFailure(error, stage = 'S00', type = 'PRECONDITION_BLOCKED') {
  const message = error instanceof Error ? error.message : String(error)
  const { runDir } = writeFailureArtifacts({ rootDir: reportRoot, runId, mode, failedStage: stage,
    errorType: type, action: 'AI E2E前置检查', message, pageUrl: frontendUrl,
    expected: { configuredResetWorkOrderCode: true },
    actual: { resetWorkOrderCodeProvided: Boolean(resetWorkOrderCode), mode },
    candidateCodePaths: stageCandidateCodePaths(stage),
    stages: stageResults(stage, 'BLOCKED') })
  console.error(JSON.stringify({ status: 'BLOCKED', runId, failedStage: stage, errorType: type, message, runDir }, null, 2))
  process.exitCode = type === 'INFRASTRUCTURE_BLOCKED' ? EXIT_CODES.INFRASTRUCTURE_BLOCKED : EXIT_CODES.PRECONDITION_BLOCKED
}

function stageError({ stage, errorType, action, message, expected = {}, actual = {} }) {
  const error = new Error(message)
  error.stage = stage
  error.errorType = errorType
  error.action = action
  error.expected = expected
  error.actual = actual
  return error
}

function errorMessage(error) {
  return error instanceof Error ? error.message : String(error || '')
}

function requirePositiveIdString(value, label) {
  const text = String(value ?? '').trim()
  assert.match(text, /^[1-9]\d*$/, `${label}必须是后端Long ID字符串`)
  return text
}

async function runWithStage(stage, action, expected, fn) {
  console.error(`[AI-E2E] ${stage} START ${action}`)
  try {
    const result = await fn()
    console.error(`[AI-E2E] ${stage} PASS ${action}`)
    return result
  } catch (error) {
    console.error(`[AI-E2E] ${stage} FAIL ${action}: ${errorMessage(error)}`)
    if (error && typeof error === 'object' && error.stage) throw error
    const classification = classifyError(error)
    throw stageError({
      stage,
      errorType: classification.errorType,
      action,
      message: errorMessage(error),
      expected,
      actual: { message: errorMessage(error) }
    })
  }
}

function readApiDataList(body) {
  if (Array.isArray(body?.data)) return body.data
  if (Array.isArray(body?.data?.list)) return body.data.list
  if (Array.isArray(body?.data?.records)) return body.data.records
  return []
}

function findActiveOrderDataByCode(rows, workOrderCode) {
  return rows.find((row) => String(row?.workOrderCode || '').trim() === workOrderCode)
}

function resolveActiveOrderQuantity(row, label) {
  const value = Number(row?.erpFixedQuantitySnapshot ?? row?.quantity ?? row?.workOrderQuantity)
  if (!Number.isFinite(value) || value <= 0) {
    throw stageError({
      stage: 'VERIFY_READY',
      errorType: 'PRECONDITION_BLOCKED',
      action: `${label}数量读取`,
      message: `${label}缺少有效ERP生产数量`,
      expected: { erpFixedQuantitySnapshot: 'positive number' },
      actual: { row }
    })
  }
  return value
}

async function parseActiveOrderListResponse(response, action) {
  assert.equal(response.ok(), true, `${action}失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `${action}业务失败：${body.msg || 'unknown'}`)
  return readApiDataList(body)
}

async function login(page) {
  await page.goto(`${frontendUrl}/login`, { waitUntil: 'commit', timeout: 60000 })
  const loginState = await waitForLoginOrAuthenticated(page)
  if (loginState === 'AUTHENTICATED') return
  await waitForLoginFormShell(page)
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(username)
  await page.locator('.login-form input[type="password"]:visible').first().fill(password)
  const response = page.waitForResponse((r) => r.url().includes('/admin-api/system/auth/login') && r.request().method() === 'POST',
    { timeout: 60000 })
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  const loginResponse = await response
  assert.equal(loginResponse.ok(), true, `登录失败：HTTP ${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function waitForLoginOrAuthenticated(page) {
  const usernameInput = page.locator('.login-form input[placeholder="请输入用户名"]:visible').first()
  const currentPath = new URL(page.url()).pathname
  if (!currentPath.includes('/login') && !(await usernameInput.isVisible({ timeout: 500 }).catch(() => false))) {
    return 'AUTHENTICATED'
  }
  try {
    return await Promise.any([
      usernameInput.waitFor({ state: 'visible', timeout: 60000 }).then(() => 'LOGIN_FORM'),
      page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 }).then(() => 'AUTHENTICATED')
    ])
  } catch (error) {
    throw new Error(`登录续接失败：既未出现登录表单，也未恢复到已认证页面。当前页面：${page.url()}。${errorMessage(error)}`)
  }
}

async function waitForLoginFormShell(page) {
  await page.locator('.login-form:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first()
    .waitFor({ state: 'visible', timeout: 60000 })
}

async function isLoginPage(page) {
  if (page.url().includes('/login')) return true
  return page.locator('.login-form input[placeholder="请输入用户名"]:visible')
    .first().isVisible({ timeout: 1000 }).catch(() => false)
}

async function ensurePqcSession(page, manifestOrder, processKey) {
  if (!(await isLoginPage(page))) return false
  await login(page)
  await selectFrontlinePqcOrder(page, manifestOrder)
  await selectFrontlinePqcProcess(page, processKey)
  return true
}

async function selectLoginTenant(page, tenantName = '芋道源码') {
  await waitForLoginFormShell(page)
  const tenant = page.locator(
    '.login-form .el-select input[role="combobox"]:visible, .login-form input.el-select__input:visible, .login-form input[placeholder*="租户"]:visible'
  ).first()
  await tenant.waitFor({ state: 'visible', timeout: 30000 })
  await tenant.click()
  await tenant.fill(tenantName)
  const option = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenantName }).first()
  await option.waitFor({ state: 'visible', timeout: 10000 })
  await option.click()
}

async function openActiveOrderPool(page, workOrderCode) {
  const listResponse = page.waitForResponse((r) =>
    r.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
    r.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${frontendUrl}/mes/pro/process-pool/production-leader`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByRole('tab', { name: '活跃订单池' }).click()
  await page.locator('[data-team-leader-active-order-list]').waitFor({ state: 'visible', timeout: 30000 })
  const rows = await parseActiveOrderListResponse(await listResponse, '活跃订单列表加载')
  if (workOrderCode) await filterActiveOrderPool(page, workOrderCode)
  return rows
}

async function filterActiveOrderPool(page, workOrderCode) {
  const filter = page
    .locator(
      '[data-team-leader-active-order-work-order-filter] input, input[data-team-leader-active-order-work-order-filter], input[placeholder="筛选生产订单号"]'
    )
    .first()
  await filter.waitFor({ state: 'visible', timeout: 30000 })
  await filter.fill(workOrderCode)
  await page.locator('[data-team-leader-active-order-list]').waitFor({ state: 'visible', timeout: 30000 })
}

function activeOrderRow(page, workOrderCode) {
  return page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-team-leader-active-order-work-order-code]:text-is("${workOrderCode}")`)
  }).first()
}

async function resetFixedTestActiveOrder(page, manifestOrder, manifest) {
  // 1. 进入活跃订单池。
  await openActiveOrderPool(page, manifest.resetWorkOrderCode)
  const resetButton = page.locator('[data-team-leader-reset-fixed-active-order]').first()
  // 2. 点击重置按钮，由系统删除旧执行数据并创建新活跃订单。
  const resetResponse = page.waitForResponse((r) =>
    r.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/simulation/test-reset') &&
    r.request().method() === 'POST'
  )
  const refreshedListResponse = page.waitForResponse((r) =>
    r.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
    r.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => null)
  await resetButton.click()
  // 3. 检查重置成功、订单可见、数量为10。
  const response = await resetResponse
  assert.equal(response.ok(), true, `重置指定测试订单失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `重置指定测试订单业务失败：${body.msg || 'unknown'}`)
  const receipt = body.data || {}
  const resetOrderCode = String(receipt.workOrderCode || '').trim()
  assert.equal(resetOrderCode, manifest.resetWorkOrderCode, `重置回执必须返回指定测试订单号：${manifest.resetWorkOrderCode}`)
  const refreshedRows = await refreshedListResponse.then((r) => r ? parseActiveOrderListResponse(r, '重置后活跃订单列表刷新') : [])
  let resetOrder = findActiveOrderDataByCode(refreshedRows, resetOrderCode)
  if (!resetOrder) {
    resetOrder = findActiveOrderDataByCode(await openActiveOrderPool(page, resetOrderCode), resetOrderCode)
  } else {
    await filterActiveOrderPool(page, resetOrderCode)
  }
  if (!resetOrder) {
    throw stageError({
      stage: 'S01',
      errorType: 'BUSINESS_ASSERTION',
      action: 'S01重置后活跃订单可见性',
      message: `重置接口返回成功，但活跃订单池找不到指定测试订单：${resetOrderCode}`,
      expected: { resetWorkOrderCode: resetOrderCode, activeOrderVisible: true },
      actual: { activeOrderVisible: false }
    })
  }
  const resetQuantity = resolveActiveOrderQuantity(resetOrder, '重置测试订单')
  if (resetQuantity !== manifest.expected.finishedQuantity) {
    throw stageError({
      stage: 'S01',
      errorType: 'BUSINESS_ASSERTION',
      action: 'S01重置测试订单数量',
      message: `重置测试订单数量不是预期值：${resetOrderCode}`,
      expected: { quantity: manifest.expected.finishedQuantity },
      actual: { quantity: resetQuantity }
    })
  }
  await activeOrderRow(page, resetOrderCode).waitFor({ state: 'visible', timeout: 30000 })
  // 4. 保存新订单信息，供后续阶段使用。
  return {
    slot: manifestOrder.slot,
    resetWorkOrderCode: manifest.resetWorkOrderCode,
    activeOrderId: requirePositiveIdString(receipt.activeOrderId, '重置测试单回执activeOrderId'),
    workOrderId: requirePositiveIdString(receipt.workOrderId, '重置测试单回执workOrderId'),
    workOrderCode: resetOrderCode,
    workOrderName: receipt.workOrderName || resetOrder.workOrderName || resetOrder.productName,
    batchCode: String(resetOrder.batchCode || receipt.batchCode || resetOrderCode).trim(),
    quantity: resetQuantity,
    routeId: resetOrder.routeId == null ? null : requirePositiveIdString(resetOrder.routeId, '重置测试单列表routeId'),
    routeVersionId: resetOrder.routeVersionId == null ? null : requirePositiveIdString(resetOrder.routeVersionId, '重置测试单列表routeVersionId'),
    routeVersionNo: resetOrder.routeVersionNo,
    aiRunOrderSlotId: manifestOrder.simulationRunId,
    simulationRunId: manifestOrder.simulationRunId,
    resetReceiptAction: receipt.action,
    previousActiveOrderCount: receipt.previousActiveOrderCount,
    deletedEventCount: receipt.deletedEventCount,
    deletedBatchExecutionCount: receipt.deletedBatchExecutionCount,
    deletedRecordExecutionCount: receipt.deletedRecordExecutionCount,
    action: 'RESET_FIXED_TEST_ORDER'
  }
}

async function selectFrontlineProductionOrder(page, manifestOrder) {
  const productionUrl = `${frontendUrl}/mes/pro/feedback/edhr-batch-production-fill`
  await page.goto(productionUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  if (await isLoginPage(page)) {
    await login(page)
    await page.goto(productionUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
  await page.locator('[data-frontline-production-stage]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-material-tab]').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-process-current]:not(:disabled)').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-active-order-card]').click()
  await page.locator('[data-frontline-production-order-search-input]').fill(manifestOrder.workOrderCode)
  const option = page.locator('[data-frontline-production-order-option]').filter({
    has: page.locator(`[data-frontline-production-order-option-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await page.locator('[data-frontline-production-order-code]').filter({ hasText: manifestOrder.workOrderCode }).waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-process-current]').filter({ hasNotText: '未选择' }).waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-frontline-production-process-current]:not(:disabled)').waitFor({ state: 'visible', timeout: 30000 })
}

async function selectFrontlineProductionProcess(page, processKey) {
  await page.locator('[data-frontline-production-process-current]').click()
  const option = page.locator(`[data-frontline-production-process-option="${processKey}"]`)
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  await page.locator('[data-frontline-production-process-current]').filter({ hasText: label }).waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-process-current]:not(:disabled)').waitFor({ state: 'visible', timeout: 30000 })
  return label
}

async function selectFrontlineProductionEmployee(page) {
  const employeeCard = page.locator('[data-frontline-production-employee-card]')
  await employeeCard.click()
  const option = page.locator(`[data-frontline-production-employee-option="${productionIdentity.employeeId}"]`)
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  return label
}

async function ensureProductionSession(page, manifestOrder, processKey) {
  if (!(await isLoginPage(page))) return false
  await login(page)
  await selectFrontlineProductionOrder(page, manifestOrder)
  await selectFrontlineProductionProcess(page, processKey)
  return true
}

async function fillProductionQuantityForAllMaterials(page, quantity, quantityMode) {
  const materialTabs = page.locator('[data-frontline-production-material-tab]')
  const tabCount = await materialTabs.count()
  if (quantityMode === 'PROCESS_QUANTITY') {
    assert.equal(tabCount, 0, '工序数量模式不应出现物料页签')
    await page.locator('[data-production-output-quantity]').fill(String(quantity))
    return { materialTabCount: 0 }
  }
  assert.ok(tabCount > 0, '当前工序未显示输出物料页签')
  for (let index = 0; index < tabCount; index += 1) {
    await materialTabs.nth(index).click()
    await page.locator('[data-production-output-quantity]').fill(String(quantity))
  }
  return { materialTabCount: tabCount }
}

async function confirmClearanceChecks(page) {
  const checkboxes = page.locator('[data-production-clearance-checkbox]')
  const count = await checkboxes.count()
  for (let index = 0; index < count; index += 1) {
    const checkbox = checkboxes.nth(index)
    if (!(await checkbox.isChecked())) await checkbox.click()
  }
  return { clearanceCount: count }
}

async function submitOneProductionReport(page, manifestOrder, step, sessionRecoveryAttempted = false) {
  await selectFrontlineProductionOrder(page, manifestOrder)
  const processLabel = await selectFrontlineProductionProcess(page, step.processKey)
  assert.deepEqual((await page.locator('[data-frontline-production-material-tab]').allTextContents()).map(text => text.trim()), step.outputMaterials, '输出物料与冻结基线不一致')
  const employeeLabel = await selectFrontlineProductionEmployee(page)
  const quantityScope = await fillProductionQuantityForAllMaterials(page, step.quantity, step.quantityMode)
  const clearance = await confirmClearanceChecks(page)
  const submitButton = page.locator('[data-production-submit-open-confirmation]')
  if (!(await submitButton.isEnabled().catch(() => false)) && !sessionRecoveryAttempted &&
      await ensureProductionSession(page, manifestOrder, step.processKey)) {
    return submitOneProductionReport(page, manifestOrder, step, true)
  }
  await submitButton.click()
  const dialog = page.locator('[data-production-submit-confirmation-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-production-submit-signature-password]').fill(productionIdentity.signaturePassword)
  const submitResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/feedback/frontline/submit') && r.request().method() === 'POST')
  await dialog.locator('[data-production-submit-confirm-accept]').click()
  const response = await submitResponse
  assert.equal(response.ok(), true, `一线生产提交失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `一线生产提交业务失败：${body.msg || 'unknown'}`)
  await page.locator('[data-production-submit-success-dialog]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-production-submit-success-continue]').click()
  return {
    processIndex: step.processIndex,
    routeProcessId: step.routeProcessId,
    processLabel,
    employeeLabel,
    quantity: step.quantity,
    materialTabCount: quantityScope.materialTabCount,
    clearanceCount: clearance.clearanceCount,
    feedbackId: body.data?.feedbackId,
    processPoolEventId: body.data?.processPoolEventId
  }
}

async function openProductionReportWorkbench(page) {
  await page.goto(`${frontendUrl}/mes/pro/process-pool/production-leader`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByRole('tab', { name: '报工管理' }).click()
  await page.locator('[data-user-table-key="mes.processPool.teamLeader.submissions"]').waitFor({ state: 'visible', timeout: 30000 })
}

async function reviewProductionReport(page, manifestOrder, submission) {
  await openProductionReportWorkbench(page)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-team-leader-submission-work-order-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({ has: page.locator(`[data-production-report-allocation-event-id="${requirePositiveIdString(submission.processPoolEventId, '生产提交事件')}"]`) })
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const reviewButton = row.locator('[data-production-report-allocation-event-id]').first()
  const eventId = await reviewButton.getAttribute('data-production-report-allocation-event-id')
  await reviewButton.click()
  const dialog = page.locator('[data-team-leader-review-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-team-leader-review-signature-password]').fill(signaturePassword)
  const previewResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/process-pool/team-leader/submission/allocation/preview-fifo') && r.request().method() === 'POST')
  await dialog.locator('[data-team-leader-fifo-allocation]').click()
  const preview = await previewResponse
  assert.equal(preview.ok(), true, `生产报工FIFO分配预览失败：HTTP ${preview.status()}`)
  const previewBody = await preview.json()
  assert.equal(Number(previewBody.code), 0, `生产报工FIFO分配预览业务失败：${previewBody.msg || 'unknown'}`)
  await dialog.locator('[data-team-leader-allocation-summary]').filter({ hasText: '未分配：0' }).waitFor({ state: 'visible', timeout: 30000 })
  const reviewResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/process-pool/team-leader/submission/allocation/confirm') && r.request().method() === 'POST')
  await dialog.locator('[data-team-leader-review-submit]').click()
  const response = await reviewResponse
  assert.equal(response.ok(), true, `生产报工复核失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `生产报工复核业务失败：${body.msg || 'unknown'}`)
  return { eventId: requirePositiveIdString(eventId, '生产报工复核eventId'), reviewStatus: 'APPROVED', allocationMode: 'FIFO' }
}

async function selectFrontlinePqcOrder(page, manifestOrder) {
  const pqcUrl = `${frontendUrl}/mes/pro/feedback/edhr-batch-pqc-fill`
  await page.goto(pqcUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  if (await isLoginPage(page)) {
    await login(page)
    await page.goto(pqcUrl, { waitUntil: 'domcontentloaded', timeout: 60000 })
  }
  await page.locator('[data-frontline-pqc-operator]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-pqc-inspection-tab]').first().waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-pqc-process-current]:not(:disabled)').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-pqc-order-summary-card]').click()
  await page.locator('[data-pqc-order-search-input]').fill(manifestOrder.workOrderCode)
  const option = page.locator('[data-pqc-order-option]').filter({
    has: page.locator(`[data-pqc-order-option-code] strong:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await page.locator('[data-pqc-order-code]').filter({ hasText: manifestOrder.workOrderCode }).waitFor({ state: 'visible', timeout: 30000 })
}

async function selectFrontlinePqcProcess(page, processKey) {
  await page.locator('[data-pqc-process-current]').click()
  const option = page.locator(`[data-pqc-process-option="${processKey}"]`)
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  await page.locator('[data-pqc-process-current]').filter({ hasText: label }).waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-pqc-process-current]:not(:disabled)').waitFor({ state: 'visible', timeout: 30000 })
  return label
}

async function fillPqcQuantity(page, quantity) {
  await page.locator('[data-pqc-inspection-quantity]').fill(String(quantity))
  await page.locator('[data-pqc-scrap-quantity]').fill('0')
}

async function selectFirstPqcEquipmentWhenAvailable(page) {
  const selects = page.locator('[data-pqc-equipment-select]')
  const count = await selects.count()
  for (let index = 0; index < count; index += 1) {
    const select = selects.nth(index)
    if (!(await select.isVisible())) continue
    const currentValue = await select.inputValue()
    if (currentValue) continue
    const optionValue = await select.locator('option').evaluateAll((options) => {
      const option = options.find((item) => item.value && item.value !== '__OTHER__')
      return option ? option.value : ''
    })
    assert.ok(optionValue, '检验设备控件缺少有效设备')
    await select.selectOption(optionValue)
  }
}

async function fillActivePqcInspectionItem(page, itemValue) {
  await selectFirstPqcEquipmentWhenAvailable(page)
  const bulkPass = page.locator('[data-pqc-bulk-pass]').first()
  if (await bulkPass.isVisible()) {
    await bulkPass.click()
    return { inputMode: 'choice', value: '合格' }
  }
  assert.ok(Number.isFinite(itemValue), '数值检验项目缺少固定合格值')
  await page.locator('[data-pqc-piece-open-button]').click()
  const modal = page.locator('[data-pqc-piece-modal]')
  await modal.waitFor({ state: 'visible', timeout: 30000 })
  const inputs = modal.locator('[data-pqc-piece-number-input]')
  const inputCount = await inputs.count()
  if (!inputCount) {
    throw stageError({
      stage: 'S03',
      errorType: 'UI_ACTION_FAILED',
      action: 'S03填写PQC逐件数值',
      message: 'PQC逐件填写弹框未提供数值输入框',
      expected: { numericInputsVisible: true },
      actual: { numericInputsVisible: false }
    })
  }
  for (let index = 0; index < inputCount; index += 1) {
    await inputs.nth(index).fill(String(itemValue))
  }
  await modal.locator('[data-pqc-piece-confirm]').click()
  await modal.waitFor({ state: 'hidden', timeout: 30000 })
  return { inputMode: 'numeric', value: itemValue, sampleCount: inputCount }
}

async function fillPqcInspectionItems(page, step) {
  const items = []
  for (const task of step.tasks) {
    for (const item of task.inspectionItems) {
      assert.ok(item.itemCode && String(item.itemCode).trim(), `检验项目缺少正式编码: ${item.itemName}`)
      const tab = page.locator(`[data-pqc-inspection-tab][data-pqc-inspection-item-code="${cssAttributeValue(item.itemCode)}"]`)
      await tab.click()
      const ruleTab = page.locator(`[data-pqc-inspection-rule-tab="${cssAttributeValue(task.ruleKey)}"]`)
      await ruleTab.waitFor({ state: 'visible' })
      await ruleTab.click()
      await fillPqcQuantity(page, task.quantity)
      await page.locator('[data-pqc-active-inspection-panel]').waitFor({ state: 'visible' })
      const numeric = ['NUMBER', 'NUMERIC'].includes(String(item.resultType).toUpperCase())
      let value
      if (numeric) {
        const lower = item.standardLowerLimit == null ? null : Number(item.standardLowerLimit)
        const upper = item.standardUpperLimit == null ? null : Number(item.standardUpperLimit)
        assert.ok((lower !== null && Number.isFinite(lower)) || (upper !== null && Number.isFinite(upper)), `数值项目无可判定合格值: ${item.itemCode}`)
        value = lower !== null ? lower : upper
        assert.ok(upper === null || value <= upper, `检验上下限矛盾: ${item.itemCode}`)
      }
      const filled = await fillActivePqcInspectionItem(page, value)
      if (numeric) assert.equal(filled.sampleCount, Number(task.quantity), '逐件输入数量必须等于冻结应检数量')
      items.push({ pqcTaskId: task.pqcTaskId, itemCode: item.itemCode, ...filled })
    }
  }
  return items
}

async function submitOnePqcInspectionRound(page, manifestOrder, step, sessionRecoveryAttempted = false) {
  let items = await fillPqcInspectionItems(page, step)
  const submitButton = page.locator('[data-pqc-submit-open-signature]')
  if (!(await submitButton.isEnabled().catch(() => false)) &&
      await ensurePqcSession(page, manifestOrder, step.processKey)) {
    items = await fillPqcInspectionItems(page, step)
  }
  await submitButton.click()
  const dialog = page.locator('[data-pqc-signature-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-pqc-signature-password]').fill(signaturePassword)
  // Each configured task posts its own receipt; the leader page reviews the formal group once.
  const receipts = Promise.all(step.tasks.map(async task => {
    const response = await page.waitForResponse(r => r.url().includes('/mes/pro/feedback/frontline/device-account/pqc/submit') &&
      r.request().method() === 'POST' && String(r.request().postDataJSON().pqcTaskId) === task.pqcTaskId)
    assert.equal(response.ok(), true, `PQC提交 ${task.pqcTaskId}: HTTP ${response.status()}`)
    const body = await response.json()
    return { task, body }
  }))
  const responseResults = await Promise.all([receipts, dialog.locator('[data-pqc-submit-confirm-accept]').click()])
    .then(([results]) => results)
  const unauthorized = responseResults.find(({ body }) => Number(body.code) === 401)
  if (unauthorized && !sessionRecoveryAttempted) {
    await page.goto(`${frontendUrl}/login`, { waitUntil: 'commit', timeout: 60000 })
    await ensurePqcSession(page, manifestOrder, step.processKey)
    return submitOnePqcInspectionRound(page, manifestOrder, step, true)
  }
  const submissions = responseResults.map(({ task, body }) => {
    assert.notEqual(Number(body.code), 401, `PQC提交 ${task.pqcTaskId}: 登录会话恢复后仍未认证`)
    assert.equal(Number(body.code), 0, `PQC提交 ${task.pqcTaskId}: ${body.msg || 'unknown'}`)
    assert.ok(body.data != null, `PQC提交 ${task.pqcTaskId}: missing data`)
    const data = body.data
    assert.equal(String(data.pqcTaskId), task.pqcTaskId)
    assert.equal(data.inspectionResult, 'SUCCESS', `PQC检验不合格: ${task.pqcTaskId}`)
    const submitSourceEventId = requirePositiveIdString(data.sourceRevision, `PQC提交 ${task.pqcTaskId} 来源事件`)
    return { ...data, processKey: step.processKey, processLabel: step.processLabel, ruleKey: task.ruleKey, formalIdentity: task.formalIdentity,
      submitSourceEventId, quantity: task.quantity, items: items.filter(item => item.pqcTaskId === task.pqcTaskId), workOrderCode: manifestOrder.workOrderCode }
  })
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return submissions
}

async function openPqcReviewWorkbench(page) {
  await page.goto(`${frontendUrl}/mes/pro/process-pool/pqc-leader`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-pqc-leader-workbench-page]').waitFor({ state: 'visible', timeout: 30000 })
  const managementTab = page.locator('[data-pqc-leader-module-tab-management]').first()
  if (await managementTab.isVisible().catch(() => false)) await managementTab.click()
  await page.locator('[data-user-table-key="mes.processPool.teamLeader.submissions"]').waitFor({ state: 'visible', timeout: 30000 })
}

async function reviewPqcInspectionSubmission(page, manifestOrder, submission, sessionRecoveryAttempted = false) {
  if (await isLoginPage(page)) {
    await login(page)
  }
  await openPqcReviewWorkbench(page)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-pqc-leader-work-order]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({
    has: page.locator('[data-team-leader-review-event-id]')
  })
  await row.first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await row.count(), 1, '当前工单下待复核PQC提交必须唯一，禁止误审其他PQC提交')
  const reviewButton = row.locator('[data-team-leader-review-event-id]').first()
  const eventId = await reviewButton.getAttribute('data-team-leader-review-event-id')
  await reviewButton.click()
  const dialog = page.locator('[data-team-leader-review-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-team-leader-review-signature-password]').fill(signaturePassword)
  const reviewResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/process-pool/team-leader/submission/review') && r.request().method() === 'POST')
  await dialog.locator('[data-team-leader-review-submit]').click()
  const response = await reviewResponse
  assert.equal(response.ok(), true, `PQC组长复核失败：HTTP ${response.status()}`)
  const body = await response.json()
  if (Number(body.code) === 401 && !sessionRecoveryAttempted) {
    await login(page)
    return reviewPqcInspectionSubmission(page, manifestOrder, submission, true)
  }
  assert.equal(Number(body.code), 0, `PQC组长复核业务失败：${body.msg || 'unknown'}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return { eventId: requirePositiveIdString(eventId, 'PQC组长复核eventId'), reviewStatus: 'APPROVED' }
}

async function readPageResponse(response, label) {
  assert.equal(response.ok(), true, `${label}: HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `${label}: ${body.msg}`)
  assert.ok(body.data != null, `${label}: missing data`)
  return body.data
}

async function readOrderDetailFromPage(page, order) {
  await openActiveOrderPool(page, order.workOrderCode)
  const response = page.waitForResponse(r => r.url().includes('/team-leader/active-order/detail?') &&
    new URL(r.url()).searchParams.get('activeOrderId') === String(order.activeOrderId))
  await activeOrderRow(page, order.workOrderCode).locator('[data-team-leader-active-order-detail]').click()
  const detail = await readPageResponse(await response, '活跃订单详情')
  await page.locator('[data-team-leader-active-order-detail-page]').waitFor({ state: 'visible' })
  assert.equal(String(detail.activeOrderId), String(order.activeOrderId))
  assert.equal(detail.workOrderCode, order.workOrderCode)
  return detail
}

async function readPqcProcessesFromPage(page, order) {
  const response = page.waitForResponse(r => r.url().includes('/pqc/active-order/processes?') &&
    new URL(r.url()).searchParams.get('activeOrderId') === String(order.activeOrderId))
  await selectFrontlinePqcOrder(page, order)
  const processes = await readPageResponse(await response, 'PQC工序')
  assert.ok(Array.isArray(processes), 'PQC processes must be array')
  for (const process of processes) assert.equal(String(process.activeOrderId), String(order.activeOrderId))
  return processes
}

function pqcInspectionContract(items) {
  // Last-used device is execution history, not the frozen acceptance standard.
  return items.map(({ lastSelectedEquipmentId, lastSelectedEquipmentNumber, ...contract }) => contract)
}

function cssAttributeValue(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/"/g, '\\"')
}

function toPqcTaskSnapshot(processKey, task) {
  const snapshot = {
    pqcTaskId: String(task.pqcTaskId), ruleKey: task.inspectionRuleKey || task.ruleKey, type: task.inspectionType || task.type,
    businessDate: task.businessDate, shiftCode: task.shiftCode, roundNo: task.roundNo,
    quantity: task.plannedInspectionQuantity == null ? task.quantity : task.plannedInspectionQuantity,
    ruleSort: task.ruleSort, taskStatus: task.taskStatus, inspectionItems: pqcInspectionContract(task.inspectionItems)
  }
  return { ...snapshot, processKey, formalIdentity: pqcTaskFormalIdentity(processKey, snapshot) }
}

async function freezeActiveOrderExecutionBaseline(page, order) {
  const detail = await readOrderDetailFromPage(page, order)
  const runtimeConfigurations = new Map()
  const captures = []
  const captureRuntime = response => {
    if (!response.url().includes('/device-account/runtime-config')) return
    // Retain errors for the synchronous verification below, avoiding unhandled rejections.
    captures.push(readPageResponse(response, '生产运行配置').then(data => {
      runtimeConfigurations.set(String(data.routeProcessId), data)
      return { data }
    }, error => ({ error })))
  }
  page.on('response', captureRuntime)
  const productionProcesses = []
  try {
  await selectFrontlineProductionOrder(page, order)
  for (const process of detail.processes) {
    const processKey = `MES-${order.activeOrderId}-${order.routeId}-${process.routeProcessId}-${process.processId}`
    const processLabel = await selectFrontlineProductionProcess(page, processKey)
    await page.locator('[data-production-output-quantity]').waitFor({ state: 'visible' })
    const outputMaterials = await page.locator('[data-frontline-production-material-tab]').allTextContents()
    for (const capture of await Promise.all(captures)) if (capture.error) throw capture.error
    const runtime = runtimeConfigurations.get(String(process.routeProcessId))
    assert.ok(runtime, `未捕获工序正式运行配置: ${processKey}`)
    assert.ok(Array.isArray(runtime.materials), '正式运行配置缺少materials')
    assert.equal(runtime.materials.length, outputMaterials.length, '正式物料与页面页签数量不一致')
    const quantityMode = runtime.materials.length === 0 ? 'PROCESS_QUANTITY' : 'MATERIAL_QUANTITY'
    productionProcesses.push({ processKey, processLabel, routeProcessId: String(process.routeProcessId),
      processId: String(process.processId), targetQuantity: process.requiredQuantity,
      outputMaterials: outputMaterials.map(text => text.trim()), quantityMode })
  }
  } finally { page.off('response', captureRuntime) }
  const processes = await readPqcProcessesFromPage(page, order)
  const pqcProcesses = []
  for (const process of processes) {
    const processKey = `QA-${process.regulationVersionId}-${process.qaProcessId}`
    const processLabel = await selectFrontlinePqcProcess(page, processKey)
    pqcProcesses.push({ processKey, processLabel, tasks: process.pqcTaskOptions.map(task => toPqcTaskSnapshot(processKey, task)) })
  }
  return freezeExecutionBaseline(order, productionProcesses, pqcProcesses)
}

async function discoverPendingPqcTasksForOrder(page, order, baseline, productionProcess) {
  const processes = await readPqcProcessesFromPage(page, order)
  const executableProcesses = []
  const frozenByIdentity = new Map(baseline.pqcTasks.map(task => [task.formalIdentity, task]))
  for (const process of processes) {
    const processKey = `QA-${process.regulationVersionId}-${process.qaProcessId}`
    const runtimeTasks = []
    for (const task of process.pqcTaskOptions) {
      const current = toPqcTaskSnapshot(processKey, task)
      const frozen = frozenByIdentity.get(current.formalIdentity)
      assert.ok(frozen, `发现未冻结PQC任务身份: ${current.formalIdentity}`)
      assert.equal(frozen.processKey, processKey, 'PQC工序身份变化')
      assert.equal(Number(current.quantity), Number(frozen.quantity), '应检数量变化')
      assert.deepEqual(current.inspectionItems, frozen.inspectionItems, '检验项目或标准变化')
      assert.equal(current.ruleKey, frozen.ruleKey, '检验类型变化')
      assert.deepEqual([current.businessDate, current.shiftCode, current.roundNo], [frozen.businessDate, frozen.shiftCode, frozen.roundNo], '检验轮次变化')
      if (current.taskStatus === 'PENDING') runtimeTasks.push(current)
    }
    if ((!productionProcess || process.productionSubmitCandidates.some(candidate =>
      String(candidate.routeProcessId) === String(productionProcess.routeProcessId))) && runtimeTasks.length > 0) {
      executableProcesses.push({ processKey, processLabel: process.processName || process.qaProcessName || processKey, tasks: runtimeTasks })
    }
  }
  const executable = buildPqcTaskGroups(executableProcesses)
  return executable.groups
}

async function submitOnePqcInspectionForProcess(page, manifestOrder, step) {
  await selectFrontlinePqcOrder(page, manifestOrder)
  const processLabel = await selectFrontlinePqcProcess(page, step.processKey)
  return submitOnePqcInspectionRound(page, manifestOrder, { ...step, processLabel })
}

async function executeProductionAndPqcInterleaved(page, manifestOrder, activeOrder) {
  const baseline = activeOrder.executionBaseline
  assert.ok(baseline, '本轮执行基线未冻结')
  const production = { activeOrderId: activeOrder.activeOrderId, expectedSubmissionCount: baseline.expected.productionFeedbackCount, submissions: [], reviews: [] }
  const pqc = { activeOrderId: activeOrder.activeOrderId, expectedTaskCount: baseline.expected.pqcTaskCount, submissions: [], reviews: [] }
  const executedSteps = []
  const executePending = async (process) => {
    const groups = await runWithStage('S03', '发现待检任务', { workOrderCode: manifestOrder.workOrderCode, process },
      () => discoverPendingPqcTasksForOrder(page, manifestOrder, baseline, process))
    for (const step of groups) {
      await runWithStage('S03', 'PQC填写及复核', { workOrderCode: manifestOrder.workOrderCode, ...step }, async () => {
        assert.ok(step.tasks.every(task => !pqc.submissions.some(s => String(s.pqcTaskId) === task.pqcTaskId)), '已提交任务仍待检/部分提交，禁止重复写入')
        const submissions = await submitOnePqcInspectionForProcess(page, manifestOrder, step)
        for (const submission of submissions) {
          pqc.submissions.push(submission)
        }
        pqc.reviews.push(await reviewPqcInspectionSubmission(page, manifestOrder, {
          ...submissions[0],
          groupedPqcTaskIds: submissions.map(submission => String(submission.pqcTaskId))
        }))
        executedSteps.push({ kind: 'PQC', processKey: step.processKey, taskIds: step.tasks.map(t => t.pqcTaskId) })
      })
    }
  }
  for (const step of baseline.productionProcesses) {
    await runWithStage('S02', '生产填写及FIFO复核', { workOrderCode: manifestOrder.workOrderCode, ...step }, async () => {
      const submission = await submitOneProductionReport(page, manifestOrder, step)
      production.submissions.push(submission)
      production.reviews.push(await reviewProductionReport(page, manifestOrder, submission))
      executedSteps.push({ kind: 'PRODUCTION', routeProcessId: step.routeProcessId, quantity: step.quantity })
    })
    await executePending(step)
  }
  await executePending(null) // 全订单尾扫，包含独立QA工序与延后可执行任务。
  await runWithStage('S03', '冻结任务覆盖核验', baseline.expected, async () => {
    assertCoverage(baseline, production.submissions.map(s => s.routeProcessId), pqc.submissions)
    assert.equal(production.reviews.length, baseline.expected.productionReviewCount)
    assert.equal(pqc.reviews.length, baseline.expected.pqcReviewCount)
  })
  const progress = await verifyExecutionProgress(page, manifestOrder)
  return { progress, activeOrderId: activeOrder.activeOrderId, interleavedStepCount: executedSteps.length, executedSteps, production,
    pqc: { ...pqc, pqcSubmissionCount: pqc.submissions.length, pqcReviewCount: pqc.reviews.length } }
}

async function verifyExecutionProgress(page, order) {
  const rows = await openActiveOrderPool(page, order.workOrderCode)
  const raw = findActiveOrderDataByCode(rows, order.workOrderCode)
  const status = await readActiveOrderProgressAndStatus(activeOrderRow(page, order.workOrderCode))
  try {
    assertDouble100(status)
    assert.equal(Number(raw?.productionProgressPercent), 100, '生产原始进度未到100，禁止使用页面四舍五入值')
    assert.equal(Number(raw?.inspectionProgressPercent), 100, '检验原始进度未到100，禁止使用页面四舍五入值')
  } catch (error) {
    const detail = await readOrderDetailFromPage(page, order)
    throw stageError({ stage: 'S04', errorType: 'BUSINESS_ASSERTION', action: 'S04_PRECHECK', message: error.message,
      expected: order.executionBaseline.expected, actual: { workOrderCode: order.workOrderCode, activeOrderId: order.activeOrderId, ...status, rawProgress: { production: raw?.productionProgressPercent, inspection: raw?.inspectionProgressPercent }, processes: detail.processes } })
  }
  return status
}

function isProductionPickListSyncResponse(response) {
  return response.url().includes('/erp/kingdee-sync/incremental-sync') &&
    response.request().method() === 'POST' &&
    String(response.request().postData() || '').includes('PRODUCTION_PICK_LIST')
}

function isActiveOrderReleaseApplyResponse(response) {
  return response.url().includes('/mes/pro/process-pool/team-leader/active-order/release/apply') &&
    response.request().method() === 'POST'
}

async function readCommonResult(response, failurePrefix) {
  assert.equal(response.ok(), true, `${failurePrefix}：HTTP ${response.status()}`)
  return response.json()
}

async function triggerProductionPickListSync(page, manifestOrder) {
  await page.goto(`${frontendUrl}/erp/production/pick-list`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-user-table-key="erp.production.pickList.main"]').waitFor({ state: 'visible', timeout: 30000 })
  const syncResponse = page.waitForResponse(isProductionPickListSyncResponse)
  await page.locator('[data-production-pick-list-sync-kingdee]').click()
  const body = await readCommonResult(await syncResponse, '生产领料单同步提交失败')
  assert.equal(Number(body.code), 0, `生产领料单同步提交业务失败：${body.msg || 'unknown'}`)
  return {
    workOrderCode: manifestOrder.workOrderCode,
    expectedPickListSource: 'ERP-generated formal production pick-list linked to the current work order',
    syncType: 'PRODUCTION_PICK_LIST',
    syncReceipt: body.data
  }
}

async function clickMessageBoxButton(page, title, buttonName) {
  const box = page.locator('.el-message-box').filter({ hasText: title }).last()
  await box.waitFor({ state: 'visible', timeout: 30000 })
  await box.getByRole('button', { name: buttonName, exact: true }).click()
  await box.waitFor({ state: 'hidden', timeout: 30000 })
}

async function readActiveOrderProgressAndStatus(row) {
  const productionProgressText = (await row.locator('[data-team-leader-active-order-production-progress]').first().innerText()).trim()
  const inspectionProgressText = (await row.locator('[data-team-leader-active-order-inspection-progress]').first().innerText()).trim()
  const releaseStatusText = (await row.locator('[data-team-leader-active-order-release-status]').first().innerText()).trim()
  return { productionProgressText, inspectionProgressText, releaseStatusText }
}

function requireReleaseReadyProgress(status, manifestOrder, action) {
  if (!/^100(?:\.0+)?\s*%$/.test(status.productionProgressText.trim()) || !/^100(?:\.0+)?\s*%$/.test(status.inspectionProgressText.trim())) {
    throw stageError({
      stage: 'S04',
      errorType: 'PRECONDITION_BLOCKED',
      action,
      message: `订单未达到完工申请前置进度：${manifestOrder.workOrderCode}`,
      expected: { productionProgress: '100%', inspectionProgress: '100%' },
      actual: status
    })
  }
}

function requireNoReplenishmentConfirmationResponse(body, manifestOrder, action) {
  if (Number(body.code) === 0) {
    throw stageError({
      stage: 'S04',
      errorType: 'BUSINESS_ASSERTION',
      action,
      message: `无补料确认前不应生成生产放行申请：${manifestOrder.workOrderCode}`,
      expected: { firstReleaseApplyResult: 'NO_REPLENISHMENT_CONFIRMATION_REQUIRED' },
      actual: { code: body.code, data: body.data }
    })
  }
  const serialized = JSON.stringify(body)
  if (!serialized.includes('NO_REPLENISHMENT_CONFIRMATION_REQUIRED')) {
    throw stageError({
      stage: 'S04',
      errorType: 'BUSINESS_ASSERTION',
      action,
      message: `完工申请未返回无补料确认要求：${manifestOrder.workOrderCode}`,
      expected: { blocker: 'NO_REPLENISHMENT_CONFIRMATION_REQUIRED' },
      actual: { code: body.code, msg: body.msg, data: body.data }
    })
  }
}

async function openReleaseApplyAndConfirmMainPrompt(page, manifestOrder, action) {
  await openActiveOrderPool(page, manifestOrder.workOrderCode)
  const row = activeOrderRow(page, manifestOrder.workOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const status = await readActiveOrderProgressAndStatus(row)
  requireReleaseReadyProgress(status, manifestOrder, action)
  await row.locator('[data-team-leader-active-order-release-apply]').click()
  const firstApplyResponse = page.waitForResponse(isActiveOrderReleaseApplyResponse)
  await clickMessageBoxButton(page, '提交生产放行申请', '申请放行')
  return {
    beforeStatus: status,
    firstApplyBody: await readCommonResult(await firstApplyResponse, '生产完工申请提交失败')
  }
}

async function applyActiveOrderReleaseAndCancelNoReplenishment(page, manifestOrder) {
  const action = 'S04取消无补料确认'
  const attempt = await openReleaseApplyAndConfirmMainPrompt(page, manifestOrder, action)
  requireNoReplenishmentConfirmationResponse(attempt.firstApplyBody, manifestOrder, action)
  await clickMessageBoxButton(page, '确认无补料信息', '取消')
  await openActiveOrderPool(page, manifestOrder.workOrderCode)
  const row = activeOrderRow(page, manifestOrder.workOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const afterStatus = await readActiveOrderProgressAndStatus(row)
  assert.equal(afterStatus.releaseStatusText, '未申请', '取消无补料确认后不应生成放行申请')
  return { cancelled: true, beforeStatus: attempt.beforeStatus, afterStatus }
}

async function applyActiveOrderReleaseWithNoReplenishmentConfirmation(page, manifestOrder) {
  const action = 'S04确认无补料并申请放行'
  const attempt = await openReleaseApplyAndConfirmMainPrompt(page, manifestOrder, action)
  requireNoReplenishmentConfirmationResponse(attempt.firstApplyBody, manifestOrder, action)
  const confirmedApplyResponse = page.waitForResponse(isActiveOrderReleaseApplyResponse)
  await clickMessageBoxButton(page, '确认无补料信息', '确认无补料信息')
  const confirmedBody = await readCommonResult(await confirmedApplyResponse, '确认无补料后的生产完工申请失败')
  assert.equal(Number(confirmedBody.code), 0, `确认无补料后的生产完工申请业务失败：${confirmedBody.msg || 'unknown'}`)
  const receipt = confirmedBody.data || {}
  assert.equal(String(receipt.status), 'PQC_RELEASE_PENDING', `S04完成后状态必须为待PQC放行，实际为${receipt.status}`)
  assert.ok(String(receipt.sourceSnapshotHash || '').trim(), 'S04放行申请回执缺少正式来源快照哈希')
  assert.ok(String(receipt.pqcReleaseWorkTaskId || '').trim(), 'S04放行申请回执缺少PQC放行待办ID')
  await openActiveOrderPool(page, manifestOrder.workOrderCode)
  const row = activeOrderRow(page, manifestOrder.workOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const afterStatus = await readActiveOrderProgressAndStatus(row)
  assert.equal(afterStatus.releaseStatusText, '待PQC放行', 'S04确认后列表状态必须进入待PQC放行')
  return { receipt, beforeStatus: attempt.beforeStatus, afterStatus }
}

function normalizeEvidenceText(text) {
  return String(text || '').replace(/\s+/g, ' ').trim()
}

async function readVisibleEvidenceTexts(locator) {
  return locator.evaluateAll((elements) =>
    elements
      .filter((element) => {
        const style = window.getComputedStyle(element)
        const rect = element.getBoundingClientRect()
        return style.visibility !== 'hidden' && style.display !== 'none' && rect.width > 0 && rect.height > 0
      })
      .map((element) => element.textContent || '')
      .map((text) => text.replace(/\s+/g, ' ').trim())
      .filter(Boolean)
  )
}

function filterResolvedInputMaterialEvidence(evidenceTexts) {
  return evidenceTexts.filter((text) => text && text !== '-' && !/未记录/.test(text))
}

async function readInputMaterialBackfillEvidence(page, manifestOrder) {
  await openActiveOrderPool(page, manifestOrder.workOrderCode)
  const row = activeOrderRow(page, manifestOrder.workOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  await row.locator('[data-team-leader-active-order-detail]').click()
  await page.waitForURL((url) =>
    url.pathname.includes('/mes/pro/process-pool/active-order/') &&
    url.pathname.includes('/submission-detail'), { timeout: 60000 })
  await page.locator('[data-team-leader-active-order-detail-page]').waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('tab', { name: '生产提交', exact: true }).click()
  await page.locator('[data-active-order-production-record-input-materials]').first()
    .waitFor({ state: 'visible', timeout: 60000 })

  const pickListEvidenceLocator = page.locator('[data-active-order-production-record-input-material-pick-list]')
  const batchEvidenceLocator = page.locator('[data-active-order-production-record-input-material-batch]')
  await pickListEvidenceLocator.first()
    .waitFor({ state: 'visible', timeout: 60000 })

  const pickListEvidence = (await readVisibleEvidenceTexts(pickListEvidenceLocator)).map(normalizeEvidenceText)
  const batchEvidence = (await readVisibleEvidenceTexts(batchEvidenceLocator)).map(normalizeEvidenceText)
  return {
    workOrderCode: manifestOrder.workOrderCode,
    pickListEvidence,
    batchEvidence,
    resolvedPickListEvidence: filterResolvedInputMaterialEvidence(pickListEvidence),
    resolvedBatchEvidence: filterResolvedInputMaterialEvidence(batchEvidence)
  }
}

async function verifyNoInputMaterialBackfillBeforePickList(page, manifestOrder) {
  const evidence = await readInputMaterialBackfillEvidence(page, manifestOrder)
  if (evidence.resolvedPickListEvidence.length || evidence.resolvedBatchEvidence.length) {
    throw stageError({
      stage: 'S04',
      errorType: 'TRACEABILITY_FAILURE',
      action: 'S04验证领料同步前输入物料未回填',
      message: `领料同步前输入物料已经显示正式来源，不能证明领料晚到时序：${manifestOrder.workOrderCode}`,
      expected: { beforePickListSync: { inputMaterialPickListEvidence: [], inputMaterialBatchEvidence: [] } },
      actual: {
        pickListEvidence: evidence.pickListEvidence,
        batchEvidence: evidence.batchEvidence
      }
    })
  }
  return {
    workOrderCode: manifestOrder.workOrderCode,
    beforePickListSyncPickListEvidence: evidence.pickListEvidence,
    beforePickListSyncBatchEvidence: evidence.batchEvidence
  }
}

async function verifyCompletionInputMaterialBackfill(page, manifestOrder) {
  const evidence = await readInputMaterialBackfillEvidence(page, manifestOrder)

  if (!evidence.resolvedPickListEvidence.length) {
    throw stageError({
      stage: 'S04',
      errorType: 'TRACEABILITY_FAILURE',
      action: 'S04验证输入物料领料单回填',
      message: `完成后输入物料未显示正式领料单号：${manifestOrder.workOrderCode}`,
      expected: { inputMaterialPickListEvidence: 'non-empty formal production pick-list code' },
      actual: { pickListEvidence: evidence.pickListEvidence }
    })
  }
  if (!evidence.resolvedBatchEvidence.length) {
    throw stageError({
      stage: 'S04',
      errorType: 'TRACEABILITY_FAILURE',
      action: 'S04验证输入物料批号回填',
      message: `完成后输入物料未显示正式批号：${manifestOrder.workOrderCode}`,
      expected: { inputMaterialBatchEvidence: 'non-empty formal batch code' },
      actual: { batchEvidence: evidence.batchEvidence }
    })
  }

  return {
    workOrderCode: manifestOrder.workOrderCode,
    inputMaterialPickListEvidence: evidence.resolvedPickListEvidence,
    rawInputMaterialPickListEvidence: evidence.pickListEvidence,
    inputMaterialBatchEvidence: evidence.resolvedBatchEvidence,
    rawInputMaterialBatchEvidence: evidence.batchEvidence
  }
}

async function completeActiveOrderAndApplyRelease(page, manifestOrder) {
  await verifyExecutionProgress(page, manifestOrder)
  const prePickListInputMaterialBackfill = await verifyNoInputMaterialBackfillBeforePickList(page, manifestOrder)
  const pickListSync = await triggerProductionPickListSync(page, manifestOrder)
  const cancelProbe = await applyActiveOrderReleaseAndCancelNoReplenishment(page, manifestOrder)
  const releaseApplication = await applyActiveOrderReleaseWithNoReplenishmentConfirmation(page, manifestOrder)
  const inputMaterialBackfill = await verifyCompletionInputMaterialBackfill(page, manifestOrder)
  return {
    workOrderCode: manifestOrder.workOrderCode,
    prePickListInputMaterialBackfill,
    pickListSync,
    cancelProbe,
    releaseApplication,
    inputMaterialBackfill,
    expectedFormalLoss: 0,
    expectedReleaseStatus: 'PQC_RELEASE_PENDING'
  }
}

function isCandidateWorkTaskPageResponse(response) {
  return response.url().includes('/mes/pro/edhr-work-task/candidate-todo-page') &&
    response.request().method() === 'GET'
}

function isMyWorkTaskPageResponseForWorkOrder(manifestOrder) {
  return (response) => {
    if (!response.url().includes('/mes/pro/edhr-work-task/my-page') ||
      response.request().method() !== 'GET') return false
    const parsed = new URL(response.url())
    return parsed.searchParams.get('workOrderCode') === manifestOrder.workOrderCode
  }
}

function isPqcProductionReleasePageResponse(response) {
  return response.url().includes('/mes/pro/production-release/pqc/page') &&
    response.request().method() === 'GET'
}

function isPqcProductionReleaseApproveResponse(response) {
  return response.url().includes('/mes/pro/production-release/pqc/approve') &&
    response.request().method() === 'POST'
}

async function openCandidateWorkTaskBoard(page, manifestOrder) {
  await page.goto(`${frontendUrl}/mes/pro/feedback/edhr-work-task`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-edhr-work-task-page]').waitFor({ state: 'visible', timeout: 30000 })
  const candidateTab = page.getByRole('tab', { name: '候选审核', exact: true })
  await candidateTab.click()
  await page.locator('[data-edhr-work-task-work-order-filter] input').fill(manifestOrder.workOrderCode)
  const pageResponse = page.waitForResponse(isCandidateWorkTaskPageResponse)
  await page.locator('[data-edhr-work-task-query]').click()
  const body = await readCommonResult(await pageResponse, 'PQC生产放行候选待办查询失败')
  assert.equal(Number(body.code), 0, `PQC生产放行候选待办查询业务失败：${body.msg || 'unknown'}`)
  return body.data || {}
}

async function openMyWorkTaskBoard(page, manifestOrder) {
  await page.goto(`${frontendUrl}/mes/pro/feedback/edhr-work-task`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('[data-edhr-work-task-page]').waitFor({ state: 'visible', timeout: 30000 })
  const myTodoTab = page.getByRole('tab', { name: '我的待办', exact: true })
  await myTodoTab.click()
  await page.locator('[data-edhr-work-task-work-order-filter] input').fill(manifestOrder.workOrderCode)
  const pageResponse = page.waitForResponse(isMyWorkTaskPageResponseForWorkOrder(manifestOrder))
  await page.locator('[data-edhr-work-task-query]').click()
  const body = await readCommonResult(await pageResponse, '最终归档我的待办查询失败')
  assert.equal(Number(body.code), 0, `最终归档我的待办查询业务失败：${body.msg || 'unknown'}`)
  return body.data || {}
}

async function openPqcProductionReleasePageFromWorkTask(page, manifestOrder) {
  const workTaskPage = await openCandidateWorkTaskBoard(page, manifestOrder)
  const row = page.locator('.el-table__row:visible').filter({
    hasText: manifestOrder.workOrderCode
  }).filter({
    has: page.locator('[data-pqc-release-open]')
  }).first()
  if (!(await row.isVisible({ timeout: 30000 }).catch(() => false))) {
    throw stageError({
      stage: 'S05',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S05打开PQC生产放行待办',
      message: `未找到本轮订单的PQC生产放行候选待办：${manifestOrder.workOrderCode}`,
      expected: { workOrderCode: manifestOrder.workOrderCode, taskType: 'PQC_PRODUCTION_RELEASE', status: 'TODO' },
      actual: { workTaskPage }
    })
  }
  await row.locator('[data-pqc-release-open]').click()
  await page.locator('[data-pqc-production-release-page]').waitFor({ state: 'visible', timeout: 60000 })
  return { workTaskPage }
}

async function filterPqcProductionReleasePage(page, manifestOrder) {
  await page.locator('[data-pqc-production-release-work-order-filter] input').fill(manifestOrder.workOrderCode)
  const pageResponse = page.waitForResponse(isPqcProductionReleasePageResponse)
  await page.locator('[data-pqc-production-release-query]').click()
  const body = await readCommonResult(await pageResponse, 'PQC生产放行列表查询失败')
  assert.equal(Number(body.code), 0, `PQC生产放行列表查询业务失败：${body.msg || 'unknown'}`)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-pqc-production-release-work-order-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  if (!(await row.isVisible({ timeout: 30000 }).catch(() => false))) {
    throw stageError({
      stage: 'S05',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S05定位PQC生产放行申请',
      message: `PQC生产放行页面未找到待放行申请：${manifestOrder.workOrderCode}`,
      expected: { workOrderCode: manifestOrder.workOrderCode, applicationStatus: 'PQC_RELEASE_PENDING' },
      actual: { releasePage: body.data || {} }
    })
  }
  return { row, releasePage: body.data || {} }
}

function isNonconformanceReviewCreateResponse(response) {
  return response.url().includes('/mes/pro/edhr-nonconformance-review/create') &&
    response.request().method() === 'POST'
}

function isNonconformanceReviewDisposeResponse(response) {
  return response.url().includes('/mes/pro/edhr-nonconformance-review/dispose') &&
    response.request().method() === 'POST'
}

function createNonconformanceReviewFixture(runDir, manifestOrder) {
  const fixtureDir = path.join(runDir, 'nonconformance-fixture')
  fs.mkdirSync(fixtureDir, { recursive: true })
  const filePath = path.join(fixtureDir, 'pqc-release-nonconformance-review.pdf')
  fs.writeFileSync(filePath, buildMinimalOnePagePdf([
    'AI eDHR nonconformance review',
    `runId ${runId}`,
    `workOrder ${manifestOrder.workOrderCode}`,
    `batch ${manifestOrder.batchCode}`
  ]), 'utf8')
  return filePath
}

async function completePqcReleaseNonconformanceReview(page, manifestOrder, row, runDir) {
  const nonconformanceButton = row.locator('[data-pqc-production-release-nonconformance]')
  const applicationId = String(await nonconformanceButton.getAttribute('data-pqc-production-release-application-id') || '')
  assert.ok(applicationId, 'PQC生产放行行缺少申请ID，无法发起不合格评审')
  await nonconformanceButton.click()
  const reviewPage = page.locator('[data-edhr-ncr-page]')
  await reviewPage.waitFor({ state: 'visible', timeout: 60000 })

  const reason = `AI E2E PQC放行前不合格评审 ${manifestOrder.workOrderCode}`
  await reviewPage.locator('[data-edhr-ncr-create-reason] textarea').fill(reason)
  const createResponse = page.waitForResponse(isNonconformanceReviewCreateResponse)
  await reviewPage.locator('[data-edhr-ncr-create-submit]').click()
  const createBody = await readCommonResult(await createResponse, '不合格评审创建失败')
  assert.equal(Number(createBody.code), 0, `不合格评审创建业务失败：${createBody.msg || 'unknown'}`)
  const created = createBody.data || {}
  assert.ok(String(created.id || '').trim(), '不合格评审创建回执缺少评审ID')
  assert.equal(String(created.activeOrderId), String(manifestOrder.activeOrderId), '不合格评审未归属当前活跃订单')
  assert.equal(String(created.sourceId), applicationId, '不合格评审未绑定当前PQC放行申请')
  assert.equal(created.reviewStatus, 'pending_review', '新建不合格评审必须为待评审')

  const material = reviewPage.locator('[data-edhr-ncr-review-material]')
  await material.waitFor({ state: 'visible', timeout: 30000 })
  const fixturePath = createNonconformanceReviewFixture(runDir, manifestOrder)
  await material.locator('input[type="file"]').setInputFiles(fixturePath)
  await material.locator('.el-upload-list__item').first().waitFor({ state: 'visible', timeout: 60000 })
  const opinion = `AI E2E QA评审确认让步放行 ${manifestOrder.workOrderCode}`
  await reviewPage.locator('[data-edhr-ncr-review-opinion] textarea').fill(opinion)
  await reviewPage.locator('[data-edhr-ncr-signature-password] input').fill(signaturePassword)
  const disposeResponse = page.waitForResponse(isNonconformanceReviewDisposeResponse)
  await reviewPage.locator('[data-edhr-ncr-concession-release]').click()
  const disposeBody = await readCommonResult(await disposeResponse, '不合格评审让步放行失败')
  assert.equal(Number(disposeBody.code), 0, `不合格评审让步放行业务失败：${disposeBody.msg || 'unknown'}`)
  const disposed = disposeBody.data || {}
  assert.equal(String(disposed.id), String(created.id), '不合格评审处置回执评审ID变化')
  assert.equal(String(disposed.activeOrderId), String(manifestOrder.activeOrderId), '处置事实未归属当前活跃订单')
  assert.equal(disposed.reviewStatus, 'closed', '不合格评审让步放行后必须关闭')
  assert.equal(disposed.disposition, 'concession_release', '不合格评审处置结果必须为让步放行')
  assert.ok(String(disposed.qaSignature || '').trim(), '不合格评审处置缺少QA电子签名')
  assert.ok(String(disposed.reviewMaterialUrl || '').trim(), '不合格评审处置缺少正式评审材料')
  await reviewPage.locator('[data-edhr-ncr-disposition-result]').filter({ hasText: '让步放行' }).waitFor({ state: 'visible', timeout: 30000 })
  await reviewPage.locator('[data-edhr-ncr-qa-signature]').filter({ hasText: /QA电子签名#[1-9]\d*/ }).waitFor({ state: 'visible', timeout: 30000 })

  await page.goBack({ waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-pqc-production-release-page]').waitFor({ state: 'visible', timeout: 60000 })
  return {
    reviewId: disposed.id,
    reviewCode: disposed.reviewCode,
    activeOrderId: disposed.activeOrderId,
    sourceType: disposed.sourceType,
    sourceId: disposed.sourceId,
    reviewStatus: disposed.reviewStatus,
    disposition: disposed.disposition,
    nonconformanceReason: disposed.nonconformanceReason,
    reviewOpinion: disposed.reviewOpinion,
    reviewMaterialUrl: disposed.reviewMaterialUrl,
    qaSignature: disposed.qaSignature,
    closedAt: disposed.closedAt,
    unfrozenAt: disposed.unfrozenAt
  }
}

function assertPqcReleaseApprovedReceipt(receipt, manifestOrder) {
  assert.equal(receipt.decision, 'APPROVE', 'S05回执必须为PQC通过')
  assert.equal(receipt.status, 'REPORT_UPLOAD_PENDING', `S05后状态必须进入报告上传，实际为${receipt.status}`)
  assert.ok(String(receipt.batchExecutionId || '').trim(), 'S05回执缺少批次执行ID')
  assert.ok(String(receipt.signatureId || '').trim(), 'S05回执缺少PQC放行签名ID')
  assert.ok(Array.isArray(receipt.batchRecordEvidenceIds) && receipt.batchRecordEvidenceIds.length > 0, 'S05回执缺少批记录证据')
  assert.ok(Array.isArray(receipt.processInspectionEvidenceIds) && receipt.processInspectionEvidenceIds.length > 0, 'S05回执缺少过程检验证据')
  assert.ok(Array.isArray(receipt.lossReportEvidenceIds), 'S05回执缺少损耗证据数组')
  assert.ok(Array.isArray(receipt.reportUploadTasks), 'S05回执缺少报告上传任务数组')
  assert.equal(receipt.reportUploadTasks.length, 4, `S05必须派发四个报告上传任务：${manifestOrder.workOrderCode}`)
  const nodeTypes = receipt.reportUploadTasks.map((task) => task.nodeType).sort()
  assert.deepEqual(nodeTypes, [
    'FINISHED_PRODUCT_INSPECTION_RECORD',
    'FINISHED_PRODUCT_INSPECTION_REPORT',
    'INCOMING_INSPECTION_REPORT',
    'STERILIZATION_REPORT'
  ])
  for (const task of receipt.reportUploadTasks) {
    assert.ok(String(task.batchTaskId || '').trim(), `${task.nodeType}缺少批次任务ID`)
    assert.ok(String(task.workTaskId || '').trim(), `${task.nodeType}缺少工作待办ID`)
    assert.equal(task.status, 'TODO', `${task.nodeType}上传任务初始状态必须为TODO`)
  }
}

async function approvePqcProductionReleaseS05(page, manifestOrder, completion, runDir) {
  await openPqcProductionReleasePageFromWorkTask(page, manifestOrder)
  const initial = await filterPqcProductionReleasePage(page, manifestOrder)
  const nonconformanceReview = await completePqcReleaseNonconformanceReview(
    page, manifestOrder, initial.row, runDir)
  const { row, releasePage } = await filterPqcProductionReleasePage(page, manifestOrder)
  const approveButton = row.locator('[data-pqc-production-release-approve]').first()
  if (!(await approveButton.isEnabled().catch(() => false))) {
    throw stageError({
      stage: 'S05',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S05提交PQC生产放行',
      message: `PQC生产放行按钮不可用：${manifestOrder.workOrderCode}`,
      expected: { approvalReady: true, applicationStatus: 'PQC_RELEASE_PENDING' },
      actual: { releasePage }
    })
  }
  await approveButton.click()
  const dialog = page.locator('[data-pqc-production-release-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-pqc-production-release-signature-password] input').fill(signaturePassword)
  await dialog.locator('[data-pqc-production-release-approval-opinion] textarea').fill(`AI E2E S05 PQC生产放行通过 ${manifestOrder.workOrderCode}`)
  const approveResponse = page.waitForResponse(isPqcProductionReleaseApproveResponse)
  await dialog.locator('[data-pqc-production-release-confirm]').click()
  const body = await readCommonResult(await approveResponse, 'PQC生产放行提交失败')
  assert.equal(Number(body.code), 0, `PQC生产放行提交业务失败：${body.msg || 'unknown'}`)
  const receipt = body.data || {}
  assertPqcReleaseApprovedReceipt(receipt, manifestOrder)
  await dialog.locator('[data-pqc-production-release-batch-execution-id]').filter({
    hasText: String(receipt.batchExecutionId)
  }).waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-pqc-production-release-report-task-list]').waitFor({ state: 'visible', timeout: 30000 })
  return {
    workOrderCode: manifestOrder.workOrderCode,
    applicationId: receipt.applicationId || completion.releaseApplication.receipt.applicationId,
    pqcReleaseWorkTaskId: receipt.pqcReleaseWorkTaskId || completion.releaseApplication.receipt.pqcReleaseWorkTaskId,
    status: receipt.status,
    batchExecutionId: receipt.batchExecutionId,
    signatureId: receipt.signatureId,
    reportUploadTaskCount: receipt.reportUploadTasks.length,
    reportUploadTasks: receipt.reportUploadTasks,
    nonconformanceReview,
    sourceSnapshotHash: receipt.sourceSnapshotHash,
    reportSnapshotHash: receipt.reportSnapshotHash
  }
}

function escapePdfText(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)')
}

function buildMinimalOnePagePdf(lines) {
  const textOperators = lines
    .map((line, index) => `${index === 0 ? '' : '0 -16 Td\n'}(${escapePdfText(line)}) Tj`)
    .join('\n')
  const stream = `BT\n/F1 12 Tf\n72 720 Td\n${textOperators}\nET\n`
  const objects = [
    '1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n',
    '2 0 obj\n<< /Type /Pages /Count 1 /Kids [3 0 R] >>\nendobj\n',
    '3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>\nendobj\n',
    `4 0 obj\n<< /Length ${Buffer.byteLength(stream, 'utf8')} >>\nstream\n${stream}endstream\nendobj\n`,
    '5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n'
  ]
  let content = '%PDF-1.4\n'
  const offsets = [0]
  for (const object of objects) {
    offsets.push(Buffer.byteLength(content, 'utf8'))
    content += object
  }
  const xrefOffset = Buffer.byteLength(content, 'utf8')
  content += `xref\n0 ${objects.length + 1}\n`
  content += '0000000000 65535 f \n'
  for (const offset of offsets.slice(1)) {
    content += `${String(offset).padStart(10, '0')} 00000 n \n`
  }
  content += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xrefOffset}\n%%EOF\n`
  return content
}

function createReportFixtureFiles(runDir, manifestOrder) {
  const fixtureDir = path.join(runDir, 'report-fixtures')
  fs.mkdirSync(fixtureDir, { recursive: true })
  const files = new Map()
  for (const item of REPORT_UPLOAD_PLAN) {
    const filePath = path.join(fixtureDir, item.fileName)
    const content = buildMinimalOnePagePdf([
      `AI eDHR ${item.label}`,
      `nodeType ${item.nodeType}`,
      `runId ${runId}`,
      `workOrder ${manifestOrder.workOrderCode}`,
      `batch ${manifestOrder.batchCode}`
    ])
    fs.writeFileSync(filePath, content, 'utf8')
    const sha256 = crypto.createHash('sha256').update(fs.readFileSync(filePath)).digest('hex')
    files.set(item.nodeType, { ...item, filePath, sha256 })
  }
  return files
}

function releaseReportRow(page, manifestOrder, nodeType) {
  const plan = REPORT_UPLOAD_PLAN.find((item) => item.nodeType === nodeType)
  const localizedLabel = plan?.label || nodeType
  const rows = page.locator('.el-table__row:visible').filter({ hasText: manifestOrder.workOrderCode })
  return rows.filter({ hasText: nodeType }).first().or(rows.filter({ hasText: localizedLabel }).first())
}

async function openReportUploadTaskFromWorkTask(page, manifestOrder, task) {
  await openCandidateWorkTaskBoard(page, manifestOrder)
  const row = releaseReportRow(page, manifestOrder, task.nodeType)
  if (!(await row.isVisible({ timeout: 30000 }).catch(() => false))) {
    throw stageError({
      stage: 'S06',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S06打开报告上传待办',
      message: `未找到报告上传待办：${manifestOrder.workOrderCode} ${task.nodeType}`,
      expected: { workOrderCode: manifestOrder.workOrderCode, nodeType: task.nodeType, status: 'TODO' },
      actual: { reportTask: task }
    })
  }
  await row.locator('[data-production-release-report-open]').click()
  await page.waitForURL((url) => url.pathname.includes('/edhr-batch-execution/detail'), { timeout: 60000 })
  await page.locator('[data-edhr-batch-detail-page]').waitFor({ state: 'visible', timeout: 60000 })
}

function isReportPrepareUploadResponse(response) {
  return response.url().includes('/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload') &&
    response.request().method() === 'POST'
}

function isReportCompleteResponse(response) {
  return response.url().includes('/mes/pro/edhr-batch-execution/task/special-node/complete') &&
    response.request().method() === 'POST'
}

function assertReportPreparedReceipt(prepared, fixture, task) {
  assert.equal(prepared.fileName, fixture.fileName, `${task.nodeType}上传回执文件名不一致`)
  assert.equal(prepared.sha256, fixture.sha256, `${task.nodeType}上传回执哈希不一致`)
  assert.ok(String(prepared.fileId || '').trim(), `${task.nodeType}上传回执缺少文件ID`)
  assert.ok(String(prepared.storageRetentionHash || '').trim(), `${task.nodeType}上传回执缺少存储留存哈希`)
}

function assertReportCompletedReceipt(completed, fixture, task) {
  assert.equal(completed.nodeType, task.nodeType, `${task.nodeType}完成回执节点类型不一致`)
  assert.equal(String(completed.batchTaskId), String(task.batchTaskId), `${task.nodeType}完成回执批次任务不一致`)
  assert.equal(completed.nodeStatus, 'COMPLETED', `${task.nodeType}完成后节点状态必须为COMPLETED`)
  assert.ok(Array.isArray(completed.attachmentIds) && completed.attachmentIds.length > 0, `${task.nodeType}完成回执缺少附件ID`)
  assert.ok(Array.isArray(completed.attachmentHashes) && completed.attachmentHashes.includes(fixture.sha256), `${task.nodeType}完成回执缺少原文件哈希`)
  assert.ok(['REPORT_UPLOAD_PENDING', 'MANAGER_RELEASE_PENDING'].includes(completed.reportUploadStatus), `${task.nodeType}完成回执状态非法：${completed.reportUploadStatus}`)
}

async function uploadOneReleaseReport(page, manifestOrder, task, fixture) {
  await openReportUploadTaskFromWorkTask(page, manifestOrder, task)
  const uploadButton = page.locator(`[data-production-release-report-upload="${task.batchTaskId}"]`)
  await uploadButton.waitFor({ state: 'visible', timeout: 60000 })
  if (!(await uploadButton.isEnabled().catch(() => false))) {
    throw stageError({
      stage: 'S06',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S06上传报告附件',
      message: `报告上传按钮不可用：${manifestOrder.workOrderCode} ${task.nodeType}`,
      expected: { nodeType: task.nodeType, uploadEnabled: true },
      actual: { uploadEnabled: false, batchTaskId: task.batchTaskId }
    })
  }
  const prepareResponse = page.waitForResponse(isReportPrepareUploadResponse)
  await page.locator('.edhr-batch-detail__special-node-hidden-upload input[type="file"]').first().setInputFiles(fixture.filePath)
  const preparedBody = await readCommonResult(await prepareResponse, `${task.nodeType}报告附件预登记失败`)
  assert.equal(Number(preparedBody.code), 0, `${task.nodeType}报告附件预登记业务失败：${preparedBody.msg || 'unknown'}`)
  const prepared = preparedBody.data || {}
  assertReportPreparedReceipt(prepared, fixture, task)

  const completeButton = page.locator(`[data-production-release-report-complete="${task.batchTaskId}"]`)
  await completeButton.waitFor({ state: 'visible', timeout: 30000 })
  await completeButton.click()
  const dialog = page.locator('[data-production-release-report-complete-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  if (task.nodeType === 'STERILIZATION_REPORT') {
    await dialog.locator('[data-production-release-sterilization-batch] input').fill(`${runId}-STER-001`)
  }
  const completeResponse = page.waitForResponse(isReportCompleteResponse)
  await dialog.locator('[data-production-release-report-complete-confirm]').click()
  const completedBody = await readCommonResult(await completeResponse, `${task.nodeType}报告完成提交失败`)
  assert.equal(Number(completedBody.code), 0, `${task.nodeType}报告完成提交业务失败：${completedBody.msg || 'unknown'}`)
  const completed = completedBody.data || {}
  assertReportCompletedReceipt(completed, fixture, task)
  return { task, fixture: { fileName: fixture.fileName, sha256: fixture.sha256 }, prepared, completed }
}

async function uploadReleaseReportsS06(page, manifestOrder, pqcRelease, runDir) {
  const fixtures = createReportFixtureFiles(runDir, manifestOrder)
  const tasksByNode = new Map((pqcRelease.reportUploadTasks || []).map((task) => [task.nodeType, task]))
  const completedReports = []
  for (const plan of REPORT_UPLOAD_PLAN) {
    const task = tasksByNode.get(plan.nodeType)
    if (!task) {
      throw stageError({
        stage: 'S06',
        errorType: 'PRECONDITION_BLOCKED',
        action: 'S06报告任务完整性检查',
        message: `PQC放行回执缺少报告任务：${plan.nodeType}`,
        expected: { reportUploadTaskCount: 4, nodeType: plan.nodeType },
        actual: { reportUploadTasks: pqcRelease.reportUploadTasks || [] }
      })
    }
    completedReports.push(await uploadOneReleaseReport(page, manifestOrder, task, fixtures.get(plan.nodeType)))
  }
  const finalCompleted = completedReports[completedReports.length - 1].completed
  assert.equal(finalCompleted.reportUploadStatus, 'MANAGER_RELEASE_PENDING', '第四份报告完成后必须进入管理者代表最终放行待办')
  assert.ok(String(finalCompleted.releaseTransactionId || '').trim(), '第四份报告完成回执缺少放行事务ID')
  assert.ok(String(finalCompleted.managerReleaseWorkTaskId || '').trim(), '第四份报告完成回执缺少管理者代表待办ID')
  return {
    workOrderCode: manifestOrder.workOrderCode,
    batchExecutionId: pqcRelease.batchExecutionId,
    completedReportCount: completedReports.length,
    releaseTransactionId: finalCompleted.releaseTransactionId,
    managerReleaseWorkTaskId: finalCompleted.managerReleaseWorkTaskId,
    reportSnapshotHash: finalCompleted.reportSnapshotHash,
    completedReports: completedReports.map((item) => ({
      nodeType: item.task.nodeType,
      batchTaskId: item.task.batchTaskId,
      workTaskId: item.task.workTaskId,
      fileName: item.fixture.fileName,
      sha256: item.fixture.sha256,
      reportUploadStatus: item.completed.reportUploadStatus
    }))
  }
}
function isApprovalCenterReviewResponse(response) {
  return response.url().includes('/approval-center/tasks/review') &&
    response.request().method() === 'POST'
}

function isEdhrReleaseGetResponse(response) {
  return response.url().includes('/mes/pro/edhr-release/get') &&
    response.request().method() === 'GET'
}

async function approveManagerFinalReleaseS07(page, manifestOrder, reportUpload) {
  await openCandidateWorkTaskBoard(page, manifestOrder)
  const row = page.locator('.el-table__row:visible').filter({
    hasText: manifestOrder.workOrderCode
  }).filter({
    has: page.locator('[data-manager-release-approve]')
  }).first()
  if (!(await row.isVisible({ timeout: 30000 }).catch(() => false))) {
    throw stageError({
      stage: 'S07',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S07打开管理者代表最终放行待办',
      message: `未找到管理者代表最终放行候选待办：${manifestOrder.workOrderCode}`,
      expected: {
        workOrderCode: manifestOrder.workOrderCode,
        taskType: 'RELEASE_APPROVE',
        releaseTransactionId: reportUpload.releaseTransactionId,
        status: 'TODO'
      },
      actual: { reportUpload }
    })
  }
  const approveButton = row.locator('[data-manager-release-approve]').first()
  if (!(await approveButton.isEnabled().catch(() => false))) {
    throw stageError({
      stage: 'S07',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S07提交管理者代表最终放行',
      message: `管理者代表最终放行按钮不可用：${manifestOrder.workOrderCode}`,
      expected: { releaseTransactionId: reportUpload.releaseTransactionId, approveEnabled: true },
      actual: { approveEnabled: false }
    })
  }
  await approveButton.click()
  const dialog = page.locator('[data-manager-release-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('[data-manager-release-status]').filter({ hasText: '待管理者代表放行' }).waitFor({ state: 'visible', timeout: 60000 })
  await dialog.locator('[data-manager-release-signature-password] input').fill(signaturePassword)
  await dialog.locator('[data-manager-release-approval-opinion] textarea').fill(`AI E2E S07 管理者代表最终放行 ${manifestOrder.workOrderCode}`)
  const reviewResponse = page.waitForResponse(isApprovalCenterReviewResponse)
  const receiptResponse = page.waitForResponse(isEdhrReleaseGetResponse)
  await dialog.locator('[data-manager-release-confirm]').click()
  const reviewBody = await readCommonResult(await reviewResponse, '管理者代表最终放行审批提交失败')
  assert.equal(Number(reviewBody.code), 0, `管理者代表最终放行审批业务失败：${reviewBody.msg || 'unknown'}`)
  const receiptBody = await readCommonResult(await receiptResponse, '管理者代表最终放行回执查询失败')
  assert.equal(Number(receiptBody.code), 0, `管理者代表最终放行回执查询业务失败：${receiptBody.msg || 'unknown'}`)
  const receipt = receiptBody.data || {}
  assert.equal(receipt.releaseStatus, 'RELEASED', `最终放行后状态必须为RELEASED，实际为${receipt.releaseStatus}`)
  assert.equal(String(receipt.releaseTransactionId), String(reportUpload.releaseTransactionId), '最终放行事务ID必须与S06一致')
  assert.equal(String(receipt.batchExecutionId), String(reportUpload.batchExecutionId), '最终放行批次ID必须与S06一致')
  assert.ok(String(receipt.releaseApprovalWorkTaskId || '').trim(), '最终放行回执缺少管理者代表待办ID')
  assert.ok(String(receipt.approvedBy || '').trim(), '最终放行回执缺少审批人')
  assert.ok(String(receipt.approvedAt || '').trim(), '最终放行回执缺少审批时间')
  await dialog.locator('[data-manager-release-status]').filter({ hasText: '已放行' }).waitFor({ state: 'visible', timeout: 60000 })
  return {
    workOrderCode: manifestOrder.workOrderCode,
    releaseTransactionId: receipt.releaseTransactionId,
    batchExecutionId: receipt.batchExecutionId,
    releaseStatus: receipt.releaseStatus,
    releaseApprovalWorkTaskId: receipt.releaseApprovalWorkTaskId,
    approvedBy: receipt.approvedBy,
    approvedAt: receipt.approvedAt,
    reportSnapshotHash: receipt.reportSnapshotHash
  }
}

function isEdhrBatchArchiveGenerateResponse(response) {
  return response.url().includes('/mes/pro/edhr-batch-execution-archive/generate') &&
    response.request().method() === 'POST'
}

function isEdhrBatchHistoryPageResponseForOrder(manifestOrder) {
  return (response) => {
    if (!response.url().includes('/mes/pro/edhr-batch-execution/page') ||
      response.request().method() !== 'GET') return false
    const parsed = new URL(response.url())
    return parsed.searchParams.get('workOrderCode') === manifestOrder.workOrderCode &&
      parsed.searchParams.get('batchCode') === manifestOrder.batchCode
  }
}

function assertArchiveGeneratedReceipt(archive, manifestOrder, finalRelease, archiveTask) {
  assert.equal(String(archive.batchExecutionId), String(finalRelease.batchExecutionId), 'S08归档批次ID必须与最终放行批次一致')
  assert.equal(String(archive.batchExecutionId), String(archiveTask.batchExecutionId), 'S08归档批次ID必须与归档待办一致')
  assert.equal(archive.archiveStatus, 'SEALED', `S08归档状态必须为SEALED，实际为${archive.archiveStatus}`)
  assert.equal(archive.pdfaValidationStatus, 'VALID', `S08归档PDF/A校验必须为VALID，实际为${archive.pdfaValidationStatus}`)
  assert.ok(String(archive.id || '').trim(), `S08归档回执缺少归档ID：${manifestOrder.workOrderCode}`)
  assert.ok(String(archive.fileId || '').trim(), `S08归档回执缺少文件ID：${manifestOrder.workOrderCode}`)
  assert.ok(String(archive.contentHash || '').trim(), `S08归档回执缺少内容哈希：${manifestOrder.workOrderCode}`)
  assert.ok(String(archive.pdfaProfile || '').trim(), `S08归档回执缺少PDF/A profile：${manifestOrder.workOrderCode}`)
}

async function openArchiveTaskFromWorkTask(page, manifestOrder, finalRelease) {
  const workTaskPage = await openMyWorkTaskBoard(page, manifestOrder)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-edhr-work-task-row-work-order]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({
    has: page.locator('[data-edhr-archive-task-open]')
  }).first()
  if (!(await row.isVisible({ timeout: 30000 }).catch(() => false))) {
    throw stageError({
      stage: 'S08',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S08打开最终归档待办',
      message: `未找到本轮订单的最终归档我的待办：${manifestOrder.workOrderCode}`,
      expected: {
        workOrderCode: manifestOrder.workOrderCode,
        taskType: 'ARCHIVE',
        batchExecutionId: finalRelease.batchExecutionId,
        status: 'TODO'
      },
      actual: { workTaskPage }
    })
  }
  const openButton = row.locator('[data-edhr-archive-task-open]').first()
  const workTaskId = await openButton.getAttribute('data-edhr-archive-task-open')
  if (!String(workTaskId || '').trim()) {
    throw stageError({
      stage: 'S08',
      errorType: 'TEST_HARNESS_FAILURE',
      action: 'S08读取最终归档待办ID',
      message: `最终归档按钮缺少data-edhr-archive-task-open：${manifestOrder.workOrderCode}`,
      expected: { archiveWorkTaskId: 'positive string id' },
      actual: { workTaskId }
    })
  }
  await openButton.click()
  await page.waitForURL((url) => url.pathname.includes('/edhr-batch-execution/detail'), { timeout: 60000 })
  await page.locator('[data-edhr-batch-detail-page]').waitFor({ state: 'visible', timeout: 60000 })
  return {
    workTaskId,
    batchExecutionId: finalRelease.batchExecutionId,
    detailUrl: page.url()
  }
}

async function generateArchiveFromDetailPage(page, manifestOrder, finalRelease, archiveTask) {
  await page.locator('[data-edhr-release-action="archive-print"]').click()
  await page.locator('[data-edhr-archive-drawer]').waitFor({ state: 'visible', timeout: 30000 })
  const generateButton = page.locator('[data-edhr-archive-generate]').first()
  if (!(await generateButton.isEnabled().catch(() => false))) {
    throw stageError({
      stage: 'S08',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S08生成最终归档',
      message: `最终归档生成按钮不可用：${manifestOrder.workOrderCode}`,
      expected: { releaseStatus: 'RELEASED', archiveTaskId: archiveTask.workTaskId, canGenerateArchive: true },
      actual: { finalRelease, archiveTask }
    })
  }
  const archiveResponse = page.waitForResponse(isEdhrBatchArchiveGenerateResponse)
  await generateButton.click()
  const body = await readCommonResult(await archiveResponse, '最终归档生成失败')
  assert.equal(Number(body.code), 0, `最终归档生成业务失败：${body.msg || 'unknown'}`)
  const archive = body.data || {}
  assertArchiveGeneratedReceipt(archive, manifestOrder, finalRelease, archiveTask)
  await page.locator('[data-edhr-archive-status]').filter({ hasText: 'SEALED' }).waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-edhr-archive-version]').filter({ hasText: /^V[1-9]\d*$/ }).waitFor({ state: 'visible', timeout: 60000 })
  return archive
}

async function verifyArchivedHistoryPage(page, manifestOrder, finalRelease, archive) {
  const historyUrl = new URL('/mes/pro/feedback/edhr-batch-history', frontendUrl)
  historyUrl.searchParams.set('batchExecutionId', String(finalRelease.batchExecutionId))
  historyUrl.searchParams.set('workOrderCode', manifestOrder.workOrderCode)
  historyUrl.searchParams.set('batchCode', manifestOrder.batchCode)
  const pageResponse = page.waitForResponse(isEdhrBatchHistoryPageResponseForOrder(manifestOrder))
  await page.goto(historyUrl.toString(), { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-edhr-batch-history-page]').waitFor({ state: 'visible', timeout: 60000 })
  const pageBody = await readCommonResult(await pageResponse, '历史追溯列表查询失败')
  assert.equal(Number(pageBody.code), 0, `历史追溯列表业务失败：${pageBody.msg || 'unknown'}`)
  const rows = pageBody.data?.list || []
  const archivedRow = rows.find((row) =>
    String(row.id) === String(finalRelease.batchExecutionId) ||
    (row.workOrderCode === manifestOrder.workOrderCode && row.batchCode === manifestOrder.batchCode)
  )
  assert.ok(archivedRow, `历史追溯列表缺少本轮批次：${manifestOrder.workOrderCode}`)
  assert.equal(Number(archivedRow.status), 40, `历史追溯列表批次必须为已归档状态40，实际为${archivedRow.status}`)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-edhr-history-work-order-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({
    has: page.locator(`[data-edhr-history-batch-code]:text-is("${manifestOrder.batchCode}")`)
  }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-edhr-history-batch-status]').filter({ hasText: '已归档' }).waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-edhr-history-active-order-detail]').click()
  await page.locator('[data-edhr-batch-active-order-detail-page]').waitFor({ state: 'visible', timeout: 60000 })
  const operationFacts = page.locator('[data-active-order-summary-operation-facts-table]')
  await operationFacts.waitFor({ state: 'visible', timeout: 60000 })
  const createFact = operationFacts.locator('[data-active-order-operation-fact]').filter({ hasText: '创建不合格评审' })
  const dispositionFact = operationFacts.locator('[data-active-order-operation-fact]').filter({ hasText: '让步放行' })
  await createFact.first().waitFor({ state: 'visible', timeout: 60000 })
  await dispositionFact.first().waitFor({ state: 'visible', timeout: 60000 })
  assert.match(await createFact.first().innerText(), /创建不合格评审[\s\S]*NONCONFORMANCE_REVIEW[\s\S]*SUCCESS/, '历史详情中的不合格评审创建事实不完整')
  assert.match(await dispositionFact.first().innerText(), /让步放行[\s\S]*NONCONFORMANCE_REVIEW[\s\S]*SUCCESS/, '历史详情中的不合格评审处置事实不完整')
  return {
    batchExecutionId: finalRelease.batchExecutionId,
    archiveId: archive.id,
    archiveStatus: archive.archiveStatus,
    archiveVersion: archive.archiveVersion,
    historyRowStatus: archivedRow.status,
    nonconformanceCreateFactVisible: true,
    nonconformanceDispositionFactVisible: true
  }
}

async function archiveAndVerifyHistoryS08(page, manifestOrder, finalRelease) {
  const archiveTask = await openArchiveTaskFromWorkTask(page, manifestOrder, finalRelease)
  const archive = await generateArchiveFromDetailPage(page, manifestOrder, finalRelease, archiveTask)
  const history = await verifyArchivedHistoryPage(page, manifestOrder, finalRelease, archive)
  return {
    workOrderCode: manifestOrder.workOrderCode,
    batchCode: manifestOrder.batchCode,
    archiveTask,
    archive: {
      id: archive.id,
      archiveStatus: archive.archiveStatus,
      archiveVersion: archive.archiveVersion,
      fileId: archive.fileId,
      contentHash: archive.contentHash,
      pdfaProfile: archive.pdfaProfile,
      pdfaValidationStatus: archive.pdfaValidationStatus
    },
    history
  }
}
async function main() {
  if (!onlyS01) {
    try { productionIdentity = resolveProductionIdentity(process.env) }
    catch (error) { exitWithFailure(error, 'S00', 'PRECONDITION_BLOCKED'); return }
  }
  let manifest = buildManifest({ runId, mode, resetWorkOrderCode })
  const selectedOrders = ordersForMode(manifest)
  const runDir = writeRunReport({ rootDir: reportRoot, manifest, result: {
    schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: 'RUNNING', failedStage: null,
    errorType: null, action: '初始化AI E2E运行', message: null, pageUrl: null,
    screenshot: null, trace: null, targetRequests: [], candidateCodePaths: [],
    startedAt: new Date().toISOString(), finishedAt: null
  } })
  const { chromium } = require('playwright')
  const browser = await chromium.launch({ headless: !headed })
  const context = await browser.newContext({ recordVideo: { dir: path.join(runDir, 'videos') } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const targetRequests = trackTargetRequests(page)
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await runWithStage('S00', 'S00登录页与账号会话初始化',
      { loginPageLoaded: true, userAuthenticated: true, tenant: '芋道源码', username }, () => login(page))
    const activeOrders = []
    for (const item of selectedOrders) {
      activeOrders.push(await runWithStage('S01', 'S01重置指定测试订单生成活跃测试订单',
        { action: 'RESET_FIXED_TEST_ORDER', resetWorkOrderCode: manifest.resetWorkOrderCode, aiRunOrderSlotId: item.simulationRunId },
        () => resetFixedTestActiveOrder(page, item, manifest)))
    }
    const mainOrder = activeOrders[0]
    if (onlyS01) {
      await page.screenshot({ path: path.join(runDir, 'S01.png'), fullPage: true })
      await context.tracing.stop({ path: path.join(runDir, 'trace.zip') })
      await targetRequests.flush()
      const result = {
        schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: 'PASS', failedStage: null,
        errorType: null, action: 'E2E01四步验证', message: 'E2E01通过；E2E02至08未执行',
        expected: { workOrderCode: manifest.resetWorkOrderCode, quantity: 10 },
        actual: { activeOrders }, pageUrl: page.url(), screenshot: 'S01.png', trace: 'trace.zip',
        targetRequests, targetRequestEvidenceFlushed: true, candidateCodePaths: [],
        stages: stageResults(null, 'NOT_RUN').map((stage) =>
          stage.stage === 'S01' ? { ...stage, status: 'PASS' } : stage),
        startedAt: JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8')).startedAt,
        finishedAt: new Date().toISOString()
      }
      writeRunReport({ rootDir: reportRoot, manifest, result })
      console.log(JSON.stringify({ status: 'PASS', runId, runDir, scope: 'S01', activeOrders }, null, 2))
      return
    }
    const executionBaseline = await runWithStage('S01', '冻结全部生产工序及PQC任务', { activeOrderId: mainOrder.activeOrderId }, () => freezeActiveOrderExecutionBaseline(page, mainOrder))
    mainOrder.executionBaseline = executionBaseline
    manifest = Object.freeze({ ...manifest, executionBaseline, expected: Object.freeze({ ...manifest.expected, ...executionBaseline.expected }) })
    fs.writeFileSync(path.join(runDir, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')
    const processExecution = await runWithStage('S02', 'S02/S03生产PQC交错执行',
      { ...executionBaseline.expected, interleaved: true }, () => executeProductionAndPqcInterleaved(page, mainOrder, activeOrders[0]))
    if (throughS03) {
      await page.screenshot({ path: path.join(runDir, 'S03.png'), fullPage: true })
      await context.tracing.stop({ path: path.join(runDir, 'trace.zip') })
      await targetRequests.flush()
      const result = {
        schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: 'PASS', failedStage: null,
        errorType: null, action: 'E2E01—03真实页面验证', message: 'E2E01—03通过；E2E04—08未执行',
        expected: executionBaseline.expected, actual: { activeOrders, processExecution },
        pageUrl: page.url(), screenshot: 'S03.png', trace: 'trace.zip',
        targetRequests, targetRequestEvidenceFlushed: true, candidateCodePaths: [],
        stages: stageResults(null, 'NOT_RUN').map(stage =>
          ['S01', 'S02', 'S03'].includes(stage.stage) ? { ...stage, status: 'PASS' } : stage),
        startedAt: JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8')).startedAt,
        finishedAt: new Date().toISOString()
      }
      writeRunReport({ rootDir: reportRoot, manifest, result })
      console.log(JSON.stringify({ status: 'PASS', runId, runDir, scope: 'S01-S03', expected: executionBaseline.expected }, null, 2))
      return
    }
    const completion = await runWithStage('S04', 'S04领料晚到回填与完工申请',
      { releaseApplicationStatus: 'PQC_RELEASE_PENDING', inputMaterialPickListVisible: true, inputMaterialBatchVisible: true }, () => completeActiveOrderAndApplyRelease(page, mainOrder))
    const pqcRelease = await runWithStage('S05', 'S05不合格评审闭环及PQC生产放行',
      { nonconformanceDisposition: 'concession_release', status: 'REPORT_UPLOAD_PENDING', reportUploadTaskCount: 4 }, () => approvePqcProductionReleaseS05(page, mainOrder, completion, runDir))
    const reportUpload = await runWithStage('S06', 'S06四份资料上传',
      { completedReportCount: 4, nextStatus: 'MANAGER_RELEASE_PENDING' }, () => uploadReleaseReportsS06(page, mainOrder, pqcRelease, runDir))
    const finalRelease = await runWithStage('S07', 'S07管理者代表最终放行',
      { releaseStatus: 'RELEASED' }, () => approveManagerFinalReleaseS07(page, mainOrder, reportUpload))
    const archiveTrace = await runWithStage('S08', 'S08归档及历史追溯',
      { archiveStatus: 'SEALED', historyStatus: 40 }, () => archiveAndVerifyHistoryS08(page, mainOrder, finalRelease))
    await context.tracing.stop({ path: path.join(runDir, 'trace.zip') })
    await targetRequests.flush()
    const result = { schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: 'PASS', failedStage: null,
      errorType: null, action: 'S01-S08 eDHR主流程完成',
      expected: { businessStages: ['S01', 'S02', 'S03', 'S04', 'S05', 'S06', 'S07', 'S08'], fullModeOrderSlots: ['O01'], s01Action: 'RESET_FIXED_TEST_ORDER', resetWorkOrderCode: manifest.resetWorkOrderCode, finishedQuantity: mainOrder.quantity, ...executionBaseline.expected, releaseApplicationStatus: 'PQC_RELEASE_PENDING', pqcReleaseStatus: 'REPORT_UPLOAD_PENDING', reportUploadTaskCount: 4, reportUploadCompletedCount: 4, finalReleasePending: true, finalReleaseStatus: 'RELEASED', archiveStatus: 'SEALED', historyStatus: 40, expectedFormalLoss: 0 },
      actual: { preparedByFixedReset: true, resetOrderSlots: activeOrders.map((order) => order.slot), resetOrderCount: activeOrders.length, activeOrders, processExecution, production: processExecution.production, pqc: processExecution.pqc, completion, pqcRelease, reportUpload, finalRelease, archiveTrace },
      message: 'S01已通过重置指定测试订单生成本轮活跃测试订单；S02/S03已按冻结的全部工序及检验任务交错完成一线生产、生产组长FIFO复核、一线PQC及PQC组长复核；S04已完成领料晚到回填与无补料确认；S05已完成PQC生产放行；S06已完成四份资料上传；S07已完成管理者代表最终放行；S08已完成最终归档并在历史追溯页验证归档版本、时间线和放行资料目录。',
      pageUrl: page.url(), screenshot: null, trace: 'trace.zip', targetRequests, targetRequestEvidenceFlushed: true, candidateCodePaths: [],
      stages: stageResults(null, 'PASS'), startedAt: JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8')).startedAt,
      finishedAt: new Date().toISOString() }
    writeRunReport({ rootDir: reportRoot, manifest, result })
    console.log(JSON.stringify({ status: 'PASS', runId, runDir, failedStage: null }, null, 2))
    process.exitCode = 0
  } catch (error) {
    await context.tracing.stop({ path: path.join(runDir, 'trace.zip') }).catch(() => {})
    const classification = classifyError(error)
    const failedStage = error.stage || (classification.errorType === 'INFRASTRUCTURE_BLOCKED' ? 'S00' : 'S01')
    const screenshot = path.join(runDir, `${failedStage}.png`)
    await page.screenshot({ path: screenshot, fullPage: true }).catch(() => {})
    await targetRequests.flush()
    const blocked = ['INFRASTRUCTURE_BLOCKED', 'UI_ACTION_FAILED', 'TEST_HARNESS_FAILURE', 'PRECONDITION_BLOCKED'].includes(classification.errorType)
    const result = { schemaVersion: 'AI_EDHR_E2E_RESULT_V1', runId, status: blocked ? 'BLOCKED' : 'FAIL', failedStage,
      errorType: classification.errorType, action: error.action || '指定测试订单重置准备',
      expected: error.expected || { resetWorkOrderCode, mode, selectedOrderSlots: ordersForMode(manifest).map((order) => order.slot) },
      actual: { runId, activeOrderId: manifest.executionBaseline?.activeOrderId, workOrderCode: manifest.executionBaseline?.workOrderCode, ...(error.actual || { message: error.message }) },
      message: error.message,
      pageUrl: page.url(), screenshot: path.basename(screenshot), trace: 'trace.zip',
      targetRequests, targetRequestEvidenceFlushed: true, candidateCodePaths: stageCandidateCodePaths(failedStage),
      stages: stageResults(failedStage, blocked ? 'BLOCKED' : 'FAIL'),
      startedAt: JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8')).startedAt, finishedAt: new Date().toISOString() }
    writeRunReport({ rootDir: reportRoot, manifest, result })
    console.error(JSON.stringify({ status: result.status, runId, runDir, failedStage: result.failedStage, error: result.message }, null, 2))
    process.exitCode = classification.exitCode
  } finally {
    await browser.close()
  }
}

if (require.main === module) {
  main().catch((error) => exitWithFailure(error, 'S00', 'INFRASTRUCTURE_BLOCKED'))
}

module.exports = { ordersForMode, stageCandidateCodePaths, trackTargetRequests }







