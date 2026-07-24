const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_COMPANION_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_COMPANION_E2E_TENANT || '测试租户'
const USERNAME = process.env.EDHR_COMPANION_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_COMPANION_E2E_PASSWORD
const EXPLICIT_BATCH_ID = Number(process.env.EDHR_COMPANION_E2E_BATCH_ID || 0)
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const READONLY_ADMIN = process.env.EDHR_COMPANION_E2E_READONLY_ADMIN === '1'
const STRUCTURAL_ONLY = process.env.EDHR_COMPANION_E2E_STRUCTURAL_ONLY === '1'
const RESULT_DIR = path.resolve(
  process.cwd(),
  'tests/output/20260710-edhr-process-companion-forms-real'
)
const REQUIRED_SLOTS = ['MAIN', 'LOSS_REPORT', 'PROCESS_INSPECTION', 'PARAMETER_RECORD']
const TASK_STATUS_WAITING = 0
const TASK_STATUS_APPROVED = 40
const TASK_STATUS_SKIPPED = 45
const PROCESS_STATE_BACKGROUNDS = {
  'is-completed': 'rgb(240, 249, 235)',
  'is-in-progress': 'rgb(255, 248, 230)',
  'is-not-started': 'rgb(247, 249, 252)'
}
const SLOT_LABELS = {
  MAIN: '主生产表',
  LOSS_REPORT: '损耗单',
  PROCESS_INSPECTION: '过程检验单',
  PARAMETER_RECORD: '参数记录表'
}

function requirePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 只允许本机前端 http://localhost:8081')
  if (READONLY_ADMIN) {
    assert.equal(TENANT, '芋道源码', '管理员只读复验必须使用芋道源码租户')
    assert.equal(USERNAME, 'admin', '管理员只读复验必须使用 admin')
  } else {
    assert.equal(TENANT, '测试租户', '真实 E2E 必须使用测试租户')
    assert.equal(USERNAME, 'aoteman', '真实 E2E 必须使用测试账号 aoteman')
  }
  assert(PASSWORD, '缺少 EDHR_COMPANION_E2E_PASSWORD')
  assert(EXECUTABLE_PATH, '缺少 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const input = locator.nth(index)
    if ((await input.isVisible()) && !(await input.isDisabled())) {
      await input.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写登录控件：${label}`)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if (
    (await loginForm.locator(
      '.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]'
    ).count()) > 0
  ) {
    throw new Error('登录页启用了验证码，无法执行无人值守真实 E2E')
  }

  const tenantInput = loginForm.locator(
    '.el-select input[role="combobox"], input.el-select__input'
  ).first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown__item:visible').filter({
      hasText: TENANT
    }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await tenantInput.press('Enter')
    }
  } else {
    await fillFirstVisible(
      loginForm.locator('input[placeholder="请输入租户名称"]'),
      TENANT,
      '租户'
    )
  }
  await fillFirstVisible(
    loginForm.locator('input.el-input__inner:not([role="combobox"]):not([type="password"])'),
    USERNAME,
    '账号'
  )
  await fillFirstVisible(loginForm.locator('input[type="password"]'), PASSWORD, '密码')
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: /^登录$/ }).click()
  const loginBody = await (await loginResponse).json()
  assert(
    loginBody.code === 0 || loginBody.code === 200,
    `登录失败：${loginBody.msg || loginBody.code}`
  )
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

function unwrapResponse(body, label) {
  assert(
    body.code === 0 || body.code === 200,
    `${label} 失败：${body.msg || body.code}`
  )
  return body.data
}

async function loadBatchCandidates(page) {
  const pageResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const pageData = unwrapResponse(await (await pageResponse).json(), '批次列表')
  const records = Array.isArray(pageData?.list)
    ? pageData.list
    : Array.isArray(pageData?.records)
      ? pageData.records
      : []
  const ids = [
    ...(EXPLICIT_BATCH_ID > 0 ? [EXPLICIT_BATCH_ID] : []),
    ...records.map((record) => Number(record.id)).filter((id) => Number.isFinite(id) && id > 0)
  ]
  return [...new Set(ids)].slice(0, 30)
}

async function loadBatchDetail(page, batchExecutionId) {
  const detailResponse = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${batchExecutionId}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(
    `${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${batchExecutionId}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  return unwrapResponse(await (await detailResponse).json(), `批次详情 ${batchExecutionId}`)
}

function findCompanionGroup(detail) {
  const groups = new Map()
  for (const task of detail.tasks || []) {
    if (!task.routeProcessId || !task.formSlotType) continue
    const tasks = groups.get(task.routeProcessId) || []
    tasks.push(task)
    groups.set(task.routeProcessId, tasks)
  }
  return [...groups.values()].find((tasks) => {
    const slots = new Set(tasks.map((task) => task.formSlotType))
    return REQUIRED_SLOTS.every((slot) => slots.has(slot))
  })
}

function findStructuralGroup(detail) {
  const groups = new Map()
  for (const task of detail.tasks || []) {
    if (!task.routeProcessId || !task.formSlotType) continue
    const tasks = groups.get(task.routeProcessId) || []
    tasks.push(task)
    groups.set(task.routeProcessId, tasks)
  }
  return [...groups.values()].find(
    (tasks) =>
      tasks.some((task) => task.formSlotType === 'MAIN') &&
      tasks.some((task) => task.executionId)
  )
}

function buildProcessStateGroups(detail) {
  const groups = new Map()
  for (const task of detail.tasks || []) {
    if (!task.routeProcessId || (task.nodeType && task.nodeType !== 'ROUTE_FORM')) continue
    const group = groups.get(task.routeProcessId) || {
      routeProcessId: task.routeProcessId,
      routeProcessSort: task.routeProcessSort || 0,
      processName: task.processName || task.processCode || String(task.routeProcessId),
      tasks: []
    }
    group.tasks.push(task)
    groups.set(task.routeProcessId, group)
  }
  return [...groups.values()].sort(
    (first, second) =>
      first.routeProcessSort - second.routeProcessSort ||
      first.routeProcessId - second.routeProcessId
  )
}

function resolveExpectedProcessState(group) {
  const requiredTasks = group.tasks.filter(
    (task) => task.requiredFlag !== false && Boolean(task.batchRecordReportId)
  )
  if (
    !requiredTasks.length ||
    requiredTasks.every(
      (task) => task.status === TASK_STATUS_APPROVED || task.status === TASK_STATUS_SKIPPED
    )
  ) {
    return 'is-completed'
  }
  return requiredTasks.some(
    (task) => task.status != null && task.status !== TASK_STATUS_WAITING
  )
    ? 'is-in-progress'
    : 'is-not-started'
}

async function verifyProcessStateBackgrounds(page, detail) {
  const groups = buildProcessStateGroups(detail)
  const groupRoots = page.locator('.edhr-batch-detail__process-task-group')
  assert.equal(await groupRoots.count(), groups.length, '页面普通工序数量必须与接口工序组一致')
  const stateSummary = []
  for (let index = 0; index < groups.length; index += 1) {
    const group = groups[index]
    const expectedState = resolveExpectedProcessState(group)
    const groupRoot = groupRoots.nth(index)
    await groupRoot.waitFor({ state: 'visible', timeout: 30000 })
    assert.equal(
      await groupRoot.locator('.el-tag').count(),
      0,
      `工序 ${group.processName} 不得显示完成计数标签`
    )
    assert(
      !/\d+\s*\/\s*\d+\s*已完成|工序已完成|无需填写/.test(await groupRoot.innerText()),
      `工序 ${group.processName} 不得显示完成计数文字`
    )
    const presentation = await groupRoot.evaluate((element, stateClass) => {
      const button = element.querySelector('.edhr-batch-detail__process-task-group-head')
      return {
        hasStateClass: element.classList.contains(stateClass),
        backgroundColor: button ? getComputedStyle(button).backgroundColor : ''
      }
    }, expectedState)
    assert.equal(
      presentation.hasStateClass,
      true,
      `工序 ${group.processName} 必须使用状态类 ${expectedState}`
    )
    assert.equal(
      presentation.backgroundColor,
      PROCESS_STATE_BACKGROUNDS[expectedState],
      `工序 ${group.processName} 背景色必须匹配 ${expectedState}`
    )
    stateSummary.push({
      routeProcessId: group.routeProcessId,
      processName: group.processName,
      state: expectedState,
      backgroundColor: presentation.backgroundColor
    })
  }
  return stateSummary
}

async function findRealCompanionBatch(page, batchIds) {
  const scanned = []
  let structuralCandidate
  for (const batchExecutionId of batchIds) {
    const detail = await loadBatchDetail(page, batchExecutionId)
    const group = findCompanionGroup(detail)
    scanned.push({
      batchExecutionId,
      batchCode: detail.batchCode,
      processSlots: (detail.tasks || [])
        .filter((task) => task.routeProcessId && task.formSlotType)
        .map((task) => `${task.routeProcessId}:${task.formSlotType}`)
    })
    if (group) return { detail, group, scanned, coverage: 'FULL_COMPANION' }
    if (STRUCTURAL_ONLY && !structuralCandidate) {
      const structuralGroup = findStructuralGroup(detail)
      if (structuralGroup) {
        structuralCandidate = {
          detail,
          group: structuralGroup,
          coverage: 'STRUCTURAL_ONLY'
        }
      }
    }
  }
  if (structuralCandidate) return { ...structuralCandidate, scanned }
  throw new Error(
    `当前租户批次中未找到 MAIN/LOSS_REPORT/PROCESS_INSPECTION/PARAMETER_RECORD 同工序任务组：${JSON.stringify(scanned)}`
  )
}

async function verifyGroupedForms(page, detail, group, coverage) {
  const routeProcessCount = new Set(
    (detail.tasks || [])
      .filter((task) => task.routeProcessId && task.formSlotType)
      .map((task) => task.routeProcessId)
  ).size
  assert.equal(
    await page.locator('.edhr-batch-detail__process-task-group').count(),
    routeProcessCount,
    '批次详情必须按 routeProcessId 渲染工序组'
  )
  const groupRoot = page.locator('.edhr-batch-detail__process-task-group').filter({
    hasText: group[0].processName || group[0].processCode
  }).first()
  await groupRoot.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(
    await groupRoot.locator('.edhr-batch-detail__process-task-form-item').count(),
    0,
    '左侧工序导航不得展开表单任务'
  )
  await groupRoot.locator('.edhr-batch-detail__process-task-group-head').click()

  const formPanel = page.locator('.edhr-batch-detail__rail-process-forms').first()
  await formPanel.waitFor({ state: 'visible', timeout: 60000 })
  assert.equal(
    await formPanel.locator('.edhr-batch-detail__rail-process-form-item').count(),
    group.length,
    '右侧详情必须展示当前工序全部表单任务'
  )

  const slotsToVerify = coverage === 'FULL_COMPANION'
    ? REQUIRED_SLOTS
    : [...new Set(group.map((task) => task.formSlotType))]
  for (const slot of slotsToVerify) {
    const task = group.find((item) => item.formSlotType === slot)
    assert(task, `接口任务组缺少 ${slot}`)
    const taskItem = formPanel.locator('.edhr-batch-detail__rail-process-form-item').filter({
      hasText: SLOT_LABELS[slot]
    }).first()
    await taskItem.waitFor({ state: 'visible', timeout: 30000 })
    await taskItem.getByText(SLOT_LABELS[slot], { exact: true }).waitFor({
      state: 'visible',
      timeout: 30000
    })
  }

  const selectedTask = group.find((task) => task.formSlotType !== 'MAIN') || group[0]
  const selectedItem = formPanel.locator('.edhr-batch-detail__rail-process-form-item').filter({
    hasText: SLOT_LABELS[selectedTask.formSlotType] || selectedTask.formSlotType
  }).first()
  await selectedItem.click()
  await assert.doesNotReject(async () => {
    await selectedItem.evaluate((element) => {
      if (!element.classList.contains('is-active')) {
        throw new Error('点击表单后未聚焦对应任务')
      }
    })
  })

  await page.screenshot({
    path: path.join(RESULT_DIR, 'companion-form-right-panel.png'),
    fullPage: true
  })
}

async function verifyReturnContext(page, detail, group) {
  const task = group.find((item) => item.executionId)
  assert(task, '同工序真实任务组缺少可只读打开的既有执行记录')
  await page.goto(
    `${BASE_URL}/mes/pro/feedback/edhr-execution/form?id=${task.executionId}` +
      `&batchExecutionId=${detail.id}&batchTaskId=${task.id}` +
      '&returnPath=/mes/pro/feedback/edhr-batch-execution/detail',
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const formTitle = page.locator('.edhr-page-shell__title').first()
  await formTitle.waitFor({ state: 'visible', timeout: 60000 })
  const formTitleText = (await formTitle.innerText()).trim()
  assert.match(formTitleText, /填写$/, `执行表单路由必须显示填写标题，实际为：${formTitleText}`)
  assert.notEqual(formTitleText, 'eDHR 执行详情', '执行表单路由不得继续显示执行详情标题')
  assert.equal(await page.getByText('执行摘要', { exact: true }).count(), 0, '填写模式不得显示执行摘要')
  assert.equal(await page.getByText('技术证据', { exact: true }).count(), 0, '填写模式不得显示技术证据')
  assert.equal(await page.getByText('最终表单归档', { exact: true }).count(), 0, '填写模式不得显示归档区')
  const backButton = page.getByRole('button', { name: '返回批次详情' }).first()
  await backButton.waitFor({ state: 'visible', timeout: 60000 })
  await backButton.click()
  await page.waitForURL(
    (url) =>
      url.pathname === '/mes/pro/feedback/edhr-batch-execution/detail' &&
      url.searchParams.get('id') === String(detail.id) &&
      url.searchParams.get('batchTaskId') === String(task.id),
    { timeout: 60000 }
  )
}

async function main() {
  requirePrerequisites()
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: EXECUTABLE_PATH
  })
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } })
  const page = await context.newPage()
  const mesWriteRequests = []
  page.on('request', (request) => {
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
      request.url().includes('/admin-api/mes/')
    ) {
      mesWriteRequests.push(`${request.method()} ${request.url()}`)
    }
  })

  try {
    await login(page)
    const batchIds = await loadBatchCandidates(page)
    assert(batchIds.length > 0, '测试租户批次列表没有可验收数据')
    const { detail, group, scanned, coverage } = await findRealCompanionBatch(page, batchIds)
    const processStateSummary = await verifyProcessStateBackgrounds(page, detail)
    await verifyGroupedForms(page, detail, group, coverage)
    await verifyReturnContext(page, detail, group)
    assert.deepEqual(mesWriteRequests, [], `只读 E2E 不得产生 MES 写请求：${mesWriteRequests.join(', ')}`)
    fs.writeFileSync(
      path.join(RESULT_DIR, 'result.json'),
      `${JSON.stringify({
        tenant: TENANT,
        username: USERNAME,
        readOnlyAdmin: READONLY_ADMIN,
        coverage,
        batchExecutionId: detail.id,
        batchCode: detail.batchCode,
        routeProcessId: group[0].routeProcessId,
        slots: group.map((task) => task.formSlotType),
        processStateSummary,
        scanned,
        mesWriteRequests
      }, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: eDHR process group ${coverage} verified on batch ${detail.id}, routeProcessId ${group[0].routeProcessId}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
