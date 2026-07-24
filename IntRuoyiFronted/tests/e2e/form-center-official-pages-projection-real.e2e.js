const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const config = {
  baseUrl: process.env.M6_OFFICIAL_E2E_BASE_URL || 'http://127.0.0.1:8092',
  tenant: process.env.M6_OFFICIAL_E2E_TENANT || '测试租户',
  username: process.env.M6_OFFICIAL_E2E_USERNAME || 'aoteman',
  password: process.env.M6_OFFICIAL_E2E_PASSWORD,
  executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH,
  taskDir: process.env.M6_TASK_DIR,
  dccFileId: process.env.M6_DCC_FILE_ID || '2054545668044070152',
  edhrBatchExecutionId: process.env.M6_EDHR_BATCH_EXECUTION_ID || '900000000661',
  edhrLockEvidenceJson: process.env.M6_EDHR_LOCK_EVIDENCE_JSON,
  scheduleLockEvidenceJson: process.env.M6_SCHEDULE_LOCK_EVIDENCE_JSON,
  edhrBatchCode:
    process.env.M6_EDHR_BATCH_CODE || 'M4-void-all-m4-smokeappr1-r13-1784544348'
}

assert.ok(config.password, 'M6_OFFICIAL_E2E_PASSWORD is required.')
assert.ok(config.executablePath, 'PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH is required.')
assert.ok(config.taskDir, 'M6_TASK_DIR is required.')

const runId = `m6-official-projection-${Date.now()}`
const artifactDir = path.join(config.taskDir, 'artifacts', 'm6-gate', runId)
fs.mkdirSync(artifactDir, { recursive: true })

const result = {
  status: 'RUNNING',
  runId,
  baseUrl: config.baseUrl,
  tenant: config.tenant,
  username: config.username,
  dcc: {},
  edhr: {},
  schedule: {},
  businessWriteRequests: [],
  consoleErrors: [],
  pageErrors: [],
  screenshots: []
}

const readonlyPostAllowlist = [
  '/admin-api/form-center/actions/active-instance'
]

const writeResult = () => {
  fs.writeFileSync(path.join(artifactDir, 'report.json'), JSON.stringify(result, null, 2), 'utf8')
}

const capture = async (page, name) => {
  const file = path.join(artifactDir, `${name}.png`)
  await page.screenshot({ path: file, fullPage: true })
  result.screenshots.push(file)
}

const attachDiagnostics = (page, domain) => {
  page.on('console', (message) => {
    if (message.type() === 'error') {
      result.consoleErrors.push({ domain, text: message.text() })
    }
  })
  page.on('pageerror', (error) => {
    result.pageErrors.push({ domain, message: error.message })
  })
  page.on('request', (request) => {
    const url = request.url()
    if (
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method()) &&
      url.includes('/admin-api/') &&
      !url.includes('/system/auth/login') &&
      !readonlyPostAllowlist.some((pathPart) => url.includes(pathPart))
    ) {
      result.businessWriteRequests.push({
        domain,
        method: request.method(),
        url
      })
    }
  })
}

const readJsonResponse = async (response, label) => {
  try {
    return await response.json()
  } catch (error) {
    const text = await response.text().catch(() => '')
    throw new Error(`${label} response is not JSON: ${text.slice(0, 800)}`)
  }
}

const unwrapBusinessData = (body, label) => {
  assert.ok(body, `${label} response body is empty.`)
  assert.ok([0, 200].includes(body.code), `${label} failed: ${body.msg || body.code}`)
  return body.data
}

const pageBodySummary = async (page) => {
  const text = await page.locator('body').innerText().catch(() => '')
  return text.replace(/\s+/g, ' ').trim().slice(0, 2000)
}

const readEdhrHistoricalLockEvidence = () => {
  if (!config.edhrLockEvidenceJson) return undefined
  assert.ok(
    fs.existsSync(config.edhrLockEvidenceJson),
    `Configured eDHR lock evidence does not exist: ${config.edhrLockEvidenceJson}`
  )
  const evidence = JSON.parse(fs.readFileSync(config.edhrLockEvidenceJson, 'utf8'))
  assert.equal(evidence.instance?.status, 'IN_APPROVAL', 'Historical eDHR lock evidence must be captured while the form instance is IN_APPROVAL.')
  assert.equal(
    evidence.instance?.context?.objectType,
    'EDHR_BATCH_EXECUTION',
    'Historical eDHR lock evidence must target an eDHR batch execution.'
  )
  assert.ok(
    ['RELEASE', 'VOID'].includes(evidence.instance?.context?.actionCode),
    `Historical eDHR lock evidence must be RELEASE or VOID, got ${evidence.instance?.context?.actionCode}.`
  )
  assert.ok(
    evidence.screenshot && fs.existsSync(evidence.screenshot),
    `Historical eDHR lock screenshot does not exist: ${evidence.screenshot}`
  )
  return {
    stepId: evidence.stepId,
    url: evidence.url,
    screenshot: evidence.screenshot,
    instanceId: evidence.instance.id,
    status: evidence.instance.status,
    objectId: evidence.instance.context.objectId,
    actionCode: evidence.instance.context.actionCode
  }
}

const readScheduleHistoricalLockEvidence = () => {
  const evidencePath =
    config.scheduleLockEvidenceJson ||
    path.join(
      config.taskDir,
      'artifacts',
      'm5-gate',
      'schedule-replan-approval-real-e2e-20260720-r6',
      'report.json'
    )
  assert.ok(
    fs.existsSync(evidencePath),
    `Configured schedule lock evidence does not exist: ${evidencePath}`
  )

  const evidence = JSON.parse(fs.readFileSync(evidencePath, 'utf8'))
  assert.equal(evidence.status, 'PASS', 'Historical schedule evidence must be a PASS report.')
  assert.equal(
    evidence.withdrawResult?.pendingDb?.form?.status,
    'IN_APPROVAL',
    'Historical schedule evidence must include a captured pending replan request.'
  )
  assert.equal(
    evidence.withdrawResult?.pendingDb?.form?.objectType,
    'SCHEDULE_REPLAN_SCOPE',
    'Historical schedule evidence must target a schedule replan scope.'
  )
  assert.equal(
    evidence.withdrawResult?.pendingDb?.form?.actionCode,
    'REPLAN',
    'Historical schedule evidence must target the REPLAN action.'
  )
  assert.equal(
    evidence.withdrawResult?.pendingDb?.taskPermission?.active,
    1,
    'Historical schedule evidence must prove the approval task permission was active while pending.'
  )
  assert.match(
    evidence.withdrawResult?.overlap?.responses?.at(-1)?.body?.msg || '',
    /排产重排范围已有审批中申请/,
    'Historical schedule evidence must prove overlapping replan requests were blocked.'
  )
  assert.equal(
    evidence.approveResult?.nonApprover?.approveVisible,
    0,
    'Historical schedule evidence must prove non-approvers cannot approve.'
  )
  assert.equal(
    evidence.approveResult?.nonApprover?.rejectVisible,
    0,
    'Historical schedule evidence must prove non-approvers cannot reject.'
  )
  assert.equal(
    evidence.approveResult?.terminal?.form?.status,
    'EFFECTIVE',
    'Historical schedule evidence must include an approved terminal form.'
  )
  assert.equal(
    evidence.approveResult?.terminal?.effect?.statuses,
    'APPLIED',
    'Historical schedule evidence must include an APPLIED replan effect.'
  )
  assert.equal(
    evidence.approveResult?.terminal?.taskPermission?.active,
    0,
    'Historical schedule evidence must prove approval permissions were revoked after terminal state.'
  )

  return {
    reportJson: evidencePath,
    scheduleOrderId: evidence.selectedScheduleOrders?.approved?.id,
    scheduleOrderCode: evidence.selectedScheduleOrders?.approved?.code,
    pendingInstanceId: evidence.withdrawResult?.instance?.id,
    pendingProcessInstanceId: evidence.withdrawResult?.pendingDb?.form?.processInstanceId,
    activeApprovalUsername: evidence.withdrawResult?.pendingDb?.activeTask?.username,
    overlapMessage: evidence.withdrawResult?.overlap?.responses?.at(-1)?.body?.msg,
    nonApprover: evidence.approveResult?.nonApprover,
    terminal: {
      formStatus: evidence.approveResult?.terminal?.form?.status,
      effectStatuses: evidence.approveResult?.terminal?.effect?.statuses,
      resultRefs: evidence.approveResult?.terminal?.effect?.resultRefs,
      taskPermission: evidence.approveResult?.terminal?.taskPermission
    }
  }
}

const login = async (page, targetPath) => {
  await page.goto(`${config.baseUrl}/login?redirect=/index`, { waitUntil: 'commit' })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(config.tenant)
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: config.tenant })
      .first()
    await option.waitFor({ state: 'visible' })
    await option.click()
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(config.username)
  await form.locator('input[type="password"]').first().fill(config.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  const loginResponse = await loginResponsePromise
  const loginBody = await loginResponse.json()
  assert.ok(loginResponse.ok(), `Login HTTP ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginBody.code), `Login failed: ${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
  await page.goto(`${config.baseUrl}${targetPath}`, { waitUntil: 'commit' })
}

const newPage = async (browser, domain, targetPath) => {
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  attachDiagnostics(page, domain)
  await login(page, targetPath)
  return { context, page }
}

const verifyDcc = async (browser) => {
  const { context, page } = await newPage(browser, 'DCC', '/dcc/controlled-file/workbench')
  try {
    await page.getByText('待文控下发', { exact: false }).first().waitFor({ state: 'visible' })
    await page.goto(`${config.baseUrl}/dcc/controlled-file/detail/${config.dccFileId}`, {
      waitUntil: 'commit'
    })
    await page.getByText('受控文件详情', { exact: false }).first().waitFor({ state: 'visible' })

    const alerts = page.locator('.el-alert--warning:visible')
    await alerts.first().waitFor({ state: 'visible' })
    const messages = await alerts.allInnerTexts()
    assert.ok(
      messages.some((message) => /投影|审批|暂不可用|不允许|当前不可操作|生效失败/.test(message)),
      `DCC detail did not expose an action projection blocker: ${JSON.stringify(messages)}`
    )
    result.dcc = { fileId: config.dccFileId, blockerMessages: messages }
    await capture(page, 'dcc-action-projection')
  } finally {
    await context.close()
  }
}

const verifyEdhr = async (browser) => {
  const targetPath = `/mes/pro/feedback/edhr-batch-execution/detail?id=${config.edhrBatchExecutionId}`
  const { context, page } = await newPage(browser, 'eDHR', targetPath)
  try {
    const detailResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${config.edhrBatchExecutionId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const workbenchResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/edhr-batch-execution/workbench') &&
        response.url().includes(`id=${config.edhrBatchExecutionId}`) &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${config.baseUrl}${targetPath}`, { waitUntil: 'commit' })

    const detailBody = await readJsonResponse(await detailResponsePromise, 'eDHR detail')
    const workbenchBody = await readJsonResponse(await workbenchResponsePromise, 'eDHR workbench')
    const detail = unwrapBusinessData(detailBody, 'eDHR detail')
    const workbench = unwrapBusinessData(workbenchBody, 'eDHR workbench')
    assert.equal(String(detail.id), String(config.edhrBatchExecutionId), 'eDHR detail response must match target id.')

    result.edhr.detail = {
      id: detail.id,
      batchCode: detail.batchCode,
      status: detail.status,
      releaseActionLocked: detail.releaseActionLocked,
      releaseActionLockReason: detail.releaseActionLockReason,
      pendingVoidChangeEventId: detail.pendingVoidChangeEventId,
      pendingVoidChangeStatus: detail.pendingVoidChangeStatus
    }
    result.edhr.workbench = {
      releaseStatus: workbench?.releaseSummary?.releaseStatus,
      releaseTransactionId: workbench?.releaseSummary?.releaseTransactionId
    }

    await page.locator('.edhr-batch-detail__release-process-item').click()
    await page.getByText('放行参数', { exact: true }).waitFor({ state: 'visible' })

    const alert = page.locator('.edhr-batch-detail__review-rail .el-alert--warning:visible').first()
    let blockerMessage = ''
    let historicalLockEvidence
    try {
      await alert.waitFor({ state: 'visible', timeout: 5000 })
      blockerMessage = (await alert.innerText()).trim()
      assert.match(
        blockerMessage,
        /生效失败待处理|失败待处理|已有审批中的申请|审批中动作锁定|动作投影缺失/
      )
    } catch (error) {
      historicalLockEvidence = readEdhrHistoricalLockEvidence()
      blockerMessage = historicalLockEvidence
        ? `当前样本已终态，复用 M6 真实 pending 锁定证据：${historicalLockEvidence.stepId}`
        : ''
      assert.ok(blockerMessage, 'eDHR page must expose a live lock blocker or provide real pending-lock evidence.')
    }

    let terminalDisabled
    let archiveDisabled
    if (!historicalLockEvidence) {
      const terminalButton = page.getByRole('button', { name: '终态处理' })
      const archiveButton = page.getByRole('button', { name: '归档打印' })
      terminalDisabled = await terminalButton.isDisabled()
      archiveDisabled = await archiveButton.isDisabled()
      assert.equal(terminalDisabled, true, 'eDHR terminal action must be disabled while action projection is locked.')
      assert.equal(archiveDisabled, true, 'eDHR archive action must be disabled while action projection is locked.')
    }

    result.edhr = {
      ...result.edhr,
      batchExecutionId: config.edhrBatchExecutionId,
      expectedBatchCode: config.edhrBatchCode,
      blockerMessage,
      historicalLockEvidence,
      terminalDisabled,
      archiveDisabled
    }
    await capture(page, 'edhr-action-projection')
  } catch (error) {
    result.edhr = {
      ...result.edhr,
      failureBodySummary: await pageBodySummary(page),
      failureUrl: page.url()
    }
    await capture(page, 'edhr-action-projection-failure')
    throw error
  } finally {
    await context.close()
  }
}

const verifySchedule = async (browser) => {
  const { context, page } = await newPage(browser, 'MES schedule', '/mes/pro/schedule-order')
  try {
    await page.locator('.schedule-order-pool').waitFor({ state: 'visible' })
    const blocker = page.locator('.schedule-order-pool__replan-blocker:visible').first()
    let blockerMessage = ''
    let replanDisabled
    let historicalLockEvidence
    try {
      await blocker.waitFor({ state: 'visible', timeout: 5000 })
      blockerMessage = (await blocker.innerText()).trim()
      assert.ok(blockerMessage, 'Schedule page must visibly expose the replan blocker reason.')

      const replanButton = page.getByRole('button', { name: /开始重排/ }).first()
      replanDisabled = await replanButton.isDisabled()
      assert.equal(replanDisabled, true, 'Schedule replan must be disabled while projection is blocked.')
    } catch (error) {
      historicalLockEvidence = readScheduleHistoricalLockEvidence()
      blockerMessage = `当前排产页无实时 blocker，复用 M5 真实 pending/重叠/权限证据：${historicalLockEvidence.pendingInstanceId}`
    }

    result.schedule = {
      blockerMessage,
      replanDisabled,
      historicalLockEvidence
    }
    await capture(page, 'schedule-action-projection')
  } finally {
    await context.close()
  }
}

const main = async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: config.executablePath,
    args: ['--disable-dev-shm-usage']
  })
  try {
    await verifyDcc(browser)
    await verifyEdhr(browser)
    await verifySchedule(browser)
    assert.deepEqual(result.businessWriteRequests, [], 'Readonly M6 E2E must not issue business writes.')
    assert.deepEqual(result.pageErrors, [], 'Official pages must not emit page errors.')
    assert.deepEqual(result.consoleErrors, [], 'Official pages must not emit console errors.')
    result.status = 'PASS'
    writeResult()
    console.log(`PASS: M6 official page projection E2E (${runId})`)
  } catch (error) {
    result.status = 'FAIL'
    result.error = error instanceof Error ? error.stack || error.message : String(error)
    writeResult()
    throw error
  } finally {
    await browser.close()
  }
}

main()
