const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const WORKSPACE_ROOT = path.resolve(__dirname, '../../..')
const BASE_URL = (process.env.EDHR_WORD_TABLE_RENDER_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '')
const BACKEND_URL = (process.env.EDHR_WORD_TABLE_RENDER_BACKEND_URL || 'http://127.0.0.1:48081').replace(/\/+$/, '')
const TEST_TENANT = process.env.EDHR_WORD_TABLE_RENDER_TEST_TENANT || '测试租户'
const TEST_USERNAME = process.env.EDHR_WORD_TABLE_RENDER_TEST_USERNAME || 'aoteman'
const TEST_PASSWORD = process.env.EDHR_WORD_TABLE_RENDER_TEST_PASSWORD || '111111'
const SAMPLE_DOC_PATH =
  process.env.EDHR_WORD_TABLE_RENDER_SAMPLE_DOC ||
  'C:\\Users\\BJB110\\Desktop\\2\\2\\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc'
const ROUTE = '/mes/pro/batch-record-form-list'
const ROUTE_KEY = 'B'
const RUN_ID = process.env.EDHR_WORD_TABLE_RENDER_RUN_ID || String(Date.now())
const BATCH_RECORD_NAME =
  process.env.EDHR_WORD_TABLE_RENDER_BATCH_RECORD_NAME || `E2E-TABLE-RENDER-${RUN_ID}`
const OUTPUT_DIR =
  process.env.EDHR_WORD_TABLE_RENDER_OUTPUT_DIR ||
  path.join(process.cwd(), 'test-results', 'edhr-word-table-render')
const EVIDENCE_FILE = path.join(OUTPUT_DIR, 'evidence.md')

const EXPECTED_TITLES = [
  '产品信息',
  '粗洗工序生产记录',
  '精洗工序生产记录',
  '清洗工序生产记录',
  '清洁工序生产记录',
  '组装Ⅰ工序生产记录',
  '光固Ⅰ工序生产记录',
  '硅化Ⅰ工序生产记录',
  '硅化Ⅱ工序生产记录',
  '组装Ⅱ工序生产记录',
  '检测工序生产记录',
  '光固Ⅱ工序生产记录',
  '单包装工序生产记录',
  '中包装工序生产记录',
  '大包装工序生产记录'
]

const REPRESENTATIVE_SCREENSHOT_TITLES = new Set(EXPECTED_TITLES)

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true })
}

function assertLocalOnly() {
  assert.equal(BASE_URL, 'http://localhost:8081', '真实页面验证必须固定使用本机前端 http://localhost:8081')
  assert.match(BACKEND_URL, /^http:\/\/(127\.0\.0\.1|localhost):48081$/, '真实页面验证必须固定使用本机后端 48081')
  assert.equal(TEST_TENANT, '测试租户', '真实页面验证必须使用测试租户')
  assert.equal(TEST_USERNAME, 'aoteman', '真实页面验证必须使用测试租户 aoteman')
  assert.ok(fs.existsSync(SAMPLE_DOC_PATH), `缺少用户指定 Word 批记录文档：${SAMPLE_DOC_PATH}`)
}

function assertBusinessSuccess(body, label) {
  assert.ok(body && typeof body === 'object', `${label} 必须返回 JSON 对象`)
  const code = Number(body.code)
  assert.ok([0, 200].includes(code), `${label} 业务响应失败：${body.msg || body.message || body.code}`)
  return body.data
}

async function parseBusinessResponse(response, label) {
  await response.finished().catch(() => undefined)
  assert.equal(response.status(), 200, `${label} HTTP 必须为 200，实际 ${response.status()}`)
  return assertBusinessSuccess(await response.json(), label)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible()) {
      await item.fill('')
      await item.fill(value)
      return
    }
  }
  throw new Error(`缺少可填写控件：${label}`)
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
  throw new Error(`缺少可点击控件：${label}`)
}

async function waitForBusinessResponse(page, endpoint, label, method, timeout = 180000) {
  const response = await page.waitForResponse(
    (item) => item.url().includes(endpoint) && item.request().method() === method,
    { timeout }
  )
  return parseBusinessResponse(response, label)
}

async function login(page) {
  await page.goto(`${BASE_URL}/login?redirect=${encodeURIComponent(ROUTE)}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  if (!page.url().includes('/login')) return

  const form = page.locator('form.login-form:visible').first()
  await form.waitFor({ state: 'visible', timeout: 60000 })
  if ((await form.locator('.verify-img-panel, .verify-bar-area, input[placeholder="请输入验证码"]').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人值守执行真实 E2E。')
  }

  const tenantInput = form.locator('.el-select input[role="combobox"], input.el-select__input').first()
  if ((await tenantInput.count()) > 0 && (await tenantInput.isVisible())) {
    await tenantInput.click()
    await tenantInput.fill('')
    await tenantInput.fill(TEST_TENANT)
    await page.keyboard.press('Enter')
  } else {
    await fillFirstVisible(form.locator('input[placeholder="请输入租户名称"]'), TEST_TENANT, '租户')
  }
  await fillFirstVisible(form.locator('input[placeholder="请输入用户名"]'), TEST_USERNAME, '用户名')
  await fillFirstVisible(form.locator('input[type="password"]'), TEST_PASSWORD, '密码')

  const loginResponsePromise = page.waitForResponse(
    (response) => response.url().includes('/system/auth/login') && response.request().method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(form.getByRole('button', { name: /^登录$/ }), '登录按钮')
  await parseBusinessResponse(await loginResponsePromise, '登录')
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
}

async function openTemplatePage(page) {
  await page.goto(`${BASE_URL}${ROUTE}`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  await page.getByText('表单模板', { exact: false }).first().waitFor({ state: 'visible', timeout: 60000 })
  await page.getByRole('button', { name: /导入 Word|文件导入/ }).first().waitFor({
    state: 'visible',
    timeout: 60000
  })
}

async function importWordTemplateByUi(page) {
  await openTemplatePage(page)
  const fileChooserPromise = page.waitForEvent('filechooser', { timeout: 30000 })
  await clickFirstEnabled(page.getByRole('button', { name: /导入 Word|文件导入/ }), '导入 Word')
  const fileChooser = await fileChooserPromise
  await fileChooser.setFiles(SAMPLE_DOC_PATH)

  const prompt = page.locator('.el-message-box:visible').filter({ hasText: '批记录名称' }).first()
  await prompt.waitFor({ state: 'visible', timeout: 60000 })
  await fillFirstVisible(prompt.locator('input'), BATCH_RECORD_NAME, '批记录名称')

  const existsPromise = waitForBusinessResponse(
    page,
    '/admin-api/mes/pro/batch-record-report/exists',
    '批记录名称重复检查',
    'GET',
    60000
  )
  const uploadRequestPromise = page.waitForRequest(
    (request) =>
      request.url().includes('/admin-api/mes/pro/batch-record-report/recognize-uploaded') &&
      request.method() === 'POST',
    { timeout: 60000 }
  )
  await clickFirstEnabled(prompt.getByRole('button', { name: /^确定$/ }), '确认批记录名称')
  const existed = await existsPromise
  if (existed) {
    const upgradeConfirm = page.locator('.el-message-box:visible').filter({ hasText: '是否使用 B Word COM 升级' }).first()
    await upgradeConfirm.waitFor({ state: 'visible', timeout: 60000 })
    await clickFirstEnabled(upgradeConfirm.locator('button.el-button--primary'), '确认升级批记录')
  }
  const uploadRequest = await uploadRequestPromise
  assert.equal(uploadRequest.method(), 'POST', 'Word 导入必须通过真实 POST 上传文件。')

  await page
    .getByText(new RegExp(`批记录名称「${BATCH_RECORD_NAME.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}」路线 B 解析完成`))
    .first()
    .waitFor({ state: 'visible', timeout: 900000 })
  await page.getByText(BATCH_RECORD_NAME).first().waitFor({ state: 'visible', timeout: 60000 })
}

async function ensureWordTemplateAvailable(page) {
  await importWordTemplateByUi(page)
}

async function browserAuth(page) {
  const snapshot = await page.evaluate(() => {
    const result = {}
    for (let index = 0; index < localStorage.length; index += 1) {
      const key = localStorage.key(index)
      result[key] = localStorage.getItem(key)
    }
    for (let index = 0; index < sessionStorage.length; index += 1) {
      const key = sessionStorage.key(index)
      result[key] = result[key] || sessionStorage.getItem(key)
    }
    return result
  })
  const unwrap = (raw) => {
    if (!raw) return ''
    let current = raw
    for (let index = 0; index < 6; index += 1) {
      try {
        current = JSON.parse(current)
      } catch {
        break
      }
      if (current && typeof current === 'object') {
        if (Object.prototype.hasOwnProperty.call(current, 'accessToken')) current = current.accessToken
        else if (Object.prototype.hasOwnProperty.call(current, 'v')) current = current.v
        else if (Object.prototype.hasOwnProperty.call(current, 'value')) current = current.value
        else break
      }
      if (typeof current !== 'string') break
    }
    return String(current || '').replace(/^"|"$/g, '')
  }
  return {
    token: unwrap(snapshot.ACCESS_TOKEN || snapshot.accessToken || snapshot.token),
    tenantId: unwrap(snapshot.tenantId || snapshot.TENANT_ID),
    visitTenantId: unwrap(snapshot.visitTenantId)
  }
}

async function verifyImportedReportsByApi(page) {
  const { token, tenantId, visitTenantId } = await browserAuth(page)
  assert.ok(token, '最终 API 核验需要浏览器登录 token')
  assert.ok(tenantId, '最终 API 核验需要 tenant-id')
  const response = await page.request.get(`${BACKEND_URL}/admin-api/mes/pro/batch-record-report/page`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'tenant-id': String(tenantId),
      ...(visitTenantId ? { 'visit-tenant-id': String(visitTenantId) } : {})
    },
    params: {
      pageNo: 1,
      pageSize: 100,
      routeKey: ROUTE_KEY,
      batchRecordName: BATCH_RECORD_NAME
    }
  })
  const data = assertBusinessSuccess(await response.json(), '导入后报表分页查询')
  assert.equal(response.status(), 200, '导入后报表分页查询 HTTP 必须为 200')
  const list = data?.list || []
  const titles = new Set(list.map((item) => item.reportName || item.tableTitle))
  const missing = EXPECTED_TITLES.filter((title) => !titles.has(title))
  assert.equal(list.length, EXPECTED_TITLES.length, `导入后必须生成 ${EXPECTED_TITLES.length} 张渲染表`)
  assert.deepEqual(missing, [], `导入后缺少渲染表：${missing.join(', ')}`)
  return list.sort((left, right) => Number(left.sourceTableIndex || 0) - Number(right.sourceTableIndex || 0))
}

async function openReportPreviewByUi(page, report) {
  await openTemplatePage(page)
  await page.locator('.batch-record-record-list__item').filter({ hasText: BATCH_RECORD_NAME }).first().click()
  await page
    .locator('.batch-record-report-list__item')
    .filter({ hasText: report.reportName })
    .first()
    .click()
  await page
    .locator('.batch-record-panel-subtitle')
    .filter({ hasText: report.reportName })
    .first()
    .waitFor({ state: 'visible', timeout: 60000 })
  await page
    .locator('.batch-record-template-preview__actions')
    .getByRole('button', { name: /^打开$/ })
    .first()
    .click()
  await page.waitForURL((url) => url.searchParams.get('mode') === 'designer', { timeout: 60000 })
  await Promise.race([
    page.locator('iframe').first().waitFor({ state: 'visible', timeout: 60000 }),
    page.locator('.el-alert:visible').first().waitFor({ state: 'visible', timeout: 60000 })
  ])
  const errorAlert = page.locator('.el-alert:visible').first()
  if ((await errorAlert.count()) > 0 && (await errorAlert.isVisible())) {
    const errorText = await errorAlert.innerText().catch(() => '')
    if (errorText.trim()) {
      throw new Error(`${report.reportName} 预览页错误：${errorText.trim()}`)
    }
  }
}

async function validatePreviewFrame(page, report) {
  const iframeLocator = page.locator('iframe').first()
  await iframeLocator.waitFor({ state: 'visible', timeout: 120000 })
  const iframeHandle = await iframeLocator.elementHandle()
  const frame = await iframeHandle.contentFrame()
  assert.ok(frame, `${report.reportName} 预览 iframe 必须可访问`)

  await frame.waitForFunction(
    () => {
      const root =
        document.querySelector('#fillFormView-app .area-content') ||
        document.querySelector('#jm-sheet-wrapper .jm-sheet') ||
        document.querySelector('canvas') ||
        document.querySelector('table')
      const canvas = document.querySelector('canvas')
      return Boolean(root && canvas && canvas.getBoundingClientRect().width > 200 && canvas.getBoundingClientRect().height > 120)
    },
    null,
    { timeout: 120000 }
  )
  await frame.waitForTimeout(1500)

  const metrics = await frame.evaluate((reportName) => {
    const isDarkPixel = (data, offset) => {
      const alpha = data[offset + 3]
      if (alpha < 24) return false
      const red = data[offset]
      const green = data[offset + 1]
      const blue = data[offset + 2]
      return red < 190 && green < 190 && blue < 190
    }
    const analyzeCanvas = (canvas) => {
      const rect = canvas.getBoundingClientRect()
      const width = Math.max(0, Math.floor(canvas.width || rect.width || 0))
      const height = Math.max(0, Math.floor(canvas.height || rect.height || 0))
      if (width <= 0 || height <= 0) {
        return {
          width,
          height,
          rectWidth: Math.round(rect.width),
          rectHeight: Math.round(rect.height),
          darkPixelCount: 0,
          horizontalLineCount: 0,
          verticalLineCount: 0,
          rightDarkRatio: 0,
          collapsedColumnDarkRatio: 0
        }
      }
      const context = canvas.getContext('2d')
      const imageData = context.getImageData(0, 0, width, height)
      const data = imageData.data
      let darkPixelCount = 0
      let rightDarkPixelCount = 0
      let rightPixelCount = 0
      let narrowVerticalRunColumns = 0
      let horizontalLineCount = 0
      let verticalLineCount = 0
      const rightStart = Math.floor(width * 0.975)
      const rowStep = Math.max(1, Math.floor(height / 260))
      const colStep = Math.max(1, Math.floor(width / 260))
      for (let y = 0; y < height; y += rowStep) {
        let darkInRow = 0
        for (let x = 0; x < width; x += colStep) {
          const offset = (y * width + x) * 4
          if (!isDarkPixel(data, offset)) continue
          darkPixelCount += 1
          darkInRow += 1
          if (x >= rightStart) rightDarkPixelCount += 1
        }
        if (darkInRow >= width / colStep * 0.45) horizontalLineCount += 1
      }
      let inVerticalLineGroup = false
      const verticalLineThreshold = Math.max(5, Math.floor((height / rowStep) * 0.06))
      const collapsedColumnThreshold = Math.max(8, Math.floor((height / rowStep) * 0.50))
      for (let x = 0; x < width; x += 1) {
        let darkInColumn = 0
        for (let y = 0; y < height; y += rowStep) {
          const offset = (y * width + x) * 4
          if (!isDarkPixel(data, offset)) continue
          darkInColumn += 1
          if (x >= rightStart) rightPixelCount += 1
        }
        if (darkInColumn >= verticalLineThreshold) {
          if (!inVerticalLineGroup) verticalLineCount += 1
          inVerticalLineGroup = true
        } else {
          inVerticalLineGroup = false
        }
        if (x >= rightStart && darkInColumn >= collapsedColumnThreshold) narrowVerticalRunColumns += 1
      }
      return {
        width,
        height,
        rectWidth: Math.round(rect.width),
        rectHeight: Math.round(rect.height),
        darkPixelCount,
        horizontalLineCount,
        verticalLineCount,
        rightDarkRatio: rightPixelCount ? rightDarkPixelCount / rightPixelCount : 0,
        collapsedColumnDarkRatio: width ? narrowVerticalRunColumns / Math.max(1, width - rightStart) : 0
      }
    }
    const root =
      document.querySelector('#fillFormView-app .area-content') ||
      document.querySelector('#jm-sheet-wrapper .jm-sheet') ||
      document.querySelector('canvas') ||
      document.querySelector('table') ||
      document.body
    const rootRect = root.getBoundingClientRect()
    const rootRight = rootRect.right || rootRect.left + root.scrollWidth
    const canvases = Array.from(document.querySelectorAll('canvas'))
      .map(analyzeCanvas)
      .filter((canvas) => canvas.width > 0 && canvas.height > 0)
      .sort((left, right) => right.darkPixelCount - left.darkPixelCount)
    const primaryCanvas = canvases[0] || {
      width: 0,
      height: 0,
      rectWidth: 0,
      rectHeight: 0,
      darkPixelCount: 0,
      horizontalLineCount: 0,
      verticalLineCount: 0,
      rightDarkRatio: 0,
      collapsedColumnDarkRatio: 0
    }
    const candidates = Array.from(root.querySelectorAll('td, th, *')).filter((element) => {
      if (!(element instanceof HTMLElement)) return false
      if (['SCRIPT', 'STYLE', 'LINK'].includes(element.tagName)) return false
      const rect = element.getBoundingClientRect()
      if (rect.width < 1 || rect.height < 1) return false
      const style = getComputedStyle(element)
      const borderWidths = [
        Number.parseFloat(style.borderTopWidth) || 0,
        Number.parseFloat(style.borderRightWidth) || 0,
        Number.parseFloat(style.borderBottomWidth) || 0,
        Number.parseFloat(style.borderLeftWidth) || 0
      ]
      const hasBorder = borderWidths.some((width) => width > 0)
      const className = String(element.className || '')
      return element.matches('td,th') || hasBorder || /cell|sheet|row|col/i.test(className)
    })
    const cells = candidates.map((element) => {
      const rect = element.getBoundingClientRect()
      const style = getComputedStyle(element)
      const text = String(element.innerText || element.textContent || '').replace(/\s+/g, ' ').trim()
      const borderWidths = [
        Number.parseFloat(style.borderTopWidth) || 0,
        Number.parseFloat(style.borderRightWidth) || 0,
        Number.parseFloat(style.borderBottomWidth) || 0,
        Number.parseFloat(style.borderLeftWidth) || 0
      ]
      return {
        text,
        width: Math.round(rect.width),
        height: Math.round(rect.height),
        left: Math.round(rect.left),
        right: Math.round(rect.right),
        hasBorder: borderWidths.some((width) => width > 0),
        borderSideCount: borderWidths.filter((width) => width > 0).length
      }
    })
    const visibleCells = cells.filter((cell) => cell.width > 2 && cell.height > 5)
    const nonEmptyCells = visibleCells.filter((cell) => cell.text.length > 0)
    const borderedCells = visibleCells.filter((cell) => cell.hasBorder)
    const narrowBlankRightCells = visibleCells.filter(
      (cell) => !cell.text && cell.width < 8 && cell.height > 12 && cell.right >= rootRight - 20
    )
    const collapsedTextCells = nonEmptyCells
      .filter((cell) => cell.text.length >= 3 && cell.width < 18 && cell.height > cell.width * 4)
      .slice(0, 8)
    return {
      reportName,
      frameUrl: window.location.href,
      title: document.title,
      bodyText: document.body.innerText.replace(/\s+/g, ' ').trim().slice(0, 1000),
      rootWidth: Math.round(Math.max(root.scrollWidth, rootRect.width, primaryCanvas.rectWidth)),
      rootHeight: Math.round(Math.max(root.scrollHeight, rootRect.height, primaryCanvas.rectHeight)),
      canvasCount: canvases.length,
      canvasWidth: primaryCanvas.width,
      canvasHeight: primaryCanvas.height,
      canvasDarkPixelCount: primaryCanvas.darkPixelCount,
      canvasHorizontalLineCount: primaryCanvas.horizontalLineCount,
      canvasVerticalLineCount: primaryCanvas.verticalLineCount,
      canvasRightDarkRatio: primaryCanvas.rightDarkRatio,
      canvasCollapsedColumnDarkRatio: primaryCanvas.collapsedColumnDarkRatio,
      cellCount: visibleCells.length,
      nonEmptyCellCount: nonEmptyCells.length,
      borderedRatio: visibleCells.length ? borderedCells.length / visibleCells.length : 0,
      narrowBlankRightCount: narrowBlankRightCells.length,
      narrowBlankRightSamples: narrowBlankRightCells.slice(0, 8),
      collapsedTextSamples: collapsedTextCells
    }
  }, report.reportName)

  if (!metrics.title.includes(report.reportName)) {
    const debugFile = path.join(OUTPUT_DIR, `${screenshotName(report.reportName)}.debug.png`)
    await page.screenshot({ path: debugFile, fullPage: true }).catch(() => undefined)
    throw new Error(
      `${report.reportName} 预览标题必须包含表格标题；frameUrl=${metrics.frameUrl}; title=${metrics.title}; bodyText=${metrics.bodyText.slice(0, 500)}; screenshot=${debugFile}`
    )
  }
  assert.ok(metrics.title.includes(report.reportName), `${report.reportName} 预览标题必须包含表格标题`)
  assert.ok(metrics.rootWidth >= 600, `${report.reportName} 预览宽度过窄：${metrics.rootWidth}`)
  assert.ok(metrics.rootHeight >= 260, `${report.reportName} 预览高度过低：${metrics.rootHeight}`)
  assert.ok(metrics.canvasCount >= 1, `${report.reportName} 预览必须包含 JMReport 画布`)
  assert.ok(metrics.canvasWidth >= 600, `${report.reportName} 画布宽度过窄：${metrics.canvasWidth}`)
  assert.ok(metrics.canvasHeight >= 240, `${report.reportName} 画布高度过低：${metrics.canvasHeight}`)
  assert.ok(metrics.canvasDarkPixelCount >= 500, `${report.reportName} 画布有效像素过少：${metrics.canvasDarkPixelCount}`)
  assert.ok(metrics.canvasHorizontalLineCount >= 5, `${report.reportName} 横向网格线过少：${metrics.canvasHorizontalLineCount}`)
  assert.ok(metrics.canvasVerticalLineCount >= 3, `${report.reportName} 纵向网格线过少：${metrics.canvasVerticalLineCount}`)
  assert.ok(
    metrics.canvasCollapsedColumnDarkRatio < 0.18,
    `${report.reportName} 右侧疑似窄列竖向堆叠：${metrics.canvasCollapsedColumnDarkRatio}`
  )
  assert.equal(
    metrics.narrowBlankRightCount,
    0,
    `${report.reportName} 存在右侧幽灵窄空白列：${JSON.stringify(metrics.narrowBlankRightSamples)}`
  )
  assert.deepEqual(
    metrics.collapsedTextSamples,
    [],
    `${report.reportName} 存在疑似内容竖排塌陷：${JSON.stringify(metrics.collapsedTextSamples)}`
  )
  return metrics
}

function screenshotName(title) {
  return `${title.replace(/[\\/:*?"<>|\s]+/g, '_')}.png`
}

function writeEvidence(status, details) {
  ensureDir(OUTPUT_DIR)
  const lines = [
    '# Word 批记录表格真实页面渲染 E2E 证据',
    '',
    `- 状态：${status}`,
    `- 前端入口：${BASE_URL}`,
    `- 后端入口：${BACKEND_URL}`,
    `- 租户/账号：${TEST_TENANT}/${TEST_USERNAME}`,
    `- Word 文档：${SAMPLE_DOC_PATH}`,
    `- 批记录名称：${BATCH_RECORD_NAME}`,
    '',
    '## BDD',
    '',
    '- BDD: Word 表格真实导入渲染 -> Given 测试租户真实登录并上传用户指定 .doc 批记录 When 页面完成路线 B 识别并逐个打开报表预览 Then 15 张表均在 JMReport iframe 中渲染出标题、文本、边框网格和稳定宽高比例。',
    '- BDD: 视觉异常阻断 -> Given 任一表格出现右侧幽灵窄列、文本竖排塌陷、字段丢失、边框网格缺失或整体压缩 When 脚本读取 iframe canvas 像素与 DOM 几何 Then 直接失败并记录具体表格。',
    '',
    '## 表格指标',
    '',
    '| 序号 | 表格 | 宽 | 高 | 画布 | 有效像素 | 横线 | 竖线 | 右侧窄列比 | 右侧窄空白 |',
    '|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|',
    ...(details.metrics || []).map(
      (item, index) =>
        `| ${index + 1} | ${item.reportName} | ${item.rootWidth} | ${item.rootHeight} | ${item.canvasCount} | ${item.canvasDarkPixelCount} | ${item.canvasHorizontalLineCount} | ${item.canvasVerticalLineCount} | ${item.canvasCollapsedColumnDarkRatio.toFixed(3)} | ${item.narrowBlankRightCount} |`
    ),
    '',
    '## 截图',
    '',
    ...(details.screenshots || []).map((item) => `- ${item.reportName}: ${item.file}`),
    '',
    details.error ? '## Error' : '',
    details.error ? '' : '',
    details.error ? `\`\`\`\n${details.error.stack || details.error.message || String(details.error)}\n\`\`\`` : ''
  ].filter((line) => line !== undefined)
  fs.writeFileSync(EVIDENCE_FILE, `${lines.join('\n')}\n`, 'utf8')
}

async function main() {
  assertLocalOnly()
  ensureDir(OUTPUT_DIR)
  const launchOptions = {
    headless: process.env.EDHR_WORD_TABLE_RENDER_HEADED !== '1',
    args: ['--disable-dev-shm-usage']
  }
  if (process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH) {
    launchOptions.executablePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
  }
  const browser = await chromium.launch(launchOptions)
  const metrics = []
  const screenshots = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 980 } })
    const page = await context.newPage()
    page.setDefaultTimeout(60000)
    page.setDefaultNavigationTimeout(60000)

    await login(page)
    await ensureWordTemplateAvailable(page)
    const reports = await verifyImportedReportsByApi(page)

    for (const report of reports) {
      await openReportPreviewByUi(page, report)
      const reportMetrics = await validatePreviewFrame(page, report)
      metrics.push(reportMetrics)
      if (REPRESENTATIVE_SCREENSHOT_TITLES.has(report.reportName)) {
        const file = path.join(OUTPUT_DIR, screenshotName(report.reportName))
        await page.screenshot({ path: file, fullPage: true })
        screenshots.push({ reportName: report.reportName, file })
      }
    }
    assert.equal(metrics.length, EXPECTED_TITLES.length, '必须完成 15 张表真实页面渲染验证')
    writeEvidence('PASS', { metrics, screenshots })
    console.log(
      `PASS: Word table render real E2E batchRecordName=${BATCH_RECORD_NAME} reports=${metrics.length} evidence=${EVIDENCE_FILE}`
    )
  } catch (error) {
    writeEvidence('FAIL', { metrics, screenshots, error })
    throw error
  } finally {
    await browser.close()
  }
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
