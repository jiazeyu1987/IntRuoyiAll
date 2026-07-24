const assert = require('node:assert/strict')

function loadPlaywright() {
  try {
    return require('playwright')
  } catch (error) {
    throw new Error(
      "Playwright is required for MES schedule freeze E2E. Run in a workspace where 'playwright' is installed."
    )
  }
}

const config = {
  baseUrl: (process.env.MES_SCHEDULE_FREEZE_E2E_BASE_URL || 'http://127.0.0.1:8081').replace(
    /\/+$/,
    ''
  ),
  tenant: process.env.MES_SCHEDULE_FREEZE_E2E_TENANT || '测试租户',
  username: process.env.MES_SCHEDULE_FREEZE_E2E_USERNAME || 'aoteman',
  password: process.env.MES_SCHEDULE_FREEZE_E2E_PASSWORD || '111111',
  workOrderCode: process.env.MES_SCHEDULE_FREEZE_E2E_WORK_ORDER_CODE || 'CODexERP20260610E',
  targetDate: process.env.MES_SCHEDULE_FREEZE_E2E_TARGET_DATE || '2026-07-04',
  headed: process.env.MES_SCHEDULE_FREEZE_E2E_HEADED === '1'
}

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
  await page.goto(`${config.baseUrl}/login?redirect=/mes/pro/schedule-order`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)

  if (!page.url().includes('/login')) {
    return
  }

  if (
    (await page
      .locator(
        '.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible'
      )
      .count()) > 0
  ) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实 E2E。')
  }

  const tenantInput = page.locator('.el-select input[role="combobox"]:visible').first()
  if ((await tenantInput.count()) > 0) {
    await tenantInput.click()
    await tenantInput.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A')
    await page.keyboard.type(config.tenant)
    await tenantInput.press('Enter')
    await tenantInput.press('Tab')
  } else {
    await fillFirstVisible(
      page.locator('input[placeholder="请输入租户名称"]'),
      config.tenant,
      'tenant'
    )
  }
  await fillFirstVisible(
    page.locator('input[placeholder="请输入用户名"]'),
    config.username,
    'username'
  )
  await fillFirstVisible(
    page.locator('input[placeholder="请输入密码"]'),
    config.password,
    'password'
  )

  const loginResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  await page.locator('.el-button--primary:visible').first().click()
  const loginResponse = await loginResponsePromise
  if (loginResponse) {
    const loginBody = await loginResponse.json()
    assert.equal(loginBody.code, 0, `登录接口返回业务错误: ${loginBody.msg || loginBody.code}`)
  }
  await page.waitForFunction(() => Boolean(localStorage.getItem('ACCESS_TOKEN')), null, {
    timeout: 60000
  })
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function authHeaders(page) {
  const cache = await page.evaluate(() => ({
    accessToken: localStorage.getItem('ACCESS_TOKEN'),
    tenantId: localStorage.getItem('tenantId') || localStorage.getItem('TENANT_ID')
  }))
  assert.ok(cache.accessToken, '已登录上下文缺少 ACCESS_TOKEN。')
  let accessToken = cache.accessToken
  try {
    const parsed = JSON.parse(cache.accessToken)
    accessToken =
      typeof parsed?.v === 'string' ? JSON.parse(parsed.v) : parsed?.v || cache.accessToken
  } catch (error) {
    accessToken = cache.accessToken
  }
  return {
    Authorization: `Bearer ${accessToken}`,
    'tenant-id': cache.tenantId || (config.tenant === '芋道源码' ? '1' : '122')
  }
}

async function apiGetTask(page, taskId) {
  const response = await page.request.get(
    `${config.baseUrl}/admin-api/mes/pro/task/get?id=${taskId}`,
    {
      headers: await authHeaders(page)
    }
  )
  assert.equal(response.status(), 200, `任务详情接口 HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `任务详情接口业务错误: ${body.msg || body.code}`)
  return body.data
}

async function apiUnlockTask(page, taskId) {
  const response = await page.request.put(
    `${config.baseUrl}/admin-api/mes/pro/task/unlock?taskId=${taskId}`,
    {
      headers: await authHeaders(page)
    }
  )
  assert.equal(response.status(), 200, `解锁任务接口 HTTP ${response.status()}`)
  const body = await response.json()
  assert.equal(body.code, 0, `解锁任务接口业务错误: ${body.msg || body.code}`)
}

async function openCalendarDate(page) {
  await page.goto(`${config.baseUrl}/mes/pro/schedule-calendar`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await page.locator('.schedule-calendar-page').waitFor({ state: 'visible', timeout: 30000 })
  await waitCalendarSettled(page)
  await navigateToTargetMonth(page)
  await page.waitForFunction(
    () =>
      !Array.from(document.querySelectorAll('.el-loading-mask')).some((item) => {
        const style = window.getComputedStyle(item)
        return (
          style.display !== 'none' &&
          style.visibility !== 'hidden' &&
          Number(style.opacity || 1) > 0
        )
      }),
    null,
    { timeout: 60000 }
  )
  await page.waitForTimeout(1000)
  const cell = page.locator(`.calendar-cell[data-date="${config.targetDate}"]`).first()
  if ((await cell.count()) === 0) {
    const title = await page
      .locator('.toolbar-title-group h2')
      .first()
      .innerText()
      .catch(() => '')
    const dates = await page
      .locator('.calendar-cell')
      .evaluateAll((items) => items.slice(0, 50).map((item) => item.getAttribute('data-date')))
      .catch(() => [])
    throw new Error(
      `日历未渲染目标日期 ${config.targetDate}，当前标题=${title}，可见日期=${dates.join(',')}`
    )
  }
  await cell.waitFor({ state: 'visible', timeout: 30000 })
  const detailResponsePromise = page
    .waitForResponse(
      (response) =>
        response.url().includes('/admin-api/mes/pro/schedule-calendar/day-detail') &&
        response.status() === 200,
      { timeout: 60000 }
    )
    .catch(() => null)
  await cell.click()
  const detailResponse = await detailResponsePromise
  if (detailResponse) {
    const detailBody = await detailResponse.json()
    assert.equal(detailBody.code, 0, `日详情接口业务错误: ${JSON.stringify(detailBody)}`)
    const taskCount = (detailBody.data?.workshops || [])
      .flatMap((workshop) => workshop.lines || [])
      .flatMap((line) => line.tasks || []).length
    if (taskCount === 0) {
      throw new Error(`日详情接口未返回任务，date=${config.targetDate}`)
    }
  }
  await page.locator('.task-card').first().waitFor({ state: 'visible', timeout: 30000 })
}

async function navigateToTargetMonth(page) {
  const targetMatch = config.targetDate.match(/^(\d{4})-(\d{2})-\d{2}$/)
  if (!targetMatch) {
    throw new Error(`Invalid MES_SCHEDULE_FREEZE_E2E_TARGET_DATE: ${config.targetDate}`)
  }
  for (let attempt = 0; attempt < 36; attempt += 1) {
    const dates = await visibleCalendarDates(page)
    if (dates.includes(config.targetDate)) {
      await page.waitForTimeout(500)
      const stableDates = await visibleCalendarDates(page)
      if (stableDates.includes(config.targetDate)) {
        return
      }
      continue
    }
    if (!dates.length) {
      throw new Error('排程日历未渲染任何日期格。')
    }
    const previousDatesKey = dates.join(',')
    const directionButtonIndex = config.targetDate > dates[dates.length - 1] ? 2 : 0
    const monthResponsePromise = page
      .waitForResponse(
        (response) =>
          response.url().includes('/admin-api/mes/pro/schedule-calendar/month') &&
          response.status() === 200,
        { timeout: 60000 }
      )
      .catch(() => null)
    await page.locator('.month-switch button').nth(directionButtonIndex).click()
    await monthResponsePromise
    await page.waitForFunction(
      (previous) =>
        Array.from(document.querySelectorAll('.calendar-cell'))
          .map((item) => item.getAttribute('data-date') || '')
          .filter(Boolean)
          .sort()
          .join(',') !== previous,
      previousDatesKey,
      { timeout: 60000 }
    )
  }
  throw new Error(`无法在 36 次月份切换内定位目标日期月份: ${config.targetDate}`)
}

async function visibleCalendarDates(page) {
  return await page.locator('.calendar-cell').evaluateAll((items) =>
    items
      .map((item) => item.getAttribute('data-date') || '')
      .filter(Boolean)
      .sort()
  )
}

async function waitCalendarSettled(page) {
  await page.waitForFunction(
    () => {
      const title = document.querySelector('.toolbar-title-group h2')?.textContent?.trim() || ''
      const match = title.match(/(\d{4})年(\d{1,2})月/)
      if (!match) {
        return false
      }
      const prefix = `${match[1]}-${String(Number(match[2])).padStart(2, '0')}`
      return Array.from(document.querySelectorAll('.calendar-cell')).some((item) => {
        const date = item.getAttribute('data-date') || ''
        return date.startsWith(prefix)
      })
    },
    null,
    { timeout: 60000 }
  )
}

async function lockFirstVisibleTask(page) {
  const card = page.locator('.task-card').filter({ hasText: config.workOrderCode }).first()
  try {
    await card.waitFor({ state: 'visible', timeout: 30000 })
  } catch (error) {
    const cards = await page
      .locator('.task-card')
      .evaluateAll((items) =>
        items.slice(0, 10).map((item) => item.textContent?.replace(/\s+/g, ' ').trim())
      )
      .catch(() => [])
    throw new Error(
      `未找到工单 ${config.workOrderCode} 的任务卡，当前任务卡=${JSON.stringify(cards)}`
    )
  }
  const metaText = await card.locator('.task-card-meta').innerText()
  const taskId = await card.locator('button:has-text("锁定")').evaluate((button) => {
    const article = button.closest('.task-card')
    return article ? article.textContent : ''
  })
  assert.ok(taskId.includes(config.workOrderCode), '锁定按钮必须属于目标工单任务卡片。')

  const lockResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/task/lock') && response.status() === 200,
    { timeout: 60000 }
  )
  await card.locator('button:has-text("锁定")').click()
  const prompt = page.locator('.el-message-box:visible').last()
  await prompt.locator('input').fill('E2E_FREEZE_REPLAN_LOCK')
  await prompt.getByRole('button', { name: '确定' }).click()
  const response = await lockResponsePromise
  const body = await response.json()
  assert.equal(body.code, 0, `锁定任务接口业务错误: ${body.msg || body.code}`)

  await page.getByText('任务已锁定').waitFor({ state: 'visible', timeout: 30000 })
  await card.getByText('已锁定').waitFor({ state: 'visible', timeout: 30000 })
  const taskIdFromUrl = response.request().postDataJSON()?.taskId
  assert.ok(taskIdFromUrl, '锁定请求必须包含 taskId。')
  return { taskId: taskIdFromUrl, metaText }
}

async function previewReplan(page, expectedTaskId) {
  await page.waitForFunction(
    () => {
      const text = document.body.textContent || ''
      return /范围\s+[1-9]\d*\s+个排产工单/.test(text)
    },
    null,
    { timeout: 60000 }
  )
  const previewResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/mes/pro/auto-schedule/preview') &&
      response.status() === 200,
    { timeout: 60000 }
  )
  await page.getByRole('button', { name: '生成预览' }).click()
  const response = await previewResponsePromise
  const body = await response.json()
  const previewRequestBody = response.request().postData() || ''
  assert.equal(
    body.code,
    0,
    `自动排产预览接口业务错误: ${JSON.stringify(body)} request=${previewRequestBody}`
  )
  const preview = body.data
  assert.ok(preview.summary.preservedTaskCount >= 1, '重排预览必须保留至少一个锁定任务。')
  const preserved = (preview.tasks || []).some(
    (task) =>
      String(task.id).includes(String(expectedTaskId)) || Number(task.id) === Number(expectedTaskId)
  )
  assert.ok(preserved, `重排预览任务列表必须包含被锁定任务 ${expectedTaskId}。`)
  await page.getByText('保留任务').waitFor({ state: 'visible', timeout: 30000 })
  return preview
}

async function main() {
  assert.notEqual(config.tenant, '芋道源码', '真实 E2E 写入/调试不能使用芋道源码租户')
  const { chromium } = loadPlaywright()
  const browser = await chromium.launch({ headless: !config.headed })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  let lockedTaskId = null
  let beforeTask = null
  try {
    await login(page)
    await openCalendarDate(page)
    const locked = await lockFirstVisibleTask(page)
    lockedTaskId = locked.taskId
    beforeTask = await apiGetTask(page, lockedTaskId)
    await previewReplan(page, lockedTaskId)
    const afterTask = await apiGetTask(page, lockedTaskId)
    assert.equal(
      afterTask.startTime,
      beforeTask.startTime,
      '锁定任务预览重排后计划开始时间不能移动。'
    )
    assert.equal(afterTask.endTime, beforeTask.endTime, '锁定任务预览重排后计划完成时间不能移动。')
    console.log(
      JSON.stringify(
        {
          status: 'PASS',
          tenant: config.tenant,
          workOrderCode: config.workOrderCode,
          targetDate: config.targetDate,
          lockedTaskId,
          startTime: beforeTask.startTime,
          endTime: beforeTask.endTime
        },
        null,
        2
      )
    )
  } finally {
    if (lockedTaskId) {
      await apiUnlockTask(page, lockedTaskId).catch((error) => {
        console.error(`恢复解锁任务失败: ${error.message}`)
      })
    }
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
