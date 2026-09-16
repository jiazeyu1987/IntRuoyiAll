#!/usr/bin/env node
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const crypto = require('node:crypto')
const { createRunId, buildManifest, FIXED_TEMPLATE_WORK_ORDER_CODE } = require('./manifest.cjs')
const { EXIT_CODES, writeRunReport, writeFailureArtifacts, classifyError } = require('./reporter.cjs')
const { stageResults } = require('./stages.cjs')

function arg(name) {
  const index = process.argv.indexOf(`--${name}`)
  return index >= 0 ? process.argv[index + 1] : undefined
}

function ordersForMode(manifest) {
  if (manifest.mode === 'full') {
    return manifest.orders.filter((order) => order.slot === 'O01')
  }
  return manifest.orders
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
  if (route.includes('/active-order/simulation/copy-latest')) return 'SIMULATION_COPY_LATEST'
  if (route.includes('/active-order/release/apply')) return 'ACTIVE_ORDER_RELEASE_APPLY'
  if (route.includes('/frontline/submit')) return 'FRONTLINE_PRODUCTION_SUBMIT'
  if (route.includes('/device-account/pqc/submit')) return 'FRONTLINE_PQC_SUBMIT'
  if (route.includes('/submission/allocation/preview-fifo')) return 'PRODUCTION_REVIEW_FIFO_PREVIEW'
  if (route.includes('/submission/allocation/confirm')) return 'PRODUCTION_REVIEW_CONFIRM'
  if (route.includes('/submission/review')) return 'PQC_REVIEW_CONFIRM'
  if (route.includes('/edhr-work-task') && method === 'GET') return 'WORK_TASK_PAGE'
  if (route.includes('/pqc-production-release/page')) return 'PQC_RELEASE_PAGE'
  if (route.includes('/pqc-production-release/approve')) return 'PQC_RELEASE_APPROVE'
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
const runId = arg('run-id') || process.env.EDHR_AI_E2E_RUN_ID || createRunId()
const templateWorkOrderCode = arg('template-work-order-code') || process.env.EDHR_AI_E2E_TEMPLATE_WORK_ORDER_CODE || FIXED_TEMPLATE_WORK_ORDER_CODE
const frontendUrl = arg('base-url') || process.env.EDHR_AI_E2E_BASE_URL || 'http://127.0.0.1:8081'
const reportRoot = path.resolve(arg('report-root') || process.env.EDHR_AI_E2E_REPORT_ROOT || 'test-results/edhr-ai-loop')
const username = process.env.EDHR_E2E_USERNAME || 'admin'
const password = process.env.EDHR_E2E_PASSWORD || 'admin123'
const signaturePassword = process.env.EDHR_E2E_SIGNATURE_PASSWORD || password
const headed = process.argv.includes('--headed')
const pqcNumericValues = Object.freeze([
  Number(process.env.EDHR_AI_E2E_PQC_GENERIC_VALUE || 10),
  Number(process.env.EDHR_AI_E2E_PQC_SPECIAL_VALUE || 20)
])
const PQC_ROUND_PLAN = Object.freeze([
  Object.freeze({ ruleKey: 'FIRST', type: 'FIRST', quantity: 2 }),
  Object.freeze({ ruleKey: 'PATROL_AM', type: 'PATROL', quantity: 3 }),
  Object.freeze({ ruleKey: 'PATROL_PM', type: 'PATROL', quantity: 3 }),
  Object.freeze({ ruleKey: 'FINAL', type: 'FINAL', quantity: 2 })
])
const INTERLEAVED_MAIN_CHAIN_PLAN = Object.freeze([
  Object.freeze({ kind: 'PRODUCTION', processIndex: 0, quantity: 40 }),
  Object.freeze({ kind: 'PQC', processIndex: 0, ruleKey: 'FIRST' }),
  Object.freeze({ kind: 'PQC', processIndex: 0, ruleKey: 'PATROL_AM' }),
  Object.freeze({ kind: 'PRODUCTION', processIndex: 0, quantity: 60 }),
  Object.freeze({ kind: 'PQC', processIndex: 0, ruleKey: 'PATROL_PM' }),
  Object.freeze({ kind: 'PQC', processIndex: 0, ruleKey: 'FINAL' }),
  Object.freeze({ kind: 'PRODUCTION', processIndex: 1, quantity: 40 }),
  Object.freeze({ kind: 'PQC', processIndex: 1, ruleKey: 'FIRST' }),
  Object.freeze({ kind: 'PQC', processIndex: 1, ruleKey: 'PATROL_AM' }),
  Object.freeze({ kind: 'PRODUCTION', processIndex: 1, quantity: 60 }),
  Object.freeze({ kind: 'PQC', processIndex: 1, ruleKey: 'PATROL_PM' }),
  Object.freeze({ kind: 'PQC', processIndex: 1, ruleKey: 'FINAL' })
])
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
    expected: { configuredTemplateOrTemplateCode: true },
    actual: { templateWorkOrderCodeProvided: Boolean(templateWorkOrderCode), mode },
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
  try {
    return await fn()
  } catch (error) {
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
  await selectLoginTenant(page)
  await page.locator('.login-form input[placeholder="请输入用户名"]:visible').first().fill(username)
  await page.locator('.login-form input[type="password"]:visible').first().fill(password)
  const response = page.waitForResponse((r) => r.url().includes('/admin-api/system/auth/login') && r.request().method() === 'POST')
  await page.locator('.login-form button[type="submit"]:visible, .login-form button:has-text("登录"):visible').first().click()
  const loginResponse = await response
  assert.equal(loginResponse.ok(), true, `登录失败：HTTP ${loginResponse.status()}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function selectLoginTenant(page, tenantName = '芋道源码') {
  const tenant = page.locator('.login-form .el-select input:visible').first()
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

async function verifyTemplateReady(page, manifest) {
  const rows = await openActiveOrderPool(page, manifest.templateWorkOrderCode)
  const source = findActiveOrderDataByCode(rows, manifest.templateWorkOrderCode)
  if (!source) {
    throw stageError({
      stage: 'VERIFY_READY',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'VERIFY_READY固定母单可见性',
      message: `活跃订单池找不到固定母单：${manifest.templateWorkOrderCode}`,
      expected: { sourceWorkOrderCode: manifest.templateWorkOrderCode, sourceVisible: true },
      actual: { sourceVisible: false, visibleCodes: rows.map((row) => row.workOrderCode).slice(0, 20) }
    })
  }
  const row = activeOrderRow(page, manifest.templateWorkOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const sourceActiveOrderId = requirePositiveIdString(
    await row.locator('[data-team-leader-active-order-id]').first().getAttribute('data-team-leader-active-order-id'),
    '固定母单活跃订单ID'
  )
  const copyButton = row.locator('[data-team-leader-copy-latest-simulation-order]').first()
  if (!(await copyButton.isVisible().catch(() => false))) {
    throw stageError({
      stage: 'VERIFY_READY',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'VERIFY_READY复制测试单入口',
      message: `固定母单缺少复制测试单按钮：${manifest.templateWorkOrderCode}`,
      expected: { copyButtonVisible: true },
      actual: { copyButtonVisible: false }
    })
  }
  const sourceQuantity = resolveActiveOrderQuantity(source, '固定母单')
  if (sourceQuantity !== manifest.expected.finishedQuantity) {
    throw stageError({
      stage: 'VERIFY_READY',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'VERIFY_READY固定母单数量',
      message: `固定母单数量不是预期值：${manifest.templateWorkOrderCode}`,
      expected: { quantity: manifest.expected.finishedQuantity },
      actual: { quantity: sourceQuantity }
    })
  }
  return {
    sourceWorkOrderCode: manifest.templateWorkOrderCode,
    sourceActiveOrderId,
    sourceQuantity,
    copyButtonVisible: true,
    readyVerified: true
  }
}

async function copyTemplateActiveOrder(page, manifestOrder, manifest, ready) {
  const rows = await openActiveOrderPool(page, manifest.templateWorkOrderCode)
  const source = findActiveOrderDataByCode(rows, manifest.templateWorkOrderCode)
  if (!source) {
    throw stageError({
      stage: 'S01',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S01定位固定母单',
      message: `复制前找不到固定母单：${manifest.templateWorkOrderCode}`,
      expected: { sourceWorkOrderCode: manifest.templateWorkOrderCode, sourceVisible: true },
      actual: { sourceVisible: false }
    })
  }
  const sourceQuantity = resolveActiveOrderQuantity(source, '固定母单')
  const row = activeOrderRow(page, manifest.templateWorkOrderCode)
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const sourceActiveOrderId = requirePositiveIdString(
    await row.locator('[data-team-leader-active-order-id]').first().getAttribute('data-team-leader-active-order-id'),
    '固定母单活跃订单ID'
  )
  if (ready?.sourceActiveOrderId && ready.sourceActiveOrderId !== sourceActiveOrderId) {
    throw stageError({
      stage: 'S01',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S01固定母单身份一致性',
      message: '固定母单活跃订单ID在VERIFY_READY和S01之间发生变化',
      expected: { sourceActiveOrderId: ready.sourceActiveOrderId },
      actual: { sourceActiveOrderId }
    })
  }
  const copyButton = row.locator('[data-team-leader-copy-latest-simulation-order]').first()
  await copyButton.waitFor({ state: 'visible', timeout: 30000 })
  await copyButton.click()
  const confirmDialog = page.locator('.el-message-box:visible').filter({ hasText: '确认复制测试单' }).first()
  await confirmDialog.waitFor({ state: 'visible', timeout: 30000 })
  const copyResponse = page.waitForResponse((r) =>
    r.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/simulation/copy-latest') &&
    r.request().method() === 'POST'
  )
  const refreshedListResponse = page.waitForResponse((r) =>
    r.url().includes('/admin-api/mes/pro/process-pool/team-leader/active-order/list') &&
    r.request().method() === 'GET',
    { timeout: 60000 }
  ).catch(() => null)
  await confirmDialog.getByRole('button', { name: '确认复制' }).click()
  const response = await copyResponse
  assert.equal(response.ok(), true, `复制测试单失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `复制测试单业务失败：${body.msg || 'unknown'}`)
  const receipt = body.data || {}
  const copiedSimulationRunId = String(receipt.simulationRunId || '').trim()
  assert.ok(copiedSimulationRunId, '复制测试单回执必须包含真实页面复制动作生成的simulationRunId')
  assert.match(copiedSimulationRunId, /^SIMCOPY-\d+-[0-9a-fA-F-]{36}$/, `复制测试单回执simulationRunId必须来自真实页面复制动作，实际为${copiedSimulationRunId}`)
  const copiedWorkOrderCode = String(receipt.workOrderCode || '').trim()
  assert.ok(copiedWorkOrderCode.startsWith(manifestOrder.expectedWorkOrderCodePrefix), `复制测试单工单号必须以${manifestOrder.expectedWorkOrderCodePrefix}开头，实际为${copiedWorkOrderCode}`)
  assert.notEqual(copiedWorkOrderCode, manifest.templateWorkOrderCode, '复制测试单不得复用固定母单工单号')
  const refreshedRows = await refreshedListResponse.then((r) => r ? parseActiveOrderListResponse(r, '复制后活跃订单列表刷新') : [])
  let copied = findActiveOrderDataByCode(refreshedRows, copiedWorkOrderCode)
  if (!copied) {
    copied = findActiveOrderDataByCode(await openActiveOrderPool(page, copiedWorkOrderCode), copiedWorkOrderCode)
  } else {
    await filterActiveOrderPool(page, copiedWorkOrderCode)
  }
  if (!copied) {
    throw stageError({
      stage: 'S01',
      errorType: 'BUSINESS_ASSERTION',
      action: 'S01复制后活跃订单可见性',
      message: `复制接口返回成功，但活跃订单池找不到新测试单：${copiedWorkOrderCode}`,
      expected: { copiedWorkOrderCode, copiedActiveOrderVisible: true },
      actual: { copiedActiveOrderVisible: false }
    })
  }
  const copiedQuantity = resolveActiveOrderQuantity(copied, '复制测试单')
  if (copiedQuantity !== sourceQuantity) {
    throw stageError({
      stage: 'S01',
      errorType: 'BUSINESS_ASSERTION',
      action: 'S01复制数量一致性',
      message: `复制测试单数量与固定母单不一致：${copiedWorkOrderCode}`,
      expected: { sourceQuantity },
      actual: { copiedQuantity }
    })
  }
  await activeOrderRow(page, copiedWorkOrderCode).waitFor({ state: 'visible', timeout: 30000 })
  return {
    slot: manifestOrder.slot,
    sourceWorkOrderCode: manifest.templateWorkOrderCode,
    sourceActiveOrderId,
    activeOrderId: requirePositiveIdString(receipt.activeOrderId, '复制测试单回执activeOrderId'),
    workOrderId: requirePositiveIdString(receipt.workOrderId, '复制测试单回执workOrderId'),
    workOrderCode: copiedWorkOrderCode,
    workOrderName: receipt.workOrderName,
    batchCode: copiedWorkOrderCode,
    quantity: copiedQuantity,
    routeId: requirePositiveIdString(receipt.routeId, '复制测试单回执routeId'),
    routeVersionId: requirePositiveIdString(receipt.routeVersionId, '复制测试单回执routeVersionId'),
    routeVersionNo: receipt.routeVersionNo,
    qaRegulationVersionId: requirePositiveIdString(receipt.qaRegulationVersionId, '复制测试单回执qaRegulationVersionId'),
    aiRunOrderSlotId: manifestOrder.simulationRunId,
    simulationRunId: copiedSimulationRunId,
    action: 'COPY_LATEST_VERSION'
  }
}

function buildProductionSubmissionPlan(quantity) {
  const total = Number(quantity)
  if (!Number.isInteger(total) || total < 2) {
    throw stageError({
      stage: 'S02',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S02生成生产提交计划',
      message: '复制测试单数量不足以拆分两次生产提交',
      expected: { quantityAtLeast: 2 },
      actual: { quantity }
    })
  }
  const first = Math.max(1, Math.floor(total * 0.4))
  const second = total - first
  return [
    { processIndex: 0, quantity: first },
    { processIndex: 0, quantity: second },
    { processIndex: 1, quantity: first },
    { processIndex: 1, quantity: second }
  ]
}

async function selectFrontlineProductionOrder(page, manifestOrder) {
  await page.goto(`${frontendUrl}/mes/pro/feedback/edhr-batch-production-fill`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-frontline-production-stage]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-frontline-production-active-order-card]').click()
  await page.locator('[data-frontline-production-order-search-input]').fill(manifestOrder.workOrderCode)
  const option = page.locator('[data-frontline-production-order-option]').filter({
    has: page.locator(`[data-frontline-production-order-option-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await page.locator('[data-frontline-production-order-code]').filter({ hasText: manifestOrder.workOrderCode }).waitFor({ state: 'visible', timeout: 30000 })
}

async function selectFrontlineProductionProcess(page, processIndex) {
  await page.locator('[data-frontline-production-process-current]').click()
  const option = page.locator('[data-frontline-production-process-option]').nth(processIndex)
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  await page.locator('[data-frontline-production-process-current]').filter({ hasText: label }).waitFor({ state: 'visible', timeout: 30000 })
  return label
}

async function selectFrontlineProductionEmployee(page) {
  const employeeCard = page.locator('[data-frontline-production-employee-card]')
  await employeeCard.click()
  const option = page.locator('[data-frontline-production-employee-option]').first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  return label
}

async function fillProductionQuantityForAllMaterials(page, quantity) {
  const materialTabs = page.locator('[data-frontline-production-material-tab]')
  const tabCount = await materialTabs.count()
  if (tabCount > 0) {
    for (let index = 0; index < tabCount; index += 1) {
      await materialTabs.nth(index).click()
      await page.locator('[data-production-output-quantity]').fill(String(quantity))
    }
    return { materialTabCount: tabCount }
  }
  await page.locator('[data-production-output-quantity]').fill(String(quantity))
  return { materialTabCount: 0 }
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

async function submitOneProductionReport(page, manifestOrder, step) {
  await selectFrontlineProductionOrder(page, manifestOrder)
  const processLabel = await selectFrontlineProductionProcess(page, step.processIndex)
  const employeeLabel = await selectFrontlineProductionEmployee(page)
  const quantityScope = await fillProductionQuantityForAllMaterials(page, step.quantity)
  const clearance = await confirmClearanceChecks(page)
  await page.locator('[data-production-submit-open-confirmation]').click()
  const dialog = page.locator('[data-production-submit-confirmation-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-production-submit-signature-password]').fill(signaturePassword)
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

async function reviewProductionReport(page, manifestOrder) {
  await openProductionReportWorkbench(page)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-team-leader-submission-work-order-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const reviewButton = row.locator('[data-team-leader-review-event-id]').first()
  const eventId = await reviewButton.getAttribute('data-team-leader-review-event-id')
  await reviewButton.click()
  const dialog = page.locator('[data-team-leader-review-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-team-leader-review-signature-password] input').fill(signaturePassword)
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

async function submitProductionReports(page, manifestOrder, activeOrder) {
  const plan = buildProductionSubmissionPlan(manifestOrder.quantity)
  const submissions = []
  for (const step of plan) submissions.push(await submitOneProductionReport(page, manifestOrder, step))
  const reviews = []
  for (const ignored of submissions) reviews.push(await reviewProductionReport(page, manifestOrder))
  return { activeOrderId: activeOrder.activeOrderId, expectedSubmissionCount: 4, submissions, reviews }
}


async function selectFrontlinePqcOrder(page, manifestOrder) {
  await page.goto(`${frontendUrl}/mes/pro/feedback/edhr-batch-pqc-fill`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-frontline-pqc-operator]').waitFor({ state: 'visible', timeout: 30000 })
  await page.locator('[data-pqc-order-summary-card]').click()
  await page.locator('[data-pqc-order-search-input]').fill(manifestOrder.workOrderCode)
  const option = page.locator('[data-pqc-order-option]').filter({
    has: page.locator(`[data-pqc-order-option-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await page.locator('[data-pqc-order-code]').filter({ hasText: manifestOrder.workOrderCode }).waitFor({ state: 'visible', timeout: 30000 })
}

async function selectFrontlinePqcProcess(page, processIndex) {
  await page.locator('[data-pqc-process-current]').click()
  const option = page.locator('[data-pqc-process-option]').nth(processIndex)
  await option.waitFor({ state: 'visible', timeout: 30000 })
  const label = (await option.innerText()).trim()
  await option.click()
  await page.locator('[data-pqc-process-current]').filter({ hasText: label }).waitFor({ state: 'visible', timeout: 30000 })
  return label
}

async function selectPqcInspectionRule(page, round) {
  const ruleTab = page.locator(`[data-pqc-inspection-rule-tab="${round.ruleKey}"]`).first()
  if (!(await ruleTab.count())) {
    throw stageError({
      stage: 'S03',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S03选择PQC检验规则',
      message: `当前PQC工序缺少待执行检验任务：${round.ruleKey}`,
      expected: { ruleKey: round.ruleKey, taskStatus: 'PENDING' },
      actual: { ruleTabVisible: false }
    })
  }
  await ruleTab.click()
  await page.locator('[data-pqc-task-option]').filter({ hasText: await ruleTab.innerText() }).first().waitFor({ state: 'visible', timeout: 30000 })
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
    if (!(await select.isVisible().catch(() => false))) continue
    const currentValue = await select.inputValue().catch(() => '')
    if (currentValue) continue
    const optionValue = await select.locator('option').evaluateAll((options) => {
      const option = options.find((item) => item.value && item.value !== '__OTHER__')
      return option ? option.value : ''
    })
    if (optionValue) await select.selectOption(optionValue)
  }
}

function resolvePqcNumericValue(itemLabel, index) {
  const label = String(itemLabel || '')
  if (/专用|special|Q1|20/.test(label)) return pqcNumericValues[1]
  if (/通用|generic|G1|10/.test(label)) return pqcNumericValues[0]
  return pqcNumericValues[Math.min(index, pqcNumericValues.length - 1)]
}

async function fillActivePqcInspectionItem(page, itemValue) {
  await selectFirstPqcEquipmentWhenAvailable(page)
  const bulkPass = page.locator('[data-pqc-bulk-pass]').first()
  if (await bulkPass.isVisible().catch(() => false)) {
    await bulkPass.click()
    return { inputMode: 'choice', value: '合格' }
  }
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

async function fillPqcInspectionItems(page) {
  const itemTabs = page.locator('[data-pqc-inspection-tab]')
  const itemCount = await itemTabs.count()
  if (!itemCount) {
    throw stageError({
      stage: 'S03',
      errorType: 'PRECONDITION_BLOCKED',
      action: 'S03填写PQC检验项目',
      message: '当前PQC任务缺少检验项目页签',
      expected: { inspectionItemCountAtLeast: 1 },
      actual: { inspectionItemCount: 0 }
    })
  }
  const items = []
  for (let index = 0; index < itemCount; index += 1) {
    const tab = itemTabs.nth(index)
    const itemLabel = (await tab.innerText()).trim()
    await tab.click()
    await page.locator('[data-pqc-active-inspection-panel]').waitFor({ state: 'visible', timeout: 30000 })
    const value = resolvePqcNumericValue(itemLabel, index)
    const filled = await fillActivePqcInspectionItem(page, value)
    items.push({ itemIndex: index, itemLabel, ...filled })
  }
  return items
}

async function submitOnePqcInspectionRound(page, manifestOrder, step) {
  await selectPqcInspectionRule(page, step)
  await fillPqcQuantity(page, step.quantity)
  const items = await fillPqcInspectionItems(page)
  await page.locator('[data-pqc-submit-open-signature]').click()
  const dialog = page.locator('[data-pqc-signature-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-pqc-signature-password]').fill(signaturePassword)
  const submitResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/feedback/frontline/device-account/pqc/submit') && r.request().method() === 'POST')
  await dialog.locator('[data-pqc-submit-confirm-accept]').click()
  const response = await submitResponse
  assert.equal(response.ok(), true, `一线PQC提交失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `一线PQC提交业务失败：${body.msg || 'unknown'}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return {
    workOrderCode: manifestOrder.workOrderCode,
    processIndex: step.processIndex,
    processLabel: step.processLabel,
    ruleKey: step.ruleKey,
    inspectionType: step.type,
    quantity: step.quantity,
    items,
    pqcTaskId: body.data?.pqcTaskId,
    pqcEventId: body.data?.pqcEventId,
    pqcRecordId: body.data?.pqcRecordId
  }
}

async function openPqcReviewWorkbench(page) {
  await page.goto(`${frontendUrl}/mes/pro/process-pool/pqc-leader`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.locator('[data-pqc-leader-workbench-page]').waitFor({ state: 'visible', timeout: 30000 })
  const managementTab = page.locator('[data-pqc-leader-module-tab-management]').first()
  if (await managementTab.isVisible().catch(() => false)) await managementTab.click()
  await page.locator('[data-user-table-key="mes.processPool.teamLeader.submissions"]').waitFor({ state: 'visible', timeout: 30000 })
}

async function reviewPqcInspectionSubmission(page, manifestOrder) {
  await openPqcReviewWorkbench(page)
  const row = page.locator('.el-table__row:visible').filter({
    has: page.locator(`[data-pqc-leader-work-order]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({
    has: page.locator('[data-team-leader-review-event-id]')
  }).first()
  await row.waitFor({ state: 'visible', timeout: 30000 })
  const reviewButton = row.locator('[data-team-leader-review-event-id]').first()
  const eventId = await reviewButton.getAttribute('data-team-leader-review-event-id')
  await reviewButton.click()
  const dialog = page.locator('[data-team-leader-review-dialog]')
  await dialog.waitFor({ state: 'visible', timeout: 30000 })
  await dialog.locator('[data-team-leader-review-signature-password] input').fill(signaturePassword)
  const reviewResponse = page.waitForResponse((r) => r.url().includes('/mes/pro/process-pool/team-leader/submission/review') && r.request().method() === 'POST')
  await dialog.locator('[data-team-leader-review-submit]').click()
  const response = await reviewResponse
  assert.equal(response.ok(), true, `PQC组长复核失败：HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(Number(body.code), 0, `PQC组长复核业务失败：${body.msg || 'unknown'}`)
  await dialog.waitFor({ state: 'hidden', timeout: 30000 })
  return { eventId: requirePositiveIdString(eventId, 'PQC组长复核eventId'), reviewStatus: 'APPROVED' }
}

async function submitPqcInspections(page, manifestOrder, production) {
  await selectFrontlinePqcOrder(page, manifestOrder)
  const submissions = []
  for (const processIndex of [0, 1]) {
    const processLabel = await selectFrontlinePqcProcess(page, processIndex)
    for (const round of PQC_ROUND_PLAN) {
      submissions.push(await submitOnePqcInspectionRound(page, manifestOrder, {
        ...round,
        processIndex,
        processLabel
      }))
    }
  }
  const reviews = []
  for (const ignored of submissions) reviews.push(await reviewPqcInspectionSubmission(page, manifestOrder))
  return {
    activeOrderId: production.activeOrderId,
    pqcSubmissionCount: 8,
    pqcReviewCount: 8,
    expectedTaskCount: 16,
    expectedPieceResultCount: 40,
    submissions,
    reviews
  }
}

function pqcRoundByKey(ruleKey) {
  const round = PQC_ROUND_PLAN.find((item) => item.ruleKey === ruleKey)
  assert.ok(round, `固定PQC轮次不存在：${ruleKey}`)
  return round
}

async function submitOnePqcInspectionForProcess(page, manifestOrder, step) {
  await selectFrontlinePqcOrder(page, manifestOrder)
  const processLabel = await selectFrontlinePqcProcess(page, step.processIndex)
  return submitOnePqcInspectionRound(page, manifestOrder, {
    ...pqcRoundByKey(step.ruleKey),
    processIndex: step.processIndex,
    processLabel
  })
}

async function executeProductionAndPqcInterleaved(page, manifestOrder, activeOrder) {
  const production = {
    activeOrderId: activeOrder.activeOrderId,
    expectedSubmissionCount: 4,
    submissions: [],
    reviews: []
  }
  const pqc = {
    activeOrderId: activeOrder.activeOrderId,
    pqcSubmissionCount: 8,
    pqcReviewCount: 8,
    expectedTaskCount: 16,
    expectedPieceResultCount: 40,
    submissions: [],
    reviews: []
  }
  const executedSteps = []
  for (const step of INTERLEAVED_MAIN_CHAIN_PLAN) {
    if (step.kind === 'PRODUCTION') {
      const submission = await submitOneProductionReport(page, manifestOrder, step)
      const review = await reviewProductionReport(page, manifestOrder)
      production.submissions.push(submission)
      production.reviews.push(review)
      executedSteps.push({
        kind: step.kind,
        processIndex: step.processIndex,
        quantity: step.quantity,
        submissionId: submission.feedbackId,
        reviewEventId: review.eventId
      })
      continue
    }
    if (step.kind === 'PQC') {
      const submission = await submitOnePqcInspectionForProcess(page, manifestOrder, step)
      const review = await reviewPqcInspectionSubmission(page, manifestOrder)
      pqc.submissions.push(submission)
      pqc.reviews.push(review)
      executedSteps.push({
        kind: step.kind,
        processIndex: step.processIndex,
        ruleKey: step.ruleKey,
        pqcTaskId: submission.pqcTaskId,
        reviewEventId: review.eventId
      })
      continue
    }
    throw stageError({
      stage: 'S02',
      errorType: 'TEST_HARNESS_FAILURE',
      action: 'S02/S03固定交错计划',
      message: `未知交错计划动作：${step.kind}`,
      expected: { allowedKinds: ['PRODUCTION', 'PQC'] },
      actual: { step }
    })
  }
  assert.equal(production.submissions.length, 4, '固定主链必须提交4笔生产记录')
  assert.equal(production.reviews.length, 4, '固定主链必须复核4笔生产记录')
  assert.equal(pqc.submissions.length, 8, '固定主链必须提交8笔PQC记录')
  assert.equal(pqc.reviews.length, 8, '固定主链必须复核8笔PQC记录')
  return {
    activeOrderId: activeOrder.activeOrderId,
    interleavedStepCount: INTERLEAVED_MAIN_CHAIN_PLAN.length,
    plan: INTERLEAVED_MAIN_CHAIN_PLAN.map((step) => ({ ...step })),
    executedSteps,
    production,
    pqc
  }
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
  if (!/100/.test(status.productionProgressText) || !/100/.test(status.inspectionProgressText)) {
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

async function approvePqcProductionReleaseS05(page, manifestOrder, completion) {
  await openPqcProductionReleasePageFromWorkTask(page, manifestOrder)
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

function isEdhrBatchHistoryTimelineResponseForBatch(batchExecutionId) {
  return (response) => {
    if (!response.url().includes('/mes/pro/edhr-batch-execution/review-timeline') ||
      response.request().method() !== 'GET') return false
    const parsed = new URL(response.url())
    return parsed.searchParams.get('id') === String(batchExecutionId)
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
  const timelineResponse = page.waitForResponse(isEdhrBatchHistoryTimelineResponseForBatch(finalRelease.batchExecutionId))
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
  const row = page.locator('[data-edhr-history-batch-item]').filter({
    has: page.locator(`[data-edhr-history-work-order-code]:text-is("${manifestOrder.workOrderCode}")`)
  }).filter({
    has: page.locator(`[data-edhr-history-batch-code]:text-is("${manifestOrder.batchCode}")`)
  }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.locator('[data-edhr-history-batch-status]').filter({ hasText: '已归档' }).waitFor({ state: 'visible', timeout: 60000 })
  const timelineBody = await readCommonResult(await timelineResponse, '历史追溯时间线查询失败')
  assert.equal(Number(timelineBody.code), 0, `历史追溯时间线业务失败：${timelineBody.msg || 'unknown'}`)
  const timeline = timelineBody.data || {}
  const archiveVersions = Array.isArray(timeline.archiveVersions) ? timeline.archiveVersions : []
  const matchingArchive = archiveVersions.find((item) => String(item.id) === String(archive.id))
  assert.ok(matchingArchive, `历史追溯时间线缺少本轮归档版本：${archive.id}`)
  assert.equal(matchingArchive.archiveStatus, 'SEALED', `历史追溯归档版本必须为SEALED，实际为${matchingArchive.archiveStatus}`)
  assert.ok(Array.isArray(timeline.taskEvents) && timeline.taskEvents.length > 0, '历史追溯缺少任务事件')
  assert.ok(Array.isArray(timeline.signatureRecords) && timeline.signatureRecords.length > 0, '历史追溯缺少电子签名')
  assert.ok(Array.isArray(timeline.approvalRecords) && timeline.approvalRecords.length > 0, '历史追溯缺少审批记录')
  assert.ok(Array.isArray(timeline.dossierItems) && timeline.dossierItems.length >= 4, '历史追溯缺少四份放行资料目录')
  await page.locator('[data-edhr-history-source-count="归档版本"]').filter({ hasText: /^[1-9]\d*$/ }).waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-edhr-history-dossier-item]').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-edhr-history-process-item]').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('[data-edhr-history-timeline-item]').first().waitFor({ state: 'visible', timeout: 60000 })
  return {
    batchExecutionId: finalRelease.batchExecutionId,
    archiveId: archive.id,
    archiveStatus: matchingArchive.archiveStatus,
    archiveVersion: matchingArchive.archiveVersion,
    taskEventCount: timeline.taskEvents.length,
    signatureCount: timeline.signatureRecords.length,
    approvalCount: timeline.approvalRecords.length,
    dossierCount: timeline.dossierItems.length
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
  const manifest = buildManifest({ runId, mode, templateWorkOrderCode })
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
  const targetRequests = trackTargetRequests(page)
  await context.tracing.start({ screenshots: true, snapshots: true, sources: true })
  try {
    await runWithStage('S00', 'S00登录页与账号会话初始化',
      { loginPageLoaded: true, userAuthenticated: true, tenant: '芋道源码', username }, () => login(page))
    const ready = await runWithStage('VERIFY_READY', 'VERIFY_READY固定母单可复制',
      { sourceWorkOrderCode: manifest.templateWorkOrderCode, sourceQuantity: manifest.expected.finishedQuantity, copyButtonVisible: true },
      () => verifyTemplateReady(page, manifest))
    const activeOrders = []
    for (const item of selectedOrders) {
      activeOrders.push(await runWithStage('S01', 'S01复制固定母单生成活跃测试订单',
        { action: 'COPY_LATEST_VERSION', sourceWorkOrderCode: manifest.templateWorkOrderCode, expectedWorkOrderCodePrefix: item.expectedWorkOrderCodePrefix, aiRunOrderSlotId: item.simulationRunId, actualSimulationRunId: 'SIMCOPY-* from real page action' },
        () => copyTemplateActiveOrder(page, item, manifest, ready)))
    }
    const mainOrder = activeOrders[0]
    const processExecution = await runWithStage('S02', 'S02/S03生产PQC交错执行',
      { productionFeedbackCount: 4, pqcSubmissionCount: 8, interleaved: true }, () => executeProductionAndPqcInterleaved(page, mainOrder, activeOrders[0]))
    const completion = await runWithStage('S04', 'S04领料晚到回填与完工申请',
      { releaseApplicationStatus: 'PQC_RELEASE_PENDING', inputMaterialPickListVisible: true, inputMaterialBatchVisible: true }, () => completeActiveOrderAndApplyRelease(page, mainOrder))
    const pqcRelease = await runWithStage('S05', 'S05PQC生产放行',
      { status: 'REPORT_UPLOAD_PENDING', reportUploadTaskCount: 4 }, () => approvePqcProductionReleaseS05(page, mainOrder, completion))
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
      expected: { businessStages: ['S01', 'S02', 'S03', 'S04', 'S05', 'S06', 'S07', 'S08'], fullModeOrderSlots: ['O01'], s01Action: 'COPY_LATEST_VERSION', sourceWorkOrderCode: manifest.templateWorkOrderCode, finishedQuantity: mainOrder.quantity, productionFeedbackCount: 4, pqcSubmissionCount: 8, pqcReviewCount: 8, pqcTaskCount: 16, pqcPieceResultCount: 40, releaseApplicationStatus: 'PQC_RELEASE_PENDING', pqcReleaseStatus: 'REPORT_UPLOAD_PENDING', reportUploadTaskCount: 4, reportUploadCompletedCount: 4, finalReleasePending: true, finalReleaseStatus: 'RELEASED', archiveStatus: 'SEALED', historyStatus: 40, expectedFormalLoss: 0 },
      actual: { preparedBySimulationCopy: true, copiedOrderSlots: activeOrders.map((order) => order.slot), copiedOrderCount: activeOrders.length, readyVerified: ready.readyVerified, activeOrders, processExecution, production: processExecution.production, pqc: processExecution.pqc, completion, pqcRelease, reportUpload, finalRelease, archiveTrace },
      message: 'S01已通过固定母单复制生成本轮测试活跃订单；S02/S03已按固定计划交错完成一线生产、生产组长FIFO复核、一线PQC及PQC组长复核；S04已完成领料晚到回填与无补料确认；S05已完成PQC生产放行；S06已完成四份资料上传；S07已完成管理者代表最终放行；S08已完成最终归档并在历史追溯页验证归档版本、时间线和放行资料目录。',
      pageUrl: page.url(), screenshot: null, trace: 'trace.zip', targetRequests, targetRequestEvidenceFlushed: true, candidateCodePaths: [],
      stages: stageResults(null, 'PASS'), erpOrders, ready, startedAt: JSON.parse(fs.readFileSync(path.join(runDir, 'result.json'), 'utf8')).startedAt,
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
      errorType: classification.errorType, action: error.action || '固定母单复制准备',
      expected: error.expected || { templateWorkOrderCode, mode, fullModeOrderSlots: ordersForMode(manifest).map((order) => order.slot) },
      actual: error.actual || { message: error.message },
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







