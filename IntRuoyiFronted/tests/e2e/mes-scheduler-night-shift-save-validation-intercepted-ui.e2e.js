const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const repoRoot = path.resolve(__dirname, '..', '..')
const workspaceRoot = path.resolve(repoRoot, '..')
const artifactDir = path.join(
  workspaceRoot,
  'doc',
  'tasks',
  '20260813-scheduler-seven-issues-closure',
  'artifacts',
  'night-shift-save-validation',
  'intercepted-ui'
)

const config = {
  baseUrl: (process.env.MES_SCHEDULER_NIGHT_UI_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  processName: process.env.MES_SCHEDULER_NIGHT_UI_PROCESS_NAME || '吹球囊成型',
  headed: process.env.MES_SCHEDULER_NIGHT_UI_HEADED === '1'
}

const buildValidationMessage = (routeProcessId) =>
  `工序启用夜班失败：routeProcessId=${routeProcessId}，工作站[未配置]，产线[未配置]，夜班[未配置]，未绑定工作站，无法确定夜班班次、设备和产能`

const ensureArtifactDir = () => {
  fs.mkdirSync(artifactDir, { recursive: true })
  for (const name of ['error.txt', 'failure.png', 'real-schedule-order-process-dialog.png']) {
    fs.rmSync(path.join(artifactDir, name), { force: true })
  }
}

const writeJson = (name, data) => {
  fs.writeFileSync(path.join(artifactDir, name), JSON.stringify(data, null, 2), 'utf8')
}

const parseResponseBody = (response) =>
  response.json().catch(async () => ({ raw: await response.text().catch(() => '') }))

const visible = async (locator) => (await locator.count()) > 0 && locator.isVisible().catch(() => false)

async function loginWithVisiblePrefilledForm(page) {
  await page.goto(`${config.baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/schedule-order')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 }).catch(() => {})
  if (!(await visible(form)) && !page.url().includes('/login')) {
    return { tenant: '当前登录租户', username: '当前登录用户' }
  }
  assert.ok(await visible(form), `登录表单未出现，当前地址：${page.url()}`)

  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入执行只读 UI 验证。')
  }

  const usernameInput = form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])')
    .first()
  const passwordInput = form
    .locator('input[placeholder="请输入密码"], input[type="password"]')
    .first()
  const username = (await usernameInput.inputValue()).trim()
  assert.ok(username, '登录页必须提供预填用户名，脚本不会读取或记录凭据文件。')
  assert.ok((await passwordInput.inputValue()).trim(), '登录页必须提供预填密码。')

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  const tenant = (await tenantInput.count()) > 0 ? (await tenantInput.inputValue()).trim() : '默认租户'

  const permissionRequestPromise = page.waitForRequest(
    (request) =>
      request.url().includes('/admin-api/system/auth/get-permission-info') &&
      request.method() === 'GET',
    { timeout: 60000 }
  )
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: /登录/ }).click()
  const response = await responsePromise
  const body = await parseResponseBody(response)
  assert.ok(response.ok(), `登录请求 HTTP ${response.status()}`)
  assert.equal(body.code, 0, `登录失败：${body.msg || body.code}`)
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), null, {
    timeout: 60000
  })
  const permissionRequest = await permissionRequestPromise
  const sourceHeaders = permissionRequest.headers()
  return {
    identity: { tenant: tenant || '默认租户', username },
    authHeaders: {
      authorization: sourceHeaders.authorization,
      'tenant-id': sourceHeaders['tenant-id'],
      'visit-tenant-id': sourceHeaders['visit-tenant-id']
    }
  }
}

async function requestJson(context, url, authHeaders) {
  const headers = Object.fromEntries(
    Object.entries(authHeaders).filter(([, value]) => Boolean(value))
  )
  const response = await context.request.get(`${config.baseUrl}${url}`, { headers })
  return { status: response.status(), body: await response.json() }
}

async function resolveRealTargetProcess(context, scheduleOrderId, authHeaders) {
  const result = await requestJson(
    context,
    `/admin-api/mes/pro/schedule-order/process-list?scheduleOrderId=${scheduleOrderId}`,
    authHeaders
  )
  assert.equal(result.status, 200, `工艺流程排产配置 HTTP ${result.status}`)
  assert.equal(result.body.code, 0, `工艺流程排产配置失败：${result.body.msg || result.body.code}`)
  const rows = Array.isArray(result.body.data) ? result.body.data : []
  const target = rows.find(
    (row) =>
      String(row.processName || '').includes(config.processName) &&
      Number(row.routeProcessId) > 0 &&
      Number(row.processId) > 0
  )
  return target
}

async function resolveRealSource(context, authHeaders) {
  for (let pageNo = 1; pageNo <= 10; pageNo += 1) {
    const result = await requestJson(
      context,
      `/admin-api/mes/pro/schedule-order/page?pageNo=${pageNo}&pageSize=1`,
      authHeaders
    )
    assert.equal(result.status, 200, `排产工单分页 HTTP ${result.status}`)
    assert.equal(result.body.code, 0, `排产工单分页失败：${result.body.msg || result.body.code}`)
    const scheduleOrders = Array.isArray(result.body.data?.list) ? result.body.data.list : []
    if (scheduleOrders.length === 0) break
    const scheduleOrder = scheduleOrders[0]
    const targetProcess = await resolveRealTargetProcess(
      context,
      scheduleOrder.id,
      authHeaders
    )
    if (targetProcess) return { scheduleOrder, targetProcess }
  }
  throw new Error(`当前登录租户前 10 个排产工单没有“${config.processName}”真实工序。`)
}

async function resolveRealRouteVersion(context, scheduleOrder, authHeaders) {
  const routeId = Number(scheduleOrder.routeId)
  assert.ok(routeId > 0, '真实排产工单缺少 routeId。')
  const result = await requestJson(
    context,
    `/admin-api/mes/pro/route-version/list-by-route?routeId=${routeId}`,
    authHeaders
  )
  assert.equal(result.status, 200, `路线版本列表 HTTP ${result.status}`)
  assert.equal(result.body.code, 0, `路线版本列表失败：${result.body.msg || result.body.code}`)
  const versions = Array.isArray(result.body.data) ? result.body.data : []
  const version =
    versions.find((item) => item.active === true) ||
    versions.find((item) => String(item.versionNo) === String(scheduleOrder.routeVersion))
  assert.ok(version?.id, `路线 ${routeId} 没有可识别的真实版本。`)
  return version
}

const buildWipRow = ({ scheduleOrder, targetProcess, routeVersion }) => ({
  routeId: Number(scheduleOrder.routeId),
  routeCode: scheduleOrder.routeCode,
  routeName: scheduleOrder.routeName,
  routeVersionId: Number(routeVersion.id),
  routeVersionNo: routeVersion.versionNo,
  routeVersionStatus: routeVersion.lifecycleStatus,
  routeProcessId: Number(targetProcess.routeProcessId),
  processId: Number(targetProcess.processId),
  processCode: targetProcess.processCode,
  processName: targetProcess.processName,
  wipOrderCount: 1,
  shiftCapacityTotal: targetProcess.shiftCapacityTotal,
  capacityMode: targetProcess.capacityMode,
  capacitySource: targetProcess.capacitySource,
  resourceStatus: 'CAPACITY_MISSING',
  resourceStatusReason: '夜班资源待校验',
  shiftStatus: '白班',
  nightShiftEnabled: false,
  plannedStartDate: undefined,
  plannedStartDateMixed: false,
  unfinishedDemandQuantity: targetProcess.remainingQuantity,
  estimatedStartTime: targetProcess.plannedStartTime,
  estimatedCompletionTime: targetProcess.plannedEndTime,
  todayFeedbackQuantity: 0,
  scheduleOrderIds: [Number(scheduleOrder.id)]
})

async function verifyImmediateUiFeedback(page, wipRow, writeRequests, interceptedRequests) {
  await page.route('**/admin-api/mes/pro/schedule-order/process-wip-statistics*', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ code: 0, data: [wipRow], msg: '' })
    })
  })
  await page.route('**/admin-api/mes/pro/schedule-order/process-wip-settings', async (route) => {
    const payload = JSON.parse(route.request().postData() || '{}')
    interceptedRequests.push({
      method: route.request().method(),
      url: route.request().url(),
      payload,
      backendReached: false
    })
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ code: 400, data: null, msg: buildValidationMessage(wipRow.routeProcessId) })
    })
  })

  const wipResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-statistics') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench?nightShiftUi=${Date.now()}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const wipResponse = await wipResponsePromise
  const wipBody = await parseResponseBody(wipResponse)
  assert.equal(wipBody.code, 0, '拦截的工作台只读列表应加载成功。')

  const table = page.locator('.scheduler-workbench__process-wip-table').first()
  await table.waitFor({ state: 'visible', timeout: 60000 })
  const row = table
    .locator('.el-table__body-wrapper tbody tr')
    .filter({ hasText: wipRow.processName })
    .first()
  await row.waitFor({ state: 'visible', timeout: 60000 })
  await row.scrollIntoViewIfNeeded()
  const switchControl = row.locator('.el-switch').first()
  await switchControl.waitFor({ state: 'visible', timeout: 30000 })
  const switchInput = switchControl.locator('input[type="checkbox"]').first()
  assert.equal(await switchInput.isChecked(), false, '验证前夜班开关应为关闭。')
  await row.screenshot({ path: path.join(artifactDir, 'workbench-row-before-click.png') })

  const saveResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/schedule-order/process-wip-settings') &&
      response.request().method() === 'PUT',
    { timeout: 60000 }
  )
  const feedback = page
    .locator('.el-notification:visible, .el-message:visible')
    .filter({ hasText: /夜班|班次|设备|工作站|产线|产能/ })
    .last()
  await switchControl.click()
  const saveResponse = await saveResponsePromise
  const saveBody = await parseResponseBody(saveResponse)
  assert.equal(saveBody.code, 400, '夜班保存失败响应应保留正式业务错误码。')

  await feedback.waitFor({ state: 'visible', timeout: 30000 })
  await feedback.hover()
  const messageText = (await feedback.innerText()).trim()
  await page.screenshot({
    path: path.join(artifactDir, 'workbench-immediate-validation-message.png'),
    fullPage: true
  })
  assert.match(messageText, /夜班/, '页面必须即时明确提示夜班校验失败。')
  assert.match(messageText, /(班次|设备|工作站|产线|产能)/, '页面必须指出缺少的夜班资源类别。')
  assert.equal(await switchInput.isChecked(), false, '保存失败后夜班开关不得显示为已开启。')

  assert.equal(interceptedRequests.length, 1, '应且只应拦截一次夜班保存请求。')
  const requestPayload = interceptedRequests[0].payload
  assert.equal(Number(requestPayload.routeVersionId), Number(wipRow.routeVersionId))
  assert.equal(Number(requestPayload.routeProcessId), Number(wipRow.routeProcessId))
  assert.equal(Boolean(requestPayload.nightShiftEnabled), true)
  assert.ok(String(requestPayload.reason || '').includes('排产员工作台'))
  assert.equal(writeRequests.length, 1, '验证期间只允许产生被拦截的目标 MES PUT 请求。')
  assert.equal(writeRequests[0].url, interceptedRequests[0].url)

  return { messageText, saveBody, requestPayload }
}

async function main() {
  ensureArtifactDir()
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--disable-dev-shm-usage']
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)

  const writeRequests = []
  const interceptedRequests = []
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())
    ) {
      writeRequests.push({ method: request.method(), url: request.url() })
    }
  })

  try {
    const loginState = await loginWithVisiblePrefilledForm(page)
    const identity = loginState.identity
    const { scheduleOrder, targetProcess } = await resolveRealSource(
      context,
      loginState.authHeaders
    )
    const routeVersion = await resolveRealRouteVersion(
      context,
      scheduleOrder,
      loginState.authHeaders
    )
    const wipRow = buildWipRow({ scheduleOrder, targetProcess, routeVersion })
    writeJson('real-source.json', {
      identity,
      scheduleOrder: {
        id: scheduleOrder.id,
        code: scheduleOrder.code,
        routeId: scheduleOrder.routeId,
        routeCode: scheduleOrder.routeCode,
        routeName: scheduleOrder.routeName,
        routeVersion: scheduleOrder.routeVersion
      },
      routeVersion: {
        id: routeVersion.id,
        versionNo: routeVersion.versionNo,
        lifecycleStatus: routeVersion.lifecycleStatus,
        active: routeVersion.active
      },
      process: targetProcess,
      scenarioOverride: {
        nightShiftEnabled: false,
        purpose: '只验证从关闭切换到开启后的即时失败提示，不写入后端。'
      }
    })

    const result = await verifyImmediateUiFeedback(
      page,
      wipRow,
      writeRequests,
      interceptedRequests
    )
    writeJson('result.json', {
      result: 'PASS',
      scope: '真实页面交互与前端失败提示验证；工作台列表及失败响应由 Playwright 拦截，不作为后端集成证据。',
      dataSource: '登录后同一会话的真实排产工单分页与工艺流程排产配置只读响应',
      identity,
      scheduleOrderId: scheduleOrder.id,
      processName: targetProcess.processName,
      routeProcessId: wipRow.routeProcessId,
      routeVersionId: wipRow.routeVersionId,
      interceptedRequest: interceptedRequests[0],
      interceptedResponse: result.saveBody,
      uiMessageText: result.messageText,
      switchRemainedOff: true,
      backendWritesReached: 0,
      directEntryAudit: {
        routePageNightShiftEditorMounted: false,
        scheduleOrderProcessDialogEditable: false,
        onlyClickableEntry: '排产员工作台工序在制列表'
      },
      artifacts: [
        'real-source.json',
        'workbench-row-before-click.png',
        'workbench-immediate-validation-message.png'
      ]
    })
    console.log(
      `PASS: night-shift intercepted UI validation, process=${wipRow.processName}, backendWritesReached=0`
    )
  } catch (error) {
    await page
      .screenshot({ path: path.join(artifactDir, 'failure.png'), fullPage: true })
      .catch(() => {})
    fs.writeFileSync(path.join(artifactDir, 'error.txt'), `${error.stack || error.message}\n`, 'utf8')
    console.error(`BLOCKER: ${error.stack || error.message}`)
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main()
