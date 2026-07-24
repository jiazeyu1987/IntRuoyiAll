const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: (process.env.MES_REPLAN_FEEDBACK_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  backendUrl: (process.env.MES_REPLAN_FEEDBACK_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, ''),
  tenant: process.env.MES_REPLAN_FEEDBACK_TENANT || '\u6d4b\u8bd5\u79df\u6237',
  username: process.env.MES_REPLAN_FEEDBACK_USERNAME || 'aoteman',
  password: process.env.MES_REPLAN_FEEDBACK_PASSWORD || '111111',
  headed: process.env.MES_REPLAN_FEEDBACK_HEADED === '1',
  artifactDir:
    process.env.MES_REPLAN_FEEDBACK_ARTIFACT_DIR ||
    path.resolve('tests/output/mes-replan-feedback-remaining-readonly')
}

function todayStart() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day} 00:00:00`
}

async function settle(page) {
  await page.waitForLoadState('domcontentloaded', { timeout: 30000 })
  await page.waitForTimeout(500)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`No visible input found for ${label}`)
}

async function login(page) {
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  const captchaCount = await page
    .locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="\u8bf7\u8f93\u5165\u9a8c\u8bc1\u7801"]:visible')
    .count()
  assert.equal(captchaCount, 0, '\u767b\u5f55\u9875\u9a8c\u8bc1\u7801\u5df2\u5f00\u542f\uff0c\u65e0\u6cd5\u65e0\u4eba\u5de5\u8f93\u5165\u590d\u8dd1\u771f\u5b9e E2E\u3002')

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(form.locator('input.el-input__inner'), config.tenant, 'tenant')
  }
  await form.locator('input.el-input__inner').nth(0).fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '\u767b\u5f55' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
}

async function authHeaders(page) {
  const cache = await page.evaluate(() => ({
    accessToken: localStorage.getItem('ACCESS_TOKEN'),
    tenantId: localStorage.getItem('tenantId') || localStorage.getItem('TENANT_ID')
  }))
  assert.ok(cache.accessToken, 'logged-in context missing ACCESS_TOKEN')
  let accessToken = cache.accessToken.trim()
  if (accessToken.startsWith('{')) {
    const parsed = JSON.parse(accessToken)
    accessToken = typeof parsed?.v === 'string' ? JSON.parse(parsed.v) : parsed?.v
  }
  assert.ok(accessToken, 'ACCESS_TOKEN cache did not contain token value')
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': cache.tenantId || '122'
  }
}

async function apiGetJson(page, urlPath, params = {}) {
  const search = new URLSearchParams()
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value))
    }
  }
  const response = await page.request.get(
    `${config.baseUrl}/admin-api${urlPath}${search.size ? `?${search.toString()}` : ''}`,
    { headers: await authHeaders(page) }
  )
  const text = await response.text()
  assert.equal(response.status(), 200, `${urlPath} HTTP ${response.status()}: ${text}`)
  const body = JSON.parse(text)
  assert.equal(body.code, 0, `${urlPath} business error: ${body.msg || JSON.stringify(body)}`)
  return body.data
}

async function apiPostJson(page, urlPath, data) {
  const response = await page.request.post(`${config.backendUrl}/admin-api${urlPath}`, {
    headers: await authHeaders(page),
    data
  })
  const text = await response.text()
  assert.equal(response.status(), 200, `${urlPath} HTTP ${response.status()}: ${text}`)
  const body = JSON.parse(text)
  assert.equal(body.code, 0, `${urlPath} business error: ${body.msg || JSON.stringify(body)}`)
  return body.data
}

async function findFeedbackRemainingCandidate(page) {
  const inspected = []
  for (let pageNo = 1; pageNo <= 8; pageNo += 1) {
    const data = await apiGetJson(page, '/mes/pro/schedule-order/page', {
      pageNo,
      pageSize: 50,
      completionFilter: 'INCOMPLETE'
    })
    const rows = data?.list || []
    for (const row of rows) {
      if (!row?.id || !row.workOrderId || row.frozen || row.manualFinished) {
        continue
      }
      const processData = await apiGetJson(page, '/mes/pro/schedule-order/process-list', {
        scheduleOrderId: row.id
      })
      const processes = processData?.list || processData || []
      const feedbackProcess = processes.find(
        (item) => Number(item.reportedQuantity || 0) > 0 && Number(item.remainingQuantity || 0) > 0
      )
      inspected.push({
        scheduleOrderId: row.id,
        code: row.code,
        workOrderId: row.workOrderId,
        feedbackProcessCount: processes.filter(
          (item) => Number(item.reportedQuantity || 0) > 0 && Number(item.remainingQuantity || 0) > 0
        ).length
      })
      if (!feedbackProcess) {
        continue
      }
      const request = {
        scopeType: 'SELECTED',
        scheduleOrderIds: [row.id],
        startTime: todayStart(),
        runtimeCapacityBasis: 'PLANNED',
        preserveManualLockedTasks: true
      }
      const preflight = await apiPostJson(page, '/mes/pro/schedule-order/preflight', {
        scopeType: request.scopeType,
        scheduleOrderIds: request.scheduleOrderIds,
        includeAdmissionDiff: false,
        startTime: request.startTime,
        capacityMode: request.runtimeCapacityBasis
      })
      if (preflight?.result === 'BLOCKED') {
        continue
      }
      const preview = await apiPostJson(page, '/mes/pro/auto-schedule/replan/preview', request)
      const feedbackProtectedTasks = (preview?.protectedTasks || []).filter(
        (item) => item.protectionReason === 'FEEDBACK'
      )
      const generatedPreviewTasks = (preview?.tasks || []).filter(
        (item) => item.id && String(item.id).includes('_preview_') && Number(item.quantity || 0) > 0
      )
      const exactRemainingTask = generatedPreviewTasks.find(
        (item) => Number(item.quantity || 0) === Number(feedbackProcess.remainingQuantity || 0)
      )
      if (feedbackProtectedTasks.length > 0 && exactRemainingTask) {
        return {
          row,
          feedbackProcess,
          request,
          preview,
          feedbackProtectedTasks,
          exactRemainingTask,
          inspected
        }
      }
    }
    if (rows.length < 50) {
      break
    }
  }
  throw new Error(
    `\u672a\u627e\u5230\u53ef\u7528\u4e8e\u53ea\u8bfb\u91cd\u6392\u9884\u89c8\u7684\u771f\u5b9e\u5df2\u62a5\u5de5\u5269\u4f59\u91cf\u6392\u4ea7\u5de5\u5355\uff0cinspected=${JSON.stringify(
      inspected.slice(0, 30),
      null,
      2
    )}`
  )
}

async function openScheduleOrderPage(page) {
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const response = await responsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `schedule-order page api failed: ${body.msg || body.code}`)
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 60000 })
}

async function selectScheduleOrder(page, candidate) {
  const scheduleCodeInput = page.locator('input[placeholder="\u8bf7\u8f93\u5165\u6392\u4ea7\u7f16\u7801"]').first()
  await scheduleCodeInput.fill(candidate.row.code)
  const workOrderCodeInput = page.locator('input[placeholder="\u8bf7\u8f93\u5165\u5de5\u5355\u7f16\u7801"]').first()
  if ((await workOrderCodeInput.count()) > 0) {
    await workOrderCodeInput.fill('')
  }
  const listPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/page') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '\u641c\u7d22' }).first().click()
  const listBody = await (await listPromise).json()
  assert.equal(listBody.code, 0, `schedule-order filter failed: ${listBody.msg || listBody.code}`)
  const rows = listBody.data?.list || []
  const rowIndex = rows.findIndex((item) => item.id === candidate.row.id)
  assert.notEqual(rowIndex, -1, `filtered page did not contain schedule order ${candidate.row.code}`)
  const tableRow = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr').nth(rowIndex)
  await tableRow.waitFor({ state: 'visible', timeout: 60000 })
  await tableRow.locator('.el-checkbox').first().click()
}

async function previewThroughUi(page, candidate) {
  const previewResponses = []
  page.on('response', async (response) => {
    if (
      response.url().includes('/admin-api/mes/pro/auto-schedule/replan/preview') &&
      response.request().method() === 'POST'
    ) {
      try {
        previewResponses.push(await response.json())
      } catch (error) {
        previewResponses.push({ error: error.message })
      }
    }
  })

  await page.getByRole('button', { name: /\u624b\u52a8\u91cd\u6392/ }).first().click()
  const drawer = page.locator('.el-drawer').filter({ hasText: '\u6392\u4ea7\u524d\u68c0\u67e5 / \u624b\u52a8\u91cd\u6392' }).first()
  await drawer.waitFor({ state: 'visible', timeout: 60000 })
  const previewPromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/auto-schedule/replan/preview') &&
      response.request().method() === 'POST' &&
      response.status() === 200,
    { timeout: 120000 }
  )
  await drawer.getByRole('button', { name: /\u9884\u89c8\u91cd\u6392/ }).first().click()
  const response = await previewPromise
  const body = await response.json()
  assert.equal(body.code, 0, `UI replan preview business error: ${body.msg || body.code}`)
  const preview = body.data
  await drawer.getByText('\u62a5\u5de5\u4fdd\u62a4', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await drawer.getByText(String(candidate.feedbackProcess.remainingQuantity).replace(/\.0+$/, ''), { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  return { preview, previewResponses }
}

async function main() {
  fs.mkdirSync(config.artifactDir, { recursive: true })
  const launchOptions = {
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    await openScheduleOrderPage(page)
    const candidate = await findFeedbackRemainingCandidate(page)
    await selectScheduleOrder(page, candidate)
    const uiResult = await previewThroughUi(page, candidate)

    const feedbackProtectedTasks = (uiResult.preview?.protectedTasks || []).filter(
      (item) => item.protectionReason === 'FEEDBACK'
    )
    const generatedPreviewTasks = (uiResult.preview?.tasks || []).filter(
      (item) => item.id && String(item.id).includes('_preview_') && Number(item.quantity || 0) > 0
    )
    const remainingQuantity = Number(candidate.feedbackProcess.remainingQuantity || 0)
    const exactRemainingTask = generatedPreviewTasks.find(
      (item) => Number(item.quantity || 0) === remainingQuantity
    )

    assert.ok(feedbackProtectedTasks.length > 0, 'UI preview must return FEEDBACK protected task rows')
    assert.ok(exactRemainingTask, `UI preview must generate a new preview task for remaining quantity ${remainingQuantity}`)

    const evidence = {
      tenant: config.tenant,
      username: config.username,
      scheduleOrder: {
        id: candidate.row.id,
        code: candidate.row.code,
        workOrderId: candidate.row.workOrderId,
        workOrderCode: candidate.row.erpWorkOrderCode
      },
      feedbackProcess: candidate.feedbackProcess,
      request: candidate.request,
      summary: uiResult.preview.summary,
      feedbackProtectedTaskCount: feedbackProtectedTasks.length,
      generatedPreviewTask: exactRemainingTask,
      generatedPreviewTaskCount: generatedPreviewTasks.length,
      note: '\u53ea\u8bfb E2E\uff1a\u771f\u5b9e\u767b\u5f55\u3001\u771f\u5b9e\u9875\u9762\u70b9\u51fb\u9884\u89c8\u91cd\u6392\uff0c\u672a\u70b9\u51fb\u5e94\u7528\u91cd\u6392\u3002'
    }
    fs.writeFileSync(
      path.join(config.artifactDir, 'replan-feedback-remaining-evidence.json'),
      JSON.stringify(evidence, null, 2),
      'utf8'
    )
    await page.screenshot({
      path: path.join(config.artifactDir, 'replan-feedback-remaining-preview.png'),
      fullPage: true
    })
    console.log(
      `GREEN: replan feedback remaining readonly E2E -> PASS, scheduleOrder=${candidate.row.code}, remaining=${remainingQuantity}, protected=${feedbackProtectedTasks.length}, generatedTaskQuantity=${exactRemainingTask.quantity}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
