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
  'night-shift-capacity-status',
  'real-ui-current'
)

const baseUrl = (
  process.env.MES_SCHEDULER_NIGHT_CAPACITY_BASE_URL || 'http://127.0.0.1:8081'
).replace(/\/+$/, '')

const isVisible = async (locator) =>
  (await locator.count()) > 0 && locator.isVisible().catch(() => false)

const parseBody = (response) =>
  response.json().catch(async () => ({ raw: await response.text().catch(() => '') }))

function assessTenantJobResult(rawResult) {
  assert.equal(typeof rawResult, 'string', '自动排产最近结果缺少租户级结果 JSON。')
  let parsed
  try {
    parsed = JSON.parse(rawResult)
  } catch (error) {
    assert.fail(`自动排产租户级结果不是有效 JSON：${error.message}`)
  }
  assert.ok(
    parsed && typeof parsed === 'object' && !Array.isArray(parsed),
    '自动排产租户级结果必须是租户到执行结果的对象。'
  )
  const entries = Object.entries(parsed)
  assert.ok(entries.length > 0, '自动排产最近日志缺少租户级执行结果。')
  assert.ok(
    entries.every(([, message]) => typeof message === 'string' && message.trim()),
    '自动排产租户级执行结果必须是非空文本。'
  )
  const successCount = entries.filter(([, message]) =>
    message.startsWith('夜间重排完成：')
  ).length
  const failureCount = entries.length - successCount
  return {
    tenantCount: entries.length,
    successCount,
    failureCount,
    expectedStatus:
      failureCount === 0 ? 'SUCCESS' : successCount === 0 ? 'FAILURE' : 'PARTIAL_FAILURE'
  }
}

async function loginWithPrefilledLocalForm(page) {
  await page.goto(
    `${baseUrl}/login?redirect=${encodeURIComponent('/mes/pro/scheduler-workbench')}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 }).catch(() => {})
  if (!(await isVisible(form)) && !page.url().includes('/login')) {
    return { tenant: '当前登录租户', username: '当前登录用户' }
  }
  assert.ok(await isVisible(form), `登录表单未出现，当前地址：${page.url()}`)
  assert.equal(
    await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count(),
    0,
    '登录页验证码已开启，无法执行无人值守只读验证。'
  )

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  const usernameInput = form
    .locator('input[placeholder="请输入用户名"], input.el-input__inner:not([role="combobox"])')
    .first()
  const passwordInput = form.locator('input[placeholder="请输入密码"], input[type="password"]').first()
  const tenant = (await tenantInput.count()) > 0 ? (await tenantInput.inputValue()).trim() : '默认租户'
  const username = (await usernameInput.inputValue()).trim()
  assert.ok(username, '本机登录页必须提供预填用户名。')
  assert.ok((await passwordInput.inputValue()).trim(), '本机登录页必须提供预填密码。')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await form.getByRole('button', { name: /登录/ }).click()
  const response = await loginResponsePromise
  const body = await parseBody(response)
  assert.equal(response.status(), 200, `登录 HTTP ${response.status()}`)
  assert.equal(body.code, 0, `登录失败：${body.msg || body.code}`)
  await page.waitForFunction(() => !window.location.pathname.includes('/login'), null, {
    timeout: 60000
  })
  return { tenant: tenant || '默认租户', username }
}

async function main() {
  fs.mkdirSync(artifactDir, { recursive: true })
  const browser = await chromium.launch({ headless: true, args: ['--disable-dev-shm-usage'] })
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, locale: 'zh-CN' })
  const page = await context.newPage()
  page.setDefaultTimeout(60000)
  page.setDefaultNavigationTimeout(60000)
  const writes = []
  const pageErrors = []
  let captureWrites = false
  let capturePageErrors = false
  page.on('request', (request) => {
    if (
      captureWrites &&
      request.url().includes('/admin-api/') &&
      !['GET', 'HEAD', 'OPTIONS'].includes(request.method())
    ) {
      writes.push({ method: request.method(), url: request.url() })
    }
  })
  page.on('pageerror', (error) => {
    if (capturePageErrors) {
      pageErrors.push(error.message)
    }
  })

  try {
    const identity = await loginWithPrefilledLocalForm(page)
    await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
    captureWrites = true
    capturePageErrors = true
    const summaryPromise = page.waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/scheduler-workbench/summary') &&
        response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(`${baseUrl}/mes/pro/scheduler-workbench?nightCapacity=${Date.now()}`, {
      waitUntil: 'domcontentloaded',
      timeout: 60000
    })
    const summaryResponse = await summaryPromise
    const summaryBody = await parseBody(summaryResponse)
    assert.equal(summaryBody.code, 0, `工作台加载失败：${summaryBody.msg || summaryBody.code}`)

    const statusPromise = page.waitForResponse(
      (response) =>
        response.url().includes(
          '/admin-api/mes/pro/scheduler-workbench/night-shift-capacity/status'
        ) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    const jobStatusPromise = page.waitForResponse(
      (response) =>
        response.url().includes(
          '/admin-api/mes/pro/scheduler-workbench/auto-schedule-job/status'
        ) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.getByRole('button', { name: /排产设置/ }).first().click()
    const [statusResponse, jobStatusResponse] = await Promise.all([
      statusPromise,
      jobStatusPromise
    ])
    const statusBody = await parseBody(statusResponse)
    const jobStatusBody = await parseBody(jobStatusResponse)
    assert.equal(statusResponse.status(), 200, `夜班容量状态 HTTP ${statusResponse.status()}`)
    assert.equal(statusBody.code, 0, `夜班容量状态失败：${statusBody.msg || statusBody.code}`)
    const status = statusBody.data
    assert.equal(typeof status.available, 'boolean', '夜班容量状态缺少 available。')
    assert.ok(Number(status.availableShiftCount) >= 0, '可用夜班数必须为非负数。')
    assert.ok(Number(status.capacityLineCount) >= 0, '夜班产能产线数必须为非负数。')
    assert.ok(Array.isArray(status.shifts), '夜班容量状态缺少 shifts 数组。')
    assert.equal(jobStatusResponse.status(), 200, `自动排产状态 HTTP ${jobStatusResponse.status()}`)
    assert.equal(jobStatusBody.code, 0, `自动排产状态失败：${jobStatusBody.msg || jobStatusBody.code}`)
    const jobStatus = jobStatusBody.data
    assert.equal(typeof jobStatus.configured, 'boolean', '自动排产状态缺少 configured。')
    assert.equal(typeof jobStatus.enabled, 'boolean', '自动排产状态缺少 enabled。')
    const tenantResultAssessment = assessTenantJobResult(jobStatus.latestResult)
    assert.equal(
      jobStatus.latestStatus,
      tenantResultAssessment.expectedStatus,
      `自动排产状态未反映租户级结果：成功 ${tenantResultAssessment.successCount}，失败 ${tenantResultAssessment.failureCount}`
    )

    const dialog = page.locator('.scheduler-workbench__settings-dialog').first()
    await dialog.waitFor({ state: 'visible', timeout: 60000 })
    await dialog.getByText('默认允许使用夜班', { exact: true }).waitFor({ state: 'visible' })
    await dialog
      .getByText(/人效h仅影响资源计算模式且产能来源为人工的工序/)
      .waitFor({ state: 'visible' })
    await dialog.getByText('可用夜班班次与产能', { exact: true }).waitFor({ state: 'visible' })
    if (status.available) {
      await dialog.getByText('可用', { exact: true }).waitFor({ state: 'visible' })
      await dialog
        .getByText(
          `可用夜班 ${status.availableShiftCount} 个，具备夜班产能的产线 ${status.capacityLineCount} 条`,
          { exact: true }
        )
        .waitFor({ state: 'visible' })
    } else if (Number(status.availableShiftCount) === 0) {
      await dialog.getByText('不可用', { exact: true }).waitFor({ state: 'visible' })
      await dialog.getByText('没有可用夜班班次', { exact: true }).waitFor({ state: 'visible' })
    } else {
      await dialog.getByText('不可用', { exact: true }).waitFor({ state: 'visible' })
      await dialog
        .getByText('已有夜班班次，但没有可用夜班产能', { exact: true })
        .waitFor({ state: 'visible' })
    }
    const jobStatusCard = dialog
      .locator('.scheduler-workbench__runtime-card')
      .filter({ hasText: '自动排产任务' })
      .first()
    const jobStatusText = (await jobStatusCard.innerText()).trim()
    assert.match(jobStatusText, /处理器 mesProNightlyReplanJob/, '页面必须展示真实自动排产处理器。')
    assert.match(jobStatusText, /下次触发/, '页面必须展示自动排产下次触发状态。')
    assert.match(jobStatusText, /最近日志/, '页面必须展示自动排产最近日志。')
    assert.equal(
      typeof jobStatus.latestResultSummary,
      'string',
      '自动排产状态缺少租户级结果摘要。'
    )
    assert.ok(jobStatus.latestResultSummary.trim(), '自动排产租户级结果摘要不能为空。')
    assert.ok(
      jobStatusText.includes(jobStatus.latestResultSummary),
      '页面必须逐字展示 API 返回的租户级结果摘要。'
    )
    const latestLogSegment = jobStatusText.match(/最近日志\s*([\s\S]*?)；执行结果/)?.[1]
    assert.ok(latestLogSegment, '页面缺少可核对的最近日志状态段。')
    if (tenantResultAssessment.failureCount > 0) {
      const expectedFailureText =
        tenantResultAssessment.expectedStatus === 'PARTIAL_FAILURE' ? '部分失败' : '失败'
      assert.ok(latestLogSegment.includes(expectedFailureText), '页面未显示租户级失败状态。')
      assert.doesNotMatch(latestLogSegment, /成功/, '任务级失败不得显示为成功。')
    }
    if (jobStatus.configured) {
      assert.match(jobStatusText, /已注册/, '已配置 Job 必须显示已注册。')
      assert.ok(jobStatus.cronExpression, '已配置 Job 必须返回 cron。')
      assert.ok(jobStatusText.includes(jobStatus.cronExpression), '页面必须展示后端返回的 cron。')
    } else {
      assert.match(jobStatusText, /未注册/, '未配置 Job 必须明确显示未注册。')
    }

    await page.waitForFunction(
      () =>
        document.querySelectorAll('.scheduler-workbench__settings-dialog .el-loading-mask').length === 0,
      null,
      { timeout: 60000 }
    )
    const capacityCard = dialog
      .locator('.scheduler-workbench__runtime-card')
      .filter({ hasText: '可用夜班班次与产能' })
      .first()
    const cardReceivesPointer = await capacityCard.evaluate((element) => {
      const rect = element.getBoundingClientRect()
      const hit = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2)
      return hit === element || (hit instanceof Node && element.contains(hit))
    })
    assert.equal(cardReceivesPointer, true, '夜班容量状态卡仍被加载遮罩或其它元素覆盖。')

    await page.screenshot({
      path: path.join(artifactDir, 'night-shift-capacity-status.png'),
      fullPage: true
    })
    assert.deepEqual(writes, [], `只读验证产生写请求：${JSON.stringify(writes)}`)
    assert.deepEqual(pageErrors, [], `页面脚本错误：${JSON.stringify(pageErrors)}`)
    fs.writeFileSync(
      path.join(artifactDir, 'result.json'),
      JSON.stringify(
        {
          result: 'PASS',
          baseUrl,
          backendHealthUrl: 'http://127.0.0.1:48081/actuator/health',
          identity,
          status,
          jobStatus,
          tenantResultAssessment,
          jobStatusText,
          writes,
          pageErrors,
          screenshot: 'night-shift-capacity-status.png'
        },
        null,
        2
      ),
      'utf8'
    )
    console.log(
      `PASS: night-shift capacity status real UI, available=${status.available}, writes=0`
    )
  } catch (error) {
    await page
      .screenshot({ path: path.join(artifactDir, 'failure.png'), fullPage: true })
      .catch(() => {})
    fs.writeFileSync(path.join(artifactDir, 'error.txt'), `${error.stack || error.message}\n`, 'utf8')
    console.error(error.stack || error.message)
    process.exitCode = 1
  } finally {
    await browser.close()
  }
}

main()
