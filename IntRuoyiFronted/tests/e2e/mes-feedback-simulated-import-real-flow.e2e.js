const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(`Playwright is required for MES feedback simulated import E2E: ${error.message}`)
  }
}

const configuredProcessCount = Number(process.env.MES_FEEDBACK_SIMULATED_E2E_PROCESS_COUNT || '2')
if (!Number.isInteger(configuredProcessCount) || configuredProcessCount < 1 || configuredProcessCount > 20) {
  throw new Error('MES_FEEDBACK_SIMULATED_E2E_PROCESS_COUNT 必须是 1 到 20 的整数。')
}

const config = {
  baseUrl: (process.env.MES_FEEDBACK_SIMULATED_E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_FEEDBACK_SIMULATED_E2E_TENANT || '测试租户',
  username: process.env.MES_FEEDBACK_SIMULATED_E2E_USERNAME || 'aoteman',
  password: process.env.MES_FEEDBACK_SIMULATED_E2E_PASSWORD || '111111',
  processCount: configuredProcessCount,
  headed: process.env.MES_FEEDBACK_SIMULATED_E2E_HEADED === '1'
}

function round6(value) {
  return Number(Number(value).toFixed(6))
}

async function fetchApiJson(page, pathname, authHeaders = {}) {
  const result = await page.evaluate(async ({ relativePath, authHeaders: nextHeaders }) => {
    const response = await fetch(relativePath, {
      method: 'GET',
      credentials: 'include',
      headers: nextHeaders
    })
    const body = await response.json()
    return {
      ok: response.ok,
      status: response.status,
      body
    }
  }, { relativePath: pathname, authHeaders })
  assert.ok(result.ok, `接口请求失败 ${pathname}: HTTP ${result.status}`)
  assert.equal(result.body.code, 0, `接口业务失败 ${pathname}: ${result.body.msg || result.body.code}`)
  return result.body.data
}

async function fetchScheduleOrderSnapshot(page, scheduleOrderId, authHeaders) {
  const [scheduleOrder, processes] = await Promise.all([
    fetchApiJson(page, `/admin-api/mes/pro/schedule-order/get?id=${scheduleOrderId}`, authHeaders),
    fetchApiJson(
      page,
      `/admin-api/mes/pro/schedule-order/process-list?scheduleOrderId=${scheduleOrderId}`,
      authHeaders
    )
  ])
  assert.ok(scheduleOrder, `排产工单 ${scheduleOrderId} 必须存在。`)
  assert.ok(Array.isArray(processes), `排产工单 ${scheduleOrderId} 工序快照必须返回数组。`)
  return { scheduleOrder, processes }
}

function calculateAverageProcessProgress(processes) {
  const enabledProcesses = processes.filter((process) => process.enabled !== false)
  assert.ok(enabledProcesses.length > 0, '排产工单必须存在至少一个启用工序。')
  const summedProgress = enabledProcesses.reduce((sum, process) => {
    const plannedQuantity = Number(process.plannedQuantity || 0)
    const reportedQuantity = Math.min(Number(process.reportedQuantity || 0), plannedQuantity)
    if (!(plannedQuantity > 0)) {
      return sum
    }
    return sum + (reportedQuantity * 100) / plannedQuantity
  }, 0)
  return round6(summedProgress / enabledProcesses.length)
}

function assertScheduleOrderSummaryMatchesAverage(scheduleOrder, processes, label) {
  const expectedProgressPercent = calculateAverageProcessProgress(processes)
  const totalQuantity = Number(scheduleOrder.totalQuantity ?? scheduleOrder.quantity ?? 0)
  const completedQuantity = Number(scheduleOrder.completedQuantity ?? 0)
  const uncompletedQuantity = Number(scheduleOrder.uncompletedQuantity ?? 0)
  const progressPercent = Number(scheduleOrder.progressPercent ?? 0)
  const expectedCompletedQuantity = round6((totalQuantity * expectedProgressPercent) / 100)
  const expectedUncompletedQuantity = round6(Math.max(totalQuantity - expectedCompletedQuantity, 0))

  assert.equal(
    round6(progressPercent),
    expectedProgressPercent,
    `${label} 排产工单总进度必须等于所有启用工序完成率的平均值。`
  )
  assert.equal(
    round6(completedQuantity),
    expectedCompletedQuantity,
    `${label} 已完成数量必须按总进度折算。`
  )
  assert.equal(
    round6(uncompletedQuantity),
    expectedUncompletedQuantity,
    `${label} 未完成数量必须等于总数量减已完成数量。`
  )
}

async function waitForScheduleOrderSnapshotChange(
  page,
  scheduleOrderId,
  scheduleOrderProcessId,
  previousReportedQuantity,
  authHeaders
) {
  const timeoutAt = Date.now() + 60000
  while (Date.now() < timeoutAt) {
    const snapshot = await fetchScheduleOrderSnapshot(page, scheduleOrderId, authHeaders)
    const targetProcess = snapshot.processes.find(
      (process) => String(process.id) === String(scheduleOrderProcessId)
    )
    if (targetProcess && round6(Number(targetProcess.reportedQuantity || 0)) !== round6(previousReportedQuantity)) {
      return snapshot
    }
    await page.waitForTimeout(1000)
  }
  throw new Error(`等待排产工单 ${scheduleOrderId} 工序 ${scheduleOrderProcessId} 进度变化超时。`)
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
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('form.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input.el-input__inner').nth(0), config.username, 'username')
  await loginForm.locator('input[type="password"]').first().fill(config.password)

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    page.getByRole('button', { name: '登录' }).click()
  ])
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `登录 HTTP 失败: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openFeedbackPage(page) {
  await page.goto(`${config.baseUrl}/mes/pro/feedback`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.getByRole('button', { name: /模拟报工/ }).waitFor({ state: 'visible', timeout: 60000 })
}

async function simulateImport(page) {
  await page.getByRole('button', { name: /模拟报工/ }).click()
  const processCountDialog = page.locator('.el-message-box:visible').filter({ hasText: '模拟工序数量' }).last()
  await processCountDialog.waitFor({ state: 'visible', timeout: 60000 })
  await processCountDialog.locator('input').first().fill(String(config.processCount))
  const [simulateResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/feedback/simulate-import-third-party-xlsx') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    processCountDialog.getByRole('button', { name: /确定/ }).click()
  ])
  const simulateBody = await simulateResponse.json()
  assert.equal(simulateBody.code, 0, `模拟报工失败: ${JSON.stringify(simulateBody)}`)
  assert.equal(simulateBody.data.importedCount, config.processCount, '模拟报工必须按输入数量导入待归属记录。')
  assert.equal(simulateBody.data.pendingCount, config.processCount, '模拟报工必须按输入数量生成待归属记录。')
  const importRecordIds = simulateBody.data.importRecordIds || []
  assert.equal(importRecordIds.length, config.processCount, '模拟报工必须返回每条待归属记录 ID。')
  assert.equal(new Set(importRecordIds).size, config.processCount, '模拟报工返回的待归属记录 ID 必须唯一。')
  const importRecordId = importRecordIds[0]
  assert.ok(importRecordId, '模拟报工必须返回待归属记录 ID。')

  const alertDialog = page.locator('.el-message-box:visible').filter({ hasText: '模拟报工完成' }).last()
  if ((await alertDialog.count()) > 0 && (await alertDialog.isVisible())) {
    await alertDialog.getByRole('button', { name: /确定/ }).click()
  }
  const pendingTab = page.getByRole('tab', { name: '待归属' })
  await pendingTab.waitFor({ state: 'visible', timeout: 60000 })
  await pendingTab.click()
  await page.waitForTimeout(1500)
  await page.locator('tr.el-table__row').first().waitFor({ state: 'visible', timeout: 60000 })
  return importRecordIds
}

async function attributeImportRecord(page, importRecordId) {
  const rows = page.locator('tr.el-table__row')
  const row = config.processCount === 1 ? rows.first() : rows.filter({ hasText: String(importRecordId) }).first()
  await row.waitFor({ state: 'visible', timeout: 60000 })

  const [candidateResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/feedback/import-record/candidates') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    ),
    row.getByText('选择归属').click()
  ])
  const candidateBody = await candidateResponse.json()
  assert.equal(candidateBody.code, 0, `归属候选接口失败: ${candidateBody.msg || candidateBody.code}`)
  assert.ok(candidateBody.data.length > 0, '模拟导入记录必须存在可选择的归属候选。')
  const candidateRequestHeaders = await candidateResponse.request().allHeaders()
  const authHeaders = {}
  for (const headerName of ['authorization', 'tenant-id', 'visit-tenant-id']) {
    if (candidateRequestHeaders[headerName]) {
      authHeaders[headerName] = candidateRequestHeaders[headerName]
    }
  }

  const selected = candidateBody.data[0]
  const remainingQuantity = Number(selected.remainingQuantity)
  assert.ok(Number.isFinite(remainingQuantity) && remainingQuantity > 0, '归属候选剩余数量必须大于 0。')
  const beforeSnapshot = await fetchScheduleOrderSnapshot(page, selected.scheduleOrderId, authHeaders)
  const beforeTargetProcess = beforeSnapshot.processes.find(
    (process) => String(process.id) === String(selected.scheduleOrderProcessId)
  )
  assert.ok(beforeTargetProcess, '归属前必须能定位目标排产工序。')
  assertScheduleOrderSummaryMatchesAverage(beforeSnapshot.scheduleOrder, beforeSnapshot.processes, '归属前')

  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '确认归属' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60000 })
  const quantityInput = dialog.locator('.el-input-number input').first()
  await dialog.getByRole('button', { name: /^全部$/ }).first().click()
  await quantityInput.waitFor({ state: 'visible', timeout: 60000 })
  const feedbackQuantity = Number(await quantityInput.inputValue())
  assert.ok(Number.isFinite(feedbackQuantity) && feedbackQuantity > 0, '点击全部后必须回填有效的本次工序完成数量。')

  const [attributeResponse] = await Promise.all([
    page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/feedback/import-record/attribute') &&
        response.request().method() === 'POST',
      { timeout: 60000 }
    ),
    dialog.getByRole('button', { name: /确认归属/ }).click()
  ])
  const attributeBody = await attributeResponse.json()
  assert.equal(attributeBody.code, 0, `确认归属失败: ${attributeBody.msg || attributeBody.code}`)
  assert.ok(attributeBody.data, '确认归属必须返回正式报工 ID。')
  return {
    feedbackId: attributeBody.data,
    feedbackQuantity,
    selected,
    beforeSnapshot,
    beforeTargetProcess,
    authHeaders
  }
}

async function main() {
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  try {
    page.setDefaultTimeout(60000)
    await login(page)
    await openFeedbackPage(page)
    const importRecordIds = await simulateImport(page)
    const importRecordId = importRecordIds[0]
    const { feedbackId, feedbackQuantity, selected, beforeSnapshot, beforeTargetProcess, authHeaders } =
      await attributeImportRecord(page, importRecordId)
    const afterSnapshot = await waitForScheduleOrderSnapshotChange(
      page,
      selected.scheduleOrderId,
      selected.scheduleOrderProcessId,
      Number(beforeTargetProcess.reportedQuantity || 0),
      authHeaders
    )
    const afterTargetProcess = afterSnapshot.processes.find(
      (process) => String(process.id) === String(selected.scheduleOrderProcessId)
    )
    assert.ok(afterTargetProcess, '归属后必须能定位目标排产工序。')
    assert.equal(
      round6(Number(afterTargetProcess.reportedQuantity || 0)),
      round6(Number(beforeTargetProcess.reportedQuantity || 0) + feedbackQuantity),
      '归属后目标工序已报工数量必须按本次完成数量增加。'
    )
    assert.equal(
      round6(Number(afterTargetProcess.remainingQuantity || 0)),
      round6(Math.max(Number(beforeTargetProcess.remainingQuantity || 0) - feedbackQuantity, 0)),
      '归属后目标工序剩余数量必须按本次完成数量减少。'
    )
    assertScheduleOrderSummaryMatchesAverage(afterSnapshot.scheduleOrder, afterSnapshot.processes, '归属后')
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          username: config.username,
          processCount: config.processCount,
          importRecordIds,
          importRecordId,
          feedbackId,
          feedbackQuantity,
          scheduleOrderId: selected.scheduleOrderId,
          scheduleOrderProcessId: selected.scheduleOrderProcessId,
          taskCode: selected.taskCode,
          beforeProgressPercent: beforeSnapshot.scheduleOrder.progressPercent,
          afterProgressPercent: afterSnapshot.scheduleOrder.progressPercent,
          enabledProcessCount: afterSnapshot.processes.filter((process) => process.enabled !== false).length
        },
        null,
        2
      )
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
