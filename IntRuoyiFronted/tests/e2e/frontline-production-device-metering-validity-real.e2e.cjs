const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = 'http://127.0.0.1:8081'
const ROUTE = '/mes/pro/feedback/edhr-batch-production-fill'
const TARGET_PROCESS = '单包装工序'
const TARGET_DEVICE_CODES = ['A05199', 'A05203', 'A05048', 'A03274']
const METERING_VALIDITY_PROCESS = '光固Ⅰ工序'
const METERING_VALIDITY_DEVICE_CODES = ['A05075', 'A05059']
const OUTPUT_DIR = path.resolve(
  process.cwd(),
  'output',
  'playwright',
  '20260811-frontline-remove-metering-validity-parameter-row'
)
const BROWSER_EXECUTABLE =
  process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH ||
  process.env.PLAYWRIGHT_CHROME_EXECUTABLE ||
  'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
const MES_WRITE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if ((await item.isVisible().catch(() => false)) && (await item.isEnabled().catch(() => false))) {
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可用的${label}输入框。`)
}

async function selectDefaultTenant(page, form) {
  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) === 0 || !(await tenantInput.isVisible().catch(() => false))) {
    return
  }
  if ((await tenantInput.inputValue()).trim() === '芋道源码') {
    return
  }
  await tenantInput.click()
  await tenantInput.fill('芋道源码')
  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item')
    .filter({ hasText: '芋道源码' })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
}

async function login(page) {
  await page.context().clearCookies()
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  if (!page.url().includes('/login')) {
    return
  }

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(
    await form
      .locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder*="验证码"]:visible')
      .count(),
    0,
    '登录页启用了验证码，无法执行无人值守真实 E2E。'
  )

  await selectDefaultTenant(page, form)
  await fillFirstVisible(
    form.locator(
      'input[placeholder*="账号"], input[placeholder*="用户名"], input.el-input__inner:not([role="combobox"]):not([type="password"])'
    ),
    'admin',
    '用户名'
  )
  const passwordInput = form.locator('input[type="password"]').first()
  await passwordInput.waitFor({ state: 'visible', timeout: 30000 })
  assert.ok((await passwordInput.inputValue()).length > 0, '本机登录页未预填默认密码。')

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 90000 }
  )
  await form.getByRole('button', { name: /^登录$/ }).click()
  const loginResponse = await loginResponsePromise
  assert.ok(loginResponse.ok(), `登录 HTTP 失败：${loginResponse.status()}`)
  const loginBody = await loginResponse.json()
  assert.ok([0, 200].includes(loginBody.code), `登录业务失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 90000 })
}

async function selectProductionProcess(screen, processName) {
  const processButton = screen.locator('.frontline-production-process-current').first()
  await processButton.waitFor({ state: 'visible', timeout: 90000 })
  await processButton.click()

  const option = screen
    .locator('.frontline-picker__option')
    .filter({ hasText: processName })
    .first()
  await option.waitFor({ state: 'visible', timeout: 30000 })
  await option.click()
  await screen.getByText(processName, { exact: false }).first().waitFor({
    state: 'visible',
    timeout: 90000
  })
}

async function measureProductionLayout(page, viewportLabel) {
  const screen = page.locator('[data-frontline-production-operator]').first()
  const layout = await screen.evaluate((element) => {
    const select = (selector) => {
      const target = element.querySelector(selector)
      assertElement(target, selector)
      return target
    }
    const assertElement = (target, selector) => {
      if (!target) {
        throw new Error(`缺少布局节点：${selector}`)
      }
    }
    const toRect = (target) => {
      const rect = target.getBoundingClientRect()
      return {
        left: rect.left,
        top: rect.top,
        right: rect.right,
        bottom: rect.bottom,
        width: rect.width,
        height: rect.height
      }
    }
    const main = select('.frontline-production-main')
    const quantity = select('.frontline-production-quantity-panel')
    const device = select('.frontline-production-device-panel')
    const submitBar = select('.frontline-production-submit-bar')
    const resetButton = select('.frontline-production-reset-button')
    const submitButton = select('.frontline-production-submit-button')
    const deviceTabs = select('.frontline-production-device-tabs')
    const parameterArea = select('.frontline-production-device-current')
    const clearance = select('.frontline-production-clearance-confirmations')
    return {
      viewportWidth: window.innerWidth,
      viewportHeight: window.innerHeight,
      main: toRect(main),
      quantity: toRect(quantity),
      device: toRect(device),
      submitBar: toRect(submitBar),
      resetButton: toRect(resetButton),
      submitButton: toRect(submitButton),
      deviceTabs: toRect(deviceTabs),
      parameterArea: toRect(parameterArea),
      clearance: toRect(clearance),
      mainGridRows: window.getComputedStyle(main).gridTemplateRows,
      mainGridColumns: window.getComputedStyle(main).gridTemplateColumns,
      deviceGridRows: window.getComputedStyle(device).gridTemplateRows
    }
  })
  const tolerance = 2
  assert.ok(
    Math.abs(layout.quantity.left - layout.submitBar.left) <= tolerance,
    `${viewportLabel} 操作区左边界必须与数量面板对齐。`
  )
  assert.ok(
    Math.abs(layout.quantity.right - layout.submitBar.right) <= tolerance,
    `${viewportLabel} 操作区右边界必须与数量面板对齐。`
  )
  assert.ok(
    layout.submitBar.right < layout.device.left,
    `${viewportLabel} 提交操作不得进入设备面板列。`
  )
  assert.ok(
    Math.abs(layout.device.top - layout.quantity.top) <= tolerance,
    `${viewportLabel} 设备面板顶边必须与数量面板对齐。`
  )
  assert.ok(
    Math.abs(layout.device.bottom - layout.submitBar.bottom) <= tolerance,
    `${viewportLabel} 设备面板必须向下延伸到操作区底边。`
  )
  assert.ok(
    layout.device.bottom > layout.quantity.bottom + layout.submitBar.height,
    `${viewportLabel} 设备面板必须跨越主体内容行和操作行。`
  )
  assert.ok(
    layout.submitButton.right <= layout.submitBar.right + tolerance,
    `${viewportLabel} 正式提交按钮不得越过左侧操作区。`
  )
  assert.ok(
    layout.resetButton.right < layout.submitButton.left,
    `${viewportLabel} 重填和正式提交按钮不得重叠。`
  )
  assert.ok(
    layout.parameterArea.height > layout.deviceTabs.height,
    `${viewportLabel} 设备参数区必须吸收右侧新增高度。`
  )
  assert.ok(
    layout.parameterArea.bottom < layout.clearance.top,
    `${viewportLabel} 参数区和清场确认区不得重叠。`
  )
  assert.match(layout.mainGridRows, /126px$/, `${viewportLabel} 左下操作轨道必须保持 126px。`)
  assert.match(layout.deviceGridRows, /^118px\s/, `${viewportLabel} 设备卡片轨道必须保持 118px。`)
  assert.ok(layout.device.right <= layout.viewportWidth + tolerance, `${viewportLabel} 设备面板不得越出视口。`)
  return layout
}

async function verifyDeviceCards(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, {
    waitUntil: 'domcontentloaded',
    timeout: 90000
  })
  const screen = page.locator('[data-frontline-production-operator]').first()
  await screen.waitFor({ state: 'visible', timeout: 90000 })
  await selectProductionProcess(screen, TARGET_PROCESS)

  const cards = screen.locator('.frontline-production-device-card')
  await cards.first().waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(await cards.count(), TARGET_DEVICE_CODES.length, '单包装工序必须显示四张设备卡片。')

  const actualCodes = []
  const visualStyles = []
  for (let index = 0; index < TARGET_DEVICE_CODES.length; index += 1) {
    const card = cards.nth(index)
    const code = String(await card.locator('.device-tab').textContent()).trim()
    actualCodes.push(code)
    const checkbox = card.locator('input[data-frontline-device-metering-validity]')
    assert.equal(await checkbox.count(), 1, `${code} 必须且只能显示一个设备计量效期 checkbox。`)
    assert.equal(await checkbox.isChecked(), true, `${code} 的设备计量效期必须默认选中。`)
    await card.getByText('在计量效期内', { exact: true }).waitFor({ state: 'visible' })
    const cardVisualStyle = await card.evaluate((element) => {
      const header = element.querySelector('.device-tab')
      const footer = element.querySelector('.frontline-production-device-metering-validity')
      const visibleCheckbox = footer.querySelector('span')
      const label = footer.querySelector('em')
      const headerStyle = window.getComputedStyle(header)
      const footerStyle = window.getComputedStyle(footer)
      const footerRect = footer.getBoundingClientRect()
      const checkboxRect = visibleCheckbox.getBoundingClientRect()
      const labelRect = label.getBoundingClientRect()
      const contentLeft = Math.min(checkboxRect.left, labelRect.left)
      const contentRight = Math.max(checkboxRect.right, labelRect.right)
      const contentTop = Math.min(checkboxRect.top, labelRect.top)
      const contentBottom = Math.max(checkboxRect.bottom, labelRect.bottom)
      return {
        headerBackground: headerStyle.backgroundColor,
        headerColor: headerStyle.color,
        footerBackground: footerStyle.backgroundColor,
        footerColor: footerStyle.color,
        horizontalCenterOffset: Math.abs(
          (contentLeft + contentRight) / 2 - (footerRect.left + footerRect.right) / 2
        ),
        verticalCenterOffset: Math.abs(
          (contentTop + contentBottom) / 2 - (footerRect.top + footerRect.bottom) / 2
        )
      }
    })
    visualStyles.push({ code, ...cardVisualStyle })
    assert.equal(cardVisualStyle.headerBackground, 'rgb(36, 50, 43)', `${code} 上半区必须为统一黑色。`)
    assert.equal(cardVisualStyle.headerColor, 'rgb(255, 255, 255)', `${code} 编码必须为统一白色。`)
    assert.equal(cardVisualStyle.footerBackground, 'rgb(255, 255, 255)', `${code} 下半区必须为统一白色。`)
    assert.equal(cardVisualStyle.footerColor, 'rgb(17, 26, 21)', `${code} 下半区文字必须保持深色可读。`)
    assert.ok(cardVisualStyle.horizontalCenterOffset <= 1, `${code} 下半区内容必须水平居中。`)
    assert.ok(cardVisualStyle.verticalCenterOffset <= 1, `${code} 下半区内容必须垂直居中。`)
  }
  assert.deepEqual(actualCodes, TARGET_DEVICE_CODES, '单包装工序设备编码及顺序必须与正式配置一致。')

  const activeCodeBefore = String(
    await cards.filter({ has: page.locator('.device-tab[aria-selected="true"]') }).locator('.device-tab').textContent()
  ).trim()
  const secondCard = cards.nth(1)
  const secondValidity = secondCard.locator('.frontline-production-device-metering-validity')
  await secondValidity.click()
  assert.equal(
    await secondCard.locator('input[data-frontline-device-metering-validity]').isChecked(),
    false,
    '取消第二台设备计量效期后必须只更新该设备。'
  )
  assert.equal(
    await cards.nth(0).locator('input[data-frontline-device-metering-validity]').isChecked(),
    true,
    '切换第二台设备计量效期不得影响第一台设备。'
  )
  const activeCodeAfter = String(
    await cards.filter({ has: page.locator('.device-tab[aria-selected="true"]') }).locator('.device-tab').textContent()
  ).trim()
  assert.equal(activeCodeAfter, activeCodeBefore, '点击卡片 checkbox 不得切换当前设备。')
  await secondValidity.click()

  const globalValidity = screen
    .locator('.frontline-production-clearance-confirmation')
    .filter({ hasText: '效期' })
    .first()
  await globalValidity.waitFor({ state: 'visible', timeout: 30000 })
  assert.equal(
    await globalValidity.locator('input[data-production-clearance-checkbox]').isChecked(),
    true,
    '底部全局效期确认必须继续独立存在并默认选中。'
  )
  assert.equal(
    await globalValidity.locator('input[data-frontline-device-metering-validity]').count(),
    0,
    '底部全局效期确认不得与设备卡片 checkbox 共用控件。'
  )

  return {
    actualCodes,
    activeCodeBefore,
    activeCodeAfter,
    visualStyles,
    deviceCheckboxCount: await screen.locator('input[data-frontline-device-metering-validity]').count(),
    globalValidityCount: await globalValidity.count()
  }
}

async function verifyMeteringValidityParameterRowsRemoved(screen) {
  await selectProductionProcess(screen, METERING_VALIDITY_PROCESS)
  const cards = screen.locator('.frontline-production-device-card')
  await cards.first().waitFor({ state: 'visible', timeout: 90000 })
  assert.equal(
    await cards.count(),
    METERING_VALIDITY_DEVICE_CODES.length,
    '光固Ⅰ工序必须显示两张正式设备卡片。'
  )

  const actualCodes = []
  const parameterNamesByDevice = {}
  for (let index = 0; index < METERING_VALIDITY_DEVICE_CODES.length; index += 1) {
    const card = cards.nth(index)
    const tab = card.locator('.device-tab')
    const code = String(await tab.textContent()).trim()
    actualCodes.push(code)
    await tab.click()
    await card.locator('.device-tab[aria-selected="true"]').waitFor({
      state: 'visible',
      timeout: 30000
    })

    const parameterArea = screen.locator('.frontline-production-device-current').first()
    const parameterRows = parameterArea.locator('.frontline-production-device-param')
    assert.equal(
      await parameterRows.filter({ hasText: '在计量效期内' }).count(),
      0,
      `${code} 的设备参数列表不得显示“在计量效期内”控件行。`
    )
    assert.equal(
      await parameterArea.locator('input[data-frontline-boolean-parameter][aria-label="在计量效期内"]').count(),
      0,
      `${code} 的设备参数区不得保留不可见或可聚焦的重复计量效期 checkbox。`
    )
    assert.equal(
      await card.locator('input[data-frontline-device-metering-validity]').count(),
      1,
      `${code} 的设备卡片必须继续保留唯一计量效期 checkbox。`
    )
    parameterNamesByDevice[code] = (await parameterRows.locator('.device-param-name').allTextContents())
      .map((name) => name.trim())
      .filter(Boolean)
  }
  assert.deepEqual(
    actualCodes,
    METERING_VALIDITY_DEVICE_CODES,
    '光固Ⅰ工序设备编码及顺序必须与正式配置一致。'
  )
  return { actualCodes, parameterNamesByDevice }
}

async function run() {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true })
  assert.ok(fs.existsSync(BROWSER_EXECUTABLE), `浏览器不存在：${BROWSER_EXECUTABLE}`)
  const frontend = await fetch(BASE_URL)
  assert.equal(frontend.status, 200, '本机前端 8081 必须可访问。')
  const backend = await fetch('http://127.0.0.1:48081/actuator/health')
  assert.equal(backend.status, 200, '本机后端 48081 健康检查必须可访问。')
  assert.equal((await backend.json()).status, 'UP', '本机后端必须为 UP。')

  const browser = await chromium.launch({
    headless: process.env.FRONTLINE_DEVICE_CARD_E2E_HEADED !== '1',
    executablePath: BROWSER_EXECUTABLE
  })
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  })
  const page = await context.newPage()
  const consoleErrors = []
  const pageErrors = []
  const mesWriteRequests = []
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text())
    }
  })
  page.on('pageerror', (error) => pageErrors.push(error.message))
  page.on('request', (request) => {
    if (
      request.url().includes('/admin-api/mes/') &&
      MES_WRITE_METHODS.has(request.method()) &&
      !request.url().includes('/frontline/device-account/switch-employee')
    ) {
      mesWriteRequests.push({
        method: request.method(),
        path: new URL(request.url()).pathname
      })
    }
  })

  try {
    await login(page)
    const evidence = await verifyDeviceCards(page)
    const layout1920x1080 = await measureProductionLayout(page, '1920x1080')
    assert.deepEqual(mesWriteRequests, [], '只读验收不得发送 MES 业务写请求。')
    assert.deepEqual(pageErrors, [], `页面错误：${pageErrors.join('\n')}`)
    const screenshotPath = path.join(OUTPUT_DIR, 'single-pack-device-cards-1920x1080.png')
    await page.screenshot({ path: screenshotPath, fullPage: true })
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.waitForFunction(() => {
      const screen = document.querySelector('[data-frontline-production-operator]')
      return screen && screen.getBoundingClientRect().right <= window.innerWidth + 2
    })
    const layout1440x900 = await measureProductionLayout(page, '1440x900')
    const narrowScreenshotPath = path.join(OUTPUT_DIR, 'single-pack-layout-1440x900.png')
    await page.screenshot({ path: narrowScreenshotPath, fullPage: true })
    await page.setViewportSize({ width: 1920, height: 1080 })
    const meteringValidityRows = await verifyMeteringValidityParameterRowsRemoved(
      page.locator('[data-frontline-production-operator]').first()
    )
    const meteringValidityScreenshotPath = path.join(
      OUTPUT_DIR,
      'uv1-metering-validity-parameter-row-removed-1920x1080.png'
    )
    await page.screenshot({ path: meteringValidityScreenshotPath, fullPage: true })
    assert.deepEqual(mesWriteRequests, [], '只读验收不得发送 MES 业务写请求。')
    assert.deepEqual(pageErrors, [], `页面错误：${pageErrors.join('\n')}`)
    assert.deepEqual(consoleErrors, [], `控制台错误：${consoleErrors.join('\n')}`)
    fs.writeFileSync(
      path.join(OUTPUT_DIR, 'result.json'),
      `${JSON.stringify(
        {
          status: 'PASS',
          generatedAt: new Date().toISOString(),
          route: ROUTE,
          process: TARGET_PROCESS,
          ...evidence,
          layout1920x1080,
          layout1440x900,
          meteringValidityRows,
          mesWriteRequests,
          consoleErrors,
          pageErrors,
          screenshotPath,
          narrowScreenshotPath,
          meteringValidityScreenshotPath
        },
        null,
        2
      )}\n`,
      'utf8'
    )
    console.log('PASS: frontline production device metering validity real E2E')
  } finally {
    await context.close().catch(() => undefined)
    await browser.close().catch(() => undefined)
  }
}

run().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
