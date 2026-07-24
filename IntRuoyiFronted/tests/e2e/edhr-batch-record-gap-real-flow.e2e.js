const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_T12_E2E_BASE_URL || 'http://localhost:8081'
const BACKEND_URL = process.env.EDHR_T12_E2E_BACKEND_URL || 'http://127.0.0.1:48081'
const TEST_TENANT = '测试租户'
const TEST_USERNAME = 'aoteman'
const TEST_PASSWORD = process.env.EDHR_T12_E2E_LOGIN_PASSWORD || '111111'
const ADMIN_TENANT = '芋道源码'
const ADMIN_USERNAME = 'admin'
const ADMIN_PASSWORD = process.env.EDHR_T12_E2E_ADMIN_PASSWORD || 'admin123'
const TASK_STATE_PATH = path.resolve(__dirname, '../../../doc/tasks/20260613-batch-record-gap-implementation/task-state.json')

const REQUIRED_REAL_GATES = ['T4', 'T5', 'T8', 'T9', 'T10', 'T11']

const COVERED_FLOWS = [
  { taskId: 'T4', script: 'edhr-auto-schedule-batch-real-flow.e2e.js', behavior: 'auto schedule creates one eDHR batch from a real scheduled work order' },
  { taskId: 'T5', script: 'edhr-candidate-upstream-real-flow.e2e.js', behavior: 'candidate pool, submit signature, upstream review tasks, and peer cancellation remain valid' },
  { taskId: 'T8', script: 'edhr-t8-advance-gate-real-flow.e2e.js', behavior: 'form task close, review aggregation, parallel gating, special node gating, and next fill task creation' },
  { taskId: 'T9', script: 'edhr-t9-close-blockers-real-flow.e2e.js', behavior: 'batch close rejects incomplete real data and returns visible blockers without changing state' },
  { taskId: 'T10', script: 'edhr-t10-readonly-review-real-flow.e2e.js', behavior: 'closed batch readonly review shows route forms, special nodes, signatures, approvals, rework, audits, and tracking' },
  { taskId: 'T11', script: 'edhr-t11-final-archive-pdf-real-flow.e2e.js', behavior: 'final SEALED archive PDF contains the complete closed batch evidence and remains controlled-downloadable' }
]

function blocked(message, details = []) {
  const error = new Error(message)
  error.blocked = true
  error.details = details
  return error
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', 'T12 gap E2E must use local frontend http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, 'T12 gap E2E must use local backend 48081')
}

function readTaskState() {
  return JSON.parse(fs.readFileSync(TASK_STATE_PATH, 'utf8'))
}

function requireRealGate(taskId, state) {
  const task = state.tasks.find((item) => item.task_id === taskId)
  if (!task) {
    throw blocked(`task-state.json 缺少 ${taskId} 状态，不能判断 T12 系统级门禁。`)
  }
  const accepted = task.status === 'validated_real_e2e_pass' || (taskId === 'T5' && task.status === 'validated_stage_pass')
  if (!accepted) {
    throw blocked(`T12 系统级总验收依赖 ${taskId} 先达到真实或已放行阶段门禁。`, [
      `当前 ${taskId} 状态：${task.status}`,
      `当前 ${taskId} 结果：${task.last_outcome || '--'}`
    ])
  }
}

function requirePrerequisites() {
  const blockers = []
  const state = readTaskState()
  for (const taskId of REQUIRED_REAL_GATES) {
    try {
      requireRealGate(taskId, state)
    } catch (error) {
      if (!error.blocked) throw error
      blockers.push(error.message)
      blockers.push(...(error.details || []))
    }
  }
  for (const flow of COVERED_FLOWS) {
    const scriptPath = path.resolve(__dirname, flow.script)
    if (!fs.existsSync(scriptPath)) {
      blockers.push(`缺少 ${flow.taskId} 真实 E2E 脚本：${flow.script}`)
    }
  }
  if (blockers.length > 0) {
    throw blocked('T12 系统级缺口总验收前置条件未满足。', [
      ...blockers,
      '不得用 mock、接口造成功、SQL 造最终状态、跳过签名、外部服务器或非本地租户替代真实 E2E。'
    ])
  }
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.fill(value)
      return
    }
  }
  throw blocked(`缺少可填写控件：${label}`)
}

async function clickFirstEnabled(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible()) && !(await item.isDisabled())) {
      await item.click()
      return
    }
  }
  throw blocked(`缺少可点击控件：${label}`)
}

async function login(page, tenant, username, password) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.evaluate(() => window.localStorage.clear())
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw blocked('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(tenant)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), tenant, '租户')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), username, '账号')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), password, '密码')
  await clickFirstEnabled(loginForm.locator('button.el-button--primary'), '登录')
  await page.waitForFunction(
    () => !window.location.href.includes('/login') || Boolean(window.localStorage.getItem('ACCESS_TOKEN')),
    { timeout: 60000 }
  )
}

async function assertAdminReadonlyReview(page) {
  const writeRequests = []
  page.on('request', (request) => {
    const method = request.method()
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) && request.url().includes('/admin-api/mes/')) {
      writeRequests.push(`${method} ${request.url()}`)
    }
  })
  await login(page, ADMIN_TENANT, ADMIN_USERNAME, ADMIN_PASSWORD)
  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(1000)
  assert.equal(writeRequests.length, 0, `admin readonly verification must not write MES data: ${writeRequests.join(', ')}`)
}

async function runGapAcceptance() {
  assertLocalOnly()
  requirePrerequisites()

  const browser = await chromium.launch({ headless: true })
  try {
    const context = await browser.newContext({ acceptDownloads: true })
    const page = await context.newPage()
    await login(page, TEST_TENANT, TEST_USERNAME, TEST_PASSWORD)
    await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-batch-execution`, { waitUntil: 'networkidle' })
    for (const flow of COVERED_FLOWS) {
      console.log(`T12 covers ${flow.taskId}: ${flow.behavior} via ${flow.script}`)
    }
    await assertAdminReadonlyReview(page)
  } finally {
    await browser.close()
  }
}

runGapAcceptance().catch((error) => {
  if (error.blocked) {
    console.error(`BLOCKED: ${error.message}`)
    for (const detail of error.details || []) console.error(`- ${detail}`)
    process.exit(2)
  }
  console.error(error)
  process.exit(1)
})
