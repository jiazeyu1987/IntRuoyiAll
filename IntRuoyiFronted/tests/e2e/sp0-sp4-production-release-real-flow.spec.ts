import { existsSync } from 'node:fs'
import { basename, resolve } from 'node:path'
import {
  expect,
  test,
  type Browser,
  type BrowserContext,
  type Locator,
  type Page,
  type TestInfo
} from 'playwright/test'

test.describe.configure({ mode: 'serial' })
test.setTimeout(15 * 60 * 1000)

const WORK_TASK_PATH = '/mes/pro/feedback/edhr-work-task'
const TEAM_LEADER_PATH = '/mes/pro/process-pool/production-leader'
const TRACE_PATH = '/mes/pro/feedback/edhr-form-trace?tab=release'
const REPORT_NODE_TYPES = [
  'INCOMING_INSPECTION_REPORT',
  'STERILIZATION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_REPORT',
  'FINISHED_PRODUCT_INSPECTION_RECORD'
] as const

type ReportNodeType = (typeof REPORT_NODE_TYPES)[number]

type Account = {
  label: string
  username: string
  password: string
}

type FullFlowConfig = {
  baseUrl: string
  tenant: string
  teamLeader: Account
  pqc: Account
  incomingOwner: Account
  sterilizationOwner: Account
  finishedOwner: Account
  manager: Account
  outsider: Account
  mainActiveOrderId: string
  mainWorkOrderCode: string
  rejectActiveOrderId: string
  rejectWorkOrderCode: string
  reportFiles: Record<ReportNodeType, string>
  sterilizationBatchNo: string
  managerSignoffEvidenceHash: string
}

type FlowEvidence = {
  targetWrites: Array<{ actor: string; method: string; path: string }>
  responses: Array<Record<string, unknown>>
  consoleErrors: Array<{ actor: string; text: string }>
  pageErrors: Array<{ actor: string; text: string }>
  requestFailures: Array<{ actor: string; method: string; path: string; failure: string }>
}

type RoleSession = {
  context: BrowserContext
  page: Page
  permissions: string[]
}

const env = (name: string) => String(process.env[name] || '').trim()

const requiredEnv = (name: string, missing: string[]) => {
  const value = env(name)
  if (!value) missing.push(name)
  return value
}

const accountFromEnv = (prefix: string, label: string, missing: string[]): Account => ({
  label,
  username: requiredEnv(`EDHR_FULL_E2E_${prefix}_USERNAME`, missing),
  password: requiredEnv(`EDHR_FULL_E2E_${prefix}_PASSWORD`, missing)
})

const loadConfig = (): FullFlowConfig => {
  const missing: string[] = []
  const confirmation = requiredEnv('EDHR_FULL_E2E_CONFIRM_WRITES', missing)
  const config: FullFlowConfig = {
    baseUrl: requiredEnv('EDHR_FULL_E2E_BASE_URL', missing).replace(/\/+$/, ''),
    tenant: requiredEnv('EDHR_FULL_E2E_TENANT', missing),
    teamLeader: accountFromEnv('TEAM_LEADER', '生产组长', missing),
    pqc: accountFromEnv('PQC', 'PQC负责人', missing),
    incomingOwner: accountFromEnv('INCOMING_OWNER', '来料检负责人', missing),
    sterilizationOwner: accountFromEnv('STERILIZATION_OWNER', '灭菌负责人', missing),
    finishedOwner: accountFromEnv('FINISHED_OWNER', '成品检负责人', missing),
    manager: accountFromEnv('MANAGER', '管理者代表', missing),
    outsider: accountFromEnv('OUTSIDER', '非候选账号', missing),
    mainActiveOrderId: requiredEnv('EDHR_FULL_E2E_MAIN_ACTIVE_ORDER_ID', missing),
    mainWorkOrderCode: requiredEnv('EDHR_FULL_E2E_MAIN_WORK_ORDER_CODE', missing),
    rejectActiveOrderId: requiredEnv('EDHR_FULL_E2E_REJECT_ACTIVE_ORDER_ID', missing),
    rejectWorkOrderCode: requiredEnv('EDHR_FULL_E2E_REJECT_WORK_ORDER_CODE', missing),
    reportFiles: {
      INCOMING_INSPECTION_REPORT: resolve(
        requiredEnv('EDHR_FULL_E2E_INCOMING_REPORT_PATH', missing)
      ),
      STERILIZATION_REPORT: resolve(
        requiredEnv('EDHR_FULL_E2E_STERILIZATION_REPORT_PATH', missing)
      ),
      FINISHED_PRODUCT_INSPECTION_REPORT: resolve(
        requiredEnv('EDHR_FULL_E2E_FINISHED_REPORT_PATH', missing)
      ),
      FINISHED_PRODUCT_INSPECTION_RECORD: resolve(
        requiredEnv('EDHR_FULL_E2E_FINISHED_RECORD_PATH', missing)
      )
    },
    sterilizationBatchNo: requiredEnv('EDHR_FULL_E2E_STERILIZATION_BATCH_NO', missing),
    managerSignoffEvidenceHash: requiredEnv('EDHR_FULL_E2E_MANAGER_SIGNOFF_EVIDENCE_HASH', missing)
  }

  if (missing.length) {
    throw new Error(`T11_BLOCKED_MISSING_FORMAL_PREREQUISITES:${[...new Set(missing)].join(',')}`)
  }
  if (confirmation !== 'PRODUCTION_RELEASE_T11_WRITE') {
    throw new Error('T11_BLOCKED_WRITE_CONFIRMATION_MISMATCH')
  }
  if (!/^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(config.baseUrl)) {
    throw new Error(`T11_BLOCKED_NON_LOCAL_RUNTIME:${config.baseUrl}`)
  }
  if (config.pqc.username !== 'zhulijiang') {
    throw new Error(`T11_BLOCKED_PQC_TARGET_ACCOUNT:${config.pqc.username}`)
  }
  if (config.manager.username !== 'xujianhai') {
    throw new Error(`T11_BLOCKED_MANAGER_TARGET_ACCOUNT:${config.manager.username}`)
  }
  const usernames = [
    config.teamLeader,
    config.pqc,
    config.incomingOwner,
    config.sterilizationOwner,
    config.finishedOwner,
    config.manager,
    config.outsider
  ].map((item) => item.username)
  if (new Set(usernames).size !== usernames.length) {
    throw new Error('T11_BLOCKED_ACCOUNTS_MUST_BE_DISTINCT')
  }
  if (
    config.mainActiveOrderId === config.rejectActiveOrderId ||
    config.mainWorkOrderCode === config.rejectWorkOrderCode
  ) {
    throw new Error('T11_BLOCKED_MAIN_AND_REJECT_FIXTURES_MUST_BE_DISTINCT')
  }
  if (!/^[a-fA-F0-9]{64}$/.test(config.managerSignoffEvidenceHash)) {
    throw new Error('T11_BLOCKED_MANAGER_SIGNOFF_MUST_BE_SHA256')
  }
  for (const [nodeType, filePath] of Object.entries(config.reportFiles)) {
    if (!existsSync(filePath))
      throw new Error(`T11_BLOCKED_REPORT_FILE_MISSING:${nodeType}:${filePath}`)
  }
  return config
}

const isSuccessPayload = (payload: any) => payload && (payload.code === 0 || payload.code === 200)

const responseData = async (response: any, action: string) => {
  expect(response.status(), `${action} HTTP status`).toBe(200)
  const payload = await response.json().catch(() => null)
  expect(isSuccessPayload(payload), `${action} business response: ${JSON.stringify(payload)}`).toBe(
    true
  )
  return payload.data
}

const isTargetWritePath = (path: string) =>
  [
    '/mes/pro/process-pool/team-leader/active-order/release/apply',
    '/mes/pro/production-release/pqc/approve',
    '/mes/pro/production-release/pqc/reject',
    '/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload',
    '/mes/pro/edhr-batch-execution/task/special-node/complete',
    '/mes/pro/edhr-release/approve'
  ].some((fragment) => path.includes(fragment))

const observePage = (page: Page, actor: string, evidence: FlowEvidence) => {
  page.on('request', (request) => {
    const url = new URL(request.url())
    if (isTargetWritePath(url.pathname)) {
      evidence.targetWrites.push({ actor, method: request.method(), path: url.pathname })
    }
  })
  page.on('requestfailed', (request) => {
    const url = new URL(request.url())
    if (isTargetWritePath(url.pathname)) {
      evidence.requestFailures.push({
        actor,
        method: request.method(),
        path: url.pathname,
        failure: request.failure()?.errorText || 'unknown'
      })
    }
  })
  page.on('console', (message) => {
    if (message.type() === 'error') evidence.consoleErrors.push({ actor, text: message.text() })
  })
  page.on('pageerror', (error) => evidence.pageErrors.push({ actor, text: error.message }))
}

const selectTenant = async (page: Page, form: Locator, tenant: string) => {
  const input = form
    .locator(
      '.el-select input[role="combobox"], input.el-select__input, input[placeholder="请输入租户名称"]'
    )
    .first()
  await input.waitFor({ state: 'visible', timeout: 30_000 })
  await input.fill('')
  await input.fill(tenant)
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: tenant })
    .first()
  if (await option.isVisible({ timeout: 10_000 }).catch(() => false)) await option.click()
  else await input.press('Enter')
}

const login = async (
  browser: Browser,
  config: FullFlowConfig,
  account: Account,
  targetPath: string,
  evidence: FlowEvidence,
  requiredPermissions: string[] = []
): Promise<RoleSession> => {
  const context = await browser.newContext()
  const page = await context.newPage()
  observePage(page, account.label, evidence)
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent(targetPath)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60_000
  })
  const form = page.locator('form.login-form:visible, .login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 30_000 })
  await selectTenant(page, form, config.tenant)
  const usernameInput = form
    .locator(
      'input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'
    )
    .first()
  await usernameInput.fill(account.username)
  await form
    .locator('input[type="password"], input[placeholder="请输入密码"]')
    .first()
    .fill(account.password)

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60_000 }
  )
  const permissionResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/get-permission-info') &&
      response.request().method() === 'GET',
    { timeout: 60_000 }
  )
  await form.getByRole('button', { name: '登录' }).click()
  await responseData(await loginResponsePromise, `${account.label}登录`)
  const permissionPayload = await (await permissionResponsePromise).json()
  expect(isSuccessPayload(permissionPayload), `${account.label}权限响应`).toBe(true)
  const permissions = Array.isArray(permissionPayload.data?.permissions)
    ? permissionPayload.data.permissions
    : []
  for (const permission of requiredPermissions) {
    expect(permissions, `${account.label}缺少权限 ${permission}`).toContain(permission)
  }
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60_000 })
  await page.goto(`${config.baseUrl}${targetPath}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60_000
  })
  return { context, page, permissions }
}

const apiResponse = (
  page: Page,
  path: string,
  method: 'GET' | 'POST',
  action: string,
  trigger: () => Promise<void>
) => {
  const pending = page.waitForResponse(
    (response) => response.url().includes(path) && response.request().method() === method,
    { timeout: 90_000 }
  )
  return trigger().then(async () => responseData(await pending, action))
}

const formInput = (form: Locator, label: string) =>
  form.locator('.el-form-item').filter({ hasText: label }).first().locator('input').last()

const tableRowWithText = async (page: Page, text: string) => {
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: text }).first()
  await row.waitFor({ state: 'visible', timeout: 60_000 })
  return row
}

const openActiveOrderPool = async (page: Page) => {
  const tab = page.getByRole('tab', { name: '活跃订单池' })
  if (await tab.isVisible({ timeout: 10_000 }).catch(() => false)) await tab.click()
  await page
    .locator('[data-team-leader-active-order-list]')
    .waitFor({ state: 'visible', timeout: 60_000 })
}

const applyReleaseFromTeamLeaderPage = async (
  page: Page,
  activeOrderId: string,
  workOrderCode: string,
  evidence: FlowEvidence
) => {
  await openActiveOrderPool(page)
  const idCell = page.locator(`[data-team-leader-active-order-id="${activeOrderId}"]`)
  await idCell.waitFor({ state: 'visible', timeout: 60_000 })
  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ has: idCell }).first()
  await expect(row.locator('[data-team-leader-active-order-work-order-code]')).toHaveText(
    workOrderCode
  )
  await expect(row.locator('[data-team-leader-active-order-production-progress]')).toContainText(
    '100%'
  )
  await expect(row.locator('[data-team-leader-active-order-inspection-progress]')).toContainText(
    '100%'
  )
  const applyButton = row.locator('[data-team-leader-active-order-release-apply]')
  await expect(applyButton).toBeEnabled()
  const result = await apiResponse(
    page,
    '/mes/pro/process-pool/team-leader/active-order/release/apply',
    'POST',
    `${workOrderCode}生产组长提交放行申请`,
    async () => {
      await applyButton.click()
      const confirm = page.locator('.el-message-box:visible').last()
      await confirm.getByRole('button', { name: '申请放行' }).click()
    }
  )
  expect(result.status).toBe('PQC_RELEASE_PENDING')
  expect(typeof result.applicationId).toBe('string')
  expect(typeof result.pqcReleaseWorkTaskId).toBe('string')
  expect(result.batchExecutionId).toBeUndefined()
  expect(result.releaseTransactionId).toBeUndefined()
  expect(result.sourceSnapshotHash).toMatch(/^[a-fA-F0-9]{64}$/)
  evidence.responses.push({
    stage: 'SP-1',
    workOrderCode,
    applicationId: result.applicationId,
    pqcReleaseWorkTaskId: result.pqcReleaseWorkTaskId,
    status: result.status
  })
  return result
}

const filterCandidateWorkTasks = async (page: Page, workOrderCode: string) => {
  await page.getByRole('tab', { name: '候选审核' }).click()
  const form = page.locator('.edhr-work-task-page__toolbar')
  await formInput(form, '工单').fill(workOrderCode)
  await apiResponse(
    page,
    '/mes/pro/edhr-work-task/candidate-todo-page',
    'GET',
    `查询${workOrderCode}候选待办`,
    async () => {
      await form.getByRole('button', { name: '查询' }).click()
    }
  )
}

const submitPqcDecision = async (
  page: Page,
  workOrderCode: string,
  action: 'APPROVE' | 'REJECT',
  evidence: FlowEvidence
) => {
  await filterCandidateWorkTasks(page, workOrderCode)
  const row = await tableRowWithText(page, workOrderCode)
  const decisionButton = row.locator(
    action === 'APPROVE' ? '[data-pqc-release-approve]' : '[data-pqc-release-reject]'
  )
  await expect(decisionButton).toBeVisible()
  await decisionButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: 'PQC生产放行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60_000 })
  if (action === 'REJECT') await dialog.locator('textarea').fill(`T11终态拒绝-${workOrderCode}`)
  else await dialog.locator('textarea').fill(`T11正式PQC通过-${workOrderCode}`)
  const endpoint = `/mes/pro/production-release/pqc/${action === 'APPROVE' ? 'approve' : 'reject'}`
  const result = await apiResponse(
    page,
    endpoint,
    'POST',
    `${workOrderCode} PQC ${action}`,
    async () => {
      await dialog
        .getByRole('button', { name: action === 'APPROVE' ? '确认通过' : '确认拒绝' })
        .click()
    }
  )
  expect(typeof result.applicationId).toBe('string')
  expect(typeof result.pqcReleaseWorkTaskId).toBe('string')
  if (action === 'REJECT') {
    expect(result.decision).toBe('REJECT')
    expect(result.status).toBe('PQC_RELEASE_REJECTED')
    expect(result.rejectReason).toContain(workOrderCode)
    expect(result.batchExecutionId).toBeFalsy()
    expect(result.reportUploadTasks).toEqual([])
  } else {
    expect(result.decision).toBe('APPROVE')
    expect(result.status).toBe('REPORT_UPLOAD_PENDING')
    expect(typeof result.batchExecutionId).toBe('string')
    expect(result.batchRecordEvidenceIds.length).toBeGreaterThan(0)
    expect(result.processInspectionEvidenceIds.length).toBeGreaterThan(0)
    expect(result.lossReportEvidenceIds.length).toBeGreaterThan(0)
    expect(result.reportUploadTasks).toHaveLength(4)
    expect(result.reportUploadTasks.map((item: any) => item.nodeType).sort()).toEqual(
      [...REPORT_NODE_TYPES].sort()
    )
    for (const task of result.reportUploadTasks) {
      expect(typeof task.batchTaskId).toBe('string')
      expect(typeof task.workTaskId).toBe('string')
      expect(task.status).toBe('TODO')
    }
  }
  evidence.responses.push({
    stage: 'SP-2',
    workOrderCode,
    decision: result.decision,
    status: result.status,
    applicationId: result.applicationId,
    batchExecutionId: result.batchExecutionId,
    reportTaskCount: result.reportUploadTasks?.length || 0
  })
  return result
}

const assertNoCandidateAction = async (
  page: Page,
  workOrderCode: string,
  selector: string,
  label: string
) => {
  await filterCandidateWorkTasks(page, workOrderCode)
  const matchingRows = page
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: workOrderCode })
  if (await matchingRows.count()) await expect(matchingRows.locator(selector), label).toHaveCount(0)
}

const completeReportNode = async (
  page: Page,
  workOrderCode: string,
  nodeType: ReportNodeType,
  batchTaskId: string,
  reportPath: string,
  sterilizationBatchNo: string,
  evidence: FlowEvidence
) => {
  await page.goto(`${new URL(page.url()).origin}${WORK_TASK_PATH}`, {
    waitUntil: 'domcontentloaded'
  })
  await filterCandidateWorkTasks(page, workOrderCode)
  const row = page
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: workOrderCode })
    .filter({ hasText: nodeType })
    .first()
  if (!(await row.isVisible({ timeout: 5_000 }).catch(() => false))) {
    const expectedLabel: Record<ReportNodeType, string> = {
      INCOMING_INSPECTION_REPORT: '来料检验报告',
      STERILIZATION_REPORT: '灭菌报告',
      FINISHED_PRODUCT_INSPECTION_REPORT: '成品检验报告',
      FINISHED_PRODUCT_INSPECTION_RECORD: '成品检验记录'
    }
    const localizedRow = page
      .locator('.el-table__body-wrapper tbody tr')
      .filter({ hasText: workOrderCode })
      .filter({ hasText: expectedLabel[nodeType] })
      .first()
    await localizedRow.waitFor({ state: 'visible', timeout: 60_000 })
    await localizedRow.locator('[data-production-release-report-open]').click()
  } else {
    await row.locator('[data-production-release-report-open]').click()
  }
  await page.waitForURL((url) => url.pathname.includes('/edhr-batch-execution/detail'), {
    timeout: 60_000
  })
  const uploadButton = page.locator(`[data-production-release-report-upload="${batchTaskId}"]`)
  await uploadButton.waitFor({ state: 'visible', timeout: 60_000 })
  await expect(uploadButton).toBeEnabled()

  const prepareResponse = page.waitForResponse(
    (response) =>
      response
        .url()
        .includes('/mes/pro/edhr-batch-execution/task/special-node/attachment/prepare-upload') &&
      response.request().method() === 'POST',
    { timeout: 120_000 }
  )
  const input = page
    .locator('.edhr-batch-detail__special-node-hidden-upload input[type="file"]')
    .first()
  await input.setInputFiles(reportPath)
  const prepared = await responseData(await prepareResponse, `${nodeType}上传报告`)
  expect(prepared.fileName).toBe(basename(reportPath))
  expect(prepared.sha256).toMatch(/^[a-fA-F0-9]{64}$/)
  expect(typeof prepared.version).toBe('number')

  const completeButton = page.locator(`[data-production-release-report-complete="${batchTaskId}"]`)
  await expect(completeButton).toBeEnabled()
  await completeButton.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '完成特殊节点' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 30_000 })
  if (nodeType === 'STERILIZATION_REPORT') {
    await dialog.locator('input[placeholder="请输入灭菌批次"]').fill(sterilizationBatchNo)
  }
  const completed = await apiResponse(
    page,
    '/mes/pro/edhr-batch-execution/task/special-node/complete',
    'POST',
    `${nodeType}完成报告`,
    async () => {
      await dialog.getByRole('button', { name: '完成报告' }).click()
    }
  )
  expect(completed.nodeType).toBe(nodeType)
  expect(completed.batchTaskId).toBe(batchTaskId)
  expect(completed.nodeStatus).toBe('COMPLETED')
  expect(completed.attachmentIds.length).toBeGreaterThan(0)
  expect(completed.attachmentHashes).toContain(prepared.sha256)
  expect(typeof completed.workTaskId).toBe('string')
  evidence.responses.push({
    stage: 'SP-3',
    nodeType,
    batchTaskId,
    workTaskId: completed.workTaskId,
    reportUploadStatus: completed.reportUploadStatus,
    releaseTransactionId: completed.releaseTransactionId,
    managerReleaseWorkTaskId: completed.managerReleaseWorkTaskId
  })
  return completed
}

const submitManagerRelease = async (
  page: Page,
  workOrderCode: string,
  signoffEvidenceHash: string,
  evidence: FlowEvidence
) => {
  await filterCandidateWorkTasks(page, workOrderCode)
  const row = await tableRowWithText(page, workOrderCode)
  const button = row.locator('[data-manager-release-approve]')
  await expect(button).toBeVisible()
  await button.click()
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '管理者代表最终放行' }).last()
  await dialog.waitFor({ state: 'visible', timeout: 60_000 })
  await dialog.locator('input[placeholder="请输入正式电子签名证据哈希"]').fill(signoffEvidenceHash)
  await dialog.locator('textarea').fill(`T11管理者代表最终放行-${workOrderCode}`)
  const released = await apiResponse(
    page,
    '/mes/pro/edhr-release/approve',
    'POST',
    `${workOrderCode}最终放行`,
    async () => {
      await dialog.getByRole('button', { name: '确认最终放行' }).click()
    }
  )
  expect(released.releaseStatus).toBe('RELEASED')
  expect(released.approvalSignoffEvidenceHash).toBe(signoffEvidenceHash)
  expect(typeof released.releaseTransactionId).toBe('string')
  expect(typeof released.batchExecutionId).toBe('string')
  evidence.responses.push({
    stage: 'SP-4',
    workOrderCode,
    releaseTransactionId: released.releaseTransactionId,
    batchExecutionId: released.batchExecutionId,
    releaseStatus: released.releaseStatus
  })
  return released
}

const verifyReleasedTrace = async (
  page: Page,
  config: FullFlowConfig,
  released: any,
  evidence: FlowEvidence,
  testInfo: TestInfo
) => {
  const traceResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/mes/pro/edhr-release/page') &&
      response.request().method() === 'GET',
    { timeout: 90_000 }
  )
  await page.goto(`${config.baseUrl}${TRACE_PATH}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60_000
  })
  const traceResponse = await traceResponsePromise
  const traceUrl = new URL(traceResponse.url())
  expect(traceUrl.searchParams.get('completedTraceOnly')).toBe('true')
  expect(traceUrl.searchParams.get('releaseStatus')).toBe('RELEASED')
  const traceData = await responseData(traceResponse, '放行追溯分页')
  const list = Array.isArray(traceData?.list) ? traceData.list : []
  const target = list.find((item: any) => item.workOrderCode === config.mainWorkOrderCode)
  expect(target).toBeTruthy()
  expect(target.releaseStatus).toBe('RELEASED')
  expect(target.releaseTransactionId).toBe(released.releaseTransactionId)
  expect(target.batchExecutionId).toBe(released.batchExecutionId)
  expect(list.some((item: any) => item.workOrderCode === config.rejectWorkOrderCode)).toBe(false)

  const row = await tableRowWithText(page, config.mainWorkOrderCode)
  await expect(row.getByText('已放行')).toBeVisible()
  await row.getByRole('button', { name: '追溯', exact: true }).click()
  const drawer = page.locator('.el-drawer:visible').filter({ hasText: '批记录表单详情' }).last()
  await drawer.waitFor({ state: 'visible', timeout: 90_000 })
  await expect(drawer.getByText('电子签名记录')).toBeVisible()
  for (const filePath of Object.values(config.reportFiles)) {
    await expect(drawer.getByText(basename(filePath), { exact: false })).toBeVisible()
  }
  await testInfo.attach('t11-release-trace.png', {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png'
  })
  evidence.responses.push({
    stage: 'TRACE',
    completedTraceOnly: traceUrl.searchParams.get('completedTraceOnly'),
    releaseStatus: traceUrl.searchParams.get('releaseStatus'),
    workOrderCode: target.workOrderCode,
    releaseTransactionId: target.releaseTransactionId,
    batchExecutionId: target.batchExecutionId
  })
}

test('SP-0 through SP-4 real multi-account production release flow', async ({
  browser
}, testInfo) => {
  const config = loadConfig()
  const evidence: FlowEvidence = {
    targetWrites: [],
    responses: [],
    consoleErrors: [],
    pageErrors: [],
    requestFailures: []
  }

  const leader = await login(browser, config, config.teamLeader, TEAM_LEADER_PATH, evidence, [
    'mes:pro-process-pool-team-leader:query',
    'mes:pro-process-pool-team-leader:release-apply'
  ])
  const mainApplication = await applyReleaseFromTeamLeaderPage(
    leader.page,
    config.mainActiveOrderId,
    config.mainWorkOrderCode,
    evidence
  )
  await applyReleaseFromTeamLeaderPage(
    leader.page,
    config.rejectActiveOrderId,
    config.rejectWorkOrderCode,
    evidence
  )
  await leader.context.close()

  const outsiderBeforePqc = await login(
    browser,
    config,
    config.outsider,
    WORK_TASK_PATH,
    evidence,
    ['mes:pro-edhr-work-task:query']
  )
  await assertNoCandidateAction(
    outsiderBeforePqc.page,
    config.mainWorkOrderCode,
    '[data-pqc-release-approve], [data-pqc-release-reject]',
    '非PQC角色候选不能处理PQC决定'
  )
  await outsiderBeforePqc.context.close()

  const pqc = await login(browser, config, config.pqc, WORK_TASK_PATH, evidence, [
    'mes:pro-edhr-work-task:query',
    'mes:pro-production-release:query',
    'mes:pro-production-release:pqc-approve',
    'mes:pro-production-release:pqc-reject'
  ])
  await submitPqcDecision(pqc.page, config.rejectWorkOrderCode, 'REJECT', evidence)
  const pqcApproved = await submitPqcDecision(
    pqc.page,
    config.mainWorkOrderCode,
    'APPROVE',
    evidence
  )
  expect(pqcApproved.applicationId).toBe(mainApplication.applicationId)
  await assertNoCandidateAction(
    pqc.page,
    config.rejectWorkOrderCode,
    '[data-pqc-release-approve], [data-pqc-release-reject]',
    'PQC拒绝终态不能重开'
  )
  await pqc.context.close()

  const tasksByNode = new Map<string, any>(
    pqcApproved.reportUploadTasks.map((item: any) => [item.nodeType, item])
  )
  const incoming = await login(browser, config, config.incomingOwner, WORK_TASK_PATH, evidence, [
    'mes:pro-edhr-work-task:query'
  ])
  const incomingCompleted = await completeReportNode(
    incoming.page,
    config.mainWorkOrderCode,
    'INCOMING_INSPECTION_REPORT',
    tasksByNode.get('INCOMING_INSPECTION_REPORT').batchTaskId,
    config.reportFiles.INCOMING_INSPECTION_REPORT,
    config.sterilizationBatchNo,
    evidence
  )
  expect(incomingCompleted.reportUploadStatus).toBe('REPORT_UPLOAD_PENDING')
  await assertNoCandidateAction(
    incoming.page,
    config.mainWorkOrderCode,
    '[data-production-release-report-open]',
    '来料负责人完成后无残留报告待办'
  )
  await incoming.context.close()

  const sterilization = await login(
    browser,
    config,
    config.sterilizationOwner,
    WORK_TASK_PATH,
    evidence,
    ['mes:pro-edhr-work-task:query']
  )
  const sterilizationCompleted = await completeReportNode(
    sterilization.page,
    config.mainWorkOrderCode,
    'STERILIZATION_REPORT',
    tasksByNode.get('STERILIZATION_REPORT').batchTaskId,
    config.reportFiles.STERILIZATION_REPORT,
    config.sterilizationBatchNo,
    evidence
  )
  expect(sterilizationCompleted.reportUploadStatus).toBe('REPORT_UPLOAD_PENDING')
  await assertNoCandidateAction(
    sterilization.page,
    config.mainWorkOrderCode,
    '[data-production-release-report-open]',
    '灭菌负责人完成后无残留报告待办'
  )
  await sterilization.context.close()

  const finished = await login(browser, config, config.finishedOwner, WORK_TASK_PATH, evidence, [
    'mes:pro-edhr-work-task:query'
  ])
  const finishedReportCompleted = await completeReportNode(
    finished.page,
    config.mainWorkOrderCode,
    'FINISHED_PRODUCT_INSPECTION_REPORT',
    tasksByNode.get('FINISHED_PRODUCT_INSPECTION_REPORT').batchTaskId,
    config.reportFiles.FINISHED_PRODUCT_INSPECTION_REPORT,
    config.sterilizationBatchNo,
    evidence
  )
  expect(finishedReportCompleted.reportUploadStatus).toBe('REPORT_UPLOAD_PENDING')
  const finalReportCompleted = await completeReportNode(
    finished.page,
    config.mainWorkOrderCode,
    'FINISHED_PRODUCT_INSPECTION_RECORD',
    tasksByNode.get('FINISHED_PRODUCT_INSPECTION_RECORD').batchTaskId,
    config.reportFiles.FINISHED_PRODUCT_INSPECTION_RECORD,
    config.sterilizationBatchNo,
    evidence
  )
  expect(finalReportCompleted.reportUploadStatus).toBe('MANAGER_RELEASE_PENDING')
  expect(typeof finalReportCompleted.releaseTransactionId).toBe('string')
  expect(typeof finalReportCompleted.managerReleaseWorkTaskId).toBe('string')
  await assertNoCandidateAction(
    finished.page,
    config.mainWorkOrderCode,
    '[data-production-release-report-open]',
    '成品负责人完成两份报告后无残留待办'
  )
  await finished.context.close()

  const outsiderBeforeManager = await login(
    browser,
    config,
    config.outsider,
    WORK_TASK_PATH,
    evidence,
    ['mes:pro-edhr-work-task:query']
  )
  await assertNoCandidateAction(
    outsiderBeforeManager.page,
    config.mainWorkOrderCode,
    '[data-manager-release-approve]',
    '非管理者代表候选不能最终放行'
  )
  await outsiderBeforeManager.context.close()

  const manager = await login(browser, config, config.manager, WORK_TASK_PATH, evidence, [
    'mes:pro-edhr-work-task:query',
    'mes:pro-edhr-release:query',
    'mes:pro-edhr-release:approve'
  ])
  const released = await submitManagerRelease(
    manager.page,
    config.mainWorkOrderCode,
    config.managerSignoffEvidenceHash,
    evidence
  )
  expect(released.releaseTransactionId).toBe(finalReportCompleted.releaseTransactionId)
  await assertNoCandidateAction(
    manager.page,
    config.mainWorkOrderCode,
    '[data-manager-release-approve]',
    '最终放行后无残留管理者待办'
  )
  await verifyReleasedTrace(manager.page, config, released, evidence, testInfo)

  expect(evidence.requestFailures).toEqual([])
  expect(evidence.pageErrors).toEqual([])
  expect(evidence.consoleErrors).toEqual([])
  expect(
    evidence.targetWrites.filter((item) => item.path.endsWith('/active-order/release/apply'))
  ).toHaveLength(2)
  expect(evidence.targetWrites.filter((item) => item.path.endsWith('/pqc/reject'))).toHaveLength(1)
  expect(evidence.targetWrites.filter((item) => item.path.endsWith('/pqc/approve'))).toHaveLength(1)
  expect(
    evidence.targetWrites.filter((item) => item.path.endsWith('/attachment/prepare-upload'))
  ).toHaveLength(4)
  expect(
    evidence.targetWrites.filter((item) => item.path.endsWith('/special-node/complete'))
  ).toHaveLength(4)
  expect(
    evidence.targetWrites.filter((item) => item.path.endsWith('/edhr-release/approve'))
  ).toHaveLength(1)
  await testInfo.attach('t11-production-release-evidence.json', {
    body: Buffer.from(JSON.stringify(evidence, null, 2), 'utf8'),
    contentType: 'application/json'
  })
  await manager.context.close()
})
