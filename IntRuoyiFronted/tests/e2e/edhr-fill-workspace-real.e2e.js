const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_FILL_WORKSPACE_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_FILL_WORKSPACE_E2E_TENANT || '测试租户'
const USERNAME = process.env.EDHR_FILL_WORKSPACE_E2E_USERNAME || 'aoteman'
const PASSWORD = process.env.EDHR_FILL_WORKSPACE_E2E_PASSWORD
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const READONLY_ADMIN = process.env.EDHR_FILL_WORKSPACE_E2E_READONLY_ADMIN === '1'
const EXPLICIT_BATCH_ID = Number(process.env.EDHR_FILL_WORKSPACE_E2E_BATCH_ID || 0)
const RESULT_DIR = path.resolve(process.cwd(), 'tests/output/20260710-edhr-fill-workspace-real')
const VIEWPORTS = [
  { width: 1920, height: 1080, label: '1920x1080' },
  { width: 1366, height: 768, label: '1366x768' }
]

function requirePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 只允许本机前端')
  if (READONLY_ADMIN) {
    assert.equal(TENANT, '芋道源码', '管理员只读复验必须使用芋道源码租户')
    assert.equal(USERNAME, 'admin', '管理员只读复验必须使用 admin')
    assert(EXPLICIT_BATCH_ID > 0, '管理员只读复验必须指定批次 ID')
  } else {
    assert.equal(TENANT, '测试租户', '真实 E2E 必须使用测试租户')
    assert.equal(USERNAME, 'aoteman', '真实 E2E 必须使用测试账号 aoteman')
  }
  assert(PASSWORD, '缺少 EDHR_FILL_WORKSPACE_E2E_PASSWORD')
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
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })
  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.fill(TENANT)
    const option = page
      .locator('.el-select-dropdown__item:visible')
      .filter({ hasText: TENANT })
      .first()
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
    loginForm.locator(
      'input.el-input__inner:not([role="combobox"]):not([type="password"])'
    ),
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
  assert(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

function unwrapResponse(body, label) {
  assert(body.code === 0 || body.code === 200, `${label}失败：${body.msg || body.code}`)
  return body.data
}

async function findExecutionContext(page) {
  let records
  if (EXPLICIT_BATCH_ID > 0) {
    records = [{ id: EXPLICIT_BATCH_ID }]
  } else {
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
    let pageData = unwrapResponse(await (await pageResponse).json(), '批次列表加载')
    records = Array.isArray(pageData?.list)
      ? pageData.list
      : Array.isArray(pageData?.records)
        ? pageData.records
        : []
    const total = Number(pageData?.total || 0)
    if (total > records.length) {
      const sizeSelect = page.locator('.el-pagination__sizes .el-select').first()
      if ((await sizeSelect.count()) > 0 && (await sizeSelect.isVisible())) {
        const expandedResponse = page.waitForResponse(
          (response) =>
            response.url().includes('/admin-api/mes/pro/edhr-batch-execution/page') &&
            response.url().includes('pageSize=100') &&
            response.request().method() === 'GET',
          { timeout: 60000 }
        )
        await sizeSelect.click()
        const option = page
          .locator('.el-select-dropdown__item:visible')
          .filter({ hasText: /^100/ })
          .first()
        await option.waitFor({ state: 'visible', timeout: 30000 })
        await option.click()
        pageData = unwrapResponse(await (await expandedResponse).json(), '批次列表 100 条加载')
        records = Array.isArray(pageData?.list)
          ? pageData.list
          : Array.isArray(pageData?.records)
            ? pageData.records
            : []
      }
    }
  }

  const scanErrors = []
  for (const record of records.slice(0, 100)) {
    const batchExecutionId = Number(record.id)
    if (!Number.isFinite(batchExecutionId) || batchExecutionId <= 0) continue
    const detailResponse = page.waitForResponse(
      (response) =>
        response.url().includes(
          `/admin-api/mes/pro/edhr-batch-execution/get?id=${batchExecutionId}`
        ) && response.request().method() === 'GET',
      { timeout: 60000 }
    )
    await page.goto(
      `${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${batchExecutionId}`,
      { waitUntil: 'domcontentloaded', timeout: 60000 }
    )
    const detail = unwrapResponse(await (await detailResponse).json(), `批次 ${batchExecutionId}`)
    const existingTask = (detail.tasks || []).find((item) => Number(item.executionId) > 0)
    if (READONLY_ADMIN && existingTask) {
      return {
        batchExecutionId,
        batchCode: detail.batchCode,
        batchTaskId: existingTask.id,
        workTaskId: existingTask.workTaskId,
        executionId: existingTask.executionId
      }
    }
    const processButtons = page.locator('.edhr-batch-detail__process-task-group-head')
    const processCount = await processButtons.count()
    for (let processIndex = 0; processIndex < processCount; processIndex += 1) {
      await processButtons.nth(processIndex).click()
      const actionButtons = page.locator('.edhr-batch-detail__rail-process-form-action')
      const actionCount = await actionButtons.count()
      for (let actionIndex = 0; actionIndex < actionCount; actionIndex += 1) {
        const button = actionButtons.nth(actionIndex)
        if (!(await button.isVisible()) || (await button.isDisabled())) continue
        const label = (await button.innerText()).trim()
        if (!/打开填写|打开返工|继续填写/.test(label)) continue
        try {
          await button.click()
          await page.waitForURL(
            (url) => url.pathname === '/mes/pro/feedback/edhr-execution/form',
            { timeout: 60000 }
          )
        } catch (error) {
          scanErrors.push(
            `batch=${batchExecutionId}, process=${processIndex}: ${
              error instanceof Error ? error.message : String(error)
            }`
          )
          break
        }
        const url = new URL(page.url())
        return {
          batchExecutionId,
          batchCode: detail.batchCode,
          batchTaskId: Number(url.searchParams.get('batchTaskId')) || undefined,
          workTaskId: Number(url.searchParams.get('workTaskId')) || undefined,
          executionId:
            Number(url.searchParams.get('executionId')) ||
            Number(url.searchParams.get('id'))
        }
      }
    }
  }
  throw new Error(
    `当前租户前 ${Math.min(records.length, 100)} 个批次没有可打开的填写任务。扫描异常：${scanErrors.join(' | ')}`
  )
}

async function verifyWorkspace(page, context, viewport) {
  const query = new URLSearchParams({
    id: String(context.executionId),
    executionId: String(context.executionId),
    batchExecutionId: String(context.batchExecutionId),
    batchTaskId: String(context.batchTaskId),
    returnPath: '/mes/pro/feedback/edhr-batch-execution/detail'
  })
  if (context.workTaskId) query.set('workTaskId', String(context.workTaskId))

  await page.goto(`${BASE_URL}/mes/pro/feedback/edhr-execution/form?${query}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const workspace = page.locator('.edhr-fill-workspace')
  const rail = page.locator('.edhr-fill-workspace__rail')
  const canvas = page.locator('.edhr-fill-workspace__canvas')
  await workspace.waitFor({ state: 'visible', timeout: 60000 })
  await page.locator('.edhr-template-editable-form__sheet').waitFor({
    state: 'visible',
    timeout: 60000
  })

  const layout = await workspace.evaluate((element) => {
    const railElement = element.querySelector('.edhr-fill-workspace__rail')
    const canvasElement = element.querySelector('.edhr-fill-workspace__canvas')
    const rect = element.getBoundingClientRect()
    const railRect = railElement.getBoundingClientRect()
    const canvasRect = canvasElement.getBoundingClientRect()
    return {
      workspaceHeight: rect.height,
      railWidth: railRect.width,
      railHeight: railRect.height,
      canvasWidth: canvasRect.width
    }
  })
  assert.equal(Math.round(layout.railWidth), 240, `${viewport.label} 左侧控制栏宽度必须为 240px`)
  assert(
    layout.railHeight >= viewport.height - 190,
    `${viewport.label} 左侧控制栏必须占满主要可用高度`
  )
  assert(layout.canvasWidth > 700, `${viewport.label} 右侧表单区域宽度不足`)

  const widthButton = rail.getByRole('button', { name: '适应宽度' })
  const heightButton = rail.getByRole('button', { name: '适应高度' })
  assert.equal(await widthButton.getAttribute('aria-pressed'), 'true', '默认必须适应宽度')
  await heightButton.click()
  assert.equal(await heightButton.getAttribute('aria-pressed'), 'true', '必须可切换适应高度')
  await widthButton.click()
  assert.equal(await widthButton.getAttribute('aria-pressed'), 'true', '必须可切回适应宽度')

  await rail.getByRole('button', { name: '返回' }).waitFor({ state: 'visible' })
  await rail.getByRole('button', { name: '保存' }).waitFor({ state: 'visible' })
  await canvas.screenshot({
    path: path.join(RESULT_DIR, `fill-workspace-${viewport.label}.png`)
  })
}

async function main() {
  requirePrerequisites()
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: EXECUTABLE_PATH
  })
  let executionContext
  const results = []
  try {
    for (const viewport of VIEWPORTS) {
      const browserContext = await browser.newContext({
        viewport: { width: viewport.width, height: viewport.height }
      })
      const page = await browserContext.newPage()
      const mesWriteRequests = []
      page.on('request', (request) => {
        if (
          ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) &&
          request.url().includes('/admin-api/mes/')
        ) {
          mesWriteRequests.push(`${request.method()} ${request.url()}`)
        }
      })
      await login(page)
      executionContext ||= await findExecutionContext(page)
      await verifyWorkspace(page, executionContext, viewport)
      const unexpectedWrites = READONLY_ADMIN
        ? mesWriteRequests
        : mesWriteRequests.filter(
            (request) =>
              !request.includes('/admin-api/mes/pro/edhr-batch-execution/task/open')
          )
      assert.deepEqual(
        unexpectedWrites,
        [],
        `填写工作区 E2E 只允许打开任务写请求：${unexpectedWrites.join(', ')}`
      )
      results.push({ viewport: viewport.label, ...executionContext, mesWriteRequests })
      await browserContext.close()
    }
    fs.writeFileSync(
      path.join(RESULT_DIR, 'result.json'),
      `${JSON.stringify(
        { tenant: TENANT, username: USERNAME, readOnlyAdmin: READONLY_ADMIN, results },
        null,
        2
      )}\n`,
      'utf8'
    )
    console.log(
      `PASS: eDHR fill workspace verified on execution ${executionContext.executionId}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
