const assert = require('node:assert/strict')
const { mkdirSync, writeFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { chromium } = require('playwright')

const baseUrl = process.env.MES_PRO_TASK_E2E_BASE_URL || 'http://localhost:8081'
const tenant = process.env.MES_PRO_TASK_E2E_TENANT
const username = process.env.MES_PRO_TASK_E2E_USERNAME
const password = process.env.MES_PRO_TASK_E2E_PASSWORD
const outputDir = resolve(__dirname, '../../output/playwright/20260720-production-gantt-controls')
const GANTT_DATE_INTERVAL_STORAGE_KEY = 'mes.pro.task.gantt.dateIntervalDays'

for (const [name, value] of Object.entries({ tenant, username, password })) {
  assert.ok(value, `missing required env MES_PRO_TASK_E2E_${name.toUpperCase()}`)
}

async function login(page) {
  const loginUrl = new URL('/login', baseUrl)
  loginUrl.searchParams.set('redirect', '/index')
  await page.goto(loginUrl.toString(), { waitUntil: 'commit' })

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible' })

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if (await tenantInput.count()) {
    await tenantInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await tenantOption.waitFor({ state: 'visible' })
    await tenantOption.click()
  } else {
    await form.locator('input.el-input__inner').nth(0).fill(tenant)
  }

  await form.locator('input.el-input__inner:not([role="combobox"]):visible').first().fill(username)
  await form.locator('input[type="password"]').first().fill(password)
  const loginButton = form.getByRole('button', { name: '登录' }).first()
  await loginButton.waitFor({ state: 'visible' })
  const loginButtonHandle = await loginButton.elementHandle()
  assert.ok(loginButtonHandle, 'login button element must exist')
  await page.waitForFunction(
    (element) => !element.disabled && !element.classList.contains('is-disabled'),
    loginButtonHandle
  )

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/system/auth/login') && response.request().method() === 'POST'
  )
  await loginButton.click()
  const loginResponse = await loginResponsePromise
  const loginPayload = await loginResponse.json()
  assert.ok(loginResponse.ok(), `login HTTP failed: ${loginResponse.status()}`)
  assert.ok([0, 200].includes(loginPayload.code), `login business code failed: ${loginPayload.code}`)
  const accessToken = loginPayload.data?.accessToken
  assert.ok(accessToken, 'login response must include accessToken for readonly API verification')
  await page.waitForURL((current) => !current.pathname.includes('/login'), { waitUntil: 'commit' })
  const loginResponseUrl = new URL(loginResponse.url())
  const apiBasePath = loginResponseUrl.pathname.replace(/\/system\/auth\/login$/, '')
  return {
    accessToken,
    apiBaseUrl: `${loginResponseUrl.origin}${apiBasePath}`
  }
}

function isReadOnlyMesRequest(request) {
  const method = request.method.toUpperCase()
  if (method === 'GET') {
    return true
  }

  const { pathname } = new URL(request.url)
  return method === 'POST' && pathname.endsWith('/mes/pro/auto-schedule/dependencies')
}

function isWhiteColor(value) {
  return /^rgba?\(255,\s*255,\s*255(?:,\s*1)?\)$/.test(value)
}

async function waitForReadableTooltip(page, hoverPoint) {
  const maxDelta = Math.max(0, Math.min(8, hoverPoint.width / 2 - 3))
  const xCandidates = [
    hoverPoint.x,
    hoverPoint.x - maxDelta,
    hoverPoint.x + maxDelta,
    hoverPoint.x - maxDelta / 2,
    hoverPoint.x + maxDelta / 2
  ]

  await page.mouse.move(0, 0)
  for (const x of xCandidates) {
    await page.mouse.move(x, hoverPoint.y, { steps: 12 })
    for (let i = 0; i < 8; i += 1) {
      const visible = await page.evaluate(() => {
        const tooltip = document.querySelector('.gantt_tooltip .gantt-readable-tooltip')
        if (!tooltip) {
          return false
        }
        const rect = tooltip.getBoundingClientRect()
        const style = window.getComputedStyle(tooltip)
        return (
          rect.width > 0 &&
          rect.height > 0 &&
          style.display !== 'none' &&
          style.visibility !== 'hidden'
        )
      })
      if (visible) {
        return
      }
      await page.waitForTimeout(150)
    }
  }

  const tooltipDiagnostics = await page.evaluate(() => {
    const tooltips = Array.from(document.querySelectorAll('.gantt_tooltip')).map((tooltip) => {
      const rect = tooltip.getBoundingClientRect()
      const style = window.getComputedStyle(tooltip)
      return {
        text: tooltip.textContent?.trim() || '',
        display: style.display,
        visibility: style.visibility,
        width: rect.width,
        height: rect.height
      }
    })
    const target = document.elementFromPoint(window.__lastGanttHoverPoint?.x || 0, window.__lastGanttHoverPoint?.y || 0)
    return {
      tooltipCount: tooltips.length,
      tooltips,
      elementFromPoint: target
        ? {
            tagName: target.tagName,
            className: String(target.className || ''),
            text: target.textContent?.trim() || ''
          }
        : null
    }
  })
  assert.fail(`gantt readable tooltip must appear after hovering a real task bar: ${JSON.stringify(tooltipDiagnostics)}`)
}

async function readWebStorageCacheValue(page, key) {
  return await page.evaluate((cacheKey) => {
    const raw = window.localStorage.getItem(cacheKey)
    if (!raw) {
      return null
    }
    try {
      const wrapper = JSON.parse(raw)
      if (wrapper && Object.prototype.hasOwnProperty.call(wrapper, 'v')) {
        return JSON.parse(wrapper.v)
      }
      return wrapper
    } catch (error) {
      return raw
    }
  }, key)
}

async function fetchCurrentScheduleOrderRows(page, apiBaseUrl, accessToken) {
  const tenantId = await readWebStorageCacheValue(page, 'tenantId')
  assert.ok(tenantId, 'tenantId cache must exist after login for schedule-order pool verification')

  return await page.evaluate(
    async ({ apiBaseUrl, accessToken, tenantId }) => {
      const rows = []
      let total = 0
      let pageNo = 1
      const pageSize = 200

      do {
        const url = new URL(`${apiBaseUrl}/mes/pro/schedule-order/page`)
        url.searchParams.set('pageNo', String(pageNo))
        url.searchParams.set('pageSize', String(pageSize))
        url.searchParams.set('completionFilter', 'INCOMPLETE')
        const response = await fetch(url.toString(), {
          method: 'GET',
          headers: {
            Accept: 'application/json',
            Authorization: `Bearer ${accessToken}`,
            'tenant-id': String(tenantId),
            'Cache-Control': 'no-cache',
            Pragma: 'no-cache'
          }
        })
        const payload = await response.json()
        if (!response.ok || ![0, 200].includes(payload.code)) {
          throw new Error(`schedule-order page API failed: HTTP ${response.status} ${JSON.stringify(payload)}`)
        }
        const data = payload.data || {}
        const list = Array.isArray(data.list) ? data.list : []
        rows.push(...list)
        total = Number(data.total || rows.length)
        pageNo += 1
      } while (rows.length < total)

      return { rows, total }
    },
    { apiBaseUrl, accessToken, tenantId }
  )
}

async function collapseOpenProjectRows(page) {
  const collapseButton = page.getByRole('button', { name: '全部折叠' })
  await collapseButton.waitFor({ state: 'visible' })
  await collapseButton.click()
  await page.waitForTimeout(500)
  await page.waitForFunction(() =>
    Array.from(document.querySelectorAll('.gantt_tree_icon.gantt_close')).every((element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width === 0 || rect.height === 0 || style.display === 'none' || style.visibility === 'hidden'
    })
  )
}

async function expandAllProjectRows(page) {
  const expandButton = page.getByRole('button', { name: '全部展开' })
  await expandButton.waitFor({ state: 'visible' })
  await expandButton.click()
  await page.waitForTimeout(500)
  await page.waitForFunction(() =>
    Array.from(document.querySelectorAll('.gantt_tree_icon.gantt_close')).some((element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    })
  )
}

async function collectVisibleOrderBarColors(page) {
  const projectRows = await page.evaluate(() => {
    const gantt = window.gantt
    const rows = []
    gantt.eachTask((task) => {
      if (!task.unscheduled && task.type === gantt.config.types.project && String(task.workOrderCode || '').trim()) {
        rows.push({
          id: task.id,
          workOrderCode: String(task.workOrderCode).trim()
        })
      }
    })
    return rows.slice(0, 10)
  })

  const sampledProjectBars = []
  for (const project of projectRows) {
    await page.evaluate((id) => window.gantt.showTask(id), project.id)
    await page.waitForTimeout(240)
    const item = await page.evaluate((projectRow) => {
      const gantt = window.gantt
      const element = gantt.getTaskNode(projectRow.id)
      if (!element || !element.isConnected) {
        return null
      }
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      const content = element.querySelector('.gantt_task_content')
      if (rect.width <= 0 || rect.height <= 0 || style.display === 'none' || style.visibility === 'hidden') {
        return null
      }
      return {
        text: element.textContent?.trim() || projectRow.workOrderCode,
        orderCode: projectRow.workOrderCode,
        className: String(element.className || ''),
        backgroundColor: style.backgroundColor,
        borderColor: style.borderColor,
        contentColor: content ? window.getComputedStyle(content).color : null
      }
    }, project)
    if (item) {
      sampledProjectBars.push(item)
    }
    if (sampledProjectBars.length >= 6) {
      break
    }
  }

  const visibleBarMetrics = await page.evaluate(() => {
    const rectOf = (element) => {
      const rect = element.getBoundingClientRect()
      return {
        width: Math.round(rect.width * 10) / 10,
        height: Math.round(rect.height * 10) / 10,
        left: Math.round(rect.left * 10) / 10,
        right: Math.round(rect.right * 10) / 10,
        top: Math.round(rect.top * 10) / 10,
        bottom: Math.round(rect.bottom * 10) / 10
      }
    }
    const taskArea = document.querySelector('.gantt_task')
    const taskAreaRect = taskArea ? rectOf(taskArea) : null
    const isRenderable = (element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const isVisibleInTaskArea = (element) => {
      if (!taskAreaRect || !isRenderable(element)) {
        return false
      }
      const rect = rectOf(element)
      return (
        rect.right > taskAreaRect.left &&
        rect.left < taskAreaRect.right &&
        rect.bottom > taskAreaRect.top &&
        rect.top < taskAreaRect.bottom
      )
    }

    const bars = Array.from(document.querySelectorAll('.gantt_task_line'))
      .filter(isVisibleInTaskArea)
      .map((element) => {
        const style = window.getComputedStyle(element)
        const content = element.querySelector('.gantt_task_content')
        const text = element.textContent?.trim() || ''
        return {
          text,
          orderCode: text.split('/')[0].trim(),
          className: String(element.className || ''),
          backgroundColor: style.backgroundColor,
          borderColor: style.borderColor,
          contentColor: content ? window.getComputedStyle(content).color : null
        }
      })
      .filter((item) => item.orderCode)
    return {
      bars: bars.slice(0, 20),
      nonWhiteContentBars: bars
        .filter((item) => item.contentColor && !/^rgba?\(255,\s*255,\s*255(?:,\s*1)?\)$/.test(item.contentColor))
        .slice(0, 5)
    }
  })

  const orderColors = new Map()
  for (const item of sampledProjectBars) {
    if (!orderColors.has(item.orderCode)) {
      orderColors.set(item.orderCode, item.backgroundColor)
    }
  }

  return {
    projectRowCount: projectRows.length,
    visibleOrderCodeCount: orderColors.size,
    distinctOrderColorCount: new Set(orderColors.values()).size,
    projectBars: sampledProjectBars,
    bars: visibleBarMetrics.bars,
    nonWhiteContentBars: visibleBarMetrics.nonWhiteContentBars
  }
}

async function collectDateIntervalMetrics(page) {
  return await page.evaluate(() => {
    const scaleLines = Array.from(document.querySelectorAll('.gantt_task_scale .gantt_scale_line'))
    const scaleLineMetrics = scaleLines
      .map((line) => {
        const rect = line.getBoundingClientRect()
        const cells = Array.from(line.querySelectorAll('.gantt_scale_cell')).map((element) => {
          const cellRect = element.getBoundingClientRect()
          return {
            text: element.textContent?.trim() || '',
            left: cellRect.left,
            width: cellRect.width
          }
        })
        return {
          top: rect.top,
          bottom: rect.bottom,
          cells,
          texts: cells.map((cell) => cell.text).filter(Boolean)
        }
      })
      .sort((a, b) => b.top - a.top)
    const bottomScaleLine = scaleLineMetrics[0]
    const bottomScaleTexts = bottomScaleLine?.texts || []
    const taskArea = document.querySelector('.gantt_task')
    const gantt = window.gantt
    const bottomScaleCellDates =
      bottomScaleLine && taskArea && gantt?.dateFromPos && gantt?.getScrollState
        ? bottomScaleLine.cells
            .filter((cell) => cell.width > 0)
            .map((cell) => {
              const taskAreaRect = taskArea.getBoundingClientRect()
              const scrollX = Number(gantt.getScrollState()?.x || 0)
              const date = gantt.dateFromPos(cell.left - taskAreaRect.left + scrollX)
              return date instanceof Date && !Number.isNaN(date.getTime()) ? date.toISOString() : null
            })
            .filter(Boolean)
        : []
    const bottomScaleCellDaySteps = bottomScaleCellDates.slice(1).map((dateText, index) => {
      const current = new Date(dateText).getTime()
      const previous = new Date(bottomScaleCellDates[index]).getTime()
      return Math.round((current - previous) / 86400000)
    })
    return {
      label: document.querySelector('.production-gantt-interval-value')?.textContent?.trim() || '',
      scaleText: document.querySelector('.gantt_task_scale')?.textContent || '',
      bottomScaleTexts: bottomScaleTexts.slice(0, 12),
      bottomScaleCellDaySteps: bottomScaleCellDaySteps.slice(0, 12),
      scaleLineTexts: scaleLineMetrics.map((line) => line.texts.slice(0, 8)),
      taskLineCount: Array.from(document.querySelectorAll('.gantt_task_line')).filter((element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
      }).length
    }
  })
}

async function setDateIntervalSlider(page, expectedDays) {
  const slider = page.locator('.production-gantt-interval-control .el-slider__runway').first()
  await slider.waitFor({ state: 'visible' })
  const box = await slider.boundingBox()
  assert.ok(box, 'date interval slider runway must expose a bounding box')
  const ratio = (expectedDays - 1) / 14
  const xOffset = Math.max(1, Math.min(box.width - 1, box.width * ratio))
  await page.mouse.click(box.x + xOffset, box.y + box.height / 2)
  await page.waitForFunction(
    (days) => document.querySelector('.production-gantt-interval-value')?.textContent?.includes(`${days} 天/格`),
    expectedDays
  )
  await page.waitForTimeout(500)
}

async function readStoredDateIntervalDays(page) {
  return await page.evaluate((key) => window.localStorage.getItem(key), GANTT_DATE_INTERVAL_STORAGE_KEY)
}

async function waitForVisibleGanttBars(page) {
  await page.locator('.gantt_container').first().waitFor({ state: 'visible' })
  await page.waitForFunction(() => {
    const taskArea = document.querySelector('.gantt_task')
    if (!taskArea) {
      return false
    }
    const taskAreaRect = taskArea.getBoundingClientRect()
    return Array.from(document.querySelectorAll('.gantt_task_line')).some((element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return (
        rect.width > 0 &&
        rect.height > 0 &&
        rect.right > taskAreaRect.left &&
        rect.left < taskAreaRect.right &&
        rect.bottom > taskAreaRect.top &&
        rect.top < taskAreaRect.bottom &&
        style.display !== 'none' &&
        style.visibility !== 'hidden'
      )
    })
  })
}

async function verifyGanttFullscreen(page) {
  const maximizeButton = page.getByRole('button', { name: '最大化' }).first()
  await maximizeButton.waitFor({ state: 'visible' })
  await maximizeButton.click()
  await page.waitForFunction(() =>
    document.fullscreenElement?.classList.contains('production-gantt-fullscreen-host')
  )
  await page.getByRole('button', { name: '恢复' }).first().waitFor({ state: 'visible' })
  const fullscreenMetrics = await page.evaluate(() => {
    const host = document.fullscreenElement
    const rail = document.querySelector('.production-gantt-fullscreen-rail')
    const gantt = document.querySelector('.gantt_container')
    const hostRect = host?.getBoundingClientRect()
    const ganttRect = gantt?.getBoundingClientRect()
    return {
      isFullscreen: Boolean(host),
      hostClassName: host ? String(host.className || '') : '',
      hostRect: hostRect
        ? {
            width: Math.round(hostRect.width),
            height: Math.round(hostRect.height)
          }
        : null,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight
      },
      railText: rail?.textContent?.replace(/\s+/g, ' ').trim() || '',
      ganttRect: ganttRect
        ? {
            width: Math.round(ganttRect.width),
            height: Math.round(ganttRect.height)
          }
        : null
    }
  })
  assert.equal(fullscreenMetrics.isFullscreen, true, 'gantt host must enter element fullscreen')
  assert.match(fullscreenMetrics.railText, /恢复/, 'fullscreen control rail must expose restore')
  assert.match(fullscreenMetrics.railText, /订单/, 'fullscreen control rail must keep order hint')
  assert.match(fullscreenMetrics.railText, /工序/, 'fullscreen control rail must keep process hint')
  assert.ok(
    fullscreenMetrics.hostRect &&
      fullscreenMetrics.hostRect.width >= fullscreenMetrics.viewport.width - 2 &&
      fullscreenMetrics.hostRect.height >= fullscreenMetrics.viewport.height - 2,
    `fullscreen host must fill the viewport, metrics=${JSON.stringify(fullscreenMetrics)}`
  )
  assert.ok(
    fullscreenMetrics.ganttRect?.width > 1000 && fullscreenMetrics.ganttRect?.height > 700,
    `fullscreen gantt must redraw to the larger viewport, metrics=${JSON.stringify(fullscreenMetrics)}`
  )

  await page.locator('.production-gantt-fullscreen-rail').getByRole('button', { name: '恢复' }).click()
  await page.waitForFunction(() => !document.fullscreenElement)
  await page.getByRole('button', { name: '最大化' }).first().waitFor({ state: 'visible' })
  const restoredMetrics = await page.evaluate(() => ({
    isFullscreen: Boolean(document.fullscreenElement),
    railVisible: Boolean(document.querySelector('.production-gantt-fullscreen-rail')),
    maximizeVisible: Array.from(document.querySelectorAll('button')).some((button) =>
      (button.textContent || '').includes('最大化')
    )
  }))
  assert.deepEqual(
    restoredMetrics,
    { isFullscreen: false, railVisible: false, maximizeVisible: true },
    `restore must return to normal page state, metrics=${JSON.stringify(restoredMetrics)}`
  )

  return { fullscreenMetrics, restoredMetrics }
}

async function scrollGanttToBottomIfScrollable(page) {
  const before = await page.evaluate(() => {
    const gantt = window.gantt
    const state = gantt?.getScrollState ? gantt.getScrollState() : null
    const vscroll = document.querySelector('.gantt_ver_scroll, .gantt_vscroll, .gantt_task_vscroll')
    const domMetrics = vscroll
      ? {
          scrollTop: vscroll.scrollTop,
          scrollHeight: vscroll.scrollHeight,
          clientHeight: vscroll.clientHeight
        }
      : null
    return { state, domMetrics }
  })
  assert.ok(before.state, 'gantt scroll state must be available for refresh/collapse verification')

  const domCanScroll =
    before.domMetrics && before.domMetrics.scrollHeight > before.domMetrics.clientHeight + 2
  const stateCanScroll =
    Number.isFinite(Number(before.state.height)) &&
    Number.isFinite(Number(before.state.inner_height)) &&
    Number(before.state.height) > Number(before.state.inner_height) + 2

  if (!domCanScroll && !stateCanScroll) {
    return {
      scrolled: false,
      reason: 'no vertical scroll after scheduled-only filtering',
      before
    }
  }

  const box = await page.locator('.gantt_container').first().boundingBox()
  assert.ok(box, 'gantt container must expose a scrollable viewport')
  await page.mouse.move(box.x + box.width - 24, box.y + box.height / 2)
  await page.mouse.wheel(0, 24000)
  await page.waitForTimeout(900)

  const after = await page.evaluate(() => {
    const gantt = window.gantt
    const state = gantt?.getScrollState ? gantt.getScrollState() : null
    const vscroll = document.querySelector('.gantt_ver_scroll, .gantt_vscroll, .gantt_task_vscroll')
    const domMetrics = vscroll
      ? {
          scrollTop: vscroll.scrollTop,
          scrollHeight: vscroll.scrollHeight,
          clientHeight: vscroll.clientHeight
        }
      : null
    return { state, domMetrics }
  })
  const movedByState = after.state.y > before.state.y
  const movedByDom =
    after.domMetrics &&
    before.domMetrics &&
    after.domMetrics.scrollTop > before.domMetrics.scrollTop
  assert.ok(
    movedByState || movedByDom,
    `gantt must scroll vertically before bottom collapse when vertical rows overflow, before=${JSON.stringify(before)}, after=${JSON.stringify(after)}`
  )
  return {
    scrolled: true,
    before,
    after
  }
}

async function collectCollapsedProjectMetrics(page) {
  return await page.evaluate(() => {
    const rectOf = (element) => {
      const rect = element.getBoundingClientRect()
      return {
        width: Math.round(rect.width * 10) / 10,
        height: Math.round(rect.height * 10) / 10,
        left: Math.round(rect.left * 10) / 10,
        right: Math.round(rect.right * 10) / 10,
        top: Math.round(rect.top * 10) / 10,
        bottom: Math.round(rect.bottom * 10) / 10
      }
    }
    const isRenderable = (element) => {
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
    }
    const taskArea = document.querySelector('.gantt_task')
    const taskAreaRect = taskArea ? rectOf(taskArea) : null
    const isVisibleInTaskArea = (element) => {
      if (!taskAreaRect || !isRenderable(element)) {
        return false
      }
      const rect = rectOf(element)
      return (
        rect.right > taskAreaRect.left &&
        rect.left < taskAreaRect.right &&
        rect.bottom > taskAreaRect.top &&
        rect.top < taskAreaRect.bottom
      )
    }
    const projectBars = Array.from(document.querySelectorAll('.gantt_task_line.gantt-project-bar'))
      .filter(isVisibleInTaskArea)
      .map((element) => element.textContent?.trim() || '')
      .filter(Boolean)
    const collapsedProjectOverflowItems = Array.from(
      document.querySelectorAll('.gantt-collapsed-project-overflow')
    )
      .filter(isVisibleInTaskArea)
      .map((element) => ({
        text: element.textContent?.trim() || '',
        rect: rectOf(element)
      }))
      .filter((item) => item.text)
    const gridRows = Array.from(document.querySelectorAll('.gantt_grid_data .gantt_row'))
      .filter(isRenderable)
      .map((row) => row.textContent?.trim() || '')
      .filter(Boolean)

    return {
      projectBarCount: projectBars.length,
      projectBarTexts: projectBars.slice(0, 10),
      collapsedProjectOverflowCount: collapsedProjectOverflowItems.length,
      collapsedProjectOverflowTexts: collapsedProjectOverflowItems.map((item) => item.text).slice(0, 10),
      gridRowCount: gridRows.length,
      gridRows: gridRows.slice(0, 10)
    }
  })
}

async function main() {
  mkdirSync(outputDir, { recursive: true })

  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-dev-shm-usage']
  })

  try {
    const context = await browser.newContext({ viewport: { width: 1600, height: 950 } })
    const page = await context.newPage()
    const mesRequests = []
    const pageErrors = []

    page.on('request', (request) => {
      const url = request.url()
      if (url.includes('/mes/')) {
        mesRequests.push({ method: request.method(), url })
      }
    })
    page.on('pageerror', (error) => {
      pageErrors.push(error.message)
    })

    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    const loginSession = await login(page)
    await page.evaluate((key) => window.localStorage.removeItem(key), GANTT_DATE_INTERVAL_STORAGE_KEY)
    const currentScheduleOrderPage = await fetchCurrentScheduleOrderRows(
      page,
      loginSession.apiBaseUrl,
      loginSession.accessToken
    )
    const scheduleOrderRows = currentScheduleOrderPage.rows
    assert.ok(scheduleOrderRows.length > 0, 'schedule-order page must return current incomplete schedule orders')
    const scheduleOrderWorkOrderCodes = new Set(
      scheduleOrderRows.map((item) => String(item.erpWorkOrderCode ?? '').trim()).filter(Boolean)
    )
    assert.ok(
      scheduleOrderWorkOrderCodes.size > 0,
      'current schedule-order pool must expose source production work-order codes'
    )

    const ganttListResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/task/gantt-list') && response.request().method() === 'GET'
    )
    await page.goto(new URL('/mes/pro/task', baseUrl).toString(), { waitUntil: 'commit' })
    const ganttListResponse = await ganttListResponsePromise
    const ganttListPayload = await ganttListResponse.json()
    const ganttRows = Array.isArray(ganttListPayload.data) ? ganttListPayload.data : []
    assert.ok(ganttRows.length > 0, 'gantt-list must return real schedule rows')
    const rowsMissingWorkOrderCode = ganttRows
      .filter((item) => !String(item.workOrderCode ?? '').trim())
      .slice(0, 5)
    assert.deepEqual(
      rowsMissingWorkOrderCode,
      [],
      `gantt-list rows must include workOrderCode before rendering, samples=${JSON.stringify(rowsMissingWorkOrderCode)}`
    )
    const projectRows = ganttRows.filter((item) => String(item.id ?? '').startsWith('301_'))
    const projectRowsOutsideScheduleOrderPool = projectRows
      .filter((item) => !scheduleOrderWorkOrderCodes.has(String(item.workOrderCode ?? '').trim()))
      .map((item) => ({ id: item.id, workOrderCode: item.workOrderCode }))
      .slice(0, 5)
    assert.deepEqual(
      projectRowsOutsideScheduleOrderPool,
      [],
      `gantt-list project rows must belong to current schedule-order pool, samples=${JSON.stringify(projectRowsOutsideScheduleOrderPool)}`
    )
    const rowsMissingProcess = ganttRows
      .filter((item) => String(item.id ?? '').startsWith('303_'))
      .filter((item) => !String(item.process ?? '').trim())
      .slice(0, 5)
    assert.deepEqual(
      rowsMissingProcess,
      [],
      `gantt-list task rows must include process before rendering, samples=${JSON.stringify(rowsMissingProcess)}`
    )
    const taskRows = ganttRows.filter((item) => String(item.id ?? '').startsWith('303_'))
    const rowsMissingScheduleOrderProcessId = taskRows
      .filter((item) => !Number.isFinite(Number(item.scheduleOrderProcessId)) || Number(item.scheduleOrderProcessId) <= 0)
      .slice(0, 5)
    assert.deepEqual(
      rowsMissingScheduleOrderProcessId,
      [],
      `gantt-list task rows must all participate in scheduling, samples=${JSON.stringify(rowsMissingScheduleOrderProcessId)}`
    )
    const taskRowsOutsideScheduleOrderPool = taskRows
      .filter((item) => !scheduleOrderWorkOrderCodes.has(String(item.workOrderCode ?? '').trim()))
      .map((item) => ({ id: item.id, workOrderCode: item.workOrderCode, process: item.process }))
      .slice(0, 5)
    assert.deepEqual(
      taskRowsOutsideScheduleOrderPool,
      [],
      `gantt-list task rows must belong to current schedule-order pool, samples=${JSON.stringify(taskRowsOutsideScheduleOrderPool)}`
    )
    const taskParentIds = new Set(taskRows.map((item) => String(item.parent ?? '')))
    const projectRowsWithoutScheduledTask = ganttRows
      .filter((item) => String(item.id ?? '').startsWith('301_'))
      .filter((item) => !taskParentIds.has(String(item.id ?? '')))
      .slice(0, 5)
    assert.deepEqual(
      projectRowsWithoutScheduledTask,
      [],
      `gantt-list project rows must have at least one scheduled task child, samples=${JSON.stringify(projectRowsWithoutScheduledTask)}`
    )
    await page.getByText('当前排产甘特图', { exact: false }).first().waitFor({ state: 'visible' })
    await waitForVisibleGanttBars(page)

    const metrics = await page.evaluate(() => {
      const rectOf = (element) => {
        const rect = element.getBoundingClientRect()
        return {
          width: Math.round(rect.width * 10) / 10,
          height: Math.round(rect.height * 10) / 10,
          left: Math.round(rect.left * 10) / 10,
          right: Math.round(rect.right * 10) / 10,
          top: Math.round(rect.top * 10) / 10,
          bottom: Math.round(rect.bottom * 10) / 10
        }
      }
      const taskArea = document.querySelector('.gantt_task')
      const taskAreaRect = taskArea ? rectOf(taskArea) : null
      const isRenderable = (element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
      }
      const isVisibleInTaskArea = (element) => {
        if (!taskAreaRect || !isRenderable(element)) {
          return false
        }
        const rect = rectOf(element)
        return (
          rect.right > taskAreaRect.left &&
          rect.left < taskAreaRect.right &&
          rect.bottom > taskAreaRect.top &&
          rect.top < taskAreaRect.bottom
        )
      }

      const taskLines = Array.from(document.querySelectorAll('.gantt_task_line'))
        .filter(isVisibleInTaskArea)
        .map((element) => ({
          rect: rectOf(element),
          className: element.className,
          text: element.textContent?.trim() || ''
        }))
      const taskBars = taskLines.filter((item) => !item.className.includes('gantt-project-bar'))
      const widths = taskBars.map((item) => item.rect.width).filter((width) => width > 0)
      const firstLink = document.querySelector('.gantt_task_link')
      const scaleText = document.querySelector('.gantt_task_scale')?.textContent || ''
      const grid = document.querySelector('.gantt_grid')
      const gridScaleText = document.querySelector('.gantt_grid_scale')?.textContent || ''
      const sideLabelItems = Array.from(document.querySelectorAll('.gantt-task-side-label'))
        .filter(isVisibleInTaskArea)
        .map((element) => ({
          text: element.textContent?.trim() || '',
          rect: rectOf(element)
        }))
        .filter((item) => item.text)
      const clippedSideLabels = taskAreaRect
        ? sideLabelItems.filter(
            (item) => item.rect.left < taskAreaRect.left + 1 || item.rect.right > taskAreaRect.right - 1
          )
        : sideLabelItems

      return {
        taskLineCount: taskLines.length,
        taskBarCount: taskBars.length,
        shortTaskCount: taskLines.filter((item) => item.className.includes('gantt-short-task')).length,
        minTaskBarWidth: widths.length ? Math.min(...widths) : 0,
        maxTaskBarHeight: Math.max(...taskLines.map((item) => item.rect.height)),
        visibleTaskTextCount: taskBars.filter((item) => item.text.length > 0).length,
        projectBarCount: taskLines.filter((item) => item.className.includes('gantt-project-bar')).length,
        dependencyOpacity: firstLink ? Number(window.getComputedStyle(firstLink).opacity) : null,
        scaleText,
        hasHourScaleText: /08:00|16:00|00:00/.test(scaleText),
        gridWidth: grid ? rectOf(grid).width : 0,
        taskAreaWidth: taskArea ? rectOf(taskArea).width : 0,
        gridScaleText,
        taskBarTexts: taskBars.map((item) => item.text).filter(Boolean).slice(0, 10),
        sideLabels: sideLabelItems.map((item) => item.text).slice(0, 10),
        clippedSideLabelCount: clippedSideLabels.length,
        clippedSideLabelSamples: clippedSideLabels.map((item) => item.text).slice(0, 5),
        projectBarTexts: taskLines
          .filter((item) => item.className.includes('gantt-project-bar'))
          .map((item) => item.text)
          .filter(Boolean)
          .slice(0, 5)
      }
    })

    assert.ok(metrics.taskBarCount > 0, 'production gantt must render real task bars')
    assert.ok(metrics.projectBarCount > 0, 'production gantt must render work-order project bars')
    assert.ok(
      metrics.minTaskBarWidth >= 40,
      `task bars must remain readable, min width=${metrics.minTaskBarWidth}`
    )
    assert.ok(metrics.maxTaskBarHeight >= 24, `task bar height is too small: ${metrics.maxTaskBarHeight}`)
    const taskIdentityLabels = [...metrics.taskBarTexts, ...metrics.sideLabels]
    assert.ok(taskIdentityLabels.length > 0, 'task bars must keep scannable visible identity labels')
    assert.ok(
      metrics.gridWidth >= 390 && metrics.gridWidth <= 480,
      `left grid must be a compact single identity column, width=${metrics.gridWidth}`
    )
    assert.ok(metrics.taskAreaWidth >= 840, `timeline area must stay dominant, width=${metrics.taskAreaWidth}`)
    assert.match(metrics.gridScaleText, /生产工单编码\s*\/\s*工序/, 'left grid header must be work order/process only')
    assert.doesNotMatch(metrics.gridScaleText, /工作站|开始时间|结束时间/, 'left grid must not expose removed columns')
    assert.ok(
      taskIdentityLabels.every((text) => text.includes('/')),
      `task labels must use workOrderCode / process, samples=${taskIdentityLabels.join(' | ')}`
    )
    assert.ok(
      taskIdentityLabels.every((text) => !text.includes('%')),
      `task labels must not show progress percentages, samples=${taskIdentityLabels.join(' | ')}`
    )
    assert.ok(
      metrics.shortTaskCount === 0 || metrics.sideLabels.length > 0,
      'short task bars must expose workOrderCode / process in side labels instead of clipped bar text'
    )
    assert.equal(
      metrics.clippedSideLabelCount,
      0,
      `short task side labels must stay inside visible timeline, clipped=${metrics.clippedSideLabelSamples.join(' | ')}`
    )
    assert.ok(
      metrics.projectBarTexts.every((text) => text.length > 0 && !text.includes('%')),
      `project bar labels must show work order code only, samples=${metrics.projectBarTexts.join(' | ')}`
    )
    assert.equal(metrics.hasHourScaleText, false, 'readonly production gantt should use day scale')
    if (metrics.dependencyOpacity !== null) {
      assert.ok(
        metrics.dependencyOpacity <= 0.5,
        `dependency links must be visually de-emphasized, opacity=${metrics.dependencyOpacity}`
      )
    }

    await page.getByText('日期间隔', { exact: false }).first().waitFor({ state: 'visible' })
    await page.getByText('天/格', { exact: false }).first().waitFor({ state: 'visible' })
    const orderColorMetrics = await collectVisibleOrderBarColors(page)
    assert.ok(
      orderColorMetrics.projectRowCount > 0,
      `real gantt page must expose at least one scheduled work order, metrics=${JSON.stringify(orderColorMetrics)}`
    )
    assert.ok(
      orderColorMetrics.projectBars.every((item) => item.className.includes('gantt-order-color-')),
      `project bars must carry stable work-order color classes, metrics=${JSON.stringify(orderColorMetrics)}`
    )
    const colorsByOrder = new Map()
    for (const item of [...orderColorMetrics.projectBars, ...orderColorMetrics.bars]) {
      if (!colorsByOrder.has(item.orderCode)) {
        colorsByOrder.set(item.orderCode, new Set())
      }
      colorsByOrder.get(item.orderCode).add(item.backgroundColor)
    }
    const inconsistentOrderColors = Array.from(colorsByOrder.entries())
      .filter(([, colors]) => colors.size > 1)
      .map(([orderCode, colors]) => ({ orderCode, colors: Array.from(colors) }))
    assert.deepEqual(
      inconsistentOrderColors,
      [],
      `same work order project/task bars must keep the same color, metrics=${JSON.stringify(orderColorMetrics)}`
    )
    if (orderColorMetrics.visibleOrderCodeCount > 1) {
      assert.ok(
        orderColorMetrics.distinctOrderColorCount > 1,
        `different visible work orders must use different colors, metrics=${JSON.stringify(orderColorMetrics)}`
      )
    } else {
      orderColorMetrics.multiOrderColorPrecondition =
        'current real gantt data exposes one scheduled work order after latest replan scope; static contract covers multi-order color hashing'
    }
    assert.deepEqual(
      orderColorMetrics.nonWhiteContentBars,
      [],
      `gantt bar content must stay white, samples=${JSON.stringify(orderColorMetrics.nonWhiteContentBars)}`
    )
    const dateIntervalBefore = await collectDateIntervalMetrics(page)
    assert.match(dateIntervalBefore.label, /1\s*天\/格/, 'date interval slider must default to 1 day per cell')
    await setDateIntervalSlider(page, 15)
    const dateIntervalAfter = await collectDateIntervalMetrics(page)
    assert.match(dateIntervalAfter.label, /15\s*天\/格/, 'date interval slider must show the selected 15 day step')
    assert.equal(await readStoredDateIntervalDays(page), '15', 'date interval must be saved to browser localStorage')
    assert.ok(dateIntervalAfter.taskLineCount > 0, 'gantt task bars must remain visible after date interval changes')
    assert.ok(
      dateIntervalAfter.bottomScaleCellDaySteps.length > 0,
      `date interval E2E must capture bottom visible grid cell date steps, metrics=${JSON.stringify(dateIntervalAfter)}`
    )
    assert.deepEqual(
      dateIntervalAfter.bottomScaleCellDaySteps.filter((step) => step !== 15),
      [],
      `bottom visible grid cells must advance by 15 days after selecting 15 days per cell, metrics=${JSON.stringify(dateIntervalAfter)}`
    )
    assert.notDeepEqual(
      dateIntervalAfter.bottomScaleTexts,
      dateIntervalBefore.bottomScaleTexts,
      `date interval slider must redraw the timeline scale, before=${JSON.stringify(dateIntervalBefore)}, after=${JSON.stringify(dateIntervalAfter)}`
    )
    const reloadGanttListResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes('/mes/pro/task/gantt-list') && response.request().method() === 'GET'
    )
    await page.goto(new URL('/mes/pro/task', baseUrl).toString(), { waitUntil: 'commit' })
    await reloadGanttListResponsePromise
    await waitForVisibleGanttBars(page)
    const dateIntervalAfterReload = await collectDateIntervalMetrics(page)
    assert.match(
      dateIntervalAfterReload.label,
      /15\s*天\/格/,
      'date interval slider must restore the saved 15 day step after reopening the page'
    )
    const fullscreenState = await verifyGanttFullscreen(page)

    const hoverPoint = await page.evaluate(() => {
      const isRenderable = (element) => {
        const rect = element.getBoundingClientRect()
        const style = window.getComputedStyle(element)
        return rect.width > 0 && rect.height > 0 && style.display !== 'none' && style.visibility !== 'hidden'
      }
      const taskArea = document.querySelector('.gantt_task')
      if (!taskArea) {
        return null
      }
      const taskAreaRect = taskArea.getBoundingClientRect()
      const task = Array.from(document.querySelectorAll('.gantt_task_line:not(.gantt-project-bar)')).find(
        (element) => {
          if (!isRenderable(element)) {
            return false
          }
          const rect = element.getBoundingClientRect()
          const x = rect.left + Math.max(3, Math.min(rect.width / 2, rect.width - 3))
          const y = rect.top + rect.height / 2
          const hitStack = document.elementsFromPoint(x, y)
          return (
            rect.left >= taskAreaRect.left + 1 &&
            rect.right <= taskAreaRect.right - 1 &&
            rect.bottom > taskAreaRect.top &&
            rect.top < taskAreaRect.bottom &&
            hitStack.some((hit) => hit === element || element.contains(hit) || hit.closest?.('.gantt_task_line') === element)
          )
        }
      )
      if (!task) {
        return null
      }
      const rect = task.getBoundingClientRect()
      return {
        x: rect.left + Math.max(2, Math.min(rect.width / 2, rect.width - 2)),
        y: rect.top + rect.height / 2,
        width: rect.width
      }
    })
    assert.ok(hoverPoint, 'a visible production task bar is required for tooltip verification')
    await page.evaluate((point) => {
      window.__lastGanttHoverPoint = point
    }, hoverPoint)
    await waitForReadableTooltip(page, hoverPoint)
    const tooltipColors = await page.evaluate(() => {
      const colorOf = (selector) => {
        const element = document.querySelector(selector)
        return element ? window.getComputedStyle(element).color : null
      }

      return {
        body: colorOf('.gantt_tooltip .gantt-readable-tooltip'),
        title: colorOf('.gantt_tooltip .gantt-readable-tooltip__title'),
        label: colorOf('.gantt_tooltip .gantt-readable-tooltip__row span'),
        value: colorOf('.gantt_tooltip .gantt-readable-tooltip__row strong'),
        text: document.querySelector('.gantt_tooltip .gantt-readable-tooltip')?.textContent || ''
      }
    })
    const { text: tooltipText, ...tooltipColorValues } = tooltipColors
    for (const [name, color] of Object.entries(tooltipColorValues)) {
      assert.ok(isWhiteColor(color), `tooltip ${name} text must be white, actual=${color}`)
    }
    assert.match(tooltipText, /工单编码/, 'tooltip must show work order code')
    assert.match(tooltipText, /工序/, 'tooltip must show process')
    assert.doesNotMatch(tooltipText, /工作站|开始|结束|时长|完成|%/, `tooltip must not show extra scheduling fields: ${tooltipText}`)

    await collapseOpenProjectRows(page)
    await page.waitForTimeout(800)
    const collapsedMetrics = await collectCollapsedProjectMetrics(page)
    assert.ok(collapsedMetrics.gridRowCount > 0, 'collapsed gantt must keep work-order rows visible')
    assert.ok(
      collapsedMetrics.projectBarCount + collapsedMetrics.collapsedProjectOverflowCount > 0,
      `collapsed gantt must keep work-order summaries visible, metrics=${JSON.stringify(collapsedMetrics)}`
    )
    assert.ok(
      collapsedMetrics.collapsedProjectOverflowTexts.every((text) => !text.includes('/') && !text.includes('%')),
      `collapsed overflow summaries must show work order code only, samples=${collapsedMetrics.collapsedProjectOverflowTexts.join(' | ')}`
    )
    await expandAllProjectRows(page)
    const expandedMetrics = await collectCollapsedProjectMetrics(page)
    assert.ok(expandedMetrics.gridRowCount >= collapsedMetrics.gridRowCount, 'expand-all must keep work-order rows visible')
    assert.ok(
      (await page.locator('.gantt_tree_icon.gantt_close:visible').count()) > 0,
      'expand-all must restore open work-order tree controls'
    )
    await collapseOpenProjectRows(page)
    const bottomScrollResult = await scrollGanttToBottomIfScrollable(page)
    await page.waitForTimeout(800)
    const scrolledCollapsedMetrics = bottomScrollResult.scrolled
      ? await collectCollapsedProjectMetrics(page)
      : null
    if (bottomScrollResult.scrolled) {
      assert.ok(scrolledCollapsedMetrics.gridRowCount > 0, 'collapsed gantt must keep rows visible after scrolling lower virtual rows')
      assert.ok(
        scrolledCollapsedMetrics.projectBarCount + scrolledCollapsedMetrics.collapsedProjectOverflowCount > 0,
        `collapsed gantt must keep summaries visible after scrolling, metrics=${JSON.stringify(scrolledCollapsedMetrics)}`
      )
    }

    const writeRequests = mesRequests.filter((request) => !isReadOnlyMesRequest(request))
    assert.deepEqual(writeRequests, [], 'readability verification must not write MES data')
    assert.deepEqual(pageErrors, [], `gantt page must not throw while refreshing, scrolling and collapsing, errors=${pageErrors.join(' | ')}`)

    const screenshotPath = resolve(outputDir, 'gantt-readability.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })

    const evidence = {
      baseUrl,
      tenant,
      username,
      targetPath: '/mes/pro/task',
      scheduleOrderTotal: currentScheduleOrderPage.total,
      scheduleOrderWorkOrderCodeCount: scheduleOrderWorkOrderCodes.size,
      scheduleOrderWorkOrderCodeSamples: Array.from(scheduleOrderWorkOrderCodes).slice(0, 10),
      projectRowsOutsideScheduleOrderPool,
      taskRowsOutsideScheduleOrderPool,
      metrics,
      orderColorMetrics,
      dateIntervalBefore,
      dateIntervalAfter,
      dateIntervalAfterReload,
      fullscreenState,
      collapsedMetrics,
      expandedMetrics,
      bottomScrollResult,
      scrolledCollapsedMetrics,
      tooltipColors,
      pageErrors,
      mesRequestCount: mesRequests.length,
      writeRequests,
      screenshotPath
    }
    writeFileSync(resolve(outputDir, 'gantt-readability-result.json'), JSON.stringify(evidence, null, 2), 'utf8')

    console.log(`mes-pro-task gantt readability real page check passed: ${screenshotPath}`)
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
