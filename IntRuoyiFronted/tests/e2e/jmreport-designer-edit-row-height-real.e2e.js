const assert = require('node:assert/strict')
const { spawnSync } = require('node:child_process')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const BASE_URL = process.env.JMREPORT_DESIGNER_ROW_HEIGHT_BASE_URL || 'http://localhost:8081'
const TENANT = process.env.JMREPORT_DESIGNER_ROW_HEIGHT_TENANT || '测试租户'
const USERNAME = process.env.JMREPORT_DESIGNER_ROW_HEIGHT_USERNAME || 'aoteman'
const PASSWORD = process.env.JMREPORT_DESIGNER_ROW_HEIGHT_PASSWORD
const EXECUTABLE_PATH = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
const RESULT_DIR = path.resolve(process.cwd(), 'tests/output/20260713-jmreport-designer-row-height')
const MIN_IFRAME_SCREENSHOT_NON_WHITE_PIXELS = 5000

function requirePrerequisites() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实 E2E 只允许本机前端入口')
  assert(PASSWORD, '缺少 JMREPORT_DESIGNER_ROW_HEIGHT_PASSWORD')
  assert(EXECUTABLE_PATH, '缺少 PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH')
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=/index`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const loginForm = page.locator('.login-form:visible').first()
  await loginForm.waitFor({ state: 'visible', timeout: 60000 })

  const tenantInput = loginForm
    .locator('.el-select input[role="combobox"], input.el-select__input')
    .first()
  await tenantInput.fill(TENANT)
  const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: TENANT }).first()
  await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
  await tenantOption.click()

  await loginForm
    .locator('input.el-input__inner:not([role="combobox"]):not([type="password"])')
    .first()
    .fill(USERNAME)
  await loginForm.locator('input[type="password"]').first().fill(PASSWORD)
  const loginResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/admin-api/system/auth/login') &&
      response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await loginForm.getByRole('button', { name: /^登录$/ }).click()
  const loginBody = await (await loginResponse).json()
  assert(loginBody.code === 0 || loginBody.code === 200, `登录失败：${loginBody.msg || loginBody.code}`)
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 60000 })
}

async function openFirstBatchRecordReportEditor(page) {
  await page.goto(`${BASE_URL}/mes/pro/batch-record-form-list`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  const reportItem = page.locator('.batch-record-report-list__item').first()
  await reportItem.waitFor({ state: 'visible', timeout: 60000 })
  await reportItem.click()

  await page.getByRole('button', { name: '编辑' }).first().click()
  await page.locator('iframe').first().waitFor({ state: 'visible', timeout: 60000 })
  await page.waitForURL((url) => url.searchParams.get('reportMode') === 'edit', { timeout: 60000 })
  await page.waitForFunction(
    () => Array.from(document.querySelectorAll('iframe')).some((iframe) => iframe.src.includes('/jmreport/index/')),
    { timeout: 60000 }
  )

  const iframeHandle = await page.locator('iframe').first().elementHandle()
  const frame = iframeHandle ? await iframeHandle.contentFrame() : undefined
  assert(frame, '未找到 JMReport 编辑 iframe')
  await frame.waitForLoadState('domcontentloaded', { timeout: 60000 })
  await frame.waitForFunction(
    () => Boolean(window.xs?.sheet?.data?.rows?._) && document.querySelectorAll('.fillForm-box').length > 0,
    { timeout: 60000 }
  )
  return frame
}

async function verifyDesignerRowHeight(frame) {
  await frame.waitForFunction(
    () => {
      const rows = window.xs?.sheet?.data?.rows?._
      if (!rows) return false
      const fillRows = Object.values(rows).filter((row) =>
        Object.values(row?.cells || {}).some((cell) => Boolean(cell?.fillForm))
      )
      const boxes = Array.from(document.querySelectorAll('.fillForm-box'))
      return (
        fillRows.length > 0 &&
        boxes.length > 0 &&
        fillRows.every((row) => Number(row.height || 0) >= 40) &&
        boxes.every((box) => box.getBoundingClientRect().height >= 35)
      )
    },
    { timeout: 30000 }
  )

  return await frame.evaluate(() => {
    const rows = window.xs.sheet.data.rows._
    const fillRows = Object.entries(rows)
      .filter(([, row]) =>
        Object.values(row?.cells || {}).some((cell) => Boolean(cell?.fillForm))
      )
      .map(([rowIndex, row]) => ({ rowIndex: Number(rowIndex), height: Number(row.height || 0) }))

    const boxes = Array.from(document.querySelectorAll('.fillForm-box'))
    const boxMetrics = boxes.map((box) => {
      const rect = box.getBoundingClientRect()
      const field = box.querySelector('input, textarea, .ivu-input')
      const fieldRect = field?.getBoundingClientRect()
      return {
        top: Math.round(rect.top),
        height: rect.height,
        fieldHeight: fieldRect?.height || 0,
        fits: !fieldRect || fieldRect.bottom <= rect.bottom + 2
      }
    })
    const uniqueTops = Array.from(new Set(boxMetrics.map((item) => item.top))).sort((a, b) => a - b)
    const topGaps = uniqueTops.slice(1).map((top, index) => top - uniqueTops[index])
    const canvas = document.querySelector('canvas.jm-sheet-table')
    const context = canvas?.getContext('2d')
    let nonWhiteCanvasPixels = 0
    if (canvas && context) {
      const imageData = context.getImageData(0, 0, Math.min(canvas.width, 800), Math.min(canvas.height, 400)).data
      for (let index = 0; index < imageData.length; index += 16) {
        const red = imageData[index]
        const green = imageData[index + 1]
        const blue = imageData[index + 2]
        const alpha = imageData[index + 3]
        if (alpha > 0 && !(red > 248 && green > 248 && blue > 248)) {
          nonWhiteCanvasPixels += 1
        }
      }
    }

    const visibleTextNodes = Array.from(document.querySelectorAll('td, th, canvas, .x-spreadsheet-table, .x-spreadsheet-scrollbar, .x-spreadsheet-sheet'))
      .map((node) => {
        const rect = node.getBoundingClientRect()
        return {
          tag: node.tagName,
          className: String(node.className || ''),
          text: String(node.textContent || '').trim(),
          width: rect.width,
          height: rect.height,
          visible: rect.width > 1 && rect.height > 1
        }
      })
      .filter((item) => item.visible)
    const hasVisibleGridContent = visibleTextNodes.some(
      (item) =>
        item.text.includes('记录编号') ||
        item.text.includes('版本') ||
        item.text.includes('产品信息') ||
        item.width > 200 ||
        item.height > 200
    )

    return {
      reportId: location.pathname.split('/').pop(),
      fillRowCount: fillRows.length,
      fillBoxCount: boxMetrics.length,
      minRowHeight: Math.min(...fillRows.map((item) => item.height)),
      minBoxHeight: Math.min(...boxMetrics.map((item) => item.height)),
      minTopGap: Math.min(...topGaps),
      allFieldsFit: boxMetrics.every((item) => item.fits),
      nonWhiteCanvasPixels,
      hasVisibleGridContent,
      visibleGridProbe: visibleTextNodes.slice(0, 20)
    }
  })
}

function analyzePngNonWhitePixels(file) {
  const script = [
    'import json, sys',
    'from PIL import Image',
    'img = Image.open(sys.argv[1]).convert("RGBA")',
    'data = img.tobytes()',
    'non_white = 0',
    'total = 0',
    'for index in range(0, len(data), 64):',
    '    red = data[index]',
    '    green = data[index + 1]',
    '    blue = data[index + 2]',
    '    alpha = data[index + 3]',
    '    total += 1',
    '    if alpha > 0 and not (red > 248 and green > 248 and blue > 248):',
    '        non_white += 1',
    'print(json.dumps({"nonWhite": non_white, "sampled": total}))'
  ].join('\n')
  const result = spawnSync('python', ['-X', 'utf8', '-c', script, file], {
    encoding: 'utf8'
  })
  assert.equal(result.status, 0, `截图像素分析失败：${result.stderr || result.stdout}`)
  return JSON.parse(result.stdout)
}

async function main() {
  requirePrerequisites()
  fs.mkdirSync(RESULT_DIR, { recursive: true })
  const browser = await chromium.launch({
    headless: true,
    executablePath: EXECUTABLE_PATH
  })
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } })
  const mesWriteRequests = []
  page.on('request', (request) => {
    const method = request.method().toUpperCase()
    if (
      ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method) &&
      request.url().includes('/admin-api/mes/')
    ) {
      mesWriteRequests.push(`${method} ${request.url()}`)
    }
  })

  try {
    await login(page)
    const frame = await openFirstBatchRecordReportEditor(page)
    const result = await verifyDesignerRowHeight(frame)
    const iframeScreenshotPath = path.join(RESULT_DIR, 'jmreport-designer-row-height-iframe.png')
    await page.locator('iframe').first().screenshot({
      path: iframeScreenshotPath
    })
    const screenshotStats = analyzePngNonWhitePixels(iframeScreenshotPath)
    await page.screenshot({
      path: path.join(RESULT_DIR, 'jmreport-designer-row-height.png'),
      fullPage: true
    })
    assert.equal(
      result.hasVisibleGridContent,
      true,
      `JMReport 编辑页视觉上仍为空白：${JSON.stringify(result.visibleGridProbe)}`
    )
    assert(
      result.nonWhiteCanvasPixels > 1000,
      `JMReport 编辑画布为空白或未完成绘制：nonWhiteCanvasPixels=${result.nonWhiteCanvasPixels}`
    )
    assert(
      screenshotStats.nonWhite > MIN_IFRAME_SCREENSHOT_NON_WHITE_PIXELS,
      `JMReport 编辑 iframe 截图仍为空白：nonWhite=${screenshotStats.nonWhite}`
    )
    assert.equal(result.allFieldsFit, true, '仍存在输入控件超出 fillForm-box 的情况')
    assert(result.minTopGap >= 35, `fillForm-box 纵向间距过小：${result.minTopGap}`)
    assert.equal(mesWriteRequests.length, 0, `只读 E2E 不应产生 MES 写请求：${mesWriteRequests.join(', ')}`)
    console.log(
      `PASS: JMReport designer edit row height real E2E tenant=${TENANT} username=${USERNAME} report=${result.reportId} fillRows=${result.fillRowCount} minRowHeight=${result.minRowHeight} minBoxHeight=${result.minBoxHeight}`
    )
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
