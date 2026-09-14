const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')

const frontendRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(frontendRoot, '..')
const artifactDir = path.resolve(repoRoot, 'output/playwright/scheduler-seven-issues-replan-start-dialog')
const baseUrl = (process.env.MES_REPLAN_START_DIALOG_BASE_URL || 'http://127.0.0.1:8081').replace(/\/+$/, '')
const tenant = process.env.MES_REPLAN_START_DIALOG_TENANT || readEnvDefault('VITE_APP_DEFAULT_LOGIN_TENANT') || '芋道源码'
const username = process.env.MES_REPLAN_START_DIALOG_USERNAME || readEnvDefault('VITE_APP_DEFAULT_LOGIN_USERNAME') || 'admin'
const password = process.env.MES_REPLAN_START_DIALOG_PASSWORD || readEnvDefault('VITE_APP_DEFAULT_LOGIN_PASSWORD')
const chromePath = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

function readEnvDefault(key) {
  for (const envFileName of ['.env.local', '.env']) {
    const envPath = path.join(frontendRoot, envFileName)
    if (!fs.existsSync(envPath)) continue
    const source = fs.readFileSync(envPath, 'utf8')
    const pattern = new RegExp(`^\\s*${key}\\s*=\\s*(.*)\\s*$`)
    for (const line of source.split(/\r?\n/)) {
      const match = line.match(pattern)
      if (match) return match[1].trim().replace(/^['"]|['"]$/g, '')
    }
  }
  return ''
}

async function settle(page) {
  await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => undefined)
  await page.waitForTimeout(500)
}

async function clickFirstVisible(locator, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.click()
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function fillFirstVisible(locator, value, label) {
  const count = await locator.count()
  for (let index = 0; index < count; index += 1) {
    const item = locator.nth(index)
    if (await item.isVisible().catch(() => false)) {
      await item.fill(value)
      return item
    }
  }
  throw new Error(`missing visible ${label}`)
}

async function login(page) {
  await page.goto(`${baseUrl}/login?redirect=${encodeURIComponent('/index')}`, {
    waitUntil: 'domcontentloaded',
    timeout: 60000
  })
  await settle(page)
  if (!page.url().includes('/login')) return
  if ((await page.locator('.verify-img-panel:visible, .verify-bar-area:visible, input[placeholder="请输入验证码"]:visible').count()) > 0) {
    throw new Error('登录页验证码已开启，无法无人工输入复跑真实页面只读验证。')
  }

  const tenantSelect = page.locator('.login-form .el-select').first()
  if ((await tenantSelect.count()) > 0 && (await tenantSelect.isVisible().catch(() => false))) {
    await tenantSelect.click()
    const selectInput = page.locator('.login-form .el-select__input').first()
    await selectInput.fill(tenant)
    const tenantOption = page.locator('.el-select-dropdown__item:visible').filter({ hasText: tenant }).first()
    await tenantOption.waitFor({ state: 'visible', timeout: 30000 })
    await tenantOption.click()
  } else {
    await fillFirstVisible(page.locator('input[placeholder="请输入租户名称"]'), tenant, 'tenant')
  }
  await fillFirstVisible(page.locator('input[placeholder="请输入用户名"]'), username, 'username')
  await fillFirstVisible(page.locator('input[placeholder="请输入密码"]'), password, 'password')

  const [loginResponse] = await Promise.all([
    page.waitForResponse(
      (response) => response.url().includes('/admin-api/system/auth/login') && response.status() === 200,
      { timeout: 60000 }
    ),
    clickFirstVisible(page.locator('.login-form .el-button--primary'), 'login button')
  ])
  const loginPayload = await loginResponse.json()
  assert.equal(loginPayload.code, 0, `登录接口返回业务错误: ${loginPayload.msg || loginPayload.code}`)
  await page.waitForURL((url) => !url.href.includes('/login'), { timeout: 60000 })
  await settle(page)
}

async function openScheduleOrderPage(page) {
  const responsePromise = page.waitForResponse(
    (response) => response.url().includes('/admin-api/mes/pro/schedule-order/page'),
    { timeout: 60000 }
  )
  await page.goto(`${baseUrl}/mes/pro/schedule-order`, { waitUntil: 'domcontentloaded', timeout: 60000 })
  const response = await responsePromise
  const payload = await response.json()
  assert.equal(response.status(), 200, `排产工单列表接口 HTTP ${response.status()}`)
  assert.equal(payload.code, 0, `排产工单列表接口业务错误: ${payload.msg || payload.code}`)
  await page.locator('.schedule-order-pool').waitFor({ state: 'visible', timeout: 30000 })
  return payload.data?.list?.length ?? 0
}

async function selectFirstWritableVisibleRow(page) {
  const rows = page.locator('.schedule-order-pool .el-table__body-wrapper tbody tr')
  const rowCount = await rows.count()
  for (let index = 0; index < rowCount; index += 1) {
    const row = rows.nth(index)
    if (!(await row.isVisible().catch(() => false))) continue
    const text = (await row.innerText().catch(() => '')).replace(/\s+/g, ' ').trim()
    if (!text || text.includes('暂无数据')) continue
    const checkbox = row.locator('.el-checkbox').first()
    if (!(await checkbox.isVisible().catch(() => false))) continue
    const disabledByClass = ((await checkbox.getAttribute('class').catch(() => '')) || '').includes('is-disabled')
    const disabledInput = await checkbox.locator('input').first().isDisabled().catch(() => false)
    if (disabledByClass || disabledInput) continue
    await checkbox.click()
    return text
  }
  throw new Error('当前页面没有可选择的排产工单，无法打开开始重排日期确认窗口。')
}

async function collectLayerEvidence(page) {
  return page.evaluate(() => {
    const visible = (element) => {
      if (!element) return false
      const rect = element.getBoundingClientRect()
      const style = window.getComputedStyle(element)
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
    }
    const dialogs = [...document.querySelectorAll('.el-dialog')].filter(
      (element) => visible(element) && (element.textContent || '').includes('开始重排日期')
    )
    const dialog = dialogs[dialogs.length - 1]
    const drawers = [...document.querySelectorAll('.el-drawer')].filter(
      (element) => visible(element) && (element.textContent || '').includes('排产前检查 / 手动重排')
    )
    const drawer = drawers[drawers.length - 1]
    const button = [...document.querySelectorAll('button')].find(
      (element) => visible(element) && (element.textContent || '').includes('确认应用重排')
    )
    const dialogRect = dialog?.getBoundingClientRect()
    const drawerRect = drawer?.getBoundingClientRect()
    const buttonRect = button?.getBoundingClientRect()
    const points = dialogRect
      ? [
          [dialogRect.left + dialogRect.width / 2, dialogRect.top + 24],
          [dialogRect.left + dialogRect.width / 2, dialogRect.top + dialogRect.height / 2],
          [dialogRect.left + dialogRect.width / 2, dialogRect.bottom - 24]
        ]
      : []
    const dialogReceivesPointer = points.every(([x, y]) => {
      const topElement = document.elementFromPoint(x, y)
      return Boolean(topElement && dialog && dialog.contains(topElement))
    })
    const buttonCenterX = buttonRect ? buttonRect.left + buttonRect.width / 2 : 0
    const buttonCenterY = buttonRect ? buttonRect.top + buttonRect.height / 2 : 0
    const buttonTopElement = buttonRect ? document.elementFromPoint(buttonCenterX, buttonCenterY) : null
    const confirmButtonReceivesPointer = Boolean(button && buttonTopElement && button.contains(buttonTopElement))
    const overlay = dialog?.closest('.el-overlay') || dialog?.parentElement
    const drawerOverlay = drawer?.closest('.el-overlay') || drawer?.parentElement
    return {
      viewport: { width: window.innerWidth, height: window.innerHeight },
      dialogRect: dialogRect
        ? {
            x: dialogRect.x,
            y: dialogRect.y,
            width: dialogRect.width,
            height: dialogRect.height,
            top: dialogRect.top,
            right: dialogRect.right,
            bottom: dialogRect.bottom,
            left: dialogRect.left
          }
        : null,
      drawerRect: drawerRect
        ? {
            x: drawerRect.x,
            y: drawerRect.y,
            width: drawerRect.width,
            height: drawerRect.height,
            top: drawerRect.top,
            right: drawerRect.right,
            bottom: drawerRect.bottom,
            left: drawerRect.left
          }
        : null,
      buttonRect: buttonRect
        ? {
            x: buttonRect.x,
            y: buttonRect.y,
            width: buttonRect.width,
            height: buttonRect.height,
            top: buttonRect.top,
            right: buttonRect.right,
            bottom: buttonRect.bottom,
            left: buttonRect.left
          }
        : null,
      dialogOverlayZIndex: overlay ? window.getComputedStyle(overlay).zIndex : null,
      drawerOverlayZIndex: drawerOverlay ? window.getComputedStyle(drawerOverlay).zIndex : null,
      dialogOpacity: dialog ? Number(window.getComputedStyle(dialog).opacity) : null,
      dialogReceivesPointer,
      confirmButtonReceivesPointer,
      dialogMountedInsideDrawer: Boolean(dialog && drawer && drawer.contains(dialog)),
      hintText: dialog?.textContent?.includes('计算日期从 00:00 开始') && dialog?.textContent?.includes('实际任务按班次 08:00 开始')
    }
  })
}

async function main() {
  assert.ok(password, '默认本机登录密码缺失，无法执行真实页面只读验证。')
  fs.mkdirSync(artifactDir, { recursive: true })
  const resultPath = path.join(artifactDir, 'result.json')
  const screenshotPath = path.join(artifactDir, 'replan-start-dialog.png')
  const targetMesWrites = []
  const browser = await chromium.launch({
    headless: process.env.MES_REPLAN_START_DIALOG_HEADED !== '1',
    executablePath: fs.existsSync(chromePath) ? chromePath : undefined
  })
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  const result = {
    status: 'UNKNOWN',
    baseUrl,
    tenantUser: `${tenant}/${username}`,
    targetMesWrites,
    artifactDir,
    screenshotPath
  }
  try {
    page.on('request', (request) => {
      const url = request.url()
      if (url.includes('/admin-api/mes/') && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) {
        targetMesWrites.push({ method: request.method(), path: new URL(url).pathname.replace(/^\/admin-api\//, '') })
      }
    })
    await login(page)
    result.initialListCount = await openScheduleOrderPage(page)
    result.selectedRowText = await selectFirstWritableVisibleRow(page)
    await clickFirstVisible(page.getByRole('button', { name: /手动重排/ }), 'manual replan button')
    const drawer = page.locator('.el-drawer:visible').filter({ hasText: '排产前检查 / 手动重排' }).first()
    await drawer.waitFor({ state: 'visible', timeout: 60000 })
    const startButton = drawer.getByRole('button', { name: /开始重排/ }).first()
    await startButton.waitFor({ state: 'visible', timeout: 60000 })
    assert.equal(await startButton.isDisabled(), false, '开始重排按钮必须可用。')
    await startButton.click()
    const dateDialog = page.locator('.el-dialog:visible').filter({ hasText: '开始重排日期' }).last()
    await dateDialog.waitFor({ state: 'visible', timeout: 60000 })
    const confirmButton = dateDialog.getByRole('button', { name: /确认应用重排/ }).first()
    await confirmButton.waitFor({ state: 'visible', timeout: 30000 })
    await page.waitForTimeout(350)
    assert.equal(await confirmButton.isEnabled(), true, '确认应用重排按钮必须可交互。')
    result.layerEvidence = await collectLayerEvidence(page)
    assert.equal(result.layerEvidence.dialogMountedInsideDrawer, false, '开始重排日期确认窗口不得挂载在右侧重排抽屉内。')
    assert.equal(result.layerEvidence.dialogReceivesPointer, true, '开始重排日期确认窗口不得被右侧抽屉遮挡。')
    assert.equal(result.layerEvidence.confirmButtonReceivesPointer, true, '确认应用重排按钮不得被右侧抽屉遮挡。')
    assert.ok(result.layerEvidence.dialogOpacity >= 0.95, '开始重排日期确认窗口必须完成入场动画后再验收。')
    assert.equal(result.layerEvidence.hintText, true, '窗口必须展示 00:00 计算边界与实际班次开始的说明。')
    assert.equal(targetMesWrites.length, 0, `只读验证不允许发出 MES 写请求: ${JSON.stringify(targetMesWrites)}`)
    await page.screenshot({ path: screenshotPath, fullPage: true })
    result.status = 'PASS'
  } catch (error) {
    result.status = 'FAIL'
    result.error = error && error.stack ? error.stack : String(error)
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => undefined)
    throw error
  } finally {
    fs.writeFileSync(resultPath, JSON.stringify(result, null, 2), 'utf8')
    await browser.close()
    console.log(JSON.stringify({
      status: result.status,
      tenantUser: result.tenantUser,
      selectedRowPresent: Boolean(result.selectedRowText),
      dialogMountedInsideDrawer: result.layerEvidence?.dialogMountedInsideDrawer,
      dialogReceivesPointer: result.layerEvidence?.dialogReceivesPointer,
      confirmButtonReceivesPointer: result.layerEvidence?.confirmButtonReceivesPointer,
      targetMesWriteCount: targetMesWrites.length,
      resultPath,
      screenshotPath
    }, null, 2))
  }
}

main().catch((error) => {
  console.error(error.stack || error.message)
  process.exit(1)
})
