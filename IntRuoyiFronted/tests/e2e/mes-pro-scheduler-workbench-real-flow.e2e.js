const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error("Playwright is required for MES scheduler workbench E2E. Run in a workspace where 'playwright' is installed.")
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULER_WORKBENCH_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, ''),
  tenant: process.env.MES_SCHEDULER_WORKBENCH_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULER_WORKBENCH_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULER_WORKBENCH_E2E_PASSWORD || 'admin123',
  headed: process.env.MES_SCHEDULER_WORKBENCH_E2E_HEADED === '1'
}

const expectedMetricLabels = [
  '待排工单',
  '今日已排任务',
  '今日可用产能',
  '今日报工数量',
  '报工偏差',
  '设备维修中',
  '全局治理风险',
  '物料短缺'
]

const hiddenWorkbenchSections = ['快捷入口', '夜间自动重排', '瓶颈建议', '操作顺序']
const expectedRehearsalTexts = [
  '演练上下文',
  '当前对象',
  '下一步入口',
  '复盘摘要',
  'A1 ERP同步',
  'A2 排产发布',
  'A3 工艺产能',
  'A4 报工执行',
  'A5 审批复盘'
]

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {})
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
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/scheduler-workbench`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.evaluate(() => localStorage.clear())
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/scheduler-workbench`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  const loginForm = page.locator('.login-form:visible').first()
  if ((await loginForm.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.fill(config.tenant)
    await tenantInput.press('Enter')
  } else {
    await fillFirstVisible(loginForm.locator('input[placeholder="请输入租户名称"]'), config.tenant, 'tenant')
  }
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入用户名"]'), config.username, 'username')
  await fillFirstVisible(loginForm.locator('input[placeholder="请输入密码"]'), config.password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    loginForm.locator('.el-button--primary').first().click()
  ])
  const loginBody = await loginResponse.json()
  if (loginBody.code !== 0) {
    throw new Error(`登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, { timeout: 60000 })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openWorkbench(page) {
  const summaryPromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') && response.status() === 200,
    { timeout: 60000 }
  )
  await page.goto(`${config.baseUrl}/mes/pro/scheduler-workbench`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const summaryResponse = await summaryPromise
  const summaryBody = await summaryResponse.json()
  assert.equal(summaryBody.code, 0, `工作台 summary 接口业务错误: ${summaryBody.msg || summaryBody.code}`)
  await page.locator('.scheduler-workbench').waitFor({ state: 'visible', timeout: 30000 })
  return summaryBody.data
}

async function assertWorkbenchContent(page, summary) {
  assert.ok('pendingScheduleOrderCount' in summary, 'summary 缺少待排工单数量')
  assert.ok('todayScheduledTaskCount' in summary, 'summary 缺少今日已排任务数量')
  assert.ok('todayAvailableCapacity' in summary, 'summary 缺少今日可用产能')
  assert.ok('todayFeedbackQuantity' in summary, 'summary 缺少今日报工数量')
  assert.ok('pendingApprovalFeedbackCount' in summary, 'summary 缺少待审批报工数量')
  assert.ok('reportedDeviationText' in summary, 'summary 缺少报工偏差说明')

  const workbench = page.locator('.scheduler-workbench')
  await page.getByText('排产员工作台').first().waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(await workbench.locator('.scheduler-workbench__metric').count(), 8, '工作台必须只展示 8 个概览卡片')

  for (const text of expectedMetricLabels) {
    await workbench.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }

  for (const text of expectedRehearsalTexts) {
    await workbench.getByText(text).first().waitFor({ state: 'visible', timeout: 30000 })
  }

  for (const text of hiddenWorkbenchSections) {
    assert.equal(await workbench.getByText(text).count(), 0, `${text} 不应在排产员工作台显示`)
  }

  assert.equal(await workbench.locator('.scheduler-workbench__quick-links').count(), 0)
  assert.equal(await workbench.locator('.scheduler-workbench__section').count(), 0)
  assert.equal(await workbench.locator('.el-table').count(), 0)
  assert.equal(await workbench.locator('.scheduler-workbench__rehearsal-link').count(), 5)
  assert.equal(await workbench.locator('.scheduler-workbench__review-summary').count(), 1)
}

async function main() {
  assert.notEqual(config.tenant, '芋道源码', '真实 E2E 写入/调试不能使用芋道源码租户')
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1366, height: 900 } })
  try {
    await login(page)
    const summary = await openWorkbench(page)
    await assertWorkbenchContent(page, summary)
    console.log('mes-pro-scheduler-workbench-real-flow: PASS')
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
