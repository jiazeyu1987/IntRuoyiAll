const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.EDHR_PROCESS_ITEM_E2E_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.EDHR_PROCESS_ITEM_E2E_TENANT
const USERNAME = process.env.EDHR_PROCESS_ITEM_E2E_USERNAME
const PASSWORD = process.env.EDHR_PROCESS_ITEM_E2E_PASSWORD
const BATCH_ID = Number(process.env.EDHR_PROCESS_ITEM_E2E_BATCH_ID || 0)
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const RESULT_DIR = path.resolve(
  process.cwd(),
  'tests/output/20260710-edhr-batch-process-item-uniform-name'
)

function requirePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 只允许本机前端')
  assert.equal(TENANT, '芋道源码', '最终只读复验必须使用芋道源码租户')
  assert.equal(USERNAME, 'admin', '最终只读复验必须使用 admin')
  assert(PASSWORD, '缺少 EDHR_PROCESS_ITEM_E2E_PASSWORD')
  assert(BATCH_ID > 0, '缺少 EDHR_PROCESS_ITEM_E2E_BATCH_ID')
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
  const tenantInput = loginForm.locator('.el-select input[role="combobox"]').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill(TENANT)
    const option = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({
      hasText: TENANT
    }).first()
    if ((await option.count()) > 0) {
      await option.click()
    } else {
      await page.keyboard.press('Enter')
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
    `${label}失败：${body.msg || body.code}`
  )
  return body.data
}

function collectProcessGroups(detail) {
  const groups = new Map()
  for (const task of detail.tasks || []) {
    if (task.nodeType !== 'ROUTE_FORM') continue
    const key = String(task.routeProcessId || task.routeProcessSort || task.id)
    if (!groups.has(key)) {
      groups.set(key, {
        routeProcessSort: task.routeProcessSort || 0,
        processCode: task.processCode || '',
        processName: task.processName || ''
      })
    }
  }
  return [...groups.values()].sort(
    (left, right) =>
      left.routeProcessSort - right.routeProcessSort ||
      left.processName.localeCompare(right.processName)
  )
}

async function loadBatchDetail(page) {
  const detailResponse = page.waitForResponse(
    (response) =>
      response.url().includes(`/admin-api/mes/pro/edhr-batch-execution/get?id=${BATCH_ID}`) &&
      response.request().method() === 'GET',
    { timeout: 60000 }
  )
  await page.goto(
    `${BASE_URL}/mes/pro/feedback/edhr-batch-execution/detail?id=${BATCH_ID}`,
    { waitUntil: 'domcontentloaded', timeout: 60000 }
  )
  const detail = unwrapResponse(await (await detailResponse).json(), '批次详情')
  await page.locator('.edhr-batch-detail__process-task-group').first().waitFor({
    state: 'visible',
    timeout: 60000
  })
  await page.waitForFunction(
    () =>
      [...document.querySelectorAll('.el-loading-mask')].every(
        (element) => element.offsetParent === null
      ),
    null,
    { timeout: 60000 }
  )
  return detail
}

async function verifyProcessItems(page, detail) {
  const expectedGroups = collectProcessGroups(detail)
  const processGroups = page.locator('.edhr-batch-detail__process-task-group')
  assert.equal(
    await processGroups.count(),
    expectedGroups.length,
    '页面普通工序数量必须与接口工序分组一致'
  )

  const visibleNames = []
  for (let index = 0; index < expectedGroups.length; index += 1) {
    const expected = expectedGroups[index]
    const group = processGroups.nth(index)
    const name = group.locator('.edhr-batch-detail__review-process-name')
    const visibleText = (await name.innerText()).trim()
    const title = await name.getAttribute('title')
    const nameMetrics = await name.evaluate((element) => ({
      clientWidth: element.clientWidth,
      scrollWidth: element.scrollWidth
    }))
    assert.equal(visibleText, expected.processName, `第 ${index + 1} 个普通工序必须显示工序名称`)
    assert.equal(title, expected.processName, `第 ${index + 1} 个普通工序 title 必须为工序名称`)
    assert(
      nameMetrics.scrollWidth <= nameMetrics.clientWidth,
      `第 ${index + 1} 个普通工序名称必须完整显示：${JSON.stringify(nameMetrics)}`
    )
    if (expected.processCode && expected.processCode !== expected.processName) {
      assert.notEqual(visibleText, expected.processCode, `第 ${index + 1} 个普通工序不得显示工序编码`)
    }
    visibleNames.push(visibleText)
  }

  const metrics = await page.locator(
    '.edhr-batch-detail__pending-task-item, ' +
      '.edhr-batch-detail__process-task-group, ' +
      '.edhr-batch-detail__release-process-item'
  ).evaluateAll((elements) =>
    elements.map((element) => {
      const rect = element.getBoundingClientRect()
      const tag = element.querySelector('.el-tag')
      const tagRect = tag?.getBoundingClientRect()
      return {
        className: element.className,
        text: element.textContent?.replace(/\s+/g, ' ').trim(),
        height: rect.height,
        clientHeight: element.clientHeight,
        scrollHeight: element.scrollHeight,
        tagInside:
          !tagRect ||
          (tagRect.top >= rect.top - 0.5 &&
            tagRect.bottom <= rect.bottom + 0.5)
      }
    })
  )
  assert(metrics.length > expectedGroups.length, '页面必须同时包含特殊节点和放行节点')
  for (const metric of metrics) {
    assert(
      Math.abs(metric.height - 48) <= 0.5,
      `卡片高度必须统一为 48px：${JSON.stringify(metric)}`
    )
    assert(
      metric.scrollHeight <= metric.clientHeight,
      `卡片内容不得被垂直裁切：${JSON.stringify(metric)}`
    )
    assert(metric.tagInside, `状态标签不得超出卡片边界：${JSON.stringify(metric)}`)
  }

  return { visibleNames, metrics }
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
    const detail = await loadBatchDetail(page)
    const verification = await verifyProcessItems(page, detail)
    assert.deepEqual(mesWriteRequests, [], `只读 E2E 不得产生 MES 写请求：${mesWriteRequests.join(', ')}`)
    await page.screenshot({
      path: path.join(RESULT_DIR, 'process-items-uniform-name.png'),
      fullPage: true
    })
    fs.writeFileSync(
      path.join(RESULT_DIR, 'result.json'),
      `${JSON.stringify({
        tenant: TENANT,
        username: USERNAME,
        batchExecutionId: detail.id,
        batchCode: detail.batchCode,
        visibleNames: verification.visibleNames,
        metrics: verification.metrics,
        mesWriteRequests
      }, null, 2)}\n`,
      'utf8'
    )
    console.log(
      `PASS: ${verification.metrics.length} process items are 48px and show process names on batch ${detail.id}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
